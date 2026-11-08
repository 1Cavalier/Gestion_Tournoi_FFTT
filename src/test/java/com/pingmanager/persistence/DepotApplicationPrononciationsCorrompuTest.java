package com.pingmanager.persistence;

import com.pingmanager.model.PrononciationClub;
import com.pingmanager.model.PrononciationJoueur;
import com.pingmanager.persistence.json.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** B018 : fichier applicatif abîmé, la lecture ne perd que les entrées fautives. */
class DepotApplicationPrononciationsCorrompuTest {

    private static DepotApplication lire(String json) {
        return DepotApplication.lire(JsonParser.analyser(json));
    }

    @Test
    void doublons_dans_le_fichier_le_dernier_gagne() {
        DepotApplication d = lire("{\"version\":1,\"prononciations\":{\"joueurs\":["
                + "{\"licence\":\"1\",\"prenom\":\"a\",\"nom\":\"\"},{\"licence\":\"1\",\"prenom\":\"b\",\"nom\":\"\"}]}}");
        assertEquals(List.of(new PrononciationJoueur("1", "b", "")), d.prononciations().tousLesJoueurs());
    }

    @Test
    void sections_vides_ou_partielles() {
        assertTrue(lire("{\"version\":1,\"prononciations\":{}}").prononciations().estVide());
        assertEquals(1, lire("{\"version\":1,\"prononciations\":{\"clubs\":[{\"numero\":\"1\",\"ipa\":\"a\"}]}}")
                .prononciations().tousLesClubs().size());
    }

    @Test
    void champ_null_ou_de_mauvais_type_ne_bloque_pas_les_autres_entrees() {
        DepotApplication d = lire("{\"version\":1,\"prononciations\":{\"joueurs\":["
                + "{\"licence\":null,\"prenom\":\"a\"},{\"licence\":\"2\",\"prenom\":null,\"nom\":\"b\"},"
                + "{\"licence\":\"3\",\"prenom\":\"c\",\"nom\":\"d\"}],"
                + "\"clubs\":[{\"numero\":\"9\",\"ipa\":null},{\"numero\":\"8\",\"ipa\":\"e\"}]}}");
        assertTrue(d.prononciations().joueur("3").isPresent());
        assertEquals(List.of(new PrononciationClub("8", "", "e")), d.prononciations().tousLesClubs());
    }

    @Test
    void element_qui_n_est_pas_un_objet_ne_fait_pas_perdre_le_reste() {
        DepotApplication d = lire("{\"version\":1,\"prononciations\":{\"joueurs\":[\"texte\",12,null,"
                + "{\"licence\":\"3\",\"prenom\":\"c\",\"nom\":\"d\"}]}}");
        assertTrue(d.prononciations().joueur("3").isPresent());
    }

    @Test
    void prenom_invalide_n_efface_pas_les_autres_joueurs_ni_ne_garde_l_entree() {
        DepotApplication d = lire("{\"version\":1,\"prononciations\":{\"joueurs\":["
                + "{\"licence\":\"1\",\"prenom\":\"A\",\"nom\":\"ok\"},{\"licence\":\"2\",\"prenom\":\"b\",\"nom\":\"\"}]}}");
        assertTrue(d.prononciations().joueur("1").isEmpty());
        assertTrue(d.prononciations().joueur("2").isPresent());
    }

    @Test
    void section_prononciations_de_mauvais_type_ne_bloque_pas_le_demarrage() {
        DepotApplication d = lire("{\"version\":1,\"clubs\":[],\"prononciations\":\"abime\"}");
        assertTrue(d.prononciations().estVide());
        d = lire("{\"version\":1,\"prononciations\":{\"joueurs\":{\"a\":1},\"clubs\":7}}");
        assertTrue(d.prononciations().estVide());
    }
}
