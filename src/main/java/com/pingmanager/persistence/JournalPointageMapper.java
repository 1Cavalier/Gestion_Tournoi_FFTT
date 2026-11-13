package com.pingmanager.persistence;

import com.pingmanager.model.OperationPointage;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.TypeOperationPointage;
import com.pingmanager.persistence.json.Json;

import java.time.Instant;

/**
 * Journal des postes de pointage en JSON (B041), champ facultatif
 * {@code journalPointage} du tournoi : écrit seulement s'il contient au moins
 * une opération ; un tournoi sans pointage garde exactement son ancien format.
 *
 * <pre>
 * "journalPointage": [ {"licence": "123", "tableau": "A", "type": "MARQUER_PAYE", "valeur": "ESPECES",
 *                       "horodatage": "2026-11-15T08:02:10Z", "poste": "accueil-1", "jour": "2026-11-15"} ]
 * </pre>
 *
 * <p>B043 : {@code jour} (jour du tableau) est facultatif, écrit seulement s'il est
 * connu ; un ancien fichier sans jour se relit comme avant (jour inconnu).</p>
 *
 * <p>Relecture tolérante : une ligne illisible (fichier abîmé ou plus récent)
 * est écartée, pour que le fichier s'ouvre toujours.</p>
 */
final class JournalPointageMapper {

    private JournalPointageMapper() {
    }

    static void ecrire(Tournoi t, Json o) {
        if (t.getJournalPointage().isEmpty()) {
            return;
        }
        Json journal = Json.tableau();
        for (OperationPointage op : t.getJournalPointage()) {
            Json j = Json.objet();
            j.set("licence", op.getLicence());
            j.set("tableau", op.getTableau());
            j.set("type", op.getType().name());
            if (op.getValeur() != null) {
                j.set("valeur", op.getValeur());
            }
            j.set("horodatage", op.getHorodatage().toString());
            j.set("poste", op.getPoste());
            if (op.getJour() != null) {
                j.set("jour", op.getJour().toString());
            }
            journal.add(j);
        }
        o.set("journalPointage", journal);
    }

    static void lire(Json o, Tournoi t) {
        if (!o.has("journalPointage") || !o.get("journalPointage").estTableau()) {
            return; // ancien fichier
        }
        for (Json j : o.get("journalPointage").elements()) {
            try {
                t.ajouterOperationPointage(OperationPointage.builder()
                        .licence(j.getString("licence", null))
                        .tableau(j.getString("tableau", null))
                        .type(TypeOperationPointage.valueOf(j.getString("type", "")))
                        .valeur(j.getString("valeur", null))
                        .horodatage(Instant.parse(j.getString("horodatage", "")))
                        .poste(j.getString("poste", null))
                        .jour(lireJour(j.getString("jour", null)))
                        .build());
            } catch (RuntimeException ligneIllisible) {
                // Ligne abîmée ou d'une version plus récente : écartée.
            }
        }
    }

    /** Jour illisible : l'opération est gardée, sans jour. */
    private static java.time.LocalDate lireJour(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return java.time.LocalDate.parse(s);
        } catch (RuntimeException illisible) {
            return null;
        }
    }
}
