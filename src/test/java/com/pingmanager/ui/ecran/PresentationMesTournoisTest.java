package com.pingmanager.ui.ecran;

import com.pingmanager.core.PerformancesJoueur;
import com.pingmanager.core.ServiceAgenda;
import com.pingmanager.core.ServiceInscriptionEnLigne;
import com.pingmanager.model.AgendaCompte;
import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.StatutTournoi;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.format.RegistreFormats;
import com.pingmanager.ui.ContexteApplication.TournoiFichier;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** « Mes tournois » : trois parties dans l'ordre, performances, désinscription, agenda. */
class PresentationMesTournoisTest {

    private static final LocalDate AUJ = LocalDate.of(2027, 4, 10);
    private static final String MOI = "7512345";

    private static Tournoi publie(String nom, LocalDate date) {
        Tournoi t = new Tournoi(nom, "Club", date);
        t.changerStatut(StatutTournoi.HOMOLOGUE);
        t.changerStatut(StatutTournoi.PUBLIE);
        return t;
    }

    private static TournoiFichier tf(Tournoi t) {
        // Nom de fichier ASCII : Path.of refuse les accents sans locale UTF-8 (LANG=C, CI).
        return new TournoiFichier(Path.of(t.getNom().replaceAll("[^A-Za-z0-9]", "_") + ".json"), t);
    }

    private static Tableau tableau(Tournoi t, String nom) {
        Tableau tab = new Tableau(nom, ConfigurationTableau.depuisFormat(RegistreFormats.formatParDefaut()).build());
        t.ajouterTableau(tab);
        return tab;
    }

    private static Joueur moi() {
        return new Joueur(MOI, "Martin", "Léa", Sexe.FEMME, "Brunoy", 900);
    }

    private static List<String> noms(List<TournoiFichier> l) {
        return l.stream().map(x -> x.tournoi().getNom()).toList();
    }

    @Test
    void trois_parties_en_cours_puis_inscriptions_par_date_puis_passes_du_plus_recent_sans_brouillon() {
        Tournoi brouillon = new Tournoi("Brouillon", "Club", AUJ.plusDays(3));
        List<TournoiFichier> mes = List.of(
                tf(publie("Mai", AUJ.plusMonths(1))),
                tf(publie("Janvier", AUJ.minusMonths(3))),
                tf(publie("Aujourd'hui", AUJ)),
                tf(brouillon),
                tf(publie("Demain", AUJ.plusDays(1))),
                tf(publie("Mars", AUJ.minusMonths(1))));

        PresentationMesTournois.Repartition r = PresentationMesTournois.repartir(mes, AUJ);

        assertEquals(List.of("Aujourd'hui"), noms(r.enCours()));
        assertEquals(List.of("Demain", "Mai"), noms(r.inscriptions()));
        assertEquals(List.of("Mars", "Janvier"), noms(r.passes()));
    }

    @Test
    void performances_au_singulier_au_pluriel_et_forfaits_signales() {
        Tableau a = new Tableau("Tableau A", ConfigurationTableau.depuisFormat(RegistreFormats.formatParDefaut()).build());
        assertEquals("Aucun match enregistré",
                PresentationMesTournois.performances(new PerformancesJoueur.Bilan(0, 0, 0, List.of())));
        assertEquals("1 victoire · 0 défaite",
                PresentationMesTournois.performances(new PerformancesJoueur.Bilan(1, 0, 0, List.of())));
        assertEquals("3 victoires · 2 défaites · dont 1 forfait ou abandon",
                PresentationMesTournois.performances(new PerformancesJoueur.Bilan(3, 2, 1, List.of())));
        assertEquals("1 victoire · 1 défaite · dont 2 forfaits ou abandons",
                PresentationMesTournois.performances(new PerformancesJoueur.Bilan(1, 1, 2, List.of())));
        assertEquals("Tableau A : 2 victoires · 1 défaite", PresentationMesTournois.performancesTableau(
                new PerformancesJoueur.BilanTableau(a, 2, 1, 0)));
    }

    @Test
    void boutons_suivi_en_direct_ou_fiche_et_archive_du_suivi() {
        Tournoi t = publie("Open", AUJ);
        assertEquals("Voir la fiche", PresentationMesTournois.texteBoutonEnCours(t), "aucun tableau lancé");
        assertFalse(PresentationMesTournois.peutRevoir(t));
        assertEquals("Voir la fiche", PresentationMesTournois.texteBoutonPasse(t));

        assertEquals("Terminé", PresentationSuiviTournoi.texteBadge(true));
        assertEquals("En cours", PresentationSuiviTournoi.texteBadge(false));
        assertEquals("badge-termine", PresentationSuiviTournoi.classeBadge(true));
        assertEquals("Résultats · lecture seule", PresentationSuiviTournoi.textePastille(true));
    }

    @Test
    void desinscription_possible_seulement_sur_les_tableaux_encore_en_inscription() {
        Tournoi t = publie("Open", AUJ.plusDays(5));
        Tableau a = tableau(t, "A");
        Tableau b = tableau(t, "B");
        Tableau c = tableau(t, "C");
        a.inscrire(moi());
        b.inscrire(moi());
        b.ouvrirPointage();

        assertEquals(List.of(a), PresentationMesTournois.tableauxDesinscriptibles(t, MOI), "B est en pointage, C sans moi");
        assertTrue(PresentationMesTournois.tableauxDesinscriptibles(t, null).isEmpty());
        assertTrue(PresentationMesTournois.tableauxDesinscriptibles(t, "AUTRE").isEmpty());
        assertEquals("Vous quitterez le tableau A. Votre place sera proposée au premier de la liste d'attente.",
                PresentationMesTournois.texteConfirmationDesinscription(List.of(a)));
        assertTrue(PresentationMesTournois.texteConfirmationDesinscription(List.of(a, c)).startsWith(
                "Vous quitterez les tableaux A, C."));
        assertEquals("Choisissez au moins un tableau.",
                PresentationMesTournois.texteConfirmationDesinscription(List.of()));
    }

    @Test
    void message_de_desinscription_motif_du_refus_ou_bilan() {
        Tournoi t = publie("Open", AUJ.plusDays(5));
        Tableau a = tableau(t, "A");
        assertEquals("Choisissez au moins un tableau.", PresentationMesTournois.messageDesinscription(
                new ServiceInscriptionEnLigne.BilanDesinscription("Choisissez au moins un tableau.", List.of(),
                        Map.of(), false, null)));
        assertEquals("Désinscription enregistrée (A). Le tournoi est retiré de votre agenda.",
                PresentationMesTournois.messageDesinscription(new ServiceInscriptionEnLigne.BilanDesinscription(
                        null, List.of(a), Map.of(), true, null)));
        assertEquals("Désinscription enregistrée (A).",
                PresentationMesTournois.messageDesinscription(new ServiceInscriptionEnLigne.BilanDesinscription(
                        null, List.of(a), Map.of(), false, null)));
    }

    @Test
    void agenda_synchronise_les_tournois_a_venir_et_laisse_les_passes() {
        Tournoi futur = publie("Futur", AUJ.plusDays(7));
        tableau(futur, "A").inscrire(moi());
        Tournoi passe = publie("Passé", AUJ.minusDays(7));
        tableau(passe, "A").inscrire(moi());
        Tournoi autre = publie("Autre", AUJ.plusDays(8));
        tableau(autre, "A");
        List<TournoiFichier> tous = List.of(tf(futur), tf(passe), tf(autre));
        AgendaCompte agenda = new AgendaCompte("lea@club.fr");
        ServiceAgenda service = ServiceAgenda.parDefaut();

        assertTrue(PresentationMesTournois.synchroniserAgenda(agenda, tous, MOI, AUJ, service));
        assertEquals(1, agenda.taille(), "seul le tournoi à venir est ajouté");
        assertEquals("Futur", agenda.evenements().get(0).getTitre());
        assertFalse(PresentationMesTournois.synchroniserAgenda(agenda, tous, MOI, AUJ, service), "déjà à jour");

        // Désinscrit par l'organisateur : la date disparaît à la synchronisation suivante.
        futur.getTableaux().get(0).desinscrire(futur.getTableaux().get(0).getInscrits().get(0));
        assertTrue(PresentationMesTournois.synchroniserAgenda(agenda, tous, MOI, AUJ, service));
        assertEquals(0, agenda.taille());

        assertFalse(PresentationMesTournois.synchroniserAgenda(null, tous, MOI, AUJ, service));
        assertFalse(PresentationMesTournois.synchroniserAgenda(agenda, tous, null, AUJ, service));
    }

    @Test
    void seule_une_date_personnelle_se_retire() {
        assertTrue(PresentationMesTournois.peutRetirer(new PresentationAgenda.LigneAgenda("p", "j", "h", "t", "",
                "Date personnelle", false, false)));
        assertFalse(PresentationMesTournois.peutRetirer(new PresentationAgenda.LigneAgenda("t", "j", "h", "t", "",
                "Tournoi", true, false)));
    }
}
