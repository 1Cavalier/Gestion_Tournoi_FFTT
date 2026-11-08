package com.pingmanager.importation;

import com.pingmanager.core.ResultatsTournoi;
import com.pingmanager.core.ServiceResultatsFederaux;
import com.pingmanager.core.TournoiTermineDeTest;
import com.pingmanager.core.FichierExporte;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** B023 (vérification) : CSV hostiles, limites et aller-retour d'export. */
class CsvHostileTest {

    private static final String E = "Licence;Nom;Prénom;Sexe;N° club;Points\r\n";

    private static byte[] b(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void guillemet_non_ferme_ne_plante_pas_et_n_avale_pas_silencieusement_tout_le_fichier() throws Exception {
        ResultatImportLicences r = LecteurLicencesCsv.lire("l.csv", b(E
                + "1;\"DUPONT;Marie;F;08910001;100\r\n2;MARTIN;Luc;M;08910001;200\r\n3;LEROY;Ann;F;08910001;300\r\n"));
        // attendu : au moins une anomalie signalée (la ligne 2 est corrompue), pas d'exception
        assertTrue(r.fiches().size() + r.anomalies().size() >= 1);
        assertTrue(r.fiches().size() < 3 ? !r.anomalies().isEmpty() : true,
                "des lignes disparaissent sans anomalie signalée");
    }

    @Test
    void plus_de_20000_lignes_le_depassement_est_signale() throws Exception {
        StringBuilder sb = new StringBuilder(E);
        for (int i = 0; i < CsvFederal.LIGNES_MAX + 5; i++) {
            sb.append("L").append(i).append(";N;P;M;08910001;100\r\n");
        }
        ResultatImportLicences r = LecteurLicencesCsv.lire("l.csv", b(sb.toString()));
        assertEquals(CsvFederal.LIGNES_MAX, r.fiches().size());
        assertFalse(r.anomalies().isEmpty(), "lignes au-delà de la limite ignorées sans aucun signalement");
    }

    @Test
    void doublon_signale_avec_numero_de_ligne_et_premiere_occurrence_conservee() throws Exception {
        ResultatImportLicences r = LecteurLicencesCsv.lire("l.csv", b(E
                + "1;A;B;M;08910001;100\r\n1;C;D;M;08910001;999\r\n"));
        assertEquals(1, r.fiches().size());
        assertEquals("A", r.fiches().get(0).getNom());
        assertEquals(3, r.anomalies().get(0).ligne());
    }

    @Test
    void colonnes_en_trop_et_lignes_courtes_sont_tolerees_ou_refusees_proprement() throws Exception {
        ResultatImportLicences r = LecteurLicencesCsv.lire("l.csv", b(E
                + "1;A;B;M;08910001;100;x;y;z\r\n2;A;B\r\n"));
        assertEquals(1, r.fiches().size());
        assertEquals(1, r.lignesRefusees());
    }

    @Test
    void points_negatifs_ou_enormes_refuses() throws Exception {
        ResultatImportLicences r = LecteurLicencesCsv.lire("l.csv", b(E
                + "1;A;B;M;08910001;-5\r\n2;A;B;M;08910001;99999999999\r\n3;A;B;M;08910001;0\r\n"));
        assertEquals(1, r.fiches().size());
        assertEquals("3", r.fiches().get(0).getNumeroLicence());
    }

    @Test
    void entete_seul_donne_zero_fiche() throws Exception {
        assertEquals(0, LecteurLicencesCsv.lire("l.csv", b(E)).fiches().size());
        assertEquals(0, LecteurClubsCsv.lire("c.csv", b("N° club;Nom\r\n")).clubs().size());
    }

    @Test
    void cellule_de_201_caracteres_refusee_200_acceptee() throws Exception {
        String n200 = "N".repeat(200);
        String n201 = "N".repeat(201);
        ResultatImportLicences r = LecteurLicencesCsv.lire("l.csv", b(E
                + "1;" + n200 + ";B;M;08910001;100\r\n2;" + n201 + ";B;M;08910001;100\r\n"));
        assertEquals(1, r.fiches().size());
        assertEquals(1, r.lignesRefusees());
    }

    @Test
    void ligne_enorme_sur_une_seule_ligne_ne_plante_pas() throws Exception {
        String enorme = "X".repeat(5_000_000);
        ResultatImportLicences r = LecteurLicencesCsv.lire("l.csv", b(E + "1;" + enorme + ";B;M;08910001;100\r\n"));
        assertEquals(0, r.fiches().size());
        assertEquals(1, r.lignesRefusees());
    }

    @Test
    void nom_avec_point_virgule_guillemets_et_saut_de_ligne_relu_a_l_identique() throws Exception {
        ResultatImportLicences r = LecteurLicencesCsv.lire("l.csv", b(E
                + "1;\"DU;PONT\";\"Ma \"\"Mie\"\"\";F;08910001;100\r\n"));
        assertEquals("DU;PONT", r.fiches().get(0).getNom());
        assertEquals("Ma \"Mie\"", r.fiches().get(0).getPrenom());
    }

    @Test
    void cellule_cellule_neutralise_formules_et_cas_limites() {
        assertEquals("'=1+1", CsvFederal.cellule("=1+1"));
        assertEquals("'+1", CsvFederal.cellule("+1"));
        assertEquals("'-1", CsvFederal.cellule("-1"));
        assertEquals("'@SUM(A1)", CsvFederal.cellule("@SUM(A1)"));
        assertEquals("'\tx", CsvFederal.cellule("\tx"));
        assertEquals("", CsvFederal.cellule(null));
        assertEquals("\"a;b\"", CsvFederal.cellule("a;b"));
        assertEquals("\"'=a;b\"", CsvFederal.cellule("=a;b"));
        assertEquals("\"a\"\"b\"", CsvFederal.cellule("a\"b"));
    }

    @Test
    void aller_retour_export_puis_relecture_conserve_lignes_et_colonnes() throws Exception {
        TournoiTermineDeTest jeu = TournoiTermineDeTest.creer("H-1", "DU;PONT \"X\"");
        ResultatsTournoi res = new ServiceResultatsFederaux().construire(jeu.tournoi, jeu.termine).getResultats().orElseThrow();
        List<FichierExporte> f = new ExportResultatsCsv().produire(res);
        CsvFederal.Table t = CsvFederal.lire("parties.csv", f.get(0).getContenu(), ExportResultatsCsv.ENTETES_PARTIES);
        assertEquals(res.parties().size(), t.lignes.size());
        for (int i = 0; i < t.lignes.size(); i++) {
            assertEquals(res.parties().get(i).getJoueurA().nom(), t.valeur(t.lignes.get(i), "Nom A"));
            assertEquals(res.parties().get(i).getJoueurB().nom(), t.valeur(t.lignes.get(i), "Nom B"));
            assertEquals(String.valueOf(res.parties().get(i).manchesA()), t.valeur(t.lignes.get(i), "Manches A"));
        }
        CsvFederal.Table c = CsvFederal.lire("classements.csv", f.get(1).getContenu(), ExportResultatsCsv.ENTETES_CLASSEMENTS);
        assertEquals(res.classements().size(), c.lignes.size());
    }

    @Test
    void export_d_un_nom_de_formule_est_neutralise_dans_les_deux_fichiers() throws Exception {
        TournoiTermineDeTest jeu = TournoiTermineDeTest.creer("=HYPERLINK(\"x\")", "=cmd|' /C calc'!A0");
        ResultatsTournoi res = new ServiceResultatsFederaux().construire(jeu.tournoi, jeu.termine).getResultats().orElseThrow();
        for (FichierExporte f : new ExportResultatsCsv().produire(res)) {
            String txt = new String(f.getContenu(), StandardCharsets.UTF_8);
            for (String ligne : txt.split("\r\n")) {
                for (String cell : ligne.split(";")) {
                    assertFalse(cell.startsWith("=") || cell.startsWith("\"="), "formule active : " + cell);
                }
            }
        }
    }

    @Test
    void doublon_de_licence_detecte_sans_tenir_compte_de_la_casse() throws Exception {
        ResultatImportLicences r = LecteurLicencesCsv.lire("l.csv", b(E
                + "ab1;A;B;M;08910001;100\r\nAB1;C;D;M;08910001;999\r\n"));
        assertEquals(1, r.fiches().size());
        assertEquals("ab1", r.fiches().get(0).getNumeroLicence());
        assertEquals(1, r.lignesRefusees());
    }

    @Test
    void formule_precedee_d_espaces_est_neutralisee() {
        assertEquals("' =1+1", CsvFederal.cellule(" =1+1"));
        assertEquals("'  @SUM(A1)", CsvFederal.cellule("  @SUM(A1)"));
        assertEquals(" 11-7", CsvFederal.cellule(" 11-7"));
    }

    @Test
    void saut_de_ligne_interne_remplace_par_une_espace_a_la_lecture() {
        assertEquals("Saint Maur", CsvFederal.nettoyer("Saint\r\nMaur"));
        assertEquals("Saint Maur", CsvFederal.nettoyer("Saint \nMaur"));
        assertEquals("a b", CsvFederal.nettoyer("a\n\nb\n"));
    }

    @Test
    void depassement_de_la_limite_signale_aussi_pour_les_clubs() throws Exception {
        StringBuilder sb = new StringBuilder("N° club;Nom;Département;Ville;Courriel officiel\r\n");
        for (int i = 0; i < CsvFederal.LIGNES_MAX + 3; i++) {
            sb.append(String.format("%08d", 10_000_000 + i)).append(";C;91;Brunoy;c@exemple.fr\r\n");
        }
        ResultatImportClubs r = LecteurClubsCsv.lire("c.csv", b(sb.toString()));
        assertEquals(CsvFederal.LIGNES_MAX, r.clubs().size());
        assertEquals(1, r.anomalies().size());
        assertEquals(CsvFederal.LIGNES_MAX + 2, r.anomalies().get(0).ligne());
        assertTrue(r.anomalies().get(0).motif().contains("limite"));
    }

    @Test
    void fichier_sous_la_limite_aucun_depassement_signale() throws Exception {
        StringBuilder sb = new StringBuilder(E);
        for (int i = 0; i < 100; i++) {
            sb.append("L").append(i).append(";N;P;M;08910001;100\r\n");
        }
        assertTrue(LecteurLicencesCsv.lire("l.csv", b(sb.toString())).anomalies().isEmpty());
    }
}
