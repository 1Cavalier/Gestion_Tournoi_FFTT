package com.pingmanager.core;

import com.pingmanager.model.Joueur;
import com.pingmanager.model.Match;
import com.pingmanager.model.Sexe;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HistoriqueAnnoncesTest {

    private static final Instant T0 = Instant.parse("2026-11-14T09:00:00Z");
    private final Match match = new Match(new Joueur("1", "MARTIN", "Lucas", Sexe.HOMME, "TT Brunoy", 1200),
            new Joueur("2", "DURAND", "Paul", Sexe.HOMME, "AS Melun", 1100));

    private ResultatAnnonceVocale resultat(int table, boolean dite) {
        AnnonceVocale a = new AnnonceVocale(match, table, List.of(SegmentAnnonce.libre("Table " + table)), false);
        return new ResultatAnnonceVocale(a, dite, dite ? "" : "voix indisponible");
    }

    @Test
    void historique_vide_au_depart() {
        assertTrue(new HistoriqueAnnonces().dernieres().isEmpty());
    }

    @Test
    void la_plus_recente_annonce_vient_en_premier_avec_son_heure_et_son_issue() {
        HistoriqueAnnonces h = new HistoriqueAnnonces();
        h.noter(resultat(1, true), T0);
        h.noter(resultat(2, false), T0.plusSeconds(60));
        List<HistoriqueAnnonces.Entree> d = h.dernieres();
        assertEquals(2, d.size());
        assertEquals(2, d.get(0).annonce().table());
        assertFalse(d.get(0).dite());
        assertEquals(T0.plusSeconds(60), d.get(0).instant());
        assertEquals(1, d.get(1).annonce().table());
        assertTrue(d.get(1).dite());
    }

    @Test
    void au_dela_de_la_capacite_les_plus_anciennes_sortent() {
        HistoriqueAnnonces h = new HistoriqueAnnonces();
        for (int i = 1; i <= HistoriqueAnnonces.CAPACITE + 3; i++) {
            h.noter(resultat(i, true), T0.plusSeconds(i));
        }
        List<HistoriqueAnnonces.Entree> d = h.dernieres();
        assertEquals(HistoriqueAnnonces.CAPACITE, d.size());
        assertEquals(HistoriqueAnnonces.CAPACITE + 3, d.get(0).annonce().table());
        assertEquals(4, d.get(d.size() - 1).annonce().table());
    }

    @Test
    void la_liste_rendue_est_une_copie() {
        HistoriqueAnnonces h = new HistoriqueAnnonces();
        h.noter(resultat(1, true), T0);
        h.dernieres().clear();
        assertEquals(1, h.dernieres().size());
    }
}
