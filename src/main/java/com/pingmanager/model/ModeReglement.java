package com.pingmanager.model;

/**
 * Mode de règlement de l'engagement d'un tableau, noté au pointage (B041).
 *
 * <p>Distinct de {@link Paiement.Mode}, qui décrit la plateforme de paiement en
 * ligne proposée par le club. Ici, c'est la façon dont <b>ce joueur</b> a réglé
 * <b>ce tableau</b>. Un paiement noté avant B041 (ancien fichier) n'a pas de
 * mode : {@link Tableau#modeReglement} rend alors {@code null}.</p>
 */
public enum ModeReglement {
    EN_LIGNE("En ligne (déjà réglé)", true),
    ESPECES("Espèces", false),
    CHEQUE("Chèque", false),
    AUTRE("Autre", false);

    private final String libelle;
    private final boolean enLigne;

    ModeReglement(String libelle, boolean enLigne) {
        this.libelle = libelle;
        this.enLigne = enLigne;
    }

    public String getLibelle() {
        return libelle;
    }

    /** @return vrai si l'engagement a été réglé en ligne avant le jour J (tarif « en ligne »). */
    public boolean estEnLigne() {
        return enLigne;
    }
}
