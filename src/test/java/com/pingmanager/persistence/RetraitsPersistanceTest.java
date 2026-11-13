package com.pingmanager.persistence;

import com.pingmanager.core.ListeRetraits;
import com.pingmanager.core.ServicePostePointage;
import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.ModeReglement;
import com.pingmanager.model.MotifRetrait;
import com.pingmanager.model.OperationPointage;
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

/** B043 : motifs de retrait, paiement au retrait et fiches des retirés survivent ; anciens fichiers relus. */
class RetraitsPersistanceTest {

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

    private static Path sauver(Tournoi t, Path dir) {
        Path f = dir.resolve("t" + DepotTournoi.EXTENSION);
        DepotTournoi.sauvegarder(t, null, f);
        return f;
    }

    @Test
    void motif_paiement_et_fiche_du_retire_survivent_et_le_retrait_s_annule_apres_relecture(@TempDir Path dir) {
        Tournoi t = tournoi();
        ServicePostePointage s = new ServicePostePointage(t, () -> Instant.parse("2026-11-15T08:00:00Z"), "p");
        s.marquerPaye("A", "100", ModeReglement.CHEQUE);
        s.desinscrire("A", "100", MotifRetrait.BLESSURE_MALADIE, "entorse");
        Tournoi r = DepotTournoi.charger(sauver(t, dir)).getTournoi();
        assertEquals(t.getJournalPointage(), r.getJournalPointage());
        OperationPointage op = r.getJournalPointage().get(1);
        assertEquals(MotifRetrait.BLESSURE_MALADIE, op.getMotifRetrait());
        assertEquals("entorse", op.getPrecision());
        assertTrue(op.avaitPaye());
        Tableau a = r.getTableaux().get(0);
        assertEquals("Martin", a.retrouverRetire("100").getNom());
        assertEquals(1, new ListeRetraits(r).desinscrits(null).size());
        assertTrue(new ServicePostePointage(r, Instant::now, "p").annulerRetrait("A", "100").estAcceptee());
    }

    @Test
    void ancien_desistement_sans_motif_se_relit_comme_decision_du_joueur(@TempDir Path dir) throws Exception {
        Tournoi t = tournoi();
        new ServicePostePointage(t, () -> Instant.parse("2026-11-15T08:00:00Z"), "p")
                .desinscrire("A", "100", MotifRetrait.DECISION_JOUEUR, "parti tôt");
        Path f = sauver(t, dir);
        // Fichier tel qu'écrit par B041 : ni motifRetrait, ni retires, ni jour.
        String json = Files.readString(f)
                .replaceAll(",\\s*\"motifRetrait\"\\s*:\\s*\"[A-Z_]+\"", "")
                .replaceAll(",\\s*\"jour\"\\s*:\\s*\"[0-9-]+\"", "")
                .replaceAll(",\\s*\"retires\"\\s*:\\s*\\[[^\\]]*\\]", "");
        assertFalse(json.contains("motifRetrait"));
        assertFalse(json.contains("retires"));
        Files.writeString(f, json);
        Tournoi r = DepotTournoi.charger(f).getTournoi();
        OperationPointage op = r.getJournalPointage().get(0);
        assertFalse(op.motifRetraitNote());
        assertEquals(MotifRetrait.DECISION_JOUEUR, op.getMotifRetrait());
        assertEquals("parti tôt", op.getPrecision());
        assertTrue(r.getTableaux().get(0).getRetires().isEmpty());
        assertEquals(ListeRetraits.Nature.DECISION_JOUEUR, new ListeRetraits(r).desinscrits(null).get(0).nature());
    }

    @Test
    void motif_inconnu_d_une_version_plus_recente_se_relit_comme_autre(@TempDir Path dir) throws Exception {
        Tournoi t = tournoi();
        new ServicePostePointage(t, () -> Instant.parse("2026-11-15T08:00:00Z"), "p")
                .desinscrire("A", "100", MotifRetrait.TRANSFERT_TABLEAU, "vers B");
        Path f = sauver(t, dir);
        Files.writeString(f, Files.readString(f).replace("\"TRANSFERT_TABLEAU\"", "\"MOTIF_FUTUR\""));
        OperationPointage op = DepotTournoi.charger(f).getTournoi().getJournalPointage().get(0);
        assertEquals(MotifRetrait.AUTRE, op.getMotifRetrait());
        assertEquals("vers B", op.getPrecision());
    }

    @Test
    void tournoi_sans_retrait_n_ecrit_ni_retires_ni_motif(@TempDir Path dir) throws Exception {
        String json = Files.readString(sauver(tournoi(), dir));
        assertFalse(json.contains("retires"));
        assertFalse(json.contains("motifRetrait"));
    }
}
