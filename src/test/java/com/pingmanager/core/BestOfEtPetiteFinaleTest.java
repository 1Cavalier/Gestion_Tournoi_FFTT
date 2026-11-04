package com.pingmanager.core;

import com.pingmanager.model.Joueur;
import com.pingmanager.model.Manche;
import com.pingmanager.model.Match;
import com.pingmanager.model.Poule;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.StatutMatch;
import com.pingmanager.model.TableauElimination;
import com.pingmanager.model.Tour;
import com.pingmanager.model.format.FormatConfigurable;
import com.pingmanager.model.format.FormatTournoi;
import com.pingmanager.model.format.RegistreFormats;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BestOfEtPetiteFinaleTest {

    private final MoteurTournoi moteur = new MoteurTournoi();

    private Joueur j(String lic, int pts) {
        return new Joueur(lic, "N" + lic, "P" + lic, Sexe.HOMME, "Cl" + lic, pts);
    }

    private Poule pouleJouee(String nom, Joueur a, Joueur b, Joueur c) {
        Poule p = new Poule(nom, List.of(a, b, c));
        Match m1 = new Match(a, b);
        m1.definirScore(List.of(new Manche(11, 5), new Manche(11, 5), new Manche(11, 5)));
        Match m2 = new Match(a, c);
        m2.definirScore(List.of(new Manche(11, 5), new Manche(11, 5), new Manche(11, 5)));
        Match m3 = new Match(b, c);
        m3.definirScore(List.of(new Manche(11, 5), new Manche(11, 5), new Manche(11, 5)));
        p.ajouterMatch(m1);
        p.ajouterMatch(m2);
        p.ajouterMatch(m3);
        return p;
    }

    private List<Poule> quatrePoulesJouees() {
        List<Poule> poules = new ArrayList<>();
        String[] n = {"A", "B", "C", "D"};
        for (int i = 0; i < 4; i++) {
            poules.add(pouleJouee(n[i], j(n[i] + "1", 2000 - i * 30),
                    j(n[i] + "2", 1500 - i * 30), j(n[i] + "3", 1000 - i * 30)));
        }
        return poules;
    }

    private FormatTournoi formatAvecFinaleBo7EtPetiteFinale() {
        return FormatConfigurable.builder()
                .nom("P3Q2 finale BO7").identifiant("P3Q2-F7")
                .taillePouleSouhaitee(3).qualifiesParPoule(2)
                .nombreManchesGagnantes(3)
                .nombreManchesGagnantesFinale(4)
                .avecPetiteFinale(true)
                .nombreManchesGagnantesPetiteFinale(2)
                .build();
    }

    private Tour tourNomme(TableauElimination t, String nom) {
        for (Tour tr : t.getTours()) {
            if (tr.getNom().equals(nom)) {
                return tr;
            }
        }
        return null;
    }

    @Test
    void applique_un_best_of_specifique_a_la_finale() {
        TableauElimination t = moteur.construireTableau(
                quatrePoulesJouees(), formatAvecFinaleBo7EtPetiteFinale(), 3L);

        Tour finale = tourNomme(t, "Finale");
        Tour quarts = t.getTours().get(0);
        assertNotNull(finale);
        assertEquals(4, finale.getMatchs().get(0).getNombreManchesGagnantes());
        assertEquals(3, quarts.getMatchs().get(0).getNombreManchesGagnantes());
    }

    @Test
    void ajoute_une_petite_finale_avec_son_propre_best_of() {
        TableauElimination t = moteur.construireTableau(
                quatrePoulesJouees(), formatAvecFinaleBo7EtPetiteFinale(), 3L);

        Tour petite = tourNomme(t, "Petite finale");
        assertNotNull(petite);
        assertEquals(1, petite.getMatchs().size());
        assertEquals(2, petite.getMatchs().get(0).getNombreManchesGagnantes());
    }

    @Test
    void la_petite_finale_oppose_les_perdants_des_demi_finales() {
        FormatTournoi format = formatAvecFinaleBo7EtPetiteFinale();
        TableauElimination t = moteur.construireTableau(quatrePoulesJouees(), format, 3L);
        ProgressionTableau progression = new ProgressionTableau();

        int garde = 0;
        for (Tour tour : t.getTours()) {
            for (Match m : tour.getMatchs()) {
                if (m.getStatut() == StatutMatch.A_JOUER && !m.estBye()) {
                    int need = m.getNombreManchesGagnantes();
                    List<Manche> ms = new ArrayList<>();
                    for (int k = 0; k < need; k++) {
                        ms.add(new Manche(11, 5));
                    }
                    m.definirScore(ms);
                }
            }
            progression.propager(t);
            if (garde++ > 30) break;
        }

        Tour demies = tourNomme(t, "Demi-finales");
        Tour petite = tourNomme(t, "Petite finale");
        assertNotNull(demies);
        assertNotNull(petite);

        Set<String> perdantsDemies = new HashSet<>();
        for (Match d : demies.getMatchs()) {
            if (d.getPerdant() != null) {
                perdantsDemies.add(d.getPerdant().getNumeroLicence());
            }
        }
        Match pf = petite.getMatchs().get(0);
        Set<String> joueursPetiteFinale = new HashSet<>();
        joueursPetiteFinale.add(pf.getJoueurA().getNumeroLicence());
        joueursPetiteFinale.add(pf.getJoueurB().getNumeroLicence());

        assertEquals(perdantsDemies, joueursPetiteFinale);
        assertEquals(StatutMatch.TERMINE, pf.getStatut());
    }

    @Test
    void le_format_standard_na_pas_de_petite_finale_et_finale_en_best_of_5() {
        TableauElimination t = moteur.construireTableau(
                quatrePoulesJouees(), RegistreFormats.formatParDefaut(), 3L);

        assertNull(tourNomme(t, "Petite finale"));
        Tour finale = tourNomme(t, "Finale");
        assertNotNull(finale);
        assertEquals(3, finale.getMatchs().get(0).getNombreManchesGagnantes());
    }

    @Test
    void un_match_nest_gagne_quen_atteignant_le_nombre_de_manches_gagnantes() {
        Match m = new Match(j("A", 1000), j("B", 1000));
        m.setNombreManchesGagnantes(3);
        // 2-1 ne suffit pas : le match n'est pas décidé.
        m.definirScore(List.of(new Manche(11, 5), new Manche(5, 11), new Manche(11, 7)));
        assertNull(m.getVainqueur());
        // 3-1 : décidé.
        m.definirScore(List.of(new Manche(11, 5), new Manche(5, 11),
                new Manche(11, 7), new Manche(11, 9)));
        assertNotNull(m.getVainqueur());
        assertEquals("A", m.getVainqueur().getNumeroLicence());
    }
}
