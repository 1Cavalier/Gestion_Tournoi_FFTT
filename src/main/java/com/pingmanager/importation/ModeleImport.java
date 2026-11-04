package com.pingmanager.importation;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Modèles d'import à remplir par le club.
 *
 * <p><b>Classeur Excel</b> (.xlsx, écrit à la main, sans bibliothèque), deux feuilles
 * mises en forme :</p>
 * <ul>
 *   <li><b>Tableaux</b> : nom du tableau, points imposés (minimum, maximum), public,
 *       jour, heure, places, liste d'attente, tarif ;</li>
 *   <li><b>Joueurs</b> : nom, prénom, points, club (obligatoires, en-têtes bleu foncé),
 *       licence, sexe, catégorie, date de naissance, tableau 1 à 3 — avec listes
 *       déroulantes (sexe, catégorie, tableaux repris de la feuille Tableaux).</li>
 * </ul>
 * Titre et mode d'emploi en tête de feuille, en-tête figé, zone de saisie encadrée, une
 * feuille cachée « Listes » pour les catégories. Le lecteur ({@link LecteurTableur}) repère
 * l'en-tête sous le titre.
 *
 * <p><b>CSV</b> : une seule liste de joueurs (point-virgule, UTF-8 avec BOM).</p>
 */
public final class ModeleImport {

    public static final List<String> ENTETES = List.of("Nom", "Prénom", "Points", "Club",
            "Licence", "Sexe", "Tableau", "Date de naissance");

    static final List<String> ENTETES_TABLEAUX = List.of("Nom du tableau", "Points min", "Points max", "Public",
            "Jour", "Heure", "Places", "Liste d'attente", "Tarif (€)");

    static final List<String> ENTETES_JOUEURS = List.of("Nom", "Prénom", "Points", "Club", "Licence", "Sexe",
            "Catégorie", "Date de naissance", "Tableau 1", "Tableau 2", "Tableau 3");

    static final List<String> CATEGORIES = List.of("Poussin", "Benjamin", "Minime", "Cadet", "Junior", "Senior",
            "Vétéran 40", "Vétéran 45", "Vétéran 50", "Vétéran 55", "Vétéran 60", "Vétéran 65", "Vétéran 70",
            "Vétéran 75", "Vétéran 80", "Vétéran 85", "Vétéran 90");

    private static final int LIGNES_TABLEAUX = 60;
    private static final int LIGNES_JOUEURS = 400;

    // Styles (index dans cellXfs de styles.xml).
    private static final int S_ENTETE_OBLIG = 1;
    private static final int S_ENTETE_FACULT = 2;
    private static final int S_TITRE = 3;
    private static final int S_AIDE = 4;
    private static final int S_SAISIE = 5;
    private static final int S_DATE = 6;
    private static final int S_HEURE = 7;
    private static final int S_NOMBRE = 8;
    private static final int S_EXEMPLE = 9;

    private ModeleImport() {
    }

    public static void ecrireCsv(Path cible) throws IOException {
        List<List<String>> exemples = List.of(
                List.of("DUPONT", "Marie", "1234", "Ping Club Exemple", "9112345", "F", "A; Dames", "14/05/1990"),
                List.of("MARTIN", "Lucas", "987", "TT Exemple", "", "H", "B", ""),
                List.of("BERNARD", "Hugo", "1502", "Ping Club Exemple", "", "", "Open", ""));
        StringBuilder sb = new StringBuilder("﻿");
        sb.append(String.join(";", ENTETES)).append("\r\n");
        for (List<String> l : exemples) {
            sb.append(String.join(";", l)).append("\r\n");
        }
        Files.writeString(cible, sb.toString(), StandardCharsets.UTF_8);
    }

    public static void ecrireXlsx(Path cible) throws IOException {
        Files.write(cible, xlsx(LocalDate.now().plusMonths(2)));
    }

    // ==================================================================
    // Classeur
    // ==================================================================

    /** @param jourExemple date proposée dans les exemples de la feuille Tableaux */
    static byte[] xlsx(LocalDate jourExemple) throws IOException {
        long jour = ChronoUnit.DAYS.between(LocalDate.of(1899, 12, 30), jourExemple);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream z = new ZipOutputStream(out)) {
            entree(z, "[Content_Types].xml", CONTENT_TYPES);
            entree(z, "_rels/.rels", RELS);
            entree(z, "docProps/app.xml", "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                    + "<Properties xmlns=\"http://schemas.openxmlformats.org/officeDocument/2006/extended-properties\">"
                    + "<Application>PingManager</Application></Properties>");
            entree(z, "xl/workbook.xml", "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                    + "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" "
                    + "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">"
                    + "<bookViews><workbookView activeTab=\"0\"/></bookViews><sheets>"
                    + "<sheet name=\"Tableaux\" sheetId=\"1\" r:id=\"rId1\"/>"
                    + "<sheet name=\"Joueurs\" sheetId=\"2\" r:id=\"rId2\"/>"
                    + "<sheet name=\"Listes\" sheetId=\"3\" state=\"hidden\" r:id=\"rId3\"/>"
                    + "</sheets></workbook>");
            entree(z, "xl/_rels/workbook.xml.rels", "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                    + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                    + rel("rId1", "worksheet", "worksheets/sheet1.xml")
                    + rel("rId2", "worksheet", "worksheets/sheet2.xml")
                    + rel("rId3", "worksheet", "worksheets/sheet3.xml")
                    + rel("rId4", "styles", "styles.xml")
                    + "</Relationships>");
            entree(z, "xl/styles.xml", STYLES);
            entree(z, "xl/worksheets/sheet1.xml", feuilleTableaux(jour));
            entree(z, "xl/worksheets/sheet2.xml", feuilleJoueurs());
            entree(z, "xl/worksheets/sheet3.xml", feuilleListes());
        }
        return out.toByteArray();
    }

    private static String feuilleTableaux(long jour) {
        Feuille f = new Feuille("FF335F8A", new double[] {22, 12, 12, 13, 13, 9, 9, 15, 10});
        f.texte(1, 0, "PingManager — Tableaux du tournoi", S_TITRE);
        f.texte(2, 0, "Une ligne par tableau. Points imposés : « Points max » seul = tableau ≤ ce plafond ; "
                + "« Points min » et « Points max » = tranche ; les deux vides = toutes séries. Seul le nom est "
                + "obligatoire ; ce sont ces noms que l'on choisit dans la feuille « Joueurs ». Les quatre lignes grises "
                + "sont des exemples : modifiez-les ou effacez-les (sinon ces tableaux seront créés).", S_AIDE);
        f.hauteur(2, 48);
        f.fusion("A1:I1");
        f.fusion("A2:I2");
        for (int c = 0; c < ENTETES_TABLEAUX.size(); c++) {
            f.texte(4, c, ENTETES_TABLEAUX.get(c) + (c == 0 ? " *" : ""), c == 0 ? S_ENTETE_OBLIG : S_ENTETE_FACULT);
        }
        f.hauteur(4, 30);
        Object[][] ex = {
                {"A", null, 799, "Mixte", jour, 0.375, 48, 8, 8},
                {"B", 800, 1299, "Mixte", jour, 0.4375, 48, 8, 8},
                {"Dames", null, null, "Dames", jour, 0.625, 24, 4, 6},
                {"Open", null, null, "Mixte", jour + 1, 0.375, 32, 4, 10},
        };
        int[] styles = {S_SAISIE, S_NOMBRE, S_NOMBRE, S_SAISIE, S_DATE, S_HEURE, S_NOMBRE, S_NOMBRE, S_NOMBRE};
        for (int r = 0; r < LIGNES_TABLEAUX; r++) {
            int ligne = 5 + r;
            for (int c = 0; c < styles.length; c++) {
                Object v = r < ex.length ? ex[r][c] : null;
                int st = r < ex.length && styles[c] == S_SAISIE ? S_EXEMPLE : styles[c];
                f.valeur(ligne, c, v, st);
            }
        }
        f.figer(4);
        f.validation("list", "D5:D" + (4 + LIGNES_TABLEAUX), "\"Mixte,Dames,Messieurs\"", null,
                "Public", "Mixte, Dames ou Messieurs.");
        f.validation("whole", "B5:C" + (4 + LIGNES_TABLEAUX), "500", "5000",
                "Points", "Nombre entier de points de licence (500 et plus).");
        f.validation("whole", "G5:H" + (4 + LIGNES_TABLEAUX), "0", "1000", "Places", "Nombre entier.");
        return f.xml();
    }

    private static String feuilleJoueurs() {
        Feuille f = new Feuille("FF1C97A3", new double[] {18, 15, 9, 24, 12, 7, 13, 14, 15, 15, 15});
        f.texte(1, 0, "PingManager — Joueurs inscrits", S_TITRE);
        f.texte(2, 0, "Une ligne par joueur. Colonnes bleu foncé obligatoires : Nom, Prénom, Points, Club "
                + "(indispensables au tirage au sort et à la formation des poules). Sexe : H ou F. Catégorie : liste "
                + "déroulante (ou date de naissance). Tableaux : choisir dans la liste, qui reprend les noms de la "
                + "feuille « Tableaux ». Les lignes « EXEMPLE » sont ignorées à l'import : remplacez-les.", S_AIDE);
        f.hauteur(2, 48);
        f.fusion("A1:K1");
        f.fusion("A2:K2");
        for (int c = 0; c < ENTETES_JOUEURS.size(); c++) {
            boolean oblig = c < 4;
            f.texte(4, c, ENTETES_JOUEURS.get(c) + (oblig ? " *" : ""), oblig ? S_ENTETE_OBLIG : S_ENTETE_FACULT);
        }
        f.hauteur(4, 30);
        Object[][] ex = {
                {"EXEMPLE", "Marie", 1234, "Ping Club Exemple", "9112345", "F", "Senior", null, "B", "Dames", null},
                {"EXEMPLE", "Lucas", 987, "TT Exemple", null, "H", "Junior", null, "B", null, null},
                {"EXEMPLE", "Hugo", 1502, "Ping Club Exemple", null, "H", "Vétéran 45", null, "Open", null, null},
        };
        int[] styles = {S_SAISIE, S_SAISIE, S_NOMBRE, S_SAISIE, S_SAISIE, S_SAISIE, S_SAISIE, S_DATE,
                S_SAISIE, S_SAISIE, S_SAISIE};
        for (int r = 0; r < LIGNES_JOUEURS; r++) {
            int ligne = 5 + r;
            for (int c = 0; c < styles.length; c++) {
                Object v = r < ex.length ? ex[r][c] : null;
                int st = r < ex.length && styles[c] == S_SAISIE ? S_EXEMPLE : styles[c];
                f.valeur(ligne, c, v, st);
            }
        }
        f.figer(4);
        int fin = 4 + LIGNES_JOUEURS;
        f.validation("list", "F5:F" + fin, "\"H,F\"", null, "Sexe", "H (homme) ou F (femme).");
        f.validation("list", "G5:G" + fin, "Listes!$A$1:$A$" + CATEGORIES.size(), null,
                "Catégorie", "Catégorie d'âge de la saison (ou laisser vide et donner la date de naissance).");
        f.validation("list", "I5:K" + fin, "Tableaux!$A$5:$A$" + (4 + LIGNES_TABLEAUX), null,
                "Tableau", "Nom d'un tableau de la feuille « Tableaux ».");
        f.validation("whole", "C5:C" + fin, "500", "5000", "Points", "Points de licence (nombre entier, 500 et plus).");
        return f.xml();
    }

    private static String feuilleListes() {
        Feuille f = new Feuille(null, new double[] {16});
        for (int i = 0; i < CATEGORIES.size(); i++) {
            f.texte(i + 1, 0, CATEGORIES.get(i), 0);
        }
        return f.xml();
    }

    // ==================================================================
    // Écriture d'une feuille
    // ==================================================================

    /** Petite aide d'écriture d'une feuille (cellules, fusions, hauteurs, validations, volet figé). */
    private static final class Feuille {
        private final String couleurOnglet;
        private final double[] largeurs;
        private final java.util.TreeMap<Integer, StringBuilder> lignes = new java.util.TreeMap<>();
        private final java.util.Map<Integer, Double> hauteurs = new java.util.HashMap<>();
        private final StringBuilder fusions = new StringBuilder();
        private final StringBuilder validations = new StringBuilder();
        private int nbFusions;
        private int nbValidations;
        private int figee;

        Feuille(String couleurOnglet, double[] largeurs) {
            this.couleurOnglet = couleurOnglet;
            this.largeurs = largeurs;
        }

        void texte(int ligne, int col, String v, int style) {
            ligne(ligne).append("<c r=\"").append(ref(ligne, col)).append("\" s=\"").append(style)
                    .append("\" t=\"inlineStr\"><is><t xml:space=\"preserve\">").append(echapper(v)).append("</t></is></c>");
        }

        void valeur(int ligne, int col, Object v, int style) {
            if (v == null) {
                ligne(ligne).append("<c r=\"").append(ref(ligne, col)).append("\" s=\"").append(style).append("\"/>");
            } else if (v instanceof Number n) {
                ligne(ligne).append("<c r=\"").append(ref(ligne, col)).append("\" s=\"").append(style)
                        .append("\"><v>").append(n).append("</v></c>");
            } else {
                texte(ligne, col, v.toString(), style);
            }
        }

        void hauteur(int ligne, double h) {
            hauteurs.put(ligne, h);
        }

        void fusion(String plage) {
            fusions.append("<mergeCell ref=\"").append(plage).append("\"/>");
            nbFusions++;
        }

        void figer(int lignesFigees) {
            figee = lignesFigees;
        }

        void validation(String type, String plage, String f1, String f2, String titre, String message) {
            validations.append("<dataValidation type=\"").append(type).append("\" allowBlank=\"1\" showInputMessage=\"1\" ")
                    .append("showErrorMessage=\"1\" errorStyle=\"warning\" sqref=\"").append(plage.replace(":", ":"))
                    .append("\" promptTitle=\"").append(echapper(titre)).append("\" prompt=\"").append(echapper(message))
                    .append("\"");
            if ("whole".equals(type)) {
                validations.append(" operator=\"between\"");
            }
            validations.append("><formula1>").append(echapper(f1)).append("</formula1>");
            if (f2 != null) {
                validations.append("<formula2>").append(f2).append("</formula2>");
            }
            validations.append("</dataValidation>");
            nbValidations++;
        }

        private StringBuilder ligne(int n) {
            return lignes.computeIfAbsent(n, k -> new StringBuilder());
        }

        String xml() {
            StringBuilder sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                    + "<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" "
                    + "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">");
            if (couleurOnglet != null) {
                sb.append("<sheetPr><tabColor rgb=\"").append(couleurOnglet).append("\"/><pageSetUpPr fitToPage=\"1\"/></sheetPr>");
            }
            sb.append("<sheetViews><sheetView workbookViewId=\"0\" showGridLines=\"0\">");
            if (figee > 0) {
                sb.append("<pane ySplit=\"").append(figee).append("\" topLeftCell=\"A").append(figee + 1)
                        .append("\" activePane=\"bottomLeft\" state=\"frozen\"/>")
                        .append("<selection pane=\"bottomLeft\" activeCell=\"A").append(figee + 1)
                        .append("\" sqref=\"A").append(figee + 1).append("\"/>");
            }
            sb.append("</sheetView></sheetViews><sheetFormatPr defaultRowHeight=\"18\"/><cols>");
            for (int i = 0; i < largeurs.length; i++) {
                sb.append("<col min=\"").append(i + 1).append("\" max=\"").append(i + 1).append("\" width=\"")
                        .append(largeurs[i]).append("\" customWidth=\"1\"/>");
            }
            sb.append("</cols><sheetData>");
            for (var e : lignes.entrySet()) {
                sb.append("<row r=\"").append(e.getKey()).append("\"");
                Double h = hauteurs.get(e.getKey());
                if (h != null) {
                    sb.append(" ht=\"").append(h).append("\" customHeight=\"1\"");
                }
                sb.append(">").append(e.getValue()).append("</row>");
            }
            sb.append("</sheetData>");
            if (nbFusions > 0) {
                sb.append("<mergeCells count=\"").append(nbFusions).append("\">").append(fusions).append("</mergeCells>");
            }
            if (nbValidations > 0) {
                sb.append("<dataValidations count=\"").append(nbValidations).append("\">").append(validations)
                        .append("</dataValidations>");
            }
            sb.append("<pageMargins left=\"0.5\" right=\"0.5\" top=\"0.6\" bottom=\"0.6\" header=\"0.3\" footer=\"0.3\"/>"
                    + "<pageSetup orientation=\"landscape\" fitToWidth=\"1\" fitToHeight=\"0\"/></worksheet>");
            return sb.toString();
        }
    }

    private static String ref(int ligne, int col) {
        return String.valueOf((char) ('A' + col)) + ligne;
    }

    private static String echapper(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static String rel(String id, String type, String cible) {
        return "<Relationship Id=\"" + id + "\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/"
                + type + "\" Target=\"" + cible + "\"/>";
    }

    private static void entree(ZipOutputStream z, String nom, String contenu) throws IOException {
        z.putNextEntry(new ZipEntry(nom));
        z.write(contenu.getBytes(StandardCharsets.UTF_8));
        z.closeEntry();
    }

    private static final String CONTENT_TYPES = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
            + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
            + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
            + "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>"
            + "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
            + "<Override PartName=\"/xl/worksheets/sheet2.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
            + "<Override PartName=\"/xl/worksheets/sheet3.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
            + "<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>"
            + "<Override PartName=\"/docProps/app.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.extended-properties+xml\"/>"
            + "</Types>";

    private static final String RELS = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
            + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>"
            + "<Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties\" Target=\"docProps/app.xml\"/>"
            + "</Relationships>";

    /** Couleurs de la charte : bleu ardoise 2F4558, bleu 335F8A, bleu doux D6E4F0, gris 5C6E7D. */
    private static final String STYLES = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
            + "<numFmts count=\"2\"><numFmt numFmtId=\"164\" formatCode=\"dd/mm/yyyy\"/>"
            + "<numFmt numFmtId=\"165\" formatCode=\"hh:mm\"/></numFmts>"
            + "<fonts count=\"6\">"
            + "<font><sz val=\"11\"/><color rgb=\"FF2F4558\"/><name val=\"Calibri\"/><family val=\"2\"/></font>"
            + "<font><b/><sz val=\"11\"/><color rgb=\"FFFFFFFF\"/><name val=\"Calibri\"/><family val=\"2\"/></font>"
            + "<font><b/><sz val=\"16\"/><color rgb=\"FF335F8A\"/><name val=\"Calibri\"/><family val=\"2\"/></font>"
            + "<font><i/><sz val=\"10\"/><color rgb=\"FF5C6E7D\"/><name val=\"Calibri\"/><family val=\"2\"/></font>"
            + "<font><b/><sz val=\"11\"/><color rgb=\"FF2F4558\"/><name val=\"Calibri\"/><family val=\"2\"/></font>"
            + "<font><i/><sz val=\"11\"/><color rgb=\"FF7A8A98\"/><name val=\"Calibri\"/><family val=\"2\"/></font>"
            + "</fonts>"
            + "<fills count=\"5\"><fill><patternFill patternType=\"none\"/></fill><fill><patternFill patternType=\"gray125\"/></fill>"
            + "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FF335F8A\"/><bgColor indexed=\"64\"/></patternFill></fill>"
            + "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFD6E4F0\"/><bgColor indexed=\"64\"/></patternFill></fill>"
            + "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFF1F4F7\"/><bgColor indexed=\"64\"/></patternFill></fill>"
            + "</fills>"
            + "<borders count=\"2\"><border><left/><right/><top/><bottom/><diagonal/></border>"
            + "<border><left style=\"thin\"><color rgb=\"FFD4DEE7\"/></left><right style=\"thin\"><color rgb=\"FFD4DEE7\"/></right>"
            + "<top style=\"thin\"><color rgb=\"FFD4DEE7\"/></top><bottom style=\"thin\"><color rgb=\"FFD4DEE7\"/></bottom><diagonal/></border>"
            + "</borders>"
            + "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>"
            + "<cellXfs count=\"10\">"
            + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>"
            + "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"2\" borderId=\"1\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\" applyAlignment=\"1\"><alignment horizontal=\"center\" vertical=\"center\" wrapText=\"1\"/></xf>"
            + "<xf numFmtId=\"0\" fontId=\"4\" fillId=\"3\" borderId=\"1\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\" applyAlignment=\"1\"><alignment horizontal=\"center\" vertical=\"center\" wrapText=\"1\"/></xf>"
            + "<xf numFmtId=\"0\" fontId=\"2\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\" applyAlignment=\"1\"><alignment vertical=\"center\"/></xf>"
            + "<xf numFmtId=\"0\" fontId=\"3\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\" applyAlignment=\"1\"><alignment vertical=\"top\" wrapText=\"1\"/></xf>"
            + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"1\" xfId=\"0\" applyBorder=\"1\"/>"
            + "<xf numFmtId=\"164\" fontId=\"0\" fillId=\"0\" borderId=\"1\" xfId=\"0\" applyNumberFormat=\"1\" applyBorder=\"1\" applyAlignment=\"1\"><alignment horizontal=\"center\"/></xf>"
            + "<xf numFmtId=\"165\" fontId=\"0\" fillId=\"0\" borderId=\"1\" xfId=\"0\" applyNumberFormat=\"1\" applyBorder=\"1\" applyAlignment=\"1\"><alignment horizontal=\"center\"/></xf>"
            + "<xf numFmtId=\"1\" fontId=\"0\" fillId=\"0\" borderId=\"1\" xfId=\"0\" applyNumberFormat=\"1\" applyBorder=\"1\" applyAlignment=\"1\"><alignment horizontal=\"center\"/></xf>"
            + "<xf numFmtId=\"0\" fontId=\"5\" fillId=\"4\" borderId=\"1\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\"/>"
            + "</cellXfs>"
            + "<cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles>"
            + "</styleSheet>";
}
