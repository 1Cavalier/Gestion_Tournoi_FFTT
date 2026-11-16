package com.pingmanager.ui.ecran;

import com.pingmanager.core.ListeLancements;
import com.pingmanager.core.TableauDeBordSalle.MatchSalle;
import com.pingmanager.model.Match;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Interface « Lancer un match » du jour J (B055), ouverte par la carte du même
 * nom : le juge-arbitre envoie un match sur une table en quelques secondes.
 *
 * <ul>
 *   <li><b>Matchs proposés</b> : la liste de l'ordonnanceur selon la stratégie
 *       en place, du plus prioritaire au moins prioritaire ; chaque ligne
 *       montre le tableau, la poule ou le tour, les deux joueurs, la table
 *       proposée et depuis combien de temps les joueurs attendent (couleur de
 *       priorité habituelle). « Lancer · table N » l'envoie en un clic ; la
 *       liste des tables permet d'en changer avant.</li>
 *   <li><b>Choix manuel</b> : recherche par joueur, filtre par tableau, puis
 *       une table libre.</li>
 * </ul>
 *
 * <p>Un match qui ne peut pas partir reste visible, désactivé, avec la raison
 * en clair (« Prénom NOM joue en table 4 », « Aucune table libre »). Cette
 * raison vient du cœur ({@link com.pingmanager.core.ServiceSalle#listeLancements}),
 * qui refait toutes les vérifications au lancement. L'interface reste ouverte
 * après un lancement : la liste se recalcule pour enchaîner.</p>
 *
 * <p>Mots et filtres : {@link PresentationLancementMatch}.</p>
 */
final class InterfaceLancementMatch {

    /** Gestes délégués à l'écran de la salle (annonce, feuille, enregistrement). */
    interface Actions {
        /** Envoie le match sur la table (le cœur vérifie de nouveau). */
        void lancer(Match match, int table);

        /** « Proposer » : remplit toutes les tables libres selon la stratégie. */
        void proposer();

        /** Ferme l'interface. */
        void fermer();

        /** Option « Feuille à l'envoi » (B016), mémorisée pour le tournoi. */
        void feuilleALEnvoi(boolean imprimer);

        /** Redessine l'interface (changement d'onglet). */
        void rafraichir();

        /** Ouvre les poules ou l'arbre d'un tableau. */
        void ouvrirTableau(String nom);
    }

    private final Actions actions;
    private boolean manuel;
    private String recherche = "";
    private String tableau = PresentationLancementMatch.TOUS_LES_TABLEAUX;
    /** Table choisie par le juge-arbitre pour un match, à la place de la table proposée. */
    private final Map<Match, Integer> choixTables = new IdentityHashMap<>();
    /** Libellés d'attente à rafraîchir, avec l'instant depuis lequel les joueurs attendent. */
    private final Map<Label, LocalDateTime> attentes = new IdentityHashMap<>();

    InterfaceLancementMatch(Actions actions) {
        this.actions = actions;
    }

    /** Remet l'interface dans son état d'ouverture (matchs proposés, sans recherche). */
    void reinitialiser() {
        manuel = false;
        recherche = "";
        tableau = PresentationLancementMatch.TOUS_LES_TABLEAUX;
        choixTables.clear();
    }

    /**
     * Construit l'interface pour la liste donnée.
     *
     * @param cleGlisser donnée glissée d'un match vers une table libre, ou null
     *                   s'il ne peut pas être glissé
     */
    VBox construire(ListeLancements liste, int nombreDeTables, boolean ferme, boolean feuilleALEnvoi,
                    Function<Match, String> cleGlisser) {
        attentes.clear();
        choixTables.keySet().removeIf(m -> liste.getProposes().stream().noneMatch(l -> l.match().match() == m));

        Button fermer = Ui.boutonLien("Fermer");
        fermer.setOnAction(e -> actions.fermer());
        Label resume = Ui.discret(PresentationLancementMatch.resume(liste));
        resume.setWrapText(true);

        ToggleGroup groupe = new ToggleGroup();
        ToggleButton proposes = new ToggleButton("Matchs proposés");
        ToggleButton choix = new ToggleButton("Choix manuel");
        HBox segment = Ui.segment(groupe, proposes, choix);
        (manuel ? choix : proposes).setSelected(true);
        groupe.selectedToggleProperty().addListener((o, avant, apres) -> {
            if (apres == null) {
                avant.setSelected(true); // un des deux reste toujours choisi
                return;
            }
            manuel = apres == choix;
            actions.rafraichir();
        });

        Button proposer = Ui.boutonSecondaire("_Proposer");
        proposer.setMnemonicParsing(true);
        proposer.setDisable(ferme || liste.nombreLancables() == 0);
        proposer.setTooltip(new Tooltip("Alt+P · remplit toutes les tables libres avec les matchs proposés, du plus "
                + "prioritaire au moins prioritaire. Un joueur n'est jamais envoyé sur deux tables."));
        proposer.setOnAction(e -> actions.proposer());

        // B016 : feuille de match imprimée à chaque envoi sur une table (mémorisé pour ce tournoi).
        CheckBox feuille = new CheckBox("Feuille à l'envoi");
        feuille.setSelected(feuilleALEnvoi);
        feuille.setDisable(ferme);
        feuille.setTooltip(new Tooltip("Imprime la feuille de match de chaque rencontre envoyée sur une table. "
                + "Sans cette option, « Feuille » sur une table occupée l'imprime à la demande."));
        feuille.setOnAction(e -> actions.feuilleALEnvoi(feuille.isSelected()));

        FlowPane outils = new FlowPane(8, 8, proposer, feuille);
        outils.setAlignment(Pos.CENTER_LEFT);
        VBox entete = new VBox(10, Ui.enteteAvecActions(Ui.titreCarte(PresentationTableauDeBordJourJ.CARTE_LANCER),
                fermer), resume, segment, outils);
        entete.getStyleClass().add("jourj-lancement-entete");

        VBox lignes = new VBox(0);
        VBox corps;
        if (manuel) {
            corps = choixManuel(liste, nombreDeTables, ferme, lignes, cleGlisser);
        } else {
            for (ListeLancements.Ligne l : liste.getProposes()) {
                lignes.getChildren().add(ligne(l, nombreDeTables, ferme, liste.getMaintenant(), cleGlisser));
            }
            if (liste.getProposes().isEmpty()) {
                lignes.getChildren().add(Ui.etatVideCompact(PresentationLancementMatch.videProposes()));
            }
            corps = Ui.carte(Ui.titreAvecCompteur("Matchs proposés", liste.nombrePrets()),
                    Ui.aide("Du plus prioritaire au moins prioritaire, selon la stratégie en place. Rouge : attend "
                            + "depuis longtemps ; orange : important ou prochain à envoyer ; vert : peut attendre."),
                    lignes);
        }
        VBox colonne = new VBox(16, entete, corps);
        colonne.getStyleClass().add("lancement-match");
        return colonne;
    }

    /** Recherche par joueur, filtre par tableau, et les lignes correspondantes. */
    private VBox choixManuel(ListeLancements liste, int nombreDeTables, boolean ferme, VBox lignes,
                             Function<Match, String> cleGlisser) {
        TextField champ = Ui.champ("Nom d'un joueur, d'un tableau ou d'une poule");
        champ.setText(recherche);
        ComboBox<String> filtre = new ComboBox<>();
        filtre.getItems().setAll(PresentationLancementMatch.tableaux(liste));
        if (!filtre.getItems().contains(tableau)) {
            tableau = PresentationLancementMatch.TOUS_LES_TABLEAUX;
        }
        filtre.setValue(tableau);
        filtre.setMaxWidth(Double.MAX_VALUE);
        Runnable remplir = () -> {
            lignes.getChildren().clear();
            List<ListeLancements.Ligne> trouvees = PresentationLancementMatch.filtrer(liste, recherche, tableau);
            for (ListeLancements.Ligne l : trouvees) {
                lignes.getChildren().add(ligne(l, nombreDeTables, ferme, liste.getMaintenant(), cleGlisser));
            }
            if (trouvees.isEmpty()) {
                lignes.getChildren().add(Ui.etatVideCompact(PresentationLancementMatch.videManuel(recherche)));
            }
        };
        champ.textProperty().addListener((o, avant, apres) -> {
            recherche = apres == null ? "" : apres;
            remplir.run();
        });
        filtre.valueProperty().addListener((o, avant, apres) -> {
            tableau = apres == null ? PresentationLancementMatch.TOUS_LES_TABLEAUX : apres;
            remplir.run();
        });
        remplir.run();
        javafx.application.Platform.runLater(() -> {
            champ.requestFocus();
            champ.end();
        });
        return Ui.carte(Ui.titreCarte("Choix manuel"),
                Ui.aide("Choisissez le match, puis la table libre. Un joueur déjà sur une table ne peut pas être "
                        + "envoyé sur une autre : ses matchs restent grisés avec la raison."),
                champ, filtre, lignes);
    }

    /** Une ligne : contexte, joueurs, attente, puis « Lancer · table N » et le choix de la table, ou la raison. */
    private Node ligne(ListeLancements.Ligne l, int nombreDeTables, boolean ferme, LocalDateTime maintenant,
                       Function<Match, String> cleGlisser) {
        MatchSalle m = l.match();
        Match match = m.match();
        Button contexte = Ui.boutonLien(PresentationSalle.contexte(m));
        contexte.getStyleClass().add("lancement-contexte");
        contexte.setTooltip(new Tooltip("Ouvrir le tableau " + m.tableau()));
        contexte.setOnAction(e -> actions.ouvrirTableau(m.tableau()));
        HBox haut = Ui.ligne(8, contexte, Ui.espace());
        if (l.estLancable()) {
            Label niveau = Ui.pastille(m.niveau().getLibelle(), PresentationSalle.pastillePriorite(m.niveau()));
            niveau.setTooltip(new Tooltip(PresentationSalle.detailPriorite(m)));
            haut.getChildren().add(niveau);
        }
        Label joueurs = Ui.gras(PresentationSalle.joueurs(match));
        joueurs.setWrapText(true);
        VBox ligne = new VBox(6, haut, joueurs);
        ligne.getStyleClass().add("salle-pret");

        if (l.attenteDepuis() != null) {
            Label attente = new Label();
            attente.getStyleClass().add("lancement-attente");
            attentes.put(attente, l.attenteDepuis());
            majAttente(attente, l.attenteDepuis(), maintenant);
            ligne.getChildren().add(attente);
        }

        if (!l.estLancable()) {
            ligne.getStyleClass().add("salle-bloque");
            Label motif = new Label(PresentationLancementMatch.motif(l.refus(), nombreDeTables));
            motif.getStyleClass().add("lancement-motif");
            motif.setWrapText(true);
            Button lancer = Ui.boutonPrimaire(PresentationLancementMatch.libelleLancer(null));
            lancer.setDisable(true);
            ligne.getChildren().add(Ui.ligne(8, lancer, motif));
            return ligne;
        }

        // La raison de la couleur est dans l'info-bulle de la pastille ; l'attente a sa propre ligne.
        ligne.getStyleClass().add(PresentationSalle.classePriorite(m.niveau()));
        Integer table = PresentationLancementMatch.tableRetenue(l, choixTables.get(match));
        Button lancer = Ui.boutonPrimaire(PresentationLancementMatch.libelleLancer(table));
        lancer.getStyleClass().add("jourj-envoi-direct");
        lancer.setDisable(ferme);
        lancer.setTooltip(new Tooltip("Envoyer ce match sur la table choisie. Les joueurs et la table sont vérifiés "
                + "de nouveau au moment du lancement."));
        lancer.setOnAction(e -> {
            Integer t = PresentationLancementMatch.tableRetenue(l, choixTables.get(match));
            if (t != null) {
                actions.lancer(match, t);
            }
        });
        HBox bas = Ui.ligne(8, lancer);
        if (l.tablesPossibles().size() > 1) {
            ComboBox<Integer> tables = new ComboBox<>();
            tables.getItems().setAll(l.tablesPossibles());
            tables.setConverter(new StringConverter<>() {
                @Override
                public String toString(Integer t) {
                    return t == null ? "" : PresentationLancementMatch.libelleTable(t);
                }

                @Override
                public Integer fromString(String s) {
                    return null;
                }
            });
            tables.setValue(table);
            tables.getStyleClass().add("lancement-table");
            tables.setTooltip(new Tooltip("Changer de table avant de lancer (tables libres qui peuvent recevoir "
                    + "ce match)."));
            tables.setDisable(ferme);
            tables.valueProperty().addListener((o, avant, apres) -> {
                if (apres != null) {
                    choixTables.put(match, apres);
                    lancer.setText(PresentationLancementMatch.libelleLancer(apres));
                }
            });
            bas.getChildren().add(tables);
        }
        ligne.getChildren().add(bas);

        String cle = cleGlisser == null ? null : cleGlisser.apply(match);
        if (cle != null && !ferme) {
            ligne.setCursor(Cursor.OPEN_HAND);
            ligne.setOnDragDetected(e -> {
                Dragboard d = ligne.startDragAndDrop(TransferMode.MOVE);
                ClipboardContent contenu = new ClipboardContent();
                contenu.putString(cle);
                d.setContent(contenu);
                e.consume();
            });
        }
        return ligne;
    }

    /** Rafraîchit les temps d'attente affichés (minuterie de la salle). */
    void rafraichirAttentes(LocalDateTime maintenant) {
        attentes.forEach((l, depuis) -> majAttente(l, depuis, maintenant));
    }

    private static void majAttente(Label l, LocalDateTime depuis, LocalDateTime maintenant) {
        Duration d = Duration.between(depuis, maintenant);
        if (d.isNegative()) {
            d = Duration.ZERO;
        }
        l.setText(PresentationLancementMatch.attente(d));
        l.getStyleClass().remove("lancement-attente-longue");
        if (PresentationLancementMatch.attenteLongue(d)) {
            l.getStyleClass().add("lancement-attente-longue");
        }
    }
}
