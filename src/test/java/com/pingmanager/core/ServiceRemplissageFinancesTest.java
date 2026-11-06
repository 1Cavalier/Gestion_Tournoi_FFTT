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

import static org.junit.jupiter.api.Assertions.*;

/** Tests du tableau de bord remplissage et finances (B036). */
class ServiceRemplissageFinancesTest {

    private final ServiceRemplissageFinances service = new ServiceRemplissageFinances();
    private int licence = 1000;

    private Tableau tableau(String nom, int places, int attente, TarifInscription tarif, GrilleRecompenses grille) {
        ConfigurationTableau.Builder b = ConfigurationTableau.builder()
                .format(RegistreFormats.formatParDefaut())
                .placesMax(places)
                .tailleMaxListeAttente(attente);
        if (tarif != null) b.tarif(tarif);
        if (grille != null) b.recompenses(grille);
        return new Tableau(nom, b.build());
    }

    private Joueur joueur() {
        licence++;
        return new Joueur(String.valueOf(licence), "Nom" + licence, "Prenom", Sexe.HOMME, "Club", 800);
    }

    private Tournoi tournoi(Tableau... tableaux) {
        Tournoi t = new Tournoi("Open", "08911132", LocalDate.of(2027, 4, 10));
        for (Tableau tab : tableaux) {
            t.ajouterTableau(tab);
        }
        return t;
    }

    private static Joueur[] inscrire(Tableau tab, int n, ServiceRemplissageFinancesTest test) {
        Joueur[] js = new Joueur[n];
        for (int i = 0; i < n; i++) {
            js[i] = test.joueur();
            tab.inscrire(js[i]);
        }
        return js;
    }

    @Test
    void aucun_inscrit_solde_negatif_egal_a_la_dotation_fixe() {
        GrilleRecompenses g = GrilleRecompenses.builder()
                .place(1, Recompense.argent(100)).place(2, Recompense.argent(50))
                .plage(3, 4, Recompense.argent(20)).build();
        Tournoi t = tournoi(tableau("A", 32, 0, new TarifInscription(8, 10), g));

        BilanRemplissageFinances b = service.calculer(t);

        assertEquals(0, b.inscrits());
        assertEquals(32, b.places());
        assertEquals(0.0, b.taux());
        assertEquals(0.0, b.recettesAttendues());
        assertEquals(0.0, b.encaisse());
        assertEquals(190.0, b.dotationFixe(), "la plage 3-4 compte deux fois");
        assertEquals(190.0, b.dotationPromise());
        assertEquals(-190.0, b.solde(), "au départ, le budget est négatif");
        assertEquals(-190.0, b.soldePrevisionnel());
    }

    @Test
    void recettes_attendues_au_tarif_en_ligne_et_encaisse_selon_les_payes() {
        Tableau a = tableau("A", 10, 0, new TarifInscription(8, 10), null);
        Tournoi t = tournoi(a);
        Joueur[] js = inscrire(a, 4, this);
        a.marquerPaye(js[0]);
        a.marquerPaye(js[1]);

        BilanRemplissageFinances.LigneTableau l = service.calculer(t).tableaux().get(0);

        assertEquals(4, l.inscrits());
        assertEquals(2, l.payes());
        assertEquals(32.0, l.recettesAttendues());
        assertEquals(16.0, l.encaisse());
        assertEquals(16.0, l.resteAEncaisser());
        assertEquals(0.4, l.taux(), 1e-9);
        // Cohérence avec le montant à régler de chaque joueur (pas de calcul en double).
        double du = 0;
        for (Joueur j : js) {
            du += RemplissageTableaux.montantARegler(t, j);
        }
        assertEquals(du, l.resteAEncaisser(), 1e-9);
    }

    @Test
    void tarif_sur_place_retenu_quand_l_en_ligne_n_est_pas_propose() {
        Tableau a = tableau("A", 10, 0, new TarifInscription(0, false, 12, true), null);
        Tournoi t = tournoi(a);
        inscrire(a, 3, this);

        assertEquals(36.0, service.calculer(t).recettesAttendues());
    }

    @Test
    void dotation_en_pourcentage_des_inscriptions_suit_le_remplissage() {
        // Tableau dames : 50 / 30 / 10 / 10 % des inscriptions, plus 10 € aux quarts.
        GrilleRecompenses g = GrilleRecompenses.builder()
                .place(1, Recompense.partInscriptions(50)).place(2, Recompense.partInscriptions(30))
                .plage(3, 4, Recompense.partInscriptions(10))
                .plage(5, 8, Recompense.argent(10)).build();
        Tableau dames = tableau("Dames", 16, 0, new TarifInscription(9, 9), g);
        Tournoi t = tournoi(dames);
        Joueur[] js = inscrire(dames, 10, this);
        for (int i = 0; i < 5; i++) {
            dames.marquerPaye(js[i]);
        }

        BilanRemplissageFinances.LigneTableau l = service.calculer(t).tableaux().get(0);

        assertEquals(100.0, l.partInscriptionsPct());
        assertEquals(90.0, l.recettesAttendues());
        assertEquals(90.0, l.dotationPart(), "100 % des inscriptions attendues");
        assertEquals(40.0, l.dotationFixe());
        assertEquals(130.0, l.dotationPromise());
        assertEquals(45.0 - 130.0, l.solde());
        assertEquals(-40.0, l.soldePrevisionnel(), "un tableau à 100 % ne coûte que sa dotation fixe");
    }

    @Test
    void dotation_en_pourcentage_partielle_et_arrondie_au_centime() {
        GrilleRecompenses g = GrilleRecompenses.builder().place(1, Recompense.partInscriptions(33)).build();
        Tableau a = tableau("A", 0, 0, new TarifInscription(7.5, 7.5), g);
        Tournoi t = tournoi(a);
        inscrire(a, 3, this);

        BilanRemplissageFinances.LigneTableau l = service.calculer(t).tableaux().get(0);

        assertEquals(22.5, l.recettesAttendues());
        assertEquals(7.43, l.dotationPart(), "33 % de 22,50 € = 7,425 €, arrondi au centime");
    }

    @Test
    void liste_d_attente_comptee_mais_ne_paie_rien() {
        Tableau a = tableau("A", 2, 5, new TarifInscription(10, 10), null);
        Tournoi t = tournoi(a);
        inscrire(a, 5, this);

        BilanRemplissageFinances b = service.calculer(t);

        assertEquals(2, b.inscrits());
        assertEquals(3, b.enAttente());
        assertEquals(1.0, b.taux());
        assertEquals(20.0, b.recettesAttendues(), "seules les places obtenues sont dues");
    }

    @Test
    void tableau_sans_limite_de_places_hors_du_taux_global() {
        Tableau limite = tableau("Limité", 10, 0, new TarifInscription(8, 8), null);
        Tableau libre = tableau("Libre", 0, 0, new TarifInscription(8, 8), null);
        Tournoi t = tournoi(limite, libre);
        inscrire(limite, 5, this);
        inscrire(libre, 7, this);

        BilanRemplissageFinances b = service.calculer(t);

        assertEquals(12, b.inscrits());
        assertEquals(10, b.places(), "le tableau sans limite n'ajoute pas de places");
        assertEquals(1, b.tableauxSansLimite());
        assertEquals(0.5, b.taux(), 1e-9, "taux sur les seuls tableaux limités");
        BilanRemplissageFinances.LigneTableau l = b.tableaux().stream()
                .filter(x -> x.nom().equals("Libre")).findFirst().orElseThrow();
        assertTrue(l.sansLimite());
        assertEquals(0.0, l.taux());
        assertEquals(96.0, b.recettesAttendues(), "les inscrits du tableau libre paient aussi");
    }

    @Test
    void totaux_sur_plusieurs_tableaux_et_tableau_gratuit() {
        GrilleRecompenses g = GrilleRecompenses.builder().place(1, Recompense.argent(60))
                .place(2, Recompense.autre("Coupe")).build();
        Tableau a = tableau("A", 4, 0, new TarifInscription(10, 10), g);
        Tableau gratuit = tableau("Gratuit", 4, 0, null, null);
        Tournoi t = tournoi(a, gratuit);
        Joueur[] js = inscrire(a, 4, this);
        a.marquerPaye(js[0]);
        inscrire(gratuit, 2, this);

        BilanRemplissageFinances b = service.calculer(t);

        assertEquals(6, b.inscrits());
        assertEquals(8, b.places());
        assertEquals(0.75, b.taux(), 1e-9);
        assertEquals(40.0, b.recettesAttendues());
        assertEquals(10.0, b.encaisse());
        assertEquals(30.0, b.resteAEncaisser());
        assertEquals(60.0, b.dotationPromise(), "un lot n'est pas de l'argent");
        assertEquals(-50.0, b.solde());
        assertEquals(-20.0, b.soldePrevisionnel());
    }

    @Test
    void tournoi_sans_tableau_tout_a_zero() {
        BilanRemplissageFinances b = service.calculer(tournoi());

        assertTrue(b.tableaux().isEmpty());
        assertEquals(0, b.places());
        assertEquals(0.0, b.taux());
        assertEquals(0.0, b.solde());
    }

    @Test
    void bilan_immuable() {
        Tournoi t = tournoi(tableau("A", 4, 0, null, null));
        BilanRemplissageFinances b = service.calculer(t);
        assertThrows(UnsupportedOperationException.class, () -> b.tableaux().clear());
    }
}
