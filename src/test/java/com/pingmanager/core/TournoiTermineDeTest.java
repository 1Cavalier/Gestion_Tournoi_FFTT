package com.pingmanager.core;

import com.pingmanager.model.AffectationJugeArbitre;
import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.GradeJugeArbitre;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Manche;
import com.pingmanager.model.Match;
import com.pingmanager.model.Poule;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tour;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.format.RegistreFormats;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Jeu d'essai partagé (B023) : un tournoi dont le tableau « A » est joué
 * jusqu'au bout (poules puis tableau final), et un tableau « B » seulement lancé.
 * Public pour servir aussi aux tests de {@code importation}.
 */
public final class TournoiTermineDeTest {

    public final Tournoi tournoi;
    public final Tableau termine;
    public final Tableau enCours;

    private TournoiTermineDeTest(Tournoi tournoi, Tableau termine, Tableau enCours) {
        this.tournoi = tournoi;
        this.termine = termine;
        this.enCours = enCours;
    }

    /**
     * @param homologation numéro d'homologation (null pour l'omettre)
     * @param nomJoueur1   nom du joueur J1 (pour tester l'échappement CSV)
     */
    public static TournoiTermineDeTest creer(String homologation, String nomJoueur1) {
        Tournoi t = new Tournoi("Open de Brunoy", "Brunoy TT", LocalDate.of(2027, 4, 10));
        if (homologation != null) {
            t.setNumeroHomologation(homologation);
        }
        t.ajouterJugeArbitre(new AffectationJugeArbitre("ARBITRE", "Jeanne", null, "9100001", GradeJugeArbitre.JA2));
        Tableau a = lance(t, "A", 8, nomJoueur1);
        a.getPoules().forEach(TournoiTermineDeTest::jouerPoule);
        ServicePhaseFinale service = new ServicePhaseFinale();
        if (!service.construire(a, 42L).estConstruit()) {
            throw new IllegalStateException("phase finale non construite");
        }
        for (Tour tour : a.getPhaseFinale().getTours()) {
            for (Match m : tour.getMatchs()) {
                if (m.estBye() || m.getJoueurA().estADeterminer() || m.getJoueurB().estADeterminer()) {
                    continue;
                }
                if (!service.saisirScore(a, m, victoire(m)).estValide()) {
                    throw new IllegalStateException("score refusé");
                }
            }
        }
        Tableau b = lance(t, "B", 6, null);
        return new TournoiTermineDeTest(t, a, b);
    }

    private static Tableau lance(Tournoi t, String nom, int nb, String nomJoueur1) {
        Tableau tableau = new Tableau(nom, ConfigurationTableau.depuisFormat(RegistreFormats.formatParDefaut()).build());
        t.ajouterTableau(tableau);
        tableau.ouvrirPointage();
        for (int i = 1; i <= nb; i++) {
            String lic = nom + "0" + i;
            String n = i == 1 && nomJoueur1 != null ? nomJoueur1 : "NOM" + nom + i;
            Joueur j = new Joueur(lic, n, "Prénom" + i, Sexe.HOMME, "Club " + i, 500 + 10 * i);
            tableau.inscrire(j);
            tableau.pointer(j);
        }
        new ServicePointage(t).cloturerPointage(tableau);
        new ServiceLancementTableau().lancer(tableau, 1L);
        return tableau;
    }

    private static void jouerPoule(Poule poule) {
        for (Match m : poule.getMatchs()) {
            m.definirScore(List.of(new Manche(11, 4), new Manche(9, 11), new Manche(11, 4), new Manche(11, 4)));
        }
    }

    private static List<Manche> victoire(Match m) {
        List<Manche> r = new ArrayList<>();
        for (int i = 0; i < m.getNombreManchesGagnantes(); i++) {
            r.add(new Manche(11, 6));
        }
        return r;
    }
}
