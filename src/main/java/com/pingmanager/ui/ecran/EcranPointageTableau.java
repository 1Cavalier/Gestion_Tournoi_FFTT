package com.pingmanager.ui.ecran;

import com.pingmanager.core.ResultatPointage;
import com.pingmanager.core.ResultatVerificationLancement;
import com.pingmanager.core.ServiceLancementTableau;
import com.pingmanager.core.ServicePointage;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.StatutTableau;
import com.pingmanager.model.StatutTournoi;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.format.FormatTournoi;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Pointage d'un tableau le jour J, puis lancement du tirage au sort des poules.
 *
 * <ul>
 *   <li>liste des inscrits (triée par nom, recherche) : cases « Présent » et
 *       « Payé », désistement ;</li>
 *   <li>liste d'attente dans l'ordre de repêchage ;</li>
 *   <li>clôture du pointage ({@link ServicePointage#cloturerPointage}) : les non
 *       pointés libèrent leur place, la liste d'attente est repêchée ; les
 *       repêchés « à pointer » sont confirmés ou retirés un par un ;</li>
 *   <li>tirage au sort (IX.109) : graine proposée, modifiable, affichée, puis
 *       {@link ServiceLancementTableau#lancer(Tableau, long)}.</li>
 * </ul>
 *
 * <p>Toute la logique est dans le cœur et dans {@link PresentationJourJ} ; cet
 * écran ne fait qu'assembler les contrôles.</p>
 */
final class EcranPointageTableau {

    private final Tournoi tournoi;
    private final Tableau tableau;
    private final Runnable enregistrer;
    private final Runnable onRetour;
    private final Consumer<Tableau> versPoules;

    private final ServicePointage pointage;
    private final ServiceLancementTableau lancement;

    private final BorderPane racine = new BorderPane();
    private final ScrollPane defilement;
    private final VBox zoneResume = new VBox();
    private final VBox zoneCloture = new VBox();
    private final VBox zoneTirage = new VBox();
    private final VBox lignesInscrits = new VBox(0);
    private final Label message = Ui.message();

    private String recherche = "";
    private final String graineProposee = String.valueOf(PresentationJourJ.nouvelleGraine(new SecureRandom()));
    private String messageEnAttente;
    private boolean messageEnAttenteErreur;

    EcranPointageTableau(Tournoi tournoi, Tableau tableau, Runnable enregistrer, Runnable onRetour,
                         Consumer<Tableau> versPoules) {
        this.tournoi = tournoi;
        this.tableau = tableau;
        this.enregistrer = enregistrer;
        this.onRetour = onRetour;
        this.versPoules = versPoules;
        this.pointage = new ServicePointage(tournoi);
        this.lancement = new ServiceLancementTableau(tournoi);
        this.defilement = Ui.defilant(new VBox());
        racine.setCenter(defilement);
    }

    Node build() {
        afficher();
        return racine;
    }

    // ------------------------------------------------------------------
    // États du tableau
    // ------------------------------------------------------------------

    private boolean ferme() {
        return tournoi.getStatut() == StatutTournoi.ANNULE;
    }

    /** Pointage modifiable : ouvert, pas encore clôturé. */
    private boolean pointageOuvert() {
        return !ferme() && tableau.getStatut() == StatutTableau.POINTAGE && !tableau.pointageEstCloture();
    }

    /** Paiement modifiable : pendant le pointage et la phase de poules. */
    private boolean paiementModifiable() {
        return !ferme() && (tableau.getStatut() == StatutTableau.POINTAGE || tableau.getStatut() == StatutTableau.LANCE);
    }

    // ------------------------------------------------------------------
    // Construction de la page
    // ------------------------------------------------------------------

    private void afficher() {
        double position = defilement.getVvalue();

        Button retour = Ui.boutonLien("← Jour J");
        retour.setOnAction(e -> onRetour.run());
        PresentationJourJ.Etape etape = PresentationJourJ.etape(tableau);
        Label titre = Ui.titre("Pointage — " + tableau.getNom());
        titre.setWrapText(true);
        HBox ligneTitre = Ui.ligne(12, titre,
                Ui.pastille(PresentationJourJ.libelle(etape), PresentationJourJ.classe(etape)));
        FormatTournoi format = tableau.getConfiguration().getFormat();
        Label sousTitre = Ui.discret(PresentationJourJ.horaires(tableau)
                + (format == null ? "" : " · " + format.nom()));
        VBox entete = new VBox(6, retour, ligneTitre, sousTitre);

        VBox contenu = Ui.page(900, entete, zoneResume, message, carteInscrits(), carteAttente(), zoneCloture,
                zoneTirage);
        if (messageEnAttente != null) {
            if (messageEnAttenteErreur) {
                Ui.erreur(message, messageEnAttente);
            } else {
                Ui.succes(message, messageEnAttente);
            }
            messageEnAttente = null;
        }
        rafraichirResume();
        defilement.setContent(contenu);
        Platform.runLater(() -> defilement.setVvalue(position));
    }

    /** Met à jour les compteurs, la clôture et le tirage sans reconstruire la liste. */
    private void rafraichirResume() {
        PresentationJourJ.Comptes c = PresentationJourJ.comptes(tableau);
        FlowPane tuiles = new FlowPane(12, 12,
                Ui.tuile(c.inscrits() + (c.places() > 0 ? " / " + c.places() : ""), "inscrits"),
                Ui.tuile(String.valueOf(c.pointes()), c.pointes() > 1 ? "pointés" : "pointé"),
                Ui.tuile(String.valueOf(c.payes()), c.payes() > 1 ? "payés" : "payé"),
                Ui.tuile(String.valueOf(c.attente()), "en liste d'attente"));
        zoneResume.getChildren().setAll(tuiles);
        zoneCloture.getChildren().setAll(carteCloture());
        zoneTirage.getChildren().setAll(carteTirage());
    }

    /** Enregistre, puis reconstruit la page avec un message. */
    private void apresAction(String texte, boolean erreur) {
        enregistrer.run();
        messageEnAttente = texte;
        messageEnAttenteErreur = erreur;
        afficher();
    }

    // ------------------------------------------------------------------
    // Inscrits
    // ------------------------------------------------------------------

    private Node carteInscrits() {
        TextField champRecherche = Ui.champ("Rechercher un joueur (nom, licence, club)");
        champRecherche.setText(recherche);
        champRecherche.getStyleClass().add("champ-recherche");
        champRecherche.textProperty().addListener((o, a, n) -> {
            recherche = n == null ? "" : n;
            remplirInscrits();
        });
        HBox entete = Ui.enteteAvecActions(Ui.titreAvecCompteur("Inscrits", tableau.getInscrits().size()),
                champRecherche);
        HBox colonnes = Ui.ligne(12, colonne(Ui.enteteColonne("Joueur"), true),
                largeur(Ui.enteteColonne("Présent"), 90), largeur(Ui.enteteColonne("Payé"), 70),
                largeur(Ui.enteteColonne(""), 150));
        colonnes.getStyleClass().add("ligne-colonnes");
        remplirInscrits();
        String aide = pointageOuvert()
                ? "Cochez « Présent » quand le joueur pointe pour ce tableau, « Payé » quand il règle son engagement "
                        + "(le paiement en ligne est déjà coché)."
                : tableau.getStatut() == StatutTableau.INSCRIPTION
                        ? "Le pointage de ce tableau n'est pas encore ouvert."
                        : "Pointage clôturé : la présence n'est plus modifiable, sauf pour les repêchés à pointer.";
        return Ui.carte(entete, Ui.aide(aide), colonnes, lignesInscrits);
    }

    private void remplirInscrits() {
        lignesInscrits.getChildren().clear();
        List<Joueur> joueurs = PresentationJourJ.filtrer(PresentationJourJ.parNom(tableau.getInscrits()), recherche);
        if (joueurs.isEmpty()) {
            lignesInscrits.getChildren().add(Ui.discret(tableau.getInscrits().isEmpty()
                    ? "Aucun inscrit." : "Aucun joueur ne correspond à la recherche."));
            return;
        }
        for (Joueur j : joueurs) {
            lignesInscrits.getChildren().add(ligneInscrit(j));
        }
    }

    private Node ligneInscrit(Joueur j) {
        VBox bloc = new VBox(8);
        VBox identite = new VBox(2, Ui.gras(j.getNomComplet()),
                Ui.discret(j.getNumeroLicence() + " · " + PresentationJourJ.detailJoueur(j)));
        boolean aPointer = tableau.pointageEstCloture() && !tableau.aPointe(j)
                && tableau.getStatut() == StatutTableau.POINTAGE;
        if (aPointer) {
            identite.getChildren().set(0, Ui.ligne(8, Ui.gras(j.getNomComplet()),
                    Ui.pastille("Repêché · à pointer", Ui.ETAT_ATTENTION)));
        }
        HBox.setHgrow(identite, Priority.ALWAYS);
        identite.setMaxWidth(Double.MAX_VALUE);

        CheckBox present = new CheckBox("Présent");
        present.setSelected(tableau.aPointe(j));
        present.setDisable(!pointageOuvert());
        CheckBox paye = new CheckBox("Payé");
        paye.setSelected(tableau.aPaye(j));
        paye.setDisable(!paiementModifiable());

        HBox actions = Ui.ligne(8);
        HBox ligne = Ui.ligne(12, identite, largeur(present, 90), largeur(paye, 70), largeur(actions, 150));
        bloc.getChildren().add(ligne);
        bloc.getStyleClass().add("ligne-pointage");
        poserEtatLigne(bloc, j);

        present.selectedProperty().addListener((o, a, n) -> {
            if (!executer(() -> {
                if (n) {
                    pointage.pointer(tableau, j);
                } else {
                    pointage.depointer(tableau, j);
                }
            })) {
                return;
            }
            enregistrer.run();
            poserEtatLigne(bloc, j);
            rafraichirResume();
        });
        paye.selectedProperty().addListener((o, a, n) -> {
            if (!executer(() -> {
                if (n) {
                    pointage.marquerPaye(tableau, j);
                } else {
                    pointage.annulerPaiement(tableau, j);
                }
            })) {
                return;
            }
            enregistrer.run();
            rafraichirResume();
        });

        if (aPointer && !ferme()) {
            Button confirmer = Ui.boutonSecondaire("Confirmer");
            confirmer.setOnAction(e -> {
                if (executer(() -> pointage.confirmerRepeche(tableau, j))) {
                    apresAction(j.getNomComplet() + " est pointé pour ce tableau.", false);
                }
            });
            Button retirer = Ui.boutonLien("Retirer");
            retirer.getStyleClass().add("lien-danger");
            retirer.setOnAction(e -> confirmerRetrait(bloc, j));
            actions.getChildren().addAll(confirmer, retirer);
        } else if (tableau.getStatut() == StatutTableau.POINTAGE && !ferme()) {
            Button desister = Ui.boutonLien("Désistement");
            desister.getStyleClass().add("lien-danger");
            desister.setOnAction(e -> confirmerDesistement(bloc, j));
            actions.getChildren().add(desister);
        }
        return bloc;
    }

    private void poserEtatLigne(VBox bloc, Joueur j) {
        bloc.getStyleClass().removeAll("ligne-pointee", "ligne-a-pointer");
        if (tableau.aPointe(j)) {
            bloc.getStyleClass().add("ligne-pointee");
        } else if (tableau.pointageEstCloture() && tableau.getStatut() == StatutTableau.POINTAGE) {
            bloc.getStyleClass().add("ligne-a-pointer");
        }
    }

    private void confirmerDesistement(VBox bloc, Joueur j) {
        if (dejaUneConfirmation(bloc)) {
            return;
        }
        Button renoncer = Ui.boutonSecondaire("Annuler");
        VBox encart;
        if (!tableau.pointageEstCloture()) {
            Button confirmer = Ui.boutonPrimaire("Confirmer le désistement");
            String suite = tableau.getFileAttente().isEmpty() ? "La liste d'attente est vide : la place reste libre."
                    : "Le premier de la liste d'attente (" + tableau.getFileAttente().get(0).getNomComplet()
                            + ") est repêché à sa place ; il devra pointer.";
            encart = Ui.encartConfirmation(j.getNomComplet() + " se désiste de ce tableau : sa place est libérée. "
                    + suite, confirmer, renoncer);
            confirmer.setOnAction(e -> {
                Joueur[] repeche = new Joueur[1];
                if (executer(() -> repeche[0] = pointage.desister(tableau, j))) {
                    apresAction(j.getNomComplet() + " s'est désisté."
                            + (repeche[0] == null ? "" : " " + repeche[0].getNomComplet() + " est repêché (à pointer)."),
                            false);
                }
            });
        } else {
            // Après clôture : le joueur est tenu de jouer ; seul un cas particulier (blessure…) est recevable.
            Button confirmer = Ui.boutonPrimaire("Retirer pour cas particulier");
            CheckBox rembourser = new CheckBox("Rembourser l'engagement");
            encart = Ui.encartConfirmation("Pointage clôturé : le joueur est tenu de jouer. Un retrait n'est possible "
                    + "que pour un cas particulier (blessure…), sur décision du juge-arbitre. Aucun repêchage "
                    + "automatique n'est fait.", confirmer, renoncer);
            encart.getChildren().add(1, rembourser);
            confirmer.setOnAction(e -> {
                if (executer(() -> pointage.desisterCasParticulier(tableau, j, rembourser.isSelected()))) {
                    apresAction(j.getNomComplet() + " est retiré du tableau"
                            + (rembourser.isSelected() ? " (remboursement noté)." : "."), false);
                }
            });
        }
        renoncer.setOnAction(e -> bloc.getChildren().remove(encart));
        bloc.getChildren().add(encart);
    }

    private void confirmerRetrait(VBox bloc, Joueur j) {
        if (dejaUneConfirmation(bloc)) {
            return;
        }
        Button confirmer = Ui.boutonPrimaire("Retirer " + j.getNomComplet());
        Button renoncer = Ui.boutonSecondaire("Annuler");
        VBox encart = Ui.encartConfirmation(j.getNomComplet() + " ne s'est pas présenté : il est retiré du tableau"
                + (tableau.getFileAttente().isEmpty() ? "." : " et le suivant de la liste d'attente est repêché."),
                confirmer, renoncer);
        confirmer.setOnAction(e -> {
            Joueur[] suivant = new Joueur[1];
            if (executer(() -> suivant[0] = pointage.retirerRepeche(tableau, j))) {
                apresAction(j.getNomComplet() + " est retiré."
                        + (suivant[0] == null ? "" : " " + suivant[0].getNomComplet() + " est repêché (à pointer)."),
                        false);
            }
        });
        renoncer.setOnAction(e -> bloc.getChildren().remove(encart));
        bloc.getChildren().add(encart);
    }

    private static boolean dejaUneConfirmation(VBox bloc) {
        return bloc.getChildren().stream().anyMatch(n -> n.getStyleClass().contains("encart-confirmation"));
    }

    // ------------------------------------------------------------------
    // Liste d'attente
    // ------------------------------------------------------------------

    private Node carteAttente() {
        List<Joueur> attente = tableau.getFileAttente();
        VBox lignes = new VBox(0);
        if (attente.isEmpty()) {
            lignes.getChildren().add(Ui.discret("Personne en liste d'attente."));
        }
        for (int i = 0; i < attente.size(); i++) {
            Joueur j = attente.get(i);
            Label rang = Ui.badge(String.valueOf(i + 1));
            VBox identite = new VBox(2, Ui.gras(j.getNomComplet()),
                    Ui.discret(j.getNumeroLicence() + " · " + PresentationJourJ.detailJoueur(j)));
            lignes.getChildren().add(Ui.ligneListe(rang, identite));
        }
        return Ui.carte(Ui.titreAvecCompteur("Liste d'attente", attente.size()),
                Ui.aide("Dans l'ordre de repêchage : le premier prend la place d'un désisté, et la liste est repêchée "
                        + "à la clôture tant qu'il reste de la place. Un joueur en attente ne paie que s'il est repêché."),
                lignes);
    }

    // ------------------------------------------------------------------
    // Clôture
    // ------------------------------------------------------------------

    private Node carteCloture() {
        if (tableau.getStatut() == StatutTableau.INSCRIPTION) {
            return new VBox();
        }
        if (tableau.pointageEstCloture()) {
            int aPointer = ServicePointage.nonPointes(tableau).size();
            String texte = tableau.getStatut() == StatutTableau.POINTAGE && aPointer > 0
                    ? "Pointage clôturé. " + aPointer + (aPointer > 1 ? " repêchés restent" : " repêché reste")
                            + " à pointer : confirmez-les quand ils se présentent, ou retirez-les."
                    : "Pointage clôturé : " + tableau.getInscrits().size() + " présents.";
            return Ui.carte("Clôture du pointage", Ui.discret(texte));
        }
        VBox corps = new VBox(10, Ui.discret(PresentationJourJ.texteCloture(tableau)));
        Button cloturer = Ui.boutonPrimaire("Clôturer le pointage…");
        cloturer.setDisable(!pointageOuvert());
        cloturer.setOnAction(e -> {
            if (dejaUneConfirmation(corps)) {
                return;
            }
            Button confirmer = Ui.boutonPrimaire("Clôturer maintenant");
            Button renoncer = Ui.boutonSecondaire("Annuler");
            VBox encart = Ui.encartConfirmation("La clôture fige la liste des présents de ce tableau. "
                    + PresentationJourJ.texteCloture(tableau) + " Elle ne se défait pas.", confirmer, renoncer);
            confirmer.setOnAction(ev -> {
                ResultatPointage[] r = new ResultatPointage[1];
                if (executer(() -> r[0] = pointage.cloturerPointage(tableau))) {
                    apresAction(PresentationJourJ.compteRenduCloture(r[0]), false);
                }
            });
            renoncer.setOnAction(ev -> corps.getChildren().remove(encart));
            corps.getChildren().add(encart);
        });
        corps.getChildren().add(Ui.actions(cloturer));
        return Ui.carte("Clôture du pointage", corps);
    }

    // ------------------------------------------------------------------
    // Tirage au sort et lancement
    // ------------------------------------------------------------------

    private Node carteTirage() {
        // Tirage au sort : étape suivante.
        return new VBox();
    }

    // ------------------------------------------------------------------
    // Utilitaires
    // ------------------------------------------------------------------

    /** Exécute une action du cœur ; un refus est affiché en message d'erreur. */
    private boolean executer(Runnable action) {
        try {
            action.run();
            Ui.cacher(message);
            return true;
        } catch (IllegalStateException | IllegalArgumentException refus) {
            Ui.erreur(message, refus.getMessage());
            return false;
        }
    }

    private static Node largeur(Region r, double l) {
        r.setMinWidth(l);
        r.setPrefWidth(l);
        r.setMaxWidth(l);
        return r;
    }

    private static Node colonne(Region r, boolean extensible) {
        if (extensible) {
            HBox.setHgrow(r, Priority.ALWAYS);
            r.setMaxWidth(Double.MAX_VALUE);
        }
        return r;
    }
}
