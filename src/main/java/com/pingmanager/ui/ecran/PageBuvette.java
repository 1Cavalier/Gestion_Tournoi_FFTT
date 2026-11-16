package com.pingmanager.ui.ecran;

import com.pingmanager.core.DroitRefuseException;
import com.pingmanager.core.Horloge;
import com.pingmanager.core.buvette.Centimes;
import com.pingmanager.core.buvette.ResultatArticleBuvette;
import com.pingmanager.core.buvette.ResultatMouvementStock;
import com.pingmanager.core.buvette.SaisieArticle;
import com.pingmanager.core.buvette.ServiceArticlesBuvette;
import com.pingmanager.core.buvette.ServiceStockBuvette;
import com.pingmanager.model.AccesClub;
import com.pingmanager.model.buvette.ArticleBuvette;
import com.pingmanager.model.buvette.FamilleArticle;
import com.pingmanager.model.buvette.LienContenant;
import com.pingmanager.model.buvette.StockBuvette;
import com.pingmanager.ui.ContexteApplication;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Page « Buvette » du groupe Gestion (B026), réservée aux organisateurs : un
 * seul écran. En tête la liste « À racheter », puis les produits et les
 * contenants avec leur jauge et leurs boutons rapides (sortir, entrer,
 * recompter), enfin le formulaire d'article. La logique d'affichage est dans
 * {@link PresentationStockBuvette}, les règles dans {@code core.buvette}.
 *
 * <p>Le stock appartient au club : il s'enregistre avec les données
 * d'application à chaque action.</p>
 */
final class PageBuvette {

    private final ContexteApplication contexte;
    private final ServiceArticlesBuvette serviceArticles = new ServiceArticlesBuvette();
    private final ServiceStockBuvette serviceStock = new ServiceStockBuvette(Horloge.systeme());

    private final VBox racine = new VBox(20);
    private final VBox contenu = new VBox(20);
    private final Label message = Ui.message();
    private String numeroClub;
    /** Article ouvert dans le formulaire ({@code null} : nouvel article). */
    private String articleEdite;

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
                    articleEdite = null;
                    afficher();
                }
            });
            entete.getChildren().addAll(Ui.espace(), Ui.libelleChamp("Club"), choix);
        } else {
            entete.getChildren().addAll(Ui.espace(), Ui.discret(clubs.values().iterator().next()));
        }
        racine.getChildren().setAll(entete,
                Ui.aide("Suivi du stock pendant le tournoi : sortez ce qui part, entrez ce qui arrive, "
                        + "recomptez quand il le faut. Sortir une portion d'un produit sort aussi ses contenants."),
                message, contenu);
        racine.getStyleClass().add("page-buvette");
        afficher();
        return Ui.defilant(Ui.page(1100, racine));
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

    private StockBuvette stock() {
        return contexte.stockBuvette(numeroClub);
    }

    /** Qui saisit : le nom du compte connecté. */
    private String auteur() {
        com.pingmanager.model.CompteUtilisateur c = contexte.compteConnecte();
        if (c == null) {
            return "inconnu";
        }
        String nom = c.getNomComplet();
        return nom == null || nom.isBlank() ? c.getEmail() : nom;
    }

    /** Reconstruit la page (après chaque action). */
    private void afficher() {
        Node n;
        try {
            n = construire();
        } catch (DroitRefuseException refus) {
            n = Ui.carteNue(Ui.etatVide(refus.getMessage(), null));
        }
        contenu.getChildren().setAll(n);
    }

    /** Enregistre le stock du club, affiche le message, puis reconstruit la page. */
    private void enregistrer(boolean accepte, String explication) {
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

    private void annoncer(ResultatMouvementStock r) {
        enregistrer(r.accepte(), r.explication());
    }

    private void annoncer(ResultatArticleBuvette r) {
        if (r.accepte()) {
            articleEdite = null;
        }
        enregistrer(r.accepte(), r.explication());
    }

    private Node construire() {
        StockBuvette stock = stock();
        PresentationStockBuvette p = new PresentationStockBuvette(stock);
        VBox v = new VBox(20);
        v.getChildren().add(carteARacheter(p));
        if (p.vide()) {
            v.getChildren().add(Ui.carteNue(Ui.etatVideCompact(PresentationStockBuvette.MESSAGE_VIDE)));
        } else {
            for (PresentationStockBuvette.Section s : p.sections()) {
                v.getChildren().add(carteSection(stock, s));
            }
        }
        v.getChildren().add(carteFormulaire(stock, p));
        List<String> mouvements = p.derniersMouvements(8, ZoneId.systemDefault());
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
        Button modifier = Ui.boutonLien("Modifier");
        modifier.setOnAction(e -> {
            articleEdite = l.id();
            afficher();
        });
        HBox actions = new HBox(6, quantite, sortir, entrer, recompter, modifier);
        if (l.retirable()) {
            Button retirer = Ui.boutonLien("Retirer");
            retirer.getStyleClass().add("lien-danger");
            retirer.setOnAction(e -> annoncer(serviceArticles.retirer(stock, l.id())));
            actions.getChildren().add(retirer);
        }
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

    // ==================================================================
    // Formulaire d'article
    // ==================================================================

    private Node carteFormulaire(StockBuvette stock, PresentationStockBuvette p) {
        ArticleBuvette existant = articleEdite == null ? null : stock.article(articleEdite).orElse(null);
        TextField nom = Ui.champ("ex. Café, Gobelet");
        ComboBox<FamilleArticle> famille = Ui.combo(FamilleArticle.values(),
                existant == null ? FamilleArticle.PRODUIT : existant.getFamille());
        TextField unite = Ui.champ("ex. dose, gobelet, canette");
        TextField prixLot = Ui.champ("ex. 4,50");
        TextField unitesParLot = Ui.champ("ex. 100");
        TextField quantiteStock = Ui.champ("ex. 200");
        TextField seuil = Ui.champ("ex. 30");
        if (existant != null) {
            nom.setText(existant.getNom());
            unite.setText(existant.getUnite());
            prixLot.setText(Centimes.nombre(existant.getPrixLotCentimes()));
            unitesParLot.setText(String.valueOf(existant.getUnitesParLot()));
            quantiteStock.setText(String.valueOf(existant.getStock()));
            quantiteStock.setDisable(true);
            seuil.setText(String.valueOf(existant.getSeuilAlerte()));
        }

        // Contenants utilisés par portion (produits seulement).
        VBox zoneContenants = new VBox(6);
        Map<String, CheckBox> coches = new LinkedHashMap<>();
        Map<String, TextField> quantites = new LinkedHashMap<>();
        List<ArticleBuvette> disponibles = p.contenantsDisponibles();
        zoneContenants.getChildren().add(Ui.libelleChamp("Contenants utilisés à chaque portion"));
        if (disponibles.isEmpty()) {
            zoneContenants.getChildren().add(Ui.aide("Ajoutez d'abord vos contenants (famille « Contenant ou "
                    + "consommable ») pour les lier à ce produit."));
        }
        FlowPane cases = new FlowPane(16, 8);
        for (ArticleBuvette c : disponibles) {
            CheckBox coche = new CheckBox(c.getNom());
            TextField q = Ui.champ("1");
            q.setPrefColumnCount(2);
            q.getStyleClass().add("buvette-quantite");
            if (existant != null) {
                for (LienContenant lien : existant.getContenants()) {
                    if (lien.idContenant().equals(c.getId())) {
                        coche.setSelected(true);
                        q.setText(String.valueOf(lien.quantite()));
                    }
                }
            }
            q.disableProperty().bind(coche.selectedProperty().not());
            coches.put(c.getId(), coche);
            quantites.put(c.getId(), q);
            HBox h = new HBox(6, coche, q, Ui.discret("par portion"));
            h.setAlignment(Pos.CENTER_LEFT);
            cases.getChildren().add(h);
        }
        zoneContenants.getChildren().add(cases);
        Runnable majFamille = () -> Ui.afficherSi(zoneContenants, famille.getValue() == FamilleArticle.PRODUIT);
        famille.valueProperty().addListener((o, a, n) -> majFamille.run());
        majFamille.run();

        Supplier<Map<String, Integer>> liens = () -> {
            Map<String, Integer> res = new LinkedHashMap<>();
            for (Map.Entry<String, CheckBox> e : coches.entrySet()) {
                if (e.getValue().isSelected()) {
                    String t = quantites.get(e.getKey()).getText().trim();
                    res.put(e.getKey(), t.isEmpty() ? 1 : t.matches("\\d{1,4}") ? Integer.parseInt(t) : 0);
                }
            }
            return res;
        };
        Button enregistrer = Ui.boutonPrimaire(existant == null ? "Ajouter l'article" : "Enregistrer");
        enregistrer.setOnAction(e -> annoncer(serviceArticles.enregistrer(stock, new SaisieArticle(
                existant == null ? null : existant.getId(), nom.getText(), famille.getValue(), unite.getText(),
                prixLot.getText(), unitesParLot.getText(), quantiteStock.getText(), seuil.getText(),
                liens.get()))));
        Button nouveau = Ui.boutonSecondaire("Nouvel article");
        nouveau.setOnAction(e -> {
            articleEdite = null;
            afficher();
        });
        FlowPane champs = new FlowPane(12, 12,
                Ui.champEtiquete("Nom", nom), Ui.champEtiquete("Famille", famille),
                Ui.champEtiquete("Unité comptée", unite), Ui.champEtiquete("Prix d'un lot (€)", prixLot),
                Ui.champEtiquete("Unités par lot", unitesParLot),
                Ui.champEtiquete(existant == null ? "Stock de départ" : "Stock (boutons de la liste)", quantiteStock),
                Ui.champEtiquete("Seuil d'alerte", seuil));
        VBox corps = new VBox(12, champs,
                Ui.aide("Le coût se saisit comme vous achetez : « 4,50 € les 100 gobelets ». "
                        + "Pour un produit, une unité de stock = une portion servie."),
                zoneContenants,
                existant == null ? Ui.actions(enregistrer) : Ui.actions(enregistrer, nouveau));
        return Ui.carte(existant == null ? "Ajouter un article" : "Modifier « " + existant.getNom() + " »", corps);
    }
}
