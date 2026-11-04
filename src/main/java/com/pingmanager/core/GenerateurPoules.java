package com.pingmanager.core;

import com.pingmanager.model.Joueur;
import com.pingmanager.model.Poule;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Service métier chargé de répartir une liste de joueurs en poules équilibrées,
 * selon les principes de la FFTT.
 *
 * <p><b>Principes appliqués :</b></p>
 * <ol>
 *   <li><b>Nombre de poules.</b> On calcule un nombre de poules tel que chaque
 *       poule ait une taille proche de la taille souhaitée (3 ou 4), en
 *       privilégiant l'homogénéité. Les éventuels joueurs "en trop" agrandissent
 *       certaines poules d'une unité plutôt que de créer une poule trop petite.</li>
 *   <li><b>Répartition serpentin (têtes de série).</b> Les joueurs sont d'abord
 *       triés par classement décroissant, puis distribués en serpentin
 *       (1→2→3→3→2→1…) afin que la force soit équilibrée entre les poules et que
 *       les mieux classés ne se retrouvent pas ensemble dès les poules.</li>
 *   <li><b>Séparation des clubs.</b> Après la répartition serpentin, on tente
 *       d'échanger des joueurs entre poules pour éviter que deux licenciés d'un
 *       même club soient dans la même poule, quand c'est possible sans casser
 *       l'équilibre.</li>
 * </ol>
 *
 * <p>Ce service est sans état et sans dépendance à l'UI ou à la persistance :
 * il prend une liste de joueurs en entrée et retourne des poules.</p>
 */
public final class GenerateurPoules {

    /**
     * Génère les poules pour une liste de joueurs.
     *
     * @param joueurs           joueurs à répartir (non modifiée)
     * @param taillePouleSouhaitee taille cible d'une poule (3 ou 4 typiquement)
     * @param separerClubs      si vrai, tente de séparer les joueurs d'un même club
     * @return la liste des poules générées, nommées A, B, C, …
     * @throws IllegalArgumentException si la taille souhaitée est &lt; 3 ou s'il
     *         n'y a pas assez de joueurs pour former au moins une poule valide
     */
    public List<Poule> genererPoules(List<Joueur> joueurs,
                                     int taillePouleSouhaitee,
                                     boolean separerClubs) {
        // Sans graine : ordre stable (les ex æquo gardent leur ordre d'entrée).
        return genererPoules(joueurs, taillePouleSouhaitee, separerClubs, null);
    }

    /**
     * Variante avec départage aléatoire reproductible des joueurs à égalité de
     * points, conformément à l'article I.302 (« en cas d'égalité, un tirage au
     * sort est effectué »).
     *
     * @param joueurs           joueurs à répartir
     * @param taillePouleSouhaitee taille cible d'une poule
     * @param separerClubs      si vrai, sépare les joueurs d'un même club
     * @param graine            graine du tirage de départage ; {@code null} pour
     *                          conserver l'ordre stable sans tirage
     * @return les poules générées
     */
    public List<Poule> genererPoules(List<Joueur> joueurs,
                                     int taillePouleSouhaitee,
                                     boolean separerClubs,
                                     Long graine) {
        Objects.requireNonNull(joueurs, "joueurs");
        if (taillePouleSouhaitee < 3) {
            throw new IllegalArgumentException("La taille de poule souhaitée doit être >= 3.");
        }
        int n = joueurs.size();
        if (n < 3) {
            throw new IllegalArgumentException(
                    "Il faut au moins 3 joueurs pour former une poule (reçu : " + n + ").");
        }

        int nombrePoules = calculerNombreDePoules(n, taillePouleSouhaitee);

        List<Joueur> tries = new ArrayList<>(joueurs);
        // Départage des ex æquo (I.302) : on mélange d'abord aléatoirement, puis
        // on trie de façon STABLE par points décroissants. Les joueurs à points
        // égaux conservent alors l'ordre issu du mélange, c'est-à-dire un ordre
        // aléatoire mais reproductible (via la graine). Sans graine, on garde
        // l'ordre d'entrée (tri stable direct).
        if (graine != null) {
            long graineBrouillee = new java.util.SplittableRandom(graine).nextLong();
            java.util.Collections.shuffle(tries, new java.util.Random(graineBrouillee));
        }
        tries.sort(Comparator.comparingInt(Joueur::getPoints).reversed());

        // Préparation des groupes vides.
        List<List<Joueur>> groupes = new ArrayList<>();
        for (int i = 0; i < nombrePoules; i++) {
            groupes.add(new ArrayList<>());
        }

        // Répartition en serpentin.
        distribuerEnSerpentin(tries, groupes);

        // Séparation des clubs (best effort).
        if (separerClubs) {
            separerLesClubs(groupes);
        }

        // Construction des objets Poule nommés A, B, C…
        List<Poule> poules = new ArrayList<>();
        for (int i = 0; i < groupes.size(); i++) {
            String nom = nomDePoule(i);
            poules.add(new Poule(nom, groupes.get(i)));
        }
        return poules;
    }

    /**
     * Calcule le nombre de poules pour n joueurs et une taille souhaitée.
     *
     * <p>On part de l'arrondi au plus proche de n / taille, borné à au moins 1,
     * puis on s'assure qu'aucune poule ne descendrait sous 3 joueurs. Si c'est
     * le cas, on réduit le nombre de poules pour agrandir les poules restantes,
     * quitte à dépasser légèrement la taille souhaitée.</p>
     */
    int calculerNombreDePoules(int n, int taillePouleSouhaitee) {
        int nombrePoules = Math.max(1, Math.round((float) n / taillePouleSouhaitee));

        // Tant que la plus petite poule aurait moins de 3 joueurs, on retire une poule.
        while (nombrePoules > 1 && (n / nombrePoules) < 3) {
            nombrePoules--;
        }
        // Cas limite : la répartition laisserait un reste créant une poule < 3.
        while (nombrePoules > 1 && tailleMinimale(n, nombrePoules) < 3) {
            nombrePoules--;
        }
        return nombrePoules;
    }

    /** Taille de la plus petite poule si n joueurs sont répartis en p poules. */
    private int tailleMinimale(int n, int p) {
        return n / p; // les poules ont soit floor(n/p) soit floor(n/p)+1 joueurs
    }

    /**
     * Distribue les joueurs triés dans les groupes en serpentin :
     * poule 0,1,2,…,p-1 puis p-1,…,1,0 puis 0,1,… etc.
     */
    private void distribuerEnSerpentin(List<Joueur> tries, List<List<Joueur>> groupes) {
        int p = groupes.size();
        int index = 0;
        boolean sensDirect = true;
        for (Joueur joueur : tries) {
            groupes.get(index).add(joueur);
            if (sensDirect) {
                index++;
                if (index == p) {
                    index = p - 1;
                    sensDirect = false;
                }
            } else {
                index--;
                if (index < 0) {
                    index = 0;
                    sensDirect = true;
                }
            }
        }
    }

    /**
     * Tente de séparer les joueurs d'un même club présents dans une même poule.
     *
     * <p>Approche best-effort : pour chaque doublon de club détecté dans un
     * groupe, on cherche dans un autre groupe un joueur à échanger (idéalement
     * de niveau proche) qui n'introduit pas de nouveau conflit. Si aucun échange
     * n'est possible, le doublon est laissé tel quel (cas de clubs très
     * nombreux).</p>
     */
    private void separerLesClubs(List<List<Joueur>> groupes) {
        int p = groupes.size();
        if (p < 2) {
            return;
        }
        for (int g = 0; g < p; g++) {
            List<Joueur> groupe = groupes.get(g);
            for (int i = 0; i < groupe.size(); i++) {
                Joueur joueur = groupe.get(i);
                if (!clubEnDouble(groupe, joueur, i)) {
                    continue;
                }
                // Chercher un échange dans un autre groupe.
                int idxEchange = trouverEchange(groupes, g, joueur);
                if (idxEchange >= 0) {
                    List<Joueur> autre = groupes.get(idxEchange);
                    Joueur candidat = choisirCandidatEchange(autre, joueur, groupe);
                    if (candidat != null) {
                        groupe.set(i, candidat);
                        autre.remove(candidat);
                        autre.add(joueur);
                    }
                }
            }
        }
    }

    /** Un joueur a-t-il un homonyme de club dans son groupe (hors lui-même) ? */
    private boolean clubEnDouble(List<Joueur> groupe, Joueur joueur, int indexJoueur) {
        String club = joueur.getClub();
        if (club == null || club.isBlank()) {
            return false;
        }
        for (int k = 0; k < groupe.size(); k++) {
            if (k != indexJoueur && club.equalsIgnoreCase(groupe.get(k).getClub())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Cherche un groupe (autre que celui d'origine) dans lequel on pourrait
     * placer le joueur sans créer de conflit de club, et qui possède un
     * candidat échangeable.
     *
     * @return l'index du groupe cible, ou -1 si aucun.
     */
    private int trouverEchange(List<List<Joueur>> groupes, int groupeOrigine, Joueur joueur) {
        for (int g = 0; g < groupes.size(); g++) {
            if (g == groupeOrigine) {
                continue;
            }
            List<Joueur> cible = groupes.get(g);
            boolean conflit = cible.stream()
                    .anyMatch(j -> j.getClub().equalsIgnoreCase(joueur.getClub())
                            && !joueur.getClub().isBlank());
            if (!conflit) {
                return g;
            }
        }
        return -1;
    }

    /**
     * Dans le groupe cible, choisit un joueur à renvoyer vers le groupe d'origine.
     * On évite de créer un nouveau conflit de club dans le groupe d'origine et
     * on privilégie un joueur de niveau proche pour préserver l'équilibre.
     */
    private Joueur choisirCandidatEchange(List<Joueur> cible, Joueur joueur, List<Joueur> groupeOrigine) {
        Joueur meilleur = null;
        int meilleurEcart = Integer.MAX_VALUE;
        for (Joueur candidat : cible) {
            boolean creeConflit = groupeOrigine.stream()
                    .anyMatch(j -> !j.equals(joueur)
                            && j.getClub().equalsIgnoreCase(candidat.getClub())
                            && !candidat.getClub().isBlank());
            if (creeConflit) {
                continue;
            }
            int ecart = Math.abs(candidat.getPoints() - joueur.getPoints());
            if (ecart < meilleurEcart) {
                meilleurEcart = ecart;
                meilleur = candidat;
            }
        }
        return meilleur;
    }

    /** Nomme les poules A, B, …, Z, puis AA, AB… au-delà de 26. */
    private String nomDePoule(int index) {
        StringBuilder sb = new StringBuilder();
        int i = index;
        do {
            sb.insert(0, (char) ('A' + (i % 26)));
            i = i / 26 - 1;
        } while (i >= 0);
        return sb.toString();
    }
}
