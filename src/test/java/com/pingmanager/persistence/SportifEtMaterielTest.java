package com.pingmanager.persistence;

import com.pingmanager.model.AffectationArbitre;
import com.pingmanager.model.AffectationJugeArbitre;
import com.pingmanager.model.CouleurBalles;
import com.pingmanager.model.FicheLicence;
import com.pingmanager.model.GradeJugeArbitre;
import com.pingmanager.model.InfosReglement;
import com.pingmanager.model.LotTables;
import com.pingmanager.model.PolitiqueBalles;
import com.pingmanager.model.RoleJugeArbitre;
import com.pingmanager.model.Tournoi;
import com.pingmanager.persistence.json.Json;
import com.pingmanager.ui.infra.DonneesDemo;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Lot « informations sportives et techniques » : JA, arbitres, tables, balles, recherche. */
class SportifEtMaterielTest {

    @Test
    void grades_lus_depuis_les_fiches_et_anciens_fichiers() {
        assertEquals(GradeJugeArbitre.JA3, GradeJugeArbitre.depuisCode("ja 3").orElseThrow());
        assertEquals(GradeJugeArbitre.JA_NATIONAL, GradeJugeArbitre.depuisCode("JAN").orElseThrow());
        assertEquals(GradeJugeArbitre.JA_NATIONAL, GradeJugeArbitre.depuisCode("JA national").orElseThrow());
        assertEquals(GradeJugeArbitre.JA1, GradeJugeArbitre.depuisCode("JA_DEPARTEMENTAL").orElseThrow());
        assertEquals(GradeJugeArbitre.JA2, GradeJugeArbitre.depuisCode("JA_REGIONAL").orElseThrow());
        assertTrue(GradeJugeArbitre.depuisCode("AR").isEmpty());
        assertTrue(GradeJugeArbitre.JA_NATIONAL.couvre(GradeJugeArbitre.JA3));
        assertTrue(!GradeJugeArbitre.JA2.couvre(GradeJugeArbitre.JA3));
    }

    @Test
    void juge_arbitre_sans_email_avec_role_relu() {
        AffectationJugeArbitre a = new AffectationJugeArbitre("LEMOINE", "Hélène", null, "9411021",
                GradeJugeArbitre.JA3, null, RoleJugeArbitre.ADJOINT);
        AffectationJugeArbitre relu = ConfigMapper.lireJugeArbitre(ConfigMapper.ecrireJugeArbitre(a));
        assertNull(relu.getEmail());
        assertEquals(RoleJugeArbitre.ADJOINT, relu.getRole());
        assertEquals(GradeJugeArbitre.JA3, relu.getGrade());
    }

    @Test
    void ancien_fichier_ja_regional_sans_role() {
        Json o = Json.objet();
        o.set("nom", "Durand");
        o.set("prenom", "Paul");
        o.set("email", "p@x.fr");
        o.set("licence", "1");
        o.set("grade", "JA_REGIONAL");
        AffectationJugeArbitre relu = ConfigMapper.lireJugeArbitre(o);
        assertEquals(GradeJugeArbitre.JA2, relu.getGrade());
        assertEquals(RoleJugeArbitre.PRINCIPAL, relu.getRole());
    }

    @Test
    void arbitres_et_lots_de_tables() {
        Tournoi t = new Tournoi("Open", "Brunoy", LocalDate.of(2027, 3, 13));
        t.ajouterArbitre(new AffectationArbitre("9411056", "PETIT", "Antoine", "AR"));
        assertThrows(IllegalArgumentException.class,
                () -> t.ajouterArbitre(new AffectationArbitre("9411056", "PETIT", "Antoine", "AR")));

        t.setLotsTables(List.of(new LotTables(12, "Cornilleau", "Bleu"), new LotTables(4, "Butterfly", "Vert")));
        assertEquals(16, t.getNombreDeTables());

        LotTables relu = ConfigMapper.lireLotTables(ConfigMapper.ecrireLotTables(t.getLotsTables().get(1)));
        assertEquals(4, relu.getNombre());
        assertEquals("Butterfly", relu.getMarque());
        AffectationArbitre arb = ConfigMapper.lireArbitre(ConfigMapper.ecrireArbitre(t.getArbitres().get(0)));
        assertEquals("AR", arb.getGrade());
        assertEquals("Arbitre régional (AR)", AffectationArbitre.libelleGrade("ar"));
    }

    @Test
    void balles_couleur_et_liste_noire_relues() {
        InfosReglement r = InfosReglement.builder()
                .politiqueBalles(PolitiqueBalles.APPORTEES_PAR_JOUEURS)
                .couleurBalles(CouleurBalles.ORANGE)
                .ballesRefusees(List.of("Balles celluloïd", "Marque X"))
                .build();
        InfosReglement relu = ConfigMapper.lireInfosReglement(ConfigMapper.ecrireInfosReglement(r));
        assertEquals(CouleurBalles.ORANGE, relu.getCouleurBalles());
        assertEquals(List.of("Balles celluloïd", "Marque X"), relu.getBallesRefusees());
        assertTrue(InfosReglement.builder().build().getBallesRefusees().isEmpty());
    }

    @Test
    void recherche_de_licences_par_nom_prenom_ou_numero() {
        var registre = DonneesDemo.licences();
        List<FicheLicence> parNom = registre.rechercher("lemo", 10);
        assertEquals(1, parNom.size());
        assertEquals("9411021", parNom.get(0).getNumeroLicence());
        assertEquals(1, registre.rechercher("helene", 10).size(), "sans accent");
        assertEquals(1, registre.rechercher("roux sophie", 10).size(), "plusieurs mots");
        assertTrue(registre.rechercher("9411", 10).size() >= 3, "début de numéro");
        assertTrue(registre.rechercher("x", 10).isEmpty(), "au moins 2 caractères");
    }
}
