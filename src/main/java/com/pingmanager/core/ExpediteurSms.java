package com.pingmanager.core;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Nom d'expéditeur alphanumérique des SMS (B019, voir {@code docs/RECHERCHE_SMS.md}).
 *
 * <p>Charte AF2M (depuis le 10 mars 2025) : 11 caractères au plus, lettres et
 * chiffres, pas uniquement des chiffres. Le nom doit identifier clairement
 * l'émetteur : on propose le <b>nom du club</b>, jamais « PINGTOURNOI » (marque
 * d'un logiciel concurrent).</p>
 */
public final class ExpediteurSms {

    /** Longueur maximale d'un expéditeur alphanumérique. */
    public static final int LONGUEUR_MAXIMALE = 11;
    /** Expéditeur de repli quand le nom du club ne donne rien d'utilisable. */
    public static final String PAR_DEFAUT = "PINGMANAGER";

    private ExpediteurSms() {
    }

    /** L'expéditeur respecte-t-il la charte (3 à 11 lettres ou chiffres, au moins une lettre) ? */
    public static boolean estValide(String expediteur) {
        return expediteur != null && expediteur.matches("[A-Za-z0-9]{3,11}") && expediteur.matches(".*[A-Za-z].*");
    }

    /**
     * Expéditeur proposé d'après le nom du club : sans accents, espaces ni
     * ponctuation, en majuscules, coupé à 11 caractères (« TT Brunoy » →
     * « TTBRUNOY »). {@link #PAR_DEFAUT} si le résultat n'est pas valide.
     */
    public static String proposer(String club) {
        if (club == null) {
            return PAR_DEFAUT;
        }
        String s = Normalizer.normalize(club, Normalizer.Form.NFD).replaceAll("[^A-Za-z0-9]", "")
                .toUpperCase(Locale.ROOT);
        if (s.length() > LONGUEUR_MAXIMALE) {
            s = s.substring(0, LONGUEUR_MAXIMALE);
        }
        return estValide(s) ? s : PAR_DEFAUT;
    }
}
