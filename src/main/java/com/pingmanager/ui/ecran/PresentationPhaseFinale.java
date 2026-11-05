package com.pingmanager.ui.ecran;

import com.pingmanager.core.DiagnosticPhaseFinale;
import com.pingmanager.core.PodiumPhaseFinale;
import com.pingmanager.core.ServicePhaseFinale;
import com.pingmanager.model.IssueMatch;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Manche;
import com.pingmanager.model.Match;
import com.pingmanager.model.StatutMatch;
import com.pingmanager.model.TableauElimination;
import com.pingmanager.model.Tour;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Présentation de la vue « Phase finale », <b>sans JavaFX</b> : tout ce qui se
 * calcule ou se formule (libellés des tours, colonnes du tableau avec les
 * exempts, textes des scores, podium, messages d'état, graine du tirage) est
 * ici et testé ; {@link VuePhaseFinale} ne fait que poser les composants.
 */
final class PresentationPhaseFinale {

    /** Libellé d'un joueur exempté (bye) : pas d'adversaire à ce tour. */
    static final String LIBELLE_EXEMPT = "Exempt";
    /** Explication d'un exempt, sous le nom du joueur. */
    static final String AIDE_EXEMPT = "Pas d'adversaire à ce tour : qualifié d'office.";
    /** Libellé d'une place encore inconnue (vainqueur d'un match non joué). */
    static final String A_DETERMINER = "À déterminer";

    private PresentationPhaseFinale() {
    }

    // ------------------------------------------------------------------
    // Tours et colonnes
    // ------------------------------------------------------------------

    /**
     * Nom d'un tour d'après sa distance à la finale : 0 = « Finale »,
     * 1 = « Demi-finales », 2 = « Quarts de finale », 3 = « 1/8 de finale »,
     * 4 = « 1/16 de finale », etc.
     */
    static String libelleTour(int rangDepuisFinale) {
        if (rangDepuisFinale < 0) {
            throw new IllegalArgumentException("Rang négatif : " + rangDepuisFinale);
        }
        if (rangDepuisFinale == 0) {
            return "Finale";
        }
        if (rangDepuisFinale == 1) {
            return "Demi-finales";
        }
        if (rangDepuisFinale == 2) {
            return "Quarts de finale";
        }
        return "1/" + (1L << rangDepuisFinale) + " de finale";
    }

    /** Nature d'une case du tableau. */
    enum TypeCase {
        /** Un match (joué, à jouer ou en attente de ses joueurs). */
        MATCH,
        /** Un joueur sans adversaire à ce tour (bye) : qualifié d'office. */
        EXEMPT,
        /** Emplacement vide (deux byes face à face) : garde l'alignement. */
        VIDE
    }

    /**
     * Une case d'une colonne : un match, un exempt ou un emplacement vide. Un
     * exempt est soit un joueur connu ({@code joueur}), soit le futur vainqueur
     * d'un match d'un tour antérieur ({@code match}) qui passera ce tour sans
     * jouer.
     */
    record CaseTableau(TypeCase type, Match match, Joueur joueur) {

        static CaseTableau match(Match m) {
            return new CaseTableau(TypeCase.MATCH, Objects.requireNonNull(m), null);
        }

        static CaseTableau exempt(Joueur j) {
            return new CaseTableau(TypeCase.EXEMPT, null, Objects.requireNonNull(j));
        }

        static CaseTableau exemptVainqueurDe(Match source) {
            return new CaseTableau(TypeCase.EXEMPT, Objects.requireNonNull(source), null);
        }

        static CaseTableau vide() {
            return new CaseTableau(TypeCase.VIDE, null, null);
        }

        /** Joueur exempté : connu, ou vainqueur du match source s'il est joué ; sinon {@code null}. */
        Joueur joueurExempte() {
            if (joueur != null) {
                return joueur;
            }
            return match != null && match.getStatut() == StatutMatch.TERMINE ? match.getVainqueur() : null;
        }
    }

    /** Une colonne du tableau (un tour), de haut en bas. */
    record Colonne(String libelle, List<CaseTableau> cases) {
        Colonne {
            cases = List.copyOf(cases);
        }
    }

    /**
     * Colonnes du tableau principal, du premier tour (à gauche) à la finale (à
     * droite). La colonne à {@code d} tours de la finale compte {@code 2^d}
     * cases : chaque case correspond à une place de l'arbre, ce qui garde
     * l'alignement. Un joueur qui entre directement à un tour (bye) apparaît
     * comme {@link TypeCase#EXEMPT} dans chacun des tours qu'il n'a pas joués.
     */
    static List<Colonne> colonnes(TableauElimination elimination) {
        Objects.requireNonNull(elimination, "elimination");
        Match finale = ServicePhaseFinale.matchFinale(elimination);
        if (finale == null) {
            return List.of();
        }
        List<Tour> principaux = ServicePhaseFinale.toursPrincipaux(elimination);
        int profondeur = principaux.size();
        // Tour (indice dans les tours principaux) de chaque match : un match source
        // peut venir de plus loin que le tour précédent quand deux byes se font
        // face au premier tour (son vainqueur saute alors un tour).
        Map<Match, Integer> tourDuMatch = new IdentityHashMap<>();
        for (int i = 0; i < principaux.size(); i++) {
            for (Match m : principaux.get(i).getMatchs()) {
                tourDuMatch.put(m, i);
            }
        }
        List<List<CaseTableau>> niveaux = new ArrayList<>();
        niveaux.add(List.of(CaseTableau.match(finale)));
        for (int d = 1; d < profondeur; d++) {
            int tourAttendu = profondeur - 1 - d;
            List<CaseTableau> parents = niveaux.get(d - 1);
            List<CaseTableau> enfants = new ArrayList<>(parents.size() * 2);
            for (CaseTableau parent : parents) {
                if (parent.type() == TypeCase.MATCH) {
                    Match m = parent.match();
                    enfants.add(origine(m.getSourceA(), m.getJoueurA(), tourAttendu, tourDuMatch));
                    enfants.add(origine(m.getSourceB(), m.getJoueurB(), tourAttendu, tourDuMatch));
                } else if (parent.type() == TypeCase.EXEMPT) {
                    // Exempté à ce tour : au tour précédent, il venait d'un match ou
                    // était déjà exempté ; la place sœur était un bye.
                    enfants.add(parent.joueur() != null
                            ? CaseTableau.exempt(parent.joueur())
                            : origine(parent.match(), null, tourAttendu, tourDuMatch));
                    enfants.add(CaseTableau.vide());
                } else {
                    enfants.add(CaseTableau.vide());
                    enfants.add(CaseTableau.vide());
                }
            }
            niveaux.add(enfants);
        }
        List<Colonne> colonnes = new ArrayList<>();
        for (int d = profondeur - 1; d >= 0; d--) {
            colonnes.add(new Colonne(libelleTour(d), niveaux.get(d)));
        }
        return colonnes;
    }

    private static CaseTableau origine(Match source, Joueur joueur, int tourAttendu,
                                       Map<Match, Integer> tourDuMatch) {
        if (source != null) {
            Integer tour = tourDuMatch.get(source);
            return tour == null || tour == tourAttendu
                    ? CaseTableau.match(source) : CaseTableau.exemptVainqueurDe(source);
        }
        if (joueur != null && !joueur.estADeterminer()) {
            return CaseTableau.exempt(joueur);
        }
        return CaseTableau.vide();
    }

    /** Match de la petite finale (3e place), ou {@code null} s'il n'y en a pas. */
    static Match matchPetiteFinale(TableauElimination elimination) {
        Tour tour = ServicePhaseFinale.petiteFinale(elimination);
        return tour == null || tour.getMatchs().isEmpty() ? null : tour.getMatchs().get(0);
    }

    /** Nombre de joueurs exemptés au premier tour. */
    static int nombreExemptes(TableauElimination elimination) {
        List<Colonne> cols = colonnes(elimination);
        if (cols.isEmpty()) {
            return 0;
        }
        int n = 0;
        for (CaseTableau c : cols.get(0).cases()) {
            if (c.type() == TypeCase.EXEMPT) {
                n++;
            }
        }
        return n;
    }

    /** Résumé du tableau : « 10 qualifiés · tableau de 16 · 6 exempts au premier tour ». */
    static String resume(TableauElimination elimination) {
        int exempts = nombreExemptes(elimination);
        String e = exempts == 0 ? "aucun exempt"
                : exempts == 1 ? "1 exempt au premier tour" : exempts + " exempts au premier tour";
        return elimination.getNombreJoueurs() + " qualifiés · tableau de " + elimination.getCadre() + " · " + e;
    }

    /**
     * Hauteur des écarts pour aligner une colonne : la colonne située
     * {@code niveaux} tours après la première a ses cases centrées entre les
     * deux cases qui l'alimentent.
     *
     * @return {marge en haut, espacement entre deux cases}
     */
    static double[] alignement(int niveaux, double hauteurCase, double ecartPremierTour) {
        double pas = (hauteurCase + ecartPremierTour) * (1L << niveaux);
        double marge = (pas - hauteurCase - ecartPremierTour) / 2.0;
        return new double[] {marge, pas - hauteurCase};
    }

    // ------------------------------------------------------------------
    // Joueurs, matchs, scores
    // ------------------------------------------------------------------

    static boolean estConnu(Joueur j) {
        return j != null && !j.estADeterminer();
    }

    /** « Prénom NOM », ou « À déterminer ». */
    static String nomJoueur(Joueur j) {
        if (!estConnu(j)) {
            return A_DETERMINER;
        }
        String prenom = j.getPrenom() == null ? "" : j.getPrenom().trim();
        String nom = j.getNom() == null ? "" : j.getNom().trim().toUpperCase(Locale.FRENCH);
        return (prenom + " " + nom).trim();
    }

    /** « Club · 1520 pts », ou chaîne vide pour une place à déterminer. */
    static String detailJoueur(Joueur j) {
        if (!estConnu(j)) {
            return "";
        }
        String club = j.getClub() == null ? "" : j.getClub().trim();
        String pts = j.getPoints() + " pts";
        return club.isEmpty() ? pts : club + " · " + pts;
    }

    /** Le match peut-il être saisi (ou corrigé) ? Ses deux joueurs doivent être connus. */
    static boolean estSaisissable(Match m) {
        return m != null && !m.estBye() && estConnu(m.getJoueurA()) && estConnu(m.getJoueurB());
    }

    /** État court d'un match : « À jouer », « En cours », « Terminé », « En attente des joueurs ». */
    static String etatMatch(Match m) {
        if (m.getStatut() == StatutMatch.TERMINE) {
            return "Terminé";
        }
        if (m.getStatut() == StatutMatch.EN_COURS) {
            return "En cours";
        }
        if (estSaisissable(m)) {
            return "À jouer";
        }
        return "En attente des joueurs";
    }

    /** Format du match : « Au meilleur des 5 manches ». */
    static String formatMatch(Match m) {
        return "Au meilleur des " + (2 * m.getNombreManchesGagnantes() - 1) + " manches";
    }

    /** Manches gagnées par A et B (« 3-1 »), ou vide si non joué. */
    static String scoreManches(Match m) {
        if (m.getStatut() != StatutMatch.TERMINE) {
            return "";
        }
        if (m.getIssue() != IssueMatch.NORMALE) {
            return libelleIssue(m.getIssue());
        }
        return m.manchesGagneesParA() + "-" + m.manchesGagneesParB();
    }

    /** Nombre de manches gagnées par l'un des joueurs, pour la colonne de score. */
    static String manchesDe(Match m, boolean joueurA) {
        if (m.getStatut() != StatutMatch.TERMINE || m.getIssue() != IssueMatch.NORMALE) {
            return "";
        }
        return String.valueOf(joueurA ? m.manchesGagneesParA() : m.manchesGagneesParB());
    }

    /**
     * Détail du résultat : « 11-8, 9-11, 11-6, 11-4 », ou « Forfait de Prénom
     * NOM » pour une issue non normale ; vide si non joué.
     */
    static String detailScore(Match m) {
        if (m.getStatut() != StatutMatch.TERMINE) {
            return "";
        }
        if (m.getIssue() != IssueMatch.NORMALE) {
            return libelleIssue(m.getIssue()) + " de " + nomJoueur(m.getJoueurFautif());
        }
        StringBuilder sb = new StringBuilder();
        for (Manche manche : m.getManches()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(manche.pointsJoueurA()).append('-').append(manche.pointsJoueurB());
        }
        return sb.toString();
    }

    /** @return true si ce joueur du match est le vainqueur (match terminé). */
    static boolean estVainqueur(Match m, boolean joueurA) {
        if (m.getStatut() != StatutMatch.TERMINE) {
            return false;
        }
        Joueur v = m.getVainqueur();
        return v != null && v.equals(joueurA ? m.getJoueurA() : m.getJoueurB());
    }

    /** Libellé d'une issue de match (pas de switch sur l'énumération). */
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
        return "Score";
    }

    // ------------------------------------------------------------------
    // Podium
    // ------------------------------------------------------------------

    /** Une ligne du podium : « 1er », « 3e ex aequo »… */
    record LignePodium(String place, String joueur, String detail) {
    }

    static List<LignePodium> lignesPodium(PodiumPhaseFinale p) {
        List<LignePodium> lignes = new ArrayList<>();
        if (p.premier() != null) {
            lignes.add(new LignePodium("1er", nomJoueur(p.premier()), detailJoueur(p.premier())));
        }
        if (p.deuxieme() != null) {
            lignes.add(new LignePodium("2e", nomJoueur(p.deuxieme()), detailJoueur(p.deuxieme())));
        }
        String troisieme = p.troisiemesExAequo() ? "3e ex aequo" : "3e";
        for (Joueur j : p.troisiemes()) {
            lignes.add(new LignePodium(troisieme, nomJoueur(j), detailJoueur(j)));
        }
        if (p.quatrieme() != null) {
            lignes.add(new LignePodium("4e", nomJoueur(p.quatrieme()), detailJoueur(p.quatrieme())));
        }
        return lignes;
    }

    /** Texte d'accompagnement du podium. */
    static String notePodium(PodiumPhaseFinale p) {
        if (!p.estCommence()) {
            return "Le podium se remplit au fil des demi-finales et de la finale.";
        }
        if (p.avecPetiteFinale()) {
            return "La 3e place se joue en petite finale.";
        }
        return "Pas de petite finale dans ce tableau : les deux demi-finalistes battus sont 3es ex aequo.";
    }

    // ------------------------------------------------------------------
    // Graine du tirage
    // ------------------------------------------------------------------

    /** Résultat de la lecture du champ « graine ». */
    record LectureGraine(Long graine, String erreur) {
        boolean estValide() {
            return erreur == null;
        }
    }

    /**
     * Lit la graine saisie (facultative). Vide : graine tirée par
     * {@code generee}. Sinon : entier positif ou nul.
     */
    static LectureGraine lireGraine(String saisie, long generee) {
        String s = saisie == null ? "" : saisie.trim().replace(" ", "").replace(" ", "");
        if (s.isEmpty()) {
            return new LectureGraine(Math.abs(generee % 1_000_000_000L), null);
        }
        try {
            long v = Long.parseLong(s);
            if (v < 0) {
                return new LectureGraine(null, "La graine est un nombre entier positif.");
            }
            return new LectureGraine(v, null);
        } catch (NumberFormatException e) {
            return new LectureGraine(null, "La graine est un nombre entier positif (ou laissez le champ vide).");
        }
    }

    /** Texte affiché sous le tableau construit. */
    static String texteGraine(Long graine) {
        if (graine == null) {
            return "Graine du tirage : non conservée (le tableau a été tiré lors d'une session précédente).";
        }
        return "Graine du tirage : " + graine
                + ". Notez-la : la même graine refait exactement le même tirage (vérification du tirage public).";
    }

    // ------------------------------------------------------------------
    // État : pourquoi on ne peut pas (encore) construire
    // ------------------------------------------------------------------

    /** Titre court de l'état de la phase finale. */
    static String titreEtat(DiagnosticPhaseFinale d) {
        DiagnosticPhaseFinale.Etat e = d.getEtat();
        if (e == DiagnosticPhaseFinale.Etat.PRETE_A_CONSTRUIRE) {
            return "Poules terminées : le tableau final peut être tiré";
        }
        if (e == DiagnosticPhaseFinale.Etat.EN_COURS) {
            return "Tableau final en cours";
        }
        if (e == DiagnosticPhaseFinale.Etat.TERMINEE) {
            return "Tableau final terminé";
        }
        if (e == DiagnosticPhaseFinale.Etat.POULES_EN_COURS) {
            return "Poules en cours";
        }
        if (e == DiagnosticPhaseFinale.Etat.POULES_NON_LANCEES) {
            return "Poules pas encore tirées";
        }
        return "Pas de tableau final";
    }

    /** Explication de l'état : pourquoi la construction est (im)possible, et quoi faire. */
    static String messageEtat(DiagnosticPhaseFinale d) {
        DiagnosticPhaseFinale.Etat e = d.getEtat();
        if (e == DiagnosticPhaseFinale.Etat.SANS_FORMAT) {
            return "Ce tableau n'a pas de format de compétition. Choisissez-en un dans la "
                    + "préparation du tournoi (section Tableaux).";
        }
        if (e == DiagnosticPhaseFinale.Etat.SANS_ELIMINATION) {
            return "Le format de ce tableau ne prévoit pas de tableau à élimination directe : "
                    + "le classement final vient des poules.";
        }
        if (e == DiagnosticPhaseFinale.Etat.POULES_NON_LANCEES) {
            return "Le tableau final se construit après les poules. Clôturez le pointage, "
                    + "lancez le tableau (tirage des poules) et faites jouer les poules.";
        }
        if (e == DiagnosticPhaseFinale.Etat.POULES_EN_COURS) {
            return "Toutes les poules doivent être terminées avant le tirage du tableau final : "
                    + pluriel(d.getPoulesNonTerminees(), "poule n'est pas terminée", "poules ne sont pas terminées")
                    + " (" + pluriel(d.getMatchsPouleRestants(), "match restant", "matchs restants")
                    + " sur " + pluriel(d.getNombrePoules(), "poule", "poules") + ").";
        }
        if (e == DiagnosticPhaseFinale.Etat.QUALIFIES_INSUFFISANTS) {
            return "Les poules ne qualifient que " + d.getQualifiesAttendus()
                    + " joueur : il en faut au moins deux pour un tableau à élimination.";
        }
        if (e == DiagnosticPhaseFinale.Etat.PRETE_A_CONSTRUIRE) {
            return pluriel(d.getQualifiesAttendus(), "qualifié", "qualifiés")
                    + " sortent des poules. Le tirage place les 1ers de poule en têtes de série "
                    + "et les 2es dans le demi-tableau opposé à leur 1er (I.305), en séparant les "
                    + "joueurs d'un même club autant que possible (I.304.1).";
        }
        if (e == DiagnosticPhaseFinale.Etat.EN_COURS) {
            return "Cliquez sur un match à jouer pour saisir son score. Le vainqueur passe "
                    + "automatiquement au tour suivant.";
        }
        return "Tous les matchs sont joués. Le podium est complet.";
    }

    /** « 1 poule » / « 3 poules ». */
    static String pluriel(int n, String singulier, String plurielTexte) {
        return n + " " + (n > 1 ? plurielTexte : singulier);
    }
}
