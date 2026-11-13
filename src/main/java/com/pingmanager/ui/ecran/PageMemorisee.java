package com.pingmanager.ui.ecran;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Optional;
import java.util.prefs.Preferences;

/**
 * B043 : la dernière page ouverte par un compte, rouverte à sa connexion suivante
 * (un poste de pointage qui tourne seul rouvre directement « Pointage »).
 *
 * <p>Stockée dans les préférences de l'utilisateur du système (JDK, sans
 * dépendance), sous une empreinte de l'email : l'email n'y est pas écrit en
 * clair. Toute erreur de lecture ou d'écriture est ignorée (on ouvre alors
 * l'accueil).</p>
 */
final class PageMemorisee {

    private static final String NOEUD = "/com/pingmanager/coquille";

    private PageMemorisee() {
    }

    static Optional<String> lire(String email) {
        try {
            return Optional.ofNullable(Preferences.userRoot().node(NOEUD).get(cle(email), null));
        } catch (RuntimeException | java.security.NoSuchAlgorithmException e) {
            return Optional.empty();
        }
    }

    static void ecrire(String email, String page) {
        try {
            Preferences.userRoot().node(NOEUD).put(cle(email), page);
        } catch (RuntimeException | java.security.NoSuchAlgorithmException e) {
            // Préférences indisponibles : la page ne sera simplement pas rouverte.
        }
    }

    private static String cle(String email) throws java.security.NoSuchAlgorithmException {
        byte[] h = java.security.MessageDigest.getInstance("SHA-256")
                .digest(String.valueOf(email).toLowerCase(java.util.Locale.ROOT).getBytes(StandardCharsets.UTF_8));
        return "page-" + HexFormat.of().formatHex(h, 0, 12);
    }
}
