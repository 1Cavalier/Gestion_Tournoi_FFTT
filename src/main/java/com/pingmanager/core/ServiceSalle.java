package com.pingmanager.core;

import com.pingmanager.model.Joueur;
import com.pingmanager.model.Match;
import com.pingmanager.model.Poule;
import com.pingmanager.model.StatutMatch;
import com.pingmanager.model.StatutTableau;
import com.pingmanager.model.StatutTournoi;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tour;
import com.pingmanager.model.Tournoi;
import com.pingmanager.ordonnancement.CategorieDuree;
import com.pingmanager.ordonnancement.EstimateurDuree;
import com.pingmanager.ordonnancement.MatchOrdonnancable;
import com.pingmanager.ordonnancement.Ordonnanceur;
import com.pingmanager.ordonnancement.PhaseMatch;
import com.pingmanager.ordonnancement.PoidsOrdonnancement;
import com.pingmanager.ordonnancement.PouleOrdonnancable;
import com.pingmanager.ordonnancement.PrevisionSalle;
import com.pingmanager.ordonnancement.PropositionLancement;
import com.pingmanager.ordonnancement.ResultatAffectationTable;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Pilotage de la salle le jour J : affectation des matchs aux tables.
 *
 * <p><b>Source de vérité unique : le tournoi.</b> Un match en cours porte déjà
 * son statut ({@link StatutMatch#EN_COURS}), son numéro de table et son heure de
 * début, et ces champs sont enregistrés avec le tournoi. Chaque appel
 * reconstruit donc l'{@link Ordonnanceur} à partir du tournoi : l'occupation des
 * tables se déduit des matchs en cours, et un score saisi depuis n'importe quel
 * écran (poules, tableau final) libère sa table sans autre formalité. La salle
 * survit ainsi à une fermeture du logiciel : il suffit de rouvrir le tournoi.</p>
 *
 * <p><b>Règle dure</b> (reprise de l'{@link Ordonnanceur}) : un joueur n'est
 * jamais sur deux tables à la fois, ni par le bouton « Proposer » ni par un
 * choix manuel ; une table occupée ne reçoit pas un second match. Tout refus
 * renvoie un {@link ResultatAffectationTable} avec son motif.</p>
 *
 * <p><b>Attente.</b> La priorité de l'ordonnanceur tient compte du temps
 * d'attente d'un match prêt. Cet instant n'est pas enregistré : il se déduit
 * du tournoi (dernier match terminé de chacun des deux joueurs, sinon premier
 * match lancé de son tableau), ce qui donne le même ordre après une reprise.</p>
 *
 * <p>Règlements : le juge-arbitre est responsable de « la programmation des
 * parties en fonction de l'horaire retenu et des tables disponibles »
 * (3.3.1.2.2) et de « l'horaire des parties » (IX.109). Le choix lui revient
 * donc toujours : l'ordre de priorité et le bouton « Proposer » ne sont qu'une
 * aide, et une affectation se corrige ({@link #annulerAffectation}). Aucun temps
 * de repos entre deux parties n'est imposé par les textes (voir le README, section 6).</p>
 *
 * <p><b>Phase de poules : on lance une poule, pas un match.</b> Comme en salle, le
 * juge-arbitre envoie une poule entière sur une table avec sa feuille de poule
 * ({@link #lancerPoule}) : tous ses joueurs doivent être présents (pointés) et
 * libres, et la table libre. Ses matchs se jouent à la suite sur cette table,
 * dans l'ordre officiel I.301 déjà calculé par {@link GenerateurMatchsPoule}.
 * Pendant toute la poule, la table et tous ses joueurs sont occupés ; ils sont
 * libérés au dernier résultat (score, forfait, abandon, disqualification). Le
 * lancement s'annule tant qu'aucun résultat n'y est saisi
 * ({@link #annulerLancementPoule}). Un match de poule déjà lancé seul (ancien
 * fichier) garde sa table jusqu'à son résultat ; le reste de la poule part
 * ensuite en bloc. Le tableau final se lance toujours match par match.</p>
 *
 * <p><b>Durées.</b> La durée prévue d'une poule est la somme des durées prévues
 * de ses matchs ({@link EstimateurDuree}, 20 min par partie au départ), affinée à
 * chaque appel par les durées réelles des matchs terminés du tournoi, match par
 * match.</p>
 */
public final class ServiceSalle {

    private final Horloge horloge;
    private final ZoneId zone;
    /** Poids imposés par l'appelant, ou {@code null} : ceux de la stratégie du tournoi (B025). */
    private final PoidsOrdonnancement poids;

    public ServiceSalle() {
        this(Horloge.systeme());
    }

    public ServiceSalle(Horloge horloge) {
        this(horloge, ZoneId.systemDefault());
    }

    /**
     * Salle dont la priorité suit la stratégie d'ordonnancement choisie par le
     * club pour chaque tournoi (B025 ; stratégie par défaut sans choix).
     */
    public ServiceSalle(Horloge horloge, ZoneId zone) {
        this.horloge = Objects.requireNonNull(horloge, "horloge");
        this.zone = Objects.requireNonNull(zone, "zone");
        this.poids = null;
    }

    /** Salle avec des poids imposés, quelle que soit la stratégie du tournoi. */
    public ServiceSalle(Horloge horloge, ZoneId zone, PoidsOrdonnancement poids) {
        this.horloge = Objects.requireNonNull(horloge, "horloge");
        this.zone = Objects.requireNonNull(zone, "zone");
        this.poids = Objects.requireNonNull(poids, "poids");
    }

    /** Durée de départ d'une partie, avant apprentissage (même valeur que les prévisions). */
    public static final Duration DUREE_PARTIE_PAR_DEFAUT = ScenarioSimulation.DUREE_PARTIE_PAR_DEFAUT;
    /** Au-delà, une durée observée est jugée aberrante (match laissé en cours) et n'est pas apprise. */
    static final Duration DUREE_OBSERVEE_MAX = Duration.ofMinutes(120);

    /** Heure courante de la salle (port {@link Horloge}). */
    public LocalDateTime maintenant() {
        return LocalDateTime.ofInstant(horloge.maintenant(), zone);
    }

    // ==================================================================
    // Lecture
    // ==================================================================

    /** Photographie de la salle : tables, matchs prêts, bloqués et anomalies. */
    public TableauDeBordSalle tableauDeBord(Tournoi tournoi) {
        return new Salle(tournoi, maintenant()).photographier();
    }

    // ==================================================================
    // Actions
    // ==================================================================

    /**
     * Envoie un match sur une table choisie par le juge-arbitre (menu ou
     * glisser-déposer). Rien n'est modifié en cas de refus.
     */
    public ResultatAffectationTable affecter(Tournoi tournoi, Match match, int table) {
        Objects.requireNonNull(match, "match");
        Salle salle = new Salle(tournoi, maintenant());
        ResultatAffectationTable r = salle.verifier(match, table);
        if (!r.estReussi()) {
            return r;
        }
        return salle.ordonnanceur.affecter(salle.parMatch.get(match), table, salle.maintenant);
    }

    /**
     * Dit, sans rien modifier, si un match peut être envoyé maintenant sur une
     * table : tournoi ouvert, table existante et libre (et admise pour ce
     * tableau), match à jouer, deux joueurs connus et disponibles (aucun des deux
     * n'est en train de jouer, même sur un match en cours sans table valable).
     * Mêmes règles que {@link #affecter}, qui les refait toujours au lancement.
     */
    public ResultatAffectationTable verifierLancement(Tournoi tournoi, Match match, int table) {
        Objects.requireNonNull(match, "match");
        return new Salle(tournoi, maintenant()).verifier(match, table);
    }

    /**
     * Interface « Lancer un match » (B055, B057) : tous les matchs à jouer dont les
     * deux joueurs sont connus, par priorité de la stratégie en place, avec la
     * table proposée, les tables libres possibles et le motif de refus éventuel.
     */
    public ListeLancements listeLancements(Tournoi tournoi) {
        return new Salle(tournoi, maintenant()).lister();
    }

    /**
     * Bouton « Proposer » : remplit les tables libres avec les matchs prêts, par
     * ordre de priorité de l'ordonnanceur, sans jamais mettre un joueur sur deux
     * tables.
     */
    public ResultatPropositionSalle proposer(Tournoi tournoi) {
        Salle salle = new Salle(tournoi, maintenant());
        if (tournoi.getStatut() == StatutTournoi.ANNULE) {
            return ResultatPropositionSalle.aucun(ResultatPropositionSalle.Motif.TOURNOI_FERME);
        }
        if (salle.nombreDeTables == 0) {
            return ResultatPropositionSalle.aucun(ResultatPropositionSalle.Motif.SALLE_SANS_TABLE);
        }
        if (salle.ordonnanceur.tablesLibres().isEmpty()) {
            return ResultatPropositionSalle.aucun(ResultatPropositionSalle.Motif.AUCUNE_TABLE_LIBRE);
        }
        List<PropositionLancement> propositions = salle.ordonnanceur.proposerLancements(salle.maintenant);
        if (propositions.isEmpty()) {
            boolean pretsBloquesParReservation = !salle.ordonnanceur.getReservations().estVide()
                    && salle.ordre.stream().anyMatch(salle.ordonnanceur::estJouableMaintenant);
            return ResultatPropositionSalle.aucun(pretsBloquesParReservation
                    ? ResultatPropositionSalle.Motif.TABLES_LIBRES_RESERVEES
                    : ResultatPropositionSalle.Motif.AUCUN_MATCH_PRET);
        }
        List<ResultatPropositionSalle.Lancement> lances = new ArrayList<>();
        for (PropositionLancement p : propositions) {
            TableauDeBordSalle.MatchSalle vu = salle.vue(p.getMatch(), p.getScore(), p.getExplication());
            salle.ordonnanceur.lancer(p, salle.maintenant);
            lances.add(new ResultatPropositionSalle.Lancement(vu, p.getNumeroTable()));
        }
        return ResultatPropositionSalle.lances(lances);
    }

    // ------------------------------------------------------------------
    // Poules : lancement en bloc (phase de poules)
    // ------------------------------------------------------------------

    /**
     * Envoie une poule entière sur une table (phase de poules) : tous ses joueurs
     * présents et libres, table libre. Ses matchs passent en cours sur cette table
     * et s'y jouent à la suite dans l'ordre I.301. Rien n'est modifié en cas de refus
     * (motif précis : « DURAND joue en table 4 », « Aucune table libre »…).
     */
    public ResultatAffectationTable lancerPoule(Tournoi tournoi, Poule poule, int table) {
        Objects.requireNonNull(poule, "poule");
        Salle salle = new Salle(tournoi, maintenant());
        ResultatAffectationTable r = salle.verifierPoule(poule, table);
        if (!r.estReussi()) {
            return r;
        }
        return salle.ordonnanceur.affecterPoule(salle.parPoule.get(poule), table, salle.maintenant);
    }

    /** Dit, sans rien modifier, si une poule peut partir maintenant sur cette table (règles de {@link #lancerPoule}). */
    public ResultatAffectationTable verifierLancementPoule(Tournoi tournoi, Poule poule, int table) {
        Objects.requireNonNull(poule, "poule");
        return new Salle(tournoi, maintenant()).verifierPoule(poule, table);
    }

    /**
     * Annule le lancement d'une poule tant qu'aucun résultat n'y a été saisi : la
     * table redevient libre, les matchs redeviennent à jouer. Ensuite, refus
     * ({@link ResultatAffectationTable.Motif#SCORE_DEJA_SAISI}) : le lancement reste.
     */
    public ResultatAffectationTable annulerLancementPoule(Tournoi tournoi, Poule poule) {
        Objects.requireNonNull(poule, "poule");
        Salle salle = new Salle(tournoi, maintenant());
        PouleOrdonnancable p = salle.parPoule.get(poule);
        if (p == null) {
            return ResultatAffectationTable.de(ResultatAffectationTable.Motif.MATCH_INCONNU, 0);
        }
        return salle.ordonnanceur.annulerLancementPoule(p);
    }

    /**
     * La poule vue depuis la salle (joueurs, ordre des matchs, table, heure, durée
     * prévue, fin estimée), ou vide si elle n'est pas dans un tableau en phase de poules.
     */
    public java.util.Optional<PouleALancer> poule(Tournoi tournoi, Poule poule) {
        Objects.requireNonNull(poule, "poule");
        Salle salle = new Salle(tournoi, maintenant());
        PouleOrdonnancable p = salle.parPoule.get(poule);
        return p == null ? java.util.Optional.empty() : java.util.Optional.of(salle.vuePoule(p));
    }

    /** Durée prévue d'une partie de cette phase, affinée par les matchs terminés du tournoi. */
    public Duration dureePrevuePartie(Tournoi tournoi, PhaseMatch phase) {
        return new Salle(tournoi, maintenant()).estimateur.estimer(new CategorieDuree(phase, 0));
    }

    /**
     * Annule une affectation faite par erreur (mauvais match ou mauvaise table) :
     * la table redevient libre et le match redevient à jouer. Pour un match d'une
     * poule lancée en bloc, c'est le lancement de la poule qui est annulé (tant
     * qu'aucun résultat n'y est saisi).
     */
    public ResultatAffectationTable annulerAffectation(Tournoi tournoi, Match match) {
        Objects.requireNonNull(match, "match");
        Salle salle = new Salle(tournoi, maintenant());
        MatchOrdonnancable mo = salle.parMatch.get(match);
        if (mo == null) {
            return ResultatAffectationTable.de(ResultatAffectationTable.Motif.MATCH_INCONNU, 0);
        }
        return salle.ordonnanceur.annulerLancement(mo);
    }

    /**
     * À appeler après la saisie du score d'un match joué sur une table (bouton
     * « Terminer ») : constate que la table est libérée et pose l'heure de fin si
     * la saisie ne l'a pas fait (tableau final).
     */
    public ResultatAffectationTable libererApresResultat(Tournoi tournoi, Match match) {
        Objects.requireNonNull(tournoi, "tournoi");
        Objects.requireNonNull(match, "match");
        int table = match.getNumeroTable() == null ? 0 : match.getNumeroTable();
        if (match.getStatut() == StatutMatch.EN_COURS) {
            return ResultatAffectationTable.de(ResultatAffectationTable.Motif.RESULTAT_NON_SAISI, table);
        }
        if (match.getStatut() != StatutMatch.TERMINE) {
            return ResultatAffectationTable.de(ResultatAffectationTable.Motif.MATCH_PAS_EN_COURS, table);
        }
        if (match.getInstantFin() == null) {
            match.setInstantFin(maintenant());
        }
        Poule poule = pouleDuMatch(tournoi, match);
        if (poule != null && poule.estLanceeEnBloc()) {
            if (!poule.estTerminee()) {
                // La poule continue : sa table et ses joueurs restent occupés.
                return ResultatAffectationTable.de(ResultatAffectationTable.Motif.POULE_CONTINUE,
                        poule.getNumeroTable());
            }
            // Dernier résultat : la table et les joueurs de la poule sont libérés.
            table = poule.getNumeroTable();
            poule.definirLancement(null, null);
        }
        return ResultatAffectationTable.de(ResultatAffectationTable.Motif.TABLE_LIBEREE, table);
    }

    /** Poule d'un match (identité), ou null s'il n'est pas un match de poule. */
    static Poule pouleDuMatch(Tournoi tournoi, Match match) {
        for (Tableau t : tournoi.getTableaux()) {
            for (Poule p : t.getPoules()) {
                for (Match m : p.getMatchs()) {
                    if (m == match) {
                        return p;
                    }
                }
            }
        }
        return null;
    }

    // ==================================================================
    // Reconstruction de la salle depuis le tournoi
    // ==================================================================

    /** État de travail : l'ordonnanceur rebâti et la description de chaque match. */
    private final class Salle {

        private final Tournoi tournoi;
        private final LocalDateTime maintenant;
        private final int nombreDeTables;
        private final Ordonnanceur ordonnanceur;
        /** Match → enveloppe d'ordonnancement (identité des instances du tournoi). */
        private final Map<Match, MatchOrdonnancable> parMatch = new IdentityHashMap<>();
        /** Enveloppe → libellé du tour. */
        private final Map<MatchOrdonnancable, String> tours = new IdentityHashMap<>();
        private final List<MatchOrdonnancable> ordre = new ArrayList<>();
        private final List<MatchOrdonnancable> anomalies = new ArrayList<>();
        /** Enveloppe → tableau, poule ou tour (nom et matchs), importance du tour. */
        private final Map<MatchOrdonnancable, Tableau> tableaux = new IdentityHashMap<>();
        private final Map<MatchOrdonnancable, String> groupes = new IdentityHashMap<>();
        private final Map<MatchOrdonnancable, List<Match>> matchsDuGroupe = new IdentityHashMap<>();
        private final Map<MatchOrdonnancable, String> importanceTour = new IdentityHashMap<>();
        /** Enveloppe → numéro du match dans son tableau (B057). */
        private final Map<MatchOrdonnancable, Integer> numeros = new IdentityHashMap<>();
        private final Map<Tableau, Integer> compteurs = new IdentityHashMap<>();
        /** Licence → fin de son dernier match ; tableau → premier match lancé. */
        private final Map<String, LocalDateTime> libreDepuis = new HashMap<>();
        private final Map<String, LocalDateTime> debutTableau = new HashMap<>();
        /** Poules des tableaux en phase de poules, comme unités de lancement (identité des poules). */
        private final Map<Poule, PouleOrdonnancable> parPoule = new IdentityHashMap<>();
        private final List<PouleOrdonnancable> poules = new ArrayList<>();
        /** Durées prévues des parties, apprises sur les matchs terminés du tournoi. */
        private final EstimateurDuree estimateur = new EstimateurDuree(DUREE_PARTIE_PAR_DEFAUT);
        private PrevisionSalle prevision;

        Salle(Tournoi tournoi, LocalDateTime maintenant) {
            this.tournoi = Objects.requireNonNull(tournoi, "tournoi");
            this.maintenant = maintenant;
            this.nombreDeTables = Math.max(0, tournoi.getNombreDeTables());
            // L'ordonnanceur exige au moins une table ; sans table renseignée, aucune
            // carte n'est montrée et toute affectation est refusée (hors limites).
            this.ordonnanceur = new Ordonnanceur(Math.max(1, nombreDeTables),
                    poids != null ? poids : PoidsOrdonnancement.pour(tournoi.getStrategieOrdonnancement()));
            recenser();
            this.ordonnanceur.setTableauxAttendus(tableauxDuJourPasCommences());
            restaurerOccupation();
            poserAttentes();
            apprendreDurees();
            this.prevision = new PrevisionSalle(ordonnanceur, estimateur, mo -> new CategorieDuree(mo.getPhase(), 0));
        }

        /**
         * Apprentissage des durées, match par match : chaque match terminé dont on
         * connaît le début et la fin affine la durée prévue de sa phase. Une durée
         * nulle (scores d'une poule saisis d'un coup) ou aberrante est ignorée.
         */
        private void apprendreDurees() {
            List<MatchOrdonnancable> termines = new ArrayList<>();
            for (MatchOrdonnancable mo : ordre) {
                Match m = mo.getMatch();
                if (m.getStatut() == StatutMatch.TERMINE && m.getInstantLancement() != null
                        && m.getInstantFin() != null) {
                    termines.add(mo);
                }
            }
            termines.sort(Comparator.comparing(mo -> mo.getMatch().getInstantFin()));
            for (MatchOrdonnancable mo : termines) {
                Duration d = Duration.between(mo.getMatch().getInstantLancement(), mo.getMatch().getInstantFin());
                if (!d.isNegative() && !d.isZero() && d.compareTo(DUREE_OBSERVEE_MAX) <= 0) {
                    estimateur.observer(new CategorieDuree(mo.getPhase(), 0), d);
                }
            }
        }

        /** Vue d'une poule : joueurs, ordre des matchs, table, durée prévue et fin estimée. */
        PouleALancer vuePoule(PouleOrdonnancable p) {
            List<Match> matchs = new ArrayList<>();
            p.getMatchs().forEach(mo -> matchs.add(mo.getMatch()));
            Poule poule = p.getPoule();
            boolean enCours = p.estEnCours();
            return new PouleALancer(poule, p.getNomTableau(), poule.getJoueurs(), matchs,
                    enCours ? poule.getNumeroTable() : null, enCours ? poule.getInstantLancement() : null,
                    prevision.dureePrevue(p), enCours ? prevision.finEstimee(p) : null);
        }

        /** Vérifications du lancement d'une poule sur une table, sans rien modifier. */
        ResultatAffectationTable verifierPoule(Poule poule, int table) {
            if (tournoi.getStatut() == StatutTournoi.ANNULE) {
                return ResultatAffectationTable.de(ResultatAffectationTable.Motif.TOURNOI_FERME, table);
            }
            if (table < 1 || table > nombreDeTables) {
                return ResultatAffectationTable.de(ResultatAffectationTable.Motif.TABLE_HORS_LIMITES, table);
            }
            PouleOrdonnancable p = parPoule.get(poule);
            if (p == null) {
                return ResultatAffectationTable.de(ResultatAffectationTable.Motif.MATCH_INCONNU, table);
            }
            return ordonnanceur.verifierAffectationPoule(p, table);
        }

        /**
         * Tableaux du jour (début des matchs aujourd'hui) pas encore lancés : avec la
         * stratégie « petit à petit », les finales attendent qu'ils soient joués.
         */
        private List<String> tableauxDuJourPasCommences() {
            List<String> attendus = new ArrayList<>();
            if (maintenant == null) {
                return attendus;
            }
            for (Tableau t : tournoi.getTableaux()) {
                boolean commence = t.getStatut() == StatutTableau.LANCE || t.getStatut() == StatutTableau.PHASE_FINALE;
                LocalDateTime debut = t.getHoraires() == null ? null : t.getHoraires().getDebutMatchs();
                if (!commence && debut != null && debut.toLocalDate().equals(maintenant.toLocalDate())) {
                    attendus.add(t.getNom());
                }
            }
            return attendus;
        }

        private void recenser() {
            for (Tableau t : tournoi.getTableaux()) {
                if (t.getStatut() != StatutTableau.LANCE && t.getStatut() != StatutTableau.PHASE_FINALE) {
                    continue;
                }
                for (Poule p : t.getPoules()) {
                    List<Match> matchs = p.getMatchs();
                    List<MatchOrdonnancable> unite = new ArrayList<>();
                    for (int i = 0; i < matchs.size(); i++) {
                        // I.301 : tour de la partie dans l'ordre officiel de la poule.
                        ajouter(matchs.get(i), t, "Poule " + p.getNom() + " · tour " + SuiviPoule.tourDuMatch(p, i),
                                PhaseMatch.POULE, "Poule " + p.getNom(), matchs, null);
                        MatchOrdonnancable mo = parMatch.get(matchs.get(i));
                        if (mo != null) {
                            unite.add(mo);
                        }
                    }
                    if (t.getStatut() == StatutTableau.LANCE && !unite.isEmpty()) {
                        // Phase de poules : la poule entière est l'unité de lancement.
                        PouleOrdonnancable po = new PouleOrdonnancable(p, t.getNom(), t.getNom(), unite,
                                absents(t, p));
                        parPoule.put(p, po);
                        poules.add(po);
                        ordonnanceur.ajouterPoule(po);
                    }
                }
                if (t.aPhaseFinale()) {
                    List<Tour> toursPf = t.getPhaseFinale().getTours();
                    for (int k = 0; k < toursPf.size(); k++) {
                        Tour tour = toursPf.get(k);
                        PhaseMatch phase = k == 0 ? PhaseMatch.PREMIER_TOUR_TABLEAU : PhaseMatch.TOUR_SUIVANT_TABLEAU;
                        // Demi-finales, finale, petite finale : tours d'au plus deux parties réelles.
                        long reels = tour.getMatchs().stream().filter(m -> !m.estBye()).count();
                        String importance = reels <= 2 ? "match important : " + tour.getNom() : null;
                        for (Match m : tour.getMatchs()) {
                            ajouter(m, t, tour.getNom(), phase, tour.getNom(), tour.getMatchs(), importance);
                        }
                    }
                }
            }
        }

        /**
         * Joueurs de la poule non présents : pas pointés dans un tableau dont le
         * pointage est clôturé (sans pointage clôturé, on ne sait pas : personne).
         */
        private java.util.Set<String> absents(Tableau t, Poule p) {
            java.util.Set<String> absents = new java.util.HashSet<>();
            if (t.pointageEstCloture()) {
                for (Joueur j : p.getJoueurs()) {
                    if (connu(j) && !t.aPointe(j)) {
                        absents.add(j.getNumeroLicence());
                    }
                }
            }
            return absents;
        }

        private void ajouter(Match m, Tableau t, String tour, PhaseMatch phase, String groupe,
                             List<Match> groupeMatchs, String importance) {
            if (m.estBye() || parMatch.containsKey(m)) {
                return;
            }
            MatchOrdonnancable mo = new MatchOrdonnancable(m, t.getNom(), t.getNom(), phase);
            parMatch.put(m, mo);
            tours.put(mo, tour);
            tableaux.put(mo, t);
            groupes.put(mo, groupe);
            matchsDuGroupe.put(mo, groupeMatchs);
            if (importance != null) {
                importanceTour.put(mo, importance);
            }
            ordre.add(mo);
            // Numéro dans le tableau : poules (ordre I.301) puis tours du tableau final, exempts exclus.
            numeros.put(mo, compteurs.merge(t, 1, Integer::sum));
            ordonnanceur.ajouterMatch(mo);
        }

        /**
         * Les matchs en cours occupent leur table. Le plus ancien garde la table en
         * cas de conflit ; les autres (table absente ou hors limites, table déjà
         * prise, joueur déjà sur une autre table) deviennent des anomalies.
         */
        private void restaurerOccupation() {
            // Poules lancées en bloc d'abord (la plus ancienne garde sa table en cas de conflit).
            List<PouleOrdonnancable> poulesEnCours = new ArrayList<>();
            for (PouleOrdonnancable p : poules) {
                if (p.estEnCours()) {
                    poulesEnCours.add(p);
                }
            }
            poulesEnCours.sort(Comparator.comparing(p -> p.getPoule().getInstantLancement()));
            java.util.Set<MatchOrdonnancable> dansUnePoule = java.util.Collections.newSetFromMap(
                    new IdentityHashMap<>());
            for (PouleOrdonnancable p : poulesEnCours) {
                dansUnePoule.addAll(p.getMatchs());
                int table = p.getPoule().getNumeroTable();
                boolean valable = table >= 1 && table <= nombreDeTables && ordonnanceur.matchSurTable(table) == null
                        && ordonnanceur.pouleSurTable(table) == null
                        && p.joueurs().stream().allMatch(j -> ordonnanceur.tableDuJoueur(licence(j)) == null);
                if (valable) {
                    ordonnanceur.restaurerPoule(table, p);
                } else {
                    ordonnanceur.restaurerPouleHorsTable(p);
                    MatchOrdonnancable enJeu = p.matchEnJeu();
                    if (enJeu != null) {
                        anomalies.add(enJeu);
                    }
                }
            }
            List<MatchOrdonnancable> enCours = new ArrayList<>();
            for (MatchOrdonnancable mo : ordre) {
                // Matchs lancés seuls : tableau final, ou match de poule d'un ancien fichier.
                if (mo.getMatch().getStatut() == StatutMatch.EN_COURS && !dansUnePoule.contains(mo)) {
                    enCours.add(mo);
                }
            }
            enCours.sort(Comparator.comparing((MatchOrdonnancable mo) -> mo.getMatch().getInstantLancement(),
                    Comparator.nullsLast(Comparator.naturalOrder())));
            for (MatchOrdonnancable mo : enCours) {
                Match m = mo.getMatch();
                Integer table = m.getNumeroTable();
                boolean valable = table != null && table >= 1 && table <= nombreDeTables
                        && ordonnanceur.matchSurTable(table) == null && ordonnanceur.pouleSurTable(table) == null
                        && ordonnanceur.tableDuJoueur(licence(m.getJoueurA())) == null
                        && ordonnanceur.tableDuJoueur(licence(m.getJoueurB())) == null;
                if (valable) {
                    ordonnanceur.restaurerOccupation(table, mo);
                } else {
                    // Ses joueurs jouent quand même : ils restent indisponibles partout.
                    ordonnanceur.restaurerEnCoursHorsTable(mo);
                    anomalies.add(mo);
                }
            }
        }

        /**
         * Instant depuis lequel chaque match à jouer attend : le moment où ses deux
         * joueurs sont libres (fin de leur dernier match), sinon le début du
         * tableau dans la salle (premier match lancé), sans dépasser maintenant.
         */
        private void poserAttentes() {
            for (MatchOrdonnancable mo : ordre) {
                Match m = mo.getMatch();
                if (m.getInstantLancement() != null) {
                    debutTableau.merge(mo.getIdentifiantTableau(), m.getInstantLancement(),
                            (a, b) -> a.isBefore(b) ? a : b);
                }
                if (m.getStatut() == StatutMatch.TERMINE && m.getInstantFin() != null) {
                    for (Joueur j : List.of(m.getJoueurA(), m.getJoueurB())) {
                        if (j != null) {
                            libreDepuis.merge(j.getNumeroLicence(), m.getInstantFin(),
                                    (a, b) -> a.isAfter(b) ? a : b);
                        }
                    }
                }
            }
            for (MatchOrdonnancable mo : ordre) {
                Match m = mo.getMatch();
                if (m.getStatut() != StatutMatch.A_JOUER || !connu(m.getJoueurA()) || !connu(m.getJoueurB())) {
                    continue;
                }
                LocalDateTime debut = debutTableau.get(mo.getIdentifiantTableau());
                LocalDateTime pret = null;
                for (Joueur j : List.of(m.getJoueurA(), m.getJoueurB())) {
                    LocalDateTime libre = libreDepuis.getOrDefault(j.getNumeroLicence(), debut);
                    if (libre != null && (pret == null || libre.isAfter(pret))) {
                        pret = libre;
                    }
                }
                if (pret != null && pret.isAfter(maintenant)) {
                    pret = maintenant;
                }
                mo.setPretDepuis(pret);
            }
            for (PouleOrdonnancable p : poules) {
                if (p.estEnCours() || p.aJouer().isEmpty()) {
                    continue;
                }
                // Attente d'une poule : depuis que tous ses joueurs sont libres.
                LocalDateTime debut = debutTableau.get(p.getIdentifiantTableau());
                LocalDateTime pret = null;
                for (Joueur j : p.joueurs()) {
                    LocalDateTime libre = j == null ? null : libreDepuis.getOrDefault(j.getNumeroLicence(), debut);
                    if (libre != null && (pret == null || libre.isAfter(pret))) {
                        pret = libre;
                    }
                }
                p.setPretDepuis(pret != null && pret.isAfter(maintenant) ? maintenant : pret);
            }
            ordonnanceur.rafraichirDisponibilites(maintenant);
        }

        /** Vérifications d'un lancement sur une table, sans rien modifier. */
        ResultatAffectationTable verifier(Match match, int table) {
            if (tournoi.getStatut() == StatutTournoi.ANNULE) {
                return ResultatAffectationTable.de(ResultatAffectationTable.Motif.TOURNOI_FERME, table);
            }
            if (table < 1 || table > nombreDeTables) {
                return ResultatAffectationTable.de(ResultatAffectationTable.Motif.TABLE_HORS_LIMITES, table);
            }
            MatchOrdonnancable mo = parMatch.get(match);
            if (mo == null) {
                return ResultatAffectationTable.de(ResultatAffectationTable.Motif.MATCH_INCONNU, table);
            }
            return ordonnanceur.verifierAffectation(mo, table);
        }

        /** Liste de l'interface « Lancer un match » (voir {@link ListeLancements}). */
        ListeLancements lister() {
            boolean ferme = tournoi.getStatut() == StatutTournoi.ANNULE;
            TableauDeBordSalle bord = photographier();
            List<Integer> libres = nombreDeTables == 0 ? List.of() : ordonnanceur.tablesLibres();
            // Tables que la stratégie en place donnerait maintenant (bouton « Proposer »).
            Map<Match, Integer> tableStrategie = new IdentityHashMap<>();
            List<PropositionLancement> propositionsPoules = new ArrayList<>();
            if (!ferme && !libres.isEmpty()) {
                for (PropositionLancement p : ordonnanceur.proposerLancements(maintenant)) {
                    if (p.estPoule()) {
                        propositionsPoules.add(p);
                    } else {
                        tableStrategie.put(p.getMatch().getMatch(), p.getNumeroTable());
                    }
                }
            }
            List<ListeLancements.Ligne> proposes = new ArrayList<>();
            for (PropositionLancement p : propositionsPoules) {
                tableStrategie.put(p.getMatch().getMatch(), p.getNumeroTable());
            }
            for (TableauDeBordSalle.MatchSalle vu : bord.getPrets()) {
                MatchOrdonnancable mo = parMatch.get(vu.match());
                ResultatAffectationTable refus = null;
                List<Integer> possibles = new ArrayList<>();
                Integer table = null;
                if (ferme) {
                    refus = ResultatAffectationTable.de(ResultatAffectationTable.Motif.TOURNOI_FERME, 0);
                } else if (nombreDeTables == 0) {
                    refus = ResultatAffectationTable.de(ResultatAffectationTable.Motif.TABLE_HORS_LIMITES, 0);
                } else {
                    for (Integer t : libres) {
                        if (ordonnanceur.verifierAffectation(mo, t).estReussi()) {
                            possibles.add(t);
                        }
                    }
                    if (possibles.isEmpty()) {
                        // Aucune table libre, ou seulement des tables réservées à d'autres tableaux.
                        refus = libres.isEmpty()
                                ? ResultatAffectationTable.de(ResultatAffectationTable.Motif.AUCUNE_TABLE_LIBRE, 0)
                                : ordonnanceur.verifierAffectation(mo, libres.get(0));
                    } else {
                        table = tableStrategie.get(vu.match());
                        if (table == null || !possibles.contains(table)) {
                            // Hors de la vague : de préférence une table que la stratégie ne prend pas.
                            table = possibles.stream().filter(t -> !tableStrategie.containsValue(t))
                                    .findFirst().orElse(possibles.get(0));
                        }
                    }
                }
                proposes.add(new ListeLancements.Ligne(vu, tableStrategie.containsKey(vu.match()), table, possibles,
                        mo.getPretDepuis(), refus, numeros.getOrDefault(mo, 0), groupes.get(mo)));
            }
            for (TableauDeBordSalle.MatchBloque b : bord.getBloques()) {
                MatchOrdonnancable mo = parMatch.get(b.match().match());
                proposes.add(new ListeLancements.Ligne(b.match(), false, null, List.of(), null, b.motif(),
                        numeros.getOrDefault(mo, 0), groupes.get(mo)));
            }
            Map<Match, Integer> rang = new IdentityHashMap<>();
            for (int i = 0; i < ordre.size(); i++) {
                rang.put(ordre.get(i).getMatch(), i);
            }
            proposes = avecPoules(proposes, ferme, libres, tableStrategie, rang);
            List<ListeLancements.Ligne> parTableau = new ArrayList<>(proposes);
            parTableau.sort(Comparator.comparingInt(l -> rang.getOrDefault(l.match().match(), Integer.MAX_VALUE)));
            return new ListeLancements(maintenant, libres, proposes, parTableau);
        }

        /**
         * Ajoute les poules à lancer (phase de poules) aux lignes des matchs, dans
         * le même ordre de priorité : lignes prêtes d'abord (priorité décroissante,
         * puis ordre du tournoi), puis les lignes bloquées.
         */
        private List<ListeLancements.Ligne> avecPoules(List<ListeLancements.Ligne> lignes, boolean ferme,
                                                       List<Integer> libres, Map<Match, Integer> tableStrategie,
                                                       Map<Match, Integer> rang) {
            if (poules.isEmpty()) {
                return lignes;
            }
            List<ListeLancements.Ligne> toutes = new ArrayList<>(lignes);
            for (PouleOrdonnancable p : poules) {
                if (p.estEnCours() || p.aJouer().isEmpty()) {
                    continue;
                }
                ResultatAffectationTable r = ordonnanceur.verifierPoule(p);
                ResultatAffectationTable.Motif m = r.getMotif();
                if (m == ResultatAffectationTable.Motif.JOUEURS_NON_CONNUS
                        || m == ResultatAffectationTable.Motif.MATCH_TERMINE
                        || m == ResultatAffectationTable.Motif.MATCH_DEJA_EN_COURS) {
                    continue;
                }
                MatchOrdonnancable premier = p.aJouer().get(0);
                ResultatAffectationTable refus = null;
                List<Integer> possibles = new ArrayList<>();
                Integer table = null;
                if (ferme) {
                    refus = ResultatAffectationTable.de(ResultatAffectationTable.Motif.TOURNOI_FERME, 0);
                } else if (nombreDeTables == 0) {
                    refus = ResultatAffectationTable.de(ResultatAffectationTable.Motif.TABLE_HORS_LIMITES, 0);
                } else if (!r.estReussi()) {
                    refus = r;
                } else {
                    for (Integer t : libres) {
                        if (ordonnanceur.verifierAffectationPoule(p, t).estReussi()) {
                            possibles.add(t);
                        }
                    }
                    if (possibles.isEmpty()) {
                        refus = libres.isEmpty()
                                ? ResultatAffectationTable.de(ResultatAffectationTable.Motif.AUCUNE_TABLE_LIBRE, 0)
                                : ordonnanceur.verifierAffectationPoule(p, libres.get(0));
                    } else {
                        table = tableStrategie.get(premier.getMatch());
                        if (table == null || !possibles.contains(table)) {
                            table = possibles.stream().filter(t -> !tableStrategie.containsValue(t))
                                    .findFirst().orElse(possibles.get(0));
                        }
                    }
                }
                double score = ordonnanceur.scorePriorite(p, maintenant);
                TableauDeBordSalle.MatchSalle vu = new TableauDeBordSalle.MatchSalle(premier.getMatch(),
                        p.getNomTableau(), "Poule " + p.getPoule().getNom(), PhaseMatch.POULE, score,
                        ordonnanceur.expliquer(p, maintenant));
                toutes.add(new ListeLancements.Ligne(vu, tableStrategie.containsKey(premier.getMatch()), table,
                        possibles, p.getPretDepuis(), refus, 0, "Poule " + p.getPoule().getNom(), vuePoule(p)));
            }
            toutes.sort(Comparator.comparing((ListeLancements.Ligne l) -> !l.estPret())
                    .thenComparingDouble(l -> -l.match().priorite())
                    .thenComparingInt(l -> rang.getOrDefault(l.match().match(), Integer.MAX_VALUE)));
            return toutes;
        }

        TableauDeBordSalle.MatchSalle vue(MatchOrdonnancable mo, double priorite, String explication) {
            return new TableauDeBordSalle.MatchSalle(mo.getMatch(), mo.getNomTableau(), tours.get(mo),
                    mo.getPhase(), priorite, explication);
        }

        TableauDeBordSalle photographier() {
            List<TableauDeBordSalle.CarteTable> cartes = new ArrayList<>();
            for (int t = 1; t <= nombreDeTables; t++) {
                MatchOrdonnancable mo = ordonnanceur.matchSurTable(t);
                PouleOrdonnancable po = ordonnanceur.pouleSurTable(t);
                if (mo == null) {
                    cartes.add(new TableauDeBordSalle.CarteTable(t, null, null));
                } else if (po != null) {
                    // Poule entière sur la table : chrono depuis son lancement, match en jeu affiché.
                    Duration ecoule = Duration.between(po.getPoule().getInstantLancement(), maintenant);
                    cartes.add(new TableauDeBordSalle.CarteTable(t, vue(mo, 0, ""),
                            ecoule.isNegative() ? Duration.ZERO : ecoule, vuePoule(po)));
                } else {
                    LocalDateTime debut = mo.getMatch().getInstantLancement();
                    Duration ecoule = debut == null ? null : Duration.between(debut, maintenant);
                    if (ecoule != null && ecoule.isNegative()) {
                        ecoule = Duration.ZERO;
                    }
                    cartes.add(new TableauDeBordSalle.CarteTable(t, vue(mo, 0, ""), ecoule));
                }
            }

            List<Classe> prets = new ArrayList<>();
            List<Classe> bloques = new ArrayList<>();
            int enAttente = 0;
            for (int i = 0; i < ordre.size(); i++) {
                MatchOrdonnancable mo = ordre.get(i);
                Match m = mo.getMatch();
                if (m.getStatut() == StatutMatch.TERMINE || m.getStatut() == StatutMatch.EN_COURS) {
                    continue;
                }
                ResultatAffectationTable r = ordonnanceur.verifierMatch(mo);
                if (r.getMotif() == ResultatAffectationTable.Motif.JOUEURS_NON_CONNUS) {
                    enAttente++;
                    continue;
                }
                if (r.getMotif() == ResultatAffectationTable.Motif.LANCEMENT_PAR_POULE) {
                    continue; // part avec sa poule (lignes de poule de l'interface « Lancer un match »)
                }
                double score = ordonnanceur.scorePriorite(mo, maintenant);
                TableauDeBordSalle.MatchSalle vu = vue(mo, score, ordonnanceur.expliquer(mo, maintenant));
                if (r.estReussi()) {
                    prets.add(new Classe(vu, null, i));
                } else {
                    bloques.add(new Classe(vu, r, i));
                }
            }
            // Priorité décroissante ; à égalité, l'ordre du tournoi (tableau, poule, I.301).
            Comparator<Classe> parPriorite = Comparator.comparingDouble((Classe c) -> -c.vu.priorite())
                    .thenComparingInt(c -> c.rang);
            prets.sort(parPriorite);
            bloques.sort(parPriorite);

            // Couleur de priorité : les premiers proposés, autant que de tables libres,
            // sont ceux que la stratégie de l'ordonnanceur enverrait maintenant.
            int libres = nombreDeTables == 0 ? 0 : ordonnanceur.tablesLibres().size();
            List<TableauDeBordSalle.MatchSalle> listePrets = new ArrayList<>();
            for (int rang = 0; rang < prets.size(); rang++) {
                listePrets.add(colorer(prets.get(rang).vu, rang < libres));
            }
            List<TableauDeBordSalle.MatchBloque> listeBloques = new ArrayList<>();
            bloques.forEach(c -> listeBloques.add(new TableauDeBordSalle.MatchBloque(c.vu, c.motif)));
            List<TableauDeBordSalle.MatchSalle> listeAnomalies = new ArrayList<>();
            anomalies.forEach(mo -> listeAnomalies.add(vue(mo, 0, "")));

            // Phase de poules : les poules prêtes (tous leurs joueurs libres), par priorité.
            List<PouleOrdonnancable> pretes = new ArrayList<>();
            for (PouleOrdonnancable p : poules) {
                if (!p.estEnCours() && !p.aJouer().isEmpty() && ordonnanceur.verifierPoule(p).estReussi()) {
                    pretes.add(p);
                }
            }
            pretes.sort(Comparator.comparingDouble((PouleOrdonnancable p) -> -ordonnanceur.scorePriorite(p, maintenant)));
            List<PouleALancer> poulesPretes = new ArrayList<>();
            pretes.forEach(p -> poulesPretes.add(vuePoule(p)));
            return new TableauDeBordSalle(maintenant, nombreDeTables, cartes, listePrets, listeBloques,
                    listeAnomalies, enAttente, joueursEnAttente(), ordonnanceur).avecPoulesPretes(poulesPretes);
        }

        /** Attente d'un match prêt (depuis que ses deux joueurs sont libres), ou null. */
        private Duration attente(MatchOrdonnancable mo) {
            LocalDateTime pret = mo.getPretDepuis();
            if (pret == null) {
                return null;
            }
            Duration d = Duration.between(pret, maintenant);
            return d.isNegative() ? Duration.ZERO : d;
        }

        /** Pose la couleur de priorité d'un match prêt ({@link ReglePrioriteSalle}). */
        private TableauDeBordSalle.MatchSalle colorer(TableauDeBordSalle.MatchSalle vu, boolean choisiParStrategie) {
            MatchOrdonnancable mo = parMatch.get(vu.match());
            Duration attente = attente(mo);
            ReglePrioriteSalle.Evaluation e = ReglePrioriteSalle.evaluer(attente, importance(mo), choisiParStrategie);
            return new TableauDeBordSalle.MatchSalle(vu.match(), vu.tableau(), vu.tour(), vu.phase(),
                    vu.priorite(), vu.explication(), e.niveau(), e.motif(), attente);
        }

        /**
         * Importance d'un match : tour final (demi-finales, finale, petite finale),
         * ou joueur attendu dans un autre tableau (le lancer libère la suite).
         */
        private String importance(MatchOrdonnancable mo) {
            List<String> raisons = new ArrayList<>();
            String tour = importanceTour.get(mo);
            if (tour != null) {
                raisons.add(tour);
            }
            Match m = mo.getMatch();
            for (Joueur j : List.of(m.getJoueurA(), m.getJoueurB())) {
                String autre = autreTableauAJouer(j, mo.getIdentifiantTableau());
                if (autre != null) {
                    raisons.add(j.getNomComplet() + " joue aussi " + autre);
                }
            }
            return String.join(", ", raisons);
        }

        /** Nom d'un autre tableau où le joueur a encore un match à jouer, ou null. */
        private String autreTableauAJouer(Joueur j, String tableau) {
            if (!connu(j)) {
                return null;
            }
            for (MatchOrdonnancable autre : ordre) {
                Match m = autre.getMatch();
                if (!autre.getIdentifiantTableau().equals(tableau) && m.getStatut() == StatutMatch.A_JOUER
                        && (memeJoueur(j, m.getJoueurA()) || memeJoueur(j, m.getJoueurB()))) {
                    return autre.getNomTableau();
                }
            }
            return null;
        }

        /**
         * Joueurs qui ont un match à jouer et ne sont sur aucune table : chrono
         * depuis la fin de leur dernier match (sinon début de leur tableau dans la
         * salle, sinon heure de début prévue du tableau si elle est passée), et
         * pourquoi chacun de leurs matchs ne se joue pas.
         */
        private List<TableauDeBordSalle.JoueurEnAttente> joueursEnAttente() {
            Map<String, Joueur> joueurs = new java.util.LinkedHashMap<>();
            Map<String, List<TableauDeBordSalle.Engagement>> engagements = new HashMap<>();
            Map<String, LocalDateTime> depuis = new HashMap<>();
            java.util.Set<String> enMatch = new java.util.HashSet<>();
            for (MatchOrdonnancable mo : ordre) {
                Match m = mo.getMatch();
                if (m.getStatut() == StatutMatch.EN_COURS) {
                    enMatch.add(licence(m.getJoueurA()));
                    enMatch.add(licence(m.getJoueurB()));
                }
            }
            for (PouleOrdonnancable p : poules) {
                if (p.estEnCours()) {
                    p.joueurs().forEach(j -> enMatch.add(licence(j)));
                }
            }
            for (MatchOrdonnancable mo : ordre) {
                Match m = mo.getMatch();
                if (m.getStatut() != StatutMatch.A_JOUER) {
                    continue;
                }
                for (Joueur j : List.of(m.getJoueurA(), m.getJoueurB())) {
                    if (!connu(j) || enMatch.contains(j.getNumeroLicence())) {
                        continue;
                    }
                    Joueur adversaire = memeJoueur(j, m.getJoueurA()) ? m.getJoueurB() : m.getJoueurA();
                    TableauDeBordSalle.RaisonAttente raison;
                    Integer tableAdversaire = null;
                    PouleOrdonnancable unite = ordonnanceur.pouleDe(mo);
                    Joueur occupeDeLaPoule = unite == null ? null : joueurOccupe(unite, j, enMatch);
                    if (!connu(adversaire)) {
                        raison = TableauDeBordSalle.RaisonAttente.ADVERSAIRE_INCONNU;
                    } else if (occupeDeLaPoule != null) {
                        // La poule part en bloc : elle attend tant qu'un de ses joueurs joue ailleurs.
                        adversaire = occupeDeLaPoule;
                        tableAdversaire = ordonnanceur.tableDuJoueur(adversaire.getNumeroLicence());
                        raison = TableauDeBordSalle.RaisonAttente.ADVERSAIRE_EN_JEU;
                    } else {
                        tableAdversaire = ordonnanceur.tableDuJoueur(adversaire.getNumeroLicence());
                        raison = tableAdversaire != null || enMatch.contains(adversaire.getNumeroLicence())
                                ? TableauDeBordSalle.RaisonAttente.ADVERSAIRE_EN_JEU
                                : TableauDeBordSalle.RaisonAttente.PRET;
                    }
                    String cle = j.getNumeroLicence();
                    joueurs.putIfAbsent(cle, j);
                    engagements.computeIfAbsent(cle, k -> new ArrayList<>()).add(new TableauDeBordSalle.Engagement(
                            vue(mo, 0, ""), raison, adversaire, tableAdversaire, groupes.get(mo),
                            matchsDuGroupe.getOrDefault(mo, List.of())));
                    LocalDateTime debut = debutAttente(j, mo);
                    if (debut != null) {
                        depuis.merge(cle, debut, (a, b) -> a.isBefore(b) ? a : b);
                    }
                }
            }
            List<TableauDeBordSalle.JoueurEnAttente> res = new ArrayList<>();
            for (Map.Entry<String, Joueur> e : joueurs.entrySet()) {
                LocalDateTime d = depuis.get(e.getKey());
                Duration attente = d == null ? null : Duration.between(d, maintenant);
                res.add(new TableauDeBordSalle.JoueurEnAttente(e.getValue(), d, attente,
                        engagements.get(e.getKey())));
            }
            res.sort(Comparator.comparing(TableauDeBordSalle.JoueurEnAttente::attente,
                            Comparator.nullsLast(Comparator.<Duration>reverseOrder()))
                    .thenComparing(x -> x.joueur().getNom())
                    .thenComparing(x -> x.joueur().getPrenom()));
            return res;
        }

        /** Premier autre joueur de la poule qui joue ailleurs (la poule ne peut pas partir), ou null. */
        private Joueur joueurOccupe(PouleOrdonnancable p, Joueur sauf, java.util.Set<String> enMatch) {
            for (Joueur x : p.joueurs()) {
                if (connu(x) && !memeJoueur(x, sauf) && (enMatch.contains(x.getNumeroLicence())
                        || ordonnanceur.tableDuJoueur(x.getNumeroLicence()) != null)) {
                    return x;
                }
            }
            return null;
        }

        /** Début de l'attente d'un joueur pour un match, borné à maintenant, ou null. */
        private LocalDateTime debutAttente(Joueur j, MatchOrdonnancable mo) {
            LocalDateTime d = libreDepuis.get(j.getNumeroLicence());
            if (d == null) {
                d = debutTableau.get(mo.getIdentifiantTableau());
            }
            if (d == null) {
                Tableau t = tableaux.get(mo);
                LocalDateTime prevu = t == null ? null : t.getHoraires().getDebutMatchs();
                d = prevu != null && !prevu.isAfter(maintenant) ? prevu : null;
            }
            return d != null && d.isAfter(maintenant) ? maintenant : d;
        }
    }

    /** Match classé : sa vue, son motif de blocage éventuel et son rang dans le tournoi. */
    private record Classe(TableauDeBordSalle.MatchSalle vu, ResultatAffectationTable motif, int rang) {
    }

    private static String licence(Joueur j) {
        return j == null ? null : j.getNumeroLicence();
    }

    private static boolean memeJoueur(Joueur j, Joueur autre) {
        return j != null && autre != null && j.getNumeroLicence().equals(autre.getNumeroLicence());
    }

    private static boolean connu(Joueur j) {
        return j != null && !j.estADeterminer();
    }
}
