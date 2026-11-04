package com.pingmanager.core;

import com.pingmanager.model.Joueur;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lecture du <b>remplissage</b> des tableaux d'un tournoi, pour la prévisualisation
 * côté club et la liste des participants de la fiche : jauge des places, inscrits
 * triés par points de licence (décroissant), liste d'attente dans l'ordre, vue par
 * joueur, et montants d'engagement (tarif en ligne).
 *
 * <p>Lecture seule : rien n'est modifié.</p>
 */
public final class RemplissageTableaux {

    /** Points décroissants, puis nom et prénom (ordre de la liste des inscrits). */
    public static final Comparator<Joueur> PAR_POINTS = Comparator.comparingInt(Joueur::getPoints).reversed()
            .thenComparing(Joueur::getNom, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(Joueur::getPrenom, String.CASE_INSENSITIVE_ORDER);

    /** Club, puis points décroissants. */
    public static final Comparator<Joueur> PAR_CLUB = Comparator
            .comparing((Joueur j) -> j.getClub() == null ? "" : j.getClub(), String.CASE_INSENSITIVE_ORDER)
            .thenComparing(PAR_POINTS);

    /** Remplissage d'un tableau. */
    public record EtatTableau(Tableau tableau, int capacite, List<Joueur> inscrits, List<Joueur> attente,
                              int capaciteAttente) {
        /** @return nombre de places libres (0 si complet ; -1 si le tableau n'a pas de limite). */
        public int placesRestantes() {
            return capacite <= 0 ? -1 : Math.max(0, capacite - inscrits.size());
        }

        /** @return le taux de remplissage, entre 0 et 1 (0 sans limite de places). */
        public double taux() {
            return capacite <= 0 ? 0 : Math.min(1.0, inscrits.size() / (double) capacite);
        }

        public boolean estComplet() {
            return capacite > 0 && inscrits.size() >= capacite;
        }
    }

    /** Un participant et ses tableaux (inscrit ou en attente). */
    public record Participant(Joueur joueur, List<Tableau> inscrit, List<Tableau> enAttente) {
        public int nombreDeTableaux() {
            return inscrit.size() + enAttente.size();
        }
    }

    private RemplissageTableaux() {
    }

    /** @return l'état de chaque tableau, dans l'ordre du planning (jour, heure, nom). */
    public static List<EtatTableau> parTableau(Tournoi t) {
        List<EtatTableau> res = new ArrayList<>();
        for (Tableau tab : ordrePlanning(t)) {
            res.add(etat(tab));
        }
        return res;
    }

    public static EtatTableau etat(Tableau tab) {
        List<Joueur> inscrits = new ArrayList<>(tab.getInscrits());
        inscrits.sort(PAR_POINTS);
        return new EtatTableau(tab, tab.getConfiguration().getPlacesMax(), inscrits,
                new ArrayList<>(tab.getFileAttente()), tab.getConfiguration().getTailleMaxListeAttente());
    }

    /** Tableaux dans l'ordre du planning : jour et heure de début, puis nom. */
    public static List<Tableau> ordrePlanning(Tournoi t) {
        List<Tableau> res = new ArrayList<>(t.getTableaux());
        res.sort(Comparator.comparing((Tableau x) -> x.getHoraires() == null || x.getHoraires().getDebutMatchs() == null
                        ? java.time.LocalDateTime.MAX : x.getHoraires().getDebutMatchs())
                .thenComparing(Tableau::getNom));
        return res;
    }

    /**
     * Vue « par joueur » : chaque participant (inscrit ou en attente sur au moins un
     * tableau) avec ses tableaux dans l'ordre du planning.
     *
     * @param ordre {@link #PAR_POINTS} ou {@link #PAR_CLUB}
     */
    public static List<Participant> parJoueur(Tournoi t, Comparator<Joueur> ordre) {
        Map<Joueur, List<Tableau>> ins = new LinkedHashMap<>();
        Map<Joueur, List<Tableau>> att = new LinkedHashMap<>();
        for (Tableau tab : ordrePlanning(t)) {
            for (Joueur j : tab.getInscrits()) {
                ins.computeIfAbsent(j, k -> new ArrayList<>()).add(tab);
                att.computeIfAbsent(j, k -> new ArrayList<>());
            }
            for (Joueur j : tab.getFileAttente()) {
                att.computeIfAbsent(j, k -> new ArrayList<>()).add(tab);
                ins.computeIfAbsent(j, k -> new ArrayList<>());
            }
        }
        List<Participant> res = new ArrayList<>();
        for (Joueur j : ins.keySet()) {
            res.add(new Participant(j, ins.get(j), att.get(j)));
        }
        res.sort(Comparator.comparing(Participant::joueur, ordre));
        return res;
    }

    /** @return le participant de cette licence, s'il est présent sur au moins un tableau. */
    public static java.util.Optional<Participant> participant(Tournoi t, String licence) {
        return parJoueur(t, PAR_POINTS).stream()
                .filter(p -> p.joueur().getNumeroLicence().equals(licence)).findFirst();
    }

    /** Nombre de joueurs distincts présents (inscrits ou en attente). */
    public static int nombreDeParticipants(Tournoi t) {
        java.util.Set<Joueur> s = new java.util.HashSet<>();
        for (Tableau tab : t.getTableaux()) {
            s.addAll(tab.getInscrits());
            s.addAll(tab.getFileAttente());
        }
        return s.size();
    }

    /**
     * Montant dû au tarif en ligne : somme des tableaux où le joueur est <b>inscrit</b>
     * et n'a pas encore payé. La liste d'attente n'est pas due : on ne paie un tableau
     * qu'une fois la place obtenue (le repêché règle au pointage).
     */
    public static double montantARegler(Tournoi t, Joueur j) {
        double total = 0;
        for (Tableau tab : t.getTableaux()) {
            if (tab.getInscrits().contains(j) && !tab.aPaye(j)) {
                total += tarifEnLigne(tab);
            }
        }
        return total;
    }

    /** Tarif retenu pour une inscription en ligne (sur place si l'en-ligne n'est pas proposé). */
    public static double tarifEnLigne(Tableau tab) {
        var tarif = tab.getConfiguration().getTarif();
        if (tarif == null) {
            return 0;
        }
        return tarif.isEnLigneProposee() ? tarif.getMontantEnLigne() : tarif.getMontantSurPlace();
    }
}
