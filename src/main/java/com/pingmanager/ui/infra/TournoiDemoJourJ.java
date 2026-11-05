package com.pingmanager.ui.infra;

import com.pingmanager.core.SaisieResultatMatch;
import com.pingmanager.core.ServiceLancementTableau;
import com.pingmanager.core.ServicePointage;
import com.pingmanager.core.ServiceSalle;
import com.pingmanager.model.AdresseTournoi;
import com.pingmanager.model.Coordonnees;
import com.pingmanager.model.FicheLicence;
import com.pingmanager.model.IssueMatch;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Manche;
import com.pingmanager.model.Match;
import com.pingmanager.model.NiveauTournoi;
import com.pingmanager.model.Poule;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Tournoi de démonstration du <b>jour J</b>, organisé par le club de Brunoy (donc
 * visible dans « Gestion des tournois » pour les comptes de Brunoy), le samedi
 * 14 novembre 2026. Ses quatre tableaux montrent chaque étape du juge-arbitre :
 *
 * <ul>
 *   <li><b>A – ≤ 899 pts</b> : encore en inscription (on peut y ouvrir le pointage) ;</li>
 *   <li><b>B – ≤ 1299 pts</b> : pointage ouvert, complet avec une liste d'attente,
 *       une partie des joueurs pointés et payés (clôture et repêchage à essayer) ;</li>
 *   <li><b>C – Toutes séries</b> : lancé (graine 141126), quatre poules de trois, la
 *       poule A terminée, la poule B avec un score et un forfait ; dans la salle, la
 *       dernière partie de la poule B se joue à la table 2 (depuis 15 min) et la
 *       première de la poule C à la table 5 (depuis 6 min) : la poule D est prête,
 *       le reste de la poule C est bloqué (joueur déjà en jeu) ;</li>
 *   <li><b>D – Dames</b> : pointage clôturé, toutes présentes : prêt pour le tirage.</li>
 * </ul>
 *
 * <p>Les données ne touchent pas au National B de Brunoy ({@link TournoiDemoBrunoy}).
 * Joueurs, clubs et n° d'homologation sont fictifs. Toutes les étapes passent par
 * les services du cœur (inscription, pointage, lancement, saisie des scores).</p>
 */
public final class TournoiDemoJourJ {

    public static final String NOM = "Tournoi de démonstration du jour J";

    /** Graine du tirage des poules du tableau C (affichée à l'écran). */
    public static final long GRAINE_TABLEAU_C = 141126L;

    private TournoiDemoJourJ() {
    }

    public static Tournoi construire(List<FicheLicence> joueurs) {
        return construire(joueurs, Instant.now());
    }

    /**
     * @param maintenant heure de création de la démo : les matchs en cours dans la
     *                   salle ont commencé quelques minutes avant
     */
    public static Tournoi construire(List<FicheLicence> joueurs, Instant maintenant) {
        LocalDate jour = LocalDate.of(2026, 11, 14);
        Tournoi t = TournoisDemo.base(NOM, "Brunoy", DonneesDemo.CLUB_BRUNOY, NiveauTournoi.DEPARTEMENTAL,
                List.of(jour), AdresseTournoi.de("157 route de Brie", "91800", "Brunoy"),
                new Coordonnees(48.6929, 2.5115), "D-91-2026-JJ");
        TournoisDemo.ajouter(t, "A – ≤ 899 pts", jour, 9, 0, 24, TournoisDemo.b().pointMax(899), 7,
                TournoisDemo.gr(40, 20, 10));
        TournoisDemo.ajouter(t, "B – ≤ 1299 pts", jour, 10, 30, 12, TournoisDemo.b().pointMax(1299), 8,
                TournoisDemo.gr(60, 30, 15));
        TournoisDemo.ajouter(t, "C – Toutes séries", jour, 9, 30, 12, TournoisDemo.b().toutesSeries(), 10,
                TournoisDemo.gr(120, 60, 30));
        TournoisDemo.ajouter(t, "D – Dames", jour, 13, 0, 12,
                TournoisDemo.b().toutesSeries().sexeRequis(Sexe.FEMME), 7, TournoisDemo.gr(50, 25, 10));
        TournoisDemo.finaliser(t, LocalDateTime.of(2026, 10, 1, 0, 0), LocalDateTime.of(2026, 11, 12, 20, 0));
        TournoisDemo.remplir(t, joueurs, Map.of("A", 17, "B", 15, "C", 12, "D", 9), 29L, null);

        ServicePointage pointage = new ServicePointage(t);
        pointageOuvertB(t, tableau(t, "B"), pointage);
        lanceC(t, tableau(t, "C"), pointage);
        pretD(t, tableau(t, "D"), pointage);
        salleC(t, tableau(t, "C"), maintenant);
        return t;
    }

    /** C : deux parties en cours dans la salle (tables 2 et 5), envoyées par le service de la salle. */
    private static void salleC(Tournoi t, Tableau c, Instant maintenant) {
        if (c.getPoules().size() < 3) {
            return;
        }
        Match finPouleB = c.getPoules().get(1).getMatchs().get(2);
        Match debutPouleC = c.getPoules().get(2).getMatchs().get(0);
        new ServiceSalle(() -> maintenant.minusSeconds(15 * 60L)).affecter(t, finPouleB, 2);
        new ServiceSalle(() -> maintenant.minusSeconds(6 * 60L)).affecter(t, debutPouleC, 5);
    }

    /** B : pointage ouvert, 8 pointés sur 12, des paiements en ligne non encore pointés. */
    private static void pointageOuvertB(Tournoi t, Tableau b, ServicePointage pointage) {
        b.ouvrirPointage();
        List<Joueur> inscrits = List.copyOf(b.getInscrits());
        for (int i = 0; i < inscrits.size(); i++) {
            Joueur j = inscrits.get(i);
            if (i % 3 != 2) {
                pointage.pointer(b, j);
            }
            if (i % 2 == 0) {
                pointage.marquerPaye(b, j);
            }
        }
    }

    /** C : tout le monde présent, pointage clôturé, tirage fait, deux poules entamées. */
    private static void lanceC(Tournoi t, Tableau c, ServicePointage pointage) {
        c.ouvrirPointage();
        for (Joueur j : List.copyOf(c.getInscrits())) {
            pointage.pointer(c, j);
            pointage.marquerPaye(c, j);
        }
        pointage.cloturerPointage(c);
        new ServiceLancementTableau(t).lancer(c, GRAINE_TABLEAU_C);

        SaisieResultatMatch saisie = new SaisieResultatMatch(() -> Instant.parse("2026-11-14T09:45:00Z"));
        Poule a = c.getPoules().get(0);
        saisie.enregistrerScore(c, a.getMatchs().get(0), manches(11, 7, 9, 11, 11, 8, 11, 6));
        saisie.enregistrerScore(c, a.getMatchs().get(1), manches(11, 4, 11, 9, 11, 5));
        saisie.enregistrerScore(c, a.getMatchs().get(2), manches(8, 11, 12, 10, 11, 13, 11, 9, 11, 7));
        if (c.getPoules().size() > 1) {
            Poule b = c.getPoules().get(1);
            saisie.enregistrerScore(c, b.getMatchs().get(0), manches(11, 9, 11, 9, 11, 9));
            Match forfait = b.getMatchs().get(1);
            saisie.enregistrerIssue(c, forfait, IssueMatch.FORFAIT, forfait.getJoueurB());
        }
    }

    /** D : toutes présentes, pointage clôturé : le tirage peut être lancé. */
    private static void pretD(Tournoi t, Tableau d, ServicePointage pointage) {
        d.ouvrirPointage();
        for (Joueur j : List.copyOf(d.getInscrits())) {
            pointage.pointer(d, j);
            pointage.marquerPaye(d, j);
        }
        pointage.cloturerPointage(d);
    }

    private static Tableau tableau(Tournoi t, String lettre) {
        return t.getTableaux().stream().filter(x -> x.getNom().startsWith(lettre + " ")).findFirst().orElseThrow();
    }

    private static List<Manche> manches(int... points) {
        Manche[] res = new Manche[points.length / 2];
        for (int i = 0; i < res.length; i++) {
            res[i] = new Manche(points[2 * i], points[2 * i + 1]);
        }
        return List.of(res);
    }
}
