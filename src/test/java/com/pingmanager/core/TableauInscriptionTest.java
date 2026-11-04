package com.pingmanager.core;

import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tableau.ResultatInscription;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TableauInscriptionTest {

    private Joueur joueur(String licence, int points, Sexe sexe) {
        return new Joueur(licence, "N" + licence, "P" + licence, sexe, "Club", points);
    }

    @Test
    void inscrit_jusqu_a_la_limite_puis_met_en_file_attente() {
        ConfigurationTableau config = ConfigurationTableau.builder()
                .placesMax(2)
                .build();
        Tableau tableau = new Tableau("A", config);

        assertEquals(ResultatInscription.INSCRIT, tableau.inscrire(joueur("1", 1000, Sexe.HOMME)));
        assertEquals(ResultatInscription.INSCRIT, tableau.inscrire(joueur("2", 1000, Sexe.HOMME)));
        assertEquals(ResultatInscription.EN_FILE_ATTENTE, tableau.inscrire(joueur("3", 1000, Sexe.HOMME)));

        assertEquals(2, tableau.getInscrits().size());
        assertEquals(1, tableau.getFileAttente().size());
    }

    @Test
    void desinscription_promeut_le_premier_en_file_attente() {
        ConfigurationTableau config = ConfigurationTableau.builder().placesMax(1).build();
        Tableau tableau = new Tableau("A", config);

        Joueur premier = joueur("1", 1000, Sexe.HOMME);
        Joueur enAttente = joueur("2", 1000, Sexe.HOMME);
        tableau.inscrire(premier);
        tableau.inscrire(enAttente);

        Joueur promu = tableau.desinscrire(premier);
        assertEquals(enAttente, promu);
        assertTrue(tableau.getInscrits().contains(enAttente));
        assertTrue(tableau.getFileAttente().isEmpty());
    }

    @Test
    void refuse_un_joueur_hors_plage_de_classement() {
        ConfigurationTableau config = ConfigurationTableau.builder()
                .pointsMax(1200)
                .build();
        Tableau tableau = new Tableau("Moins de 1200", config);

        assertEquals(ResultatInscription.NON_ELIGIBLE,
                tableau.inscrire(joueur("1", 1500, Sexe.HOMME)));
        assertEquals(ResultatInscription.INSCRIT,
                tableau.inscrire(joueur("2", 1100, Sexe.HOMME)));
    }

    @Test
    void refuse_le_mauvais_sexe_pour_un_tableau_reserve() {
        ConfigurationTableau config = ConfigurationTableau.builder()
                .sexeRequis(Sexe.FEMME)
                .build();
        Tableau tableau = new Tableau("Dames", config);

        assertEquals(ResultatInscription.NON_ELIGIBLE,
                tableau.inscrire(joueur("1", 1000, Sexe.HOMME)));
        assertEquals(ResultatInscription.INSCRIT,
                tableau.inscrire(joueur("2", 1000, Sexe.FEMME)));
    }

    @Test
    void ne_reinscrit_pas_un_joueur_deja_present() {
        Tableau tableau = new Tableau("A", ConfigurationTableau.builder().build());
        Joueur j = joueur("1", 1000, Sexe.HOMME);
        assertEquals(ResultatInscription.INSCRIT, tableau.inscrire(j));
        assertEquals(ResultatInscription.DEJA_PRESENT, tableau.inscrire(j));
    }
}
