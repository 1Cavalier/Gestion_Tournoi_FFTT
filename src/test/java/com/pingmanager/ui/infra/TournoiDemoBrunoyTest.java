package com.pingmanager.ui.infra;

import com.pingmanager.core.RegistreDisciplinaire;
import com.pingmanager.core.ServiceCycleTournoi;
import com.pingmanager.model.DocumentReglement;
import com.pingmanager.core.reglement.GenerateurReglement;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.persistence.TournoiMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Le tournoi de Brunoy reprend le règlement validé par le club (26 octobre 2026). */
class TournoiDemoBrunoyTest {

    private static String texte(DocumentReglement d) {
        StringBuilder sb = new StringBuilder();
        for (DocumentReglement.Article a : d.articles()) {
            sb.append(a.titre()).append('\n');
            a.blocs().forEach(b -> sb.append(b.texteBrut()).append('\n'));
        }
        return sb.toString();
    }

    private static DocumentReglement.Grille grille(DocumentReglement d, String article) {
        return (DocumentReglement.Grille) d.articles().stream().filter(a -> a.titre().equals(article))
                .findFirst().orElseThrow().blocs().stream()
                .filter(b -> b instanceof DocumentReglement.Grille).findFirst().orElseThrow();
    }

    @Test
    void dates_lieu_contact_et_materiel() {
        Tournoi t = TournoiDemoBrunoy.construire();
        DocumentReglement d = new GenerateurReglement().generer(t);
        String txt = texte(d);
        assertEquals("Samedi 10 et dimanche 11 avril 2027, Brunoy", d.sousTitre());
        assertTrue(txt.contains("organise les samedi 10 et dimanche 11 avril 2027 un tournoi national B intitulé "
                + "« Tournoi National B de Brunoy 2027 », au 157 Route de Brie, 91800 Brunoy. Le tournoi est doté de "
                + "3 390 € en espèces."), txt);
        assertTrue(txt.contains("du dimanche 17 janvier 2027 à 10h au vendredi 9 avril 2027 à 19h"), txt);
        assertTrue(txt.contains("Les poussins (garçons et filles) ne sont pas admis."), txt);
        assertTrue(txt.contains("Le tournoi se déroule sur 20 tables, conformes aux normes"), txt);
        assertTrue(txt.contains("Une salle d'échauffement de 8 tables est à la disposition des joueurs."), txt);
        assertTrue(txt.contains("Les balles, en plastique blanches de marque Donic P40+ 3 étoiles, agréées, "
                + "sont fournies par le club."), txt);
        assertTrue(txt.contains("Les portes de la salle s'ouvrent dès 7h chaque jour du tournoi : venez tôt pour "
                + "vous changer, vous échauffer et aborder le pointage sereinement. Accompagnateurs et spectateurs "
                + "sont les bienvenus pour encourager les joueurs !"), txt);
        assertTrue(txt.contains("Pour tout renseignement : Serge BOULIER, sboulier@gmail.com."), txt);
        assertTrue(txt.contains("Que le forfait intervienne pendant la période d'inscription, après la clôture des "
                + "inscriptions ou le jour du tournoi, aucun remboursement n'est effectué : toute inscription est "
                + "définitive et n'ouvre droit à aucun remboursement."), txt);
        assertTrue(txt.contains("dès 7 h 30 le matin et jusqu'à 23 h, sans interruption le midi. Boissons"), txt);
    }

    @Test
    void juges_arbitres_loobuyck_principal_et_priou_adjoint() {
        Tournoi t = TournoiDemoBrunoy.construire();
        assertEquals(2, t.getJugesArbitres().size());
        DocumentReglement d = new GenerateurReglement().generer(t);
        assertTrue(texte(d).contains("Le tournoi est placé sous l'autorité de Samuel LOOBUYCK, juge-arbitre de grade "
                + "JA3, assisté de William PRIOU (JA2)."), texte(d));
        // Seul le numéro d'homologation reste à compléter.
        List<String> trous = d.aCompleter().stream().filter(x -> x.startsWith("Compléter")).collect(Collectors.toList());
        assertEquals(List.of("Compléter : numéro d'homologation, délivré après la demande"), trous);
        // Avec un JA3 principal, la demande d'homologation est possible.
        assertTrue(new ServiceCycleTournoi().demanderHomologation(t).estEffectuee(),
                new ServiceCycleTournoi().elementsManquantsPourHomologation(t).toString());
    }

    @Test
    void treize_tableaux_horaires_ecarts_1h_puis_1h30() {
        Tournoi t = TournoiDemoBrunoy.construire();
        assertEquals(13, t.getTableaux().size());
        DocumentReglement d = new GenerateurReglement().generer(t);
        DocumentReglement.Grille g = grille(d, "Tableaux");
        assertEquals(List.of("Tableau", "Jour", "Fin du pointage", "Début", "Points", "Joueurs admis", "Places",
                "Tarif d'inscription*"), g.entetes());
        // Tarif en ligne : 10 € pour F (≤ 2000), K (≤ 1900) et M (Non numérotés), 9 € ailleurs.
        assertEquals(List.of("9 €", "9 €", "9 €", "9 €", "9 €", "10 €", "9 €", "9 €", "9 €", "9 €", "10 €", "9 €", "10 €"),
                g.lignes().stream().map(l -> l.get(7)).toList());
        assertTrue(g.note().startsWith("* Tarif de l'inscription en ligne ; voir les modalités à l'article 8 « Droits "
                + "d'engagement »."), g.note());
        assertTrue(texte(d).contains("L'inscription coûte 9 € en ligne et 10 € sur place par tableau ; pour les tableaux "
                + "F, K et M – Non numérotés, elle coûte 10 €, en ligne comme sur place."), texte(d));
        assertEquals(List.of(
                        "A|Samedi 10 avril|8h|8h30", "B|Samedi 10 avril|9h|9h30",
                        "C|Samedi 10 avril|10h30|11h", "D|Samedi 10 avril|11h30|12h",
                        "E|Samedi 10 avril|13h|13h30", "F|Samedi 10 avril|14h|14h30",
                        "G – Toutes séries dames|Samedi 10 avril|15h30|16h",
                        "H|Dimanche 11 avril|8h|8h30", "I|Dimanche 11 avril|9h|9h30",
                        "J|Dimanche 11 avril|10h30|11h", "K|Dimanche 11 avril|11h30|12h",
                        "L|Dimanche 11 avril|13h|13h30", "M – Non numérotés|Dimanche 11 avril|14h|14h30"),
                g.lignes().stream().map(l -> String.join("|", l.subList(0, 4))).toList());
        assertEquals(List.of("A", "Samedi 10 avril", "8h", "8h30", "≤ 600 pts", "Tous", "60", "9 €"), g.lignes().get(0));
        assertEquals(List.of("G – Toutes séries dames", "Samedi 10 avril", "15h30", "16h", "Toutes séries", "Dames", "48",
                "9 €"), g.lignes().get(6));
        assertEquals(List.of("M – Non numérotés", "Dimanche 11 avril", "14h", "14h30", "Toutes séries (hors n° 1 à 999)",
                "Tous", "60", "10 €"), g.lignes().get(12));
        Tableau dames = t.getTableaux().stream()
                .filter(x -> x.getConfiguration().getSexeRequis() == Sexe.FEMME).findFirst().orElseThrow();
        assertEquals(LocalTime.of(16, 0), dames.getHoraires().getDebutMatchs().toLocalTime());
    }

    @Test
    void dotations_3390_euros() {
        Tournoi t = TournoiDemoBrunoy.construire();
        assertEquals(3390, new ServiceCycleTournoi().calculerDotationGlobale(t));
        DocumentReglement d = new GenerateurReglement().generer(t);
        assertTrue(texte(d).contains("Le tournoi est doté de 3 390 € en espèces (IX.108)."), texte(d));
        DocumentReglement.Grille g = grille(d, "Dotations");
        List<String> attendu = List.of(
                "A|60 €|30 €|15 €|10 €|160 €", "H|70 €|35 €|20 €|10 €|185 €",
                "C|70 €|35 €|20 €|10 €|185 €", "J|80 €|40 €|20 €|10 €|200 €",
                "E|90 €|45 €|25 €|15 €|245 €", "L|100 €|50 €|25 €|15 €|260 €",
                "B|110 €|55 €|30 €|15 €|285 €", "I|120 €|60 €|30 €|15 €|300 €",
                "D|130 €|65 €|35 €|20 €|345 €", "K|140 €|70 €|35 €|20 €|360 €",
                "F|150 €|75 €|40 €|20 €|385 €", "M – Non numérotés|200 €|100 €|50 €|20 €|480 €",
                "G – Toutes séries dames|50 % des inscr.|30 % des inscr.|10 % des inscr.|—|100 % des inscriptions");
        // Colonne « Points » (index 1) retirée pour la comparaison.
        assertEquals(attendu, g.lignes().stream().map(l -> {
            List<String> sans = new java.util.ArrayList<>(l);
            sans.remove(1);
            return String.join("|", sans);
        }).toList());
        List<DocumentReglement.Bloc> dot = d.articles().stream().filter(a -> a.titre().equals("Dotations"))
                .findFirst().orElseThrow().blocs();
        assertEquals("Pour le tableau « G – Toutes séries dames », les pourcentages portent sur le total des droits "
                + "d'engagement perçus sur ce tableau : la dotation suit le nombre d'inscrits.", g.note());
        assertEquals("Si un tableau compte moins de 50 % de ses places occupées au début de la compétition, sa "
                + "dotation en espèces peut être réduite.", dot.get(2).texteBrut());
    }

    @Test
    void regles_d_inscription_et_paiement() {
        String txt = texte(new GenerateurReglement().generer(TournoiDemoBrunoy.construire()));
        assertTrue(txt.contains("de la 2e phase 2026-2027"));
        assertTrue(txt.contains("Nombre de tableaux par joueur\nChaque joueur et chaque joueuse peut s'inscrire à 2 tableaux "
                + "maximum par jour. Le samedi 10 avril, toute joueuse qui s'inscrit doit aussi participer au tableau "
                + "« G – Toutes séries dames » : ce tableau imposé s'ajoute à ses 2 tableaux au choix, soit 3 tableaux au "
                + "maximum.\n"
                + "Un joueur ou une joueuse ne peut pas s'inscrire à deux tableaux qui se suivent dans la journée.\n"
                + "Déroulement"), txt);
        assertTrue(txt.contains("9 € en ligne et 10 € sur place"));
        assertTrue(txt.contains("en ligne sur https://www.helloasso.com/"), txt);
        assertTrue(txt.contains("Le paiement se fait en ligne via HelloAsso, au moment de l'inscription."), txt);
        assertTrue(txt.contains("Remise des récompenses"));
    }

    @Test
    void tableau_dames_en_option_et_non_impose() {
        Tournoi t = TournoiDemoBrunoy.construire();
        t.setQuotasTableaux(t.getQuotasTableaux().avecTableauBonusObligatoire(false));
        String txt = texte(new GenerateurReglement().generer(t));
        assertTrue(txt.contains("Chaque joueur et chaque joueuse peut s'inscrire à 2 tableaux maximum par jour. "
                + "Le samedi 10 avril, les joueuses inscrites au tableau « G – Toutes séries dames » peuvent s'inscrire à "
                + "un troisième tableau de leur choix."), txt);
    }

    @Test
    void relu_a_l_identique_apres_enregistrement() {
        Tournoi t = TournoiDemoBrunoy.construire();
        Tournoi r = TournoiMapper.lire(TournoiMapper.ecrireDocument(t, new RegistreDisciplinaire())).getTournoi();
        assertEquals(texte(new GenerateurReglement().generer(t)), texte(new GenerateurReglement().generer(r)));
    }
}
