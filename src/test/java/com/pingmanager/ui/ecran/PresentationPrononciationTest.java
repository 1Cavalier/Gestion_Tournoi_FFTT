package com.pingmanager.ui.ecran;

import com.pingmanager.core.SyntheseVocale.ResultatSynthese;
import com.pingmanager.core.ValidationPrononciation;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.PrononciationJoueur;
import com.pingmanager.model.Sexe;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PresentationPrononciationTest {

    @Test
    void symboles_et_exemples_de_l_aide_acceptes_par_la_validation() {
        for (PresentationPrononciation.Symbole s : PresentationPrononciation.SYMBOLES) {
            String essai = s.symbole().equals(".") ? "a.a" : s.symbole();
            assertTrue(ValidationPrononciation.verifier(essai).valide(), s.symbole());
        }
        for (PresentationPrononciation.Exemple e : PresentationPrononciation.EXEMPLES) {
            assertTrue(ValidationPrononciation.verifier(e.ipa()).valide(), e.ipa());
        }
        assertEquals("ɑ̃ comme dans « enfant »", PresentationPrononciation.SYMBOLES.get(0).infobulle());
        assertEquals("Dupont → dy.pɔ̃", PresentationPrononciation.EXEMPLES.get(0).libelle());
    }

    @Test
    void insertion_au_curseur_ou_a_la_place_de_la_selection() {
        assertEquals(new PresentationPrononciation.Insertion("dyʁ", 3), PresentationPrononciation.inserer("dy", 2, 2, "ʁ"));
        assertEquals(new PresentationPrononciation.Insertion("dɔ̃t", 3),
                PresentationPrononciation.inserer("dot", 1, 2, "ɔ̃"));
        assertEquals(new PresentationPrononciation.Insertion("ʃ", 1), PresentationPrononciation.inserer(null, 5, 9, "ʃ"));
    }

    @Test
    void message_d_ecoute_repli_si_voix_indisponible() {
        assertEquals("", PresentationPrononciation.messageEcoute(ResultatSynthese.ok()));
        assertEquals(PresentationPrononciation.VOIX_INDISPONIBLE, PresentationPrononciation.messageEcoute(
                ResultatSynthese.echec("synthèse vocale indisponible sur cet ordinateur")));
        assertEquals("Écoute impossible : délai dépassé",
                PresentationPrononciation.messageEcoute(ResultatSynthese.echec("délai dépassé")));
    }

    @Test
    void resume_de_la_prononciation() {
        assertEquals("Non renseignée", PresentationPrononciation.resume(null));
        assertEquals("kɑ̃.tɛ̃ su.mɛ", PresentationPrononciation.resume(new PrononciationJoueur("1", "kɑ̃.tɛ̃", "su.mɛ")));
        assertEquals("su.mɛ", PresentationPrononciation.resume(new PrononciationJoueur("1", "", "su.mɛ")));
    }

    @Test
    void joueurs_du_tournoi_uniques_tries_et_filtres() {
        Joueur morin = new Joueur("1", "MORIN", "Hugo", Sexe.HOMME, "TT Brunoy", 1200);
        Joueur emery = new Joueur("2", "ÉMERY", "Léa", Sexe.FEMME, "US Évry", 1100);
        Joueur durand = new Joueur("3", "DURAND", "Paul", Sexe.HOMME, "TT Brunoy", 900);
        List<List<Joueur>> tableaux = List.of(List.of(morin, emery), List.of(durand, morin, Joueur.aDeterminer()));
        assertEquals(List.of(durand, emery, morin), PresentationPrononciation.joueurs(tableaux, ""));
        assertEquals(List.of(emery), PresentationPrononciation.joueurs(tableaux, "evry"));
        assertEquals(List.of(emery), PresentationPrononciation.joueurs(tableaux, "lea emery"));
        assertEquals(List.of(durand, morin), PresentationPrononciation.joueurs(tableaux, " brunoy "));
        assertEquals(List.of(morin), PresentationPrononciation.joueurs(tableaux, "1"));
    }
}
