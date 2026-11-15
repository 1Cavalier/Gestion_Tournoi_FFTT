package com.pingmanager.ui;

import com.pingmanager.core.ServicePreparationJourJ.Acces;
import com.pingmanager.model.Tournoi;
import com.pingmanager.ui.infra.DonneesDemo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** B051 : l'étape « J-7 : préparer le jour J » est toujours ouverte pour les tournois de démo. */
class PreparationJourJContexteTest {

    @TempDir
    Path dossier;

    @Test
    void les_tournois_de_demo_homologue_et_publie_ouvrent_l_etape_quelle_que_soit_la_date() {
        ContexteApplication ctx = new ContexteApplication(dossier, DonneesDemo.clubs(), DonneesDemo.licences());
        for (Path fichier : new Path[] {ctx.fichierTournoiHomologueDemo(), ctx.fichierTournoiPublieDemo(),
                ctx.fichierTournoiJourJDemo()}) {
            assertTrue(ctx.estTournoiDeDemo(fichier), fichier.toString());
            Acces acces = ctx.accesPreparationJourJ(ctx.chargerTournoi(fichier), fichier);
            assertTrue(acces.proposee(), fichier.toString());
            assertTrue(acces.ouverte(), fichier.toString());
        }
    }

    @Test
    void brouillon_et_demande_de_demo_n_ont_pas_l_etape() {
        ContexteApplication ctx = new ContexteApplication(dossier, DonneesDemo.clubs(), DonneesDemo.licences());
        for (Path fichier : new Path[] {ctx.fichierTournoiDemo(), ctx.fichierTournoiDemandeDemo()}) {
            Acces acces = ctx.accesPreparationJourJ(ctx.chargerTournoi(fichier), fichier);
            assertFalse(acces.proposee(), fichier.toString());
            assertFalse(acces.ouverte(), fichier.toString());
        }
    }

    @Test
    void un_autre_fichier_n_est_pas_un_tournoi_de_demo_et_suit_la_regle_j_moins_7() {
        ContexteApplication ctx = new ContexteApplication(dossier, DonneesDemo.clubs(), DonneesDemo.licences());
        Path autre = dossier.resolve("tournois").resolve("autre.pingmanager");
        assertFalse(ctx.estTournoiDeDemo(autre));
        assertFalse(ctx.estTournoiDeDemo(null));
        // Le même tournoi (Brunoy, avril 2027), vu comme un vrai tournoi : fermé loin de la date.
        Tournoi publie = ctx.chargerTournoi(ctx.fichierTournoiPublieDemo());
        Acces acces = ctx.accesPreparationJourJ(publie, autre);
        assertTrue(acces.proposee());
        assertEquals(acces.joursAvantPremierJour() <= 7, acces.ouverte());
    }
}
