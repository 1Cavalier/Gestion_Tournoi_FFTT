package com.pingmanager.persistence;

import com.pingmanager.model.PrononciationClub;
import com.pingmanager.model.PrononciationJoueur;
import com.pingmanager.persistence.json.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** B018 : registre des prononciations dans les données de l'application. */
class DepotApplicationPrononciationsTest {

    @TempDir
    Path dossier;

    @Test
    void aller_retour_des_prononciations_joueurs_et_clubs() {
        DepotApplication depot = new DepotApplication();
        depot.prononciations().definirJoueur("9412345", "ʒɑ̃", "dy.pɔ̃");
        depot.prononciations().definirJoueur("9400001", "", "ŋɡu.jɛn");
        depot.prononciations().definirClub("08910001", "TT Brunoy", "te.te bʁy.nwa");
        Path fichier = dossier.resolve(DepotApplication.NOM_FICHIER);
        depot.sauvegarder(fichier);

        DepotApplication relu = DepotApplication.charger(fichier);
        assertEquals(List.of(new PrononciationJoueur("9412345", "ʒɑ̃", "dy.pɔ̃"),
                new PrononciationJoueur("9400001", "", "ŋɡu.jɛn")), relu.prononciations().tousLesJoueurs());
        assertEquals(List.of(new PrononciationClub("08910001", "TT Brunoy", "te.te bʁy.nwa")),
                relu.prononciations().tousLesClubs());
    }

    @Test
    void ancien_fichier_sans_registre_relu_avec_registre_vide() throws Exception {
        Path fichier = dossier.resolve(DepotApplication.NOM_FICHIER);
        Files.writeString(fichier, "{\"version\":1,\"kind\":\"application\",\"clubs\":[],\"comptes\":[],"
                + "\"acces\":[],\"recents\":[]}", StandardCharsets.UTF_8);
        DepotApplication relu = DepotApplication.charger(fichier);
        assertTrue(relu.prononciations().estVide());
        relu.sauvegarder(fichier);
        assertTrue(DepotApplication.charger(fichier).prononciations().estVide());
    }

    @Test
    void entree_invalide_modifiee_a_la_main_ignoree_sans_bloquer_la_lecture() {
        DepotApplication relu = DepotApplication.lire(JsonParser.analyser("{\"version\":1,\"prononciations\":{"
                + "\"joueurs\":[{\"licence\":\"1\",\"prenom\":\"ti\",\"nom\":\"a\\\"/><x\"},"
                + "{\"licence\":\"2\",\"prenom\":\"le.a\",\"nom\":\"\"},{\"prenom\":\"sans\"}],"
                + "\"clubs\":[{\"numero\":\"08910001\",\"ipa\":\"<break/>\"},{\"numero\":\"08910002\",\"ipa\":\"e.vʁi\"}]}}"));
        assertEquals(List.of(new PrononciationJoueur("2", "le.a", "")), relu.prononciations().tousLesJoueurs());
        assertEquals(List.of(new PrononciationClub("08910002", "", "e.vʁi")), relu.prononciations().tousLesClubs());
    }
}
