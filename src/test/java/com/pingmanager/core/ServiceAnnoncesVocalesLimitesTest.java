package com.pingmanager.core;

import com.pingmanager.core.SyntheseVocale.ParametresVoix;
import com.pingmanager.core.SyntheseVocale.ResultatSynthese;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Match;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.StatutMatch;
import com.pingmanager.model.Tournoi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/** B017 (vérification du cœur) : cas limites de la file, du rappel et de l'identité de rencontre. */
class ServiceAnnoncesVocalesLimitesTest {

    private static final Instant T0 = Instant.parse("2026-11-14T09:00:00Z");

    private Instant maintenant;
    private final List<String> dits = new ArrayList<>();
    private ServiceAnnoncesVocales service;
    private Tournoi tournoi;
    private final Joueur hugo = new Joueur("1", "MORIN", "Hugo", Sexe.HOMME, "TT Brunoy", 1200);
    private final Joueur lea = new Joueur("2", "DURAND", "Léa", Sexe.FEMME, "US Évry", 1100);
    private final Joueur max = new Joueur("3", "ROUX", "Max", Sexe.HOMME, "AS Melun", 900);

    private static final class Voix implements SyntheseVocale {
        Runnable pendantLaDiction = () -> { };
        final List<String> dits;

        Voix(List<String> dits) {
            this.dits = dits;
        }

        @Override
        public boolean estDisponible() {
            return true;
        }

        @Override
        public ResultatSynthese dire(String ssml, ParametresVoix v) {
            dits.add(ssml);
            pendantLaDiction.run();
            return ResultatSynthese.ok();
        }
    }

    private Voix voix;

    @BeforeEach
    void preparer() {
        maintenant = T0;
        voix = new Voix(dits);
        service = new ServiceAnnoncesVocales(() -> maintenant, voix);
        tournoi = new Tournoi("T", "TT Brunoy", LocalDate.of(2026, 11, 14));
    }

    private Match surTable(Match m) {
        m.setNumeroTable(3);
        m.setStatut(StatutMatch.EN_COURS);
        return m;
    }

    @Test
    void file_vide_rien_a_diffuser() {
        assertTrue(service.diffuserSuivante().isEmpty());
        assertFalse(service.aDiffuser());
        assertEquals(0, service.programmerRappelsDus((m, t) -> true));
        assertFalse(service.oublier(new Match(hugo, lea)));
        assertFalse(service.oublier(null));
    }

    @Test
    void demandes_nulles_ou_invalides_refusees_sans_exception() {
        assertFalse(service.annoncer(tournoi, null, 3).acceptee());
        assertFalse(service.annoncer(tournoi, surTable(new Match(hugo, lea)), 0).acceptee());
        assertThrows(NullPointerException.class, () -> service.annoncer(null, new Match(hugo, lea), 3));
        assertFalse(service.apresLancement(tournoi, null, 3, null).acceptee());
        assertFalse(ServiceAnnoncesVocales.annonceAutomatique(tournoi, null));
    }

    @Test
    void rencontre_oubliee_pendant_la_diction_ne_programme_pas_de_rappel() {
        tournoi.getAnnoncesVocales().setRappelMinutes(5);
        Match m = surTable(new Match(hugo, lea));
        service.annoncer(tournoi, m, 3);
        // Score saisi / envoi annulé pendant que la voix parle.
        voix.pendantLaDiction = () -> service.oublier(m);
        service.diffuserSuivante();
        assertTrue(service.rappelPrevu(m).isEmpty(), "rencontre oubliée : aucun rappel ne doit rester programmé");
    }

    @Test
    void reannoncer_pendant_la_diction_remet_en_file_sans_perdre_la_diction_en_cours() {
        Match m = surTable(new Match(hugo, lea));
        service.annoncer(tournoi, m, 3);
        AtomicReference<AnnonceVocale> vue = new AtomicReference<>();
        voix.pendantLaDiction = () -> {
            vue.set(service.enCours().orElse(null));
            service.annoncer(tournoi, m, 4);
        };
        service.diffuserSuivante();
        assertNotNull(vue.get());
        assertEquals(1, service.enAttente().size());
        assertEquals(4, service.enAttente().get(0).table());
        assertTrue(service.enCours().isEmpty());
    }

    @Test
    void horloge_qui_recule_le_rappel_n_arrive_pas_trop_tot() {
        tournoi.getAnnoncesVocales().setRappelMinutes(5);
        Match m = surTable(new Match(hugo, lea));
        service.annoncer(tournoi, m, 3);
        service.diffuserSuivante();
        maintenant = T0.minusSeconds(3600);
        assertEquals(0, service.programmerRappelsDus((x, t) -> true));
        assertTrue(service.rappelPrevu(m).isPresent(), "le rappel reste programmé");
        maintenant = T0.plusSeconds(5 * 60);
        assertEquals(1, service.programmerRappelsDus((x, t) -> true), "échéance exacte = due");
    }

    @Test
    void rappel_puis_nouvelle_demande_manuelle_ne_double_pas() {
        tournoi.getAnnoncesVocales().setRappelMinutes(5);
        Match m = surTable(new Match(hugo, lea));
        service.annoncer(tournoi, m, 3);
        service.diffuserSuivante();
        maintenant = T0.plusSeconds(600);
        service.programmerRappelsDus((x, t) -> true);
        service.annoncer(tournoi, m, 3);
        assertEquals(1, service.enAttente().size());
        assertFalse(service.enAttente().get(0).rappel(), "la demande manuelle remplace le rappel");
        // Un rappel dit ne reprogramme pas de rappel : un seul rappel par annonce.
        service.diffuserSuivante();
        service.diffuserSuivante();
        assertTrue(service.rappelPrevu(m).isPresent(), "l'annonce manuelle reprogramme son propre rappel");
    }

    @Test
    void nombre_de_repetitions_de_1_a_3_et_valeurs_hors_bornes_bornees() {
        for (int demande : new int[] {-5, 0, 1, 2, 3, 4, 99}) {
            dits.clear();
            tournoi.getAnnoncesVocales().setDiffusions(demande);
            service.annoncer(tournoi, surTable(new Match(hugo, lea)), 3);
            service.diffuserSuivante();
            int attendu = Math.max(1, Math.min(3, demande));
            assertEquals(attendu, dits.get(0).split("<s>", -1).length - 1, "diffusions demandées : " + demande);
        }
    }

    @Test
    void identite_de_rencontre_par_licence_licences_identiques_ou_absentes() {
        // Même licence, nom différent : c'est le même joueur (identité par licence).
        Joueur huguette = new Joueur("1", "AUTRE", "Nom", Sexe.HOMME, "Autre club", 1);
        assertTrue(ServiceAnnoncesVocales.memeRencontre(new Match(hugo, lea), new Match(huguette, lea)));
        // Joueurs inversés : c'est la même rencontre (A contre B = B contre A).
        assertTrue(ServiceAnnoncesVocales.memeRencontre(new Match(hugo, lea), new Match(lea, hugo)));
        // Une moitié connue seulement, ou null : jamais « la même ».
        assertFalse(ServiceAnnoncesVocales.memeRencontre(new Match(hugo, Joueur.aDeterminer()),
                new Match(hugo, Joueur.aDeterminer())));
        assertFalse(ServiceAnnoncesVocales.memeRencontre(null, new Match(hugo, lea)));
        assertFalse(ServiceAnnoncesVocales.memeRencontre(new Match(hugo, lea), null));
        assertTrue(ServiceAnnoncesVocales.memeRencontre(null, null));
    }

    @Test
    void synthese_qui_leve_une_exception_ou_rend_null_ne_perd_pas_l_annonce() {
        ServiceAnnoncesVocales s = new ServiceAnnoncesVocales(() -> maintenant, new SyntheseVocale() {
            @Override
            public boolean estDisponible() {
                return true;
            }

            @Override
            public ResultatSynthese dire(String ssml, ParametresVoix v) {
                return null;
            }
        });
        s.annoncer(tournoi, surTable(new Match(hugo, max)), 2);
        var r = s.diffuserSuivante().orElseThrow();
        assertFalse(r.dite());
        assertFalse(r.motif().isBlank());
    }
}
