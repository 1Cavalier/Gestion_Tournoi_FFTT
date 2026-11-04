package com.pingmanager.core;

import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Scénario complet reproduisant les cas décrits pour valider le format :
 *
 * <ul>
 *   <li>Joueur A : inscrit A et B en ligne, paie les deux en ligne. Pointe A et B
 *       le jour J → les deux tableaux sont validés.</li>
 *   <li>Joueur B : inscrit B en ligne, paie en ligne. Pointe B, puis se désiste
 *       avant la fin du pointage → libère sa place (repêchage).</li>
 *   <li>Joueur C : inscrit A, en liste d'attente sur B. Paie A uniquement (on ne
 *       paie pas un tableau en liste d'attente). Pointe A. S'il est repêché sur B,
 *       il doit passer par le paiement pour confirmer B.</li>
 *   <li>Joueur D : inscrit A et B, ne paie pas en ligne, paie sur place. Pointe et
 *       paie A et B le jour J.</li>
 * </ul>
 */
class ScenarioInscriptionPointageTest {

    private Joueur joueur(String licence, Sexe sexe) {
        return new Joueur(licence, "Nom" + licence, "Prenom" + licence, sexe, "Club", 1000);
    }

    @Test
    void scenario_complet_quatre_joueurs() {
        // Tableau A : capacité 2 pour forcer une liste d'attente sur B et A.
        Tableau tableauA = new Tableau("A", ConfigurationTableau.builder()
                .placesMax(3).tailleMaxListeAttente(4).build());
        Tableau tableauB = new Tableau("B", ConfigurationTableau.builder()
                .placesMax(2).tailleMaxListeAttente(4).build());

        Tournoi tournoi = new Tournoi("Open", "Club", LocalDate.now());
        tournoi.ajouterTableau(tableauA);
        tournoi.ajouterTableau(tableauB);

        Joueur a = joueur("A", Sexe.HOMME);
        Joueur b = joueur("B", Sexe.HOMME);
        Joueur c = joueur("C", Sexe.HOMME);
        Joueur d = joueur("D", Sexe.HOMME);

        ServiceInscription inscription = ServiceInscription.sansContrainteSuccession();
        ServicePointage pointage = new ServicePointage(tournoi);

        // --- Phase d'inscription (avant le jour J) ---

        // B remplit d'abord pour mettre C en liste d'attente sur B.
        // Joueur A : inscrit A et B, paie les deux en ligne.
        assertEquals(ResultatInscriptionTournoi.Motif.INSCRIT, inscription.inscrire(tournoi, a, tableauA).getMotif());
        assertEquals(ResultatInscriptionTournoi.Motif.INSCRIT, inscription.inscrire(tournoi, a, tableauB).getMotif());
        tableauA.marquerPaye(a);
        tableauB.marquerPaye(a);

        // Joueur B : inscrit B, paie en ligne.
        assertEquals(ResultatInscriptionTournoi.Motif.INSCRIT, inscription.inscrire(tournoi, b, tableauB).getMotif());
        tableauB.marquerPaye(b);

        // À ce stade B est plein (a, b) -> capacité 2 atteinte.
        assertTrue(tableauB.estPlein());

        // Joueur C : inscrit A, liste d'attente B (B plein). Paie A uniquement.
        assertEquals(ResultatInscriptionTournoi.Motif.INSCRIT, inscription.inscrire(tournoi, c, tableauA).getMotif());
        assertEquals(ResultatInscriptionTournoi.Motif.EN_FILE_ATTENTE,
                inscription.inscrire(tournoi, c, tableauB).getMotif());
        tableauA.marquerPaye(c);
        // On ne peut pas payer B tant qu'on est en liste d'attente.
        assertThrows(IllegalArgumentException.class, () -> tableauB.marquerPaye(c));
        assertFalse(tableauB.aPaye(c));

        // Joueur D : inscrit A et B ? B est plein -> D va en file B aussi.
        assertEquals(ResultatInscriptionTournoi.Motif.INSCRIT, inscription.inscrire(tournoi, d, tableauA).getMotif());
        assertEquals(ResultatInscriptionTournoi.Motif.EN_FILE_ATTENTE,
                inscription.inscrire(tournoi, d, tableauB).getMotif());
        // D ne paie rien en ligne (paiement sur place le jour J).
        assertFalse(tableauA.aPaye(d));

        // --- Jour J : pointage ---

        // --- Jour J : le JA ouvre le pointage, tableau par tableau ---
        tableauA.ouvrirPointage();
        tableauB.ouvrirPointage();

        // Joueur A pointe A et B : présent + déjà payé => validé sur les deux.
        pointage.pointer(tableauA, a);
        pointage.pointer(tableauB, a);
        assertTrue(tableauA.aPointe(a) && tableauA.aPaye(a));
        assertTrue(tableauB.aPointe(a) && tableauB.aPaye(a));

        // Joueur B pointe B, puis se désiste avant la fin du pointage.
        pointage.pointer(tableauB, b);
        assertTrue(tableauB.aPointe(b));
        // Désistement avant clôture : libère la place, repêche le 1er de la file B (C).
        Joueur repecheB = pointage.desister(tableauB, b);
        assertEquals(c, repecheB, "C, premier en file B, est repêché");
        assertFalse(tableauB.getInscrits().contains(b));
        assertTrue(tableauB.getInscrits().contains(c));
        // C repêché sur B : il n'a ni pointé ni payé B, il doit passer par le paiement.
        assertFalse(tableauB.aPointe(c));
        assertFalse(tableauB.aPaye(c));

        // Joueur C pointe A (déjà payé) ; confirme B par paiement + pointage (repêché).
        pointage.pointer(tableauA, c);
        assertTrue(tableauA.aPointe(c) && tableauA.aPaye(c));
        // Maintenant qu'il est inscrit sur B (repêché), il peut payer et pointer B.
        pointage.marquerPaye(tableauB, c);
        pointage.pointer(tableauB, c);
        assertTrue(tableauB.aPointe(c) && tableauB.aPaye(c));

        // Joueur D pointe et paie A et B sur place.
        pointage.pointer(tableauA, d);
        pointage.marquerPaye(tableauA, d);
        // D est-il inscrit sur B ? Une place s'est libérée (désistement de b) et C l'a prise.
        // D reste donc en file d'attente B ; on ne peut pas encore le pointer sur B.
        assertFalse(tableauB.getInscrits().contains(d));
        assertThrows(IllegalArgumentException.class, () -> pointage.pointer(tableauB, d));
        assertTrue(tableauA.aPointe(d) && tableauA.aPaye(d));

        // --- Clôture du pointage de A : personne à libérer (tous pointés). ---
        ResultatPointage rA = pointage.cloturerPointage(tableauA);
        assertEquals(0, rA.nombreLiberes());
        assertTrue(tableauA.getInscrits().contains(a));
        assertTrue(tableauA.getInscrits().contains(c));
        assertTrue(tableauA.getInscrits().contains(d));
        assertTrue(tableauA.pointageEstCloture());
        assertEquals(com.pingmanager.model.StatutTableau.POINTAGE, tableauA.getStatut(),
                "clôturer le pointage ne lance pas : le tableau reste en POINTAGE");
    }

    @Test
    void desistement_apres_cloture_refuse_sauf_cas_particulier() {
        Tableau a = new Tableau("A", ConfigurationTableau.builder()
                .placesMax(2).tailleMaxListeAttente(4).build());
        Tournoi t = new Tournoi("Open", "Club", LocalDate.now());
        t.ajouterTableau(a);
        Joueur j1 = joueur("1", Sexe.HOMME);
        Joueur j2 = joueur("2", Sexe.HOMME);
        a.inscrire(j1);
        a.inscrire(j2);
        ServicePointage pointage = new ServicePointage(t);
        pointage.pointer(a, j1);
        pointage.pointer(a, j2);
        pointage.cloturerPointage(a); // pointage clôturé

        // Désistement normal après clôture : refusé (le joueur est tenu de jouer).
        assertThrows(IllegalStateException.class, () -> pointage.desister(a, j1));
        assertTrue(a.getInscrits().contains(j1));

        // Cas particulier (blessure) : le JA peut retirer et rembourser.
        pointage.desisterCasParticulier(a, j1, true);
        assertFalse(a.getInscrits().contains(j1));
        assertTrue(a.aEteRembourse(j1), "le remboursement est tracé même après retrait");
    }

    @Test
    void desistement_avant_cloture_sans_file_ne_repeche_personne() {
        Tableau a = new Tableau("A", ConfigurationTableau.builder()
                .placesMax(4).tailleMaxListeAttente(4).build());
        Tournoi t = new Tournoi("Open", "Club", LocalDate.now());
        t.ajouterTableau(a);
        Joueur j1 = joueur("1", Sexe.HOMME);
        a.inscrire(j1);
        ServicePointage pointage = new ServicePointage(t);

        Joueur repeche = pointage.desister(a, j1);
        assertNull(repeche);
        assertFalse(a.getInscrits().contains(j1));
    }
}
