package com.pingmanager.model.format;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Catalogue des {@link FormatTournoi} disponibles dans l'application.
 *
 * <p>Point unique où sont recensés les formats proposés. Ajouter un format au
 * logiciel se fait en deux temps : écrire sa classe, puis l'enregistrer ici.
 * L'interface pourra ainsi lister les formats, et la persistance retrouver un
 * format à partir de son identifiant.</p>
 *
 * <p>Actuellement, un seul format est disponible : « Poule de 3 — 2 qualifiés ».</p>
 */
public final class RegistreFormats {

    private static final List<FormatTournoi> FORMATS = List.of(
            FormatPoule3Qualifie2.INSTANCE
            // Les futurs formats (poule de 4, avec consolante, etc.) s'ajoutent ici.
    );

    private RegistreFormats() {
    }

    /** Tous les formats disponibles, dans l'ordre d'affichage. */
    public static List<FormatTournoi> tousLesFormats() {
        return FORMATS;
    }

    /** Le format retenu par défaut pour un nouveau tournoi. */
    public static FormatTournoi formatParDefaut() {
        return FormatPoule3Qualifie2.INSTANCE;
    }

    /**
     * Retrouve un format par son identifiant stable (ex. « P3Q2 »).
     *
     * @param identifiant l'identifiant recherché
     * @return le format correspondant, s'il existe
     */
    public static Optional<FormatTournoi> parIdentifiant(String identifiant) {
        Objects.requireNonNull(identifiant, "identifiant");
        return FORMATS.stream()
                .filter(f -> f.identifiant().equals(identifiant))
                .findFirst();
    }
}
