package com.pingmanager.ui.ecran;

import com.pingmanager.model.AccesClub;
import com.pingmanager.model.FicheLicence;
import com.pingmanager.model.NiveauAcces;
import com.pingmanager.model.Sexe;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PresentationProfilTest {

    private static AccesClub acces(NiveauAcces niveau) {
        return new AccesClub("08910001", "lea@exemple.fr", "Martin", "Léa", niveau);
    }

    private static PresentationProfil.Droit droit(List<PresentationProfil.Droit> droits, String action) {
        return droits.stream().filter(d -> d.action().equals(action)).findFirst().orElseThrow();
    }

    @Test
    void identite_donne_nom_prenom_email_telephone_dans_l_ordre_et_jamais_vide() {
        List<PresentationProfil.Champ> champs =
                PresentationProfil.identite("Martin", "Léa", "lea@exemple.fr", null);

        assertEquals(List.of("Nom", "Prénom", "Email", "Téléphone"),
                champs.stream().map(PresentationProfil.Champ::libelle).toList());
        assertEquals("Léa", champs.get(1).valeur());
        assertEquals(PresentationProfil.NON_RENSEIGNE, champs.get(3).valeur());
    }

    @Test
    void licence_absente_ne_donne_aucune_ligne() {
        assertTrue(PresentationProfil.licence(null, Optional.empty()).isEmpty());
    }

    @Test
    void licence_liee_reprend_toutes_les_informations_disponibles_de_la_fiche() {
        FicheLicence f = new FicheLicence.Builder("9412345", "MARTIN", "Léa")
                .sexe(Sexe.FEMME).club("08910001", "US Brunoy").points(1234).pointsMensuels(1250)
                .classement("12").categorie("S").dateNaissance(LocalDate.of(1995, 3, 7))
                .nationalite("Française").typeLicence("Traditionnelle").saison("2026-2027")
                .statut("Validée").arbitre("AR").jugeArbitre("JA1").technique("EF").build();

        List<String> libelles = PresentationProfil.licence("9412345", Optional.of(f)).stream()
                .map(PresentationProfil.Champ::libelle).toList();

        assertEquals(List.of("N° de licence", "Nom", "Prénom", "Sexe", "Date de naissance",
                "Nationalité", "Catégorie", "Club", "Saison", "Type de licence", "Statut",
                "Classement", "Points (début de phase)", "Points (mensuels)", "Arbitre",
                "Juge-arbitre", "Technique"), libelles);
    }

    @Test
    void liaison_impossible_sans_confirmation_de_l_avertissement() {
        assertEquals(Optional.of(PresentationProfil.MOTIF_SANS_CONFIRMATION),
                PresentationProfil.refusDemandeLiaison(false, "08910001"));
    }

    @Test
    void liaison_confirmee_exige_encore_un_numero_puis_est_acceptee() {
        assertEquals(Optional.of(PresentationProfil.MOTIF_SANS_NUMERO),
                PresentationProfil.refusDemandeLiaison(true, "  "));
        assertTrue(PresentationProfil.refusDemandeLiaison(true, "08910001").isEmpty());
    }

    @Test
    void tableau_des_droits_present_seulement_si_le_compte_est_relie() {
        assertFalse(PresentationProfil.afficheTableauDroits(List.of()));
        assertTrue(PresentationProfil.clubsRelies(List.of(), n -> "US Brunoy").isEmpty());

        List<AccesClub> relie = List.of(acces(NiveauAcces.ADMIN_NIVEAU_1));
        assertTrue(PresentationProfil.afficheTableauDroits(relie));
        List<PresentationProfil.ClubRelie> clubs = PresentationProfil.clubsRelies(relie, n -> "US Brunoy");
        assertEquals(1, clubs.size());
        assertEquals("US Brunoy", clubs.get(0).nom());
        assertFalse(clubs.get(0).droits().isEmpty());
    }

    @Test
    void administrateur_peut_organiser_et_se_dissocier_mais_pas_gerer_les_administrateurs() {
        List<PresentationProfil.Droit> d = PresentationProfil.droits(acces(NiveauAcces.ADMIN_NIVEAU_1));

        assertEquals(PresentationProfil.EtatDroit.OUI, droit(d, "Créer et organiser les tournois du club").etat());
        assertEquals(PresentationProfil.EtatDroit.OUI, droit(d, "Se dissocier du club").etat());
        assertEquals(PresentationProfil.EtatDroit.NON, droit(d, "Gérer les autres administrateurs").etat());
    }

    @Test
    void referent_ne_peut_pas_se_dissocier_et_gestion_des_administrateurs_a_venir() {
        List<PresentationProfil.Droit> d = PresentationProfil.droits(acces(NiveauAcces.ADMIN_PRINCIPAL));

        assertEquals(PresentationProfil.EtatDroit.OUI, droit(d, "Créer et organiser les tournois du club").etat());
        assertEquals(PresentationProfil.EtatDroit.NON, droit(d, "Se dissocier du club").etat());
        assertEquals(PresentationProfil.EtatDroit.A_VENIR, droit(d, "Gérer les autres administrateurs").etat());
        assertFalse(PresentationProfil.clubsRelies(List.of(acces(NiveauAcces.ADMIN_PRINCIPAL)), n -> null)
                .get(0).dissociable());
    }

    @Test
    void club_sans_nom_connu_est_designe_par_son_numero() {
        assertEquals("Club 08910001", PresentationProfil.clubsRelies(
                List.of(acces(NiveauAcces.ADMIN_NIVEAU_1)), n -> null).get(0).nom());
    }

    @Test
    void appel_micro_non_renseigne_et_resume_des_tournois() {
        assertEquals("Non renseignée", PresentationProfil.prononciation(null));
        assertEquals("ma.ʁɛ̃", PresentationProfil.prononciation("ma.ʁɛ̃"));
        assertTrue(PresentationProfil.resumeTournois(false, 0).startsWith("Liez votre licence"));
        assertEquals("Vous êtes inscrit (ou en liste d'attente) à 2 tournois.",
                PresentationProfil.resumeTournois(true, 2));
    }
}
