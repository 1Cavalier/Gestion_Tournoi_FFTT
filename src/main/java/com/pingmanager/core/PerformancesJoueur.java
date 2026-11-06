package com.pingmanager.core;

import com.pingmanager.model.IssueMatch;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Match;
import com.pingmanager.model.StatutMatch;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Performances d'un joueur dans un tournoi (« Mes tournois », tournois passés) :
 * victoires et défaites, par tableau et au total, en lecture seule.
 *
 * <p>Ne comptent que les matchs <b>décidés</b> (terminés, avec un vainqueur) du
 * joueur, en poule comme dans le tableau final. Un bye (qualification sans
 * adversaire) n'est ni une victoire ni une défaite. Un match gagné ou perdu sur
 * forfait, abandon, refus de jouer ou disqualification compte, et il est aussi
 * relevé à part ({@link Bilan#forfaitsEtAbandons()}) pour être signalé.</p>
 */
public final class PerformancesJoueur {

    private PerformancesJoueur() {
    }

    /** Bilan du joueur dans un tableau. */
    public record BilanTableau(Tableau tableau, int victoires, int defaites, int forfaitsEtAbandons) {
        public BilanTableau {
            Objects.requireNonNull(tableau, "tableau");
        }

        public int matchsDecides() {
            return victoires + defaites;
        }
    }

    /** Bilan du joueur dans le tournoi : totaux et détail par tableau (ordre de la journée). */
    public record Bilan(int victoires, int defaites, int forfaitsEtAbandons, List<BilanTableau> tableaux) {
        public Bilan {
            tableaux = List.copyOf(tableaux);
        }

        public int matchsDecides() {
            return victoires + defaites;
        }

        /** @return true si aucun match du joueur n'a été décidé. */
        public boolean estVide() {
            return matchsDecides() == 0;
        }
    }

    /**
     * Bilan du joueur de licence donnée : un {@link BilanTableau} par tableau où il
     * est inscrit (hors liste d'attente) et a au moins un match décidé.
     * Bilan vide si la licence est {@code null} ou absente du tournoi.
     */
    public static Bilan calculer(Tournoi tournoi, String numeroLicence) {
        List<BilanTableau> parTableau = new ArrayList<>();
        int victoires = 0;
        int defaites = 0;
        int forfaits = 0;
        if (tournoi == null || numeroLicence == null) {
            return new Bilan(0, 0, 0, parTableau);
        }
        for (Tableau tab : SuiviTournoi.tableauxOrdonnes(tournoi, numeroLicence)) {
            if (!SuiviTournoi.estInscrit(tab, numeroLicence)) {
                continue;
            }
            BilanTableau b = calculerTableau(tab, numeroLicence);
            if (b.matchsDecides() == 0) {
                continue;
            }
            parTableau.add(b);
            victoires += b.victoires();
            defaites += b.defaites();
            forfaits += b.forfaitsEtAbandons();
        }
        return new Bilan(victoires, defaites, forfaits, parTableau);
    }

    /** Bilan du joueur dans un tableau (poules puis tableau final). */
    public static BilanTableau calculerTableau(Tableau tableau, String numeroLicence) {
        Objects.requireNonNull(tableau, "tableau");
        int victoires = 0;
        int defaites = 0;
        int forfaits = 0;
        if (numeroLicence != null) {
            for (SuiviTournoi.Partie p : SuiviTournoi.parties(tableau)) {
                Match m = p.match();
                if (m.estBye() || m.getStatut() != StatutMatch.TERMINE || !joue(m, numeroLicence)) {
                    continue;
                }
                Joueur vainqueur = m.getVainqueur();
                if (vainqueur == null) {
                    continue;
                }
                if (numeroLicence.equals(vainqueur.getNumeroLicence())) {
                    victoires++;
                } else {
                    defaites++;
                }
                if (m.getIssue() != IssueMatch.NORMALE) {
                    forfaits++;
                }
            }
        }
        return new BilanTableau(tableau, victoires, defaites, forfaits);
    }

    private static boolean joue(Match m, String licence) {
        return estJoueur(m.getJoueurA(), licence) || estJoueur(m.getJoueurB(), licence);
    }

    private static boolean estJoueur(Joueur j, String licence) {
        return j != null && !j.estADeterminer() && licence.equals(j.getNumeroLicence());
    }
}
