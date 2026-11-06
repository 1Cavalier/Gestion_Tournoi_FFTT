package com.pingmanager.ui.ecran;

import com.pingmanager.core.ExportIcs;
import com.pingmanager.core.Horloge;
import com.pingmanager.core.PerformancesJoueur;
import com.pingmanager.core.RemplissageTableaux;
import com.pingmanager.core.ResultatAgenda;
import com.pingmanager.core.ServiceAgenda;
import com.pingmanager.core.ServiceInscriptionEnLigne;
import com.pingmanager.model.AgendaCompte;
import com.pingmanager.model.FicheLicence;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.ui.ContexteApplication;
import com.pingmanager.ui.ContexteApplication.TournoiFichier;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * « Mes tournois » (cahier des charges v0, section 3), en trois parties dans
 * l'ordre : le <b>tournoi en cours</b> avec accès direct au suivi, <b>mes
 * inscriptions</b> triées par date avec l'<b>agenda</b> (dates personnelles,
 * export .ics, désinscription), puis les <b>tournois passés</b> avec mes
 * performances. Vue mince : textes et règles dans {@link PresentationMesTournois}.
 */
final class MesTournois {

    private final ContexteApplication contexte;
    private final List<TournoiFichier> tous;
    private final Consumer<String> ouvrirFiche;
    private final Consumer<String> revoir;
    private final ServiceAgenda serviceAgenda = ServiceAgenda.parDefaut();
    private final VBox racine = new VBox();
    private final Label message = Ui.message();

    /**
     * @param ouvrirFiche ouvre la fiche (ou le suivi en direct) d'un tournoi par son identifiant
     * @param revoir      rouvre un tournoi passé en lecture seule (poules, scores, tableau final)
     */
    MesTournois(ContexteApplication contexte, List<TournoiFichier> tous, Consumer<String> ouvrirFiche,
                Consumer<String> revoir) {
        this.contexte = contexte;
        this.tous = tous;
        this.ouvrirFiche = ouvrirFiche;
        this.revoir = revoir;
    }

    Node build() {
        String licence = licence();
        AgendaCompte agenda = contexte.agendaDuCompte();
        if (PresentationMesTournois.synchroniserAgenda(agenda, tous, licence, LocalDate.now(), serviceAgenda)) {
            contexte.sauvegarder();
        }
        afficher();
        return Ui.defilant(racine);
    }

    private String licence() {
        return contexte.ficheLicenceLiee().map(FicheLicence::getNumeroLicence).orElse(null);
    }

    private void afficher() {
        VBox page = Ui.page(900);
        page.getStyleClass().add("mes-tournois");
        page.getChildren().add(message);
        Optional<FicheLicence> fiche = contexte.ficheLicenceLiee();
        if (fiche.isEmpty()) {
            page.getChildren().add(Ui.etatVide(PresentationMesTournois.texteSansLicence(), null));
            page.getChildren().add(partieAgenda());
            racine.getChildren().setAll(page);
            return;
        }
        String licence = fiche.get().getNumeroLicence();
        PresentationMesTournois.Repartition r = PresentationMesTournois.repartir(contexte.mesTournoisJoueur(tous),
                LocalDate.now());

        // (1) Tournoi en cours
        VBox enCours = partie(PresentationMesTournois.TITRE_EN_COURS, r.enCours().size(), "mes-tournois-en-cours");
        if (r.enCours().isEmpty()) {
            enCours.getChildren().add(Ui.etatVideCompact(PresentationMesTournois.texteVideEnCours()));
        }
        r.enCours().forEach(tf -> enCours.getChildren().add(carteEnCours(tf, licence)));

        // (2) Mes inscriptions puis agenda
        VBox inscriptions = partie(PresentationMesTournois.TITRE_INSCRIPTIONS, r.inscriptions().size(),
                "mes-tournois-inscriptions");
        if (r.inscriptions().isEmpty()) {
            inscriptions.getChildren().add(Ui.etatVideCompact(PresentationMesTournois.texteVideInscriptions()));
        }
        r.inscriptions().forEach(tf -> inscriptions.getChildren().add(carteInscription(tf, licence)));

        // (3) Tournois passés
        VBox passes = partie(PresentationMesTournois.TITRE_PASSES, r.passes().size(), "mes-tournois-passes");
        if (r.passes().isEmpty()) {
            passes.getChildren().add(Ui.etatVideCompact(PresentationMesTournois.texteVidePasses()));
        }
        r.passes().forEach(tf -> passes.getChildren().add(cartePasse(tf, licence)));

        page.getChildren().addAll(enCours, inscriptions, partieAgenda(), passes);
        racine.getChildren().setAll(page);
    }

    private static VBox partie(String titre, int nombre, String classe) {
        VBox v = new VBox(8, Ui.titreAvecCompteur(titre, nombre));
        v.getStyleClass().addAll("mes-tournois-partie", classe);
        return v;
    }

    // ------------------------------------------------------------------
    // Cartes
    // ------------------------------------------------------------------

    private Node carteEnCours(TournoiFichier tf, String licence) {
        Tournoi t = tf.tournoi();
        Button suivre = Ui.boutonPrimaire(PresentationMesTournois.texteBoutonEnCours(t));
        suivre.setOnAction(e -> ouvrirFiche.accept(tf.fichier().toString()));
        Label badge = Ui.badge("En cours");
        badge.getStyleClass().add("badge-en-cours");
        VBox c = ligne(t, puces(t, licence), badge, suivre);
        c.getStyleClass().add("mes-tournois-carte-en-cours");
        return c;
    }

    private Node carteInscription(TournoiFichier tf, String licence) {
        Tournoi t = tf.tournoi();
        RemplissageTableaux.Participant p = RemplissageTableaux.participant(t, licence).orElseThrow();
        Button voir = Ui.boutonSecondaire("Voir la fiche");
        voir.setOnAction(e -> ouvrirFiche.accept(tf.fichier().toString()));
        List<Tableau> quittables = PresentationMesTournois.tableauxDesinscriptibles(t, licence);
        VBox c;
        if (quittables.isEmpty()) {
            c = ligne(t, puces(t, licence), BlocsInscriptions.pastillePaiement(t, p), voir);
        } else {
            Button quitter = Ui.boutonLien("Me désinscrire");
            c = ligne(t, puces(t, licence), BlocsInscriptions.pastillePaiement(t, p), voir, quitter);
            VBox encart = new VBox();
            c.getChildren().add(encart);
            quitter.setOnAction(e -> encart.getChildren().setAll(encartDesinscription(tf, quittables, encart)));
        }
        return c;
    }

    private Node encartDesinscription(TournoiFichier tf, List<Tableau> quittables, VBox encart) {
        List<CheckBox> cases = new ArrayList<>();
        FlowPane choix = new FlowPane(12, 6);
        for (Tableau tab : quittables) {
            CheckBox cb = new CheckBox(tab.getNom());
            cb.setSelected(true);
            cases.add(cb);
            choix.getChildren().add(cb);
        }
        Label texte = Ui.texte(PresentationMesTournois.texteConfirmationDesinscription(quittables));
        texte.setWrapText(true);
        Button confirmer = Ui.boutonPrimaire("Confirmer la désinscription");
        Button renoncer = Ui.boutonSecondaire("Annuler");
        renoncer.setOnAction(e -> encart.getChildren().clear());
        cases.forEach(cb -> cb.selectedProperty().addListener((o, a, b) ->
                texte.setText(PresentationMesTournois.texteConfirmationDesinscription(choisis(quittables, cases)))));
        confirmer.setOnAction(e -> desinscrire(tf, choisis(quittables, cases)));
        VBox v = new VBox(10, choix, texte, Ui.actions(confirmer, renoncer));
        v.getStyleClass().add("encart-confirmation");
        return v;
    }

    private static List<Tableau> choisis(List<Tableau> quittables, List<CheckBox> cases) {
        List<Tableau> res = new ArrayList<>();
        for (int i = 0; i < quittables.size(); i++) {
            if (cases.get(i).isSelected()) {
                res.add(quittables.get(i));
            }
        }
        return res;
    }

    private void desinscrire(TournoiFichier tf, List<Tableau> choisis) {
        // Les tableaux viennent du tournoi affiché : on désinscrit sur ce même objet, puis on enregistre.
        ServiceInscriptionEnLigne.BilanDesinscription b = contexte.desinscrireEnLigne(tf.fichier(), tf.tournoi(),
                choisis);
        afficher();
        montrer(b.estValide(), PresentationMesTournois.messageDesinscription(b));
    }

    private Node cartePasse(TournoiFichier tf, String licence) {
        Tournoi t = tf.tournoi();
        PerformancesJoueur.Bilan bilan = PerformancesJoueur.calculer(t, licence);
        String perf = PresentationMesTournois.performances(bilan);
        VBox detail = new VBox(2, bilan.estVide() ? Ui.discret(perf) : Ui.gras(perf));
        detail.getStyleClass().add("mes-tournois-performances");
        if (bilan.tableaux().size() > 1) {
            bilan.tableaux().forEach(b -> detail.getChildren().add(
                    Ui.discret(PresentationMesTournois.performancesTableau(b))));
        }
        Button revoirBouton = Ui.boutonSecondaire(PresentationMesTournois.texteBoutonPasse(t));
        String id = tf.fichier().toString();
        revoirBouton.setOnAction(e -> {
            if (PresentationMesTournois.peutRevoir(t)) {
                revoir.accept(id);
            } else {
                ouvrirFiche.accept(id);
            }
        });
        VBox c = ligne(t, detail, null, revoirBouton);
        c.getStyleClass().add("mes-tournois-carte-passe");
        return c;
    }

    private FlowPane puces(Tournoi t, String licence) {
        FlowPane puces = new FlowPane(6, 4);
        Optional<RemplissageTableaux.Participant> p = RemplissageTableaux.participant(t, licence);
        if (p.isPresent()) {
            for (Tableau tab : p.get().inscrit()) {
                puces.getChildren().add(BlocsInscriptions.puceTableau(tab, null));
            }
            for (Tableau tab : p.get().enAttente()) {
                puces.getChildren().add(BlocsInscriptions.puceTableau(tab,
                        tab.getFileAttente().indexOf(p.get().joueur()) + 1));
            }
        }
        return puces;
    }

    private static VBox ligne(Tournoi t, Node detail, Node etat, Node... actions) {
        String lieu = t.getAdresse() == null || t.getAdresse().getVille() == null ? "" : " · " + t.getAdresse().getVille();
        VBox texte = new VBox(4, Ui.gras(t.getNom()), Ui.discret(BlocsInscriptions.periode(t) + lieu), detail);
        HBox.setHgrow(texte, Priority.ALWAYS);
        HBox h = Ui.ligne(14, Ui.blocDate(com.pingmanager.core.CalendrierTournoi.debut(t)), texte);
        if (etat != null) {
            h.getChildren().add(etat);
        }
        h.getChildren().add(Ui.actions(actions));
        VBox c = Ui.carteNue(h);
        c.getStyleClass().add("mes-tournois-carte");
        return c;
    }

    // ------------------------------------------------------------------
    // Agenda (B032) : dates personnelles, export .ics
    // ------------------------------------------------------------------

    private Node partieAgenda() {
        AgendaCompte agenda = contexte.agendaDuCompte();
        List<PresentationAgenda.LigneAgenda> lignes = agenda == null ? List.of()
                : PresentationAgenda.lignes(agenda, LocalDate.now(), false);
        VBox v = partie(PresentationMesTournois.TITRE_AGENDA, lignes.size(), "mes-tournois-agenda");
        if (agenda == null) {
            v.getChildren().add(Ui.etatVideCompact(PresentationMesTournois.texteAgendaHorsConnexion()));
            return v;
        }
        Button exporter = Ui.boutonSecondaire("Exporter (.ics)");
        exporter.setOnAction(e -> exporter(agenda));
        ((HBox) v.getChildren().get(0)).getChildren().addAll(Ui.espace(), exporter);

        VBox liste = new VBox(0);
        liste.getStyleClass().add("mes-agenda-liste");
        if (lignes.isEmpty()) {
            liste.getChildren().add(Ui.etatVideCompact(PresentationAgenda.texteVide()));
        }
        for (PresentationAgenda.LigneAgenda l : lignes) {
            liste.getChildren().add(ligneAgenda(agenda, l));
        }
        VBox carte = Ui.carteNue(liste, formulaireDate(agenda));
        v.getChildren().add(carte);
        return v;
    }

    private Node ligneAgenda(AgendaCompte agenda, PresentationAgenda.LigneAgenda l) {
        Label etiquette = Ui.badge(l.etiquette());
        etiquette.getStyleClass().add(l.automatique() ? "mes-agenda-etiquette-tournoi" : "mes-agenda-etiquette-perso");
        String detail = l.jour() + " · " + l.heure() + (l.detail().isEmpty() ? "" : " · " + l.detail());
        HBox h;
        if (PresentationMesTournois.peutRetirer(l)) {
            Button retirer = Ui.boutonLien("Retirer");
            retirer.setOnAction(e -> {
                ResultatAgenda r = serviceAgenda.retirerDate(agenda, l.id());
                if (r.estAccepte()) {
                    contexte.sauvegarder();
                }
                afficher();
                montrer(r.estAccepte(), r.message());
            });
            h = Ui.ligneElement(etiquette, l.titre(), detail, retirer);
        } else {
            h = Ui.ligneElement(etiquette, l.titre(), detail);
        }
        h.getStyleClass().add("mes-agenda-ligne");
        return h;
    }

    private Node formulaireDate(AgendaCompte agenda) {
        TextField titre = Ui.champ("Titre (entraînement, stage…)");
        DatePicker date = new DatePicker();
        date.setPromptText("Date");
        TextField heure = Ui.champ("Heure (facultatif)");
        heure.setPrefColumnCount(10);
        TextField note = Ui.champ("Note (facultatif)");
        HBox.setHgrow(titre, Priority.ALWAYS);
        HBox.setHgrow(note, Priority.ALWAYS);
        Button ajouter = Ui.boutonPrimaire("Ajouter la date");
        ajouter.setOnAction(e -> {
            LocalTime h;
            try {
                h = Saisie.lireHeure(heure.getText(), "Heure");
            } catch (IllegalArgumentException ex) {
                montrer(false, ex.getMessage());
                return;
            }
            String n = note.getText() == null || note.getText().isBlank() ? null : note.getText().trim();
            ResultatAgenda r = serviceAgenda.ajouterDate(agenda, titre.getText(), date.getValue(), h, n);
            if (r.estAccepte()) {
                contexte.sauvegarder();
                afficher();
            }
            montrer(r.estAccepte(), r.message());
        });
        VBox v = new VBox(8, Ui.libelleChamp("Ajouter une date personnelle"),
                Ui.ligne(8, titre, date, heure), Ui.ligne(8, note, ajouter));
        v.getStyleClass().add("mes-agenda-formulaire");
        return v;
    }

    private void exporter(AgendaCompte agenda) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Exporter l'agenda");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Agenda iCalendar", "*.ics"));
        fc.setInitialFileName(ExportIcs.nomFichier());
        File f = fc.showSaveDialog(racine.getScene() == null ? null : racine.getScene().getWindow());
        if (f == null) {
            return;
        }
        Path cible = f.getName().toLowerCase().endsWith(".ics") ? f.toPath() : f.toPath().resolveSibling(f.getName() + ".ics");
        try {
            Files.writeString(cible, new ExportIcs(Horloge.systeme()).exporter(agenda), StandardCharsets.UTF_8);
            montrer(true, PresentationMesTournois.messageExport(cible.getFileName().toString()));
        } catch (IOException ex) {
            montrer(false, "Export impossible : " + ex.getMessage());
        }
    }

    private void montrer(boolean ok, String texte) {
        if (ok) {
            Ui.succes(message, texte);
        } else {
            Ui.erreur(message, texte);
        }
    }
}
