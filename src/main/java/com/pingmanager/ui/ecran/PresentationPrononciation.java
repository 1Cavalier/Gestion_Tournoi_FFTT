package com.pingmanager.ui.ecran;

import com.pingmanager.core.SyntheseVocale.ResultatSynthese;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.PrononciationJoueur;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Contenu des écrans de prononciation phonétique (B018) : aide à la saisie
 * (symboles courants du français, exemples), insertion d'un symbole dans le
 * champ, messages d'écoute, liste des joueurs du tournoi. Sans JavaFX, testé
 * seul.
 */
final class PresentationPrononciation {

    private PresentationPrononciation() {
    }

    static final String TITRE_JOUR_J = "Prononciation des noms";
    static final String AIDE_SAISIE = "Écrivez le nom tel qu'il se prononce, en alphabet phonétique international "
            + "(API) : cliquez sur un symbole pour l'insérer, séparez les syllabes par un point. Laissez vide pour "
            + "que la voix lise le nom écrit.";
    static final String AIDE_PROFIL_SANS_LICENCE = "Reliez votre licence FFTT (à droite) pour indiquer comment "
            + "prononcer votre nom lors des appels au micro.";
    static final String AIDE_JOUR_J = "Pour un nom mal prononcé par la voix : choisissez le joueur, écrivez sa "
            + "prononciation, écoutez, enregistrez. Gardée pour les tournois suivants.";
    static final String CLUB_SANS_NUMERO = "Numéro du club inconnu (licence absente de la base) : la prononciation "
            + "du club ne peut pas être enregistrée.";

    /**
     * Symbole de l'alphabet phonétique et un mot où on l'entend.
     *
     * @param symbole symbole inséré au clic
     * @param exemple « an » de « enfant »…
     */
    record Symbole(String symbole, String exemple) {
        String infobulle() {
            return symbole + " comme dans « " + exemple + " »";
        }
    }

    /** Symboles courants du français (voyelles nasales et sons difficiles d'abord). */
    static final List<Symbole> SYMBOLES = List.of(
            new Symbole("ɑ̃", "enfant"), new Symbole("ɛ̃", "vin"), new Symbole("ɔ̃", "bon"),
            new Symbole("œ̃", "brun"), new Symbole("ʁ", "rue"), new Symbole("ʃ", "chat"),
            new Symbole("ʒ", "je"), new Symbole("ɲ", "agneau"), new Symbole("ŋ", "parking"),
            new Symbole("ɥ", "huit"), new Symbole("j", "yeux"), new Symbole("w", "oui"),
            new Symbole("e", "été"), new Symbole("ɛ", "lait"), new Symbole("ə", "le"),
            new Symbole("ø", "deux"), new Symbole("œ", "peur"), new Symbole("o", "eau"),
            new Symbole("ɔ", "port"), new Symbole("u", "ou"), new Symbole("y", "tu"),
            new Symbole("ɡ", "gare"), new Symbole(".", "syllabe : sé.pa.ré"));

    /**
     * Exemple de prononciation.
     *
     * @param ecrit nom écrit
     * @param ipa   sa prononciation
     */
    record Exemple(String ecrit, String ipa) {
        String libelle() {
            return ecrit + " → " + ipa;
        }
    }

    static final List<Exemple> EXEMPLES = List.of(
            new Exemple("Dupont", "dy.pɔ̃"), new Exemple("Jean", "ʒɑ̃"), new Exemple("Lefebvre", "lə.fɛvʁ"),
            new Exemple("Nguyen", "ŋɡu.jɛn"), new Exemple("Saint-Saëns", "sɛ̃.sɑ̃s"),
            new Exemple("Brunoy", "bʁy.nwa"));

    /**
     * Texte et position du curseur après insertion.
     *
     * @param texte  nouveau texte du champ
     * @param curseur position du curseur, juste après le symbole
     */
    record Insertion(String texte, int curseur) {
    }

    /** Insère le symbole à la place de la sélection (ou au curseur). */
    static Insertion inserer(String texte, int debut, int fin, String symbole) {
        String t = texte == null ? "" : texte;
        int a = Math.max(0, Math.min(Math.min(debut, fin), t.length()));
        int b = Math.max(a, Math.min(Math.max(debut, fin), t.length()));
        String s = symbole == null ? "" : symbole;
        return new Insertion(t.substring(0, a) + s + t.substring(b), a + s.length());
    }

    /** Message après « Écouter » : rien si la voix a parlé, sinon le repli. */
    static String messageEcoute(ResultatSynthese r) {
        if (r.dite()) {
            return "";
        }
        if (r.motif().contains("indisponible")) {
            return VOIX_INDISPONIBLE;
        }
        return "Écoute impossible : " + r.motif();
    }

    static final String VOIX_INDISPONIBLE = "Écoute impossible : la voix n'est disponible que sous Windows. "
            + "La prononciation peut quand même être enregistrée ; elle servira aux annonces.";

    /** Résumé affiché d'une prononciation : « kɑ̃.tɛ̃ su.mɛ », ou « Non renseignée ». */
    static String resume(PrononciationJoueur p) {
        if (p == null || p.estVide()) {
            return "Non renseignée";
        }
        return (p.prenom() + " " + p.nom()).strip();
    }

    /**
     * Joueurs d'un tournoi, une fois chacun (par licence), triés par nom puis
     * prénom, filtrés par le texte cherché (nom, prénom, licence, club).
     */
    static List<Joueur> joueurs(Collection<List<Joueur>> listes, String recherche) {
        Map<String, Joueur> parLicence = new LinkedHashMap<>();
        for (List<Joueur> l : listes) {
            for (Joueur j : l) {
                if (j != null && !j.estADeterminer()) {
                    parLicence.putIfAbsent(j.getNumeroLicence(), j);
                }
            }
        }
        String r = normaliser(recherche);
        List<Joueur> res = new ArrayList<>();
        for (Joueur j : parLicence.values()) {
            String cle = normaliser(j.getNom() + " " + j.getPrenom() + " " + j.getPrenom() + " " + j.getNom()
                    + " " + j.getNumeroLicence() + " " + j.getClub());
            if (r.isEmpty() || cle.contains(r)) {
                res.add(j);
            }
        }
        Collator c = Collator.getInstance(Locale.FRENCH);
        c.setStrength(Collator.PRIMARY);
        res.sort(Comparator.comparing((Joueur j) -> j.getNom(), c).thenComparing(j -> j.getPrenom(), c));
        return res;
    }

    private static String normaliser(String s) {
        if (s == null) {
            return "";
        }
        return java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
    }
}
