package com.pingmanager.model;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Règlement généré, indépendant de tout affichage : un titre, des articles
 * faits de blocs (paragraphes, tableaux, listes) et la liste de ce qui reste à
 * compléter. Les trous apparaissent dans le texte sous la forme
 * {@code [à compléter : …]} ({@link #A_COMPLETER}), que chaque rendu peut
 * surligner.
 *
 * @param titre       titre du règlement
 * @param sousTitre   dates et lieu
 * @param articles    articles numérotés
 * @param aCompleter  check-list « à faire avant le dépôt » (vide si le dossier est complet)
 */
public record DocumentReglement(String titre, String sousTitre, List<Article> articles, List<String> aCompleter) {

    /** Repère les trous à surligner dans un texte. */
    public static final Pattern A_COMPLETER = Pattern.compile("\\[à compléter[^\\]]*\\]");

    /** @return vrai si le texte du règlement contient encore des trous. */
    public boolean estProjet() {
        for (Article a : articles) {
            for (Bloc b : a.blocs()) {
                if (b.texteBrut().contains("[à compléter")) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Un article numéroté. */
    public record Article(int numero, String titre, List<Bloc> blocs) {
    }

    /** Un bloc de contenu. */
    public sealed interface Bloc permits Paragraphe, Grille, Liste {
        /** Texte concaténé (pour les recherches et les tests). */
        String texteBrut();
    }

    /** Un paragraphe de texte. */
    public record Paragraphe(String texte) implements Bloc {
        @Override
        public String texteBrut() {
            return texte;
        }
    }

    /** Un tableau : en-têtes, lignes, et une note facultative en dessous. */
    public record Grille(List<String> entetes, List<List<String>> lignes, String note) implements Bloc {
        @Override
        public String texteBrut() {
            StringBuilder sb = new StringBuilder(String.join(" ", entetes));
            lignes.forEach(l -> sb.append('\n').append(String.join(" ", l)));
            if (note != null) {
                sb.append('\n').append(note);
            }
            return sb.toString();
        }
    }

    /** Une liste à puces. */
    public record Liste(List<String> elements) implements Bloc {
        @Override
        public String texteBrut() {
            return String.join("\n", elements);
        }
    }
}
