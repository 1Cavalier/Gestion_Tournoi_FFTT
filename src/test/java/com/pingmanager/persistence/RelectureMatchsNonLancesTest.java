package com.pingmanager.persistence;

import com.pingmanager.core.RegistreDisciplinaire;
import com.pingmanager.core.SaisieResultatMatch;
import com.pingmanager.core.ServiceLancementTableau;
import com.pingmanager.core.ServicePhaseFinale;
import com.pingmanager.core.ServicePointage;
import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.IssueMatch;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Manche;
import com.pingmanager.model.Match;
import com.pingmanager.model.Poule;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.StatutMatch;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.format.RegistreFormats;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Anciens fichiers : avant la règle « pas de score sans lancement sur une
 * table », des matchs pouvaient être terminés sans avoir jamais été lancés
 * (ni table, ni heure de début). Ils se relisent comme avant : la relecture
 * passe par le modèle, pas par le service de saisie, et ne bute donc pas sur
 * la règle. Le classement et la construction du tableau final ne changent pas.
 */
class RelectureMatchsNonLancesTest {

    /** Tournoi dont toutes les poules sont terminées sans aucun lancement, dont un forfait. */
    private static Tournoi ancienTournoi() {
        Tournoi t = new Tournoi("Open ancien", "Club", List.of(LocalDate.of(2026, 5, 10)));
        Tableau tableau = new Tableau("Série A", ConfigurationTableau.depuisFormat(RegistreFormats.formatParDefaut()).build());
        t.ajouterTableau(tableau);
        tableau.ouvrirPointage();
        for (int i = 1; i <= 12; i++) {
            Joueur j = new Joueur("L" + i, "Nom" + i, "Prenom" + i, Sexe.HOMME, "Club" + (i % 5), 600 + 15 * i);
            tableau.inscrire(j);
            tableau.pointer(j);
        }
        new ServicePointage(t).cloturerPointage(tableau);
        new ServiceLancementTableau().lancer(tableau, 3L);
        boolean premier = true;
        for (Poule p : tableau.getPoules()) {
            for (Match m : p.getMatchs()) {
                // Ce qu'enregistrait l'ancienne saisie : résultat posé sans lancement.
                if (premier) {
                    m.declarerIssue(IssueMatch.ABANDON, m.getJoueurB());
                    premier = false;
                } else {
                    m.definirScore(List.of(new Manche(11, 4), new Manche(11, 4), new Manche(11, 4)));
                }
            }
        }
        return t;
    }

    @Test
    void ancien_fichier_avec_matchs_termines_non_lances_se_recharge_et_le_tableau_final_se_construit()
            throws Exception {
        Tournoi original = ancienTournoi();
        Path fichier = Files.createTempFile("ancien_non_lance", DepotTournoi.EXTENSION);
        DepotTournoi.sauvegarder(original, new RegistreDisciplinaire(), fichier);

        Tournoi relu = DepotTournoi.charger(fichier).getTournoi();
        Tableau tableau = relu.getTableaux().get(0);
        int termines = 0;
        for (Poule p : tableau.getPoules()) {
            for (Match m : p.getMatchs()) {
                assertEquals(StatutMatch.TERMINE, m.getStatut());
                assertNull(m.getNumeroTable(), "jamais lancé sur une table");
                assertNull(m.getInstantLancement());
                assertNotNull(m.getVainqueur());
                termines++;
            }
        }
        assertTrue(termines > 0);
        Match abandon = tableau.getPoules().get(0).getMatchs().get(0);
        assertEquals(IssueMatch.ABANDON, abandon.getIssue(), "issue relue telle quelle");
        assertTrue(tableau.poulesTerminees());

        // Progression inchangée : le tableau final se construit à partir de ces poules.
        assertTrue(new ServicePhaseFinale().construire(tableau, 77L).estConstruit());

        // Un score relu reste corrigeable (match déjà joué), sans relancer le match.
        Match joue = tableau.getPoules().get(0).getMatchs().get(1);
        assertTrue(SaisieResultatMatch.scoreSaisissable(joue));
    }
}
