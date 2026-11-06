package com.pingmanager.ui.ecran;

import com.pingmanager.core.ServiceCompletudeTournoi.Niveau;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
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
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.FillRule;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;

import javafx.util.StringConverter;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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

    // ------------------------------------------------------------------
    // Charte v2 : tuiles de chiffres, pastilles, avatars, lignes, dates
    // ------------------------------------------------------------------

    /** Tuile de chiffre clé (v2) : grand nombre semi-gras, libellé discret dessous. */
    static VBox tuileChiffre(String valeur, String libelle) {
        return tuileChiffre(valeur, libelle, null);
    }

    /** Tuile de chiffre clé avec un complément sous le libellé (pastille, évolution). */
    static VBox tuileChiffre(String valeur, String libelle, Node complement) {
        Label v = label(valeur, "tuile-cle-valeur");
        Label l = label(libelle, "tuile-cle-libelle");
        l.setWrapText(true);
        VBox t = new VBox(4, v, l);
        if (complement != null) {
            VBox.setMargin(complement, new javafx.geometry.Insets(4, 0, 0, 0));
            t.getChildren().add(complement);
        }
        t.getStyleClass().add("tuile-cle");
        t.setMaxWidth(Double.MAX_VALUE);
        return t;
    }

    /** Rangée de tuiles qui se partagent la largeur à parts égales. */
    static HBox rangeeTuiles(Node... tuiles) {
        HBox h = new HBox(16);
        for (Node t : tuiles) {
            HBox.setHgrow(t, Priority.ALWAYS);
            if (t instanceof Region r) {
                r.setPrefWidth(0);
                r.setMinWidth(140);
            }
            h.getChildren().add(t);
        }
        h.getStyleClass().add("rangee-tuiles");
        return h;
    }

    /** Pastille de statut (rayon 999, fond doux, texte foncé). */
    static Label pastille(String texte, StatutPastille statut) {
        Label l = badge(texte);
        l.getStyleClass().addAll("pastille", statut.classeCss());
        return l;
    }

    /** Avatar rond aux initiales, teinte tirée du nom (diamètre 32). */
    static StackPane avatar(String prenom, String nom) {
        Label initiales = label(Avatars.initiales(prenom, nom), "avatar-initiales");
        StackPane p = new StackPane(initiales);
        p.getStyleClass().addAll("avatar", "avatar-teinte-" + Avatars.teinte(prenom, nom));
        p.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        p.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        return p;
    }

    /** Grand avatar (diamètre 40), pour un en-tête de fiche. */
    static StackPane avatarGrand(String prenom, String nom) {
        StackPane p = avatar(prenom, nom);
        p.getStyleClass().add("avatar-grand");
        return p;
    }

    /**
     * Ligne de liste v2 : un visuel (avatar, date…), un titre et un détail, puis
     * des éléments alignés à droite (pastilles, points, actions). Survol
     * {@code -surface-2}.
     */
    static HBox ligneElement(Node visuel, Node titre, String detail, Node... aDroite) {
        VBox textes = new VBox(2, titre);
        if (detail != null && !detail.isBlank()) {
            Label d = label(detail, "ligne-element-detail");
            textes.getChildren().add(d);
        }
        textes.setMinWidth(0);
        HBox.setHgrow(textes, Priority.ALWAYS);
        HBox h = new HBox(14);
        if (visuel != null) {
            h.getChildren().add(visuel);
        }
        h.getChildren().add(textes);
        h.getChildren().addAll(aDroite);
        h.setAlignment(Pos.CENTER_LEFT);
        h.getStyleClass().add("ligne-element");
        return h;
    }

    /** Variante avec un titre texte (semi-gras). */
    static HBox ligneElement(Node visuel, String titre, String detail, Node... aDroite) {
        Label t = label(titre, "ligne-element-titre");
        return ligneElement(visuel, t, detail, aDroite);
    }

    /** Ligne de joueur : avatar, nom, club, points puis pastilles. */
    static HBox ligneJoueur(String prenom, String nom, String club, Integer points, Node... pastilles) {
        Node[] droite = new Node[pastilles.length + (points == null ? 0 : 1)];
        int i = 0;
        if (points != null) {
            droite[i++] = label(points + " pts", "ligne-element-points");
        }
        for (Node p : pastilles) {
            droite[i++] = p;
        }
        String nomComplet = ((prenom == null ? "" : prenom) + " " + (nom == null ? "" : nom)).trim();
        return ligneElement(avatar(prenom, nom), nomComplet, club, droite);
    }

    private static final DateTimeFormatter MOIS_COURT = DateTimeFormatter.ofPattern("MMM", Locale.FRENCH);

    /** Petit bloc date (jour en grand, mois abrégé dessous) ; « — / à fixer » si inconnue. */
    static VBox blocDate(LocalDate date) {
        String jour = date == null ? "—" : String.valueOf(date.getDayOfMonth());
        String mois = date == null ? "À FIXER" : MOIS_COURT.format(date).replace(".", "").toUpperCase(Locale.FRENCH);
        VBox v = new VBox(0, label(jour, "bloc-date-jour"), label(mois, "bloc-date-mois"));
        v.setAlignment(Pos.CENTER);
        v.getStyleClass().add("bloc-date");
        v.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        return v;
    }

    /** État vide compact (une ligne dans une carte de liste). */
    static VBox etatVideCompact(String texte) {
        VBox v = etatVide(texte, null);
        v.getStyleClass().add("etat-vide-compact");
        return v;
    }

    /** État vide soigné : une phrase qui dit ce qui apparaîtra, et une action facultative. */
    static VBox etatVide(String texte, Node action) {
        Label l = label(texte, "etat-vide-texte");
        l.setWrapText(true);
        VBox v = new VBox(10, l);
        if (action != null) {
            v.getChildren().add(action);
        }
        v.getStyleClass().add("etat-vide");
        return v;
    }

    /** Icône simple dessinée (tracé SVG sur 16 × 16), couleur portée par une classe CSS. */
    static SVGPath icone(String trace, String classe) {
        SVGPath p = new SVGPath();
        p.setContent(trace);
        p.setFillRule(FillRule.EVEN_ODD);
        p.getStyleClass().addAll("icone", classe);
        return p;
    }

    /** Icône posée dans un carré fixe de 16 px : les libellés qui suivent restent alignés. */
    static StackPane iconeCadree(String trace, String classe) {
        StackPane p = new StackPane(icone(trace, classe));
        p.setMinSize(16, 16);
        p.setPrefSize(16, 16);
        p.setMaxSize(16, 16);
        return p;
    }

    /**
     * Icône dans une tuile carrée de 36 px à fond teinté (en-têtes de cartes de section).
     * {@code neutre} : tuile grise, pour une section pas encore disponible.
     */
    static StackPane iconeTuile(String trace, boolean neutre) {
        StackPane p = new StackPane(icone(trace, "icone-tuile-trace"));
        p.getStyleClass().add("icone-tuile");
        if (neutre) {
            p.getStyleClass().add("icone-tuile-neutre");
        }
        p.setMinSize(36, 36);
        p.setPrefSize(36, 36);
        p.setMaxSize(36, 36);
        return p;
    }

    /** Durée des transitions d'ouverture (charte : 150 à 200 ms, jamais plus). */
    static final Duration DUREE_APPARITION = Duration.millis(180);

    /** Apparition d'une page ou d'un panneau : fondu et léger glissement vers le haut. */
    static void apparition(Node n) {
        if (n == null) {
            return;
        }
        FadeTransition fondu = new FadeTransition(DUREE_APPARITION, n);
        fondu.setFromValue(0);
        fondu.setToValue(1);
        TranslateTransition glisse = new TranslateTransition(DUREE_APPARITION, n);
        glisse.setFromY(8);
        glisse.setToY(0);
        glisse.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(fondu, glisse).play();
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

    /**
     * Encart d'alerte bien visible (fond et filet d'attention) : un titre en
     * gras puis le texte, à placer <b>avant</b> une action engageante.
     */
    static VBox encartAlerte(String titre, String texte) {
        Label t = label(titre, "encart-alerte-titre");
        t.setWrapText(true);
        Label l = label(texte, "encart-alerte-texte");
        l.setWrapText(true);
        l.setMaxWidth(Double.MAX_VALUE);
        VBox v = new VBox(6, t, l);
        v.getStyleClass().add("encart-alerte");
        v.setMaxWidth(Double.MAX_VALUE);
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
        l.setMaxWidth(Region.USE_PREF_SIZE); // une pastille ne s'étire jamais
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

    /**
     * Verrouille un contrôle (champ du dossier d'homologation, B035) : grisé,
     * non modifiable, avec le motif en infobulle.
     */
    static void verrouiller(javafx.scene.control.Control c, String motif) {
        if (c instanceof javafx.scene.control.TextInputControl t) {
            t.setEditable(false);
            t.setFocusTraversable(false);
        } else {
            c.setDisable(true);
        }
        c.getStyleClass().add("champ-verrouille");
        if (motif != null) {
            c.setTooltip(new javafx.scene.control.Tooltip(motif));
        }
    }

    /** Carte d'une partie annoncée mais pas encore disponible (pastille « Bientôt »). */
    static VBox carteBientot(String titre, String description, String tache) {
        return carteBientot(null, titre, description, tache);
    }

    /**
     * Carte « bientôt » avec une icône en tuile : icône et pastille sur la
     * première ligne, puis le titre et la description (tous deux à la ligne si
     * besoin), pour tenir dans une colonne étroite.
     */
    static VBox carteBientot(String trace, String titre, String description, String tache) {
        Label pastille = label("Bientôt", "pastille-bientot");
        pastille.setMinWidth(Region.USE_PREF_SIZE);
        HBox haut = trace == null ? ligne(8, espace(), pastille) : ligne(8, iconeTuile(trace, false), espace(), pastille);
        haut.setAlignment(Pos.CENTER_LEFT);
        Label t = titreCarte(titre);
        t.setWrapText(true);
        Label d = discret(description);
        d.setWrapText(true);
        VBox v = new VBox(10, haut, t, d);
        v.getStyleClass().addAll("carte", "carte-bientot");
        if (tache != null) {
            v.setAccessibleHelp("Tâche " + tache);
        }
        return v;
    }

    /** Bandeau expliquant pourquoi des champs sont verrouillés (dossier d'homologation, B035). */
    static Label bandeauVerrou(String motif) {
        Label l = label(motif, "bandeau-verrou");
        l.setGraphic(iconeCadree(Icones.CADENAS, "icone-verrou"));
        l.setGraphicTextGap(10);
        l.setWrapText(true);
        l.setMaxWidth(Double.MAX_VALUE);
        return l;
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

    /** Libellé portant une classe de style. */
    static Label label(String texte, String classe) {
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

    /** Variante pour un sélecteur de date : l'état suit le texte tapé comme le jour choisi. */
    static VBox champEtiquete(String libelle, javafx.scene.control.DatePicker date, Niveau niveau) {
        return champEtiquete(libelle, date, niveau, date.getEditor().textProperty(),
                () -> date.getEditor().getText() != null && !date.getEditor().getText().isBlank());
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
