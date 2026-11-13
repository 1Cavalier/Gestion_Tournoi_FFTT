package com.pingmanager.core;

import com.pingmanager.model.AdresseTournoi;
import com.pingmanager.model.Club;
import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.FicheLicence;
import com.pingmanager.model.HorairesTableau;
import com.pingmanager.model.InfosReglement;
import com.pingmanager.model.NiveauTournoi;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.StatutTournoi;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.TarifInscription;
import com.pingmanager.model.Tournoi;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Inscription en ligne : accès IX.102, période d'inscription, tout ou rien, montants. */
class ServiceInscriptionEnLigneTest {

    private static final LocalDate JOUR = LocalDate.of(2027, 1, 17);
    private static final LocalDateTime PENDANT = LocalDateTime.of(2026, 12, 1, 10, 0);

    private final RegistreClubsMemoire clubs = new RegistreClubsMemoire();
    private final ServiceInscriptionEnLigne service;

    ServiceInscriptionEnLigneTest() {
        clubs.ajouter(new Club("08911132", "Brunoy", "91", "Brunoy", "a@b.fr"));
        clubs.ajouter(new Club("07450115", "Orléans", "45", "Orléans", "c@d.fr"));
        service = new ServiceInscriptionEnLigne(ServiceInscription.sansContrainteSuccession(), new EligibiliteTournoi(clubs));
    }

    private static Tournoi tournoi(String club, NiveauTournoi niveau) {
        Tournoi t = new Tournoi("T", "Club", JOUR);
        t.setNumeroAffiliationClub(club);
        t.setNiveau(niveau);
        t.setAdresse(AdresseTournoi.de("x", "45000", "Orléans"));
        Tableau a = new Tableau("A", ConfigurationTableau.builder().placesMax(1).tailleMaxListeAttente(2)
                .pointMax(1000).tarif(new TarifInscription(8, 10)).build());
        a.setHoraires(new HorairesTableau(null, null, JOUR.atTime(9, 0)));
        Tableau b = new Tableau("B", ConfigurationTableau.builder().placesMax(10).toutesSeries()
                .tarif(new TarifInscription(10, 12)).build());
        b.setHoraires(new HorairesTableau(null, null, JOUR.atTime(13, 0)));
        t.ajouterTableau(a);
        t.ajouterTableau(b);
        t.setInfosReglement(InfosReglement.builder().ouvertureInscriptions(LocalDateTime.of(2026, 11, 1, 0, 0))
                .clotureInscriptions(LocalDateTime.of(2027, 1, 15, 20, 0)).build());
        t.changerStatut(StatutTournoi.HOMOLOGUE);
        t.changerStatut(StatutTournoi.PUBLIE);
        return t;
    }

    private static FicheLicence fiche(String licence, String club) {
        return new FicheLicence.Builder(licence, "NOM" + licence, "Prénom").sexe(Sexe.HOMME).club(club, "Club")
                .points(923).dateNaissance(LocalDate.of(2002, 3, 15)).typeLicence("Compétition").build();
    }

    @Test
    void refus_global_acces_et_periode() {
        Tournoi dep = tournoi("07450115", NiveauTournoi.DEPARTEMENTAL);
        assertTrue(service.refusGlobal(dep, fiche("1", "08911132"), PENDANT).contains("Loiret"));
        assertNull(service.refusGlobal(dep, fiche("2", "07450115"), PENDANT));
        assertTrue(service.refusGlobal(dep, fiche("2", "07450115"), LocalDateTime.of(2026, 10, 1, 0, 0))
                .startsWith("Inscriptions à partir du"));
        assertTrue(service.refusGlobal(dep, fiche("2", "07450115"), LocalDateTime.of(2027, 1, 16, 0, 0))
                .startsWith("Inscriptions closes"));
        assertNotNull(service.refusGlobal(dep, null, PENDANT));

        var b = service.inscrire(dep, fiche("1", "08911132"), dep.getTableaux(), PENDANT);
        assertFalse(b.estValide());
        assertTrue(dep.getTableaux().get(1).getInscrits().isEmpty(), "rien n'est enregistré");
    }

    @Test
    void montants_inscrit_et_liste_d_attente() {
        Tournoi t = tournoi("08911132", NiveauTournoi.NATIONAL_B);
        assertTrue(service.inscrire(t, fiche("10", "07450115"), List.of(t.getTableaux().get(0)), PENDANT).estValide());
        var b = service.simuler(t, fiche("11", "08911132"), t.getTableaux(), PENDANT);
        assertTrue(b.estValide());
        assertEquals(10.0, b.montantInscrit(), "B inscrit");
        assertEquals(8.0, b.montantSiRepeche(), "A en liste d'attente : dû seulement si repêché");

        var fait = service.inscrire(t, fiche("11", "08911132"), t.getTableaux(), PENDANT);
        assertTrue(fait.estValide());
        assertEquals(1, t.getTableaux().get(0).getFileAttente().size());
        assertEquals(10.0, RemplissageTableaux.montantARegler(t, fait.joueur()));
        // Le même joueur (même licence) est retrouvé tel quel pour une seconde demande.
        assertSame(fait.joueur(), ServiceInscriptionEnLigne.joueurPour(t, fiche("11", "08911132")));
        assertFalse(service.simuler(t, fiche("11", "08911132"), List.of(t.getTableaux().get(1)), PENDANT).estValide(),
                "déjà inscrit");
    }

    @Test
    void liste_d_attente_rien_a_payer_puis_a_regler_apres_repechage() {
        Tournoi t = tournoi("08911132", NiveauTournoi.NATIONAL_B);
        Tableau a = t.getTableaux().get(0);
        var premier = service.inscrire(t, fiche("20", "07450115"), List.of(a), PENDANT);
        var attente = service.inscrire(t, fiche("21", "08911132"), List.of(a), PENDANT);
        assertTrue(attente.estValide());
        assertEquals(ResultatInscriptionTournoi.Motif.EN_FILE_ATTENTE, attente.parTableau().get(a).getMotif());
        assertEquals(0.0, attente.montantInscrit());
        assertEquals(8.0, attente.montantSiRepeche());
        assertEquals(0.0, RemplissageTableaux.montantARegler(t, attente.joueur()), "pas de place : rien à payer");

        // Désistement du premier : le premier en attente est promu et doit alors régler.
        a.desinscrire(premier.joueur());
        assertTrue(a.getInscrits().contains(attente.joueur()));
        assertEquals(8.0, RemplissageTableaux.montantARegler(t, attente.joueur()));
    }
}
