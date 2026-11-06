package com.pingmanager.ui.ecran;

import com.pingmanager.core.OrdonnancementTableaux;
import com.pingmanager.core.ResultatModificationTournoi;
import com.pingmanager.core.ServiceCompletudeTournoi.Niveau;
import com.pingmanager.core.ServiceVerrousHomologation;
import com.pingmanager.model.ChampHomologation;
import com.pingmanager.core.reglement.LibellesTableau;
import com.pingmanager.model.CategorieAge;
import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.GrilleRecompenses;
import com.pingmanager.model.HorairesTableau;
import com.pingmanager.model.InfosReglement;
import com.pingmanager.model.Recompense;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.TarifInscription;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.TypeRecompense;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Section « Tableaux et inscriptions », en deux parties :
 * <ol>
 *   <li><b>Échéances</b> : rappel des dates du tournoi, puis ouverture et
 *       clôture des inscriptions (la clôture du pointage se règle désormais
 *       tableau par tableau) ;</li>
 *   <li><b>Tableaux</b> : grille récapitulative, avec « Ajouter un tableau » en
 *       haut à droite qui ouvre le {@link FormulaireTableau formulaire complet} ;</li>
 *   <li><b>Règles d'inscription</b> ({@link CarteReglesInscription}) : tableaux
 *       par jour, maintien des tableaux et dotations, forfaits.</li>
 * </ol>
 */
final class EcranTableaux {

    private final Tournoi tournoi;
    private final Runnable enregistrer;
    private final Runnable retour;
    private final BorderPane racine = new BorderPane();
    private final ServiceVerrousHomologation verrous = new ServiceVerrousHomologation();

    /**
     * @param enregistrer persiste le tournoi (appelé après chaque modification validée)
     * @param retour      revient à la vue d'ensemble
     */
    EcranTableaux(Tournoi tournoi, Runnable enregistrer, Runnable retour) {
        this.tournoi = tournoi;
        this.enregistrer = enregistrer;
        this.retour = retour;
    }

    Node build() {
        afficherListe();
        return racine;
    }

    private void poser(Node contenu) {
        racine.setCenter(Ui.defilant(contenu));
    }

    // ==================================================================
    // Vue principale : échéances + grille
    // ==================================================================

    private void afficherListe() {
        Button btnRetour = Ui.boutonLien("← Vue d'ensemble");
        btnRetour.setOnAction(e -> retour.run());

        VBox enTete = new VBox(6, btnRetour, Ui.titre("Tableaux et inscriptions"));
        poser(Ui.page(1040, enTete,
                Ui.legende(EnumSet.of(Niveau.OBLIGATOIRE_HOMOLOGATION, Niveau.OBLIGATOIRE_PUBLICATION,
                        Niveau.RECOMMANDE, Niveau.OPTIONNEL)),
                carteEcheances(), carteTableaux(), new CarteReglesInscription(tournoi, enregistrer).build(),
                carteOrdonnancement()));
    }

    // --- Échéances ------------------------------------------------------

    private Node carteEcheances() {
        InfosReglement ir = tournoi.getInfosReglement();
        LocalDateTime ouv = ir == null ? null : ir.getOuvertureInscriptions();
        LocalDateTime clo = ir == null ? null : ir.getClotureInscriptions();

        // Bornes : l'ouverture avant le 1er jour du tournoi ; la clôture au plus tard le 1er jour.
        LocalDate j1 = tournoi.getDateDebut();
        LocalDate tresTot = LocalDate.of(2000, 1, 1);
        DatePicker jourOuv = Saisie.selecteurDate(ouv == null ? null : ouv.toLocalDate(),
                j1 == null ? null : tresTot, j1 == null ? null : j1.minusDays(1));
        TextField heureOuv = Ui.champ("ex. 10h");
        heureOuv.setText(ouv == null ? "" : Saisie.heure(ouv.toLocalTime()));
        DatePicker jourClo = Saisie.selecteurDate(clo == null ? null : clo.toLocalDate(),
                j1 == null ? null : tresTot, j1);
        TextField heureClo = Ui.champ("ex. 20h");
        heureClo.setText(clo == null ? "" : Saisie.heure(clo.toLocalTime()));

        Niveau P = Niveau.OBLIGATOIRE_PUBLICATION;
        GridPane g = grilleDeuxColonnes();
        g.add(Ui.champEtiquete("Ouverture des inscriptions", jourOuv, P,
                jourOuv.valueProperty(), () -> jourOuv.getValue() != null), 0, 0);
        g.add(Ui.champEtiquete("Heure d'ouverture", heureOuv, null), 1, 0);
        g.add(Ui.champEtiquete("Clôture des inscriptions", jourClo, P,
                jourClo.valueProperty(), () -> jourClo.getValue() != null), 0, 1);
        g.add(Ui.champEtiquete("Heure de clôture", heureClo, null), 1, 1);

        Label message = Ui.message();
        Button valider = Ui.boutonPrimaire("Enregistrer les échéances");
        valider.setOnAction(e -> {
            Ui.cacher(message);
            try {
                LocalDateTime o = dateHeure(jourOuv.getValue(), heureOuv, "Heure d'ouverture", LocalTime.of(0, 0));
                LocalDateTime c = dateHeure(jourClo.getValue(), heureClo, "Heure de clôture", LocalTime.of(23, 59));
                String j1Texte = j1 == null ? "" : j1.format(Saisie.JJ_MM_AAAA);
                if (o != null && j1 != null && !o.toLocalDate().isBefore(j1)) {
                    Ui.marquerErreur(jourOuv);
                    throw new IllegalArgumentException("L'ouverture des inscriptions doit avoir lieu avant le "
                            + "premier jour du tournoi (" + j1Texte + ").");
                }
                if (o != null && c != null && !o.isBefore(c)) {
                    Ui.marquerErreur(jourClo);
                    throw new IllegalArgumentException("La clôture doit venir après l'ouverture des inscriptions.");
                }
                if (c != null && j1 != null && c.toLocalDate().isAfter(j1)) {
                    Ui.marquerErreur(jourClo);
                    throw new IllegalArgumentException("La clôture des inscriptions doit se situer entre l'ouverture "
                            + "et le premier jour du tournoi (" + j1Texte + ") inclus.");
                }
                InfosReglement.Builder b = ir != null ? ir.toBuilder() : InfosReglement.builder();
                tournoi.setInfosReglement(b.ouvertureInscriptions(o).clotureInscriptions(c).build());
                enregistrer.run();
                Ui.succes(message, "Échéances enregistrées.");
            } catch (IllegalArgumentException ex) {
                Ui.erreur(message, ex.getMessage());
            }
        });

        VBox corps = new VBox(14,
                rappelDates(),
                g,
                Ui.note("L'ouverture se place avant le premier jour du tournoi, la clôture entre "
                        + "l'ouverture et le premier jour. La clôture du pointage se règle tableau par tableau."),
                message,
                Ui.actions(valider));
        return Ui.carte("Échéances", corps);
    }

    /** « Votre tournoi se déroule du samedi 13 mars 2027 au dimanche 14 mars 2027. » */
    private Node rappelDates() {
        LocalDate d = tournoi.getDateDebut();
        LocalDate f = tournoi.getDateFin();
        if (d == null || f == null) {
            Label l = Ui.texte("Les dates du tournoi ne sont pas encore renseignées (section Général). "
                    + "Renseignez-les d'abord : chaque tableau doit se jouer pendant le tournoi.");
            l.getStyleClass().addAll("rappel-attention", "rappel-doux");
            return l;
        }
        Label debut = Ui.gras(Saisie.jourLong(d));
        if (d.equals(f)) {
            return Ui.ligne(5, Ui.texte("Vous avez sélectionné votre tournoi le"), debut);
        }
        return Ui.ligne(5, Ui.texte("Vous avez sélectionné votre tournoi du"), debut,
                Ui.texte("au"), Ui.gras(Saisie.jourLong(f)));
    }

    private static LocalDateTime dateHeure(LocalDate jour, TextField heure, String nom, LocalTime parDefaut) {
        LocalTime h = Saisie.lireHeure(heure.getText(), nom);
        if (jour == null) {
            if (h != null) {
                Ui.marquerErreur(heure);
                throw new IllegalArgumentException(nom + " : choisissez aussi le jour.");
            }
            return null;
        }
        return LocalDateTime.of(jour, h == null ? parDefaut : h);
    }

    // --- Ordonnancement automatique -----------------------------------

    private Node carteOrdonnancement() {
        LocalTime plusTot = tournoi.getTableaux().stream()
                .map(Tableau::getHoraires)
                .filter(h -> h != null && h.getDebutMatchs() != null)
                .map(h -> h.getDebutMatchs().toLocalTime())
                .min(LocalTime::compareTo).orElse(LocalTime.of(8, 30));
        TextField premier = Ui.champ("ex. 8h30");
        premier.setText(Saisie.heure(plusTot));
        TextField intervalle = Ui.champ("ex. 90");
        intervalle.setText("90");
        TextField intervalle2 = Ui.champ("vide = même écart");
        intervalle2.setText("120");
        javafx.scene.control.CheckBox damesEnDernier =
                new javafx.scene.control.CheckBox("Tableaux réservés aux dames en dernier de leur journée");
        damesEnDernier.setSelected(true);
        TextField finPointage = Ui.champ("ex. 30");
        finPointage.setText("30");
        TextField ouverture = Ui.champ("ex. 60");
        ouverture.setText("60");

        GridPane g = grilleDeuxColonnes();
        g.add(Ui.champEtiquete("Début du premier tableau, chaque jour", premier), 0, 0);
        g.add(Ui.champEtiquete("Écart entre le 1er et le 2e tableau (minutes)", intervalle), 1, 0);
        g.add(Ui.champEtiquete("Puis écart suivant, en alternance (minutes)", intervalle2), 0, 1);
        g.add(Ui.champEtiquete("Fin du pointage … minutes avant le début", finPointage), 1, 1);
        g.add(Ui.champEtiquete("Ouverture du pointage … minutes avant le début", ouverture), 0, 2);
        g.add(new VBox(6, new Label(" "), damesEnDernier), 1, 2);

        VBox resultat = new VBox(10);
        Label message = Ui.message();
        Button proposer = Ui.boutonSecondaire("Proposer un ordre");
        proposer.setOnAction(e -> {
            Ui.cacher(message);
            resultat.getChildren().clear();
            try {
                if (tournoi.getTableaux().isEmpty()) {
                    throw new IllegalArgumentException("Ajoutez d'abord des tableaux.");
                }
                boolean fige = tournoi.getTableaux().stream()
                        .anyMatch(t -> !t.getInscrits().isEmpty() || !t.getFileAttente().isEmpty());
                if (fige) {
                    throw new IllegalArgumentException("Des joueurs sont déjà inscrits : l'ordre des tableaux "
                            + "ne peut plus être changé automatiquement.");
                }
                LocalTime h = Saisie.lireHeure(premier.getText(), "Début du premier tableau");
                if (h == null) {
                    Ui.marquerErreur(premier);
                    throw new IllegalArgumentException("Indiquez l'heure du premier tableau.");
                }
                int pas = minutes(intervalle, "Écart entre deux tableaux", 1);
                List<Duration> ecarts = new java.util.ArrayList<>(List.of(Duration.ofMinutes(pas)));
                if (intervalle2.getText() != null && !intervalle2.getText().isBlank()) {
                    ecarts.add(Duration.ofMinutes(minutes(intervalle2, "Écart suivant", 1)));
                }
                int fin = minutes(finPointage, "Fin du pointage", 0);
                int ouv = minutes(ouverture, "Ouverture du pointage", 0);
                if (ouv < fin) {
                    Ui.marquerErreur(ouverture);
                    throw new IllegalArgumentException("Le pointage doit ouvrir avant de se terminer : "
                            + "ouverture ≥ fin du pointage.");
                }
                List<OrdonnancementTableaux.Creneau> creneaux = OrdonnancementTableaux.proposer(
                        tournoi.getTableaux(), joursDuTournoi(), h, ecarts, damesEnDernier.isSelected());
                Button appliquer = Ui.boutonPrimaire("Appliquer cet ordre");
                appliquer.setOnAction(ev -> {
                    OrdonnancementTableaux.appliquer(creneaux, Duration.ofMinutes(fin), Duration.ofMinutes(ouv));
                    enregistrer.run();
                    afficherListe();
                });
                Button annuler = Ui.boutonLien("Annuler");
                annuler.setOnAction(ev -> resultat.getChildren().clear());
                resultat.getChildren().addAll(apercuOrdre(creneaux, fin),
                        Ui.note("Rien n'est modifié tant que vous n'avez pas appliqué. Les jours, horaires et "
                                + "pointages des tableaux seront remplacés ; tout reste modifiable ensuite."),
                        Ui.actions(appliquer, annuler));
            } catch (IllegalArgumentException ex) {
                Ui.erreur(message, ex.getMessage());
            }
        });

        Node entete = Ui.ligne(8, Ui.puceDependance(Niveau.OPTIONNEL), Ui.titreCarte("Ordonnancement automatique"));
        return Ui.carte(entete,
                Ui.note("Range les tableaux du plus faible au plus fort, en place un sur x pour chacun des x jours "
                        + "du tournoi, puis alterne dans chaque journée un tableau faible et un tableau moyen : "
                        + "faible, moyen, faible + 1, moyen + 1… Deux tableaux de niveaux voisins ne se suivent "
                        + "jamais. Les tableaux toutes séries passent après les tableaux limités en points. Les "
                        + "écarts alternent : A → 1 h 30 → B → 2 h → C → 1 h 30 → D…"),
                g, message, Ui.actions(proposer), resultat);
    }

    private List<LocalDate> joursDuTournoi() {
        List<LocalDate> l = new java.util.ArrayList<>();
        LocalDate d = tournoi.getDateDebut();
        LocalDate f = tournoi.getDateFin();
        if (d == null || f == null) {
            throw new IllegalArgumentException("Renseignez d'abord les dates du tournoi (section Général).");
        }
        for (LocalDate x = d; !x.isAfter(f); x = x.plusDays(1)) {
            l.add(x);
        }
        return l;
    }

    private static int minutes(TextField champ, String nom, int minimum) {
        Integer v;
        try {
            v = Saisie.lireEntier(champ.getText(), nom);
        } catch (IllegalArgumentException e) {
            Ui.marquerErreur(champ);
            throw e;
        }
        if (v == null || v < minimum) {
            Ui.marquerErreur(champ);
            throw new IllegalArgumentException(nom + " : un nombre de minutes" + (minimum > 0 ? " (au moins "
                    + minimum + ")" : "") + ".");
        }
        return v;
    }

    /** Tableau de l'ordre proposé : jour, début, fin du pointage, tableau, joueurs admis. */
    private static Node apercuOrdre(List<OrdonnancementTableaux.Creneau> creneaux, int finPointage) {
        GridPane g = new GridPane();
        g.getStyleClass().add("grille-tableaux");
        String[] cols = {"Jour", "Début", "Fin du pointage", "Tableau", "Joueurs admis"};
        for (int c = 0; c < cols.length; c++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setHgrow(c >= 3 ? Priority.SOMETIMES : Priority.NEVER);
            g.getColumnConstraints().add(cc);
            Label l = Ui.enteteColonne(cols[c]);
            l.getStyleClass().add("cellule-entete");
            l.setMinWidth(Region.USE_PREF_SIZE);
            l.setMaxWidth(Double.MAX_VALUE);   // bande d'en-tête continue
            g.add(l, c, 0);
        }
        int r = 1;
        g.add(filet(), 0, r++, cols.length, 1);
        for (OrdonnancementTableaux.Creneau cr : creneaux) {
            Node[] cellules = {
                    new Label(Saisie.jourCourt(cr.jour())),
                    Ui.gras(Saisie.heure(cr.debut())),
                    new Label(Saisie.heure(cr.debut().minusMinutes(finPointage))),
                    Ui.gras(cr.tableau().getNom()),
                    new Label(publicVise(cr.tableau().getConfiguration()) + " · "
                            + points(cr.tableau().getConfiguration()))
            };
            for (int c = 0; c < cellules.length; c++) {
                cellules[c].getStyleClass().add("cellule");
                ((Region) cellules[c]).setMinWidth(Region.USE_PREF_SIZE);
                g.add(cellules[c], c, r);
            }
            r++;
            g.add(filet(), 0, r++, cols.length, 1);
        }
        return g;
    }

    // --- Grille des tableaux -------------------------------------------

    private Node carteTableaux() {
        Button ajouter = Ui.boutonPrimaire("Ajouter un tableau");
        ajouter.setOnAction(e -> ouvrirFormulaire(null));
        // B035 : liste des tableaux verrouillée dès la demande d'homologation (ni ajout ni retrait).
        boolean listeFigee = verrous.estVerrouille(tournoi, ChampHomologation.LISTE_TABLEAUX);
        Node entete = listeFigee
                ? Ui.enteteAvecActions(Ui.titreAvecCompteur("Tableaux", tournoi.getTableaux().size()))
                : Ui.enteteAvecActions(Ui.titreAvecCompteur("Tableaux", tournoi.getTableaux().size()), ajouter);
        String motifVerrou = new PresentationPreparationTournoi(tournoi)
                .motifVerrouSection(PresentationPreparationTournoi.SECTION_TABLEAUX);

        boolean dames = tournoi.getTableaux().stream()
                .anyMatch(t -> t.getConfiguration().getSexeRequis() == Sexe.FEMME);
        HBox regleDames = Ui.ligne(8,
                Ui.puceDependance(Niveau.OBLIGATOIRE_HOMOLOGATION),
                Ui.texte("Au moins un tableau réservé aux dames (IX.104)"),
                Ui.espace(),
                Ui.badgeEtat(Niveau.OBLIGATOIRE_HOMOLOGATION, dames));

        if (tournoi.getTableaux().isEmpty()) {
            VBox vide = new VBox(6,
                    Ui.texte("Aucun tableau pour l'instant."),
                    Ui.discret("Ajoutez-en un avec « Ajouter un tableau » : nom, joueurs admis, jour "
                            + "et horaires, places, prix et récompenses."));
            vide.getStyleClass().add("grille-vide");
            return motifVerrou == null ? Ui.carte(entete, regleDames, vide)
                    : Ui.carte(entete, Ui.bandeauVerrou(motifVerrou), regleDames, vide);
        }
        return motifVerrou == null ? Ui.carte(entete, regleDames, grille())
                : Ui.carte(entete, Ui.bandeauVerrou(motifVerrou), regleDames, grille());
    }

    private static final String[] COLONNES = {"Nom", "Jour", "Début", "Points", "Public", "Places", "Prix", "Récompenses", ""};

    private Node grille() {
        GridPane g = new GridPane();
        g.getStyleClass().add("grille-tableaux");
        g.setHgap(0);
        g.setVgap(0);
        for (int c = 0; c < COLONNES.length; c++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setHgrow(c == 0 || c == 3 || c == 7 ? Priority.SOMETIMES : Priority.NEVER);
            g.getColumnConstraints().add(cc);
            Label l = Ui.enteteColonne(COLONNES[c]);
            l.getStyleClass().add("cellule-entete");
            l.setMinWidth(Region.USE_PREF_SIZE);
            l.setMaxWidth(Double.MAX_VALUE);   // bande d'en-tête continue
            g.add(l, c, 0);
        }
        // Les filets sont des lignes pleine largeur entre les rangées (et non des
        // bordures de cellules) : ils restent continus et alignés quelle que soit
        // la hauteur de chaque cellule (boutons « Modifier / Retirer » compris).
        int r = 1;
        g.add(filet(), 0, r++, COLONNES.length, 1);
        // Affichage dans l'ordre du planning : jour, puis heure de début (sans horaire en fin).
        List<Tableau> ordre = new java.util.ArrayList<>(tournoi.getTableaux());
        ordre.sort(java.util.Comparator.comparing((Tableau x) -> x.getHoraires() == null
                        || x.getHoraires().getDebutMatchs() == null ? null : x.getHoraires().getDebutMatchs(),
                java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())));
        for (Tableau t : ordre) {
            HorairesTableau h = t.getHoraires();
            ConfigurationTableau cfg = t.getConfiguration();
            Node[] cellules = {
                    Ui.gras(t.getNom()),
                    texteOuTiret(h == null ? null : Saisie.jourCourt(h.getJour())),
                    texteOuTiret(h == null || h.getDebutMatchs() == null ? null : Saisie.heure(h.getDebutMatchs().toLocalTime())),
                    new Label(points(cfg)),
                    pastillePublic(cfg),
                    new Label(places(cfg)),
                    new Label(prix(cfg.getTarif())),
                    new Label(recompenses(cfg.getRecompenses())),
                    actions(t)
            };
            for (int c = 0; c < cellules.length; c++) {
                Node n = cellules[c];
                n.getStyleClass().add("cellule");
                if (n instanceof Label l && (c == 0 || c == 3 || c == 7)) {
                    // Colonnes de texte : elles passent à la ligne plutôt que de pousser
                    // les actions hors de l'écran quand la fenêtre est étroite.
                    l.setWrapText(true);
                    l.setMinWidth(72);
                } else if (n instanceof Region reg) {
                    reg.setMinWidth(Region.USE_PREF_SIZE);   // jamais de texte tronqué
                }
                GridPane.setValignment(n, javafx.geometry.VPos.CENTER);
                g.add(n, c, r);
            }
            r++;
            g.add(filet(), 0, r++, COLONNES.length, 1);
        }
        return g;
    }

    /** Public du tableau en pastille : bleue pour un tableau réservé, grise sinon. */
    private static Node pastillePublic(ConfigurationTableau cfg) {
        StatutPastille statut = cfg.getSexeRequis() == null ? StatutPastille.NEUTRE : StatutPastille.INFO;
        return Ui.ligne(0, Ui.pastille(publicVise(cfg), statut));
    }

    private static Region filet() {
        Region f = new Region();
        f.getStyleClass().add("filet-ligne");
        f.setMinHeight(1);
        f.setPrefHeight(1);
        f.setMaxHeight(1);
        f.setMaxWidth(Double.MAX_VALUE);
        return f;
    }

    private HBox actions(Tableau t) {
        Button modifier = Ui.boutonLien("Modifier");
        modifier.setOnAction(e -> ouvrirFormulaire(t));
        Button retirer = Ui.boutonLien("Retirer");
        retirer.getStyleClass().add("lien-danger");
        retirer.setOnAction(e -> {
            try {
                ResultatModificationTournoi r = verrous.retirerTableau(tournoi, t);
                if (!r.estAppliquee()) {
                    retirer.setDisable(true);
                    retirer.setTooltip(new javafx.scene.control.Tooltip(r.getMessage()));
                    return;
                }
                enregistrer.run();
                afficherListe();
            } catch (IllegalStateException ex) {
                retirer.setText("Inscrits : impossible");
                retirer.setDisable(true);
            }
        });
        boolean fige = !t.getInscrits().isEmpty() || !t.getFileAttente().isEmpty();
        modifier.setDisable(fige);
        retirer.setDisable(fige);
        if (verrous.estVerrouille(tournoi, ChampHomologation.LISTE_TABLEAUX)) {
            Ui.cacher(retirer);
        }
        if (fige) {
            // Avec des inscrits, les règles du tableau sont figées, mais pas son créneau
            // (utile après un import : jour, heure et pointage restent à poser).
            Button horaires = Ui.boutonLien("Horaires");
            horaires.setOnAction(e -> ouvrirHoraires(t));
            return Ui.ligne(10, horaires, retirer);
        }
        return Ui.ligne(10, modifier, retirer);
    }

    /** Jour, début et pointage d'un tableau qui a déjà des inscrits (ses règles restent figées). */
    private void ouvrirHoraires(Tableau t) {
        com.pingmanager.model.HorairesTableau h = t.getHoraires() == null
                ? new com.pingmanager.model.HorairesTableau() : t.getHoraires();
        LocalDate d = tournoi.getDateDebut();
        LocalDate f = tournoi.getDateFin() == null ? d : tournoi.getDateFin();
        DatePicker jour = Saisie.selecteurDate(h.getJour() != null ? h.getJour() : (d != null && d.equals(f) ? d : null), d, f);
        TextField debut = Ui.champ("ex. 9h");
        TextField debutPointage = Ui.champ("ex. 8h");
        TextField finPointage = Ui.champ("ex. 8h45");
        if (h.getDebutMatchs() != null) {
            debut.setText(Saisie.heure(h.getDebutMatchs().toLocalTime()));
        }
        if (h.getDebutPointage() != null) {
            debutPointage.setText(Saisie.heure(h.getDebutPointage().toLocalTime()));
        }
        if (h.getFinPointage() != null) {
            finPointage.setText(Saisie.heure(h.getFinPointage().toLocalTime()));
        }
        Label message = Ui.message();
        Button enregistrerH = Ui.boutonPrimaire("Enregistrer les horaires");
        Button annuler = Ui.boutonSecondaire("Annuler");
        annuler.setOnAction(e -> afficherListe());
        enregistrerH.setOnAction(e -> {
            try {
                if (jour.getValue() == null) {
                    throw new IllegalArgumentException("Choisissez le jour du tableau.");
                }
                LocalTime deb = Saisie.lireHeure(debut.getText(), "Heure de début");
                if (deb == null) {
                    throw new IllegalArgumentException("Heure de début : obligatoire.");
                }
                LocalTime dp = Saisie.lireHeure(debutPointage.getText(), "Début du pointage");
                LocalTime fp = Saisie.lireHeure(finPointage.getText(), "Fin du pointage");
                if (dp != null && fp != null && !dp.isBefore(fp)) {
                    throw new IllegalArgumentException("Le pointage doit commencer avant de finir.");
                }
                if (fp != null && fp.isAfter(deb)) {
                    throw new IllegalArgumentException("Le pointage doit finir au plus tard au début du tableau.");
                }
                LocalDate j = jour.getValue();
                // B035 : le jour et l'heure de début font partie du dossier d'homologation.
                LocalDateTime ancienDebut = t.getHoraires() == null ? null : t.getHoraires().getDebutMatchs();
                if (!j.atTime(deb).equals(ancienDebut)) {
                    String refus = PresentationPreparationTournoi.messageModification(
                            verrous.verifier(tournoi, ChampHomologation.TABLEAU_DEBUT));
                    if (refus != null) {
                        throw new IllegalArgumentException(refus);
                    }
                }
                t.setHoraires(new com.pingmanager.model.HorairesTableau(dp == null ? null : j.atTime(dp),
                        fp == null ? null : j.atTime(fp), j.atTime(deb), h.getFinTheorique() == null ? null
                        : j.atTime(h.getFinTheorique().toLocalTime())));
                enregistrer.run();
                afficherListe();
            } catch (IllegalArgumentException ex) {
                Ui.erreur(message, ex.getMessage());
            }
        });
        Button retourListe = Ui.boutonLien("← Tableaux");
        retourListe.setOnAction(e -> afficherListe());
        GridPane g = grilleDeuxColonnes();
        g.add(Ui.champEtiquete("Jour", jour), 0, 0);
        g.add(Ui.champEtiquete("Heure de début", debut), 1, 0);
        g.add(Ui.champEtiquete("Début du pointage", debutPointage), 0, 1);
        g.add(Ui.champEtiquete("Fin du pointage", finPointage), 1, 1);
        poser(Ui.page(900, new VBox(6, retourListe, Ui.titre("Horaires — " + t.getNom())),
                Ui.carte("Créneau du tableau", Ui.aide("Le tableau a des inscrits : ses conditions (points, places, "
                        + "tarifs) ne changent plus, mais son jour et ses horaires peuvent être posés ou corrigés."),
                        g, message, Ui.ligne(10, enregistrerH, annuler))));
    }

    private void ouvrirFormulaire(Tableau existant) {
        FormulaireTableau f = new FormulaireTableau(tournoi, existant,
                nouveau -> {
                    // B035 : par les méthodes gardées ; un refus remonte au formulaire (motif affiché).
                    if (existant == null) {
                        exigerAppliquee(verrous.ajouterTableau(tournoi, nouveau));
                    } else {
                        String ancien = existant.getNom();
                        exigerAppliquee(verrous.remplacerTableau(tournoi, existant, nouveau));
                        // Le tableau exigé pour le surplus des joueuses suit le renommage.
                        if (ancien.equals(tournoi.getQuotasTableaux().getTableauBonusJoueuses())
                                && !ancien.equals(nouveau.getNom())) {
                            tournoi.setQuotasTableaux(tournoi.getQuotasTableaux()
                                    .avecTableauBonusJoueuses(nouveau.getNom()));
                        }
                    }
                    enregistrer.run();
                    afficherListe();
                },
                this::afficherListe);
        poser(f.build());
    }

    /** @throws IllegalStateException avec le motif si la modification est refusée par un verrou */
    private static void exigerAppliquee(ResultatModificationTournoi r) {
        if (!r.estAppliquee()) {
            throw new IllegalStateException(r.getMessage());
        }
    }

    // ==================================================================
    // Résumés affichés dans la grille (logique partagée avec le formulaire)
    // ==================================================================

    // Résumés : logique partagée avec le règlement généré (cœur).
    static String points(ConfigurationTableau c) {
        return LibellesTableau.points(c);
    }

    static String type(ConfigurationTableau c) {
        return LibellesTableau.type(c);
    }

    static String publicVise(ConfigurationTableau c) {
        return LibellesTableau.publicVise(c);
    }

    static String places(ConfigurationTableau c) {
        return LibellesTableau.places(c);
    }

    static String prix(TarifInscription t) {
        return LibellesTableau.prix(t);
    }

    static String recompenses(GrilleRecompenses g) {
        return LibellesTableau.recompenses(g);
    }

    private static Label texteOuTiret(String s) {
        return new Label(s == null || s.isBlank() ? "—" : s);
    }

    static GridPane grilleDeuxColonnes() {
        GridPane g = new GridPane();
        g.setHgap(16);
        g.setVgap(14);
        for (int i = 0; i < 2; i++) {
            ColumnConstraints c = new ColumnConstraints();
            c.setPercentWidth(50);
            g.getColumnConstraints().add(c);
        }
        return g;
    }
}
