package com.pingmanager.model.buvette;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Vente enregistrée par la buvette pendant un tournoi (B026), en mode
 * « Stock et ventes ». Objet immuable : une annulation produit une copie
 * marquée annulée ({@link #annulee(AnnulationVente)}), la vente n'est jamais
 * effacée.
 *
 * <p>Suivi interne du club, pas un ticket de caisse.</p>
 *
 * @param numero           numéro de la vente dans le tournoi (1, 2, 3…)
 * @param referenceTournoi tournoi de la vente
 * @param horodatage       date et heure de la vente
 * @param lignes           articles vendus (au moins un), prix et coûts figés
 * @param moyen            moyen de paiement
 * @param origine          d'où vient la vente (comptoir aujourd'hui)
 * @param referenceOrigine référence de l'origine (vide au comptoir ; un jour, le
 *                         numéro de la commande en ligne)
 * @param auteur           qui a enregistré la vente
 * @param sorties          quantités réellement sorties du stock (remises à l'annulation)
 * @param aRecompter       noms des articles qui manquaient en stock au moment de la vente
 * @param annulation       annulation, {@code null} si la vente est valable
 */
public record VenteBuvette(int numero, String referenceTournoi, Instant horodatage, List<LigneVente> lignes,
                           MoyenPaiement moyen, OrigineVente origine, String referenceOrigine, String auteur,
                           List<SortieVente> sorties, List<String> aRecompter, AnnulationVente annulation) {

    public VenteBuvette {
        if (numero < 1) {
            throw new IllegalArgumentException("numéro au moins 1");
        }
        if (referenceTournoi == null || referenceTournoi.isBlank()) {
            throw new IllegalArgumentException("tournoi obligatoire");
        }
        Objects.requireNonNull(horodatage, "horodatage");
        lignes = List.copyOf(lignes);
        if (lignes.isEmpty()) {
            throw new IllegalArgumentException("au moins un article");
        }
        Objects.requireNonNull(moyen, "moyen de paiement");
        Objects.requireNonNull(origine, "origine");
        referenceOrigine = referenceOrigine == null ? "" : referenceOrigine.trim();
        auteur = auteur == null || auteur.isBlank() ? "inconnu" : auteur.trim();
        sorties = sorties == null ? List.of() : List.copyOf(sorties);
        aRecompter = aRecompter == null ? List.of() : List.copyOf(aRecompter);
    }

    /** @return vrai si la vente a été annulée (elle ne compte plus dans le bilan). */
    public boolean estAnnulee() {
        return annulation != null;
    }

    public Optional<AnnulationVente> annulationEventuelle() {
        return Optional.ofNullable(annulation);
    }

    /** Montant de la vente, en centimes. */
    public long totalCentimes() {
        long t = 0;
        for (LigneVente l : lignes) {
            t += l.montantCentimes();
        }
        return t;
    }

    /** Coût des articles vendus, en centimes exacts. */
    public BigDecimal coutTotal() {
        BigDecimal c = BigDecimal.ZERO;
        for (LigneVente l : lignes) {
            c = c.add(l.cout());
        }
        return c;
    }

    /** Nombre de portions vendues. */
    public int nombreArticles() {
        int n = 0;
        for (LigneVente l : lignes) {
            n += l.quantite();
        }
        return n;
    }

    /** @return une copie de la vente, annulée. */
    public VenteBuvette annulee(AnnulationVente a) {
        if (estAnnulee()) {
            throw new IllegalStateException("vente déjà annulée");
        }
        return new VenteBuvette(numero, referenceTournoi, horodatage, lignes, moyen, origine, referenceOrigine,
                auteur, sorties, aRecompter, Objects.requireNonNull(a, "annulation"));
    }
}
