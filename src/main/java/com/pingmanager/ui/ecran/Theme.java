package com.pingmanager.ui.ecran;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.text.Font;

import java.net.URL;
import java.util.List;

/**
 * Thème visuel de l'application : une seule feuille {@code theme.css}, deux
 * variantes (clair / sombre) portées par une classe sur le root de la scène.
 *
 * <p>{@link #installer(Scene)} se fait une fois au démarrage ; ensuite, chaque
 * {@code scene.setRoot(...)} reçoit automatiquement la bonne classe. La police
 * est choisie dans la pile système de la charte (Segoe UI, puis Inter, sinon
 * la police système de JavaFX) : c'est le seul style posé depuis Java, car
 * une feuille JavaFX ne sait pas exprimer une pile de polices.</p>
 */
public final class Theme {

    public static final String CLAIR = "theme-clair";
    public static final String SOMBRE = "theme-sombre";

    private static final String FEUILLE = "/com/pingmanager/ui/theme.css";
    private static final List<String> PILE_POLICES = List.of("Segoe UI", "Inter");

    private static boolean sombre = false;
    private static String police;

    private Theme() {
    }

    /** Ajoute la feuille de style à la scène et suit les changements de root. */
    public static void installer(Scene scene) {
        URL css = Theme.class.getResource(FEUILLE);
        if (css == null) {
            throw new IllegalStateException("Feuille de style introuvable : " + FEUILLE);
        }
        chargerPolicesAffichage();
        scene.getStylesheets().add(css.toExternalForm());
        appliquer(scene.getRoot());
        scene.rootProperty().addListener((obs, ancien, nouveau) -> appliquer(nouveau));
    }

    /** Polices d'affichage embarquées (titres) : Bricolage Grotesque, licence OFL. */
    private static final List<String> POLICES_AFFICHAGE = List.of(
            "/com/pingmanager/polices/BricolageGrotesque-Bold.ttf",
            "/com/pingmanager/polices/BricolageGrotesque-ExtraBold.ttf");

    /**
     * Enregistre les polices des titres auprès de JavaFX (familles « Bricolage Grotesque »
     * et « Bricolage Grotesque ExtraBold », utilisées par {@code theme.css}). Une police
     * absente n'empêche rien : la feuille retombe sur la police du texte.
     */
    private static void chargerPolicesAffichage() {
        for (String chemin : POLICES_AFFICHAGE) {
            try (java.io.InputStream in = Theme.class.getResourceAsStream(chemin)) {
                if (in != null) {
                    Font.loadFont(in, 12);
                }
            } catch (java.io.IOException e) {
                // Police facultative : on garde la police du texte.
            }
        }
    }

    /** Passe du clair au sombre (ou l'inverse) sur la scène donnée. */
    public static void basculer(Scene scene) {
        sombre = !sombre;
        if (scene != null) {
            appliquer(scene.getRoot());
        }
    }

    public static boolean estSombre() {
        return sombre;
    }

    private static void appliquer(Parent racine) {
        if (racine == null) {
            return;
        }
        racine.getStyleClass().removeAll(CLAIR, SOMBRE);
        racine.getStyleClass().add(sombre ? SOMBRE : CLAIR);
        racine.setStyle("-fx-font-family: \"" + police() + "\";");
    }

    private static String police() {
        if (police == null) {
            List<String> disponibles = Font.getFamilies();
            police = PILE_POLICES.stream()
                    .filter(disponibles::contains)
                    .findFirst()
                    .orElse("System");
        }
        return police;
    }
}
