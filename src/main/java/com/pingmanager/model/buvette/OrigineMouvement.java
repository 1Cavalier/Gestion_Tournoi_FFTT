package com.pingmanager.model.buvette;

/**
 * D'où vient un mouvement de stock de la buvette (B026).
 *
 * <p>Aujourd'hui, mouvements saisis à la main ou ventes du comptoir. Le jour où un joueur commandera
 * depuis l'application, une origine « commande » s'ajoutera ici : la sortie
 * passera par le même {@code ServiceStockBuvette#sortir}, avec en référence le
 * numéro de la commande retirée au comptoir. Les ventes du comptoir (mode
 * « Stock et ventes ») passent déjà par là, avec l'origine {@link #VENTE}.</p>
 */
public enum OrigineMouvement {
    /** Bouton d'entrée, de sortie ou recomptage à l'écran. */
    SAISIE_MANUELLE("Saisie à la main"),
    /** Vente enregistrée au comptoir (référence : « tournoi#numéro »). */
    VENTE("Vente"),
    /** Annulation d'une vente : le stock sorti est remis (même référence). */
    ANNULATION_VENTE("Annulation de vente");

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
