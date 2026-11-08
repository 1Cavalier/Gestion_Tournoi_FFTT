package com.pingmanager.ui.ecran;

import com.pingmanager.core.ServiceLancementTableau;
import com.pingmanager.core.ServicePointage;
import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Match;
import com.pingmanager.model.Poule;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.format.RegistreFormats;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Préparation des PDF des boutons d'impression du jour J (sans visionneuse). */
class ImpressionJourJTest {

    private static Tournoi tournoi() {
        Tournoi t = new Tournoi("Open", "Club", LocalDate.of(2026, 11, 14));
        Tableau tableau = new Tableau("C – Toutes séries",
                ConfigurationTableau.depuisFormat(RegistreFormats.formatParDefaut()).build());
        t.ajouterTableau(tableau);
        tableau.ouvrirPointage();
        for (int i = 1; i <= 9; i++) {
            Joueur j = new Joueur("L" + i, "Nom" + i, "Prénom", Sexe.HOMME, "Club", 500 + i);
            tableau.inscrire(j);
            tableau.pointer(j);
        }
        new ServicePointage(t).cloturerPointage(tableau);
        new ServiceLancementTableau().lancer(tableau, 1L);
        return t;
    }

    private static boolean estPdf(byte[] b) {
        return b != null && b.length > 4 && b[0] == '%' && b[1] == 'P' && b[2] == 'D' && b[3] == 'F';
    }

    @Test
    void feuille_d_un_match_envoye_porte_le_numero_de_table_dans_le_nom() {
        Tournoi t = tournoi();
        Match m = t.getTableaux().get(0).getPoules().get(0).getMatchs().get(0);
        m.setNumeroTable(4);

        ImpressionJourJ.Preparation p = ImpressionJourJ.feuilleMatch(t, m);

        assertTrue(p.estPrete(), p.refus());
        assertTrue(estPdf(p.pdf()));
        assertEquals("feuille-match-Table 4", p.nom());
    }

    @Test
    void plusieurs_matchs_proposes_dans_un_seul_pdf_et_matchs_sans_feuille_ignores() {
        Tournoi t = tournoi();
        Poule p = t.getTableaux().get(0).getPoules().get(0);
        Match etranger = new Match(new Joueur("X", "X", "X", Sexe.HOMME, "", 0), new Joueur("Y", "Y", "Y", Sexe.HOMME, "", 0));

        ImpressionJourJ.Preparation r = ImpressionJourJ.feuillesMatchs(t,
                List.of(p.getMatchs().get(0), etranger, p.getMatchs().get(1)));

        assertTrue(r.estPrete());
        assertEquals("feuilles-match-2", r.nom());
    }

    @Test
    void refus_explique_quand_aucune_feuille() {
        Tournoi t = tournoi();
        Match etranger = new Match(new Joueur("X", "X", "X", Sexe.HOMME, "", 0), new Joueur("Y", "Y", "Y", Sexe.HOMME, "", 0));

        ImpressionJourJ.Preparation r = ImpressionJourJ.feuilleMatch(t, etranger);
        ImpressionJourJ.Message m = ImpressionJourJ.imprimer(r, false);

        assertFalse(r.estPrete());
        assertTrue(m.erreur());
        assertEquals("Ce match n'appartient à aucun tableau du tournoi.", m.texte());
    }

    @Test
    void feuille_de_poule_feuilles_de_la_poule_et_arbre_refuse_avant_le_tableau_final() {
        Tournoi t = tournoi();
        Tableau tableau = t.getTableaux().get(0);
        Poule p = tableau.getPoules().get(0);

        assertTrue(estPdf(ImpressionJourJ.feuillePoule(t, tableau, p).pdf()));
        assertTrue(estPdf(ImpressionJourJ.feuillesMatchsPoule(t, tableau, p).pdf()));
        ImpressionJourJ.Preparation arbre = ImpressionJourJ.arbre(t, tableau);
        assertFalse(arbre.estPrete());
        assertTrue(arbre.refus().contains("tableau final"));
    }
}
