package com.pingmanager.persistence;

import com.pingmanager.persistence.json.FichierJson;
import com.pingmanager.persistence.json.Json;
import com.pingmanager.persistence.json.JsonParser;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Point 21 du README : écriture et relecture <b>sûres</b> des fichiers de données
 * (tournoi, session, application, journal), partagées par tous les dépôts.
 *
 * <p>Écriture ({@link #ecrire}) :</p>
 * <ol>
 *   <li>le contenu est écrit dans un fichier temporaire du <b>même dossier</b> et
 *       forcé sur le disque ({@link FileChannel#force}) ;</li>
 *   <li>la version précédente, si elle est lisible, est gardée en
 *       {@code <nom>.bak} (elle aussi forcée sur le disque) ; une version déjà abîmée
 *       n'écrase jamais une bonne copie de secours ;</li>
 *   <li>le temporaire remplace la cible par un déplacement atomique (repli : simple
 *       remplacement quand le système de fichiers ne le permet pas), puis le dossier
 *       est forcé sur le disque quand le système le permet.</li>
 * </ol>
 * <p>Relecture ({@link #lireJson}) : si le fichier principal est illisible ou tronqué,
 * la copie {@code .bak} est utilisée et le fait est signalé (message gardé dans
 * {@link #avertissements()}), sans planter. Si les deux sont illisibles, l'erreur du
 * fichier principal est levée, comme avant.</p>
 */
public final class EcritureSure {

    /** Suffixe de la copie de la version précédente. */
    public static final String SUFFIXE_SECOURS = ".bak";

    private static final List<String> AVERTISSEMENTS = Collections.synchronizedList(new ArrayList<>());

    private EcritureSure() {
    }

    /** Une lecture : le document, et si la copie de secours a servi. */
    public record LectureJson(Json document, boolean depuisSecours, String avertissement) {
    }

    /** @return la copie de secours d'un fichier ({@code <nom>.bak}, même dossier). */
    public static Path secours(Path fichier) {
        return fichier.resolveSibling(fichier.getFileName().toString() + SUFFIXE_SECOURS);
    }

    /**
     * Écrit un texte UTF-8 de façon sûre (temporaire forcé sur disque, copie {@code .bak}
     * de la version précédente, déplacement atomique).
     *
     * @throws UncheckedIOException si l'écriture échoue (la cible est alors intacte)
     */
    public static void ecrire(Path cible, String texte) {
        Path abs = cible.toAbsolutePath();
        Path parent = abs.getParent();
        Path tmp = null;
        try {
            if (parent != null) {
                Files.createDirectories(parent);
            }
            tmp = Files.createTempFile(parent, "pingmanager", ".tmp");
            ecrireForce(tmp, texte.getBytes(StandardCharsets.UTF_8));
            garderSecours(abs);
            deplacer(tmp, abs);
            tmp = null;
            forcerDossier(parent);
        } catch (IOException e) {
            throw new UncheckedIOException("Échec de la sauvegarde vers " + cible, e);
        } finally {
            if (tmp != null) {
                try {
                    Files.deleteIfExists(tmp);
                } catch (IOException ignore) {
                    // temporaire orphelin : ignoré, jamais relu
                }
            }
        }
    }

    /**
     * Relit un fichier JSON ; en cas de fichier principal illisible ou tronqué, se replie
     * sur la copie {@code .bak} et le signale.
     *
     * @throws UncheckedIOException ou {@link com.pingmanager.persistence.json.JsonException}
     *                              si ni le fichier ni sa copie ne sont lisibles
     */
    public static LectureJson lireJson(Path source) {
        RuntimeException erreur;
        try {
            return new LectureJson(analyser(source), false, null);
        } catch (IOException e) {
            erreur = new UncheckedIOException("Échec du chargement depuis " + source, e);
        } catch (RuntimeException e) {
            erreur = e;
        }
        Path bak = secours(source);
        if (Files.isRegularFile(bak)) {
            try {
                Json doc = analyser(bak);
                String message = "Le fichier " + source.getFileName() + " est illisible (" + erreur.getMessage()
                        + ") : la copie de secours " + bak.getFileName() + " a été relue. Les dernières "
                        + "modifications peuvent manquer.";
                AVERTISSEMENTS.add(message);
                System.err.println(message);
                return new LectureJson(doc, true, message);
            } catch (IOException | RuntimeException secoursIllisible) {
                // on lève l'erreur du fichier principal
            }
        }
        throw erreur;
    }

    /** @return les replis sur une copie de secours survenus depuis le démarrage (pour l'affichage). */
    public static List<String> avertissements() {
        synchronized (AVERTISSEMENTS) {
            return List.copyOf(AVERTISSEMENTS);
        }
    }

    private static Json analyser(Path f) throws IOException {
        FichierJson.verifierTaille(f);
        return JsonParser.analyser(Files.readString(f, StandardCharsets.UTF_8));
    }

    /** Garde la version actuelle en {@code .bak}, seulement si elle est lisible. */
    private static void garderSecours(Path cible) throws IOException {
        if (!Files.isRegularFile(cible)) {
            return;
        }
        byte[] actuel;
        try {
            FichierJson.verifierTaille(cible);
            actuel = Files.readAllBytes(cible);
            JsonParser.analyser(new String(actuel, StandardCharsets.UTF_8));
        } catch (IOException | RuntimeException abime) {
            return; // version actuelle abîmée : la bonne copie de secours est conservée
        }
        Path bak = secours(cible);
        Path tmp = Files.createTempFile(cible.getParent(), "pingmanager", ".tmp");
        try {
            ecrireForce(tmp, actuel);
            deplacer(tmp, bak);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    private static void ecrireForce(Path f, byte[] contenu) throws IOException {
        try (FileChannel c = FileChannel.open(f, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
            ByteBuffer b = ByteBuffer.wrap(contenu);
            while (b.hasRemaining()) {
                c.write(b);
            }
            c.force(true);
        }
    }

    private static void deplacer(Path source, Path cible) throws IOException {
        try {
            Files.move(source, cible, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source, cible, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            // Certains systèmes refusent ATOMIC_MOVE autrement : remplacement simple, toujours
            // après écriture complète du temporaire.
            Files.move(source, cible, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /** Force l'entrée de répertoire sur le disque (Linux, macOS) ; sans effet ailleurs. */
    private static void forcerDossier(Path dossier) {
        if (dossier == null) {
            return;
        }
        try (FileChannel c = FileChannel.open(dossier, StandardOpenOption.READ)) {
            c.force(true);
        } catch (IOException | RuntimeException nonPrisEnCharge) {
            // Windows : un dossier ne s'ouvre pas ainsi ; le déplacement reste atomique.
        }
    }
}
