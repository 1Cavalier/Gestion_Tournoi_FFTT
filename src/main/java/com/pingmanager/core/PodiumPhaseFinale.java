package com.pingmanager.core;

import com.pingmanager.model.Joueur;

import java.util.List;

/**
 * Podium d'un tableau à élimination directe, calculé par
 * {@link ServicePhaseFinale#podium}. Chaque place est renseignée dès que le
 * match qui la décide est joué ({@code null} / liste vide sinon).
 *
 * <p><b>Troisièmes.</b> Si une petite finale est jouée, son vainqueur est seul
 * 3e et son perdant 4e. Sans petite finale, les perdants des demi-finales sont
 * affichés <b>3es ex aequo</b> : les règlements fédéraux 2026-2027 ne disent
 * rien de ce cas pour les tournois (chapitre IX), choix provisoire noté dans
 * {@code QUESTIONS.md}.</p>
 *
 * @param premier          vainqueur de la finale
 * @param deuxieme         finaliste perdant
 * @param troisiemes       vainqueur de la petite finale, ou perdants des demi-finales
 * @param quatrieme        perdant de la petite finale ({@code null} sans petite finale)
 * @param avecPetiteFinale le tableau comporte une petite finale
 */
public record PodiumPhaseFinale(Joueur premier, Joueur deuxieme, List<Joueur> troisiemes,
                                Joueur quatrieme, boolean avecPetiteFinale) {

    public PodiumPhaseFinale {
        troisiemes = troisiemes == null ? List.of() : List.copyOf(troisiemes);
    }

    /** @return true si plusieurs joueurs partagent la 3e place (pas de petite finale). */
    public boolean troisiemesExAequo() {
        return !avecPetiteFinale && troisiemes.size() > 1;
    }

    /** @return true si au moins une place du podium est connue. */
    public boolean estCommence() {
        return premier != null || !troisiemes.isEmpty();
    }
}
