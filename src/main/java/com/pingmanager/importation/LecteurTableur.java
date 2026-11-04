package com.pingmanager.importation;

import com.pingmanager.persistence.json.Json;
import com.pingmanager.persistence.json.JsonParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Lit la <b>première feuille</b> d'un fichier de joueurs en cellules texte, sans
 * bibliothèque externe (même choix que le JSON maison de la persistance) :
 * <ul>
 *   <li><b>Excel</b> .xlsx / .xlsm (archive zip + XML : classeur, chaînes partagées, feuille) ;</li>
 *   <li><b>LibreOffice / OpenOffice</b> .ods (archive zip + {@code content.xml}) ;</li>
 *   <li><b>CSV / texte</b> .csv / .txt (séparateur « ; », « , » ou tabulation détecté ; UTF-8,
 *       avec ou sans BOM, sinon Windows-1252 — l'enregistrement CSV par défaut d'Excel en France) ;</li>
 *   <li><b>JSON</b> .json : tableau d'objets, ou objet avec une liste « joueurs » ; les clés
 *       deviennent les en-têtes, une liste de tableaux est jointe par « ; ».</li>
 * </ul>
 * L'ancien format Excel 97 (.xls, binaire) n'est pas lu : message clair pour l'enregistrer
 * en .xlsx ou en .csv.
 */
public final class LecteurTableur {

    /** Format reconnu, pour l'affichage. */
    public enum Format {
        XLSX("Excel (.xlsx)"), ODS("LibreOffice (.ods)"), CSV("CSV"), JSON("JSON");

        private final String libelle;

        Format(String libelle) {
            this.libelle = libelle;
        }

        public String getLibelle() {
            return libelle;
        }
    }

    /** Contenu lu : format et lignes de cellules. */
    public record Contenu(Format format, List<List<String>> lignes) {
    }

    /** Classeur complet : format et feuilles par nom, dans l'ordre (une seule pour CSV). */
    public record Classeur(Format format, LinkedHashMap<String, List<List<String>>> feuilles) {

        /** Feuille dont le nom contient ce mot (sans accents ni casse), ou {@code null}. */
        public List<List<String>> feuille(String mot) {
            String m = normaliser(mot);
            for (Map.Entry<String, List<List<String>>> e : feuilles.entrySet()) {
                if (normaliser(e.getKey()).contains(m)) {
                    return e.getValue();
                }
            }
            return null;
        }

        /** Nom réel de la feuille contenant ce mot, ou {@code null}. */
        public String nomFeuille(String mot) {
            String m = normaliser(mot);
            for (String k : feuilles.keySet()) {
                if (normaliser(k).contains(m)) {
                    return k;
                }
            }
            return null;
        }
    }

    public static Classeur lireClasseur(Path fichier) throws ImportException {
        try {
            return lireClasseur(fichier.getFileName().toString(), Files.readAllBytes(fichier));
        } catch (IOException e) {
            throw new ImportException("Lecture impossible : " + e.getMessage());
        }
    }

    /**
     * Lit toutes les feuilles. JSON : un objet {@code {"tableaux": [...], "joueurs": [...]}}
     * donne deux feuilles « Tableaux » et « Joueurs ».
     */
    public static Classeur lireClasseur(String nom, byte[] octets) throws ImportException {
        String n = nom.toLowerCase(Locale.ROOT);
        if (n.endsWith(".xlsx") || n.endsWith(".xlsm")) {
            return new Classeur(Format.XLSX, xlsxClasseur(octets));
        }
        if (n.endsWith(".ods")) {
            return new Classeur(Format.ODS, odsClasseur(octets));
        }
        if (n.endsWith(".json")) {
            String texte = texte(octets);
            LinkedHashMap<String, List<List<String>>> f = new LinkedHashMap<>();
            Json racine;
            try {
                racine = JsonParser.analyser(texte);
            } catch (RuntimeException e) {
                throw new ImportException("JSON illisible : " + e.getMessage());
            }
            if (racine.estObjet() && racine.has("tableaux") && racine.get("tableaux").estTableau()) {
                f.put("Tableaux", objets(racine.get("tableaux")));
                if (racine.has("joueurs")) {
                    f.put("Joueurs", json(texte));
                }
                return new Classeur(Format.JSON, f);
            }
            f.put("Joueurs", json(texte));
            return new Classeur(Format.JSON, f);
        }
        Contenu c = lire(nom, octets);
        LinkedHashMap<String, List<List<String>>> f = new LinkedHashMap<>();
        f.put("Feuille 1", c.lignes());
        return new Classeur(c.format(), f);
    }

    private static String normaliser(String s) {
        return java.text.Normalizer.normalize(s == null ? "" : s, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    /** Fichier illisible : le message est destiné à l'utilisateur. */
    public static final class ImportException extends Exception {
        public ImportException(String message) {
            super(message);
        }
    }

    private static final int MAX_LIGNES = 20_000;
    private static final int MAX_COLONNES = 200;

    private LecteurTableur() {
    }

    public static Contenu lire(Path fichier) throws ImportException {
        String nom = fichier.getFileName().toString().toLowerCase(Locale.ROOT);
        byte[] octets;
        try {
            octets = Files.readAllBytes(fichier);
        } catch (IOException e) {
            throw new ImportException("Lecture impossible : " + e.getMessage());
        }
        return lire(nom, octets);
    }

    /** @param nom nom du fichier (l'extension choisit le format) */
    public static Contenu lire(String nom, byte[] octets) throws ImportException {
        String n = nom.toLowerCase(Locale.ROOT);
        if (n.endsWith(".xls")) {
            throw new ImportException("Le format Excel 97-2003 (.xls) n'est pas pris en charge : "
                    + "dans Excel, « Enregistrer sous » → « Classeur Excel (.xlsx) » ou « CSV (séparateur : point-virgule) ».");
        }
        if (n.endsWith(".xlsx") || n.endsWith(".xlsm")) {
            return new Contenu(Format.XLSX, xlsx(octets));
        }
        if (n.endsWith(".ods")) {
            return new Contenu(Format.ODS, ods(octets));
        }
        if (n.endsWith(".json")) {
            return new Contenu(Format.JSON, json(texte(octets)));
        }
        if (n.endsWith(".csv") || n.endsWith(".txt") || n.endsWith(".tsv")) {
            return new Contenu(Format.CSV, csv(texte(octets)));
        }
        throw new ImportException("Format non reconnu : utilisez un fichier .xlsx, .ods, .csv ou .json.");
    }

    // ------------------------------------------------------------------
    // CSV
    // ------------------------------------------------------------------

    /** UTF-8 (BOM retiré) si le texte est valide, sinon Windows-1252. */
    static String texte(byte[] o) {
        int debut = o.length >= 3 && (o[0] & 0xFF) == 0xEF && (o[1] & 0xFF) == 0xBB && (o[2] & 0xFF) == 0xBF ? 3 : 0;
        ByteBuffer b = ByteBuffer.wrap(o, debut, o.length - debut);
        try {
            return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(b).toString();
        } catch (CharacterCodingException e) {
            return new String(o, debut, o.length - debut, Charset.forName("windows-1252"));
        }
    }

    static List<List<String>> csv(String texte) {
        String premiere = texte.lines().filter(l -> !l.isBlank()).findFirst().orElse("");
        char sep = ';';
        int pv = compter(premiere, ';');
        int v = compter(premiere, ',');
        int tab = compter(premiere, '\t');
        if (tab > pv && tab >= v) {
            sep = '\t';
        } else if (v > pv) {
            sep = ',';
        }
        List<List<String>> res = new ArrayList<>();
        List<String> ligne = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean guillemets = false;
        for (int i = 0; i < texte.length(); i++) {
            char c = texte.charAt(i);
            if (guillemets) {
                if (c == '"') {
                    if (i + 1 < texte.length() && texte.charAt(i + 1) == '"') {
                        cell.append('"');
                        i++;
                    } else {
                        guillemets = false;
                    }
                } else {
                    cell.append(c);
                }
            } else if (c == '"' && cell.length() == 0) {
                guillemets = true;
            } else if (c == sep) {
                ligne.add(cell.toString());
                cell.setLength(0);
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < texte.length() && texte.charAt(i + 1) == '\n') {
                    i++;
                }
                ligne.add(cell.toString());
                cell.setLength(0);
                res.add(ligne);
                ligne = new ArrayList<>();
                if (res.size() > MAX_LIGNES) {
                    break;
                }
            } else {
                cell.append(c);
            }
        }
        if (cell.length() > 0 || !ligne.isEmpty()) {
            ligne.add(cell.toString());
            res.add(ligne);
        }
        return res;
    }

    private static int compter(String s, char c) {
        int n = 0;
        boolean g = false;
        for (char x : s.toCharArray()) {
            if (x == '"') {
                g = !g;
            } else if (x == c && !g) {
                n++;
            }
        }
        return n;
    }

    // ------------------------------------------------------------------
    // JSON
    // ------------------------------------------------------------------

    static List<List<String>> json(String texte) throws ImportException {
        Json racine;
        try {
            racine = JsonParser.analyser(texte);
        } catch (RuntimeException e) {
            throw new ImportException("JSON illisible : " + e.getMessage());
        }
        Json liste = racine;
        if (racine.estObjet()) {
            liste = null;
            for (String cle : List.of("joueurs", "players", "inscrits", "lignes")) {
                if (racine.has(cle) && racine.get(cle).estTableau()) {
                    liste = racine.get(cle);
                    break;
                }
            }
        }
        if (liste == null || !liste.estTableau()) {
            throw new ImportException("JSON attendu : une liste d'objets joueur, ou un objet avec une liste « joueurs ».");
        }
        return objets(liste);
    }

    /** Liste d'objets JSON → lignes (en-tête = union des clés). */
    private static List<List<String>> objets(Json liste) {
        List<String> entetes = new ArrayList<>();
        for (Json o : liste.elements()) {
            if (o.estObjet()) {
                for (String k : o.entrees().keySet()) {
                    if (!entetes.contains(k)) {
                        entetes.add(k);
                    }
                }
            }
        }
        List<List<String>> res = new ArrayList<>();
        res.add(entetes);
        for (Json o : liste.elements()) {
            if (!o.estObjet()) {
                continue;
            }
            List<String> l = new ArrayList<>();
            for (String k : entetes) {
                Json v = o.get(k);
                l.add(enTexte(v));
            }
            res.add(l);
        }
        return res;
    }

    private static String enTexte(Json v) {
        if (v == null || v.estNul()) {
            return "";
        }
        if (v.estTableau()) {
            List<String> p = new ArrayList<>();
            v.elements().forEach(x -> p.add(enTexte(x)));
            return String.join(";", p);
        }
        if (v.getType() == Json.Type.NOMBRE) {
            double d = v.asDouble();
            return d == Math.rint(d) ? String.valueOf((long) d) : String.valueOf(d);
        }
        if (v.getType() == Json.Type.BOOLEEN) {
            return String.valueOf(v.asBoolean());
        }
        return v.asString();
    }

    // ------------------------------------------------------------------
    // XLSX
    // ------------------------------------------------------------------

    static List<List<String>> xlsx(byte[] octets) throws ImportException {
        return xlsxClasseur(octets).values().iterator().next();
    }

    /** Toutes les feuilles, dans l'ordre du classeur, par nom. */
    static LinkedHashMap<String, List<List<String>>> xlsxClasseur(byte[] octets) throws ImportException {
        Map<String, byte[]> zip = dezipper(octets, "xl/");
        if (!zip.containsKey("xl/workbook.xml")) {
            throw new ImportException("Fichier Excel illisible (classeur absent).");
        }
        // Chaînes partagées.
        List<String> partagees = new ArrayList<>();
        if (zip.containsKey("xl/sharedStrings.xml")) {
            Document d = xml(zip.get("xl/sharedStrings.xml"));
            NodeList si = d.getElementsByTagNameNS("*", "si");
            for (int i = 0; i < si.getLength(); i++) {
                partagees.add(textes((Element) si.item(i)));
            }
        }
        // Feuilles : workbook.xml (nom, r:id) → rels → fichier de chaque feuille.
        LinkedHashMap<String, String> cibles = new LinkedHashMap<>();
        Document wb = xml(zip.get("xl/workbook.xml"));
        NodeList feuilles = wb.getElementsByTagNameNS("*", "sheet");
        Map<String, String> relations = new LinkedHashMap<>();
        if (zip.containsKey("xl/_rels/workbook.xml.rels")) {
            NodeList rl = xml(zip.get("xl/_rels/workbook.xml.rels")).getElementsByTagNameNS("*", "Relationship");
            for (int i = 0; i < rl.getLength(); i++) {
                Element r = (Element) rl.item(i);
                String t = r.getAttribute("Target");
                relations.put(r.getAttribute("Id"), t.startsWith("/") ? t.substring(1) : "xl/" + t);
            }
        }
        for (int i = 0; i < feuilles.getLength(); i++) {
            Element f = (Element) feuilles.item(i);
            String rid = f.getAttributeNS("http://schemas.openxmlformats.org/officeDocument/2006/relationships", "id");
            String cible = relations.getOrDefault(rid, "xl/worksheets/sheet" + (i + 1) + ".xml");
            if (zip.containsKey(cible)) {
                cibles.put(f.getAttribute("name").isEmpty() ? "Feuille " + (i + 1) : f.getAttribute("name"), cible);
            }
        }
        if (cibles.isEmpty() && zip.containsKey("xl/worksheets/sheet1.xml")) {
            cibles.put("Feuille 1", "xl/worksheets/sheet1.xml");
        }
        if (cibles.isEmpty()) {
            throw new ImportException("Fichier Excel illisible (feuille introuvable).");
        }
        LinkedHashMap<String, List<List<String>>> classeur = new LinkedHashMap<>();
        for (Map.Entry<String, String> fc : cibles.entrySet()) {
            classeur.put(fc.getKey(), feuilleXlsx(xml(zip.get(fc.getValue())), partagees));
        }
        return classeur;
    }

    private static List<List<String>> feuilleXlsx(Document feuille, List<String> partagees) {
        List<List<String>> res = new ArrayList<>();
        NodeList lignes = feuille.getElementsByTagNameNS("*", "row");
        for (int i = 0; i < lignes.getLength() && i < MAX_LIGNES; i++) {
            Element row = (Element) lignes.item(i);
            int numero = row.hasAttribute("r") ? Integer.parseInt(row.getAttribute("r")) : res.size() + 1;
            while (res.size() < numero - 1) {
                res.add(new ArrayList<>());
            }
            List<String> l = new ArrayList<>();
            NodeList cells = row.getElementsByTagNameNS("*", "c");
            for (int k = 0; k < cells.getLength(); k++) {
                Element c = (Element) cells.item(k);
                int col = c.hasAttribute("r") ? colonne(c.getAttribute("r")) : l.size();
                if (col >= MAX_COLONNES) {
                    continue;
                }
                while (l.size() < col) {
                    l.add("");
                }
                l.add(valeurXlsx(c, partagees));
            }
            res.add(l);
        }
        return res;
    }

    private static String valeurXlsx(Element c, List<String> partagees) {
        String type = c.getAttribute("t");
        if ("inlineStr".equals(type)) {
            NodeList is = c.getElementsByTagNameNS("*", "is");
            return is.getLength() == 0 ? "" : textes((Element) is.item(0));
        }
        NodeList v = c.getElementsByTagNameNS("*", "v");
        String brut = v.getLength() == 0 ? "" : v.item(0).getTextContent();
        if ("s".equals(type)) {
            try {
                return partagees.get(Integer.parseInt(brut.trim()));
            } catch (RuntimeException e) {
                return "";
            }
        }
        if ("b".equals(type)) {
            return "1".equals(brut) ? "VRAI" : "FAUX";
        }
        if (type.isEmpty() || "n".equals(type)) {
            try {
                double d = Double.parseDouble(brut);
                return d == Math.rint(d) && Math.abs(d) < 1e15 ? String.valueOf((long) d) : brut;
            } catch (NumberFormatException e) {
                return brut;
            }
        }
        return brut;
    }

    /** « AB12 » → 27 (colonnes numérotées à partir de 0). */
    static int colonne(String ref) {
        int n = 0;
        for (char ch : ref.toCharArray()) {
            if (Character.isLetter(ch)) {
                n = n * 26 + (Character.toUpperCase(ch) - 'A' + 1);
            } else {
                break;
            }
        }
        return n - 1;
    }

    /** Concatène les nœuds texte {@code <t>} (texte enrichi : plusieurs runs). */
    private static String textes(Element e) {
        NodeList t = e.getElementsByTagNameNS("*", "t");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < t.getLength(); i++) {
            // Les annotations phonétiques (rPh) ne font pas partie de la valeur.
            if (!"rPh".equals(t.item(i).getParentNode().getLocalName())) {
                sb.append(t.item(i).getTextContent());
            }
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------
    // ODS
    // ------------------------------------------------------------------

    private static final String NS_TABLE = "urn:oasis:names:tc:opendocument:xmlns:table:1.0";
    private static final String NS_OFFICE = "urn:oasis:names:tc:opendocument:xmlns:office:1.0";

    static List<List<String>> ods(byte[] octets) throws ImportException {
        return odsClasseur(octets).values().iterator().next();
    }

    static LinkedHashMap<String, List<List<String>>> odsClasseur(byte[] octets) throws ImportException {
        Map<String, byte[]> zip = dezipper(octets, "content.xml");
        if (!zip.containsKey("content.xml")) {
            throw new ImportException("Fichier LibreOffice illisible (contenu absent).");
        }
        Document d = xml(zip.get("content.xml"));
        NodeList tables = d.getElementsByTagNameNS(NS_TABLE, "table");
        if (tables.getLength() == 0) {
            throw new ImportException("Aucune feuille dans le fichier LibreOffice.");
        }
        LinkedHashMap<String, List<List<String>>> classeur = new LinkedHashMap<>();
        for (int t = 0; t < tables.getLength(); t++) {
            Element table = (Element) tables.item(t);
            String nom = table.getAttributeNS(NS_TABLE, "name");
            classeur.put(nom.isEmpty() ? "Feuille " + (t + 1) : nom, feuilleOds(table));
        }
        return classeur;
    }

    private static List<List<String>> feuilleOds(Element table) {
        List<List<String>> res = new ArrayList<>();
        NodeList rows = table.getElementsByTagNameNS(NS_TABLE, "table-row");
        for (int i = 0; i < rows.getLength() && res.size() < MAX_LIGNES; i++) {
            Element row = (Element) rows.item(i);
            List<String> l = new ArrayList<>();
            for (Node n = row.getFirstChild(); n != null; n = n.getNextSibling()) {
                if (!(n instanceof Element c) || !NS_TABLE.equals(c.getNamespaceURI())
                        || !(c.getLocalName().equals("table-cell") || c.getLocalName().equals("covered-table-cell"))) {
                    continue;
                }
                int rep = c.hasAttributeNS(NS_TABLE, "number-columns-repeated")
                        ? Integer.parseInt(c.getAttributeNS(NS_TABLE, "number-columns-repeated")) : 1;
                String v = valeurOds(c);
                for (int k = 0; k < rep && l.size() < MAX_COLONNES; k++) {
                    l.add(v);
                }
            }
            while (!l.isEmpty() && l.get(l.size() - 1).isEmpty()) {
                l.remove(l.size() - 1);
            }
            int repLignes = row.hasAttributeNS(NS_TABLE, "number-rows-repeated")
                    ? Integer.parseInt(row.getAttributeNS(NS_TABLE, "number-rows-repeated")) : 1;
            if (l.isEmpty()) {
                // Lignes vides répétées (souvent ~1 million en fin de feuille) : une seule suffit.
                res.add(l);
            } else {
                for (int k = 0; k < Math.min(repLignes, MAX_LIGNES); k++) {
                    res.add(new ArrayList<>(l));
                }
            }
        }
        while (!res.isEmpty() && res.get(res.size() - 1).isEmpty()) {
            res.remove(res.size() - 1);
        }
        return res;
    }

    private static String valeurOds(Element c) {
        String type = c.getAttributeNS(NS_OFFICE, "value-type");
        if ("float".equals(type) || "percentage".equals(type) || "currency".equals(type)) {
            String v = c.getAttributeNS(NS_OFFICE, "value");
            try {
                double d = Double.parseDouble(v);
                return d == Math.rint(d) ? String.valueOf((long) d) : v;
            } catch (NumberFormatException e) {
                return v;
            }
        }
        if ("date".equals(type)) {
            return c.getAttributeNS(NS_OFFICE, "date-value");
        }
        StringBuilder sb = new StringBuilder();
        NodeList p = c.getElementsByTagNameNS("*", "p");
        for (int i = 0; i < p.getLength(); i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(p.item(i).getTextContent());
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------
    // Outils zip / XML
    // ------------------------------------------------------------------

    private static Map<String, byte[]> dezipper(byte[] octets, String prefixe) throws ImportException {
        Map<String, byte[]> res = new LinkedHashMap<>();
        try (ZipInputStream z = new ZipInputStream(new ByteArrayInputStream(octets))) {
            ZipEntry e;
            long total = 0;
            while ((e = z.getNextEntry()) != null) {
                if (e.getName().startsWith(prefixe) && !e.isDirectory()) {
                    byte[] b = z.readAllBytes();
                    total += b.length;
                    if (total > 200L * 1024 * 1024) {
                        throw new ImportException("Fichier trop volumineux.");
                    }
                    res.put(e.getName(), b);
                }
            }
        } catch (IOException e) {
            throw new ImportException("Archive illisible : le fichier est peut-être endommagé ou n'est pas un tableur.");
        }
        if (res.isEmpty()) {
            throw new ImportException("Ce fichier n'est pas un tableur reconnu.");
        }
        return res;
    }

    private static Document xml(byte[] octets) throws ImportException {
        try {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            f.setNamespaceAware(true);
            // Sécurité : aucune entité externe ni DTD (fichiers venant de l'extérieur).
            f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            f.setFeature("http://xml.org/sax/features/external-general-entities", false);
            f.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            f.setXIncludeAware(false);
            f.setExpandEntityReferences(false);
            DocumentBuilder b = f.newDocumentBuilder();
            return b.parse(new ByteArrayInputStream(octets));
        } catch (Exception e) {
            throw new ImportException("Contenu XML illisible : " + e.getMessage());
        }
    }
}
