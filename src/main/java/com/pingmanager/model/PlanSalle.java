package com.pingmanager.model;

import com.pingmanager.model.ElementPlan.AlleePlan;
import com.pingmanager.model.ElementPlan.ReperePlan;
import com.pingmanager.model.ElementPlan.TablePlan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 * Plan d'une salle sur une grille (B024, cahier des charges v0, section 6) :
 * tables de 2 × 2 cases, allées de 1 × 1, repères (table d'arbitrage, entrée,
 * WC…). Les cases sont numérotées à partir de 0, colonne puis ligne.
 *
 * <p>Objet immuable : chaque modification rend un {@link ResultatPlacementPlan}
 * qui porte le nouveau plan ou, en cas de refus, le motif (hors de la grille,
 * chevauchement, numéro déjà pris…) et le plan d'origine. Deux éléments ne se
 * recouvrent jamais et aucun ne dépasse de la grille ; deux tables d'un même
 * plan n'ont jamais le même numéro.</p>
 */
public final class PlanSalle {

    /** Taille maximale d'un côté de la grille, en cases. */
    public static final int COTE_MAXIMAL = 120;

    private final int colonnes;
    private final int lignes;
    private final List<ElementPlan> elements;

    private PlanSalle(int colonnes, int lignes, List<ElementPlan> elements) {
        this.colonnes = colonnes;
        this.lignes = lignes;
        this.elements = List.copyOf(elements);
    }

    /**
     * Plan vide.
     *
     * @throws IllegalArgumentException si une dimension sort de 1 à {@link #COTE_MAXIMAL}
     */
    public static PlanSalle vide(int colonnes, int lignes) {
        if (!dimensionValable(colonnes) || !dimensionValable(lignes)) {
            throw new IllegalArgumentException("La grille doit compter de 1 à " + COTE_MAXIMAL
                    + " cases de côté (reçu " + colonnes + " × " + lignes + ").");
        }
        return new PlanSalle(colonnes, lignes, List.of());
    }

    /**
     * Reconstruit un plan relu (fichier) : les éléments sont posés dans l'ordre et
     * ceux qui ne tiennent pas (hors grille, chevauchement, numéro en double) sont
     * écartés, pour qu'un fichier abîmé s'ouvre quand même.
     */
    public static PlanSalle reconstruire(int colonnes, int lignes, List<? extends ElementPlan> elements) {
        PlanSalle plan = vide(Math.max(1, Math.min(COTE_MAXIMAL, colonnes)),
                Math.max(1, Math.min(COTE_MAXIMAL, lignes)));
        for (ElementPlan e : elements) {
            plan = plan.ajouter(e).getPlan();
        }
        return plan;
    }

    private static boolean dimensionValable(int n) {
        return n >= 1 && n <= COTE_MAXIMAL;
    }

    // ------------------------------------------------------------------
    // Lecture
    // ------------------------------------------------------------------

    public int getColonnes() {
        return colonnes;
    }

    public int getLignes() {
        return lignes;
    }

    /** @return tous les éléments, dans l'ordre où ils ont été posés. */
    public List<ElementPlan> getElements() {
        return elements;
    }

    /** @return les tables, par numéro croissant. */
    public List<TablePlan> getTables() {
        List<TablePlan> tables = new ArrayList<>();
        for (ElementPlan e : elements) {
            if (e instanceof TablePlan t) {
                tables.add(t);
            }
        }
        tables.sort(Comparator.comparingInt(TablePlan::numero));
        return Collections.unmodifiableList(tables);
    }

    public List<AlleePlan> getAllees() {
        List<AlleePlan> allees = new ArrayList<>();
        for (ElementPlan e : elements) {
            if (e instanceof AlleePlan a) {
                allees.add(a);
            }
        }
        return Collections.unmodifiableList(allees);
    }

    public List<ReperePlan> getReperes() {
        List<ReperePlan> reperes = new ArrayList<>();
        for (ElementPlan e : elements) {
            if (e instanceof ReperePlan r) {
                reperes.add(r);
            }
        }
        return Collections.unmodifiableList(reperes);
    }

    /** @return la table portant ce numéro, si elle est sur le plan. */
    public Optional<TablePlan> table(int numero) {
        for (ElementPlan e : elements) {
            if (e instanceof TablePlan t && t.numero() == numero) {
                return Optional.of(t);
            }
        }
        return Optional.empty();
    }

    /** @return l'élément qui recouvre la case, s'il y en a un. */
    public Optional<ElementPlan> elementEn(int colonne, int ligne) {
        for (ElementPlan e : elements) {
            if (e.couvre(colonne, ligne)) {
                return Optional.of(e);
            }
        }
        return Optional.empty();
    }

    /** @return vrai si un élément de cette taille tient à cet endroit, sans rien recouvrir. */
    public boolean peutPoser(int colonne, int ligne, int largeur, int hauteur) {
        return dansLaGrille(colonne, ligne, largeur, hauteur) && conflit(colonne, ligne, largeur, hauteur, null) == null;
    }

    /**
     * Première place libre pour un élément de cette taille, en lisant le plan ligne
     * par ligne de gauche à droite (case en haut à gauche), ou vide si le plan est plein.
     */
    public Optional<int[]> premierePlaceLibre(int largeur, int hauteur) {
        for (int l = 0; l + hauteur <= lignes; l++) {
            for (int c = 0; c + largeur <= colonnes; c++) {
                if (peutPoser(c, l, largeur, hauteur)) {
                    return Optional.of(new int[] {c, l});
                }
            }
        }
        return Optional.empty();
    }

    // ------------------------------------------------------------------
    // Modifications (chacune rend un nouveau plan)
    // ------------------------------------------------------------------

    /** Pose un nouvel élément. */
    public ResultatPlacementPlan ajouter(ElementPlan element) {
        Objects.requireNonNull(element, "element");
        ResultatPlacementPlan refus = verifier(element, null);
        if (refus != null) {
            return refus;
        }
        List<ElementPlan> nouveaux = new ArrayList<>(elements);
        nouveaux.add(element);
        return ResultatPlacementPlan.reussite(ResultatPlacementPlan.Motif.PLACE,
                new PlanSalle(colonnes, lignes, nouveaux));
    }

    /** Déplace l'élément qui recouvre la case (colonne, ligne) : sa case en haut à gauche va en (nouvelleColonne, nouvelleLigne). */
    public ResultatPlacementPlan deplacer(int colonne, int ligne, int nouvelleColonne, int nouvelleLigne) {
        Optional<ElementPlan> e = elementEn(colonne, ligne);
        if (e.isEmpty()) {
            return ResultatPlacementPlan.refus(ResultatPlacementPlan.Motif.ELEMENT_INTROUVABLE, this, null);
        }
        ResultatPlacementPlan r = remplacer(e.get(), e.get().placeEn(nouvelleColonne, nouvelleLigne));
        return r.estReussi() ? ResultatPlacementPlan.reussite(ResultatPlacementPlan.Motif.DEPLACE, r.getPlan()) : r;
    }

    /** Retire l'élément qui recouvre la case (colonne, ligne). */
    public ResultatPlacementPlan retirer(int colonne, int ligne) {
        Optional<ElementPlan> e = elementEn(colonne, ligne);
        if (e.isEmpty()) {
            return ResultatPlacementPlan.refus(ResultatPlacementPlan.Motif.ELEMENT_INTROUVABLE, this, null);
        }
        List<ElementPlan> nouveaux = new ArrayList<>(elements);
        nouveaux.remove(e.get());
        return ResultatPlacementPlan.reussite(ResultatPlacementPlan.Motif.RETIRE,
                new PlanSalle(colonnes, lignes, nouveaux));
    }

    /**
     * Remplace un élément du plan par sa version modifiée (numéro, orientation,
     * position, taille d'un repère…), vérifiée comme un nouvel élément sans tenir
     * compte de l'ancien.
     */
    public ResultatPlacementPlan remplacer(ElementPlan ancien, ElementPlan nouveau) {
        Objects.requireNonNull(nouveau, "nouveau");
        int index = elements.indexOf(ancien);
        if (index < 0) {
            return ResultatPlacementPlan.refus(ResultatPlacementPlan.Motif.ELEMENT_INTROUVABLE, this, null);
        }
        ResultatPlacementPlan refus = verifier(nouveau, elements.get(index));
        if (refus != null) {
            return refus;
        }
        List<ElementPlan> nouveaux = new ArrayList<>(elements);
        nouveaux.set(index, nouveau);
        return ResultatPlacementPlan.reussite(ResultatPlacementPlan.Motif.MODIFIE,
                new PlanSalle(colonnes, lignes, nouveaux));
    }

    /** Change le numéro d'une table (numérotation libre, unique dans le plan). */
    public ResultatPlacementPlan numeroter(int numeroActuel, int nouveauNumero) {
        return modifierTable(numeroActuel, t -> t.numerotee(nouveauNumero));
    }

    /** Tourne une table d'un quart de tour (horizontale ↔ verticale). */
    public ResultatPlacementPlan tourner(int numero) {
        return modifierTable(numero, t -> t.orientee(t.orientation().tournee()));
    }

    /** Réserve une table à un tableau (nom du tableau), ou lève la réservation (null). */
    public ResultatPlacementPlan reserver(int numero, String tableau) {
        return modifierTable(numero, t -> t.reservee(tableau));
    }

    private ResultatPlacementPlan modifierTable(int numero, UnaryOperator<TablePlan> modification) {
        Optional<TablePlan> t = table(numero);
        if (t.isEmpty()) {
            return ResultatPlacementPlan.refus(ResultatPlacementPlan.Motif.ELEMENT_INTROUVABLE, this, null);
        }
        return remplacer(t.get(), modification.apply(t.get()));
    }

    /** Change la taille de la grille, sans couper aucun élément déjà posé. */
    public ResultatPlacementPlan redimensionner(int nouvellesColonnes, int nouvellesLignes) {
        if (!dimensionValable(nouvellesColonnes) || !dimensionValable(nouvellesLignes)) {
            return ResultatPlacementPlan.refus(ResultatPlacementPlan.Motif.DIMENSIONS_INVALIDES, this, null);
        }
        for (ElementPlan e : elements) {
            if (e.colonne() + e.largeur() > nouvellesColonnes || e.ligne() + e.hauteur() > nouvellesLignes) {
                return ResultatPlacementPlan.refus(ResultatPlacementPlan.Motif.GRILLE_TROP_PETITE, this, e);
            }
        }
        return ResultatPlacementPlan.reussite(ResultatPlacementPlan.Motif.REDIMENSIONNE,
                new PlanSalle(nouvellesColonnes, nouvellesLignes, elements));
    }

    // ------------------------------------------------------------------
    // Vérifications
    // ------------------------------------------------------------------

    /** @return le refus qui empêche de poser l'élément (en ignorant {@code ignore}), ou null s'il tient. */
    private ResultatPlacementPlan verifier(ElementPlan e, ElementPlan ignore) {
        if (e.largeur() < 1 || e.hauteur() < 1 || e.largeur() > COTE_MAXIMAL || e.hauteur() > COTE_MAXIMAL) {
            return ResultatPlacementPlan.refus(ResultatPlacementPlan.Motif.DIMENSIONS_INVALIDES, this, null);
        }
        if (e instanceof TablePlan t) {
            if (t.numero() < 1) {
                return ResultatPlacementPlan.refus(ResultatPlacementPlan.Motif.NUMERO_INVALIDE, this, null);
            }
            for (ElementPlan autre : elements) {
                if (autre != ignore && autre instanceof TablePlan ta && ta.numero() == t.numero()) {
                    return ResultatPlacementPlan.refus(ResultatPlacementPlan.Motif.NUMERO_DEJA_PRIS, this, autre);
                }
            }
        }
        if (!dansLaGrille(e.colonne(), e.ligne(), e.largeur(), e.hauteur())) {
            return ResultatPlacementPlan.refus(ResultatPlacementPlan.Motif.HORS_GRILLE, this, null);
        }
        ElementPlan gene = conflit(e.colonne(), e.ligne(), e.largeur(), e.hauteur(), ignore);
        if (gene != null) {
            return ResultatPlacementPlan.refus(ResultatPlacementPlan.Motif.CHEVAUCHEMENT, this, gene);
        }
        return null;
    }

    private boolean dansLaGrille(int c, int l, int largeur, int hauteur) {
        return c >= 0 && l >= 0 && largeur >= 1 && hauteur >= 1
                && c + largeur <= colonnes && l + hauteur <= lignes;
    }

    private ElementPlan conflit(int c, int l, int largeur, int hauteur, ElementPlan ignore) {
        ElementPlan zone = new ReperePlan(TypeRepere.AUTRE, null, c, l, largeur, hauteur);
        for (ElementPlan autre : elements) {
            if (autre != ignore && autre.chevauche(zone)) {
                return autre;
            }
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof PlanSalle p && p.colonnes == colonnes && p.lignes == lignes && p.elements.equals(elements);
    }

    @Override
    public int hashCode() {
        return Objects.hash(colonnes, lignes, elements);
    }

    @Override
    public String toString() {
        return "PlanSalle[" + colonnes + " × " + lignes + ", " + getTables().size() + " tables]";
    }
}
