package com.pingmanager.model.buvette;

/**
 * D'où vient une vente de la buvette (B026).
 *
 * <p>Aujourd'hui, seulement le comptoir. Le jour où un joueur commandera
 * depuis son application (B026a), une origine « commande en ligne »
 * s'ajoutera ici, avec en référence le numéro de la commande : la vente sera
 * enregistrée par le même service et sortira le stock de la même façon.</p>
 */
public enum OrigineVente {
    /** Saisie sur l'écran de comptoir par un bénévole. */
    COMPTOIR("Au comptoir");

    private final String libelle;

    OrigineVente(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }

    /** Relecture tolérante : {@code null} si l'origine est inconnue. */
    public static OrigineVente lire(String nom) {
        for (OrigineVente o : values()) {
            if (o.name().equals(nom)) {
                return o;
            }
        }
        return null;
    }
}
