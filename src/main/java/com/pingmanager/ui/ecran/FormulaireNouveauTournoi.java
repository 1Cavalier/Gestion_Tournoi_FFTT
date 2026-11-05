package com.pingmanager.ui.ecran;

import com.pingmanager.core.ServiceCompletudeTournoi.Niveau;
import com.pingmanager.model.Club;
import com.pingmanager.model.ModePreparation;
import com.pingmanager.model.NiveauTournoi;
import com.pingmanager.model.Tournoi;
import com.pingmanager.ui.ContexteApplication;
import com.pingmanager.ui.ecran.PresentationNouveauTournoi.Champ;
import com.pingmanager.ui.ecran.PresentationNouveauTournoi.Verification;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Consumer;

/**
 * Formulaire « Nouveau tournoi » (mise en page v2) : texte explicatif, carte
 * « Le tournoi », carte « Dates » avec sélecteurs à icône calendrier. Vue
 * mince : libellés, vérification et résumé viennent de
 * {@link PresentationNouveauTournoi}. Le brouillon créé est enregistré
 * aussitôt par {@link ContexteApplication#creerBrouillon(Tournoi)}, puis on
 * passe à la page suivante (choix du mode de préparation), inchangée.
 */
final class FormulaireNouveauTournoi {

    private final ContexteApplication contexte;
    private final Consumer<Path> apresCreation;
    private final Runnable annulation;

    FormulaireNouveauTournoi(ContexteApplication contexte, Consumer<Path> apresCreation, Runnable annulation) {
        this.contexte = contexte;
        this.apresCreation = apresCreation;
        this.annulation = annulation;
    }

    Node build() {
        Niveau h = Niveau.OBLIGATOIRE_HOMOLOGATION;
        List<Club> clubs = contexte.clubsGeres();
        Club[] club = {clubs.isEmpty() ? null : clubs.get(0)};

        // Club organisateur : repris de la base FFTT (choix si plusieurs clubs gérés).
        TextField clubLecture = Ui.champLecture(libelleClub(club[0]));
        ComboBox<Club> choixClub = new ComboBox<>();
        choixClub.getItems().addAll(clubs);
        choixClub.setConverter(new StringConverter<>() {
            @Override
            public String toString(Club c) {
                return libelleClub(c);
            }

            @Override
            public Club fromString(String s) {
                return null;
            }
        });
        choixClub.setValue(club[0]);

        // Nom : libre, avec une proposition « Tournoi de/du… » tant qu'on n'y touche pas.
        TextField nom = Ui.champ(null);
        nom.setText(PropositionsTournoi.nomTournoi(club[0] == null ? null : club[0].getNom()));
        boolean[] nomModifie = {false};
        nom.setOnKeyTyped(e -> nomModifie[0] = true);
        choixClub.valueProperty().addListener((o, a, n) -> {
            club[0] = n;
            if (!nomModifie[0]) {
                nom.setText(PropositionsTournoi.nomTournoi(n == null ? null : n.getNom()));
            }
        });

        ComboBox<NiveauTournoi> niveau = Ui.combo(NiveauTournoi.values(), null);

        DatePicker debut = selecteur();
        DatePicker fin = selecteur();
        // Calendrier de fin : les jours avant le début sont grisés.
        fin.setDayCellFactory(dp -> new DateCell() {
            @Override
            public void updateItem(LocalDate jour, boolean vide) {
                super.updateItem(jour, vide);
                LocalDate d = PresentationNouveauTournoi.lireDate(debut.getEditor().getText());
                if (!vide && !PresentationNouveauTournoi.jourDeFinPossible(d, jour)) {
                    setDisable(true);
                }
            }
        });

        Label resume = Ui.discret("");
        resume.getStyleClass().add("nouveau-tournoi-resume");
        Runnable majResume = () -> {
            String texte = PresentationNouveauTournoi.resumePeriode(
                    debut.getEditor().getText(), fin.getEditor().getText());
            resume.setText(texte);
            if (texte.isEmpty()) {
                Ui.cacher(resume);
            } else {
                Ui.montrer(resume);
            }
        };
        debut.getEditor().textProperty().addListener((o, a, n) -> majResume.run());
        fin.getEditor().textProperty().addListener((o, a, n) -> majResume.run());
        majResume.run();

        Label erreur = Ui.message();

        Button creer = Ui.boutonPrimaire(PresentationNouveauTournoi.BOUTON_CREER);
        creer.setDefaultButton(true);
        creer.setOnAction(e -> {
            Ui.cacher(erreur);
            Verification v = PresentationNouveauTournoi.verifier(nom.getText(), club[0] != null,
                    debut.getEditor().getText(), fin.getEditor().getText());
            if (!v.valide()) {
                Node fautif = champ(v.champEnErreur(), nom, debut, fin);
                if (fautif != null) {
                    Ui.marquerErreur(fautif);
                }
                Ui.erreur(erreur, v.motif());
                return;
            }
            Tournoi cree = construire(v.nom(), club[0], v.debut(), v.fin(), niveau.getValue());
            // Choix imposé juste après la création : préparation complète ou import.
            cree.setModePreparation(ModePreparation.A_CHOISIR);
            apresCreation.accept(contexte.creerBrouillon(cree));
        });

        Button annuler = Ui.boutonSecondaire(PresentationNouveauTournoi.BOUTON_ANNULER);
        annuler.setCancelButton(true);
        annuler.setOnAction(e -> annulation.run());

        Node champClub = clubs.size() > 1
                ? Ui.champEtiquete(PresentationNouveauTournoi.LIBELLE_CLUB, choixClub, h)
                : Ui.champEtiquete(PresentationNouveauTournoi.LIBELLE_CLUB, clubLecture, h);

        VBox carteTournoi = Ui.carte(PresentationNouveauTournoi.TITRE_CARTE_TOURNOI,
                champClub,
                Ui.champEtiquete(PresentationNouveauTournoi.LIBELLE_NOM, nom, h),
                Ui.champEtiquete(PresentationNouveauTournoi.LIBELLE_NIVEAU, niveau, h));

        VBox blocDebut = Ui.champEtiquete(PresentationNouveauTournoi.LIBELLE_DEBUT, debut, h);
        // À la création, une date de fin vide = tournoi d'un jour : facultatif ici.
        VBox blocFin = Ui.champEtiquete(PresentationNouveauTournoi.LIBELLE_FIN, fin, Niveau.OPTIONNEL);
        HBox dates = new HBox(16, blocDebut, blocFin);
        HBox.setHgrow(blocDebut, Priority.ALWAYS);
        HBox.setHgrow(blocFin, Priority.ALWAYS);
        VBox carteDates = Ui.carte(PresentationNouveauTournoi.TITRE_CARTE_DATES,
                dates, resume, Ui.note(PresentationNouveauTournoi.NOTE_DATES));

        Label explication = Ui.encartInfo(PresentationNouveauTournoi.TEXTE_EXPLICATIF);
        explication.getStyleClass().add("nouveau-tournoi-explication");

        VBox contenu = Ui.page(640,
                Ui.titre(PresentationNouveauTournoi.TITRE),
                explication,
                Ui.legende(EnumSet.of(h, Niveau.OPTIONNEL)),
                carteTournoi,
                carteDates,
                erreur,
                Ui.actions(creer, annuler));
        contenu.getStyleClass().add("nouveau-tournoi");
        return contenu;
    }

    /** Sélecteur jj/mm/aaaa avec icône calendrier (classe {@code selecteur-calendrier}). */
    private static DatePicker selecteur() {
        DatePicker p = Saisie.selecteurDate(null, null, null);
        p.setPromptText(PresentationNouveauTournoi.FORMAT_DATE);
        p.setTooltip(new Tooltip(PresentationNouveauTournoi.AIDE_CALENDRIER));
        p.getStyleClass().add("selecteur-calendrier");
        p.setMaxWidth(Double.MAX_VALUE);
        return p;
    }

    private static Node champ(Champ c, Node nom, Node debut, Node fin) {
        if (c == Champ.NOM) {
            return nom;
        }
        if (c == Champ.DEBUT) {
            return debut;
        }
        if (c == Champ.FIN) {
            return fin;
        }
        return null;
    }

    static String libelleClub(Club c) {
        return c == null ? "" : c.getNom() + " (n° " + c.getNumero() + ")";
    }

    /**
     * Brouillon avec les valeurs par défaut de la base : club et n° d'affiliation,
     * adresse du club, et correspondant = administrateur principal. Tout reste
     * modifiable ensuite dans la section « Général ».
     */
    private Tournoi construire(String nom, Club club, LocalDate debut, LocalDate fin, NiveauTournoi niveau) {
        List<LocalDate> jours = new ArrayList<>();
        for (LocalDate d = debut; !d.isAfter(fin); d = d.plusDays(1)) {
            jours.add(d);
        }
        Tournoi tournoi = new Tournoi(nom.trim(), club.getNom(), jours);
        tournoi.setNumeroAffiliationClub(club.getNumero());
        tournoi.setNiveau(niveau);
        tournoi.setPeriode(debut, fin);
        tournoi.setAdresse(FormulairesSection.adresseGeocodee(FormulairesSection.adresse(
                club.getAdresse1(), club.getCodePostal(), club.getVille()), null));
        String[] cor = contexte.correspondantParDefaut(club.getNumero());
        tournoi.setCorrespondant(cor[0], cor[1], cor[2], null, null);
        return tournoi;
    }
}
