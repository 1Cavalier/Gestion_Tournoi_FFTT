package com.pingmanager.ui.ecran;

import com.pingmanager.core.DroitRefuseException;
import com.pingmanager.core.buvette.Centimes;
import com.pingmanager.core.buvette.ResultatIngredientBuvette;
import com.pingmanager.core.buvette.ResultatProduitBuvette;
import com.pingmanager.core.buvette.ServiceIngredientsBuvette;
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
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

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
    // Outils communs aux onglets
    // ==================================================================

    static HBox tuiles(List<? extends Node> tuiles) {
        HBox h = Ui.rangeeTuiles(tuiles.toArray(new Node[0]));
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }
}
