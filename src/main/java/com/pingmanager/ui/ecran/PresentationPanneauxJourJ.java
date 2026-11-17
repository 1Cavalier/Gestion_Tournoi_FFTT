package com.pingmanager.ui.ecran;

import com.pingmanager.core.TexteConvocation;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.ModeAnnonce;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.StrategieOrdonnancement;
import com.pingmanager.model.Tournoi;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Présentation (sans JavaFX) des panneaux « Annonces » et « Stratégie » du jour
 * J (B056) : options de mode, blocs utilisés ou non, cartes de stratégie. Les
 * mots sont dans {@link TextesPanneauxJourJ} ; l'affichage dans
 * {@link PanneauAnnonces} et {@link PanneauStrategie}. Aucun réglage ni calcul
 * n'est changé : seul l'affichage est réorganisé.
 */
final class PresentationPanneauxJourJ {

    /** Ordre d'affichage des quatre modes : Voix, SMS, Voix + SMS, Aucune annonce. */
    static final List<ModeAnnonce> ORDRE_MODES =
            List.of(ModeAnnonce.APPEL, ModeAnnonce.SMS, ModeAnnonce.SMS_ET_APPEL, ModeAnnonce.AUCUNE);

    private static final DateTimeFormatter HEURE = DateTimeFormatter.ofPattern("HH:mm");

    private PresentationPanneauxJourJ() {
    }

    // ==================================================================
    // Annonces
    // ==================================================================

    /**
     * Une option de mode d'annonce.
     *
     * @param active    mode enregistré pour ce tournoi
     * @param remarques précisions à afficher sous la phrase (réglage à activer…), éventuellement vide
     */
    record OptionMode(ModeAnnonce mode, String nom, String phrase, boolean active, List<String> remarques) {
    }

    /** @return les quatre options, dans l'ordre {@link #ORDRE_MODES}. */
    static List<OptionMode> optionsMode(Tournoi t, boolean voixDisponible) {
        List<OptionMode> options = new ArrayList<>();
        for (ModeAnnonce m : ORDRE_MODES) {
            List<String> remarques = new ArrayList<>();
            if (m.avecAppel() && !t.getAnnoncesVocales().isAnnonceAutomatique()) {
                remarques.add(TextesPanneauxJourJ.MODE_VOIX_A_ACTIVER);
            } else if (m.avecAppel() && !voixDisponible) {
                remarques.add(TextesPanneauxJourJ.MODE_VOIX_ECRAN);
            }
            if (m.avecSms() && !t.getConvocationsSms().isEnvoiAutomatique()) {
                remarques.add(TextesPanneauxJourJ.MODE_SMS_A_ACTIVER);
            }
            options.add(new OptionMode(m, TextesPanneauxJourJ.nomMode(m), TextesPanneauxJourJ.phraseMode(m),
                    m == t.getModeAnnonce(), List.copyOf(remarques)));
        }
        return options;
    }

    /**
     * Le bloc « Voix » sert-il avec ce mode ? Oui pour Voix et Voix + SMS, et
     * quand aucun mode n'est choisi (la question est posée à chaque match).
     */
    static boolean voixUtilisee(ModeAnnonce mode) {
        return mode == null || mode.avecAppel();
    }

    /** Le bloc « SMS » sert-il avec ce mode ? Oui pour SMS et Voix + SMS, et sans mode choisi. */
    static boolean smsUtilise(ModeAnnonce mode) {
        return mode == null || mode.avecSms();
    }

    /** Exemple de SMS reçu par un joueur (joueurs fictifs, rien n'est envoyé). */
    static String exempleSms() {
        Joueur lucas = new Joueur("0000001", "MARTIN", "Lucas", Sexe.HOMME, "Brunoy", 1200);
        Joueur paul = new Joueur("0000002", "DURAND", "Paul", Sexe.HOMME, "Melun", 1100);
        return TexteConvocation.joueur(lucas, paul, 4, null);
    }

    /** Heure d'une annonce de l'historique : « 14:05 ». */
    static String heure(Instant instant, ZoneId zone) {
        return HEURE.format(instant.atZone(zone));
    }

    // ==================================================================
    // Stratégie
    // ==================================================================

    /**
     * Une carte de stratégie.
     *
     * @param active  stratégie qui s'applique (choisie ou par défaut)
     * @param choisie le club l'a choisie (sinon elle s'applique par défaut)
     */
    record CarteStrategie(StrategieOrdonnancement strategie, String nom, String typeTournoi, String tables,
                          String objectif, String enSalle, String avantage, String inconvenient, boolean active,
                          boolean choisie) {

        /** Pastille de la carte active, sinon {@code null}. */
        String marque() {
            if (!active) {
                return null;
            }
            return choisie ? TextesPanneauxJourJ.STRATEGIE_EN_PLACE : TextesPanneauxJourJ.STRATEGIE_PAR_DEFAUT;
        }

        /** Libellé du bouton de la carte. */
        String bouton() {
            if (!active) {
                return TextesPanneauxJourJ.STRATEGIE_CHOISIR;
            }
            return choisie ? TextesPanneauxJourJ.STRATEGIE_ACTIVE : TextesPanneauxJourJ.STRATEGIE_CONFIRMER_DEFAUT;
        }

        /** Le bouton agit-il ? Non sur la stratégie déjà choisie. */
        boolean boutonActif() {
            return !(active && choisie);
        }

        /** Faut-il confirmer avant d'appliquer ? Oui pour un vrai changement de stratégie. */
        boolean aConfirmer() {
            return !active;
        }
    }

    /** @return les trois cartes, dans l'ordre de l'énumération. */
    static List<CarteStrategie> cartesStrategie(Tournoi t) {
        StrategieOrdonnancement actuelle = t.getStrategieOrdonnancement();
        boolean choisie = t.strategieOrdonnancementChoisie();
        List<CarteStrategie> cartes = new ArrayList<>();
        for (StrategieOrdonnancement s : StrategieOrdonnancement.values()) {
            cartes.add(new CarteStrategie(s, s.getLibelle(), TextesPanneauxJourJ.typeTournoi(s),
                    TextesPanneauxJourJ.tables(s), TextesPanneauxJourJ.objectif(s), TextesPanneauxJourJ.enSalle(s),
                    TextesPanneauxJourJ.avantage(s), TextesPanneauxJourJ.inconvenient(s), s == actuelle,
                    s == actuelle && choisie));
        }
        return cartes;
    }
}
