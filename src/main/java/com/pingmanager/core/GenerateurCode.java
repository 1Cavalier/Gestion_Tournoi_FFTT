package com.pingmanager.core;

/**
 * Port : génère un code de vérification (à usage unique) envoyé à l'organisateur.
 *
 * <p>Interface d'abstraction (au sens de l'architecture hexagonale) : le cœur
 * définit le <i>besoin</i> — obtenir un code — sans connaître la façon de le
 * produire. Une implémentation concrète (aléatoire sécurisée, séquentielle pour
 * les tests…) sera fournie hors du cœur, ce qui préserve le découplage et la
 * testabilité du {@code core}.</p>
 */
@FunctionalInterface
public interface GenerateurCode {

    /**
     * @return un nouveau code de vérification, non vide (typiquement 6 chiffres).
     */
    String genererCode();
}
