package com.pingmanager.core;

import com.pingmanager.model.AffectationJugeArbitre;
import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.GradeJugeArbitre;
import com.pingmanager.model.Manche;
import com.pingmanager.model.Match;
import com.pingmanager.model.Poule;
import com.pingmanager.model.TraceTirage;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.StatutTableau;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.format.FormatTournoi;
import com.pingmanager.model.format.RegistreFormats;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ServiceLancementTableauTest {

    private final FormatTournoi format = RegistreFormats.formatParDefaut(); // P3Q2 : poules de 3

    private Joueur j(String licence) {
        return new Joueur(licence, "N" + licence, "P" + licence, Sexe.HOMME, "Club" + licence, 1000);
    }

    private Tableau tableauAvecFormat(String nom) {
        return new Tableau(nom, ConfigurationTableau.depuisFormat(format)
                .placesMax(0).tailleMaxListeAttente(4).build());
    }

    /** Inscrit n joueurs, ouvre puis clôture le pointage (tous présents). Retourne le tournoi. */
    private Tournoi prepareTableauPret(Tableau tableau, int nbJoueurs) {
        Tournoi t = new Tournoi("Open", "Club", LocalDate.now());
        t.ajouterTableau(tableau);
        tableau.ouvrirPointage();
        for (int i = 1; i <= nbJoueurs; i++) {
            Joueur joueur = j("J" + i);
            tableau.inscrire(joueur);
            tableau.pointer(joueur);
        }
        new ServicePointage(t).cloturerPointage(tableau);
        return t;
    }

    @Test
    void lancement_genere_les_poules_et_passe_en_lance() {
        Tableau tableau = tableauAvecFormat("A");
        Tournoi t = prepareTableauPret(tableau, 6);

        ServiceLancementTableau service = new ServiceLancementTableau();
        ResultatLancement r = service.lancer(tableau, 42L);

        assertEquals(StatutTableau.LANCE, tableau.getStatut());
        assertTrue(tableau.estLance());
        assertFalse(tableau.getPoules().isEmpty());
        // 6 joueurs en poules de 3 => 2 poules
        assertEquals(2, r.nombreDePoules());
        assertEquals(2, tableau.getPoules().size());
        assertEquals(6, r.getNombreDePresents());
        assertEquals(42L, r.getGraine());
    }

    @Test
    void lancement_refuse_si_pointage_non_cloture() {
        Tableau tableau = tableauAvecFormat("A");
        Tournoi t = new Tournoi("Open", "Club", LocalDate.now());
        t.ajouterTableau(tableau);
        tableau.ouvrirPointage();
        tableau.inscrire(j("1"));
        tableau.pointer(j("1"));
        // pas de clôture
        ServiceLancementTableau service = new ServiceLancementTableau();
        assertThrows(IllegalStateException.class, () -> service.lancer(tableau, 1L));
        assertEquals(StatutTableau.POINTAGE, tableau.getStatut());
    }

    @Test
    void lancement_refuse_si_pas_en_phase_pointage() {
        Tableau tableau = tableauAvecFormat("A");
        Tournoi t = new Tournoi("Open", "Club", LocalDate.now());
        t.ajouterTableau(tableau);
        // encore en INSCRIPTION
        ServiceLancementTableau service = new ServiceLancementTableau();
        assertThrows(IllegalStateException.class, () -> service.lancer(tableau, 1L));
    }

    @Test
    void lancement_refuse_sans_format() {
        Tableau tableau = new Tableau("A", ConfigurationTableau.builder()
                .taillePouleSouhaitee(3).qualifiesParPoule(2).build()); // pas de format
        Tournoi t = prepareTableauPret(tableau, 3);
        ServiceLancementTableau service = new ServiceLancementTableau();
        assertThrows(IllegalArgumentException.class, () -> service.lancer(tableau, 1L));
    }

    @Test
    void tirage_reproductible_avec_la_meme_graine() {
        // Deux tableaux identiques, mêmes joueurs, même graine => mêmes poules.
        Tableau t1 = tableauAvecFormat("A");
        Tableau t2 = tableauAvecFormat("B");
        Tournoi tournoi = new Tournoi("Open", "Club", LocalDate.now());
        tournoi.ajouterTableau(t1);
        tournoi.ajouterTableau(t2);
        t1.ouvrirPointage();
        t2.ouvrirPointage();
        for (int i = 1; i <= 6; i++) {
            Joueur joueur = j("J" + i);
            t1.inscrire(joueur);
            t1.pointer(joueur);
            t2.inscrire(joueur);
            t2.pointer(joueur);
        }
        ServicePointage pointage = new ServicePointage(tournoi);
        pointage.cloturerPointage(t1);
        pointage.cloturerPointage(t2);

        ServiceLancementTableau service = new ServiceLancementTableau();
        ResultatLancement r1 = service.lancer(t1, 123L);
        ResultatLancement r2 = service.lancer(t2, 123L);

        // Même structure : mêmes joueurs dans les mêmes poules (comparaison par licence).
        assertEquals(r1.nombreDePoules(), r2.nombreDePoules());
        for (int p = 0; p < r1.nombreDePoules(); p++) {
            List<Joueur> j1 = r1.getPoules().get(p).getJoueurs();
            List<Joueur> j2 = r2.getPoules().get(p).getJoueurs();
            assertEquals(licences(j1), licences(j2), "poule " + p + " identique");
        }
    }

    private List<String> licences(List<Joueur> joueurs) {
        return joueurs.stream().map(Joueur::getNumeroLicence).sorted().toList();
    }

    @Test
    void on_ne_relance_pas_un_tableau_deja_lance() {
        Tableau tableau = tableauAvecFormat("A");
        Tournoi t = prepareTableauPret(tableau, 3);
        ServiceLancementTableau service = new ServiceLancementTableau();
        service.lancer(tableau, 1L);
        assertThrows(IllegalStateException.class, () -> service.lancer(tableau, 1L));
    }

    // ------------------------------------------------------------------
    // Trace des tirages : date, heure, juge-arbitre (IX.109, IX.111)
    // ------------------------------------------------------------------

    private static void jouerToutesLesPoules(Tableau tableau) {
        for (Poule p : tableau.getPoules()) {
            for (Match m : p.getMatchs()) {
                m.definirScore(List.of(new Manche(11, 5), new Manche(11, 7), new Manche(11, 9)));
            }
        }
    }

    @Test
    void tirages_des_poules_et_du_tableau_final_dates_avec_le_juge_arbitre_et_graines_conservees() {
        Tableau tableau = tableauAvecFormat("A");
        Tournoi t = prepareTableauPret(tableau, 6);
        // IX.109 : le juge-arbitre est le responsable direct du tirage au sort.
        t.ajouterJugeArbitre(new AffectationJugeArbitre("Durand", "Claire", null, "9412345",
                GradeJugeArbitre.JA2, null));
        Instant[] maintenant = {Instant.parse("2026-11-16T13:05:00Z")};
        ServiceLancementTableau service = new ServiceLancementTableau(new MoteurTournoi(), t,
                PolitiqueOuverture.REFUSER_SI_ANNULE, () -> maintenant[0]);

        service.lancer(tableau, 42L);
        assertEquals(new TraceTirage(Instant.parse("2026-11-16T13:05:00Z"), "Claire Durand"),
                tableau.getTraceTiragePoules());
        assertEquals(Long.valueOf(42L), tableau.getGrainePoules());
        assertNull(tableau.getTraceTirageTableauFinal(), "tableau final pas encore tiré");

        jouerToutesLesPoules(tableau);
        maintenant[0] = Instant.parse("2026-11-16T16:40:00Z");
        service.construirePhaseFinale(tableau, 777L);
        assertEquals(new TraceTirage(Instant.parse("2026-11-16T16:40:00Z"), "Claire Durand"),
                tableau.getTraceTirageTableauFinal());
        assertEquals(Long.valueOf(777L), tableau.getGraineTableauFinal());
    }

    @Test
    void tirage_sans_juge_arbitre_designe_reste_date() {
        Tableau tableau = tableauAvecFormat("A");
        Tournoi t = prepareTableauPret(tableau, 6);
        new ServiceLancementTableau(new MoteurTournoi(), t, PolitiqueOuverture.REFUSER_SI_ANNULE,
                () -> Instant.parse("2026-11-16T09:00:00Z")).lancer(tableau, 3L);
        assertEquals(Instant.parse("2026-11-16T09:00:00Z"), tableau.getTraceTiragePoules().effectueLe());
        assertNull(tableau.getTraceTiragePoules().jugeArbitre());
    }

    @Test
    void un_tirage_fait_ne_peut_pas_etre_refait_et_sa_trace_ne_change_pas() {
        Tableau tableau = tableauAvecFormat("A");
        Tournoi t = prepareTableauPret(tableau, 6);
        Instant[] maintenant = {Instant.parse("2026-11-16T10:00:00Z")};
        ServiceLancementTableau service = new ServiceLancementTableau(new MoteurTournoi(), t,
                PolitiqueOuverture.REFUSER_SI_ANNULE, () -> maintenant[0]);
        service.lancer(tableau, 5L);
        List<Poule> poules = List.copyOf(tableau.getPoules());
        TraceTirage tracePoules = tableau.getTraceTiragePoules();

        // Manuel JA, I-6.4 : un joueur placé par tirage au sort ne peut plus être déplacé.
        maintenant[0] = Instant.parse("2026-11-16T11:00:00Z");
        assertThrows(IllegalStateException.class, () -> service.lancer(tableau, 6L));
        assertEquals(poules, tableau.getPoules());
        assertEquals(Long.valueOf(5L), tableau.getGrainePoules());
        assertEquals(tracePoules, tableau.getTraceTiragePoules());

        jouerToutesLesPoules(tableau);
        service.construirePhaseFinale(tableau, 8L);
        TraceTirage traceFinal = tableau.getTraceTirageTableauFinal();
        maintenant[0] = Instant.parse("2026-11-16T12:00:00Z");
        assertThrows(IllegalStateException.class, () -> service.construirePhaseFinale(tableau, 9L));
        assertEquals(Long.valueOf(8L), tableau.getGraineTableauFinal());
        assertEquals(traceFinal, tableau.getTraceTirageTableauFinal());
        // Par le service de phase finale (écran) : refus motivé, rien de changé.
        ResultatConstructionPhaseFinale r = new ServicePhaseFinale(t).construire(tableau, 10L);
        assertFalse(r.estConstruit());
        assertEquals(Long.valueOf(8L), tableau.getGraineTableauFinal());
        assertEquals(traceFinal, tableau.getTraceTirageTableauFinal());
    }
}
