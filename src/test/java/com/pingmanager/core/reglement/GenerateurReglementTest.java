package com.pingmanager.core.reglement;

import com.pingmanager.model.DocumentReglement;

import com.pingmanager.model.AdresseTournoi;
import com.pingmanager.model.AffectationArbitre;
import com.pingmanager.model.AffectationJugeArbitre;
import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.CouleurBalles;
import com.pingmanager.model.GradeJugeArbitre;
import com.pingmanager.model.GrilleRecompenses;
import com.pingmanager.model.HorairesTableau;
import com.pingmanager.model.InfosReglement;
import com.pingmanager.model.LotTables;
import com.pingmanager.model.NiveauTournoi;
import com.pingmanager.model.ArticleOptionnel;
import com.pingmanager.model.ConditionsMaintien;
import com.pingmanager.model.OptionsReglement;
import com.pingmanager.model.Paiement;
import com.pingmanager.model.PhasePoints;
import com.pingmanager.model.PolitiqueForfaits;
import com.pingmanager.model.QuotasTableaux;
import com.pingmanager.model.PolitiqueBalles;
import com.pingmanager.model.Recompense;
import com.pingmanager.model.RoleJugeArbitre;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.TarifInscription;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.format.RegistreFormats;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GenerateurReglementTest {

    private final GenerateurReglement generateur = new GenerateurReglement();

    /** Tournoi national B complet, proche de l'exemple de Brunoy (17-18 avril 2027). */
    static Tournoi tournoiComplet() {
        LocalDate sam = LocalDate.of(2027, 4, 17);
        LocalDate dim = LocalDate.of(2027, 4, 18);
        Tournoi t = new Tournoi("Tournoi National B de Brunoy", "Brunoy", List.of(sam, dim));
        t.setNumeroAffiliationClub("08911132");
        t.setNiveau(NiveauTournoi.NATIONAL_B);
        t.setPeriode(sam, dim);
        t.setAdresse(AdresseTournoi.de("1 rue du Gymnase", "91800", "Brunoy"));
        t.setCorrespondant("DUPONT", "Alexis", "organisateur.demo@example.org", null, null);
        t.setNumeroHomologation("2027-N-042");
        t.setSiteWeb("https://exemple.fr/inscription");
        t.ajouterJugeArbitre(new AffectationJugeArbitre("LEMOINE", "Hélène", null, "9411021",
                GradeJugeArbitre.JA3, null, RoleJugeArbitre.PRINCIPAL));
        t.ajouterJugeArbitre(new AffectationJugeArbitre("ROUX", "Sophie", null, "9411034",
                GradeJugeArbitre.JA2, null, RoleJugeArbitre.ADJOINT));
        t.setLotsTables(List.of(new LotTables(24, "Cornilleau", "Bleue")));
        t.setInfosReglement(InfosReglement.builder()
                .ouvertureInscriptions(LocalDateTime.of(2027, 2, 1, 0, 0))
                .clotureInscriptions(LocalDateTime.of(2027, 4, 15, 20, 0))
                .politiqueBalles(PolitiqueBalles.FOURNIES_PAR_ORGANISATEUR)
                .couleurBalles(CouleurBalles.BLANCHE)
                .build());
        t.ajouterTableau(tableau("A", ConfigurationTableau.depuisFormat(RegistreFormats.formatParDefaut())
                .pointMax(600).placesMax(60).tailleMaxListeAttente(10)
                .tarif(new TarifInscription(9, 10))
                .recompenses(GrilleRecompenses.builder()
                        .place(1, Recompense.argent(60)).place(2, Recompense.argent(30))
                        .plage(3, 4, Recompense.argent(15)).build()), sam, 8, 30));
        t.ajouterTableau(tableau("Dames", ConfigurationTableau.depuisFormat(RegistreFormats.formatParDefaut())
                .toutesSeries().sexeRequis(Sexe.FEMME).placesMax(48)
                .tarif(new TarifInscription(9, 10))
                .recompenses(GrilleRecompenses.builder()
                        .place(1, Recompense.argent(100)).place(2, Recompense.autre("Coupe")).build()), dim, 15, 0));
        t.setPhasePoints(PhasePoints.DEUXIEME);
        t.setQuotasTableaux(QuotasTableaux.identiques(2, 3));
        t.setConditionsMaintien(new ConditionsMaintien(12, LocalDate.of(2027, 4, 15),
                ConditionsMaintien.ModeReduction.REMPLISSAGE, 50, 0, 50));
        t.setPolitiqueForfaits(new PolitiqueForfaits(Map.of(
                PolitiqueForfaits.Periode.PENDANT_INSCRIPTIONS,
                new PolitiqueForfaits.Regle(PolitiqueForfaits.Remboursement.COMPLET, 0, false),
                PolitiqueForfaits.Periode.APRES_CLOTURE,
                new PolitiqueForfaits.Regle(PolitiqueForfaits.Remboursement.PARTIEL, 50, true),
                PolitiqueForfaits.Periode.JOUR_J,
                new PolitiqueForfaits.Regle(PolitiqueForfaits.Remboursement.AUCUN, 0, false)), true));
        t.setPaiement(new Paiement(Paiement.Mode.HELLOASSO,
                "https://www.helloasso.com/associations/brunoy-tt/evenements/tournoi-national-b"));
        t.setOptionsReglement(new OptionsReglement(OptionsReglement.Mode.GENERE, List.of(
                ArticleOptionnel.de(ArticleOptionnel.Type.DROIT_IMAGE),
                new ArticleOptionnel(ArticleOptionnel.Type.BUVETTE, Map.of(ArticleOptionnel.HORAIRES, "de 8 h à 20 h")))));
        return t;
    }

    private static Tableau tableau(String nom, ConfigurationTableau.Builder b, LocalDate jour, int h, int m) {
        Tableau tab = new Tableau(nom, b.build());
        tab.setType(LibellesTableau.type(tab.getConfiguration()));
        LocalDateTime debut = jour.atTime(h, m);
        tab.setHoraires(new HorairesTableau(debut.minusMinutes(60), debut.minusMinutes(30), debut));
        return tab;
    }

    private static String texte(DocumentReglement d) {
        StringBuilder sb = new StringBuilder(d.titre()).append('\n').append(d.sousTitre()).append('\n');
        for (DocumentReglement.Article a : d.articles()) {
            sb.append(a.numero()).append(' ').append(a.titre()).append('\n');
            a.blocs().forEach(b -> sb.append(b.texteBrut()).append('\n'));
        }
        return sb.toString();
    }

    private DocumentReglement.Article article(DocumentReglement d, String titre) {
        return d.articles().stream().filter(a -> a.titre().equals(titre)).findFirst().orElseThrow();
    }

    @Test
    void tournoi_complet_sans_trou_ni_check_list() {
        DocumentReglement d = generateur.generer(tournoiComplet());
        String txt = texte(d);
        assertFalse(d.estProjet(), () -> "trous inattendus :\n" + txt);
        assertTrue(d.aCompleter().isEmpty(), () -> "check-list inattendue : " + d.aCompleter());
        assertEquals("Règlement — Tournoi National B de Brunoy", d.titre());
        assertEquals("Samedi 17 et dimanche 18 avril 2027, Brunoy", d.sousTitre());
    }

    @Test
    void le_texte_reprend_les_donnees_saisies() {
        String txt = texte(generateur.generer(tournoiComplet()));
        assertTrue(txt.contains("les samedi 17 et dimanche 18 avril 2027 un tournoi national B"), txt);
        assertTrue(txt.contains("homologué par la FFTT sous le numéro 2027-N-042"));
        assertTrue(txt.contains("saison 2026-2027"));
        assertTrue(txt.contains("de la 2e phase 2026-2027"), txt);
        assertTrue(txt.contains("Chaque joueur et chaque joueuse peut s'inscrire à 2 tableaux maximum par jour. "
                + "Les joueuses peuvent s'inscrire à un troisième tableau de leur choix."), txt);
        assertTrue(txt.contains("poules de 3 joueurs") && txt.contains("meilleur des 5 manches de 11 points"));
        assertTrue(txt.contains("Hélène LEMOINE, juge-arbitre de grade JA3, assisté de Sophie ROUX"));
        assertTrue(txt.contains("24 tables (Cornilleau bleue)"));
        assertTrue(txt.contains("balles, en plastique blanches, agréées, sont fournies par le club"));
        assertTrue(txt.contains("9 € en ligne et 10 € sur place par tableau"));
        assertTrue(txt.contains("une liste d'attente est ouverte"), "le tableau A a une liste d'attente");
        assertTrue(txt.contains("Des inscriptions sur place restent possibles"));
        assertTrue(txt.contains("moins de 12 inscrits le jeudi 15 avril 2027") && txt.contains("moins de 50 %"), txt);
        assertTrue(txt.contains("(jeudi 15 avril 2027 à 20h) est remboursé intégralement"), txt);
        assertTrue(txt.contains("est remboursé à hauteur de 50 %, sur présentation d'un justificatif"), txt);
        assertTrue(txt.contains("absence au pointage, n'est pas remboursé."), txt);
        assertTrue(txt.contains("frais de paiement en ligne ne sont pas remboursés"));
        assertTrue(txt.contains("via HelloAsso : https://www.helloasso.com/"), txt);
        assertTrue(txt.contains("Une buvette est ouverte dans la salle pendant toute la durée du tournoi, de 8 h à 20 h."), txt);
        assertTrue(txt.contains("chaque perdant arbitre"), "pas d'arbitre officiel");
        assertTrue(txt.contains("droit à l'image") || txt.contains("photographié"));
    }

    @Test
    void dotation_et_grille_des_recompenses() {
        DocumentReglement d = generateur.generer(tournoiComplet());
        // A : 60 + 30 + 2 × 15 = 120 € ; Dames : 100 € + coupe → 220 € au total.
        assertTrue(texte(d).contains("doté de 220 € en espèces, ainsi que de lots"), texte(d));
        DocumentReglement.Grille g = (DocumentReglement.Grille) article(d, "Dotations").blocs().stream()
                .filter(b -> b instanceof DocumentReglement.Grille).findFirst().orElseThrow();
        assertEquals(List.of("Tableau", "Points", "Vainqueur", "Finaliste", "½ finalistes (chacun)", "Total"), g.entetes());
        assertEquals(List.of("A", "NC à 600", "60 €", "30 €", "15 €", "120 €"), g.lignes().get(0));
        assertEquals(List.of("Dames", "Toutes séries", "100 €", "Coupe", "—", "100 € + lots"), g.lignes().get(1));
    }

    @Test
    void tableaux_tries_par_jour_et_heure_avec_fin_de_pointage() {
        DocumentReglement d = generateur.generer(tournoiComplet());
        DocumentReglement.Grille g = (DocumentReglement.Grille) article(d, "Tableaux").blocs().get(1);
        assertEquals(List.of("Tableau", "Jour", "Fin du pointage", "Début", "Points", "Joueurs admis", "Places",
                "Tarif d'inscription*"), g.entetes());
        assertEquals(List.of("A", "Samedi 17 avril", "8h", "8h30", "NC à 600", "Tous", "60", "9 €"), g.lignes().get(0));
        assertEquals(List.of("Dames", "Dimanche 18 avril", "14h30", "15h", "Toutes séries", "Dames", "48", "9 €"),
                g.lignes().get(1));
        assertTrue(g.note().startsWith("* Tarif de l'inscription en ligne ; voir les modalités à l'article 8 « Droits "
                + "d'engagement »."), g.note());
    }

    @Test
    void le_texte_s_adapte_balles_des_joueurs_arbitres_et_pas_de_droit_a_l_image() {
        Tournoi t = tournoiComplet();
        t.setInfosReglement(t.getInfosReglement().toBuilder()
                .politiqueBalles(PolitiqueBalles.APPORTEES_PAR_JOUEURS)
                .couleurBalles(CouleurBalles.ORANGE)
                .ballesRefusees(List.of("Balles en celluloïd")).build());
        t.ajouterArbitre(new AffectationArbitre("9411056", "PETIT", "Antoine", "AR"));
        t.setOptionsReglement(t.getOptionsReglement().avecArticles(List.of()));
        String txt = texte(generateur.generer(t));
        assertTrue(txt.contains("Les joueurs fournissent leurs balles : balles en plastique orange"));
        assertTrue(txt.contains("Sont refusées : balles en celluloïd."));
        assertTrue(txt.contains("Un arbitre officiel officie"));
        assertFalse(txt.contains("photographié"));
    }

    @Test
    void tournoi_vide_trous_surlignables_et_check_list_avant_depot() {
        Tournoi t = new Tournoi("Open d'automne", "Brunoy", LocalDate.of(2026, 11, 21));
        DocumentReglement d = generateur.generer(t);
        assertTrue(d.estProjet());
        String txt = texte(d);
        assertTrue(txt.contains("[à compléter : catégorie du tournoi]"));
        assertTrue(txt.contains("[à compléter : juge-arbitre désigné"));
        assertTrue(txt.contains("[à compléter : tableaux du tournoi]"));
        assertTrue(d.aCompleter().contains("Compléter : numéro d'homologation, délivré après la demande"));
        assertTrue(d.aCompleter().stream().anyMatch(x -> x.startsWith("Dossier d'homologation : ")));
        assertTrue(d.aCompleter().stream().anyMatch(x -> x.contains("IX.106")));
        assertTrue(DocumentReglement.A_COMPLETER.matcher("x [à compléter : y] z").find());
    }

    @Test
    void echeance_ix104_deux_mois_avant_pour_un_tournoi_federal_incomplet() {
        Tournoi t = tournoiComplet();
        t.setNumeroHomologation(null);
        DocumentReglement d = generateur.generer(t);
        assertTrue(d.aCompleter().stream().anyMatch(x -> x.contains("au plus tard le mercredi 17 février 2027")),
                d.aCompleter().toString());
    }

    @Test
    void quotas_differents_selon_le_jour() {
        Tournoi t = tournoiComplet();
        t.setQuotasTableaux(QuotasTableaux.parJour(Map.of(
                LocalDate.of(2027, 4, 17), new QuotasTableaux.Quota(2, 2),
                LocalDate.of(2027, 4, 18), new QuotasTableaux.Quota(1, null))));
        String txt = texte(generateur.generer(t));
        assertTrue(txt.contains("varie selon le jour"), txt);
        assertTrue(txt.contains("Samedi 17 avril : chaque joueur et chaque joueuse peut s'inscrire à 2 tableaux par jour."), txt);
        assertTrue(txt.contains("Dimanche 18 avril : chaque joueur peut s'inscrire à 1 tableau par jour, les joueuses sans limite."), txt);
    }

    @Test
    void sans_phase_ni_forfaits_ni_paiement_des_trous_apparaissent() {
        Tournoi t = tournoiComplet();
        t.setPhasePoints(null);
        t.setPolitiqueForfaits(PolitiqueForfaits.nonDefinie());
        t.setPaiement(Paiement.aucun());
        String txt = texte(generateur.generer(t));
        assertTrue(txt.contains("[à compléter : phase de référence des points"), txt);
        assertTrue(txt.contains("[à compléter : remboursement en cas de forfait, le jour du tournoi]"), txt);
        assertTrue(txt.contains("[à compléter : lien de paiement"), txt);
    }

    @Test
    void articles_libres_et_infos_pratiques() {
        Tournoi t = tournoiComplet();
        t.setOptionsReglement(t.getOptionsReglement().avecArticles(List.of(
                new ArticleOptionnel(ArticleOptionnel.Type.INFOS_PRATIQUES,
                        Map.of(ArticleOptionnel.PARKING, "oui", ArticleOptionnel.PMR, "oui")),
                new ArticleOptionnel(ArticleOptionnel.Type.LIBRE,
                        Map.of(ArticleOptionnel.TITRE, "Tenue", ArticleOptionnel.TEXTE, "Short obligatoire.")))));
        DocumentReglement d = generateur.generer(t);
        String txt = texte(d);
        assertTrue(txt.contains("La salle dispose d'un parking et un accès pour les personnes à mobilité réduite."), txt);
        assertEquals("Short obligatoire.", article(d, "Tenue").blocs().get(0).texteBrut());
        // Les articles ajoutés précèdent « Acceptation ».
        List<String> titres = d.articles().stream().map(DocumentReglement.Article::titre).toList();
        assertTrue(titres.indexOf("Tenue") < titres.indexOf("Acceptation"), titres.toString());
    }

    @Test
    void rendu_pdf_valide_avec_trous_et_check_list() throws Exception {
        Tournoi t = tournoiComplet();
        t.setNumeroHomologation(null);
        byte[] pdf = RenduPdfReglement.rendre(generateur.generer(t));
        assertTrue(pdf.length > 10_000);
        assertEquals("%PDF", new String(pdf, 0, 4, java.nio.charset.StandardCharsets.US_ASCII));
        try (org.apache.pdfbox.pdmodel.PDDocument doc = org.apache.pdfbox.pdmodel.PDDocument.load(pdf)) {
            assertTrue(doc.getNumberOfPages() >= 2);
            String texte = new org.apache.pdfbox.text.PDFTextStripper().getText(doc);
            assertTrue(texte.contains("Article 1 — Organisation"), texte);
            assertTrue(texte.contains("à compléter : numéro d'homologation"), texte);
            assertFalse(texte.contains("À faire avant le dépôt"), "check-list retirée du PDF");
            assertFalse(texte.contains("Projet de règlement"), "pas de bandeau explicatif");
            assertTrue(texte.contains("page 1 / " + doc.getNumberOfPages()));
            assertTrue(texte.contains("½ finalistes"));
        }
    }
}
