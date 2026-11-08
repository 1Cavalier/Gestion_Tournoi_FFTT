package com.pingmanager.ui.ecran;

import com.pingmanager.core.AnnonceRencontre;
import com.pingmanager.core.ResultatAnnonceVocale;
import com.pingmanager.core.ResultatDemandeAnnonce;
import com.pingmanager.core.ServiceAnnoncesVocales;
import com.pingmanager.core.SyntheseVocale;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Match;
import com.pingmanager.model.ModeAnnonce;
import com.pingmanager.model.ReglagesAnnoncesVocales;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.Tournoi;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

/** B017 : libellés et messages des annonces vocales dans la salle. */
class PresentationSalleVoixTest {

    private final Match match = new Match(new Joueur("1", "MORIN", "Hugo", Sexe.HOMME, "TT Brunoy", 1),
            new Joueur("2", "DURAND", "Léa", Sexe.FEMME, "US Évry", 1));
    private final Tournoi tournoi = new Tournoi("Open", "TT Brunoy", LocalDate.of(2026, 11, 14));

    @Test
    void libelles_du_menu_voix() {
        ReglagesAnnoncesVocales r = new ReglagesAnnoncesVocales();
        assertEquals("Voix : à la demande", PresentationSalle.libelleVoix(r, true));
        r.setAnnonceAutomatique(true);
        assertEquals("Voix : auto", PresentationSalle.libelleVoix(r, true));
        assertEquals("Voix : indisponible", PresentationSalle.libelleVoix(r, false));
        assertEquals("Dire 2 fois", PresentationSalle.libelleDiffusions(2));
        assertEquals("Pas de rappel", PresentationSalle.libelleRappel(0));
        assertEquals("Rappel après 5 min", PresentationSalle.libelleRappel(5));
        assertEquals("Lent", PresentationSalle.libelleDebit(-3));
        assertEquals("Normal", PresentationSalle.libelleDebit(0));
        assertEquals("Rapide", PresentationSalle.libelleDebit(3));
    }

    @Test
    void messages_apres_une_demande_d_annonce() {
        ServiceAnnoncesVocales s = new ServiceAnnoncesVocales(Instant::now, SyntheseVocale.AUCUNE);
        ResultatDemandeAnnonce premiere = s.annoncer(tournoi, match, 4);
        assertEquals("Annonce de la table 4 en cours.", PresentationSalle.messageDemandeAnnonce(premiere, true));
        assertEquals("Voix indisponible : annonce de la table 4 affichée, à lire au micro.",
                PresentationSalle.messageDemandeAnnonce(premiere, false));
        Match autre = new Match(new Joueur("3", "ROUX", "Max", Sexe.HOMME, "", 1),
                new Joueur("4", "BLANC", "Zoé", Sexe.FEMME, "", 1));
        assertEquals("Annonce de la table 6 en file (1 avant elle).",
                PresentationSalle.messageDemandeAnnonce(s.annoncer(tournoi, autre, 6), true));
        assertEquals("La rencontre n'est sur aucune table.",
                PresentationSalle.messageDemandeAnnonce(s.annoncer(tournoi, match, 0), true));
    }

    @Test
    void annonce_non_dite_affichee_avec_son_motif() {
        ServiceAnnoncesVocales s = new ServiceAnnoncesVocales(Instant::now, SyntheseVocale.AUCUNE);
        s.annoncer(tournoi, match, 4);
        ResultatAnnonceVocale r = s.diffuserSuivante().orElseThrow();
        assertEquals("À lire au micro (synthèse vocale indisponible sur cet ordinateur) · Hugo Morin, club TT Brunoy, "
                + "contre Léa Durand, club US Évry, table 4.", PresentationSalle.texteRepli(r));
    }

    @Test
    void annonce_affichee_pour_l_appel_vocal_et_heure_du_rappel() {
        String t = PresentationSalle.texteAnnonce(AnnonceRencontre.preparer(ModeAnnonce.APPEL, match, 4, null));
        assertTrue(t.startsWith("Appel vocal (dit par l'ordinateur"), t);
        assertTrue(PresentationSalle.infoRappel(Instant.parse("2026-11-14T09:05:00Z"), ZoneOffset.UTC)
                .startsWith("Rappel prévu à 09h05"));
    }
}
