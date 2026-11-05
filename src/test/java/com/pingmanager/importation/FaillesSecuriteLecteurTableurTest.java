package com.pingmanager.importation;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Audit de sécurité de l'import de tableurs (voir RAPPORT_SECURITE.md).
 *
 * <p>Les tests « faille_Sxx » passent <b>parce que</b> le code actuel est
 * vulnérable ; ils devront être <b>inversés</b> lors de la correction. Le test
 * « controle_en_place » vérifie au contraire une protection existante (XXE) et
 * doit rester tel quel.</p>
 */
class FaillesSecuriteLecteurTableurTest {

    private static final String NS = "http://schemas.openxmlformats.org/spreadsheetml/2006/main";

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

    /**
     * Faille S-07 : la limite MAX_LIGNES (20 000) porte sur le nombre d'éléments
     * {@code <row>}, pas sur leur numéro. Un classeur de quelques centaines d'octets
     * avec {@code <row r="60000">} produit 60 000 lignes ; avec r="2000000000" la
     * boucle de remplissage crée deux milliards de listes (mémoire saturée, logiciel
     * figé). Correctif : refuser tout numéro de ligne > MAX_LIGNES.
     */
    @Test
    void numero_de_ligne_geant_contourne_la_limite_de_lignes_faille_S07() throws Exception {
        String feuille = "<worksheet xmlns=\"" + NS + "\"><sheetData><row r=\"60000\">"
                + "<c r=\"A60000\" t=\"inlineStr\"><is><t>x</t></is></c></row></sheetData></worksheet>";
        byte[] octets = xlsx(feuille);
        assertTrue(octets.length < 1024, "fichier minuscule");

        List<List<String>> lignes = LecteurTableur.lire("joueurs.xlsx", octets).lignes();

        assertEquals(60_000, lignes.size(), "comportement vulnérable : 3 fois la limite de 20 000 lignes");
    }

    /**
     * Faille S-07 (robustesse) : un attribut de ligne non numérique lève une
     * {@link NumberFormatException} brute au lieu d'une {@code ImportException} :
     * l'écran d'import, qui n'attrape que {@code ImportException}, la laisse remonter.
     */
    @Test
    void numero_de_ligne_non_numerique_echappe_au_message_d_import_faille_S07() throws Exception {
        String feuille = "<worksheet xmlns=\"" + NS + "\"><sheetData><row r=\"abc\"/></sheetData></worksheet>";
        byte[] octets = xlsx(feuille);

        assertThrows(NumberFormatException.class, () -> LecteurTableur.lire("joueurs.xlsx", octets));
    }

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
    }
}
