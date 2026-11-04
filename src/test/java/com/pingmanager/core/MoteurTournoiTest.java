package com.pingmanager.core;

import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Manche;
import com.pingmanager.model.Match;
import com.pingmanager.model.Poule;
import com.pingmanager.model.Qualifie;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.TableauElimination;
import com.pingmanager.model.Tour;
import com.pingmanager.model.format.FormatTournoi;
import com.pingmanager.model.format.RegistreFormats;
import com.pingmanager.model.format.SortNonQualifies;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MoteurTournoiTest {

    private final MoteurTournoi moteur = new MoteurTournoi();
    private final FormatTournoi format = RegistreFormats.formatParDefaut();

    private Joueur joueur(String lic, int pts, String club) {
        return new Joueur(lic, "N" + lic, "P" + lic, Sexe.HOMME, club, pts);
    }

    private void jouerPoule(Poule p) {
        for (Match m : p.getMatchs()) {
            if (m.estBye()) continue;
            Joueur a = m.getJoueurA();
            Joueur b = m.getJoueurB();
            if (a.getPoints() >= b.getPoints()) {
                m.definirScore(List.of(new Manche(11, 5), new Manche(11, 5), new Manche(11, 5)));
            } else {
                m.definirScore(List.of(new Manche(5, 11), new Manche(5, 11), new Manche(5, 11)));
            }
        }
    }

    private List<Joueur> neufJoueurs() {
        List<Joueur> joueurs = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            joueurs.add(joueur("J" + i, 2000 - i * 50, "Club" + i));
        }
        return joueurs;
    }

    @Test
    void le_format_par_defaut_est_poule_de_3_deux_qualifies() {
        assertEquals(3, format.taillePouleSouhaitee());
        assertEquals(2, format.qualifiesParPoule());
        assertEquals(SortNonQualifies.ELIMINE, format.sortNonQualifies());
        assertEquals("P3Q2", format.identifiant());
    }

    @Test
    void prepare_des_poules_de_trois_avec_leurs_matchs() {
        List<Poule> poules = moteur.preparerPoules(neufJoueurs(), format);
        assertEquals(3, poules.size());
        assertTrue(poules.stream().allMatch(p -> p.getTaille() == 3));
        assertTrue(poules.stream().allMatch(p -> p.getMatchs().size() == 3));
    }

    @Test
    void deux_qualifies_par_poule_et_le_troisieme_est_elimine() {
        List<Poule> poules = moteur.preparerPoules(neufJoueurs(), format);
        for (Poule p : poules) {
            jouerPoule(p);
        }
        List<Qualifie> quals = moteur.extraireQualifies(poules, format);
        List<Qualifie> elimines = moteur.nonQualifies(poules, format);

        assertEquals(6, quals.size());
        assertEquals(3, elimines.size());
        assertTrue(quals.stream().allMatch(q -> q.rangDansPoule() <= 2));
        assertTrue(elimines.stream().allMatch(q -> q.rangDansPoule() == 3));
        assertTrue(moteur.nonQualifiesElimines(format));
    }

    @Test
    void aucun_elimine_ne_figure_dans_le_tableau() {
        List<Poule> poules = moteur.preparerPoules(neufJoueurs(), format);
        for (Poule p : poules) {
            jouerPoule(p);
        }
        List<Qualifie> elimines = moteur.nonQualifies(poules, format);
        TableauElimination t = moteur.construireTableau(poules, format, 1L);

        assertEquals(8, t.getCadre());
        assertEquals(2, t.getNombreExemptes());

        Set<String> dansTableau = new HashSet<>();
        for (Tour tour : t.getTours()) {
            for (Match m : tour.getMatchs()) {
                if (!m.getJoueurA().estADeterminer()) {
                    dansTableau.add(m.getJoueurA().getNumeroLicence());
                }
                if (m.getJoueurB() != null && !m.getJoueurB().estADeterminer()) {
                    dansTableau.add(m.getJoueurB().getNumeroLicence());
                }
            }
        }
        for (Qualifie e : elimines) {
            assertFalse(dansTableau.contains(e.joueur().getNumeroLicence()),
                    "un éliminé ne doit pas être dans le tableau");
        }
    }

    @Test
    void configuration_depuis_format_reprend_les_regles() {
        ConfigurationTableau cfg = ConfigurationTableau.depuisFormat(format).build();
        assertEquals(3, cfg.getTaillePouleSouhaitee());
        assertEquals(2, cfg.getQualifiesParPoule());
        assertEquals(3, cfg.getNombreManchesGagnantes());
    }

    @Test
    void un_seul_format_est_disponible_aujourdhui() {
        assertEquals(1, RegistreFormats.tousLesFormats().size());
        assertTrue(RegistreFormats.parIdentifiant("P3Q2").isPresent());
        assertTrue(RegistreFormats.parIdentifiant("INEXISTANT").isEmpty());
    }
}
