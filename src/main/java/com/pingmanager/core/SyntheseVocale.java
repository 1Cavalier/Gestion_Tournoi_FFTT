package com.pingmanager.core;

import java.util.List;
import java.util.Objects;

/**
 * Port : synthèse vocale du système (B017). Reçoit un document SSML déjà
 * échappé ({@link GenerateurSsml}) et le dit, en bloquant jusqu'à la fin (ou
 * jusqu'à une durée limite fixée par l'implémentation).
 *
 * <p>Une implémentation ne doit jamais placer le texte de l'annonce (noms des
 * joueurs) dans une ligne de commande.</p>
 */
public interface SyntheseVocale {

    /** Aucune synthèse : l'annonce est affichée à l'écran (repli silencieux). */
    SyntheseVocale AUCUNE = new SyntheseVocale() {
        @Override
        public boolean estDisponible() {
            return false;
        }

        @Override
        public ResultatSynthese dire(String ssml, ParametresVoix voix) {
            return ResultatSynthese.echec("synthèse vocale indisponible sur cet ordinateur");
        }
    };

    /** La voix peut-elle parler sur cet ordinateur ? */
    boolean estDisponible();

    /** Dit le document SSML ; ne lève pas d'exception, rend un échec avec motif. */
    ResultatSynthese dire(String ssml, ParametresVoix voix);

    /** Noms des voix installées (vide si inconnu). */
    default List<String> voix() {
        return List.of();
    }

    /**
     * Voix et débit choisis par le club.
     *
     * @param voix  nom de la voix du système, ou null pour celle par défaut
     * @param debit de -10 (lent) à 10 (rapide), 0 = normal
     */
    record ParametresVoix(String voix, int debit) {
        public static final ParametresVoix PAR_DEFAUT = new ParametresVoix(null, 0);
    }

    /**
     * Issue d'une synthèse.
     *
     * @param dite  l'annonce a-t-elle été dite ?
     * @param motif pourquoi elle ne l'a pas été (vide si dite)
     */
    record ResultatSynthese(boolean dite, String motif) {
        public ResultatSynthese {
            Objects.requireNonNull(motif, "motif");
        }

        public static ResultatSynthese ok() {
            return new ResultatSynthese(true, "");
        }

        public static ResultatSynthese echec(String motif) {
            return new ResultatSynthese(false, motif == null || motif.isBlank() ? "erreur inconnue" : motif);
        }
    }
}
