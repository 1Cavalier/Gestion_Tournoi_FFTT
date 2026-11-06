package com.pingmanager.core;

import com.pingmanager.model.Joueur;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.TypeConvocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Textes des convocations par SMS : un seul SMS GSM 7 bits, quels que soient les noms. */
class TexteConvocationTest {

    private static Joueur j(String licence, String nom, String prenom, String club) {
        return new Joueur(licence, nom, prenom, Sexe.FEMME, club, 1000);
    }

    private static final Joueur HUGO = j("1", "MORIN", "Hugo", "TT Brunoy");
    private static final Joueur LEA = j("2", "DURAND", "Léa", "US Évry");
    private static final Joueur PAUL = j("3", "MARTIN", "Paul", "CTT Évry");

    private static void unSeulSmsGsm(String texte) {
        assertTrue(TexteSms.estGsm7(texte), texte);
        assertTrue(TexteSms.longueur(texte) <= 160, texte.length() + " : " + texte);
        assertEquals(1, TexteSms.nombreDeSms(texte), texte);
    }

    @Test
    void convocation_match_de_poule() {
        String t = TexteConvocation.joueur(HUGO, LEA, 12, ContexteConvocation.poule("C", "B"));
        assertEquals("Hugo, vous etes attendu(e) table 12 contre Léa DURAND (US Évry) - Tableau C, poule B.", t);
        unSeulSmsGsm(t);
    }

    @Test
    void convocation_tour_de_tableau() {
        String t = TexteConvocation.joueur(LEA, HUGO, 3, ContexteConvocation.tour("Tableau Dames", "8es de finale"));
        assertEquals("Léa, vous etes attendu(e) table 3 contre Hugo MORIN (TT Brunoy) - Tableau Dames, 8es de finale.",
                t);
        unSeulSmsGsm(t);
    }

    @Test
    void convocation_match_de_classement() {
        String t = TexteConvocation.joueur(HUGO, LEA, 7, ContexteConvocation.classement("F", 17, 32));
        assertTrue(t.endsWith(" - Tableau F, match de classement (places 17 à 32)."), t);
        unSeulSmsGsm(t);
    }

    @Test
    void convocation_arbitrage() {
        String t = TexteConvocation.arbitre("Paul", HUGO, LEA, 4, ContexteConvocation.poule("C", "Poule B"));
        assertEquals("Paul, vous arbitrez la partie table 4 : Hugo MORIN contre Léa DURAND - Tableau C, poule B.", t);
        unSeulSmsGsm(t);
    }

    @Test
    void sans_contexte_ni_prenom() {
        String t = TexteConvocation.arbitre("", HUGO, LEA, 4, null);
        assertEquals("Bonjour, vous arbitrez la partie table 4 : Hugo MORIN contre Léa DURAND.", t);
    }

    @Test
    void jamais_de_stop_ni_de_caractere_unicode() {
        String t = TexteConvocation.joueur(j("4", "LŒUILLET", "Jérôme", "Club «Pongistes» de l’Île"),
                j("5", "GRÉGOIRE", "Maëlle", "Saint-Brévin – Côte"), 2, ContexteConvocation.tour("Élite", "Finale"));
        unSeulSmsGsm(t);
        assertFalse(t.toUpperCase().contains("STOP"));
        assertTrue(t.contains("contre Maelle GRÉGOIRE (Saint-Brévin - Cote)"), t);
        assertTrue(t.startsWith("Jérome, "), t);
    }

    @Test
    void club_retire_puis_noms_abreges_quand_le_texte_est_trop_long() {
        Joueur longClub = j("6", "DE LA FONTAINE-MARTIN", "Marie-Charlotte",
                "Association Sportive et Culturelle de Tennis de Table de la Vallée de l'Yerres");
        String t = TexteConvocation.joueur(HUGO, longClub, 12, ContexteConvocation.poule("C - Toutes séries", "B"));
        unSeulSmsGsm(t);
        assertFalse(t.contains("Association"), t);
        assertTrue(t.contains("Marie-Charlotte DE LA FONTAINE-MARTIN"), t);

        String tresLong = TexteConvocation.arbitre("Marie-Charlotte", longClub,
                j("7", "VAN DER BERGHE-LEFEBVRE DU PLESSIS", "Jean-Baptiste", ""), 112,
                ContexteConvocation.classement("Tableau Vétérans 3 et 4 hommes et dames", 17, 32));
        unSeulSmsGsm(tresLong);
        assertTrue(tresLong.contains("M. DE LA FONTAINE-MARTIN contre J. VAN DER BERGHE"), tresLong);
    }

    @Test
    void noms_hors_normes_coupes_a_160_caracteres() {
        Joueur geant = j("8", "X".repeat(150), "Y".repeat(40), "");
        String t = TexteConvocation.joueur(HUGO, geant, 1, ContexteConvocation.poule("C", "B"));
        unSeulSmsGsm(t);
        assertTrue(t.startsWith("Hugo, vous etes attendu(e) table 1 contre "), t);
        assertTrue(t.endsWith("..."), t);
    }

    @Test
    void contexte_poule_tour_classement() {
        assertEquals("Tableau C, poule B", ContexteConvocation.poule("C", "B").libelle());
        assertEquals("Tableau C, poule B", ContexteConvocation.poule("C", "Poule B").libelle());
        assertEquals("Tableau Dames, finale", ContexteConvocation.tour("Tableau Dames", "Finale").libelle());
        assertEquals(TypeConvocation.MATCH_CLASSEMENT, ContexteConvocation.classement("C", 9, 16).type());
        assertThrows(IllegalArgumentException.class, () -> ContexteConvocation.classement("C", 16, 9));
        assertThrows(IllegalArgumentException.class,
                () -> new ContexteConvocation(TypeConvocation.ARBITRAGE, "C", ""));
    }
}
