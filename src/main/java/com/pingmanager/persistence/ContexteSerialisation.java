package com.pingmanager.persistence;

import com.pingmanager.model.Joueur;
import com.pingmanager.model.Match;
import com.pingmanager.persistence.json.Json;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * État de travail partagé pendant l'écriture puis la lecture d'un document.
 *
 * <p>Résout deux problèmes structurels du modèle :</p>
 *
 * <ol>
 *   <li><b>Déduplication des joueurs.</b> Un même {@link Joueur} (identité =
 *       licence) est référencé par de nombreux objets (poules, matchs, états,
 *       cartons…). On l'écrit une seule fois dans un annuaire et on le désigne
 *       partout ailleurs par sa licence. À la relecture, une seule instance est
 *       recréée par licence, ce qui préserve {@code equals}/{@code hashCode} et
 *       le bon fonctionnement des {@code Map<Joueur, …>}.</li>
 *   <li><b>Graphe des matchs.</b> Un {@link Match} de tableau à élimination
 *       porte {@code sourceA}/{@code sourceB} pointant vers d'autres matchs.
 *       Chaque match reçoit un identifiant stable ; les liens sont écrits comme
 *       références à ces identifiants et rétablis en seconde passe à la lecture,
 *       préservant le partage d'instances.</li>
 * </ol>
 */
final class ContexteSerialisation {

    // --- Écriture ---
    private final Map<String, Joueur> joueursParLicence = new LinkedHashMap<>();
    private final IdentityHashMap<Match, String> idParMatch = new IdentityHashMap<>();
    private int compteurMatch = 0;

    // --- Lecture ---
    private final Map<String, Joueur> joueursLus = new LinkedHashMap<>();
    private final Map<String, Match> matchsParId = new LinkedHashMap<>();
    private final List<Runnable> liaisonsDifferees = new ArrayList<>();

    // ------------------------------------------------------------------
    // Écriture — annuaire des joueurs
    // ------------------------------------------------------------------

    /** Enregistre un joueur dans l'annuaire (idempotent par licence). */
    void enregistrerJoueur(Joueur j) {
        if (j == null) {
            return;
        }
        joueursParLicence.putIfAbsent(j.getNumeroLicence(), j);
    }

    /** @return la référence sérialisée d'un joueur : sa licence (ou nul). */
    Json refJoueur(Joueur j) {
        if (j == null) {
            return Json.nul();
        }
        enregistrerJoueur(j);
        return Json.of(j.getNumeroLicence());
    }

    /** @return l'annuaire complet des joueurs rencontrés, en JSON. */
    Json ecrireAnnuaire() {
        Json arr = Json.tableau();
        for (Joueur j : joueursParLicence.values()) {
            arr.add(ConversionsCommunes.ecrireJoueurComplet(j));
        }
        return arr;
    }

    /** Identifiant stable d'un match (créé à la volée à la première rencontre). */
    String idDeMatch(Match m) {
        return idParMatch.computeIfAbsent(m, x -> "m" + (compteurMatch++));
    }

    // ------------------------------------------------------------------
    // Lecture — annuaire des joueurs
    // ------------------------------------------------------------------

    /** Reconstruit l'annuaire des joueurs (une instance par licence). */
    void lireAnnuaire(Json arr) {
        if (arr == null || arr.estNul()) {
            return;
        }
        for (Json o : arr.elements()) {
            Joueur j = ConversionsCommunes.lireJoueurComplet(o);
            joueursLus.put(j.getNumeroLicence(), j);
        }
    }

    /** @return l'instance unique du joueur pour cette référence (licence), ou null. */
    Joueur joueur(Json ref) {
        if (ref == null || ref.estNul()) {
            return null;
        }
        String licence = ref.asString();
        if (Joueur.LICENCE_A_DETERMINER.equals(licence)) {
            // Chaque emplacement « à déterminer » est un sentinelle neutre ;
            // on peut en partager une instance sans risque (égalité par licence).
            return joueursLus.computeIfAbsent(licence, l -> Joueur.aDeterminer());
        }
        Joueur j = joueursLus.get(licence);
        if (j == null) {
            throw new com.pingmanager.persistence.json.JsonException(
                    "Joueur référencé absent de l'annuaire : " + licence);
        }
        return j;
    }

    // ------------------------------------------------------------------
    // Lecture — graphe des matchs
    // ------------------------------------------------------------------

    void enregistrerMatchLu(String id, Match m) {
        if (id != null) {
            matchsParId.put(id, m);
        }
    }

    Match matchParId(String id) {
        return id == null ? null : matchsParId.get(id);
    }

    /** Diffère une liaison (source A/B) jusqu'à ce que tous les matchs existent. */
    void differerLiaison(Runnable r) {
        liaisonsDifferees.add(r);
    }

    /** Applique toutes les liaisons différées (2e passe). */
    void resoudreLiaisons() {
        for (Runnable r : liaisonsDifferees) {
            r.run();
        }
        liaisonsDifferees.clear();
    }
}
