package com.pingmanager.persistence;

import com.pingmanager.core.ServicePostePointage;
import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.ModeReglement;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/** B041 : modes de règlement et journal du pointage survivent ; les anciens fichiers se relisent. */
class PostePointagePersistanceTest {

    private final Joueur alice = new Joueur("100", "Martin", "Alice", Sexe.FEMME, "Brunoy", 800);
    private final Joueur bruno = new Joueur("200", "Durand", "Bruno", Sexe.HOMME, "Yerres", 900);

    private Tournoi tournoi() {
        Tournoi t = new Tournoi("Open", "Brunoy", LocalDate.of(2026, 11, 15));
        Tableau a = new Tableau("A", ConfigurationTableau.depuisFormat(
                com.pingmanager.model.format.RegistreFormats.formatParDefaut()).placesMax(8).build());
        t.ajouterTableau(a);
        a.inscrire(alice);
        a.inscrire(bruno);
        return t;
    }

    private Tournoi allerRetour(Tournoi t, Path dir) {
        Path f = dir.resolve("t" + DepotTournoi.EXTENSION);
        DepotTournoi.sauvegarder(t, null, f);
        return DepotTournoi.charger(f).getTournoi();
    }

    @Test
    void mode_de_reglement_et_journal_survivent_a_l_aller_retour(@TempDir Path dir) {
        Tournoi t = tournoi();
        ServicePostePointage s = new ServicePostePointage(t, () -> Instant.parse("2026-11-15T08:01:02Z"), "accueil-1");
        s.pointer("A", "100");
        s.marquerPaye("A", "100", ModeReglement.CHEQUE);
        s.marquerPaye("A", "200", ModeReglement.EN_LIGNE);
        s.desinscrire("A", "200", "Malade « grippe »");

        Tournoi r = allerRetour(t, dir);
        Tableau a = r.getTableaux().get(0);
        Joueur aliceLue = a.getInscrits().get(0);
        assertTrue(a.aPointe(aliceLue));
        assertEquals(ModeReglement.CHEQUE, a.modeReglement(aliceLue));
        assertEquals(1, a.getInscrits().size());
        assertEquals(t.getJournalPointage(), r.getJournalPointage());
        assertEquals(4, r.getJournalPointage().size());
    }

    @Test
    void tournoi_sans_pointage_garde_son_ancien_format(@TempDir Path dir) throws Exception {
        Tournoi t = tournoi();
        t.getTableaux().get(0).marquerPaye(alice); // payé sans mode, comme avant B041
        Path f = dir.resolve("t" + DepotTournoi.EXTENSION);
        DepotTournoi.sauvegarder(t, null, f);
        String json = Files.readString(f);
        assertFalse(json.contains("journalPointage"));
        assertFalse(json.contains("modesReglement"));
        Tournoi r = DepotTournoi.charger(f).getTournoi();
        Tableau a = r.getTableaux().get(0);
        assertTrue(a.aPaye(a.getInscrits().get(0)));
        assertNull(a.modeReglement(a.getInscrits().get(0)));
        assertTrue(r.getJournalPointage().isEmpty());
    }

    @Test
    void ligne_de_journal_ou_mode_illisible_est_ecartee_sans_bloquer_l_ouverture(@TempDir Path dir) throws Exception {
        Tournoi t = tournoi();
        ServicePostePointage s = new ServicePostePointage(t, () -> Instant.parse("2026-11-15T08:00:00Z"), "p");
        s.marquerPaye("A", "100", ModeReglement.ESPECES);
        Path f = dir.resolve("t" + DepotTournoi.EXTENSION);
        DepotTournoi.sauvegarder(t, null, f);
        String json = Files.readString(f)
                .replace("\"ESPECES\"", "\"CRYPTO\"");
        Files.writeString(f, json);
        Tournoi r = DepotTournoi.charger(f).getTournoi();
        Tableau a = r.getTableaux().get(0);
        assertTrue(a.aPaye(a.getInscrits().get(0)));
        assertNull(a.modeReglement(a.getInscrits().get(0)));
        assertTrue(r.getJournalPointage().isEmpty());
    }
}
