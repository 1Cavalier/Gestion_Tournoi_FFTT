package com.pingmanager.ui.ecran;

import com.pingmanager.core.CalendrierTournoi;
import com.pingmanager.core.CatalogueTournois;
import com.pingmanager.core.reglement.Formats;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Carte interactive des tournois : {@link WebView} + <b>Leaflet 1.9.4</b> embarqué dans
 * les ressources (aucun CDN : script et feuille de style sont injectés dans la page).
 * Fond de carte <b>Plan IGN v2</b> (Géoplateforme, sans clé, licence ouverte) avec repli
 * automatique sur OpenStreetMap ; en thème sombre, le plan est inversé par un filtre CSS.
 * Sans réseau, un bandeau le signale et les marqueurs restent affichés.
 *
 * <p>Pont Java ↔ JavaScript : Java appelle {@code majMarqueurs}, {@code selectionner}
 * et {@code initialiser} ; la page appelle {@link Pont#ouvrir(String)} au clic sur un
 * marqueur et {@link Pont#survol(String)} au survol.</p>
 */
final class CarteTournois {

    private final WebView vue = new WebView();
    private final WebEngine moteur = vue.getEngine();
    private final StackPane racine = new StackPane(vue);
    /** Référence forte : le pont serait sinon ramassé par le GC et les clics perdus. */
    private final Pont pont;
    private boolean prete;
    private List<CatalogueTournois.Entree> enAttente;
    private String selectionEnAttente;

    /** Objet exposé à la page sous le nom {@code java} (méthodes publiques uniquement). */
    public static final class Pont {
        private final Consumer<String> onOuvrir;
        private final Consumer<String> onSurvol;

        Pont(Consumer<String> onOuvrir, Consumer<String> onSurvol) {
            this.onOuvrir = onOuvrir;
            this.onSurvol = onSurvol;
        }

        public void ouvrir(String id) {
            Platform.runLater(() -> onOuvrir.accept(id));
        }

        public void survol(String id) {
            Platform.runLater(() -> onSurvol.accept(id));
        }
    }

    CarteTournois(Consumer<String> onOuvrir, Consumer<String> onSurvol) {
        this.pont = new Pont(onOuvrir, onSurvol);
        racine.getStyleClass().add("carte-tournois");
        vue.setContextMenuEnabled(false);
        // Identification de l'application auprès des serveurs de tuiles (politique d'usage OSM).
        moteur.setUserAgent(moteur.getUserAgent() + " PingManager/0.1 (gestion de tournois FFTT)");
        moteur.getLoadWorker().stateProperty().addListener((o, a, etat) -> {
            if (etat == Worker.State.SUCCEEDED) {
                JSObject fenetre = (JSObject) moteur.executeScript("window");
                fenetre.setMember("java", pont);
                appliquerTheme();
                prete = true;
                redimensionner();
                if (enAttente != null) {
                    afficher(enAttente, true);
                }
                if (selectionEnAttente != null) {
                    selectionner(selectionEnAttente, false);
                }
            }
        });
        moteur.loadContent(page(), "text/html");
        // Bascule clair / sombre : la classe du root de la scène change → fond et couleurs suivent.
        javafx.collections.ListChangeListener<String> theme = c -> {
            if (prete) {
                appliquerTheme();
            }
        };
        racine.sceneProperty().addListener((o, ancienne, nouvelle) -> {
            if (ancienne != null && ancienne.getRoot() != null) {
                ancienne.getRoot().getStyleClass().removeListener(theme);
            }
            if (nouvelle != null && nouvelle.getRoot() != null) {
                nouvelle.getRoot().getStyleClass().addListener(theme);
            }
        });
        racine.widthProperty().addListener((o, a, b) -> redimensionner());
        racine.heightProperty().addListener((o, a, b) -> redimensionner());
    }

    StackPane noeud() {
        return racine;
    }

    /**
     * Couleurs reprises des variables de la charte v2 (clair / sombre) : la carte
     * est une page web (Leaflet) qui ne lit pas theme.css. À garder alignées sur
     * -primaire, -dep-optionnel, -surface / -sur-primaire et -dep-publication.
     */
    void appliquerTheme() {
        boolean sombre = Theme.estSombre();
        String couleurs = sombre
                ? "{primaire:'#6B88FF', grise:'#56648A', contour:'#0B1020', accent:'#55D5E0'}"
                : "{primaire:'#1D3FCF', grise:'#B8C4DC', contour:'#FFFFFF', accent:'#1597AB'}";
        executer("initialiser(" + sombre + ", " + couleurs + ")");
    }

    /** Remplace les marqueurs par ceux des entrées (résultat filtré). */
    void afficher(List<CatalogueTournois.Entree> entrees, boolean ajuster) {
        if (!prete) {
            enAttente = entrees;
            return;
        }
        StringBuilder sb = new StringBuilder("[");
        for (CatalogueTournois.Entree e : entrees) {
            if (e.position() == null) {
                continue;
            }
            if (sb.length() > 1) {
                sb.append(',');
            }
            sb.append("{id:").append(js(e.id()))
                    .append(",lat:").append(String.format(Locale.ROOT, "%.6f", e.position().latitude()))
                    .append(",lon:").append(String.format(Locale.ROOT, "%.6f", e.position().longitude()))
                    .append(",nom:").append(js(e.tournoi().getNom()))
                    .append(",sous:").append(js(sousTitre(e)))
                    .append(",acces:").append(!e.acces().estRefuse())
                    .append(",motif:").append(e.acces().estRefuse() ? js(e.acces().motif()) : "null")
                    .append(",approx:").append(e.positionApprochee())
                    .append(",enCours:").append(e.moment() == CalendrierTournoi.Moment.EN_COURS)
                    .append('}');
        }
        sb.append(']');
        executer("majMarqueurs(" + sb + ", " + ajuster + ")");
    }

    void selectionner(String id, boolean centrer) {
        if (!prete) {
            selectionEnAttente = id;
            return;
        }
        executer("selectionner(" + js(id) + ", " + centrer + ")");
    }

    private void redimensionner() {
        if (prete) {
            executer("redimensionner()");
        }
    }

    static String sousTitre(CatalogueTournois.Entree e) {
        String date = BlocsInscriptions.periode(e.tournoi());
        String lieu = e.tournoi().getAdresse() == null ? "" : e.tournoi().getAdresse().getVille();
        String niveau = e.tournoi().getNiveau() == null ? "" : e.tournoi().getNiveau().getLibelle();
        return niveau + " · " + date + (lieu == null || lieu.isEmpty() ? "" : " · " + lieu);
    }

    /** Pour le harnais de captures : exécute un script dans la page. */
    void script(String js) {
        executer(js);
    }

    private void executer(String script) {
        try {
            moteur.executeScript(script);
        } catch (RuntimeException e) {
            System.err.println("Carte : " + e.getMessage());
        }
    }

    /** Chaîne JavaScript entre apostrophes, échappée. */
    static String js(String s) {
        if (s == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder("'");
        for (char c : s.toCharArray()) {
            if (c == '\'' || c == '\\') {
                sb.append('\\').append(c);
            } else if (c == '\n') {
                sb.append("\\n");
            } else if (c == '\r') {
                continue;
            } else if (c == '<') {
                sb.append("\\x3c");
            } else {
                sb.append(c);
            }
        }
        return sb.append('\'').toString();
    }

    /** Page HTML avec Leaflet injecté (script et style lus dans les ressources). */
    private static String page() {
        return lire("carte.html")
                .replace("/*LEAFLET_CSS*/", lire("leaflet.css"))
                .replace("/*LEAFLET_JS*/", lire("leaflet.js"));
    }

    private static String lire(String nom) {
        try (InputStream in = CarteTournois.class.getResourceAsStream("/com/pingmanager/ui/carte/" + nom)) {
            if (in == null) {
                throw new IllegalStateException("Ressource de carte introuvable : " + nom);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
