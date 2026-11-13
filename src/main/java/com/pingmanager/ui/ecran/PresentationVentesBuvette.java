package com.pingmanager.ui.ecran;

import com.pingmanager.core.buvette.Centimes;
import com.pingmanager.core.buvette.ResultatVenteBuvette;
import com.pingmanager.core.buvette.ServiceVentesBuvette;
import com.pingmanager.core.buvette.SyntheseVentes;
import com.pingmanager.model.buvette.BuvetteTournoi;
import com.pingmanager.model.buvette.CatalogueBuvette;
import com.pingmanager.model.buvette.CategorieProduit;
import com.pingmanager.model.buvette.Produit;
import com.pingmanager.model.buvette.VenteBuvette;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Présentation (sans JavaFX) de l'onglet « Ventes » de la page Buvette (B046) :
 * la caisse rapide d'un poste d'accueil. Un gros bouton par produit actif (avec
 * le nombre vendu aujourd'hui), totaux du jour et du tournoi, dernières ventes et
 * message après chaque clic.
 */
final class PresentationVentesBuvette {

    private static final DateTimeFormatter HEURE = DateTimeFormatter.ofPattern("HH:mm");

    record Tuile(String valeur, String libelle) {
    }

    record BoutonProduit(String id, String nom, String prix, String compteur) {
    }

    record Groupe(String titre, List<BoutonProduit> boutons) {
    }

    record LigneVente(String titre, String detail, boolean annulee, boolean annulation) {
    }

    private final BuvetteTournoi buvette;
    private final CatalogueBuvette catalogue;
    private final LocalDate jour;
    private final ZoneId zone;
    private final SyntheseVentes duJour;

    PresentationVentesBuvette(BuvetteTournoi buvette, CatalogueBuvette catalogue, LocalDate jour, ZoneId zone) {
        this.buvette = Objects.requireNonNull(buvette, "buvette");
        this.catalogue = Objects.requireNonNull(catalogue, "catalogue");
        this.jour = Objects.requireNonNull(jour, "jour");
        this.zone = Objects.requireNonNull(zone, "zone");
        this.duJour = ServiceVentesBuvette.synthese(buvette, jour, zone);
    }

    List<Tuile> tuiles() {
        SyntheseVentes tout = ServiceVentesBuvette.synthese(buvette, null, zone);
        return List.of(new Tuile(Centimes.euros(duJour.recetteCentimes()), "recette du jour"),
                new Tuile(String.valueOf(duJour.articles()), duJour.articles() > 1 ? "articles vendus aujourd'hui"
                        : "article vendu aujourd'hui"),
                new Tuile(Centimes.euros(tout.recetteCentimes()), "recette du tournoi"),
                new Tuile(ServiceVentesBuvette.POSTE_PAR_DEFAUT, "poste de caisse"));
    }

    /** Boutons des produits actifs, regroupés par catégorie. */
    List<Groupe> groupes() {
        List<Groupe> res = new ArrayList<>();
        for (CategorieProduit c : CategorieProduit.values()) {
            List<BoutonProduit> b = new ArrayList<>();
            for (Produit p : catalogue.getProduits()) {
                if (p.isActif() && p.getCategorie() == c) {
                    long n = duJour.quantite(p.getId());
                    b.add(new BoutonProduit(p.getId(), p.getNom(), Centimes.euros(p.getPrixVenteCentimes()),
                            n == 0 ? "" : "× " + n));
                }
            }
            if (!b.isEmpty()) {
                res.add(new Groupe(c.getLibelle(), b));
            }
        }
        return res;
    }

    boolean sansProduit() {
        return groupes().isEmpty();
    }

    String messageSansProduit() {
        return "Aucun produit actif : ajoutez des produits dans l'onglet Catalogue pour ouvrir la caisse.";
    }

    /** Dix dernières ventes, la plus récente en tête. */
    List<LigneVente> dernieres() {
        Set<Integer> annulees = ServiceVentesBuvette.annulees(buvette);
        List<LigneVente> res = new ArrayList<>();
        List<VenteBuvette> v = buvette.getVentes();
        for (int i = v.size() - 1; i >= 0 && res.size() < 10; i--) {
            VenteBuvette x = v.get(i);
            String titre = x.estAnnulation()
                    ? "Annulation de la vente n° " + x.numeroAnnule() + " · " + x.nomProduit()
                    : "n° " + x.numero() + " · " + x.nomProduit() + " · " + Centimes.euros(x.prixCentimes());
            String detail = HEURE.format(x.horodatage().atZone(zone)) + " · " + x.auteur() + " · " + x.poste();
            res.add(new LigneVente(titre, detail, annulees.contains(x.numero()), x.estAnnulation()));
        }
        return res;
    }

    /** Message affiché après un clic. */
    String message(ResultatVenteBuvette r) {
        if (!r.accepte()) {
            return r.motif().explication();
        }
        SyntheseVentes apres = ServiceVentesBuvette.synthese(buvette, jour, zone);
        String debut = r.motif() == ResultatVenteBuvette.Motif.ANNULEE
                ? "Vente n° " + r.vente().numeroAnnule() + " annulée (" + r.vente().nomProduit() + ")"
                : r.vente().nomProduit() + " : " + Centimes.euros(r.vente().prixCentimes());
        String stock = r.stockEpuise().isEmpty() ? ""
                : " Attention, stock épuisé : " + String.join(", ", r.stockEpuise()) + " (vérifiez l'inventaire).";
        return debut + ". Total du jour " + Centimes.euros(apres.recetteCentimes()) + "." + stock;
    }
}
