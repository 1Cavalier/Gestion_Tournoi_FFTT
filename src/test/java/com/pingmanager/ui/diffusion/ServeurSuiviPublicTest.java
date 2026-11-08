package com.pingmanager.ui.diffusion;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** Vrai serveur sur un port choisi par le système (port 0), sur l'adresse de bouclage. */
class ServeurSuiviPublicTest {

    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private ServeurSuiviPublic serveur;
    private InstantaneSuivi instantane;

    @BeforeEach
    void demarrer() throws IOException {
        instantane = InstantaneSuivi.depuis(InstantaneSuiviTest.demo(), InstantaneSuiviTest.HORLOGE);
        serveur = ServeurSuiviPublic.demarrer(InetAddress.getLoopbackAddress(), 0, instantane);
    }

    @AfterEach
    void arreter() {
        serveur.arreter();
    }

    private HttpResponse<String> envoyer(String methode, String chemin) throws Exception {
        HttpRequest.BodyPublisher corps = methode.equals("GET") || methode.equals("HEAD")
                ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString("statut=TERMINE");
        HttpRequest r = HttpRequest.newBuilder(URI.create(serveur.url().replaceAll("/$", "") + chemin))
                .timeout(Duration.ofSeconds(5)).method(methode, corps).build();
        return client.send(r, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void get_sert_les_pages_avec_les_entetes_de_securite() throws Exception {
        assertNotEquals(0, serveur.port());
        HttpResponse<String> r = envoyer("GET", "/");
        assertEquals(200, r.statusCode());
        assertTrue(r.headers().firstValue("Content-Type").orElse("").startsWith("text/html"));
        assertEquals("nosniff", r.headers().firstValue("X-Content-Type-Options").orElse(""));
        assertTrue(r.headers().firstValue("Content-Security-Policy").orElse("").contains("default-src 'none'"));
        assertEquals("no-store", r.headers().firstValue("Cache-Control").orElse(""));
        assertTrue(r.body().contains(Html.echapper(instantane.nomTournoi())));
        assertEquals(200, envoyer("GET", "/matchs").statusCode());
        assertEquals(200, envoyer("GET", "/tableau?n=1").statusCode());
        assertEquals(200, envoyer("GET", "/partie?q=a").statusCode());
        assertEquals(404, envoyer("GET", "/inconnu").statusCode());
    }

    @Test
    void head_renvoie_les_entetes_sans_corps() throws Exception {
        HttpResponse<String> r = envoyer("HEAD", "/");
        assertEquals(200, r.statusCode());
        assertTrue(r.body().isEmpty());
    }

    @Test
    void aucune_ecriture_possible_post_put_delete_patch_refuses_en_405() throws Exception {
        for (String methode : List.of("POST", "PUT", "DELETE", "PATCH")) {
            for (String chemin : List.of("/", "/partie?q=a", "/tableau?n=1", "/nimporte")) {
                HttpResponse<String> r = envoyer(methode, chemin);
                assertEquals(405, r.statusCode(), methode + " " + chemin);
                assertEquals("GET, HEAD", r.headers().firstValue("Allow").orElse(""));
            }
        }
        assertSame(instantane, serveur.instantane(), "rien n'a changé côté serveur");
    }

    @Test
    void adresse_trop_longue_refusee() throws Exception {
        assertEquals(414, envoyer("GET", "/" + "a".repeat(300)).statusCode());
        assertEquals(414, envoyer("GET", "/partie?q=" + "a".repeat(700)).statusCode());
    }

    @Test
    void publier_remplace_la_page_servie() throws Exception {
        serveur.publier(InstantaneSuivi.vide(instantane.prisLe()));
        assertEquals(503, envoyer("GET", "/").statusCode());
    }

    @Test
    void arret_propre_puis_plus_aucune_connexion() throws Exception {
        String url = serveur.url();
        serveur.arreter();
        serveur.arreter(); // sans effet la seconde fois
        assertFalse(serveur.estActif());
        HttpRequest r = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(3)).GET().build();
        assertThrows(IOException.class, () -> client.send(r, HttpResponse.BodyHandlers.ofString()));
    }

    @Test
    void url_affichee_en_ipv4_et_ipv6() throws Exception {
        assertEquals("http://192.168.1.12:8080/",
                ServeurSuiviPublic.url(InetAddress.getByAddress(new byte[]{(byte) 192, (byte) 168, 1, 12}), 8080));
        assertEquals("http://[0:0:0:0:0:0:0:1]:8080/", ServeurSuiviPublic.url(InetAddress.getByName("::1"), 8080));
    }

    // ------------------------------------------------------------------
    // Cycle de vie (DiffusionSuivi) et adresse du réseau local
    // ------------------------------------------------------------------

    private static int portLibre() throws IOException {
        try (ServerSocket s = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            return s.getLocalPort() >= DiffusionSuivi.PORT_MIN ? s.getLocalPort() : 50123;
        }
    }

    @Test
    void diffusion_demarre_publie_et_s_arrete() throws Exception {
        DiffusionSuivi d = new DiffusionSuivi(InstantaneSuiviTest.HORLOGE,
                () -> Optional.of(InetAddress.getLoopbackAddress()));
        int port = portLibre();
        DiffusionSuivi.ResultatDemarrage r = d.demarrer(port, InstantaneSuiviTest.demo());
        try {
            assertEquals(DiffusionSuivi.Motif.DEMARRE, r.motif(), r.message());
            assertTrue(d.estDemarre());
            assertEquals("http://127.0.0.1:" + port + "/", d.url());
            assertEquals(port, d.port());
            assertEquals(DiffusionSuivi.Motif.DEJA_DEMARRE, d.demarrer(port, null).motif());
            HttpResponse<String> page = client.send(HttpRequest.newBuilder(URI.create(d.url())).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(200, page.statusCode());
        } finally {
            d.arreter();
        }
        assertFalse(d.estDemarre());
        assertEquals("", d.url());
        d.arreter();
    }

    @Test
    void diffusion_refuse_port_invalide_occupe_ou_sans_reseau() throws Exception {
        DiffusionSuivi d = new DiffusionSuivi(InstantaneSuiviTest.HORLOGE,
                () -> Optional.of(InetAddress.getLoopbackAddress()));
        assertEquals(DiffusionSuivi.Motif.PORT_INVALIDE, d.demarrer(80, null).motif());
        assertEquals(DiffusionSuivi.Motif.PORT_INVALIDE, d.demarrer(70000, null).motif());
        try (ServerSocket pris = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            if (pris.getLocalPort() >= DiffusionSuivi.PORT_MIN) {
                DiffusionSuivi.ResultatDemarrage r = d.demarrer(pris.getLocalPort(), null);
                assertEquals(DiffusionSuivi.Motif.PORT_OCCUPE, r.motif(), r.message());
                assertFalse(d.estDemarre());
            }
        }
        DiffusionSuivi sansReseau = new DiffusionSuivi(InstantaneSuiviTest.HORLOGE, Optional::empty);
        assertEquals(DiffusionSuivi.Motif.PAS_DE_RESEAU_LOCAL, sansReseau.demarrer(portLibre(), null).motif());
    }

    @Test
    void lecture_du_port_saisi() {
        assertEquals(8080, DiffusionSuivi.lirePort(" 8080 "));
        assertEquals(-1, DiffusionSuivi.lirePort("80"));
        assertEquals(-1, DiffusionSuivi.lirePort("65536"));
        assertEquals(-1, DiffusionSuivi.lirePort("8o80"));
        assertEquals(-1, DiffusionSuivi.lirePort(""));
        assertEquals(-1, DiffusionSuivi.lirePort(null));
        assertEquals(-1, DiffusionSuivi.lirePort("123456"));
    }

    @Test
    void adresse_du_reseau_local_preferee_aux_autres() throws Exception {
        InetAddress boucle = InetAddress.getByAddress(new byte[]{127, 0, 0, 1});
        InetAddress lien = InetAddress.getByAddress(new byte[]{(byte) 169, (byte) 254, 1, 1});
        InetAddress publique = InetAddress.getByAddress(new byte[]{8, 8, 8, 8});
        InetAddress privee = InetAddress.getByAddress(new byte[]{(byte) 192, (byte) 168, 1, 12});
        InetAddress ipv6 = InetAddress.getByName("fe80::1");
        assertEquals(Optional.of(privee), AdresseReseauLocal.choisir(List.of(boucle, lien, ipv6, publique, privee)));
        assertEquals(Optional.of(publique), AdresseReseauLocal.choisir(List.of(boucle, publique)));
        assertEquals(Optional.empty(), AdresseReseauLocal.choisir(List.of(boucle, lien, ipv6)));
    }
}
