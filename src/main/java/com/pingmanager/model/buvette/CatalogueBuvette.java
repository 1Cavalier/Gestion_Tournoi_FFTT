package com.pingmanager.model.buvette;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Catalogue de base de la buvette (B026) : liste immuable d'articles
 * courants, rangés par rayon, pour qu'un club qui démarre ait une idée de ce
 * qu'il peut vendre. Lu par {@code core.buvette.CatalogueBase}.
 */
public final class CatalogueBuvette {

    private final Map<String, ArticleCatalogue> articles = new LinkedHashMap<>();

    /**
     * @throws IllegalArgumentException si un identifiant est en double ou si une
     *                                  composition cite un article absent ou qui n'est pas un contenant
     */
    public CatalogueBuvette(List<ArticleCatalogue> liste) {
        for (ArticleCatalogue a : liste) {
            if (articles.putIfAbsent(a.id(), a) != null) {
                throw new IllegalArgumentException("article du catalogue en double : " + a.id());
            }
        }
        for (ArticleCatalogue a : articles.values()) {
            for (LienContenant l : a.composition()) {
                ArticleCatalogue c = articles.get(l.idContenant());
                if (c == null || c.estProduit()) {
                    throw new IllegalArgumentException(a.id() + " : contenant inconnu " + l.idContenant());
                }
            }
        }
    }

    /** @return tous les articles, dans l'ordre du fichier. */
    public List<ArticleCatalogue> getArticles() {
        return List.copyOf(articles.values());
    }

    public Optional<ArticleCatalogue> article(String id) {
        return Optional.ofNullable(id == null ? null : articles.get(id));
    }

    /** @return les articles d'un rayon, dans l'ordre du fichier. */
    public List<ArticleCatalogue> rayon(RayonBuvette r) {
        List<ArticleCatalogue> res = new ArrayList<>();
        for (ArticleCatalogue a : articles.values()) {
            if (a.rayon() == r) {
                res.add(a);
            }
        }
        return res;
    }

    public int taille() {
        return articles.size();
    }
}
