package com.pingmanager.core;

import com.pingmanager.model.Club;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Implémentation en mémoire de {@link RegistreClubs}, indexée par numéro de club.
 *
 * <p>Elle ne réalise aucun effet de bord (c'est une simple {@link Map}) et sert
 * à amorcer le référentiel « à la main » tant que la source fédérale n'est pas
 * branchée, ainsi qu'aux tests. Les clubs y sont ajoutés via
 * {@link #ajouter(Club)}.</p>
 */
public final class RegistreClubsMemoire implements RegistreClubs {

    private final Map<String, Club> clubsParNumero = new LinkedHashMap<>();

    /**
     * Ajoute (ou remplace) un club dans le référentiel.
     *
     * @param club le club à enregistrer
     * @return {@code this}, pour chaîner les ajouts
     */
    public RegistreClubsMemoire ajouter(Club club) {
        Objects.requireNonNull(club, "club");
        clubsParNumero.put(normaliser(club.getNumero()), club);
        return this;
    }

    @Override
    public Optional<Club> trouverParNumero(String numeroClub) {
        if (numeroClub == null || numeroClub.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(clubsParNumero.get(normaliser(numeroClub)));
    }

    /** @return le nombre de clubs enregistrés. */
    public int taille() {
        return clubsParNumero.size();
    }

    private static String normaliser(String numero) {
        return numero.trim();
    }
}
