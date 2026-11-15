package com.pingmanager.core;

import com.pingmanager.model.ConditionsMaintien;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.StatutTournoi;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * B051 : préparation de la mise en place du jour J (étape « J-7 »).
 *
 * <p>Deux questions, sans rien modifier au tournoi :</p>
 * <ul>
 *   <li>{@link #acces} : l'étape est-elle proposée (tournoi homologué ou publié)
 *       et ouverte (à partir de J-7, règle de {@link ServiceTournoiEnCours} ;
 *       toujours pour un tournoi de démonstration) ?</li>
 *   <li>{@link #alertes} : que reste-t-il à régler avant le jour J ?</li>
 * </ul>
 *
 * <p>Aucune alerte ne repose sur une règle fédérale : ce sont des oublis de
 * saisie (horaire, juge-arbitre principal, club, points) ou des clauses de
 * l'organisateur. Le seuil de maintien d'un tableau vient de
 * {@link ConditionsMaintien#getMinimumInscrits()} : s'il n'est pas défini (0),
 * l'alerte n'existe pas.</p>
 */
public final class ServicePreparationJourJ {

    /** Ce que l'organisateur doit faire d'une alerte. */
    public enum Gravite {
        /** Oubli ou manque à corriger avant le jour J. */
        A_TRAITER,
        /** Situation normale, à avoir en tête. */
        A_SAVOIR
    }

    /** Nature d'une alerte. */
    public enum TypeAlerte {
        JUGE_ARBITRE_NON_DESIGNE(Gravite.A_TRAITER),
        TABLEAU_SANS_HORAIRE(Gravite.A_TRAITER),
        TABLEAU_SOUS_SEUIL(Gravite.A_TRAITER),
        JOUEUR_SANS_CLUB(Gravite.A_TRAITER),
        JOUEUR_SANS_POINTS(Gravite.A_TRAITER),
        LISTE_ATTENTE_NON_VIDE(Gravite.A_SAVOIR),
        TABLEAU_COMPLET(Gravite.A_SAVOIR);

        private final Gravite gravite;

        TypeAlerte(Gravite gravite) {
            this.gravite = gravite;
        }

        public Gravite getGravite() {
            return gravite;
        }
    }

    /**
     * Une alerte de préparation.
     *
     * @param type     nature
     * @param tableaux tableaux concernés (un seul pour une alerte de tableau, ceux
     *                 du joueur pour une alerte de joueur, vide pour le juge-arbitre)
     * @param joueur   joueur concerné, ou {@code null}
     * @param nombre   inscrits (sous le seuil, complet) ou joueurs en attente ; 0 sinon
     * @param seuil    minimum d'inscrits (sous le seuil) ou places (complet) ; 0 sinon
     */
    public record Alerte(TypeAlerte type, List<String> tableaux, Joueur joueur, int nombre, int seuil) {
        public Alerte {
            Objects.requireNonNull(type, "type");
            tableaux = List.copyOf(tableaux);
        }

        /** @return le tableau concerné (le premier), ou une chaîne vide. */
        public String tableau() {
            return tableaux.isEmpty() ? "" : tableaux.get(0);
        }
    }

    /**
     * Accès à l'étape de préparation.
     *
     * @param proposee          vrai si la carte est montrée (homologué ou publié)
     * @param ouverte           vrai si l'étape s'ouvre (à partir de J-7, ou démo)
     * @param ouverture         J-7 : premier jour d'ouverture
     * @param joursAvantPremierJour jours d'ici le premier jour (négatif ensuite)
     * @param demo              vrai si l'étape n'est ouverte que parce que le tournoi est de démonstration
     */
    public record Acces(boolean proposee, boolean ouverte, LocalDate ouverture, long joursAvantPremierJour,
                        boolean demo) {
    }

    private final ServiceTournoiEnCours calendrier;

    public ServicePreparationJourJ(ServiceTournoiEnCours calendrier) {
        this.calendrier = Objects.requireNonNull(calendrier, "calendrier");
    }

    /**
     * @param tournoiDeDemo vrai pour un tournoi de démonstration : l'étape est
     *                      toujours ouverte (jamais pour un statut non proposé)
     */
    public Acces acces(Tournoi t, boolean tournoiDeDemo) {
        Objects.requireNonNull(t, "tournoi");
        boolean proposee = t.getStatut() == StatutTournoi.HOMOLOGUE || t.getStatut() == StatutTournoi.PUBLIE;
        LocalDate ouverture = ServiceTournoiEnCours.debutPreparation(t);
        boolean aLaDate = !calendrier.aujourdhui().isBefore(ouverture);
        return new Acces(proposee, proposee && (aLaDate || tournoiDeDemo), ouverture,
                calendrier.joursAvantPremierJour(t), proposee && !aLaDate && tournoiDeDemo);
    }

    /**
     * Alertes de préparation : le juge-arbitre d'abord, puis tableau par tableau
     * (ordre du tournoi : sans horaire, sous le seuil, complet, liste d'attente),
     * puis joueur par joueur (sans club, sans points), dans l'ordre d'inscription.
     */
    public static List<Alerte> alertes(Tournoi t) {
        Objects.requireNonNull(t, "tournoi");
        List<Alerte> res = new ArrayList<>();
        if (t.getJugeArbitrePrincipal().isEmpty()) {
            res.add(new Alerte(TypeAlerte.JUGE_ARBITRE_NON_DESIGNE, List.of(), null, 0, 0));
        }
        ConditionsMaintien maintien = t.getConditionsMaintien();
        int minimum = maintien == null ? 0 : maintien.getMinimumInscrits();
        Map<Joueur, List<String>> tableauxParJoueur = new LinkedHashMap<>();
        for (Tableau tab : t.getTableaux()) {
            List<String> nom = List.of(tab.getNom());
            int inscrits = tab.getInscrits().size();
            if (tab.getHoraires() == null || tab.getHoraires().getDebutMatchs() == null) {
                res.add(new Alerte(TypeAlerte.TABLEAU_SANS_HORAIRE, nom, null, 0, 0));
            }
            // Clause de l'organisateur : pas de seuil défini, pas d'alerte.
            if (minimum > 0 && inscrits < minimum) {
                res.add(new Alerte(TypeAlerte.TABLEAU_SOUS_SEUIL, nom, null, inscrits, minimum));
            }
            if (tab.estPlein()) {
                res.add(new Alerte(TypeAlerte.TABLEAU_COMPLET, nom, null, inscrits,
                        tab.getConfiguration().getPlacesMax()));
            }
            if (!tab.getFileAttente().isEmpty()) {
                res.add(new Alerte(TypeAlerte.LISTE_ATTENTE_NON_VIDE, nom, null, tab.getFileAttente().size(), 0));
            }
            for (Joueur j : tab.getInscrits()) {
                tableauxParJoueur.computeIfAbsent(j, x -> new ArrayList<>()).add(tab.getNom());
            }
            for (Joueur j : tab.getFileAttente()) {
                tableauxParJoueur.computeIfAbsent(j, x -> new ArrayList<>()).add(tab.getNom());
            }
        }
        for (Map.Entry<Joueur, List<String>> e : tableauxParJoueur.entrySet()) {
            Joueur j = e.getKey();
            if (j.estADeterminer()) {
                continue;
            }
            if (j.getClub() == null || j.getClub().isBlank()) {
                res.add(new Alerte(TypeAlerte.JOUEUR_SANS_CLUB, e.getValue(), j, 0, 0));
            }
            // Points non renseignés (0) : le classement sert au tirage des poules.
            if (j.getPoints() <= 0) {
                res.add(new Alerte(TypeAlerte.JOUEUR_SANS_POINTS, e.getValue(), j, 0, 0));
            }
        }
        return res;
    }
}
