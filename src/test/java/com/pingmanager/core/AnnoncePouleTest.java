package com.pingmanager.core;

import com.pingmanager.model.Joueur;
import com.pingmanager.model.Match;
import com.pingmanager.model.ModeAnnonce;
import com.pingmanager.model.Poule;
import com.pingmanager.model.Sexe;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Au lancement d'une poule entière : l'annonce appelle tous ses joueurs et la table. */
class AnnoncePouleTest {

    private static Poule poule() {
        Joueur a = new Joueur("1", "Dupont", "Pierre", Sexe.HOMME, "TT Brunoy", 1500);
        Joueur b = new Joueur("2", "MARTIN", "Paul", Sexe.HOMME, "US Melun", 1400);
        Joueur c = new Joueur("3", "Durand", "Jean", Sexe.HOMME, "", 1300);
        Poule p = new Poule("3", List.of(a, b, c));
        p.ajouterMatch(new Match(a, c));
        p.ajouterMatch(new Match(a, b));
        p.ajouterMatch(new Match(b, c));
        return p;
    }

    @Test
    void annonce_a_l_ecran_tableau_poule_table_et_tous_les_joueurs() {
        AnnonceRencontre a = AnnonceRencontre.preparerPoule(ModeAnnonce.APPEL, "B", poule(), 5);
        assertEquals("Tableau B, poule 3, table 5 : DUPONT, MARTIN, DURAND.", a.texte());
        assertEquals(5, a.table());
    }

    @Test
    void annonce_vocale_de_la_poule_avec_noms_lisibles_et_clubs() {
        AnnonceVocale v = TexteAnnonceVocale.composerPoule("Tableau B", poule(), 5);
        assertEquals("Tableau B, poule 3, table 5 : Pierre Dupont, club TT Brunoy ; Paul Martin, club US Melun "
                + "et Jean Durand.", v.texte());
        assertEquals(5, v.table());
    }

    @Test
    void sms_de_convocation_a_la_poule() {
        Joueur a = poule().getJoueurs().get(0);
        String s = TexteConvocation.poule(a, 5, ContexteConvocation.poule("B", "3"));
        assertEquals("Pierre, vous etes attendu(e) table 5 pour la poule 3 - Tableau B.", s);
        assertTrue(TexteSms.longueur(s) <= TexteSms.LONGUEUR_GSM7);
    }
}
