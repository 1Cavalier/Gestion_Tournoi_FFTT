package com.pingmanager.ui.ecran;

import com.pingmanager.core.CatalogueTournois;
import com.pingmanager.core.Territoires;
import com.pingmanager.model.FicheLicence;
import com.pingmanager.model.NiveauTournoi;
import com.pingmanager.ui.ContexteApplication;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.MenuButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Onglet <b>Tournois</b> : « Carte des tournois » (filtres à gauche, deux présentations
 * au choix : carte + liste avec « Voir plus », ou agenda par jour), la fiche,
 * l'inscription en ligne, et « Mes tournois ». Chaque tournoi porte l'un des quatre
 * statuts de {@link com.pingmanager.core.StatutCarteTournoi} (pastille + couleur + texte) ;
 * sa fiche s'ouvre toujours, même sans inscription possible.
 *
 * <p>Les tournois auxquels le joueur ne peut pas participer (IX.101 / IX.102) restent
 * visibles, grisés, avec le motif en rouge ; l'interrupteur « Seulement ceux auxquels
 * je peux participer » les masque. Liste et carte partagent le même résultat filtré
 * ({@link CatalogueTournois#filtrer}).</p>
 */
public final class PageTournois {

    private final ContexteApplication contexte;
    private final BorderPane racine = new BorderPane();

    private List<ContexteApplication.TournoiFichier> tous = List.of();
    private List<CatalogueTournois.Entree> catalogue = List.of();
    private CatalogueTournois.Criteres criteres = CatalogueTournois.Criteres.aucun();
    private boolean mesTournois;

    // Explorateur (construit une fois, conservé entre deux fiches).
    private Node explorateur;
    private ListView<CatalogueTournois.Entree> liste;
    private CarteTournois carte;
    private Label compteur;
    private boolean ajusterCarte = true;
    /** Résultat filtré courant ; la présentation cachée (carte ou agenda) est remplie à la bascule. */
    private List<CatalogueTournois.Entree> resultat = List.of();
    private boolean carteAJour;
    private boolean agendaAJour;
    /** Saisie du texte : on filtre une fois la frappe posée, pas à chaque lettre. */
    private final javafx.animation.PauseTransition pauseSaisie =
            new javafx.animation.PauseTransition(javafx.util.Duration.millis(180));
    private VBox agenda;
    private PresentationCarteTournois.Vue vue = PresentationCarteTournois.Vue.CARTE;
    private Runnable basculerVue;

    public PageTournois(ContexteApplication contexte) {
        this.contexte = contexte;
    }

    /** Dernière page construite (harnais de captures d'écran, même paquet). */
    static PageTournois derniere;

    public Parent build() {
        derniere = this;
        recharger();
        afficherAccueil();
        return racine;
    }

    /** Relit les fichiers (nouvelles inscriptions, nouveaux tournois) et refait le catalogue. */
    private void recharger() {
        tous = contexte.tousLesTournois();
        catalogue = contexte.catalogue(tous);
    }

    // ==================================================================
    // En-tête commun : titre + bascule Trouver / Mes tournois
    // ==================================================================

    private void afficherAccueil() {
        ToggleGroup g = new ToggleGroup();
        ToggleButton trouver = new ToggleButton(PresentationCarteTournois.TITRE_ONGLET);
        ToggleButton mes = new ToggleButton("Mes tournois");
        HBox segment = Ui.segment(g, trouver, mes);
        (mesTournois ? mes : trouver).setSelected(true);
        trouver.setOnAction(e -> { mesTournois = false; trouver.setSelected(true); afficherAccueil(); });
        mes.setOnAction(e -> { mesTournois = true; mes.setSelected(true); afficherAccueil(); });

        Label titre = Ui.titre("Tournois");
        Optional<FicheLicence> fiche = contexte.ficheLicenceLiee();
        Label sous = Ui.discret(fiche.map(f -> "Licence " + f.getNumeroLicence() + " · " + f.getNomClub() + " · "
                + f.getPoints() + " points").orElse("Aucune licence liée : liez-la dans « Mon profil » pour vérifier "
                + "votre accès et vous inscrire."));
        HBox entete = Ui.ligne(16, new VBox(2, titre, sous), Ui.espace(), segment);
        entete.getStyleClass().add("page-entete");

        BorderPane corps = new BorderPane();
        corps.setTop(entete);
        if (mesTournois) {
            corps.setCenter(new MesTournois(contexte, tous, this::ouvrirFiche, this::revoirTournoi).build());
        } else {
            if (explorateur == null) {
                explorateur = construireExplorateur();
            }
            corps.setCenter(explorateur);
            appliquerFiltres();
        }
        racine.setCenter(corps);
    }

    // ==================================================================
    // Carte des tournois : filtres à gauche, présentation carte ou agenda par jour
    // ==================================================================

    private Node construireExplorateur() {
        // --- Filtres (colonne de gauche) ---
        TextField texte = Ui.champ("Nom, ville, club, département…");
        pauseSaisie.setOnFinished(e -> appliquerFiltres());
        texte.textProperty().addListener((o, a, b) -> {
            criteres = criteres.avecTexte(b);
            ajusterCarte = true;
            pauseSaisie.playFromStart();
        });

        DatePicker du = new DatePicker();
        du.setPromptText("Du");
        du.setMaxWidth(Double.MAX_VALUE);
        DatePicker au = new DatePicker();
        au.setPromptText("Au");
        au.setMaxWidth(Double.MAX_VALUE);
        Runnable periode = () -> { criteres = criteres.avecPeriode(du.getValue(), au.getValue()); ajusterCarte = true; appliquerFiltres(); };
        du.valueProperty().addListener((o, a, b) -> periode.run());
        au.valueProperty().addListener((o, a, b) -> periode.run());

        MenuButton niveaux = new MenuButton("Tous niveaux");
        niveaux.getStyleClass().add("filtre-menu");
        niveaux.setMaxWidth(Double.MAX_VALUE);
        EnumSet<NiveauTournoi> choisis = EnumSet.noneOf(NiveauTournoi.class);
        for (NiveauTournoi n : NiveauTournoi.values()) {
            CheckMenuItem item = new CheckMenuItem(n.getLibelle());
            item.selectedProperty().addListener((o, a, b) -> {
                if (b) {
                    choisis.add(n);
                } else {
                    choisis.remove(n);
                }
                niveaux.setText(choisis.isEmpty() ? "Tous niveaux"
                        : choisis.size() == 1 ? choisis.iterator().next().getLibelle() : choisis.size() + " niveaux");
                criteres = criteres.avecNiveaux(choisis);
                ajusterCarte = true;
                appliquerFiltres();
            });
            niveaux.getItems().add(item);
        }

        ComboBox<String> zone = new ComboBox<>();
        Map<String, String[]> zones = zones();
        zone.getItems().addAll(zones.keySet());
        zone.setValue("Toute la France");
        zone.setMaxWidth(Double.MAX_VALUE);
        zone.valueProperty().addListener((o, a, b) -> {
            String[] z = zones.getOrDefault(b, new String[] {null, null});
            criteres = criteres.avecDepartement(z[0]).avecLigue(z[1]);
            ajusterCarte = true;
            appliquerFiltres();
        });

        ComboBox<String> distance = new ComboBox<>();
        distance.getItems().addAll("Toutes distances", "≤ 25 km", "≤ 50 km", "≤ 100 km", "≤ 200 km", "≤ 400 km");
        distance.setValue("Toutes distances");
        distance.setMaxWidth(Double.MAX_VALUE);
        distance.setDisable(contexte.positionDuJoueur() == null);
        distance.valueProperty().addListener((o, a, b) -> {
            Integer km = b == null || b.startsWith("Toutes") ? null : Integer.valueOf(b.replaceAll("\\D", ""));
            criteres = criteres.avecDistanceMax(km);
            ajusterCarte = true;
            appliquerFiltres();
        });

        CheckBox aMesPoints = new CheckBox("Tableaux à mes points");
        aMesPoints.setDisable(contexte.ficheLicenceLiee().isEmpty());
        aMesPoints.selectedProperty().addListener((o, a, b) -> { criteres = criteres.avecMesPoints(b); appliquerFiltres(); });
        CheckBox places = new CheckBox("Places restantes");
        places.selectedProperty().addListener((o, a, b) -> { criteres = criteres.avecPlaces(b); appliquerFiltres(); });
        CheckBox accessibles = new CheckBox("Seulement ceux auxquels je peux participer");
        accessibles.getStyleClass().add("interrupteur");
        accessibles.setWrapText(true);
        accessibles.selectedProperty().addListener((o, a, b) -> {
            criteres = criteres.avecSeulementAccessibles(b);
            ajusterCarte = true;
            appliquerFiltres();
        });

        Button reinit = Ui.boutonLien("Réinitialiser les filtres");
        reinit.setOnAction(e -> {
            texte.clear();
            du.setValue(null);
            au.setValue(null);
            niveaux.getItems().forEach(i -> ((CheckMenuItem) i).setSelected(false));
            zone.setValue("Toute la France");
            distance.setValue("Toutes distances");
            aMesPoints.setSelected(false);
            places.setSelected(false);
            accessibles.setSelected(false);
        });

        VBox legende = new VBox(6);
        legende.getStyleClass().add("legende-statuts-carte");
        legende.getChildren().add(Ui.libelleChamp("Statuts"));
        for (PresentationCarteTournois.Pastille p : PresentationCarteTournois.legende()) {
            legende.getChildren().add(pastille(p));
        }

        VBox filtres = new VBox(12,
                Ui.titreCarte("Filtres"),
                Ui.champEtiquete("Recherche", texte),
                Ui.champEtiquete("Période", new VBox(6, du, au)),
                Ui.champEtiquete("Niveau", niveaux),
                Ui.champEtiquete("Zone", zone),
                Ui.champEtiquete("Distance", distance),
                accessibles, aMesPoints, places, reinit, legende);
        filtres.getStyleClass().addAll("barre-filtres", "filtres-carte-tournois");
        filtres.setPrefWidth(270);
        filtres.setMinWidth(240);
        filtres.setMaxWidth(290);
        ScrollPane colonneFiltres = new ScrollPane(filtres);
        colonneFiltres.setFitToWidth(true);
        colonneFiltres.getStyleClass().add("defilant-filtres-carte");
        colonneFiltres.setMinWidth(250);
        colonneFiltres.setPrefWidth(290);
        colonneFiltres.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        // --- Bascule des deux présentations ---
        ToggleGroup gVue = new ToggleGroup();
        ToggleButton bCarte = new ToggleButton(PresentationCarteTournois.libelleVue(PresentationCarteTournois.Vue.CARTE));
        ToggleButton bAgenda = new ToggleButton(PresentationCarteTournois.libelleVue(PresentationCarteTournois.Vue.AGENDA));
        HBox segmentVue = Ui.segment(gVue, bCarte, bAgenda);
        compteur = Ui.gras("");
        HBox barre = Ui.ligne(12, compteur, Ui.espace(), segmentVue);
        barre.getStyleClass().add("barre-vue-carte-tournois");

        // --- Présentation carte : liste avec « Voir plus » + carte ---
        liste = new ListView<>();
        liste.getStyleClass().add("liste-tournois");
        liste.setPlaceholder(Ui.discret(PresentationCarteTournois.AUCUN_RESULTAT));
        liste.setCellFactory(x -> new CelluleTournoi());
        liste.getSelectionModel().selectedItemProperty().addListener((o, a, e) -> {
            if (e != null) {
                carte.selectionner(e.id(), true);
            }
        });
        liste.setOnMouseClicked(ev -> {
            if (ev.getClickCount() == 2 && liste.getSelectionModel().getSelectedItem() != null) {
                ouvrirFiche(liste.getSelectionModel().getSelectedItem().id());
            }
        });
        liste.setOnKeyPressed(ev -> {
            if (ev.getCode() == KeyCode.ENTER && liste.getSelectionModel().getSelectedItem() != null) {
                ouvrirFiche(liste.getSelectionModel().getSelectedItem().id());
            }
        });
        VBox.setVgrow(liste, Priority.ALWAYS);
        Label astuce = Ui.aide("« Voir plus » ouvre la fiche, même si vous ne pouvez pas vous inscrire. "
                + "Un clic sur un marqueur de la carte l'ouvre aussi.");
        astuce.setWrapText(true);
        VBox colonne = new VBox(8, liste, astuce);
        colonne.setPrefWidth(400);
        colonne.setMinWidth(340);

        carte = CarteTournois.partagee(this::ouvrirFiche, id -> catalogue.stream()
                .filter(e -> e.id().equals(id)).findFirst()
                .ifPresent(e -> liste.getSelectionModel().select(e)));
        Node vueCarte = carte.noeud();
        VBox cadreCarte = new VBox(vueCarte);
        VBox.setVgrow(vueCarte, Priority.ALWAYS);
        cadreCarte.getStyleClass().add("cadre-carte");
        HBox.setHgrow(cadreCarte, Priority.ALWAYS);
        HBox presentationCarte = new HBox(16, colonne, cadreCarte);

        // --- Présentation agenda : un bloc par jour ---
        agenda = new VBox(16);
        agenda.getStyleClass().add("agenda-tournois");
        ScrollPane presentationAgenda = new ScrollPane(agenda);
        presentationAgenda.setFitToWidth(true);
        presentationAgenda.getStyleClass().add("defilant-agenda-tournois");

        StackPane contenu = new StackPane(presentationCarte, presentationAgenda);
        VBox.setVgrow(contenu, Priority.ALWAYS);
        basculerVue = () -> {
            boolean enAgenda = vue == PresentationCarteTournois.Vue.AGENDA;
            presentationCarte.setVisible(!enAgenda);
            presentationAgenda.setVisible(enAgenda);
            (enAgenda ? bAgenda : bCarte).setSelected(true);
            Ui.apparition(enAgenda ? presentationAgenda : presentationCarte);
        };
        bCarte.setOnAction(e -> choisirVue(PresentationCarteTournois.Vue.CARTE));
        bAgenda.setOnAction(e -> choisirVue(PresentationCarteTournois.Vue.AGENDA));
        basculerVue.run();

        VBox droite = new VBox(12, barre, contenu);
        HBox.setHgrow(droite, Priority.ALWAYS);
        HBox explo = new HBox(16, colonneFiltres, droite);
        VBox.setVgrow(explo, Priority.ALWAYS);
        explo.getStyleClass().addAll("explorateur", "carte-des-tournois");
        return explo;
    }

    /** Affiche la présentation choisie (carte ou agenda par jour) ; aussi pour le harnais. */
    void choisirVue(PresentationCarteTournois.Vue v) {
        vue = v;
        if (basculerVue != null) {
            basculerVue.run();
        }
        remplirVueVisible();
    }

    /** Pastille d'un statut de la carte (couleur de la charte + classe propre au statut). */
    private static Label pastille(PresentationCarteTournois.Pastille p) {
        Label l = Ui.pastille(p.texte(), p.couleur());
        l.getStyleClass().addAll("pastille-statut-carte", p.classeCss());
        // Puce devant le texte : le statut se lit aussi sans la couleur de fond.
        l.setGraphic(Ui.puce("puce-statut-carte"));
        l.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        return l;
    }

    /** Motif du statut : rouge si non accessible, discret si complet ou hors niveau. */
    private static Label motif(PresentationCarteTournois.CarteTournoi c) {
        Label l = Ui.label(c.motif(), PresentationCarteTournois.classeMotif(c));
        l.setWrapText(true);
        return l;
    }

    /** Bouton « Voir plus » : ouvre la fiche, quel que soit le statut. */
    private Button voirPlus(String id) {
        Button b = Ui.boutonSecondaire(PresentationCarteTournois.VOIR_PLUS);
        b.getStyleClass().add("bouton-voir-plus");
        b.setOnAction(ev -> ouvrirFiche(id));
        b.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        return b;
    }

    /** Met à jour seulement la présentation affichée (la carte ou l'agenda), si elle a changé. */
    private void remplirVueVisible() {
        if (carte == null) {
            return;
        }
        if (vue == PresentationCarteTournois.Vue.AGENDA) {
            if (!agendaAJour) {
                remplirAgenda(resultat);
                agendaAJour = true;
            }
        } else if (!carteAJour) {
            carte.afficher(resultat, ajusterCarte);
            ajusterCarte = false;
            carteAJour = true;
        }
    }

    /** Remplit la présentation agenda (un bloc par jour). */
    private void remplirAgenda(List<CatalogueTournois.Entree> res) {
        agenda.getChildren().clear();
        List<PresentationCarteTournois.Jour> jours = PresentationCarteTournois.agenda(res, java.time.LocalDate.now());
        if (jours.isEmpty()) {
            agenda.getChildren().add(Ui.etatVideCompact(PresentationCarteTournois.AUCUN_RESULTAT));
            return;
        }
        for (PresentationCarteTournois.Jour j : jours) {
            Label titre = Ui.label(j.titre(), "agenda-jour-titre");
            VBox bloc = new VBox(0);
            bloc.getStyleClass().add("agenda-jour-liste");
            boolean premiere = true;
            for (PresentationCarteTournois.CarteTournoi c : j.tournois()) {
                bloc.getChildren().add(ligneTournoi(c, premiere));
                premiere = false;
            }
            VBox jour = new VBox(8, titre, bloc);
            jour.getStyleClass().add("agenda-jour");
            if (j.aujourdhui()) {
                jour.getStyleClass().add("agenda-jour-aujourdhui");
            }
            agenda.getChildren().add(jour);
        }
    }

    /** Une ligne de tournoi de l'agenda : pastille, nom, niveau et lieu, places, motif, « Voir plus ». */
    private VBox ligneTournoi(PresentationCarteTournois.CarteTournoi c, boolean premiere) {
        HBox l = Ui.ligneElement(pastille(c.pastille()), c.nom(),
                c.niveau() + " · " + c.dateLieu() + " · " + c.places(), voirPlus(c.id()));
        if (premiere) {
            l.getStyleClass().add("ligne-element-premiere");
        }
        VBox v = new VBox(4, l);
        if (c.motif() != null) {
            v.getChildren().add(motif(c));
        } else if (c.remarque() != null) {
            v.getChildren().add(Ui.aide(c.remarque()));
        }
        v.getStyleClass().add("agenda-ligne-tournoi");
        if (c.grise()) {
            v.getStyleClass().add("tournoi-grise");
        }
        return v;
    }

    /** Zones proposées : toute la France, mon département, ma ligue, puis chaque ligue. */
    private Map<String, String[]> zones() {
        Map<String, String[]> z = new LinkedHashMap<>();
        z.put("Toute la France", new String[] {null, null});
        contexte.ficheLicenceLiee().flatMap(f -> contexte.eligibilite().departementDuJoueur(f)).ifPresent(d -> {
            z.put("Mon département (" + d.code() + ")", new String[] {d.code(), null});
            z.put("Ma ligue (" + d.ligue() + ")", new String[] {null, d.ligue()});
        });
        java.util.TreeSet<String> ligues = new java.util.TreeSet<>();
        Territoires.tous().values().forEach(d -> ligues.add(d.ligue()));
        for (String l : ligues) {
            z.put("Ligue " + l, new String[] {null, l});
        }
        return z;
    }

    private void appliquerFiltres() {
        if (liste == null) {
            return;
        }
        List<CatalogueTournois.Entree> res = CatalogueTournois.filtrer(catalogue, criteres);
        resultat = res;
        CatalogueTournois.Entree avant = liste.getSelectionModel().getSelectedItem();
        liste.getItems().setAll(res);
        compteur.setText(PresentationCarteTournois.compteur(res));
        carteAJour = false;
        agendaAJour = false;
        remplirVueVisible();
        if (avant != null) {
            res.stream().filter(e -> e.id().equals(avant.id())).findFirst()
                    .ifPresent(e -> liste.getSelectionModel().select(e));
        }
    }

    /** Une carte de tournoi : nom et pastille de statut, niveau, date et lieu, places, motif, « Voir plus ». */
    private final class CelluleTournoi extends ListCell<CatalogueTournois.Entree> {
        CelluleTournoi() {
            // Largeur = celle de la liste (pas de défilement horizontal) ; le texte se replie.
            setPrefWidth(0);
        }

        @Override
        protected void updateItem(CatalogueTournois.Entree e, boolean vide) {
            super.updateItem(e, vide);
            getStyleClass().remove("tournoi-grise");
            setText(null);
            if (vide || e == null) {
                setGraphic(null);
                return;
            }
            PresentationCarteTournois.CarteTournoi c = PresentationCarteTournois.carte(e);
            Label nom = Ui.gras(c.nom());
            nom.setWrapText(true);
            nom.setMinWidth(0);
            HBox.setHgrow(nom, Priority.SOMETIMES);
            HBox l1 = Ui.ligne(8, nom, Ui.espace(), pastille(c.pastille()));
            Label l2 = Ui.discret(c.niveau() + " · " + c.dateLieu());
            l2.setWrapText(true);
            Label l3 = Ui.discret(c.places());
            l3.setWrapText(true);
            VBox v = new VBox(3, l1, l2, l3);
            if (c.motif() != null) {
                v.getChildren().add(motif(c));
            } else if (c.remarque() != null) {
                v.getChildren().add(Ui.aide(c.remarque()));
            }
            if (c.grise()) {
                getStyleClass().add("tournoi-grise");
            }
            HBox bas = Ui.ligne(8, Ui.espace(), voirPlus(c.id()));
            v.getChildren().add(bas);
            v.setMaxWidth(Double.MAX_VALUE);
            v.setMinWidth(0);
            setGraphic(v);
        }
    }

    // ==================================================================
    // Fiche et inscription
    // ==================================================================

    /** Ouvre la fiche du tournoi d'identifiant {@code id} (chemin du fichier). */
    private void ouvrirFiche(String id) {
        Path f = Path.of(id);
        FicheTournoi fiche = new FicheTournoi(contexte, f, () -> {
            recharger();
            afficherAccueil();
        }, this::ouvrirFiche);
        racine.setCenter(fiche.build());
    }

    /** « Mes tournois », tournoi passé : poules, scores et tableau final en lecture seule (B031). */
    private void revoirTournoi(String id) {
        Path f = Path.of(id);
        racine.setCenter(new EcranSuiviTournoi(contexte, f, () -> {
            recharger();
            afficherAccueil();
        }, () -> ouvrirFiche(id)).enArchive().build(contexte.chargerTournoi(f)));
    }

    /** Pour le harnais de captures : ouvre la fiche d'un tournoi par son nom. */
    void ouvrirFicheParNom(String debutNom) {
        for (ContexteApplication.TournoiFichier tf : tous) {
            if (tf.tournoi().getNom().startsWith(debutNom)) {
                ouvrirFiche(tf.fichier().toString());
                return;
            }
        }
    }

    /** Pour le harnais : la carte. */
    CarteTournois carte() {
        return carte;
    }

    /** Pour les tests : les entrées visibles après filtrage. */
    List<CatalogueTournois.Entree> visibles() {
        return liste == null ? List.of() : new ArrayList<>(liste.getItems());
    }
}
