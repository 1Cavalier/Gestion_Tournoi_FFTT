package com.pingmanager.model;

/**
 * Réglages des annonces vocales d'un tournoi (B017) : l'ordinateur annonce dans
 * la salle la rencontre envoyée sur une table.
 *
 * <p>Par défaut rien ne se dit tout seul : l'annonce automatique est coupée
 * (le bouton « Annoncer » reste disponible), l'annonce est dite deux fois,
 * sans tableau ni tour et sans rappel. Les règles (déclenchement, file, rappel)
 * sont dans {@code core.ServiceAnnoncesVocales} ; cette classe garde les
 * données, bornées pour qu'une faute de frappe ne bloque pas la salle.</p>
 */
public final class ReglagesAnnoncesVocales {

    /** Nombre de fois que l'annonce est dite à la suite, par défaut. */
    public static final int DIFFUSIONS_PAR_DEFAUT = 2;
    /** Nombre maximal de diffusions à la suite. */
    public static final int DIFFUSIONS_MAX = 3;
    /** Débit de la voix : de -10 (très lent) à 10 (très rapide), 0 = normal (échelle SAPI). */
    public static final int DEBIT_MIN = -10;
    public static final int DEBIT_MAX = 10;
    /** Délai de rappel maximal, en minutes. */
    public static final int RAPPEL_MAX_MINUTES = 30;

    private boolean annonceAutomatique;
    private String voix;
    private int debit;
    private int diffusions = DIFFUSIONS_PAR_DEFAUT;
    private int rappelMinutes;
    private boolean avecTableauEtTour;

    /**
     * L'annonce part-elle d'elle-même au lancement d'un match (si l'annonce
     * choisie comprend l'appel vocal) ?
     */
    public boolean isAnnonceAutomatique() {
        return annonceAutomatique;
    }

    public void setAnnonceAutomatique(boolean annonceAutomatique) {
        this.annonceAutomatique = annonceAutomatique;
    }

    /** Nom de la voix du système choisie, ou null pour la voix par défaut. */
    public String getVoix() {
        return voix;
    }

    public void setVoix(String voix) {
        this.voix = voix == null || voix.isBlank() ? null : voix.trim();
    }

    /** Débit de la voix, de {@link #DEBIT_MIN} à {@link #DEBIT_MAX}. */
    public int getDebit() {
        return debit;
    }

    /** Débit borné à [{@link #DEBIT_MIN}, {@link #DEBIT_MAX}]. */
    public void setDebit(int debit) {
        this.debit = Math.max(DEBIT_MIN, Math.min(DEBIT_MAX, debit));
    }

    /** Nombre de fois que l'annonce est dite à la suite (1 à {@link #DIFFUSIONS_MAX}). */
    public int getDiffusions() {
        return diffusions;
    }

    /** Diffusions bornées à [1, {@link #DIFFUSIONS_MAX}]. */
    public void setDiffusions(int diffusions) {
        this.diffusions = Math.max(1, Math.min(DIFFUSIONS_MAX, diffusions));
    }

    /** Rappel au bout de N minutes si les joueurs ne sont pas à la table ; 0 = pas de rappel. */
    public int getRappelMinutes() {
        return rappelMinutes;
    }

    /** Délai borné à [0, {@link #RAPPEL_MAX_MINUTES}]. */
    public void setRappelMinutes(int rappelMinutes) {
        this.rappelMinutes = Math.max(0, Math.min(RAPPEL_MAX_MINUTES, rappelMinutes));
    }

    /** Annonce-t-on aussi le tableau et le tour (« Tableau C, poule B ») ? */
    public boolean isAvecTableauEtTour() {
        return avecTableauEtTour;
    }

    public void setAvecTableauEtTour(boolean avecTableauEtTour) {
        this.avecTableauEtTour = avecTableauEtTour;
    }

    /** Réglages jamais touchés (rien à enregistrer : l'ancien format est gardé). */
    public boolean estVierge() {
        return !annonceAutomatique && voix == null && debit == 0 && diffusions == DIFFUSIONS_PAR_DEFAUT
                && rappelMinutes == 0 && !avecTableauEtTour;
    }
}
