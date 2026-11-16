package com.pingmanager.ui.ecran;

import com.pingmanager.model.Tournoi;
import com.pingmanager.model.TraceTirage;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Libellés du tirage au sort (poules et tableau final), hors JavaFX et testés.
 *
 * <p>L'utilisateur courant voit la date, l'heure et le juge-arbitre du tirage ;
 * la graine du générateur, mécanisme technique interne qu'aucun texte fédéral
 * n'impose, n'apparaît que dans un détail discret destiné au support.</p>
 *
 * <ul>
 *   <li>IX.109 : le juge-arbitre est le responsable direct du tirage au sort ;</li>
 *   <li>IX.111 : le règlement du tournoi indique la date, l'heure et le lieu du
 *       tirage au sort public ;</li>
 *   <li>I.302 à I.305 : méthode de placement ;</li>
 *   <li>manuel JA, I-6.4 : un joueur placé par tirage au sort ne peut plus être
 *       déplacé.</li>
 * </ul>
 */
final class PresentationTirage {

    /** Libellé du détail technique (support), à la place du mot « graine ». */
    static final String LIBELLE_REFERENCE = "Référence technique du tirage (support)";

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HEURE = DateTimeFormatter.ofPattern("HH'h'mm");

    private PresentationTirage() {
    }

    /**
     * « Tirage au sort effectué le 16/11/2026 à 14h05 — juge-arbitre : Prénom Nom. »
     *
     * @param trace trace du tirage, {@code null} pour un tirage fait par une
     *              version qui ne l'enregistrait pas
     */
    static String texteTirageEffectue(TraceTirage trace, ZoneId zone) {
        if (trace == null) {
            return "Tirage au sort effectué (date, heure et juge-arbitre non enregistrés "
                    + "par la version qui l'a fait).";
        }
        LocalDateTime local = LocalDateTime.ofInstant(trace.effectueLe(), zone);
        String texte = "Tirage au sort effectué le " + DATE.format(local) + " à " + HEURE.format(local);
        if (trace.jugeArbitre() == null) {
            return texte + " (aucun juge-arbitre désigné à ce moment).";
        }
        return texte + " — juge-arbitre : " + trace.jugeArbitre() + ".";
    }

    /** Même texte, au fuseau du poste. */
    static String texteTirageEffectue(TraceTirage trace) {
        return texteTirageEffectue(trace, ZoneId.systemDefault());
    }

    /** Détail discret pour le support : la référence technique (graine), ou son absence. */
    static String texteReferenceTechnique(Long graine) {
        return LIBELLE_REFERENCE + " : " + (graine == null ? "non enregistrée" : String.valueOf(graine)) + ".";
    }

    /**
     * Rappel affiché avant un tirage : qui en est responsable et ce qu'il fige.
     *
     * @param tournoi le tournoi ({@code null} : sans nom de juge-arbitre)
     */
    static String rappelAvantTirage(Tournoi tournoi) {
        String ja = tournoi == null ? null : tournoi.getJugeArbitrePrincipal()
                .map(a -> (a.getPrenom() + " " + a.getNom()).strip())
                .orElse(null);
        String qui = ja == null
                ? "Le juge-arbitre est le responsable direct du tirage au sort (IX.109)"
                : "Le juge-arbitre, " + ja + ", est le responsable direct du tirage au sort (IX.109)";
        return qui + ". Faites-le à la date, à l'heure et au lieu annoncés dans le règlement du tournoi "
                + "(IX.111). Une fois fait, le tirage est daté et ne peut plus être refait : un joueur placé "
                + "par tirage au sort ne peut plus être déplacé (manuel JA, I-6.4).";
    }

    /** Texte d'aide du détail technique, réservé au support. */
    static String aideReferenceTechnique() {
        return "Réservé au support. Une référence est proposée automatiquement ; ne la changez que pour "
                + "refaire à l'identique, après un incident, un tirage déjà fait et publié : la même "
                + "référence et les mêmes joueurs donnent exactement le même placement.";
    }
}
