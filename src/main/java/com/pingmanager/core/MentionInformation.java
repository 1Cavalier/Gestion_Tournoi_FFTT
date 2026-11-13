package com.pingmanager.core;

/**
 * Point 21 du README : mention d'information sur les données personnelles (RGPD),
 * affichée à la création d'un compte, à l'inscription d'un joueur et dans « Mon profil ».
 *
 * <p>Texte d'information <b>sans valeur juridique affirmée</b> : à faire valider par
 * Quentin ou un juriste (README section 6). La durée de conservation des résultats
 * sportifs et du numéro de licence (obligations fédérales) n'est pas fixée ici : elle
 * reste à confirmer.</p>
 */
public final class MentionInformation {

    private MentionInformation() {
    }

    /** Titre commun. */
    public static final String TITRE = "Vos données personnelles";

    /** Version courte, sous le formulaire de création de compte. */
    public static final String CREATION_COMPTE = "Vos nom, prénom et email servent à gérer votre compte et vos "
            + "inscriptions aux tournois. Ils restent sur cet ordinateur et ne sont ni vendus ni transmis à des "
            + "tiers, hors obligations envers la FFTT. Détails et contact : « Mon profil ».";

    /** Version courte, au moment de l'inscription d'un joueur à un tournoi. */
    public static final String INSCRIPTION = "Le club organisateur reçoit votre nom, prénom, numéro de licence, "
            + "club, points et catégorie pour organiser le tournoi ; les résultats sont transmis à la FFTT. "
            + "Pour exercer vos droits, contactez le correspondant du tournoi.";

    /**
     * Version complète (« Mon profil »).
     *
     * @param anneesConservation durée de conservation des tournois terminés, en années
     */
    public static String complete(int anneesConservation) {
        return "Quelles données ? Pour votre compte : nom, prénom, email, mot de passe (jamais en clair) et, "
                + "si vous la liez, votre licence. Pour les tournois : nom, prénom, numéro de licence, sexe, club, "
                + "points, catégorie, pointage, paiement et résultats ; votre numéro de portable seulement si vous "
                + "acceptez les convocations par SMS.\n\n"
                + "Pourquoi ? Gérer votre compte, vos inscriptions, l'organisation du tournoi le jour J et la "
                + "transmission des résultats à la FFTT.\n\n"
                + "Combien de temps ? Votre compte : tant que vous le gardez. Les numéros de portable : effacés "
                + "après le dernier jour du tournoi. Les tournois terminés : supprimés après " + anneesConservation
                + " ans par le club. Les résultats sportifs et le numéro de licence peuvent être conservés plus "
                + "longtemps pour les obligations fédérales (durée à confirmer).\n\n"
                + "Qui contacter ? Pour un tournoi, le correspondant indiqué par le club organisateur, qui peut "
                + "effacer vos nom et prénom des tournois de son club (vos résultats et votre numéro de licence "
                + "restent). Pour votre compte, l'administrateur de PingManager sur cet ordinateur.\n\n"
                + "Vous pouvez aussi saisir la CNIL (cnil.fr).";
    }
}
