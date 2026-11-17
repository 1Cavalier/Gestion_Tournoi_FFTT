package com.pingmanager.core.buvette;

import com.pingmanager.model.buvette.LigneVente;
import com.pingmanager.model.buvette.VenteBuvette;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Export CSV du bilan et de l'historique des ventes de la buvette (B026),
 * sans dépendance : texte UTF-8 avec marque d'ordre (BOM), séparateur « ; »
 * et virgule décimale, pour s'ouvrir directement dans un tableur réglé en
 * français (Excel, LibreOffice Calc).
 *
 * <p>Les textes qui commencent par {@code = + - @} sont précédés d'une
 * apostrophe, pour qu'un nom d'article ne soit jamais pris pour une formule
 * (injection de formules).</p>
 */
public final class ExportCsvBuvette {

    /** Marque d'ordre des octets : indique l'UTF-8 au tableur. */
    public static final String BOM = "﻿";
    private static final String FIN = "\r\n";
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HEURE = DateTimeFormatter.ofPattern("HH:mm:ss");

    private ExportCsvBuvette() {
    }

    /** Bilan : chiffres clés, détail par article, détail par moyen de paiement. */
    public static String bilan(BilanBuvette b, String nomTournoi) {
        StringBuilder sb = new StringBuilder(BOM);
        ligne(sb, texte("Bilan de la buvette"), texte(nomTournoi),
                texte(b.jour() == null ? "Tout le tournoi" : DATE.format(b.jour())));
        ligne(sb, texte("Suivi interne du club (pas un document de caisse)"));
        sb.append(FIN);
        ligne(sb, "Indicateur", "Valeur");
        ligne(sb, "Ventes", String.valueOf(b.nombreVentes()));
        ligne(sb, "Ventes annulées (non comptées)", String.valueOf(b.nombreAnnulees()));
        ligne(sb, "Articles vendus", String.valueOf(b.nombreArticles()));
        ligne(sb, "Recette (€)", montant(b.recetteCentimes()));
        ligne(sb, "Coût des articles vendus (€)", montant(b.coutCentimes()));
        ligne(sb, "Marge (€)", montant(b.margeCentimes()));
        sb.append(FIN);
        ligne(sb, "Article", "Quantité", "Recette (€)", "Coût (€)", "Marge (€)");
        for (BilanBuvette.LigneArticle l : b.parArticle()) {
            ligne(sb, texte(l.nom()), String.valueOf(l.quantite()), montant(l.recetteCentimes()),
                    montant(l.coutCentimes()), montant(l.margeCentimes()));
        }
        sb.append(FIN);
        ligne(sb, "Moyen de paiement", "Ventes", "Recette (€)");
        for (BilanBuvette.LigneMoyen m : b.parMoyen()) {
            ligne(sb, texte(m.moyen().getLibelle()), String.valueOf(m.nombreVentes()), montant(m.recetteCentimes()));
        }
        return sb.toString();
    }

    /** Historique : une ligne par article de chaque vente, annulées comprises (colonne « État »). */
    public static String historique(List<VenteBuvette> ventes, ZoneId zone) {
        StringBuilder sb = new StringBuilder(BOM);
        ligne(sb, "Vente n°", "Date", "Heure", "Article", "Quantité", "Prix unitaire (€)", "Montant (€)",
                "Coût (€)", "Moyen de paiement", "Origine", "Saisie par", "État", "Annulée le", "Annulée par");
        for (VenteBuvette v : ventes) {
            var t = v.horodatage().atZone(zone);
            String annuleeLe = "";
            String annuleePar = "";
            if (v.estAnnulee()) {
                var a = v.annulation().horodatage().atZone(zone);
                annuleeLe = DATE.format(a) + " " + HEURE.format(a);
                annuleePar = texte(v.annulation().auteur());
            }
            for (LigneVente l : v.lignes()) {
                ligne(sb, String.valueOf(v.numero()), DATE.format(t), HEURE.format(t), texte(l.nom()),
                        String.valueOf(l.quantite()), montant(l.prixUnitaireCentimes()), montant(l.montantCentimes()),
                        montant(Centimes.arrondir(l.cout())), texte(v.moyen().getLibelle()),
                        texte(v.origine().getLibelle()), texte(v.auteur()), v.estAnnulee() ? "Annulée" : "Valable",
                        annuleeLe, annuleePar);
            }
        }
        return sb.toString();
    }

    /** 123450 → « 1234,50 » ; -250 → « -2,50 » (sans espace de milliers, lisible par un tableur). */
    static String montant(long centimes) {
        String signe = centimes < 0 ? "-" : "";
        long abs = Math.abs(centimes);
        long c = abs % 100;
        return signe + (abs / 100) + "," + (c < 10 ? "0" : "") + c;
    }

    /** Champ de texte : neutralise les formules, puis protège « ; », guillemets et retours à la ligne. */
    static String texte(String s) {
        String v = s == null ? "" : s;
        if (!v.isEmpty() && "=+-@\t\r".indexOf(v.charAt(0)) >= 0) {
            v = "'" + v;
        }
        if (v.contains(";") || v.contains("\"") || v.contains("\n") || v.contains("\r")) {
            v = "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }

    private static void ligne(StringBuilder sb, String... champs) {
        sb.append(String.join(";", champs)).append(FIN);
    }
}
