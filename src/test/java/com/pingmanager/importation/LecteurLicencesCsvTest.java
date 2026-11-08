package com.pingmanager.importation;

import com.pingmanager.model.FicheLicence;
import com.pingmanager.model.Sexe;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/** B023 : lecture de licencies.csv (entrée non fiable) et registre des licences par fichier. */
class LecteurLicencesCsvTest {

    private static byte[] csv(String texte) {
        return texte.getBytes(StandardCharsets.UTF_8);
    }

    private static final String ENTETES = "﻿Licence;Nom;Prénom;Sexe;N° club;Club;Points;Points mensuels;Certificat;Relevé le\r\n";

    @Test
    void lit_les_colonnes_par_leur_nom_avec_bom_et_conserve_les_zeros_du_club() throws Exception {
        ResultatImportLicences r = LecteurLicencesCsv.lire("licencies.csv", csv(ENTETES
                + "9112345;DUPONT;Marie;F;08910001;Brunoy TT;1234;1250;EN_REGLE;01/10/2026\r\n"
                + "9154321;MARTIN;Lucas;M;8910001;Brunoy TT;987;;;03/10/2026\r\n"));
        assertEquals(2, r.fiches().size(), r.anomalies()::toString);
        FicheLicence m = r.fiches().get(0);
        assertEquals("08910001", m.getNumeroClub());
        assertEquals(Sexe.FEMME, m.getSexe());
        assertEquals(1234, m.getPoints());
        assertEquals(1250, m.getPointsMensuels());
        FicheLicence l = r.fiches().get(1);
        assertEquals("08910001", l.getNumeroClub(), "zéro perdu par le tableur rétabli");
        assertEquals(987, l.getPointsMensuels(), "points mensuels vides = points");
        assertEquals(SituationCertificat.EN_REGLE, r.certificats().get("9112345"));
        assertEquals(SituationCertificat.INCONNU, r.certificats().get("9154321"));
        assertEquals(LocalDate.of(2026, 10, 1), r.releveLe(), "date la plus ancienne");
    }

    @Test
    void ordre_libre_casse_et_accents_ignores_colonnes_inconnues_ignorees() throws Exception {
        ResultatImportLicences r = LecteurLicencesCsv.lire("l.csv", csv(
                "POINTS;prenom;Colonne inconnue;NOM;licence;sexe;n° CLUB\n1500;Éva;x;LÉGER;9100001;f;08910001\n"));
        assertEquals(1, r.fiches().size());
        assertEquals("LÉGER", r.fiches().get(0).getNom());
    }

    @Test
    void colonne_obligatoire_absente_refuse_le_fichier() {
        LecteurTableur.ImportException e = assertThrows(LecteurTableur.ImportException.class,
                () -> LecteurLicencesCsv.lire("l.csv", csv("Licence;Nom;Prénom\n1;A;B\n")));
        assertTrue(e.getMessage().contains("Sexe"));
        assertTrue(e.getMessage().contains("Points"));
    }

    @Test
    void fichier_vide_ou_trop_gros_refuse() {
        assertThrows(LecteurTableur.ImportException.class, () -> LecteurLicencesCsv.lire("l.csv", new byte[0]));
        assertThrows(LecteurTableur.ImportException.class,
                () -> LecteurLicencesCsv.lire("l.csv", new byte[CsvFederal.TAILLE_MAX_OCTETS + 1]));
    }

    @Test
    void lignes_invalides_refusees_avec_leur_numero() throws Exception {
        ResultatImportLicences r = LecteurLicencesCsv.lire("l.csv", csv(ENTETES
                + ";SANS;Licence;M;08910001;;500;;;\n"                // ligne 2 : licence vide
                + "9100002;A;B;X;08910001;;500;;;\n"                 // 3 : sexe
                + "9100003;A;B;M;123;;500;;;\n"                      // 4 : club
                + "9100004;A;B;M;08910001;;cinq cents;;;\n"          // 5 : points
                + "9100005;A;B;M;08910001;;-12;;;\n"                 // 6 : points négatifs
                + "9100006;A;B;M;08910001;;600;;;\n"                 // 7 : valide
                + "9100006;C;D;M;08910001;;700;;;\n"                 // 8 : doublon
                + "91-00;A;B;M;08910001;;600;;;\n"));                // 9 : licence mal formée
        assertEquals(1, r.fiches().size());
        assertEquals(7, r.lignesRefusees());
        assertEquals(2, r.anomalies().get(0).ligne());
        assertTrue(r.anomalies().stream().anyMatch(a -> a.ligne() == 8 && a.motif().contains("double")));
        assertTrue(r.anomalies().stream().anyMatch(a -> a.ligne() == 9));
    }

    @Test
    void guillemets_points_virgules_et_ligne_trop_longue() throws Exception {
        String longue = "x".repeat(CsvFederal.LONGUEUR_MAX_CELLULE + 1);
        ResultatImportLicences r = LecteurLicencesCsv.lire("l.csv", csv(ENTETES
                + "9100001;\"D'ARC; dite \"\"Jeanne\"\"\";Jeanne;F;08910001;\"Club ; Orléans\";800;;;\n"
                + "9100002;" + longue + ";B;M;08910001;;600;;;\n"));
        assertEquals(1, r.fiches().size());
        assertEquals("D'ARC; dite \"Jeanne\"", r.fiches().get(0).getNom());
        assertEquals("Club ; Orléans", r.fiches().get(0).getNomClub());
        assertTrue(r.anomalies().get(0).motif().contains("trop longue"));
    }

    @Test
    void valeur_facultative_mal_formee_ignoree_et_signalee() throws Exception {
        ResultatImportLicences r = LecteurLicencesCsv.lire("l.csv", csv(ENTETES
                + "9100001;A;B;M;08910001;;800;beaucoup;PEUT_ETRE;32/13/2026\n"));
        assertEquals(1, r.fiches().size());
        assertEquals(800, r.fiches().get(0).getPointsMensuels());
        assertEquals(SituationCertificat.INCONNU, r.certificats().get("9100001"));
        assertNull(r.releveLe());
        assertEquals(3, r.anomalies().size());
        assertEquals(0, r.lignesRefusees());
    }

    @Test
    void windows_1252_accepte() throws Exception {
        byte[] o = ("Licence;Nom;Prénom;Sexe;N° club;Points\n9100001;LÉGER;Zoé;F;08910001;700\n")
                .getBytes(java.nio.charset.Charset.forName("windows-1252"));
        ResultatImportLicences r = LecteurLicencesCsv.lire("l.csv", o);
        assertEquals("Zoé", r.fiches().get(0).getPrenom());
    }

    @Test
    void registre_trouve_recherche_sans_accents_et_liste_par_club() throws Exception {
        ResultatImportLicences r = LecteurLicencesCsv.lire("l.csv", csv(ENTETES
                + "9112345;DUPONT;Hélène;F;08910001;Brunoy;1234;;SANS_PRATIQUE;\n"
                + "9154321;MARTIN;Lucas;M;07510002;Paris;987;;;\n"));
        RegistreLicencesFichier reg = new RegistreLicencesFichier(r);
        assertEquals(2, reg.taille());
        assertTrue(reg.existe(" 9112345 "));
        assertFalse(reg.existe(""));
        assertTrue(reg.trouverParLicence(null).isEmpty());
        assertEquals("9112345", reg.rechercher("helene dup", 10).get(0).getNumeroLicence());
        assertEquals(1, reg.rechercher("915", 10).size());
        assertTrue(reg.rechercher("h", 10).isEmpty());
        assertEquals(1, reg.listerParClub("07510002").size());
        assertEquals(SituationCertificat.SANS_PRATIQUE, reg.situationCertificat("9112345"));
        assertEquals(SituationCertificat.INCONNU, reg.situationCertificat("inconnue"));
    }
}
