package com.pingmanager.ui.ecran;

import com.pingmanager.model.FicheLicence;
import com.pingmanager.model.Sexe;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Contenu de l'accueil (« home », cahier des charges v0, section 1), sans
 * JavaFX : cartes pour tous (Profil, Tournois, Agenda, Statistiques), puis la
 * section « Gestion de votre club », présente <b>uniquement</b> pour un compte
 * relié à au moins un club. Chaque carte porte une phrase d'explication et la
 * clé de la rubrique de la {@link Coquille} où elle mène.
 */
public final class PresentationAccueil {

    /** Clés des rubriques de la coquille (mêmes valeurs que dans {@link Coquille}). */
    public static final String PROFIL = "profil";
    public static final String TOURNOIS = "tournois";
    public static final String AGENDA = "agenda";
    public static final String STATS = "stats";
    public static final String GESTION = "gestion";
    public static final String HISTORIQUE = "historique";
    public static final String BUVETTE = "buvette";

    /** Nombre de dates montrées sur la carte Agenda. */
    static final int DATES_MAX = 3;

    private static final DateTimeFormatter DATE_COURTE = DateTimeFormatter.ofPattern("EEE d MMM", Locale.FRENCH);

    /** Une ligne « libellé : valeur » d'une carte (identité du profil, date de l'agenda). */
    public record Champ(String libelle, String valeur) {
    }

    /** Une date à venir du joueur (tournoi où il est inscrit ou en liste d'attente). */
    public record DateAgenda(LocalDate date, String libelle) {
    }

    /**
     * Une carte de l'accueil.
     *
     * @param titre       titre de la carte
     * @param explication une phrase : à quoi sert la section
     * @param action      libellé du bouton qui y mène
     * @param destination clé de la rubrique de la coquille
     * @param aVenir      section pas encore livrée (pastille « Bientôt »)
     * @param champs      lignes de détail (peut être vide)
     */
    public record Carte(String titre, String explication, String action, String destination,
                        boolean aVenir, List<Champ> champs) {
        public Carte {
            champs = List.copyOf(champs);
        }
    }

    private final String salutation;
    private final List<Carte> cartesPourTous;
    private final List<Carte> cartesClub;
    private final String introClub;

    private PresentationAccueil(String salutation, List<Carte> pourTous, List<Carte> club, String introClub) {
        this.salutation = salutation;
        this.cartesPourTous = List.copyOf(pourTous);
        this.cartesClub = List.copyOf(club);
        this.introClub = introClub;
    }

    /**
     * @param prenomCompte prénom du compte connecté
     * @param nomCompte    nom du compte connecté
     * @param fiche        fiche de licence liée au compte, si elle existe
     * @param nomsClubs    clubs auxquels le compte est relié (vide pour un joueur)
     * @param dates        prochaines dates du joueur (ordre quelconque)
     */
    public static PresentationAccueil pour(String prenomCompte, String nomCompte, Optional<FicheLicence> fiche,
                                           List<String> nomsClubs, List<DateAgenda> dates) {
        String prenom = fiche.map(FicheLicence::getPrenom).filter(s -> !s.isBlank()).orElse(texte(prenomCompte));
        String nom = fiche.map(FicheLicence::getNom).filter(s -> !s.isBlank()).orElse(texte(nomCompte));

        List<Carte> tous = new ArrayList<>();
        tous.add(carteProfil(prenom, nom, fiche));
        tous.add(new Carte("Tournois",
                "Cherchez un tournoi sur la carte interactive, consultez sa fiche et inscrivez-vous en ligne.",
                "Voir la carte des tournois", TOURNOIS, false, List.of()));
        tous.add(carteAgenda(dates));
        tous.add(new Carte("Statistiques",
                "Bientôt : vos rencontres recensées et vos statistiques de joueur (résultats, progression).",
                "Voir les statistiques", STATS, true, List.of()));

        List<Carte> club = new ArrayList<>();
        String intro = "";
        List<String> clubs = nomsClubs.stream().filter(Objects::nonNull).filter(s -> !s.isBlank()).toList();
        if (!clubs.isEmpty()) {
            intro = (clubs.size() > 1 ? "Vous êtes relié aux clubs " : "Vous êtes relié au club ")
                    + String.join(", ", clubs) + " : vos outils d'organisateur sont réunis ici.";
            club.add(new Carte("Gestion des tournois",
                    "Créez les tournois du club, préparez-les jusqu'à l'homologation et la publication, "
                            + "puis menez le jour J.",
                    "Gérer les tournois", GESTION, false, List.of()));
            club.add(new Carte("Historique des tournois",
                    "Bientôt : le bilan des tournois passés du club (joueurs, présents, recettes).",
                    "Voir l'historique", HISTORIQUE, true, List.of()));
            club.add(new Carte("Buvette",
                    "Bientôt : stocks, prix et prévisionnel de la buvette pendant le tournoi.",
                    "Voir la buvette", BUVETTE, true, List.of()));
        }
        return new PresentationAccueil("Bonjour " + prenom, tous, club, intro);
    }

    private static Carte carteProfil(String prenom, String nom, Optional<FicheLicence> fiche) {
        List<Champ> champs = new ArrayList<>();
        champs.add(new Champ("Nom", nom.isBlank() ? "—" : nom));
        champs.add(new Champ("Prénom", prenom.isBlank() ? "—" : prenom));
        champs.add(new Champ("Points", fiche.map(f -> String.valueOf(f.getPoints())).orElse("—")));
        champs.add(new Champ("Sexe", fiche.map(f -> libelleSexe(f.getSexe())).orElse("—")));
        String explication = fiche.isPresent()
                ? "Vous retrouverez toutes vos informations personnelles (licence, club, coordonnées) dans « Mon profil »."
                : "Liez votre licence dans « Mon profil » pour afficher vos points et vous inscrire aux tournois.";
        return new Carte("Profil", explication, "Ouvrir mon profil", PROFIL, false, champs);
    }

    private static Carte carteAgenda(List<DateAgenda> dates) {
        List<DateAgenda> tries = new ArrayList<>(dates);
        tries.removeIf(d -> d == null || d.date() == null);
        tries.sort(Comparator.comparing(DateAgenda::date));
        List<Champ> champs = new ArrayList<>();
        for (DateAgenda d : tries.subList(0, Math.min(DATES_MAX, tries.size()))) {
            champs.add(new Champ(capitaliser(DATE_COURTE.format(d.date())), texte(d.libelle())));
        }
        return new Carte("Agenda",
                "Bientôt : ajoutez vos dates, retrouvez-y automatiquement les tournois où vous êtes inscrit "
                        + "et exportez-les vers votre agenda (Google Agenda…).",
                "Ouvrir l'agenda", AGENDA, true, champs);
    }

    static String libelleSexe(Sexe s) {
        if (s == Sexe.FEMME) {
            return "Femme";
        }
        if (s == Sexe.HOMME) {
            return "Homme";
        }
        return "—";
    }

    private static String texte(String s) {
        return s == null ? "" : s;
    }

    private static String capitaliser(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    public String salutation() {
        return salutation;
    }

    /** Cartes visibles de tous, dans l'ordre d'affichage. */
    public List<Carte> cartesPourTous() {
        return cartesPourTous;
    }

    /** La section « Gestion de votre club » n'existe que pour un compte relié à un club. */
    public boolean sectionClubVisible() {
        return !cartesClub.isEmpty();
    }

    public String titreSectionClub() {
        return "Gestion de votre club";
    }

    public String introSectionClub() {
        return introClub;
    }

    /** Cartes de la section club (vide pour un compte joueur). */
    public List<Carte> cartesClub() {
        return cartesClub;
    }

    /** Mention de fin de section : d'autres outils viendront. */
    public String mentionAutresOutils() {
        return "D'autres outils pour le club arriveront ici.";
    }
}
