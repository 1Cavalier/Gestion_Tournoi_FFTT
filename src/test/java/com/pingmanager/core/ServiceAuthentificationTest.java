package com.pingmanager.core;

import com.pingmanager.model.CompteUtilisateur;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Création, connexion limitée et changement de mot de passe (README section 5, point 15). */
class ServiceAuthentificationTest {

    private final ServiceComptesOrganisateursTest.HorlogeFixe horloge =
            new ServiceComptesOrganisateursTest.HorlogeFixe(Instant.parse("2026-11-13T10:00:00Z"));
    private final ServiceAuthentification service =
            new ServiceAuthentification(new ServiceComptesOrganisateursTest.HachageFactice(), horloge);
    private final Map<String, CompteUtilisateur> comptes = new HashMap<>();

    private CompteUtilisateur creer(String email, String mdp) {
        CompteUtilisateur c = service.creer(email, "NOM", "Prénom", mdp, comptes::containsKey).getCompte();
        comptes.put(c.getEmail(), c);
        return c;
    }

    private ResultatConnexion connexion(String email, String mdp) {
        return service.connexion(email, mdp, e -> Optional.ofNullable(comptes.get(e)));
    }

    @Test
    void creation_refusee_sous_dix_caracteres_avec_message() {
        ResultatCreationCompte r = service.creer("a@example.org", "N", "P", "court", null);
        assertEquals(ResultatCreationCompte.Motif.MOT_DE_PASSE_TROP_COURT, r.getMotif());
        assertEquals("Le mot de passe doit contenir au moins 10 caractères (5 saisis).", r.getMessage());
        assertTrue(service.creerCompte("a@example.org", "N", "P", "court", null).isEmpty());
    }

    @Test
    void creation_email_deja_pris_prioritaire_sur_le_mot_de_passe() {
        creer("a@example.org", "0123456789");
        assertEquals(ResultatCreationCompte.Motif.EMAIL_DEJA_UTILISE,
                service.creer("A@example.org", "N", "P", "x", comptes::containsKey).getMotif());
    }

    @Test
    void ancien_compte_au_mot_de_passe_court_se_connecte_toujours() {
        // Compte créé avant la règle (empreinte d'un mot de passe de 3 caractères).
        comptes.put("ancien@example.org", new CompteUtilisateur("ancien@example.org", "N", "P", "h:abc"));
        assertTrue(connexion("ancien@example.org", "abc").estConnecte(), "les comptes existants ne sont pas invalidés");
    }

    @Test
    void cinq_echecs_bloquent_une_minute_puis_le_bon_mot_de_passe_passe() {
        creer("a@example.org", "0123456789");
        for (int i = 0; i < 5; i++) {
            assertEquals(ResultatConnexion.Motif.IDENTIFIANTS_INCORRECTS, connexion("a@example.org", "faux").getMotif());
        }
        ResultatConnexion bloque = connexion("A@Example.org", "0123456789");
        assertEquals(ResultatConnexion.Motif.TROP_D_ESSAIS, bloque.getMotif());
        assertEquals(Duration.ofMinutes(1), bloque.getAttente());

        horloge.avancer(Duration.ofSeconds(61));
        assertTrue(connexion("a@example.org", "0123456789").estConnecte());
        assertEquals(ResultatConnexion.Motif.IDENTIFIANTS_INCORRECTS, connexion("a@example.org", "faux").getMotif(),
                "la réussite a remis le compteur à zéro");
    }

    @Test
    void blocage_croissant_apres_le_seuil() {
        creer("a@example.org", "0123456789");
        for (int i = 0; i < 5; i++) {
            connexion("a@example.org", "faux");
        }
        horloge.avancer(Duration.ofMinutes(1));
        connexion("a@example.org", "faux");
        assertEquals(Duration.ofMinutes(2), connexion("a@example.org", "0123456789").getAttente());
    }

    @Test
    void email_inconnu_compte_aussi_dans_la_limite_sans_reveler_l_absence_de_compte() {
        for (int i = 0; i < 5; i++) {
            assertEquals("Email ou mot de passe incorrect.", connexion("inconnu@example.org", "x").getMessage());
        }
        assertEquals(ResultatConnexion.Motif.TROP_D_ESSAIS, connexion("inconnu@example.org", "x").getMotif());
    }

    @Test
    void changement_exige_l_ancien_et_dix_caracteres() {
        CompteUtilisateur c = creer("a@example.org", "0123456789");
        assertEquals(ResultatChangementMotDePasse.Motif.ANCIEN_INCORRECT,
                service.changerMotDePasse(c, "faux", "nouveau-mot-de-passe").getMotif());
        ResultatChangementMotDePasse court = service.changerMotDePasse(c, "0123456789", "court");
        assertEquals(ResultatChangementMotDePasse.Motif.MOT_DE_PASSE_TROP_COURT, court.getMotif());
        assertEquals("h:0123456789", c.getEmpreinteMotDePasse(), "rien n'a changé");

        assertTrue(service.changerMotDePasse(c, "0123456789", "nouveau-mot-de-passe").estChange());
        assertTrue(connexion("a@example.org", "nouveau-mot-de-passe").estConnecte());
    }

    @Test
    void changement_soumis_a_la_limite_d_essais() {
        CompteUtilisateur c = creer("a@example.org", "0123456789");
        for (int i = 0; i < 5; i++) {
            service.changerMotDePasse(c, "faux", "nouveau-mot-de-passe");
        }
        ResultatChangementMotDePasse r = service.changerMotDePasse(c, "0123456789", "nouveau-mot-de-passe");
        assertEquals(ResultatChangementMotDePasse.Motif.TROP_D_ESSAIS, r.getMotif());
        assertEquals("h:0123456789", c.getEmpreinteMotDePasse());
    }
}
