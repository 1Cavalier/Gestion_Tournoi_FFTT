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

        // Matchs jouables, triés par score décroissant.
        List<MatchOrdonnancable> jouables = new ArrayList<>();
        for (MatchOrdonnancable mo : matchs) {
            if (estJouableMaintenant(mo)) {
                jouables.add(mo);
            }
        }
        jouables.sort(Comparator.comparingDouble((MatchOrdonnancable mo) ->
                scorePriorite(mo, maintenant)).reversed());

        // Affectation gloutonne : on descend la liste triée, en réservant les
        // joueurs déjà engagés dans cette vague pour ne pas les mettre 2 fois.
        List<PropositionLancement> propositions = new ArrayList<>();
        Set<String> joueursReserves = new HashSet<>(joueursOccupes());
        int indexTable = 0;

        for (MatchOrdonnancable mo : jouables) {
            if (indexTable >= libres.size()) {
                break;
            }
            Match m = mo.getMatch();
            String a = m.getJoueurA().getNumeroLicence();
            String b = m.getJoueurB().getNumeroLicence();
            if (joueursReserves.contains(a) || joueursReserves.contains(b)) {
                continue; // un joueur déjà pris dans cette vague
            }
            int table = libres.get(indexTable++);
            double score = scorePriorite(mo, maintenant);
            String explication = expliquer(mo, maintenant);
            propositions.add(new PropositionLancement(mo, table, score, explication));
            joueursReserves.add(a);
            joueursReserves.add(b);
        }
        return propositions;
    }

    /**
     * Calcule le score de priorité d'un match jouable. Plus il est élevé, plus
     * le match doit être lancé tôt.
     */
    public double scorePriorite(MatchOrdonnancable mo, LocalDateTime maintenant) {
        double score = 0.0;

        // 1) Avancement par vagues : POULE > 1er tour > tours suivants.
        //    On donne un bonus décroissant selon la phase.
        double bonusPhase = switch (mo.getPhase()) {
            case POULE -> 3.0;
            case PREMIER_TOUR_TABLEAU -> 2.0;
            case TOUR_SUIVANT_TABLEAU -> 1.0;
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
        int engagements = compterEngagementsAilleurs(mo);
        score += poids.getPoidsDeblocage() * engagements;

        return score;
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

    private String expliquer(MatchOrdonnancable mo, LocalDateTime maintenant) {
        List<String> raisons = new ArrayList<>();
        raisons.add(switch (mo.getPhase()) {
            case POULE -> "poule";
            case PREMIER_TOUR_TABLEAU -> "1er tour";
            case TOUR_SUIVANT_TABLEAU -> "tour suivant";
        });
        if (mo.getPretDepuis() != null) {
            long min = Math.max(0, Duration.between(mo.getPretDepuis(), maintenant).toMinutes());
            if (min > 0) {
                raisons.add("attend " + min + " min");
            }
        }
        int engagements = compterEngagementsAilleurs(mo);
        if (engagements > 0) {
            raisons.add("débloque " + engagements + " match(s)");
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
}
