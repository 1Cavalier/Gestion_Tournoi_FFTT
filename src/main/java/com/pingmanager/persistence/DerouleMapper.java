package com.pingmanager.persistence;

import com.pingmanager.model.IssueMatch;
import com.pingmanager.model.Manche;
import com.pingmanager.model.Match;
import com.pingmanager.model.Poule;
import com.pingmanager.model.StatutMatch;
import com.pingmanager.model.Tour;
import com.pingmanager.model.TableauElimination;
import com.pingmanager.persistence.json.Json;

import java.util.ArrayList;
import java.util.List;

import static com.pingmanager.persistence.ConversionsCommunes.ecrire;
import static com.pingmanager.persistence.ConversionsCommunes.ecrireEnum;
import static com.pingmanager.persistence.ConversionsCommunes.lireDateHeure;
import static com.pingmanager.persistence.ConversionsCommunes.lireEnum;

/**
 * Traduit le déroulé sportif (matchs, manches, poules, tableau à élimination)
 * entre le modèle et le JSON.
 *
 * <p>Le point sensible est le {@link Match} : il porte son score, son issue,
 * ses pénalités, ses horodatages, et — dans un tableau à élimination — des
 * liens {@code sourceA}/{@code sourceB} vers d'autres matchs. On écrit chaque
 * match avec un identifiant stable (via {@link ContexteSerialisation}) et on
 * réfère les sources par cet identifiant, pour préserver le graphe et le
 * partage d'instances à la relecture.</p>
 */
final class DerouleMapper {

    private DerouleMapper() {
    }

    // ------------------------------------------------------------------
    // Manche
    // ------------------------------------------------------------------

    static Json ecrireManche(Manche m) {
        Json o = Json.objet();
        o.set("a", m.pointsJoueurA());
        o.set("b", m.pointsJoueurB());
        return o;
    }

    static Manche lireManche(Json o) {
        return new Manche(o.getInt("a", 0), o.getInt("b", 0));
    }

    // ------------------------------------------------------------------
    // Match
    // ------------------------------------------------------------------

    static Json ecrireMatch(Match m, ContexteSerialisation ctx) {
        Json o = Json.objet();
        o.set("id", ctx.idDeMatch(m));
        o.set("joueurA", ctx.refJoueur(m.getJoueurA()));
        o.set("joueurB", ctx.refJoueur(m.getJoueurB())); // null = bye
        o.set("statut", ecrireEnum(m.getStatut()));
        o.set("nombreManchesGagnantes", m.getNombreManchesGagnantes());
        o.set("issue", ecrireEnum(m.getIssue()));
        if (m.getJoueurFautif() != null) {
            o.set("joueurFautif", ctx.refJoueur(m.getJoueurFautif()));
        }
        if (m.getNumeroTable() != null) {
            o.set("numeroTable", m.getNumeroTable());
        }
        int penA = m.getPointsPenaliteAdverseDeA();
        int penB = m.getPointsPenaliteAdverseDeB();
        if (penA != 0) {
            o.set("penaliteContreA", penA);
        }
        if (penB != 0) {
            o.set("penaliteContreB", penB);
        }
        if (m.getInstantLancement() != null) {
            o.set("instantLancement", ecrire(m.getInstantLancement()));
        }
        if (m.getInstantFin() != null) {
            o.set("instantFin", ecrire(m.getInstantFin()));
        }
        Json manches = Json.tableau();
        for (Manche man : m.getManches()) {
            manches.add(ecrireManche(man));
        }
        o.set("manches", manches);
        // Liens de progression (tableau à élimination) : par identifiant.
        if (m.getSourceA() != null) {
            o.set("sourceA", ctx.idDeMatch(m.getSourceA()));
        }
        if (m.getSourceB() != null) {
            o.set("sourceB", ctx.idDeMatch(m.getSourceB()));
        }
        return o;
    }

    /**
     * Reconstruit un {@link Match} et l'enregistre dans le contexte par son
     * identifiant. Les liens {@code sourceA}/{@code sourceB} sont différés
     * (résolus une fois tous les matchs recréés).
     */
    static Match lireMatch(Json o, ContexteSerialisation ctx) {
        var joueurA = ctx.joueur(o.get("joueurA"));
        var joueurB = ctx.joueur(o.get("joueurB")); // peut être null (bye)
        Match m = new Match(joueurA, joueurB);

        m.setNombreManchesGagnantes(o.getInt("nombreManchesGagnantes", 3));
        if (o.has("numeroTable")) {
            m.setNumeroTable(o.get("numeroTable").asInt());
        }

        // Score et issue : reconstruire l'état exact du match.
        IssueMatch issue = lireEnum(o.get("issue"), IssueMatch.class);
        List<Manche> manches = new ArrayList<>();
        if (o.has("manches")) {
            for (Json jm : o.get("manches").elements()) {
                manches.add(lireManche(jm));
            }
        }
        if (!m.estBye()) {
            if (issue != null && issue != IssueMatch.NORMALE && o.has("joueurFautif")) {
                m.declarerIssue(issue, ctx.joueur(o.get("joueurFautif")));
            } else if (!manches.isEmpty()) {
                m.definirScore(manches);
            }
        }
        // Statut : réappliquer tel quel (definirScore/declarerIssue fixent TERMINE,
        // mais un match A_JOUER partiellement renseigné doit garder son statut).
        StatutMatch statut = lireEnum(o.get("statut"), StatutMatch.class);
        if (statut != null) {
            m.setStatut(statut);
        }

        // Pénalités en cours de match.
        int penA = o.getInt("penaliteContreA", 0);
        int penB = o.getInt("penaliteContreB", 0);
        if (penA != 0) {
            m.ajouterPointsPenalite(joueurA, penA);
        }
        if (penB != 0 && joueurB != null) {
            m.ajouterPointsPenalite(joueurB, penB);
        }

        m.setInstantLancement(lireDateHeure(o.get("instantLancement")));
        m.setInstantFin(lireDateHeure(o.get("instantFin")));

        String id = o.getString("id", null);
        ctx.enregistrerMatchLu(id, m);

        // Liens différés vers les matchs sources.
        String srcA = o.getString("sourceA", null);
        String srcB = o.getString("sourceB", null);
        if (srcA != null) {
            ctx.differerLiaison(() -> m.setSourceA(ctx.matchParId(srcA)));
        }
        if (srcB != null) {
            ctx.differerLiaison(() -> m.setSourceB(ctx.matchParId(srcB)));
        }
        return m;
    }

    // ------------------------------------------------------------------
    // Poule
    // ------------------------------------------------------------------

    static Json ecrirePoule(Poule p, ContexteSerialisation ctx) {
        Json o = Json.objet();
        o.set("nom", p.getNom());
        Json joueurs = Json.tableau();
        for (var j : p.getJoueurs()) {
            joueurs.add(ctx.refJoueur(j));
        }
        o.set("joueurs", joueurs);
        Json matchs = Json.tableau();
        for (Match m : p.getMatchs()) {
            matchs.add(ecrireMatch(m, ctx));
        }
        o.set("matchs", matchs);
        return o;
    }

    static Poule lirePoule(Json o, ContexteSerialisation ctx) {
        List<com.pingmanager.model.Joueur> joueurs = new ArrayList<>();
        for (Json ref : o.get("joueurs").elements()) {
            joueurs.add(ctx.joueur(ref));
        }
        Poule p = new Poule(o.get("nom").asString(), joueurs);
        if (o.has("matchs")) {
            for (Json jm : o.get("matchs").elements()) {
                p.ajouterMatch(lireMatch(jm, ctx));
            }
        }
        return p;
    }

    // ------------------------------------------------------------------
    // Tour + TableauElimination
    // ------------------------------------------------------------------

    static Json ecrireTour(Tour t, ContexteSerialisation ctx) {
        Json o = Json.objet();
        o.set("numero", t.getNumero());
        o.set("nom", t.getNom());
        Json matchs = Json.tableau();
        for (Match m : t.getMatchs()) {
            matchs.add(ecrireMatch(m, ctx));
        }
        o.set("matchs", matchs);
        return o;
    }

    static Tour lireTour(Json o, ContexteSerialisation ctx) {
        Tour t = new Tour(o.getInt("numero", 1), o.get("nom").asString());
        if (o.has("matchs")) {
            for (Json jm : o.get("matchs").elements()) {
                t.ajouterMatch(lireMatch(jm, ctx));
            }
        }
        return t;
    }

    static Json ecrirePhaseFinale(TableauElimination te, ContexteSerialisation ctx) {
        Json o = Json.objet();
        o.set("nombreJoueurs", te.getNombreJoueurs());
        o.set("cadre", te.getCadre());
        Json tours = Json.tableau();
        for (Tour t : te.getTours()) {
            tours.add(ecrireTour(t, ctx));
        }
        o.set("tours", tours);
        return o;
    }

    static TableauElimination lirePhaseFinale(Json o, ContexteSerialisation ctx) {
        TableauElimination te = new TableauElimination(
                o.get("nombreJoueurs").asInt(), o.get("cadre").asInt());
        if (o.has("tours")) {
            for (Json jt : o.get("tours").elements()) {
                te.ajouterTour(lireTour(jt, ctx));
            }
        }
        return te;
    }
}
