package com.pingmanager.importation;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Fichiers piégés à l'import (README section 5, point 17 ; anciennes failles
 * S-07 et bombes zip) : de vrais fichiers malveillants sont fabriqués ici, et
 * le lecteur doit répondre vite par une {@link LecteurTableur.ImportException}
 * claire ou par un résultat borné, jamais par un plantage ni un blocage.
 */
class RobustesseLecteurTableurTest {

    private static final String NS = "http://schemas.openxmlformats.org/spreadsheetml/2006/main";
    private static final Duration RAPIDE = Duration.ofSeconds(5);

    private static byte[] xlsx(String feuille) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream z = new ZipOutputStream(out)) {
            entree(z, "xl/workbook.xml", "<workbook xmlns=\"" + NS + "\"><sheets><sheet name=\"Joueurs\" sheetId=\"1\"/>"
                    + "</sheets></workbook>");
            entree(z, "xl/worksheets/sheet1.xml", feuille);
        }
        return out.toByteArray();
    }

    private static void entree(ZipOutputStream z, String nom, String contenu) throws IOException {
        z.putNextEntry(new ZipEntry(nom));
        z.write(contenu.getBytes(StandardCharsets.UTF_8));
        z.closeEntry();
    }

    /** Bombe zip : une entrée de {@code mega} Mo de zéros, compressée au maximum. */
    private static byte[] bombe(String nomEntree, int mega) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream z = new ZipOutputStream(out)) {
            z.setLevel(Deflater.BEST_COMPRESSION);
            entree(z, "xl/workbook.xml", "<workbook xmlns=\"" + NS + "\"><sheets/></workbook>");
            z.putNextEntry(new ZipEntry(nomEntree));
            byte[] zeros = new byte[1024 * 1024];
            for (int i = 0; i < mega; i++) {
                z.write(zeros);
            }
            z.closeEntry();
        }
        return out.toByteArray();
    }

    private static String feuille(String lignes) {
        return "<worksheet xmlns=\"" + NS + "\"><sheetData>" + lignes + "</sheetData></worksheet>";
    }

    // --- Numéros de ligne et de colonne géants (ancienne faille S-07) ---

    @Test
    void numero_de_ligne_au_dela_de_la_limite_ne_contourne_plus_la_borne() throws Exception {
        byte[] octets = xlsx(feuille("<row r=\"60000\"><c r=\"A60000\" t=\"inlineStr\"><is><t>x</t></is></c></row>"));
        assertTrue(octets.length < 1024, "fichier minuscule");

        List<List<String>> lignes = LecteurTableur.lire("joueurs.xlsx", octets).lignes();

        assertTrue(lignes.size() <= LecteurTableur.MAX_LIGNES, "lignes : " + lignes.size());
    }

    @Test
    void numero_de_ligne_de_deux_milliards_ou_hors_entier_traite_vite_sans_saturer_la_memoire() throws Exception {
        for (String r : List.of("2000000000", "99999999999999999999999999")) {
            byte[] octets = xlsx(feuille("<row r=\"1\"><c r=\"A1\" t=\"inlineStr\"><is><t>Nom</t></is></c></row>"
                    + "<row r=\"" + r + "\"><c t=\"inlineStr\"><is><t>x</t></is></c></row>"));
            List<List<String>> lignes = assertTimeoutPreemptively(RAPIDE,
                    () -> LecteurTableur.lire("joueurs.xlsx", octets).lignes());
            assertEquals(List.of("Nom"), lignes.get(0));
            assertTrue(lignes.size() <= LecteurTableur.MAX_LIGNES);
        }
    }

    @Test
    void numero_de_ligne_non_numerique_ne_fait_plus_planter_l_import() throws Exception {
        byte[] octets = xlsx(feuille("<row r=\"abc\"><c t=\"inlineStr\"><is><t>x</t></is></c></row>"));

        List<List<String>> lignes = LecteurTableur.lire("joueurs.xlsx", octets).lignes();

        assertEquals(List.of(List.of("x")), lignes);
    }

    @Test
    void reference_de_colonne_geante_bornee_sans_debordement() throws Exception {
        assertEquals(LecteurTableur.MAX_COLONNES, LecteurTableur.colonne("ZZZZZZZZZZZZZZZZ1"));
        assertEquals(27, LecteurTableur.colonne("AB12"));
        byte[] octets = xlsx(feuille("<row r=\"1\"><c r=\"A1\" t=\"inlineStr\"><is><t>a</t></is></c>"
                + "<c r=\"ZZZZZZZZZZZZZZZZ1\" t=\"inlineStr\"><is><t>b</t></is></c>"
                + "<c r=\"\u00c91\" t=\"inlineStr\"><is><t>c</t></is></c></row>"));

        List<List<String>> lignes = assertTimeoutPreemptively(RAPIDE,
                () -> LecteurTableur.lire("joueurs.xlsx", octets).lignes());

        assertTrue(lignes.get(0).size() <= LecteurTableur.MAX_COLONNES);
        assertEquals("a", lignes.get(0).get(0));
    }

    @Test
    void ods_avec_repetitions_geantes_ou_invalides_borne() throws Exception {
        String t = "urn:oasis:names:tc:opendocument:xmlns:table:1.0";
        String contenu = "<office:document-content xmlns:office=\"urn:oasis:names:tc:opendocument:xmlns:office:1.0\""
                + " xmlns:table=\"" + t + "\" xmlns:text=\"urn:oasis:names:tc:opendocument:xmlns:text:1.0\">"
                + "<office:body><office:spreadsheet><table:table table:name=\"J\">"
                + "<table:table-row table:number-rows-repeated=\"2147483647\">"
                + "<table:table-cell table:number-columns-repeated=\"99999999999999999999\"><text:p>x</text:p></table:table-cell>"
                + "</table:table-row>"
                + "<table:table-row table:number-rows-repeated=\"abc\"><table:table-cell><text:p>y</text:p></table:table-cell>"
                + "</table:table-row></table:table></office:spreadsheet></office:body></office:document-content>";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream z = new ZipOutputStream(out)) {
            entree(z, "content.xml", contenu);
        }

        List<List<String>> lignes = assertTimeoutPreemptively(RAPIDE,
                () -> LecteurTableur.lire("joueurs.ods", out.toByteArray()).lignes());

        assertEquals(LecteurTableur.MAX_LIGNES, lignes.size());
        assertEquals(LecteurTableur.MAX_COLONNES, lignes.get(0).size());
    }

    // --- Bombes zip ---

    @Test
    void bombe_zip_minuscule_refusee_vite_sans_tout_decompresser() throws Exception {
        byte[] octets = bombe("xl/worksheets/sheet1.xml", 1024); // 1 Go de zéros
        assertTrue(octets.length < 2 * 1024 * 1024, "archive de " + octets.length + " octets");

        LecteurTableur.ImportException e = assertTimeoutPreemptively(RAPIDE,
                () -> assertThrows(LecteurTableur.ImportException.class,
                        () -> LecteurTableur.lire("joueurs.xlsx", octets)));
        assertTrue(e.getMessage().contains("bombe zip"), e.getMessage());
    }

    @Test
    void bombe_zip_cachee_dans_une_entree_ignoree_aussi_refusee() throws Exception {
        byte[] octets = bombe("xl/media/image1.bin", 1024);

        LecteurTableur.ImportException e = assertTimeoutPreemptively(RAPIDE,
                () -> assertThrows(LecteurTableur.ImportException.class,
                        () -> LecteurTableur.lireClasseur("joueurs.xlsx", octets)));
        assertTrue(e.getMessage().contains("bombe zip"), e.getMessage());
    }

    @Test
    void bombe_zip_ods_refusee() throws Exception {
        byte[] octets = bombe("content.xml", 512);

        assertTimeoutPreemptively(RAPIDE, () -> assertThrows(LecteurTableur.ImportException.class,
                () -> LecteurTableur.lire("joueurs.ods", octets)));
    }

    @Test
    void archive_a_trop_d_entrees_refusee() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream z = new ZipOutputStream(out)) {
            for (int i = 0; i <= LecteurTableur.MAX_ENTREES_ZIP; i++) {
                z.putNextEntry(new ZipEntry("x" + i));
                z.closeEntry();
            }
        }
        LecteurTableur.ImportException e = assertThrows(LecteurTableur.ImportException.class,
                () -> LecteurTableur.lire("joueurs.xlsx", out.toByteArray()));
        assertTrue(e.getMessage().contains("suspecte"), e.getMessage());
    }

    @Test
    void fichier_au_dela_de_la_taille_maximale_refuse() {
        byte[] enorme = new byte[(int) LecteurTableur.TAILLE_MAX_FICHIER + 1];
        assertThrows(LecteurTableur.ImportException.class, () -> LecteurTableur.lire("joueurs.csv", enorme));
        assertThrows(LecteurTableur.ImportException.class, () -> LecteurTableur.lireClasseur("joueurs.xlsx", enorme));
    }

    // --- XML piégé ---

    /** Contrôle en place : une DTD (porte d'entrée XXE / « billion laughs ») est refusée. */
    @Test
    void doctype_et_entite_externe_refuses_controle_en_place() throws Exception {
        String feuille = "<?xml version=\"1.0\"?><!DOCTYPE w [<!ENTITY xxe SYSTEM \"file:///etc/passwd\">]>"
                + "<worksheet xmlns=\"" + NS + "\"><sheetData><row r=\"1\"><c t=\"inlineStr\"><is><t>&xxe;</t></is></c>"
                + "</row></sheetData></worksheet>";
        byte[] octets = xlsx(feuille);

        LecteurTableur.ImportException e = assertThrows(LecteurTableur.ImportException.class,
                () -> LecteurTableur.lire("joueurs.xlsx", octets));
        assertTrue(e.getMessage().contains("XML"), e.getMessage());
        assertFalse(e.getMessage().contains("root:"), "aucun contenu du fichier local divulgué");
    }

    @Test
    void entites_en_cascade_billion_laughs_refusees_vite() throws Exception {
        StringBuilder dtd = new StringBuilder("<!DOCTYPE w [<!ENTITY l0 \"lol\">");
        for (int i = 1; i <= 9; i++) {
            dtd.append("<!ENTITY l").append(i).append(" \"").append(("&l" + (i - 1) + ";").repeat(10)).append("\">");
        }
        dtd.append("]>");
        byte[] octets = xlsx("<?xml version=\"1.0\"?>" + dtd + feuille("<row><c t=\"inlineStr\"><is><t>&l9;</t></is></c></row>"));

        assertTimeoutPreemptively(RAPIDE, () -> assertThrows(LecteurTableur.ImportException.class,
                () -> LecteurTableur.lire("joueurs.xlsx", octets)));
    }

    // --- CSV et JSON ---

    @Test
    void csv_avec_un_million_de_separateurs_sur_une_ligne_borne_en_colonnes() throws Exception {
        String texte = "Nom;" + ";".repeat(1_000_000) + "\nDupont;x\n";
        List<List<String>> lignes = assertTimeoutPreemptively(RAPIDE,
                () -> LecteurTableur.lire("joueurs.csv", texte.getBytes(StandardCharsets.UTF_8)).lignes());

        assertEquals(LecteurTableur.MAX_COLONNES, lignes.get(0).size());
        assertEquals(List.of("Dupont", "x"), lignes.get(1));
    }

    @Test
    void csv_trop_long_borne_a_la_limite_de_lignes() throws Exception {
        String texte = "a\n".repeat(LecteurTableur.MAX_LIGNES + 5_000);
        List<List<String>> lignes = LecteurTableur.lire("joueurs.csv", texte.getBytes(StandardCharsets.UTF_8)).lignes();
        assertEquals(LecteurTableur.MAX_LIGNES + 1, lignes.size(), "en-tête + limite de lignes");
    }

    @Test
    void json_imbrique_sur_100_000_niveaux_donne_un_message_d_import() {
        byte[] octets = ("[" + "{\"a\":".repeat(100_000) + "1" + "}".repeat(100_000) + "]")
                .getBytes(StandardCharsets.UTF_8);

        LecteurTableur.ImportException e = assertTimeoutPreemptively(RAPIDE,
                () -> assertThrows(LecteurTableur.ImportException.class,
                        () -> LecteurTableur.lireClasseur("joueurs.json", octets)));
        assertTrue(e.getMessage().startsWith("JSON illisible"), e.getMessage());
    }

    @Test
    void json_aux_cles_innombrables_borne_en_colonnes_et_en_lignes() throws Exception {
        StringBuilder sb = new StringBuilder("[{");
        for (int i = 0; i < 5_000; i++) {
            sb.append(i == 0 ? "" : ",").append("\"k").append(i).append("\":1");
        }
        sb.append("}");
        sb.append(",{\"k0\":2}".repeat(LecteurTableur.MAX_LIGNES + 10)).append("]");

        List<List<String>> lignes = assertTimeoutPreemptively(RAPIDE,
                () -> LecteurTableur.lire("joueurs.json", sb.toString().getBytes(StandardCharsets.UTF_8)).lignes());

        assertEquals(LecteurTableur.MAX_COLONNES, lignes.get(0).size());
        assertTrue(lignes.size() <= LecteurTableur.MAX_LIGNES);
    }

    @Test
    void entier_borne_resiste_aux_valeurs_absurdes() {
        assertEquals(1, LecteurTableur.entierBorne(null, 1, 10));
        assertEquals(1, LecteurTableur.entierBorne("-5", 1, 10));
        assertEquals(1, LecteurTableur.entierBorne("abc", 1, 10));
        assertEquals(10, LecteurTableur.entierBorne("99999999999999999999999", 1, 10));
        assertEquals(7, LecteurTableur.entierBorne(" 7 ", 1, 10));
    }
}
