package com.pingmanager.ordonnancement;

import com.pingmanager.model.Joueur;
import com.pingmanager.model.Match;
import com.pingmanager.model.Poule;
import com.pingmanager.model.StatutMatch;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Moteur d'ordonnancement des matchs sur les tables, <b>réactif et
 * semi-automatique</b> : à tout instant, il connaît l'état de la salle (tables
 * libres/occupées, joueurs occupés) et produit une liste de
 * {@link PropositionLancement} classées par priorité. L'organisateur valide.
 *
 * <p><b>Contrainte dure.</b> Un joueur ne peut jamais être affecté à deux tables
 * en même temps : un match dont l'un des joueurs est déjà en train de jouer
 * n'est pas proposé.</p>
 *
 * <p><b>Priorité.</b> Parmi les matchs jouables, le score combine (poids
 * réglables) : l'avancement par vagues (poules avant tableau), l'ancienneté
 * d'attente (équité), et le pouvoir de déblocage (un match dont un joueur est
 * engagé sur plusieurs tableaux passe avant pour ne pas geler la situation).</p>
 *
 * <p><b>Poules (unité de lancement).</b> Pendant la phase de poules, le
 * juge-arbitre envoie une poule entière sur une table ({@link PouleOrdonnancable},
 * {@link #ajouterPoule}) : elle occupe sa table et tous ses joueurs jusqu'au
 * dernier résultat, ses matchs ne sont jamais proposés seuls, et la priorité
 * d'une poule se calcule avec la même formule que celle d'un match (phase,
 * attente, déblocage, tableau commencé). Un match de poule déjà lancé seul (ancien
 * fichier) garde sa table jusqu'à son résultat ; la poule part ensuite en bloc.</p>
 *
 * <p><b>Temps.</b> Le lancement horodate le match automatiquement ; la fin est
 * posée à la saisie du score. Aucun temps de repos n'est imposé : seule la
 * contrainte « un joueur, une table » s'applique.</p>
 */
public final class Ordonnanceur {

    private final int nombreDeTables;
    private final PoidsOrdonnancement poids;
    /**
     * B054 : tables d'échauffement utilisables en renfort, numérotées après les
     * tables de compétition (N + 1 à N + max). Seules les {@link #tablesRenfortEnService}
     * premières reçoivent des matchs.
     */
    private final int tablesRenfortMax;
    private int tablesRenfortEnService;

    /** Tous les matchs sous gestion de l'ordonnanceur. */
    private final List<MatchOrdonnancable> matchs = new ArrayList<>();
    /** Les mêmes matchs, pour tester la présence en temps constant (l'ordre reste celui de {@link #matchs}). */
    private final Set<MatchOrdonnancable> matchsGeres = new HashSet<>();

    /** Table (1..N) → match en cours dessus, ou null si libre. */
    private final Map<Integer, MatchOrdonnancable> occupationTables = new HashMap<>();

    /**
     * Matchs en cours sans table valable (table absente, hors limites ou déjà
     * prise à la reprise) : leurs joueurs jouent quand même, ils restent occupés.
     */
    private final List<MatchOrdonnancable> enCoursHorsTable = new ArrayList<>();

    /** Poules gérées comme unités de lancement, et la poule de chaque match. */
    private final List<PouleOrdonnancable> poules = new ArrayList<>();
    private final Map<MatchOrdonnancable, PouleOrdonnancable> pouleDuMatch = new HashMap<>();
    /** Table → poule lancée en bloc dessus, ou absente. */
    private final Map<Integer, PouleOrdonnancable> occupationPoules = new HashMap<>();
    /** Poules en cours sans table valable (reprise) : leurs joueurs restent occupés. */
    private final List<PouleOrdonnancable> poulesHorsTable = new ArrayList<>();

    /** Tables réservées et préférences des tableaux (B024) ; aucune par défaut. */
    private ReservationsTables reservations = ReservationsTables.AUCUNE;

    public Ordonnanceur(int nombreDeTables) {
        this(nombreDeTables, new PoidsOrdonnancement());
    }

    public Ordonnanceur(int nombreDeTables, PoidsOrdonnancement poids) {
        this(nombreDeTables, 0, poids);
    }

    /**
     * Ordonnanceur avec une réserve de tables de renfort (B054) : les tables
     * d'échauffement utilisables comme tables de match portent les numéros
     * {@code nombreDeTables + 1} à {@code nombreDeTables + tablesRenfortMax} ;
     * aucune n'est en service au départ.
     */
    public Ordonnanceur(int nombreDeTables, int tablesRenfortMax, PoidsOrdonnancement poids) {
        if (nombreDeTables < 1) {
            throw new IllegalArgumentException("Il faut au moins une table.");
        }
        if (tablesRenfortMax < 0) {
            throw new IllegalArgumentException("Le nombre de tables de renfort ne peut pas être négatif.");
        }
        this.nombreDeTables = nombreDeTables;
        this.tablesRenfortMax = tablesRenfortMax;
        this.poids = Objects.requireNonNull(poids, "poids");
        for (int t = 1; t <= nombreDeTables + tablesRenfortMax; t++) {
            occupationTables.put(t, null);
        }
    }

    /** Ajoute un match à gérer. Sans effet s'il est déjà présent. */
    public void ajouterMatch(MatchOrdonnancable m) {
        Objects.requireNonNull(m, "m");
        gerer(m);
    }


    /**
     * Ajoute une poule à gérer comme unité de lancement : ses matchs sont gérés,
     * mais ne seront proposés et lancés qu'avec leur poule.
     */
    public void ajouterPoule(PouleOrdonnancable p) {
        Objects.requireNonNull(p, "p");
        if (poules.contains(p)) {
            return;
        }
        poules.add(p);
        for (MatchOrdonnancable mo : p.getMatchs()) {
            gerer(mo);
            pouleDuMatch.put(mo, p);
        }
    }

    /** Vue en lecture seule des poules gérées comme unités de lancement. */
    public List<PouleOrdonnancable> toutesLesPoules() {
        return java.util.Collections.unmodifiableList(poules);
    }

    /** Poule (unité de lancement) d'un match, ou null s'il se lance seul. */
    public PouleOrdonnancable pouleDe(MatchOrdonnancable mo) {
        return pouleDuMatch.get(mo);
    }

    /** Poule lancée en bloc sur une table, ou null. */
    public PouleOrdonnancable pouleSurTable(int table) {
        return occupationPoules.get(table);
    }

    /** La table est-elle libre (ni match, ni poule) ? */
    private boolean estLibre(int table) {
        return occupationTables.get(table) == null && occupationPoules.get(table) == null;
    }

    /** Ajoute le match à la liste ordonnée s'il n'y est pas encore. */
    private void gerer(MatchOrdonnancable m) {
        if (matchsGeres.add(m)) {
            matchs.add(m);
        }
    }

    /** Vue en lecture seule de tous les matchs sous gestion. */
    public List<MatchOrdonnancable> tousLesMatchs() {
        return java.util.Collections.unmodifiableList(matchs);
    }

    /** @return le nombre de tables de compétition (sans les tables de renfort). */
    public int getNombreDeTables() {
        return nombreDeTables;
    }

    /** @return le nombre maximal de tables de renfort (B054). */
    public int getTablesRenfortMax() {
        return tablesRenfortMax;
    }

    /** @return les tables de renfort en service (B054), de 0 à {@link #getTablesRenfortMax()}. */
    public int getTablesRenfortEnService() {
        return tablesRenfortEnService;
    }

    /**
     * Met en service les {@code tables} premières tables de renfort (B054). Une
     * table retirée du service garde son match en cours jusqu'à sa fin, mais ne
     * reçoit plus de nouveau match.
     *
     * @throws IllegalArgumentException si {@code tables} sort de 0 à {@link #getTablesRenfortMax()}
     */
    public void setTablesRenfortEnService(int tables) {
        if (tables < 0 || tables > tablesRenfortMax) {
            throw new IllegalArgumentException("Le renfort va de 0 à " + tablesRenfortMax + " table(s).");
        }
        this.tablesRenfortEnService = tables;
    }

    /** @return vrai si la table est une table de renfort (salle d'échauffement), en service ou non. */
    public boolean estTableDeRenfort(int table) {
        return table > nombreDeTables && table <= nombreDeTables + tablesRenfortMax;
    }

    /** @return le nombre de tables de renfort sur lesquelles un match est en cours. */
    public int tablesRenfortOccupees() {
        int n = 0;
        for (int t = nombreDeTables + 1; t <= nombreDeTables + tablesRenfortMax; t++) {
            if (!estLibre(t)) {
                n++;
            }
        }
        return n;
    }

    /** @return les poids de scoring utilisés par l'ordonnanceur (pour la persistance/le réglage). */
    public PoidsOrdonnancement getPoids() {
        return poids;
    }

    /** @return les réservations de tables respectées par l'ordonnanceur (B024). */
    public ReservationsTables getReservations() {
        return reservations;
    }

    /**
     * Pose les réservations de tables (B024) : une table restreinte ne reçoit que
     * ses tableaux, ni par « Proposer » ni par un choix manuel ; un tableau va
     * d'abord sur ses tables préférées.
     */
    public void setReservations(ReservationsTables reservations) {
        this.reservations = Objects.requireNonNull(reservations, "reservations");
    }

    /**
     * Tables actuellement libres (numéros croissants) : tables de compétition,
     * puis tables de renfort en service (B054). Les tables de compétition sont
     * donc toujours servies d'abord.
     */
    public List<Integer> tablesLibres() {
        List<Integer> libres = new ArrayList<>();
        for (int t = 1; t <= nombreDeTables + tablesRenfortEnService; t++) {
            if (estLibre(t)) {
                libres.add(t);
            }
        }
        return libres;
    }

    /** Joueurs actuellement en train de jouer (occupés sur une table). */
    public Set<String> joueursOccupes() {
        Set<String> occupes = new HashSet<>();
        for (MatchOrdonnancable m : occupationTables.values()) {
            if (m != null) {
                Match match = m.getMatch();
                if (match.getJoueurA() != null) occupes.add(match.getJoueurA().getNumeroLicence());
                if (match.getJoueurB() != null) occupes.add(match.getJoueurB().getNumeroLicence());
            }
        }
        for (MatchOrdonnancable m : enCoursHorsTable) {
            Match match = m.getMatch();
            if (match.getJoueurA() != null) occupes.add(match.getJoueurA().getNumeroLicence());
            if (match.getJoueurB() != null) occupes.add(match.getJoueurB().getNumeroLicence());
        }
        // Une poule lancée occupe tous ses joueurs, entre deux matchs compris.
        List<PouleOrdonnancable> enJeu = new ArrayList<>(occupationPoules.values());
        enJeu.addAll(poulesHorsTable);
        for (PouleOrdonnancable p : enJeu) {
            for (Joueur j : p.joueurs()) {
                if (j != null) occupes.add(j.getNumeroLicence());
            }
        }
        return occupes;
    }

    /**
     * Indique si un match est jouable maintenant : statut A_JOUER, deux joueurs
     * connus (non « à déterminer »), et aucun des deux déjà occupé sur une table.
     */
    public boolean estJouableMaintenant(MatchOrdonnancable mo) {
        if (pouleDuMatch.containsKey(mo)) {
            return false; // se lance avec sa poule
        }
        Match m = mo.getMatch();
        if (m.getStatut() != StatutMatch.A_JOUER || m.estBye()) {
            return false;
        }
        Joueur a = m.getJoueurA();
        Joueur b = m.getJoueurB();
        if (a == null || b == null || a.estADeterminer() || b.estADeterminer()) {
            return false;
        }
        Set<String> occupes = joueursOccupes();
        return !occupes.contains(a.getNumeroLicence()) && !occupes.contains(b.getNumeroLicence());
    }

    /**
     * Marque à quel instant chaque match encore à jouer est devenu « prêt »
     * (jouable), afin de mesurer l'attente. Appelé au fil de l'eau ; ne réécrase
     * pas un instant déjà posé.
     */
    public void rafraichirDisponibilites(LocalDateTime maintenant) {
        for (MatchOrdonnancable mo : matchs) {
            Match m = mo.getMatch();
            boolean pret = m.getStatut() == StatutMatch.A_JOUER && !m.estBye()
                    && m.getJoueurA() != null && m.getJoueurB() != null
                    && !m.getJoueurA().estADeterminer() && !m.getJoueurB().estADeterminer();
            if (pret && mo.getPretDepuis() == null) {
                mo.setPretDepuis(maintenant);
            } else if (!pret) {
                mo.setPretDepuis(null);
            }
        }
        for (PouleOrdonnancable p : poules) {
            boolean pret = !p.estEnCours() && !p.aJouer().isEmpty();
            if (pret && p.getPretDepuis() == null) {
                p.setPretDepuis(maintenant);
            } else if (!pret) {
                p.setPretDepuis(null);
            }
        }
    }

    /**
     * Produit les propositions de lancement pour l'instant donné, classées par
     * priorité décroissante, sans dépasser le nombre de tables libres et sans
     * jamais affecter deux fois le même joueur dans la même vague de propositions.
     *
     * @param maintenant instant de référence (pour l'attente)
     * @return propositions ordonnées ; l'organisateur en valide tout ou partie
     */
    public List<PropositionLancement> proposerLancements(LocalDateTime maintenant) {
        rafraichirDisponibilites(maintenant);

        List<Integer> libres = tablesLibres();
        if (libres.isEmpty()) {
            return List.of();
        }

        // Index calculés une seule fois pour toute la vague (B025 : performance
        // sur 400 joueurs et 15 tableaux) ; résultat identique au calcul match par match.
        Contexte ctx = new Contexte(maintenant);

        // Matchs jouables, triés par score décroissant (tri stable : à score égal,
        // l'ordre d'ajout est conservé).
        // Les poules (unités de lancement) cohabitent avec les matchs du tableau
        // final : même liste, même formule de priorité.
        List<Candidat> jouables = new ArrayList<>();
        for (MatchOrdonnancable mo : matchs) {
            if (!pouleDuMatch.containsKey(mo) && estJouable(mo, ctx.occupes)
                    && !(poids.isTourParTour() && ctx.retenuParLeTour(mo))
                    && !(poids.isFinalesEnFinDeJournee() && ctx.finaleRetenue(mo))) {
                jouables.add(new Candidat(mo, null, score(mo, maintenant, ctx)));
            }
        }
        for (PouleOrdonnancable p : poules) {
            if (verifierPoule(p, ctx.occupes).estReussi()) {
                jouables.add(new Candidat(p.aJouer().get(0), p, scorePoule(p, maintenant, ctx)));
            }
        }
        jouables.sort(Comparator.comparingDouble((Candidat c) -> c.score).reversed());

        // Affectation gloutonne : on descend la liste triée, en réservant les
        // joueurs déjà engagés dans cette vague pour ne pas les mettre 2 fois.
        List<PropositionLancement> propositions = new ArrayList<>();
        Set<String> joueursReserves = new HashSet<>(ctx.occupes);
        // B024 : chaque match prend la table libre qui lui convient le mieux (table
        // préférée de son tableau, sinon table ordinaire), jamais une table réservée
        // à d'autres tableaux. Sans réservation : les tables libres dans l'ordre.
        List<Integer> restantes = new ArrayList<>(libres);

        for (Candidat c : jouables) {
            if (restantes.isEmpty()) {
                break;
            }
            if (c.poule != null) {
                PouleOrdonnancable p = c.poule;
                boolean pris = false;
                for (Joueur j : p.joueurs()) {
                    pris |= j != null && joueursReserves.contains(j.getNumeroLicence());
                }
                int index = pris ? -1 : meilleureTable(restantes, p.getIdentifiantTableau());
                if (index < 0) {
                    continue;
                }
                int table = restantes.remove(index);
                propositions.add(new PropositionLancement(p, table, c.score, expliquerPoule(p, maintenant, ctx)));
                for (Joueur j : p.joueurs()) {
                    if (j != null) joueursReserves.add(j.getNumeroLicence());
                }
                continue;
            }
            MatchOrdonnancable mo = c.match;
            Match m = mo.getMatch();
            String a = m.getJoueurA().getNumeroLicence();
            String b = m.getJoueurB().getNumeroLicence();
            if (joueursReserves.contains(a) || joueursReserves.contains(b)) {
                continue; // un joueur déjà pris dans cette vague
            }
            int index = meilleureTable(restantes, mo.getIdentifiantTableau());
            if (index < 0) {
                continue; // aucune table libre n'accepte ce tableau
            }
            int table = restantes.remove(index);
            double score = c.score;
            String explication = expliquer(mo, maintenant, ctx);
            propositions.add(new PropositionLancement(mo, table, score, explication));
            joueursReserves.add(a);
            joueursReserves.add(b);
        }
        return propositions;
    }

    /** Un candidat d'une vague : un match seul, ou une poule (match = son premier match à jouer). */
    private record Candidat(MatchOrdonnancable match, PouleOrdonnancable poule, double score) {
    }

    /** Tableaux du jour qui n'ont pas encore commencé (identifiants), pour « finales en fin de journée ». */
    private Set<String> tableauxAttendus = Set.of();

    /**
     * Déclare les tableaux de la journée pas encore commencés (aucun match chargé) :
     * avec « finales en fin de journée », aucune finale n'est proposée tant qu'il
     * en reste. Vide par défaut.
     */
    public void setTableauxAttendus(java.util.Collection<String> identifiants) {
        this.tableauxAttendus = identifiants == null ? Set.of() : Set.copyOf(identifiants);
    }

    public Set<String> getTableauxAttendus() {
        return tableauxAttendus;
    }

    /** Index, dans les tables libres (croissantes), de la meilleure table pour ce tableau, ou -1. */
    private int meilleureTable(List<Integer> libres, String tableau) {
        int meilleur = -1;
        int meilleurRang = Integer.MAX_VALUE;
        for (int i = 0; i < libres.size(); i++) {
            int rang = reservations.rang(libres.get(i), tableau);
            if (rang >= 0 && rang < meilleurRang) {
                meilleur = i;
                meilleurRang = rang;
                if (rang == 0) {
                    break;
                }
            }
        }
        return meilleur;
    }

    /** Comme {@link #estJouableMaintenant}, avec l'ensemble des joueurs occupés déjà calculé. */
    private static boolean estJouable(MatchOrdonnancable mo, Set<String> occupes) {
        Match m = mo.getMatch();
        if (m.getStatut() != StatutMatch.A_JOUER || m.estBye()) {
            return false;
        }
        Joueur a = m.getJoueurA();
        Joueur b = m.getJoueurB();
        if (a == null || b == null || a.estADeterminer() || b.estADeterminer()) {
            return false;
        }
        return !occupes.contains(a.getNumeroLicence()) && !occupes.contains(b.getNumeroLicence());
    }

    /**
     * Calcule le score de priorité d'un match jouable. Plus il est élevé, plus
     * le match doit être lancé tôt.
     */
    public double scorePriorite(MatchOrdonnancable mo, LocalDateTime maintenant) {
        return score(mo, maintenant, null);
    }

    private double score(MatchOrdonnancable mo, LocalDateTime maintenant, Contexte ctx) {
        int engagements = ctx == null ? compterEngagementsAilleurs(mo) : ctx.engagementsAilleurs(mo);
        long minutesTableau = poids.getPoidsTableauCommence() == 0.0 ? 0
                : ctx == null ? minutesDepuisDebutTableau(mo.getIdentifiantTableau(), maintenant)
                : ctx.minutesDepuisDebutTableau(mo.getIdentifiantTableau());
        return score(mo.getPhase(), mo.getPretDepuis(), engagements, minutesTableau, mo.getMatch(), maintenant);
    }

    /**
     * Score de priorité d'une poule à lancer en bloc : la même formule que pour
     * un match (phase « poule », attente depuis que tous ses joueurs sont libres,
     * matchs de ses joueurs hors de la poule, tableau commencé).
     */
    public double scorePriorite(PouleOrdonnancable p, LocalDateTime maintenant) {
        return scorePoule(p, maintenant, null);
    }

    private double scorePoule(PouleOrdonnancable p, LocalDateTime maintenant, Contexte ctx) {
        long minutesTableau = poids.getPoidsTableauCommence() == 0.0 ? 0
                : ctx == null ? minutesDepuisDebutTableau(p.getIdentifiantTableau(), maintenant)
                : ctx.minutesDepuisDebutTableau(p.getIdentifiantTableau());
        return score(PhaseMatch.POULE, p.getPretDepuis(), engagementsHorsPoule(p), minutesTableau, null, maintenant);
    }

    /** Matchs encore à jouer, hors de la poule, où figure un de ses joueurs (déblocage). */
    int engagementsHorsPoule(PouleOrdonnancable p) {
        int count = 0;
        for (MatchOrdonnancable autre : matchs) {
            Match m = autre.getMatch();
            if (m.getStatut() == StatutMatch.TERMINE || m.estBye() || pouleDuMatch.get(autre) == p) {
                continue;
            }
            if (p.contient(licence(m.getJoueurA())) || p.contient(licence(m.getJoueurB()))) {
                count++;
            }
        }
        return count;
    }

    /** Formule unique de priorité (match seul ou poule entière). */
    private double score(PhaseMatch phase, LocalDateTime pretDepuis, int engagements, long minutesTableau,
                         Match pourProfondeur, LocalDateTime maintenant) {
        double score = 0.0;

        // 1) Avancement par vagues : POULE > 1er tour > tours suivants.
        //    On donne un bonus décroissant selon la phase.
        double bonusPhase = switch (phase.name()) {
            case "POULE" -> 3.0;
            case "PREMIER_TOUR_TABLEAU" -> 2.0;
            case "TOUR_SUIVANT_TABLEAU" -> 1.0;
            default -> throw new IllegalStateException("Phase inconnue : " + phase);
        };
        score += poids.getPoidsVague() * bonusPhase;

        // 2) Équité : ancienneté d'attente (minutes depuis que le match est prêt).
        long minutesAttente = 0;
        if (pretDepuis != null) {
            minutesAttente = Math.max(0, Duration.between(pretDepuis, maintenant).toMinutes());
        }
        score += poids.getPoidsAttente() * poids.getPointsParMinuteAttente() * minutesAttente;

        // 3) Déblocage : si un joueur de ce match est engagé dans d'autres matchs
        //    encore à jouer (typiquement inscrit à deux tableaux), le lancer
        //    libère plus vite la suite. On compte ces engagements.
        score += poids.getPoidsDeblocage() * engagements;

        // 4) Tableau commencé tôt (B025) : minutes depuis le premier match lancé
        //    du tableau. Inactif avec les poids par défaut.
        if (poids.getPoidsTableauCommence() != 0.0) {
            score += poids.getPoidsTableauCommence() * minutesTableau;
        }

        // 5) Tours avancés d'abord (B025, stratégie « tableaux commencés tôt »).
        if (poids.getPoidsProfondeur() != 0.0 && phase != PhaseMatch.POULE && pourProfondeur != null) {
            score += poids.getPoidsProfondeur() * profondeur(pourProfondeur, new IdentityHashMap<>());
        }

        return score;
    }

    /**
     * Tour d'un match du tableau final compté depuis le premier tour (0), d'après
     * ses matchs sources : un match sans source est au 1er tour, sinon un tour
     * de plus que sa source la plus avancée.
     */
    static int profondeur(Match m, Map<Match, Integer> memo) {
        Integer connu = memo.get(m);
        if (connu != null) {
            return connu;
        }
        int p = 0;
        if (m.getSourceA() != null) {
            p = Math.max(p, 1 + profondeur(m.getSourceA(), memo));
        }
        if (m.getSourceB() != null) {
            p = Math.max(p, 1 + profondeur(m.getSourceB(), memo));
        }
        memo.put(m, p);
        return p;
    }

    /**
     * Minutes écoulées depuis le lancement du premier match du tableau de ce
     * match (0 si le tableau n'a encore rien lancé).
     */
    long minutesDepuisDebutTableau(MatchOrdonnancable mo, LocalDateTime maintenant) {
        return minutesDepuisDebutTableau(mo.getIdentifiantTableau(), maintenant);
    }

    private long minutesDepuisDebutTableau(String tableau, LocalDateTime maintenant) {
        LocalDateTime debut = null;
        for (MatchOrdonnancable autre : matchs) {
            LocalDateTime l = autre.getMatch().getInstantLancement();
            if (l != null && autre.getIdentifiantTableau().equals(tableau)
                    && (debut == null || l.isBefore(debut))) {
                debut = l;
            }
        }
        return debut == null ? 0 : Math.max(0, Duration.between(debut, maintenant).toMinutes());
    }

    /**
     * Compte, pour les joueurs d'un match, le nombre d'autres matchs encore à
     * jouer où ils apparaissent (indicateur de risque de blocage).
     */
    int compterEngagementsAilleurs(MatchOrdonnancable mo) {
        Match ref = mo.getMatch();
        String a = ref.getJoueurA() != null ? ref.getJoueurA().getNumeroLicence() : null;
        String b = ref.getJoueurB() != null ? ref.getJoueurB().getNumeroLicence() : null;
        int count = 0;
        for (MatchOrdonnancable autre : matchs) {
            if (autre.equals(mo)) continue;
            Match m = autre.getMatch();
            if (m.getStatut() == StatutMatch.TERMINE || m.estBye()) continue;
            String x = m.getJoueurA() != null ? m.getJoueurA().getNumeroLicence() : null;
            String y = m.getJoueurB() != null ? m.getJoueurB().getNumeroLicence() : null;
            if ((a != null && (a.equals(x) || a.equals(y)))
                    || (b != null && (b.equals(x) || b.equals(y)))) {
                count++;
            }
        }
        return count;
    }

    /** Explication lisible de la priorité d'un match (phase, attente, déblocage, tableau commencé). */
    public String expliquer(MatchOrdonnancable mo, LocalDateTime maintenant) {
        return expliquer(mo, maintenant, null);
    }

    private String expliquer(MatchOrdonnancable mo, LocalDateTime maintenant, Contexte ctx) {
        int engagements = ctx == null ? compterEngagementsAilleurs(mo) : ctx.engagementsAilleurs(mo);
        long minutesTableau = poids.getPoidsTableauCommence() == 0.0 ? 0
                : ctx == null ? minutesDepuisDebutTableau(mo.getIdentifiantTableau(), maintenant)
                : ctx.minutesDepuisDebutTableau(mo.getIdentifiantTableau());
        return expliquer(mo.getPhase(), mo.getPretDepuis(), engagements, minutesTableau, maintenant);
    }

    /** Explication lisible de la priorité d'une poule à lancer en bloc. */
    public String expliquer(PouleOrdonnancable p, LocalDateTime maintenant) {
        return expliquerPoule(p, maintenant, null);
    }

    private String expliquerPoule(PouleOrdonnancable p, LocalDateTime maintenant, Contexte ctx) {
        long minutesTableau = poids.getPoidsTableauCommence() == 0.0 ? 0
                : ctx == null ? minutesDepuisDebutTableau(p.getIdentifiantTableau(), maintenant)
                : ctx.minutesDepuisDebutTableau(p.getIdentifiantTableau());
        return expliquer(PhaseMatch.POULE, p.getPretDepuis(), engagementsHorsPoule(p), minutesTableau, maintenant);
    }

    private String expliquer(PhaseMatch phase, LocalDateTime pretDepuis, int engagements, long minutesTableau,
                             LocalDateTime maintenant) {
        List<String> raisons = new ArrayList<>();
        raisons.add(switch (phase.name()) {
            case "POULE" -> "poule";
            case "PREMIER_TOUR_TABLEAU" -> "1er tour";
            case "TOUR_SUIVANT_TABLEAU" -> "tour suivant";
            default -> throw new IllegalStateException("Phase inconnue : " + phase);
        });
        if (pretDepuis != null) {
            long min = Math.max(0, Duration.between(pretDepuis, maintenant).toMinutes());
            if (min > 0) {
                raisons.add("attend " + min + " min");
            }
        }
        if (engagements > 0) {
            raisons.add("débloque " + engagements + " match(s)");
        }
        if (poids.getPoidsTableauCommence() != 0.0) {
            if (minutesTableau > 0) {
                raisons.add("tableau commencé depuis " + minutesTableau + " min");
            }
        }
        return String.join(", ", raisons);
    }

    /**
     * Applique le lancement d'un match sur une table : marque la table occupée,
     * horodate le lancement, et passe le match en cours.
     */
    public void lancer(PropositionLancement proposition, LocalDateTime maintenant) {
        Objects.requireNonNull(proposition, "proposition");
        if (proposition.estPoule()) {
            lancerPoule(proposition.getPoule(), proposition.getNumeroTable(), maintenant);
            return;
        }
        MatchOrdonnancable mo = proposition.getMatch();
        int table = proposition.getNumeroTable();
        if (!estLibre(table)) {
            throw new IllegalStateException("La table " + table + " est déjà occupée.");
        }
        Match m = mo.getMatch();
        m.setNumeroTable(table);
        m.setInstantLancement(maintenant);
        m.setStatut(StatutMatch.EN_COURS);
        occupationTables.put(table, mo);
    }

    /**
     * Libère la table d'un match terminé et horodate sa fin. À appeler après la
     * saisie du score (le score lui-même est posé sur le {@link Match}).
     */
    public void terminer(MatchOrdonnancable mo, LocalDateTime maintenant) {
        Objects.requireNonNull(mo, "mo");
        Match m = mo.getMatch();
        Integer table = m.getNumeroTable();
        if (table != null && occupationTables.containsKey(table)
                && mo.equals(occupationTables.get(table))) {
            occupationTables.put(table, null);
        }
        enCoursHorsTable.remove(mo);
        m.setInstantFin(maintenant);
        PouleOrdonnancable p = pouleDuMatch.get(mo);
        if (p != null) {
            libererSiTerminee(p);
        }
    }

    /**
     * Libère la table et les joueurs d'une poule lancée dont tous les matchs ont
     * un résultat (le lancement de la poule est effacé).
     *
     * @return true si la poule vient d'être libérée
     */
    public boolean libererSiTerminee(PouleOrdonnancable p) {
        Objects.requireNonNull(p, "p");
        if (!p.getPoule().estLanceeEnBloc() || p.estEnCours()) {
            return false;
        }
        occupationPoules.values().removeIf(x -> x == p);
        poulesHorsTable.remove(p);
        p.getPoule().definirLancement(null, null);
        return true;
    }

    /**
     * Match en cours sur une table donnée, ou null si libre. Pour une table
     * occupée par une poule : le match de la poule qui s'y joue (le premier sans
     * résultat dans l'ordre I.301).
     */
    public MatchOrdonnancable matchSurTable(int table) {
        MatchOrdonnancable mo = occupationTables.get(table);
        if (mo == null) {
            PouleOrdonnancable p = occupationPoules.get(table);
            return p == null ? null : p.matchEnJeu();
        }
        return mo;
    }

    // -------------------------------------------------------------------------
    // Affectation manuelle (choix du juge-arbitre), avec motif de refus
    // -------------------------------------------------------------------------

    /**
     * Table sur laquelle joue actuellement un joueur (par licence), ou null s'il
     * n'est sur aucune table.
     */
    public Integer tableDuJoueur(String licence) {
        if (licence == null) {
            return null;
        }
        for (int t = 1; t <= nombreDeTables + tablesRenfortMax; t++) {
            MatchOrdonnancable mo = occupationTables.get(t);
            if (mo != null && joue(mo.getMatch(), licence)) {
                return t;
            }
            PouleOrdonnancable p = occupationPoules.get(t);
            if (p != null && p.contient(licence)) {
                return t;
            }
        }
        return null;
    }

    private static boolean joue(Match m, String licence) {
        return (m.getJoueurA() != null && licence.equals(m.getJoueurA().getNumeroLicence()))
                || (m.getJoueurB() != null && licence.equals(m.getJoueurB().getNumeroLicence()));
    }

    /**
     * Dit si un match peut être lancé maintenant, indépendamment de la table :
     * match géré, à jouer, deux joueurs connus, aucun des deux déjà en jeu.
     *
     * @return {@link ResultatAffectationTable.Motif#AFFECTE} (table 0) si le match
     *         est jouable, sinon le motif du blocage
     */
    public ResultatAffectationTable verifierMatch(MatchOrdonnancable mo) {
        Objects.requireNonNull(mo, "mo");
        if (!matchsGeres.contains(mo)) {
            return ResultatAffectationTable.de(ResultatAffectationTable.Motif.MATCH_INCONNU, 0);
        }
        Match m = mo.getMatch();
        if (m.estBye()) {
            return ResultatAffectationTable.de(ResultatAffectationTable.Motif.MATCH_EXEMPT, 0);
        }
        if (m.getStatut() == StatutMatch.TERMINE) {
            return ResultatAffectationTable.de(ResultatAffectationTable.Motif.MATCH_TERMINE, 0);
        }
        if (m.getStatut() == StatutMatch.EN_COURS) {
            Integer table = m.getNumeroTable();
            return ResultatAffectationTable.de(ResultatAffectationTable.Motif.MATCH_DEJA_EN_COURS,
                    table == null ? 0 : table);
        }
        Joueur a = m.getJoueurA();
        Joueur b = m.getJoueurB();
        if (m.getStatut() != StatutMatch.A_JOUER || a == null || b == null
                || a.estADeterminer() || b.estADeterminer()) {
            return ResultatAffectationTable.de(ResultatAffectationTable.Motif.JOUEURS_NON_CONNUS, 0);
        }
        if (pouleDuMatch.containsKey(mo)) {
            // Phase de poules : la poule part en bloc sur une table, pas match par match.
            return ResultatAffectationTable.de(ResultatAffectationTable.Motif.LANCEMENT_PAR_POULE, 0);
        }
        // Contrainte dure : un joueur n'est jamais sur deux tables à la fois.
        for (Joueur j : List.of(a, b)) {
            Integer table = tableDuJoueur(j.getNumeroLicence());
            if (table != null) {
                return ResultatAffectationTable.joueurEnJeu(0, j, table);
            }
        }
        // Même règle pour un match en cours dont la table n'est pas valable :
        // ses joueurs jouent, ils ne sont pas disponibles.
        for (Joueur j : List.of(a, b)) {
            if (joueSansTable(j.getNumeroLicence(), mo)) {
                return ResultatAffectationTable.joueurEnJeuSansTable(0, j);
            }
        }
        return ResultatAffectationTable.de(ResultatAffectationTable.Motif.AFFECTE, 0);
    }

    /** Le joueur est-il dans un match ou une poule en cours sans table valable (hors {@code sauf}) ? */
    private boolean joueSansTable(String licence, MatchOrdonnancable sauf) {
        for (MatchOrdonnancable autre : enCoursHorsTable) {
            if (autre != sauf && joue(autre.getMatch(), licence)) {
                return true;
            }
        }
        for (PouleOrdonnancable p : poulesHorsTable) {
            if (p.contient(licence)) {
                return true;
            }
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Poules : lancement en bloc (phase de poules)
    // -------------------------------------------------------------------------

    /**
     * Dit si une poule peut être lancée maintenant, indépendamment de la table :
     * poule gérée, pas déjà en cours, au moins un match à jouer, tous ses joueurs
     * connus, présents et libres (aucun ne joue ailleurs, ni dans un match de la
     * poule lancé seul dans un ancien fichier).
     *
     * @return {@link ResultatAffectationTable.Motif#AFFECTE} (table 0) si elle peut partir
     */
    public ResultatAffectationTable verifierPoule(PouleOrdonnancable p) {
        return verifierPoule(p, null);
    }

    private ResultatAffectationTable verifierPoule(PouleOrdonnancable p, Set<String> occupesCalcules) {
        Objects.requireNonNull(p, "p");
        if (!poules.contains(p)) {
            return ResultatAffectationTable.de(ResultatAffectationTable.Motif.MATCH_INCONNU, 0);
        }
        if (p.estEnCours()) {
            Integer table = p.getPoule().getNumeroTable();
            return ResultatAffectationTable.de(ResultatAffectationTable.Motif.MATCH_DEJA_EN_COURS,
                    table == null ? 0 : table);
        }
        if (p.aJouer().isEmpty()) {
            boolean enCours = p.getMatchs().stream()
                    .anyMatch(mo -> mo.getMatch().getStatut() == StatutMatch.EN_COURS);
            if (!enCours) {
                return ResultatAffectationTable.de(ResultatAffectationTable.Motif.MATCH_TERMINE, 0);
            }
        }
        for (Joueur j : p.joueurs()) {
            if (j == null || j.estADeterminer()) {
                return ResultatAffectationTable.de(ResultatAffectationTable.Motif.JOUEURS_NON_CONNUS, 0);
            }
        }
        for (Joueur j : p.joueurs()) {
            if (p.getAbsents().contains(j.getNumeroLicence())) {
                return ResultatAffectationTable.joueurAbsent(0, j);
            }
        }
        if (occupesCalcules != null) {
            for (Joueur j : p.joueurs()) {
                if (occupesCalcules.contains(j.getNumeroLicence())) {
                    return ResultatAffectationTable.joueurEnJeuSansTable(0, j); // vague : le détail importe peu
                }
            }
            return ResultatAffectationTable.de(ResultatAffectationTable.Motif.AFFECTE, 0);
        }
        // Contrainte dure : aucun joueur de la poule ne joue ailleurs (ni entre deux de ses matchs).
        for (Joueur j : p.joueurs()) {
            Integer table = tableDuJoueur(j.getNumeroLicence());
            if (table != null) {
                return ResultatAffectationTable.joueurEnJeu(0, j, table);
            }
            if (joueSansTable(j.getNumeroLicence(), null)) {
                return ResultatAffectationTable.joueurEnJeuSansTable(0, j);
            }
        }
        return ResultatAffectationTable.de(ResultatAffectationTable.Motif.AFFECTE, 0);
    }

    /** Vérifie qu'une poule peut partir sur une table précise, sans rien modifier. */
    public ResultatAffectationTable verifierAffectationPoule(PouleOrdonnancable p, int table) {
        Objects.requireNonNull(p, "p");
        if (table < 1 || table > nombreDeTables + tablesRenfortMax) {
            return ResultatAffectationTable.de(ResultatAffectationTable.Motif.TABLE_HORS_LIMITES, table);
        }
        if (table > nombreDeTables + tablesRenfortEnService) {
            return ResultatAffectationTable.de(ResultatAffectationTable.Motif.TABLE_RENFORT_HORS_SERVICE, table);
        }
        ResultatAffectationTable r = verifierPoule(p);
        if (r.getMotif() == ResultatAffectationTable.Motif.JOUEUR_DEJA_EN_JEU) {
            return r.getTableDuJoueurBloquant() == null
                    ? ResultatAffectationTable.joueurEnJeuSansTable(table, r.getJoueurBloquant())
                    : ResultatAffectationTable.joueurEnJeu(table, r.getJoueurBloquant(), r.getTableDuJoueurBloquant());
        }
        if (r.getMotif() == ResultatAffectationTable.Motif.JOUEUR_ABSENT) {
            return ResultatAffectationTable.joueurAbsent(table, r.getJoueurBloquant());
        }
        if (!r.estReussi()) {
            return r;
        }
        if (!estLibre(table)) {
            return ResultatAffectationTable.de(ResultatAffectationTable.Motif.TABLE_OCCUPEE, table);
        }
        if (!reservations.admet(table, p.getIdentifiantTableau())) {
            return ResultatAffectationTable.tableReservee(table, reservations.tableauxAdmis(table));
        }
        return ResultatAffectationTable.de(ResultatAffectationTable.Motif.AFFECTE, table);
    }

    /**
     * Lance la poule entière sur une table : la poule porte la table et l'heure,
     * chacun de ses matchs à jouer passe en cours sur cette table (ils se jouent à
     * la suite, dans l'ordre I.301). Rien n'est modifié en cas de refus.
     */
    public ResultatAffectationTable affecterPoule(PouleOrdonnancable p, int table, LocalDateTime maintenant) {
        Objects.requireNonNull(maintenant, "maintenant");
        ResultatAffectationTable r = verifierAffectationPoule(p, table);
        if (r.estReussi()) {
            lancerPoule(p, table, maintenant);
        }
        return r;
    }

    private void lancerPoule(PouleOrdonnancable p, int table, LocalDateTime maintenant) {
        if (!estLibre(table)) {
            throw new IllegalStateException("La table " + table + " est déjà occupée.");
        }
        p.getPoule().definirLancement(table, maintenant);
        for (MatchOrdonnancable mo : p.aJouer()) {
            Match m = mo.getMatch();
            m.setNumeroTable(table);
            m.setInstantLancement(maintenant);
            m.setStatut(StatutMatch.EN_COURS);
        }
        occupationPoules.put(table, p);
    }

    /**
     * Annule le lancement d'une poule tant qu'aucun résultat n'y a été saisi
     * depuis le lancement : la table redevient libre, les matchs redeviennent à
     * jouer. Ensuite, le lancement reste en place.
     */
    public ResultatAffectationTable annulerLancementPoule(PouleOrdonnancable p) {
        Objects.requireNonNull(p, "p");
        Poule poule = p.getPoule();
        Integer table = poule.getNumeroTable();
        int t = table == null ? 0 : table;
        if (!p.estEnCours()) {
            return ResultatAffectationTable.de(ResultatAffectationTable.Motif.MATCH_PAS_EN_COURS, t);
        }
        if (aUnResultatDepuisLeLancement(p)) {
            return ResultatAffectationTable.de(ResultatAffectationTable.Motif.SCORE_DEJA_SAISI, t);
        }
        for (MatchOrdonnancable mo : p.getMatchs()) {
            Match m = mo.getMatch();
            if (m.getStatut() == StatutMatch.EN_COURS) {
                m.setStatut(StatutMatch.A_JOUER);
                m.setNumeroTable(null);
                m.setInstantLancement(null);
            }
        }
        occupationPoules.values().removeIf(x -> x == p);
        poulesHorsTable.remove(p);
        poule.definirLancement(null, null);
        return ResultatAffectationTable.de(ResultatAffectationTable.Motif.AFFECTATION_ANNULEE, t);
    }

    /** Un résultat a-t-il été enregistré dans la poule depuis son lancement ? */
    public static boolean aUnResultatDepuisLeLancement(PouleOrdonnancable p) {
        LocalDateTime lancement = p.getPoule().getInstantLancement();
        if (lancement == null) {
            return false;
        }
        for (MatchOrdonnancable mo : p.getMatchs()) {
            Match m = mo.getMatch();
            if (m.getStatut() == StatutMatch.TERMINE && (m.getInstantFin() == null
                    || !m.getInstantFin().isBefore(lancement))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Reprise : une poule lancée en bloc (statuts et table déjà relus) occupe de
     * nouveau sa table.
     *
     * @throws IllegalStateException si la table est déjà occupée
     */
    public void restaurerPoule(int table, PouleOrdonnancable p) {
        Objects.requireNonNull(p, "p");
        if (!occupationTables.containsKey(table)) {
            throw new IllegalArgumentException("Table hors bornes : " + table);
        }
        if (!estLibre(table)) {
            throw new IllegalStateException("La table " + table + " est déjà occupée.");
        }
        ajouterPoule(p);
        occupationPoules.put(table, p);
    }

    /** Reprise : poule en cours sans table valable ; ses joueurs restent occupés. */
    public void restaurerPouleHorsTable(PouleOrdonnancable p) {
        Objects.requireNonNull(p, "p");
        ajouterPoule(p);
        if (!poulesHorsTable.contains(p)) {
            poulesHorsTable.add(p);
        }
    }

    /**
     * Vérifie qu'un match peut être envoyé sur une table précise (choix manuel
     * du juge-arbitre), sans rien modifier.
     */
    public ResultatAffectationTable verifierAffectation(MatchOrdonnancable mo, int table) {
        Objects.requireNonNull(mo, "mo");
        if (table < 1 || table > nombreDeTables + tablesRenfortMax) {
            return ResultatAffectationTable.de(ResultatAffectationTable.Motif.TABLE_HORS_LIMITES, table);
        }
        if (table > nombreDeTables + tablesRenfortEnService) {
            // B054 : table d'échauffement que le juge-arbitre n'a pas ouverte.
            return ResultatAffectationTable.de(ResultatAffectationTable.Motif.TABLE_RENFORT_HORS_SERVICE, table);
        }
        ResultatAffectationTable match = verifierMatch(mo);
        if (match.getMotif() == ResultatAffectationTable.Motif.JOUEUR_DEJA_EN_JEU) {
            return match.getTableDuJoueurBloquant() == null
                    ? ResultatAffectationTable.joueurEnJeuSansTable(table, match.getJoueurBloquant())
                    : ResultatAffectationTable.joueurEnJeu(table, match.getJoueurBloquant(),
                    match.getTableDuJoueurBloquant());
        }
        if (!match.estReussi()) {
            return match;
        }
        if (!estLibre(table)) {
            return ResultatAffectationTable.de(ResultatAffectationTable.Motif.TABLE_OCCUPEE, table);
        }
        if (!reservations.admet(table, mo.getIdentifiantTableau())) {
            return ResultatAffectationTable.tableReservee(table, reservations.tableauxAdmis(table));
        }
        return ResultatAffectationTable.de(ResultatAffectationTable.Motif.AFFECTE, table);
    }

    /**
     * Envoie un match sur une table choisie : vérifie d'abord (table libre et
     * existante, match à jouer, aucun joueur déjà en jeu), puis lance le match
     * (statut en cours, numéro de table, heure de début). Rien n'est modifié en
     * cas de refus.
     */
    public ResultatAffectationTable affecter(MatchOrdonnancable mo, int table, LocalDateTime maintenant) {
        Objects.requireNonNull(maintenant, "maintenant");
        ResultatAffectationTable r = verifierAffectation(mo, table);
        if (r.estReussi()) {
            lancer(new PropositionLancement(mo, table, 0.0, "choix du juge-arbitre"), maintenant);
        }
        return r;
    }

    /**
     * Annule une affectation faite par erreur : la table redevient libre et le
     * match redevient à jouer, sans numéro de table ni heure de début.
     */
    public ResultatAffectationTable annulerLancement(MatchOrdonnancable mo) {
        Objects.requireNonNull(mo, "mo");
        PouleOrdonnancable p = pouleDuMatch.get(mo);
        if (p != null && p.estEnCours()) {
            return annulerLancementPoule(p); // le lancement porte sur la poule entière
        }
        Match m = mo.getMatch();
        Integer table = m.getNumeroTable();
        if (m.getStatut() != StatutMatch.EN_COURS) {
            return ResultatAffectationTable.de(ResultatAffectationTable.Motif.MATCH_PAS_EN_COURS,
                    table == null ? 0 : table);
        }
        if (table != null && mo.equals(occupationTables.get(table))) {
            occupationTables.put(table, null);
        }
        enCoursHorsTable.remove(mo);
        m.setStatut(StatutMatch.A_JOUER);
        m.setNumeroTable(null);
        m.setInstantLancement(null);
        return ResultatAffectationTable.de(ResultatAffectationTable.Motif.AFFECTATION_ANNULEE,
                table == null ? 0 : table);
    }

    // -------------------------------------------------------------------------
    // Restauration (reprise de session)
    // -------------------------------------------------------------------------

    /**
     * Marque une table comme occupée par un match déjà en cours, sans modifier
     * le {@link Match} (dont le statut, la table et l'horodatage de lancement ont
     * déjà été restaurés). Réservé au rechargement d'une session interrompue :
     * l'occupation des tables se reconstruit à partir des matchs en cours plutôt
     * que d'être relancée.
     *
     * @throws IllegalArgumentException si la table est hors bornes
     * @throws IllegalStateException    si la table est déjà occupée
     */
    public void restaurerOccupation(int table, MatchOrdonnancable mo) {
        Objects.requireNonNull(mo, "mo");
        if (!occupationTables.containsKey(table)) {
            throw new IllegalArgumentException("Table hors bornes : " + table);
        }
        if (!estLibre(table)) {
            throw new IllegalStateException("La table " + table + " est déjà occupée.");
        }
        gerer(mo);
        occupationTables.put(table, mo);
    }

    /**
     * Déclare un match en cours dont la table n'est pas valable (anomalie à la
     * reprise) : il n'occupe aucune table, mais ses deux joueurs restent occupés
     * et ne peuvent être envoyés sur aucune table tant qu'il n'est pas terminé
     * ou annulé.
     */
    public void restaurerEnCoursHorsTable(MatchOrdonnancable mo) {
        Objects.requireNonNull(mo, "mo");
        gerer(mo);
        if (!enCoursHorsTable.contains(mo)) {
            enCoursHorsTable.add(mo);
        }
    }

    // -------------------------------------------------------------------------
    // Index d'une vague de propositions (B025 : performance)
    // -------------------------------------------------------------------------

    /**
     * Index calculés une fois par vague : joueurs occupés, nombre de matchs
     * restants par joueur et par paire de joueurs, premier lancement de chaque
     * tableau. Donne exactement les mêmes valeurs que
     * {@link #compterEngagementsAilleurs} et {@link #minutesDepuisDebutTableau},
     * en temps constant par match au lieu d'un parcours de tous les matchs.
     */
    private final class Contexte {
        private final LocalDateTime maintenant;
        private final Set<String> occupes = joueursOccupes();
        private final Map<String, Integer> restantsParJoueur = new HashMap<>();
        private final Map<String, Integer> restantsParPaire = new HashMap<>();
        private final Set<MatchOrdonnancable> restants = new HashSet<>();
        private final Map<String, LocalDateTime> debutTableau = new HashMap<>();

        Contexte(LocalDateTime maintenant) {
            this.maintenant = maintenant;
            for (MatchOrdonnancable mo : matchs) {
                Match m = mo.getMatch();
                LocalDateTime l = m.getInstantLancement();
                if (l != null) {
                    debutTableau.merge(mo.getIdentifiantTableau(), l, (x, y) -> x.isBefore(y) ? x : y);
                }
                if (m.getStatut() == StatutMatch.TERMINE || m.estBye()) {
                    continue;
                }
                restants.add(mo);
                String x = licence(m.getJoueurA());
                String y = licence(m.getJoueurB());
                if (x != null) {
                    restantsParJoueur.merge(x, 1, Integer::sum);
                }
                if (y != null && !y.equals(x)) {
                    restantsParJoueur.merge(y, 1, Integer::sum);
                }
                if (x != null && y != null && !x.equals(y)) {
                    restantsParPaire.merge(paire(x, y), 1, Integer::sum);
                }
            }
        }

        int engagementsAilleurs(MatchOrdonnancable mo) {
            Match ref = mo.getMatch();
            String a = licence(ref.getJoueurA());
            String b = licence(ref.getJoueurB());
            int union;
            if (a == null && b == null) {
                return 0;
            } else if (a == null || b == null || a.equals(b)) {
                union = restantsParJoueur.getOrDefault(a == null ? b : a, 0);
            } else {
                union = restantsParJoueur.getOrDefault(a, 0) + restantsParJoueur.getOrDefault(b, 0)
                        - restantsParPaire.getOrDefault(paire(a, b), 0);
            }
            // Le match lui-même figure dans l'union s'il est encore à jouer.
            return restants.contains(mo) ? union - 1 : union;
        }

        /** Tour le moins avancé encore à finir, par tableau (poules : -1), calculé à la demande. */
        private Map<String, Integer> tourMinimalRestant;
        private final Map<Match, Integer> memoProfondeur = new IdentityHashMap<>();

        /** Tour de la finale de chaque tableau (le plus avancé de son tableau final), calculé à la demande. */
        private Map<String, Integer> tourDeLaFinale;
        /** Vrai s'il reste, tous tableaux confondus, un match à jouer qui n'est pas une finale. */
        private Boolean resteAutreChoseQueDesFinales;

        /** Vrai si ce match est une finale (ou une petite finale) de son tableau final. */
        boolean estFinale(MatchOrdonnancable mo) {
            if (mo.getPhase() == PhaseMatch.POULE) {
                return false;
            }
            if (tourDeLaFinale == null) {
                tourDeLaFinale = new HashMap<>();
                for (MatchOrdonnancable x : matchs) {
                    if (x.getPhase() != PhaseMatch.POULE) {
                        tourDeLaFinale.merge(x.getIdentifiantTableau(),
                                profondeur(x.getMatch(), memoProfondeur), Math::max);
                    }
                }
            }
            Integer f = tourDeLaFinale.get(mo.getIdentifiantTableau());
            return f != null && profondeur(mo.getMatch(), memoProfondeur) == f;
        }

        /**
         * Finales en fin de journée : vrai si ce match est une finale et qu'il
         * reste autre chose à jouer dans la journée (un autre match, ou un
         * tableau du jour pas encore commencé).
         */
        boolean finaleRetenue(MatchOrdonnancable mo) {
            if (!estFinale(mo)) {
                return false;
            }
            if (!tableauxAttendus.isEmpty()) {
                return true;
            }
            if (resteAutreChoseQueDesFinales == null) {
                resteAutreChoseQueDesFinales = false;
                for (MatchOrdonnancable r : restants) {
                    if (!estFinale(r)) {
                        resteAutreChoseQueDesFinales = true;
                        break;
                    }
                }
            }
            return resteAutreChoseQueDesFinales;
        }

        /**
         * Tour par tour : vrai si le tableau de ce match a encore des matchs à
         * finir dans un tour précédent (ou des poules à finir).
         */
        boolean retenuParLeTour(MatchOrdonnancable mo) {
            if (mo.getPhase() == PhaseMatch.POULE) {
                return false;
            }
            if (tourMinimalRestant == null) {
                tourMinimalRestant = new HashMap<>();
                for (MatchOrdonnancable r : restants) {
                    int t = r.getPhase() == PhaseMatch.POULE ? -1 : profondeur(r.getMatch(), memoProfondeur);
                    tourMinimalRestant.merge(r.getIdentifiantTableau(), t, Math::min);
                }
            }
            Integer min = tourMinimalRestant.get(mo.getIdentifiantTableau());
            return min != null && min < profondeur(mo.getMatch(), memoProfondeur);
        }

        long minutesDepuisDebutTableau(String tableau) {
            LocalDateTime debut = debutTableau.get(tableau);
            return debut == null ? 0 : Math.max(0, Duration.between(debut, maintenant).toMinutes());
        }
    }

    private static String licence(Joueur j) {
        return j == null ? null : j.getNumeroLicence();
    }

    private static String paire(String x, String y) {
        return x.compareTo(y) < 0 ? x + "\u0000" + y : y + "\u0000" + x;
    }
}
