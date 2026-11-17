package com.pingmanager.core.buvette;

import java.util.List;

/**
 * Résultat de la copie d'articles du catalogue de base dans les articles du
 * club (B026).
 *
 * @param ajoutes      noms des articles ajoutés (contenants entraînés compris)
 * @param dejaPresents noms des articles cochés que le club avait déjà
 */
public record ResultatAjoutCatalogue(List<String> ajoutes, List<String> dejaPresents) {

    public ResultatAjoutCatalogue {
        ajoutes = List.copyOf(ajoutes);
        dejaPresents = List.copyOf(dejaPresents);
    }

    /** @return vrai si au moins un article a été ajouté. */
    public boolean accepte() {
        return !ajoutes.isEmpty();
    }

    /** Phrase à afficher. */
    public String explication() {
        StringBuilder sb = new StringBuilder();
        if (ajoutes.isEmpty()) {
            sb.append(dejaPresents.isEmpty() ? "Cochez au moins un article du catalogue."
                    : "Rien à ajouter : ces articles sont déjà dans vos articles.");
        } else {
            sb.append(ajoutes.size() == 1 ? "1 article ajouté : " : ajoutes.size() + " articles ajoutés : ")
                    .append(String.join(", ", ajoutes))
                    .append(". Stock à 0 : entrez ce que vous avez, puis adaptez prix et coûts.");
        }
        if (!ajoutes.isEmpty() && !dejaPresents.isEmpty()) {
            sb.append(" Déjà dans vos articles : ").append(String.join(", ", dejaPresents)).append('.');
        }
        return sb.toString();
    }
}
