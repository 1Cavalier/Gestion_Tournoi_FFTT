package com.pingmanager.persistence;

import com.pingmanager.model.Match;
import com.pingmanager.ordonnancement.CategorieDuree;
import com.pingmanager.ordonnancement.EstimateurDuree;
import com.pingmanager.ordonnancement.MatchOrdonnancable;
import com.pingmanager.ordonnancement.Ordonnanceur;
import com.pingmanager.ordonnancement.PhaseMatch;
import com.pingmanager.ordonnancement.PoidsOrdonnancement;
import com.pingmanager.persistence.json.Json;

import java.time.Duration;
import java.util.Map;

import static com.pingmanager.persistence.ConversionsCommunes.ecrire;
import static com.pingmanager.persistence.ConversionsCommunes.ecrireEnum;
import static com.pingmanager.persistence.ConversionsCommunes.lireDateHeure;
import static com.pingmanager.persistence.ConversionsCommunes.lireDuree;
import static com.pingmanager.persistence.ConversionsCommunes.lireEnum;

/**
 * Traduit l'état vivant de l'ordonnancement en salle : l'{@link EstimateurDuree}
 * (mémoire d'apprentissage des durées) et l'{@link Ordonnanceur} (matchs sous
 * gestion et occupation des tables).
 *
 * <p>Cet état accompagne l'autosave de reprise sur incident. Deux points de
 * conception :</p>
 *
 * <ul>
 *   <li>Les {@link MatchOrdonnancable} enveloppent des {@link Match} qui sont
 *       <b>les mêmes instances</b> que celles du tournoi. On les référence donc
 *       par l'identifiant de match du {@link ContexteSerialisation} partagé avec
 *       l'écriture du tournoi, et on les relie aux mêmes objets à la relecture.</li>
 *   <li>L'occupation des tables n'est pas sérialisée en double : elle se
 *       reconstruit à partir des matchs dont le statut est « en cours » et qui
 *       portent déjà leur numéro de table.</li>
 * </ul>
 */
final class SessionMapper {

    private SessionMapper() {
    }

    // ==================================================================
    // EstimateurDuree
    // ==================================================================

    static Json ecrireEstimateur(EstimateurDuree est) {
        if (est == null) {
            return Json.nul();
        }
        Json o = Json.objet();
        o.set("alpha", est.getAlpha());
        o.set("minObservations", est.getMinObservations());
        o.set("valeurParDefautGlobale", ecrire(est.getValeurParDefautGlobale()));

        Json depart = Json.tableau();
        for (Map.Entry<CategorieDuree, Duration> e : est.getValeursDepart().entrySet()) {
            Json je = ecrireCategorie(e.getKey());
            je.set("duree", ecrire(e.getValue()));
            depart.add(je);
        }
        o.set("valeursDepart", depart);

        Json appris = Json.tableau();
        Map<CategorieDuree, Double> minutes = est.getMinutesApprises();
        Map<CategorieDuree, Integer> obs = est.getNombreObservations();
        for (Map.Entry<CategorieDuree, Double> e : minutes.entrySet()) {
            Json je = ecrireCategorie(e.getKey());
            je.set("minutes", e.getValue());
            je.set("observations", obs.getOrDefault(e.getKey(), 0));
            appris.add(je);
        }
        o.set("apprentissage", appris);
        return o;
    }

    static EstimateurDuree lireEstimateur(Json o) {
        if (o == null || o.estNul()) {
            return null;
        }
        Duration parDefaut = lireDuree(o.get("valeurParDefautGlobale"));
        double alpha = o.getDouble("alpha", 0.3);
        int minObs = o.getInt("minObservations", 3);
        EstimateurDuree est = new EstimateurDuree(parDefaut, alpha, minObs);

        if (o.has("valeursDepart")) {
            for (Json je : o.get("valeursDepart").elements()) {
                est.definirValeurDepart(lireCategorie(je), lireDuree(je.get("duree")));
            }
        }
        if (o.has("apprentissage")) {
            for (Json je : o.get("apprentissage").elements()) {
                est.restaurerApprentissage(
                        lireCategorie(je),
                        je.get("minutes").asDouble(),
                        je.getInt("observations", 0));
            }
        }
        return est;
    }

    private static Json ecrireCategorie(CategorieDuree c) {
        Json o = Json.objet();
        o.set("phase", ecrireEnum(c.getPhase()));
        o.set("trancheNiveau", c.getTrancheNiveau());
        return o;
    }

    private static CategorieDuree lireCategorie(Json o) {
        return new CategorieDuree(
                lireEnum(o.get("phase"), PhaseMatch.class),
                o.getInt("trancheNiveau", 0));
    }

    // ==================================================================
    // Ordonnanceur
    // ==================================================================

    static Json ecrireOrdonnanceur(Ordonnanceur ord, ContexteSerialisation ctx) {
        if (ord == null) {
            return Json.nul();
        }
        Json o = Json.objet();
        o.set("nombreDeTables", ord.getNombreDeTables());
        o.set("poids", ecrirePoids(ord.getPoids()));

        Json matchs = Json.tableau();
        for (MatchOrdonnancable mo : ord.tousLesMatchs()) {
            Json jm = Json.objet();
            // Référence au match par son identifiant (même instance que dans le tournoi).
            jm.set("matchId", ctx.idDeMatch(mo.getMatch()));
            jm.set("identifiantTableau", mo.getIdentifiantTableau());
            jm.set("nomTableau", mo.getNomTableau());
            jm.set("phase", ecrireEnum(mo.getPhase()));
            if (mo.getPretDepuis() != null) {
                jm.set("pretDepuis", ecrire(mo.getPretDepuis()));
            }
            matchs.add(jm);
        }
        o.set("matchs", matchs);
        return o;
    }

    static Ordonnanceur lireOrdonnanceur(Json o, ContexteSerialisation ctx) {
        if (o == null || o.estNul()) {
            return null;
        }
        int tables = o.getInt("nombreDeTables", 1);
        PoidsOrdonnancement poids = lirePoids(o.get("poids"));
        Ordonnanceur ord = new Ordonnanceur(tables, poids);

        if (o.has("matchs")) {
            for (Json jm : o.get("matchs").elements()) {
                Match match = ctx.matchParId(jm.getString("matchId", null));
                if (match == null) {
                    // Un match référencé par l'ordonnanceur mais absent du tournoi
                    // rechargé est ignoré (état incohérent, on privilégie la reprise).
                    continue;
                }
                MatchOrdonnancable mo = new MatchOrdonnancable(
                        match,
                        jm.getString("identifiantTableau", ""),
                        jm.getString("nomTableau", null),
                        lireEnum(jm.get("phase"), PhaseMatch.class));
                mo.setPretDepuis(lireDateHeure(jm.get("pretDepuis")));
                ord.ajouterMatch(mo);

                // Reconstruire l'occupation des tables depuis les matchs en cours.
                if (match.getStatut() == com.pingmanager.model.StatutMatch.EN_COURS
                        && match.getNumeroTable() != null) {
                    ord.restaurerOccupation(match.getNumeroTable(), mo);
                }
            }
        }
        return ord;
    }

    private static Json ecrirePoids(PoidsOrdonnancement p) {
        Json o = Json.objet();
        o.set("poidsVague", p.getPoidsVague());
        o.set("poidsAttente", p.getPoidsAttente());
        o.set("poidsDeblocage", p.getPoidsDeblocage());
        o.set("pointsParMinuteAttente", p.getPointsParMinuteAttente());
        return o;
    }

    private static PoidsOrdonnancement lirePoids(Json o) {
        PoidsOrdonnancement p = new PoidsOrdonnancement();
        if (o == null || o.estNul()) {
            return p;
        }
        p.setPoidsVague(o.getDouble("poidsVague", 1.0));
        p.setPoidsAttente(o.getDouble("poidsAttente", 2.0));
        p.setPoidsDeblocage(o.getDouble("poidsDeblocage", 3.0));
        p.setPointsParMinuteAttente(o.getDouble("pointsParMinuteAttente", 1.0));
        return p;
    }
}
