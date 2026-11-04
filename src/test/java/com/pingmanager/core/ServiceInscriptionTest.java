package com.pingmanager.core;

import com.pingmanager.core.ResultatInscriptionTournoi.Motif;
import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.HorairesTableau;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.RegleTableauxParJour;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.ordonnancement.ValidationInscription;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ServiceInscriptionTest {

    private Joueur joueur(String licence, int points, Sexe sexe) {
        return new Joueur(licence, "N" + licence, "P" + licence, sexe, "Club", points);
    }

    private Tableau tableau(String nom) {
        return new Tableau(nom, ConfigurationTableau.builder().build());
    }

    private Tableau tableauAvecDebut(String nom, LocalDateTime debut) {
        Tableau t = tableau(nom);
        t.setHoraires(new HorairesTableau(null, null, debut));
        return t;
    }

    private Tournoi tournoiAvec(Tableau... tableaux) {
        Tournoi t = new Tournoi("Open", "Club", LocalDate.now());
        for (Tableau tb : tableaux) {
            t.ajouterTableau(tb);
        }
        return t;
    }

    // --- Cas nominal ---

    @Test
    void inscription_simple_reussit() {
        Tableau a = tableau("A");
        Tournoi t = tournoiAvec(a);
        ServiceInscription service = ServiceInscription.sansContrainteSuccession();

        ResultatInscriptionTournoi r = service.inscrire(t, joueur("1", 1000, Sexe.HOMME), a);
        assertEquals(Motif.INSCRIT, r.getMotif());
        assertTrue(r.estInscrit());
        assertEquals(1, a.getInscrits().size());
    }

    @Test
    void refuse_un_joueur_non_eligible_sans_l_inscrire() {
        Tableau dames = new Tableau("Dames",
                ConfigurationTableau.builder().sexeRequis(Sexe.FEMME).build());
        Tournoi t = tournoiAvec(dames);
        ServiceInscription service = ServiceInscription.sansContrainteSuccession();

        ResultatInscriptionTournoi r = service.inscrire(t, joueur("1", 1000, Sexe.HOMME), dames);
        assertEquals(Motif.NON_ELIGIBLE, r.getMotif());
        assertTrue(dames.getInscrits().isEmpty());
        assertTrue(dames.getFileAttente().isEmpty());
    }

    @Test
    void tableau_plein_met_en_file_attente() {
        Tableau a = new Tableau("A", ConfigurationTableau.builder().placesMax(1).build());
        Tournoi t = tournoiAvec(a);
        ServiceInscription service = ServiceInscription.sansContrainteSuccession();

        assertEquals(Motif.INSCRIT, service.inscrire(t, joueur("1", 1000, Sexe.HOMME), a).getMotif());
        assertEquals(Motif.EN_FILE_ATTENTE, service.inscrire(t, joueur("2", 1000, Sexe.HOMME), a).getMotif());
    }

    @Test
    void refuse_un_joueur_deja_present() {
        Tableau a = tableau("A");
        Tournoi t = tournoiAvec(a);
        ServiceInscription service = ServiceInscription.sansContrainteSuccession();
        Joueur j = joueur("1", 1000, Sexe.HOMME);

        assertEquals(Motif.INSCRIT, service.inscrire(t, j, a).getMotif());
        assertEquals(Motif.DEJA_PRESENT, service.inscrire(t, j, a).getMotif());
    }

    @Test
    void tableau_hors_tournoi_leve_une_exception() {
        Tableau a = tableau("A");
        Tableau etranger = tableau("X");
        Tournoi t = tournoiAvec(a);
        ServiceInscription service = ServiceInscription.sansContrainteSuccession();

        assertThrows(IllegalArgumentException.class,
                () -> service.inscrire(t, joueur("1", 1000, Sexe.HOMME), etranger));
    }

    // --- Quota de tableaux par jour ---

    @Test
    void quota_atteint_refuse_le_tableau_de_trop() {
        Tableau a = tableau("A");
        Tableau b = tableau("B");
        Tableau c = tableau("C");
        Tournoi t = tournoiAvec(a, b, c);
        t.setRegleTableauxParJour(RegleTableauxParJour.quota(2));
        ServiceInscription service = ServiceInscription.sansContrainteSuccession();
        Joueur j = joueur("1", 1000, Sexe.HOMME);

        assertEquals(Motif.INSCRIT, service.inscrire(t, j, a).getMotif());
        assertEquals(Motif.INSCRIT, service.inscrire(t, j, b).getMotif());
        assertEquals(Motif.QUOTA_TABLEAUX_ATTEINT, service.inscrire(t, j, c).getMotif());
    }

    @Test
    void exception_feminine_sans_condition_accorde_un_tableau_de_plus() {
        Tableau a = tableau("A");
        Tableau b = tableau("B");
        Tableau c = tableau("C");
        Tournoi t = tournoiAvec(a, b, c);
        t.setRegleTableauxParJour(RegleTableauxParJour.quotaAvecBonusFemininSansCondition(2));
        ServiceInscription service = ServiceInscription.sansContrainteSuccession();

        Joueur homme = joueur("H", 1000, Sexe.HOMME);
        assertEquals(Motif.INSCRIT, service.inscrire(t, homme, a).getMotif());
        assertEquals(Motif.INSCRIT, service.inscrire(t, homme, b).getMotif());
        assertEquals(Motif.QUOTA_TABLEAUX_ATTEINT, service.inscrire(t, homme, c).getMotif());

        Joueur femme = joueur("F", 1000, Sexe.FEMME);
        assertEquals(Motif.INSCRIT, service.inscrire(t, femme, a).getMotif());
        assertEquals(Motif.INSCRIT, service.inscrire(t, femme, b).getMotif());
        assertEquals(Motif.INSCRIT, service.inscrire(t, femme, c).getMotif(), "bonus féminin : 3e tableau permis");
    }

    @Test
    void exception_feminine_conditionnee_exige_le_tableau_special() {
        Tableau special = tableau("Dames");
        Tableau a = tableau("A");
        Tableau b = tableau("B");
        Tableau c = tableau("C");
        Tournoi t = tournoiAvec(special, a, b, c);
        t.setRegleTableauxParJour(
                RegleTableauxParJour.quotaAvecBonusFemininSiTableauSpecial(2, special));
        ServiceInscription service = ServiceInscription.sansContrainteSuccession();

        // Femme SANS le tableau spécial : quota de base = 2.
        Joueur femmeSansSpecial = joueur("F1", 1000, Sexe.FEMME);
        assertEquals(Motif.INSCRIT, service.inscrire(t, femmeSansSpecial, a).getMotif());
        assertEquals(Motif.INSCRIT, service.inscrire(t, femmeSansSpecial, b).getMotif());
        assertEquals(Motif.QUOTA_TABLEAUX_ATTEINT, service.inscrire(t, femmeSansSpecial, c).getMotif());

        // Femme AVEC le tableau spécial : bonus accordé (3 tableaux au total).
        Joueur femmeAvecSpecial = joueur("F2", 1000, Sexe.FEMME);
        assertEquals(Motif.INSCRIT, service.inscrire(t, femmeAvecSpecial, special).getMotif());
        assertEquals(Motif.INSCRIT, service.inscrire(t, femmeAvecSpecial, a).getMotif());
        assertEquals(Motif.INSCRIT, service.inscrire(t, femmeAvecSpecial, b).getMotif(),
                "spécial + 2 tableaux = 3, autorisé par le bonus");
    }

    // --- Règle « pas deux tableaux qui se suivent » ---

    @Test
    void tableaux_successifs_refuses_quand_regle_active() {
        LocalDateTime base = LocalDateTime.of(2026, 1, 10, 9, 0);
        Tableau a = tableauAvecDebut("A", base);
        Tableau b = tableauAvecDebut("B", base.plusMinutes(30));
        Tournoi t = tournoiAvec(a, b);

        ValidationInscription v = new ValidationInscription(true, Duration.ofMinutes(45));
        v.definirTousLesTableaux(List.of(a, b));
        ServiceInscription service = new ServiceInscription(v);
        Joueur j = joueur("1", 1000, Sexe.HOMME);

        assertEquals(Motif.INSCRIT, service.inscrire(t, j, a).getMotif());
        ResultatInscriptionTournoi r = service.inscrire(t, j, b);
        assertEquals(Motif.TABLEAUX_SUCCESSIFS, r.getMotif());
        assertEquals(a, r.getTableauEnConflit());
        assertTrue(b.getInscrits().isEmpty(), "refus successif ne doit pas inscrire ni mettre en attente");
    }

    @Test
    void tableaux_eloignes_autorises() {
        LocalDateTime base = LocalDateTime.of(2026, 1, 10, 9, 0);
        Tableau a = tableauAvecDebut("A", base);
        Tableau b = tableauAvecDebut("B", base.plusHours(4));
        Tournoi t = tournoiAvec(a, b);

        ValidationInscription v = new ValidationInscription(true, Duration.ofMinutes(45));
        v.definirTousLesTableaux(List.of(a, b));
        ServiceInscription service = new ServiceInscription(v);
        Joueur j = joueur("1", 1000, Sexe.HOMME);

        assertEquals(Motif.INSCRIT, service.inscrire(t, j, a).getMotif());
        assertEquals(Motif.INSCRIT, service.inscrire(t, j, b).getMotif());
    }
}
