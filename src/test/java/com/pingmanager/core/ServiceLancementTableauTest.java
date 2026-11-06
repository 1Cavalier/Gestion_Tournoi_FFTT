package com.pingmanager.core;

import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.StatutTableau;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.format.FormatTournoi;
import com.pingmanager.model.format.RegistreFormats;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ServiceLancementTableauTest {

    private final FormatTournoi format = RegistreFormats.formatParDefaut(); // P3Q2 : poules de 3

    private Joueur j(String licence) {
        return new Joueur(licence, "N" + licence, "P" + licence, Sexe.HOMME, "Club" + licence, 1000);
    }

    private Tableau tableauAvecFormat(String nom) {
        return new Tableau(nom, ConfigurationTableau.depuisFormat(format)
                .placesMax(0).tailleMaxListeAttente(4).build());
    }

    /** Inscrit n joueurs, ouvre puis clôture le pointage (tous présents). Retourne le tournoi. */
    private Tournoi prepareTableauPret(Tableau tableau, int nbJoueurs) {
        Tournoi t = new Tournoi("Open", "Club", LocalDate.now());
        t.ajouterTableau(tableau);
        tableau.ouvrirPointage();
        for (int i = 1; i <= nbJoueurs; i++) {
            Joueur joueur = j("J" + i);
            tableau.inscrire(joueur);
            tableau.pointer(joueur);
        }
        new ServicePointage(t).cloturerPointage(tableau);
        return t;
    }

    @Test
    void lancement_genere_les_poules_et_passe_en_lance() {
        Tableau tableau = tableauAvecFormat("A");
        Tournoi t = prepareTableauPret(tableau, 6);

        ServiceLancementTableau service = new ServiceLancementTableau();
        ResultatLancement r = service.lancer(tableau, 42L);

        assertEquals(StatutTableau.LANCE, tableau.getStatut());
        assertTrue(tableau.estLance());
        assertFalse(tableau.getPoules().isEmpty());
        // 6 joueurs en poules de 3 => 2 poules
        assertEquals(2, r.nombreDePoules());
        assertEquals(2, tableau.getPoules().size());
        assertEquals(6, r.getNombreDePresents());
        assertEquals(42L, r.getGraine());
    }

    @Test
    void lancement_refuse_si_pointage_non_cloture() {
        Tableau tableau = tableauAvecFormat("A");
        Tournoi t = new Tournoi("Open", "Club", LocalDate.now());
        t.ajouterTableau(tableau);
        tableau.ouvrirPointage();
        tableau.inscrire(j("1"));
        tableau.pointer(j("1"));
        // pas de clôture
        ServiceLancementTableau service = new ServiceLancementTableau();
        assertThrows(IllegalStateException.class, () -> service.lancer(tableau, 1L));
        assertEquals(StatutTableau.POINTAGE, tableau.getStatut());
    }

    @Test
    void lancement_refuse_si_pas_en_phase_pointage() {
        Tableau tableau = tableauAvecFormat("A");
        Tournoi t = new Tournoi("Open", "Club", LocalDate.now());
        t.ajouterTableau(tableau);
        // encore en INSCRIPTION
        ServiceLancementTableau service = new ServiceLancementTableau();
        assertThrows(IllegalStateException.class, () -> service.lancer(tableau, 1L));
    }

    @Test
    void lancement_refuse_sans_format() {
        Tableau tableau = new Tableau("A", ConfigurationTableau.builder()
                .taillePouleSouhaitee(3).qualifiesParPoule(2).build()); // pas de format
        Tournoi t = prepareTableauPret(tableau, 3);
        ServiceLancementTableau service = new ServiceLancementTableau();
        assertThrows(IllegalArgumentException.class, () -> service.lancer(tableau, 1L));
    }

    @Test
    void tirage_reproductible_avec_la_meme_graine() {
        // Deux tableaux identiques, mêmes joueurs, même graine => mêmes poules.
        Tableau t1 = tableauAvecFormat("A");
        Tableau t2 = tableauAvecFormat("B");
        Tournoi tournoi = new Tournoi("Open", "Club", LocalDate.now());
        tournoi.ajouterTableau(t1);
        tournoi.ajouterTableau(t2);
        t1.ouvrirPointage();
        t2.ouvrirPointage();
        for (int i = 1; i <= 6; i++) {
            Joueur joueur = j("J" + i);
            t1.inscrire(joueur);
            t1.pointer(joueur);
            t2.inscrire(joueur);
            t2.pointer(joueur);
        }
        ServicePointage pointage = new ServicePointage(tournoi);
        pointage.cloturerPointage(t1);
        pointage.cloturerPointage(t2);

        ServiceLancementTableau service = new ServiceLancementTableau();
        ResultatLancement r1 = service.lancer(t1, 123L);
        ResultatLancement r2 = service.lancer(t2, 123L);

        // Même structure : mêmes joueurs dans les mêmes poules (comparaison par licence).
        assertEquals(r1.nombreDePoules(), r2.nombreDePoules());
        for (int p = 0; p < r1.nombreDePoules(); p++) {
            List<Joueur> j1 = r1.getPoules().get(p).getJoueurs();
            List<Joueur> j2 = r2.getPoules().get(p).getJoueurs();
            assertEquals(licences(j1), licences(j2), "poule " + p + " identique");
        }
    }

    private List<String> licences(List<Joueur> joueurs) {
        return joueurs.stream().map(Joueur::getNumeroLicence).sorted().toList();
    }

    @Test
    void on_ne_relance_pas_un_tableau_deja_lance() {
        Tableau tableau = tableauAvecFormat("A");
        Tournoi t = prepareTableauPret(tableau, 3);
        ServiceLancementTableau service = new ServiceLancementTableau();
        service.lancer(tableau, 1L);
        assertThrows(IllegalStateException.class, () -> service.lancer(tableau, 1L));
    }
}
