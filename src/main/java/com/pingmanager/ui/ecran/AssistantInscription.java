package com.pingmanager.ui.ecran;

import com.pingmanager.core.MotifsInscription;
import com.pingmanager.core.RemplissageTableaux;
import com.pingmanager.core.ResultatInscriptionTournoi;
import com.pingmanager.core.ResultatInscriptionTournoi.Motif;
import com.pingmanager.core.ServiceInscription;
import com.pingmanager.core.ServiceInscriptionEnLigne;
import com.pingmanager.core.reglement.Formats;
import com.pingmanager.core.reglement.GenerateurReglement;
import com.pingmanager.model.FicheLicence;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.ui.ContexteApplication;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Assistant d'inscription d'un joueur à un tournoi, en quatre étapes :
 * <ol>
 *   <li><b>Tableaux</b> : cases à cocher, validation <b>en direct</b> par
 *       {@link ServiceInscriptionEnLigne#simuler} (quotas par jour, tableau dames
 *       imposé, tableaux consécutifs, points, sexe, catégories, capacité et liste
 *       d'attente) avec le motif en clair sur chaque ligne ;</li>
 *   <li><b>Récapitulatif</b> et montant (tarif en ligne), acceptation du règlement ;</li>
 *   <li><b>Phase de paiement — faite ultérieurement</b> : montant dû, statut « à régler » ;
 *       l'inscription est déjà enregistrée ;</li>
 *   <li><b>Confirmation</b>.</li>
 * </ol>
 */
final class AssistantInscription {

    private static final String[] ETAPES = {"Tableaux", "Récapitulatif", "Paiement", "Confirmation"};

    private final ContexteApplication contexte;
    private final Path fichier;
    private final Tournoi tournoi;
    private final Runnable onFiche;
    private final Runnable onListe;
    private final BorderPane racine = new BorderPane();

    private final Set<Tableau> selection = new LinkedHashSet<>();
    private ServiceInscriptionEnLigne.Bilan bilanFinal;

    AssistantInscription(ContexteApplication contexte, Path fichier, Tournoi tournoi, Runnable onFiche, Runnable onListe) {
        this.contexte = contexte;
        this.fichier = fichier;
        this.tournoi = tournoi;
        this.onFiche = onFiche;
        this.onListe = onListe;
    }

    Node build() {
        etapeTableaux();
        return racine;
    }

    private VBox page(int etape, Node... contenu) {
        Button retour = Ui.boutonLien("← Fiche du tournoi");
        retour.setOnAction(e -> onFiche.run());
        retour.setDisable(etape >= 2);
        Label titre = Ui.titre("Inscription — " + tournoi.getNom());
        titre.setWrapText(true);
        VBox p = Ui.page(860, new VBox(6, retour, titre), etapes(etape));
        p.getChildren().addAll(contenu);
        return p;
    }

    /** Fil d'Ariane des étapes : faites (✓), courante, à venir. */
    private static Node etapes(int courante) {
        HBox h = new HBox(6);
        h.setAlignment(Pos.CENTER_LEFT);
        h.getStyleClass().add("fil-etapes");
        for (int i = 0; i < ETAPES.length; i++) {
            Label l = new Label((i < courante ? "✓ " : (i + 1) + ". ") + ETAPES[i]);
            l.getStyleClass().add("etape");
            l.getStyleClass().add(i == courante ? "etape-courante" : i < courante ? "etape-faite" : "etape-a-venir");
            h.getChildren().add(l);
            if (i < ETAPES.length - 1) {
                Label sep = Ui.discret("›");
                h.getChildren().add(sep);
            }
        }
        return h;
    }

    // ==================================================================
    // Étape 1 : tableaux, validation en direct
    // ==================================================================

    private void etapeTableaux() {
        FicheLicence fiche = contexte.ficheLicenceLiee().orElseThrow();
        Joueur j = ServiceInscriptionEnLigne.joueurPour(tournoi, fiche);
        Node joueur = Ui.carte("Joueur",
                Ui.ligne(16, Ui.gras(fiche.getPrenom() + " " + fiche.getNom()),
                        Ui.discret("Licence " + fiche.getNumeroLicence()), Ui.discret(fiche.getNomClub()),
                        Ui.discret(j.getPoints() + " points"), Ui.discret(MotifsInscription.categorie(j.getCategorie()))));

        List<String> regles;
        try {
            regles = new GenerateurReglement().apercuNombreDeTableaux(tournoi, tournoi.getQuotasTableaux());
        } catch (RuntimeException e) {
            regles = List.of();
        }

        VBox lignes = new VBox();
        Map<Tableau, Label> statuts = new LinkedHashMap<>();
        Map<Tableau, CheckBox> cases = new LinkedHashMap<>();
        Label total = Ui.gras("");
        Label resume = Ui.discret("");
        Button continuer = Ui.boutonPrimaire("Continuer");

        LocalDate jourCourant = null;
        for (Tableau t : RemplissageTableaux.ordrePlanning(tournoi)) {
            LocalDate jour = t.getHoraires() == null ? null : t.getHoraires().getJour();
            if (jour != null && !jour.equals(jourCourant)) {
                Label lj = Ui.gras(Formats.capitale(Formats.jourLong(jour)));
                lj.getStyleClass().add("intertitre-jour");
                lignes.getChildren().add(lj);
                jourCourant = jour;
            }
            CheckBox c = new CheckBox();
            boolean deja = t.getInscrits().contains(j) || t.getFileAttente().contains(j);
            c.setSelected(deja);
            c.setDisable(deja);
            Label nom = Ui.gras(t.getNom());
            Label sous = Ui.discret(BlocsInscriptions.sousTitreTableau(t) + " · "
                    + BlocsInscriptions.placesPourJoueur(RemplissageTableaux.etat(t)));
            VBox ident = new VBox(2, nom, sous);
            HBox.setHgrow(ident, Priority.ALWAYS);
            Label prix = Ui.gras(Formats.montant(RemplissageTableaux.tarifEnLigne(t)));
            prix.setMinWidth(60);
            prix.setAlignment(Pos.CENTER_RIGHT);
            Label statut = new Label();
            statut.setWrapText(true);
            statut.setPrefWidth(270);
            statut.setMinWidth(220);
            statut.getStyleClass().add("statut-tableau");
            HBox l = new HBox(12, c, ident, statut, prix);
            l.setAlignment(Pos.CENTER_LEFT);
            l.getStyleClass().add("ligne-liste");
            l.setOnMouseClicked(ev -> {
                if (!c.isDisabled() && ev.getTarget() != c) {
                    c.setSelected(!c.isSelected());
                }
            });
            lignes.getChildren().add(l);
            statuts.put(t, statut);
            cases.put(t, c);
            c.selectedProperty().addListener((o, a, b) -> {
                if (b) {
                    selection.add(t);
                } else {
                    selection.remove(t);
                }
                valider(statuts, cases, j, total, resume, continuer);
            });
            if (selection.contains(t) && !deja) {
                c.setSelected(true);
            }
        }
        valider(statuts, cases, j, total, resume, continuer);

        continuer.setOnAction(e -> etapeRecapitulatif());
        Button annuler = Ui.boutonSecondaire("Annuler");
        annuler.setOnAction(e -> onFiche.run());
        HBox pied = Ui.ligne(12, new VBox(2, total, resume), Ui.espace(), annuler, continuer);
        pied.getStyleClass().add("pied-assistant");

        VBox carte = Ui.carte("Choisissez vos tableaux", lignes);
        if (!regles.isEmpty()) {
            Label r = Ui.aide("Règles du tournoi : " + String.join(" ", regles));
            r.getStyleClass().add("rappel-regles");
            carte.getChildren().add(1, r);
        }
        racine.setCenter(Ui.defilant(page(0, joueur, carte, pied)));
    }

    /**
     * Validation en direct : la sélection est simulée en bloc (statut de chaque case
     * cochée) ; chaque case non cochée est simulée avec la sélection, pour dire tout de
     * suite si elle serait acceptée et sinon pourquoi.
     */
    private void valider(Map<Tableau, Label> statuts, Map<Tableau, CheckBox> cases, Joueur j,
                         Label total, Label resume, Button continuer) {
        ServiceInscriptionEnLigne.Bilan b = contexte.simulerInscription(tournoi, selection);
        for (Map.Entry<Tableau, Label> e : statuts.entrySet()) {
            Tableau t = e.getKey();
            Label l = e.getValue();
            l.getStyleClass().removeAll("statut-ok", "statut-attente", "statut-refus", "statut-neutre");
            if (cases.get(t).isDisabled()) {
                boolean attente = t.getFileAttente().contains(j);
                l.setText(attente ? "Déjà en liste d'attente (" + (t.getFileAttente().indexOf(j) + 1) + "e)" : "✓ Déjà inscrit");
                l.getStyleClass().add(attente ? "statut-attente" : "statut-ok");
                continue;
            }
            ResultatInscriptionTournoi r;
            if (selection.contains(t)) {
                r = b.parTableau().get(t);
            } else {
                List<Tableau> essai = new ArrayList<>(selection);
                essai.add(t);
                r = contexte.simulerInscription(tournoi, essai).parTableau().get(t);
            }
            if (r == null) {
                l.setText("");
                continue;
            }
            String texte = MotifsInscription.texte(r, tournoi, j, t);
            boolean coche = selection.contains(t);
            if (r.getMotif() == Motif.INSCRIT) {
                l.setText(coche ? "✓ " + texte : texte);
                l.getStyleClass().add(coche ? "statut-ok" : "statut-neutre");
            } else if (r.getMotif() == Motif.EN_FILE_ATTENTE) {
                l.setText((coche ? "⏳ " : "") + texte);
                l.getStyleClass().add("statut-attente");
            } else {
                l.setText((coche ? "✗ " : "") + texte);
                l.getStyleClass().add("statut-refus");
            }
        }
        long refus = b.nombreRefus();
        total.setText(selection.isEmpty() ? "Aucun tableau choisi"
                : selection.size() + (selection.size() > 1 ? " tableaux" : " tableau") + " · "
                + Formats.montant(b.montantInscrit()) + " (tarif en ligne)");
        resume.setText(refus > 0 ? refus + (refus > 1 ? " choix ne respectent pas" : " choix ne respecte pas")
                + " les règles : décochez-le ou changez de tableau."
                : b.montantSiRepeche() > 0 ? "+ " + Formats.montant(b.montantSiRepeche())
                + " seulement si vous êtes repêché sur liste d'attente." : "");
        resume.getStyleClass().remove("motif-refus");
        if (refus > 0) {
            resume.getStyleClass().add("motif-refus");
        }
        continuer.setDisable(!b.estValide());
    }

    // ==================================================================
    // Étape 2 : récapitulatif
    // ==================================================================

    private void etapeRecapitulatif() {
        ServiceInscriptionEnLigne.Bilan b = contexte.simulerInscription(tournoi, selection);
        VBox lignes = new VBox();
        for (Map.Entry<Tableau, ResultatInscriptionTournoi> e : b.parTableau().entrySet()) {
            Tableau t = e.getKey();
            boolean attente = e.getValue().getMotif() == Motif.EN_FILE_ATTENTE;
            Label etat = attente ? Ui.pastille("Liste d'attente", Ui.ETAT_ATTENTION) : Ui.pastille("Place disponible", Ui.ETAT_OK);
            VBox ident = new VBox(2, Ui.gras(t.getNom()), Ui.discret(BlocsInscriptions.sousTitreTableau(t)));
            HBox.setHgrow(ident, Priority.ALWAYS);
            Label prix = Ui.gras(attente ? "(" + Formats.montant(RemplissageTableaux.tarifEnLigne(t)) + ")"
                    : Formats.montant(RemplissageTableaux.tarifEnLigne(t)));
            prix.setMinWidth(70);
            prix.setAlignment(Pos.CENTER_RIGHT);
            HBox l = Ui.ligneListe(ident, etat, prix);
            lignes.getChildren().add(l);
        }
        HBox totalLigne = Ui.ligne(12, Ui.gras("Montant à régler"), Ui.espace(),
                montant(b.montantInscrit()));
        totalLigne.getStyleClass().add("ligne-total");
        VBox recap = Ui.carte("Récapitulatif", lignes, totalLigne);
        if (b.montantSiRepeche() > 0) {
            recap.getChildren().add(Ui.aide("Les tableaux en liste d'attente (montant entre parenthèses) ne sont dus "
                    + "que si vous êtes repêché ; vous les réglerez alors au pointage."));
        }

        CheckBox accepte = new CheckBox("J'ai lu le règlement du tournoi et je l'accepte.");
        Label message = Ui.message();
        Button confirmer = Ui.boutonPrimaire("Confirmer l'inscription");
        confirmer.setDisable(true);
        accepte.selectedProperty().addListener((o, a, v) -> confirmer.setDisable(!v));
        confirmer.setOnAction(e -> {
            ServiceInscriptionEnLigne.Bilan res = contexte.inscrireEnLigne(fichier, tournoi, selection);
            if (!res.estValide()) {
                Ui.erreur(message, res.refusGlobal() != null ? res.refusGlobal()
                        : "Les places ont changé entre-temps : revenez au choix des tableaux.");
                return;
            }
            bilanFinal = res;
            etapePaiement();
        });
        Button modifier = Ui.boutonSecondaire("← Modifier mes tableaux");
        modifier.setOnAction(e -> etapeTableaux());
        HBox pied = Ui.ligne(12, accepte, Ui.espace(), modifier, confirmer);
        pied.getStyleClass().add("pied-assistant");
        racine.setCenter(Ui.defilant(page(1, recap, pied, message)));
    }

    private static Label montant(double v) {
        Label l = new Label(Formats.montant(v));
        l.getStyleClass().add("montant");
        return l;
    }

    // ==================================================================
    // Étape 3 : paiement (fait ultérieurement)
    // ==================================================================

    private void etapePaiement() {
        double du = bilanFinal.montantInscrit();
        Joueur j = bilanFinal.joueur();
        Label titre = new Label("Phase de paiement — faite ultérieurement");
        titre.getStyleClass().add("carte-titre");
        Label statut = du > 0 ? Ui.pastille("À régler", Ui.ETAT_ATTENTION)
                : Ui.pastille("Rien à régler pour l'instant", Ui.ETAT_RECOMMANDE);

        // Détail : tableaux obtenus (dus) et tableaux en liste d'attente (non dus).
        VBox detail = new VBox();
        List<Tableau> enAttente = new ArrayList<>();
        for (Map.Entry<Tableau, ResultatInscriptionTournoi> e : bilanFinal.parTableau().entrySet()) {
            Tableau t = e.getKey();
            double prix = RemplissageTableaux.tarifEnLigne(t);
            if (e.getValue().getMotif() == Motif.EN_FILE_ATTENTE) {
                enAttente.add(t);
                int pos = t.getFileAttente().indexOf(j) + 1;
                Label etat = Ui.pastille("Liste d'attente · " + pos + "e", Ui.ETAT_ATTENTION);
                Label montant = Ui.discret("0 € (" + Formats.montant(prix) + " si repêché)");
                montant.setMinWidth(Region.USE_PREF_SIZE);
                detail.getChildren().add(Ui.ligneListe(Ui.gras(t.getNom()), Ui.espace(), etat, montant));
            } else {
                Label etat = Ui.pastille("Place obtenue", Ui.ETAT_OK);
                Label montant = Ui.gras(Formats.montant(prix));
                montant.setMinWidth(Region.USE_PREF_SIZE);
                detail.getChildren().add(Ui.ligneListe(Ui.gras(t.getNom()), Ui.espace(), etat, montant));
            }
        }
        HBox totalLigne = Ui.ligne(12, Ui.gras("Montant dû maintenant"), Ui.espace(), montant(du));
        totalLigne.getStyleClass().add("ligne-total");

        String lien = tournoi.getPaiement() == null || tournoi.getPaiement().getLien() == null ? null
                : tournoi.getPaiement().getLien();
        Button payer = Ui.boutonPrimaire("Payer en ligne (bientôt disponible)");
        payer.setDisable(true);
        String intro = du > 0
                ? "Votre inscription est enregistrée. Le paiement en ligne n'est pas encore ouvert dans "
                + "PingManager : le montant des tableaux obtenus est noté « à régler » et rien n'est débité aujourd'hui."
                : "Votre inscription est enregistrée en liste d'attente : vous n'avez rien à payer tant "
                + "qu'aucune place ne vous est attribuée.";
        VBox c = Ui.carte(Ui.enteteAvecActions(titre, statut), Ui.texte(intro), detail, totalLigne);
        if (!enAttente.isEmpty()) {
            Label regle = Ui.aide("Liste d'attente : aucune place n'est garantie, donc rien n'est dû. Si un inscrit se "
                    + "désiste avant le tournoi, le premier en attente prend sa place ; le tableau apparaît alors "
                    + "« à régler » dans « Mes tournois ». Le jour J, les repêchés règlent au pointage.");
            regle.getStyleClass().add("rappel-regles");
            c.getChildren().add(regle);
        }
        if (du > 0) {
            c.getChildren().addAll(Ui.aide(lien == null ? "Le club indiquera le moyen de paiement."
                    : "Plateforme prévue par le club : " + Ui.libelle(tournoi.getPaiement().getMode())
                    + " (" + lien + ")."), payer);
        }
        c.getStyleClass().add(du > 0 ? "carte-paiement" : "carte-moi");
        Button suivant = Ui.boutonPrimaire("Continuer");
        suivant.setOnAction(e -> etapeConfirmation());
        HBox pied = Ui.ligne(12, Ui.espace(), suivant);
        pied.getStyleClass().add("pied-assistant");
        racine.setCenter(Ui.defilant(page(2, c, pied)));
    }

    // ==================================================================
    // Étape 4 : confirmation
    // ==================================================================

    private void etapeConfirmation() {
        VBox lignes = new VBox();
        Joueur j = bilanFinal.joueur();
        for (Map.Entry<Tableau, ResultatInscriptionTournoi> e : bilanFinal.parTableau().entrySet()) {
            Tableau t = e.getKey();
            boolean attente = e.getValue().getMotif() == Motif.EN_FILE_ATTENTE;
            String etat = attente ? "Liste d'attente, " + (t.getFileAttente().indexOf(j) + 1) + "e position"
                    : "Inscrit · rang " + (RemplissageTableaux.etat(t).inscrits().indexOf(j) + 1)
                    + " sur " + t.getInscrits().size() + " (par points)";
            lignes.getChildren().add(Ui.ligneListe(Ui.gras(t.getNom()), Ui.espace(), Ui.discret(etat)));
        }
        Label ok = new Label("✓ Inscription enregistrée");
        ok.getStyleClass().add("titre-succes");
        Button fiche = Ui.boutonPrimaire("Voir la fiche et les participants");
        fiche.setOnAction(e -> onFiche.run());
        Button tournois = Ui.boutonSecondaire("Retour aux tournois");
        tournois.setOnAction(e -> onListe.run());
        VBox c = Ui.carte(ok, Ui.texte("Vous apparaissez dès maintenant dans la liste des participants de chaque tableau. "
                        + "Montant à régler : " + Formats.montant(bilanFinal.montantInscrit())
                        + (bilanFinal.montantSiRepeche() > 0 ? " (les tableaux en liste d'attente ne sont pas dus)" : "")
                        + ". Retrouvez cette inscription dans « Mes tournois »."),
                lignes, Ui.ligne(12, fiche, tournois));
        racine.setCenter(Ui.defilant(page(3, c)));
    }

    /** Pour le harnais de captures : coche les tableaux dont le nom commence ainsi. */
    void cocher(String... debuts) {
        for (String d : debuts) {
            tournoi.getTableaux().stream().filter(t -> t.getNom().startsWith(d)).forEach(selection::add);
        }
        etapeTableaux();
    }
}
