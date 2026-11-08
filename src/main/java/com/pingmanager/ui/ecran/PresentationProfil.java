package com.pingmanager.ui.ecran;

import com.pingmanager.model.AccesClub;
import com.pingmanager.model.FicheLicence;
import com.pingmanager.model.Sexe;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Contenu de « Mon profil » (cahier des charges v0, section 2), sans JavaFX.
 *
 * <p>Deux blocs : en haut, pour tout le monde, l'identité du compte et la
 * licence FFTT, puis les sections « Tournoi » et « Appel micro » ; en bas, le
 * bloc « Club » : liaison du compte à un club (avertissement et confirmation
 * explicite <b>avant</b> toute demande de code) et, pour chaque club relié, le
 * tableau des droits du compte.</p>
 *
 * <p>Les droits sont la traduction lisible des règles déjà portées par le
 * modèle ({@link com.pingmanager.model.NiveauAcces}, {@link AccesClub#estPrincipal()})
 * et par {@code ContexteApplication.dissocierClub} : cette classe n'en invente
 * aucune.</p>
 */
public final class PresentationProfil {

    private static final DateTimeFormatter DATE_FR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Valeur affichée pour une information absente. */
    static final String NON_RENSEIGNE = "Non renseigné";

    /** Une ligne « libellé : valeur ». */
    public record Champ(String libelle, String valeur) {
    }

    /** État d'un droit dans le tableau : accordé, refusé ou pas encore livré. */
    public enum EtatDroit {
        OUI("Oui", StatutPastille.SUCCES),
        NON("Non", StatutPastille.NEUTRE),
        A_VENIR("À venir", StatutPastille.INFO);

        private final String libelle;
        private final StatutPastille pastille;

        EtatDroit(String libelle, StatutPastille pastille) {
            this.libelle = libelle;
            this.pastille = pastille;
        }

        public String libelle() {
            return libelle;
        }

        StatutPastille pastille() {
            return pastille;
        }
    }

    /** Une ligne du tableau des droits. */
    public record Droit(String action, EtatDroit etat, String precision) {
    }

    /** Un club relié au compte, avec son niveau et ses droits. */
    public record ClubRelie(String numero, String nom, String niveau, boolean dissociable,
                            List<Droit> droits) {
        public ClubRelie {
            droits = List.copyOf(droits);
        }
    }

    // ------------------------------------------------------------------
    // Libellés
    // ------------------------------------------------------------------

    static final String TITRE = "Mon profil";
    static final String TITRE_IDENTITE = "Mes informations";
    static final String TITRE_LICENCE = "Licence FFTT officielle";
    static final String TITRE_TOURNOI = "Tournoi";
    static final String TITRE_APPEL = "Appel micro : prononciation phonétique";
    static final String TITRE_CLUB = "Club";
    static final String INTRO_CLUB = "Cette partie ne concerne que les personnes qui gèrent les "
            + "tournois d'un club. Pour vous inscrire à un tournoi, inutile de relier un club.";

    static final String TITRE_AVERTISSEMENT = "Avant de relier votre compte à un club";
    static final String AVERTISSEMENT =
            "Relier votre compte à un club vous donne des droits d'administration sur ce club : "
                    + "créer et organiser ses tournois au nom du club. Ne le faites que si le club "
                    + "vous a confié cette mission. Un code de vérification sera envoyé à l'adresse "
                    + "officielle du club : son responsable verra votre nom et votre email. "
                    + "Ce n'est pas nécessaire pour vous inscrire à un tournoi.";
    static final String CONFIRMATION =
            "J'ai lu cet avertissement : je gère les tournois de ce club et je veux y relier mon compte.";
    static final String MOTIF_SANS_CONFIRMATION =
            "Cochez d'abord la case de confirmation sous l'avertissement.";
    static final String MOTIF_SANS_NUMERO = "Saisissez un numéro de club.";


    private PresentationProfil() {
    }

    // ------------------------------------------------------------------
    // Bloc du haut
    // ------------------------------------------------------------------

    /** Identité du compte : nom, prénom, email, téléphone (jamais de valeur vide). */
    static List<Champ> identite(String nom, String prenom, String email, String telephone) {
        return List.of(
                new Champ("Nom", ouNonRenseigne(nom)),
                new Champ("Prénom", ouNonRenseigne(prenom)),
                new Champ("Email", ouNonRenseigne(email)),
                new Champ("Téléphone", ouNonRenseigne(telephone)));
    }

    /**
     * Toutes les informations disponibles de la licence liée, dans l'ordre de
     * lecture ; les informations absentes de la fiche ne sont pas listées.
     *
     * @param numeroLie numéro de licence lié au compte ({@code null} si aucun)
     * @param fiche     fiche FFTT correspondante, si elle est connue
     * @return lignes à afficher ; vide si aucune licence n'est liée
     */
    static List<Champ> licence(String numeroLie, Optional<FicheLicence> fiche) {
        List<Champ> l = new ArrayList<>();
        if (numeroLie == null || numeroLie.isBlank()) {
            return l;
        }
        l.add(new Champ("N° de licence", numeroLie));
        if (fiche.isEmpty()) {
            return l;
        }
        FicheLicence f = fiche.get();
        ajouter(l, "Nom", f.getNom());
        ajouter(l, "Prénom", f.getPrenom());
        if (f.getSexe() != null) {
            l.add(new Champ("Sexe", f.getSexe() == Sexe.FEMME ? "Femme" : "Homme"));
        }
        if (f.getDateNaissance() != null) {
            l.add(new Champ("Date de naissance", f.getDateNaissance().format(DATE_FR)));
        }
        ajouter(l, "Nationalité", f.getNationalite());
        ajouter(l, "Catégorie", f.getCategorie());
        String club = f.getNomClub();
        if (club != null && f.getNumeroClub() != null) {
            club = club + " (" + f.getNumeroClub() + ")";
        }
        ajouter(l, "Club", club);
        ajouter(l, "Saison", f.getSaison());
        ajouter(l, "Type de licence", f.getTypeLicence());
        ajouter(l, "Statut", f.getStatut());
        ajouter(l, "Classement", f.getClassement());
        l.add(new Champ("Points (début de phase)", String.valueOf(f.getPoints())));
        if (f.getPointsMensuels() > 0) {
            l.add(new Champ("Points (mensuels)", String.valueOf(f.getPointsMensuels())));
        }
        ajouter(l, "Arbitre", f.getArbitre());
        ajouter(l, "Juge-arbitre", f.getJugeArbitre());
        ajouter(l, "Technique", f.getTechnique());
        return l;
    }

    /** Phrase de la section « Tournoi » selon le nombre de tournois du joueur. */
    static String resumeTournois(boolean licenceLiee, int nombre) {
        if (!licenceLiee) {
            return "Liez votre licence pour retrouver ici les tournois où vous êtes inscrit.";
        }
        if (nombre == 0) {
            return "Vous n'êtes inscrit à aucun tournoi pour l'instant.";
        }
        if (nombre == 1) {
            return "Vous êtes inscrit (ou en liste d'attente) à 1 tournoi.";
        }
        return "Vous êtes inscrit (ou en liste d'attente) à " + nombre + " tournois.";
    }

    /** Valeur de la section « Appel micro » : la prononciation, ou « Non renseignée ». */
    static String prononciation(String phonetique) {
        return phonetique == null || phonetique.isBlank() ? "Non renseignée" : phonetique;
    }

    // ------------------------------------------------------------------
    // Bloc Club
    // ------------------------------------------------------------------

    /**
     * Contrôle préalable à la demande de code : l'avertissement doit avoir été
     * confirmé explicitement, puis un numéro saisi.
     *
     * @return le motif du refus, ou vide si la demande peut partir
     */
    static Optional<String> refusDemandeLiaison(boolean avertissementConfirme, String numeroClub) {
        if (!avertissementConfirme) {
            return Optional.of(MOTIF_SANS_CONFIRMATION);
        }
        if (numeroClub == null || numeroClub.isBlank()) {
            return Optional.of(MOTIF_SANS_NUMERO);
        }
        return Optional.empty();
    }

    /** Le tableau des droits n'apparaît que si le compte est relié à au moins un club. */
    static boolean afficheTableauDroits(List<AccesClub> acces) {
        return acces != null && !acces.isEmpty();
    }

    /** Clubs reliés au compte, chacun avec ses droits (vide si aucun). */
    static List<ClubRelie> clubsRelies(List<AccesClub> acces, Function<String, String> nomClub) {
        List<ClubRelie> res = new ArrayList<>();
        if (!afficheTableauDroits(acces)) {
            return res;
        }
        for (AccesClub a : acces) {
            String nom = nomClub.apply(a.getNumeroClub());
            res.add(new ClubRelie(a.getNumeroClub(),
                    nom == null || nom.isBlank() ? "Club " + a.getNumeroClub() : nom,
                    a.getNiveau().getLibelle(), !a.estPrincipal(), droits(a)));
        }
        return res;
    }

    /**
     * Ce que l'accès permet pour le club, d'après le modèle existant :
     * tout niveau peut organiser les tournois du club et voir son équipe de
     * gestion ; seul un administrateur non référent peut se dissocier ; la
     * gestion des autres administrateurs est annoncée pour le référent
     * (à venir, voir {@link com.pingmanager.model.NiveauAcces}).
     */
    static List<Droit> droits(AccesClub a) {
        boolean principal = a.estPrincipal();
        List<Droit> d = new ArrayList<>();
        d.add(new Droit("Créer et organiser les tournois du club", EtatDroit.OUI,
                "Préparation, homologation, publication, jour J."));
        d.add(new Droit("Voir l'équipe de gestion du club", EtatDroit.OUI,
                "Les personnes reliées au club et leur niveau."));
        d.add(principal
                ? new Droit("Se dissocier du club", EtatDroit.NON,
                        "Référent du club : statut défini par la base fédérale.")
                : new Droit("Se dissocier du club", EtatDroit.OUI,
                        "Retire vos droits sur ce club."));
        d.add(principal
                ? new Droit("Gérer les autres administrateurs", EtatDroit.A_VENIR,
                        "Réservé au référent du club.")
                : new Droit("Gérer les autres administrateurs", EtatDroit.NON,
                        "Réservé au référent du club."));
        return d;
    }

    // ------------------------------------------------------------------

    private static void ajouter(List<Champ> l, String libelle, String valeur) {
        if (valeur != null && !valeur.isBlank()) {
            l.add(new Champ(libelle, valeur));
        }
    }

    private static String ouNonRenseigne(String v) {
        return v == null || v.isBlank() ? NON_RENSEIGNE : v;
    }
}
