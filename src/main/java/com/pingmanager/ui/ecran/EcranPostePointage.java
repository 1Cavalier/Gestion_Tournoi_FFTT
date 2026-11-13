package com.pingmanager.ui.ecran;

import com.pingmanager.core.Horloge;
import com.pingmanager.core.JoursPointage;
import com.pingmanager.core.ListePointage;
import com.pingmanager.core.ResultatActionPointage;
import com.pingmanager.core.ServicePostePointage;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.ModeReglement;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.TypeOperationPointage;
import com.pingmanager.persistence.ProjetTournoi;
import com.pingmanager.ui.ContexteApplication;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Écran autonome « Pointage » (B041) : le poste où l'on coche les présents et
 * les paiements, souvent sur un autre ordinateur que celui du juge-arbitre.
 *
 * <p>Recherche toujours active, sélecteur de tableau, filtres d'état, une
 * grosse ligne par engagement (cases Pointé et Payé à gauche), désistement avec
 * motif. Pas de score ni d'accès au reste du tournoi. Le fichier est relu avant
 * chaque action et enregistré juste après (le journal garde l'heure et le
 * poste). Logique dans {@link ListePointage} et {@link ServicePostePointage},
 * textes dans {@link PresentationPostePointage}.</p>
 */
public final class EcranPostePointage {

    private final ContexteApplication contexte;
    private final String poste;
    private final Horloge horloge = Horloge.systeme();
    private final BorderPane racine = new BorderPane();

    private final TextField recherche = Ui.champ("Nom, prénom ou licence…");
    private final ComboBox<String> choixTableau = new ComboBox<>();
    private final ToggleGroup groupeEtat = new ToggleGroup();
    private final VBox liste = new VBox();
    private final HBox tuiles = new HBox();
    private final Label compte = Ui.discret("");
    private final Label message = Ui.message();
    private final Label sousTitre = Ui.discret("");
    /** B043 : un onglet par jour du tournoi, la case « Tous les jours » et le total du tournoi. */
    private final ToggleGroup groupeJour = new ToggleGroup();
    private final HBox ongletsJours = new HBox();
    private final CheckBox tousLesJours = new CheckBox(PresentationPostePointage.TOUS_LES_JOURS);
    private final Label totalTournoi = Ui.discret("");
    private LocalDate jourChoisi;

    private Path fichier;
    private Tournoi tournoi;
    /** B043 : tournoi imposé (groupe « Tournoi en cours »), ou {@code null} : choix parmi ceux du club. */
    private final Path fichierImpose;

    public EcranPostePointage(ContexteApplication contexte) {
        this(contexte, null);
    }

    /** B043 : poste ouvert directement sur le tournoi en cours choisi. */
    public EcranPostePointage(ContexteApplication contexte, Path fichierImpose) {
        this.contexte = contexte;
        this.poste = PresentationPostePointage.identifiantPoste(System.getenv());
        this.fichierImpose = fichierImpose;
    }

    public Parent build() {
        List<ProjetTournoi> tournois = fichierImpose != null ? List.of(new ProjetTournoi(fichierImpose, "", java.time.LocalDateTime.now()))
                : tournoisDuCompte();
        if (tournois.isEmpty()) {
            racine.setCenter(Ui.page(1040, Ui.titre(PresentationPostePointage.TITRE),
                    Ui.etatVide(PresentationPostePointage.AUCUN_TOURNOI, null)));
            return racine;
        }
        fichier = parDefaut(tournois);
        racine.getStyleClass().add("pointage-poste");
        racine.setTop(entete(tournois));
        ScrollPane defil = new ScrollPane(liste);
        defil.setFitToWidth(true);
        defil.getStyleClass().add("pointage-defilement");
        liste.getStyleClass().add("pointage-liste");
        racine.setCenter(defil);
        recharger(true);
        return racine;
    }

    // ------------------------------------------------------------------
    // En-tête : tournoi, tuiles, recherche et filtres
    // ------------------------------------------------------------------

    private Node entete(List<ProjetTournoi> tournois) {
        VBox titres = new VBox(4, Ui.titre(PresentationPostePointage.TITRE), sousTitre);
        HBox haut;
        if (tournois.size() > 1) {
            ComboBox<ProjetTournoi> choixTournoi = new ComboBox<>();
            choixTournoi.getItems().setAll(tournois);
            choixTournoi.setConverter(convertisseur(p -> nomTournoi(p.getFichier())));
            choixTournoi.getSelectionModel().select(tournois.stream()
                    .filter(p -> p.getFichier().equals(fichier)).findFirst().orElse(tournois.get(0)));
            choixTournoi.setOnAction(e -> {
                fichier = choixTournoi.getValue().getFichier();
                choixTableau.getItems().clear();
                jourChoisi = null;
                recharger(true);
            });
            haut = Ui.enteteAvecActions(titres, choixTournoi);
        } else {
            haut = Ui.enteteAvecActions(titres);
        }

        tuiles.getStyleClass().add("pointage-tuiles");
        tuiles.setSpacing(12);

        recherche.getStyleClass().add("pointage-recherche");
        HBox.setHgrow(recherche, Priority.ALWAYS);
        recherche.textProperty().addListener((o, a, n) -> afficherListe());
        recherche.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                recherche.clear();
            }
        });
        choixTableau.getStyleClass().add("pointage-choix-tableau");
        choixTableau.setOnAction(e -> {
            afficherTuiles();
            afficherListe();
            focaliserRecherche();
        });

        List<ToggleButton> boutons = new ArrayList<>();
        for (ListePointage.FiltreEtat f : ListePointage.FiltreEtat.values()) {
            ToggleButton b = new ToggleButton(f.getLibelle());
            b.setUserData(f);
            b.setSelected(f == ListePointage.FiltreEtat.TOUS);
            boutons.add(b);
        }
        HBox filtres = Ui.segment(groupeEtat, boutons.toArray(new ToggleButton[0]));
        groupeEtat.selectedToggleProperty().addListener((o, a, n) -> {
            if (n == null) {
                a.setSelected(true); // toujours un filtre choisi
                return;
            }
            afficherListe();
            focaliserRecherche();
        });

        tousLesJours.getStyleClass().add("pointage-tous-les-jours");
        tousLesJours.setOnAction(e -> {
            afficherListe();
            focaliserRecherche();
        });
        ongletsJours.getStyleClass().add("pointage-jours");
        totalTournoi.getStyleClass().add("pointage-total-tournoi");

        HBox barre = Ui.ligne(12, recherche, choixTableau, filtres, tousLesJours);
        barre.getStyleClass().add("pointage-barre");

        VBox v = new VBox(12, haut, ongletsJours, tuiles, totalTournoi, barre, Ui.aide(PresentationPostePointage.AIDE), Ui.ligne(12, compte),
                message);
        v.getStyleClass().add("pointage-entete");
        return v;
    }

    // ------------------------------------------------------------------
    // Données
    // ------------------------------------------------------------------

    /** Relit le fichier du tournoi (un autre écran a pu le modifier), puis réaffiche. */
    private void recharger(boolean focaliser) {
        try {
            tournoi = contexte.chargerTournoi(fichier);
        } catch (RuntimeException e) {
            Ui.erreur(message, "Lecture du tournoi impossible : " + e.getMessage());
            return;
        }
        sousTitre.setText(PresentationPostePointage.sousTitre(tournoi.getNom(), poste));
        ListePointage lp = new ListePointage(tournoi);
        List<LocalDate> jours = lp.jours();
        if (jourChoisi == null || !jours.contains(jourChoisi)) {
            jourChoisi = JoursPointage.jourParDefaut(tournoi, java.time.LocalDateTime.now());
        }
        afficherOngletsJours(lp, jours);
        String choix = choixTableau.getValue();
        List<String> noms = new ArrayList<>();
        noms.add(PresentationPostePointage.TOUS);
        noms.addAll(lp.nomsTableaux(jourChoisi));
        if (!choixTableau.getItems().equals(noms)) {
            choixTableau.getItems().setAll(noms);
        }
        choixTableau.setValue(choix != null && noms.contains(choix) ? choix : PresentationPostePointage.TOUS);
        afficherTuiles();
        afficherListe();
        if (focaliser) {
            focaliserRecherche();
        }
    }

    /** B043 : un onglet par jour (seulement si le tournoi en a plusieurs), avec ses non-pointés. */
    private void afficherOngletsJours(ListePointage lp, List<LocalDate> jours) {
        ongletsJours.getChildren().clear();
        boolean plusieurs = jours.size() > 1;
        ongletsJours.setVisible(plusieurs);
        ongletsJours.setManaged(plusieurs);
        tousLesJours.setVisible(plusieurs);
        tousLesJours.setManaged(plusieurs);
        if (!plusieurs) {
            tousLesJours.setSelected(false);
            return;
        }
        List<ToggleButton> boutons = new ArrayList<>();
        for (LocalDate j : jours) {
            ToggleButton b = new ToggleButton(PresentationPostePointage.ongletJour(j, lp.nonPointes(j)));
            b.setUserData(j);
            b.getStyleClass().add("pointage-onglet-jour");
            b.setSelected(j.equals(jourChoisi));
            b.setOnAction(e -> {
                if (!b.isSelected()) {
                    b.setSelected(true); // toujours un jour choisi
                    return;
                }
                jourChoisi = j;
                choixTableau.setValue(PresentationPostePointage.TOUS);
                recharger(true);
            });
            boutons.add(b);
        }
        ongletsJours.getChildren().add(Ui.segment(groupeJour, boutons.toArray(new ToggleButton[0])));
    }

    private void afficherTuiles() {
        if (tournoi == null) {
            return;
        }
        ListePointage lp = new ListePointage(tournoi);
        totalTournoi.setText(PresentationPostePointage.totalTournoi(lp.totalGeneral()));
        String nom = PresentationPostePointage.nomTableau(choixTableau.getValue());
        ListePointage.Totaux duJour = lp.totalJour(jourChoisi);
        ListePointage.Totaux t = nom == null ? duJour
                : lp.totaux().stream().filter(x -> x.tableau().equals(nom)).findFirst().orElse(duJour);
        tuiles.getChildren().setAll(
                Ui.tuileChiffre(String.valueOf(t.inscrits()), "Inscrits"),
                Ui.tuileChiffre(PresentationPostePointage.proportion(t.pointes(), t.inscrits()), "Pointés"),
                Ui.tuileChiffre(PresentationPostePointage.proportion(t.payes(), t.inscrits()), "Payés"),
                Ui.tuileChiffre(com.pingmanager.core.reglement.Formats.montant(t.encaisse()), "Encaissé"),
                Ui.tuileChiffre(com.pingmanager.core.reglement.Formats.montant(t.enAttente()), "En attente"));
    }

    private void afficherListe() {
        if (tournoi == null) {
            return;
        }
        ListePointage.FiltreEtat etat = groupeEtat.getSelectedToggle() == null ? ListePointage.FiltreEtat.TOUS
                : (ListePointage.FiltreEtat) groupeEtat.getSelectedToggle().getUserData();
        // B043 : le jour choisi, ou tous les jours si la case est cochée (recherche étendue).
        LocalDate jour = tousLesJours.isSelected() ? null : jourChoisi;
        List<ListePointage.Ligne> lignes = new ListePointage(tournoi).lignes(recherche.getText(),
                PresentationPostePointage.nomTableau(choixTableau.getValue()), etat, jour);
        int n = Math.min(lignes.size(), PresentationPostePointage.LIMITE_LIGNES);
        compte.setText(PresentationPostePointage.compte(n, lignes.size()));
        List<Node> noeuds = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            noeuds.add(ligneJoueur(lignes.get(i), i == 0));
        }
        if (lignes.isEmpty()) {
            noeuds.add(Ui.etatVideCompact("Aucun joueur ne correspond à la recherche et aux filtres."));
        }
        liste.getChildren().setAll(noeuds);
    }

    // ------------------------------------------------------------------
    // Lignes
    // ------------------------------------------------------------------

    private Node ligneJoueur(ListePointage.Ligne l, boolean premiere) {
        Joueur j = l.joueur();
        VBox bloc = new VBox();
        bloc.getStyleClass().add("pointage-joueur");
        if (premiere) {
            bloc.getStyleClass().add("pointage-joueur-premier");
        }
        boolean premierEngagement = true;
        for (ListePointage.Engagement e : l.engagements()) {
            bloc.getChildren().add(ligneEngagement(bloc, j, e, premierEngagement));
            premierEngagement = false;
        }
        return bloc;
    }

    private Node ligneEngagement(VBox bloc, Joueur j, ListePointage.Engagement e, boolean premier) {
        CheckBox pointe = new CheckBox("Pointé");
        pointe.getStyleClass().addAll("pointage-case", "pointage-case-pointe");
        pointe.setSelected(e.pointe());
        pointe.setDisable(!e.modifiable());
        pointe.setOnAction(x -> agir(j, e.tableau(), pointe.isSelected() ? TypeOperationPointage.POINTER
                : TypeOperationPointage.DEPOINTER, null));

        ComboBox<ModeReglement> mode = new ComboBox<>();
        mode.getItems().setAll(ModeReglement.values());
        mode.setConverter(convertisseur(ModeReglement::getLibelle));
        // Payé sans mode (noté avant B041) : on ne prétend pas savoir comment.
        mode.setPromptText(PresentationPostePointage.MODE_NON_NOTE);
        mode.setValue(e.paye() ? e.mode() : PresentationPostePointage.MODE_PAR_DEFAUT);
        mode.getStyleClass().add("pointage-mode");

        CheckBox paye = new CheckBox("Payé");
        paye.getStyleClass().addAll("pointage-case", "pointage-case-paye");
        paye.setSelected(e.paye());
        paye.setOnAction(x -> agir(j, e.tableau(), paye.isSelected() ? TypeOperationPointage.MARQUER_PAYE
                : TypeOperationPointage.MARQUER_NON_PAYE,
                mode.getValue() != null ? mode.getValue() : PresentationPostePointage.MODE_PAR_DEFAUT));
        mode.setOnAction(x -> {
            if (paye.isSelected() && mode.getValue() != null && mode.getValue() != e.mode()) {
                agir(j, e.tableau(), TypeOperationPointage.MARQUER_PAYE, mode.getValue());
            }
        });

        HBox cases = Ui.ligne(16, pointe, paye);
        cases.getStyleClass().add("pointage-cases");

        Label nom = Ui.label(premier ? PresentationPostePointage.nomAffiche(j) : "", "pointage-nom");
        Label detail = Ui.label(premier ? PresentationPostePointage.detail(j) : "", "pointage-detail");
        VBox identite = new VBox(2, nom, detail);
        identite.getStyleClass().add("pointage-identite");
        if (!premier) {
            identite.getStyleClass().add("pointage-identite-suite");
        }
        HBox.setHgrow(identite, Priority.ALWAYS);

        FlowPane etiquettes = new FlowPane(6, 6);
        etiquettes.getStyleClass().add("pointage-etiquettes");
        etiquettes.getChildren().add(Ui.pastille(PresentationPostePointage.libelleTableau(e.tableau()), StatutPastille.INFO));
        if (tousLesJours.isVisible() && e.jour() != null) {
            Label pJour = Ui.pastille(PresentationPostePointage.jour(e.jour()), StatutPastille.NEUTRE);
            pJour.getStyleClass().add("pointage-pastille-jour");
            etiquettes.getChildren().add(pJour);
        }
        if (e.jourNonDefini() && tousLesJours.isVisible()) {
            etiquettes.getChildren().add(Ui.pastille(PresentationPostePointage.JOUR_NON_DEFINI,
                    StatutPastille.ATTENTION));
        }
        if (e.payeEnLigne()) {
            etiquettes.getChildren().add(Ui.pastille("Payé en ligne", StatutPastille.SUCCES));
        }
        if (!e.modifiable()) {
            etiquettes.getChildren().add(Ui.pastille("Tableau lancé", StatutPastille.NEUTRE));
        } else if (!e.desistementPossible()) {
            etiquettes.getChildren().add(Ui.pastille("Pointage clôturé", StatutPastille.NEUTRE));
        }

        Label montant = Ui.label(PresentationPostePointage.montant(e), "pointage-montant");

        Button desinscrire = Ui.boutonSecondaire("Désinscrire…");
        desinscrire.getStyleClass().add("pointage-desinscrire");
        desinscrire.setDisable(!e.desistementPossible());
        VBox conteneur = new VBox();
        desinscrire.setOnAction(x -> confirmerDesistement(conteneur, j, e.tableau()));

        HBox ligne = Ui.ligne(16, cases, identite, etiquettes, mode, montant, desinscrire);
        ligne.getStyleClass().add("pointage-engagement");
        if (!premier) {
            ligne.getStyleClass().add("pointage-engagement-suite");
        }
        conteneur.getChildren().add(ligne);
        return conteneur;
    }

    private void confirmerDesistement(VBox conteneur, Joueur j, String tableau) {
        if (conteneur.getChildren().size() > 1) {
            return; // encart déjà ouvert
        }
        TextField motif = Ui.champ("Motif (obligatoire) : blessure, absent prévenu…");
        motif.getStyleClass().add("pointage-motif");
        Button confirmer = Ui.boutonPrimaire("Confirmer le désistement");
        Button renoncer = Ui.boutonSecondaire("Renoncer");
        VBox encart = Ui.encartConfirmation(PresentationPostePointage.confirmationDesistement(j, tableau),
                confirmer, renoncer);
        encart.getChildren().add(1, motif);
        encart.getStyleClass().add("pointage-encart-desistement");
        confirmer.disableProperty().bind(motif.textProperty().isEmpty());
        confirmer.setOnAction(x -> agir(j, tableau, TypeOperationPointage.DESINSCRIRE, motif.getText()));
        motif.setOnAction(x -> {
            if (!motif.getText().isBlank()) {
                agir(j, tableau, TypeOperationPointage.DESINSCRIRE, motif.getText());
            }
        });
        renoncer.setOnAction(x -> {
            conteneur.getChildren().remove(encart);
            focaliserRecherche();
        });
        conteneur.getChildren().add(encart);
        Platform.runLater(motif::requestFocus);
    }

    // ------------------------------------------------------------------
    // Actions : relire, appliquer, enregistrer
    // ------------------------------------------------------------------

    private void agir(Joueur j, String tableau, TypeOperationPointage type, Object valeur) {
        Tournoi frais;
        try {
            frais = contexte.chargerTournoi(fichier); // une autre fenêtre a pu changer le tournoi
        } catch (RuntimeException e) {
            Ui.erreur(message, "Lecture du tournoi impossible : " + e.getMessage());
            return;
        }
        ServicePostePointage service = new ServicePostePointage(frais, horloge, poste);
        String licence = j.getNumeroLicence();
        ResultatActionPointage r;
        if (type == TypeOperationPointage.POINTER) {
            r = service.pointer(tableau, licence);
        } else if (type == TypeOperationPointage.DEPOINTER) {
            r = service.depointer(tableau, licence);
        } else if (type == TypeOperationPointage.MARQUER_PAYE) {
            r = service.marquerPaye(tableau, licence, (ModeReglement) valeur);
        } else if (type == TypeOperationPointage.MARQUER_NON_PAYE) {
            r = service.marquerNonPaye(tableau, licence);
        } else {
            r = service.desinscrire(tableau, licence, (String) valeur);
        }
        if (r.aModifie()) {
            try {
                contexte.enregistrerTournoi(frais, fichier);
            } catch (RuntimeException e) {
                Ui.erreur(message, "Enregistrement impossible : " + e.getMessage() + " Rien n'a été modifié.");
                recharger(true);
                return;
            }
        }
        String texte = PresentationPostePointage.message(r, j, tableau, type);
        if (r.estAcceptee()) {
            Ui.succes(message, texte);
        } else {
            Ui.erreur(message, texte);
        }
        recharger(true);
    }

    private void focaliserRecherche() {
        Platform.runLater(() -> {
            recherche.requestFocus();
            recherche.selectAll();
        });
    }

    // ------------------------------------------------------------------
    // Tournois du compte
    // ------------------------------------------------------------------

    private List<ProjetTournoi> tournoisDuCompte() {
        List<ProjetTournoi> res = new ArrayList<>();
        for (ProjetTournoi p : contexte.tournoisRecents()) {
            try {
                if (contexte.estTournoiDuCompte(contexte.lireTournoi(p.getFichier()))) {
                    res.add(p);
                }
            } catch (RuntimeException ignore) {
                // Fichier déplacé ou illisible : on ne le propose pas.
            }
        }
        return res;
    }

    /** Le « tournoi actuel » de l'accueil de la gestion, sinon le plus récent. */
    private Path parDefaut(List<ProjetTournoi> tournois) {
        List<PresentationGestionTournois.Fiche> fiches = new ArrayList<>();
        for (ProjetTournoi p : tournois) {
            fiches.add(PresentationGestionTournois.Fiche.depuis(contexte.lireTournoi(p.getFichier()), p.getFichier()));
        }
        Optional<PresentationGestionTournois.Actuel> actuel =
                new PresentationGestionTournois(fiches, LocalDate.now()).actuel();
        return actuel.map(a -> a.fiche().fichier()).orElse(tournois.get(0).getFichier());
    }

    private String nomTournoi(Path f) {
        try {
            String nom = contexte.lireTournoi(f).getNom();
            return nom == null || nom.isBlank() ? "Sans titre" : nom;
        } catch (RuntimeException e) {
            return f.getFileName().toString();
        }
    }

    private static <T> StringConverter<T> convertisseur(Function<T, String> texte) {
        return new StringConverter<>() {
            @Override
            public String toString(T t) {
                return t == null ? "" : texte.apply(t);
            }

            @Override
            public T fromString(String s) {
                return null;
            }
        };
    }
}
