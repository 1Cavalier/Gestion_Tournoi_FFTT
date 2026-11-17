package com.pingmanager.core.buvette;

import com.pingmanager.model.buvette.ArticleBuvette;
import com.pingmanager.model.buvette.ArticleCatalogue;
import com.pingmanager.model.buvette.BuvetteTournoi;
import com.pingmanager.model.buvette.CatalogueBuvette;
import com.pingmanager.model.buvette.FamilleArticle;
import com.pingmanager.model.buvette.LienContenant;
import com.pingmanager.model.buvette.LigneVente;
import com.pingmanager.model.buvette.MouvementStock;
import com.pingmanager.model.buvette.RayonBuvette;
import com.pingmanager.model.buvette.StockBuvette;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Articles du club pour la buvette (B026) : création de zéro, copie depuis le
 * catalogue de base, modification, archivage (retrait de la vente sans perte
 * d'historique) et suppression, avec contrôle de la saisie. Ne connaît ni
 * l'écran ni l'enregistrement.
 *
 * <p>Le catalogue de base est en lecture seule : copier un article en fait un
 * article du club, indépendant ; le modifier ne touche jamais le catalogue.</p>
 */
public final class ServiceArticlesBuvette {

    /** Stock, seuil et unités par lot maximaux acceptés à la saisie. */
    public static final long QUANTITE_MAX = 1_000_000;

    /**
     * Ajoute (id {@code null}) ou modifie un article. En modification, le stock
     * n'est pas repris de la saisie : il ne change que par entrée, sortie ou
     * recomptage (pour que chaque changement soit dans le journal).
     */
    public ResultatArticleBuvette enregistrer(StockBuvette stock, SaisieArticle s) {
        ArticleBuvette existant = null;
        if (s.id() != null) {
            existant = stock.article(s.id()).orElse(null);
            if (existant == null) {
                return ResultatArticleBuvette.refus(ResultatArticleBuvette.Motif.INCONNU);
            }
        }
        String nom = s.nom() == null ? "" : s.nom().trim().replaceAll("\\s+", " ");
        if (nom.isEmpty()) {
            return ResultatArticleBuvette.refus(ResultatArticleBuvette.Motif.NOM_OBLIGATOIRE);
        }
        for (ArticleBuvette a : stock.getArticles()) {
            if (!a.getId().equals(s.id()) && cle(a.getNom()).equals(cle(nom))) {
                return ResultatArticleBuvette.refus(ResultatArticleBuvette.Motif.NOM_EN_DOUBLE,
                        a.getNom() + (a.estArchive() ? " (retiré de la vente)" : ""));
            }
        }
        FamilleArticle famille = s.famille() == null ? FamilleArticle.PRODUIT : s.famille();
        if (existant != null && famille == FamilleArticle.PRODUIT && !existant.estProduit()) {
            List<ArticleBuvette> utilisateurs = stock.produitsUtilisant(existant.getId());
            if (!utilisateurs.isEmpty()) {
                return ResultatArticleBuvette.refus(ResultatArticleBuvette.Motif.FAMILLE_VERROUILLEE,
                        noms(utilisateurs));
            }
        }
        OptionalLong prix = vide(s.prixLot()) ? OptionalLong.of(0) : Centimes.lire(s.prixLot());
        if (prix.isEmpty()) {
            return ResultatArticleBuvette.refus(ResultatArticleBuvette.Motif.PRIX_INVALIDE);
        }
        long prixVente;
        if (s.prixVente() == null) {
            prixVente = existant == null ? 0 : existant.getPrixVenteCentimes();
        } else {
            OptionalLong pv = vide(s.prixVente()) ? OptionalLong.of(0) : Centimes.lire(s.prixVente());
            if (pv.isEmpty()) {
                return ResultatArticleBuvette.refus(ResultatArticleBuvette.Motif.PRIX_VENTE_INVALIDE);
            }
            prixVente = pv.getAsLong();
        }
        RayonBuvette rayon = s.rayon() != null ? s.rayon() : existant == null ? null : existant.getRayon();
        OptionalLong lot = entier(s.unitesParLot(), 1);
        if (lot.isEmpty() || lot.getAsLong() < 1) {
            return ResultatArticleBuvette.refus(ResultatArticleBuvette.Motif.LOT_INVALIDE);
        }
        long quantiteStock;
        if (existant != null) {
            quantiteStock = existant.getStock();
        } else {
            OptionalLong q = entier(s.stock(), 0);
            if (q.isEmpty()) {
                return ResultatArticleBuvette.refus(ResultatArticleBuvette.Motif.STOCK_INVALIDE);
            }
            quantiteStock = q.getAsLong();
        }
        OptionalLong seuil = entier(s.seuil(), 0);
        if (seuil.isEmpty()) {
            return ResultatArticleBuvette.refus(ResultatArticleBuvette.Motif.SEUIL_INVALIDE);
        }
        List<LienContenant> liens = new ArrayList<>();
        if (famille == FamilleArticle.PRODUIT) {
            for (Map.Entry<String, Integer> e : s.contenants().entrySet()) {
                Optional<ArticleBuvette> c = stock.article(e.getKey());
                if (c.isEmpty() || c.get().estProduit() || e.getValue() == null || e.getValue() < 1) {
                    return ResultatArticleBuvette.refus(ResultatArticleBuvette.Motif.CONTENANT_INVALIDE);
                }
            }
            // Ordre stable : celui des contenants dans le stock.
            for (ArticleBuvette c : stock.articles(FamilleArticle.CONTENANT)) {
                Integer q = s.contenants().get(c.getId());
                if (q != null) {
                    liens.add(new LienContenant(c.getId(), q));
                }
            }
        }
        ArticleBuvette a = ArticleBuvette.builder()
                .id(existant == null ? stock.idLibre(nom) : existant.getId())
                .nom(nom)
                .famille(famille)
                .unite(s.unite())
                .lot(prix.getAsLong(), (int) lot.getAsLong())
                .stock(quantiteStock)
                .seuilAlerte(seuil.getAsLong())
                .contenants(liens)
                .rayon(rayon)
                .prixVenteCentimes(famille == FamilleArticle.PRODUIT ? prixVente : 0)
                .archive(existant != null && existant.estArchive())
                .idCatalogue(existant == null ? "" : existant.getIdCatalogue())
                .build();
        stock.enregistrer(a);
        return new ResultatArticleBuvette(existant == null ? ResultatArticleBuvette.Motif.AJOUTE
                : ResultatArticleBuvette.Motif.MODIFIE, a, nom);
    }

    /**
     * Copie des articles du catalogue de base dans les articles du club (stock
     * à 0). Pour un produit, ses contenants sont ajoutés aussi s'ils manquent ;
     * un contenant déjà présent (même article du catalogue, ou même nom) est
     * réutilisé. Un article déjà présent n'est pas ajouté une deuxième fois.
     *
     * @param ids articles du catalogue cochés (ordre indifférent : celui du catalogue est suivi)
     */
    public ResultatAjoutCatalogue ajouterDepuisCatalogue(StockBuvette stock, CatalogueBuvette catalogue,
                                                         Collection<String> ids) {
        List<String> ajoutes = new ArrayList<>();
        List<String> deja = new ArrayList<>();
        Map<String, String> correspondance = new LinkedHashMap<>(); // id catalogue → id du club
        for (ArticleCatalogue c : catalogue.getArticles()) {
            if (!ids.contains(c.id())) {
                continue;
            }
            for (LienContenant l : c.composition()) {
                ArticleCatalogue cont = catalogue.article(l.idContenant()).orElseThrow();
                if (!correspondance.containsKey(cont.id())) {
                    Optional<ArticleBuvette> present = present(stock, cont);
                    if (present.isPresent()) {
                        correspondance.put(cont.id(), present.get().getId());
                    } else {
                        ArticleBuvette copie = copier(stock, cont, List.of());
                        correspondance.put(cont.id(), copie.getId());
                        ajoutes.add(copie.getNom());
                    }
                }
            }
            Optional<ArticleBuvette> present = present(stock, c);
            if (present.isPresent()) {
                correspondance.putIfAbsent(c.id(), present.get().getId());
                if (!ajoutes.contains(present.get().getNom()) && !deja.contains(present.get().getNom())) {
                    deja.add(present.get().getNom());
                }
                continue;
            }
            List<LienContenant> liens = new ArrayList<>();
            for (LienContenant l : c.composition()) {
                liens.add(new LienContenant(correspondance.get(l.idContenant()), l.quantite()));
            }
            ArticleBuvette copie = copier(stock, c, liens);
            correspondance.put(c.id(), copie.getId());
            ajoutes.add(copie.getNom());
        }
        return new ResultatAjoutCatalogue(ajoutes, deja);
    }

    /** Article du club déjà copié de cet article du catalogue, ou de même nom et même famille. */
    private static Optional<ArticleBuvette> present(StockBuvette stock, ArticleCatalogue c) {
        for (ArticleBuvette a : stock.getArticles()) {
            if (a.getIdCatalogue().equals(c.id())) {
                return Optional.of(a);
            }
        }
        for (ArticleBuvette a : stock.getArticles()) {
            if (cle(a.getNom()).equals(cle(c.nom())) && a.getFamille() == c.famille()) {
                return Optional.of(a);
            }
        }
        return Optional.empty();
    }

    private static ArticleBuvette copier(StockBuvette stock, ArticleCatalogue c, List<LienContenant> liens) {
        String nom = c.nom();
        // Un article d'une autre famille porte déjà ce nom : on distingue la copie.
        for (ArticleBuvette a : stock.getArticles()) {
            if (cle(a.getNom()).equals(cle(nom))) {
                nom = nom + " (" + c.rayon().getLibelle().toLowerCase() + ")";
                break;
            }
        }
        ArticleBuvette a = ArticleBuvette.builder()
                .id(stock.idLibre(c.id()))
                .nom(nom)
                .famille(c.famille())
                .rayon(c.rayon())
                .unite(c.unite())
                .lot(c.prixLotCentimes(), c.unitesParLot())
                .prixVenteCentimes(c.prixVenteCentimes())
                .seuilAlerte(c.seuilAlerte())
                .contenants(liens)
                .idCatalogue(c.id())
                .build();
        stock.enregistrer(a);
        return a;
    }

    /**
     * Retire un article de la vente (archivage) : il disparaît du comptoir et
     * des listes, son historique (journal, ventes) est gardé. Refusé pour un
     * contenant qui sert encore à un produit en vente.
     */
    public ResultatArticleBuvette archiver(StockBuvette stock, String id) {
        Optional<ArticleBuvette> a = stock.article(id);
        if (a.isEmpty()) {
            return ResultatArticleBuvette.refus(ResultatArticleBuvette.Motif.INCONNU);
        }
        if (a.get().estArchive()) {
            return ResultatArticleBuvette.refus(ResultatArticleBuvette.Motif.DEJA_ARCHIVE);
        }
        List<ArticleBuvette> utilisateurs = new ArrayList<>();
        for (ArticleBuvette p : stock.produitsUtilisant(id)) {
            if (!p.estArchive()) {
                utilisateurs.add(p);
            }
        }
        if (!utilisateurs.isEmpty()) {
            return ResultatArticleBuvette.refus(ResultatArticleBuvette.Motif.CONTENANT_UTILISE, noms(utilisateurs));
        }
        ArticleBuvette archive = a.get().avecArchive(true);
        stock.enregistrer(archive);
        return new ResultatArticleBuvette(ResultatArticleBuvette.Motif.ARCHIVE, archive, archive.getNom());
    }

    /** Remet en vente un article archivé (et ses contenants archivés, sans quoi il ne pourrait pas servir). */
    public ResultatArticleBuvette remettreEnVente(StockBuvette stock, String id) {
        Optional<ArticleBuvette> a = stock.article(id);
        if (a.isEmpty()) {
            return ResultatArticleBuvette.refus(ResultatArticleBuvette.Motif.INCONNU);
        }
        if (!a.get().estArchive()) {
            return ResultatArticleBuvette.refus(ResultatArticleBuvette.Motif.NON_ARCHIVE);
        }
        for (LienContenant l : a.get().getContenants()) {
            stock.article(l.idContenant()).filter(ArticleBuvette::estArchive)
                    .ifPresent(c -> stock.enregistrer(c.avecArchive(false)));
        }
        ArticleBuvette remis = a.get().avecArchive(false);
        stock.enregistrer(remis);
        return new ResultatArticleBuvette(ResultatArticleBuvette.Motif.REMIS_EN_VENTE, remis, remis.getNom());
    }

    /** @return vrai si l'article apparaît dans le journal de stock ou dans une vente d'un tournoi. */
    public static boolean aUnHistorique(StockBuvette stock, String id) {
        for (MouvementStock m : stock.getJournal()) {
            if (m.idArticle().equals(id)) {
                return true;
            }
        }
        for (BuvetteTournoi t : stock.getTournois()) {
            for (var v : t.getVentes()) {
                for (LigneVente l : v.lignes()) {
                    if (l.idArticle().equals(id)) {
                        return true;
                    }
                }
                for (var sv : v.sorties()) {
                    if (sv.idArticle().equals(id)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Supprime définitivement un article <b>sans historique</b> (saisi par erreur) ;
     * sinon, il faut l'archiver.
     */
    public ResultatArticleBuvette supprimer(StockBuvette stock, String id) {
        if (stock.article(id).isPresent() && aUnHistorique(stock, id)) {
            return ResultatArticleBuvette.refus(ResultatArticleBuvette.Motif.A_UN_HISTORIQUE);
        }
        return retirer(stock, id);
    }

    /** Retire un article ; refusé pour un contenant qui sert encore à un produit. */
    public ResultatArticleBuvette retirer(StockBuvette stock, String id) {
        Optional<ArticleBuvette> a = stock.article(id);
        if (a.isEmpty()) {
            return ResultatArticleBuvette.refus(ResultatArticleBuvette.Motif.INCONNU);
        }
        List<ArticleBuvette> utilisateurs = stock.produitsUtilisant(id);
        if (!utilisateurs.isEmpty()) {
            return ResultatArticleBuvette.refus(ResultatArticleBuvette.Motif.CONTENANT_UTILISE, noms(utilisateurs));
        }
        stock.retirer(id);
        return new ResultatArticleBuvette(ResultatArticleBuvette.Motif.RETIRE, null, a.get().getNom());
    }

    private static String noms(List<ArticleBuvette> articles) {
        List<String> n = new ArrayList<>();
        articles.forEach(a -> n.add(a.getNom()));
        return String.join(", ", n);
    }

    private static boolean vide(String s) {
        return s == null || s.isBlank();
    }

    /** Entier positif ou nul (espaces tolérés), {@code defaut} si vide ; vide si illisible. */
    static OptionalLong entier(String saisie, long defaut) {
        if (vide(saisie)) {
            return OptionalLong.of(defaut);
        }
        String t = saisie.replace(" ", "").replace(" ", "").trim();
        if (!t.matches("\\d{1,7}")) {
            return OptionalLong.empty();
        }
        long v = Long.parseLong(t);
        return v > QUANTITE_MAX ? OptionalLong.empty() : OptionalLong.of(v);
    }

    /** Clé de comparaison des noms : sans accents, casse ni ponctuation. */
    static String cle(String nom) {
        return StockBuvette.slug(nom);
    }
}
