package com.pingmanager.ui.ecran;

import com.pingmanager.core.buvette.BilanBuvette;
import com.pingmanager.core.buvette.CalculBilanBuvette;
import com.pingmanager.core.buvette.Centimes;
import com.pingmanager.model.buvette.LigneVente;
import com.pingmanager.model.buvette.VenteBuvette;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Logique d'affichage du bilan de la buvette (B026), testée hors JavaFX :
 * choix de la période (tout le tournoi ou une journée), chiffres clés,
 * détails et historique des ventes (annulées comprises).
 */
final class PresentationBilanBuvette {

    static final String AUCUNE_VENTE = "Aucune vente enregistrée pour ce tournoi. Les ventes validées au comptoir "
            + "s'afficheront ici.";
    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH);
    private static final DateTimeFormatter HEURE = DateTimeFormatter.ofPattern("dd/MM HH:mm");
    private static final DateTimeFormatter HEURE_SEULE = DateTimeFormatter.ofPattern("HH:mm");

    /** Période proposée : {@code jour} {@code null} = tout le tournoi. */
    record Periode(LocalDate jour, String libelle) {
    }

    /** Chiffre clé : valeur et libellé. */
    record Chiffre(String valeur, String libelle) {
    }

    /**
     * Ligne de l'historique.
     *
     * @param numero      numéro de la vente
     * @param titre       « Vente n° 3 · 21/11 10:00 »
     * @param articles    « 2 × Café, 1 × Soda »
     * @param total       « 3,50 € · Espèces »
     * @param etat        « Annulée le 21/11 à 10:05 par Paul » ou vide
     * @param avertissement « Stock insuffisant : Gobelet » ou vide
     * @param annulable   vrai si la vente est encore valable
     */
    record LigneHistorique(int numero, String titre, String articles, String total, String etat,
                           String avertissement, boolean annulable) {
    }

    private final List<VenteBuvette> ventes;
    private final ZoneId zone;

    PresentationBilanBuvette(List<VenteBuvette> ventes, ZoneId zone) {
        this.ventes = List.copyOf(ventes);
        this.zone = zone;
    }

    boolean vide() {
        return ventes.isEmpty();
    }

    List<Periode> periodes() {
        List<Periode> res = new ArrayList<>();
        res.add(new Periode(null, "Tout le tournoi"));
        for (LocalDate j : CalculBilanBuvette.jours(ventes, zone)) {
            String l = JOUR.format(j);
            res.add(new Periode(j, Character.toUpperCase(l.charAt(0)) + l.substring(1)));
        }
        return res;
    }

    BilanBuvette bilan(LocalDate jour) {
        return jour == null ? CalculBilanBuvette.bilan(ventes) : CalculBilanBuvette.bilanDuJour(ventes, jour, zone);
    }

    static List<Chiffre> chiffres(BilanBuvette b) {
        return List.of(new Chiffre(String.valueOf(b.nombreVentes()), b.nombreVentes() > 1 ? "Ventes" : "Vente"),
                new Chiffre(Centimes.euros(b.recetteCentimes()), "Recette"),
                new Chiffre(Centimes.euros(b.coutCentimes()), "Coût des articles vendus"),
                new Chiffre(Centimes.euros(b.margeCentimes()), "Marge"));
    }

    /** « 1 vente annulée (non comptée) · panier moyen 2,25 € · 4 articles vendus ». */
    static String complement(BilanBuvette b) {
        List<String> p = new ArrayList<>();
        p.add(b.nombreArticles() + (b.nombreArticles() > 1 ? " articles vendus" : " article vendu"));
        if (b.nombreVentes() > 0) {
            p.add("panier moyen " + Centimes.euros(b.panierMoyenCentimes()));
        }
        if (b.nombreAnnulees() > 0) {
            p.add(b.nombreAnnulees() + (b.nombreAnnulees() > 1 ? " ventes annulées" : " vente annulée")
                    + " (non comptées)");
        }
        return String.join(" · ", p);
    }

    /** Lignes du détail par article : nom, quantité, recette, coût, marge. */
    static List<List<String>> detailArticles(BilanBuvette b) {
        List<List<String>> res = new ArrayList<>();
        for (BilanBuvette.LigneArticle l : b.parArticle()) {
            res.add(List.of(l.nom(), String.valueOf(l.quantite()), Centimes.euros(l.recetteCentimes()),
                    Centimes.euros(l.coutCentimes()), Centimes.euros(l.margeCentimes())));
        }
        return res;
    }

    /** Lignes du détail par moyen de paiement : moyen, ventes, recette. */
    static List<List<String>> detailMoyens(BilanBuvette b) {
        List<List<String>> res = new ArrayList<>();
        for (BilanBuvette.LigneMoyen m : b.parMoyen()) {
            res.add(List.of(m.moyen().getLibelle(), String.valueOf(m.nombreVentes()),
                    Centimes.euros(m.recetteCentimes())));
        }
        return res;
    }

    /** Historique de la période, de la plus récente à la plus ancienne. */
    List<LigneHistorique> historique(LocalDate jour) {
        List<LigneHistorique> res = new ArrayList<>();
        for (int i = ventes.size() - 1; i >= 0; i--) {
            VenteBuvette v = ventes.get(i);
            var t = v.horodatage().atZone(zone);
            if (jour != null && !t.toLocalDate().equals(jour)) {
                continue;
            }
            List<String> a = new ArrayList<>();
            for (LigneVente l : v.lignes()) {
                a.add(l.quantite() + " × " + l.nom());
            }
            String etat = "";
            if (v.estAnnulee()) {
                var at = v.annulation().horodatage().atZone(zone);
                etat = "Annulée le " + at.format(DateTimeFormatter.ofPattern("dd/MM")) + " à "
                        + HEURE_SEULE.format(at) + " par " + v.annulation().auteur();
            }
            res.add(new LigneHistorique(v.numero(), "Vente n° " + v.numero() + " · " + HEURE.format(t),
                    String.join(", ", a), Centimes.euros(v.totalCentimes()) + " · " + v.moyen().getLibelle()
                    + " · " + v.auteur(), etat,
                    v.aRecompter().isEmpty() ? "" : "Stock insuffisant : " + String.join(", ", v.aRecompter()),
                    !v.estAnnulee()));
        }
        return res;
    }

    /** Nom du fichier proposé à l'export : « buvette-bilan-tournoi-2026-11-21.csv ». */
    static String nomFichier(String prefixe, String referenceTournoi, LocalDate jour) {
        String base = referenceTournoi.replaceAll("[^A-Za-z0-9_-]+", "-");
        return prefixe + "-" + base + (jour == null ? "" : "-" + jour) + ".csv";
    }
}
