package com.pingmanager.ui.ecran;

import com.pingmanager.model.Tournoi;
import com.pingmanager.model.TypeTournoi;
import com.pingmanager.ui.ContexteApplication;
import com.pingmanager.ui.infra.DonneesDemo;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Labeled;
import javafx.scene.control.TextField;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import javafx.util.Duration;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Banc de captures : args = dossier de sortie, "sombre" optionnel. */
public class Capture extends Application {

    static String out;
    static boolean sombre;
    static boolean brunoy;
    static boolean onglet;
    static boolean importer;
    static boolean choix;
    static boolean v2;
    static boolean accueil;
    static boolean suivi;
    static boolean jourj;
    Scene scene;
    ContexteApplication ctx;
    final List<Runnable> etapes = new ArrayList<>();

    public static void main(String[] a) {
        out = a[0];
        sombre = java.util.Arrays.asList(a).contains("sombre");
        brunoy = java.util.Arrays.asList(a).contains("brunoy");
        onglet = java.util.Arrays.asList(a).contains("onglet");
        importer = java.util.Arrays.asList(a).contains("import");
        choix = java.util.Arrays.asList(a).contains("choix");
        v2 = java.util.Arrays.asList(a).contains("v2");
        accueil = java.util.Arrays.asList(a).contains("accueil");
        suivi = java.util.Arrays.asList(a).contains("suivi");
        jourj = java.util.Arrays.asList(a).contains("jourj");
        launch(a);
    }

    @Override
    public void start(Stage stage) throws Exception {
        Path data = Files.createTempDirectory("pm");
        ctx = new ContexteApplication(data, DonneesDemo.clubs(), DonneesDemo.licences());
        ctx.connecter(DonneesDemo.EMAIL_QUENTIN, "Soumet123/");
        Tournoi t = new Tournoi("Tournoi national de Brunoy", "Brunoy",
                List.of(LocalDate.of(2027, 3, 13), LocalDate.of(2027, 3, 14)));
        t.setNiveau(com.pingmanager.model.NiveauTournoi.REGIONAL); t.setNumeroAffiliationClub("08911132");
        t.setPeriode(LocalDate.of(2027, 3, 13), LocalDate.of(2027, 3, 14));
        t.setCorrespondant("SOUMET", "Quentin", DonneesDemo.EMAIL_QUENTIN, null, null);
        // Deux tableaux de démonstration.
        com.pingmanager.model.Tableau ta = new com.pingmanager.model.Tableau("A",
                com.pingmanager.model.ConfigurationTableau.depuisFormat(com.pingmanager.model.format.RegistreFormats.formatParDefaut())
                        .tranche(500, 899).placesMax(48).tailleMaxListeAttente(8)
                        .tarif(new com.pingmanager.model.TarifInscription(8, 10))
                        .recompenses(com.pingmanager.model.GrilleRecompenses.builder()
                                .place(1, com.pingmanager.model.Recompense.argent(50))
                                .place(2, com.pingmanager.model.Recompense.argent(30))
                                .plage(3, 4, com.pingmanager.model.Recompense.autre("Coupe")).build())
                        .build());
        ta.setType("Points 500 à 899 pts");
        ta.setHoraires(new com.pingmanager.model.HorairesTableau(
                java.time.LocalDateTime.of(2027, 3, 13, 8, 0), java.time.LocalDateTime.of(2027, 3, 13, 8, 45),
                java.time.LocalDateTime.of(2027, 3, 13, 9, 0), java.time.LocalDateTime.of(2027, 3, 13, 13, 0)));
        t.ajouterTableau(ta);
        com.pingmanager.model.Tableau tf = new com.pingmanager.model.Tableau("Féminin",
                com.pingmanager.model.ConfigurationTableau.depuisFormat(com.pingmanager.model.format.RegistreFormats.formatParDefaut())
                        .toutesSeries().sexeRequis(com.pingmanager.model.Sexe.FEMME).placesMax(24)
                        .tarif(new com.pingmanager.model.TarifInscription(8, true, 0, false)).build());
        tf.setType("Toutes séries");
        tf.setHoraires(new com.pingmanager.model.HorairesTableau(null, null,
                java.time.LocalDateTime.of(2027, 3, 14, 10, 0)));
        t.ajouterTableau(tf);
        t.setImagePromotionnelle(new com.pingmanager.model.DocumentReference("Affiche",
                "/tmp/claude-0/-home-claude/40c9c3cb-8e04-5a97-98c0-6872a2d479a8/scratchpad/affiche-demo.png"));
        ctx.creerBrouillon(t);
        Tournoi t2 = new Tournoi("Open d'automne", "Brunoy", List.of(LocalDate.of(2026, 11, 21)));
        ctx.creerBrouillon(t2);

        scene = new Scene(new javafx.scene.layout.Pane(), 1100, 760);
        installerTheme();
        stage.setScene(scene);
        stage.show();

        Coquille[] coq = new Coquille[1];
        if (jourj) {
            stage.setWidth(1440);
            stage.setHeight(900);
            etapesJourJ(coq);
            lancer(0);
            return;
        }
        if (accueil) {
            stage.setWidth(1440);
            stage.setHeight(900);
            etapesAccueil(coq);
            lancer(0);
            return;
        }
        if (v2) {
            stage.setWidth(1440);
            stage.setHeight(900);
            etapesV2(coq);
            lancer(0);
            return;
        }
        if (choix) {
            stage.setWidth(1360);
            stage.setHeight(880);
            etapesChoix(coq);
            lancer(0);
            return;
        }
        if (importer) {
            stage.setWidth(1360);
            stage.setHeight(880);
            etapesImport(coq);
            lancer(0);
            return;
        }
        if (suivi) {
            stage.setWidth(1440);
            stage.setHeight(900);
            etapesSuivi(coq);
            lancer(0);
            return;
        }
        if (onglet) {
            stage.setWidth(1360);
            stage.setHeight(880);
            etapesOnglet(coq);
            lancer(0);
            return;
        }
        if (brunoy) {
            etapesBrunoy(coq);
            lancer(0);
            return;
        }
        etape(() -> scene.setRoot(new EcranConnexion(ctx, () -> { }).build()), "01-connexion");
        etape(() -> {
            coq[0] = new Coquille(ctx, () -> { });
            scene.setRoot(coq[0].build());
        }, "02-accueil");
        etape(() -> coq[0].afficher("profil"), "03-profil");
        etape(() -> coq[0].afficher("gestion"), "04-gestion");
        etape(() -> cliquer("Créer un tournoi"), "05-nouveau");
        etape(() -> { cliquer("Annuler"); }, null);
        etape(() -> cliquer("Compléter"), "06-completion");
        etape(() -> cliquer("Modifier"), "07-formulaire");
        etape(() -> {
            List<TextField> champs = new ArrayList<>();
            collect(scene.getRoot(), TextField.class, champs::add);
            champs.stream().filter(f -> "jj/mm/aaaa".equals(f.getPromptText())).findFirst().orElseThrow().setText("13-03-27");
            cliquer("Enregistrer");
        }, "08-formulaire-erreur");
        etape(() -> {
            List<javafx.scene.control.ScrollPane> sp = new ArrayList<>();
            collect(scene.getRoot(), javafx.scene.control.ScrollPane.class, sp::add);
            sp.get(sp.size() - 1).setVvalue(1.0);
        }, "08b-formulaire-erreur-bas");
        etape(() -> coq[0].afficher("stats"), "09-placeholder");
        etape(() -> coq[0].afficher("gestion"), null);
        etape(() -> cliquerN("Compléter", 1), null);
        etape(() -> cliquer("Gérer les tableaux"), "10-tableaux");
        etape(() -> basDePage(), "10b-tableaux-bas");
        etape(() -> cliquer("Ajouter un tableau"), "11-nouveau-tableau");
        etape(() -> defiler(0.45), "11b-nouveau-tableau-milieu");
        etape(() -> basDePage(), "11c-nouveau-tableau-bas");
        etape(() -> cliquer("Valider le tableau"), "12-erreur-tableau");
        etape(() -> {
            saisir("ex. 500", "900");
            saisir("ex. 899", "1299");
            saisir("ex. 9h", "13h30");
            saisir("ex. 8h", "12h45");
            saisir("ex. 8h45", "13h15");
            saisir("ex. 48", "40");
            List<TextField> m = new ArrayList<>();
            collect(scene.getRoot(), TextField.class, f -> { if ("montant en €".equals(f.getPromptText())) m.add(f); });
            m.get(0).setText("8"); m.get(1).setText("10");
            List<javafx.scene.control.DatePicker> dp = new ArrayList<>();
            collect(scene.getRoot(), javafx.scene.control.DatePicker.class, dp::add);
            dp.get(0).setValue(LocalDate.of(2027, 3, 13));
            cliquer("+ Ajouter une récompense");
        }, null);
        etape(() -> {
            saisir("de", "1");
            saisir("ex. 50", "60");
        }, "12b-saisie-tableau");
        etape(() -> cliquer("Valider le tableau"), "13-tableaux-apres");
        etape(() -> {
            saisirN("illimité", 0, "2");
            saisirN("illimité", 1, "3");
            saisir("—", "12");
            cliquer("Selon le remplissage");
            saisirN("ex. 50", 0, "50");
            saisirN("ex. 50", 1, "50");
            cliquerN("Complet", 0);
            cliquerN("Partiel", 1);
            saisirN("ex. 50", 3, "50");
            cliquerN("Sur justificatif", 1);
            cliquerN("Aucun remboursement", 2);
            cliquer("Les frais de paiement en ligne ne sont pas remboursés");
            defiler(0.62);
        }, "13b-regles");
        etape(() -> { cliquer("Le nombre varie selon le jour"); cliquer("Enregistrer les règles"); basDePage(); }, "13c-regles-enregistrees");
        etape(() -> cliquer("Ajouter un tableau"), null);
        etape(() -> {
            List<javafx.scene.control.ComboBox> cb = new ArrayList<>();
            collect(scene.getRoot(), javafx.scene.control.ComboBox.class, cb::add);
            cb.get(0).getSelectionModel().select("Féminin");
            javafx.scene.control.ComboBox sexe = cb.stream().filter(c -> c.getItems().contains("Femmes uniquement")).findFirst().orElseThrow();
            List<javafx.scene.control.RadioButton> rb = new ArrayList<>();
            collect(scene.getRoot(), javafx.scene.control.RadioButton.class, rb::add);
            System.out.println("VERIF nom=Féminin -> sexe=" + sexe.getValue());
            cb.get(0).getSelectionModel().select("Non numérotés");
            System.out.println("VERIF nom=Non numérotés -> toutesSeries=" + rb.get(2).isSelected());
            defiler(0.3);
        }, "14-nom-usuel");
        etape(() -> { cliquer("Toutes catégories"); cliquer("Sélectionner des catégories"); defiler(0.32); }, "14b-categories");
        etape(() -> { cliquer("Annuler"); cliquer("← Vue d'ensemble"); }, null);
        etape(() -> cliquer("Gérer"), "15-sportif");
        etape(() -> chercher(0, "rou"), "16a-recherche");
        etape(() -> cliquerN("Désigner", 0), "16-ja-refus");
        etape(() -> chercher(0, "lemoine"), null);
        etape(() -> cliquerN("Désigner", 0), "17-ja-ok");
        etape(() -> chercher(1, "martinez"), null);
        etape(() -> { cliquerN("Désigner", 0); defiler(0.25); }, "18-arbitre-refus");
        etape(() -> chercher(1, "petit"), null);
        etape(() -> { cliquerN("Désigner", 0); defiler(0.3); }, "18b-arbitre-ok");
        etape(() -> {
            cliquer("Apportées par les joueurs");
            cliquer("Orange");
            cliquer("+ Balles en celluloïd");
            saisir("nombre", "12");
            saisir("marque", "Cornilleau");
            cliquer("+ Ajouter des tables d'une autre marque ou couleur");
            defiler(0.62);
        }, "19-materiel");
        etape(() -> { cliquer("Enregistrer le matériel et l'aire de jeu"); basDePage(); }, "20-rappel");
        etape(() -> cliquer("← Vue d'ensemble"), null);
        etape(() -> cliquerN("Gérer", 1), "21-complementaire");
        etape(() -> {
            List<javafx.scene.control.MenuButton> mb = new ArrayList<>();
            collect(scene.getRoot(), javafx.scene.control.MenuButton.class, mb::add);
            mb.get(0).getItems().get(1).fire();   // Buvette
            mb.get(0).getItems().get(6).fire();   // Informations pratiques
            mb.get(0).getItems().get(7).fire();   // Article libre
        }, null);
        etape(() -> {
            saisir("ex. de 8 h à 20 h, restauration chaude le midi", "de 8 h à 20 h, restauration chaude le midi");
            cliquer("Parking");
            cliquer("Accès PMR");
            saisir("ex. Tenue des bénévoles", "Tenue");
            defiler(0.22);
        }, "21b-articles");
        etape(() -> {
            List<javafx.scene.control.MenuButton> mb = new ArrayList<>();
            collect(scene.getRoot(), javafx.scene.control.MenuButton.class, mb::add);
            mb.get(0).show();
        }, "21c-menu-articles");
        etape(() -> {
            List<javafx.scene.control.MenuButton> mb = new ArrayList<>();
            collect(scene.getRoot(), javafx.scene.control.MenuButton.class, mb::add);
            mb.get(0).hide();
            saisir("https://www.helloasso.com/associations/…", "https://www.helloasso.com/associations/brunoy-tt/evenements/tournoi-2027");
            basDePage();
        }, "21d-paiement");
        etape(() -> {
            saisir("https://www.helloasso.com/associations/…", "https://exemple.fr/payer");
            basDePage();
        }, "21e-paiement-alerte");
        etape(() -> {
            saisir("https://www.helloasso.com/associations/…", "https://www.helloasso.com/associations/brunoy-tt/evenements/tournoi-2027");
            List<javafx.scene.control.TextArea> z = new ArrayList<>();
            collect(scene.getRoot(), javafx.scene.control.TextArea.class, z::add);
            z.get(z.size() - 1).setText("Short ou jupette et maillot de sport sont exigés pour les bénévoles à la table de marque.");
        }, null);
        etape(() -> cliquer("Aperçu du règlement"), "22-apercu");
        etape(() -> defiler(0.4), "22b-apercu-milieu");
        etape(() -> basDePage(), "22c-apercu-bas");
        lancer(0);
    }

    void etapesBrunoy(Coquille[] coq) {
        etape(() -> {
            coq[0] = new Coquille(ctx, () -> { });
            scene.setRoot(coq[0].build());
            coq[0].afficher("gestion");
        }, "b01-gestion");
        etape(() -> cliquerN("Compléter", 2), "b02-vue-ensemble");
        etape(() -> defiler(1.0), "b02b-vue-ensemble-bas");
        etape(() -> cliquer("Modifier"), "b03-general");
        etape(() -> defiler(0.5), "b03b-general-milieu");
        etape(() -> cliquer("Annuler"), null);
        etape(() -> cliquer("Gérer les tableaux"), "b04-tableaux");
        etape(() -> defiler(0.3), "b04b-grille");
        etape(() -> defiler(0.62), "b04c-regles");
        etape(() -> defiler(0.85), "b04d-forfaits");
        etape(() -> defiler(1.0), "b04e-ordonnancement");
        etape(() -> cliquer("Proposer un ordre"), null);
        etape(() -> defiler(0.93), "b04f-ordre-propose");
        etape(() -> defiler(1.0), "b04g-ordre-appliquer");
        etape(() -> cliquerN("Modifier", 6), "b05-tableau-non-numerotes");
        etape(() -> defiler(0.5), "b05b-tableau-milieu");
        etape(() -> defiler(1.0), "b05c-tableau-bas");
        etape(() -> { cliquer("Annuler"); cliquer("← Vue d'ensemble"); }, null);
        etape(() -> cliquer("Gérer"), "b06-sportif");
        etape(() -> defiler(0.6), "b06b-materiel");
        etape(() -> defiler(1.0), "b06c-rappel");
        etape(() -> cliquer("← Vue d'ensemble"), null);
        etape(() -> cliquerN("Gérer", 1), "b07-complementaire");
        etape(() -> defiler(0.4), "b07b-articles");
        etape(() -> defiler(1.0), "b07c-paiement");
        etape(() -> cliquer("Aperçu du règlement"), "b08-apercu");
        etape(() -> defiler(0.25), "b08b-apercu");
        etape(() -> defiler(0.5), "b08c-apercu");
        etape(() -> defiler(0.75), "b08d-apercu");
        etape(() -> defiler(1.0), "b08e-apercu");
        etape(() -> { cliquer("← Complémentaire"); cliquer("← Vue d'ensemble"); defiler(1.0); }, "b09-etapes");
        etape(() -> { cliquer("Demander l'homologation"); defiler(1.0); }, "b09b-demande-faite");
        etape(() -> { cliquer("← Gestion des tournois"); }, "b10-gestion-demandes");
        etape(() -> {
            ctx.deconnecter();
            ctx.connecter(DonneesDemo.EMAIL_ULTRA_ADMIN, DonneesDemo.MDP_ULTRA_ADMIN);
            coq[0] = new Coquille(ctx, () -> { });
            scene.setRoot(coq[0].build());
            coq[0].afficher("supervision");
        }, "b11-supervision");
        etape(() -> cliquerN("Ouvrir", 2), "b12-supervision-ouvert");
    }

    /** Accueil (B028) : compte de club (Quentin) puis compte joueur sans accès club, 1440 px. */
    void etapesAccueil(Coquille[] coq) {
        etape(() -> {
            coq[0] = new Coquille(ctx, () -> { });
            scene.setRoot(coq[0].build());
        }, "accueil-club");
        etape(() -> defiler(1.0), "accueil-club-bas");
        etape(() -> {
            ctx.deconnecter();
            com.pingmanager.model.FicheLicence f = com.pingmanager.ui.infra.DonneesDemo.JOUEURS_FICTIFS.get(0);
            ctx.creerCompteEtConnecter("joueur.demo@exemple.fr", f.getNom(), f.getPrenom(), "Joueur123/");
            ctx.lierLicence(f.getNumeroLicence());
            coq[0] = new Coquille(ctx, () -> { });
            scene.setRoot(coq[0].build());
        }, "accueil-joueur");
        etape(() -> {
            ctx.deconnecter();
            ctx.creerCompteEtConnecter("sans.licence@exemple.fr", "DURAND", "Paul", "Joueur123/");
            coq[0] = new Coquille(ctx, () -> { });
            scene.setRoot(coq[0].build());
        }, "accueil-sans-licence");
    }

    /** Charte v2 : écrans représentatifs, largeur 1440 (noms suffixés -clair / -sombre). */
    void etapesV2(Coquille[] coq) {
        etape(() -> scene.setRoot(new EcranConnexion(ctx, () -> { }).build()), "connexion");
        etape(() -> {
            coq[0] = new Coquille(ctx, () -> { });
            scene.setRoot(coq[0].build());
        }, "accueil");
        etape(() -> coq[0].afficher("profil"), "profil");
        etape(() -> coq[0].afficher("gestion"), "gestion-tournois");
        etape(() -> defiler(1.0), "gestion-tournois-bas");
        etape(() -> {
            coq[0].afficher("gestion");
            cliquer("Créer un tournoi");
        }, "nouveau-tournoi");
        etape(() -> {
            coq[0].afficher("gestion");
            cliquer("Compléter");
        }, "vue-ensemble-tournoi");
        etape(() -> {
            coq[0].afficher("gestion");
            cliquer("Suivre le tournoi");
        }, "jour-j");
        etape(() -> coq[0].afficher("stats"), "page-a-venir");
        etape(() -> coq[0].afficher("tournois"), "tournois-carte", 2500);
    }

    /**
     * Jour J du tournoi de démonstration (B011) : écran principal du juge-arbitre,
     * envoi d'un match, saisie du score, détail d'un tableau. Les boutons absents
     * (capture « avant ») sont ignorés.
     */
    void etapesJourJ(Coquille[] coq) {
        etape(() -> {
            coq[0] = new Coquille(ctx, () -> { });
            scene.setRoot(coq[0].build());
            coq[0].afficher("gestion");
            cliquer("Suivre le tournoi");
        }, "j01-jour-j");
        etape(() -> defiler(1.0), "j01b-jour-j-bas");
        // B037 : annonce, joueurs en attente, lancer un match, annuler, jour suivant.
        etape(() -> { defiler(0); cliquerDebut("Table "); }, "j10-annonce-question");
        etape(() -> cliquerSiPresent("SMS"), "j11-annonce-affichee");
        etape(() -> clicStyle("jourj-joueur-attente", 0), "j12-joueur-pourquoi");
        etape(() -> clicStyle("jourj-joueur-attente", 0), null);
        etape(() -> { cliquerSiPresent("← Matchs proposés"); clicStyle("jourj-joueur-attente", 3); },
                "j12b-joueur-pourquoi-autre");
        etape(() -> { cliquerSiPresent("← Matchs proposés"); cliquerDebut("_Lancer un match"); }, "j13-lancer-un-match");
        etape(() -> saisir("Nom d'un joueur ou d'un tableau", "open du"), "j13b-lancer-recherche");
        etape(() -> { cliquerSiPresent("← Matchs proposés"); cliquerDebut("Annuler"); }, null);
        etape(() -> defiler(0.6), "j14-annuler-confirmation");
        etape(() -> { cliquerSiPresent("Non"); cliquerSiPresent("Jour suivant →"); }, "j15-jour-suivant");
        etape(() -> cliquerSiPresent("← Jour précédent"), null);
        etape(() -> { defiler(0); cliquerSiPresent("Ouvrir la salle"); cliquerDebut("Table "); }, "j02-apres-envoi");
        etape(() -> cliquerSiPresent("Terminer"), "j03-saisie-score");
        etape(() -> {
            try {
                saisir("1", "11-7");
                saisir("2", "9-11");
                saisir("3", "11-4");
                saisir("4", "11-8");
                cliquerSiPresent("Enregistrer le score");
            } catch (IllegalStateException autreEditeur) {
                System.out.println("(saisie non faite : " + autreEditeur.getMessage() + ")");
            }
        }, "j04-apres-score");
        etape(() -> cliquerDebut("Envoyer "), "j05-match-suivant");
        etape(() -> cliquerSiPresent("C – Toutes séries"), "j06-detail-poules");
        etape(() -> { cliquerSiPresent("← Jour J"); cliquerSiPresent("F – Open du matin"); }, "j07-detail-arbre");
    }

    /** Déclenche le clic du n-ième nœud portant la classe de style donnée (lignes cliquables). */
    void clicStyle(String classe, int rang) {
        List<javafx.scene.Node> ns = new ArrayList<>();
        collect(scene.getRoot(), javafx.scene.Node.class, n -> {
            if (n.getStyleClass().contains(classe) && n.getOnMouseClicked() != null) {
                ns.add(n);
            }
        });
        if (ns.size() <= rang) {
            System.out.println("(aucun nœud : " + classe + " #" + rang + ")");
        } else {
            ns.get(rang).getOnMouseClicked().handle(null);
        }
    }

    /** Clique le premier bouton dont le texte commence par {@code debut} (s'il existe). */
    void cliquerDebut(String debut) {
        List<ButtonBase> bs = new ArrayList<>();
        collect(scene.getRoot(), ButtonBase.class, b -> {
            String t = ((Labeled) b).getText();
            if (t != null && t.startsWith(debut) && !b.isDisabled()) {
                bs.add(b);
            }
        });
        if (bs.isEmpty()) {
            System.out.println("(aucun bouton : " + debut + "…)");
        } else {
            bs.get(0).fire();
        }
    }

    void cliquerSiPresent(String texte) {
        try {
            cliquer(texte);
        } catch (IllegalStateException absent) {
            System.out.println("(bouton absent : " + texte + ")");
        }
    }

    /** Onglet Tournois (carte, filtres, fiche, inscription, Mes tournois) puis vue Inscriptions du club. */
    void etapesOnglet(Coquille[] coq) {
        etape(() -> {
            coq[0] = new Coquille(ctx, () -> { });
            scene.setRoot(coq[0].build());
            coq[0].afficher("tournois");
        }, "o01-carte-liste", 2500);
        etape(() -> cliquer("Seulement ceux auxquels je peux participer"), "o02-seulement-accessibles", 1200);
        etape(() -> {
            cliquer("Seulement ceux auxquels je peux participer");
            saisir("Nom, ville, club, département…", "loiret");
        }, "o03-recherche-loiret", 1200);
        etape(() -> {
            saisir("Nom, ville, club, département…", "");
            PageTournois.derniere.ouvrirFicheParNom("Tournoi National B de Brunoy");
        }, "o04-fiche-brunoy");
        etape(() -> defiler(0.45), "o04b-fiche-brunoy-tableaux");
        etape(() -> defiler(1.0), "o04c-fiche-brunoy-participants");
        etape(() -> PageTournois.derniere.ouvrirFicheParNom("Tournoi départemental du Loiret"), "o05-fiche-loiret-grise");
        etape(() -> PageTournois.derniere.ouvrirFicheParNom("Tournoi régional de Créteil"), "o06-fiche-creteil");
        etape(() -> { defiler(1.0); cliquer("S'inscrire"); }, "o07-assistant");
        etape(() -> { cases().get(1).fire(); cases().get(2).fire(); }, "o08-validation-consecutifs");
        etape(() -> defiler(1.0), "o08b-validation-bas");
        etape(() -> { cases().get(2).fire(); cases().get(3).fire(); defiler(0); }, "o09-selection-valide");
        etape(() -> cliquer("Continuer"), "o10-recapitulatif");
        etape(() -> {
            List<javafx.scene.control.CheckBox> c = cases();
            c.get(c.size() - 1).fire();
            cliquer("Confirmer l'inscription");
        }, "o11-paiement-ulterieur");
        etape(() -> cliquer("Continuer"), "o12-confirmation");
        etape(() -> cliquer("Voir la fiche et les participants"), "o13-fiche-apres");
        etape(() -> defiler(1.0), "o13b-participants-apres");
        etape(() -> {
            coq[0].afficher("tournois");
            cliquer("Mes tournois");
        }, "o14-mes-tournois", 800);
        etape(() -> defiler(1.0), "o14b-mes-tournois-club");
        etape(() -> {
            javafx.scene.layout.BorderPane bp = new javafx.scene.layout.BorderPane(
                    new EcranCompletion(ctx, ctx.fichierTournoiDemo(), () -> { }).build());
            scene.setRoot(bp);
        }, "o15-vue-ensemble-carte-inscriptions");
        etape(() -> cliquer("Voir les inscriptions"), "o16-inscriptions-par-tableau");
        etape(() -> {
            List<Node> lignes = new ArrayList<>();
            collect(scene.getRoot(), Node.class, n -> { if (n.getStyleClass().contains("choix-tableau")) lignes.add(n); });
            javafx.event.Event.fireEvent(lignes.get(1), new javafx.scene.input.MouseEvent(
                    javafx.scene.input.MouseEvent.MOUSE_CLICKED, 0, 0, 0, 0, javafx.scene.input.MouseButton.PRIMARY, 1,
                    false, false, false, false, true, false, false, true, false, false, null));
        }, "o17-inscriptions-tableau-b");
        etape(() -> cliquer("Par joueur"), "o18-inscriptions-par-joueur");
        etape(() -> {
            List<javafx.scene.control.ComboBox> cb = new ArrayList<>();
            collect(scene.getRoot(), javafx.scene.control.ComboBox.class, cb::add);
            cb.get(0).setValue("Tri par club");
        }, "o19-par-joueur-club");
    }

    /** B027 : fiche du tournoi du jour J en mode suivi (lecture seule), navigation entre tableaux. */
    void etapesSuivi(Coquille[] coq) {
        etape(() -> {
            coq[0] = new Coquille(ctx, () -> { });
            scene.setRoot(coq[0].build());
            coq[0].afficher("tournois");
            PageTournois.derniere.ouvrirFicheParNom("Tournoi de démonstration du jour J");
        }, "s01-suivi-tableau-f", 1200);
        etape(() -> defiler(0.5), "s02-suivi-f-arbre");
        etape(() -> defiler(1.0), "s03-suivi-f-poules");
        etape(() -> {
            List<javafx.scene.control.Button> onglets = new ArrayList<>();
            collect(scene.getRoot(), javafx.scene.control.Button.class, b -> {
                if (b.getStyleClass().contains("suivi-onglet")) onglets.add(b);
            });
            onglets.get(2).fire();
        }, "s04-suivi-tableau-c");
        etape(() -> defiler(1.0), "s05-suivi-c-poules");
        etape(() -> cliquer("Informations du tournoi"), "s06-fiche-informations");
        etape(() -> { coq[0].afficher("tournois"); cliquer("Mes tournois"); }, "s07-mes-tournois", 800);
    }

    /** Liste d'attente (Créteil D) puis import d'un fichier dans un tournoi préparé hors application. */
    void etapesImport(Coquille[] coq) {
        etape(() -> {
            coq[0] = new Coquille(ctx, () -> { });
            scene.setRoot(coq[0].build());
            coq[0].afficher("tournois");
            PageTournois.derniere.ouvrirFicheParNom("Tournoi régional de Créteil");
        }, null, 800);
        etape(() -> { defiler(1.0); cliquer("S'inscrire"); }, null);
        // B (10h30) inscrit + D (13h30, complet) en liste d'attente.
        etape(() -> { cases().get(1).fire(); cases().get(3).fire(); defiler(0.4); }, "i01-attente-validation");
        etape(() -> cliquer("Continuer"), "i02-attente-recapitulatif");
        etape(() -> {
            List<javafx.scene.control.CheckBox> c = cases();
            c.get(c.size() - 1).fire();
            cliquer("Confirmer l'inscription");
        }, "i03-attente-paiement");
        etape(() -> cliquer("Continuer"), "i04-attente-confirmation");
        etape(() -> { coq[0].afficher("tournois"); cliquer("Mes tournois"); }, "i05-attente-mes-tournois", 800);
        // Import : fichier de 40 joueurs (3 tableaux, quelques lignes fautives) dans « Open d'automne ».
        etape(() -> {
            Path fichierTournoi = ctx.tousLesTournois().stream()
                    .filter(tf -> tf.tournoi().getNom().equals("Open d'automne")).findFirst().orElseThrow().fichier();
            EcranImportJoueurs e = new EcranImportJoueurs(ctx, fichierTournoi, () -> { }, () -> {
                scene.setRoot(new javafx.scene.layout.BorderPane(new VueInscriptionsClub(ctx, fichierTournoi, () -> { }).build()));
                installerTheme();
            });
            scene.setRoot(new javafx.scene.layout.BorderPane(e.build()));
            installerTheme();
            ecranImport = e;
        }, "i06-import-vide");
        etape(() -> ecranImport.charger(fichierImport()), "i07-import-apercu");
        etape(() -> defiler(1.0), "i08-import-apercu-bas");
        etape(() -> {
            List<TextField> h = new ArrayList<>();
            collect(scene.getRoot(), TextField.class, f -> { if ("heure, ex. 9h".equals(f.getPromptText())) h.add(f); });
            String[] heures = {"9h", "11h", "14h"};
            for (int i = 0; i < h.size(); i++) h.get(i).setText(heures[i % 3]);
            List<Node> bs = new ArrayList<>();
            collect(scene.getRoot(), ButtonBase.class, b -> { if (((Labeled) b).getText().startsWith("Importer ")) bs.add(b); });
            ((ButtonBase) bs.get(bs.size() - 1)).fire();
        }, "i09-import-termine");
        etape(() -> cliquer("Voir les inscriptions"), "i10-import-inscriptions");
    }

    EcranImportJoueurs ecranImport;

    /** Choix imposé après création, tournoi importé (classeur 2 feuilles), puis carte sur tuiles locales. */
    void etapesChoix(Coquille[] coq) {
        Path[] f = new Path[1];
        etape(() -> {
            Tournoi t = new Tournoi("Tournoi de la Toussaint de Brunoy", "Brunoy", List.of(LocalDate.of(2026, 11, 28)));
            t.setNumeroAffiliationClub("08911132");
            t.setNiveau(com.pingmanager.model.NiveauTournoi.DEPARTEMENTAL);
            t.setPeriode(LocalDate.of(2026, 11, 28), LocalDate.of(2026, 11, 28));
            t.setAdresse(com.pingmanager.model.AdresseTournoi.de("1 rue du Gymnase", "91800", "Brunoy"));
            t.setModePreparation(com.pingmanager.model.ModePreparation.A_CHOISIR);
            f[0] = ctx.creerBrouillon(t);
            scene.setRoot(new javafx.scene.layout.BorderPane(new EcranChoixPreparation(ctx, f[0], () -> {
                scene.setRoot(new javafx.scene.layout.BorderPane(new EcranTournoiImporte(ctx, f[0], () -> { }, () -> { }).build()));
                installerTheme();
            }, () -> { }).build()));
            installerTheme();
        }, "c01-choix-methode");
        etape(() -> cliquer("Choisir l'import"), "c02-tournoi-importe-vide");
        etape(() -> cliquer("Importer le classeur…"), null);
        etape(() -> EcranImportJoueurs.dernier.charger(classeurImport()), "c03-import-classeur");
        etape(() -> defiler(1.0), "c04-import-classeur-bas");
        etape(() -> {
            List<Node> bs = new ArrayList<>();
            collect(scene.getRoot(), ButtonBase.class, b -> { if (((Labeled) b).getText().startsWith("Importer ")) bs.add(b); });
            ((ButtonBase) bs.get(bs.size() - 1)).fire();
        }, null);
        etape(() -> cliquer("← Retour"), "c05-tournoi-importe-rempli");
        etape(() -> {
            coq[0] = new Coquille(ctx, () -> { });
            scene.setRoot(coq[0].build());
            coq[0].afficher("tournois");
        }, null, 2500);
        etape(() -> PageTournois.derniere.carte().script(
                "FONDS.ign.url='http://127.0.0.1:8765/{z}/{x}/{y}.png'; fond=null; poserFond('ign');"), "c06-carte-tuiles-locales", 3000);
    }

    /** Classeur JSON à deux feuilles (tableaux + joueurs), comme le modèle Excel. */
    Path classeurImport() {
        try {
            StringBuilder sb = new StringBuilder("{\"tableaux\": [");
            sb.append("{\"Nom du tableau\": \"A\", \"Points max\": 899, \"Public\": \"Mixte\", \"Heure\": \"9h\", \"Places\": 24, \"Liste d'attente\": 4},");
            sb.append("{\"Nom du tableau\": \"B\", \"Points min\": 900, \"Points max\": 1399, \"Heure\": \"11h\", \"Places\": 24},");
            sb.append("{\"Nom du tableau\": \"Dames\", \"Public\": \"Dames\", \"Heure\": \"14h\"},");
            sb.append("{\"Nom du tableau\": \"Open\", \"Heure\": \"16h\", \"Places\": 32}");
            sb.append("], \"joueurs\": [");
            List<com.pingmanager.model.FicheLicence> fs = DonneesDemo.JOUEURS_FICTIFS;
            for (int i = 0; i < 70; i++) {
                com.pingmanager.model.FicheLicence x = fs.get((i * 5) % fs.size());
                String t1 = x.getPoints() <= 899 ? "A" : x.getPoints() <= 1399 ? "B" : "Open";
                String t2 = x.getSexe() == com.pingmanager.model.Sexe.FEMME ? "Dames" : (i % 3 == 0 ? "Open" : "");
                sb.append(i > 0 ? "," : "").append("{\"Nom\": \"").append(x.getNom()).append("\", \"Prénom\": \"")
                        .append(x.getPrenom()).append("\", \"Points\": ").append(x.getPoints())
                        .append(", \"Club\": \"").append(x.getNomClub()).append("\", \"Sexe\": \"")
                        .append(x.getSexe() == com.pingmanager.model.Sexe.FEMME ? "F" : "H")
                        .append("\", \"Catégorie\": \"Senior\", \"Tableau 1\": \"").append(t1)
                        .append("\", \"Tableau 2\": \"").append(t2).append("\"}");
            }
            sb.append("]}");
            Path p = Files.createTempFile("classeur", ".json");
            Files.writeString(p, sb.toString());
            return p;
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
    }

    /** Fichier CSV d'exemple écrit dans un dossier temporaire. */
    Path fichierImport() {
        try {
            StringBuilder sb = new StringBuilder("Open d'automne — engagés;;;;;\nNom;Prénom;Points;Club;Sexe;Tableau\n");
            List<com.pingmanager.model.FicheLicence> f = DonneesDemo.JOUEURS_FICTIFS;
            for (int i = 0; i < 40; i++) {
                com.pingmanager.model.FicheLicence x = f.get(i * 9);
                String tab = x.getPoints() <= 1000 ? "≤ 1000" : "Open";
                if (x.getSexe() == com.pingmanager.model.Sexe.FEMME) tab += " + Dames";
                sb.append(x.getNom()).append(';').append(x.getPrenom()).append(';').append(x.getPoints()).append(';')
                        .append(x.getNomClub()).append(';').append(x.getSexe() == com.pingmanager.model.Sexe.FEMME ? "F" : "H")
                        .append(';').append(tab).append('\n');
            }
            sb.append("SANSCLUB;Paul;900;;H;Open\nPOINTS;Inès;mille;Club X;F;Dames\n");
            Path p = Files.createTempFile("engages", ".csv");
            Files.writeString(p, sb.toString());
            return p;
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
    }

    List<javafx.scene.control.CheckBox> cases() {
        List<javafx.scene.control.CheckBox> l = new ArrayList<>();
        collect(scene.getRoot(), javafx.scene.control.CheckBox.class, l::add);
        return l;
    }

    void installerTheme() {
        try {
            Class<?> th = Class.forName("com.pingmanager.ui.ecran.Theme");
            th.getMethod("installer", Scene.class).invoke(null, scene);
            if (sombre) {
                th.getMethod("basculer", Scene.class).invoke(null, scene);
            }
        } catch (ClassNotFoundException e) {
            // pas encore de thème (capture « avant »)
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    final List<String> noms = new ArrayList<>();

    final List<Integer> delais = new ArrayList<>();

    void etape(Runnable action, String nom) {
        etape(action, nom, 400);
    }

    void etape(Runnable action, String nom, int delaiMs) {
        etapes.add(action);
        noms.add(nom);
        delais.add(delaiMs);
    }

    void lancer(int i) {
        if (i >= etapes.size()) {
            Platform.exit();
            return;
        }
        PauseTransition p = new PauseTransition(Duration.millis(150));
        p.setOnFinished(e -> {
            try {
                etapes.get(i).run();
            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.exit();
                return;
            }
            if (noms.get(i) == null) {
                lancer(i + 1);
                return;
            }
            PauseTransition q = new PauseTransition(Duration.millis(delais.get(i)));
            q.setOnFinished(f -> {
                snap(noms.get(i));
                lancer(i + 1);
            });
            q.play();
        });
        p.play();
    }

    void snap(String nom) {
        WritableImage img = scene.snapshot(null);
        int w = (int) img.getWidth(), h = (int) img.getHeight();
        BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        PixelReader pr = img.getPixelReader();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                bi.setRGB(x, y, pr.getArgb(x, y));
            }
        }
        try {
            ImageIO.write(bi, "png", new File(out, nom + (sombre ? "-sombre" : ((v2 || accueil) ? "-clair" : "")) + ".png"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    void cliquerN(String texte, int n) {
        List<ButtonBase> bs = new ArrayList<>();
        collect(scene.getRoot(), ButtonBase.class, b -> {
            if (texte.equals(((Labeled) b).getText())) {
                bs.add(b);
            }
        });
        bs.get(n).fire();
    }

    void saisir(String invite, String valeur) {
        List<TextField> l = new ArrayList<>();
        collect(scene.getRoot(), TextField.class, f -> { if (invite.equals(f.getPromptText())) l.add(f); });
        if (l.isEmpty()) throw new IllegalStateException("champ introuvable : " + invite);
        l.get(0).setText(valeur);
    }

    void saisirN(String invite, int n, String valeur) {
        List<TextField> l = new ArrayList<>();
        collect(scene.getRoot(), TextField.class, f -> { if (invite.equals(f.getPromptText())) l.add(f); });
        l.get(n).setText(valeur);
    }

    void chercher(int index, String texte) {
        List<TextField> l = new ArrayList<>();
        collect(scene.getRoot(), TextField.class, f -> { if ("ex. Lemoine, Hélène ou 9411021".equals(f.getPromptText())) l.add(f); });
        l.get(index).setText(texte);
    }

    void defiler(double v) {
        List<javafx.scene.control.ScrollPane> sp = new ArrayList<>();
        collect(scene.getRoot(), javafx.scene.control.ScrollPane.class, x -> { if (!(x.getParent() instanceof javafx.scene.control.TextArea) && !x.getStyleClass().contains("virtual-flow")) sp.add(x); });
        sp.get(0).setVvalue(v);
    }

    void basDePage() {
        defiler(1.0);
    }

    void cliquer(String texte) {
        List<ButtonBase> bs = new ArrayList<>();
        collect(scene.getRoot(), ButtonBase.class, b -> {
            if (texte.equals(((Labeled) b).getText())) {
                bs.add(b);
            }
        });
        if (bs.isEmpty()) {
            throw new IllegalStateException("bouton introuvable : " + texte);
        }
        bs.get(0).fire();
    }

    static <T> void collect(Node n, Class<T> type, Consumer<T> c) {
        if (type.isInstance(n)) {
            c.accept(type.cast(n));
        }
        if (n instanceof javafx.scene.control.ScrollPane sp && sp.getContent() != null) {
            collect(sp.getContent(), type, c);
        }
        if (n instanceof Parent p) {
            for (Node k : p.getChildrenUnmodifiable()) {
                collect(k, type, c);
            }
        }
    }
}
