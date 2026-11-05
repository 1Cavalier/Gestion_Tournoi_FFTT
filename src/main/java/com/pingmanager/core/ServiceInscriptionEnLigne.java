package com.pingmanager.core;

import com.pingmanager.model.AgendaCompte;
import com.pingmanager.model.FicheLicence;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.StatutTableau;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Inscription <b>en ligne</b> d'un licencié par lui-même (onglet Tournois). Ajoute aux
 * règles de tableau de {@link ServiceInscription} les conditions propres au tournoi :
 * accès selon la catégorie (IX.101 / IX.102, {@link EligibiliteTournoi}) et période
 * d'inscription ouverte ({@link CalendrierTournoi}). Le paiement n'est pas traité ici :
 * l'inscription est enregistrée, le montant reste « à régler » (étape ultérieure).
 */
public final class ServiceInscriptionEnLigne {

    /**
     * Bilan d'une demande : refus global éventuel (accès, période), résultat par
     * tableau et montants au tarif en ligne.
     */
    public record Bilan(String refusGlobal, Joueur joueur, Map<Tableau, ResultatInscriptionTournoi> parTableau,
                        double montantInscrit, double montantSiRepeche) {

        /** @return vrai si la demande peut être (ou a été) enregistrée en entier. */
        public boolean estValide() {
            return refusGlobal == null && !parTableau.isEmpty()
                    && parTableau.values().stream().allMatch(ResultatInscriptionTournoi::estAccepte);
        }

        public long nombreRefus() {
            return parTableau.values().stream().filter(r -> !r.estAccepte()).count();
        }
    }

    /**
     * Bilan d'une désinscription : refus éventuel (rien n'est alors modifié), tableaux
     * quittés, joueurs repêchés de la liste d'attente, et mise à jour de l'agenda
     * ({@code null} si aucun agenda n'est fourni).
     */
    public record BilanDesinscription(String refus, List<Tableau> quittes, Map<Tableau, Joueur> repeches,
                                      boolean desinscriptionComplete, ResultatAgenda agenda) {

        public boolean estValide() {
            return refus == null;
        }
    }

    private final ServiceInscription inscription;
    private final EligibiliteTournoi eligibilite;
    private final ServiceAgenda agenda;

    public ServiceInscriptionEnLigne(ServiceInscription inscription, EligibiliteTournoi eligibilite) {
        this(inscription, eligibilite, ServiceAgenda.parDefaut());
    }

    public ServiceInscriptionEnLigne(ServiceInscription inscription, EligibiliteTournoi eligibilite,
                                     ServiceAgenda agenda) {
        this.inscription = Objects.requireNonNull(inscription, "inscription");
        this.eligibilite = Objects.requireNonNull(eligibilite, "eligibilite");
        this.agenda = Objects.requireNonNull(agenda, "agenda");
    }

    /**
     * Refus qui empêche toute inscription à ce tournoi, ou {@code null} : licence non
     * liée, accès IX.102 refusé, inscriptions pas encore ouvertes ou closes.
     */
    public String refusGlobal(Tournoi t, FicheLicence fiche, LocalDateTime maintenant) {
        if (fiche == null) {
            return "Liez votre licence dans « Mon profil » pour vous inscrire.";
        }
        EligibiliteTournoi.Acces acces = eligibilite.evaluer(t, fiche);
        if (acces.estRefuse()) {
            return acces.motif();
        }
        if (CalendrierTournoi.etatInscriptions(t, maintenant) != CalendrierTournoi.EtatInscriptions.OUVERTES) {
            return CalendrierTournoi.texteInscriptions(t, maintenant) + ".";
        }
        return null;
    }

    /** Essai à blanc (validation en direct), sans rien modifier. */
    public Bilan simuler(Tournoi t, FicheLicence fiche, Collection<Tableau> selection, LocalDateTime maintenant) {
        String refus = refusGlobal(t, fiche, maintenant);
        Joueur j = fiche == null ? null : joueurPour(t, fiche);
        if (j == null) {
            return new Bilan(refus, null, Map.of(), 0, 0);
        }
        return bilan(refus, j, inscription.simuler(t, j, selection));
    }

    /**
     * Enregistre l'inscription (tout ou rien). Rien n'est modifié en cas de refus
     * global ou si l'un des tableaux est refusé.
     */
    public Bilan inscrire(Tournoi t, FicheLicence fiche, Collection<Tableau> selection, LocalDateTime maintenant) {
        Bilan essai = simuler(t, fiche, selection, maintenant);
        if (!essai.estValide()) {
            return essai;
        }
        return bilan(null, essai.joueur(), inscription.inscrireSelection(t, essai.joueur(), selection));
    }

    /**
     * Comme {@link #inscrire(Tournoi, FicheLicence, Collection, LocalDateTime)}, puis
     * <b>ajoute automatiquement</b> le tournoi à l'agenda du compte (ou le met à jour)
     * si l'inscription est enregistrée. Un refus ne touche pas l'agenda.
     */
    public Bilan inscrire(Tournoi t, FicheLicence fiche, Collection<Tableau> selection, LocalDateTime maintenant,
                          AgendaCompte agendaCompte) {
        Bilan b = inscrire(t, fiche, selection, maintenant);
        if (b.estValide() && agendaCompte != null) {
            agenda.synchroniserTournoi(agendaCompte, t, fiche.getNumeroLicence());
        }
        return b;
    }

    /**
     * Désinscrit la licence des tableaux choisis (tout ou rien), qu'elle y soit inscrite
     * ou en liste d'attente. Une place libérée repêche le premier de la liste d'attente
     * ({@link Tableau#desinscrire(Joueur)}). Refusé si un tableau n'est plus en phase
     * d'inscription (le pointage a commencé : c'est alors un désistement, géré par le
     * juge-arbitre). Si la licence ne participe plus à aucun tableau, le tournoi est
     * <b>retiré automatiquement</b> de l'agenda fourni ; sinon sa date est mise à jour.
     *
     * @param agendaCompte agenda du compte à tenir à jour, ou {@code null}
     */
    public BilanDesinscription desinscrire(Tournoi t, FicheLicence fiche, Collection<Tableau> tableaux,
                                           AgendaCompte agendaCompte) {
        Objects.requireNonNull(t, "tournoi");
        if (fiche == null) {
            return refusDesinscription("Liez votre licence dans « Mon profil » pour gérer vos inscriptions.");
        }
        if (tableaux == null || tableaux.isEmpty()) {
            return refusDesinscription("Choisissez au moins un tableau.");
        }
        String licence = fiche.getNumeroLicence();
        for (Tableau tab : tableaux) {
            if (!t.getTableaux().contains(tab)) {
                return refusDesinscription("Le tableau " + tab.getNom() + " n'appartient pas à ce tournoi.");
            }
            if (present(tab, licence) == null) {
                return refusDesinscription("Vous n'êtes pas inscrit au tableau " + tab.getNom() + ".");
            }
            if (tab.getStatut() != StatutTableau.INSCRIPTION) {
                return refusDesinscription("Le pointage du tableau " + tab.getNom()
                        + " a commencé : adressez-vous au juge-arbitre.");
            }
        }
        List<Tableau> quittes = new ArrayList<>();
        Map<Tableau, Joueur> repeches = new LinkedHashMap<>();
        for (Tableau tab : tableaux) {
            Joueur promu = tab.desinscrire(present(tab, licence));
            quittes.add(tab);
            if (promu != null) {
                repeches.put(tab, promu);
            }
        }
        boolean complete = RemplissageTableaux.participant(t, licence).isEmpty();
        ResultatAgenda maj = agendaCompte == null ? null : agenda.synchroniserTournoi(agendaCompte, t, licence);
        return new BilanDesinscription(null, List.copyOf(quittes), repeches, complete, maj);
    }

    private static BilanDesinscription refusDesinscription(String motif) {
        return new BilanDesinscription(motif, List.of(), Map.of(), false, null);
    }

    private static Joueur present(Tableau tab, String licence) {
        for (Joueur j : tab.getInscrits()) {
            if (j.getNumeroLicence().equals(licence)) {
                return j;
            }
        }
        for (Joueur j : tab.getFileAttente()) {
            if (j.getNumeroLicence().equals(licence)) {
                return j;
            }
        }
        return null;
    }

    /**
     * Joueur du tournoi pour cette licence : l'instance déjà inscrite s'il est présent
     * sur un tableau (égalité par licence), sinon construit depuis la fiche.
     */
    public static Joueur joueurPour(Tournoi t, FicheLicence fiche) {
        for (Tableau tab : t.getTableaux()) {
            for (Joueur j : tab.getInscrits()) {
                if (j.getNumeroLicence().equals(fiche.getNumeroLicence())) {
                    return j;
                }
            }
            for (Joueur j : tab.getFileAttente()) {
                if (j.getNumeroLicence().equals(fiche.getNumeroLicence())) {
                    return j;
                }
            }
        }
        return JoueurDepuisLicence.creer(fiche, t);
    }

    private static Bilan bilan(String refus, Joueur j, LinkedHashMap<Tableau, ResultatInscriptionTournoi> res) {
        double inscrit = 0;
        double repeche = 0;
        for (Map.Entry<Tableau, ResultatInscriptionTournoi> e : res.entrySet()) {
            if (e.getValue().getMotif() == ResultatInscriptionTournoi.Motif.INSCRIT) {
                inscrit += RemplissageTableaux.tarifEnLigne(e.getKey());
            } else if (e.getValue().getMotif() == ResultatInscriptionTournoi.Motif.EN_FILE_ATTENTE) {
                repeche += RemplissageTableaux.tarifEnLigne(e.getKey());
            }
        }
        return new Bilan(refus, j, res, inscrit, repeche);
    }
}
