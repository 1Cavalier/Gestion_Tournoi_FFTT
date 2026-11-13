package com.pingmanager.core;

import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.HorairesTableau;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.ModeReglement;
import com.pingmanager.model.MotifRetrait;
import com.pingmanager.model.OperationPointage;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.TypeOperationPointage;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * B043 : non pointé, désinscription par décision du joueur et autres motifs sont
 * séparés ; la vue « Retraits » et l'annulation d'un retrait.
 */
class RetraitsPointageTest {

    private static final Instant T0 = Instant.parse("2026-11-14T08:10:00Z");
    private static final LocalDate SAMEDI = LocalDate.of(2026, 11, 14);

    private final Joueur alice = new Joueur("100", "Martin", "Alice", Sexe.FEMME, "Brunoy", 800);
    private final Joueur bruno = new Joueur("200", "Durand", "Bruno", Sexe.HOMME, "Yerres", 900);
    private final Joueur chloe = new Joueur("300", "Petit", "Chloé", Sexe.FEMME, "Brunoy", 700);

    private Tableau a;
    private Tournoi tournoi;

    /** Tableau A (samedi) : 2 places, Alice et Bruno inscrits, Chloé en liste d'attente. */
    private ServicePostePointage service() {
        a = new Tableau("A", ConfigurationTableau.builder().placesMax(2).tailleMaxListeAttente(2).build());
        a.setHoraires(new HorairesTableau(SAMEDI.atTime(8, 0), SAMEDI.atTime(8, 45), SAMEDI.atTime(9, 0)));
        tournoi = new Tournoi("Open", "Brunoy", SAMEDI);
        tournoi.ajouterTableau(a);
        a.inscrire(alice);
        a.inscrire(bruno);
        a.inscrire(chloe);
        return new ServicePostePointage(tournoi, () -> T0, "accueil-1");
    }

    private OperationPointage derniere() {
        List<OperationPointage> j = tournoi.getJournalPointage();
        return j.get(j.size() - 1);
    }

    @Test
    void desinscription_par_decision_du_joueur_note_motif_heure_poste_jour_et_paiement() {
        ServicePostePointage s = service();
        s.marquerPaye("A", "100", ModeReglement.EN_LIGNE);
        ResultatActionPointage r = s.desinscrire("A", "100", MotifRetrait.DECISION_JOUEUR, null);
        assertTrue(r.estAcceptee());
        assertEquals(chloe, r.getRepeche());
        OperationPointage op = derniere();
        assertEquals(TypeOperationPointage.DESINSCRIRE, op.getType());
        assertEquals(MotifRetrait.DECISION_JOUEUR, op.getMotifRetrait());
        assertNull(op.getPrecision());
        assertEquals(T0, op.getHorodatage());
        assertEquals("accueil-1", op.getPoste());
        assertEquals(SAMEDI, op.getJour());
        assertTrue(op.avaitPaye());
        assertFalse(a.aEteRembourse(alice), "aucun remboursement automatique : le club décide");
    }

    @Test
    void motif_autre_sans_precision_est_refuse_avec_un_motif_precis() {
        ServicePostePointage s = service();
        assertEquals(ResultatActionPointage.Motif.PRECISION_OBLIGATOIRE,
                s.desinscrire("A", "100", MotifRetrait.AUTRE, "  ").getMotif());
        assertEquals(ResultatActionPointage.Motif.MOTIF_OBLIGATOIRE,
                s.desinscrire("A", "100", (MotifRetrait) null, null).getMotif());
        assertTrue(a.getInscrits().contains(alice));
        assertTrue(s.desinscrire("A", "100", MotifRetrait.AUTRE, "Convoqué en sélection").estAcceptee());
        assertEquals("Convoqué en sélection", derniere().getPrecision());
    }

    @Test
    void desinscription_refusee_apres_cloture_du_pointage_voir_le_juge_arbitre() {
        ServicePostePointage s = service();
        a.ouvrirPointage();
        s.pointer("A", "100");
        s.pointer("A", "200");
        JournalRetraits.cloturerPointage(tournoi, a, () -> T0, "ja");
        assertEquals(ResultatActionPointage.Motif.POINTAGE_CLOTURE,
                s.desinscrire("A", "100", MotifRetrait.BLESSURE_MALADIE, null).getMotif());
    }

    @Test
    void non_pointe_a_la_cloture_est_note_comme_non_pointe_jamais_comme_choix_du_joueur() {
        ServicePostePointage s = service();
        a.ouvrirPointage();
        s.pointer("A", "100");
        s.marquerPaye("A", "200", ModeReglement.ESPECES); // payé mais absent
        ResultatPointage r = JournalRetraits.cloturerPointage(tournoi, a, () -> T0, "ja");
        assertEquals(List.of(bruno), r.getLiberes());
        OperationPointage op = derniere();
        assertEquals(TypeOperationPointage.NON_POINTE, op.getType());
        assertNull(op.getMotifRetrait(), "un non-pointé n'a pas de motif de désinscription");
        assertTrue(op.avaitPaye());

        ListeRetraits lr = new ListeRetraits(tournoi);
        assertEquals(1, lr.nonPointes(SAMEDI).size());
        assertTrue(lr.desinscrits(SAMEDI).isEmpty());
        ListeRetraits.Retrait retrait = lr.nonPointes(SAMEDI).get(0);
        assertEquals(ListeRetraits.Nature.NON_POINTE, retrait.nature());
        assertEquals(bruno, retrait.joueur());
        assertTrue(retrait.avaitPaye());
    }

    @Test
    void vue_retraits_separe_decision_du_joueur_et_autres_motifs() {
        Tableau b = new Tableau("B", ConfigurationTableau.builder().placesMax(4).build());
        ServicePostePointage s = service();
        tournoi.ajouterTableau(b);
        b.inscrire(alice);
        b.inscrire(bruno);
        s.desinscrire("A", "100", MotifRetrait.DECISION_JOUEUR, null);
        s.desinscrire("B", "200", MotifRetrait.ERREUR_INSCRIPTION, "doublon");
        List<ListeRetraits.Retrait> d = new ListeRetraits(tournoi).desinscrits(null);
        assertEquals(2, d.size());
        assertTrue(d.stream().anyMatch(x -> x.nature() == ListeRetraits.Nature.DECISION_JOUEUR && x.tableau().equals("A")));
        assertTrue(d.stream().anyMatch(x -> x.nature() == ListeRetraits.Nature.AUTRE_MOTIF
                && x.motif() == MotifRetrait.ERREUR_INSCRIPTION && "doublon".equals(x.precision())));
    }

    @Test
    void annuler_un_retrait_reinscrit_le_joueur_si_une_place_est_libre() {
        ServicePostePointage s = service();
        a.desinscrire(chloe); // plus personne en liste d'attente
        s.desinscrire("A", "100", MotifRetrait.BLESSURE_MALADIE, null);
        assertTrue(new ListeRetraits(tournoi).estRetire("A", "100"));
        ResultatActionPointage r = s.annulerRetrait("A", "100");
        assertTrue(r.estAcceptee());
        assertEquals(TypeOperationPointage.ANNULER_RETRAIT, derniere().getType());
        assertTrue(a.getInscrits().contains(alice));
        assertFalse(a.aPointe(alice), "le joueur réinscrit doit pointer");
        assertFalse(new ListeRetraits(tournoi).estRetire("A", "100"));
        assertTrue(new ListeRetraits(tournoi).retraits(null).isEmpty());
        assertEquals(ResultatActionPointage.Motif.RETRAIT_INTROUVABLE, s.annulerRetrait("A", "100").getMotif());
    }

    @Test
    void annuler_un_retrait_est_refuse_sans_place_libre_jamais_de_depassement() {
        ServicePostePointage s = service();
        s.desinscrire("A", "100", MotifRetrait.DECISION_JOUEUR, null); // Chloé repêchée : tableau plein
        ResultatActionPointage r = s.annulerRetrait("A", "100");
        assertEquals(ResultatActionPointage.Motif.PLUS_DE_PLACE, r.getMotif());
        assertEquals(2, a.getInscrits().size());
        assertFalse(a.getInscrits().contains(alice));
        assertFalse(a.getFileAttente().contains(alice), "jamais remis en liste d'attente");
    }

    @Test
    void annuler_un_non_pointe_libere_a_la_cloture_si_une_place_reste_libre() {
        ServicePostePointage s = service();
        a.desinscrire(chloe);
        a.ouvrirPointage();
        s.pointer("A", "100");
        JournalRetraits.cloturerPointage(tournoi, a, () -> T0, "ja");
        assertFalse(a.getInscrits().contains(bruno));
        assertTrue(s.annulerRetrait("A", "200").estAcceptee());
        assertTrue(a.getInscrits().contains(bruno));
    }

    @Test
    void ancien_desistement_sans_motif_se_lit_comme_decision_du_joueur_et_sans_fiche_ne_s_annule_pas() {
        service();
        a.desinscrire(chloe);
        a.retirerInscrit(alice); // retrait fait avant B043 : fiche non gardée
        tournoi.ajouterOperationPointage(OperationPointage.builder().licence("100").tableau("A")
                .type(TypeOperationPointage.DESINSCRIRE).valeur("Blessée").horodatage(T0).poste("p").build());
        ListeRetraits.Retrait r = new ListeRetraits(tournoi).desinscrits(null).get(0);
        assertEquals(ListeRetraits.Nature.DECISION_JOUEUR, r.nature());
        assertEquals("Blessée", r.precision());
        assertEquals(SAMEDI, r.jour(), "sans jour noté : le jour du tableau");
        assertNull(r.joueur());
        assertEquals(ResultatActionPointage.Motif.FICHE_JOUEUR_ABSENTE,
                new ServicePostePointage(tournoi, () -> T0, "p").annulerRetrait("A", "100").getMotif());
    }

    @Test
    void desistement_et_cas_particulier_du_juge_arbitre_sont_notes_au_journal() {
        service();
        a.ouvrirPointage();
        Joueur repeche = JournalRetraits.desister(tournoi, a, alice, () -> T0, "ja");
        assertEquals(chloe, repeche);
        assertEquals(MotifRetrait.DECISION_JOUEUR, derniere().getMotifRetrait());
        new ServicePointage(tournoi).pointer(a, bruno);
        new ServicePointage(tournoi).pointer(a, chloe);
        JournalRetraits.cloturerPointage(tournoi, a, () -> T0, "ja");
        JournalRetraits.desisterCasParticulier(tournoi, a, bruno, true, () -> T0, "ja");
        assertEquals(MotifRetrait.RETIRE_PAR_ORGANISATION, derniere().getMotifRetrait());
        assertTrue(derniere().getPrecision().startsWith(JournalRetraits.CAS_PARTICULIER));
        assertTrue(a.aEteRembourse(bruno));
        assertEquals(2, new ListeRetraits(tournoi).desinscrits(SAMEDI).size());
    }

    @Test
    void annuler_un_retrait_est_refuse_sur_un_tableau_lance() {
        ServicePostePointage s = service();
        a.desinscrire(chloe);
        a.ouvrirPointage();
        s.pointer("A", "100");
        JournalRetraits.cloturerPointage(tournoi, a, () -> T0, "ja");
        a.marquerLance();
        assertTrue(new ListeRetraits(tournoi).nonPointes(null).get(0).tableauLance());
        assertEquals(ResultatActionPointage.Motif.TABLEAU_LANCE, s.annulerRetrait("A", "200").getMotif());
        assertFalse(a.getInscrits().contains(bruno));
    }
}
