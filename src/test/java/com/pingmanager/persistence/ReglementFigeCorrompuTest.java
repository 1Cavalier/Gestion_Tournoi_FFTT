package com.pingmanager.persistence;

import com.pingmanager.core.RegistreDisciplinaire;
import com.pingmanager.core.reglement.TournoisReglementDeTest;
import com.pingmanager.model.StatutTournoi;
import com.pingmanager.model.Tournoi;
import com.pingmanager.persistence.json.Json;
import com.pingmanager.persistence.json.JsonParser;
import com.pingmanager.persistence.json.JsonWriter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** Copie figée corrompue ou partielle : la lecture du tournoi ne doit jamais échouer. */
class ReglementFigeCorrompuTest {

    private static Json doc() {
        return JsonParser.analyser(JsonWriter.ecrire(TournoiMapper.ecrireDocument(
                TournoisReglementDeTest.tournoiPublie(), new RegistreDisciplinaire())));
    }

    private static Tournoi relire(Json d) {
        return TournoiMapper.lire(d).getTournoi();
    }

    private static void verifie(Tournoi r) {
        assertEquals(StatutTournoi.PUBLIE, r.getStatut());
        assertNotNull(r.getReglementFige(), "copie régénérée ou partielle, jamais absente pour un publié généré");
        assertNotNull(r.getFeuilleEngagementFigee());
    }

    @Test
    void copie_figee_de_mauvais_type_chaine_ne_fait_pas_echouer_la_lecture() {
        Json d = doc();
        d.get("tournoi").set("reglementFige", "n'importe quoi");
        d.get("tournoi").set("feuilleEngagementFigee", 12);
        verifie(relire(d));
    }

    @Test
    void articles_de_mauvais_type_ne_font_pas_echouer_la_lecture() {
        Json d = doc();
        Json jt = d.get("tournoi");
        Json r = Json.objet();
        r.set("titre", "T");
        r.set("articles", "pas un tableau");
        jt.set("reglementFige", r);
        Json f = Json.objet();
        Json arts = Json.tableau();
        arts.add(Json.of("pas un objet"));
        f.set("articles", arts);
        jt.set("feuilleEngagementFigee", f);
        verifie(relire(d));
    }

    @Test
    void numero_d_article_illisible_ne_fait_pas_echouer_la_lecture() {
        Json d = doc();
        Json jt = d.get("tournoi");
        Json art = Json.objet();
        art.set("numero", "abc");
        art.set("titre", "A");
        art.set("blocs", Json.tableau());
        Json arts = Json.tableau();
        arts.add(art);
        Json r = Json.objet();
        r.set("articles", arts);
        jt.set("reglementFige", r);
        verifie(relire(d));
    }

    @Test
    void objet_vide_comme_copie_figee_relit_un_document_vide() {
        Json d = doc();
        d.get("tournoi").set("reglementFige", Json.objet());
        Tournoi r = relire(d);
        assertNotNull(r.getReglementFige());
        assertEquals(StatutTournoi.PUBLIE, r.getStatut());
    }

    @Test
    void ancien_fichier_publie_sans_copie_puis_relecture_double_est_stable() {
        Json d = doc();
        d.get("tournoi").entrees().remove("reglementFige");
        d.get("tournoi").entrees().remove("feuilleEngagementFigee");
        Tournoi a = relire(d);
        Json reecrit = JsonParser.analyser(JsonWriter.ecrire(TournoiMapper.ecrireDocument(a,
                new RegistreDisciplinaire())));
        Tournoi b = relire(reecrit);
        assertEquals(a.getReglementFige(), b.getReglementFige());
        assertEquals(a.getFeuilleEngagementFigee(), b.getFeuilleEngagementFigee());
    }
}
