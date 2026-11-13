package com.pingmanager.core.buvette;

import com.pingmanager.core.Horloge;
import com.pingmanager.model.buvette.BuvetteTournoi;
import com.pingmanager.model.buvette.CatalogueBuvette;
import com.pingmanager.model.buvette.Ingredient;
import com.pingmanager.model.buvette.LigneRecette;
import com.pingmanager.model.buvette.MouvementStock;
import com.pingmanager.model.buvette.Produit;
import com.pingmanager.model.buvette.TypeMouvementStock;
import com.pingmanager.model.buvette.VenteBuvette;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Caisse de la buvette (B046) : une vente par clic, annulation de la dernière
 * vente (d'un produit ou de toutes), totaux. Chaque vente déduit le stock selon
 * la recette du produit (mouvements {@code VENTE}) ; son annulation remet le
 * stock par des mouvements de correction. Rien ne se supprime.
 *
 * <p>Une seule caisse par défaut ({@link #POSTE_PAR_DEFAUT}) ; plusieurs postes
 * et leur synchronisation relèvent d'une autre tâche (README, section 6).</p>
 */
public final class ServiceVentesBuvette {

    /** Poste de caisse unique, par défaut. */
    public static final String POSTE_PAR_DEFAUT = "Caisse 1";

    private final Horloge horloge;
    private final ServiceStockBuvette stock;

    public ServiceVentesBuvette(Horloge horloge) {
        this.horloge = Objects.requireNonNull(horloge, "horloge");
        this.stock = new ServiceStockBuvette(horloge);
    }

    /** Vend une unité d'un produit actif et déduit le stock selon sa recette. */
    public ResultatVenteBuvette vendre(BuvetteTournoi buvette, CatalogueBuvette catalogue, String idProduit,
                                       String auteur, String poste) {
        Produit p = catalogue.produit(idProduit).orElse(null);
        if (p == null) {
            return ResultatVenteBuvette.refus(ResultatVenteBuvette.Motif.PRODUIT_INCONNU);
        }
        if (!p.isActif()) {
            return ResultatVenteBuvette.refus(ResultatVenteBuvette.Motif.PRODUIT_INACTIF);
        }
        CoutProduit cout = new CalculCoutBuvette().cout(p, catalogue);
        VenteBuvette v = new VenteBuvette(buvette.prochainNumeroVente(), horloge.maintenant(), auteur, poste,
                p.getId(), p.getNom(), p.getPrixVenteCentimes(), cout.connu() ? cout.coutCentimes() : -1, 0);
        buvette.ajouterVente(v);
        catalogue.marquerVendu(p.getId());
        List<String> epuises = new ArrayList<>();
        for (LigneRecette l : p.getRecette()) {
            ResultatMouvementStock r = stock.ajouter(buvette, catalogue, TypeMouvementStock.VENTE, l.idIngredient(),
                    -l.quantite(), 0, null, auteur, "vente n° " + v.numero() + " : " + p.getNom(), v.numero(), -1);
            if (r.accepte() && ServiceStockBuvette.stock(buvette, l.idIngredient()) < 0) {
                epuises.add(catalogue.ingredient(l.idIngredient()).map(Ingredient::getNom).orElse(l.idIngredient()));
            }
        }
        return new ResultatVenteBuvette(ResultatVenteBuvette.Motif.VENDUE, v, epuises);
    }

    /**
     * Annule la dernière vente non annulée (de ce produit si {@code idProduit} est
     * donné, sinon de n'importe quel produit) : enregistrement d'annulation et
     * remise en stock par correction des mouvements de cette vente.
     */
    public ResultatVenteBuvette annulerDerniere(BuvetteTournoi buvette, CatalogueBuvette catalogue,
                                                String idProduit, String auteur, String poste) {
        Set<Integer> annulees = annulees(buvette);
        VenteBuvette cible = null;
        List<VenteBuvette> ventes = buvette.getVentes();
        for (int i = ventes.size() - 1; i >= 0 && cible == null; i--) {
            VenteBuvette v = ventes.get(i);
            if (!v.estAnnulation() && !annulees.contains(v.numero())
                    && (idProduit == null || v.idProduit().equals(idProduit))) {
                cible = v;
            }
        }
        if (cible == null) {
            return ResultatVenteBuvette.refus(ResultatVenteBuvette.Motif.RIEN_A_ANNULER);
        }
        VenteBuvette annulation = new VenteBuvette(buvette.prochainNumeroVente(), horloge.maintenant(), auteur, poste,
                cible.idProduit(), cible.nomProduit(), -cible.prixCentimes(), cible.coutCentimes(), cible.numero());
        buvette.ajouterVente(annulation);
        for (MouvementStock m : new ArrayList<>(buvette.getMouvements())) {
            if (m.type() == TypeMouvementStock.VENTE && m.numeroLie() == cible.numero()) {
                buvette.ajouterMouvement(new MouvementStock(buvette.prochainNumeroMouvement(),
                        TypeMouvementStock.CORRECTION, m.idIngredient(), -m.quantite(), 0, null, horloge.maintenant(),
                        auteur, "annulation de la vente n° " + cible.numero(), m.numero(), -1));
            }
        }
        return new ResultatVenteBuvette(ResultatVenteBuvette.Motif.ANNULEE, annulation, List.of());
    }

    /** Numéros des ventes annulées. */
    public static Set<Integer> annulees(BuvetteTournoi buvette) {
        Set<Integer> res = new HashSet<>();
        buvette.getVentes().forEach(v -> {
            if (v.estAnnulation()) {
                res.add(v.numeroAnnule());
            }
        });
        return res;
    }

    /**
     * Synthèse des ventes nettes (annulations déduites) d'un jour, ou de tout le
     * tournoi si {@code jour} est {@code null}.
     */
    public static SyntheseVentes synthese(BuvetteTournoi buvette, LocalDate jour, ZoneId zone) {
        Map<String, Long> quantites = new LinkedHashMap<>();
        Map<String, String> noms = new LinkedHashMap<>();
        long recette = 0;
        long cout = 0;
        boolean coutComplet = true;
        long articles = 0;
        for (VenteBuvette v : buvette.getVentes()) {
            if (jour != null && !v.horodatage().atZone(zone).toLocalDate().equals(jour)) {
                continue;
            }
            int signe = v.estAnnulation() ? -1 : 1;
            articles += signe;
            recette += v.prixCentimes();
            if (v.coutCentimes() >= 0) {
                cout += signe * v.coutCentimes();
            } else {
                coutComplet = false;
            }
            quantites.merge(v.idProduit(), (long) signe, Long::sum);
            noms.putIfAbsent(v.idProduit(), v.nomProduit());
        }
        return new SyntheseVentes(articles, recette, cout, coutComplet, quantites, noms);
    }
}
