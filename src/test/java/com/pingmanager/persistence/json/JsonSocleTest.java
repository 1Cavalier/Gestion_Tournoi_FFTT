package com.pingmanager.persistence.json;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Vérifie le socle JSON sans dépendance : écriture, relecture, échappements,
 * nombres, et robustesse du parseur face à un texte mal formé.
 */
class JsonSocleTest {

    @Test
    void aller_retour_objet_complexe() {
        Json o = Json.objet()
                .set("nom", "Tournoi \"Open\"\nNantes")
                .set("tables", 8)
                .set("dotation", 1500.5)
                .set("publie", true)
                .set("vide", Json.nul());
        Json tableau = Json.tableau().add(Json.of(1)).add(Json.of(2)).add(Json.of("trois"));
        o.set("liste", tableau);

        String texte = JsonWriter.ecrire(o, true);
        Json relu = JsonParser.analyser(texte);

        assertEquals("Tournoi \"Open\"\nNantes", relu.get("nom").asString());
        assertEquals(8, relu.get("tables").asInt());
        assertEquals(1500.5, relu.get("dotation").asDouble(), 0.0001);
        assertTrue(relu.get("publie").asBoolean());
        assertTrue(relu.get("vide").estNul());
        assertEquals(3, relu.get("liste").taille());
        assertEquals("trois", relu.get("liste").elements().get(2).asString());
    }

    @Test
    void entier_ecrit_sans_partie_decimale() {
        assertEquals("42", JsonWriter.ecrire(Json.of(42), false));
        assertEquals("-7", JsonWriter.ecrire(Json.of(-7L), false));
    }

    @Test
    void champs_optionnels_avec_valeur_par_defaut() {
        Json o = Json.objet().set("present", 5);
        assertEquals(5, o.getInt("present", 99));
        assertEquals(99, o.getInt("absent", 99));
        assertFalse(o.has("absent"));
    }

    @Test
    void sequence_unicode_echappee() {
        Json relu = JsonParser.analyser("{\"c\":\"\\u00e9t\\u00e9\"}");
        assertEquals("été", relu.get("c").asString());
    }

    @Test
    void texte_malforme_leve_une_exception() {
        assertThrows(JsonException.class, () -> JsonParser.analyser("{\"a\": }"));
        assertThrows(JsonException.class, () -> JsonParser.analyser("[1, 2"));
        assertThrows(JsonException.class, () -> JsonParser.analyser("{\"a\": 1 \"b\": 2}"));
    }
}
