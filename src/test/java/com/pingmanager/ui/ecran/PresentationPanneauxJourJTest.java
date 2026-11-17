package com.pingmanager.ui.ecran;

import com.pingmanager.model.ModeAnnonce;
import com.pingmanager.model.StrategieOrdonnancement;
import com.pingmanager.model.Tournoi;
import com.pingmanager.persistence.DepotTournoi;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class PresentationPanneauxJourJTest {

    private static Tournoi tournoi() {
        return new Tournoi("Open", "TT Brunoy", LocalDate.of(2027, 4, 10));
    }

    // ------------------------------------------------------------------
    // Annonces
    // ------------------------------------------------------------------

    @Test
    void quatre_modes_dans_l_ordre_voix_sms_voix_et_sms_aucune() {
        List<PresentationPanneauxJourJ.OptionMode> o = PresentationPanneauxJourJ.optionsMode(tournoi(), true);
        assertEquals(List.of("Voix", "SMS", "Voix + SMS", "Aucune annonce"),
                o.stream().map(PresentationPanneauxJourJ.OptionMode::nom).toList());
        assertEquals(List.of(ModeAnnonce.APPEL, ModeAnnonce.SMS, ModeAnnonce.SMS_ET_APPEL, ModeAnnonce.AUCUNE),
                o.stream().map(PresentationPanneauxJourJ.OptionMode::mode).toList());
        o.forEach(x -> assertFalse(x.phrase().isBlank()));
    }

    @Test
    void chaque_mode_dit_ce_qui_se_passe_au_lancement() {
        assertEquals("Le haut-parleur appelle les deux joueurs et annonce la table.",
                TextesPanneauxJourJ.phraseMode(ModeAnnonce.APPEL));
        assertTrue(TextesPanneauxJourJ.phraseMode(ModeAnnonce.SMS).contains("SMS"));
        assertTrue(TextesPanneauxJourJ.phraseMode(ModeAnnonce.SMS_ET_APPEL).contains("haut-parleur"));
        assertTrue(TextesPanneauxJourJ.phraseMode(ModeAnnonce.SMS_ET_APPEL).contains("SMS"));
        assertTrue(TextesPanneauxJourJ.phraseMode(ModeAnnonce.AUCUNE).contains("vous-même"));
    }

    @Test
    void seul_le_mode_enregistre_est_marque_actif_et_aucun_sans_choix() {
        Tournoi t = tournoi();
        assertTrue(PresentationPanneauxJourJ.optionsMode(t, true).stream().noneMatch(
                PresentationPanneauxJourJ.OptionMode::active));
        t.setModeAnnonce(ModeAnnonce.SMS);
        List<PresentationPanneauxJourJ.OptionMode> o = PresentationPanneauxJourJ.optionsMode(t, true);
        assertEquals(1, o.stream().filter(PresentationPanneauxJourJ.OptionMode::active).count());
        assertTrue(o.get(1).active());
    }

    @Test
    void un_mode_signale_le_reglage_automatique_a_activer() {
        Tournoi t = tournoi();
        t.getAnnoncesVocales().setAnnonceAutomatique(false);
        t.getConvocationsSms().setEnvoiAutomatique(false);
        List<PresentationPanneauxJourJ.OptionMode> o = PresentationPanneauxJourJ.optionsMode(t, true);
        assertEquals(List.of(TextesPanneauxJourJ.MODE_VOIX_A_ACTIVER), o.get(0).remarques());
        assertEquals(List.of(TextesPanneauxJourJ.MODE_SMS_A_ACTIVER), o.get(1).remarques());
        assertEquals(2, o.get(2).remarques().size());
        assertTrue(o.get(3).remarques().isEmpty());

        t.getAnnoncesVocales().setAnnonceAutomatique(true);
        t.getConvocationsSms().setEnvoiAutomatique(true);
        PresentationPanneauxJourJ.optionsMode(t, true).forEach(x -> assertTrue(x.remarques().isEmpty()));
    }

    @Test
    void sans_voix_sur_l_ordinateur_le_mode_voix_le_dit() {
        Tournoi t = tournoi();
        t.getAnnoncesVocales().setAnnonceAutomatique(true);
        List<PresentationPanneauxJourJ.OptionMode> o = PresentationPanneauxJourJ.optionsMode(t, false);
        assertTrue(o.get(0).remarques().contains(TextesPanneauxJourJ.MODE_VOIX_ECRAN));
        assertFalse(o.get(1).remarques().contains(TextesPanneauxJourJ.MODE_VOIX_ECRAN));
    }

    @Test
    void blocs_voix_et_sms_utilises_selon_le_mode() {
        assertTrue(PresentationPanneauxJourJ.voixUtilisee(ModeAnnonce.APPEL));
        assertFalse(PresentationPanneauxJourJ.smsUtilise(ModeAnnonce.APPEL));
        assertFalse(PresentationPanneauxJourJ.voixUtilisee(ModeAnnonce.SMS));
        assertTrue(PresentationPanneauxJourJ.smsUtilise(ModeAnnonce.SMS));
        assertTrue(PresentationPanneauxJourJ.voixUtilisee(ModeAnnonce.SMS_ET_APPEL));
        assertTrue(PresentationPanneauxJourJ.smsUtilise(ModeAnnonce.SMS_ET_APPEL));
        assertFalse(PresentationPanneauxJourJ.voixUtilisee(ModeAnnonce.AUCUNE));
        assertFalse(PresentationPanneauxJourJ.smsUtilise(ModeAnnonce.AUCUNE));
        // Sans mode choisi, la question est posée à chaque match : les deux peuvent servir.
        assertTrue(PresentationPanneauxJourJ.voixUtilisee(null));
        assertTrue(PresentationPanneauxJourJ.smsUtilise(null));
    }

    @Test
    void exemple_de_sms_avec_la_table_et_l_adversaire() {
        String s = PresentationPanneauxJourJ.exempleSms();
        assertTrue(s.contains("table 4"), s);
        assertTrue(s.contains("Lucas"), s);
        assertTrue(s.toUpperCase().contains("DURAND"), s);
    }

    @Test
    void heure_d_une_annonce_de_l_historique() {
        assertEquals("09:05", PresentationPanneauxJourJ.heure(Instant.parse("2026-11-14T09:05:30Z"), ZoneOffset.UTC));
    }

    @Test
    void message_apres_changement_de_mode_avec_le_nom_affiche() {
        assertEquals("Mode d'annonce : Voix + SMS. Choix enregistré pour ce tournoi.",
                TextesPanneauxJourJ.modeEnregistre(ModeAnnonce.SMS_ET_APPEL));
        assertTrue(TextesPanneauxJourJ.modeEnregistre(null).contains("à chaque match"));
    }

    // ------------------------------------------------------------------
    // Stratégie
    // ------------------------------------------------------------------

    @Test
    void trois_cartes_completes_une_par_strategie() {
        List<PresentationPanneauxJourJ.CarteStrategie> c = PresentationPanneauxJourJ.cartesStrategie(tournoi());
        assertEquals(StrategieOrdonnancement.values().length, c.size());
        for (PresentationPanneauxJourJ.CarteStrategie x : c) {
            for (String s : List.of(x.nom(), x.typeTournoi(), x.tables(), x.objectif(), x.enSalle(), x.avantage(),
                    x.inconvenient())) {
                assertFalse(s.isBlank(), x.strategie().name());
            }
        }
        // Textes différents d'une stratégie à l'autre.
        assertEquals(3, c.stream().map(PresentationPanneauxJourJ.CarteStrategie::enSalle).distinct().count());
        assertEquals(3, c.stream().map(PresentationPanneauxJourJ.CarteStrategie::avantage).distinct().count());
    }

    @Test
    void sans_choix_la_strategie_par_defaut_est_en_place_et_se_confirme_sans_question() {
        List<PresentationPanneauxJourJ.CarteStrategie> c = PresentationPanneauxJourJ.cartesStrategie(tournoi());
        PresentationPanneauxJourJ.CarteStrategie defaut = c.stream()
                .filter(x -> x.strategie() == StrategieOrdonnancement.PAR_DEFAUT).findFirst().orElseThrow();
        assertTrue(defaut.active());
        assertFalse(defaut.choisie());
        assertEquals(TextesPanneauxJourJ.STRATEGIE_PAR_DEFAUT, defaut.marque());
        assertEquals(TextesPanneauxJourJ.STRATEGIE_CONFIRMER_DEFAUT, defaut.bouton());
        assertTrue(defaut.boutonActif());
        assertFalse(defaut.aConfirmer());
        assertEquals(1, c.stream().filter(PresentationPanneauxJourJ.CarteStrategie::active).count());
    }

    @Test
    void la_strategie_choisie_est_marquee_et_les_autres_demandent_confirmation() {
        Tournoi t = tournoi();
        t.setStrategieOrdonnancement(StrategieOrdonnancement.AVANCER_TABLEAUX_COMMENCES);
        for (PresentationPanneauxJourJ.CarteStrategie x : PresentationPanneauxJourJ.cartesStrategie(t)) {
            if (x.strategie() == StrategieOrdonnancement.AVANCER_TABLEAUX_COMMENCES) {
                assertEquals(TextesPanneauxJourJ.STRATEGIE_EN_PLACE, x.marque());
                assertFalse(x.boutonActif());
            } else {
                assertNull(x.marque());
                assertEquals(TextesPanneauxJourJ.STRATEGIE_CHOISIR, x.bouton());
                assertTrue(x.boutonActif());
                assertTrue(x.aConfirmer());
            }
        }
    }

    @Test
    void la_confirmation_rappelle_que_les_matchs_en_cours_ne_changent_pas() {
        String c = TextesPanneauxJourJ.confirmationChangement(StrategieOrdonnancement.MINIMISER_ATTENTE);
        assertTrue(c.contains(StrategieOrdonnancement.MINIMISER_ATTENTE.getLibelle()));
        assertTrue(c.contains("prochains matchs proposés"));
        assertTrue(c.contains("déjà sur une table ne changent pas"));
    }

    @Test
    void mode_et_strategie_choisis_dans_les_panneaux_sont_enregistres_et_relus() throws Exception {
        Tournoi t = tournoi();
        Path fichier = Files.createTempFile("panneaux", DepotTournoi.EXTENSION);
        try {
            t.setModeAnnonce(ModeAnnonce.SMS);
            PresentationSelecteurStrategie.ResultatChoixStrategie r = PresentationSelecteurStrategie.choisir(t,
                    StrategieOrdonnancement.FAIRE_AVANCER_PETIT_A_PETIT, x -> DepotTournoi.sauvegarder(x, fichier));
            assertTrue(r.enregistre());
            Tournoi relu = DepotTournoi.charger(fichier).getTournoi();
            assertEquals(ModeAnnonce.SMS, relu.getModeAnnonce());
            assertTrue(PresentationPanneauxJourJ.optionsMode(relu, true).get(1).active());
            PresentationPanneauxJourJ.CarteStrategie active = PresentationPanneauxJourJ.cartesStrategie(relu).stream()
                    .filter(PresentationPanneauxJourJ.CarteStrategie::active).findFirst().orElseThrow();
            assertEquals(StrategieOrdonnancement.FAIRE_AVANCER_PETIT_A_PETIT, active.strategie());
            assertTrue(active.choisie());
        } finally {
            Files.deleteIfExists(fichier);
        }
    }

    // ------------------------------------------------------------------
    // Rédaction : pas de terme technique interne dans les textes affichés
    // ------------------------------------------------------------------

    @Test
    void aucun_nom_de_classe_ni_terme_technique_dans_les_textes() throws Exception {
        List<String> textes = new ArrayList<>();
        for (Field f : TextesPanneauxJourJ.class.getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers()) && f.getType() == String.class) {
                f.setAccessible(true);
                textes.add((String) f.get(null));
            }
        }
        for (ModeAnnonce m : ModeAnnonce.values()) {
            textes.add(TextesPanneauxJourJ.nomMode(m));
            textes.add(TextesPanneauxJourJ.phraseMode(m));
            textes.add(TextesPanneauxJourJ.modeEnregistre(m));
        }
        for (StrategieOrdonnancement s : StrategieOrdonnancement.values()) {
            textes.addAll(List.of(TextesPanneauxJourJ.typeTournoi(s), TextesPanneauxJourJ.tables(s),
                    TextesPanneauxJourJ.objectif(s), TextesPanneauxJourJ.enSalle(s), TextesPanneauxJourJ.avantage(s),
                    TextesPanneauxJourJ.inconvenient(s), TextesPanneauxJourJ.confirmationChangement(s)));
        }
        // Mot collé façon code (ModeAnnonce, voixUtilisee), constante (SMS_ET_APPEL) ou jargon.
        Pattern code = Pattern.compile("[a-zé][A-Z]|[A-Z]{2,}_|\\b(null|true|false|ordonnanceur|ordonnancement|"
                + "enum|SAPI|SSML)\\b");
        for (String s : textes) {
            assertFalse(code.matcher(s).find(), s);
        }
    }

    @Test
    void les_phrases_des_cartes_de_strategie_restent_courtes() {
        for (StrategieOrdonnancement s : StrategieOrdonnancement.values()) {
            for (String x : List.of(TextesPanneauxJourJ.typeTournoi(s), TextesPanneauxJourJ.tables(s),
                    TextesPanneauxJourJ.objectif(s), TextesPanneauxJourJ.avantage(s),
                    TextesPanneauxJourJ.inconvenient(s))) {
                assertTrue(x.length() <= 110, x);
            }
            assertTrue(TextesPanneauxJourJ.enSalle(s).length() <= 160);
        }
    }
}
