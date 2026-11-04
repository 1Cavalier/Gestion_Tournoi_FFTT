package com.pingmanager.model;

/**
 * Catégorie d'un tournoi homologué FFTT (règlements sportifs IX.102), de la plus
 * locale à la plus élevée : promotionnel, départemental, régional, national B,
 * national A, international. « Aucune dérogation ne peut être apportée à la
 * présente classification. »
 *
 * <p>La catégorie détermine aussi l'échelon qui homologue le tournoi
 * ({@link EchelonHomologation#pour(NiveauTournoi, double)}, IX.103).</p>
 *
 * <p>Le niveau conditionne notamment le {@link GradeJugeArbitre grade de
 * juge-arbitre} minimal requis pour diriger l'épreuve. Les correspondances
 * exactes pourront être ajustées à la réglementation en vigueur ; la structure
 * permet d'exprimer la règle sans la figer.</p>
 */
public enum NiveauTournoi {

    /** Résultats non pris en compte pour les classements individuels (IX.102). */
    PROMOTIONNEL("Promotionnel", GradeJugeArbitre.JA3),
    DEPARTEMENTAL("Départemental", GradeJugeArbitre.JA3),
    REGIONAL("Régional", GradeJugeArbitre.JA3),
    /** Dotation inférieure à 5 000 €. */
    NATIONAL_B("National B", GradeJugeArbitre.JA3),
    /** Dotation supérieure ou égale à 5 000 €. */
    NATIONAL_A("National A", GradeJugeArbitre.JA_NATIONAL),
    INTERNATIONAL("International", GradeJugeArbitre.JA_NATIONAL);

    private final String libelle;
    private final GradeJugeArbitre gradeJugeArbitreRequis;

    NiveauTournoi(String libelle, GradeJugeArbitre gradeJugeArbitreRequis) {
        this.libelle = libelle;
        this.gradeJugeArbitreRequis = gradeJugeArbitreRequis;
    }

    /** @return le libellé affichable (« National B »…). */
    public String getLibelle() {
        return libelle;
    }

    /** @return le libellé dans une phrase : « national B », « régional ». */
    public String getLibelleMinuscule() {
        return Character.toLowerCase(libelle.charAt(0)) + libelle.substring(1);
    }

    /** @return vrai si l'homologation relève de la commission sportive fédérale d'office. */
    public boolean estNationalOuInternational() {
        return this == NATIONAL_B || this == NATIONAL_A || this == INTERNATIONAL;
    }

    /**
     * IX.104 : en promotionnel, à défaut d'un JA3, un grade inférieur reste
     * possible avec l'avis préalable de la commission d'arbitrage.
     */
    public boolean derogationJugeArbitrePossible() {
        return this == PROMOTIONNEL;
    }

    /**
     * @return le grade de juge-arbitre minimal requis pour ce niveau (IX.104) :
     *         JA national en national A et international, JA3 ailleurs.
     */
    public GradeJugeArbitre getGradeJugeArbitreRequis() {
        return gradeJugeArbitreRequis;
    }
}
