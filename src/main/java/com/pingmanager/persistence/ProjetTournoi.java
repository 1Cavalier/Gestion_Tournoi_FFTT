package com.pingmanager.persistence;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entrée de la liste des « tournois récents » : un fichier de tournoi déjà
 * ouvert, avec de quoi le ré-afficher sans le charger (nom lisible et date de
 * dernière ouverture).
 *
 * <p>Type immuable, identifié par le chemin absolu de son fichier. Sert
 * uniquement à l'affichage d'un menu « récemment ouverts » ; le contenu réel du
 * tournoi n'est chargé qu'à la demande, via {@code ServiceFichierTournoi}.</p>
 */
public final class ProjetTournoi {

    private final Path fichier;
    private final String nom;
    private final LocalDateTime derniereOuverture;

    public ProjetTournoi(Path fichier, String nom, LocalDateTime derniereOuverture) {
        this.fichier = Objects.requireNonNull(fichier, "fichier").toAbsolutePath();
        this.nom = nom == null || nom.isBlank() ? this.fichier.getFileName().toString() : nom;
        this.derniereOuverture = Objects.requireNonNull(derniereOuverture, "derniereOuverture");
    }

    public Path getFichier() {
        return fichier;
    }

    public String getNom() {
        return nom;
    }

    public LocalDateTime getDerniereOuverture() {
        return derniereOuverture;
    }

    /** Deux entrées sont la même si elles désignent le même fichier (chemin absolu). */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProjetTournoi autre)) {
            return false;
        }
        return fichier.equals(autre.fichier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fichier);
    }

    @Override
    public String toString() {
        return nom + " — " + fichier;
    }
}
