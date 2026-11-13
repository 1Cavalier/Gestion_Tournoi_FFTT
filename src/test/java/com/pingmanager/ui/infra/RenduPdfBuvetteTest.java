package com.pingmanager.ui.infra;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RenduPdfBuvetteTest {

    @Test
    void tableau_sur_plusieurs_pages_produit_un_pdf() throws Exception {
        List<List<String>> lignes = new ArrayList<>();
        for (int i = 0; i < 80; i++) {
            lignes.add(List.of("Ingrédient " + i + " avec un nom très très long qui doit être tronqué", "3 × lot",
                    "12 portions", "4,50 €"));
        }
        byte[] pdf = RenduPdfBuvette.tableau("Liste de courses", "Open · scénario moyen",
                List.of("Ingrédient", "À acheter", "Besoin", "Coût"), lignes, new float[] {4, 4, 2, 2},
                List.of("Prix indicatifs à valider par le club."));
        assertTrue(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1).startsWith("%PDF"));
    }

    @Test
    void une_largeur_par_colonne_exigee() {
        assertThrows(IllegalArgumentException.class, () -> RenduPdfBuvette.tableau("t", "s", List.of("a", "b"),
                List.of(), new float[] {1}, List.of()));
    }
}
