package com.pingmanager.ui.ecran;

import com.pingmanager.ui.ContexteApplication;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.util.StringConverter;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * B043 : les tournois en cours du compte et celui choisi, partagés par les trois
 * pages du groupe « Tournoi en cours » (Gestion, Tableau, Pointage).
 *
 * <p>Par défaut, le tournoi le plus proche d'aujourd'hui ; le choix fait dans le
 * petit sélecteur en haut d'une page vaut pour les deux autres. Réévaluée à chaque
 * {@link Coquille#rafraichirNavigation()}.</p>
 */
final class SelectionTournoiEnCours {

    private final ContexteApplication contexte;
    private List<ContexteApplication.TournoiFichier> enCours = List.of();
    private Path choisi;

    SelectionTournoiEnCours(ContexteApplication contexte) {
        this.contexte = contexte;
    }

    /** Relit les tournois en cours ; garde le choix s'il est toujours en cours. */
    void rafraichir() {
        enCours = contexte.tournoisEnCours();
        if (choisi == null || enCours.stream().noneMatch(tf -> tf.fichier().equals(choisi))) {
            choisi = enCours.isEmpty() ? null : enCours.get(0).fichier();
        }
    }

    boolean estVide() {
        return enCours.isEmpty();
    }

    /** @return le fichier du tournoi choisi, ou {@code null} s'il n'y a aucun tournoi en cours. */
    Path fichier() {
        return choisi;
    }

    List<Path> fichiers() {
        List<Path> res = new ArrayList<>();
        for (ContexteApplication.TournoiFichier tf : enCours) {
            res.add(tf.fichier());
        }
        return res;
    }

    void choisir(Path fichier) {
        if (fichiers().contains(fichier)) {
            choisi = fichier;
        }
    }

    /**
     * Petit sélecteur de tournoi (seulement s'il y en a plusieurs en cours), ou
     * {@code null}. {@code apresChoix} reconstruit la page.
     */
    Node selecteur(Runnable apresChoix) {
        if (enCours.size() < 2) {
            return null;
        }
        ComboBox<ContexteApplication.TournoiFichier> choix = new ComboBox<>();
        choix.getStyleClass().add("encours-selecteur");
        choix.getItems().setAll(enCours);
        choix.setConverter(new StringConverter<>() {
            @Override
            public String toString(ContexteApplication.TournoiFichier tf) {
                if (tf == null) {
                    return "";
                }
                String nom = tf.tournoi().getNom();
                return nom == null || nom.isBlank() ? "Sans titre" : nom;
            }

            @Override
            public ContexteApplication.TournoiFichier fromString(String s) {
                return null;
            }
        });
        enCours.stream().filter(tf -> tf.fichier().equals(choisi)).findFirst()
                .ifPresent(tf -> choix.getSelectionModel().select(tf));
        choix.setOnAction(e -> {
            if (choix.getValue() != null && !choix.getValue().fichier().equals(choisi)) {
                choisi = choix.getValue().fichier();
                apresChoix.run();
            }
        });
        return choix;
    }
}
