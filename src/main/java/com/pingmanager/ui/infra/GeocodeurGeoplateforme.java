package com.pingmanager.ui.infra;

import com.pingmanager.core.Geocodeur;
import com.pingmanager.model.AdresseTournoi;
import com.pingmanager.model.Coordonnees;
import com.pingmanager.persistence.json.Json;
import com.pingmanager.persistence.json.JsonParser;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

/**
 * Géocodage par l'API de la <b>Géoplateforme IGN</b> (ex-API Adresse de la Base Adresse
 * Nationale, transférée à l'IGN en 2025 : {@code https://data.geopf.fr/geocodage/search},
 * gratuite, sans clé, réponse GeoJSON). Appelé à l'enregistrement de l'adresse d'un
 * tournoi ; délais courts pour ne pas bloquer l'écran. Sans réseau ou en cas d'échec, la
 * réponse est vide : la carte se rabat alors sur la préfecture du département
 * ({@link Geocodeur#centroideDuDepartement()}), sans stocker cette position approchée.
 */
public final class GeocodeurGeoplateforme implements Geocodeur {

    public static final GeocodeurGeoplateforme INSTANCE = new GeocodeurGeoplateforme();

    private static final String URL = "https://data.geopf.fr/geocodage/search?index=address&limit=1&q=";
    /** Score minimal de confiance (0 à 1) pour retenir une réponse. */
    private static final double SCORE_MIN = 0.4;

    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();

    private GeocodeurGeoplateforme() {
    }

    @Override
    public Optional<Coordonnees> geocoder(AdresseTournoi a) {
        if (a == null || !a.estComplete()) {
            return Optional.empty();
        }
        try {
            String q = a.getVoie() + " " + a.getCodePostal() + " " + a.getVille();
            String url = URL + URLEncoder.encode(q, StandardCharsets.UTF_8)
                    + "&postcode=" + URLEncoder.encode(a.getCodePostal(), StandardCharsets.UTF_8);
            HttpRequest req = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(3))
                    .header("User-Agent", "PingManager").GET().build();
            HttpResponse<String> rep = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (rep.statusCode() != 200) {
                return Optional.empty();
            }
            return lire(rep.body());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /** Lit la première réponse GeoJSON : {@code features[0].geometry.coordinates = [lon, lat]}. */
    static Optional<Coordonnees> lire(String json) {
        Json racine = JsonParser.analyser(json);
        Json features = racine.get("features");
        if (features == null || !features.estTableau() || features.taille() == 0) {
            return Optional.empty();
        }
        Json f = features.elements().get(0);
        Json props = f.get("properties");
        if (props != null && props.getDouble("score", 1) < SCORE_MIN) {
            return Optional.empty();
        }
        Json c = f.get("geometry").get("coordinates");
        return Optional.of(new Coordonnees(c.elements().get(1).asDouble(), c.elements().get(0).asDouble()));
    }
}
