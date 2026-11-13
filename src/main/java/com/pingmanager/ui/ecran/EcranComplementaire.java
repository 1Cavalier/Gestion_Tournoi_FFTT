package com.pingmanager.ui.ecran;

import com.pingmanager.core.ServiceCompletudeTournoi.Niveau;
import com.pingmanager.model.DocumentReglement;
import com.pingmanager.core.reglement.GenerateurReglement;
import com.pingmanager.core.reglement.GenerateurFeuilleEngagement;
import com.pingmanager.core.reglement.RenduPdfReglement;
import com.pingmanager.core.reglement.ServiceReglementTournoi;
import com.pingmanager.model.ArticleOptionnel;
import com.pingmanager.model.CategorieAge;
import com.pingmanager.model.Paiement;
import com.pingmanager.model.DocumentReference;
import com.pingmanager.persistence.DocumentsDeposes;
import com.pingmanager.persistence.ResultatDepotDocument;
import com.pingmanager.model.NiveauTournoi;
import com.pingmanager.model.OptionsReglement;
import com.pingmanager.model.Tournoi;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Section « Complémentaire » :
 * <ul>
 *   <li><b>Règlement du tournoi</b> : soit déposé par le club (PDF), soit
 *       <b>généré</b> à partir des informations des autres sections, avec
 *       une bibliothèque d'articles à ajouter (droit à l'image, buvette…),
 *       un aperçu et un téléchargement en PDF ;</li>
 *   <li><b>Image promotionnelle</b> (facultative), avec vignette ;</li>
 *   <li><b>Conditions de participation</b>, expliquées en une phrase ;</li>
 *   <li><b>Site web</b> du tournoi ;</li>
 *   <li><b>Paiement</b> en ligne (HelloAsso pour l'instant : un simple lien).</li>
 * </ul>
 * La feuille d'engagement (ancien bulletin d'inscription papier) n'est plus
 * proposée : les inscriptions passent par le site ou se font sur place.
 */
final class EcranComplementaire {

    private final Tournoi tournoi;
    private final Path dossier;
    private final Runnable enregistrer;
    private final Runnable retour;
    private final BorderPane racine = new BorderPane();
    private final GenerateurReglement generateur = new GenerateurReglement();
    private final ServiceReglementTournoi reglements = new ServiceReglementTournoi();
    private final DocumentsDeposes documents;

    // Règlement
    private final ToggleGroup mode = new ToggleGroup();
    private final RadioButton modeGenere = new RadioButton("Générer le règlement à partir des informations saisies");
    private final RadioButton modeDepose = new RadioButton("Déposer mon propre règlement (fichier PDF)");
    /** Règlement déposé choisi (copie dans le dossier de données), ou {@code null}. */
    private DocumentReference reglementChoisi;
    private final Label libelleReglement = new Label();
    private final List<EditeurArticle> articles = new ArrayList<>();
    private final VBox listeArticles = new VBox(10);
    private final Label aucunArticle = Ui.note("Aucun article ajouté : le règlement ne contiendra que les articles "
            + "issus de vos informations.");
    private final MenuButton menuArticles = new MenuButton("Ajouter un article");
    private final Label etatReglement = new Label();

    // Image, conditions, site
    private String cheminImage;
    private final ImageView vignette = new ImageView();
    private final Label libelleImage = new Label();
    private final CheckBox licencePromo = new CheckBox("Licence promotionnelle acceptée");
    private final CheckBox etrangers = new CheckBox("Joueurs étrangers autorisés");
    private final TextField site = Ui.champ("https://");
    private final Map<CategorieAge, CheckBox> casesExclues = new java.util.EnumMap<>(CategorieAge.class);

    // Paiement
    private final ToggleGroup modePaiement = new ToggleGroup();
    private final TextField lienPaiement = Ui.champ("https://www.helloasso.com/associations/…");
    private final Label alerteLien = new Label();

    /**
     * @param dossier dossier du fichier tournoi (point de départ des sélecteurs de fichiers)
     */
    EcranComplementaire(Tournoi tournoi, DocumentsDeposes documents, Path dossier, Runnable enregistrer,
                        Runnable retour) {
        this.tournoi = tournoi;
        this.documents = documents;
        this.dossier = dossier;
        this.enregistrer = enregistrer;
        this.retour = retour;
    }

    Node build() {
        afficher();
        return racine;
    }

    private void afficher() {
        Button btnRetour = Ui.boutonLien("← Vue d'ensemble");
        btnRetour.setOnAction(e -> retour.run());
        VBox enTete = new VBox(6, btnRetour, Ui.titre("Complémentaire"));

        Label message = Ui.message();
        Button valider = Ui.boutonPrimaire("Enregistrer");
        valider.setOnAction(e -> {
            Ui.cacher(message);
            try {
                appliquer();
                enregistrer.run();
                Ui.succes(message, "Section Complémentaire enregistrée.");
                majEtatReglement();
            } catch (IllegalArgumentException ex) {
                Ui.erreur(message, ex.getMessage());
            }
        });

        VBox page = Ui.page(900, enTete,
                Ui.legende(EnumSet.of(Niveau.OBLIGATOIRE_PUBLICATION, Niveau.RECOMMANDE, Niveau.OPTIONNEL)),
                reglementOuLien(message), carteImage(), carteConditions(), carteSite(), cartePaiement(),
                message, Ui.actions(valider));
        racine.setCenter(Ui.defilant(page));
        majEtatReglement();
    }

    // ==================================================================
    // Règlement
    // ==================================================================

    /**
     * Avant publication : la carte d'édition du règlement. Après : le règlement
     * est figé, il ne reste qu'un lien vers la version publiée (lecture seule).
     */
    private Node reglementOuLien(Label message) {
        if (tournoi.reglementModifiable()) {
            return carteReglement(message);
        }
        return VueReglementFige.lien(tournoi, documents, dossier, enregistrer, () -> racine.setCenter(
                VueReglementFige.page(tournoi, documents, dossier, enregistrer, this::afficher, "← Complémentaire")));
    }

    private Node carteReglement(Label message) {
        OptionsReglement o = tournoi.getOptionsReglement();
        modeGenere.setToggleGroup(mode);
        modeDepose.setToggleGroup(mode);
        if (o.getMode() == OptionsReglement.Mode.DEPOSE) {
            modeDepose.setSelected(true);
        } else if (o.getMode() == OptionsReglement.Mode.GENERE || tournoi.getReglementDocument() == null) {
            modeGenere.setSelected(true); // proposé par défaut
        } else {
            modeDepose.setSelected(true);
        }
        reglementChoisi = tournoi.getReglementDocument();

        // --- Déposé ---
        Button choisir = Ui.boutonSecondaire("Choisir un fichier PDF…");
        choisir.setOnAction(e -> choisirFichier("Règlement du tournoi", "*.pdf")
                .ifPresent(f -> {
                    // Copie immédiate dans le dossier de données : aucun chemin absolu n'est conservé.
                    ResultatDepotDocument r = documents.deposer(f.toPath(), "Règlement");
                    if (r.estDepose()) {
                        Ui.cacher(message);
                        reglementChoisi = r.getReference();
                    } else {
                        Ui.erreur(message, r.getMessage());
                    }
                    majLibelleReglement();
                }));
        Button retirer = Ui.boutonLien("Retirer");
        retirer.getStyleClass().add("lien-danger");
        retirer.setOnAction(e -> {
            reglementChoisi = null;
            majLibelleReglement();
        });
        libelleReglement.setWrapText(true);
        majLibelleReglement();
        VBox blocDepose = new VBox(8, libelleReglement, Ui.ligne(12, choisir, retirer));
        blocDepose.getStyleClass().add("liste-depliee");

        // --- Généré : bibliothèque d'articles ---
        articles.clear();
        listeArticles.getChildren().clear();
        for (ArticleOptionnel a : o.getArticles()) {
            ajouterArticle(a);
        }
        majMenuArticles();

        Button apercu = Ui.boutonPrimaire("Aperçu du règlement");
        apercu.setOnAction(e -> {
            Ui.cacher(message);
            try {
                appliquer();
                enregistrer.run();
                afficherApercu();
            } catch (IllegalArgumentException ex) {
                Ui.erreur(message, ex.getMessage());
            }
        });
        Button telecharger = Ui.boutonSecondaire("Télécharger le règlement (PDF)…");
        telecharger.setOnAction(e -> {
            Ui.cacher(message);
            try {
                appliquer();
                enregistrer.run();
                telecharger().ifPresent(f -> Ui.succes(message, "Règlement enregistré : " + f + "."));
            } catch (IllegalArgumentException | IOException ex) {
                Ui.erreur(message, ex.getMessage());
            }
        });
        Button feuilleEngagement = Ui.boutonSecondaire("Feuille d'engagement (PDF)…");
        feuilleEngagement.setOnAction(e -> {
            Ui.cacher(message);
            try {
                appliquer();
                enregistrer.run();
                telecharger("Télécharger la feuille d'engagement", "Feuille d'engagement - ", this::ecrireFeuilleEngagement)
                        .ifPresent(f -> Ui.succes(message, "Feuille d'engagement enregistrée : " + f + "."));
            } catch (IllegalArgumentException | IOException ex) {
                Ui.erreur(message, ex.getMessage());
            }
        });

        etatReglement.setWrapText(true);
        menuArticles.getStyleClass().add("menu-ajout");
        VBox blocGenere = new VBox(12,
                etatReglement,
                Ui.note("Le texte reprend automatiquement les sections Général, Tableaux et Informations sportives "
                        + "(points, quotas, maintien des tableaux, forfaits, paiement…) et s'adapte à vos choix. "
                        + "Ajoutez ici les articles propres à votre tournoi : ils sont placés avant l'article "
                        + "« Acceptation »."),
                Ui.enteteAvecActions(Ui.gras("Articles ajoutés au règlement"), menuArticles),
                listeArticles,
                Ui.note("La feuille d'engagement, remplie avec les tableaux, horaires et tarifs du tournoi, sert aux "
                        + "joueurs qui ne peuvent pas s'inscrire en ligne."),
                Ui.actions(apercu, telecharger, feuilleEngagement));
        blocGenere.getStyleClass().add("liste-depliee");

        Runnable majMode = () -> {
            Ui.afficherSi(blocGenere, modeGenere.isSelected());
            Ui.afficherSi(blocDepose, modeDepose.isSelected());
        };
        mode.selectedToggleProperty().addListener((obs, a, n) -> majMode.run());
        majMode.run();

        Node entete = Ui.enteteAvecActions(
                Ui.ligne(8, Ui.puceDependance(Niveau.OBLIGATOIRE_PUBLICATION), Ui.titreCarte("Règlement du tournoi")),
                Ui.badgeEtat(Niveau.OBLIGATOIRE_PUBLICATION, tournoi.aUnReglement()));
        return Ui.carte(entete, modeGenere, blocGenere, modeDepose, blocDepose);
    }

    // --- Articles ------------------------------------------------------

    /** Un article ajouté : son type et l'éditeur de ses paramètres. */
    private final class EditeurArticle {
        final ArticleOptionnel.Type type;
        final Map<String, TextField> champs = new LinkedHashMap<>();
        final Map<String, CheckBox> cases = new LinkedHashMap<>();
        final Map<String, TextArea> zones = new LinkedHashMap<>();
        ComboBox<CategorieAge> categorie;
        final VBox noeud = new VBox();

        EditeurArticle(ArticleOptionnel a) {
            this.type = a.type();
            noeud.getStyleClass().add("bloc-regle");
            Button retirer = Ui.boutonLien("Retirer");
            retirer.getStyleClass().add("lien-danger");
            retirer.setOnAction(e -> {
                articles.remove(this);
                listeArticles.getChildren().remove(noeud);
                majMenuArticles();
                majVide();
            });
            noeud.getChildren().add(Ui.enteteAvecActions(Ui.gras(type.getLibelle()), retirer));
            construire(a);
        }

        private void construire(ArticleOptionnel a) {
            String t = type.name();
            if ("DROIT_IMAGE".equals(t)) {
                noeud.getChildren().add(Ui.note("Autorisation de photographier ou filmer les participants et de diffuser "
                        + "les images (site, réseaux, presse), sauf opposition écrite au pointage ; usage des données "
                        + "d'inscription limité au tournoi. Texte type, sans paramètre."));
            } else if ("BUVETTE".equals(t)) {
                noeud.getChildren().addAll(Ui.champEtiquete("Horaires de la buvette",
                        champ(ArticleOptionnel.HORAIRES, a, "ex. dès 7 h 30 et jusqu'à 23 h, sans interruption le midi")),
                        Ui.note("Complète la phrase : « Une buvette est ouverte dans la salle pendant toute la durée "
                                + "du tournoi, [horaires]. » Une phrase sur les boissons et la petite restauration suit."));
            } else if ("OUVERTURE_SALLE".equals(t)) {
                noeud.getChildren().add(Ui.champEtiquete("Heure d'ouverture de la salle",
                        champ(ArticleOptionnel.HEURE, a, "ex. 7 h 30")));
            } else if ("TIRAGE_AU_SORT".equals(t)) {
                TextField m = champ(ArticleOptionnel.MINUTES, a, "ex. 10");
                noeud.getChildren().add(Ui.champEtiquete("Minutes après la fin du pointage (vide = dès la fin)", m));
            } else if ("SUR_CLASSEMENT".equals(t)) {
                categorie = Ui.combo(new CategorieAge[] {CategorieAge.POUSSIN, CategorieAge.BENJAMIN,
                        CategorieAge.MINIME, CategorieAge.CADET, CategorieAge.JUNIOR}, categorieDe(a));
                noeud.getChildren().addAll(
                        Ui.champEtiquete("Catégorie minimale admise dans les tableaux toutes catégories", categorie),
                        Ui.note("Les jeunes de cette catégorie et au-dessus peuvent jouer les tableaux sans limite d'âge."));
            } else if ("FORFAIT_NON_EXCUSE".equals(t)) {
                noeud.getChildren().add(Ui.note(tournoi.getNiveau() == NiveauTournoi.PROMOTIONNEL
                        ? "Sans objet en tournoi promotionnel (les résultats ne comptent pas) : l'article ne sera pas imprimé."
                        : "Rappelle qu'un forfait non excusé fait perdre les points de la première partie non jouée "
                        + "(IV.202.2). Texte type, sans paramètre."));
            } else if ("INFOS_PRATIQUES".equals(t)) {
                HBox c = Ui.ligne(16, caseACocher(ArticleOptionnel.PARKING, a, "Parking"),
                        caseACocher(ArticleOptionnel.VESTIAIRES, a, "Vestiaires et douches"),
                        caseACocher(ArticleOptionnel.PMR, a, "Accès PMR"));
                noeud.getChildren().addAll(c, Ui.champEtiquete("Précisions (facultatif)",
                        zone(ArticleOptionnel.TEXTE, a, "ex. Hôtels partenaires, accès en transports…", 2)));
            } else {
                noeud.getChildren().addAll(
                        Ui.champEtiquete("Titre de l'article", champ(ArticleOptionnel.TITRE, a, "ex. Tenue des bénévoles")),
                        Ui.champEtiquete("Texte", zone(ArticleOptionnel.TEXTE, a,
                                "Une ligne vide sépare deux paragraphes.", 3)));
            }
        }

        private TextField champ(String cle, ArticleOptionnel a, String invite) {
            TextField f = Ui.champ(invite);
            f.setText(valeur(a.param(cle)));
            champs.put(cle, f);
            return f;
        }

        private CheckBox caseACocher(String cle, ArticleOptionnel a, String libelle) {
            CheckBox c = new CheckBox(libelle);
            c.setSelected(a.coche(cle));
            cases.put(cle, c);
            return c;
        }

        private TextArea zone(String cle, ArticleOptionnel a, String invite, int lignes) {
            TextArea z = new TextArea(valeur(a.param(cle)));
            z.setPromptText(invite);
            z.setPrefRowCount(lignes);
            z.setWrapText(true);
            zones.put(cle, z);
            return z;
        }

        ArticleOptionnel lire() {
            Map<String, String> p = new LinkedHashMap<>();
            champs.forEach((k, f) -> p.put(k, f.getText()));
            cases.forEach((k, c) -> p.put(k, c.isSelected() ? "oui" : null));
            zones.forEach((k, z) -> p.put(k, z.getText()));
            if (categorie != null && categorie.getValue() != null) {
                p.put(ArticleOptionnel.CATEGORIE, categorie.getValue().getLibelle());
            }
            String minutes = p.get(ArticleOptionnel.MINUTES);
            if (minutes != null && !minutes.isBlank()) {
                lireEntier(champs.get(ArticleOptionnel.MINUTES), "Tirage au sort : minutes après le pointage");
            }
            if ("LIBRE".equals(type.name())) {
                String titre = p.get(ArticleOptionnel.TITRE);
                String texte = p.get(ArticleOptionnel.TEXTE);
                if ((titre == null || titre.isBlank()) != (texte == null || texte.isBlank())) {
                    Ui.marquerErreur(titre == null || titre.isBlank() ? champs.get(ArticleOptionnel.TITRE)
                            : zones.get(ArticleOptionnel.TEXTE));
                    throw new IllegalArgumentException("Article libre : renseignez le titre et le texte.");
                }
            }
            return new ArticleOptionnel(type, p);
        }
    }

    private static CategorieAge categorieDe(ArticleOptionnel a) {
        String c = a.param(ArticleOptionnel.CATEGORIE);
        for (CategorieAge x : CategorieAge.values()) {
            if (x.getLibelle().equalsIgnoreCase(c == null ? "" : c)) {
                return x;
            }
        }
        return null;
    }

    private void ajouterArticle(ArticleOptionnel a) {
        EditeurArticle ed = new EditeurArticle(a);
        articles.add(ed);
        listeArticles.getChildren().remove(aucunArticle);
        listeArticles.getChildren().add(ed.noeud);
    }

    private void majVide() {
        if (articles.isEmpty() && !listeArticles.getChildren().contains(aucunArticle)) {
            listeArticles.getChildren().add(aucunArticle);
        }
    }

    /** Menu « Ajouter un article » : un modèle déjà présent (sauf l'article libre) est grisé. */
    private void majMenuArticles() {
        menuArticles.getItems().clear();
        for (ArticleOptionnel.Type ty : ArticleOptionnel.Type.values()) {
            MenuItem mi = new MenuItem(ty.getLibelle());
            mi.setDisable(ty.unique() && articles.stream().anyMatch(x -> x.type == ty));
            mi.setOnAction(e -> {
                ajouterArticle(ArticleOptionnel.de(ty));
                majMenuArticles();
            });
            menuArticles.getItems().add(mi);
        }
        majVide();
    }

    private void majLibelleReglement() {
        if (reglementChoisi == null) {
            libelleReglement.setText("Aucun fichier choisi.");
        } else if (documents.fichier(reglementChoisi).isEmpty()) {
            libelleReglement.setText("Fichier introuvable : " + DocumentsDeposes.nomAffiche(reglementChoisi)
                    + " — choisissez de nouveau le PDF.");
        } else {
            libelleReglement.setText("Fichier : " + DocumentsDeposes.nomAffiche(reglementChoisi));
        }
    }

    /** « Prêt » ou « N éléments à compléter », calculé avec les options en cours de saisie. */
    private void majEtatReglement() {
        if (!tournoi.reglementModifiable()) {
            return; // règlement figé : pas de carte d'édition
        }
        etatReglement.getStyleClass().removeAll("message-succes", "rappel-attention", "rappel-doux");
        OptionsReglement o;
        try {
            o = lireOptions();
        } catch (IllegalArgumentException ex) {
            o = tournoi.getOptionsReglement();
        }
        DocumentReglement d = generateur.generer(tournoi, o);
        long trous = d.aCompleter().stream().filter(x -> x.startsWith("Compléter")).count();
        if (!d.estProjet()) {
            etatReglement.setText("Le règlement est complet : il peut être joint à la demande d'homologation.");
            etatReglement.getStyleClass().add("message-succes");
        } else {
            etatReglement.setText(trous + (trous > 1 ? " passages restent" : " passage reste")
                    + " à compléter, surlignés en orange dans l'aperçu.");
            etatReglement.getStyleClass().addAll("rappel-attention", "rappel-doux");
        }
    }

    private void afficherApercu() {
        DocumentReglement d = reglements.reglement(tournoi);
        Button btnRetour = Ui.boutonLien("← Complémentaire");
        btnRetour.setOnAction(e -> afficher());
        Label message = Ui.message();
        Button exporter = Ui.boutonPrimaire("Télécharger le règlement (PDF)…");
        exporter.setOnAction(e -> {
            try {
                telecharger().ifPresent(f -> Ui.succes(message, "Règlement enregistré : " + f + "."));
            } catch (IOException ex) {
                Ui.erreur(message, ex.getMessage());
            }
        });
        VBox feuille = ApercuReglement.rendre(d);
        VBox page = Ui.page(860, Ui.enteteAvecActions(btnRetour, exporter), message, Ui.carteNue(feuille));
        racine.setCenter(Ui.defilant(page));
    }

    /** Enregistre le règlement en PDF à l'endroit choisi. @return le fichier écrit, si l'utilisateur a validé. */
    private Optional<File> telecharger() throws IOException {
        return telecharger("Télécharger le règlement", "Reglement - ", this::ecrirePdf);
    }

    /** Écrit un PDF à l'emplacement choisi dans un dialogue d'enregistrement. */
    private interface EcrivainPdf {
        void ecrire(Path cible) throws IOException;
    }

    private Optional<File> telecharger(String titre, String prefixe, EcrivainPdf ecrivain) throws IOException {
        FileChooser fc = new FileChooser();
        fc.setTitle(titre);
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Document PDF", "*.pdf"));
        fc.setInitialFileName(prefixe + nomFichier() + ".pdf");
        if (dossier != null && Files.isDirectory(dossier)) {
            fc.setInitialDirectory(dossier.toFile());
        }
        File f = fc.showSaveDialog(fenetre());
        if (f == null) {
            return Optional.empty();
        }
        Path cible = f.getName().toLowerCase().endsWith(".pdf") ? f.toPath() : f.toPath().resolveSibling(f.getName() + ".pdf");
        ecrivain.ecrire(cible);
        return Optional.of(cible.toFile());
    }

    /** Écrit le règlement généré au format PDF (utilisé aussi hors dialogue). */
    void ecrirePdf(Path cible) throws IOException {
        RenduPdfReglement.ecrire(reglements.reglement(tournoi), cible);
    }

    /** Écrit la feuille d'engagement remplie au format PDF (utilisé aussi hors dialogue). */
    void ecrireFeuilleEngagement(Path cible) throws IOException {
        RenduPdfReglement.ecrire(reglements.feuilleEngagement(tournoi), cible,
                GenerateurFeuilleEngagement.FORMAT_TITRE);
    }

    private String nomFichier() {
        String nom = tournoi.getNom() == null ? "tournoi" : tournoi.getNom();
        return nom.replaceAll("[\\\\/:*?\"<>|]", "-");
    }

    // ==================================================================
    // Image promotionnelle
    // ==================================================================

    private Node carteImage() {
        cheminImage = tournoi.getImagePromotionnelle() == null ? null : tournoi.getImagePromotionnelle().getEmplacement();
        vignette.setFitWidth(260);
        vignette.setFitHeight(160);
        vignette.setPreserveRatio(true);
        vignette.getStyleClass().add("vignette");
        Button choisir = Ui.boutonSecondaire("Choisir une image…");
        choisir.setOnAction(e -> choisirFichier("Image promotionnelle", "*.png", "*.jpg", "*.jpeg")
                .ifPresent(f -> {
                    cheminImage = f.getAbsolutePath();
                    majImage();
                }));
        Button retirer = Ui.boutonLien("Retirer");
        retirer.getStyleClass().add("lien-danger");
        retirer.setOnAction(e -> {
            cheminImage = null;
            majImage();
        });
        libelleImage.setWrapText(true);
        majImage();
        Node entete = Ui.ligne(8, Ui.puceDependance(Niveau.OPTIONNEL), Ui.titreCarte("Image promotionnelle"));
        return Ui.carte(entete,
                Ui.note("Facultatif. Affiche ou visuel du tournoi (PNG ou JPEG), utilisé pour annoncer le tournoi."),
                Ui.ligne(16, vignette, new VBox(8, libelleImage, Ui.ligne(12, choisir, retirer))));
    }

    private void majImage() {
        if (cheminImage == null) {
            vignette.setImage(null);
            Ui.afficherSi(vignette, false);
            libelleImage.setText("Aucune image choisie.");
            return;
        }
        File f = new File(cheminImage);
        libelleImage.setText(f.getName());
        if (f.isFile()) {
            vignette.setImage(new Image(f.toURI().toString(), 260, 160, true, true, true));
            Ui.afficherSi(vignette, true);
        } else {
            Ui.afficherSi(vignette, false);
            libelleImage.setText(f.getName() + " (fichier introuvable)");
        }
    }

    // ==================================================================
    // Conditions de participation, site web
    // ==================================================================

    private Node carteConditions() {
        NiveauTournoi n = tournoi.getNiveau();
        boolean international = n == NiveauTournoi.INTERNATIONAL;
        licencePromo.setSelected(tournoi.accepteLicencePromotionnelle());
        etrangers.setSelected(international && tournoi.accepteJoueursEtrangers());
        etrangers.setDisable(!international);

        VBox promo = new VBox(4, licencePromo, Ui.note("Cochez si les titulaires d'une licence promotionnelle "
                + "(et d'un pass'tournoi) peuvent s'inscrire : en tournoi promotionnel, ou dans des tableaux qui "
                + "leur sont réservés (IX.101)."));
        String noteEtrangers = international
                ? "Licenciés d'une fédération étrangère affiliée à l'ITTF : admis en tournoi international (IX.102)."
                : "Bloqué : les licenciés de fédérations étrangères ne sont admis qu'en tournoi international.";
        VBox etr = new VBox(4, etrangers, Ui.note(noteEtrangers));

        // Catégories d'âge non admises (choix du club, ex. poussins).
        casesExclues.clear();
        HBox cases = Ui.ligne(16);
        for (CategorieAge c : new CategorieAge[] {CategorieAge.POUSSIN, CategorieAge.BENJAMIN, CategorieAge.MINIME}) {
            CheckBox cb = new CheckBox(c.getLibelle());
            cb.setSelected(tournoi.getCategoriesExclues().contains(c));
            casesExclues.put(c, cb);
            cases.getChildren().add(cb);
        }
        VBox exclues = new VBox(6, Ui.gras("Catégories non admises"), cases,
                Ui.note("Choix du club, écrit dans l'article « Participants ». Le règlement fédéral n'interdit aucune "
                        + "catégorie en tournoi ; exclure les poussins (moins de 9 ans au 1er janvier, II.303) reste "
                        + "une pratique courante."));
        Node entete = Ui.ligne(8, Ui.puceDependance(Niveau.RECOMMANDE), Ui.titreCarte("Conditions de participation"));
        return Ui.carte(entete, promo, etr, exclues);
    }

    private Node carteSite() {
        site.setText(valeur(tournoi.getSiteWeb()));
        Node entete = Ui.ligne(8, Ui.puceDependance(Niveau.OPTIONNEL), Ui.titreCarte("Communication"));
        return Ui.carte(entete, Ui.champEtiquete("Site web du tournoi (inscriptions, informations)", site, Niveau.OPTIONNEL));
    }

    // ==================================================================
    // Paiement
    // ==================================================================

    private Node cartePaiement() {
        Paiement p = tournoi.getPaiement();
        RadioButton hello = new RadioButton("HelloAsso");
        hello.setUserData(Paiement.Mode.HELLOASSO);
        RadioButton plateforme = new RadioButton("Plateforme PingManager — bientôt disponible");
        plateforme.setUserData(Paiement.Mode.PLATEFORME);
        RadioButton autre = new RadioButton("Autre lien de paiement — bientôt disponible");
        autre.setUserData(Paiement.Mode.AUTRE);
        for (RadioButton r : List.of(hello, plateforme, autre)) {
            r.setToggleGroup(modePaiement);
            r.setDisable(!((Paiement.Mode) r.getUserData()).estDisponible());
        }
        hello.setSelected(true); // seul mode disponible pour l'instant
        lienPaiement.setText(valeur(p.getLien()));

        VBox etapes = new VBox(4,
                new Label("1. Connectez-vous à l'espace de votre association sur helloasso.com (ou créez-le : "
                        + "SIRET ou RNA du club, compte bancaire pour les versements)."),
                new Label("2. Créez une billetterie « " + (tournoi.getNom() == null ? "Tournoi" : tournoi.getNom())
                        + " » avec un tarif par tableau, au prix de l'inscription en ligne."),
                new Label("3. Ajoutez au formulaire les champs utiles : n° de licence, nom, prénom, club."),
                new Label("4. Publiez la billetterie, copiez son lien et collez-le ci-dessous."));
        etapes.getStyleClass().addAll("encart-regles", "etapes");

        alerteLien.setWrapText(true);
        alerteLien.getStyleClass().addAll("rappel-attention", "rappel-doux");
        Runnable maj = () -> {
            String l = lienPaiement.getText() == null ? "" : lienPaiement.getText().trim();
            boolean alerte = !l.isEmpty() && !l.toLowerCase().contains("helloasso.");
            alerteLien.setText("Ce lien ne semble pas être une page HelloAsso. Vérifiez que vous avez collé le lien "
                    + "de la billetterie.");
            Ui.afficherSi(alerteLien, alerte);
        };
        lienPaiement.textProperty().addListener((o, a, n) -> maj.run());
        maj.run();

        Node entete = Ui.ligne(8, Ui.puceDependance(Niveau.RECOMMANDE), Ui.titreCarte("Paiement"));
        return Ui.carte(entete,
                Ui.note("Les joueurs règlent leur inscription en ligne. Le lien apparaît sur la page du tournoi et "
                        + "dans le règlement ; une inscription n'est validée qu'à réception du paiement."),
                hello, plateforme, autre,
                etapes,
                Ui.champEtiquete("Lien de la billetterie HelloAsso", lienPaiement, Niveau.RECOMMANDE),
                alerteLien);
    }

    // ==================================================================
    // Lecture du formulaire
    // ==================================================================

    private OptionsReglement lireOptions() {
        List<ArticleOptionnel> lus = new ArrayList<>();
        for (EditeurArticle ed : articles) {
            lus.add(ed.lire());
        }
        return new OptionsReglement(modeGenere.isSelected() ? OptionsReglement.Mode.GENERE
                : modeDepose.isSelected() ? OptionsReglement.Mode.DEPOSE : null, lus);
    }

    private Paiement lirePaiement() {
        Paiement.Mode m = modePaiement.getSelectedToggle() == null ? null
                : (Paiement.Mode) modePaiement.getSelectedToggle().getUserData();
        try {
            return new Paiement(m, lienPaiement.getText());
        } catch (IllegalArgumentException e) {
            Ui.marquerErreur(lienPaiement);
            throw e;
        }
    }

    /** Applique le formulaire au tournoi (sans enregistrer). */
    private void appliquer() {
        Paiement paiement = lirePaiement();
        // Tournoi publié : règlement figé, le formulaire du règlement n'est pas affiché.
        if (tournoi.reglementModifiable()) {
            OptionsReglement o = lireOptions();
            if (modeDepose.isSelected() && reglementChoisi == null) {
                throw new IllegalArgumentException("Règlement déposé : choisissez le fichier PDF du règlement.");
            }
            reglements.modifierOptions(tournoi, o);
            reglements.deposer(tournoi, reglementChoisi);
        }
        tournoi.setPaiement(paiement);
        tournoi.setImagePromotionnelle(cheminImage == null ? null
                : new DocumentReference("Image promotionnelle", cheminImage));
        tournoi.setAccepteLicencePromotionnelle(licencePromo.isSelected());
        tournoi.setAccepteJoueursEtrangers(tournoi.getNiveau() == NiveauTournoi.INTERNATIONAL && etrangers.isSelected());
        List<CategorieAge> exclues = new ArrayList<>();
        casesExclues.forEach((c, cb) -> {
            if (cb.isSelected()) {
                exclues.add(c);
            }
        });
        tournoi.setCategoriesExclues(exclues);
        tournoi.setSiteWeb(site.getText() == null || site.getText().isBlank() ? null : site.getText().trim());
    }

    private static Integer lireEntier(TextField champ, String nom) {
        try {
            return Saisie.lireEntier(champ.getText(), nom);
        } catch (IllegalArgumentException e) {
            Ui.marquerErreur(champ);
            throw e;
        }
    }

    private Optional<File> choisirFichier(String titre, String... extensions) {
        FileChooser fc = new FileChooser();
        fc.setTitle(titre);
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter(titre, extensions));
        if (dossier != null && Files.isDirectory(dossier)) {
            fc.setInitialDirectory(dossier.toFile());
        }
        return Optional.ofNullable(fc.showOpenDialog(fenetre()));
    }

    private Window fenetre() {
        return racine.getScene() == null ? null : racine.getScene().getWindow();
    }

    private static String valeur(String s) {
        return s == null ? "" : s;
    }
}
