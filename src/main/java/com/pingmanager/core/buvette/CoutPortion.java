package com.pingmanager.core.buvette;

import java.math.BigDecimal;

/**
 * Coût d'une portion d'un produit de la buvette (B026), en <b>centimes exacts</b>
 * (non arrondis : une touillette peut coûter 0,3 centime).
 *
 * @param brut      coût du produit lui-même (une unité de son stock)
 * @param emballage coût des contenants et consommables utilisés par portion
 */
public record CoutPortion(BigDecimal brut, BigDecimal emballage) {

    /** Coût complet : produit brut + emballage. */
    public BigDecimal complet() {
        return brut.add(emballage);
    }
}
