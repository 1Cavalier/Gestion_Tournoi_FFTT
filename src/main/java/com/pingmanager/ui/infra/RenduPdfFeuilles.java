package com.pingmanager.ui.infra;

import com.pingmanager.core.feuilles.ArbreImprimable;
import com.pingmanager.core.feuilles.ArbreImprimable.CaseArbre;
import com.pingmanager.core.feuilles.ArbreImprimable.ColonneArbre;
import com.pingmanager.core.feuilles.ArbreImprimable.TypeCase;
import com.pingmanager.core.feuilles.FeuilleMatch;
import com.pingmanager.core.feuilles.FeuillePoule;
import com.pingmanager.core.feuilles.FeuillePoule.PartiePoule;
import com.pingmanager.core.feuilles.JoueurFeuille;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Rendu PDF (PDFBox, police Inter intégrée) des feuilles du jour J préparées
 * par le cœur ({@code core.feuilles.GenerateurFeuilles}) : feuilles de match
 * (deux par page A4, à découper), feuille de poule suivie de la feuille de
 * rencontre, tableau final (A4 paysage, A3 paysage au-delà de 16 places).
 *
 * <p><b>Pourquoi ici et pas dans {@code core.reglement}</b> : le cœur ne doit
 * pas connaître PDFBox ; {@code RenduPdfReglement} est une exception connue à
 * ne pas étendre (CLAUDE.md, test {@code RegleOrModeleSansDependanceTest}).
 * Le contenu des feuilles, qui porte les règles (I.301, I.303, grille 3 × 5),
 * reste dans le cœur et y est testé ; ce fichier ne fait que dessiner, comme
 * un adaptateur d'infrastructure (même rôle que l'envoi de courriels ou le
 * géocodage dans {@code ui.infra}).</p>
 */
public final class RenduPdfFeuilles {

    private static final String POLICE_NORMALE = "/com/pingmanager/polices/Inter-Regular.ttf";
    private static final String POLICE_GRASSE = "/com/pingmanager/polices/Inter-SemiBold.ttf";

    private static final float[] TEXTE = rgb(0x2F4558);
    private static final float[] DOUX = rgb(0x5C6E7D);
    private static final float[] PRIMAIRE = rgb(0x335F8A);
    private static final float[] FILET = rgb(0x9FB2C3);
    private static final float[] FOND_ENTETE = rgb(0xE6EEF6);
    private static final float[] FOND_BARRE = rgb(0xD4DEE7);
    private static final float[] FOND_VAINQUEUR = rgb(0xEAF4EC);

    private static final float A4_L = PDRectangle.A4.getWidth();
    private static final float A4_H = PDRectangle.A4.getHeight();
    private static final float MARGE = 36;

    private RenduPdfFeuilles() {
    }

    // ==================================================================
    // Feuilles de match : deux par page A4, séparées par un trait de coupe
    // ==================================================================

    /** @return le PDF des feuilles de match, deux par page (au moins une feuille). */
    public static byte[] feuillesMatch(List<FeuilleMatch> feuilles) throws IOException {
        if (feuilles == null || feuilles.isEmpty()) {
            throw new IllegalArgumentException("Aucune feuille de match à imprimer.");
        }
        try (PDDocument doc = new PDDocument()) {
            Polices p = new Polices(doc);
            doc.getDocumentInformation().setTitle(feuilles.size() == 1 ? FeuilleMatch.TITRE : "Feuilles de match");
            doc.getDocumentInformation().setCreator("PingManager");
            for (int i = 0; i < feuilles.size(); i += 2) {
                PDPage page = new PDPage(PDRectangle.A4);
                doc.addPage(page);
                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    Dessin d = new Dessin(cs, p);
                    feuilleMatch(d, feuilles.get(i), A4_H);
                    // Trait de coupe pointillé au milieu de la page.
                    cs.setLineDashPattern(new float[] {4, 4}, 0);
                    d.trait(MARGE / 2, A4_H / 2, A4_L - MARGE / 2, A4_H / 2, FILET, 0.6f);
                    cs.setLineDashPattern(new float[] {}, 0);
                    if (i + 1 < feuilles.size()) {
                        feuilleMatch(d, feuilles.get(i + 1), A4_H / 2);
                    }
                }
            }
            return enregistrer(doc);
        }
    }

    /** Une feuille de match dans la demi-page dont le haut est à {@code haut}. */
    private static void feuilleMatch(Dessin d, FeuilleMatch f, float haut) throws IOException {
        float x0 = MARGE;
        float largeur = A4_L - 2 * MARGE;
        float y = haut - MARGE + 4;

        d.texte(FeuilleMatch.TITRE, x0, y - 16, d.p.grasse, 16, PRIMAIRE);
        String table = f.table().isEmpty() ? "Table : ______" : f.table();
        float lt = d.largeur(table, d.p.grasse, 22);
        d.texte(table, x0 + largeur - lt, y - 20, d.p.grasse, 22, TEXTE);
        y -= 34;
        d.texte(d.ajuster(f.entete().ligneTournoi(), d.p.normale, 10, largeur - lt - 12), x0, y - 10,
                d.p.normale, 10, DOUX);
        y -= 16;
        String ligne = f.entete().tableau() + (f.phase().isEmpty() ? "" : " · " + f.phase())
                + (f.format().isEmpty() ? "" : " · " + f.format());
        d.texte(d.ajuster(ligne, d.p.grasse, 10.5f, largeur), x0, y - 11, d.p.grasse, 10.5f, TEXTE);
        y -= 24;

        // Les deux joueurs face à face.
        float colonne = (largeur - 40) / 2;
        float hBloc = 66;
        blocJoueur(d, "Joueur A", f.joueurA(), x0, y, colonne, hBloc);
        float lc = d.largeur("contre", d.p.normale, 10);
        d.texte("contre", x0 + colonne + (40 - lc) / 2, y - hBloc / 2 - 4, d.p.normale, 10, DOUX);
        blocJoueur(d, "Joueur B", f.joueurB(), x0 + colonne + 40, y, colonne, hBloc);
        y -= hBloc + 14;

        // Grille 3 × 5 : manches 1 à 5 en en-tête, autres cases vides.
        float etiquette = 150;
        grilleManches(d, f.grille(), f.etiquettesLignes(), x0, y, largeur, etiquette, 24);
        y -= 3 * 24 + 16;

        d.texte("Vainqueur : ", x0, y - 10, d.p.grasse, 10, TEXTE);
        d.trait(x0 + 70, y - 12, x0 + 300, y - 12, FILET, 0.6f);
        d.texte("Score en manches :", x0 + 320, y - 10, d.p.grasse, 10, TEXTE);
        d.trait(x0 + 425, y - 12, x0 + largeur, y - 12, FILET, 0.6f);
        y -= 24;
        d.texte("Cartons (règlement disciplinaire, art. 3) :", x0, y - 10, d.p.normale, 9, DOUX);
        d.trait(x0 + 200, y - 12, x0 + largeur, y - 12, FILET, 0.6f);
        y -= 20;

        // Signatures.
        int n = f.signatures().size();
        float ls = (largeur - (n - 1) * 10) / n;
        float hs = Math.max(28, y - (haut - A4_H / 2) - 14);
        hs = Math.min(hs, 44);
        for (int i = 0; i < n; i++) {
            float xs = x0 + i * (ls + 10);
            d.cadre(xs, y - hs, ls, hs, FILET, null);
            d.texte("Signature — " + f.signatures().get(i), xs + 5, y - 10,
                    d.p.normale, 8, DOUX);
        }
    }

    private static void blocJoueur(Dessin d, String libelle, JoueurFeuille j, float x, float y, float l, float h)
            throws IOException {
        d.cadre(x, y - h, l, h, FILET, null);
        d.texte(libelle, x + 8, y - 12, d.p.normale, 8, DOUX);
        String nom = j.estADeterminer() ? JoueurFeuille.A_DETERMINER : j.nom() + " " + j.prenom();
        d.texte(d.ajuster(nom, d.p.grasse, 13, l - 16), x + 8, y - 29, d.p.grasse, 13, TEXTE);
        d.texte(d.ajuster(j.club().isEmpty() ? "Club : ________" : j.club(), d.p.normale, 10, l - 16),
                x + 8, y - 44, d.p.normale, 10, TEXTE);
        String bas = (j.points().isEmpty() ? "Points : ______" : j.points())
                + (j.licence().isEmpty() ? "" : "  ·  licence " + j.licence());
        d.texte(d.ajuster(bas, d.p.normale, 9.5f, l - 16), x + 8, y - 58, d.p.normale, 9.5f, DOUX);
    }

    /** Grille des manches : étiquettes à gauche (sans cadre), puis 3 lignes × 5 colonnes encadrées. */
    private static void grilleManches(Dessin d, List<List<String>> grille, List<String> etiquettes, float x,
                                      float y, float largeur, float etiquette, float hLigne) throws IOException {
        int colonnes = grille.get(0).size();
        float lc = (largeur - etiquette) / colonnes;
        for (int l = 0; l < grille.size(); l++) {
            float yl = y - (l + 1) * hLigne;
            if (l < etiquettes.size() && !etiquettes.get(l).isEmpty()) {
                d.texte(d.ajuster(etiquettes.get(l), d.p.grasse, 9.5f, etiquette - 8), x, yl + hLigne / 2 - 3.5f,
                        d.p.grasse, 9.5f, TEXTE);
            }
            for (int c = 0; c < colonnes; c++) {
                float xc = x + etiquette + c * lc;
                d.cadre(xc, yl, lc, hLigne, FILET, l == 0 ? FOND_ENTETE : null);
                String s = grille.get(l).get(c);
                if (!s.isEmpty()) {
                    float ls = d.largeur(s, d.p.grasse, 9.5f);
                    d.texte(s, xc + (lc - ls) / 2, yl + hLigne / 2 - 3.5f, d.p.grasse, 9.5f, PRIMAIRE);
                }
            }
        }
    }

    // ==================================================================
    // Feuille de poule, puis feuille de rencontre
    // ==================================================================

    /** @return le PDF de la feuille de poule suivie de la feuille de rencontre. */
    public static byte[] feuillePoule(FeuillePoule f) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            Polices p = new Polices(doc);
            doc.getDocumentInformation().setTitle(FeuillePoule.TITRE + " — " + f.poule());
            doc.getDocumentInformation().setCreator("PingManager");
            Flux fx = new Flux(doc, p, PDRectangle.A4);
            float largeur = A4_L - 2 * MARGE;

            enTetePoule(fx, FeuillePoule.TITRE + " — " + f.poule(), f);

            // Joueurs, numérotés comme au tirage.
            fx.intertitre("Joueurs");
            float[] lj = {28, 130, 110, 150, 60, largeur - 478};
            fx.ligneTableau(lj, List.of("N°", "Nom", "Prénom", "Club", "Points", "Licence"), true, 18);
            for (int i = 0; i < f.joueurs().size(); i++) {
                JoueurFeuille j = f.joueurs().get(i);
                fx.ligneTableau(lj, List.of(String.valueOf(i + 1), j.nom(), j.prenom(), j.club(), j.points(),
                        j.licence()), false, 18);
            }
            fx.espace(12);

            // Ordre des parties (I.301).
            fx.intertitre("Ordre des parties (I.301)");
            float[] lp = {32, 40, 64, (largeur - 136) / 2, (largeur - 136) / 2};
            fx.ligneTableau(lp, List.of("N°", "Tour", "Partie", "Joueur", "Joueur"), true, 17);
            for (PartiePoule pp : f.parties()) {
                fx.ligneTableau(lp, List.of(String.valueOf(pp.numero()), String.valueOf(pp.tour()),
                        pp.libellePositions(), pp.joueurA().nomComplet(),
                        pp.exempt() ? "exempt" : pp.joueurB().nomComplet()), false, 17);
            }
            fx.espace(12);

            // Grille des résultats : diagonale barrée.
            int n = f.grilleResultats().size();
            float cote = Math.min(46, largeur / n);
            fx.assurer(18 + n * cote + 10);
            fx.intertitre("Grille des résultats (manches gagnées par le joueur de la ligne)");
            for (int l = 0; l < n; l++) {
                float yl = fx.y - (l + 1) * cote;
                for (int c = 0; c < n; c++) {
                    String s = f.grilleResultats().get(l).get(c);
                    boolean entete = l == 0 || c == 0;
                    boolean barree = FeuillePoule.CASE_BARREE.equals(s);
                    float xc = MARGE + c * cote;
                    if (l == 0 && c == 0) {
                        continue;
                    }
                    fx.d.cadre(xc, yl, cote, cote, FILET, entete ? FOND_ENTETE : barree ? FOND_BARRE : null);
                    if (entete) {
                        float ls = fx.d.largeur(s, p.grasse, 11);
                        fx.d.texte(s, xc + (cote - ls) / 2, yl + cote / 2 - 4, p.grasse, 11, PRIMAIRE);
                    }
                }
            }
            fx.y -= n * cote + 14;

            // Classement à remplir (I.303).
            fx.assurer(18 + (f.lignesClassement() + 1) * 20 + 40);
            fx.intertitre("Classement à remplir (I.303)" + (f.qualifies() > 0
                    ? " — " + f.qualifies() + (f.qualifies() == 1 ? " qualifié" : " qualifiés") : ""));
            float[] lk = {44, 32, largeur - 340, 66, 66, 66, 66};
            fx.ligneTableau(lk, f.colonnesClassement(), true, 20);
            for (int i = 0; i < f.lignesClassement(); i++) {
                fx.ligneTableau(lk, List.of("", "", "", "", "", "", ""), false, 20);
            }
            fx.espace(6);
            fx.paragraphe(f.noteClassement(), 8.5f, DOUX);

            // Feuille de rencontre : chaque partie avec sa grille de manches 3 × 5.
            fx.nouvellePage();
            enTetePoule(fx, FeuillePoule.TITRE_RENCONTRE + " — " + f.poule(), f);
            for (PartiePoule pp : f.parties()) {
                if (pp.exempt()) {
                    continue;
                }
                float hGrille = 3 * 20;
                fx.assurer(20 + hGrille + 16);
                String titre = "Partie " + pp.numero() + " · tour " + pp.tour() + " · " + pp.libellePositions()
                        + " :  " + pp.joueurA().nomComplet() + " (" + pp.joueurA().detail() + ")  contre  "
                        + pp.joueurB().nomComplet() + " (" + pp.joueurB().detail() + ")";
                fx.d.texte(fx.d.ajuster(titre, p.grasse, 9.5f, largeur), MARGE, fx.y - 10, p.grasse, 9.5f, TEXTE);
                fx.y -= 16;
                grilleManches(fx.d, pp.grille(), List.of("", pp.joueurA().nomComplet(), pp.joueurB().nomComplet()),
                        MARGE, fx.y, largeur, 150, 20);
                fx.y -= hGrille + 14;
            }
            fx.fermer();
            return enregistrer(doc);
        }
    }

    private static void enTetePoule(Flux fx, String titre, FeuillePoule f) throws IOException {
        float largeur = A4_L - 2 * MARGE;
        fx.d.texte(titre, MARGE, fx.y - 18, fx.p.grasse, 18, PRIMAIRE);
        fx.y -= 28;
        fx.d.texte(fx.d.ajuster(f.entete().ligneTournoi(), fx.p.normale, 10, largeur), MARGE, fx.y - 10,
                fx.p.normale, 10, DOUX);
        fx.y -= 16;
        fx.d.texte(fx.d.ajuster(f.entete().tableau() + (f.format().isEmpty() ? "" : " · " + f.format()),
                fx.p.grasse, 10.5f, largeur), MARGE, fx.y - 11, fx.p.grasse, 10.5f, TEXTE);
        fx.y -= 22;
    }

    // ==================================================================
    // Tableau final (arbre)
    // ==================================================================

    /** @return le PDF du tableau final : A4 paysage jusqu'à 8 matchs au premier tour, A3 paysage au-delà. */
    public static byte[] arbre(ArbreImprimable a) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            Polices p = new Polices(doc);
            doc.getDocumentInformation().setTitle(ArbreImprimable.TITRE + " — " + a.entete().tableau());
            doc.getDocumentInformation().setCreator("PingManager");
            PDRectangle base = a.casesPremierTour() <= 8 ? PDRectangle.A4 : PDRectangle.A3;
            PDRectangle format = new PDRectangle(base.getHeight(), base.getWidth()); // paysage
            PDPage page = new PDPage(format);
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                Dessin d = new Dessin(cs, p);
                float l = format.getWidth();
                float h = format.getHeight();
                float y = h - MARGE + 4;
                d.texte(ArbreImprimable.TITRE + " — " + a.entete().tableau(), MARGE, y - 20, p.grasse, 20, PRIMAIRE);
                y -= 30;
                d.texte(d.ajuster(a.entete().ligneTournoi() + (a.resume().isEmpty() ? "" : " · " + a.resume()),
                        p.normale, 10, l - 2 * MARGE), MARGE, y - 10, p.normale, 10, DOUX);
                y -= 26;
                dessinerArbre(d, a, MARGE, y, l - 2 * MARGE, y - MARGE);
            }
            return enregistrer(doc);
        }
    }

    private static void dessinerArbre(Dessin d, ArbreImprimable a, float x0, float haut, float largeur, float hauteur)
            throws IOException {
        List<ColonneArbre> cols = a.colonnes();
        int nc = cols.size();
        if (nc == 0) {
            return;
        }
        float ecart = 22;
        float lcol = (largeur - (nc - 1) * ecart) / nc;
        float hTitre = 18;
        float zone = hauteur - hTitre;
        int cases = Math.max(1, a.casesPremierTour());
        float pas0 = zone / cases;
        float hCase = Math.min(46, pas0 * 0.86f);
        float taille = Math.max(6.5f, Math.min(10, hCase / 4.2f));

        for (int c = 0; c < nc; c++) {
            ColonneArbre col = cols.get(c);
            float xc = x0 + c * (lcol + ecart);
            d.texte(d.ajuster(col.libelle(), d.p.grasse, 10, lcol), xc, haut - 11, d.p.grasse, 10, PRIMAIRE);
            float pas = zone / col.cases().size();
            for (int k = 0; k < col.cases().size(); k++) {
                CaseArbre ca = col.cases().get(k);
                if (ca.type() == TypeCase.VIDE) {
                    continue;
                }
                float cy = haut - hTitre - (k + 0.5f) * pas;
                caseArbre(d, ca, xc, cy - hCase / 2, lcol, hCase, taille);
                if (c + 1 < nc) {
                    float pasParent = zone / cols.get(c + 1).cases().size();
                    float cyParent = haut - hTitre - (k / 2 + 0.5f) * pasParent;
                    float xm = xc + lcol + ecart / 2;
                    d.trait(xc + lcol, cy, xm, cy, FILET, 0.8f);
                    d.trait(xm, cy, xm, cyParent, FILET, 0.8f);
                    d.trait(xm, cyParent, xc + lcol + ecart, cyParent, FILET, 0.8f);
                }
            }
        }
        CaseArbre pf = a.petiteFinale();
        if (pf != null) {
            float xc = x0 + (nc - 1) * (lcol + ecart);
            float yb = haut - hauteur;
            float hPf = Math.max(hCase, 34);
            d.texte(ArbreImprimable.TITRE_PETITE_FINALE, xc, yb + hPf + 6, d.p.grasse, 9.5f, PRIMAIRE);
            caseArbre(d, pf, xc, yb, lcol, hPf, Math.max(taille, 8));
        }
    }

    private static void caseArbre(Dessin d, CaseArbre ca, float x, float y, float l, float h, float taille)
            throws IOException {
        boolean exempt = ca.type() == TypeCase.EXEMPT;
        float hl = h / 2;
        ligneJoueur(d, ca.joueurA(), ca.scoreA(), ca.vainqueurA(), x, y + hl, l, hl, taille);
        if (exempt) {
            d.texte("exempt", x + 4, y + hl / 2 - taille / 3, d.p.normale, taille * 0.9f, DOUX);
        } else {
            ligneJoueur(d, ca.joueurB(), ca.scoreB(), ca.vainqueurB(), x, y, l, hl, taille);
            d.trait(x, y + hl, x + l, y + hl, FOND_BARRE, 0.5f);
        }
        // Cadre posé en dernier : le fond du vainqueur ne le masque pas.
        if (exempt) {
            d.cs.setLineDashPattern(new float[] {2, 2}, 0);
        }
        d.cadre(x, y, l, h, FILET, null);
        d.cs.setLineDashPattern(new float[] {}, 0);
        if (exempt) {
            return;
        }
        if (!ca.mention().isEmpty()) {
            float lm = d.largeur(ca.mention(), d.p.normale, taille * 0.8f);
            d.texte(ca.mention(), x + l - lm, y - taille * 0.9f, d.p.normale, taille * 0.8f, PRIMAIRE);
        }
    }

    private static void ligneJoueur(Dessin d, JoueurFeuille j, String score, boolean vainqueur, float x, float y,
                                    float l, float h, float taille) throws IOException {
        if (vainqueur) {
            d.cadre(x, y, l, h, null, FOND_VAINQUEUR);
        }
        PDFont f = vainqueur ? d.p.grasse : d.p.normale;
        float ls = score.isEmpty() ? 0 : d.largeur(score, d.p.grasse, taille) + 8;
        String nom = j.estADeterminer() ? JoueurFeuille.A_DETERMINER
                : j.nomComplet() + (j.points().isEmpty() ? "" : " (" + j.points().replace(" pts", "") + ")");
        d.texte(d.ajuster(nom, f, taille, l - 8 - ls), x + 4, y + h / 2 - taille / 3, f, taille,
                j.estADeterminer() ? DOUX : TEXTE);
        if (!score.isEmpty()) {
            d.texte(score, x + l - ls + 2, y + h / 2 - taille / 3, d.p.grasse, taille, TEXTE);
        }
    }

    // ==================================================================
    // Outils de dessin
    // ==================================================================

    private static byte[] enregistrer(PDDocument doc) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        doc.save(out);
        return out.toByteArray();
    }

    private static final class Polices {
        final PDFont normale;
        final PDFont grasse;

        Polices(PDDocument doc) throws IOException {
            normale = charger(doc, POLICE_NORMALE);
            grasse = charger(doc, POLICE_GRASSE);
        }

        private static PDFont charger(PDDocument doc, String chemin) throws IOException {
            try (InputStream in = RenduPdfFeuilles.class.getResourceAsStream(chemin)) {
                if (in == null) {
                    throw new IOException("Police introuvable dans l'application : " + chemin);
                }
                return PDType0Font.load(doc, in);
            }
        }
    }

    /** Primitives de dessin sur une page. */
    private static final class Dessin {
        final PDPageContentStream cs;
        final Polices p;

        Dessin(PDPageContentStream cs, Polices p) {
            this.cs = cs;
            this.p = p;
        }

        void texte(String s, float x, float y, PDFont f, float taille, float[] couleur) throws IOException {
            String t = propre(s);
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

        float largeur(String s, PDFont f, float taille) {
            try {
                return f.getStringWidth(propre(s)) / 1000f * taille;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        /** Coupe le texte avec « … » pour qu'il tienne dans la largeur. */
        String ajuster(String s, PDFont f, float taille, float max) {
            String t = propre(s);
            if (largeur(t, f, taille) <= max) {
                return t;
            }
            while (!t.isEmpty() && largeur(t + "…", f, taille) > max) {
                t = t.substring(0, t.length() - 1);
            }
            return t.isEmpty() ? "" : t.stripTrailing() + "…";
        }

        void trait(float x1, float y1, float x2, float y2, float[] couleur, float epaisseur) throws IOException {
            cs.setStrokingColor(couleur[0], couleur[1], couleur[2]);
            cs.setLineWidth(epaisseur);
            cs.moveTo(x1, y1);
            cs.lineTo(x2, y2);
            cs.stroke();
        }

        /** Rectangle : contour si {@code contour} non nul, fond si {@code fond} non nul. */
        void cadre(float x, float y, float l, float h, float[] contour, float[] fond) throws IOException {
            if (fond != null) {
                cs.setNonStrokingColor(fond[0], fond[1], fond[2]);
                cs.addRect(x, y, l, h);
                cs.fill();
            }
            if (contour != null) {
                cs.setStrokingColor(contour[0], contour[1], contour[2]);
                cs.setLineWidth(0.7f);
                cs.addRect(x, y, l, h);
                cs.stroke();
            }
        }
    }

    /** Mise en page qui s'écoule de haut en bas, avec sauts de page. */
    private static final class Flux {
        final PDDocument doc;
        final Polices p;
        final PDRectangle format;
        PDPageContentStream cs;
        Dessin d;
        float y;

        Flux(PDDocument doc, Polices p, PDRectangle format) throws IOException {
            this.doc = doc;
            this.p = p;
            this.format = format;
            nouvellePage();
        }

        void nouvellePage() throws IOException {
            fermer();
            PDPage page = new PDPage(format);
            doc.addPage(page);
            cs = new PDPageContentStream(doc, page);
            d = new Dessin(cs, p);
            y = format.getHeight() - MARGE;
        }

        void fermer() throws IOException {
            if (cs != null) {
                cs.close();
                cs = null;
            }
        }

        void assurer(float h) throws IOException {
            if (y - h < MARGE) {
                nouvellePage();
            }
        }

        void espace(float h) {
            y -= h;
        }

        void intertitre(String s) throws IOException {
            assurer(40);
            d.texte(s, MARGE, y - 12, p.grasse, 12, PRIMAIRE);
            y -= 20;
        }

        void ligneTableau(float[] largeurs, List<String> cellules, boolean entete, float h) throws IOException {
            assurer(h);
            float x = MARGE;
            for (int i = 0; i < largeurs.length; i++) {
                d.cadre(x, y - h, largeurs[i], h, FILET, entete ? FOND_ENTETE : null);
                String s = i < cellules.size() ? cellules.get(i) : "";
                PDFont f = entete ? p.grasse : p.normale;
                d.texte(d.ajuster(s, f, 9, largeurs[i] - 8), x + 4, y - h / 2 - 3.2f, f, 9, entete ? PRIMAIRE : TEXTE);
                x += largeurs[i];
            }
            y -= h;
        }

        void paragraphe(String s, float taille, float[] couleur) throws IOException {
            float max = format.getWidth() - 2 * MARGE;
            StringBuilder ligne = new StringBuilder();
            for (String mot : propre(s).split(" ")) {
                String essai = ligne.length() == 0 ? mot : ligne + " " + mot;
                if (d.largeur(essai, p.normale, taille) > max && ligne.length() > 0) {
                    assurer(taille + 4);
                    d.texte(ligne.toString(), MARGE, y - taille, p.normale, taille, couleur);
                    y -= taille + 4;
                    ligne = new StringBuilder(mot);
                } else {
                    ligne = new StringBuilder(essai);
                }
            }
            if (ligne.length() > 0) {
                assurer(taille + 4);
                d.texte(ligne.toString(), MARGE, y - taille, p.normale, taille, couleur);
                y -= taille + 4;
            }
        }
    }

    /** Retire ce que la police ne sait pas poser (sauts de ligne, tabulations, espaces fines). */
    static String propre(String s) {
        return s == null ? "" : s.replace('\n', ' ').replace('\r', ' ').replace('\t', ' ')
                .replace(' ', ' ').replace(' ', ' ');
    }

    private static float[] rgb(int hex) {
        return new float[] {((hex >> 16) & 0xFF) / 255f, ((hex >> 8) & 0xFF) / 255f, (hex & 0xFF) / 255f};
    }
}
