package com.pingmanager.core;

import com.pingmanager.core.SyntheseVocale.ParametresVoix;
import com.pingmanager.core.SyntheseVocale.ResultatSynthese;
import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Manche;
import com.pingmanager.model.Match;
import com.pingmanager.model.ModeAnnonce;
import com.pingmanager.model.Poule;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.StatutMatch;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/** B017 : file d'annonces vocales, déclenchement, rappel, repli silencieux. */
class ServiceAnnoncesVocalesTest {

    private static final Instant T0 = Instant.parse("2026-11-14T09:00:00Z");

    /** Synthèse simulée : garde les SSML dits et les paramètres de voix. */
    private static final class VoixEnregistree implements SyntheseVocale {
        final List<String> dits = Collections.synchronizedList(new ArrayList<>());
        final List<ParametresVoix> voix = Collections.synchronizedList(new ArrayList<>());
        boolean disponible = true;
        RuntimeException panne;

        @Override
        public boolean estDisponible() {
            return disponible;
        }

        @Override
        public ResultatSynthese dire(String ssml, ParametresVoix v) {
            if (panne != null) {
                throw panne;
            }
            dits.add(ssml);
            voix.add(v);
            return ResultatSynthese.ok();
        }
    }

    private final Joueur hugo = new Joueur("1", "MORIN", "Hugo", Sexe.HOMME, "TT Brunoy", 1200);
    private final Joueur lea = new Joueur("2", "DURAND", "Léa", Sexe.FEMME, "US Évry", 1100);
    private final Joueur max = new Joueur("3", "ROUX", "Max", Sexe.HOMME, "AS Melun", 900);
    private final Joueur zoe = new Joueur("4", "BLANC", "Zoé", Sexe.FEMME, "AS Melun", 900);
    private Instant maintenant;
    private VoixEnregistree voix;
    private ServiceAnnoncesVocales service;
    private Tournoi tournoi;
    private Match m1;
    private Match m2;

    @BeforeEach
    void preparer() {
        maintenant = T0;
        voix = new VoixEnregistree();
        service = new ServiceAnnoncesVocales(() -> maintenant, voix);
        tournoi = new Tournoi("Tournoi de Brunoy", "TT Brunoy", LocalDate.of(2026, 11, 14));
        Tableau c = new Tableau("C", ConfigurationTableau.builder().toutesSeries().build());
        Poule b = new Poule("B", List.of(hugo, lea, max, zoe));
        m1 = new Match(hugo, lea);
        m2 = new Match(max, zoe);
        b.ajouterMatch(m1);
        b.ajouterMatch(m2);
        c.definirPoules(List.of(b));
        tournoi.ajouterTableau(c);
    }

    private void surTable(Match m, int table) {
        m.setNumeroTable(table);
        m.setStatut(StatutMatch.EN_COURS);
    }

    @Test
    void automatique_seulement_avec_appel_vocal_et_option_activee() {
        assertFalse(ServiceAnnoncesVocales.annonceAutomatique(tournoi, ModeAnnonce.APPEL), "option coupée par défaut");
        tournoi.getAnnoncesVocales().setAnnonceAutomatique(true);
        assertTrue(ServiceAnnoncesVocales.annonceAutomatique(tournoi, ModeAnnonce.APPEL));
        assertTrue(ServiceAnnoncesVocales.annonceAutomatique(tournoi, ModeAnnonce.SMS_ET_APPEL));
        assertFalse(ServiceAnnoncesVocales.annonceAutomatique(tournoi, ModeAnnonce.SMS));
        assertFalse(ServiceAnnoncesVocales.annonceAutomatique(tournoi, ModeAnnonce.AUCUNE));
        assertFalse(ServiceAnnoncesVocales.annonceAutomatique(tournoi, null), "mode à demander");

        assertFalse(service.apresLancement(tournoi, m1, 4, ModeAnnonce.SMS).acceptee());
        assertTrue(service.enAttente().isEmpty());
        ResultatDemandeAnnonce r = service.apresLancement(tournoi, m1, 4, ModeAnnonce.APPEL);
        assertTrue(r.acceptee());
        assertEquals(1, r.position());
    }

    @Test
    void une_annonce_a_la_fois_dans_l_ordre_d_arrivee() {
        assertEquals(1, service.annoncer(tournoi, m1, 4).position());
        assertEquals(2, service.annoncer(tournoi, m2, 5).position());
        assertTrue(service.aDiffuser());

        ResultatAnnonceVocale r1 = service.diffuserSuivante().orElseThrow();
        assertTrue(r1.dite());
        assertEquals(m1, r1.annonce().match());
        assertEquals(m2, service.diffuserSuivante().orElseThrow().annonce().match());
        assertTrue(service.diffuserSuivante().isEmpty());
        assertEquals(2, voix.dits.size());
        assertTrue(voix.dits.get(0).contains("table 4."), voix.dits.get(0));
        assertTrue(voix.dits.get(1).contains("table 5."), voix.dits.get(1));
    }

    @Test
    void annoncer_de_nouveau_ne_double_pas_l_annonce_en_attente() {
        service.annoncer(tournoi, m1, 4);
        service.annoncer(tournoi, m2, 5);
        ResultatDemandeAnnonce r = service.annoncer(tournoi, m1, 4);
        assertEquals(2, r.position(), "remise en fin de file, une seule fois");
        assertEquals(List.of(m2, m1), service.enAttente().stream().map(AnnonceVocale::match).toList());
    }

    @Test
    void repetition_et_reglages_du_tournoi_appliques() {
        tournoi.getAnnoncesVocales().setDiffusions(3);
        tournoi.getAnnoncesVocales().setVoix("Microsoft Hortense Desktop");
        tournoi.getAnnoncesVocales().setDebit(-2);
        tournoi.getAnnoncesVocales().setAvecTableauEtTour(true);
        service.annoncer(tournoi, m1, 4);
        service.diffuserSuivante();
        String ssml = voix.dits.get(0);
        assertEquals(3, ssml.split("<s>", -1).length - 1, ssml);
        assertTrue(ssml.contains("<s>Tableau C, poule B. Hugo Morin"), ssml);
        assertEquals(new ParametresVoix("Microsoft Hortense Desktop", -2), voix.voix.get(0));
    }

    @Test
    void refus_motives() {
        assertEquals("Aucune rencontre à annoncer.", service.annoncer(tournoi, null, 4).motif());
        assertEquals("La rencontre n'est sur aucune table.", service.annoncer(tournoi, m1, 0).motif());
        Match inconnu = new Match(hugo, Joueur.aDeterminer());
        assertEquals("Les deux joueurs ne sont pas encore connus.", service.annoncer(tournoi, inconnu, 2).motif());
        m2.definirScore(List.of(new Manche(11, 5), new Manche(11, 5), new Manche(11, 5)));
        m2.setStatut(StatutMatch.TERMINE);
        assertEquals("Cette rencontre est terminée : rien à annoncer.", service.annoncer(tournoi, m2, 3).motif());
        assertTrue(service.enAttente().isEmpty());
    }

    @Test
    void repli_silencieux_si_la_voix_est_indisponible_ou_en_panne() {
        voix.disponible = false;
        service.annoncer(tournoi, m1, 4);
        ResultatAnnonceVocale r = service.diffuserSuivante().orElseThrow();
        assertTrue(r.estAffichee());
        assertEquals("synthèse vocale indisponible sur cet ordinateur", r.motif());
        assertTrue(voix.dits.isEmpty());

        voix.disponible = true;
        voix.panne = new IllegalStateException("carte son absente");
        service.annoncer(tournoi, m2, 5);
        ResultatAnnonceVocale p = service.diffuserSuivante().orElseThrow();
        assertTrue(p.estAffichee());
        assertTrue(p.motif().contains("carte son absente"), p.motif());
        assertFalse(service.aDiffuser(), "la file continue après une panne");
    }

    @Test
    void rappel_au_bout_de_n_minutes_si_la_table_reste_vide() {
        tournoi.getAnnoncesVocales().setRappelMinutes(5);
        surTable(m1, 4);
        service.annoncer(tournoi, m1, 4);
        service.diffuserSuivante();
        assertEquals(T0.plus(Duration.ofMinutes(5)), service.rappelPrevu(m1).orElseThrow());

        maintenant = T0.plus(Duration.ofMinutes(4));
        assertEquals(0, service.programmerRappelsDus((m, t) -> true), "pas encore l'heure");
        maintenant = T0.plus(Duration.ofMinutes(5));
        assertEquals(1, service.programmerRappelsDus((m, t) -> m.getStatut() == StatutMatch.EN_COURS
                && t.equals(m.getNumeroTable())));
        ResultatAnnonceVocale r = service.diffuserSuivante().orElseThrow();
        assertTrue(r.annonce().rappel());
        assertTrue(voix.dits.get(1).contains("<s>Rappel : Hugo Morin"), voix.dits.get(1));
        assertTrue(service.rappelPrevu(m1).isEmpty(), "un seul rappel par annonce");
        maintenant = T0.plus(Duration.ofMinutes(30));
        assertEquals(0, service.programmerRappelsDus((m, t) -> true));
    }

    @Test
    void pas_de_rappel_si_les_joueurs_sont_a_la_table_ou_si_le_match_a_quitte_la_table() {
        tournoi.getAnnoncesVocales().setRappelMinutes(3);
        surTable(m1, 4);
        surTable(m2, 5);
        service.annoncer(tournoi, m1, 4);
        service.annoncer(tournoi, m2, 5);
        service.diffuserSuivante();
        service.diffuserSuivante();
        assertTrue(service.oublier(m1), "« Joueurs à la table »");
        assertFalse(service.oublier(m1));
        m2.setNumeroTable(null); // envoi annulé : la table est libre
        maintenant = T0.plus(Duration.ofMinutes(3));
        assertEquals(0, service.programmerRappelsDus((m, t) -> t.equals(m.getNumeroTable())));
        assertFalse(service.aDiffuser());
        assertTrue(service.rappelPrevu(m2).isEmpty(), "rappel échu abandonné");
    }

    @Test
    void sans_rappel_par_defaut() {
        service.annoncer(tournoi, m1, 4);
        service.diffuserSuivante();
        assertTrue(service.rappelPrevu(m1).isEmpty());
    }

    @Test
    void deux_fils_ne_parlent_jamais_en_meme_temps() throws Exception {
        AtomicInteger enParole = new AtomicInteger();
        AtomicInteger maximum = new AtomicInteger();
        SyntheseVocale lente = new SyntheseVocale() {
            @Override
            public boolean estDisponible() {
                return true;
            }

            @Override
            public ResultatSynthese dire(String ssml, ParametresVoix v) {
                int n = enParole.incrementAndGet();
                maximum.accumulateAndGet(n, Math::max);
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                enParole.decrementAndGet();
                return ResultatSynthese.ok();
            }
        };
        ServiceAnnoncesVocales s = new ServiceAnnoncesVocales(() -> maintenant, lente);
        List<Match> matchs = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            Match m = new Match(new Joueur("a" + i, "A", "a", Sexe.HOMME, "C", 1),
                    new Joueur("b" + i, "B", "b", Sexe.HOMME, "C", 1));
            matchs.add(m);
            s.annoncer(tournoi, m, i + 1);
        }
        CountDownLatch depart = new CountDownLatch(1);
        AtomicInteger dites = new AtomicInteger();
        List<Thread> fils = new ArrayList<>();
        for (int f = 0; f < 3; f++) {
            Thread t = new Thread(() -> {
                try {
                    depart.await();
                } catch (InterruptedException e) {
                    return;
                }
                while (s.diffuserSuivante().isPresent()) {
                    dites.incrementAndGet();
                }
            });
            fils.add(t);
            t.start();
        }
        depart.countDown();
        for (Thread t : fils) {
            t.join(5000);
        }
        assertEquals(6, dites.get());
        assertEquals(1, maximum.get());
    }

    @Test
    void prononciation_personnalisee_branchee_par_le_point_d_accroche() {
        service.definirResolveur(s -> s.texte().equals("Morin") ? java.util.Optional.of("mɔ.ʁɛ̃")
                : java.util.Optional.empty());
        service.annoncer(tournoi, m1, 4);
        service.diffuserSuivante();
        assertTrue(voix.dits.get(0).contains("<phoneme alphabet=\"ipa\" ph=\"mɔ.ʁɛ̃\">Morin</phoneme>"),
                voix.dits.get(0));
        service.definirResolveur(null);
        service.annoncer(tournoi, m1, 4);
        service.diffuserSuivante();
        assertFalse(voix.dits.get(1).contains("<phoneme"));
    }

    @Test
    void tournoi_relu_depuis_le_fichier_meme_rencontre_reconnue_par_les_licences() {
        tournoi.getAnnoncesVocales().setRappelMinutes(5);
        service.annoncer(tournoi, m1, 4);
        service.diffuserSuivante();
        Match relu = new Match(new Joueur("1", "MORIN", "Hugo", Sexe.HOMME, "TT Brunoy", 1200),
                new Joueur("2", "DURAND", "Léa", Sexe.FEMME, "US Évry", 1100));
        assertTrue(ServiceAnnoncesVocales.memeRencontre(m1, relu));
        assertFalse(ServiceAnnoncesVocales.memeRencontre(m1, m2));
        assertFalse(ServiceAnnoncesVocales.memeRencontre(m1, new Match(hugo, Joueur.aDeterminer())));
        assertTrue(service.rappelPrevu(relu).isPresent());
        assertTrue(service.oublier(relu), "« Joueurs à la table » après relecture");
        assertTrue(service.rappelPrevu(m1).isEmpty());
    }
}
