package com.pingmanager.ui.ecran;

import com.pingmanager.core.buvette.BilanBuvette;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PresentationHistoriqueBuvetteTest {

    private static BilanBuvette bilan(String nom, LocalDate date, long recette, long achats) {
        return new BilanBuvette(nom, date, com.pingmanager.core.reglement.Formats.saison(date), 5000, 2000, recette,
                achats, 0, recette / 100, recette > 0 ? List.of(Map.entry("Café", 12L), Map.entry("Soda", 5L))
                : List.of(), 3, 150, 400, Map.of());
    }

    private final List<BilanBuvette> bilans = List.of(
            bilan("Open 2027", LocalDate.of(2027, 1, 9), 4600, 2400),
            bilan("Open 2026", LocalDate.of(2026, 1, 10), 3000, 3500),
            bilan("Tournoi de mai", LocalDate.of(2026, 5, 2), 0, 0));

    @Test
    void saisons_les_plus_recentes_en_tete_et_filtre() {
        PresentationHistoriqueBuvette toutes = new PresentationHistoriqueBuvette(bilans, null);
        assertEquals(List.of("Toutes les saisons", "2026-2027", "2025-2026"), toutes.saisons());
        assertEquals(3, toutes.lignes().size());
        PresentationHistoriqueBuvette une = new PresentationHistoriqueBuvette(bilans, "2025-2026");
        assertEquals(List.of("Open 2026", "Tournoi de mai"), une.lignes().stream()
                .map(PresentationHistoriqueBuvette.LigneTournoi::titre).toList());
        assertEquals(1, une.lignes().get(0).index(), "index dans la liste complète");
        assertEquals("-5,00 €", une.lignes().get(0).resultat());
        assertFalse(une.lignes().get(0).positif());
        assertEquals("Samedi 2 mai 2026 · aucune vente ni achat saisi", une.lignes().get(1).detail());
    }

    @Test
    void tuiles_totalisent_les_tournois_filtres() {
        PresentationHistoriqueBuvette p = new PresentationHistoriqueBuvette(bilans, "Toutes les saisons");
        assertEquals("3", p.tuiles().get(0).valeur());
        assertEquals("76,00 €", p.tuiles().get(1).valeur());
        assertEquals("59,00 €", p.tuiles().get(2).valeur());
        assertEquals("17,00 €", p.tuiles().get(3).valeur());
        assertEquals("résultat de la buvette (76 articles)", p.tuiles().get(3).libelle());
    }

    @Test
    void bilan_d_un_tournoi_prevu_contre_reel_et_phrases() {
        PresentationHistoriqueBuvette p = new PresentationHistoriqueBuvette(bilans, null);
        BilanBuvette b = bilans.get(0);
        List<PresentationHistoriqueBuvette.LigneComparaison> l = p.comparaison(b);
        assertEquals(new PresentationHistoriqueBuvette.LigneComparaison("Recette", "50,00 €", "46,00 €"), l.get(0));
        assertEquals(new PresentationHistoriqueBuvette.LigneComparaison("Marge", "30,00 €", "22,00 €"), l.get(2));
        assertEquals("3 portions · 1,50 €", l.get(4).reel());
        assertEquals("Les plus vendus : Café (12), Soda (5).", p.plusVendus(b));
        assertEquals("Résultat de la buvette : 46,00 € encaissés − 24,00 € d'achats = 22,00 €, plus un stock "
                + "restant estimé à 4,00 €.", p.phraseResultat(b));
        assertEquals("Aucune vente.", p.plusVendus(bilans.get(2)));
    }

    @Test
    void export_csv_du_filtre() {
        PresentationHistoriqueBuvette p = new PresentationHistoriqueBuvette(bilans, "2026-2027");
        String csv = p.csv();
        assertTrue(csv.contains("Open 2027"));
        assertFalse(csv.contains("Open 2026"));
        assertEquals("bilan-buvette-2026-2027.csv", p.nomFichier());
    }
}
