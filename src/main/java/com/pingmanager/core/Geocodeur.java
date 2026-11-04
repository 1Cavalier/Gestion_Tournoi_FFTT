package com.pingmanager.core;

import com.pingmanager.model.AdresseTournoi;
import com.pingmanager.model.Coordonnees;

import java.util.Optional;

/**
 * Port de géocodage : position d'une adresse. L'implémentation de production
 * (API Adresse de l'État) vit hors du cœur ; le cœur fournit le repli
 * {@link #centroideDuDepartement()} (préfecture du département du code postal), qui
 * fonctionne sans réseau.
 */
@FunctionalInterface
public interface Geocodeur {

    /** @return la position de l'adresse, ou vide si elle est introuvable ou le service indisponible. */
    Optional<Coordonnees> geocoder(AdresseTournoi adresse);

    /** Repli hors ligne : la préfecture du département du code postal. */
    static Geocodeur centroideDuDepartement() {
        return a -> a == null ? Optional.empty()
                : Territoires.departementDuCodePostal(a.getCodePostal()).map(Territoires.Departement::prefecture);
    }

    /** Ce géocodeur, puis {@code repli} s'il ne trouve rien (ou échoue). */
    default Geocodeur ouSinon(Geocodeur repli) {
        return a -> {
            Optional<Coordonnees> c;
            try {
                c = geocoder(a);
            } catch (RuntimeException e) {
                c = Optional.empty();
            }
            return c.isPresent() ? c : repli.geocoder(a);
        };
    }

    /**
     * Renseigne les coordonnées d'une adresse si elles manquent (sans toucher à une
     * adresse déjà géocodée).
     *
     * @return l'adresse, géocodée si possible
     */
    default AdresseTournoi completer(AdresseTournoi a) {
        if (a == null || a.getCoordonnees() != null || !a.estComplete()) {
            return a;
        }
        return geocoder(a).map(a::avecCoordonnees).orElse(a);
    }
}
