package com.pingmanager.core;

import com.pingmanager.model.ElementPlan.TablePlan;
import com.pingmanager.model.SalleTournoi;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.ordonnancement.ReservationsTables;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Salles et plans d'un tournoi (B024) : salles effectives (une salle en grille
 * pour un tournoi sans plan), cohérence du plan avec le nombre de tables du
 * tournoi, et réservations à transmettre à l'ordonnanceur.
 *
 * <p>Les tables de compétition restent numérotées de 1 à N (N = nombre de
 * tables du tournoi, section Informations sportives et techniques) : ce sont
 * ces numéros que portent les matchs. Le plan dit où se trouve chaque table ;
 * il peut en oublier ou en compter en trop, ce que {@link #verifier} signale
 * sans rien bloquer.</p>
 */
public final class ServicePlanSalle {

    /** Nom de la salle unique d'un tournoi sans plan. */
    public static final String NOM_SALLE_PAR_DEFAUT = "Salle";

    /**
     * Bilan de cohérence du plan.
     *
     * @param tablesHorsPlan        tables de 1 à N qui ne sont sur aucun plan
     * @param tablesEnTrop          tables du plan au-delà de N (ignorées le jour J)
     * @param tableauxInconnus      réservations ou préférences vers un tableau qui n'existe pas
     * @param planPrepare           vrai si le club a préparé un plan (sinon salle par défaut)
     */
    public record BilanPlanSalles(Set<Integer> tablesHorsPlan, Set<Integer> tablesEnTrop,
                                  Set<String> tableauxInconnus, boolean planPrepare) {
        public BilanPlanSalles {
            tablesHorsPlan = Set.copyOf(tablesHorsPlan);
            tablesEnTrop = Set.copyOf(tablesEnTrop);
            tableauxInconnus = Set.copyOf(tableauxInconnus);
        }

        /** @return vrai si le plan colle exactement aux tables du tournoi et aux tableaux. */
        public boolean estCoherent() {
            return tablesHorsPlan.isEmpty() && tablesEnTrop.isEmpty() && tableauxInconnus.isEmpty();
        }
    }

    /**
     * @return les salles du tournoi ; pour un tournoi sans plan (ancien fichier),
     *         une seule salle « Salle » avec ses tables en grille
     */
    public List<SalleTournoi> salles(Tournoi tournoi) {
        if (tournoi.aUnPlanDeSalle()) {
            return tournoi.getSalles();
        }
        return List.of(new SalleTournoi(NOM_SALLE_PAR_DEFAUT,
                PlansTypesSalle.grilleParDefaut(tournoi.getNombreDeTables())));
    }

    /** Cohérence du plan avec le nombre de tables et les tableaux du tournoi. */
    public BilanPlanSalles verifier(Tournoi tournoi) {
        Set<String> noms = new HashSet<>();
        for (Tableau t : tournoi.getTableaux()) {
            noms.add(t.getNom());
        }
        return verifier(salles(tournoi), tournoi.getNombreDeTables(), noms, tournoi.aUnPlanDeSalle());
    }

    /** Cohérence de salles en cours d'édition (brouillon de l'éditeur). */
    public BilanPlanSalles verifier(List<SalleTournoi> salles, int nombreDeTables, Set<String> tableaux,
                                    boolean planPrepare) {
        int n = Math.max(0, nombreDeTables);
        Set<Integer> placees = new HashSet<>();
        Set<Integer> enTrop = new TreeSet<>();
        Set<String> inconnus = new TreeSet<>();
        for (SalleTournoi s : salles) {
            for (TablePlan t : s.getPlan().getTables()) {
                placees.add(t.numero());
                if (t.numero() > n) {
                    enTrop.add(t.numero());
                }
                if (t.tableauReserve() != null && !tableaux.contains(t.tableauReserve())) {
                    inconnus.add(t.tableauReserve());
                }
            }
            for (String x : s.getTableauxReserves()) {
                if (!tableaux.contains(x)) {
                    inconnus.add(x);
                }
            }
            for (String x : s.getTableauxPreferes()) {
                if (!tableaux.contains(x)) {
                    inconnus.add(x);
                }
            }
        }
        Set<Integer> horsPlan = new TreeSet<>();
        for (int i = 1; i <= n; i++) {
            if (!placees.contains(i)) {
                horsPlan.add(i);
            }
        }
        return new BilanPlanSalles(horsPlan, enTrop, inconnus, planPrepare);
    }

    /**
     * Réservations à respecter le jour J (B024), d'après les salles préparées :
     * une table réservée à un tableau ne reçoit que lui ; les tables d'une salle
     * réservée ne reçoivent que ses tableaux (sauf réservation propre à la table) ;
     * un tableau préféré dans une salle va d'abord sur ses tables. Sans plan :
     * aucune réservation.
     */
    public ReservationsTables reservations(Tournoi tournoi) {
        if (!tournoi.aUnPlanDeSalle()) {
            return ReservationsTables.AUCUNE;
        }
        ReservationsTables.Builder b = ReservationsTables.builder();
        for (SalleTournoi s : tournoi.getSalles()) {
            for (TablePlan t : s.getPlan().getTables()) {
                if (t.tableauReserve() != null) {
                    b.restreindre(t.numero(), Set.of(t.tableauReserve()));
                } else if (!s.getTableauxReserves().isEmpty()) {
                    b.restreindre(t.numero(), s.getTableauxReserves());
                }
                for (String prefere : s.getTableauxPreferes()) {
                    if (t.tableauReserve() == null || t.tableauReserve().equals(prefere)) {
                        b.preferer(prefere, t.numero());
                    }
                }
            }
        }
        return b.construire();
    }

    /** @return la salle qui contient la table, ou null si elle n'est sur aucun plan. */
    public SalleTournoi salleDeLaTable(Tournoi tournoi, int numero) {
        for (SalleTournoi s : salles(tournoi)) {
            if (s.getPlan().table(numero).isPresent()) {
                return s;
            }
        }
        return null;
    }

    /** @return les numéros de table utilisés dans toutes les salles, sauf celle d'index {@code sauf}. */
    static Set<Integer> numerosAilleurs(List<SalleTournoi> salles, int sauf) {
        Set<Integer> numeros = new HashSet<>();
        for (int i = 0; i < salles.size(); i++) {
            if (i != sauf) {
                for (TablePlan t : salles.get(i).getPlan().getTables()) {
                    numeros.add(t.numero());
                }
            }
        }
        return numeros;
    }

    /** @return le plus petit numéro de table positif libre dans toutes les salles. */
    static int premierNumeroLibre(List<SalleTournoi> salles) {
        Set<Integer> pris = numerosAilleurs(salles, -1);
        int n = 1;
        while (pris.contains(n)) {
            n++;
        }
        return n;
    }

}
