package com.pingmanager.core;

import com.pingmanager.model.Carton;
import com.pingmanager.model.CouleurCarton;
import com.pingmanager.model.IssueMatch;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.LigneClassement;
import com.pingmanager.model.Manche;
import com.pingmanager.model.Match;
import com.pingmanager.model.Poule;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.StatutMatch;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DisciplineEtForfaitTest {

    private Joueur j(String lic) {
        return new Joueur(lic, "N" + lic, "P" + lic, Sexe.HOMME, "Cl" + lic, 1000);
    }

    // --- I.303 : 0 point pour forfait / abandon / disqualification ---

    @Test
    void forfait_donne_zero_point_au_fautif() {
        Joueur x = j("X"), y = j("Y"), z = j("Z");
        Poule p = new Poule("A", List.of(x, y, z));
        Match m1 = new Match(x, y);
        m1.definirScore(List.of(new Manche(11, 5), new Manche(11, 5), new Manche(11, 5)));
        Match m2 = new Match(x, z);
        m2.definirScore(List.of(new Manche(11, 5), new Manche(11, 5), new Manche(11, 5)));
        Match m3 = new Match(y, z);
        m3.declarerIssue(IssueMatch.FORFAIT, z);
        p.ajouterMatch(m1);
        p.ajouterMatch(m2);
        p.ajouterMatch(m3);

        List<LigneClassement> c = new ClassementPouleService().calculer(p);
        LigneClassement lz = c.stream()
                .filter(l -> l.getJoueur().getNumeroLicence().equals("Z"))
                .findFirst().orElseThrow();
        // Z : défaite jouée vs X (1 pt) + forfait vs Y (0 pt) = 1 pt.
        assertEquals(1, lz.getPointsClassement());
        assertEquals(1, lz.getMatchsPerdusZeroPoint());
    }

    @Test
    void le_vainqueur_dun_forfait_est_ladversaire() {
        Joueur a = j("A"), b = j("B");
        Match m = new Match(a, b);
        m.declarerIssue(IssueMatch.FORFAIT, b);
        assertEquals(a, m.getVainqueur());
        assertEquals(IssueMatch.FORFAIT, m.getIssue());
        assertEquals(StatutMatch.TERMINE, m.getStatut());
    }

    @Test
    void abandon_et_disqualification_font_perdre_le_fautif() {
        Joueur a = j("A"), b = j("B");
        Match ab = new Match(a, b);
        ab.declarerIssue(IssueMatch.ABANDON, b);
        assertEquals(a, ab.getVainqueur());

        Match dq = new Match(a, b);
        dq.declarerIssue(IssueMatch.DISQUALIFICATION, a);
        assertEquals(b, dq.getVainqueur());
    }

    // --- 3.5.2 : progression des cartons in-match ---

    @Test
    void progression_des_cartons_jaune_puis_un_puis_deux_points() {
        RegistreDisciplinaire reg = new RegistreDisciplinaire();
        GestionCartonsMatch gc = new GestionCartonsMatch(reg);
        Joueur x = j("X"), y = j("Y");
        Match m = new Match(x, y);

        Carton c1 = gc.appliquerFauteEnMatch(m, x, "1re faute");
        assertEquals(CouleurCarton.JAUNE, c1.getCouleur());
        assertEquals(0, c1.getPointsPenalite());

        Carton c2 = gc.appliquerFauteEnMatch(m, x, "récidive");
        assertEquals(CouleurCarton.JAUNE_ROUGE, c2.getCouleur());
        assertEquals(1, c2.getPointsPenalite());

        Carton c3 = gc.appliquerFauteEnMatch(m, x, "encore");
        assertEquals(2, c3.getPointsPenalite());

        // Points cumulés offerts à Y : 0 + 1 + 2 = 3.
        assertEquals(3, m.getPointsPenaliteAdverseDeA());
    }

    @Test
    void le_compteur_de_cartons_est_independant_par_joueur() {
        RegistreDisciplinaire reg = new RegistreDisciplinaire();
        GestionCartonsMatch gc = new GestionCartonsMatch(reg);
        Joueur x = j("X"), y = j("Y");
        Match m = new Match(x, y);

        gc.appliquerFauteEnMatch(m, x, "faute X");
        gc.appliquerFauteEnMatch(m, x, "récidive X"); // X en est à +1
        Carton premierY = gc.appliquerFauteEnMatch(m, y, "faute Y");
        // La première faute de Y reste un simple jaune.
        assertEquals(CouleurCarton.JAUNE, premierY.getCouleur());
        assertEquals(0, premierY.getPointsPenalite());
    }

    // --- 3.5.2.9 : deux disqualifications = exclusion de la compétition ---

    @Test
    void deux_disqualifications_excluent_de_la_competition() {
        RegistreDisciplinaire reg = new RegistreDisciplinaire();
        Joueur z = j("Z");

        boolean exclu1 = reg.enregistrer(new Carton(z, CouleurCarton.ROUGE, 0, false, "dq1", LocalDateTime.now()));
        assertFalse(exclu1);
        assertFalse(reg.estExcluDeLaCompetition(z));

        boolean exclu2 = reg.enregistrer(new Carton(z, CouleurCarton.ROUGE, 0, false, "dq2", LocalDateTime.now()));
        assertTrue(exclu2);
        assertTrue(reg.estExcluDeLaCompetition(z));
        assertEquals(2, reg.compterDisqualifications(z));
    }

    @Test
    void un_carton_hors_match_est_enregistre_et_marque_comme_tel() {
        RegistreDisciplinaire reg = new RegistreDisciplinaire();
        GestionCartonsMatch gc = new GestionCartonsMatch(reg);
        Joueur w = j("W");

        Carton hm = gc.appliquerCartonHorsMatch(w, CouleurCarton.JAUNE, "salle");
        assertTrue(hm.estHorsMatch());
        assertEquals(1, reg.compterCartons(w, CouleurCarton.JAUNE));

        // Un rouge hors match compte comme disqualification.
        gc.appliquerCartonHorsMatch(w, CouleurCarton.ROUGE, "grave");
        assertEquals(1, reg.compterDisqualifications(w));
    }
}
