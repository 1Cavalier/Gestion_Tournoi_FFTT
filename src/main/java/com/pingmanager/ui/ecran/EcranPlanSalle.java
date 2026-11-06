package com.pingmanager.ui.ecran;

import com.pingmanager.core.EditionPlanSalles;
import com.pingmanager.core.PlansTypesSalle;
import com.pingmanager.core.ResultatEditionSalles;
import com.pingmanager.core.ServicePlanSalle;
import com.pingmanager.model.ElementPlan;
import com.pingmanager.model.ElementPlan.ReperePlan;
import com.pingmanager.model.ElementPlan.TablePlan;
import com.pingmanager.model.PlanSalle;
import com.pingmanager.model.ResultatPlacementPlan;
import com.pingmanager.model.SalleTournoi;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.TypeRepere;
import com.pingmanager.ui.ContexteApplication;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Éditeur du plan de salle (B024), ouvert depuis la deuxième version de l'écran
 * du tournoi : plusieurs salles (mini-carte par étage), grille où l'on pose des
 * tables (2 × 2 cases), des allées (1 × 1) et des repères ; déplacer (glisser ou
 * flèches), numéroter, tourner, réserver ; plans types ; taille de la grille.
 *
 * <p>Le travail se fait sur un brouillon ({@link EditionPlanSalles}) : rien
 * n'est écrit avant « Enregistrer le plan ». Mots et disposition viennent de
 * {@link PresentationPlanSalle} (testée sans JavaFX).</p>
 */
final class EcranPlanSalle {

    /** Côté d'une case de la grille, en pixels. */
    private static final double CASE = 30;
    private static final String PREFIXE_GLISSER = "plan-salle:";

    private final ContexteApplication contexte;
    private final Path fichier;
    private final Runnable onRetour;
    private final Tournoi tournoi;
    private final EditionPlanSalles edition;
    private final ServicePlanSalle service = new ServicePlanSalle();

    private final BorderPane racine = new BorderPane();
    private final Label message = Ui.message();
    private final ComboBox<TypeRepere> typeRepere = new ComboBox<>();

    /** Case en haut à gauche de l'élément sélectionné, ou null. */
    private int[] selection;
    private boolean modifie;
    /** Outil choisi : selection, table, allee, repere ou gomme. */
    private String outilCourant = "selection";
    private boolean confirmerPlanType;
    private boolean confirmerRetraitSalle;

    EcranPlanSalle(ContexteApplication contexte, Path fichier, Runnable onRetour) {
        this.contexte = contexte;
        this.fichier = fichier;
        this.onRetour = onRetour;
        this.tournoi = contexte.chargerTournoi(fichier);
        this.edition = new EditionPlanSalles(tournoi);
        typeRepere.getItems().addAll(TypeRepere.values());
        typeRepere.setValue(TypeRepere.TABLE_ARBITRAGE);
        typeRepere.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(TypeRepere t) {
                return t == null ? "" : t.getLibelle();
            }

            @Override
            public TypeRepere fromString(String s) {
                return null;
            }
        });
    }

    Node build() {
        afficher();
        return racine;
    }

    private void afficher() {
        Button retour = Ui.boutonLien("← Retour au tournoi");
        retour.setOnAction(e -> onRetour.run());
        Label titre = Ui.titre(PresentationPlanSalle.TITRE_CARTE);
        Label sousTitre = Ui.discret(tournoi.getNom() == null ? "" : tournoi.getNom());
        VBox contenu = Ui.page(1180, new VBox(6, retour, titre, sousTitre), carteSalles(), cartePlan(),
                carteModeles(), carteReservations());
        List<String> alertes = alertes();
        if (!alertes.isEmpty()) {
            contenu.getChildren().add(Ui.encartAlerte("À vérifier", String.join("\n", alertes)));
        }
        Button enregistrer = Ui.boutonPrimaire("Enregistrer le plan");
        enregistrer.setOnAction(e -> enregistrer());
        Button annuler = Ui.boutonLien(modifie ? "Abandonner les modifications" : "Fermer");
        annuler.setOnAction(e -> onRetour.run());
        contenu.getChildren().add(Ui.actions(enregistrer, annuler));
        racine.setCenter(Ui.defilant(contenu));
    }

    private List<String> alertes() {
        return PresentationPlanSalle.alertes(edition.bilan());
    }

    private void enregistrer() {
        edition.appliquer(tournoi);
        contexte.enregistrerTournoi(tournoi, fichier);
        modifie = false;
        afficher();
        Ui.succes(message, "Plan enregistré : il sert désormais à la salle du jour J.");
    }

    // ------------------------------------------------------------------
    // Salles et mini-carte
    // ------------------------------------------------------------------

    private Node carteSalles() {
        HBox entete = Ui.ligne(10, Ui.iconeTuile(Icones.SALLE, false), Ui.titreCarte("Salles"));
        entete.setAlignment(Pos.CENTER_LEFT);
        VBox carte = Ui.carte(entete);
        if (PresentationPlanSalle.afficheMiniCarte(edition.getSalles())) {
            carte.getChildren().add(miniCarte());
        }
        SalleTournoi s = edition.salleCourante();
        TextField nom = Ui.champ("Nom de la salle");
        nom.setText(s.getNom());
        Spinner<Integer> etage = new Spinner<>(-3, 30, s.getEtage());
        etage.setEditable(true);
        etage.setPrefWidth(90);
        Button appliquer = Ui.boutonSecondaire("Renommer / changer d'étage");
        appliquer.setOnAction(e -> {
            ResultatEditionSalles r = edition.renommerSalle(nom.getText());
            if (r.estReussi()) {
                r = edition.changerEtage(etage.getValue());
            }
            apresSalle(r);
        });
        HBox ligneSalle = Ui.ligne(10, Ui.champEtiquete("Salle affichée", nom),
                Ui.champEtiquete("Étage (0 = rez-de-chaussée)", etage), appliquer);
        ligneSalle.setAlignment(Pos.BOTTOM_LEFT);

        TextField nouvelle = Ui.champ("Par exemple : Dojo");
        Button ajouter = Ui.boutonSecondaire("Ajouter une salle");
        ajouter.setOnAction(e -> apresSalle(edition.ajouterSalle(nouvelle.getText(), etage.getValue())));
        HBox ligneAjout = Ui.ligne(10, Ui.champEtiquete("Nouvelle salle", nouvelle), ajouter);
        ligneAjout.setAlignment(Pos.BOTTOM_LEFT);
        carte.getChildren().addAll(ligneSalle, ligneAjout);

        if (edition.getSalles().size() > 1) {
            if (confirmerRetraitSalle) {
                Button oui = Ui.boutonSecondaire("Oui, retirer « " + s.getNom() + " »");
                oui.getStyleClass().add("jourj-danger");
                Button non = Ui.boutonLien("Non");
                oui.setOnAction(e -> {
                    confirmerRetraitSalle = false;
                    apresSalle(edition.retirerSalle());
                });
                non.setOnAction(e -> {
                    confirmerRetraitSalle = false;
                    afficher();
                });
                carte.getChildren().add(Ui.encartConfirmation("La salle « " + s.getNom() + " » et ses "
                        + PresentationPlanSalle.nombreDeTables(s.getPlan().getTables().size())
                        + " seront retirées du plan.", oui, non));
            } else {
                Button retirer = Ui.boutonLien("Retirer cette salle");
                retirer.getStyleClass().add("lien-danger");
                retirer.setOnAction(e -> {
                    confirmerRetraitSalle = true;
                    afficher();
                });
                carte.getChildren().add(retirer);
            }
        }
        return carte;
    }

    /** Mini-carte : un bloc par étage, une tuile cliquable par salle. */
    private Node miniCarte() {
        VBox etages = new VBox(8);
        etages.getStyleClass().add("plan-mini-carte");
        for (PresentationPlanSalle.EtageMiniCarte etage : PresentationPlanSalle.miniCarte(edition.getSalles(),
                edition.getIndexCourant())) {
            FlowPane salles = new FlowPane(8, 8);
            for (PresentationPlanSalle.SalleMiniCarte s : etage.salles()) {
                VBox tuile = new VBox(2, Ui.gras(s.nom()), Ui.discret(s.detail()));
                tuile.getStyleClass().add("plan-mini-salle");
                if (s.courante()) {
                    tuile.getStyleClass().add("plan-mini-salle-active");
                }
                tuile.setOnMouseClicked(e -> {
                    edition.choisirSalle(s.index());
                    selection = null;
                    confirmerRetraitSalle = false;
                    afficher();
                });
                salles.getChildren().add(tuile);
            }
            Label libelle = Ui.discret(etage.libelle());
            libelle.getStyleClass().add("plan-mini-etage");
            etages.getChildren().add(new VBox(4, libelle, salles));
        }
        return etages;
    }

    private void apresSalle(ResultatEditionSalles r) {
        if (r.estReussi()) {
            modifie = true;
            selection = null;
        }
        afficher();
        if (r.estReussi()) {
            Ui.succes(message, PresentationPlanSalle.message(r));
        } else {
            Ui.erreur(message, PresentationPlanSalle.message(r));
        }
    }

    // ------------------------------------------------------------------
    // Plan
    // ------------------------------------------------------------------

    private Node cartePlan() {
        PlanSalle plan = edition.planCourant();
        ToggleButton deplacer = outil("Sélectionner", "selection");
        ToggleButton table = outil("Table", "table");
        ToggleButton allee = outil("Allée", "allee");
        ToggleButton repere = outil("Repère", "repere");
        ToggleButton gomme = outil("Gomme", "gomme");
        ToggleGroup outils = new ToggleGroup();
        HBox barre = Ui.ligne(10, Ui.segment(outils, deplacer, table, allee, repere, gomme), typeRepere);
        barre.setAlignment(Pos.CENTER_LEFT);
        Label aide = Ui.aide("Choisissez un outil puis cliquez sur une case. Une table occupe 2 × 2 cases ; "
                + "glissez un élément pour le déplacer. Prochaine table : n° " + edition.premierNumeroLibre() + ".");
        aide.setWrapText(true);

        HBox zone = new HBox(16, grille(plan), panneauSelection(plan));
        return Ui.carte(Ui.titreCarte(edition.salleCourante().getNom() + " · " + plan.getColonnes() + " × "
                + plan.getLignes() + " cases"), barre, aide, message, zone);
    }

    private ToggleButton outil(String texte, String code) {
        ToggleButton b = new ToggleButton(texte);
        b.setUserData(code);
        b.setSelected(code.equals(outilCourant));
        b.selectedProperty().addListener((obs, avant, apres) -> {
            if (apres) {
                outilCourant = code;
            }
        });
        return b;
    }

    private String outilChoisi() {
        return outilCourant;
    }

    private Node grille(PlanSalle plan) {
        GridPane g = new GridPane();
        g.getStyleClass().add("plan-grille");
        for (int c = 0; c < plan.getColonnes(); c++) {
            g.getColumnConstraints().add(new javafx.scene.layout.ColumnConstraints(CASE, CASE, CASE));
        }
        for (int l = 0; l < plan.getLignes(); l++) {
            g.getRowConstraints().add(new javafx.scene.layout.RowConstraints(CASE, CASE, CASE));
        }
        for (int c = 0; c < plan.getColonnes(); c++) {
            for (int l = 0; l < plan.getLignes(); l++) {
                Region caseVide = new Region();
                caseVide.getStyleClass().add("plan-case");
                int cc = c;
                int ll = l;
                caseVide.setOnMouseClicked(e -> cliquer(cc, ll));
                accepterDepot(caseVide, cc, ll, 0, 0);
                g.add(caseVide, c, l);
            }
        }
        for (PresentationPlanSalle.Tuile t : PresentationPlanSalle.tuiles(plan)) {
            Label texte = new Label(t.texte());
            texte.getStyleClass().add("plan-element-texte");
            StackPane noeud = new StackPane(texte);
            noeud.getStyleClass().addAll(t.classes());
            if (selection != null && t.element().couvre(selection[0], selection[1])) {
                noeud.getStyleClass().add("plan-element-selectionne");
            }
            Tooltip.install(noeud, new Tooltip(t.infobulle()));
            noeud.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            ElementPlan e = t.element();
            noeud.setOnMouseClicked(ev -> cliquer(e.colonne() + (int) (ev.getX() / CASE),
                    e.ligne() + (int) (ev.getY() / CASE)));
            noeud.setOnDragDetected(ev -> {
                Dragboard db = noeud.startDragAndDrop(TransferMode.MOVE);
                ClipboardContent cc = new ClipboardContent();
                cc.putString(PREFIXE_GLISSER + e.colonne() + "," + e.ligne());
                db.setContent(cc);
                ev.consume();
            });
            accepterDepot(noeud, e.colonne(), e.ligne(), 1, 1);
            g.add(noeud, t.colonne(), t.ligne(), t.largeur(), t.hauteur());
        }
        return g;
    }

    /**
     * Accepte le dépôt d'un élément glissé. Sur une case vide, la case visée est
     * celle-ci ; sur un élément, elle se calcule depuis la position de la souris.
     */
    private void accepterDepot(Node cible, int colonne, int ligne, int surElementX, int surElementY) {
        cible.setOnDragOver(ev -> {
            if (contenuGlisse(ev) != null) {
                ev.acceptTransferModes(TransferMode.MOVE);
            }
            ev.consume();
        });
        cible.setOnDragDropped(ev -> {
            int[] source = contenuGlisse(ev);
            boolean fait = false;
            if (source != null) {
                int c = colonne + surElementX * (int) (ev.getX() / CASE);
                int l = ligne + surElementY * (int) (ev.getY() / CASE);
                ResultatPlacementPlan r = edition.deplacer(source[0], source[1], c, l);
                fait = r.estReussi();
                if (fait) {
                    selection = new int[] {c, l};
                }
                apresPlan(r);
            }
            ev.setDropCompleted(fait);
            ev.consume();
        });
    }

    private static int[] contenuGlisse(DragEvent ev) {
        Dragboard db = ev.getDragboard();
        if (!db.hasString() || !db.getString().startsWith(PREFIXE_GLISSER)) {
            return null;
        }
        String[] p = db.getString().substring(PREFIXE_GLISSER.length()).split(",");
        try {
            return new int[] {Integer.parseInt(p[0]), Integer.parseInt(p[1])};
        } catch (RuntimeException e) {
            return null;
        }
    }

    private void cliquer(int colonne, int ligne) {
        String outil = outilChoisi();
        if ("table".equals(outil)) {
            ResultatPlacementPlan r = edition.ajouterTable(colonne, ligne);
            if (r.estReussi()) {
                selection = new int[] {colonne, ligne};
            }
            apresPlan(r);
        } else if ("allee".equals(outil)) {
            apresPlan(edition.ajouterAllee(colonne, ligne));
        } else if ("repere".equals(outil)) {
            TypeRepere type = typeRepere.getValue() == null ? TypeRepere.AUTRE : typeRepere.getValue();
            apresPlan(edition.ajouterRepere(type, colonne, ligne));
        } else if ("gomme".equals(outil)) {
            selection = null;
            apresPlan(edition.retirer(colonne, ligne));
        } else {
            Optional<ElementPlan> e = edition.planCourant().elementEn(colonne, ligne);
            selection = e.map(x -> new int[] {x.colonne(), x.ligne()}).orElse(null);
            afficher();
        }
    }

    private void apresPlan(ResultatPlacementPlan r) {
        if (r.estReussi()) {
            modifie = true;
        }
        afficher();
        if (r.estReussi()) {
            Ui.succes(message, PresentationPlanSalle.message(r));
        } else {
            Ui.erreur(message, PresentationPlanSalle.message(r));
        }
    }

    /** Panneau de l'élément sélectionné : numéro, orientation, réservation, flèches, retrait. */
    private Node panneauSelection(PlanSalle plan) {
        VBox panneau = new VBox(10);
        panneau.getStyleClass().add("plan-panneau");
        panneau.setMinWidth(240);
        Optional<ElementPlan> choisi = selection == null ? Optional.empty()
                : plan.elementEn(selection[0], selection[1]);
        if (choisi.isEmpty()) {
            panneau.getChildren().add(Ui.aide("Outil « Sélectionner » : cliquez sur un élément pour le modifier."));
            return panneau;
        }
        ElementPlan e = choisi.get();
        panneau.getChildren().add(Ui.gras(PresentationPlanSalle.infobulle(e)));
        if (e instanceof TablePlan t) {
            TextField numero = Ui.champ("Numéro");
            numero.setText(String.valueOf(t.numero()));
            numero.setPrefColumnCount(4);
            Button renumeroter = Ui.boutonSecondaire("Renuméroter");
            renumeroter.setOnAction(ev -> {
                int n;
                try {
                    n = Integer.parseInt(numero.getText().trim());
                } catch (NumberFormatException ex) {
                    n = 0;
                }
                apresPlan(edition.numeroter(t.numero(), n));
            });
            HBox ligneNumero = Ui.ligne(8, numero, renumeroter);
            Button tourner = Ui.boutonSecondaire("Tourner (" + t.orientation().tournee().getLibelle().toLowerCase() + ")");
            tourner.setOnAction(ev -> apresPlan(edition.tourner(t.numero())));
            ComboBox<String> reservation = new ComboBox<>();
            reservation.getItems().add(SANS_RESERVATION);
            reservation.getItems().addAll(edition.getTableaux());
            reservation.setValue(t.tableauReserve() == null ? SANS_RESERVATION : t.tableauReserve());
            reservation.setOnAction(ev -> {
                String v = reservation.getValue();
                apresPlan(edition.reserverTable(t.numero(), SANS_RESERVATION.equals(v) ? null : v));
            });
            panneau.getChildren().addAll(ligneNumero, tourner, Ui.champEtiquete("Réservée au tableau", reservation));
        } else if (e instanceof ReperePlan r) {
            panneau.getChildren().add(Ui.discret(r.largeur() + " × " + r.hauteur() + " cases"));
        }
        panneau.getChildren().add(fleches(e));
        Button retirer = Ui.boutonLien("Retirer du plan");
        retirer.getStyleClass().add("lien-danger");
        retirer.setOnAction(ev -> {
            selection = null;
            apresPlan(edition.retirer(e.colonne(), e.ligne()));
        });
        panneau.getChildren().add(retirer);
        return panneau;
    }

    private static final String SANS_RESERVATION = "Aucune réservation";

    /** Flèches de déplacement d'une case (alternative au glisser-déposer, au clavier). */
    private Node fleches(ElementPlan e) {
        Button gauche = fleche("←", "Déplacer d'une case à gauche", e, -1, 0);
        Button haut = fleche("↑", "Déplacer d'une case vers le haut", e, 0, -1);
        Button bas = fleche("↓", "Déplacer d'une case vers le bas", e, 0, 1);
        Button droite = fleche("→", "Déplacer d'une case à droite", e, 1, 0);
        return Ui.ligne(6, gauche, haut, bas, droite);
    }

    private Button fleche(String texte, String aide, ElementPlan e, int dc, int dl) {
        Button b = Ui.boutonSecondaire(texte);
        b.setTooltip(new Tooltip(aide));
        b.setOnAction(ev -> {
            ResultatPlacementPlan r = edition.deplacer(e.colonne(), e.ligne(), e.colonne() + dc, e.ligne() + dl);
            if (r.estReussi()) {
                selection = new int[] {e.colonne() + dc, e.ligne() + dl};
            }
            apresPlan(r);
        });
        return b;
    }

    // ------------------------------------------------------------------
    // Plans types et taille de la grille
    // ------------------------------------------------------------------

    private Node carteModeles() {
        ComboBox<PlansTypesSalle.PlanType> type = new ComboBox<>();
        type.getItems().addAll(PlansTypesSalle.PlanType.values());
        type.setValue(PlansTypesSalle.PlanType.DEUX_RANGEES);
        type.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(PlansTypesSalle.PlanType t) {
                return t == null ? "" : t.getLibelle();
            }

            @Override
            public PlansTypesSalle.PlanType fromString(String s) {
                return null;
            }
        });
        Spinner<Integer> tables = new Spinner<>(0, 200, edition.tablesRestantesPourLaSalle());
        tables.setEditable(true);
        tables.setPrefWidth(90);
        Button appliquer = Ui.boutonSecondaire("Appliquer le plan type");
        Runnable faire = () -> {
            confirmerPlanType = false;
            selection = null;
            apresPlan(edition.appliquerPlanType(type.getValue(), tables.getValue()));
        };
        appliquer.setOnAction(e -> {
            if (edition.planCourant().getElements().isEmpty()) {
                faire.run();
            } else {
                confirmerPlanType = true;
                afficher();
            }
        });
        HBox ligneType = Ui.ligne(10, Ui.champEtiquete("Plan type", type), Ui.champEtiquete("Tables", tables),
                appliquer);
        ligneType.setAlignment(Pos.BOTTOM_LEFT);

        PlanSalle plan = edition.planCourant();
        Spinner<Integer> colonnes = new Spinner<>(1, PlanSalle.COTE_MAXIMAL, plan.getColonnes());
        Spinner<Integer> lignes = new Spinner<>(1, PlanSalle.COTE_MAXIMAL, plan.getLignes());
        colonnes.setEditable(true);
        lignes.setEditable(true);
        colonnes.setPrefWidth(90);
        lignes.setPrefWidth(90);
        Button taille = Ui.boutonSecondaire("Changer la taille");
        taille.setOnAction(e -> apresPlan(edition.redimensionner(colonnes.getValue(), lignes.getValue())));
        HBox ligneTaille = Ui.ligne(10, Ui.champEtiquete("Colonnes", colonnes), Ui.champEtiquete("Lignes", lignes),
                taille);
        ligneTaille.setAlignment(Pos.BOTTOM_LEFT);

        VBox carte = Ui.carte("Plans types et taille de la grille", ligneType);
        if (confirmerPlanType) {
            Button oui = Ui.boutonSecondaire("Oui, remplacer");
            Button non = Ui.boutonLien("Non");
            oui.setOnAction(e -> faire.run());
            non.setOnAction(e -> {
                confirmerPlanType = false;
                afficher();
            });
            carte.getChildren().add(Ui.encartConfirmation("Le plan de « " + edition.salleCourante().getNom()
                    + " » sera remplacé par le plan type (tables, allées et repères actuels retirés).", oui, non));
        }
        carte.getChildren().add(ligneTaille);
        return carte;
    }

    // ------------------------------------------------------------------
    // Réservations de la salle
    // ------------------------------------------------------------------

    private Node carteReservations() {
        SalleTournoi s = edition.salleCourante();
        VBox carte = Ui.carte("Réservations de « " + s.getNom() + " »");
        if (edition.getTableaux().isEmpty()) {
            carte.getChildren().add(Ui.aide("Aucun tableau dans le tournoi pour l'instant."));
            return carte;
        }
        Label aide = Ui.aide("Salle réservée : ses tables ne reçoivent que ces tableaux (une table peut avoir sa "
                + "propre réservation). Préférence : ces tableaux jouent ici quand une table est libre, sinon "
                + "ailleurs. Ce sont des choix du club ; le juge-arbitre garde la main.");
        aide.setWrapText(true);
        FlowPane reserves = cases(s.getTableauxReserves(), true);
        FlowPane preferes = cases(s.getTableauxPreferes(), false);
        carte.getChildren().addAll(aide, Ui.libelleChamp("Salle réservée aux tableaux"), reserves,
                Ui.libelleChamp("Tableaux joués de préférence ici"), preferes);
        return carte;
    }

    private FlowPane cases(Set<String> cochees, boolean reservation) {
        FlowPane f = new FlowPane(12, 6);
        for (String nom : edition.getTableaux()) {
            CheckBox c = new CheckBox(nom);
            c.setSelected(cochees.contains(nom));
            c.setOnAction(e -> {
                Set<String> noms = new LinkedHashSet<>(cochees);
                if (c.isSelected()) {
                    noms.add(nom);
                } else {
                    noms.remove(nom);
                }
                apresSalle(reservation ? edition.reserverSalle(noms) : edition.preferer(noms));
            });
            f.getChildren().add(c);
        }
        return f;
    }
}
