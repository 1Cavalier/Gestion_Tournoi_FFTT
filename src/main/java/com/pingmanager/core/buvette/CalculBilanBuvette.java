package com.pingmanager.core.buvette;

import com.pingmanager.model.buvette.LigneVente;
import com.pingmanager.model.buvette.MoyenPaiement;
import com.pingmanager.model.buvette.VenteBuvette;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Calcul du bilan de la buvette (B026) à partir des ventes d'un tournoi :
 * nombre de ventes, recette, coût des articles vendus (figé à la vente),
 * marge, détail par article et par moyen de paiement.
 */
public final class CalculBilanBuvette {

    private CalculBilanBuvette() {
    }

    /** Bilan de tout le tournoi. */
    public static BilanBuvette bilan(List<VenteBuvette> ventes) {
        return calculer(ventes, null, null);
    }

    /** Bilan d'une journée (date de la vente dans le fuseau donné). */
    public static BilanBuvette bilanDuJour(List<VenteBuvette> ventes, LocalDate jour, ZoneId zone) {
        return calculer(ventes, jour, zone);
    }

    /** @return les journées où au moins une vente a été enregistrée, dans l'ordre. */
    public static List<LocalDate> jours(List<VenteBuvette> ventes, ZoneId zone) {
        TreeSet<LocalDate> res = new TreeSet<>();
        for (VenteBuvette v : ventes) {
            res.add(v.horodatage().atZone(zone).toLocalDate());
        }
        return new ArrayList<>(res);
    }

    private static BilanBuvette calculer(List<VenteBuvette> ventes, LocalDate jour, ZoneId zone) {
        int nb = 0;
        int annulees = 0;
        int articles = 0;
        long recette = 0;
        BigDecimal cout = BigDecimal.ZERO;
        Map<String, long[]> quantitesRecettes = new LinkedHashMap<>();
        Map<String, BigDecimal> couts = new LinkedHashMap<>();
        Map<String, String> noms = new LinkedHashMap<>();
        Map<MoyenPaiement, long[]> moyens = new LinkedHashMap<>();
        for (MoyenPaiement m : MoyenPaiement.values()) {
            moyens.put(m, new long[2]);
        }
        for (VenteBuvette v : ventes) {
            if (jour != null && !v.horodatage().atZone(zone).toLocalDate().equals(jour)) {
                continue;
            }
            if (v.estAnnulee()) {
                annulees++;
                continue;
            }
            nb++;
            articles += v.nombreArticles();
            recette += v.totalCentimes();
            cout = cout.add(v.coutTotal());
            long[] m = moyens.get(v.moyen());
            m[0]++;
            m[1] += v.totalCentimes();
            for (LigneVente l : v.lignes()) {
                long[] qr = quantitesRecettes.computeIfAbsent(l.idArticle(), k -> new long[2]);
                qr[0] += l.quantite();
                qr[1] += l.montantCentimes();
                couts.merge(l.idArticle(), l.cout(), BigDecimal::add);
                noms.put(l.idArticle(), l.nom());
            }
        }
        List<BilanBuvette.LigneArticle> parArticle = new ArrayList<>();
        for (Map.Entry<String, long[]> e : quantitesRecettes.entrySet()) {
            parArticle.add(new BilanBuvette.LigneArticle(e.getKey(), noms.get(e.getKey()), e.getValue()[0],
                    e.getValue()[1], Centimes.arrondir(couts.get(e.getKey()))));
        }
        parArticle.sort(Comparator.comparingLong(BilanBuvette.LigneArticle::recetteCentimes).reversed()
                .thenComparing(BilanBuvette.LigneArticle::nom));
        List<BilanBuvette.LigneMoyen> parMoyen = new ArrayList<>();
        for (Map.Entry<MoyenPaiement, long[]> e : moyens.entrySet()) {
            parMoyen.add(new BilanBuvette.LigneMoyen(e.getKey(), (int) e.getValue()[0], e.getValue()[1]));
        }
        return new BilanBuvette(jour, nb, annulees, articles, recette, Centimes.arrondir(cout), parArticle,
                parMoyen);
    }
}
