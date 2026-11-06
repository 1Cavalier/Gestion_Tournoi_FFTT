package com.pingmanager.ui.ecran;

import com.pingmanager.core.BilanRemplissageFinances;
import com.pingmanager.core.BilanRemplissageFinances.LigneTableau;
import com.pingmanager.core.reglement.Formats;
import com.pingmanager.ui.ecran.PresentationRemplissageFinances.Chiffre;
import com.pingmanager.ui.ecran.PresentationRemplissageFinances.Ligne;
import com.pingmanager.ui.ecran.PresentationRemplissageFinances.Tonalite;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Mise en mots de la carte « Remplissage et finances » (B036), sans JavaFX. */
class PresentationRemplissageFinancesTest {

    private static LigneTableau ligne(String nom, int inscrits, int places, int attente, int payes, double tarif,
                                      double fixe, double pct) {
        double attendues = tarif * inscrits;
        return new LigneTableau(nom, inscrits, places, attente, payes, tarif, attendues, tarif * payes, fixe, pct,
                attendues * pct / 100);
    }

    private static PresentationRemplissageFinances p(LigneTableau... lignes) {
        return new PresentationRemplissageFinances(new BilanRemplissageFinances(List.of(lignes)));
    }

    @Test
    void sans_tableau_message_vide() {
        PresentationRemplissageFinances pr = p();
        assertTrue(pr.vide());
        assertFalse(pr.messageVide().isBlank());
    }

    @Test
    void au_depart_solde_negatif_en_rouge() {
        PresentationRemplissageFinances pr = p(ligne("A", 0, 32, 0, 0, 8, 190, 0));

        List<Chiffre> c = pr.chiffres();
        assertEquals(4, c.size());
        assertEquals("0 %", c.get(0).valeur());
        assertEquals("0 / 32 places occupées", c.get(0).libelle());
        assertEquals(Formats.montant(190), c.get(2).valeur());
        assertEquals("−" + Formats.montant(190), c.get(3).valeur());
        assertEquals(Tonalite.NEGATIF, c.get(3).tonalite());
        assertEquals("finances-negatif", c.get(3).tonalite().classeCss());
        assertEquals("Aucune inscription pour l'instant.", pr.phraseRemplissage());
        assertEquals(Formats.montant(8) + " l'inscription", pr.lignes().get(0).argent());
        assertEquals("solde : encaissé − dotation", c.get(3).libelle());
    }

    @Test
    void solde_positif_en_vert_et_previsionnel() {
        PresentationRemplissageFinances pr = p(ligne("A", 20, 24, 0, 20, 10, 50, 0));

        assertEquals("+" + Formats.montant(150), pr.chiffres().get(3).valeur());
        assertEquals(Tonalite.POSITIF, pr.chiffres().get(3).tonalite());
        assertEquals("Reste à encaisser : " + Formats.montant(0) + ". Si tous les inscrits règlent, solde de +"
                + Formats.montant(150) + ".", pr.phraseSolde());
        assertEquals("83 %", pr.chiffres().get(0).valeur());
    }

    @Test
    void liste_d_attente_et_tableau_sans_limite_expliques() {
        PresentationRemplissageFinances pr = p(
                ligne("Limité", 10, 10, 3, 0, 8, 0, 0),
                ligne("Libre", 5, 0, 0, 0, 8, 0, 0));

        assertEquals("15 inscriptions · 3 en liste d'attente (rien à payer avant d'avoir une place) · "
                + "1 tableau sans limite de places, hors taux.", pr.phraseRemplissage());
        assertEquals("10 / 10 places occupées (hors tableaux sans limite)", pr.chiffres().get(0).libelle());
        List<Ligne> l = pr.lignes();
        assertEquals("10 / 10 · 100 %", l.get(0).remplissage());
        assertEquals("3 en attente", l.get(0).attente());
        assertEquals("5 inscrits · sans limite", l.get(1).remplissage());
        assertEquals(-1, l.get(1).jauge());
        assertEquals("", l.get(1).attente());
    }

    @Test
    void uniquement_des_tableaux_sans_limite_affiche_le_nombre_d_inscrits() {
        PresentationRemplissageFinances pr = p(ligne("Libre", 1, 0, 0, 0, 8, 0, 0));
        assertEquals("1", pr.chiffres().get(0).valeur());
        assertEquals("inscrit (pas de limite de places)", pr.chiffres().get(0).libelle());
        assertEquals(0.0, pr.jaugeGlobale());
    }

    @Test
    void dotation_fixe_et_en_pourcentage_par_tableau() {
        assertEquals("Pas de dotation", PresentationRemplissageFinances.dotation(ligne("A", 4, 8, 0, 0, 8, 0, 0)));
        assertEquals("Dotation " + Formats.montant(190),
                PresentationRemplissageFinances.dotation(ligne("A", 4, 8, 0, 0, 8, 190, 0)));
        assertEquals("Dotation " + Formats.montant(40) + " + 100 % des inscriptions (" + Formats.montant(90) + ")",
                PresentationRemplissageFinances.dotation(ligne("Dames", 10, 16, 0, 0, 9, 40, 100)));
        assertEquals("50 % des inscriptions (" + Formats.montant(40) + ")",
                PresentationRemplissageFinances.dotation(ligne("D", 10, 16, 0, 0, 8, 0, 50))
                        .substring("Dotation ".length()));
        PresentationRemplissageFinances pr = p(ligne("Dames", 10, 16, 0, 0, 9, 40, 100));
        assertEquals("dotation promise, dont " + Formats.montant(90) + " en part des inscriptions",
                pr.chiffres().get(2).libelle());
    }

    @Test
    void tableau_gratuit_et_montants_encaisses() {
        PresentationRemplissageFinances pr = p(ligne("Gratuit", 2, 4, 0, 0, 0, 0, 0),
                ligne("A", 4, 4, 0, 1, 10, 0, 0));
        assertEquals("Gratuit", pr.lignes().get(0).argent());
        assertEquals(Formats.montant(10) + " / " + Formats.montant(40) + " encaissés", pr.lignes().get(1).argent());
        assertEquals("encaissés sur " + Formats.montant(40) + " attendus", pr.chiffres().get(1).libelle());
    }

    @Test
    void montant_signe_sans_moins_zero() {
        assertEquals(Formats.montant(0), PresentationRemplissageFinances.signe(0));
        assertEquals(Tonalite.NEUTRE, PresentationRemplissageFinances.tonalite(0));
        assertEquals("", Tonalite.NEUTRE.classeCss());
    }
}
