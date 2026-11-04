package com.pingmanager.persistence;

import com.pingmanager.core.RegistreDisciplinaire;
import com.pingmanager.model.Tournoi;
import com.pingmanager.persistence.json.Json;
import com.pingmanager.persistence.json.JsonParser;
import com.pingmanager.persistence.json.JsonWriter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Point d'entrée de la persistance « document » : sauvegarder et recharger un
 * tournoi complet (et son registre disciplinaire) dans un fichier local.
 *
 * <p>Conçu pour fonctionner totalement hors ligne, sur le disque de la machine
 * de gestion en salle. L'écriture est <b>atomique</b> : le contenu est d'abord
 * écrit dans un fichier temporaire voisin, vidé sur le disque, puis renommé
 * par-dessus la cible. Une coupure de courant en pleine écriture ne peut donc
 * pas corrompre la sauvegarde précédente — au pire, le fichier temporaire est
 * incomplet et sera ignoré.</p>
 *
 * <p>Le format est du JSON versionné, lisible et inspectable à la main en cas de
 * besoin. Aucune dépendance externe.</p>
 */
public final class DepotTournoi {

    /** Extension conseillée pour un document de tournoi PingManager. */
    public static final String EXTENSION = ".ptt.json";

    private DepotTournoi() {
    }

    // ------------------------------------------------------------------
    // Sauvegarde
    // ------------------------------------------------------------------

    /**
     * Sauvegarde un tournoi et sa discipline dans un fichier (écriture atomique).
     *
     * @param tournoi    le tournoi à sauvegarder
     * @param discipline le registre disciplinaire, ou {@code null}
     * @param cible      le fichier de destination
     */
    public static void sauvegarder(Tournoi tournoi, RegistreDisciplinaire discipline, Path cible) {
        Json doc = TournoiMapper.ecrireDocument(tournoi, discipline);
        String texte = JsonWriter.ecrire(doc, true);
        ecrireAtomique(cible, texte);
    }

    /** Sauvegarde un tournoi seul (sans registre disciplinaire). */
    public static void sauvegarder(Tournoi tournoi, Path cible) {
        sauvegarder(tournoi, null, cible);
    }

    /**
     * Écrit un texte dans un fichier de façon atomique : fichier temporaire +
     * flush disque + renommage. Garantit qu'on ne remplace jamais un fichier
     * valide par un contenu partiel.
     */
    static void ecrireAtomique(Path cible, String texte) {
        try {
            Path parent = cible.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Path tmp = Files.createTempFile(parent, "pingmanager", ".tmp");
            Files.writeString(tmp, texte, StandardCharsets.UTF_8);
            try {
                Files.move(tmp, cible,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomiqueImpossible) {
                // Certains systèmes de fichiers ne supportent pas ATOMIC_MOVE :
                // on retombe sur un remplacement simple (toujours après écriture
                // complète du temporaire).
                Files.move(tmp, cible, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Échec de la sauvegarde vers " + cible, e);
        }
    }

    // ------------------------------------------------------------------
    // Chargement
    // ------------------------------------------------------------------

    /**
     * Recharge un tournoi et sa discipline depuis un fichier.
     *
     * @param source le fichier à lire
     * @return le tournoi et son registre disciplinaire reconstruits
     */
    public static TournoiMapper.DocumentCharge charger(Path source) {
        String texte;
        try {
            texte = Files.readString(source, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Échec du chargement depuis " + source, e);
        }
        Json doc = JsonParser.analyser(texte);
        return TournoiMapper.lire(doc);
    }
}
