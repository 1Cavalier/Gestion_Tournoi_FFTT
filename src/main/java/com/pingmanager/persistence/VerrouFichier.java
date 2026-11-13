package com.pingmanager.persistence;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Point 21 du README : verrou <b>court</b> sur un fichier de tournoi, le temps de
 * relire, modifier et écrire, pour que deux postes (pointage et juge-arbitre sur un
 * dossier partagé) ou deux fenêtres ne s'écrasent pas.
 *
 * <ul>
 *   <li>Verrou par fichier : un fichier voisin {@code <nom>.verrou} créé de façon
 *       exclusive, qui contient le nom de l'ordinateur et l'heure. Il marche aussi sur
 *       un dossier partagé, où les verrous du système ne sont pas fiables.</li>
 *   <li>Dans une même application, un verrou réentrant par fichier sert les fenêtres
 *       et les tâches : une écriture imbriquée dans une relecture-modification ne se
 *       bloque pas elle-même.</li>
 *   <li>La lecture n'est jamais bloquée.</li>
 *   <li>Un verrou abandonné (plantage) expire après {@link #EXPIRATION}.</li>
 *   <li>Après {@link #ESSAIS} essais, {@link FichierOccupeException} donne un message
 *       clair : rien n'est écrit, rien n'est perdu.</li>
 * </ul>
 */
public final class VerrouFichier {

    /** Suffixe du fichier de verrou. */
    public static final String SUFFIXE = ".verrou";
    /** Un verrou plus vieux que cela est considéré abandonné (une écriture dure quelques ms). */
    public static final Duration EXPIRATION = Duration.ofSeconds(30);
    /** Nombre d'essais avant d'abandonner. */
    public static final int ESSAIS = 25;
    /** Pause entre deux essais (25 × 120 ms = 3 s d'attente au plus). */
    public static final Duration PAUSE = Duration.ofMillis(120);

    private static final Map<Path, ReentrantLock> VERROUS_LOCAUX = new ConcurrentHashMap<>();
    private static final DateTimeFormatter HEURE = DateTimeFormatter.ofPattern("HH:mm:ss");

    private VerrouFichier() {
    }

    /** @return le fichier de verrou d'un fichier de tournoi. */
    public static Path cheminVerrou(Path fichier) {
        Path abs = fichier.toAbsolutePath().normalize();
        return abs.resolveSibling(abs.getFileName().toString() + SUFFIXE);
    }

    /** Exécute l'action sous le verrou du fichier (réglages par défaut). */
    public static <T> T executer(Path fichier, Supplier<T> action) {
        return executer(fichier, EXPIRATION, ESSAIS, PAUSE, action);
    }

    /** Variante sans résultat. */
    public static void executer(Path fichier, Runnable action) {
        executer(fichier, () -> {
            action.run();
            return null;
        });
    }

    /**
     * Exécute l'action sous le verrou du fichier.
     *
     * @throws FichierOccupeException si le verrou n'a pas pu être pris (l'action n'est pas lancée)
     */
    public static <T> T executer(Path fichier, Duration expiration, int essais, Duration pause, Supplier<T> action) {
        Objects.requireNonNull(action, "action");
        Path cle = fichier.toAbsolutePath().normalize();
        ReentrantLock local = VERROUS_LOCAUX.computeIfAbsent(cle, k -> new ReentrantLock());
        boolean pris;
        try {
            pris = local.tryLock(pause.toMillis() * Math.max(1, essais), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FichierOccupeException("Enregistrement interrompu : réessayez.");
        }
        if (!pris) {
            throw new FichierOccupeException("Le tournoi est en cours d'enregistrement dans une autre fenêtre : "
                    + "réessayez dans un instant. Rien n'a été modifié.");
        }
        try {
            if (local.getHoldCount() > 1) {
                return action.get(); // déjà détenu par ce fil : écriture imbriquée
            }
            Path verrou = cheminVerrou(cle);
            String jeton = prendre(verrou, expiration, essais, pause);
            try {
                return action.get();
            } finally {
                relacher(verrou, jeton);
            }
        } finally {
            local.unlock();
        }
    }

    private static String prendre(Path verrou, Duration expiration, int essais, Duration pause) {
        String jeton = UUID.randomUUID().toString();
        String contenu = "ordinateur=" + nomOrdinateur() + "\nheure=" + Instant.now() + "\njeton=" + jeton + "\n";
        String occupant = null;
        for (int i = 0; i < Math.max(1, essais); i++) {
            try {
                Files.createDirectories(verrou.getParent());
                Files.write(verrou, contenu.getBytes(StandardCharsets.UTF_8),
                        StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
                return jeton;
            } catch (FileAlreadyExistsException deja) {
                occupant = decrire(verrou);
                if (abandonne(verrou, expiration)) {
                    try {
                        Files.deleteIfExists(verrou);
                    } catch (IOException ignore) {
                        // un autre poste l'a peut-être déjà repris : nouvel essai
                    }
                    continue;
                }
            } catch (IOException e) {
                throw new FichierOccupeException("Verrou du tournoi impossible à créer (" + e.getMessage()
                        + ") : vérifiez l'accès au dossier. Rien n'a été modifié.");
            }
            dormir(pause);
        }
        throw new FichierOccupeException("Le tournoi est en cours de modification"
                + (occupant == null ? "" : " sur " + occupant) + " : réessayez dans un instant. Rien n'a été modifié.");
    }

    private static boolean abandonne(Path verrou, Duration expiration) {
        try {
            Instant date = Files.getLastModifiedTime(verrou).toInstant();
            return date.plus(expiration).isBefore(Instant.now());
        } catch (NoSuchFileException disparu) {
            return false; // relâché entre-temps : nouvel essai immédiat au tour suivant
        } catch (IOException e) {
            return false;
        }
    }

    /** « PC-ARBITRE (depuis 14:02:11) », lu dans le fichier de verrou. */
    private static String decrire(Path verrou) {
        try {
            String ordinateur = null;
            String heure = null;
            for (String ligne : Files.readAllLines(verrou, StandardCharsets.UTF_8)) {
                if (ligne.startsWith("ordinateur=")) {
                    ordinateur = ligne.substring("ordinateur=".length());
                } else if (ligne.startsWith("heure=")) {
                    heure = ligne.substring("heure=".length());
                }
            }
            String h = "";
            if (heure != null) {
                h = " (depuis " + LocalTime.ofInstant(Instant.parse(heure), ZoneId.systemDefault()).format(HEURE) + ")";
            }
            return (ordinateur == null || ordinateur.isBlank() ? "un autre poste" : ordinateur) + h;
        } catch (IOException | RuntimeException illisible) {
            return null;
        }
    }

    private static void relacher(Path verrou, String jeton) {
        try {
            String contenu = Files.readString(verrou, StandardCharsets.UTF_8);
            if (contenu.contains("jeton=" + jeton)) {
                Files.deleteIfExists(verrou);
            }
        } catch (IOException ignore) {
            // déjà retiré (expiré et repris ailleurs) : rien à faire
        }
    }

    private static void dormir(Duration pause) {
        try {
            Thread.sleep(Math.max(1, pause.toMillis()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FichierOccupeException("Enregistrement interrompu : réessayez.");
        }
    }

    static String nomOrdinateur() {
        for (String variable : new String[] {"COMPUTERNAME", "HOSTNAME"}) {
            String v = System.getenv(variable);
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (IOException | RuntimeException e) {
            return "poste inconnu";
        }
    }
}
