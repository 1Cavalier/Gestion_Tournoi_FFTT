package com.pingmanager.core.feuilles;

import com.pingmanager.core.ServicePhaseFinale;
import com.pingmanager.core.SuiviPoule;
import com.pingmanager.core.feuilles.ArbreImprimable.CaseArbre;
import com.pingmanager.core.feuilles.ArbreImprimable.ColonneArbre;
import com.pingmanager.core.feuilles.ArbreImprimable.TypeCase;
import com.pingmanager.core.feuilles.FeuillePoule.PartiePoule;
import com.pingmanager.model.IssueMatch;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Match;
import com.pingmanager.model.Poule;
import com.pingmanager.model.StatutMatch;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.TableauElimination;
import com.pingmanager.model.Tour;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.format.FormatTournoi;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Prépare le <b>contenu</b> des feuilles imprimées du jour J, sans rien savoir
 * du PDF (le rendu est fait hors du cœur, par {@code ui.infra.RenduPdfFeuilles}) :
 * <ul>
 *   <li>feuille de match d'une partie (poule ou tableau final) ;</li>
 *   <li>feuille de poule (ordre I.301, grille, classement I.303 à remplir) suivie
 *       de la feuille de rencontre (chaque partie avec sa grille de manches) ;</li>
 *   <li>tableau final imprimable (arbre), avec les résultats déjà saisis.</li>
 * </ul>
 * Format provisoire imposé par Quentin (cahier des charges v0, section 9) ; le
 * format officiel viendra plus tard. Service sans état.
 */
public final class GenerateurFeuilles {

    private static final DateTimeFormatter JJ_MM_AAAA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Signatures en bas d'une feuille de match. */
    static final List<String> SIGNATURES = List.of("Joueur A", "Joueur B", "Arbitre");

    /** Colonnes du classement à remplir d'une feuille de poule. */
    static final List<String> COLONNES_CLASSEMENT =
            List.of("Place", "N°", "Joueur", "Points", "Victoires", "Défaites", "Manches");

    /** I.303 a) et b) à e), résumés sous le classement à remplir. */
    static final String NOTE_I303 = "I.303 : victoire 2 points, défaite 1 point ; 0 point en cas d'absence à l'appel, "
            + "d'abandon, de refus de jouer ou de disqualification. Deux ex aequo : départagés par leur partie. "
            + "Plus de deux : points, puis quotient des manches, puis quotient des points des parties entre eux ; "
            + "égalité persistante : tirage au sort.";

    // ==================================================================
    // Feuille de match
    // ==================================================================

    /**
     * Feuille de match d'une partie, en cherchant le tableau, la poule ou le tour
     * dans le tournoi (cas de la salle, où l'on ne connaît que le match).
     */
    public ResultatFeuille<FeuilleMatch> feuilleMatch(Tournoi tournoi, Match match) {
        Objects.requireNonNull(tournoi, "tournoi");
        Objects.requireNonNull(match, "match");
        for (Tableau t : tournoi.getTableaux()) {
            if (situer(t, match) != null) {
                return feuilleMatch(tournoi, t, match);
            }
        }
        return ResultatFeuille.refusee(MotifRefusFeuille.MATCH_INTROUVABLE);
    }

    /** Feuille de match d'une partie du tableau donné. */
    public ResultatFeuille<FeuilleMatch> feuilleMatch(Tournoi tournoi, Tableau tableau, Match match) {
        Objects.requireNonNull(tournoi, "tournoi");
        Objects.requireNonNull(tableau, "tableau");
        Objects.requireNonNull(match, "match");
        String phase = situer(tableau, match);
        if (phase == null) {
            return ResultatFeuille.refusee(MotifRefusFeuille.MATCH_INTROUVABLE);
        }
        if (match.estBye()) {
            return ResultatFeuille.refusee(MotifRefusFeuille.MATCH_EXEMPT);
        }
        if (!connu(match.getJoueurA()) || !connu(match.getJoueurB())) {
            return ResultatFeuille.refusee(MotifRefusFeuille.JOUEURS_A_DETERMINER);
        }
        Integer table = match.getNumeroTable();
        return ResultatFeuille.prete(new FeuilleMatch(entete(tournoi, tableau), phase,
                table == null ? "" : "Table " + table, format(match.getNombreManchesGagnantes()),
                JoueurFeuille.de(match.getJoueurA()), JoueurFeuille.de(match.getJoueurB()),
                GrilleManches.vide(match.getNombreManchesGagnantes()), SIGNATURES));
    }

    /**
     * Feuilles de match de toutes les parties d'une poule, dans l'ordre I.301
     * (les exempts n'ont pas de feuille). Liste vide si la poule n'a pas de partie.
     */
    public List<FeuilleMatch> feuillesMatchsPoule(Tournoi tournoi, Tableau tableau, Poule poule) {
        List<FeuilleMatch> r = new ArrayList<>();
        for (Match m : poule.getMatchs()) {
            ResultatFeuille<FeuilleMatch> f = feuilleMatch(tournoi, tableau, m);
            if (f.estPrete()) {
                r.add(f.feuille());
            }
        }
        return r;
    }

    // ==================================================================
    // Feuille de poule
    // ==================================================================

    /** Feuille de poule (et feuille de rencontre) d'une poule du tableau. */
    public ResultatFeuille<FeuillePoule> feuillePoule(Tournoi tournoi, Tableau tableau, Poule poule) {
        Objects.requireNonNull(tournoi, "tournoi");
        Objects.requireNonNull(tableau, "tableau");
        Objects.requireNonNull(poule, "poule");
        if (poule.getMatchs().isEmpty()) {
            return ResultatFeuille.refusee(MotifRefusFeuille.POULE_SANS_PARTIE);
        }
        List<Joueur> joueurs = poule.getJoueurs();
        List<JoueurFeuille> imprimes = new ArrayList<>(joueurs.size());
        for (Joueur j : joueurs) {
            imprimes.add(JoueurFeuille.de(j));
        }
        // I.301 : les parties sont déjà rangées dans l'ordre officiel par
        // GenerateurMatchsPoule ; on garde cet ordre et le tour de chacune.
        List<PartiePoule> parties = new ArrayList<>();
        List<Match> matchs = poule.getMatchs();
        for (int i = 0; i < matchs.size(); i++) {
            Match m = matchs.get(i);
            int posA = joueurs.indexOf(m.getJoueurA()) + 1;
            int posB = m.estBye() ? 0 : joueurs.indexOf(m.getJoueurB()) + 1;
            parties.add(new PartiePoule(i + 1, SuiviPoule.tourDuMatch(poule, i), posA, posB,
                    JoueurFeuille.de(m.getJoueurA()), JoueurFeuille.de(m.getJoueurB()), m.estBye(),
                    GrilleManches.vide(m.getNombreManchesGagnantes())));
        }
        int manches = matchs.get(0).getNombreManchesGagnantes();
        return ResultatFeuille.prete(new FeuillePoule(entete(tournoi, tableau), "Poule " + poule.getNom(),
                format(manches), qualifies(tableau), imprimes, parties, grilleResultats(joueurs.size()),
                COLONNES_CLASSEMENT, NOTE_I303));
    }

    /** Grille (n + 1) × (n + 1) : coin vide, en-têtes « 1 »…« n », diagonale barrée, cases vides. */
    static List<List<String>> grilleResultats(int n) {
        List<List<String>> g = new ArrayList<>(n + 1);
        List<String> entete = new ArrayList<>(n + 1);
        entete.add("");
        for (int c = 1; c <= n; c++) {
            entete.add(String.valueOf(c));
        }
        g.add(entete);
        for (int l = 1; l <= n; l++) {
            List<String> ligne = new ArrayList<>(n + 1);
            ligne.add(String.valueOf(l));
            for (int c = 1; c <= n; c++) {
                ligne.add(c == l ? FeuillePoule.CASE_BARREE : "");
            }
            g.add(ligne);
        }
        return g;
    }

    // ==================================================================
    // Tableau final (arbre)
    // ==================================================================

    /** Arbre imprimable du tableau final, avec les résultats déjà saisis. */
    public ResultatFeuille<ArbreImprimable> arbre(Tournoi tournoi, Tableau tableau) {
        Objects.requireNonNull(tournoi, "tournoi");
        Objects.requireNonNull(tableau, "tableau");
        TableauElimination elimination = tableau.getPhaseFinale();
        Match finale = elimination == null ? null : ServicePhaseFinale.matchFinale(elimination);
        if (finale == null) {
            return ResultatFeuille.refusee(MotifRefusFeuille.PAS_DE_TABLEAU_FINAL);
        }
        List<Tour> principaux = ServicePhaseFinale.toursPrincipaux(elimination);
        int profondeur = principaux.size();
        Map<Match, Integer> tourDuMatch = new IdentityHashMap<>();
        for (int i = 0; i < profondeur; i++) {
            for (Match m : principaux.get(i).getMatchs()) {
                tourDuMatch.put(m, i);
            }
        }
        // On descend depuis la finale : chaque place du niveau d se dédouble au
        // niveau d + 1 (même principe que l'arbre affiché à l'écran).
        List<List<Place>> niveaux = new ArrayList<>();
        niveaux.add(List.of(Place.match(finale)));
        for (int d = 1; d < profondeur; d++) {
            int tourAttendu = profondeur - 1 - d;
            List<Place> enfants = new ArrayList<>();
            for (Place parent : niveaux.get(d - 1)) {
                if (parent.type == TypeCase.MATCH) {
                    enfants.add(origine(parent.match.getSourceA(), parent.match.getJoueurA(), tourAttendu, tourDuMatch));
                    enfants.add(origine(parent.match.getSourceB(), parent.match.getJoueurB(), tourAttendu, tourDuMatch));
                } else if (parent.type == TypeCase.EXEMPT) {
                    enfants.add(parent.joueur != null ? Place.exempt(parent.joueur, null)
                            : origine(parent.match, null, tourAttendu, tourDuMatch));
                    enfants.add(Place.VIDE);
                } else {
                    enfants.add(Place.VIDE);
                    enfants.add(Place.VIDE);
                }
            }
            niveaux.add(enfants);
        }
        List<ColonneArbre> colonnes = new ArrayList<>();
        for (int d = profondeur - 1; d >= 0; d--) {
            List<CaseArbre> cases = new ArrayList<>();
            for (Place p : niveaux.get(d)) {
                cases.add(p.enCase());
            }
            colonnes.add(new ColonneArbre(principaux.get(profondeur - 1 - d).getNom(), cases));
        }
        Tour pf = ServicePhaseFinale.petiteFinale(elimination);
        CaseArbre petite = pf == null || pf.getMatchs().isEmpty() ? null : caseMatch(pf.getMatchs().get(0));
        return ResultatFeuille.prete(new ArbreImprimable(entete(tournoi, tableau), resume(elimination, colonnes),
                colonnes, petite));
    }

    private static Place origine(Match source, Joueur joueur, int tourAttendu, Map<Match, Integer> tourDuMatch) {
        if (source != null) {
            Integer tour = tourDuMatch.get(source);
            return tour == null || tour == tourAttendu ? Place.match(source) : Place.exempt(null, source);
        }
        if (connu(joueur)) {
            return Place.exempt(joueur, null);
        }
        return Place.VIDE;
    }

    private static String resume(TableauElimination e, List<ColonneArbre> colonnes) {
        int exempts = 0;
        if (!colonnes.isEmpty()) {
            for (CaseArbre c : colonnes.get(0).cases()) {
                if (c.type() == TypeCase.EXEMPT) {
                    exempts++;
                }
            }
        }
        String x = exempts == 0 ? "aucun exempt"
                : exempts == 1 ? "1 exempt au premier tour" : exempts + " exempts au premier tour";
        return e.getNombreJoueurs() + " qualifiés · tableau de " + e.getCadre() + " · " + x;
    }

    /** Case d'un match : joueurs, manches gagnées, vainqueur, table ou issue. */
    static CaseArbre caseMatch(Match m) {
        boolean termine = m.getStatut() == StatutMatch.TERMINE;
        Joueur vainqueur = termine ? m.getVainqueur() : null;
        boolean normale = m.getIssue() == null || m.getIssue() == IssueMatch.NORMALE;
        String scoreA = termine && normale ? String.valueOf(m.manchesGagneesParA()) : "";
        String scoreB = termine && normale ? String.valueOf(m.manchesGagneesParB()) : "";
        String mention = "";
        if (termine && !normale) {
            mention = libelleIssue(m.getIssue());
        } else if (!termine && m.getNumeroTable() != null) {
            mention = "Table " + m.getNumeroTable();
        }
        return new CaseArbre(TypeCase.MATCH, JoueurFeuille.de(m.getJoueurA()), JoueurFeuille.de(m.getJoueurB()),
                scoreA, scoreB, vainqueur != null && vainqueur.equals(m.getJoueurA()),
                vainqueur != null && vainqueur.equals(m.getJoueurB()), mention);
    }

    /** Une place de l'arbre pendant la construction. */
    private static final class Place {
        static final Place VIDE = new Place(TypeCase.VIDE, null, null);
        final TypeCase type;
        final Match match;
        final Joueur joueur;

        private Place(TypeCase type, Match match, Joueur joueur) {
            this.type = type;
            this.match = match;
            this.joueur = joueur;
        }

        static Place match(Match m) {
            return new Place(TypeCase.MATCH, m, null);
        }

        /** Exempt connu ({@code joueur}) ou futur vainqueur de {@code source} qui saute ce tour. */
        static Place exempt(Joueur joueur, Match source) {
            return new Place(TypeCase.EXEMPT, source, joueur);
        }

        CaseArbre enCase() {
            if (type == TypeCase.MATCH) {
                return caseMatch(match);
            }
            if (type == TypeCase.EXEMPT) {
                Joueur j = joueur;
                if (j == null && match != null && match.getStatut() == StatutMatch.TERMINE) {
                    j = match.getVainqueur();
                }
                return CaseArbre.exempt(JoueurFeuille.de(j));
            }
            return CaseArbre.vide();
        }
    }

    // ==================================================================
    // Outils communs
    // ==================================================================

    /**
     * Où se trouve ce match dans le tableau : « Poule B · tour 2 »,
     * « Quarts de finale », « Petite finale » ; {@code null} s'il n'y est pas.
     */
    static String situer(Tableau tableau, Match match) {
        for (Poule p : tableau.getPoules()) {
            List<Match> matchs = p.getMatchs();
            for (int i = 0; i < matchs.size(); i++) {
                if (matchs.get(i) == match) {
                    return "Poule " + p.getNom() + " · tour " + SuiviPoule.tourDuMatch(p, i);
                }
            }
        }
        TableauElimination e = tableau.getPhaseFinale();
        if (e != null) {
            for (Tour t : e.getTours()) {
                for (Match m : t.getMatchs()) {
                    if (m == match) {
                        return t.getNom();
                    }
                }
            }
        }
        return null;
    }

    static EnTeteFeuille entete(Tournoi tournoi, Tableau tableau) {
        String ville = tournoi.getAdresse() == null || tournoi.getAdresse().getVille() == null
                ? "" : tournoi.getAdresse().getVille();
        LocalDate date = tournoi.getDateDebut();
        if (date == null) {
            try {
                date = tournoi.getDate();
            } catch (NoSuchElementException e) {
                date = null;
            }
        }
        return new EnTeteFeuille(tournoi.getNom(), date == null ? "" : JJ_MM_AAAA.format(date), ville,
                tableau.getNom());
    }

    /** 2.12.1 : « Au meilleur des 5 manches (3 manches gagnantes) ». */
    static String format(int manchesGagnantes) {
        if (manchesGagnantes <= 0) {
            return "";
        }
        return "Au meilleur des " + (2 * manchesGagnantes - 1) + " manches (" + manchesGagnantes
                + (manchesGagnantes == 1 ? " manche gagnante)" : " manches gagnantes)");
    }

    private static int qualifies(Tableau tableau) {
        FormatTournoi f = tableau.getConfiguration().getFormat();
        return f != null ? f.qualifiesParPoule() : tableau.getConfiguration().getQualifiesParPoule();
    }

    private static boolean connu(Joueur j) {
        return j != null && !j.estADeterminer();
    }

    static String libelleIssue(IssueMatch issue) {
        if (issue == IssueMatch.FORFAIT) {
            return "Forfait";
        }
        if (issue == IssueMatch.ABANDON) {
            return "Abandon";
        }
        if (issue == IssueMatch.REFUS_DE_JOUER) {
            return "Refus de jouer";
        }
        if (issue == IssueMatch.DISQUALIFICATION) {
            return "Disqualification";
        }
        return "";
    }
}
