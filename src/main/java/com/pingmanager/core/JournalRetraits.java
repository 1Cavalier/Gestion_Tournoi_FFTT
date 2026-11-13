package com.pingmanager.core;

import com.pingmanager.model.Joueur;
import com.pingmanager.model.MotifRetrait;
import com.pingmanager.model.OperationPointage;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.TypeOperationPointage;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * B043 : les <b>retraits</b> faits depuis l'écran du juge-arbitre (pointage d'un
 * tableau), notés au journal du pointage comme ceux du poste de pointage, pour
 * que la vue « Retraits » les montre tous, séparés :
 *
 * <ul>
 *   <li><b>non pointé</b> : à la clôture ({@link ServicePointage#cloturerPointage}),
 *       chaque inscrit qui n'a pas pointé est libéré ; c'est noté
 *       {@link TypeOperationPointage#NON_POINTE}, jamais comme un choix du joueur ;</li>
 *   <li><b>désistement</b> pendant le pointage ({@link ServicePointage#desister}) :
 *       décision du joueur ;</li>
 *   <li><b>cas particulier</b> après la clôture
 *       ({@link ServicePointage#desisterCasParticulier}) : retiré par le
 *       juge-arbitre.</li>
 * </ul>
 *
 * <p>Chaque fois : le jour du tableau, l'heure, le poste, et si le joueur avait
 * payé (aucun remboursement automatique : le club décide). La fiche du joueur est
 * gardée dans le tableau ({@link Tableau#noterRetire}) pour pouvoir annuler le
 * retrait.</p>
 */
public final class JournalRetraits {

    /** Précision notée pour un retrait pour cas particulier décidé par le juge-arbitre. */
    public static final String CAS_PARTICULIER = "Cas particulier, décision du juge-arbitre";

    private JournalRetraits() {
    }

    /** Clôture le pointage du tableau et note chaque non-pointé libéré. */
    public static ResultatPointage cloturerPointage(Tournoi tournoi, Tableau tableau, Horloge horloge, String poste) {
        List<Joueur> payes = new ArrayList<>();
        for (Joueur j : tableau.getInscrits()) {
            if (!tableau.aPointe(j) && tableau.aPaye(j)) {
                payes.add(j);
            }
        }
        ResultatPointage r = new ServicePointage(tournoi).cloturerPointage(tableau);
        for (Joueur j : r.getLiberes()) {
            tableau.noterRetire(j);
            tournoi.ajouterOperationPointage(operation(tournoi, tableau, j, TypeOperationPointage.NON_POINTE,
                    null, null, payes.contains(j), horloge, poste));
        }
        return r;
    }

    /** Désistement pendant le pointage (décision du joueur), noté au journal. */
    public static Joueur desister(Tournoi tournoi, Tableau tableau, Joueur joueur, Horloge horloge, String poste) {
        boolean payait = tableau.getInscrits().contains(joueur) && tableau.aPaye(joueur);
        Joueur repeche = new ServicePointage(tournoi).desister(tableau, joueur);
        tableau.noterRetire(joueur);
        tournoi.ajouterOperationPointage(operation(tournoi, tableau, joueur, TypeOperationPointage.DESINSCRIRE,
                MotifRetrait.DECISION_JOUEUR, null, payait, horloge, poste));
        return repeche;
    }

    /** Retrait pour cas particulier par le juge-arbitre (après clôture possible), noté au journal. */
    public static Joueur desisterCasParticulier(Tournoi tournoi, Tableau tableau, Joueur joueur, boolean rembourse,
                                               Horloge horloge, String poste) {
        boolean payait = tableau.getInscrits().contains(joueur) && tableau.aPaye(joueur);
        Joueur repeche = new ServicePointage(tournoi).desisterCasParticulier(tableau, joueur, rembourse);
        tableau.noterRetire(joueur);
        tournoi.ajouterOperationPointage(operation(tournoi, tableau, joueur, TypeOperationPointage.DESINSCRIRE,
                MotifRetrait.RETIRE_PAR_ORGANISATION,
                CAS_PARTICULIER + (rembourse ? " (remboursement noté)" : ""), payait, horloge, poste));
        return repeche;
    }

    static OperationPointage operation(Tournoi tournoi, Tableau tableau, Joueur joueur, TypeOperationPointage type,
                                       MotifRetrait motif, String precision, boolean avaitPaye, Horloge horloge,
                                       String poste) {
        Objects.requireNonNull(horloge, "horloge");
        return OperationPointage.builder()
                .licence(joueur.getNumeroLicence())
                .tableau(tableau.getNom())
                .type(type)
                .valeur(precision)
                .motifRetrait(motif)
                .avaitPaye(avaitPaye)
                .horodatage(horloge.maintenant())
                .poste(poste)
                .jour(JoursPointage.jourDe(tournoi, tableau))
                .build();
    }
}
