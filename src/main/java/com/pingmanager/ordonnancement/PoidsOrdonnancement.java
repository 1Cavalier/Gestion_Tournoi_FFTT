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
     * Tour par tour : un match du tableau final n'est proposé que lorsque tous
     * les matchs des tours précédents de son tableau sont terminés (stratégie
     * « petit à petit »). Faux par défaut : un match est proposé dès que ses deux
     * joueurs sont connus et libres.
     */
    private boolean tourParTour = false;

    /**
     * Points de priorité par tour d'avance dans le tableau final (0 au 1er tour,
     * 1 au suivant…) : favorise les tours avancés pour atteindre vite la finale
     * (stratégie « tableaux commencés tôt »). Nul par défaut.
     */
    private double poidsProfondeur = 0.0;

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
            // Vraie conduite par vagues : un tour ne démarre qu'une fois le
            // précédent fini dans son tableau, même si des tables sont libres.
            p.setTourParTour(true);
        } else if (strategie == StrategieOrdonnancement.AVANCER_TABLEAUX_COMMENCES) {
            // 10 points par minute d'ancienneté du tableau contre 0,3 par minute
            // d'attente : l'ordre des tableaux prime presque toujours ; à
            // l'intérieur d'un tableau, les tours avancés passent devant pour
            // atteindre la finale au plus vite.
            p.setPoidsVague(0.0);
            p.setPoidsAttente(0.3);
            p.setPoidsDeblocage(1.0);
            p.setPoidsTableauCommence(10.0);
            p.setPoidsProfondeur(30.0);
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

    public boolean isTourParTour() {
        return tourParTour;
    }

    public void setTourParTour(boolean tourParTour) {
        this.tourParTour = tourParTour;
    }

    public double getPoidsProfondeur() {
        return poidsProfondeur;
    }

    public void setPoidsProfondeur(double poidsProfondeur) {
        this.poidsProfondeur = poidsProfondeur;
    }

    public double getPoidsTableauCommence() {
        return poidsTableauCommence;
    }

    public void setPoidsTableauCommence(double poidsTableauCommence) {
        this.poidsTableauCommence = poidsTableauCommence;
    }
}
