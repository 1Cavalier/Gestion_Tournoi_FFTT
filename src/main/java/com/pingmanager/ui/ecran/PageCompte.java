package com.pingmanager.ui.ecran;

import com.pingmanager.core.DemandeVerification;
import com.pingmanager.core.ResultatLiaisonClub;
import com.pingmanager.model.AccesClub;
import com.pingmanager.model.Club;
import com.pingmanager.model.CompteUtilisateur;
import com.pingmanager.model.Tournoi;
import com.pingmanager.ui.ContexteApplication;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Page « Mon profil » (cahier des charges v0, section 2). Vue mince : le
 * contenu (libellés, ordre, droits, contrôle de la confirmation) vient de
 * {@link PresentationProfil}.
 *
 * <p>En haut, pour tous : identité et licence FFTT côte à côte, puis les
 * sections « Tournoi » et « Appel micro ». En bas, bien séparé : le bloc
 * « Club » (avertissement et confirmation avant liaison, tableau des droits
 * de chaque club relié, équipe de gestion).</p>
 */
public final class PageCompte {

    private final ContexteApplication contexte;
    private final Runnable onHabilitationsChangees;
    private final Consumer<String> naviguer;

    private DemandeVerification demandeEnCours;
    private String numeroClubEnCours;

    private final VBox boiteLicence = new VBox(10);
    private final VBox boiteAcces = new VBox(16);
    private final TextField champNumeroClub = champ("numéro de club", 200);
    private final CheckBox caseConfirmation = new CheckBox(PresentationProfil.CONFIRMATION);
    private final Button boutonEnvoyer = Ui.boutonPrimaire("Envoyer le code");
    private final Label messageClub = message();
    private final VBox zoneCode = new VBox(8);
    private final TextField champCode = champ("code reçu", 140);
    private final TextArea panneauDemo = new TextArea();
    private final VBox boiteDemo = new VBox(6);
    private final Hyperlink toggleDemo = new Hyperlink();
    private boolean demoOuvert = false;

    public PageCompte(ContexteApplication contexte, Runnable onHabilitationsChangees) {
        this(contexte, onHabilitationsChangees, null);
    }

    /** @param naviguer ouvre une rubrique de la coquille par sa clé (peut être {@code null}) */
    public PageCompte(ContexteApplication contexte, Runnable onHabilitationsChangees,
                      Consumer<String> naviguer) {
        this.contexte = contexte;
        this.onHabilitationsChangees = onHabilitationsChangees;
        this.naviguer = naviguer;
    }

    public Parent build() {
        CompteUtilisateur compte = contexte.compteConnecte();

        // Bloc du haut : à gauche identité, Tournoi et Appel micro ; à droite,
        // à côté de l'identité, la licence (longue) : pas de carte étirée à vide.
        Node enteteIdentite = Ui.ligne(14,
                Ui.avatarGrand(compte.getPrenom(), compte.getNom()),
                Ui.titreCarte(PresentationProfil.TITRE_IDENTITE));
        VBox carteIdentite = Ui.carte(enteteIdentite,
                grilleChamps(PresentationProfil.identite(
                        compte.getNom(), compte.getPrenom(), compte.getEmail(), null)));
        remplirLicence();
        VBox carteLicence = Ui.carte(PresentationProfil.TITRE_LICENCE, boiteLicence);

        VBox blocPersonnel = new VBox(deuxColonnes(
                new VBox(20, carteIdentite, construireCarteTournoi(), construireCarteAppel()),
                new VBox(20, carteLicence)));
        blocPersonnel.getStyleClass().add("profil-bloc-personnel");

        VBox contenu = Ui.page(1080, titre(PresentationProfil.TITRE), blocPersonnel, construireBlocClub());
        return Ui.defilant(contenu);
    }

    /** Deux colonnes de cartes de même largeur, alignées en haut. */
    private static GridPane deuxColonnes(VBox gauche, VBox droite) {
        GridPane g = new GridPane();
        g.setHgap(20);
        for (int i = 0; i < 2; i++) {
            ColumnConstraints c = new ColumnConstraints();
            c.setPercentWidth(50);
            g.getColumnConstraints().add(c);
        }
        GridPane.setValignment(gauche, VPos.TOP);
        GridPane.setValignment(droite, VPos.TOP);
        g.add(gauche, 0, 0);
        g.add(droite, 1, 0);
        return g;
    }

    private static GridPane grilleChamps(List<PresentationProfil.Champ> champs) {
        GridPane g = new GridPane();
        g.setHgap(24);
        g.setVgap(8);
        for (int i = 0; i < champs.size(); i++) {
            g.add(discret(champs.get(i).libelle()), 0, i);
            Label v = new Label(champs.get(i).valeur());
            v.setWrapText(true);
            g.add(v, 1, i);
        }
        return g;
    }

    // Licence ---------------------------------------------------------------

    private void remplirLicence() {
        boiteLicence.getChildren().clear();
        CompteUtilisateur compte = contexte.compteConnecte();

        if (compte.aLicenceLiee()) {
            boiteLicence.getChildren().add(grilleChamps(
                    PresentationProfil.licence(compte.getLicenceLiee(), contexte.ficheLicenceLiee())));
            return;
        }

        TextField champLicence = champ("numéro de licence", 200);
        Label msg = message();
        Button lier = Ui.boutonPrimaire("Vérifier et lier");
        lier.setOnAction(e -> {
            String numero = valeur(champLicence);
            if (numero.isBlank()) {
                erreur(msg, "Saisissez un numéro de licence.");
                return;
            }
            if (contexte.lierLicence(numero).isPresent()) {
                remplirLicence();
            } else {
                erreur(msg, "Aucune licence ne correspond à ce numéro.");
            }
        });

        boiteLicence.getChildren().addAll(
                aide("Saisissez votre numéro de licence pour le rattacher à votre compte."),
                ligne(champLicence, lier),
                msg);
    }

    // Tournoi et appel micro -----------------------------------------------

    private VBox construireCarteTournoi() {
        List<ContexteApplication.TournoiFichier> miens =
                contexte.mesTournoisJoueur(contexte.tousLesTournois());
        VBox corps = new VBox(8, Ui.texte(PresentationProfil.resumeTournois(
                contexte.compteConnecte().aLicenceLiee(), miens.size())));
        for (ContexteApplication.TournoiFichier tf : miens) {
            Tournoi t = tf.tournoi();
            corps.getChildren().add(gras(t.getNom()));
        }
        if (naviguer != null) {
            Button voir = Ui.boutonSecondaire("Voir les tournois");
            voir.setOnAction(e -> naviguer.accept(PresentationAccueil.TOURNOIS));
            corps.getChildren().add(Ui.actions(voir));
        }
        return Ui.carte(PresentationProfil.TITRE_TOURNOI, corps);
    }

    /** Appel micro : pas encore de saisie (B018), d'où la mention « à venir ». */
    private static VBox construireCarteAppel() {
        Node entete = Ui.ligne(12, Ui.titreCarte(PresentationProfil.TITRE_APPEL), Ui.espace(),
                Ui.pastille("Bientôt", StatutPastille.NEUTRE));
        return Ui.carte(entete,
                grilleChamps(List.of(new PresentationProfil.Champ("Prononciation",
                        PresentationProfil.prononciation(null)))),
                aide(PresentationProfil.APPEL_A_VENIR));
    }

    // Clubs -----------------------------------------------------------------

    private VBox construireBlocClub() {
        remplirAcces();

        boutonEnvoyer.setOnAction(e -> envoyerCode());
        caseConfirmation.getStyleClass().add("profil-confirmation-liaison");
        caseConfirmation.setWrapText(true);
        caseConfirmation.setMaxWidth(Double.MAX_VALUE);
        caseConfirmation.selectedProperty().addListener((o, a, n) -> majEnvoiPossible());
        majEnvoiPossible();

        Button valider = Ui.boutonPrimaire("Valider le code");
        valider.setOnAction(e -> validerCode());
        zoneCode.getChildren().setAll(
                aide("Saisissez le code envoyé à l'adresse officielle du club :"),
                ligne(champCode, valider));
        cacher(zoneCode);

        panneauDemo.getStyleClass().add("zone-texte");
        panneauDemo.setEditable(false);
        panneauDemo.setWrapText(true);
        panneauDemo.setPrefRowCount(9);
        panneauDemo.setMaxWidth(Double.MAX_VALUE);
        cacher(panneauDemo);                 // contenu replié par défaut

        toggleDemo.setOnAction(e -> basculerDemo());
        majToggleDemo();

        boiteDemo.getChildren().setAll(toggleDemo, panneauDemo);
        cacher(boiteDemo);                   // rien tant qu'aucun code n'est envoyé

        VBox carteLiaison = Ui.carte("Relier mon compte à un club",
                Ui.encartAlerte(PresentationProfil.TITRE_AVERTISSEMENT, PresentationProfil.AVERTISSEMENT),
                caseConfirmation,
                ligne(champNumeroClub, boutonEnvoyer),
                messageClub,
                zoneCode,
                boiteDemo);

        Node tuile = Ui.iconeTuile(Icones.GESTION, false);
        tuile.getStyleClass().add("profil-club-tuile");
        Label titreClub = Ui.label(PresentationProfil.TITRE_CLUB, "profil-bloc-club-titre");
        Label intro = Ui.label(PresentationProfil.INTRO_CLUB, "profil-bloc-club-intro");
        intro.setWrapText(true);

        VBox bloc = new VBox(16, Ui.ligne(12, tuile, titreClub), intro, boiteAcces, carteLiaison);
        bloc.getStyleClass().add("profil-bloc-club");
        return bloc;
    }

    /** Le bouton d'envoi ne s'active qu'après confirmation explicite de l'avertissement. */
    private void majEnvoiPossible() {
        boutonEnvoyer.setDisable(!caseConfirmation.isSelected());
    }

    /** Clubs reliés : pour chacun, niveau, tableau des droits, équipe et dissociation. */
    private void remplirAcces() {
        boiteAcces.getChildren().clear();
        List<AccesClub> acces = contexte.accesCourants();
        if (!PresentationProfil.afficheTableauDroits(acces)) {
            boiteAcces.getChildren().add(aide("Votre compte n'est relié à aucun club."));
            return;
        }
        for (PresentationProfil.ClubRelie c : PresentationProfil.clubsRelies(acces,
                num -> contexte.clubParNumero(num).map(Club::getNom).orElse(null))) {
            VBox identiteClub = new VBox(1, gras(c.nom()), discret("N° " + c.numero()));
            HBox entete = Ui.ligne(10, identiteClub, Ui.espace(),
                    badge(c.niveau()), actionDissociation(c));
            boiteAcces.getChildren().add(Ui.carte(entete,
                    Ui.sousTitre("Mes droits pour ce club"),
                    tableauDroits(c.droits()),
                    new Separator(),
                    Ui.sousTitre("Équipe de gestion"),
                    tableauMembres(contexte.membresDuClub(c.numero()))));
        }
    }

    private static GridPane tableauDroits(List<PresentationProfil.Droit> droits) {
        GridPane g = tableau("profil-tableau-droits", 2);
        entetes(g, "Action", "Autorisé", "Précision");
        int r = 1;
        for (PresentationProfil.Droit d : droits) {
            Label p = discret(d.precision());
            p.setWrapText(true);
            ligneTableau(g, r, new Label(d.action()),
                    Ui.pastille(d.etat().libelle(), d.etat().pastille()), p);
            r++;
        }
        return g;
    }

    /** @return l'action de dissociation d'un club, ou une mention pour le référent. */
    private Node actionDissociation(PresentationProfil.ClubRelie c) {
        if (!c.dissociable()) {
            return discret("référent — non dissociable");
        }
        Button b = Ui.boutonSecondaire("Se dissocier");
        b.setOnAction(e -> {
            if (contexte.dissocierClub(c.numero())) {
                remplirAcces();
                if (onHabilitationsChangees != null) {
                    onHabilitationsChangees.run();
                }
            }
        });
        return b;
    }

    private void envoyerCode() {
        cacherMessage(messageClub);
        String numero = valeur(champNumeroClub);
        Optional<String> refus = PresentationProfil.refusDemandeLiaison(caseConfirmation.isSelected(), numero);
        if (refus.isPresent()) {
            erreur(messageClub, refus.get());
            return;
        }
        Optional<DemandeVerification> demande = contexte.demanderLiaisonClub(numero);
        if (demande.isEmpty()) {
            demandeEnCours = null;
            numeroClubEnCours = null;
            cacher(zoneCode);
            cacher(boiteDemo);
            erreur(messageClub, "Aucun club ne porte ce numéro.");
            return;
        }
        demandeEnCours = demande.get();
        numeroClubEnCours = numero;
        champCode.clear();
        montrer(zoneCode);
        construireEmailType(numero);
        demoOuvert = false;          // replié par défaut à chaque envoi
        cacher(panneauDemo);
        majToggleDemo();
        montrer(boiteDemo);
        info(messageClub, "Un code a été envoyé à l'adresse officielle du club.");
    }

    private void validerCode() {
        if (demandeEnCours == null || numeroClubEnCours == null) {
            return;
        }
        ResultatLiaisonClub resultat =
                contexte.confirmerLiaisonClub(numeroClubEnCours, demandeEnCours, valeur(champCode));
        switch (resultat.getStatut().name()) {
            case "VERIFIE" -> {
                String niveau = resultat.getAcces().getNiveau().getLibelle();
                demandeEnCours = null;
                numeroClubEnCours = null;
                champNumeroClub.clear();
                champCode.clear();
                caseConfirmation.setSelected(false);
                cacher(zoneCode);
                demoOuvert = false;
                cacher(panneauDemo);
                cacher(boiteDemo);
                info(messageClub, "Club rattaché. Vous êtes « " + niveau + " ».");
                remplirAcces();
                if (onHabilitationsChangees != null) {
                    onHabilitationsChangees.run();
                }
            }
            case "CODE_EXPIRE" -> erreur(messageClub, "Le code a expiré. Renvoyez-en un nouveau.");
            case "CODE_INCORRECT" -> erreur(messageClub, "Code incorrect.");
            case "CLUB_INTROUVABLE" -> erreur(messageClub, "Club introuvable.");
            default -> erreur(messageClub, "Rattachement impossible.");
        }
    }

    /** Construit un email type (code masqué) adressé au référent du club visé. */
    private void construireEmailType(String numeroClub) {
        Club club = contexte.clubParNumero(numeroClub).orElse(null);
        if (club == null) {
            return;
        }
        String identiteRef = ((safe(club.getResponsablePrenom()) + " "
                + safe(club.getResponsableNom())).trim());
        if (identiteRef.isEmpty()) {
            identiteRef = "Référent du club";
        }
        CompteUtilisateur moi = contexte.compteConnecte();
        panneauDemo.setText(
                "Destinataire : " + identiteRef + " <" + club.getEmailOfficiel() + ">\n"
                        + "Objet        : Code de rattachement — club " + club.getNom() + "\n\n"
                        + "Une demande de rattachement au club " + club.getNom()
                        + " (" + club.getNumero() + ")\n"
                        + "a été faite par " + moi.getNomComplet() + " (" + moi.getEmail() + ").\n\n"
                        + "Code de vérification : XXXXXX\n"
                        + "Ce code est valable 30 minutes.\n\n"
                        + "(Exemple — le vrai code n'est pas affiché ici : il est envoyé au contact "
                        + "officiel du club.)");
    }

    private void basculerDemo() {
        demoOuvert = !demoOuvert;
        if (demoOuvert) {
            montrer(panneauDemo);
        } else {
            cacher(panneauDemo);
        }
        majToggleDemo();
    }

    private void majToggleDemo() {
        toggleDemo.setText((demoOuvert ? "▲ Masquer l'email type"
                : "▼ Voir l'email type envoyé au référent"));
    }

    private static GridPane tableauMembres(List<AccesClub> membres) {
        GridPane g = tableau("profil-tableau-membres", 3);
        entetes(g, "Nom", "Prénom", "Email", "Autorisation");
        int r = 1;
        for (AccesClub a : membres) {
            ligneTableau(g, r, new Label(safe(a.getNom())), new Label(safe(a.getPrenom())),
                    new Label(safe(a.getEmail())), new Label(a.getNiveau().getLibelle()));
            r++;
        }
        return g;
    }

    // Tableaux à lignes filetées (classes .profil-tableau*) -----------------

    /** Tableau pleine largeur ; la colonne {@code colonneExtensible} prend le reste. */
    private static GridPane tableau(String classe, int colonneExtensible) {
        GridPane g = new GridPane();
        g.getStyleClass().addAll("profil-tableau", classe);
        g.setMaxWidth(Double.MAX_VALUE);
        for (int i = 0; i <= colonneExtensible; i++) {
            ColumnConstraints c = new ColumnConstraints();
            if (i == colonneExtensible) {
                c.setHgrow(Priority.ALWAYS);
            }
            g.getColumnConstraints().add(c);
        }
        return g;
    }

    private static void entetes(GridPane g, String... titres) {
        for (int i = 0; i < titres.length; i++) {
            HBox c = cellule(Ui.enteteColonne(titres[i]));
            c.getStyleClass().add("profil-tableau-entete");
            g.add(c, i, 0);
        }
    }

    private static void ligneTableau(GridPane g, int rang, Node... valeurs) {
        for (int i = 0; i < valeurs.length; i++) {
            g.add(cellule(valeurs[i]), i, rang);
        }
    }

    /** Cellule pleine largeur de colonne, filet bas posé par la feuille de style. */
    private static HBox cellule(Node contenu) {
        HBox c = new HBox(contenu);
        c.setAlignment(Pos.CENTER_LEFT);
        c.getStyleClass().add("profil-tableau-cellule");
        c.setMaxWidth(Double.MAX_VALUE);
        c.setMaxHeight(Double.MAX_VALUE);
        if (contenu instanceof Label l && l.isWrapText()) {
            HBox.setHgrow(l, Priority.ALWAYS);
        }
        return c;
    }

    // Présentation : délègue à Ui (classes de theme.css) ------------------

    private static HBox ligne(Node... noeuds) {
        return Ui.ligne(8, noeuds);
    }

    private static TextField champ(String invite, double largeur) {
        TextField t = Ui.champ(invite);
        t.setPrefWidth(largeur);
        return t;
    }

    private static Label titre(String texte) {
        return Ui.titre(texte);
    }

    private static Label gras(String texte) {
        return Ui.gras(texte);
    }

    private static Label discret(String texte) {
        Label l = Ui.discret(texte);
        l.setWrapText(false);
        return l;
    }

    private static Label aide(String texte) {
        return Ui.aide(texte);
    }

    private static Label badge(String texte) {
        return Ui.badge(texte);
    }

    private static Label message() {
        return Ui.message();
    }

    private static void info(Label l, String texte) {
        Ui.succes(l, texte);
    }

    private static void erreur(Label l, String texte) {
        Ui.erreur(l, texte);
    }

    private static void cacherMessage(Label l) {
        Ui.cacher(l);
    }

    private static void montrer(Node n) {
        Ui.montrer(n);
    }

    private static void cacher(Node n) {
        Ui.cacher(n);
    }

    private static String valeur(TextField champ) {
        return champ.getText() == null ? "" : champ.getText().trim();
    }

    private static String safe(String v) {
        return v == null ? "" : v;
    }
}
