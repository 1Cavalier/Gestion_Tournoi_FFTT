package com.pingmanager.persistence;

import com.pingmanager.model.Joueur;
import com.pingmanager.model.Sexe;
import com.pingmanager.persistence.json.Json;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Fonctions de conversion partagées entre les mappers de la couche
 * {@code persistence}.
 *
 * <p>Regroupe le traitement des types transverses : dates et heures (ISO-8601,
 * texte stable et lisible), durées (en secondes), énumérations (par leur
 * {@code name()}, robuste au réordonnancement) et le format sérialisé d'un
 * {@link Joueur}. Aucune logique métier : uniquement de la traduction
 * modèle ↔ JSON.</p>
 */
final class ConversionsCommunes {

    private ConversionsCommunes() {
    }

    // ------------------------------------------------------------------
    // Dates / heures / durées (représentation texte ISO, ou nombres)
    // ------------------------------------------------------------------

    static Json ecrire(LocalDate d) {
        return d == null ? Json.nul() : Json.of(d.toString());
    }

    static LocalDate lireDate(Json j) {
        return j == null || j.estNul() ? null : LocalDate.parse(j.asString());
    }

    static Json ecrire(LocalDateTime d) {
        return d == null ? Json.nul() : Json.of(d.toString());
    }

    static LocalDateTime lireDateHeure(Json j) {
        return j == null || j.estNul() ? null : LocalDateTime.parse(j.asString());
    }

    static Json ecrire(LocalTime t) {
        return t == null ? Json.nul() : Json.of(t.toString());
    }

    static LocalTime lireHeure(Json j) {
        return j == null || j.estNul() ? null : LocalTime.parse(j.asString());
    }

    static Json ecrire(Duration d) {
        return d == null ? Json.nul() : Json.of(d.toSeconds());
    }

    static Duration lireDuree(Json j) {
        return j == null || j.estNul() ? null : Duration.ofSeconds(j.asLong());
    }

    // ------------------------------------------------------------------
    // Énumérations : par name(), avec valeur par défaut null-safe
    // ------------------------------------------------------------------

    static <E extends Enum<E>> Json ecrireEnum(E e) {
        return e == null ? Json.nul() : Json.of(e.name());
    }

    static <E extends Enum<E>> E lireEnum(Json j, Class<E> type) {
        if (j == null || j.estNul()) {
            return null;
        }
        return Enum.valueOf(type, j.asString());
    }

    // ------------------------------------------------------------------
    // Joueur : sérialisé en entier dans l'annuaire, référencé par licence
    // ailleurs. Le sentinelle « à déterminer » a un traitement dédié.
    // ------------------------------------------------------------------

    static Json ecrireJoueurComplet(Joueur j) {
        Json o = Json.objet();
        o.set("licence", j.getNumeroLicence());
        o.set("nom", j.getNom());
        o.set("prenom", j.getPrenom());
        o.set("sexe", ecrireEnum(j.getSexe()));
        o.set("club", j.getClub());
        o.set("points", j.getPoints());
        if (j.aRangNationalConnu()) {
            o.set("rangNational", j.getRangNational());
        }
        if (j.getCategorie() != null) {
            o.set("categorie", ecrireEnum(j.getCategorie()));
        }
        return o;
    }

    static Joueur lireJoueurComplet(Json o) {
        String licence = o.get("licence").asString();
        if (Joueur.LICENCE_A_DETERMINER.equals(licence)) {
            return Joueur.aDeterminer();
        }
        Joueur j = new Joueur(
                licence,
                o.get("nom").asString(),
                o.get("prenom").asString(),
                lireEnum(o.get("sexe"), Sexe.class),
                o.getString("club", ""),
                o.getInt("points", 0));
        int rang = o.getInt("rangNational", Joueur.RANG_NATIONAL_INCONNU);
        if (rang != Joueur.RANG_NATIONAL_INCONNU) {
            j.setRangNational(rang);
        }
        if (o.has("categorie")) {
            j.setCategorie(lireEnum(o.get("categorie"), com.pingmanager.model.CategorieAge.class));
        }
        return j;
    }
}
