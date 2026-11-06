package com.pingmanager.core;

import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.GrilleRecompenses;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Recompense;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.TarifInscription;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.format.RegistreFormats;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Vérification (B036) : cas limites et cohérence avec {@link RemplissageTableaux#montantARegler}. */
class ServiceRemplissageFinancesCoherenceTest {

    private final ServiceRemplissageFinances service = new ServiceRemplissageFinances();
    private int licence = 5000;

    private Tableau tableau(String nom, int places, int attente, TarifInscription tarif, GrilleRecompenses grille) {
        ConfigurationTableau.Builder b = ConfigurationTableau.builder()
                .format(RegistreFormats.formatParDefaut()).placesMax(places).tailleMaxListeAttente(attente);
        if (tarif != null) b.tarif(tarif);
        if (grille != null) b.recompenses(grille);
        return new Tableau(nom, b.build());
    }

    private Joueur joueur() {
        licence++;
        return new Joueur(String.valueOf(licence), "Nom" + licence, "Prenom", Sexe.HOMME, "Club", 800);
    }

    private Tournoi tournoi(Tableau... tabs) {
        Tournoi t = new Tournoi("Open", "08911132", LocalDate.of(2027, 4, 10));
        for (Tableau tab : tabs) t.ajouterTableau(tab);
        return t;
    }

    @Test
    void reste_a_encaisser_egal_somme_des_montants_a_regler_des_joueurs() {
        Tableau a = tableau("A", 10, 0, new TarifInscription(8, 10), null);
        Tableau b = tableau("B", 10, 0, new TarifInscription(5.5, 7), null);
        Tournoi t = tournoi(a, b);
        Joueur j1 = joueur(), j2 = joueur(), j3 = joueur();
        a.inscrire(j1); b.inscrire(j1);
        a.inscrire(j2);
        b.inscrire(j3);
        a.marquerPaye(j1);
        b.marquerPaye(j3);

        Set<Joueur> tous = new HashSet<>();
        for (Tableau tab : t.getTableaux()) tous.addAll(tab.getInscrits());
        double du = 0;
        for (Joueur j : tous) du += RemplissageTableaux.montantARegler(t, j);

        BilanRemplissageFinances bilan = service.calculer(t);
        assertEquals(du, bilan.resteAEncaisser(), 0.001, "une seule vérité pour le montant dû");
        assertEquals(8 + 8 + 5.5 + 5.5, bilan.recettesAttendues(), 0.001);
        assertEquals(8 + 5.5, bilan.encaisse(), 0.001);
    }

    @Test
    void joueur_en_liste_d_attente_puis_repeche_entre_dans_les_recettes_attendues() {
        Tableau a = tableau("A", 1, 2, new TarifInscription(8, 10), null);
        Tournoi t = tournoi(a);
        Joueur j1 = joueur(), j2 = joueur();
        a.inscrire(j1);
        a.inscrire(j2);
        a.marquerPaye(j1);
        BilanRemplissageFinances avant = service.calculer(t);
        assertEquals(1, avant.enAttente());
        assertEquals(8.0, avant.recettesAttendues());

        a.desinscrire(j1); // j2 repêché automatiquement, j1 perd son état payé
        BilanRemplissageFinances apres = service.calculer(t);
        assertEquals(0, apres.enAttente());
        assertEquals(1, apres.inscrits());
        assertEquals(8.0, apres.recettesAttendues());
        assertEquals(0.0, apres.encaisse(), "le paiement du désinscrit ne compte plus");
        assertEquals(1.0, apres.taux());
    }

    @Test
    void joueur_retire_apres_paiement_n_est_plus_encaisse() {
        Tableau a = tableau("A", 5, 0, new TarifInscription(8, 10), null);
        Tournoi t = tournoi(a);
        Joueur j1 = joueur(), j2 = joueur();
        a.inscrire(j1);
        a.inscrire(j2);
        a.marquerPaye(j1);
        a.marquerPaye(j2);
        a.retirerInscrit(j1);
        BilanRemplissageFinances b = service.calculer(t);
        assertEquals(1, b.payes());
        assertEquals(8.0, b.encaisse());
        assertEquals(8.0, b.recettesAttendues());
    }

    @Test
    void annulation_de_paiement_remet_l_encaisse_a_zero() {
        Tableau a = tableau("A", 5, 0, new TarifInscription(8, 10), null);
        Tournoi t = tournoi(a);
        Joueur j = joueur();
        a.inscrire(j);
        a.marquerPaye(j);
        a.annulerPaiement(j);
        assertEquals(0.0, service.calculer(t).encaisse());
        assertEquals(8.0, service.calculer(t).resteAEncaisser());
    }

    @Test
    void tableau_a_zero_place_ne_divise_pas_par_zero() {
        Tableau a = tableau("A", 0, 0, new TarifInscription(8, 10), null);
        Joueur j = joueur();
        a.inscrire(j);
        BilanRemplissageFinances b = service.calculer(tournoi(a));
        assertEquals(0.0, b.taux());
        assertEquals(0.0, b.tableaux().get(0).taux());
        assertEquals(0, b.places());
        assertEquals(1, b.tableauxSansLimite());
    }

    @Test
    void taux_jamais_superieur_a_100_pour_cent_meme_si_la_capacite_est_reduite_apres_coup() {
        Tableau a = tableau("A", 10, 0, new TarifInscription(8, 10), null);
        for (int i = 0; i < 5; i++) a.inscrire(joueur());
        BilanRemplissageFinances b = service.calculer(tournoi(a));
        assertTrue(b.taux() <= 1.0);
        BilanRemplissageFinances.LigneTableau l = new BilanRemplissageFinances.LigneTableau(
                "X", 5, 3, 0, 0, 8, 40, 0, 0, 0, 0);
        assertEquals(1.0, l.taux());
    }

    @Test
    void sans_tarif_defini_aucune_recette() {
        Tableau a = tableau("A", 5, 0, null, null);
        a.inscrire(joueur());
        BilanRemplissageFinances b = service.calculer(tournoi(a));
        assertEquals(0.0, b.recettesAttendues());
    }

    @Test
    void part_des_inscriptions_arrondie_au_centime_et_total_egal_somme_des_lignes() {
        GrilleRecompenses g = GrilleRecompenses.builder()
                .place(1, Recompense.partInscriptions(33.33)).build();
        Tableau a = tableau("A", 10, 0, new TarifInscription(7.33, 8), g);
        Tableau b = tableau("B", 10, 0, new TarifInscription(7.33, 8), g);
        for (int i = 0; i < 3; i++) { a.inscrire(joueur()); b.inscrire(joueur()); }
        BilanRemplissageFinances bilan = service.calculer(tournoi(a, b));
        double somme = 0;
        for (var l : bilan.tableaux()) {
            assertEquals(Math.round(l.dotationPart() * 100) / 100.0, l.dotationPart(), 1e-9, "centimes entiers");
            somme += l.dotationPart();
        }
        assertEquals(somme, bilan.dotationPart(), 0.0051);
        assertEquals(7.33, bilan.tableaux().get(0).dotationPart(), 0.001, "33,33 % de 21,99 = 7,33");
    }

    @Test
    void part_des_inscriptions_superieure_a_100_pour_cent_plafonnee() {
        GrilleRecompenses g = GrilleRecompenses.builder()
                .place(1, Recompense.partInscriptions(80)).place(2, Recompense.partInscriptions(80)).build();
        Tableau a = tableau("A", 10, 0, new TarifInscription(10, 10), g);
        a.inscrire(joueur());
        BilanRemplissageFinances b = service.calculer(tournoi(a));
        assertEquals(10.0, b.dotationPart(), "jamais plus que les recettes attendues");
    }

    @Test
    void liste_d_attente_pleine_ou_non_eligible_n_entre_pas_dans_les_chiffres() {
        Tableau a = tableau("A", 1, 1, new TarifInscription(8, 10), null);
        Tournoi t = tournoi(a);
        a.inscrire(joueur());
        a.inscrire(joueur());
        assertEquals(Tableau.ResultatInscription.LISTE_ATTENTE_PLEINE, a.inscrire(joueur()));
        BilanRemplissageFinances b = service.calculer(t);
        assertEquals(1, b.enAttente());
        assertEquals(1, b.inscrits());
    }
}
