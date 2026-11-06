package com.pingmanager.ui.ecran;

import com.pingmanager.core.ComparaisonStrategies;
import com.pingmanager.core.ResultatSimulation;
import com.pingmanager.core.ResultatSimulation.BilanTableau;
import com.pingmanager.model.StrategieOrdonnancement;
import com.pingmanager.model.Tournoi;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Écran « Prévisions et stratégie » (B025) : mots, chiffres et couleurs, sans JavaFX. */
class PresentationPrevisionsTest {

    private static final LocalDateTime J = LocalDateTime.of(2027, 4, 10, 9, 0);

    private static ResultatSimulation resultat(StrategieOrdonnancement s, LocalDateTime finA, LocalDateTime finB,
                                               double attente, long max) {
        List<BilanTableau> tableaux = List.of(
                new BilanTableau("A", J, finA, J.withHour(13), 24, 39, attente, max),
                new BilanTableau("B", J.plusHours(1), finB, J.withHour(20), 18, 30, attente, max));
        LocalDateTime fin = finA.isAfter(finB) ? finA : finB;
        return new ResultatSimulation(s, 7, 8, J, fin, tableaux,
                List.of(new ResultatSimulation.TableauIgnore("C", "2 joueur(s) pour des poules de 3")),
                69, attente, max, "Paul MARTIN", 0.734, true);
    }

    @Test
    void trois_choix_expliques_celui_du_tournoi_selectionne() {
        Tournoi t = new Tournoi("Open", "Club", LocalDate.of(2027, 4, 10));
        List<PresentationPrevisions.ChoixStrategie> choix = PresentationPrevisions.choix(t);
        assertEquals(3, choix.size());
        assertEquals(1, choix.stream().filter(PresentationPrevisions.ChoixStrategie::selectionnee).count());
        assertEquals(StrategieOrdonnancement.PAR_DEFAUT,
                choix.stream().filter(PresentationPrevisions.ChoixStrategie::selectionnee).findFirst().orElseThrow().strategie());
        assertTrue(choix.get(0).contrepartie().startsWith("En contrepartie : les tableaux"));
        assertEquals("Aucun choix pour l'instant : « Faire attendre le moins possible les joueurs » s'applique par défaut.",
                PresentationPrevisions.phraseStrategie(t));

        t.setStrategieOrdonnancement(StrategieOrdonnancement.AVANCER_TABLEAUX_COMMENCES);
        assertTrue(PresentationPrevisions.choix(t).get(0).selectionnee());
        assertEquals("Stratégie choisie : « Avancer les tableaux commencés tôt ».", PresentationPrevisions.phraseStrategie(t));
        assertTrue(PresentationPrevisions.messageChoixEnregistre(StrategieOrdonnancement.AVANCER_TABLEAUX_COMMENCES)
                .contains("Le jour J"));
    }

    @Test
    void reglages_tables_et_graine_lus_avec_erreurs_claires() {
        Tournoi t = new Tournoi("Open", "Club", LocalDate.of(2027, 4, 10));
        assertEquals(16, PresentationPrevisions.tablesProposees(t));
        t.setNombreDeTables(24);
        assertEquals(24, PresentationPrevisions.tablesProposees(t));
        assertEquals(12, PresentationPrevisions.lireTables(" 12 "));
        assertNull(PresentationPrevisions.lireTables("0"));
        assertNull(PresentationPrevisions.lireTables("201"));
        assertNull(PresentationPrevisions.lireTables("douze"));
        assertNull(PresentationPrevisions.lireTables(null));
        assertEquals(2027L, PresentationPrevisions.lireGraine("2027"));
        assertEquals(1L, PresentationPrevisions.lireGraine("  "));
        assertNull(PresentationPrevisions.lireGraine("abc"));
    }

    @Test
    void chiffres_cles_synthese_et_lignes_par_tableau() {
        ResultatSimulation r = resultat(StrategieOrdonnancement.PAR_DEFAUT, J.withHour(13).withMinute(35),
                J.withHour(18).withMinute(5), 22.4, 75);
        List<PresentationPrevisions.Chiffre> chiffres = PresentationPrevisions.chiffres(r);
        assertEquals(List.of("18 h 05", "22 min", "1 h 15", "73 %", "1"),
                chiffres.stream().map(PresentationPrevisions.Chiffre::valeur).toList());
        assertEquals("Tableau qui déborde", chiffres.get(4).libelle());
        assertTrue(chiffres.get(4).alerte());
        assertFalse(chiffres.get(0).alerte());

        String s = PresentationPrevisions.synthese(r);
        assertTrue(s.startsWith("69 parties simulées sur 8 tables (graine 7) ; c'est Paul MARTIN qui attend le plus : 1 h 15."), s);
        assertTrue(s.contains("Tableau « C » non simulé : 2 joueur(s) pour des poules de 3."));

        List<PresentationPrevisions.LigneTableau> lignes = PresentationPrevisions.lignes(r);
        assertEquals("A", lignes.get(0).nom());
        assertEquals("9 h 00", lignes.get(0).debut());
        assertEquals("13 h 35", lignes.get(0).fin());
        assertEquals("13 h 00", lignes.get(0).finSouhaitee());
        assertEquals("Déborde de 35 min", lignes.get(0).etat());
        assertEquals(StatutPastille.ERREUR, lignes.get(0).statut());
        assertEquals("À l'heure", lignes.get(1).etat());
        assertEquals(StatutPastille.SUCCES, lignes.get(1).statut());
        assertEquals("22 min", lignes.get(1).attente());
    }

    @Test
    void sur_plusieurs_jours_les_heures_portent_le_jour() {
        ResultatSimulation r = resultat(StrategieOrdonnancement.PAR_DEFAUT, J.withHour(12),
                J.plusDays(1).withHour(10).withMinute(7), 10, 30);
        assertEquals("11/04 10 h 07", PresentationPrevisions.chiffres(r).get(0).valeur());
        assertEquals("10/04 9 h 00", PresentationPrevisions.lignes(r).get(0).debut());
    }

    @Test
    void comparaison_indicateurs_meilleure_valeur_et_verdict() {
        ResultatSimulation a = resultat(StrategieOrdonnancement.FAIRE_AVANCER_PETIT_A_PETIT, J.withHour(16),
                J.withHour(18), 30, 90);
        ResultatSimulation b = resultat(StrategieOrdonnancement.AVANCER_TABLEAUX_COMMENCES, J.withHour(12).withMinute(30),
                J.withHour(18).withMinute(20), 25, 120);
        ComparaisonStrategies c = new ComparaisonStrategies(a, b);
        List<PresentationPrevisions.LigneComparaison> lignes = PresentationPrevisions.comparaison(c);
        assertEquals(List.of("Fin prévue", "Attente moyenne", "Attente la plus longue", "Occupation des tables",
                        "Tableaux qui débordent", "Fin de « A »", "Fin de « B »"),
                lignes.stream().map(PresentationPrevisions.LigneComparaison::indicateur).toList());
        assertEquals(1, lignes.get(0).meilleure(), "18 h 00 contre 18 h 20");
        assertEquals(2, lignes.get(1).meilleure(), "25 min contre 30 min");
        assertEquals(1, lignes.get(2).meilleure());
        assertEquals(0, lignes.get(3).meilleure());
        assertEquals(2, lignes.get(5).meilleure());
        assertEquals("12 h 30", lignes.get(5).seconde());
        assertEquals("« Tout faire avancer petit à petit » finit 20 min plus tôt ; avec « Avancer les tableaux "
                + "commencés tôt », les joueurs attendent en moyenne 5 min de moins par partie.",
                PresentationPrevisions.verdict(c));
        assertEquals("Choisissez deux stratégies différentes pour les comparer.",
                PresentationPrevisions.verdict(new ComparaisonStrategies(a, a)));
    }

    @Test
    void durees_lisibles() {
        assertEquals("0 min", PresentationPrevisions.minutes(-3));
        assertEquals("45 min", PresentationPrevisions.minutes(45));
        assertEquals("1 h", PresentationPrevisions.minutes(60));
        assertEquals("2 h 05", PresentationPrevisions.minutes(125));
    }
}
