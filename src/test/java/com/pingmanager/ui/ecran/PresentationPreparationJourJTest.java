package com.pingmanager.ui.ecran;

import com.pingmanager.core.ListePointage;
import com.pingmanager.core.PrevisionTournoi;
import com.pingmanager.core.ReglagesPrevision;
import com.pingmanager.core.ServicePreparationJourJ;
import com.pingmanager.core.ServicePreparationJourJ.Acces;
import com.pingmanager.core.SimulateurTournoi;
import com.pingmanager.model.AffectationJugeArbitre;
import com.pingmanager.model.ConditionsMaintien;
import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.GradeJugeArbitre;
import com.pingmanager.model.HorairesTableau;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.StatutTableau;
import com.pingmanager.model.StatutTournoi;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.ui.ecran.PresentationPreparationJourJ.CarteAcces;
import com.pingmanager.ui.ecran.PresentationPreparationJourJ.Disponibilite;
import com.pingmanager.ui.ecran.PresentationPreparationJourJ.Impression;
import com.pingmanager.ui.ecran.PresentationPreparationJourJ.JourneeTableaux;
import com.pingmanager.ui.ecran.PresentationPreparationJourJ.LigneAlerte;
import com.pingmanager.ui.ecran.PresentationPreparationJourJ.LigneTableau;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** B051 : mots, ordre et états de l'étape « J-7 : préparer le jour J ». */
class PresentationPreparationJourJTest {

    private static final LocalDate SAMEDI = LocalDate.of(2027, 4, 10);
    private static final LocalDate DIMANCHE = SAMEDI.plusDays(1);

    private static Tournoi tournoi() {
        Tournoi t = new Tournoi("Open de Brunoy", "Brunoy", List.of(SAMEDI, DIMANCHE));
        t.changerStatut(StatutTournoi.HOMOLOGUE);
        t.changerStatut(StatutTournoi.PUBLIE);
        return t;
    }

    private static Tableau tableau(Tournoi t, String nom, int places, LocalDate jour, int heure) {
        Tableau tab = new Tableau(nom, ConfigurationTableau.builder().placesMax(places).tailleMaxListeAttente(4).build());
        if (jour != null) {
            tab.setHoraires(new HorairesTableau(jour.atTime(heure - 1, 0), jour.atTime(heure - 1, 40),
                    jour.atTime(heure, 0)));
        }
        t.ajouterTableau(tab);
        return tab;
    }

    private static Joueur joueur(int n, String club, int points) {
        return new Joueur("91" + n, "Nom" + n, "Prenom" + n, Sexe.HOMME, club, points);
    }

    private static Acces acces(boolean proposee, boolean ouverte, long jours, boolean demo) {
        return new Acces(proposee, ouverte, SAMEDI.minusDays(7), jours, demo);
    }

    // ------------------------------------------------------------------
    // Accès et en-tête
    // ------------------------------------------------------------------

    @Test
    void compte_a_rebours_j_moins_n_jour_j_puis_j_plus_n() {
        assertEquals("J-7", PresentationPreparationJourJ.compteARebours(7));
        assertEquals("J-1", PresentationPreparationJourJ.compteARebours(1));
        assertEquals("Jour J", PresentationPreparationJourJ.compteARebours(0));
        assertEquals("J+1", PresentationPreparationJourJ.compteARebours(-1));
        assertEquals("Préparation — J-3", PresentationPreparationJourJ.sousTitre(acces(true, true, 3, false)));
    }

    @Test
    void carte_invisible_hors_homologue_et_publie() {
        CarteAcces c = PresentationPreparationJourJ.carte(acces(false, false, 3, false));
        assertFalse(c.visible());
        assertFalse(c.ouverte());
    }

    @Test
    void carte_avant_j_moins_7_explique_la_date_d_ouverture_sans_s_ouvrir() {
        CarteAcces c = PresentationPreparationJourJ.carte(acces(true, false, 30, false));
        assertTrue(c.visible());
        assertFalse(c.ouverte());
        assertEquals("J-30", c.pastille());
        assertTrue(c.etat().startsWith("Disponible à partir du samedi 3 avril 2027"), c.etat());
    }

    @Test
    void carte_ouverte_a_partir_de_j_moins_7_et_toujours_en_demo() {
        CarteAcces j5 = PresentationPreparationJourJ.carte(acces(true, true, 5, false));
        assertTrue(j5.ouverte());
        assertEquals("Le tournoi commence dans 5 jours.", j5.etat());
        assertEquals("Le tournoi commence demain.",
                PresentationPreparationJourJ.carte(acces(true, true, 1, false)).etat());
        assertEquals("Le tournoi commence aujourd'hui.",
                PresentationPreparationJourJ.carte(acces(true, true, 0, false)).etat());
        assertEquals("Le tournoi a commencé.",
                PresentationPreparationJourJ.carte(acces(true, true, -1, false)).etat());

        CarteAcces demo = PresentationPreparationJourJ.carte(acces(true, true, 120, true));
        assertTrue(demo.ouverte());
        assertTrue(demo.etat().contains("tournoi de démonstration"), demo.etat());
        assertTrue(demo.etat().contains("samedi 3 avril 2027"), demo.etat());
    }

    @Test
    void dates_sur_un_ou_plusieurs_jours() {
        assertEquals("Du samedi 10 avril 2027 au dimanche 11 avril 2027", PresentationPreparationJourJ.dates(tournoi()));
        assertEquals("Samedi 10 avril 2027",
                PresentationPreparationJourJ.dates(new Tournoi("Open", "Brunoy", SAMEDI)));
    }

    @Test
    void bandeau_dit_que_rien_n_est_lance() {
        assertTrue(PresentationPreparationJourJ.BANDEAU.contains("rien n'est lancé"));
        assertEquals(List.of("Général", "Tableaux", "Pointage", "Salle", "À imprimer / à préparer"),
                java.util.Arrays.stream(PresentationPreparationJourJ.Onglet.values())
                        .map(PresentationPreparationJourJ.Onglet::libelle).toList());
    }

    // ------------------------------------------------------------------
    // Alertes et chiffres
    // ------------------------------------------------------------------

    @Test
    void alertes_a_traiter_d_abord_puis_a_savoir_joueurs_regroupes() {
        Tournoi t = tournoi();
        t.setConditionsMaintien(new ConditionsMaintien(4, null, null, 0, 0, 0));
        Tableau a = tableau(t, "A", 2, SAMEDI, 9);
        tableau(t, "B", 8, null, 0);
        a.inscrire(joueur(1, "", 1200));
        a.inscrire(joueur(2, "", 0));
        a.inscrire(joueur(3, "TT Brunoy", 900));

        List<LigneAlerte> lignes = PresentationPreparationJourJ.alertes(ServicePreparationJourJ.alertes(t));
        assertEquals(List.of(
                "Juge-arbitre principal non désigné",
                "Tableau A : sous le seuil de maintien",
                "Tableau B : horaire non renseigné",
                "Tableau B : sous le seuil de maintien",
                "2 joueurs sans club",
                "1 joueur sans points",
                "Tableau A : complet, liste d'attente"),
                lignes.stream().map(LigneAlerte::titre).toList());
        assertEquals("2 inscrits pour 2 places · 1 joueur attend une place : à repêcher en cas de désistement.",
                lignes.get(6).detail());
        assertEquals("NOM1 Prenom1, NOM2 Prenom2.", lignes.get(4).detail());
        assertEquals(StatutPastille.ATTENTION, lignes.get(0).statut());
        assertEquals("À traiter", lignes.get(0).pastille());
        assertEquals(StatutPastille.INFO, lignes.get(6).statut());
        assertEquals("À savoir", lignes.get(6).pastille());
        assertTrue(lignes.get(1).detail().startsWith("2 inscrits pour un minimum de 4"), lignes.get(1).detail());
        assertEquals("6 alertes à traiter", PresentationPreparationJourJ.resumeAlertes(lignes));
    }

    @Test
    void complet_sans_attente_et_attente_sans_complet_restent_deux_lignes_distinctes() {
        Tournoi t = tournoi();
        Tableau a = tableau(t, "A", 1, SAMEDI, 9);
        a.inscrire(joueur(1, "TT Brunoy", 1200));
        List<String> titres = PresentationPreparationJourJ.alertes(ServicePreparationJourJ.alertes(t)).stream()
                .map(LigneAlerte::titre).toList();
        assertTrue(titres.contains("Tableau A : complet"), titres.toString());
        assertEquals("Tableau X : liste d'attente", PresentationPreparationJourJ.alertes(List.of(
                new ServicePreparationJourJ.Alerte(ServicePreparationJourJ.TypeAlerte.LISTE_ATTENTE_NON_VIDE,
                        List.of("X"), null, 2, 0))).get(0).titre());
    }

    @Test
    void sans_seuil_defini_aucune_alerte_de_maintien_et_resume_vide() {
        Tournoi t = tournoi();
        t.ajouterJugeArbitre(new AffectationJugeArbitre("ARBITRE", "Jeanne", "ja@exemple.invalid", "7700001",
                GradeJugeArbitre.JA_NATIONAL, null));
        Tableau a = tableau(t, "A", 8, SAMEDI, 9);
        a.inscrire(joueur(1, "TT Brunoy", 1200));
        List<LigneAlerte> lignes = PresentationPreparationJourJ.alertes(ServicePreparationJourJ.alertes(t));
        assertEquals(List.of(), lignes);
        assertEquals("Aucune alerte à traiter", PresentationPreparationJourJ.resumeAlertes(lignes));
    }

    @Test
    void joueurs_trop_nombreux_les_cinq_premiers_sont_nommes() {
        Tournoi t = tournoi();
        Tableau a = tableau(t, "A", 16, SAMEDI, 9);
        for (int i = 1; i <= 7; i++) {
            a.inscrire(joueur(i, "", 1000));
        }
        LigneAlerte sansClub = PresentationPreparationJourJ.alertes(ServicePreparationJourJ.alertes(t)).stream()
                .filter(l -> l.titre().contains("sans club")).findFirst().orElseThrow();
        assertEquals("7 joueurs sans club", sansClub.titre());
        assertTrue(sansClub.detail().endsWith("NOM5 Prenom5 et 2 autres."), sansClub.detail());
    }

    @Test
    void chiffres_cles_joueurs_inscriptions_attente_payes_tables() {
        Tournoi t = tournoi();
        Tableau a = tableau(t, "A", 2, SAMEDI, 9);
        Tableau b = tableau(t, "B", 8, SAMEDI, 11);
        Joueur j1 = joueur(1, "TT Brunoy", 1200);
        a.inscrire(j1);
        b.inscrire(j1);
        a.inscrire(joueur(2, "TT Brunoy", 1100));
        a.inscrire(joueur(3, "TT Brunoy", 1000)); // liste d'attente
        a.marquerPaye(j1);
        List<PresentationPreparationJourJ.Chiffre> c = PresentationPreparationJourJ.chiffres(t);
        assertEquals(List.of("Joueurs inscrits", "Inscriptions aux tableaux", "En liste d'attente",
                "Inscriptions payées", "Tables"), c.stream().map(PresentationPreparationJourJ.Chiffre::libelle).toList());
        assertEquals("3", c.get(1).valeur());
        assertEquals("1", c.get(2).valeur());
        assertEquals("1 / 3", c.get(3).valeur());
        assertEquals("—", c.get(4).valeur(), "tables non renseignées");
        t.setNombreDeTables(12);
        assertEquals("12", PresentationPreparationJourJ.chiffres(t).get(4).valeur());
    }

    // ------------------------------------------------------------------
    // Tableaux, pointage, salle
    // ------------------------------------------------------------------

    @Test
    void tableaux_par_jour_dans_l_ordre_des_horaires_avec_compteurs_et_etat() {
        Tournoi t = tournoi();
        t.setConditionsMaintien(new ConditionsMaintien(2, null, null, 0, 0, 0));
        Tableau tard = tableau(t, "B", 8, SAMEDI, 14);
        Tableau tot = tableau(t, "A", 2, SAMEDI, 9);
        Tableau dimanche = tableau(t, "C", 8, DIMANCHE, 9);
        tableau(t, "D", 8, null, 0);
        Joueur j1 = joueur(1, "TT Brunoy", 1200);
        tot.inscrire(j1);
        tot.inscrire(joueur(2, "TT Brunoy", 1100));
        tot.inscrire(joueur(3, "TT Brunoy", 1000));
        tot.marquerPaye(j1);
        dimanche.inscrire(j1);
        dimanche.inscrire(joueur(4, "TT Brunoy", 800));

        List<JourneeTableaux> js = PresentationPreparationJourJ.journees(t);
        assertEquals(List.of("Samedi 10 avril 2027", "Dimanche 11 avril 2027",
                "Jour non défini (tableaux sans horaire)"), js.stream().map(JourneeTableaux::titre).toList());
        assertEquals(List.of("A", "B"), js.get(0).lignes().stream().map(LigneTableau::nom).toList());

        LigneTableau a = js.get(0).lignes().get(0);
        assertEquals("Pointage 8h – 8h40 · début 9h", a.horaires());
        assertEquals("2 / 2 inscrits", a.inscrits());
        assertEquals("1 en attente", a.attente());
        assertEquals("1 / 2 payés", a.payes());
        assertEquals("Complet", a.etat());
        assertEquals(StatutPastille.SUCCES, a.statut());
        assertEquals(1.0, a.jauge());

        LigneTableau b = js.get(0).lignes().get(1);
        assertEquals("Sous le seuil (2)", b.etat());
        assertEquals("Pas d'attente", b.attente());
        assertEquals(tard.getNom(), b.nom());

        assertEquals("Inscriptions ouvertes", js.get(1).lignes().get(0).etat());
        assertEquals("Sans horaire", js.get(2).lignes().get(0).etat());
        assertEquals(StatutPastille.ATTENTION, js.get(2).lignes().get(0).statut());
    }

    @Test
    void apercu_du_pointage_reprend_la_liste_du_poste_sans_rien_modifier() {
        Tournoi t = tournoi();
        Tableau a = tableau(t, "A", 8, SAMEDI, 9);
        Tableau b = tableau(t, "B", 8, SAMEDI, 11);
        Joueur j1 = joueur(1, "TT Brunoy", 1200);
        a.inscrire(j1);
        b.inscrire(j1);
        a.inscrire(joueur(2, "TT Brunoy", 1100));

        List<ListePointage.Ligne> apercu = PresentationPreparationJourJ.apercuPointage(t);
        assertEquals(new ListePointage(t).lignes("", null, ListePointage.FiltreEtat.TOUS), apercu);
        assertEquals(2, apercu.size());
        assertEquals(StatutTableau.INSCRIPTION, a.getStatut(), "l'aperçu n'ouvre pas le pointage");
        assertFalse(a.aPointe(j1));
        assertEquals("2 joueurs à pointer", PresentationPreparationJourJ.comptePointage(2, 2));
        assertEquals("150 premiers joueurs sur 412.", PresentationPreparationJourJ.comptePointage(150, 412));
        assertEquals("Aucun joueur inscrit pour l'instant.", PresentationPreparationJourJ.comptePointage(0, 0));
    }

    @Test
    void salle_tables_et_ordre_prevu_des_tableaux_par_journee() {
        Tournoi t = tournoi();
        tableau(t, "B", 8, SAMEDI, 14);
        Tableau a = tableau(t, "A", 8, SAMEDI, 9);
        tableau(t, "Tableau C", 8, DIMANCHE, 10);
        tableau(t, "D", 8, null, 0);
        a.inscrire(joueur(1, "TT Brunoy", 1200));
        assertEquals("Nombre de tables non renseigné", PresentationPreparationJourJ.resumeSalle(t));
        t.setNombreDeTables(16);
        assertEquals("16 tables de jeu · toutes libres tant que rien n'est lancé",
                PresentationPreparationJourJ.resumeSalle(t));

        List<PresentationPreparationJourJ.JourneeSalle> ordre = PresentationPreparationJourJ.ordrePrevu(t);
        assertEquals(2, ordre.size(), "les tableaux sans horaire n'ont pas d'ordre prévu");
        assertEquals("Samedi 10 avril 2027", ordre.get(0).titre());
        assertEquals(new PresentationPreparationJourJ.Passage(1, "Tableau A", "Début 9h · 1 joueur"),
                ordre.get(0).passages().get(0));
        assertEquals(new PresentationPreparationJourJ.Passage(2, "Tableau B", "Début 14h · 0 joueur"),
                ordre.get(0).passages().get(1));
        assertEquals("Tableau C", ordre.get(1).passages().get(0).nom());
    }

    // ------------------------------------------------------------------
    // Prévision et impressions
    // ------------------------------------------------------------------

    @Test
    void resume_de_la_prevision_une_phrase_par_journee_pour_la_strategie_du_tournoi() {
        Tournoi t = tournoi();
        t.setNombreDeTables(4);
        Tableau a = new Tableau("A", ConfigurationTableau.depuisFormat(
                com.pingmanager.model.format.FormatPoule3Qualifie2.INSTANCE).placesMax(8).build());
        a.setHoraires(new HorairesTableau(null, null, SAMEDI.atTime(9, 0)));
        Tableau c = new Tableau("C", ConfigurationTableau.depuisFormat(
                com.pingmanager.model.format.FormatPoule3Qualifie2.INSTANCE).placesMax(8).build());
        c.setHoraires(new HorairesTableau(null, null, DIMANCHE.atTime(9, 0)));
        t.ajouterTableau(a);
        t.ajouterTableau(c);
        for (int i = 1; i <= 6; i++) {
            Joueur j = joueur(i, "TT Brunoy", 1500 - 50 * i);
            a.inscrire(j);
            c.inscrire(j);
        }
        PrevisionTournoi p = new SimulateurTournoi().prevoir(t,
                ReglagesPrevision.builder().nombreDeTables(PresentationPrevisions.tablesProposees(t)).build());
        List<String> resume = PresentationPreparationJourJ.resumePrevision(p, t.getStrategieOrdonnancement());
        assertEquals(2, resume.size(), resume.toString());
        assertTrue(resume.get(0).startsWith("Samedi 10 avril : fin moyenne à "), resume.get(0));
        assertTrue(resume.get(0).contains("risque de dépassement : "), resume.get(0));
        assertTrue(resume.get(1).startsWith("Dimanche 11 avril : fin moyenne à "), resume.get(1));
        assertEquals(6, a.getInscrits().size(), "la prévision ne touche pas au tournoi");
        assertEquals(StatutTableau.INSCRIPTION, a.getStatut());
    }

    @Test
    void resume_de_la_prevision_sans_tableau_a_simuler() {
        Tournoi t = tournoi();
        PrevisionTournoi p = new SimulateurTournoi().prevoir(t, ReglagesPrevision.builder().build());
        assertEquals(List.of("Aucun tableau à simuler pour l'instant."),
                PresentationPreparationJourJ.resumePrevision(p, t.getStrategieOrdonnancement()));
    }

    @Test
    void impressions_un_bouton_seulement_pour_ce_qui_existe_deja() {
        List<Impression> liste = PresentationPreparationJourJ.impressions();
        for (Impression i : liste) {
            assertEquals(i.disponibilite() == Disponibilite.DISPONIBLE, i.aBouton(), i.titre());
            if (i.aBouton()) {
                assertFalse(i.libelleBouton().isBlank(), i.titre());
            }
        }
        assertTrue(liste.stream().anyMatch(i -> i.disponibilite() == Disponibilite.A_VENIR));
        assertTrue(liste.stream().anyMatch(i -> i.disponibilite() == Disponibilite.JOUR_J));
        assertEquals(PresentationPreparationJourJ.Document.REGLEMENT, liste.get(0).document());
        assertEquals("À venir", Disponibilite.A_VENIR.libelle());
    }
}
