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
     * d'abord une adresse IPv4 privée (192.168.x.x, 10.x.x.x, 172.16-31.x.x),
     * puis toute autre IPv4 qui n'est ni de bouclage ni locale au lien. Vide si
     * aucune ne convient (pas de réseau local).
     */
    public static Optional<InetAddress> choisir(List<InetAddress> candidates) {
        InetAddress repli = null;
        for (InetAddress a : candidates) {
            if (!(a instanceof Inet4Address) || a.isLoopbackAddress() || a.isLinkLocalAddress()
                    || a.isAnyLocalAddress() || a.isMulticastAddress()) {
                continue;
            }
            if (a.isSiteLocalAddress()) {
                return Optional.of(a);
            }
            if (repli == null) {
                repli = a;
            }
        }
        return Optional.ofNullable(repli);
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
