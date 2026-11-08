package com.pingmanager.core;

import com.pingmanager.model.Joueur;
import com.pingmanager.model.Match;
import com.pingmanager.model.Sexe;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResolveurPrononciationRegistreTest {

    private final RegistrePrononciations registre = new RegistrePrononciations();
    private final Map<String, String> clubDuJoueur = Map.of("1", "08910001", "2", "08910002");
    private final ResolveurPrononciationRegistre resolveur = new ResolveurPrononciationRegistre(registre,
            l -> Optional.ofNullable(clubDuJoueur.get(l)));

    private final Match match = new Match(
            new Joueur("1", "NGUYEN", "Thi", Sexe.FEMME, "TT Brunoy", 1200),
            new Joueur("2", "DURAND", "Léa", Sexe.FEMME, "US Évry", 1100));

    private String ssml() {
        return GenerateurSsml.generer(TexteAnnonceVocale.composer(match, 4, null), 1, resolveur);
    }

    @Test
    void sans_entree_ssml_identique_au_texte_normal() {
        assertEquals(GenerateurSsml.generer(TexteAnnonceVocale.composer(match, 4, null), 1,
                ResolveurPrononciation.TEXTE_NORMAL), ssml());
        assertFalse(ssml().contains("<phoneme"));
    }

    @Test
    void prononciation_du_joueur_par_sa_licence_en_balise_phoneme() {
        registre.definirJoueur("1", "", "ŋɡu.jɛn");
        String ssml = ssml();
        assertTrue(ssml.contains("Thi <phoneme alphabet=\"ipa\" ph=\"ŋɡu.jɛn\">Nguyen</phoneme>"), ssml);
        assertTrue(ssml.contains("Léa Durand"), "l'autre joueur reste en texte normal : " + ssml);
    }

    @Test
    void prononciation_du_second_joueur_ne_touche_pas_le_premier() {
        registre.definirJoueur("2", "le.a", "dy.ʁɑ̃");
        String ssml = ssml();
        assertTrue(ssml.contains("<phoneme alphabet=\"ipa\" ph=\"le.a\">Léa</phoneme> "
                + "<phoneme alphabet=\"ipa\" ph=\"dy.ʁɑ̃\">Durand</phoneme>"), ssml);
        assertTrue(ssml.contains("Thi Nguyen"), ssml);
    }

    @Test
    void prononciation_du_club_par_son_numero_retrouve_depuis_la_licence() {
        registre.definirClub("08910002", "US Évry", "y.ɛs e.vʁi");
        String ssml = ssml();
        assertTrue(ssml.contains("club <phoneme alphabet=\"ipa\" ph=\"y.ɛs e.vʁi\">US Évry</phoneme>"), ssml);
        assertTrue(ssml.contains("club TT Brunoy"), ssml);
    }

    @Test
    void club_inconnu_ou_rappel_sans_effet_de_bord() {
        registre.definirClub("99999999", "Autre", "o.tʁ");
        assertFalse(ssml().contains("<phoneme"));
        registre.definirJoueur("1", "ti", "");
        AnnonceVocale rappel = TexteAnnonceVocale.composer(match, 4, null).enRappel();
        String ssml = GenerateurSsml.generer(rappel, 1, resolveur);
        assertTrue(ssml.contains("Rappel : <phoneme alphabet=\"ipa\" ph=\"ti\">Thi</phoneme>"), ssml);
    }

    @Test
    void sans_licence_rien_n_est_personnalise() {
        registre.definirJoueur("1", "ti", "ŋɡu.jɛn");
        assertTrue(resolveur.phonemesIpa(new SegmentAnnonce("Thi", SegmentAnnonce.Nature.PRENOM)).isEmpty());
        assertTrue(resolveur.phonemesIpa(new SegmentAnnonce("Thi", SegmentAnnonce.Nature.PRENOM), "").isEmpty());
        assertTrue(new ResolveurPrononciationRegistre(registre, null)
                .phonemesIpa(new SegmentAnnonce("TT Brunoy", SegmentAnnonce.Nature.CLUB), "1").isEmpty());
    }

    @Test
    void annonce_garde_la_licence_de_chaque_morceau() {
        AnnonceVocale a = TexteAnnonceVocale.composer(match, 4, ContexteConvocation.tour("Tableau A", "Finale"));
        List<SegmentAnnonce> s = a.segments();
        for (int i = 0; i < s.size(); i++) {
            SegmentAnnonce.Nature n = s.get(i).nature();
            if (n == SegmentAnnonce.Nature.LIBRE) {
                assertEquals("", a.licenceDu(i));
            } else {
                assertEquals(s.get(i).texte().equals("Thi") || s.get(i).texte().equals("Nguyen")
                        || s.get(i).texte().equals("TT Brunoy") ? "1" : "2", a.licenceDu(i));
            }
        }
        assertEquals(s.size(), a.licences().size());
    }

    @Test
    void essai_d_ecoute_avec_prononciation_valide_seulement() {
        String ssml = GenerateurSsml.essai(List.of(new GenerateurSsml.Morceau("Thi", "ti"),
                new GenerateurSsml.Morceau("Nguyen", "ŋ<")));
        assertEquals("<speak version=\"1.0\" xmlns=\"http://www.w3.org/2001/10/synthesis\" xml:lang=\"fr-FR\">"
                + "<s><phoneme alphabet=\"ipa\" ph=\"ti\">Thi</phoneme> Nguyen</s></speak>", ssml);
    }
}
