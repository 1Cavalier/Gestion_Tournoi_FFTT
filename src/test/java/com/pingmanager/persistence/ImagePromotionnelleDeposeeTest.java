package com.pingmanager.persistence;

import com.pingmanager.model.DocumentReference;
import com.pingmanager.model.Tournoi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Reliquat du point 18 : l'image promotionnelle suit la même mécanique que le règlement déposé. */
class ImagePromotionnelleDeposeeTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 13};
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 16};

    @TempDir
    Path dossier;

    private Path donnees() {
        return dossier.resolve("donnees");
    }

    private Path fichier(String nom, byte[] contenu) throws IOException {
        Path p = dossier.resolve(nom);
        Files.createDirectories(p.getParent());
        Files.write(p, contenu);
        return p;
    }

    @Test
    void image_png_copiee_dans_le_dossier_de_donnees_avec_un_nom_assaini() throws IOException {
        DocumentsDeposes documents = new DocumentsDeposes(donnees());
        ResultatDepotDocument r = documents.deposerImage(fichier("source/Affiche été ../x.PNG", PNG), "Image");
        assertTrue(r.estDepose(), r.getMessage());
        String emplacement = r.getReference().getEmplacement();
        assertTrue(emplacement.matches("documents/x-[0-9a-f]{12}\\.png"), emplacement);
        assertTrue(DocumentsDeposes.emplacementStocke(emplacement));
        assertTrue(documents.fichier(r.getReference()).isPresent());
    }

    @Test
    void image_jpeg_reconnue_a_son_contenu_et_non_a_son_extension() throws IOException {
        DocumentsDeposes documents = new DocumentsDeposes(donnees());
        ResultatDepotDocument r = documents.deposerImage(fichier("affiche.jpeg", JPEG), "Image");
        assertTrue(r.getReference().getEmplacement().endsWith(".jpg"));
        ResultatDepotDocument faux = documents.deposerImage(fichier("faux.png", "<html>".getBytes()), "Image");
        assertEquals(ResultatDepotDocument.Motif.PAS_UNE_IMAGE, faux.getMotif());
        assertEquals(ResultatDepotDocument.Motif.FICHIER_INTROUVABLE,
                documents.deposerImage(dossier.resolve("absent.png"), "Image").getMotif());
    }

    @Test
    void emplacements_forges_jamais_resolus() throws IOException {
        DocumentsDeposes documents = new DocumentsDeposes(donnees());
        Files.createDirectories(donnees().resolve(DocumentsDeposes.DOSSIER));
        Files.write(donnees().resolve(DocumentsDeposes.DOSSIER).resolve(".cache.png"), PNG);
        for (String e : new String[] {"documents/.cache.png", "documents/../x.png", "documents/x.gif",
                "documents/a b.png", "/tmp/x.png"}) {
            assertTrue(documents.fichier(new DocumentReference("I", e)).isEmpty(), e);
        }
    }

    @Test
    void ancien_chemin_absolu_existant_copie_a_la_premiere_lecture_puis_enregistre() throws IOException {
        Path source = fichier("ancien/affiche.png", PNG);
        Tournoi t = new Tournoi("Open", "Brunoy", LocalDate.of(2027, 4, 10));
        t.setImagePromotionnelle(new DocumentReference("Image promotionnelle", source.toAbsolutePath().toString()));
        Path f = dossier.resolve("open" + DepotTournoi.EXTENSION);
        DepotTournoi.sauvegarder(t, f);
        Tournoi relu = DepotTournoi.charger(f).getTournoi();
        DocumentsDeposes documents = new DocumentsDeposes(donnees());

        assertTrue(documents.migrerImage(relu));
        assertTrue(DocumentsDeposes.emplacementStocke(relu.getImagePromotionnelle().getEmplacement()));
        assertEquals("Image promotionnelle", relu.getImagePromotionnelle().getLibelle());
        assertTrue(documents.fichier(relu.getImagePromotionnelle()).isPresent());
        assertFalse(documents.migrerImage(relu), "déjà migrée");

        DepotTournoi.sauvegarder(relu, f);
        assertEquals(relu.getImagePromotionnelle().getEmplacement(),
                DepotTournoi.charger(f).getTournoi().getImagePromotionnelle().getEmplacement());
    }

    @Test
    void ancien_chemin_absolu_introuvable_garde_la_reference_sans_planter() {
        Tournoi t = new Tournoi("Open", "Brunoy", LocalDate.of(2027, 4, 10));
        String absent = dossier.resolve("disparu/affiche.png").toAbsolutePath().toString();
        t.setImagePromotionnelle(new DocumentReference("Image promotionnelle", absent));
        DocumentsDeposes documents = new DocumentsDeposes(donnees());
        assertFalse(documents.migrerImage(t));
        assertEquals(absent, t.getImagePromotionnelle().getEmplacement());
        assertTrue(documents.fichier(t.getImagePromotionnelle()).isEmpty(), "« image introuvable », jamais ouverte");
        assertEquals("affiche.png", DocumentsDeposes.nomAffiche(t.getImagePromotionnelle()));
    }

    @Test
    void ancien_chemin_absolu_vers_un_fichier_qui_n_est_pas_une_image_non_copie() throws IOException {
        Path source = fichier("ancien/secret.png", "pas une image".getBytes());
        Tournoi t = new Tournoi("Open", "Brunoy", LocalDate.of(2027, 4, 10));
        t.setImagePromotionnelle(new DocumentReference("Image", source.toAbsolutePath().toString()));
        assertFalse(new DocumentsDeposes(donnees()).migrerImage(t));
    }
}
