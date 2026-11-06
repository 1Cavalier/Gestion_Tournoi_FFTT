package com.pingmanager.core;

import com.pingmanager.model.ConfigurationTableau;
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

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Jour J v2 (B037) : couleur de priorité des matchs proposés et joueurs en
 * attente avec leur chrono et le motif (pourquoi ils ne jouent pas).
 */
class ServiceSalleAttenteTest {

    private Instant instant = Instant.parse("2026-11-14T10:00:00Z");
    private final Horloge horloge = () -> instant;
    private final ServiceSalle salle = new ServiceSalle(horloge);

    private void avancer(int minutes) {
        instant = instant.plusSeconds(60L * minutes);
    }

    private static LocalDateTime dixHeuresPlus(int minutes) {
        return LocalDateTime.ofInstant(Instant.parse("2026-11-14T10:00:00Z").plusSeconds(60L * minutes),
                ZoneId.systemDefault());
    }

    private static Joueur j(String licence) {
        return new Joueur(licence, "N" + licence, "P" + licence, Sexe.HOMME, "Club" + licence, 1000);
    }

    private static Tableau tableau(String nom) {
        return new Tableau(nom, ConfigurationTableau.depuisFormat(RegistreFormats.formatParDefaut()).build());
    }

    private static void lancer(Tournoi t, Tableau tab, List<Joueur> joueurs) {
        tab.ouvrirPointage();
        for (Joueur joueur : joueurs) {
            tab.inscrire(joueur);
            tab.pointer(joueur);
        }
        new ServicePointage(t).cloturerPointage(tab);
        new ServiceLancementTableau(t).lancer(tab, 1L);
    }

    /** Six joueurs, deux poules de trois, {@code tables} tables. */
    private static Tournoi tournoiSixJoueurs(int tables) {
        Tournoi t = new Tournoi("Open", "Club", LocalDate.of(2026, 11, 14));
        t.setNombreDeTables(tables);
        Tableau a = tableau("A");
        t.ajouterTableau(a);
        lancer(t, a, List.of(j("L1"), j("L2"), j("L3"), j("L4"), j("L5"), j("L6")));
        return t;
    }

    private static Tableau tab(Tournoi t, String nom) {
        return t.getTableaux().stream().filter(x -> x.getNom().equals(nom)).findFirst().orElseThrow();
    }

    private static List<Manche> troisZero() {
        return List.of(new Manche(11, 5), new Manche(11, 5), new Manche(11, 5));
    }

    private static boolean joue(Match m, Joueur j) {
        return m.getJoueurA().equals(j) || m.getJoueurB().equals(j);
    }

    // ------------------------------------------------------------------
    // Couleurs de priorité
    // ------------------------------------------------------------------

    @Test
    void au_depart_les_premiers_proposes_autant_que_de_tables_libres_sont_orange() {
        Tournoi t = tournoiSixJoueurs(4);
        List<TableauDeBordSalle.MatchSalle> prets = salle.tableauDeBord(t).getPrets();
        assertEquals(6, prets.size());
        for (int i = 0; i < prets.size(); i++) {
            NiveauPriorite attendu = i < 4 ? NiveauPriorite.IMPORTANT : NiveauPriorite.NORMAL;
            assertEquals(attendu, prets.get(i).niveau(), "rang " + i);
        }
        assertEquals("prochain à envoyer", prets.get(0).motifNiveau());
        assertEquals("", prets.get(5).motifNiveau());
    }

    @Test
    void sans_table_renseignee_rien_n_est_choisi_par_la_strategie() {
        Tournoi t = tournoiSixJoueurs(0);
        assertTrue(salle.tableauDeBord(t).getPrets().stream().allMatch(p -> p.niveau() == NiveauPriorite.NORMAL));
    }

    @Test
    void attente_longue_en_rouge_avec_la_duree() {
        Tournoi t = tournoiSixJoueurs(6);
        Poule p1 = tab(t, "A").getPoules().get(0);
        Match premier = p1.getMatchs().get(0);
        assertTrue(salle.affecter(t, premier, 1).estReussi());
        avancer(5);
        new SaisieResultatMatch(horloge).enregistrerScore(tab(t, "A"), premier, troisZero());
        salle.libererApresResultat(t, premier);
        avancer(25);

        TableauDeBordSalle bord = salle.tableauDeBord(t);
        // Poule 2 : prête depuis le début du tableau (10 h 00) ; poule 1 : depuis 10 h 05.
        TableauDeBordSalle.MatchSalle tete = bord.getPrets().get(0);
        assertFalse(p1.getMatchs().contains(tete.match()), "la poule 2 attend le plus");
        assertEquals(NiveauPriorite.URGENT, tete.niveau());
        assertEquals(Duration.ofMinutes(30), tete.attente());
        TableauDeBordSalle.MatchSalle deP1 = bord.getPrets().stream()
                .filter(m -> p1.getMatchs().contains(m.match())).findFirst().orElseThrow();
        assertEquals(NiveauPriorite.URGENT, deP1.niveau());
        assertEquals(Duration.ofMinutes(25), deP1.attente());
        assertTrue(deP1.motifNiveau().contains("attend depuis 25 min"), deP1.motifNiveau());
    }

    @Test
    void joueur_attendu_dans_un_autre_tableau_rend_le_match_important() {
        Tournoi t = tournoiSixJoueurs(0); // aucune table : la stratégie ne colore rien
        Tableau b = tableau("B");
        t.ajouterTableau(b);
        Joueur commun = j("L1");
        lancer(t, b, List.of(commun, j("M2"), j("M3")));

        for (TableauDeBordSalle.MatchSalle m : salle.tableauDeBord(t).getPrets()) {
            if (joue(m.match(), commun)) {
                assertEquals(NiveauPriorite.IMPORTANT, m.niveau());
                String autre = "A".equals(m.tableau()) ? "B" : "A";
                assertTrue(m.motifNiveau().contains(commun.getNomComplet() + " joue aussi " + autre),
                        m.motifNiveau());
            } else {
                assertEquals(NiveauPriorite.NORMAL, m.niveau(), m.tableau() + " " + m.tour());
            }
        }
    }

    @Test
    void finale_du_tableau_final_est_importante() {
        Tournoi t = new Tournoi("Open", "Club", LocalDate.of(2026, 11, 14));
        Tableau a = tableau("A");
        t.ajouterTableau(a);
        lancer(t, a, List.of(j("L1"), j("L2"), j("L3"), j("L4"), j("L5"), j("L6")));
        for (Poule p : a.getPoules()) {
            for (Match m : p.getMatchs()) {
                m.definirScore(troisZero());
            }
        }
        ServicePhaseFinale pf = new ServicePhaseFinale();
        assertTrue(pf.construire(a, 7L).estConstruit());
        // Jouer tous les tours jusqu'à ce qu'il ne reste que des tours d'au plus deux parties.
        List<Tour> tours = a.getPhaseFinale().getTours();
        for (TableauDeBordSalle.MatchSalle m : salle.tableauDeBord(t).getPrets()) {
            Tour tour = tours.stream().filter(x -> x.getMatchs().contains(m.match())).findFirst().orElseThrow();
            long reels = tour.getMatchs().stream().filter(x -> !x.estBye()).count();
            if (reels <= 2) {
                assertEquals(NiveauPriorite.IMPORTANT, m.niveau(), tour.getNom());
                assertTrue(m.motifNiveau().contains(tour.getNom()), m.motifNiveau());
            }
        }
        // Avec 4 qualifiés, le premier tour est une demi-finale : au moins un match important.
        assertTrue(salle.tableauDeBord(t).getPrets().stream().anyMatch(m -> m.niveau() == NiveauPriorite.IMPORTANT));
    }

    // ------------------------------------------------------------------
    // Joueurs en attente
    // ------------------------------------------------------------------

    @Test
    void joueurs_en_attente_hors_table_avec_motif_adversaire_en_jeu_ou_pret() {
        Tournoi t = tournoiSixJoueurs(4);
        Poule p1 = tab(t, "A").getPoules().get(0);
        Match enCours = p1.getMatchs().get(0);
        assertTrue(salle.affecter(t, enCours, 3).estReussi());

        TableauDeBordSalle bord = salle.tableauDeBord(t);
        List<TableauDeBordSalle.JoueurEnAttente> attente = bord.getJoueursEnAttente();
        assertEquals(4, attente.size(), "les deux joueurs à la table 3 ne sont pas en attente");
        assertNull(bord.joueurEnAttente(enCours.getJoueurA().getNumeroLicence()));
        assertNull(bord.joueurEnAttente(enCours.getJoueurB().getNumeroLicence()));

        // Le troisième joueur de la poule 1 : ses deux adversaires jouent ensemble à la table 3.
        Joueur troisieme = p1.getJoueurs().stream()
                .filter(x -> !joue(enCours, x)).findFirst().orElseThrow();
        TableauDeBordSalle.JoueurEnAttente seul = bord.joueurEnAttente(troisieme.getNumeroLicence());
        assertFalse(seul.peutJouer());
        assertEquals(2, seul.engagements().size());
        for (TableauDeBordSalle.Engagement e : seul.engagements()) {
            assertEquals(TableauDeBordSalle.RaisonAttente.ADVERSAIRE_EN_JEU, e.raison());
            assertEquals(Integer.valueOf(3), e.tableAdversaire());
            assertEquals("Poule " + p1.getNom(), e.groupe());
            assertEquals(p1.getMatchs(), e.matchsDuGroupe());
        }
        // Les joueurs de la poule 2 peuvent jouer.
        for (Joueur x : tab(t, "A").getPoules().get(1).getJoueurs()) {
            assertTrue(bord.joueurEnAttente(x.getNumeroLicence()).peutJouer());
        }
    }

    @Test
    void chrono_depuis_la_fin_du_dernier_match_puis_tri_du_plus_long_au_plus_court() {
        Tournoi t = tournoiSixJoueurs(6);
        Poule p1 = tab(t, "A").getPoules().get(0);
        Poule p2 = tab(t, "A").getPoules().get(1);
        Match m1 = p1.getMatchs().get(0);
        assertTrue(salle.affecter(t, m1, 1).estReussi());
        avancer(5);
        new SaisieResultatMatch(horloge).enregistrerScore(tab(t, "A"), m1, troisZero());
        Match m2 = p2.getMatchs().get(0);
        assertTrue(salle.affecter(t, m2, 2).estReussi());
        avancer(10);
        new SaisieResultatMatch(horloge).enregistrerScore(tab(t, "A"), m2, troisZero());
        avancer(3);

        TableauDeBordSalle bord = salle.tableauDeBord(t);
        // Le premier lancement (10 h 00) donne le début du tableau ; m1 finit à 10 h 05, m2 à 10 h 15.
        TableauDeBordSalle.JoueurEnAttente premier = bord.getJoueursEnAttente().get(0);
        assertEquals(dixHeuresPlus(0), premier.depuis(), "joueurs n'ayant pas encore joué : début du tableau");
        assertEquals(Duration.ofMinutes(18), premier.attente());
        TableauDeBordSalle.JoueurEnAttente gagnant = bord.joueurEnAttente(m1.getJoueurA().getNumeroLicence());
        assertEquals(dixHeuresPlus(5), gagnant.depuis());
        assertEquals(Duration.ofMinutes(13), gagnant.attente());
        assertEquals(Duration.ofMinutes(3),
                bord.joueurEnAttente(m2.getJoueurA().getNumeroLicence()).attente());
        List<Duration> chronos = bord.getJoueursEnAttente().stream()
                .map(TableauDeBordSalle.JoueurEnAttente::attente).toList();
        for (int i = 1; i < chronos.size(); i++) {
            assertTrue(chronos.get(i - 1).compareTo(chronos.get(i)) >= 0, chronos.toString());
        }
    }

    @Test
    void avant_tout_lancement_le_chrono_part_de_l_heure_prevue_du_tableau_si_elle_est_passee() {
        Tournoi t = tournoiSixJoueurs(4);
        TableauDeBordSalle bord = salle.tableauDeBord(t);
        assertTrue(bord.getJoueursEnAttente().stream().allMatch(x -> x.attente() == null),
                "sans horaire ni lancement : chrono inconnu");

        tab(t, "A").getHoraires().setDebutMatchs(dixHeuresPlus(-15));
        bord = salle.tableauDeBord(t);
        assertEquals(6, bord.getJoueursEnAttente().size());
        assertTrue(bord.getJoueursEnAttente().stream().allMatch(x -> Duration.ofMinutes(15).equals(x.attente())));

        tab(t, "A").getHoraires().setDebutMatchs(dixHeuresPlus(30));
        assertTrue(salle.tableauDeBord(t).getJoueursEnAttente().stream().allMatch(x -> x.attente() == null),
                "heure prévue pas encore atteinte");
    }

    @Test
    void joueur_engage_sur_deux_tableaux_en_jeu_dans_l_un_n_est_pas_en_attente_dans_l_autre() {
        Tournoi t = tournoiSixJoueurs(6);
        Tableau b = tableau("B");
        t.ajouterTableau(b);
        Joueur commun = j("L1");
        lancer(t, b, List.of(commun, j("M2"), j("M3")));
        Match deA = tab(t, "A").getPoules().stream().flatMap(p -> p.getMatchs().stream())
                .filter(m -> joue(m, commun)).findFirst().orElseThrow();
        assertTrue(salle.affecter(t, deA, 2).estReussi());

        TableauDeBordSalle bord = salle.tableauDeBord(t);
        assertNull(bord.joueurEnAttente(commun.getNumeroLicence()));
        // Ses adversaires du tableau B l'attendent : il joue à la table 2.
        TableauDeBordSalle.JoueurEnAttente m2 = bord.joueurEnAttente("M2");
        assertTrue(m2.engagements().stream().anyMatch(e -> e.raison() == TableauDeBordSalle.RaisonAttente
                .ADVERSAIRE_EN_JEU && commun.equals(e.adversaire()) && Integer.valueOf(2).equals(e.tableAdversaire())
                && "B".equals(e.match().tableau())));
    }

    @Test
    void tableau_final_le_joueur_qualifie_attend_un_adversaire_inconnu() {
        Tournoi t = new Tournoi("Open", "Club", LocalDate.of(2026, 11, 14));
        t.setNombreDeTables(8);
        Tableau a = tableau("A");
        t.ajouterTableau(a);
        lancer(t, a, List.of(j("L1"), j("L2"), j("L3"), j("L4"), j("L5"), j("L6"), j("L7"), j("L8"), j("L9")));
        for (Poule p : a.getPoules()) {
            for (Match m : p.getMatchs()) {
                m.definirScore(troisZero());
            }
        }
        assertTrue(new ServicePhaseFinale().construire(a, 7L).estConstruit());

        TableauDeBordSalle bord = salle.tableauDeBord(t);
        long attendus = 0;
        for (Tour tour : a.getPhaseFinale().getTours()) {
            for (Match m : tour.getMatchs()) {
                if (!m.estBye() && m.getStatut() == StatutMatch.A_JOUER
                        && (m.getJoueurA().estADeterminer() != m.getJoueurB().estADeterminer())) {
                    attendus++;
                    Joueur connu = m.getJoueurA().estADeterminer() ? m.getJoueurB() : m.getJoueurA();
                    TableauDeBordSalle.JoueurEnAttente x = bord.joueurEnAttente(connu.getNumeroLicence());
                    assertNotNull(x, connu.getNomComplet());
                    assertTrue(x.engagements().stream().anyMatch(
                            e -> e.raison() == TableauDeBordSalle.RaisonAttente.ADVERSAIRE_INCONNU
                                    && e.groupe().equals(tour.getNom())));
                }
            }
        }
        for (TableauDeBordSalle.JoueurEnAttente x : bord.getJoueursEnAttente()) {
            for (TableauDeBordSalle.Engagement e : x.engagements()) {
                if (e.raison() == TableauDeBordSalle.RaisonAttente.ADVERSAIRE_INCONNU) {
                    assertTrue(e.adversaire() == null || e.adversaire().estADeterminer());
                }
            }
        }
        assertTrue(bord.getJoueursEnAttente().size() > 0);
    }

    @Test
    void joueur_qui_a_fini_tous_ses_matchs_n_est_plus_en_attente() {
        Tournoi t = tournoiSixJoueurs(4);
        Poule p1 = tab(t, "A").getPoules().get(0);
        for (Match m : p1.getMatchs()) {
            m.definirScore(troisZero());
        }
        TableauDeBordSalle bord = salle.tableauDeBord(t);
        for (Joueur x : p1.getJoueurs()) {
            assertNull(bord.joueurEnAttente(x.getNumeroLicence()));
        }
        assertEquals(3, bord.getJoueursEnAttente().size());
    }
}
