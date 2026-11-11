package com.pingmanager.ui.infra;

import com.pingmanager.model.DocumentReglement;
import com.pingmanager.model.Tournoi;
import com.pingmanager.core.reglement.GenerateurReglement;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Article « Matériel » : avec une salle d'échauffement, le juge-arbitre peut en
 * utiliser les tables comme tables de match en cas de nécessité (3.3.1.2.2).
 */
class TablesEchauffementReglementTest {

    private static final String CLAUSE = "En cas de nécessité, pour faire avancer le tournoi plus rapidement et "
            + "éviter des attentes trop longues, le juge-arbitre peut décider d'utiliser des tables de la salle "
            + "d'échauffement comme tables de match, pourvu qu'elles soient elles aussi conformes (3.3.1.2.2).";

    private static DocumentReglement.Article materiel(Tournoi t) {
        return new GenerateurReglement().generer(t).articles().stream()
                .filter(a -> a.titre().equals("Matériel")).findFirst().orElseThrow();
    }

    private static String texte(DocumentReglement.Article a) {
        StringBuilder sb = new StringBuilder();
        a.blocs().forEach(b -> sb.append(b.texteBrut()));
        return sb.toString();
    }

    @Test
    void avec_salle_d_echauffement_le_ja_peut_en_utiliser_les_tables_pour_des_matchs() {
        Tournoi t = TournoiDemoBrunoy.construire();
        DocumentReglement.Article a = materiel(t);
        assertEquals(12, a.numero(), "article 12 du règlement de Brunoy");
        String txt = texte(a);
        assertTrue(txt.contains("Une salle d'échauffement de 8 tables est à la disposition des joueurs. " + CLAUSE), txt);
    }

    @Test
    void sans_salle_d_echauffement_pas_de_clause() {
        Tournoi t = TournoiDemoBrunoy.construire();
        t.setTablesEchauffement(0);
        assertFalse(texte(materiel(t)).contains("tables de match"));
    }
}
