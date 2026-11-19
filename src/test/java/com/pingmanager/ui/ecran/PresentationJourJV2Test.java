package com.pingmanager.ui.ecran;

import com.pingmanager.core.AnnonceRencontre;
import com.pingmanager.core.NiveauPriorite;
import com.pingmanager.core.ServiceLancementTableau;
import com.pingmanager.core.ServicePointage;
import com.pingmanager.core.ServiceSalle;
import com.pingmanager.core.TableauDeBordSalle;
import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Manche;
import com.pingmanager.model.Match;
import com.pingmanager.model.ModeAnnonce;
import com.pingmanager.model.Poule;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.format.RegistreFormats;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Jour J v2 (B037), logique d'affichage sans JavaFX : couleurs, joueurs en
 * attente et « pourquoi », « Lancer un match », « Terminer la rencontre / la
 * poule », annonce, jour suivant.
 */
class PresentationJourJV2Test {

    private static Joueur j(String licence, String prenom, String nom) {
        return new Joueur(licence, nom, prenom, Sexe.HOMME, "Club", 1000);
    }

    private static Tableau tableau(String nom, LocalDateTime debut) {
        Tableau t = new Tableau(nom, ConfigurationTableau.depuisFormat(RegistreFormats.formatParDefaut()).build());
        t.getHoraires().setDebutMatchs(debut);
        return t;
    }

    private static Tournoi tournoi(int tables) {
        Tournoi t = new Tournoi("Open", "Club", LocalDate.of(2026, 11, 14));
        t.setNombreDeTables(tables);
        Tableau a = tableau("C – Toutes séries", null);
        t.ajouterTableau(a);
        a.ouvrirPointage();
        for (int i = 1; i <= 6; i++) {
            Joueur joueur = j("L" + i, "Prénom" + i, "NOM" + i);
            a.inscrire(joueur);
            a.pointer(joueur);
        }
        new ServicePointage(t).cloturerPointage(a);
        new ServiceLancementTableau(t).lancer(a, 1L);
        return t;
    }

    private final ServiceSalle salle = new ServiceSalle(() -> Instant.parse("2026-11-14T10:00:00Z"));

    private static Poule poule(Tournoi t, int i) {
        return t.getTableaux().get(0).getPoules().get(i);
    }

    private static List<Manche> troisZero() {
        return List.of(new Manche(11, 5), new Manche(11, 5), new Manche(11, 5));
    }

    /** Ajoute et lance un tableau « B » de trois joueurs (dont {@code commun}), début prévu à 10 h. */
    private static Tableau ajouterB(Tournoi t, Joueur commun) {
        Tableau b = tableau("B", LocalDateTime.ofInstant(Instant.parse("2026-11-14T10:00:00Z"),
                java.time.ZoneId.systemDefault()));
        t.ajouterTableau(b);
        b.ouvrirPointage();
        for (Joueur x : List.of(commun, j("M2", "Prénom", "MDEUX"), j("M3", "Prénom", "MTROIS"))) {
            b.inscrire(x);
            b.pointer(x);
        }
        new ServicePointage(t).cloturerPointage(b);
        new ServiceLancementTableau(t).lancer(b, 1L);
        return b;
    }

    /** Douze joueurs, poules jouées, tableau final construit (quarts) : matchs lancés un par un. */
    private static Tournoi tournoiTableauFinal(int tables) {
        Tournoi t = new Tournoi("Open", "Club", LocalDate.of(2026, 11, 14));
        t.setNombreDeTables(tables);
        Tableau a = tableau("C – Toutes séries", null);
        t.ajouterTableau(a);
        a.ouvrirPointage();
        for (int i = 1; i <= 12; i++) {
            Joueur joueur = j("L" + i, "Prénom" + i, "NOM" + i);
            a.inscrire(joueur);
            a.pointer(joueur);
        }
        new ServicePointage(t).cloturerPointage(a);
        new ServiceLancementTableau(t).lancer(a, 1L);
        a.getPoules().forEach(p -> p.getMatchs().forEach(m -> m.definirScore(troisZero())));
        assertTrue(new com.pingmanager.core.ServicePhaseFinale().construire(a, 7L).estConstruit());
        return t;
    }

    @Test
    void couleur_de_priorite_en_classe_et_en_pastille() {
        assertEquals("priorite-urgent", PresentationSalle.classePriorite(NiveauPriorite.URGENT));
        assertEquals("priorite-important", PresentationSalle.classePriorite(NiveauPriorite.IMPORTANT));
        assertEquals("priorite-normal", PresentationSalle.classePriorite(NiveauPriorite.NORMAL));
        assertEquals(StatutPastille.ERREUR, PresentationSalle.pastillePriorite(NiveauPriorite.URGENT));
        assertEquals(StatutPastille.ATTENTION, PresentationSalle.pastillePriorite(NiveauPriorite.IMPORTANT));
        assertEquals(StatutPastille.SUCCES, PresentationSalle.pastillePriorite(NiveauPriorite.NORMAL));

        // Couleur des matchs du tableau final (lancés un par un) ; une table libre.
        List<TableauDeBordSalle.MatchSalle> prets = salle.tableauDeBord(tournoiTableauFinal(1)).getPrets();
        assertEquals("Important : prochain à envoyer", PresentationSalle.detailPriorite(prets.get(0)));
        assertEquals("Normal : peut attendre", PresentationSalle.detailPriorite(prets.get(3)));
    }

    @Test
    void ligne_de_chiffres_avec_les_joueurs_en_attente() {
        Tournoi t = tournoi(4);
        assertEquals("4 tables libres · 0 en cours · 2 poules proposées · 6 joueurs en attente",
                PresentationSalle.resumeSalle(salle.tableauDeBord(t)));
        assertTrue(salle.lancerPoule(t, poule(t, 0), 1).estReussi());
        assertEquals("3 tables libres · 1 en cours · 1 poule proposée · 3 joueurs en attente",
                PresentationSalle.resumeSalle(salle.tableauDeBord(t)));
        assertEquals("Nombre de tables non renseigné", PresentationSalle.resumeSalle(salle.tableauDeBord(tournoi(0))));
    }


    @Test
    void joueur_en_attente_chrono_motif_court_et_pourquoi() {
        Tournoi t = tournoi(1);
        Joueur commun = poule(t, 0).getJoueurs().get(0);
        Tableau b = ajouterB(t, commun);
        assertTrue(salle.lancerPoule(t, poule(t, 0), 1).estReussi());
        TableauDeBordSalle bord = salle.tableauDeBord(t);

        // M2, dans le tableau B : sa poule attend un joueur qui joue sa poule de A à la table 1.
        TableauDeBordSalle.JoueurEnAttente seul = bord.joueurEnAttente("M2");
        assertEquals("moins d'1 min", PresentationSalle.chrono(seul), "le tableau B commence à 10 h");
        assertFalse(PresentationSalle.attenteLongue(seul));
        assertTrue(PresentationSalle.motifCourt(seul).startsWith("Attend "), PresentationSalle.motifCourt(seul));
        assertTrue(PresentationSalle.motifCourt(seul).endsWith("(table 1)"), PresentationSalle.motifCourt(seul));
        List<String> pourquoi = PresentationSalle.pourquoi(seul, bord);
        assertEquals(2, pourquoi.size());
        assertTrue(pourquoi.get(0).contains(", qui joue à la table 1. Un joueur ne peut pas être sur deux tables."),
                pourquoi.get(0));
        assertTrue(pourquoi.get(0).startsWith("B · Poule " + b.getPoules().get(0).getNom()), pourquoi.get(0));

        // Poule 2 de A : prête, mais la seule table est prise.
        Joueur deP2 = poule(t, 1).getJoueurs().get(0);
        TableauDeBordSalle.JoueurEnAttente pret = bord.joueurEnAttente(deP2.getNumeroLicence());
        assertEquals("Peut jouer : match prêt", PresentationSalle.motifCourt(pret));
        assertTrue(PresentationSalle.pourquoi(pret, bord).get(0).endsWith("toutes les tables sont occupées."));
    }


    @Test
    void groupe_du_joueur_avec_son_nom_surligne_et_l_etat_des_matchs() {
        Tournoi t = tournoi(4);
        Joueur commun = poule(t, 0).getJoueurs().get(0);
        Tableau b = ajouterB(t, commun);
        assertTrue(salle.lancerPoule(t, poule(t, 0), 2).estReussi());
        TableauDeBordSalle bord = salle.tableauDeBord(t);
        Joueur m2 = b.getPoules().get(0).getJoueurs().stream().filter(x -> "M2".equals(x.getNumeroLicence()))
                .findFirst().orElseThrow();
        TableauDeBordSalle.Engagement e = bord.joueurEnAttente("M2").engagements().get(0);

        List<PresentationSalle.LigneGroupe> lignes = PresentationSalle.lignesGroupe(e, m2);
        assertEquals(3, lignes.size(), "poule de trois : trois parties");
        assertEquals(2, lignes.stream().filter(PresentationSalle.LigneGroupe::concerne).count());
        assertTrue(lignes.stream().allMatch(l -> l.etat().equals("À jouer")), "la poule de B n'est pas lancée");
        for (PresentationSalle.LigneGroupe l : lignes) {
            if (l.surligneA()) {
                assertEquals(m2.getNomComplet(), l.joueurA());
            }
            if (l.surligneB()) {
                assertEquals(m2.getNomComplet(), l.joueurB());
            }
        }
        Match enJeu = poule(t, 0).getMatchs().get(0);
        assertEquals("Table 2", PresentationSalle.etatMatch(enJeu), "poule lancée en table 2");
        enJeu.definirScore(troisZero());
        assertEquals("Joué", PresentationSalle.etatMatch(enJeu));
    }


    @Test
    void lancer_un_match_filtre_par_nom_sans_accent_et_donne_le_motif_des_bloques() {
        // Tableau final (matchs lancés un par un) ; un qualifié joue aussi la poule de B, lancée en table 1.
        Tournoi t = tournoiTableauFinal(4);
        Joueur commun = salle.tableauDeBord(t).getPrets().get(0).match().getJoueurA();
        Tableau b = ajouterB(t, commun);
        assertTrue(salle.lancerPoule(t, b.getPoules().get(0), 1).estReussi());
        TableauDeBordSalle bord = salle.tableauDeBord(t);

        List<PresentationSalle.Lancable> tous = PresentationSalle.lancables(bord, "");
        assertEquals(4, tous.size(), "3 prêts et 1 bloqué");
        assertEquals(3, tous.stream().filter(PresentationSalle.Lancable::possible).count());
        assertTrue(tous.stream().filter(l -> !l.possible())
                .allMatch(l -> l.refus().endsWith("déjà en jeu à la table 1")));

        Joueur cherche = bord.getPrets().get(0).match().getJoueurB();
        List<PresentationSalle.Lancable> filtres = PresentationSalle.lancables(bord,
                "  " + cherche.getPrenom().toUpperCase().replace("É", "E") + " ");
        assertEquals(1, filtres.size(), "son quart de finale");
        assertTrue(PresentationSalle.lancables(bord, "inconnu au bataillon").isEmpty());
    }


    @Test
    void terminer_la_rencontre_ou_la_poule_selon_ce_qu_il_reste() {
        Tournoi t = tournoi(4);
        List<Match> p = poule(t, 0).getMatchs();
        assertEquals("Terminer la rencontre", PresentationSalle.libelleTerminer(t, p.get(0)));
        p.get(0).definirScore(List.of(new Manche(11, 5), new Manche(11, 5), new Manche(11, 5)));
        p.get(1).definirScore(List.of(new Manche(11, 5), new Manche(11, 5), new Manche(11, 5)));
        assertEquals("Terminer la poule", PresentationSalle.libelleTerminer(t, p.get(2)));
        assertEquals("Terminer la rencontre", PresentationSalle.libelleTerminer(t,
                new Match(j("X", "x", "X"), j("Y", "y", "Y"))));
        assertTrue(PresentationSalle.confirmationAnnulation(3).contains("La table 3 redevient libre"));
    }

    @Test
    void annonce_affichee_a_l_ecran_et_libelle_du_choix_memorise() {
        assertEquals("Annonce : à demander", PresentationSalle.libelleAnnonce(null));
        assertEquals("Annonce : SMS et appel vocal", PresentationSalle.libelleAnnonce(ModeAnnonce.SMS_ET_APPEL));
        Match m = new Match(j("A", "Ana", "AUBERT"), j("B", "Bob", "BLANC"));
        String sms = PresentationSalle.texteAnnonce(AnnonceRencontre.preparer(ModeAnnonce.SMS, m, 7, "C"));
        assertTrue(sms.startsWith("SMS (envoi automatique à venir"), sms);
        assertTrue(sms.endsWith("Table 7 : Ana AUBERT contre Bob BLANC (C)."), sms);
        String rien = PresentationSalle.texteAnnonce(AnnonceRencontre.preparer(ModeAnnonce.AUCUNE, m, 7, null));
        assertTrue(rien.startsWith("À annoncer au micro · Table 7"), rien);
    }

    @Test
    void jour_du_tournoi_montre_par_defaut_puis_jour_suivant() {
        Tournoi t = new Tournoi("Open", "Club",
                List.of(LocalDate.of(2026, 11, 14), LocalDate.of(2026, 11, 15)));
        t.ajouterTableau(tableau("Dimanche B", LocalDateTime.of(2026, 11, 15, 9, 0)));
        t.ajouterTableau(tableau("Samedi tard", LocalDateTime.of(2026, 11, 14, 14, 0)));
        t.ajouterTableau(tableau("Samedi tôt", LocalDateTime.of(2026, 11, 14, 9, 0)));
        t.ajouterTableau(tableau("Sans horaire", null));
        List<PresentationAvancement.Journee> jours = PresentationAvancement.journees(t);
        assertEquals(3, jours.size());
        // Tableaux dans l'ordre des horaires de début.
        assertEquals(List.of("Samedi tôt", "Samedi tard"),
                jours.get(0).lignes().stream().map(l -> l.tableau().getNom()).toList());

        assertEquals(0, PresentationAvancement.indexJourParDefaut(jours, LocalDate.of(2026, 11, 14)));
        assertEquals(1, PresentationAvancement.indexJourParDefaut(jours, LocalDate.of(2026, 11, 15)));
        assertEquals(0, PresentationAvancement.indexJourParDefaut(jours, LocalDate.of(2026, 11, 1)), "avant");
        assertEquals(1, PresentationAvancement.indexJourParDefaut(jours, LocalDate.of(2026, 12, 1)), "après");
        assertEquals(0, PresentationAvancement.indexJourParDefaut(List.of(), LocalDate.of(2026, 12, 1)));

        assertEquals(2, PresentationAvancement.borner(5, jours));
        assertEquals(0, PresentationAvancement.borner(-1, jours));
        LocalDate samedi = LocalDate.of(2026, 11, 14);
        assertTrue(PresentationAvancement.titreJour(jours.get(0), samedi).startsWith("Aujourd'hui · "));
        assertTrue(PresentationAvancement.titreJour(jours.get(1), samedi).startsWith("Demain · "));
        assertEquals("Sans horaire", PresentationAvancement.titreJour(jours.get(2), samedi));
    }
}
