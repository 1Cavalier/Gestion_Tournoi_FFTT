package com.pingmanager.core.reglement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.pingmanager.model.CategorieAge;
import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.GrilleRecompenses;
import com.pingmanager.model.Recompense;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.TarifInscription;
import org.junit.jupiter.api.Test;

/**
 * Tests de caractérisation : ils figent les libellés produits par
 * {@link LibellesTableau} avant la refonte de nuit du 13 novembre 2026.
 */
class LibellesTableauCaracterisationTest {

    private static String sansInsecables(String s) {
        return s.replace(' ', ' ').replace(' ', ' ');
    }

    @Test
    void points_selon_le_mode_de_limite_et_l_exclusion_par_rang() {
        assertEquals("Toutes séries", LibellesTableau.points(ConfigurationTableau.builder().build()));
        assertEquals("500 à 899 pts", LibellesTableau.points(ConfigurationTableau.builder().tranche(500, 899).build()));
        assertEquals("NC à 1499", LibellesTableau.points(ConfigurationTableau.builder().pointMax(1499).build()));
        assertEquals("Toutes séries (hors n° 1 à 300)",
                LibellesTableau.points(ConfigurationTableau.builder().exclusionRangNational(300).build()));
    }

    @Test
    void type_prefixe_points_sauf_toutes_series() {
        assertEquals("Toutes séries", LibellesTableau.type(ConfigurationTableau.builder().build()));
        assertEquals("Points 500 à 899 pts", LibellesTableau.type(ConfigurationTableau.builder().tranche(500, 899).build()));
        assertEquals("Points NC à 1499", LibellesTableau.type(ConfigurationTableau.builder().pointMax(1499).build()));
    }

    @Test
    void public_vise_sexe_puis_categories_detaillees_jusqu_a_deux() {
        assertEquals("Mixte", LibellesTableau.publicVise(ConfigurationTableau.builder().build()));
        assertEquals("Femmes", LibellesTableau.publicVise(ConfigurationTableau.builder().sexeRequis(Sexe.FEMME).build()));
        assertEquals("Hommes", LibellesTableau.publicVise(ConfigurationTableau.builder().sexeRequis(Sexe.HOMME).build()));
        ConfigurationTableau deux = ConfigurationTableau.builder().sexeRequis(Sexe.FEMME)
                .autoriserCategorie(CategorieAge.POUSSIN).autoriserCategorie(CategorieAge.BENJAMIN).build();
        assertEquals("Femmes, " + CategorieAge.POUSSIN.getLibelle() + ", " + CategorieAge.BENJAMIN.getLibelle(),
                LibellesTableau.publicVise(deux));
        ConfigurationTableau trois = ConfigurationTableau.builder()
                .autoriserCategorie(CategorieAge.POUSSIN).autoriserCategorie(CategorieAge.BENJAMIN)
                .autoriserCategorie(CategorieAge.MINIME).build();
        assertEquals("Mixte, 3 catégories", LibellesTableau.publicVise(trois));
    }

    @Test
    void categories_en_toutes_lettres_ou_null_sans_restriction() {
        assertNull(LibellesTableau.categories(ConfigurationTableau.builder().build()));
        ConfigurationTableau deux = ConfigurationTableau.builder()
                .autoriserCategorie(CategorieAge.POUSSIN).autoriserCategorie(CategorieAge.BENJAMIN).build();
        assertEquals(CategorieAge.POUSSIN.getLibelle() + ", " + CategorieAge.BENJAMIN.getLibelle(),
                LibellesTableau.categories(deux));
    }

    @Test
    void places_avec_ou_sans_liste_d_attente() {
        assertEquals("—", LibellesTableau.places(ConfigurationTableau.builder().build()));
        assertEquals("48 + 4 att.", LibellesTableau.places(ConfigurationTableau.builder().placesMax(48).build()));
        assertEquals("48", LibellesTableau.places(
                ConfigurationTableau.builder().placesMax(48).tailleMaxListeAttente(0).build()));
    }

    @Test
    void prix_selon_les_modes_de_reglement_proposes() {
        assertEquals("—", LibellesTableau.prix(null));
        assertEquals("Gratuit", LibellesTableau.prix(TarifInscription.gratuit()));
        assertEquals("8 €", sansInsecables(LibellesTableau.prix(new TarifInscription(8, 8))));
        assertEquals("8 € / 10 €", sansInsecables(LibellesTableau.prix(new TarifInscription(8, 10))));
        assertEquals("8,50 € en ligne", sansInsecables(LibellesTableau.prix(new TarifInscription(8.5, true, 0, false))));
        assertEquals("9 € sur place", sansInsecables(LibellesTableau.prix(new TarifInscription(0, false, 9, true))));
    }

    @Test
    void totaux_d_une_grille_chaque_place_d_une_plage_comptee() {
        GrilleRecompenses g = new GrilleRecompenses.Builder()
                .place(1, Recompense.argent(60))
                .place(2, Recompense.partInscriptions(12.5))
                .plage(3, 4, Recompense.argent(10))
                .plage(5, 8, Recompense.partInscriptions(5))
                .build();
        assertEquals(80.0, LibellesTableau.totalArgent(g));
        assertEquals(32.5, LibellesTableau.partInscriptions(g));
        assertEquals(0.0, LibellesTableau.totalArgent(null));
        assertEquals(0.0, LibellesTableau.partInscriptions(null));
    }

    @Test
    void resume_des_recompenses_dans_l_ordre_part_argent_lots_reductions() {
        assertEquals("—", LibellesTableau.recompenses(null));
        assertEquals("—", LibellesTableau.recompenses(GrilleRecompenses.vide()));
        GrilleRecompenses tout = new GrilleRecompenses.Builder()
                .place(1, Recompense.argent(80))
                .place(2, Recompense.partInscriptions(12.5))
                .place(3, Recompense.autre("Coupe"))
                .place(4, Recompense.reductionPourcentage(20))
                .build();
        assertEquals("12,5 % des inscriptions + 80 € + lots + réductions",
                sansInsecables(LibellesTableau.recompenses(tout)));
        assertEquals("Lots", LibellesTableau.recompenses(
                new GrilleRecompenses.Builder().place(1, Recompense.autre("Coupe")).build()));
        assertEquals("Réductions", LibellesTableau.recompenses(
                new GrilleRecompenses.Builder().place(1, Recompense.reductionPourcentage(20)).build()));
        assertEquals("100 % des inscriptions", LibellesTableau.recompenses(
                new GrilleRecompenses.Builder().place(1, Recompense.partInscriptions(100)).build()));
    }

    @Test
    void une_recompense_en_toutes_lettres() {
        assertEquals("60 €", sansInsecables(LibellesTableau.recompense(Recompense.argent(60))));
        assertEquals("Réduction de 20 %", LibellesTableau.recompense(Recompense.reductionPourcentage(20)));
        assertEquals("Réduction de 12.5 %", LibellesTableau.recompense(Recompense.reductionPourcentage(12.5)));
        assertEquals("12,5 % des inscr.", LibellesTableau.recompense(Recompense.partInscriptions(12.5)));
        assertEquals("Coupe", LibellesTableau.recompense(Recompense.autre("Coupe")));
    }

    @Test
    void pourcentage_entier_sans_decimale_sinon_virgule() {
        assertEquals("50 %", LibellesTableau.pourcent(50));
        assertEquals("12,5 %", LibellesTableau.pourcent(12.5));
    }
}
