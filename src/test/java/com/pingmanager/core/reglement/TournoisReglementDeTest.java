package com.pingmanager.core.reglement;

import com.pingmanager.core.ServiceCycleTournoi;
import com.pingmanager.model.AdresseTournoi;
import com.pingmanager.model.AffectationJugeArbitre;
import com.pingmanager.model.ArticleOptionnel;
import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.GradeJugeArbitre;
import com.pingmanager.model.HorairesTableau;
import com.pingmanager.model.InfosReglement;
import com.pingmanager.model.NiveauTournoi;
import com.pingmanager.model.OptionsReglement;
import com.pingmanager.model.PolitiqueBalles;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.format.RegistreFormats;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tournois de test partagés (règlement généré, publiable) : cœur et persistance. */
public final class TournoisReglementDeTest {

    private TournoisReglementDeTest() {
    }

    private static final LocalDate JOUR = LocalDate.of(2027, 3, 13);

    /** Tournoi au règlement généré, prêt à publier une fois homologué. */
    public static Tournoi tournoiPubliable() {
        Tournoi t = new Tournoi("Open de Brunoy", "Brunoy CTT", JOUR);
        t.setInfosReglement(InfosReglement.builder()
                .responsableNom("Boulier Serge").responsableEmail("serge@club.fr")
                .salleNom("Gymnase municipal").salleVille("Brunoy")
                .nombreDeTables(12).marqueBalles("Cornilleau ABS 40+")
                .politiqueBalles(PolitiqueBalles.FOURNIES_PAR_ORGANISATEUR)
                .ouvertureInscriptions(LocalDateTime.of(2027, 2, 1, 9, 0))
                .clotureInscriptions(LocalDateTime.of(2027, 3, 10, 23, 59))
                .cloturePointage(LocalDateTime.of(2027, 3, 13, 8, 30))
                .debutMatchs(LocalDateTime.of(2027, 3, 13, 9, 0))
                .finPrevue(LocalTime.of(18, 0))
                .build());
        t.setCorrespondant("Boulier", "Serge", "serge@club.fr", null, null);
        t.ajouterJugeArbitre(new AffectationJugeArbitre("Durand", "Paul", "paul@x.fr", "1234567",
                GradeJugeArbitre.JA3));
        t.setPeriode(JOUR, JOUR);
        t.setAdresse(AdresseTournoi.de("5 rue de la Concorde", "91800", "Brunoy"));
        t.setNiveau(NiveauTournoi.DEPARTEMENTAL);
        t.setNombreDeTables(12);
        t.setOptionsReglement(new OptionsReglement(OptionsReglement.Mode.GENERE,
                List.of(ArticleOptionnel.de(ArticleOptionnel.Type.DROIT_IMAGE))));
        t.ajouterTableau(tableau("Tableau A", null));
        t.ajouterTableau(tableau("Dames", Sexe.FEMME));
        return t;
    }

    private static Tableau tableau(String nom, Sexe sexe) {
        Tableau t = new Tableau(nom, ConfigurationTableau.builder().sexeRequis(sexe)
                .format(RegistreFormats.formatParDefaut()).placesMax(24).build());
        t.setType("Toutes séries");
        HorairesTableau h = new HorairesTableau();
        h.setDebutMatchs(LocalDateTime.of(2027, 3, 13, 9, 0));
        t.setHoraires(h);
        return t;
    }

    public static Tournoi tournoiPublie() {
        Tournoi t = tournoiPubliable();
        ServiceCycleTournoi cycle = new ServiceCycleTournoi();
        assertTrue(cycle.homologuer(t, "H-2027-001").estEffectuee());
        assertTrue(cycle.publier(t).estEffectuee(), "le tournoi de test doit être publiable");
        return t;
    }
}
