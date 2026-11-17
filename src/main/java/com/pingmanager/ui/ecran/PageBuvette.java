package com.pingmanager.ui.ecran;

import com.pingmanager.core.DroitRefuseException;
import com.pingmanager.core.Horloge;
import com.pingmanager.core.buvette.CommandeComptoir;
import com.pingmanager.core.buvette.ResultatArticleBuvette;
import com.pingmanager.core.buvette.ResultatMouvementStock;
import com.pingmanager.core.buvette.ServiceArticlesBuvette;
import com.pingmanager.core.buvette.ServiceStockBuvette;
import com.pingmanager.core.buvette.ServiceVentesBuvette;
import com.pingmanager.model.AccesClub;
import com.pingmanager.model.buvette.BuvetteTournoi;
import com.pingmanager.model.buvette.FamilleArticle;
import com.pingmanager.model.buvette.ModeBuvette;
import com.pingmanager.model.buvette.MoyenPaiement;
import com.pingmanager.model.buvette.StockBuvette;
import com.pingmanager.persistence.DepotTournoi;
import com.pingmanager.ui.ContexteApplication;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Page « Buvette » du groupe Gestion (B026), réservée aux organisateurs. En
 * tête : le club, le tournoi et son réglage (« Stock seulement » ou « Stock
 * et ventes »). Puis quatre onglets :
 * <ul>
 *   <li><b>Stock</b> : liste « À racheter », jauges, sorties et entrées à la main ;</li>
 *   <li><b>Articles</b> : articles du club, fiche, catalogue de base à cocher
 *       ({@link VueArticlesBuvette}) ;</li>
 *   <li><b>Comptoir</b> (mode « Stock et ventes ») : grille de gros boutons et
 *       commande en cours ({@link VueComptoirBuvette}) ;</li>
 *   <li><b>Bilan</b> : chiffres, détails, historique et export CSV
 *       ({@link VueBilanBuvette}).</li>
 * </ul>
 * La logique d'affichage est dans les classes {@code Presentation…Buvette},
 * les règles dans {@code core.buvette}.
 *
 * <p>Les articles et le stock appartiennent au club ; le réglage et les ventes
 * sont rangés par tournoi. Tout s'enregistre avec les données d'application à
 * chaque action. Suivi interne du club, pas une caisse enregistreuse.</p>
 */
final class PageBuvette {

    static final String ONGLET_STOCK = "stock";
    static final String ONGLET_ARTICLES = "articles";
    static final String ONGLET_COMPTOIR = "comptoir";
    static final String ONGLET_BILAN = "bilan";

    final ContexteApplication contexte;
    final ServiceArticlesBuvette serviceArticles = new ServiceArticlesBuvette();
    final ServiceStockBuvette serviceStock = new ServiceStockBuvette(Horloge.systeme());
    final ServiceVentesBuvette serviceVentes = new ServiceVentesBuvette(Horloge.systeme());
    final ZoneId zone = ZoneId.systemDefault();

    private final VBox racine = new VBox(16);
    private final VBox reglage = new VBox(10);
    private final HBox onglets = new HBox();
    private final VBox contenu = new VBox(20);
    final Label message = Ui.message();
    private String numeroClub;
    private String onglet;

    /** Tournoi choisi ({@code null} : aucun tournoi du club). */
    private ChoixTournoiBuvette.Option tournoi;
    private List<ChoixTournoiBuvette.Option> tournois = List.of();

    // État des vues, gardé tant que la page est ouverte.
    /** Article ouvert dans la fiche ({@code null} : aucun ; {@link #NOUVEL_ARTICLE} : création). */
    String articleEdite;
    static final String NOUVEL_ARTICLE = "";
    boolean vueCatalogue;
    final Set<String> cochesCatalogue = new HashSet<>();
    final CommandeComptoir commande = new CommandeComptoir();
    MoyenPaiement moyen = MoyenPaiement.ESPECES;
    /** Avertissement de stock de la dernière vente (vide : aucun). */
    String avertissementComptoir = "";
    /** Journée du bilan ({@code null} : tout le tournoi). */
    LocalDate periodeBilan;
    /** Vente dont l'annulation est en attente de confirmation (0 : aucune). */
    int annulationAConfirmer;

    PageBuvette(ContexteApplication contexte) {
        this.contexte = contexte;
    }

    Parent build() {
        Map<String, String> clubs = clubsGeres();
        if (clubs.isEmpty()) {
            return PagePlaceholder.page("Buvette", "Rattachez votre compte à un club pour suivre le stock de sa buvette.");
        }
        numeroClub = clubs.keySet().iterator().next();
        HBox entete = Ui.ligne(12, Ui.titre("Buvette"));
        if (clubs.size() > 1) {
            ComboBox<String> choix = new ComboBox<>(FXCollections.observableArrayList(clubs.keySet()));
            choix.setConverter(convertisseur(clubs));
            choix.getSelectionModel().select(numeroClub);
            choix.valueProperty().addListener((o, a, n) -> {
                if (n != null) {
                    numeroClub = n;
                    reinitialiser();
                    chargerTournois();
                    afficher();
                }
            });
            entete.getChildren().addAll(Ui.espace(), Ui.libelleChamp("Club"), choix);
        } else {
            entete.getChildren().addAll(Ui.espace(), Ui.discret(clubs.values().iterator().next()));
        }
        onglets.getStyleClass().add("buvette-onglets");
        racine.getChildren().setAll(entete, reglage, onglets, message, contenu);
        racine.getStyleClass().add("page-buvette");
        chargerTournois();
        afficher();
        return Ui.defilant(Ui.page(1200, racine));
    }

    private void reinitialiser() {
        articleEdite = null;
        vueCatalogue = false;
        cochesCatalogue.clear();
        commande.vider();
        avertissementComptoir = "";
        periodeBilan = null;
        annulationAConfirmer = 0;
        onglet = null;
    }

    private Map<String, String> clubsGeres() {
        Map<String, String> res = new LinkedHashMap<>();
        for (AccesClub a : contexte.accesCourants()) {
            String nom = contexte.clubParNumero(a.getNumeroClub()).map(c -> c.getNom()).orElse(a.getNumeroClub());
            res.putIfAbsent(a.getNumeroClub(), nom);
        }
        return res;
    }

    private static StringConverter<String> convertisseur(Map<String, String> clubs) {
        return new StringConverter<>() {
            @Override
            public String toString(String numero) {
                return numero == null ? "" : clubs.getOrDefault(numero, numero);
            }

            @Override
            public String fromString(String s) {
                return null;
            }
        };
    }

    /** Tournois du club (fichiers lisibles) et tournoi choisi par défaut. */
    private void chargerTournois() {
        List<ChoixTournoiBuvette.Candidat> candidats = new ArrayList<>();
        Set<String> enCours = new HashSet<>();
        try {
            for (ContexteApplication.TournoiFichier tf : contexte.mesTournoisClub(contexte.tousLesTournois())) {
                String club = tf.tournoi().getNumeroAffiliationClub();
                if (club != null && club.trim().equals(numeroClub.trim())) {
                    candidats.add(new ChoixTournoiBuvette.Candidat(reference(tf), tf.tournoi().getNom(),
                            tf.tournoi().getDateDebut()));
                }
            }
            for (ContexteApplication.TournoiFichier tf : contexte.tournoisEnCours()) {
                enCours.add(reference(tf));
            }
        } catch (RuntimeException illisible) {
            // dossier des tournois illisible : la page reste utilisable pour le stock
        }
        List<BuvetteTournoi> connus;
        try {
            connus = stock().getTournois();
        } catch (DroitRefuseException refus) {
            connus = List.of();
        }
        tournois = ChoixTournoiBuvette.options(candidats, connus);
        String defaut = ChoixTournoiBuvette.parDefaut(candidats, enCours, LocalDate.now(zone));
        tournoi = null;
        for (ChoixTournoiBuvette.Option o : tournois) {
            if (o.reference().equals(defaut) || (tournoi == null && defaut == null)) {
                tournoi = o;
            }
        }
        if (tournoi == null && !tournois.isEmpty()) {
            tournoi = tournois.get(0);
        }
    }

    /** Référence d'un tournoi : nom de son fichier, sans dossier ni extension. */
    static String reference(ContexteApplication.TournoiFichier tf) {
        String n = tf.fichier().getFileName().toString();
        return n.endsWith(DepotTournoi.EXTENSION) ? n.substring(0, n.length() - DepotTournoi.EXTENSION.length()) : n;
    }

    StockBuvette stock() {
        return contexte.stockBuvette(numeroClub);
    }

    /** Référence du tournoi choisi ({@code null} si aucun). */
    String referenceTournoi() {
        return tournoi == null ? null : tournoi.reference();
    }

    String nomTournoi() {
        return tournoi == null ? "" : tournoi.nom();
    }

    ModeBuvette mode() {
        return tournoi == null ? ModeBuvette.STOCK_SEUL : ServiceVentesBuvette.mode(stock(), tournoi.reference());
    }

    /** Ventes du tournoi choisi (vide si aucune). */
    List<com.pingmanager.model.buvette.VenteBuvette> ventes() {
        if (tournoi == null) {
            return List.of();
        }
        return stock().tournoi(tournoi.reference()).map(BuvetteTournoi::getVentes).orElse(List.of());
    }

    /** Qui saisit : le nom du compte connecté. */
    String auteur() {
        com.pingmanager.model.CompteUtilisateur c = contexte.compteConnecte();
        if (c == null) {
            return "inconnu";
        }
        String nom = c.getNomComplet();
        return nom == null || nom.isBlank() ? c.getEmail() : nom;
    }

    void ouvrir(String nouvelOnglet) {
        onglet = nouvelOnglet;
        annulationAConfirmer = 0;
        afficher();
    }

    /** Reconstruit la page (après chaque action). */
    void afficher() {
        Node n;
        try {
            construireReglage();
            construireOnglets();
            n = construireOnglet();
        } catch (DroitRefuseException refus) {
            reglage.getChildren().clear();
            onglets.getChildren().clear();
            n = Ui.carteNue(Ui.etatVide(refus.getMessage(), null));
        }
        contenu.getChildren().setAll(n);
    }

    /** Enregistre les données du club, affiche le message, puis reconstruit la page. */
    void enregistrer(boolean accepte, String explication) {
        if (!accepte) {
            Ui.erreur(message, explication);
            return;
        }
        try {
            contexte.sauvegarder();
        } catch (RuntimeException echec) {
            Ui.erreur(message, "Enregistrement impossible : " + echec.getMessage());
            return;
        }
        Ui.succes(message, explication);
        afficher();
    }

    void annoncer(ResultatMouvementStock r) {
        enregistrer(r.accepte(), r.explication());
    }

    void annoncer(ResultatArticleBuvette r) {
        if (r.accepte()) {
            articleEdite = null;
        }
        enregistrer(r.accepte(), r.explication());
    }

    // ==================================================================
    // En-tête : tournoi et réglage
    // ==================================================================

    private void construireReglage() {
        reglage.getChildren().clear();
        reglage.getStyleClass().setAll("carte", "buvette-reglage");
        if (tournoi == null) {
            reglage.getChildren().add(Ui.discret("Aucun tournoi de ce club pour l'instant : la buvette suit le stock. "
                    + "Pour enregistrer des ventes, créez d'abord le tournoi."));
            return;
        }
        ComboBox<ChoixTournoiBuvette.Option> choix = new ComboBox<>(FXCollections.observableArrayList(tournois));
        choix.setConverter(new StringConverter<>() {
            @Override
            public String toString(ChoixTournoiBuvette.Option o) {
                return o == null ? "" : o.libelle();
            }

            @Override
            public ChoixTournoiBuvette.Option fromString(String s) {
                return null;
            }
        });
        choix.getSelectionModel().select(tournoi);
        choix.valueProperty().addListener((o, a, n) -> {
            if (n != null && n != tournoi) {
                tournoi = n;
                commande.vider();
                avertissementComptoir = "";
                periodeBilan = null;
                onglet = null;
                message.setText("");
                afficher();
            }
        });
        choix.getStyleClass().add("buvette-choix-tournoi");

        ToggleGroup groupe = new ToggleGroup();
        ModeBuvette actuel = mode();
        List<ToggleButton> boutons = new ArrayList<>();
        for (ModeBuvette m : ModeBuvette.values()) {
            ToggleButton b = new ToggleButton(m.getLibelle());
            b.setSelected(m == actuel);
            b.setOnAction(e -> {
                if (m == mode()) {
                    b.setSelected(true);
                    return;
                }
                serviceVentes.regler(stock(), tournoi.reference(), tournoi.nom(), m);
                onglet = null;
                enregistrer(true, "Buvette de « " + tournoi.nom() + " » réglée sur « " + m.getLibelle() + " ».");
            });
            boutons.add(b);
        }
        HBox ligne = new HBox(16, Ui.champEtiquete("Tournoi", choix),
                Ui.champEtiquete("Tenir les comptes", Ui.segment(groupe, boutons.toArray(new ToggleButton[0]))));
        ligne.setAlignment(Pos.BOTTOM_LEFT);
        FlowPane souple = new FlowPane(16, 10, ligne.getChildren().toArray(new Node[0]));
        souple.setAlignment(Pos.BOTTOM_LEFT);
        reglage.getChildren().addAll(souple, Ui.aide(actuel.getExplication()
                + (actuel == ModeBuvette.STOCK_ET_VENTES ? " Suivi interne du club, pas une caisse enregistreuse." : "")));
    }

    private boolean ongletVisible(String o) {
        if (ONGLET_COMPTOIR.equals(o)) {
            return mode() == ModeBuvette.STOCK_ET_VENTES;
        }
        if (ONGLET_BILAN.equals(o)) {
            return mode() == ModeBuvette.STOCK_ET_VENTES || !ventes().isEmpty();
        }
        return true;
    }

    private void construireOnglets() {
        if (onglet == null || !ongletVisible(onglet)) {
            onglet = mode() == ModeBuvette.STOCK_ET_VENTES ? ONGLET_COMPTOIR : ONGLET_STOCK;
        }
        ToggleGroup groupe = new ToggleGroup();
        List<ToggleButton> boutons = new ArrayList<>();
        String[][] tous = {{ONGLET_COMPTOIR, "Comptoir"}, {ONGLET_STOCK, "Stock"}, {ONGLET_ARTICLES, "Articles"},
                {ONGLET_BILAN, "Bilan"}};
        for (String[] o : tous) {
            if (!ongletVisible(o[0])) {
                continue;
            }
            ToggleButton b = new ToggleButton(o[1]);
            b.setSelected(o[0].equals(onglet));
            b.setOnAction(e -> {
                if (o[0].equals(onglet)) {
                    b.setSelected(true);
                    return;
                }
                message.setText("");
                ouvrir(o[0]);
            });
            boutons.add(b);
        }
        onglets.getChildren().setAll(Ui.segment(groupe, boutons.toArray(new ToggleButton[0])));
    }

    private Node construireOnglet() {
        if (ONGLET_ARTICLES.equals(onglet)) {
            return new VueArticlesBuvette(this).construire();
        }
        if (ONGLET_COMPTOIR.equals(onglet)) {
            return new VueComptoirBuvette(this).construire();
        }
        if (ONGLET_BILAN.equals(onglet)) {
            return new VueBilanBuvette(this).construire();
        }
        return construireStock();
    }

    // ==================================================================
    // Onglet « Stock »
    // ==================================================================

    private Node construireStock() {
        StockBuvette stock = stock();
        PresentationStockBuvette p = new PresentationStockBuvette(stock);
        VBox v = new VBox(20);
        v.getChildren().add(Ui.aide("Suivi du stock : sortez ce qui part, entrez ce qui arrive, recomptez quand il "
                + "le faut. Sortir une portion d'un produit sort aussi ses contenants."));
        v.getChildren().add(carteARacheter(p));
        if (!p.aDesArticlesActifs()) {
            Button ajouter = Ui.boutonPrimaire("Choisir dans le catalogue");
            ajouter.setOnAction(e -> {
                vueCatalogue = true;
                ouvrir(ONGLET_ARTICLES);
            });
            v.getChildren().add(Ui.carteNue(Ui.etatVide(PresentationStockBuvette.MESSAGE_VIDE, ajouter)));
        } else {
            for (PresentationStockBuvette.Section s : p.sections()) {
                v.getChildren().add(carteSection(stock, s));
            }
        }
        List<String> mouvements = p.derniersMouvements(8, zone);
        if (!mouvements.isEmpty()) {
            VBox lignes = new VBox(4);
            mouvements.forEach(m -> lignes.getChildren().add(Ui.discret(m)));
            v.getChildren().add(Ui.carte("Derniers mouvements", lignes));
        }
        return v;
    }

    private Node carteARacheter(PresentationStockBuvette p) {
        List<PresentationStockBuvette.LigneARacheter> lignes = p.aRacheter();
        VBox corps = new VBox(4);
        if (lignes.isEmpty()) {
            corps.getChildren().add(Ui.discret(p.vide() ? "Rien à suivre pour l'instant."
                    : PresentationStockBuvette.RIEN_A_RACHETER));
        }
        boolean premiere = true;
        for (PresentationStockBuvette.LigneARacheter l : lignes) {
            HBox ligne = Ui.ligneElement(null, l.nom(), l.detail(), Ui.pastille("À racheter", StatutPastille.ERREUR));
            if (premiere) {
                ligne.getStyleClass().add("ligne-element-premiere");
                premiere = false;
            }
            corps.getChildren().add(ligne);
        }
        VBox carte = Ui.carte(Ui.titreAvecCompteur("À racheter", lignes.size()), corps);
        carte.getStyleClass().add(lignes.isEmpty() ? "buvette-a-racheter-vide" : "buvette-a-racheter");
        return carte;
    }

    private Node carteSection(StockBuvette stock, PresentationStockBuvette.Section s) {
        VBox liste = new VBox(4);
        if (s.lignes().isEmpty()) {
            liste.getChildren().add(Ui.discret(s.famille() == FamilleArticle.PRODUIT
                    ? "Aucun produit." : "Aucun contenant ni consommable."));
        }
        boolean premiere = true;
        for (PresentationStockBuvette.LigneArticle l : s.lignes()) {
            HBox ligne = ligneArticle(stock, l);
            if (premiere) {
                ligne.getStyleClass().add("ligne-element-premiere");
                premiere = false;
            }
            liste.getChildren().add(ligne);
        }
        return Ui.carte(Ui.titreAvecCompteur(s.titre(), s.lignes().size()), liste);
    }

    private HBox ligneArticle(StockBuvette stock, PresentationStockBuvette.LigneArticle l) {
        ProgressBar barre = new ProgressBar(l.remplissage());
        barre.getStyleClass().addAll("jauge", l.classeJauge());
        barre.setPrefHeight(10);
        barre.setPrefWidth(150);
        Label texteStock = Ui.gras(l.stock());
        HBox enteteJauge = new HBox(8, texteStock, Ui.pastille(l.niveau().getLibelle(),
                PresentationStockBuvette.statut(l.niveau())));
        if (l.aRecompter()) {
            enteteJauge.getChildren().add(Ui.pastille("À recompter", StatutPastille.ATTENTION));
        }
        enteteJauge.setAlignment(Pos.CENTER_LEFT);
        VBox jauge = new VBox(4, enteteJauge, barre);
        jauge.getStyleClass().add("buvette-jauge");
        jauge.setMinWidth(170);

        TextField quantite = Ui.champ("1");
        quantite.getStyleClass().add("buvette-quantite");
        quantite.setPrefColumnCount(3);
        Button sortir = Ui.boutonSecondaire("− Sortir");
        sortir.setOnAction(e -> lireQuantite(quantite, n -> annoncer(serviceStock.sortir(stock, l.id(), n, auteur()))));
        Button entrer = Ui.boutonSecondaire("+ Entrer");
        entrer.setOnAction(e -> lireQuantite(quantite, n -> annoncer(serviceStock.entrer(stock, l.id(), n, auteur()))));
        Button recompter = Ui.boutonLien("Recompter");
        recompter.setOnAction(e -> {
            if (quantite.getText().isBlank()) {
                Ui.erreur(message, "Tapez le nombre compté dans la case, puis « Recompter ».");
                Ui.marquerErreur(quantite);
                return;
            }
            annoncer(serviceStock.recompter(stock, l.id(), quantite.getText(), auteur()));
        });
        Button fiche = Ui.boutonLien("Fiche");
        fiche.setOnAction(e -> {
            articleEdite = l.id();
            vueCatalogue = false;
            ouvrir(ONGLET_ARTICLES);
        });
        HBox actions = new HBox(6, quantite, sortir, entrer, recompter, fiche);
        actions.setAlignment(Pos.CENTER_LEFT);
        HBox ligne = Ui.ligneElement(null, l.nom(), l.detail(), jauge, actions);
        ligne.getStyleClass().add("buvette-article");
        return ligne;
    }

    /** Quantité de la case (vide : 1) ; message d'erreur si elle est illisible. */
    private void lireQuantite(TextField champ, java.util.function.LongConsumer action) {
        String t = champ.getText() == null ? "" : champ.getText().trim();
        if (t.isEmpty()) {
            action.accept(1);
            return;
        }
        if (!t.matches("\\d{1,7}") || Long.parseLong(t) < 1) {
            Ui.erreur(message, "La quantité doit être un nombre entier d'au moins 1.");
            Ui.marquerErreur(champ);
            return;
        }
        action.accept(Long.parseLong(t));
    }
}
