package com.pingmanager.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Point 21 : verrou court par fichier entre deux postes ou deux fenêtres. */
class VerrouFichierTest {

    @TempDir
    Path dossier;

    private static void ecrireCompteur(Path f, int n) {
        EcritureSure.ecrire(f, "{\"n\":" + n + "}");
    }

    private static int lireCompteur(Path f) {
        return EcritureSure.lireJson(f).document().get("n").asInt();
    }

    @Test
    void deux_fils_qui_relisent_modifient_et_ecrivent_en_meme_temps_ne_perdent_rien() throws Exception {
        Path f = dossier.resolve("t.ptt.json");
        ecrireCompteur(f, 0);
        int parFil = 40;
        CountDownLatch depart = new CountDownLatch(1);
        AtomicReference<Throwable> erreur = new AtomicReference<>();
        Runnable travail = () -> {
            try {
                depart.await();
                for (int i = 0; i < parFil; i++) {
                    VerrouFichier.executer(f, () -> ecrireCompteur(f, lireCompteur(f) + 1));
                }
            } catch (Throwable e) {
                erreur.set(e);
            }
        };
        Thread a = new Thread(travail);
        Thread b = new Thread(travail);
        a.start();
        b.start();
        depart.countDown();
        a.join();
        b.join();
        assertEquals(null, erreur.get());
        assertEquals(2 * parFil, lireCompteur(f), "aucune modification perdue");
        assertFalse(Files.exists(VerrouFichier.cheminVerrou(f)), "verrou relâché");
    }

    @Test
    void verrou_tenu_par_un_autre_poste_message_clair_apres_quelques_essais_et_rien_n_est_ecrit() throws Exception {
        Path f = dossier.resolve("t.ptt.json");
        ecrireCompteur(f, 7);
        Files.writeString(VerrouFichier.cheminVerrou(f),
                "ordinateur=PC-ARBITRE\nheure=" + Instant.now() + "\njeton=autre\n");
        boolean[] lance = {false};
        FichierOccupeException e = assertThrows(FichierOccupeException.class, () -> VerrouFichier.executer(f,
                Duration.ofSeconds(30), 3, Duration.ofMillis(10), () -> {
                    lance[0] = true;
                    return null;
                }));
        assertTrue(e.getMessage().startsWith("Le tournoi est en cours de modification sur PC-ARBITRE (depuis "),
                e.getMessage());
        assertTrue(e.getMessage().endsWith("Rien n'a été modifié."));
        assertFalse(lance[0]);
        assertEquals(7, lireCompteur(f), "la lecture n'est pas bloquée");
        assertTrue(Files.exists(VerrouFichier.cheminVerrou(f)), "le verrou d'autrui n'est pas retiré");
    }

    @Test
    void verrou_abandonne_apres_un_plantage_expire_et_est_repris() throws Exception {
        Path f = dossier.resolve("t.ptt.json");
        ecrireCompteur(f, 1);
        Path verrou = VerrouFichier.cheminVerrou(f);
        Files.writeString(verrou, "ordinateur=PC-PLANTE\nheure=2026-01-01T00:00:00Z\njeton=x\n");
        Files.setLastModifiedTime(verrou, FileTime.from(Instant.now().minusSeconds(120)));
        VerrouFichier.executer(f, () -> ecrireCompteur(f, 2));
        assertEquals(2, lireCompteur(f));
        assertFalse(Files.exists(verrou));
    }

    @Test
    void ecriture_imbriquee_dans_le_meme_fil_ne_se_bloque_pas() {
        Path f = dossier.resolve("t.ptt.json");
        ecrireCompteur(f, 0);
        int r = VerrouFichier.executer(f, () -> VerrouFichier.executer(f, () -> {
            ecrireCompteur(f, 5);
            return lireCompteur(f);
        }));
        assertEquals(5, r);
        assertFalse(Files.exists(VerrouFichier.cheminVerrou(f)));
    }
}
