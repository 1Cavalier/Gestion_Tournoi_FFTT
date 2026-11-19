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
    void match_bloque_reste_dans_la_liste_avec_son_motif() {
        Tournoi t = tournoi(4);
        Match enJeu = matchDe(t, "A", "A1");
        assertTrue(salle.affecter(t, enJeu, 4).estReussi());
        ListeLancements liste = salle.listeLancements(t);

        List<ListeLancements.Ligne> bloquees = liste.getProposes().stream().filter(l -> !l.estLancable()).toList();

        assertEquals(2, bloquees.size());
        for (ListeLancements.Ligne l : bloquees) {
            assertTrue(PresentationLancementMatch.motif(l.refus(), 4).endsWith("joue en table 4"),
                    PresentationLancementMatch.motif(l.refus(), 4));
        }
        // Les bloqués viennent après les matchs lançables.
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
        assertEquals("Proposition 1 sur 6", PresentationLancementMatch.rang(liste, premier));

        assertTrue(salle.affecter(t, premier, plein.ligne().tableProposee()).estReussi());
        PresentationLancementMatch.Bandeau apres = PresentationLancementMatch.bandeau(salle.listeLancements(t), true,
                premier, false, 4);
        assertNull(apres.ligne(), "le match lancé n'est plus proposé : la zone se vide");
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
        assertTrue(salle.affecter(plein, matchDe(plein, "A", "A1"), 1).estReussi());
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
    void empreinte_change_quand_un_match_se_termine_ailleurs() {
        Tournoi t = tournoi(4);
        Match m = matchDe(t, "A", "A1");
        assertTrue(salle.affecter(t, m, 1).estReussi());
        String avant = PresentationLancementMatch.empreinte(salle.listeLancements(t));
        assertEquals(avant, PresentationLancementMatch.empreinte(salle.listeLancements(t)), "rien n'a changé");

        m.setStatut(com.pingmanager.model.StatutMatch.TERMINE);

        assertNotEquals(avant, PresentationLancementMatch.empreinte(salle.listeLancements(t)));
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
