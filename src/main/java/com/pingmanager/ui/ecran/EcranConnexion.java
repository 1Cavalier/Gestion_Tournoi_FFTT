package com.pingmanager.ui.ecran;

import com.pingmanager.core.PolitiqueMotDePasse;
import com.pingmanager.core.ResultatConnexion;
import com.pingmanager.core.ResultatCreationCompte;
import com.pingmanager.ui.ContexteApplication;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;

/**
 * Écran d'entrée : se connecter, ou créer un compte (email + mot de passe).
 *
 * <p>À gauche, un panneau bleu roi avec la marque, trois points forts et une
 * table vue de dessus (charte v2). À droite, sur le fond clair, une carte
 * blanche avec un seul formulaire et une bascule entre les deux modes. En cas de
 * succès, le callback {@code onConnexionReussie} est déclenché : l'application
 * affiche alors l'accueil.</p>
 */
public final class EcranConnexion {

    private final ContexteApplication contexte;
    private final Runnable onConnexionReussie;

    private boolean modeCreation = false;

    // Champs (construits une fois, montrés/masqués selon le mode).
    private final TextField champEmail = Ui.champ("nom@exemple.fr");
    private final TextField champNom = Ui.champ(null);
    private final TextField champPrenom = Ui.champ(null);
    private final PasswordField champMotDePasse = new PasswordField();

    // Prénom + nom, affichés seulement en création de compte.
    private Node blocIdentite;

    private final Label titre = new Label("Connexion");
    private final Label chapeau = new Label();
    private final Label erreur = Ui.message();
    private final Button bouton = Ui.boutonPrimaire("Se connecter");
    private final Hyperlink bascule = new Hyperlink("Pas encore de compte ? Créer un compte");

    // Point 13 : étape « code reçu par email », montrée après la création du compte.
    private final TextField champCode = Ui.champ("code reçu");
    private final Label messageCode = Ui.message();
    private final Label apercuDemo = new Label();
    private Node formulaireSaisie;
    /** Point 21 (RGPD) : mention d'information, en création de compte seulement. */
    private final Label mentionRgpd = new Label(com.pingmanager.core.MentionInformation.CREATION_COMPTE);
    private Node etapeCode;

    public EcranConnexion(ContexteApplication contexte, Runnable onConnexionReussie) {
        this.contexte = contexte;
        this.onConnexionReussie = onConnexionReussie;
    }

    public Parent build() {
        titre.getStyleClass().add("connexion-titre");
        chapeau.getStyleClass().add("discret");
        chapeau.setWrapText(true);

        bouton.setDefaultButton(true);
        bouton.setMaxWidth(Double.MAX_VALUE);
        bouton.setOnAction(e -> soumettre());

        bascule.setOnAction(e -> basculerMode());

        Node blocPrenom = Ui.champEtiquete("Prénom", champPrenom);
        Node blocNom = Ui.champEtiquete("Nom", champNom);
        HBox identite = new HBox(12, blocPrenom, blocNom);
        HBox.setHgrow(blocPrenom, Priority.ALWAYS);
        HBox.setHgrow(blocNom, Priority.ALWAYS);
        blocIdentite = identite;

        VBox formulaire = new VBox(14,
                identite,
                Ui.champEtiquete("Adresse email", champEmail),
                Ui.champEtiquete("Mot de passe", champMotDePasse),
                mentionRgpd,
                erreur,
                bouton);
        mentionRgpd.setWrapText(true);
        mentionRgpd.getStyleClass().add("discret");
        formulaire.setFillWidth(true);

        formulaireSaisie = new VBox(22, formulaire, bascule);
        etapeCode = construireEtapeCode();
        Ui.cacher(etapeCode);

        VBox enteteCarte = new VBox(6, titre, chapeau);
        VBox carte = new VBox(22, enteteCarte, formulaireSaisie, etapeCode);
        carte.getStyleClass().addAll("carte", "connexion-carte");
        carte.setMaxWidth(420);
        carte.setMaxHeight(Region.USE_PREF_SIZE);

        appliquerMode(); // configure la visibilité initiale (mode connexion)

        StackPane zone = new StackPane(carte);
        zone.getStyleClass().add("connexion-zone");
        HBox.setHgrow(zone, Priority.ALWAYS);

        return new HBox(vitrine(), zone);
    }

    /** Panneau de marque : nom, accroche, points forts et la table vue de dessus. */
    private static Node vitrine() {
        Label marque = new Label("PingManager");
        marque.getStyleClass().add("connexion-marque");
        HBox ligneMarque = new HBox(12, Coquille.marque(), marque);
        ligneMarque.setAlignment(Pos.CENTER_LEFT);

        Label accroche = new Label("Préparer, homologuer et faire vivre\nvos tournois de tennis de table.");
        accroche.getStyleClass().add("connexion-accroche");
        accroche.setWrapText(true);

        VBox points = new VBox(10,
                point("Création et homologation du tournoi"),
                point("Inscriptions en ligne et listes d'attente"),
                point("Pointage, poules et tableaux le jour J"));
        VBox.setMargin(points, new javafx.geometry.Insets(18, 0, 0, 0));

        StackPane table = new StackPane(dessinTable());
        VBox.setVgrow(table, Priority.ALWAYS);

        Label pied = new Label("Tournois individuels homologués FFTT");
        pied.getStyleClass().add("connexion-pied");

        VBox v = new VBox(12, ligneMarque, accroche, points, table, pied);
        v.getStyleClass().add("connexion-vitrine");
        v.setPrefWidth(440);
        v.setMinWidth(340);
        return v;
    }

    private static Node point(String texte) {
        javafx.scene.shape.Circle puce = new javafx.scene.shape.Circle(3.5);
        puce.getStyleClass().add("connexion-puce");
        Label l = new Label(texte);
        l.getStyleClass().add("connexion-point");
        HBox h = new HBox(10, puce, l);
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    /**
     * Table réglementaire vue de dessus, à l'échelle (152,5 × 274 cm) : plateau,
     * lignes de côté et de fond, ligne médiane, et filet qui déborde de 15,25 cm
     * de chaque côté.
     */
    private static Node dessinTable() {
        double k = 0.82;                       // px par cm
        double l = 152.5 * k;
        double h = 274 * k;
        double debord = 15.25 * k;

        Rectangle plateau = new Rectangle(l, h);
        plateau.getStyleClass().add("table-plateau");

        Line mediane = new Line(l / 2, 0, l / 2, h);
        mediane.getStyleClass().add("table-ligne-centrale");

        Line filet = new Line(-debord, h / 2, l + debord, h / 2);
        filet.getStyleClass().add("table-filet");

        Rectangle poteauG = new Rectangle(-debord - 3, h / 2 - 4, 4, 8);
        Rectangle poteauD = new Rectangle(l + debord - 1, h / 2 - 4, 4, 8);
        poteauG.getStyleClass().add("table-poteau");
        poteauD.getStyleClass().add("table-poteau");

        Group g = new Group(plateau, mediane, filet, poteauG, poteauD);
        g.setRotate(-8);
        return g;
    }

    private void basculerMode() {
        modeCreation = !modeCreation;
        appliquerMode();
    }

    private void appliquerMode() {
        cacherErreur();
        boolean creation = modeCreation;

        if (creation) {
            Ui.montrer(blocIdentite);
            Ui.montrer(mentionRgpd);
        } else {
            Ui.cacher(blocIdentite);
            Ui.cacher(mentionRgpd);
        }

        titre.setText(creation ? "Créer un compte" : "Connexion");
        chapeau.setText(creation
                ? "Quelques informations pour commencer. Vous lierez votre licence ensuite."
                : "Accédez à vos tournois et à ceux de votre club.");
        bouton.setText(creation ? "Créer mon compte" : "Se connecter");
        champMotDePasse.setPromptText(creation
                ? PolitiqueMotDePasse.LONGUEUR_MINIMALE + " caractères minimum" : "");
        bascule.setText(creation
                ? "Déjà un compte ? Se connecter"
                : "Pas encore de compte ? Créer un compte");
    }

    private void soumettre() {
        cacherErreur();
        String email = champEmail.getText() == null ? "" : champEmail.getText().trim();
        String motDePasse = champMotDePasse.getText() == null ? "" : champMotDePasse.getText();

        if (email.isBlank() || motDePasse.isBlank()) {
            afficherErreur("Merci de renseigner l'email et le mot de passe.");
            return;
        }

        try {
            if (modeCreation) {
                creerCompte(email, motDePasse);
            } else {
                connecter(email, motDePasse);
            }
        } catch (RuntimeException ex) {
            afficherErreur("Une erreur est survenue : " + ex.getMessage());
        }
    }

    private void connecter(String email, String motDePasse) {
        ResultatConnexion resultat = contexte.connexion(email, motDePasse);
        if (resultat.estConnecte()) {
            onConnexionReussie.run();
        } else {
            afficherErreur(resultat.getMessage());
        }
    }

    private void creerCompte(String email, String motDePasse) {
        String nom = champNom.getText() == null ? "" : champNom.getText().trim();
        String prenom = champPrenom.getText() == null ? "" : champPrenom.getText().trim();
        if (nom.isBlank() || prenom.isBlank()) {
            afficherErreur("Merci de renseigner votre nom et votre prénom.");
            return;
        }
        ResultatCreationCompte resultat = contexte.creerCompte(email, nom, prenom, motDePasse);
        if (resultat.estCree()) {
            montrerEtapeCode(email);
        } else {
            afficherErreur(resultat.getMessage());
        }
    }

    /** Étape de vérification de l'email (point 13) : saisie du code, renvoi, « plus tard ». */
    private Node construireEtapeCode() {
        Label explication = Ui.texte("Sans cette vérification, votre compte n'a aucun droit "
                + "d'organisateur ni de club. Vous pourrez aussi la faire plus tard depuis « Mon profil ».");
        explication.setWrapText(true);
        apercuDemo.setWrapText(true);
        apercuDemo.getStyleClass().add("discret");
        Button valider = Ui.boutonPrimaire("Valider le code");
        valider.setOnAction(e -> validerCode());
        Button renvoyer = Ui.boutonSecondaire("Renvoyer un code");
        renvoyer.setOnAction(e -> {
            contexte.envoyerCodeEmail();
            majApercuDemo();
            Ui.succes(messageCode, "Un nouveau code a été envoyé.");
        });
        Hyperlink plusTard = new Hyperlink("Plus tard");
        plusTard.setOnAction(e -> onConnexionReussie.run());
        return new VBox(14, explication, Ui.champEtiquete("Code de vérification", champCode),
                messageCode, Ui.actions(valider, renvoyer), apercuDemo, plusTard);
    }

    private void montrerEtapeCode(String email) {
        titre.setText("Vérifiez votre email");
        chapeau.setText("Un code vient d'être envoyé à " + email + ".");
        Ui.cacher(formulaireSaisie);
        Ui.montrer(etapeCode);
        champCode.clear();
        Ui.cacher(messageCode);
        majApercuDemo();
    }

    private void majApercuDemo() {
        java.util.Optional<String> apercu = contexte.apercuEmailDemo();
        apercuDemo.setText(apercu.map(a -> "Mode démo — email envoyé :\n" + a).orElse(""));
        if (apercu.isPresent()) {
            Ui.montrer(apercuDemo);
        } else {
            Ui.cacher(apercuDemo);
        }
    }

    private void validerCode() {
        com.pingmanager.core.ResultatVerificationEmail r = contexte.confirmerEmail(
                champCode.getText() == null ? "" : champCode.getText());
        if (r.estVerifie()) {
            onConnexionReussie.run();
        } else {
            Ui.erreur(messageCode, r.getMessage());
        }
    }

    private void afficherErreur(String message) {
        Ui.erreur(erreur, message);
    }

    private void cacherErreur() {
        Ui.cacher(erreur);
    }
}
