package com.pingmanager.core;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Mesures complémentaires d'un tirage de simulation (B050), en plus des
 * indicateurs de {@link ResultatSimulation}. Elles se lisent par journée : la
 * prévision simule chaque journée séparément.
 *
 * <p><b>Attente</b> et <b>pause</b> ne mesurent pas la même chose. L'attente
 * d'une partie court depuis que le joueur est disponible <i>dans le tableau</i>
 * (début du tableau, ou fin de sa partie précédente). La pause est le temps
 * réel entre deux parties consécutives d'un même joueur, tous tableaux
 * confondus, dans la même journée : un joueur qui finit un tableau à 11 h et
 * commence le suivant à 14 h a une pause de trois heures.</p>
 *
 * @param attenteMediane        médiane des attentes par partie, en minutes
 * @param plusLonguePause       plus longue pause d'un joueur entre deux de ses parties, en minutes
 * @param joueurPlusLonguePause nom du joueur concerné ({@code null} si aucun)
 * @param picTables             plus grand nombre de tables occupées en même temps
 * @param matchsParHeure        matchs lancés dans chaque heure pleine (clé : début de l'heure)
 * @param debutPremiereFinale   lancement de la première finale de la journée ({@code null} si aucune)
 * @param debutDerniereFinale   lancement de la dernière finale de la journée ({@code null} si aucune)
 * @param dureeJournee          minutes entre le début du premier tableau et la fin de la dernière
 *                              partie (-1 si rien n'a été joué)
 */
public record MesuresTirage(double attenteMediane, long plusLonguePause, String joueurPlusLonguePause,
                            int picTables, SortedMap<LocalDateTime, Integer> matchsParHeure,
                            LocalDateTime debutPremiereFinale, LocalDateTime debutDerniereFinale,
                            long dureeJournee) {

    /** Mesures inconnues (résultat construit sans elles). */
    public static final MesuresTirage VIDE = new MesuresTirage(0, 0, null, 0, new TreeMap<>(), null, null, -1);

    public MesuresTirage {
        matchsParHeure = Collections.unmodifiableSortedMap(new TreeMap<>(matchsParHeure));
    }
}
