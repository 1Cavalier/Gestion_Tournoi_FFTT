package com.pingmanager.core;

import com.pingmanager.model.Joueur;
import com.pingmanager.model.ModeReglement;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.TarifInscription;
import com.pingmanager.model.Tournoi;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Liste du <b>poste de pointage</b> (B041), sans JavaFX : les inscrits de tous
 * les tableaux d'un tournoi, la recherche, les filtres et les totaux.
 *
 * <p>Une {@link Ligne} par joueur, avec un {@link Engagement} par tableau où il
 * est inscrit (le pointage et le paiement sont des faits <b>par tableau</b>).
 * Les joueurs en liste d'attente n'y figurent pas : ils ne peuvent ni pointer ni
 * payer tant qu'ils ne sont pas repêchés.</p>
 *
 * <p>Instantané : à reconstruire après chaque action (l'écran relit le
 * fichier).</p>
 */
public final class ListePointage {

    /** Filtre par état, appliqué tableau par tableau. */
    public enum FiltreEtat {
        TOUS("Tous"),
        NON_POINTE("Non pointés"),
        POINTE("Pointés"),
        NON_PAYE("Non payés"),
        PAYE("Payés");

        private final String libelle;

        FiltreEtat(String libelle) {
            this.libelle = libelle;
        }

        public String getLibelle() {
            return libelle;
        }

        boolean garde(Engagement e) {
            if (this == NON_POINTE) {
                return !e.pointe();
            }
            if (this == POINTE) {
                return e.pointe();
            }
            if (this == NON_PAYE) {
                return !e.paye();
            }
            if (this == PAYE) {
                return e.paye();
            }
            return true;
        }
    }

    /**
     * Inscription d'un joueur à un tableau, vue du poste de pointage.
     *
     * @param montant  montant encaissé si payé, sinon montant à régler sur place
     * @param modifiable faux si le tableau est lancé (plus de pointage ni de désistement)
     * @param desistementPossible faux après la clôture du pointage ou le lancement
     */
    public record Engagement(String tableau, boolean pointe, boolean paye, ModeReglement mode, double montant,
                             boolean modifiable, boolean desistementPossible) {

        /** @return vrai si l'engagement a été réglé en ligne avant le jour J. */
        public boolean payeEnLigne() {
            return paye && mode != null && mode.estEnLigne();
        }
    }

    /** Un joueur et ses engagements (après filtres). */
    public record Ligne(Joueur joueur, List<Engagement> engagements) {
        public Ligne {
            engagements = List.copyOf(engagements);
        }
    }

    /** Totaux d'un tableau (sans filtre ni recherche). */
    public record Totaux(String tableau, int inscrits, int pointes, int payes, double encaisse, double enAttente) {
    }

    private final Tournoi tournoi;

    public ListePointage(Tournoi tournoi) {
        this.tournoi = Objects.requireNonNull(tournoi, "tournoi");
    }

    /** @return les noms des tableaux, dans l'ordre du tournoi (pour le sélecteur). */
    public List<String> nomsTableaux() {
        List<String> res = new ArrayList<>();
        for (Tableau t : tournoi.getTableaux()) {
            res.add(t.getNom());
        }
        return res;
    }

    /**
     * @param recherche  texte libre (nom, prénom, licence), insensible à la casse et
     *                   aux accents ; chaque mot doit être trouvé ; vide = tout
     * @param nomTableau tableau à garder, ou {@code null} pour tous
     * @param etat       filtre d'état ({@code null} = tous)
     * @return les lignes triées par nom puis prénom
     */
    public List<Ligne> lignes(String recherche, String nomTableau, FiltreEtat etat) {
        FiltreEtat filtre = etat == null ? FiltreEtat.TOUS : etat;
        Map<Joueur, List<Engagement>> parJoueur = new LinkedHashMap<>();
        for (Tableau t : tournoi.getTableaux()) {
            if (nomTableau != null && !t.getNom().equals(nomTableau)) {
                continue;
            }
            for (Joueur j : t.getInscrits()) {
                if (j.estADeterminer() || !correspond(j, recherche)) {
                    continue;
                }
                Engagement e = engagement(t, j);
                if (filtre.garde(e)) {
                    parJoueur.computeIfAbsent(j, x -> new ArrayList<>()).add(e);
                }
            }
        }
        List<Ligne> res = new ArrayList<>();
        parJoueur.forEach((j, e) -> res.add(new Ligne(j, e)));
        res.sort(Comparator.comparing((Ligne l) -> normaliser(l.joueur().getNom()))
                .thenComparing(l -> normaliser(l.joueur().getPrenom()))
                .thenComparing(l -> l.joueur().getNumeroLicence()));
        return res;
    }

    /** @return les totaux de chaque tableau, dans l'ordre du tournoi. */
    public List<Totaux> totaux() {
        List<Totaux> res = new ArrayList<>();
        for (Tableau t : tournoi.getTableaux()) {
            int pointes = 0;
            int payes = 0;
            double encaisse = 0;
            double enAttente = 0;
            for (Joueur j : t.getInscrits()) {
                Engagement e = engagement(t, j);
                if (e.pointe()) {
                    pointes++;
                }
                if (e.paye()) {
                    payes++;
                    encaisse += e.montant();
                } else {
                    enAttente += e.montant();
                }
            }
            res.add(new Totaux(t.getNom(), t.getInscrits().size(), pointes, payes, encaisse, enAttente));
        }
        return res;
    }

    /** @return la somme des totaux de tous les tableaux (nom « Tous »). */
    public Totaux totalGeneral() {
        int inscrits = 0;
        int pointes = 0;
        int payes = 0;
        double encaisse = 0;
        double enAttente = 0;
        for (Totaux t : totaux()) {
            inscrits += t.inscrits();
            pointes += t.pointes();
            payes += t.payes();
            encaisse += t.encaisse();
            enAttente += t.enAttente();
        }
        return new Totaux("Tous", inscrits, pointes, payes, encaisse, enAttente);
    }

    private static Engagement engagement(Tableau t, Joueur j) {
        boolean paye = t.aPaye(j);
        ModeReglement mode = t.modeReglement(j);
        boolean lance = ServicePostePointage.estLance(t);
        return new Engagement(t.getNom(), t.aPointe(j), paye, mode,
                montant(t.getConfiguration().getTarif(), paye ? mode : null),
                !lance, !lance && !t.pointageEstCloture());
    }

    /**
     * Montant d'un engagement : tarif en ligne pour un règlement en ligne, tarif sur
     * place sinon (y compris un paiement ancien sans mode, et un montant restant
     * dû). Si un seul des deux tarifs est proposé, c'est lui qui s'applique.
     */
    public static double montant(TarifInscription tarif, ModeReglement mode) {
        if (tarif == null) {
            return 0;
        }
        boolean enLigne = mode != null && mode.estEnLigne();
        if (enLigne) {
            return tarif.isEnLigneProposee() ? tarif.getMontantEnLigne() : tarif.getMontantSurPlace();
        }
        return tarif.isSurPlaceProposee() ? tarif.getMontantSurPlace() : tarif.getMontantEnLigne();
    }

    /** @return vrai si chaque mot de la recherche se trouve dans le nom, le prénom ou la licence. */
    public static boolean correspond(Joueur j, String recherche) {
        if (recherche == null || recherche.isBlank()) {
            return true;
        }
        String cible = normaliser(j.getNom()) + " " + normaliser(j.getPrenom()) + " "
                + normaliser(j.getNumeroLicence());
        for (String mot : normaliser(recherche).split("\\s+")) {
            if (!mot.isEmpty() && !cible.contains(mot)) {
                return false;
            }
        }
        return true;
    }

    /** Minuscules, sans accents, tirets et apostrophes changés en espaces. */
    static String normaliser(String s) {
        if (s == null) {
            return "";
        }
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).replaceAll("[-'’]", " ").trim();
    }
}
