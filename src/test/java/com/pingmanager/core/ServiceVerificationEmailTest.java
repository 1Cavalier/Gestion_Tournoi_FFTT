package com.pingmanager.core;

import com.pingmanager.model.CompteUtilisateur;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Point 13 : vérification de l'email d'un compte par un code envoyé à cette adresse. */
class ServiceVerificationEmailTest {

    private final List<String[]> envoyes = new ArrayList<>();
    private Instant maintenant = Instant.parse("2026-11-13T10:00:00Z");
    private ServiceVerificationEmail service;
    private CompteUtilisateur compte;

    private static final HachageMotDePasse HACHAGE = new HachageMotDePasse() {
        @Override
        public String hacher(String enClair) {
            return "h:" + enClair;
        }

        @Override
        public boolean verifier(String enClair, String empreinte) {
            return ("h:" + enClair).equals(empreinte);
        }
    };

    @BeforeEach
    void monter() {
        int[] n = {0};
        service = new ServiceVerificationEmail(() -> String.format("%06d", ++n[0]),
                (dest, sujet, corps) -> envoyes.add(new String[] {dest, sujet, corps}),
                HACHAGE, () -> maintenant);
        compte = new CompteUtilisateur("joueur@example.org", "TEST", "Jean", "h:x");
    }

    @Test
    void compte_cree_non_verifie_et_code_envoye_a_son_propre_email() {
        assertFalse(compte.isEmailVerifie());
        DemandeVerification d = service.envoyerCode(compte);
        assertEquals("joueur@example.org", envoyes.get(0)[0]);
        assertTrue(envoyes.get(0)[2].contains("000001"));
        assertFalse(d.getEmpreinteCode().contains("000001") && !d.getEmpreinteCode().startsWith("h:"),
                "seule l'empreinte du code est gardée");
    }

    @Test
    void bon_code_verifie_l_email() {
        DemandeVerification d = service.envoyerCode(compte);
        ResultatVerificationEmail r = service.confirmer(compte, d, " 000001 ");
        assertEquals(ResultatVerificationEmail.Statut.VERIFIE, r.getStatut());
        assertTrue(compte.isEmailVerifie());
        assertEquals(ResultatVerificationEmail.Statut.DEJA_VERIFIE, service.confirmer(compte, d, "000001").getStatut());
        assertThrows(IllegalStateException.class, () -> service.envoyerCode(compte));
    }

    @Test
    void code_faux_ou_vide_refuse_sans_verifier() {
        DemandeVerification d = service.envoyerCode(compte);
        assertEquals(ResultatVerificationEmail.Statut.CODE_INCORRECT, service.confirmer(compte, d, "999999").getStatut());
        assertEquals(ResultatVerificationEmail.Statut.CODE_INCORRECT, service.confirmer(compte, d, " ").getStatut());
        assertFalse(compte.isEmailVerifie());
    }

    @Test
    void code_expire_refuse() {
        DemandeVerification d = service.envoyerCode(compte);
        maintenant = maintenant.plus(Duration.ofMinutes(31));
        assertEquals(ResultatVerificationEmail.Statut.CODE_EXPIRE, service.confirmer(compte, d, "000001").getStatut());
        assertFalse(compte.isEmailVerifie());
    }

    @Test
    void sans_demande_ou_demande_d_un_autre_compte_refuse() {
        CompteUtilisateur autre = new CompteUtilisateur("autre@example.org", "AUTRE", "Anne", "h:x");
        DemandeVerification dAutre = service.envoyerCode(autre);
        assertEquals(ResultatVerificationEmail.Statut.AUCUNE_DEMANDE, service.confirmer(compte, null, "000001").getStatut());
        assertEquals(ResultatVerificationEmail.Statut.AUCUNE_DEMANDE, service.confirmer(compte, dAutre, "000001").getStatut());
        // Une demande de rattachement à un club ne vaut pas vérification d'email.
        DemandeVerification club = new DemandeVerification(compte.getEmail(), "h:000001",
                maintenant.plusSeconds(60), "club:01000001");
        assertEquals(ResultatVerificationEmail.Statut.AUCUNE_DEMANDE, service.confirmer(compte, club, "000001").getStatut());
        assertFalse(compte.isEmailVerifie());
    }

    @Test
    void bloque_apres_cinq_codes_faux_meme_avec_le_bon_code() {
        DemandeVerification d = service.envoyerCode(compte);
        for (int i = 0; i < 5; i++) {
            service.confirmer(compte, d, "faux" + i);
        }
        ResultatVerificationEmail r = service.confirmer(compte, d, "000001");
        assertEquals(ResultatVerificationEmail.Statut.TROP_D_ESSAIS, r.getStatut());
        assertEquals("Trop d'essais infructueux : réessayez dans 1 min.", r.getMessage());
        assertFalse(compte.isEmailVerifie());
    }
}
