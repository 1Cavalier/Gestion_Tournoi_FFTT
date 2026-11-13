package com.pingmanager.ui.ecran;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.pingmanager.ui.ecran.PolitiqueNavigationCarte.Decision.BLOQUEE;
import static com.pingmanager.ui.ecran.PolitiqueNavigationCarte.Decision.NAVIGATEUR_SYSTEME;
import static com.pingmanager.ui.ecran.PolitiqueNavigationCarte.Decision.PAGE_LOCALE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Navigation de la WebView de la carte (README section 5, point 19). */
class PolitiqueNavigationCarteTest {

    @Test
    void seule_la_page_locale_recoit_le_pont_java() {
        assertTrue(PolitiqueNavigationCarte.pontAutorise(""), "loadContent : adresse vide");
        assertTrue(PolitiqueNavigationCarte.pontAutorise(null));
        assertTrue(PolitiqueNavigationCarte.pontAutorise("about:blank"));
        for (String a : new String[] {"https://www.openstreetmap.org/copyright", "http://leafletjs.com",
                "file:///etc/passwd", "about:blank#x", "jar:file:/x.jar!/a.html", " "}) {
            assertFalse(PolitiqueNavigationCarte.pontAutorise(a), a);
        }
    }

    @Test
    void lien_web_ouvert_dans_le_navigateur_du_systeme() {
        assertEquals(PAGE_LOCALE, PolitiqueNavigationCarte.decider(""));
        assertEquals(NAVIGATEUR_SYSTEME, PolitiqueNavigationCarte.decider("https://www.openstreetmap.org/copyright"));
        assertEquals(NAVIGATEUR_SYSTEME, PolitiqueNavigationCarte.decider("http://leafletjs.com"));
        assertEquals(NAVIGATEUR_SYSTEME, PolitiqueNavigationCarte.decider("HTTPS://geoservices.ign.fr/"));
    }

    @Test
    void toute_autre_adresse_est_bloquee() {
        for (String a : new String[] {"file:///home/x/.pingmanager/pingmanager.json", "javascript:alert(1)",
                "data:text/html,<script>x</script>", "jar:file:/app.jar!/x.html", "ftp://exemple.fr/x",
                "mailto:x@exemple.fr", "https://", "https://moi:secret@exemple.fr/", "//exemple.fr",
                "https://exemple.fr/\n", "pas une adresse", "https://exemple.fr/" + "x".repeat(3000),
                "about:config", "https://exem ple.fr"}) {
            assertEquals(BLOQUEE, PolitiqueNavigationCarte.decider(a), a);
        }
    }

    @Test
    void la_page_de_la_carte_intercepte_les_liens_et_ne_les_suit_pas() throws IOException {
        String page = Files.readString(Path.of("src/main/resources/com/pingmanager/ui/carte/carte.html"));
        assertTrue(page.contains("e.preventDefault()"));
        assertTrue(page.contains("window.java.lien(a.href)"));
    }

    @Test
    void la_carte_branche_la_politique_sur_le_pont_et_les_fenetres_surgissantes() throws IOException {
        String source = Files.readString(Path.of("src/main/java/com/pingmanager/ui/ecran/CarteTournois.java"));
        assertTrue(source.contains("PolitiqueNavigationCarte.pontAutorise(moteur.getLocation())"));
        assertTrue(source.contains("setCreatePopupHandler(f -> null)"));
        assertTrue(source.contains("moteur.getLoadWorker().cancel()"));
    }
}
