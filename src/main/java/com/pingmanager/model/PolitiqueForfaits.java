package com.pingmanager.model;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Remboursement des droits d'engagement en cas de forfait, selon le moment où
 * le joueur se retire : pendant les inscriptions, entre leur clôture et le
 * tournoi, ou le jour même.
 */
public final class PolitiqueForfaits {

    /** Moment du forfait. */
    public enum Periode {
        PENDANT_INSCRIPTIONS("Pendant les inscriptions"),
        APRES_CLOTURE("Après la clôture, avant le tournoi"),
        JOUR_J("Le jour du tournoi");

        private final String libelle;

        Periode(String libelle) {
            this.libelle = libelle;
        }

        public String getLibelle() {
            return libelle;
        }
    }

    /** Niveau de remboursement. */
    public enum Remboursement {
        AUCUN("Aucun remboursement"),
        PARTIEL("Partiel"),
        COMPLET("Complet");

        private final String libelle;

        Remboursement(String libelle) {
            this.libelle = libelle;
        }

        public String getLibelle() {
            return libelle;
        }
    }

    /**
     * Règle d'une période.
     *
     * @param remboursement aucun, partiel ou complet
     * @param pourcentage   part remboursée si partiel (1 à 99)
     * @param justificatif  remboursement soumis à un justificatif (certificat médical…)
     */
    public record Regle(Remboursement remboursement, int pourcentage, boolean justificatif) {
        public Regle {
            if (remboursement == null) {
                throw new IllegalArgumentException("Choisissez le remboursement.");
            }
            if (remboursement == Remboursement.PARTIEL && (pourcentage < 1 || pourcentage > 99)) {
                throw new IllegalArgumentException("Remboursement partiel : une part entre 1 et 99 %.");
            }
            if (remboursement != Remboursement.PARTIEL) {
                pourcentage = remboursement == Remboursement.COMPLET ? 100 : 0;
            }
            if (remboursement == Remboursement.AUCUN) {
                justificatif = false;
            }
        }
    }

    private final Map<Periode, Regle> regles;
    private final boolean fraisEnLigneDeduits;

    public PolitiqueForfaits(Map<Periode, Regle> regles, boolean fraisEnLigneDeduits) {
        EnumMap<Periode, Regle> m = new EnumMap<>(Periode.class);
        if (regles != null) {
            m.putAll(regles);
        }
        this.regles = Collections.unmodifiableMap(m);
        this.fraisEnLigneDeduits = fraisEnLigneDeduits;
    }

    public static PolitiqueForfaits nonDefinie() {
        return new PolitiqueForfaits(Map.of(), false);
    }

    /** @return la règle de la période, ou {@code null} si elle n'est pas encore choisie. */
    public Regle pour(Periode p) {
        return regles.get(p);
    }

    public Map<Periode, Regle> getRegles() {
        return regles;
    }

    public boolean isFraisEnLigneDeduits() {
        return fraisEnLigneDeduits;
    }

    /** @return vrai si les trois périodes sont réglées. */
    public boolean estComplete() {
        return regles.size() == Periode.values().length;
    }
}
