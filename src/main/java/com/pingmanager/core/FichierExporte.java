package com.pingmanager.core;

import java.util.Arrays;
import java.util.Objects;

/**
 * Fichier produit en mémoire par un port d'export ({@link ExportResultatsFederaux}).
 * Le contenu est copié à l'entrée et à la sortie : l'objet est immuable.
 */
public final class FichierExporte {

    private final String nom;
    private final byte[] contenu;
    private final String typeMime;

    /**
     * @param nom      nom de fichier simple, sans dossier (ex. {@code parties.csv})
     * @param contenu  octets du fichier
     * @param typeMime type MIME (ex. {@code text/csv})
     */
    public FichierExporte(String nom, byte[] contenu, String typeMime) {
        Objects.requireNonNull(nom, "nom");
        if (nom.isBlank() || nom.contains("/") || nom.contains("\\") || nom.contains("..")) {
            throw new IllegalArgumentException("Nom de fichier exporté invalide : " + nom);
        }
        this.nom = nom;
        this.contenu = Objects.requireNonNull(contenu, "contenu").clone();
        this.typeMime = Objects.requireNonNull(typeMime, "typeMime");
    }

    public String getNom() {
        return nom;
    }

    /** @return une copie des octets du fichier. */
    public byte[] getContenu() {
        return contenu.clone();
    }

    public String getTypeMime() {
        return typeMime;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof FichierExporte f && nom.equals(f.nom) && typeMime.equals(f.typeMime)
                && Arrays.equals(contenu, f.contenu);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nom, typeMime, Arrays.hashCode(contenu));
    }

    @Override
    public String toString() {
        return nom + " (" + contenu.length + " octets)";
    }
}
