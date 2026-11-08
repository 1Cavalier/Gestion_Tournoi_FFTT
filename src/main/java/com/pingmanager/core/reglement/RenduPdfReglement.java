package com.pingmanager.core.reglement;

import com.pingmanager.model.DocumentReglement;

import com.pingmanager.model.DocumentReglement.Article;
import com.pingmanager.model.DocumentReglement.Bloc;
import com.pingmanager.model.DocumentReglement.Grille;
import com.pingmanager.model.DocumentReglement.Liste;
import com.pingmanager.model.DocumentReglement.Paragraphe;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

/**
 * Rendu PDF (A4) du règlement, avec PDFBox et la police Inter intégrée (elle
 * couvre « ≤ », « ½ », « € », les guillemets français…). Mise en page simple et
 * lisible : titre, articles, tableaux (en-tête répété en cas de saut de page),
 * trous « [à compléter : …] » surlignés, numéros de page. Pas de bandeau
 * « projet » ni de check-list : les manques se voient au surlignage.
 */
public final class RenduPdfReglement {

    private static final String POLICE_NORMALE = "/com/pingmanager/polices/Inter-Regular.ttf";
    private static final String POLICE_GRASSE = "/com/pingmanager/polices/Inter-SemiBold.ttf";

    private static final float LARGEUR_PAGE = PDRectangle.A4.getWidth();
    private static final float HAUTEUR_PAGE = PDRectangle.A4.getHeight();
    private static final float MARGE_X = 56;
    private static final float MARGE_HAUT = 58;
    private static final float MARGE_BAS = 62;
    private static final float LARGEUR_UTILE = LARGEUR_PAGE - 2 * MARGE_X;

    private static final float[] TEXTE = rgb(0x2F4558);
    private static final float[] DOUX = rgb(0x5C6E7D);
    private static final float[] PRIMAIRE = rgb(0x335F8A);
    private static final float[] FILET = rgb(0xD4DEE7);
    private static final float[] TROU = rgb(0xFCE7B6);

    private RenduPdfReglement() {
    }

    /** Écrit le PDF du règlement dans un fichier. */
    public static void ecrire(DocumentReglement d, Path cible) throws IOException {
        Files.write(cible, rendre(d));
    }

    /** @return le PDF du règlement (titres « Article 1 — Organisation »). */
    public static byte[] rendre(DocumentReglement d) throws IOException {
        return rendre(d, "Article %d — %s");
    }

    /** Écrit un document dans un fichier, avec un format de titre de section (ex. « %d. %s »). */
    public static void ecrire(DocumentReglement d, Path cible, String formatTitre) throws IOException {
        Files.write(cible, rendre(d, formatTitre));
    }

    /**
     * @param formatTitre format des titres de section : numéro puis titre
     *                    (« Article %d — %s » pour un règlement, « %d. %s » pour une fiche)
     * @return le PDF du document
     */
    public static byte[] rendre(DocumentReglement d, String formatTitre) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            Mise m = new Mise(doc);
            PDDocumentInformation info = doc.getDocumentInformation();
            info.setTitle(d.titre());
            info.setCreator("PingManager");

            m.texte(d.titre(), m.grasse, 20, TEXTE, 26, false);
            m.texte(d.sousTitre(), m.normale, 12, DOUX, 16, false);
            m.espace(10);
            for (Article a : d.articles()) {
                m.espace(8);
                // Titre + paragraphe d'introduction + début du tableau restent sur la même page.
                if (a.blocs().size() > 1 && a.blocs().get(0) instanceof Paragraphe p0
                        && a.blocs().get(1) instanceof Grille) {
                    m.assurer(19 + m.hauteurTexte(p0.texte(), 10.5f, 15.2f) + 110);
                }
                m.titreArticle(String.format(formatTitre, a.numero(), a.titre()));
                for (int i = 0; i < a.blocs().size(); i++) {
                    Bloc b = a.blocs().get(i);
                    if (b instanceof Paragraphe p) {
                        // Un paragraphe qui introduit un tableau reste avec son début (en-tête + 3 lignes).
                        if (i + 1 < a.blocs().size() && a.blocs().get(i + 1) instanceof Grille) {
                            m.assurer(m.hauteurTexte(p.texte(), 10.5f, 15.2f) + 110);
                        }
                        m.texte(p.texte(), m.normale, 10.5f, TEXTE, 15.2f, true);
                        m.espace(4);
                    } else if (b instanceof Grille g) {
                        m.grille(g);
                    } else if (b instanceof Liste l) {
                        for (String e : l.elements()) {
                            m.puce(e);
                        }
                        m.espace(4);
                    }
                }
            }
            // Ni bandeau « projet », ni check-list « à faire avant le dépôt » : seuls les
            // passages à compléter restent visibles, surlignés dans le texte.
            m.fermer();
            m.numeroter(d.titre());

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        }
    }

    // ==================================================================
    // Moteur de mise en page
    // ==================================================================

    /** Un mot (ou morceau de mot) à poser, avec son style. */
    private record Mot(String texte, boolean trou, boolean colle) {
        Mot(String texte, boolean trou) {
            this(texte, trou, false);
        }
    }

    private static final class Mise {
        final PDDocument doc;
        final PDFont normale;
        final PDFont grasse;
        PDPage page;
        PDPageContentStream cs;
        float y;

        Mise(PDDocument doc) throws IOException {
            this.doc = doc;
            this.normale = police(doc, POLICE_NORMALE);
            this.grasse = police(doc, POLICE_GRASSE);
            nouvellePage();
        }

        private static PDFont police(PDDocument doc, String chemin) throws IOException {
            try (InputStream in = RenduPdfReglement.class.getResourceAsStream(chemin)) {
                if (in == null) {
                    throw new IOException("Police introuvable dans l'application : " + chemin);
                }
                return PDType0Font.load(doc, in);
            }
        }

        void nouvellePage() throws IOException {
            fermer();
            page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            cs = new PDPageContentStream(doc, page);
            y = HAUTEUR_PAGE - MARGE_HAUT;
        }

        void fermer() throws IOException {
            if (cs != null) {
                cs.close();
                cs = null;
            }
        }

        void assurer(float hauteur) throws IOException {
            if (y - hauteur < MARGE_BAS) {
                nouvellePage();
            }
        }

        void espace(float h) {
            y -= h;
        }

        float largeur(String s, PDFont f, float taille) {
            try {
                return f.getStringWidth(imprimable(s, f)) / 1000f * taille;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        // --- Texte -------------------------------------------------------

        /** Paragraphe à la largeur utile ; {@code trous} : surligne les « [à compléter : …] ». */
        void texte(String s, PDFont f, float taille, float[] couleur, float interligne, boolean trous) throws IOException {
            for (List<Mot> ligne : couper(mots(s, trous), f, taille, LARGEUR_UTILE)) {
                assurer(interligne);
                poserLigne(ligne, MARGE_X, y - taille, f, taille, couleur);
                y -= interligne;
            }
        }

        /** Hauteur d'un paragraphe une fois coupé à la largeur utile. */
        float hauteurTexte(String s, float taille, float interligne) {
            return couper(mots(s, true), normale, taille, LARGEUR_UTILE).size() * interligne;
        }

        void titreArticle(String s) throws IOException {
            assurer(40); // pas de titre seul en bas de page
            texte(s, grasse, 12, PRIMAIRE, 17, false);
            espace(2);
        }

        void puce(String s) throws IOException {
            float taille = 10.5f;
            List<List<Mot>> lignes = couper(mots(s, true), normale, taille, LARGEUR_UTILE - 14);
            for (int i = 0; i < lignes.size(); i++) {
                assurer(15);
                if (i == 0 && !s.startsWith("□")) { // case à cocher : pas de puce en plus
                    poserLigne(List.of(new Mot("•", false)), MARGE_X + 2, y - taille, normale, taille, TEXTE);
                }
                poserLigne(lignes.get(i), MARGE_X + 14, y - taille, normale, taille, TEXTE);
                y -= 15;
            }
        }

        /** Découpe en mots ; les mots d'un trou sont marqués pour être surlignés. */
        static List<Mot> mots(String s, boolean trous) {
            List<Mot> res = new ArrayList<>();
            Matcher m = DocumentReglement.A_COMPLETER.matcher(s);
            int i = 0;
            while (trous && m.find()) {
                ajouterMots(res, s.substring(i, m.start()), false, i > 0 && !finitParEspace(s, i));
                ajouterMots(res, m.group(), true, m.start() > 0 && !finitParEspace(s, m.start()));
                i = m.end();
            }
            ajouterMots(res, s.substring(i), false, i > 0 && !finitParEspace(s, i));
            return res;
        }

        private static boolean finitParEspace(String s, int fin) {
            return Character.isWhitespace(s.charAt(fin - 1));
        }

        /**
         * @param colleAuPrecedent le morceau suit le précédent sans espace (ex. « ] » puis « . ») :
         *                         son premier mot est alors collé au mot précédent
         */
        static void ajouterMots(List<Mot> res, String s, boolean trou, boolean colleAuPrecedent) {
            String p = propre(s);
            boolean premier = true;
            for (String w : p.split(" ")) {
                if (!w.isEmpty()) {
                    boolean colle = premier && colleAuPrecedent && !res.isEmpty() && !p.startsWith(" ");
                    res.add(new Mot(w, trou, colle));
                }
                premier = false;
            }
        }

        List<List<Mot>> couper(List<Mot> mots, PDFont f, float taille, float largeurMax) {
            List<List<Mot>> lignes = new ArrayList<>();
            List<Mot> courante = new ArrayList<>();
            float l = 0;
            float espace = largeur(" ", f, taille);
            for (Mot m : mots) {
                float w = largeur(m.texte(), f, taille);
                float sep = m.colle() ? 0 : espace;
                if (!courante.isEmpty() && !m.colle() && l + sep + w > largeurMax) {
                    lignes.add(courante);
                    courante = new ArrayList<>();
                    l = 0;
                }
                l += (courante.isEmpty() ? 0 : sep) + w;
                courante.add(m);
            }
            if (!courante.isEmpty()) {
                lignes.add(courante);
            }
            if (lignes.isEmpty()) {
                lignes.add(List.of());
            }
            return lignes;
        }

        void poserLigne(List<Mot> ligne, float x, float base, PDFont f, float taille, float[] couleur) throws IOException {
            float espace = largeur(" ", f, taille);
            float xx = x;
            // Fonds surlignés d'abord (un seul rectangle par suite de mots surlignés).
            for (int i = 0; i < ligne.size(); i++) {
                Mot m = ligne.get(i);
                float w = largeur(m.texte(), f, taille);
                if (i > 0 && !m.colle()) {
                    xx += espace;
                }
                if (m.trou()) {
                    boolean suivantTrou = i + 1 < ligne.size() && ligne.get(i + 1).trou();
                    couleur(TROU, true);
                    cs.addRect(xx - 1.5f, base - 2.8f, w + (suivantTrou ? espace : 0) + 3, taille + 4.2f);
                    cs.fill();
                }
                xx += w;
            }
            cs.beginText();
            cs.setFont(f, taille);
            couleur(couleur, true);
            cs.newLineAtOffset(x, base);
            StringBuilder sb = new StringBuilder();
            for (Mot m : ligne) {
                if (sb.length() > 0 && !m.colle()) {
                    sb.append(' ');
                }
                sb.append(m.texte());
            }
            cs.showText(imprimable(sb.toString(), f));
            cs.endText();
        }

        // --- Tableaux ----------------------------------------------------

        void grille(Grille g) throws IOException {
            float taille = 9.5f;
            float interligne = 13;
            int n = g.entetes().size();
            float[] naturelle = new float[n];
            for (int c = 0; c < n; c++) {
                naturelle[c] = largeur(g.entetes().get(c), grasse, taille);
                for (List<String> l : g.lignes()) {
                    naturelle[c] = Math.max(naturelle[c], largeur(l.get(c), normale, taille));
                }
                naturelle[c] += 10;
            }
            // Largeur minimale d'une colonne : son mot le plus long (un mot ne se coupe pas).
            float[] minimum = new float[n];
            for (int c = 0; c < n; c++) {
                minimum[c] = motLePlusLong(g.entetes().get(c), grasse, taille);
                for (List<String> l : g.lignes()) {
                    minimum[c] = Math.max(minimum[c], motLePlusLong(l.get(c), c == 0 ? grasse : normale, taille));
                }
                minimum[c] = Math.min(minimum[c] + 10, naturelle[c]);
            }
            float total = 0;
            float totalMin = 0;
            for (int c = 0; c < n; c++) {
                total += naturelle[c];
                totalMin += minimum[c];
            }
            float[] col = new float[n];
            for (int c = 0; c < n; c++) {
                if (total <= LARGEUR_UTILE) {
                    col[c] = naturelle[c] * (LARGEUR_UTILE / total);          // tout tient : on étire
                } else if (totalMin < LARGEUR_UTILE) {
                    // chaque colonne garde son minimum ; le reste est partagé selon ce qui dépasse
                    col[c] = minimum[c] + (naturelle[c] - minimum[c]) * ((LARGEUR_UTILE - totalMin) / (total - totalMin));
                } else {
                    col[c] = minimum[c] * (LARGEUR_UTILE / totalMin);
                }
            }
            espace(2);
            enteteGrille(g, col, taille, interligne);
            for (List<String> ligne : g.lignes()) {
                List<List<List<Mot>>> cellules = new ArrayList<>();
                int nbLignes = 1;
                for (int c = 0; c < n; c++) {
                    List<List<Mot>> cl = couper(mots(ligne.get(c), true), c == 0 ? grasse : normale, taille, col[c] - 8);
                    cellules.add(cl);
                    nbLignes = Math.max(nbLignes, cl.size());
                }
                float h = nbLignes * interligne + 8;
                if (ligne.stream().anyMatch(String::isBlank)) {
                    h = Math.max(h, 24); // ligne à remplir à la main : un peu plus de hauteur
                }
                if (y - h < MARGE_BAS) {
                    nouvellePage();
                    enteteGrille(g, col, taille, interligne);
                }
                float x = MARGE_X;
                for (int c = 0; c < n; c++) {
                    float yy = y - 4;
                    for (List<Mot> l : cellules.get(c)) {
                        poserLigne(l, x, yy - taille, c == 0 ? grasse : normale, taille, TEXTE);
                        yy -= interligne;
                    }
                    x += col[c];
                }
                y -= h;
                filet(y);
            }
            if (g.note() != null) {
                espace(5);
                texte(g.note(), normale, 9, DOUX, 12.5f, true);
            }
            espace(6);
        }

        float motLePlusLong(String s, PDFont f, float taille) {
            float max = 0;
            for (String w : propre(s).split(" ")) {
                max = Math.max(max, largeur(w, f, taille));
            }
            return max;
        }

        void enteteGrille(Grille g, float[] col, float taille, float interligne) throws IOException {
            List<List<List<Mot>>> cellules = new ArrayList<>();
            int nb = 1;
            for (int c = 0; c < col.length; c++) {
                List<List<Mot>> cl = couper(mots(g.entetes().get(c), false), grasse, taille, col[c] - 8);
                cellules.add(cl);
                nb = Math.max(nb, cl.size());
            }
            float h = nb * interligne + 6;
            assurer(h + 30);
            float x = MARGE_X;
            for (int c = 0; c < col.length; c++) {
                float yy = y;
                for (List<Mot> l : cellules.get(c)) {
                    poserLigne(l, x, yy - taille, grasse, taille, DOUX);
                    yy -= interligne;
                }
                x += col[c];
            }
            y -= h;
            filet(y);
        }

        void filet(float yy) throws IOException {
            couleur(FILET, false);
            cs.setLineWidth(0.7f);
            cs.moveTo(MARGE_X, yy);
            cs.lineTo(MARGE_X + LARGEUR_UTILE, yy);
            cs.stroke();
        }

        // --- Pied de page ------------------------------------------------

        void numeroter(String titre) throws IOException {
            int total = doc.getNumberOfPages();
            for (int i = 0; i < total; i++) {
                PDPage p = doc.getPage(i);
                try (PDPageContentStream c = new PDPageContentStream(doc, p, PDPageContentStream.AppendMode.APPEND, true, true)) {
                    String texte = imprimable(titre + "   ·   page " + (i + 1) + " / " + total, normale);
                    float taille = 8.5f;
                    float w = normale.getStringWidth(texte) / 1000f * taille;
                    c.beginText();
                    c.setFont(normale, taille);
                    c.setNonStrokingColor(DOUX[0], DOUX[1], DOUX[2]);
                    c.newLineAtOffset((LARGEUR_PAGE - w) / 2, MARGE_BAS / 2 - 4);
                    c.showText(texte);
                    c.endText();
                }
            }
        }

        void couleur(float[] c, boolean remplissage) throws IOException {
            if (remplissage) {
                cs.setNonStrokingColor(c[0], c[1], c[2]);
            } else {
                cs.setStrokingColor(c[0], c[1], c[2]);
            }
        }
    }

    /** Retire ce qu'une police ne sait pas poser (sauts de ligne, tabulations, espaces fines). */
    static String propre(String s) {
        return s == null ? "" : s.replace('\n', ' ').replace('\r', ' ').replace('\t', ' ')
                .replace(' ', ' ').replace(' ', ' ');
    }

    /**
     * Texte nettoyé où chaque caractère que la police ne sait pas encoder (CJK, emoji…)
     * devient « ? » : un titre ou un nom exotique ne fait plus échouer tout le PDF.
     * Même règle que {@code ui.infra.RenduPdfFeuilles.imprimable} (le cœur ne peut pas
     * dépendre de {@code ui}). Sert à la mesure comme au dessin.
     */
    static String imprimable(String s, PDFont f) {
        String t = propre(s);
        StringBuilder sb = null;
        for (int i = 0; i < t.length(); ) {
            int cp = t.codePointAt(i);
            int n = Character.charCount(cp);
            boolean ok;
            try {
                f.encode(t.substring(i, i + n));
                ok = true;
            } catch (IllegalArgumentException | IOException e) {
                ok = false;
            }
            if (!ok && sb == null) {
                sb = new StringBuilder(t.length()).append(t, 0, i);
            }
            if (sb != null) {
                sb.append(ok ? t.substring(i, i + n) : "?");
            }
            i += n;
        }
        return sb == null ? t : sb.toString();
    }

    private static float[] rgb(int hex) {
        return new float[] {((hex >> 16) & 0xFF) / 255f, ((hex >> 8) & 0xFF) / 255f, (hex & 0xFF) / 255f};
    }
}
