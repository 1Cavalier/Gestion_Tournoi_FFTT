package com.pingmanager.core;

import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.FormatPhaseFinale;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Manche;
import com.pingmanager.model.Match;
import com.pingmanager.model.Poule;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.StatutMatch;
import com.pingmanager.model.StrategieOrdonnancement;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.TableauElimination;
import com.pingmanager.model.Tour;
import com.pingmanager.model.Tournoi;
import com.pingmanager.ordonnancement.CategorieDuree;
import com.pingmanager.ordonnancement.LoiDureeMatch;
import com.pingmanager.ordonnancement.TempsAnnexesMatch;
import com.pingmanager.ordonnancement.MatchOrdonnancable;
import com.pingmanager.ordonnancement.Ordonnanceur;
import com.pingmanager.ordonnancement.PhaseMatch;
import com.pingmanager.ordonnancement.PoidsOrdonnancement;
import com.pingmanager.ordonnancement.PropositionLancement;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.TreeMap;

/**
 * Simulateur de tournoi accéléré (B025) : déroule un tournoi complet — poules,
 * tableaux finaux, N tables — avec l'{@link Ordonnanceur} réglé selon une
 * {@link StrategieOrdonnancement} et les durées de l'{@code EstimateurDuree},
 * sans attendre l'horloge réelle (simulation par événements).
 *
 * <p><b>Reproductible.</b> Tout dépend de la graine du scénario : composition des
 * poules, tirage du tableau final, vainqueurs et durées. Le résultat et la durée
 * d'une partie sont tirés à partir de la graine et de la partie elle-même (tableau,
 * phase, joueurs), pas de l'ordre de lancement : deux stratégies comparées sur la
 * même graine voient exactement les mêmes poules, les mêmes résultats et les
 * mêmes durées ; seul l'ordre des lancements change.</p>
 *
 * <p>Ce n'est qu'une prévision : le juge-arbitre reste maître de la programmation
 * des parties (3.3.1.2.2, IX.109). Vainqueur tiré selon l'écart de points ;
 * aucun forfait ni abandon ; pas de temps de repos imposé (aucun dans les textes
 * fournis pour un tournoi individuel). L'estimateur n'est que lu, jamais modifié.</p>
 */
public final class SimulateurTournoi {

    /** Largeur d'un palier de niveau pour la catégorie de durée (points de classement). */
    public static final int PALIER_NIVEAU = 500;

    /** Heure de début des matchs d'un tableau sans horaire. */
    public static final LocalTime DEBUT_PAR_DEFAUT = LocalTime.of(9, 0);

    /** Nombre de tirages par défaut d'une simulation statistique. */
    public static final int TIRAGES_PAR_DEFAUT = 100;

    /** Simule le scénario avec la stratégie donnée (un seul tirage, celui de la graine). */
    public ResultatSimulation simuler(ScenarioSimulation scenario, StrategieOrdonnancement strategie) {
        Objects.requireNonNull(scenario, "scenario");
        Objects.requireNonNull(strategie, "strategie");
        return new Deroule(scenario, strategie).executer();
    }

    /** Simule le même scénario (même graine) avec deux stratégies. */
    public ComparaisonStrategies comparer(ScenarioSimulation scenario, StrategieOrdonnancement premiere,
                                          StrategieOrdonnancement seconde) {
        return new ComparaisonStrategies(simuler(scenario, premiere), simuler(scenario, seconde));
    }

    /**
     * Simulation statistique (méthode de Monte-Carlo) : rejoue le tournoi
     * {@code tirages} fois avec la stratégie donnée. Chaque tirage a sa propre
     * graine, dérivée de celle du scénario : poules, tableau final, vainqueurs et
     * durées des matchs (loi {@link ScenarioSimulation#getLoiDuree()}) changent
     * d'un tirage à l'autre. Les tirages sont joués en parallèle ; le résultat ne
     * dépend que de la graine du scénario et du nombre de tirages.
     */
    public StatistiquesSimulation simulerPlusieurs(ScenarioSimulation scenario, StrategieOrdonnancement strategie,
                                                   int tirages) {
        Objects.requireNonNull(scenario, "scenario");
        Objects.requireNonNull(strategie, "strategie");
        if (tirages < 1) {
            throw new IllegalArgumentException("Il faut au moins un tirage.");
        }
        List<ResultatSimulation> resultats = java.util.stream.IntStream.range(0, tirages).parallel()
                .mapToObj(k -> simuler(scenario.avecGraine(graineDuTirage(scenario.getGraine(), k)), strategie))
                .toList();
        return StatistiquesSimulation.depuis(strategie, scenario.getNombreDeTables(), resultats);
    }

    /**
     * Simule toutes les stratégies sur les mêmes tirages (tirage k identique pour
     * chaque stratégie : mêmes poules, mêmes résultats, mêmes durées de match),
     * pour que les écarts mesurés ne viennent que de la stratégie.
     */
    public List<StatistiquesSimulation> comparerToutes(ScenarioSimulation scenario, int tirages) {
        List<StatistiquesSimulation> stats = new ArrayList<>();
        for (StrategieOrdonnancement s : StrategieOrdonnancement.values()) {
            stats.add(simulerPlusieurs(scenario, s, tirages));
        }
        return stats;
    }

    /** Graine du tirage k : bien brassée, pour que deux tirages voisins diffèrent vraiment. */
    static long graineDuTirage(long graine, int k) {
        long z = graine + 0x9E3779B97F4A7C15L * (k + 1L);
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    // ------------------------------------------------------------------
    // Scénario depuis un tournoi
    // ------------------------------------------------------------------

    /**
     * Construit le scénario d'un tournoi : chaque tableau qui a un format, avec
     * ses inscrits, son heure de début des matchs (sinon 9 h le premier jour du
     * tournoi) et sa fin théorique.
     *
     * @param completer vrai pour compléter chaque tableau limité jusqu'à ses places
     *                  maximales avec des joueurs fictifs (prévision « tableaux pleins »)
     */
    public static ScenarioSimulation scenarioDepuis(Tournoi tournoi, int nombreDeTables, long graine,
                                                    boolean completer) {
        Objects.requireNonNull(tournoi, "tournoi");
        ScenarioSimulation.Builder b = ScenarioSimulation.builder()
                .nombreDeTables(Math.max(1, nombreDeTables)).graine(graine);
        LocalDate jour = tournoi.getDate() != null ? tournoi.getDate() : LocalDate.of(2000, 1, 1);
        Random fictifs = new Random(graine);
        int index = 0;
        for (Tableau t : tournoi.getTableaux()) {
            index++;
            ConfigurationTableau c = t.getConfiguration();
            if (c.getFormat() == null) {
                continue;
            }
            List<Joueur> joueurs = new ArrayList<>(t.getInscrits());
            if (completer && c.aLimiteDePlaces()) {
                for (int n = joueurs.size() + 1; n <= c.getPlacesMax(); n++) {
                    joueurs.add(joueurFictif(c, index, n, fictifs));
                }
            }
            LocalDateTime debut = t.getHoraires() != null && t.getHoraires().getDebutMatchs() != null
                    ? t.getHoraires().getDebutMatchs() : jour.atTime(DEBUT_PAR_DEFAUT);
            LocalDateTime finSouhaitee = t.getHoraires() != null ? t.getHoraires().getFinTheorique() : null;
            b.tableau(new ScenarioSimulation.TableauSimule(t.getNom(), debut, joueurs, c.getFormat(),
                    c.avecPetiteFinale(), finSouhaitee, Math.max(0, c.getPointsMax())));
        }
        return b.build();
    }

    private static Joueur joueurFictif(ConfigurationTableau c, int tableau, int n, Random r) {
        int min = Math.max(500, c.getPointsMin());
        int max = c.getPointsMax() > 0 ? Math.max(min, c.getPointsMax()) : 2500;
        int points = min + r.nextInt(max - min + 1);
        Sexe sexe = c.getSexeRequis() != null ? c.getSexeRequis() : Sexe.HOMME;
        Joueur j = new Joueur("SIM-" + tableau + "-" + n, "FICTIF " + tableau + "-" + n, "Joueur", sexe,
                "Club fictif " + (n % 40), points);
        return j;
    }

    // ------------------------------------------------------------------
    // Déroulé
    // ------------------------------------------------------------------

    /** État d'un tableau pendant la simulation. */
    private static final class EtatTableau {
        final ScenarioSimulation.TableauSimule def;
        final int rang;
        final LocalDateTime finSouhaitee;
        List<Poule> poules;
        TableauElimination phaseFinale;
        boolean demarre;
        int restants;
        int parties;
        LocalDateTime fin;
        final List<Long> attentes = new ArrayList<>();
        /** Premier lancement de chaque poule (début réel du tableau : la dernière à commencer). */
        final Map<Poule, LocalDateTime> debutPoule = new IdentityHashMap<>();
        final Map<Match, Poule> pouleDe = new IdentityHashMap<>();
        LocalDateTime premierLancement;

        EtatTableau(ScenarioSimulation.TableauSimule def, int rang, LocalTime heureLimite) {
            this.def = def;
            this.rang = rang;
            this.finSouhaitee = def.finSouhaitee() != null ? def.finSouhaitee()
                    : def.debut().toLocalDate().atTime(heureLimite);
        }
    }

    /** Partie en cours sur une table, avec son résultat déjà tiré. */
    private record EnCours(MatchOrdonnancable mo, LocalDateTime fin, List<Manche> manches, long ordre) {
    }

    private final class Deroule {
        private final ScenarioSimulation scenario;
        private final StrategieOrdonnancement strategie;
        private final Ordonnanceur ordonnanceur;
        // Propres à chaque déroulé : les tirages peuvent tourner en parallèle.
        private final MoteurTournoi moteur = new MoteurTournoi();
        private final ProgressionTableau progression = new ProgressionTableau();
        private final List<EtatTableau> etats = new ArrayList<>();
        private final List<ResultatSimulation.TableauIgnore> ignores = new ArrayList<>();
        private final Map<Match, EtatTableau> tableauDe = new IdentityHashMap<>();
        /** Parties encore à jouer, prêtes ou non. */
        private final List<MatchOrdonnancable> aJouer = new ArrayList<>();
        private final PriorityQueue<EnCours> enCours = new PriorityQueue<>(
                Comparator.comparing(EnCours::fin).thenComparingLong(EnCours::ordre));
        private final Map<String, LocalDateTime> libreDepuis = new HashMap<>();
        /** Jour → secondes de jeu, premier lancement, dernière fin. */
        private final Map<LocalDate, long[]> occupation = new TreeMap<>();
        private final Map<LocalDate, LocalDateTime[]> bornes = new TreeMap<>();
        private final List<Long> attentes = new ArrayList<>();
        private final List<Long> attentesUnTableau = new ArrayList<>();
        private final List<Long> attentesPlusieursTableaux = new ArrayList<>();
        /** Licence → nombre de tableaux simulés où le joueur est inscrit. */
        private final Map<String, Integer> tableauxParJoueur = new HashMap<>();
        private long attenteMax = -1;
        private long secondesAnnexes;
        private long matchsLances;
        private String joueurAttenteMax;
        private long sequence;

        Deroule(ScenarioSimulation scenario, StrategieOrdonnancement strategie) {
            this.scenario = scenario;
            this.strategie = strategie;
            this.ordonnanceur = new Ordonnanceur(scenario.getNombreDeTables(), PoidsOrdonnancement.pour(strategie));
            int rang = 0;
            for (ScenarioSimulation.TableauSimule t : scenario.getTableaux()) {
                int minimum = Math.max(2, t.format().taillePouleSouhaitee());
                if (t.joueurs().size() < minimum) {
                    ignores.add(new ResultatSimulation.TableauIgnore(t.nom(),
                            t.joueurs().size() + " joueur(s) pour des poules de "
                                    + t.format().taillePouleSouhaitee()));
                    continue;
                }
                etats.add(new EtatTableau(t, rang++, scenario.getHeureLimite()));
                for (Joueur j : t.joueurs()) {
                    tableauxParJoueur.merge(j.getNumeroLicence(), 1, Integer::sum);
                }
            }
        }

        ResultatSimulation executer() {
            LocalDateTime maintenant = null;
            for (EtatTableau e : etats) {
                if (maintenant == null || e.def.debut().isBefore(maintenant)) {
                    maintenant = e.def.debut();
                }
            }
            LocalDateTime debut = maintenant;
            while (maintenant != null) {
                demarrerTableaux(maintenant);
                poserAttentes();
                for (PropositionLancement p : ordonnanceur.proposerLancements(maintenant)) {
                    lancer(p, maintenant);
                }
                LocalDateTime prochain = prochainEvenement();
                if (prochain == null) {
                    break;
                }
                maintenant = prochain;
                while (!enCours.isEmpty() && !enCours.peek().fin().isAfter(maintenant)) {
                    terminer(enCours.poll());
                }
            }
            return bilan(debut);
        }

        private LocalDateTime prochainEvenement() {
            LocalDateTime prochain = enCours.isEmpty() ? null : enCours.peek().fin();
            for (EtatTableau e : etats) {
                if (!e.demarre && (prochain == null || e.def.debut().isBefore(prochain))) {
                    prochain = e.def.debut();
                }
            }
            return prochain;
        }

        private void demarrerTableaux(LocalDateTime maintenant) {
            for (EtatTableau e : etats) {
                if (e.demarre || e.def.debut().isAfter(maintenant)) {
                    continue;
                }
                e.demarre = true;
                // Tirage des poules reproductible (I.302 : départage aléatoire), graine propre au tableau.
                e.poules = moteur.preparerPoules(e.def.joueurs(), e.def.format(), melanger(e.rang * 2L + 1));
                for (Poule p : e.poules) {
                    for (Match m : p.getMatchs()) {
                        e.pouleDe.put(m, p);
                        ajouter(m, e, PhaseMatch.POULE);
                    }
                }
                verifierFinPoules(e);
            }
        }

        private void ajouter(Match m, EtatTableau e, PhaseMatch phase) {
            if (m.estBye() || m.getStatut() == StatutMatch.TERMINE) {
                return;
            }
            MatchOrdonnancable mo = new MatchOrdonnancable(m, e.def.nom(), e.def.nom(), phase);
            tableauDe.put(m, e);
            aJouer.add(mo);
            e.restants++;
            ordonnanceur.ajouterMatch(mo);
        }

        /**
         * Attente d'une partie prête : depuis que ses deux joueurs sont libres
         * (fin de leur partie précédente) et au plus tôt depuis le début du tableau.
         */
        private void poserAttentes() {
            for (MatchOrdonnancable mo : aJouer) {
                Match m = mo.getMatch();
                if (m.getStatut() != StatutMatch.A_JOUER || inconnu(m.getJoueurA()) || inconnu(m.getJoueurB())) {
                    mo.setPretDepuis(null);
                    continue;
                }
                LocalDateTime pret = tableauDe.get(m).def.debut();
                pret = plusTard(pret, libreDepuis.get(m.getJoueurA().getNumeroLicence()));
                pret = plusTard(pret, libreDepuis.get(m.getJoueurB().getNumeroLicence()));
                mo.setPretDepuis(pret);
            }
        }

        private void lancer(PropositionLancement p, LocalDateTime maintenant) {
            MatchOrdonnancable mo = p.getMatch();
            Match m = mo.getMatch();
            EtatTableau e = tableauDe.get(m);
            ordonnanceur.lancer(p, maintenant);
            if (e.premierLancement == null) {
                e.premierLancement = maintenant;
            }
            Poule poule = e.pouleDe.get(m);
            if (poule != null) {
                e.debutPoule.putIfAbsent(poule, maintenant);
            }
            for (Joueur j : List.of(m.getJoueurA(), m.getJoueurB())) {
                LocalDateTime dispo = plusTard(e.def.debut(), libreDepuis.get(j.getNumeroLicence()));
                long attente = Math.max(0, Duration.between(dispo, maintenant).toMinutes());
                attentes.add(attente);
                e.attentes.add(attente);
                if (tableauxParJoueur.getOrDefault(j.getNumeroLicence(), 1) > 1) {
                    attentesPlusieursTableaux.add(attente);
                } else {
                    attentesUnTableau.add(attente);
                }
                if (attente > attenteMax) {
                    attenteMax = attente;
                    joueurAttenteMax = j.getNomComplet();
                }
            }
            Random r = new Random(melanger(cleDe(mo).hashCode()));
            // u : rang de la durée dans sa loi (0 = la plus courte). Un match long
            // a plus de chances d'aller en cinq manches.
            double u = r.nextDouble();
            List<Manche> manches = tirerScore(m, r, u);
            // Durée totale d'occupation : le jeu, plus l'appel (et les rappels),
            // l'échauffement (3.4.3.1), les pauses (3.4.4.1.1), les temps morts
            // (3.4.4.2) et le retour de la feuille. Tirage séparé, propre au match.
            TempsAnnexesMatch.Tirage annexes = scenario.getTempsAnnexes()
                    .tirer(new Random(melanger(cleDe(mo).hashCode() * 31L + 7)), manches.size());
            secondesAnnexes += annexes.total();
            matchsLances++;
            Duration duree = duree(mo, e, u).plusSeconds(annexes.total());
            LocalDateTime fin = maintenant.plus(duree);
            enCours.add(new EnCours(mo, fin, manches, sequence++));
            LocalDate jour = maintenant.toLocalDate();
            occupation.computeIfAbsent(jour, k -> new long[1])[0] += duree.toSeconds();
            LocalDateTime[] b = bornes.computeIfAbsent(jour, k -> new LocalDateTime[2]);
            b[0] = b[0] == null || maintenant.isBefore(b[0]) ? maintenant : b[0];
            b[1] = plusTard(b[1], fin);
        }

        private void terminer(EnCours c) {
            MatchOrdonnancable mo = c.mo();
            Match m = mo.getMatch();
            m.definirScore(c.manches());
            ordonnanceur.terminer(mo, c.fin());
            aJouer.remove(mo);
            libreDepuis.put(m.getJoueurA().getNumeroLicence(), c.fin());
            libreDepuis.put(m.getJoueurB().getNumeroLicence(), c.fin());
            EtatTableau e = tableauDe.get(m);
            e.parties++;
            e.restants--;
            e.fin = plusTard(e.fin, c.fin());
            if (mo.getPhase() == PhaseMatch.POULE) {
                verifierFinPoules(e);
            } else {
                progression.propager(e.phaseFinale);
            }
        }

        /** Poules toutes jouées : tirage du tableau final (IX.109), graine propre au tableau. */
        private void verifierFinPoules(EtatTableau e) {
            if (e.phaseFinale != null || e.def.format().formatPhaseFinale() != FormatPhaseFinale.ELIMINATION_DIRECTE) {
                return;
            }
            for (Poule p : e.poules) {
                if (!p.estTerminee()) {
                    return;
                }
            }
            e.phaseFinale = moteur.construireTableau(e.poules, e.def.format(), melanger(e.rang * 2L + 2),
                    e.def.petiteFinale());
            progression.propager(e.phaseFinale);
            List<Tour> tours = e.phaseFinale.getTours();
            for (int k = 0; k < tours.size(); k++) {
                PhaseMatch phase = k == 0 ? PhaseMatch.PREMIER_TOUR_TABLEAU : PhaseMatch.TOUR_SUIVANT_TABLEAU;
                for (Match m : tours.get(k).getMatchs()) {
                    ajouter(m, e, phase);
                }
            }
        }

        /**
         * Vainqueur tiré selon l'écart de points ; score réaliste en manches de 11.
         * Manches du perdant selon le rang {@code u} de la durée : 3-0 pour un
         * match court, 3-2 pour un match long.
         */
        private List<Manche> tirerScore(Match m, Random r, double u) {
            int n = Math.max(1, m.getNombreManchesGagnantes());
            double ecart = m.getJoueurA().getPoints() - m.getJoueurB().getPoints();
            boolean gagneA = r.nextDouble() < 1.0 / (1.0 + Math.exp(-ecart / 250.0));
            int perdues = Math.min(n - 1, (int) (u * n)); // manches du perdant : 0 à n - 1
            List<Boolean> pourVainqueur = new ArrayList<>();
            for (int i = 0; i < n - 1; i++) {
                pourVainqueur.add(true);
            }
            for (int i = 0; i < perdues; i++) {
                pourVainqueur.add(false);
            }
            Collections.shuffle(pourVainqueur, r);
            pourVainqueur.add(true); // la dernière manche est celle du vainqueur
            List<Manche> manches = new ArrayList<>();
            for (boolean v : pourVainqueur) {
                int perdant = r.nextInt(10);
                if (r.nextInt(8) == 0) {
                    perdant = 10 + r.nextInt(3); // égalité à 10-10 : deux points d'écart
                }
                int gagnant = Math.max(11, perdant + 2);
                boolean pourA = v == gagneA;
                manches.add(pourA ? new Manche(gagnant, perdant) : new Manche(perdant, gagnant));
            }
            return manches;
        }

        /**
         * Durée tirée dans la loi du scénario (exponentielle entre un minimum et
         * un maximum), mise à l'échelle de la durée estimée de la catégorie du
         * match et de son nombre de manches (référence : trois manches gagnantes).
         */
        private Duration duree(MatchOrdonnancable mo, EtatTableau e, double u) {
            int n = Math.max(1, mo.getMatch().getNombreManchesGagnantes());
            LoiDureeMatch loi = scenario.getLoiDuree();
            Duration estimee = scenario.getEstimateur().estimer(
                    CategorieDuree.depuisNiveau(mo.getPhase(), e.def.niveauPoints(), PALIER_NIVEAU));
            double facteur = (estimee.toSeconds() / (double) loi.getMoyenne().toSeconds()) * (n / 3.0);
            double minutes = loi.quantileMinutes(u) * facteur;
            return Duration.ofSeconds(Math.max(60, Math.round(minutes * 60)));
        }

        /** Clé stable d'une partie : ne dépend pas de l'ordre de lancement. */
        private String cleDe(MatchOrdonnancable mo) {
            Match m = mo.getMatch();
            return mo.getIdentifiantTableau() + "|" + mo.getPhase().name() + "|"
                    + m.getJoueurA().getNumeroLicence() + "|" + m.getJoueurB().getNumeroLicence();
        }

        private long melanger(long valeur) {
            long z = scenario.getGraine() * 0x9E3779B97F4A7C15L + valeur;
            z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
            z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
            return z ^ (z >>> 31);
        }

        private ResultatSimulation bilan(LocalDateTime debut) {
            List<ResultatSimulation.BilanTableau> bilans = new ArrayList<>();
            LocalDateTime fin = null;
            boolean complete = true;
            int parties = 0;
            for (EtatTableau e : etats) {
                boolean termine = e.demarre && e.restants == 0;
                complete &= termine;
                parties += e.parties;
                fin = plusTard(fin, e.fin);
                long max = e.attentes.stream().mapToLong(Long::longValue).max().orElse(0);
                bilans.add(new ResultatSimulation.BilanTableau(e.def.nom(), e.def.debut(),
                        termine ? e.fin : null, e.finSouhaitee, e.def.joueurs().size(), e.parties,
                        moyenne(e.attentes), max, retardDebut(e)));
            }
            long jeu = 0;
            long disponible = 0;
            for (Map.Entry<LocalDate, long[]> o : occupation.entrySet()) {
                LocalDateTime[] b = bornes.get(o.getKey());
                jeu += o.getValue()[0];
                disponible += Duration.between(b[0], b[1]).toSeconds() * scenario.getNombreDeTables();
            }
            double taux = disponible == 0 ? 0 : Math.min(1.0, (double) jeu / disponible);
            return new ResultatSimulation(strategie, scenario.getGraine(), scenario.getNombreDeTables(), debut, fin,
                    bilans, ignores, parties, moyenne(attentes), Math.max(0, attenteMax), joueurAttenteMax,
                    taux, complete, attentesUnTableau.isEmpty() ? -1 : moyenne(attentesUnTableau),
                    attentesPlusieursTableaux.isEmpty() ? -1 : moyenne(attentesPlusieursTableaux),
                    matchsLances == 0 ? 0 : secondesAnnexes / 60.0 / matchsLances);
        }

        /**
         * Retard du début réel sur l'heure prévue : le tableau a vraiment commencé
         * quand sa dernière poule a commencé (sans poules : son premier match).
         */
        private Long retardDebut(EtatTableau e) {
            LocalDateTime reel;
            if (e.poules != null && !e.poules.isEmpty()) {
                if (e.debutPoule.size() < e.poules.size()) {
                    return null;
                }
                reel = null;
                for (LocalDateTime t : e.debutPoule.values()) {
                    reel = plusTard(reel, t);
                }
            } else {
                reel = e.premierLancement;
            }
            return reel == null ? null : Math.max(0, Duration.between(e.def.debut(), reel).toMinutes());
        }
    }

    private static boolean inconnu(Joueur j) {
        return j == null || j.estADeterminer();
    }

    private static LocalDateTime plusTard(LocalDateTime a, LocalDateTime b) {
        if (a == null) {
            return b;
        }
        return b != null && b.isAfter(a) ? b : a;
    }

    private static double moyenne(List<Long> valeurs) {
        return valeurs.stream().mapToLong(Long::longValue).average().orElse(0);
    }
}
