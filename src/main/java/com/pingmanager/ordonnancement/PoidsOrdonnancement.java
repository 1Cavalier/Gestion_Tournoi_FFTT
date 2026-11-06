package com.pingmanager.ordonnancement;

import com.pingmanager.model.StrategieOrdonnancement;

import java.util.Objects;

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

    /**
     * Points de priorité gagnés par minute écoulée depuis le premier match lancé
     * du tableau (B025, stratégie « avancer les tableaux commencés tôt »). Nul par
     * défaut : le critère est inactif et l'ordonnanceur se comporte comme avant.
     */
    private double poidsTableauCommence = 0.0;

    /**
     * Traduit la stratégie choisie par le club en poids de l'ordonnanceur.
     *
     * <ul>
     *   <li>{@link StrategieOrdonnancement#MINIMISER_ATTENTE} : les poids historiques
     *       (attente et déblocage forts), identiques à {@code new PoidsOrdonnancement()} ;</li>
     *   <li>{@link StrategieOrdonnancement#FAIRE_AVANCER_PETIT_A_PETIT} : la vague
     *       (poules, puis 1er tour, puis tours suivants) domine ;</li>
     *   <li>{@link StrategieOrdonnancement#AVANCER_TABLEAUX_COMMENCES} : l'ancienneté
     *       du tableau domine, sans préférence de phase.</li>
     * </ul>
     */
    public static PoidsOrdonnancement pour(StrategieOrdonnancement strategie) {
        Objects.requireNonNull(strategie, "strategie");
        PoidsOrdonnancement p = new PoidsOrdonnancement();
        if (strategie == StrategieOrdonnancement.FAIRE_AVANCER_PETIT_A_PETIT) {
            // Bonus de phase 3 / 2 / 1 × 20 : une vague passe devant la suivante
            // tant que l'attente (0,2 point par minute) reste inférieure à 100 min.
            p.setPoidsVague(20.0);
            p.setPoidsAttente(0.2);
            p.setPoidsDeblocage(1.0);
        } else if (strategie == StrategieOrdonnancement.AVANCER_TABLEAUX_COMMENCES) {
            // 1 point par minute d'ancienneté du tableau contre 0,3 par minute d'attente.
            p.setPoidsVague(0.0);
            p.setPoidsAttente(0.3);
            p.setPoidsDeblocage(1.0);
            p.setPoidsTableauCommence(1.0);
        }
        // MINIMISER_ATTENTE : valeurs par défaut (réglage historique).
        return p;
    }

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

    public double getPoidsTableauCommence() {
        return poidsTableauCommence;
    }

    public void setPoidsTableauCommence(double poidsTableauCommence) {
        this.poidsTableauCommence = poidsTableauCommence;
    }
}
