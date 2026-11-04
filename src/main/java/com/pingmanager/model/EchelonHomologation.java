package com.pingmanager.model;

/**
 * Échelon qui délivre l'homologation d'un tournoi. Il n'est pas choisi par
 * l'organisateur : il découle de la catégorie et de la dotation
 * ({@link #pour(NiveauTournoi, double)}).
 *
 * <p>Règlements sportifs 2026-2027 :</p>
 * <ul>
 *   <li>IX.103 — la commission sportive <b>fédérale</b> homologue les tournois
 *       nationaux, internationaux, et tout tournoi dont la dotation globale
 *       dépasse 3 000 € ;</li>
 *   <li>IX.103 / IX.105 — la commission sportive <b>régionale</b> (ligue)
 *       homologue les autres ; la ligue peut déléguer aux comités départementaux
 *       l'homologation de leurs propres tournois. Cette délégation dépend de
 *       chaque ligue : on ne peut pas la déduire, d'où {@link #LIGUE} par défaut.</li>
 * </ul>
 */
public enum EchelonHomologation {

    COMITE_DEPARTEMENTAL("Comité départemental (par délégation de la ligue)"),
    LIGUE("Ligue régionale (commission sportive régionale)"),
    FFTT("FFTT (commission sportive fédérale)");

    /** Seuil de dotation au-delà duquel l'homologation est fédérale (IX.103). */
    public static final double SEUIL_DOTATION_FEDERALE = 3000.0;

    private final String libelle;

    EchelonHomologation(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }

    /**
     * Échelon compétent pour homologuer un tournoi (IX.103).
     *
     * @param niveau          la catégorie du tournoi ({@code null} : inconnu)
     * @param dotationGlobale la dotation globale en euros (espèces + nature)
     * @return l'échelon, ou {@code null} si la catégorie n'est pas encore choisie
     */
    public static EchelonHomologation pour(NiveauTournoi niveau, double dotationGlobale) {
        if (niveau == null) {
            return null;
        }
        if (niveau.estNationalOuInternational() || dotationGlobale > SEUIL_DOTATION_FEDERALE) {
            return FFTT;
        }
        return LIGUE;
    }
}
