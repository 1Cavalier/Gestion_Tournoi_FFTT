package com.pingmanager.ui.ecran;

import com.pingmanager.core.ServiceCompletudeTournoi.Niveau;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

import javafx.util.StringConverter;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BooleanSupplier;

/**
 * Point central de la présentation : chaque fabrique pose des <b>classes de
 * style</b> définies dans {@code theme.css} — aucun style écrit en dur ici ni
 * dans les écrans. Pour changer l'apparence, on modifie la feuille, pas le Java.
 */
final class Ui {

    // Classes d'état de complétude (cf. CHARTE_GRAPHIQUE.md §4).
    static final String ETAT_OK = "etat-ok";
    static final String ETAT_ATTENTION = "etat-attention";
    static final String ETAT_RECOMMANDE = "etat-recommande";
    static final String ETAT_OPTIONNEL = "etat-optionnel";
    static final String EN_ERREUR = "en-erreur";

    private Ui() {
    }

    // ------------------------------------------------------------------
    // Conteneurs
    // ------------------------------------------------------------------

    /** Colonne de page (marges standard), largeur max donnée. */
    static VBox page(double largeurMax, Node... contenu) {
        VBox v = new VBox(20, contenu);
        v.getStyleClass().add("page");
        v.setMaxWidth(largeurMax);
        return v;
    }

    /** Page défilante : la colonne est posée dans un ScrollPane transparent. */
    static ScrollPane defilant(Node contenu) {
        ScrollPane scroll = new ScrollPane(contenu);
        scroll.setFitToWidth(true);
        return scroll;
    }

    /** Carte encadrée avec un titre et un contenu. */
    static VBox carte(String titre, Node... contenu) {
        return carte(titreCarte(titre), contenu);
    }

    /** Carte encadrée dont l'en-tête est un nœud libre (titre + actions, etc.). */
    static VBox carte(Node entete, Node... contenu) {
        VBox c = new VBox(12);
        c.getStyleClass().add("carte");
        c.getChildren().add(entete);
        c.getChildren().addAll(contenu);
        return c;
    }

    /** Carte encadrée sans titre (le contenu porte sa propre structure). */
    static VBox carteNue(Node... contenu) {
        VBox c = new VBox(12, contenu);
        c.getStyleClass().add("carte");
        return c;
    }

    /** Titre de carte suivi d'un compteur en badge. */
    static HBox titreAvecCompteur(String titre, int n) {
        HBox h = new HBox(8, titreCarte(titre), badge(String.valueOf(n)));
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    /** En-tête de carte : titre à gauche, actions alignées à droite. */
    static HBox enteteAvecActions(Node titre, Node... actions) {
        HBox h = new HBox(10, titre, espace());
        h.getChildren().addAll(actions);
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    /** Ligne horizontale centrée verticalement. */
    static HBox ligne(double espacement, Node... noeuds) {
        HBox h = new HBox(espacement, noeuds);
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    /** Ligne d'une liste dans une carte (filet au-dessus). */
    static HBox ligneListe(Node... noeuds) {
        HBox h = ligne(12, noeuds);
        h.getStyleClass().add("ligne-liste");
        return h;
    }

    /** Contrôle segmenté : boutons à bascule accolés, un seul choisi (groupe fourni). */
    static HBox segment(javafx.scene.control.ToggleGroup groupe, javafx.scene.control.ToggleButton... boutons) {
        HBox h = new HBox();
        h.getStyleClass().add("segment");
        for (int i = 0; i < boutons.length; i++) {
            boutons[i].setToggleGroup(groupe);
            if (i == 0) {
                boutons[i].getStyleClass().add("segment-premier");
            }
            if (i == boutons.length - 1) {
                boutons[i].getStyleClass().add("segment-dernier");
            }
        }
        h.getChildren().addAll(boutons);
        return h;
    }

    /** Tuile de chiffre clé : grand nombre, libellé discret dessous. */
    static VBox tuile(String chiffre, String libelle) {
        VBox v = new VBox(2, label(chiffre, "tuile-chiffre"), label(libelle, "discret"));
        v.getStyleClass().add("tuile");
        v.setMinWidth(Region.USE_PREF_SIZE);
        return v;
    }

    /**
     * Encart de confirmation en ligne, pour une action qui ne se défait pas
     * (clôture du pointage, tirage au sort) : une phrase qui dit ce qui va se
     * passer, puis le bouton qui confirme et celui qui renonce.
     */
    static VBox encartConfirmation(String texte, Button confirmer, Button renoncer) {
        Label l = new Label(texte);
        l.setWrapText(true);
        l.getStyleClass().add("texte");
        VBox v = new VBox(10, l, actions(confirmer, renoncer));
        v.getStyleClass().add("encart-confirmation");
        return v;
    }

    /** Encart d'information (fond doux, filet primaire à gauche), sans couleur d'alerte. */
    static Label encartInfo(String texte) {
        Label l = label(texte, "encart-info");
        l.setWrapText(true);
        l.setMaxWidth(Double.MAX_VALUE);
        return l;
    }

    /** Région extensible qui pousse la suite d'une HBox vers la droite. */
    static Region espace() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    // ------------------------------------------------------------------
    // Textes
    // ------------------------------------------------------------------

    static Label titre(String texte) {
        return label(texte, "titre");
    }

    static Label sousTitre(String texte) {
        return label(texte, "sous-titre");
    }

    static Label titreCarte(String texte) {
        return label(texte, "carte-titre");
    }

    static Label gras(String texte) {
        return label(texte, "gras");
    }

    static Label texte(String s) {
        Label l = label(s, "texte");
        l.setWrapText(true);
        return l;
    }

    static Label discret(String s) {
        Label l = label(s, "discret");
        l.setWrapText(true);
        return l;
    }

    /** Texte d'aide (plus petit, interligne aéré). */
    static Label aide(String s) {
        Label l = label(s, "aide");
        l.setWrapText(true);
        return l;
    }

    /** Libellé de champ seul (même style que dans {@link #champEtiquete}). */
    static Label libelleChamp(String s) {
        return label(s, "libelle-champ");
    }

    /** En-tête de colonne d'un petit tableau. */
    static Label enteteColonne(String s) {
        return label(s, "entete-colonne");
    }

    /** Petite étiquette (badge) pour un compteur ou un état neutre. */
    static Label badge(String s) {
        Label l = label(s, "badge");
        l.setMinWidth(Region.USE_PREF_SIZE);
        return l;
    }

    /** Badge coloré selon un état de complétude ({@link #ETAT_OK}…). */
    static Label pastille(String s, String etat) {
        Label l = badge(s);
        l.getStyleClass().add(etat);
        return l;
    }

    /** Petite puce ronde colorée selon un état ({@link #ETAT_ATTENTION}…). */
    static Circle puce(String etat) {
        Circle c = new Circle(3.5);
        c.getStyleClass().addAll("puce", etat);
        return c;
    }

    /** Ce qu'implique un niveau d'exigence, en langage simple. */
    // NB : pas de switch sur l'énumération ici. javac génère alors une classe
    // annexe (Ui$2) qu'un build incrémental peut oublier -> NoClassDefFoundError.
    static String exigence(Niveau n) {
        if (n == Niveau.OBLIGATOIRE_HOMOLOGATION) {
            return "requis pour homologuer";
        }
        if (n == Niveau.OBLIGATOIRE_PUBLICATION) {
            return "requis pour publier";
        }
        return n == Niveau.RECOMMANDE ? "recommandé" : "optionnel";
    }

    /** Classe d'état associée à un niveau d'exigence non satisfait. */
    static String etatManquant(Niveau n) {
        if (n == Niveau.OBLIGATOIRE_HOMOLOGATION || n == Niveau.OBLIGATOIRE_PUBLICATION) {
            return ETAT_ATTENTION;
        }
        return n == Niveau.RECOMMANDE ? ETAT_RECOMMANDE : ETAT_OPTIONNEL;
    }

    // ------------------------------------------------------------------
    // Code couleur : rond = dépendance, cadre = état
    // ------------------------------------------------------------------

    static final String CADRE_OK = "cadre-ok";
    static final String CADRE_MANQUANT = "cadre-manquant";
    static final String CADRE_FACULTATIF = "cadre-facultatif";
    private static final List<String> CADRES = List.of(CADRE_OK, CADRE_MANQUANT, CADRE_FACULTATIF);

    static boolean estObligatoire(Niveau n) {
        return n == Niveau.OBLIGATOIRE_HOMOLOGATION || n == Niveau.OBLIGATOIRE_PUBLICATION;
    }

    /** Classe de cadre : vert si rempli, orange si obligatoire et vide, gris sinon. */
    static String cadre(Niveau n, boolean rempli) {
        if (rempli) {
            return CADRE_OK;
        }
        return estObligatoire(n) ? CADRE_MANQUANT : CADRE_FACULTATIF;
    }

    /** Applique la classe de cadre (en retirant la précédente). */
    static void poserCadre(Node n, String cadre) {
        n.getStyleClass().removeAll(CADRES);
        n.getStyleClass().add(cadre);
    }

    /** Libellé court de la dépendance, pour la légende et les infobulles. */
    static String dependance(Niveau n) {
        if (n == Niveau.OBLIGATOIRE_HOMOLOGATION) {
            return "Homologation";
        }
        if (n == Niveau.OBLIGATOIRE_PUBLICATION) {
            return "Publication";
        }
        return n == Niveau.RECOMMANDE ? "Recommandé" : "Optionnel";
    }

    private static String classeDependance(Niveau n) {
        if (n == Niveau.OBLIGATOIRE_HOMOLOGATION) {
            return "dep-homologation";
        }
        if (n == Niveau.OBLIGATOIRE_PUBLICATION) {
            return "dep-publication";
        }
        return n == Niveau.RECOMMANDE ? "dep-recommande" : "dep-optionnel";
    }

    /** Rond coloré selon la dépendance du champ (infobulle explicative). */
    static Circle puceDependance(Niveau n) {
        Circle c = new Circle(5);
        c.getStyleClass().addAll("puce-dependance", classeDependance(n));
        Tooltip.install(c, new Tooltip(exigence(n).substring(0, 1).toUpperCase()
                + exigence(n).substring(1)));
        return c;
    }

    /**
     * Légende du code couleur, à poser en haut d'un écran. On n'y montre que les
     * dépendances réellement présentes, pour rester simple.
     */
    static VBox legende(Collection<Niveau> presentes) {
        FlowPane ronds = new FlowPane(14, 6);
        ronds.getChildren().add(label("Le rond indique ce qui en dépend :", "legende-titre"));
        for (Niveau n : Niveau.values()) {
            if (presentes.contains(n)) {
                ronds.getChildren().add(ligne(6, puceDependance(n), new Label(dependance(n))));
            }
        }
        FlowPane cadres = new FlowPane(14, 6);
        cadres.getChildren().addAll(
                label("Le cadre indique l'état :", "legende-titre"),
                ligne(6, echantillon(CADRE_OK), new Label("rempli")),
                ligne(6, echantillon(CADRE_MANQUANT), new Label("obligatoire, à remplir")),
                ligne(6, echantillon(CADRE_FACULTATIF), new Label("facultatif")));
        VBox v = new VBox(8, ronds, cadres);
        v.getStyleClass().add("legende");
        return v;
    }

    private static Region echantillon(String cadre) {
        Region r = new Region();
        r.getStyleClass().addAll("echantillon", cadre);
        return r;
    }

    /** Badge d'état « rempli / à remplir / facultatif » pour la vue d'ensemble. */
    static Label badgeEtat(Niveau n, boolean rempli) {
        String cadre = cadre(n, rempli);
        String texte = rempli ? "rempli" : (estObligatoire(n) ? "à remplir" : "facultatif");
        return pastille(texte, cadre);
    }

    /** Champ en lecture seule (valeur issue de la base FFTT). */
    static TextField champLecture(String valeur) {
        TextField t = new TextField(valeur == null ? "" : valeur);
        t.setEditable(false);
        t.setFocusTraversable(false);
        t.getStyleClass().add("lecture");
        return t;
    }

    /** Petite note explicative sous un groupe de champs. */
    static Label note(String texte) {
        Label l = label(texte, "note-groupe");
        l.setWrapText(true);
        return l;
    }

    /** Mention discrète du niveau d'exigence. */
    static Label mentionExigence(Niveau n) {
        Label l = label(exigence(n), "mention-exigence");
        l.setMinWidth(Region.USE_PREF_SIZE);
        return l;
    }

    /** Message (erreur ou succès) masqué tant qu'il n'est pas rempli. */
    static Label message() {
        Label l = new Label();
        l.setWrapText(true);
        cacher(l);
        return l;
    }

    static void erreur(Label l, String texte) {
        l.getStyleClass().removeAll("message-succes", "message-erreur");
        l.getStyleClass().add("message-erreur");
        l.setText(texte);
        montrer(l);
    }

    static void succes(Label l, String texte) {
        l.getStyleClass().removeAll("message-succes", "message-erreur");
        l.getStyleClass().add("message-succes");
        l.setText(texte);
        montrer(l);
    }

    private static Label label(String texte, String classe) {
        Label l = new Label(texte);
        l.getStyleClass().add(classe);
        return l;
    }

    // ------------------------------------------------------------------
    // Boutons
    // ------------------------------------------------------------------

    static Button boutonPrimaire(String texte) {
        return bouton(texte, "bouton-primaire");
    }

    static Button boutonSecondaire(String texte) {
        return bouton(texte, "bouton-secondaire");
    }

    static Button boutonLien(String texte) {
        return bouton(texte, "bouton-lien");
    }

    private static Button bouton(String texte, String classe) {
        Button b = new Button(texte);
        b.getStyleClass().add(classe);
        b.setMinWidth(Region.USE_PREF_SIZE);
        return b;
    }

    /** Rangée d'actions de formulaire (primaire d'abord). */
    static HBox actions(Node... boutons) {
        return ligne(10, boutons);
    }

    // ------------------------------------------------------------------
    // Champs de formulaire
    // ------------------------------------------------------------------

    /** Champ texte avec une invite ; il perd l'état « en erreur » dès qu'on le modifie. */
    static TextField champ(String invite) {
        TextField t = new TextField();
        if (invite != null) {
            t.setPromptText(invite);
        }
        t.textProperty().addListener((o, a, n) -> t.getStyleClass().remove(EN_ERREUR));
        return t;
    }

    /** Liste déroulante d'énumération, affichée avec des libellés lisibles. */
    static <E extends Enum<E>> ComboBox<E> combo(E[] valeurs, E courant) {
        ComboBox<E> c = new ComboBox<>();
        c.getItems().addAll(valeurs);
        c.setConverter(new StringConverter<>() {
            @Override
            public String toString(E e) {
                return libelle(e);
            }

            @Override
            public E fromString(String s) {
                return null;
            }
        });
        c.setPromptText("Choisir…");
        if (courant != null) {
            c.getSelectionModel().select(courant);
        }
        c.valueProperty().addListener((o, a, n) -> c.getStyleClass().remove(EN_ERREUR));
        return c;
    }

    private static final Map<String, String> LIBELLES = Map.ofEntries(
            Map.entry("NATIONAL_A", "National A"),
            Map.entry("NATIONAL_B", "National B"),
            Map.entry("DEPARTEMENTAL", "Départemental"),
            Map.entry("REGIONAL", "Régional"),
            Map.entry("FOURNIES_PAR_ORGANISATEUR", "Fournies par l'organisateur"),
            Map.entry("APPORTEES_PAR_JOUEURS", "Apportées par les joueurs"));

    /**
     * Libellé lisible d'une constante : son {@code getLibelle()} s'il existe,
     * sinon une table de correspondance, sinon « Constante » mise en forme.
     */
    static String libelle(Enum<?> e) {
        if (e == null) {
            return "";
        }
        try {
            Method m = e.getClass().getMethod("getLibelle");
            Object v = m.invoke(e);
            if (v instanceof String s && !s.isBlank()) {
                return s;
            }
        } catch (ReflectiveOperationException ignore) {
            // pas de libellé métier : on retombe sur la table ou le nom
        }
        String connu = LIBELLES.get(e.name());
        if (connu != null) {
            return connu;
        }
        String brut = e.name().replace('_', ' ').toLowerCase(Locale.FRENCH);
        return Character.toUpperCase(brut.charAt(0)) + brut.substring(1);
    }

    static void marquerErreur(Node champ) {
        if (!champ.getStyleClass().contains(EN_ERREUR)) {
            champ.getStyleClass().add(EN_ERREUR);
        }
    }

    /** Libellé au-dessus du contrôle, sans niveau d'exigence. */
    static VBox champEtiquete(String libelle, Node controle) {
        return champEtiquete(libelle, controle, null, null, null);
    }

    /**
     * Libellé au-dessus du contrôle, avec le code couleur :
     * <ul>
     *   <li>un <b>rond</b> devant le libellé indique la dépendance
     *       (homologation, publication, recommandé, optionnel) ;</li>
     *   <li>le <b>cadre</b> du contrôle indique l'état, mis à jour à la frappe :
     *       vert = rempli, orange = obligatoire et vide, gris = facultatif vide.</li>
     * </ul>
     * Sans niveau ({@code null}) : libellé seul, cadre neutre.
     *
     * @param valeur  la propriété à observer pour savoir si le champ est rempli
     * @param rempli  test « le champ est-il renseigné ? »
     */
    static VBox champEtiquete(String libelle, Node controle, Niveau niveau,
                              ObservableValue<?> valeur, BooleanSupplier rempli) {
        Label l = label(libelle, "libelle-champ");
        l.setMinWidth(Region.USE_PREF_SIZE);
        HBox entete = ligne(7, l);
        if (niveau != null) {
            entete.getChildren().add(0, puceDependance(niveau));
            if (valeur != null && rempli != null) {
                Runnable maj = () -> poserCadre(controle, cadre(niveau, rempli.getAsBoolean()));
                valeur.addListener((o, a, n) -> maj.run());
                maj.run();
            }
        }
        VBox v = new VBox(6, entete, controle);
        if (controle instanceof Region r) {
            r.setMaxWidth(Double.MAX_VALUE);
        }
        return v;
    }

    /** Variante pratique pour un champ texte. */
    static VBox champEtiquete(String libelle, TextField champ, Niveau niveau) {
        return champEtiquete(libelle, champ, niveau, champ.textProperty(),
                () -> champ.getText() != null && !champ.getText().isBlank());
    }

    /** Variante pratique pour une liste déroulante. */
    static VBox champEtiquete(String libelle, ComboBox<?> combo, Niveau niveau) {
        return champEtiquete(libelle, combo, niveau, combo.valueProperty(),
                () -> combo.getValue() != null);
    }

    // ------------------------------------------------------------------
    // Visibilité
    // ------------------------------------------------------------------

    static void montrer(Node n) {
        n.setVisible(true);
        n.setManaged(true);
    }

    static void cacher(Node n) {
        n.setVisible(false);
        n.setManaged(false);
    }
}
