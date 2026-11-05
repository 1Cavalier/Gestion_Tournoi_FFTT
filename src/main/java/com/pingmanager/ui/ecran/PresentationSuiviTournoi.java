package com.pingmanager.ui.ecran;

import com.pingmanager.core.DiagnosticPhaseFinale;
import com.pingmanager.core.ServicePhaseFinale;
import com.pingmanager.core.SuiviPoule;
import com.pingmanager.core.SuiviTournoi;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.LigneClassement;
import com.pingmanager.model.Match;
import com.pingmanager.model.Poule;
import com.pingmanager.model.StatutMatch;
import com.pingmanager.model.StatutTableau;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.TableauElimination;
import com.pingmanager.model.Tour;
import com.pingmanager.model.Tournoi;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Présentation du <b>suivi d'un tournoi en cours</b> (fiche du tournoi en mode
 * suivi, onglet Tournois et « Mes tournois »), <b>sans JavaFX</b> : liste des
 * tableaux et leur étape, tableau affiché, poules avec classement en direct,
 * matchs en cours, prochaine partie du joueur connecté, podium, texte de mise à
 * jour. {@link EcranSuiviTournoi} ne fait que poser les composants, en lecture
 * seule. Les données viennent de {@link SuiviTournoi}, {@link SuiviPoule} et
 * {@link ServicePhaseFinale} ; les textes de match et de podium de
 * {@link PresentationPhaseFinale}.
 */
final class PresentationSuiviTournoi {

    /** Relecture automatique du fichier du tournoi, en secondes. */
    static final int INTERVALLE_ACTUALISATION_SECONDES = 30;

    private PresentationSuiviTournoi() {
    }

    /** La fiche du tournoi bascule-t-elle en mode suivi ? */
    static boolean afficherSuivi(Tournoi tournoi) {
        return SuiviTournoi.estEnSuivi(tournoi);
    }

    // ------------------------------------------------------------------
    // Liste des tableaux
    // ------------------------------------------------------------------

    /** Un tableau dans la barre de navigation. */
    record EntreeTableau(Tableau tableau, String nom, String etape, String classeEtape, boolean estAMoi) {
    }

    /** Tableaux du tournoi, ceux du joueur (licence, ou {@code null}) en premier, puis l'ordre de la journée. */
    static List<EntreeTableau> tableaux(Tournoi tournoi, String licence) {
        List<EntreeTableau> res = new ArrayList<>();
        for (Tableau t : SuiviTournoi.tableauxOrdonnes(tournoi, licence)) {
            res.add(new EntreeTableau(t, t.getNom(), etape(t), classeEtape(t), SuiviTournoi.estInscrit(t, licence)));
        }
        return res;
    }

    /**
     * Tableau à afficher : celui de même nom que le précédent (le tournoi est
     * relu à chaque actualisation, les objets changent), sinon le premier
     * tableau commencé de la liste, sinon le premier ; {@code null} si aucun.
     */
    static Tableau tableauAffiche(List<EntreeTableau> entrees, String nomPrecedent) {
        if (nomPrecedent != null) {
            for (EntreeTableau e : entrees) {
                if (e.nom().equals(nomPrecedent)) {
                    return e.tableau();
                }
            }
        }
        for (EntreeTableau e : entrees) {
            if (SuiviTournoi.estCommence(e.tableau())) {
                return e.tableau();
            }
        }
        return entrees.isEmpty() ? null : entrees.get(0).tableau();
    }

    /**
     * Étape d'un tableau, vue du public : « Inscriptions », « Pointage »,
     * « Poules : 2 sur 4 jouées », « Tableau final : Quarts de finale »,
     * « Terminé ».
     */
    static String etape(Tableau t) {
        StatutTableau s = t.getStatut();
        if (s == StatutTableau.PHASE_FINALE && t.aPhaseFinale()) {
            Tour tour = SuiviTournoi.tourEnCours(t.getPhaseFinale());
            return tour == null ? "Terminé" : "Tableau final : " + libelleTour(t.getPhaseFinale(), tour);
        }
        if (s == StatutTableau.LANCE) {
            if (t.poulesTerminees()) {
                return sansTableauFinal(t) ? "Terminé" : "Poules terminées : tirage du tableau final à venir";
            }
            return "Poules : " + SuiviTournoi.poulesTerminees(t) + " sur " + t.getPoules().size()
                    + (SuiviTournoi.poulesTerminees(t) > 1 ? " jouées" : " jouée");
        }
        if (s == StatutTableau.POINTAGE) {
            return "Pointage";
        }
        return "Inscriptions";
    }

    /** Classe de style de la pastille d'étape (section « Suivi du tournoi » de theme.css). */
    static String classeEtape(Tableau t) {
        String e = etape(t);
        if (e.equals("Terminé")) {
            return "suivi-etape-termine";
        }
        if (t.getStatut() == StatutTableau.PHASE_FINALE) {
            return "suivi-etape-final";
        }
        if (t.getStatut() == StatutTableau.LANCE) {
            return "suivi-etape-poules";
        }
        return "suivi-etape-attente";
    }

    private static boolean sansTableauFinal(Tableau t) {
        return new ServicePhaseFinale().diagnostiquer(t).getEtat() == DiagnosticPhaseFinale.Etat.SANS_ELIMINATION;
    }

    /** Texte affiché à la place des poules et de l'arbre quand le tableau n'a pas commencé. */
    static String texteNonCommence(Tableau t) {
        if (t.getStatut() == StatutTableau.POINTAGE) {
            return "Le pointage de ce tableau est en cours : les poules s'afficheront dès le tirage.";
        }
        return "Ce tableau n'a pas encore commencé : les poules s'afficheront dès le tirage.";
    }

    // ------------------------------------------------------------------
    // Tours et matchs
    // ------------------------------------------------------------------

    /** Libellé d'un tour du tableau final : « Quarts de finale », « Finale », « Petite finale »… */
    static String libelleTour(TableauElimination elimination, Tour tour) {
        if (ServicePhaseFinale.estPetiteFinale(tour)) {
            return "Petite finale";
        }
        List<Tour> principaux = ServicePhaseFinale.toursPrincipaux(elimination);
        int i = principaux.indexOf(tour);
        return i < 0 ? tour.getNom() : PresentationPhaseFinale.libelleTour(principaux.size() - 1 - i);
    }

    /** Où se situe une partie : « Poule B, tour 2 » ou « Quarts de finale ». */
    static String situation(SuiviTournoi.Partie p) {
        if (p.estEnPoule()) {
            return "Poule " + p.poule().getNom() + ", tour " + p.tourDePoule();
        }
        return libelleTour(p.tableau().getPhaseFinale(), p.tour());
    }

    /** « Table 3 », ou vide si aucune table n'est attribuée. */
    static String table(Match m) {
        return m.getNumeroTable() == null ? "" : "Table " + m.getNumeroTable();
    }

    /**
     * Une ligne de match (poule ou matchs en cours), en lecture seule :
     * {@code resultat} court à droite (« 3-1 », « Forfait », ou l'état « À jouer »),
     * {@code detail} des manches ou de l'issue en dessous (vide si non joué).
     */
    record LigneMatch(String situation, String joueurA, String joueurB, String resultat, String detail,
                      String table, boolean vainqueurA, boolean vainqueurB, boolean enCours) {
    }

    static LigneMatch ligneMatch(String situation, Match m) {
        String manches = PresentationPhaseFinale.scoreManches(m);
        return new LigneMatch(situation,
                PresentationPhaseFinale.nomJoueur(m.getJoueurA()),
                PresentationPhaseFinale.nomJoueur(m.getJoueurB()),
                manches.isEmpty() ? PresentationPhaseFinale.etatMatch(m) : manches,
                PresentationPhaseFinale.detailScore(m),
                m.getStatut() == StatutMatch.EN_COURS ? table(m) : "",
                PresentationPhaseFinale.estVainqueur(m, true),
                PresentationPhaseFinale.estVainqueur(m, false),
                m.getStatut() == StatutMatch.EN_COURS);
    }

    /** Matchs en cours du tableau, avec leur table. */
    static List<LigneMatch> matchsEnCours(Tableau t) {
        List<LigneMatch> res = new ArrayList<>();
        for (SuiviTournoi.Partie p : SuiviTournoi.matchsEnCours(t)) {
            res.add(ligneMatch(situation(p), p.match()));
        }
        return res;
    }

    static String aucunMatchEnCours() {
        return "Aucun match de ce tableau ne se joue en ce moment.";
    }

    // ------------------------------------------------------------------
    // Poules
    // ------------------------------------------------------------------

    /** « Poule B · 2 / 3 matchs joués », ou « Poule B · terminée ». */
    static String titrePoule(Poule p) {
        if (p.estTerminee()) {
            return "Poule " + p.getNom() + " · terminée";
        }
        return "Poule " + p.getNom() + " · " + SuiviPoule.matchsTermines(p) + " / " + p.getMatchs().size()
                + (SuiviPoule.matchsTermines(p) > 1 ? " matchs joués" : " match joué");
    }

    /** Une ligne du classement en direct d'une poule. */
    record LignePoule(String rang, String joueur, String detail, String bilan, String qualification,
                      String classe, boolean estMoi) {
    }

    /** Classement en direct de la poule (I.303), places qualificatives signalées. */
    static List<LignePoule> classement(Tableau t, Poule p, String licence) {
        List<LignePoule> res = new ArrayList<>();
        int qualifies = t.getConfiguration().getQualifiesParPoule();
        for (SuiviPoule.Ligne l : new SuiviPoule().classementEnDirect(p, qualifies)) {
            LigneClassement c = l.classement();
            Joueur j = c.getJoueur();
            res.add(new LignePoule(c.getRang() + (c.getRang() == 1 ? "er" : "e"),
                    PresentationPhaseFinale.nomJoueur(j),
                    PresentationPhaseFinale.detailJoueur(j),
                    c.getMatchsGagnes() + (c.getMatchsGagnes() > 1 ? " victoires · " : " victoire · ")
                            + c.getPointsClassement() + " pts",
                    libelleQualification(l.qualification()),
                    l.enPlaceQualificative() ? "suivi-qualifie" : "suivi-non-qualifie",
                    licence != null && licence.equals(j.getNumeroLicence())));
        }
        return res;
    }

    /** Situation vis-à-vis de la qualification (pas de switch sur l'énumération). */
    static String libelleQualification(SuiviPoule.Qualification q) {
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

    /** Matchs de la poule dans l'ordre officiel (I.301), avec leur tour. */
    static List<LigneMatch> matchsPoule(Poule p) {
        List<LigneMatch> res = new ArrayList<>();
        for (int i = 0; i < p.getMatchs().size(); i++) {
            res.add(ligneMatch("Tour " + SuiviPoule.tourDuMatch(p, i), p.getMatchs().get(i)));
        }
        return res;
    }

    // ------------------------------------------------------------------
    // Tableau final et podium
    // ------------------------------------------------------------------

    /** Texte sous les poules quand le tableau final n'est pas (encore) tiré. */
    static String texteSansArbre(Tableau t) {
        if (t.getStatut() == StatutTableau.LANCE && t.poulesTerminees()) {
            return sansTableauFinal(t) ? "Ce tableau se termine à l'issue des poules : pas de tableau final."
                    : "Poules terminées : le juge-arbitre va tirer le tableau final.";
        }
        return "Le tableau final sera tiré à la fin des poules.";
    }

    /** Le podium s'affiche-t-il ? Seulement quand le tableau final est entièrement joué. */
    static boolean afficherPodium(Tableau t) {
        return t.aPhaseFinale() && ServicePhaseFinale.estTerminee(t.getPhaseFinale());
    }

    static List<PresentationPhaseFinale.LignePodium> podium(Tableau t) {
        return PresentationPhaseFinale.lignesPodium(ServicePhaseFinale.podium(t.getPhaseFinale()));
    }

    // ------------------------------------------------------------------
    // Prochaine partie du joueur connecté
    // ------------------------------------------------------------------

    /** La partie mise en avant pour le joueur connecté. */
    record ProchainePartie(String titre, String adversaire, String detail, String table, boolean enCours) {
    }

    /**
     * Prochaine partie du joueur de licence donnée, ou vide (non inscrit,
     * aucune licence liée, rien à jouer pour l'instant).
     */
    static Optional<ProchainePartie> prochainePartie(Tournoi tournoi, String licence) {
        Optional<SuiviTournoi.Partie> p = SuiviTournoi.prochainePartie(tournoi, licence);
        if (p.isEmpty()) {
            return Optional.empty();
        }
        SuiviTournoi.Partie partie = p.get();
        Joueur adv = partie.adversaire(licence);
        String adversaire = adv == null ? "Adversaire à déterminer" : "Contre " + PresentationPhaseFinale.nomJoueur(adv);
        if (adv != null) {
            String d = PresentationPhaseFinale.detailJoueur(adv);
            adversaire += d.isEmpty() ? "" : " (" + d + ")";
        }
        String detail = partie.tableau().getNom() + " · " + situation(partie);
        String table = table(partie.match());
        if (table.isEmpty()) {
            table = "Table pas encore attribuée";
        }
        return Optional.of(new ProchainePartie(
                partie.estEnCours() ? "Vous jouez en ce moment" : "Votre prochaine partie",
                adversaire, detail, table, partie.estEnCours()));
    }

    /**
     * Texte pour un joueur inscrit qui n'a pas de partie à venir ; vide s'il
     * n'est inscrit à aucun tableau commencé (rien à afficher).
     */
    static String texteSansProchainePartie(Tournoi tournoi, String licence) {
        if (licence == null) {
            return "";
        }
        for (Tableau t : tournoi.getTableaux()) {
            if (SuiviTournoi.estInscrit(t, licence) && SuiviTournoi.estCommence(t)) {
                return "Aucune partie à jouer pour l'instant : suivez vos tableaux ci-dessous.";
            }
        }
        return "";
    }

    // ------------------------------------------------------------------
    // Mise à jour
    // ------------------------------------------------------------------

    /** « Mis à jour à 14 h 05 · actualisation automatique toutes les 30 s ». */
    static String texteMiseAJour(LocalTime heure) {
        return "Mis à jour à " + heure.getHour() + " h " + String.format("%02d", heure.getMinute())
                + " · actualisation automatique toutes les " + INTERVALLE_ACTUALISATION_SECONDES + " s";
    }

    /** Message si le fichier du tournoi n'a pas pu être relu (l'affichage précédent reste). */
    static String erreurRelecture(String detail) {
        return "Le tournoi n'a pas pu être relu" + (detail == null || detail.isBlank() ? "." : " : " + detail + ".")
                + " L'affichage précédent est conservé.";
    }
}
