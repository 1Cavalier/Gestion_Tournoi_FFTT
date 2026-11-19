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

    /**
     * Tableau « A » de douze joueurs dont les poules sont jouées et le tableau final
     * construit (quarts de finale) : ses matchs se lancent un par un, la couleur de
     * priorité s'y applique. Pendant la phase de poules, on lance une poule entière.
     */
    private static Tournoi tournoiTableauFinal(int tables) {
        Tournoi t = new Tournoi("Open", "Club", LocalDate.of(2026, 11, 14));
        t.setNombreDeTables(tables);
        Tableau a = tableau("A");
        t.ajouterTableau(a);
        List<Joueur> joueurs = new java.util.ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            joueurs.add(j("L" + i));
        }
        lancer(t, a, joueurs);
        a.getPoules().forEach(p -> p.getMatchs().forEach(m -> m.definirScore(troisZero())));
        assertTrue(new ServicePhaseFinale().construire(a, 7L).estConstruit());
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
        Tournoi t = tournoiTableauFinal(2);
        List<TableauDeBordSalle.MatchSalle> prets = salle.tableauDeBord(t).getPrets();
        assertEquals(4, prets.size(), "quarts de finale");
        for (int i = 0; i < prets.size(); i++) {
            NiveauPriorite attendu = i < 2 ? NiveauPriorite.IMPORTANT : NiveauPriorite.NORMAL;
            assertEquals(attendu, prets.get(i).niveau(), "rang " + i);
        }
        assertEquals("prochain à envoyer", prets.get(0).motifNiveau());
        assertEquals("", prets.get(3).motifNiveau());
    }


    @Test
    void sans_table_renseignee_rien_n_est_choisi_par_la_strategie() {
        Tournoi t = tournoiTableauFinal(0);
        List<TableauDeBordSalle.MatchSalle> prets = salle.tableauDeBord(t).getPrets();
        assertFalse(prets.isEmpty());
        assertTrue(prets.stream().allMatch(p -> p.niveau() == NiveauPriorite.NORMAL));
    }


    @Test
    void attente_longue_en_rouge_avec_la_duree() {
        Tournoi t = tournoiTableauFinal(6);
        Tableau a = tab(t, "A");
        // La demi-finale dont on joue les deux quarts de 10 h 00 à 10 h 05.
        Match demi = a.getPhaseFinale().getTours().get(1).getMatchs().get(0);
        ServicePhaseFinale pf = new ServicePhaseFinale();
        assertTrue(salle.affecter(t, demi.getSourceA(), 1).estReussi());
        assertTrue(salle.affecter(t, demi.getSourceB(), 2).estReussi());
        avancer(5);
        for (Match q : List.of(demi.getSourceA(), demi.getSourceB())) {
            assertTrue(pf.saisirScore(a, q, troisZero()).estValide());
            salle.libererApresResultat(t, q);
        }
        avancer(25);

        TableauDeBordSalle bord = salle.tableauDeBord(t);
        // Les deux autres quarts : prêts depuis le début du tableau (10 h 00) ; la demi : depuis 10 h 05.
        TableauDeBordSalle.MatchSalle tete = bord.getPrets().get(0);
        assertNotSame(demi, tete.match(), "les quarts attendent le plus");
        assertEquals(NiveauPriorite.URGENT, tete.niveau());
        assertEquals(Duration.ofMinutes(30), tete.attente());
        TableauDeBordSalle.MatchSalle laDemi = bord.getPrets().stream().filter(m -> m.match() == demi).findFirst()
                .orElseThrow();
        assertEquals(NiveauPriorite.URGENT, laDemi.niveau());
        assertEquals(Duration.ofMinutes(25), laDemi.attente());
        assertTrue(laDemi.motifNiveau().contains("attend depuis 25 min"), laDemi.motifNiveau());
    }


    @Test
    void joueur_attendu_dans_un_autre_tableau_rend_le_match_important() {
        Tournoi t = tournoiTableauFinal(0); // aucune table : la stratégie ne colore rien
        // Un qualifié du tableau final de A joue aussi le tableau B (phase de poules).
        Joueur commun = salle.tableauDeBord(t).getPrets().get(0).match().getJoueurA();
        Tableau b = tableau("B");
        t.ajouterTableau(b);
        lancer(t, b, List.of(commun, j("M2"), j("M3")));

        List<TableauDeBordSalle.MatchSalle> prets = salle.tableauDeBord(t).getPrets();
        assertTrue(prets.stream().anyMatch(m -> joue(m.match(), commun)));
        for (TableauDeBordSalle.MatchSalle m : prets) {
            if (joue(m.match(), commun)) {
                assertEquals(NiveauPriorite.IMPORTANT, m.niveau());
                assertTrue(m.motifNiveau().contains(commun.getNomComplet() + " joue aussi B"), m.motifNiveau());
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
        Tableau b = tableau("B");
        t.ajouterTableau(b);
        Joueur commun = j("L1");
        lancer(t, b, List.of(commun, j("M2"), j("M3")));
        Poule p1 = tab(t, "A").getPoules().stream().filter(p -> p.getJoueurs().contains(commun)).findFirst()
                .orElseThrow();
        assertTrue(salle.lancerPoule(t, p1, 3).estReussi());

        TableauDeBordSalle bord = salle.tableauDeBord(t);
        List<TableauDeBordSalle.JoueurEnAttente> attente = bord.getJoueursEnAttente();
        assertEquals(5, attente.size(), "les trois joueurs de la poule lancée en table 3 ne sont pas en attente");
        for (Joueur x : p1.getJoueurs()) {
            assertNull(bord.joueurEnAttente(x.getNumeroLicence()), "occupé par sa poule, même entre deux matchs");
        }

        // M2, dans le tableau B : son adversaire L1 joue sa poule à la table 3.
        Poule pouleB = b.getPoules().get(0);
        TableauDeBordSalle.JoueurEnAttente m2 = bord.joueurEnAttente("M2");
        TableauDeBordSalle.Engagement e = m2.engagements().stream().filter(x -> commun.equals(x.adversaire()))
                .findFirst().orElseThrow();
        assertEquals(TableauDeBordSalle.RaisonAttente.ADVERSAIRE_EN_JEU, e.raison());
        assertEquals(Integer.valueOf(3), e.tableAdversaire());
        assertEquals("Poule " + pouleB.getNom(), e.groupe());
        assertEquals(pouleB.getMatchs(), e.matchsDuGroupe());
        // Les joueurs de l'autre poule de A peuvent jouer.
        Poule p2 = tab(t, "A").getPoules().stream().filter(p -> p != p1).findFirst().orElseThrow();
        for (Joueur x : p2.getJoueurs()) {
            assertTrue(bord.joueurEnAttente(x.getNumeroLicence()).peutJouer());
        }
    }


    @Test
    void chrono_depuis_la_fin_du_dernier_match_puis_tri_du_plus_long_au_plus_court() {
        Tournoi t = tournoiSixJoueurs(6);
        Poule p1 = tab(t, "A").getPoules().get(0);
        Poule p2 = tab(t, "A").getPoules().get(1);
        // Tableau B : un joueur de chaque poule de A, qui a donc encore à jouer après sa poule.
        Tableau b = tableau("B");
        t.ajouterTableau(b);
        Joueur de1 = p1.getJoueurs().get(0);
        Joueur de2 = p2.getJoueurs().get(0);
        lancer(t, b, List.of(de1, de2, j("M3")));
        SaisieResultatMatch saisie = new SaisieResultatMatch(horloge);
        assertTrue(salle.lancerPoule(t, p1, 1).estReussi());
        avancer(3);
        // Pas encore joué : la poule 2 attend depuis le début du tableau dans la salle (10 h 00).
        TableauDeBordSalle avant = salle.tableauDeBord(t);
        assertEquals(dixHeuresPlus(0), avant.joueurEnAttente(de2.getNumeroLicence()).depuis());
        assertEquals(Duration.ofMinutes(3), avant.joueurEnAttente(de2.getNumeroLicence()).attente());
        avancer(2);
        p1.getMatchs().forEach(m -> saisie.enregistrerScore(tab(t, "A"), m, troisZero()));
        assertTrue(salle.lancerPoule(t, p2, 2).estReussi());
        avancer(10);
        p2.getMatchs().forEach(m -> saisie.enregistrerScore(tab(t, "A"), m, troisZero()));
        avancer(3);

        TableauDeBordSalle bord = salle.tableauDeBord(t);
        // La poule 1 finit à 10 h 05, la poule 2 à 10 h 15.
        TableauDeBordSalle.JoueurEnAttente premier = bord.getJoueursEnAttente().get(0);
        assertEquals(de1, premier.joueur());
        assertEquals(dixHeuresPlus(5), premier.depuis());
        assertEquals(Duration.ofMinutes(13), premier.attente());
        assertEquals(Duration.ofMinutes(3), bord.joueurEnAttente(de2.getNumeroLicence()).attente());
        List<Duration> chronos = bord.getJoueursEnAttente().stream()
                .map(TableauDeBordSalle.JoueurEnAttente::attente).filter(java.util.Objects::nonNull).toList();
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
        Poule deA = tab(t, "A").getPoules().stream().filter(p -> p.getJoueurs().contains(commun)).findFirst()
                .orElseThrow();
        assertTrue(salle.lancerPoule(t, deA, 2).estReussi());

        TableauDeBordSalle bord = salle.tableauDeBord(t);
        assertNull(bord.joueurEnAttente(commun.getNumeroLicence()));
        // Ses adversaires du tableau B l'attendent : il joue (sa poule) à la table 2.
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
