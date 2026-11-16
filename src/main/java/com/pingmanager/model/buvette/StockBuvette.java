package com.pingmanager.model.buvette;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Stock de la buvette d'un club (B026) : ses articles (produits et contenants)
 * et le journal des derniers mouvements. Le stock appartient au club et le
 * suit d'un tournoi à l'autre.
 *
 * <p>Entité mutable gardée : les règles (nom en double, contenant encore
 * utilisé, sortie d'une portion) sont dans {@code core.buvette}, qui renvoie
 * des résultats détaillés.</p>
 *
 * <p>Le journal ne garde que les {@value #JOURNAL_MAX} dernières lignes, pour
 * que le fichier d'application ne grossisse pas sans fin.</p>
 */
public final class StockBuvette {

    /** Nombre de lignes du journal conservées. */
    public static final int JOURNAL_MAX = 500;

    private final String numeroClub;
    private final Map<String, ArticleBuvette> articles = new LinkedHashMap<>();
    private final List<MouvementStock> journal = new ArrayList<>();
    private int derniereOperation;

    public StockBuvette(String numeroClub) {
        if (numeroClub == null || numeroClub.isBlank()) {
            throw new IllegalArgumentException("numéro de club obligatoire");
        }
        this.numeroClub = numeroClub.trim();
    }

    public String getNumeroClub() {
        return numeroClub;
    }

    /** @return les articles, dans l'ordre d'ajout (lecture seule). */
    public List<ArticleBuvette> getArticles() {
        return List.copyOf(articles.values());
    }

    /** @return les articles d'une famille, dans l'ordre d'ajout. */
    public List<ArticleBuvette> articles(FamilleArticle famille) {
        List<ArticleBuvette> res = new ArrayList<>();
        for (ArticleBuvette a : articles.values()) {
            if (a.getFamille() == famille) {
                res.add(a);
            }
        }
        return res;
    }

    public Optional<ArticleBuvette> article(String id) {
        return Optional.ofNullable(id == null ? null : articles.get(id));
    }

    /** Ajoute ou remplace un article (même identifiant), à sa place d'origine. */
    public void enregistrer(ArticleBuvette a) {
        articles.put(Objects.requireNonNull(a, "article").getId(), a);
    }

    /** Retire un article (sans contrôle : passer par le service). */
    public void retirer(String id) {
        articles.remove(id);
    }

    /** @return les produits qui utilisent ce contenant. */
    public List<ArticleBuvette> produitsUtilisant(String idContenant) {
        List<ArticleBuvette> res = new ArrayList<>();
        for (ArticleBuvette a : articles.values()) {
            for (LienContenant l : a.getContenants()) {
                if (l.idContenant().equals(idContenant)) {
                    res.add(a);
                    break;
                }
            }
        }
        return res;
    }

    /** @return le journal, du plus ancien au plus récent (lecture seule). */
    public List<MouvementStock> getJournal() {
        return Collections.unmodifiableList(journal);
    }

    /** Numéro de la prochaine opération. */
    public int prochaineOperation() {
        return derniereOperation + 1;
    }

    /** Ajoute une ligne en fin de journal ; les plus anciennes au-delà de {@value #JOURNAL_MAX} sont oubliées. */
    public void journaliser(MouvementStock m) {
        Objects.requireNonNull(m, "mouvement");
        journal.add(m);
        derniereOperation = Math.max(derniereOperation, m.operation());
        while (journal.size() > JOURNAL_MAX) {
            journal.remove(0);
        }
    }

    /** @return vrai si rien n'a été saisi (le dépôt ne l'écrit alors pas). */
    public boolean estVide() {
        return articles.isEmpty() && journal.isEmpty();
    }

    /** Identifiant libre dérivé d'un nom (« Café » → {@code cafe}, puis {@code cafe-2}). */
    public String idLibre(String nom) {
        String base = slug(nom);
        String id = base;
        int i = 2;
        while (articles.containsKey(id)) {
            id = base + "-" + i++;
        }
        return id;
    }

    /** Forme normalisée d'un nom : minuscules sans accents, tirets. */
    public static String slug(String nom) {
        String s = Normalizer.normalize(nom == null ? "" : nom, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.FRENCH)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
        return s.isEmpty() ? "article" : s;
    }
}
