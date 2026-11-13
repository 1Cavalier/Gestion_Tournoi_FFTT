package com.pingmanager.ui;

import com.pingmanager.model.Tournoi;
import com.pingmanager.persistence.FichierOccupeException;
import com.pingmanager.persistence.VerrouFichier;
import com.pingmanager.ui.infra.DonneesDemo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Point 21 : poste de pointage et juge-arbitre sur le même fichier, chacun relit le
 * tournoi avant son action et l'enregistre juste après (B041/B043) : sous le verrou,
 * aucune modification n'est perdue.
 */
class VerrouTournoiContexteTest {

    @TempDir
    Path dossier;

    @Test
    void deux_postes_qui_modifient_le_meme_tournoi_en_meme_temps_ne_perdent_rien() throws Exception {
        ContexteApplication pointage = TournoisDeTest.connecteBrunoy(
                new ContexteApplication(dossier, DonneesDemo.clubs(), DonneesDemo.licences()));
        ContexteApplication arbitre = TournoisDeTest.connecteBrunoy(
                new ContexteApplication(dossier, DonneesDemo.clubs(), DonneesDemo.licences()));
        Path f = pointage.creerBrouillon(TournoisDeTest.deBrunoy(new Tournoi("Open", "Brunoy", LocalDate.of(2027, 6, 5))));
        int parPoste = 15;
        CountDownLatch depart = new CountDownLatch(1);
        AtomicReference<Throwable> erreur = new AtomicReference<>();
        Thread a = new Thread(() -> ajouter(pointage, f, "a", parPoste, depart, erreur));
        Thread b = new Thread(() -> ajouter(arbitre, f, "b", parPoste, depart, erreur));
        a.start();
        b.start();
        depart.countDown();
        a.join();
        b.join();
        assertEquals(null, erreur.get());
        String nom = pointage.chargerTournoi(f).getNom();
        assertEquals(parPoste, nom.chars().filter(c -> c == 'a').count(), nom);
        assertEquals(parPoste, nom.chars().filter(c -> c == 'b').count(), nom);
    }

    private static void ajouter(ContexteApplication ctx, Path f, String marque, int fois, CountDownLatch depart,
                                AtomicReference<Throwable> erreur) {
        try {
            depart.await();
            for (int i = 0; i < fois; i++) {
                ctx.modifierTournoi(f, t -> {
                    t.setNom(t.getNom() + marque);
                    return true;
                });
            }
        } catch (Throwable e) {
            erreur.set(e);
        }
    }

    @Test
    void tournoi_verrouille_par_un_autre_ordinateur_enregistrement_refuse_proprement() throws Exception {
        ContexteApplication ctx = TournoisDeTest.connecteBrunoy(
                new ContexteApplication(dossier, DonneesDemo.clubs(), DonneesDemo.licences()));
        Path f = ctx.creerBrouillon(TournoisDeTest.deBrunoy(new Tournoi("Open", "Brunoy", LocalDate.of(2027, 6, 5))));
        Files.writeString(VerrouFichier.cheminVerrou(f), "ordinateur=PC-JA\nheure=" + Instant.now() + "\njeton=j\n");
        Tournoi t = ctx.chargerTournoi(f); // la lecture n'est pas bloquée
        t.setNom("Changé");
        FichierOccupeException e = assertThrows(FichierOccupeException.class, () -> ctx.enregistrerTournoi(t, f));
        assertEquals(true, e.getMessage().contains("PC-JA"));
        assertEquals("Open", ctx.chargerTournoi(f).getNom(), "rien n'est écrit");
    }
}
