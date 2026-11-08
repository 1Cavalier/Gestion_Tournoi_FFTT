package com.pingmanager.ui.diffusion;

import com.pingmanager.core.Horloge;
import com.pingmanager.model.Tournoi;

import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Cycle de vie du suivi sur téléphone (B021) pour l'application : démarrage
 * depuis le jour J sur l'adresse du réseau local, publication d'un nouvel
 * instantané après chaque action du juge-arbitre, arrêt à la sortie du jour J
 * et à la fermeture de l'application. Sans JavaFX ; toutes les méthodes sont
 * appelées depuis le fil JavaFX (celui qui possède le tournoi).
 */
public final class DiffusionSuivi {

    /** Port proposé par défaut. */
    public static final int PORT_PAR_DEFAUT = 8080;
    /** Bornes du port configurable (pas de port privilégié). */
    public static final int PORT_MIN = 1024;
    public static final int PORT_MAX = 65535;

    /** Issue d'une demande de démarrage. Pas de {@code switch} sur cette énumération. */
    public enum Motif {
        DEMARRE,
        DEJA_DEMARRE,
        PORT_INVALIDE,
        PORT_OCCUPE,
        PAS_DE_RESEAU_LOCAL,
        ERREUR
    }

    /** Résultat du démarrage : motif, adresse à afficher (vide si refus) et message pour le juge-arbitre. */
    public record ResultatDemarrage(Motif motif, String url, String message) {
        public boolean estDemarre() {
            return motif == Motif.DEMARRE || motif == Motif.DEJA_DEMARRE;
        }
    }

    private final Horloge horloge;
    private final Supplier<Optional<InetAddress>> reseau;
    private ServeurSuiviPublic serveur;
    private int port = PORT_PAR_DEFAUT;

    public DiffusionSuivi() {
        this(Horloge.systeme(), AdresseReseauLocal::detecter);
    }

    /** Pour les tests : horloge et détection du réseau local imposées. */
    public DiffusionSuivi(Horloge horloge, Supplier<Optional<InetAddress>> reseau) {
        this.horloge = Objects.requireNonNull(horloge, "horloge");
        this.reseau = Objects.requireNonNull(reseau, "reseau");
    }

    /** @return le dernier port choisi (proposé dans le champ de l'écran). */
    public int port() {
        return port;
    }

    /** @return true si le serveur tourne. */
    public boolean estDemarre() {
        return serveur != null && serveur.estActif();
    }

    /** @return l'adresse à afficher, ou vide si le serveur est arrêté. */
    public String url() {
        return estDemarre() ? serveur.url() : "";
    }

    /** Lit un port saisi : entier entre {@link #PORT_MIN} et {@link #PORT_MAX}, sinon -1. */
    public static int lirePort(String saisie) {
        if (saisie == null) {
            return -1;
        }
        String s = saisie.strip();
        if (s.isEmpty() || s.length() > 5 || !s.chars().allMatch(c -> c >= '0' && c <= '9')) {
            return -1;
        }
        int p = Integer.parseInt(s);
        return p < PORT_MIN || p > PORT_MAX ? -1 : p;
    }

    /** Démarre le serveur sur le port donné et publie aussitôt le tournoi. */
    public ResultatDemarrage demarrer(int portDemande, Tournoi tournoi) {
        if (estDemarre()) {
            return new ResultatDemarrage(Motif.DEJA_DEMARRE, serveur.url(), "Le suivi sur téléphone est déjà ouvert.");
        }
        if (portDemande < PORT_MIN || portDemande > PORT_MAX) {
            return new ResultatDemarrage(Motif.PORT_INVALIDE, "",
                    "Port invalide : choisissez un nombre entre " + PORT_MIN + " et " + PORT_MAX + ".");
        }
        Optional<InetAddress> adresse = reseau.get();
        if (adresse.isEmpty()) {
            return new ResultatDemarrage(Motif.PAS_DE_RESEAU_LOCAL, "",
                    "Aucun réseau local privé détecté (adresse 192.168.x.x, 10.x.x.x ou 172.16-31.x.x) : "
                    + "reliez l'ordinateur au Wi-Fi de la salle, puis réessayez. Le suivi n'est jamais ouvert sur Internet.");
        }
        port = portDemande;
        try {
            serveur = ServeurSuiviPublic.demarrer(adresse.get(), portDemande,
                    tournoi == null ? null : InstantaneSuivi.depuis(tournoi, horloge));
        } catch (BindException occupe) {
            return new ResultatDemarrage(Motif.PORT_OCCUPE, "",
                    "Le port " + portDemande + " est déjà utilisé : choisissez-en un autre.");
        } catch (IOException | RuntimeException e) {
            return new ResultatDemarrage(Motif.ERREUR, "", "Le suivi sur téléphone n'a pas pu démarrer.");
        }
        return new ResultatDemarrage(Motif.DEMARRE, serveur.url(),
                "Suivi ouvert : les téléphones reliés au même Wi-Fi ouvrent " + serveur.url());
    }

    /** Publie une nouvelle photographie du tournoi si le serveur tourne (fil JavaFX). */
    public void publier(Tournoi tournoi) {
        if (estDemarre() && tournoi != null) {
            try {
                serveur.publier(InstantaneSuivi.depuis(tournoi, horloge));
            } catch (RuntimeException e) {
                // Le suivi ne doit jamais gêner le juge-arbitre : la photographie précédente reste servie.
            }
        }
    }

    /** Arrête le serveur s'il tourne. */
    public void arreter() {
        if (serveur != null) {
            serveur.arreter();
            serveur = null;
        }
    }
}
