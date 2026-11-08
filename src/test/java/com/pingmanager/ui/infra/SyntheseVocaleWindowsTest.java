package com.pingmanager.ui.infra;

import com.pingmanager.core.GenerateurSsml;
import com.pingmanager.core.SyntheseVocale.ParametresVoix;
import com.pingmanager.core.SyntheseVocale.ResultatSynthese;
import com.pingmanager.core.TexteAnnonceVocale;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Match;
import com.pingmanager.model.Sexe;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** B017 : synthèse de Windows ; aucune donnée de joueur dans la ligne de commande. */
class SyntheseVocaleWindowsTest {

    /** Lanceur simulé : garde la commande, l'environnement et le fichier SSML lu pendant l'appel. */
    private static final class LanceurEnregistre implements SyntheseVocaleWindows.Lanceur {
        final List<List<String>> commandes = new ArrayList<>();
        final List<Map<String, String>> environnements = new ArrayList<>();
        final List<String> fichiersLus = new ArrayList<>();
        final List<Duration> limites = new ArrayList<>();
        SyntheseVocaleWindows.Sortie sortie = new SyntheseVocaleWindows.Sortie(0, "", false);
        IOException erreur;

        @Override
        public SyntheseVocaleWindows.Sortie lancer(List<String> commande, Map<String, String> env, Duration limite)
                throws IOException {
            if (erreur != null) {
                throw erreur;
            }
            commandes.add(commande);
            environnements.add(env);
            limites.add(limite);
            String chemin = env.get(SyntheseVocaleWindows.VAR_SSML);
            if (chemin != null) {
                fichiersLus.add(Files.readString(Path.of(chemin), StandardCharsets.UTF_8));
            }
            return sortie;
        }
    }

    private static String ssmlAvecNoms() {
        Joueur a = new Joueur("1", "O'BRIEN; Remove-Item C:\\ -Recurse", "Seán", Sexe.HOMME, "TT \"Brunoy\" & Co", 1);
        Joueur b = new Joueur("2", "DURAND", "Léa", Sexe.FEMME, "US Évry", 1);
        return GenerateurSsml.generer(TexteAnnonceVocale.composer(new Match(a, b), 4, null), 2, null);
    }

    @Test
    void ligne_de_commande_sans_aucune_donnee_de_joueur() {
        LanceurEnregistre lanceur = new LanceurEnregistre();
        SyntheseVocaleWindows s = new SyntheseVocaleWindows(true, lanceur);
        String ssml = ssmlAvecNoms();
        assertTrue(s.dire(ssml, new ParametresVoix("Microsoft Hortense Desktop", 2)).dite());

        List<String> commande = lanceur.commandes.get(0);
        assertEquals(SyntheseVocaleWindows.commande(SyntheseVocaleWindows.SCRIPT_DIRE), commande,
                "ligne de commande constante");
        String script = new String(Base64.getDecoder().decode(commande.get(commande.size() - 1)),
                StandardCharsets.UTF_16LE);
        assertEquals(SyntheseVocaleWindows.SCRIPT_DIRE, script);
        String tout = String.join(" ", commande) + " " + script;
        for (String donnee : List.of("Brien", "BRIEN", "Seán", "Remove-Item", "Brunoy", "Durand", "DURAND", "Léa",
                "Évry", "table 4", "speak")) {
            assertFalse(tout.contains(donnee), "donnée dans la ligne de commande : " + donnee);
        }
        // L'environnement ne porte que le chemin du fichier, la voix et le débit (réglages du club).
        Map<String, String> env = lanceur.environnements.get(0);
        assertEquals(List.of(SyntheseVocaleWindows.VAR_SSML, SyntheseVocaleWindows.VAR_VOIX,
                SyntheseVocaleWindows.VAR_DEBIT), List.copyOf(env.keySet()));
        assertEquals("Microsoft Hortense Desktop", env.get(SyntheseVocaleWindows.VAR_VOIX));
        assertEquals("2", env.get(SyntheseVocaleWindows.VAR_DEBIT));
        for (String v : env.values()) {
            assertFalse(v.contains("Durand") || v.contains("Brien") || v.contains("Brunoy"), v);
        }
        // Le SSML passe par le fichier temporaire, échappé, puis le fichier est supprimé.
        assertEquals(ssml, lanceur.fichiersLus.get(0));
        assertTrue(ssml.contains("TT &quot;Brunoy&quot; &amp; Co"), ssml);
        assertFalse(Files.exists(Path.of(env.get(SyntheseVocaleWindows.VAR_SSML))), "fichier temporaire supprimé");
        assertEquals(SyntheseVocaleWindows.LIMITE_ANNONCE, lanceur.limites.get(0), "processus borné");
    }

    @Test
    void indisponible_hors_windows_sans_lancer_de_processus() {
        LanceurEnregistre lanceur = new LanceurEnregistre();
        SyntheseVocaleWindows s = new SyntheseVocaleWindows(false, lanceur);
        assertFalse(s.estDisponible());
        ResultatSynthese r = s.dire(ssmlAvecNoms(), null);
        assertFalse(r.dite());
        assertEquals("synthèse vocale disponible sous Windows seulement", r.motif());
        assertTrue(s.voix().isEmpty());
        assertTrue(lanceur.commandes.isEmpty());
    }

    @Test
    void delai_depasse_ou_code_d_erreur_rendent_un_echec_motive() {
        LanceurEnregistre lanceur = new LanceurEnregistre();
        SyntheseVocaleWindows s = new SyntheseVocaleWindows(true, lanceur);
        lanceur.sortie = new SyntheseVocaleWindows.Sortie(-1, "", true);
        assertTrue(s.dire("<speak/>", null).motif().startsWith("la voix n'a pas fini à temps"));
        lanceur.sortie = new SyntheseVocaleWindows.Sortie(1, "Exception", false);
        assertEquals("la synthèse vocale de Windows a échoué (code 1)", s.dire("<speak/>", null).motif());
        assertTrue(s.estDisponible());
        assertEquals("", lanceur.environnements.get(1).get(SyntheseVocaleWindows.VAR_VOIX), "voix par défaut");
    }

    @Test
    void powershell_introuvable_rend_la_voix_indisponible_ensuite() {
        LanceurEnregistre lanceur = new LanceurEnregistre();
        lanceur.erreur = new IOException("Cannot run program \"powershell.exe\"");
        SyntheseVocaleWindows s = new SyntheseVocaleWindows(true, lanceur);
        ResultatSynthese r = s.dire("<speak/>", null);
        assertFalse(r.dite());
        assertTrue(r.motif().startsWith("PowerShell introuvable"), r.motif());
        assertFalse(s.estDisponible());
    }

    @Test
    void liste_des_voix_lue_par_un_script_fixe() {
        LanceurEnregistre lanceur = new LanceurEnregistre();
        lanceur.sortie = new SyntheseVocaleWindows.Sortie(0, "Microsoft Hortense Desktop\r\n\r\nMicrosoft Paul\n", false);
        SyntheseVocaleWindows s = new SyntheseVocaleWindows(true, lanceur);
        assertEquals(List.of("Microsoft Hortense Desktop", "Microsoft Paul"), s.voix());
        assertEquals(SyntheseVocaleWindows.commande(SyntheseVocaleWindows.SCRIPT_VOIX), lanceur.commandes.get(0));
        assertTrue(lanceur.environnements.get(0).isEmpty());
        assertEquals(SyntheseVocaleWindows.LIMITE_VOIX, lanceur.limites.get(0));
    }
}
