package com.pingmanager.ordonnancement;

import com.pingmanager.model.Joueur;
import com.pingmanager.model.Match;
import com.pingmanager.model.StatutMatch;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
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
 * <p><b>Temps.</b> Le lancement horodate le match automatiquement ; la fin est
 * posée à la saisie du score. Aucun temps de repos n'est imposé : seule la
 * contrainte « un joueur, une table » s'applique.</p>
 */
public final class Ordonnanceur {

    private final int nombreDeTables;
    private final PoidsOrdonnancement poids;

    /** Tous les matchs sous gestion de l'ordonnanceur. */
    private final List<MatchOrdonnancable> matchs = new ArrayList<>();

    /** Table (1..N) → match en cours dessus, ou null si libre. */
    private final Map<Integer, MatchOrdonnancable> occupationTables = new HashMap<>();

    /** Tables réservées et préférences des tableaux (B024) ; aucune par défaut. */
    private ReservationsTables reservations = ReservationsTables.AUCUNE;

    public Ordonnanceur(int nombreDeTables) {
        this(nombreDeTables, new PoidsOrdonnancement());
    }

    public Ordonnanceur(int nombreDeTables, PoidsOrdonnancement poids) {
        if (nombreDeTables < 1) {
            throw new IllegalArgumentException("Il faut au moins une table.");
        }
        this.nombreDeTables = nombreDeTables;
        this.poids = Objects.requireNonNull(poids, "poids");
        for (int t = 1; t <= nombreDeTables; t++) {
            occupationTables.put(t, null);
        }
    }

    /** Ajoute un match à gérer. Sans effet s'il est déjà présent. */
    public void ajouterMatch(MatchOrdonnancable m) {
        Objects.requireNonNull(m, "m");
        if (!matchs.contains(m)) {
            matchs.add(m);
        }
    }


    /** Vue en lecture seule de tous les matchs sous gestion. */
    public List<MatchOrdonnancable> tousLesMatchs() {
        return java.util.Collections.unmodifiableList(matchs);
    }

    public int getNombreDeTables() {
        return nombreDeTables;
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

    /** Tables actuellement libres (numéros croissants). */
    public List<Integer> tablesLibres() {
        List<Integer> libres = new ArrayList<>();
        for (int t = 1; t <= nombreDeTables; t++) {
            if (occupationTables.get(t) == null) {
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
        return occupes;
    }

    /**
     * Indique si un match est jouable maintenant : statut A_JOUER, deux joueurs
     * connus (non « à déterminer »), et aucun des deux déjà occupé sur une table.
     */
    public boolean estJouableMaintenant(MatchOrdonnancable mo) {
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
        List<MatchOrdonnancable> jouables = new ArrayList<>();
        Map<MatchOrdonnancable, Double> scores = new HashMap<>();
        for (MatchOrdonnancable mo : matchs) {
            if (estJouable(mo, ctx.occupes)) {
                jouables.add(mo);
                scores.put(mo, score(mo, maintenant, ctx));
            }
        }
        jouables.sort(Comparator.comparingDouble((MatchOrdonnancable mo) -> scores.get(mo)).reversed());

        // Affectation gloutonne : on descend la liste triée, en réservant les
        // joueurs déjà engagés dans cette vague pour ne pas les mettre 2 fois.
        List<PropositionLancement> propositions = new ArrayList<>();
        Set<String> joueursReserves = new HashSet<>(ctx.occupes);
        // B024 : chaque match prend la table libre qui lui convient le mieux (table
        // préférée de son tableau, sinon table ordinaire), jamais une table réservée
        // à d'autres tableaux. Sans réservation : les tables libres dans l'ordre.
        List<Integer> restantes = new ArrayList<>(libres);

        for (MatchOrdonnancable mo : jouables) {
            if (restantes.isEmpty()) {
                break;
            }
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
            double score = scores.get(mo);
            String explication = expliquer(mo, maintenant, ctx);
            propositions.add(new PropositionLancement(mo, table, score, explication));
            joueursReserves.add(a);
            joueursReserves.add(b);
        }
        return propositions;
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
        double score = 0.0;

        // 1) Avancement par vagues : POULE > 1er tour > tours suivants.
        //    On donne un bonus décroissant selon la phase.
        double bonusPhase = switch (mo.getPhase().name()) {
            case "POULE" -> 3.0;
            case "PREMIER_TOUR_TABLEAU" -> 2.0;
            case "TOUR_SUIVANT_TABLEAU" -> 1.0;
            default -> throw new IllegalStateException("Phase inconnue : " + mo.getPhase());
        };
        score += poids.getPoidsVague() * bonusPhase;

        // 2) Équité : ancienneté d'attente (minutes depuis que le match est prêt).
        long minutesAttente = 0;
        if (mo.getPretDepuis() != null) {
            minutesAttente = Math.max(0, Duration.between(mo.getPretDepuis(), maintenant).toMinutes());
        }
        score += poids.getPoidsAttente() * poids.getPointsParMinuteAttente() * minutesAttente;

        // 3) Déblocage : si un joueur de ce match est engagé dans d'autres matchs
        //    encore à jouer (typiquement inscrit à deux tableaux), le lancer
        //    libère plus vite la suite. On compte ces engagements.
        int engagements = ctx == null ? compterEngagementsAilleurs(mo) : ctx.engagementsAilleurs(mo);
        score += poids.getPoidsDeblocage() * engagements;

        // 4) Tableau commencé tôt (B025) : minutes depuis le premier match lancé
        //    du tableau. Inactif avec les poids par défaut.
        if (poids.getPoidsTableauCommence() != 0.0) {
            long minutesTableau = ctx == null ? minutesDepuisDebutTableau(mo, maintenant)
                    : ctx.minutesDepuisDebutTableau(mo);
            score += poids.getPoidsTableauCommence() * minutesTableau;
        }

        return score;
    }

    /**
     * Minutes écoulées depuis le lancement du premier match du tableau de ce
     * match (0 si le tableau n'a encore rien lancé).
     */
    long minutesDepuisDebutTableau(MatchOrdonnancable mo, LocalDateTime maintenant) {
        LocalDateTime debut = null;
        for (MatchOrdonnancable autre : matchs) {
            LocalDateTime l = autre.getMatch().getInstantLancement();
            if (l != null && autre.getIdentifiantTableau().equals(mo.getIdentifiantTableau())
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
        List<String> raisons = new ArrayList<>();
        raisons.add(switch (mo.getPhase().name()) {
            case "POULE" -> "poule";
            case "PREMIER_TOUR_TABLEAU" -> "1er tour";
            case "TOUR_SUIVANT_TABLEAU" -> "tour suivant";
            default -> throw new IllegalStateException("Phase inconnue : " + mo.getPhase());
        });
        if (mo.getPretDepuis() != null) {
            long min = Math.max(0, Duration.between(mo.getPretDepuis(), maintenant).toMinutes());
            if (min > 0) {
                raisons.add("attend " + min + " min");
            }
        }
        int engagements = ctx == null ? compterEngagementsAilleurs(mo) : ctx.engagementsAilleurs(mo);
        if (engagements > 0) {
            raisons.add("débloque " + engagements + " match(s)");
        }
        if (poids.getPoidsTableauCommence() != 0.0) {
            long minutesTableau = ctx == null ? minutesDepuisDebutTableau(mo, maintenant)
                    : ctx.minutesDepuisDebutTableau(mo);
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
        MatchOrdonnancable mo = proposition.getMatch();
        int table = proposition.getNumeroTable();
        if (occupationTables.get(table) != null) {
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
        m.setInstantFin(maintenant);
    }

    /** Match en cours sur une table donnée, ou null si libre. */
    public MatchOrdonnancable matchSurTable(int table) {
        return occupationTables.get(table);
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
        for (int t = 1; t <= nombreDeTables; t++) {
            MatchOrdonnancable mo = occupationTables.get(t);
            if (mo != null && joue(mo.getMatch(), licence)) {
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
        if (!matchs.contains(mo)) {
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
        // Contrainte dure : un joueur n'est jamais sur deux tables à la fois.
        for (Joueur j : List.of(a, b)) {
            Integer table = tableDuJoueur(j.getNumeroLicence());
            if (table != null) {
                return ResultatAffectationTable.joueurEnJeu(0, j, table);
            }
        }
        return ResultatAffectationTable.de(ResultatAffectationTable.Motif.AFFECTE, 0);
    }

    /**
     * Vérifie qu'un match peut être envoyé sur une table précise (choix manuel
     * du juge-arbitre), sans rien modifier.
     */
    public ResultatAffectationTable verifierAffectation(MatchOrdonnancable mo, int table) {
        Objects.requireNonNull(mo, "mo");
        if (table < 1 || table > nombreDeTables) {
            return ResultatAffectationTable.de(ResultatAffectationTable.Motif.TABLE_HORS_LIMITES, table);
        }
        ResultatAffectationTable match = verifierMatch(mo);
        if (match.getMotif() == ResultatAffectationTable.Motif.JOUEUR_DEJA_EN_JEU) {
            return ResultatAffectationTable.joueurEnJeu(table, match.getJoueurBloquant(),
                    match.getTableDuJoueurBloquant());
        }
        if (!match.estReussi()) {
            return match;
        }
        if (occupationTables.get(table) != null) {
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
        Match m = mo.getMatch();
        Integer table = m.getNumeroTable();
        if (m.getStatut() != StatutMatch.EN_COURS) {
            return ResultatAffectationTable.de(ResultatAffectationTable.Motif.MATCH_PAS_EN_COURS,
                    table == null ? 0 : table);
        }
        if (table != null && mo.equals(occupationTables.get(table))) {
            occupationTables.put(table, null);
        }
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
        if (occupationTables.get(table) != null) {
            throw new IllegalStateException("La table " + table + " est déjà occupée.");
        }
        if (!matchs.contains(mo)) {
            matchs.add(mo);
        }
        occupationTables.put(table, mo);
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

        long minutesDepuisDebutTableau(MatchOrdonnancable mo) {
            LocalDateTime debut = debutTableau.get(mo.getIdentifiantTableau());
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
