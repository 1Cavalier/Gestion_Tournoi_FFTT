package com.pingmanager.core;

import com.pingmanager.core.OrdonnancementTableaux.Creneau;
import com.pingmanager.model.HorairesTableau;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.ui.infra.TournoiDemoBrunoy;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrdonnancementTableauxTest {

    private static final LocalDate SAM = LocalDate.of(2027, 4, 17);
    private static final LocalDate DIM = LocalDate.of(2027, 4, 18);

    private static List<String> noms(List<Creneau> l, LocalDate jour) {
        return l.stream().filter(c -> c.jour().equals(jour)).map(c -> c.tableau().getNom()).collect(Collectors.toList());
    }

    @Test
    void ordre_de_la_reunion_un_sur_deux_alterne_dames_en_dernier_ecarts_1h30_puis_2h() {
        Tournoi t = TournoiDemoBrunoy.construire();
        List<Creneau> c = OrdonnancementTableaux.proposer(t.getTableaux(), List.of(SAM, DIM), LocalTime.of(8, 30),
                List.of(Duration.ofMinutes(90), Duration.ofMinutes(120)), true);
        assertEquals(List.of("A", "B", "C", "D", "E", "F",
                "G – Toutes séries dames"), noms(c, SAM));
        assertEquals(List.of("H", "I", "J", "K", "L", "M – Non numérotés"), noms(c, DIM));
        // A → 1 h 30 → B → 2 h → C → 1 h 30 → D …
        List<LocalTime> samedi = c.stream().filter(x -> x.jour().equals(SAM)).map(Creneau::debut)
                .collect(Collectors.toList());
        assertEquals(List.of(LocalTime.of(8, 30), LocalTime.of(10, 0), LocalTime.of(12, 0), LocalTime.of(13, 30),
                LocalTime.of(15, 30), LocalTime.of(17, 0), LocalTime.of(19, 0)), samedi);
    }

    @Test
    void sans_option_les_dames_restent_dans_l_alternance() {
        Tournoi t = TournoiDemoBrunoy.construire();
        List<Creneau> c = OrdonnancementTableaux.proposer(t.getTableaux(), List.of(SAM, DIM),
                LocalTime.of(8, 30), Duration.ofMinutes(60));
        // Samedi trié : 600 900 1200 1500 1800 2000 TS dames → faibles 600 900 1200 1500 / moyens 1800 2000 TSD
        assertEquals(List.of("A", "D", "C", "F", "E", "G – Toutes séries dames",
                "B"), noms(c, SAM));
        assertEquals(LocalTime.of(14, 30), c.get(6).debut());
    }

    @Test
    void application_des_horaires_et_du_pointage() {
        Tableau a = TournoiDemoBrunoy.construire().getTableaux().get(0);
        a.setHoraires(new HorairesTableau(null, null, SAM.atTime(10, 0), SAM.atTime(13, 0)));
        OrdonnancementTableaux.appliquer(List.of(new Creneau(a, DIM, LocalTime.of(9, 0))),
                Duration.ofMinutes(30), Duration.ofMinutes(60));
        assertEquals(DIM.atTime(9, 0), a.getHoraires().getDebutMatchs());
        assertEquals(DIM.atTime(8, 30), a.getHoraires().getFinPointage());
        assertEquals(DIM.atTime(8, 0), a.getHoraires().getDebutPointage());
        assertEquals(DIM.atTime(12, 0), a.getHoraires().getFinTheorique(), "durée de 3 h conservée");
    }

    @Test
    void sans_jours_ou_journee_qui_deborde_refuse() {
        List<Tableau> tabs = TournoiDemoBrunoy.construire().getTableaux();
        assertThrows(IllegalArgumentException.class,
                () -> OrdonnancementTableaux.proposer(tabs, List.of(), LocalTime.of(8, 0), Duration.ofMinutes(60)));
        assertThrows(IllegalArgumentException.class,
                () -> OrdonnancementTableaux.proposer(tabs, List.of(SAM), LocalTime.of(20, 0), Duration.ofMinutes(60)));
    }

    @Test
    void alternance_avec_un_nombre_impair() {
        Tournoi t = TournoiDemoBrunoy.construire();
        List<Tableau> trois = t.getTableaux().subList(0, 3); // 600, 800, 900
        List<Creneau> c = OrdonnancementTableaux.proposer(trois, List.of(SAM), LocalTime.of(9, 0), Duration.ofMinutes(45));
        assertEquals(List.of("A", "C", "H"), noms(c, SAM));
        assertEquals(LocalTime.of(10, 30), c.get(2).debut());
    }
}
