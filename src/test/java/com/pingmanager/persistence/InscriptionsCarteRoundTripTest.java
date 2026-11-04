package com.pingmanager.persistence;

import com.pingmanager.model.AdresseTournoi;
import com.pingmanager.model.CategorieAge;
import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.Coordonnees;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/** Coordonnées du lieu et catégorie d'âge des inscrits : relus à l'identique, anciens fichiers lisibles. */
class InscriptionsCarteRoundTripTest {

    @Test
    void coordonnees_et_categorie_relues(@TempDir Path dir) {
        Tournoi t = new Tournoi("T", "Club", LocalDate.of(2027, 4, 10));
        t.setAdresse(AdresseTournoi.de("157 Route de Brie", "91800", "Brunoy")
                .avecCoordonnees(new Coordonnees(48.6951, 2.5031)));
        Tableau a = new Tableau("A", ConfigurationTableau.depuisFormat(com.pingmanager.model.format.RegistreFormats.formatParDefaut()).placesMax(4).build());
        t.ajouterTableau(a);
        Joueur j = new Joueur("1", "N", "P", Sexe.FEMME, "C", 1200);
        j.setCategorie(CategorieAge.VETERAN_45);
        a.inscrire(j);
        Path f = dir.resolve("t" + DepotTournoi.EXTENSION);
        DepotTournoi.sauvegarder(t, f);

        Tournoi relu = DepotTournoi.charger(f).getTournoi();
        assertEquals(new Coordonnees(48.6951, 2.5031), relu.getAdresse().getCoordonnees());
        assertEquals(CategorieAge.VETERAN_45, relu.getTableaux().get(0).getInscrits().get(0).getCategorie());
    }

    @Test
    void adresse_sans_coordonnees_reste_sans() {
        AdresseTournoi a = ConfigMapper.lireAdresse(ConfigMapper.ecrireAdresse(AdresseTournoi.de("x", "91800", "Brunoy")));
        assertNull(a.getCoordonnees());
    }

    @Test
    void mode_de_preparation_relu_et_complet_par_defaut(@TempDir Path dir) {
        Tournoi t = new Tournoi("T", "Club", LocalDate.of(2027, 4, 10));
        assertEquals(com.pingmanager.model.ModePreparation.COMPLETE, t.getModePreparation());
        t.setModePreparation(com.pingmanager.model.ModePreparation.IMPORT);
        Path f = dir.resolve("m" + DepotTournoi.EXTENSION);
        DepotTournoi.sauvegarder(t, f);
        assertEquals(com.pingmanager.model.ModePreparation.IMPORT, DepotTournoi.charger(f).getTournoi().getModePreparation());
        t.setModePreparation(com.pingmanager.model.ModePreparation.A_CHOISIR);
        DepotTournoi.sauvegarder(t, f);
        assertEquals(com.pingmanager.model.ModePreparation.A_CHOISIR, DepotTournoi.charger(f).getTournoi().getModePreparation());
    }
}
