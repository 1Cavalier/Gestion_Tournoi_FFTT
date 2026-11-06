package com.pingmanager.ui.ecran;

import com.pingmanager.core.CatalogueTournois;
import com.pingmanager.core.EligibiliteTournoi;
import com.pingmanager.core.Geocodeur;
import com.pingmanager.core.RegistreClubsMemoire;
import com.pingmanager.core.StatutCarteTournoi;
import com.pingmanager.model.AdresseTournoi;
import com.pingmanager.model.Club;
import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.FicheLicence;
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
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/** Logique d'affichage de la carte des tournois (présentations carte et agenda). */
class PresentationCarteTournoisTest {

    private static final LocalDate AUJ = LocalDate.of(2026, 11, 5);

    private final RegistreClubsMemoire clubs = new RegistreClubsMemoire();
    private final EligibiliteTournoi eligibilite = new EligibiliteTournoi(clubs);

    PresentationCarteTournoisTest() {
        clubs.ajouter(new Club("08911132", "Brunoy", "91", "Brunoy", "a@b.fr"));
        clubs.ajouter(new Club("07450001", "Club du Loiret", "45", "Orléans", "c@d.fr"));
    }

    private static Tournoi tournoi(String nom, String club, NiveauTournoi niveau, String cp, LocalDate debut,
                                   LocalDate fin, int pointMax) {
        Tournoi t = new Tournoi(nom, "Club", debut);
        t.setNumeroAffiliationClub(club);
        t.setNiveau(niveau);
        t.setPeriode(debut, fin);
        t.setAdresse(AdresseTournoi.de("1 rue", cp, "Ville"));
        t.ajouterTableau(new Tableau("A", ConfigurationTableau.builder().placesMax(10).pointMax(pointMax).build()));
        t.changerStatut(StatutTournoi.HOMOLOGUE);
        t.changerStatut(StatutTournoi.PUBLIE);
        return t;
    }

    private List<CatalogueTournois.Entree> catalogue() {
        Map<String, Tournoi> m = new LinkedHashMap<>();
        m.put("enCours", tournoi("En cours", "08911132", NiveauTournoi.REGIONAL, "91800", AUJ.minusDays(1), AUJ, 1000));
        m.put("aVenir", tournoi("À venir", "08911132", NiveauTournoi.REGIONAL, "91800", AUJ.plusDays(9),
                AUJ.plusDays(10), 1000));
        m.put("loiret", tournoi("Loiret", "07450001", NiveauTournoi.DEPARTEMENTAL, "45000", AUJ.plusDays(9),
                AUJ.plusDays(9), 1000));
        m.put("niveau", tournoi("Niveau", "08911132", NiveauTournoi.REGIONAL, "91800", AUJ.plusDays(1),
                AUJ.plusDays(1), 500));
        FicheLicence f = new FicheLicence.Builder("1", "SOUMET", "Quentin").sexe(Sexe.HOMME).club("08911132", "Brunoy")
                .points(800).typeLicence("Compétition").dateNaissance(LocalDate.of(2003, 1, 20)).build();
        return CatalogueTournois.construire(m, f, null, eligibilite, Geocodeur.centroideDuDepartement(), AUJ);
    }

    private static CatalogueTournois.Entree entree(List<CatalogueTournois.Entree> cat, String id) {
        return cat.stream().filter(e -> e.id().equals(id)).findFirst().orElseThrow();
    }

    @Test
    void quatre_statuts_quatre_pastilles_distinctes() {
        List<CatalogueTournois.Entree> cat = catalogue();
        PresentationCarteTournois.Pastille enCours = PresentationCarteTournois.carte(entree(cat, "enCours")).pastille();
        PresentationCarteTournois.Pastille aVenir = PresentationCarteTournois.carte(entree(cat, "aVenir")).pastille();
        PresentationCarteTournois.Pastille loiret = PresentationCarteTournois.carte(entree(cat, "loiret")).pastille();
        PresentationCarteTournois.Pastille niveau = PresentationCarteTournois.carte(entree(cat, "niveau")).pastille();
        assertEquals("En cours", enCours.texte());
        assertEquals("À venir", aVenir.texte());
        assertEquals("Non accessible", loiret.texte());
        assertEquals("Pas à votre niveau", niveau.texte());
        Set<String> classes = Set.of(enCours.classeCss(), aVenir.classeCss(), loiret.classeCss(), niveau.classeCss());
        assertEquals(4, classes.size());
        assertEquals(4, Set.of(enCours.couleur(), aVenir.couleur(), loiret.couleur(), niveau.couleur()).size());
        assertEquals(4, PresentationCarteTournois.legende().stream().map(PresentationCarteTournois.Pastille::classeCss)
                .collect(Collectors.toSet()).size());
    }

    @Test
    void complet_s_affiche_complet() {
        PresentationCarteTournois.Pastille p = PresentationCarteTournois.pastille(
                new StatutCarteTournoi.Verdict(StatutCarteTournoi.Statut.COMPLET_OU_HORS_NIVEAU,
                        StatutCarteTournoi.MOTIF_COMPLET));
        assertEquals("Complet", p.texte());
        assertEquals("statut-carte-complet", p.classeCss());
    }

    @Test
    void carte_non_accessible_grisee_avec_motif_et_carte_ouverte_sans_motif() {
        List<CatalogueTournois.Entree> cat = catalogue();
        PresentationCarteTournois.CarteTournoi loiret = PresentationCarteTournois.carte(entree(cat, "loiret"));
        assertTrue(loiret.grise());
        assertTrue(loiret.motif().contains("IX.102"));
        assertEquals("motif-refus", PresentationCarteTournois.classeMotif(loiret));
        assertEquals("motif-complet", PresentationCarteTournois.classeMotif(
                PresentationCarteTournois.carte(entree(cat, "niveau"))));
        PresentationCarteTournois.CarteTournoi aVenir = PresentationCarteTournois.carte(entree(cat, "aVenir"));
        assertFalse(aVenir.grise());
        assertNull(aVenir.motif());
        assertEquals("10 places restantes sur 10", aVenir.places());
        assertEquals("Régional", aVenir.niveau());
        assertTrue(aVenir.dateLieu().startsWith("Samedi 14 et dimanche 15 novembre 2026 · Ville (91)"),
                aVenir.dateLieu());
    }

    @Test
    void compteur_signale_les_tournois_non_accessibles() {
        assertEquals("4 tournois · 2 non accessibles", PresentationCarteTournois.compteur(catalogue()));
        assertEquals("0 tournoi", PresentationCarteTournois.compteur(List.of()));
    }

    @Test
    void agenda_par_jour_dans_l_ordre_avec_tournois_sur_plusieurs_jours() {
        List<PresentationCarteTournois.Jour> jours = PresentationCarteTournois.agenda(catalogue(), AUJ);
        assertEquals(List.of(AUJ, AUJ.plusDays(1), AUJ.plusDays(9), AUJ.plusDays(10)),
                jours.stream().map(PresentationCarteTournois.Jour::date).toList());
        assertTrue(jours.get(0).aujourdhui());
        assertEquals("Aujourd'hui · jeudi 5 novembre 2026", jours.get(0).titre());
        assertEquals("Demain · vendredi 6 novembre 2026", jours.get(1).titre());
        assertEquals("Samedi 14 novembre 2026", jours.get(2).titre());
        assertEquals(List.of("En cours"), jours.get(0).tournois().stream()
                .map(PresentationCarteTournois.CarteTournoi::nom).toList(), "déjà commencé : à partir d'aujourd'hui");
        assertEquals(2, jours.get(2).tournois().size());
        assertEquals(List.of("À venir"), jours.get(3).tournois().stream()
                .map(PresentationCarteTournois.CarteTournoi::nom).toList());
    }

    @Test
    void libelles_des_deux_presentations() {
        assertEquals("Carte", PresentationCarteTournois.libelleVue(PresentationCarteTournois.Vue.CARTE));
        assertEquals("Agenda par jour", PresentationCarteTournois.libelleVue(PresentationCarteTournois.Vue.AGENDA));
    }
}
