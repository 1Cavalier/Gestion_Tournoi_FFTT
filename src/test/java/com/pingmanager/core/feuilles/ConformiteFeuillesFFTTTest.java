package com.pingmanager.core.feuilles;

import com.pingmanager.core.ServiceLancementTableau;
import com.pingmanager.core.ServicePointage;
import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Match;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.format.FormatConfigurable;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Contrôle de conformité des feuilles (B016) face aux règlements sportifs. */
class ConformiteFeuillesFFTTTest {

    /**
     * 2.12.1 : « Elle se dispute au meilleur des 5 ou des 7 manches ». Écart prouvé : la
     * feuille annonce 7 manches mais la grille vierge n'a que 5 colonnes.
     */
    @Test
    void feuille_de_match_au_meilleur_des_7_manches_a_une_grille_de_7_colonnes() {
        ConfigurationTableau config = ConfigurationTableau.depuisFormat(FormatConfigurable.builder()
                .nom("S").identifiant("S7").taillePouleSouhaitee(3).qualifiesParPoule(1)
                .nombreManchesGagnantes(4).build()).build();
        Tournoi t = new Tournoi("T", "Club", LocalDate.of(2026, 11, 14));
        Tableau tableau = new Tableau("A", config);
        t.ajouterTableau(tableau);
        tableau.ouvrirPointage();
        for (int i = 1; i <= 3; i++) {
            Joueur j = new Joueur("L" + i, "Nom" + i, "P" + i, Sexe.HOMME, "Club" + i, 500 + i);
            tableau.inscrire(j);
            tableau.pointer(j);
        }
        new ServicePointage(t).cloturerPointage(tableau);
        new ServiceLancementTableau().lancer(tableau, 1L);
        Match m = tableau.getPoules().get(0).getMatchs().get(0);
        m.setNombreManchesGagnantes(4); // 7 manches (le lancement ne propage pas encore le format aux matchs)

        FeuilleMatch f = new GenerateurFeuilles().feuilleMatch(t, tableau, m).feuille();

        assertTrue(f.format().contains("7 manches"), f.format());
        assertEquals(7, f.grille().get(0).size(), "2.12.1 : une colonne par manche possible");
    }
}
