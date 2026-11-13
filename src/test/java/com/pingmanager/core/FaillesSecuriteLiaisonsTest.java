package com.pingmanager.core;

import com.pingmanager.model.Club;
import com.pingmanager.model.CompteUtilisateur;
import com.pingmanager.model.FicheLicence;
import com.pingmanager.model.Sexe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DÉMONSTRATIONS DE FAILLES (audit de sécurité, voir RAPPORT_SECURITE.md).
 *
 * <p>Ces tests passent <b>parce que</b> le code actuel est vulnérable : ils figent le
 * comportement fautif pour qu'il soit visible. Lors de la correction de chaque
 * faille, le test correspondant devra être <b>inversé</b> (l'assertion décrit alors
 * le refus attendu) et renommé sans le suffixe « faille_Sxx ».</p>
 */
class FaillesSecuriteLiaisonsTest {

    /** Codes déterministes « 000001 », « 000002 »… */
    private static final class CodesSequentiels implements GenerateurCode {
        private int n;

        @Override
        public String genererCode() {
            return String.format("%06d", ++n);
        }
    }

    /** Boîte aux lettres en mémoire : on sait à qui chaque code a été envoyé. */
    private static final class BoiteMemoire implements EnvoiEmail {
        final List<String[]> envoyes = new ArrayList<>();

        @Override
        public void envoyer(String destinataire, String sujet, String corps) {
            envoyes.add(new String[] {destinataire, sujet, corps});
        }
    }

    /** Hachage factice (la logique testée ne dépend pas de l'algorithme). */
    private static final class HachageFactice implements HachageMotDePasse {
        @Override
        public String hacher(String enClair) {
            return "h:" + enClair;
        }

        @Override
        public boolean verifier(String enClair, String empreinte) {
            return ("h:" + enClair).equals(empreinte);
        }
    }

    private static final String CLUB_ATTAQUANT = "01000001";
    private static final String EMAIL_CLUB_ATTAQUANT = "president@club-attaquant.fr";
    private static final String CLUB_VICTIME = "09100002";
    private static final String EMAIL_CLUB_VICTIME = "referent@club-victime.fr";

    private BoiteMemoire boite;
    private ServiceLiaisonClub service;
    private CompteUtilisateur attaquant;

    @BeforeEach
    void monter() {
        RegistreClubsMemoire clubs = new RegistreClubsMemoire()
                .ajouter(new Club(CLUB_ATTAQUANT, "Club de l'attaquant", "01", "Bourg", EMAIL_CLUB_ATTAQUANT))
                .ajouter(new Club(CLUB_VICTIME, "Club victime", "91", "Evry", EMAIL_CLUB_VICTIME));
        boite = new BoiteMemoire();
        Instant t0 = Instant.parse("2026-11-05T10:00:00Z");
        service = new ServiceLiaisonClub(clubs, new CodesSequentiels(), boite, new HachageFactice(), () -> t0);
        attaquant = new CompteUtilisateur("president@club-attaquant.fr", "PIRATE", "Paul", "h:x");
    }

    /**
     * Faille S-04 : la {@link DemandeVerification} ne mémorise pas le club visé.
     * L'attaquant demande un code pour SON club (le code arrive dans SA boîte), puis
     * confirme ce code en indiquant le numéro d'un AUTRE club : l'accès est accordé
     * sur le club victime, dont le référent n'a jamais été sollicité.
     * Correctif : lier le numéro de club à la demande et refuser toute divergence.
     */
    @Test
    void code_recu_pour_son_club_ouvre_l_acces_a_un_autre_club_faille_S04() {
        DemandeVerification demande = service.demander(attaquant, CLUB_ATTAQUANT).orElseThrow();
        assertEquals(EMAIL_CLUB_ATTAQUANT, boite.envoyes.get(0)[0], "le code part chez l'attaquant");

        ResultatLiaisonClub r = service.confirmer(attaquant, CLUB_VICTIME, demande, "000001");

        assertTrue(r.estReussi(), "comportement vulnérable : le code d'un club ouvre un autre club");
        assertEquals(CLUB_VICTIME, r.getAcces().getNumeroClub());
        assertTrue(boite.envoyes.stream().noneMatch(m -> m[0].equals(EMAIL_CLUB_VICTIME)),
                "le référent du club victime n'a reçu aucun message");
    }

    /**
     * Faille S-08 : aucun plafond de tentatives sur le code à 6 chiffres. Après
     * 200 essais faux, le bon code est toujours accepté : rien n'invalide la demande.
     * Correctif : compteur d'essais dans la demande (5 au plus), puis invalidation.
     */
    @Test
    void code_de_rattachement_sans_limite_d_essais_faille_S08() {
        DemandeVerification demande = service.demander(attaquant, CLUB_VICTIME).orElseThrow();
        for (int essai = 100; essai < 300; essai++) {
            ResultatLiaisonClub r = service.confirmer(attaquant, CLUB_VICTIME, demande, String.format("%06d", essai));
            assertEquals(ResultatLiaisonClub.Statut.CODE_INCORRECT, r.getStatut());
        }
        assertTrue(service.confirmer(attaquant, CLUB_VICTIME, demande, "000001").estReussi(),
                "comportement vulnérable : la demande reste valable après 200 essais faux");
    }

    /**
     * Faille S-05 : lier une licence ne demande aucune preuve. N'importe quel compte
     * peut se lier à la licence d'un tiers (et plusieurs comptes à la même), puis
     * l'inscrire à des tournois en son nom et lire sa fiche (date de naissance…).
     * Correctif : unicité licence ↔ compte et preuve de possession (code envoyé à
     * l'email connu de la FFTT, ou validation par le club).
     */
    @Test
    void n_importe_quel_compte_se_lie_a_la_licence_d_un_tiers_faille_S05() {
        FicheLicence victime = new FicheLicence("9999001", "VICTIME", "Vic", Sexe.HOMME, CLUB_VICTIME, "Club victime", 1500);
        RegistreLicences registre = numero -> victime.getNumeroLicence().equals(numero)
                ? Optional.of(victime) : Optional.empty();
        ServiceLiaisonLicence liaison = new ServiceLiaisonLicence(registre);
        CompteUtilisateur intrus1 = new CompteUtilisateur("intrus1@exemple.fr", "INTRUS", "Un", "h:x");
        CompteUtilisateur intrus2 = new CompteUtilisateur("intrus2@exemple.fr", "INTRUS", "Deux", "h:x");

        assertTrue(liaison.lier(intrus1, "9999001").isPresent());
        assertTrue(liaison.lier(intrus2, "9999001").isPresent());

        assertEquals("9999001", intrus1.getLicenceLiee());
        assertEquals("9999001", intrus2.getLicenceLiee(), "deux comptes étrangers portent la même licence");
    }
}
