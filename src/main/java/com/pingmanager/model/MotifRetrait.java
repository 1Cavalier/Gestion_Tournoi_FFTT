package com.pingmanager.model;

/**
 * B043 : motif d'une <b>désinscription</b> d'un joueur d'un tableau (poste de
 * pointage ou juge-arbitre). À ne pas confondre avec le joueur <b>non pointé</b>,
 * libéré à la clôture du pointage, qui n'est jamais un choix du joueur
 * ({@link TypeOperationPointage#NON_POINTE}).
 *
 * <p>Les désistements enregistrés avant B043 n'ont pas de motif choisi dans
 * cette liste : ils se relisent comme {@link #DECISION_JOUEUR}, la seule
 * hypothèse prudente (leur texte libre est conservé comme précision).</p>
 */
public enum MotifRetrait {
    /** Le joueur retire lui-même son inscription (avant ou le jour J). */
    DECISION_JOUEUR("Décision du joueur"),
    BLESSURE_MALADIE("Blessure ou maladie"),
    EMPECHEMENT_PREVENU("Empêchement prévenu à l'avance"),
    ERREUR_INSCRIPTION("Erreur d'inscription ou doublon"),
    RETIRE_PAR_ORGANISATION("Retiré par le juge-arbitre ou l'organisateur"),
    TRANSFERT_TABLEAU("Transféré vers un autre tableau"),
    /** Autre motif : une précision en texte libre est obligatoire. */
    AUTRE("Autre");

    private final String libelle;

    MotifRetrait(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }

    /** @return vrai si une précision en texte libre est obligatoire ({@link #AUTRE}). */
    public boolean exigePrecision() {
        return this == AUTRE;
    }

    /** @return vrai pour le retrait voulu par le joueur, faux pour les « autres motifs ». */
    public boolean estDecisionDuJoueur() {
        return this == DECISION_JOUEUR;
    }

    /**
     * Relecture tolérante : nom inconnu (fichier plus récent) → {@link #AUTRE}
     * (le texte de l'opération est gardé) ; absent (fichier ancien) → {@link #DECISION_JOUEUR}.
     */
    public static MotifRetrait lire(String nom) {
        if (nom == null || nom.isBlank()) {
            return DECISION_JOUEUR;
        }
        for (MotifRetrait m : values()) {
            if (m.name().equals(nom)) {
                return m;
            }
        }
        return AUTRE;
    }
}
