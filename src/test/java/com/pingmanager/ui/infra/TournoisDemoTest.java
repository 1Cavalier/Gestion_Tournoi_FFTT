package com.pingmanager.ui.infra;

import com.pingmanager.core.CatalogueTournois;
import com.pingmanager.core.EligibiliteTournoi;
import com.pingmanager.core.Geocodeur;
import com.pingmanager.core.RemplissageTableaux;
import com.pingmanager.model.CategorieAge;
import com.pingmanager.model.FicheLicence;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.StatutTournoi;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Données de démo de l'onglet Tournois : niveaux de remplissage et respect des règles. */
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
    void brunoy_publie_avec_tableaux_pleins_moities_et_presque_vides() {
        Tournoi t = TournoisDemo.brunoyPublieEtRempli(DonneesDemo.JOUEURS_FICTIFS);
        assertEquals(StatutTournoi.PUBLIE, t.getStatut());
        assertNotNull(t.getAdresse().getCoordonnees());

        Tableau a = tab(t, "A");
        assertEquals(60, a.getInscrits().size(), "A complet");
        assertEquals(10, a.getFileAttente().size(), "liste d'attente pleine");
        assertTrue(tab(t, "C").estPlein());
        double moitieB = RemplissageTableaux.etat(tab(t, "B")).taux();
        assertTrue(moitieB > 0.4 && moitieB < 0.6, "B à moitié : " + moitieB);
        assertTrue(tab(t, "F").getInscrits().size() <= 6, "F presque vide");
        assertTrue(tab(t, "M").getInscrits().size() <= 4, "M presque vide");

        // Règles du club respectées : pas de poussin, joueuses du samedi inscrites au tableau dames,
        // au plus 2 tableaux par jour pour les joueurs, jamais deux tableaux qui se suivent.
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
        List<Joueur> ins = RemplissageTableaux.etat(a).inscrits();
        for (int i = 1; i < ins.size(); i++) {
            assertTrue(ins.get(i - 1).getPoints() >= ins.get(i).getPoints());
        }
    }

    @Test
    void autres_tournois_publies_et_acces_de_quentin() {
        FicheLicence quentin = DonneesDemo.licences().trouverParLicence(DonneesDemo.LICENCE_QUENTIN).orElseThrow();
        Map<String, Tournoi> autres = TournoisDemo.autres(DonneesDemo.JOUEURS_FICTIFS, DonneesDemo.clubs(), quentin);
        assertEquals(5, autres.size());
        assertTrue(autres.values().stream().allMatch(t -> t.getStatut() == StatutTournoi.PUBLIE));
        EligibiliteTournoi e = new EligibiliteTournoi(DonneesDemo.clubs());
        assertTrue(e.evaluer(autres.get("Demo_Regional_Creteil_2026"), quentin).estAccessible());
        assertTrue(e.evaluer(autres.get("Demo_Departemental_Loiret_2027"), quentin).estRefuse());
        assertTrue(e.evaluer(autres.get("Demo_Regional_Lille_2027"), quentin).estRefuse());
        assertTrue(e.evaluer(autres.get("Demo_National_A_Lyon_2027"), quentin).estAccessible());
        assertTrue(RemplissageTableaux.participant(autres.get("Demo_Promotionnel_Rennes_2026"),
                DonneesDemo.LICENCE_QUENTIN).isPresent());
        assertTrue(autres.get("Demo_National_A_Lyon_2027").getDotationGlobale() >= 5000, "national A : ≥ 5 000 €");
        // Créteil D : complet avec une liste d'attente non pleine, pour essayer l'inscription en attente.
        Tableau d = tab(autres.get("Demo_Regional_Creteil_2026"), "D");
        assertTrue(d.estPlein());
        assertFalse(d.getFileAttente().isEmpty());
        assertFalse(d.fileAttentePleine());

        // Créteil : seuls des licenciés d'Île-de-France (IX.102).
        Set<String> clubsIdf = new HashSet<>();
        for (FicheLicence f : DonneesDemo.JOUEURS_FICTIFS) {
            if (e.evaluer(autres.get("Demo_Regional_Creteil_2026"), f).estAccessible()) {
                clubsIdf.add(f.getNumeroLicence());
            }
        }
        for (RemplissageTableaux.Participant p : RemplissageTableaux.parJoueur(autres.get("Demo_Regional_Creteil_2026"),
                RemplissageTableaux.PAR_POINTS)) {
            assertTrue(clubsIdf.contains(p.joueur().getNumeroLicence()));
        }

        Map<String, Tournoi> tous = new LinkedHashMap<>(autres);
        tous.put("brunoy", TournoisDemo.brunoyPublieEtRempli(DonneesDemo.JOUEURS_FICTIFS));
        List<CatalogueTournois.Entree> cat = CatalogueTournois.construire(tous, quentin, null, e,
                Geocodeur.centroideDuDepartement(), LocalDate.of(2026, 11, 2));
        assertEquals(6, cat.size(), "Rennes en cours le 2 novembre : visible");
        assertEquals(2, cat.stream().filter(x -> x.acces().estRefuse()).count());
    }
}
