package com.pingmanager.ui.ecran;

import com.pingmanager.core.buvette.BilanBuvette;
import com.pingmanager.core.buvette.Centimes;
import com.pingmanager.core.buvette.ExportCsvBuvette;
import com.pingmanager.core.reglement.Formats;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;

/**
 * Présentation (sans JavaFX) de l'onglet « Historique » de la page Buvette (B046) :
 * liste des tournois du club avec leurs totaux, filtre par saison, bilan prévu
 * contre réel d'un tournoi et export CSV.
 */
final class PresentationHistoriqueBuvette {

    /** Valeur du filtre qui montre toutes les saisons. */
    static final String TOUTES = "Toutes les saisons";

    record Tuile(String valeur, String libelle) {
    }

    record LigneTournoi(int index, String titre, String detail, String resultat, boolean positif) {
    }

    /** Une ligne du tableau prévu / réel. */
    record LigneComparaison(String libelle, String prevu, String reel) {
    }

    private final List<BilanBuvette> tous;
    private final String saison;

    /**
     * @param bilans bilans de tous les tournois du club (du plus récent au plus ancien)
     * @param saison saison filtrée, ou {@link #TOUTES}
     */
    PresentationHistoriqueBuvette(List<BilanBuvette> bilans, String saison) {
        this.tous = List.copyOf(Objects.requireNonNull(bilans, "bilans"));
        this.saison = saison == null ? TOUTES : saison;
    }

    /** Saisons présentes, la plus récente en tête, précédées de « Toutes les saisons ». */
    List<String> saisons() {
        TreeSet<String> s = new TreeSet<>(java.util.Comparator.reverseOrder());
        tous.forEach(b -> s.add(b.saison()));
        List<String> res = new ArrayList<>();
        res.add(TOUTES);
        res.addAll(s);
        return res;
    }

    /** Bilans retenus par le filtre. */
    List<BilanBuvette> filtres() {
        return tous.stream().filter(b -> TOUTES.equals(saison) || b.saison().equals(saison)).toList();
    }

    List<Tuile> tuiles() {
        List<BilanBuvette> f = filtres();
        long recette = f.stream().mapToLong(BilanBuvette::recetteReelle).sum();
        long achats = f.stream().mapToLong(BilanBuvette::achatsReels).sum();
        long articles = f.stream().mapToLong(BilanBuvette::articles).sum();
        return List.of(new Tuile(String.valueOf(f.size()), f.size() > 1 ? "tournois" : "tournoi"),
                new Tuile(Centimes.euros(recette), "encaissé"),
                new Tuile(Centimes.euros(achats), "achats payés"),
                new Tuile(Centimes.euros(recette - achats), "résultat de la buvette (" + articles + " articles)"));
    }

    List<LigneTournoi> lignes() {
        List<LigneTournoi> res = new ArrayList<>();
        List<BilanBuvette> f = filtres();
        for (BilanBuvette b : f) {
            String detail = Formats.capitale(Formats.jourLong(b.date())) + " · encaissé "
                    + Centimes.euros(b.recetteReelle()) + " · achats " + Centimes.euros(b.achatsReels());
            if (!b.aDesDonnees()) {
                detail = Formats.capitale(Formats.jourLong(b.date())) + " · aucune vente ni achat saisi";
            }
            res.add(new LigneTournoi(tous.indexOf(b), b.nomTournoi(), detail, Centimes.euros(b.margeReelle()),
                    b.margeReelle() >= 0));
        }
        return res;
    }

    String messageVide() {
        return "Aucun tournoi pour cette saison.";
    }

    /** Tableau prévu / réel d'un tournoi. */
    List<LigneComparaison> comparaison(BilanBuvette b) {
        return List.of(
                new LigneComparaison("Recette", Centimes.euros(b.recettePrevue()), Centimes.euros(b.recetteReelle())),
                new LigneComparaison("Achats", Centimes.euros(b.achatsPrevus()), Centimes.euros(b.achatsReels())),
                new LigneComparaison("Marge", Centimes.euros(b.margePrevue()), Centimes.euros(b.margeReelle())),
                new LigneComparaison("Coût de revient des ventes", "—", Centimes.euros(b.coutDesVentes())),
                new LigneComparaison("Pertes et dons", "—", b.pertesPortions() + " portions · "
                        + Centimes.euros(b.pertesCentimes())),
                new LigneComparaison("Stock restant", "—", Centimes.euros(b.stockRestantCentimes())));
    }

    /** « Café (82), Soda (40) » ou « Aucune vente ». */
    String plusVendus(BilanBuvette b) {
        if (b.plusVendus().isEmpty()) {
            return "Aucune vente.";
        }
        StringBuilder sb = new StringBuilder("Les plus vendus : ");
        boolean premier = true;
        for (Map.Entry<String, Long> e : b.plusVendus()) {
            sb.append(premier ? "" : ", ").append(e.getKey()).append(" (").append(e.getValue()).append(')');
            premier = false;
        }
        return sb.append('.').toString();
    }

    /** Phrase de lecture : résultat net de la buvette, à rapprocher des finances du tournoi. */
    String phraseResultat(BilanBuvette b) {
        return "Résultat de la buvette : " + Centimes.euros(b.recetteReelle()) + " encaissés − "
                + Centimes.euros(b.achatsReels()) + " d'achats = " + Centimes.euros(b.margeReelle())
                + (b.stockRestantCentimes() > 0 ? ", plus un stock restant estimé à "
                + Centimes.euros(b.stockRestantCentimes()) : "") + ".";
    }

    String csv() {
        return ExportCsvBuvette.bilan(saison, filtres());
    }

    String nomFichier() {
        return "bilan-buvette-" + com.pingmanager.model.buvette.CatalogueBuvette.slug(saison) + ".csv";
    }
}
