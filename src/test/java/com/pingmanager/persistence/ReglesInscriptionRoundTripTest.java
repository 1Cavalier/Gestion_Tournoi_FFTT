package com.pingmanager.persistence;

import com.pingmanager.core.RegistreDisciplinaire;
import com.pingmanager.model.ArticleOptionnel;
import com.pingmanager.model.ConditionsMaintien;
import com.pingmanager.model.OptionsReglement;
import com.pingmanager.model.Paiement;
import com.pingmanager.model.PhasePoints;
import com.pingmanager.model.PolitiqueForfaits;
import com.pingmanager.model.PolitiqueForfaits.Periode;
import com.pingmanager.model.PolitiqueForfaits.Regle;
import com.pingmanager.model.PolitiqueForfaits.Remboursement;
import com.pingmanager.model.QuotasTableaux;
import com.pingmanager.model.Tournoi;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Phase des points, quotas, maintien, forfaits, paiement et articles : relus à l'identique. */
class ReglesInscriptionRoundTripTest {

    private static final LocalDate SAM = LocalDate.of(2027, 4, 17);
    private static final LocalDate DIM = LocalDate.of(2027, 4, 18);

    private static Tournoi relu(Tournoi t) {
        return TournoiMapper.lire(TournoiMapper.ecrireDocument(t, new RegistreDisciplinaire())).getTournoi();
    }

    private static Tournoi tournoi() {
        Tournoi t = new Tournoi("Tournoi de Brunoy", "Brunoy", List.of(SAM, DIM));
        t.setPeriode(SAM, DIM);
        return t;
    }

    @Test
    void toutes_les_regles_relues() {
        Tournoi t = tournoi();
        t.setPhasePoints(PhasePoints.DEUXIEME);
        t.setQuotasTableaux(QuotasTableaux.parJour(Map.of(
                SAM, new QuotasTableaux.Quota(2, 3), DIM, new QuotasTableaux.Quota(1, null)))
                .avecTableauBonusJoueuses("Toutes séries dames"));
        t.setConditionsMaintien(new ConditionsMaintien(8, LocalDate.of(2027, 4, 15),
                ConditionsMaintien.ModeReduction.ABSENTS, 0, 5, 10));
        t.setPolitiqueForfaits(new PolitiqueForfaits(Map.of(
                Periode.PENDANT_INSCRIPTIONS, new Regle(Remboursement.COMPLET, 0, false),
                Periode.APRES_CLOTURE, new Regle(Remboursement.PARTIEL, 50, true),
                Periode.JOUR_J, new Regle(Remboursement.AUCUN, 0, false)), true));
        t.setPaiement(new Paiement(Paiement.Mode.HELLOASSO, "https://www.helloasso.com/associations/x"));
        t.setOptionsReglement(new OptionsReglement(OptionsReglement.Mode.GENERE, List.of(
                new ArticleOptionnel(ArticleOptionnel.Type.BUVETTE, Map.of(ArticleOptionnel.HORAIRES, "8 h – 20 h")),
                new ArticleOptionnel(ArticleOptionnel.Type.INFOS_PRATIQUES, Map.of(ArticleOptionnel.PMR, "oui")))));

        Tournoi r = relu(t);

        assertEquals(PhasePoints.DEUXIEME, r.getPhasePoints());
        assertTrue(r.getQuotasTableaux().varieSelonLeJour());
        assertEquals("Toutes séries dames", r.getQuotasTableaux().getTableauBonusJoueuses());
        assertEquals(new QuotasTableaux.Quota(2, 3), r.getQuotasTableaux().pour(SAM));
        assertEquals(new QuotasTableaux.Quota(1, null), r.getQuotasTableaux().pour(DIM));
        ConditionsMaintien m = r.getConditionsMaintien();
        assertEquals(8, m.getMinimumInscrits());
        assertEquals(LocalDate.of(2027, 4, 15), m.getDateDecision());
        assertEquals(ConditionsMaintien.ModeReduction.ABSENTS, m.getReduction());
        assertEquals(5, m.getTrancheAbsents());
        assertEquals(10, m.getReductionPct());
        PolitiqueForfaits f = r.getPolitiqueForfaits();
        assertTrue(f.estComplete());
        assertEquals(new Regle(Remboursement.PARTIEL, 50, true), f.pour(Periode.APRES_CLOTURE));
        assertTrue(f.isFraisEnLigneDeduits());
        assertEquals("https://www.helloasso.com/associations/x", r.getPaiement().getLien());
        assertEquals(Paiement.Mode.HELLOASSO, r.getPaiement().getMode());
        assertEquals(OptionsReglement.Mode.GENERE, r.getOptionsReglement().getMode());
        assertEquals(2, r.getOptionsReglement().getArticles().size());
        assertEquals("8 h – 20 h", r.getOptionsReglement().getArticles().get(0).param(ArticleOptionnel.HORAIRES));
        assertTrue(r.getOptionsReglement().getArticles().get(1).coche(ArticleOptionnel.PMR));
    }

    @Test
    void valeurs_par_defaut_relues() {
        Tournoi r = relu(tournoi());
        assertNull(r.getPhasePoints());
        assertFalse(r.getQuotasTableaux().aLimite());
        assertEquals(0, r.getConditionsMaintien().getMinimumInscrits());
        assertFalse(r.getPolitiqueForfaits().estComplete());
        assertFalse(r.getPaiement().estPret());
        assertEquals(List.of(ArticleOptionnel.Type.DROIT_IMAGE),
                r.getOptionsReglement().getArticles().stream().map(ArticleOptionnel::type).toList());
    }
}
