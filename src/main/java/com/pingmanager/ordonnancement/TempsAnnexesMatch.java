package com.pingmanager.ordonnancement;

import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Temps passés autour du jeu d'un match, pendant lesquels la table et les
 * joueurs sont occupés sans jouer (B025, simulation) :
 * <ul>
 *   <li><b>appel</b> : le temps d'appeler les joueurs et qu'ils arrivent ; un
 *       joueur n'entend pas toujours son nom : avec une certaine
 *       probabilité il faut le rappeler, ce qui ajoute un retard ;</li>
 *   <li><b>échauffement</b> : 3.4.3.1, deux minutes d'adaptation à la table
 *       avant le début de la partie ;</li>
 *   <li><b>pauses entre manches</b> : 3.4.4.1.1, au plus une minute entre deux
 *       manches successives (ici une durée moyenne réglable) ;</li>
 *   <li><b>temps morts</b> : 3.4.4.2, chaque joueur a droit à un temps mort d'une
 *       minute au plus par partie ; il le prend avec une certaine probabilité ;</li>
 *   <li><b>feuille</b> : rapporter la feuille de partie et saisir le résultat ;
 *       tant que ce n'est pas fait, la table n'est pas réattribuée et le
 *       vainqueur n'est pas connu.</li>
 * </ul>
 *
 * <p>Immuable, construit par {@link #builder()} ; {@link #PAR_DEFAUT} donne des
 * valeurs prudentes. Les temps sont en secondes.</p>
 */
public final class TempsAnnexesMatch {

    /** Valeurs par défaut : appel 1 min, 15 % de rappels (+3 min), échauffement 2 min,
     *  30 s de pause entre manches, 20 % de temps mort par joueur, feuille 1 min 30. */
    public static final TempsAnnexesMatch PAR_DEFAUT = builder().build();

    /** Aucun temps annexe (le match seul). */
    public static final TempsAnnexesMatch AUCUN = builder().appel(0).probabiliteRappel(0).rappel(0)
            .echauffement(0).pauseEntreManches(0).probabiliteTempsMort(0).feuille(0).build();

    /** 3.4.4.2 : un temps mort dure une minute au plus. */
    public static final int DUREE_TEMPS_MORT = 60;

    private final int appel;
    private final double probabiliteRappel;
    private final int rappel;
    private final int echauffement;
    private final int pauseEntreManches;
    private final double probabiliteTempsMort;
    private final int feuille;

    private TempsAnnexesMatch(Builder b) {
        this.appel = b.appel;
        this.probabiliteRappel = b.probabiliteRappel;
        this.rappel = b.rappel;
        this.echauffement = b.echauffement;
        this.pauseEntreManches = b.pauseEntreManches;
        this.probabiliteTempsMort = b.probabiliteTempsMort;
        this.feuille = b.feuille;
    }

    /** Temps annexes tirés pour un match. */
    public record Tirage(int appel, int echauffement, int pauses, int tempsMorts, int feuille) {
        /** Total avant le premier échange (appel + échauffement). */
        public int avantJeu() {
            return appel + echauffement;
        }

        /** Total, en secondes. */
        public int total() {
            return appel + echauffement + pauses + tempsMorts + feuille;
        }
    }

    /**
     * Tire les temps annexes d'un match.
     *
     * @param r              générateur (propre au match)
     * @param manchesJouees  nombre de manches jouées (pauses entre elles)
     */
    public Tirage tirer(Random r, int manchesJouees) {
        Objects.requireNonNull(r, "r");
        int a = appel;
        // Chaque joueur peut ne pas entendre l'appel : il faut le rappeler.
        for (int joueur = 0; joueur < 2; joueur++) {
            if (r.nextDouble() < probabiliteRappel) {
                a += (int) Math.round(rappel * (0.5 + r.nextDouble()));
            }
        }
        int pauses = 0;
        for (int i = 1; i < Math.max(1, manchesJouees); i++) {
            pauses += (int) Math.round(pauseEntreManches * 2 * r.nextDouble()); // moyenne réglée, au plus 2×
        }
        pauses = Math.min(pauses, Math.max(0, manchesJouees - 1) * 60); // 3.4.4.1.1 : une minute au plus
        int tm = 0;
        for (int joueur = 0; joueur < 2; joueur++) {
            if (r.nextDouble() < probabiliteTempsMort) {
                tm += (int) Math.round(DUREE_TEMPS_MORT * (0.5 + 0.5 * r.nextDouble()));
            }
        }
        int f = (int) Math.round(feuille * (0.5 + r.nextDouble()));
        return new Tirage(a, echauffement, pauses, tm, f);
    }

    /** Moyenne attendue des temps annexes d'un match de {@code manches} manches, en secondes. */
    public double moyenne(int manches) {
        double rappels = 2 * probabiliteRappel * rappel;
        double pauses = Math.max(0, manches - 1) * Math.min(60, pauseEntreManches);
        double tm = 2 * probabiliteTempsMort * DUREE_TEMPS_MORT * 0.75;
        return appel + rappels + echauffement + pauses + tm + feuille;
    }

    // ------------------------------------------------------------------
    // Présentation par groupes (B050)
    // ------------------------------------------------------------------

    /** Unité d'une variable de temps annexe. */
    public static final String UNITE_SECONDES = "s";
    /** Unité d'une probabilité (valeur de 0 à 1). */
    public static final String UNITE_PROBABILITE = "%";

    /**
     * Une variable réglable.
     *
     * @param cle     identifiant stable (« appel », « probabiliteRappel »…)
     * @param libelle libellé en clair
     * @param unite   {@link #UNITE_SECONDES} ou {@link #UNITE_PROBABILITE} (valeur de 0 à 1)
     * @param valeur  valeur réglée
     */
    public record Variable(String cle, String libelle, String unite, double valeur) {
    }

    /**
     * Un groupe nommé de variables, avec ce qu'il coûte en moyenne par match.
     *
     * @param nom             nom du groupe (« Avant le match »…)
     * @param variables       ses variables, dans l'ordre de présentation
     * @param moyenneSecondes temps moyen du groupe par match, en secondes
     */
    public record Groupe(String nom, List<Variable> variables, double moyenneSecondes) {
        public Groupe {
            variables = List.copyOf(variables);
        }
    }

    /**
     * Les temps autour du match, par groupes nommés (avant, pendant, après),
     * pour un match de {@code manches} manches. La somme des moyennes des
     * groupes vaut {@link #moyenne(int)}. Ajouter une variable : un champ, son
     * réglage dans le builder et une ligne dans le groupe concerné (ou un
     * nouveau groupe) ; l'appelant parcourt la liste sans la connaître.
     */
    public List<Groupe> groupes(int manches) {
        double avant = appel + 2 * probabiliteRappel * rappel + echauffement;
        double pendant = Math.max(0, manches - 1) * Math.min(60, pauseEntreManches)
                + 2 * probabiliteTempsMort * DUREE_TEMPS_MORT * 0.75;
        return List.of(
                new Groupe("Avant le match", List.of(
                        new Variable("appel", "Appel des joueurs", UNITE_SECONDES, appel),
                        new Variable("probabiliteRappel", "Joueur à rappeler", UNITE_PROBABILITE, probabiliteRappel),
                        new Variable("rappel", "Retard d'un rappel", UNITE_SECONDES, rappel),
                        new Variable("echauffement", "Échauffement", UNITE_SECONDES, echauffement)), avant),
                new Groupe("Pendant le match", List.of(
                        new Variable("pauseEntreManches", "Pause entre deux manches", UNITE_SECONDES,
                                pauseEntreManches),
                        new Variable("probabiliteTempsMort", "Temps mort pris, par joueur", UNITE_PROBABILITE,
                                probabiliteTempsMort)), pendant),
                new Groupe("Après le match", List.of(
                        new Variable("feuille", "Retour de la feuille", UNITE_SECONDES, feuille)), feuille));
    }

    /**
     * Copie où la variable {@code cle} (celle d'une {@link Variable} de
     * {@link #groupes}) prend la valeur donnée : secondes pour un temps, 0 à 1
     * pour une probabilité. Permet à l'appelant de régler toutes les variables
     * en parcourant les groupes, sans les connaître une à une.
     *
     * @throws IllegalArgumentException si la clé est inconnue ou la valeur refusée
     */
    public TempsAnnexesMatch avec(String cle, double valeur) {
        Objects.requireNonNull(cle, "cle");
        Builder b = builder().appel(appel).probabiliteRappel(probabiliteRappel).rappel(rappel)
                .echauffement(echauffement).pauseEntreManches(pauseEntreManches)
                .probabiliteTempsMort(probabiliteTempsMort).feuille(feuille);
        int secondes = (int) Math.round(valeur);
        if (cle.equals("appel")) {
            b.appel(secondes);
        } else if (cle.equals("probabiliteRappel")) {
            b.probabiliteRappel(valeur);
        } else if (cle.equals("rappel")) {
            b.rappel(secondes);
        } else if (cle.equals("echauffement")) {
            b.echauffement(secondes);
        } else if (cle.equals("pauseEntreManches")) {
            b.pauseEntreManches(secondes);
        } else if (cle.equals("probabiliteTempsMort")) {
            b.probabiliteTempsMort(valeur);
        } else if (cle.equals("feuille")) {
            b.feuille(secondes);
        } else {
            throw new IllegalArgumentException("Variable de temps autour du match inconnue : " + cle);
        }
        return b.build();
    }

    /** Total moyen par match de {@code manches} manches, en secondes (somme des groupes). */
    public double totalParMatch(int manches) {
        return moyenne(manches);
    }

    public int getAppel() {
        return appel;
    }

    public double getProbabiliteRappel() {
        return probabiliteRappel;
    }

    public int getRappel() {
        return rappel;
    }

    public int getEchauffement() {
        return echauffement;
    }

    public int getPauseEntreManches() {
        return pauseEntreManches;
    }

    public double getProbabiliteTempsMort() {
        return probabiliteTempsMort;
    }

    public int getFeuille() {
        return feuille;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Réglage pas à pas ; temps en secondes, probabilités de 0 à 1. */
    public static final class Builder {
        private int appel = 60;
        private double probabiliteRappel = 0.15;
        private int rappel = 180;
        private int echauffement = 120;
        private int pauseEntreManches = 30;
        private double probabiliteTempsMort = 0.2;
        private int feuille = 90;

        public Builder appel(int secondes) {
            this.appel = positif(secondes, "appel");
            return this;
        }

        public Builder probabiliteRappel(double p) {
            this.probabiliteRappel = probabilite(p, "rappel");
            return this;
        }

        public Builder rappel(int secondes) {
            this.rappel = positif(secondes, "rappel");
            return this;
        }

        public Builder echauffement(int secondes) {
            this.echauffement = positif(secondes, "échauffement");
            return this;
        }

        /** Pause moyenne entre deux manches (3.4.4.1.1 : 60 s au plus). */
        public Builder pauseEntreManches(int secondes) {
            positif(secondes, "pause");
            if (secondes > 60) {
                throw new IllegalArgumentException("La pause entre deux manches est d'une minute au plus (3.4.4.1.1).");
            }
            this.pauseEntreManches = secondes;
            return this;
        }

        public Builder probabiliteTempsMort(double p) {
            this.probabiliteTempsMort = probabilite(p, "temps mort");
            return this;
        }

        public Builder feuille(int secondes) {
            this.feuille = positif(secondes, "feuille");
            return this;
        }

        public TempsAnnexesMatch build() {
            return new TempsAnnexesMatch(this);
        }

        private static int positif(int v, String nom) {
            if (v < 0) {
                throw new IllegalArgumentException("Le temps de " + nom + " ne peut pas être négatif.");
            }
            return v;
        }

        private static double probabilite(double p, String nom) {
            if (p < 0 || p > 1) {
                throw new IllegalArgumentException("La probabilité de " + nom + " doit être entre 0 et 100 %.");
            }
            return p;
        }
    }
}
