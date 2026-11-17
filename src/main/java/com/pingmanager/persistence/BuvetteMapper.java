package com.pingmanager.persistence;

import com.pingmanager.model.buvette.AnnulationVente;
import com.pingmanager.model.buvette.ArticleBuvette;
import com.pingmanager.model.buvette.BuvetteTournoi;
import com.pingmanager.model.buvette.FamilleArticle;
import com.pingmanager.model.buvette.LienContenant;
import com.pingmanager.model.buvette.LigneVente;
import com.pingmanager.model.buvette.ModeBuvette;
import com.pingmanager.model.buvette.MouvementStock;
import com.pingmanager.model.buvette.MoyenPaiement;
import com.pingmanager.model.buvette.NatureMouvement;
import com.pingmanager.model.buvette.OrigineMouvement;
import com.pingmanager.model.buvette.OrigineVente;
import com.pingmanager.model.buvette.RayonBuvette;
import com.pingmanager.model.buvette.SortieVente;
import com.pingmanager.model.buvette.StockBuvette;
import com.pingmanager.model.buvette.VenteBuvette;
import com.pingmanager.persistence.json.Json;

import java.math.BigDecimal;
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
 * <p>Depuis le catalogue et les ventes (B026, suite), chaque article peut porter
 * {@code rayon}, {@code prixVente} (centimes), {@code archive} et
 * {@code catalogue} ; le stock porte {@code aRecompter} (identifiants) et
 * {@code tournois} : un objet par tournoi avec {@code ref} (nom du fichier du
 * tournoi), {@code nom}, {@code mode} ({@code STOCK_SEUL} / {@code STOCK_ET_VENTES})
 * et {@code ventes} (numéro, instant, lignes au prix et au coût figés — coût en
 * centimes exacts, texte décimal —, moyen de paiement, origine, sorties de
 * stock, annulation éventuelle). Absents des fichiers plus anciens : catalogue
 * vide, mode « stock seulement », aucune vente.</p>
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
        if (!s.getARecompter().isEmpty()) {
            Json rc = Json.tableau();
            s.getARecompter().forEach(id -> rc.add(Json.of(id)));
            o.set("aRecompter", rc);
        }
        Json tournois = Json.tableau();
        for (BuvetteTournoi t : s.getTournois()) {
            if (!t.estVide()) {
                tournois.add(ecrireTournoi(t));
            }
        }
        if (tournois.taille() > 0) {
            o.set("tournois", tournois);
        }
        return o;
    }

    static Json ecrireTournoi(BuvetteTournoi t) {
        Json o = Json.objet();
        o.set("ref", t.getReferenceTournoi());
        o.set("nom", t.getNomTournoi());
        o.set("mode", t.getMode().name());
        Json ventes = Json.tableau();
        for (VenteBuvette v : t.getVentes()) {
            ventes.add(ecrireVente(v));
        }
        o.set("ventes", ventes);
        return o;
    }

    static Json ecrireVente(VenteBuvette v) {
        Json o = Json.objet();
        o.set("n", v.numero());
        o.set("t", v.horodatage().toString());
        o.set("moyen", v.moyen().name());
        o.set("origine", v.origine().name());
        o.set("refOrigine", v.referenceOrigine());
        o.set("auteur", v.auteur());
        Json lignes = Json.tableau();
        for (LigneVente l : v.lignes()) {
            lignes.add(Json.objet().set("article", l.idArticle()).set("nom", l.nom()).set("q", l.quantite())
                    .set("prix", l.prixUnitaireCentimes()).set("cout", l.coutUnitaire().toPlainString()));
        }
        o.set("lignes", lignes);
        Json sorties = Json.tableau();
        for (SortieVente sv : v.sorties()) {
            sorties.add(Json.objet().set("article", sv.idArticle()).set("q", sv.quantite()));
        }
        o.set("sorties", sorties);
        if (!v.aRecompter().isEmpty()) {
            Json rc = Json.tableau();
            v.aRecompter().forEach(n -> rc.add(Json.of(n)));
            o.set("aRecompter", rc);
        }
        if (v.estAnnulee()) {
            o.set("annulation", Json.objet().set("t", v.annulation().horodatage().toString())
                    .set("auteur", v.annulation().auteur()));
        }
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
        j.set("rayon", a.getRayon().name());
        if (a.getPrixVenteCentimes() > 0) {
            j.set("prixVente", a.getPrixVenteCentimes());
        }
        if (a.estArchive()) {
            j.set("archive", true);
        }
        if (!a.getIdCatalogue().isEmpty()) {
            j.set("catalogue", a.getIdCatalogue());
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
        for (Json j : tableau(o, "aRecompter")) {
            try {
                s.marquerARecompter(j.asString());
            } catch (RuntimeException illisible) {
                // valeur abîmée : écartée
            }
        }
        for (Json j : tableau(o, "tournois")) {
            try {
                s.enregistrer(lireTournoi(j));
            } catch (RuntimeException illisible) {
                // tournoi abîmé : écarté
            }
        }
        return s;
    }

    static BuvetteTournoi lireTournoi(Json o) {
        BuvetteTournoi t = new BuvetteTournoi(o.get("ref").asString(), o.getString("nom", null));
        t.setMode(ModeBuvette.lire(o.getString("mode", null)));
        for (Json j : tableau(o, "ventes")) {
            try {
                VenteBuvette v = lireVente(j, t.getReferenceTournoi());
                if (v != null && t.vente(v.numero()).isEmpty()) {
                    t.ajouter(v);
                }
            } catch (RuntimeException illisible) {
                // vente abîmée : écartée, les autres sont relues
            }
        }
        return t;
    }

    /** @return la vente, ou {@code null} si son origine vient d'une version plus récente. */
    static VenteBuvette lireVente(Json j, String referenceTournoi) {
        OrigineVente origine = OrigineVente.lire(j.getString("origine", OrigineVente.COMPTOIR.name()));
        if (origine == null) {
            return null;
        }
        List<LigneVente> lignes = new ArrayList<>();
        for (Json l : tableau(j, "lignes")) {
            lignes.add(new LigneVente(l.get("article").asString(), l.get("nom").asString(), l.get("q").asInt(),
                    l.get("prix").asLong(), new BigDecimal(l.getString("cout", "0"))));
        }
        List<SortieVente> sorties = new ArrayList<>();
        for (Json l : tableau(j, "sorties")) {
            sorties.add(new SortieVente(l.get("article").asString(), l.get("q").asLong()));
        }
        List<String> aRecompter = new ArrayList<>();
        for (Json l : tableau(j, "aRecompter")) {
            aRecompter.add(l.asString());
        }
        AnnulationVente annulation = null;
        if (j.has("annulation") && j.get("annulation").estObjet()) {
            Json a = j.get("annulation");
            annulation = new AnnulationVente(Instant.parse(a.get("t").asString()), a.getString("auteur", null));
        }
        return new VenteBuvette(j.get("n").asInt(), referenceTournoi, Instant.parse(j.get("t").asString()), lignes,
                MoyenPaiement.lire(j.getString("moyen", null)), origine, j.getString("refOrigine", ""),
                j.getString("auteur", null), sorties, aRecompter, annulation);
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
                .rayon(RayonBuvette.lire(j.getString("rayon", null)))
                .prixVenteCentimes(Math.max(0, j.has("prixVente") ? j.get("prixVente").asLong() : 0))
                .archive(j.getBoolean("archive", false))
                .idCatalogue(j.getString("catalogue", null))
                .build();
    }

    static List<Json> tableau(Json o, String cle) {
        if (!o.has(cle) || !o.get(cle).estTableau()) {
            return List.of();
        }
        return new ArrayList<>(o.get(cle).elements());
    }
}
