package com.pingmanager.importation;

import com.pingmanager.core.ImportJoueurs;
import com.pingmanager.core.ImportJoueurs.Champ;
import com.pingmanager.core.ImportTableaux;
import com.pingmanager.model.CategorieAge;
import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.format.RegistreFormats;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

/** Import de joueurs : lecture des formats, colonnes obligatoires, plan et application. */
class ImportJoueursTest {

    private static final LocalDate JOUR = LocalDate.of(2027, 3, 13);

    private static Tournoi tournoi() {
        Tournoi t = new Tournoi("Open", "Club", JOUR);
        t.setPeriode(JOUR, JOUR);
        t.ajouterTableau(new Tableau("A – ≤ 999 pts", ConfigurationTableau.depuisFormat(RegistreFormats.formatParDefaut())
                .pointMax(999).placesMax(2).tailleMaxListeAttente(1).build()));
        return t;
    }

    // ------------------------------------------------------------------ formats

    @Test
    void csv_point_virgule_windows_1252_avec_titre_au_dessus() throws Exception {
        String texte = "Tournoi de l'Exemple;;;\r\nNom;Prénom;Nb points;Club\r\n"
                + "DURAND;Élodie;1 234;Ping Club\r\n\"MARTIN, dit Max\";paul;987;\"TT \"\"Les As\"\"\"\r\n";
        byte[] o = texte.getBytes(Charset.forName("windows-1252"));
        LecteurTableur.Contenu c = LecteurTableur.lire("joueurs.csv", o);
        assertEquals(LecteurTableur.Format.CSV, c.format());
        ImportJoueurs.Analyse a = ImportJoueurs.analyser(c.lignes());
        assertTrue(a.colonnesCompletes());
        assertEquals(2, a.lignes().size());
        assertEquals("Élodie", a.lignes().get(0).prenom());
        assertEquals(1234, a.lignes().get(0).points());
        assertEquals("MARTIN, DIT MAX", a.lignes().get(1).nom());
        assertEquals("Paul", a.lignes().get(1).prenom());
        assertEquals("TT \"Les As\"", a.lignes().get(1).club());
        assertEquals(3, a.lignes().get(0).ligne(), "numéro de ligne du tableur");
    }

    @Test
    void csv_virgule_utf8_bom_et_tabulation() throws Exception {
        byte[] o = ("﻿last name,first name,points,club\nLEROY,Anne,1500,Club A\n").getBytes(StandardCharsets.UTF_8);
        assertEquals(1, ImportJoueurs.analyser(LecteurTableur.lire("a.csv", o).lignes()).lignes().size());
        byte[] t = "Nom\tPrénom\tPoints\tClub\nX\tY\t800\tZ\n".getBytes(StandardCharsets.UTF_8);
        assertEquals(800, ImportJoueurs.analyser(LecteurTableur.lire("a.txt", t).lignes()).lignes().get(0).points());
    }

    @Test
    void modele_xlsx_deux_feuilles_relu_par_le_lecteur(@TempDir Path dir) throws Exception {
        Path f = dir.resolve("modele.xlsx");
        Files.write(f, ModeleImport.xlsx(JOUR));
        LecteurTableur.Classeur c = LecteurTableur.lireClasseur(f);
        assertEquals(LecteurTableur.Format.XLSX, c.format());
        assertEquals(List.of("Tableaux", "Joueurs", "Listes"), List.copyOf(c.feuilles().keySet()));

        ImportTableaux.Analyse t = ImportTableaux.analyser(c.feuille("tableau"));
        assertTrue(t.enteteTrouvee());
        assertTrue(t.erreurs().isEmpty(), t.erreurs().toString());
        assertEquals(4, t.definitions().size());
        ImportTableaux.Definition a = t.definitions().get(0);
        assertEquals("A", a.nom());
        assertNull(a.pointsMin());
        assertEquals(799, a.pointsMax());
        assertEquals(JOUR, a.jour());
        assertEquals(LocalTime.of(9, 0), a.heure());
        assertEquals(48, a.places());
        assertEquals(8.0, a.tarif());
        assertEquals(800, t.definitions().get(1).pointsMin());
        assertEquals(Sexe.FEMME, t.definitions().get(2).sexe());
        assertEquals("Toutes séries", t.definitions().get(3).libellePoints());

        // Les trois lignes d'exemple de la feuille Joueurs sont ignorées.
        ImportJoueurs.Analyse j = ImportJoueurs.analyser(c.feuille("joueur"));
        assertTrue(j.colonnesCompletes(), j.manquantes().toString());
        assertEquals(0, j.lignes().size());
        assertEquals(3, j.avertissements().stream().filter(x -> x.message().contains("exemple")).count());
        assertEquals(3, j.colonnesTableau().size(), "Tableau 1, 2 et 3");

        Path csv = dir.resolve("modele.csv");
        ModeleImport.ecrireCsv(csv);
        assertEquals(3, ImportJoueurs.analyser(LecteurTableur.lire(csv).lignes()).lignes().size());
    }

    @Test
    void classeur_tableaux_et_joueurs_applique() {
        Tournoi t = new Tournoi("Externe", "Club", JOUR);
        t.setPeriode(JOUR, JOUR);
        ImportTableaux.Analyse defs = ImportTableaux.analyser(List.of(
                List.of("Tableaux du tournoi"),
                List.of(),
                List.of("Nom du tableau *", "Points min", "Points max", "Public", "Jour", "Heure", "Places", "Liste d'attente"),
                List.of("A", "", "799", "Mixte", "", "9h", "2", "1"),
                List.of("Dames", "", "", "Dames", "13/03/2027", "0.625", "", ""),
                List.of("Vide", "1000", "1499", "", "", "", "", ""),
                List.of("Faux", "1500", "900", "", "", "", "", "")));
        assertEquals(3, defs.definitions().size());
        assertEquals(1, defs.erreurs().size(), "min > max");
        ImportJoueurs.Analyse j = ImportJoueurs.analyser(List.of(
                List.of("Nom", "Prénom", "Points", "Club", "Sexe", "Catégorie", "Tableau 1", "Tableau 2"),
                List.of("UN", "Un", "700", "C1", "F", "V45", "A", "Dames"),
                List.of("DEUX", "Deux", "750", "C2", "H", "Junior 2", "a", ""),
                List.of("TROIS", "Trois", "600", "C3", "H", "Senior", "A", ""),
                List.of("QUATRE", "Quatre", "650", "C1", "H", "", "A", ""),
                List.of("CINQ", "Cinq", "1200", "C2", "H", "zzz", "A", ""),
                List.of("SIX", "Six", "900", "C2", "H", "", "Dames", "")));
        ImportJoueurs.Plan p = ImportJoueurs.planifier(t, j, new ImportJoueurs.Options(null, true), defs.definitions());
        ImportJoueurs.PlanTableau pa = p.tableaux().get("A");
        assertNotNull(pa.definition());
        assertEquals(2, pa.inscrits().size(), "2 places");
        assertEquals(1, pa.attente().size(), "1 en attente");
        assertEquals(2, pa.refus().size(), "attente pleine + 1200 > 799");
        assertEquals(1, p.tableaux().get("Dames").refus().size(), "SIX n'est pas une dame");
        assertTrue(p.tableaux().containsKey("Vide"), "tableau défini sans joueur : créé quand même");

        ImportJoueurs.Bilan b = ImportJoueurs.appliquer(t, p, Map.of());
        assertEquals(3, b.tableauxCrees());
        Tableau a = ImportJoueurs.trouver(t, "A");
        assertEquals(799, a.getConfiguration().getPointsMax());
        assertEquals(2, a.getConfiguration().getPlacesMax());
        assertEquals(JOUR.atTime(9, 0), a.getHoraires().getDebutMatchs(), "tournoi d'un jour : jour déduit");
        Tableau dames = ImportJoueurs.trouver(t, "Dames");
        assertEquals(Sexe.FEMME, dames.getConfiguration().getSexeRequis());
        assertEquals(1, dames.getConfiguration().getPlacesMax(), "places = liste importée");
        assertEquals(JOUR.atTime(15, 0), dames.getHoraires().getDebutMatchs());
        assertEquals(CategorieAge.VETERAN_45, dames.getInscrits().get(0).getCategorie());
        assertEquals(1000, ImportJoueurs.trouver(t, "Vide").getConfiguration().getPointsMin());
        assertTrue(j.avertissements().stream().anyMatch(x -> x.message().contains("zzz")));
    }

    @Test
    void categories_en_clair_ou_en_code() {
        assertEquals(CategorieAge.VETERAN_45, ImportJoueurs.lireCategorie("Vétéran 45"));
        assertEquals(CategorieAge.VETERAN_70, ImportJoueurs.lireCategorie("V70"));
        assertEquals(CategorieAge.JUNIOR, ImportJoueurs.lireCategorie("J3"));
        assertEquals(CategorieAge.MINIME, ImportJoueurs.lireCategorie("U13"));
        assertEquals(CategorieAge.SENIOR, ImportJoueurs.lireCategorie("S"));
        assertEquals(CategorieAge.POUSSIN, ImportJoueurs.lireCategorie("Poussins"));
        assertNull(ImportJoueurs.lireCategorie("?"));
    }

    @Test
    void xlsx_chaines_partagees_et_date_serie() throws Exception {
        String ss = "<sst xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
                + "<si><t>Nom</t></si><si><t>Prénom</t></si><si><t>Points</t></si><si><t>Club</t></si>"
                + "<si><t>Naissance</t></si><si><r><t>LE</t></r><r><t>GALL</t></r></si><si><t>Yann</t></si><si><t>Brest TT</t></si></sst>";
        String sheet = "<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>"
                + "<row r=\"1\"><c r=\"A1\" t=\"s\"><v>0</v></c><c r=\"B1\" t=\"s\"><v>1</v></c><c r=\"C1\" t=\"s\"><v>2</v></c>"
                + "<c r=\"D1\" t=\"s\"><v>3</v></c><c r=\"E1\" t=\"s\"><v>4</v></c></row>"
                + "<row r=\"3\"><c r=\"A3\" t=\"s\"><v>5</v></c><c r=\"B3\" t=\"s\"><v>6</v></c><c r=\"C3\"><v>1450.0</v></c>"
                + "<c r=\"D3\" t=\"s\"><v>7</v></c><c r=\"E3\"><v>37330</v></c></row></sheetData></worksheet>";
        byte[] z = zip(Map.of("xl/workbook.xml", "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheets/></workbook>",
                "xl/sharedStrings.xml", ss, "xl/worksheets/sheet1.xml", sheet));
        ImportJoueurs.Analyse a = ImportJoueurs.analyser(LecteurTableur.lire("j.xlsx", z).lignes());
        assertEquals(1, a.lignes().size());
        assertEquals("LEGALL", a.lignes().get(0).nom());
        assertEquals(1450, a.lignes().get(0).points());
        assertEquals(LocalDate.of(2002, 3, 15), a.lignes().get(0).naissance());
        assertEquals(3, a.lignes().get(0).ligne());
    }

    @Test
    void ods_cellules_repetees() throws Exception {
        String content = "<office:document-content xmlns:office=\"urn:oasis:names:tc:opendocument:xmlns:office:1.0\" "
                + "xmlns:table=\"urn:oasis:names:tc:opendocument:xmlns:table:1.0\" xmlns:text=\"urn:oasis:names:tc:opendocument:xmlns:text:1.0\">"
                + "<office:body><office:spreadsheet><table:table table:name=\"Feuille1\">"
                + "<table:table-row><table:table-cell office:value-type=\"string\"><text:p>Nom</text:p></table:table-cell>"
                + "<table:table-cell office:value-type=\"string\"><text:p>Prénom</text:p></table:table-cell>"
                + "<table:table-cell office:value-type=\"string\"><text:p>Points</text:p></table:table-cell>"
                + "<table:table-cell office:value-type=\"string\"><text:p>Club</text:p></table:table-cell>"
                + "<table:table-cell table:number-columns-repeated=\"1020\"/></table:table-row>"
                + "<table:table-row><table:table-cell office:value-type=\"string\"><text:p>PETIT</text:p></table:table-cell>"
                + "<table:table-cell office:value-type=\"string\"><text:p>Léa</text:p></table:table-cell>"
                + "<table:table-cell office:value-type=\"float\" office:value=\"612\"><text:p>612</text:p></table:table-cell>"
                + "<table:table-cell office:value-type=\"string\"><text:p>Club B</text:p></table:table-cell></table:table-row>"
                + "<table:table-row table:number-rows-repeated=\"1048570\"><table:table-cell table:number-columns-repeated=\"1024\"/></table:table-row>"
                + "</table:table></office:spreadsheet></office:body></office:document-content>";
        LecteurTableur.Contenu c = LecteurTableur.lire("j.ods", zip(Map.of("content.xml", content)));
        assertEquals(2, c.lignes().size());
        ImportJoueurs.Analyse a = ImportJoueurs.analyser(c.lignes());
        assertEquals(612, a.lignes().get(0).points());
    }

    @Test
    void json_liste_ou_objet_joueurs() throws Exception {
        String j = "{\"joueurs\": [{\"nom\": \"ROUX\", \"prenom\": \"Lina\", \"points\": 1320, \"club\": \"Ping 13\","
                + " \"tableaux\": [\"A\", \"B\"], \"sexe\": \"F\"}]}";
        ImportJoueurs.Analyse a = ImportJoueurs.analyser(LecteurTableur.lire("j.json", j.getBytes(StandardCharsets.UTF_8)).lignes());
        assertEquals(List.of("A", "B"), a.lignes().get(0).tableaux());
        assertEquals(1320, a.lignes().get(0).points());
        assertThrows(LecteurTableur.ImportException.class,
                () -> LecteurTableur.lire("j.json", "{\"x\": 1}".getBytes(StandardCharsets.UTF_8)));
        assertThrows(LecteurTableur.ImportException.class, () -> LecteurTableur.lire("vieux.xls", new byte[] {1}));
    }

    // ------------------------------------------------------------------ analyse

    @Test
    void colonnes_obligatoires_et_erreurs_par_ligne() {
        ImportJoueurs.Analyse sansClub = ImportJoueurs.analyser(List.of(List.of("Nom", "Prénom", "Points"),
                List.of("A", "B", "900")));
        assertFalse(sansClub.colonnesCompletes());
        assertEquals(List.of(Champ.CLUB), sansClub.manquantes());

        ImportJoueurs.Analyse a = ImportJoueurs.analyser(List.of(
                List.of("Nom", "Prénom", "Points", "Club", "Sexe"),
                List.of("DUPONT", "", "900", "C", "H"),
                List.of("DURAND", "Paul", "beaucoup", "C", "H"),
                List.of("DURAND", "Paul", "900", "C", "X"),
                List.of("DURAND", "Paul", "900", "C", "H"),
                List.of("", "", "", "", "")));
        assertEquals(1, a.lignes().size());
        assertEquals(2, a.erreurs().size());
        assertEquals("Ligne 2 : prénom manquant (DUPONT )", a.erreurs().get(0).toString().replace("  ", " "));
        assertTrue(a.erreurs().get(1).message().contains("illisibles"));
        assertTrue(a.avertissements().stream().anyMatch(p -> p.message().contains("non reconnu")));
        assertTrue(a.avertissements().stream().anyMatch(p -> p.message().contains("deux fois")));
    }

    // ------------------------------------------------------------------ plan et application

    @Test
    void plan_puis_application_tableau_existant_et_nouveau() {
        Tournoi t = tournoi();
        ImportJoueurs.Analyse a = ImportJoueurs.analyser(List.of(
                List.of("Nom", "Prénom", "Points", "Club", "Tableau", "Date de naissance"),
                List.of("UN", "Un", "900", "C1", "A", ""),
                List.of("DEUX", "Deux", "800", "C2", "A; Vétérans", "12/03/1980"),
                List.of("TROIS", "Trois", "700", "C1", "a", ""),
                List.of("QUATRE", "Quatre", "600", "C3", "A", ""),
                List.of("CINQ", "Cinq", "1500", "C3", "A", ""),
                List.of("SIX", "Six", "1100", "C2", "", "")));
        ImportJoueurs.Plan p = ImportJoueurs.planifier(t, a, new ImportJoueurs.Options(null, true));
        ImportJoueurs.PlanTableau pa = p.tableaux().get("A – ≤ 999 pts");
        assertFalse(pa.estNouveau());
        assertEquals(2, pa.inscrits().size());
        assertEquals(1, pa.attente().size());
        assertEquals(2, pa.refus().size(), "un hors points, un liste d'attente pleine");
        assertTrue(pa.refus().stream().anyMatch(r -> r.message().contains("dépassent la limite")));
        ImportJoueurs.PlanTableau pv = p.tableaux().get("Vétérans");
        assertTrue(pv.estNouveau());
        assertEquals(1, p.sansTableau().size(), "SIX n'a pas de tableau");
        assertTrue(t.getTableaux().get(0).getInscrits().isEmpty(), "le plan ne modifie rien");

        ImportJoueurs.Bilan b = ImportJoueurs.appliquer(t, p, Map.of("Vétérans",
                new ImportJoueurs.HoraireNouveau(JOUR, LocalTime.of(14, 0))));
        assertEquals(1, b.tableauxCrees());
        assertEquals(3, b.inscrits());
        assertEquals(1, b.enAttente());
        Tableau vet = ImportJoueurs.trouver(t, "Vétérans");
        assertEquals(JOUR.atTime(14, 0), vet.getHoraires().getDebutMatchs());
        Joueur deux = vet.getInscrits().get(0);
        assertSame(deux, t.getTableaux().get(0).getInscrits().get(1), "même joueur dans ses deux tableaux");
        assertTrue(deux.getNumeroLicence().startsWith("IMP-"));
        assertEquals(CategorieAge.VETERAN_45, deux.getCategorie());
        assertEquals("C2", deux.getClub());

        // Un second import du même fichier ne crée pas de doublon.
        ImportJoueurs.Plan p2 = ImportJoueurs.planifier(t, a, new ImportJoueurs.Options(null, true));
        assertEquals(0, p2.nombreAjouts());
    }

    @Test
    void tableau_par_defaut_et_creation_desactivee() {
        Tournoi t = tournoi();
        ImportJoueurs.Analyse a = ImportJoueurs.analyser(List.of(List.of("Nom", "Prénom", "Points", "Club"),
                List.of("X", "Y", "900", "C")));
        assertEquals(1, ImportJoueurs.planifier(t, a, new ImportJoueurs.Options("Open", true)).tableaux().get("Open").inscrits().size());
        ImportJoueurs.Plan sans = ImportJoueurs.planifier(t, a, new ImportJoueurs.Options("Open", false));
        assertEquals(0, sans.nombreAjouts());
        assertEquals(1, sans.nombreRefus());
    }

    private static byte[] zip(Map<String, String> fichiers) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream z = new ZipOutputStream(out)) {
            for (Map.Entry<String, String> e : fichiers.entrySet()) {
                z.putNextEntry(new ZipEntry(e.getKey()));
                z.write(e.getValue().getBytes(StandardCharsets.UTF_8));
                z.closeEntry();
            }
        }
        return out.toByteArray();
    }
}
