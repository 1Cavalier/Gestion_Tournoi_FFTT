package com.pingmanager.core.buvette;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Commande en cours au comptoir de la buvette (B026) : chaque appui sur le
 * bouton d'un article en ajoute une portion. Rien n'est enregistré tant que
 * la vente n'est pas validée ({@link ServiceVentesBuvette#vendre}).
 */
public final class CommandeComptoir {

    /** Portions maximales d'un même article dans une commande. */
    public static final int QUANTITE_MAX = 999;

    private final Map<String, Integer> quantites = new LinkedHashMap<>();

    /** Ajoute une portion de l'article (jusqu'à {@value #QUANTITE_MAX}). */
    public void ajouter(String idArticle) {
        quantites.merge(idArticle, 1, (a, b) -> Math.min(QUANTITE_MAX, a + b));
    }

    /** Retire une portion de l'article (la ligne disparaît à 0). */
    public void retirerUn(String idArticle) {
        Integer q = quantites.get(idArticle);
        if (q == null) {
            return;
        }
        if (q <= 1) {
            quantites.remove(idArticle);
        } else {
            quantites.put(idArticle, q - 1);
        }
    }

    public void vider() {
        quantites.clear();
    }

    public boolean estVide() {
        return quantites.isEmpty();
    }

    public int quantite(String idArticle) {
        return quantites.getOrDefault(idArticle, 0);
    }

    /** @return article → portions, dans l'ordre des appuis (copie). */
    public Map<String, Integer> quantites() {
        return new LinkedHashMap<>(quantites);
    }
}
