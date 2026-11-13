package com.pingmanager.persistence;

import com.pingmanager.model.CompteUtilisateur;
import com.pingmanager.model.RoleCompte;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Rôle des comptes utilisateurs (README section 5, point 12) : enregistré dans le compte,
 * relu à l'identique, et déduit une dernière fois de l'email pour les anciens fichiers
 * (dans la lignée de {@code RoundTripTournoiTest}).
 */
class RoleCompteRoundTripTest {

    @TempDir
    Path dossier;

    private static CompteUtilisateur compte(String email) {
        return new CompteUtilisateur(email, "NOM", "Prénom", "pbkdf2$1$sel$empreinte");
    }

    @Test
    void role_ecrit_puis_relu_a_l_identique() {
        DepotApplication depot = new DepotApplication();
        CompteUtilisateur membre = compte("membre@example.org");
        CompteUtilisateur supervision = compte("supervision@example.org");
        supervision.setRole(RoleCompte.SUPERVISION);
        depot.enregistrerCompteUtilisateur(membre);
        depot.enregistrerCompteUtilisateur(supervision);
        Path f = dossier.resolve("app.json");
        depot.sauvegarder(f);

        DepotApplication relu = DepotApplication.charger(f);
        assertEquals(RoleCompte.MEMBRE, relu.trouverCompteUtilisateur("membre@example.org").orElseThrow().getRole());
        assertEquals(RoleCompte.SUPERVISION,
                relu.trouverCompteUtilisateur("supervision@example.org").orElseThrow().getRole(),
                "le rôle vient du fichier, pas de l'email");
    }

    @Test
    void ancien_fichier_sans_role_garde_le_role_qu_avait_chaque_compte() throws Exception {
        Path f = dossier.resolve("ancien.json");
        Files.writeString(f, """
                {"version": 1, "kind": "application", "comptesUtilisateurs": [
                  {"email": "admin@pingmanager.fr", "nom": "SUPERVISION", "prenom": "PingManager",
                   "empreinteMotDePasse": "pbkdf2$1$a$b"},
                  {"email": "joueur@example.org", "nom": "DURAND", "prenom": "Paul",
                   "empreinteMotDePasse": "pbkdf2$1$a$b", "licenceLiee": "9411003"}
                ]}
                """, StandardCharsets.UTF_8);

        DepotApplication relu = DepotApplication.charger(f);
        CompteUtilisateur admin = relu.trouverCompteUtilisateur("admin@pingmanager.fr").orElseThrow();
        CompteUtilisateur joueur = relu.trouverCompteUtilisateur("joueur@example.org").orElseThrow();
        assertTrue(admin.estSupervision(), "l'ancien compte de supervision le reste");
        assertFalse(joueur.estSupervision());
        assertEquals("9411003", joueur.getLicenceLiee());

        // Réécrit : le rôle est désormais un champ du fichier.
        Path f2 = dossier.resolve("reecrit.json");
        relu.sauvegarder(f2);
        assertTrue(Files.readString(f2).contains("\"role\""));
        assertEquals(RoleCompte.SUPERVISION,
                DepotApplication.charger(f2).trouverCompteUtilisateur("admin@pingmanager.fr").orElseThrow().getRole());
    }

    @Test
    void role_inconnu_relu_comme_membre() throws Exception {
        Path f = dossier.resolve("inconnu.json");
        Files.writeString(f, """
                {"version": 1, "kind": "application", "comptesUtilisateurs": [
                  {"email": "admin@pingmanager.fr", "nom": "X", "prenom": "Y",
                   "empreinteMotDePasse": "pbkdf2$1$a$b", "role": "SUPER_ADMIN_DIVIN"}
                ]}
                """, StandardCharsets.UTF_8);
        assertEquals(RoleCompte.MEMBRE,
                DepotApplication.charger(f).trouverCompteUtilisateur("admin@pingmanager.fr").orElseThrow().getRole(),
                "valeur inconnue : le moins de droits, même avec l'ancien email de supervision");
    }

    @Test
    void compte_neuf_est_membre_par_defaut() {
        assertEquals(RoleCompte.MEMBRE, compte("x@example.org").getRole());
    }
}
