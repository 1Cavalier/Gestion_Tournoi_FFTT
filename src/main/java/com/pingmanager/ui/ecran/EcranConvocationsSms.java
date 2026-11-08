package com.pingmanager.ui.ecran;

import com.pingmanager.core.ExpediteurSms;
import com.pingmanager.core.Horloge;
import com.pingmanager.core.ResultatContactSms;
import com.pingmanager.core.ServiceConvocationsSms;
import com.pingmanager.model.ContactSms;
import com.pingmanager.model.ConvocationsSms;
import com.pingmanager.model.Match;
import com.pingmanager.model.MessageSms;
import com.pingmanager.model.Tournoi;
import com.pingmanager.ui.infra.EnvoiSmsSimule;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Convocations par SMS (B019) : réglages, numéros de portable avec
 * consentement explicite, envoi manuel, journal des envois et boîte d'envoi
 * simulée. <b>Jamais d'appel</b>, des messages écrits seulement.
 *
 * <p>Vue mince : textes, ordre et états dans {@link PresentationConvocationsSms}
 * et {@link PresentationSaisieSms} ; chaque action enregistre aussitôt le tournoi.</p>
 */
final class EcranConvocationsSms {

    private final Tournoi tournoi;
    private final ServiceConvocationsSms service;
    private final EnvoiSmsSimule simulation;
    private final Runnable enregistrer;
    private final Runnable onRetour;
    private final Horloge horloge;
    private final BorderPane racine = new BorderPane();

    private String messageSucces;
    private String messageErreur;
    private boolean effacementAConfirmer;

    EcranConvocationsSms(Tournoi tournoi, ServiceConvocationsSms service, EnvoiSmsSimule simulation,
                         Runnable enregistrer, Runnable onRetour, Horloge horloge) {
        this.tournoi = tournoi;
        this.service = service;
        this.simulation = simulation;
        this.enregistrer = enregistrer;
        this.onRetour = onRetour;
        this.horloge = horloge;
    }

    Node build() {
        // Fin du tournoi passée : les numéros n'ont plus lieu d'être gardés.
        int effaces = service.effacerSiTermine(tournoi, maintenant());
        if (effaces > 0) {
            sauver(true, effaces + (effaces > 1 ? " numéros effacés" : " numéro effacé")
                    + " : le tournoi est terminé.");
        }
        afficher();
        return racine;
    }

    private LocalDateTime maintenant() {
        return LocalDateTime.ofInstant(horloge.maintenant(), ZoneId.systemDefault());
    }

    private void afficher() {
        Button retour = Ui.boutonLien("← Jour J");
        retour.setOnAction(e -> onRetour.run());
        VBox page = Ui.page(960, retour, Ui.titre("Convocations par SMS"),
                Ui.aide("Messages écrits seulement, jamais d'appel. Simulation : aucun SMS ne part réellement, "
                        + "les messages s'affichent en bas de page."));
        if (messageSucces != null || messageErreur != null) {
            page.getChildren().add(Ui.message(messageErreur, messageSucces));
            messageSucces = null;
            messageErreur = null;
        }
        ConvocationsSms c = tournoi.getConvocationsSms();
        List<Node> tuiles = new ArrayList<>();
        for (PresentationConvocationsSms.Tuile t : PresentationConvocationsSms.tuiles(c)) {
            tuiles.add(Ui.tuileChiffre(t.valeur(), t.libelle()));
        }
        page.getChildren().add(Ui.rangeeTuiles(tuiles.toArray(new Node[0])));
        PresentationConvocationsSms.alerteLimite(c).ifPresent(a -> {
            Label l = Ui.aide(a);
            l.getStyleClass().add("sms-alerte-limite");
            page.getChildren().add(l);
        });
        List<PresentationConvocationsSms.Personne> personnes = PresentationConvocationsSms.personnes(tournoi);
        page.getChildren().addAll(carteReglages(), carteNumeros(personnes), carteEnvoiManuel(personnes),
                carteJournal(), carteSimulation());
        racine.setCenter(Ui.defilant(page));
    }

    // ------------------------------------------------------------------
    // Réglages
    // ------------------------------------------------------------------

    private Node carteReglages() {
        ConvocationsSms c = tournoi.getConvocationsSms();
        CheckBox auto = new CheckBox("Envoyer les SMS automatiquement quand un match est envoyé sur une table");
        auto.setSelected(c.isEnvoiAutomatique());
        Label aideAuto = Ui.aide(PresentationConvocationsSms.aideEnvoiAutomatique(tournoi));
        TextField expediteur = Ui.champ(ExpediteurSms.proposer(tournoi.getClubOrganisateur()));
        expediteur.setText(c.getExpediteur() == null ? "" : c.getExpediteur());
        TextField limite = Ui.champ(String.valueOf(ConvocationsSms.LIMITE_PAR_DEFAUT));
        limite.setText(String.valueOf(c.getLimiteEnvois()));
        limite.setPrefColumnCount(6);
        Button valider = Ui.boutonPrimaire("Enregistrer les réglages");
        valider.setOnAction(e -> {
            java.util.Optional<Integer> l = PresentationConvocationsSms.limite(limite.getText());
            if (l.isEmpty()) {
                messageErreur = PresentationConvocationsSms.limiteRefusee();
                afficher();
                return;
            }
            if (!service.definirExpediteur(tournoi, expediteur.getText())) {
                messageErreur = PresentationConvocationsSms.EXPEDITEUR_REFUSE;
                afficher();
                return;
            }
            c.setLimiteEnvois(l.get());
            c.setEnvoiAutomatique(auto.isSelected());
            sauver(true, "Réglages des SMS enregistrés.");
            afficher();
        });
        expediteur.setMaxWidth(220);
        limite.setMaxWidth(120);
        VBox colExp = new VBox(6, Ui.libelleChamp("Expéditeur (nom affiché sur le téléphone)"), expediteur,
                Ui.aide("Laisser vide pour « " + ExpediteurSms.proposer(tournoi.getClubOrganisateur())
                        + " », tiré du nom du club."));
        VBox colLim = new VBox(6, Ui.libelleChamp("Limite d'envois pour le tournoi"), limite,
                Ui.aide("Au-delà, rien ne part et le refus est noté au journal."));
        HBox.setHgrow(colExp, javafx.scene.layout.Priority.ALWAYS);
        HBox.setHgrow(colLim, javafx.scene.layout.Priority.ALWAYS);
        HBox champs = new HBox(24, colExp, colLim);
        champs.getStyleClass().add("sms-reglages-champs");
        return Ui.carte("Réglages", new VBox(4, auto, aideAuto), champs, Ui.actions(valider));
    }

    // ------------------------------------------------------------------
    // Numéros et consentement
    // ------------------------------------------------------------------

    private Node carteNumeros(List<PresentationConvocationsSms.Personne> personnes) {
        ConvocationsSms c = tournoi.getConvocationsSms();
        ComboBox<PresentationConvocationsSms.Personne> qui = new ComboBox<>();
        qui.getItems().setAll(personnes);
        qui.setPromptText("Joueur ou arbitre");
        qui.setMaxWidth(420);
        BlocSaisieSms saisie = new BlocSaisieSms(PresentationSaisieSms.CONSENTEMENT_CLUB, null);
        saisie.montrerEtat(tournoi, null);
        qui.valueProperty().addListener((o, a, p) -> saisie.montrerEtat(tournoi, p == null ? null : p.licence()));
        Button ajouter = Ui.boutonPrimaire("Enregistrer le numéro");
        ajouter.setOnAction(e -> {
            PresentationConvocationsSms.Personne p = qui.getValue();
            ResultatContactSms r = service.enregistrerContact(tournoi, p == null ? null : p.licence(),
                    saisie.saisie(), saisie.consenti(), maintenant());
            if (r.estReussi()) {
                sauver(true, r.message());
            } else {
                messageErreur = r.message();
            }
            afficher();
        });
        VBox liste = new VBox(4);
        if (c.getContacts().isEmpty()) {
            liste.getChildren().add(Ui.etatVideCompact(c.getNumerosEffacesLe() == null
                    ? "Aucun numéro enregistré." : "Numéros effacés le " + c.getNumerosEffacesLe().toLocalDate()
                    + "."));
        }
        for (ContactSms k : new ArrayList<>(c.getContacts())) {
            Button retirer = Ui.boutonLien("Retirer");
            retirer.setOnAction(e -> {
                service.retirerContact(tournoi, k.numeroLicence());
                sauver(true, "Numéro retiré.");
                afficher();
            });
            HBox ligne = Ui.ligneElement(null, PresentationConvocationsSms.nomDe(personnes, k.numeroLicence()),
                    PresentationConvocationsSms.detailContact(k), retirer);
            if (liste.getChildren().isEmpty()) {
                ligne.getStyleClass().add("ligne-element-premiere");
            }
            liste.getChildren().add(ligne);
        }
        Button effacer = Ui.boutonSecondaire(effacementAConfirmer ? "Oui, effacer" : "Effacer tous les numéros");
        effacer.setDisable(c.getContacts().isEmpty());
        effacer.setOnAction(e -> {
            if (!effacementAConfirmer) {
                effacementAConfirmer = true;
            } else {
                effacementAConfirmer = false;
                int n = service.effacerNumeros(tournoi, maintenant());
                sauver(true, n + (n > 1 ? " numéros effacés." : " numéro effacé."));
            }
            afficher();
        });
        HBox pied = Ui.actions(effacer);
        if (effacementAConfirmer && !c.getContacts().isEmpty()) {
            effacer.getStyleClass().add("bouton-danger-sms");
            Button annuler = Ui.boutonLien("Annuler");
            annuler.setOnAction(e -> {
                effacementAConfirmer = false;
                afficher();
            });
            Label question = Ui.texte(PresentationConvocationsSms.confirmationEffacement(c.getContacts().size()));
            question.setWrapText(true);
            question.getStyleClass().add("sms-confirmation-effacement");
            pied = new HBox(10, question, Ui.espace(), annuler, effacer);
            pied.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        }
        return Ui.carte("Numéros de portable (facultatifs)",
                Ui.libelleChamp("Joueur ou arbitre"), qui, saisie.noeud(), Ui.actions(ajouter),
                Ui.filet(), liste, pied);
    }

    // ------------------------------------------------------------------
    // Envoi manuel
    // ------------------------------------------------------------------

    private Node carteEnvoiManuel(List<PresentationConvocationsSms.Personne> personnes) {
        ComboBox<PresentationConvocationsSms.Personne> arbitre = new ComboBox<>();
        arbitre.getItems().setAll(personnes);
        arbitre.setPromptText("Arbitre à convoquer");
        VBox lignes = new VBox(4);
        List<Match> matchs = PresentationConvocationsSms.matchsSurTable(tournoi);
        if (matchs.isEmpty()) {
            lignes.getChildren().add(Ui.etatVideCompact("Aucun match sur une table pour l'instant."));
        }
        for (Match m : matchs) {
            Button joueurs = Ui.boutonSecondaire("SMS aux joueurs");
            joueurs.setOnAction(e -> {
                String r = service.convoquerJoueurs(tournoi, m, m.getNumeroTable(), maintenant()).resume();
                sauver(true, r);
                afficher();
            });
            Button arb = Ui.boutonSecondaire("SMS à l'arbitre");
            arb.setOnAction(e -> {
                PresentationConvocationsSms.Personne p = arbitre.getValue();
                if (p == null) {
                    messageErreur = "Choisissez d'abord l'arbitre à convoquer.";
                    afficher();
                    return;
                }
                String r = service.convoquerArbitre(tournoi, p.licence(), p.prenom(), p.nomComplet(), m,
                        m.getNumeroTable(), maintenant()).resume();
                sauver(true, r);
                afficher();
            });
            lignes.getChildren().add(Ui.ligne(10, Ui.texte(PresentationConvocationsSms.libelleMatch(m)), Ui.espace(),
                    joueurs, arb));
        }
        return Ui.carte("Envoi manuel", arbitre, lignes);
    }

    // ------------------------------------------------------------------
    // Journal et simulation
    // ------------------------------------------------------------------

    private Node carteJournal() {
        ConvocationsSms c = tournoi.getConvocationsSms();
        VBox lignes = new VBox();
        List<MessageSms> journal = PresentationConvocationsSms.journalAffiche(c);
        if (journal.isEmpty()) {
            lignes.getChildren().add(Ui.etatVideCompact("Aucun envoi pour l'instant. Les convocations envoyées "
                    + "s'afficheront ici, la plus récente en haut."));
        }
        for (MessageSms m : journal) {
            Label statut = Ui.pastille(m.statut().getLibelle(), PresentationConvocationsSms.pastille(m.statut()));
            HBox entete = Ui.ligneElement(null, PresentationConvocationsSms.destinataire(m),
                    PresentationConvocationsSms.detailMessage(m), statut);
            entete.getStyleClass().addAll("sms-journal-entete", "ligne-element-premiere");
            Label texte = Ui.texte(m.texte());
            texte.setWrapText(true);
            texte.getStyleClass().add("sms-texte");
            VBox ligne = new VBox(4, entete, texte);
            PresentationConvocationsSms.motif(m).ifPresent(mo -> ligne.getChildren().add(Ui.aide(mo)));
            ligne.getStyleClass().add("sms-journal-ligne");
            if (lignes.getChildren().isEmpty()) {
                ligne.getStyleClass().add("sms-journal-premiere");
            }
            lignes.getChildren().add(ligne);
        }
        PresentationConvocationsSms.resteJournal(c).ifPresent(r -> lignes.getChildren().add(Ui.aide(r)));
        return Ui.carte("Journal des envois", Ui.discret(PresentationConvocationsSms.compteur(c)), lignes);
    }

    private Node carteSimulation() {
        VBox lignes = new VBox(10);
        List<EnvoiSmsSimule.Message> messages = simulation.messages();
        if (messages.isEmpty()) {
            lignes.getChildren().add(Ui.etatVideCompact("Aucun SMS simulé depuis l'ouverture du logiciel."));
        }
        for (int i = messages.size() - 1; i >= 0 && messages.size() - i <= PresentationConvocationsSms.JOURNAL_AFFICHE;
             i--) {
            EnvoiSmsSimule.Message m = messages.get(i);
            Label texte = Ui.texte(m.texte());
            texte.setWrapText(true);
            texte.getStyleClass().add("sms-bulle-texte");
            Label meta = Ui.discret(PresentationConvocationsSms.enteteSimule(m) + " · "
                    + PresentationConvocationsSms.tailleTexte(m.texte()));
            VBox bulle = new VBox(4, meta, texte);
            bulle.getStyleClass().add("sms-bulle");
            bulle.setMaxWidth(560);
            lignes.getChildren().add(bulle);
        }
        Label titre = Ui.titreCarte("SMS simulés");
        Label badge = Ui.pastille("Aucun envoi réel", StatutPastille.INFO);
        return Ui.carte(Ui.enteteAvecActions(titre, badge),
                Ui.aide("Ce que les téléphones auraient reçu depuis l'ouverture du logiciel (non sauvegardé)."),
                lignes);
    }

    private void sauver(boolean reussi, String texte) {
        try {
            enregistrer.run();
            if (reussi) {
                messageSucces = texte;
            }
        } catch (RuntimeException ex) {
            messageErreur = "Attention : l'enregistrement a échoué (" + ex.getMessage() + ").";
        }
    }
}
