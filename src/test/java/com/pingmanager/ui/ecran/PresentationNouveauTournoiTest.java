package com.pingmanager.ui.ecran;

import com.pingmanager.ui.ecran.PresentationNouveauTournoi.Champ;
import com.pingmanager.ui.ecran.PresentationNouveauTournoi.Verification;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PresentationNouveauTournoiTest {

    @Test
    void texte_explicatif_parle_d_homologation_et_de_gestion() {
        String t = PresentationNouveauTournoi.TEXTE_EXPLICATIF;
        assertTrue(t.contains("obligatoires pour demander l'homologation"));
        assertTrue(t.contains("simplement pour le gérer"));
    }

    @Test
    void lecture_d_une_date_jj_mm_aaaa() {
        assertEquals(LocalDate.of(2026, 12, 12), PresentationNouveauTournoi.lireDate(" 12/12/2026 "));
        assertEquals("05/01/2027", PresentationNouveauTournoi.formater(LocalDate.of(2027, 1, 5)));
        assertEquals("", PresentationNouveauTournoi.formater(null));
    }

    @Test
    void date_vide_ou_illisible_ou_inexistante_donne_rien() {
        assertNull(PresentationNouveauTournoi.lireDate(null));
        assertNull(PresentationNouveauTournoi.lireDate("  "));
        assertNull(PresentationNouveauTournoi.lireDate("12-12-2026"));
        assertNull(PresentationNouveauTournoi.lireDate("31/02/2027"));
    }

    @Test
    void saisie_complete_valide_avec_nom_nettoye() {
        Verification v = PresentationNouveauTournoi.verifier("  Tournoi de Brunoy ", true,
                "12/12/2026", "13/12/2026");
        assertTrue(v.valide());
        assertNull(v.champEnErreur());
        assertEquals("Tournoi de Brunoy", v.nom());
        assertEquals(LocalDate.of(2026, 12, 12), v.debut());
        assertEquals(LocalDate.of(2026, 12, 13), v.fin());
    }

    @Test
    void fin_vide_donne_un_tournoi_d_un_jour() {
        Verification v = PresentationNouveauTournoi.verifier("Open", true, "12/12/2026", "");
        assertTrue(v.valide());
        assertEquals(v.debut(), v.fin());
    }

    @Test
    void nom_manquant_refuse_en_premier() {
        Verification v = PresentationNouveauTournoi.verifier(" ", false, "", "");
        assertFalse(v.valide());
        assertEquals(Champ.NOM, v.champEnErreur());
        assertEquals(PresentationNouveauTournoi.MOTIF_NOM, v.motif());
    }

    @Test
    void sans_club_rattache_le_motif_renvoie_au_profil() {
        Verification v = PresentationNouveauTournoi.verifier("Open", false, "12/12/2026", "");
        assertEquals(Champ.CLUB, v.champEnErreur());
        assertTrue(v.motif().contains("Mon profil"));
    }

    @Test
    void debut_vide_et_debut_illisible_ont_des_motifs_distincts() {
        assertEquals(PresentationNouveauTournoi.MOTIF_DEBUT_VIDE,
                PresentationNouveauTournoi.verifier("Open", true, "", "").motif());
        Verification v = PresentationNouveauTournoi.verifier("Open", true, "32/12/2026", "");
        assertEquals(Champ.DEBUT, v.champEnErreur());
        assertEquals(PresentationNouveauTournoi.MOTIF_DEBUT_ILLISIBLE, v.motif());
    }

    @Test
    void fin_illisible_ou_avant_le_debut_est_refusee() {
        Verification illisible = PresentationNouveauTournoi.verifier("Open", true, "12/12/2026", "demain");
        assertEquals(Champ.FIN, illisible.champEnErreur());
        assertEquals(PresentationNouveauTournoi.MOTIF_FIN_ILLISIBLE, illisible.motif());

        Verification avant = PresentationNouveauTournoi.verifier("Open", true, "12/12/2026", "11/12/2026");
        assertEquals(Champ.FIN, avant.champEnErreur());
        assertEquals(PresentationNouveauTournoi.MOTIF_FIN_AVANT_DEBUT, avant.motif());
    }

    @Test
    void calendrier_de_fin_grise_les_jours_avant_le_debut() {
        LocalDate debut = LocalDate.of(2026, 12, 12);
        assertFalse(PresentationNouveauTournoi.jourDeFinPossible(debut, debut.minusDays(1)));
        assertTrue(PresentationNouveauTournoi.jourDeFinPossible(debut, debut));
        assertTrue(PresentationNouveauTournoi.jourDeFinPossible(debut, debut.plusDays(3)));
        assertTrue(PresentationNouveauTournoi.jourDeFinPossible(null, debut.minusYears(1)));
    }

    @Test
    void resume_d_un_tournoi_d_un_jour_puis_de_plusieurs_jours() {
        assertEquals("Tournoi d'un jour : samedi 12 décembre 2026.",
                PresentationNouveauTournoi.resumePeriode("12/12/2026", ""));
        assertEquals("Tournoi sur 2 jours : du samedi 12 décembre 2026 au dimanche 13 décembre 2026.",
                PresentationNouveauTournoi.resumePeriode("12/12/2026", "13/12/2026"));
    }

    @Test
    void pas_de_resume_tant_que_les_dates_sont_incoherentes() {
        assertEquals("", PresentationNouveauTournoi.resumePeriode("", ""));
        assertEquals("", PresentationNouveauTournoi.resumePeriode("12/12/2026", "10/12/2026"));
        assertEquals("", PresentationNouveauTournoi.resumePeriode("12/12/2026", "xx"));
    }
}
