package com.pingmanager.core;

import com.pingmanager.model.Joueur;
import com.pingmanager.model.Match;
import com.pingmanager.model.Sexe;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** B017 : texte de l'annonce vocale d'une rencontre envoyée sur une table. */
class TexteAnnonceVocaleTest {

    private final Joueur hugo = new Joueur("1", "MORIN", "Hugo", Sexe.HOMME, "TT Brunoy", 1200);
    private final Joueur lea = new Joueur("2", "DURAND", "Léa", Sexe.FEMME, "US Évry", 1100);

    @Test
    void prenom_nom_club_contre_prenom_nom_club_table() {
        AnnonceVocale a = TexteAnnonceVocale.composer(new Match(hugo, lea), 4, null);
        assertEquals("Hugo Morin, club TT Brunoy, contre Léa Durand, club US Évry, table 4.", a.texte());
        assertEquals(4, a.table());
        assertFalse(a.rappel());
    }

    @Test
    void tableau_et_tour_en_option_au_debut() {
        AnnonceVocale poule = TexteAnnonceVocale.composer(new Match(hugo, lea), 7, ContexteConvocation.poule("C", "B"));
        assertEquals("Tableau C, poule B. Hugo Morin, club TT Brunoy, contre Léa Durand, club US Évry, table 7.",
                poule.texte());
        AnnonceVocale tour = TexteAnnonceVocale.composer(new Match(hugo, lea), 2,
                ContexteConvocation.tour("Tableau A", "Finale"));
        assertTrue(tour.texte().startsWith("Tableau A, finale. Hugo"), tour.texte());
    }

    @Test
    void segments_separent_prenom_nom_et_club_pour_la_prononciation() {
        AnnonceVocale a = TexteAnnonceVocale.composer(new Match(hugo, lea), 4, null);
        assertEquals(new SegmentAnnonce("Hugo", SegmentAnnonce.Nature.PRENOM), a.segments().get(0));
        assertEquals(new SegmentAnnonce("Morin", SegmentAnnonce.Nature.NOM), a.segments().get(2));
        assertEquals(new SegmentAnnonce("TT Brunoy", SegmentAnnonce.Nature.CLUB), a.segments().get(4));
    }

    @Test
    void noms_en_capitales_remis_en_casse_de_nom_propre() {
        assertEquals("De La Fontaine", TexteAnnonceVocale.nomPropre("DE LA FONTAINE"));
        assertEquals("Le-Gall", TexteAnnonceVocale.nomPropre("LE-GALL"));
        assertEquals("D'Artois", TexteAnnonceVocale.nomPropre("D'ARTOIS"));
        assertEquals("Émery", TexteAnnonceVocale.nomPropre("ÉMERY"));
        assertEquals("van Damme", TexteAnnonceVocale.nomPropre("van Damme"), "nom déjà en casse mixte gardé");
    }

    @Test
    void sans_club_ni_prenom_ou_club_deja_nomme_club() {
        Joueur sansClub = new Joueur("3", "PETIT", "", Sexe.HOMME, "  ", 500);
        Joueur clubNomme = new Joueur("4", "ROUX", "Max", Sexe.HOMME, "Club Pongiste de Melun", 500);
        AnnonceVocale a = TexteAnnonceVocale.composer(new Match(sansClub, clubNomme), 12, null);
        assertEquals("Petit, contre Max Roux, Club Pongiste de Melun, table 12.", a.texte());
    }

    @Test
    void rappel_precede_de_rappel_une_seule_fois() {
        AnnonceVocale a = TexteAnnonceVocale.composer(new Match(hugo, lea), 4, null).enRappel();
        assertTrue(a.rappel());
        assertTrue(a.texte().startsWith("Rappel : Hugo Morin"), a.texte());
        assertSame(a, a.enRappel());
    }
}
