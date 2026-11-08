package com.pingmanager.core;

import java.util.Objects;

/**
 * Issue de la vérification ou de l'enregistrement d'une prononciation
 * phonétique (B018).
 *
 * @param valide la saisie est-elle acceptée ?
 * @param ipa    prononciation normalisée (espaces réduites ; vide = effacée), vide si refusée
 * @param motif  pourquoi la saisie est refusée (vide si acceptée)
 */
public record ResultatSaisiePrononciation(boolean valide, String ipa, String motif) {

    public ResultatSaisiePrononciation {
        Objects.requireNonNull(ipa, "ipa");
        Objects.requireNonNull(motif, "motif");
    }

    static ResultatSaisiePrononciation acceptee(String ipa) {
        return new ResultatSaisiePrononciation(true, ipa, "");
    }

    static ResultatSaisiePrononciation refusee(String motif) {
        return new ResultatSaisiePrononciation(false, "", motif);
    }

    /** La saisie acceptée efface-t-elle la prononciation ? */
    public boolean estEffacement() {
        return valide && ipa.isEmpty();
    }
}
