package com.pingmanager.persistence;

import com.pingmanager.core.ServicePostePointage;
import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.HorairesTableau;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** B043 : le jour porté par le journal de pointage survit ; un ancien journal sans jour se relit. */
class PointageDeuxJoursPersistanceTest {

    private static final LocalDate SAMEDI = LocalDate.of(2026, 11, 14);
    private static final LocalDate DIMANCHE = SAMEDI.plusDays(1);

    private final Joueur alice = new Joueur("100", "Martin", "Alice", Sexe.FEMME, "Brunoy", 800);

    private Tournoi tournoi() {
        Tournoi t = new Tournoi("Open", "Brunoy", List.of(SAMEDI, DIMANCHE));
        for (String nom : List.of("A", "B")) {
            Tableau tab = new Tableau(nom, ConfigurationTableau.depuisFormat(
                    com.pingmanager.model.format.RegistreFormats.formatParDefaut()).placesMax(8).build());
            LocalDate jour = nom.equals("A") ? SAMEDI : DIMANCHE;
            tab.setHoraires(new HorairesTableau(jour.atTime(8, 0), jour.atTime(8, 45), jour.atTime(9, 0)));
            t.ajouterTableau(tab);
            tab.inscrire(alice);
        }
        return t;
    }

    @Test
    void le_jour_de_chaque_operation_survit_a_l_aller_retour(@TempDir Path dir) {
        Tournoi t = tournoi();
        ServicePostePointage s = new ServicePostePointage(t, () -> Instant.parse("2026-11-14T08:01:00Z"), "accueil");
        s.pointer("A", "100");
        s.pointer("B", "100");
        Path f = dir.resolve("t" + DepotTournoi.EXTENSION);
        DepotTournoi.sauvegarder(t, null, f);
        Tournoi r = DepotTournoi.charger(f).getTournoi();
        assertEquals(t.getJournalPointage(), r.getJournalPointage());
        assertEquals(SAMEDI, r.getJournalPointage().get(0).getJour());
        assertEquals(DIMANCHE, r.getJournalPointage().get(1).getJour());
    }

    @Test
    void ancien_journal_sans_jour_se_relit_avec_un_jour_inconnu(@TempDir Path dir) throws Exception {
        Tournoi t = tournoi();
        new ServicePostePointage(t, () -> Instant.parse("2026-11-14T08:01:00Z"), "accueil").pointer("A", "100");
        Path f = dir.resolve("t" + DepotTournoi.EXTENSION);
        DepotTournoi.sauvegarder(t, null, f);
        String json = Files.readString(f);
        assertTrue(json.contains("\"jour\""));
        Files.writeString(f, json.replaceAll(",\\s*\"jour\"\\s*:\\s*\"2026-11-14\"", ""));
        Tournoi r = DepotTournoi.charger(f).getTournoi();
        assertEquals(1, r.getJournalPointage().size());
        assertNull(r.getJournalPointage().get(0).getJour());
        assertTrue(r.getTableaux().get(0).aPointe(r.getTableaux().get(0).getInscrits().get(0)));
    }
}
