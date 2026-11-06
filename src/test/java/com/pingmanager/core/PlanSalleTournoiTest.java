package com.pingmanager.core;

import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.ElementPlan.ReperePlan;
import com.pingmanager.model.ElementPlan.TablePlan;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Match;
import com.pingmanager.model.PlanSalle;
import com.pingmanager.model.ResultatPlacementPlan;
import com.pingmanager.model.SalleTournoi;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.TypeRepere;
import com.pingmanager.model.format.RegistreFormats;
import com.pingmanager.ordonnancement.ReservationsTables;
import com.pingmanager.ordonnancement.ResultatAffectationTable;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Plans types, salles d'un tournoi, brouillon de l'éditeur et réservations
 * respectées par la salle du jour J (B024).
 */
class PlanSalleTournoiTest {

    private final Horloge horloge = () -> Instant.parse("2026-11-14T09:00:00Z");
    private final ServicePlanSalle service = new ServicePlanSalle();

    private static Joueur j(String licence) {
        return new Joueur(licence, "N" + licence, "P" + licence, Sexe.HOMME, "Club" + licence, 1000);
    }

    private static Tableau lancer(Tournoi t, String nom, List<Joueur> joueurs) {
        Tableau tab = new Tableau(nom, ConfigurationTableau.depuisFormat(RegistreFormats.formatParDefaut()).build());
        t.ajouterTableau(tab);
        tab.ouvrirPointage();
        for (Joueur joueur : joueurs) {
            tab.inscrire(joueur);
            tab.pointer(joueur);
        }
        new ServicePointage(t).cloturerPointage(tab);
        new ServiceLancementTableau(t).lancer(tab, 1L);
        return tab;
    }

    private static Tournoi tournoi(int tables) {
        Tournoi t = new Tournoi("Open", "Club", LocalDate.of(2026, 11, 14));
        t.setNombreDeTables(tables);
        return t;
    }

    private static List<Integer> numeros(PlanSalle p) {
        return p.getTables().stream().map(TablePlan::numero).toList();
    }

    // --- Plans types -----------------------------------------------------------

    @Test
    void plan_type_deux_rangees_separees_par_une_allee_avec_arbitrage_et_entree() {
        PlanSalle p = PlansTypesSalle.generer(PlansTypesSalle.PlanType.DEUX_RANGEES, 8);
        assertEquals(List.of(1, 2, 3, 4, 5, 6, 7, 8), numeros(p));
        assertEquals(13, p.getColonnes(), "4 tables par rangée, une colonne entre deux tables");
        assertEquals(8, p.getLignes());
        assertEquals(new TablePlan(1, 1, 2), p.table(1).orElseThrow());
        assertEquals(new TablePlan(5, 1, 5), p.table(5).orElseThrow(), "deuxième rangée sous une allée");
        assertTrue(p.elementEn(1, 4).orElseThrow() instanceof com.pingmanager.model.ElementPlan.AlleePlan);
        assertEquals(List.of(TypeRepere.TABLE_ARBITRAGE, TypeRepere.ENTREE),
                p.getReperes().stream().map(ReperePlan::type).toList());
    }

    @Test
    void plan_type_trois_rangees_inegales_et_numerotation_a_partir_d_un_numero() {
        PlanSalle p = PlansTypesSalle.generer(3, 7, 11);
        assertEquals(List.of(11, 12, 13, 14, 15, 16, 17), numeros(p));
        assertEquals(3, p.getTables().stream().map(TablePlan::ligne).distinct().count());
        assertThrows(IllegalArgumentException.class, () -> PlansTypesSalle.generer(0, 4, 1));
        assertThrows(IllegalArgumentException.class, () -> PlansTypesSalle.generer(1, 60, 1),
                "60 tables sur une rangée ne tiennent pas");
        assertTrue(PlansTypesSalle.generer(4, 0, 1).getTables().isEmpty());
        assertEquals(1, PlansTypesSalle.generer(4, 1, 1).getTables().size(), "pas plus de rangées que de tables");
    }

    @Test
    void un_tournoi_sans_plan_a_une_seule_salle_avec_ses_tables_en_grille() {
        Tournoi t = tournoi(20);
        List<SalleTournoi> salles = service.salles(t);
        assertEquals(1, salles.size());
        assertEquals("Salle", salles.get(0).getNom());
        assertEquals(20, salles.get(0).getPlan().getTables().size());
        assertEquals(3, salles.get(0).getPlan().getTables().stream().map(TablePlan::ligne).distinct().count());
        assertTrue(service.verifier(t).estCoherent());
        assertFalse(service.verifier(t).planPrepare());
        assertSame(ReservationsTables.AUCUNE, service.reservations(t));
        assertEquals(1, PlansTypesSalle.grilleParDefaut(4).getTables().stream().map(TablePlan::ligne).distinct().count());
    }

    @Test
    void le_bilan_signale_les_tables_oubliees_en_trop_et_les_tableaux_inconnus() {
        Tournoi t = tournoi(4);
        t.ajouterTableau(new Tableau("Dames", ConfigurationTableau.depuisFormat(RegistreFormats.formatParDefaut()).build()));
        PlanSalle p = PlansTypesSalle.generer(1, 3, 2).reserver(3, "Vétérans").getPlan();
        t.setSalles(List.of(new SalleTournoi("Gymnase", 0, p, Set.of("Dames"), Set.of("Open"))));
        ServicePlanSalle.BilanPlanSalles bilan = service.verifier(t);
        assertEquals(Set.of(1), bilan.tablesHorsPlan());
        assertTrue(bilan.tablesEnTrop().isEmpty());
        assertEquals(Set.of("Open", "Vétérans"), bilan.tableauxInconnus());
        assertFalse(bilan.estCoherent());
        t.setNombreDeTables(3);
        assertEquals(Set.of(4), service.verifier(t).tablesEnTrop());
    }

    // --- Réservations tirées du plan ----------------------------------------------

    @Test
    void reservations_d_une_table_d_une_salle_reservee_et_d_une_salle_preferee() {
        Tournoi t = tournoi(6);
        PlanSalle gymnase = PlansTypesSalle.generer(1, 4, 1).reserver(4, "Open").getPlan();
        PlanSalle dojo = PlansTypesSalle.generer(1, 2, 5);
        t.setSalles(List.of(new SalleTournoi("Gymnase", 0, gymnase, Set.of(), Set.of("Dames")),
                new SalleTournoi("Dojo", 1, dojo, Set.of("Benjamins"), Set.of())));
        ReservationsTables r = service.reservations(t);
        assertFalse(r.estRestreinte(1));
        assertEquals(Set.of("Open"), r.tableauxAdmis(4));
        assertEquals(Set.of("Benjamins"), r.tableauxAdmis(5));
        assertTrue(r.prefere("Dames", 1));
        assertFalse(r.prefere("Dames", 4), "une table réservée à un autre tableau n'est pas préférée");
        assertTrue(r.prefere("Benjamins", 6));
        assertEquals("Dojo", service.salleDeLaTable(t, 6).getNom());
        assertNull(service.salleDeLaTable(t, 9));
    }

    @Test
    void la_salle_du_jour_j_respecte_la_table_reservee_pour_proposer_et_pour_l_envoi_manuel() {
        Tournoi t = tournoi(2);
        Tableau a = lancer(t, "A", List.of(j("A1"), j("A2"), j("A3"), j("A4"), j("A5"), j("A6")));
        t.setSalles(List.of(new SalleTournoi("Salle", PlansTypesSalle.generer(1, 2, 1).reserver(1, "B").getPlan())));
        ServiceSalle salle = new ServiceSalle(horloge);

        Match m = a.getPoules().get(0).getMatchs().get(0);
        ResultatAffectationTable refus = salle.affecter(t, m, 1);
        assertEquals(ResultatAffectationTable.Motif.TABLE_RESERVEE, refus.getMotif());
        assertEquals(Set.of("B"), refus.getTableauxAdmis());

        ResultatPropositionSalle r = salle.proposer(t);
        assertEquals(ResultatPropositionSalle.Motif.LANCES, r.getMotif());
        assertEquals(List.of(2), r.getLancements().stream().map(ResultatPropositionSalle.Lancement::table).toList());
        assertEquals(ResultatPropositionSalle.Motif.TABLES_LIBRES_RESERVEES, salle.proposer(t).getMotif(),
                "la table 1 reste libre pour le tableau B");
    }

    @Test
    void la_preference_envoie_le_tableau_dans_sa_salle() {
        Tournoi t = tournoi(4);
        lancer(t, "A", List.of(j("A1"), j("A2"), j("A3")));
        t.setSalles(List.of(new SalleTournoi("Gymnase", PlansTypesSalle.generer(1, 2, 1)),
                new SalleTournoi("Dojo", 0, PlansTypesSalle.generer(1, 2, 3), Set.of(), Set.of("A"))));
        ResultatPropositionSalle r = new ServiceSalle(horloge).proposer(t);
        assertEquals(List.of(3), r.getLancements().stream().map(ResultatPropositionSalle.Lancement::table).toList());
    }

    // --- Brouillon de l'éditeur ------------------------------------------------------

    @Test
    void l_editeur_part_de_la_grille_par_defaut_et_n_ecrit_rien_avant_d_appliquer() {
        Tournoi t = tournoi(4);
        EditionPlanSalles e = new EditionPlanSalles(t);
        assertEquals(4, e.nombreDeTablesPosees());
        assertEquals(5, e.premierNumeroLibre());
        assertEquals(ResultatPlacementPlan.Motif.CHEVAUCHEMENT, e.ajouterRepere(TypeRepere.WC, 0, 1).getMotif(),
                "la ligne 1 est une allée");
        assertTrue(e.ajouterRepere(TypeRepere.WC, 2, 0).estReussi());
        assertFalse(t.aUnPlanDeSalle());
        e.appliquer(t);
        assertTrue(t.aUnPlanDeSalle());
        assertEquals("Salle", t.getSalles().get(0).getNom());
    }

    @Test
    void numerotation_unique_sur_toutes_les_salles() {
        Tournoi t = tournoi(4);
        EditionPlanSalles e = new EditionPlanSalles(t);
        assertTrue(e.ajouterSalle("Dojo", 1).estReussi());
        assertEquals(1, e.getIndexCourant());
        ResultatPlacementPlan ajout = e.ajouterTable(1, 2);
        assertEquals(ResultatPlacementPlan.Motif.PLACE, ajout.getMotif());
        assertEquals(List.of(5), numeros(e.planCourant()), "plus petit numéro libre du tournoi");
        ResultatPlacementPlan doublon = e.numeroter(5, 2);
        assertEquals(ResultatPlacementPlan.Motif.NUMERO_DEJA_PRIS, doublon.getMotif());
        assertEquals(2, ((TablePlan) doublon.getConflit()).numero());
        assertTrue(e.numeroter(5, 40).estReussi());
        assertEquals(5, e.premierNumeroLibre());
        assertEquals(ResultatPlacementPlan.Motif.CHEVAUCHEMENT, e.ajouterTable(2, 3).getMotif());
    }

    @Test
    void plan_type_dans_une_seconde_salle_numerote_apres_les_tables_des_autres_salles() {
        Tournoi t = tournoi(10);
        EditionPlanSalles e = new EditionPlanSalles(t);
        assertTrue(e.appliquerPlanType(PlansTypesSalle.PlanType.DEUX_RANGEES, 6).estReussi());
        assertEquals(List.of(1, 2, 3, 4, 5, 6), numeros(e.planCourant()));
        e.ajouterSalle("Dojo", 0);
        assertEquals(4, e.tablesRestantesPourLaSalle());
        assertTrue(e.appliquerPlanType(PlansTypesSalle.PlanType.UNE_RANGEE, e.tablesRestantesPourLaSalle()).estReussi());
        assertEquals(List.of(7, 8, 9, 10), numeros(e.planCourant()));
        e.appliquer(t);
        assertTrue(service.verifier(t).estCoherent());
    }

    @Test
    void gestion_des_salles_nom_unique_derniere_salle_gardee_tableaux_connus() {
        Tournoi t = tournoi(2);
        t.ajouterTableau(new Tableau("Dames", ConfigurationTableau.depuisFormat(RegistreFormats.formatParDefaut()).build()));
        EditionPlanSalles e = new EditionPlanSalles(t);
        assertEquals(ResultatEditionSalles.Motif.DERNIERE_SALLE, e.retirerSalle().getMotif());
        assertEquals(ResultatEditionSalles.Motif.NOM_VIDE, e.ajouterSalle(" ", 0).getMotif());
        assertEquals(ResultatEditionSalles.Motif.NOM_DEJA_PRIS, e.ajouterSalle("salle", 0).getMotif());
        assertTrue(e.ajouterSalle("Dojo", 1).estReussi());
        assertEquals(ResultatEditionSalles.Motif.NOM_DEJA_PRIS, e.renommerSalle("SALLE").getMotif());
        assertTrue(e.renommerSalle("Petit dojo").estReussi());
        assertEquals(ResultatEditionSalles.Motif.TABLEAU_INCONNU, e.reserverSalle(Set.of("Open")).getMotif());
        assertTrue(e.reserverSalle(Set.of("Dames")).estReussi());
        assertTrue(e.preferer(Set.of("Dames")).estReussi());
        assertEquals(ResultatPlacementPlan.Motif.ELEMENT_INTROUVABLE, e.reserverTable(1, "Open").getMotif());
        assertTrue(e.changerEtage(2).estReussi());
        assertEquals(2, e.salleCourante().getEtage());
        assertEquals(ResultatEditionSalles.Motif.SALLE_INTROUVABLE, e.choisirSalle(5).getMotif());
        assertEquals(ResultatEditionSalles.Motif.SALLE_RETIREE, e.retirerSalle().getMotif());
        assertEquals(0, e.getIndexCourant());
        assertEquals(1, e.getSalles().size());
    }

    @Test
    void le_bilan_du_brouillon_suit_les_modifications_avant_enregistrement() {
        Tournoi t = tournoi(3);
        EditionPlanSalles e = new EditionPlanSalles(t);
        assertTrue(e.bilan().estCoherent());
        assertEquals(ResultatPlacementPlan.Motif.HORS_GRILLE, e.ajouterTable(0, 5).getMotif());
        assertTrue(e.redimensionner(10, 8).estReussi());
        assertTrue(e.ajouterTable(0, 5).estReussi());
        assertEquals(Set.of(4), e.bilan().tablesEnTrop());
        assertTrue(e.retirer(1, 2).estReussi());
        assertEquals(Set.of(1), e.bilan().tablesHorsPlan());
        assertTrue(service.verifier(t).estCoherent(), "le tournoi n'a pas changé");
    }
}
