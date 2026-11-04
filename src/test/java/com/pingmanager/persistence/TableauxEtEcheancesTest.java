package com.pingmanager.persistence;

import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.HorairesTableau;
import com.pingmanager.model.InfosReglement;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.TarifInscription;
import com.pingmanager.model.Tournoi;
import com.pingmanager.persistence.json.Json;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Lot « tableaux » : échéances d'inscription, horaires d'un tableau (avec fin
 * théorique), tarif en ligne et/ou sur place, retrait / remplacement d'un tableau.
 */
class TableauxEtEcheancesTest {

    private static final LocalDateTime J = LocalDateTime.of(2027, 3, 13, 9, 0);

    // --- Tarif : canaux -------------------------------------------------------

    @Test
    void tarif_sur_place_uniquement() {
        TarifInscription t = new TarifInscription(8, false, 10, true);
        assertFalse(t.isEnLigneProposee());
        assertTrue(t.isSurPlaceProposee());
        assertEquals(0, t.getMontantEnLigne());
        assertEquals(10, t.getMontantSurPlace());
    }

    @Test
    void tarif_sans_aucun_canal_refuse() {
        assertThrows(IllegalArgumentException.class, () -> new TarifInscription(8, false, 10, false));
    }

    @Test
    void tarif_relu_avec_ses_canaux_et_ancien_fichier_ouvert_partout() {
        TarifInscription relu = ConfigMapper.lireTarif(
                ConfigMapper.ecrireTarif(new TarifInscription(8, true, 0, false)));
        assertTrue(relu.isEnLigneProposee());
        assertFalse(relu.isSurPlaceProposee());
        assertEquals(8, relu.getMontantEnLigne());

        Json ancien = Json.objet();
        ancien.set("enLigne", 8.0);
        ancien.set("surPlace", 10.0);
        TarifInscription vieux = ConfigMapper.lireTarif(ancien);
        assertTrue(vieux.isEnLigneProposee() && vieux.isSurPlaceProposee());
    }

    // --- Horaires : fin théorique et jour -------------------------------------

    @Test
    void horaires_relus_avec_fin_theorique() {
        HorairesTableau h = new HorairesTableau(J.minusMinutes(60), J.minusMinutes(15), J, J.plusHours(8));
        HorairesTableau relu = ConfigMapper.lireHoraires(ConfigMapper.ecrireHoraires(h));
        assertEquals(J.plusHours(8), relu.getFinTheorique());
        assertEquals(LocalDate.of(2027, 3, 13), relu.getJour());
    }

    @Test
    void ancien_fichier_sans_fin_theorique() {
        Json o = ConfigMapper.ecrireHoraires(new HorairesTableau(null, null, J));
        o.set("finTheorique", Json.nul());
        assertNull(ConfigMapper.lireHoraires(o).getFinTheorique());
    }

    // --- Échéances ------------------------------------------------------------

    @Test
    void ouverture_relue_et_doit_preceder_la_cloture() {
        InfosReglement r = InfosReglement.builder()
                .ouvertureInscriptions(J.minusDays(60))
                .clotureInscriptions(J.minusDays(2))
                .build();
        assertEquals(J.minusDays(60),
                ConfigMapper.lireInfosReglement(ConfigMapper.ecrireInfosReglement(r)).getOuvertureInscriptions());

        assertThrows(IllegalArgumentException.class, () -> InfosReglement.builder()
                .ouvertureInscriptions(J.minusDays(2))
                .clotureInscriptions(J.minusDays(5))
                .build());
    }

    // --- Retrait / remplacement d'un tableau -----------------------------------

    @Test
    void remplacer_garde_la_position_et_retirer_supprime() {
        Tournoi t = new Tournoi("Open", "Brunoy", LocalDate.of(2027, 3, 13));
        Tableau a = new Tableau("A", ConfigurationTableau.builder().build());
        Tableau b = new Tableau("B", ConfigurationTableau.builder().build());
        t.ajouterTableau(a);
        t.ajouterTableau(b);

        Tableau a2 = new Tableau("A bis", ConfigurationTableau.builder().build());
        t.remplacerTableau(a, a2);
        assertSame(a2, t.getTableaux().get(0));

        t.retirerTableau(b);
        assertEquals(1, t.getTableaux().size());
    }

    @Test
    void un_tableau_avec_inscrits_ne_se_modifie_plus() {
        Tournoi t = new Tournoi("Open", "Brunoy", LocalDate.of(2027, 3, 13));
        Tableau a = new Tableau("A", ConfigurationTableau.builder().placesMax(8).build());
        t.ajouterTableau(a);
        a.inscrire(new Joueur("1234", "Durand", "Paul", Sexe.HOMME, "Brunoy", 900));
        assertThrows(IllegalStateException.class, () -> t.retirerTableau(a));
        assertThrows(IllegalStateException.class,
                () -> t.remplacerTableau(a, new Tableau("A", ConfigurationTableau.builder().build())));
    }
}
