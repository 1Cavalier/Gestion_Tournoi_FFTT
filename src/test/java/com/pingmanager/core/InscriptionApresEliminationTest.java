package com.pingmanager.core;

import com.pingmanager.core.ResultatInscriptionTournoi.Motif;
import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.HorairesTableau;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Manche;
import com.pingmanager.model.Match;
import com.pingmanager.model.Poule;
import com.pingmanager.model.QuotasTableaux;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.StatutTableau;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Clause d'organisateur « nouveau tableau après élimination » : un joueur éliminé d'un
 * tableau peut s'inscrire, le même jour, à un tableau qui n'a pas commencé, au-delà de son
 * quota, dans la limite du nombre choisi par le club.
 */
class InscriptionApresEliminationTest {

    private static final LocalDate JOUR = LocalDate.of(2027, 4, 10);

    private final ServiceInscription service = ServiceInscription.sansContrainteSuccession();

    private final Tableau a = tableau("A", 8);
    private final Tableau b = tableau("B", 10);
    private final Tableau c = tableau("C", 12);
    private final Tableau d = tableau("D", 14);
    private final Tableau e = tableau("E", 16);
    private final Joueur p = joueur("P", 800);
    private final List<Joueur> autres = new ArrayList<>();

    private static Tableau tableau(String nom, int heure) {
        Tableau t = new Tableau(nom, ConfigurationTableau.builder().placesMax(8)
                .format(com.pingmanager.model.format.RegistreFormats.formatParDefaut()).build());
        t.setHoraires(new HorairesTableau(null, null, JOUR.atTime(heure, 0)));
        return t;
    }

    private static Joueur joueur(String licence, int points) {
        return new Joueur(licence, "N" + licence, "P" + licence, Sexe.HOMME, "Club " + licence, points);
    }

    /** Tournoi d'un jour, 2 tableaux par joueur ; P inscrit en A et en D. */
    private Tournoi tournoi(int apresElimination, boolean consecutifsInterdits) {
        Tournoi t = new Tournoi("Open", "Club", List.of(JOUR));
        for (Tableau x : List.of(a, b, c, d, e)) {
            t.ajouterTableau(x);
        }
        t.setQuotasTableaux(QuotasTableaux.identiques(2, 2)
                .avecTableauxConsecutifsInterdits(consecutifsInterdits)
                .avecTableauxApresElimination(apresElimination));
        assertTrue(service.inscrire(t, p, a).estInscrit());
        assertTrue(service.inscrire(t, p, d).estInscrit());
        for (int i = 1; i <= 5; i++) {
            Joueur j = joueur("J" + i, 1000 + i * 10);
            autres.add(j);
            assertTrue(service.inscrire(t, j, a).estInscrit());
        }
        return t;
    }

    /** Tableau A lancé (2 poules de 3) ; P perd toutes ses parties, les autres matchs sont joués. */
    private void lancerAEtEliminerP(Tournoi t, boolean jouerToutes) {
        ServicePointage pointage = new ServicePointage(t);
        a.ouvrirPointage();
        for (Joueur j : List.copyOf(a.getInscrits())) {
            pointage.pointer(a, j);
        }
        pointage.cloturerPointage(a);
        new ServiceLancementTableau(t).lancer(a, 42L);
        SaisieResultatMatch saisie = new SaisieResultatMatch(() -> Instant.parse("2027-04-10T08:30:00Z"));
        for (Poule poule : a.getPoules()) {
            boolean sienne = poule.getJoueurs().contains(p);
            if (!sienne && !jouerToutes) {
                continue;
            }
            for (Match m : poule.getMatchs()) {
                boolean pEnB = p.equals(m.getJoueurB());
                saisie.enregistrerScore(a, m, pEnB ? victoireA() : victoireB());
            }
        }
    }

    private static List<Manche> victoireA() {
        return List.of(new Manche(11, 5), new Manche(11, 6), new Manche(11, 7));
    }

    private static List<Manche> victoireB() {
        return List.of(new Manche(5, 11), new Manche(6, 11), new Manche(7, 11));
    }

    @Test
    void elimine_quand_sa_poule_est_terminee_et_qu_il_n_est_pas_qualifie() {
        Tournoi t = tournoi(1, false);
        assertFalse(EliminationTableau.estElimine(a, p), "tableau pas commencé");
        assertTrue(EliminationTableau.nAPasCommence(a));
        lancerAEtEliminerP(t, false);
        assertEquals(StatutTableau.LANCE, a.getStatut());
        assertFalse(EliminationTableau.nAPasCommence(a));
        assertTrue(EliminationTableau.estElimine(a, p));
        Poule sienne = a.getPoules().stream().filter(x -> x.getJoueurs().contains(p)).findFirst().orElseThrow();
        for (Joueur j : sienne.getJoueurs()) {
            if (!j.equals(p)) {
                assertFalse(EliminationTableau.estElimine(a, j), "qualifié : " + j);
            }
        }
        assertFalse(EliminationTableau.estElimine(d, p), "D n'a pas commencé");
    }

    @Test
    void pas_elimine_tant_que_sa_poule_n_est_pas_terminee() {
        Tournoi t = tournoi(1, false);
        lancerAEtEliminerP(t, false);
        Poule autre = a.getPoules().stream().filter(x -> !x.getJoueurs().contains(p)).findFirst().orElseThrow();
        for (Joueur j : autre.getJoueurs()) {
            assertFalse(EliminationTableau.estElimine(a, j), "poule pas jouée : " + j);
        }
    }

    @Test
    void sans_la_clause_le_quota_du_jour_reste_la_limite() {
        Tournoi t = tournoi(0, false);
        lancerAEtEliminerP(t, false);
        assertEquals(Motif.QUOTA_TABLEAUX_ATTEINT, service.inscrire(t, p, b).getMotif());
    }

    @Test
    void avec_la_clause_un_tableau_de_plus_mais_pas_deux() {
        Tournoi t = tournoi(1, false);
        // Pas encore éliminé : le quota s'applique.
        assertEquals(Motif.QUOTA_TABLEAUX_ATTEINT, service.inscrire(t, p, b).getMotif());
        lancerAEtEliminerP(t, false);
        assertEquals(Motif.INSCRIT, service.inscrire(t, p, b).getMotif());
        assertEquals(Motif.QUOTA_TABLEAUX_ATTEINT, service.inscrire(t, p, c).getMotif());
    }

    @Test
    void un_joueur_encore_en_lice_ne_profite_pas_de_la_clause() {
        Tournoi t = tournoi(1, false);
        lancerAEtEliminerP(t, false);
        Joueur qualifie = a.getPoules().stream().filter(x -> x.getJoueurs().contains(p)).findFirst().orElseThrow()
                .getJoueurs().stream().filter(j -> !j.equals(p)).findFirst().orElseThrow();
        assertTrue(service.inscrire(t, qualifie, b).estInscrit());
        assertEquals(Motif.QUOTA_TABLEAUX_ATTEINT, service.inscrire(t, qualifie, c).getMotif());
    }

    @Test
    void le_tableau_dont_il_est_elimine_ne_compte_plus_pour_les_tableaux_qui_se_suivent() {
        Tournoi t = tournoi(1, true);
        lancerAEtEliminerP(t, false);
        // B suit A (éliminé) : accepté ; C suivrait B, désormais en lice : refusé.
        assertEquals(Motif.INSCRIT, service.inscrire(t, p, b).getMotif());
    }

    @Test
    void sur_place_seulement_les_tableaux_pas_commences_avec_une_place_libre() {
        Tournoi t = tournoi(1, false);
        lancerAEtEliminerP(t, false);
        ServiceInscriptionSurPlace surPlace = new ServiceInscriptionSurPlace();
        assertEquals(List.of(b, c, d, e), ServiceInscriptionSurPlace.tableauxOuverts(t));
        assertEquals(List.of(a), ServiceInscriptionSurPlace.tableauxElimines(t, p));
        assertFalse(surPlace.inscrire(t, p, a, true).accepte(), "A a commencé");

        // C complet : pas de liste d'attente sur place.
        for (int i = 1; i <= 8; i++) {
            assertTrue(service.inscrire(t, joueur("C" + i, 900), c).estInscrit());
        }
        Map<Tableau, String> verif = surPlace.verifier(t, p);
        assertNull(verif.get(b));
        assertTrue(verif.get(c).startsWith("Complet"), verif.get(c));
        assertNotNull(verif.get(d), "déjà inscrit");

        // B en pointage : inscrit, pointé et payé sur place.
        b.ouvrirPointage();
        ServiceInscriptionSurPlace.Resultat r = surPlace.inscrire(t, p, b, true);
        assertTrue(r.accepte(), r.motif());
        assertTrue(b.getInscrits().contains(p));
        assertTrue(b.aPointe(p));
        assertTrue(b.aPaye(p));
        // Le tableau de plus est pris : E est refusé (quota du jour + 1 atteint).
        assertFalse(surPlace.inscrire(t, p, e, false).accepte());
    }

    @Test
    void sur_place_tableau_en_inscription_sans_pointage_ni_paiement() {
        Tournoi t = tournoi(1, false);
        lancerAEtEliminerP(t, false);
        ServiceInscriptionSurPlace.Resultat r = new ServiceInscriptionSurPlace().inscrire(t, p, e, false);
        assertTrue(r.accepte(), r.motif());
        assertFalse(e.aPointe(p));
        assertFalse(e.aPaye(p));
    }
}
