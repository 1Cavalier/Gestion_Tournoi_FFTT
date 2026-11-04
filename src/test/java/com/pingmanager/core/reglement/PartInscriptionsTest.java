package com.pingmanager.core.reglement;

import com.pingmanager.core.RegistreDisciplinaire;
import com.pingmanager.model.GrilleRecompenses;
import com.pingmanager.model.Recompense;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.TypeRecompense;
import com.pingmanager.persistence.TournoiMapper;
import com.pingmanager.ui.infra.TournoiDemoBrunoy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Dotation en part des droits d'engagement (ex. tableau dames : 50 / 30 / 10 / 10 %). */
class PartInscriptionsTest {

    @Test
    void libelles_et_total_des_parts() {
        GrilleRecompenses g = GrilleRecompenses.builder()
                .place(1, Recompense.partInscriptions(50)).place(2, Recompense.partInscriptions(30))
                .plage(3, 4, Recompense.partInscriptions(10)).build();
        assertEquals(100, LibellesTableau.partInscriptions(g));
        assertEquals("100 % des inscriptions", LibellesTableau.recompenses(g));
        assertEquals("50 % des inscr.", LibellesTableau.recompense(Recompense.partInscriptions(50)));
        assertEquals(0, LibellesTableau.totalArgent(g), "ne compte pas dans la dotation fixe");
        assertThrows(IllegalArgumentException.class, () -> Recompense.partInscriptions(0));
        assertThrows(IllegalArgumentException.class, () -> Recompense.partInscriptions(120));
    }

    @Test
    void relue_a_l_identique_apres_enregistrement() {
        Tournoi t = TournoiDemoBrunoy.construire();
        Tournoi r = TournoiMapper.lire(TournoiMapper.ecrireDocument(t, new RegistreDisciplinaire())).getTournoi();
        Tableau dames = r.getTableaux().stream().filter(x -> x.getNom().equals(TournoiDemoBrunoy.DAMES)).findFirst().orElseThrow();
        GrilleRecompenses.Entree vainqueur = dames.getConfiguration().getRecompenses().getEntrees().get(0);
        assertEquals(TypeRecompense.PART_INSCRIPTIONS, vainqueur.getRecompense().getType());
        assertEquals(50, vainqueur.getRecompense().getPourcentage());
    }
}
