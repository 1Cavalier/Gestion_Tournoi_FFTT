package com.pingmanager.ui.diffusion;

import com.pingmanager.core.DiagnosticPhaseFinale;
import com.pingmanager.core.Horloge;
import com.pingmanager.core.PodiumPhaseFinale;
import com.pingmanager.core.ServicePhaseFinale;
import com.pingmanager.core.ServiceSalle;
import com.pingmanager.core.SuiviPoule;
import com.pingmanager.core.SuiviTournoi;
import com.pingmanager.core.TableauDeBordSalle;
import com.pingmanager.model.IssueMatch;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.LigneClassement;
import com.pingmanager.model.Manche;
import com.pingmanager.model.Match;
import com.pingmanager.model.Poule;
import com.pingmanager.model.StatutMatch;
import com.pingmanager.model.StatutTableau;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.TableauElimination;
import com.pingmanager.model.Tour;
import com.pingmanager.model.Tournoi;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Photographie <b>immuable</b> du tournoi pour le suivi sur téléphone (B021).
 *
 * <p>Construite sur le fil JavaFX à partir du tournoi (objets mutables) par
 * {@link #depuis(Tournoi, Horloge)}, puis lue par les fils du serveur web sans
 * jamais toucher au tournoi : il n'y a donc aucune lecture concurrente d'objets
 * mutables. Elle ne contient que des textes déjà mis en forme et, pour chaque
 * joueur, <b>le nom, le club et les points</b> ; le numéro de licence n'y est
 * gardé que pour la recherche « Ma prochaine partie » et n'est jamais affiché.
 * Ni téléphone, ni date de naissance, ni courriel.</p>
 *
 * <p>Les textes reprennent ceux du suivi dans l'application
 * ({@code ui.ecran.PresentationSuiviTournoi}) ; ils sont recopiés ici pour que ce
 * package ne dépende pas des écrans JavaFX.</p>
 */
public record InstantaneSuivi(String nomTournoi, LocalDateTime prisLe, List<TableauPublic> tableaux,
                              List<MatchPublic> enCours, List<MatchPublic> aVenir, List<JoueurPublic> joueurs) {

    /** Nombre maximal de matchs « à venir » photographiés (file d'attente des tables). */
    public static final int MAX_A_VENIR = 30;

    public InstantaneSuivi {
        nomTournoi = nomTournoi == null ? "" : nomTournoi;
        Objects.requireNonNull(prisLe, "prisLe");
        tableaux = List.copyOf(tableaux);
        enCours = List.copyOf(enCours);
        aVenir = List.copyOf(aVenir);
        joueurs = List.copyOf(joueurs);
    }

    /** Un tableau : étape, poules, tours du tableau final et podium. */
    public record TableauPublic(String nom, String etape, String note, List<PoulePublique> poules,
                                List<TourPublic> tours, List<LignePodium> podium) {
        public TableauPublic {
            poules = List.copyOf(poules);
            tours = List.copyOf(tours);
            podium = List.copyOf(podium);
        }
    }

    /** Une poule : titre (« Poule B · 2 / 3 matchs joués »), classement en direct, matchs. */
    public record PoulePublique(String titre, List<LigneClassementPublique> classement, List<MatchPublic> matchs) {
        public PoulePublique {
            classement = List.copyOf(classement);
            matchs = List.copyOf(matchs);
        }
    }

    /** Une ligne du classement d'une poule (I.303). */
    public record LigneClassementPublique(String rang, String joueur, String detail, String bilan,
                                          String qualification, boolean enPlaceQualificative) {
    }

    /** Un tour du tableau final (« Quarts de finale », « Petite finale »…). */
    public record TourPublic(String nom, List<MatchPublic> matchs) {
        public TourPublic {
            matchs = List.copyOf(matchs);
        }
    }

    /** Une place du podium. */
    public record LignePodium(String place, String joueur, String detail) {
    }

    /**
     * Un match vu du public : {@code resultat} court (« 3-1 », « Forfait »,
     * « À jouer »), {@code detail} des manches, {@code table} (« Table 3 ») si
     * attribuée.
     */
    public record MatchPublic(String tableau, String situation, String joueurA, String detailA, String joueurB,
                              String detailB, String resultat, String detail, String table, boolean enCours,
                              boolean vainqueurA, boolean vainqueurB) {
    }

    /** La partie mise en avant pour un joueur. */
    public record ProchainePartie(String titre, String adversaire, String detail, String table, String quand,
                                  boolean enCours) {
    }

    /**
     * Un joueur inscrit : nom affiché, club et points. {@code licence} et
     * {@code nomRecherche} ne servent qu'à la recherche (jamais affichés).
     */
    public record JoueurPublic(String licence, String nomRecherche, String nom, String detail,
                               ProchainePartie prochaine, String sansPartie) {
        /** @return la prochaine partie du joueur, s'il en a une. */
        public Optional<ProchainePartie> prochainePartie() {
            return Optional.ofNullable(prochaine);
        }
    }

    /** Instantané vide (aucun tournoi publié pour l'instant). */
    public static InstantaneSuivi vide(LocalDateTime prisLe) {
        return new InstantaneSuivi("", prisLe, List.of(), List.of(), List.of(), List.of());
    }

    // ==================================================================
    // Construction (fil JavaFX uniquement)
    // ==================================================================

    /**
     * Photographie le tournoi. À appeler sur le fil qui possède le tournoi (fil
     * JavaFX) : le tournoi n'est que lu, jamais modifié.
     */
    public static InstantaneSuivi depuis(Tournoi tournoi, Horloge horloge) {
        Objects.requireNonNull(tournoi, "tournoi");
        ServiceSalle salle = new ServiceSalle(Objects.requireNonNull(horloge, "horloge"));
        LocalDateTime maintenant = salle.maintenant();
        TableauDeBordSalle bord = salle.tableauDeBord(tournoi);

        List<Tableau> ordonnes = new ArrayList<>(tournoi.getTableaux());
        ordonnes.sort(SuiviTournoi.ORDRE_DE_LA_JOURNEE);

        List<TableauPublic> tableaux = new ArrayList<>();
        List<MatchPublic> enCours = new ArrayList<>();
        for (Tableau t : ordonnes) {
            tableaux.add(tableau(t));
            for (SuiviTournoi.Partie p : SuiviTournoi.matchsEnCours(t)) {
                enCours.add(match(t.getNom(), situation(p), p.match()));
            }
        }
        enCours.sort(Comparator.comparing(m -> numeroTable(m.table())));

        // File d'attente des tables : matchs prêts, dans l'ordre de l'ordonnanceur.
        Map<Match, Integer> rangs = new IdentityHashMap<>();
        List<MatchPublic> aVenir = new ArrayList<>();
        int rang = 0;
        for (TableauDeBordSalle.MatchSalle ms : bord.getPrets()) {
            rang++;
            rangs.put(ms.match(), rang);
            if (aVenir.size() < MAX_A_VENIR) {
                aVenir.add(match(ms.tableau(), ms.tour(), ms.match()));
            }
        }
        Map<Match, Boolean> bloques = new IdentityHashMap<>();
        for (TableauDeBordSalle.MatchBloque mb : bord.getBloques()) {
            bloques.put(mb.match().match(), Boolean.TRUE);
        }

        return new InstantaneSuivi(tournoi.getNom(), maintenant, tableaux, enCours, aVenir,
                joueurs(tournoi, ordonnes, rangs, bloques));
    }

    private static int numeroTable(String table) {
        try {
            return table.startsWith("Table ") ? Integer.parseInt(table.substring(6)) : Integer.MAX_VALUE;
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }

    private static TableauPublic tableau(Tableau t) {
        List<PoulePublique> poules = new ArrayList<>();
        List<TourPublic> tours = new ArrayList<>();
        List<LignePodium> podium = new ArrayList<>();
        String note = "";
        if (!SuiviTournoi.estCommence(t)) {
            note = t.getStatut() == StatutTableau.POINTAGE
                    ? "Le pointage de ce tableau est en cours : les poules s'afficheront dès le tirage."
                    : "Ce tableau n'a pas encore commencé : les poules s'afficheront dès le tirage.";
            return new TableauPublic(t.getNom(), etape(t), note, poules, tours, podium);
        }
        int qualifies = t.getConfiguration().getQualifiesParPoule();
        SuiviPoule suivi = new SuiviPoule();
        for (Poule p : t.getPoules()) {
            List<LigneClassementPublique> classement = new ArrayList<>();
            for (SuiviPoule.Ligne l : suivi.classementEnDirect(p, qualifies)) {
                LigneClassement c = l.classement();
                classement.add(new LigneClassementPublique(c.getRang() + (c.getRang() == 1 ? "er" : "e"),
                        nomJoueur(c.getJoueur()), detailJoueur(c.getJoueur()),
                        c.getMatchsGagnes() + (c.getMatchsGagnes() > 1 ? " victoires · " : " victoire · ")
                                + c.getPointsClassement() + " pts",
                        libelleQualification(l.qualification()), l.enPlaceQualificative()));
            }
            List<MatchPublic> matchs = new ArrayList<>();
            for (int i = 0; i < p.getMatchs().size(); i++) {
                matchs.add(match(t.getNom(), "Tour " + SuiviPoule.tourDuMatch(p, i), p.getMatchs().get(i)));
            }
            poules.add(new PoulePublique(titrePoule(p), classement, matchs));
        }
        if (t.aPhaseFinale()) {
            TableauElimination e = t.getPhaseFinale();
            for (Tour tour : e.getTours()) {
                List<MatchPublic> matchs = new ArrayList<>();
                for (Match m : tour.getMatchs()) {
                    if (m.getJoueurA() == null && m.getJoueurB() == null) {
                        continue;
                    }
                    matchs.add(match(t.getNom(), libelleTour(e, tour), m));
                }
                tours.add(new TourPublic(libelleTour(e, tour), matchs));
            }
            if (ServicePhaseFinale.estTerminee(e)) {
                podium.addAll(podium(ServicePhaseFinale.podium(e)));
            }
        } else if (t.getStatut() == StatutTableau.LANCE && t.poulesTerminees()) {
            note = sansTableauFinal(t) ? "Ce tableau se termine à l'issue des poules : pas de tableau final."
                    : "Poules terminées : le juge-arbitre va tirer le tableau final.";
        } else {
            note = "Le tableau final sera tiré à la fin des poules.";
        }
        return new TableauPublic(t.getNom(), etape(t), note, poules, tours, podium);
    }

    private static List<LignePodium> podium(PodiumPhaseFinale p) {
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

    /** Joueurs inscrits (hors liste d'attente), triés par nom, avec leur prochaine partie. */
    private static List<JoueurPublic> joueurs(Tournoi tournoi, List<Tableau> ordonnes, Map<Match, Integer> rangs,
                                              Map<Match, Boolean> bloques) {
        Map<String, Joueur> parLicence = new LinkedHashMap<>();
        Map<String, Boolean> commence = new HashMap<>();
        for (Tableau t : ordonnes) {
            for (Joueur j : t.getInscrits()) {
                if (j == null || j.estADeterminer() || j.getNumeroLicence() == null) {
                    continue;
                }
                parLicence.putIfAbsent(j.getNumeroLicence(), j);
                if (SuiviTournoi.estCommence(t)) {
                    commence.put(j.getNumeroLicence(), Boolean.TRUE);
                }
            }
        }
        List<JoueurPublic> res = new ArrayList<>();
        for (Joueur j : parLicence.values()) {
            String licence = j.getNumeroLicence();
            ProchainePartie prochaine = SuiviTournoi.prochainePartie(tournoi, licence)
                    .map(p -> prochaine(p, licence, rangs, bloques)).orElse(null);
            String sansPartie = prochaine != null ? ""
                    : commence.containsKey(licence)
                    ? "Aucune partie à jouer pour l'instant : suivez vos tableaux."
                    : "Vos tableaux n'ont pas encore commencé.";
            res.add(new JoueurPublic(licence, normaliser(nomJoueur(j)), nomJoueur(j), detailJoueur(j),
                    prochaine, sansPartie));
        }
        res.sort(Comparator.comparing(JoueurPublic::nomRecherche).thenComparing(JoueurPublic::licence));
        return res;
    }

    private static ProchainePartie prochaine(SuiviTournoi.Partie partie, String licence, Map<Match, Integer> rangs,
                                             Map<Match, Boolean> bloques) {
        Joueur adv = partie.adversaire(licence);
        String adversaire = adv == null ? "Adversaire à déterminer" : "Contre " + nomJoueur(adv);
        if (adv != null) {
            String d = detailJoueur(adv);
            adversaire += d.isEmpty() ? "" : " (" + d + ")";
        }
        String detail = partie.tableau().getNom() + " · " + situation(partie);
        String table = table(partie.match());
        String quand;
        if (partie.estEnCours()) {
            quand = "En ce moment";
        } else if (rangs.containsKey(partie.match())) {
            int r = rangs.get(partie.match());
            quand = r == 1 ? "Prochain match envoyé sur une table libre"
                    : r + "e dans la file d'attente des tables";
        } else if (bloques.containsKey(partie.match())) {
            quand = "Après la partie en cours d'un des deux joueurs";
        } else {
            quand = "Plus tard : l'adversaire ou le tour n'est pas encore prêt";
        }
        return new ProchainePartie(partie.estEnCours() ? "Vous jouez en ce moment" : "Votre prochaine partie",
                adversaire, detail, table.isEmpty() ? "Table pas encore attribuée" : table, quand,
                partie.estEnCours());
    }

    // ==================================================================
    // Textes (repris de ui.ecran.PresentationSuiviTournoi / PresentationPhaseFinale)
    // ==================================================================

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
            int n = SuiviTournoi.poulesTerminees(t);
            return "Poules : " + n + " sur " + t.getPoules().size() + (n > 1 ? " jouées" : " jouée");
        }
        if (s == StatutTableau.POINTAGE) {
            return "Pointage";
        }
        return "Inscriptions";
    }

    private static boolean sansTableauFinal(Tableau t) {
        return new ServicePhaseFinale().diagnostiquer(t).getEtat() == DiagnosticPhaseFinale.Etat.SANS_ELIMINATION;
    }

    static String libelleTour(TableauElimination elimination, Tour tour) {
        if (ServicePhaseFinale.estPetiteFinale(tour)) {
            return "Petite finale";
        }
        List<Tour> principaux = ServicePhaseFinale.toursPrincipaux(elimination);
        int i = principaux.indexOf(tour);
        if (i < 0) {
            return tour.getNom();
        }
        int rang = principaux.size() - 1 - i;
        if (rang == 0) {
            return "Finale";
        }
        if (rang == 1) {
            return "Demi-finales";
        }
        if (rang == 2) {
            return "Quarts de finale";
        }
        return "1/" + (1L << rang) + " de finale";
    }

    private static String situation(SuiviTournoi.Partie p) {
        if (p.estEnPoule()) {
            return "Poule " + p.poule().getNom() + ", tour " + p.tourDePoule();
        }
        return libelleTour(p.tableau().getPhaseFinale(), p.tour());
    }

    private static String titrePoule(Poule p) {
        if (p.estTerminee()) {
            return "Poule " + p.getNom() + " · terminée";
        }
        int n = SuiviPoule.matchsTermines(p);
        return "Poule " + p.getNom() + " · " + n + " / " + p.getMatchs().size()
                + (n > 1 ? " matchs joués" : " match joué");
    }

    private static String libelleQualification(SuiviPoule.Qualification q) {
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

    private static MatchPublic match(String tableau, String situation, Match m) {
        boolean enCours = m.getStatut() == StatutMatch.EN_COURS;
        String resultat;
        if (m.estBye()) {
            resultat = "Exempt";
        } else if (m.getStatut() == StatutMatch.TERMINE) {
            resultat = m.getIssue() != IssueMatch.NORMALE ? libelleIssue(m.getIssue())
                    : m.manchesGagneesParA() + "-" + m.manchesGagneesParB();
        } else if (enCours) {
            resultat = "En cours";
        } else if (estConnu(m.getJoueurA()) && estConnu(m.getJoueurB())) {
            resultat = "À jouer";
        } else {
            resultat = "En attente des joueurs";
        }
        return new MatchPublic(tableau, situation, nomJoueur(m.getJoueurA()), detailJoueur(m.getJoueurA()),
                m.estBye() ? "Exempt" : nomJoueur(m.getJoueurB()), m.estBye() ? "" : detailJoueur(m.getJoueurB()),
                resultat, detailScore(m), enCours ? table(m) : "", enCours,
                estVainqueur(m, true), estVainqueur(m, false));
    }

    private static String table(Match m) {
        return m.getStatut() == StatutMatch.EN_COURS && m.getNumeroTable() != null
                ? "Table " + m.getNumeroTable() : "";
    }

    private static String detailScore(Match m) {
        if (m.getStatut() != StatutMatch.TERMINE || m.estBye()) {
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

    private static boolean estVainqueur(Match m, boolean joueurA) {
        if (m.getStatut() != StatutMatch.TERMINE || m.estBye()) {
            return false;
        }
        Joueur v = m.getVainqueur();
        return v != null && v.equals(joueurA ? m.getJoueurA() : m.getJoueurB());
    }

    private static String libelleIssue(IssueMatch issue) {
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

    private static boolean estConnu(Joueur j) {
        return j != null && !j.estADeterminer();
    }

    /** « Prénom NOM », ou « À déterminer ». Seuls le nom et le prénom sont repris. */
    static String nomJoueur(Joueur j) {
        if (!estConnu(j)) {
            return "À déterminer";
        }
        String prenom = j.getPrenom() == null ? "" : j.getPrenom().trim();
        String nom = j.getNom() == null ? "" : j.getNom().trim().toUpperCase(Locale.FRENCH);
        return (prenom + " " + nom).trim();
    }

    /** « Club · 1520 pts » : seuls le club et les points sont repris. */
    static String detailJoueur(Joueur j) {
        if (!estConnu(j)) {
            return "";
        }
        String club = j.getClub() == null ? "" : j.getClub().trim();
        String pts = j.getPoints() + " pts";
        return club.isEmpty() ? pts : club + " · " + pts;
    }

    /** Minuscules sans accents ni espaces multiples, pour la recherche. */
    static String normaliser(String s) {
        if (s == null) {
            return "";
        }
        String sansAccents = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sansAccents.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }
}
