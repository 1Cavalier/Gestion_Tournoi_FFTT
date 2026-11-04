package com.pingmanager.core;

import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.StatutTournoi;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.format.RegistreFormats;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Vérifie que le déroulé (pointage, lancement) est bien couplé au statut du
 * tournoi : refusé si le tournoi est annulé (mode par défaut) ou non publié
 * (mode strict), autorisé sinon.
 */
class CoutureStatutDerouleTest {

    private Tournoi tournoiAvecTableau() {
        Tournoi t = new Tournoi("Open", "C", LocalDate.of(2026, 3, 8));
        Tableau tab = new Tableau("A", ConfigurationTableau.builder()
                .format(RegistreFormats.formatParDefaut()).placesMax(16).build());
        t.ajouterTableau(tab);
        return t;
    }

    private Joueur joueur(String lic) {
        return new Joueur(lic, "N", "P", Sexe.HOMME, "Club", 800);
    }

    // --- Pointage -------------------------------------------------------------

    @Test
    void pointage_refuse_si_tournoi_annule() {
        Tournoi t = tournoiAvecTableau();
        Tableau tab = t.getTableaux().get(0);
        Joueur j = joueur("L1");
        tab.inscrire(j);

        ServicePointage pointage = new ServicePointage(t); // défaut REFUSER_SI_ANNULE
        // En brouillon : autorisé.
        pointage.pointer(tab, j);

        // Annulé : refusé.
        t.changerStatut(StatutTournoi.ANNULE);
        assertThrows(IllegalStateException.class, () -> pointage.pointer(tab, j));
    }

    @Test
    void pointage_mode_strict_refuse_hors_publie() {
        Tournoi t = tournoiAvecTableau();
        Tableau tab = t.getTableaux().get(0);
        Joueur j = joueur("L1");
        tab.inscrire(j);

        ServicePointage strict = new ServicePointage(t, PolitiqueOuverture.EXIGER_PUBLIE);
        assertThrows(IllegalStateException.class, () -> strict.pointer(tab, j),
                "brouillon refusé en mode strict");
    }

    // --- Lancement ------------------------------------------------------------

    @Test
    void lancement_refuse_si_tournoi_annule() {
        Tournoi t = tournoiAvecTableau();
        Tableau tab = t.getTableaux().get(0);
        t.changerStatut(StatutTournoi.ANNULE);

        ServiceLancementTableau service = new ServiceLancementTableau(t);
        assertThrows(IllegalStateException.class, () -> service.lancer(tab, 1L));
    }

    @Test
    void lancement_sans_tournoi_associe_reste_possible() {
        // Rétrocompatibilité : sans tournoi injecté, pas de contrôle de statut
        // (le contrôle de phase du tableau s'applique quand même, d'où l'échec ici
        // sur un tableau non pointé — on vérifie juste que ce n'est PAS un refus lié
        // au statut du tournoi).
        Tournoi t = tournoiAvecTableau();
        Tableau tab = t.getTableaux().get(0);
        ServiceLancementTableau service = new ServiceLancementTableau(); // sans tournoi
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> service.lancer(tab, 1L));
        assertFalse(e.getMessage().contains("le tournoi est"),
                "l'échec vient de la phase du tableau, pas du statut du tournoi");
    }
}
