package com.pingmanager.persistence;

import com.pingmanager.persistence.json.Json;
import com.pingmanager.persistence.json.JsonWriter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Point 21 du README (RGPD) : journal de ce qui a été purgé ou anonymisé, <b>sans
 * donnée personnelle</b> (ni nom, ni licence, ni email : seulement l'action, l'heure,
 * le nom du tournoi et des nombres). Écrit avec {@link EcritureSure}, sous
 * {@link VerrouFichier}.
 */
public final class JournalDonneesPersonnelles {

    /** Nom du fichier, dans le dossier de données. */
    public static final String NOM_FICHIER = "pingmanager-journal-rgpd.json";

    /** Une ligne du journal. */
    public record Entree(Instant instant, String action, String detail) {
    }

    private final Path fichier;

    public JournalDonneesPersonnelles(Path dossierDonnees) {
        this.fichier = dossierDonnees.resolve(NOM_FICHIER);
    }

    public Path fichier() {
        return fichier;
    }

    /** Ajoute une ligne (relecture, ajout, écriture sûre, sous verrou). */
    public void ajouter(Instant instant, String action, String detail) {
        VerrouFichier.executer(fichier, () -> {
            List<Entree> entrees = new ArrayList<>(lire());
            entrees.add(new Entree(instant, action, detail));
            Json tab = Json.tableau();
            for (Entree e : entrees) {
                tab.add(Json.objet().set("instant", e.instant().toString()).set("action", e.action())
                        .set("detail", e.detail()));
            }
            EcritureSure.ecrire(fichier, JsonWriter.ecrire(Json.objet().set("version", 1).set("entrees", tab), true));
        });
    }

    /** @return les lignes du journal, de la plus ancienne à la plus récente (vide si aucun). */
    public List<Entree> lire() {
        if (!Files.exists(fichier) && !Files.exists(EcritureSure.secours(fichier))) {
            return List.of();
        }
        Json doc = EcritureSure.lireJson(fichier).document();
        List<Entree> res = new ArrayList<>();
        if (doc.has("entrees")) {
            for (Json o : doc.get("entrees").elements()) {
                res.add(new Entree(Instant.parse(o.get("instant").asString()), o.getString("action", ""),
                        o.getString("detail", "")));
            }
        }
        return res;
    }
}
