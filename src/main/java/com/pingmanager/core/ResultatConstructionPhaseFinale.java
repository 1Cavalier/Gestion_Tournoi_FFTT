package com.pingmanager.core;

import com.pingmanager.model.TableauElimination;

import java.util.Objects;

/**
 * Résultat de la construction du tableau final d'un tableau
 * ({@link ServicePhaseFinale#construire}).
 *
 * <p>En cas de succès, porte le tableau à élimination et la <b>graine</b> du
 * tirage (pour rejouer ou vérifier le tirage public, I.304 / I.305). En cas de
 * refus, porte le {@link DiagnosticPhaseFinale} qui explique pourquoi.</p>
 */
public final class ResultatConstructionPhaseFinale {

    private final DiagnosticPhaseFinale diagnostic;
    private final TableauElimination tableau;
    private final long graine;

    private ResultatConstructionPhaseFinale(DiagnosticPhaseFinale diagnostic,
                                            TableauElimination tableau, long graine) {
        this.diagnostic = Objects.requireNonNull(diagnostic, "diagnostic");
        this.tableau = tableau;
        this.graine = graine;
    }

    static ResultatConstructionPhaseFinale construit(DiagnosticPhaseFinale apres,
                                                     TableauElimination tableau, long graine) {
        return new ResultatConstructionPhaseFinale(apres, Objects.requireNonNull(tableau, "tableau"), graine);
    }

    static ResultatConstructionPhaseFinale refuse(DiagnosticPhaseFinale diagnostic) {
        return new ResultatConstructionPhaseFinale(diagnostic, null, 0L);
    }

    /** @return true si le tableau final a été construit. */
    public boolean estConstruit() {
        return tableau != null;
    }

    /**
     * @return le diagnostic : en cas de refus, le motif ; en cas de succès,
     *         l'état après construction ({@code EN_COURS}).
     */
    public DiagnosticPhaseFinale getDiagnostic() {
        return diagnostic;
    }

    /** @return le tableau à élimination construit, ou {@code null} en cas de refus. */
    public TableauElimination getTableau() {
        return tableau;
    }

    /** @return la graine du tirage (sans objet en cas de refus). */
    public long getGraine() {
        return graine;
    }
}
