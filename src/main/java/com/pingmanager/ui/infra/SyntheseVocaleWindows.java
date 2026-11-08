package com.pingmanager.ui.infra;

import com.pingmanager.core.SyntheseVocale;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Synthèse vocale de Windows (B017) : SAPI ({@code System.Speech}) appelée par
 * PowerShell, sans dépendance Maven.
 *
 * <p><b>Sécurité</b> : la ligne de commande est une <b>constante</b> (script
 * fixe passé en {@code -EncodedCommand}) ; elle ne contient jamais de nom de
 * joueur. Le SSML (déjà échappé XML par {@code core.GenerateurSsml}) est écrit
 * dans un fichier temporaire dont le chemin, comme la voix et le débit, passe
 * par des variables d'environnement du seul processus fils. Le processus est
 * borné dans le temps puis tué ; le fichier est toujours supprimé.</p>
 *
 * <p>Hors Windows, ou si PowerShell est introuvable, la synthèse se déclare
 * indisponible : l'annonce est affichée à l'écran (repli silencieux).</p>
 */
public final class SyntheseVocaleWindows implements SyntheseVocale {

    /** Variables d'environnement lues par le script. */
    static final String VAR_SSML = "PINGMANAGER_SSML";
    static final String VAR_VOIX = "PINGMANAGER_VOIX";
    static final String VAR_DEBIT = "PINGMANAGER_DEBIT";

    /** Durée maximale d'une annonce (trois diffusions comprises). */
    static final Duration LIMITE_ANNONCE = Duration.ofSeconds(60);
    /** Durée maximale de la liste des voix. */
    static final Duration LIMITE_VOIX = Duration.ofSeconds(15);

    /** Script fixe : lit le SSML dans le fichier désigné par l'environnement et le dit. */
    static final String SCRIPT_DIRE = String.join("\n",
            "$ErrorActionPreference = 'Stop'",
            "Add-Type -AssemblyName System.Speech",
            "$s = New-Object System.Speech.Synthesis.SpeechSynthesizer",
            "try {",
            "  $v = $env:" + VAR_VOIX,
            "  if ($v) { try { $s.SelectVoice($v) } catch { } }",
            "  $d = 0",
            "  if ([int]::TryParse($env:" + VAR_DEBIT + ", [ref]$d)) { $s.Rate = [Math]::Max(-10, [Math]::Min(10, $d)) }",
            "  $s.SetOutputToDefaultAudioDevice()",
            "  $x = [IO.File]::ReadAllText($env:" + VAR_SSML + ", [Text.Encoding]::UTF8)",
            "  $s.SpeakSsml($x)",
            "} finally { $s.Dispose() }");

    /** Script fixe : noms des voix installées et actives, une par ligne. */
    static final String SCRIPT_VOIX = String.join("\n",
            "[Console]::OutputEncoding = [Text.Encoding]::UTF8",
            "Add-Type -AssemblyName System.Speech",
            "$s = New-Object System.Speech.Synthesis.SpeechSynthesizer",
            "try { $s.GetInstalledVoices() | Where-Object { $_.Enabled } | ForEach-Object { $_.VoiceInfo.Name } }",
            "finally { $s.Dispose() }");

    /** Lance un processus (remplaçable dans les tests). */
    interface Lanceur {
        Sortie lancer(List<String> commande, Map<String, String> environnement, Duration limite) throws IOException;
    }

    /** Fin d'un processus : code de sortie, sortie standard, délai dépassé ? */
    record Sortie(int code, String texte, boolean expire) {
    }

    private final boolean windows;
    private final Lanceur lanceur;
    private volatile boolean powershellAbsent;

    /** Synthèse du système, active seulement sous Windows. */
    public SyntheseVocaleWindows() {
        this(System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("windows"),
                SyntheseVocaleWindows::lancerProcessus);
    }

    SyntheseVocaleWindows(boolean windows, Lanceur lanceur) {
        this.windows = windows;
        this.lanceur = lanceur;
    }

    @Override
    public boolean estDisponible() {
        return windows && !powershellAbsent;
    }

    @Override
    public ResultatSynthese dire(String ssml, ParametresVoix voix) {
        if (!estDisponible()) {
            return ResultatSynthese.echec(windows ? "PowerShell introuvable"
                    : "synthèse vocale disponible sous Windows seulement");
        }
        Path fichier = null;
        try {
            fichier = Files.createTempFile("pingmanager-annonce-", ".ssml");
            Files.writeString(fichier, ssml, StandardCharsets.UTF_8);
            Sortie s = lanceur.lancer(commande(SCRIPT_DIRE), environnement(fichier, voix), LIMITE_ANNONCE);
            if (s.expire()) {
                return ResultatSynthese.echec("la voix n'a pas fini à temps (" + LIMITE_ANNONCE.toSeconds()
                        + " s) : arrêtée");
            }
            if (s.code() != 0) {
                return ResultatSynthese.echec("la synthèse vocale de Windows a échoué (code " + s.code() + ")");
            }
            return ResultatSynthese.ok();
        } catch (IOException e) {
            powershellAbsent = fichier != null;
            return ResultatSynthese.echec("PowerShell introuvable ou inutilisable (" + e.getMessage() + ")");
        } finally {
            if (fichier != null) {
                try {
                    Files.deleteIfExists(fichier);
                } catch (IOException ignore) {
                    // Fichier temporaire : le système le nettoiera.
                }
            }
        }
    }

    @Override
    public List<String> voix() {
        if (!estDisponible()) {
            return List.of();
        }
        try {
            Sortie s = lanceur.lancer(commande(SCRIPT_VOIX), Map.of(), LIMITE_VOIX);
            if (s.expire() || s.code() != 0) {
                return List.of();
            }
            List<String> noms = new ArrayList<>();
            for (String ligne : s.texte().split("\\R")) {
                if (!ligne.isBlank()) {
                    noms.add(ligne.strip());
                }
            }
            return noms;
        } catch (IOException e) {
            return List.of();
        }
    }

    /** Ligne de commande : PowerShell et un script fixe encodé ; aucune donnée de joueur. */
    static List<String> commande(String script) {
        String encode = Base64.getEncoder().encodeToString(script.getBytes(StandardCharsets.UTF_16LE));
        return List.of("powershell.exe", "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass",
                "-EncodedCommand", encode);
    }

    /** Variables du processus fils : chemin du fichier SSML, voix et débit (réglages du club). */
    static Map<String, String> environnement(Path fichierSsml, ParametresVoix voix) {
        Map<String, String> env = new LinkedHashMap<>();
        env.put(VAR_SSML, fichierSsml.toAbsolutePath().toString());
        ParametresVoix v = voix == null ? ParametresVoix.PAR_DEFAUT : voix;
        env.put(VAR_VOIX, v.voix() == null ? "" : v.voix());
        env.put(VAR_DEBIT, String.valueOf(v.debit()));
        return env;
    }

    private static Sortie lancerProcessus(List<String> commande, Map<String, String> environnement, Duration limite)
            throws IOException {
        ProcessBuilder pb = new ProcessBuilder(commande).redirectErrorStream(true);
        pb.environment().putAll(environnement);
        Process p = pb.start();
        p.getOutputStream().close();
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        Thread lecteur = new Thread(() -> copier(p.getInputStream(), sortie), "pingmanager-voix-sortie");
        lecteur.setDaemon(true);
        lecteur.start();
        try {
            if (!p.waitFor(limite.toMillis(), TimeUnit.MILLISECONDS)) {
                p.destroyForcibly();
                return new Sortie(-1, "", true);
            }
            lecteur.join(2000);
        } catch (InterruptedException e) {
            p.destroyForcibly();
            Thread.currentThread().interrupt();
            return new Sortie(-1, "", true);
        }
        synchronized (sortie) {
            return new Sortie(p.exitValue(), sortie.toString(StandardCharsets.UTF_8), false);
        }
    }

    private static void copier(InputStream in, ByteArrayOutputStream sortie) {
        byte[] tampon = new byte[4096];
        try (in) {
            int n;
            while ((n = in.read(tampon)) >= 0) {
                synchronized (sortie) {
                    sortie.write(tampon, 0, n);
                }
            }
        } catch (IOException ignore) {
            // Processus tué : sortie incomplète, sans importance.
        }
    }
}
