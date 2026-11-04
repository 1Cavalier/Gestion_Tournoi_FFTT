package com.pingmanager.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConfigurationTableauTest {

    private Joueur joueur(int points, Sexe sexe) {
        return new Joueur("L" + points + sexe, "Nom", "Prenom", sexe, "Club", points);
    }

    // --- Modes de limite de classement ---

    @Test
    void mode_tranche_accepte_dans_la_fourchette_et_refuse_hors() {
        ConfigurationTableau c = ConfigurationTableau.builder().tranche(1000, 1299).build();
        assertEquals(ModeLimiteClassement.TRANCHE, c.getModeLimite());
        assertFalse(c.pointsAutorises(999));
        assertTrue(c.pointsAutorises(1000));
        assertTrue(c.pointsAutorises(1299));
        assertFalse(c.pointsAutorises(1300));
    }

    @Test
    void mode_point_max_ne_verifie_que_le_plafond() {
        ConfigurationTableau c = ConfigurationTableau.builder().pointMax(1500).build();
        assertEquals(ModeLimiteClassement.POINT_MAX, c.getModeLimite());
        assertTrue(c.pointsAutorises(500));
        assertTrue(c.pointsAutorises(1500));
        assertFalse(c.pointsAutorises(1501));
    }

    @Test
    void mode_toutes_series_accepte_tout() {
        ConfigurationTableau c = ConfigurationTableau.builder().toutesSeries().build();
        assertEquals(ModeLimiteClassement.TOUTES_SERIES, c.getModeLimite());
        assertTrue(c.pointsAutorises(0));
        assertTrue(c.pointsAutorises(4000));
    }

    @Test
    void defaut_est_toutes_series() {
        ConfigurationTableau c = ConfigurationTableau.builder().build();
        assertEquals(ModeLimiteClassement.TOUTES_SERIES, c.getModeLimite());
    }

    @Test
    void tranche_incoherente_est_refusee() {
        assertThrows(IllegalArgumentException.class,
                () -> ConfigurationTableau.builder().tranche(1300, 1000).build());
    }

    // --- Rétro-compatibilité des anciennes bornes ---

    @Test
    void compat_pointsMax_seul_bascule_en_point_max() {
        ConfigurationTableau c = ConfigurationTableau.builder().pointsMax(1200).build();
        assertEquals(ModeLimiteClassement.POINT_MAX, c.getModeLimite());
        assertFalse(c.joueurEligible(joueur(1500, Sexe.HOMME)));
        assertTrue(c.joueurEligible(joueur(1100, Sexe.HOMME)));
    }

    @Test
    void compat_pointsMin_et_pointsMax_forment_une_tranche() {
        ConfigurationTableau c = ConfigurationTableau.builder().pointsMin(800).pointsMax(1200).build();
        assertEquals(ModeLimiteClassement.TRANCHE, c.getModeLimite());
        assertFalse(c.pointsAutorises(799));
        assertTrue(c.pointsAutorises(1000));
        assertFalse(c.pointsAutorises(1201));
    }

    // --- Exclusion par rang national ---

    @Test
    void exclusion_rang_national_refuse_les_mieux_numerotes() {
        ConfigurationTableau c = ConfigurationTableau.builder()
                .toutesSeries()
                .exclusionRangNational(500)
                .build();
        Joueur numero100 = joueur(3000, Sexe.HOMME);
        numero100.setRangNational(100);
        Joueur numero800 = joueur(2500, Sexe.HOMME);
        numero800.setRangNational(800);

        assertFalse(c.rangNationalAutorise(numero100), "n°100 doit être exclu (<= 500)");
        assertTrue(c.rangNationalAutorise(numero800), "n°800 doit être admis (> 500)");
        assertFalse(c.joueurEligible(numero100));
        assertTrue(c.joueurEligible(numero800));
    }

    @Test
    void rang_national_inconnu_n_est_jamais_exclu() {
        ConfigurationTableau c = ConfigurationTableau.builder()
                .pointMax(2000)
                .exclusionRangNational(500)
                .build();
        Joueur sansRang = joueur(1800, Sexe.HOMME); // rang inconnu par défaut
        assertFalse(sansRang.aRangNationalConnu());
        assertTrue(c.rangNationalAutorise(sansRang));
        assertTrue(c.joueurEligible(sansRang));
    }

    @Test
    void exclusion_rang_national_interdite_en_mode_tranche() {
        assertThrows(IllegalArgumentException.class,
                () -> ConfigurationTableau.builder().tranche(1000, 1500).exclusionRangNational(500).build());
    }

    // --- Sexe et catégories ---

    @Test
    void tableau_reserve_aux_femmes_refuse_les_hommes() {
        ConfigurationTableau c = ConfigurationTableau.builder().sexeRequis(Sexe.FEMME).build();
        assertFalse(c.joueurEligible(joueur(1000, Sexe.HOMME)));
        assertTrue(c.joueurEligible(joueur(1000, Sexe.FEMME)));
    }

    @Test
    void categories_vide_signifie_toutes_admises() {
        ConfigurationTableau c = ConfigurationTableau.builder().build();
        assertTrue(c.estOuvertATouteCategorie());
        assertTrue(c.categorieAutorisee(CategorieAge.SENIOR));
        assertTrue(c.categorieAutorisee(CategorieAge.VETERAN_60));
    }

    @Test
    void categories_restreintes_filtrent() {
        ConfigurationTableau c = ConfigurationTableau.builder()
                .autoriserCategorie(CategorieAge.JUNIOR)
                .autoriserCategorie(CategorieAge.CADET)
                .build();
        assertFalse(c.estOuvertATouteCategorie());
        assertTrue(c.categorieAutorisee(CategorieAge.JUNIOR));
        assertTrue(c.categorieAutorisee(CategorieAge.CADET));
        assertFalse(c.categorieAutorisee(CategorieAge.SENIOR));
    }

    // --- Tarifs ---

    @Test
    void tarif_distingue_en_ligne_et_sur_place() {
        ConfigurationTableau c = ConfigurationTableau.builder()
                .tarif(new TarifInscription(8, 10))
                .build();
        assertEquals(8.0, c.getTarif().getMontantEnLigne());
        assertEquals(10.0, c.getTarif().getMontantSurPlace());
    }

    @Test
    void tarif_par_defaut_est_gratuit() {
        ConfigurationTableau c = ConfigurationTableau.builder().build();
        assertEquals(0.0, c.getTarif().getMontantEnLigne());
        assertEquals(0.0, c.getTarif().getMontantSurPlace());
    }
}
