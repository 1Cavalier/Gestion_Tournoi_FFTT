package com.pingmanager.model.buvette;

/**
 * D'où vient un mouvement de stock de la buvette (B026).
 *
 * <p>Aujourd'hui tout est saisi à la main. Le jour où un joueur commandera
 * depuis l'application, une origine « commande » s'ajoutera ici : la sortie
 * passera par le même {@code ServiceStockBuvette#sortir}, avec en référence le
 * numéro de la commande retirée au comptoir. Rien de la vente n'existe encore.</p>
 */
public enum OrigineMouvement {
    /** Bouton d'entrée, de sortie ou recomptage à l'écran. */
    SAISIE_MANUELLE("Saisie à la main");

    private final String libelle;

    OrigineMouvement(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }

    /** Relecture tolérante : {@code null} si l'origine est inconnue (ligne écartée). */
    public static OrigineMouvement lire(String nom) {
        for (OrigineMouvement o : values()) {
            if (o.name().equals(nom)) {
                return o;
            }
        }
        return null;
    }
}
