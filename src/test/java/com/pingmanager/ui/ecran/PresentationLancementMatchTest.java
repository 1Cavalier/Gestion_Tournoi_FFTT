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

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Mots et filtres de l'interface « Lancer un match » (B055). */
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
    void libelles_du_bouton_de_l_attente_et_du_resume() {
        assertEquals("Lancer · table 3", PresentationLancementMatch.libelleLancer(3));
        assertEquals("Lancer", PresentationLancementMatch.libelleLancer(null));
        assertEquals("Table 7", PresentationLancementMatch.libelleTable(7));
        assertEquals("Attend depuis 12 min", PresentationLancementMatch.attente(Duration.ofMinutes(12)));
        assertEquals("", PresentationLancementMatch.attente(null));
        assertFalse(PresentationLancementMatch.attenteLongue(Duration.ofMinutes(5)));
        assertTrue(PresentationLancementMatch.attenteLongue(Duration.ofMinutes(45)));

        Tournoi t = tournoi(2);
        assertEquals("6 matchs prêts · 2 tables libres", PresentationLancementMatch.resume(salle.listeLancements(t)));
        Tournoi plein = tournoi(1);
        salle.affecter(plein, matchDe(plein, "A", "A1"), 1);
        assertEquals("3 matchs prêts · aucune table libre",
                PresentationLancementMatch.resume(salle.listeLancements(plein)));
    }

    @Test
    void recherche_par_joueur_sans_accents_et_filtre_par_tableau() {
        Tournoi t = tournoi(4);
        ListeLancements liste = salle.listeLancements(t);

        assertEquals(List.of(PresentationLancementMatch.TOUS_LES_TABLEAUX, "A", "B"),
                PresentationLancementMatch.tableaux(liste));
        assertEquals(6, PresentationLancementMatch.filtrer(liste, "", null).size());
        assertEquals(3, PresentationLancementMatch.filtrer(liste, "", "B").size());
        List<ListeLancements.Ligne> lefevre = PresentationLancementMatch.filtrer(liste, "lefevre", null);
        assertEquals(2, lefevre.size(), "poule de 3 : deux parties pour chaque joueur");
        assertTrue(lefevre.stream().allMatch(l -> l.match().tableau().equals("B")));
        assertTrue(PresentationLancementMatch.filtrer(liste, "lefevre", "A").isEmpty());
        assertTrue(PresentationLancementMatch.filtrer(liste, "inconnu", null).isEmpty());
        assertTrue(PresentationLancementMatch.videManuel("inconnu").contains("« inconnu »"));
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
}
