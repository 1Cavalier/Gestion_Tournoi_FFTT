package com.pingmanager.core;

import com.pingmanager.model.AccesClub;
import com.pingmanager.model.Club;
import com.pingmanager.model.CompteOrganisateur;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de la couche core de la brique A : {@link ServiceComptesOrganisateurs}.
 * Les ports (génération de code, email, hachage, horloge) sont implémentés en
 * mémoire ici même, ce qui rend le flux entièrement reproductible et sans réseau.
 */
class ServiceComptesOrganisateursTest {

    // --- Doubles de test (implémentations en mémoire des ports) ---------------

    /** Génère des codes déterministes : "000001", "000002", … */
    static final class GenerateurCodeSequentiel implements GenerateurCode {
        private int n = 0;
        @Override public String genererCode() {
            return String.format("%06d", ++n);
        }
    }

    /** Capture les emails envoyés. */
    static final class EnvoiEmailMemoire implements EnvoiEmail {
        record Message(String destinataire, String sujet, String corps) {}
        final List<Message> envoyes = new ArrayList<>();
        @Override public void envoyer(String destinataire, String sujet, String corps) {
            envoyes.add(new Message(destinataire, sujet, corps));
        }
        Message dernier() { return envoyes.get(envoyes.size() - 1); }
    }

    /** Hachage réversible factice : "h:" + valeur. Suffisant pour tester la logique. */
    static final class HachageFactice implements HachageMotDePasse {
        @Override public String hacher(String enClair) { return "h:" + enClair; }
        @Override public boolean verifier(String enClair, String empreinte) {
            return empreinte != null && empreinte.equals("h:" + enClair);
        }
    }

    /** Horloge contrôlable. */
    static final class HorlogeFixe implements Horloge {
        private Instant instant;
        HorlogeFixe(Instant depart) { this.instant = depart; }
        @Override public Instant maintenant() { return instant; }
        void avancer(Duration d) { instant = instant.plus(d); }
    }

    // --- Montage --------------------------------------------------------------

    private RegistreClubsMemoire registre;
    private GenerateurCodeSequentiel generateur;
    private EnvoiEmailMemoire email;
    private HachageFactice hachage;
    private HorlogeFixe horloge;
    private ServiceComptesOrganisateurs service;

    private static final String NUM_CLUB = "08911132";
    private static final String EMAIL_CLUB = "contact@brunoyctt.fr";

    @BeforeEach
    void setUp() {
        registre = new RegistreClubsMemoire()
                .ajouter(new Club(NUM_CLUB, "Brunoy CTT", "91", "Brunoy", EMAIL_CLUB));
        generateur = new GenerateurCodeSequentiel();
        email = new EnvoiEmailMemoire();
        hachage = new HachageFactice();
        horloge = new HorlogeFixe(Instant.parse("2026-01-10T09:00:00Z"));
        service = new ServiceComptesOrganisateurs(registre, generateur, email, hachage, horloge);
    }

    // --- Inscription ----------------------------------------------------------

    @Test
    void inscription_reussie_envoie_le_code_a_l_email_officiel_du_club() {
        ResultatInscriptionOrganisateur res = service.inscrire(
                "Referent.Demo@Example.org", "Boulier", "Serge", "MotDePasse1!", NUM_CLUB, e -> false);

        assertTrue(res.estSucces());
        assertEquals(ResultatInscriptionOrganisateur.Motif.CODE_ENVOYE, res.getMotif());

        CompteOrganisateur compte = res.getCompte();
        assertNotNull(compte);
        assertEquals("referent.demo@example.org", compte.getEmail());
        assertFalse(compte.isEmailVerifie(), "le compte n'est pas encore vérifié");
        assertEquals("h:MotDePasse1!", compte.getEmpreinteMotDePasse(), "mot de passe haché via le port");

        // Le code part vers l'email du CLUB, pas vers celui de la personne.
        assertEquals(1, email.envoyes.size());
        assertEquals(EMAIL_CLUB, email.dernier().destinataire());
        assertTrue(email.dernier().corps().contains("000001"), "le code en clair figure dans l'email");

        // La demande ne stocke que l'empreinte du code, jamais le clair.
        DemandeVerification demande = res.getDemande();
        assertNotNull(demande);
        assertEquals("h:000001", demande.getEmpreinteCode());
        assertEquals("referent.demo@example.org", demande.getEmail());
    }

    @Test
    void inscription_refusee_si_club_introuvable() {
        ResultatInscriptionOrganisateur res = service.inscrire(
                "s@x.fr", "Nom", "Prenom", "pwd", "00000000", e -> false);
        assertEquals(ResultatInscriptionOrganisateur.Motif.CLUB_INTROUVABLE, res.getMotif());
        assertNull(res.getCompte());
        assertTrue(email.envoyes.isEmpty(), "aucun email si le club n'existe pas");
    }

    @Test
    void inscription_refusee_si_email_deja_utilise() {
        ResultatInscriptionOrganisateur res = service.inscrire(
                "s@x.fr", "Nom", "Prenom", "pwd", NUM_CLUB, e -> true);
        assertEquals(ResultatInscriptionOrganisateur.Motif.EMAIL_DEJA_UTILISE, res.getMotif());
        assertNull(res.getCompte());
        assertTrue(email.envoyes.isEmpty());
    }

    @Test
    void inscription_sans_controle_unicite_est_toleree() {
        ResultatInscriptionOrganisateur res = service.inscrire(
                "s@x.fr", "Nom", "Prenom", "pwd", NUM_CLUB, null);
        assertTrue(res.estSucces());
    }

    // --- Confirmation ---------------------------------------------------------

    @Test
    void confirmation_reussie_verifie_le_compte_et_cree_l_acces() {
        ResultatInscriptionOrganisateur ins = service.inscrire(
                "s@x.fr", "Boulier", "Serge", "pwd", NUM_CLUB, e -> false);
        CompteOrganisateur compte = ins.getCompte();

        ResultatConfirmation conf = service.confirmer(compte, ins.getDemande(), "000001");

        assertTrue(conf.estVerifie());
        assertEquals(ResultatConfirmation.Motif.VERIFIE, conf.getMotif());
        assertTrue(compte.isEmailVerifie(), "le compte est désormais vérifié");

        AccesClub acces = conf.getAcces();
        assertNotNull(acces);
        assertEquals(NUM_CLUB, acces.getNumeroClub());
        assertEquals("s@x.fr", acces.getEmail());
        assertEquals("Boulier", acces.getNom());
        assertEquals("Serge", acces.getPrenom());
    }

    @Test
    void confirmation_refusee_si_code_incorrect() {
        ResultatInscriptionOrganisateur ins = service.inscrire(
                "s@x.fr", "Nom", "Prenom", "pwd", NUM_CLUB, e -> false);

        ResultatConfirmation conf = service.confirmer(ins.getCompte(), ins.getDemande(), "999999");
        assertEquals(ResultatConfirmation.Motif.CODE_INCORRECT, conf.getMotif());
        assertFalse(ins.getCompte().isEmailVerifie());
        assertNull(conf.getAcces());
    }

    @Test
    void confirmation_refusee_si_code_vide() {
        ResultatInscriptionOrganisateur ins = service.inscrire(
                "s@x.fr", "Nom", "Prenom", "pwd", NUM_CLUB, e -> false);
        ResultatConfirmation conf = service.confirmer(ins.getCompte(), ins.getDemande(), "   ");
        assertEquals(ResultatConfirmation.Motif.CODE_INCORRECT, conf.getMotif());
    }

    @Test
    void confirmation_refusee_si_code_expire() {
        ResultatInscriptionOrganisateur ins = service.inscrire(
                "s@x.fr", "Nom", "Prenom", "pwd", NUM_CLUB, e -> false);

        // Au-delà de la validité par défaut (30 min).
        horloge.avancer(Duration.ofMinutes(31));

        ResultatConfirmation conf = service.confirmer(ins.getCompte(), ins.getDemande(), "000001");
        assertEquals(ResultatConfirmation.Motif.CODE_EXPIRE, conf.getMotif(),
                "un code juste mais périmé est refusé");
        assertFalse(ins.getCompte().isEmailVerifie());
    }

    @Test
    void confirmation_juste_avant_expiration_reussit() {
        ResultatInscriptionOrganisateur ins = service.inscrire(
                "s@x.fr", "Nom", "Prenom", "pwd", NUM_CLUB, e -> false);
        horloge.avancer(Duration.ofMinutes(29));
        ResultatConfirmation conf = service.confirmer(ins.getCompte(), ins.getDemande(), "000001");
        assertTrue(conf.estVerifie());
    }

    @Test
    void confirmation_refuse_une_demande_d_un_autre_compte() {
        ResultatInscriptionOrganisateur ins = service.inscrire(
                "s@x.fr", "Nom", "Prenom", "pwd", NUM_CLUB, e -> false);
        CompteOrganisateur autre = new CompteOrganisateur(
                "autre@x.fr", "A", "B", "h:pwd", NUM_CLUB);
        assertThrows(IllegalArgumentException.class,
                () -> service.confirmer(autre, ins.getDemande(), "000001"));
    }

    // --- Renvoi de code -------------------------------------------------------

    @Test
    void renvoyer_code_produit_une_nouvelle_demande_valide() {
        ResultatInscriptionOrganisateur ins = service.inscrire(
                "s@x.fr", "Nom", "Prenom", "pwd", NUM_CLUB, e -> false);
        // Premier code = 000001. Le renvoi génère 000002.
        DemandeVerification demande2 = service.renvoyerCode(ins.getCompte());

        assertEquals("h:000002", demande2.getEmpreinteCode());
        assertEquals(EMAIL_CLUB, email.dernier().destinataire());

        // L'ancienne demande ne valide plus le nouveau code, la nouvelle si.
        ResultatConfirmation conf = service.confirmer(ins.getCompte(), demande2, "000002");
        assertTrue(conf.estVerifie());
    }

    @Test
    void renvoyer_code_refuse_si_deja_verifie() {
        ResultatInscriptionOrganisateur ins = service.inscrire(
                "s@x.fr", "Nom", "Prenom", "pwd", NUM_CLUB, e -> false);
        service.confirmer(ins.getCompte(), ins.getDemande(), "000001");
        assertThrows(IllegalStateException.class,
                () -> service.renvoyerCode(ins.getCompte()));
    }

    // --- Registre des clubs ---------------------------------------------------

    @Test
    void registre_clubs_trouve_par_numero_et_gere_absence() {
        assertTrue(registre.trouverParNumero(NUM_CLUB).isPresent());
        assertTrue(registre.trouverParNumero("  " + NUM_CLUB + " ").isPresent(), "tolère les espaces");
        assertTrue(registre.trouverParNumero("inconnu").isEmpty());
        assertTrue(registre.trouverParNumero(null).isEmpty());
        assertTrue(registre.trouverParNumero("  ").isEmpty());
        assertEquals(1, registre.taille());
    }

    // --- Validation de configuration -----------------------------------------

    @Test
    void validite_code_non_positive_est_refusee() {
        assertThrows(IllegalArgumentException.class,
                () -> new ServiceComptesOrganisateurs(
                        registre, generateur, email, hachage, horloge, Duration.ZERO));
    }
}
