package com.pingmanager.ui.ecran;

import com.pingmanager.core.ResultatSaisieMatch;
import com.pingmanager.core.SaisiePoule;
import com.pingmanager.model.IssueMatch;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Manche;
import com.pingmanager.model.Match;
import com.pingmanager.model.Poule;
import com.pingmanager.model.Sexe;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Saisie d'une poule entière d'un coup : pré-remplissage et lecture des lignes. */
class PresentationSaisiePouleTest {

    private static Joueur j(String l, String nom) {
        return new Joueur(l, nom, "Paul", Sexe.HOMME, "Club", 1000);
    }

    @Test
    void scores_deja_saisis_pre_remplis_et_lignes_vides_sinon() {
        Match m = new Match(j("1", "DUPONT"), j("2", "MARTIN"));
        assertEquals(List.of("", "", "", "", ""), PresentationSaisiePoule.valeurs(m).manches());
        m.definirScore(List.of(new Manche(11, 7), new Manche(9, 11), new Manche(11, 8), new Manche(11, 6)));
        PresentationSaisiePoule.Valeurs v = PresentationSaisiePoule.valeurs(m);
        assertEquals("11-7", v.manches().get(0));
        assertEquals("", v.manches().get(4));
        assertNull(v.issue());
        Match forfait = new Match(j("1", "DUPONT"), j("3", "DURAND"));
        forfait.declarerIssue(IssueMatch.FORFAIT, forfait.getJoueurB());
        assertEquals(IssueMatch.FORFAIT, PresentationSaisiePoule.valeurs(forfait).issue());
        assertEquals(forfait.getJoueurB(), PresentationSaisiePoule.valeurs(forfait).fautif());
    }

    @Test
    void lecture_d_une_ligne_score_issue_ou_manche_illisible() {
        Match m = new Match(j("1", "DUPONT"), j("2", "MARTIN"));
        PresentationSaisiePoule.Lecture l = PresentationSaisiePoule.lire(2, m, List.of("11-5", "7", "-9", "", ""),
                null, null);
        assertNull(l.erreur());
        assertEquals(List.of(new Manche(11, 5), new Manche(11, 7), new Manche(9, 11)), l.partie().manches());
        assertTrue(PresentationSaisiePoule.lire(3, m, List.of("", "", "", "", ""), IssueMatch.NORMALE, null)
                .partie().estVide());
        assertEquals(IssueMatch.ABANDON, PresentationSaisiePoule.lire(1, m, List.of("11-5"), IssueMatch.ABANDON,
                m.getJoueurA()).partie().issue());
        assertTrue(PresentationSaisiePoule.lire(2, m, List.of("onze"), null, null).erreur()
                .startsWith("Partie 2, manche 1 illisible"));
    }

    @Test
    void comptes_rendus_et_libelle_de_la_partie() {
        Joueur a = j("1", "DUPONT");
        Joueur b = j("2", "MARTIN");
        Joueur c = j("3", "DURAND");
        Poule p = new Poule("3", List.of(a, b, c));
        Match m = new Match(a, c);
        p.ajouterMatch(m);
        assertEquals("1. 1–3  DUPONT – DURAND", PresentationSaisiePoule.libelle(1, p, m));
        assertEquals("Poule 3 terminée : la table 5 est libre.", PresentationSaisiePoule.enregistre(
                new SaisiePoule.Resultat(okay(), 0, 3, true), p, 5));
        assertEquals("2 résultats enregistrés pour la poule 3.", PresentationSaisiePoule.enregistre(
                new SaisiePoule.Resultat(okay(), 0, 2, false), p, 5));
        assertEquals("3-1", PresentationSaisiePoule.apercu(List.of("11-7", "9-11", "11-8", "11-6", "")));
    }

    /** Un résultat « enregistré » du cœur (vérification d'un score valide). */
    private static ResultatSaisieMatch okay() {
        return enregistre();
    }

    private static ResultatSaisieMatch enregistre() {
        Match m = new Match(j("8", "X"), j("9", "Y"));
        m.setStatut(com.pingmanager.model.StatutMatch.EN_COURS);
        return new com.pingmanager.core.SaisieResultatMatch().verifierScore(m,
                List.of(new Manche(11, 1), new Manche(11, 1), new Manche(11, 1)));
    }
}
