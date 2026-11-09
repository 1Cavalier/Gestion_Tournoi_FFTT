package com.pingmanager.ui.infra;

import com.pingmanager.core.RegistreClubsMemoire;
import com.pingmanager.model.Club;
import com.pingmanager.model.FicheLicence;
import com.pingmanager.model.Sexe;

import java.time.LocalDate;

/**
 * Jeu de données FFTT <b>de démonstration</b>, alimenté à la main, pour dérouler
 * les deux démarches (liaison licence, liaison club) sans vraie base fédérale.
 *
 * <p>Fournit une fausse base clubs ({@link RegistreClubsMemoire}) et une fausse
 * base licences ({@link RegistreLicencesFFTT}). Tout est reconstruit à chaque
 * démarrage : ces données ne sont pas persistées (la vraie source, à terme, est
 * la FFTT). Le profil personnel pré-chargé, lui, est enregistré dans le dépôt
 * applicatif par le contexte.</p>
 */
public final class DonneesDemo {

    private DonneesDemo() {
    }

    // Numéros de club de démo.
    public static final String CLUB_BRUNOY = "08911132";
    public static final String CLUB_PARIS_15 = "07500123";

    // Profil personnel pré-chargé.
    public static final String EMAIL_QUENTIN = "soumet.quentin@gmail.com";
    public static final String LICENCE_QUENTIN = "9141442";

    // Compte « ultra admin » (supervision PingManager) : accès à tous les tournois de tous
    // les clubs, pour vérifier et aider en direct. Ce compte n'est rattaché à aucun club.
    public static final String EMAIL_ULTRA_ADMIN = "admin@pingmanager.fr";
    public static final String MDP_ULTRA_ADMIN = "PingAdmin123/";
    public static final java.util.Set<String> ULTRA_ADMINS = java.util.Set.of(EMAIL_ULTRA_ADMIN);

    // Administrateur par défaut (référent) de Brunoy, défini par la base.
    public static final String EMAIL_REFERENT_BRUNOY = "sboulier@gmail.com";
    public static final String REFERENT_BRUNOY_NOM = "BOULIER";
    public static final String REFERENT_BRUNOY_PRENOM = "Serge";

    /** @return une fausse base clubs FFTT amorcée. */
    public static RegistreClubsMemoire clubs() {
        RegistreClubsMemoire clubs = new RegistreClubsMemoire();

        // Référent officiel du club (administrateur par défaut, défini par la base) :
        // c'est à cette adresse qu'arrive le code de rattachement.
        Club brunoy = new Club(CLUB_BRUNOY, "Brunoy", "91", "Brunoy", EMAIL_REFERENT_BRUNOY);
        brunoy.setResponsableNom(REFERENT_BRUNOY_NOM);
        brunoy.setResponsablePrenom(REFERENT_BRUNOY_PRENOM);
        // Adresse de démonstration (fictive) : sert d'adresse par défaut des tournois.
        brunoy.setAdresse1("1 rue du Gymnase");
        brunoy.setCodePostal("91800");
        clubs.ajouter(brunoy);

        Club paris15 = new Club(CLUB_PARIS_15, "TT Paris 15", "75", "Paris", "contact@ttparis15.fr");
        paris15.setResponsableNom("Martin");
        paris15.setResponsablePrenom("Claire");
        paris15.setAdresse1("12 rue de l'Exemple");
        paris15.setCodePostal("75015");
        clubs.ajouter(paris15);

        // Clubs fictifs des joueurs et des tournois de démo (onglet Tournois).
        JoueursFictifs.clubs().forEach(clubs::ajouter);
        return clubs;
    }

    private static FicheLicence officiel(String licence, String nom, String prenom, Sexe sexe,
                                         String numClub, String nomClub, String arbitre, String ja) {
        return new FicheLicence.Builder(licence, nom, prenom)
                .sexe(sexe)
                .club(numClub, nomClub)
                .saison("2026-2027")
                .points(1100)
                .pointsMensuels(1100)
                .categorie("Senior")
                .typeLicence("Compétition")
                .statut("Active")
                .arbitre(arbitre)
                .jugeArbitre(ja)
                .build();
    }

    /** Joueurs fictifs de la démo (générés une fois, toujours les mêmes). */
    public static final java.util.List<FicheLicence> JOUEURS_FICTIFS = JoueursFictifs.generer();

    /** @return une fausse base licences FFTT amorcée. */
    public static RegistreLicencesFFTT licences() {
        RegistreLicencesFFTT licences = new RegistreLicencesFFTT();

        licences.ajouter(new FicheLicence.Builder(LICENCE_QUENTIN, "SOUMET", "Quentin")
                .sexe(Sexe.HOMME)
                .dateNaissance(LocalDate.of(2003, 1, 20))
                .nationalite("Française")
                .club(CLUB_BRUNOY, "Brunoy")
                .saison("2026-2027")
                .points(923)           // points de début de phase (référence)
                .pointsMensuels(923)   // situation mensuelle (= référence en début de phase)
                .classement("9")       // 900-999 -> classement 9
                .categorie("Senior")
                .typeLicence("Compétition")
                .statut("Active")
                .arbitre("AR")
                .jugeArbitre("JA1")
                .build());

        licences.ajouter(new FicheLicence("9411003", "Durand", "Paul", Sexe.HOMME,
                CLUB_BRUNOY, "Brunoy", 980));
        licences.ajouter(new FicheLicence("7511002", "Martin", "Claire", Sexe.FEMME,
                CLUB_PARIS_15, "TT Paris 15", 1620));

        // Officiels fictifs, pour essayer la désignation des juges-arbitres et arbitres.
        licences.ajouter(officiel("9411021", "LEMOINE", "Hélène", Sexe.FEMME, CLUB_BRUNOY, "Brunoy", "AR", "JA3"));
        licences.ajouter(officiel("7511045", "GARNIER", "Marc", Sexe.HOMME, CLUB_PARIS_15, "TT Paris 15", "AN", "JAN"));
        licences.ajouter(officiel("9411034", "ROUX", "Sophie", Sexe.FEMME, CLUB_BRUNOY, "Brunoy", null, "JA2"));
        licences.ajouter(officiel("9411056", "PETIT", "Antoine", Sexe.HOMME, CLUB_BRUNOY, "Brunoy", "AR", null));
        licences.ajouter(officiel("7511078", "FAURE", "Julien", Sexe.HOMME, CLUB_PARIS_15, "TT Paris 15", "AR", "JA3"));
        licences.ajouter(officiel("9411090", "MARTINEZ", "Lucas", Sexe.HOMME, CLUB_BRUNOY, "Brunoy", null, null));

        // Joueurs fictifs (licences 99…) qui remplissent les tableaux des tournois de démo.
        JOUEURS_FICTIFS.forEach(licences::ajouter);

        return licences;
    }

    /** @return un mémo lisible des données de démo. */
    public static String memo() {
        return """
                PingManager — données de démo

                Profil pré-chargé (connexion directe) :
                  email       : soumet.quentin@gmail.com
                  mot de passe: Soumet123/
                  licence 9141442 ; administrateur (niveau 1) du club Brunoy (08911132).
                  Référent / administrateur par défaut de Brunoy : Serge BOULIER <sboulier@gmail.com>.

                Compte ultra admin (supervision de tous les tournois) :
                  email       : admin@pingmanager.fr
                  mot de passe: PingAdmin123/

                Autres licences de test : 9411003 (Paul Durand, Brunoy), 7511002 (Claire Martin, Paris 15).
                Joueurs fictifs : licences 9900001 et suivantes (inscrits aux tournois de démo).
                Tournois de démo : National B de Brunoy (publié, n° d'homologation fictif) et sa
                copie au jour J, presque pleine, samedi en cours (Gestion des tournois → Suivre).
                Autre club de test      : 07500123 (TT Paris 15).
                """;
    }
}
