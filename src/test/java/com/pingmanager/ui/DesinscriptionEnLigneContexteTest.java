package com.pingmanager.ui;

import com.pingmanager.core.JoueurDepuisLicence;
import com.pingmanager.core.RemplissageTableaux;
import com.pingmanager.core.ServiceAgenda;
import com.pingmanager.core.ServiceInscription;
import com.pingmanager.core.ServiceInscriptionEnLigne;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.StatutTournoi;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.ui.infra.DonneesDemo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** « Mes tournois » (B031) : la désinscription enregistre le tournoi et l'agenda du compte. */
class DesinscriptionEnLigneContexteTest {

    @TempDir
    Path dossier;

    private ContexteApplication ouvrir() {
        ContexteApplication ctx = new ContexteApplication(dossier, DonneesDemo.clubs(), DonneesDemo.licences());
        assertTrue(ctx.connecter(DonneesDemo.EMAIL_QUENTIN, "Soumet123/").isPresent());
        return ctx;
    }

    /**
     * Publie le National B (brouillon dans la démo), y inscrit le compte de démo à un tableau
     * encore en inscription et renvoie ce tournoi tel que le voit « Mes tournois ». Son seul
     * autre tournoi est la copie au jour J, où ses tableaux sont déjà en pointage.
     */
    private static ContexteApplication.TournoiFichier inscrit(ContexteApplication ctx, String licence) {
        Path nb = ctx.fichierTournoiDemo().toAbsolutePath();
        Tournoi t = ctx.chargerTournoi(nb);
        t.changerStatut(StatutTournoi.HOMOLOGUE);
        t.changerStatut(StatutTournoi.PUBLIE);
        Joueur q = JoueurDepuisLicence.creer(ctx.ficheLicenceLiee().orElseThrow(), t);
        ServiceInscription service = ServiceInscription.sansContrainteSuccession();
        assertTrue(t.getTableaux().stream().anyMatch(tab -> service.inscrire(t, q, tab).estAccepte()));
        ctx.enregistrerTournoi(t, nb);
        return ctx.mesTournoisJoueur(ctx.tousLesTournois()).stream()
                .filter(tf -> tf.fichier().toAbsolutePath().equals(nb)).findFirst().orElseThrow();
    }

    @Test
    void desinscription_enregistre_le_tournoi_et_retire_la_date_de_l_agenda() {
        ContexteApplication ctx = ouvrir();
        String licence = ctx.ficheLicenceLiee().orElseThrow().getNumeroLicence();
        ContexteApplication.TournoiFichier tf = inscrit(ctx, licence);
        Tournoi t = tf.tournoi();
        List<Tableau> miens = t.getTableaux().stream()
                .filter(tab -> tab.getInscrits().stream().anyMatch(j -> j.getNumeroLicence().equals(licence))
                        || tab.getFileAttente().stream().anyMatch(j -> j.getNumeroLicence().equals(licence)))
                .toList();
        assertFalse(miens.isEmpty());
        // Date d'agenda présente au départ, enregistrée sur disque.
        new ServiceAgenda(() -> "id").synchroniserTournoi(ctx.agendaDuCompte(), t, licence);
        ctx.sauvegarder();
        assertTrue(ctx.agendaDuCompte().dateDuTournoi(ServiceAgenda.cleTournoi(t)).isPresent());

        ServiceInscriptionEnLigne.BilanDesinscription b = ctx.desinscrireEnLigne(tf.fichier(), t, miens);
        assertTrue(b.estValide(), b.refus());
        assertTrue(b.desinscriptionComplete());

        // Relecture à froid : tournoi et agenda enregistrés.
        ContexteApplication neuf = ouvrir();
        Tournoi relu = neuf.chargerTournoi(tf.fichier());
        assertTrue(RemplissageTableaux.participant(relu, licence).isEmpty(), "tournoi enregistré");
        assertTrue(neuf.agendaDuCompte().dateDuTournoi(ServiceAgenda.cleTournoi(relu)).isEmpty(),
                "agenda enregistré sans la date");
    }

    @Test
    void desinscription_refusee_ne_modifie_rien() {
        ContexteApplication ctx = ouvrir();
        String licence = ctx.ficheLicenceLiee().orElseThrow().getNumeroLicence();
        ContexteApplication.TournoiFichier tf = inscrit(ctx, licence);
        ServiceInscriptionEnLigne.BilanDesinscription b = ctx.desinscrireEnLigne(tf.fichier(), tf.tournoi(), List.of());
        assertFalse(b.estValide());
        assertTrue(RemplissageTableaux.participant(ctx.chargerTournoi(tf.fichier()), licence).isPresent());
    }
}
