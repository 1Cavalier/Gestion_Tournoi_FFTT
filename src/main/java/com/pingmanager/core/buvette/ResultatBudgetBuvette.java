package com.pingmanager.core.buvette;

/**
 * Résultat d'une saisie du budget prévisionnel de la buvette (B046).
 *
 * @param motif ce qui s'est passé
 */
public record ResultatBudgetBuvette(Motif motif) {

    /** Motifs possibles. */
    public enum Motif {
        ENREGISTRE(true, "Budget mis à jour."),
        QUANTITES_REPRISES(true, "Quantités reprises : le budget part des ventes réelles de ce tournoi."),
        NOMBRE_INVALIDE(false, "Le nombre de joueurs est un entier entre 0 et 5 000 (vide : estimé)."),
        POURCENT_INVALIDE(false, "La part des accompagnants est un pourcentage entier entre 0 et 500."),
        PRESENCE_INVALIDE(false, "La présence moyenne s'écrit en demi-journées, par exemple 1,5 (entre 0,1 et 8)."),
        SCENARIOS_INVALIDES(false, "Scénario bas entre 1 et 100 %, scénario haut entre 100 et 500 %."),
        CONSOMMATION_INVALIDE(false, "La consommation s'écrit par personne et par demi-journée, par exemple 0,35."),
        PRODUIT_INCONNU(false, "Produit introuvable dans le catalogue."),
        RIEN_A_REPRENDRE(false, "Ce tournoi n'a aucune vente à reprendre.");

        private final boolean accepte;
        private final String explication;

        Motif(boolean accepte, String explication) {
            this.accepte = accepte;
            this.explication = explication;
        }

        public boolean accepte() {
            return accepte;
        }

        public String explication() {
            return explication;
        }
    }

    public boolean accepte() {
        return motif.accepte();
    }

    public String explication() {
        return motif.explication();
    }
}
