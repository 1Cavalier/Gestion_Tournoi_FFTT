package com.pingmanager.core;

import com.pingmanager.model.Joueur;
import com.pingmanager.model.Match;
import com.pingmanager.model.StatutMatch;
import com.pingmanager.model.TableauElimination;
import com.pingmanager.model.Tour;

import java.util.List;
import java.util.Objects;

/**
 * Construit un tableau à élimination directe à partir d'une liste de joueurs
 * classés par ordre de force (le meilleur en premier), en appliquant les règles
 * FFTT de placement et d'exemption.
 *
 * <p><b>Principe des exemptions.</b> Lorsque le nombre de joueurs N n'est pas
 * une puissance de 2, on complète jusqu'au « cadre » (la puissance de 2
 * immédiatement supérieure ou égale : 2, 4, 8, 16, 32, 64…). Les places
 * excédentaires sont des adversaires « fictifs » (byes). Les joueurs les mieux
 * classés tombent face à ces byes : ils sont donc <b>exemptés du premier tour</b>
 * et entrent directement au deuxième. Les joueurs moins bien classés, eux,
 * disputent un tour de plus. C'est le fameux « tour préliminaire » (souvent un
 * 64e ou 32e de finale), qui n'est pas un barrage mais bien le premier tour du
 * tableau.</p>
 *
 * <p><b>Principe de placement (seeding).</b> Le meilleur joue le plus faible, le
 * 2e joue l'avant-dernier, etc., et les têtes de série sont réparties pour se
 * rencontrer le plus tard possible (le N°1 et le N°2 ne peuvent se croiser qu'en
 * finale). Ceci est obtenu en générant l'ordre standard des positions du cadre.</p>
 *
 * <p>Service sans état, sans dépendance UI/persistance.</p>
 */
public final class GenerateurTableauElimination {

    /**
     * Construit le tableau à élimination.
     *
     * @param joueursClasses joueurs triés par force décroissante (index 0 = tête
     *                       de série n°1). Cet ordre est en général issu du
     *                       classement des poules (1ers de poule puis 2es…).
     * @return le {@link TableauElimination} avec tous ses tours et matchs. Le
     *         premier tour contient les matchs réels (les exemptés n'y figurent
     *         pas). Les tours suivants sont créés « en attente » : leurs matchs
     *         seront complétés au fur et à mesure que les vainqueurs sont connus.
     * @throws IllegalArgumentException si moins de 2 joueurs
     */
    public TableauElimination genererTableau(List<Joueur> joueursClasses) {
        Objects.requireNonNull(joueursClasses, "joueursClasses");
        int n = joueursClasses.size();
        if (n < 2) {
            throw new IllegalArgumentException(
                    "Un tableau à élimination requiert au moins 2 joueurs (reçu : " + n + ").");
        }

        int cadre = puissanceDeDeuxSuperieureOuEgale(n);

        // Placement standard : le joueur d'index i occupe le seed i+1.
        // parSeed[seed] = joueur, ou null (bye) si seed > n.
        Joueur[] parSeed = new Joueur[cadre + 1];
        for (int seed = 1; seed <= cadre; seed++) {
            parSeed[seed] = (seed <= n) ? joueursClasses.get(seed - 1) : null;
        }
        return genererDepuisPlacement(parSeed, n);
    }

    /**
     * Construit le tableau à partir d'un placement explicite par seed.
     *
     * <p>Cette entrée est utilisée par {@code PlacementQualifiesService}, qui
     * décide quel joueur occupe quel seed du cadre selon les règles FFTT (I.305 :
     * 2es de poule dans le demi opposé à leur 1er, séparation des clubs, etc.).
     * Le générateur ne fait alors que bâtir l'arbre et les liens de progression.</p>
     *
     * @param parSeed        tableau indexé de 0 à cadre inclus ; {@code parSeed[s]}
     *                       est le joueur placé au seed s (1-based), ou null pour
     *                       un bye. L'index 0 est ignoré.
     * @param nombreJoueurs  nombre de joueurs réels (non byes) placés
     * @return le tableau à élimination correspondant
     */
    public TableauElimination genererDepuisPlacement(Joueur[] parSeed, int nombreJoueurs) {
        Objects.requireNonNull(parSeed, "parSeed");
        if (nombreJoueurs < 2) {
            throw new IllegalArgumentException(
                    "Un tableau à élimination requiert au moins 2 joueurs (reçu : " + nombreJoueurs + ").");
        }
        int cadre = parSeed.length - 1;
        if (Integer.bitCount(cadre) != 1) {
            throw new IllegalArgumentException("Le cadre doit être une puissance de 2 (reçu : " + cadre + ").");
        }

        TableauElimination tableau = new TableauElimination(nombreJoueurs, cadre);

        // Ordre standard des positions du cadre (seeds 1-based).
        // Ex. cadre 8 -> [1,8,4,5,2,7,3,6] : positions consécutives = affrontements.
        int[] ordre = ordreDuCadre(cadre);

        // Chaque "slot" du cadre est occupé par le joueur du seed correspondant
        // (ou null s'il s'agit d'un bye). L'ordre des slots suit `ordre`.
        Joueur[] slots = new Joueur[cadre];
        for (int i = 0; i < cadre; i++) {
            slots[i] = parSeed[ordre[i]];
        }

        // Génération tour par tour. À chaque niveau, on apparie les slots deux à
        // deux. Un "résultat de slot" au niveau suivant est soit un joueur connu
        // (exempté qui passe sans jouer, ou seul présent de sa paire), soit le
        // match dont le vainqueur occupera ce slot (source pour la propagation).
        int numeroTour = 1;
        int taille = cadre;

        // sourceParSlot[i] : le match dont le vainqueur alimente le slot i du
        // niveau courant (null si le slot est occupé par un joueur déjà connu).
        Match[] sourceParSlot = new Match[cadre];

        while (taille >= 2) {
            Tour tour = new Tour(numeroTour, nomTourParCadre(taille));

            Joueur[] slotsSuivant = new Joueur[taille / 2];
            Match[] sourceSuivant = new Match[taille / 2];

            for (int k = 0; k < taille; k += 2) {
                Joueur a = slots[k];
                Joueur b = slots[k + 1];
                Match srcA = sourceParSlot[k];
                Match srcB = sourceParSlot[k + 1];
                int idxSuivant = k / 2;

                boolean aReel = a != null || srcA != null;
                boolean bReel = b != null || srcB != null;

                if (aReel && bReel) {
                    // Un match a lieu ici. Les joueurs connus sont placés, les
                    // inconnus deviennent des placeholders "à déterminer".
                    Joueur ja = (a != null) ? a : Joueur.aDeterminer();
                    Joueur jb = (b != null) ? b : Joueur.aDeterminer();
                    Match match = new Match(ja, jb);
                    match.setSourceA(srcA);
                    match.setSourceB(srcB);
                    if (srcA != null || srcB != null) {
                        match.setStatut(StatutMatch.EN_ATTENTE);
                    }
                    tour.ajouterMatch(match);
                    // Le vainqueur de ce match alimentera le slot au niveau suivant.
                    slotsSuivant[idxSuivant] = null;
                    sourceSuivant[idxSuivant] = match;
                } else if (aReel) {
                    // Seul le haut est présent : il progresse sans jouer (exempté).
                    slotsSuivant[idxSuivant] = a;
                    sourceSuivant[idxSuivant] = srcA; // en général null ici
                } else if (bReel) {
                    slotsSuivant[idxSuivant] = b;
                    sourceSuivant[idxSuivant] = srcB;
                } else {
                    // Slot totalement vide (deux byes) : rien ne progresse.
                    slotsSuivant[idxSuivant] = null;
                    sourceSuivant[idxSuivant] = null;
                }
            }

            // On n'ajoute le tour que s'il contient au moins un match. Le premier
            // tour peut être "sauté" si toutes ses paires sont des exemptés (cas
            // où le cadre égale n : alors ce tour a bien des matchs, donc conservé).
            if (tour.getNombreMatchs() > 0) {
                tableau.ajouterTour(tour);
                numeroTour++;
            }

            slots = slotsSuivant;
            sourceParSlot = sourceSuivant;
            taille /= 2;
        }

        return tableau;
    }

    /** Plus petite puissance de 2 supérieure ou égale à n. */
    int puissanceDeDeuxSuperieureOuEgale(int n) {
        int p = 1;
        while (p < n) {
            p <<= 1;
        }
        return p;
    }

    /**
     * Génère l'ordre standard des seeds pour un cadre donné (puissance de 2).
     *
     * <p>L'algorithme construit récursivement le bracket de sorte que les seeds
     * forts soient répartis pour se rencontrer le plus tard possible. Le tableau
     * retourné est de longueur {@code cadre} ; les éléments consécutifs par
     * paires (0-1, 2-3, …) forment les affrontements du premier tour, en seeds
     * 1-based.</p>
     *
     * <p>Exemples : cadre 2 -&gt; [1,2] ; cadre 4 -&gt; [1,4,3,2] ;
     * cadre 8 -&gt; [1,8,5,4,3,6,7,2].</p>
     */
    int[] ordreDuCadre(int cadre) {
        int[] seeds = {1, 2};
        int taille = 2;
        while (taille < cadre) {
            int[] suivant = new int[taille * 2];
            int somme = taille * 2 + 1; // complément : seed i <-> (2*taille+1 - i)
            for (int i = 0; i < taille; i++) {
                suivant[2 * i] = seeds[i];
                suivant[2 * i + 1] = somme - seeds[i];
            }
            seeds = suivant;
            taille *= 2;
        }
        return seeds;
    }

    /**
     * Nom d'un tour d'après la taille du cadre traité à ce tour (nombre de
     * joueurs présents en début de tour).
     *
     * @param joueursDebutTour nombre de joueurs au début du tour (ex. 64, 32, 16…)
     */
    private String nomTourParCadre(int joueursDebutTour) {
        return switch (joueursDebutTour) {
            case 2 -> "Finale";
            case 4 -> "Demi-finales";
            case 8 -> "Quarts de finale";
            case 16 -> "8es de finale";
            case 32 -> "16es de finale";
            case 64 -> "32es de finale";
            case 128 -> "64es de finale";
            case 256 -> "128es de finale";
            default -> {
                int tiers = joueursDebutTour / 2;
                yield tiers + "es de finale";
            }
        };
    }
}
