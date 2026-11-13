package com.pingmanager.core.buvette;

import java.util.List;

/**
 * Exports CSV de la buvette (B046) : séparateur point-virgule, montants en euros
 * avec virgule, première ligne d'en-tête, marque d'ordre UTF-8 pour les tableurs.
 * Toute cellule qui commence par {@code = + - @} (ou une tabulation) est
 * précédée d'une apostrophe (sauf un simple nombre négatif) : pas d'injection
 * de formule.
 */
public final class ExportCsvBuvette {

    private static final String BOM = "﻿";

    private ExportCsvBuvette() {
    }

    /** Liste de courses d'un scénario du budget prévisionnel. */
    public static String listeCourses(String nomTournoi, BudgetCalcule.Scenario s) {
        StringBuilder sb = new StringBuilder(BOM);
        ligne(sb, List.of("Liste de courses", nomTournoi, "Scénario " + s.nom().toLowerCase()));
        ligne(sb, List.of("Ingrédient", "Unité d'achat", "Quantité à acheter", "Portions nécessaires",
                "Portions en stock", "Coût estimé (€)"));
        for (BudgetCalcule.LigneCourse c : s.courses()) {
            ligne(sb, List.of(c.nom(), c.uniteAchat(), String.valueOf(c.unites()), String.valueOf(c.portionsNecessaires()),
                    String.valueOf(c.stockDisponible()), Centimes.nombre(c.coutCentimes())));
        }
        ligne(sb, List.of("Total", "", "", "", "", Centimes.nombre(s.coutAchatsCentimes())));
        ligne(sb, List.of("Prix indicatifs à valider par le club ; quantités issues d'hypothèses à ajuster."));
        return sb.toString();
    }

    /** Bilan de plusieurs tournois, prévu contre réel, avec une ligne de totaux. */
    public static String bilan(String titre, List<BilanBuvette> bilans) {
        StringBuilder sb = new StringBuilder(BOM);
        ligne(sb, List.of("Bilan de la buvette", titre));
        ligne(sb, List.of("Tournoi", "Date", "Saison", "Recette prévue (€)", "Recette réelle (€)",
                "Achats prévus (€)", "Achats payés (€)", "Marge prévue (€)", "Résultat réel (€)", "Articles vendus",
                "Pertes et dons (€)", "Stock restant (€)", "Produits les plus vendus"));
        long rp = 0, rr = 0, ap = 0, ar = 0, art = 0, pe = 0, st = 0;
        for (BilanBuvette b : bilans) {
            StringBuilder top = new StringBuilder();
            b.plusVendus().forEach(e -> top.append(top.length() == 0 ? "" : ", ").append(e.getKey()).append(" (")
                    .append(e.getValue()).append(')'));
            ligne(sb, List.of(b.nomTournoi(), b.date().toString(), b.saison(), Centimes.nombre(b.recettePrevue()),
                    Centimes.nombre(b.recetteReelle()), Centimes.nombre(b.achatsPrevus()),
                    Centimes.nombre(b.achatsReels()), Centimes.nombre(b.margePrevue()),
                    Centimes.nombre(b.margeReelle()), String.valueOf(b.articles()),
                    Centimes.nombre(b.pertesCentimes()), Centimes.nombre(b.stockRestantCentimes()), top.toString()));
            rp += b.recettePrevue();
            rr += b.recetteReelle();
            ap += b.achatsPrevus();
            ar += b.achatsReels();
            art += b.articles();
            pe += b.pertesCentimes();
            st += b.stockRestantCentimes();
        }
        ligne(sb, List.of("Total", "", "", Centimes.nombre(rp), Centimes.nombre(rr), Centimes.nombre(ap),
                Centimes.nombre(ar), Centimes.nombre(rp - ap), Centimes.nombre(rr - ar), String.valueOf(art),
                Centimes.nombre(pe), Centimes.nombre(st), ""));
        return sb.toString();
    }

    /** Ajoute une ligne CSV (cellules protégées et échappées). */
    public static void ligne(StringBuilder sb, List<String> cellules) {
        for (int i = 0; i < cellules.size(); i++) {
            if (i > 0) {
                sb.append(';');
            }
            sb.append(cellule(cellules.get(i)));
        }
        sb.append("\r\n");
    }

    /** Cellule protégée contre les formules et échappée (guillemets doublés si besoin). */
    static String cellule(String brut) {
        String s = brut == null ? "" : brut.replace(' ', ' ');
        boolean nombre = s.matches("-?[0-9 ]+(,[0-9]+)?");
        if (!nombre && !s.isEmpty() && "=+-@\t\r".indexOf(s.charAt(0)) >= 0) {
            s = "'" + s;
        }
        if (s.contains(";") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            s = "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
