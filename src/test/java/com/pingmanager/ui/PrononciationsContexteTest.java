package com.pingmanager.ui;

import com.pingmanager.ui.infra.DonneesDemo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** B018 : registre des prononciations porté par les données de l'application. */
class PrononciationsContexteTest {

    @TempDir
    Path dossier;

    @Test
    void prononciations_gardees_d_un_demarrage_a_l_autre() {
        ContexteApplication ctx = new ContexteApplication(dossier, DonneesDemo.clubs(), DonneesDemo.licences());
        assertTrue(ctx.prononciations().definirJoueur(DonneesDemo.LICENCE_QUENTIN, "kɑ̃.tɛ̃", "su.mɛ").valide());
        assertTrue(ctx.prononciations().definirClub(DonneesDemo.CLUB_BRUNOY, "TT Brunoy", "bʁy.nwa").valide());
        ctx.sauvegarder();
        ctx.fermer();

        ContexteApplication relu = new ContexteApplication(dossier, DonneesDemo.clubs(), DonneesDemo.licences());
        assertEquals("su.mɛ", relu.prononciations().joueur(DonneesDemo.LICENCE_QUENTIN).orElseThrow().nom());
        assertEquals("bʁy.nwa", relu.prononciations().club(DonneesDemo.CLUB_BRUNOY).orElseThrow().ipa());
        relu.fermer();
    }

    @Test
    void numero_du_club_retrouve_par_la_licence() {
        ContexteApplication ctx = new ContexteApplication(dossier, DonneesDemo.clubs(), DonneesDemo.licences());
        assertEquals(Optional.of(DonneesDemo.CLUB_BRUNOY), ctx.numeroClubDuJoueur(DonneesDemo.LICENCE_QUENTIN));
        assertEquals(Optional.empty(), ctx.numeroClubDuJoueur("0000000"));
        assertEquals(Optional.empty(), ctx.numeroClubDuJoueur(" "));
        ctx.fermer();
    }
}
