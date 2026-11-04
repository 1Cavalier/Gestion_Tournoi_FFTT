package com.pingmanager.persistence;

import com.pingmanager.core.RegistreDisciplinaire;
import com.pingmanager.model.Tournoi;
import com.pingmanager.ordonnancement.EstimateurDuree;
import com.pingmanager.ordonnancement.Ordonnanceur;
import com.pingmanager.persistence.json.Json;

/**
 * Sérialise et reconstruit une {@link SessionSalle} complète : le tournoi, sa
 * discipline et l'état vivant de l'ordonnancement, dans un seul document.
 *
 * <p>Le contexte de sérialisation est partagé entre toutes les parties : les
 * joueurs vont dans un annuaire unique et les matchs reçoivent un identifiant
 * unique. L'ordonnancement peut ainsi référencer exactement les mêmes instances
 * de match que le tournoi, et la relecture les relie sans duplication.</p>
 *
 * <p>Ordre d'écriture : on écrit d'abord le tournoi et la discipline, puis
 * l'ordonnancement (qui référence des matchs déjà identifiés), et seulement
 * ensuite l'annuaire des joueurs — car il n'est complet qu'une fois toutes les
 * parties parcourues. À la lecture, l'annuaire est chargé en premier, puis le
 * tournoi, puis l'ordonnancement, et les liaisons de matchs sont résolues à la
 * fin.</p>
 */
public final class SessionMapperComplet {

    /** Version du format de document de session. */
    public static final int VERSION = 1;

    private SessionMapperComplet() {
    }

    // ==================================================================
    // Écriture
    // ==================================================================

    public static Json ecrire(SessionSalle session) {
        ContexteSerialisation ctx = new ContexteSerialisation();

        Json jTournoi = TournoiMapper.ecrirePartieTournoi(session.getTournoi(), ctx);
        Json jDiscipline = TournoiMapper.ecrirePartieDiscipline(session.getDiscipline(), ctx);
        Json jOrdonnanceur = SessionMapper.ecrireOrdonnanceur(session.getOrdonnanceur(), ctx);
        Json jEstimateur = SessionMapper.ecrireEstimateur(session.getEstimateur());

        Json doc = Json.objet();
        doc.set("version", VERSION);
        doc.set("kind", "session"); // marqueur : document de session (autosave)
        doc.set("joueurs", ctx.ecrireAnnuaire());
        doc.set("tournoi", jTournoi);
        if (jDiscipline != null) {
            doc.set("discipline", jDiscipline);
        }
        if (!jOrdonnanceur.estNul()) {
            doc.set("ordonnanceur", jOrdonnanceur);
        }
        if (!jEstimateur.estNul()) {
            doc.set("estimateur", jEstimateur);
        }
        return doc;
    }

    // ==================================================================
    // Lecture
    // ==================================================================

    public static SessionSalle lire(Json doc) {
        int version = doc.getInt("version", 1);
        if (version > VERSION) {
            throw new com.pingmanager.persistence.json.JsonException(
                    "Version de session " + version + " plus récente que celle supportée ("
                            + VERSION + ").");
        }
        ContexteSerialisation ctx = new ContexteSerialisation();
        ctx.lireAnnuaire(doc.get("joueurs"));

        Tournoi tournoi = TournoiMapper.lirePartieTournoi(doc.get("tournoi"), ctx);

        // Résoudre le graphe de matchs avant de brancher l'ordonnancement, qui
        // référence ces mêmes matchs.
        ctx.resoudreLiaisons();

        RegistreDisciplinaire discipline = TournoiMapper.lirePartieDiscipline(doc.get("discipline"), ctx);

        Ordonnanceur ordonnanceur = doc.has("ordonnanceur")
                ? SessionMapper.lireOrdonnanceur(doc.get("ordonnanceur"), ctx)
                : null;
        EstimateurDuree estimateur = doc.has("estimateur")
                ? SessionMapper.lireEstimateur(doc.get("estimateur"))
                : null;

        return new SessionSalle(tournoi, discipline, ordonnanceur, estimateur);
    }
}
