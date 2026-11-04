package com.pingmanager.core;

import com.pingmanager.model.Joueur;
import com.pingmanager.model.Poule;
import com.pingmanager.model.Sexe;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GenerateurPoulesTest {

    private final GenerateurPoules generateur = new GenerateurPoules();

    private Joueur joueur(String licence, int points, String club) {
        return new Joueur(licence, "Nom" + licence, "Prenom" + licence,
                Sexe.HOMME, club, points);
    }

    private List<Joueur> nJoueurs(int n) {
        List<Joueur> liste = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            // points décroissants pour différencier les niveaux
            liste.add(joueur("L" + i, 2000 - i * 10, "Club" + (i % 3)));
        }
        return liste;
    }

    @Test
    void repartit_tous_les_joueurs_sans_perte_ni_doublon() {
        List<Joueur> joueurs = nJoueurs(16);
        List<Poule> poules = generateur.genererPoules(joueurs, 4, false);

        int total = poules.stream().mapToInt(Poule::getTaille).sum();
        assertEquals(16, total, "Tous les joueurs doivent être répartis.");

        // Aucun joueur ne doit apparaître deux fois.
        List<Joueur> vus = new ArrayList<>();
        for (Poule p : poules) {
            vus.addAll(p.getJoueurs());
        }
        assertEquals(16, vus.stream().distinct().count(), "Pas de doublon de joueur.");
    }

    @Test
    void seize_joueurs_donnent_quatre_poules_de_quatre() {
        List<Poule> poules = generateur.genererPoules(nJoueurs(16), 4, false);
        assertEquals(4, poules.size());
        assertTrue(poules.stream().allMatch(p -> p.getTaille() == 4));
    }

    @Test
    void aucune_poule_ne_descend_sous_trois_joueurs() {
        // 17 joueurs en poules de 4 : on ne doit jamais avoir de poule de 1 ou 2.
        for (int n = 6; n <= 40; n++) {
            List<Poule> poules = generateur.genererPoules(nJoueurs(n), 4, false);
            for (Poule p : poules) {
                assertTrue(p.getTaille() >= 3,
                        "n=" + n + " : poule " + p.getNom() + " a " + p.getTaille() + " joueurs.");
            }
        }
    }

    @Test
    void tailles_de_poules_sont_equilibrees() {
        // 17 joueurs : attendu 4 poules (4,4,4,5 ou 5,4,4,4) — écart max de 1.
        List<Poule> poules = generateur.genererPoules(nJoueurs(17), 4, false);
        int min = poules.stream().mapToInt(Poule::getTaille).min().orElseThrow();
        int max = poules.stream().mapToInt(Poule::getTaille).max().orElseThrow();
        assertTrue(max - min <= 1, "Les poules doivent être équilibrées (écart <= 1).");
    }

    @Test
    void repartition_serpentin_equilibre_la_force() {
        // Avec un serpentin, la somme des points par poule doit être proche.
        List<Poule> poules = generateur.genererPoules(nJoueurs(16), 4, false);
        List<Integer> sommes = poules.stream()
                .map(p -> p.getJoueurs().stream().mapToInt(Joueur::getPoints).sum())
                .toList();
        int min = sommes.stream().min(Integer::compareTo).orElseThrow();
        int max = sommes.stream().max(Integer::compareTo).orElseThrow();
        // L'écart doit rester modéré (bien inférieur à la force d'un joueur moyen * taille).
        assertTrue(max - min < 100,
                "Le serpentin doit équilibrer la force des poules (écart=" + (max - min) + ").");
    }

    @Test
    void separation_des_clubs_resout_un_conflit_evitable() {
        // 12 joueurs en 3 poules de 4. Le serpentin place les rangs 0 et 5 dans
        // la même poule (A) : en donnant le même club à ces deux joueurs, on crée
        // un conflit évitable que la séparation doit résoudre par échange.
        String[] clubs = new String[12];
        for (int i = 0; i < 12; i++) clubs[i] = "Club" + i;
        clubs[0] = "Dup";
        clubs[5] = "Dup";

        List<Joueur> joueurs = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            joueurs.add(joueur("L" + i, 2000 - i * 10, clubs[i]));
        }

        int conflitsSans = compterConflitsClub(
                generateur.genererPoules(new ArrayList<>(joueurs), 4, false));
        int conflitsAvec = compterConflitsClub(
                generateur.genererPoules(new ArrayList<>(joueurs), 4, true));

        assertEquals(1, conflitsSans, "Sans séparation, le serpentin crée bien 1 conflit.");
        assertEquals(0, conflitsAvec, "La séparation doit résoudre le conflit évitable.");
    }

    @Test
    void separation_ne_promet_pas_l_impossible() {
        // 2 clubs de 4 en 2 poules de 4 : au moins 2 joueurs d'un même club
        // sont forcément ensemble. La séparation ne doit pas planter et doit
        // laisser un résultat cohérent (tous les joueurs répartis).
        List<Joueur> joueurs = new ArrayList<>();
        for (int i = 0; i < 4; i++) joueurs.add(joueur("A" + i, 1500 - i, "Alpha"));
        for (int i = 0; i < 4; i++) joueurs.add(joueur("B" + i, 1400 - i, "Beta"));

        List<Poule> poules = generateur.genererPoules(joueurs, 4, true);
        int total = poules.stream().mapToInt(Poule::getTaille).sum();
        assertEquals(8, total, "Tous les joueurs restent répartis même si le conflit est inévitable.");
    }

    @Test
    void refuse_moins_de_trois_joueurs() {
        assertThrows(IllegalArgumentException.class,
                () -> generateur.genererPoules(nJoueurs(2), 4, false));
    }

    @Test
    void refuse_taille_souhaitee_inferieure_a_trois() {
        assertThrows(IllegalArgumentException.class,
                () -> generateur.genererPoules(nJoueurs(8), 2, false));
    }

    private int compterConflitsClub(List<Poule> poules) {
        int conflits = 0;
        for (Poule p : poules) {
            List<Joueur> js = p.getJoueurs();
            for (int i = 0; i < js.size(); i++) {
                for (int k = i + 1; k < js.size(); k++) {
                    if (!js.get(i).getClub().isBlank()
                            && js.get(i).getClub().equalsIgnoreCase(js.get(k).getClub())) {
                        conflits++;
                    }
                }
            }
        }
        return conflits;
    }
}
