package com.pingmanager.persistence;

import com.pingmanager.core.ServiceCycleTournoi;
import com.pingmanager.core.reglement.TournoisReglementDeTest;
import com.pingmanager.model.DocumentReference;
import com.pingmanager.model.Tournoi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Règlement déposé copié dans le dossier de données (README section 5,
 * point 18 ; ancienne faille S-06 : chemin absolu libre relu sans contrôle).
 */
class DocumentsDeposesTest {

    @TempDir
    Path dossier;

    private Path donnees() {
        return dossier.resolve("donnees");
    }

    private Path pdf(String nom, String contenu) throws IOException {
        Path f = dossier.resolve(nom);
        Files.createDirectories(f.getParent());
        Files.writeString(f, "%PDF-1.4\n" + contenu, StandardCharsets.ISO_8859_1);
        return f;
    }

    private long nombreDeCopies() throws IOException {
        Path d = donnees().resolve(DocumentsDeposes.DOSSIER);
        if (!Files.isDirectory(d)) {
            return 0;
        }
        try (Stream<Path> s = Files.list(d)) {
            return s.count();
        }
    }

    /** Ancien test de démonstration de la faille S-06, inversé après correction. */
    @Test
    void reglement_depose_pointant_vers_le_depot_applicatif_n_est_jamais_ouvert_ni_copie() throws IOException {
        Path depot = donnees().resolve(DepotApplication.NOM_FICHIER);
        Files.createDirectories(depot.getParent());
        Files.writeString(depot, "{\"comptes\":[]}");
        Tournoi t = new Tournoi("Open", "Brunoy", LocalDate.of(2027, 4, 10));
        t.setReglementDocument(new DocumentReference("Règlement", depot.toString()));
        Path f = dossier.resolve("open" + DepotTournoi.EXTENSION);
        DepotTournoi.sauvegarder(t, f);
        Tournoi relu = DepotTournoi.charger(f).getTournoi();
        DocumentsDeposes documents = new DocumentsDeposes(donnees());

        assertFalse(documents.migrerReglement(relu), "pas un PDF : aucune copie");
        assertTrue(documents.fichier(relu.getReglementDocument()).isEmpty(), "chemin absolu jamais résolu");
        assertEquals(0, nombreDeCopies());
    }

    @Test
    void depot_d_un_pdf_copie_dans_le_dossier_de_donnees_avec_un_emplacement_relatif() throws IOException {
        Path source = pdf("club/Règlement été 2027.pdf", "contenu");
        DocumentsDeposes documents = new DocumentsDeposes(donnees());

        ResultatDepotDocument r = documents.deposer(source, "Règlement");

        assertTrue(r.estDepose(), r.getMessage());
        String emplacement = r.getReference().getEmplacement();
        assertTrue(emplacement.matches("documents/Reglement_ete_2027-[0-9a-f]{12}\\.pdf"), emplacement);
        Path copie = documents.fichier(r.getReference()).orElseThrow();
        assertTrue(copie.startsWith(donnees().toAbsolutePath()));
        assertEquals(Files.readString(source, StandardCharsets.ISO_8859_1),
                Files.readString(copie, StandardCharsets.ISO_8859_1));

        Files.delete(source);
        assertTrue(documents.fichier(r.getReference()).isPresent(), "la copie survit à l'original");
    }

    @Test
    void deposer_deux_fois_le_meme_fichier_ne_cree_qu_une_copie() throws IOException {
        Path source = pdf("r.pdf", "x");
        DocumentsDeposes documents = new DocumentsDeposes(donnees());

        DocumentReference a = documents.deposer(source, "R").getReference();
        DocumentReference b = documents.deposer(source, "R").getReference();

        assertEquals(a, b);
        assertEquals(1, nombreDeCopies());
        Files.writeString(source, "%PDF-1.4 autre version");
        assertFalse(a.equals(documents.deposer(source, "R").getReference()), "contenu différent : autre nom");
    }

    @Test
    void depot_refuse_avec_un_motif_precis() throws IOException {
        DocumentsDeposes documents = new DocumentsDeposes(donnees());
        Path texte = dossier.resolve("faux.pdf");
        Files.writeString(texte, "je ne suis pas un PDF");
        Path enorme = dossier.resolve("enorme.pdf");
        Files.writeString(enorme, "%PDF-1.4");
        try (RandomAccessFile f = new RandomAccessFile(enorme.toFile(), "rw")) {
            f.setLength(DocumentsDeposes.TAILLE_MAX_OCTETS + 1); // fichier creux
        }

        assertEquals(ResultatDepotDocument.Motif.PAS_UN_PDF, documents.deposer(texte, "R").getMotif());
        assertEquals(ResultatDepotDocument.Motif.FICHIER_INTROUVABLE,
                documents.deposer(dossier.resolve("absent.pdf"), "R").getMotif());
        assertEquals(ResultatDepotDocument.Motif.FICHIER_INTROUVABLE, documents.deposer(dossier, "R").getMotif());
        assertEquals(ResultatDepotDocument.Motif.TROP_VOLUMINEUX, documents.deposer(enorme, "R").getMotif());
        assertEquals(0, nombreDeCopies());
    }

    @Test
    void nom_de_fichier_assaini_sans_remontee_ni_separateur() {
        assertEquals("passwd.pdf", DocumentsDeposes.assainir("../../etc/passwd"));
        assertEquals("Reglement_ete.pdf", DocumentsDeposes.assainir("C:\\Users\\x\\Règlement été.pdf"));
        assertEquals("reglement.pdf", DocumentsDeposes.assainir(".."));
        assertEquals("reglement.pdf", DocumentsDeposes.assainir("..pdf"));
        assertEquals("cache.pdf", DocumentsDeposes.assainir(".cache.PDF"));
        assertEquals("a.b.pdf", DocumentsDeposes.assainir("a...b.pdf"));
        assertEquals("reglement.pdf", DocumentsDeposes.assainir(null));
        assertTrue(DocumentsDeposes.assainir("x".repeat(500) + ".pdf").length() <= 84);
        for (String nom : new String[] {"../a.pdf", "a/../../b.pdf", "a\\b.pdf", "a:b.pdf", "\u0000.pdf"}) {
            String s = DocumentsDeposes.assainir(nom);
            assertFalse(s.contains("..") || s.contains("/") || s.contains("\\") || s.startsWith("."), s);
        }
    }

    @Test
    void emplacement_forge_jamais_resolu_hors_du_dossier() throws IOException {
        DocumentsDeposes documents = new DocumentsDeposes(donnees());
        Files.createDirectories(donnees().resolve(DocumentsDeposes.DOSSIER).resolve("sous"));
        Files.writeString(donnees().resolve(DepotApplication.NOM_FICHIER), "{}");
        Files.writeString(donnees().resolve(DocumentsDeposes.DOSSIER).resolve("sous").resolve("x.pdf"), "%PDF-");
        for (String e : new String[] {"documents/../" + DepotApplication.NOM_FICHIER, "documents/sous/x.pdf",
                "documents/..", "documents/", "/etc/passwd", "documents\\..\\x.pdf", "../documents/x.pdf",
                donnees().resolve(DepotApplication.NOM_FICHIER).toString()}) {
            assertTrue(documents.fichier(new DocumentReference("R", e)).isEmpty(), e);
        }
        assertTrue(documents.fichier(null).isEmpty());
    }

    @Test
    void ancien_fichier_au_chemin_absolu_existant_copie_a_la_premiere_lecture() throws IOException {
        Path source = pdf("ancien/reglement.pdf", "ancien");
        Tournoi t = new Tournoi("Open", "Brunoy", LocalDate.of(2027, 4, 10));
        t.setReglementDocument(new DocumentReference("Règlement", source.toAbsolutePath().toString()));
        Path f = dossier.resolve("open" + DepotTournoi.EXTENSION);
        DepotTournoi.sauvegarder(t, f);
        Tournoi relu = DepotTournoi.charger(f).getTournoi();
        DocumentsDeposes documents = new DocumentsDeposes(donnees());

        assertTrue(documents.migrerReglement(relu));

        assertTrue(DocumentsDeposes.emplacementStocke(relu.getReglementDocument().getEmplacement()));
        assertEquals("Règlement", relu.getReglementDocument().getLibelle());
        assertTrue(documents.fichier(relu.getReglementDocument()).isPresent());
        assertFalse(documents.migrerReglement(relu), "déjà migré : rien à faire");
    }

    @Test
    void ancien_fichier_au_chemin_absolu_disparu_signale_introuvable_sans_planter() throws IOException {
        Tournoi t = new Tournoi("Open", "Brunoy", LocalDate.of(2027, 4, 10));
        String disparu = dossier.resolve("disparu/reglement.pdf").toAbsolutePath().toString();
        t.setReglementDocument(new DocumentReference("Règlement", disparu));
        Path f = dossier.resolve("open" + DepotTournoi.EXTENSION);
        DepotTournoi.sauvegarder(t, f);
        Tournoi relu = DepotTournoi.charger(f).getTournoi();
        DocumentsDeposes documents = new DocumentsDeposes(donnees());

        assertFalse(documents.migrerReglement(relu));

        assertEquals(disparu, relu.getReglementDocument().getEmplacement(), "référence conservée (réversible)");
        assertTrue(documents.fichier(relu.getReglementDocument()).isEmpty(), "fichier introuvable");
        assertEquals("reglement.pdf", DocumentsDeposes.nomAffiche(relu.getReglementDocument()));
    }

    @Test
    void migration_permise_pour_un_tournoi_deja_publie() throws IOException {
        Path source = pdf("publie/reglement.pdf", "publié");
        Tournoi t = TournoisReglementDeTest.tournoiPubliable();
        t.setReglementDocument(new DocumentReference("Règlement", source.toAbsolutePath().toString()));
        ServiceCycleTournoi cycle = new ServiceCycleTournoi();
        assertTrue(cycle.homologuer(t, "H-2027-002").estEffectuee());
        assertTrue(cycle.publier(t).estEffectuee());
        assertFalse(t.reglementModifiable());
        DocumentsDeposes documents = new DocumentsDeposes(donnees());

        assertTrue(documents.migrerReglement(t));
        assertTrue(documents.fichier(t.getReglementDocument()).isPresent());
    }
}
