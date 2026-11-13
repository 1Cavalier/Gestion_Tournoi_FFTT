package com.pingmanager.core;

import com.pingmanager.model.DateFederale;
import com.pingmanager.model.StatutDateFederale;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Lecture et validation des fichiers de calendrier fédéral (B045). */
class ChargeurCalendrierFederalTest {

    private static final String EN_TETE = ChargeurCalendrierFederal.EN_TETE + "\n";
    private static final String SOURCE = "https://www.fftt.com/site/competition/criterium-federal";

    private static String ligne(String id, String titre, String debut, String fin, String statut) {
        return id + ";Critérium fédéral;" + titre + ";" + debut + ";" + fin + ";" + statut + ";" + SOURCE
                + ";2026-11-13;\n";
    }

    @Test
    void fichier_valide_lu_trie_avec_commentaires_et_lignes_vides_ignores() {
        String texte = "# commentaire\n\n" + EN_TETE
                + ligne("t2", "2e tour", "2026-11-13", "2026-11-15", "a-verifier")
                + "# entre deux\n"
                + "t1;Critérium fédéral;1er tour;2026-10-10;;CONFIRMEE;" + SOURCE + ";2026-11-13;Note libre\n";
        ChargeurCalendrierFederal.Resultat r = ChargeurCalendrierFederal.lire(texte);
        assertTrue(r.estValide(), r.erreurs().toString());
        assertEquals(List.of("t1", "t2"), r.dates().stream().map(DateFederale::getId).toList());
        DateFederale t1 = r.dates().get(0);
        assertEquals(StatutDateFederale.CONFIRMEE, t1.getStatut());
        assertFalse(t1.estSurPlusieursJours());
        assertEquals("Note libre", t1.getNote());
        DateFederale t2 = r.dates().get(1);
        assertTrue(t2.estAVerifier());
        assertEquals(LocalDate.of(2026, 11, 15), t2.getDateFin());
        assertEquals(LocalDate.of(2026, 11, 13), t2.getReluLe());
        assertEquals(SOURCE, t2.getSource());
        assertNull(t2.getNote());
    }

    @Test
    void date_invalide_titre_vide_statut_inconnu_et_fin_avant_debut_refusent_tout_le_fichier() {
        String texte = EN_TETE
                + ligne("ok", "Bon", "2026-10-10", "", "confirmee")
                + ligne("a", "Date fausse", "2026-02-30", "", "confirmee")
                + ligne("b", " ", "2026-10-10", "", "confirmee")
                + ligne("c", "Statut", "2026-10-11", "", "peut-etre")
                + ligne("d", "Fin", "2026-10-12", "2026-10-11", "confirmee");
        ChargeurCalendrierFederal.Resultat r = ChargeurCalendrierFederal.lire(texte);
        assertFalse(r.estValide());
        assertTrue(r.dates().isEmpty(), "un calendrier à moitié lu n'est jamais affiché");
        assertEquals(4, r.erreurs().size(), r.erreurs().toString());
        assertTrue(r.erreurs().get(0).startsWith("Ligne 3 : date de début invalide"), r.erreurs().get(0));
        assertTrue(r.erreurs().get(1).contains("titre vide"));
        assertTrue(r.erreurs().get(2).contains("statut inconnu « peut-etre »"));
        assertTrue(r.erreurs().get(3).contains("fin avant le début"));
    }

    @Test
    void identifiant_en_double_et_doublon_meme_titre_meme_jour_refuses() {
        String texte = EN_TETE
                + ligne("x", "1er tour", "2026-10-10", "", "confirmee")
                + ligne("x", "Autre", "2026-10-11", "", "confirmee")
                + ligne("y", "1ER TOUR", "2026-10-10", "", "a-verifier");
        ChargeurCalendrierFederal.Resultat r = ChargeurCalendrierFederal.lire(texte);
        assertFalse(r.estValide());
        assertTrue(r.erreurs().get(0).contains("identifiant « x » en double"), r.erreurs().toString());
        assertTrue(r.erreurs().get(1).contains("doublon"), r.erreurs().toString());
    }

    @Test
    void en_tete_absent_ou_faux_colonnes_manquantes_source_et_relecture_obligatoires() {
        assertFalse(ChargeurCalendrierFederal.lire("").estValide());
        assertFalse(ChargeurCalendrierFederal.lire("# rien\n").estValide());
        assertTrue(ChargeurCalendrierFederal.lire("id;titre\n" + ligne("a", "A", "2026-10-10", "", "confirmee"))
                .erreurs().get(0).contains("en-tête attendu"));
        assertTrue(ChargeurCalendrierFederal.lire(EN_TETE + "a;b;c\n").erreurs().get(0)
                .contains("9 colonnes attendues, 3 trouvées"));
        String sansSource = EN_TETE + "a;Cat;Titre;2026-10-10;;confirmee; ;2026-11-13;\n";
        assertTrue(ChargeurCalendrierFederal.lire(sansSource).erreurs().get(0).contains("source vide"));
        String sansRelecture = EN_TETE + "a;Cat;Titre;2026-10-10;;confirmee;" + SOURCE + ";;\n";
        assertTrue(ChargeurCalendrierFederal.lire(sansRelecture).erreurs().get(0)
                .contains("date de relecture manquante"));
    }

    @Test
    void lecture_d_un_flux_utf8_avec_bom_et_fins_de_ligne_windows() throws Exception {
        String texte = "﻿" + EN_TETE.replace("\n", "\r\n")
                + ligne("é", "Critérium à Joué-lès-Tours", "2026-10-09", "2026-10-11", "a-verifier")
                .replace("\n", "\r\n");
        try (InputStream in = new ByteArrayInputStream(texte.getBytes(StandardCharsets.UTF_8))) {
            ChargeurCalendrierFederal.Resultat r = ChargeurCalendrierFederal.lire(in);
            assertTrue(r.estValide(), r.erreurs().toString());
            assertEquals("Critérium à Joué-lès-Tours", r.dates().get(0).getTitre());
        }
    }

    @Test
    void nom_de_ressource_par_saison_et_saisons_cherchees() {
        assertEquals("calendrier-federal-2026-2027.csv",
                ChargeurCalendrierFederal.nomRessource(LocalDate.of(2026, 11, 13)));
        assertEquals("calendrier-federal-2025-2026.csv",
                ChargeurCalendrierFederal.nomRessource(LocalDate.of(2026, 6, 1)));
        assertEquals(List.of("calendrier-federal-2026-2027.csv", "calendrier-federal-2027-2028.csv"),
                ChargeurCalendrierFederal.ressourcesAChercher(LocalDate.of(2026, 11, 13)));
    }

    @Test
    void fichier_livre_2026_2027_valide_et_sourcé() throws Exception {
        String chemin = ChargeurCalendrierFederal.DOSSIER_RESSOURCES + "calendrier-federal-2026-2027.csv";
        try (InputStream in = ChargeurCalendrierFederalTest.class.getResourceAsStream(chemin)) {
            assertNotNull(in, "ressource " + chemin);
            ChargeurCalendrierFederal.Resultat r = ChargeurCalendrierFederal.lire(in);
            assertTrue(r.estValide(), r.erreurs().toString());
            assertFalse(r.dates().isEmpty());
            for (DateFederale d : r.dates()) {
                assertTrue(d.getSource().startsWith("https://"), "source en adresse : " + d);
                assertEquals("2026-2027", com.pingmanager.core.reglement.Formats.saison(d.getDate()),
                        "date dans la saison du fichier : " + d);
            }
        }
    }
}
