package com.pingmanager.core.feuilles;

import com.pingmanager.core.ServiceLancementTableau;
import com.pingmanager.core.ServicePhaseFinale;
import com.pingmanager.core.ServicePointage;
import com.pingmanager.core.feuilles.ArbreImprimable.CaseArbre;
import com.pingmanager.core.feuilles.ArbreImprimable.ColonneArbre;
import com.pingmanager.core.feuilles.ArbreImprimable.TypeCase;
import com.pingmanager.core.feuilles.FeuillePoule.PartiePoule;
import com.pingmanager.model.AdresseTournoi;
import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.IssueMatch;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Manche;
import com.pingmanager.model.Match;
import com.pingmanager.model.Poule;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.StatutMatch;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tour;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.format.FormatConfigurable;
import com.pingmanager.model.format.FormatTournoi;
import com.pingmanager.model.format.RegistreFormats;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Contenu des feuilles imprimées : feuille de match (format provisoire de
 * Quentin, grille 3 × 5), feuille de poule (I.301, grille, classement I.303 à
 * remplir) et tableau final imprimable.
 */
class GenerateurFeuillesTest {

    private final GenerateurFeuilles generateur = new GenerateurFeuilles();
    private final ServicePhaseFinale phaseFinale = new ServicePhaseFinale();

    private static Joueur j(String licence, int points) {
        return new Joueur(licence, "nom" + licence, "Prenom" + licence, Sexe.HOMME, "Club " + licence, points);
    }

    private static FormatTournoi format(int taille, int qualifies, boolean petiteFinale) {
        return FormatConfigurable.builder()
                .nom("P" + taille + "Q" + qualifies).identifiant("P" + taille + "Q" + qualifies + (petiteFinale ? "PF" : ""))
                .taillePouleSouhaitee(taille).qualifiesParPoule(qualifies)
                .nombreManchesGagnantes(3)
                .avecPetiteFinale(petiteFinale)
                .build();
    }

    /** Tournoi avec un tableau lancé (poules tirées avec la graine 1). */
    private static Tournoi tournoiLance(int nbJoueurs, ConfigurationTableau config, Joueur... autres) {
        Tournoi t = new Tournoi("Tournoi de Brunoy", "Club", LocalDate.of(2026, 11, 14));
        t.setAdresse(AdresseTournoi.de("Gymnase", "91800", "Brunoy"));
        Tableau tableau = new Tableau("C – Toutes séries", config);
        t.ajouterTableau(tableau);
        tableau.ouvrirPointage();
        List<Joueur> joueurs = new ArrayList<>(List.of(autres));
        for (int i = 1; joueurs.size() < nbJoueurs; i++) {
            joueurs.add(j("L" + i, 500 + 10 * i));
        }
        for (Joueur joueur : joueurs) {
            tableau.inscrire(joueur);
            tableau.pointer(joueur);
        }
        new ServicePointage(t).cloturerPointage(tableau);
        new ServiceLancementTableau().lancer(tableau, 1L);
        return t;
    }

    private static Tournoi tournoiLance(int nbJoueurs) {
        return tournoiLance(nbJoueurs, ConfigurationTableau.depuisFormat(RegistreFormats.formatParDefaut()).build());
    }

    private static List<Manche> victoire(Match m, boolean pourA) {
        List<Manche> r = new ArrayList<>();
        for (int i = 0; i < m.getNombreManchesGagnantes(); i++) {
            r.add(pourA ? new Manche(11, 6) : new Manche(6, 11));
        }
        return r;
    }

    private Tableau avecTableauFinal(Tournoi t) {
        Tableau tableau = t.getTableaux().get(0);
        for (Poule p : tableau.getPoules()) {
            for (Match m : p.getMatchs()) {
                if (!m.estBye()) {
                    m.definirScore(victoire(m, true));
                }
            }
        }
        assertTrue(phaseFinale.construire(tableau, 42L).estConstruit());
        return tableau;
    }

    // ------------------------------------------------------------------
    // Feuille de match
    // ------------------------------------------------------------------

    @Test
    void feuille_de_match_d_une_partie_de_poule_joueurs_face_a_face_et_grille_3_par_5() {
        Tournoi t = tournoiLance(9);
        Tableau tableau = t.getTableaux().get(0);
        Poule p = tableau.getPoules().get(0);
        Match m = p.getMatchs().get(0);
        m.setNumeroTable(4);

        ResultatFeuille<FeuilleMatch> r = generateur.feuilleMatch(t, tableau, m);

        assertTrue(r.estPrete(), r.message());
        FeuilleMatch f = r.feuille();
        assertEquals("Tournoi de Brunoy · 14/11/2026 · Brunoy", f.entete().ligneTournoi());
        assertEquals("C – Toutes séries", f.entete().tableau());
        assertEquals("Poule " + p.getNom() + " · tour 1", f.phase());
        assertEquals("Table 4", f.table());
        assertEquals("Au meilleur des 5 manches (3 manches gagnantes)", f.format());
        // Nom, prénom, club, points de chaque joueur face à son adversaire.
        Joueur a = m.getJoueurA();
        assertEquals(a.getNom().toUpperCase(), f.joueurA().nom());
        assertEquals(a.getPrenom(), f.joueurA().prenom());
        assertEquals(a.getClub(), f.joueurA().club());
        assertEquals(a.getPoints() + " pts", f.joueurA().points());
        assertEquals(m.getJoueurB().getNumeroLicence(), f.joueurB().licence());
        // Grille 3 × 5 : manches 1 à 5 en en-tête, autres cases vides.
        assertEquals(3, f.grille().size());
        assertEquals(List.of("Manche 1", "Manche 2", "Manche 3", "Manche 4", "Manche 5"), f.grille().get(0));
        assertEquals(List.of("", "", "", "", ""), f.grille().get(1));
        assertEquals(List.of("", "", "", "", ""), f.grille().get(2));
        assertEquals(List.of("", f.joueurA().nomComplet(), f.joueurB().nomComplet()), f.etiquettesLignes());
        assertEquals(List.of("Joueur A", "Joueur B", "Arbitre"), f.signatures());
    }

    @Test
    void feuille_de_match_sans_table_laisse_la_table_vide_et_se_retrouve_depuis_le_tournoi() {
        Tournoi t = tournoiLance(6);
        Match m = t.getTableaux().get(0).getPoules().get(0).getMatchs().get(2);

        ResultatFeuille<FeuilleMatch> r = generateur.feuilleMatch(t, m);

        assertTrue(r.estPrete());
        assertEquals("", r.feuille().table());
        assertEquals("C – Toutes séries", r.feuille().entete().tableau());
    }

    @Test
    void feuille_de_match_refusee_pour_un_match_hors_du_tournoi() {
        Tournoi t = tournoiLance(6);
        Match etranger = new Match(j("X1", 500), j("X2", 600));

        ResultatFeuille<FeuilleMatch> r = generateur.feuilleMatch(t, etranger);

        assertFalse(r.estPrete());
        assertEquals(MotifRefusFeuille.MATCH_INTROUVABLE, r.motif());
        assertFalse(r.message().isEmpty());
    }

    @Test
    void feuille_de_match_refusee_tant_que_les_joueurs_du_tableau_final_ne_sont_pas_connus() {
        Tournoi t = tournoiLance(9);
        Tableau tableau = avecTableauFinal(t);
        Match finale = ServicePhaseFinale.matchFinale(tableau.getPhaseFinale());

        ResultatFeuille<FeuilleMatch> r = generateur.feuilleMatch(t, tableau, finale);

        assertEquals(MotifRefusFeuille.JOUEURS_A_DETERMINER, r.motif());
    }

    @Test
    void feuille_de_match_refusee_pour_un_exempt() {
        Tournoi t = tournoiLance(6);
        Tableau tableau = t.getTableaux().get(0);
        Poule p = tableau.getPoules().get(0);
        Match bye = new Match(p.getJoueurs().get(0), null);
        p.ajouterMatch(bye);

        assertEquals(MotifRefusFeuille.MATCH_EXEMPT, generateur.feuilleMatch(t, tableau, bye).motif());
    }

    @Test
    void feuille_de_match_d_un_quart_de_finale_porte_le_nom_du_tour() {
        Tournoi t = tournoiLance(12);
        Tableau tableau = avecTableauFinal(t);
        Tour premier = tableau.getPhaseFinale().getPremierTour();
        Match m = premier.getMatchs().stream()
                .filter(x -> !x.getJoueurA().estADeterminer() && !x.getJoueurB().estADeterminer())
                .findFirst().orElseThrow();

        ResultatFeuille<FeuilleMatch> r = generateur.feuilleMatch(t, m);

        assertTrue(r.estPrete(), r.message());
        assertEquals(premier.getNom(), r.feuille().phase());
    }

    @Test
    void feuille_de_match_d_un_score_corrige_reste_vierge() {
        Tournoi t = tournoiLance(6);
        Tableau tableau = t.getTableaux().get(0);
        Match m = tableau.getPoules().get(0).getMatchs().get(0);
        m.definirScore(victoire(m, true));
        m.effacerResultat();
        m.definirScore(victoire(m, false));

        FeuilleMatch f = generateur.feuilleMatch(t, tableau, m).feuille();

        // La feuille sert à noter le score : elle est toujours imprimée vierge.
        assertEquals(List.of("", "", "", "", ""), f.grille().get(1));
    }

    @Test
    void joueur_engage_sur_deux_tableaux_a_une_feuille_par_tableau() {
        Joueur commun = j("COMMUN", 1500);
        Tournoi t = tournoiLance(6, ConfigurationTableau.depuisFormat(RegistreFormats.formatParDefaut()).build(), commun);
        Tableau second = new Tableau("D – ≤ 1599 pts", ConfigurationTableau.depuisFormat(RegistreFormats.formatParDefaut()).build());
        t.ajouterTableau(second);
        second.ouvrirPointage();
        second.inscrire(commun);
        second.pointer(commun);
        for (int i = 1; i <= 5; i++) {
            Joueur x = j("D" + i, 700 + i);
            second.inscrire(x);
            second.pointer(x);
        }
        new ServicePointage(t).cloturerPointage(second);
        new ServiceLancementTableau().lancer(second, 2L);

        Match dansSecond = second.getPoules().stream().flatMap(p -> p.getMatchs().stream())
                .filter(m -> commun.equals(m.getJoueurA()) || commun.equals(m.getJoueurB()))
                .findFirst().orElseThrow();
        ResultatFeuille<FeuilleMatch> r = generateur.feuilleMatch(t, dansSecond);

        assertTrue(r.estPrete());
        assertEquals("D – ≤ 1599 pts", r.feuille().entete().tableau());
    }

    @Test
    void feuilles_de_match_d_une_poule_dans_l_ordre_I301_sans_les_exempts() {
        Tournoi t = tournoiLance(6);
        Tableau tableau = t.getTableaux().get(0);
        Poule p = tableau.getPoules().get(0);
        p.ajouterMatch(new Match(p.getJoueurs().get(0), null));

        List<FeuilleMatch> feuilles = generateur.feuillesMatchsPoule(t, tableau, p);

        assertEquals(p.getMatchs().size() - 1, feuilles.size());
        assertEquals(JoueurFeuille.de(p.getMatchs().get(0).getJoueurA()), feuilles.get(0).joueurA());
    }

    @Test
    void format_au_meilleur_des_trois_manches() {
        assertEquals("Au meilleur des 3 manches (2 manches gagnantes)", GenerateurFeuilles.format(2));
        assertEquals("Au meilleur des 1 manches (1 manche gagnante)", GenerateurFeuilles.format(1));
        assertEquals("", GenerateurFeuilles.format(0));
    }

    @Test
    void grille_des_manches_refuse_une_forme_autre_que_3_par_5() {
        assertThrows(IllegalArgumentException.class, () -> GrilleManches.copie(List.of(List.of("a"))));
        assertThrows(IllegalArgumentException.class,
                () -> GrilleManches.copie(List.of(List.of("", "", "", "", ""), List.of("", "", "", "", ""),
                        List.of("", "", "", ""))));
    }

    // ------------------------------------------------------------------
    // Feuille de poule
    // ------------------------------------------------------------------

    @Test
    void feuille_de_poule_de_trois_ordre_I301_grille_et_classement_a_remplir() {
        // Poules de 3, 2 qualifiés : I.301.2 donne 1-3, 1-2, 2-3.
        Tournoi t = tournoiLance(6, ConfigurationTableau.depuisFormat(format(3, 2, false)).build());
        Tableau tableau = t.getTableaux().get(0);
        Poule p = tableau.getPoules().get(0);

        ResultatFeuille<FeuillePoule> r = generateur.feuillePoule(t, tableau, p);

        assertTrue(r.estPrete(), r.message());
        FeuillePoule f = r.feuille();
        assertEquals("Poule " + p.getNom(), f.poule());
        assertEquals(2, f.qualifies());
        assertEquals(3, f.joueurs().size());
        assertEquals(JoueurFeuille.de(p.getJoueurs().get(0)), f.joueurs().get(0));
        List<String> ordre = f.parties().stream().map(PartiePoule::libellePositions).toList();
        assertEquals(List.of("1 – 3", "1 – 2", "2 – 3"), ordre);
        assertEquals(List.of(1, 2, 3), f.parties().stream().map(PartiePoule::tour).toList());
        // Grille 4 × 4 : en-têtes, diagonale barrée, cases vides.
        assertEquals(List.of("", "1", "2", "3"), f.grilleResultats().get(0));
        assertEquals(List.of("2", "", "X", ""), f.grilleResultats().get(2));
        assertEquals(3, f.lignesClassement());
        assertTrue(f.colonnesClassement().contains("Points"));
        assertTrue(f.noteClassement().startsWith("I.303"));
        // Feuille de rencontre : chaque partie a sa grille 3 × 5.
        for (PartiePoule partie : f.parties()) {
            assertEquals("Manche 1", partie.grille().get(0).get(0));
            assertFalse(partie.exempt());
        }
    }

    @Test
    void feuille_de_poule_de_quatre_un_qualifie_suit_I301_3() {
        // I.301.3 : (1-4, 2-3) (1-3, 2-4) (1-2, 3-4).
        Tournoi t = tournoiLance(8, ConfigurationTableau.depuisFormat(format(4, 1, false)).build());
        Tableau tableau = t.getTableaux().get(0);

        FeuillePoule f = generateur.feuillePoule(t, tableau, tableau.getPoules().get(0)).feuille();

        assertEquals(List.of("1 – 4", "2 – 3", "1 – 3", "2 – 4", "1 – 2", "3 – 4"),
                f.parties().stream().map(PartiePoule::libellePositions).toList());
        assertEquals(List.of(1, 1, 2, 2, 3, 3), f.parties().stream().map(PartiePoule::tour).toList());
    }

    @Test
    void feuille_de_poule_effectif_non_rond_poules_de_tailles_differentes() {
        // 10 joueurs en poules de 3 : des poules de 3 et une de 4 (ou l'inverse).
        Tournoi t = tournoiLance(10, ConfigurationTableau.depuisFormat(format(3, 2, false)).build());
        Tableau tableau = t.getTableaux().get(0);
        for (Poule p : tableau.getPoules()) {
            FeuillePoule f = generateur.feuillePoule(t, tableau, p).feuille();
            int n = p.getTaille();
            assertEquals(n + 1, f.grilleResultats().size());
            assertEquals(n * (n - 1) / 2, f.parties().size());
        }
    }

    @Test
    void feuille_de_poule_refusee_sans_parties() {
        Tournoi t = tournoiLance(6);
        Tableau tableau = t.getTableaux().get(0);
        Poule vide = new Poule("Z", List.of(j("Z1", 500), j("Z2", 600)));

        assertEquals(MotifRefusFeuille.POULE_SANS_PARTIE, generateur.feuillePoule(t, tableau, vide).motif());
    }

    @Test
    void feuille_de_poule_imprime_une_partie_exempte_sans_adversaire() {
        Tournoi t = tournoiLance(6);
        Tableau tableau = t.getTableaux().get(0);
        Poule p = tableau.getPoules().get(0);
        p.ajouterMatch(new Match(p.getJoueurs().get(1), null));

        FeuillePoule f = generateur.feuillePoule(t, tableau, p).feuille();
        PartiePoule derniere = f.parties().get(f.parties().size() - 1);

        assertTrue(derniere.exempt());
        assertEquals("2 – exempt", derniere.libellePositions());
        assertTrue(derniere.joueurB().estADeterminer());
    }

    // ------------------------------------------------------------------
    // Tableau final
    // ------------------------------------------------------------------

    @Test
    void arbre_refuse_tant_que_le_tableau_final_n_est_pas_construit() {
        Tournoi t = tournoiLance(9);

        assertEquals(MotifRefusFeuille.PAS_DE_TABLEAU_FINAL,
                generateur.arbre(t, t.getTableaux().get(0)).motif());
    }

    @Test
    void arbre_effectif_non_rond_colonnes_alignees_avec_exempts() {
        // 9 joueurs en poules de 3, 2 qualifiés : 6 qualifiés dans un tableau de 8, 2 exempts.
        Tournoi t = tournoiLance(9);
        Tableau tableau = avecTableauFinal(t);

        ArbreImprimable a = generateur.arbre(t, tableau).feuille();

        assertEquals(tableau.getPhaseFinale().getCadre() / 2, a.casesPremierTour());
        List<ColonneArbre> cols = a.colonnes();
        for (int i = 1; i < cols.size(); i++) {
            assertEquals(cols.get(i - 1).cases().size(), 2 * cols.get(i).cases().size());
        }
        assertEquals("Finale", cols.get(cols.size() - 1).libelle());
        long exempts = cols.get(0).cases().stream().filter(c -> c.type() == TypeCase.EXEMPT).count();
        assertEquals(tableau.getPhaseFinale().getCadre() - tableau.getPhaseFinale().getNombreJoueurs(), exempts);
        assertTrue(a.resume().contains("tableau de " + tableau.getPhaseFinale().getCadre()));
        assertNull(a.petiteFinale());
    }

    @Test
    void arbre_imprime_scores_vainqueur_table_et_abandon() {
        Tournoi t = tournoiLance(16);
        Tableau tableau = avecTableauFinal(t);
        List<Match> premiers = tableau.getPhaseFinale().getPremierTour().getMatchs();
        Match joue = premiers.get(0);
        assertTrue(phaseFinale.saisirScore(tableau, joue, victoire(joue, false)).estValide());
        Match abandon = premiers.get(1);
        assertTrue(phaseFinale.declarerIssue(tableau, abandon, IssueMatch.ABANDON, abandon.getJoueurA()).estValide());
        Match enCours = premiers.get(2);
        enCours.setNumeroTable(7);
        enCours.setStatut(StatutMatch.EN_COURS);

        List<CaseArbre> cases = generateur.arbre(t, tableau).feuille().colonnes().get(0).cases();

        CaseArbre c0 = trouver(cases, joue);
        assertEquals("0", c0.scoreA());
        assertEquals("3", c0.scoreB());
        assertTrue(c0.vainqueurB());
        assertFalse(c0.vainqueurA());
        CaseArbre c1 = trouver(cases, abandon);
        assertEquals("Abandon", c1.mention());
        assertTrue(c1.vainqueurB());
        assertEquals("", c1.scoreA());
        assertEquals("Table 7", trouver(cases, enCours).mention());
        // Le vainqueur apparaît au tour suivant.
        JoueurFeuille gagnant = JoueurFeuille.de(joue.getJoueurB());
        assertTrue(generateur.arbre(t, tableau).feuille().colonnes().get(1).cases().stream()
                .anyMatch(c -> gagnant.equals(c.joueurA()) || gagnant.equals(c.joueurB())));
    }

    @Test
    void arbre_avec_petite_finale() {
        Tournoi t = tournoiLance(12,
                ConfigurationTableau.depuisFormat(format(3, 2, true)).build());
        Tableau tableau = avecTableauFinal(t);

        ArbreImprimable a = generateur.arbre(t, tableau).feuille();

        assertNotNull(a.petiteFinale());
        assertEquals(TypeCase.MATCH, a.petiteFinale().type());
        assertTrue(a.petiteFinale().joueurA().estADeterminer());
        assertEquals(JoueurFeuille.A_DETERMINER, a.petiteFinale().joueurA().nomComplet());
    }

    @Test
    void en_tete_sans_date_ni_adresse() {
        Tournoi t = new Tournoi("Sans date", "Club", LocalDate.of(2026, 11, 14));
        EnTeteFeuille e = GenerateurFeuilles.entete(t, new Tableau("A", ConfigurationTableau.builder().build()));
        assertEquals("Sans date", e.tournoi());
        assertEquals("", e.lieu());
    }

    private static CaseArbre trouver(List<CaseArbre> cases, Match m) {
        JoueurFeuille a = JoueurFeuille.de(m.getJoueurA());
        return cases.stream().filter(c -> c.type() == TypeCase.MATCH && c.joueurA().equals(a))
                .findFirst().orElseThrow();
    }
}
