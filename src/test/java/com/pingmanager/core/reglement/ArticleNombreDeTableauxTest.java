package com.pingmanager.core.reglement;

import com.pingmanager.model.QuotasTableaux;
import com.pingmanager.model.QuotasTableaux.Quota;
import com.pingmanager.model.Tournoi;
import com.pingmanager.ui.infra.TournoiDemoBrunoy;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Article « Nombre de tableaux par joueur » : le texte suit les options choisies
 * (tableau dames imposé ou en option, sans condition, tableaux consécutifs…).
 * Le tableau « G – Toutes séries dames » du tournoi de démo se joue le samedi.
 */
class ArticleNombreDeTableauxTest {

    private static final LocalDate SAM = LocalDate.of(2027, 4, 10);
    private static final LocalDate DIM = LocalDate.of(2027, 4, 11);
    private static final String DAMES = TournoiDemoBrunoy.DAMES;

    private final Tournoi t = TournoiDemoBrunoy.construire();
    private final GenerateurReglement g = new GenerateurReglement();

    private QuotasTableaux parJour(int samJoueuses, int dimJoueuses) {
        return QuotasTableaux.parJour(Map.of(SAM, new Quota(2, samJoueuses), DIM, new Quota(2, dimJoueuses)));
    }

    private List<String> texte(QuotasTableaux q) {
        return g.apercuNombreDeTableaux(t, q);
    }

    @Test
    void dames_impose_et_en_plus_des_deux_au_choix() {
        assertEquals(List.of("Chaque joueur et chaque joueuse peut s'inscrire à 2 tableaux maximum par jour. "
                        + "Le samedi 10 avril, toute joueuse qui s'inscrit doit aussi participer au tableau « " + DAMES
                        + " » : ce tableau imposé s'ajoute à ses 2 tableaux au choix, soit 3 tableaux au maximum.",
                        "Un joueur ou une joueuse ne peut pas s'inscrire à deux tableaux qui se suivent dans la journée."),
                texte(parJour(3, 2).avecTableauBonusJoueuses(DAMES).avecTableauBonusObligatoire(true)
                        .avecTableauxConsecutifsInterdits(true)));
    }

    @Test
    void dames_en_option_pour_le_troisieme() {
        assertEquals(List.of("Chaque joueur et chaque joueuse peut s'inscrire à 2 tableaux maximum par jour. "
                        + "Le samedi 10 avril, les joueuses inscrites au tableau « " + DAMES + " » peuvent s'inscrire "
                        + "à un troisième tableau de leur choix."),
                texte(parJour(3, 2).avecTableauBonusJoueuses(DAMES)));
    }

    @Test
    void troisieme_tableau_sans_condition() {
        assertEquals(List.of("Chaque joueur et chaque joueuse peut s'inscrire à 2 tableaux maximum par jour. "
                        + "Le samedi 10 avril, les joueuses peuvent s'inscrire à un troisième tableau de leur choix."),
                texte(parJour(3, 2)));
        assertEquals(List.of("Chaque joueur et chaque joueuse peut s'inscrire à 2 tableaux maximum par jour. "
                        + "Les joueuses peuvent s'inscrire à un troisième tableau de leur choix."),
                texte(QuotasTableaux.identiques(2, 3)));
    }

    @Test
    void dames_impose_sans_tableau_en_plus_compte_parmi_les_deux() {
        assertEquals(List.of("Chaque joueur et chaque joueuse peut s'inscrire à 2 tableaux maximum par jour. "
                        + "Le samedi 10 avril, toute joueuse qui s'inscrit doit aussi participer au tableau « " + DAMES
                        + " », qui compte parmi ses 2 tableaux."),
                texte(QuotasTableaux.identiques(2, 2).avecTableauBonusJoueuses(DAMES).avecTableauBonusObligatoire(true)));
    }

    @Test
    void meme_limite_pour_tous_et_regle_des_consecutifs_seule() {
        assertEquals(List.of("Chaque joueur et chaque joueuse peut s'inscrire à 2 tableaux maximum par jour."),
                texte(QuotasTableaux.identiques(2, 2)));
        assertEquals(List.of("Un joueur ou une joueuse ne peut pas s'inscrire à deux tableaux qui se suivent dans la journée."),
                texte(QuotasTableaux.aucun().avecTableauxConsecutifsInterdits(true)));
        assertEquals(List.of(), texte(QuotasTableaux.aucun()), "aucune règle : pas d'article");
    }

    @Test
    void limites_differentes_selon_le_jour_ou_le_sexe_liste_detaillee() {
        List<String> l = texte(QuotasTableaux.parJour(Map.of(SAM, new Quota(2, 2), DIM, new Quota(1, null))));
        assertEquals("Le nombre de tableaux auxquels on peut s'inscrire varie selon le jour :", l.get(0));
        assertTrue(l.get(2).startsWith("• Dimanche 11 avril : chaque joueur peut s'inscrire à 1 tableau par jour"), l.toString());
    }

    @Test
    void obligation_conservee_si_le_jour_du_tableau_dames_n_a_pas_de_limite() {
        // Limite seulement le dimanche ; le tableau dames (samedi) reste obligatoire : note en bas d'article.
        List<String> l = texte(QuotasTableaux.parJour(Map.of(DIM, new Quota(2, 2)))
                .avecTableauBonusJoueuses(DAMES).avecTableauBonusObligatoire(true));
        assertTrue(l.get(l.size() - 1).contains("Le samedi 10 avril, toute joueuse inscrite à un tableau doit aussi "
                + "participer au tableau « " + DAMES + " »."), l.toString());
    }
}
