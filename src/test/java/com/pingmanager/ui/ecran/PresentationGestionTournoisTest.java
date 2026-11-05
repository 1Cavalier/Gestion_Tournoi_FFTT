package com.pingmanager.ui.ecran;

import com.pingmanager.model.ModePreparation;
import com.pingmanager.model.StatutTournoi;
import com.pingmanager.ui.ecran.PresentationGestionTournois.Etape;
import com.pingmanager.ui.ecran.PresentationGestionTournois.Fiche;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PresentationGestionTournoisTest {

    private static final LocalDate AUJ = LocalDate.of(2026, 11, 5);

    private static Fiche fiche(String nom, StatutTournoi statut, LocalDate debut, LocalDate fin) {
        return fiche(nom, statut, debut, fin, false, ModePreparation.COMPLETE);
    }

    private static Fiche fiche(String nom, StatutTournoi statut, LocalDate debut, LocalDate fin,
                               boolean entame, ModePreparation mode) {
        return new Fiche(nom, statut, mode, entame, debut, fin, 3, 0, 1, 12, Path.of("t" + Math.abs(nom.hashCode()) + ".json"));
    }

    private static List<String> noms(List<Fiche> fiches) {
        return fiches.stream().map(Fiche::nom).toList();
    }

    @Test
    void frise_dans_l_ordre_etape_4_publie_a_etape_1_brouillon() {
        List<Etape> frise = new PresentationGestionTournois(List.of(), AUJ).frise();
        assertEquals(List.of(4, 3, 2, 1), frise.stream().map(Etape::numero).toList());
        assertEquals(List.of(StatutTournoi.PUBLIE, StatutTournoi.HOMOLOGUE,
                StatutTournoi.DEMANDE_HOMOLOGATION, StatutTournoi.BROUILLON),
                frise.stream().map(Etape::statut).toList());
        assertEquals("Homologué validé", frise.get(1).titre());
        assertEquals("Demande d'homologation en cours", frise.get(2).titre());
        assertEquals("Étape 4", frise.get(0).libelle());
        assertTrue(frise.stream().allMatch(Etape::estVide));
    }

    @Test
    void chaque_tournoi_va_dans_l_etape_de_son_statut_trie_par_date_sans_date_en_dernier() {
        PresentationGestionTournois p = new PresentationGestionTournois(List.of(
                fiche("B2", StatutTournoi.BROUILLON, null, null),
                fiche("B1", StatutTournoi.BROUILLON, LocalDate.of(2027, 3, 1), null),
                fiche("D", StatutTournoi.DEMANDE_HOMOLOGATION, LocalDate.of(2027, 1, 1), null),
                fiche("H", StatutTournoi.HOMOLOGUE, LocalDate.of(2027, 2, 1), null),
                fiche("P", StatutTournoi.PUBLIE, LocalDate.of(2027, 4, 10), LocalDate.of(2027, 4, 11))), AUJ);
        List<Etape> frise = p.frise();
        assertEquals(List.of("P"), noms(frise.get(0).tournois()));
        assertEquals(List.of("H"), noms(frise.get(1).tournois()));
        assertEquals(List.of("D"), noms(frise.get(2).tournois()));
        assertEquals(List.of("B1", "B2"), noms(frise.get(3).tournois()));
    }

    @Test
    void annule_et_publie_termine_vont_dans_l_historique_pas_dans_la_frise() {
        PresentationGestionTournois p = new PresentationGestionTournois(List.of(
                fiche("Annulé", StatutTournoi.ANNULE, LocalDate.of(2027, 5, 1), null),
                fiche("Ancien", StatutTournoi.PUBLIE, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 2)),
                fiche("Plus ancien", StatutTournoi.PUBLIE, LocalDate.of(2025, 3, 1), null),
                fiche("Futur", StatutTournoi.PUBLIE, LocalDate.of(2027, 4, 10), null)), AUJ);
        assertEquals(List.of("Annulé", "Ancien", "Plus ancien"), noms(p.historique()));
        assertEquals(List.of("Futur"), noms(p.frise().get(0).tournois()));
        assertEquals("Historique des tournois passés (3)", p.lienHistorique());
        assertEquals("Annulé", PresentationGestionTournois.libelleHistorique(p.historique().get(0)));
        assertEquals("Terminé", PresentationGestionTournois.libelleHistorique(p.historique().get(1)));
    }

    @Test
    void brouillon_date_depassee_reste_dans_la_frise() {
        PresentationGestionTournois p = new PresentationGestionTournois(List.of(
                fiche("Vieux brouillon", StatutTournoi.BROUILLON, LocalDate.of(2026, 1, 1), null)), AUJ);
        assertTrue(p.historique().isEmpty());
        assertEquals(List.of("Vieux brouillon"), noms(p.frise().get(3).tournois()));
        assertEquals("Historique des tournois passés", p.lienHistorique());
    }

    @Test
    void tournoi_dont_le_dernier_jour_est_aujourd_hui_n_est_pas_passe() {
        Fiche f = fiche("Dimanche", StatutTournoi.PUBLIE, AUJ.minusDays(1), AUJ);
        PresentationGestionTournois p = new PresentationGestionTournois(List.of(f), AUJ);
        assertFalse(p.estPasse(f));
        assertTrue(p.actuel().orElseThrow().enCours());
    }

    @Test
    void sans_tournoi_publie_le_tournoi_actuel_est_vide() {
        PresentationGestionTournois p = new PresentationGestionTournois(List.of(
                fiche("B", StatutTournoi.BROUILLON, AUJ, null),
                fiche("H", StatutTournoi.HOMOLOGUE, AUJ, null)), AUJ);
        assertTrue(p.actuel().isEmpty());
    }

    @Test
    void jour_j_entame_prime_sur_le_tournoi_publie_du_jour() {
        PresentationGestionTournois p = new PresentationGestionTournois(List.of(
                fiche("Du jour", StatutTournoi.PUBLIE, AUJ, AUJ),
                fiche("Entamé", StatutTournoi.PUBLIE, AUJ.plusDays(3), null, true, ModePreparation.COMPLETE)), AUJ);
        PresentationGestionTournois.Actuel a = p.actuel().orElseThrow();
        assertEquals("Entamé", a.fiche().nom());
        assertEquals("TOURNOI EN COURS", a.surtitre());
    }

    @Test
    void sinon_le_prochain_tournoi_publie_a_venir_est_propose() {
        PresentationGestionTournois p = new PresentationGestionTournois(List.of(
                fiche("Mai", StatutTournoi.PUBLIE, LocalDate.of(2027, 5, 1), null),
                fiche("Avril", StatutTournoi.PUBLIE, LocalDate.of(2027, 4, 10), null),
                fiche("Passé", StatutTournoi.PUBLIE, LocalDate.of(2026, 1, 1), null)), AUJ);
        PresentationGestionTournois.Actuel a = p.actuel().orElseThrow();
        assertEquals("Avril", a.fiche().nom());
        assertFalse(a.enCours());
        assertEquals("PROCHAIN TOURNOI", a.surtitre());
        assertTrue(p.estActuel(a.fiche()));
        assertFalse(p.estActuel(p.frise().get(0).tournois().get(1)));
    }

    @Test
    void libelles_d_action_et_pastilles_selon_le_mode() {
        Fiche brouillon = fiche("B", StatutTournoi.BROUILLON, null, null);
        Fiche publie = fiche("P", StatutTournoi.PUBLIE, null, null);
        Fiche importe = fiche("I", StatutTournoi.BROUILLON, null, null, false, ModePreparation.IMPORT);
        Fiche aChoisir = fiche("C", StatutTournoi.BROUILLON, null, null, false, ModePreparation.A_CHOISIR);
        assertEquals("Compléter", PresentationGestionTournois.libelleAction(brouillon));
        assertEquals("Ouvrir", PresentationGestionTournois.libelleAction(publie));
        assertEquals("Ouvrir", PresentationGestionTournois.libelleAction(importe));
        assertEquals("Choisir le mode", PresentationGestionTournois.libelleAction(aChoisir));
        assertEquals(null, PresentationGestionTournois.pastilleMode(brouillon));
        assertEquals("Tournoi importé", PresentationGestionTournois.pastilleMode(importe));
        assertEquals("Mode à choisir", PresentationGestionTournois.pastilleMode(aChoisir));
    }

    @Test
    void details_accordes_au_singulier_et_au_pluriel() {
        Fiche f = new Fiche("T", StatutTournoi.PUBLIE, ModePreparation.COMPLETE, false,
                LocalDate.of(2027, 4, 10), LocalDate.of(2027, 4, 11), 1, 120, 2, 24, Path.of("t.json"));
        assertEquals("10 avril 2027 · 1 tableau · 120 joueurs", PresentationGestionTournois.detailCourt(f));
        assertEquals("2 jours  ·  10 avril 2027  ·  24 tables  ·  120 joueurs",
                PresentationGestionTournois.detailActuel(f));
        Fiche vide = fiche("V", StatutTournoi.BROUILLON, null, null);
        assertEquals("dates à définir · 3 tableaux", PresentationGestionTournois.detailCourt(vide));
    }
}
