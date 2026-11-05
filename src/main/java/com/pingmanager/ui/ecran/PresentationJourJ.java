package com.pingmanager.ui.ecran;

import com.pingmanager.core.NotationManche;
import com.pingmanager.core.ResultatPointage;
import com.pingmanager.core.ResultatSaisieMatch;
import com.pingmanager.core.ResultatVerificationLancement;
import com.pingmanager.core.SaisieResultatMatch;
import com.pingmanager.core.SuiviPoule;
import com.pingmanager.core.reglement.Formats;
import com.pingmanager.model.IssueMatch;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Manche;
import com.pingmanager.model.Match;
import com.pingmanager.model.Poule;
import com.pingmanager.model.StatutMatch;
import com.pingmanager.model.StatutTableau;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

/**
 * Textes et calculs d'affichage des écrans du jour J (liste des tableaux,
 * pointage, tirage, poules). Aucune dépendance à JavaFX : tout ce qui se décide
 * ici est testé sans lancer l'interface ({@code PresentationJourJTest}).
 *
 * <p>Pas de {@code switch} sur une énumération (classe annexe générée par javac) :
 * des chaînes de {@code if}.</p>
 */
final class PresentationJourJ {

    private PresentationJourJ() {
    }

    // ------------------------------------------------------------------
    // Étape d'un tableau le jour J
    // ------------------------------------------------------------------

    /** Où en est un tableau, du point de vue du juge-arbitre. */
    enum Etape {
        INSCRIPTION,
        POINTAGE_OUVERT,
        POINTAGE_CLOTURE,
        POULES,
        POULES_TERMINEES,
        PHASE_FINALE
    }

    static Etape etape(Tableau t) {
        StatutTableau s = t.getStatut();
        if (s == StatutTableau.PHASE_FINALE) {
            return Etape.PHASE_FINALE;
        }
        if (s == StatutTableau.LANCE) {
            return t.poulesTerminees() ? Etape.POULES_TERMINEES : Etape.POULES;
        }
        if (s == StatutTableau.POINTAGE) {
            return t.pointageEstCloture() ? Etape.POINTAGE_CLOTURE : Etape.POINTAGE_OUVERT;
        }
        return Etape.INSCRIPTION;
    }

    static String libelle(Etape e) {
        if (e == Etape.POINTAGE_OUVERT) {
            return "Pointage ouvert";
        }
        if (e == Etape.POINTAGE_CLOTURE) {
            return "Prêt à lancer";
        }
        if (e == Etape.POULES) {
            return "Poules en cours";
        }
        if (e == Etape.POULES_TERMINEES) {
            return "Poules terminées";
        }
        if (e == Etape.PHASE_FINALE) {
            return "Phase finale";
        }
        return "Inscriptions";
    }

    /** Classe de style de la pastille d'étape (définie dans theme.css). */
    static String classe(Etape e) {
        if (e == Etape.POINTAGE_OUVERT) {
            return "etape-pointage";
        }
        if (e == Etape.POINTAGE_CLOTURE) {
            return "etape-pret";
        }
        if (e == Etape.POULES) {
            return "etape-poules";
        }
        if (e == Etape.POULES_TERMINEES || e == Etape.PHASE_FINALE) {
            return "etape-terminee";
        }
        return "etape-inscription";
    }

    /**
     * Libellé du bouton principal d'un tableau sur la page Jour J : l'action
     * suivante du juge-arbitre.
     */
    static String libelleAction(Etape e) {
        if (e == Etape.INSCRIPTION) {
            return "Ouvrir le pointage";
        }
        if (e == Etape.POINTAGE_OUVERT) {
            return "Pointer";
        }
        if (e == Etape.POINTAGE_CLOTURE) {
            return "Lancer le tirage";
        }
        if (e == Etape.POULES_TERMINEES) {
            return "Construire le tableau final";
        }
        if (e == Etape.PHASE_FINALE) {
            return "Tableau final";
        }
        return "Poules";
    }

    /** « Tableau de 16 · 7 / 16 matchs joués · petite finale », ou « Tableau final terminé ». */
    static String resumeTableauFinal(Tableau t) {
        if (!t.aPhaseFinale()) {
            return resumePoules(t);
        }
        com.pingmanager.model.TableauElimination elim = t.getPhaseFinale();
        int total = 0;
        int joues = 0;
        for (com.pingmanager.model.Tour tour : elim.getTours()) {
            for (Match m : tour.getMatchs()) {
                total++;
                if (m.getStatut() == StatutMatch.TERMINE) {
                    joues++;
                }
            }
        }
        boolean pf = com.pingmanager.core.ServicePhaseFinale.petiteFinale(elim) != null;
        if (com.pingmanager.core.ServicePhaseFinale.estTerminee(elim)) {
            return "Tableau final terminé · " + joues + (joues > 1 ? " matchs joués" : " match joué");
        }
        return "Tableau de " + elim.getCadre() + " · " + joues + " / " + total
                + (total > 1 ? " matchs joués" : " match joué") + (pf ? " · petite finale" : "");
    }

    // ------------------------------------------------------------------
    // Liste des tableaux par jour
    // ------------------------------------------------------------------

    /**
     * Tableaux regroupés par jour (ordre chronologique), triés dans la journée par
     * heure de début puis par nom. Les tableaux sans horaire sont rangés à la fin,
     * sous la clé {@code null}.
     */
    static Map<LocalDate, List<Tableau>> tableauxParJour(Tournoi tournoi) {
        List<Tableau> tries = new ArrayList<>(tournoi.getTableaux());
        tries.sort(Comparator
                .comparing((Tableau t) -> t.getHoraires().getDebutMatchs(),
                        Comparator.nullsLast(Comparator.<LocalDateTime>naturalOrder()))
                .thenComparing(Tableau::getNom));
        Map<LocalDate, List<Tableau>> res = new LinkedHashMap<>();
        for (Tableau t : tries) {
            res.computeIfAbsent(jourDe(t), j -> new ArrayList<>()).add(t);
        }
        return res;
    }

    private static LocalDate jourDe(Tableau t) {
        LocalDateTime d = t.getHoraires().getDebutMatchs();
        return d == null ? null : d.toLocalDate();
    }

    /** « Samedi 14 novembre 2026 », ou « Sans horaire ». */
    static String jour(LocalDate d) {
        return d == null ? "Sans horaire" : Formats.capitale(Formats.jourLong(d));
    }

    /** « Pointage 8h – 8h40 · début 9h », selon ce qui est renseigné. */
    static String horaires(Tableau t) {
        LocalDateTime dp = t.getHoraires().getDebutPointage();
        LocalDateTime fp = t.getHoraires().getFinPointage();
        LocalDateTime dm = t.getHoraires().getDebutMatchs();
        List<String> morceaux = new ArrayList<>();
        if (dp != null && fp != null) {
            morceaux.add("Pointage " + Formats.heure(dp.toLocalTime()) + " – " + Formats.heure(fp.toLocalTime()));
        } else if (fp != null) {
            morceaux.add("Fin du pointage " + Formats.heure(fp.toLocalTime()));
        }
        if (dm != null) {
            morceaux.add("début " + Formats.heure(dm.toLocalTime()));
        }
        return morceaux.isEmpty() ? "Horaires non renseignés" : String.join(" · ", morceaux);
    }

    // ------------------------------------------------------------------
    // Pointage
    // ------------------------------------------------------------------

    /** Compteurs du pointage d'un tableau. */
    record Comptes(int inscrits, int places, int pointes, int payes, int attente, int nonPointes) {
    }

    static Comptes comptes(Tableau t) {
        int pointes = 0;
        int payes = 0;
        for (Joueur j : t.getInscrits()) {
            if (t.aPointe(j)) {
                pointes++;
            }
            if (t.aPaye(j)) {
                payes++;
            }
        }
        return new Comptes(t.getInscrits().size(), Math.max(0, t.getConfiguration().getPlacesMax()),
                pointes, payes, t.getFileAttente().size(), t.getInscrits().size() - pointes);
    }

    /** « 12 / 16 inscrits · 8 pointés · 6 payés · 3 en liste d'attente ». */
    static String resumePointage(Tableau t) {
        Comptes c = comptes(t);
        StringBuilder sb = new StringBuilder();
        sb.append(c.inscrits());
        if (c.places() > 0) {
            sb.append(" / ").append(c.places());
        }
        sb.append(c.inscrits() > 1 ? " inscrits" : " inscrit");
        sb.append(" · ").append(c.pointes()).append(c.pointes() > 1 ? " pointés" : " pointé");
        sb.append(" · ").append(c.payes()).append(c.payes() > 1 ? " payés" : " payé");
        if (c.attente() > 0) {
            sb.append(" · ").append(c.attente()).append(" en liste d'attente");
        }
        return sb.toString();
    }

    /** « 4 poules · 7 / 12 matchs joués ». */
    static String resumePoules(Tableau t) {
        int total = 0;
        int joues = 0;
        for (Poule p : t.getPoules()) {
            total += p.getMatchs().size();
            joues += SuiviPoule.matchsTermines(p);
        }
        int n = t.getPoules().size();
        return n + (n > 1 ? " poules" : " poule") + " · " + joues + " / " + total + " matchs joués";
    }

    /** Explication affichée avant la clôture du pointage. */
    static String texteCloture(Tableau t) {
        Comptes c = comptes(t);
        StringBuilder sb = new StringBuilder("À la clôture, ");
        if (c.nonPointes() == 0) {
            sb.append("aucune place n'est libérée : tous les inscrits ont pointé.");
        } else {
            sb.append(c.nonPointes()).append(c.nonPointes() > 1
                    ? " inscrits non pointés libèrent leur place" : " inscrit non pointé libère sa place");
            sb.append(c.attente() > 0
                    ? " ; la liste d'attente est repêchée dans l'ordre. Les repêchés devront pointer avant le lancement."
                    : ".");
        }
        return sb.toString();
    }

    /** Compte rendu de la clôture. */
    static String compteRenduCloture(ResultatPointage r) {
        String lib = r.nombreLiberes() == 0 ? "Aucune place libérée"
                : r.nombreLiberes() + (r.nombreLiberes() > 1 ? " places libérées" : " place libérée");
        String rep = r.nombreRepeches() == 0 ? "aucun repêché"
                : r.nombreRepeches() + (r.nombreRepeches() > 1 ? " repêchés à pointer" : " repêché à pointer");
        return "Pointage clôturé. " + lib + ", " + rep + ".";
    }

    /** Filtre une liste de joueurs par nom, prénom, licence ou club (sans accents ni casse). */
    static List<Joueur> filtrer(List<Joueur> joueurs, String recherche) {
        String r = normaliser(recherche);
        if (r.isEmpty()) {
            return new ArrayList<>(joueurs);
        }
        List<Joueur> res = new ArrayList<>();
        for (Joueur j : joueurs) {
            String cible = normaliser(j.getNom() + " " + j.getPrenom() + " " + j.getPrenom() + " " + j.getNom()
                    + " " + j.getNumeroLicence() + " " + (j.getClub() == null ? "" : j.getClub()));
            if (cible.contains(r)) {
                res.add(j);
            }
        }
        return res;
    }

    /** Tri alphabétique (nom puis prénom), comme sur une liste de pointage papier. */
    static List<Joueur> parNom(List<Joueur> joueurs) {
        List<Joueur> res = new ArrayList<>(joueurs);
        res.sort(Comparator.comparing((Joueur j) -> normaliser(j.getNom()))
                .thenComparing(j -> normaliser(j.getPrenom())));
        return res;
    }

    private static String normaliser(String s) {
        if (s == null) {
            return "";
        }
        String sans = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sans.toLowerCase(Locale.FRENCH).trim().replaceAll("\\s+", " ");
    }

    // ------------------------------------------------------------------
    // Lancement et tirage
    // ------------------------------------------------------------------

    /** Phrase expliquant pourquoi le tirage ne peut pas encore être fait, ou qu'il est prêt. */
    static String motifLancement(ResultatVerificationLancement r) {
        ResultatVerificationLancement.Motif m = r.getMotif();
        if (m == ResultatVerificationLancement.Motif.PRET) {
            if (!r.getNonPointes().isEmpty()) {
                int n = r.getNonPointes().size();
                return n + (n > 1 ? " repêchés n'ont pas encore pointé" : " repêché n'a pas encore pointé")
                        + " : confirmez leur présence ou retirez-les avant de lancer le tirage.";
            }
            return r.getPresents() + " présents : le tirage au sort des poules peut être fait.";
        }
        if (m == ResultatVerificationLancement.Motif.POINTAGE_NON_OUVERT) {
            return "Le pointage de ce tableau n'est pas encore ouvert.";
        }
        if (m == ResultatVerificationLancement.Motif.POINTAGE_NON_CLOTURE) {
            return "Clôturez d'abord le pointage : la liste des présents doit être figée avant le tirage.";
        }
        if (m == ResultatVerificationLancement.Motif.DEJA_LANCE) {
            return "Ce tableau est déjà lancé.";
        }
        if (m == ResultatVerificationLancement.Motif.SANS_FORMAT) {
            return "Ce tableau n'a pas de format de compétition : choisissez-en un dans « Tableaux et inscriptions ».";
        }
        if (m == ResultatVerificationLancement.Motif.PAS_ASSEZ_DE_PRESENTS) {
            return "Pas assez de présents (" + r.getPresents() + ") pour former une poule de " + r.getMinimum()
                    + ". Repêchez des joueurs ou regroupez ce tableau avec un autre.";
        }
        if (m == ResultatVerificationLancement.Motif.TOURNOI_FERME) {
            return "Le tournoi est annulé : aucun tableau ne peut être lancé.";
        }
        return "Ce tableau n'appartient pas à ce tournoi.";
    }

    /**
     * @return true si l'écran autorise le tirage : le cœur l'accepte et aucun repêché
     *         ne reste à pointer (choix prudent, voir QUESTIONS.md).
     */
    static boolean tiragePossible(ResultatVerificationLancement r) {
        return r.estPret() && r.getNonPointes().isEmpty();
    }

    /** Graine courte (6 chiffres), facile à annoncer et à noter au tableau d'affichage. */
    static long nouvelleGraine(Random hasard) {
        return 100_000 + hasard.nextInt(900_000);
    }

    /** @return la graine saisie, ou vide si le texte n'est pas un nombre entier positif. */
    static Optional<Long> lireGraine(String texte) {
        if (texte == null) {
            return Optional.empty();
        }
        String t = texte.trim().replace(" ", "").replace(" ", "").replace(" ", "");
        if (!t.matches("\\d{1,18}")) {
            return Optional.empty();
        }
        return Optional.of(Long.parseLong(t));
    }

    // ------------------------------------------------------------------
    // Poules : saisie des scores, issues, classement
    // ------------------------------------------------------------------

    /** Lecture des champs de manches d'un match. */
    record LectureManches(List<Manche> manches, int premiereIllisible, int premiereInvalide) {
        /** @return true si toutes les manches saisies sont lisibles et possibles (2.11.1). */
        boolean estLisible() {
            return premiereIllisible == 0 && premiereInvalide == 0;
        }
    }

    /**
     * Lit les champs de manches dans l'ordre ; les champs vides en fin de liste sont
     * ignorés (match gagné avant la dernière manche possible). Un champ vide au
     * milieu est signalé comme illisible.
     *
     * @return les manches lues ; numéros (à partir de 1) de la première manche
     *         illisible et de la première manche impossible (0 si aucune)
     */
    static LectureManches lireManches(List<String> saisies) {
        int dernier = -1;
        for (int i = 0; i < saisies.size(); i++) {
            String s = saisies.get(i);
            if (s != null && !s.isBlank()) {
                dernier = i;
            }
        }
        List<Manche> manches = new ArrayList<>();
        int illisible = 0;
        int invalide = 0;
        for (int i = 0; i <= dernier; i++) {
            Optional<Manche> m = NotationManche.lire(saisies.get(i));
            if (m.isEmpty()) {
                if (illisible == 0) {
                    illisible = i + 1;
                }
                continue;
            }
            if (!SaisieResultatMatch.mancheValide(m.get()) && invalide == 0) {
                invalide = i + 1;
            }
            manches.add(m.get());
        }
        return new LectureManches(manches, illisible, invalide);
    }

    /** Manches gagnées par A et par B (« 3-1 ») sur une liste de manches. */
    static String manchesGagnees(List<Manche> manches) {
        int a = 0;
        int b = 0;
        for (Manche m : manches) {
            if (m.gagneeParA()) {
                a++;
            } else if (m.gagneeParB()) {
                b++;
            }
        }
        return a + "-" + b;
    }

    /** Message affiché pour un refus de saisie. */
    static String motifSaisie(ResultatSaisieMatch r) {
        ResultatSaisieMatch.Motif m = r.getMotif();
        if (m == ResultatSaisieMatch.Motif.ENREGISTRE) {
            return "Résultat enregistré.";
        }
        if (m == ResultatSaisieMatch.Motif.AUCUNE_MANCHE) {
            return "Saisissez au moins une manche (par exemple 11-9, ou 9 ; -9 si le second joueur gagne).";
        }
        if (m == ResultatSaisieMatch.Motif.MANCHE_INVALIDE) {
            return "Manche " + r.getNumeroManche() + " impossible : une manche se gagne à 11 points, "
                    + "ou avec 2 points d'écart après 10-10 (article 2.11.1).";
        }
        if (m == ResultatSaisieMatch.Motif.TROP_DE_MANCHES) {
            return "Trop de manches pour ce match.";
        }
        if (m == ResultatSaisieMatch.Motif.MANCHE_APRES_DECISION) {
            return "Le match est déjà gagné avant la manche " + r.getNumeroManche() + " : retirez les manches en trop.";
        }
        if (m == ResultatSaisieMatch.Motif.MATCH_NON_DECIDE) {
            return "Aucun joueur n'a encore gagné le nombre de manches nécessaire : complétez le score.";
        }
        if (m == ResultatSaisieMatch.Motif.POULES_NON_MODIFIABLES) {
            return "Les poules de ce tableau ne sont plus modifiables.";
        }
        if (m == ResultatSaisieMatch.Motif.MATCH_EXEMPT) {
            return "Ce match est une exemption : il n'a pas de score.";
        }
        if (m == ResultatSaisieMatch.Motif.ISSUE_NORMALE) {
            return "Choisissez le type d'issue (forfait, abandon, refus de jouer, disqualification).";
        }
        if (m == ResultatSaisieMatch.Motif.FAUTIF_HORS_MATCH) {
            return "Indiquez lequel des deux joueurs est concerné.";
        }
        return "Ce match n'appartient pas à ce tableau.";
    }

    /** Issues non normales proposées à la saisie, dans l'ordre de l'article I.303 a). */
    static final List<IssueMatch> ISSUES_NON_NORMALES = List.of(
            IssueMatch.FORFAIT, IssueMatch.ABANDON, IssueMatch.REFUS_DE_JOUER, IssueMatch.DISQUALIFICATION);

    static String libelleIssue(IssueMatch i) {
        if (i == IssueMatch.FORFAIT) {
            return "Forfait (absent à l'appel)";
        }
        if (i == IssueMatch.ABANDON) {
            return "Abandon";
        }
        if (i == IssueMatch.REFUS_DE_JOUER) {
            return "Refus de jouer";
        }
        if (i == IssueMatch.DISQUALIFICATION) {
            return "Disqualification";
        }
        return "Match joué";
    }

    /**
     * Résultat d'un match tel qu'affiché dans la liste : « 3-1 (11-9, 8-11, 11-5,
     * 12-10) », « Forfait de DUPONT Jean », ou vide s'il n'est pas joué.
     */
    static String resultat(Match m) {
        if (m.getStatut() != StatutMatch.TERMINE || m.estBye()) {
            return "";
        }
        if (m.getIssue() != IssueMatch.NORMALE && m.getJoueurFautif() != null) {
            String quoi = m.getIssue() == IssueMatch.FORFAIT ? "Forfait"
                    : m.getIssue() == IssueMatch.ABANDON ? "Abandon"
                    : m.getIssue() == IssueMatch.REFUS_DE_JOUER ? "Refus de jouer" : "Disqualification";
            return quoi + " de " + m.getJoueurFautif().getNomComplet();
        }
        List<String> detail = new ArrayList<>();
        for (Manche manche : m.getManches()) {
            detail.add(NotationManche.ecrire(manche));
        }
        return manchesGagnees(m.getManches()) + " (" + String.join(", ", detail) + ")";
    }

    /** Pastille de situation dans le classement en direct. */
    static String libelleQualification(SuiviPoule.Qualification q) {
        if (q == SuiviPoule.Qualification.QUALIFIE) {
            return "Qualifié";
        }
        if (q == SuiviPoule.Qualification.PLACE_QUALIFICATIVE) {
            return "Place qualificative";
        }
        if (q == SuiviPoule.Qualification.NON_QUALIFIE) {
            return "Éliminé";
        }
        return "";
    }

    /** « 1er », « 2e »… */
    static String rang(int r) {
        return r == 1 ? "1er" : r + "e";
    }

    /** « 1 » point de classement, « 4 » points… avec l'unité. */
    static String points(int p) {
        return p + (p > 1 ? " pts" : " pt");
    }

    /** Points de licence et club d'un joueur : « 1 245 pts · Brunoy ». */
    static String detailJoueur(Joueur j) {
        String club = j.getClub() == null || j.getClub().isBlank() ? "club non renseigné" : j.getClub();
        return j.getPoints() + " pts · " + club;
    }
}
