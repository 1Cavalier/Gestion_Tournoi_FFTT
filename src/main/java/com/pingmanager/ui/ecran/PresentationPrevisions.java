package com.pingmanager.ui.ecran;

import com.pingmanager.core.ComparaisonStrategies;
import com.pingmanager.core.ResultatSimulation;
import com.pingmanager.core.ResultatSimulation.BilanTableau;
import com.pingmanager.core.StatistiquesSimulation;
import com.pingmanager.core.StatistiquesSimulation.StatTableau;
import com.pingmanager.model.StrategieOrdonnancement;
import com.pingmanager.model.Tournoi;
import com.pingmanager.ordonnancement.LoiDureeMatch;
import com.pingmanager.ordonnancement.TempsAnnexesMatch;

import java.time.Duration;
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
    // Simulation statistique : réglages
    // ------------------------------------------------------------------

    static final int TIRAGES_PAR_DEFAUT = 100;
    static final String ERREUR_TIRAGES = "Indiquez un nombre de tirages entre 1 et 1000.";
    static final String ERREUR_DUREES = "Indiquez les durées d'un match en minutes (nombres entiers).";
    /** Durées par défaut d'un match en trois manches gagnantes : moyenne, minimum, maximum (minutes). */
    static final int DUREE_MOYENNE = 20;
    static final int DUREE_MINIMUM = 10;
    static final int DUREE_MAXIMUM = 50;
    static final String AVERTISSEMENT_STATISTIQUE = "Le tournoi est rejoué de nombreuses fois : à chaque "
            + "tirage, d'autres poules, d'autres vainqueurs (selon les points) et d'autres durées de match, "
            + "tirées entre le minimum et le maximum (les matchs longs sont rares). Les trois stratégies "
            + "jouent exactement les mêmes tirages : les écarts viennent de la stratégie seule. "
            + "Le juge-arbitre reste maître de l'ordre des parties.";

    /** @return le nombre de tirages saisi (vide : {@value #TIRAGES_PAR_DEFAUT}), ou {@code null} s'il est invalide. */
    static Integer lireTirages(String saisie) {
        String s = saisie == null ? "" : saisie.trim();
        if (s.isEmpty()) {
            return TIRAGES_PAR_DEFAUT;
        }
        try {
            int n = Integer.parseInt(s);
            return n >= 1 && n <= 1000 ? n : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Loi des durées lue à l'écran, ou le message d'erreur à afficher. */
    record ReglageDurees(LoiDureeMatch loi, String erreur) {
    }

    /** @return la loi des durées d'un match (moyenne, minimum, maximum en minutes), ou l'erreur. */
    static ReglageDurees lireDurees(String moyenne, String minimum, String maximum) {
        Integer moy = entier(moyenne, DUREE_MOYENNE);
        Integer min = entier(minimum, DUREE_MINIMUM);
        Integer max = entier(maximum, DUREE_MAXIMUM);
        if (moy == null || min == null || max == null) {
            return new ReglageDurees(null, ERREUR_DUREES);
        }
        try {
            return new ReglageDurees(new LoiDureeMatch(Duration.ofMinutes(moy), Duration.ofMinutes(min),
                    Duration.ofMinutes(max)), null);
        } catch (IllegalArgumentException e) {
            return new ReglageDurees(null, e.getMessage());
        }
    }

    private static Integer entier(String saisie, int defaut) {
        String s = saisie == null ? "" : saisie.trim();
        if (s.isEmpty()) {
            return defaut;
        }
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Temps autour du jeu lus à l'écran, ou le message d'erreur à afficher. */
    record ReglageAnnexes(TempsAnnexesMatch temps, String erreur) {
    }

    static final String ERREUR_ANNEXES = "Indiquez les temps autour du match en nombres (minutes, secondes "
            + "ou pourcentages, par exemple 1,5).";

    /** Valeurs proposées à l'écran (celles de {@link TempsAnnexesMatch#PAR_DEFAUT}). */
    static final String APPEL = "1";
    static final String RAPPEL_POURCENT = "15";
    static final String RAPPEL_MINUTES = "3";
    static final String ECHAUFFEMENT = "2";
    static final String PAUSE_SECONDES = "30";
    static final String TEMPS_MORT_POURCENT = "20";
    static final String FEUILLE = "1,5";

    /**
     * @return les temps autour du jeu : appel (min), joueur à rappeler (%), retard
     *         d'un rappel (min), échauffement (min), pause entre manches (s),
     *         temps mort pris par un joueur (%), retour de la feuille (min).
     */
    static ReglageAnnexes lireTempsAnnexes(String appel, String rappelPourcent, String rappelMinutes,
                                           String echauffement, String pauseSecondes, String tempsMortPourcent,
                                           String feuille) {
        Double a = nombre(appel, APPEL);
        Double rp = nombre(rappelPourcent, RAPPEL_POURCENT);
        Double rm = nombre(rappelMinutes, RAPPEL_MINUTES);
        Double e = nombre(echauffement, ECHAUFFEMENT);
        Double p = nombre(pauseSecondes, PAUSE_SECONDES);
        Double tm = nombre(tempsMortPourcent, TEMPS_MORT_POURCENT);
        Double f = nombre(feuille, FEUILLE);
        if (a == null || rp == null || rm == null || e == null || p == null || tm == null || f == null) {
            return new ReglageAnnexes(null, ERREUR_ANNEXES);
        }
        try {
            return new ReglageAnnexes(TempsAnnexesMatch.builder()
                    .appel((int) Math.round(a * 60)).probabiliteRappel(rp / 100).rappel((int) Math.round(rm * 60))
                    .echauffement((int) Math.round(e * 60)).pauseEntreManches((int) Math.round(p))
                    .probabiliteTempsMort(tm / 100).feuille((int) Math.round(f * 60)).build(), null);
        } catch (IllegalArgumentException ex) {
            return new ReglageAnnexes(null, ex.getMessage());
        }
    }

    private static Double nombre(String saisie, String defaut) {
        String s = saisie == null || saisie.isBlank() ? defaut : saisie.trim();
        try {
            return Double.parseDouble(s.replace(',', '.'));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ------------------------------------------------------------------
    // Simulation statistique : les trois stratégies
    // ------------------------------------------------------------------

    /** Écart (minutes) en deçà duquel deux heures de fin moyennes sont jugées équivalentes. */
    static final int ECART_NEGLIGEABLE = 5;

    /**
     * Une ligne du tableau des stratégies : l'indicateur, la valeur de chaque
     * stratégie (dans l'ordre reçu) et l'index de la meilleure (-1 à égalité).
     */
    record LigneStrategies(String indicateur, List<String> valeurs, int meilleure) {
        LigneStrategies {
            valeurs = List.copyOf(valeurs);
        }
    }

    /**
     * @return les indicateurs comparés pour chaque stratégie, dans l'ordre de
     *         l'écran : début des tableaux, fin, attentes, tables, débordement.
     */
    static List<LigneStrategies> lignesStrategies(List<StatistiquesSimulation> stats) {
        List<LigneStrategies> lignes = new ArrayList<>();
        lignes.add(ligne("Début à l'heure (" + StatistiquesSimulation.RETARD_A_L_HEURE + " min de retard au plus)",
                stats, s -> -Math.round(s.debutALHeure() * 100), s -> pourcent(s.debutALHeure())));
        lignes.add(ligne("Début en léger retard (" + StatistiquesSimulation.RETARD_A_L_HEURE + " à "
                        + StatistiquesSimulation.GROS_RETARD + " min)", stats,
                s -> Math.round(s.debutLegerRetard() * 100), s -> pourcent(s.debutLegerRetard())));
        lignes.add(ligne("Début en gros retard (plus de " + StatistiquesSimulation.GROS_RETARD + " min)", stats,
                s -> Math.round(s.debutGrosRetard() * 100), s -> pourcent(s.debutGrosRetard())));
        lignes.add(ligne("Première finale (en moyenne)", stats, s -> secondes(s.premiereFinale()),
                s -> dateEtHeure(s.premiereFinale())));
        lignes.add(ligne("Fin moyenne d'un tableau", stats, s -> secondes(s.finMoyenneTableau()),
                s -> dateEtHeure(s.finMoyenneTableau())));
        lignes.add(ligne("Fin de la journée (moyenne)", stats, s -> secondes(s.finMoyenne()),
                s -> dateEtHeure(s.finMoyenne())));
        lignes.add(ligne("Cas rare : fin tôt (1 fois sur 20)", stats, s -> secondes(s.finTot()),
                s -> dateEtHeure(s.finTot())));
        lignes.add(ligne("Cas rare : fin tard (1 fois sur 20)", stats, s -> secondes(s.finTard()),
                s -> dateEtHeure(s.finTard())));
        lignes.add(ligne("Attente moyenne d'un joueur inscrit sur 1 tableau", stats,
                s -> attenteCle(s.attenteUnTableau()), s -> attente(s.attenteUnTableau())));
        lignes.add(ligne("Attente moyenne d'un joueur inscrit sur 2 tableaux ou plus", stats,
                s -> attenteCle(s.attentePlusieursTableaux()), s -> attente(s.attentePlusieursTableaux())));
        lignes.add(ligne("Plus longue attente", stats, s -> Math.round(s.plusLongueAttente()),
                s -> minutes(Math.round(s.plusLongueAttente()))));
        lignes.add(ligne("Occupation des tables", stats, s -> -Math.round(s.occupationMoyenne() * 100),
                s -> Math.round(s.occupationMoyenne() * 100) + " %"));
        lignes.add(ligne("Risque qu'un tableau finisse après l'heure visée", stats, s -> Math.round(s.probabiliteDebordement() * 100),
                s -> pourcent(s.probabiliteDebordement())));
        return lignes;
    }

    private static long attenteCle(double minutes) {
        return minutes < 0 ? Long.MAX_VALUE : Math.round(minutes);
    }

    private static String attente(double minutes) {
        return minutes < 0 ? "Aucun joueur" : minutes(Math.round(minutes));
    }

    /** « dim. 11/04, 21 h 03 » : le jour et l'heure. */
    static String dateEtHeure(LocalDateTime t) {
        if (t == null) {
            return "—";
        }
        String jour = t.getDayOfWeek().getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.FRENCH);
        return String.format("%s %02d/%02d, %s", jour, t.getDayOfMonth(), t.getMonthValue(), heure(t, false));
    }

    private static LigneStrategies ligne(String indicateur, List<StatistiquesSimulation> stats,
                                         java.util.function.ToLongFunction<StatistiquesSimulation> cle,
                                         java.util.function.Function<StatistiquesSimulation, String> texte) {
        List<String> valeurs = new ArrayList<>();
        int meilleure = -1;
        long meilleurScore = Long.MAX_VALUE;
        boolean egalite = false;
        for (int i = 0; i < stats.size(); i++) {
            valeurs.add(texte.apply(stats.get(i)));
            long c = cle.applyAsLong(stats.get(i));
            if (c < meilleurScore) {
                meilleurScore = c;
                meilleure = i;
                egalite = false;
            } else if (c == meilleurScore) {
                egalite = true;
            }
        }
        return new LigneStrategies(indicateur, valeurs, egalite ? -1 : meilleure);
    }

    /**
     * @return la phrase qui résume ce que la simulation apprend : quelle stratégie
     *         finit le plus tôt, laquelle fait le moins attendre, et si le choix
     *         compte vraiment avec ce nombre de tables.
     */
    static String verdictStrategies(List<StatistiquesSimulation> stats, StrategieOrdonnancement choisie) {
        if (stats.isEmpty() || stats.stream().allMatch(s -> s.finMoyenne() == null)) {
            return "Rien à simuler : aucun tableau n'a assez de joueurs.";
        }
        StatistiquesSimulation tot = stats.get(0);
        StatistiquesSimulation tard = stats.get(0);
        StatistiquesSimulation attente = stats.get(0);
        for (StatistiquesSimulation s : stats) {
            if (secondes(s.finMoyenne()) < secondes(tot.finMoyenne())) {
                tot = s;
            }
            if (secondes(s.finMoyenne()) > secondes(tard.finMoyenne())) {
                tard = s;
            }
            if (s.attenteMoyenne() < attente.attenteMoyenne()) {
                attente = s;
            }
        }
        long ecart = (secondes(tard.finMoyenne()) - secondes(tot.finMoyenne())) / 60;
        long ecartAttente = Math.round(stats.stream().mapToDouble(StatistiquesSimulation::attenteMoyenne).max()
                .orElse(0) - attente.attenteMoyenne());
        StringBuilder sb = new StringBuilder();
        if (ecart < ECART_NEGLIGEABLE && ecartAttente < 2) {
            sb.append("Avec ").append(tot.nombreDeTables()).append(" tables, la salle n'est presque jamais "
                    + "saturée : les trois stratégies donnent le même résultat à quelques minutes près. "
                    + "Essayez avec moins de tables pour voir à partir de quand le choix compte.");
            return sb.toString();
        }
        sb.append("« ").append(tot.strategie().getLibelle()).append(" » finit en moyenne le plus tôt");
        if (ecart >= ECART_NEGLIGEABLE) {
            sb.append(" (").append(minutes(ecart)).append(" avant « ").append(tard.strategie().getLibelle())
                    .append(" »)");
        }
        sb.append(". ");
        if (attente.strategie() == tot.strategie()) {
            sb.append("C'est aussi celle qui fait le moins attendre les joueurs.");
        } else {
            sb.append("Les joueurs attendent le moins avec « ").append(attente.strategie().getLibelle())
                    .append(" »");
            if (ecartAttente > 0) {
                sb.append(" (jusqu'à ").append(minutes(ecartAttente)).append(" de moins par partie)");
            }
            sb.append('.');
        }
        StatistiquesSimulation vagues = trouver(stats, StrategieOrdonnancement.FAIRE_AVANCER_PETIT_A_PETIT);
        StatistiquesSimulation tot1 = trouver(stats, StrategieOrdonnancement.AVANCER_TABLEAUX_COMMENCES);
        if (vagues != null && tot1 != null && vagues.premiereFinale() != null && tot1.premiereFinale() != null) {
            sb.append(" Avec « ").append(tot1.strategie().getLibelle()).append(" », chaque tableau joue sa finale "
                    + "à son heure : la première vers ").append(heure(tot1.premiereFinale(), false))
                    .append(", un tableau finit en moyenne vers ").append(heure(tot1.finMoyenneTableau(), false))
                    .append(". Avec « ").append(vagues.strategie().getLibelle())
                    .append(" », toutes les finales sont gardées pour la fin : de ")
                    .append(heure(vagues.premiereFinale(), false)).append(" à ")
                    .append(heure(vagues.finMoyenne(), false)).append('.');
        }
        if (choisie != null && choisie != tot.strategie() && choisie != attente.strategie()) {
            sb.append(" Votre stratégie « ").append(choisie.getLibelle())
                    .append(" » n'est la meilleure sur aucun de ces deux points.");
        }
        return sb.toString();
    }

    private static StatistiquesSimulation trouver(List<StatistiquesSimulation> stats, StrategieOrdonnancement s) {
        return stats.stream().filter(x -> x.strategie() == s).findFirst().orElse(null);
    }

    /** Ce que montre une stratégie pour un tableau, dans la comparaison côte à côte. */
    record CelluleTableau(String debut, String fin, String finTard, String attente, String risque,
                          StatutPastille statut) {
    }

    /**
     * Un tableau comparé sous les trois stratégies : une cellule par stratégie
     * (dans l'ordre reçu) et l'index de celle qui le finit le plus tôt (-1 à égalité).
     */
    record TableauCompare(String nom, List<CelluleTableau> cellules, int finitLePlusTot) {
        TableauCompare {
            cellules = List.copyOf(cellules);
        }
    }

    /** @return chaque tableau, avec les trois stratégies côte à côte. */
    static List<TableauCompare> tableauxCompares(List<StatistiquesSimulation> stats) {
        List<TableauCompare> resultat = new ArrayList<>();
        if (stats.isEmpty()) {
            return resultat;
        }
        List<List<LigneTableauStat>> parStrategie = new ArrayList<>();
        for (StatistiquesSimulation s : stats) {
            parStrategie.add(lignesTableaux(s));
        }
        for (int k = 0; k < stats.get(0).tableaux().size(); k++) {
            List<CelluleTableau> cellules = new ArrayList<>();
            int meilleur = -1;
            long meilleureFin = Long.MAX_VALUE;
            boolean egalite = false;
            for (int i = 0; i < stats.size(); i++) {
                LigneTableauStat l = parStrategie.get(i).get(k);
                cellules.add(new CelluleTableau(l.debut(), l.finMoyenne(), l.finTard(), l.attente(), l.risque(),
                        l.statut()));
                long fin = secondes(stats.get(i).tableaux().get(k).finMoyenne()) / 60;
                if (fin < meilleureFin) {
                    meilleureFin = fin;
                    meilleur = i;
                    egalite = false;
                } else if (fin == meilleureFin) {
                    egalite = true;
                }
            }
            resultat.add(new TableauCompare(stats.get(0).tableaux().get(k).nom(), cellules, egalite ? -1 : meilleur));
        }
        return resultat;
    }

    // ------------------------------------------------------------------
    // Heure de fin visée et journées
    // ------------------------------------------------------------------

    static final String FIN_VISEE_PAR_DEFAUT = "20:00";
    static final String ERREUR_FIN_VISEE = "Indiquez l'heure de fin visée sous la forme 20:00 (ou 20 h 30).";

    /** @return l'heure saisie (« 20:00 », « 20h30 », « 20 h », vide : 20:00), ou {@code null}. */
    static java.time.LocalTime lireFinVisee(String saisie) {
        String s = saisie == null || saisie.isBlank() ? FIN_VISEE_PAR_DEFAUT : saisie.trim().toLowerCase();
        s = s.replace(" ", "").replace('h', ':');
        if (s.endsWith(":")) {
            s = s + "00";
        }
        String[] parties = s.split(":");
        try {
            int h = Integer.parseInt(parties[0]);
            int m = parties.length > 1 ? Integer.parseInt(parties[1]) : 0;
            if (parties.length > 2 || h < 0 || h > 23 || m < 0 || m > 59) {
                return null;
            }
            return java.time.LocalTime.of(h, m);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** « 20:00 » : l'heure proposée pour une journée (la fin théorique la plus tardive, sinon 20:00). */
    static String finViseeProposee(java.util.Collection<LocalDateTime> finsTheoriques) {
        LocalDateTime max = null;
        for (LocalDateTime t : finsTheoriques) {
            if (t != null && (max == null || t.isAfter(max))) {
                max = t;
            }
        }
        return max == null ? FIN_VISEE_PAR_DEFAUT : String.format("%02d:%02d", max.getHour(), max.getMinute());
    }

    /** « Samedi 10 avril » : libellé d'une journée. */
    static String libelleJour(LocalDate jour) {
        String j = jour.getDayOfWeek().getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.FRENCH);
        String m = jour.getMonth().getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.FRENCH);
        return Character.toUpperCase(j.charAt(0)) + j.substring(1) + " " + jour.getDayOfMonth() + " " + m;
    }

    /**
     * Une ligne par tableau pour une stratégie : début prévu et retard moyen,
     * fin moyenne, fin tard (cas rare), heure à ne pas dépasser, attente, risque.
     */
    record LigneTableauStat(String nom, String debut, String finMoyenne, String finTard, String finSouhaitee,
                            String attente, String risque, StatutPastille statut) {
    }

    /** @return une ligne par tableau, dans l'ordre du tournoi. */
    static List<LigneTableauStat> lignesTableaux(StatistiquesSimulation s) {
        boolean jours = plusieursJours(List.of(s));
        List<LigneTableauStat> lignes = new ArrayList<>();
        for (StatTableau t : s.tableaux()) {
            double p = t.probabiliteDebordement();
            StatutPastille statut = p < 0.1 ? StatutPastille.SUCCES
                    : (p < 0.5 ? StatutPastille.ATTENTION : StatutPastille.ERREUR);
            String risque = p == 0 ? "Jamais" : pourcent(p) + " des fois";
            String debut = t.debutPrevu() == null ? "—" : heure(t.debutPrevu(), jours);
            if (t.retardDebutMoyen() < 0) {
                debut += " (ne commence pas)";
            } else if (Math.round(t.retardDebutMoyen()) > StatistiquesSimulation.RETARD_A_L_HEURE) {
                debut += " (+" + minutes(Math.round(t.retardDebutMoyen())) + ")";
            } else {
                debut += " (à l'heure)";
            }
            lignes.add(new LigneTableauStat(t.nom(), debut,
                    t.finMoyenne() == null ? "—" : heure(t.finMoyenne(), jours),
                    t.finTard() == null ? "—" : heure(t.finTard(), jours),
                    t.finSouhaitee() == null ? "—" : heure(t.finSouhaitee(), jours),
                    minutes(Math.round(t.attenteMoyenne())), risque, statut));
        }
        return lignes;
    }

    /** @return la phrase sous le tableau des stratégies (tirages, tables, loi des durées, blocages). */
    static String syntheseStatistique(List<StatistiquesSimulation> stats, LoiDureeMatch loi) {
        if (stats.isEmpty()) {
            return "";
        }
        StatistiquesSimulation s = stats.get(0);
        StringBuilder sb = new StringBuilder();
        sb.append(s.tirages()).append(s.tirages() > 1 ? " tirages" : " tirage").append(" par stratégie sur ")
                .append(s.nombreDeTables()).append(s.nombreDeTables() > 1 ? " tables" : " table");
        if (loi != null) {
            sb.append(" ; un match en cinq manches dure ").append(minutes(loi.getMoyenne().toMinutes()))
                    .append(" en moyenne, de ").append(minutes(loi.getMinimum().toMinutes())).append(" à ")
                    .append(minutes(loi.getMaximum().toMinutes()));
        }
        sb.append('.');
        if (s.tempsAnnexesMoyen() > 0) {
            sb.append(" En plus du jeu, chaque match occupe sa table ").append(minutesDecimales(s.tempsAnnexesMoyen()))
                    .append(" en moyenne : appel des joueurs, échauffement, pauses, temps morts et retour de "
                            + "la feuille.");
        }
        int bloques = stats.stream().mapToInt(StatistiquesSimulation::tiragesIncomplets).sum();
        if (bloques > 0) {
            sb.append(" ").append(bloques).append(" tirage(s) n'ont pas pu aller au bout.");
        }
        return sb.toString();
    }

    /** « 6 min 30 » pour 6,5 minutes. */
    static String minutesDecimales(double minutes) {
        long secondes = Math.round(minutes * 60);
        long m = secondes / 60;
        long s = Math.round((secondes % 60) / 10.0) * 10;
        if (s == 60) {
            m++;
            s = 0;
        }
        return s == 0 ? minutes(m) : m + " min " + s;
    }

    static String pourcent(double p) {
        long v = Math.round(p * 100);
        return (v == 0 && p > 0 ? "< 1" : String.valueOf(v)) + " %";
    }

    private static long secondes(LocalDateTime t) {
        return t == null ? Long.MAX_VALUE : t.toEpochSecond(java.time.ZoneOffset.UTC);
    }

    private static boolean plusieursJours(List<StatistiquesSimulation> stats) {
        LocalDate premier = null;
        for (StatistiquesSimulation s : stats) {
            for (StatTableau t : s.tableaux()) {
                for (LocalDateTime h : new LocalDateTime[]{t.debutPrevu(), t.finSouhaitee(), t.finMoyenne(), t.finTard()}) {
                    if (h == null) {
                        continue;
                    }
                    if (premier == null) {
                        premier = h.toLocalDate();
                    } else if (!premier.equals(h.toLocalDate())) {
                        return true;
                    }
                }
            }
        }
        return false;
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
