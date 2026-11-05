package com.pingmanager.core.reglement;

import com.pingmanager.model.DocumentReglement;

import com.pingmanager.model.DocumentReglement.Article;
import com.pingmanager.model.DocumentReglement.Bloc;
import com.pingmanager.model.DocumentReglement.Grille;
import com.pingmanager.model.DocumentReglement.Liste;
import com.pingmanager.model.DocumentReglement.Paragraphe;

import java.util.List;
import java.util.regex.Matcher;

/**
 * Rendu HTML autonome du règlement (une seule page, styles inclus), lisible à
 * l'écran et prêt à imprimer en PDF depuis un navigateur (format A4). Les trous
 * {@code [à compléter : …]} sont surlignés ; la check-list est clairement
 * marquée « à retirer de la version publiée ».
 */
public final class RenduHtmlReglement {

    private RenduHtmlReglement() {
    }

    public static String rendre(DocumentReglement d) {
        StringBuilder sb = new StringBuilder(8192);
        sb.append("<!doctype html>\n<html lang=\"fr\">\n<head>\n<meta charset=\"utf-8\">\n")
                .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n")
                .append("<title>").append(esc(d.titre())).append("</title>\n<style>\n").append(CSS).append("</style>\n</head>\n<body>\n<main>\n");
        sb.append("<h1>").append(esc(d.titre())).append("</h1>\n");
        sb.append("<p class=\"sous-titre\">").append(esc(d.sousTitre())).append("</p>\n");
        if (d.estProjet()) {
            sb.append("<p class=\"projet\">Projet de règlement : les passages surlignés restent à compléter "
                    + "avant le dépôt de la demande d'homologation.</p>\n");
        }
        for (Article a : d.articles()) {
            sb.append("<section>\n<h2>Article ").append(a.numero()).append(" — ").append(esc(a.titre())).append("</h2>\n");
            for (Bloc b : a.blocs()) {
                rendreBloc(sb, b);
            }
            sb.append("</section>\n");
        }
        if (!d.aCompleter().isEmpty()) {
            sb.append("<aside class=\"check\">\n<h2>À faire avant le dépôt de la demande d'homologation</h2>\n")
                    .append("<p class=\"retirer\">Section de travail, à retirer de la version publiée.</p>\n<ul>\n");
            for (String item : d.aCompleter()) {
                sb.append("<li>").append(esc(item)).append("</li>\n");
            }
            sb.append("</ul>\n</aside>\n");
        }
        sb.append("</main>\n</body>\n</html>\n");
        return sb.toString();
    }

    private static void rendreBloc(StringBuilder sb, Bloc b) {
        if (b instanceof Paragraphe p) {
            sb.append("<p>").append(avecTrous(p.texte())).append("</p>\n");
        } else if (b instanceof Grille g) {
            sb.append("<div class=\"grille\"><table>\n<thead><tr>");
            for (String e : g.entetes()) {
                sb.append("<th>").append(esc(e)).append("</th>");
            }
            sb.append("</tr></thead>\n<tbody>\n");
            for (List<String> l : g.lignes()) {
                sb.append("<tr>");
                for (String c : l) {
                    sb.append("<td>").append(avecTrous(c)).append("</td>");
                }
                sb.append("</tr>\n");
            }
            sb.append("</tbody></table></div>\n");
            if (g.note() != null) {
                sb.append("<p class=\"note\">").append(avecTrous(g.note())).append("</p>\n");
            }
        } else if (b instanceof Liste l) {
            sb.append("<ul>");
            l.elements().forEach(e -> sb.append("<li>").append(avecTrous(e)).append("</li>"));
            sb.append("</ul>\n");
        }
    }

    /** Échappe le texte et surligne les trous « [à compléter : …] ». */
    static String avecTrous(String texte) {
        StringBuilder sb = new StringBuilder();
        Matcher m = DocumentReglement.A_COMPLETER.matcher(texte);
        int i = 0;
        while (m.find()) {
            sb.append(esc(texte.substring(i, m.start())));
            sb.append("<mark>").append(esc(m.group())).append("</mark>");
            i = m.end();
        }
        sb.append(esc(texte.substring(i)));
        return sb.toString();
    }

    static String esc(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static final String CSS = """
            :root { --texte: #2F4558; --doux: #5C6E7D; --filet: #D4DEE7; --fond-tete: #EEF3F8; --primaire: #335F8A; --trou: #FCE7B6; }
            * { box-sizing: border-box; }
            body { margin: 0; background: #F5F8FB; color: var(--texte);
                   font: 15px/1.55 "Segoe UI", Inter, system-ui, sans-serif; }
            main { max-width: 800px; margin: 32px auto; background: #fff; padding: 48px 56px;
                   border: 1px solid var(--filet); border-radius: 10px; }
            h1 { font-size: 26px; line-height: 1.2; margin: 0 0 6px; }
            .sous-titre { color: var(--doux); margin: 0 0 24px; font-size: 16px; }
            h2 { font-size: 16px; margin: 26px 0 8px; color: var(--primaire); }
            p { margin: 0 0 10px; max-width: 72ch; }
            .projet { background: var(--trou); padding: 10px 14px; border-radius: 6px; max-width: none; }
            mark { background: var(--trou); color: inherit; padding: 0 3px; border-radius: 3px; }
            .grille { overflow-x: auto; margin: 6px 0 10px; }
            table { border-collapse: collapse; width: 100%; font-size: 14px; }
            th, td { text-align: left; padding: 6px 10px 6px 0; border-bottom: 1px solid var(--filet); vertical-align: top; }
            th { color: var(--doux); font-weight: 600; white-space: nowrap; }
            td:first-child { font-weight: 600; }
            .note { color: var(--doux); font-size: 13.5px; }
            .check { margin-top: 36px; border: 2px dashed var(--filet); border-radius: 8px; padding: 16px 20px; }
            .check h2 { margin-top: 0; }
            .retirer { color: var(--doux); font-size: 13.5px; }
            .check li { margin-bottom: 4px; }
            @media (max-width: 640px) { main { margin: 0; padding: 24px 16px; border: 0; border-radius: 0; } }
            @media print {
              @page { size: A4; margin: 16mm 15mm; }
              body { background: #fff; font-size: 11pt; }
              main { margin: 0; padding: 0; border: 0; max-width: none; }
              section, table { break-inside: avoid; }
              .check { break-before: page; }
            }
            """;
}
