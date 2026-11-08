package com.pingmanager.ui.diffusion;

import com.pingmanager.model.Joueur;
import com.pingmanager.model.Match;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.ui.diffusion.InstantaneSuivi.JoueurPublic;
import com.pingmanager.ui.diffusion.InstantaneSuivi.LigneClassementPublique;
import com.pingmanager.ui.diffusion.InstantaneSuivi.MatchPublic;
import com.pingmanager.ui.diffusion.InstantaneSuivi.PoulePublique;
import com.pingmanager.ui.diffusion.InstantaneSuivi.ProchainePartie;
import com.pingmanager.ui.diffusion.InstantaneSuivi.TableauPublic;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PagesSuiviTest {

    private static final String PIEGE = "<script>alert('x')</script> & \"Co\"";
    private static final LocalDateTime HEURE = LocalDateTime.of(2026, 11, 14, 14, 5);

    /** Instantané fabriqué à la main dont chaque texte contient du HTML hostile. */
    private static InstantaneSuivi piege() {
        MatchPublic m = new MatchPublic(PIEGE, PIEGE, PIEGE, PIEGE, PIEGE, PIEGE, PIEGE, PIEGE, "Table 1", true,
                true, false);
        PoulePublique p = new PoulePublique(PIEGE, List.of(new LigneClassementPublique("1er", PIEGE, PIEGE, PIEGE,
                PIEGE, true)), List.of(m));
        TableauPublic t = new TableauPublic(PIEGE, PIEGE, PIEGE, List.of(p),
                List.of(new InstantaneSuivi.TourPublic(PIEGE, List.of(m))),
                List.of(new InstantaneSuivi.LignePodium("1er", PIEGE, PIEGE)));
        JoueurPublic j = new JoueurPublic("9912345", "dupont jean", PIEGE, PIEGE,
                new ProchainePartie(PIEGE, PIEGE, PIEGE, PIEGE, PIEGE, false), "");
        return new InstantaneSuivi(PIEGE, HEURE, List.of(t), List.of(m), List.of(m), List.of(j));
    }

    private static void assertEchappe(String html) {
        assertFalse(html.contains("<script"), "aucune balise injectée");
        assertFalse(html.contains("\"Co\""), "guillemets échappés");
        assertTrue(html.contains("&lt;script&gt;alert(&#39;x&#39;)&lt;/script&gt; &amp; &quot;Co&quot;"));
    }

    @Test
    void toutes_les_pages_echappent_les_textes_du_tournoi() {
        InstantaneSuivi s = piege();
        assertEchappe(PagesSuivi.accueil(s));
        assertEchappe(PagesSuivi.matchs(s));
        assertEchappe(PagesSuivi.tableau(s, 1));
        assertEchappe(PagesSuivi.partie(s, null, "0"));
    }

    @Test
    void la_recherche_saisie_est_echappee_dans_le_champ_et_le_message() {
        String html = PagesSuivi.partie(piege(), "\"><img src=x onerror=alert(1)>", null);
        assertFalse(html.contains("<img"));
        assertTrue(html.contains("&quot;&gt;&lt;img src=x onerror=alert(1)&gt;"));
    }

    @Test
    void echappement_des_caracteres_speciaux() {
        assertEquals("&lt;a href=&quot;x&quot;&gt;l&#39;été &amp; co&lt;/a&gt;",
                Html.echapper("<a href=\"x\">l'été & co</a>"));
        assertEquals("", Html.echapper(null));
        assertEquals("a b", Html.echapper("a\u0000b"));
    }

    @Test
    void pages_sans_javascript_avec_rafraichissement_automatique_et_charte() {
        String html = PagesSuivi.accueil(piege());
        assertFalse(html.toLowerCase().contains("<script"));
        assertTrue(html.contains("<meta http-equiv=\"refresh\" content=\"" + PagesSuivi.RAFRAICHISSEMENT_SECONDES + "\">"));
        assertTrue(html.contains("name=\"viewport\""));
        assertTrue(html.contains("#1D3FCF"), "bleu roi de la charte v2");
        assertTrue(html.contains("Mis à jour à 14 h 05"));
    }

    @Test
    void seules_get_et_head_sont_acceptees() {
        InstantaneSuivi s = piege();
        for (String methode : List.of("POST", "PUT", "DELETE", "PATCH", "OPTIONS", "TRACE", "CONNECT", "get")) {
            assertEquals(405, PagesSuivi.repondre(methode, "/", null, s).statut(), methode);
        }
        assertEquals(200, PagesSuivi.repondre("GET", "/", null, s).statut());
        assertEquals(200, PagesSuivi.repondre("HEAD", "/matchs", null, s).statut());
    }

    @Test
    void routes_inconnues_ou_parametres_invalides() {
        InstantaneSuivi s = piege();
        assertEquals(404, PagesSuivi.repondre("GET", "/admin", null, s).statut());
        assertEquals(404, PagesSuivi.repondre("GET", "/tableau", "n=0", s).statut());
        assertEquals(404, PagesSuivi.repondre("GET", "/tableau", "n=2", s).statut());
        assertEquals(404, PagesSuivi.repondre("GET", "/tableau", "n=abc", s).statut());
        assertEquals(404, PagesSuivi.repondre("GET", "/tableau", "n=99999999999", s).statut());
        assertEquals(200, PagesSuivi.repondre("GET", "/tableau", "n=1", s).statut());
        assertEquals(414, PagesSuivi.repondre("GET", "/partie", "q=" + "a".repeat(600), s).statut());
        assertEquals(503, PagesSuivi.repondre("GET", "/", null, InstantaneSuivi.vide(HEURE)).statut());
        assertEquals(503, PagesSuivi.repondre("GET", "/", null, null).statut());
        // Identifiant de joueur hors liste : simple formulaire.
        assertEquals(200, PagesSuivi.repondre("GET", "/partie", "id=-1", s).statut());
        assertEquals(200, PagesSuivi.repondre("GET", "/partie", "id=5000", s).statut());
    }

    @Test
    void parametres_bornes() {
        Map<String, String> p = Html.parametres("q=" + "b".repeat(200) + "&id=3");
        assertEquals(Html.LONGUEUR_MAX_PARAMETRE, p.get("q").length());
        assertEquals("3", p.get("id"));
        assertTrue(Html.parametres("q=%zz").isEmpty(), "encodage invalide ignoré");
        assertEquals("jean dupont", Html.parametres("q=jean+dupont").get("q"));
        StringBuilder beaucoup = new StringBuilder();
        for (int i = 0; i < 40; i++) {
            beaucoup.append("p").append(i).append("=1&");
        }
        assertEquals(Html.NOMBRE_MAX_PARAMETRES, Html.parametres(beaucoup.toString()).size());
        assertTrue(Html.parametres("q=" + "x".repeat(Html.LONGUEUR_MAX_REQUETE)).isEmpty());
    }

    // ------------------------------------------------------------------
    // Sur la démo du jour J
    // ------------------------------------------------------------------

    @Test
    void ma_prochaine_partie_sur_la_demo_par_nom_et_par_licence() {
        Tournoi t = InstantaneSuiviTest.demo();
        Match m = InstantaneSuiviTest.enCoursTable(InstantaneSuiviTest.tab(t, "C"), 2);
        Joueur j = m.getJoueurA();
        InstantaneSuivi s = InstantaneSuivi.depuis(t, InstantaneSuiviTest.HORLOGE);

        String parLicence = PagesSuivi.partie(s, j.getNumeroLicence(), null);
        assertTrue(parLicence.contains("Vous jouez en ce moment"), parLicence);
        assertTrue(parLicence.contains("Table 2"));
        assertTrue(parLicence.contains(Html.echapper("Contre " + InstantaneSuivi.nomJoueur(m.getJoueurB()))));

        // Par le nom, sans accents ni majuscules : le joueur figure dans les résultats.
        String nom = InstantaneSuivi.normaliser(InstantaneSuivi.nomJoueur(j));
        List<Integer> trouves = PagesSuivi.rechercher(s, nom.toUpperCase());
        assertFalse(trouves.isEmpty());
        assertTrue(trouves.stream().anyMatch(i -> s.joueurs().get(i).licence().equals(j.getNumeroLicence())));
        int index = s.joueurs().indexOf(InstantaneSuiviTest.joueur(s, j.getNumeroLicence()));
        String fiche = PagesSuivi.repondre("GET", "/partie", "id=" + index, s).corps();
        assertTrue(fiche.contains("Table 2"));
        assertTrue(fiche.contains(Html.echapper(InstantaneSuivi.nomJoueur(j))));
    }

    @Test
    void recherche_large_limitee_et_licence_partielle_refusee() {
        InstantaneSuivi s = InstantaneSuivi.depuis(InstantaneSuiviTest.demo(), InstantaneSuiviTest.HORLOGE);
        String html = PagesSuivi.partie(s, "a", null);
        assertTrue(html.contains("Plusieurs joueurs"));
        int liens = html.split("href=\"/partie\\?id=", -1).length - 1;
        assertTrue(liens > 0 && liens <= PagesSuivi.MAX_RESULTATS, "liens : " + liens);
        String licence = s.joueurs().get(0).licence();
        assertTrue(PagesSuivi.rechercher(s, licence.substring(0, 3)).isEmpty(), "licence exacte seulement");
        assertTrue(PagesSuivi.partie(s, "zzzzqqq", null).contains("Aucun joueur inscrit"));
    }

    @Test
    void aucune_licence_ni_donnee_personnelle_dans_les_pages_de_la_demo() {
        Tournoi t = InstantaneSuiviTest.demo();
        InstantaneSuivi s = InstantaneSuivi.depuis(t, InstantaneSuiviTest.HORLOGE);
        List<String> pages = new ArrayList<>();
        pages.add(PagesSuivi.accueil(s));
        pages.add(PagesSuivi.matchs(s));
        for (int n = 1; n <= s.tableaux().size(); n++) {
            pages.add(PagesSuivi.tableau(s, n));
        }
        for (int i = 0; i < s.joueurs().size(); i += 7) {
            pages.add(PagesSuivi.partie(s, null, String.valueOf(i)));
        }
        String tout = String.join("\n", pages);
        for (Tableau tab : t.getTableaux()) {
            for (Joueur j : tab.getInscrits()) {
                assertFalse(tout.contains(j.getNumeroLicence()), "licence affichée : " + j.getNumeroLicence());
                if (j.getCategorie() != null) {
                    assertFalse(tout.contains(j.getCategorie().name()), "catégorie d'âge non diffusée");
                }
            }
        }
        assertFalse(java.util.regex.Pattern.compile("[\\w.-]+@[\\w-]+\\.").matcher(tout).find(), "aucun courriel");
    }
}
