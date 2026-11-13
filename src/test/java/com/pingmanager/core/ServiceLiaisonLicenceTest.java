package com.pingmanager.core;

import com.pingmanager.model.AccesClub;
import com.pingmanager.model.Club;
import com.pingmanager.model.CompteUtilisateur;
import com.pingmanager.model.FicheLicence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Point 14 : liaison de licence avec preuve de possession, une licence pour un compte. */
class ServiceLiaisonLicenceTest {

    private static final String CLUB = "08910001";
    private static final String AUTRE_CLUB = "08910002";
    private static final LocalDate NAISSANCE = LocalDate.of(2001, 4, 5);

    private final List<String[]> envoyes = new ArrayList<>();
    private Instant maintenant = Instant.parse("2026-11-13T10:00:00Z");
    private ServiceLiaisonLicence service;
    private CompteUtilisateur joueur;
    private CompteUtilisateur autre;
    private final List<CompteUtilisateur> comptes = new ArrayList<>();
    private final Function<String, Optional<CompteUtilisateur>> porteur =
            n -> comptes.stream().filter(c -> n.equals(c.getLicenceLiee())).findFirst();

    @BeforeEach
    void monter() {
        Map<String, FicheLicence> fiches = Map.of(
                "9990001", new FicheLicence.Builder("9990001", "JOUEUR", "Jo").club(CLUB, "Club test")
                        .dateNaissance(NAISSANCE).build(),
                "9990002", new FicheLicence.Builder("9990002", "SANSDATE", "Sam").club(CLUB, "Club test").build(),
                "9990003", new FicheLicence.Builder("9990003", "ORPHELIN", "Oscar").club("99999999", "Inconnu")
                        .dateNaissance(NAISSANCE).build());
        RegistreClubsMemoire clubs = new RegistreClubsMemoire()
                .ajouter(new Club(CLUB, "Club test", "91", "Brunoy", "referent@club-test.example.org"))
                .ajouter(new Club(AUTRE_CLUB, "Autre club", "91", "Evry", "referent@autre.example.org"));
        int[] n = {0};
        service = new ServiceLiaisonLicence(numero -> Optional.ofNullable(fiches.get(numero)), clubs,
                () -> String.format("%06d", ++n[0]), (d, s, c) -> envoyes.add(new String[] {d, s, c}),
                new HachageMotDePasse() {
                    @Override
                    public String hacher(String enClair) {
                        return "h:" + enClair;
                    }

                    @Override
                    public boolean verifier(String enClair, String empreinte) {
                        return ("h:" + enClair).equals(empreinte);
                    }
                }, () -> maintenant);
        joueur = verifie("joueur@example.org");
        autre = verifie("autre@example.org");
        comptes.add(joueur);
        comptes.add(autre);
    }

    private static CompteUtilisateur verifie(String email) {
        CompteUtilisateur c = new CompteUtilisateur(email, "NOM", "Prenom", "h:x");
        c.marquerEmailVerifie();
        return c;
    }

    private ResultatLiaisonLicence lier(CompteUtilisateur c, String licence) {
        ResultatLiaisonLicence d = service.demander(c, licence, NAISSANCE, porteur);
        String code = envoyes.get(envoyes.size() - 1)[2].replaceAll("(?s).*code : (\\d{6}).*", "$1");
        return service.confirmer(c, licence, d.getDemande(), code, porteur);
    }

    @Test
    void compte_non_verifie_ne_peut_pas_demander() {
        CompteUtilisateur nonVerifie = new CompteUtilisateur("nv@example.org", "NV", "Nv", "h:x");
        assertEquals(ResultatLiaisonLicence.Statut.EMAIL_NON_VERIFIE,
                service.demander(nonVerifie, "9990001", NAISSANCE, porteur).getStatut());
        assertEquals(0, envoyes.size());
    }

    @Test
    void licence_inconnue_refusee() {
        assertEquals(ResultatLiaisonLicence.Statut.LICENCE_INTROUVABLE,
                service.demander(joueur, "1234567", NAISSANCE, porteur).getStatut());
    }

    @Test
    void code_envoye_au_referent_du_club_de_la_licence_puis_liee() {
        ResultatLiaisonLicence d = service.demander(joueur, "9990001", NAISSANCE, porteur);
        assertEquals(ResultatLiaisonLicence.Statut.CODE_ENVOYE, d.getStatut());
        assertEquals("referent@club-test.example.org", envoyes.get(0)[0]);
        ResultatLiaisonLicence r = service.confirmer(joueur, "9990001", d.getDemande(), "000001", porteur);
        assertEquals(ResultatLiaisonLicence.Statut.LIEE, r.getStatut());
        assertEquals("9990001", joueur.getLicenceLiee());
    }

    @Test
    void fiche_sans_date_de_naissance_exige_quand_meme_le_code_du_referent() {
        ResultatLiaisonLicence d = service.demander(joueur, "9990002", null, porteur);
        assertEquals(ResultatLiaisonLicence.Statut.CODE_ENVOYE, d.getStatut());
        assertNull(joueur.getLicenceLiee(), "rien n'est lié sans le code");
    }

    @Test
    void club_sans_referent_connu_refuse() {
        assertEquals(ResultatLiaisonLicence.Statut.CLUB_SANS_REFERENT,
                service.demander(joueur, "9990003", NAISSANCE, porteur).getStatut());
    }

    @Test
    void code_d_une_autre_licence_refuse() {
        ResultatLiaisonLicence d = service.demander(joueur, "9990002", null, porteur);
        assertEquals(ResultatLiaisonLicence.Statut.AUCUNE_DEMANDE,
                service.confirmer(joueur, "9990001", d.getDemande(), "000001", porteur).getStatut());
        assertNull(joueur.getLicenceLiee());
    }

    @Test
    void code_expire_refuse() {
        ResultatLiaisonLicence d = service.demander(joueur, "9990001", NAISSANCE, porteur);
        maintenant = maintenant.plus(Duration.ofDays(8));
        assertEquals(ResultatLiaisonLicence.Statut.CODE_EXPIRE,
                service.confirmer(joueur, "9990001", d.getDemande(), "000001", porteur).getStatut());
    }

    @Test
    void licence_prise_entre_la_demande_et_le_code_refusee_au_second() {
        ResultatLiaisonLicence dAutre = service.demander(autre, "9990001", NAISSANCE, porteur);
        assertEquals(ResultatLiaisonLicence.Statut.LIEE, lier(joueur, "9990001").getStatut());
        assertEquals(ResultatLiaisonLicence.Statut.DEJA_LIEE_A_UN_AUTRE_COMPTE,
                service.confirmer(autre, "9990001", dAutre.getDemande(), "000001", porteur).getStatut());
        assertNull(autre.getLicenceLiee());
    }

    @Test
    void dates_de_naissance_fausses_bloquees_apres_cinq_essais() {
        for (int i = 0; i < 5; i++) {
            service.demander(joueur, "9990001", NAISSANCE.plusDays(i + 1), porteur);
        }
        assertEquals(ResultatLiaisonLicence.Statut.TROP_D_ESSAIS,
                service.demander(joueur, "9990001", NAISSANCE, porteur).getStatut());
    }

    @Test
    void compte_deja_lie_doit_d_abord_delier() {
        lier(joueur, "9990001");
        assertEquals(ResultatLiaisonLicence.Statut.COMPTE_DEJA_LIE,
                service.demander(joueur, "9990002", null, porteur).getStatut());
    }

    @Test
    void le_titulaire_delie_sa_licence() {
        lier(joueur, "9990001");
        assertEquals(ResultatLiaisonLicence.Statut.DELIEE, service.delier(joueur, joueur, List.of()).getStatut());
        assertNull(joueur.getLicenceLiee());
        assertEquals(ResultatLiaisonLicence.Statut.AUCUNE_LICENCE_LIEE,
                service.delier(joueur, joueur, List.of()).getStatut());
    }

    @Test
    void administrateur_du_club_de_la_licence_delie_mais_pas_celui_d_un_autre_club() {
        lier(joueur, "9990001");
        CompteUtilisateur adminAutre = verifie("admin@autre.example.org");
        assertEquals(ResultatLiaisonLicence.Statut.DROIT_INSUFFISANT, service.delier(joueur, adminAutre,
                List.of(new AccesClub(AUTRE_CLUB, adminAutre.getEmail()))).getStatut());
        assertEquals(ResultatLiaisonLicence.Statut.DROIT_INSUFFISANT, service.delier(joueur, autre, List.of())
                .getStatut(), "un simple compte ne délie pas la licence d'un autre");
        CompteUtilisateur admin = verifie("admin@club-test.example.org");
        assertEquals(ResultatLiaisonLicence.Statut.DROIT_INSUFFISANT, service.delier(joueur, admin,
                List.of(new AccesClub(CLUB, "quelquun@example.org"))).getStatut(), "accès d'un autre compte");
        assertEquals(ResultatLiaisonLicence.Statut.DELIEE,
                service.delier(joueur, admin, List.of(new AccesClub(CLUB, admin.getEmail()))).getStatut());
        assertNull(joueur.getLicenceLiee());
    }
}
