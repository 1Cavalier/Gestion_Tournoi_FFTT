package com.pingmanager.core.reglement;

import com.pingmanager.model.DocumentReglement;

import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.FormatPhaseFinale;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.format.RegistreFormats;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Règlement généré : la troisième place de chaque tableau (petite finale, option
 * du tableau, ou 3es ex aequo). Clause d'organisateur : le chapitre IX ne
 * l'impose pas (décision de Quentin du 5 novembre 2026, QUESTIONS.md).
 */
class ReglementPetiteFinaleTest {

    private static Tableau tab(String nom, boolean petiteFinale) {
        return new Tableau(nom, ConfigurationTableau.depuisFormat(RegistreFormats.formatParDefaut())
                .petiteFinale(petiteFinale).build());
    }

    @Test
    void aucun_tableau_avec_petite_finale_troisiemes_ex_aequo() {
        assertEquals("Il n'y a pas de petite finale : les deux joueurs battus en demi-finale sont classés "
                + "troisièmes ex aequo.", GenerateurReglement.phraseTroisiemePlace(List.of(tab("A", false), tab("B", false))));
    }

    @Test
    void tous_les_tableaux_avec_petite_finale() {
        assertEquals("Une petite finale (match pour la troisième place) oppose les deux joueurs battus en "
                + "demi-finale dans chaque tableau.",
                GenerateurReglement.phraseTroisiemePlace(List.of(tab("A", true), tab("B", true))));
        assertEquals("Une petite finale (match pour la troisième place) oppose les deux joueurs battus en "
                + "demi-finale.", GenerateurReglement.phraseTroisiemePlace(List.of(tab("A", true))));
    }

    @Test
    void certains_tableaux_seulement_nommes_dans_le_reglement() {
        String phrase = GenerateurReglement.phraseTroisiemePlace(
                List.of(tab("A", true), tab("B", false), tab("C", true)));
        assertEquals("Une petite finale (match pour la troisième place) oppose les deux joueurs battus en "
                + "demi-finale dans les tableaux « A » et « C » ; dans les autres tableaux, les deux joueurs "
                + "battus en demi-finale sont classés troisièmes ex aequo.", phrase);
        assertTrue(GenerateurReglement.phraseTroisiemePlace(List.of(tab("Dames", true), tab("B", false)))
                .contains("dans le tableau « Dames » ;"));
    }

    @Test
    void sans_tableau_final_pas_de_phrase() {
        Tableau poulesSeules = new Tableau("A", ConfigurationTableau.depuisFormat(RegistreFormats.formatParDefaut())
                .formatPhaseFinale(FormatPhaseFinale.AUCUNE).petiteFinale(true).build());
        assertNull(GenerateurReglement.phraseTroisiemePlace(List.of(poulesSeules)));
        assertNull(GenerateurReglement.phraseTroisiemePlace(List.of()));
    }

    @Test
    void la_phrase_figure_dans_l_article_du_deroulement() {
        Tournoi t = GenerateurReglementTest.tournoiComplet();
        Tableau dames = t.getTableaux().get(1);
        Tableau avecPf = new Tableau(dames.getNom(), ConfigurationTableau.depuisFormat(RegistreFormats.formatParDefaut())
                .toutesSeries().placesMax(48).petiteFinale(true).build());
        avecPf.setHoraires(dames.getHoraires());
        avecPf.setType(dames.getType());
        t.retirerTableau(dames);
        t.ajouterTableau(avecPf);
        DocumentReglement d = new GenerateurReglement().generer(t);
        StringBuilder sb = new StringBuilder();
        for (DocumentReglement.Article a : d.articles()) {
            a.blocs().forEach(b -> sb.append(b.texteBrut()).append('\n'));
        }
        assertTrue(sb.toString().contains("dans le tableau « " + dames.getNom() + " » ; dans les autres tableaux"),
                sb.toString());
    }
}
