package com.pingmanager.ui.ecran;

import com.pingmanager.core.CalendrierTournoi;
import com.pingmanager.core.RemplissageTableaux;
import com.pingmanager.model.FicheLicence;
import com.pingmanager.model.StatutTournoi;
import com.pingmanager.model.Tournoi;
import com.pingmanager.ui.ContexteApplication;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Page d'accueil (vue joueur, visible de tous), en tableau de bord : une rangée
 * de <b>tuiles de chiffres clés</b> (tournois ouverts, mes inscriptions, mes
 * points, tournois du club), puis les prochains tournois du joueur et des
 * raccourcis. Lecture seule : les chiffres sont recalculés à chaque ouverture
 * depuis le {@link ContexteApplication}.
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
        List<ContexteApplication.TournoiFichier> tous = contexte.tousLesTournois();
        Optional<FicheLicence> fiche = contexte.ficheLicenceLiee();

        List<Tournoi> mesProchains = new ArrayList<>();
        for (ContexteApplication.TournoiFichier tf : contexte.mesTournoisJoueur(tous)) {
            Tournoi t = tf.tournoi();
            LocalDate fin = CalendrierTournoi.fin(t);
            if (t.getStatut() != StatutTournoi.BROUILLON && (fin == null || !fin.isBefore(auj))) {
                mesProchains.add(t);
            }
        }
        mesProchains.sort(Comparator.comparing(t -> CalendrierTournoi.debut(t),
                Comparator.nullsLast(Comparator.naturalOrder())));

        VBox entete = new VBox(4,
                Ui.titre("Bonjour " + contexte.compteConnecte().getPrenom()),
                Ui.label(capitaliser(JOUR.format(auj)), "page-chapeau"));

        VBox contenu = Ui.page(1180, entete, tuiles(tous, mesProchains, fiche, auj));

        GridPane grille = new GridPane();
        grille.setHgap(20);
        grille.setVgap(20);
        grille.add(carteProchains(mesProchains, fiche.isPresent()), 0, 0);
        grille.add(colonneDroite(), 1, 0);
        ColumnConstraints gauche = new ColumnConstraints();
        gauche.setPercentWidth(62);
        ColumnConstraints droite = new ColumnConstraints();
        droite.setPercentWidth(38);
        grille.getColumnConstraints().addAll(gauche, droite);
        contenu.getChildren().add(grille);
        return Ui.defilant(contenu);
    }

    private Node tuiles(List<ContexteApplication.TournoiFichier> tous, List<Tournoi> mesProchains,
                        Optional<FicheLicence> fiche, LocalDate auj) {
        int ouverts = 0;
        for (ContexteApplication.TournoiFichier tf : tous) {
            Tournoi t = tf.tournoi();
            LocalDate fin = CalendrierTournoi.fin(t);
            if (t.getStatut() == StatutTournoi.PUBLIE && (fin == null || !fin.isBefore(auj))) {
                ouverts++;
            }
        }
        List<Node> t = new ArrayList<>();
        VBox ouvertsTuile = Ui.tuileChiffre(String.valueOf(ouverts),
                ouverts > 1 ? "tournois publiés à venir" : "tournoi publié à venir");
        ouvertsTuile.getStyleClass().add("tuile-cle-accent");
        t.add(ouvertsTuile);
        t.add(Ui.tuileChiffre(String.valueOf(mesProchains.size()),
                mesProchains.size() > 1 ? "inscriptions à venir" : "inscription à venir"));
        if (fiche.isPresent()) {
            t.add(Ui.tuileChiffre(String.valueOf(fiche.get().getPoints()), "points (classement officiel)"));
        } else {
            t.add(Ui.tuileChiffre("—", "points", Ui.pastille("Licence non liée", StatutPastille.NEUTRE)));
        }
        if (contexte.estOrganisateur()) {
            List<ContexteApplication.TournoiFichier> club = contexte.mesTournoisClub(tous);
            long enPreparation = club.stream().filter(x -> x.tournoi().getStatut() != StatutTournoi.PUBLIE
                    && x.tournoi().getStatut() != StatutTournoi.ANNULE).count();
            t.add(Ui.tuileChiffre(String.valueOf(club.size()), club.size() > 1 ? "tournois du club" : "tournoi du club",
                    enPreparation > 0 ? Ui.pastille(enPreparation + " en préparation", StatutPastille.INFO) : null));
        }
        return Ui.rangeeTuiles(t.toArray(new Node[0]));
    }

    private Node carteProchains(List<Tournoi> prochains, boolean licenceLiee) {
        VBox corps = new VBox(0);
        if (!licenceLiee) {
            Button profil = Ui.boutonSecondaire("Lier ma licence");
            profil.setOnAction(e -> naviguer.accept("profil"));
            corps.getChildren().add(Ui.etatVide("Liez votre licence dans « Mon profil » : vos prochains tournois "
                    + "s'afficheront ici, avec la date et vos tableaux.", profil));
        } else if (prochains.isEmpty()) {
            Button trouver = Ui.boutonPrimaire("Trouver un tournoi");
            trouver.setOnAction(e -> naviguer.accept("tournois"));
            corps.getChildren().add(Ui.etatVide("Vous n'êtes inscrit à aucun tournoi à venir. "
                    + "Les tournois où vous vous inscrirez s'afficheront ici.", trouver));
        } else {
            String licence = contexte.ficheLicenceLiee().map(FicheLicence::getNumeroLicence).orElse("");
            boolean premiere = true;
            for (Tournoi t : prochains) {
                HBox l = ligneTournoi(t, licence);
                if (premiere) {
                    l.getStyleClass().add("ligne-element-premiere");
                    premiere = false;
                }
                corps.getChildren().add(l);
            }
        }
        return Ui.carte(Ui.titreAvecCompteur("Mes prochains tournois", prochains.size()), corps);
    }

    private static HBox ligneTournoi(Tournoi t, String licence) {
        Optional<RemplissageTableaux.Participant> p = RemplissageTableaux.participant(t, licence);
        String detail = lieu(t);
        Node etat;
        if (p.isPresent() && !p.get().inscrit().isEmpty()) {
            int n = p.get().inscrit().size();
            etat = Ui.pastille("Inscrit · " + n + (n > 1 ? " tableaux" : " tableau"), StatutPastille.SUCCES);
        } else {
            etat = Ui.pastille("Liste d'attente", StatutPastille.ATTENTION);
        }
        return Ui.ligneElement(Ui.blocDate(CalendrierTournoi.debut(t)), t.getNom(), detail, etat);
    }

    private static String lieu(Tournoi t) {
        if (t.getAdresse() != null && t.getAdresse().getVille() != null && !t.getAdresse().getVille().isBlank()) {
            return t.getAdresse().getVille() + " · " + t.getClubOrganisateur();
        }
        return t.getClubOrganisateur();
    }

    private Node colonneDroite() {
        VBox raccourcis = new VBox(8);
        raccourcis.getChildren().add(raccourci("Trouver un tournoi", "tournois", Icones.TOURNOIS));
        if (contexte.estOrganisateur()) {
            raccourcis.getChildren().add(raccourci("Gérer les tournois du club", "gestion", Icones.GESTION));
        }
        raccourcis.getChildren().add(raccourci("Mon profil et ma licence", "profil", Icones.PROFIL));

        VBox bientot = new VBox(10,
                Ui.ligne(8, Ui.texte("Agenda des échéances"), Ui.espace(), Ui.pastille("Bientôt", StatutPastille.NEUTRE)),
                Ui.ligne(8, Ui.texte("Notes personnelles"), Ui.espace(), Ui.pastille("Bientôt", StatutPastille.NEUTRE)),
                Ui.aide("Clôtures d'inscription, rappels et objectifs apparaîtront ici."));

        return new VBox(20, Ui.carte("Raccourcis", raccourcis), Ui.carte("À venir dans PingManager", bientot));
    }

    private Button raccourci(String texte, String cle, String trace) {
        Button b = Ui.boutonSecondaire(texte);
        b.setGraphic(Ui.iconeCadree(trace, "icone-raccourci"));
        b.getStyleClass().add("raccourci");
        b.setMaxWidth(Double.MAX_VALUE);
        b.setOnAction(e -> naviguer.accept(cle));
        return b;
    }

    private static String capitaliser(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
