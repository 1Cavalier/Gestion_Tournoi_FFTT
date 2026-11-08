package com.pingmanager.ui.diffusion;

import com.pingmanager.ui.diffusion.InstantaneSuivi.JoueurPublic;
import com.pingmanager.ui.diffusion.InstantaneSuivi.LigneClassementPublique;
import com.pingmanager.ui.diffusion.InstantaneSuivi.LignePodium;
import com.pingmanager.ui.diffusion.InstantaneSuivi.MatchPublic;
import com.pingmanager.ui.diffusion.InstantaneSuivi.PoulePublique;
import com.pingmanager.ui.diffusion.InstantaneSuivi.ProchainePartie;
import com.pingmanager.ui.diffusion.InstantaneSuivi.TableauPublic;
import com.pingmanager.ui.diffusion.InstantaneSuivi.TourPublic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Pages HTML du suivi sur téléphone (B021), produites depuis un
 * {@link InstantaneSuivi} : aucune dépendance au serveur, donc testables
 * directement. Tout texte venant du tournoi passe par {@link Html#echapper}.
 * Pas de JavaScript : rafraîchissement par {@code <meta http-equiv="refresh">},
 * CSS en ligne aux couleurs de la charte v2 (thème sombre si le téléphone le
 * demande).
 *
 * <p>Routage en lecture seule : seules les méthodes GET et HEAD sont acceptées
 * (405 sinon).</p>
 */
public final class PagesSuivi {

    /** Intervalle du rafraîchissement automatique des pages, en secondes. */
    public static final int RAFRAICHISSEMENT_SECONDES = 30;
    /** Nombre maximal de joueurs proposés par une recherche. */
    public static final int MAX_RESULTATS = 20;

    public static final String TYPE_HTML = "text/html; charset=utf-8";
    public static final String TYPE_TEXTE = "text/plain; charset=utf-8";

    /** Réponse HTTP à envoyer : statut, type de contenu, corps. */
    public record Reponse(int statut, String typeContenu, String corps) {
    }

    private PagesSuivi() {
    }

    // ==================================================================
    // Routage
    // ==================================================================

    /**
     * Réponse à une requête. {@code chemin} est le chemin brut de l'URL,
     * {@code requeteBrute} la chaîne de requête brute (ou null).
     */
    public static Reponse repondre(String methode, String chemin, String requeteBrute, InstantaneSuivi instantane) {
        if (!"GET".equals(methode) && !"HEAD".equals(methode)) {
            return new Reponse(405, TYPE_TEXTE, "Méthode non autorisée : ce suivi est en lecture seule.");
        }
        if (requeteBrute != null && requeteBrute.length() > Html.LONGUEUR_MAX_REQUETE) {
            return new Reponse(414, TYPE_TEXTE, "Requête trop longue.");
        }
        String c = chemin == null ? "/" : chemin;
        if (instantane == null || (instantane.nomTournoi().isEmpty() && instantane.tableaux().isEmpty())) {
            if (c.equals("/") || c.equals("/matchs") || c.equals("/tableau") || c.equals("/partie")) {
                return new Reponse(503, TYPE_HTML, pageNonDisponible());
            }
            return new Reponse(404, TYPE_HTML, pageIntrouvable(null));
        }
        Map<String, String> p = Html.parametres(requeteBrute);
        if (c.equals("/")) {
            return new Reponse(200, TYPE_HTML, accueil(instantane));
        }
        if (c.equals("/matchs")) {
            return new Reponse(200, TYPE_HTML, matchs(instantane));
        }
        if (c.equals("/tableau")) {
            int n = entier(p.get("n"));
            if (n < 1 || n > instantane.tableaux().size()) {
                return new Reponse(404, TYPE_HTML, pageIntrouvable(instantane));
            }
            return new Reponse(200, TYPE_HTML, tableau(instantane, n));
        }
        if (c.equals("/partie")) {
            return new Reponse(200, TYPE_HTML, partie(instantane, p.get("q"), p.get("id")));
        }
        return new Reponse(404, TYPE_HTML, pageIntrouvable(instantane));
    }

    /** Entier positif lu dans un paramètre (au plus 4 chiffres), -1 sinon. */
    static int entier(String s) {
        if (s == null || s.isEmpty() || s.length() > 4) {
            return -1;
        }
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) < '0' || s.charAt(i) > '9') {
                return -1;
            }
        }
        return Integer.parseInt(s);
    }

    // ==================================================================
    // Pages
    // ==================================================================

    /** Accueil : accès rapides et liste des tableaux avec leur étape. */
    public static String accueil(InstantaneSuivi s) {
        StringBuilder b = new StringBuilder();
        b.append(formulaireRecherche(""));
        b.append("<section class=\"carte\"><h2>En ce moment</h2><p>")
                .append(s.enCours().size()).append(s.enCours().size() > 1 ? " parties en cours" : " partie en cours")
                .append(" · ").append(s.aVenir().size()).append(s.aVenir().size() > 1 ? " prêtes" : " prête")
                .append("</p><p><a class=\"bouton\" href=\"/matchs\">Matchs en cours et à venir</a></p></section>");
        b.append("<section class=\"carte\"><h2>Tableaux</h2>");
        if (s.tableaux().isEmpty()) {
            b.append("<p class=\"doux\">Aucun tableau.</p>");
        }
        b.append("<ul class=\"liste\">");
        for (int i = 0; i < s.tableaux().size(); i++) {
            TableauPublic t = s.tableaux().get(i);
            b.append("<li><a href=\"/tableau?n=").append(i + 1).append("\">").append(Html.echapper(t.nom()))
                    .append("</a><span class=\"doux\">").append(Html.echapper(t.etape())).append("</span></li>");
        }
        b.append("</ul></section>");
        return gabarit(s, "Suivi du tournoi", b.toString());
    }

    /** Matchs en cours (avec leur table) puis matchs prêts dans l'ordre d'envoi aux tables. */
    public static String matchs(InstantaneSuivi s) {
        StringBuilder b = new StringBuilder();
        b.append("<section class=\"carte\"><h2>En cours</h2>");
        if (s.enCours().isEmpty()) {
            b.append("<p class=\"doux\">Aucune partie en cours.</p>");
        }
        for (MatchPublic m : s.enCours()) {
            b.append(match(m, true));
        }
        b.append("</section><section class=\"carte\"><h2>À venir</h2>");
        if (s.aVenir().isEmpty()) {
            b.append("<p class=\"doux\">Aucune partie prête pour l'instant.</p>");
        } else {
            b.append("<p class=\"doux\">Dans l'ordre d'envoi sur les tables libres.</p>");
        }
        for (MatchPublic m : s.aVenir()) {
            b.append(match(m, true));
        }
        b.append("</section>");
        return gabarit(s, "Matchs en cours et à venir", b.toString());
    }

    /** Un tableau : poules (classement et matchs), tableau final, podium. {@code n} à partir de 1. */
    public static String tableau(InstantaneSuivi s, int n) {
        TableauPublic t = s.tableaux().get(n - 1);
        StringBuilder b = new StringBuilder();
        b.append("<section class=\"carte\"><h2>").append(Html.echapper(t.nom())).append("</h2><p><span class=\"pastille\">")
                .append(Html.echapper(t.etape())).append("</span></p>");
        if (!t.note().isEmpty()) {
            b.append("<p class=\"doux\">").append(Html.echapper(t.note())).append("</p>");
        }
        b.append("</section>");
        if (!t.podium().isEmpty()) {
            b.append("<section class=\"carte\"><h2>Podium</h2><ul class=\"liste\">");
            for (LignePodium l : t.podium()) {
                b.append("<li><strong>").append(Html.echapper(l.place())).append("</strong> ")
                        .append(Html.echapper(l.joueur())).append("<span class=\"doux\">")
                        .append(Html.echapper(l.detail())).append("</span></li>");
            }
            b.append("</ul></section>");
        }
        for (TourPublic tour : t.tours()) {
            b.append("<section class=\"carte\"><h2>").append(Html.echapper(tour.nom())).append("</h2>");
            for (MatchPublic m : tour.matchs()) {
                b.append(match(m, false));
            }
            b.append("</section>");
        }
        for (PoulePublique p : t.poules()) {
            b.append("<section class=\"carte\"><h2>").append(Html.echapper(p.titre())).append("</h2>");
            b.append("<table><thead><tr><th>Rang</th><th>Joueur</th><th>Bilan</th></tr></thead><tbody>");
            for (LigneClassementPublique l : p.classement()) {
                b.append("<tr").append(l.enPlaceQualificative() ? " class=\"qualifie\"" : "").append("><td>")
                        .append(Html.echapper(l.rang())).append("</td><td>").append(Html.echapper(l.joueur()))
                        .append("<br><span class=\"doux\">").append(Html.echapper(l.detail())).append("</span></td><td>")
                        .append(Html.echapper(l.bilan()));
                if (!l.qualification().isEmpty()) {
                    b.append("<br><span class=\"pastille\">").append(Html.echapper(l.qualification())).append("</span>");
                }
                b.append("</td></tr>");
            }
            b.append("</tbody></table>");
            for (MatchPublic m : p.matchs()) {
                b.append(match(m, false));
            }
            b.append("</section>");
        }
        return gabarit(s, t.nom(), b.toString());
    }

    /**
     * « Ma prochaine partie » : recherche par nom (sans accents, tous les mots)
     * ou par numéro de licence exact, puis fiche du joueur choisi.
     */
    public static String partie(InstantaneSuivi s, String q, String id) {
        StringBuilder b = new StringBuilder();
        int index = entier(id);
        if (index >= 0 && index < s.joueurs().size()) {
            b.append(formulaireRecherche(""));
            b.append(fiche(s.joueurs().get(index)));
            return gabarit(s, "Ma prochaine partie", b.toString());
        }
        String requete = q == null ? "" : q;
        b.append(formulaireRecherche(requete));
        if (requete.isBlank()) {
            b.append("<p class=\"doux\">Tapez votre nom ou votre numéro de licence.</p>");
            return gabarit(s, "Ma prochaine partie", b.toString());
        }
        List<Integer> trouves = rechercher(s, requete);
        if (trouves.isEmpty()) {
            b.append("<section class=\"carte\"><p>Aucun joueur inscrit ne correspond à « ")
                    .append(Html.echapper(requete)).append(" ».</p></section>");
        } else if (trouves.size() == 1) {
            b.append(fiche(s.joueurs().get(trouves.get(0))));
        } else {
            b.append("<section class=\"carte\"><h2>Plusieurs joueurs</h2><ul class=\"liste\">");
            for (int i = 0; i < Math.min(MAX_RESULTATS, trouves.size()); i++) {
                JoueurPublic j = s.joueurs().get(trouves.get(i));
                b.append("<li><a href=\"/partie?id=").append(trouves.get(i)).append("\">")
                        .append(Html.echapper(j.nom())).append("</a><span class=\"doux\">")
                        .append(Html.echapper(j.detail())).append("</span></li>");
            }
            b.append("</ul>");
            if (trouves.size() > MAX_RESULTATS) {
                b.append("<p class=\"doux\">Plus de ").append(MAX_RESULTATS)
                        .append(" joueurs : précisez la recherche.</p>");
            }
            b.append("</section>");
        }
        return gabarit(s, "Ma prochaine partie", b.toString());
    }

    /** Indices des joueurs correspondant à la recherche (licence exacte, ou tous les mots du nom). */
    static List<Integer> rechercher(InstantaneSuivi s, String requete) {
        List<Integer> res = new ArrayList<>();
        String brute = requete.strip();
        boolean licence = !brute.isEmpty() && brute.chars().allMatch(ch -> ch >= '0' && ch <= '9');
        String norm = InstantaneSuivi.normaliser(brute);
        if (norm.isEmpty()) {
            return res;
        }
        String[] mots = norm.split(" ");
        for (int i = 0; i < s.joueurs().size(); i++) {
            JoueurPublic j = s.joueurs().get(i);
            if (licence) {
                if (brute.equals(j.licence())) {
                    res.add(i);
                }
                continue;
            }
            String nom = " " + j.nomRecherche() + " ";
            boolean tous = true;
            for (String mot : mots) {
                if (!nom.contains(" " + mot)) {
                    tous = false;
                    break;
                }
            }
            if (tous) {
                res.add(i);
            }
        }
        return res;
    }

    private static String fiche(JoueurPublic j) {
        StringBuilder b = new StringBuilder();
        b.append("<section class=\"carte\"><h2>").append(Html.echapper(j.nom())).append("</h2><p class=\"doux\">")
                .append(Html.echapper(j.detail())).append("</p>");
        if (j.prochaine() == null) {
            b.append("<p>").append(Html.echapper(j.sansPartie())).append("</p></section>");
            return b.toString();
        }
        ProchainePartie p = j.prochaine();
        b.append("<div class=\"prochaine").append(p.enCours() ? " en-cours" : "").append("\"><p class=\"titre\">")
                .append(Html.echapper(p.titre())).append("</p><p class=\"table\">").append(Html.echapper(p.table()))
                .append("</p><p><strong>").append(Html.echapper(p.adversaire())).append("</strong></p><p>")
                .append(Html.echapper(p.detail())).append("</p><p class=\"doux\">").append(Html.echapper(p.quand()))
                .append("</p></div></section>");
        return b.toString();
    }

    private static String formulaireRecherche(String valeur) {
        return "<form class=\"carte recherche\" method=\"get\" action=\"/partie\"><label for=\"q\">Ma prochaine partie</label>"
                + "<input id=\"q\" name=\"q\" type=\"search\" maxlength=\"" + Html.LONGUEUR_MAX_PARAMETRE
                + "\" placeholder=\"Nom ou n° de licence\" value=\"" + Html.echapper(valeur) + "\">"
                + "<button type=\"submit\">Chercher</button></form>";
    }

    private static String match(MatchPublic m, boolean avecTableau) {
        StringBuilder b = new StringBuilder();
        b.append("<div class=\"match").append(m.enCours() ? " en-cours" : "").append("\"><div class=\"entete\">");
        if (!m.table().isEmpty()) {
            b.append("<span class=\"num-table\">").append(Html.echapper(m.table())).append("</span>");
        }
        b.append("<span class=\"doux\">");
        if (avecTableau) {
            b.append(Html.echapper(m.tableau())).append(" · ");
        }
        b.append(Html.echapper(m.situation())).append("</span><span class=\"resultat\">")
                .append(Html.echapper(m.resultat())).append("</span></div>");
        b.append(joueur(m.joueurA(), m.detailA(), m.vainqueurA()));
        b.append(joueur(m.joueurB(), m.detailB(), m.vainqueurB()));
        if (!m.detail().isEmpty()) {
            b.append("<div class=\"doux\">").append(Html.echapper(m.detail())).append("</div>");
        }
        b.append("</div>");
        return b.toString();
    }

    private static String joueur(String nom, String detail, boolean vainqueur) {
        return "<div class=\"joueur\">" + (vainqueur ? "<strong>" : "") + Html.echapper(nom) + (vainqueur ? "</strong>" : "")
                + (detail.isEmpty() ? "" : " <span class=\"doux\">" + Html.echapper(detail) + "</span>") + "</div>";
    }

    static String pageNonDisponible() {
        return gabaritNu("Suivi du tournoi", "<section class=\"carte\"><p>Le suivi n'est pas encore disponible. "
                + "Cette page se rafraîchit toute seule.</p></section>", true);
    }

    static String pageIntrouvable(InstantaneSuivi s) {
        String corps = "<section class=\"carte\"><p>Page introuvable.</p><p><a href=\"/\">Retour à l'accueil</a></p></section>";
        return s == null ? gabaritNu("Page introuvable", corps, false) : gabarit(s, "Page introuvable", corps);
    }

    // ==================================================================
    // Gabarit
    // ==================================================================

    private static String gabarit(InstantaneSuivi s, String titre, String corps) {
        String heure = s.prisLe().getHour() + " h " + String.format("%02d", s.prisLe().getMinute());
        String entete = "<header><p class=\"tournoi\">" + Html.echapper(s.nomTournoi()) + "</p><nav>"
                + "<a href=\"/\">Accueil</a><a href=\"/matchs\">Matchs</a><a href=\"/partie\">Ma partie</a></nav></header>";
        String pied = "<footer>Mis à jour à " + heure + " · actualisation automatique toutes les "
                + RAFRAICHISSEMENT_SECONDES + " s · consultation seule</footer>";
        return document(titre, entete + "<main><h1>" + Html.echapper(titre) + "</h1>" + corps + "</main>" + pied, true);
    }

    private static String gabaritNu(String titre, String corps, boolean rafraichir) {
        return document(titre, "<main><h1>" + Html.echapper(titre) + "</h1>" + corps + "</main>", rafraichir);
    }

    private static String document(String titre, String corps, boolean rafraichir) {
        return "<!DOCTYPE html><html lang=\"fr\"><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
                + (rafraichir ? "<meta http-equiv=\"refresh\" content=\"" + RAFRAICHISSEMENT_SECONDES + "\">" : "")
                + "<title>" + Html.echapper(titre) + "</title><style>" + CSS + "</style></head><body>" + corps
                + "</body></html>";
    }

    /** Charte v2 « blanc et bleu » (valeurs de la skill charte-graphique), clair puis sombre. */
    static final String CSS = ":root{--fond:#F5F7FB;--surface:#FFFFFF;--bordure:#DCE3F0;--texte:#0F1B3D;"
            + "--doux:#5B6785;--primaire:#1D3FCF;--primaire-doux:#E8EDFD;--sur-primaire:#FFFFFF;"
            + "--succes-doux:#E3F5EC;--succes-texte:#12714B}"
            + "@media (prefers-color-scheme:dark){:root{--fond:#0E1424;--surface:#161E33;--bordure:#2A3656;"
            + "--texte:#E7ECF7;--doux:#9AA6C3;--primaire:#5B7BFF;--primaire-doux:#1E2B57;--sur-primaire:#0B1020;"
            + "--succes-doux:#12352A;--succes-texte:#7FE0B5}}"
            + "*{box-sizing:border-box}body{margin:0;font-family:system-ui,-apple-system,'Segoe UI',Roboto,sans-serif;"
            + "background:var(--fond);color:var(--texte);font-size:16px;line-height:1.4}"
            + "header{background:var(--surface);border-bottom:1px solid var(--bordure);padding:10px 14px}"
            + ".tournoi{margin:0;font-weight:600}nav a{margin-right:14px;color:var(--primaire);text-decoration:none;"
            + "font-weight:600}main{padding:12px;max-width:720px;margin:0 auto}h1{font-size:22px;margin:6px 0 12px}"
            + "h2{font-size:17px;margin:0 0 8px}a{color:var(--primaire)}"
            + ".carte{background:var(--surface);border:1px solid var(--bordure);border-radius:12px;padding:12px;"
            + "margin-bottom:12px;display:block}.doux{color:var(--doux);font-size:14px}"
            + ".liste{list-style:none;margin:0;padding:0}.liste li{padding:8px 0;border-top:1px solid var(--bordure);"
            + "display:flex;flex-direction:column}.liste li:first-child{border-top:0}"
            + ".bouton,button{display:inline-block;background:var(--primaire);color:var(--sur-primaire);border:0;"
            + "border-radius:8px;padding:9px 14px;font-size:16px;font-weight:600;text-decoration:none}"
            + ".recherche label{display:block;font-weight:600;margin-bottom:6px}"
            + ".recherche input{width:100%;padding:9px;font-size:16px;border:1px solid var(--bordure);border-radius:8px;"
            + "background:var(--surface);color:var(--texte);margin-bottom:8px}"
            + ".pastille{display:inline-block;background:var(--primaire-doux);color:var(--primaire);border-radius:999px;"
            + "padding:2px 10px;font-size:13px;font-weight:600}"
            + ".match{border-top:1px solid var(--bordure);padding:8px 0}.match.en-cours{background:var(--primaire-doux);"
            + "border-radius:8px;padding:8px}.entete{display:flex;gap:8px;align-items:baseline}"
            + ".resultat{margin-left:auto;font-weight:600}.num-table{background:var(--primaire);color:var(--sur-primaire);"
            + "border-radius:6px;padding:1px 8px;font-weight:700}"
            + "table{width:100%;border-collapse:collapse;margin-bottom:8px}th,td{text-align:left;padding:6px 4px;"
            + "border-top:1px solid var(--bordure);vertical-align:top;font-size:15px}th{color:var(--doux);font-size:13px}"
            + "tr.qualifie td{background:var(--succes-doux)}tr.qualifie .pastille{background:var(--succes-doux);"
            + "color:var(--succes-texte)}"
            + ".prochaine{border:2px solid var(--primaire);border-radius:12px;padding:12px;margin-top:8px}"
            + ".prochaine .titre{margin:0;color:var(--doux);font-weight:600}"
            + ".prochaine .table{font-size:34px;font-weight:700;color:var(--primaire);margin:4px 0}"
            + "footer{color:var(--doux);font-size:13px;text-align:center;padding:12px 12px 24px}";
}
