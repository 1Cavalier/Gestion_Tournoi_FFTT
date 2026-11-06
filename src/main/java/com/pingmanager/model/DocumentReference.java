package com.pingmanager.model;

import java.util.Objects;

/**
 * Référence vers un document joint au tournoi (règlement, feuille d'engagement),
 * sans en porter le contenu binaire.
 *
 * <p>Le modèle ne stocke qu'un <b>libellé</b> et un <b>emplacement</b> (chemin
 * local ou URL) : le fichier lui-même est géré en dehors du cœur, ce qui garde
 * le {@code model} pur (aucune I/O). À terme, une brique pourra générer un
 * règlement officiel automatiquement ; en attendant, l'organisateur peut fournir
 * son propre PDF, dont on conserve ici la référence.</p>
 *
 * <p>Value-class immuable.</p>
 */
public final class DocumentReference {

    private final String libelle;
    private final String emplacement;

    /**
     * @param libelle     libellé lisible du document (ex. « Règlement du tournoi »)
     * @param emplacement chemin ou URL du fichier (non vide)
     */
    public DocumentReference(String libelle, String emplacement) {
        this.libelle = Textes.optionnel(libelle);
        this.emplacement = Textes.obligatoire(emplacement, "emplacement");
    }

    public String getLibelle() {
        return libelle;
    }

    public String getEmplacement() {
        return emplacement;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DocumentReference autre)) {
            return false;
        }
        return emplacement.equals(autre.emplacement)
                && Objects.equals(libelle, autre.libelle);
    }

    @Override
    public int hashCode() {
        return Objects.hash(libelle, emplacement);
    }

    @Override
    public String toString() {
        return (libelle != null ? libelle + " " : "") + "(" + emplacement + ")";
    }
}
