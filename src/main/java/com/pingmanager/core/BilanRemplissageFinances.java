package com.pingmanager.core;

import java.util.List;
import java.util.Objects;

/**
 * Tableau de bord <b>remplissage et finances</b> d'un tournoi (B036), calculé par
 * {@link ServiceRemplissageFinances} : par tableau puis au total, inscrits sur
 * places, taux de remplissage, liste d'attente, recettes attendues, encaissées,
 * dotation promise (fixe + part des inscriptions) et solde.
 *
 * <p>Objet valeur immuable. Les montants sont en euros, arrondis au centime.</p>
 *
 * <p>Conventions (voir le journal de B036 pour les questions ouvertes) :</p>
 * <ul>
 *   <li>tarif retenu : celui de l'inscription en ligne (le sur place si l'en-ligne
 *       n'est pas proposé), comme {@link RemplissageTableaux#montantARegler} ;</li>
 *   <li>la liste d'attente ne paie rien tant que la place n'est pas obtenue ;</li>
 *   <li>la part des inscriptions se calcule sur les recettes attendues du
 *       tableau (tous les inscrits), pas seulement sur l'encaissé ;</li>
 *   <li>la dotation promise est la dotation pleine : une réduction prévue par
 *       les conditions de maintien n'est pas déduite ;</li>
 *   <li>solde = encaissé − dotation promise (négatif au départ) ; solde
 *       prévisionnel = recettes attendues − dotation promise.</li>
 * </ul>
 */
public record BilanRemplissageFinances(List<LigneTableau> tableaux) {

    public BilanRemplissageFinances {
        tableaux = List.copyOf(Objects.requireNonNull(tableaux, "tableaux"));
    }

    /**
     * Une ligne du tableau de bord : un tableau du tournoi.
     *
     * @param nom                 nom du tableau
     * @param inscrits            joueurs ayant une place
     * @param places              capacité (0 = sans limite de places)
     * @param enAttente           joueurs en liste d'attente (ne paient rien)
     * @param payes               inscrits ayant réglé
     * @param tarif               tarif retenu par inscription
     * @param recettesAttendues   tarif × inscrits
     * @param encaisse            tarif × inscrits ayant payé
     * @param dotationFixe        dotation en argent de la grille (chaque place d'une plage comptée)
     * @param partInscriptionsPct part des inscriptions promise, en % (0 à 100)
     * @param dotationPart        part des inscriptions, en euros
     */
    public record LigneTableau(String nom, int inscrits, int places, int enAttente, int payes, double tarif,
                               double recettesAttendues, double encaisse, double dotationFixe,
                               double partInscriptionsPct, double dotationPart) {

        /** @return vrai si le tableau n'a pas de limite de places. */
        public boolean sansLimite() {
            return places <= 0;
        }

        /** @return le taux de remplissage, entre 0 et 1 (0 sans limite de places). */
        public double taux() {
            return sansLimite() ? 0 : Math.min(1.0, inscrits / (double) places);
        }

        /** @return la dotation promise : fixe + part des inscriptions. */
        public double dotationPromise() {
            return centimes(dotationFixe + dotationPart);
        }

        /** @return ce qu'il reste à encaisser auprès des inscrits. */
        public double resteAEncaisser() {
            return centimes(recettesAttendues - encaisse);
        }

        /** @return encaissé − dotation promise. */
        public double solde() {
            return centimes(encaisse - dotationPromise());
        }

        /** @return recettes attendues − dotation promise (si tous les inscrits règlent). */
        public double soldePrevisionnel() {
            return centimes(recettesAttendues - dotationPromise());
        }
    }

    // --- Remplissage, au total -----------------------------------------------

    public int inscrits() {
        return tableaux.stream().mapToInt(LigneTableau::inscrits).sum();
    }

    public int enAttente() {
        return tableaux.stream().mapToInt(LigneTableau::enAttente).sum();
    }

    public int payes() {
        return tableaux.stream().mapToInt(LigneTableau::payes).sum();
    }

    /** @return le total des places des tableaux limités (les tableaux sans limite n'y comptent pas). */
    public int places() {
        return tableaux.stream().filter(l -> !l.sansLimite()).mapToInt(LigneTableau::places).sum();
    }

    /** @return le nombre de tableaux sans limite de places. */
    public int tableauxSansLimite() {
        return (int) tableaux.stream().filter(LigneTableau::sansLimite).count();
    }

    /**
     * Taux de remplissage global, entre 0 et 1, calculé sur les seuls tableaux
     * limités (inscrits de ces tableaux / leurs places) ; 0 s'il n'y en a aucun.
     */
    public double taux() {
        int places = places();
        if (places == 0) {
            return 0;
        }
        int occupes = tableaux.stream().filter(l -> !l.sansLimite())
                .mapToInt(l -> Math.min(l.inscrits(), l.places())).sum();
        return occupes / (double) places;
    }

    // --- Argent, au total ----------------------------------------------------

    public double recettesAttendues() {
        return centimes(tableaux.stream().mapToDouble(LigneTableau::recettesAttendues).sum());
    }

    public double encaisse() {
        return centimes(tableaux.stream().mapToDouble(LigneTableau::encaisse).sum());
    }

    public double resteAEncaisser() {
        return centimes(recettesAttendues() - encaisse());
    }

    public double dotationFixe() {
        return centimes(tableaux.stream().mapToDouble(LigneTableau::dotationFixe).sum());
    }

    public double dotationPart() {
        return centimes(tableaux.stream().mapToDouble(LigneTableau::dotationPart).sum());
    }

    public double dotationPromise() {
        return centimes(dotationFixe() + dotationPart());
    }

    /** @return encaissé − dotation promise : négatif au départ. */
    public double solde() {
        return centimes(encaisse() - dotationPromise());
    }

    /** @return recettes attendues − dotation promise (si tous les inscrits règlent). */
    public double soldePrevisionnel() {
        return centimes(recettesAttendues() - dotationPromise());
    }

    static double centimes(double v) {
        double r = Math.round(v * 100) / 100.0;
        return r == 0 ? 0 : r; // pas de « -0 »
    }
}
