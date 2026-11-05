package com.pingmanager.persistence;

import com.pingmanager.model.DocumentReglement;
import com.pingmanager.model.DocumentReglement.Article;
import com.pingmanager.model.DocumentReglement.Bloc;
import com.pingmanager.model.DocumentReglement.Grille;
import com.pingmanager.model.DocumentReglement.Liste;
import com.pingmanager.model.DocumentReglement.Paragraphe;
import com.pingmanager.persistence.json.Json;

import java.util.ArrayList;
import java.util.List;

/**
 * Conversion JSON d'un {@link DocumentReglement} : copies figées du règlement
 * et de la feuille d'engagement d'un tournoi publié (champs facultatifs
 * {@code reglementFige} et {@code feuilleEngagementFigee}).
 *
 * <p>Forme : {@code {titre, sousTitre, articles: [{numero, titre, blocs: [...]}],
 * aCompleter: [...]}}, chaque bloc portant un {@code type} :
 * {@code paragraphe} (texte), {@code grille} (entetes, lignes, note) ou
 * {@code liste} (elements). Un bloc de type inconnu (version future) est ignoré.</p>
 */
final class DocumentReglementMapper {

    private DocumentReglementMapper() {
    }

    static Json ecrire(DocumentReglement d) {
        if (d == null) {
            return Json.nul();
        }
        Json o = Json.objet();
        o.set("titre", d.titre());
        o.set("sousTitre", d.sousTitre());
        Json articles = Json.tableau();
        for (Article a : d.articles()) {
            Json ja = Json.objet();
            ja.set("numero", a.numero());
            ja.set("titre", a.titre());
            Json blocs = Json.tableau();
            a.blocs().forEach(b -> blocs.add(ecrireBloc(b)));
            ja.set("blocs", blocs);
            articles.add(ja);
        }
        o.set("articles", articles);
        o.set("aCompleter", chaines(d.aCompleter()));
        return o;
    }

    private static Json ecrireBloc(Bloc b) {
        Json o = Json.objet();
        if (b instanceof Paragraphe p) {
            o.set("type", "paragraphe");
            o.set("texte", p.texte());
        } else if (b instanceof Grille g) {
            o.set("type", "grille");
            o.set("entetes", chaines(g.entetes()));
            Json lignes = Json.tableau();
            g.lignes().forEach(l -> lignes.add(chaines(l)));
            o.set("lignes", lignes);
            o.set("note", g.note());
        } else if (b instanceof Liste l) {
            o.set("type", "liste");
            o.set("elements", chaines(l.elements()));
        }
        return o;
    }

    /** @return le document, ou {@code null} si le champ est absent (ancien fichier). */
    static DocumentReglement lire(Json o) {
        if (o == null || o.estNul()) {
            return null;
        }
        List<Article> articles = new ArrayList<>();
        for (Json ja : elements(o.get("articles"))) {
            List<Bloc> blocs = new ArrayList<>();
            for (Json jb : elements(ja.get("blocs"))) {
                Bloc b = lireBloc(jb);
                if (b != null) {
                    blocs.add(b);
                }
            }
            articles.add(new Article(ja.getInt("numero", 0), ja.getString("titre", ""), List.copyOf(blocs)));
        }
        return new DocumentReglement(o.getString("titre", ""), o.getString("sousTitre", ""),
                List.copyOf(articles), lireChaines(o.get("aCompleter")));
    }

    private static Bloc lireBloc(Json o) {
        String type = o.getString("type", "");
        if ("paragraphe".equals(type)) {
            return new Paragraphe(o.getString("texte", ""));
        }
        if ("grille".equals(type)) {
            List<List<String>> lignes = new ArrayList<>();
            for (Json l : elements(o.get("lignes"))) {
                lignes.add(lireChaines(l));
            }
            return new Grille(lireChaines(o.get("entetes")), List.copyOf(lignes), o.getString("note", null));
        }
        if ("liste".equals(type)) {
            return new Liste(lireChaines(o.get("elements")));
        }
        return null;
    }

    private static Json chaines(List<String> valeurs) {
        Json t = Json.tableau();
        valeurs.forEach(v -> t.add(Json.of(v)));
        return t;
    }

    private static List<String> lireChaines(Json t) {
        List<String> l = new ArrayList<>();
        for (Json v : elements(t)) {
            l.add(v.estNul() ? "" : v.asString());
        }
        return List.copyOf(l);
    }

    /** @return les éléments d'un tableau JSON, vide si le nœud est absent ou d'un autre type. */
    private static List<Json> elements(Json t) {
        return t != null && t.estTableau() ? t.elements() : List.of();
    }
}
