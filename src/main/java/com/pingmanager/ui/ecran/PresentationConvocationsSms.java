package com.pingmanager.ui.ecran;

import com.pingmanager.core.NumeroPortable;
import com.pingmanager.core.TexteSms;
import com.pingmanager.model.AffectationArbitre;
import com.pingmanager.model.ContactSms;
import com.pingmanager.model.ConvocationsSms;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Match;
import com.pingmanager.model.MessageSms;
import com.pingmanager.model.Poule;
import com.pingmanager.model.StatutEnvoiSms;
import com.pingmanager.model.StatutMatch;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tour;
import com.pingmanager.model.Tournoi;
import com.pingmanager.ui.infra.EnvoiSmsSimule;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

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

    /** Tuile de chiffre clé en tête d'écran. */
    record Tuile(String valeur, String libelle) {
    }

    /** Nombre de lignes du journal affichées (les plus récentes). */
    static final int JOURNAL_AFFICHE = 50;

    static final String EXPEDITEUR_REFUSE = "Expéditeur : 3 à 11 lettres ou chiffres, sans espace ni accent, avec "
            + "au moins une lettre (le nom du club de préférence).";

    private PresentationConvocationsSms() {
    }

    // ------------------------------------------------------------------
    // Tuiles et réglages
    // ------------------------------------------------------------------

    /** Envoyés / limite, échecs et refus, numéros enregistrés. */
    static List<Tuile> tuiles(ConvocationsSms c) {
        int echecs = nombre(c, StatutEnvoiSms.ECHEC) + nombre(c, StatutEnvoiSms.LIMITE_ATTEINTE);
        int numeros = c.getContacts().size();
        return List.of(
                new Tuile(c.nombreEnvoyes() + " / " + c.getLimiteEnvois(), "SMS envoyés / limite du tournoi"),
                new Tuile(String.valueOf(echecs), echecs > 1 ? "non envoyés (échec ou limite)"
                        : "non envoyé (échec ou limite)"),
                new Tuile(String.valueOf(numeros), numeros > 1 ? "numéros avec accord" : "numéro avec accord"));
    }

    static int nombre(ConvocationsSms c, StatutEnvoiSms statut) {
        int n = 0;
        for (MessageSms m : c.getJournal()) {
            if (m.statut() == statut) {
                n++;
            }
        }
        return n;
    }

    /** Alerte quand la limite est atteinte ou proche (90 %), sinon vide. */
    static Optional<String> alerteLimite(ConvocationsSms c) {
        int n = c.nombreEnvoyes();
        int l = c.getLimiteEnvois();
        if (n >= l) {
            return Optional.of("Limite atteinte : plus aucun SMS ne part. Relevez la limite dans les réglages si besoin.");
        }
        if (l > 0 && n * 10 >= l * 9) {
            return Optional.of("Limite bientôt atteinte : encore " + (l - n) + (l - n > 1 ? " SMS possibles." : " SMS possible."));
        }
        return Optional.empty();
    }

    /** Aide de la case « envoi automatique », avec l'annonce choisie pour le tournoi. */
    static String aideEnvoiAutomatique(Tournoi t) {
        String annonce = t.getModeAnnonce() == null ? "demandée à chaque lancement"
                : "« " + t.getModeAnnonce().getLibelle() + " »";
        return "Seulement si l'annonce choisie comprend le SMS (« SMS » ou « SMS et appel vocal »). "
                + "Annonce actuelle : " + annonce + ".";
    }

    /** Limite saisie, ou vide si elle n'est pas un nombre entre 0 et la limite maximale. */
    static Optional<Integer> limite(String saisie) {
        try {
            int l = Integer.parseInt(saisie == null ? "" : saisie.trim());
            return l < 0 || l > ConvocationsSms.LIMITE_MAXIMALE ? Optional.empty() : Optional.of(l);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    static String limiteRefusee() {
        return "Limite d'envois : un nombre entre 0 et " + ConvocationsSms.LIMITE_MAXIMALE + ".";
    }

    /** Demande de confirmation de l'effacement manuel de tous les numéros. */
    static String confirmationEffacement(int n) {
        return "Effacer " + (n > 1 ? "les " + n + " numéros" : "le numéro") + " ? Plus aucun SMS ne partira vers "
                + (n > 1 ? "ces personnes" : "cette personne") + ". Le journal des envois reste (sans numéro).";
    }

    // ------------------------------------------------------------------
    // Journal
    // ------------------------------------------------------------------

    /** Messages du journal à afficher : les plus récents d'abord, au plus {@link #JOURNAL_AFFICHE}. */
    static List<MessageSms> journalAffiche(ConvocationsSms c) {
        List<MessageSms> j = c.getJournal();
        List<MessageSms> res = new ArrayList<>();
        for (int i = j.size() - 1; i >= 0 && res.size() < JOURNAL_AFFICHE; i--) {
            res.add(j.get(i));
        }
        return res;
    }

    /** « … et 12 envois plus anciens (gardés dans le fichier du tournoi). », ou vide. */
    static Optional<String> resteJournal(ConvocationsSms c) {
        int reste = c.getJournal().size() - JOURNAL_AFFICHE;
        return reste <= 0 ? Optional.empty() : Optional.of("… et " + reste + (reste > 1 ? " envois plus anciens"
                : " envoi plus ancien") + " (gardés dans le fichier du tournoi).");
    }

    /** Destinataire d'une ligne du journal. */
    static String destinataire(MessageSms m) {
        return m.destinataire().isEmpty() ? "Licence " + m.numeroLicence() : m.destinataire();
    }

    /** « 14/11 10:02 · Match de poule ». */
    static String detailMessage(MessageSms m) {
        return HEURE.format(m.instant()) + " · " + m.type().getLibelle();
    }

    /** Pastille du statut : envoyé (succès), échec (erreur), limite atteinte (attention). */
    static StatutPastille pastille(StatutEnvoiSms s) {
        if (s == StatutEnvoiSms.ENVOYE) {
            return StatutPastille.SUCCES;
        }
        if (s == StatutEnvoiSms.ECHEC) {
            return StatutPastille.ERREUR;
        }
        return StatutPastille.ATTENTION;
    }

    /** Motif d'échec affiché sous le texte ; l'identifiant d'un envoi réussi reste discret (vide). */
    static Optional<String> motif(MessageSms m) {
        if (m.statut() == StatutEnvoiSms.ENVOYE || m.detail().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of("Motif : " + m.detail() + ".");
    }

    /** « 142 caractères · 1 SMS ». */
    static String tailleTexte(String texte) {
        int n = TexteSms.nombreDeSms(texte);
        return TexteSms.longueur(texte) + " caractères · " + n + " SMS";
    }

    /** En-tête d'un SMS simulé : « De TTBRUNOY à +33 6 •• •• •• 78 · 14/11 10:02 ». */
    static String enteteSimule(EnvoiSmsSimule.Message m) {
        return "De " + m.expediteur() + " à " + m.numeroMasque() + " · " + HEURE.format(m.instant());
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
