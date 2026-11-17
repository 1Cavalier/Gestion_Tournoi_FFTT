package com.pingmanager.model.buvette;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Stock de la buvette d'un club (B026) : ses articles (produits et contenants)
 * et le journal des derniers mouvements. Le stock appartient au club et le
 * suit d'un tournoi à l'autre.
 *
 * <p>Entité mutable gardée : les règles (nom en double, contenant encore
 * utilisé, sortie d'une portion) sont dans {@code core.buvette}, qui renvoie
 * des résultats détaillés.</p>
 *
 * <p>Les articles sont ceux <b>du club</b>, réutilisés d'un tournoi à l'autre,
 * comme le stock (un placard de gobelets ne se vide pas entre deux tournois).
 * Le réglage et les ventes de chaque tournoi sont rangés à part
 * ({@link BuvetteTournoi}), par tournoi.</p>
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
    private final Map<String, BuvetteTournoi> tournois = new LinkedHashMap<>();
    private final Set<String> aRecompter = new LinkedHashSet<>();

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

    /** @return les articles non archivés d'une famille, dans l'ordre d'ajout. */
    public List<ArticleBuvette> articlesActifs(FamilleArticle famille) {
        List<ArticleBuvette> res = new ArrayList<>();
        for (ArticleBuvette a : articles(famille)) {
            if (!a.estArchive()) {
                res.add(a);
            }
        }
        return res;
    }

    /** @return les articles archivés (retirés de la vente), dans l'ordre d'ajout. */
    public List<ArticleBuvette> articlesArchives() {
        List<ArticleBuvette> res = new ArrayList<>();
        for (ArticleBuvette a : articles.values()) {
            if (a.estArchive()) {
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
        aRecompter.remove(id);
    }

    /** @return la buvette d'un tournoi, si elle a déjà été réglée ou a des ventes. */
    public Optional<BuvetteTournoi> tournoi(String reference) {
        return Optional.ofNullable(reference == null ? null : tournois.get(reference.trim()));
    }

    /** @return la buvette d'un tournoi, créée (mode « stock seulement ») au premier appel ; le nom est mis à jour. */
    public BuvetteTournoi tournoiOuCree(String reference, String nom) {
        BuvetteTournoi t = tournois.computeIfAbsent(reference.trim(), r -> new BuvetteTournoi(r, nom));
        if (nom != null && !nom.isBlank()) {
            t.renommer(nom);
        }
        return t;
    }

    /** Ajoute une buvette de tournoi relue (remplace celle de même référence). */
    public void enregistrer(BuvetteTournoi t) {
        tournois.put(Objects.requireNonNull(t, "tournoi").getReferenceTournoi(), t);
    }

    /** @return les buvettes de tournoi, dans l'ordre de création (lecture seule). */
    public List<BuvetteTournoi> getTournois() {
        return List.copyOf(tournois.values());
    }

    /** Signale qu'un article est à recompter (stock manquant à une sortie ou une vente). */
    public void marquerARecompter(String id) {
        if (id != null && articles.containsKey(id)) {
            aRecompter.add(id);
        }
    }

    /** Retire le signalement « à recompter » (après un recomptage). */
    public void recompte(String id) {
        aRecompter.remove(id);
    }

    public boolean estARecompter(String id) {
        return aRecompter.contains(id);
    }

    /** @return les identifiants des articles à recompter, dans l'ordre du signalement. */
    public List<String> getARecompter() {
        return List.copyOf(aRecompter);
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
        if (!articles.isEmpty() || !journal.isEmpty()) {
            return false;
        }
        for (BuvetteTournoi t : tournois.values()) {
            if (!t.estVide()) {
                return false;
            }
        }
        return true;
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
