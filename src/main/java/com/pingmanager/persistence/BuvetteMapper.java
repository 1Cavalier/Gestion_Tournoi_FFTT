package com.pingmanager.persistence;

import com.pingmanager.model.buvette.ArticleBuvette;
import com.pingmanager.model.buvette.FamilleArticle;
import com.pingmanager.model.buvette.LienContenant;
import com.pingmanager.model.buvette.MouvementStock;
import com.pingmanager.model.buvette.NatureMouvement;
import com.pingmanager.model.buvette.OrigineMouvement;
import com.pingmanager.model.buvette.StockBuvette;
import com.pingmanager.persistence.json.Json;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Stock de la buvette d'un club (B026) en JSON, dans le fichier d'application
 * (section facultative {@code stocksBuvette}) :
 *
 * <pre>
 * "stocksBuvette": [ {"club": "08910012",
 *   "articles": [ {"id": "cafe", "nom": "Café", "famille": "PRODUIT", "unite": "dose",
 *                  "prixLot": 800, "unitesParLot": 125, "stock": 200, "seuil": 40,
 *                  "contenants": [ {"contenant": "gobelet", "quantite": 1} ]} ],
 *   "journal": [ {"op": 1, "t": "2026-11-16T08:00:00Z", "article": "cafe", "nature": "SORTIE",
 *                 "variation": -1, "stockApres": 199, "origine": "SAISIE_MANUELLE",
 *                 "reference": "", "auteur": "Q. S."} ] } ]
 * </pre>
 *
 * <p>Montants en centimes entiers. Relecture tolérante : une ligne illisible
 * (fichier abîmé ou d'une version plus récente) est écartée, le reste est relu.
 * L'ancienne section {@code buvettes} (catalogue B046) n'est pas lue ici : le
 * dépôt la recopie telle quelle (voir {@link DepotApplication}).</p>
 */
final class BuvetteMapper {

    private BuvetteMapper() {
    }

    static Json ecrireStock(StockBuvette s) {
        Json o = Json.objet();
        o.set("club", s.getNumeroClub());
        Json articles = Json.tableau();
        for (ArticleBuvette a : s.getArticles()) {
            articles.add(ecrireArticle(a));
        }
        o.set("articles", articles);
        Json journal = Json.tableau();
        for (MouvementStock m : s.getJournal()) {
            journal.add(Json.objet()
                    .set("op", m.operation())
                    .set("t", m.horodatage().toString())
                    .set("article", m.idArticle())
                    .set("nature", m.nature().name())
                    .set("variation", m.variation())
                    .set("stockApres", m.stockApres())
                    .set("origine", m.origine().name())
                    .set("reference", m.reference())
                    .set("auteur", m.auteur()));
        }
        o.set("journal", journal);
        return o;
    }

    static Json ecrireArticle(ArticleBuvette a) {
        Json j = Json.objet();
        j.set("id", a.getId());
        j.set("nom", a.getNom());
        j.set("famille", a.getFamille().name());
        j.set("unite", a.getUnite());
        j.set("prixLot", a.getPrixLotCentimes());
        j.set("unitesParLot", a.getUnitesParLot());
        j.set("stock", a.getStock());
        j.set("seuil", a.getSeuilAlerte());
        if (!a.getContenants().isEmpty()) {
            Json c = Json.tableau();
            for (LienContenant l : a.getContenants()) {
                c.add(Json.objet().set("contenant", l.idContenant()).set("quantite", l.quantite()));
            }
            j.set("contenants", c);
        }
        return j;
    }

    static StockBuvette lireStock(Json o) {
        StockBuvette s = new StockBuvette(o.get("club").asString());
        for (Json j : tableau(o, "articles")) {
            try {
                s.enregistrer(lireArticle(j));
            } catch (RuntimeException illisible) {
                // ligne abîmée : écartée
            }
        }
        for (Json j : tableau(o, "journal")) {
            try {
                NatureMouvement nature = NatureMouvement.lire(j.getString("nature", ""));
                OrigineMouvement origine = OrigineMouvement.lire(j.getString("origine", ""));
                if (nature == null || origine == null) {
                    continue; // valeur d'une version plus récente : écartée
                }
                s.journaliser(new MouvementStock(j.get("op").asInt(), Instant.parse(j.getString("t", "")),
                        j.get("article").asString(), nature, j.get("variation").asLong(),
                        j.get("stockApres").asLong(), origine, j.getString("reference", ""),
                        j.getString("auteur", null)));
            } catch (RuntimeException illisible) {
                // ligne abîmée : écartée, le reste du journal est relu
            }
        }
        return s;
    }

    static ArticleBuvette lireArticle(Json j) {
        List<LienContenant> liens = new ArrayList<>();
        for (Json l : tableau(j, "contenants")) {
            try {
                liens.add(new LienContenant(l.get("contenant").asString(), l.getInt("quantite", 1)));
            } catch (RuntimeException illisible) {
                // lien abîmé : écarté
            }
        }
        return ArticleBuvette.builder()
                .id(j.get("id").asString())
                .nom(j.get("nom").asString())
                .famille(FamilleArticle.lire(j.getString("famille", null)))
                .unite(j.getString("unite", null))
                .lot(Math.max(0, j.has("prixLot") ? j.get("prixLot").asLong() : 0),
                        Math.max(1, j.getInt("unitesParLot", 1)))
                .stock(Math.max(0, j.has("stock") ? j.get("stock").asLong() : 0))
                .seuilAlerte(Math.max(0, j.has("seuil") ? j.get("seuil").asLong() : 0))
                .contenants(liens)
                .build();
    }

    static List<Json> tableau(Json o, String cle) {
        if (!o.has(cle) || !o.get(cle).estTableau()) {
            return List.of();
        }
        return new ArrayList<>(o.get(cle).elements());
    }
}
