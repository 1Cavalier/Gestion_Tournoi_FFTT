package com.pingmanager.core.buvette;

import com.pingmanager.model.buvette.VenteBuvette;

import java.util.List;

/**
 * Résultat d'une vente ou d'une annulation à la caisse de la buvette (B046).
 *
 * @param motif          ce qui s'est passé
 * @param vente          la vente (ou l'annulation) enregistrée, ou {@code null}
 * @param stockEpuise    noms des ingrédients passés sous zéro (la vente est gardée :
 *                       elle a eu lieu, l'inventaire corrigera)
 */
public record ResultatVenteBuvette(Motif motif, VenteBuvette vente, List<String> stockEpuise) {

    /** Motifs possibles. */
    public enum Motif {
        VENDUE(true, "Vente enregistrée."),
        ANNULEE(true, "Vente annulée."),
        PRODUIT_INCONNU(false, "Produit introuvable dans le catalogue."),
        PRODUIT_INACTIF(false, "Ce produit est désactivé : réactivez-le dans le catalogue pour le vendre."),
        RIEN_A_ANNULER(false, "Aucune vente à annuler.");

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

    public ResultatVenteBuvette {
        stockEpuise = List.copyOf(stockEpuise);
    }

    public boolean accepte() {
        return motif.accepte();
    }

    static ResultatVenteBuvette refus(Motif m) {
        return new ResultatVenteBuvette(m, null, List.of());
    }
}
