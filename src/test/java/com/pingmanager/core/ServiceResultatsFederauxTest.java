package com.pingmanager.core;

import com.pingmanager.model.Tournoi;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/** B023 : préparation des résultats d'un tournoi pour l'export fédéral (IX.107). */
class ServiceResultatsFederauxTest {

    private static final Instant INSTANT = Instant.parse("2027-04-10T20:00:00Z");
    private final ServiceResultatsFederaux service =
            new ServiceResultatsFederaux(() -> INSTANT, ZoneId.of("Europe/Paris"));

    @Test
    void tableau_termine_exporte_parties_et_classement() {
        TournoiTermineDeTest jeu = TournoiTermineDeTest.creer("H-2027-091", null);
        assertTrue(ServiceResultatsFederaux.estTermine(jeu.termine));
        ResultatPreparationExport r = service.construire(jeu.tournoi, jeu.termine);
        assertTrue(r.estExportable(), r::toString);
        ResultatsTournoi res = r.getResultats().orElseThrow();
        assertEquals("H-2027-091", res.numeroHomologation());
        assertEquals("Jeanne ARBITRE", res.jugeArbitreNom());
        assertEquals("9100001", res.jugeArbitreLicence());
        assertEquals(INSTANT, res.horodatage());
        assertTrue(res.parties().stream().anyMatch(p -> p.getPhase() == PartieExportee.Phase.POULE));
        assertTrue(res.parties().stream().anyMatch(p -> p.getPhase() == PartieExportee.Phase.TABLEAU));
        // numérotation continue à partir de 1
        for (int i = 0; i < res.parties().size(); i++) {
            assertEquals(i + 1, res.parties().get(i).getNumeroPartie());
        }
        assertTrue(r.getAvertissements().isEmpty(), () -> r.getAvertissements().toString());
    }

    @Test
    void aucune_partie_exempt_ni_joueur_a_determiner_dans_l_export() {
        TournoiTermineDeTest jeu = TournoiTermineDeTest.creer("H1", null);
        ResultatsTournoi res = service.construire(jeu.tournoi, jeu.termine).getResultats().orElseThrow();
        for (PartieExportee p : res.parties()) {
            assertFalse(p.getJoueurB().licence().isBlank());
            assertFalse(p.getLicenceVainqueur().isBlank());
            assertEquals("NORMALE", p.getIssue());
            assertEquals("", p.getLicenceFautif());
            assertTrue(p.manchesA() != p.manchesB());
        }
    }

    @Test
    void classement_premier_deuxieme_puis_troisiemes_ex_aequo_sans_petite_finale() {
        TournoiTermineDeTest jeu = TournoiTermineDeTest.creer("H1", null);
        List<ClassementExporte> c = service.construire(jeu.tournoi, jeu.termine).getResultats().orElseThrow().classements();
        PodiumPhaseFinale podium = ServicePhaseFinale.podium(jeu.termine.getPhaseFinale());
        assertEquals(1, c.get(0).rang());
        assertEquals(podium.premier().getNumeroLicence(), c.get(0).joueur().licence());
        assertEquals(2, c.get(1).rang());
        List<Integer> rangs = c.stream().map(ClassementExporte::rang).collect(Collectors.toList());
        assertEquals(2, rangs.stream().filter(x -> x == 3).count());
        // rangs croissants, pas de doublon de joueur
        for (int i = 1; i < rangs.size(); i++) {
            assertTrue(rangs.get(i) >= rangs.get(i - 1));
        }
        assertEquals(c.size(), c.stream().map(x -> x.joueur().licence()).distinct().count());
    }

    @Test
    void tableau_non_termine_refuse_avec_motif() {
        TournoiTermineDeTest jeu = TournoiTermineDeTest.creer("H1", null);
        ResultatPreparationExport r = service.construire(jeu.tournoi, jeu.enCours);
        assertFalse(r.estExportable());
        assertEquals(MotifRefusExport.TABLEAU_NON_TERMINE, r.getMotif());
        assertTrue(r.getResultats().isEmpty());
    }

    @Test
    void tableau_d_un_autre_tournoi_refuse() {
        TournoiTermineDeTest jeu = TournoiTermineDeTest.creer("H1", null);
        TournoiTermineDeTest autre = TournoiTermineDeTest.creer("H2", null);
        assertEquals(MotifRefusExport.TABLEAU_INCONNU, service.construire(jeu.tournoi, autre.termine).getMotif());
    }

    @Test
    void tournoi_entier_exporte_les_tableaux_termines_et_signale_les_autres() {
        TournoiTermineDeTest jeu = TournoiTermineDeTest.creer("H1", null);
        ResultatPreparationExport r = service.construire(jeu.tournoi);
        assertTrue(r.estExportable());
        assertTrue(r.getResultats().orElseThrow().parties().stream().allMatch(p -> p.getTableau().equals("A")));
        assertTrue(r.getAvertissements().stream().anyMatch(a -> a.contains("« B » non terminé")));
    }

    @Test
    void aucun_tableau_termine_refuse() {
        Tournoi t = new Tournoi("Vide", "Club", java.time.LocalDate.of(2027, 1, 1));
        assertEquals(MotifRefusExport.AUCUN_TABLEAU_TERMINE, service.construire(t).getMotif());
    }

    @Test
    void homologation_absente_avertit_sans_bloquer() {
        TournoiTermineDeTest jeu = TournoiTermineDeTest.creer(null, null);
        ResultatPreparationExport r = service.construire(jeu.tournoi, jeu.termine);
        assertTrue(r.estExportable());
        assertEquals("", r.getResultats().orElseThrow().numeroHomologation());
        assertTrue(r.getAvertissements().stream().anyMatch(a -> a.contains("homologation")));
    }

    @Test
    void fichier_exporte_copie_son_contenu_et_refuse_un_chemin() {
        byte[] o = {1, 2, 3};
        FichierExporte f = new FichierExporte("parties.csv", o, "text/csv");
        o[0] = 9;
        assertEquals(1, f.getContenu()[0]);
        f.getContenu()[1] = 9;
        assertEquals(2, f.getContenu()[1]);
        assertThrows(IllegalArgumentException.class, () -> new FichierExporte("../x.csv", o, "text/csv"));
        assertThrows(IllegalArgumentException.class, () -> new FichierExporte("a/b.csv", o, "text/csv"));
    }

    @Test
    void ports_registres_methodes_par_defaut_vides() {
        RegistreLicences l = numero -> java.util.Optional.empty();
        RegistreClubs c = numero -> java.util.Optional.empty();
        assertTrue(l.listerParClub("08910001").isEmpty());
        assertTrue(c.rechercher("brunoy", 10).isEmpty());
    }
}
