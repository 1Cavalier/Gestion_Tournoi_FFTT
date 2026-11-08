package com.pingmanager.core;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Résultats d'un tournoi prêts à exporter (objet neutre, sans format de
 * fichier), produits par {@link ServiceResultatsFederaux#construire}.
 *
 * @param numeroHomologation numéro d'homologation (vide s'il n'est pas encore connu)
 * @param nomTournoi         nom du tournoi
 * @param dateDebut          premier jour
 * @param dateFin            dernier jour
 * @param jugeArbitreNom     juge-arbitre principal, « Prénom Nom » (vide si non désigné)
 * @param jugeArbitreLicence licence du juge-arbitre principal (vide si inconnue)
 * @param parties            parties jouées des tableaux exportés
 * @param classements        classements finaux des tableaux exportés
 * @param horodatage         instant de préparation de l'export
 */
public record ResultatsTournoi(String numeroHomologation, String nomTournoi, LocalDate dateDebut,
                               LocalDate dateFin, String jugeArbitreNom, String jugeArbitreLicence,
                               List<PartieExportee> parties, List<ClassementExporte> classements,
                               Instant horodatage) {

    public ResultatsTournoi {
        numeroHomologation = numeroHomologation == null ? "" : numeroHomologation.trim();
        nomTournoi = Objects.requireNonNull(nomTournoi, "nomTournoi");
        jugeArbitreNom = jugeArbitreNom == null ? "" : jugeArbitreNom;
        jugeArbitreLicence = jugeArbitreLicence == null ? "" : jugeArbitreLicence;
        parties = parties == null ? List.of() : List.copyOf(parties);
        classements = classements == null ? List.of() : List.copyOf(classements);
        Objects.requireNonNull(horodatage, "horodatage");
    }
}
