package com.pingmanager.ui.infra;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static com.pingmanager.core.reglement.RenduPdfReglement.imprimable;
import static com.pingmanager.core.reglement.RenduPdfReglement.rgb;

/**
 * PDF simples de la buvette (B046) : un titre, un sous-titre, un tableau à
 * colonnes et des lignes de pied (liste de courses, bilan). Même police et même
 * manière que {@link RenduPdfFeuilles} ; PDFBox est déjà une dépendance du projet.
 */
public final class RenduPdfBuvette {

    private static final String POLICE_NORMALE = "/com/pingmanager/polices/Inter-Regular.ttf";
    private static final String POLICE_GRASSE = "/com/pingmanager/polices/Inter-SemiBold.ttf";
    private static final float[] TEXTE = rgb(0x0F1B3D);
    private static final float[] DOUX = rgb(0x5B6785);
    private static final float[] PRIMAIRE = rgb(0x1D3FCF);
    private static final float[] FILET = rgb(0xDCE3F0);
    private static final float MARGE = 40;
    private static final float HAUTEUR_LIGNE = 18;
    private static final float TAILLE = 9.5f;

    private RenduPdfBuvette() {
    }

    /**
     * @param largeurs largeurs relatives des colonnes (même nombre que l'en-tête)
     * @return le PDF (A4 portrait, plusieurs pages si besoin)
     */
    public static byte[] tableau(String titre, String sousTitre, List<String> entetes, List<List<String>> lignes,
                                 float[] largeurs, List<String> pied) throws IOException {
        if (entetes.size() != largeurs.length) {
            throw new IllegalArgumentException("une largeur par colonne");
        }
        try (PDDocument doc = new PDDocument()) {
            PDFont normale = charger(doc, POLICE_NORMALE);
            PDFont grasse = charger(doc, POLICE_GRASSE);
            doc.getDocumentInformation().setTitle(titre);
            doc.getDocumentInformation().setCreator("PingManager");
            float l = PDRectangle.A4.getWidth() - 2 * MARGE;
            float total = 0;
            for (float v : largeurs) {
                total += v;
            }
            float[] cols = new float[largeurs.length];
            for (int i = 0; i < largeurs.length; i++) {
                cols[i] = largeurs[i] / total * l;
            }
            int k = 0;
            boolean premiere = true;
            while (premiere || k < lignes.size()) {
                PDPage page = new PDPage(PDRectangle.A4);
                doc.addPage(page);
                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    float y = PDRectangle.A4.getHeight() - MARGE;
                    if (premiere) {
                        texte(cs, titre, MARGE, y - 16, grasse, 16, PRIMAIRE);
                        y -= 26;
                        texte(cs, sousTitre, MARGE, y - 10, normale, 10, DOUX);
                        y -= 26;
                        premiere = false;
                    }
                    y = ligne(cs, entetes, cols, y, grasse, TEXTE);
                    while (k < lignes.size() && y > MARGE + 80) {
                        y = ligne(cs, lignes.get(k++), cols, y, normale, TEXTE);
                    }
                    if (k >= lignes.size()) {
                        y -= 10;
                        for (String p : pied) {
                            texte(cs, p, MARGE, y - 10, normale, 9, DOUX);
                            y -= 14;
                        }
                    }
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        }
    }

    private static float ligne(PDPageContentStream cs, List<String> cellules, float[] cols, float y, PDFont f,
                               float[] couleur) throws IOException {
        float x = MARGE;
        for (int i = 0; i < cols.length; i++) {
            String c = i < cellules.size() ? cellules.get(i) : "";
            texte(cs, ajuster(c, f, cols[i] - 6), x, y - 12, f, TAILLE, couleur);
            x += cols[i];
        }
        cs.setStrokingColor(FILET[0], FILET[1], FILET[2]);
        cs.setLineWidth(0.6f);
        cs.moveTo(MARGE, y - HAUTEUR_LIGNE + 2);
        cs.lineTo(x, y - HAUTEUR_LIGNE + 2);
        cs.stroke();
        return y - HAUTEUR_LIGNE;
    }

    private static String ajuster(String s, PDFont f, float max) throws IOException {
        String t = imprimable(s, f);
        if (f.getStringWidth(t) / 1000f * TAILLE <= max) {
            return t;
        }
        while (!t.isEmpty() && f.getStringWidth(t + "…") / 1000f * TAILLE > max) {
            t = t.substring(0, t.length() - 1);
        }
        return t + "…";
    }

    private static void texte(PDPageContentStream cs, String s, float x, float y, PDFont f, float taille,
                              float[] couleur) throws IOException {
        String t = imprimable(s, f);
        if (t.isEmpty()) {
            return;
        }
        cs.beginText();
        cs.setFont(f, taille);
        cs.setNonStrokingColor(couleur[0], couleur[1], couleur[2]);
        cs.newLineAtOffset(x, y);
        cs.showText(t);
        cs.endText();
    }

    private static PDFont charger(PDDocument doc, String chemin) throws IOException {
        try (InputStream in = RenduPdfBuvette.class.getResourceAsStream(chemin)) {
            if (in == null) {
                throw new IOException("Police introuvable dans l'application : " + chemin);
            }
            return PDType0Font.load(doc, in);
        }
    }
}
