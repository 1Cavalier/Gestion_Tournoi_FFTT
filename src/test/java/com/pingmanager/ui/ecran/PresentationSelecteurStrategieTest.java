package com.pingmanager.ui.ecran;

import com.pingmanager.core.DroitRefuseException;
import com.pingmanager.core.OperationProtegee;
import com.pingmanager.model.StrategieOrdonnancement;
import com.pingmanager.model.Tournoi;
import com.pingmanager.persistence.DepotTournoi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PresentationSelecteurStrategieTest {

    private static Tournoi tournoi() {
        return new Tournoi("Open", "Club", LocalDate.of(2027, 4, 10));
    }

    @Test
    void trois_options_dans_l_ordre_avec_nom_et_phrase_celle_par_defaut_marquee() {
        List<PresentationSelecteurStrategie.Option> options = PresentationSelecteurStrategie.options(tournoi());
        assertEquals(3, options.size());
        assertEquals(List.of(StrategieOrdonnancement.values()),
                options.stream().map(PresentationSelecteurStrategie.Option::strategie).toList());
        for (PresentationSelecteurStrategie.Option o : options) {
            assertEquals(o.strategie().getLibelle(), o.nom());
            assertFalse(o.phrase().isBlank());
            assertTrue(o.phrase().endsWith("."), "une phrase : " + o.phrase());
            assertEquals(1, o.phrase().split("\\. ").length, "une seule phrase : " + o.phrase());
        }
        List<PresentationSelecteurStrategie.Option> actives = options.stream()
                .filter(PresentationSelecteurStrategie.Option::active).toList();
        assertEquals(1, actives.size());
        assertEquals(StrategieOrdonnancement.PAR_DEFAUT, actives.get(0).strategie());
        assertEquals("Appliquée par défaut", actives.get(0).marque());
        assertEquals(2, options.stream().filter(o -> o.marque() == null).count());
    }

    @Test
    void choix_enregistre_aussitot_et_marque_stratégie_choisie() {
        Tournoi t = tournoi();
        AtomicInteger enregistrements = new AtomicInteger();
        PresentationSelecteurStrategie.ResultatChoixStrategie r = PresentationSelecteurStrategie.choisir(t,
                StrategieOrdonnancement.FAIRE_AVANCER_PETIT_A_PETIT, x -> enregistrements.incrementAndGet());
        assertTrue(r.enregistre());
        assertFalse(r.inchange());
        assertEquals(1, enregistrements.get());
        assertTrue(r.message().contains("Tout faire avancer petit à petit"));
        assertTrue(t.strategieOrdonnancementChoisie());
        PresentationSelecteurStrategie.Option active = PresentationSelecteurStrategie.options(t).stream()
                .filter(PresentationSelecteurStrategie.Option::active).findFirst().orElseThrow();
        assertEquals(StrategieOrdonnancement.FAIRE_AVANCER_PETIT_A_PETIT, active.strategie());
        assertEquals("Stratégie choisie", active.marque());
    }

    @Test
    void recliquer_la_stratégie_deja_choisie_n_enregistre_rien() {
        Tournoi t = tournoi();
        t.setStrategieOrdonnancement(StrategieOrdonnancement.AVANCER_TABLEAUX_COMMENCES);
        AtomicInteger enregistrements = new AtomicInteger();
        PresentationSelecteurStrategie.ResultatChoixStrategie r = PresentationSelecteurStrategie.choisir(t,
                StrategieOrdonnancement.AVANCER_TABLEAUX_COMMENCES, x -> enregistrements.incrementAndGet());
        assertTrue(r.enregistre());
        assertTrue(r.inchange());
        assertNull(r.message());
        assertEquals(0, enregistrements.get());
    }

    @Test
    void choisir_la_stratégie_par_defaut_sans_choix_prealable_l_enregistre_comme_choix() {
        Tournoi t = tournoi();
        AtomicInteger enregistrements = new AtomicInteger();
        PresentationSelecteurStrategie.ResultatChoixStrategie r = PresentationSelecteurStrategie.choisir(t,
                StrategieOrdonnancement.PAR_DEFAUT, x -> enregistrements.incrementAndGet());
        assertFalse(r.inchange());
        assertEquals(1, enregistrements.get());
        assertTrue(t.strategieOrdonnancementChoisie());
    }

    @Test
    void refus_d_enregistrer_remet_l_etat_precedent_meme_sans_choix() {
        Tournoi t = tournoi();
        PresentationSelecteurStrategie.Enregistreur refuse = x -> {
            throw new DroitRefuseException(DroitRefuseException.Motif.NON_CONNECTE,
                    OperationProtegee.values()[0], "0891");
        };
        PresentationSelecteurStrategie.ResultatChoixStrategie r = PresentationSelecteurStrategie.choisir(t,
                StrategieOrdonnancement.AVANCER_TABLEAUX_COMMENCES, refuse);
        assertFalse(r.enregistre());
        assertFalse(r.message().isBlank());
        assertFalse(t.strategieOrdonnancementChoisie(), "toujours aucun choix");

        t.setStrategieOrdonnancement(StrategieOrdonnancement.FAIRE_AVANCER_PETIT_A_PETIT);
        PresentationSelecteurStrategie.choisir(t, StrategieOrdonnancement.MINIMISER_ATTENTE, refuse);
        assertEquals(StrategieOrdonnancement.FAIRE_AVANCER_PETIT_A_PETIT, t.getStrategieOrdonnancement());
    }

    @Test
    void choix_enregistre_dans_le_fichier_puis_relu(@TempDir Path dossier) {
        Path fichier = dossier.resolve("tournoi.json");
        Tournoi t = tournoi();
        DepotTournoi.sauvegarder(t, fichier);
        PresentationSelecteurStrategie.Enregistreur versFichier = x -> DepotTournoi.sauvegarder(x, fichier);
        for (StrategieOrdonnancement s : StrategieOrdonnancement.values()) {
            assertTrue(PresentationSelecteurStrategie.choisir(t, s, versFichier).enregistre());
            Tournoi relu = DepotTournoi.charger(fichier).getTournoi();
            assertTrue(relu.strategieOrdonnancementChoisie());
            assertEquals(s, relu.getStrategieOrdonnancement());
            PresentationSelecteurStrategie.Option active = PresentationSelecteurStrategie.options(relu).stream()
                    .filter(PresentationSelecteurStrategie.Option::active).findFirst().orElseThrow();
            assertEquals(s, active.strategie());
            assertEquals("Stratégie choisie", active.marque());
        }
    }
}
