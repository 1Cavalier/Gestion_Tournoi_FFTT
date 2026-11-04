package com.pingmanager.model;

/**
 * Tarifs d'inscription à un {@link Tableau}, selon le mode de règlement.
 *
 * <p>Le coût peut différer entre une inscription réglée <b>en ligne</b> et une
 * inscription réglée <b>sur place</b>. À ce stade, seuls les deux montants sont
 * modélisés : le choix effectif du mode par le joueur et le traitement d'un
 * paiement en ligne réel (encaissement, transaction bancaire) relèvent d'une
 * étape ultérieure. L'espace est prévu ici mais la logique de paiement n'est pas
 * encore implémentée.</p>
 *
 * <p>Objet valeur immuable.</p>
 */
public final class TarifInscription {

    private final double montantEnLigne;
    private final double montantSurPlace;
    /** Inscription en ligne proposée (sinon seul le sur place compte). */
    private final boolean enLigneProposee;
    /** Inscription sur place proposée. */
    private final boolean surPlaceProposee;

    /** Tarif proposé en ligne et sur place. */
    public TarifInscription(double montantEnLigne, double montantSurPlace) {
        this(montantEnLigne, true, montantSurPlace, true);
    }

    /**
     * Tarif avec choix des canaux : en ligne et/ou sur place. Au moins un canal
     * doit être proposé ; le montant d'un canal fermé est ignoré (mis à 0).
     */
    public TarifInscription(double montantEnLigne, boolean enLigneProposee,
                            double montantSurPlace, boolean surPlaceProposee) {
        if (montantEnLigne < 0 || montantSurPlace < 0) {
            throw new IllegalArgumentException("Les montants d'inscription ne peuvent pas être négatifs.");
        }
        if (!enLigneProposee && !surPlaceProposee) {
            throw new IllegalArgumentException("Proposez au moins une inscription : en ligne ou sur place.");
        }
        this.enLigneProposee = enLigneProposee;
        this.surPlaceProposee = surPlaceProposee;
        this.montantEnLigne = enLigneProposee ? montantEnLigne : 0;
        this.montantSurPlace = surPlaceProposee ? montantSurPlace : 0;
    }

    public boolean isEnLigneProposee() {
        return enLigneProposee;
    }

    public boolean isSurPlaceProposee() {
        return surPlaceProposee;
    }


    /** Inscription gratuite. */
    public static TarifInscription gratuit() {
        return new TarifInscription(0, 0);
    }

    public double getMontantEnLigne() {
        return montantEnLigne;
    }

    public double getMontantSurPlace() {
        return montantSurPlace;
    }

    @Override
    public String toString() {
        if (!enLigneProposee) {
            return montantSurPlace + " € (sur place uniquement)";
        }
        if (!surPlaceProposee) {
            return montantEnLigne + " € (en ligne uniquement)";
        }
        if (montantEnLigne == montantSurPlace) {
            return montantEnLigne + " €";
        }
        return montantEnLigne + " € (en ligne) / " + montantSurPlace + " € (sur place)";
    }
}
