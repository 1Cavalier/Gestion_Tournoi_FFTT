package com.pingmanager.ui.ecran;

import com.pingmanager.core.SaisieResultatMatch;
import com.pingmanager.core.SuiviPoule;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.LigneClassement;
import com.pingmanager.model.Match;
import com.pingmanager.model.Poule;
import com.pingmanager.model.StatutMatch;
import com.pingmanager.model.StatutTableau;
import com.pingmanager.model.StatutTournoi;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.format.FormatTournoi;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Phase de poules d'un tableau lancé : tirage (graine et composition), puis, poule
 * par poule, les parties dans l'ordre officiel (I.301), la saisie des scores
 * manche par manche ou d'une issue non normale (I.303 a), et le classement en
 * direct (I.303) avec les places qualificatives.
 *
 * <p>La phase finale n'est pas traitée ici : une fois toutes les poules jouées,
 * l'écran propose de construire le tableau final ({@link VuePhaseFinale}).</p>
 */
final class EcranPoulesTableau {

    private final Tournoi tournoi;
    private final Tableau tableau;
    private final Runnable enregistrer;
    private final Runnable onRetour;
    /** Ouvre le tableau final (second tirage, arbre) ; {@code null} si indisponible. */
    private final Runnable onTableauFinal;

    private final SaisieResultatMatch saisie = new SaisieResultatMatch();
    private final SuiviPoule suivi = new SuiviPoule();

    private final BorderPane racine = new BorderPane();
    private final ScrollPane defilement;
    private boolean compositionDepliee;
    private boolean apresTirage;
    private int pouleChoisie;
    private Match enEdition;
    private String messageSucces;
    private String messageErreur;

    EcranPoulesTableau(Tournoi tournoi, Tableau tableau, Runnable enregistrer, Runnable onRetour,
                       boolean apresTirage) {
        this(tournoi, tableau, enregistrer, onRetour, apresTirage, null);
    }

    EcranPoulesTableau(Tournoi tournoi, Tableau tableau, Runnable enregistrer, Runnable onRetour,
                       boolean apresTirage, Runnable onTableauFinal) {
        this.onTableauFinal = onTableauFinal;
        this.tournoi = tournoi;
        this.tableau = tableau;
        this.enregistrer = enregistrer;
        this.onRetour = onRetour;
        this.apresTirage = apresTirage;
        this.compositionDepliee = apresTirage;
        this.defilement = Ui.defilant(new VBox());
        racine.setCenter(defilement);
        // On ouvre la première poule qui a encore des matchs à jouer.
        for (int i = 0; i < tableau.getPoules().size(); i++) {
            if (!tableau.getPoules().get(i).estTerminee()) {
                pouleChoisie = i;
                break;
            }
        }
    }

    Node build() {
        afficher();
        return racine;
    }

    private boolean modifiable() {
        return tableau.getStatut() == StatutTableau.LANCE && tournoi.getStatut() != StatutTournoi.ANNULE;
    }

    private int qualifiesParPoule() {
        FormatTournoi f = tableau.getConfiguration().getFormat();
        return f == null ? 0 : f.qualifiesParPoule();
    }

    // ------------------------------------------------------------------
    // Page
    // ------------------------------------------------------------------

    private void afficher() {
        double position = defilement.getVvalue();
        Button retour = Ui.boutonLien("← Jour J");
        retour.setOnAction(e -> onRetour.run());
        PresentationJourJ.Etape etape = PresentationJourJ.etape(tableau);
        Label titre = Ui.titre("Poules — " + tableau.getNom());
        titre.setWrapText(true);
        HBox ligneTitre = Ui.ligne(12, titre,
                Ui.pastille(PresentationJourJ.libelle(etape), PresentationJourJ.classe(etape)));
        FormatTournoi format = tableau.getConfiguration().getFormat();
        Label sous = Ui.discret((format == null ? "" : format.nom() + " · ") + PresentationJourJ.resumePoules(tableau));
        VBox contenu = Ui.page(900, new VBox(6, retour, ligneTitre, sous));

        if (apresTirage) {
            Label ok = Ui.message();
            Ui.succes(ok, "Tirage effectué. Annoncez la graine et affichez la composition des poules.");
            contenu.getChildren().add(ok);
            apresTirage = false;
        }
        contenu.getChildren().add(carteTirage());

        if (tableau.getPoules().isEmpty()) {
            contenu.getChildren().add(Ui.carte("Poules", Ui.discret("Aucune poule : le tableau n'a pas été lancé.")));
            defilement.setContent(contenu);
            return;
        }
        if (tableau.getStatut() == StatutTableau.PHASE_FINALE) {
            contenu.getChildren().add(encartTableauFinal(
                    "Le tableau final est construit : les résultats de poule sont figés.", "Voir le tableau final"));
        } else if (tableau.poulesTerminees()) {
            contenu.getChildren().add(encartTableauFinal("Toutes les poules sont terminées : les qualifiés sont "
                    + "connus. Construisez le tableau final (second tirage au sort).", "Construire le tableau final"));
        }
        contenu.getChildren().addAll(selecteurPoules(), cartePoule(tableau.getPoules().get(pouleChoisie)));
        defilement.setContent(contenu);
        Platform.runLater(() -> defilement.setVvalue(position));
    }

    /** Encart menant au tableau final, mis à jour dès que la dernière partie de poule est saisie. */
    private Node encartTableauFinal(String texte, String action) {
        Label info = Ui.encartInfo(texte);
        if (onTableauFinal == null) {
            return info;
        }
        Button ouvrir = Ui.boutonPrimaire(action);
        ouvrir.setOnAction(e -> onTableauFinal.run());
        HBox h = Ui.ligne(12, info, ouvrir);
        HBox.setHgrow(info, Priority.ALWAYS);
        info.setMaxWidth(Double.MAX_VALUE);
        return h;
    }

    // ------------------------------------------------------------------
    // Tirage : graine et composition
    // ------------------------------------------------------------------

    private Node carteTirage() {
        Long graine = tableau.getGrainePoules();
        Label valeur = new Label(graine == null ? "non conservée" : String.valueOf(graine));
        valeur.getStyleClass().add(graine == null ? "discret" : "graine-tirage");
        HBox ligneGraine = Ui.ligne(12, Ui.gras("Graine du tirage"), valeur);

        Button deplier = Ui.boutonLien(compositionDepliee ? "Masquer la composition" : "Voir la composition des poules");
        deplier.setOnAction(e -> {
            compositionDepliee = !compositionDepliee;
            afficher();
        });
        VBox carte = Ui.carte(Ui.enteteAvecActions(Ui.titreCarte("Tirage au sort"), deplier), ligneGraine,
                Ui.aide(graine == null
                        ? "Ce tableau a été lancé avec une version qui ne conservait pas la graine du tirage."
                        : "IX.109 : le juge-arbitre est responsable du tirage au sort. Avec la même graine et les mêmes "
                                + "présents, PingManager refait exactement le même tirage : chacun peut le vérifier."));
        if (compositionDepliee) {
            FlowPane poules = new FlowPane(12, 12);
            for (Poule p : tableau.getPoules()) {
                VBox bloc = new VBox(4, Ui.gras("Poule " + p.getNom()));
                for (int i = 0; i < p.getJoueurs().size(); i++) {
                    Joueur j = p.getJoueurs().get(i);
                    bloc.getChildren().add(Ui.texte((i + 1) + ". " + j.getNomComplet()));
                    bloc.getChildren().add(Ui.aide(PresentationJourJ.detailJoueur(j)));
                }
                bloc.getStyleClass().add("tuile");
                bloc.setPrefWidth(250);
                poules.getChildren().add(bloc);
            }
            carte.getChildren().add(poules);
        }
        return carte;
    }

    // ------------------------------------------------------------------
    // Choix de la poule
    // ------------------------------------------------------------------

    private Node selecteurPoules() {
        FlowPane f = new FlowPane(8, 8);
        List<Poule> poules = tableau.getPoules();
        for (int i = 0; i < poules.size(); i++) {
            Poule p = poules.get(i);
            int index = i;
            Button b = new Button("Poule " + p.getNom() + "  " + SuiviPoule.matchsTermines(p) + "/" + p.getMatchs().size());
            b.getStyleClass().add("choix-poule");
            if (p.estTerminee()) {
                b.getStyleClass().add("choix-poule-terminee");
            }
            if (i == pouleChoisie) {
                b.getStyleClass().add("choix-poule-actif");
            }
            b.setMinWidth(Region.USE_PREF_SIZE);
            b.setOnAction(e -> {
                pouleChoisie = index;
                enEdition = null;
                messageSucces = null;
                afficher();
            });
            f.getChildren().add(b);
        }
        return f;
    }

    // ------------------------------------------------------------------
    // Une poule : classement en direct et parties
    // ------------------------------------------------------------------

    private Node cartePoule(Poule p) {
        int joues = SuiviPoule.matchsTermines(p);
        HBox entete = Ui.ligne(10, Ui.titreCarte("Poule " + p.getNom()),
                Ui.discret(joues + " / " + p.getMatchs().size() + " matchs joués"));
        if (p.estTerminee()) {
            entete.getChildren().add(Ui.pastille("Terminée", Ui.ETAT_OK));
        }
        // B016 : feuilles imprimées de la poule (feuille de poule + rencontre, feuilles de match).
        Button feuillePoule = Ui.boutonSecondaire("Imprimer la feuille de poule");
        feuillePoule.setTooltip(new Tooltip("Joueurs, ordre des parties (I.301), grille des résultats et classement "
                + "à remplir, puis la feuille de rencontre (une grille de manches par partie)."));
        feuillePoule.setOnAction(e -> imprimer(ImpressionJourJ.feuillePoule(tournoi, tableau, p)));
        Button feuillesMatch = Ui.boutonLien("Feuilles de match");
        feuillesMatch.setTooltip(new Tooltip("Une feuille par partie, deux par page, dans l'ordre officiel."));
        feuillesMatch.setOnAction(e -> imprimer(ImpressionJourJ.feuillesMatchsPoule(tournoi, tableau, p)));
        entete.getChildren().addAll(Ui.espace(), feuillesMatch, feuillePoule);
        VBox carte = Ui.carte(entete);
        if (messageSucces != null || messageErreur != null) {
            carte.getChildren().add(Ui.message(messageErreur, messageSucces));
            messageSucces = null;
            messageErreur = null;
        }
        carte.getChildren().addAll(classement(p), parties(p));
        return carte;
    }

    private Node classement(Poule p) {
        VBox v = new VBox(0);
        HBox colonnes = Ui.ligne(10, largeur(Ui.enteteColonne("Rang"), 48), extensible(Ui.enteteColonne("Joueur")),
                largeur(Ui.enteteColonne("Points"), 64), largeur(Ui.enteteColonne("V - D"), 64),
                largeur(Ui.enteteColonne("Manches"), 72), largeur(Ui.enteteColonne(""), 150));
        colonnes.getStyleClass().add("ligne-colonnes");
        v.getChildren().add(colonnes);
        for (SuiviPoule.Ligne l : suivi.classementEnDirect(p, qualifiesParPoule())) {
            LigneClassement c = l.classement();
            String situation = PresentationJourJ.libelleQualification(l.qualification());
            Node pastille = situation.isEmpty() ? new Label()
                    : Ui.pastille(situation, l.qualification() == SuiviPoule.Qualification.QUALIFIE ? Ui.ETAT_OK
                            : l.qualification() == SuiviPoule.Qualification.NON_QUALIFIE ? Ui.ETAT_OPTIONNEL
                            : Ui.ETAT_RECOMMANDE);
            int defaites = c.getMatchsPerdus() + c.getMatchsPerdusZeroPoint();
            HBox ligne = Ui.ligne(10, largeur(Ui.gras(PresentationJourJ.rang(c.getRang())), 48),
                    extensible(new VBox(1, Ui.texte(c.getJoueur().getNomComplet()),
                            Ui.aide(PresentationJourJ.detailJoueur(c.getJoueur())))),
                    largeur(Ui.gras(PresentationJourJ.points(c.getPointsClassement())), 64),
                    largeur(Ui.texte(c.getMatchsGagnes() + " - " + defaites), 64),
                    largeur(Ui.texte(c.getManchesGagnees() + " / " + c.getManchesPerdues()), 72),
                    largeur(new HBox(pastille), 150));
            ligne.getStyleClass().add("ligne-classement");
            if (l.enPlaceQualificative()) {
                ligne.getStyleClass().add("ligne-qualifiee");
            }
            v.getChildren().add(ligne);
        }
        return new VBox(6, Ui.sousTitre("Classement"), v,
                Ui.aide("I.303 : victoire 2 points, défaite 1 point, 0 point en cas de forfait, abandon, refus de jouer "
                        + "ou disqualification. À égalité, on départage par les parties entre ex æquo (points, puis "
                        + "quotient des manches, puis des points). Les " + qualifiesParPoule()
                        + " premiers sont qualifiés une fois la poule terminée."));
    }

    private Node parties(Poule p) {
        VBox v = new VBox(0);
        List<Match> matchs = p.getMatchs();
        int tourPrecedent = 0;
        for (int i = 0; i < matchs.size(); i++) {
            Match m = matchs.get(i);
            int tour = SuiviPoule.tourDuMatch(p, i);
            v.getChildren().add(ligneMatch(m, tour, tour != tourPrecedent));
            tourPrecedent = tour;
        }
        return new VBox(6, Ui.sousTitre("Parties, dans l'ordre officiel (I.301)"), v);
    }

    private Node ligneMatch(Match m, int tour, boolean nouveauTour) {
        Label badgeTour = nouveauTour ? Ui.badge("Tour " + tour) : new Label();
        String affiche = m.estBye() ? m.getJoueurA().getNomComplet() + " (exempt)"
                : m.getJoueurA().getNomComplet() + "  –  " + m.getJoueurB().getNomComplet();
        Label nom = Ui.texte(affiche);
        Joueur vainqueur = m.getStatut() == StatutMatch.TERMINE ? m.getVainqueur() : null;
        Label resultat = Ui.gras(PresentationJourJ.resultat(m));
        // Match en cours dans la salle : son numéro de table se lit de loin.
        String table = PresentationSalle.libelleTable(m);
        VBox centre = new VBox(2, table.isEmpty() ? nom
                : Ui.ligne(8, nom, Ui.pastille(table + " · en cours", Ui.ETAT_RECOMMANDE)));
        if (!resultat.getText().isEmpty()) {
            centre.getChildren().add(resultat);
            if (vainqueur != null && !m.estBye()) {
                centre.getChildren().add(Ui.aide("Vainqueur : " + vainqueur.getNomComplet()));
            }
        }
        HBox.setHgrow(centre, Priority.ALWAYS);
        centre.setMaxWidth(Double.MAX_VALUE);

        HBox actions = Ui.ligne(8);
        if (!m.estBye() && modifiable() && enEdition != m) {
            Button b = m.getStatut() == StatutMatch.TERMINE ? Ui.boutonLien("Corriger") : Ui.boutonSecondaire("Saisir");
            b.setOnAction(e -> {
                enEdition = m;
                messageSucces = null;
                afficher();
            });
            actions.getChildren().add(b);
        }
        if (!m.estBye()) {
            Button feuille = Ui.boutonLien("Feuille");
            feuille.setTooltip(new Tooltip("Imprimer la feuille de match de cette partie."));
            feuille.setOnAction(e -> imprimer(ImpressionJourJ.feuilleMatch(tournoi, m)));
            actions.getChildren().add(feuille);
        }
        HBox ligne = Ui.ligne(12, largeur(badgeTour, 64), centre, actions);
        VBox bloc = new VBox(8, ligne);
        bloc.getStyleClass().add("ligne-match");
        if (m.getStatut() == StatutMatch.TERMINE) {
            bloc.getStyleClass().add("ligne-match-jouee");
        } else if (!table.isEmpty()) {
            bloc.getStyleClass().add("ligne-match-en-cours");
        }
        if (enEdition == m && modifiable()) {
            bloc.getChildren().add(editeur(m));
        }
        return bloc;
    }

    // ------------------------------------------------------------------
    // Saisie d'un match (composant partagé avec la salle : EditeurScorePoule)
    // ------------------------------------------------------------------

    private Node editeur(Match m) {
        return EditeurScorePoule.creer(tableau, m, saisie, () -> {
            enEdition = null;
            afficher();
        }, this::apresSaisie);
    }

    private void apresSaisie(Match m) {
        enregistrer.run();
        enEdition = null;
        messageSucces = m.getStatut() == StatutMatch.TERMINE
                ? "Résultat enregistré : " + PresentationJourJ.resultat(m) + "."
                : "Résultat effacé : le match est de nouveau à jouer.";
        afficher();
    }

    /** Enregistre et ouvre la feuille, puis affiche le résultat dans la carte de la poule. */
    private void imprimer(ImpressionJourJ.Preparation preparation) {
        ImpressionJourJ.Message m = ImpressionJourJ.imprimer(preparation, false);
        if (m.erreur()) {
            messageErreur = m.texte();
        } else {
            messageSucces = m.texte();
        }
        afficher();
    }

    private static <R extends Region> R largeur(R r, double l) {
        r.setMinWidth(l);
        r.setPrefWidth(l);
        r.setMaxWidth(l);
        return r;
    }

    private static <R extends Region> R extensible(R r) {
        HBox.setHgrow(r, Priority.ALWAYS);
        r.setMaxWidth(Double.MAX_VALUE);
        return r;
    }
}
