package com.pingmanager.ui;

import com.pingmanager.model.Tournoi;
import com.pingmanager.ui.infra.DonneesDemo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** B019 : les numéros de portable s'effacent à l'ouverture d'un tournoi terminé, et c'est enregistré. */
class EffacementSmsOuvertureTest {

    private static final LocalDate JOUR = LocalDate.of(2027, 6, 5);

    @TempDir
    Path dossier;

    @Test
    void numeros_effaces_et_enregistres_a_l_ouverture_d_un_tournoi_termine() {
        ContexteApplication ctx = TournoisDeTest.connecteBrunoy(
                new ContexteApplication(dossier, DonneesDemo.clubs(), DonneesDemo.licences()));
        Path f = ctx.creerBrouillon(TournoisDeTest.deBrunoy(new Tournoi("Open SMS", "Brunoy", JOUR))).toAbsolutePath();
        Tournoi t = ctx.chargerTournoi(f);
        assertTrue(ctx.convocationsSms().enregistrerContact(t, "1", "0612345678", true,
                JOUR.minusDays(3).atStartOfDay()).estReussi());
        ctx.enregistrerTournoi(t, f);

        assertEquals(0, ctx.effacerNumerosSmsSiTermine(f, JOUR.atTime(20, 0)), "pas pendant le tournoi");
        assertEquals(1, ctx.chargerTournoi(f).getConvocationsSms().getContacts().size());

        assertEquals(1, ctx.effacerNumerosSmsSiTermine(f, JOUR.plusDays(1).atTime(8, 0)));
        assertTrue(ctx.chargerTournoi(f).getConvocationsSms().getContacts().isEmpty(), "effacement enregistré");
    }
}
