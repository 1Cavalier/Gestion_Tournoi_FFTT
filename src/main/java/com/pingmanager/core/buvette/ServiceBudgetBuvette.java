package com.pingmanager.core.buvette;

import com.pingmanager.model.buvette.BuvetteTournoi;
import com.pingmanager.model.buvette.CatalogueBuvette;
import com.pingmanager.model.buvette.HypothesesConsommation;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.OptionalLong;

/**
 * Saisies du budget prévisionnel de la buvette (B046) : joueurs prévus par jour,
 * hypothèses (accompagnants, présence, scénarios), consommation d'un produit,
 * déduction du stock. Chaque saisie est contrôlée et renvoie un motif précis.
 */
public final class ServiceBudgetBuvette {

    /** Joueurs prévus d'un jour ; une saisie vide revient à l'estimation. */
    public ResultatBudgetBuvette saisirJoueurs(BuvetteTournoi buvette, LocalDate jour, String saisie) {
        if (saisie == null || saisie.isBlank()) {
            buvette.setBudget(buvette.getBudget().avecJoueurs(jour, null));
            return ok();
        }
        OptionalLong n = NombresBuvette.lire(saisie, 0);
        if (n.isEmpty() || n.getAsLong() > 5000) {
            return new ResultatBudgetBuvette(ResultatBudgetBuvette.Motif.NOMBRE_INVALIDE);
        }
        buvette.setBudget(buvette.getBudget().avecJoueurs(jour, (int) n.getAsLong()));
        return ok();
    }

    /** Hypothèses générales (les consommations par produit sont gardées). */
    public ResultatBudgetBuvette saisirHypotheses(BuvetteTournoi buvette, String accompagnants, String presence, String bas, String haut) {
        OptionalLong a = NombresBuvette.lire(accompagnants, 0);
        if (a.isEmpty() || a.getAsLong() > 500) {
            return new ResultatBudgetBuvette(ResultatBudgetBuvette.Motif.POURCENT_INVALIDE);
        }
        OptionalLong p = NombresBuvette.lire(presence, 1);
        if (p.isEmpty() || p.getAsLong() < 1 || p.getAsLong() > 80) {
            return new ResultatBudgetBuvette(ResultatBudgetBuvette.Motif.PRESENCE_INVALIDE);
        }
        OptionalLong b = NombresBuvette.lire(bas, 0);
        OptionalLong h = NombresBuvette.lire(haut, 0);
        if (b.isEmpty() || h.isEmpty() || b.getAsLong() < 1 || b.getAsLong() > 100 || h.getAsLong() < 100
                || h.getAsLong() > 500) {
            return new ResultatBudgetBuvette(ResultatBudgetBuvette.Motif.SCENARIOS_INVALIDES);
        }
        HypothesesConsommation actuelles = buvette.getBudget().hypotheses();
        Map<String, Integer> conso = actuelles == null ? Map.of() : actuelles.consommations();
        buvette.setBudget(buvette.getBudget().avecHypotheses(new HypothesesConsommation((int) a.getAsLong(),
                (int) p.getAsLong(), (int) b.getAsLong(), (int) h.getAsLong(), conso)));
        return ok();
    }

    /** Consommation d'un produit par personne et par demi-journée (« 0,35 »). */
    public ResultatBudgetBuvette saisirConsommation(BuvetteTournoi buvette, HypothesesConsommation suggestions,
                                                    CatalogueBuvette catalogue, String idProduit, String saisie) {
        if (catalogue.produit(idProduit).isEmpty()) {
            return new ResultatBudgetBuvette(ResultatBudgetBuvette.Motif.PRODUIT_INCONNU);
        }
        OptionalLong c = NombresBuvette.lire(saisie, 2);
        if (c.isEmpty() || c.getAsLong() > 1000) {
            return new ResultatBudgetBuvette(ResultatBudgetBuvette.Motif.CONSOMMATION_INVALIDE);
        }
        HypothesesConsommation base = buvette.getBudget().hypotheses() == null ? suggestions
                : buvette.getBudget().hypotheses();
        Map<String, Integer> conso = new LinkedHashMap<>(buvette.getBudget().hypotheses() == null ? Map.of()
                : base.consommations());
        conso.put(idProduit, (int) c.getAsLong());
        buvette.setBudget(buvette.getBudget().avecHypotheses(new HypothesesConsommation(base.accompagnantsPourcent(),
                base.presenceDixiemes(), base.scenarioBasPourcent(), base.scenarioHautPourcent(), conso)));
        return ok();
    }

    /** Revient aux hypothèses suggérées (les joueurs saisis et les quantités reprises sont gardés). */
    public ResultatBudgetBuvette revenirAuxSuggestions(BuvetteTournoi buvette) {
        buvette.setBudget(buvette.getBudget().avecHypotheses(null));
        return ok();
    }

    /** Déduire ou non le stock disponible de la liste de courses. */
    public ResultatBudgetBuvette deduireStock(BuvetteTournoi buvette, boolean deduire) {
        buvette.setBudget(buvette.getBudget().avecDeduireStock(deduire));
        return ok();
    }

    /** Abandonne les quantités reprises d'un ancien tournoi (retour au calcul). */
    public ResultatBudgetBuvette oublierQuantitesReprises(BuvetteTournoi buvette) {
        buvette.setBudget(buvette.getBudget().avecQuantitesImposees(Map.of(), ""));
        return ok();
    }

    /**
     * Préremplit le budget avec les quantités vendues lors d'un ancien tournoi
     * (« Reprendre les quantités d'un ancien tournoi ») : elles remplacent le calcul
     * par personne, produit par produit, et suivent les scénarios bas et haut.
     */
    public ResultatBudgetBuvette reprendreQuantites(BuvetteTournoi cible, BilanBuvette ancien) {
        Map<String, Long> q = new LinkedHashMap<>();
        ancien.ventesParProduit().forEach((id, n) -> {
            if (n > 0) {
                q.put(id, n);
            }
        });
        if (q.isEmpty()) {
            return new ResultatBudgetBuvette(ResultatBudgetBuvette.Motif.RIEN_A_REPRENDRE);
        }
        cible.setBudget(cible.getBudget().avecQuantitesImposees(q, ancien.nomTournoi()));
        return new ResultatBudgetBuvette(ResultatBudgetBuvette.Motif.QUANTITES_REPRISES);
    }

    private static ResultatBudgetBuvette ok() {
        return new ResultatBudgetBuvette(ResultatBudgetBuvette.Motif.ENREGISTRE);
    }
}
