package com.pingmanager.ui.ecran;

import com.pingmanager.core.EliminationTableau;
import com.pingmanager.core.ServiceInscriptionSurPlace;
import com.pingmanager.model.FicheLicence;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.ui.infra.DonneesDemo;
import com.pingmanager.ui.infra.TournoiDemoBrunoyJourJ;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Écran « Inscrire sur place » sur la copie au jour J du National B (clause : un tableau de plus). */
class PresentationInscriptionSurPlaceTest {

    private final Tournoi t = TournoiDemoBrunoyJourJ.construire(DonneesDemo.JOUEURS_FICTIFS, null);

    /** Un joueur éliminé en poule du 2e tableau du samedi (poules toutes jouées). */
    private Joueur elimine(Tableau tab) {
        return tab.getInscrits().stream().filter(j -> EliminationTableau.estElimine(tab, j)).findFirst().orElseThrow();
    }

    private Tableau samedi(int i) {
        List<Tableau> sa = new java.util.ArrayList<>(t.getTableaux().stream()
                .filter(x -> x.getHoraires().getJour().equals(t.getJours().first())).toList());
        sa.sort(java.util.Comparator.comparing(x -> x.getHoraires().getDebutMatchs()));
        return sa.get(i);
    }

    @Test
    void la_regle_du_club_est_rappelee() {
        assertTrue(PresentationInscriptionSurPlace.regle(t).contains("peut en prendre un de plus"),
                PresentationInscriptionSurPlace.regle(t));
    }

    @Test
    void situation_et_tableaux_proposes_a_un_joueur_elimine() {
        Tableau b = samedi(1);
        Joueur j = elimine(b);
        assertTrue(PresentationInscriptionSurPlace.situation(t, j).startsWith("Éliminé de : " + b.getNom()),
                PresentationInscriptionSurPlace.situation(t, j));
        Map<Tableau, String> verif = PresentationInscriptionSurPlace.trier(new ServiceInscriptionSurPlace().verifier(t, j));
        assertFalse(verif.containsKey(samedi(0)), "tableau final en cours");
        assertFalse(verif.containsKey(b), "poules jouées");
        assertFalse(verif.containsKey(samedi(2)), "poules en cours");
        assertTrue(verif.containsKey(samedi(3)) && verif.containsKey(samedi(4)), "pointage : pas commencé");
        List<Tableau> ordre = List.copyOf(verif.keySet());
        for (int i = 1; i < ordre.size(); i++) {
            assertFalse(ordre.get(i).getHoraires().getDebutMatchs()
                    .isBefore(ordre.get(i - 1).getHoraires().getDebutMatchs()), "dans l'ordre des horaires");
        }
    }

    @Test
    void detail_d_un_tableau_ouvert() {
        String d = PresentationInscriptionSurPlace.detail(samedi(4));
        assertTrue(d.startsWith("Samedi 10 avril · "), d);
        assertTrue(d.contains("pointage ouvert"), d);
        assertTrue(d.contains("€ sur place"), d);
    }

    @Test
    void un_licencie_pas_encore_inscrit_est_propose_a_la_recherche() {
        FicheLicence quentin = DonneesDemo.licences().trouverParLicence(DonneesDemo.LICENCE_QUENTIN).orElseThrow();
        List<Joueur> res = PresentationInscriptionSurPlace.joueurs(t, "SOUMET", List.of(quentin));
        assertTrue(res.stream().anyMatch(j -> j.getNumeroLicence().equals(DonneesDemo.LICENCE_QUENTIN)));
        assertNull(PresentationInscriptionSurPlace.participant(t, DonneesDemo.LICENCE_QUENTIN));
        assertEquals("Pas encore inscrit dans ce tournoi.", PresentationInscriptionSurPlace.situation(t,
                res.stream().filter(j -> j.getNumeroLicence().equals(DonneesDemo.LICENCE_QUENTIN)).findFirst().orElseThrow()));
    }
}
