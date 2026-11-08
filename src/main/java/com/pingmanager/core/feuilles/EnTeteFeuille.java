package com.pingmanager.core.feuilles;

/**
 * En-tête commun aux feuilles imprimées : tournoi, date, lieu et tableau.
 *
 * @param tournoi nom du tournoi
 * @param date    date au format « 14/11/2026 », vide si inconnue
 * @param lieu    ville, vide si inconnue
 * @param tableau nom du tableau
 */
public record EnTeteFeuille(String tournoi, String date, String lieu, String tableau) {

    public EnTeteFeuille {
        tournoi = tournoi == null ? "" : tournoi;
        date = date == null ? "" : date;
        lieu = lieu == null ? "" : lieu;
        tableau = tableau == null ? "" : tableau;
    }

    /** « Tournoi de Brunoy · 14/11/2026 · Brunoy » (sans les parties vides). */
    public String ligneTournoi() {
        StringBuilder sb = new StringBuilder(tournoi);
        for (String s : new String[] {date, lieu}) {
            if (!s.isEmpty()) {
                sb.append(sb.length() == 0 ? "" : " · ").append(s);
            }
        }
        return sb.toString();
    }
}
