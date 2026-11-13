package com.pingmanager.ui.ecran;

import com.pingmanager.core.buvette.BudgetCalcule;
import com.pingmanager.core.buvette.Centimes;
import com.pingmanager.core.buvette.ExportCsvBuvette;
import com.pingmanager.core.buvette.NombresBuvette;
import com.pingmanager.core.buvette.SuggestionsBuvette;
import com.pingmanager.core.reglement.Formats;
import com.pingmanager.model.buvette.HypothesesConsommation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Présentation (sans JavaFX) de l'onglet « Budget » de la page Buvette (B046) :
 * tuiles du scénario moyen, phrase d'affluence, joueurs par jour, comparaison des
 * scénarios, produits prévus et liste de courses (avec leur phrase
 * d'explication), valeurs du formulaire des hypothèses et exports.
 */
final class PresentationBudgetBuvette {

    record Tuile(String valeur, String libelle) {
    }

    record LigneJour(java.time.LocalDate jour, String libelle, String joueurs, String origine) {
    }

    record LigneScenario(String nom, String detail) {
    }

    record Ligne(String id, String titre, String detail, String pastille) {
    }

    private final BudgetCalcule budget;
    private final String nomTournoi;

    PresentationBudgetBuvette(BudgetCalcule budget, String nomTournoi) {
        this.budget = Objects.requireNonNull(budget, "budget");
        this.nomTournoi = nomTournoi == null ? "" : nomTournoi;
    }

    /** Tuiles du scénario moyen. */
    List<Tuile> tuiles() {
        BudgetCalcule.Scenario m = budget.moyen();
        return List.of(new Tuile(Centimes.euros(m.recetteCentimes()), "recette attendue (scénario moyen)"),
                new Tuile(Centimes.euros(m.coutAchatsCentimes()), "achats à prévoir"),
                new Tuile(Centimes.euros(m.margeCentimes()), "marge attendue"),
                new Tuile(m.seuilRentabilitePourcent().isPresent()
                        ? m.seuilRentabilitePourcent().getAsLong() + " %" : "—", "seuil de rentabilité"));
    }

    String affluence() {
        return budget.explicationAffluence() + ".";
    }

    String mentionHypotheses() {
        return SuggestionsBuvette.MENTION_HYPOTHESE + " : ces valeurs de départ ne sont pas des statistiques. "
                + SuggestionsBuvette.MENTION + ".";
    }

    List<LigneJour> jours() {
        List<LigneJour> res = new ArrayList<>();
        for (BudgetCalcule.JourBudget j : budget.jours()) {
            res.add(new LigneJour(j.jour(), Formats.capitale(Formats.jourLong(j.jour())), String.valueOf(j.joueurs()),
                    j.saisi() ? "saisi à la main" : "estimé d'après les inscriptions"));
        }
        return res;
    }

    List<LigneScenario> scenarios() {
        List<LigneScenario> res = new ArrayList<>();
        for (BudgetCalcule.Scenario s : budget.scenarios()) {
            res.add(new LigneScenario(s.nom() + " (" + s.pourcent() + " %)", "recette "
                    + Centimes.euros(s.recetteCentimes()) + " · achats " + Centimes.euros(s.coutAchatsCentimes())
                    + " · marge " + Centimes.euros(s.margeCentimes())));
        }
        return res;
    }

    /** « Les achats (312,00 €) sont remboursés dès 312,00 € de ventes, soit 46 % de la recette attendue. » */
    String phraseSeuil(String nomScenario) {
        BudgetCalcule.Scenario s = scenario(nomScenario);
        if (s.seuilRentabilitePourcent().isEmpty()) {
            return "Aucune vente prévue : renseignez les joueurs et les consommations.";
        }
        return "Les achats (" + Centimes.euros(s.coutAchatsCentimes()) + ") sont remboursés dès "
                + Centimes.euros(s.coutAchatsCentimes()) + " de ventes, soit " + s.seuilRentabilitePourcent().getAsLong()
                + " % de la recette attendue.";
    }

    BudgetCalcule.Scenario scenario(String nom) {
        for (BudgetCalcule.Scenario s : budget.scenarios()) {
            if (s.nom().equals(nom)) {
                return s;
            }
        }
        return budget.moyen();
    }

    List<Ligne> produits(String nomScenario) {
        List<Ligne> res = new ArrayList<>();
        for (BudgetCalcule.LigneProduit p : scenario(nomScenario).produits()) {
            String pastille = p.imposee() ? "Repris" : (p.sansRecette() ? "Sans recette" : "");
            res.add(new Ligne(p.idProduit(), p.nom() + " : " + p.quantite() + " · " + Centimes.euros(p.recetteCentimes()),
                    p.explication(), pastille));
        }
        return res;
    }

    List<Ligne> courses(String nomScenario) {
        List<Ligne> res = new ArrayList<>();
        for (BudgetCalcule.LigneCourse c : scenario(nomScenario).courses()) {
            res.add(new Ligne(c.idIngredient(), c.nom() + " : " + c.unites() + " × " + c.uniteAchat() + " · "
                    + Centimes.euros(c.coutCentimes()), c.explication(), c.unites() == 0 ? "En stock" : ""));
        }
        return res;
    }

    boolean sansProduit() {
        return budget.moyen().produits().isEmpty();
    }

    String messageSansProduit() {
        return "Aucun produit prévu : ajoutez des produits au catalogue et une consommation par personne.";
    }

    // Valeurs du formulaire des hypothèses
    String accompagnants() {
        return String.valueOf(hypotheses().accompagnantsPourcent());
    }

    String presence() {
        return NombresBuvette.ecrire(hypotheses().presenceDixiemes(), 1);
    }

    String scenarioBas() {
        return String.valueOf(hypotheses().scenarioBasPourcent());
    }

    String scenarioHaut() {
        return String.valueOf(hypotheses().scenarioHautPourcent());
    }

    String consommation(String idProduit) {
        return NombresBuvette.ecrire(hypotheses().consommation(idProduit), 2);
    }

    private HypothesesConsommation hypotheses() {
        return budget.hypotheses();
    }

    String csvCourses(String nomScenario) {
        return ExportCsvBuvette.listeCourses(nomTournoi, scenario(nomScenario));
    }

    /** Lignes du PDF de la liste de courses : ingrédient, quantité, coût. */
    List<List<String>> lignesPdfCourses(String nomScenario) {
        List<List<String>> res = new ArrayList<>();
        for (BudgetCalcule.LigneCourse c : scenario(nomScenario).courses()) {
            res.add(List.of(c.nom(), c.unites() + " × " + c.uniteAchat(), c.portionsNecessaires() + " portions",
                    Centimes.euros(c.coutCentimes())));
        }
        res.add(List.of("Total", "", "", Centimes.euros(scenario(nomScenario).coutAchatsCentimes())));
        return res;
    }

    String nomFichier(String nomScenario, String extension) {
        return "courses-buvette-" + com.pingmanager.model.buvette.CatalogueBuvette.slug(nomTournoi) + "-"
                + nomScenario.toLowerCase() + "." + extension;
    }
}
