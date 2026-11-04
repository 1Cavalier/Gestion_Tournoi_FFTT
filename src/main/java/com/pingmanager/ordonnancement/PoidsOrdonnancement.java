package com.pingmanager.ordonnancement;

/**
 * Pondération configurable des critères de priorité de l'ordonnanceur.
 *
 * <p>Reflète l'équilibre souhaité entre les objectifs : avancement par vagues
 * (poules avant tableau), équité (ne pas faire attendre), et déblocage (éviter
 * de geler un tableau à cause d'un joueur inscrit ailleurs). Les valeurs par
 * défaut privilégient l'équité, conformément au réglage retenu.</p>
 */
public final class PoidsOrdonnancement {

    private double poidsVague = 1.0;
    private double poidsAttente = 2.0;   // équité : priorité forte
    private double poidsDeblocage = 3.0; // éviter de bloquer un tableau : le plus fort

    /** Points de priorité gagnés par minute d'attente d'un match prêt. */
    private double pointsParMinuteAttente = 1.0;

    public double getPoidsVague() {
        return poidsVague;
    }

    public void setPoidsVague(double poidsVague) {
        this.poidsVague = poidsVague;
    }

    public double getPoidsAttente() {
        return poidsAttente;
    }

    public void setPoidsAttente(double poidsAttente) {
        this.poidsAttente = poidsAttente;
    }

    public double getPoidsDeblocage() {
        return poidsDeblocage;
    }

    public void setPoidsDeblocage(double poidsDeblocage) {
        this.poidsDeblocage = poidsDeblocage;
    }

    public double getPointsParMinuteAttente() {
        return pointsParMinuteAttente;
    }

    public void setPointsParMinuteAttente(double pointsParMinuteAttente) {
        this.pointsParMinuteAttente = pointsParMinuteAttente;
    }
}
