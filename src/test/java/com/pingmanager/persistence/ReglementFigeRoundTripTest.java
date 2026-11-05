package com.pingmanager.persistence;

import com.pingmanager.core.RegistreDisciplinaire;
import com.pingmanager.core.reglement.GenerateurReglement;
import com.pingmanager.core.reglement.ServiceReglementTournoi;
import com.pingmanager.core.reglement.TournoisReglementDeTest;
import com.pingmanager.model.DocumentReglement;
import com.pingmanager.model.DocumentReglement.Article;
import com.pingmanager.model.DocumentReglement.Grille;
import com.pingmanager.model.DocumentReglement.Liste;
import com.pingmanager.model.DocumentReglement.Paragraphe;
import com.pingmanager.model.StatutTournoi;
import com.pingmanager.model.Tournoi;
import com.pingmanager.persistence.json.Json;
import com.pingmanager.persistence.json.JsonParser;
import com.pingmanager.persistence.json.JsonWriter;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Copies figées du règlement et de la feuille d'engagement : aller-retour JSON et anciens fichiers. */
class ReglementFigeRoundTripTest {

    private static Json enJson(Tournoi t) {
        return JsonParser.analyser(JsonWriter.ecrire(TournoiMapper.ecrireDocument(t, new RegistreDisciplinaire())));
    }

    private static Tournoi relire(Json doc) {
        return TournoiMapper.lire(doc).getTournoi();
    }

    /** Tournoi publié par le cycle (donc avec ses copies figées). */
    private static Tournoi publie() {
        return TournoisReglementDeTest.tournoiPublie();
    }

    @Test
    void copies_figees_relues_a_l_identique() {
        Tournoi t = publie();
        Tournoi r = relire(enJson(t));
        assertEquals(StatutTournoi.PUBLIE, r.getStatut());
        assertEquals(t.getReglementFige(), r.getReglementFige());
        assertEquals(t.getFeuilleEngagementFigee(), r.getFeuilleEngagementFigee());
    }

    @Test
    void horaire_modifie_apres_publication_le_reglement_relu_reste_celui_publie() {
        Tournoi t = publie();
        DocumentReglement publie = t.getReglementFige();
        t.getTableaux().get(0).getHoraires().setDebutMatchs(LocalDateTime.of(2027, 3, 13, 14, 0));

        Tournoi r = relire(enJson(t));
        assertEquals(LocalDateTime.of(2027, 3, 13, 14, 0), r.getTableaux().get(0).getHoraires().getDebutMatchs());
        assertNotEquals(publie, new GenerateurReglement().generer(r));
        assertEquals(publie, new ServiceReglementTournoi().reglement(r));
    }

    @Test
    void relu_publie_les_options_du_reglement_restent_refusees() {
        Tournoi r = relire(enJson(publie()));
        assertFalse(r.reglementModifiable());
        assertThrows(IllegalStateException.class,
                () -> r.setOptionsReglement(com.pingmanager.model.OptionsReglement.parDefaut()));
    }

    @Test
    void ancien_fichier_publie_sans_copie_la_genere_a_la_relecture() {
        Json doc = enJson(publie());
        Json jt = doc.get("tournoi");
        jt.entrees().remove("reglementFige");
        jt.entrees().remove("feuilleEngagementFigee");

        Tournoi r = relire(doc);
        assertEquals(StatutTournoi.PUBLIE, r.getStatut());
        assertNotNull(r.getReglementFige(), "copie générée à la relecture");
        assertNotNull(r.getFeuilleEngagementFigee());
        // … et gardée au prochain enregistrement.
        Json reecrit = enJson(r).get("tournoi");
        assertTrue(reecrit.has("reglementFige"));
        assertTrue(reecrit.has("feuilleEngagementFigee"));
    }

    @Test
    void brouillon_sans_champ_fige() {
        Tournoi t = TournoisReglementDeTest.tournoiPubliable();
        Json jt = enJson(t).get("tournoi");
        assertFalse(jt.has("reglementFige"));
        assertFalse(jt.has("feuilleEngagementFigee"));
        Tournoi r = relire(enJson(t));
        assertNull(r.getReglementFige());
        assertNull(r.getFeuilleEngagementFigee());
    }

    @Test
    void tous_les_types_de_blocs_relus() {
        DocumentReglement d = new DocumentReglement("Titre", "13 mars 2027", List.of(
                new Article(1, "Organisation", List.of(
                        new Paragraphe("Texte « accentué » [à compléter : salle]"),
                        new Grille(List.of("Tableau", "Horaire"),
                                List.of(List.of("A", "9 h"), List.of("B", "14 h")), null),
                        new Grille(List.of("X"), List.of(), "note"),
                        new Liste(List.of("un", "deux"))))),
                List.of("Compléter la salle"));
        assertEquals(d, DocumentReglementMapper.lire(
                JsonParser.analyser(JsonWriter.ecrire(DocumentReglementMapper.ecrire(d)))));
        assertNull(DocumentReglementMapper.lire(Json.nul()));
    }
}
