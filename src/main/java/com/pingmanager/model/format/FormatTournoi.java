package com.pingmanager.model.format;

import com.pingmanager.model.FormatPhaseFinale;

/**
 * Décrit les <b>règles structurelles</b> d'un format de tournoi : comment les
 * joueurs sont regroupés en poules, combien en sortent, ce que deviennent les
 * autres, et comment se déroule la phase finale.
 *
 * <p><b>Un format = un fichier.</b> Chaque format concret (poule de 3 à 2
 * qualifiés, poule de 4, etc.) est une classe implémentant cette interface,
 * dans le package {@code com.pingmanager.model.format}. Ajouter un format
 * revient à ajouter une classe, sans toucher au moteur : les services
 * (génération de poules, placement, tableau) lisent ces règles de façon
 * générique.</p>
 *
 * <p><b>Ce qui n'est PAS ici.</b> Les règles transverses qui s'appliquent quel
 * que soit le format ne relèvent pas d'un format particulier. C'est notamment le
 * cas de la <i>règle anti-arrangement</i> (deux joueurs d'un même club se
 * rencontrent au premier tour de leur poule), gérée directement par le
 * générateur de matchs de poule pour tous les formats.</p>
 */
public interface FormatTournoi {

    /** Nom lisible du format (ex. « Poule de 3 — 2 qualifiés »). */
    String nom();

    /** Identifiant stable et court du format (ex. « P3Q2 »), utile pour la persistance. */
    String identifiant();

    /**
     * Taille de poule visée par ce format. Le générateur de poules peut s'en
     * écarter à la marge selon l'effectif (poules d'appoint), mais vise cette
     * valeur.
     */
    int taillePouleSouhaitee();

    /** Nombre de joueurs qui sortent de chaque poule vers le tableau principal. */
    int qualifiesParPoule();

    /** Sort réservé aux joueurs non qualifiés d'une poule. */
    SortNonQualifies sortNonQualifies();

    /** Format de la phase qui suit les poules. */
    FormatPhaseFinale formatPhaseFinale();

    /**
     * Nombre de manches gagnantes d'un match (best-of). 3 signifie « au meilleur
     * des 5 manches » (le premier à 3 gagne).
     */
    int nombreManchesGagnantes();

    /**
     * Best-of spécifique à la <b>finale</b>, si le format en prévoit un différent
     * du best-of courant. Par défaut, la finale suit le best-of standard.
     *
     * @return le nombre de manches gagnantes de la finale
     */
    default int nombreManchesGagnantesFinale() {
        return nombreManchesGagnantes();
    }

    /**
     * Indique si le format prévoit une <b>petite finale</b> (match pour la 3e
     * place entre les deux perdants des demi-finales). Désactivée par défaut.
     */
    default boolean avecPetiteFinale() {
        return false;
    }

    /**
     * Best-of spécifique à la <b>petite finale</b>, si prévue. Par défaut, suit
     * le best-of standard.
     */
    default int nombreManchesGagnantesPetiteFinale() {
        return nombreManchesGagnantes();
    }

    /**
     * Indique si ce format sépare les joueurs d'un même club à la constitution
     * des poules. Toujours vrai en pratique (règle FFTT), mais exposé pour
     * rester explicite et permettre des variantes futures.
     */
    default boolean separerLesClubsEnPoule() {
        return true;
    }
}
