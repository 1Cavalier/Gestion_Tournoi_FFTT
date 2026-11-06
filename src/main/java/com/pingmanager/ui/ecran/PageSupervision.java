package com.pingmanager.ui.ecran;

import com.pingmanager.core.ServiceCompletudeTournoi;
import com.pingmanager.model.StatutTournoi;
import com.pingmanager.model.Tournoi;
import com.pingmanager.ui.ContexteApplication;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Supervision (compte « ultra admin ») : tous les tournois de tous les clubs,
 * regroupés par club, avec leur statut et l'avancement du dossier. « Ouvrir »
 * donne accès au tournoi comme si l'on était organisateur, pour vérifier ou aider
 * en direct ; les modifications sont enregistrées dans le fichier du club.
 */
public final class PageSupervision {

    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH);

    private final ContexteApplication contexte;
    private final BorderPane racine = new BorderPane();
    private final ServiceCompletudeTournoi completude = new ServiceCompletudeTournoi();

    public PageSupervision(ContexteApplication contexte) {
        this.contexte = contexte;
    }

    public Parent build() {
        afficherListe();
        return racine;
    }

    private record Ligne(Path fichier, Tournoi tournoi, String club) {
    }

    private void afficherListe() {
        Map<String, List<Ligne>> parClub = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        int total = 0;
        for (Path f : contexte.fichiersTournois()) {
            try {
                Tournoi t = contexte.lireTournoi(f);
                String club = nomClub(t);
                String cle = club.replaceFirst(" \\(\\d+\\)$", ""); // même club, avec ou sans n°
                parClub.computeIfAbsent(cle, k -> new ArrayList<>()).add(new Ligne(f, t, club));
                total++;
            } catch (RuntimeException illisible) {
                // fichier déplacé ou illisible : ignoré
            }
        }

        VBox titres = new VBox(4, Ui.titre("Supervision"),
                Ui.discret(total + (total > 1 ? " tournois" : " tournoi") + " · tous les clubs"));
        Label info = new Label("Compte ultra admin : vous pouvez ouvrir n'importe quel tournoi pour vérifier son "
                + "dossier ou aider le club en direct. Vos modifications sont enregistrées dans le tournoi du club.");
        info.setWrapText(true);
        info.getStyleClass().add("rappel-attention");
        VBox contenu = Ui.page(860, titres, info);
        if (parClub.isEmpty()) {
            contenu.getChildren().add(Ui.carte("Aucun tournoi", Ui.discret("Aucun tournoi n'a encore été créé.")));
        }
        parClub.forEach((cle, lignes) -> {
            String club = lignes.stream().map(Ligne::club).filter(c -> !c.equals(cle)).findFirst().orElse(cle);
            VBox corps = new VBox(0);
            lignes.forEach(l -> corps.getChildren().add(ligne(l)));
            contenu.getChildren().add(Ui.carte(Ui.titreAvecCompteur(club, lignes.size()), corps));
        });
        racine.setCenter(Ui.defilant(contenu));
    }

    private Node ligne(Ligne l) {
        Tournoi t = l.tournoi();
        String nom = t.getNom() == null || t.getNom().isBlank() ? "Sans titre" : t.getNom();
        String date = t.getDateDebut() == null ? "dates à définir" : t.getDateDebut().format(JOUR);
        int nb = t.getTableaux().size();
        int h = completude.bloquantsHomologation(t);
        int p = completude.bloquantsPublication(t);
        String avancement = "homologation : " + (h == 0 ? "complet" : h + " manquant" + (h > 1 ? "s" : ""))
                + " · publication : " + (p == 0 ? "complet" : p + " manquant" + (p > 1 ? "s" : ""));
        VBox infos = new VBox(2,
                Ui.ligne(8, Ui.gras(nom), Ui.badge(libelleStatut(t.getStatut()))),
                Ui.discret(date + " · " + nb + (nb > 1 ? " tableaux" : " tableau")),
                Ui.discret(avancement));
        HBox.setHgrow(infos, Priority.ALWAYS);
        Button ouvrir = Ui.boutonSecondaire("Ouvrir");
        ouvrir.setOnAction(e -> racine.setCenter(
                new EcranCompletion(contexte, l.fichier(), this::afficherListe).build()));
        return Ui.ligneListe(infos, Ui.espace(), ouvrir);
    }

    private String nomClub(Tournoi t) {
        String num = t.getNumeroAffiliationClub();
        String nom = num == null ? null : contexte.clubParNumero(num).map(c -> c.getNom()).orElse(null);
        if (nom == null) {
            nom = t.getClubOrganisateur() == null ? "Club inconnu" : t.getClubOrganisateur();
        }
        return num == null ? nom : nom + " (" + num + ")";
    }

    static String libelleStatut(StatutTournoi statut) {
        if (statut == StatutTournoi.DEMANDE_HOMOLOGATION) {
            return "Demande d'homologation";
        }
        if (statut == StatutTournoi.HOMOLOGUE) {
            return "Homologué";
        }
        if (statut == StatutTournoi.PUBLIE) {
            return "Publié";
        }
        return statut == StatutTournoi.ANNULE ? "Annulé" : "Brouillon";
    }
}
