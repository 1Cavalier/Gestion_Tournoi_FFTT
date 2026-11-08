package com.pingmanager.core;

import java.util.List;
import java.util.Optional;

/**
 * Document SSML d'une annonce vocale (B017), lu par la synthèse vocale du
 * système.
 *
 * <p>Tout texte venant des données (noms, clubs, tableau) est échappé XML ; les
 * caractères interdits en XML 1.0 (caractères de contrôle) sont remplacés par
 * une espace. Une prononciation personnalisée ({@link ResolveurPrononciation},
 * B018) devient une balise {@code <phoneme alphabet="ipa" ph="…">texte</phoneme>}.
 * L'annonce est dite plusieurs fois à la suite, séparée par un silence.</p>
 */
public final class GenerateurSsml {

    /** Silence entre deux diffusions d'une même annonce. */
    static final String SILENCE_ENTRE_DIFFUSIONS = "<break time=\"1500ms\"/>";

    private GenerateurSsml() {
    }

    /**
     * @param diffusions nombre de fois que l'annonce est dite (au moins 1)
     */
    public static String generer(AnnonceVocale annonce, int diffusions, ResolveurPrononciation resolveur) {
        ResolveurPrononciation r = resolveur == null ? ResolveurPrononciation.TEXTE_NORMAL : resolveur;
        StringBuilder phrase = new StringBuilder();
        List<SegmentAnnonce> segments = annonce.segments();
        for (int i = 0; i < segments.size(); i++) {
            SegmentAnnonce s = segments.get(i);
            Optional<String> ipa = s.nature() == SegmentAnnonce.Nature.LIBRE ? Optional.empty()
                    : resoudre(r, s, annonce.licenceDu(i));
            morceau(phrase, s.texte(), ipa);
        }
        StringBuilder sb = new StringBuilder().append(ENTETE);
        for (int i = 0; i < Math.max(1, diffusions); i++) {
            if (i > 0) {
                sb.append(SILENCE_ENTRE_DIFFUSIONS);
            }
            sb.append("<s>").append(phrase).append("</s>");
        }
        return sb.append("</speak>").toString();
    }

    /**
     * Essai d'une prononciation (B018, bouton « Écouter ») : les morceaux dits
     * une fois, chacun avec sa prononciation si elle est valide
     * ({@link ValidationPrononciation}), sinon en texte normal.
     */
    public static String essai(List<Morceau> morceaux) {
        StringBuilder phrase = new StringBuilder();
        for (Morceau m : morceaux) {
            if (phrase.length() > 0) {
                phrase.append(' ');
            }
            ResultatSaisiePrononciation v = ValidationPrononciation.verifier(m.ipa());
            morceau(phrase, m.texte(), v.valide() && !v.ipa().isEmpty() ? Optional.of(v.ipa()) : Optional.empty());
        }
        return ENTETE + "<s>" + phrase + "</s></speak>";
    }

    /**
     * Morceau d'un essai de prononciation.
     *
     * @param texte texte écrit (prénom, nom, club)
     * @param ipa   prononciation en API, ou vide / null
     */
    public record Morceau(String texte, String ipa) {
    }

    private static final String ENTETE =
            "<speak version=\"1.0\" xmlns=\"http://www.w3.org/2001/10/synthesis\" xml:lang=\"fr-FR\">";

    private static void morceau(StringBuilder phrase, String texte, Optional<String> ipa) {
        if (ipa.isPresent()) {
            phrase.append("<phoneme alphabet=\"ipa\" ph=\"").append(echapperXml(ipa.get())).append("\">")
                    .append(echapperXml(texte)).append("</phoneme>");
        } else {
            phrase.append(echapperXml(texte));
        }
    }

    private static Optional<String> resoudre(ResolveurPrononciation r, SegmentAnnonce s, String licence) {
        try {
            Optional<String> ipa = r.phonemesIpa(s, licence);
            return ipa == null ? Optional.empty() : ipa.filter(p -> !p.isBlank()).map(String::strip);
        } catch (RuntimeException e) {
            // Une prononciation défaillante ne doit jamais empêcher l'annonce : texte normal.
            return Optional.empty();
        }
    }

    /**
     * Échappe un texte pour un contenu ou un attribut XML : {@code & < > " '} ;
     * les caractères interdits en XML 1.0 deviennent une espace.
     */
    public static String echapperXml(String texte) {
        if (texte == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(texte.length() + 16);
        for (int i = 0; i < texte.length(); i++) {
            char c = texte.charAt(i);
            if (c == '&') {
                sb.append("&amp;");
            } else if (c == '<') {
                sb.append("&lt;");
            } else if (c == '>') {
                sb.append("&gt;");
            } else if (c == '"') {
                sb.append("&quot;");
            } else if (c == '\'') {
                sb.append("&apos;");
            } else if (Character.isHighSurrogate(c) && i + 1 < texte.length()
                    && Character.isLowSurrogate(texte.charAt(i + 1))) {
                sb.append(c).append(texte.charAt(++i));
            } else if (autorise(c)) {
                sb.append(c);
            } else {
                sb.append(' ');
            }
        }
        return sb.toString();
    }

    private static boolean autorise(char c) {
        if (Character.isSurrogate(c)) {
            return false; // moitié de paire isolée
        }
        return c == '\t' || c == '\n' || c == '\r' || (c >= 0x20 && c <= 0xD7FF) || (c >= 0xE000 && c <= 0xFFFD);
    }
}
