package com.pingmanager.core;

import com.pingmanager.model.Joueur;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.format.FormatPoule3Qualifie2;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Scénarios de simulation pour les tests (B025). */
final class JeuDeSimulation {

    private JeuDeSimulation() {
    }

    static final LocalDate JOUR = LocalDate.of(2027, 4, 10);

    static Joueur joueur(String licence, int points) {
        return new Joueur(licence, "NOM" + licence, "Prenom" + licence, Sexe.HOMME, "Club" + (licence.hashCode() % 20), points);
    }

    static List<Joueur> joueurs(String prefixe, int nombre, int pointsMin, int pointsMax, long graine) {
        Random r = new Random(graine);
        List<Joueur> js = new ArrayList<>();
        for (int i = 1; i <= nombre; i++) {
            js.add(joueur(prefixe + i, pointsMin + r.nextInt(pointsMax - pointsMin + 1)));
        }
        return js;
    }

    static ScenarioSimulation.TableauSimule tableau(String nom, LocalTime debut, List<Joueur> joueurs) {
        return new ScenarioSimulation.TableauSimule(nom, JOUR.atTime(debut), joueurs,
                FormatPoule3Qualifie2.INSTANCE, false, null, 0);
    }

    /**
     * Petit tournoi d'une journée : trois tableaux qui commencent à 9 h, 10 h et
     * 11 h, avec des joueurs inscrits dans deux tableaux.
     */
    static ScenarioSimulation petitTournoi(int tables, long graine) {
        List<Joueur> a = joueurs("A", 24, 500, 1200, 1);
        List<Joueur> b = new ArrayList<>(joueurs("B", 18, 800, 1500, 2));
        b.addAll(a.subList(0, 6)); // six joueurs du A jouent aussi le B
        List<Joueur> c = joueurs("C", 21, 1000, 2500, 3);
        return ScenarioSimulation.builder().nombreDeTables(tables).graine(graine)
                .tableau(tableau("A", LocalTime.of(9, 0), a))
                .tableau(tableau("B", LocalTime.of(10, 0), b))
                .tableau(tableau("C", LocalTime.of(11, 0), c))
                .build();
    }

    /**
     * Grand tournoi de deux jours : 400 joueurs, 15 tableaux, dont un Open au
     * tableau final de 128 places ; chaque joueur s'inscrit dans 1 à 3 tableaux.
     */
    static ScenarioSimulation grandTournoi(int tables, long graine) {
        Random r = new Random(graine);
        List<Joueur> tous = new ArrayList<>();
        for (int i = 1; i <= 400; i++) {
            // Beaucoup de joueurs modestes, peu de très bons (répartition réaliste).
            tous.add(joueur("L" + i, 500 + (int) (2500 * Math.pow(r.nextDouble(), 1.6))));
        }
        int[] maxPoints = {799, 999, 1199, 1399, 1599, 1799, 1999, 2299, 0, 899, 1099, 1299, 1499, 1699, 0};
        String[] noms = {"A ≤ 799", "B ≤ 999", "C ≤ 1199", "D ≤ 1399", "E ≤ 1599", "F ≤ 1799", "G ≤ 1999",
                "H ≤ 2299", "I Open", "J ≤ 899", "K ≤ 1099", "L ≤ 1299", "M ≤ 1499", "N ≤ 1699", "O Open"};
        LocalTime[] heures = {LocalTime.of(8, 30), LocalTime.of(9, 0), LocalTime.of(9, 30), LocalTime.of(10, 30),
                LocalTime.of(11, 30), LocalTime.of(13, 0), LocalTime.of(14, 0), LocalTime.of(15, 0)};
        List<List<Joueur>> inscrits = new ArrayList<>();
        for (int t = 0; t < 15; t++) {
            inscrits.add(new ArrayList<>());
        }
        for (Joueur j : tous) {
            // L'Open du samedi attire la moitié des joueurs : tableau final de 128.
            if (r.nextInt(2) == 0) {
                inscrits.get(8).add(j);
            }
            int voulus = 1 + r.nextInt(3);
            for (int essai = 0; essai < 40 && voulus > 0; essai++) {
                int t = r.nextInt(15);
                boolean admis = maxPoints[t] == 0 || j.getPoints() <= maxPoints[t];
                if (t != 8 && admis && !inscrits.get(t).contains(j)) {
                    inscrits.get(t).add(j);
                    voulus--;
                }
            }
            if (inscrits.stream().noneMatch(l -> l.contains(j))) {
                inscrits.get(14).add(j); // chacun joue au moins un tableau : l'Open du dimanche
            }
        }
        ScenarioSimulation.Builder b = ScenarioSimulation.builder().nombreDeTables(tables).graine(graine);
        for (int t = 0; t < 15; t++) {
            LocalDate jour = t < 9 ? JOUR : JOUR.plusDays(1);
            LocalTime heure = t < 9 ? heures[Math.min(t, 7)] : heures[t - 9];
            LocalDateTime debut = jour.atTime(heure);
            b.tableau(new ScenarioSimulation.TableauSimule(noms[t], debut, inscrits.get(t),
                    FormatPoule3Qualifie2.INSTANCE, t == 8 || t == 14, null, maxPoints[t]));
        }
        return b.build();
    }
}
