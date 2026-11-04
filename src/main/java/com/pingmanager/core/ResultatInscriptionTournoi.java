package com.pingmanager.core;

import com.pingmanager.model.Tableau;

/**
 * Résultat détaillé d'une tentative d'inscription passée par le
 * {@link ServiceInscription}.
 *
 * <p>Là où {@link com.pingmanager.model.Tableau.ResultatInscription} ne décrit
 * que l'issue bas niveau au sein d'un seul tableau, ce résultat porte un
 * <b>motif précis</b> couvrant aussi les règles de niveau tournoi (quota de
 * tableaux par jour, tableaux qui se suivent), et référence le cas échéant le
 * tableau en conflit.</p>
 *
 * <p>Objet valeur immuable, obtenu via les fabriques statiques.</p>
 */
public final class ResultatInscriptionTournoi {

    /** Motif de l'issue d'une inscription. */
    public enum Motif {
        /** Inscription enregistrée dans les inscrits du tableau. */
        INSCRIT,
        /** Tableau plein : joueur placé en file d'attente. */
        EN_FILE_ATTENTE,
        /** Refus : le joueur ne satisfait pas les critères d'accès (classement, rang, sexe, catégorie). */
        NON_ELIGIBLE,
        /** Refus : le joueur est déjà inscrit (ou en file d'attente) sur ce tableau. */
        DEJA_PRESENT,
        /** Refus : le quota de tableaux par jour pour ce joueur est atteint. */
        QUOTA_TABLEAUX_ATTEINT,
        /** Refus : le tableau visé « suit » un tableau où le joueur est déjà inscrit. */
        TABLEAUX_SUCCESSIFS,
        /**
         * Refus : le jour du tableau des joueuses, une joueuse doit y être inscrite
         * avant de s'inscrire à un autre tableau (le tableau à rejoindre est donné
         * par {@link #getTableauEnConflit()}).
         */
        TABLEAU_FEMININ_OBLIGATOIRE,
        /** Refus : tableau plein et liste d'attente également pleine. */
        LISTE_ATTENTE_PLEINE,
        /** Refus : le tournoi n'accepte pas (ou plus) les inscriptions dans son statut actuel. */
        TOURNOI_FERME,
        /** Refus : catégorie d'âge non admise dans le tournoi (choix du club, {@code Tournoi.getCategoriesExclues()}). */
        CATEGORIE_NON_ADMISE,
        /** Refus : catégorie d'âge non prévue par le tableau ({@code ConfigurationTableau.categorieAutorisee}). */
        CATEGORIE_NON_AUTORISEE
    }

    private final Motif motif;
    private final Tableau tableauEnConflit;

    private ResultatInscriptionTournoi(Motif motif, Tableau tableauEnConflit) {
        this.motif = motif;
        this.tableauEnConflit = tableauEnConflit;
    }

    public static ResultatInscriptionTournoi inscrit() {
        return new ResultatInscriptionTournoi(Motif.INSCRIT, null);
    }

    public static ResultatInscriptionTournoi enFileAttente() {
        return new ResultatInscriptionTournoi(Motif.EN_FILE_ATTENTE, null);
    }

    public static ResultatInscriptionTournoi nonEligible() {
        return new ResultatInscriptionTournoi(Motif.NON_ELIGIBLE, null);
    }

    public static ResultatInscriptionTournoi dejaPresent() {
        return new ResultatInscriptionTournoi(Motif.DEJA_PRESENT, null);
    }

    public static ResultatInscriptionTournoi quotaTableauxAtteint() {
        return new ResultatInscriptionTournoi(Motif.QUOTA_TABLEAUX_ATTEINT, null);
    }

    public static ResultatInscriptionTournoi tableauxSuccessifs(Tableau tableauEnConflit) {
        return new ResultatInscriptionTournoi(Motif.TABLEAUX_SUCCESSIFS, tableauEnConflit);
    }

    public static ResultatInscriptionTournoi tableauFemininObligatoire(Tableau tableauFeminin) {
        return new ResultatInscriptionTournoi(Motif.TABLEAU_FEMININ_OBLIGATOIRE, tableauFeminin);
    }

    public static ResultatInscriptionTournoi listeAttentePleine() {
        return new ResultatInscriptionTournoi(Motif.LISTE_ATTENTE_PLEINE, null);
    }

    public static ResultatInscriptionTournoi categorieNonAdmise() {
        return new ResultatInscriptionTournoi(Motif.CATEGORIE_NON_ADMISE, null);
    }

    public static ResultatInscriptionTournoi categorieNonAutorisee() {
        return new ResultatInscriptionTournoi(Motif.CATEGORIE_NON_AUTORISEE, null);
    }

    public static ResultatInscriptionTournoi tournoiFerme() {
        return new ResultatInscriptionTournoi(Motif.TOURNOI_FERME, null);
    }

    public Motif getMotif() {
        return motif;
    }

    /**
     * @return le tableau en conflit (renseigné pour le motif
     *         {@link Motif#TABLEAUX_SUCCESSIFS}), ou {@code null}.
     */
    public Tableau getTableauEnConflit() {
        return tableauEnConflit;
    }

    /** @return true si l'inscription a été acceptée (inscrit ou en file d'attente). */
    public boolean estAccepte() {
        return motif == Motif.INSCRIT || motif == Motif.EN_FILE_ATTENTE;
    }

    /** @return true si le joueur figure désormais parmi les inscrits effectifs. */
    public boolean estInscrit() {
        return motif == Motif.INSCRIT;
    }

    @Override
    public String toString() {
        return tableauEnConflit == null
                ? "ResultatInscription[" + motif + "]"
                : "ResultatInscription[" + motif + ", conflit avec " + tableauEnConflit.getNom() + "]";
    }
}
