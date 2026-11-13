package com.pingmanager.core;

import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.ModeReglement;
import com.pingmanager.model.OperationPointage;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.StatutTournoi;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.TypeOperationPointage;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/** B041 : actions du poste de pointage, horodatées et journalisées. */
class ServicePostePointageTest {

    private static final Instant T0 = Instant.parse("2026-11-15T08:00:00Z");

    private final Joueur alice = new Joueur("100", "Martin", "Alice", Sexe.FEMME, "Brunoy", 800);
    private final Joueur bruno = new Joueur("200", "Durand", "Bruno", Sexe.HOMME, "Yerres", 900);
    private final Joueur chloe = new Joueur("300", "Petit", "Chloé", Sexe.FEMME, "Brunoy", 700);

    private Tableau a;
    private Tournoi tournoi;

    private ServicePostePointage service() {
        a = new Tableau("A", ConfigurationTableau.builder().placesMax(2).tailleMaxListeAttente(2).build());
        tournoi = new Tournoi("Open", "Brunoy", LocalDate.of(2026, 11, 15));
        tournoi.ajouterTableau(a);
        a.inscrire(alice);
        a.inscrire(bruno);
        a.inscrire(chloe); // liste d'attente
        return new ServicePostePointage(tournoi, () -> T0, "accueil-1");
    }

    @Test
    void pointer_journalise_une_operation_horodatee_du_poste() {
        ServicePostePointage s = service();
        ResultatActionPointage r = s.pointer("A", "100");
        assertTrue(r.estAcceptee());
        assertTrue(a.aPointe(alice));
        OperationPointage op = tournoi.getJournalPointage().get(0);
        assertEquals(TypeOperationPointage.POINTER, op.getType());
        assertEquals("100", op.getLicence());
        assertEquals("A", op.getTableau());
        assertEquals(T0, op.getHorodatage());
        assertEquals("accueil-1", op.getPoste());
    }

    @Test
    void pointer_deux_fois_ne_change_rien_et_ne_journalise_pas() {
        ServicePostePointage s = service();
        s.pointer("A", "100");
        ResultatActionPointage r = s.pointer("A", "100");
        assertTrue(r.estAcceptee());
        assertFalse(r.aModifie());
        assertEquals(1, tournoi.getJournalPointage().size());
    }

    @Test
    void payer_note_le_mode_et_changer_de_mode_est_journalise() {
        ServicePostePointage s = service();
        assertTrue(s.marquerPaye("A", "200", ModeReglement.ESPECES).aModifie());
        assertEquals(ModeReglement.ESPECES, a.modeReglement(bruno));
        assertTrue(s.marquerPaye("A", "200", ModeReglement.CHEQUE).aModifie());
        assertEquals(ModeReglement.CHEQUE, a.modeReglement(bruno));
        assertEquals(2, tournoi.getJournalPointage().size());
        assertTrue(s.marquerNonPaye("A", "200").aModifie());
        assertFalse(a.aPaye(bruno));
        assertEquals(TypeOperationPointage.MARQUER_NON_PAYE, tournoi.getJournalPointage().get(2).getType());
    }

    @Test
    void payer_sans_mode_refuse() {
        ServicePostePointage s = service();
        assertEquals(ResultatActionPointage.Motif.MODE_OBLIGATOIRE, s.marquerPaye("A", "200", null).getMotif());
    }

    @Test
    void joueur_en_liste_d_attente_ou_inconnu_refuse() {
        ServicePostePointage s = service();
        assertEquals(ResultatActionPointage.Motif.JOUEUR_NON_INSCRIT, s.pointer("A", "300").getMotif());
        assertEquals(ResultatActionPointage.Motif.TABLEAU_INCONNU, s.pointer("Z", "100").getMotif());
        assertTrue(tournoi.getJournalPointage().isEmpty());
    }

    @Test
    void desinscrire_exige_un_motif_puis_repeche_la_liste_d_attente() {
        ServicePostePointage s = service();
        s.marquerPaye("A", "100", ModeReglement.EN_LIGNE);
        assertEquals(ResultatActionPointage.Motif.MOTIF_OBLIGATOIRE, s.desinscrire("A", "100", " ").getMotif());
        assertTrue(a.getInscrits().contains(alice));

        ResultatActionPointage r = s.desinscrire("A", "100", "Blessée");
        assertTrue(r.estAcceptee());
        assertEquals(chloe, r.getRepeche());
        assertFalse(a.getInscrits().contains(alice));
        assertTrue(a.getInscrits().contains(chloe));
        assertFalse(a.aPointe(chloe));
        assertFalse(a.aPaye(chloe)); // le repêché doit pointer et payer
        assertTrue(a.getFileAttente().isEmpty());
        assertEquals("Blessée", r.getOperation().getValeur());
    }

    @Test
    void apres_cloture_on_peut_pointer_un_repeche_mais_ni_depointer_ni_desinscrire() {
        ServicePostePointage s = service();
        a.ouvrirPointage();
        s.pointer("A", "100");
        new ServicePointage(tournoi).cloturerPointage(a); // Bruno libéré, Chloé repêchée
        assertTrue(s.pointer("A", "300").aModifie());
        assertEquals(ResultatActionPointage.Motif.POINTAGE_CLOTURE, s.depointer("A", "100").getMotif());
        assertEquals(ResultatActionPointage.Motif.POINTAGE_CLOTURE,
                s.desinscrire("A", "300", "Partie").getMotif());
        assertTrue(s.marquerPaye("A", "300", ModeReglement.ESPECES).aModifie());
    }

    @Test
    void tableau_lance_bloque_pointage_et_desistement_mais_pas_la_caisse() {
        ServicePostePointage s = service();
        a.ouvrirPointage();
        s.pointer("A", "100");
        s.pointer("A", "200");
        new ServicePointage(tournoi).cloturerPointage(a);
        a.marquerLance();
        assertEquals(ResultatActionPointage.Motif.TABLEAU_LANCE, s.pointer("A", "100").getMotif());
        assertEquals(ResultatActionPointage.Motif.TABLEAU_LANCE, s.depointer("A", "100").getMotif());
        assertEquals(ResultatActionPointage.Motif.TABLEAU_LANCE, s.desinscrire("A", "100", "x").getMotif());
        assertTrue(s.marquerPaye("A", "100", ModeReglement.AUTRE).estAcceptee());
    }

    @Test
    void tournoi_annule_refuse_tout() {
        ServicePostePointage s = service();
        tournoi.changerStatut(StatutTournoi.ANNULE);
        assertEquals(ResultatActionPointage.Motif.TOURNOI_FERME, s.pointer("A", "100").getMotif());
    }

    @Test
    void poste_obligatoire() {
        Tournoi t = new Tournoi("Open", "Brunoy", LocalDate.of(2026, 11, 15));
        assertThrows(IllegalArgumentException.class, () -> new ServicePostePointage(t, () -> T0, " "));
    }
}
