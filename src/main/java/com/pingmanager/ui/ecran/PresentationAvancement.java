package com.pingmanager.ui.ecran;

import com.pingmanager.core.ServicePhaseFinale;
import com.pingmanager.core.SuiviPoule;
import com.pingmanager.core.reglement.Formats;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Match;
import com.pingmanager.model.Poule;
import com.pingmanager.model.StatutMatch;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.TableauElimination;
import com.pingmanager.model.Tour;
import com.pingmanager.model.Tournoi;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Bandeau « Où en sont les tableaux » de l'écran principal du jour J : une
 * ligne par tableau avec son étape, un texte d'avancement, une jauge et la vue
 * de détail qu'ouvre un clic (pointage, poules ou arbre). Aucune dépendance à
 * JavaFX : testé dans {@code PresentationAvancementTest}.
 *
 * <p>La jauge mesure l'étape en cours : joueurs pointés pendant le pointage,
 * parties jouées pendant les poules, parties jouées du tableau final (les
 * exemptions ne comptent pas, elles ne se jouent pas). Pas de {@code switch}
 * sur une énumération : des chaînes de {@code if}.</p>
 */
final class PresentationAvancement {

    private PresentationAvancement() {
    }

    /** Vue de détail ouverte par un clic sur la ligne d'un tableau. */
    enum Detail {
        /** Inscriptions encore ouvertes : l'ouverture du pointage demande confirmation. */
        OUVRIR_POINTAGE,
        POINTAGE,
        POULES,
        TABLEAU_FINAL
    }

    /**
     * Une ligne du bandeau.
     *
     * @param jauge entre 0 et 1, ou -1 s'il n'y a rien à mesurer (inscriptions)
     * @param enJeu nombre de parties de ce tableau actuellement sur une table
     */
    record Ligne(Tableau tableau, PresentationJourJ.Etape etape, String detail, double jauge, int enJeu,
                 Detail vue, String libelleBouton) {

        String libelleEtape() {
            return PresentationJourJ.libelle(etape);
        }

        String classeEtape() {
            return PresentationJourJ.classe(etape);
        }

        boolean aJauge() {
            return jauge >= 0;
        }

        /** « 7 / 12 » sous forme de pourcentage arrondi, pour l'infobulle de la jauge. */
        String pourcentage() {
            return aJauge() ? Math.round(jauge * 100) + " %" : "";
        }
    }

    /** Un groupe de lignes d'une même journée ({@code jour} nul : sans horaire). */
    record Journee(LocalDate jour, List<Ligne> lignes) {
    }

    /**
     * Lignes du bandeau, regroupées par jour et triées comme la liste du jour J
     * (heure de début puis nom).
     */
    static List<Journee> journees(Tournoi tournoi) {
        List<Journee> res = new ArrayList<>();
        for (Map.Entry<LocalDate, List<Tableau>> e : PresentationJourJ.tableauxParJour(tournoi).entrySet()) {
            List<Ligne> lignes = new ArrayList<>();
            for (Tableau t : e.getValue()) {
                lignes.add(ligne(t));
            }
            res.add(new Journee(e.getKey(), lignes));
        }
        return res;
    }

    /**
     * Journée montrée à l'ouverture (B037 : « garder la vue du jour ») : celle
     * d'aujourd'hui ; sinon la prochaine journée à venir ; sinon la dernière
     * journée datée (tournoi passé). Les tableaux sans horaire ne sont montrés
     * par défaut que s'il n'y a rien d'autre.
     *
     * @return l'indice dans {@code journees}, 0 si la liste est vide
     */
    static int indexJourParDefaut(List<Journee> journees, LocalDate aujourdhui) {
        int derniereDatee = -1;
        for (int i = 0; i < journees.size(); i++) {
            LocalDate j = journees.get(i).jour();
            if (j == null) {
                continue;
            }
            if (!j.isBefore(aujourdhui)) {
                return i; // aujourd'hui, ou la prochaine journée (ordre chronologique)
            }
            derniereDatee = i;
        }
        return Math.max(0, derniereDatee);
    }

    /** Indice borné à la liste (navigation « jour précédent / suivant »). */
    static int borner(int index, List<Journee> journees) {
        return Math.max(0, Math.min(index, journees.size() - 1));
    }

    /** Titre de la journée montrée : « Aujourd'hui · Samedi 14 novembre 2026 ». */
    static String titreJour(Journee j, LocalDate aujourdhui) {
        String jour = PresentationJourJ.jour(j.jour());
        if (j.jour() == null) {
            return jour;
        }
        if (j.jour().equals(aujourdhui)) {
            return "Aujourd'hui · " + jour;
        }
        if (j.jour().equals(aujourdhui.plusDays(1))) {
            return "Demain · " + jour;
        }
        return jour;
    }

    /** Intertitre de journée, seulement si le tournoi s'étale sur plusieurs jours. */
    static boolean afficherJours(List<Journee> journees) {
        return journees.size() > 1;
    }

    static Ligne ligne(Tableau t) {
        PresentationJourJ.Etape e = PresentationJourJ.etape(t);
        int enJeu = enJeu(t);
        if (e == PresentationJourJ.Etape.POINTAGE_OUVERT) {
            PresentationJourJ.Comptes c = PresentationJourJ.comptes(t);
            String texte = c.pointes() + " / " + c.inscrits() + (c.pointes() > 1 ? " pointés" : " pointé")
                    + (c.attente() > 0 ? " · " + c.attente() + " en attente" : "");
            return new Ligne(t, e, texte, fraction(c.pointes(), c.inscrits()), enJeu, Detail.POINTAGE, "Pointer");
        }
        if (e == PresentationJourJ.Etape.POINTAGE_CLOTURE) {
            PresentationJourJ.Comptes c = PresentationJourJ.comptes(t);
            return new Ligne(t, e, "Pointage clos · " + c.pointes() + (c.pointes() > 1 ? " présents" : " présent")
                    + " · tirage à faire", 1, enJeu, Detail.POINTAGE, "Tirage");
        }
        if (e == PresentationJourJ.Etape.POULES || e == PresentationJourJ.Etape.POULES_TERMINEES) {
            return lignePoules(t, e, enJeu);
        }
        if (e == PresentationJourJ.Etape.PHASE_FINALE) {
            return ligneTableauFinal(t, enJeu);
        }
        PresentationJourJ.Comptes c = PresentationJourJ.comptes(t);
        String texte = c.inscrits() + (c.places() > 0 ? " / " + c.places() : "")
                + (c.inscrits() > 1 ? " inscrits" : " inscrit") + debut(t);
        return new Ligne(t, e, texte, -1, enJeu, Detail.OUVRIR_POINTAGE, "Ouvrir le pointage");
    }

    private static Ligne lignePoules(Tableau t, PresentationJourJ.Etape e, int enJeu) {
        int poules = t.getPoules().size();
        int poulesFinies = 0;
        int total = 0;
        int joues = 0;
        for (Poule p : t.getPoules()) {
            total += p.getMatchs().size();
            joues += SuiviPoule.matchsTermines(p);
            if (p.estTerminee()) {
                poulesFinies++;
            }
        }
        if (e == PresentationJourJ.Etape.POULES_TERMINEES) {
            return new Ligne(t, e, (poules > 1 ? "Les " + poules + " poules sont jouées" : "La poule est jouée")
                    + " · tableau final à construire", 1, enJeu, Detail.TABLEAU_FINAL, "Tableau final");
        }
        String texte = "Poules " + poulesFinies + " / " + poules + (poulesFinies > 1 ? " terminées" : " terminée")
                + " · " + joues + " / " + total + (total > 1 ? " parties" : " partie");
        return new Ligne(t, e, texte, fraction(joues, total), enJeu, Detail.POULES, "Poules");
    }

    private static Ligne ligneTableauFinal(Tableau t, int enJeu) {
        if (!t.aPhaseFinale()) {
            return lignePoules(t, PresentationJourJ.Etape.POULES_TERMINEES, enJeu);
        }
        TableauElimination elim = t.getPhaseFinale();
        int total = 0;
        int joues = 0;
        for (Tour tour : elim.getTours()) {
            for (Match m : tour.getMatchs()) {
                if (m.estBye()) {
                    continue;
                }
                total++;
                if (m.getStatut() == StatutMatch.TERMINE) {
                    joues++;
                }
            }
        }
        if (ServicePhaseFinale.estTerminee(elim)) {
            Match finale = ServicePhaseFinale.matchFinale(elim);
            Joueur vainqueur = finale == null ? null : finale.getVainqueur();
            String texte = "Terminé" + (vainqueur == null ? "" : " · vainqueur " + PresentationSalle.nom(vainqueur));
            return new Ligne(t, PresentationJourJ.Etape.PHASE_FINALE, texte, 1, enJeu, Detail.TABLEAU_FINAL,
                    "Arbre");
        }
        String texte = tourEnCours(elim) + " · " + joues + " / " + total + (total > 1 ? " parties" : " partie");
        return new Ligne(t, PresentationJourJ.Etape.PHASE_FINALE, texte, fraction(joues, total), enJeu,
                Detail.TABLEAU_FINAL, "Arbre");
    }

    /**
     * Tour en cours du tableau final : le premier tour du tableau principal qui
     * n'est pas entièrement joué (« Quarts de finale »), « Finale et petite
     * finale » quand les deux restent à jouer, « Petite finale » si elle seule
     * reste.
     */
    static String tourEnCours(TableauElimination elim) {
        List<Tour> principaux = ServicePhaseFinale.toursPrincipaux(elim);
        Tour pf = ServicePhaseFinale.petiteFinale(elim);
        boolean pfAJouer = pf != null && !pf.estTermine();
        for (int i = 0; i < principaux.size(); i++) {
            if (!principaux.get(i).estTermine()) {
                int rang = principaux.size() - 1 - i;
                if (rang == 0 && pfAJouer) {
                    return "Finale et petite finale";
                }
                return PresentationPhaseFinale.libelleTour(rang);
            }
        }
        return pfAJouer ? ServicePhaseFinale.NOM_TOUR_PETITE_FINALE : "Terminé";
    }

    /** Parties de ce tableau actuellement sur une table (poules et tableau final). */
    static int enJeu(Tableau t) {
        int n = 0;
        for (Poule p : t.getPoules()) {
            for (Match m : p.getMatchs()) {
                if (m.getStatut() == StatutMatch.EN_COURS) {
                    n++;
                }
            }
        }
        if (t.aPhaseFinale()) {
            for (Tour tour : t.getPhaseFinale().getTours()) {
                for (Match m : tour.getMatchs()) {
                    if (m.getStatut() == StatutMatch.EN_COURS) {
                        n++;
                    }
                }
            }
        }
        return n;
    }

    /** « 2 en jeu », vide si aucune partie de ce tableau n'est sur une table. */
    static String libelleEnJeu(int n) {
        return n <= 0 ? "" : n + " en jeu";
    }

    private static String debut(Tableau t) {
        LocalDateTime d = t.getHoraires().getDebutMatchs();
        return d == null ? "" : " · début " + Formats.heure(d.toLocalTime());
    }

    private static double fraction(int fait, int total) {
        if (total <= 0) {
            return 0;
        }
        return Math.min(1, Math.max(0, (double) fait / total));
    }
}
