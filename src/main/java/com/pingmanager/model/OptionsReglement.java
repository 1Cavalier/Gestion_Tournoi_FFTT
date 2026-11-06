package com.pingmanager.model;

import java.util.List;

/**
 * Choix propres au règlement : son origine (déposé par le club ou généré) et
 * les articles facultatifs ajoutés au règlement généré (droit à l'image,
 * buvette, tirage au sort…). Les autres clauses viennent des sections du
 * dossier (tableaux, maintien, forfaits, paiement…).
 */
public final class OptionsReglement {

    /** D'où vient le règlement du tournoi. */
    public enum Mode {
        /** Le club dépose son propre fichier (PDF). */
        DEPOSE,
        /** Le règlement est généré à partir des informations saisies. */
        GENERE
    }

    private final Mode mode;
    private final List<ArticleOptionnel> articles;

    public OptionsReglement(Mode mode, List<ArticleOptionnel> articles) {
        this.mode = mode;
        this.articles = articles == null ? List.of() : List.copyOf(articles);
    }

    /** Par défaut : mode non choisi, article « droit à l'image » proposé. */
    public static OptionsReglement parDefaut() {
        return new OptionsReglement(null, List.of(ArticleOptionnel.de(ArticleOptionnel.Type.DROIT_IMAGE)));
    }

    public Mode getMode() {
        return mode;
    }

    public List<ArticleOptionnel> getArticles() {
        return articles;
    }

    public OptionsReglement avecArticles(List<ArticleOptionnel> a) {
        return new OptionsReglement(mode, a);
    }
}
