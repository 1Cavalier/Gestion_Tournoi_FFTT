package com.pingmanager.model;

/**
 * Cycle de vie d'un {@link Tableau}, le long d'une progression linéaire :
 *
 * <pre>
 *   INSCRIPTION ──ouvrirPointage()──▶ POINTAGE ──lancer()──▶ LANCE
 * </pre>
 *
 * <ul>
 *   <li>{@link #INSCRIPTION} : le tableau accepte les inscriptions (avant le jour
 *       J). L'ouverture et la fermeture globales de la période d'inscription
 *       relèveront du cycle de vie du <i>tournoi</i> (homologation, publication,
 *       dates) — non traité ici ; ce statut décrit seulement l'étape côté tableau.</li>
 *   <li>{@link #POINTAGE} : le jour J. Les joueurs pointent et paient ; les
 *       désistements et repêchages ont lieu. Le juge-arbitre ouvre le pointage
 *       tableau par tableau ({@link Tableau#ouvrirPointage()}).</li>
 *   <li>{@link #LANCE} : le tirage au sort a été effectué et les poules générées ;
 *       la phase de poules se déroule.</li>
 *   <li>{@link #PHASE_FINALE} : toutes les poules sont terminées, les qualifiés ont
 *       été placés dans le tableau à élimination directe (tirage I.304/I.305) ; la
 *       phase finale se déroule jusqu'à la finale.</li>
 * </ul>
 *
 * <p>Le fait que le pointage ait été <i>clôturé</i> (présents figés) n'est pas un
 * état distinct : c'est un fait ({@link Tableau#pointageEstCloture()}) vérifié
 * comme pré-condition du lancement. Un tableau reste en {@link #POINTAGE} entre la
 * clôture du pointage et le lancement.</p>
 */
public enum StatutTableau {
    INSCRIPTION,
    POINTAGE,
    LANCE,
    PHASE_FINALE
}
