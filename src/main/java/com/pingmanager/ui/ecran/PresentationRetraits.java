package com.pingmanager.ui.ecran;

import com.pingmanager.core.ListeRetraits;
import com.pingmanager.core.ResultatActionPointage;
import com.pingmanager.core.reglement.Formats;
import com.pingmanager.model.MotifRetrait;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * B043 : textes de la vue « Retraits » et de la fenêtre « Désinscrire… » du
 * poste de pointage, sans JavaFX (testés). Les non-pointés ne sont jamais
 * présentés comme un choix du joueur.
 */
final class PresentationRetraits {

    static final String VUE_LISTE = "Liste";
    static final String TITRE_NON_POINTES = "Non pointés (place libérée à la clôture)";
    static final String TITRE_DESINSCRITS = "Désinscrits";
    static final String AUCUN_NON_POINTE = "Aucun joueur libéré à la clôture du pointage.";
    static final String AUCUN_DESINSCRIT = "Aucune désinscription.";
    static final String A_REMBOURSER = "Avait payé : à rembourser ou à reporter ?";
    static final String ANNULER = "Annuler le retrait";
    static final String AIDE = "Annuler un retrait réinscrit le joueur seulement s'il reste une place libre "
            + "(jamais en surnombre) ; il devra pointer. Aucun remboursement n'est fait par le logiciel : "
            + "le club décide.";
    static final String TITRE_FENETRE = "Désinscrire";
    static final String CONFIRMER = "Confirmer la désinscription";
    static final String PRECISION = "Précision (obligatoire pour « Autre »)";

    private PresentationRetraits() {
    }

    /** « Retraits (3) ». */
    static String ongletRetraits(int n) {
        return n == 0 ? "Retraits" : "Retraits (" + n + ")";
    }

    /** Les motifs proposés dans la fenêtre, la décision du joueur en premier. */
    static List<MotifRetrait> motifsProposes() {
        List<MotifRetrait> res = new ArrayList<>();
        res.add(MotifRetrait.DECISION_JOUEUR);
        for (MotifRetrait m : MotifRetrait.values()) {
            if (m != MotifRetrait.DECISION_JOUEUR) {
                res.add(m);
            }
        }
        return res;
    }

    /** @return vrai si la désinscription peut être confirmée (motif choisi, précision si « Autre »). */
    static boolean confirmable(MotifRetrait motif, String precision) {
        return motif != null && (!motif.exigePrecision() || (precision != null && !precision.isBlank()));
    }

    /** Nom du joueur retiré, ou sa licence si sa fiche n'a pas été gardée. */
    static String nom(ListeRetraits.Retrait r) {
        return r.joueur() != null ? PresentationPostePointage.nomAffiche(r.joueur()) : "Licence " + r.licence();
    }

    /** Ce qui s'est passé : « Non pointé à la clôture », « Décision du joueur », « Blessure ou maladie — … ». */
    static String motif(ListeRetraits.Retrait r) {
        if (r.nature() == ListeRetraits.Nature.NON_POINTE) {
            return "Non pointé à la clôture du pointage : place libérée";
        }
        String texte = r.motif().getLibelle();
        if (r.precision() != null && !r.precision().isBlank()) {
            texte += " — " + r.precision();
        }
        return texte;
    }

    /** « Tableau A · Samedi 14 · 9h12 · poste accueil-1 ». */
    static String detail(ListeRetraits.Retrait r, ZoneId fuseau) {
        return "Tableau " + r.tableau() + " · " + PresentationPostePointage.jour(r.jour()) + " · "
                + Formats.heure(r.horodatage().atZone(fuseau).toLocalTime()) + " · poste " + r.poste();
    }

    /** Message après une annulation de retrait. */
    static String messageAnnulation(ResultatActionPointage r, String nom, String tableau) {
        if (!r.estAcceptee()) {
            return r.getMotif().getMessage();
        }
        return "Retrait annulé : " + nom + " est réinscrit au tableau " + tableau
                + ". À pointer ; paiement à noter s'il y a lieu.";
    }
}
