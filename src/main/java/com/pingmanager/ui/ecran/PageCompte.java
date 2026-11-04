package com.pingmanager.ui.ecran;

import com.pingmanager.core.DemandeVerification;
import com.pingmanager.core.ResultatLiaisonClub;
import com.pingmanager.model.AccesClub;
import com.pingmanager.model.Club;
import com.pingmanager.model.CompteUtilisateur;
import com.pingmanager.model.FicheLicence;
import com.pingmanager.ui.ContexteApplication;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Page « Compte » : identité, liaison à la licence FFTT, et liaison à un ou
 * plusieurs clubs en tant qu'organisateur.
 */
public final class PageCompte {

    private static final DateTimeFormatter DATE_FR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ContexteApplication contexte;
    private final Runnable onHabilitationsChangees;

    private DemandeVerification demandeEnCours;
    private String numeroClubEnCours;

    private final VBox boiteLicence = new VBox(10);
    private final VBox boiteAcces = new VBox(8);
    private final TextField champNumeroClub = champ("numéro de club", 200);
    private final Label messageClub = message();
    private final VBox zoneCode = new VBox(8);
    private final TextField champCode = champ("code reçu", 140);
    private final TextArea panneauDemo = new TextArea();
    private final VBox boiteDemo = new VBox(6);
    private final Hyperlink toggleDemo = new Hyperlink();
    private boolean demoOuvert = false;

    public PageCompte(ContexteApplication contexte, Runnable onHabilitationsChangees) {
        this.contexte = contexte;
        this.onHabilitationsChangees = onHabilitationsChangees;
    }

    public Parent build() {
        CompteUtilisateur compte = contexte.compteConnecte();

        VBox carteIdentite = carte("Identité",
                etiquetteValeur("Nom", compte.getNomComplet()),
                etiquetteValeur("Email", compte.getEmail()));

        remplirLicence();
        VBox carteLicence = carte("Licence FFTT", boiteLicence);

        VBox carteClubs = carte("Clubs", construireCorpsClub());

        VBox contenu = Ui.page(680, titre("Mon compte"), carteIdentite, carteLicence, carteClubs);
        if (contexte.estOrganisateur()) {
            contenu.getChildren().add(construireCarteEquipe());
        }
        return Ui.defilant(contenu);
    }

    // Licence ---------------------------------------------------------------

    private void remplirLicence() {
        boiteLicence.getChildren().clear();
        CompteUtilisateur compte = contexte.compteConnecte();

        if (compte.aLicenceLiee()) {
            GridPane fiche = new GridPane();
            fiche.setHgap(24);
            fiche.setVgap(8);
            int r = 0;
            ligneFiche(fiche, r++, "Licence", compte.getLicenceLiee());
            Optional<FicheLicence> f = contexte.ficheLicenceLiee();
            if (f.isPresent()) {
                FicheLicence fl = f.get();
                ligneFiche(fiche, r++, "Titulaire", fl.getNomComplet());
                if (fl.getDateNaissance() != null) {
                    ligneFiche(fiche, r++, "Naissance", fl.getDateNaissance().format(DATE_FR));
                }
                if (fl.getNationalite() != null) {
                    ligneFiche(fiche, r++, "Nationalité", fl.getNationalite());
                }
                if (fl.getNomClub() != null) {
                    ligneFiche(fiche, r++, "Club", fl.getNomClub());
                }
                if (fl.getSaison() != null) {
                    ligneFiche(fiche, r++, "Saison", fl.getSaison());
                }
                if (fl.getCategorie() != null) {
                    ligneFiche(fiche, r++, "Catégorie", fl.getCategorie());
                }
                if (fl.getClassement() != null) {
                    ligneFiche(fiche, r++, "Classement", fl.getClassement());
                }
                ligneFiche(fiche, r++, "Points (début de phase)", String.valueOf(fl.getPoints()));
                if (fl.getPointsMensuels() > 0) {
                    ligneFiche(fiche, r++, "Points (mensuel)", String.valueOf(fl.getPointsMensuels()));
                }
                if (fl.getTypeLicence() != null) {
                    ligneFiche(fiche, r++, "Type de licence", fl.getTypeLicence());
                }
                if (fl.getStatut() != null) {
                    ligneFiche(fiche, r++, "Statut", fl.getStatut());
                }
                String officiel = qualifications(fl);
                if (officiel != null) {
                    ligneFiche(fiche, r++, "Officiel", officiel);
                }
            }
            boiteLicence.getChildren().add(fiche);
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

    // Clubs -----------------------------------------------------------------

    private VBox construireCorpsClub() {
        remplirAcces();

        Button envoyer = Ui.boutonPrimaire("Envoyer le code");
        envoyer.setOnAction(e -> envoyerCode());

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

        return new VBox(12,
                boiteAcces,
                new Separator(),
                Ui.sousTitre("Rattacher un club (gestion uniquement)"),
                aide("Cette étape ne concerne que les personnes qui auront des droits "
                        + "d'administration pour gérer les tournois d'un club. Si ce n'est pas "
                        + "votre cas, inutile de rattacher un club : cela n'empêche en rien de "
                        + "vous inscrire à des tournois ni d'accéder aux informations. Pour la "
                        + "gestion, un code de vérification est envoyé à l'adresse officielle du "
                        + "club, ce qui confirme que vous y avez accès."),
                ligne(champNumeroClub, envoyer),
                messageClub,
                zoneCode,
                boiteDemo);
    }

    private void remplirAcces() {
        boiteAcces.getChildren().clear();
        List<AccesClub> acces = contexte.accesCourants();
        if (acces.isEmpty()) {
            boiteAcces.getChildren().add(aide("Aucun club rattaché pour l'instant."));
            return;
        }
        for (AccesClub a : acces) {
            String nomClub = contexte.clubParNumero(a.getNumeroClub())
                    .map(Club::getNom).orElse("Club " + a.getNumeroClub());

            VBox identiteClub = new VBox(1, gras(nomClub), discret("N° " + a.getNumeroClub()));
            HBox ligne = Ui.ligne(10, identiteClub, Ui.espace(),
                    badge(a.getNiveau().getLibelle()), actionDissociation(a));
            boiteAcces.getChildren().add(ligne);
        }
    }

    /** @return l'action de dissociation d'un club, ou une mention pour le référent. */
    private javafx.scene.Node actionDissociation(AccesClub a) {
        if (a.estPrincipal()) {
            return discret("référent — non dissociable");
        }
        Button b = Ui.boutonSecondaire("Se dissocier");
        b.setOnAction(e -> {
            if (contexte.dissocierClub(a.getNumeroClub())) {
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
        if (numero.isBlank()) {
            erreur(messageClub, "Saisissez un numéro de club.");
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

    private static String qualifications(FicheLicence fl) {
        StringBuilder sb = new StringBuilder();
        if (fl.getArbitre() != null) {
            sb.append("Arbitre ").append(fl.getArbitre());
        }
        if (fl.getJugeArbitre() != null) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append("Juge-arbitre ").append(fl.getJugeArbitre());
        }
        if (fl.getTechnique() != null) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append("Technique ").append(fl.getTechnique());
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    // Équipe de gestion -----------------------------------------------------

    private VBox construireCarteEquipe() {
        VBox corps = new VBox(16);
        for (AccesClub mien : contexte.accesCourants()) {
            String num = mien.getNumeroClub();
            String nomClub = contexte.clubParNumero(num).map(Club::getNom).orElse("Club " + num);
            corps.getChildren().add(new VBox(6,
                    gras(nomClub),
                    tableauMembres(contexte.membresDuClub(num))));
        }
        return carte("Équipe de gestion", corps);
    }

    private static GridPane tableauMembres(List<AccesClub> membres) {
        GridPane g = new GridPane();
        g.setHgap(18);
        g.setVgap(6);
        g.add(enteteCol("Nom"), 0, 0);
        g.add(enteteCol("Prénom"), 1, 0);
        g.add(enteteCol("Email"), 2, 0);
        g.add(enteteCol("Autorisation"), 3, 0);
        int r = 1;
        for (AccesClub a : membres) {
            g.add(new Label(safe(a.getNom())), 0, r);
            g.add(new Label(safe(a.getPrenom())), 1, r);
            g.add(new Label(safe(a.getEmail())), 2, r);
            g.add(new Label(a.getNiveau().getLibelle()), 3, r);
            r++;
        }
        return g;
    }

    private static Label enteteCol(String texte) {
        return Ui.enteteColonne(texte);
    }

    // Présentation : délègue à Ui (classes de theme.css) ------------------

    private static VBox carte(String titre, Node... contenu) {
        return Ui.carte(titre, contenu);
    }

    private static void ligneFiche(GridPane grille, int rang, String cle, String valeur) {
        grille.add(discret(cle), 0, rang);
        grille.add(new Label(valeur), 1, rang);
    }

    private static HBox etiquetteValeur(String cle, String valeur) {
        Label c = discret(cle);
        c.setMinWidth(70);
        HBox h = new HBox(16, c, new Label(valeur));
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

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
