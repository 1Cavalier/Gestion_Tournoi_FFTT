package com.pingmanager.ui.ecran;

import com.pingmanager.model.ModeAnnonce;
import com.pingmanager.model.StrategieOrdonnancement;

/**
 * Tous les textes affichés dans les panneaux « Annonces » et « Stratégie » du
 * jour J (B056), réunis ici pour être relus et corrigés au même endroit.
 * Phrases courtes, dans les mots d'un bénévole ou d'un juge-arbitre.
 *
 * <p>Pas de {@code switch} sur les énumérations : chaînes de {@code if}.</p>
 */
final class TextesPanneauxJourJ {

    private TextesPanneauxJourJ() {
    }

    // ==================================================================
    // Commun aux deux panneaux
    // ==================================================================

    static final String RETOUR = "← Retour au jour J";
    static final String FERMER = "Fermer";

    // ==================================================================
    // Panneau « Annonces »
    // ==================================================================

    static final String ANNONCES_TITRE = "Annonces";
    static final String ANNONCES_PHRASE = "Choisissez comment les joueurs sont prévenus quand vous lancez un "
            + "match, puis réglez la voix et les SMS.";

    // --- Choix du mode ---
    static final String MODE_TITRE = "Quand un match est lancé";
    static final String MODE_ACTUEL = "Mode actuel";
    static final String MODE_AUCUN_CHOISI = "Aucun mode choisi : le logiciel vous demande comment prévenir les "
            + "joueurs à chaque match lancé.";
    static final String MODE_DEMANDER = "Me demander à chaque match";

    /** Nom du mode, tel qu'il est affiché sur son option. */
    static String nomMode(ModeAnnonce m) {
        if (m == ModeAnnonce.APPEL) {
            return "Voix";
        }
        if (m == ModeAnnonce.SMS) {
            return "SMS";
        }
        if (m == ModeAnnonce.SMS_ET_APPEL) {
            return "Voix + SMS";
        }
        return "Aucune annonce";
    }

    /** Ce qui se passe quand un match est lancé avec ce mode. */
    static String phraseMode(ModeAnnonce m) {
        if (m == ModeAnnonce.APPEL) {
            return "Le haut-parleur appelle les deux joueurs et annonce la table.";
        }
        if (m == ModeAnnonce.SMS) {
            return "Les deux joueurs reçoivent un SMS avec leur table et leur adversaire.";
        }
        if (m == ModeAnnonce.SMS_ET_APPEL) {
            return "Le haut-parleur appelle les joueurs et chacun reçoit aussi un SMS.";
        }
        return "Rien ne part : vous appelez les joueurs vous-même.";
    }

    static final String MODE_VOIX_A_ACTIVER = "À activer : « Annoncer chaque match lancé » (bloc Voix).";
    static final String MODE_SMS_A_ACTIVER = "À activer : « Envoyer à chaque match lancé » (bloc SMS).";
    static final String MODE_VOIX_ECRAN = "Sur cet ordinateur, l'annonce s'affiche à l'écran pour être lue au "
            + "micro.";

    /** Message après un changement de mode. */
    static String modeEnregistre(ModeAnnonce m) {
        if (m == null) {
            return "Le logiciel vous demandera comment prévenir les joueurs à chaque match lancé.";
        }
        return "Mode d'annonce : " + nomMode(m) + ". Choix enregistré pour ce tournoi.";
    }

    // --- États des blocs ---
    static final String BLOC_ACTIF = "Utilisé avec le mode actuel";
    static final String BLOC_INACTIF = "Inactif avec le mode actuel";
    static final String BLOC_DEPLIER = "Préparer les réglages";
    static final String BLOC_REPLIER = "Replier";

    // --- Bloc Voix ---
    static final String VOIX_TITRE = "Voix";
    static final String VOIX_EXPLICATION = "L'ordinateur lit l'annonce au haut-parleur, par exemple : « Lucas "
            + "Martin, Brunoy, contre Paul Durand, Melun, table 4 ».";
    static final String VOIX_INDISPONIBLE = "Sur cet ordinateur, la voix ne fonctionne pas (Windows seulement). "
            + "Chaque annonce s'affiche en haut de l'écran pour être lue au micro.";
    static final String VOIX_AUTOMATIQUE = "Annoncer chaque match lancé";
    static final String VOIX_AUTOMATIQUE_AIDE = "Sinon, utilisez le bouton « Annoncer » de la table.";
    static final String VOIX_TABLEAU_TOUR = "Commencer par le tableau et le tour";
    static final String VOIX_REPETITION = "Répétition";
    static final String VOIX_RAPPEL = "Rappel si la table reste vide";
    static final String VOIX_DEBIT = "Débit";
    static final String VOIX_CHOIX = "Voix";
    static final String VOIX_PAR_DEFAUT = "Voix de l'ordinateur";
    static final String VOIX_TESTER = "Tester la voix";
    static final String VOIX_TEST_PHRASE = "Essai de la voix. Lucas Martin, contre Paul Durand, table 4.";
    static final String VOIX_TEST_EN_COURS = "Écoute en cours…";
    static final String VOIX_PRONONCIATION = "Prononciation des noms…";

    static String voixAutomatique(boolean active) {
        return active ? "Chaque match lancé sera annoncé à la voix."
                : "Annonce automatique coupée : bouton « Annoncer » sur chaque table.";
    }

    static String voixTableauTour(boolean active) {
        return active ? "Les annonces commencent par le tableau et le tour."
                : "Les annonces ne disent plus le tableau ni le tour.";
    }

    static String voixReglage(String libelle) {
        return "Voix : " + libelle.toLowerCase() + ".";
    }

    // --- Bloc SMS ---
    static final String SMS_TITRE = "SMS";
    static final String SMS_SIMULATION = "Envoi simulé : aucun SMS ne part réellement pour l'instant. Les messages "
            + "sont visibles dans « Numéros et journal des SMS ».";
    static final String SMS_EXPLICATION = "Chaque joueur reçoit un SMS avec sa table et son adversaire, s'il a "
            + "donné son numéro et son accord.";
    static final String SMS_AUTOMATIQUE = "Envoyer à chaque match lancé";
    static final String SMS_EXPEDITEUR = "Nom affiché sur le téléphone";
    static final String SMS_NUMEROS = "Numéros enregistrés";
    static final String SMS_ENVOIS = "SMS envoyés";
    static final String SMS_TESTER = "Voir un exemple de SMS";
    static final String SMS_EXEMPLE = "Exemple (rien n'est envoyé) :";
    static final String SMS_JOURNAL = "Numéros et journal des SMS…";

    static String smsAutomatique(boolean actif) {
        return actif ? "Les SMS partiront à chaque match lancé." : "Envoi automatique des SMS coupé.";
    }

    static String nombreNumeros(int n) {
        return n == 0 ? "aucun" : n + (n > 1 ? " joueurs" : " joueur");
    }

    static String envois(int envoyes, int limite) {
        return envoyes + " sur " + limite + " au plus";
    }

    // --- Historique ---
    static final String HISTORIQUE_TITRE = "Dernières annonces";
    static final String HISTORIQUE_AIDE = "Les dix dernières annonces à la voix depuis l'ouverture du logiciel.";
    static final String HISTORIQUE_VIDE = "Aucune annonce pour l'instant. Elles s'afficheront ici dès le premier "
            + "match annoncé.";
    static final String HISTORIQUE_DITE = "Dite";
    static final String HISTORIQUE_A_LIRE = "À lire au micro";
    static final String HISTORIQUE_RAPPEL = "Rappel";
    static final String HISTORIQUE_REANNONCER = "Réannoncer";

    // ==================================================================
    // Panneau « Stratégie »
    // ==================================================================

    static final String STRATEGIE_TITRE = "Stratégie";
    static final String STRATEGIE_PHRASE = "La stratégie décide de l'ordre des matchs que le logiciel vous "
            + "propose. Changez-la si la journée prend du retard ou si les joueurs attendent trop.";
    static final String STRATEGIE_EN_PLACE = "En place";
    static final String STRATEGIE_PAR_DEFAUT = "En place par défaut";
    static final String STRATEGIE_QUAND = "Quand la choisir";
    static final String STRATEGIE_TOURNOI = "Tournoi";
    static final String STRATEGIE_TABLES = "Tables";
    static final String STRATEGIE_OBJECTIF = "Objectif";
    static final String STRATEGIE_EN_SALLE = "En salle";
    static final String STRATEGIE_AVANTAGE = "Avantage";
    static final String STRATEGIE_INCONVENIENT = "Inconvénient";
    static final String STRATEGIE_CHOISIR = "Passer à cette stratégie";
    static final String STRATEGIE_CONFIRMER_DEFAUT = "Garder cette stratégie";
    static final String STRATEGIE_ACTIVE = "Stratégie en place";
    static final String STRATEGIE_OUI = "Oui, changer";
    static final String STRATEGIE_ANNULER = "Annuler";

    /** Confirmation demandée avant de changer de stratégie. */
    static String confirmationChangement(StrategieOrdonnancement s) {
        return "Passer à « " + s.getLibelle() + " » ? Le changement vaut pour les prochains matchs proposés. "
                + "Les matchs déjà sur une table ne changent pas.";
    }

    static String typeTournoi(StrategieOrdonnancement s) {
        if (s == StrategieOrdonnancement.AVANCER_TABLEAUX_COMMENCES) {
            return "Beaucoup de tableaux qui se suivent dans la journée.";
        }
        if (s == StrategieOrdonnancement.FAIRE_AVANCER_PETIT_A_PETIT) {
            return "Toutes les finales ensemble, en fin de journée.";
        }
        return "Beaucoup de joueurs inscrits dans plusieurs tableaux.";
    }

    static String tables(StrategieOrdonnancement s) {
        if (s == StrategieOrdonnancement.AVANCER_TABLEAUX_COMMENCES) {
            return "Salle juste en tables.";
        }
        if (s == StrategieOrdonnancement.FAIRE_AVANCER_PETIT_A_PETIT) {
            return "Assez de tables pour ne pas courir après l'heure.";
        }
        return "Tout nombre de tables.";
    }

    static String objectif(StrategieOrdonnancement s) {
        if (s == StrategieOrdonnancement.AVANCER_TABLEAUX_COMMENCES) {
            return "Libérer tôt les joueurs et les tables.";
        }
        if (s == StrategieOrdonnancement.FAIRE_AVANCER_PETIT_A_PETIT) {
            return "Un final groupé, devant le public.";
        }
        return "Le confort des joueurs.";
    }

    static String enSalle(StrategieOrdonnancement s) {
        if (s == StrategieOrdonnancement.AVANCER_TABLEAUX_COMMENCES) {
            return "Le tableau commencé le plus tôt passe d'abord, ses tours les plus avancés en premier. "
                    + "Sa finale se joue dès qu'elle est prête.";
        }
        if (s == StrategieOrdonnancement.FAIRE_AVANCER_PETIT_A_PETIT) {
            return "Tous les tableaux avancent ensemble : les poules, puis chaque tour une fois le précédent "
                    + "fini. Les finales sont gardées pour la fin.";
        }
        return "Le match qui attend depuis le plus longtemps part d'abord, ainsi que ceux des joueurs attendus "
                + "dans un autre tableau.";
    }

    static String avantage(StrategieOrdonnancement s) {
        if (s == StrategieOrdonnancement.AVANCER_TABLEAUX_COMMENCES) {
            return "Les joueurs des premiers tableaux repartent tôt et les tables se libèrent pour les suivants.";
        }
        if (s == StrategieOrdonnancement.FAIRE_AVANCER_PETIT_A_PETIT) {
            return "Les finales sont groupées en fin de journée.";
        }
        return "Personne ne reste longtemps sans jouer.";
    }

    static String inconvenient(StrategieOrdonnancement s) {
        if (s == StrategieOrdonnancement.AVANCER_TABLEAUX_COMMENCES) {
            return "Les tableaux qui commencent tard prennent du retard, et les finales s'étalent dans la journée.";
        }
        if (s == StrategieOrdonnancement.FAIRE_AVANCER_PETIT_A_PETIT) {
            return "Des tables restent libres en attendant la fin d'un tour : la journée finit plus tard.";
        }
        return "Un tableau commencé tôt n'est pas forcément fini en premier.";
    }

    // --- Comparer les stratégies ---
    static final String COMPARER_TITRE = "Comparer les stratégies";
    static final String COMPARER_AIDE = "Le logiciel rejoue la journée avec chacune des trois stratégies et "
            + "compare l'heure de fin et l'attente des joueurs. Ce sont des estimations.";
    static final String COMPARER_BOUTON = "Voir la comparaison";
}
