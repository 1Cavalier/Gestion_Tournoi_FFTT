package com.pingmanager.ui.ecran;

import com.pingmanager.core.ResultatConstructionPhaseFinale;
import com.pingmanager.core.ServiceLancementTableau;
import com.pingmanager.core.ServicePhaseFinale;
import com.pingmanager.core.ServicePointage;
import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.HorairesTableau;
import com.pingmanager.model.IssueMatch;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Manche;
import com.pingmanager.model.Match;
import com.pingmanager.model.Poule;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.StatutMatch;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tour;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.format.RegistreFormats;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Bandeau « Où en sont les tableaux » de l'écran principal du jour J (B011), sans JavaFX. */
class PresentationAvancementTest {

    private static Joueur j(int i) {
        return new Joueur("L" + i, "NOM" + i, "Prénom" + i, Sexe.HOMME, "Club " + (i % 4), 1000 + i);
    }

    private static Tableau tableau(String nom, int places, LocalDateTime debut) {
        Tableau t = new Tableau(nom, ConfigurationTableau.depuisFormat(RegistreFormats.formatParDefaut())
                .placesMax(places).tailleMaxListeAttente(4).build());
        if (debut != null) {
            t.setHoraires(new HorairesTableau(debut.minusMinutes(60), debut.minusMinutes(20), debut));
        }
        return t;
    }

    private static List<Manche> victoireA(Match m) {
        List<Manche> r = new ArrayList<>();
        for (int i = 0; i < m.getNombreManchesGagnantes(); i++) {
            r.add(new Manche(11, 6));
        }
        return r;
    }

    /** Tournoi d'un tableau de {@code n} présents, pointage clos. */
    private static Tournoi tournoiPointe(int n) {
        Tournoi t = new Tournoi("Open", "Club", LocalDate.of(2026, 11, 14));
        Tableau a = tableau("A – Toutes séries", 0, LocalDateTime.of(2026, 11, 14, 9, 30));
        t.ajouterTableau(a);
        a.ouvrirPointage();
        for (int i = 1; i <= n; i++) {
            a.inscrire(j(i));
            a.pointer(j(i));
        }
        new ServicePointage(t).cloturerPointage(a);
        return t;
    }

    @Test
    void inscriptions_sans_jauge_et_ouverture_du_pointage_proposee() {
        Tableau a = tableau("A", 24, LocalDateTime.of(2026, 11, 14, 9, 0));
        for (int i = 1; i <= 17; i++) {
            a.inscrire(j(i));
        }
        PresentationAvancement.Ligne l = PresentationAvancement.ligne(a);
        assertEquals("17 / 24 inscrits · début 9h", l.detail());
        assertFalse(l.aJauge());
        assertEquals("", l.pourcentage());
        assertEquals(PresentationAvancement.Detail.OUVRIR_POINTAGE, l.vue());
        assertEquals("Ouvrir le pointage", l.libelleBouton());
        assertEquals("Inscriptions", l.libelleEtape());
    }

    @Test
    void pointage_ouvert_jauge_des_pointes() {
        Tableau a = tableau("A", 0, null);
        for (int i = 1; i <= 4; i++) {
            a.inscrire(j(i));
        }
        a.ouvrirPointage();
        a.pointer(j(1));
        PresentationAvancement.Ligne l = PresentationAvancement.ligne(a);
        assertEquals("1 / 4 pointé", l.detail());
        assertEquals(0.25, l.jauge(), 1e-9);
        assertEquals("25 %", l.pourcentage());
        assertEquals(PresentationAvancement.Detail.POINTAGE, l.vue());
        a.pointer(j(2));
        assertEquals("2 / 4 pointés", PresentationAvancement.ligne(a).detail());
    }

    @Test
    void pointage_vide_jauge_a_zero_sans_division_par_zero() {
        Tableau a = tableau("A", 0, null);
        a.ouvrirPointage();
        PresentationAvancement.Ligne l = PresentationAvancement.ligne(a);
        assertEquals(0, l.jauge());
        assertEquals("0 / 0 pointé", l.detail());
    }

    @Test
    void pointage_clos_tirage_a_faire() {
        Tournoi t = tournoiPointe(7);
        PresentationAvancement.Ligne l = PresentationAvancement.ligne(t.getTableaux().get(0));
        assertEquals("Pointage clos · 7 présents · tirage à faire", l.detail());
        assertEquals(PresentationAvancement.Detail.POINTAGE, l.vue());
        assertEquals("Tirage", l.libelleBouton());
    }

    @Test
    void poules_effectif_non_rond_jauge_des_parties_et_parties_en_jeu() {
        // 7 présents : effectif non rond, poules de tailles différentes (I.301 pour l'ordre des parties).
        Tournoi t = tournoiPointe(7);
        Tableau a = t.getTableaux().get(0);
        new ServiceLancementTableau(t).lancer(a, 3L);
        int total = 0;
        for (Poule p : a.getPoules()) {
            total += p.getMatchs().size();
        }
        int poules = a.getPoules().size();
        PresentationAvancement.Ligne l = PresentationAvancement.ligne(a);
        assertEquals("Poules 0 / " + poules + " terminée · 0 / " + total + " parties", l.detail());
        assertEquals(0, l.jauge());
        assertEquals(PresentationAvancement.Detail.POULES, l.vue());

        // Une partie jouée par forfait (I.303 a) compte comme jouée ; une autre est sur une table.
        Match forfait = a.getPoules().get(0).getMatchs().get(0);
        forfait.declarerIssue(IssueMatch.FORFAIT, forfait.getJoueurB());
        Match enCours = a.getPoules().get(poules - 1).getMatchs().get(0);
        enCours.setStatut(StatutMatch.EN_COURS);
        enCours.setNumeroTable(3);
        l = PresentationAvancement.ligne(a);
        assertTrue(l.detail().endsWith("1 / " + total + " parties"), l.detail());
        assertEquals(1.0 / total, l.jauge(), 1e-9);
        assertEquals(1, l.enJeu());
        assertEquals("1 en jeu", PresentationAvancement.libelleEnJeu(l.enJeu()));
        assertEquals("", PresentationAvancement.libelleEnJeu(0));
    }

    @Test
    void poules_terminees_le_bandeau_ouvre_le_tableau_final() {
        Tournoi t = tournoiPointe(6);
        Tableau a = t.getTableaux().get(0);
        new ServiceLancementTableau(t).lancer(a, 1L);
        for (Poule p : a.getPoules()) {
            for (Match m : p.getMatchs()) {
                m.definirScore(victoireA(m));
            }
        }
        PresentationAvancement.Ligne l = PresentationAvancement.ligne(a);
        assertEquals(PresentationJourJ.Etape.POULES_TERMINEES, l.etape());
        assertTrue(l.detail().endsWith("tableau final à construire"), l.detail());
        assertEquals(1, l.jauge());
        assertEquals(PresentationAvancement.Detail.TABLEAU_FINAL, l.vue());
    }

    @Test
    void tableau_final_tour_en_cours_exempts_exclus_puis_vainqueur() {
        // 9 joueurs : 3 poules, 6 qualifiés dans un tableau de 8, donc des exempts (byes) au premier tour.
        Tableau modele = new Tableau("F – Open", ConfigurationTableau.depuisFormat(RegistreFormats.formatParDefaut())
                .petiteFinale(true).build());
        for (int i = 1; i <= 9; i++) {
            modele.inscrire(j(i));
        }
        Tableau f = TableauEssaiPhaseFinale.creer(modele, 11L);
        ServicePhaseFinale service = new ServicePhaseFinale();
        ResultatConstructionPhaseFinale construit = service.construire(f, 5L);
        assertTrue(construit.estConstruit(), String.valueOf(construit));

        int qualifies = f.getPhaseFinale().getNombreJoueurs();
        // Un match par joueur éliminé, plus la petite finale ; les exemptions ne se jouent pas.
        int jouables = qualifies - 1 + 1;
        PresentationAvancement.Ligne l = PresentationAvancement.ligne(f);
        assertEquals(PresentationAvancement.Detail.TABLEAU_FINAL, l.vue());
        assertEquals("Arbre", l.libelleBouton());
        String premierTour = PresentationAvancement.tourEnCours(f.getPhaseFinale());
        assertTrue(f.getPhaseFinale().getNombreExemptes() > 0, "qualifiés en nombre non rond : des exempts");
        assertEquals(premierTour + " · 0 / " + jouables + " parties", l.detail(), "les exemptions ne se jouent pas");

        // On joue tour par tour : le tour en cours avance, finale et petite finale ensemble à la fin.
        List<String> vus = new ArrayList<>();
        for (int garde = 0; garde < 20 && !ServicePhaseFinale.estTerminee(f.getPhaseFinale()); garde++) {
            vus.add(PresentationAvancement.tourEnCours(f.getPhaseFinale()));
            List<Match> prets = new ArrayList<>();
            for (Tour tour : f.getPhaseFinale().getTours()) {
                for (Match m : tour.getMatchs()) {
                    if (!m.estBye() && m.getStatut() != StatutMatch.TERMINE
                            && !m.getJoueurA().estADeterminer() && !m.getJoueurB().estADeterminer()) {
                        prets.add(m);
                    }
                }
            }
            for (Match m : prets) {
                service.saisirScore(f, m, victoireA(m));
            }
        }
        assertTrue(vus.contains("Finale et petite finale"), vus.toString());
        assertFalse(vus.contains("Finale"), "la petite finale se joue avec la finale : " + vus);
        l = PresentationAvancement.ligne(f);
        assertEquals(1, l.jauge());
        assertTrue(l.detail().startsWith("Terminé · vainqueur "), l.detail());
    }

    @Test
    void nom_des_tours_en_cours() {
        assertEquals("Quarts de finale", PresentationPhaseFinale.libelleTour(2));
        assertEquals("1/8 de finale", PresentationPhaseFinale.libelleTour(3));
    }

    @Test
    void tableaux_regroupes_par_jour_seulement_sur_plusieurs_jours() {
        Tournoi t = new Tournoi("Open", "Club", LocalDate.of(2026, 11, 14));
        t.ajouterTableau(tableau("B", 0, LocalDateTime.of(2026, 11, 14, 10, 30)));
        t.ajouterTableau(tableau("A", 0, LocalDateTime.of(2026, 11, 14, 9, 0)));
        List<PresentationAvancement.Journee> un = PresentationAvancement.journees(t);
        assertEquals(1, un.size());
        assertFalse(PresentationAvancement.afficherJours(un));
        assertEquals("A", un.get(0).lignes().get(0).tableau().getNom(), "ordre des horaires");

        t.ajouterTableau(tableau("C", 0, LocalDateTime.of(2026, 11, 15, 9, 0)));
        List<PresentationAvancement.Journee> deux = PresentationAvancement.journees(t);
        assertEquals(2, deux.size());
        assertTrue(PresentationAvancement.afficherJours(deux));
        assertEquals(LocalDate.of(2026, 11, 15), deux.get(1).jour());
    }
}
