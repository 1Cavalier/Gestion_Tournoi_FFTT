package com.pingmanager.persistence;

import com.pingmanager.model.ReglagesAnnoncesVocales;
import com.pingmanager.model.Tournoi;
import com.pingmanager.persistence.json.Json;

/**
 * Réglages des annonces vocales en JSON (B017), champ facultatif
 * {@code annoncesVocales} du tournoi : écrit seulement si le club a touché aux
 * réglages ; un tournoi sans annonces vocales garde exactement son ancien format.
 *
 * <pre>
 * "annoncesVocales": { "annonceAutomatique": true, "voix": "Microsoft Hortense Desktop", "debit": -2,
 *   "diffusions": 2, "rappelMinutes": 5, "avecTableauEtTour": true }
 * </pre>
 *
 * <p>Relecture tolérante : une valeur absente, du mauvais type ou hors bornes
 * revient à la valeur par défaut (ou est bornée), pour que le fichier s'ouvre
 * toujours.</p>
 */
final class AnnoncesVocalesMapper {

    private AnnoncesVocalesMapper() {
    }

    static void ecrire(Tournoi t, Json o) {
        ReglagesAnnoncesVocales r = t.getAnnoncesVocales();
        if (r.estVierge()) {
            return;
        }
        Json j = Json.objet();
        j.set("annonceAutomatique", r.isAnnonceAutomatique());
        if (r.getVoix() != null) {
            j.set("voix", r.getVoix());
        }
        j.set("debit", r.getDebit());
        j.set("diffusions", r.getDiffusions());
        j.set("rappelMinutes", r.getRappelMinutes());
        j.set("avecTableauEtTour", r.isAvecTableauEtTour());
        o.set("annoncesVocales", j);
    }

    static void lire(Json o, Tournoi t) {
        Json j = o.get("annoncesVocales");
        if (j == null || !j.estObjet()) {
            return;
        }
        ReglagesAnnoncesVocales r = t.getAnnoncesVocales();
        r.setAnnonceAutomatique(booleen(j.get("annonceAutomatique"), false));
        Json voix = j.get("voix");
        r.setVoix(voix != null && voix.getType() == Json.Type.CHAINE ? voix.asString() : null);
        r.setDebit(entier(j.get("debit"), 0));
        r.setDiffusions(entier(j.get("diffusions"), ReglagesAnnoncesVocales.DIFFUSIONS_PAR_DEFAUT));
        r.setRappelMinutes(entier(j.get("rappelMinutes"), 0));
        r.setAvecTableauEtTour(booleen(j.get("avecTableauEtTour"), false));
    }

    private static boolean booleen(Json j, boolean defaut) {
        return j != null && j.getType() == Json.Type.BOOLEEN ? j.asBoolean() : defaut;
    }

    private static int entier(Json j, int defaut) {
        return j != null && j.getType() == Json.Type.NOMBRE ? j.asInt() : defaut;
    }
}
