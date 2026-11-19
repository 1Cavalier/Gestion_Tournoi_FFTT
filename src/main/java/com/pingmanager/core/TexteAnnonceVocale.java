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
        Morceaux m = new Morceaux();
        if (contexte != null) {
            m.libre(contexte.libelle() + ". ");
        }
        joueur(m, match.getJoueurA());
        m.libre(", contre ");
        joueur(m, match.getJoueurB());
        m.libre(", table " + table + ".");
        return new AnnonceVocale(match, table, m.segments, false, m.licences);
    }

    /**
     * Annonce d'une poule lancée en bloc : « Tableau B, poule 3, table 5 : Pierre
     * Dupont, club TT Brunoy, Paul Martin… » (tous les joueurs, dans l'ordre de la
     * poule). L'annonce est rattachée au premier match de la poule.
     */
    public static AnnonceVocale composerPoule(String tableau, com.pingmanager.model.Poule poule, int table) {
        Objects.requireNonNull(poule, "poule");
        if (poule.getMatchs().isEmpty()) {
            throw new IllegalArgumentException("Poule sans match : rien à annoncer.");
        }
        Morceaux m = new Morceaux();
        m.libre(entetePoule(tableau, poule, table) + " : ");
        List<Joueur> joueurs = poule.getJoueurs();
        for (int i = 0; i < joueurs.size(); i++) {
            if (i > 0) {
                m.libre(i == joueurs.size() - 1 ? " et " : " ; ");
            }
            joueur(m, joueurs.get(i));
        }
        m.libre(".");
        return new AnnonceVocale(poule.getMatchs().get(0), table, m.segments, false, m.licences);
    }

    /** « Tableau B, poule 3, table 5 » (sans répéter « Tableau » si le nom le contient). */
    public static String entetePoule(String tableau, com.pingmanager.model.Poule poule, int table) {
        String t = tableau == null ? "" : tableau.trim();
        String debut = t.isEmpty() ? "" : (t.toLowerCase(Locale.ROOT).startsWith("tableau") ? t : "Tableau " + t) + ", ";
        return debut + "poule " + poule.getNom() + ", table " + table;
    }

    /** Morceaux de l'annonce et, pour chacun, la licence de son joueur (B018). */
    private static final class Morceaux {
        final List<SegmentAnnonce> segments = new ArrayList<>();
        final List<String> licences = new ArrayList<>();

        void libre(String texte) {
            segments.add(SegmentAnnonce.libre(texte));
            licences.add("");
        }

        void du(Joueur j, String texte, SegmentAnnonce.Nature nature) {
            segments.add(new SegmentAnnonce(texte, nature));
            licences.add(j.getNumeroLicence());
        }
    }

    private static void joueur(Morceaux m, Joueur j) {
        if (j == null || j.estADeterminer()) {
            m.libre("joueur à déterminer");
            return;
        }
        String prenom = propre(j.getPrenom());
        String nom = nomPropre(propre(j.getNom()));
        if (!prenom.isEmpty()) {
            m.du(j, prenom, SegmentAnnonce.Nature.PRENOM);
            m.libre(" ");
        }
        m.du(j, nom, SegmentAnnonce.Nature.NOM);
        String club = propre(j.getClub());
        if (!club.isEmpty()) {
            boolean dejaClub = club.toLowerCase(Locale.ROOT).startsWith("club ");
            m.libre(dejaClub ? ", " : ", club ");
            m.du(j, club, SegmentAnnonce.Nature.CLUB);
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
    public static String nomPropre(String nom) {
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
