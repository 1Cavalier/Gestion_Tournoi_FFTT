package com.pingmanager.ui.diffusion;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Petit serveur web local <b>en lecture seule</b> du suivi sur téléphone (B021),
 * sur la classe {@code com.sun.net.httpserver} du JDK (aucune dépendance).
 *
 * <p>Placé dans {@code ui.diffusion}, hors du cœur : c'est un adaptateur
 * d'affichage (comme un écran) qui ouvre un port réseau ; il ne dépend ni de
 * JavaFX ni de la persistance, ce qui le garde testable sans interface et
 * isole tout le code réseau dans un seul package, facile à auditer.</p>
 *
 * <p>Sécurité : seules GET et HEAD sont servies (405 sinon), le corps des
 * requêtes n'est jamais lu, les pages ne lisent qu'un {@link InstantaneSuivi}
 * immuable publié par le fil JavaFX ({@link #publier}) : aucun fil du serveur ne
 * touche au tournoi. Quatre fils au plus, démons, arrêtés par {@link #arreter}.</p>
 */
public final class ServeurSuiviPublic implements AutoCloseable {

    /** Longueur maximale acceptée du chemin d'une URL. */
    static final int LONGUEUR_MAX_CHEMIN = 128;
    private static final int FILS = 4;

    private final HttpServer serveur;
    private final ExecutorService fils;
    private final AtomicReference<InstantaneSuivi> instantane;
    private volatile boolean arrete;

    private ServeurSuiviPublic(HttpServer serveur, ExecutorService fils, InstantaneSuivi initial) {
        this.serveur = serveur;
        this.fils = fils;
        this.instantane = new AtomicReference<>(initial);
    }

    /**
     * Démarre le serveur sur l'adresse et le port donnés (port 0 : choisi par
     * le système, pour les tests).
     *
     * @throws IOException si le port est déjà pris ou l'adresse indisponible
     */
    public static ServeurSuiviPublic demarrer(InetAddress adresse, int port, InstantaneSuivi initial)
            throws IOException {
        Objects.requireNonNull(adresse, "adresse");
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("Port hors limites : " + port);
        }
        HttpServer http = HttpServer.create(new InetSocketAddress(adresse, port), 16);
        AtomicInteger numero = new AtomicInteger();
        ExecutorService fils = Executors.newFixedThreadPool(FILS, r -> {
            Thread t = new Thread(r, "suivi-telephone-" + numero.incrementAndGet());
            t.setDaemon(true);
            return t;
        });
        ServeurSuiviPublic s = new ServeurSuiviPublic(http, fils,
                initial == null ? InstantaneSuivi.vide(LocalDateTime.now()) : initial);
        http.createContext("/", s::traiter);
        http.setExecutor(fils);
        http.start();
        return s;
    }

    /** Remplace l'instantané servi (appelé depuis le fil JavaFX). */
    public void publier(InstantaneSuivi nouveau) {
        instantane.set(Objects.requireNonNull(nouveau, "instantane"));
    }

    /** @return l'instantané servi en ce moment. */
    public InstantaneSuivi instantane() {
        return instantane.get();
    }

    /** @return le port d'écoute réel. */
    public int port() {
        return serveur.getAddress().getPort();
    }

    /** @return l'adresse d'écoute. */
    public InetAddress adresse() {
        return serveur.getAddress().getAddress();
    }

    /** « http://192.168.1.12:8080/ », à afficher en grand. */
    public String url() {
        return url(adresse(), port());
    }

    static String url(InetAddress adresse, int port) {
        String hote = adresse.getHostAddress();
        if (adresse instanceof Inet6Address) {
            int zone = hote.indexOf('%');
            hote = "[" + (zone >= 0 ? hote.substring(0, zone) : hote) + "]";
        }
        return "http://" + hote + ":" + port + "/";
    }

    /** @return true tant que le serveur n'est pas arrêté. */
    public boolean estActif() {
        return !arrete;
    }

    /** Arrête le serveur et ses fils (sans attendre les échanges en cours). Sans effet s'il l'est déjà. */
    public synchronized void arreter() {
        if (arrete) {
            return;
        }
        arrete = true;
        serveur.stop(0);
        fils.shutdownNow();
    }

    @Override
    public void close() {
        arreter();
    }

    private void traiter(HttpExchange echange) throws IOException {
        try {
            String methode = echange.getRequestMethod();
            String chemin = echange.getRequestURI().getRawPath();
            String requete = echange.getRequestURI().getRawQuery();
            PagesSuivi.Reponse r;
            if (chemin != null && chemin.length() > LONGUEUR_MAX_CHEMIN) {
                r = new PagesSuivi.Reponse(414, PagesSuivi.TYPE_TEXTE, "Adresse trop longue.");
            } else {
                r = PagesSuivi.repondre(methode, chemin, requete, instantane.get());
            }
            Headers h = echange.getResponseHeaders();
            h.set("Content-Type", r.typeContenu());
            h.set("Cache-Control", "no-store");
            h.set("X-Content-Type-Options", "nosniff");
            h.set("X-Frame-Options", "DENY");
            h.set("Referrer-Policy", "no-referrer");
            h.set("Content-Security-Policy",
                    "default-src 'none'; style-src 'unsafe-inline'; form-action 'self'; frame-ancestors 'none'");
            if (r.statut() == 405) {
                h.set("Allow", "GET, HEAD");
            }
            byte[] corps = r.corps().getBytes(StandardCharsets.UTF_8);
            if ("HEAD".equals(methode) || r.statut() == 405) {
                // HEAD : en-têtes seuls ; 405 : refus sans corps.
                echange.sendResponseHeaders(r.statut(), -1);
                return;
            }
            echange.sendResponseHeaders(r.statut(), corps.length);
            try (OutputStream sortie = echange.getResponseBody()) {
                sortie.write(corps);
            }
        } catch (RuntimeException e) {
            // Une page qui échoue ne doit jamais arrêter le serveur ni laisser fuiter une trace.
            try {
                echange.sendResponseHeaders(500, -1);
            } catch (IOException | RuntimeException deja) {
                // en-têtes déjà envoyés : rien de plus à faire
            }
        } finally {
            echange.close();
        }
    }
}
