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
 * <p>Écran minimal fonctionnel ; la mise en forme fine viendra avec l'agent
 * {@code developpeur-interface}.</p>
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
        String annonce = tournoi.getModeAnnonce() == null ? "demandée à chaque lancement"
                : "« " + tournoi.getModeAnnonce().getLibelle() + " »";
        Label aideAuto = Ui.aide("Seulement si l'annonce choisie comprend le SMS (« SMS » ou « SMS et appel vocal »). "
                + "Annonce actuelle : " + annonce + ".");
        TextField expediteur = Ui.champ(ExpediteurSms.proposer(tournoi.getClubOrganisateur()));
        expediteur.setText(c.getExpediteur() == null ? "" : c.getExpediteur());
        TextField limite = Ui.champ(String.valueOf(ConvocationsSms.LIMITE_PAR_DEFAUT));
        limite.setText(String.valueOf(c.getLimiteEnvois()));
        limite.setPrefColumnCount(6);
        Button valider = Ui.boutonPrimaire("Enregistrer les réglages");
        valider.setOnAction(e -> {
            int l;
            try {
                l = Integer.parseInt(limite.getText().trim());
            } catch (NumberFormatException ex) {
                l = -1;
            }
            if (l < 0 || l > ConvocationsSms.LIMITE_MAXIMALE) {
                messageErreur = "Limite d'envois : un nombre entre 0 et " + ConvocationsSms.LIMITE_MAXIMALE + ".";
                afficher();
                return;
            }
            if (!service.definirExpediteur(tournoi, expediteur.getText())) {
                messageErreur = "Expéditeur : 3 à 11 lettres ou chiffres, sans espace ni accent, avec au moins une "
                        + "lettre (le nom du club de préférence).";
                afficher();
                return;
            }
            c.setLimiteEnvois(l);
            c.setEnvoiAutomatique(auto.isSelected());
            sauver(true, "Réglages des SMS enregistrés.");
            afficher();
        });
        return Ui.carte("Réglages", auto, aideAuto,
                Ui.libelleChamp("Expéditeur (nom affiché sur le téléphone)"), expediteur,
                Ui.aide("Laisser vide pour « " + ExpediteurSms.proposer(tournoi.getClubOrganisateur())
                        + " », tiré du nom du club."),
                Ui.libelleChamp("Limite d'envois pour le tournoi"), limite, Ui.actions(valider));
    }

    // ------------------------------------------------------------------
    // Numéros et consentement
    // ------------------------------------------------------------------

    private Node carteNumeros(List<PresentationConvocationsSms.Personne> personnes) {
        ConvocationsSms c = tournoi.getConvocationsSms();
        ComboBox<PresentationConvocationsSms.Personne> qui = new ComboBox<>();
        qui.getItems().setAll(personnes);
        qui.setPromptText("Joueur ou arbitre");
        TextField numero = Ui.champ("06 12 34 56 78");
        CheckBox consentement = new CheckBox("La personne accepte de recevoir ses convocations par SMS pour ce "
                + "tournoi. Son numéro sert uniquement à cela et sera effacé à la fin du tournoi.");
        consentement.setWrapText(true);
        Button ajouter = Ui.boutonPrimaire("Enregistrer le numéro");
        ajouter.setOnAction(e -> {
            PresentationConvocationsSms.Personne p = qui.getValue();
            ResultatContactSms r = service.enregistrerContact(tournoi, p == null ? null : p.licence(),
                    numero.getText(), consentement.isSelected(), maintenant());
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
            HBox ligne = Ui.ligne(10, Ui.gras(PresentationConvocationsSms.nomDe(personnes, k.numeroLicence())),
                    Ui.discret(PresentationConvocationsSms.detailContact(k)), Ui.espace(), retirer);
            liste.getChildren().add(ligne);
        }
        Button effacer = Ui.boutonSecondaire(effacementAConfirmer ? "Confirmer l'effacement de tous les numéros"
                : "Effacer tous les numéros");
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
        return Ui.carte("Numéros de portable (facultatifs)", qui, numero, consentement, Ui.actions(ajouter),
                Ui.filet(), liste, Ui.actions(effacer));
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
        VBox lignes = new VBox(6);
        List<MessageSms> journal = new ArrayList<>(c.getJournal());
        if (journal.isEmpty()) {
            lignes.getChildren().add(Ui.etatVideCompact("Aucun envoi pour l'instant."));
        }
        for (int i = journal.size() - 1; i >= 0; i--) {
            MessageSms m = journal.get(i);
            Label texte = Ui.discret(m.texte());
            texte.setWrapText(true);
            lignes.getChildren().add(new VBox(2, Ui.gras(PresentationConvocationsSms.enteteMessage(m)), texte));
        }
        return Ui.carte("Journal des envois", Ui.discret(PresentationConvocationsSms.compteur(c)), lignes);
    }

    private Node carteSimulation() {
        VBox lignes = new VBox(6);
        List<EnvoiSmsSimule.Message> messages = simulation.messages();
        if (messages.isEmpty()) {
            lignes.getChildren().add(Ui.etatVideCompact("Aucun SMS simulé depuis l'ouverture du logiciel."));
        }
        for (int i = messages.size() - 1; i >= 0; i--) {
            EnvoiSmsSimule.Message m = messages.get(i);
            Label texte = Ui.texte(m.texte());
            texte.setWrapText(true);
            lignes.getChildren().add(new VBox(2, Ui.discret("De " + m.expediteur() + " à " + m.numeroMasque()), texte));
        }
        return Ui.carte("SMS simulés (aucun envoi réel)", lignes);
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
