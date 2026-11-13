package com.pingmanager.ui.ecran;

import com.pingmanager.core.DroitRefuseException;
import com.pingmanager.core.buvette.Centimes;
import com.pingmanager.core.Horloge;
import com.pingmanager.core.buvette.BilanBuvette;
import com.pingmanager.core.buvette.BudgetCalcule;
import com.pingmanager.core.buvette.CalculBilanBuvette;
import com.pingmanager.core.buvette.CalculBudgetBuvette;
import com.pingmanager.core.buvette.ResultatBudgetBuvette;
import com.pingmanager.core.buvette.ResultatIngredientBuvette;
import com.pingmanager.core.buvette.ResultatMouvementStock;
import com.pingmanager.core.buvette.ResultatProduitBuvette;
import com.pingmanager.core.buvette.ServiceBudgetBuvette;
import com.pingmanager.core.buvette.ServiceIngredientsBuvette;
import com.pingmanager.core.buvette.ServiceStockBuvette;
import com.pingmanager.core.buvette.ServiceVentesBuvette;
import com.pingmanager.core.buvette.ResultatVenteBuvette;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.buvette.BuvetteTournoi;
import com.pingmanager.model.buvette.TypeMouvementStock;
import com.pingmanager.persistence.DepotBuvette;
import com.pingmanager.core.buvette.ServiceCatalogueBuvette;
import com.pingmanager.core.buvette.SuggestionsBuvette;
import com.pingmanager.model.AccesClub;
import com.pingmanager.model.buvette.CatalogueBuvette;
import com.pingmanager.model.buvette.CategorieProduit;
import com.pingmanager.model.buvette.Ingredient;
import com.pingmanager.model.buvette.LigneRecette;
import com.pingmanager.model.buvette.Produit;
import com.pingmanager.model.buvette.UniteVente;
import com.pingmanager.persistence.SuggestionsBuvetteJson;
import com.pingmanager.ui.ContexteApplication;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Page « Buvette » du groupe Gestion (B046), réservée aux organisateurs : onglets
 * Catalogue | Stock | Budget | Ventes | Historique. Toute la logique d'affichage est
 * dans les classes {@code Presentation…Buvette}, testées hors JavaFX ; les calculs
 * sont dans {@code core.buvette}.
 *
 * <p>Le catalogue (produits, ingrédients) appartient au club et s'enregistre avec
 * les données d'application ; le stock, le budget et les ventes appartiennent au
 * tournoi et s'enregistrent dans un fichier voisin du fichier du tournoi.</p>
 */
final class PageBuvette {

    static final String CATALOGUE = "Catalogue";
    static final String STOCK = "Stock";
    static final String BUDGET = "Budget";
    static final String VENTES = "Ventes";
    static final String HISTORIQUE = "Historique";
    static final List<String> ONGLETS = List.of(CATALOGUE, STOCK, BUDGET, VENTES, HISTORIQUE);

    private final ContexteApplication contexte;
    private final SuggestionsBuvette suggestions = SuggestionsBuvetteJson.charger();
    private final ServiceCatalogueBuvette serviceCatalogue = new ServiceCatalogueBuvette();
    private final ServiceIngredientsBuvette serviceIngredients = new ServiceIngredientsBuvette();
    private final ServiceStockBuvette serviceStock = new ServiceStockBuvette(Horloge.systeme());
    private final ServiceBudgetBuvette serviceBudget = new ServiceBudgetBuvette();
    private final ServiceVentesBuvette serviceVentes = new ServiceVentesBuvette(Horloge.systeme());
    /** Saison filtrée et tournoi détaillé dans l'onglet Historique. */
    private String saisonHistorique = PresentationHistoriqueBuvette.TOUTES;
    private int bilanOuvert = -1;
    /** Scénario affiché dans l'onglet Budget. */
    private String scenarioCourant = "Moyen";
    /** Tournoi choisi pour les onglets Stock, Budget et Ventes ({@code null} : aucun). */
    private Path fichierTournoi;
    /** Produit dont la recette est ouverte dans l'onglet Catalogue ({@code null} : aucune). */
    private String produitRecette;

    private final VBox racine = new VBox(20);
    private final VBox contenu = new VBox(20);
    private final Label message = Ui.message();
    private String numeroClub;
    private String ongletCourant = CATALOGUE;

    PageBuvette(ContexteApplication contexte) {
        this.contexte = contexte;
    }

    Parent build() {
        Map<String, String> clubs = clubsGeres();
        if (clubs.isEmpty()) {
            return PagePlaceholder.page("Buvette", "Rattachez votre compte à un club pour gérer sa buvette.");
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
                    fichierTournoi = null;
                    afficher();
                }
            });
            entete.getChildren().addAll(Ui.espace(), Ui.libelleChamp("Club"), choix);
        } else {
            entete.getChildren().addAll(Ui.espace(), Ui.discret(clubs.values().iterator().next()));
        }
        ToggleGroup groupe = new ToggleGroup();
        List<ToggleButton> boutons = new ArrayList<>();
        for (String o : ONGLETS) {
            ToggleButton b = new ToggleButton(o);
            b.setOnAction(e -> {
                if (!b.isSelected()) {
                    b.setSelected(true); // un onglet reste toujours choisi
                    return;
                }
                ongletCourant = o;
                afficher();
            });
            b.setSelected(o.equals(ongletCourant));
            boutons.add(b);
        }
        HBox segment = Ui.segment(groupe, boutons.toArray(new ToggleButton[0]));
        racine.getChildren().setAll(entete, segment, message, contenu);
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

    /** Reconstruit l'onglet courant (après chaque action). */
    private void afficher() {
        Node n;
        try {
            n = construireOnglet();
        } catch (DroitRefuseException refus) {
            n = Ui.carteNue(Ui.etatVide(refus.getMessage(), null));
        }
        contenu.getChildren().setAll(n);
        Ui.apparition(contenu);
    }

    private Node construireOnglet() {
        if (CATALOGUE.equals(ongletCourant)) {
            return ongletCatalogue();
        }
        if (STOCK.equals(ongletCourant)) {
            return avecTournoi(this::ongletStock);
        }
        if (BUDGET.equals(ongletCourant)) {
            return avecTournoi(this::ongletBudget);
        }
        if (VENTES.equals(ongletCourant)) {
            return avecTournoi(this::ongletVentes);
        }
        if (HISTORIQUE.equals(ongletCourant)) {
            return ongletHistorique();
        }
        return Ui.carteNue(Ui.etatVide("Cet onglet arrive bientôt.", null));
    }

    private CatalogueBuvette catalogue() {
        return contexte.catalogueBuvette(numeroClub);
    }

    /** Enregistre les données du club et affiche le message du résultat. */
    private void enregistrerClub(String erreur, String succes) {
        try {
            contexte.sauvegarder();
        } catch (RuntimeException echec) {
            Ui.erreur(message, "Enregistrement impossible : " + echec.getMessage());
            return;
        }
        if (erreur != null) {
            Ui.erreur(message, erreur);
        } else if (succes != null) {
            Ui.succes(message, succes);
        }
        afficher();
    }

    private void annoncer(ResultatIngredientBuvette r) {
        if (r.accepte()) {
            enregistrerClub(null, r.explication());
        } else {
            Ui.erreur(message, r.explication());
        }
    }

    private void annoncer(ResultatProduitBuvette r) {
        if (r.accepte()) {
            enregistrerClub(null, r.explication());
        } else {
            Ui.erreur(message, r.explication());
        }
    }

    // ==================================================================
    // Onglet Catalogue
    // ==================================================================

    private Node ongletCatalogue() {
        CatalogueBuvette cat = catalogue();
        PresentationCatalogueBuvette p = new PresentationCatalogueBuvette(cat, suggestions, serviceCatalogue);
        VBox v = new VBox(20);
        v.getChildren().add(tuiles(p.tuiles().stream()
                .map(t -> Ui.tuileChiffre(t.valeur(), t.libelle())).toList()));

        // Liste des produits par catégorie.
        VBox liste = new VBox(4);
        if (p.vide()) {
            liste.getChildren().add(Ui.etatVideCompact(p.messageVide()));
        }
        VBox formulaire = new VBox(12);
        for (PresentationCatalogueBuvette.Groupe g : p.groupes()) {
            liste.getChildren().add(Ui.label(g.titre(), "buvette-categorie"));
            boolean premiere = true;
            for (PresentationCatalogueBuvette.LigneProduit l : g.lignes()) {
                Button modifier = Ui.boutonLien("Modifier");
                modifier.setOnAction(e -> remplirFormulaire(formulaire, cat, p, l.id()));
                Button bascule = Ui.boutonLien(l.actif() ? "Désactiver" : "Réactiver");
                bascule.setOnAction(e -> annoncer(serviceCatalogue.changerActif(cat, l.id(), !l.actif())));
                Button recette = Ui.boutonLien("Recette");
                recette.setOnAction(e -> {
                    produitRecette = l.id();
                    afficher();
                });
                List<Node> droite = new ArrayList<>();
                if (!l.actif()) {
                    droite.add(Ui.pastille("Désactivé", StatutPastille.NEUTRE));
                }
                droite.add(recette);
                droite.add(modifier);
                droite.add(bascule);
                if ("Retirer".equals(l.actionRetrait())) {
                    Button retirer = Ui.boutonLien("Retirer");
                    retirer.setOnAction(e -> annoncer(serviceCatalogue.retirer(cat, l.id())));
                    droite.add(retirer);
                }
                HBox ligne = Ui.ligneElement(null, l.nom(), l.detail(), droite.toArray(new Node[0]));
                if (premiere) {
                    ligne.getStyleClass().add("ligne-element-premiere");
                    premiere = false;
                }
                liste.getChildren().add(ligne);
            }
        }
        remplirFormulaire(formulaire, cat, p, null);
        v.getChildren().add(Ui.carte(Ui.titreAvecCompteur("Produits du club", cat.getProduits().size()), liste));
        p.recette(produitRecette).ifPresent(r -> v.getChildren().add(carteRecette(cat, p, r)));
        v.getChildren().add(Ui.carte("Ajouter ou modifier un produit", formulaire));
        v.getChildren().add(carteIngredients(cat, p));
        v.getChildren().add(carteSuggestions(cat, p));
        v.getChildren().add(carteSuggestionsIngredients(cat, p));
        v.getChildren().add(carteAlcool(cat, p));
        return v;
    }

    private void remplirFormulaire(VBox zone, CatalogueBuvette cat, PresentationCatalogueBuvette p, String id) {
        Produit existant = id == null ? null : cat.produit(id).orElse(null);
        TextField nom = Ui.champ("Nom (ex. Café)");
        ComboBox<CategorieProduit> categorie = Ui.combo(
                p.categoriesSaisissables().toArray(new CategorieProduit[0]), CategorieProduit.BOISSONS_FROIDES);
        ComboBox<UniteVente> unite = Ui.combo(UniteVente.values(), UniteVente.PIECE);
        TextField prix = Ui.champ("Prix en euros (ex. 1,50)");
        if (existant != null) {
            nom.setText(existant.getNom());
            categorie.getSelectionModel().select(existant.getCategorie());
            unite.getSelectionModel().select(existant.getUnite());
            prix.setText(Centimes.nombre(existant.getPrixVenteCentimes()));
        }
        Button enregistrer = Ui.boutonPrimaire(existant == null ? "Ajouter le produit" : "Enregistrer");
        enregistrer.setOnAction(e -> annoncer(serviceCatalogue.saisir(cat, id, nom.getText(),
                categorie.getValue(), unite.getValue(), prix.getText())));
        Button annuler = Ui.boutonSecondaire("Nouveau produit");
        annuler.setOnAction(e -> remplirFormulaire(zone, cat, p, null));
        FlowPane champs = new FlowPane(12, 12,
                Ui.champEtiquete("Nom", nom), Ui.champEtiquete("Catégorie", categorie),
                Ui.champEtiquete("Unité de vente", unite), Ui.champEtiquete("Prix de vente", prix));
        zone.getChildren().setAll(champs, existant == null ? Ui.actions(enregistrer) : Ui.actions(enregistrer, annuler));
    }

    private Node carteSuggestions(CatalogueBuvette cat, PresentationCatalogueBuvette p) {
        List<PresentationCatalogueBuvette.LigneSuggestion> lignes = p.suggestions();
        VBox corps = new VBox(4);
        corps.getChildren().add(Ui.aide(p.mentionSuggestions()));
        VBox confirmation = new VBox();
        Button tout = Ui.boutonSecondaire("Tout ajouter…");
        tout.setDisable(lignes.isEmpty());
        tout.setOnAction(e -> {
            Button oui = Ui.boutonPrimaire("Tout ajouter");
            Button non = Ui.boutonSecondaire("Renoncer");
            oui.setOnAction(x -> {
                int n = serviceCatalogue.ajouterToutesSuggestions(cat, suggestions);
                enregistrerClub(null, n + (n > 1 ? " produits ajoutés." : " produit ajouté."));
            });
            non.setOnAction(x -> confirmation.getChildren().clear());
            confirmation.getChildren().setAll(Ui.encartConfirmation(p.confirmationToutAjouter(), oui, non));
        });
        corps.getChildren().add(confirmation);
        if (lignes.isEmpty()) {
            corps.getChildren().add(Ui.etatVideCompact("Toutes les suggestions sont déjà dans votre catalogue."));
        }
        boolean premiere = true;
        for (PresentationCatalogueBuvette.LigneSuggestion l : lignes) {
            Button ajouter = Ui.boutonLien("Ajouter");
            ajouter.setOnAction(e -> annoncer(serviceCatalogue.ajouterSuggestion(cat, suggestions, l.id())));
            HBox ligne = Ui.ligneElement(null, l.nom(), l.detail(),
                    Ui.pastille("Suggestion", StatutPastille.INFO), ajouter);
            if (premiere) {
                ligne.getStyleClass().add("ligne-element-premiere");
                premiere = false;
            }
            corps.getChildren().add(ligne);
        }
        return Ui.carte(Ui.enteteAvecActions(Ui.titreAvecCompteur("Suggestions", lignes.size()), tout), corps);
    }

    /** Recette du produit choisi : lignes expliquées, ajout et retrait d'ingrédients. */
    private Node carteRecette(CatalogueBuvette cat, PresentationCatalogueBuvette p,
                              PresentationCatalogueBuvette.Recette r) {
        VBox corps = new VBox(6);
        List<LigneRecette> actuelles = new ArrayList<>();
        r.lignes().forEach(l -> actuelles.add(new LigneRecette(l.idIngredient(), l.quantite())));
        if (r.lignes().isEmpty()) {
            corps.getChildren().add(Ui.etatVideCompact("Aucun ingrédient pour l'instant."));
        }
        for (PresentationCatalogueBuvette.LigneRecetteAffichee l : r.lignes()) {
            Button retirer = Ui.boutonLien("Retirer");
            retirer.setOnAction(e -> {
                List<LigneRecette> sans = new ArrayList<>(actuelles);
                sans.removeIf(x -> x.idIngredient().equals(l.idIngredient()));
                annoncer(serviceIngredients.definirRecette(cat, produitRecette, sans));
            });
            corps.getChildren().add(Ui.ligneListe(Ui.texte(l.explication()), Ui.espace(), retirer));
        }
        corps.getChildren().add(Ui.label(r.synthese(), "buvette-synthese"));
        ComboBox<Ingredient> choix = new ComboBox<>(FXCollections.observableArrayList(p.ingredientsProposables()));
        choix.setPromptText("Ingrédient…");
        TextField quantite = Ui.champ("Portions");
        quantite.setText("1");
        quantite.setPrefColumnCount(4);
        Button ajouter = Ui.boutonSecondaire("Ajouter à la recette");
        ajouter.setOnAction(e -> {
            int q;
            try {
                q = Integer.parseInt(quantite.getText().trim());
            } catch (NumberFormatException illisible) {
                q = 0;
            }
            if (choix.getValue() == null || q < 1) {
                Ui.erreur(message, "Choisissez un ingrédient et un nombre de portions (au moins 1).");
                return;
            }
            List<LigneRecette> avec = new ArrayList<>(actuelles);
            avec.add(new LigneRecette(choix.getValue().getId(), q));
            annoncer(serviceIngredients.definirRecette(cat, produitRecette, avec));
        });
        Button fermer = Ui.boutonLien("Fermer");
        fermer.setOnAction(e -> {
            produitRecette = null;
            afficher();
        });
        corps.getChildren().add(Ui.ligne(10, choix, quantite, ajouter));
        if (p.ingredientsProposables().isEmpty()) {
            corps.getChildren().add(Ui.aide("Ajoutez d'abord des ingrédients (carte « Ingrédients » ou suggestions)."));
        }
        return Ui.carte(Ui.enteteAvecActions(Ui.titreCarte(r.titre()), fermer), corps);
    }

    /** Ingrédients du club : liste et formulaire de saisie. */
    private Node carteIngredients(CatalogueBuvette cat, PresentationCatalogueBuvette p) {
        VBox liste = new VBox(4);
        VBox formulaire = new VBox(12);
        List<PresentationCatalogueBuvette.LigneIngredient> lignes = p.ingredients();
        if (lignes.isEmpty()) {
            liste.getChildren().add(Ui.etatVideCompact("Aucun ingrédient. Partez des suggestions ou ajoutez les vôtres."));
        }
        boolean premiere = true;
        for (PresentationCatalogueBuvette.LigneIngredient l : lignes) {
            Button modifier = Ui.boutonLien("Modifier");
            modifier.setOnAction(e -> remplirFormulaireIngredient(formulaire, cat, l.id()));
            List<Node> droite = new ArrayList<>();
            if (!l.actif()) {
                droite.add(Ui.pastille("Désactivé", StatutPastille.NEUTRE));
                Button reactiver = Ui.boutonLien("Réactiver");
                reactiver.setOnAction(e -> annoncer(serviceIngredients.changerActif(cat, l.id(), true)));
                droite.add(reactiver);
            }
            droite.add(modifier);
            if (l.actif()) {
                Button retirer = Ui.boutonLien(l.actionRetrait());
                retirer.setOnAction(e -> annoncer(serviceIngredients.retirer(cat, l.id())));
                droite.add(retirer);
            }
            HBox ligne = Ui.ligneElement(null, l.nom(), l.detail(), droite.toArray(new Node[0]));
            if (premiere) {
                ligne.getStyleClass().add("ligne-element-premiere");
                premiere = false;
            }
            liste.getChildren().add(ligne);
        }
        remplirFormulaireIngredient(formulaire, cat, null);
        return Ui.carte(Ui.titreAvecCompteur("Ingrédients", lignes.size()), liste,
                Ui.aide("Rendement : combien de portions vendables donne une unité d'achat "
                        + "(« 1 baguette = 2 à 3 sandwichs »). Le coût retenu prend le rendement le plus bas."),
                formulaire);
    }

    private void remplirFormulaireIngredient(VBox zone, CatalogueBuvette cat, String id) {
        Ingredient existant = id == null ? null : cat.ingredient(id).orElse(null);
        TextField nom = Ui.champ("Nom (ex. Baguette)");
        TextField uniteAchat = Ui.champ("Unité d'achat (ex. baguette)");
        TextField portion = Ui.champ("Portion (ex. sandwich)");
        TextField prix = Ui.champ("Prix d'achat (ex. 1,10)");
        TextField min = Ui.champ("Min");
        TextField max = Ui.champ("Max");
        TextField perte = Ui.champ("0");
        min.setPrefColumnCount(4);
        max.setPrefColumnCount(4);
        perte.setPrefColumnCount(4);
        if (existant != null) {
            nom.setText(existant.getNom());
            uniteAchat.setText(existant.getUniteAchat());
            portion.setText(existant.getUnitePortion());
            prix.setText(Centimes.nombre(existant.getPrixAchatCentimes()));
            min.setText(String.valueOf(existant.getRendementMin()));
            max.setText(String.valueOf(existant.getRendementMax()));
            perte.setText(String.valueOf(existant.getPertePourcent()));
        }
        Button enregistrer = Ui.boutonPrimaire(existant == null ? "Ajouter l'ingrédient" : "Enregistrer l'ingrédient");
        enregistrer.setOnAction(e -> annoncer(serviceIngredients.saisir(cat, id, nom.getText(), uniteAchat.getText(),
                portion.getText(), prix.getText(), min.getText(), max.getText(), perte.getText())));
        Button nouveau = Ui.boutonSecondaire("Nouvel ingrédient");
        nouveau.setOnAction(e -> remplirFormulaireIngredient(zone, cat, null));
        FlowPane champs = new FlowPane(12, 12,
                Ui.champEtiquete("Nom", nom), Ui.champEtiquete("Unité d'achat", uniteAchat),
                Ui.champEtiquete("Portion vendue", portion), Ui.champEtiquete("Prix d'achat", prix),
                Ui.champEtiquete("Rendement min", min), Ui.champEtiquete("Rendement max", max),
                Ui.champEtiquete("Perte (%)", perte));
        zone.getChildren().setAll(champs, existant == null ? Ui.actions(enregistrer) : Ui.actions(enregistrer, nouveau));
    }

    private Node carteSuggestionsIngredients(CatalogueBuvette cat, PresentationCatalogueBuvette p) {
        List<PresentationCatalogueBuvette.LigneSuggestion> lignes = p.suggestionsIngredients();
        VBox corps = new VBox(4, Ui.aide(p.mentionSuggestions()));
        if (lignes.isEmpty()) {
            corps.getChildren().add(Ui.etatVideCompact("Tous les ingrédients suggérés sont déjà dans votre liste."));
        }
        boolean premiere = true;
        for (PresentationCatalogueBuvette.LigneSuggestion l : lignes) {
            Button ajouter = Ui.boutonLien("Ajouter");
            ajouter.setOnAction(e -> annoncer(serviceIngredients.ajouterSuggestion(cat, suggestions, l.id())));
            HBox ligne = Ui.ligneElement(null, l.nom(), l.detail(),
                    Ui.pastille("Suggestion", StatutPastille.INFO), ajouter);
            if (premiere) {
                ligne.getStyleClass().add("ligne-element-premiere");
                premiere = false;
            }
            corps.getChildren().add(ligne);
        }
        return Ui.carte(Ui.titreAvecCompteur("Ingrédients suggérés", lignes.size()), corps);
    }

    private Node carteAlcool(CatalogueBuvette cat, PresentationCatalogueBuvette p) {
        VBox zone = new VBox(10);
        Button bascule = Ui.boutonSecondaire(p.actionAlcool());
        bascule.setOnAction(e -> {
            if (cat.isAlcoolActive()) {
                serviceCatalogue.activerAlcool(cat, false);
                enregistrerClub(null, "Catégorie « Boissons alcoolisées » désactivée.");
                return;
            }
            Button oui = Ui.boutonPrimaire("J'ai lu, activer");
            Button non = Ui.boutonSecondaire("Renoncer");
            oui.setOnAction(x -> {
                serviceCatalogue.activerAlcool(cat, true);
                enregistrerClub(null, "Catégorie « Boissons alcoolisées » activée.");
            });
            non.setOnAction(x -> zone.getChildren().setAll(bascule));
            zone.getChildren().setAll(Ui.encartAlerte("Avant de vendre de l'alcool",
                    ServiceCatalogueBuvette.AVERTISSEMENT_ALCOOL), Ui.actions(oui, non));
        });
        zone.getChildren().add(bascule);
        return Ui.carte("Boissons alcoolisées", Ui.aide("Jamais proposées par défaut."), zone);
    }

    // ==================================================================
    // Choix du tournoi (onglets Stock, Budget, Ventes)
    // ==================================================================

    /** Tournois organisés par le club choisi, du plus récent au plus ancien. */
    private List<ContexteApplication.TournoiFichier> tournoisDuClub() {
        List<ContexteApplication.TournoiFichier> res = new ArrayList<>();
        for (ContexteApplication.TournoiFichier tf : contexte.mesTournoisClub(contexte.tousLesTournois())) {
            String n = tf.tournoi().getNumeroAffiliationClub();
            if (n != null && n.trim().equals(numeroClub.trim())) {
                res.add(tf);
            }
        }
        res.sort((a, b) -> b.tournoi().getDate().compareTo(a.tournoi().getDate()));
        return res;
    }

    /** Sélecteur de tournoi puis le contenu de l'onglet pour ce tournoi. */
    private Node avecTournoi(java.util.function.Function<Tournoi, Node> onglet) {
        List<ContexteApplication.TournoiFichier> tournois = tournoisDuClub();
        if (tournois.isEmpty()) {
            return Ui.carteNue(Ui.etatVide("Aucun tournoi pour ce club. Créez-en un dans « Gestion des tournois » "
                    + "pour préparer sa buvette.", null));
        }
        ContexteApplication.TournoiFichier choisi = null;
        for (ContexteApplication.TournoiFichier tf : tournois) {
            if (tf.fichier().equals(fichierTournoi)) {
                choisi = tf;
            }
        }
        if (choisi == null) {
            List<LocalDate> fins = tournois.stream().map(tf -> tf.tournoi().getJours().last()).toList();
            choisi = tournois.get(Math.max(0, PresentationStockBuvette.tournoiParDefaut(fins, LocalDate.now())));
            fichierTournoi = choisi.fichier();
        }
        Map<Path, String> noms = new LinkedHashMap<>();
        tournois.forEach(tf -> noms.put(tf.fichier(), tf.tournoi().getNom() + " — "
                + com.pingmanager.core.reglement.Formats.jourCourt(tf.tournoi().getDate())));
        ComboBox<Path> choix = new ComboBox<>(FXCollections.observableArrayList(noms.keySet()));
        choix.setConverter(new StringConverter<>() {
            @Override
            public String toString(Path p) {
                return p == null ? "" : noms.getOrDefault(p, p.getFileName().toString());
            }

            @Override
            public Path fromString(String s) {
                return null;
            }
        });
        choix.getSelectionModel().select(fichierTournoi);
        choix.valueProperty().addListener((o, a, n) -> {
            if (n != null && !n.equals(fichierTournoi)) {
                fichierTournoi = n;
                afficher();
            }
        });
        VBox v = new VBox(20, Ui.ligne(12, Ui.libelleChamp("Tournoi"), choix));
        v.getChildren().add(onglet.apply(choisi.tournoi()));
        return v;
    }

    /** Buvette du tournoi choisi, relue depuis son fichier. */
    private BuvetteTournoi buvette() {
        return DepotBuvette.charger(fichierTournoi);
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

    /** Enregistre la buvette du tournoi (et le catalogue, qui note les ingrédients utilisés). */
    private void enregistrerTournoi(BuvetteTournoi b, String erreur, String succes) {
        try {
            DepotBuvette.sauvegarder(b, fichierTournoi);
        } catch (RuntimeException echec) {
            Ui.erreur(message, "Enregistrement de la buvette impossible : " + echec.getMessage());
            return;
        }
        enregistrerClub(erreur, succes);
    }

    private void annoncer(BuvetteTournoi b, ResultatMouvementStock r) {
        if (r.accepte()) {
            enregistrerTournoi(b, null, r.explication());
        } else {
            Ui.erreur(message, r.explication());
        }
    }

    // ==================================================================
    // Onglet Stock
    // ==================================================================

    private Node ongletStock(Tournoi tournoi) {
        CatalogueBuvette cat = catalogue();
        BuvetteTournoi b = buvette();
        PresentationStockBuvette p = new PresentationStockBuvette(b, cat, ZoneId.systemDefault());
        VBox v = new VBox(20);
        v.getChildren().add(tuiles(p.tuiles().stream().map(t -> Ui.tuileChiffre(t.valeur(), t.libelle())).toList()));
        if (p.sansIngredient()) {
            v.getChildren().add(Ui.carteNue(Ui.etatVide(p.messageSansIngredient(), null)));
            return v;
        }
        VBox liste = new VBox(4);
        boolean premiere = true;
        for (PresentationStockBuvette.LigneStock l : p.lignes()) {
            HBox ligne = l.sousLeSeuil()
                    ? Ui.ligneElement(null, l.nom(), l.detail(), Ui.pastille("Sous le seuil", StatutPastille.ATTENTION))
                    : Ui.ligneElement(null, l.nom(), l.detail());
            if (premiere) {
                ligne.getStyleClass().add("ligne-element-premiere");
                premiere = false;
            }
            liste.getChildren().add(ligne);
        }
        v.getChildren().add(Ui.carte("Stock par ingrédient", Ui.aide("Le stock se compte en portions vendables. "
                + "Un achat saisi en unités d'achat est converti avec le rendement le plus bas."), liste));
        v.getChildren().add(carteSaisieStock(cat, b));
        v.getChildren().add(carteSeuil(cat, b));
        v.getChildren().add(carteReport(cat, b, tournoi));
        v.getChildren().add(carteJournal(b, p));
        return v;
    }

    private ComboBox<Ingredient> comboIngredients(CatalogueBuvette cat) {
        ComboBox<Ingredient> c = new ComboBox<>(FXCollections.observableArrayList(
                cat.getIngredients().stream().filter(Ingredient::isActif).toList()));
        c.setPromptText("Ingrédient…");
        return c;
    }

    private Node carteSaisieStock(CatalogueBuvette cat, BuvetteTournoi b) {
        ComboBox<TypeMouvementStock> type = Ui.combo(PresentationStockBuvette.TYPES_SAISIS, TypeMouvementStock.ACHAT);
        ComboBox<Ingredient> ingredient = comboIngredients(cat);
        TextField quantite = Ui.champ("Quantité");
        TextField prix = Ui.champ("Prix payé (ex. 11,00)");
        DatePicker date = new DatePicker(LocalDate.now());
        Label libelleQuantite = Ui.libelleChamp(PresentationStockBuvette.libelleQuantite(TypeMouvementStock.ACHAT));
        VBox champQuantite = new VBox(6, libelleQuantite, quantite);
        VBox champPrix = Ui.champEtiquete("Prix payé", prix);
        VBox champDate = Ui.champEtiquete("Date d'achat", date);
        FlowPane champs = new FlowPane(12, 12, Ui.champEtiquete("Mouvement", type),
                Ui.champEtiquete("Ingrédient", ingredient), champQuantite, champPrix, champDate);
        type.valueProperty().addListener((o, a, n) -> {
            if (n == null) {
                return;
            }
            libelleQuantite.setText(PresentationStockBuvette.libelleQuantite(n));
            boolean achat = n == TypeMouvementStock.ACHAT;
            champPrix.setVisible(achat);
            champPrix.setManaged(achat);
            champDate.setVisible(achat);
            champDate.setManaged(achat);
        });
        Button enregistrer = Ui.boutonPrimaire("Enregistrer le mouvement");
        enregistrer.setOnAction(e -> annoncer(b, PresentationStockBuvette.saisir(serviceStock, b, cat, type.getValue(),
                ingredient.getValue() == null ? null : ingredient.getValue().getId(), quantite.getText(),
                prix.getText(), date.getValue(), auteur())));
        return Ui.carte("Saisir un mouvement", champs,
                Ui.aide("Inventaire de fin : saisissez ce que vous comptez ; l'écart avec le stock théorique est "
                        + "enregistré. Aucun mouvement ne s'efface : une erreur se corrige depuis le journal."),
                Ui.actions(enregistrer));
    }

    private Node carteSeuil(CatalogueBuvette cat, BuvetteTournoi b) {
        ComboBox<Ingredient> ingredient = comboIngredients(cat);
        TextField seuil = Ui.champ("Portions (vide : aucun)");
        Button enregistrer = Ui.boutonSecondaire("Fixer le seuil");
        enregistrer.setOnAction(e -> {
            if (ingredient.getValue() == null) {
                Ui.erreur(message, ResultatMouvementStock.Motif.INGREDIENT_INCONNU.explication());
                return;
            }
            long valeur;
            String t = seuil.getText() == null ? "" : seuil.getText().trim();
            if (t.isEmpty()) {
                valeur = -1;
            } else if (t.matches("\\d{1,7}")) {
                valeur = Long.parseLong(t);
            } else {
                Ui.erreur(message, ResultatMouvementStock.Motif.QUANTITE_INVALIDE.explication());
                return;
            }
            serviceStock.definirSeuil(b, cat, ingredient.getValue().getId(), valeur);
            enregistrerTournoi(b, null, valeur < 0 ? "Seuil retiré." : "Seuil d'alerte enregistré.");
        });
        return Ui.carte("Seuil d'alerte", Ui.aide("Une pastille « Sous le seuil » signale l'ingrédient à racheter."),
                Ui.ligne(10, ingredient, seuil, enregistrer));
    }

    private Node carteReport(CatalogueBuvette cat, BuvetteTournoi b, Tournoi tournoi) {
        List<ContexteApplication.TournoiFichier> autres = new ArrayList<>();
        for (ContexteApplication.TournoiFichier tf : tournoisDuClub()) {
            if (!tf.fichier().equals(fichierTournoi)) {
                autres.add(tf);
            }
        }
        VBox zone = new VBox(10);
        if (autres.isEmpty()) {
            zone.getChildren().add(Ui.etatVideCompact("Aucun autre tournoi du club."));
            return Ui.carte("Reprendre le stock d'un autre tournoi", zone);
        }
        ComboBox<ContexteApplication.TournoiFichier> source = new ComboBox<>(FXCollections.observableArrayList(autres));
        source.setConverter(new StringConverter<>() {
            @Override
            public String toString(ContexteApplication.TournoiFichier tf) {
                return tf == null ? "" : tf.tournoi().getNom();
            }

            @Override
            public ContexteApplication.TournoiFichier fromString(String s) {
                return null;
            }
        });
        source.setPromptText("Tournoi d'origine…");
        Button reporter = Ui.boutonSecondaire("Reporter le stock restant…");
        VBox confirmation = new VBox();
        reporter.setOnAction(e -> {
            if (source.getValue() == null) {
                Ui.erreur(message, "Choisissez le tournoi dont le stock restant est reporté.");
                return;
            }
            Button oui = Ui.boutonPrimaire("Reporter");
            Button non = Ui.boutonSecondaire("Renoncer");
            ContexteApplication.TournoiFichier tf = source.getValue();
            oui.setOnAction(x -> annoncer(b, serviceStock.reporter(DepotBuvette.charger(tf.fichier()),
                    tf.tournoi().getNom(), b, cat, auteur())));
            non.setOnAction(x -> confirmation.getChildren().clear());
            confirmation.getChildren().setAll(Ui.encartConfirmation("Ajouter au stock de « " + tournoi.getNom()
                    + " » tout ce qui restait à « " + tf.tournoi().getNom() + " » ? Faites d'abord son inventaire "
                    + "de fin. Le tournoi d'origine n'est pas modifié.", oui, non));
        });
        zone.getChildren().addAll(Ui.ligne(10, source, reporter), confirmation);
        return Ui.carte("Reprendre le stock d'un autre tournoi", zone);
    }

    private Node carteJournal(BuvetteTournoi b, PresentationStockBuvette p) {
        VBox liste = new VBox(4);
        List<PresentationStockBuvette.LigneMouvement> lignes = p.journal();
        if (lignes.isEmpty()) {
            liste.getChildren().add(Ui.etatVideCompact("Aucun mouvement pour ce tournoi."));
        }
        boolean premiere = true;
        for (PresentationStockBuvette.LigneMouvement l : lignes) {
            List<Node> droite = new ArrayList<>();
            if (l.corrige()) {
                droite.add(Ui.pastille("Corrigé", StatutPastille.NEUTRE));
            }
            if (l.corrigeable()) {
                Button corriger = Ui.boutonLien("Corriger");
                corriger.setOnAction(e -> annoncer(b, serviceStock.corriger(b, l.numero(), auteur(), null)));
                droite.add(corriger);
            }
            HBox ligne = Ui.ligneElement(null, l.titre(), l.detail(), droite.toArray(new Node[0]));
            if (premiere) {
                ligne.getStyleClass().add("ligne-element-premiere");
                premiere = false;
            }
            liste.getChildren().add(ligne);
        }
        return Ui.carte(Ui.titreAvecCompteur("Journal des mouvements", lignes.size()),
                Ui.aide("« Corriger » ajoute le mouvement inverse : rien ne s'efface."), liste);
    }

    // ==================================================================
    // Onglet Budget
    // ==================================================================

    private void annoncer(BuvetteTournoi b, ResultatBudgetBuvette r) {
        if (r.accepte()) {
            enregistrerTournoi(b, null, r.explication());
        } else {
            Ui.erreur(message, r.explication());
        }
    }

    private Node ongletBudget(Tournoi tournoi) {
        CatalogueBuvette cat = catalogue();
        BuvetteTournoi b = buvette();
        BudgetCalcule calcul = new CalculBudgetBuvette().calculer(tournoi, b, cat, suggestions.hypotheses());
        PresentationBudgetBuvette p = new PresentationBudgetBuvette(calcul, tournoi.getNom());
        VBox v = new VBox(20);
        v.getChildren().add(tuiles(p.tuiles().stream().map(t -> Ui.tuileChiffre(t.valeur(), t.libelle())).toList()));

        // Affluence : joueurs par jour.
        VBox jours = new VBox(6, Ui.texte(p.affluence()));
        for (PresentationBudgetBuvette.LigneJour j : p.jours()) {
            TextField n = Ui.champ("Joueurs");
            n.setText(j.joueurs());
            n.setPrefColumnCount(5);
            Button ok = Ui.boutonLien("Enregistrer");
            ok.setOnAction(e -> annoncer(b, serviceBudget.saisirJoueurs(b, j.jour(), n.getText())));
            jours.getChildren().add(Ui.ligneListe(Ui.gras(j.libelle()), Ui.espace(), n, Ui.discret(j.origine()), ok));
        }
        jours.getChildren().add(Ui.aide("Laissez vide pour revenir à l'estimation d'après les inscriptions."));
        v.getChildren().add(Ui.carte("Affluence prévue", jours));

        // Hypothèses générales.
        TextField acc = Ui.champ("30");
        acc.setText(p.accompagnants());
        TextField pres = Ui.champ("1,5");
        pres.setText(p.presence());
        TextField bas = Ui.champ("75");
        bas.setText(p.scenarioBas());
        TextField haut = Ui.champ("125");
        haut.setText(p.scenarioHaut());
        Button enregistrer = Ui.boutonPrimaire("Enregistrer les hypothèses");
        enregistrer.setOnAction(e -> annoncer(b, serviceBudget.saisirHypotheses(b, acc.getText(), pres.getText(),
                bas.getText(), haut.getText())));
        Button revenir = Ui.boutonSecondaire("Revenir aux suggestions");
        revenir.setOnAction(e -> annoncer(b, serviceBudget.revenirAuxSuggestions(b)));
        javafx.scene.control.CheckBox stock = new javafx.scene.control.CheckBox(
                "Déduire le stock disponible de la liste de courses");
        stock.setSelected(b.getBudget().deduireStock());
        stock.setOnAction(e -> annoncer(b, serviceBudget.deduireStock(b, stock.isSelected())));
        v.getChildren().add(Ui.carte(Ui.ligne(10, Ui.titreCarte("Hypothèses"),
                        Ui.pastille(SuggestionsBuvette.MENTION_HYPOTHESE, StatutPastille.INFO)),
                Ui.aide(p.mentionHypotheses()),
                new FlowPane(12, 12, Ui.champEtiquete("Accompagnants pour 100 joueurs (%)", acc),
                        Ui.champEtiquete("Présence moyenne (demi-journées)", pres),
                        Ui.champEtiquete("Scénario bas (%)", bas), Ui.champEtiquete("Scénario haut (%)", haut)),
                stock, Ui.actions(enregistrer, revenir)));

        // Scénarios.
        VBox sc = new VBox(4);
        for (PresentationBudgetBuvette.LigneScenario l : p.scenarios()) {
            sc.getChildren().add(Ui.ligneListe(Ui.gras(l.nom()), Ui.espace(), Ui.discret(l.detail())));
        }
        ToggleGroup groupe = new ToggleGroup();
        List<ToggleButton> boutons = new ArrayList<>();
        for (String nom : List.of("Bas", "Moyen", "Haut")) {
            ToggleButton t = new ToggleButton(nom);
            t.setSelected(nom.equals(scenarioCourant));
            t.setOnAction(e -> {
                scenarioCourant = nom;
                afficher();
            });
            boutons.add(t);
        }
        sc.getChildren().addAll(Ui.texte(p.phraseSeuil(scenarioCourant)),
                Ui.ligne(10, Ui.libelleChamp("Détail du scénario"),
                        Ui.segment(groupe, boutons.toArray(new ToggleButton[0]))));
        v.getChildren().add(Ui.carte("Scénarios", sc));

        if (p.sansProduit()) {
            v.getChildren().add(Ui.carteNue(Ui.etatVide(p.messageSansProduit(), null)));
            return v;
        }
        // Produits prévus, avec la consommation modifiable.
        VBox produits = new VBox(4);
        boolean premiere = true;
        for (PresentationBudgetBuvette.Ligne l : p.produits(scenarioCourant)) {
            String id = l.id();
            TextField conso = Ui.champ("0,35");
            conso.setText(p.consommation(id));
            conso.setPrefColumnCount(4);
            Button ok = Ui.boutonLien("Modifier");
            ok.setOnAction(e -> annoncer(b, serviceBudget.saisirConsommation(b, suggestions.hypotheses(), cat, id,
                    conso.getText())));
            List<Node> droite = new ArrayList<>();
            if (!l.pastille().isEmpty()) {
                droite.add(Ui.pastille(l.pastille(), StatutPastille.NEUTRE));
            }
            droite.add(Ui.discret("par personne"));
            droite.add(conso);
            droite.add(ok);
            HBox ligne = Ui.ligneElement(null, l.titre(), l.detail(), droite.toArray(new Node[0]));
            if (premiere) {
                ligne.getStyleClass().add("ligne-element-premiere");
                premiere = false;
            }
            produits.getChildren().add(ligne);
        }
        if (!b.getBudget().origineQuantites().isEmpty()) {
            Button oublier = Ui.boutonLien("Revenir au calcul");
            oublier.setOnAction(e -> annoncer(b, serviceBudget.oublierQuantitesReprises(b)));
            produits.getChildren().add(0, Ui.ligne(10, Ui.encartInfo("Quantités reprises de « "
                    + b.getBudget().origineQuantites() + " »."), oublier));
        }
        v.getChildren().add(Ui.carte("Produits prévus · scénario " + scenarioCourant.toLowerCase(),
                Ui.aide("Consommation par personne et par demi-journée de présence (0,35 = 35 pour 100 personnes)."),
                produits));

        // Liste de courses.
        VBox courses = new VBox(4);
        premiere = true;
        for (PresentationBudgetBuvette.Ligne l : p.courses(scenarioCourant)) {
            HBox ligne = l.pastille().isEmpty() ? Ui.ligneElement(null, l.titre(), l.detail())
                    : Ui.ligneElement(null, l.titre(), l.detail(), Ui.pastille(l.pastille(), StatutPastille.SUCCES));
            if (premiere) {
                ligne.getStyleClass().add("ligne-element-premiere");
                premiere = false;
            }
            courses.getChildren().add(ligne);
        }
        Button csv = Ui.boutonSecondaire("Exporter en CSV…");
        csv.setOnAction(e -> exporter(p.nomFichier(scenarioCourant, "csv"), "Fichier CSV", "*.csv",
                () -> p.csvCourses(scenarioCourant).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        Button pdf = Ui.boutonSecondaire("Exporter en PDF…");
        pdf.setOnAction(e -> exporter(p.nomFichier(scenarioCourant, "pdf"), "Document PDF", "*.pdf",
                () -> com.pingmanager.ui.infra.RenduPdfBuvette.tableau("Liste de courses de la buvette",
                        tournoi.getNom() + " · scénario " + scenarioCourant.toLowerCase(),
                        List.of("Ingrédient", "À acheter", "Besoin", "Coût estimé"),
                        p.lignesPdfCourses(scenarioCourant), new float[] {4, 4, 2, 2},
                        List.of(SuggestionsBuvette.MENTION + ".", "Quantités issues d'hypothèses à ajuster."))));
        v.getChildren().add(Ui.carte(Ui.enteteAvecActions(Ui.titreCarte("Liste de courses · scénario "
                + scenarioCourant.toLowerCase()), csv, pdf), courses));
        return v;
    }

    // ==================================================================
    // Onglet Ventes : la caisse
    // ==================================================================

    private Node ongletVentes(Tournoi tournoi) {
        CatalogueBuvette cat = catalogue();
        BuvetteTournoi b = buvette();
        PresentationVentesBuvette p = new PresentationVentesBuvette(b, cat, LocalDate.now(), ZoneId.systemDefault());
        VBox v = new VBox(20);
        v.getChildren().add(tuiles(p.tuiles().stream().map(t -> Ui.tuileChiffre(t.valeur(), t.libelle())).toList()));
        if (p.sansProduit()) {
            v.getChildren().add(Ui.carteNue(Ui.etatVide(p.messageSansProduit(), null)));
            return v;
        }
        VBox caisse = new VBox(12);
        for (PresentationVentesBuvette.Groupe g : p.groupes()) {
            FlowPane boutons = new FlowPane(12, 12);
            for (PresentationVentesBuvette.BoutonProduit bp : g.boutons()) {
                Button vendre = new Button();
                Label nom = Ui.label(bp.nom(), "buvette-caisse-nom");
                nom.setWrapText(true);
                VBox contenuBouton = new VBox(4, nom, Ui.label(bp.prix(), "buvette-caisse-prix"),
                        Ui.label(bp.compteur(), "buvette-caisse-compteur"));
                vendre.setGraphic(contenuBouton);
                vendre.getStyleClass().add("buvette-caisse-bouton");
                vendre.setOnAction(e -> encaisser(b, p, serviceVentes.vendre(b, cat, bp.id(), auteur(),
                        ServiceVentesBuvette.POSTE_PAR_DEFAUT)));
                Button moins = Ui.boutonLien("−");
                moins.getStyleClass().add("buvette-caisse-moins");
                moins.setOnAction(e -> encaisser(b, p, serviceVentes.annulerDerniere(b, cat, bp.id(), auteur(),
                        ServiceVentesBuvette.POSTE_PAR_DEFAUT)));
                VBox tuile = new VBox(4, vendre, moins);
                tuile.setAlignment(Pos.CENTER);
                boutons.getChildren().add(tuile);
            }
            caisse.getChildren().addAll(Ui.label(g.titre(), "buvette-categorie"), boutons);
        }
        Button annuler = Ui.boutonSecondaire("Annuler la dernière vente");
        annuler.setOnAction(e -> encaisser(b, p, serviceVentes.annulerDerniere(b, cat, null, auteur(),
                ServiceVentesBuvette.POSTE_PAR_DEFAUT)));
        v.getChildren().add(Ui.carte(Ui.enteteAvecActions(Ui.titreCarte("Caisse"), annuler),
                Ui.aide("Un clic = une vente ; « − » annule la dernière vente de ce produit. Chaque vente déduit "
                        + "le stock selon la recette. Une seule caisse : « " + ServiceVentesBuvette.POSTE_PAR_DEFAUT
                        + " »."), caisse));
        VBox liste = new VBox(4);
        boolean premiere = true;
        for (PresentationVentesBuvette.LigneVente l : p.dernieres()) {
            HBox ligne = l.annulee() ? Ui.ligneElement(null, l.titre(), l.detail(),
                    Ui.pastille("Annulée", StatutPastille.NEUTRE)) : Ui.ligneElement(null, l.titre(), l.detail());
            if (premiere) {
                ligne.getStyleClass().add("ligne-element-premiere");
                premiere = false;
            }
            liste.getChildren().add(ligne);
        }
        if (liste.getChildren().isEmpty()) {
            liste.getChildren().add(Ui.etatVideCompact("Aucune vente pour l'instant."));
        }
        v.getChildren().add(Ui.carte("Dernières ventes", liste));
        return v;
    }

    /** Enregistre aussitôt la vente (ou l'annulation) et affiche le total du jour. */
    private void encaisser(BuvetteTournoi b, PresentationVentesBuvette p, ResultatVenteBuvette r) {
        if (!r.accepte()) {
            Ui.erreur(message, p.message(r));
            return;
        }
        String texte = p.message(r);
        if (r.stockEpuise().isEmpty()) {
            enregistrerTournoi(b, null, texte);
        } else {
            enregistrerTournoi(b, texte, null);
        }
    }

    // ==================================================================
    // Onglet Historique
    // ==================================================================

    private Node ongletHistorique() {
        CatalogueBuvette cat = catalogue();
        List<ContexteApplication.TournoiFichier> tournois = tournoisDuClub();
        List<BilanBuvette> bilans = new ArrayList<>();
        CalculBilanBuvette calcul = new CalculBilanBuvette();
        for (ContexteApplication.TournoiFichier tf : tournois) {
            BuvetteTournoi b;
            try {
                b = DepotBuvette.charger(tf.fichier());
            } catch (RuntimeException illisible) {
                b = new BuvetteTournoi();
            }
            bilans.add(calcul.bilan(tf.tournoi(), b, cat, suggestions.hypotheses(), ZoneId.systemDefault()));
        }
        PresentationHistoriqueBuvette p = new PresentationHistoriqueBuvette(bilans, saisonHistorique);
        VBox v = new VBox(20);
        ComboBox<String> saison = new ComboBox<>(FXCollections.observableArrayList(p.saisons()));
        saison.getSelectionModel().select(saisonHistorique);
        saison.valueProperty().addListener((o, a, n) -> {
            if (n != null) {
                saisonHistorique = n;
                bilanOuvert = -1;
                afficher();
            }
        });
        Button csv = Ui.boutonSecondaire("Exporter le bilan en CSV…");
        csv.setOnAction(e -> exporter(p.nomFichier(), "Fichier CSV", "*.csv",
                () -> p.csv().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        v.getChildren().add(Ui.ligne(12, Ui.libelleChamp("Saison"), saison, Ui.espace(), csv));
        v.getChildren().add(tuiles(p.tuiles().stream().map(t -> Ui.tuileChiffre(t.valeur(), t.libelle())).toList()));

        VBox liste = new VBox(4);
        boolean premiere = true;
        for (PresentationHistoriqueBuvette.LigneTournoi l : p.lignes()) {
            Label resultat = Ui.label(l.resultat(), l.positif() ? "buvette-resultat-positif" : "buvette-resultat-negatif");
            Button voir = Ui.boutonLien(l.index() == bilanOuvert ? "Fermer" : "Voir le bilan");
            voir.setOnAction(e -> {
                bilanOuvert = l.index() == bilanOuvert ? -1 : l.index();
                afficher();
            });
            HBox ligne = Ui.ligneElement(null, l.titre(), l.detail(), resultat, voir);
            if (premiere) {
                ligne.getStyleClass().add("ligne-element-premiere");
                premiere = false;
            }
            liste.getChildren().add(ligne);
        }
        if (liste.getChildren().isEmpty()) {
            liste.getChildren().add(Ui.etatVideCompact(p.messageVide()));
        }
        v.getChildren().add(Ui.carte("Tournois du club", liste));
        if (bilanOuvert >= 0 && bilanOuvert < bilans.size()) {
            v.getChildren().add(carteBilan(p, bilans.get(bilanOuvert), tournois.get(bilanOuvert), tournois));
        }
        return v;
    }

    private Node carteBilan(PresentationHistoriqueBuvette p, BilanBuvette b, ContexteApplication.TournoiFichier source,
                            List<ContexteApplication.TournoiFichier> tournois) {
        javafx.scene.layout.GridPane grille = new javafx.scene.layout.GridPane();
        grille.setHgap(24);
        grille.setVgap(6);
        grille.add(Ui.enteteColonne(""), 0, 0);
        grille.add(Ui.enteteColonne("Prévu"), 1, 0);
        grille.add(Ui.enteteColonne("Réel"), 2, 0);
        int r = 1;
        for (PresentationHistoriqueBuvette.LigneComparaison l : p.comparaison(b)) {
            grille.add(Ui.gras(l.libelle()), 0, r);
            grille.add(Ui.texte(l.prevu()), 1, r);
            grille.add(Ui.texte(l.reel()), 2, r);
            r++;
        }
        // Reprendre les quantités vendues dans le budget d'un autre tournoi.
        List<ContexteApplication.TournoiFichier> cibles = new ArrayList<>();
        for (ContexteApplication.TournoiFichier tf : tournois) {
            if (!tf.fichier().equals(source.fichier())) {
                cibles.add(tf);
            }
        }
        VBox reprise = new VBox(10);
        if (!cibles.isEmpty() && !b.ventesParProduit().isEmpty()) {
            ComboBox<ContexteApplication.TournoiFichier> cible = new ComboBox<>(FXCollections.observableArrayList(cibles));
            cible.setConverter(new StringConverter<>() {
                @Override
                public String toString(ContexteApplication.TournoiFichier tf) {
                    return tf == null ? "" : tf.tournoi().getNom();
                }

                @Override
                public ContexteApplication.TournoiFichier fromString(String s) {
                    return null;
                }
            });
            cible.setPromptText("Tournoi à préparer…");
            Button reprendre = Ui.boutonSecondaire("Reprendre les quantités de ce tournoi…");
            VBox confirmation = new VBox();
            reprendre.setOnAction(e -> {
                ContexteApplication.TournoiFichier tf = cible.getValue();
                if (tf == null) {
                    Ui.erreur(message, "Choisissez le tournoi dont le budget sera prérempli.");
                    return;
                }
                Button oui = Ui.boutonPrimaire("Préremplir");
                Button non = Ui.boutonSecondaire("Renoncer");
                oui.setOnAction(x -> {
                    BuvetteTournoi destination = DepotBuvette.charger(tf.fichier());
                    ResultatBudgetBuvette res = serviceBudget.reprendreQuantites(destination, b);
                    if (!res.accepte()) {
                        Ui.erreur(message, res.explication());
                        return;
                    }
                    Path courant = fichierTournoi;
                    fichierTournoi = tf.fichier();
                    enregistrerTournoi(destination, null, res.explication() + " (« " + tf.tournoi().getNom()
                            + " », onglet Budget).");
                    fichierTournoi = courant;
                });
                non.setOnAction(x -> confirmation.getChildren().clear());
                confirmation.getChildren().setAll(Ui.encartConfirmation("Le budget de « " + tf.tournoi().getNom()
                        + " » partira des quantités vendues à « " + b.nomTournoi() + " » (au lieu du calcul par "
                        + "personne). Vous pourrez revenir au calcul depuis l'onglet Budget.", oui, non));
            });
            reprise.getChildren().addAll(Ui.ligne(10, cible, reprendre), confirmation);
        }
        return Ui.carte("Bilan : " + b.nomTournoi(), grille, Ui.texte(p.plusVendus(b)), Ui.texte(p.phraseResultat(b)),
                Ui.aide("Prévu = scénario moyen du budget. Les finances du tournoi (inscriptions) ne sont pas "
                        + "modifiées par la buvette."), reprise);
    }

    /** Contenu d'un fichier à exporter (peut échouer). */
    @FunctionalInterface
    private interface Contenu {
        byte[] produire() throws java.io.IOException;
    }

    /** Demande où enregistrer puis écrit le fichier exporté. */
    private void exporter(String nomPropose, String description, String extension, Contenu contenu) {
        FileChooser fc = new FileChooser();
        fc.setInitialFileName(nomPropose);
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter(description, extension));
        java.io.File f = fc.showSaveDialog(racine.getScene() == null ? null : racine.getScene().getWindow());
        if (f == null) {
            return;
        }
        try {
            java.nio.file.Files.write(f.toPath(), contenu.produire());
            Ui.succes(message, "Fichier enregistré : " + f.getName());
        } catch (java.io.IOException | RuntimeException echec) {
            Ui.erreur(message, "Export impossible : " + echec.getMessage());
        }
    }

    // ==================================================================
    // Outils communs aux onglets
    // ==================================================================

    static HBox tuiles(List<? extends Node> tuiles) {
        HBox h = Ui.rangeeTuiles(tuiles.toArray(new Node[0]));
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }
}
