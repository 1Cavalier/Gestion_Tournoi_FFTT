package com.pingmanager.ui.ecran;

import com.pingmanager.core.NumeroPortable;
import com.pingmanager.model.AffectationArbitre;
import com.pingmanager.model.ContactSms;
import com.pingmanager.model.ConvocationsSms;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Match;
import com.pingmanager.model.MessageSms;
import com.pingmanager.model.Poule;
import com.pingmanager.model.StatutMatch;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tour;
import com.pingmanager.model.Tournoi;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Textes et listes de l'écran des convocations par SMS (B019), sans JavaFX
 * pour être testés.
 */
final class PresentationConvocationsSms {

    private static final DateTimeFormatter HEURE = DateTimeFormatter.ofPattern("dd/MM HH:mm", Locale.FRANCE);

    /** Personne qui peut recevoir une convocation : joueur inscrit ou arbitre. */
    record Personne(String licence, String prenom, String nomComplet, boolean arbitre) {
        @Override
        public String toString() {
            return nomComplet + (arbitre ? " (arbitre)" : "") + " · " + licence;
        }
    }

    private PresentationConvocationsSms() {
    }

    /** Joueurs inscrits (tous tableaux) puis arbitres, une fois chacun, triés par nom. */
    static List<Personne> personnes(Tournoi t) {
        Map<String, Personne> parLicence = new LinkedHashMap<>();
        for (Tableau tab : t.getTableaux()) {
            for (Joueur j : tab.getInscrits()) {
                if (!j.estADeterminer()) {
                    parLicence.putIfAbsent(j.getNumeroLicence(),
                            new Personne(j.getNumeroLicence(), j.getPrenom(), j.getNomComplet(), false));
                }
            }
        }
        for (AffectationArbitre a : t.getArbitres()) {
            parLicence.putIfAbsent(a.getNumeroLicence(),
                    new Personne(a.getNumeroLicence(), a.getPrenom(), a.getPrenom() + " " + a.getNom(), true));
        }
        List<Personne> liste = new ArrayList<>(parLicence.values());
        liste.sort(Comparator.comparing(p -> p.nomComplet().toLowerCase(Locale.ROOT)));
        return liste;
    }

    /** Nom d'une licence pour la liste des numéros (la licence seule si la personne n'est plus au tournoi). */
    static String nomDe(List<Personne> personnes, String licence) {
        for (Personne p : personnes) {
            if (p.licence().equals(licence)) {
                return p.nomComplet();
            }
        }
        return "Licence " + licence;
    }

    /** « +33 6 •• •• •• 78 · accord du 01/11 18:30 ». */
    static String detailContact(ContactSms c) {
        return NumeroPortable.masquer(c.numero()) + " · accord du " + HEURE.format(c.consentementLe());
    }

    /** « 12 SMS envoyés sur 300 autorisés ». */
    static String compteur(ConvocationsSms c) {
        int n = c.nombreEnvoyes();
        return n + (n > 1 ? " SMS envoyés" : " SMS envoyé") + " sur " + c.getLimiteEnvois() + " autorisés";
    }

    /** Ligne de journal : « 14/11 10:02 · Match de poule · Hugo MORIN · Envoyé ». */
    static String enteteMessage(MessageSms m) {
        return HEURE.format(m.instant()) + " · " + m.type().getLibelle() + " · "
                + (m.destinataire().isEmpty() ? "Licence " + m.numeroLicence() : m.destinataire()) + " · "
                + m.statut().getLibelle() + (m.detail().isEmpty() ? "" : " (" + m.detail() + ")");
    }

    /** Matchs en cours sur une table, par numéro de table : ceux qu'on peut convoquer à la main. */
    static List<Match> matchsSurTable(Tournoi t) {
        List<Match> liste = new ArrayList<>();
        for (Tableau tab : t.getTableaux()) {
            for (Poule p : tab.getPoules()) {
                ajouterEnCours(p.getMatchs(), liste);
            }
            if (tab.aPhaseFinale()) {
                for (Tour tour : tab.getPhaseFinale().getTours()) {
                    ajouterEnCours(tour.getMatchs(), liste);
                }
            }
        }
        liste.sort(Comparator.comparing(Match::getNumeroTable));
        return liste;
    }

    private static void ajouterEnCours(List<Match> matchs, List<Match> liste) {
        for (Match m : matchs) {
            if (m.getStatut() == StatutMatch.EN_COURS && m.getNumeroTable() != null) {
                liste.add(m);
            }
        }
    }

    /** « Table 4 · Hugo MORIN – Léa DURAND ». */
    static String libelleMatch(Match m) {
        return "Table " + m.getNumeroTable() + " · " + m.getJoueurA().getNomComplet() + " – "
                + m.getJoueurB().getNomComplet();
    }
}
