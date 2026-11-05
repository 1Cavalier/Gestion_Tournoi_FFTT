package com.pingmanager.core;

import com.pingmanager.model.Joueur;
import com.pingmanager.model.Match;
import com.pingmanager.model.ModeAnnonce;

import java.util.Objects;

/**
 * Annonce d'une rencontre envoyée sur une table (cahier des charges v0,
 * section 7) : le texte à dire ou à envoyer aux deux joueurs, et la façon de
 * l'annoncer choisie par le juge-arbitre.
 *
 * <p>Les envois réels (SMS, appel vocal) passeront par des ports dédiés quand
 * leurs tâches seront faites ; en attendant, l'annonce est affichée à l'écran
 * pour que la table de marque l'appelle au micro.</p>
 *
 * @param mode    façon d'annoncer
 * @param table   numéro de la table
 * @param joueurA premier joueur
 * @param joueurB second joueur
 * @param texte   texte de l'annonce
 */
public record AnnonceRencontre(ModeAnnonce mode, int table, Joueur joueurA, Joueur joueurB, String texte) {

    public AnnonceRencontre {
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(texte, "texte");
    }

    /**
     * Prépare l'annonce d'un match lancé : « Table 4 : Hugo MORIN contre Léa
     * DURAND (C – Toutes séries, Poule B · tour 2). »
     *
     * @param contexte tableau et tour, ou null / vide
     */
    public static AnnonceRencontre preparer(ModeAnnonce mode, Match match, int table, String contexte) {
        Objects.requireNonNull(match, "match");
        String texte = "Table " + table + " : " + nom(match.getJoueurA()) + " contre " + nom(match.getJoueurB())
                + (contexte == null || contexte.isBlank() ? "" : " (" + contexte + ")") + ".";
        return new AnnonceRencontre(mode, table, match.getJoueurA(), match.getJoueurB(), texte);
    }

    /** Faut-il prévenir les joueurs (SMS ou appel) ? Non pour « Rien ». */
    public boolean aEnvoyer() {
        return mode.avecSms() || mode.avecAppel();
    }

    private static String nom(Joueur j) {
        return j == null || j.estADeterminer() ? "à déterminer" : j.getNomComplet();
    }
}
