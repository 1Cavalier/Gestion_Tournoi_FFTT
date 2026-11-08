package com.pingmanager.core;

import com.pingmanager.model.Joueur;
import com.pingmanager.model.Match;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Texte d'une annonce vocale (B017) : « Hugo Morin, club TT Brunoy, contre Léa
 * Durand, club US Melun, table 4. », précédé au choix du tableau et du tour
 * (« Tableau C, poule B. »).
 *
 * <p>Les noms écrits en capitales (« MORIN ») sont remis en casse de nom propre
 * (« Morin ») pour que la voix les lise comme un mot et non lettre par lettre.
 * Le nom du club est gardé tel quel (« TT », « US » se lisent en sigles).</p>
 */
public final class TexteAnnonceVocale {

    private TexteAnnonceVocale() {
    }

    /**
     * @param contexte tableau et tour, ou null si inconnu ou non annoncé
     */
    public static AnnonceVocale composer(Match match, int table, ContexteConvocation contexte) {
        Objects.requireNonNull(match, "match");
        List<SegmentAnnonce> s = new ArrayList<>();
        if (contexte != null) {
            s.add(SegmentAnnonce.libre(contexte.libelle() + ". "));
        }
        joueur(s, match.getJoueurA());
        s.add(SegmentAnnonce.libre(", contre "));
        joueur(s, match.getJoueurB());
        s.add(SegmentAnnonce.libre(", table " + table + "."));
        return new AnnonceVocale(match, table, s, false);
    }

    private static void joueur(List<SegmentAnnonce> s, Joueur j) {
        if (j == null || j.estADeterminer()) {
            s.add(SegmentAnnonce.libre("joueur à déterminer"));
            return;
        }
        String prenom = propre(j.getPrenom());
        String nom = nomPropre(propre(j.getNom()));
        if (!prenom.isEmpty()) {
            s.add(new SegmentAnnonce(prenom, SegmentAnnonce.Nature.PRENOM));
            s.add(SegmentAnnonce.libre(" "));
        }
        s.add(new SegmentAnnonce(nom, SegmentAnnonce.Nature.NOM));
        String club = propre(j.getClub());
        if (!club.isEmpty()) {
            boolean dejaClub = club.toLowerCase(Locale.ROOT).startsWith("club ");
            s.add(SegmentAnnonce.libre(dejaClub ? ", " : ", club "));
            s.add(new SegmentAnnonce(club, SegmentAnnonce.Nature.CLUB));
        }
    }

    private static String propre(String s) {
        return s == null ? "" : s.trim().replaceAll("\\s+", " ");
    }

    /**
     * « MORIN » → « Morin », « DE LA FONTAINE » → « De La Fontaine »,
     * « LE-GALL » → « Le-Gall », « D'ARTOIS » → « D'Artois » ; un nom déjà en
     * minuscules est gardé tel quel.
     */
    static String nomPropre(String nom) {
        if (nom.isEmpty() || !nom.equals(nom.toUpperCase(Locale.FRENCH))) {
            return nom;
        }
        StringBuilder sb = new StringBuilder(nom.length());
        boolean debutMot = true;
        for (int i = 0; i < nom.length(); i++) {
            char c = nom.charAt(i);
            sb.append(debutMot ? Character.toUpperCase(c) : Character.toLowerCase(c));
            debutMot = c == ' ' || c == '-' || c == '\'' || c == '’';
        }
        return sb.toString();
    }
}
