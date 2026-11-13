package com.pingmanager.core;

import com.pingmanager.model.AccesClub;
import com.pingmanager.model.CompteUtilisateur;

import java.util.Collection;
import java.util.Optional;

/**
 * Point 16 du README : règle unique des droits du compte connecté, appelée par chaque
 * opération sensible (et non plus seulement déduite de la navigation).
 *
 * <ul>
 *   <li>tout exige un compte connecté dont l'email est vérifié (point 13) ;</li>
 *   <li>un club se désigne <b>uniquement par son numéro</b> : un tournoi sans numéro
 *       n'appartient à aucun club (plus de repli par nom, qui se falsifie) ;</li>
 *   <li>créer, modifier, supprimer, homologuer, gérer les comptes : un accès sur le
 *       club ({@link AccesClub}) ;</li>
 *   <li>la supervision (privilège honoré par l'appelant, mode démo seulement) peut
 *       modifier, homologuer et consulter les tournois de tous les clubs, mais ni créer,
 *       ni supprimer, ni gérer les comptes d'un club.</li>
 * </ul>
 */
public final class ControleDroits {

    private ControleDroits() {
    }

    /**
     * @param supervisionHonoree vrai si le privilège de supervision est honoré pour ce
     *                           compte (rôle porté ET mode démo)
     * @param numeroClub         numéro FFTT du club visé ({@code null} pour la supervision)
     * @return le refus, ou vide si l'opération est permise
     */
    public static Optional<DroitRefuseException> refus(CompteUtilisateur compte, Collection<AccesClub> acces,
                                                       boolean supervisionHonoree,
                                                       OperationProtegee operation, String numeroClub) {
        String club = numeroClub == null || numeroClub.isBlank() ? null : numeroClub.trim();
        if (compte == null) {
            return Optional.of(new DroitRefuseException(DroitRefuseException.Motif.NON_CONNECTE, operation, club));
        }
        if (!compte.isEmailVerifie()) {
            return Optional.of(new DroitRefuseException(DroitRefuseException.Motif.EMAIL_NON_VERIFIE, operation, club));
        }
        boolean supervision = supervisionHonoree && compte.estSupervision();
        if (operation == OperationProtegee.SUPERVISION) {
            return supervision ? Optional.empty() : Optional.of(new DroitRefuseException(
                    DroitRefuseException.Motif.SUPERVISION_REQUISE, operation, club));
        }
        if (supervision && (operation == OperationProtegee.MODIFIER_TOURNOI
                || operation == OperationProtegee.HOMOLOGUER_TOURNOI
                || operation == OperationProtegee.DONNEES_CLUB)) {
            return Optional.empty();
        }
        if (club == null) {
            return Optional.of(new DroitRefuseException(DroitRefuseException.Motif.CLUB_NON_DESIGNE, operation, null));
        }
        if (acces != null) {
            for (AccesClub a : acces) {
                if (club.equals(a.getNumeroClub().trim()) && a.getEmail().equalsIgnoreCase(compte.getEmail())) {
                    return Optional.empty();
                }
            }
        }
        return Optional.of(new DroitRefuseException(DroitRefuseException.Motif.AUCUN_DROIT_SUR_LE_CLUB, operation, club));
    }

    /** Lève {@link DroitRefuseException} si l'opération n'est pas permise. */
    public static void exiger(CompteUtilisateur compte, Collection<AccesClub> acces, boolean supervisionHonoree,
                              OperationProtegee operation, String numeroClub) {
        Optional<DroitRefuseException> r = refus(compte, acces, supervisionHonoree, operation, numeroClub);
        if (r.isPresent()) {
            throw r.get();
        }
    }
}
