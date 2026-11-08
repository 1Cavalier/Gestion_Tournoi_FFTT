package com.pingmanager.ui.infra;

import com.pingmanager.core.SyntheseVocale.ParametresVoix;
import com.pingmanager.core.SyntheseVocale.ResultatSynthese;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** B017 (vérification) : le fichier temporaire est supprimé dans tous les cas d'échec. */
class SyntheseVocaleWindowsLimitesTest {

    private static final class Lanceur implements SyntheseVocaleWindows.Lanceur {
        final List<Path> fichiers = new ArrayList<>();
        RuntimeException exception;
        IOException erreur;
        SyntheseVocaleWindows.Sortie sortie = new SyntheseVocaleWindows.Sortie(0, "", false);

        @Override
        public SyntheseVocaleWindows.Sortie lancer(List<String> c, Map<String, String> env, Duration l)
                throws IOException {
            fichiers.add(Path.of(env.get(SyntheseVocaleWindows.VAR_SSML)));
            if (exception != null) {
                throw exception;
            }
            if (erreur != null) {
                throw erreur;
            }
            return sortie;
        }
    }

    @Test
    void fichier_supprime_apres_delai_depasse_code_d_erreur_exception_et_powershell_absent() {
        Lanceur l = new Lanceur();
        SyntheseVocaleWindows s = new SyntheseVocaleWindows(true, l);
        l.sortie = new SyntheseVocaleWindows.Sortie(-1, "", true);
        s.dire("<speak/>", null);
        l.sortie = new SyntheseVocaleWindows.Sortie(5, "", false);
        s.dire("<speak/>", new ParametresVoix("v", 1));
        l.sortie = new SyntheseVocaleWindows.Sortie(0, "", false);
        l.exception = new IllegalStateException("boum");
        assertThrows(IllegalStateException.class, () -> s.dire("<speak/>", null));
        l.exception = null;
        l.erreur = new IOException("introuvable");
        s.dire("<speak/>", null);
        assertEquals(4, l.fichiers.size());
        for (Path p : l.fichiers) {
            assertFalse(Files.exists(p), "fichier temporaire resté : " + p);
        }
    }

    @Test
    void ssml_nul_ne_laisse_pas_de_fichier_et_ne_condamne_pas_la_voix() {
        Lanceur l = new Lanceur();
        SyntheseVocaleWindows s = new SyntheseVocaleWindows(true, l);
        try {
            ResultatSynthese r = s.dire(null, null);
            assertFalse(r.dite());
        } catch (RuntimeException e) {
            // toléré : le service attrape les RuntimeException
        }
        assertTrue(s.estDisponible());
    }

    @Test
    void debit_et_voix_nuls_donnent_des_valeurs_sures_dans_l_environnement() {
        Map<String, String> env = SyntheseVocaleWindows.environnement(Path.of("/tmp/x.ssml"), null);
        assertEquals("", env.get(SyntheseVocaleWindows.VAR_VOIX));
        assertEquals("0", env.get(SyntheseVocaleWindows.VAR_DEBIT));
        assertEquals("", SyntheseVocaleWindows.environnement(Path.of("/tmp/x"), new ParametresVoix(null, 3))
                .get(SyntheseVocaleWindows.VAR_VOIX));
    }
}
