package com.pingmanager.ui.infra;

import com.pingmanager.model.Club;
import com.pingmanager.model.FicheLicence;
import com.pingmanager.model.Sexe;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Joueurs <b>fictifs</b> de la démo (noms inventés, licences « 99… » qui n'existent pas
 * à la FFTT) et leurs clubs, pour remplir les tableaux et voir la prévisualisation du
 * remplissage. Génération déterministe (graine fixe) : mêmes joueurs à chaque démarrage.
 */
public final class JoueursFictifs {

    private JoueursFictifs() {
    }

    /** Clubs fictifs des joueurs : numéro, nom, département, ville, code postal. */
    private static final String[][] CLUBS = {
            {"08910101", "Ping Club de Montgeron", "91", "Montgeron", "91230"},
            {"08910102", "Yerres Tennis de Table", "91", "Yerres", "91330"},
            {"08910103", "AS Évry Ping", "91", "Évry-Courcouronnes", "91000"},
            {"08910104", "Draveil Ping", "91", "Draveil", "91210"},
            {"08940105", "Villeneuve Ping 94", "94", "Villeneuve-Saint-Georges", "94190"},
            {"08940106", "Créteil Ping", "94", "Créteil", "94000"},
            {"08770107", "Combs Sénart TT", "77", "Combs-la-Ville", "77380"},
            {"08770108", "Melun Val de Seine Ping", "77", "Melun", "77000"},
            {"08750109", "Paris 12 Ping", "75", "Paris", "75012"},
            {"08920110", "Boulogne Ping 92", "92", "Boulogne-Billancourt", "92100"},
            {"08780111", "Versailles Ping", "78", "Versailles", "78000"},
            {"08930112", "Montreuil TT", "93", "Montreuil", "93100"},
            {"08950113", "Cergy Ping", "95", "Cergy", "95000"},
            {"07450114", "Saran Ping", "45", "Saran", "45770"},
            {"07450115", "Orléans Loiret Ping", "45", "Orléans", "45000"},
            {"10690116", "Villeurbanne Ping", "69", "Villeurbanne", "69100"},
            {"09590117", "Lille Métropole Ping", "59", "Lille", "59000"},
            {"04350118", "Rennes Ping Loisirs", "35", "Rennes", "35000"},
    };

    /** Poids de chaque club (Île-de-France surtout, puisque la démo tourne autour de Brunoy). */
    private static final int[] POIDS = {9, 8, 8, 6, 6, 7, 5, 5, 6, 4, 4, 4, 3, 3, 3, 3, 3, 3};

    private static final String[] NOMS = {
            "MARCHAND", "LEBRUN", "FONTAINE", "CHEVALIER", "ROBIN", "MASSON", "SANCHEZ", "GERARD", "NGUYEN",
            "BOYER", "DENIS", "LEMAIRE", "DUFOUR", "BLANC", "GUERIN", "MULLER", "HENRY", "ROUSSEL", "NICOLAS",
            "PERRIN", "MORIN", "MATHIEU", "CLEMENT", "GAUTHIER", "DUMONT", "LOPEZ", "FERNANDEZ", "BRUNET",
            "SCHMITT", "COLIN", "VIDAL", "CARON", "PICARD", "ROGER", "LACROIX", "AUBERT", "HUBERT", "LEROUX",
            "RENAUD", "BARBIER", "ARNAUD", "MARTEL", "DA SILVA", "BENOIT", "REY", "LECLERC", "PAYET", "OLIVIER",
            "PHILIPPE", "COLLET", "CARPENTIER", "BOUVIER", "HOARAU", "TESSIER", "PREVOST", "JOLY", "BAILLY",
            "GAILLARD", "MAILLARD", "TRAN", "DIALLO", "KONATE", "BERTIN", "LAMBERT", "VASSEUR", "POULAIN",
    };

    private static final String[] PRENOMS_H = {
            "Lucas", "Hugo", "Louis", "Nathan", "Gabriel", "Arthur", "Jules", "Adam", "Raphaël", "Léo",
            "Tom", "Théo", "Maxime", "Antoine", "Alexandre", "Thomas", "Nicolas", "Julien", "Romain",
            "Mathis", "Baptiste", "Clément", "Pierre", "Olivier", "Stéphane", "Franck", "Laurent", "Karim",
            "Yanis", "Noah", "Sacha", "Ethan", "Éric", "Didier", "Bruno", "Mehdi",
    };

    private static final String[] PRENOMS_F = {
            "Emma", "Jade", "Louise", "Alice", "Chloé", "Lina", "Léa", "Manon", "Inès", "Camille", "Sarah",
            "Zoé", "Clara", "Juliette", "Anaïs", "Julie", "Pauline", "Céline", "Nathalie", "Sophie", "Laura",
            "Margaux", "Yasmine", "Mélanie", "Isabelle",
    };

    /** Tranches de points et nombre de joueurs par tranche (beaucoup de joueurs modestes, peu de forts). */
    private static final int[][] TRANCHES = {
            {500, 600, 95}, {601, 800, 70}, {801, 1000, 65}, {1001, 1300, 55},
            {1301, 1600, 40}, {1601, 2000, 30}, {2001, 2700, 20},
    };

    /** @return les clubs fictifs, pour la base clubs de démo. */
    public static List<Club> clubs() {
        List<Club> res = new ArrayList<>();
        for (String[] c : CLUBS) {
            Club club = new Club(c[0], c[1], c[2], c[3], "contact@" + c[3].toLowerCase()
                    .replaceAll("[^a-z]", "") + "-ping.fr");
            club.setCodePostal(c[4]);
            club.setAdresse1("Gymnase municipal");
            res.add(club);
        }
        return res;
    }

    /** @return environ 375 licenciés fictifs, toujours les mêmes. */
    public static List<FicheLicence> generer() {
        Random r = new Random(20261102L);
        List<FicheLicence> res = new ArrayList<>();
        int total = 0;
        for (int[] p : POIDS_CUMULES()) {
            total = p[0];
        }
        int numero = 9900001;
        java.util.Set<String> noms = new java.util.HashSet<>();
        for (int[] tranche : TRANCHES) {
            for (int k = 0; k < tranche[2]; k++) {
                boolean femme = r.nextInt(100) < 22;
                String nom;
                String prenom;
                do {
                    nom = NOMS[r.nextInt(NOMS.length)];
                    prenom = femme ? PRENOMS_F[r.nextInt(PRENOMS_F.length)] : PRENOMS_H[r.nextInt(PRENOMS_H.length)];
                } while (!noms.add(nom + " " + prenom));
                String[] club = CLUBS[clubAuHasard(r, total)];
                int points = tranche[0] + r.nextInt(tranche[1] - tranche[0] + 1);
                int age = ageAuHasard(r, points);
                LocalDate naissance = LocalDate.of(2026 - age, 1 + r.nextInt(12), 1 + r.nextInt(28));
                res.add(new FicheLicence.Builder(String.valueOf(numero++), nom, prenom)
                        .sexe(femme ? Sexe.FEMME : Sexe.HOMME)
                        .club(club[0], club[1])
                        .saison("2026-2027")
                        .points(points)
                        .pointsMensuels(points)
                        .classement(String.valueOf(Math.max(5, points / 100)))
                        .dateNaissance(naissance)
                        .typeLicence("Compétition")
                        .statut("Active")
                        .build());
            }
        }
        return Collections.unmodifiableList(res);
    }

    /** Âge au 1er janvier 2026 : quelques poussins et jeunes chez les plus faibles, vétérans partout. */
    private static int ageAuHasard(Random r, int points) {
        int t = r.nextInt(100);
        if (points < 650 && t < 4) {
            return 7 + r.nextInt(2);          // poussin (refusé à Brunoy, catégorie non admise)
        }
        if (t < 18) {
            return 11 + r.nextInt(8);         // minime à junior
        }
        if (t < 60) {
            return 19 + r.nextInt(21);        // senior
        }
        return 40 + r.nextInt(36);            // vétérans 40 à 75
    }

    private static int[][] POIDS_CUMULES() {
        int[][] res = new int[POIDS.length][1];
        int s = 0;
        for (int i = 0; i < POIDS.length; i++) {
            s += POIDS[i];
            res[i][0] = s;
        }
        return res;
    }

    private static int clubAuHasard(Random r, int total) {
        int x = r.nextInt(total);
        int[][] cumul = POIDS_CUMULES();
        for (int i = 0; i < cumul.length; i++) {
            if (x < cumul[i][0]) {
                return i;
            }
        }
        return 0;
    }
}
