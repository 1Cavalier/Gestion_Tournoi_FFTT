package com.pingmanager.core;

import com.pingmanager.core.ServicePreparationJourJ.Acces;
import com.pingmanager.core.ServicePreparationJourJ.Alerte;
import com.pingmanager.core.ServicePreparationJourJ.Gravite;
import com.pingmanager.core.ServicePreparationJourJ.TypeAlerte;
import com.pingmanager.model.AffectationJugeArbitre;
import com.pingmanager.model.ConditionsMaintien;
import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.GradeJugeArbitre;
import com.pingmanager.model.HorairesTableau;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.StatutTournoi;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** B051 : accès à l'étape « J-7 : préparer le jour J » et alertes de préparation. */
class ServicePreparationJourJTest {

    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");
    private static final LocalDate SAMEDI = LocalDate.of(2027, 4, 10);

    private static ServicePreparationJourJ a(LocalDate jour) {
        return new ServicePreparationJourJ(new ServiceTournoiEnCours(
                () -> ZonedDateTime.of(jour.atTime(9, 0), PARIS).toInstant(), PARIS));
    }

    private static Tournoi tournoi(StatutTournoi statut) {
        Tournoi t = new Tournoi("Open", "Brunoy", List.of(SAMEDI, SAMEDI.plusDays(1)));
        if (statut == StatutTournoi.HOMOLOGUE || statut == StatutTournoi.PUBLIE) {
            t.changerStatut(StatutTournoi.HOMOLOGUE);
        }
        if (statut == StatutTournoi.PUBLIE) {
            t.changerStatut(StatutTournoi.PUBLIE);
        }
        return t;
    }

    private static Tableau tableau(Tournoi t, String nom, int places, boolean avecHoraire) {
        Tableau tab = new Tableau(nom, ConfigurationTableau.builder().placesMax(places).build());
        if (avecHoraire) {
            tab.setHoraires(new HorairesTableau(SAMEDI.atTime(8, 0), SAMEDI.atTime(8, 40), SAMEDI.atTime(9, 0)));
        }
        t.ajouterTableau(tab);
        return tab;
    }

    private static Joueur joueur(int n, String club, int points) {
        return new Joueur("91" + n, "NOM" + n, "Prenom" + n, Sexe.HOMME, club, points);
    }

    private static void designerJugeArbitre(Tournoi t) {
        t.ajouterJugeArbitre(new AffectationJugeArbitre("ARBITRE", "Jeanne", "ja@exemple.invalid", "7700001",
                GradeJugeArbitre.JA_NATIONAL, null));
    }

    private static List<TypeAlerte> types(List<Alerte> alertes) {
        return alertes.stream().map(Alerte::type).toList();
    }

    // ------------------------------------------------------------------
    // Accès
    // ------------------------------------------------------------------

    @Test
    void etape_proposee_seulement_aux_statuts_homologue_et_publie() {
        ServicePreparationJourJ s = a(SAMEDI.minusDays(3));
        assertTrue(s.acces(tournoi(StatutTournoi.HOMOLOGUE), false).proposee());
        assertTrue(s.acces(tournoi(StatutTournoi.PUBLIE), false).proposee());
        Acces brouillon = s.acces(tournoi(StatutTournoi.BROUILLON), true);
        assertFalse(brouillon.proposee());
        assertFalse(brouillon.ouverte(), "même en démo, un brouillon n'a pas l'étape");
    }

    @Test
    void etape_ouverte_a_partir_de_j_moins_7_du_premier_jour() {
        Tournoi t = tournoi(StatutTournoi.PUBLIE);
        Acces jMoins8 = a(SAMEDI.minusDays(8)).acces(t, false);
        assertTrue(jMoins8.proposee());
        assertFalse(jMoins8.ouverte());
        assertEquals(SAMEDI.minusDays(ServiceTournoiEnCours.JOURS_AVANT), jMoins8.ouverture());
        assertEquals(8, jMoins8.joursAvantPremierJour());

        Acces jMoins7 = a(SAMEDI.minusDays(7)).acces(t, false);
        assertTrue(jMoins7.ouverte());
        assertEquals(7, jMoins7.joursAvantPremierJour());
        assertFalse(jMoins7.demo());

        assertTrue(a(SAMEDI).acces(t, false).ouverte(), "le premier jour");
        assertEquals(-1, a(SAMEDI.plusDays(1)).acces(t, false).joursAvantPremierJour(), "deuxième jour");
    }

    @Test
    void tournoi_de_demo_toujours_ouvert_meme_avant_j_moins_7() {
        Acces demo = a(SAMEDI.minusDays(120)).acces(tournoi(StatutTournoi.HOMOLOGUE), true);
        assertTrue(demo.ouverte());
        assertTrue(demo.demo(), "ouvert seulement parce que c'est la démo");
        assertFalse(a(SAMEDI.minusDays(2)).acces(tournoi(StatutTournoi.PUBLIE), true).demo(),
                "à partir de J-7, la démo suit la règle commune");
    }

    // ------------------------------------------------------------------
    // Alertes
    // ------------------------------------------------------------------

    @Test
    void tournoi_pret_aucune_alerte() {
        Tournoi t = tournoi(StatutTournoi.PUBLIE);
        designerJugeArbitre(t);
        Tableau a = tableau(t, "A", 8, true);
        a.inscrire(joueur(1, "TT Brunoy", 1200));
        assertEquals(List.of(), ServicePreparationJourJ.alertes(t));
    }

    @Test
    void juge_arbitre_principal_non_designe_et_tableau_sans_horaire() {
        Tournoi t = tournoi(StatutTournoi.PUBLIE);
        tableau(t, "A", 8, true);
        tableau(t, "B", 8, false);
        List<Alerte> alertes = ServicePreparationJourJ.alertes(t);
        assertEquals(List.of(TypeAlerte.JUGE_ARBITRE_NON_DESIGNE, TypeAlerte.TABLEAU_SANS_HORAIRE), types(alertes));
        assertEquals("B", alertes.get(1).tableau());
        assertEquals(Gravite.A_TRAITER, alertes.get(0).type().getGravite());
    }

    @Test
    void tableau_sous_le_seuil_seulement_si_l_organisateur_a_defini_un_minimum() {
        Tournoi t = tournoi(StatutTournoi.PUBLIE);
        designerJugeArbitre(t);
        Tableau a = tableau(t, "A", 16, true);
        a.inscrire(joueur(1, "TT Brunoy", 1200));
        a.inscrire(joueur(2, "TT Brunoy", 1100));
        assertEquals(List.of(), ServicePreparationJourJ.alertes(t), "pas de seuil défini : pas d'alerte");

        t.setConditionsMaintien(new ConditionsMaintien(4, null, null, 0, 0, 0));
        List<Alerte> alertes = ServicePreparationJourJ.alertes(t);
        assertEquals(List.of(TypeAlerte.TABLEAU_SOUS_SEUIL), types(alertes));
        assertEquals(2, alertes.get(0).nombre());
        assertEquals(4, alertes.get(0).seuil());

        a.inscrire(joueur(3, "TT Brunoy", 1000));
        a.inscrire(joueur(4, "TT Brunoy", 900));
        assertEquals(List.of(), ServicePreparationJourJ.alertes(t), "au seuil : maintenu");
    }

    @Test
    void tableau_complet_et_liste_d_attente_sont_a_savoir() {
        Tournoi t = tournoi(StatutTournoi.PUBLIE);
        designerJugeArbitre(t);
        Tableau a = new Tableau("A", ConfigurationTableau.builder().placesMax(2).tailleMaxListeAttente(4).build());
        a.setHoraires(new HorairesTableau(null, null, SAMEDI.atTime(9, 0)));
        t.ajouterTableau(a);
        a.inscrire(joueur(1, "TT Brunoy", 1200));
        a.inscrire(joueur(2, "TT Brunoy", 1100));
        a.inscrire(joueur(3, "TT Brunoy", 1000));
        List<Alerte> alertes = ServicePreparationJourJ.alertes(t);
        assertEquals(List.of(TypeAlerte.TABLEAU_COMPLET, TypeAlerte.LISTE_ATTENTE_NON_VIDE), types(alertes));
        assertEquals(2, alertes.get(0).nombre());
        assertEquals(2, alertes.get(0).seuil());
        assertEquals(1, alertes.get(1).nombre());
        assertEquals(Gravite.A_SAVOIR, alertes.get(0).type().getGravite());
        assertEquals(Gravite.A_SAVOIR, alertes.get(1).type().getGravite());
    }

    @Test
    void joueur_sans_club_ou_sans_points_une_alerte_par_joueur_avec_ses_tableaux() {
        Tournoi t = tournoi(StatutTournoi.PUBLIE);
        designerJugeArbitre(t);
        Tableau a = tableau(t, "A", 8, true);
        Tableau b = tableau(t, "B", 8, true);
        Joueur sansClub = joueur(1, " ", 1200);
        Joueur sansPoints = joueur(2, "TT Brunoy", 0);
        Joueur complet = joueur(3, "TT Brunoy", 500);
        a.inscrire(sansClub);
        b.inscrire(sansClub);
        a.inscrire(sansPoints);
        a.inscrire(complet);
        List<Alerte> alertes = ServicePreparationJourJ.alertes(t);
        assertEquals(List.of(TypeAlerte.JOUEUR_SANS_CLUB, TypeAlerte.JOUEUR_SANS_POINTS), types(alertes));
        assertEquals(sansClub, alertes.get(0).joueur());
        assertEquals(List.of("A", "B"), alertes.get(0).tableaux());
        assertEquals(sansPoints, alertes.get(1).joueur());
    }

    @Test
    void les_alertes_ne_modifient_pas_le_tournoi() {
        Tournoi t = tournoi(StatutTournoi.PUBLIE);
        Tableau a = tableau(t, "A", 8, false);
        a.inscrire(joueur(1, "", 0));
        ServicePreparationJourJ.alertes(t);
        assertEquals(1, a.getInscrits().size());
        assertEquals(com.pingmanager.model.StatutTableau.INSCRIPTION, a.getStatut());
        assertTrue(t.getJugesArbitres().isEmpty());
    }
}
