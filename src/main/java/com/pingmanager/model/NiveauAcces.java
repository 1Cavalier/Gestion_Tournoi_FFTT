package com.pingmanager.model;

/**
 * Niveau d'habilitation d'une personne sur un club (porté par {@link AccesClub}).
 *
 * <p>Deux niveaux pour l'instant :</p>
 * <ul>
 *   <li>{@link #ADMIN_NIVEAU_1} — administrateur de premier niveau : la personne
 *       a prouvé son rattachement au club (code reçu à l'adresse officielle) et
 *       peut organiser des tournois pour ce club.</li>
 *   <li>{@link #ADMIN_PRINCIPAL} — administrateur principal : cas particulier de
 *       la personne référente du club au sens FFTT. Elle dispose des droits les
 *       plus élevés (gestion des autres administrateurs, à venir).</li>
 * </ul>
 */
public enum NiveauAcces {

    ADMIN_NIVEAU_1("Administrateur"),
    ADMIN_PRINCIPAL("Administrateur principal");

    private final String libelle;

    NiveauAcces(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
