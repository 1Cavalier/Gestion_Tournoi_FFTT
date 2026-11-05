package com.pingmanager.persistence;

import com.pingmanager.model.DocumentReference;
import com.pingmanager.model.Tournoi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * DÉMONSTRATION DE FAILLE (audit de sécurité, voir RAPPORT_SECURITE.md).
 *
 * <p>Ce test passe <b>parce que</b> la persistance ne valide pas les chemins de
 * documents. Lors de la correction, il devra être <b>inversé</b> et renommé sans
 * le suffixe « faille_S06 ».</p>
 */
class FaillesSecuritePersistanceTest {

    @TempDir
    Path dossier;

    /**
     * Faille S-06 : le « règlement déposé » est stocké comme un chemin absolu libre
     * dans le fichier du tournoi, relu sans contrôle. L'onglet Tournois (ouvert à
     * tout compte, joueurs compris) recopie ensuite ce fichier tel quel vers
     * l'emplacement choisi (FicheTournoi.telecharger). Un chemin vers le dépôt
     * applicatif (empreintes de tous les comptes) ou un fichier système est
     * accepté. Au passage web, c'est une lecture arbitraire de fichiers du serveur.
     * Correctif : copier le PDF déposé dans le dossier de données (nom généré,
     * contrôle de l'en-tête %PDF et de la taille) et ne stocker qu'un nom relatif.
     */
    @Test
    void reglement_depose_pointant_vers_le_depot_applicatif_relu_sans_controle_faille_S06() {
        Tournoi t = new Tournoi("Open", "Brunoy", LocalDate.of(2027, 4, 10));
        String cheminSensible = dossier.resolve("..").resolve(DepotApplication.NOM_FICHIER).toString();
        t.setReglementDocument(new DocumentReference("Règlement", cheminSensible));
        Path f = dossier.resolve("open" + DepotTournoi.EXTENSION);

        DepotTournoi.sauvegarder(t, f);
        Tournoi relu = DepotTournoi.charger(f).getTournoi();

        assertEquals(cheminSensible, relu.getReglementDocument().getEmplacement(),
                "comportement vulnérable : chemin hors du dossier de données accepté");
    }
}
