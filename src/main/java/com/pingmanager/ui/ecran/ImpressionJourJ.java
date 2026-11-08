package com.pingmanager.ui.ecran;

import com.pingmanager.core.feuilles.ArbreImprimable;
import com.pingmanager.core.feuilles.FeuilleMatch;
import com.pingmanager.core.feuilles.FeuillePoule;
import com.pingmanager.core.feuilles.GenerateurFeuilles;
import com.pingmanager.core.feuilles.ResultatFeuille;
import com.pingmanager.model.Match;
import com.pingmanager.model.Poule;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.ui.infra.ImpressionFeuilles;
import com.pingmanager.ui.infra.RenduPdfFeuilles;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Boutons d'impression du jour J (poules, arbre, salle) : prépare le PDF d'une
 * feuille (contenu du cœur, rendu {@link RenduPdfFeuilles}) puis l'enregistre
 * et l'ouvre ({@link ImpressionFeuilles}). La préparation, sans JavaFX ni
 * visionneuse, est testée dans {@code ImpressionJourJTest}.
 */
final class ImpressionJourJ {

    private static final GenerateurFeuilles FEUILLES = new GenerateurFeuilles();

    private ImpressionJourJ() {
    }

    /**
     * Un PDF prêt à imprimer, ou la raison du refus.
     *
     * @param pdf   contenu, ou {@code null} si refusé
     * @param nom   début du nom de fichier
     * @param refus explication du refus, ou vide
     */
    record Preparation(byte[] pdf, String nom, String refus) {
        boolean estPrete() {
            return pdf != null;
        }

        static Preparation refusee(String refus) {
            return new Preparation(null, "", refus);
        }
    }

    /** Message à afficher après un clic sur un bouton d'impression. */
    record Message(boolean erreur, String texte) {
    }

    // ------------------------------------------------------------------
    // Préparation (testée)
    // ------------------------------------------------------------------

    /** Feuille de match d'une partie (poule ou tableau final). */
    static Preparation feuilleMatch(Tournoi tournoi, Match match) {
        return feuillesMatchs(tournoi, List.of(match));
    }

    /**
     * Feuilles de plusieurs parties dans un seul PDF (deux par page), par
     * exemple tous les matchs envoyés par « Proposer ». Les parties sans
     * feuille (exempt, joueurs inconnus) sont ignorées ; refus si aucune.
     */
    static Preparation feuillesMatchs(Tournoi tournoi, List<Match> matchs) {
        List<FeuilleMatch> feuilles = new ArrayList<>();
        String refus = "Aucune feuille de match à imprimer.";
        for (Match m : matchs) {
            ResultatFeuille<FeuilleMatch> r = FEUILLES.feuilleMatch(tournoi, m);
            if (r.estPrete()) {
                feuilles.add(r.feuille());
            } else {
                refus = r.message();
            }
        }
        if (feuilles.isEmpty()) {
            return Preparation.refusee(refus);
        }
        FeuilleMatch premiere = feuilles.get(0);
        String nom = feuilles.size() == 1
                ? "feuille-match-" + (premiere.table().isEmpty() ? premiere.entete().tableau() : premiere.table())
                : "feuilles-match-" + feuilles.size();
        return rendre(() -> RenduPdfFeuilles.feuillesMatch(feuilles), nom);
    }

    /** Feuilles de match de toutes les parties d'une poule, dans l'ordre I.301. */
    static Preparation feuillesMatchsPoule(Tournoi tournoi, Tableau tableau, Poule poule) {
        List<FeuilleMatch> feuilles = FEUILLES.feuillesMatchsPoule(tournoi, tableau, poule);
        if (feuilles.isEmpty()) {
            return Preparation.refusee("Cette poule n'a aucune partie à jouer.");
        }
        return rendre(() -> RenduPdfFeuilles.feuillesMatch(feuilles),
                "feuilles-match-" + tableau.getNom() + "-poule-" + poule.getNom());
    }

    /** Feuille de poule suivie de la feuille de rencontre. */
    static Preparation feuillePoule(Tournoi tournoi, Tableau tableau, Poule poule) {
        ResultatFeuille<FeuillePoule> r = FEUILLES.feuillePoule(tournoi, tableau, poule);
        if (!r.estPrete()) {
            return Preparation.refusee(r.message());
        }
        return rendre(() -> RenduPdfFeuilles.feuillePoule(r.feuille()),
                "feuille-poule-" + tableau.getNom() + "-" + poule.getNom());
    }

    /** Tableau final imprimable (arbre). */
    static Preparation arbre(Tournoi tournoi, Tableau tableau) {
        ResultatFeuille<ArbreImprimable> r = FEUILLES.arbre(tournoi, tableau);
        if (!r.estPrete()) {
            return Preparation.refusee(r.message());
        }
        return rendre(() -> RenduPdfFeuilles.arbre(r.feuille()), "tableau-final-" + tableau.getNom());
    }

    private interface Rendu {
        byte[] pdf() throws IOException;
    }

    private static Preparation rendre(Rendu rendu, String nom) {
        try {
            return new Preparation(rendu.pdf(), nom, "");
        } catch (IOException | RuntimeException e) {
            return Preparation.refusee("Le PDF n'a pas pu être préparé (" + e.getMessage() + ").");
        }
    }

    // ------------------------------------------------------------------
    // Impression (enregistrement puis visionneuse)
    // ------------------------------------------------------------------

    /** Enregistre et ouvre la feuille ; {@code direct} : vers l'imprimante si le système le permet. */
    static Message imprimer(Preparation p, boolean direct) {
        if (!p.estPrete()) {
            return new Message(true, p.refus());
        }
        ImpressionFeuilles.Resultat r = ImpressionFeuilles.ouvrir(p.pdf(), p.nom(), direct);
        return new Message(!r.estReussi(), r.message());
    }
}
