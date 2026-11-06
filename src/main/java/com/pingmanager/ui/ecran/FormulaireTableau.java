package com.pingmanager.ui.ecran;

import com.pingmanager.core.ServiceCompletudeTournoi.Niveau;
import com.pingmanager.model.CategorieAge;
import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.GrilleRecompenses;
import com.pingmanager.model.HorairesTableau;
import com.pingmanager.model.ModeLimiteClassement;
import com.pingmanager.model.Recompense;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.TarifInscription;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.TypeRecompense;
import com.pingmanager.model.format.FormatTournoi;
import com.pingmanager.model.format.RegistreFormats;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Formulaire complet d'un tableau (création ou modification), ouvert depuis
 * l'{@link EcranTableaux écran Tableaux}. À la validation, le tableau est
 * construit, contrôlé, puis rendu à l'appelant qui l'ajoute à la grille.
 *
 * <p>Groupes : nom · joueurs admis (points, catégories d'âge, sexe) · jour et
 * horaires (un seul jour, compris dans les dates du tournoi) · places · prix
 * d'inscription (en ligne et/ou sur place) · récompenses et lots · déroulement.</p>
 */
final class FormulaireTableau {

    private static final Niveau H = Niveau.OBLIGATOIRE_HOMOLOGATION;
    private static final Niveau P = Niveau.OBLIGATOIRE_PUBLICATION;
    private static final Niveau R = Niveau.RECOMMANDE;
    private static final Niveau O = Niveau.OPTIONNEL;

    /** Noms usuels proposés en plus de la prochaine lettre libre. */
    static final String NON_NUMEROTES = "Non numérotés";
    static final String OPEN = "Open";
    static final String FEMININ = "Féminin";

    private final Tournoi tournoi;
    private final Tableau existant;
    private final Consumer<Tableau> onValider;
    private final Runnable onAnnuler;

    // Champs
    private ComboBox<String> nom;
    private final ToggleGroup modePoints = new ToggleGroup();
    private RadioButton parTranche;
    private RadioButton pointMax;
    private RadioButton toutesSeries;
    private final TextField pointsMin = Ui.champ("ex. 500");
    private final TextField pointsMaxi = Ui.champ("ex. 899");
    private final TextField exclusion = Ui.champ("ex. 1000");
    private final Map<CategorieAge, CheckBox> categories = new EnumMap<>(CategorieAge.class);
    private ComboBox<String> sexe;
    private RadioButton toutesCategories;
    private RadioButton selectionCategories;
    private DatePicker jour;
    private final TextField debutPointage = Ui.champ("ex. 8h");
    private final TextField finPointage = Ui.champ("ex. 8h45");
    private final TextField debutMatchs = Ui.champ("ex. 9h");
    private final TextField finTheorique = Ui.champ("ex. 13h");
    private final TextField capacite = Ui.champ("ex. 48");
    private final TextField attente = Ui.champ("0 = pas de liste");
    private final CheckBox enLigne = new CheckBox("Inscription en ligne");
    private final TextField montantEnLigne = Ui.champ("montant en €");
    private final CheckBox surPlace = new CheckBox("Inscription sur place");
    private final TextField montantSurPlace = Ui.champ("montant en €");
    private final VBox lignesRecompenses = new VBox(8);
    private final List<LigneRecompense> recompenses = new ArrayList<>();
    private ComboBox<FormatTournoi> format;
    private final CheckBox petiteFinale = new CheckBox("Petite finale (match pour la 3e place)");

    private static final String SEXE_MIXTE = "Mixte (ouvert à tous)";
    private static final String SEXE_HOMMES = "Hommes uniquement";
    private static final String SEXE_FEMMES = "Femmes uniquement";

    FormulaireTableau(Tournoi tournoi, Tableau existant, Consumer<Tableau> onValider, Runnable onAnnuler) {
        this.tournoi = tournoi;
        this.existant = existant;
        this.onValider = onValider;
        this.onAnnuler = onAnnuler;
    }

    Node build() {
        ConfigurationTableau cfg = existant != null ? existant.getConfiguration() : null;
        HorairesTableau h = existant != null ? existant.getHoraires() : null;

        Label erreur = Ui.message();
        Button valider = Ui.boutonPrimaire(existant == null ? "Valider le tableau" : "Enregistrer le tableau");
        valider.setDefaultButton(true);
        valider.setOnAction(e -> {
            Ui.cacher(erreur);
            try {
                onValider.accept(construire());
            } catch (IllegalArgumentException | IllegalStateException ex) {
                Ui.erreur(erreur, ex.getMessage());
            }
        });
        Button annuler = Ui.boutonSecondaire("Annuler");
        annuler.setCancelButton(true);
        annuler.setOnAction(e -> onAnnuler.run());

        VBox corps = new VBox(22,
                groupe("Nom du tableau", true, groupeNom()),
                groupe("Joueurs admis", false, groupeJoueurs(cfg)),
                groupe("Jour et horaires", false, groupeHoraires(h)),
                groupe("Places", false, groupePlaces(cfg)),
                groupe("Prix d'inscription", false, groupePrix(cfg)),
                groupe("Récompenses et lots", false, groupeRecompenses(cfg)),
                groupe("Déroulement", false, groupeFormat(cfg)));

        VBox enTete = new VBox(6,
                Ui.titre(existant == null ? "Nouveau tableau" : "Tableau « " + existant.getNom() + " »"),
                Ui.discret("Une fois validé, le tableau rejoint la grille de la section. "
                        + "Rien n'est enregistré avant la validation."));
        verrouillerDossier(enTete);

        // Chaque groupe de champs devient sa propre carte (règles B012 de theme.css).
        Node cartes = Ui.carteNue(corps);
        cartes.getStyleClass().add("formulaire-cartes");
        return Ui.page(760, enTete, Ui.legende(EnumSet.of(H, P, R, O)), cartes,
                erreur, Ui.actions(valider, annuler));
    }

    /**
     * B035 : dès la demande d'homologation, nom, catégorie de points, public et
     * début des matchs d'un tableau ne sont plus modifiables (grisés, motif en
     * tête) ; format, places, tarifs et récompenses restent libres.
     */
    private void verrouillerDossier(VBox enTete) {
        com.pingmanager.core.ServiceVerrousHomologation verrous = new com.pingmanager.core.ServiceVerrousHomologation();
        if (existant == null || !verrous.estVerrouille(tournoi, com.pingmanager.model.ChampHomologation.TABLEAU_NOM)) {
            return;
        }
        String motif = verrous.motifVerrou(tournoi, List.of(com.pingmanager.model.ChampHomologation.TABLEAU_NOM,
                com.pingmanager.model.ChampHomologation.TABLEAU_CATEGORIE_POINTS,
                com.pingmanager.model.ChampHomologation.TABLEAU_PUBLIC,
                com.pingmanager.model.ChampHomologation.TABLEAU_DEBUT));
        enTete.getChildren().add(Ui.bandeauVerrou(motif));
        for (javafx.scene.control.Control c : List.of(nom, parTranche, pointMax, toutesSeries, pointsMin, pointsMaxi,
                sexe, jour, debutMatchs)) {
            if (c != null) {
                Ui.verrouiller(c, motif);
            }
        }
    }

    private static VBox groupe(String titre, boolean premier, Node... contenu) {
        Label t = new Label(titre);
        t.getStyleClass().add("groupe-titre");
        VBox v = new VBox(12, t);
        v.getChildren().addAll(contenu);
        v.getStyleClass().add("groupe-champs");
        if (premier) {
            v.getStyleClass().add("premier");
        }
        return v;
    }

    // ------------------------------------------------------------------
    // Nom
    // ------------------------------------------------------------------

    private Node groupeNom() {
        String lettre = prochaineLettre();
        nom = new ComboBox<>();
        nom.setEditable(true);
        nom.getItems().addAll(lettre, NON_NUMEROTES, OPEN, FEMININ);
        nom.setValue(existant != null ? existant.getNom() : lettre);
        nom.setMaxWidth(Double.MAX_VALUE);
        nom.getEditor().textProperty().addListener((o, a, x) -> nom.getStyleClass().remove(Ui.EN_ERREUR));
        // Choisir un nom usuel pré-règle les joueurs admis (modifiable ensuite).
        nom.getSelectionModel().selectedItemProperty().addListener((o, a, n) -> appliquerNomUsuel(n));

        VBox champ = Ui.champEtiquete("Nom", nom, H, nom.getEditor().textProperty(),
                () -> !nom.getEditor().getText().isBlank());
        return new VBox(6, champ,
                Ui.note("Par défaut la prochaine lettre libre (" + lettre + "). Noms usuels : Non numérotés, "
                        + "Open, Féminin. Le nom reste libre."));
    }

    private String prochaineLettre() {
        for (char c = 'A'; c <= 'Z'; c++) {
            String l = String.valueOf(c);
            boolean pris = tournoi.getTableaux().stream()
                    .anyMatch(t -> t != existant && t.getNom().equalsIgnoreCase(l));
            if (!pris) {
                return l;
            }
        }
        return "Tableau " + (tournoi.getTableaux().size() + 1);
    }

    private void appliquerNomUsuel(String n) {
        if (n == null || sexe == null) {
            return;
        }
        switch (n) {
            case FEMININ -> sexe.setValue(SEXE_FEMMES);
            case OPEN -> {
                toutesSeries.setSelected(true);
                exclusion.clear();
            }
            case NON_NUMEROTES -> {
                toutesSeries.setSelected(true);
                exclusion.setText("1000");
            }
            default -> {
                // une lettre : rien à pré-régler
            }
        }
    }

    // ------------------------------------------------------------------
    // Joueurs admis
    // ------------------------------------------------------------------

    private Node groupeJoueurs(ConfigurationTableau cfg) {
        parTranche = radio("Par tranche de points");
        pointMax = radio("Points maximum");
        toutesSeries = radio("Toutes séries");
        HBox choix = Ui.ligne(18, parTranche, pointMax, toutesSeries);

        VBox blocMin = Ui.champEtiquete("Points minimum", pointsMin, H);
        VBox blocMax = Ui.champEtiquete("Points maximum", pointsMaxi, H);
        VBox blocExcl = Ui.champEtiquete("Exclure les numérotés : rangs nationaux 1 à", exclusion, O);
        GridPane grillePoints = EcranTableaux.grilleDeuxColonnes();
        grillePoints.add(blocMin, 0, 0);
        grillePoints.add(blocMax, 1, 0);
        grillePoints.add(blocExcl, 0, 1, 2, 1);

        Runnable majMode = () -> {
            boolean tranche = parTranche.isSelected();
            boolean max = pointMax.isSelected();
            Ui.afficherSi(blocMin, tranche);
            Ui.afficherSi(blocMax, tranche || max);
            Ui.afficherSi(blocExcl, !tranche);
            // En « points maximum », le champ passe dans la colonne de gauche.
            GridPane.setColumnIndex(blocMax, tranche ? 1 : 0);
        };
        modePoints.selectedToggleProperty().addListener((o, a, n) -> majMode.run());

        if (cfg == null || cfg.getModeLimite() == ModeLimiteClassement.TRANCHE) {
            parTranche.setSelected(true);
        } else if (cfg.getModeLimite() == ModeLimiteClassement.POINT_MAX) {
            pointMax.setSelected(true);
        } else {
            toutesSeries.setSelected(true);
        }
        if (cfg != null) {
            if (cfg.getModeLimite() == ModeLimiteClassement.TRANCHE && cfg.getPointsMin() > 0) {
                pointsMin.setText(String.valueOf(cfg.getPointsMin()));
            }
            if (cfg.getModeLimite() != ModeLimiteClassement.TOUTES_SERIES && cfg.getPointsMax() > 0) {
                pointsMaxi.setText(String.valueOf(cfg.getPointsMax()));
            }
            if (cfg.aExclusionRangNational()) {
                exclusion.setText(String.valueOf(cfg.getExclusionRangNational()));
            }
        }
        majMode.run();

        // Catégories d'âge : « Toutes catégories » par défaut ; « Sélectionner des
        // catégories » déplie la liste. Les cases cochées sont conservées même
        // repliées, mais ne comptent que si la sélection est active.
        FlowPane jeunes = new FlowPane(14, 8);
        FlowPane adultes = new FlowPane(14, 8);
        for (CategorieAge c : CategorieAge.values()) {
            CheckBox cb = new CheckBox(c.getCodeU() != null ? c.getLibelle() + " (" + c.getCodeU() + ")" : c.getLibelle());
            cb.setSelected(cfg != null && cfg.getCategoriesAutorisees().contains(c));
            categories.put(c, cb);
            (c.estJeune() ? jeunes : adultes).getChildren().add(cb);
        }
        Button toutJeunes = Ui.boutonLien("Tous les jeunes");
        toutJeunes.setOnAction(e -> categories.forEach((c, cb) -> cb.setSelected(c.estJeune() || cb.isSelected())));
        Button toutVeterans = Ui.boutonLien("Tous les vétérans");
        toutVeterans.setOnAction(e -> categories.forEach((c, cb) -> cb.setSelected(c.estVeteran() || cb.isSelected())));
        Button aucune = Ui.boutonLien("Tout décocher");
        aucune.setOnAction(e -> categories.values().forEach(cb -> cb.setSelected(false)));
        VBox liste = new VBox(8,
                Ui.note("Jeunes"), jeunes,
                Ui.note("Adultes"), adultes,
                Ui.ligne(16, toutJeunes, toutVeterans, aucune));
        liste.getStyleClass().add("liste-depliee");

        toutesCategories = new RadioButton("Toutes catégories");
        selectionCategories = new RadioButton("Sélectionner des catégories");
        ToggleGroup choixCategories = new ToggleGroup();
        toutesCategories.setToggleGroup(choixCategories);
        selectionCategories.setToggleGroup(choixCategories);
        boolean restreint = cfg != null && !cfg.getCategoriesAutorisees().isEmpty();
        (restreint ? selectionCategories : toutesCategories).setSelected(true);
        Label resume = Ui.note("");
        Runnable majCategories = () -> {
            Ui.afficherSi(liste, selectionCategories.isSelected());
            long n = categories.values().stream().filter(CheckBox::isSelected).count();
            resume.setText(selectionCategories.isSelected()
                    ? (n == 0 ? "Aucune catégorie cochée pour l'instant." : n + " catégorie(s) cochée(s).")
                    : "Tous les âges sont admis.");
        };
        choixCategories.selectedToggleProperty().addListener((o, a, x) -> majCategories.run());
        categories.values().forEach(cb -> cb.selectedProperty().addListener((o, a, x) -> majCategories.run()));
        majCategories.run();

        VBox blocCategories = new VBox(8,
                Ui.libelleChamp("Catégories concernées"),
                Ui.ligne(18, toutesCategories, selectionCategories),
                resume,
                liste);

        sexe = new ComboBox<>();
        sexe.getItems().addAll(SEXE_MIXTE, SEXE_HOMMES, SEXE_FEMMES);
        Sexe s = cfg == null ? null : cfg.getSexeRequis();
        sexe.setValue(s == Sexe.FEMME ? SEXE_FEMMES : s == Sexe.HOMME ? SEXE_HOMMES : SEXE_MIXTE);
        if (existant == null && FEMININ.equals(nom.getValue())) {
            sexe.setValue(SEXE_FEMMES);
        }

        return new VBox(14,
                new VBox(8, Ui.libelleChamp("Catégorie de points"), choix),
                grillePoints,
                blocCategories,
                Ui.champEtiquete("Sexe", sexe, null, null, null));
    }

    private RadioButton radio(String texte) {
        RadioButton r = new RadioButton(texte);
        r.setToggleGroup(modePoints);
        return r;
    }

    // ------------------------------------------------------------------
    // Jour et horaires
    // ------------------------------------------------------------------

    private Node groupeHoraires(HorairesTableau h) {
        LocalDate d = tournoi.getDateDebut();
        LocalDate f = tournoi.getDateFin() != null ? tournoi.getDateFin() : d;
        LocalDate valeur = h != null ? h.getJour() : null;
        if (valeur == null && d != null && d.equals(f)) {
            valeur = d; // tournoi d'un jour : jour pré-choisi
        }
        jour = Saisie.selecteurDate(valeur, d, f);
        if (h != null) {
            debutPointage.setText(heure(h.getDebutPointage()));
            finPointage.setText(heure(h.getFinPointage()));
            debutMatchs.setText(heure(h.getDebutMatchs()));
            finTheorique.setText(heure(h.getFinTheorique()));
        }

        String bornes = d == null
                ? "Les dates du tournoi ne sont pas renseignées (section Général) : le jour ne peut pas être contrôlé."
                : d.equals(f)
                ? "Un tableau se joue sur un seul jour : ici le " + Saisie.jourLong(d) + "."
                : "Un tableau se joue sur un seul jour, compris entre le " + Saisie.jourLong(d)
                        + " et le " + Saisie.jourLong(f) + ".";

        GridPane g = EcranTableaux.grilleDeuxColonnes();
        g.add(Ui.champEtiquete("Jour", jour, H, jour.valueProperty(), () -> jour.getValue() != null), 0, 0);
        g.add(Ui.champEtiquete("Heure de début", debutMatchs, H), 1, 0);
        g.add(Ui.champEtiquete("Début du pointage", debutPointage, R), 0, 1);
        g.add(Ui.champEtiquete("Fin du pointage", finPointage, R), 1, 1);
        g.add(Ui.champEtiquete("Fin théorique", finTheorique, O), 0, 2);
        return new VBox(10, Ui.note(bornes), g);
    }

    private static String heure(LocalDateTime dt) {
        return dt == null ? "" : Saisie.heure(dt.toLocalTime());
    }

    // ------------------------------------------------------------------
    // Places, prix
    // ------------------------------------------------------------------

    private Node groupePlaces(ConfigurationTableau cfg) {
        if (cfg != null) {
            capacite.setText(cfg.getPlacesMax() > 0 ? String.valueOf(cfg.getPlacesMax()) : "");
            attente.setText(cfg.getTailleMaxListeAttente() > 0 ? String.valueOf(cfg.getTailleMaxListeAttente()) : "");
        }
        GridPane g = EcranTableaux.grilleDeuxColonnes();
        g.add(Ui.champEtiquete("Capacité (joueurs)", capacite, P), 0, 0);
        g.add(Ui.champEtiquete("Liste d'attente (places)", attente, O), 1, 0);
        return g;
    }

    private Node groupePrix(ConfigurationTableau cfg) {
        TarifInscription t = cfg == null ? null : cfg.getTarif();
        enLigne.setSelected(t == null || t.isEnLigneProposee());
        surPlace.setSelected(t == null || t.isSurPlaceProposee());
        if (t != null) {
            montantEnLigne.setText(t.isEnLigneProposee() ? Saisie.montantSaisie(t.getMontantEnLigne()) : "");
            montantSurPlace.setText(t.isSurPlaceProposee() ? Saisie.montantSaisie(t.getMontantSurPlace()) : "");
        }
        VBox blocEnLigne = Ui.champEtiquete("Montant en ligne (€)", montantEnLigne, P);
        VBox blocSurPlace = Ui.champEtiquete("Montant sur place (€)", montantSurPlace, P);
        montantEnLigne.disableProperty().bind(enLigne.selectedProperty().not());
        montantSurPlace.disableProperty().bind(surPlace.selectedProperty().not());

        GridPane g = EcranTableaux.grilleDeuxColonnes();
        g.add(enLigne, 0, 0);
        g.add(surPlace, 1, 0);
        g.add(blocEnLigne, 0, 1);
        g.add(blocSurPlace, 1, 1);
        return new VBox(10, g, Ui.note("Au moins une des deux. Indiquez 0 pour une inscription gratuite."));
    }

    // ------------------------------------------------------------------
    // Récompenses et lots
    // ------------------------------------------------------------------

    private static final String ARGENT = "Argent (€)";
    private static final String LOT = "Lot";
    private static final String REDUCTION = "Réduction (%)";
    private static final String PART = "% des inscriptions";

    /** Une ligne éditable de la grille de récompenses. */
    private final class LigneRecompense {
        final TextField de = Ui.champ("de");
        final TextField a = Ui.champ("à");
        final ComboBox<String> type = new ComboBox<>();
        final TextField valeur = Ui.champ("");
        final HBox noeud;

        LigneRecompense(GrilleRecompenses.Entree e) {
            type.getItems().addAll(ARGENT, PART, LOT, REDUCTION);
            type.setValue(ARGENT);
            de.setPrefColumnCount(3);
            a.setPrefColumnCount(3);
            type.valueProperty().addListener((o, x, n) -> majInvite());
            if (e != null) {
                de.setText(String.valueOf(e.getPlaceDebut()));
                if (e.getPlaceFin() != e.getPlaceDebut()) {
                    a.setText(String.valueOf(e.getPlaceFin()));
                }
                Recompense r = e.getRecompense();
                if (r.getType() == TypeRecompense.ARGENT) {
                    valeur.setText(Saisie.montantSaisie(r.getMontant()));
                } else if (r.getType() == TypeRecompense.AUTRE) {
                    type.setValue(LOT);
                    valeur.setText(r.getLibelle());
                } else if (r.getType() == TypeRecompense.PART_INSCRIPTIONS) {
                    type.setValue(PART);
                    valeur.setText(Saisie.montantSaisie(r.getPourcentage()));
                } else {
                    type.setValue(REDUCTION);
                    valeur.setText(Saisie.montantSaisie(r.getPourcentage()));
                }
            }
            majInvite();
            Button retirer = Ui.boutonLien("Retirer");
            retirer.getStyleClass().add("lien-danger");
            retirer.setOnAction(ev -> retirer());
            Label place = new Label("Place");
            Label tiret = new Label("à");
            HBox.setHgrow(valeur, Priority.ALWAYS);
            valeur.setMaxWidth(Double.MAX_VALUE);
            de.setMaxWidth(64);
            a.setMaxWidth(64);
            noeud = Ui.ligne(8, place, de, tiret, a, type, valeur, retirer);
        }

        private void retirer() {
            recompenses.remove(this);
            lignesRecompenses.getChildren().remove(noeud);
        }

        private void majInvite() {
            valeur.setPromptText(switch (type.getValue()) {
                case LOT -> "ex. coupe + raquette";
                case REDUCTION -> "ex. 20 (sur un achat partenaire)";
                case PART -> "ex. 50 (% des inscriptions du tableau)";
                default -> "ex. 50";
            });
        }

        void ajouterA(GrilleRecompenses.Builder b) {
            Integer debut = Saisie.lireEntier(de.getText(), "Récompense : place");
            if (debut == null || debut < 1) {
                Ui.marquerErreur(de);
                throw new IllegalArgumentException("Récompense : indiquez la place (1 = vainqueur).");
            }
            Integer fin = Saisie.lireEntier(a.getText(), "Récompense : place");
            int f = fin == null ? debut : fin;
            if (f < debut) {
                Ui.marquerErreur(a);
                throw new IllegalArgumentException("Récompense : « à » doit être après « de » (places "
                        + debut + " à " + f + ").");
            }
            Recompense r = switch (type.getValue()) {
                case LOT -> {
                    String lib = valeur.getText() == null ? "" : valeur.getText().trim();
                    if (lib.isEmpty()) {
                        Ui.marquerErreur(valeur);
                        throw new IllegalArgumentException("Récompense : décrivez le lot.");
                    }
                    yield Recompense.autre(lib);
                }
                case PART -> {
                    Double p = Saisie.lireMontant(valeur.getText(), "Part des inscriptions");
                    if (p == null || p <= 0 || p > 100) {
                        Ui.marquerErreur(valeur);
                        throw new IllegalArgumentException("Part des inscriptions : un pourcentage entre 1 et 100.");
                    }
                    yield Recompense.partInscriptions(p);
                }
                case REDUCTION -> {
                    Double p = Saisie.lireMontant(valeur.getText(), "Réduction");
                    if (p == null || p <= 0 || p > 100) {
                        Ui.marquerErreur(valeur);
                        throw new IllegalArgumentException("Réduction : un pourcentage entre 1 et 100.");
                    }
                    yield Recompense.reductionPourcentage(p);
                }
                default -> {
                    Double m = Saisie.lireMontant(valeur.getText(), "Récompense en argent");
                    if (m == null || m <= 0) {
                        Ui.marquerErreur(valeur);
                        throw new IllegalArgumentException("Récompense en argent : indiquez le montant.");
                    }
                    yield Recompense.argent(m);
                }
            };
            if (f == debut) {
                b.place(debut, r);
            } else {
                b.plage(debut, f, r);
            }
        }
    }

    private Node groupeRecompenses(ConfigurationTableau cfg) {
        if (cfg != null && cfg.getRecompenses() != null) {
            for (GrilleRecompenses.Entree e : cfg.getRecompenses().getEntrees()) {
                ajouterLigne(e);
            }
        }
        Button ajouter = Ui.boutonLien("+ Ajouter une récompense");
        ajouter.setOnAction(e -> ajouterLigne(null));
        return new VBox(10,
                Ui.ligne(6, Ui.puceDependance(R), Ui.libelleChamp("Dotation du tableau")),
                lignesRecompenses,
                ajouter,
                Ui.note("Argent, part des inscriptions, lot (coupe, matériel…) ou réduction. Une ligne par place "
                        + "ou par groupe de places, par exemple places 3 à 4. Les lots sont mis en avant à l'annonce. "
                        + "« % des inscriptions » : la dotation suit le remplissage (ex. tableau dames : 50 % au "
                        + "vainqueur, 30 % au finaliste, 10 % à chaque demi-finaliste), pour ne pas jouer à perte."));
    }

    private void ajouterLigne(GrilleRecompenses.Entree e) {
        LigneRecompense l = new LigneRecompense(e);
        recompenses.add(l);
        lignesRecompenses.getChildren().add(l.noeud);
    }

    // ------------------------------------------------------------------
    // Déroulement
    // ------------------------------------------------------------------

    private Node groupeFormat(ConfigurationTableau cfg) {
        format = new ComboBox<>();
        format.getItems().addAll(RegistreFormats.tousLesFormats());
        format.setConverter(new StringConverter<>() {
            @Override
            public String toString(FormatTournoi f) {
                return f == null ? "" : f.nom();
            }

            @Override
            public FormatTournoi fromString(String s) {
                return null;
            }
        });
        format.setValue(cfg != null && cfg.getFormat() != null ? cfg.getFormat() : RegistreFormats.formatParDefaut());
        // Clause d'organisateur, réglée par tableau (le chapitre IX ne l'impose pas).
        petiteFinale.setSelected(cfg != null ? cfg.avecPetiteFinale() : format.getValue().avecPetiteFinale());
        boolean figee = existant != null && existant.aPhaseFinale();
        petiteFinale.setDisable(figee);
        return new VBox(8,
                Ui.champEtiquete("Format de compétition", format, P),
                Ui.note("D'autres formats viendront ensuite (poules de 4, consolante…)."),
                petiteFinale,
                Ui.note(figee
                        ? "Le tableau final est déjà tiré : la petite finale ne peut plus être ajoutée ni retirée."
                        : "Sans petite finale, les deux perdants des demi-finales sont classés 3es ex aequo. "
                                + "Le choix est écrit dans le règlement du tournoi."));
    }

    // ------------------------------------------------------------------
    // Construction et contrôles
    // ------------------------------------------------------------------

    private Tableau construire() {
        // --- Nom (libre, unique dans le tournoi) ---
        String n = nom.getEditor().getText() == null ? "" : nom.getEditor().getText().trim();
        if (n.isEmpty()) {
            Ui.marquerErreur(nom);
            throw new IllegalArgumentException("Donnez un nom au tableau.");
        }
        boolean doublon = tournoi.getTableaux().stream()
                .anyMatch(t -> t != existant && t.getNom().equalsIgnoreCase(n));
        if (doublon) {
            Ui.marquerErreur(nom);
            throw new IllegalArgumentException("Un tableau « " + n + " » existe déjà dans ce tournoi.");
        }

        // --- Format, places ---
        FormatTournoi f = format.getValue() != null ? format.getValue() : RegistreFormats.formatParDefaut();
        ConfigurationTableau.Builder b = ConfigurationTableau.depuisFormat(f).petiteFinale(petiteFinale.isSelected());
        Integer cap = lire(capacite, () -> Saisie.lireEntier(capacite.getText(), "Capacité"));
        Integer att = lire(attente, () -> Saisie.lireEntier(attente.getText(), "Liste d'attente"));
        b.placesMax(cap == null ? 0 : cap).tailleMaxListeAttente(att == null ? 0 : att);

        // --- Points ---
        if (parTranche.isSelected()) {
            Integer min = lire(pointsMin, () -> Saisie.lireEntier(pointsMin.getText(), "Points minimum"));
            Integer max = lire(pointsMaxi, () -> Saisie.lireEntier(pointsMaxi.getText(), "Points maximum"));
            if (min == null || max == null) {
                Ui.marquerErreur(min == null ? pointsMin : pointsMaxi);
                throw new IllegalArgumentException("Tranche de points : indiquez le minimum et le maximum.");
            }
            if (min >= max) {
                Ui.marquerErreur(pointsMaxi);
                throw new IllegalArgumentException("Tranche de points : le maximum doit dépasser le minimum.");
            }
            b.tranche(min, max);
        } else {
            if (pointMax.isSelected()) {
                Integer max = lire(pointsMaxi, () -> Saisie.lireEntier(pointsMaxi.getText(), "Points maximum"));
                if (max == null || max == 0) {
                    Ui.marquerErreur(pointsMaxi);
                    throw new IllegalArgumentException("Points maximum : indiquez la limite.");
                }
                b.pointMax(max);
            } else {
                b.toutesSeries();
            }
            Integer excl = lire(exclusion, () -> Saisie.lireEntier(exclusion.getText(), "Exclusion des numérotés"));
            if (excl != null && excl > 0) {
                b.exclusionRangNational(excl);
            }
        }

        // --- Catégories, sexe ---
        if (selectionCategories.isSelected()) {
            boolean auMoinsUne = categories.values().stream().anyMatch(CheckBox::isSelected);
            if (!auMoinsUne) {
                throw new IllegalArgumentException("Catégories : cochez au moins une catégorie, "
                        + "ou choisissez « Toutes catégories ».");
            }
            categories.forEach((c, cb) -> {
                if (cb.isSelected()) {
                    b.autoriserCategorie(c);
                }
            });
        }
        String sx = sexe.getValue();
        b.sexeRequis(SEXE_FEMMES.equals(sx) ? Sexe.FEMME : SEXE_HOMMES.equals(sx) ? Sexe.HOMME : null);

        // --- Prix ---
        if (!enLigne.isSelected() && !surPlace.isSelected()) {
            throw new IllegalArgumentException("Prix : proposez l'inscription en ligne, sur place, ou les deux.");
        }
        Double pl = enLigne.isSelected()
                ? lire(montantEnLigne, () -> Saisie.lireMontant(montantEnLigne.getText(), "Montant en ligne")) : null;
        Double ps = surPlace.isSelected()
                ? lire(montantSurPlace, () -> Saisie.lireMontant(montantSurPlace.getText(), "Montant sur place")) : null;
        if (enLigne.isSelected() && pl == null) {
            Ui.marquerErreur(montantEnLigne);
            throw new IllegalArgumentException("Montant en ligne : indiquez un montant (0 si gratuit).");
        }
        if (surPlace.isSelected() && ps == null) {
            Ui.marquerErreur(montantSurPlace);
            throw new IllegalArgumentException("Montant sur place : indiquez un montant (0 si gratuit).");
        }
        b.tarif(new TarifInscription(pl == null ? 0 : pl, enLigne.isSelected(),
                ps == null ? 0 : ps, surPlace.isSelected()));

        // --- Récompenses ---
        GrilleRecompenses.Builder gb = GrilleRecompenses.builder();
        for (LigneRecompense l : recompenses) {
            l.ajouterA(gb);
        }
        GrilleRecompenses grille = gb.build();
        double part = com.pingmanager.core.reglement.LibellesTableau.partInscriptions(grille);
        if (part > 100) {
            throw new IllegalArgumentException("Récompenses : les parts des inscriptions dépassent 100 % ("
                    + Saisie.montantSaisie(part) + " %, chaque place d'un groupe étant comptée).");
        }
        b.recompenses(grille);

        ConfigurationTableau config = b.build();

        // --- Jour et horaires ---
        LocalDate j = jour.getValue();
        if (j == null) {
            Ui.marquerErreur(jour);
            throw new IllegalArgumentException("Choisissez le jour du tableau.");
        }
        LocalDate d = tournoi.getDateDebut();
        LocalDate fin = tournoi.getDateFin() != null ? tournoi.getDateFin() : d;
        if (d != null && (j.isBefore(d) || j.isAfter(fin))) {
            Ui.marquerErreur(jour);
            throw new IllegalArgumentException("Le jour du tableau doit être compris dans les dates du tournoi ("
                    + d.format(Saisie.JJ_MM_AAAA) + " au " + fin.format(Saisie.JJ_MM_AAAA) + ").");
        }
        LocalTime hDebut = lire(debutMatchs, () -> Saisie.lireHeure(debutMatchs.getText(), "Heure de début"));
        if (hDebut == null) {
            Ui.marquerErreur(debutMatchs);
            throw new IllegalArgumentException("Indiquez l'heure de début du tableau.");
        }
        LocalTime hPoint = lire(debutPointage, () -> Saisie.lireHeure(debutPointage.getText(), "Début du pointage"));
        LocalTime hFinPoint = lire(finPointage, () -> Saisie.lireHeure(finPointage.getText(), "Fin du pointage"));
        LocalTime hFin = lire(finTheorique, () -> Saisie.lireHeure(finTheorique.getText(), "Fin théorique"));
        if (hPoint != null && hFinPoint != null && !hPoint.isBefore(hFinPoint)) {
            Ui.marquerErreur(finPointage);
            throw new IllegalArgumentException("Le pointage doit se terminer après avoir commencé.");
        }
        if (hFinPoint != null && hFinPoint.isAfter(hDebut)) {
            Ui.marquerErreur(finPointage);
            throw new IllegalArgumentException("La fin du pointage doit précéder (ou égaler) l'heure de début.");
        }
        if (hPoint != null && hPoint.isAfter(hDebut)) {
            Ui.marquerErreur(debutPointage);
            throw new IllegalArgumentException("Le pointage doit commencer avant le début du tableau.");
        }
        if (hFin != null && !hFin.isAfter(hDebut)) {
            Ui.marquerErreur(finTheorique);
            throw new IllegalArgumentException("La fin théorique doit venir après l'heure de début.");
        }

        Tableau t = new Tableau(n, config);
        t.setType(EcranTableaux.type(config));
        t.setHoraires(new HorairesTableau(
                hPoint == null ? null : LocalDateTime.of(j, hPoint),
                hFinPoint == null ? null : LocalDateTime.of(j, hFinPoint),
                LocalDateTime.of(j, hDebut),
                hFin == null ? null : LocalDateTime.of(j, hFin)));
        return t;
    }

    /** Lit un champ ; en cas de saisie invalide, borde le champ en rouge et relance. */
    private static <T> T lire(Node champ, java.util.function.Supplier<T> lecture) {
        try {
            return lecture.get();
        } catch (IllegalArgumentException e) {
            Ui.marquerErreur(champ);
            throw e;
        }
    }
}
