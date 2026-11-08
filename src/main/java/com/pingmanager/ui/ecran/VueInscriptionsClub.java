package com.pingmanager.ui.ecran;

import com.pingmanager.core.RemplissageTableaux;
import com.pingmanager.core.ResultatContactSms;
import com.pingmanager.core.reglement.Formats;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.ui.ContexteApplication;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Prévisualisation du <b>remplissage des tableaux</b> côté club organisateur
 * (Gestion des tournois → tournoi → « Inscriptions ») :
 * <ul>
 *   <li>par tableau : jauge des places, inscrits triés par points de licence
 *       (décroissant), puis la liste d'attente dans l'ordre ;</li>
 *   <li>par joueur : chaque participant et tous ses tableaux, tri par points ou par
 *       club, recherche par nom, licence ou club ; le joueur choisi peut recevoir
 *       un numéro de portable pour les convocations par SMS (B019), avec son
 *       accord explicite.</li>
 * </ul>
 * Le tournoi est relu depuis son fichier à l'ouverture et à chaque « Actualiser » :
 * les inscriptions en ligne enregistrées entre-temps apparaissent.
 */
final class VueInscriptionsClub {

    private final ContexteApplication contexte;
    private final Path fichier;
    private final Runnable onRetour;
    private Tournoi tournoi;

    private final BorderPane racine = new BorderPane();
    private final VBox zone = new VBox(16);
    private boolean parJoueur;
    private Tableau tableauChoisi;

    VueInscriptionsClub(ContexteApplication contexte, Path fichier, Runnable onRetour) {
        this.contexte = contexte;
        this.fichier = fichier;
        this.onRetour = onRetour;
    }

    Node build() {
        recharger();
        return racine;
    }

    private void recharger() {
        tournoi = contexte.chargerTournoi(fichier);
        if (tableauChoisi != null) {
            String nom = tableauChoisi.getNom();
            tableauChoisi = tournoi.getTableaux().stream().filter(t -> t.getNom().equals(nom)).findFirst().orElse(null);
        }
        afficher();
    }

    private void afficher() {
        Button retour = Ui.boutonLien("← Vue d'ensemble du tournoi");
        retour.setOnAction(e -> onRetour.run());
        Label titre = Ui.titre("Inscriptions");
        Label sous = Ui.discret(tournoi.getNom());

        ToggleGroup g = new ToggleGroup();
        ToggleButton bTab = new ToggleButton("Par tableau");
        ToggleButton bJou = new ToggleButton("Par joueur");
        HBox segment = Ui.segment(g, bTab, bJou);
        (parJoueur ? bJou : bTab).setSelected(true);
        bTab.setOnAction(e -> { parJoueur = false; bTab.setSelected(true); remplirZone(); });
        bJou.setOnAction(e -> { parJoueur = true; bJou.setSelected(true); remplirZone(); });
        Button actualiser = Ui.boutonSecondaire("Actualiser");
        actualiser.setOnAction(e -> recharger());
        Button importer = Ui.boutonSecondaire("Importer des joueurs…");
        importer.setOnAction(e -> racine.setCenter(new EcranImportJoueurs(contexte, fichier, this::recharger,
                this::recharger).build()));
        HBox barre = Ui.ligne(10, segment, Ui.espace(), importer, actualiser);

        VBox page = Ui.page(1060, new VBox(4, retour, titre, sous), resume(), barre, zone);
        remplirZone();
        racine.setCenter(Ui.defilant(page));
    }

    /** Quatre chiffres : participants, inscriptions, liste d'attente, engagements prévus. */
    private Node resume() {
        int inscriptions = 0;
        int attente = 0;
        int places = 0;
        double recettes = 0;
        for (Tableau t : tournoi.getTableaux()) {
            inscriptions += t.getInscrits().size();
            attente += t.getFileAttente().size();
            places += Math.max(0, t.getConfiguration().getPlacesMax());
            recettes += t.getInscrits().size() * RemplissageTableaux.tarifEnLigne(t);
        }
        HBox h = new HBox(12,
                tuile(String.valueOf(RemplissageTableaux.nombreDeParticipants(tournoi)), "participants"),
                tuile(inscriptions + " / " + places, "places occupées"),
                tuile(String.valueOf(attente), "en liste d'attente"),
                tuile(Formats.montant(recettes), "d'engagements (tarif en ligne)"));
        h.getChildren().forEach(n -> HBox.setHgrow(n, Priority.ALWAYS));
        return h;
    }

    static VBox tuile(String chiffre, String libelle) {
        Label c = new Label(chiffre);
        c.getStyleClass().add("tuile-chiffre");
        VBox v = new VBox(2, c, Ui.discret(libelle));
        v.getStyleClass().add("tuile");
        v.setMaxWidth(Double.MAX_VALUE);
        return v;
    }

    private void remplirZone() {
        zone.getChildren().setAll(parJoueur ? vueParJoueur() : vueParTableau());
    }

    // ------------------------------------------------------------------
    // Par tableau : liste des tableaux (jauges) à gauche, détail à droite
    // ------------------------------------------------------------------

    private Node vueParTableau() {
        List<RemplissageTableaux.EtatTableau> etats = RemplissageTableaux.parTableau(tournoi);
        if (etats.isEmpty()) {
            return Ui.carteNue(Ui.discret("Ce tournoi n'a pas encore de tableau."));
        }
        if (tableauChoisi == null) {
            tableauChoisi = etats.get(0).tableau();
        }
        VBox gauche = new VBox(6);
        gauche.setMinWidth(360);
        gauche.setPrefWidth(380);
        RemplissageTableaux.EtatTableau choisi = etats.get(0);
        for (RemplissageTableaux.EtatTableau e : etats) {
            VBox ligne = new VBox(6, Ui.ligne(8, Ui.gras(e.tableau().getNom()), Ui.espace(),
                    Ui.discret(BlocsInscriptions.horaire(e.tableau()))),
                    BlocsInscriptions.jauge(e));
            ligne.getStyleClass().add("choix-tableau");
            if (e.tableau() == tableauChoisi) {
                ligne.getStyleClass().add("choix-tableau-actif");
                choisi = e;
            }
            ligne.setOnMouseClicked(ev -> {
                tableauChoisi = e.tableau();
                remplirZone();
            });
            gauche.getChildren().add(ligne);
        }

        ListView<Object> liste = BlocsInscriptions.liste(choisi, null);
        liste.setPrefHeight(Math.max(260, Math.min(640, 46 * (liste.getItems().size() + 1))));
        VBox detail = Ui.carte(new VBox(4, Ui.titreCarte(choisi.tableau().getNom()),
                        Ui.discret(BlocsInscriptions.sousTitreTableau(choisi.tableau()))),
                BlocsInscriptions.jauge(choisi),
                Ui.aide("Inscrits triés par points de licence (décroissant), puis la liste d'attente dans l'ordre "
                        + "d'arrivée : le premier en attente est repêché au pointage."),
                liste);
        HBox.setHgrow(detail, Priority.ALWAYS);
        HBox h = new HBox(16, gauche, detail);
        h.setAlignment(Pos.TOP_LEFT);
        return h;
    }

    // ------------------------------------------------------------------
    // Par joueur
    // ------------------------------------------------------------------

    private Node vueParJoueur() {
        TextField recherche = Ui.champ("Rechercher un joueur, une licence, un club…");
        HBox.setHgrow(recherche, Priority.ALWAYS);
        ComboBox<String> tri = new ComboBox<>();
        tri.getItems().addAll("Tri par points", "Tri par club");
        tri.setValue("Tri par points");
        Label compteur = Ui.discret("");

        ListView<RemplissageTableaux.Participant> lv = new ListView<>();
        lv.getStyleClass().add("liste-joueurs");
        lv.setPrefHeight(620);
        lv.setCellFactory(x -> new ListCell<>() {
            @Override
            protected void updateItem(RemplissageTableaux.Participant p, boolean vide) {
                super.updateItem(p, vide);
                setText(null);
                if (vide || p == null) {
                    setGraphic(null);
                    return;
                }
                FlowPane puces = new FlowPane(6, 4);
                puces.setPrefWrapLength(320);
                for (Tableau t : p.inscrit()) {
                    puces.getChildren().add(BlocsInscriptions.puceTableau(t, null));
                }
                for (Tableau t : p.enAttente()) {
                    puces.getChildren().add(BlocsInscriptions.puceTableau(t, t.getFileAttente().indexOf(p.joueur()) + 1));
                }
                HBox l = BlocsInscriptions.ligneJoueur("", p.joueur());
                l.getChildren().remove(0);
                puces.setMinWidth(300);
                puces.setMaxWidth(300);
                l.getChildren().add(puces);
                setGraphic(l);
            }
        });

        Runnable maj = () -> {
            List<RemplissageTableaux.Participant> tous = RemplissageTableaux.parJoueur(tournoi,
                    "Tri par club".equals(tri.getValue()) ? RemplissageTableaux.PAR_CLUB : RemplissageTableaux.PAR_POINTS);
            String q = normaliser(recherche.getText());
            List<RemplissageTableaux.Participant> res = new ArrayList<>();
            for (RemplissageTableaux.Participant p : tous) {
                Joueur j = p.joueur();
                if (q.isEmpty() || normaliser(j.getNom() + " " + j.getPrenom() + " " + j.getNumeroLicence() + " "
                        + j.getClub()).contains(q)) {
                    res.add(p);
                }
            }
            lv.getItems().setAll(res);
            compteur.setText(res.size() + (res.size() > 1 ? " participants" : " participant"));
        };
        recherche.textProperty().addListener((o, a, b) -> maj.run());
        tri.valueProperty().addListener((o, a, b) -> maj.run());
        maj.run();
        lv.setPlaceholder(Ui.discret("Aucun participant."));

        VBox panneauSms = new VBox();
        lv.getSelectionModel().selectedItemProperty().addListener((o, a, p) ->
                panneauSms.getChildren().setAll(carteSms(p == null ? null : p.joueur(), null, null)));
        panneauSms.getChildren().setAll(carteSms(null, null, null));
        return new VBox(16, Ui.carte(Ui.ligne(10, recherche, tri, compteur), BlocsInscriptions.legendePuces(), lv),
                panneauSms);
    }

    /**
     * Portable du joueur choisi pour les convocations par SMS (B019) : saisie avec
     * consentement explicite, retrait ; chaque action enregistre aussitôt le tournoi.
     */
    private Node carteSms(Joueur j, String erreur, String succes) {
        if (j == null) {
            return Ui.carte(PresentationSaisieSms.TITRE,
                    Ui.etatVideCompact("Choisissez un joueur dans la liste pour saisir son portable."));
        }
        BlocSaisieSms bloc = new BlocSaisieSms(PresentationSaisieSms.CONSENTEMENT_CLUB, null);
        bloc.montrerEtat(tournoi, j.getNumeroLicence());
        VBox carte = Ui.carte(PresentationSaisieSms.TITRE + " · " + j.getNomComplet());
        if (erreur != null || succes != null) {
            carte.getChildren().add(Ui.message(erreur, succes));
        }
        Button enregistrer = Ui.boutonPrimaire("Enregistrer le numéro");
        enregistrer.setOnAction(e -> {
            ResultatContactSms r = contexte.convocationsSms().enregistrerContact(tournoi, j.getNumeroLicence(),
                    bloc.saisie(), bloc.consenti(), java.time.LocalDateTime.now());
            String echec = r.estReussi() ? sauver() : r.message();
            remplacerCarteSms(carte, j, echec, echec == null ? r.message() : null);
        });
        HBox actions = Ui.actions(enregistrer);
        if (PresentationSaisieSms.aUnNumero(tournoi, j.getNumeroLicence())) {
            Button retirer = Ui.boutonSecondaire("Retirer le numéro");
            retirer.setOnAction(e -> {
                contexte.convocationsSms().retirerContact(tournoi, j.getNumeroLicence());
                String echec = sauver();
                remplacerCarteSms(carte, j, echec, echec == null ? "Numéro retiré." : null);
            });
            actions.getChildren().add(retirer);
        }
        carte.getChildren().addAll(bloc.noeud(), actions);
        carte.getStyleClass().add("carte-sms-club");
        return carte;
    }

    private void remplacerCarteSms(VBox ancienne, Joueur j, String erreur, String succes) {
        if (ancienne.getParent() instanceof VBox parent) {
            parent.getChildren().setAll(carteSms(j, erreur, succes));
        }
    }

    /** Enregistre le tournoi ; renvoie le motif de l'échec, ou {@code null}. */
    private String sauver() {
        try {
            contexte.enregistrerTournoi(tournoi, fichier);
            return null;
        } catch (RuntimeException ex) {
            return "Attention : l'enregistrement a échoué (" + ex.getMessage() + ").";
        }
    }

    private static String normaliser(String s) {
        return Normalizer.normalize(s == null ? "" : s, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).trim();
    }
}
