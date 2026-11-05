package com.pingmanager.core;

import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.format.RegistreFormats;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Compléments du pointage utilisés par l'écran du jour J. */
class PointageJourJTest {

    private static Joueur j(String licence) {
        return new Joueur(licence, "N" + licence, "P" + licence, Sexe.HOMME, "Club", 1000);
    }

    @Test
    void annuler_un_paiement_coche_par_erreur() {
        Tableau tab = new Tableau("A", ConfigurationTableau.depuisFormat(RegistreFormats.formatParDefaut())
                .placesMax(4).build());
        Tournoi t = new Tournoi("Open", "Club", LocalDate.of(2026, 11, 14));
        t.ajouterTableau(tab);
        tab.inscrire(j("L1"));
        tab.ouvrirPointage();
        ServicePointage service = new ServicePointage(t);
        service.pointer(tab, j("L1"));
        service.marquerPaye(tab, j("L1"));
        service.annulerPaiement(tab, j("L1"));
        assertFalse(tab.aPaye(j("L1")));
        assertTrue(tab.aPointe(j("L1")), "le pointage n'est pas touché");
        assertThrows(IllegalArgumentException.class, () -> service.annulerPaiement(tab, j("ABSENT")));
    }

    @Test
    void non_pointes_donne_les_inscrits_restant_a_pointer_dans_l_ordre() {
        Tableau tab = new Tableau("A", ConfigurationTableau.depuisFormat(RegistreFormats.formatParDefaut())
                .placesMax(0).build());
        for (int i = 1; i <= 4; i++) {
            tab.inscrire(j("L" + i));
        }
        tab.ouvrirPointage();
        tab.pointer(j("L2"));
        assertEquals(List.of(j("L1"), j("L3"), j("L4")), ServicePointage.nonPointes(tab));
    }
}
