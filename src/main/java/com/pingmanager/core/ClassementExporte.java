package com.pingmanager.core;

import java.util.Objects;

/**
 * Une ligne du classement final d'un tableau, dans un export de résultats.
 *
 * @param tableau nom du tableau
 * @param rang    rang (les ex aequo partagent le même rang)
 * @param joueur  joueur classé
 */
public record ClassementExporte(String tableau, int rang, JoueurExporte joueur) {

    public ClassementExporte {
        Objects.requireNonNull(tableau, "tableau");
        Objects.requireNonNull(joueur, "joueur");
        if (rang < 1) {
            throw new IllegalArgumentException("Rang invalide : " + rang);
        }
    }
}
