package com.pingmanager.ui.ecran;

import com.pingmanager.core.buvette.CatalogueBase;
import com.pingmanager.core.buvette.CommandeComptoir;
import com.pingmanager.core.buvette.ServiceArticlesBuvette;
import com.pingmanager.core.buvette.ServiceVentesBuvette;
import com.pingmanager.model.buvette.ArticleBuvette;
import com.pingmanager.model.buvette.BuvetteTournoi;
import com.pingmanager.model.buvette.ModeBuvette;
import com.pingmanager.model.buvette.MoyenPaiement;
import com.pingmanager.model.buvette.StockBuvette;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Logique d'affichage du catalogue, des articles, du comptoir et du bilan de la buvette (B026). */
class PresentationVentesBuvetteTest {

    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");

    /** Café (1,00 €), thé (1,00 €) et eau (1,00 €) du catalogue, avec du stock. */
    private static StockBuvette stock() {
        StockBuvette s = new StockBuvette("08910012");
        new ServiceArticlesBuvette().ajouterDepuisCatalogue(s, CatalogueBase.catalogue(), Set.of("cafe", "the", "eau"));
        for (ArticleBuvette a : s.getArticles()) {
            s.enregistrer(a.avecStock(100));
        }
        return s;
    }

    @Test
    void catalogue_par_rayon_avec_couts_indicatifs_et_deja_au_club() {
        StockBuvette s = stock();
        PresentationCatalogueBuvette p = new PresentationCatalogueBuvette(CatalogueBase.catalogue(), s);
        List<PresentationCatalogueBuvette.Rayon> rayons = p.rayons();
        assertEquals("Boissons chaudes", rayons.get(0).titre());
        assertEquals("Contenants et consommables", rayons.get(rayons.size() - 1).titre());
        PresentationCatalogueBuvette.Ligne cafe = rayons.get(0).lignes().get(0);
        assertEquals("Café", cafe.nom());
        assertTrue(cafe.dejaAuClub());
        // 12,00 € / 125 = 9,6 c ; gobelet 4,9 c + touillette 0,5 c + sucre 0,9 c = 6,3 c.
        assertEquals("1 dose + gobelet carton (boisson chaude) + touillette en bois + sucre en dosette · "
                + "coût indicatif 0,096 € + emballage 0,063 € = 0,159 € · prix conseillé 1,00 €", cafe.detail());
        PresentationCatalogueBuvette.Ligne chocolat = rayons.get(0).lignes().get(2);
        assertFalse(chocolat.dejaAuClub());
        assertTrue(PresentationCatalogueBuvette.AVERTISSEMENT.contains("à adapter"));
    }

    @Test
    void articles_du_club_par_rayon_coût_prix_marge_et_origine() {
        StockBuvette s = stock();
        PresentationArticlesBuvette p = new PresentationArticlesBuvette(s);
        PresentationArticlesBuvette.Rayon chaudes = p.rayons().get(0);
        assertEquals("Boissons chaudes", chaudes.titre());
        PresentationArticlesBuvette.Ligne cafe = chaudes.lignes().get(0);
        assertEquals("1 dose + gobelet carton (boisson chaude), touillette en bois, sucre en dosette · coût 0,159 € · "
                + "vendu 1,00 € · marge 0,841 €", cafe.detail());
        assertEquals("Du catalogue", cafe.origine());
        assertTrue(cafe.supprimable());
        assertFalse(cafe.prixAFixer());
        assertEquals("Produit brut 0,096 € + emballage 0,063 € = portion complète 0,159 € · vendu 1,00 € · "
                + "marge 0,841 €.", p.resumeCouts(s.article("cafe").orElseThrow()));
        new ServiceArticlesBuvette().archiver(s, "eau");
        assertEquals(1, p.archives().size());
        assertEquals("Bouteille d'eau 50 cl", p.archives().get(0).nom());
    }

    @Test
    void comptoir_grille_par_rayon_commande_et_total() {
        StockBuvette s = stock();
        CommandeComptoir c = new CommandeComptoir();
        PresentationComptoirBuvette p = new PresentationComptoirBuvette(s, c);
        assertEquals(List.of("Boissons chaudes", "Boissons froides"),
                p.rayons().stream().map(PresentationComptoirBuvette.Rayon::titre).toList());
        assertEquals("Valider", p.libelleValider());
        assertEquals("Commande vide", p.resume());
        c.ajouter("cafe");
        c.ajouter("cafe");
        c.ajouter("eau");
        p = new PresentationComptoirBuvette(s, c);
        assertEquals(2, p.rayons().get(0).boutons().get(0).quantite());
        assertEquals("1,00 €", p.rayons().get(0).boutons().get(0).prix());
        assertEquals(List.of(new PresentationComptoirBuvette.LigneCommande("cafe", "Café", 2, "2,00 €"),
                new PresentationComptoirBuvette.LigneCommande("eau", "Bouteille d'eau 50 cl", 1, "1,00 €")), p.lignes());
        assertEquals(300, p.totalCentimes());
        assertEquals("Valider · 3,00 €", p.libelleValider());
        assertEquals("3 articles", p.resume());
    }

    @Test
    void comptoir_signale_prix_a_fixer_epuise_et_a_recompter() {
        StockBuvette s = stock();
        s.enregistrer(s.article("the").orElseThrow().modifier().prixVenteCentimes(0).build());
        s.enregistrer(s.article("eau").orElseThrow().avecStock(0));
        s.marquerARecompter("sucre");
        PresentationComptoirBuvette p = new PresentationComptoirBuvette(s, new CommandeComptoir());
        var chaudes = p.rayons().get(0).boutons();
        assertEquals("À recompter", chaudes.get(0).alerte()); // le café utilise le sucre
        assertFalse(chaudes.get(1).vendable());
        assertEquals("Prix à fixer", chaudes.get(1).prix());
        assertEquals("Épuisé", p.rayons().get(1).boutons().get(0).alerte());
    }

    @Test
    void bilan_periodes_chiffres_details_et_historique() {
        StockBuvette s = stock();
        Instant[] t = {Instant.parse("2026-11-21T09:00:00Z")};
        ServiceVentesBuvette v = new ServiceVentesBuvette(() -> t[0]);
        v.regler(s, "brunoy", "Brunoy", ModeBuvette.STOCK_ET_VENTES);
        CommandeComptoir c = new CommandeComptoir();
        c.ajouter("cafe");
        c.ajouter("cafe");
        v.vendre(s, "brunoy", c, MoyenPaiement.ESPECES, "Claire");
        t[0] = Instant.parse("2026-11-22T13:00:00Z");
        v.vendre(s, "brunoy", c, MoyenPaiement.CARTE, "Claire");
        t[0] = Instant.parse("2026-11-22T13:02:00Z");
        v.annulerDerniere(s, "brunoy", "Paul");

        PresentationBilanBuvette p = new PresentationBilanBuvette(s.tournoi("brunoy").map(BuvetteTournoi::getVentes)
                .orElseThrow(), PARIS);
        assertEquals(List.of("Tout le tournoi", "Samedi 21 novembre", "Dimanche 22 novembre"),
                p.periodes().stream().map(PresentationBilanBuvette.Periode::libelle).toList());
        var b = p.bilan(null);
        assertEquals(List.of(new PresentationBilanBuvette.Chiffre("1", "Vente"),
                new PresentationBilanBuvette.Chiffre("2,00 €", "Recette"),
                new PresentationBilanBuvette.Chiffre("0,32 €", "Coût des articles vendus"),
                new PresentationBilanBuvette.Chiffre("1,68 €", "Marge")), PresentationBilanBuvette.chiffres(b));
        assertEquals("2 articles vendus · panier moyen 2,00 € · 1 vente annulée (non comptées)",
                PresentationBilanBuvette.complement(b));
        assertEquals(List.of(List.of("Café", "2", "2,00 €", "0,32 €", "1,68 €")),
                PresentationBilanBuvette.detailArticles(b));
        assertEquals("Espèces", PresentationBilanBuvette.detailMoyens(b).get(0).get(0));

        List<PresentationBilanBuvette.LigneHistorique> h = p.historique(null);
        assertEquals(2, h.size());
        assertEquals("Vente n° 2 · 22/11 14:00", h.get(0).titre());
        assertEquals("Annulée le 22/11 à 14:02 par Paul", h.get(0).etat());
        assertFalse(h.get(0).annulable());
        assertEquals("2 × Café", h.get(1).articles());
        assertEquals("2,00 € · Espèces · Claire", h.get(1).total());
        assertTrue(h.get(1).annulable());
        assertEquals(1, p.historique(LocalDate.of(2026, 11, 21)).size());
        assertEquals("buvette-bilan-brunoy-2026-2026-11-21.csv",
                PresentationBilanBuvette.nomFichier("buvette-bilan", "brunoy 2026", LocalDate.of(2026, 11, 21)));
    }

    @Test
    void choix_du_tournoi_en_cours_sinon_prochain_sinon_plus_recent() {
        var passe = new ChoixTournoiBuvette.Candidat("t-2025", "Tournoi 2025", LocalDate.of(2025, 11, 22));
        var prochain = new ChoixTournoiBuvette.Candidat("t-2026", "Tournoi 2026", LocalDate.of(2026, 11, 21));
        var lointain = new ChoixTournoiBuvette.Candidat("t-2027", "Tournoi 2027", LocalDate.of(2027, 3, 1));
        LocalDate aujourdhui = LocalDate.of(2026, 11, 17);
        List<ChoixTournoiBuvette.Candidat> tous = List.of(passe, lointain, prochain);
        assertEquals("t-2026", ChoixTournoiBuvette.parDefaut(tous, Set.of(), aujourdhui));
        assertEquals("t-2025", ChoixTournoiBuvette.parDefaut(tous, Set.of("t-2025"), aujourdhui));
        assertEquals("t-2025", ChoixTournoiBuvette.parDefaut(List.of(passe), Set.of(), aujourdhui));
        assertEquals(null, ChoixTournoiBuvette.parDefaut(List.of(), Set.of(), aujourdhui));

        BuvetteTournoi disparu = new BuvetteTournoi("ancien", "Ancien tournoi");
        disparu.ajouter(new com.pingmanager.model.buvette.VenteBuvette(1, "ancien", Instant.EPOCH,
                List.of(new com.pingmanager.model.buvette.LigneVente("cafe", "Café", 1, 100, java.math.BigDecimal.ONE)),
                MoyenPaiement.ESPECES, com.pingmanager.model.buvette.OrigineVente.COMPTOIR, "", "x", List.of(),
                List.of(), null));
        List<ChoixTournoiBuvette.Option> options = ChoixTournoiBuvette.options(tous,
                List.of(disparu, new BuvetteTournoi("sans-vente", "Sans vente")));
        assertEquals(List.of("t-2027", "t-2026", "t-2025", "ancien"),
                options.stream().map(ChoixTournoiBuvette.Option::reference).toList());
        assertEquals("Tournoi 2026 · 21/11/2026", options.get(1).libelle());
        assertEquals("Ancien tournoi · fichier absent", options.get(3).libelle());
    }
}
