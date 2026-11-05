package com.pingmanager.ui.ecran;

import com.pingmanager.model.FicheLicence;
import com.pingmanager.model.Sexe;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PresentationAccueilTest {

    private static final Set<String> RUBRIQUES = Set.of(
            PresentationAccueil.PROFIL, PresentationAccueil.TOURNOIS, PresentationAccueil.AGENDA,
            PresentationAccueil.STATS, PresentationAccueil.GESTION, PresentationAccueil.HISTORIQUE,
            PresentationAccueil.BUVETTE);

    private static FicheLicence fiche() {
        return new FicheLicence.Builder("9412345", "MARTIN", "Léa").sexe(Sexe.FEMME).points(1234).build();
    }

    private static PresentationAccueil joueur() {
        return PresentationAccueil.pour("Léa", "Martin", Optional.of(fiche()), List.of(), List.of());
    }

    private static PresentationAccueil club() {
        return PresentationAccueil.pour("Léa", "Martin", Optional.of(fiche()), List.of("US Brunoy"), List.of());
    }

    private static List<PresentationAccueil.Carte> toutes(PresentationAccueil p) {
        List<PresentationAccueil.Carte> l = new ArrayList<>(p.cartesPourTous());
        l.addAll(p.cartesClub());
        return l;
    }

    @Test
    void compte_joueur_ne_voit_pas_la_section_gestion_de_votre_club() {
        PresentationAccueil p = joueur();
        assertFalse(p.sectionClubVisible());
        assertTrue(p.cartesClub().isEmpty());
        assertTrue(p.cartesPourTous().stream().noneMatch(c -> c.destination().equals(PresentationAccueil.GESTION)));
    }

    @Test
    void compte_relie_a_un_club_voit_la_section_gestion_de_votre_club() {
        PresentationAccueil p = club();
        assertTrue(p.sectionClubVisible());
        assertEquals("Gestion de votre club", p.titreSectionClub());
        assertTrue(p.introSectionClub().contains("US Brunoy"));
        assertEquals(List.of(PresentationAccueil.GESTION, PresentationAccueil.HISTORIQUE, PresentationAccueil.BUVETTE),
                p.cartesClub().stream().map(PresentationAccueil.Carte::destination).toList());
        assertFalse(p.cartesClub().get(0).aVenir());
        assertTrue(p.cartesClub().get(2).aVenir());
    }

    @Test
    void nom_de_club_vide_ne_suffit_pas_a_ouvrir_la_section_club() {
        PresentationAccueil p = PresentationAccueil.pour("Léa", "Martin", Optional.empty(), List.of(" "), List.of());
        assertFalse(p.sectionClubVisible());
    }

    @Test
    void cartes_pour_tous_dans_l_ordre_profil_tournois_agenda_statistiques() {
        assertEquals(List.of("Profil", "Tournois", "Agenda", "Statistiques"),
                club().cartesPourTous().stream().map(PresentationAccueil.Carte::titre).toList());
    }

    @Test
    void chaque_carte_a_une_explication_une_action_et_une_destination_connue() {
        for (PresentationAccueil.Carte c : toutes(club())) {
            assertTrue(RUBRIQUES.contains(c.destination()), c.titre() + " → " + c.destination());
            assertFalse(c.explication().isBlank(), c.titre());
            assertFalse(c.action().isBlank(), c.titre());
        }
    }

    @Test
    void profil_montre_nom_prenom_points_et_sexe_de_la_licence() {
        PresentationAccueil.Carte profil = joueur().cartesPourTous().get(0);
        assertEquals(List.of(
                new PresentationAccueil.Champ("Nom", "MARTIN"),
                new PresentationAccueil.Champ("Prénom", "Léa"),
                new PresentationAccueil.Champ("Points", "1234"),
                new PresentationAccueil.Champ("Sexe", "Femme")), profil.champs());
        assertTrue(profil.explication().contains("informations personnelles"));
        assertEquals("Bonjour Léa", joueur().salutation());
    }

    @Test
    void sans_licence_liee_le_profil_reprend_le_compte_et_invite_a_lier_la_licence() {
        PresentationAccueil p = PresentationAccueil.pour("Paul", "Durand", Optional.empty(), List.of(), List.of());
        PresentationAccueil.Carte profil = p.cartesPourTous().get(0);
        assertEquals("Durand", profil.champs().get(0).valeur());
        assertEquals("—", profil.champs().get(2).valeur());
        assertEquals("—", profil.champs().get(3).valeur());
        assertTrue(profil.explication().contains("Liez votre licence"));
    }

    @Test
    void agenda_a_venir_montre_les_trois_prochaines_dates_dans_l_ordre() {
        List<PresentationAccueil.DateAgenda> dates = List.of(
                new PresentationAccueil.DateAgenda(LocalDate.of(2026, 12, 20), "C"),
                new PresentationAccueil.DateAgenda(LocalDate.of(2026, 11, 8), "A"),
                new PresentationAccueil.DateAgenda(null, "sans date"),
                new PresentationAccueil.DateAgenda(LocalDate.of(2027, 1, 10), "D"),
                new PresentationAccueil.DateAgenda(LocalDate.of(2026, 11, 22), "B"));
        PresentationAccueil.Carte agenda = PresentationAccueil.pour("Léa", "Martin", Optional.empty(), List.of(), dates)
                .cartesPourTous().get(2);
        assertTrue(agenda.aVenir());
        assertEquals(List.of("A", "B", "C"), agenda.champs().stream().map(PresentationAccueil.Champ::valeur).toList());
        assertTrue(agenda.champs().get(0).libelle().startsWith("Dim"), agenda.champs().get(0).libelle());
    }

    @Test
    void statistiques_est_annoncee_bientot() {
        assertTrue(joueur().cartesPourTous().get(3).aVenir());
    }
}
