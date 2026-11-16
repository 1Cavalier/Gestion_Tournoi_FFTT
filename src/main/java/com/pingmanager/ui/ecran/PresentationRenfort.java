package com.pingmanager.ui.ecran;

import com.pingmanager.core.PrevisionTournoi.PrevisionJournee;
import com.pingmanager.core.RenfortTirage;
import com.pingmanager.core.StatistiquesRenfort;
import com.pingmanager.core.StatistiquesSimulation;
import com.pingmanager.model.StrategieOrdonnancement;
import com.pingmanager.ordonnancement.ReglageRenfort;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Mots et lecture des saisies des tables de renfort (B054), sans JavaFX :
 * champ de l'écran Sportif, réglages et résultats de l'écran Prévisions.
 */
final class PresentationRenfort {

    static final String LIBELLE_CHAMP = "Tables d'échauffement utilisables en renfort";
    static final String AIDE_CHAMP = "Le juge-arbitre peut en utiliser comme tables de match pour rattraper "
            + "un retard (3.3.1.2.2). 0 = aucune.";
    /** Probabilité d'ouverture sous laquelle le renfort est dit « presque jamais nécessaire ». */
    static final double PRESQUE_JAMAIS = 0.05;

    private PresentationRenfort() {
    }

    /** Texte d'aide qui explique la règle de déclenchement, pour l'écran Prévisions. */
    static String aideRegle() {
        return "Les tables de renfort sont ouvertes quand le retard prévu dépasse le seuil d'ouverture : un "
                + "tableau dont l'heure est passée et dont toutes les poules n'ont pas commencé, ou une fin "
                + "projetée avec les seules tables de compétition après la fin visée. Elles sont rendues à "
                + "l'échauffement quand ce retard retombe au seuil de retour (0 = retard résorbé). Les tables de "
                + "compétition sont toujours servies d'abord ; un match commencé sur une table de renfort va à "
                + "son terme.";
    }

    /**
     * Lit le champ « tables utilisables en renfort » de l'écran Sportif.
     *
     * @return de 0 à {@code echauffement} (vide = 0 ; sans salle d'échauffement, toujours 0)
     * @throws IllegalArgumentException avec un message en clair si la saisie est illisible ou trop grande
     */
    static int lireTablesRenfort(String saisie, int echauffement) {
        if (echauffement <= 0) {
            return 0;
        }
        Integer n = Saisie.lireEntier(saisie, LIBELLE_CHAMP);
        if (n == null) {
            return 0;
        }
        if (n < 0) {
            throw new IllegalArgumentException(LIBELLE_CHAMP + " : le nombre ne peut pas être négatif.");
        }
        if (n > echauffement) {
            throw new IllegalArgumentException(LIBELLE_CHAMP + " : au plus " + echauffement
                    + (echauffement > 1 ? " tables" : " table") + ", le nombre de tables de la salle d'échauffement.");
        }
        return n;
    }

    /** Réglage du renfort lu à l'écran Prévisions, ou le motif du refus. */
    record ReglageLu(ReglageRenfort reglage, String erreur) {
    }

    /** Lit les réglages du renfort de l'écran Prévisions (tables, seuil d'ouverture, seuil de retour). */
    static ReglageLu lireReglage(String tables, String ouverture, String retour, int echauffement) {
        int n;
        try {
            n = lireTablesRenfort(tables, echauffement);
        } catch (IllegalArgumentException e) {
            return new ReglageLu(null, e.getMessage());
        }
        Integer o = entier(ouverture, ReglageRenfort.SEUIL_DEPLOIEMENT_PAR_DEFAUT);
        Integer r = entier(retour, ReglageRenfort.SEUIL_RETRAIT_PAR_DEFAUT);
        if (o == null || o < 0 || o > 600) {
            return new ReglageLu(null, "Seuil d'ouverture du renfort : un nombre de minutes de 0 à 600.");
        }
        if (r == null || r < 0 || r > o) {
            return new ReglageLu(null, "Seuil de retour du renfort : un nombre de minutes de 0 au seuil d'ouverture ("
                    + o + " min).");
        }
        return new ReglageLu(ReglageRenfort.builder().tablesMax(n).seuilDeploiementMinutes(o)
                .seuilRetraitMinutes(r).build(), null);
    }

    private static Integer entier(String saisie, int parDefaut) {
        if (saisie == null || saisie.isBlank()) {
            return parDefaut;
        }
        try {
            return Integer.parseInt(saisie.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Une ligne du bilan du renfort : un indicateur et sa valeur en clair. */
    record Ligne(String indicateur, String valeur) {
    }

    /** Bilan du renfort d'une journée pour la stratégie choisie, ou vide sans renfort. */
    static Optional<Bilan> bilan(PrevisionJournee journee, StrategieOrdonnancement strategie) {
        if (journee == null || !journee.avecRenfort()) {
            return Optional.empty();
        }
        Optional<StatistiquesSimulation> avec = journee.pour(strategie);
        Optional<StatistiquesSimulation> sans = journee.sansRenfortPour(strategie);
        if (avec.isEmpty() || sans.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new Bilan(journee, avec.get(), sans.get()));
    }

    /** Bilan du renfort d'une journée : verdict et lignes. */
    record Bilan(PrevisionJournee journee, StatistiquesSimulation avec, StatistiquesSimulation sans) {

        /** Phrase de conclusion, à lire en premier. */
        String verdict() {
            StatistiquesRenfort r = avec.renfort();
            LocalDate jour = journee.jour();
            if (avec.finMoyenne() == null) {
                return "Aucune partie simulée pour cette journée : rien à dire sur le renfort.";
            }
            if (r.probabiliteDeploiement() < PRESQUE_JAMAIS) {
                return "Le renfort n'a presque jamais été nécessaire : les " + journee.nombreDeTables()
                        + " tables de compétition suffisent.";
            }
            String ouvrir = r.premiereMiseEnService() == null ? ""
                    : " Prévoir d'ouvrir les tables d'échauffement vers "
                    + PresentationPrevisions.heureDuJour(r.premiereMiseEnService(), jour) + ".";
            double risqueSans = sans.probabiliteDebordement();
            double risqueAvec = avec.probabiliteDebordement();
            if (risqueSans >= 0.5 && risqueAvec < 0.5) {
                return "Le renfort permet de finir à l'heure : sans lui, la journée dépasse la fin visée dans "
                        + PresentationPrevisions.pourcent(risqueSans) + " des cas, avec lui dans "
                        + PresentationPrevisions.pourcent(risqueAvec) + "." + ouvrir;
            }
            if (risqueAvec >= 0.5) {
                return "Même avec le renfort, la journée dépasse la fin visée dans "
                        + PresentationPrevisions.pourcent(risqueAvec) + " des cas : il faut d'autres mesures "
                        + "(plus de tables, moins de joueurs, horaires)." + ouvrir;
            }
            return "Le renfort rattrape les retards passagers ; la journée finit à l'heure avec ou sans lui dans la "
                    + "plupart des cas." + ouvrir;
        }

        List<Ligne> lignes() {
            LocalDate jour = journee.jour();
            StatistiquesRenfort r = avec.renfort();
            List<Ligne> l = new ArrayList<>();
            if (journee.finVisee() != null) {
                l.add(new Ligne("Fin visée", PresentationPrevisions.heureDuJour(journee.finVisee(), jour)));
            }
            l.add(new Ligne("Fin prévue sans renfort", fin(sans, jour)));
            l.add(new Ligne("Fin prévue avec renfort", fin(avec, jour)));
            if (sans.finMoyenne() != null && avec.finMoyenne() != null) {
                long gagne = Duration.between(avec.finMoyenne(), sans.finMoyenne()).toMinutes();
                l.add(new Ligne("Temps gagné en moyenne", gagne > 0 ? PresentationPrevisions.minutes(gagne) : "aucun"));
            }
            l.add(new Ligne("Renfort ouvert", PresentationPrevisions.pourcent(r.probabiliteDeploiement())
                    + " des simulations"));
            if (r.premiereMiseEnService() != null) {
                l.add(new Ligne("Première mise en service", "vers "
                        + PresentationPrevisions.heureDuJour(r.premiereMiseEnService(), jour)
                        + (r.premiereMiseEnServiceTot() != null
                        && r.premiereMiseEnServiceTot().isBefore(r.premiereMiseEnService())
                        ? " (au plus tôt " + PresentationPrevisions.heureDuJour(r.premiereMiseEnServiceTot(), jour)
                        + ")" : "")));
            }
            l.add(new Ligne("Plages habituelles", plages(r.plagesHabituelles(), jour)));
            l.add(new Ligne("Tables ouvertes en même temps", r.tablesEnServiceMax() + " sur " + r.tablesMax()));
            l.add(new Ligne("Utilisation des tables de renfort", PresentationPrevisions.pourcent(r.utilisationMoyenne())
                    + " de la journée"));
            return l;
        }
    }

    /** « 19 h 40 (1 fois sur 20 après 20 h 10) ». */
    static String fin(StatistiquesSimulation s, LocalDate jour) {
        if (s.finMoyenne() == null) {
            return "—";
        }
        String texte = "vers " + PresentationPrevisions.heureDuJour(s.finMoyenne(), jour);
        if (s.finTard() != null && s.finTard().isAfter(s.finMoyenne().plusMinutes(4))) {
            texte += " (1 fois sur 20 après " + PresentationPrevisions.heureDuJour(s.finTard(), jour) + ")";
        }
        return texte;
    }

    /** « 10 h 15 – 12 h 30 (3 tables) ; 16 h – 17 h (1 table) », ou une phrase si aucune. */
    static String plages(List<RenfortTirage.Plage> plages, LocalDate jour) {
        if (plages.isEmpty()) {
            return "aucune : le renfort n'est ouvert que dans moins de la moitié des simulations";
        }
        List<String> textes = new ArrayList<>();
        for (RenfortTirage.Plage p : plages) {
            textes.add(PresentationPrevisions.heureDuJour(p.debut(), jour) + " – "
                    + PresentationPrevisions.heureDuJour(p.fin(), jour)
                    + " (" + p.tablesMax() + (p.tablesMax() > 1 ? " tables)" : " table)"));
        }
        return String.join(" ; ", textes);
    }
}
