package com.pingmanager.ui.ecran;

import com.pingmanager.core.ExportIcs;
import com.pingmanager.core.Horloge;
import com.pingmanager.core.ResultatAgenda;
import com.pingmanager.core.ServiceAgenda;
import com.pingmanager.model.AgendaCompte;
import com.pingmanager.model.EvenementAgenda;
import com.pingmanager.model.FicheLicence;
import com.pingmanager.ui.ContexteApplication;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Page « Agenda » de la navigation (B045) : dates personnelles, tournois où le
 * joueur est inscrit (automatiques) et repères du calendrier fédéral, en vue
 * Liste (groupée par mois) ou Mois (grille). Vue mince : la logique d'affichage
 * est dans {@link PresentationAgenda}, les règles dans {@link ServiceAgenda}.
 */
final class PageAgenda {

    private final ContexteApplication contexte;
    private final ServiceAgenda service = ServiceAgenda.parDefaut();
    private final VBox racine = new VBox();
    private final Label message = Ui.message();

    private boolean vueMois;
    private boolean avecPassees;
    private final Set<PresentationAgenda.TypeDate> types = EnumSet.allOf(PresentationAgenda.TypeDate.class);
    private YearMonth moisAffiche = YearMonth.now();
    private LocalDate jourChoisi = LocalDate.now();

    PageAgenda(ContexteApplication contexte) {
        this.contexte = contexte;
    }

    Parent build() {
        AgendaCompte agenda = contexte.agendaDuCompte();
        String licence = contexte.ficheLicenceLiee().map(FicheLicence::getNumeroLicence).orElse(null);
        // Dates de tournoi tenues à jour d'après les inscriptions (comme « Mes tournois »).
        if (agenda != null && licence != null && PresentationMesTournois.synchroniserAgenda(agenda,
                contexte.tousLesTournois(), licence, LocalDate.now(), service)) {
            contexte.sauvegarder();
        }
        afficher();
        return Ui.defilant(racine);
    }

    private LocalDate aujourdhui() {
        return LocalDate.now();
    }

    private void afficher() {
        VBox page = Ui.page(900);
        page.getStyleClass().add("agenda-page");
        AgendaCompte agenda = contexte.agendaDuCompte();
        if (agenda == null) {
            page.getChildren().addAll(Ui.titre("Agenda"),
                    Ui.carteNue(Ui.etatVide(PresentationAgenda.texteHorsConnexion(), null)));
            racine.getChildren().setAll(page);
            return;
        }
        List<PresentationAgenda.Element> tous = elements(agenda);
        List<PresentationAgenda.Element> visibles = PresentationAgenda.filtrer(tous, types, avecPassees);

        page.getChildren().addAll(entete(agenda), filtres(), message);
        PresentationAgenda.prochain(tous, aujourdhui()).ifPresent(e -> {
            Label l = Ui.label(PresentationAgenda.texteProchain(e), "agenda-prochain");
            l.setWrapText(true);
            page.getChildren().add(l);
        });
        if (vueMois) {
            page.getChildren().add(vueMois(agenda, PresentationAgenda.filtrer(tous, types, true)));
        } else if (visibles.isEmpty()) {
            Button ajouter = Ui.boutonPrimaire("Ajouter une date");
            ajouter.setOnAction(e -> ajouter(agenda, null));
            page.getChildren().add(Ui.carteNue(Ui.etatVide(PresentationAgenda.texteVidePage(agenda.estVide()),
                    ajouter)));
        } else {
            page.getChildren().add(vueListe(agenda, visibles));
        }
        complements(page, agenda);
        racine.getChildren().setAll(page);
    }

    /** Éléments affichés (dates de l'agenda, et suggestions quand elles existent). */
    private List<PresentationAgenda.Element> elements(AgendaCompte agenda) {
        return PresentationAgenda.elements(agenda, aujourdhui());
    }

    /** Point d'extension : blocs ajoutés sous la liste (calendrier fédéral). */
    private void complements(VBox page, AgendaCompte agenda) {
    }

    // ------------------------------------------------------------------
    // En-tête : titre, Liste | Mois, Ajouter, Exporter
    // ------------------------------------------------------------------

    private Node entete(AgendaCompte agenda) {
        ToggleGroup groupe = new ToggleGroup();
        ToggleButton liste = new ToggleButton("Liste");
        ToggleButton mois = new ToggleButton("Mois");
        HBox segment = Ui.segment(groupe, liste, mois);
        (vueMois ? mois : liste).setSelected(true);
        groupe.selectedToggleProperty().addListener((o, a, n) -> {
            if (n == null) {
                (vueMois ? mois : liste).setSelected(true); // un choix reste toujours sélectionné
                return;
            }
            vueMois = n == mois;
            afficher();
        });
        Button ajouter = Ui.boutonPrimaire("Ajouter une date");
        ajouter.setOnAction(e -> ajouter(agenda, vueMois ? jourChoisi : null));
        Button exporter = Ui.boutonSecondaire("Exporter (.ics)");
        exporter.setOnAction(e -> exporter(agenda));
        HBox h = Ui.ligne(12, Ui.titre("Agenda"), Ui.espace(), segment, exporter, ajouter);
        h.getStyleClass().add("agenda-entete");
        return h;
    }

    private Node filtres() {
        FlowPane f = new FlowPane(16, 8);
        f.getStyleClass().add("agenda-filtres");
        f.setAlignment(Pos.CENTER_LEFT);
        for (PresentationAgenda.TypeDate t : PresentationAgenda.TypeDate.values()) {
            CheckBox c = new CheckBox(t.libelle());
            c.getStyleClass().addAll("agenda-filtre", t.classeCss());
            c.setSelected(types.contains(t));
            c.selectedProperty().addListener((o, a, n) -> {
                if (n) {
                    types.add(t);
                } else {
                    types.remove(t);
                }
                afficher();
            });
            f.getChildren().add(c);
        }
        Region sep = new Region();
        sep.getStyleClass().add("agenda-filtres-separateur");
        CheckBox passees = new CheckBox("Afficher les dates passées");
        passees.getStyleClass().add("agenda-filtre-passees");
        passees.setSelected(avecPassees);
        passees.selectedProperty().addListener((o, a, n) -> {
            avecPassees = n;
            afficher();
        });
        f.getChildren().addAll(sep, passees);
        return f;
    }

    // ------------------------------------------------------------------
    // Vue Liste
    // ------------------------------------------------------------------

    private Node vueListe(AgendaCompte agenda, List<PresentationAgenda.Element> visibles) {
        VBox v = new VBox(16);
        v.getStyleClass().add("agenda-liste");
        for (PresentationAgenda.GroupeMois g : PresentationAgenda.parMois(visibles)) {
            VBox lignes = new VBox(0);
            lignes.getStyleClass().add("agenda-mois-lignes");
            boolean premiere = true;
            for (PresentationAgenda.Element e : g.elements()) {
                HBox l = ligne(agenda, e);
                if (premiere) {
                    l.getStyleClass().add("ligne-element-premiere");
                    premiere = false;
                }
                lignes.getChildren().add(l);
            }
            VBox carte = Ui.carteNue(lignes);
            carte.getStyleClass().add("agenda-mois-carte");
            v.getChildren().addAll(Ui.label(g.libelle(), "agenda-mois-titre"), carte);
        }
        return v;
    }

    /** Une ligne : bloc date, titre, jour · heure · détail, pastilles puis actions. */
    HBox ligne(AgendaCompte agenda, PresentationAgenda.Element e) {
        HBox actions = new HBox(6);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.getChildren().add(pastilleType(e.type()));
        if (e.statut() != null) {
            actions.getChildren().add(Ui.pastille(e.statut(),
                    e.aVerifier() ? StatutPastille.ATTENTION : StatutPastille.SUCCES));
        }
        actionsLigne(agenda, e, actions);
        HBox h = Ui.ligneElement(Ui.blocDate(e.date()), e.titre(), PresentationAgenda.detailLigne(e), actions);
        h.getStyleClass().add("agenda-ligne");
        if (e.passee()) {
            h.getStyleClass().add("agenda-ligne-passee");
        }
        if (e.suggestion()) {
            h.getStyleClass().add("agenda-ligne-suggestion");
        }
        return h;
    }

    /** Actions d'une ligne : modifier et retirer une date personnelle. */
    private void actionsLigne(AgendaCompte agenda, PresentationAgenda.Element e, HBox actions) {
        if (!e.modifiable() || e.suggestion()) {
            return;
        }
        Button modifier = Ui.boutonLien("Modifier");
        modifier.setOnAction(a -> agenda.trouver(e.id()).ifPresent(ev -> modifier(agenda, ev)));
        Button retirer = Ui.boutonLien("Retirer");
        retirer.getStyleClass().add("agenda-retirer");
        retirer.setOnAction(a -> {
            ResultatAgenda r = service.retirerDate(agenda, e.id());
            if (r.estAccepte()) {
                contexte.sauvegarder();
            }
            afficher();
            montrer(r.estAccepte(), r.message());
        });
        actions.getChildren().addAll(modifier, retirer);
    }

    private static Label pastilleType(PresentationAgenda.TypeDate type) {
        Label l = Ui.badge(type.libelle());
        l.getStyleClass().addAll("pastille", "agenda-pastille", type.classeCss());
        return l;
    }

    // ------------------------------------------------------------------
    // Vue Mois
    // ------------------------------------------------------------------

    private Node vueMois(AgendaCompte agenda, List<PresentationAgenda.Element> elements) {
        Button precedent = Ui.boutonSecondaire("‹");
        precedent.setOnAction(e -> {
            moisAffiche = moisAffiche.minusMonths(1);
            afficher();
        });
        Button suivant = Ui.boutonSecondaire("›");
        suivant.setOnAction(e -> {
            moisAffiche = moisAffiche.plusMonths(1);
            afficher();
        });
        Button ceMois = Ui.boutonLien("Aujourd'hui");
        ceMois.setOnAction(e -> {
            moisAffiche = YearMonth.now();
            jourChoisi = aujourdhui();
            afficher();
        });
        HBox nav = Ui.ligne(8, Ui.titreCarte(PresentationAgenda.libelleMois(moisAffiche)), Ui.espace(),
                ceMois, precedent, suivant);
        nav.getStyleClass().add("agenda-mois-nav");

        GridPane grille = new GridPane();
        grille.getStyleClass().add("agenda-grille");
        for (int c = 0; c < 7; c++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / 7);
            cc.setHgrow(Priority.ALWAYS);
            grille.getColumnConstraints().add(cc);
        }
        List<String> entetes = PresentationAgenda.entetesSemaine();
        for (int c = 0; c < entetes.size(); c++) {
            Label l = Ui.label(entetes.get(c), "agenda-grille-entete");
            l.setMaxWidth(Double.MAX_VALUE);
            grille.add(l, c, 0);
        }
        List<PresentationAgenda.CaseJour> cases = PresentationAgenda.grille(moisAffiche, elements, aujourdhui());
        for (int i = 0; i < cases.size(); i++) {
            grille.add(caseJour(cases.get(i)), i % 7, 1 + i / 7);
        }
        VBox carte = Ui.carteNue(nav, grille);
        carte.getStyleClass().add("agenda-mois-vue");
        return new VBox(16, carte, detailJour(agenda, elements));
    }

    private Node caseJour(PresentationAgenda.CaseJour c) {
        Label numero = Ui.label(String.valueOf(c.jour().getDayOfMonth()), "agenda-case-numero");
        HBox points = new HBox(3);
        points.setAlignment(Pos.CENTER);
        points.getStyleClass().add("agenda-case-points");
        for (PresentationAgenda.TypeDate t : c.types()) {
            Region p = new Region();
            p.getStyleClass().addAll("agenda-point", t.classeCss());
            points.getChildren().add(p);
        }
        VBox v = new VBox(4, numero, points);
        v.setAlignment(Pos.TOP_CENTER);
        v.setMaxWidth(Double.MAX_VALUE);
        v.getStyleClass().add("agenda-case");
        if (!c.dansLeMois()) {
            v.getStyleClass().add("agenda-case-hors-mois");
        }
        if (c.aujourdhui()) {
            v.getStyleClass().add("agenda-case-aujourdhui");
        }
        if (c.occupe()) {
            v.getStyleClass().add("agenda-case-occupee");
        }
        if (c.jour().equals(jourChoisi)) {
            v.getStyleClass().add("agenda-case-choisie");
        }
        v.setOnMouseClicked(e -> {
            jourChoisi = c.jour();
            moisAffiche = YearMonth.from(c.jour());
            afficher();
        });
        return v;
    }

    private Node detailJour(AgendaCompte agenda, List<PresentationAgenda.Element> elements) {
        List<PresentationAgenda.Element> duJour = PresentationAgenda.duJour(elements, jourChoisi);
        VBox lignes = new VBox(0);
        if (duJour.isEmpty()) {
            lignes.getChildren().add(Ui.etatVideCompact(PresentationAgenda.texteJourLibre()));
        }
        boolean premiere = true;
        for (PresentationAgenda.Element e : duJour) {
            HBox l = ligne(agenda, e);
            if (premiere) {
                l.getStyleClass().add("ligne-element-premiere");
                premiere = false;
            }
            lignes.getChildren().add(l);
        }
        VBox carte = Ui.carte(Ui.titreCarte(PresentationAgenda.titreJour(jourChoisi)), lignes);
        carte.getStyleClass().add("agenda-detail-jour");
        return carte;
    }

    // ------------------------------------------------------------------
    // Actions
    // ------------------------------------------------------------------

    private void ajouter(AgendaCompte agenda, LocalDate jour) {
        Optional<ResultatAgenda> r = FenetreDateAgenda.ouvrir(racine, service, agenda, null, jour);
        r.ifPresent(this::apresSaisie);
    }

    private void modifier(AgendaCompte agenda, EvenementAgenda e) {
        Optional<ResultatAgenda> r = FenetreDateAgenda.ouvrir(racine, service, agenda, e, null);
        r.ifPresent(this::apresSaisie);
    }

    private void apresSaisie(ResultatAgenda r) {
        contexte.sauvegarder();
        if (r.getEvenement() != null) {
            jourChoisi = r.getEvenement().getDate();
            moisAffiche = YearMonth.from(jourChoisi);
        }
        afficher();
        montrer(true, r.message());
    }

    private void exporter(AgendaCompte agenda) {
        Optional<Boolean> seulementTournois = choisirExport();
        if (seulementTournois.isEmpty()) {
            return;
        }
        FileChooser fc = new FileChooser();
        fc.setTitle("Exporter l'agenda");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Agenda iCalendar", "*.ics"));
        fc.setInitialFileName(ExportIcs.nomFichier());
        File f = fc.showSaveDialog(racine.getScene() == null ? null : racine.getScene().getWindow());
        if (f == null) {
            return;
        }
        Path cible = f.getName().toLowerCase().endsWith(".ics") ? f.toPath()
                : f.toPath().resolveSibling(f.getName() + ".ics");
        ExportIcs export = new ExportIcs(Horloge.systeme());
        String texte = seulementTournois.get() ? export.exporterTournois(agenda) : export.exporter(agenda);
        try {
            Files.writeString(cible, texte, StandardCharsets.UTF_8);
            montrer(true, PresentationMesTournois.messageExport(cible.getFileName().toString()));
        } catch (IOException ex) {
            montrer(false, "Export impossible : " + ex.getMessage());
        }
    }

    /** @return vrai pour « seulement les tournois », faux pour « tout », vide si l'on renonce. */
    private Optional<Boolean> choisirExport() {
        Dialog<ButtonType> fenetre = new Dialog<>();
        fenetre.setTitle("Exporter (.ics)");
        if (racine.getScene() != null) {
            fenetre.initOwner(racine.getScene().getWindow());
            fenetre.getDialogPane().getStylesheets().setAll(racine.getScene().getStylesheets());
            fenetre.getDialogPane().getStyleClass().addAll(racine.getScene().getRoot().getStyleClass());
        }
        fenetre.getDialogPane().getStyleClass().add("agenda-fenetre");
        ToggleGroup g = new ToggleGroup();
        RadioButton tout = new RadioButton("Tout l'agenda");
        RadioButton tournois = new RadioButton("Seulement les tournois");
        tout.setToggleGroup(g);
        tournois.setToggleGroup(g);
        tout.setSelected(true);
        VBox contenu = new VBox(10, Ui.libelleChamp("Que voulez-vous exporter ?"), tout, tournois,
                Ui.aide("Le fichier .ics s'importe dans Google Agenda, Outlook ou Apple Calendrier."));
        contenu.getStyleClass().add("agenda-fenetre-contenu");
        fenetre.getDialogPane().setContent(contenu);
        ButtonType ok = new ButtonType("Exporter…", ButtonBar.ButtonData.OK_DONE);
        fenetre.getDialogPane().getButtonTypes().setAll(ok, new ButtonType("Renoncer",
                ButtonBar.ButtonData.CANCEL_CLOSE));
        Optional<ButtonType> r = fenetre.showAndWait();
        if (r.isEmpty() || r.get() != ok) {
            return Optional.empty();
        }
        return Optional.of(tournois.isSelected());
    }

    private void montrer(boolean ok, String texte) {
        if (ok) {
            Ui.succes(message, texte);
        } else {
            Ui.erreur(message, texte);
        }
    }
}
