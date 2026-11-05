package com.pingmanager.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Une rencontre entre deux joueurs (joueur A vs joueur B).
 *
 * <p>Le match agrège une liste de {@link Manche}. Le vainqueur est déterminé
 * par le nombre de manches gagnées, selon un format best-of défini au niveau
 * du tableau (ex. best-of 5 : le premier à 3 manches gagne).</p>
 *
 * <p>Un match peut être un "bye" (joueur B absent) : le joueur A est alors
 * qualifié d'office. Ce cas sert dans les tableaux à élimination lorsque le
 * nombre de joueurs n'est pas une puissance de 2.</p>
 */
public final class Match {

    private Joueur joueurA;
    private Joueur joueurB; // peut être null en cas de bye
    private final List<Manche> manches = new ArrayList<>();
    private StatutMatch statut;

    /** Numéro de table affectée (null tant que non affecté). */
    private Integer numeroTable;

    /**
     * Nombre de manches gagnantes de ce match (best-of). 3 signifie « le premier
     * à 3 manches gagne » (au meilleur des 5). Propre à chaque match afin de
     * permettre un format différent pour certains matchs (ex. finale, petite
     * finale) sans changer le reste de l'épreuve. Par défaut 3.
     */
    private int nombreManchesGagnantes = 3;

    /**
     * Dans un tableau à élimination, matchs dont les vainqueurs alimentent
     * respectivement la place A et la place B de ce match. {@code null} si la
     * place est occupée par un joueur d'emblée (exempté ou premier tour).
     */
    private Match sourceA;
    private Match sourceB;

    /** Issue du match (normale, forfait, abandon, refus, disqualification). */
    private IssueMatch issue = IssueMatch.NORMALE;

    /**
     * En cas d'issue non normale (forfait/abandon/refus/disqualification), le
     * joueur fautif qui marque 0 point. {@code null} pour une issue normale.
     */
    private Joueur joueurFautif;

    /**
     * Points de pénalité accordés en cours de match à cause de cartons
     * jaune+rouge : ces points s'ajoutent au score de l'adversaire du fautif.
     * Indexés par joueur pénalisé (le fautif) : la valeur est le nombre de
     * points offerts à l'adversaire.
     */
    private int pointsPenaliteContreA = 0; // points offerts à B parce que A a été pénalisé
    private int pointsPenaliteContreB = 0; // points offerts à A parce que B a été pénalisé

    public Match(Joueur joueurA, Joueur joueurB) {
        this.joueurA = Objects.requireNonNull(joueurA, "joueurA");
        this.joueurB = joueurB; // null autorisé (bye)
        this.statut = (joueurB == null) ? StatutMatch.TERMINE : StatutMatch.A_JOUER;
    }

    public Joueur getJoueurA() {
        return joueurA;
    }

    public Joueur getJoueurB() {
        return joueurB;
    }

    public boolean estBye() {
        return joueurB == null;
    }

    public List<Manche> getManches() {
        return Collections.unmodifiableList(manches);
    }


    /** Remplace tout le score par la liste fournie. */
    public void definirScore(List<Manche> nouvellesManches) {
        if (estBye()) {
            throw new IllegalStateException("Impossible de définir un score sur un bye.");
        }
        manches.clear();
        manches.addAll(Objects.requireNonNull(nouvellesManches, "nouvellesManches"));
        statut = StatutMatch.TERMINE;
        issue = IssueMatch.NORMALE;
        joueurFautif = null;
    }

    /**
     * Efface le résultat du match (correction d'une saisie erronée) : plus de
     * manches, issue normale, pas de fautif, et le match redevient à jouer.
     * Les pénalités et horodatages de lancement ne sont pas touchés ; l'heure de
     * fin est effacée.
     *
     * @throws IllegalStateException sur un bye (son résultat est automatique)
     */
    public void effacerResultat() {
        if (estBye()) {
            throw new IllegalStateException("Le résultat d'un bye ne peut pas être effacé.");
        }
        manches.clear();
        issue = IssueMatch.NORMALE;
        joueurFautif = null;
        statut = StatutMatch.A_JOUER;
        instantFin = null;
    }

    /**
     * Déclare une issue non normale (forfait, abandon, refus, disqualification)
     * en désignant le joueur fautif. Le vainqueur est automatiquement l'autre
     * joueur ; le fautif marquera 0 point au classement de poule (I.303).
     *
     * @param issue        l'issue (doit être différente de {@link IssueMatch#NORMALE})
     * @param joueurFautif le joueur absent / abandonnant / disqualifié
     */
    public void declarerIssue(IssueMatch issue, Joueur joueurFautif) {
        Objects.requireNonNull(issue, "issue");
        Objects.requireNonNull(joueurFautif, "joueurFautif");
        if (estBye()) {
            throw new IllegalStateException("Un bye n'a pas d'issue à déclarer.");
        }
        if (issue == IssueMatch.NORMALE) {
            throw new IllegalArgumentException("Utiliser definirScore pour une issue normale.");
        }
        if (!joueurFautif.equals(joueurA) && !joueurFautif.equals(joueurB)) {
            throw new IllegalArgumentException("Le joueur fautif doit être l'un des deux joueurs du match.");
        }
        this.issue = issue;
        this.joueurFautif = joueurFautif;
        this.statut = StatutMatch.TERMINE;
    }

    public IssueMatch getIssue() {
        return issue;
    }

    public Joueur getJoueurFautif() {
        return joueurFautif;
    }

    /**
     * Ajoute des points de pénalité en cours de match : à la suite d'un carton
     * jaune+rouge, l'arbitre accorde des points à l'adversaire du fautif. Ces
     * points s'ajoutent au score de manches courant lors du calcul du vainqueur.
     *
     * @param joueurFautifDuPoint le joueur pénalisé
     * @param points              nombre de points accordés à son adversaire (1 ou 2)
     */
    public void ajouterPointsPenalite(Joueur joueurFautifDuPoint, int points) {
        Objects.requireNonNull(joueurFautifDuPoint, "joueurFautifDuPoint");
        if (points < 0) {
            throw new IllegalArgumentException("Les points de pénalité ne peuvent pas être négatifs.");
        }
        if (joueurFautifDuPoint.equals(joueurA)) {
            pointsPenaliteContreA += points;
        } else if (joueurFautifDuPoint.equals(joueurB)) {
            pointsPenaliteContreB += points;
        } else {
            throw new IllegalArgumentException("Le joueur pénalisé doit être l'un des deux joueurs.");
        }
    }

    /** Points de pénalité accordés à B (car A a été pénalisé). */
    public int getPointsPenaliteAdverseDeA() {
        return pointsPenaliteContreA;
    }

    /** Points de pénalité accordés à A (car B a été pénalisé). */
    public int getPointsPenaliteAdverseDeB() {
        return pointsPenaliteContreB;
    }

    public int manchesGagneesParA() {
        return (int) manches.stream().filter(Manche::gagneeParA).count();
    }

    public int manchesGagneesParB() {
        return (int) manches.stream().filter(Manche::gagneeParB).count();
    }

    /**
     * @return le joueur vainqueur, ou {@code null} si le match n'est pas
     *         encore décidé (aucun joueur n'a atteint le nombre de manches
     *         gagnantes). En cas de bye, retourne le joueur A.
     */
    public Joueur getVainqueur() {
        if (estBye()) {
            return joueurA;
        }
        // Issue non normale (forfait, abandon, refus, disqualification) : le
        // joueur fautif perd, son adversaire est déclaré vainqueur.
        if (issue != IssueMatch.NORMALE && joueurFautif != null) {
            return joueurFautif.equals(joueurA) ? joueurB : joueurA;
        }
        int a = manchesGagneesParA();
        int b = manchesGagneesParB();
        // Un joueur n'est vainqueur qu'en atteignant le nombre de manches
        // gagnantes requis (best-of). Tant que ce seuil n'est pas atteint, le
        // match n'est pas décidé, même si un joueur mène.
        if (a >= nombreManchesGagnantes && a > b) {
            return joueurA;
        }
        if (b >= nombreManchesGagnantes && b > a) {
            return joueurB;
        }
        return null;
    }

    /**
     * @return le joueur perdant, ou {@code null} si non décidé / bye.
     */
    public Joueur getPerdant() {
        Joueur vainqueur = getVainqueur();
        if (vainqueur == null || estBye()) {
            return null;
        }
        return vainqueur.equals(joueurA) ? joueurB : joueurA;
    }

    public int getNombreManchesGagnantes() {
        return nombreManchesGagnantes;
    }

    public void setNombreManchesGagnantes(int nombreManchesGagnantes) {
        if (nombreManchesGagnantes < 1) {
            throw new IllegalArgumentException("Le best-of doit être d'au moins 1 manche gagnante.");
        }
        this.nombreManchesGagnantes = nombreManchesGagnantes;
    }

    public StatutMatch getStatut() {
        return statut;
    }

    public void setStatut(StatutMatch statut) {
        this.statut = Objects.requireNonNull(statut, "statut");
    }

    /**
     * Remplace les joueurs de ce match. Réservé à la construction/progression
     * des tableaux à élimination, où les emplacements « à déterminer » sont
     * renseignés une fois les vainqueurs connus. Réinitialise le score si le
     * match n'était pas encore joué.
     *
     * @param nouveauA nouveau joueur A (non null)
     * @param nouveauB nouveau joueur B (peut être null pour un bye)
     */
    public void remplacerJoueurs(Joueur nouveauA, Joueur nouveauB) {
        this.joueurA = Objects.requireNonNull(nouveauA, "nouveauA");
        this.joueurB = nouveauB;
        if (statut != StatutMatch.TERMINE) {
            manches.clear();
        }
    }

    public Integer getNumeroTable() {
        return numeroTable;
    }

    public void setNumeroTable(Integer numeroTable) {
        this.numeroTable = numeroTable;
    }

    /**
     * Horodatage du lancement du match sur une table (renseigné automatiquement
     * lors de l'affectation). {@code null} tant que le match n'a pas été lancé.
     */
    private java.time.LocalDateTime instantLancement;

    /** Horodatage de fin du match (renseigné à la saisie du score). */
    private java.time.LocalDateTime instantFin;

    public java.time.LocalDateTime getInstantLancement() {
        return instantLancement;
    }

    public void setInstantLancement(java.time.LocalDateTime instantLancement) {
        this.instantLancement = instantLancement;
    }

    public java.time.LocalDateTime getInstantFin() {
        return instantFin;
    }

    public void setInstantFin(java.time.LocalDateTime instantFin) {
        this.instantFin = instantFin;
    }

    public Match getSourceA() {
        return sourceA;
    }

    public void setSourceA(Match sourceA) {
        this.sourceA = sourceA;
    }

    public Match getSourceB() {
        return sourceB;
    }

    public void setSourceB(Match sourceB) {
        this.sourceB = sourceB;
    }

    @Override
    public String toString() {
        if (estBye()) {
            return joueurA.getNomComplet() + " (exempt)";
        }
        return joueurA.getNomComplet() + " vs " + joueurB.getNomComplet()
                + " [" + manchesGagneesParA() + "-" + manchesGagneesParB() + "]";
    }
}
