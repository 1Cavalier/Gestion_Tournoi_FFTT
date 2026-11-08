package com.pingmanager.core;

import com.pingmanager.model.Joueur;
import com.pingmanager.model.Match;
import com.pingmanager.model.Sexe;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** B017 : SSML de l'annonce, échappement XML et point d'accroche phonétique (B018). */
class GenerateurSsmlTest {

    private static Joueur j(String licence, String nom, String prenom, String club) {
        return new Joueur(licence, nom, prenom, Sexe.HOMME, club, 1000);
    }

    @Test
    void echappement_xml_des_cinq_caracteres_speciaux() {
        assertEquals("&lt;b&gt; &amp; &quot;x&quot; l&apos;as", GenerateurSsml.echapperXml("<b> & \"x\" l'as"));
        assertEquals("", GenerateurSsml.echapperXml(null));
        assertEquals("Léa Ñúñez 漢字", GenerateurSsml.echapperXml("Léa Ñúñez 漢字"));
    }

    @Test
    void caracteres_interdits_en_xml_remplaces_par_une_espace() {
        assertEquals("a b c", GenerateurSsml.echapperXml("a\u0000b\u001Bc"));
        assertEquals("x y", GenerateurSsml.echapperXml("x\uD800y"), "moitié de paire isolée");
        assertEquals("🏓", GenerateurSsml.echapperXml("🏓"), "paire complète gardée");
    }

    @Test
    void nom_malveillant_ne_peut_pas_injecter_de_balise() {
        Match m = new Match(j("1", "</s></speak><audio src=\"x\"/>", "Eve", "Club & Co"), j("2", "DURAND", "Léa", "C"));
        String ssml = GenerateurSsml.generer(TexteAnnonceVocale.composer(m, 3, null), 1, null);
        assertFalse(ssml.contains("<audio"), ssml);
        assertTrue(ssml.contains("&lt;/s&gt;&lt;/speak&gt;&lt;audio src=&quot;x&quot;/&gt;"), ssml);
        assertTrue(ssml.contains("Club &amp; Co"), ssml);
        assertEquals(1, ssml.split("</speak>", -1).length - 1, "un seul document");
    }

    @Test
    void document_speak_francais_et_diffusions_separees_par_un_silence() {
        Match m = new Match(j("1", "MORIN", "Hugo", "TT Brunoy"), j("2", "DURAND", "Léa", "US Évry"));
        String ssml = GenerateurSsml.generer(TexteAnnonceVocale.composer(m, 4, null), 2,
                ResolveurPrononciation.TEXTE_NORMAL);
        assertTrue(ssml.startsWith("<speak version=\"1.0\" xmlns=\"http://www.w3.org/2001/10/synthesis\" "
                + "xml:lang=\"fr-FR\">"), ssml);
        String phrase = "<s>Hugo Morin, club TT Brunoy, contre Léa Durand, club US Évry, table 4.</s>";
        assertEquals("<speak version=\"1.0\" xmlns=\"http://www.w3.org/2001/10/synthesis\" xml:lang=\"fr-FR\">"
                + phrase + GenerateurSsml.SILENCE_ENTRE_DIFFUSIONS + phrase + "</speak>", ssml);
        assertFalse(ssml.contains("<phoneme"), "par défaut : texte normal");
    }

    @Test
    void prononciation_personnalisee_en_balise_phoneme_ipa_echappee() {
        Match m = new Match(j("1", "NGUYEN", "Thi", "TT Brunoy"), j("2", "DURAND", "Léa", "US Évry"));
        ResolveurPrononciation r = s -> s.nature() == SegmentAnnonce.Nature.NOM && s.texte().equals("Nguyen")
                ? Optional.of("ŋwjɛn\"<") : Optional.empty();
        String ssml = GenerateurSsml.generer(TexteAnnonceVocale.composer(m, 1, null), 1, r);
        assertTrue(ssml.contains("<phoneme alphabet=\"ipa\" ph=\"ŋwjɛn&quot;&lt;\">Nguyen</phoneme>"), ssml);
        assertTrue(ssml.contains("Durand"), ssml);
    }

    @Test
    void resolveur_defaillant_ou_vide_retombe_sur_le_texte_normal() {
        Match m = new Match(j("1", "MORIN", "Hugo", "A"), j("2", "DURAND", "Léa", "B"));
        AnnonceVocale a = TexteAnnonceVocale.composer(m, 1, null);
        String attendu = GenerateurSsml.generer(a, 1, null);
        assertEquals(attendu, GenerateurSsml.generer(a, 1, s -> {
            throw new IllegalStateException("panne");
        }));
        assertEquals(attendu, GenerateurSsml.generer(a, 1, s -> Optional.of("  ")));
        assertEquals(attendu, GenerateurSsml.generer(a, 1, s -> null));
    }
}
