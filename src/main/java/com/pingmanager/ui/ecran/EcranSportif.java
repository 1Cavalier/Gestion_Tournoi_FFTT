package com.pingmanager.ui.ecran;

import com.pingmanager.core.ServiceCompletudeTournoi.Niveau;
import com.pingmanager.core.ServiceVerrousHomologation;
import com.pingmanager.model.ChampHomologation;
import com.pingmanager.model.AffectationArbitre;
import com.pingmanager.model.AffectationJugeArbitre;
import com.pingmanager.model.AireDeJeu;
import com.pingmanager.model.CouleurBalles;
import com.pingmanager.model.FicheLicence;
import com.pingmanager.model.GradeJugeArbitre;
import com.pingmanager.model.InfosReglement;
import com.pingmanager.model.LotTables;
import com.pingmanager.model.NiveauTournoi;
import com.pingmanager.model.PolitiqueBalles;
import com.pingmanager.model.RoleJugeArbitre;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Section « Informations sportives et techniques » :
 * <ol>
 *   <li><b>Juge-arbitre</b> : obligations et recommandations (IX.104, IX.109),
 *       recherche dans la base des licences, refus motivé si le licencié n'a
 *       pas le grade exigé, liste des JA désignés (principal / adjoints) ;</li>
 *   <li><b>Arbitres</b> : même recherche, réservée aux licenciés ayant un grade
 *       d'arbitre ;</li>
 *   <li><b>Salle et matériel</b> : balles (provenance, couleur, marque, et liste
 *       des balles refusées quand les joueurs les apportent), tables (lots de
 *       nombre / marque / couleur) ;</li>
 *   <li><b>Rappel du règlement et aire de jeu</b> : rappels calculés selon la
 *       catégorie et les choix faits, dimensions et éclairage pré-remplis aux
 *       valeurs réglementaires, modifiables.</li>
 * </ol>
 * Les JA et arbitres sont enregistrés dès qu'on les ajoute ou les retire ; le
 * matériel et l'aire de jeu avec le bouton en bas de page.
 */
final class EcranSportif {

    private static final int MAX_RESULTATS = 8;

    private final Tournoi tournoi;
    private final BiFunction<String, Integer, List<FicheLicence>> rechercheLicences;
    private final Runnable enregistrer;
    private final Runnable retour;
    private final BorderPane racine = new BorderPane();
    private final ServiceVerrousHomologation verrous = new ServiceVerrousHomologation();

    // Matériel (lu à l'enregistrement)
    private final ToggleGroup provenance = new ToggleGroup();
    private RadioButton parClub;
    private RadioButton parJoueurs;
    private final ToggleGroup couleur = new ToggleGroup();
    private RadioButton blanche;
    private RadioButton orange;
    private final TextField marqueBalles = Ui.champ("ex. une marque agréée");
    private final List<String> refusees = new ArrayList<>();
    private final VBox listeRefusees = new VBox(6);
    private final List<LigneTables> lignesTables = new ArrayList<>();
    private final VBox boiteTables = new VBox(8);
    private final Label totalTables = Ui.note("");
    private final TextField tablesEchauffement = Ui.champ("0 = pas de salle d'échauffement");
    private final TextField longueur = Ui.champ("ex. 12");
    private final TextField largeur = Ui.champ("ex. 6");
    private final TextField eclairage = Ui.champ("ex. 500");
    private final VBox rappels = new VBox(8);

    EcranSportif(Tournoi tournoi, BiFunction<String, Integer, List<FicheLicence>> rechercheLicences,
                 Runnable enregistrer, Runnable retour) {
        this.tournoi = tournoi;
        this.rechercheLicences = rechercheLicences;
        this.enregistrer = enregistrer;
        this.retour = retour;
    }

    Node build() {
        afficher();
        return racine;
    }

    private void afficher() {
        Button btnRetour = Ui.boutonLien("← Vue d'ensemble");
        btnRetour.setOnAction(e -> retour.run());
        VBox enTete = new VBox(6, btnRetour, Ui.titre("Informations sportives et techniques"));

        Label message = Ui.message();
        Button valider = Ui.boutonPrimaire("Enregistrer le matériel et l'aire de jeu");
        valider.setOnAction(e -> enregistrerMateriel(message));

        VBox page = Ui.page(900, enTete,
                Ui.legende(EnumSet.of(Niveau.OBLIGATOIRE_HOMOLOGATION, Niveau.OBLIGATOIRE_PUBLICATION,
                        Niveau.RECOMMANDE, Niveau.OPTIONNEL)),
                carteJugesArbitres(), carteArbitres(), carteMateriel(), carteRappel(),
                message, Ui.actions(valider));
        racine.setCenter(Ui.defilant(page));
        majRappels();
    }

    // ==================================================================
    // Juges-arbitres
    // ==================================================================

    private Node carteJugesArbitres() {
        boolean jaFige = verrous.estVerrouille(tournoi, ChampHomologation.JUGES_ARBITRES);
        NiveauTournoi niveau = tournoi.getNiveau();
        Niveau H = Niveau.OBLIGATOIRE_HOMOLOGATION;
        Niveau R = Niveau.RECOMMANDE;

        String gradeExige;
        if (niveau == null) {
            gradeExige = "Choisissez d'abord la catégorie (section Général) : le grade exigé en dépend.";
        } else if (niveau.derogationJugeArbitrePossible()) {
            gradeExige = "Tournoi promotionnel : juge-arbitre principal au moins JA3 ; à défaut, l'avis "
                    + "préalable de la commission d'arbitrage est requis (IX.104).";
        } else {
            gradeExige = "Tournoi " + niveau.getLibelleMinuscule() + " : juge-arbitre principal au moins "
                    + niveau.getGradeJugeArbitreRequis().getLibelle() + " (IX.104).";
        }
        VBox regles = new VBox(6,
                regle(H, "Au moins un juge-arbitre désigné, avec son nom et son grade (IX.104)."),
                regle(H, gradeExige),
                regle(H, "Le juge-arbitre ne peut pas participer au tournoi en tant que joueur (IX.109)."),
                regle(R, "Le juge-arbitre est désigné par la commission d'arbitrage compétente : "
                        + "faites-lui valider ce choix (IX.109)."),
                regle(R, "Plusieurs tableaux en même temps : prévoyez un ou plusieurs juges-arbitres adjoints."));
        regles.getStyleClass().add("encart-regles");

        ComboBox<RoleJugeArbitre> role = new ComboBox<>();
        role.getItems().addAll(RoleJugeArbitre.values());
        role.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(RoleJugeArbitre r) {
                return r == null ? "" : r.getLibelle();
            }

            @Override
            public RoleJugeArbitre fromString(String s) {
                return null;
            }
        });
        role.setValue(tournoi.getJugeArbitrePrincipal().isPresent() ? RoleJugeArbitre.ADJOINT : RoleJugeArbitre.PRINCIPAL);

        Label message = Ui.message();
        Node recherche = recherche("Rechercher un juge-arbitre : nom, prénom ou n° de licence",
                f -> f.getJugeArbitre() == null ? Ui.pastille("Pas juge-arbitre", Ui.CADRE_FACULTATIF)
                        : Ui.badge(GradeJugeArbitre.depuisCode(f.getJugeArbitre())
                        .map(GradeJugeArbitre::getLibelle).orElse(f.getJugeArbitre())),
                f -> designerJugeArbitre(f, role.getValue(), message));

        HBox ligneRecherche = Ui.ligne(10, Ui.champEtiquete("Rôle", role), recherche);
        HBox.setHgrow(recherche, Priority.ALWAYS);
        ligneRecherche.setAlignment(javafx.geometry.Pos.TOP_LEFT);

        Node liste = tournoi.getJugesArbitres().isEmpty()
                ? Ui.discret("Aucun juge-arbitre désigné pour l'instant.")
                : tableauPersonnes(new String[] {"Nom", "Prénom", "Rôle", "Grade", ""},
                tournoi.getJugesArbitres(), a -> new String[] {a.getNom(), a.getPrenom(),
                        a.getRole().getLibelle(), a.getGrade().getLibelle()},
                jaFige ? null : a -> {
                    // B035 : par la méthode gardée (dossier d'homologation verrouillé dès la demande).
                    String refus = PresentationPreparationTournoi.messageModification(
                            verrous.retirerJugeArbitre(tournoi, a.getNumeroLicence()));
                    if (refus != null) {
                        Ui.erreur(message, refus);
                        return;
                    }
                    enregistrer.run();
                    afficher();
                });

        Node entete = Ui.enteteAvecActions(Ui.ligne(8, Ui.puceDependance(H), Ui.titreCarte("Juge-arbitre")),
                Ui.badgeEtat(H, !tournoi.getJugesArbitres().isEmpty()));
        if (jaFige) {
            // Dossier d'homologation verrouillé : ni ajout ni retrait, le motif est affiché.
            return Ui.carte(entete, Ui.bandeauVerrou(new PresentationPreparationTournoi(tournoi)
                    .motifVerrouSection(PresentationPreparationTournoi.SECTION_SPORTIF)), regles, message, liste);
        }
        return Ui.carte(entete, regles, ligneRecherche, message, liste);
    }

    private void designerJugeArbitre(FicheLicence f, RoleJugeArbitre role, Label message) {
        String qui = f.getPrenom() + " " + f.getNom();
        try {
            if (f.getJugeArbitre() == null) {
                throw new IllegalArgumentException(qui + " n'a pas de grade de juge-arbitre sur sa licence : "
                        + "impossible de le désigner.");
            }
            GradeJugeArbitre grade = GradeJugeArbitre.depuisCode(f.getJugeArbitre())
                    .orElseThrow(() -> new IllegalArgumentException("Grade « " + f.getJugeArbitre()
                            + " » non reconnu pour " + qui + "."));
            boolean deja = tournoi.getJugesArbitres().stream()
                    .anyMatch(a -> a.getNumeroLicence().equals(f.getNumeroLicence()));
            if (deja) {
                throw new IllegalArgumentException(qui + " est déjà juge-arbitre de ce tournoi.");
            }
            if (estInscritCommeJoueur(f.getNumeroLicence())) {
                throw new IllegalArgumentException(qui + " est inscrit comme joueur : un juge-arbitre ne peut "
                        + "pas participer au tournoi (IX.109).");
            }
            String avertissement = null;
            if (role == RoleJugeArbitre.PRINCIPAL) {
                Optional<AffectationJugeArbitre> principal = tournoi.getJugeArbitrePrincipal();
                if (principal.isPresent()) {
                    throw new IllegalArgumentException("Il y a déjà un juge-arbitre principal ("
                            + principal.get().getPrenom() + " " + principal.get().getNom()
                            + "). Désignez " + qui + " comme adjoint, ou retirez d'abord le principal.");
                }
                NiveauTournoi niveau = tournoi.getNiveau();
                if (niveau == null) {
                    avertissement = "Catégorie du tournoi non choisie : le grade n'a pas pu être contrôlé.";
                } else if (!grade.couvre(niveau.getGradeJugeArbitreRequis())) {
                    if (!niveau.derogationJugeArbitrePossible()) {
                        throw new IllegalArgumentException(qui + " est " + grade.getLibelle() + " : un tournoi "
                                + niveau.getLibelleMinuscule() + " exige un juge-arbitre principal au moins "
                                + niveau.getGradeJugeArbitreRequis().getLibelle() + " (IX.104).");
                    }
                    avertissement = qui + " est " + grade.getLibelle() + " : en promotionnel c'est possible, "
                            + "avec l'avis préalable de la commission d'arbitrage (IX.104).";
                }
            }
            String refus = PresentationPreparationTournoi.messageModification(verrous.ajouterJugeArbitre(tournoi,
                    new AffectationJugeArbitre(f.getNom(), f.getPrenom(), null, f.getNumeroLicence(), grade,
                            null, role)));
            if (refus != null) {
                throw new IllegalArgumentException(refus);
            }
            enregistrer.run();
            afficher();
            if (avertissement != null) {
                // Le message de la carte reconstruite n'existe plus : on le signale en haut de page.
                signaler(avertissement);
            }
        } catch (IllegalArgumentException ex) {
            Ui.erreur(message, ex.getMessage());
        }
    }

    private boolean estInscritCommeJoueur(String licence) {
        for (Tableau t : tournoi.getTableaux()) {
            boolean inscrit = t.getInscrits().stream().anyMatch(j -> licence.equals(j.getNumeroLicence()))
                    || t.getFileAttente().stream().anyMatch(j -> licence.equals(j.getNumeroLicence()));
            if (inscrit) {
                return true;
            }
        }
        return false;
    }

    // ==================================================================
    // Arbitres
    // ==================================================================

    private Node carteArbitres() {
        Niveau O = Niveau.OPTIONNEL;
        Label message = Ui.message();
        Node recherche = recherche("Rechercher un arbitre : nom, prénom ou n° de licence",
                f -> f.getArbitre() == null ? Ui.pastille("Pas arbitre", Ui.CADRE_FACULTATIF)
                        : Ui.badge(f.getArbitre()),
                f -> designerArbitre(f, message));

        Node liste = tournoi.getArbitres().isEmpty()
                ? Ui.discret("Aucun arbitre pour l'instant.")
                : tableauPersonnes(new String[] {"Nom", "Prénom", "Grade", ""}, tournoi.getArbitres(),
                a -> new String[] {a.getNom(), a.getPrenom(), AffectationArbitre.libelleGrade(a.getGrade())},
                a -> {
                    tournoi.retirerArbitre(a.getNumeroLicence());
                    enregistrer.run();
                    afficher();
                });

        Node entete = Ui.ligne(8, Ui.puceDependance(O), Ui.titreCarte("Arbitres"));
        return Ui.carte(entete,
                Ui.note("Facultatif. Seuls les licenciés qui ont un grade d'arbitre sur leur licence peuvent "
                        + "être désignés. Sans arbitre officiel, prévoyez le mode d'arbitrage des parties."),
                recherche, message, liste);
    }

    private void designerArbitre(FicheLicence f, Label message) {
        String qui = f.getPrenom() + " " + f.getNom();
        try {
            if (f.getArbitre() == null) {
                throw new IllegalArgumentException(qui + " n'a pas de grade d'arbitre sur sa licence : "
                        + "impossible de le désigner.");
            }
            tournoi.ajouterArbitre(new AffectationArbitre(f.getNumeroLicence(), f.getNom(), f.getPrenom(),
                    f.getArbitre()));
            enregistrer.run();
            afficher();
        } catch (IllegalArgumentException ex) {
            Ui.erreur(message, ex.getMessage());
        }
    }

    // ==================================================================
    // Recherche dans la base des licences (partagée JA / arbitres)
    // ==================================================================

    /**
     * Champ de recherche + résultats au fil de la frappe. Chaque résultat montre
     * le grade utile (ou son absence) et un bouton « Désigner » ; c'est au clic
     * que l'on contrôle et que l'on refuse, avec un message explicite.
     */
    private Node recherche(String libelle, Function<FicheLicence, Label> badge, Consumer<FicheLicence> choisir) {
        TextField saisie = Ui.champ("ex. Lemoine, Hélène ou 9411021");
        VBox resultats = new VBox(0);
        resultats.getStyleClass().add("resultats-recherche");
        Ui.cacher(resultats);
        saisie.textProperty().addListener((o, a, texte) -> {
            resultats.getChildren().clear();
            List<FicheLicence> trouves = rechercheLicences.apply(texte, MAX_RESULTATS);
            if (texte == null || texte.trim().length() < 2) {
                Ui.cacher(resultats);
                return;
            }
            if (trouves.isEmpty()) {
                resultats.getChildren().add(Ui.discret("Aucun licencié ne correspond à « " + texte.trim() + " »."));
            }
            for (FicheLicence f : trouves) {
                Button designer = Ui.boutonSecondaire("Désigner");
                designer.setOnAction(e -> choisir.accept(f));
                VBox identite = new VBox(1, Ui.gras(f.getNom() + " " + f.getPrenom()),
                        Ui.discret("Licence " + f.getNumeroLicence()
                                + (f.getNomClub() != null ? ", " + f.getNomClub() : "")));
                HBox ligne = Ui.ligne(10, identite, Ui.espace(), badge.apply(f), designer);
                ligne.getStyleClass().add("resultat");
                resultats.getChildren().add(ligne);
            }
            Ui.montrer(resultats);
        });
        return new VBox(6, Ui.champEtiquete(libelle, saisie), resultats);
    }

    /** Petit tableau Nom / Prénom / … avec un lien « Retirer » par ligne. */
    private <T> Node tableauPersonnes(String[] colonnes, List<T> lignes, Function<T, String[]> valeurs,
                                      Consumer<T> retirer) {
        GridPane g = new GridPane();
        g.getStyleClass().add("grille-tableaux");
        for (int c = 0; c < colonnes.length; c++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setHgrow(c == colonnes.length - 1 ? Priority.NEVER : Priority.SOMETIMES);
            g.getColumnConstraints().add(cc);
            Label l = Ui.enteteColonne(colonnes[c]);
            l.getStyleClass().add("cellule-entete");
            l.setMaxWidth(Double.MAX_VALUE);   // bande d'en-tête continue
            g.add(l, c, 0);
        }
        int r = 1;
        g.add(filet(), 0, r++, colonnes.length, 1);
        for (T t : new ArrayList<>(lignes)) {
            String[] v = valeurs.apply(t);
            for (int c = 0; c < v.length; c++) {
                Label l = c == 0 ? Ui.gras(v[c]) : new Label(v[c]);
                l.getStyleClass().add("cellule");
                l.setMinWidth(Region.USE_PREF_SIZE);
                GridPane.setValignment(l, VPos.CENTER);
                g.add(l, c, r);
            }
            if (retirer != null) {
                Button retirerBtn = Ui.boutonLien("Retirer");
                retirerBtn.getStyleClass().addAll("lien-danger", "cellule");
                retirerBtn.setOnAction(e -> retirer.accept(t));
                g.add(retirerBtn, colonnes.length - 1, r);
            }
            r++;
            g.add(filet(), 0, r++, colonnes.length, 1);
        }
        return g;
    }

    private static Region filet() {
        Region f = new Region();
        f.getStyleClass().add("filet-ligne");
        f.setMinHeight(1);
        f.setPrefHeight(1);
        f.setMaxHeight(1);
        return f;
    }

    // ==================================================================
    // Salle et matériel
    // ==================================================================

    private Node carteMateriel() {
        InfosReglement ir = tournoi.getInfosReglement();
        Niveau P = Niveau.OBLIGATOIRE_PUBLICATION;
        Niveau R = Niveau.RECOMMANDE;

        // --- Balles ---
        parClub = radio("Fournies par le club", provenance);
        parJoueurs = radio("Apportées par les joueurs", provenance);
        blanche = radio("Blanche", couleur);
        orange = radio("Orange", couleur);
        PolitiqueBalles pb = ir == null ? null : ir.getPolitiqueBalles();
        if (pb == PolitiqueBalles.FOURNIES_PAR_ORGANISATEUR) {
            parClub.setSelected(true);
        } else if (pb == PolitiqueBalles.APPORTEES_PAR_JOUEURS) {
            parJoueurs.setSelected(true);
        }
        CouleurBalles cb = ir == null ? null : ir.getCouleurBalles();
        if (cb == CouleurBalles.BLANCHE) {
            blanche.setSelected(true);
        } else if (cb == CouleurBalles.ORANGE) {
            orange.setSelected(true);
        }
        marqueBalles.setText(ir == null || ir.getMarqueBalles() == null ? "" : ir.getMarqueBalles());
        refusees.clear();
        if (ir != null) {
            refusees.addAll(ir.getBallesRefusees());
        }

        VBox blocRefusees = blocBallesRefusees();
        Runnable majProvenance = () -> {
            Ui.cacher(blocRefusees);
            if (parJoueurs.isSelected()) {
                Ui.montrer(blocRefusees);
            }
            majRappels();
        };
        provenance.selectedToggleProperty().addListener((o, a, n) -> majProvenance.run());
        couleur.selectedToggleProperty().addListener((o, a, n) -> majRappels());

        VBox balles = new VBox(12,
                titreGroupe("Balles", true),
                choix("Provenance", P, provenance, parClub, parJoueurs),
                choix("Couleur", R, couleur, blanche, orange),
                Ui.champEtiquete("Marque", marqueBalles, R),
                blocRefusees);
        majProvenance.run();

        // --- Tables ---
        lignesTables.clear();
        boiteTables.getChildren().clear();
        if (tournoi.getLotsTables().isEmpty()) {
            ajouterLigneTables(tournoi.getNombreDeTables() > 0
                    ? new LotTables(tournoi.getNombreDeTables(), null, null) : null);
        } else {
            tournoi.getLotsTables().forEach(this::ajouterLigneTables);
        }
        Button ajouterLot = Ui.boutonLien("+ Ajouter des tables d'une autre marque ou couleur");
        ajouterLot.setOnAction(e -> ajouterLigneTables(null));
        tablesEchauffement.setText(tournoi.getTablesEchauffement() > 0
                ? String.valueOf(tournoi.getTablesEchauffement()) : "");
        tablesEchauffement.setMaxWidth(260);
        tablesEchauffement.textProperty().addListener((o, a, n) -> majTotal());
        majTotal();
        VBox tables = new VBox(10,
                titreGroupe("Tables", false),
                Ui.ligne(6, Ui.puceDependance(P), labelChamp("Tables de compétition : nombre, marque et couleur")),
                boiteTables, ajouterLot,
                Ui.champEtiquete("Salle d'échauffement : nombre de tables", tablesEchauffement, Niveau.OPTIONNEL),
                totalTables);

        return Ui.carte("Salle et matériel", balles, tables);
    }

    private VBox blocBallesRefusees() {
        TextField nouvelle = Ui.champ("ex. une marque, une matière…");
        Button ajouter = Ui.boutonSecondaire("Ajouter");
        Runnable ajout = () -> {
            String v = nouvelle.getText() == null ? "" : nouvelle.getText().trim();
            if (!v.isEmpty() && refusees.stream().noneMatch(x -> x.equalsIgnoreCase(v))) {
                refusees.add(v);
                majRefusees();
            }
            nouvelle.clear();
        };
        ajouter.setOnAction(e -> ajout.run());
        nouvelle.setOnAction(e -> ajout.run());
        HBox.setHgrow(nouvelle, Priority.ALWAYS);

        FlowPane raccourcis = new FlowPane(14, 6);
        for (String p : List.of("Balles en celluloïd", "Balles non agréées ITTF/FFTT", "Balles d'entraînement")) {
            Button b = Ui.boutonLien("+ " + p);
            b.setOnAction(e -> {
                if (!refusees.contains(p)) {
                    refusees.add(p);
                    majRefusees();
                }
            });
            raccourcis.getChildren().add(b);
        }
        majRefusees();
        VBox v = new VBox(8,
                labelChamp("Balles refusées (liste noire)"),
                Ui.note("Les joueurs apportent leurs balles : indiquez les marques, matières ou modèles que "
                        + "vous n'acceptez pas. Ce bloc disparaît si le club fournit les balles."),
                listeRefusees,
                Ui.ligne(8, nouvelle, ajouter),
                raccourcis);
        v.getStyleClass().add("liste-depliee");
        return v;
    }

    private void majRefusees() {
        listeRefusees.getChildren().clear();
        if (refusees.isEmpty()) {
            listeRefusees.getChildren().add(Ui.discret("Aucune restriction pour l'instant."));
        }
        for (String r : new ArrayList<>(refusees)) {
            Button retirer = Ui.boutonLien("Retirer");
            retirer.getStyleClass().add("lien-danger");
            retirer.setOnAction(e -> {
                refusees.remove(r);
                majRefusees();
            });
            listeRefusees.getChildren().add(Ui.ligne(10, new Label("• " + r), retirer));
        }
    }

    /** Une ligne « nombre / marque / couleur » de tables. */
    private final class LigneTables {
        final TextField nombre = Ui.champ("nombre");
        final TextField marque = Ui.champ("marque");
        final ComboBox<String> teinte = new ComboBox<>();
        final HBox noeud;

        LigneTables(LotTables lot) {
            teinte.setEditable(true);
            teinte.getItems().addAll("Bleue", "Verte", "Noire", "Grise");
            teinte.setPromptText("couleur");
            if (lot != null) {
                nombre.setText(String.valueOf(lot.getNombre()));
                marque.setText(lot.getMarque() == null ? "" : lot.getMarque());
                teinte.setValue(lot.getCouleur());
            }
            nombre.setPrefColumnCount(5);
            nombre.textProperty().addListener((o, a, n) -> majTotal());
            marque.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(marque, Priority.ALWAYS);
            teinte.setPrefWidth(150);
            Button retirer = Ui.boutonLien("Retirer");
            retirer.getStyleClass().add("lien-danger");
            retirer.setOnAction(e -> retirer());
            noeud = Ui.ligne(8, nombre, new Label("tables"), marque, teinte, retirer);
        }

        private void retirer() {
            lignesTables.remove(this);
            boiteTables.getChildren().remove(noeud);
            majTotal();
        }

        boolean estVide() {
            return vide(nombre.getText()) && vide(marque.getText()) && vide(teinte.getEditor().getText());
        }

        LotTables lire() {
            Integer n;
            try {
                n = Saisie.lireEntier(nombre.getText(), "Tables : nombre");
            } catch (IllegalArgumentException e) {
                Ui.marquerErreur(nombre);
                throw e;
            }
            if (n == null || n == 0) {
                Ui.marquerErreur(nombre);
                throw new IllegalArgumentException("Tables : indiquez le nombre de tables de chaque ligne.");
            }
            return new LotTables(n, marque.getText(), teinte.getEditor().getText());
        }
    }

    private void ajouterLigneTables(LotTables lot) {
        LigneTables l = new LigneTables(lot);
        lignesTables.add(l);
        boiteTables.getChildren().add(l.noeud);
    }

    private void majTotal() {
        int total = 0;
        for (LigneTables l : lignesTables) {
            try {
                Integer n = Saisie.lireEntier(l.nombre.getText(), "");
                total += n == null ? 0 : n;
            } catch (IllegalArgumentException ignore) {
                // saisie en cours
            }
        }
        int echauffement = 0;
        try {
            Integer e = Saisie.lireEntier(tablesEchauffement.getText(), "");
            echauffement = e == null ? 0 : e;
        } catch (IllegalArgumentException ignore) {
            // saisie en cours
        }
        if (total == 0) {
            totalTables.setText("Nombre de tables non renseigné.");
        } else if (echauffement > 0) {
            totalTables.setText("Total : " + total + " tables de compétition + " + echauffement
                    + " en salle d'échauffement, soit " + (total + echauffement) + " tables.");
        } else {
            totalTables.setText("Total : " + total + " tables.");
        }
    }

    // ==================================================================
    // Rappel du règlement et aire de jeu
    // ==================================================================

    private Node carteRappel() {
        InfosReglement ir = tournoi.getInfosReglement();
        AireDeJeu aire = ir == null ? null : ir.getAireDeJeu();
        if (aire != null) {
            longueur.setText(aire.getLongueurCourtM() == null ? "" : nombre(aire.getLongueurCourtM()));
            largeur.setText(aire.getLargeurCourtM() == null ? "" : nombre(aire.getLargeurCourtM()));
            eclairage.setText(aire.getEclairageLux() == null ? "" : String.valueOf(aire.getEclairageLux()));
        } else {
            valeursReglementaires();
        }
        Button reglementaires = Ui.boutonLien("Remettre les valeurs réglementaires");
        reglementaires.setOnAction(e -> valeursReglementaires());

        GridPane g = new GridPane();
        g.setHgap(16);
        g.setVgap(12);
        for (int i = 0; i < 3; i++) {
            ColumnConstraints c = new ColumnConstraints();
            c.setPercentWidth(100.0 / 3);
            g.getColumnConstraints().add(c);
        }
        g.add(Ui.champEtiquete("Longueur de l'aire (m)", longueur, Niveau.OPTIONNEL), 0, 0);
        g.add(Ui.champEtiquete("Largeur de l'aire (m)", largeur, Niveau.OPTIONNEL), 1, 0);
        g.add(Ui.champEtiquete("Éclairage (lux)", eclairage, Niveau.OPTIONNEL), 2, 0);

        rappels.getStyleClass().add("encart-regles");
        return Ui.carte("Rappel du règlement",
                Ui.note("Ce qu'il faut prévoir, selon la catégorie et vos choix ci-dessus."),
                rappels,
                titreGroupe("Aire de jeu", false),
                Ui.note(estNationale()
                        ? "Pré-rempli aux minimums réglementaires d'une compétition nationale ; modifiable."
                        : "Pré-rempli aux valeurs de référence des compétitions nationales ; modifiable."),
                g, reglementaires);
    }

    private boolean estNationale() {
        NiveauTournoi n = tournoi.getNiveau();
        return n != null && n.estNationalOuInternational();
    }

    private void valeursReglementaires() {
        longueur.setText("12");
        largeur.setText("6");
        eclairage.setText("500");
    }

    /** Rappels recalculés à chaque changement de catégorie, de balles ou de provenance. */
    private void majRappels() {
        rappels.getChildren().clear();
        NiveauTournoi niveau = tournoi.getNiveau();
        Niveau H = Niveau.OBLIGATOIRE_HOMOLOGATION;
        Niveau R = Niveau.RECOMMANDE;

        if (niveau != null) {
            rappels.getChildren().add(regle(H, niveau.derogationJugeArbitrePossible()
                    ? "Juge-arbitre au moins JA3, ou avis préalable de la commission d'arbitrage (IX.104)."
                    : "Juge-arbitre principal au moins " + niveau.getGradeJugeArbitreRequis().getLibelle()
                    + " (IX.104)."));
        }
        rappels.getChildren().add(regle(H, "Tables conformes à la norme NF EN 14468-1, filets NF EN 14468-2, "
                + "de marques agréées ITTF ou FFTT (3.2.1.1)."));
        rappels.getChildren().add(regle(H, "Surface de jeu de couleur uniformément foncée et mate, "
                + "bordée de blanc (2.1.4)."));
        String balle = "Balles en plastique, 40 mm, 2,7 g, mates, d'une marque agréée (2.3, 3.2.1.1)";
        if (blanche != null && blanche.isSelected()) {
            balle += " ; vous avez choisi des balles blanches.";
        } else if (orange != null && orange.isSelected()) {
            balle += " ; vous avez choisi des balles orange.";
        } else {
            balle += ", blanches ou orange.";
        }
        rappels.getChildren().add(regle(H, balle));
        if (parJoueurs != null && parJoueurs.isSelected()) {
            rappels.getChildren().add(regle(R, "Balles apportées par les joueurs : annoncez au règlement du "
                    + "tournoi la couleur imposée et les balles refusées."));
        }
        if (estNationale()) {
            rappels.getChildren().add(regle(H, "Espace de jeu d'au moins 12 m × 6 m et 4,5 m de haut "
                    + "(3.2.3.1, IX.110). Avec plusieurs tables côte à côte sans entourages latéraux, la "
                    + "largeur peut être réduite de 50 cm."));
            rappels.getChildren().add(regle(H, "Éclairage moyen d'au moins 500 ou 750 lux au niveau de la "
                    + "surface de jeu, uniformité 0,7 (3.2.3.3)."));
        } else {
            rappels.getChildren().add(regle(R, "Hors compétition nationale, pas de dimension imposée : "
                    + "12 m × 6 m reste la référence (3.2.3.1)."));
        }
        rappels.getChildren().add(regle(R, "Aire de jeu délimitée par des séparations de couleur foncée, "
                + "de 50 à 100 cm de haut (3.2.3.2)."));
    }

    // ==================================================================
    // Enregistrement du matériel
    // ==================================================================

    private void enregistrerMateriel(Label message) {
        Ui.cacher(message);
        try {
            List<LotTables> lots = new ArrayList<>();
            for (LigneTables l : lignesTables) {
                if (!l.estVide()) {
                    lots.add(l.lire());
                }
            }
            Double lg = lireDecimal(longueur, "Longueur de l'aire");
            Double la = lireDecimal(largeur, "Largeur de l'aire");
            Integer lux = lireEntierChamp(eclairage, "Éclairage");
            AireDeJeu aire = lg == null && la == null && lux == null ? null : new AireDeJeu(lg, la, lux);

            InfosReglement ir = tournoi.getInfosReglement();
            InfosReglement.Builder b = ir != null ? ir.toBuilder() : InfosReglement.builder();
            b.politiqueBalles(parClub.isSelected() ? PolitiqueBalles.FOURNIES_PAR_ORGANISATEUR
                            : parJoueurs.isSelected() ? PolitiqueBalles.APPORTEES_PAR_JOUEURS : null)
                    .couleurBalles(blanche.isSelected() ? CouleurBalles.BLANCHE
                            : orange.isSelected() ? CouleurBalles.ORANGE : null)
                    .marqueBalles(vide(marqueBalles.getText()) ? null : marqueBalles.getText().trim())
                    .ballesRefusees(parJoueurs.isSelected() ? new ArrayList<>(refusees) : List.of())
                    .aireDeJeu(aire);
            tournoi.setInfosReglement(b.build());
            Integer echauffement = lireEntierChamp(tablesEchauffement, "Tables de la salle d'échauffement");
            tournoi.setTablesEchauffement(echauffement == null ? 0 : echauffement);
            tournoi.setLotsTables(lots);
            if (lots.isEmpty()) {
                tournoi.setNombreDeTables(0);
            }
            enregistrer.run();
            Ui.succes(message, "Matériel et aire de jeu enregistrés.");
        } catch (IllegalArgumentException ex) {
            Ui.erreur(message, ex.getMessage());
        }
    }

    private static Double lireDecimal(TextField champ, String nom) {
        try {
            return Saisie.lireMontant(champ.getText(), nom);
        } catch (IllegalArgumentException e) {
            Ui.marquerErreur(champ);
            throw new IllegalArgumentException(nom + " : nombre attendu, par exemple 12 ou 11,5.");
        }
    }

    private static Integer lireEntierChamp(TextField champ, String nom) {
        try {
            return Saisie.lireEntier(champ.getText(), nom);
        } catch (IllegalArgumentException e) {
            Ui.marquerErreur(champ);
            throw e;
        }
    }

    // ==================================================================
    // Petits éléments
    // ==================================================================

    /** Avertissement non bloquant affiché en haut de la page reconstruite. */
    private void signaler(String texte) {
        Label l = new Label(texte);
        l.setWrapText(true);
        l.getStyleClass().addAll("rappel-attention", "rappel-doux");
        if (racine.getCenter() instanceof javafx.scene.control.ScrollPane sp
                && sp.getContent() instanceof VBox page) {
            page.getChildren().add(1, l);
        }
    }

    private static HBox regle(Niveau n, String texte) {
        Label l = new Label(texte);
        l.setWrapText(true);
        // Le rond est calé sur la première ligne du texte (qui peut passer à la ligne).
        javafx.scene.layout.StackPane rond = new javafx.scene.layout.StackPane(Ui.puceDependance(n));
        rond.setPadding(new javafx.geometry.Insets(4, 0, 0, 0));
        HBox h = new HBox(8, rond, l);
        h.setAlignment(javafx.geometry.Pos.TOP_LEFT);
        HBox.setHgrow(l, Priority.ALWAYS);
        return h;
    }

    private static RadioButton radio(String texte, ToggleGroup g) {
        RadioButton r = new RadioButton(texte);
        r.setToggleGroup(g);
        return r;
    }

    private static VBox choix(String libelle, Niveau n, ToggleGroup g, RadioButton... options) {
        HBox entete = Ui.ligne(6, Ui.puceDependance(n), labelChamp(libelle));
        HBox boutons = Ui.ligne(18, options);
        Label etat = Ui.badgeEtat(n, g.getSelectedToggle() != null);
        g.selectedToggleProperty().addListener((o, a, x) -> {
            etat.getStyleClass().removeAll(Ui.CADRE_OK, Ui.CADRE_MANQUANT, Ui.CADRE_FACULTATIF);
            Label neuf = Ui.badgeEtat(n, x != null);
            etat.setText(neuf.getText());
            etat.getStyleClass().add(neuf.getStyleClass().get(neuf.getStyleClass().size() - 1));
        });
        return new VBox(8, Ui.ligne(10, entete, etat), boutons);
    }

    private static Label titreGroupe(String texte, boolean premier) {
        Label l = new Label(texte);
        l.getStyleClass().add("groupe-titre");
        if (!premier) {
            l.getStyleClass().add("groupe-titre-separe");
        }
        return l;
    }

    private static Label labelChamp(String texte) {
        Label l = new Label(texte);
        l.getStyleClass().add("libelle-champ");
        return l;
    }

    private static String nombre(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.valueOf(v).replace('.', ',');
    }

    private static boolean vide(String s) {
        return s == null || s.isBlank();
    }
}
