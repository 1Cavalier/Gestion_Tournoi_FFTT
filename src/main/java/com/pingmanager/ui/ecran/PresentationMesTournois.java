package com.pingmanager.ui.ecran;

import com.pingmanager.core.CalendrierTournoi;
import com.pingmanager.core.PerformancesJoueur;
import com.pingmanager.core.RemplissageTableaux;
import com.pingmanager.core.ResultatAgenda;
import com.pingmanager.core.ServiceAgenda;
import com.pingmanager.core.ServiceInscriptionEnLigne;
import com.pingmanager.core.SuiviTournoi;
import com.pingmanager.model.AgendaCompte;
import com.pingmanager.model.StatutTableau;
import com.pingmanager.model.StatutTournoi;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.ui.ContexteApplication.TournoiFichier;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Logique d'affichage de « Mes tournois » (cahier des charges v0, section 3),
 * <b>sans JavaFX</b> : répartition en trois parties dans l'ordre (tournoi en cours,
 * mes inscriptions avec l'agenda, tournois passés), textes, performances et
 * désinscription. {@link MesTournois} ne fait que poser les composants.
 */
final class PresentationMesTournois {

    static final String TITRE_EN_COURS = "Tournoi en cours";
    static final String TITRE_INSCRIPTIONS = "Mes inscriptions";
    static final String TITRE_AGENDA = "Mon agenda";
    static final String TITRE_PASSES = "Tournois passés";

    private PresentationMesTournois() {
    }

    /** Les trois parties, chacune triée : en cours et inscriptions par date croissante, passés du plus récent. */
    record Repartition(List<TournoiFichier> enCours, List<TournoiFichier> inscriptions, List<TournoiFichier> passes) {
        Repartition {
            enCours = List.copyOf(enCours);
            inscriptions = List.copyOf(inscriptions);
            passes = List.copyOf(passes);
        }
    }

    /** Répartit les tournois du joueur (les brouillons sont écartés). */
    static Repartition repartir(List<TournoiFichier> mesTournois, LocalDate aujourdhui) {
        Objects.requireNonNull(aujourdhui, "aujourdhui");
        List<TournoiFichier> enCours = new ArrayList<>();
        List<TournoiFichier> aVenir = new ArrayList<>();
        List<TournoiFichier> passes = new ArrayList<>();
        for (TournoiFichier tf : mesTournois) {
            Tournoi t = tf.tournoi();
            if (t.getStatut() == StatutTournoi.BROUILLON) {
                continue;
            }
            CalendrierTournoi.Moment m = CalendrierTournoi.moment(t, aujourdhui);
            if (m == CalendrierTournoi.Moment.EN_COURS) {
                enCours.add(tf);
            } else if (m == CalendrierTournoi.Moment.PASSE) {
                passes.add(tf);
            } else {
                aVenir.add(tf);
            }
        }
        Comparator<TournoiFichier> parDate = Comparator.comparing(tf -> debut(tf.tournoi()));
        enCours.sort(parDate);
        aVenir.sort(parDate);
        passes.sort(parDate.reversed());
        return new Repartition(enCours, aVenir, passes);
    }

    private static LocalDate debut(Tournoi t) {
        LocalDate d = CalendrierTournoi.debut(t);
        return d == null ? LocalDate.MAX : d;
    }

    // ------------------------------------------------------------------
    // Textes
    // ------------------------------------------------------------------

    static String texteSansLicence() {
        return "Liez votre licence dans « Mon profil » pour retrouver ici les tournois auxquels vous êtes inscrit.";
    }

    static String texteVideEnCours() {
        return "Aucun tournoi en cours aujourd'hui.";
    }

    static String texteVideInscriptions() {
        return "Vous n'êtes inscrit à aucun tournoi à venir. Trouvez-en un dans la carte des tournois.";
    }

    static String texteVidePasses() {
        return "Aucun tournoi passé pour l'instant.";
    }

    static String texteAgendaHorsConnexion() {
        return "Connectez-vous pour tenir votre agenda.";
    }

    /** Bouton du tournoi en cours : le suivi en direct dès le lancement du premier tableau. */
    static String texteBoutonEnCours(Tournoi t) {
        return PresentationSuiviTournoi.afficherSuivi(t) ? "Suivre en direct" : "Voir la fiche";
    }

    /** Un tournoi passé se revoit (poules, scores, tableau final) s'il a été joué dans le logiciel. */
    static boolean peutRevoir(Tournoi t) {
        return SuiviTournoi.estEnSuivi(t);
    }

    static String texteBoutonPasse(Tournoi t) {
        return peutRevoir(t) ? "Revoir poule et tableau" : "Voir la fiche";
    }

    // ------------------------------------------------------------------
    // Performances (calculées par le cœur : PerformancesJoueur)
    // ------------------------------------------------------------------

    /** « 3 victoires · 1 défaite », suivi de « dont 1 forfait ou abandon » s'il y a lieu. */
    static String performances(PerformancesJoueur.Bilan b) {
        if (b == null || b.estVide()) {
            return "Aucun match enregistré";
        }
        String texte = victoiresDefaites(b.victoires(), b.defaites());
        if (b.forfaitsEtAbandons() > 0) {
            texte += " · dont " + b.forfaitsEtAbandons()
                    + (b.forfaitsEtAbandons() > 1 ? " forfaits ou abandons" : " forfait ou abandon");
        }
        return texte;
    }

    /** « Tableau B : 2 victoires · 1 défaite ». */
    static String performancesTableau(PerformancesJoueur.BilanTableau b) {
        return b.tableau().getNom() + " : " + victoiresDefaites(b.victoires(), b.defaites());
    }

    private static String victoiresDefaites(int v, int d) {
        return v + (v > 1 ? " victoires" : " victoire") + " · " + d + (d > 1 ? " défaites" : " défaite");
    }

    // ------------------------------------------------------------------
    // Désinscription
    // ------------------------------------------------------------------

    /** Tableaux (inscrit ou en attente) que le joueur peut encore quitter : ceux encore en phase d'inscription. */
    static List<Tableau> tableauxDesinscriptibles(Tournoi t, String licence) {
        List<Tableau> res = new ArrayList<>();
        if (licence == null) {
            return res;
        }
        Optional<RemplissageTableaux.Participant> p = RemplissageTableaux.participant(t, licence);
        if (p.isEmpty()) {
            return res;
        }
        List<Tableau> tous = new ArrayList<>(p.get().inscrit());
        tous.addAll(p.get().enAttente());
        for (Tableau tab : tous) {
            if (tab.getStatut() == StatutTableau.INSCRIPTION) {
                res.add(tab);
            }
        }
        return res;
    }

    /** Phrase de confirmation avant de quitter les tableaux choisis. */
    static String texteConfirmationDesinscription(List<Tableau> tableaux) {
        if (tableaux.isEmpty()) {
            return "Choisissez au moins un tableau.";
        }
        List<String> noms = new ArrayList<>();
        tableaux.forEach(t -> noms.add(t.getNom()));
        return (tableaux.size() > 1 ? "Vous quitterez les tableaux " : "Vous quitterez le tableau ")
                + String.join(", ", noms) + ". Votre place sera proposée au premier de la liste d'attente.";
    }

    /** Message après la désinscription : le motif du refus, ou ce qui a été fait. */
    static String messageDesinscription(ServiceInscriptionEnLigne.BilanDesinscription b) {
        if (!b.estValide()) {
            return b.refus();
        }
        List<String> noms = new ArrayList<>();
        b.quittes().forEach(t -> noms.add(t.getNom()));
        String texte = "Désinscription enregistrée (" + String.join(", ", noms) + ").";
        if (b.desinscriptionComplete()) {
            texte += " Le tournoi est retiré de votre agenda.";
        }
        return texte;
    }

    // ------------------------------------------------------------------
    // Agenda
    // ------------------------------------------------------------------

    /**
     * Tient à jour les dates de tournoi de l'agenda d'après les inscriptions de la
     * licence : chaque tournoi non passé (publié ou plus) est synchronisé (ajouté si
     * le joueur y participe, retiré sinon). Les tournois passés restent tels quels
     * (historique).
     *
     * @return true si l'agenda a changé (à enregistrer)
     */
    static boolean synchroniserAgenda(AgendaCompte agenda, List<TournoiFichier> tous, String licence,
                                      LocalDate aujourdhui, ServiceAgenda service) {
        if (agenda == null || licence == null) {
            return false;
        }
        boolean change = false;
        for (TournoiFichier tf : tous) {
            Tournoi t = tf.tournoi();
            if (t.getStatut() == StatutTournoi.BROUILLON
                    || CalendrierTournoi.moment(t, aujourdhui) == CalendrierTournoi.Moment.PASSE) {
                continue;
            }
            ResultatAgenda r = service.synchroniserTournoi(agenda, t, licence);
            if (r.getMotif() != ResultatAgenda.Motif.INCHANGE) {
                change = true;
            }
        }
        return change;
    }

    /** Une date personnelle se retire ; une date de tournoi suit l'inscription. */
    static boolean peutRetirer(PresentationAgenda.LigneAgenda ligne) {
        return !ligne.automatique();
    }

    static String messageExport(String nomFichier) {
        return "Agenda exporté dans « " + nomFichier + " ». Importez ce fichier dans votre agenda habituel.";
    }
}
