package com.pingmanager.ui.ecran;

import com.pingmanager.core.ComparaisonStrategies;
import com.pingmanager.core.PrevisionTournoi.EffectifTableau;
import com.pingmanager.core.PrevisionTournoi.PrevisionJournee;
import com.pingmanager.core.RemplissageSimulation;
import com.pingmanager.core.ResultatSimulation;
import com.pingmanager.core.ResultatSimulation.BilanTableau;
import com.pingmanager.core.SimulateurTournoi;
import com.pingmanager.core.StatistiquesDetaillees;
import com.pingmanager.core.StatistiquesSimulation;
import com.pingmanager.core.StatistiquesSimulation.StatTableau;
import com.pingmanager.model.StrategieOrdonnancement;
import com.pingmanager.model.Tournoi;
import com.pingmanager.ordonnancement.LoiDureeMatch;
import com.pingmanager.ordonnancement.TempsAnnexesMatch;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Présentation (sans JavaFX) de l'écran « Prévisions et stratégie » (B025,
 * refait en B050) : les trois cartes de stratégie, l'explication du calcul, la
 * lecture des réglages (tables, durées, fin visée jour et heure, temps autour
 * du match par groupes, remplissage des tableaux) et les statistiques des trois
 * stratégies regroupées par thème.
 */
final class PresentationPrevisions {

    static final String TITRE = "Prévisions et stratégie";
    static final String TITRE_CARTE = "Stratégie d'ordonnancement";
    static final int TABLES_PAR_DEFAUT = 16;
    static final String ERREUR_TABLES = "Indiquez un nombre de tables entre 1 et 200.";
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

    private static Double nombre(String saisie, String defaut) {
        String s = saisie == null || saisie.isBlank() ? defaut : saisie.trim();
        try {
            double v = Double.parseDouble(s.replace(',', '.'));
            return Double.isFinite(v) ? Double.valueOf(v) : null;
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

    /**
     * Ce que l'on compare tableau par tableau, un indicateur à la fois : une
     * seule valeur par case, pour lire d'un coup d'œil quelle stratégie fait
     * mieux sur chaque tableau.
     */
    enum IndicateurTableau {
        FIN("Fin moyenne", "Heure moyenne de la finale du tableau. En gras : la plus tôt."),
        FIN_TARD("Fin au plus tard", "Cas rare : 1 tirage sur 20 finit plus tard. En gras : la plus tôt."),
        RETARD("Retard au début", "Retard moyen du lancement de la dernière poule sur l'heure prévue. "
                + "En gras : le plus faible."),
        ATTENTE("Attente", "Attente moyenne d'un joueur du tableau avant chaque match. En gras : la plus courte."),
        DEPASSEMENT("Après l'heure visée", "Part des tirages où le tableau finit après l'heure visée de la journée. "
                + "En gras : la plus faible.");

        private final String libelle;
        private final String explication;

        IndicateurTableau(String libelle, String explication) {
            this.libelle = libelle;
            this.explication = explication;
        }

        String libelle() {
            return libelle;
        }

        String explication() {
            return explication;
        }
    }

    /**
     * Une ligne de la grille par tableau : nom, début prévu, la valeur de chaque
     * stratégie (dans l'ordre reçu), l'index de la meilleure (-1 à égalité) et,
     * pour le dépassement, la couleur de chaque valeur ({@code null} sinon).
     */
    record LigneIndicateur(String nom, String debutPrevu, List<String> valeurs, int meilleure,
                           List<StatutPastille> statuts) {
        LigneIndicateur {
            valeurs = List.copyOf(valeurs);
            statuts = statuts == null ? null : List.copyOf(statuts);
        }
    }

    /**
     * @return une ligne par tableau pour l'indicateur choisi, triées par heure de
     *         début prévue ; les heures sont données dans la journée du tableau
     *         (« 2 h 41 (lendemain) » après minuit).
     */
    static List<LigneIndicateur> grilleIndicateur(List<StatistiquesSimulation> stats, IndicateurTableau indicateur) {
        List<LigneIndicateur> lignes = new ArrayList<>();
        if (stats.isEmpty()) {
            return lignes;
        }
        List<Integer> ordre = new ArrayList<>();
        for (int k = 0; k < stats.get(0).tableaux().size(); k++) {
            ordre.add(k);
        }
        List<StatTableau> reference = stats.get(0).tableaux();
        ordre.sort(java.util.Comparator.comparing((Integer k) -> secondes(reference.get(k).debutPrevu()))
                .thenComparing(k -> reference.get(k).nom()));
        for (int k : ordre) {
            StatTableau ref = reference.get(k);
            LocalDate jour = ref.debutPrevu() == null ? null : ref.debutPrevu().toLocalDate();
            List<String> valeurs = new ArrayList<>();
            List<StatutPastille> statuts = indicateur == IndicateurTableau.DEPASSEMENT ? new ArrayList<>() : null;
            int meilleure = -1;
            long meilleurScore = Long.MAX_VALUE;
            boolean egalite = false;
            for (int i = 0; i < stats.size(); i++) {
                StatTableau t = stats.get(i).tableaux().get(k);
                long score;
                String texte;
                if (indicateur == IndicateurTableau.FIN) {
                    score = secondes(t.finMoyenne()) / 60;
                    texte = heureDuJour(t.finMoyenne(), jour);
                } else if (indicateur == IndicateurTableau.FIN_TARD) {
                    score = secondes(t.finTard()) / 60;
                    texte = heureDuJour(t.finTard(), jour);
                } else if (indicateur == IndicateurTableau.RETARD) {
                    score = t.retardDebutMoyen() < 0 ? Long.MAX_VALUE : Math.round(t.retardDebutMoyen());
                    texte = t.retardDebutMoyen() < 0 ? "Ne commence pas"
                            : (score <= StatistiquesSimulation.RETARD_A_L_HEURE ? "À l'heure" : "+ " + minutes(score));
                } else if (indicateur == IndicateurTableau.ATTENTE) {
                    score = Math.round(t.attenteMoyenne());
                    texte = minutes(score);
                } else {
                    double p = t.probabiliteDebordement();
                    score = Math.round(p * 100);
                    texte = p == 0 ? "Jamais" : pourcent(p);
                    statuts.add(p < 0.1 ? StatutPastille.SUCCES
                            : (p < 0.5 ? StatutPastille.ATTENTION : StatutPastille.ERREUR));
                }
                valeurs.add(texte);
                if (score < meilleurScore) {
                    meilleurScore = score;
                    meilleure = i;
                    egalite = false;
                } else if (score == meilleurScore) {
                    egalite = true;
                }
            }
            lignes.add(new LigneIndicateur(ref.nom(), ref.debutPrevu() == null ? "—" : heure(ref.debutPrevu(), false),
                    valeurs, egalite ? -1 : meilleure, statuts));
        }
        return lignes;
    }

    /** « 14 h 13 » dans la journée {@code jour}, « 2 h 41 (lendemain) » le jour suivant. */
    static String heureDuJour(LocalDateTime t, LocalDate jour) {
        if (t == null) {
            return "—";
        }
        if (jour == null || t.toLocalDate().equals(jour)) {
            return heure(t, false);
        }
        long jours = java.time.temporal.ChronoUnit.DAYS.between(jour, t.toLocalDate());
        if (jours == 1) {
            return heure(t, false) + " (lendemain)";
        }
        return heure(t, true);
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
    // B050 : les trois cartes de stratégie
    // ------------------------------------------------------------------

    /** Deux lignes sous le titre de l'écran. */
    static final String MICRO_DESCRIPTION = "La stratégie décide de l'ordre des matchs proposés en salle le jour J.\n"
            + "Choisissez celle du club, puis comparez les trois avec les prévisions : vous pouvez en changer "
            + "à tout moment.";
    static final String BOUTON_CHOISIR = "Choisir cette stratégie";
    static final String MARQUE_CHOISIE = "Stratégie choisie";
    static final String MARQUE_PAR_DEFAUT = "Appliquée par défaut";
    static final String RUBRIQUE_LANCEMENT = "Comment elle lance les matchs";
    static final String RUBRIQUE_TOURNOI = "Pour quel tournoi";
    static final String RUBRIQUE_POINT_FAIBLE = "Son point faible";

    /**
     * Une carte de stratégie : nom, trois lignes d'information, et son état.
     *
     * @param choisie vrai si le club a choisi cette stratégie (bouton inactif, carte marquée)
     * @param marque  « Stratégie choisie », « Appliquée par défaut » tant que rien n'est choisi, sinon {@code null}
     */
    record CarteStrategie(StrategieOrdonnancement strategie, String nom, String lancement, String pourQuelTournoi,
                          String pointFaible, boolean choisie, String marque) {
    }

    /** @return les trois cartes, dans l'ordre de l'énumération. */
    static List<CarteStrategie> cartes(Tournoi tournoi) {
        StrategieOrdonnancement actuelle = tournoi.getStrategieOrdonnancement();
        boolean choisie = tournoi.strategieOrdonnancementChoisie();
        List<CarteStrategie> cartes = new ArrayList<>();
        for (StrategieOrdonnancement s : StrategieOrdonnancement.values()) {
            boolean laSienne = s == actuelle;
            cartes.add(new CarteStrategie(s, s.getLibelle(), lancement(s), pourQuelTournoi(s), s.getContrepartie(),
                    laSienne && choisie, laSienne ? (choisie ? MARQUE_CHOISIE : MARQUE_PAR_DEFAUT) : null));
        }
        return cartes;
    }

    private static String lancement(StrategieOrdonnancement s) {
        if (s == StrategieOrdonnancement.AVANCER_TABLEAUX_COMMENCES) {
            return "Les tableaux commencés le plus tôt passent en premier, tours les plus avancés d'abord ; "
                    + "chaque tableau joue sa finale dès qu'elle est prête.";
        }
        if (s == StrategieOrdonnancement.FAIRE_AVANCER_PETIT_A_PETIT) {
            return "Tous les tableaux avancent ensemble, par vagues : les poules, puis chaque tour une fois le "
                    + "précédent entièrement fini. Les finales sont gardées pour la fin.";
        }
        if (s == StrategieOrdonnancement.MINIMISER_ATTENTE) {
            return "Les matchs qui attendent depuis le plus longtemps partent d'abord, ainsi que ceux des "
                    + "joueurs attendus dans un autre tableau.";
        }
        return s.getExplication();
    }

    private static String pourQuelTournoi(StrategieOrdonnancement s) {
        if (s == StrategieOrdonnancement.AVANCER_TABLEAUX_COMMENCES) {
            return "Beaucoup de tableaux qui se succèdent dans la journée : les premiers finissent vite, leurs "
                    + "joueurs repartent tôt et la salle se libère pour les suivants.";
        }
        if (s == StrategieOrdonnancement.FAIRE_AVANCER_PETIT_A_PETIT) {
            return "Un tournoi qui veut toutes ses finales ensemble en fin de journée, avec assez de tables "
                    + "pour ne pas être pressé par l'heure.";
        }
        if (s == StrategieOrdonnancement.MINIMISER_ATTENTE) {
            return "Beaucoup de joueurs inscrits dans plusieurs tableaux, ou un club qui fait passer le "
                    + "confort des joueurs avant tout.";
        }
        return "Tout tournoi.";
    }

    // ------------------------------------------------------------------
    // B050 : explication du calcul
    // ------------------------------------------------------------------

    static final String TITRE_PREVISIONS = "Prévisions";
    static final String TITRE_EXPLICATION = "Comment sont calculées ces prévisions";

    /** Un paragraphe de l'explication : un titre court et son texte. */
    record Explication(String titre, String texte) {
    }

    /** @return l'explication du calcul, dans l'ordre de lecture. */
    static List<Explication> explications() {
        int n = SimulateurTournoi.NOMBRE_DE_TIRAGES;
        return List.of(
                new Explication("Le tournoi est rejoué " + n + " fois",
                        "Chaque journée est jouée " + n + " fois avec chacune des trois stratégies. À chaque "
                                + "tirage : d'autres poules, d'autres vainqueurs (tirés selon les points des "
                                + "joueurs) et d'autres durées de match. Les trois stratégies jouent exactement "
                                + "les mêmes " + n + " tirages : les écarts entre elles viennent de la stratégie "
                                + "seule, pas du hasard."),
                new Explication("La durée d'un match",
                        "Elle est tirée au hasard entre le minimum et le maximum que vous réglez. La plupart "
                                + "des matchs durent un peu plus que le minimum ; les matchs longs sont de plus "
                                + "en plus rares à l'approche du maximum. Sur l'ensemble des matchs, la moyenne "
                                + "retombe sur la durée moyenne réglée. Ces durées valent pour un match en cinq "
                                + "manches ; en trois manches : deux tiers, en sept manches : quatre tiers."),
                new Explication("Le temps autour du match",
                        "À la durée du jeu s'ajoutent, pour chaque match, l'appel des joueurs (et parfois un "
                                + "rappel), l'échauffement, les pauses entre les manches, les temps morts et le "
                                + "retour de la feuille : pendant ce temps, la table et les joueurs restent "
                                + "occupés."),
                new Explication("Lire les chiffres",
                        "Moyenne : la moyenne des " + n + " tirages. Médiane : une fois sur deux, c'est fini "
                                + "avant. Au plus tôt et au plus tard : le tirage le plus rapide et le plus lent "
                                + "des " + n + ". Cas rare : 1 tirage sur 20 fait mieux (ou pire). Risque : la "
                                + "part des tirages qui dépassent la fin visée (12 % = 12 tirages sur " + n
                                + ")."),
                new Explication("Une aide, pas une promesse",
                        "Ces prévisions comparent les stratégies à réglages égaux. Le jour J, le juge-arbitre "
                                + "reste maître de l'ordre des parties."));
    }

    /** @return le message affiché pendant le calcul. */
    static String messageSimulationEnCours() {
        return "Simulation en cours : chaque journée est rejouée " + SimulateurTournoi.NOMBRE_DE_TIRAGES
                + " fois avec les trois stratégies…";
    }

    // ------------------------------------------------------------------
    // B050 : fin visée d'une journée, jour et heure
    // ------------------------------------------------------------------

    static final String AIDE_FIN_VISEE = "Chaque journée est simulée seule. Sa fin visée peut tomber le lendemain "
            + "(un samedi fini à 1 h du matin vise dimanche 1 h). Le risque de dépassement se mesure sur cet "
            + "instant.";

    /** Fin visée proposée à l'écran pour une journée : l'heure (« 20:00 ») et vrai si elle tombe le lendemain. */
    record FinProposee(LocalDate journee, String heure, boolean lendemain) {
    }

    /** @return une proposition par journée, à partir des fins visées proposées par le simulateur. */
    static List<FinProposee> finsProposees(Map<LocalDate, LocalDateTime> proposees) {
        List<FinProposee> liste = new ArrayList<>();
        for (Map.Entry<LocalDate, LocalDateTime> e : new java.util.TreeMap<>(proposees).entrySet()) {
            LocalDateTime t = e.getValue();
            if (t == null) {
                liste.add(new FinProposee(e.getKey(), FIN_VISEE_PAR_DEFAUT, false));
            } else {
                liste.add(new FinProposee(e.getKey(), String.format("%02d:%02d", t.getHour(), t.getMinute()),
                        t.toLocalDate().isAfter(e.getKey())));
            }
        }
        return liste;
    }

    /** « Le jour même (samedi 10 avril) » ou « Le lendemain (dimanche 11 avril) ». */
    static String libelleChoixJour(LocalDate journee, boolean lendemain) {
        return lendemain ? "Le lendemain (" + jourEnClair(journee.plusDays(1)) + ")"
                : "Le jour même (" + jourEnClair(journee) + ")";
    }

    /** Fin visée lue à l'écran (jour et heure), ou le message d'erreur à afficher. */
    record ReglageFinVisee(LocalDateTime instant, String erreur) {
    }

    /**
     * @param journee   journée de compétition
     * @param heure     heure saisie (« 20:00 », « 1 h », « 1h30 »)
     * @param lendemain vrai si l'heure tombe le lendemain de la journée
     * @return l'instant visé, ou l'erreur (heure vide ou illisible)
     */
    static ReglageFinVisee lireFinVisee(LocalDate journee, String heure, boolean lendemain) {
        LocalTime h = heure == null || heure.isBlank() ? null : lireFinVisee(heure);
        if (h == null) {
            return new ReglageFinVisee(null, libelleJour(journee) + " : " + ERREUR_FIN_VISEE);
        }
        return new ReglageFinVisee(com.pingmanager.core.ReglagesPrevision.instantFinVisee(journee, h, lendemain),
                null);
    }

    /** « Samedi 10 avril, fin visée dimanche 11 avril à 1 h ». */
    static String phraseFinVisee(LocalDate journee, LocalDateTime instant) {
        if (instant == null) {
            return libelleJour(journee);
        }
        return libelleJour(journee) + ", fin visée " + jourEnClair(instant.toLocalDate()) + " à "
                + heureCourte(instant.toLocalTime());
    }

    /** Aperçu sous les champs : la phrase de fin visée, ou l'erreur de saisie. */
    static String apercuFinVisee(LocalDate journee, String heure, boolean lendemain) {
        ReglageFinVisee r = lireFinVisee(journee, heure, lendemain);
        return r.erreur() != null ? r.erreur() : phraseFinVisee(journee, r.instant());
    }

    /** « samedi 10 avril ». */
    static String jourEnClair(LocalDate jour) {
        String j = jour.getDayOfWeek().getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.FRENCH);
        String m = jour.getMonth().getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.FRENCH);
        return j + " " + jour.getDayOfMonth() + " " + m;
    }

    /** « 1 h », « 20 h 30 ». */
    static String heureCourte(LocalTime h) {
        return h.getMinute() == 0 ? h.getHour() + " h"
                : h.getHour() + " h " + (h.getMinute() < 10 ? "0" : "") + h.getMinute();
    }

    // ------------------------------------------------------------------
    // B050 : temps autour du match, par groupes
    // ------------------------------------------------------------------

    /** Nombre de manches du match de référence pour les moyennes affichées. */
    static final int MANCHES_REFERENCE = 5;
    static final String UNITE_MINUTES = "min";
    static final String AIDE_ANNEXES = "Ajoutés à la durée du jeu : la table et les joueurs restent occupés. "
            + "Échauffement de 2 min avant la partie (3.4.3.1), pause d'une minute au plus entre deux manches "
            + "(3.4.4.1.1), un temps mort d'une minute au plus par joueur (3.4.4.2).";

    /**
     * Un champ de temps autour du match.
     *
     * @param cle     clé de la variable du cœur ({@link TempsAnnexesMatch.Variable#cle()})
     * @param libelle libellé avec l'unité (« Appel des joueurs (min) »)
     * @param unite   « min », « s » ou « % »
     * @param valeur  valeur proposée, dans cette unité (« 1,5 »)
     */
    record ChampAnnexe(String cle, String libelle, String unite, String valeur) {
    }

    /** Un groupe de champs (« Avant le match »…) et ce qu'il coûte en moyenne par match. */
    record GroupeAnnexes(String nom, List<ChampAnnexe> champs, String moyenne) {
        GroupeAnnexes {
            champs = List.copyOf(champs);
        }
    }

    /**
     * @return les groupes de champs à afficher, lus dans le cœur
     *         ({@link TempsAnnexesMatch#groupes}) : une variable ajoutée au cœur
     *         apparaît ici sans toucher à l'écran.
     */
    static List<GroupeAnnexes> groupesAnnexes() {
        List<GroupeAnnexes> groupes = new ArrayList<>();
        for (TempsAnnexesMatch.Groupe g : TempsAnnexesMatch.PAR_DEFAUT.groupes(MANCHES_REFERENCE)) {
            List<ChampAnnexe> champs = new ArrayList<>();
            for (TempsAnnexesMatch.Variable v : g.variables()) {
                String unite = uniteAffichee(v);
                champs.add(new ChampAnnexe(v.cle(), v.libelle() + " (" + unite + ")", unite,
                        nombre(v.valeur() / diviseur(unite))));
            }
            groupes.add(new GroupeAnnexes(g.nom(), champs, moyenneGroupe(g.moyenneSecondes())));
        }
        return groupes;
    }

    /** Unité d'affichage, fixée par la valeur de départ : minutes dès une minute, sinon secondes. */
    private static String uniteAffichee(TempsAnnexesMatch.Variable v) {
        if (TempsAnnexesMatch.UNITE_PROBABILITE.equals(v.unite())) {
            return TempsAnnexesMatch.UNITE_PROBABILITE;
        }
        return v.valeur() >= 60 ? UNITE_MINUTES : TempsAnnexesMatch.UNITE_SECONDES;
    }

    /** Par combien diviser la valeur du cœur (secondes, ou 0 à 1) pour l'afficher dans l'unité. */
    private static double diviseur(String unite) {
        if (UNITE_MINUTES.equals(unite)) {
            return 60;
        }
        return TempsAnnexesMatch.UNITE_PROBABILITE.equals(unite) ? 0.01 : 1;
    }

    /**
     * @param saisies valeur saisie par clé de variable, dans l'unité affichée ;
     *                une clé absente ou vide garde la valeur de départ
     * @return les temps autour du match, ou l'erreur à afficher
     */
    static ReglageAnnexes lireTempsAnnexes(Map<String, String> saisies) {
        TempsAnnexesMatch temps = TempsAnnexesMatch.PAR_DEFAUT;
        try {
            for (TempsAnnexesMatch.Groupe g : TempsAnnexesMatch.PAR_DEFAUT.groupes(MANCHES_REFERENCE)) {
                for (TempsAnnexesMatch.Variable v : g.variables()) {
                    String saisie = saisies.get(v.cle());
                    if (saisie == null || saisie.isBlank()) {
                        continue;
                    }
                    Double valeur = nombre(saisie, "");
                    if (valeur == null) {
                        return new ReglageAnnexes(null, ERREUR_ANNEXES);
                    }
                    temps = temps.avec(v.cle(), valeur * diviseur(uniteAffichee(v)));
                }
            }
        } catch (IllegalArgumentException e) {
            return new ReglageAnnexes(null, e.getMessage());
        }
        return new ReglageAnnexes(temps, null);
    }

    /** Ce que coûtent les temps saisis : une moyenne par groupe (dans l'ordre des groupes) et le total. */
    record ResumeAnnexes(List<String> moyennes, String total) {
        ResumeAnnexes {
            moyennes = List.copyOf(moyennes);
        }
    }

    /** @return le résumé des temps saisis ; sur une saisie invalide, le total porte l'erreur. */
    static ResumeAnnexes resumeAnnexes(Map<String, String> saisies) {
        ReglageAnnexes r = lireTempsAnnexes(saisies);
        if (r.erreur() != null) {
            return new ResumeAnnexes(List.of(), r.erreur());
        }
        List<String> moyennes = new ArrayList<>();
        for (TempsAnnexesMatch.Groupe g : r.temps().groupes(MANCHES_REFERENCE)) {
            moyennes.add(moyenneGroupe(g.moyenneSecondes()));
        }
        return new ResumeAnnexes(moyennes, "Total autour d'un match joué en " + MANCHES_REFERENCE + " manches : "
                + minutesDecimales(r.temps().totalParMatch(MANCHES_REFERENCE) / 60.0)
                + " en moyenne, en plus de la durée du jeu.");
    }

    private static String moyenneGroupe(double secondes) {
        return minutesDecimales(secondes / 60.0) + " en moyenne par match";
    }

    // ------------------------------------------------------------------
    // B050 : remplissage des tableaux
    // ------------------------------------------------------------------

    static final String AIDE_REMPLISSAGE = "Hypothèse de prévision, rien n'est modifié dans le tournoi. Avec un "
            + "taux, la simulation joue « places × taux » joueurs dans chaque tableau limité en places : elle "
            + "complète avec des joueurs fictifs s'il y a moins d'inscrits, elle écarte les derniers inscrits "
            + "s'il y en a plus. Le taux d'un tableau prime sur le taux global.";
    static final String ERREUR_TAUX = "Indiquez un taux de remplissage entre 0 et 100 % (par exemple 80).";
    static final String INVITE_TAUX_TABLEAU = "Taux global";

    /** Choix global de remplissage. Le taux est porté par la constante (pas de {@code switch}). */
    enum ChoixRemplissage {
        INSCRITS_REELS("Inscrits réels", null),
        PLEIN("100 %", RemplissageSimulation.TAUX_PLEIN),
        QUATRE_VINGT_QUINZE("95 %", RemplissageSimulation.TAUX_95),
        AUTRE("Autre valeur", null);

        private final String libelle;
        private final Double taux;

        ChoixRemplissage(String libelle, Double taux) {
            this.libelle = libelle;
            this.taux = taux;
        }

        String libelle() {
            return libelle;
        }
    }

    /** Remplissage lu à l'écran, ou le message d'erreur à afficher. */
    record ReglageRemplissage(RemplissageSimulation remplissage, String erreur) {
    }

    /** @return le taux saisi (« 80 », « 80 % », « 87,5 ») entre 0 et 1, ou {@code null} s'il est invalide. */
    static Double lireTaux(String saisie) {
        if (saisie == null || saisie.isBlank()) {
            return null;
        }
        Double v = nombre(saisie.replace("%", "").trim(), "");
        return v == null || v.isNaN() || v < 0 || v > 100 ? null : Double.valueOf(v / 100);
    }

    /**
     * @param choix      choix global
     * @param autre      taux saisi quand le choix est {@link ChoixRemplissage#AUTRE}
     * @param parTableau taux propre saisi par nom de tableau (vide : le taux global s'applique)
     */
    static ReglageRemplissage lireRemplissage(ChoixRemplissage choix, String autre, Map<String, String> parTableau) {
        RemplissageSimulation r = RemplissageSimulation.INSCRITS_REELS;
        if (choix == ChoixRemplissage.AUTRE) {
            Double taux = lireTaux(autre);
            if (taux == null) {
                return new ReglageRemplissage(null, "Taux global : " + ERREUR_TAUX);
            }
            r = RemplissageSimulation.global(taux);
        } else if (choix != null && choix.taux != null) {
            r = RemplissageSimulation.global(choix.taux);
        }
        for (Map.Entry<String, String> e : parTableau.entrySet()) {
            if (e.getValue() == null || e.getValue().isBlank()) {
                continue;
            }
            Double taux = lireTaux(e.getValue());
            if (taux == null) {
                return new ReglageRemplissage(null, "Tableau « " + e.getKey() + " » : " + ERREUR_TAUX);
            }
            r = r.avecTableau(e.getKey(), taux);
        }
        return new ReglageRemplissage(r, null);
    }

    /**
     * Aperçu d'un tableau : ce que la simulation jouera.
     *
     * @param reglable faux pour un tableau sans limite de places (un taux n'y change rien)
     * @param effet    « + 4 fictifs », « 3 inscrits écartés » ou « Inscrits réels »
     */
    record LigneRemplissage(String nom, String inscrits, String capacite, String taux, String simules, String effet,
                            StatutPastille statut, boolean reglable) {
    }

    /** @return une ligne d'aperçu par tableau, dans l'ordre reçu (celui du tournoi). */
    static List<LigneRemplissage> lignesRemplissage(List<EffectifTableau> effectifs) {
        List<LigneRemplissage> lignes = new ArrayList<>();
        for (EffectifTableau e : effectifs) {
            String effet;
            StatutPastille statut;
            if (e.fictifs() > 0) {
                effet = "+ " + e.fictifs() + (e.fictifs() > 1 ? " fictifs" : " fictif");
                statut = StatutPastille.INFO;
            } else if (e.ecartes() > 0) {
                effet = e.ecartes() + (e.ecartes() > 1 ? " inscrits écartés" : " inscrit écarté");
                statut = StatutPastille.ATTENTION;
            } else {
                effet = "Inscrits réels";
                statut = StatutPastille.NEUTRE;
            }
            boolean limite = e.capacite() > 0;
            lignes.add(new LigneRemplissage(e.nom(), String.valueOf(e.inscrits()),
                    limite ? String.valueOf(e.capacite()) : "Sans limite",
                    !limite || e.taux() < 0 ? "—" : pourcent(e.taux()), String.valueOf(e.simules()), effet, statut,
                    limite));
        }
        return lignes;
    }

    /** « 184 inscriptions simulées pour 152 réelles : 36 fictifs ajoutés, 4 inscrits écartés. » (tableaux cumulés). */
    static String resumeRemplissage(List<EffectifTableau> effectifs) {
        int inscrits = 0;
        int simules = 0;
        int fictifs = 0;
        int ecartes = 0;
        for (EffectifTableau e : effectifs) {
            inscrits += e.inscrits();
            simules += e.simules();
            fictifs += e.fictifs();
            ecartes += e.ecartes();
        }
        if (effectifs.isEmpty()) {
            return "Aucun tableau à simuler.";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(simules).append(" inscriptions simulées pour ").append(inscrits).append(" réelles");
        if (fictifs == 0 && ecartes == 0) {
            return sb.append(" : les tableaux sont joués avec leurs inscrits.").toString();
        }
        sb.append(" : ");
        if (fictifs > 0) {
            sb.append(fictifs).append(fictifs > 1 ? " fictifs ajoutés" : " fictif ajouté");
        }
        if (ecartes > 0) {
            sb.append(fictifs > 0 ? ", " : "").append(ecartes)
                    .append(ecartes > 1 ? " inscrits écartés" : " inscrit écarté");
        }
        return sb.append('.').toString();
    }

    // ------------------------------------------------------------------
    // B050 : statistiques des trois stratégies, par thème
    // ------------------------------------------------------------------

    /** Nombre de tableaux à risque listés par stratégie. */
    static final int TABLEAUX_A_RISQUE_AFFICHES = 3;
    static final String MARQUE_COLONNE_CHOISIE = "Votre stratégie";

    /** Un thème de statistiques : son titre, une phrase d'aide et ses lignes (une valeur par stratégie). */
    record GroupeStatistiques(String titre, String aide, List<LigneStrategies> lignes) {
        GroupeStatistiques {
            lignes = List.copyOf(lignes);
        }
    }

    /**
     * @return les statistiques d'une journée regroupées par thème, dans l'ordre
     *         de l'écran ; les heures sont dites dans la journée (« 1 h 10
     *         (lendemain) » après minuit).
     */
    static List<GroupeStatistiques> groupesStatistiques(PrevisionJournee journee) {
        List<StatistiquesSimulation> stats = journee.strategies();
        LocalDate jour = journee.jour();
        List<GroupeStatistiques> groupes = new ArrayList<>();

        List<LigneStrategies> fin = new ArrayList<>();
        fin.add(ligne("Fin moyenne", stats, s -> secondes(s.finMoyenne()), s -> heureDuJour(s.finMoyenne(), jour)));
        fin.add(ligne("Fin médiane (une fois sur deux, c'est fini avant)", stats, s -> secondes(s.finMediane()),
                s -> heureDuJour(s.finMediane(), jour)));
        fin.add(ligne("Fin au plus tôt (le tirage le plus rapide)", stats, s -> secondes(s.detail().finAuPlusTot()),
                s -> heureDuJour(s.detail().finAuPlusTot(), jour)));
        fin.add(ligne("Fin au plus tard (le tirage le plus lent)", stats, s -> secondes(s.detail().finAuPlusTard()),
                s -> heureDuJour(s.detail().finAuPlusTard(), jour)));
        fin.add(ligne("Cas rare : fin tôt (1 fois sur 20)", stats, s -> secondes(s.finTot()),
                s -> heureDuJour(s.finTot(), jour)));
        fin.add(ligne("Cas rare : fin tard (1 fois sur 20)", stats, s -> secondes(s.finTard()),
                s -> heureDuJour(s.finTard(), jour)));
        fin.add(ligne("Dispersion de l'heure de fin (écart-type)", stats, s -> Math.round(s.detail().ecartTypeFin()),
                s -> "± " + minutes(Math.round(s.detail().ecartTypeFin()))));
        fin.add(ligne("Risque de dépasser la fin visée", stats,
                s -> Math.round(s.detail().probabiliteDepassement() * 100),
                s -> pourcent(s.detail().probabiliteDepassement())));
        fin.add(ligne("Dépassement moyen de la fin visée", stats, s -> Math.round(s.detail().depassementMoyen()),
                s -> minutes(Math.round(s.detail().depassementMoyen()))));
        fin.add(ligne("Plus grand dépassement observé", stats, s -> s.detail().depassementMax(),
                s -> minutes(s.detail().depassementMax())));
        fin.add(ligne("Durée de la journée (du premier début à la dernière partie)", stats,
                s -> s.detail().dureeJourneeMoyenne() < 0 ? Long.MAX_VALUE : Math.round(s.detail().dureeJourneeMoyenne()),
                s -> s.detail().dureeJourneeMoyenne() < 0 ? "—" : minutes(Math.round(s.detail().dureeJourneeMoyenne()))));
        groupes.add(new GroupeStatistiques("Fin de journée", phraseFinVisee(jour, journee.finVisee()) + ".", fin));

        List<LigneStrategies> ponctualite = new ArrayList<>();
        ponctualite.add(ligne("Début à l'heure (" + StatistiquesSimulation.RETARD_A_L_HEURE + " min de retard au plus)",
                stats, s -> -Math.round(s.debutALHeure() * 100), s -> pourcent(s.debutALHeure())));
        ponctualite.add(ligne("Début en léger retard (" + StatistiquesSimulation.RETARD_A_L_HEURE + " à "
                        + StatistiquesSimulation.GROS_RETARD + " min)", stats,
                s -> Math.round(s.debutLegerRetard() * 100), s -> pourcent(s.debutLegerRetard())));
        ponctualite.add(ligne("Début en gros retard (plus de " + StatistiquesSimulation.GROS_RETARD + " min)", stats,
                s -> Math.round(s.debutGrosRetard() * 100), s -> pourcent(s.debutGrosRetard())));
        ponctualite.add(ligne("Retard moyen au début d'un tableau", stats,
                s -> Math.round(s.detail().retardDebutMoyen()), s -> minutes(Math.round(s.detail().retardDebutMoyen()))));
        ponctualite.add(ligne("Plus grand retard de début observé", stats, s -> s.detail().retardDebutMax(),
                s -> minutes(s.detail().retardDebutMax())));
        ponctualite.add(ligne("Fin moyenne d'un tableau", stats, s -> secondes(s.finMoyenneTableau()),
                s -> heureDuJour(s.finMoyenneTableau(), jour)));
        groupes.add(new GroupeStatistiques("Ponctualité des tableaux",
                "Un tableau a vraiment commencé quand sa dernière poule a commencé.", ponctualite));

        List<LigneStrategies> attente = new ArrayList<>();
        attente.add(ligne("Attente moyenne avant un match", stats, s -> Math.round(s.attenteMoyenne()),
                s -> minutes(Math.round(s.attenteMoyenne()))));
        attente.add(ligne("Attente médiane avant un match", stats, s -> Math.round(s.detail().attenteMediane()),
                s -> minutes(Math.round(s.detail().attenteMediane()))));
        attente.add(ligne("Attente moyenne d'un joueur inscrit sur 1 tableau", stats,
                s -> attenteCle(s.attenteUnTableau()), s -> attente(s.attenteUnTableau())));
        attente.add(ligne("Attente moyenne d'un joueur inscrit sur 2 tableaux ou plus", stats,
                s -> attenteCle(s.attentePlusieursTableaux()), s -> attente(s.attentePlusieursTableaux())));
        attente.add(ligne("Plus longue attente d'une journée (moyenne des tirages)", stats,
                s -> Math.round(s.plusLongueAttente()), s -> minutes(Math.round(s.plusLongueAttente()))));
        attente.add(ligne("Plus longue attente observée", stats, s -> s.detail().attenteMaximale(),
                s -> minutes(s.detail().attenteMaximale())));
        attente.add(ligne("Plus longue pause d'un joueur entre deux matchs (moyenne des tirages)", stats,
                s -> Math.round(s.detail().plusLonguePause()), s -> minutes(Math.round(s.detail().plusLonguePause()))));
        attente.add(ligne("Plus longue pause observée", stats, s -> s.detail().plusLonguePauseMax(),
                s -> minutes(s.detail().plusLonguePauseMax())));
        groupes.add(new GroupeStatistiques("Attente des joueurs",
                "L'attente se compte avant chaque match, à partir du moment où il pouvait être lancé.", attente));

        int nbTables = journee.nombreDeTables();
        List<LigneStrategies> tables = new ArrayList<>();
        tables.add(ligne("Occupation des tables", stats, s -> -Math.round(s.occupationMoyenne() * 100),
                s -> Math.round(s.occupationMoyenne() * 100) + " %"));
        tables.add(ligneNeutre("Pic de tables occupées en même temps (moyenne des tirages)", stats,
                s -> nombre(s.detail().picTablesMoyen()) + " sur " + nbTables));
        tables.add(ligneNeutre("Pic le plus haut observé", stats,
                s -> s.detail().picTablesMax() + " sur " + nbTables));
        groupes.add(new GroupeStatistiques("Tables",
                "Un pic égal au nombre de tables : la salle est saturée à ce moment-là.", tables));

        List<LigneStrategies> finales = new ArrayList<>();
        finales.add(ligneNeutre("Première finale lancée (en moyenne)", stats,
                s -> heureDuJour(s.detail().debutPremiereFinale(), jour)));
        finales.add(ligneNeutre("Dernière finale lancée (en moyenne)", stats,
                s -> heureDuJour(s.detail().debutDerniereFinale(), jour)));
        finales.add(ligneNeutre("Premier tableau terminé (en moyenne)", stats,
                s -> heureDuJour(s.premiereFinale(), jour)));
        groupes.add(new GroupeStatistiques("Finales",
                "Finales étalées dans la journée ou regroupées à la fin : c'est un choix, pas un défaut.", finales));

        groupes.add(new GroupeStatistiques("Tableaux à risque",
                "Les tableaux qui finissent le plus souvent après la fin visée, avec leur dépassement moyen.",
                lignesTableauxARisque(stats)));
        groupes.add(new GroupeStatistiques("Matchs par heure",
                "Matchs lancés dans chaque heure, en moyenne sur les tirages.", lignesMatchsParHeure(stats, jour)));
        return groupes;
    }

    private static List<LigneStrategies> lignesTableauxARisque(List<StatistiquesSimulation> stats) {
        List<LigneStrategies> lignes = new ArrayList<>();
        for (int rang = 0; rang < TABLEAUX_A_RISQUE_AFFICHES; rang++) {
            List<String> valeurs = new ArrayList<>();
            boolean unRisque = false;
            for (StatistiquesSimulation s : stats) {
                List<StatistiquesDetaillees.RisqueTableau> risques = s.detail().tableauxARisque();
                StatistiquesDetaillees.RisqueTableau r = rang < risques.size() ? risques.get(rang) : null;
                if (r == null || r.probabiliteDebordement() <= 0) {
                    valeurs.add(rang == 0 ? "Aucun" : "—");
                    continue;
                }
                unRisque = true;
                long depassement = Math.round(r.depassementMoyen());
                valeurs.add(r.nom() + " : " + pourcent(r.probabiliteDebordement())
                        + (depassement > 0 ? ", + " + minutes(depassement) : ""));
            }
            if (rang > 0 && !unRisque) {
                break;
            }
            lignes.add(new LigneStrategies(rang == 0 ? "Le plus à risque" : (rang + 1) + "e plus à risque", valeurs, -1));
        }
        return lignes;
    }

    private static List<LigneStrategies> lignesMatchsParHeure(List<StatistiquesSimulation> stats, LocalDate jour) {
        List<LigneStrategies> lignes = new ArrayList<>();
        lignes.add(ligneNeutre("Heure la plus chargée", stats, s -> {
            StatistiquesDetaillees.PointHoraire p = s.detail().heureLaPlusChargee();
            return p == null ? "—" : trancheHoraire(p.heure(), jour) + " : " + nombre(p.matchs()) + " matchs";
        }));
        java.util.SortedSet<LocalDateTime> heures = new java.util.TreeSet<>();
        for (StatistiquesSimulation s : stats) {
            s.detail().matchsParHeure().forEach(p -> heures.add(p.heure()));
        }
        for (LocalDateTime h : heures) {
            lignes.add(ligneNeutre(trancheHoraire(h, jour), stats, s -> {
                for (StatistiquesDetaillees.PointHoraire p : s.detail().matchsParHeure()) {
                    if (p.heure().equals(h)) {
                        return nombre(p.matchs());
                    }
                }
                return "0";
            }));
        }
        return lignes;
    }

    /** « 14 h – 15 h », « 0 h – 1 h (lendemain) ». */
    static String trancheHoraire(LocalDateTime debut, LocalDate jour) {
        String t = debut.getHour() + " h – " + debut.plusHours(1).getHour() + " h";
        return jour != null && debut.toLocalDate().isAfter(jour) ? t + " (lendemain)" : t;
    }

    private static LigneStrategies ligneNeutre(String indicateur, List<StatistiquesSimulation> stats,
                                               java.util.function.Function<StatistiquesSimulation, String> texte) {
        List<String> valeurs = new ArrayList<>();
        for (StatistiquesSimulation s : stats) {
            valeurs.add(texte.apply(s));
        }
        return new LigneStrategies(indicateur, valeurs, -1);
    }

    /** @return l'index de la colonne de la stratégie choisie dans la journée, ou -1. */
    static int colonneChoisie(List<StatistiquesSimulation> stats, StrategieOrdonnancement choisie) {
        for (int i = 0; i < stats.size(); i++) {
            if (stats.get(i).strategie() == choisie) {
                return i;
            }
        }
        return -1;
    }

    /** « 12,4 », « 13 » : un nombre à une décimale au plus, à la française. */
    static String nombre(double v) {
        double arrondi = Math.round(v * 10) / 10.0;
        if (arrondi == Math.rint(arrondi)) {
            return String.valueOf((long) arrondi);
        }
        return String.valueOf(arrondi).replace('.', ',');
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
