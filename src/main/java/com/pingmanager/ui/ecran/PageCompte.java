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

        VBox colonneDroite = new VBox(20);
        if (!contexte.emailVerifie()) {
            colonneDroite.getChildren().add(construireCarteVerificationEmail());
        }
        colonneDroite.getChildren().add(carteLicence);
        VBox blocPersonnel = new VBox(deuxColonnes(
                new VBox(20, carteIdentite, construireCarteTournoi(), construireCarteAppel(),
                        construireCarteDonneesPersonnelles()),
                colonneDroite));
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

    // Données personnelles (point 21, RGPD) --------------------------------

    private VBox construireCarteDonneesPersonnelles() {
        Label texte = new Label(com.pingmanager.core.MentionInformation.complete(
                com.pingmanager.core.ServiceDonneesPersonnelles.ANNEES_CONSERVATION_PAR_DEFAUT));
        texte.setWrapText(true);
        return Ui.carte(com.pingmanager.core.MentionInformation.TITRE, texte);
    }

    // Vérification de l'email (point 13) -----------------------------------

    private VBox construireCarteVerificationEmail() {
        TextField champCode = champ("code reçu", 160);
        Label msg = message();
        Label apercu = new Label();
        apercu.setWrapText(true);
        apercu.getStyleClass().add("discret");
        cacher(apercu);
        Button envoyer = Ui.boutonSecondaire("Recevoir un code");
        envoyer.setOnAction(e -> {
            contexte.envoyerCodeEmail();
            info(msg, "Un code a été envoyé à " + contexte.compteConnecte().getEmail() + ".");
            contexte.apercuEmailDemo().ifPresent(a -> {
                apercu.setText("Mode démo — email envoyé :\n" + a);
                montrer(apercu);
            });
        });
        Button valider = Ui.boutonPrimaire("Valider");
        valider.setOnAction(e -> {
            com.pingmanager.core.ResultatVerificationEmail r = contexte.confirmerEmail(valeur(champCode));
            if (r.estVerifie()) {
                info(msg, r.getMessage());
                if (onHabilitationsChangees != null) {
                    onHabilitationsChangees.run();
                }
            } else {
                erreur(msg, r.getMessage());
            }
        });
        return Ui.carte("Vérifier mon email",
                aide("Votre email n'est pas encore vérifié : votre compte n'a aucun droit "
                        + "d'organisateur ni de club, et ne peut pas lier de licence."),
                ligne(champCode, valider, envoyer), msg, apercu);
    }

    // Licence ---------------------------------------------------------------

    private void remplirLicence() {
        boiteLicence.getChildren().clear();
        CompteUtilisateur compte = contexte.compteConnecte();
        Label msg = message();

        if (compte.aLicenceLiee()) {
            Button delier = Ui.boutonSecondaire("Délier ma licence");
            delier.setOnAction(e -> {
                com.pingmanager.core.ResultatLiaisonLicence r = contexte.delierLicence();
                if (r.estReussi()) {
                    remplirLicence();
                } else {
                    erreur(msg, r.getMessage());
                }
            });
            boiteLicence.getChildren().addAll(grilleChamps(
                    PresentationProfil.licence(compte.getLicenceLiee(), contexte.ficheLicenceLiee())),
                    Ui.actions(delier), msg);
            return;
        }

        // Point 14 : numéro + date de naissance, puis code transmis par le référent du club.
        TextField champLicence = champ("numéro de licence", 200);
        javafx.scene.control.DatePicker champNaissance = new javafx.scene.control.DatePicker();
        champNaissance.setPromptText("date de naissance");
        Button demander = Ui.boutonPrimaire("Demander la liaison");
        TextField champCodeLicence = champ("code du référent", 160);
        Button valider = Ui.boutonPrimaire("Valider le code");
        VBox zoneCodeLicence = new VBox(8,
                aide("Le référent de votre club a reçu un code : demandez-le-lui et saisissez-le ici."),
                ligne(champCodeLicence, valider));
        if (contexte.licenceEnAttenteDeCode().isEmpty()) {
            cacher(zoneCodeLicence);
        }
        demander.setOnAction(e -> {
            String numero = valeur(champLicence);
            if (numero.isBlank() || champNaissance.getValue() == null) {
                erreur(msg, "Saisissez le numéro de licence et la date de naissance.");
                return;
            }
            com.pingmanager.core.ResultatLiaisonLicence r =
                    contexte.demanderLiaisonLicence(numero, champNaissance.getValue());
            if (r.estReussi()) {
                info(msg, r.getMessage());
                montrer(zoneCodeLicence);
            } else {
                erreur(msg, r.getMessage());
            }
        });
        valider.setOnAction(e -> {
            com.pingmanager.core.ResultatLiaisonLicence r = contexte.confirmerLiaisonLicence(valeur(champCodeLicence));
            if (r.estReussi()) {
                remplirLicence();
            } else {
                erreur(msg, r.getMessage());
            }
        });
        if (!contexte.emailVerifie()) {
            demander.setDisable(true);
        }
        boiteLicence.getChildren().addAll(
                aide("Saisissez votre numéro de licence et votre date de naissance. Pour prouver qu'il "
                        + "s'agit bien de vous, un code est envoyé au référent de votre club."),
                ligne(champLicence, champNaissance, demander),
                zoneCodeLicence,
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

    /**
     * Appel micro (B018) : sa prononciation en alphabet phonétique, avec
     * « Écouter » et l'aide à la saisie. Il faut une licence reliée : le
     * registre des prononciations est indexé par licence.
     */
    private VBox construireCarteAppel() {
        CompteUtilisateur compte = contexte.compteConnecte();
        if (!compte.aLicenceLiee()) {
            return Ui.carte(PresentationProfil.TITRE_APPEL,
                    grilleChamps(List.of(new PresentationProfil.Champ("Prononciation",
                            PresentationProfil.prononciation(null)))),
                    aide(PresentationPrononciation.AIDE_PROFIL_SANS_LICENCE));
        }
        Optional<com.pingmanager.model.FicheLicence> fiche = contexte.ficheLicenceLiee();
        String prenom = fiche.map(com.pingmanager.model.FicheLicence::getPrenom).orElse(compte.getPrenom());
        String nom = fiche.map(f -> com.pingmanager.core.TexteAnnonceVocale.nomPropre(f.getNom()))
                .orElse(compte.getNom());
        return Ui.carte(PresentationProfil.TITRE_APPEL,
                new FormulairePrononciation(contexte, compte.getLicenceLiee(), prenom, nom, null, null, null, null)
                        .build());
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
        if (contexte.estOrganisateur()) {
            bloc.getChildren().add(construireCarteRgpdClub());
        }
        bloc.getStyleClass().add("profil-bloc-club");
        return bloc;
    }

    /**
     * Point 21 (RGPD) : purge des tournois terminés (aperçu, puis confirmation explicite)
     * et effacement d'un joueur à sa demande, pour les tournois des clubs du compte.
     */
    private VBox construireCarteRgpdClub() {
        javafx.scene.control.Spinner<Integer> annees = new javafx.scene.control.Spinner<>(1, 20,
                com.pingmanager.core.ServiceDonneesPersonnelles.ANNEES_CONSERVATION_PAR_DEFAUT);
        annees.setPrefWidth(90);
        VBox listeApercu = new VBox(4);
        Label msgPurge = message();
        CheckBox confirmerPurge = new CheckBox("Je confirme la suppression définitive de ces tournois");
        Button supprimer = Ui.boutonSecondaire("Supprimer ces tournois");
        supprimer.getStyleClass().add("lien-danger");
        cacher(confirmerPurge);
        cacher(supprimer);
        @SuppressWarnings("unchecked")
        List<com.pingmanager.core.ServiceDonneesPersonnelles.TournoiAPurger<java.nio.file.Path>>[] apercu =
                new List[] {List.of()};
        Button voir = Ui.boutonSecondaire("Aperçu (rien n'est supprimé)");
        voir.setOnAction(e -> {
            apercu[0] = contexte.apercuPurge(annees.getValue());
            listeApercu.getChildren().clear();
            cacherMessage(msgPurge);
            confirmerPurge.setSelected(false);
            if (apercu[0].isEmpty()) {
                info(msgPurge, "Aucun tournoi terminé depuis plus de " + annees.getValue() + " ans.");
                cacher(confirmerPurge);
                cacher(supprimer);
                return;
            }
            for (com.pingmanager.core.ServiceDonneesPersonnelles.TournoiAPurger<java.nio.file.Path> a : apercu[0]) {
                listeApercu.getChildren().add(Ui.texte("•  " + a.nom() + " — dernier jour " + a.dernierJour()));
            }
            montrer(confirmerPurge);
            montrer(supprimer);
        });
        supprimer.disableProperty().bind(confirmerPurge.selectedProperty().not());
        supprimer.setOnAction(e -> {
            ContexteApplication.BilanPurge b = contexte.purgerTournois(apercu[0], annees.getValue(),
                    confirmerPurge.isSelected());
            listeApercu.getChildren().clear();
            cacher(confirmerPurge);
            cacher(supprimer);
            String texte = b.supprimes().size() + " tournoi(s) supprimé(s)."
                    + (b.refuses().isEmpty() ? "" : " Non supprimés : " + String.join(" ; ", b.refuses()));
            if (b.refuses().isEmpty()) {
                info(msgPurge, texte);
            } else {
                erreur(msgPurge, texte);
            }
        });

        TextField licence = champ("numéro de licence", 200);
        CheckBox demande = new CheckBox("Le joueur a demandé l'effacement de ses données");
        Label msgEffacement = message();
        Button effacer = Ui.boutonSecondaire("Effacer le joueur");
        effacer.disableProperty().bind(demande.selectedProperty().not());
        effacer.setOnAction(e -> {
            ContexteApplication.BilanEffacement b = contexte.effacerJoueur(valeur(licence), demande.isSelected());
            String texte = "Joueur effacé dans " + b.tournoisModifies() + " tournoi(s)"
                    + (b.prononciationRetiree() ? ", prononciation retirée" : "") + "."
                    + (b.refuses().isEmpty() ? "" : " Refus : " + String.join(" ; ", b.refuses()));
            if (b.refuses().isEmpty()) {
                info(msgEffacement, texte);
                licence.clear();
                demande.setSelected(false);
            } else {
                erreur(msgEffacement, texte);
            }
        });

        return Ui.carte("Données personnelles des joueurs",
                Ui.sousTitre("Tournois terminés"),
                aide("Supprime les tournois de vos clubs terminés depuis plus du nombre d'années choisi. "
                        + "Regardez d'abord l'aperçu ; la suppression demande une confirmation."),
                ligne(new Label("Conserver"), annees, new Label("ans"), voir),
                listeApercu, confirmerPurge, ligne(supprimer), msgPurge,
                new Separator(),
                Ui.sousTitre("Effacer un joueur à sa demande"),
                aide("Remplace son nom et son prénom par « ANONYME Joueur » et efface son numéro de portable dans "
                        + "les tournois de vos clubs, terminés ou non. Son numéro de licence et ses résultats "
                        + "sportifs restent."),
                ligne(licence, effacer), demande, msgEffacement);
    }

    /** Point 14 : un administrateur délie une licence de son club du compte qui la porte. */
    private Node formulaireDeliaisonLicence() {
        TextField champ = champ("numéro de licence", 200);
        Label msg = message();
        Button delier = Ui.boutonSecondaire("Délier");
        delier.setOnAction(e -> {
            com.pingmanager.core.ResultatLiaisonLicence r = contexte.delierLicenceParAdministrateur(valeur(champ));
            if (r.estReussi()) {
                info(msg, r.getMessage());
                champ.clear();
            } else {
                erreur(msg, r.getMessage());
            }
        });
        return new VBox(8, aide("Si un compte s'est lié à la licence d'un de vos licenciés, "
                + "vous pouvez la délier : le licencié pourra alors la lier à son propre compte."),
                ligne(champ, delier), msg);
    }

    /** Le bouton d'envoi ne s'active qu'après confirmation explicite de l'avertissement. */
    private void majEnvoiPossible() {
        boutonEnvoyer.setDisable(!caseConfirmation.isSelected() || !contexte.emailVerifie());
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
                    tableauMembres(contexte.membresDuClub(c.numero())),
                    new Separator(),
                    Ui.sousTitre("Licence liée à un compte par erreur"),
                    formulaireDeliaisonLicence()));
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
            case "TROP_D_ESSAIS" -> erreur(messageClub, resultat.messageBlocage());
            case "DEMANDE_D_UN_AUTRE_CLUB" -> erreur(messageClub, "Ce code a été demandé pour un autre club.");
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
