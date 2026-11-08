package com.pingmanager.core;

import com.pingmanager.model.Joueur;
import com.pingmanager.model.Match;
import com.pingmanager.model.Sexe;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** B017 (vérification) : SSML, échappement complet, noms extrêmes, texte d'annonce. */
class GenerateurSsmlLimitesTest {

    private static Joueur j(String lic, String nom, String prenom, String club) {
        return new Joueur(lic, nom, prenom, Sexe.HOMME, club, 1);
    }

    @Test
    void les_cinq_caracteres_sont_echappes_dans_l_ordre_sans_double_echappement() {
        assertEquals("&amp;lt; &lt;b&gt; &quot;x&quot; &apos;y&apos; &amp;amp;",
                GenerateurSsml.echapperXml("&lt; <b> \"x\" 'y' &amp;"));
        assertEquals("", GenerateurSsml.echapperXml(null));
        assertEquals("", GenerateurSsml.echapperXml(""));
    }

    @Test
    void caracteres_de_controle_nul_et_non_caracteres_remplaces_emoji_conserve() {
        String r = GenerateurSsml.echapperXml("a\u0000b\u0001c\u001Fd\u007Fe￾f￿g h😀i\uD83Dj\uDE00k");
        assertFalse(r.contains("\u0000") || r.contains("\u0001") || r.contains("\u001F")
                || r.contains("￾") || r.contains("￿"));
        assertTrue(r.contains("😀"), "paire de substituts valide conservée");
        assertTrue(r.contains("i j") || r.contains("i\uD83Dj") == false, r);
        assertEquals(-1, r.indexOf("\uD83Dj"));
        // Le résultat doit être du XML bien formé.
        assertDoesNotThrow(() -> javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new java.io.ByteArrayInputStream(("<a>" + r + "</a>").getBytes(java.nio.charset.StandardCharsets.UTF_8))));
    }

    @Test
    void document_complet_bien_forme_pour_des_noms_hostiles_et_tres_longs() throws Exception {
        String long1 = "A".repeat(100_000);
        Joueur a = j("1", "</s></speak><x a='1'>&\"" + long1, "Zoë\u0000", "Club <&> \"'\u0007");
        Joueur b = j("2", "O'BRIEN-LE GALL", "", "");
        String ssml = GenerateurSsml.generer(TexteAnnonceVocale.composer(new Match(a, b), 12, null), 3, null);
        var doc = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new java.io.ByteArrayInputStream(ssml.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        assertEquals("speak", doc.getDocumentElement().getLocalName() == null
                ? doc.getDocumentElement().getNodeName() : doc.getDocumentElement().getLocalName());
        assertEquals(3, doc.getElementsByTagName("s").getLength());
        assertEquals(0, doc.getElementsByTagName("x").getLength(), "aucune balise injectée");
    }

    @Test
    void diffusions_nulles_ou_negatives_disent_au_moins_une_fois() {
        var annonce = TexteAnnonceVocale.composer(new Match(j("1", "A", "a", "c"), j("2", "B", "b", "c")), 1, null);
        for (int n : new int[] {Integer.MIN_VALUE, -1, 0, 1}) {
            String s = GenerateurSsml.generer(annonce, n, null);
            assertEquals(1, s.split("<s>", -1).length - 1);
            assertFalse(s.contains("<break"));
        }
    }

    @Test
    void exempt_a_determiner_et_noms_composes_dans_le_texte() {
        var ex = TexteAnnonceVocale.composer(new Match(j("1", "DE LA FONTAINE", "Jean-Pierre", "TT Brunoy"),
                Joueur.aDeterminer()), 2, null);
        StringBuilder sb = new StringBuilder();
        ex.segments().forEach(s -> sb.append(s.texte()));
        assertEquals("Jean-Pierre De La Fontaine, club TT Brunoy, contre joueur à déterminer, table 2.", sb.toString());
        var compose = TexteAnnonceVocale.composer(new Match(j("1", "LE-GALL D'ARTOIS", "Anne", "  "),
                j("2", "  Van   Der  Berg ", "  ", null == null ? "x" : "y")), 20, null);
        StringBuilder t = new StringBuilder();
        compose.segments().forEach(s -> t.append(s.texte()));
        assertEquals("Anne Le-Gall D'Artois, contre Van Der Berg, club x, table 20.", t.toString());
    }

    @Test
    void nom_vide_ne_produit_pas_de_segment_vide_incoherent() {
        var a = TexteAnnonceVocale.composer(new Match(j("1", "", "", ""), j("2", "B", "b", "")), 1, null);
        StringBuilder t = new StringBuilder();
        a.segments().forEach(s -> t.append(s.texte()));
        assertTrue(t.toString().contains("contre"));
        assertDoesNotThrow(() -> GenerateurSsml.generer(a, 2, null));
    }
}
