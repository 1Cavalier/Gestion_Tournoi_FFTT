package com.pingmanager.persistence;

import com.pingmanager.core.EnvoiEmail;
import com.pingmanager.core.GenerateurCode;
import com.pingmanager.core.HachageMotDePasse;
import com.pingmanager.core.Horloge;
import com.pingmanager.core.RegistreClubs;
import com.pingmanager.core.ResultatConfirmation;
import com.pingmanager.core.ResultatInscriptionOrganisateur;
import com.pingmanager.core.ServiceComptesOrganisateurs;
import com.pingmanager.model.AccesClub;
import com.pingmanager.model.Club;
import com.pingmanager.model.CompteOrganisateur;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Vérifie la persistance des données d'application (clubs, comptes, accès) : le
 * round-trip fichier, le branchement du dépôt comme {@link RegistreClubs}, et le
 * parcours complet inscription → confirmation → sauvegarde → rechargement, avec
 * conservation de l'email vérifié et de l'habilitation.
 */
class DepotApplicationTest {

    // --- Ports simples pour piloter le service dans le test ---

    /** Hachage réversible factice : suffixe « # » (jamais du vrai hash en test). */
    private static final class HachageFactice implements HachageMotDePasse {
        @Override public String hacher(String enClair) { return enClair + "#"; }
        @Override public boolean verifier(String enClair, String empreinte) {
            return empreinte != null && empreinte.equals(enClair + "#");
        }
    }

    private static final class CodeFixe implements GenerateurCode {
        @Override public String genererCode() { return "123456"; }
    }

    private static final class EmailIgnore implements EnvoiEmail {
        @Override public void envoyer(String destinataire, String sujet, String corps) { }
    }

    private Club club() {
        return new Club("08911132", "Ping Club", "91", "Orsay", "contact@pingclub.fr");
    }

    @Test
    void round_trip_clubs_comptes_acces() throws Exception {
        DepotApplication depot = new DepotApplication();
        depot.ajouterClub(club());

        CompteOrganisateur compte = new CompteOrganisateur(
                "claire@x.fr", "Martin", "Claire", "empreinte#", "08911132");
        compte.marquerEmailVerifie();
        depot.enregistrerCompte(compte);
        depot.enregistrerAcces(new AccesClub("08911132", "claire@x.fr", "Martin", "Claire"));

        Path fichier = Files.createTempFile("app", ".json");
        depot.sauvegarder(fichier);

        DepotApplication relu = DepotApplication.charger(fichier);
        assertEquals(1, relu.nombreDeClubs());
        assertTrue(relu.trouverParNumero("08911132").isPresent());
        assertEquals("Ping Club", relu.trouverParNumero("08911132").get().getNom());

        Optional<CompteOrganisateur> c = relu.trouverCompte("claire@x.fr");
        assertTrue(c.isPresent());
        assertTrue(c.get().isEmailVerifie(), "l'email vérifié doit survivre au rechargement");
        assertEquals("empreinte#", c.get().getEmpreinteMotDePasse());

        assertTrue(relu.aAcces("08911132", "claire@x.fr"));
        assertEquals(1, relu.accesDe("claire@x.fr").size());
    }

    @Test
    void depot_vide_si_fichier_absent() throws Exception {
        Path inexistant = Files.createTempDirectory("app").resolve("absent.json");
        DepotApplication depot = DepotApplication.charger(inexistant);
        assertEquals(0, depot.nombreDeClubs());
        assertEquals(0, depot.tousLesComptes().size());
    }

    @Test
    void email_deja_utilise_reflete_les_comptes_stockes() {
        DepotApplication depot = new DepotApplication();
        assertFalse(depot.emailDejaUtilise().test("a@x.fr"));
        depot.enregistrerCompte(new CompteOrganisateur("a@x.fr", "N", "P", "h#", "08911132"));
        assertTrue(depot.emailDejaUtilise().test("a@x.fr"));
        assertTrue(depot.emailDejaUtilise().test("A@X.FR"), "la casse de l'email ne doit pas compter");
    }

    @Test
    void parcours_complet_service_puis_persistance() throws Exception {
        DepotApplication depot = new DepotApplication();
        depot.ajouterClub(club());

        Horloge horloge = () -> Instant.parse("2026-05-02T09:00:00Z");
        ServiceComptesOrganisateurs service = new ServiceComptesOrganisateurs(
                depot,                 // le dépôt EST le RegistreClubs
                new CodeFixe(),
                new EmailIgnore(),
                new HachageFactice(),
                horloge);

        // Inscription : le dépôt fournit le contrôle d'unicité d'email.
        ResultatInscriptionOrganisateur inscription = service.inscrire(
                "claire@x.fr", "Martin", "Claire", "motdepasse", "08911132",
                depot.emailDejaUtilise());
        assertTrue(inscription.estSucces());
        depot.enregistrerCompte(inscription.getCompte()); // l'appelant persiste le compte

        // Confirmation avec le bon code : crée l'accès.
        ResultatConfirmation confirmation = service.confirmer(
                inscription.getCompte(), inscription.getDemande(), "123456");
        assertTrue(confirmation.estVerifie());
        depot.enregistrerCompte(inscription.getCompte());   // reflète l'email vérifié
        depot.enregistrerAcces(confirmation.getAcces());     // persiste l'habilitation

        // Sauvegarde puis rechargement : tout est là.
        Path fichier = Files.createTempFile("app_parcours", ".json");
        depot.sauvegarder(fichier);
        DepotApplication relu = DepotApplication.charger(fichier);

        assertTrue(relu.trouverCompte("claire@x.fr").isPresent());
        assertTrue(relu.trouverCompte("claire@x.fr").get().isEmailVerifie());
        assertTrue(relu.aAcces("08911132", "claire@x.fr"),
                "l'habilitation doit survivre à la fermeture du logiciel");

        // Un deuxième compte avec le même email est refusé après rechargement.
        ResultatInscriptionOrganisateur doublon = service.inscrire(
                "claire@x.fr", "Autre", "Personne", "x", "08911132",
                relu.emailDejaUtilise());
        assertFalse(doublon.estSucces());
    }
}
