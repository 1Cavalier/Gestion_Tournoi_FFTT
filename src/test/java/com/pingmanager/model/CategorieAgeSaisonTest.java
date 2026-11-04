package com.pingmanager.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/** Grille « Catégories d'âge pour la saison 2026-2027 » des règlements généraux (II.302 / II.303). */
class CategorieAgeSaisonTest {

    private static final LocalDate TOURNOI = LocalDate.of(2027, 4, 10);

    private static CategorieAge nesEn(int annee) {
        return CategorieAge.pour(LocalDate.of(annee, 6, 15), TOURNOI);
    }

    @Test
    void grille_2026_2027() {
        assertEquals(2026, CategorieAge.anneeDeReference(TOURNOI));
        assertEquals(2026, CategorieAge.anneeDeReference(LocalDate.of(2026, 9, 1)));
        assertEquals(CategorieAge.POUSSIN, nesEn(2018));
        assertEquals(CategorieAge.BENJAMIN, nesEn(2017));
        assertEquals(CategorieAge.BENJAMIN, nesEn(2016));
        assertEquals(CategorieAge.MINIME, nesEn(2015));
        assertEquals(CategorieAge.MINIME, nesEn(2014));
        assertEquals(CategorieAge.CADET, nesEn(2013));
        assertEquals(CategorieAge.CADET, nesEn(2012));
        assertEquals(CategorieAge.JUNIOR, nesEn(2011));
        assertEquals(CategorieAge.JUNIOR, nesEn(2008));
        assertEquals(CategorieAge.SENIOR, nesEn(2007));
        assertEquals(CategorieAge.SENIOR, nesEn(1987));
        assertEquals(CategorieAge.VETERAN_40, nesEn(1986));
        assertEquals(CategorieAge.VETERAN_40, nesEn(1982));
        assertEquals(CategorieAge.VETERAN_45, nesEn(1981));
        assertEquals(CategorieAge.VETERAN_85, nesEn(1937));
        assertEquals(CategorieAge.VETERAN_90, nesEn(1936));
        assertNull(CategorieAge.pour(null, TOURNOI));
    }

    @Test
    void distance_entre_deux_points() {
        Coordonnees brunoy = new Coordonnees(48.697, 2.504);
        Coordonnees orleans = new Coordonnees(47.903, 1.909);
        assertEquals(99, brunoy.distanceKm(orleans), 3);
        assertThrows(IllegalArgumentException.class, () -> new Coordonnees(91, 0));
    }
}
