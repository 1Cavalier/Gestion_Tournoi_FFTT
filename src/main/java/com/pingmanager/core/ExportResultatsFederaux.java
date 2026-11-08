package com.pingmanager.core;

import java.util.List;

/**
 * Port : export des résultats d'un tournoi vers la fédération (B023).
 *
 * <p>Le format officiel de remontée des résultats d'un tournoi homologué n'est
 * pas public : il est « donné lors de l'homologation » (IX.107 des règlements
 * sportifs 2026-2027) et passe aujourd'hui par SPIDD. Ce port isole le cœur de
 * ce format : l'implémentation provisoire produit des CSV documentés
 * ({@code importation.ExportResultatsCsv}) ; un adaptateur au format fédéral
 * pourra la remplacer sans toucher au cœur.</p>
 *
 * <p>{@link #produire} ne fait <b>aucune écriture disque</b> : elle renvoie des
 * fichiers en mémoire, que l'interface enregistre où l'utilisateur le choisit.</p>
 */
public interface ExportResultatsFederaux {

    /** @return le nom lisible du format (ex. « CSV provisoire PingManager v1 »). */
    String nomFormat();

    /**
     * @param resultats résultats préparés par {@link ServiceResultatsFederaux}
     * @return les fichiers à enregistrer (jamais {@code null})
     */
    List<FichierExporte> produire(ResultatsTournoi resultats);
}
