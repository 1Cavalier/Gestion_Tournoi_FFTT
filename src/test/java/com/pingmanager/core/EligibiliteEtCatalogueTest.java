package com.pingmanager.core;

import com.pingmanager.model.AdresseTournoi;
import com.pingmanager.model.Club;
import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.Coordonnees;
import com.pingmanager.model.FicheLicence;
import com.pingmanager.model.NiveauTournoi;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.StatutTournoi;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** Accès IX.101 / IX.102, référentiel des territoires et filtres du catalogue. */
class EligibiliteEtCatalogueTest {

    private final RegistreClubsMemoire clubs = new RegistreClubsMemoire();
    private final EligibiliteTournoi eligibilite = new EligibiliteTournoi(clubs);

    EligibiliteEtCatalogueTest() {
        clubs.ajouter(new Club("08911132", "Brunoy", "91", "Brunoy", "a@b.fr"));
        clubs.ajouter(new Club("07450001", "Club du Loiret", "45", "Orléans", "c@d.fr"));
        clubs.ajouter(new Club("08940001", "Club du Val-de-Marne", "94", "Créteil", "e@f.fr"));
    }

    private static FicheLicence licencie(String club, String type) {
        return new FicheLicence.Builder("1", "SOUMET", "Quentin").sexe(Sexe.HOMME).club(club, "x")
                .points(923).typeLicence(type).dateNaissance(LocalDate.of(2003, 1, 20)).build();
    }

    private static Tournoi tournoi(String club, NiveauTournoi niveau, String cp, LocalDate jour) {
        Tournoi t = new Tournoi("Tournoi " + niveau.getLibelle(), "Club", jour);
        t.setNumeroAffiliationClub(club);
        t.setNiveau(niveau);
        t.setPeriode(jour, jour);
        t.setAdresse(AdresseTournoi.de("1 rue", cp, "Ville"));
        t.ajouterTableau(new Tableau("A", ConfigurationTableau.builder().placesMax(10).pointMax(1000).build()));
        t.ajouterTableau(new Tableau("B", ConfigurationTableau.builder().placesMax(10).pointMax(800).build()));
        return t;
    }

    @Test
    void territoires_codes_postaux_et_ligues() {
        assertEquals("91", Territoires.departementDuCodePostal("91800").orElseThrow().code());
        assertEquals("2A", Territoires.departementDuCodePostal("20000").orElseThrow().code());
        assertEquals("2B", Territoires.departementDuCodePostal("20200").orElseThrow().code());
        assertEquals("974", Territoires.departementDuCodePostal("97400").orElseThrow().code());
        assertEquals("Île-de-France", Territoires.ligue("91").orElseThrow());
        assertEquals("Centre-Val de Loire", Territoires.ligue("45").orElseThrow());
        assertEquals("09", Territoires.departement("9").orElseThrow().code());
        assertEquals("de l'Essonne", Territoires.departement("91").orElseThrow().formeDe());
        assertEquals("du Loiret", Territoires.departement("45").orElseThrow().formeDe());
        assertTrue(Territoires.tous().size() >= 101);
    }

    @Test
    void departemental_reserve_au_departement_du_club_organisateur() {
        LocalDate j = LocalDate.of(2027, 1, 10);
        EligibiliteTournoi.Acces loiret = eligibilite.evaluer(tournoi("07450001", NiveauTournoi.DEPARTEMENTAL, "45000", j),
                licencie("08911132", "Compétition"));
        assertTrue(loiret.estRefuse());
        assertEquals("Réservé aux licenciés du Loiret (45) — tournoi départemental (IX.102).", loiret.motif());

        assertTrue(eligibilite.evaluer(tournoi("08911132", NiveauTournoi.DEPARTEMENTAL, "91800", j),
                licencie("08911132", "Compétition")).estAccessible());
    }

    @Test
    void regional_reserve_a_la_ligue_du_club_organisateur() {
        LocalDate j = LocalDate.of(2027, 1, 10);
        assertTrue(eligibilite.evaluer(tournoi("08940001", NiveauTournoi.REGIONAL, "94000", j),
                licencie("08911132", "Compétition")).estAccessible(), "Val-de-Marne et Essonne : même ligue");
        EligibiliteTournoi.Acces a = eligibilite.evaluer(tournoi("07450001", NiveauTournoi.REGIONAL, "45000", j),
                licencie("08911132", "Compétition"));
        assertTrue(a.estRefuse());
        assertTrue(a.motif().contains("Centre-Val de Loire"));
    }

    @Test
    void departement_du_joueur_deduit_du_numero_de_club_inconnu() {
        LocalDate j = LocalDate.of(2027, 1, 10);
        // Club absent du référentiel : 3e et 4e chiffres du numéro FFTT.
        assertTrue(eligibilite.evaluer(tournoi("07450001", NiveauTournoi.DEPARTEMENTAL, "45000", j),
                licencie("07459999", "Compétition")).estAccessible());
    }

    @Test
    void licence_loisir_reservee_aux_tournois_promotionnels() {
        LocalDate j = LocalDate.of(2027, 1, 10);
        FicheLicence loisir = licencie("08911132", "Loisir");
        EligibiliteTournoi.Acces nb = eligibilite.evaluer(tournoi("08911132", NiveauTournoi.NATIONAL_B, "91800", j), loisir);
        assertTrue(nb.estRefuse());
        assertTrue(nb.motif().contains("IX.101"));
        assertTrue(eligibilite.evaluer(tournoi("07450001", NiveauTournoi.PROMOTIONNEL, "45000", j), loisir).estAccessible());

        Tournoi ouvert = tournoi("08911132", NiveauTournoi.NATIONAL_B, "91800", j);
        ouvert.setAccepteLicencePromotionnelle(true);
        EligibiliteTournoi.Acces a = eligibilite.evaluer(ouvert, loisir);
        assertTrue(a.estAccessible());
        assertNotNull(a.remarque());
    }

    @Test
    void sans_licence_liee_l_acces_est_a_verifier() {
        assertEquals(EligibiliteTournoi.Statut.A_VERIFIER, eligibilite.evaluer(
                tournoi("08911132", NiveauTournoi.REGIONAL, "91800", LocalDate.now()), null).statut());
    }

    // ------------------------------------------------------------------ catalogue

    private Map<String, Tournoi> jeu(LocalDate aujourdhui) {
        Map<String, Tournoi> m = new LinkedHashMap<>();
        Tournoi a = tournoi("08940001", NiveauTournoi.REGIONAL, "94000", aujourdhui.plusDays(20));
        Tournoi b = tournoi("07450001", NiveauTournoi.DEPARTEMENTAL, "45000", aujourdhui.plusDays(40));
        Tournoi passe = tournoi("08911132", NiveauTournoi.REGIONAL, "91800", aujourdhui.minusDays(3));
        Tournoi enCours = tournoi("08911132", NiveauTournoi.NATIONAL_B, "91800", aujourdhui);
        Tournoi brouillon = tournoi("08911132", NiveauTournoi.NATIONAL_B, "91800", aujourdhui.plusDays(5));
        for (Tournoi t : List.of(a, b, passe, enCours)) {
            t.changerStatut(StatutTournoi.HOMOLOGUE);
            t.changerStatut(StatutTournoi.PUBLIE);
        }
        a.setAdresse(a.getAdresse().avecCoordonnees(new Coordonnees(48.79, 2.455)));
        m.put("a", a);
        m.put("b", b);
        m.put("passe", passe);
        m.put("enCours", enCours);
        m.put("brouillon", brouillon);
        return m;
    }

    @Test
    void catalogue_publie_non_termine_avec_position_et_acces() {
        LocalDate auj = LocalDate.of(2026, 11, 2);
        List<CatalogueTournois.Entree> cat = CatalogueTournois.construire(jeu(auj), licencie("08911132", "Compétition"),
                Territoires.departement("91").orElseThrow().prefecture(), eligibilite,
                Geocodeur.centroideDuDepartement(), auj);
        assertEquals(List.of("enCours", "a", "b"), cat.stream().map(CatalogueTournois.Entree::id).toList());
        CatalogueTournois.Entree a = cat.get(1);
        assertFalse(a.positionApprochee());
        assertEquals(CalendrierTournoi.Moment.EN_COURS, cat.get(0).moment());
        CatalogueTournois.Entree b = cat.get(2);
        assertTrue(b.positionApprochee(), "repli : préfecture du Loiret");
        assertTrue(b.acces().estRefuse());
        assertEquals(20, b.placesTotales());
        assertTrue(b.distanceKm() > 80 && b.distanceKm() < 120);
    }

    @Test
    void filtres_composables() {
        LocalDate auj = LocalDate.of(2026, 11, 2);
        List<CatalogueTournois.Entree> cat = CatalogueTournois.construire(jeu(auj), licencie("08911132", "Compétition"),
                Territoires.departement("91").orElseThrow().prefecture(), eligibilite,
                Geocodeur.centroideDuDepartement(), auj);
        CatalogueTournois.Criteres c = CatalogueTournois.Criteres.aucun();
        assertEquals(3, CatalogueTournois.filtrer(cat, c).size());
        assertEquals(2, CatalogueTournois.filtrer(cat, c.avecSeulementAccessibles(true)).size());
        assertEquals(1, CatalogueTournois.filtrer(cat, c.avecTexte("loiret")).size());
        assertEquals(1, CatalogueTournois.filtrer(cat, c.avecNiveaux(EnumSet.of(NiveauTournoi.REGIONAL))).size());
        assertEquals(2, CatalogueTournois.filtrer(cat, c.avecLigue("Île-de-France")).size());
        assertEquals(1, CatalogueTournois.filtrer(cat, c.avecDepartement("45")).size());
        assertEquals(2, CatalogueTournois.filtrer(cat, c.avecDistanceMax(50)).size());
        assertEquals(1, CatalogueTournois.filtrer(cat, c.avecPeriode(auj.plusDays(10), auj.plusDays(30))).size());
        assertEquals(3, CatalogueTournois.filtrer(cat, c.avecMesPoints(true).avecPlaces(true)).size());
    }

    @Test
    void geocodeur_repli_et_completion() {
        Geocodeur enPanne = a -> { throw new IllegalStateException("réseau"); };
        Geocodeur g = enPanne.ouSinon(Geocodeur.centroideDuDepartement());
        AdresseTournoi a = g.completer(AdresseTournoi.de("157 Route de Brie", "91800", "Brunoy"));
        assertNotNull(a.getCoordonnees());
        Coordonnees fixe = new Coordonnees(48.69, 2.50);
        AdresseTournoi deja = AdresseTournoi.de("x", "91800", "Brunoy").avecCoordonnees(fixe);
        assertSame(deja, g.completer(deja));
        assertEquals(Optional.empty(), Geocodeur.centroideDuDepartement().geocoder(AdresseTournoi.partielle(null, null, null, "abc", null)));
    }
}
