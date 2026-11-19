package com.pingmanager.ui.ecran;

import com.pingmanager.core.ListeLancements;
import com.pingmanager.core.ServiceLancementTableau;
import com.pingmanager.core.ServicePointage;
import com.pingmanager.core.ServiceSalle;
import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Match;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.format.RegistreFormats;
import com.pingmanager.ordonnancement.ResultatAffectationTable;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Logique d'affichage de l'interface « Lancer un match » (B055, refaite en B057). */
class PresentationLancementMatchTest {

    private final ServiceSalle salle = new ServiceSalle(() -> Instant.parse("2026-11-14T10:00:00Z"));

    private static Joueur j(String licence, String nom) {
        return new Joueur(licence, nom, "Paul", Sexe.HOMME, "Club", 1000);
    }

    /** Deux tableaux lancés, « A » et « B », de trois joueurs chacun (une poule). */
    private static Tournoi tournoi(int tables) {
        Tournoi t = new Tournoi("Open", "Club", LocalDate.of(2026, 11, 14));
        t.setNombreDeTables(tables);
        lancer(t, "A", List.of(j("A1", "MARTIN"), j("A2", "DURAND"), j("A3", "PETIT")));
        lancer(t, "B", List.of(j("B1", "LEFÈVRE"), j("B2", "ROUX"), j("B3", "BLANC")));
        return t;
    }

    private static void lancer(Tournoi t, String nom, List<Joueur> joueurs) {
        Tableau tab = new Tableau(nom, ConfigurationTableau.depuisFormat(RegistreFormats.formatParDefaut()).build());
        t.ajouterTableau(tab);
        tab.ouvrirPointage();
        for (Joueur joueur : joueurs) {
            tab.inscrire(joueur);
            tab.pointer(joueur);
        }
        new ServicePointage(t).cloturerPointage(tab);
        new ServiceLancementTableau(t).lancer(tab, 1L);
    }

    private static com.pingmanager.model.Poule pouleDe(Tournoi t, String tableau) {
        return t.getTableaux().stream().filter(x -> x.getNom().equals(tableau)).findFirst().orElseThrow()
                .getPoules().get(0);
    }

    private static Match matchDe(Tournoi t, String tableau, String licence) {
        return t.getTableaux().stream().filter(x -> x.getNom().equals(tableau)).findFirst().orElseThrow()
                .getPoules().get(0).getMatchs().stream()
                .filter(m -> m.getJoueurA().getNumeroLicence().equals(licence)
                        || m.getJoueurB().getNumeroLicence().equals(licence))
                .findFirst().orElseThrow();
    }

    @Test
    void motif_joueur_en_jeu_dit_qui_joue_et_sur_quelle_table() {
        Joueur martin = j("A1", "MARTIN");
        assertEquals("Paul MARTIN joue en table 4", PresentationLancementMatch.motif(
                ResultatAffectationTable.joueurEnJeu(0, martin, 4), 8));
        assertEquals("Paul MARTIN est déjà en jeu (table inconnue)", PresentationLancementMatch.motif(
                ResultatAffectationTable.joueurEnJeuSansTable(0, martin), 8));
    }

    @Test
    void motifs_aucune_table_libre_salle_fermee_et_tables_non_renseignees() {
        assertEquals("Aucune table libre", PresentationLancementMatch.motif(
                ResultatAffectationTable.de(ResultatAffectationTable.Motif.AUCUNE_TABLE_LIBRE, 0), 8));
        assertEquals("Tournoi annulé : salle fermée", PresentationLancementMatch.motif(
                ResultatAffectationTable.de(ResultatAffectationTable.Motif.TOURNOI_FERME, 0), 8));
        assertEquals("Nombre de tables non renseigné", PresentationLancementMatch.motif(
                ResultatAffectationTable.de(ResultatAffectationTable.Motif.TABLE_HORS_LIMITES, 0), 0));
        assertEquals("", PresentationLancementMatch.motif(null, 8));
        assertTrue(PresentationSalle.message(
                ResultatAffectationTable.de(ResultatAffectationTable.Motif.AUCUNE_TABLE_LIBRE, 0), 8)
                .startsWith("Aucune table libre"));
    }

    @Test
    void table_choisie_gardee_si_toujours_possible_sinon_table_proposee() {
        Tournoi t = tournoi(4);
        ListeLancements.Ligne l = salle.listeLancements(t).getProposes().get(0);
        Integer autre = l.tablesPossibles().stream().filter(x -> !x.equals(l.tableProposee())).findFirst()
                .orElseThrow();

        assertEquals(autre, PresentationLancementMatch.tableRetenue(l, autre));
        assertEquals(l.tableProposee(), PresentationLancementMatch.tableRetenue(l, null));
        assertEquals(l.tableProposee(), PresentationLancementMatch.tableRetenue(l, 99), "table qui n'est plus libre");
    }

    @Test
    void poule_bloquee_reste_dans_la_liste_avec_son_motif() {
        Tournoi t = tournoi(4);
        // Tableau C : Paul MARTIN (A1) y joue aussi.
        lancer(t, "C", List.of(j("A1", "MARTIN"), j("C2", "MOREL"), j("C3", "FAURE")));
        assertTrue(salle.lancerPoule(t, pouleDe(t, "A"), 4).estReussi());
        ListeLancements liste = salle.listeLancements(t);

        List<ListeLancements.Ligne> bloquees = liste.getProposes().stream().filter(l -> !l.estLancable()).toList();

        assertEquals(1, bloquees.size(), "la poule de C attend Paul MARTIN");
        assertEquals("Paul MARTIN joue en table 4", PresentationLancementMatch.motif(bloquees.get(0).refus(), 4));
        // Les bloquées viennent après les lignes lançables.
        int dernierLancable = -1;
        int premierBloque = Integer.MAX_VALUE;
        for (int i = 0; i < liste.getProposes().size(); i++) {
            if (liste.getProposes().get(i).estLancable()) {
                dernierLancable = i;
            } else {
                premierBloque = Math.min(premierBloque, i);
            }
        }
        assertTrue(dernierLancable < premierBloque);
    }


    // ------------------------------------------------------------------
    // B057
    // ------------------------------------------------------------------

    @Test
    void libelles_table_tableau_et_message_de_lancement() {
        Tournoi t = tournoi(4);
        ListeLancements.Ligne l = salle.listeLancements(t).getJouables().get(0);

        assertEquals("Table 7", PresentationLancementMatch.libelleTable(7));
        assertTrue(PresentationLancementMatch.tableauEtEchelon(l).matches("[AB] · Poule .+"),
                PresentationLancementMatch.tableauEtEchelon(l));
        Match m = matchDe(t, "A", "A1");
        String attendu = m.getJoueurA().getNom() + " – " + m.getJoueurB().getNom() + " lancé en table 5";
        assertEquals(attendu, PresentationLancementMatch.lance(m, 5));
        assertEquals("Match non lancé : Paul MARTIN joue en table 4.", PresentationLancementMatch.refus(
                ResultatAffectationTable.joueurEnJeu(0, j("A1", "MARTIN"), 4), 8));
        assertEquals("Match non lancé : la table 5 vient d'être prise.", PresentationLancementMatch.refus(
                ResultatAffectationTable.de(ResultatAffectationTable.Motif.TABLE_OCCUPEE, 5), 8));
    }

    @Test
    void bandeau_vide_avant_la_demande_puis_proposition_puis_vide_apres_lancement() {
        Tournoi t = tournoi(4);
        ListeLancements liste = salle.listeLancements(t);

        PresentationLancementMatch.Bandeau vide = PresentationLancementMatch.bandeau(liste, false, null, false, 4);
        assertNull(vide.ligne());
        assertEquals(TextesLancementMatch.ZONE_VIDE, vide.texte());
        assertFalse(vide.lancerActif());
        assertFalse(vide.autreActif());

        Match premier = liste.propositionApres(null).orElseThrow().match().match();
        PresentationLancementMatch.Bandeau plein = PresentationLancementMatch.bandeau(liste, true, premier, false, 4);
        assertSame(premier, plein.ligne().match().match());
        assertTrue(plein.lancerActif());
        assertTrue(plein.autreActif());
        assertEquals("Proposition 1 sur 2", PresentationLancementMatch.rang(liste, premier), "une poule par tableau");

        assertTrue(salle.lancerPoule(t, plein.ligne().poule().poule(), plein.ligne().tableProposee()).estReussi());
        PresentationLancementMatch.Bandeau apres = PresentationLancementMatch.bandeau(salle.listeLancements(t), true,
                premier, false, 4);
        assertNull(apres.ligne(), "la poule lancée n'est plus proposée : la zone se vide");
        assertEquals(TextesLancementMatch.ZONE_VIDE, apres.texte());
    }

    @Test
    void bandeau_aucun_match_et_aucune_table_libre() {
        Tournoi t = tournoi(4);
        ListeLancements liste = salle.listeLancements(t);
        PresentationLancementMatch.Bandeau aucun = PresentationLancementMatch.bandeau(liste, true, null, false, 4);
        assertEquals(TextesLancementMatch.AUCUN_MATCH, aucun.texte());
        assertFalse(aucun.lancerActif());

        Tournoi plein = tournoi(1);
        assertTrue(salle.lancerPoule(plein, pouleDe(plein, "A"), 1).estReussi());
        ListeLancements sansTable = salle.listeLancements(plein);
        Match propose = sansTable.propositionApres(null).orElseThrow().match().match();
        PresentationLancementMatch.Bandeau b = PresentationLancementMatch.bandeau(sansTable, true, propose, false, 1);
        assertNotNull(b.ligne(), "la proposition reste affichée");
        assertFalse(b.lancerActif());
        assertEquals(TextesLancementMatch.AUCUNE_TABLE, b.motifLancer());
        assertEquals(TextesLancementMatch.AUCUNE_TABLE,
                PresentationLancementMatch.motifLigne(sansTable.getJouables().get(0), false, 1));
    }

    @Test
    void empreinte_change_quand_une_poule_se_termine_ailleurs() {
        Tournoi t = tournoi(4);
        com.pingmanager.model.Poule p = pouleDe(t, "A");
        assertTrue(salle.lancerPoule(t, p, 1).estReussi());
        String avant = PresentationLancementMatch.empreinte(salle.listeLancements(t));
        assertEquals(avant, PresentationLancementMatch.empreinte(salle.listeLancements(t)), "rien n'a changé");

        p.getMatchs().forEach(m -> m.setStatut(com.pingmanager.model.StatutMatch.TERMINE));

        assertNotEquals(avant, PresentationLancementMatch.empreinte(salle.listeLancements(t)));
    }

    @Test
    void ligne_de_poule_joueurs_dans_l_ordre_matchs_duree_et_ordre_de_jeu() {
        Tournoi t = tournoi(4);
        ListeLancements.Ligne l = salle.listeLancements(t).getJouables().stream()
                .filter(x -> "A".equals(x.match().tableau())).findFirst().orElseThrow();
        assertTrue(l.estPoule());
        com.pingmanager.core.PouleALancer p = l.poule();
        List<Joueur> js = p.joueurs();

        assertEquals(List.of("1. " + js.get(0).getNomComplet() + " · 1000 pts", "2. " + js.get(1).getNomComplet()
                + " · 1000 pts", "3. " + js.get(2).getNomComplet() + " · 1000 pts"),
                PresentationLancementMatch.joueursPoule(p));
        // Deux lignes au plus, jamais tronquées : 1 et 2, puis 3.
        assertEquals("1. " + js.get(0).getNom() + " 1000 · 2. " + js.get(1).getNom() + " 1000\n3. "
                + js.get(2).getNom() + " 1000", PresentationLancementMatch.joueursPouleCompact(p));
        assertEquals("3 matchs · environ 1 h 00", PresentationLancementMatch.matchsEtDuree(p));
        // I.301 : ordre où les matchs se jouent sur la table (poule de 3, 2 qualifiés : 1-3, 1-2, 2-3).
        List<String> ordre = PresentationLancementMatch.ordreDesMatchs(p);
        assertTrue(ordre.get(0).startsWith("1. 1–3  "), ordre.get(0));
        assertTrue(ordre.get(1).startsWith("2. 1–2  "), ordre.get(1));
        assertTrue(ordre.get(2).startsWith("3. 2–3  "), ordre.get(2));
        assertEquals("Tableau A, poule " + p.poule().getNom() + " lancée en table 5",
                PresentationLancementMatch.pouleLancee("A", p.poule(), 5));
        assertEquals("Poule non lancée : Paul MARTIN joue en table 4.", PresentationLancementMatch.refusPoule(
                ResultatAffectationTable.joueurEnJeu(0, j("A1", "MARTIN"), 4), 8));
    }


    @Test
    void voir_plus_retrouve_la_poule_du_match() {
        Tournoi t = tournoi(4);
        Match m = matchDe(t, "B", "B1");

        PresentationLancementMatch.Situation s = PresentationLancementMatch.situer(t, m).orElseThrow();

        assertTrue(s.estPoule());
        assertEquals("B", s.tableau().getNom());
        assertTrue(s.poule().getMatchs().contains(m));
        assertEquals("B · Poule " + s.poule().getNom(), PresentationLancementMatch.titreSituation(s));
        assertTrue(PresentationLancementMatch.situer(t, new Match(j("X", "X"), j("Y", "Y"))).isEmpty());
    }
}
