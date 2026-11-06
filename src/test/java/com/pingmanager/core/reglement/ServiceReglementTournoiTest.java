package com.pingmanager.core.reglement;

import com.pingmanager.core.ServiceCycleTournoi;
import com.pingmanager.model.DocumentReference;
import com.pingmanager.model.DocumentReglement;
import com.pingmanager.model.OptionsReglement;
import com.pingmanager.model.StatutTournoi;
import com.pingmanager.model.Tournoi;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Règlement et feuille d'engagement figés à la publication, modifications refusées ensuite. */
class ServiceReglementTournoiTest {

    private final ServiceCycleTournoi cycle = new ServiceCycleTournoi();
    private final ServiceReglementTournoi service = new ServiceReglementTournoi();

    private static Tournoi tournoiPubliable() {
        return TournoisReglementDeTest.tournoiPubliable();
    }

    private static Tournoi tournoiPublie() {
        return TournoisReglementDeTest.tournoiPublie();
    }

    @Test
    void avant_publication_rien_n_est_fige_et_le_reglement_suit_les_donnees() {
        Tournoi t = tournoiPubliable();
        assertNull(t.getReglementFige());
        assertNull(t.getFeuilleEngagementFigee());
        assertTrue(t.reglementModifiable());
        DocumentReglement avant = service.reglement(t);
        t.getTableaux().get(0).getHoraires().setDebutMatchs(LocalDateTime.of(2027, 3, 13, 14, 0));
        assertNotEquals(avant, service.reglement(t), "avant publication, le règlement est régénéré");
        assertNull(t.getReglementFige(), "consulter le règlement d'un brouillon ne le fige pas");
    }

    @Test
    void la_publication_fige_le_reglement_et_la_feuille_d_engagement() {
        Tournoi t = tournoiPublie();
        assertEquals(StatutTournoi.PUBLIE, t.getStatut());
        assertNotNull(t.getReglementFige());
        assertNotNull(t.getFeuilleEngagementFigee());
        assertSame(t.getReglementFige(), service.reglement(t));
        assertSame(t.getFeuilleEngagementFigee(), service.feuilleEngagement(t));
    }

    @Test
    void reglement_identique_apres_un_changement_d_horaire_post_publication() {
        Tournoi t = tournoiPublie();
        DocumentReglement reglement = service.reglement(t);
        DocumentReglement feuille = service.feuilleEngagement(t);

        // Les horaires restent modifiables après publication (tableau avec inscrits)…
        t.getTableaux().get(0).getHoraires().setDebutMatchs(LocalDateTime.of(2027, 3, 13, 14, 0));
        // … et changent bien le texte généré : la copie figée n'est pas une coïncidence.
        assertNotEquals(reglement, new GenerateurReglement().generer(t));
        assertNotEquals(feuille, new GenerateurFeuilleEngagement().generer(t));

        assertEquals(reglement, service.reglement(t), "le règlement publié ne change plus");
        assertEquals(feuille, service.feuilleEngagement(t), "la feuille d'engagement publiée ne change plus");
    }

    @Test
    void modification_des_options_refusee_apres_publication_avec_motif() {
        Tournoi t = tournoiPublie();
        OptionsReglement avant = t.getOptionsReglement();

        ResultatModificationReglement r = service.modifierOptions(t,
                new OptionsReglement(OptionsReglement.Mode.DEPOSE, List.of()));
        assertFalse(r.estAppliquee());
        assertEquals(ResultatModificationReglement.Motif.TOURNOI_PUBLIE, r.getMotif());
        assertTrue(r.getMessage().contains("publié"));
        assertSame(avant, t.getOptionsReglement(), "rien n'a changé");

        ResultatModificationReglement articles = service.modifierOptions(t, avant.avecArticles(List.of()));
        assertEquals(ResultatModificationReglement.Motif.TOURNOI_PUBLIE, articles.getMotif());
        assertEquals(1, t.getOptionsReglement().getArticles().size());
    }

    @Test
    void depot_d_un_fichier_refuse_apres_publication() {
        Tournoi t = tournoiPublie();
        ResultatModificationReglement r = service.deposer(t, new DocumentReference("Règlement", "/autre.pdf"));
        assertEquals(ResultatModificationReglement.Motif.TOURNOI_PUBLIE, r.getMotif());
        assertNull(t.getReglementDocument());
    }

    @Test
    void le_modele_refuse_aussi_la_modification_directe_apres_publication() {
        Tournoi t = tournoiPublie();
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> t.setOptionsReglement(OptionsReglement.parDefaut()));
        assertEquals(Tournoi.MOTIF_REGLEMENT_FIGE, e.getMessage());
        assertThrows(IllegalStateException.class,
                () -> t.setReglementDocument(new DocumentReference("Règlement", "/r.pdf")));
        assertThrows(IllegalStateException.class, () -> t.setReglementFige(null),
                "la copie figée ne se remplace pas");
        assertThrows(IllegalStateException.class, () -> t.setFeuilleEngagementFigee(null));
    }

    @Test
    void modifications_acceptees_avant_publication() {
        Tournoi t = tournoiPubliable();
        assertTrue(service.modifierOptions(t, new OptionsReglement(OptionsReglement.Mode.DEPOSE, List.of()))
                .estAppliquee());
        assertTrue(service.deposer(t, new DocumentReference("Règlement", "/r.pdf")).estAppliquee());
        assertEquals(OptionsReglement.Mode.DEPOSE, t.getOptionsReglement().getMode());
        assertEquals("/r.pdf", t.getReglementDocument().getEmplacement());
    }

    @Test
    void reglement_depose_non_copie_mais_feuille_figee() {
        Tournoi t = tournoiPubliable();
        t.setOptionsReglement(new OptionsReglement(OptionsReglement.Mode.DEPOSE, List.of()));
        t.setReglementDocument(new DocumentReference("Règlement", "/r.pdf"));
        assertTrue(cycle.homologuer(t, "H-2027-002").estEffectuee());
        assertTrue(cycle.publier(t).estEffectuee());
        assertNull(t.getReglementFige(), "le PDF déposé est figé par nature");
        assertNotNull(t.getFeuilleEngagementFigee());
    }

    @Test
    void tournoi_publie_sans_copie_la_recoit_a_la_premiere_demande() {
        // Tournoi publié par une ancienne version (ou une démo) : aucune copie.
        Tournoi t = tournoiPubliable();
        t.changerStatut(StatutTournoi.HOMOLOGUE);
        t.changerStatut(StatutTournoi.PUBLIE);
        assertNull(t.getReglementFige());

        DocumentReglement premier = service.reglement(t);
        assertSame(premier, t.getReglementFige());
        t.getTableaux().get(0).getHoraires().setDebutMatchs(LocalDateTime.of(2027, 3, 13, 14, 0));
        assertSame(premier, service.reglement(t));
        assertNotNull(service.feuilleEngagement(t));
    }

    @Test
    void publication_depuis_la_demande_fige_aussi() {
        Tournoi t = tournoiPubliable();
        assertTrue(cycle.demanderHomologation(t).estEffectuee());
        t.setNumeroHomologation("H-2027-003");
        assertTrue(cycle.publierDepuisDemande(t).estEffectuee());
        assertNotNull(t.getReglementFige());
        assertNotNull(t.getFeuilleEngagementFigee());
    }
}
