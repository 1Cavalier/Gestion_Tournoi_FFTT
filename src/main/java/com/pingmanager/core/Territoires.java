package com.pingmanager.core;

import com.pingmanager.model.Coordonnees;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Référentiel des départements et de leur <b>ligue régionale</b> FFTT (une ligue par
 * région administrative), lu depuis la ressource {@code departements.csv} du cœur.
 *
 * <p>Sert aux règles d'accès IX.102 (tournoi régional : associations de la région du
 * club organisateur ; départemental : de son département) et de repli pour la carte
 * (coordonnées de la préfecture quand une adresse n'a pas pu être géocodée).</p>
 */
public final class Territoires {

    /**
     * Un département : code (« 91 », « 2A », « 974 »), nom, ligue, préfecture et forme
     * précédée de « de » (« de l'Essonne », « du Loiret », « des Yvelines »).
     */
    public record Departement(String code, String nom, String ligue, Coordonnees prefecture, String formeDe) {
    }

    private static final Map<String, Departement> PAR_CODE = charger();

    private Territoires() {
    }

    /** @return le département de ce code (« 91 », « 9 » → « 09 », « 2a » → « 2A »). */
    public static Optional<Departement> departement(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        String c = code.trim().toUpperCase(java.util.Locale.ROOT);
        if (c.length() == 1) {
            c = "0" + c;
        }
        return Optional.ofNullable(PAR_CODE.get(c));
    }

    /**
     * Département d'un code postal : les 2 premiers chiffres, 3 en outre-mer (97x),
     * Corse 2A (20000 à 20199) ou 2B (20200 et au-delà).
     */
    public static Optional<Departement> departementDuCodePostal(String codePostal) {
        if (codePostal == null) {
            return Optional.empty();
        }
        String cp = codePostal.replaceAll("\\s", "");
        if (cp.length() != 5 || !cp.chars().allMatch(Character::isDigit)) {
            return Optional.empty();
        }
        if (cp.startsWith("97")) {
            return departement(cp.substring(0, 3));
        }
        if (cp.startsWith("20")) {
            return departement(Integer.parseInt(cp) < 20200 ? "2A" : "2B");
        }
        return departement(cp.substring(0, 2));
    }

    /** @return la ligue régionale d'un département, si le code est connu. */
    public static Optional<String> ligue(String codeDepartement) {
        return departement(codeDepartement).map(Departement::ligue);
    }

    /** @return tous les départements, dans l'ordre des codes. */
    public static Map<String, Departement> tous() {
        return Collections.unmodifiableMap(PAR_CODE);
    }

    private static Map<String, Departement> charger() {
        Map<String, Departement> res = new LinkedHashMap<>();
        try (InputStream in = Territoires.class.getResourceAsStream("departements.csv")) {
            if (in == null) {
                throw new IllegalStateException("Ressource departements.csv introuvable");
            }
            BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String ligne;
            while ((ligne = r.readLine()) != null) {
                if (ligne.isBlank() || ligne.startsWith("#")) {
                    continue;
                }
                String[] p = ligne.split(";");
                res.put(p[0].trim(), new Departement(p[0].trim(), p[1].trim(), p[2].trim(),
                        new Coordonnees(Double.parseDouble(p[3]), Double.parseDouble(p[4])),
                        p.length > 5 ? p[5].trim() : "de " + p[1].trim()));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return res;
    }
}
