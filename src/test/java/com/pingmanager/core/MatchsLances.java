package com.pingmanager.core;

import com.pingmanager.model.Match;
import com.pingmanager.model.StatutMatch;

import java.time.LocalDateTime;

/**
 * Aide aux tests : un match se joue sur une table, son score ne se saisit donc
 * qu'une fois lancé ({@link SaisieResultatMatch#scoreSaisissable}). Les tests
 * qui jouent des matchs pour préparer une situation les lancent d'abord, comme
 * la salle le ferait.
 */
public final class MatchsLances {

    private MatchsLances() {
    }

    /**
     * Lance un match à jouer (statut « en cours », heure de début posée, comme
     * l'ordonnanceur de la salle) et le renvoie ; un match d'un
     * autre statut (en attente, déjà en cours, terminé, exemption) est renvoyé tel quel.
     */
    public static Match lance(Match m) {
        if (m != null && m.getStatut() == StatutMatch.A_JOUER) {
            m.setStatut(StatutMatch.EN_COURS);
            m.setInstantLancement(LocalDateTime.of(2027, 4, 10, 9, 0));
        }
        return m;
    }
}
