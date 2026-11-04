package com.pingmanager.core;

import com.pingmanager.model.CompteUtilisateur;
import com.pingmanager.model.FicheLicence;

import java.util.Objects;
import java.util.Optional;

/**
 * Service de la <b>démarche 1</b> : lier le compte d'une personne à sa licence
 * FFTT.
 *
 * <p>La personne saisit son numéro de licence ; le service interroge le
 * référentiel fédéral ({@link RegistreLicences}). Si la licence existe, la
 * fiche est renvoyée (pour confirmation à l'écran) et le numéro est enregistré
 * sur le compte via {@link CompteUtilisateur#lierLicence(String)}. Sinon, la
 * recherche revient vide et rien n'est modifié.</p>
 *
 * <p>Sans état, adossé au seul port {@link RegistreLicences} : le cœur ne sait
 * pas d'où viennent les licences (base de test aujourd'hui, source FFTT
 * demain).</p>
 */
public final class ServiceLiaisonLicence {

    private final RegistreLicences registreLicences;

    public ServiceLiaisonLicence(RegistreLicences registreLicences) {
        this.registreLicences = Objects.requireNonNull(registreLicences, "registreLicences");
    }

    /**
     * Recherche la licence puis, si elle existe, la lie au compte.
     *
     * @param compte        le compte à rattacher
     * @param numeroLicence le numéro de licence saisi
     * @return la fiche de licence si la liaison a réussi ; {@link Optional#empty()}
     *         si aucune licence ne porte ce numéro (le compte reste inchangé)
     */
    public Optional<FicheLicence> lier(CompteUtilisateur compte, String numeroLicence) {
        Objects.requireNonNull(compte, "compte");
        Optional<FicheLicence> fiche = registreLicences.trouverParLicence(numeroLicence);
        fiche.ifPresent(f -> compte.lierLicence(f.getNumeroLicence()));
        return fiche;
    }
}
