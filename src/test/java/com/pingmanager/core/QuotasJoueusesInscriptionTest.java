package com.pingmanager.core;

import com.pingmanager.core.ResultatInscriptionTournoi.Motif;
import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.HorairesTableau;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.QuotasTableaux;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tableaux par jour : 2 pour les joueurs, 3 pour les joueuses seulement si l'un
 * d'eux est le tableau « Toutes séries dames » ; quotas comptés jour par jour.
 */
class QuotasJoueusesInscriptionTest {

    private static final LocalDate SAM = LocalDate.of(2027, 4, 17);
    private static final LocalDate DIM = LocalDate.of(2027, 4, 18);

    private final ServiceInscription service = ServiceInscription.sansContrainteSuccession();

    private static Tableau tableau(String nom, LocalDate jour, int heure, Sexe sexe) {
        ConfigurationTableau.Builder b = ConfigurationTableau.builder();
        if (sexe != null) {
            b.sexeRequis(sexe);
        }
        Tableau t = new Tableau(nom, b.build());
        t.setHoraires(new HorairesTableau(null, null, jour.atTime(heure, 0)));
        return t;
    }

    private static Joueur joueuse(String licence) {
        return new Joueur(licence, "N" + licence, "P" + licence, Sexe.FEMME, "Club", 1000);
    }

    private static Joueur joueur(String licence) {
        return new Joueur(licence, "N" + licence, "P" + licence, Sexe.HOMME, "Club", 1000);
    }

    private final Tableau a = tableau("A", DIM, 8, null);
    private final Tableau b = tableau("B", DIM, 10, null);
    private final Tableau c = tableau("C", DIM, 12, null);
    private final Tableau dames = tableau("Toutes séries dames", DIM, 14, Sexe.FEMME);
    private final Tableau samedi = tableau("S", SAM, 9, null);

    private Tournoi tournoi(String bonus) {
        Tournoi t = new Tournoi("Open", "Club", List.of(SAM, DIM));
        for (Tableau x : List.of(a, b, c, dames, samedi)) {
            t.ajouterTableau(x);
        }
        t.setQuotasTableaux(QuotasTableaux.identiques(2, 3).avecTableauBonusJoueuses(bonus));
        return t;
    }

    @Test
    void troisieme_tableau_refuse_sans_le_tableau_dames() {
        Tournoi t = tournoi("Toutes séries dames");
        Joueur f = joueuse("1");
        assertEquals(Motif.INSCRIT, service.inscrire(t, f, a).getMotif());
        assertEquals(Motif.INSCRIT, service.inscrire(t, f, b).getMotif());
        assertEquals(Motif.QUOTA_TABLEAUX_ATTEINT, service.inscrire(t, f, c).getMotif());
        // Le tableau dames, lui, ouvre droit au 3e.
        assertEquals(Motif.INSCRIT, service.inscrire(t, f, dames).getMotif());
    }

    @Test
    void troisieme_tableau_accepte_si_deja_inscrite_au_tableau_dames() {
        Tournoi t = tournoi("Toutes séries dames");
        Joueur f = joueuse("2");
        assertEquals(Motif.INSCRIT, service.inscrire(t, f, dames).getMotif());
        assertEquals(Motif.INSCRIT, service.inscrire(t, f, a).getMotif());
        assertEquals(Motif.INSCRIT, service.inscrire(t, f, b).getMotif());
        assertEquals(Motif.QUOTA_TABLEAUX_ATTEINT, service.inscrire(t, f, c).getMotif());
    }

    @Test
    void sans_condition_trois_tableaux_pour_les_joueuses() {
        Tournoi t = tournoi(null);
        Joueur f = joueuse("3");
        service.inscrire(t, f, a);
        service.inscrire(t, f, b);
        assertEquals(Motif.INSCRIT, service.inscrire(t, f, c).getMotif());
    }

    @Test
    void joueurs_limites_a_deux_et_quota_compte_par_jour() {
        Tournoi t = tournoi("Toutes séries dames");
        Joueur h = joueur("4");
        assertEquals(Motif.INSCRIT, service.inscrire(t, h, samedi).getMotif());
        assertEquals(Motif.INSCRIT, service.inscrire(t, h, a).getMotif());
        assertEquals(Motif.INSCRIT, service.inscrire(t, h, b).getMotif()); // le samedi ne compte pas
        assertEquals(Motif.QUOTA_TABLEAUX_ATTEINT, service.inscrire(t, h, c).getMotif());
    }

    @Test
    void limite_effective_du_modele() {
        QuotasTableaux q = QuotasTableaux.parJour(Map.of(SAM, new QuotasTableaux.Quota(2, 2),
                DIM, new QuotasTableaux.Quota(2, 3))).avecTableauBonusJoueuses("Dames");
        assertEquals(2, q.limite(DIM, Sexe.FEMME, false));
        assertEquals(3, q.limite(DIM, Sexe.FEMME, true));
        assertEquals(2, q.limite(SAM, Sexe.FEMME, true));
        assertEquals(2, q.limite(DIM, Sexe.HOMME, true));
        assertNull(QuotasTableaux.aucun().limite(DIM, Sexe.FEMME, false));
    }

    @Test
    void le_jour_du_tableau_dames_une_joueuse_doit_s_y_inscrire_d_abord() {
        Tournoi t = tournoi("Toutes séries dames");
        t.setQuotasTableaux(t.getQuotasTableaux().avecTableauBonusObligatoire(true));
        Joueur f = joueuse("5");
        ResultatInscriptionTournoi r = service.inscrire(t, f, a);
        assertEquals(Motif.TABLEAU_FEMININ_OBLIGATOIRE, r.getMotif());
        assertEquals(dames, r.getTableauEnConflit());
        assertEquals(Motif.INSCRIT, service.inscrire(t, f, dames).getMotif());
        assertEquals(Motif.INSCRIT, service.inscrire(t, f, a).getMotif());
        // Un autre jour, pas d'obligation ; et les joueurs ne sont pas concernés.
        assertEquals(Motif.INSCRIT, service.inscrire(t, joueuse("6"), samedi).getMotif());
        assertEquals(Motif.INSCRIT, service.inscrire(t, joueur("7"), a).getMotif());
    }

    @Test
    void deux_tableaux_qui_se_suivent_le_meme_jour_refuses() {
        Tournoi t = tournoi(null);
        t.setQuotasTableaux(QuotasTableaux.aucun().avecTableauxConsecutifsInterdits(true));
        Joueur h = joueur("8");
        assertEquals(Motif.INSCRIT, service.inscrire(t, h, a).getMotif());      // 8 h
        ResultatInscriptionTournoi r = service.inscrire(t, h, b);               // 10 h : suit A
        assertEquals(Motif.TABLEAUX_SUCCESSIFS, r.getMotif());
        assertEquals(a, r.getTableauEnConflit());
        assertEquals(Motif.INSCRIT, service.inscrire(t, h, c).getMotif());      // 12 h : ne suit pas A
        assertEquals(Motif.INSCRIT, service.inscrire(t, h, samedi).getMotif()); // autre jour
    }
}
