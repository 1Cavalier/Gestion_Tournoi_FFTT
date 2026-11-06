package com.pingmanager.core;

import com.pingmanager.model.ElementPlan;
import com.pingmanager.model.ElementPlan.AlleePlan;
import com.pingmanager.model.ElementPlan.ReperePlan;
import com.pingmanager.model.ElementPlan.TablePlan;
import com.pingmanager.model.PlanSalle;
import com.pingmanager.model.ResultatPlacementPlan;
import com.pingmanager.model.SalleTournoi;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.TypeRepere;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Brouillon de l'éditeur de plan (B024) : les salles du tournoi en cours de
 * modification et la salle affichée. Toutes les opérations rendent un résultat
 * à motif ; rien n'est écrit dans le tournoi avant {@link #appliquer(Tournoi)}.
 *
 * <p>La numérotation des tables est libre mais unique sur tout le tournoi :
 * une nouvelle table prend le plus petit numéro libre, toutes salles
 * confondues, et deux salles ne partagent jamais un numéro.</p>
 */
public final class EditionPlanSalles {

    private final List<SalleTournoi> salles = new ArrayList<>();
    private final Set<String> tableaux = new LinkedHashSet<>();
    private final int nombreDeTables;
    private int courante;

    /** Brouillon depuis les salles du tournoi (une salle en grille s'il n'a pas de plan). */
    public EditionPlanSalles(Tournoi tournoi) {
        Objects.requireNonNull(tournoi, "tournoi");
        salles.addAll(new ServicePlanSalle().salles(tournoi));
        for (Tableau t : tournoi.getTableaux()) {
            tableaux.add(t.getNom());
        }
        nombreDeTables = Math.max(0, tournoi.getNombreDeTables());
        courante = 0;
    }

    // ------------------------------------------------------------------
    // Lecture
    // ------------------------------------------------------------------

    public List<SalleTournoi> getSalles() {
        return Collections.unmodifiableList(salles);
    }

    public int getIndexCourant() {
        return courante;
    }

    public SalleTournoi salleCourante() {
        return salles.get(courante);
    }

    public PlanSalle planCourant() {
        return salleCourante().getPlan();
    }

    /** @return les noms des tableaux du tournoi (pour les réservations). */
    public Set<String> getTableaux() {
        return Collections.unmodifiableSet(tableaux);
    }

    /** @return le nombre de tables du tournoi (numéros 1 à N portés par les matchs). */
    public int getNombreDeTables() {
        return nombreDeTables;
    }

    /** @return le plus petit numéro de table libre, toutes salles confondues. */
    public int premierNumeroLibre() {
        return ServicePlanSalle.premierNumeroLibre(salles);
    }

    /** @return le nombre de tables posées, toutes salles confondues. */
    public int nombreDeTablesPosees() {
        int n = 0;
        for (SalleTournoi s : salles) {
            n += s.getPlan().getTables().size();
        }
        return n;
    }

    /** Choisit la salle affichée. */
    public ResultatEditionSalles choisirSalle(int index) {
        if (index < 0 || index >= salles.size()) {
            return ResultatEditionSalles.de(ResultatEditionSalles.Motif.SALLE_INTROUVABLE);
        }
        courante = index;
        return ResultatEditionSalles.de(ResultatEditionSalles.Motif.SALLE_MODIFIEE, salles.get(index).getNom());
    }

    // ------------------------------------------------------------------
    // Salles
    // ------------------------------------------------------------------

    /** Ajoute une salle vide (8 × 6 cases avec table d'arbitrage et entrée) et l'affiche. */
    public ResultatEditionSalles ajouterSalle(String nom, int etage) {
        ResultatEditionSalles refus = verifierNom(nom, -1);
        if (refus != null) {
            return refus;
        }
        salles.add(new SalleTournoi(nom.trim(), etage, PlansTypesSalle.generer(1, 0, 1), Set.of(), Set.of()));
        courante = salles.size() - 1;
        return ResultatEditionSalles.de(ResultatEditionSalles.Motif.SALLE_AJOUTEE, nom.trim());
    }

    public ResultatEditionSalles renommerSalle(String nom) {
        ResultatEditionSalles refus = verifierNom(nom, courante);
        if (refus != null) {
            return refus;
        }
        salles.set(courante, salleCourante().avecNom(nom.trim()));
        return ResultatEditionSalles.de(ResultatEditionSalles.Motif.SALLE_MODIFIEE, nom.trim());
    }

    public ResultatEditionSalles changerEtage(int etage) {
        salles.set(courante, salleCourante().avecEtage(etage));
        return ResultatEditionSalles.de(ResultatEditionSalles.Motif.SALLE_MODIFIEE, salleCourante().getNom());
    }

    /** Retire la salle affichée et ses tables ; le tournoi garde toujours une salle. */
    public ResultatEditionSalles retirerSalle() {
        if (salles.size() <= 1) {
            return ResultatEditionSalles.de(ResultatEditionSalles.Motif.DERNIERE_SALLE);
        }
        String nom = salles.remove(courante).getNom();
        courante = Math.min(courante, salles.size() - 1);
        return ResultatEditionSalles.de(ResultatEditionSalles.Motif.SALLE_RETIREE, nom);
    }

    /** Réserve la salle affichée à ces tableaux (vide : la salle reçoit tout le monde). */
    public ResultatEditionSalles reserverSalle(Set<String> noms) {
        Optional<String> inconnu = inconnu(noms);
        if (inconnu.isPresent()) {
            return ResultatEditionSalles.de(ResultatEditionSalles.Motif.TABLEAU_INCONNU, inconnu.get());
        }
        salles.set(courante, salleCourante().avecTableauxReserves(noms));
        return ResultatEditionSalles.de(ResultatEditionSalles.Motif.SALLE_MODIFIEE, salleCourante().getNom());
    }

    /** Ces tableaux jouent de préférence dans la salle affichée. */
    public ResultatEditionSalles preferer(Set<String> noms) {
        Optional<String> inconnu = inconnu(noms);
        if (inconnu.isPresent()) {
            return ResultatEditionSalles.de(ResultatEditionSalles.Motif.TABLEAU_INCONNU, inconnu.get());
        }
        salles.set(courante, salleCourante().avecTableauxPreferes(noms));
        return ResultatEditionSalles.de(ResultatEditionSalles.Motif.SALLE_MODIFIEE, salleCourante().getNom());
    }

    private Optional<String> inconnu(Set<String> noms) {
        Objects.requireNonNull(noms, "noms");
        return noms.stream().filter(n -> !tableaux.contains(n)).findFirst();
    }

    private ResultatEditionSalles verifierNom(String nom, int sauf) {
        if (nom == null || nom.isBlank()) {
            return ResultatEditionSalles.de(ResultatEditionSalles.Motif.NOM_VIDE);
        }
        String cle = nom.trim().toLowerCase(Locale.ROOT);
        for (int i = 0; i < salles.size(); i++) {
            if (i != sauf && salles.get(i).getNom().toLowerCase(Locale.ROOT).equals(cle)) {
                return ResultatEditionSalles.de(ResultatEditionSalles.Motif.NOM_DEJA_PRIS, salles.get(i).getNom());
            }
        }
        return null;
    }

    // ------------------------------------------------------------------
    // Plan de la salle affichée
    // ------------------------------------------------------------------

    /** Pose une table (plus petit numéro libre du tournoi) avec sa case en haut à gauche en (colonne, ligne). */
    public ResultatPlacementPlan ajouterTable(int colonne, int ligne) {
        return appliquerPlan(planCourant().ajouter(new TablePlan(premierNumeroLibre(), colonne, ligne)));
    }

    public ResultatPlacementPlan ajouterAllee(int colonne, int ligne) {
        return appliquerPlan(planCourant().ajouter(new AlleePlan(colonne, ligne)));
    }

    /** Pose un repère à sa taille par défaut. */
    public ResultatPlacementPlan ajouterRepere(TypeRepere type, int colonne, int ligne) {
        return appliquerPlan(planCourant().ajouter(new ReperePlan(type, colonne, ligne)));
    }

    public ResultatPlacementPlan deplacer(int colonne, int ligne, int nouvelleColonne, int nouvelleLigne) {
        return appliquerPlan(planCourant().deplacer(colonne, ligne, nouvelleColonne, nouvelleLigne));
    }

    public ResultatPlacementPlan retirer(int colonne, int ligne) {
        return appliquerPlan(planCourant().retirer(colonne, ligne));
    }

    /** Renumérote une table de la salle affichée ; le numéro doit être libre dans toutes les salles. */
    public ResultatPlacementPlan numeroter(int numeroActuel, int nouveauNumero) {
        PlanSalle plan = planCourant();
        if (nouveauNumero >= 1 && ServicePlanSalle.numerosAilleurs(salles, courante).contains(nouveauNumero)) {
            return ResultatPlacementPlan.refus(ResultatPlacementPlan.Motif.NUMERO_DEJA_PRIS, plan,
                    tableAilleurs(nouveauNumero));
        }
        return appliquerPlan(plan.numeroter(numeroActuel, nouveauNumero));
    }

    public ResultatPlacementPlan tourner(int numero) {
        return appliquerPlan(planCourant().tourner(numero));
    }

    /** Réserve une table à un tableau du tournoi, ou lève la réservation (null). */
    public ResultatPlacementPlan reserverTable(int numero, String tableau) {
        if (tableau != null && !tableau.isBlank() && !tableaux.contains(tableau.trim())) {
            return ResultatPlacementPlan.refus(ResultatPlacementPlan.Motif.ELEMENT_INTROUVABLE, planCourant(), null);
        }
        return appliquerPlan(planCourant().reserver(numero, tableau));
    }

    public ResultatPlacementPlan redimensionner(int colonnes, int lignes) {
        return appliquerPlan(planCourant().redimensionner(colonnes, lignes));
    }

    /**
     * Remplace le plan de la salle affichée par un plan type. Le nombre de tables
     * par défaut : celles du tournoi qui ne sont pas déjà dans une autre salle ;
     * numérotées à partir du premier numéro libre des autres salles.
     */
    public ResultatPlacementPlan appliquerPlanType(PlansTypesSalle.PlanType type, int tables) {
        Objects.requireNonNull(type, "type");
        if (tables < 0) {
            return ResultatPlacementPlan.refus(ResultatPlacementPlan.Motif.NUMERO_INVALIDE, planCourant(), null);
        }
        Set<Integer> ailleurs = ServicePlanSalle.numerosAilleurs(salles, courante);
        // Premier numéro à partir duquel toutes les tables du plan type sont libres.
        int premier = 1;
        while (!plageLibre(ailleurs, premier, tables)) {
            premier++;
        }
        PlanSalle plan;
        try {
            plan = PlansTypesSalle.generer(type.getRangees(), tables, premier);
        } catch (IllegalArgumentException e) {
            return ResultatPlacementPlan.refus(ResultatPlacementPlan.Motif.DIMENSIONS_INVALIDES, planCourant(), null);
        }
        salles.set(courante, salleCourante().avecPlan(plan));
        return ResultatPlacementPlan.reussite(ResultatPlacementPlan.Motif.REDIMENSIONNE, plan);
    }

    private static boolean plageLibre(Set<Integer> pris, int premier, int nombre) {
        for (int k = premier; k < premier + nombre; k++) {
            if (pris.contains(k)) {
                return false;
            }
        }
        return true;
    }

    /** @return les tables du tournoi qui ne sont dans aucune autre salle que celle affichée. */
    public int tablesRestantesPourLaSalle() {
        Set<Integer> ailleurs = new HashSet<>(ServicePlanSalle.numerosAilleurs(salles, courante));
        int n = 0;
        for (int i = 1; i <= nombreDeTables; i++) {
            if (!ailleurs.contains(i)) {
                n++;
            }
        }
        return n;
    }

    private ElementPlan tableAilleurs(int numero) {
        for (int i = 0; i < salles.size(); i++) {
            if (i != courante) {
                Optional<TablePlan> t = salles.get(i).getPlan().table(numero);
                if (t.isPresent()) {
                    return t.get();
                }
            }
        }
        return null;
    }

    private ResultatPlacementPlan appliquerPlan(ResultatPlacementPlan r) {
        if (r.estReussi()) {
            salles.set(courante, salleCourante().avecPlan(r.getPlan()));
        }
        return r;
    }

    /** Bilan de cohérence du brouillon (tables oubliées, en trop, tableaux inconnus). */
    public ServicePlanSalle.BilanPlanSalles bilan() {
        return new ServicePlanSalle().verifier(salles, nombreDeTables, tableaux, true);
    }

    // ------------------------------------------------------------------
    // Enregistrement
    // ------------------------------------------------------------------

    /** Écrit les salles dans le tournoi (à enregistrer ensuite par l'appelant). */
    public void appliquer(Tournoi tournoi) {
        tournoi.setSalles(salles);
    }
}
