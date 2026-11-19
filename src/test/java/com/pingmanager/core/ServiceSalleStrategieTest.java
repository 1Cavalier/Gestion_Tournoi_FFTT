package com.pingmanager.core;

import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Match;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.StrategieOrdonnancement;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.format.RegistreFormats;
import com.pingmanager.ordonnancement.PoidsOrdonnancement;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * B025 : la salle du jour J classe les matchs proposés (et donc leurs couleurs
 * de priorité, B037) selon la stratégie choisie par le club pour le tournoi.
 */
class ServiceSalleStrategieTest {

    private Instant instant = Instant.parse("2026-11-14T09:00:00Z");
    private final Horloge horloge = () -> instant;

    private static Joueur j(String licence) {
        return new Joueur(licence, "N" + licence, "P" + licence, Sexe.HOMME, "Club" + licence, 1000);
    }

    private static Tableau lancer(Tournoi t, String nom, List<Joueur> joueurs) {
        Tableau tab = new Tableau(nom, ConfigurationTableau.depuisFormat(RegistreFormats.formatParDefaut()).build());
        t.ajouterTableau(tab);
        tab.ouvrirPointage();
        for (Joueur joueur : joueurs) {
            tab.inscrire(joueur);
            tab.pointer(joueur);
        }
        new ServicePointage(t).cloturerPointage(tab);
        new ServiceLancementTableau(t).lancer(tab, 1L);
        return tab;
    }

    /** A lancé à 9 h (une poule en cours), B lancé à 10 h 30 sans partie jouée ; une table libre. */
    private Tournoi tournoi() {
        Tournoi t = new Tournoi("Open", "Club", LocalDate.of(2026, 11, 14));
        t.setNombreDeTables(2);
        Tableau a = lancer(t, "A", List.of(j("A1"), j("A2"), j("A3"), j("A4"), j("A5"), j("A6")));
        lancer(t, "B", List.of(j("B1"), j("B2"), j("B3")));
        // Phase de poules : on lance une poule entière.
        assertTrue(new ServiceSalle(horloge).lancerPoule(t, a.getPoules().get(0), 1).estReussi());
        instant = instant.plusSeconds(90 * 60L);
        return t;
    }

    @Test
    void sans_choix_la_salle_garde_le_classement_historique() {
        Tournoi t = tournoi();
        assertFalse(t.strategieOrdonnancementChoisie());
        List<ListeLancements.Ligne> bord = new ServiceSalle(horloge, ZoneId.systemDefault()).listeLancements(t)
                .getJouables();
        List<ListeLancements.Ligne> imposee = new ServiceSalle(horloge, ZoneId.systemDefault(),
                new PoidsOrdonnancement()).listeLancements(t).getJouables();
        assertEquals(2, bord.size(), "l'autre poule de A et la poule de B");
        assertEquals(imposee.stream().map(l -> l.match().match()).toList(),
                bord.stream().map(l -> l.match().match()).toList());
        assertTrue(bord.stream().noneMatch(l -> l.match().explication().contains("tableau commencé")));
    }


    @Test
    void strategie_avancer_les_tableaux_commences_met_le_tableau_a_en_tete_et_l_explique() {
        Tournoi t = tournoi();
        t.setStrategieOrdonnancement(StrategieOrdonnancement.AVANCER_TABLEAUX_COMMENCES);
        ListeLancements liste = new ServiceSalle(horloge).listeLancements(t);
        TableauDeBordSalle.MatchSalle premier = liste.getJouables().get(0).match();
        assertEquals("A", premier.tableau());
        assertTrue(premier.explication().contains("tableau commencé depuis 90 min"), premier.explication());

        // Des poids imposés par l'appelant l'emportent sur la stratégie du tournoi.
        ListeLancements imposee = new ServiceSalle(horloge, ZoneId.systemDefault(), new PoidsOrdonnancement())
                .listeLancements(t);
        assertTrue(imposee.getJouables().stream().noneMatch(l -> l.match().explication().contains("tableau commencé")));
    }

}
