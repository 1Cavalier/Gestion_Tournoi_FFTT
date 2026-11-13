package com.pingmanager.core.buvette;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExportCsvBuvetteTest {

    @Test
    void cellules_protegees_contre_les_formules_mais_pas_les_nombres_negatifs() {
        assertEquals("'=SOMME(A1)", ExportCsvBuvette.cellule("=SOMME(A1)"));
        assertEquals("'+33", ExportCsvBuvette.cellule("+33"));
        assertEquals("'@cmd", ExportCsvBuvette.cellule("@cmd"));
        assertEquals("'-x", ExportCsvBuvette.cellule("-x"));
        assertEquals("-2,50", ExportCsvBuvette.cellule("-2,50"));
        assertEquals("\"a;b\"", ExportCsvBuvette.cellule("a;b"));
        assertEquals("\"dit \"\"oui\"\"\"", ExportCsvBuvette.cellule("dit \"oui\""));
    }

    @Test
    void liste_de_courses_avec_entete_total_et_mention() {
        BudgetCalcule.Scenario s = new BudgetCalcule.Scenario("Moyen", 100, List.of(), List.of(
                new BudgetCalcule.LigneCourse("baguette", "Baguette", 24, 0, 24, 14, "baguette", 1540, "")),
                15_400, 1540);
        String csv = ExportCsvBuvette.listeCourses("Open", s);
        assertTrue(csv.startsWith("﻿Liste de courses;Open;Scénario moyen\r\n"));
        assertTrue(csv.contains("Baguette;baguette;14;24;0;15,40\r\n"));
        assertTrue(csv.contains("Total;;;;;15,40\r\n"));
        assertTrue(csv.contains("Prix indicatifs à valider par le club"));
    }
}
