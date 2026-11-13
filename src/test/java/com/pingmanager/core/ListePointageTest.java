package com.pingmanager.core;

import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.ModeReglement;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.TarifInscription;
import com.pingmanager.model.Tournoi;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** B041 : liste, recherche, filtres et totaux du poste de pointage. */
class ListePointageTest {

    private final Joueur eloise = new Joueur("111", "Lefèvre", "Éloïse", Sexe.FEMME, "Brunoy", 800);
    private final Joueur jp = new Joueur("222", "D'Artagnan", "Jean-Pierre", Sexe.HOMME, "Yerres", 1200);
    private final Joueur zoe = new Joueur("333", "Abel", "Zoé", Sexe.FEMME, "Brunoy", 600);
    private final Joueur attente = new Joueur("444", "Attente", "Paul", Sexe.HOMME, "Brunoy", 500);

    private Tableau a;
    private Tableau b;

    private Tournoi tournoi() {
        a = new Tableau("A", ConfigurationTableau.builder().placesMax(3).tailleMaxListeAttente(2)
                .tarif(new TarifInscription(8, 10)).build());
        b = new Tableau("B", ConfigurationTableau.builder().placesMax(8)
                .tarif(new TarifInscription(6, true, 0, false)).build());
        Tournoi t = new Tournoi("Open", "Brunoy", LocalDate.of(2026, 11, 15));
        t.ajouterTableau(a);
        t.ajouterTableau(b);
        a.inscrire(eloise);
        a.inscrire(jp);
        a.inscrire(zoe);
        a.inscrire(attente); // liste d'attente : absent de la liste
        b.inscrire(eloise);
        return t;
    }

    private static List<String> licences(List<ListePointage.Ligne> l) {
        return l.stream().map(x -> x.joueur().getNumeroLicence()).toList();
    }

    @Test
    void liste_tous_les_inscrits_tries_par_nom_sans_la_liste_d_attente() {
        ListePointage l = new ListePointage(tournoi());
        List<ListePointage.Ligne> lignes = l.lignes("", null, ListePointage.FiltreEtat.TOUS);
        assertEquals(List.of("333", "222", "111"), licences(lignes));
        assertEquals(2, lignes.get(2).engagements().size()); // Éloïse : A et B
        assertEquals(List.of("A", "B"), l.nomsTableaux());
    }

    @Test
    void recherche_insensible_casse_accents_par_prenom_et_licence() {
        ListePointage l = new ListePointage(tournoi());
        assertEquals(List.of("111"), licences(l.lignes("LEFEVRE", null, null)));
        assertEquals(List.of("111"), licences(l.lignes("eloise", null, null)));
        assertEquals(List.of("222"), licences(l.lignes("jean pierre", null, null)));
        assertEquals(List.of("222"), licences(l.lignes("d artagnan jean", null, null)));
        assertEquals(List.of("333"), licences(l.lignes("33", null, null)));
        assertTrue(l.lignes("inconnu", null, null).isEmpty());
    }

    @Test
    void filtre_par_tableau_ne_garde_que_ses_engagements() {
        ListePointage l = new ListePointage(tournoi());
        List<ListePointage.Ligne> lignes = l.lignes(null, "B", ListePointage.FiltreEtat.TOUS);
        assertEquals(List.of("111"), licences(lignes));
        assertEquals("B", lignes.get(0).engagements().get(0).tableau());
    }

    @Test
    void filtre_par_etat_tableau_par_tableau() {
        Tournoi t = tournoi();
        a.pointer(eloise);
        a.marquerPaye(jp, ModeReglement.EN_LIGNE);
        ListePointage l = new ListePointage(t);
        List<ListePointage.Ligne> pointes = l.lignes(null, null, ListePointage.FiltreEtat.POINTE);
        assertEquals(List.of("111"), licences(pointes));
        assertEquals(1, pointes.get(0).engagements().size()); // seulement A
        List<ListePointage.Ligne> nonPointes = l.lignes(null, null, ListePointage.FiltreEtat.NON_POINTE);
        assertEquals(List.of("333", "222", "111"), licences(nonPointes)); // Éloïse non pointée sur B
        assertEquals(List.of("222"), licences(l.lignes(null, null, ListePointage.FiltreEtat.PAYE)));
        assertTrue(l.lignes(null, null, ListePointage.FiltreEtat.PAYE).get(0).engagements().get(0).payeEnLigne());
        assertEquals(List.of("333", "111"), licences(l.lignes(null, null, ListePointage.FiltreEtat.NON_PAYE)));
    }

    @Test
    void totaux_par_tableau_encaisse_selon_le_mode_et_attente_au_tarif_sur_place() {
        Tournoi t = tournoi();
        a.pointer(eloise);
        a.pointer(jp);
        a.marquerPaye(jp, ModeReglement.EN_LIGNE); // 8
        a.marquerPaye(eloise, ModeReglement.ESPECES); // 10
        b.marquerPaye(eloise); // ancien paiement sans mode : en ligne seul proposé → 6
        ListePointage l = new ListePointage(t);
        ListePointage.Totaux ta = l.totaux().get(0);
        assertEquals(new ListePointage.Totaux("A", 3, 2, 2, 18, 10), ta);
        assertEquals(new ListePointage.Totaux("B", 1, 0, 1, 6, 0), l.totaux().get(1));
        assertEquals(new ListePointage.Totaux("Tous", 4, 2, 3, 24, 10), l.totalGeneral());
    }

    @Test
    void engagement_d_un_tableau_cloture_n_autorise_plus_le_desistement() {
        Tournoi t = tournoi();
        a.ouvrirPointage();
        a.pointer(eloise);
        a.pointer(jp);
        a.pointer(zoe);
        new ServicePointage(t).cloturerPointage(a);
        ListePointage.Engagement e = new ListePointage(t).lignes("zoe", "A", null).get(0).engagements().get(0);
        assertTrue(e.modifiable());
        assertFalse(e.desistementPossible());
    }

    @Test
    void montant_sans_tarif_est_nul() {
        assertEquals(0, ListePointage.montant(null, ModeReglement.ESPECES));
        assertEquals(10, ListePointage.montant(new TarifInscription(8, 10), null));
        assertEquals(8, ListePointage.montant(new TarifInscription(8, 10), ModeReglement.EN_LIGNE));
    }
}
