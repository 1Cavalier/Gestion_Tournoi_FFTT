package com.pingmanager.ui.infra;

import com.pingmanager.core.EnvoiEmail;

import java.time.LocalDateTime;
import java.util.function.Consumer;

/**
 * Implémentation de démonstration du port {@link EnvoiEmail} : au lieu d'envoyer
 * un vrai email, elle <b>affiche le message dans la console</b> et le retient.
 *
 * <p>Indispensable pour les tests manuels : le code de rattachement est censé
 * partir vers l'adresse officielle du club (le référent) ; ici on le voit
 * directement, sans serveur mail. Un {@link #setObservateur(Consumer) observateur}
 * optionnel permet en plus à l'interface d'afficher ce message dans un panneau
 * « démo », pour ne pas avoir à surveiller le terminal.</p>
 *
 * <p>Cette classe n'a évidemment pas vocation à la production : le jour où un
 * vrai envoi (SMTP, service tiers) est branché, on remplace cette
 * implémentation, sans toucher au cœur.</p>
 */
public final class EmailConsole implements EnvoiEmail {

    /** Un message « envoyé » (retenu pour l'affichage). */
    public static final class Message {
        private final String destinataire;
        private final String sujet;
        private final String corps;
        private final LocalDateTime instant;

        Message(String destinataire, String sujet, String corps, LocalDateTime instant) {
            this.destinataire = destinataire;
            this.sujet = sujet;
            this.corps = corps;
            this.instant = instant;
        }

        public String getDestinataire() {
            return destinataire;
        }

        public String getSujet() {
            return sujet;
        }

        public String getCorps() {
            return corps;
        }

        public LocalDateTime getInstant() {
            return instant;
        }
    }

    private Message dernierMessage;
    private Consumer<Message> observateur;

    @Override
    public void envoyer(String destinataire, String sujet, String corps) {
        Message message = new Message(destinataire, sujet, corps, LocalDateTime.now());
        this.dernierMessage = message;

        System.out.println();
        System.out.println("================ EMAIL (démo) ================");
        System.out.println("À      : " + destinataire);
        System.out.println("Objet  : " + sujet);
        System.out.println("----------------------------------------------");
        System.out.println(corps);
        System.out.println("==============================================");
        System.out.println();

        if (observateur != null) {
            observateur.accept(message);
        }
    }

    /** Branche (ou débranche avec {@code null}) un observateur des messages, pour l'UI. */
    public void setObservateur(Consumer<Message> observateur) {
        this.observateur = observateur;
    }
}
