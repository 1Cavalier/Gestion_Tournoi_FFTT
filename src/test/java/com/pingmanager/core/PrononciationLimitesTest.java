package com.pingmanager.core;

import com.pingmanager.model.Joueur;
import com.pingmanager.model.Match;
import com.pingmanager.model.Sexe;
import org.junit.jupiter.api.Test;

import java.text.Normalizer;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/** B018 : cas limites de la validation, du registre et du résolveur (vérification du cœur). */
class PrononciationLimitesTest {

    private static boolean ok(String s) {
        return ValidationPrononciation.verifier(s).valide();
    }

    @Test
    void ipa_reel_du_francais_avec_ligatures_accents_tonique_et_longueur() {
        assertTrue(ok("ˈbʁy.nwa"));
        assertTrue(ok("ˌɛ̃.fi.ˈni"));
        assertTrue(ok("aː"));
        assertTrue(ok("œ̃ ɔ̃ ɑ̃ ɛ̃ ø œ ə ɥ ɲ ŋ ʃ ʒ ʁ"));
        assertTrue(ok("a‿b | c"));
        assertTrue(ok("t͡ʃ"));          // diacritique de liaison U+0361
    }

    @Test
    void nasale_en_nfd_et_nfc_donne_le_meme_resultat() {
        String nfd = Normalizer.normalize("ɑ̃", Normalizer.Form.NFD);
        String nfc = Normalizer.normalize("ɑ̃", Normalizer.Form.NFC);
        assertEquals(ValidationPrononciation.verifier(nfc), ValidationPrononciation.verifier(nfd));
    }

    @Test
    void lettre_accentuee_precomposee_et_decomposee_traitees_pareil() {
        // e + accent aigu (NFD) est accepté ; é précomposé (NFC) doit l'être aussi, ou les deux refusés.
        boolean nfd = ok("e\u0301");
        boolean nfc = ok("\u00E9");
        assertEquals(nfd, nfc, "é en NFD et en NFC doivent donner le même verdict");
    }

    @Test
    void deux_diacritiques_sur_un_meme_symbole() {
        assertTrue(ok("ɔ\u0303\u0308"), "plusieurs diacritiques combinants sur un symbole sont de l'API valide");
    }

    @Test
    void frontieres_de_longueur_en_points_de_code() {
        assertTrue(ok("a".repeat(60)));
        assertFalse(ok("a".repeat(61)));
        assertTrue(ok("ɑ\u0303".repeat(30)));   // 60 points de code
        assertFalse(ok("ɑ\u0303".repeat(31)));
    }

    @Test
    void espaces_exotiques_tabulations_et_sauts_de_ligne() {
        assertEquals("a b", ValidationPrononciation.verifier("  a \t\n  b ").ipa());
        assertFalse(ok("a\u00A0b"), "espace insécable refusée");
        assertFalse(ok("a\u0000b"));
        assertFalse(ok("a\u202Eb"));
        assertFalse(ok("\uD83D\uDE00"));
        assertTrue(ValidationPrononciation.verifier("   \t ").estEffacement());
        assertTrue(ValidationPrononciation.verifier(null).estEffacement());
    }

    @Test
    void aucun_caractere_dangereux_n_est_jamais_autorise() {
        for (int c = 0; c < 0x3000; c++) {
            if (ValidationPrononciation.estAutorise(c)) {
                assertTrue("<>&\"';=/".indexOf(c) < 0, "autorisé à tort : U+" + Integer.toHexString(c));
                assertFalse(Character.isISOControl(c), "contrôle autorisé : U+" + Integer.toHexString(c));
                assertFalse(Character.isDigit(c));
            }
        }
    }

    @Test
    void ssml_phoneme_bien_forme_meme_si_le_resolveur_renvoie_n_importe_quoi() {
        Match m = new Match(new Joueur("1", "A", "B", Sexe.HOMME, "C", 500),
                new Joueur("2", "D", "E", Sexe.HOMME, "F", 500));
        String ssml = GenerateurSsml.generer(TexteAnnonceVocale.composer(m, 1, null), 1,
                s -> Optional.of("x\"><break time=\"9s\"/><a b='"));
        assertFalse(ssml.contains("<break"), ssml);
        assertFalse(ssml.contains("ph=\"x\"><"), ssml);
    }

    // --- registre ---

    @Test
    void licence_avec_espaces_et_sans_est_la_meme_entree() {
        RegistrePrononciations r = new RegistrePrononciations();
        r.definirJoueur(" 94 ", "a", "");
        r.definirJoueur("94", "b", "");
        assertEquals(1, r.tousLesJoueurs().size());
        assertEquals("b", r.joueur("94 ").orElseThrow().prenom());
    }

    @Test
    void valeurs_nulles_dans_le_registre() {
        RegistrePrononciations r = new RegistrePrononciations();
        assertFalse(r.definirJoueur(null, "a", "b").valide());
        assertFalse(r.definirJoueur("  ", "a", "b").valide());
        assertFalse(r.definirClub(null, "x", "a").valide());
        assertTrue(r.definirJoueur("1", null, null).estEffacement());
        assertTrue(r.estVide());
        assertTrue(r.joueur(null).isEmpty());
        assertTrue(r.club(null).isEmpty());
        assertTrue(r.definirClub("5", null, "te.te").valide());
        assertEquals("", r.club("5").orElseThrow().nom());
    }

    @Test
    void effacer_prenom_garde_le_nom_et_effacer_les_deux_supprime() {
        RegistrePrononciations r = new RegistrePrononciations();
        r.definirJoueur("1", "a", "b");
        r.definirJoueur("1", "", "b");
        assertEquals("", r.joueur("1").orElseThrow().prenom());
        r.definirJoueur("1", " ", "");
        assertTrue(r.joueur("1").isEmpty());
        assertTrue(r.estVide());
    }

    @Test
    void liste_rendue_est_une_copie() {
        RegistrePrononciations r = new RegistrePrononciations();
        r.definirJoueur("1", "a", "");
        r.tousLesJoueurs().clear();
        assertEquals(1, r.tousLesJoueurs().size());
    }

    @Test
    void remplacement_garde_la_place_et_club_remplace_son_nom() {
        RegistrePrononciations r = new RegistrePrononciations();
        r.definirClub("5", "Ancien", "a");
        r.definirClub("5", "Nouveau", "b");
        assertEquals(1, r.tousLesClubs().size());
        assertEquals("Nouveau", r.club("5").orElseThrow().nom());
    }

    // --- résolveur ---

    private static Match match() {
        return new Match(new Joueur("1", "NGUYEN", "Thi", Sexe.FEMME, "TT Brunoy", 1200),
                new Joueur("2", "DURAND", "Léa", Sexe.FEMME, "US Évry", 1100));
    }

    @Test
    void joueur_qui_change_de_club_utilise_le_club_courant() {
        RegistrePrononciations r = new RegistrePrononciations();
        r.definirClub("A", "Ancien", "an.sjɛ̃");
        r.definirClub("B", "Nouveau", "nu.vo");
        AtomicReference<String> club = new AtomicReference<>("A");
        ResolveurPrononciationRegistre res = new ResolveurPrononciationRegistre(r, l -> Optional.of(club.get()));
        String avant = GenerateurSsml.generer(TexteAnnonceVocale.composer(match(), 1, null), 1, res);
        assertTrue(avant.contains("an.sjɛ̃"));
        club.set("B");
        String apres = GenerateurSsml.generer(TexteAnnonceVocale.composer(match(), 1, null), 1, res);
        assertTrue(apres.contains("nu.vo") && !apres.contains("an.sjɛ̃"));
    }

    @Test
    void fonction_de_club_nulle_ou_renvoyant_null_ou_levant_ne_casse_pas_l_annonce() {
        RegistrePrononciations r = new RegistrePrononciations();
        r.definirJoueur("1", "", "ŋɡu.jɛn");
        r.definirClub("A", "x", "a");
        for (java.util.function.Function<String, Optional<String>> f : java.util.List.<java.util.function.Function<String, Optional<String>>>of(
                l -> null, l -> { throw new IllegalStateException("base"); })) {
            String s = GenerateurSsml.generer(TexteAnnonceVocale.composer(match(), 1, null), 1,
                    new ResolveurPrononciationRegistre(r, f));
            assertTrue(s.endsWith("</speak>"));
            assertTrue(s.contains("ŋɡu.jɛn"), "le nom du joueur reste prononcé");
        }
        assertDoesNotThrow(() -> GenerateurSsml.generer(TexteAnnonceVocale.composer(match(), 1, null), 1,
                new ResolveurPrononciationRegistre(r, null)));
    }

    @Test
    void licence_du_registre_avec_espaces_retrouvee_depuis_la_licence_du_joueur() {
        RegistrePrononciations r = new RegistrePrononciations();
        r.definirJoueur("1", "", "ŋɡu.jɛn");
        ResolveurPrononciationRegistre res = new ResolveurPrononciationRegistre(r, l -> Optional.empty());
        assertTrue(GenerateurSsml.generer(TexteAnnonceVocale.composer(match(), 1, null), 1, res).contains("ŋɡu.jɛn"));
    }
}
