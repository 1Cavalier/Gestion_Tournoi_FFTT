package com.pingmanager.core;

/**
 * Port : hachage et vérification d'un mot de passe.
 *
 * <p>Le cœur ne connaît jamais l'algorithme employé (BCrypt, Argon2…) : il
 * délègue la transformation d'un mot de passe en clair vers une empreinte, et
 * la vérification d'un mot de passe contre une empreinte. L'implémentation
 * concrète est fournie hors du {@code core}.</p>
 *
 * <p>Le mot de passe en clair ne transite que le temps de l'appel ; seul le
 * résultat ({@link #hacher(String)}) est conservé par le modèle
 * ({@link com.pingmanager.model.CompteOrganisateur#getEmpreinteMotDePasse()}).</p>
 */
public interface HachageMotDePasse {

    /**
     * @param motDePasseEnClair le mot de passe saisi (non vide)
     * @return l'empreinte à stocker
     */
    String hacher(String motDePasseEnClair);

    /**
     * @param motDePasseEnClair le mot de passe saisi
     * @param empreinte         l'empreinte précédemment stockée
     * @return {@code true} si le mot de passe correspond à l'empreinte
     */
    boolean verifier(String motDePasseEnClair, String empreinte);
}
