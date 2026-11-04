package com.pingmanager.model;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Optional;

/**
 * Grade d'un juge-arbitre (JA) FFTT, par ordre croissant de qualification :
 * JA1, JA2, JA3, juge-arbitre national.
 *
 * <p>Règlements sportifs IX.104 : le juge-arbitre d'un tournoi doit être
 * <b>juge-arbitre national</b> pour les tournois internationaux et nationaux de
 * classe A, et <b>au moins JA3</b> pour les autres ; pour un tournoi
 * promotionnel, à défaut d'un JA3, l'avis préalable de la commission
 * d'arbitrage est requis. Voir {@link NiveauTournoi#getGradeJugeArbitreRequis()}.</p>
 *
 * <p>Un grade plus élevé couvre les besoins d'un grade plus bas
 * ({@link #couvre(GradeJugeArbitre)}).</p>
 */
public enum GradeJugeArbitre {

    /** Juge-arbitre 1er degré. */
    JA1("JA1", "JA1"),
    /** Juge-arbitre 2e degré. */
    JA2("JA2", "JA2"),
    /** Juge-arbitre 3e degré : grade minimum pour un tournoi (IX.104). */
    JA3("JA3", "JA3"),
    /** Juge-arbitre national : requis en national A et en international (IX.104). */
    JA_NATIONAL("JAN", "JA national");

    private final String code;
    private final String libelle;

    GradeJugeArbitre(String code, String libelle) {
        this.code = code;
        this.libelle = libelle;
    }

    /** @return le code court (« JA3 », « JAN »). */
    public String getCode() {
        return code;
    }

    /** @return le libellé affichable (« JA3 », « JA national »). */
    public String getLibelle() {
        return libelle;
    }

    /**
     * @param requis grade minimum demandé
     * @return {@code true} si ce grade est au moins égal au grade requis
     */
    public boolean couvre(GradeJugeArbitre requis) {
        return requis != null && this.ordinal() >= requis.ordinal();
    }

    /**
     * Lit un grade tel qu'il figure sur une fiche de licence ou dans un fichier :
     * « JA1 », « ja 3 », « JAN », « JA national »… Les anciens noms internes
     * (« JA_DEPARTEMENTAL », « JA_REGIONAL ») sont relus comme JA1 et JA2.
     *
     * @return le grade, ou vide si le texte n'est pas reconnu
     */
    public static Optional<GradeJugeArbitre> depuisCode(String texte) {
        if (texte == null || texte.isBlank()) {
            return Optional.empty();
        }
        String t = Normalizer.normalize(texte, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT).replaceAll("[\\s_.-]", "");
        switch (t) {
            case "JA1":
            case "JADEPARTEMENTAL":
                return Optional.of(JA1);
            case "JA2":
            case "JAREGIONAL":
                return Optional.of(JA2);
            case "JA3":
                return Optional.of(JA3);
            case "JAN":
            case "JANATIONAL":
            case "JANAT":
                return Optional.of(JA_NATIONAL);
            default:
                return Optional.empty();
        }
    }
}
