package com.pingmanager.core.buvette;

import com.pingmanager.model.buvette.AnnulationVente;
import com.pingmanager.model.buvette.LigneVente;
import com.pingmanager.model.buvette.MoyenPaiement;
import com.pingmanager.model.buvette.OrigineVente;
import com.pingmanager.model.buvette.VenteBuvette;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExportCsvBuvetteTest {

    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");

    private static VenteBuvette vente(int n, String t, MoyenPaiement m, LigneVente... lignes) {
        return new VenteBuvette(n, "brunoy", Instant.parse(t), List.of(lignes), m, OrigineVente.COMPTOIR, "",
                "Claire", List.of(), List.of(), null);
    }

    private static final LigneVente DEUX_CAFES = new LigneVente("cafe", "Café", 2, 100, new BigDecimal("11.2"));
    private static final LigneVente SODA = new LigneVente("soda", "Soda", 1, 150, new BigDecimal("40"));

    @Test
    void bilan_en_csv_francais_avec_bom_point_virgule_et_virgule_decimale() {
        List<VenteBuvette> ventes = List.of(vente(1, "2026-11-21T09:00:00Z", MoyenPaiement.ESPECES, DEUX_CAFES, SODA));
        String csv = ExportCsvBuvette.bilan(CalculBilanBuvette.bilan(ventes), "Tournoi de Brunoy");
        assertTrue(csv.startsWith("﻿Bilan de la buvette;Tournoi de Brunoy;Tout le tournoi\r\n"), csv);
        assertTrue(csv.contains("\r\nRecette (€);3,50\r\n"), csv);
        assertTrue(csv.contains("\r\nCoût des articles vendus (€);0,62\r\n"), csv);
        assertTrue(csv.contains("\r\nMarge (€);2,88\r\n"), csv);
        assertTrue(csv.contains("\r\nCafé;2;2,00;0,22;1,78\r\n"), csv);
        assertTrue(csv.contains("\r\nEspèces;1;3,50\r\n"), csv);
        assertTrue(csv.contains("\r\nCarte;0;0,00\r\n"), csv);
    }

    @Test
    void bilan_d_une_journee_porte_la_date() {
        String csv = ExportCsvBuvette.bilan(CalculBilanBuvette.bilanDuJour(List.of(), LocalDate.of(2026, 11, 22),
                PARIS), "T");
        assertTrue(csv.startsWith("﻿Bilan de la buvette;T;22/11/2026\r\n"), csv);
    }

    @Test
    void historique_une_ligne_par_article_annulation_visible_heure_locale() {
        VenteBuvette annulee = vente(2, "2026-11-21T14:30:00Z", MoyenPaiement.CARTE, SODA)
                .annulee(new AnnulationVente(Instant.parse("2026-11-21T14:31:00Z"), "Paul"));
        String csv = ExportCsvBuvette.historique(List.of(
                vente(1, "2026-11-21T09:00:00Z", MoyenPaiement.ESPECES, DEUX_CAFES, SODA), annulee), PARIS);
        String[] lignes = csv.split("\r\n");
        assertEquals(4, lignes.length);
        assertEquals("﻿Vente n°;Date;Heure;Article;Quantité;Prix unitaire (€);Montant (€);Coût (€);"
                + "Moyen de paiement;Origine;Saisie par;État;Annulée le;Annulée par", lignes[0]);
        assertEquals("1;21/11/2026;10:00:00;Café;2;1,00;2,00;0,22;Espèces;Au comptoir;Claire;Valable;;", lignes[1]);
        assertEquals("2;21/11/2026;15:30:00;Soda;1;1,50;1,50;0,40;Carte;Au comptoir;Claire;Annulée;"
                + "21/11/2026 15:31:00;Paul", lignes[3]);
    }

    @Test
    void nom_d_article_pris_pour_une_formule_neutralise_et_separateurs_proteges() {
        assertEquals("'=SOMME(A1:A9)", ExportCsvBuvette.texte("=SOMME(A1:A9)"));
        assertEquals("'+33", ExportCsvBuvette.texte("+33"));
        assertEquals("'@cmd", ExportCsvBuvette.texte("@cmd"));
        assertEquals("\"Café; thé\"", ExportCsvBuvette.texte("Café; thé"));
        assertEquals("\"Le \"\"grand\"\" café\"", ExportCsvBuvette.texte("Le \"grand\" café"));
        assertEquals("", ExportCsvBuvette.texte(null));
    }

    @Test
    void montants_sans_espace_de_milliers_et_negatifs() {
        assertEquals("1234,50", ExportCsvBuvette.montant(123450));
        assertEquals("-2,05", ExportCsvBuvette.montant(-205));
        assertEquals("0,00", ExportCsvBuvette.montant(0));
    }
}
