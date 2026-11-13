package com.pingmanager.model.buvette;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalLong;

/**
 * Buvette d'un tournoi (B046) : stock des ingrédients (journal de mouvements,
 * seuils d'alerte), budget prévisionnel et ventes. Enregistrée à côté du fichier
 * du tournoi, jamais dans le catalogue du club.
 *
 * <p>Le journal est en ajout seul : aucun mouvement ne se supprime (traçabilité).</p>
 */
public final class BuvetteTournoi {

    private final List<MouvementStock> mouvements = new ArrayList<>();
    private final Map<String, Long> seuils = new LinkedHashMap<>();

    /** @return les mouvements, dans l'ordre de saisie (lecture seule). */
    public List<MouvementStock> getMouvements() {
        return Collections.unmodifiableList(mouvements);
    }

    /** Numéro du prochain mouvement. */
    public int prochainNumeroMouvement() {
        return mouvements.isEmpty() ? 1 : mouvements.get(mouvements.size() - 1).numero() + 1;
    }

    /** Ajoute un mouvement en fin de journal (numéro strictement croissant). */
    public void ajouterMouvement(MouvementStock m) {
        Objects.requireNonNull(m, "mouvement");
        if (m.numero() < prochainNumeroMouvement()) {
            throw new IllegalArgumentException("numéro de mouvement déjà utilisé : " + m.numero());
        }
        mouvements.add(m);
    }

    /** Seuil d'alerte d'un ingrédient, en portions (vide si aucun). */
    public OptionalLong seuil(String idIngredient) {
        Long s = seuils.get(idIngredient);
        return s == null ? OptionalLong.empty() : OptionalLong.of(s);
    }

    /** Fixe (seuil ≥ 0) ou retire (seuil négatif) le seuil d'alerte d'un ingrédient. */
    public void definirSeuil(String idIngredient, long seuil) {
        if (seuil < 0) {
            seuils.remove(idIngredient);
        } else {
            seuils.put(idIngredient, seuil);
        }
    }

    public Map<String, Long> getSeuils() {
        return Collections.unmodifiableMap(seuils);
    }

    /** @return vrai si rien n'a été saisi pour ce tournoi. */
    public boolean estVide() {
        return mouvements.isEmpty() && seuils.isEmpty();
    }
}
