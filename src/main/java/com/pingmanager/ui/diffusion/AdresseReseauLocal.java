package com.pingmanager.ui.diffusion;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Adresse du réseau local (Wi-Fi de la salle) sur laquelle écoute le suivi
 * sur téléphone : on n'écoute jamais sur toutes les interfaces.
 */
public final class AdresseReseauLocal {

    private AdresseReseauLocal() {
    }

    /**
     * Choisit l'adresse d'écoute parmi les adresses des interfaces actives :
     * uniquement une adresse IPv4 privée (10/8, 172.16/12, 192.168/16). Une
     * adresse publique n'est jamais retenue : le suivi serait alors exposé à
     * Internet. Vide si aucune ne convient (pas de réseau local).
     */
    public static Optional<InetAddress> choisir(List<InetAddress> candidates) {
        for (InetAddress a : candidates) {
            if (estPriveeIpv4(a)) {
                return Optional.of(a);
            }
        }
        return Optional.empty();
    }

    /** Vrai pour une adresse IPv4 privée (10/8, 172.16/12, 192.168/16, RFC 1918). */
    static boolean estPriveeIpv4(InetAddress a) {
        // isSiteLocalAddress() couvre exactement ces trois plages en IPv4 ;
        // bouclage, lien local (169.254), multidiffusion et 0.0.0.0 en sont exclus.
        return a instanceof Inet4Address && a.isSiteLocalAddress();
    }

    /** Adresse du réseau local de cet ordinateur, ou vide s'il n'est relié à aucun réseau. */
    public static Optional<InetAddress> detecter() {
        List<InetAddress> candidates = new ArrayList<>();
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (ni.isUp() && !ni.isLoopback() && !ni.isVirtual() && !ni.isPointToPoint()) {
                    candidates.addAll(Collections.list(ni.getInetAddresses()));
                }
            }
        } catch (SocketException | RuntimeException e) {
            return Optional.empty();
        }
        return choisir(candidates);
    }
}
