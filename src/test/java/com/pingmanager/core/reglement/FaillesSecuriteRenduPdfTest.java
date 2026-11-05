package com.pingmanager.core.reglement;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * DÉMONSTRATION DE FAILLE (audit de sécurité, voir RAPPORT_SECURITE.md).
 *
 * <p>Ce test passe <b>parce que</b> le rendu PDF ne filtre pas les caractères
 * absents de la police. Lors de la correction, il devra être <b>inversé</b> (PDF
 * produit, caractère remplacé) et renommé sans le suffixe « faille_S17 ».</p>
 */
class FaillesSecuriteRenduPdfTest {

    /**
     * Faille S-17 (disponibilité) : un nom de tournoi (ou un nom de joueur importé
     * repris dans un document) contenant un caractère absent de la police Inter,
     * par exemple un émoji, fait échouer toute la génération du PDF. Un seul nom
     * saisi rend le règlement et la feuille d'engagement impossibles à produire.
     * Correctif : remplacer les glyphes manquants (hasGlyph) par « ? » ou une police de repli.
     */
    @Test
    void emoji_dans_le_titre_fait_echouer_le_pdf_faille_S17() {
        DocumentReglement d = new DocumentReglement("Open de ping 🏓", "Règlement", List.of(), List.of());

        assertThrows(IllegalArgumentException.class, () -> RenduPdfReglement.rendre(d));
    }
}
