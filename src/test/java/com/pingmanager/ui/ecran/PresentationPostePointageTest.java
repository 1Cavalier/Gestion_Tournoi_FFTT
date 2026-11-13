package com.pingmanager.ui.ecran;

import com.pingmanager.core.ListePointage;
import com.pingmanager.core.ResultatActionPointage;
import com.pingmanager.core.ServicePostePointage;
import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.ModeReglement;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.TypeOperationPointage;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** B041 : textes de l'écran « Pointage ». */
class PresentationPostePointageTest {

    private final Joueur alice = new Joueur("100", "Martin", "Alice", Sexe.FEMME, "TT Brunoy", 1234);
    private final Joueur bruno = new Joueur("200", "Durand", "Bruno", Sexe.HOMME, "Yerres", 900);

    @Test
    void identifiant_du_poste_depuis_le_nom_de_l_ordinateur() {
        assertEquals("ACCUEIL-1", PresentationPostePointage.identifiantPoste(Map.of("COMPUTERNAME", "ACCUEIL-1")));
        assertEquals("pc", PresentationPostePointage.identifiantPoste(Map.of("HOSTNAME", " pc ")));
        assertEquals("poste-local", PresentationPostePointage.identifiantPoste(Map.of()));
    }

    @Test
    void nom_et_detail_lisibles() {
        assertEquals("MARTIN Alice", PresentationPostePointage.nomAffiche(alice));
        assertEquals("TT Brunoy · 1 234 pts · lic. 100", PresentationPostePointage.detail(alice));
    }

    @Test
    void montant_regle_a_regler_ou_gratuit() {
        assertEquals("Réglé 8 €", PresentationPostePointage.montant(
                new ListePointage.Engagement("A", true, true, ModeReglement.EN_LIGNE, 8, true, true)));
        assertEquals("À régler 10 €", PresentationPostePointage.montant(
                new ListePointage.Engagement("A", false, false, null, 10, true, true)));
        assertEquals("Gratuit", PresentationPostePointage.montant(
                new ListePointage.Engagement("A", false, false, null, 0, true, true)));
    }

    @Test
    void selecteur_tous_les_tableaux_et_compte() {
        assertNull(PresentationPostePointage.nomTableau(PresentationPostePointage.TOUS));
        assertEquals("B", PresentationPostePointage.nomTableau("B"));
        assertEquals("Tableau B", PresentationPostePointage.libelleTableau("B"));
        assertEquals("Tableau Dames", PresentationPostePointage.libelleTableau("Tableau Dames"));
        assertEquals("Aucun joueur ne correspond.", PresentationPostePointage.compte(0, 0));
        assertEquals("1 joueur", PresentationPostePointage.compte(1, 1));
        assertEquals("150 premiers joueurs sur 412 : affinez la recherche.", PresentationPostePointage.compte(150, 412));
    }

    @Test
    void message_d_un_desistement_annonce_le_repeche() {
        Tableau a = new Tableau("A", ConfigurationTableau.builder().placesMax(1).tailleMaxListeAttente(1).build());
        Tournoi t = new Tournoi("Open", "Brunoy", LocalDate.of(2026, 11, 15));
        t.ajouterTableau(a);
        a.inscrire(alice);
        a.inscrire(bruno);
        ServicePostePointage s = new ServicePostePointage(t, () -> Instant.EPOCH, "p");
        ResultatActionPointage r = s.desinscrire("A", "100", "Blessée");
        String m = PresentationPostePointage.message(r, alice, "A", TypeOperationPointage.DESINSCRIRE);
        assertTrue(m.startsWith("Désinscrit : MARTIN Alice, tableau A."), m);
        assertTrue(m.contains("DURAND Bruno est repêché"), m);
        ResultatActionPointage refus = s.pointer("A", "100");
        assertEquals("Ce joueur n'est pas (ou plus) inscrit à ce tableau.",
                PresentationPostePointage.message(refus, alice, "A", TypeOperationPointage.POINTER));
        String paye = PresentationPostePointage.message(s.marquerPaye("A", "200", ModeReglement.CHEQUE), bruno, "A",
                TypeOperationPointage.MARQUER_PAYE);
        assertEquals("Payé (chèque) : DURAND Bruno, tableau A.", paye);
    }
}
