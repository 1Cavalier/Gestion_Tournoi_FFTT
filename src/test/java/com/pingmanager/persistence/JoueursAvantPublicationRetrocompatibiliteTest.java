package com.pingmanager.persistence;

import com.pingmanager.core.PolitiqueOuverture;
import com.pingmanager.core.RemplissageTableaux;
import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.StatutTournoi;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * B052 : un fichier écrit avant la règle « inscriptions à la publication » (tournoi
 * homologué non publié contenant des joueurs) reste lisible, garde ses joueurs, et
 * le constat est disponible pour être signalé à l'ouverture.
 */
class JoueursAvantPublicationRetrocompatibiliteTest {

    @Test
    void ancien_fichier_homologue_avec_joueurs_relu_sans_perte_et_signale(@TempDir Path dir) throws Exception {
        Tournoi t = new Tournoi("Open", "Club", LocalDate.of(2027, 4, 10));
        Tableau a = new Tableau("A", ConfigurationTableau.depuisFormat(
                com.pingmanager.model.format.RegistreFormats.formatParDefaut()).placesMax(8).build());
        t.ajouterTableau(a);
        t.changerStatut(StatutTournoi.HOMOLOGUE);
        a.inscrire(new Joueur("1", "Un", "Un", Sexe.HOMME, "C", 800)); // écrit sans passer par les services
        a.inscrire(new Joueur("2", "Deux", "Deux", Sexe.HOMME, "C", 900));
        Path f = dir.resolve("ancien" + DepotTournoi.EXTENSION);
        DepotTournoi.sauvegarder(t, null, f);

        Tournoi relu = DepotTournoi.charger(f).getTournoi();
        assertEquals(StatutTournoi.HOMOLOGUE, relu.getStatut());
        assertEquals(2, RemplissageTableaux.nombreDeParticipants(relu), "aucun joueur supprimé à la lecture");
        assertTrue(PolitiqueOuverture.constatJoueursAvantPublication(relu).isPresent());
    }
}
