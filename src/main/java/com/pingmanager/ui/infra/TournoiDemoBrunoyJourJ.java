package com.pingmanager.ui.infra;

import com.pingmanager.core.JoueurDepuisLicence;
import com.pingmanager.core.SaisieResultatMatch;
import com.pingmanager.core.ServiceInscription;
import com.pingmanager.core.ServiceLancementTableau;
import com.pingmanager.core.ServicePhaseFinale;
import com.pingmanager.core.ServicePointage;
import com.pingmanager.core.ServiceSalle;
import com.pingmanager.model.AffectationJugeArbitre;
import com.pingmanager.model.Coordonnees;
import com.pingmanager.model.GradeJugeArbitre;
import com.pingmanager.model.Paiement;
import com.pingmanager.model.RoleJugeArbitre;
import com.pingmanager.model.FicheLicence;
import com.pingmanager.model.IssueMatch;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Manche;
import com.pingmanager.model.Match;
import com.pingmanager.model.Poule;
import com.pingmanager.model.StatutMatch;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tour;
import com.pingmanager.model.Tournoi;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Copie du National B de Brunoy ({@link TournoiDemoBrunoy}) mise au <b>jour J</b> : mêmes
 * tableaux, horaires, règles et dotations, mais presque tous les tableaux remplis et le
 * samedi en cours, pour essayer chaque étape du juge-arbitre sur un vrai volume de joueurs.
 *
 * <p>Les tableaux du samedi, dans l'ordre de leurs horaires :</p>
 * <ol>
 *   <li>tableau final tiré, premier tour joué, tour suivant entamé, un match en cours ;</li>
 *   <li>poules toutes jouées : prêt pour le tirage du tableau final ;</li>
 *   <li>lancé, poules en cours (un score, un forfait), trois parties dans la salle ;</li>
 *   <li>pointage clôturé, tout le monde présent : prêt pour le tirage des poules ;</li>
 *   <li>pointage ouvert : une partie des joueurs pointés et payés ;</li>
 *   <li>les suivants, et tous ceux du dimanche, encore en inscription.</li>
 * </ol>
 *
 * <p>Le vrai National B (en brouillon) n'est pas modifié. Joueurs, clubs, juges-arbitres,
 * correspondant, lien de paiement et n° d'homologation sont <b>fictifs</b> ; toutes les
 * étapes passent par les services du cœur.</p>
 */
public final class TournoiDemoBrunoyJourJ {

    public static final String NOM = "Tournoi National B de Brunoy – jour J (copie de démo)";

    /** Graines des tirages, pour retrouver les mêmes poules et le même tableau. */
    public static final long GRAINE_POULES = 101027L;
    public static final long GRAINE_TABLEAU_FINAL = 101028L;

    /** Table du match en cours du tableau final (1er tableau du samedi). */
    public static final int TABLE_TABLEAU_FINAL = 1;

    /** Tables des parties en cours du tableau lancé (3e tableau du samedi). */
    public static final List<Integer> TABLES_POULES = List.of(4, 7, 9);

    private TournoiDemoBrunoyJourJ() {
    }

    public static Tournoi construire(List<FicheLicence> joueurs, FicheLicence quentin) {
        return construire(joueurs, quentin, Instant.now());
    }

    /**
     * @param joueurs    licenciés fictifs (la réserve de la démo)
     * @param quentin    licence liée au compte de démo, inscrite aux 2e et 4e tableaux du samedi
     *                   si ses points le permettent (« Mes tournois », « Ma prochaine partie ») ;
     *                   {@code null} accepté
     * @param maintenant heure de création : les parties en cours ont commencé peu avant
     */
    public static Tournoi construire(List<FicheLicence> joueurs, FicheLicence quentin, Instant maintenant) {
        Tournoi t = TournoiDemoBrunoy.construire();
        t.setNom(NOM);
        t.setNumeroHomologation(TournoisDemo.HOMOLOGATION_BRUNOY_DEMO);
        t.setAdresse(t.getAdresse().avecCoordonnees(new Coordonnees(48.6929, 2.5115)));
        personnesFictives(t);
        TournoisDemo.publier(t);

        List<Tableau> samedi = tableauxDuJour(t, t.getJours().first());
        if (quentin != null && samedi.size() > 3) {
            // Poules jouées (2e tableau) et prêt pour le tirage (4e) : il aura des parties à jouer.
            Joueur q = JoueurDepuisLicence.creer(quentin, t);
            ServiceInscription inscription = ServiceInscription.sansContrainteSuccession();
            inscription.inscrire(t, q, samedi.get(1));
            inscription.inscrire(t, q, samedi.get(3));
        }
        TournoisDemo.remplir(t, joueurs, cibles(t), 1011L, null);

        ServicePointage pointage = new ServicePointage(t);
        if (samedi.size() > 0) {
            tableauFinalEnCours(t, samedi.get(0), pointage, maintenant);
        }
        if (samedi.size() > 1) {
            lancerEtJouerPoules(t, samedi.get(1), pointage, GRAINE_POULES + 1, Instant.parse("2027-04-10T08:00:00Z"));
        }
        if (samedi.size() > 2) {
            poulesEnCours(t, samedi.get(2), pointage, maintenant);
        }
        if (samedi.size() > 3) {
            pointageCloture(samedi.get(3), pointage);
        }
        if (samedi.size() > 4) {
            pointageOuvert(samedi.get(4), pointage);
        }
        return t;
    }

    /** Juge-arbitre principal fictif de la copie (grade minimum du national B, IX.104). */
    public static final AffectationJugeArbitre JA_FICTIF = new AffectationJugeArbitre("ARBITRE", "Jean",
            null, "Licence fictive (JA démo)", GradeJugeArbitre.JA3, null, RoleJugeArbitre.PRINCIPAL);
    /** Juge-arbitre adjoint fictif de la copie. */
    public static final AffectationJugeArbitre ADJOINT_FICTIF = new AffectationJugeArbitre("ADJOINT", "Julie",
            null, "Licence fictive (adjoint démo)", GradeJugeArbitre.JA2, null, RoleJugeArbitre.ADJOINT);

    /**
     * Remplace les vraies personnes du National B (juges-arbitres, correspondant) par des
     * personnes fictives, et le lien de paiement par un exemple : la copie de démo ne
     * montre ni ne contacte personne de réel.
     */
    static void personnesFictives(Tournoi t) {
        for (AffectationJugeArbitre ja : List.copyOf(t.getJugesArbitres())) {
            t.retirerJugeArbitre(ja.getNumeroLicence());
        }
        t.ajouterJugeArbitre(JA_FICTIF);
        t.ajouterJugeArbitre(ADJOINT_FICTIF);
        t.setCorrespondant("ORGANISATEUR", "Démo", "tournoi-demo@pingmanager.invalid", null, null);
        t.setPaiement(new Paiement(Paiement.Mode.HELLOASSO,
                "https://www.helloasso.com/associations/exemple-demo/evenements/tournoi"));
    }

    /**
     * Cibles de remplissage (inscrits + attente) : tous les tableaux complets ou presque ; le
     * dames et quelques tableaux avec une liste d'attente. La réserve de joueurs fictifs limite
     * les tableaux les plus exigeants (points bas, numérotés exclus).
     */
    static Map<String, Integer> cibles(Tournoi t) {
        Map<String, Integer> m = new LinkedHashMap<>();
        int i = 0;
        for (Tableau tab : t.getTableaux()) {
            int places = tab.getConfiguration().getPlacesMax();
            int cible = (i % 3 == 0) ? places + 3 : places - (i % 3);
            m.put(lettre(tab.getNom()), cible);
            i++;
        }
        return m;
    }

    /** Tableaux d'un jour, dans l'ordre de leurs horaires. */
    static List<Tableau> tableauxDuJour(Tournoi t, LocalDate jour) {
        List<Tableau> res = new ArrayList<>();
        for (Tableau tab : t.getTableaux()) {
            if (tab.getHoraires() != null && tab.getHoraires().getDebutMatchs().toLocalDate().equals(jour)) {
                res.add(tab);
            }
        }
        res.sort(Comparator.comparing((Tableau x) -> x.getHoraires().getDebutMatchs()));
        return res;
    }

    /** Pointage ouvert : deux joueurs sur trois pointés, un sur deux payé. */
    private static void pointageOuvert(Tableau tab, ServicePointage pointage) {
        tab.ouvrirPointage();
        List<Joueur> inscrits = List.copyOf(tab.getInscrits());
        for (int i = 0; i < inscrits.size(); i++) {
            if (i % 3 != 2) {
                pointage.pointer(tab, inscrits.get(i));
            }
            if (i % 2 == 0) {
                pointage.marquerPaye(tab, inscrits.get(i));
            }
        }
    }

    /** Tout le monde pointé et payé, pointage clôturé : le tirage des poules peut être lancé. */
    private static void pointageCloture(Tableau tab, ServicePointage pointage) {
        tab.ouvrirPointage();
        for (Joueur j : List.copyOf(tab.getInscrits())) {
            pointage.pointer(tab, j);
            pointage.marquerPaye(tab, j);
        }
        pointage.cloturerPointage(tab);
    }

    /**
     * Lancé : les premières poules terminées, une poule avec un score et un forfait, puis
     * trois parties envoyées en salle par le service de la salle.
     */
    private static void poulesEnCours(Tournoi t, Tableau tab, ServicePointage pointage, Instant maintenant) {
        pointageCloture(tab, pointage);
        new ServiceLancementTableau(t).lancer(tab, GRAINE_POULES);
        SaisieResultatMatch saisie = new SaisieResultatMatch(() -> Instant.parse("2027-04-10T09:20:00Z"));
        List<Poule> poules = tab.getPoules();
        int terminees = poules.size() / 3;
        int n = 0;
        for (int p = 0; p < terminees; p++) {
            for (Match m : poules.get(p).getMatchs()) {
                saisie.enregistrerScore(tab, m, scoreVictoire(n++ % 4 != 3));
            }
        }
        if (poules.size() > terminees) {
            Poule entamee = poules.get(terminees);
            saisie.enregistrerScore(tab, entamee.getMatchs().get(0), manches(11, 9, 9, 11, 11, 6, 12, 10));
            if (entamee.getMatchs().size() > 1) {
                Match forfait = entamee.getMatchs().get(1);
                saisie.enregistrerIssue(tab, forfait, IssueMatch.FORFAIT, forfait.getJoueurB());
            }
        }
        // Trois parties en salle, dans les poules suivantes (premier match de chacune) ; une
        // partie dont un joueur est déjà à une table est refusée : on passe à la poule suivante.
        int minutes = 18;
        int k = 0;
        for (int p = terminees + 1; p < poules.size() && k < TABLES_POULES.size(); p++) {
            Match m = poules.get(p).getMatchs().get(0);
            long depuis = minutes * 60L;
            if (new ServiceSalle(() -> maintenant.minusSeconds(depuis)).affecter(t, m, TABLES_POULES.get(k))
                    .estReussi()) {
                k++;
                minutes -= 6;
            }
        }
    }

    /**
     * Poules jouées, tableau final tiré, premier tour joué, trois matchs du tour suivant
     * joués et un en cours sur une table ; la suite reste à jouer.
     */
    private static void tableauFinalEnCours(Tournoi t, Tableau tab, ServicePointage pointage, Instant maintenant) {
        lancerEtJouerPoules(t, tab, pointage, GRAINE_POULES - 1, Instant.parse("2027-04-10T07:00:00Z"));
        ServicePhaseFinale phaseFinale = new ServicePhaseFinale();
        phaseFinale.construire(tab, GRAINE_TABLEAU_FINAL);
        List<Tour> tours = ServicePhaseFinale.toursPrincipaux(tab.getPhaseFinale());
        int n = 0;
        for (Match m : aJouer(tours.get(0))) {
            phaseFinale.saisirScore(tab, m, scoreVictoire(n++ % 3 != 1));
        }
        if (tours.size() > 2) {
            List<Match> suivants = aJouer(tours.get(1));
            for (int i = 0; i < suivants.size() && i < 4; i++) {
                Match m = suivants.get(i);
                if (i < 3) {
                    phaseFinale.saisirScore(tab, m, scoreVictoire(i != 1));
                } else {
                    m.setStatut(StatutMatch.EN_COURS);
                    m.setNumeroTable(TABLE_TABLEAU_FINAL);
                    m.setInstantLancement(LocalDateTime.ofInstant(maintenant.minusSeconds(10 * 60L),
                            java.time.ZoneId.systemDefault()));
                }
            }
        }
    }

    /** Matchs jouables d'un tour : à jouer, avec deux joueurs connus. */
    private static List<Match> aJouer(Tour tour) {
        List<Match> res = new ArrayList<>();
        for (Match m : tour.getMatchs()) {
            if (m.getStatut() == StatutMatch.A_JOUER && m.getJoueurA() != null && m.getJoueurB() != null
                    && !m.getJoueurA().estADeterminer() && !m.getJoueurB().estADeterminer()) {
                res.add(m);
            }
        }
        return res;
    }

    /** Tout le monde présent, pointage clôturé, tirage des poules, puis toutes les poules jouées. */
    private static void lancerEtJouerPoules(Tournoi t, Tableau tab, ServicePointage pointage, long graine,
                                            Instant heure) {
        pointageCloture(tab, pointage);
        new ServiceLancementTableau(t).lancer(tab, graine);
        SaisieResultatMatch saisie = new SaisieResultatMatch(() -> heure);
        int n = 0;
        for (Poule p : tab.getPoules()) {
            for (Match m : p.getMatchs()) {
                saisie.enregistrerScore(tab, m, scoreVictoire(n++ % 4 != 3));
            }
        }
    }

    /** Score conforme (2.11.1) en 3 manches gagnantes : le joueur A gagne 3-1, ou le B 3-1. */
    private static List<Manche> scoreVictoire(boolean pourA) {
        return pourA ? manches(11, 7, 9, 11, 11, 8, 11, 6) : manches(7, 11, 11, 9, 8, 11, 6, 11);
    }

    private static String lettre(String nom) {
        int i = nom.indexOf(" – ");
        return i > 0 ? nom.substring(0, i) : nom;
    }

    private static List<Manche> manches(int... points) {
        Manche[] res = new Manche[points.length / 2];
        for (int i = 0; i < res.length; i++) {
            res[i] = new Manche(points[2 * i], points[2 * i + 1]);
        }
        return List.of(res);
    }
}
