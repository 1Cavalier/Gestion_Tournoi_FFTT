package com.pingmanager.persistence;

import com.pingmanager.model.CompteUtilisateur;
import com.pingmanager.persistence.json.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Point 13 : état « email vérifié » des comptes, relecture des anciens fichiers applicatifs. */
class VerificationEmailRoundTripTest {

    @Test
    void ancien_fichier_sans_le_champ_compte_actif_considere_verifie() {
        String ancien = "{\"version\":1,\"kind\":\"application\",\"comptesUtilisateurs\":[{"
                + "\"email\":\"ancien@example.org\",\"nom\":\"ANCIEN\",\"prenom\":\"Alain\","
                + "\"empreinteMotDePasse\":\"pbkdf2$1$a$b\",\"licenceLiee\":\"9999001\",\"role\":\"MEMBRE\"}]}";
        DepotApplication depot = DepotApplication.lire(JsonParser.analyser(ancien));
        CompteUtilisateur c = depot.trouverCompteUtilisateur("ancien@example.org").orElseThrow();
        assertTrue(c.isEmailVerifie(), "un compte déjà actif reste actif");
        assertTrue("9999001".equals(c.getLicenceLiee()), "rien n'est perdu");
    }

    @Test
    void compte_jamais_confirme_reste_sans_droits_apres_aller_retour(@TempDir Path dossier) {
        DepotApplication depot = new DepotApplication();
        CompteUtilisateur nouveau = new CompteUtilisateur("nouveau@example.org", "NOUVEAU", "Nina", "h:x");
        CompteUtilisateur verifie = new CompteUtilisateur("verifie@example.org", "VERIFIE", "Vera", "h:x");
        verifie.marquerEmailVerifie();
        depot.enregistrerCompteUtilisateur(nouveau).enregistrerCompteUtilisateur(verifie);
        Path f = dossier.resolve(DepotApplication.NOM_FICHIER);
        depot.sauvegarder(f);

        DepotApplication relu = DepotApplication.charger(f);
        assertFalse(relu.trouverCompteUtilisateur("nouveau@example.org").orElseThrow().isEmailVerifie());
        assertTrue(relu.trouverCompteUtilisateur("verifie@example.org").orElseThrow().isEmailVerifie());
    }

    @Test
    void valeur_d_un_autre_type_lue_comme_non_verifiee(@TempDir Path dossier) throws Exception {
        String pirate = "{\"version\":1,\"comptesUtilisateurs\":[{\"email\":\"x@example.org\",\"nom\":\"X\","
                + "\"prenom\":\"X\",\"empreinteMotDePasse\":\"h\",\"emailVerifie\":\"oui\"}]}";
        Path f = dossier.resolve(DepotApplication.NOM_FICHIER);
        Files.writeString(f, pirate);
        assertFalse(DepotApplication.charger(f).trouverCompteUtilisateur("x@example.org").orElseThrow().isEmailVerifie());
    }
}
