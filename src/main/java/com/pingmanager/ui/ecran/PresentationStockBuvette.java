package com.pingmanager.ui.ecran;

import com.pingmanager.core.buvette.CalculCoutBuvette;
import com.pingmanager.core.buvette.Centimes;
import com.pingmanager.core.buvette.EtatStock;
import com.pingmanager.core.buvette.ResultatMouvementStock;
import com.pingmanager.core.buvette.ServiceStockBuvette;
import com.pingmanager.model.buvette.BuvetteTournoi;
import com.pingmanager.model.buvette.CatalogueBuvette;
import com.pingmanager.model.buvette.Ingredient;
import com.pingmanager.model.buvette.MouvementStock;
import com.pingmanager.model.buvette.TypeMouvementStock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.OptionalLong;

/**
 * Présentation (sans JavaFX) de l'onglet « Stock » de la page Buvette (B046) :
 * tuiles, état par ingrédient (avec alerte sous le seuil), journal des
 * mouvements (le plus récent en tête) et saisie d'un mouvement.
 */
final class PresentationStockBuvette {

    /** Types proposés à la saisie (les ventes viennent de la caisse, les corrections du journal). */
    static final TypeMouvementStock[] TYPES_SAISIS = {TypeMouvementStock.ACHAT, TypeMouvementStock.STOCK_DEPART,
            TypeMouvementStock.PERTE, TypeMouvementStock.DON, TypeMouvementStock.INVENTAIRE};

    private static final DateTimeFormatter HEURE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    record Tuile(String valeur, String libelle) {
    }

    record LigneStock(String id, String nom, String detail, boolean sousLeSeuil) {
    }

    record LigneMouvement(int numero, String titre, String detail, boolean corrigeable, boolean corrige) {
    }

    private final BuvetteTournoi buvette;
    private final CatalogueBuvette catalogue;
    private final ZoneId zone;

    PresentationStockBuvette(BuvetteTournoi buvette, CatalogueBuvette catalogue, ZoneId zone) {
        this.buvette = Objects.requireNonNull(buvette, "buvette");
        this.catalogue = Objects.requireNonNull(catalogue, "catalogue");
        this.zone = Objects.requireNonNull(zone, "zone");
    }

    List<Tuile> tuiles() {
        List<EtatStock> etat = ServiceStockBuvette.etat(buvette, catalogue);
        long sous = etat.stream().filter(EtatStock::sousLeSeuil).count();
        BigDecimal valeur = BigDecimal.ZERO;
        long paye = 0;
        for (EtatStock e : etat) {
            if (e.restant() > 0 && catalogue.ingredient(e.ingredient().getId()).isPresent()) {
                valeur = valeur.add(CalculCoutBuvette.coutPortion(e.ingredient(), true)
                        .multiply(BigDecimal.valueOf(e.restant())));
            }
            paye += e.achatsPayesCentimes();
        }
        return List.of(
                new Tuile(String.valueOf(etat.size()), etat.size() > 1 ? "ingrédients suivis" : "ingrédient suivi"),
                new Tuile(String.valueOf(sous), "sous le seuil d'alerte"),
                new Tuile(Centimes.euros(Centimes.arrondir(valeur)), "valeur du stock restant (au coût d'achat)"),
                new Tuile(Centimes.euros(paye), "achats payés pour ce tournoi"));
    }

    boolean sansIngredient() {
        return catalogue.getIngredients().isEmpty();
    }

    String messageSansIngredient() {
        return "Ajoutez d'abord des ingrédients dans l'onglet Catalogue (ou acceptez les suggestions).";
    }

    List<LigneStock> lignes() {
        List<LigneStock> res = new ArrayList<>();
        for (EtatStock e : ServiceStockBuvette.etat(buvette, catalogue)) {
            Ingredient i = e.ingredient();
            StringBuilder d = new StringBuilder("Reste ").append(e.restant()).append(' ').append(i.getUnitePortion());
            if (e.restant() > 0 && i.getRendementMin() > 1) {
                d.append(" (≈ ").append(e.unitesAchatEntieres()).append(" × ").append(i.getUniteAchat()).append(')');
            }
            d.append(" · départ ").append(e.depart()).append(" · achats ").append(e.achats())
                    .append(" · ventes ").append(e.ventes());
            if (e.pertesDons() != 0) {
                d.append(" · pertes et dons ").append(e.pertesDons());
            }
            if (e.ecartsInventaire() != 0) {
                d.append(" · écarts ").append(e.ecartsInventaire() > 0 ? "+" : "").append(e.ecartsInventaire());
            }
            if (e.seuil() >= 0) {
                d.append(" · seuil ").append(e.seuil());
            }
            res.add(new LigneStock(i.getId(), i.getNom(), d.toString(), e.sousLeSeuil()));
        }
        return res;
    }

    /** Journal, le plus récent en tête. */
    List<LigneMouvement> journal() {
        List<LigneMouvement> res = new ArrayList<>();
        List<MouvementStock> ms = buvette.getMouvements();
        for (int k = ms.size() - 1; k >= 0; k--) {
            MouvementStock m = ms.get(k);
            String nom = catalogue.ingredient(m.idIngredient()).map(Ingredient::getNom).orElse(m.idIngredient());
            String unite = catalogue.ingredient(m.idIngredient()).map(Ingredient::getUnitePortion).orElse("portion");
            String titre = "n° " + m.numero() + " · " + m.type().getLibelle() + " · " + nom + " : "
                    + (m.quantite() > 0 ? "+" : "") + m.quantite() + " " + unite;
            StringBuilder d = new StringBuilder(HEURE.format(m.horodatage().atZone(zone))).append(" · ").append(m.auteur());
            if (m.dateAchat() != null) {
                d.append(" · acheté le ").append(JOUR.format(m.dateAchat()));
            }
            if (m.prixPayeCentimes() != 0) {
                d.append(" · ").append(Centimes.euros(m.prixPayeCentimes()));
            }
            if (!m.commentaire().isEmpty()) {
                d.append(" · ").append(m.commentaire());
            }
            boolean corrige = ServiceStockBuvette.estCorrige(buvette, m.numero());
            boolean corrigeable = !corrige && m.type() != TypeMouvementStock.CORRECTION
                    && m.type() != TypeMouvementStock.VENTE;
            res.add(new LigneMouvement(m.numero(), titre, d.toString(), corrigeable, corrige));
        }
        return res;
    }

    /** Libellé du champ de quantité selon le type de mouvement. */
    static String libelleQuantite(TypeMouvementStock type) {
        if (type == TypeMouvementStock.ACHAT) {
            return "Unités d'achat";
        }
        if (type == TypeMouvementStock.INVENTAIRE) {
            return "Portions comptées";
        }
        return "Portions";
    }

    /**
     * Enregistre la saisie du formulaire (quantité et prix lus ici), via le service.
     *
     * @return le résultat, avec un motif précis en cas de refus
     */
    static ResultatMouvementStock saisir(ServiceStockBuvette service, BuvetteTournoi buvette,
                                         CatalogueBuvette catalogue, TypeMouvementStock type, String idIngredient,
                                         String quantiteSaisie, String prixSaisi, LocalDate date, String auteur) {
        OptionalLong q = entier(quantiteSaisie);
        if (q.isEmpty()) {
            return new ResultatMouvementStock(ResultatMouvementStock.Motif.QUANTITE_INVALIDE, List.of());
        }
        if (idIngredient == null || catalogue.ingredient(idIngredient).isEmpty()) {
            return new ResultatMouvementStock(ResultatMouvementStock.Motif.INGREDIENT_INCONNU, List.of());
        }
        if (type == TypeMouvementStock.ACHAT) {
            OptionalLong prix = prixSaisi == null || prixSaisi.isBlank() ? OptionalLong.of(0) : lirePrixTotal(prixSaisi);
            if (prix.isEmpty()) {
                return new ResultatMouvementStock(ResultatMouvementStock.Motif.PRIX_INVALIDE, List.of());
            }
            return service.acheter(buvette, catalogue, idIngredient, q.getAsLong(), prix.getAsLong(), date, auteur);
        }
        if (type == TypeMouvementStock.INVENTAIRE) {
            return service.inventaire(buvette, catalogue, idIngredient, q.getAsLong(), auteur);
        }
        if (type == TypeMouvementStock.STOCK_DEPART) {
            return service.stockDepart(buvette, catalogue, idIngredient, q.getAsLong(), auteur);
        }
        return service.sortie(buvette, catalogue, type, idIngredient, q.getAsLong(), auteur, "");
    }

    /** Prix total d'un achat : jusqu'à 100 000 € (plusieurs cartons). */
    private static OptionalLong lirePrixTotal(String s) {
        String t = s.replace("€", "").replace(" ", "").replace(' ', ' ').replace(" ", "").replace(',', '.').trim();
        if (!t.matches("\\d{1,7}(\\.\\d{0,2})?")) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(new BigDecimal(t).movePointRight(2).longValueExact());
    }

    private static OptionalLong entier(String s) {
        if (s == null || !s.trim().matches("\\d{1,7}")) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(Long.parseLong(s.trim()));
    }

    /**
     * Index du tournoi proposé par défaut : le prochain à venir (ou en cours), sinon le
     * plus récent ; -1 si la liste est vide.
     */
    static int tournoiParDefaut(List<LocalDate> dates, LocalDate aujourdhui) {
        int prochain = -1;
        int recent = -1;
        for (int i = 0; i < dates.size(); i++) {
            LocalDate d = dates.get(i);
            if (d == null) {
                continue;
            }
            if (!d.isBefore(aujourdhui) && (prochain < 0 || d.isBefore(dates.get(prochain)))) {
                prochain = i;
            }
            if (d.isBefore(aujourdhui) && (recent < 0 || d.isAfter(dates.get(recent)))) {
                recent = i;
            }
        }
        if (prochain >= 0) {
            return prochain;
        }
        return recent >= 0 ? recent : (dates.isEmpty() ? -1 : 0);
    }
}
