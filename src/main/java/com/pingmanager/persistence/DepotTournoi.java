package com.pingmanager.persistence;

import com.pingmanager.core.RegistreDisciplinaire;
import com.pingmanager.model.Tournoi;
import com.pingmanager.persistence.json.Json;
import com.pingmanager.persistence.json.JsonWriter;

import java.nio.file.Path;

/**
 * Point d'entrée de la persistance « document » : sauvegarder et recharger un
 * tournoi complet (et son registre disciplinaire) dans un fichier local.
 *
 * <p>Conçu pour fonctionner totalement hors ligne, sur le disque de la machine
 * de gestion en salle. L'écriture est <b>sûre</b> ({@link EcritureSure}) : le
 * contenu est d'abord écrit dans un fichier temporaire voisin, forcé sur le disque,
 * la version précédente est gardée en {@code .bak}, puis le temporaire est renommé
 * par-dessus la cible. Une coupure de courant en pleine écriture ne peut donc
 * pas corrompre la sauvegarde précédente ; un fichier abîmé est relu depuis sa
 * copie de secours.</p>
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
     * Écrit un texte dans un fichier de façon sûre (point 21) : délègue à
     * {@link EcritureSure#ecrire} (temporaire forcé sur disque, copie {@code .bak},
     * déplacement atomique).
     */
    static void ecrireAtomique(Path cible, String texte) {
        EcritureSure.ecrire(cible, texte);
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
        // Point 21 : fichier illisible ou tronqué → copie de secours « .bak », signalée.
        return TournoiMapper.lire(EcritureSure.lireJson(source).document());
    }
}
