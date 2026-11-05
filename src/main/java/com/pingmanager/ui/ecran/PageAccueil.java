package com.pingmanager.ui.ecran;

import com.pingmanager.core.CalendrierTournoi;
import com.pingmanager.model.AccesClub;
import com.pingmanager.model.Club;
import com.pingmanager.model.StatutTournoi;
import com.pingmanager.model.Tournoi;
import com.pingmanager.ui.ContexteApplication;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Page d'accueil (« home ») : point d'entrée qui explique chaque section et y
 * mène. Vue mince : le contenu des cartes et la visibilité de la section
 * « Gestion de votre club » viennent de {@link PresentationAccueil}.
 */
public final class PageAccueil {

    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);

    private final ContexteApplication contexte;
    private final Consumer<String> naviguer;

    public PageAccueil(ContexteApplication contexte) {
        this(contexte, cle -> { });
    }

    /** @param naviguer ouvre une rubrique de la coquille par sa clé (« tournois », « gestion »…) */
    public PageAccueil(ContexteApplication contexte, Consumer<String> naviguer) {
        this.contexte = contexte;
        this.naviguer = naviguer;
    }

    public Parent build() {
        LocalDate auj = LocalDate.now();
        PresentationAccueil p = PresentationAccueil.pour(
                contexte.compteConnecte().getPrenom(), contexte.compteConnecte().getNom(),
                contexte.ficheLicenceLiee(), nomsClubs(), prochainesDates(auj));

        VBox entete = new VBox(4,
                Ui.titre(p.salutation()),
                Ui.label(capitaliser(JOUR.format(auj)), "page-chapeau"));
        VBox contenu = Ui.page(1180, entete, grille(p.cartesPourTous()));

        if (p.sectionClubVisible()) {
            Label titreClub = Ui.label(p.titreSectionClub(), "accueil-section-club-titre");
            Label intro = Ui.label(p.introSectionClub(), "accueil-section-club-intro");
            intro.setWrapText(true);
            Node tuileClub = Ui.iconeTuile(Icones.GESTION, false);
            tuileClub.getStyleClass().add("accueil-club-tuile");
            VBox section = new VBox(18,
                    Ui.ligne(12, tuileClub, new VBox(2, titreClub, intro)),
                    grille(p.cartesClub()),
                    Ui.aide(p.mentionAutresOutils()));
            section.getStyleClass().add("accueil-section-club");
            contenu.getChildren().add(section);
        }
        return Ui.defilant(contenu);
    }

    /** Prochaines dates du joueur : ses tournois non terminés (hors brouillons). */
    private List<PresentationAccueil.DateAgenda> prochainesDates(LocalDate auj) {
        List<PresentationAccueil.DateAgenda> dates = new ArrayList<>();
        for (ContexteApplication.TournoiFichier tf : contexte.mesTournoisJoueur(contexte.tousLesTournois())) {
            Tournoi t = tf.tournoi();
            LocalDate fin = CalendrierTournoi.fin(t);
            if (t.getStatut() != StatutTournoi.BROUILLON && (fin == null || !fin.isBefore(auj))) {
                dates.add(new PresentationAccueil.DateAgenda(CalendrierTournoi.debut(t), t.getNom()));
            }
        }
        return dates;
    }

    private List<String> nomsClubs() {
        Set<String> noms = new LinkedHashSet<>();
        for (AccesClub a : contexte.accesCourants()) {
            noms.add(contexte.clubParNumero(a.getNumeroClub()).map(Club::getNom).orElse(a.getNumeroClub()));
        }
        return new ArrayList<>(noms);
    }

    /** Cartes sur deux colonnes de même largeur. */
    private GridPane grille(List<PresentationAccueil.Carte> cartes) {
        GridPane g = new GridPane();
        g.setHgap(20);
        g.setVgap(20);
        for (int i = 0; i < 2; i++) {
            ColumnConstraints c = new ColumnConstraints();
            c.setPercentWidth(50);
            g.getColumnConstraints().add(c);
        }
        for (int i = 0; i < cartes.size(); i++) {
            VBox c = carte(cartes.get(i));
            c.setMaxHeight(Double.MAX_VALUE);
            GridPane.setVgrow(c, Priority.ALWAYS);
            g.add(c, i % 2, i / 2);
        }
        return g;
    }

    private VBox carte(PresentationAccueil.Carte c) {
        Node tuile = Ui.iconeTuile(icone(c.destination()), c.aVenir());
        Node entete = c.aVenir()
                ? Ui.ligne(12, tuile, Ui.titreCarte(c.titre()), Ui.espace(), Ui.pastille("Bientôt", StatutPastille.NEUTRE))
                : Ui.ligne(12, tuile, Ui.titreCarte(c.titre()));

        VBox corps = new VBox(10, Ui.texte(c.explication()));
        if (!c.champs().isEmpty()) {
            GridPane champs = new GridPane();
            champs.setHgap(16);
            champs.setVgap(4);
            for (int i = 0; i < c.champs().size(); i++) {
                champs.add(Ui.discret(c.champs().get(i).libelle()), 0, i);
                champs.add(Ui.gras(c.champs().get(i).valeur()), 1, i);
            }
            corps.getChildren().add(champs);
        }
        VBox.setVgrow(corps, Priority.ALWAYS);

        Button aller = c.aVenir() ? Ui.boutonSecondaire(c.action()) : Ui.boutonPrimaire(c.action());
        aller.setOnAction(e -> naviguer.accept(c.destination()));

        VBox carte = Ui.carte(entete, corps, Ui.actions(aller));
        carte.getStyleClass().add("accueil-carte");
        return carte;
    }

    private static String icone(String destination) {
        if (PresentationAccueil.PROFIL.equals(destination)) {
            return Icones.PROFIL;
        }
        if (PresentationAccueil.TOURNOIS.equals(destination)) {
            return Icones.TOURNOIS;
        }
        if (PresentationAccueil.AGENDA.equals(destination)) {
            return Icones.AGENDA;
        }
        if (PresentationAccueil.STATS.equals(destination)) {
            return Icones.STATS;
        }
        if (PresentationAccueil.HISTORIQUE.equals(destination)) {
            return Icones.HISTORIQUE;
        }
        if (PresentationAccueil.BUVETTE.equals(destination)) {
            return Icones.BUVETTE;
        }
        return Icones.GESTION;
    }

    private static String capitaliser(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
