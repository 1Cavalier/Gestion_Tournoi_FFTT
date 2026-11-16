package com.pingmanager.ui.ecran;

import com.pingmanager.core.DroitRefuseException;
import com.pingmanager.model.StrategieOrdonnancement;
import com.pingmanager.model.Tournoi;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Présentation (sans JavaFX) du sélecteur de stratégie à trois choix
 * ({@link SelecteurStrategie}) : une option par stratégie avec son nom, une
 * phrase d'explication et sa marque, et l'enregistrement d'un choix.
 *
 * <p>Composant partagé : posé sur la vue du tournoi homologué ou publié, et
 * prévu tel quel pour l'écran du jour J (changer de stratégie en cours de
 * tournoi). L'explication complète et les statistiques restent sur l'écran
 * « Prévisions et stratégie » ({@link PresentationPrevisions}).</p>
 */
final class PresentationSelecteurStrategie {

    static final String TITRE = PresentationPrevisions.TITRE_CARTE;
    static final String AIDE = "La stratégie décide de l'ordre des matchs proposés en salle le jour J. "
            + "Vous pouvez en changer à tout moment.";
    static final String BOUTON_EN_SAVOIR_PLUS = "En savoir plus";
    static final String MARQUE_CHOISIE = PresentationPrevisions.MARQUE_CHOISIE;
    static final String MARQUE_PAR_DEFAUT = PresentationPrevisions.MARQUE_PAR_DEFAUT;

    private PresentationSelecteurStrategie() {
    }

    /**
     * Enregistre le tournoi après un changement de stratégie (fichier, autosave…).
     * Un refus ({@link DroitRefuseException}) annule le choix.
     */
    @FunctionalInterface
    interface Enregistreur {
        void enregistrer(Tournoi tournoi);
    }

    /**
     * Une option du sélecteur.
     *
     * @param active vrai pour la stratégie qui s'applique (choisie ou par défaut)
     * @param marque « Stratégie choisie », « Appliquée par défaut » tant que rien n'est choisi, sinon {@code null}
     */
    record Option(StrategieOrdonnancement strategie, String nom, String phrase, boolean active, String marque) {
    }

    /** Issue d'un choix : enregistré (ou inchangé) ou refusé, avec le message à afficher. */
    record ResultatChoixStrategie(boolean enregistre, boolean inchange, String message) {
    }

    /** @return les trois options, dans l'ordre de l'énumération, celle qui s'applique marquée. */
    static List<Option> options(Tournoi tournoi) {
        StrategieOrdonnancement actuelle = tournoi.getStrategieOrdonnancement();
        boolean choisie = tournoi.strategieOrdonnancementChoisie();
        List<Option> options = new ArrayList<>();
        for (StrategieOrdonnancement s : StrategieOrdonnancement.values()) {
            boolean active = s == actuelle;
            options.add(new Option(s, s.getLibelle(), phrase(s), active,
                    active ? (choisie ? MARQUE_CHOISIE : MARQUE_PAR_DEFAUT) : null));
        }
        return options;
    }

    /** @return une phrase d'explication courte (le détail est dans « En savoir plus »). */
    static String phrase(StrategieOrdonnancement s) {
        if (s == StrategieOrdonnancement.AVANCER_TABLEAUX_COMMENCES) {
            return "Chaque tableau avance le plus vite possible et joue sa finale dès qu'elle est prête.";
        }
        if (s == StrategieOrdonnancement.FAIRE_AVANCER_PETIT_A_PETIT) {
            return "Tous les tableaux avancent ensemble, tour par tour, et les finales sont gardées pour la fin.";
        }
        if (s == StrategieOrdonnancement.MINIMISER_ATTENTE) {
            return "Les matchs qui attendent depuis le plus longtemps partent d'abord : personne ne reste "
                    + "trop longtemps sans jouer.";
        }
        return s.getExplication();
    }

    /**
     * Applique le choix et l'enregistre aussitôt. Un refus d'écriture remet l'état
     * précédent (y compris « aucun choix ») et en donne le motif.
     */
    static ResultatChoixStrategie choisir(Tournoi tournoi, StrategieOrdonnancement s, Enregistreur enregistreur) {
        Objects.requireNonNull(s, "stratégie");
        if (tournoi.strategieOrdonnancementChoisie() && tournoi.getStrategieOrdonnancement() == s) {
            return new ResultatChoixStrategie(true, true, null);
        }
        StrategieOrdonnancement avant = tournoi.strategieOrdonnancementChoisie()
                ? tournoi.getStrategieOrdonnancement() : null;
        tournoi.setStrategieOrdonnancement(s);
        try {
            enregistreur.enregistrer(tournoi);
        } catch (DroitRefuseException refus) {
            tournoi.setStrategieOrdonnancement(avant);
            return new ResultatChoixStrategie(false, false, refus.getMessage());
        }
        return new ResultatChoixStrategie(true, false, PresentationPrevisions.messageChoixEnregistre(s));
    }
}
