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
     * Faille S-01 : le compte « ultra admin » (supervision de TOUS les tournois de
     * TOUS les clubs) est créé à chaque installation avec un mot de passe écrit dans
     * le code, la documentation et la console. Toute personne qui a lu la doc ou
     * décompilé le jar l'ouvre sur n'importe quel poste client.
     * Correctif : aucun compte privilégié créé par défaut dans la version vendue.
     */
    @Test
    void ultra_admin_ouvert_avec_le_mot_de_passe_ecrit_dans_le_code_faille_S01() {
        ContexteApplication ctx = new ContexteApplication(dossier, DonneesDemo.clubs(), DonneesDemo.licences());

        assertTrue(ctx.connecter(DonneesDemo.EMAIL_ULTRA_ADMIN, DonneesDemo.MDP_ULTRA_ADMIN).isPresent());
        assertTrue(ctx.estUltraAdmin(), "accès supervision à tous les clubs");
        assertTrue(ctx.memoDemo().contains(DonneesDemo.MDP_ULTRA_ADMIN), "mot de passe affiché dans la console au démarrage");
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
     * Faille S-03 : l'accès « administrateur principal » du référent de Brunoy est
     * pré-enregistré par email, sans compte. La création de compte ne vérifiant pas
     * l'email, quiconque crée un compte avec cette adresse (mot de passe d'un seul
     * caractère accepté) hérite immédiatement de l'accès principal au club.
     * Correctif : vérifier l'email à la création (code envoyé à l'adresse) avant
     * d'activer les accès pré-enregistrés ; politique de mot de passe minimale.
     */
    @Test
    void compte_cree_avec_l_email_du_referent_herite_de_l_acces_principal_faille_S03() {
        ContexteApplication ctx = new ContexteApplication(dossier, DonneesDemo.clubs(), DonneesDemo.licences());

        assertTrue(ctx.creerCompteEtConnecter(DonneesDemo.EMAIL_REFERENT_BRUNOY, "USURPATEUR", "Ivan", "x")
                .isPresent(), "aucune vérification d'email, mot de passe d'un caractère accepté");

        List<AccesClub> acces = ctx.accesCourants();
        assertEquals(1, acces.size());
        assertEquals(DonneesDemo.CLUB_BRUNOY, acces.get(0).getNumeroClub());
        assertEquals(NiveauAcces.ADMIN_PRINCIPAL, acces.get(0).getNiveau(), "accès principal obtenu sans aucune preuve");
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
