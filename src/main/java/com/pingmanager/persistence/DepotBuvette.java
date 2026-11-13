package com.pingmanager.persistence;

import com.pingmanager.model.buvette.BuvetteTournoi;
import com.pingmanager.persistence.json.Json;
import com.pingmanager.persistence.json.JsonException;
import com.pingmanager.persistence.json.JsonWriter;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Buvette d'un tournoi (B046) : stock, budget et ventes, enregistrés dans un
 * <b>fichier voisin</b> du fichier du tournoi ({@code X.ptt.json} →
 * {@code X.buvette.json}).
 *
 * <p>Pourquoi un fichier à part : le fichier du tournoi est réécrit en entier par
 * les écrans du juge-arbitre et du pointage ; une caisse qui écrirait dans le
 * même fichier risquerait d'effacer leurs saisies (ou l'inverse). Le fichier du
 * tournoi n'est donc jamais touché par la buvette, et un ancien tournoi sans
 * buvette se relit exactement comme avant. Choix réversible (README, section 6).</p>
 *
 * <p>Écriture sûre ({@link EcritureSure}), relecture tolérante (lignes abîmées
 * écartées) ; un fichier absent donne une buvette vide.</p>
 */
public final class DepotBuvette {

    /** Version du format du fichier de buvette. */
    public static final int VERSION = 1;

    /** Extension du fichier voisin. */
    public static final String EXTENSION = ".buvette.json";

    private DepotBuvette() {
    }

    /** Fichier de buvette voisin d'un fichier de tournoi. */
    public static Path fichierPour(Path fichierTournoi) {
        String nom = fichierTournoi.getFileName().toString();
        String base = nom.endsWith(DepotTournoi.EXTENSION)
                ? nom.substring(0, nom.length() - DepotTournoi.EXTENSION.length())
                : nom;
        return fichierTournoi.resolveSibling(base + EXTENSION);
    }

    /** @return la buvette du tournoi, vide si elle n'a jamais été enregistrée. */
    public static BuvetteTournoi charger(Path fichierTournoi) {
        Path f = fichierPour(fichierTournoi);
        if (!Files.exists(f) && !Files.exists(EcritureSure.secours(f))) {
            return new BuvetteTournoi();
        }
        return lire(EcritureSure.lireJson(f).document());
    }

    /** Enregistre la buvette du tournoi (écriture atomique). */
    public static void sauvegarder(BuvetteTournoi buvette, Path fichierTournoi) {
        EcritureSure.ecrire(fichierPour(fichierTournoi), JsonWriter.ecrire(ecrire(buvette), true));
    }

    static Json ecrire(BuvetteTournoi buvette) {
        Json doc = BuvetteMapper.ecrireBuvetteTournoi(buvette);
        Json entete = Json.objet();
        entete.set("version", VERSION);
        entete.set("kind", "buvette");
        doc.entrees().forEach(entete::set);
        return entete;
    }

    static BuvetteTournoi lire(Json doc) {
        if (doc.getInt("version", 1) > VERSION) {
            throw new JsonException("Fichier de buvette d'une version plus récente (" + doc.getInt("version", 1)
                    + ") que celle supportée (" + VERSION + ").");
        }
        return BuvetteMapper.lireBuvetteTournoi(doc);
    }
}
