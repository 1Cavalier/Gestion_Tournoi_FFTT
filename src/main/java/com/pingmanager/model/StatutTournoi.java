package com.pingmanager.model;

/**
 * Cycle de vie d'un {@link Tournoi}, du montage du dossier à l'ouverture des
 * inscriptions :
 *
 * <pre>
 *   BROUILLON ──demanderHomologation()──▶ DEMANDE_HOMOLOGATION ──homologuer(numéro)──▶ HOMOLOGUE ──publier()──▶ PUBLIE
 *       ▲               (dossier complet)        │
 *       └──── retirerDemande() / refuserHomologation(motif) ────┘
 *   (homologuer(numéro) reste possible directement depuis BROUILLON ; annuler() depuis tout état non terminal)
 * </pre>
 *
 * <ul>
 *   <li>{@link #BROUILLON} : le tournoi se construit (tableaux, capacités, nombre
 *       de tables, dates…). Tout est modifiable ; peu de contraintes.</li>
 *   <li>{@link #DEMANDE_HOMOLOGATION} : le dossier d'homologation est complet et la
 *       demande a été faite ; le club attend le numéro de la FFTT (ou de la ligue).
 *       La demande peut être retirée (retour au brouillon).</li>
 *   <li>{@link #HOMOLOGUE} : la FFTT a délivré un numéro d'homologation. Le socle
 *       du règlement (IX.111) doit être complet et cohérent, et au moins un tableau
 *       féminin doit exister (IX.104). Certains paramètres peuvent encore être
 *       ajustés, mais le tournoi est « validé ».</li>
 *   <li>{@link #PUBLIE} : le tournoi est publié ; les inscriptions sont ouvertes.
 *       C'est le point d'entrée du déroulé déjà en place (inscription →
 *       pointage → lancement).</li>
 *   <li>{@link #ANNULE} : le tournoi est abandonné. État terminal, atteignable
 *       depuis n'importe quel état non terminal.</li>
 * </ul>
 *
 * <p>Les transitions autorisées sont portées par l'enum lui-même
 * ({@link #peutAllerVers(StatutTournoi)}), ce qui centralise la règle et évite
 * de la disperser dans les services. La <i>vérification des pré-requis</i> d'une
 * transition (dossier complet, numéro présent…) relève, elle, du service de
 * cycle de vie — pas de l'enum.</p>
 */
public enum StatutTournoi {

    BROUILLON,
    DEMANDE_HOMOLOGATION,
    HOMOLOGUE,
    PUBLIE,
    ANNULE;

    /**
     * Indique si une transition directe de ce statut vers {@code cible} est
     * structurellement permise (indépendamment des pré-requis de contenu).
     *
     * @param cible statut visé
     * @return {@code true} si la transition est autorisée
     */
    public boolean peutAllerVers(StatutTournoi cible) {
        if (cible == null || cible == this) {
            return false;
        }
        // Pas de switch sur l'énumération (classe annexe générée par javac).
        if (this == BROUILLON) {
            return cible == DEMANDE_HOMOLOGATION || cible == HOMOLOGUE || cible == ANNULE;
        }
        if (this == DEMANDE_HOMOLOGATION) {
            return cible == HOMOLOGUE || cible == BROUILLON || cible == ANNULE;
        }
        if (this == HOMOLOGUE) {
            return cible == PUBLIE || cible == ANNULE;
        }
        if (this == PUBLIE) {
            return cible == ANNULE;
        }
        return false;
    }

    /** @return true si aucune transition n'est possible depuis ce statut. */
    public boolean estTerminal() {
        return this == ANNULE;
    }
}
