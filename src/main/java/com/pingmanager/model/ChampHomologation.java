package com.pingmanager.model;

/**
 * Champs du dossier d'homologation (grille 🔴 de
 * {@code docs/passation/REGLES_CREATION_TOURNOI.md}) : ils figurent sur la
 * demande transmise à la FFTT et sont donc <b>verrouillés</b> dès la demande
 * d'homologation (voir {@link Tournoi#champsHomologationVerrouilles()}).
 *
 * <p>Les autres champs (numéro d'homologation, tables, balles, tarifs, places,
 * règlement…) restent modifiables : le numéro d'homologation, en particulier,
 * se saisit justement pendant la demande.</p>
 */
public enum ChampHomologation {

    NOM("nom du tournoi"),
    CATEGORIE("catégorie du tournoi"),
    CLUB_ORGANISATEUR("club organisateur"),
    NUMERO_AFFILIATION("n° d'affiliation du club"),
    ADRESSE("adresse du tournoi (voie, code postal, ville)"),
    DATES("dates de début et de fin"),
    CORRESPONDANT("correspondant (nom, prénom, email)"),
    JUGES_ARBITRES("juges-arbitres (nom, prénom, grade, rôle)"),
    LISTE_TABLEAUX("liste des tableaux (ajout, retrait)"),
    TABLEAU_NOM("nom d'un tableau"),
    TABLEAU_CATEGORIE_POINTS("catégorie de points d'un tableau"),
    TABLEAU_PUBLIC("public d'un tableau (dames, messieurs, mixte)"),
    TABLEAU_DEBUT("jour et heure de début d'un tableau");

    private final String libelle;

    ChampHomologation(String libelle) {
        this.libelle = libelle;
    }

    /** @return le libellé en clair, pour les messages et l'écran. */
    public String getLibelle() {
        return libelle;
    }
}
