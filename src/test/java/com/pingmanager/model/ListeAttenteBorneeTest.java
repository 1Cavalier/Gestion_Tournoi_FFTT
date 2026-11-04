package com.pingmanager.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ListeAttenteBorneeTest {

    private Joueur j(String licence) {
        return new Joueur(licence, "N" + licence, "P" + licence, Sexe.HOMME, "Club", 1000);
    }

    private Tableau tableau(int placesMax, int tailleFile) {
        return new Tableau("A", ConfigurationTableau.builder()
                .placesMax(placesMax)
                .tailleMaxListeAttente(tailleFile)
                .build());
    }

    @Test
    void empile_en_file_tant_que_la_limite_n_est_pas_atteinte() {
        Tableau t = tableau(1, 2); // 1 place, file de 2
        assertEquals(Tableau.ResultatInscription.INSCRIT, t.inscrire(j("1")));
        assertEquals(Tableau.ResultatInscription.EN_FILE_ATTENTE, t.inscrire(j("2")));
        assertEquals(Tableau.ResultatInscription.EN_FILE_ATTENTE, t.inscrire(j("3")));
        assertEquals(2, t.getFileAttente().size());
    }

    @Test
    void refuse_quand_capacite_et_file_sont_pleines() {
        Tableau t = tableau(1, 2);
        t.inscrire(j("1")); // inscrit
        t.inscrire(j("2")); // file 1/2
        t.inscrire(j("3")); // file 2/2
        assertEquals(Tableau.ResultatInscription.LISTE_ATTENTE_PLEINE, t.inscrire(j("4")));
        assertEquals(1, t.getInscrits().size());
        assertEquals(2, t.getFileAttente().size());
    }

    @Test
    void file_desactivee_refuse_des_que_plein() {
        Tableau t = tableau(2, 0); // pas de liste d'attente
        assertEquals(Tableau.ResultatInscription.INSCRIT, t.inscrire(j("1")));
        assertEquals(Tableau.ResultatInscription.INSCRIT, t.inscrire(j("2")));
        assertEquals(Tableau.ResultatInscription.LISTE_ATTENTE_PLEINE, t.inscrire(j("3")));
        assertTrue(t.getFileAttente().isEmpty());
    }

    @Test
    void taille_de_file_par_defaut_est_quatre() {
        // Builder par défaut : placesMax illimité -> jamais plein, donc on force une capacité.
        ConfigurationTableau c = ConfigurationTableau.builder().placesMax(1).build();
        assertEquals(4, c.getTailleMaxListeAttente());
    }

    @Test
    void desinscription_promeut_le_premier_de_la_file() {
        Tableau t = tableau(1, 3);
        Joueur premier = j("1");
        Joueur enAttente = j("2");
        t.inscrire(premier);
        t.inscrire(enAttente);
        Joueur promu = t.desinscrire(premier);
        assertEquals(enAttente, promu);
        assertTrue(t.getInscrits().contains(enAttente));
        assertTrue(t.getFileAttente().isEmpty());
    }
}
