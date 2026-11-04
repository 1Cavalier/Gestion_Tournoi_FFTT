package com.pingmanager.ui;

import com.pingmanager.ui.ecran.Coquille;
import com.pingmanager.ui.ecran.EcranConnexion;
import com.pingmanager.ui.ecran.Theme;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Application JavaFX PingManager.
 *
 * <p>Enchaînement volontairement simple :</p>
 * <ol>
 *   <li>au démarrage, l'{@link EcranConnexion} occupe la fenêtre ;</li>
 *   <li>une fois connecté, la {@link Coquille} (accueil + navigation à droite)
 *       prend sa place ;</li>
 *   <li>à la déconnexion, on revient à l'écran de connexion.</li>
 * </ol>
 *
 * <p>Toute la logique métier passe par un unique {@link ContexteApplication}.
 * L'écran ne connaît pas les services : il appelle le contexte.</p>
 */
public final class AppPingManager extends Application {

    private ContexteApplication contexte;
    private Scene scene;

    @Override
    public void start(Stage stage) {
        this.contexte = new ContexteApplication();

        // Petit rappel des données de démo dans la console, pour les tests manuels.
        System.out.println(contexte.memoDemo());

        this.scene = new Scene(construireConnexion(), 1040, 700);
        Theme.installer(scene);   // theme.css + classe clair/sombre sur chaque root

        stage.setTitle("PingManager");
        stage.setScene(scene);
        stage.setMinWidth(820);
        stage.setMinHeight(560);
        stage.show();
    }

    /** Construit l'écran de connexion, câblé pour basculer vers la coquille. */
    private javafx.scene.Parent construireConnexion() {
        EcranConnexion connexion = new EcranConnexion(contexte, this::afficherCoquille);
        return connexion.build();
    }

    /** Bascule vers l'accueil (coquille) une fois connecté. */
    private void afficherCoquille() {
        Coquille coquille = new Coquille(contexte, this::afficherConnexion);
        scene.setRoot(coquille.build());
    }

    /** Retour à l'écran de connexion (déconnexion). */
    private void afficherConnexion() {
        contexte.deconnecter();
        scene.setRoot(construireConnexion());
    }

    @Override
    public void stop() {
        // Fermeture propre : persiste le dépôt applicatif et arrête l'autosave.
        if (contexte != null) {
            contexte.fermer();
        }
    }
}
