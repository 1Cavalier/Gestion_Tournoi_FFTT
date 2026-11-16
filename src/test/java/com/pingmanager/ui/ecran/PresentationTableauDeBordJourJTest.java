package com.pingmanager.ui.ecran;

import com.pingmanager.core.CalculEtapeTableau;
import com.pingmanager.core.EtapeTableau;
import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.ModeAnnonce;
import com.pingmanager.model.ReglagesAnnoncesVocales;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.StrategieOrdonnancement;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.format.RegistreFormats;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Écran du jour J restructuré : frise, actions par étape, cartes (sans JavaFX). */
class PresentationTableauDeBordJourJTest {

    private static Joueur j(int i) {
        return new Joueur("L" + i, "NOM" + i, "Prénom" + i, Sexe.HOMME, "Club " + (i % 4), 1000 + i);
    }

    @Test
    void frise_etape_courante_mise_en_avant_et_etapes_passees_et_a_venir() {
        List<PresentationTableauDeBordJourJ.Pas> pas = PresentationTableauDeBordJourJ.frise(
                new CalculEtapeTableau.EtapeCourante(EtapeTableau.POULES, ""));
        assertEquals(6, pas.size());
        assertEquals(PresentationTableauDeBordJourJ.EtatPas.PASSE, pas.get(2).etat());
        assertEquals(PresentationTableauDeBordJourJ.EtatPas.COURANT, pas.get(3).etat());
        assertEquals("jourj-frise-pas-courant", pas.get(3).classe());
        assertEquals(PresentationTableauDeBordJourJ.EtatPas.A_VENIR, pas.get(4).etat());
        assertEquals("Phase de poules", pas.get(3).libelle());
    }

    @Test
    void frise_etape_5_porte_le_tour_en_cours_en_long_et_en_court() {
        List<PresentationTableauDeBordJourJ.Pas> pas = PresentationTableauDeBordJourJ.frise(
                new CalculEtapeTableau.EtapeCourante(EtapeTableau.TABLEAU_FINAL, "1/8 de finale"));
        assertEquals("1/8 de finale", pas.get(4).libelle());
        assertEquals("1/8", pas.get(4).libelleCourt());
        assertEquals("Quarts", PresentationTableauDeBordJourJ.courant("Quarts de finale"));
        assertEquals("Demies", PresentationTableauDeBordJourJ.courant("Demi-finales"));
        assertEquals("Finale", PresentationTableauDeBordJourJ.courant("Finale et petite finale"));
        assertEquals("Pte finale", PresentationTableauDeBordJourJ.courant("Petite finale"));
        // Hors étape courante, l'étape 5 garde son libellé générique.
        assertEquals("Élimination directe", PresentationTableauDeBordJourJ.frise(
                new CalculEtapeTableau.EtapeCourante(EtapeTableau.POULES, "")).get(4).libelle());
    }

    @Test
    void frise_libelles_complets_courts_ou_numeros_seuls_selon_la_largeur() {
        assertEquals(PresentationTableauDeBordJourJ.ModeFrise.COMPLET, PresentationTableauDeBordJourJ.modeFrise(900));
        assertEquals(PresentationTableauDeBordJourJ.ModeFrise.COURT, PresentationTableauDeBordJourJ.modeFrise(500));
        assertEquals(PresentationTableauDeBordJourJ.ModeFrise.NUMEROS, PresentationTableauDeBordJourJ.modeFrise(300));
        PresentationTableauDeBordJourJ.Pas p = new PresentationTableauDeBordJourJ.Pas(2, "Pointage ouvert",
                "Pointage", PresentationTableauDeBordJourJ.EtatPas.COURANT);
        assertEquals("Pointage ouvert", p.texte(PresentationTableauDeBordJourJ.ModeFrise.COMPLET));
        assertEquals("Pointage", p.texte(PresentationTableauDeBordJourJ.ModeFrise.COURT));
        assertEquals("", p.texte(PresentationTableauDeBordJourJ.ModeFrise.NUMEROS));
    }

    @Test
    void actions_de_l_etape_courante_ouvrir_pointer_lancer() {
        Tableau t = new Tableau("A", ConfigurationTableau.depuisFormat(RegistreFormats.formatParDefaut()).build());
        t.inscrire(j(1));
        PresentationTableauDeBordJourJ.LigneTableau l = PresentationTableauDeBordJourJ.ligne(t);
        assertEquals("Étape 1 · Avant pointage", l.libelleEtape());
        assertEquals("Ouvrir le pointage", l.actions().get(0).libelle());
        assertEquals(PresentationAvancement.Detail.OUVRIR_POINTAGE, l.vue());

        t.ouvrirPointage();
        l = PresentationTableauDeBordJourJ.ligne(t);
        assertEquals("Pointer et clôturer", l.actions().get(0).libelle());
        assertEquals(PresentationAvancement.Detail.POINTAGE, l.vue());

        t.marquerPointageCloture();
        l = PresentationTableauDeBordJourJ.ligne(t);
        assertEquals("Étape 3 · Pointage fermé", l.libelleEtape());
        assertEquals("Lancer le tableau", l.actions().get(0).libelle());
        assertTrue(l.actions().get(0).principale());
    }

    @Test
    void actions_poules_tableau_final_et_termine() {
        CalculEtapeTableau.EtapeCourante poules = new CalculEtapeTableau.EtapeCourante(EtapeTableau.POULES, "");
        Tableau t = new Tableau("A", ConfigurationTableau.depuisFormat(RegistreFormats.formatParDefaut()).build());
        List<PresentationTableauDeBordJourJ.Action> a = PresentationTableauDeBordJourJ.actions(t, poules);
        assertEquals(1, a.size());
        assertEquals(PresentationAvancement.Detail.POULES, a.get(0).vue());

        a = PresentationTableauDeBordJourJ.actions(t, new CalculEtapeTableau.EtapeCourante(EtapeTableau.POULES,
                CalculEtapeTableau.TABLEAU_FINAL_A_TIRER));
        assertEquals("Tirer le tableau final", a.get(0).libelle());
        assertEquals(PresentationAvancement.Detail.TABLEAU_FINAL, a.get(0).vue());
        assertEquals("Poules", a.get(1).libelle());

        a = PresentationTableauDeBordJourJ.actions(t, new CalculEtapeTableau.EtapeCourante(
                EtapeTableau.TABLEAU_FINAL, "Quarts de finale"));
        assertEquals("Arbre", a.get(0).libelle());
        assertTrue(a.get(0).principale());

        // Terminé sans tableau final (format aux poules seules) : on revoit les poules.
        a = PresentationTableauDeBordJourJ.actions(t, new CalculEtapeTableau.EtapeCourante(EtapeTableau.TERMINE, ""));
        assertEquals(1, a.size());
        assertEquals(PresentationAvancement.Detail.POULES, a.get(0).vue());
        assertFalse(a.get(0).principale(), "plus rien à faire : pas de bouton bleu");
    }

    @Test
    void etat_des_annonces_en_un_mot() {
        assertEquals("Voix", PresentationTableauDeBordJourJ.etatAnnonces(ModeAnnonce.APPEL));
        assertEquals("SMS", PresentationTableauDeBordJourJ.etatAnnonces(ModeAnnonce.SMS));
        assertEquals("Voix et SMS", PresentationTableauDeBordJourJ.etatAnnonces(ModeAnnonce.SMS_ET_APPEL));
        assertEquals("Coupées", PresentationTableauDeBordJourJ.etatAnnonces(ModeAnnonce.AUCUNE));
        assertEquals("À chaque match", PresentationTableauDeBordJourJ.etatAnnonces(null));
    }

    @Test
    void detail_des_annonces_voix_et_sms() {
        ReglagesAnnoncesVocales r = new ReglagesAnnoncesVocales();
        r.setAnnonceAutomatique(true);
        assertEquals("Voix automatique · SMS automatiques.",
                PresentationTableauDeBordJourJ.detailAnnonces(ModeAnnonce.SMS_ET_APPEL, r, true, true));
        assertEquals("Voix indisponible : annonce à l'écran.",
                PresentationTableauDeBordJourJ.detailAnnonces(ModeAnnonce.APPEL, r, false, false));
        assertEquals("SMS non automatiques.",
                PresentationTableauDeBordJourJ.detailAnnonces(ModeAnnonce.SMS, r, true, false));
        assertEquals("Aucune annonce au lancement d'un match.",
                PresentationTableauDeBordJourJ.detailAnnonces(ModeAnnonce.AUCUNE, r, true, true));
    }

    @Test
    void strategie_en_place_par_defaut_puis_choisie() {
        Tournoi t = new Tournoi("Open", "Club", LocalDate.of(2026, 11, 14));
        assertEquals(StrategieOrdonnancement.PAR_DEFAUT.getLibelle(), PresentationTableauDeBordJourJ.valeurStrategie(t));
        assertEquals(PresentationSelecteurStrategie.MARQUE_PAR_DEFAUT, PresentationTableauDeBordJourJ.detailStrategie(t));
        t.setStrategieOrdonnancement(StrategieOrdonnancement.MINIMISER_ATTENTE);
        assertEquals(PresentationSelecteurStrategie.MARQUE_CHOISIE, PresentationTableauDeBordJourJ.detailStrategie(t));
        String message = PresentationTableauDeBordJourJ.messageChangementStrategie(t);
        assertTrue(message.contains(StrategieOrdonnancement.MINIMISER_ATTENTE.getLibelle()), message);
        assertTrue(message.contains("prochains matchs proposés"), message);
        assertTrue(message.contains("ne changent pas"), message);
    }
}
