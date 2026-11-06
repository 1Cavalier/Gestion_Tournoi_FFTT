package com.pingmanager.ui.ecran;

import com.pingmanager.core.ResultatEditionSalles;
import com.pingmanager.core.ServicePlanSalle;
import com.pingmanager.core.TableauDeBordSalle;
import com.pingmanager.model.ElementPlan;
import com.pingmanager.model.ElementPlan.AlleePlan;
import com.pingmanager.model.ElementPlan.ReperePlan;
import com.pingmanager.model.ElementPlan.TablePlan;
import com.pingmanager.model.OrientationTable;
import com.pingmanager.model.PlanSalle;
import com.pingmanager.model.ResultatPlacementPlan;
import com.pingmanager.model.SalleTournoi;
import com.pingmanager.model.Tournoi;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Mots et disposition du plan de salle (B024), sans JavaFX : tuiles du plan
 * (éditeur et vue Salle du jour J), mini-carte des salles par étage, messages
 * des opérations et alertes de cohérence. Testé par {@code PresentationPlanSalleTest}.
 */
final class PresentationPlanSalle {

    static final String TITRE_CARTE = "Plan de la salle";

    private PresentationPlanSalle() {
    }

    // ------------------------------------------------------------------
    // Tuiles du plan
    // ------------------------------------------------------------------

    /**
     * Un élément du plan tel qu'il se dessine : position et taille en cases,
     * texte court, info-bulle et classes de style.
     */
    record Tuile(ElementPlan element, int colonne, int ligne, int largeur, int hauteur, String texte,
                 String infobulle, List<String> classes) {
    }

    static List<Tuile> tuiles(PlanSalle plan) {
        List<Tuile> tuiles = new ArrayList<>();
        for (ElementPlan e : plan.getElements()) {
            List<String> classes = new ArrayList<>();
            classes.add("plan-element");
            if (e instanceof TablePlan t) {
                classes.add("plan-table");
                classes.add(t.orientation() == OrientationTable.VERTICALE ? "plan-table-verticale" : "plan-table-horizontale");
                if (t.tableauReserve() != null) {
                    classes.add("plan-table-reservee");
                }
            } else if (e instanceof AlleePlan) {
                classes.add("plan-allee");
            } else {
                classes.add("plan-repere");
            }
            tuiles.add(new Tuile(e, e.colonne(), e.ligne(), e.largeur(), e.hauteur(), texte(e), infobulle(e),
                    List.copyOf(classes)));
        }
        return tuiles;
    }

    /** Texte court dessiné dans l'élément : numéro de table, sigle du repère, rien pour une allée. */
    static String texte(ElementPlan e) {
        if (e instanceof TablePlan t) {
            return String.valueOf(t.numero());
        }
        if (e instanceof ReperePlan r) {
            // Un repère d'au moins trois cases de large affiche son libellé, sinon son sigle.
            return r.largeur() >= 3 ? r.libelle() : r.type().getSigle();
        }
        return "";
    }

    static String infobulle(ElementPlan e) {
        if (e instanceof TablePlan t) {
            return "Table " + t.numero() + " · " + t.orientation().getLibelle().toLowerCase()
                    + (t.tableauReserve() == null ? "" : " · réservée au tableau " + t.tableauReserve());
        }
        if (e instanceof ReperePlan r) {
            return r.libelle() + (r.libelle().equals(r.type().getLibelle()) ? "" : " (" + r.type().getLibelle() + ")");
        }
        return "Allée";
    }

    /**
     * Colonnes (ou lignes) où passe au moins une table : elles prennent la taille
     * d'une demi-table dans la vue du jour J, les autres (allées, repères) restent
     * étroites.
     *
     * @param colonnes vrai pour les colonnes, faux pour les lignes
     */
    static boolean[] avecTable(PlanSalle plan, boolean colonnes) {
        boolean[] r = new boolean[colonnes ? plan.getColonnes() : plan.getLignes()];
        for (TablePlan t : plan.getTables()) {
            int debut = colonnes ? t.colonne() : t.ligne();
            for (int k = debut; k < debut + TablePlan.TAILLE && k < r.length; k++) {
                r[k] = true;
            }
        }
        return r;
    }

    // ------------------------------------------------------------------
    // Mini-carte des salles
    // ------------------------------------------------------------------

    /** Une salle sur la mini-carte. */
    record SalleMiniCarte(int index, String nom, int tables, String detail, boolean courante) {
    }

    /** Un étage de la mini-carte et ses salles, dans l'ordre du tournoi. */
    record EtageMiniCarte(int etage, String libelle, List<SalleMiniCarte> salles) {
    }

    /** Mini-carte : les étages du plus haut au plus bas, chacun avec ses salles. */
    static List<EtageMiniCarte> miniCarte(List<SalleTournoi> salles, int courante) {
        Map<Integer, List<SalleMiniCarte>> parEtage = new LinkedHashMap<>();
        for (int i = 0; i < salles.size(); i++) {
            SalleTournoi s = salles.get(i);
            int n = s.getPlan().getTables().size();
            parEtage.computeIfAbsent(s.getEtage(), k -> new ArrayList<>())
                    .add(new SalleMiniCarte(i, s.getNom(), n, nombreDeTables(n) + resumeReservations(s), i == courante));
        }
        List<EtageMiniCarte> etages = new ArrayList<>();
        parEtage.forEach((etage, liste) -> etages.add(new EtageMiniCarte(etage, SalleTournoi.libelleEtage(etage),
                List.copyOf(liste))));
        etages.sort(Comparator.comparingInt(EtageMiniCarte::etage).reversed());
        return etages;
    }

    /** @return vrai si la mini-carte vaut la peine d'être montrée (plusieurs salles). */
    static boolean afficheMiniCarte(List<SalleTournoi> salles) {
        return salles.size() > 1;
    }

    static String nombreDeTables(int n) {
        return n == 0 ? "aucune table" : n + (n > 1 ? " tables" : " table");
    }

    /** « · réservée à Dames · préférée par 1500 », ou rien. */
    static String resumeReservations(SalleTournoi s) {
        StringBuilder b = new StringBuilder();
        if (!s.getTableauxReserves().isEmpty()) {
            b.append(" · réservée à ").append(PresentationSalle.listeLisible(new ArrayList<>(s.getTableauxReserves())));
        }
        if (!s.getTableauxPreferes().isEmpty()) {
            b.append(" · préférée par ").append(PresentationSalle.listeLisible(new ArrayList<>(s.getTableauxPreferes())));
        }
        return b.toString();
    }

    // ------------------------------------------------------------------
    // Carte de l'écran du tournoi
    // ------------------------------------------------------------------

    /** Phrase de la carte « Plan de la salle » de l'écran du tournoi (deuxième version). */
    static String phraseCarte(Tournoi tournoi, ServicePlanSalle service) {
        if (!tournoi.aUnPlanDeSalle()) {
            int n = tournoi.getNombreDeTables();
            return n == 0
                    ? "Pas encore de plan. Indiquez d'abord le nombre de tables (Informations sportives et techniques)."
                    : "Pas encore de plan : les " + nombreDeTables(n) + " sont rangées en grille par défaut.";
        }
        List<SalleTournoi> salles = tournoi.getSalles();
        int tables = 0;
        for (SalleTournoi s : salles) {
            tables += s.getPlan().getTables().size();
        }
        String phrase = (salles.size() > 1 ? salles.size() + " salles, " : "1 salle, ") + nombreDeTables(tables)
                + (tables > 1 ? " placées." : tables == 1 ? " placée." : ".");
        return phrase;
    }

    /** Alertes de cohérence du plan, à montrer dans l'éditeur et sur la carte. */
    static List<String> alertes(ServicePlanSalle.BilanPlanSalles bilan) {
        List<String> alertes = new ArrayList<>();
        if (!bilan.tablesHorsPlan().isEmpty()) {
            alertes.add(phraseTables(bilan.tablesHorsPlan(),
                    " n'est sur aucun plan : elle s'affichera à part dans la salle du jour J.",
                    " ne sont sur aucun plan : elles s'afficheront à part dans la salle du jour J."));
        }
        if (!bilan.tablesEnTrop().isEmpty()) {
            alertes.add(phraseTables(bilan.tablesEnTrop(),
                    " dépasse le nombre de tables du tournoi : elle ne recevra pas de match.",
                    " dépassent le nombre de tables du tournoi : elles ne recevront pas de match."));
        }
        if (!bilan.tableauxInconnus().isEmpty()) {
            List<String> noms = new ArrayList<>(new TreeSet<>(bilan.tableauxInconnus()));
            alertes.add("Réservation vers " + (noms.size() > 1 ? "des tableaux qui n'existent plus : "
                    : "un tableau qui n'existe plus : ") + PresentationSalle.listeLisible(noms) + ".");
        }
        return alertes;
    }

    private static String phraseTables(Set<Integer> numeros, String singulier, String pluriel) {
        List<String> t = new ArrayList<>();
        new TreeSet<>(numeros).forEach(n -> t.add(String.valueOf(n)));
        return (t.size() > 1 ? "Les tables " : "La table ") + PresentationSalle.listeLisible(t)
                + (t.size() > 1 ? pluriel : singulier);
    }

    // ------------------------------------------------------------------
    // Messages
    // ------------------------------------------------------------------

    static String message(ResultatPlacementPlan r) {
        ResultatPlacementPlan.Motif m = r.getMotif();
        String gene = r.getConflit() == null ? "un autre élément" : r.getConflit().designation();
        if (m == ResultatPlacementPlan.Motif.PLACE) {
            return "Élément posé sur le plan.";
        }
        if (m == ResultatPlacementPlan.Motif.DEPLACE) {
            return "Élément déplacé.";
        }
        if (m == ResultatPlacementPlan.Motif.RETIRE) {
            return "Élément retiré du plan.";
        }
        if (m == ResultatPlacementPlan.Motif.MODIFIE) {
            return "Modification faite.";
        }
        if (m == ResultatPlacementPlan.Motif.REDIMENSIONNE) {
            return "Plan mis à jour.";
        }
        if (m == ResultatPlacementPlan.Motif.HORS_GRILLE) {
            return "Il n'y a pas la place ici : l'élément dépasserait du plan (une table occupe 2 × 2 cases). "
                    + "Agrandissez la grille ou choisissez une autre case.";
        }
        if (m == ResultatPlacementPlan.Motif.CHEVAUCHEMENT) {
            return "Cet emplacement est déjà pris par " + gene + ".";
        }
        if (m == ResultatPlacementPlan.Motif.NUMERO_INVALIDE) {
            return "Le numéro d'une table est un nombre entier à partir de 1.";
        }
        if (m == ResultatPlacementPlan.Motif.NUMERO_DEJA_PRIS) {
            return "Ce numéro est déjà celui de " + gene + " : deux tables ne portent jamais le même numéro.";
        }
        if (m == ResultatPlacementPlan.Motif.ELEMENT_INTROUVABLE) {
            return "Rien à modifier à cet endroit.";
        }
        if (m == ResultatPlacementPlan.Motif.DIMENSIONS_INVALIDES) {
            return "Taille impossible : de 1 à " + PlanSalle.COTE_MAXIMAL + " cases de côté.";
        }
        if (m == ResultatPlacementPlan.Motif.GRILLE_TROP_PETITE) {
            return "La grille ne peut pas être aussi petite : " + gene + " en sortirait. Déplacez-le d'abord.";
        }
        return m.name();
    }

    static String message(ResultatEditionSalles r) {
        ResultatEditionSalles.Motif m = r.getMotif();
        if (m == ResultatEditionSalles.Motif.SALLE_AJOUTEE) {
            return "Salle « " + r.getPrecision() + " » ajoutée.";
        }
        if (m == ResultatEditionSalles.Motif.SALLE_RETIREE) {
            return "Salle « " + r.getPrecision() + " » retirée avec ses tables.";
        }
        if (m == ResultatEditionSalles.Motif.SALLE_MODIFIEE) {
            return "Salle « " + r.getPrecision() + " » mise à jour.";
        }
        if (m == ResultatEditionSalles.Motif.NOM_VIDE) {
            return "Donnez un nom à la salle (par exemple « Gymnase » ou « Dojo »).";
        }
        if (m == ResultatEditionSalles.Motif.NOM_DEJA_PRIS) {
            return "Une salle s'appelle déjà « " + r.getPrecision() + " ».";
        }
        if (m == ResultatEditionSalles.Motif.DERNIERE_SALLE) {
            return "Le tournoi garde au moins une salle.";
        }
        if (m == ResultatEditionSalles.Motif.SALLE_INTROUVABLE) {
            return "Cette salle n'existe plus.";
        }
        if (m == ResultatEditionSalles.Motif.TABLEAU_INCONNU) {
            return "Le tableau « " + r.getPrecision() + " » n'existe pas dans ce tournoi.";
        }
        return m.name();
    }

    // ------------------------------------------------------------------
    // Vue Salle du jour J
    // ------------------------------------------------------------------

    /** @return les tables de 1 à N qui ne sont sur aucun plan (affichées à part). */
    static List<Integer> tablesHorsPlan(List<SalleTournoi> salles, int nombreDeTables) {
        Set<Integer> placees = new TreeSet<>();
        for (SalleTournoi s : salles) {
            s.getPlan().getTables().forEach(t -> placees.add(t.numero()));
        }
        List<Integer> hors = new ArrayList<>();
        for (int i = 1; i <= nombreDeTables; i++) {
            if (!placees.contains(i)) {
                hors.add(i);
            }
        }
        return hors;
    }

    /** « 3 / 12 tables occupées » pour une salle du jour J (tables en compétition seulement). */
    static String occupation(SalleTournoi salle, TableauDeBordSalle bord) {
        int total = 0;
        int occupees = 0;
        for (TablePlan t : salle.getPlan().getTables()) {
            TableauDeBordSalle.CarteTable c = t.numero() <= bord.getNombreDeTables() ? bord.table(t.numero()) : null;
            if (c != null) {
                total++;
                if (!c.estLibre()) {
                    occupees++;
                }
            }
        }
        return occupees + " / " + total + (occupees > 1 ? " tables occupées" : " table occupée");
    }
}
