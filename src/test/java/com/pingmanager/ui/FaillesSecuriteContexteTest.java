package com.pingmanager.ui;

import com.pingmanager.model.AccesClub;
import com.pingmanager.model.NiveauAcces;
import com.pingmanager.ui.infra.DonneesDemo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DÉMONSTRATIONS DE FAILLES (audit de sécurité, voir RAPPORT_SECURITE.md).
 *
 * <p>Ces tests passent <b>parce que</b> le code actuel est vulnérable. Lors de la
 * correction, chaque test devra être <b>inversé</b> (il vérifiera alors le refus) et
 * renommé sans le suffixe « faille_Sxx ».</p>
 */
class FaillesSecuriteContexteTest {

    @TempDir
    Path dossier;

    /**
     * Ex-faille S-01, corrigée (README section 5, point 12) : le compte « ultra admin »
     * (supervision de TOUS les tournois de TOUS les clubs) n'est plus créé hors du mode
     * démo explicite ({@code -Dpingmanager.demo=true}), et aucun mot de passe n'est écrit
     * dans le code : en mode démo, il est tiré au hasard à chaque démarrage.
     */
    @Test
    void ultra_admin_absent_hors_mode_demo() {
        ContexteApplication ctx = new ContexteApplication(dossier, DonneesDemo.clubs(), DonneesDemo.licences(), false);

        assertFalse(ctx.estModeDemo());
        assertTrue(ctx.motDePasseSupervisionDemo().isEmpty());
        assertTrue(ctx.connecter(DonneesDemo.EMAIL_ULTRA_ADMIN, "PingAdmin123/").isEmpty(),
                "l'ancien mot de passe publié n'ouvre rien");
        assertTrue(ctx.memoDemo().contains("non créé"));
    }

    @Test
    void ultra_admin_en_mode_demo_avec_mot_de_passe_tire_a_chaque_demarrage() {
        ContexteApplication ctx = new ContexteApplication(dossier, DonneesDemo.clubs(), DonneesDemo.licences(), true);
        String premier = ctx.motDePasseSupervisionDemo().orElseThrow();
        assertTrue(premier.length() >= 10);
        assertTrue(ctx.memoDemo().contains(premier), "affiché dans la console de ce démarrage");
        assertTrue(ctx.connecter(DonneesDemo.EMAIL_ULTRA_ADMIN, premier).isPresent());
        assertTrue(ctx.estUltraAdmin(), "accès supervision à tous les clubs en mode démo");
        ctx.fermer();

        ContexteApplication ctx2 = new ContexteApplication(dossier, DonneesDemo.clubs(), DonneesDemo.licences(), true);
        String second = ctx2.motDePasseSupervisionDemo().orElseThrow();
        assertNotEquals(premier, second);
        assertTrue(ctx2.connecter(DonneesDemo.EMAIL_ULTRA_ADMIN, premier).isEmpty(), "l'ancien tirage n'ouvre plus");
        assertTrue(ctx2.connecter(DonneesDemo.EMAIL_ULTRA_ADMIN, second).isPresent());
        ctx2.fermer();
    }

    @Test
    void compte_de_supervision_existant_sans_privilege_hors_mode_demo() {
        ContexteApplication demo = new ContexteApplication(dossier, DonneesDemo.clubs(), DonneesDemo.licences(), true);
        String mdp = demo.motDePasseSupervisionDemo().orElseThrow();
        demo.fermer();

        ContexteApplication ctx = new ContexteApplication(dossier, DonneesDemo.clubs(), DonneesDemo.licences(), false);
        assertTrue(ctx.connecter(DonneesDemo.EMAIL_ULTRA_ADMIN, mdp).isPresent());
        assertTrue(ctx.compteConnecte().estSupervision(), "le rôle reste enregistré dans le compte");
        assertFalse(ctx.estUltraAdmin(), "mais le privilège n'est honoré qu'en mode démo");
    }

    /**
     * Ex-faille S-02, corrigée (README section 5, point 11) : le profil pré-chargé n'est
     * plus un compte personnel réel. Identité fictive (email en example.org, licence et
     * date de naissance inventées), mot de passe clairement fictif. Il reste créé sur
     * chaque poste, administrateur du club de démo de Brunoy (question en section 6).
     */
    @Test
    void profil_de_demo_fictif_sans_donnee_personnelle_reelle() {
        ContexteApplication ctx = new ContexteApplication(dossier, DonneesDemo.clubs(), DonneesDemo.licences());

        assertTrue(ctx.connecter(DonneesDemo.EMAIL_PROFIL_DEMO, DonneesDemo.MDP_PROFIL_DEMO).isPresent());
        assertTrue(ctx.estOrganisateur(), "le profil de démo administre le club de démo");
        assertTrue(DonneesDemo.EMAIL_PROFIL_DEMO.endsWith("@example.org"));
        assertTrue(DonneesDemo.EMAIL_REFERENT_BRUNOY.endsWith("@example.org"));
        assertTrue(ctx.ficheLicenceLiee().orElseThrow().getNumeroLicence().startsWith("99"),
                "licence fictive (plage 99…, celle des joueurs fictifs)");
    }

    /** Garde-fou du point 11 : aucune adresse de messagerie personnelle dans les sources. */
    @Test
    void aucune_adresse_gmail_dans_les_sources() throws Exception {
        for (String racine : List.of("src/main/java", "src/test/java")) {
            try (var fichiers = Files.walk(Path.of(racine))) {
                for (Path f : fichiers.filter(p -> p.toString().endsWith(".java")).toList()) {
                    String texte = Files.readString(f).toLowerCase();
                    assertFalse(texte.contains("@" + "gmail."), "adresse personnelle dans " + f);
                }
            }
        }
    }

    /**
     * Ex-faille S-03, corrigée (README section 5, point 13) : le référent de Brunoy a un
     * accès principal pré-enregistré par email, sans compte. Créer un compte avec cette
     * adresse ne donne plus aucun droit tant que l'email n'est pas confirmé par le code
     * envoyé à cette adresse (que seul le vrai référent reçoit).
     */
    @Test
    void compte_cree_avec_l_email_du_referent_sans_aucun_droit_avant_verification() {
        ContexteApplication ctx = new ContexteApplication(dossier, DonneesDemo.clubs(), DonneesDemo.licences());

        assertTrue(ctx.creerCompteEtConnecter(DonneesDemo.EMAIL_REFERENT_BRUNOY, "USURPATEUR", "Ivan", "x1234567890")
                .isPresent());
        assertFalse(ctx.emailVerifie());
        assertTrue(ctx.accesCourants().isEmpty(), "aucun accès avant la vérification de l'email");
        assertFalse(ctx.estOrganisateur());
        assertTrue(ctx.mesTournoisClub(ctx.tousLesTournois()).isEmpty());
        assertEquals(DonneesDemo.EMAIL_REFERENT_BRUNOY, ctx.email().dernierMessage().orElseThrow().getDestinataire(),
                "le code part à l'adresse du compte, donc chez le vrai référent");

        // Un code inventé ne suffit pas ; le code reçu à l'adresse active l'accès.
        assertFalse(ctx.confirmerEmail("000000").estVerifie());
        assertTrue(ctx.accesCourants().isEmpty());
        assertTrue(ctx.confirmerEmail(codeRecu(ctx)).estVerifie());
        List<AccesClub> acces = ctx.accesCourants();
        assertEquals(1, acces.size());
        assertEquals(NiveauAcces.ADMIN_PRINCIPAL, acces.get(0).getNiveau());

        // L'état vérifié est enregistré : il survit à un redémarrage.
        ContexteApplication relu = new ContexteApplication(dossier, DonneesDemo.clubs(), DonneesDemo.licences());
        relu.connexion(DonneesDemo.EMAIL_REFERENT_BRUNOY, "x1234567890");
        assertTrue(relu.emailVerifie());
    }

    /** Point 13 : un compte non vérifié le reste après un redémarrage, et peut redemander un code. */
    @Test
    void compte_non_verifie_reste_sans_droits_apres_redemarrage_et_redemande_un_code() {
        ContexteApplication ctx = new ContexteApplication(dossier, DonneesDemo.clubs(), DonneesDemo.licences());
        ctx.creerCompte(DonneesDemo.EMAIL_REFERENT_BRUNOY, "USURPATEUR", "Ivan", "x1234567890");

        ContexteApplication relu = new ContexteApplication(dossier, DonneesDemo.clubs(), DonneesDemo.licences());
        relu.connexion(DonneesDemo.EMAIL_REFERENT_BRUNOY, "x1234567890");
        assertFalse(relu.emailVerifie());
        assertTrue(relu.accesCourants().isEmpty());
        assertEquals(com.pingmanager.core.ResultatVerificationEmail.Statut.AUCUNE_DEMANDE,
                relu.confirmerEmail("123456").getStatut(), "la demande d'avant le redémarrage n'existe plus");
        assertTrue(relu.envoyerCodeEmail());
        assertTrue(relu.confirmerEmail(codeRecu(relu)).estVerifie());
        assertFalse(relu.accesCourants().isEmpty());
    }

    /** Code à 6 chiffres du dernier email « envoyé » (emails à la console). */
    static String codeRecu(ContexteApplication ctx) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\b(\\d{6})\\b")
                .matcher(ctx.email().dernierMessage().orElseThrow().getCorps());
        assertTrue(m.find());
        return m.group(1);
    }

    /** Point 15 : la politique de mot de passe s'applique à la création, avec un message clair. */
    @Test
    void creation_de_compte_refusee_avec_un_mot_de_passe_de_moins_de_10_caracteres() {
        ContexteApplication ctx = new ContexteApplication(dossier, DonneesDemo.clubs(), DonneesDemo.licences());

        com.pingmanager.core.ResultatCreationCompte r = ctx.creerCompte("nouveau@example.org", "NOUVEAU", "Nina", "x");
        assertEquals(com.pingmanager.core.ResultatCreationCompte.Motif.MOT_DE_PASSE_TROP_COURT, r.getMotif());
        assertEquals("Le mot de passe doit contenir au moins 10 caractères (1 saisi).", r.getMessage());
        assertTrue(ctx.compteConnecte() == null, "personne n'est connecté");
        assertTrue(ctx.creerCompte("nouveau@example.org", "NOUVEAU", "Nina", "0123456789").estCree());
    }

    /** Point 15 : le profil de démo existant reste utilisable, et la connexion est limitée. */
    @Test
    void connexion_bloquee_apres_cinq_echecs_meme_avec_le_bon_mot_de_passe() {
        ContexteApplication ctx = new ContexteApplication(dossier, DonneesDemo.clubs(), DonneesDemo.licences());
        for (int i = 0; i < 5; i++) {
            assertEquals(com.pingmanager.core.ResultatConnexion.Motif.IDENTIFIANTS_INCORRECTS,
                    ctx.connexion(DonneesDemo.EMAIL_PROFIL_DEMO, "faux-" + i).getMotif());
        }
        com.pingmanager.core.ResultatConnexion r = ctx.connexion(DonneesDemo.EMAIL_PROFIL_DEMO, DonneesDemo.MDP_PROFIL_DEMO);
        assertEquals(com.pingmanager.core.ResultatConnexion.Motif.TROP_D_ESSAIS, r.getMotif());
        assertEquals("Trop d'essais infructueux : réessayez dans 1 min.", r.getMessage());
        assertTrue(ctx.compteConnecte() == null);
    }

    /**
     * Faille S-15 (RGPD) : le dépôt applicatif est un JSON en clair qui contient les
     * emails, noms et empreintes de mot de passe de tous les comptes du poste.
     * (Sur Linux/macOS il est créé en 0600 grâce au fichier temporaire ; sur
     * Windows il hérite des droits du profil.) Aucun chiffrement ni purge.
     */
    @Test
    void depot_applicatif_en_clair_avec_emails_et_empreintes_faille_S15() throws Exception {
        ContexteApplication ctx = new ContexteApplication(dossier, DonneesDemo.clubs(), DonneesDemo.licences());
        String contenu = Files.readString(ctx.fichierDepot());

        assertTrue(contenu.contains(DonneesDemo.EMAIL_PROFIL_DEMO));
        assertTrue(contenu.contains("pbkdf2$120000$"), "empreintes lisibles : attaque hors ligne possible");
    }
}
