package com.pingmanager.ui.ecran;

import com.pingmanager.core.BilanRemplissageFinances;
import com.pingmanager.core.BilanRemplissageFinances.LigneTableau;
import com.pingmanager.core.reglement.Formats;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Présentation (sans JavaFX) de la carte « Remplissage et finances » de la
 * deuxième version de l'écran du tournoi (B036) : chiffres clés, phrases de
 * synthèse et une ligne par tableau, à partir du {@link BilanRemplissageFinances}
 * calculé par le cœur.
 */
final class PresentationRemplissageFinances {

    static final String TITRE = "Remplissage et finances";

    /** Tonalité d'un chiffre : neutre, positif (vert) ou négatif (rouge). */
    enum Tonalite {
        NEUTRE(""), POSITIF("finances-positif"), NEGATIF("finances-negatif");

        private final String classeCss;

        Tonalite(String classeCss) {
            this.classeCss = classeCss;
        }

        /** @return la classe CSS à poser sur la valeur (vide si neutre). */
        String classeCss() {
            return classeCss;
        }
    }

    /** Un chiffre clé : valeur, libellé et tonalité. */
    record Chiffre(String valeur, String libelle, Tonalite tonalite) {
    }

    /**
     * Une ligne par tableau.
     *
     * @param jauge fraction 0..1 pour la jauge, ou -1 si le tableau n'a pas de limite
     */
    record Ligne(String nom, String remplissage, double jauge, String attente, String argent, String dotation) {
    }

    private final BilanRemplissageFinances bilan;

    PresentationRemplissageFinances(BilanRemplissageFinances bilan) {
        this.bilan = Objects.requireNonNull(bilan, "bilan");
    }

    /** @return vrai s'il n'y a encore aucun tableau à suivre. */
    boolean vide() {
        return bilan.tableaux().isEmpty();
    }

    String messageVide() {
        return "Ajoutez des tableaux pour suivre leur remplissage et le budget du tournoi.";
    }

    /** Quatre chiffres clés : remplissage, recettes encaissées, dotation promise, solde. */
    List<Chiffre> chiffres() {
        List<Chiffre> res = new ArrayList<>();
        String remplissage = bilan.places() > 0 ? pourcent(bilan.taux()) : String.valueOf(bilan.inscrits());
        String libRemplissage = bilan.places() > 0
                ? bilan.placesOccupees() + " / " + bilan.places() + " places occupées" + suffixeSansLimite()
                : (bilan.inscrits() > 1 ? "inscrits" : "inscrit") + " (pas de limite de places)";
        res.add(new Chiffre(remplissage, libRemplissage, Tonalite.NEUTRE));
        res.add(new Chiffre(Formats.montant(bilan.encaisse()),
                "encaissés sur " + Formats.montant(bilan.recettesAttendues()) + " attendus", Tonalite.NEUTRE));
        res.add(new Chiffre(Formats.montant(bilan.dotationPromise()), libelleDotation(), Tonalite.NEUTRE));
        res.add(new Chiffre(signe(bilan.solde()), "solde : encaissé − dotation", tonalite(bilan.solde())));
        return res;
    }

    /** @return la fraction 0..1 de la jauge globale (places des tableaux limités). */
    double jaugeGlobale() {
        return bilan.taux();
    }

    /** « 42 inscriptions · 3 en liste d'attente (rien à payer) · 1 tableau sans limite de places ». */
    String phraseRemplissage() {
        StringBuilder sb = new StringBuilder();
        int n = bilan.inscrits();
        sb.append(n == 0 ? "Aucune inscription pour l'instant" : n + (n > 1 ? " inscriptions" : " inscription"));
        if (bilan.enAttente() > 0) {
            sb.append(" · ").append(bilan.enAttente()).append(" en liste d'attente (rien à payer avant d'avoir une place)");
        }
        int libres = bilan.tableauxSansLimite();
        if (libres > 0) {
            sb.append(" · ").append(libres).append(libres > 1 ? " tableaux" : " tableau")
                    .append(" sans limite de places, hors taux");
        }
        return sb.append('.').toString();
    }

    /** « Reste à encaisser : 120 €. Si tous les inscrits règlent, solde de +40 €. » */
    String phraseSolde() {
        return "Reste à encaisser : " + Formats.montant(bilan.resteAEncaisser())
                + ". Si tous les inscrits règlent, solde de " + signe(bilan.soldePrevisionnel()) + ".";
    }

    Tonalite tonaliteSoldePrevisionnel() {
        return tonalite(bilan.soldePrevisionnel());
    }

    /** Une ligne par tableau, dans l'ordre du planning. */
    List<Ligne> lignes() {
        List<Ligne> res = new ArrayList<>();
        for (LigneTableau l : bilan.tableaux()) {
            String remplissage = l.sansLimite()
                    ? l.inscrits() + (l.inscrits() > 1 ? " inscrits" : " inscrit") + " · sans limite"
                    : l.inscrits() + " / " + l.places() + " · " + pourcent(l.taux());
            String attente = l.enAttente() == 0 ? "" : l.enAttente() + " en attente";
            String argent = l.tarif() == 0 ? "Gratuit"
                    : l.inscrits() == 0 ? Formats.montant(l.tarif()) + " l'inscription"
                    : Formats.montant(l.encaisse()) + " / " + Formats.montant(l.recettesAttendues()) + " encaissés";
            res.add(new Ligne(l.nom(), remplissage, l.sansLimite() ? -1 : l.taux(), attente, argent,
                    dotation(l)));
        }
        return res;
    }

    /** « Dotation 190 € », « Dotation 40 € + 100 % des inscriptions (90 €) », « Pas de dotation ». */
    static String dotation(LigneTableau l) {
        if (l.dotationFixe() == 0 && l.partInscriptionsPct() == 0) {
            return "Pas de dotation";
        }
        StringBuilder sb = new StringBuilder("Dotation ");
        if (l.dotationFixe() > 0) {
            sb.append(Formats.montant(l.dotationFixe()));
        }
        if (l.partInscriptionsPct() > 0) {
            if (l.dotationFixe() > 0) {
                sb.append(" + ");
            }
            sb.append(nombre(l.partInscriptionsPct())).append(" % des inscriptions (")
                    .append(Formats.montant(l.dotationPart())).append(')');
        }
        return sb.toString();
    }

    private String libelleDotation() {
        if (bilan.dotationPart() > 0) {
            return "dotation promise, dont " + Formats.montant(bilan.dotationPart()) + " en part des inscriptions";
        }
        return "dotation promise";
    }

    private String suffixeSansLimite() {
        return bilan.tableauxSansLimite() > 0 ? " (hors tableaux sans limite)" : "";
    }

    static Tonalite tonalite(double montant) {
        if (montant < 0) {
            return Tonalite.NEGATIF;
        }
        return montant > 0 ? Tonalite.POSITIF : Tonalite.NEUTRE;
    }

    /** Montant signé : « −190 € », « +40 € », « 0 € ». */
    static String signe(double montant) {
        if (montant < 0) {
            return "−" + Formats.montant(-montant);
        }
        return montant > 0 ? "+" + Formats.montant(montant) : Formats.montant(0);
    }

    /** 0,4375 → « 44 % ». */
    static String pourcent(double fraction) {
        return Math.round(fraction * 100) + " %";
    }

    private static String nombre(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.valueOf(v).replace('.', ',');
    }
}
