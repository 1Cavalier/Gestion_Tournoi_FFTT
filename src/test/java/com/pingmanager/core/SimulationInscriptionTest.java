package com.pingmanager.core;

import com.pingmanager.core.ResultatInscriptionTournoi.Motif;
import com.pingmanager.model.CategorieAge;
import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.FicheLicence;
import com.pingmanager.model.HorairesTableau;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.QuotasTableaux;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.TarifInscription;
import com.pingmanager.model.Tournoi;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Essai à blanc d'une sélection (validation en direct), inscription tout-ou-rien,
 * catégories d'âge, motifs en clair et remplissage.
 */
class SimulationInscriptionTest {

    private static final LocalDate SAMEDI = LocalDate.of(2027, 4, 10);

    private final ServiceInscription service = ServiceInscription.sansContrainteSuccession();

    private static Tableau tab(String nom, int h, ConfigurationTableau.Builder b) {
        Tableau t = new Tableau(nom, b.tarif(new TarifInscription(9, 10)).build());
        t.setHoraires(new HorairesTableau(null, null, SAMEDI.atTime(h, 0)));
        return t;
    }

    /** Samedi : A 8h (≤ 600), B 9h (≤ 1500), C 10h (≤ 900), D 11h (≤ 1800), G 16h (dames). */
    private static Tournoi tournoi() {
        Tournoi t = new Tournoi("Test", "Club", SAMEDI);
        t.ajouterTableau(tab("A", 8, ConfigurationTableau.builder().placesMax(2).tailleMaxListeAttente(1).pointMax(600)));
        t.ajouterTableau(tab("B", 9, ConfigurationTableau.builder().placesMax(10).pointMax(1500)));
        t.ajouterTableau(tab("C", 10, ConfigurationTableau.builder().placesMax(10).pointMax(900)));
        t.ajouterTableau(tab("D", 11, ConfigurationTableau.builder().placesMax(10).pointMax(1800)));
        t.ajouterTableau(tab("G", 16, ConfigurationTableau.builder().placesMax(10).toutesSeries().sexeRequis(Sexe.FEMME)));
        t.setQuotasTableaux(QuotasTableaux.parJour(Map.of(SAMEDI, new QuotasTableaux.Quota(2, 3)))
                .avecTableauBonusJoueuses("G").avecTableauBonusObligatoire(true).avecTableauxConsecutifsInterdits(true));
        return t;
    }

    private static Tableau t(Tournoi to, String nom) {
        return to.getTableaux().stream().filter(x -> x.getNom().equals(nom)).findFirst().orElseThrow();
    }

    @Test
    void la_joueuse_peut_cocher_le_tableau_dames_apres_les_autres() {
        Tournoi to = tournoi();
        Joueur j = new Joueur("1", "X", "Léa", Sexe.FEMME, "Club", 800);
        // Cochés dans le « désordre » : le tableau dames est traité en premier.
        var r = service.simuler(to, j, List.of(t(to, "B"), t(to, "G")));
        assertEquals(List.of("G", "B"), r.keySet().stream().map(Tableau::getNom).toList());
        assertTrue(r.values().stream().allMatch(ResultatInscriptionTournoi::estAccepte), r.toString());
        // D (11h) précède directement G (16h) dans la journée : refusé.
        var r2 = service.simuler(to, j, List.of(t(to, "B"), t(to, "D"), t(to, "G")));
        assertEquals(Motif.TABLEAUX_SUCCESSIFS, r2.get(t(to, "D")).getMotif());
        // Rien n'a été modifié.
        assertTrue(to.getTableaux().stream().allMatch(x -> x.getInscrits().isEmpty()));
    }

    @Test
    void sans_le_tableau_dames_la_joueuse_est_refusee_avec_le_motif() {
        Tournoi to = tournoi();
        Joueur j = new Joueur("1", "X", "Léa", Sexe.FEMME, "Club", 800);
        var r = service.simuler(to, j, List.of(t(to, "B")));
        assertEquals(Motif.TABLEAU_FEMININ_OBLIGATOIRE, r.get(t(to, "B")).getMotif());
        assertTrue(MotifsInscription.texte(r.get(t(to, "B")), to, j, t(to, "B")).contains("« G »"));
    }

    @Test
    void quota_et_tableaux_consecutifs_dans_la_selection() {
        Tournoi to = tournoi();
        Joueur j = new Joueur("2", "Y", "Paul", Sexe.HOMME, "Club", 850);
        var r = service.simuler(to, j, List.of(t(to, "B"), t(to, "C")));
        assertEquals(Motif.INSCRIT, r.get(t(to, "B")).getMotif());
        assertEquals(Motif.TABLEAUX_SUCCESSIFS, r.get(t(to, "C")).getMotif());
        assertEquals("B", r.get(t(to, "C")).getTableauEnConflit().getNom());

        var r2 = service.simuler(to, j, List.of(t(to, "B"), t(to, "D"), t(to, "C")));
        // B accepté, C suit B (refusé), D accepté → 2 tableaux.
        assertEquals(Motif.TABLEAUX_SUCCESSIFS, r2.get(t(to, "C")).getMotif());
        assertEquals(Motif.INSCRIT, r2.get(t(to, "D")).getMotif());
    }

    @Test
    void quota_par_jour_atteint() {
        Tournoi to = tournoi();
        to.setQuotasTableaux(QuotasTableaux.parJour(Map.of(SAMEDI, new QuotasTableaux.Quota(1, 1))));
        Joueur j = new Joueur("2", "Y", "Paul", Sexe.HOMME, "Club", 850);
        var r = service.simuler(to, j, List.of(t(to, "B"), t(to, "D")));
        assertEquals(Motif.QUOTA_TABLEAUX_ATTEINT, r.get(t(to, "D")).getMotif());
        assertTrue(MotifsInscription.texte(r.get(t(to, "D")), to, j, t(to, "D")).contains("1 au plus"));
    }

    @Test
    void points_et_motif_en_clair() {
        Tournoi to = tournoi();
        Joueur j = new Joueur("3", "Z", "Quentin", Sexe.HOMME, "Brunoy", 923);
        var r = service.simuler(to, j, List.of(t(to, "C")));
        assertEquals(Motif.NON_ELIGIBLE, r.get(t(to, "C")).getMotif());
        assertEquals("Vos 923 points dépassent la limite du tableau (NC à 900)",
                MotifsInscription.texte(r.get(t(to, "C")), to, j, t(to, "C")));
        assertEquals("Tableau réservé aux dames", MotifsInscription.texte(
                service.simuler(to, j, List.of(t(to, "G"))).get(t(to, "G")), to, j, t(to, "G")));
    }

    @Test
    void capacite_prevue_puis_liste_d_attente_pleine() {
        Tournoi to = tournoi();
        Tableau a = t(to, "A");
        service.inscrire(to, new Joueur("10", "A", "a", Sexe.HOMME, "C", 500), a);
        service.inscrire(to, new Joueur("11", "B", "b", Sexe.HOMME, "C", 400), a);
        Joueur j = new Joueur("12", "C", "c", Sexe.HOMME, "C", 300);
        assertEquals(Motif.EN_FILE_ATTENTE, service.simuler(to, j, List.of(a)).get(a).getMotif());
        assertEquals("Tableau complet : liste d'attente, 1re position — rien à payer sauf si repêché",
                MotifsInscription.texte(service.simuler(to, j, List.of(a)).get(a), to, j, a));
        service.inscrire(to, j, a);
        assertEquals(Motif.LISTE_ATTENTE_PLEINE, service.simuler(to, new Joueur("13", "D", "d", Sexe.HOMME, "C", 200),
                List.of(a)).get(a).getMotif());
    }

    @Test
    void inscription_selection_tout_ou_rien() {
        Tournoi to = tournoi();
        Joueur j = new Joueur("2", "Y", "Paul", Sexe.HOMME, "Club", 850);
        var refus = service.inscrireSelection(to, j, List.of(t(to, "B"), t(to, "C")));
        assertFalse(refus.values().stream().allMatch(ResultatInscriptionTournoi::estAccepte));
        assertTrue(t(to, "B").getInscrits().isEmpty(), "rien n'est enregistré en cas de refus");

        var ok = service.inscrireSelection(to, j, List.of(t(to, "D"), t(to, "B")));
        assertTrue(ok.values().stream().allMatch(ResultatInscriptionTournoi::estInscrit));
        assertTrue(t(to, "B").getInscrits().contains(j) && t(to, "D").getInscrits().contains(j));
        assertEquals(18.0, RemplissageTableaux.montantARegler(to, j));
        t(to, "B").marquerPaye(j);
        assertEquals(9.0, RemplissageTableaux.montantARegler(to, j));
    }

    @Test
    void categories_non_admises_et_par_tableau() {
        Tournoi to = tournoi();
        to.setCategoriesExclues(List.of(CategorieAge.POUSSIN));
        FicheLicence enfant = new FicheLicence.Builder("5", "PETIT", "Léo").sexe(Sexe.HOMME).club("1", "C")
                .points(500).dateNaissance(LocalDate.of(2019, 3, 1)).build();
        Joueur poussin = JoueurDepuisLicence.creer(enfant, to);
        assertEquals(CategorieAge.POUSSIN, poussin.getCategorie());
        var r = service.simuler(to, poussin, List.of(t(to, "A")));
        assertEquals(Motif.CATEGORIE_NON_ADMISE, r.get(t(to, "A")).getMotif());
        assertEquals("Les poussins ne sont pas admis dans ce tournoi",
                MotifsInscription.texte(r.get(t(to, "A")), to, poussin, t(to, "A")));

        Tableau veterans = tab("V", 18, ConfigurationTableau.builder().placesMax(10).toutesSeries()
                .autoriserCategorie(CategorieAge.VETERAN_40).autoriserCategorie(CategorieAge.VETERAN_45));
        to.ajouterTableau(veterans);
        Joueur senior = new Joueur("6", "S", "s", Sexe.HOMME, "C", 900);
        senior.setCategorie(CategorieAge.SENIOR);
        assertEquals(Motif.CATEGORIE_NON_AUTORISEE, service.inscrire(to, senior, veterans).getMotif());
        Joueur sansCategorie = new Joueur("7", "S", "s", Sexe.HOMME, "C", 900);
        assertEquals(Motif.INSCRIT, service.inscrire(to, sansCategorie, veterans).getMotif(),
                "catégorie inconnue : pas de contrôle possible");
    }

    @Test
    void remplissage_trie_par_points_et_vue_par_joueur() {
        Tournoi to = tournoi();
        Joueur a = new Joueur("20", "AA", "a", Sexe.HOMME, "Zeta", 700);
        Joueur b = new Joueur("21", "BB", "b", Sexe.HOMME, "Alpha", 1400);
        Joueur c = new Joueur("22", "CC", "c", Sexe.FEMME, "Alpha", 1000);
        service.inscrire(to, a, t(to, "B"));
        service.inscrire(to, b, t(to, "B"));
        service.inscrire(to, c, t(to, "G"));
        service.inscrire(to, c, t(to, "B"));
        RemplissageTableaux.EtatTableau eb = RemplissageTableaux.parTableau(to).get(1);
        assertEquals("B", eb.tableau().getNom());
        assertEquals(List.of(b, c, a), eb.inscrits());
        assertEquals(7, eb.placesRestantes());
        assertEquals(0.3, eb.taux(), 1e-9);
        var parClub = RemplissageTableaux.parJoueur(to, RemplissageTableaux.PAR_CLUB);
        assertEquals(List.of(b, c, a), parClub.stream().map(RemplissageTableaux.Participant::joueur).toList());
        assertEquals(List.of("B", "G"), parClub.get(1).inscrit().stream().map(Tableau::getNom).toList());
        assertEquals(3, RemplissageTableaux.nombreDeParticipants(to));
    }

    @Test
    void calendrier_et_periode_d_inscription() {
        Tournoi to = tournoi();
        to.setPeriode(SAMEDI, SAMEDI.plusDays(1));
        assertEquals(CalendrierTournoi.Moment.A_VENIR, CalendrierTournoi.moment(to, SAMEDI.minusDays(1)));
        assertEquals(CalendrierTournoi.Moment.EN_COURS, CalendrierTournoi.moment(to, SAMEDI.plusDays(1)));
        assertEquals(CalendrierTournoi.Moment.PASSE, CalendrierTournoi.moment(to, SAMEDI.plusDays(2)));
        assertEquals(CalendrierTournoi.EtatInscriptions.PAS_ENCORE_OUVERTES,
                CalendrierTournoi.etatInscriptions(to, LocalDateTime.of(2027, 1, 1, 0, 0)), "brouillon");
        to.changerStatut(com.pingmanager.model.StatutTournoi.HOMOLOGUE);
        to.changerStatut(com.pingmanager.model.StatutTournoi.PUBLIE);
        assertEquals(CalendrierTournoi.EtatInscriptions.OUVERTES,
                CalendrierTournoi.etatInscriptions(to, LocalDateTime.of(2027, 1, 1, 0, 0)));
        assertEquals(CalendrierTournoi.EtatInscriptions.CLOSES,
                CalendrierTournoi.etatInscriptions(to, SAMEDI.atStartOfDay()));
    }
}
