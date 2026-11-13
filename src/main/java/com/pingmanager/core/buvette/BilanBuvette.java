package com.pingmanager.core.buvette;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Bilan de la buvette d'un tournoi (B046) : prévu (scénario moyen du budget)
 * contre réel (caisse, achats, stock). Montants en centimes.
 *
 * @param nomTournoi        nom du tournoi
 * @param date              premier jour
 * @param saison            saison FFTT (« 2026-2027 »)
 * @param recettePrevue     recette attendue (scénario moyen)
 * @param achatsPrevus      achats prévus (scénario moyen)
 * @param recetteReelle     encaissé, annulations déduites
 * @param achatsReels       achats payés (corrections déduites)
 * @param coutDesVentes     coût de revient des articles vendus (recettes connues)
 * @param articles          articles vendus
 * @param plusVendus        jusqu'à cinq produits, du plus vendu au moins vendu (nom → quantité)
 * @param pertesPortions    portions perdues ou données
 * @param pertesCentimes    valeur de ces portions au coût d'achat
 * @param stockRestantCentimes valeur du stock restant au coût d'achat
 * @param ventesParProduit  quantité nette vendue par identifiant de produit
 */
public record BilanBuvette(String nomTournoi, LocalDate date, String saison, long recettePrevue, long achatsPrevus,
                           long recetteReelle, long achatsReels, long coutDesVentes, long articles,
                           List<Map.Entry<String, Long>> plusVendus, long pertesPortions, long pertesCentimes,
                           long stockRestantCentimes, Map<String, Long> ventesParProduit) {

    public BilanBuvette {
        plusVendus = List.copyOf(plusVendus);
        ventesParProduit = Map.copyOf(ventesParProduit);
    }

    /** Marge prévue = recette prévue − achats prévus. */
    public long margePrevue() {
        return recettePrevue - achatsPrevus;
    }

    /** Résultat net de trésorerie = encaissé − achats payés. */
    public long margeReelle() {
        return recetteReelle - achatsReels;
    }

    /** @return vrai si le tournoi a au moins une vente ou un achat. */
    public boolean aDesDonnees() {
        return articles != 0 || achatsReels != 0 || recetteReelle != 0;
    }
}
