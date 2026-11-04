package com.pingmanager.ui.ecran;

import com.pingmanager.core.CalendrierTournoi;
import com.pingmanager.core.CatalogueTournois;
import com.pingmanager.core.Territoires;
import com.pingmanager.core.reglement.Formats;
import com.pingmanager.model.FicheLicence;
import com.pingmanager.model.NiveauTournoi;
import com.pingmanager.ui.ContexteApplication;
import javafx.geometry.Pos;
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
import javafx.scene.control.MenuButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Onglet <b>Tournois</b> : trouver un tournoi (carte + liste + filtres synchronisés),
 * sa fiche, l'inscription en ligne, et « Mes tournois ».
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
        ToggleButton trouver = new ToggleButton("Trouver un tournoi");
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
            corps.setCenter(new MesTournois(contexte, tous, this::ouvrirFiche).build());
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
    // Explorateur : filtres, liste, carte
    // ==================================================================

    private Node construireExplorateur() {
        // --- Filtres ---
        TextField texte = Ui.champ("Nom, ville, club, département…");
        texte.setPrefWidth(240);
        texte.textProperty().addListener((o, a, b) -> { criteres = criteres.avecTexte(b); ajusterCarte = true; appliquerFiltres(); });

        DatePicker du = new DatePicker();
        du.setPromptText("Du");
        du.setPrefWidth(128);
        DatePicker au = new DatePicker();
        au.setPromptText("Au");
        au.setPrefWidth(128);
        Runnable periode = () -> { criteres = criteres.avecPeriode(du.getValue(), au.getValue()); ajusterCarte = true; appliquerFiltres(); };
        du.valueProperty().addListener((o, a, b) -> periode.run());
        au.valueProperty().addListener((o, a, b) -> periode.run());

        MenuButton niveaux = new MenuButton("Tous niveaux");
        niveaux.getStyleClass().add("filtre-menu");
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
        zone.setPrefWidth(200);
        zone.valueProperty().addListener((o, a, b) -> {
            String[] z = zones.getOrDefault(b, new String[] {null, null});
            criteres = criteres.avecDepartement(z[0]).avecLigue(z[1]);
            ajusterCarte = true;
            appliquerFiltres();
        });

        ComboBox<String> distance = new ComboBox<>();
        distance.getItems().addAll("Toutes distances", "≤ 25 km", "≤ 50 km", "≤ 100 km", "≤ 200 km", "≤ 400 km");
        distance.setValue("Toutes distances");
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
        accessibles.selectedProperty().addListener((o, a, b) -> {
            criteres = criteres.avecSeulementAccessibles(b);
            ajusterCarte = true;
            appliquerFiltres();
        });

        Button reinit = Ui.boutonLien("Réinitialiser");
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

        FlowPane ligne1 = new FlowPane(10, 10, texte, du, au, niveaux, zone, distance);
        FlowPane ligne2 = new FlowPane(18, 8, accessibles, aMesPoints, places, reinit);
        ligne2.setAlignment(Pos.CENTER_LEFT);
        VBox filtres = new VBox(10, ligne1, ligne2);
        filtres.getStyleClass().add("barre-filtres");

        // --- Liste ---
        compteur = Ui.gras("");
        liste = new ListView<>();
        liste.getStyleClass().add("liste-tournois");
        liste.setPlaceholder(Ui.discret("Aucun tournoi ne correspond à ces filtres."));
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
        Label astuce = Ui.aide("Double-clic ou Entrée : ouvrir la fiche. Un clic sur un marqueur de la carte l'ouvre aussi.");
        VBox colonne = new VBox(8, compteur, liste, astuce);
        colonne.setPrefWidth(430);
        colonne.setMinWidth(380);

        // --- Carte ---
        carte = new CarteTournois(this::ouvrirFiche, id -> catalogue.stream()
                .filter(e -> e.id().equals(id)).findFirst()
                .ifPresent(e -> liste.getSelectionModel().select(e)));
        Node vueCarte = carte.noeud();
        HBox.setHgrow(vueCarte, Priority.ALWAYS);
        VBox cadreCarte = new VBox(vueCarte);
        VBox.setVgrow(vueCarte, Priority.ALWAYS);
        cadreCarte.getStyleClass().add("cadre-carte");
        HBox.setHgrow(cadreCarte, Priority.ALWAYS);

        HBox corps = new HBox(16, colonne, cadreCarte);
        VBox.setVgrow(corps, Priority.ALWAYS);
        VBox explo = new VBox(14, filtres, corps);
        explo.getStyleClass().add("explorateur");
        return explo;
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
        CatalogueTournois.Entree avant = liste.getSelectionModel().getSelectedItem();
        liste.getItems().setAll(res);
        long grises = res.stream().filter(e -> e.acces().estRefuse()).count();
        compteur.setText(res.size() + (res.size() > 1 ? " tournois" : " tournoi")
                + (grises > 0 ? " · " + grises + " non accessible" + (grises > 1 ? "s" : "") : ""));
        carte.afficher(res, ajusterCarte);
        ajusterCarte = false;
        if (avant != null) {
            res.stream().filter(e -> e.id().equals(avant.id())).findFirst()
                    .ifPresent(e -> liste.getSelectionModel().select(e));
        }
    }

    /** Une ligne de la liste : nom, date et lieu, niveau, places, distance ; grisée avec motif si non accessible. */
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
            Label nom = Ui.gras(e.tournoi().getNom());
            nom.setWrapText(true);
            Label niveau = Ui.badge(e.tournoi().getNiveau() == null ? "?" : e.tournoi().getNiveau().getLibelle());
            HBox l1 = Ui.ligne(8, nom, Ui.espace(), niveau);
            if (e.moment() == CalendrierTournoi.Moment.EN_COURS) {
                Label enCours = Ui.badge("En cours");
                enCours.getStyleClass().add("badge-en-cours");
                l1.getChildren().add(2, enCours);
            }
            String date = BlocsInscriptions.periode(e.tournoi());
            String lieu = e.tournoi().getAdresse() == null ? "" : e.tournoi().getAdresse().getVille()
                    + (e.departement() == null ? "" : " (" + e.departement().code() + ")");
            Label l2 = Ui.discret(date + " · " + lieu);
            String placesTxt = e.placesRestantes() < 0 ? "Places illimitées"
                    : e.placesRestantes() == 0 ? "Complet (liste d'attente)"
                    : e.placesRestantes() + " places restantes sur " + e.placesTotales();
            String dist = e.distanceKm() == null ? "" : " · ≈ " + Math.round(e.distanceKm()) + " km";
            Label l3 = Ui.discret(placesTxt + dist + (e.tableauAMesPoints() ? "" : " · aucun tableau à vos points"));
            VBox v = new VBox(3, l1, l2, l3);
            if (e.acces().estRefuse()) {
                getStyleClass().add("tournoi-grise");
                Label motif = new Label(e.acces().motif());
                motif.setWrapText(true);
                motif.getStyleClass().add("motif-refus");
                v.getChildren().add(motif);
            } else if (e.acces().remarque() != null) {
                v.getChildren().add(Ui.aide(e.acces().remarque()));
            }
            l2.setWrapText(true);
            l3.setWrapText(true);
            nom.setMinWidth(0);
            HBox.setHgrow(nom, Priority.SOMETIMES);
            niveau.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
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
