package com.pingmanager.model;

/**
 * Rôle d'un {@link CompteUtilisateur}, enregistré dans le compte (et non plus déduit
 * de son email).
 *
 * <ul>
 *   <li>{@link #MEMBRE} : tout compte ordinaire (joueur, organisateur : les droits
 *       sur un club viennent des {@link AccesClub}, pas du rôle) ;</li>
 *   <li>{@link #SUPERVISION} : compte de supervision PingManager, qui voit les
 *       tournois de tous les clubs. Il n'est créé, et son privilège n'est honoré,
 *       qu'en mode démo explicite ({@code -Dpingmanager.demo=true}).</li>
 * </ul>
 */
public enum RoleCompte {
    MEMBRE,
    SUPERVISION
}
