package com.pingmanager.ui.infra;

import com.pingmanager.core.RegistreClubs;
import com.pingmanager.core.RegistreClubsMemoire;
import com.pingmanager.core.RegistreLicences;
import com.pingmanager.model.Club;
import com.pingmanager.model.FicheLicence;
import com.pingmanager.model.Sexe;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** B023 : licenciés et clubs importés superposés à la base existante. */
class RegistresImportesTest {

    @TempDir
    Path dossier;

    private final FicheLicence base = new FicheLicence("9100001", "ANCIEN", "Nom", Sexe.HOMME, "08910001", "Brunoy", 500);

    private RegistreLicences baseLicences() {
        return new RegistreLicences() {
            @Override
            public Optional<FicheLicence> trouverParLicence(String n) {
                return "9100001".equals(n) ? Optional.of(base) : Optional.empty();
            }

            @Override
            public List<FicheLicence> rechercher(String texte, int max) {
                return List.of(base);
            }
        };
    }

    @Test
    void import_licencies_prioritaire_sur_la_base_puis_repli() throws Exception {
        RegistresImportes ri = new RegistresImportes();
        RegistreLicences reg = ri.licences(baseLicences());
        assertEquals("ANCIEN", reg.trouverParLicence("9100001").orElseThrow().getNom());
        Path f = dossier.resolve("licencies.csv");
        Files.writeString(f, "Licence;Nom;Prénom;Sexe;N° club;Points\n9100001;NOUVEAU;Nom;M;08910001;600\n"
                + "9100002;AUTRE;Nom;F;08910001;700\n;;;;;\nmauvais;X;Y;Z;1;a\n", StandardCharsets.UTF_8);
        String resume = ri.importerLicencies(f);
        assertTrue(resume.startsWith("2 licencié(s) importé(s), 1 ligne(s) refusée(s)."), resume);
        assertEquals("NOUVEAU", reg.trouverParLicence("9100001").orElseThrow().getNom());
        assertTrue(reg.existe("9100002"));
        assertEquals(2, reg.rechercher("nom", 10).size(), "sans doublon avec la base");
        assertEquals(2, reg.listerParClub("08910001").size());
        assertEquals(2, ri.nombreLicences());
    }

    @Test
    void import_clubs_superpose_et_fichier_invalide_signale() throws Exception {
        RegistresImportes ri = new RegistresImportes();
        RegistreClubs reg = ri.clubs(new RegistreClubsMemoire()
                .ajouter(new Club("08910001", "Base", "91", "Brunoy", "a@b.fr")));
        Path f = dossier.resolve("clubs.csv");
        Files.writeString(f, "N° club;Nom;Code postal;Ville;Courriel officiel\n07510002;Paris Ping;75011;Paris;p@b.fr\n",
                StandardCharsets.UTF_8);
        ri.importerClubs(f);
        assertTrue(reg.existe("08910001"));
        assertEquals("75", reg.trouverParNumero("07510002").orElseThrow().getDepartement());
        Path mauvais = dossier.resolve("mauvais.csv");
        Files.writeString(mauvais, "Nom\nx\n", StandardCharsets.UTF_8);
        assertThrows(com.pingmanager.importation.LecteurTableur.ImportException.class, () -> ri.importerClubs(mauvais));
        assertEquals(1, ri.nombreClubs(), "import précédent conservé si le fichier est refusé");
    }
}
