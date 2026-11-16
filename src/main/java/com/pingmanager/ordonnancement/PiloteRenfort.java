package com.pingmanager.ordonnancement;

import java.util.Objects;

/**
 * Pilote des tables de renfort (B054) : à partir du retard mesuré
 * ({@link EvaluationRetard}) et du {@link ReglageRenfort}, il <b>propose</b>
 * d'ouvrir ou de rendre des tables d'échauffement ; le juge-arbitre
 * <b>décide</b> (3.3.1.2.2 : il programme les parties selon les tables
 * disponibles).
 *
 * <ul>
 *   <li>{@link #accepter} applique la proposition ;</li>
 *   <li>{@link #refuser} l'écarte : la même action n'est plus proposée tant
 *   que la situation ne change pas (une autre action, ou plus rien, est
 *   proposée entre-temps) ;</li>
 *   <li>{@link #forcer} pose un nombre de tables choisi par le juge-arbitre,
 *   même sans retard ; les propositions sont alors suspendues jusqu'à
 *   {@link #reprendreLesPropositions()}.</li>
 * </ul>
 *
 * <p>Le nombre de tables en service reste toujours entre 0 et
 * {@link ReglageRenfort#getTablesMax()}. La simulation accepte chaque
 * proposition (juge-arbitre qui suit l'application).</p>
 */
public final class PiloteRenfort {

    private final ReglageRenfort reglage;
    private final int tablesCompetition;
    private int enService;
    private boolean force;
    private PropositionRenfort.Action refusee;

    /**
     * @param reglage           tables permises et seuils
     * @param tablesCompetition tables de compétition (au moins une)
     */
    public PiloteRenfort(ReglageRenfort reglage, int tablesCompetition) {
        this.reglage = Objects.requireNonNull(reglage, "reglage");
        this.tablesCompetition = Math.max(1, tablesCompetition);
    }

    public ReglageRenfort getReglage() {
        return reglage;
    }

    /** @return les tables de renfort en service, de 0 au maximum réglé. */
    public int getTablesEnService() {
        return enService;
    }

    /** @return vrai si le juge-arbitre a forcé le nombre de tables (propositions suspendues). */
    public boolean estForce() {
        return force;
    }

    /**
     * Proposition pour l'instant mesuré. Ne modifie rien (sauf l'oubli d'un
     * refus devenu sans objet).
     */
    public PropositionRenfort proposer(EvaluationRetard evaluation) {
        Objects.requireNonNull(evaluation, "evaluation");
        long retard = evaluation.retardPrevuMinutes(tablesCompetition);
        if (!reglage.estActif()) {
            return aucune(retard, "Aucune table d'échauffement n'est prévue en renfort.");
        }
        if (force) {
            return aucune(retard, "Le juge-arbitre a fixé lui-même les tables de renfort.");
        }
        PropositionRenfort calculee = calculer(evaluation, retard);
        if (calculee.action() != refusee) {
            refusee = null; // la situation a changé : le refus ne vaut plus
            return calculee;
        }
        return aucune(retard, "Proposition refusée par le juge-arbitre.");
    }

    private PropositionRenfort calculer(EvaluationRetard e, long retard) {
        int max = reglage.getTablesMax();
        if (retard > reglage.getSeuilDeploiementMinutes() && enService < max) {
            int besoin = e.tablesNecessaires(tablesCompetition, max, reglage.getSeuilDeploiementMinutes());
            int cible = Math.max(enService + 1, Math.min(max, besoin));
            return new PropositionRenfort(PropositionRenfort.Action.DEPLOYER, cible, enService, retard,
                    "Retard prévu de " + retard + " min (au-delà de " + reglage.getSeuilDeploiementMinutes()
                            + " min) : ouvrir " + (cible - enService) + " table(s) d'échauffement.");
        }
        if (enService > 0 && retard <= reglage.getSeuilRetraitMinutes()) {
            return new PropositionRenfort(PropositionRenfort.Action.RETIRER, 0, enService, retard,
                    "Retard résorbé : rendre les " + enService + " table(s) à l'échauffement.");
        }
        return aucune(retard, enService > 0 ? "Renfort en service." : "Pas de retard à rattraper.");
    }

    private PropositionRenfort aucune(long retard, String motif) {
        return new PropositionRenfort(PropositionRenfort.Action.AUCUNE, enService, enService, retard, motif);
    }

    /** Applique une proposition ; sans effet si elle ne demande rien. */
    public void accepter(PropositionRenfort p) {
        Objects.requireNonNull(p, "p");
        if (p.demandeUnChangement()) {
            enService = borner(p.tablesCible());
            refusee = null;
        }
    }

    /** Écarte une proposition : la même action n'est plus proposée tant que la situation ne change pas. */
    public void refuser(PropositionRenfort p) {
        Objects.requireNonNull(p, "p");
        if (p.demandeUnChangement()) {
            refusee = p.action();
        }
    }

    /**
     * Le juge-arbitre impose le nombre de tables de renfort en service, avec ou
     * sans retard ; les propositions sont suspendues.
     *
     * @throws IllegalArgumentException si {@code tables} sort de 0 à {@link ReglageRenfort#getTablesMax()}
     */
    public void forcer(int tables) {
        if (tables < 0 || tables > reglage.getTablesMax()) {
            throw new IllegalArgumentException("Le renfort va de 0 à " + reglage.getTablesMax() + " table(s).");
        }
        enService = tables;
        force = true;
        refusee = null;
    }

    /** Fin du forçage : les propositions reprennent, les tables en service restent. */
    public void reprendreLesPropositions() {
        force = false;
    }

    private int borner(int tables) {
        return Math.max(0, Math.min(reglage.getTablesMax(), tables));
    }
}
