package com.pingmanager.ui.ecran;

import com.pingmanager.model.AffectationJugeArbitre;
import com.pingmanager.model.GradeJugeArbitre;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.TraceTirage;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;

class PresentationTirageTest {

    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");

    @Test
    void tirage_effectue_affiche_date_heure_et_juge_arbitre_sans_le_mot_graine() {
        TraceTirage trace = new TraceTirage(Instant.parse("2026-11-16T13:05:00Z"), "Claire Durand");
        String texte = PresentationTirage.texteTirageEffectue(trace, PARIS);
        assertEquals("Tirage au sort effectué le 16/11/2026 à 14h05 — juge-arbitre : Claire Durand.", texte);
        assertFalse(texte.toLowerCase().contains("graine"));
    }

    @Test
    void tirage_sans_juge_arbitre_ou_venu_d_un_ancien_fichier() {
        TraceTirage sansJa = new TraceTirage(Instant.parse("2026-07-01T07:00:00Z"), "  ");
        assertNull(sansJa.jugeArbitre(), "nom vide : aucun juge-arbitre");
        assertEquals("Tirage au sort effectué le 01/07/2026 à 09h00 (aucun juge-arbitre désigné à ce moment).",
                PresentationTirage.texteTirageEffectue(sansJa, PARIS));
        String ancien = PresentationTirage.texteTirageEffectue(null, PARIS);
        assertTrue(ancien.startsWith("Tirage au sort effectué"));
        assertFalse(ancien.toLowerCase().contains("graine"));
    }

    @Test
    void reference_technique_reservee_au_support_sans_le_mot_graine() {
        assertEquals("Référence technique du tirage (support) : 141126.",
                PresentationTirage.texteReferenceTechnique(141126L));
        assertEquals("Référence technique du tirage (support) : non enregistrée.",
                PresentationTirage.texteReferenceTechnique(null));
        assertFalse(PresentationTirage.aideReferenceTechnique().toLowerCase().contains("graine"));
    }

    @Test
    void rappel_avant_tirage_cite_ix109_ix111_et_i_6_4_avec_le_juge_arbitre() {
        Tournoi t = new Tournoi("Open", "Club", LocalDate.of(2026, 11, 16));
        String sansJa = PresentationTirage.rappelAvantTirage(t);
        assertTrue(sansJa.startsWith("Le juge-arbitre est le responsable direct du tirage au sort (IX.109)"));
        assertTrue(sansJa.contains("(IX.111)"));
        assertTrue(sansJa.contains("(manuel JA, I-6.4)"));
        assertFalse(sansJa.toLowerCase().contains("graine"));

        t.ajouterJugeArbitre(new AffectationJugeArbitre("Durand", "Claire", null, "9412345",
                GradeJugeArbitre.JA2, null));
        assertTrue(PresentationTirage.rappelAvantTirage(t)
                .startsWith("Le juge-arbitre, Claire Durand, est le responsable direct du tirage au sort (IX.109)"));
        assertTrue(PresentationTirage.rappelAvantTirage(null).startsWith("Le juge-arbitre est"));
    }
}
