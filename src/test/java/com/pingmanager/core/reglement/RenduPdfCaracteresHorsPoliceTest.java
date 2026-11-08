package com.pingmanager.core.reglement;

import com.pingmanager.model.DocumentReglement;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Faille S-17 corrigée (audit de sécurité, voir RAPPORT_SECURITE.md) : un caractère
 * absent de la police Inter (émoji, CJK…) dans le titre ne fait plus échouer tout le
 * PDF ; il est imprimé « ? ». Ce test remplace la démonstration de faille, inversée
 * comme elle le demandait.
 */
class RenduPdfCaracteresHorsPoliceTest {

    @Test
    void emoji_dans_le_titre_produit_le_pdf_avec_un_point_d_interrogation() {
        DocumentReglement d = new DocumentReglement("Open de ping 🏓", "Règlement", List.of(), List.of());

        byte[] pdf = assertDoesNotThrow(() -> RenduPdfReglement.rendre(d));
        assertTrue(pdf.length > 0);
    }

    @Test
    void ideogrammes_et_paires_de_substitution_remplaces_un_pour_un() {
        DocumentReglement d = new DocumentReglement("Tournoi 日本 🏓", "Règlement", List.of(), List.of());
        assertDoesNotThrow(() -> RenduPdfReglement.rendre(d));
    }
}
