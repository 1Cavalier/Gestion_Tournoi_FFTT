package com.pingmanager.ui.ecran;

import com.pingmanager.core.ComparaisonStrategies;
import com.pingmanager.core.ResultatSimulation;
import com.pingmanager.core.ResultatSimulation.BilanTableau;
import com.pingmanager.model.StrategieOrdonnancement;
import com.pingmanager.model.Tournoi;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Présentation (sans JavaFX) de l'écran « Prévisions » (B025) : choix de la
 * stratégie du club, lecture des réglages de simulation, résumé chiffré d'une
 * simulation, une ligne par tableau et comparaison de deux stratégies.
 */
final class PresentationPrevisions {

    static final String TITRE = "Prévisions et stratégie";
    static final String TITRE_CARTE = "Stratégie d'ordonnancement";
    static final int TABLES_PAR_DEFAUT = 16;
    static final String ERREUR_TABLES = "Indiquez un nombre de tables entre 1 et 200.";
    static final String ERREUR_GRAINE = "La graine doit être un nombre entier (par exemple 2027).";
    static final String AVERTISSEMENT = "Prévision indicative : les parties sont simulées avec la durée estimée "
            + "et un vainqueur tiré selon les points. Le juge-arbitre reste maître de l'ordre des parties.";

    private PresentationPrevisions() {
    }

    // ------------------------------------------------------------------
    // Stratégie du club
    // ------------------------------------------------------------------

    /** Une stratégie proposée au club, avec son explication en clair. */
    record ChoixStrategie(StrategieOrdonnancement strategie, String libelle, String explication,
                          String contrepartie, boolean selectionnee) {
    }

    /** @return les trois stratégies, celle du tournoi sélectionnée. */
    static List<ChoixStrategie> choix(Tournoi tournoi) {
        StrategieOrdonnancement actuelle = tournoi.getStrategieOrdonnancement();
        List<ChoixStrategie> liste = new ArrayList<>();
        for (StrategieOrdonnancement s : StrategieOrdonnancement.values()) {
            liste.add(new ChoixStrategie(s, s.getLibelle(), s.getExplication(),
                    "En contrepartie : " + minusculeInitiale(s.getContrepartie()), s == actuelle));
        }
        return liste;
    }

    /** @return la phrase de la carte du tournoi : stratégie appliquée, choisie ou par défaut. */
    static String phraseStrategie(Tournoi tournoi) {
        StrategieOrdonnancement s = tournoi.getStrategieOrdonnancement();
        return tournoi.strategieOrdonnancementChoisie()
                ? "Stratégie choisie : « " + s.getLibelle() + " »."
                : "Aucun choix pour l'instant : « " + s.getLibelle() + " » s'applique par défaut.";
    }

    /** @return le message affiché après l'enregistrement d'un choix. */
    static String messageChoixEnregistre(StrategieOrdonnancement s) {
        return "Stratégie enregistrée : « " + s.getLibelle() + " ». Le jour J, les matchs proposés en salle "
                + "sont classés selon ce choix.";
    }

    // ------------------------------------------------------------------
    // Réglages de la simulation
    // ------------------------------------------------------------------

    /** @return le nombre de tables proposé : celui du tournoi, sinon {@value #TABLES_PAR_DEFAUT}. */
    static int tablesProposees(Tournoi tournoi) {
        return tournoi.getNombreDeTables() > 0 ? tournoi.getNombreDeTables() : TABLES_PAR_DEFAUT;
    }

    /** @return le nombre de tables saisi, ou {@code null} s'il est invalide (voir {@link #ERREUR_TABLES}). */
    static Integer lireTables(String saisie) {
        try {
            int n = Integer.parseInt(saisie == null ? "" : saisie.trim());
            return n >= 1 && n <= 200 ? n : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** @return la graine saisie (vide : 1), ou {@code null} si elle est invalide (voir {@link #ERREUR_GRAINE}). */
    static Long lireGraine(String saisie) {
        String s = saisie == null ? "" : saisie.trim();
        if (s.isEmpty()) {
            return 1L;
        }
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ------------------------------------------------------------------
    // Résultat d'une simulation
    // ------------------------------------------------------------------

    /** Un chiffre clé (tuile) : valeur, libellé, et vrai s'il signale un problème. */
    record Chiffre(String valeur, String libelle, boolean alerte) {
    }

    /** Une ligne par tableau : nom, début, fin prévue, heure souhaitée, état et sa pastille. */
    record LigneTableau(String nom, String debut, String fin, String finSouhaitee, String etat,
                        StatutPastille statut, String attente) {
    }

    /** @return les chiffres clés : fin prévue, attente moyenne et maximale, occupation, débordements. */
    static List<Chiffre> chiffres(ResultatSimulation r) {
        boolean plusieursJours = plusieursJours(r);
        int debordent = r.tableauxQuiDebordent().size();
        return List.of(
                new Chiffre(r.fin() == null ? "—" : heure(r.fin(), plusieursJours), "Fin prévue", !r.complete()),
                new Chiffre(minutes(Math.round(r.attenteMoyenne())), "Attente moyenne d'un joueur", false),
                new Chiffre(minutes(r.attenteMaximale()), "Attente la plus longue", false),
                new Chiffre(Math.round(r.occupationTables() * 100) + " %", "Occupation des tables", false),
                new Chiffre(String.valueOf(debordent), debordent > 1 ? "Tableaux qui débordent"
                        : "Tableau qui déborde", debordent > 0));
    }

    /** @return une phrase de synthèse (parties, tables, joueur qui a le plus attendu, tableaux écartés). */
    static String synthese(ResultatSimulation r) {
        StringBuilder sb = new StringBuilder();
        sb.append(r.nombreParties()).append(r.nombreParties() > 1 ? " parties simulées" : " partie simulée")
                .append(" sur ").append(r.nombreDeTables()).append(r.nombreDeTables() > 1 ? " tables" : " table")
                .append(" (graine ").append(r.graine()).append(")");
        if (r.joueurAttenteMax() != null && r.attenteMaximale() > 0) {
            sb.append(" ; c'est ").append(r.joueurAttenteMax()).append(" qui attend le plus : ")
                    .append(minutes(r.attenteMaximale()));
        }
        sb.append('.');
        if (!r.complete()) {
            sb.append(" La simulation n'a pas pu aller au bout : des parties n'ont jamais pu être lancées.");
        }
        for (ResultatSimulation.TableauIgnore i : r.ignores()) {
            sb.append(" Tableau « ").append(i.nom()).append(" » non simulé : ").append(i.motif()).append('.');
        }
        return sb.toString();
    }

    /** @return une ligne par tableau simulé, dans l'ordre du tournoi. */
    static List<LigneTableau> lignes(ResultatSimulation r) {
        boolean plusieursJours = plusieursJours(r);
        List<LigneTableau> lignes = new ArrayList<>();
        for (BilanTableau b : r.tableaux()) {
            String etat;
            StatutPastille statut;
            if (b.fin() == null) {
                etat = "Non terminé";
                statut = StatutPastille.ERREUR;
            } else if (b.deborde()) {
                etat = "Déborde de " + minutes(b.minutesDepassement());
                statut = StatutPastille.ERREUR;
            } else {
                etat = "À l'heure";
                statut = StatutPastille.SUCCES;
            }
            lignes.add(new LigneTableau(b.nom(), heure(b.debut(), plusieursJours),
                    b.fin() == null ? "—" : heure(b.fin(), plusieursJours),
                    b.finSouhaitee() == null ? "—" : heure(b.finSouhaitee(), plusieursJours),
                    etat, statut, minutes(Math.round(b.attenteMoyenne()))));
        }
        return lignes;
    }

    // ------------------------------------------------------------------
    // Comparaison
    // ------------------------------------------------------------------

    /**
     * Une ligne de comparaison : indicateur, valeur de chaque stratégie, et
     * laquelle est la meilleure (1, 2, ou 0 à égalité).
     */
    record LigneComparaison(String indicateur, String premiere, String seconde, int meilleure) {
    }

    /** @return les indicateurs comparés, puis l'heure de fin de chaque tableau. */
    static List<LigneComparaison> comparaison(ComparaisonStrategies c) {
        ResultatSimulation a = c.premiere();
        ResultatSimulation b = c.seconde();
        boolean jours = plusieursJours(a) || plusieursJours(b);
        List<LigneComparaison> lignes = new ArrayList<>();
        lignes.add(new LigneComparaison("Fin prévue", a.fin() == null ? "—" : heure(a.fin(), jours),
                b.fin() == null ? "—" : heure(b.fin(), jours), plusTot(a.fin(), b.fin())));
        lignes.add(new LigneComparaison("Attente moyenne", minutes(Math.round(a.attenteMoyenne())),
                minutes(Math.round(b.attenteMoyenne())), plusPetit(Math.round(a.attenteMoyenne()),
                Math.round(b.attenteMoyenne()))));
        lignes.add(new LigneComparaison("Attente la plus longue", minutes(a.attenteMaximale()),
                minutes(b.attenteMaximale()), plusPetit(a.attenteMaximale(), b.attenteMaximale())));
        long occA = Math.round(a.occupationTables() * 100);
        long occB = Math.round(b.occupationTables() * 100);
        lignes.add(new LigneComparaison("Occupation des tables", occA + " %", occB + " %", plusPetit(-occA, -occB)));
        int debA = a.tableauxQuiDebordent().size();
        int debB = b.tableauxQuiDebordent().size();
        lignes.add(new LigneComparaison("Tableaux qui débordent", String.valueOf(debA), String.valueOf(debB),
                plusPetit(debA, debB)));
        for (BilanTableau ta : a.tableaux()) {
            BilanTableau tb = b.tableau(ta.nom()).orElse(null);
            if (tb == null) {
                continue;
            }
            lignes.add(new LigneComparaison("Fin de « " + ta.nom() + " »",
                    ta.fin() == null ? "—" : heure(ta.fin(), jours),
                    tb.fin() == null ? "—" : heure(tb.fin(), jours), plusTot(ta.fin(), tb.fin())));
        }
        return lignes;
    }

    /** @return la phrase qui résume l'écart entre les deux stratégies. */
    static String verdict(ComparaisonStrategies c) {
        String premiere = "« " + c.premiere().strategie().getLibelle() + " »";
        String seconde = "« " + c.seconde().strategie().getLibelle() + " »";
        if (c.premiere().strategie() == c.seconde().strategie()) {
            return "Choisissez deux stratégies différentes pour les comparer.";
        }
        long fin = c.minutesFinGagneesParLaSeconde();
        long attente = Math.round(c.attenteMoyenneGagneeParLaSeconde());
        String phraseFin = fin == 0 ? "Les deux stratégies finissent à la même heure"
                : (fin > 0 ? seconde : premiere) + " finit " + minutes(Math.abs(fin)) + " plus tôt";
        String phraseAttente = attente == 0 ? "l'attente moyenne est la même"
                : "avec " + (attente > 0 ? seconde : premiere) + ", les joueurs attendent en moyenne "
                + minutes(Math.abs(attente)) + " de moins par partie";
        return phraseFin + " ; " + phraseAttente + ".";
    }

    // ------------------------------------------------------------------
    // Formats
    // ------------------------------------------------------------------

    /** « 45 min », « 1 h », « 2 h 05 ». */
    static String minutes(long minutes) {
        long m = Math.max(0, minutes);
        if (m < 60) {
            return m + " min";
        }
        long h = m / 60;
        long reste = m % 60;
        return reste == 0 ? h + " h" : h + " h " + (reste < 10 ? "0" : "") + reste;
    }

    /** « 18 h 05 », précédé du jour (« 11/04 ») quand la simulation dure plusieurs jours. */
    static String heure(LocalDateTime t, boolean avecJour) {
        Objects.requireNonNull(t, "t");
        String h = t.getHour() + " h " + (t.getMinute() < 10 ? "0" : "") + t.getMinute();
        if (!avecJour) {
            return h;
        }
        return String.format("%02d/%02d %s", t.getDayOfMonth(), t.getMonthValue(), h);
    }

    private static boolean plusieursJours(ResultatSimulation r) {
        LocalDate premier = null;
        for (BilanTableau b : r.tableaux()) {
            for (LocalDateTime t : new LocalDateTime[]{b.debut(), b.fin()}) {
                if (t == null) {
                    continue;
                }
                if (premier == null) {
                    premier = t.toLocalDate();
                } else if (!premier.equals(t.toLocalDate())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static int plusTot(LocalDateTime a, LocalDateTime b) {
        if (a == null || b == null || a.equals(b)) {
            return a == null && b != null ? 2 : (b == null && a != null ? 1 : 0);
        }
        return a.isBefore(b) ? 1 : 2;
    }

    private static int plusPetit(long a, long b) {
        return a == b ? 0 : (a < b ? 1 : 2);
    }

    private static String minusculeInitiale(String s) {
        return s.isEmpty() ? s : Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }
}
