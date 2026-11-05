package com.pingmanager.core.reglement;

import com.pingmanager.model.AffectationJugeArbitre;
import com.pingmanager.model.DocumentReglement;
import com.pingmanager.model.GradeJugeArbitre;
import com.pingmanager.model.OptionsReglement;
import com.pingmanager.model.Paiement;
import com.pingmanager.model.Tournoi;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Règlement et feuille figés : aucune voie de contournement, et copies indépendantes des données vivantes. */
class ReglementFigeContournementTest {

    private final ServiceReglementTournoi service = new ServiceReglementTournoi();

    @Test
    void les_copies_figees_sont_immuables_en_profondeur() {
        Tournoi t = TournoisReglementDeTest.tournoiPublie();
        DocumentReglement r = t.getReglementFige();
        assertThrows(UnsupportedOperationException.class, () -> r.articles().clear());
        assertThrows(UnsupportedOperationException.class, () -> r.aCompleter().add("x"));
        assertThrows(UnsupportedOperationException.class, () -> r.articles().get(0).blocs().clear());
        assertThrows(UnsupportedOperationException.class,
                () -> t.getFeuilleEngagementFigee().articles().clear());
    }

    @Test
    void les_options_ne_sont_pas_modifiables_en_place() {
        Tournoi t = TournoisReglementDeTest.tournoiPublie();
        assertThrows(UnsupportedOperationException.class, () -> t.getOptionsReglement().getArticles().clear());
    }

    @Test
    void figer_sur_un_tournoi_deja_publie_ne_remplace_pas_les_copies() {
        Tournoi t = TournoisReglementDeTest.tournoiPublie();
        DocumentReglement r = t.getReglementFige();
        DocumentReglement f = t.getFeuilleEngagementFigee();
        t.getTableaux().get(0).getHoraires().setDebutMatchs(LocalDateTime.of(2027, 3, 13, 15, 0));
        service.figer(t);
        assertSame(r, t.getReglementFige());
        assertSame(f, t.getFeuilleEngagementFigee());
    }

    @Test
    void remplacer_la_copie_figee_par_une_autre_est_refuse() {
        Tournoi t = TournoisReglementDeTest.tournoiPublie();
        DocumentReglement autre = new DocumentReglement("Autre", "", List.of(), List.of());
        assertThrows(IllegalStateException.class, () -> t.setReglementFige(autre));
        assertThrows(IllegalStateException.class, () -> t.setFeuilleEngagementFigee(autre));
    }

    @Test
    void nom_ja_paiement_dotation_tableau_modifies_apres_publication_ne_changent_pas_les_copies() {
        Tournoi t = TournoisReglementDeTest.tournoiPublie();
        DocumentReglement r = t.getReglementFige();
        DocumentReglement f = t.getFeuilleEngagementFigee();

        t.setNom("Autre nom");
        t.ajouterJugeArbitre(new AffectationJugeArbitre("Martin", "Luc", "luc@x.fr", "7654321",
                GradeJugeArbitre.JA2));
        t.setPaiement(new Paiement(Paiement.Mode.values()[Paiement.Mode.values().length - 1],
                "https://exemple.fr/paiement"));
        t.setDotationGlobale(9999);
        t.getTableaux().get(1).getHoraires().setDebutMatchs(LocalDateTime.of(2027, 3, 13, 17, 30));

        assertEquals(r, service.reglement(t));
        assertEquals(f, service.feuilleEngagement(t));
        assertSame(r, t.getReglementFige());
        assertSame(f, t.getFeuilleEngagementFigee());
    }

    @Test
    void modifierOptions_avec_valeur_nulle_apres_publication_est_refuse_sans_exception() {
        Tournoi t = TournoisReglementDeTest.tournoiPublie();
        OptionsReglement avant = t.getOptionsReglement();
        ResultatModificationReglement r = service.modifierOptions(t, null);
        assertEquals(ResultatModificationReglement.Motif.TOURNOI_PUBLIE, r.getMotif());
        assertSame(avant, t.getOptionsReglement());
        assertEquals(ResultatModificationReglement.Motif.TOURNOI_PUBLIE, service.deposer(t, null).getMotif());
    }

    @Test
    void deposer_null_avant_publication_retire_le_fichier() {
        Tournoi t = TournoisReglementDeTest.tournoiPubliable();
        service.deposer(t, new com.pingmanager.model.DocumentReference("R", "/r.pdf"));
        service.deposer(t, null);
        assertEquals(null, t.getReglementDocument());
    }
}
