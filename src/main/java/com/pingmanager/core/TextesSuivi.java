package com.pingmanager.core;

import com.pingmanager.model.IssueMatch;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Manche;
import com.pingmanager.model.Match;
import com.pingmanager.model.Poule;
import com.pingmanager.model.StatutTableau;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.TableauElimination;
import com.pingmanager.model.Tour;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiFunction;

/**
 * Textes du suivi d'un tournoi : étape d'un tableau, nom d'un tour, joueur,
 * score, podium. Écrits une seule fois pour l'écran de suivi, la phase finale,
 * les feuilles et le suivi sur téléphone, qui affichent ainsi les mêmes mots.
 */
public final class TextesSuivi {

    /** Libellé d'une place encore inconnue (vainqueur d'un match non joué). */
    public static final String A_DETERMINER = "À déterminer";

    private TextesSuivi() {
    }

    // ------------------------------------------------------------------
    // Joueurs
    // ------------------------------------------------------------------

    public static boolean estConnu(Joueur j) {
        return j != null && !j.estADeterminer();
    }

    /** « Prénom NOM », ou « À déterminer ». */
    public static String nomJoueur(Joueur j) {
        if (!estConnu(j)) {
            return A_DETERMINER;
        }
        String prenom = j.getPrenom() == null ? "" : j.getPrenom().trim();
        String nom = j.getNom() == null ? "" : j.getNom().trim().toUpperCase(Locale.FRENCH);
        return (prenom + " " + nom).trim();
    }

    /** « Club · 1520 pts », ou chaîne vide pour une place à déterminer. */
    public static String detailJoueur(Joueur j) {
        if (!estConnu(j)) {
            return "";
        }
        String club = j.getClub() == null ? "" : j.getClub().trim();
        String pts = j.getPoints() + " pts";
        return club.isEmpty() ? pts : club + " · " + pts;
    }

    // ------------------------------------------------------------------
    // Étape d'un tableau
    // ------------------------------------------------------------------

    /**
     * Étape d'un tableau, vue du public : « Inscriptions », « Pointage »,
     * « Poules : 2 sur 4 jouées », « Tableau final : Quarts de finale »,
     * « Terminé ».
     */
    public static String etape(Tableau t) {
        StatutTableau s = t.getStatut();
        if (s == StatutTableau.PHASE_FINALE && t.aPhaseFinale()) {
            Tour tour = SuiviTournoi.tourEnCours(t.getPhaseFinale());
            return tour == null ? "Terminé" : "Tableau final : " + libelleTour(t.getPhaseFinale(), tour);
        }
        if (s == StatutTableau.LANCE) {
            if (t.poulesTerminees()) {
                return sansTableauFinal(t) ? "Terminé" : "Poules terminées : tirage du tableau final à venir";
            }
            int n = SuiviTournoi.poulesTerminees(t);
            return "Poules : " + n + " sur " + t.getPoules().size() + (n > 1 ? " jouées" : " jouée");
        }
        if (s == StatutTableau.POINTAGE) {
            return "Pointage";
        }
        return "Inscriptions";
    }

    /** Le format de ce tableau s'arrête-t-il aux poules ? */
    public static boolean sansTableauFinal(Tableau t) {
        return new ServicePhaseFinale().diagnostiquer(t).getEtat() == DiagnosticPhaseFinale.Etat.SANS_ELIMINATION;
    }

    /** Texte affiché à la place des poules et de l'arbre quand le tableau n'a pas commencé. */
    public static String texteNonCommence(Tableau t) {
        if (t.getStatut() == StatutTableau.POINTAGE) {
            return "Le pointage de ce tableau est en cours : les poules s'afficheront dès le tirage.";
        }
        return "Ce tableau n'a pas encore commencé : les poules s'afficheront dès le tirage.";
    }

    /** Texte sous les poules quand le tableau final n'est pas (encore) tiré. */
    public static String texteSansArbre(Tableau t) {
        if (t.getStatut() == StatutTableau.LANCE && t.poulesTerminees()) {
            return sansTableauFinal(t) ? "Ce tableau se termine à l'issue des poules : pas de tableau final."
                    : "Poules terminées : le juge-arbitre va tirer le tableau final.";
        }
        return "Le tableau final sera tiré à la fin des poules.";
    }

    // ------------------------------------------------------------------
    // Tours, poules, parties
    // ------------------------------------------------------------------

    /**
     * Nom d'un tour d'après sa distance à la finale : 0 = « Finale »,
     * 1 = « Demi-finales », 2 = « Quarts de finale », 3 = « 1/8 de finale »,
     * 4 = « 1/16 de finale », etc.
     */
    public static String libelleTour(int rangDepuisFinale) {
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

    /** Libellé d'un tour du tableau final : « Quarts de finale », « Finale », « Petite finale »… */
    public static String libelleTour(TableauElimination elimination, Tour tour) {
        if (ServicePhaseFinale.estPetiteFinale(tour)) {
            return "Petite finale";
        }
        List<Tour> principaux = ServicePhaseFinale.toursPrincipaux(elimination);
        int i = principaux.indexOf(tour);
        return i < 0 ? tour.getNom() : libelleTour(principaux.size() - 1 - i);
    }

    /** Où se situe une partie : « Poule B, tour 2 » ou « Quarts de finale ». */
    public static String situation(SuiviTournoi.Partie p) {
        if (p.estEnPoule()) {
            return "Poule " + p.poule().getNom() + ", tour " + p.tourDePoule();
        }
        return libelleTour(p.tableau().getPhaseFinale(), p.tour());
    }

    /** « Poule B · 2 / 3 matchs joués », ou « Poule B · terminée ». */
    public static String titrePoule(Poule p) {
        if (p.estTerminee()) {
            return "Poule " + p.getNom() + " · terminée";
        }
        int n = SuiviPoule.matchsTermines(p);
        return "Poule " + p.getNom() + " · " + n + " / " + p.getMatchs().size()
                + (n > 1 ? " matchs joués" : " match joué");
    }

    /** Situation vis-à-vis de la qualification (pas de switch sur l'énumération). */
    public static String libelleQualification(SuiviPoule.Qualification q) {
        if (q == SuiviPoule.Qualification.QUALIFIE) {
            return "Qualifié";
        }
        if (q == SuiviPoule.Qualification.PLACE_QUALIFICATIVE) {
            return "Place qualificative";
        }
        if (q == SuiviPoule.Qualification.NON_QUALIFIE) {
            return "Non qualifié";
        }
        return "";
    }

    // ------------------------------------------------------------------
    // Scores et podium
    // ------------------------------------------------------------------

    /** Issue non normale en clair (« Forfait », « Abandon »…), {@code siNormale} sinon. */
    public static String libelleIssue(IssueMatch issue, String siNormale) {
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
        return siNormale;
    }

    /** Détail d'un match terminé : « 11-8, 9-11, 11-6 », ou « Forfait de Prénom NOM ». */
    public static String detailScore(Match m) {
        if (m.getIssue() != IssueMatch.NORMALE) {
            return libelleIssue(m.getIssue(), "Score") + " de " + nomJoueur(m.getJoueurFautif());
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

    /**
     * Lignes du podium, dans l'ordre : « 1er », « 2e », « 3e » (ou « 3e ex aequo »
     * sans petite finale), « 4e ». {@code ligne} fabrique chaque ligne à partir
     * de la place et du joueur.
     */
    public static <T> List<T> podium(PodiumPhaseFinale p, BiFunction<String, Joueur, T> ligne) {
        List<T> lignes = new ArrayList<>();
        if (p.premier() != null) {
            lignes.add(ligne.apply("1er", p.premier()));
        }
        if (p.deuxieme() != null) {
            lignes.add(ligne.apply("2e", p.deuxieme()));
        }
        String troisieme = p.troisiemesExAequo() ? "3e ex aequo" : "3e";
        for (Joueur j : p.troisiemes()) {
            lignes.add(ligne.apply(troisieme, j));
        }
        if (p.quatrieme() != null) {
            lignes.add(ligne.apply("4e", p.quatrieme()));
        }
        return lignes;
    }
}
