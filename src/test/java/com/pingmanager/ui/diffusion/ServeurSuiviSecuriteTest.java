package com.pingmanager.ui.diffusion;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Audit de sécurité B021 : défauts démontrés et garanties vérifiées du serveur de suivi. */
class ServeurSuiviSecuriteTest {

    private ServeurSuiviPublic serveur;
    private final HttpClient client = HttpClient.newHttpClient();

    @BeforeEach
    void demarrer() throws IOException {
        serveur = ServeurSuiviPublic.demarrer(InetAddress.getLoopbackAddress(), 0,
                InstantaneSuivi.depuis(InstantaneSuiviTest.demo(), InstantaneSuiviTest.HORLOGE));
    }

    @AfterEach
    void arreter() {
        serveur.arreter();
    }

    private HttpResponse<String> get(String chemin) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(serveur.url().replaceAll("/$", "") + chemin))
                .timeout(Duration.ofSeconds(3)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    /** DEFAUT (moyenne) : 4 connexions qui n'envoient qu'une requête partielle gèlent les 4 fils, sans délai. */
    @Test
    void quatre_connexions_lentes_bloquent_le_serveur() throws Exception {
        assertEquals(200, get("/").statusCode());
        List<Socket> lentes = new ArrayList<>();
        try {
            for (int i = 0; i < 4; i++) {
                Socket s = new Socket(serveur.adresse(), serveur.port());
                s.getOutputStream().write("GET / HTTP/1.1\r\nHost: x\r\nX-Lent:".getBytes(StandardCharsets.US_ASCII));
                s.getOutputStream().flush();
                lentes.add(s);
            }
            Thread.sleep(500);
            // Le comportement attendu serait de répondre encore (ou de couper les lentes) : ce test échoue
            // quand le défaut est corrigé (délai de lecture ou plus de fils), et doit alors être retourné.
            assertThrows(IOException.class, () -> get("/"),
                    "Le serveur répond encore : défaut corrigé, retourner ce test");
        } finally {
            for (Socket s : lentes) {
                s.close();
            }
        }
    }

    @Test
    void en_tetes_hostiles_et_chemins_etranges_ne_donnent_ni_500_ni_trace() throws Exception {
        for (String chemin : new String[] {"/%00", "/..%2f..%2fetc/passwd", "/partie?id=-1", "/partie?id=99999999999",
                "/partie?q=%C3%28", "/tableau?n=1e3", "/matchs?" + "a=1&".repeat(300)}) {
            HttpResponse<String> r = get(chemin);
            assertTrue(r.statusCode() == 200 || r.statusCode() == 404 || r.statusCode() == 414,
                    chemin + " -> " + r.statusCode());
            assertFalse(r.body().contains("Exception"), chemin);
            assertFalse(r.body().contains("\tat "), chemin);
        }
    }

    @Test
    void encodage_pourcent_invalide_ne_fait_pas_tomber_le_serveur() throws Exception {
        try (Socket s = new Socket(serveur.adresse(), serveur.port())) {
            s.setSoTimeout(3000);
            s.getOutputStream().write("GET /partie?q=%ZZ HTTP/1.1\r\nHost: x\r\nConnection: close\r\n\r\n"
                    .getBytes(StandardCharsets.US_ASCII));
            String ligne = new java.io.BufferedReader(new java.io.InputStreamReader(s.getInputStream())).readLine();
            assertTrue(ligne != null && (ligne.contains(" 400") || ligne.contains(" 200")), String.valueOf(ligne));
        }
        assertEquals(200, get("/").statusCode());
    }

    @Test
    void injection_dans_la_recherche_est_echappee() throws Exception {
        String corps = get("/partie?q=%22%3E%3Cscript%3Ealert(1)%3C%2Fscript%3E").body();
        assertFalse(corps.contains("<script>alert"));
        assertTrue(corps.contains("&quot;&gt;&lt;script&gt;"));
    }

    @Test
    void aucune_donnee_privee_dans_les_pages() throws Exception {
        String tout = get("/").body() + get("/matchs").body() + get("/tableau?n=1").body() + get("/partie?id=0").body();
        assertFalse(java.util.regex.Pattern.compile("[\\w.-]+@[\\w-]+\\.[a-z]{2,}").matcher(tout).find(), "courriel ?");
        assertFalse(tout.toLowerCase().contains("naissance"));
        assertFalse(tout.toLowerCase().contains("mailto:"));
        assertFalse(tout.toLowerCase().contains("tel:"));
    }

    @Test
    void en_tetes_de_securite_presents_aussi_sur_404_et_405() throws Exception {
        for (HttpResponse<String> r : List.of(get("/nimporte"),
                client.send(HttpRequest.newBuilder(URI.create(serveur.url()))
                        .method("POST", HttpRequest.BodyPublishers.ofString("x")).build(),
                        HttpResponse.BodyHandlers.ofString()))) {
            assertEquals("nosniff", r.headers().firstValue("X-Content-Type-Options").orElse(""));
            assertEquals("no-referrer", r.headers().firstValue("Referrer-Policy").orElse(""));
            assertTrue(r.headers().firstValue("Content-Type").orElse("").toLowerCase().contains("charset=utf-8"));
        }
    }

    /** DEFAUT (faible) : l'écoute sur une IPv4 publique est acceptée par AdresseReseauLocal (repli). */
    @Test
    void adresse_publique_acceptee_en_repli() throws Exception {
        assertTrue(AdresseReseauLocal.choisir(List.of(InetAddress.getByName("8.8.8.8"))).isPresent());
    }
}
