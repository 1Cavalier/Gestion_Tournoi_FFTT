package com.pingmanager.core.buvette;

import com.pingmanager.core.Horloge;
import com.pingmanager.model.buvette.BuvetteTournoi;
import com.pingmanager.model.buvette.CatalogueBuvette;
import com.pingmanager.model.buvette.Ingredient;
import com.pingmanager.model.buvette.MouvementStock;
import com.pingmanager.model.buvette.TypeMouvementStock;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Stock de la buvette d'un tournoi (B046) : stock de départ, achats (date,
 * quantité, prix payé), pertes et dons, inventaire de fin, seuil d'alerte et
 * report du stock restant d'un autre tournoi. Chaque saisie est un mouvement
 * daté et signé de son auteur ; aucune suppression, une erreur se corrige par
 * un mouvement inverse.
 *
 * <p>Le stock se compte en <b>portions</b> ; un achat se saisit en unités d'achat
 * et se convertit avec le rendement minimal (prudent : on ne surestime pas le stock).</p>
 */
public final class ServiceStockBuvette {

    private final Horloge horloge;

    public ServiceStockBuvette(Horloge horloge) {
        this.horloge = Objects.requireNonNull(horloge, "horloge");
    }

    /** Stock théorique restant d'un ingrédient, en portions. */
    public static long stock(BuvetteTournoi buvette, String idIngredient) {
        long total = 0;
        for (MouvementStock m : buvette.getMouvements()) {
            if (m.idIngredient().equals(idIngredient)) {
                total += m.quantite();
            }
        }
        return total;
    }

    /** Saisit un stock de départ (portions comptées avant le tournoi). */
    public ResultatMouvementStock stockDepart(BuvetteTournoi buvette, CatalogueBuvette catalogue, String idIngredient,
                                             long portions, String auteur) {
        if (portions <= 0) {
            return ResultatMouvementStock.refus(ResultatMouvementStock.Motif.QUANTITE_INVALIDE);
        }
        return ajouter(buvette, catalogue, TypeMouvementStock.STOCK_DEPART, idIngredient, portions, 0, null, auteur,
                "", 0, -1);
    }

    /**
     * Saisit un achat en unités d'achat (« 10 baguettes ») avec le prix payé ; le stock
     * augmente de {@code unites × rendement minimal} portions.
     */
    public ResultatMouvementStock acheter(BuvetteTournoi buvette, CatalogueBuvette catalogue, String idIngredient,
                                         long unites, long prixPayeCentimes, LocalDate date, String auteur) {
        Ingredient i = catalogue.ingredient(idIngredient).orElse(null);
        if (i == null) {
            return ResultatMouvementStock.refus(ResultatMouvementStock.Motif.INGREDIENT_INCONNU);
        }
        if (unites <= 0 || unites > 100_000) {
            return ResultatMouvementStock.refus(ResultatMouvementStock.Motif.QUANTITE_INVALIDE);
        }
        if (prixPayeCentimes < 0 || prixPayeCentimes > 100 * Centimes.MAXIMUM) {
            return ResultatMouvementStock.refus(ResultatMouvementStock.Motif.PRIX_INVALIDE);
        }
        String detail = unites + " × " + i.getUniteAchat();
        return ajouter(buvette, catalogue, TypeMouvementStock.ACHAT, idIngredient, unites * i.getRendementMin(),
                prixPayeCentimes, date, auteur, detail, 0, -1);
    }

    /** Saisit une perte ou un don (portions positives, retirées du stock). */
    public ResultatMouvementStock sortie(BuvetteTournoi buvette, CatalogueBuvette catalogue, TypeMouvementStock type,
                                        String idIngredient, long portions, String auteur, String commentaire) {
        if (type != TypeMouvementStock.PERTE && type != TypeMouvementStock.DON) {
            throw new IllegalArgumentException("perte ou don seulement");
        }
        if (portions <= 0) {
            return ResultatMouvementStock.refus(ResultatMouvementStock.Motif.QUANTITE_INVALIDE);
        }
        return ajouter(buvette, catalogue, type, idIngredient, -portions, 0, null, auteur, commentaire, 0, -1);
    }

    /**
     * Inventaire de fin : le nombre de portions comptées remplace le stock théorique ;
     * l'écart (compté − théorique) est enregistré comme mouvement.
     */
    public ResultatMouvementStock inventaire(BuvetteTournoi buvette, CatalogueBuvette catalogue, String idIngredient,
                                            long compte, String auteur) {
        if (compte < 0) {
            return ResultatMouvementStock.refus(ResultatMouvementStock.Motif.QUANTITE_INVALIDE);
        }
        long ecart = compte - stock(buvette, idIngredient);
        return ajouter(buvette, catalogue, TypeMouvementStock.INVENTAIRE, idIngredient, ecart, 0, null, auteur,
                "compté : " + compte, 0, compte);
    }

    /**
     * Corrige un mouvement par son inverse (quantité et prix opposés). Une vente se
     * corrige en caisse ; une correction ne se corrige pas ; un mouvement ne se corrige
     * qu'une fois.
     */
    public ResultatMouvementStock corriger(BuvetteTournoi buvette, int numero, String auteur, String motif) {
        MouvementStock m = buvette.getMouvements().stream().filter(x -> x.numero() == numero).findFirst()
                .orElse(null);
        if (m == null) {
            return ResultatMouvementStock.refus(ResultatMouvementStock.Motif.MOUVEMENT_INCONNU);
        }
        if (m.type() == TypeMouvementStock.CORRECTION) {
            return ResultatMouvementStock.refus(ResultatMouvementStock.Motif.CORRECTION_NON_CORRIGEABLE);
        }
        if (m.type() == TypeMouvementStock.VENTE) {
            return ResultatMouvementStock.refus(ResultatMouvementStock.Motif.VENTE_A_ANNULER_EN_CAISSE);
        }
        if (estCorrige(buvette, numero)) {
            return ResultatMouvementStock.refus(ResultatMouvementStock.Motif.DEJA_CORRIGE);
        }
        MouvementStock inverse = new MouvementStock(buvette.prochainNumeroMouvement(), TypeMouvementStock.CORRECTION,
                m.idIngredient(), -m.quantite(), -m.prixPayeCentimes(), m.dateAchat(), horloge.maintenant(), auteur,
                motif == null || motif.isBlank() ? "annule le mouvement n° " + numero : motif, numero, -1);
        buvette.ajouterMouvement(inverse);
        return ResultatMouvementStock.ok(List.of(inverse));
    }

    /** @return vrai si le mouvement a déjà un mouvement de correction. */
    public static boolean estCorrige(BuvetteTournoi buvette, int numero) {
        return buvette.getMouvements().stream()
                .anyMatch(x -> x.type() == TypeMouvementStock.CORRECTION && x.numeroLie() == numero);
    }

    /** Fixe le seuil d'alerte d'un ingrédient (portions ; négatif = aucun seuil). */
    public ResultatMouvementStock definirSeuil(BuvetteTournoi buvette, CatalogueBuvette catalogue,
                                              String idIngredient, long seuil) {
        if (catalogue.ingredient(idIngredient).isEmpty()) {
            return ResultatMouvementStock.refus(ResultatMouvementStock.Motif.INGREDIENT_INCONNU);
        }
        buvette.definirSeuil(idIngredient, seuil);
        return ResultatMouvementStock.ok(List.of());
    }

    /**
     * Reporte le stock restant d'un autre tournoi (sur demande) : un mouvement
     * {@code REPORT} par ingrédient encore en stock. Le tournoi d'origine n'est pas
     * modifié ; un même tournoi ne se reporte qu'une fois.
     *
     * @param origine nom du tournoi d'origine, cité dans le commentaire
     */
    public ResultatMouvementStock reporter(BuvetteTournoi source, String origine, BuvetteTournoi cible,
                                          CatalogueBuvette catalogue, String auteur) {
        String commentaire = "report de " + origine;
        boolean dejaFait = cible.getMouvements().stream()
                .anyMatch(m -> m.type() == TypeMouvementStock.REPORT && m.commentaire().equals(commentaire)
                        && !estCorrige(cible, m.numero()));
        if (dejaFait) {
            return ResultatMouvementStock.refus(ResultatMouvementStock.Motif.DEJA_REPORTE);
        }
        Map<String, Long> restes = restants(source);
        List<MouvementStock> faits = new ArrayList<>();
        for (Map.Entry<String, Long> e : restes.entrySet()) {
            if (e.getValue() > 0 && catalogue.ingredient(e.getKey()).isPresent()) {
                faits.addAll(ajouter(cible, catalogue, TypeMouvementStock.REPORT, e.getKey(), e.getValue(), 0, null,
                        auteur, commentaire, 0, -1).mouvements());
            }
        }
        return faits.isEmpty() ? ResultatMouvementStock.refus(ResultatMouvementStock.Motif.RIEN_A_REPORTER)
                : ResultatMouvementStock.ok(faits);
    }

    /** Stock restant par ingrédient (ordre de première apparition). */
    public static Map<String, Long> restants(BuvetteTournoi buvette) {
        Map<String, Long> res = new LinkedHashMap<>();
        for (MouvementStock m : buvette.getMouvements()) {
            res.merge(m.idIngredient(), m.quantite(), Long::sum);
        }
        return res;
    }

    /**
     * État du stock de chaque ingrédient concerné : ceux du catalogue qui sont actifs
     * ou ont des mouvements, dans l'ordre du catalogue.
     */
    public static List<EtatStock> etat(BuvetteTournoi buvette, CatalogueBuvette catalogue) {
        Set<String> ids = new LinkedHashSet<>();
        for (Ingredient i : catalogue.getIngredients()) {
            if (i.isActif()) {
                ids.add(i.getId());
            }
        }
        buvette.getMouvements().forEach(m -> ids.add(m.idIngredient()));
        List<EtatStock> res = new ArrayList<>();
        for (String id : ids) {
            Ingredient i = catalogue.ingredient(id).orElse(Ingredient.builder().id(id).nom(id + " (retiré)").build());
            long depart = 0;
            long achats = 0;
            long ventes = 0;
            long pertes = 0;
            long ecarts = 0;
            long paye = 0;
            for (MouvementStock m : buvette.getMouvements()) {
                if (!m.idIngredient().equals(id)) {
                    continue;
                }
                TypeMouvementStock t = m.type();
                if (t == TypeMouvementStock.STOCK_DEPART || t == TypeMouvementStock.REPORT) {
                    depart += m.quantite();
                } else if (t == TypeMouvementStock.ACHAT) {
                    achats += m.quantite();
                } else if (t == TypeMouvementStock.VENTE) {
                    ventes -= m.quantite();
                } else if (t == TypeMouvementStock.PERTE || t == TypeMouvementStock.DON) {
                    pertes -= m.quantite();
                } else {
                    ecarts += m.quantite();
                }
                paye += m.prixPayeCentimes();
            }
            long restant = depart + achats - ventes - pertes + ecarts;
            res.add(new EtatStock(i, depart, achats, ventes, pertes, ecarts, restant,
                    buvette.seuil(id).orElse(-1), paye));
        }
        return res;
    }

    /** Ajoute un mouvement après contrôle de l'ingrédient, et le note « utilisé » au catalogue. */
    ResultatMouvementStock ajouter(BuvetteTournoi buvette, CatalogueBuvette catalogue, TypeMouvementStock type,
                                   String idIngredient, long quantite, long prix, LocalDate date, String auteur,
                                   String commentaire, int numeroLie, long compte) {
        if (catalogue.ingredient(idIngredient).isEmpty()) {
            return ResultatMouvementStock.refus(ResultatMouvementStock.Motif.INGREDIENT_INCONNU);
        }
        MouvementStock m = new MouvementStock(buvette.prochainNumeroMouvement(), type, idIngredient, quantite, prix,
                date, horloge.maintenant(), auteur, commentaire, numeroLie, compte);
        buvette.ajouterMouvement(m);
        catalogue.marquerIngredientUtilise(idIngredient);
        return ResultatMouvementStock.ok(List.of(m));
    }
}
