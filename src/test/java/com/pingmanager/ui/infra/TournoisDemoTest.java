package com.pingmanager.ui.infra;

import com.pingmanager.core.RemplissageTableaux;
import com.pingmanager.core.reglement.GenerateurReglement;
import com.pingmanager.model.DocumentReglement;
import com.pingmanager.model.CategorieAge;
import com.pingmanager.model.FicheLicence;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.StatutTournoi;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Données de démo : le vrai National B (brouillon) et sa copie au jour J. */
class TournoisDemoTest {

    private static Tableau tab(Tournoi t, String lettre) {
        return t.getTableaux().stream().filter(x -> x.getNom().startsWith(lettre)).findFirst().orElseThrow();
    }

    @Test
    void joueurs_fictifs_deterministes() {
        List<FicheLicence> a = JoueursFictifs.generer();
        assertEquals(a, JoueursFictifs.generer());
        assertEquals(375, a.size());
        assertTrue(a.stream().allMatch(f -> f.getNumeroLicence().startsWith("99")));
        assertEquals(375, a.stream().map(f -> f.getNom() + f.getPrenom()).distinct().count());
    }

    @Test
    void national_b_a_l_etat_reel_brouillon_sans_numero_ni_inscrit() {
        Tournoi t = TournoiDemoBrunoy.construire();
        assertEquals(StatutTournoi.BROUILLON, t.getStatut());
        assertTrue(t.getNumeroHomologation() == null || t.getNumeroHomologation().isBlank());
        assertEquals(13, t.getTableaux().size());
        assertEquals(0, RemplissageTableaux.nombreDeParticipants(t));
        assertEquals(3390, t.getDotationGlobale(), 0.001);
        assertEquals("LOOBUYCK", t.getJugeArbitrePrincipal().orElseThrow().getNom());
        assertTrue(t.getJugesArbitres().stream().anyMatch(ja -> ja.getNom().equals("PRIOU")));
        assertEquals("BOULIER", t.getCorrespondantNom());
        assertEquals("https://www.helloasso.com/associations/cttb-brunoy/evenements/tournoi-national-b-2027",
                t.getPaiement().getLien());
    }

    /** Règlement du club reçu le 9 novembre 2026 : 20 articles, régénérés à l'identique. */
    @Test
    void reglement_du_national_b_conforme_au_reglement_du_club() {
        DocumentReglement d = new GenerateurReglement().generer(TournoiDemoBrunoy.construire());
        List<String> titres = d.articles().stream().map(DocumentReglement.Article::titre).toList();
        assertEquals(List.of("Organisation", "Homologation", "Participants", "Tableaux",
                "Nombre de tableaux par joueur", "Déroulement", "Inscriptions", "Droits d'engagement",
                "Dotations", "Pointage", "Juge-arbitrage", "Matériel", "Tenue et comportement", "Résultats",
                "Responsabilité", "Droit à l'image et données personnelles", "Buvette",
                "Remise des récompenses", "Acceptation", "Contact"), titres);
        assertTrue(article(d, 1).contains("3 390 €"), article(d, 1));
        assertTrue(article(d, 2).contains("[à compléter"), "numéro d'homologation pas encore délivré");
        assertTrue(article(d, 3).contains("2e phase 2026-2027"), article(d, 3));
        assertTrue(article(d, 7).contains("https://www.helloasso.com/associations/cttb-brunoy/evenements/tournoi-national-b-2027"));
        assertTrue(article(d, 7).contains("1er février 2027"), article(d, 7));
        assertTrue(article(d, 11).contains("Samuel LOOBUYCK"), article(d, 11));
        assertTrue(article(d, 11).contains("William PRIOU (JA2)"), article(d, 11));
        assertTrue(article(d, 12).contains("20 tables"), article(d, 12));
        assertTrue(article(d, 20).contains("Serge BOULIER, sboulier@gmail.com"), article(d, 20));
    }

    @Test
    void copie_au_jour_j_inscriptions_conformes_aux_regles_du_club() {
        Tournoi t = TournoiDemoBrunoyJourJ.construire(DonneesDemo.JOUEURS_FICTIFS, null);
        // Pas de poussin, joueuses du samedi inscrites au tableau dames, au plus 2 tableaux par
        // jour (3 pour les joueuses avec le tableau dames).
        Tableau dames = tab(t, "G");
        for (RemplissageTableaux.Participant p : RemplissageTableaux.parJoueur(t, RemplissageTableaux.PAR_POINTS)) {
            Joueur j = p.joueur();
            assertNotEquals(CategorieAge.POUSSIN, j.getCategorie());
            List<Tableau> tous = new java.util.ArrayList<>(p.inscrit());
            tous.addAll(p.enAttente());
            Map<LocalDate, List<Tableau>> parJour = new LinkedHashMap<>();
            tous.forEach(x -> parJour.computeIfAbsent(x.getHoraires().getJour(), k -> new java.util.ArrayList<>()).add(x));
            for (Map.Entry<LocalDate, List<Tableau>> e : parJour.entrySet()) {
                int max = j.getSexe() == Sexe.FEMME && e.getValue().contains(dames) ? 3 : 2;
                assertTrue(e.getValue().size() <= max, j + " " + e.getValue());
                if (j.getSexe() == Sexe.FEMME && e.getKey().equals(dames.getHoraires().getJour())) {
                    assertTrue(e.getValue().contains(dames), "tableau dames imposé : " + j);
                }
            }
        }
        // Par points décroissants.
        List<Joueur> ins = RemplissageTableaux.etat(tab(t, "H")).inscrits();
        for (int i = 1; i < ins.size(); i++) {
            assertTrue(ins.get(i - 1).getPoints() >= ins.get(i).getPoints());
        }
    }

    private static String article(DocumentReglement d, int numero) {
        StringBuilder sb = new StringBuilder();
        d.articles().get(numero - 1).blocs().forEach(b -> sb.append(b.texteBrut()).append('\n'));
        return sb.toString();
    }
}
