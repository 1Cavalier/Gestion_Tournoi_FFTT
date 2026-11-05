package com.pingmanager.ui.ecran;

import com.pingmanager.model.CompteUtilisateur;
import com.pingmanager.ui.ContexteApplication;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Accueil de l'application une fois connecté (charte v2) : une <b>barre du
 * haut blanche</b> (marque, titre de la page, bascule clair/sombre, compte
 * connecté avec son avatar), une <b>barre latérale blanche à gauche</b>
 * (rubriques avec icône, élément actif en bleu) et le contenu au centre, qui
 * apparaît par un court fondu à chaque changement de page.
 *
 * <p>Les entrées publiques (Accueil, Mon profil, Tournois, Stats) sont toujours
 * présentes. Dès que le compte administre au moins un club, un second groupe
 * « Gestion » apparaît (Gestion des tournois, Historique, Buvette). La
 * {@link com.pingmanager.ui.ecran.PageCompte} déclenche
 * {@link #rafraichirNavigation()} après une liaison ou une dissociation.</p>
 */
public final class Coquille {

    /** Une entrée de navigation : une clé, un libellé, une icône et de quoi bâtir la page. */
    private static final class Entree {
        final String cle;
        final String libelle;
        final String icone;
        final Supplier<Parent> page;

        Entree(String cle, String libelle, String icone, Supplier<Parent> page) {
            this.cle = cle;
            this.libelle = libelle;
            this.icone = icone;
            this.page = page;
        }
    }

    private final ContexteApplication contexte;
    private final Runnable onDeconnexion;

    private final BorderPane racine = new BorderPane();
    private final VBox navBox = new VBox(2);
    private final List<Button> boutonsNav = new ArrayList<>();
    private final Label titrePage = new Label();

    private String cleCourante = "accueil";

    public Coquille(ContexteApplication contexte, Runnable onDeconnexion) {
        this.contexte = contexte;
        this.onDeconnexion = onDeconnexion;
    }

    public Parent build() {
        racine.setTop(construireEntete());

        navBox.getStyleClass().add("nav");
        navBox.setPrefWidth(236);
        navBox.setMinWidth(236);
        racine.setLeft(navBox);

        rafraichirNavigation();
        afficher(cleCourante);
        // Pas de focus d'office sur un bouton de l'en-tête : Tab reste disponible.
        Platform.runLater(racine::requestFocus);
        return racine;
    }

    private Parent construireEntete() {
        Label appNom = new Label("PingManager");
        appNom.getStyleClass().add("entete-titre");
        HBox marque = new HBox(10, marque(), appNom);
        marque.setAlignment(Pos.CENTER_LEFT);
        // Même largeur que la barre latérale : le titre de page s'aligne sur le contenu.
        marque.setMinWidth(196);
        marque.setPrefWidth(196);

        Region separateur = new Region();
        separateur.getStyleClass().add("entete-separateur");
        titrePage.getStyleClass().add("entete-page");

        CompteUtilisateur compte = contexte.compteConnecte();
        Label utilisateur = new Label(compte.getNomComplet());
        utilisateur.getStyleClass().add("entete-utilisateur");
        HBox identite = new HBox(10, Ui.avatar(compte.getPrenom(), compte.getNom()), utilisateur);
        identite.setAlignment(Pos.CENTER_LEFT);

        Button theme = new Button(libelleTheme());
        theme.getStyleClass().add("bouton-entete");
        theme.setOnAction(e -> {
            Theme.basculer(theme.getScene());
            theme.setText(libelleTheme());
        });

        Button deconnexion = new Button("Se déconnecter");
        deconnexion.getStyleClass().add("bouton-entete");
        deconnexion.setOnAction(e -> onDeconnexion.run());

        HBox barre = new HBox(16, marque, separateur, titrePage, Ui.espace(), theme, identite, deconnexion);
        barre.setAlignment(Pos.CENTER_LEFT);
        barre.getStyleClass().add("entete");
        return barre;
    }

    /** Marque : petit carré bleu arrondi portant une balle (dessin propre à PingManager). */
    static Node marque() {
        Rectangle carre = new Rectangle(28, 28);
        carre.setArcWidth(10);
        carre.setArcHeight(10);
        carre.getStyleClass().add("marque-carre");
        Circle balle = new Circle(6);
        balle.getStyleClass().add("marque-balle");
        StackPane m = new StackPane(carre, balle);
        StackPane.setAlignment(balle, Pos.CENTER);
        balle.setTranslateX(3);
        balle.setTranslateY(-3);
        return m;
    }

    /** Le bouton annonce le thème vers lequel il bascule. */
    private static String libelleTheme() {
        return Theme.estSombre() ? "Thème clair" : "Thème sombre";
    }

    /** (Re)construit la liste des entrées de navigation selon l'état du compte. */
    public void rafraichirNavigation() {
        // Entrées publiques, visibles de tous.
        List<Entree> general = new ArrayList<>();
        general.add(new Entree("accueil", "Accueil", Icones.ACCUEIL,
                () -> new PageAccueil(contexte, this::afficher).build()));
        general.add(new Entree("profil", "Mon profil", Icones.PROFIL,
                () -> new PageCompte(contexte, this::rafraichirNavigation).build()));
        general.add(new Entree("tournois", "Tournois", Icones.TOURNOIS,
                () -> new PageTournois(contexte).build()));
        general.add(new Entree("agenda", "Agenda", Icones.AGENDA,
                () -> PagePlaceholder.page("Agenda",
                        "Vos dates, et les tournois où vous êtes inscrit, apparaîtront ici ; "
                                + "vous pourrez les exporter vers votre agenda (Google Agenda…).")));
        general.add(new Entree("stats", "Stats", Icones.STATS,
                () -> PagePlaceholder.page("Stats",
                        "Vos statistiques de joueur (résultats, progression) apparaîtront ici.")));

        // Entrées de gestion, visibles seulement si rattaché à un club.
        List<Entree> gestion = new ArrayList<>();
        if (contexte.estOrganisateur()) {
            gestion.add(new Entree("gestion", "Gestion des tournois", Icones.GESTION,
                    () -> new PageGestionTournoi(contexte).build()));
            gestion.add(new Entree("historique", "Historique", Icones.HISTORIQUE,
                    () -> PagePlaceholder.page("Historique",
                            "Les statistiques du club sur ses tournois passés (recettes, nombre "
                                    + "de joueurs, présents) apparaîtront ici.")));
            gestion.add(new Entree("buvette", "Buvette", Icones.BUVETTE,
                    () -> PagePlaceholder.page("Buvette",
                            "La gestion de la buvette (stocks, prix, prévisionnel en direct) "
                                    + "apparaîtra ici.")));
        }

        // Supervision, réservée au compte « ultra admin ».
        List<Entree> supervision = new ArrayList<>();
        if (contexte.estUltraAdmin()) {
            supervision.add(new Entree("supervision", "Tous les tournois", Icones.SUPERVISION,
                    () -> new PageSupervision(contexte).build()));
        }

        navBox.getChildren().clear();
        boutonsNav.clear();

        ajouterGroupe("Général", general);
        if (!gestion.isEmpty()) {
            Separator sep = new Separator();
            sep.getStyleClass().add("nav-separation");
            navBox.getChildren().add(sep);
            ajouterGroupe("Gestion", gestion);
        }
        if (!supervision.isEmpty()) {
            Separator sep = new Separator();
            sep.getStyleClass().add("nav-separation");
            navBox.getChildren().add(sep);
            ajouterGroupe("Supervision", supervision);
        }

        boolean cleCouranteExiste = boutonsNav.stream()
                .anyMatch(b -> ((Entree) b.getUserData()).cle.equals(cleCourante));
        if (!cleCouranteExiste) {
            cleCourante = "accueil";
        }
        souligneCourant();
    }

    /** Ajoute un titre de groupe puis les boutons de ses entrées. */
    private void ajouterGroupe(String titre, List<Entree> entrees) {
        Label titreGroupe = new Label(titre);
        titreGroupe.getStyleClass().add("nav-groupe-titre");
        navBox.getChildren().add(titreGroupe);

        for (Entree e : entrees) {
            Button b = new Button(e.libelle, Ui.iconeCadree(e.icone, "nav-icone"));
            b.getStyleClass().add("nav-item");
            b.setMaxWidth(Double.MAX_VALUE);
            b.setAlignment(Pos.CENTER_LEFT);
            b.setOnAction(ev -> afficher(e.cle));
            b.setUserData(e);
            navBox.getChildren().add(b);
            boutonsNav.add(b);
        }
    }

    /** Affiche la page correspondant à la clé et met à jour la zone centrale. */
    public void afficher(String cle) {
        Entree cible = null;
        for (Button b : boutonsNav) {
            Entree e = (Entree) b.getUserData();
            if (e.cle.equals(cle)) {
                cible = e;
                break;
            }
        }
        if (cible == null) {
            return;
        }
        cleCourante = cle;
        Parent page = cible.page.get();
        racine.setCenter(page);
        titrePage.setText(cible.libelle);
        Ui.apparition(page);
        souligneCourant();
    }

    /** Marque l'entrée active (classe nav-item-actif) pour repérer où l'on se trouve. */
    private void souligneCourant() {
        for (Button b : boutonsNav) {
            Entree e = (Entree) b.getUserData();
            b.getStyleClass().remove("nav-item-actif");
            if (e.cle.equals(cleCourante)) {
                b.getStyleClass().add("nav-item-actif");
            }
        }
    }
}
