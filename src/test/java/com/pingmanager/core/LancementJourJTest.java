package com.pingmanager.core;

import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.StatutTournoi;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.format.RegistreFormats;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Vérification préalable au lancement (écran du jour J) et graine conservée (IX.109). */
class LancementJourJTest {

    private static Joueur j(String licence) {
        return new Joueur(licence, "N" + licence, "P" + licence, Sexe.HOMME, "Club" + licence, 1000);
    }

    private static Tableau tableau(int places) {
        return new Tableau("A", ConfigurationTableau.depuisFormat(RegistreFormats.formatParDefaut())
                .placesMax(places).tailleMaxListeAttente(4).build());
    }

    private static Tournoi tournoiAvec(Tableau tab) {
        Tournoi t = new Tournoi("Open", "Club", LocalDate.of(2026, 11, 14));
        t.ajouterTableau(tab);
        return t;
    }

    @Test
    void verification_suit_les_etapes_du_tableau_jusqu_au_lancement() {
        Tableau tab = tableau(0);
        Tournoi t = tournoiAvec(tab);
        ServiceLancementTableau service = new ServiceLancementTableau(t);
        for (int i = 1; i <= 6; i++) {
            tab.inscrire(j("L" + i));
        }
        assertEquals(ResultatVerificationLancement.Motif.POINTAGE_NON_OUVERT, service.verifierLancement(tab).getMotif());

        tab.ouvrirPointage();
        tab.getInscrits().forEach(tab::pointer);
        assertEquals(ResultatVerificationLancement.Motif.POINTAGE_NON_CLOTURE, service.verifierLancement(tab).getMotif());

        new ServicePointage(t).cloturerPointage(tab);
        ResultatVerificationLancement pret = service.verifierLancement(tab);
        assertTrue(pret.estPret());
        assertEquals(6, pret.getPresents());
        assertEquals(3, pret.getMinimum());
        assertTrue(pret.getNonPointes().isEmpty());

        service.lancer(tab, 2026L);
        assertEquals(ResultatVerificationLancement.Motif.DEJA_LANCE, service.verifierLancement(tab).getMotif());
    }

    @Test
    void verification_refuse_un_tableau_trop_petit_ou_un_tournoi_annule() {
        Tableau tab = tableau(0);
        Tournoi t = tournoiAvec(tab);
        tab.ouvrirPointage();
        tab.inscrire(j("L1"));
        tab.inscrire(j("L2"));
        tab.getInscrits().forEach(tab::pointer);
        new ServicePointage(t).cloturerPointage(tab);
        ServiceLancementTableau service = new ServiceLancementTableau(t);
        ResultatVerificationLancement r = service.verifierLancement(tab);
        assertEquals(ResultatVerificationLancement.Motif.PAS_ASSEZ_DE_PRESENTS, r.getMotif());
        assertThrows(IllegalStateException.class, () -> service.lancer(tab, 1L),
                "la vérification annonce le même refus que le lancement");

        t.changerStatut(StatutTournoi.ANNULE);
        assertEquals(ResultatVerificationLancement.Motif.TOURNOI_FERME, service.verifierLancement(tab).getMotif());
        assertEquals(ResultatVerificationLancement.Motif.HORS_TOURNOI,
                service.verifierLancement(tableau(0)).getMotif());
    }

    @Test
    void repeches_a_pointer_signales_avant_le_lancement() {
        Tableau tab = tableau(3);
        Tournoi t = tournoiAvec(tab);
        for (int i = 1; i <= 5; i++) {
            tab.inscrire(j("L" + i)); // 3 inscrits, 2 en attente
        }
        tab.ouvrirPointage();
        tab.pointer(j("L1"));
        tab.pointer(j("L2")); // L3 absent
        ResultatPointage clo = new ServicePointage(t).cloturerPointage(tab);
        assertEquals(List.of(j("L4")), clo.getRepeches());

        ResultatVerificationLancement r = new ServiceLancementTableau(t).verifierLancement(tab);
        assertTrue(r.estPret(), "le lancement reste possible");
        assertEquals(List.of(j("L4")), r.getNonPointes(), "mais le repêché à pointer est signalé");
    }

    @Test
    void la_graine_du_tirage_est_conservee_avec_le_tableau() {
        Tableau tab = tableau(0);
        Tournoi t = tournoiAvec(tab);
        tab.ouvrirPointage();
        for (int i = 1; i <= 9; i++) {
            Joueur joueur = j("L" + i);
            tab.inscrire(joueur);
            tab.pointer(joueur);
        }
        new ServicePointage(t).cloturerPointage(tab);
        assertNull(tab.getGrainePoules());
        ResultatLancement r = new ServiceLancementTableau(t).lancer(tab, 20261114L);
        assertEquals(20261114L, r.getGraine());
        assertEquals(Long.valueOf(20261114L), tab.getGrainePoules());
    }
}
