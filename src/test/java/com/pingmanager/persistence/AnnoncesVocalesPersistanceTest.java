package com.pingmanager.persistence;

import com.pingmanager.model.ReglagesAnnoncesVocales;
import com.pingmanager.model.Tournoi;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/** Annonces vocales (B017) : aller-retour JSON et anciens fichiers. */
class AnnoncesVocalesPersistanceTest {

    private static Tournoi tournoi() {
        return new Tournoi("Tournoi de Brunoy", "TT Brunoy", LocalDate.of(2026, 11, 14));
    }

    private static Tournoi allerRetour(Tournoi t, Path fichier) throws Exception {
        DepotTournoi.sauvegarder(t, fichier);
        return DepotTournoi.charger(fichier).getTournoi();
    }

    @Test
    void tournoi_sans_reglages_garde_son_ancien_format() throws Exception {
        Path fichier = Files.createTempFile("voix_vierge", DepotTournoi.EXTENSION);
        Tournoi relu = allerRetour(tournoi(), fichier);
        assertFalse(Files.readString(fichier).contains("annoncesVocales"));
        ReglagesAnnoncesVocales r = relu.getAnnoncesVocales();
        assertTrue(r.estVierge());
        assertFalse(r.isAnnonceAutomatique());
        assertEquals(2, r.getDiffusions());
        assertEquals(0, r.getRappelMinutes());
    }

    @Test
    void reglages_relus_a_l_identique() throws Exception {
        Tournoi t = tournoi();
        ReglagesAnnoncesVocales r = t.getAnnoncesVocales();
        r.setAnnonceAutomatique(true);
        r.setVoix("Microsoft Hortense Desktop");
        r.setDebit(-3);
        r.setDiffusions(3);
        r.setRappelMinutes(5);
        r.setAvecTableauEtTour(true);

        Path fichier = Files.createTempFile("voix_complet", DepotTournoi.EXTENSION);
        ReglagesAnnoncesVocales lu = allerRetour(t, fichier).getAnnoncesVocales();
        assertTrue(lu.isAnnonceAutomatique());
        assertEquals("Microsoft Hortense Desktop", lu.getVoix());
        assertEquals(-3, lu.getDebit());
        assertEquals(3, lu.getDiffusions());
        assertEquals(5, lu.getRappelMinutes());
        assertTrue(lu.isAvecTableauEtTour());
    }

    @Test
    void valeurs_abimees_ou_hors_bornes_relues_sans_bloquer() throws Exception {
        Tournoi t = tournoi();
        t.getAnnoncesVocales().setRappelMinutes(10);
        Path fichier = Files.createTempFile("voix_abime", DepotTournoi.EXTENSION);
        DepotTournoi.sauvegarder(t, fichier);
        String json = Files.readString(fichier);
        Files.writeString(fichier, json.replaceAll("\"annoncesVocales\"\\s*:\\s*\\{[^{}]*\\}",
                "\"annoncesVocales\": {\"annonceAutomatique\": \"oui\", \"voix\": 12, \"debit\": 99, "
                        + "\"diffusions\": 0, \"rappelMinutes\": 600}"));
        ReglagesAnnoncesVocales lu = DepotTournoi.charger(fichier).getTournoi().getAnnoncesVocales();
        assertFalse(lu.isAnnonceAutomatique());
        assertNull(lu.getVoix());
        assertEquals(ReglagesAnnoncesVocales.DEBIT_MAX, lu.getDebit());
        assertEquals(1, lu.getDiffusions());
        assertEquals(ReglagesAnnoncesVocales.RAPPEL_MAX_MINUTES, lu.getRappelMinutes());

        Files.writeString(fichier, json.replaceAll("\"annoncesVocales\"\\s*:\\s*\\{[^{}]*\\}",
                "\"annoncesVocales\": 7"));
        assertTrue(DepotTournoi.charger(fichier).getTournoi().getAnnoncesVocales().estVierge());
    }

    @Test
    void ancien_fichier_sans_champ_relu_puis_reecrit_sans_le_champ() throws Exception {
        Tournoi t = tournoi();
        t.getAnnoncesVocales().setRappelMinutes(10);
        Path fichier = Files.createTempFile("voix_ancien", DepotTournoi.EXTENSION);
        DepotTournoi.sauvegarder(t, fichier);
        String json = Files.readString(fichier).replaceAll(",?\\s*\"annoncesVocales\"\\s*:\\s*\\{[^{}]*\\}", "");
        assertFalse(json.contains("annoncesVocales"));
        Files.writeString(fichier, json);
        Tournoi relu = DepotTournoi.charger(fichier).getTournoi();
        assertTrue(relu.getAnnoncesVocales().estVierge());
        Path copie = Files.createTempFile("voix_ancien2", DepotTournoi.EXTENSION);
        assertTrue(allerRetour(relu, copie).getAnnoncesVocales().estVierge());
    }

    @Test
    void reglages_remis_a_zero_apres_modification_reviennent_vierges() throws Exception {
        Tournoi t = tournoi();
        t.getAnnoncesVocales().setVoix("  ");
        t.getAnnoncesVocales().setDebit(0);
        t.getAnnoncesVocales().setDiffusions(2);
        Path fichier = Files.createTempFile("voix_zero", DepotTournoi.EXTENSION);
        assertTrue(allerRetour(t, fichier).getAnnoncesVocales().estVierge());
        t.getAnnoncesVocales().setVoix("Voix \"spéciale\" <&>");
        t.getAnnoncesVocales().setDebit(-10);
        assertEquals("Voix \"spéciale\" <&>", allerRetour(t, fichier).getAnnoncesVocales().getVoix());
        assertEquals(-10, allerRetour(t, fichier).getAnnoncesVocales().getDebit());
    }
}
