package com.pingmanager.ui.infra;

import com.pingmanager.core.HachageMotDePasse;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

/**
 * Implémentation concrète du port {@link HachageMotDePasse} avec <b>PBKDF2</b>
 * (algorithme du JDK, aucune dépendance externe).
 *
 * <p>Chaque empreinte embarque son propre sel aléatoire et le nombre
 * d'itérations, au format {@code pbkdf2$<iterations>$<sel base64>$<hash base64>}.
 * La vérification recalcule le hash avec le sel stocké et compare en temps
 * constant. C'est volontairement simple et autonome : suffisant pour l'appli de
 * bureau, remplaçable par Argon2/BCrypt le jour où l'on ajoutera une
 * dépendance.</p>
 */
public final class HachageMotDePassePBKDF2 implements HachageMotDePasse {

    private static final String ALGO = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 120_000;
    private static final int TAILLE_SEL = 16;   // octets
    private static final int TAILLE_CLE = 256;  // bits
    private static final String PREFIXE = "pbkdf2";

    private final SecureRandom alea = new SecureRandom();

    @Override
    public String hacher(String motDePasseEnClair) {
        if (motDePasseEnClair == null || motDePasseEnClair.isEmpty()) {
            throw new IllegalArgumentException("Le mot de passe ne peut pas être vide.");
        }
        byte[] sel = new byte[TAILLE_SEL];
        alea.nextBytes(sel);
        byte[] hash = calculer(motDePasseEnClair.toCharArray(), sel, ITERATIONS);
        Base64.Encoder enc = Base64.getEncoder().withoutPadding();
        return PREFIXE + "$" + ITERATIONS + "$" + enc.encodeToString(sel) + "$" + enc.encodeToString(hash);
    }

    @Override
    public boolean verifier(String motDePasseEnClair, String empreinte) {
        if (motDePasseEnClair == null || empreinte == null) {
            return false;
        }
        String[] parts = empreinte.split("\\$");
        if (parts.length != 4 || !PREFIXE.equals(parts[0])) {
            return false;
        }
        try {
            int iterations = Integer.parseInt(parts[1]);
            byte[] sel = Base64.getDecoder().decode(parts[2]);
            byte[] attendu = Base64.getDecoder().decode(parts[3]);
            byte[] calcule = calculer(motDePasseEnClair.toCharArray(), sel, iterations);
            return constantTimeEquals(attendu, calcule);
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static byte[] calculer(char[] motDePasse, byte[] sel, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(motDePasse, sel, iterations, TAILLE_CLE);
            SecretKeyFactory skf = SecretKeyFactory.getInstance(ALGO);
            return skf.generateSecret(spec).getEncoded();
        } catch (java.security.NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Hachage PBKDF2 indisponible : " + e.getMessage(), e);
        }
    }

    private static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a.length != b.length) {
            return false;
        }
        int diff = 0;
        for (int i = 0; i < a.length; i++) {
            diff |= a[i] ^ b[i];
        }
        return diff == 0;
    }
}
