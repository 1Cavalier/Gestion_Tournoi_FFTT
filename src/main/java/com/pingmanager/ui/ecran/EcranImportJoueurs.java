package com.pingmanager.ui.ecran;

import com.pingmanager.core.ImportJoueurs;
import com.pingmanager.core.ImportJoueurs.Champ;
import com.pingmanager.core.reglement.Formats;
import com.pingmanager.importation.LecteurTableur;
import com.pingmanager.importation.ModeleImport;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.ui.ContexteApplication;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Import d'une liste de joueurs dans les tableaux d'un tournoi (club qui a tout préparé
 * hors de PingManager). Fichier Excel (.xlsx), LibreOffice (.ods), CSV ou JSON ; colonnes
 * obligatoires Nom, Prénom, Points, Club ; facultatives Licence, Sexe, Tableau, Date de
 * naissance. Aperçu complet (par tableau : nouveaux ou existants, inscrits, liste
 * d'attente, refus et motifs, lignes en erreur) avant d'importer quoi que ce soit.
 */
final class EcranImportJoueurs {

    private static final String NOUVEAU = "Nouveau tableau…";

    private final ContexteApplication contexte;
    private final Path fichierTournoi;
    private final Runnable onRetour;
    private final Runnable onInscriptions;
    private final Tournoi tournoi;
    private final BorderPane racine = new BorderPane();

    private LecteurTableur.Format format;
    /** Feuille « Tableaux » du classeur (définitions), ou {@code null}. */
    private com.pingmanager.core.ImportTableaux.Analyse definitions;
    private String descriptionFeuilles = "";
    private String nomFichier;
    private ImportJoueurs.Analyse analyse;
    private ImportJoueurs.Plan plan;
    private String erreurLecture;
    private final Map<String, ComboBox<LocalDate>> jours = new LinkedHashMap<>();
    private final Map<String, TextField> heures = new LinkedHashMap<>();

    // Destination.
    private final ToggleGroup destination = new ToggleGroup();
    private final RadioButton selonFichier = new RadioButton("Selon la colonne « Tableau » du fichier");
    private final RadioButton unTableau = new RadioButton("Tous dans le tableau :");
    private final ComboBox<String> tableauChoisi = new ComboBox<>();
    private final TextField nomNouveau = Ui.champ("nom du nouveau tableau (ex. Open)");
    private final CheckBox creer = new CheckBox("Créer les tableaux du fichier qui n'existent pas encore");
    private final VBox zoneApercu = new VBox(16);
    private final Label labelFichier = Ui.discret("Aucun fichier choisi.");

    EcranImportJoueurs(ContexteApplication contexte, Path fichierTournoi, Runnable onRetour, Runnable onInscriptions) {
        this.contexte = contexte;
        this.fichierTournoi = fichierTournoi;
        this.onRetour = onRetour;
        this.onInscriptions = onInscriptions;
        this.tournoi = contexte.chargerTournoi(fichierTournoi);
    }

    /** Dernier écran construit (harnais de captures, même paquet). */
    static EcranImportJoueurs dernier;

    Node build() {
        dernier = this;
        Button retour = Ui.boutonLien("← Retour");
        retour.setOnAction(e -> onRetour.run());
        Label titre = Ui.titre("Importer des joueurs");
        Label sous = Ui.discret(tournoi.getNom() + " — pour un tournoi préparé hors de PingManager, ou pour "
                + "compléter des tableaux avec une liste d'inscrits.");
        sous.setWrapText(true);
        VBox page = Ui.page(900, new VBox(4, retour, titre, sous), carteFichier(), carteDestination(), zoneApercu);
        racine.setCenter(Ui.defilant(page));
        majApercu();
        return racine;
    }

    // ------------------------------------------------------------------

    private Node carteFichier() {
        FlowPane obligatoires = new FlowPane(6, 6);
        FlowPane facultatives = new FlowPane(6, 6);
        for (Champ c : Champ.values()) {
            Label b = Ui.badge(c.getLibelle());
            b.getStyleClass().add(c.estObligatoire() ? "puce-tableau" : "etat-optionnel");
            (c.estObligatoire() ? obligatoires : facultatives).getChildren().add(b);
        }
        Button choisir = Ui.boutonPrimaire("Choisir un fichier…");
        choisir.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Liste des joueurs à importer");
            fc.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Tableurs et listes (xlsx, ods, csv, json)",
                            "*.xlsx", "*.xlsm", "*.ods", "*.csv", "*.txt", "*.json"),
                    new FileChooser.ExtensionFilter("Tous les fichiers", "*.*"));
            File f = fc.showOpenDialog(fenetre());
            if (f != null) {
                charger(f.toPath());
            }
        });
        Button modeleXlsx = Ui.boutonSecondaire("Modèle Excel…");
        modeleXlsx.setOnAction(e -> enregistrerModele(true));
        Button modeleCsv = Ui.boutonSecondaire("Modèle CSV…");
        modeleCsv.setOnAction(e -> enregistrerModele(false));
        Label choisi = labelFichier;
        return Ui.carte("1. Le fichier",
                Ui.texte("Le plus simple : le modèle Excel, avec une feuille « Tableaux » (nom, points imposés, public, "
                        + "jour, heure, places) et une feuille « Joueurs » (une ligne par joueur, ses tableaux en "
                        + "colonnes Tableau 1 à 3, listes déroulantes). Un simple fichier de joueurs (CSV…) convient "
                        + "aussi : les tableaux absents sont alors créés « toutes séries »."),
                Ui.ligne(10, Ui.gras("Joueurs — obligatoires"), obligatoires),
                Ui.ligne(10, Ui.gras("Facultatives"), facultatives),
                Ui.aide("Points et club sont indispensables au tirage au sort et à la formation des poules "
                        + "(classement, séparation des clubs, I.302). Sans licence, le joueur reçoit un identifiant "
                        + "interne stable (nom + prénom + club). Formats : Excel .xlsx, LibreOffice .ods, CSV "
                        + "(« ; », « , » ou tabulation), JSON. L'ancien .xls doit être réenregistré en .xlsx."),
                Ui.ligne(10, choisir, modeleXlsx, modeleCsv), choisi);
    }

    private Node carteDestination() {
        selonFichier.setToggleGroup(destination);
        unTableau.setToggleGroup(destination);
        selonFichier.setSelected(true);
        tableauChoisi.getItems().clear();
        for (Tableau t : tournoi.getTableaux()) {
            tableauChoisi.getItems().add(t.getNom());
        }
        tableauChoisi.getItems().add(NOUVEAU);
        tableauChoisi.setValue(tournoi.getTableaux().isEmpty() ? NOUVEAU : tournoi.getTableaux().get(0).getNom());
        nomNouveau.setPrefWidth(240);
        creer.setSelected(true);
        Runnable maj = () -> {
            boolean nouveau = NOUVEAU.equals(tableauChoisi.getValue());
            nomNouveau.setVisible(nouveau);
            nomNouveau.setManaged(nouveau);
            tableauChoisi.setDisable(!unTableau.isSelected());
            nomNouveau.setDisable(!unTableau.isSelected());
            creer.setDisable(!selonFichier.isSelected());
            majApercu();
        };
        destination.selectedToggleProperty().addListener((o, a, b) -> maj.run());
        tableauChoisi.valueProperty().addListener((o, a, b) -> maj.run());
        nomNouveau.textProperty().addListener((o, a, b) -> majApercu());
        creer.selectedProperty().addListener((o, a, b) -> majApercu());
        maj.run();
        return Ui.carte("2. Les tableaux",
                selonFichier, Ui.ligne(10, unTableau, tableauChoisi, nomNouveau), creer,
                Ui.aide("Les noms du fichier sont rapprochés des tableaux existants (nom exact ou lettre : « A » → "
                        + "« A – ≤ 799 pts ») et aux tableaux de la feuille « Tableaux » du classeur, créés avec leurs points "
                        + "imposés. Un tableau absent de cette feuille est créé « toutes séries », dimensionné sur sa liste et "
                        + "gratuit ; points, horaires et tarifs se règlent ensuite dans « Tableaux et inscriptions ». "
                        + "Dans un tableau existant, ses conditions (points, sexe, catégories) et sa capacité "
                        + "s'appliquent : le surplus passe en liste d'attente."));
    }

    // ------------------------------------------------------------------

    /** Charge un fichier (aussi utilisé par le harnais de captures). */
    void charger(Path f) {
        nomFichier = f.getFileName().toString();
        try {
            LecteurTableur.Classeur c = LecteurTableur.lireClasseur(f);
            format = c.format();
            // Classeur modèle : feuille « Tableaux » (définitions) + feuille « Joueurs ».
            List<List<String>> feuilleTableaux = c.feuille("tableau");
            List<List<String>> feuilleJoueurs = c.feuille("joueur");
            String nomJoueurs = c.nomFeuille("joueur");
            if (feuilleJoueurs == null) {
                for (var e : c.feuilles().entrySet()) {
                    String n = e.getKey().toLowerCase();
                    if (!n.contains("tableau") && !n.contains("liste")) {
                        feuilleJoueurs = e.getValue();
                        nomJoueurs = e.getKey();
                        break;
                    }
                }
            }
            definitions = feuilleTableaux == null ? null : com.pingmanager.core.ImportTableaux.analyser(feuilleTableaux);
            if (definitions != null && !definitions.enteteTrouvee()) {
                definitions = null;
            }
            analyse = feuilleJoueurs == null ? null : ImportJoueurs.analyser(feuilleJoueurs);
            descriptionFeuilles = (definitions == null ? "" : "feuille « " + c.nomFeuille("tableau") + " » : "
                    + definitions.definitions().size() + " tableau" + (definitions.definitions().size() > 1 ? "x" : "")
                    + " défini" + (definitions.definitions().size() > 1 ? "s" : "") + " · ")
                    + (nomJoueurs == null ? "pas de feuille de joueurs" : "feuille « " + nomJoueurs + " »");
            erreurLecture = null;
            boolean colonneTableau = analyse != null && !analyse.colonnesTableau().isEmpty();
            if (!colonneTableau && definitions == null) {
                unTableau.setSelected(true);
            } else {
                selonFichier.setSelected(true);
            }
        } catch (LecteurTableur.ImportException e) {
            format = null;
            analyse = null;
            definitions = null;
            erreurLecture = e.getMessage();
        }
        labelFichier.setText("Fichier : " + nomFichier);
        majApercu();
    }

    private ImportJoueurs.Options options() {
        String parDefaut = null;
        if (unTableau.isSelected()) {
            parDefaut = NOUVEAU.equals(tableauChoisi.getValue()) ? nomNouveau.getText() : tableauChoisi.getValue();
        }
        boolean creation = unTableau.isSelected() ? NOUVEAU.equals(tableauChoisi.getValue()) : creer.isSelected();
        return new ImportJoueurs.Options(parDefaut, creation);
    }

    private ImportJoueurs.Analyse analyseEffective() {
        if (analyse == null || !unTableau.isSelected()) {
            return analyse;
        }
        // « Tous dans le tableau » : la colonne Tableau du fichier est ignorée.
        List<ImportJoueurs.LigneJoueur> l = new ArrayList<>();
        for (ImportJoueurs.LigneJoueur x : analyse.lignes()) {
            l.add(new ImportJoueurs.LigneJoueur(x.ligne(), x.nom(), x.prenom(), x.points(), x.club(), x.licence(),
                    x.sexe(), List.of(), x.naissance(), x.categorie()));
        }
        return new ImportJoueurs.Analyse(analyse.entetes(), analyse.colonnes(), analyse.manquantes(), l,
                analyse.erreurs(), analyse.avertissements(), analyse.colonnesTableau());
    }

    private void majApercu() {
        zoneApercu.getChildren().clear();
        if (erreurLecture != null) {
            zoneApercu.getChildren().add(bandeau(erreurLecture));
            return;
        }
        if (format == null) {
            return;
        }
        boolean avecTableaux = definitions != null && !definitions.definitions().isEmpty();
        if (analyse == null || !analyse.colonnesCompletes()) {
            if (!avecTableaux) {
                if (analyse == null) {
                    zoneApercu.getChildren().add(bandeau("Aucune feuille de joueurs ni de tableaux reconnue."));
                    return;
                }
                StringBuilder sb = new StringBuilder();
                analyse.manquantes().forEach(c -> sb.append(sb.length() > 0 ? ", " : "").append(c.getLibelle()));
                zoneApercu.getChildren().add(bandeau("Colonnes obligatoires absentes : " + sb
                        + ". En-têtes lus : « " + String.join(" | ", analyse.entetes()) + " »."));
                return;
            }
            // Feuille Tableaux seule (joueurs pas encore saisis) : on crée seulement les tableaux.
            analyse = new ImportJoueurs.Analyse(List.of(), java.util.Map.of(), List.of(), List.of(), List.of(),
                    List.of(), List.of());
        }
        plan = ImportJoueurs.planifier(tournoi, analyseEffective(), options(),
                definitions == null ? List.of() : definitions.definitions());

        // Colonnes repérées.
        FlowPane cols = new FlowPane(6, 6);
        for (Champ c : Champ.values()) {
            boolean ok = c == Champ.TABLEAU ? !analyse.colonnesTableau().isEmpty() : analyse.colonnes().containsKey(c);
            cols.getChildren().add(Ui.pastille((ok ? "✓ " : "— ") + c.getLibelle(), ok ? Ui.ETAT_OK : Ui.ETAT_OPTIONNEL));
        }
        VBox apercu = Ui.carte("3. Aperçu",
                Ui.discret(format.getLibelle() + " · " + descriptionFeuilles),
                Ui.discret(analyse.lignes().size() + " joueur" + (analyse.lignes().size() > 1 ? "s" : "")
                        + " valide" + (analyse.lignes().size() > 1 ? "s" : "")
                        + (analyse.erreurs().isEmpty() ? "" : " · " + analyse.erreurs().size() + " ligne"
                        + (analyse.erreurs().size() > 1 ? "s" : "") + " en erreur")),
                cols);

        // Par tableau.
        jours.clear();
        heures.clear();
        List<LocalDate> joursTournoi = new ArrayList<>(tournoi.getJours());
        for (ImportJoueurs.PlanTableau p : plan.tableaux().values()) {
            Label nom = Ui.gras(p.nom());
            Label etat = p.estNouveau() ? Ui.pastille("Nouveau", Ui.ETAT_RECOMMANDE) : Ui.badge("Existant");
            String regles = p.definition() != null ? p.definition().libellePoints()
                    + (p.definition().sexe() == null ? "" : p.definition().sexe() == com.pingmanager.model.Sexe.FEMME
                    ? " · dames" : " · messieurs")
                    + (p.definition().places() == null ? "" : " · " + p.definition().places() + " places") + " · "
                    : p.estNouveau() ? "Toutes séries (non défini dans la feuille Tableaux) · " : "";
            String resume = regles + "+" + p.inscrits().size() + " inscrit" + (p.inscrits().size() > 1 ? "s" : "")
                    + (p.attente().isEmpty() ? "" : " · " + p.attente().size() + " en liste d'attente")
                    + (p.refus().isEmpty() ? "" : " · " + p.refus().size() + " refusé" + (p.refus().size() > 1 ? "s" : ""));
            VBox gauche = new VBox(2, Ui.ligne(8, nom, etat), Ui.discret(resume));
            HBox.setHgrow(gauche, Priority.ALWAYS);
            HBox ligne = Ui.ligneListe(gauche);
            if (p.estNouveau()) {
                ComboBox<LocalDate> jour = new ComboBox<>();
                jour.getItems().addAll(joursTournoi);
                jour.setConverter(new javafx.util.StringConverter<>() {
                    @Override
                    public String toString(LocalDate d) {
                        return d == null ? "" : Formats.jourCourt(d);
                    }

                    @Override
                    public LocalDate fromString(String s) {
                        return null;
                    }
                });
                jour.setPromptText("Jour");
                if (joursTournoi.size() == 1) {
                    jour.setValue(joursTournoi.get(0));
                }
                TextField heure = Ui.champ("heure, ex. 9h");
                heure.setPrefWidth(110);
                if (p.definition() != null) {
                    if (p.definition().jour() != null && joursTournoi.contains(p.definition().jour())) {
                        jour.setValue(p.definition().jour());
                    }
                    if (p.definition().heure() != null) {
                        heure.setText(Saisie.heure(p.definition().heure()));
                    }
                }
                jours.put(p.nom(), jour);
                heures.put(p.nom(), heure);
                ligne.getChildren().addAll(jour, heure);
            }
            apercu.getChildren().add(ligne);
        }
        if (plan.tableaux().isEmpty()) {
            apercu.getChildren().add(Ui.discret("Aucun tableau à alimenter : choisissez un tableau ou cochez la création."));
        }

        // Problèmes.
        List<ImportJoueurs.Probleme> bloquants = new ArrayList<>(analyse.erreurs());
        if (definitions != null) {
            definitions.erreurs().forEach(x -> bloquants.add(new ImportJoueurs.Probleme(x.ligne(), "Tableaux — " + x.message())));
        }
        bloquants.addAll(plan.sansTableau());
        plan.tableaux().values().forEach(p -> bloquants.addAll(p.refus()));
        if (!bloquants.isEmpty()) {
            apercu.getChildren().add(listeProblemes("Lignes non importées (" + bloquants.size() + ")", bloquants, "motif-refus"));
        }
        List<ImportJoueurs.Probleme> aVerifier = new ArrayList<>(analyse.avertissements());
        if (definitions != null) {
            definitions.avertissements().forEach(x -> aVerifier.add(new ImportJoueurs.Probleme(x.ligne(), "Tableaux — " + x.message())));
        }
        aVerifier.addAll(plan.remarques());
        if (!aVerifier.isEmpty()) {
            apercu.getChildren().add(listeProblemes("À vérifier (" + aVerifier.size() + ")", aVerifier, "texte"));
        }

        int n = plan.nombreAjouts();
        long nouveaux = plan.tableaux().values().stream().filter(ImportJoueurs.PlanTableau::estNouveau).count();
        String libelle = (nouveaux > 0 ? nouveaux + " tableau" + (nouveaux > 1 ? "x" : "") : "")
                + (nouveaux > 0 && n > 0 ? " et " : "") + (n > 0 ? n + " inscription" + (n > 1 ? "s" : "") : "");
        Button importer = Ui.boutonPrimaire(n == 0 && nouveaux == 0 ? "Rien à importer" : "Importer " + libelle);
        importer.setDisable(n == 0 && nouveaux == 0);
        Label message = Ui.message();
        importer.setOnAction(e -> importer(message));
        HBox pied = Ui.ligne(12, Ui.discret("Rien n'est enregistré avant de cliquer."), Ui.espace(), importer);
        pied.getStyleClass().add("pied-assistant");
        zoneApercu.getChildren().addAll(apercu, pied, message);
    }

    private void importer(Label message) {
        Map<String, ImportJoueurs.HoraireNouveau> h = new LinkedHashMap<>();
        try {
            for (Map.Entry<String, ComboBox<LocalDate>> e : jours.entrySet()) {
                LocalTime heure = Saisie.lireHeure(heures.get(e.getKey()).getText(), "Heure de « " + e.getKey() + " »");
                if (e.getValue().getValue() != null) {
                    h.put(e.getKey(), new ImportJoueurs.HoraireNouveau(e.getValue().getValue(), heure));
                }
            }
        } catch (IllegalArgumentException ex) {
            Ui.erreur(message, ex.getMessage());
            return;
        }
        try {
            ImportJoueurs.Bilan b = ImportJoueurs.appliquer(tournoi, plan, h);
            contexte.enregistrerTournoi(tournoi, fichierTournoi);
            Label ok = new Label("✓ Import terminé");
            ok.getStyleClass().add("titre-succes");
            Button voir = Ui.boutonPrimaire("Voir les inscriptions");
            voir.setOnAction(e -> onInscriptions.run());
            Button encore = Ui.boutonSecondaire("Importer un autre fichier");
            encore.setOnAction(e -> {
                format = null;
                analyse = null;
                definitions = null;
                nomFichier = null;
                labelFichier.setText("Aucun fichier choisi.");
                majApercu();
            });
            zoneApercu.getChildren().setAll(Ui.carte(ok,
                    Ui.texte(b.inscrits() + " inscription" + (b.inscrits() > 1 ? "s" : "")
                            + (b.enAttente() > 0 ? ", " + b.enAttente() + " en liste d'attente" : "")
                            + (b.tableauxCrees() > 0 ? ", " + b.tableauxCrees() + " tableau" + (b.tableauxCrees() > 1 ? "x" : "")
                            + " créé" + (b.tableauxCrees() > 1 ? "s" : "") : "")
                            + (b.refus() > 0 ? " ; " + b.refus() + " ligne" + (b.refus() > 1 ? "s" : "") + " non importée"
                            + (b.refus() > 1 ? "s" : "") : "") + ". Le tournoi est enregistré."),
                    Ui.ligne(10, voir, encore)));
        } catch (RuntimeException ex) {
            Ui.erreur(message, "Import impossible : " + ex.getMessage());
        }
    }

    private static Node listeProblemes(String titre, List<ImportJoueurs.Probleme> l, String classe) {
        VBox v = new VBox(3, Ui.gras(titre));
        int max = 40;
        for (int i = 0; i < Math.min(max, l.size()); i++) {
            Label x = new Label(l.get(i).toString());
            x.setWrapText(true);
            x.getStyleClass().add(classe);
            v.getChildren().add(x);
        }
        if (l.size() > max) {
            v.getChildren().add(Ui.discret("… et " + (l.size() - max) + " autres."));
        }
        v.getStyleClass().add("bloc-regle");
        return v;
    }

    private static Node bandeau(String texte) {
        Label l = new Label(texte);
        l.setWrapText(true);
        l.setMaxWidth(Double.MAX_VALUE);
        l.getStyleClass().add("bandeau-refus");
        return l;
    }

    private void enregistrerModele(boolean excel) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Enregistrer le modèle");
        fc.setInitialFileName(excel ? "modele-joueurs-pingmanager.xlsx" : "modele-joueurs-pingmanager.csv");
        File f = fc.showSaveDialog(fenetre());
        if (f == null) {
            return;
        }
        try {
            if (excel) {
                ModeleImport.ecrireXlsx(f.toPath());
            } else {
                ModeleImport.ecrireCsv(f.toPath());
            }
        } catch (Exception ex) {
            erreurLecture = "Modèle non enregistré : " + ex.getMessage();
            majApercu();
        }
    }

    private javafx.stage.Window fenetre() {
        return racine.getScene() == null ? null : racine.getScene().getWindow();
    }
}
