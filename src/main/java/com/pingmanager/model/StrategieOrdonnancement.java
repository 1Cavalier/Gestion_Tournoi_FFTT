package com.pingmanager.model;

/**
 * Stratégie principale d'ordonnancement choisie par le club pour faire avancer
 * la journée (cahier des charges v0, section 6). Chaque stratégie est expliquée
 * en clair : ce qu'elle cherche, et ce qu'elle coûte.
 *
 * <p>Ce n'est pas une règle fédérale : le juge-arbitre programme les parties
 * selon l'horaire et les tables disponibles (3.3.1.2.2, IX.109) ; la stratégie
 * ne fait que classer les matchs proposés. Elle est mémorisée par tournoi
 * ({@link Tournoi#getStrategieOrdonnancement()}) ; sans choix, c'est
 * {@link #PAR_DEFAUT} qui s'applique (réglage historique de l'ordonnanceur).</p>
 *
 * <p>Pas de {@code switch} sur cette énumération : libellés et explications sont
 * portés par les constantes ; la traduction en poids de l'ordonnanceur est
 * faite côté ordonnancement ({@code PoidsOrdonnancement.pour}).</p>
 */
public enum StrategieOrdonnancement {

    /** (1) Finir au plus vite les tableaux qui ont commencé tôt pour libérer de la place. */
    AVANCER_TABLEAUX_COMMENCES(
            "Avancer les tableaux commencés tôt",
            "Les tableaux qui ont commencé le plus tôt passent en premier, jusqu'à la finale. "
                    + "Ils se terminent vite, leurs joueurs repartent et la salle se libère "
                    + "pour les tableaux suivants.",
            "Les tableaux qui commencent plus tard peuvent prendre du retard au départ."),

    /** (2) Faire progresser tous les tableaux en même temps, par vagues. */
    FAIRE_AVANCER_PETIT_A_PETIT(
            "Tout faire avancer petit à petit",
            "Tous les tableaux avancent ensemble, par vagues : d'abord les poules, puis les "
                    + "premiers tours, puis les tours suivants. Aucun tableau ne prend beaucoup "
                    + "d'avance sur les autres.",
            "Les premiers tableaux finissent plus tard : leurs joueurs restent plus longtemps "
                    + "dans la salle."),

    /** (3) Lancer d'abord les matchs qui attendent depuis le plus longtemps. */
    MINIMISER_ATTENTE(
            "Faire attendre le moins possible les joueurs",
            "Le logiciel lance d'abord les matchs qui attendent depuis le plus longtemps, et "
                    + "ceux des joueurs attendus dans un autre tableau. Personne ne reste trop "
                    + "longtemps sans jouer.",
            "L'avancement général peut être un peu plus lent : un tableau commencé tôt n'est "
                    + "pas forcément fini en premier.");

    /** Stratégie appliquée tant que le club n'a rien choisi (réglage historique de l'ordonnanceur). */
    public static final StrategieOrdonnancement PAR_DEFAUT = MINIMISER_ATTENTE;

    private final String libelle;
    private final String explication;
    private final String contrepartie;

    StrategieOrdonnancement(String libelle, String explication, String contrepartie) {
        this.libelle = libelle;
        this.explication = explication;
        this.contrepartie = contrepartie;
    }

    /** Libellé court, tel que présenté au club. */
    public String getLibelle() {
        return libelle;
    }

    /** Explication en clair de ce que fait la stratégie. */
    public String getExplication() {
        return explication;
    }

    /** Ce que la stratégie coûte (pour un choix éclairé). */
    public String getContrepartie() {
        return contrepartie;
    }
}
