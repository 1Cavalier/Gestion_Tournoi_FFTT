package com.pingmanager.core;

import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ServicePointageTest {

    private Joueur j(String licence) {
        return new Joueur(licence, "N" + licence, "P" + licence, Sexe.HOMME, "Club", 1000);
    }

    private Tableau tableau(String nom, int placesMax, int tailleFile) {
        return new Tableau(nom, ConfigurationTableau.builder()
                .placesMax(placesMax)
                .tailleMaxListeAttente(tailleFile)
                .build());
    }

    private Tournoi tournoi(Tableau... tableaux) {
        Tournoi t = new Tournoi("Open", "Club", LocalDate.now());
        for (Tableau tb : tableaux) {
            t.ajouterTableau(tb);
        }
        return t;
    }

    // --- Pointage et paiement, par tableau ---

    @Test
    void pointe_et_paye_sont_faux_par_defaut() {
        Tableau a = tableau("A", 8, 4);
        Tournoi t = tournoi(a);
        Joueur joueur = j("1");
        a.inscrire(joueur);
        assertFalse(a.aPointe(joueur));
        assertFalse(a.aPaye(joueur));
    }

    @Test
    void pointer_et_payer_sont_independants() {
        Tableau a = tableau("A", 8, 4);
        Tournoi t = tournoi(a);
        Joueur joueur = j("1");
        a.inscrire(joueur);
        ServicePointage service = new ServicePointage(t);

        service.marquerPaye(a, joueur); // payé en ligne, pas encore pointé
        assertTrue(a.aPaye(joueur));
        assertFalse(a.aPointe(joueur));

        service.pointer(a, joueur);
        assertTrue(a.aPointe(joueur));
        assertTrue(a.aPaye(joueur));
    }

    @Test
    void presence_est_par_tableau_pas_globale() {
        Tableau a = tableau("A", 8, 4);
        Tableau b = tableau("B", 8, 4);
        Tournoi t = tournoi(a, b);
        Joueur joueur = j("1");
        a.inscrire(joueur);
        b.inscrire(joueur);
        ServicePointage service = new ServicePointage(t);

        service.pointer(a, joueur);
        assertTrue(a.aPointe(joueur));
        assertFalse(b.aPointe(joueur), "pointer A ne pointe pas B");
    }

    @Test
    void pointer_un_non_inscrit_leve_une_exception() {
        Tableau a = tableau("A", 8, 4);
        Tournoi t = tournoi(a);
        ServicePointage service = new ServicePointage(t);
        assertThrows(IllegalArgumentException.class, () -> service.pointer(a, j("inconnu")));
    }

    // --- Clôture : libération des non-pointés ---

    @Test
    void cloture_libere_les_inscrits_non_pointes_meme_payes() {
        Tableau a = tableau("A", 8, 4);
        Tournoi t = tournoi(a);
        Joueur pointe = j("pointe");
        Joueur payeAbsent = j("payeAbsent");
        a.inscrire(pointe);
        a.inscrire(payeAbsent);
        ServicePointage service = new ServicePointage(t);
        service.pointer(a, pointe);
        service.marquerPaye(a, payeAbsent); // payé mais pas pointé => libéré quand même

        ResultatPointage r = service.cloturerPointage(a);
        assertEquals(1, r.nombreLiberes());
        assertTrue(r.getLiberes().contains(payeAbsent));
        assertTrue(a.getInscrits().contains(pointe));
        assertFalse(a.getInscrits().contains(payeAbsent));
    }

    // --- Repêchage : tout repêché est à pointer ---

    @Test
    void repeche_est_toujours_a_pointer_meme_pointe_sur_un_autre_tableau() {
        Tableau autre = tableau("Autre", 8, 4);
        Tableau a = tableau("A", 1, 4);
        Tournoi t = tournoi(autre, a);
        Joueur absent = j("absent");
        Joueur remplacant = j("remplacant");
        a.inscrire(absent);      // inscrit A
        a.inscrire(remplacant);  // file A
        autre.inscrire(remplacant);
        ServicePointage service = new ServicePointage(t);
        service.pointer(autre, remplacant); // il a pointé (et payé) l'autre tableau

        ResultatPointage r = service.cloturerPointage(a);
        assertTrue(r.getLiberes().contains(absent));
        assertTrue(r.getRepeches().contains(remplacant));
        assertTrue(a.getInscrits().contains(remplacant));
        assertFalse(a.aPointe(remplacant), "repêché doit repasser par le pointage de CE tableau");
        assertFalse(a.aPaye(remplacant), "repêché n'a pas payé CE tableau");
    }

    @Test
    void cloture_repeche_dans_l_ordre_de_la_file() {
        Tableau a = tableau("A", 2, 4);
        Tournoi t = tournoi(a);
        Joueur absent1 = j("absent1");
        Joueur absent2 = j("absent2");
        Joueur file1 = j("file1");
        Joueur file2 = j("file2");
        a.inscrire(absent1);
        a.inscrire(absent2); // capacité 2 atteinte
        a.inscrire(file1);   // file pos 1
        a.inscrire(file2);   // file pos 2
        ServicePointage service = new ServicePointage(t);
        // personne n'a pointé : les 2 inscrits partent, les 2 de la file sont repêchés

        ResultatPointage r = service.cloturerPointage(a);
        assertEquals(2, r.nombreLiberes());
        assertEquals(2, r.nombreRepeches());
        assertEquals(file1, r.getRepeches().get(0));
        assertEquals(file2, r.getRepeches().get(1));
    }

    // --- Corrections manuelles du juge-arbitre ---

    @Test
    void ja_peut_confirmer_un_repeche_arrive_en_retard() {
        Tableau a = tableau("A", 1, 4);
        Tournoi t = tournoi(a);
        Joueur absent = j("absent");
        Joueur remplacant = j("remplacant");
        a.inscrire(absent);
        a.inscrire(remplacant);
        ServicePointage service = new ServicePointage(t);
        service.cloturerPointage(a); // remplacant repêché « à pointer »

        assertFalse(a.aPointe(remplacant));
        service.confirmerRepeche(a, remplacant);
        assertTrue(a.aPointe(remplacant));
    }

    @Test
    void ja_peut_retirer_un_repeche_absent_et_repecher_le_suivant() {
        Tableau a = tableau("A", 1, 4);
        Tournoi t = tournoi(a);
        Joueur absent = j("absent");
        Joueur remplacant1 = j("remplacant1");
        Joueur remplacant2 = j("remplacant2");
        a.inscrire(absent);
        a.inscrire(remplacant1); // file 1
        a.inscrire(remplacant2); // file 2
        ServicePointage service = new ServicePointage(t);
        service.cloturerPointage(a); // remplacant1 repêché, remplacant2 reste en file

        assertTrue(a.getInscrits().contains(remplacant1));
        Joueur suivant = service.retirerRepeche(a, remplacant1);
        assertEquals(remplacant2, suivant);
        assertTrue(a.getInscrits().contains(remplacant2));
        assertFalse(a.getInscrits().contains(remplacant1));
    }

    @Test
    void cloture_d_un_tableau_etranger_leve_une_exception() {
        Tableau a = tableau("A", 8, 4);
        Tableau etranger = tableau("X", 8, 4);
        Tournoi t = tournoi(a);
        ServicePointage service = new ServicePointage(t);
        assertThrows(IllegalArgumentException.class, () -> service.cloturerPointage(etranger));
    }
}
