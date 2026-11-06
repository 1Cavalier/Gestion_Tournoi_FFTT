package com.pingmanager.core;

import com.pingmanager.model.AdresseTournoi;
import com.pingmanager.model.Club;
import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.FicheLicence;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.NiveauTournoi;
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

/** Les quatre statuts de la carte des tournois (cahier des charges v0, section 3). */
class StatutCarteTournoiTest {

    private static final LocalDate AUJ = LocalDate.of(2026, 11, 5);

    private final RegistreClubsMemoire clubs = new RegistreClubsMemoire();
    private final EligibiliteTournoi eligibilite = new EligibiliteTournoi(clubs);

    StatutCarteTournoiTest() {
        clubs.ajouter(new Club("08911132", "Brunoy", "91", "Brunoy", "a@b.fr"));
        clubs.ajouter(new Club("07450001", "Club du Loiret", "45", "Orléans", "c@d.fr"));
    }

    private static FicheLicence licencie(int points) {
        return new FicheLicence.Builder("1", "SOUMET", "Quentin").sexe(Sexe.HOMME).club("08911132", "Brunoy")
                .points(points).typeLicence("Compétition").dateNaissance(LocalDate.of(2003, 1, 20)).build();
    }

    /** Tournoi publié du club donné, un tableau « A » (≤ 1000 pts, {@code places} places). */
    private static Tournoi tournoi(String club, NiveauTournoi niveau, String cp, LocalDate jour, int places) {
        Tournoi t = new Tournoi("Tournoi " + niveau.getLibelle(), "Club", jour);
        t.setNumeroAffiliationClub(club);
        t.setNiveau(niveau);
        t.setPeriode(jour, jour);
        t.setAdresse(AdresseTournoi.de("1 rue", cp, "Ville"));
        t.ajouterTableau(new Tableau("A", ConfigurationTableau.builder().placesMax(places).pointMax(1000).build()));
        t.changerStatut(StatutTournoi.HOMOLOGUE);
        t.changerStatut(StatutTournoi.PUBLIE);
        return t;
    }

    private StatutCarteTournoi.Verdict statut(Tournoi t, FicheLicence fiche) {
        Map<String, Tournoi> m = new LinkedHashMap<>();
        m.put("t", t);
        List<CatalogueTournois.Entree> cat = CatalogueTournois.construire(m, fiche, null, eligibilite,
                Geocodeur.centroideDuDepartement(), AUJ);
        assertEquals(1, cat.size());
        return cat.get(0).statut();
    }

    @Test
    void tournoi_accessible_avec_places_est_a_venir_sans_motif() {
        StatutCarteTournoi.Verdict v = statut(tournoi("08911132", NiveauTournoi.REGIONAL, "91800", AUJ.plusDays(10), 10),
                licencie(800));
        assertEquals(StatutCarteTournoi.Statut.A_VENIR, v.statut());
        assertNull(v.motif());
        assertFalse(v.estBloquant());
    }

    @Test
    void tournoi_du_jour_est_en_cours() {
        StatutCarteTournoi.Verdict v = statut(tournoi("08911132", NiveauTournoi.REGIONAL, "91800", AUJ, 10),
                licencie(800));
        assertEquals(StatutCarteTournoi.Statut.EN_COURS, v.statut());
        assertFalse(v.estBloquant());
    }

    @Test
    void departemental_d_un_autre_departement_non_accessible_avec_motif_ix_102() {
        StatutCarteTournoi.Verdict v = statut(tournoi("07450001", NiveauTournoi.DEPARTEMENTAL, "45000",
                AUJ.plusDays(10), 10), licencie(800));
        assertEquals(StatutCarteTournoi.Statut.NON_ACCESSIBLE, v.statut());
        assertTrue(v.motif().contains("IX.102"));
        assertTrue(v.estBloquant());
    }

    @Test
    void aucun_tableau_a_ses_points_donne_le_quatrieme_statut() {
        StatutCarteTournoi.Verdict v = statut(tournoi("08911132", NiveauTournoi.REGIONAL, "91800", AUJ.plusDays(10), 10),
                licencie(1500));
        assertEquals(StatutCarteTournoi.Statut.COMPLET_OU_HORS_NIVEAU, v.statut());
        assertEquals(StatutCarteTournoi.MOTIF_HORS_NIVEAU, v.motif());
        assertTrue(v.estBloquant());
    }

    @Test
    void tableaux_a_son_niveau_complets_donnent_le_quatrieme_statut() {
        Tournoi t = tournoi("08911132", NiveauTournoi.REGIONAL, "91800", AUJ.plusDays(10), 2);
        Tableau a = t.getTableaux().get(0);
        a.inscrire(new Joueur("11", "N11", "P11", Sexe.HOMME, "C", 700));
        a.inscrire(new Joueur("12", "N12", "P12", Sexe.HOMME, "C", 600));
        StatutCarteTournoi.Verdict v = statut(t, licencie(800));
        assertEquals(StatutCarteTournoi.Statut.COMPLET_OU_HORS_NIVEAU, v.statut());
        assertEquals(StatutCarteTournoi.MOTIF_COMPLET, v.motif());
    }

    @Test
    void un_tableau_complet_hors_de_son_niveau_ne_bloque_pas() {
        Tournoi t = tournoi("08911132", NiveauTournoi.REGIONAL, "91800", AUJ.plusDays(10), 10);
        Tableau b = new Tableau("B", ConfigurationTableau.builder().placesMax(1).tranche(1200, 3000).build());
        b.inscrire(new Joueur("13", "N13", "P13", Sexe.HOMME, "C", 1300));
        t.ajouterTableau(b);
        assertEquals(StatutCarteTournoi.Statut.A_VENIR, statut(t, licencie(800)).statut());
    }

    @Test
    void sans_licence_liee_le_statut_ne_depend_que_du_remplissage() {
        Tournoi t = tournoi("07450001", NiveauTournoi.DEPARTEMENTAL, "45000", AUJ.plusDays(10), 1);
        assertEquals(StatutCarteTournoi.Statut.A_VENIR, statut(t, null).statut(), "accès à vérifier : pas un refus");
        t.getTableaux().get(0).inscrire(new Joueur("11", "N11", "P11", Sexe.HOMME, "C", 700));
        assertEquals(StatutCarteTournoi.Statut.COMPLET_OU_HORS_NIVEAU, statut(t, null).statut());
    }

    @Test
    void en_cours_l_emporte_sur_le_refus_d_acces() {
        StatutCarteTournoi.Verdict v = statut(tournoi("07450001", NiveauTournoi.DEPARTEMENTAL, "45000", AUJ, 10),
                licencie(800));
        assertEquals(StatutCarteTournoi.Statut.EN_COURS, v.statut());
    }
}
