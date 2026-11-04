package com.pingmanager.ui.ecran;

import com.pingmanager.core.reglement.DocumentReglement;
import com.pingmanager.core.reglement.DocumentReglement.Article;
import com.pingmanager.core.reglement.DocumentReglement.Bloc;
import com.pingmanager.core.reglement.DocumentReglement.Grille;
import com.pingmanager.core.reglement.DocumentReglement.Liste;
import com.pingmanager.core.reglement.DocumentReglement.Paragraphe;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.List;
import java.util.regex.Matcher;

/**
 * Rendu JavaFX d'un {@link DocumentReglement} : titre, articles, tableaux, et
 * passages « [à compléter : …] » surlignés. Même contenu que le PDF : ni bandeau
 * « projet », ni check-list « à faire avant le dépôt ».
 */
final class ApercuReglement {

    private ApercuReglement() {
    }

    static VBox rendre(DocumentReglement d) {
        VBox v = new VBox(6);
        v.getStyleClass().add("reglement");
        Label titre = new Label(d.titre());
        titre.getStyleClass().add("regl-titre");
        titre.setWrapText(true);
        Label sous = new Label(d.sousTitre());
        sous.getStyleClass().add("regl-sous-titre");
        v.getChildren().addAll(titre, sous);
        for (Article a : d.articles()) {
            Label t = new Label("Article " + a.numero() + " — " + a.titre());
            t.getStyleClass().add("regl-article");
            t.setWrapText(true);
            v.getChildren().add(t);
            for (Bloc b : a.blocs()) {
                v.getChildren().add(bloc(b));
            }
        }
        return v;
    }

    private static Node bloc(Bloc b) {
        if (b instanceof Paragraphe p) {
            return texte(p.texte(), "regl-texte");
        }
        if (b instanceof Grille g) {
            VBox v = new VBox(4, grille(g));
            if (g.note() != null) {
                v.getChildren().add(texte(g.note(), "regl-note"));
            }
            return v;
        }
        Liste l = (Liste) b;
        VBox v = new VBox(2);
        l.elements().forEach(e -> v.getChildren().add(texte("• " + e, "regl-texte")));
        return v;
    }

    /** Paragraphe dont les trous « [à compléter : …] » sont surlignés. */
    private static TextFlow texte(String s, String classe) {
        TextFlow f = new TextFlow();
        f.getStyleClass().add("regl-paragraphe");
        Matcher m = DocumentReglement.A_COMPLETER.matcher(s);
        int i = 0;
        while (m.find()) {
            if (m.start() > i) {
                f.getChildren().add(morceau(s.substring(i, m.start()), classe));
            }
            Label trou = new Label(m.group());
            trou.getStyleClass().add("regl-trou");
            f.getChildren().add(trou);
            i = m.end();
        }
        if (i < s.length()) {
            f.getChildren().add(morceau(s.substring(i), classe));
        }
        return f;
    }

    private static Text morceau(String s, String classe) {
        Text t = new Text(s);
        t.getStyleClass().add(classe);
        return t;
    }

    private static Node grille(Grille g) {
        GridPane p = new GridPane();
        p.getStyleClass().add("grille-tableaux");
        List<String> entetes = g.entetes();
        for (int c = 0; c < entetes.size(); c++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setHgrow(Priority.SOMETIMES);
            p.getColumnConstraints().add(cc);
            Label l = Ui.enteteColonne(entetes.get(c));
            l.getStyleClass().add("cellule-entete");
            l.setMinWidth(Region.USE_PREF_SIZE);
            p.add(l, c, 0);
        }
        int r = 1;
        p.add(filet(), 0, r++, entetes.size(), 1);
        for (List<String> ligne : g.lignes()) {
            for (int c = 0; c < ligne.size(); c++) {
                Node n = DocumentReglement.A_COMPLETER.matcher(ligne.get(c)).find()
                        ? texte(ligne.get(c), "regl-texte") : new Label(ligne.get(c));
                n.getStyleClass().add("cellule");
                if (c == 0) {
                    n.getStyleClass().add("gras");
                }
                if (n instanceof Region reg) {
                    reg.setMinWidth(Region.USE_PREF_SIZE);
                }
                GridPane.setValignment(n, VPos.TOP);
                p.add(n, c, r);
            }
            r++;
            p.add(filet(), 0, r++, entetes.size(), 1);
        }
        return p;
    }

    private static Region filet() {
        Region f = new Region();
        f.getStyleClass().add("filet-ligne");
        f.setMinHeight(1);
        f.setPrefHeight(1);
        f.setMaxHeight(1);
        return f;
    }
}
