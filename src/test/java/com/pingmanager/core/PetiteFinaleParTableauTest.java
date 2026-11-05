package com.pingmanager.core;

import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.IssueMatch;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Manche;
import com.pingmanager.model.Match;
import com.pingmanager.model.Poule;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.StatutMatch;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.TableauElimination;
import com.pingmanager.model.Tour;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.format.FormatConfigurable;
import com.pingmanager.model.format.FormatTournoi;
import com.pingmanager.model.format.RegistreFormats;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Petite finale réglée <b>par tableau</b> (option de la configuration du
 * tableau, décision de Quentin du 5 novembre 2026, QUESTIONS.md) : construction
 * du tableau final, podium, graine du second tirage (IX.109).
 *
 * <p>Le chapitre IX des règlements sportifs ne dit rien de la 3e place d'un
 * tournoi : c'est une clause d'organisateur. Sans petite finale, les deux
 * demi-finalistes battus sont 3es ex aequo (choix A maintenu).</p>
 */
class PetiteFinaleParTableauTest {

    private final ServicePhaseFinale service = new ServicePhaseFinale();

    private static Joueur j(String licence, int points, String club) {
        return new Joueur(licence, "N" + licence, "P" + licence, Sexe.HOMME, club, points);
    }

    private static ConfigurationTableau config(boolean petiteFinale) {
        return ConfigurationTableau.depuisFormat(RegistreFormats.formatParDefaut()).petiteFinale(petiteFinale).build();
    }

    private static Tableau tableauPoulesJouees(int nbJoueurs, ConfigurationTableau config) {
        Tableau tableau = new Tableau("A", config);
        Tournoi t = new Tournoi("Open", "Club", LocalDate.of(2027, 4, 10));
        t.ajouterTableau(tableau);
        tableau.ouvrirPointage();
        for (int i = 1; i <= nbJoueurs; i++) {
            Joueur joueur = j("J" + i, 500 + 10 * i, "Club" + (i % 5));
            tableau.inscrire(joueur);
            tableau.pointer(joueur);
        }
        new ServicePointage(t).cloturerPointage(tableau);
        new ServiceLancementTableau().lancer(tableau, 7L);
        for (Poule p : tableau.getPoules()) {
            for (Match m : p.getMatchs()) {
                m.definirScore(List.of(new Manche(11, 4), new Manche(11, 4), new Manche(11, 4)));
            }
        }
        return tableau;
    }

    private static List<Manche> victoire(Match m, boolean pourA) {
        List<Manche> r = new ArrayList<>();
        for (int i = 0; i < m.getNombreManchesGagnantes(); i++) {
            r.add(pourA ? new Manche(11, 6) : new Manche(6, 11));
        }
        return r;
    }

    private void jouerTout(Tableau tableau) {
        for (Tour tour : tableau.getPhaseFinale().getTours()) {
            for (Match m : tour.getMatchs()) {
                assertTrue(service.saisirScore(tableau, m, victoire(m, true)).estValide(), m.toString());
            }
        }
    }

    // ------------------------------------------------------------------
    // Configuration du tableau
    // ------------------------------------------------------------------

    @Test
    void par_defaut_le_format_standard_n_a_pas_de_petite_finale() {
        assertFalse(ConfigurationTableau.depuisFormat(RegistreFormats.formatParDefaut()).build().avecPetiteFinale());
        assertFalse(ConfigurationTableau.builder().build().avecPetiteFinale());
    }

    @Test
    void depuis_un_format_avec_petite_finale_l_option_est_reprise_et_reste_modifiable() {
        FormatTournoi f = FormatConfigurable.builder().nom("Avec PF").identifiant("PF")
                .taillePouleSouhaitee(3).qualifiesParPoule(2).avecPetiteFinale(true).build();
        assertTrue(ConfigurationTableau.depuisFormat(f).build().avecPetiteFinale());
        assertFalse(ConfigurationTableau.depuisFormat(f).petiteFinale(false).build().avecPetiteFinale());
    }

    // ------------------------------------------------------------------
    // Construction du tableau final
    // ------------------------------------------------------------------

    @Test
    void option_du_tableau_ajoute_la_petite_finale_au_format_standard() {
        Tableau tableau = tableauPoulesJouees(12, config(true));
        assertTrue(service.construire(tableau, 42L).estConstruit());
        TableauElimination elim = tableau.getPhaseFinale();

        Tour pf = ServicePhaseFinale.petiteFinale(elim);
        assertNotNull(pf, "petite finale ajoutée");
        assertEquals(1, pf.getMatchs().size());
        Match petite = pf.getMatchs().get(0);
        List<Tour> principaux = ServicePhaseFinale.toursPrincipaux(elim);
        Tour demies = principaux.get(principaux.size() - 2);
        assertSame(demies.getMatchs().get(0), petite.getSourceA());
        assertSame(demies.getMatchs().get(1), petite.getSourceB());
        assertEquals(StatutMatch.EN_ATTENTE, petite.getStatut());
        assertEquals(RegistreFormats.formatParDefaut().nombreManchesGagnantesPetiteFinale(),
                petite.getNombreManchesGagnantes());
    }

    @Test
    void sans_l_option_pas_de_petite_finale_meme_avec_le_meme_tirage() {
        Tableau tableau = tableauPoulesJouees(12, config(false));
        assertTrue(service.construire(tableau, 42L).estConstruit());
        assertNull(ServicePhaseFinale.petiteFinale(tableau.getPhaseFinale()));
    }

    @Test
    void l_option_du_tableau_prime_sur_le_format() {
        FormatTournoi f = FormatConfigurable.builder().nom("Avec PF").identifiant("PF")
                .taillePouleSouhaitee(3).qualifiesParPoule(2).avecPetiteFinale(true).build();
        Tableau tableau = tableauPoulesJouees(12, ConfigurationTableau.depuisFormat(f).petiteFinale(false).build());
        assertTrue(service.construire(tableau, 42L).estConstruit());
        assertNull(ServicePhaseFinale.petiteFinale(tableau.getPhaseFinale()));
    }

    @Test
    void effectif_non_rond_avec_exempts_petite_finale_jouable_jusqu_au_bout() {
        // 9 joueurs : 3 poules de 3, 6 qualifiés dans un tableau de 8 (2 exempts).
        Tableau tableau = tableauPoulesJouees(9, config(true));
        assertTrue(service.construire(tableau, 3L).estConstruit());
        TableauElimination elim = tableau.getPhaseFinale();
        assertEquals(6, elim.getNombreJoueurs());
        assertEquals(8, elim.getCadre());
        assertNotNull(ServicePhaseFinale.petiteFinale(elim));

        jouerTout(tableau);

        PodiumPhaseFinale p = ServicePhaseFinale.podium(elim);
        assertNotNull(p.premier());
        assertNotNull(p.deuxieme());
        assertEquals(1, p.troisiemes().size());
        assertNotNull(p.quatrieme());
        Set<Joueur> distincts = new HashSet<>(List.of(p.premier(), p.deuxieme(), p.troisiemes().get(0), p.quatrieme()));
        assertEquals(4, distincts.size(), "quatre joueurs différents sur le podium");
        assertTrue(ServicePhaseFinale.estTerminee(elim));
    }

    @Test
    void quatre_qualifies_demi_finales_directes_et_petite_finale() {
        Tableau tableau = tableauPoulesJouees(6, config(true));
        assertTrue(service.construire(tableau, 5L).estConstruit());
        TableauElimination elim = tableau.getPhaseFinale();
        assertEquals(2, ServicePhaseFinale.toursPrincipaux(elim).size(), "demi-finales puis finale");
        assertNotNull(ServicePhaseFinale.petiteFinale(elim));
    }

    @Test
    void deux_qualifies_seulement_pas_de_petite_finale_possible() {
        // Une seule poule de 3 : 2 qualifiés, une finale directe, pas de demi-finales.
        Tableau tableau = tableauPoulesJouees(3, config(true));
        assertTrue(service.construire(tableau, 5L).estConstruit());
        TableauElimination elim = tableau.getPhaseFinale();
        assertNull(ServicePhaseFinale.petiteFinale(elim));
        jouerTout(tableau);
        PodiumPhaseFinale p = ServicePhaseFinale.podium(elim);
        assertNotNull(p.premier());
        assertTrue(p.troisiemes().isEmpty());
        assertFalse(p.avecPetiteFinale());
    }

    // ------------------------------------------------------------------
    // Podium
    // ------------------------------------------------------------------

    @Test
    void podium_avec_petite_finale_un_troisieme_seul_et_un_quatrieme() {
        Tableau tableau = tableauPoulesJouees(12, config(true));
        service.construire(tableau, 42L);
        TableauElimination elim = tableau.getPhaseFinale();
        jouerTout(tableau);
        Match pf = ServicePhaseFinale.petiteFinale(elim).getMatchs().get(0);
        PodiumPhaseFinale p = ServicePhaseFinale.podium(elim);
        assertTrue(p.avecPetiteFinale());
        assertEquals(List.of(pf.getVainqueur()), p.troisiemes());
        assertEquals(pf.getPerdant(), p.quatrieme());
        assertFalse(p.troisiemesExAequo());
    }

    @Test
    void podium_sans_petite_finale_deux_troisiemes_ex_aequo() {
        Tableau tableau = tableauPoulesJouees(12, config(false));
        service.construire(tableau, 42L);
        TableauElimination elim = tableau.getPhaseFinale();
        jouerTout(tableau);
        PodiumPhaseFinale p = ServicePhaseFinale.podium(elim);
        assertFalse(p.avecPetiteFinale());
        assertEquals(2, p.troisiemes().size());
        assertTrue(p.troisiemesExAequo());
        assertNull(p.quatrieme());
    }

    @Test
    void petite_finale_attend_ses_deux_joueurs_avant_d_etre_jouable() {
        Tableau tableau = tableauPoulesJouees(12, config(true));
        service.construire(tableau, 42L);
        TableauElimination elim = tableau.getPhaseFinale();
        List<Tour> principaux = ServicePhaseFinale.toursPrincipaux(elim);
        for (int i = 0; i < principaux.size() - 2; i++) {
            for (Match m : principaux.get(i).getMatchs()) {
                service.saisirScore(tableau, m, victoire(m, true));
            }
        }
        Match pf = ServicePhaseFinale.petiteFinale(elim).getMatchs().get(0);
        Tour demies = principaux.get(principaux.size() - 2);
        Match demi1 = demies.getMatchs().get(0);
        service.saisirScore(tableau, demi1, victoire(demi1, true));
        assertEquals(demi1.getPerdant(), pf.getJoueurA());
        assertTrue(pf.getJoueurB().estADeterminer());
        assertEquals(ResultatSaisieScore.Motif.JOUEURS_NON_CONNUS,
                service.saisirScore(tableau, pf, victoire(pf, true)).getMotif());

        Match demi2 = demies.getMatchs().get(1);
        service.declarerIssue(tableau, demi2, IssueMatch.FORFAIT, demi2.getJoueurB());
        // Choix provisoire (QUESTIONS.md) : le forfait de la demie va en petite finale ;
        // le juge-arbitre y déclare au besoin un nouveau forfait.
        assertEquals(demi2.getJoueurB(), pf.getJoueurB());
        assertEquals(StatutMatch.A_JOUER, pf.getStatut());
    }

    // ------------------------------------------------------------------
    // Graine du second tirage (IX.109)
    // ------------------------------------------------------------------

    @Test
    void la_graine_du_tableau_final_est_conservee_avec_le_tableau() {
        Tableau tableau = tableauPoulesJouees(12, config(true));
        assertNull(tableau.getGraineTableauFinal());
        assertEquals(Long.valueOf(7L), tableau.getGrainePoules());
        ResultatConstructionPhaseFinale r = service.construire(tableau, 987654L);
        assertEquals(987654L, r.getGraine());
        assertEquals(Long.valueOf(987654L), tableau.getGraineTableauFinal());
        assertEquals(Long.valueOf(7L), tableau.getGrainePoules(), "la graine des poules ne change pas");
    }

    @Test
    void construction_refusee_ne_pose_pas_de_graine() {
        Tableau tableau = tableauPoulesJouees(12, config(true));
        tableau.getPoules().get(0).getMatchs().get(0).effacerResultat();
        assertFalse(service.construire(tableau, 1L).estConstruit());
        assertNull(tableau.getGraineTableauFinal());
    }

    @Test
    void meme_graine_meme_tableau_final_avec_petite_finale() {
        Tableau a = tableauPoulesJouees(12, config(true));
        Tableau b = tableauPoulesJouees(12, config(true));
        service.construire(a, 2026L);
        service.construire(b, 2026L);
        List<Tour> ta = a.getPhaseFinale().getTours();
        List<Tour> tb = b.getPhaseFinale().getTours();
        assertEquals(ta.size(), tb.size());
        for (int i = 0; i < ta.size(); i++) {
            assertEquals(ta.get(i).getNom(), tb.get(i).getNom());
            for (int k = 0; k < ta.get(i).getMatchs().size(); k++) {
                assertEquals(ta.get(i).getMatchs().get(k).getJoueurA(), tb.get(i).getMatchs().get(k).getJoueurA());
                assertEquals(ta.get(i).getMatchs().get(k).getJoueurB(), tb.get(i).getMatchs().get(k).getJoueurB());
            }
        }
    }
}
