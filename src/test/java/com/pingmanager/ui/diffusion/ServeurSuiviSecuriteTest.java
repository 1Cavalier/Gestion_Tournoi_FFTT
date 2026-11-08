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

    private static Socket ouvrirConnexionLente(ServeurSuiviPublic serveur) throws IOException {
        Socket s = new Socket(serveur.adresse(), serveur.port());
        s.getOutputStream().write("GET / HTTP/1.1\r\nHost: x\r\nX-Lent:".getBytes(StandardCharsets.US_ASCII));
        s.getOutputStream().flush();
        return s;
    }

    /** Correction audit (moyenne) : des connexions qui n'envoient qu'une requête partielle ne gèlent plus le serveur. */
    @Test
    void quatre_connexions_lentes_ne_bloquent_pas_le_serveur() throws Exception {
        assertEquals(200, get("/").statusCode());
        List<Socket> lentes = new ArrayList<>();
        try {
            for (int i = 0; i < 4; i++) {
                lentes.add(ouvrirConnexionLente(serveur));
            }
            Thread.sleep(500);
            assertEquals(200, get("/").statusCode(), "Le serveur doit répondre malgré les connexions lentes");
        } finally {
            for (Socket s : lentes) {
                s.close();
            }
        }
    }

    /** Correction audit (moyenne) : autant de connexions lentes que de fils sont coupées après le délai de requête. */
    @Test
    void connexions_lentes_coupees_apres_le_delai_puis_le_serveur_repond() throws Exception {
        List<Socket> lentes = new ArrayList<>();
        try {
            for (int i = 0; i < ServeurSuiviPublic.FILS; i++) {
                lentes.add(ouvrirConnexionLente(serveur));
            }
            long limite = (ServeurSuiviPublic.DELAI_REQUETE_S + 5) * 1000L;
            for (Socket s : lentes) {
                s.setSoTimeout((int) limite);
                // -1 (fin de flux) ou une réponse d'erreur suivie de la fermeture : la connexion est coupée.
                long debut = System.nanoTime();
                try {
                    while (s.getInputStream().read() != -1) {
                        // lire jusqu'à la fermeture
                    }
                } catch (java.net.SocketException coupee) {
                    // réinitialisée par le serveur : coupée aussi
                }
                assertTrue((System.nanoTime() - debut) / 1_000_000 < limite, "connexion lente non coupée");
            }
            assertEquals(200, get("/").statusCode());
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

    /** Correction audit (faible) : une IPv4 publique n'est jamais retenue pour écouter. */
    @Test
    void adresse_publique_jamais_retenue() throws Exception {
        assertTrue(AdresseReseauLocal.choisir(List.of(InetAddress.getByName("8.8.8.8"))).isEmpty());
        assertTrue(AdresseReseauLocal.choisir(List.of(InetAddress.getByName("172.32.0.1"),
                InetAddress.getByName("100.64.0.1"), InetAddress.getByName("0.0.0.0"))).isEmpty());
    }

    @Test
    void seules_les_trois_plages_privees_sont_retenues() throws Exception {
        for (String privee : new String[] {"10.0.0.1", "10.255.255.254", "172.16.0.1", "172.31.255.254",
                "192.168.0.1", "192.168.255.254"}) {
            InetAddress a = InetAddress.getByName(privee);
            assertEquals(java.util.Optional.of(a), AdresseReseauLocal.choisir(List.of(a)), privee);
        }
        InetAddress publique = InetAddress.getByName("8.8.8.8");
        InetAddress privee = InetAddress.getByName("10.1.2.3");
        assertEquals(java.util.Optional.of(privee), AdresseReseauLocal.choisir(List.of(publique, privee)));
    }

    @Test
    void sans_adresse_privee_pas_de_serveur_et_message_clair() {
        DiffusionSuivi diffusion = new DiffusionSuivi(InstantaneSuiviTest.HORLOGE, java.util.Optional::empty);
        try {
            DiffusionSuivi.ResultatDemarrage r = diffusion.demarrer(8080, InstantaneSuiviTest.demo());
            assertEquals(DiffusionSuivi.Motif.PAS_DE_RESEAU_LOCAL, r.motif());
            assertTrue(r.message().contains("privé"), r.message());
            assertFalse(diffusion.estDemarre());
        } finally {
            diffusion.arreter();
        }
    }
}
