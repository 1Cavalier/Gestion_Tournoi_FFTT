package com.pingmanager.core;

import com.pingmanager.model.StatutTableau;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.TableauElimination;
import com.pingmanager.model.Tour;

import java.util.List;
import java.util.Objects;

/**
 * Étape d'un tableau le jour J (frise en six étapes de l'écran du
 * juge-arbitre), calculée à partir de son état réel : statut, pointage clôturé
 * ou non, poules terminées, tour courant du tableau final, finale jouée.
 *
 * <p>Le calcul lui-même ({@link #calculer(Situation)}) est une fonction pure
 * sur une {@link Situation} ; {@link #calculer(Tableau)} se contente de lire
 * cette situation dans le tableau. Pas de {@code switch} sur une énumération :
 * des chaînes de {@code if}.</p>
 */
public final class CalculEtapeTableau {

    /** Précision affichée quand les poules sont jouées et que le tableau final reste à tirer. */
    public static final String TABLEAU_FINAL_A_TIRER = "Tableau final à tirer";

    private CalculEtapeTableau() {
    }

    /**
     * Ce qui décide de l'étape, lu dans le tableau.
     *
     * @param statut                 statut du tableau (jamais nul)
     * @param pointageCloture        le pointage a-t-il été clôturé ?
     * @param poulesTerminees        toutes les poules sont-elles jouées (et y en a-t-il) ?
     * @param sansTableauFinal       le format s'arrête-t-il aux poules ?
     * @param tableauFinalConstruit  le tableau à élimination directe est-il tiré ?
     * @param tourEnCours            tour courant du tableau final (« 1/8 de finale »), vide sinon
     * @param tableauFinalTermine    finale (et petite finale s'il y en a une) jouée(s) ?
     */
    public record Situation(StatutTableau statut, boolean pointageCloture, boolean poulesTerminees,
                            boolean sansTableauFinal, boolean tableauFinalConstruit, String tourEnCours,
                            boolean tableauFinalTermine) {

        public Situation {
            Objects.requireNonNull(statut, "statut");
            tourEnCours = tourEnCours == null ? "" : tourEnCours;
        }
    }

    /**
     * Étape courante et sa précision : le tour en cours du tableau final
     * (« Quarts de finale »), ou {@link #TABLEAU_FINAL_A_TIRER} en fin de poules ;
     * vide sinon.
     */
    public record EtapeCourante(EtapeTableau etape, String precision) {

        public EtapeCourante {
            Objects.requireNonNull(etape, "etape");
            precision = precision == null ? "" : precision;
        }

        /** Libellé à afficher : la précision si elle existe (« 1/8 de finale »), sinon celui de l'étape. */
        public String libelle() {
            return precision.isEmpty() ? etape.libelle() : precision;
        }
    }

    /** Fonction pure : l'étape correspondant à une situation. */
    public static EtapeCourante calculer(Situation s) {
        Objects.requireNonNull(s, "situation");
        StatutTableau statut = s.statut();
        if (statut == StatutTableau.INSCRIPTION) {
            return new EtapeCourante(EtapeTableau.AVANT_POINTAGE, "");
        }
        if (statut == StatutTableau.POINTAGE) {
            return new EtapeCourante(s.pointageCloture() ? EtapeTableau.POINTAGE_FERME
                    : EtapeTableau.POINTAGE_OUVERT, "");
        }
        if (statut == StatutTableau.PHASE_FINALE && s.tableauFinalConstruit()) {
            if (s.tableauFinalTermine()) {
                return new EtapeCourante(EtapeTableau.TERMINE, "");
            }
            return new EtapeCourante(EtapeTableau.TABLEAU_FINAL, s.tourEnCours());
        }
        // LANCE (ou PHASE_FINALE sans arbre, fichier incohérent) : phase de poules.
        if (s.poulesTerminees()) {
            if (s.sansTableauFinal()) {
                return new EtapeCourante(EtapeTableau.TERMINE, "");
            }
            return new EtapeCourante(EtapeTableau.POULES, TABLEAU_FINAL_A_TIRER);
        }
        return new EtapeCourante(EtapeTableau.POULES, "");
    }

    /** Étape d'un tableau, d'après son état réel. */
    public static EtapeCourante calculer(Tableau t) {
        return calculer(situation(t));
    }

    /** Lit dans le tableau ce qui décide de son étape. */
    public static Situation situation(Tableau t) {
        Objects.requireNonNull(t, "tableau");
        boolean poulesTerminees = t.poulesTerminees();
        boolean sansFinal = poulesTerminees && !t.aPhaseFinale() && TextesSuivi.sansTableauFinal(t);
        TableauElimination elim = t.getPhaseFinale();
        boolean construit = elim != null;
        boolean termine = construit && ServicePhaseFinale.estTerminee(elim);
        String tour = construit && !termine ? tourEnCours(elim) : "";
        return new Situation(t.getStatut(), t.pointageEstCloture(), poulesTerminees, sansFinal, construit, tour,
                termine);
    }

    /**
     * Tour en cours du tableau final : le premier tour du tableau principal qui
     * n'est pas entièrement joué (« Quarts de finale »), « Finale et petite
     * finale » quand les deux restent à jouer, « Petite finale » si elle seule
     * reste, « Terminé » sinon.
     */
    public static String tourEnCours(TableauElimination elim) {
        List<Tour> principaux = ServicePhaseFinale.toursPrincipaux(elim);
        Tour pf = ServicePhaseFinale.petiteFinale(elim);
        boolean pfAJouer = pf != null && !pf.estTermine();
        for (int i = 0; i < principaux.size(); i++) {
            if (!principaux.get(i).estTermine()) {
                int rang = principaux.size() - 1 - i;
                if (rang == 0 && pfAJouer) {
                    return "Finale et petite finale";
                }
                return TextesSuivi.libelleTour(rang);
            }
        }
        return pfAJouer ? "Petite finale" : "Terminé";
    }
}
