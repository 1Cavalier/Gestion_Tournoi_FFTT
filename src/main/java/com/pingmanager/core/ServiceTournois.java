package com.pingmanager.core;

import com.pingmanager.model.AccesClub;
import com.pingmanager.model.Tournoi;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Objects;

/**
 * Point d'entrée <b>habilité</b> pour créer et piloter les tournois d'un club.
 *
 * <p>Cette fabrique fait le lien entre la brique « comptes / clubs » (brique A)
 * et la brique « cycle de vie du tournoi » (brique B) : elle garantit qu'un
 * tournoi n'est créé — et que ses transitions sensibles ne sont déclenchées —
 * que par une personne <b>habilitée pour le club concerné</b>.</p>
 *
 * <p>L'habilitation est matérialisée par un {@link AccesClub}, lequel n'est
 * produit que par une vérification d'email réussie
 * ({@link ServiceComptesOrganisateurs#confirmer}). Sa présence atteste donc le
 * droit d'agir pour ce club. Deux filets de sécurité :</p>
 * <ul>
 *   <li>à la <b>création</b>, le tournoi est rattaché au numéro de club porté par
 *       l'accès — impossible de créer un tournoi pour un club sans y être
 *       habilité ;</li>
 *   <li>aux <b>transitions</b> (homologuer / publier / annuler), l'accès est
 *       revérifié : il doit correspondre au club du tournoi. Un accès révoqué ou
 *       relatif à un autre club est rejeté, même si le tournoi existe déjà.</li>
 * </ul>
 *
 * <p>Le service délègue la mécanique de cycle de vie au
 * {@link ServiceCycleTournoi} : il n'ajoute que le contrôle d'habilitation, en
 * restant sans état et sans dépendance hors du {@code core}.</p>
 */
public final class ServiceTournois {

    private final ServiceCycleTournoi cycle;

    /**
     * @param cycle service de cycle de vie auquel déléguer les transitions
     */
    public ServiceTournois(ServiceCycleTournoi cycle) {
        this.cycle = Objects.requireNonNull(cycle, "cycle");
    }

    /**
     * Crée un tournoi pour le club de l'accès fourni, sur un ou plusieurs jours.
     * Le tournoi naît en brouillon, rattaché au numéro de club de l'accès.
     *
     * @param acces l'habilitation de l'organisateur (non nulle)
     * @param nom   nom du tournoi
     * @param jours jours du tournoi (au moins un)
     * @return le tournoi créé, en brouillon
     */
    public Tournoi creerTournoi(AccesClub acces, String nom, Collection<LocalDate> jours) {
        Objects.requireNonNull(acces, "acces");
        return new Tournoi(nom, acces.getNumeroClub(), jours);
    }

    /**
     * Crée un tournoi d'un seul jour pour le club de l'accès fourni.
     */
    public Tournoi creerTournoi(AccesClub acces, String nom, LocalDate jour) {
        Objects.requireNonNull(acces, "acces");
        Objects.requireNonNull(jour, "jour");
        return new Tournoi(nom, acces.getNumeroClub(), jour);
    }

    /**
     * Homologue un tournoi, après contrôle de l'habilitation.
     *
     * @param acces              habilitation de l'organisateur (doit couvrir le club du tournoi)
     * @param tournoi            le tournoi à homologuer
     * @param numeroHomologation numéro délivré par la FFTT
     * @return le résultat de la transition (détail des manques si dossier incomplet)
     * @throws SecurityException si l'accès ne couvre pas le club du tournoi
     */
    public ResultatTransition homologuer(AccesClub acces, Tournoi tournoi, String numeroHomologation) {
        exigerHabilitation(acces, tournoi);
        return cycle.homologuer(tournoi, numeroHomologation);
    }

    /**
     * Publie un tournoi (ouvre les inscriptions), après contrôle de l'habilitation.
     *
     * @throws SecurityException si l'accès ne couvre pas le club du tournoi
     */
    public ResultatTransition publier(AccesClub acces, Tournoi tournoi) {
        exigerHabilitation(acces, tournoi);
        return cycle.publier(tournoi);
    }

    /**
     * Annule un tournoi, après contrôle de l'habilitation.
     *
     * @throws SecurityException si l'accès ne couvre pas le club du tournoi
     */
    public void annuler(AccesClub acces, Tournoi tournoi) {
        exigerHabilitation(acces, tournoi);
        cycle.annuler(tournoi);
    }

    /**
     * Indique si un accès habilite à agir sur un tournoi donné, c'est-à-dire si
     * l'accès concerne le même club que celui du tournoi.
     *
     * @return {@code true} si l'accès couvre le club du tournoi
     */
    public boolean estHabilite(AccesClub acces, Tournoi tournoi) {
        Objects.requireNonNull(acces, "acces");
        Objects.requireNonNull(tournoi, "tournoi");
        return acces.getNumeroClub().equals(tournoi.getClubOrganisateur());
    }

    private void exigerHabilitation(AccesClub acces, Tournoi tournoi) {
        Objects.requireNonNull(acces, "acces");
        Objects.requireNonNull(tournoi, "tournoi");
        if (!estHabilite(acces, tournoi)) {
            throw new SecurityException(
                    "Habilitation insuffisante : l'accès concerne le club « " + acces.getNumeroClub()
                            + " » mais le tournoi relève du club « " + tournoi.getClubOrganisateur() + " ».");
        }
    }
}
