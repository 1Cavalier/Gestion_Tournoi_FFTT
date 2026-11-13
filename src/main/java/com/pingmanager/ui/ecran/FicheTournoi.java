package com.pingmanager.ui.ecran;

import com.pingmanager.core.CalendrierTournoi;
import com.pingmanager.core.EligibiliteTournoi;
import com.pingmanager.core.MotifsInscription;
import com.pingmanager.core.RemplissageTableaux;
import com.pingmanager.core.ResultatInscriptionTournoi;
import com.pingmanager.core.ServiceInscriptionEnLigne;
import com.pingmanager.core.reglement.Formats;
import com.pingmanager.core.reglement.GenerateurReglement;
import com.pingmanager.core.reglement.LibellesTableau;
import com.pingmanager.model.AffectationJugeArbitre;
import com.pingmanager.model.FicheLicence;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.TarifInscription;
import com.pingmanager.model.Tournoi;
import com.pingmanager.ui.ContexteApplication;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Fiche d'un tournoi (onglet Tournois) : dates, lieu, niveau, juges-arbitres,
 * dotation ; tableaux avec horaires, points, conditions, places restantes et tarifs ;
 * règlement et feuille d'engagement à télécharger ; <b>liste des participants</b> par
 * tableau, triée par points et relue à chaque ouverture (donc à jour après chaque
 * inscription) ; en bas, le bouton « S'inscrire ».
 */
final class FicheTournoi {

    private final ContexteApplication contexte;
    private final Path fichier;
    private final Runnable onRetour;
    private final Consumer<String> rouvrir;
    private final BorderPane racine = new BorderPane();
    private Tournoi tournoi;

    FicheTournoi(ContexteApplication contexte, Path fichier, Runnable onRetour, Consumer<String> rouvrir) {
        this.contexte = contexte;
        this.fichier = fichier;
        this.onRetour = onRetour;
        this.rouvrir = rouvrir;
    }

    Node build() {
        tournoi = contexte.chargerTournoi(fichier);
        // B027 : dès le lancement du premier tableau, la fiche bascule en mode suivi.
        if (PresentationSuiviTournoi.afficherSuivi(tournoi)) {
            afficherSuivi();
        } else {
            afficherFiche();
        }
        return racine;
    }

    /** Mode suivi (lecture seule) ; « Informations du tournoi » revient à la fiche complète. */
    private void afficherSuivi() {
        racine.setCenter(new EcranSuiviTournoi(contexte, fichier, onRetour, () -> {
            tournoi = contexte.chargerTournoi(fichier);
            afficherFiche();
        }).build(tournoi));
    }

    private void afficherFiche() {
        Button retour = Ui.boutonLien("← Retour aux tournois");
        retour.setOnAction(e -> onRetour.run());
        Label titre = Ui.titre(tournoi.getNom());
        titre.setWrapText(true);
        HBox badges = Ui.ligne(8);
        if (tournoi.getNiveau() != null) {
            badges.getChildren().add(Ui.badge(tournoi.getNiveau().getLibelle()));
        }
        LocalDateTime maintenant = LocalDateTime.now();
        if (CalendrierTournoi.estEnCours(tournoi, LocalDate.now())) {
            Label b = Ui.badge("En cours");
            b.getStyleClass().add("badge-en-cours");
            badges.getChildren().add(b);
        }
        CalendrierTournoi.EtatInscriptions etat = CalendrierTournoi.etatInscriptions(tournoi, maintenant);
        badges.getChildren().add(Ui.pastille(CalendrierTournoi.texteInscriptions(tournoi, maintenant),
                etat == CalendrierTournoi.EtatInscriptions.OUVERTES ? Ui.ETAT_OK : Ui.ETAT_OPTIONNEL));
        if (PresentationSuiviTournoi.afficherSuivi(tournoi)) {
            Button suivre = Ui.boutonSecondaire("Suivre le tournoi en direct");
            suivre.setOnAction(e -> {
                tournoi = contexte.chargerTournoi(fichier);
                afficherSuivi();
            });
            badges.getChildren().add(suivre);
        }
        Label club = Ui.discret("Organisé par " + tournoi.getClubOrganisateur()
                + (tournoi.getNumeroAffiliationClub() == null ? "" : " (n° " + tournoi.getNumeroAffiliationClub() + ")"));

        VBox page = Ui.page(900, new VBox(6, retour, titre, badges, club));
        Node acces = bandeauAcces();
        if (acces != null) {
            page.getChildren().add(acces);
        }
        Node moi = carteMonInscription();
        if (moi != null) {
            page.getChildren().add(moi);
        }
        page.getChildren().addAll(carteInformations(), carteTableaux(), carteDocuments(), carteParticipants(),
                barreInscription());
        racine.setCenter(Ui.defilant(page));
    }

    // ------------------------------------------------------------------

    /** Bandeau rouge si le joueur ne peut pas participer (motif IX.101 / IX.102). */
    private Node bandeauAcces() {
        EligibiliteTournoi.Acces a = contexte.eligibilite().evaluer(tournoi, contexte.ficheLicenceLiee().orElse(null));
        if (a.estRefuse()) {
            Label l = new Label("Vous ne pouvez pas participer à ce tournoi. " + a.motif());
            l.setWrapText(true);
            l.getStyleClass().add("bandeau-refus");
            l.setMaxWidth(Double.MAX_VALUE);
            return l;
        }
        if (a.remarque() != null || a.statut() == EligibiliteTournoi.Statut.A_VERIFIER) {
            Label l = new Label(a.remarque() != null ? a.remarque() : a.motif());
            l.setWrapText(true);
            l.getStyleClass().add("rappel-attention");
            l.setMaxWidth(Double.MAX_VALUE);
            return l;
        }
        return null;
    }

    /** Si le joueur est déjà inscrit : ses tableaux et le montant à régler. */
    private Node carteMonInscription() {
        Optional<FicheLicence> f = contexte.ficheLicenceLiee();
        if (f.isEmpty()) {
            return null;
        }
        Optional<RemplissageTableaux.Participant> p = RemplissageTableaux.participant(tournoi, f.get().getNumeroLicence());
        if (p.isEmpty()) {
            return null;
        }
        HBox puces = Ui.ligne(6);
        p.get().inscrit().forEach(t -> puces.getChildren().add(BlocsInscriptions.puceTableau(t, null)));
        p.get().enAttente().forEach(t -> puces.getChildren().add(
                BlocsInscriptions.puceTableau(t, t.getFileAttente().indexOf(p.get().joueur()) + 1)));
        double du = RemplissageTableaux.montantARegler(tournoi, p.get().joueur());
        Label paiement = BlocsInscriptions.pastillePaiement(tournoi, p.get());
        String texte = du > 0 ? "Paiement : phase faite ultérieurement (paiement en ligne bientôt disponible)."
                : "Aucun montant en attente.";
        if (!p.get().enAttente().isEmpty()) {
            texte += " Liste d'attente : rien à payer tant qu'aucune place ne se libère ; si vous êtes repêché "
                    + "avant le tournoi, le tableau passe « à régler » ; le jour J, il se règle au pointage.";
        }
        VBox c = Ui.carte(Ui.enteteAvecActions(Ui.titreCarte("Votre inscription"), paiement), puces,
                Ui.discret(texte));
        c.getStyleClass().add("carte-moi");
        return c;
    }

    private Node carteInformations() {
        GridPane g = new GridPane();
        g.setHgap(18);
        g.setVgap(10);
        ColumnConstraints c0 = new ColumnConstraints();
        c0.setMinWidth(150);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setHgrow(Priority.ALWAYS);
        g.getColumnConstraints().addAll(c0, c1);
        int r = 0;
        r = info(g, r, "Dates", BlocsInscriptions.periode(tournoi));
        r = info(g, r, "Lieu", tournoi.getAdresse() == null ? "À préciser" : tournoi.getAdresse().toString()
                + contexte.eligibilite().departementDuTournoi(tournoi).map(d -> " — " + d.nom() + ", ligue " + d.ligue()).orElse(""));
        r = info(g, r, "Niveau", tournoi.getNiveau() == null ? "—" : tournoi.getNiveau().getLibelle()
                + " (" + publicNiveau() + ")");
        r = info(g, r, "Homologation", tournoi.getNumeroHomologation() == null ? "En cours" : tournoi.getNumeroHomologation());
        r = info(g, r, "Juges-arbitres", jugesArbitres());
        r = info(g, r, "Dotation", tournoi.getDotationGlobale() > 0 ? Formats.montant(tournoi.getDotationGlobale())
                + " au total" : "Lots et récompenses (voir les tableaux)");
        if (tournoi.getPhasePoints() != null) {
            r = info(g, r, "Points retenus", Formats.capitale(Ui.libelle(tournoi.getPhasePoints())));
        }
        if (tournoi.getInfosReglement() != null && tournoi.getInfosReglement().getClotureInscriptions() != null) {
            r = info(g, r, "Inscriptions", (tournoi.getInfosReglement().getOuvertureInscriptions() == null ? ""
                    : "du " + Formats.dateHeure(tournoi.getInfosReglement().getOuvertureInscriptions()) + " ")
                    + "jusqu'au " + Formats.dateHeure(tournoi.getInfosReglement().getClotureInscriptions()));
        }
        if (!tournoi.getCategoriesExclues().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            tournoi.getCategoriesExclues().forEach(c -> sb.append(sb.length() > 0 ? ", " : "").append(c.getLibelle()));
            r = info(g, r, "Non admis", sb.toString());
        }
        if (tournoi.getCorrespondantEmail() != null) {
            info(g, r, "Contact", (tournoi.getCorrespondantPrenom() == null ? "" : tournoi.getCorrespondantPrenom() + " ")
                    + (tournoi.getCorrespondantNom() == null ? "" : tournoi.getCorrespondantNom() + " — ")
                    + tournoi.getCorrespondantEmail());
        }
        return Ui.carte("Informations", g);
    }

    private String publicNiveau() {
        if (tournoi.getNiveau() == null) {
            return "";
        }
        String n = tournoi.getNiveau().name();
        if ("DEPARTEMENTAL".equals(n)) {
            return contexte.eligibilite().departementDuTournoi(tournoi)
                    .map(d -> "licenciés " + d.formeDe()).orElse("licenciés du département");
        }
        if ("REGIONAL".equals(n)) {
            return contexte.eligibilite().departementDuTournoi(tournoi)
                    .map(d -> "licenciés de la ligue " + d.ligue()).orElse("licenciés de la région");
        }
        if ("PROMOTIONNEL".equals(n)) {
            return "ouvert aux licences loisir, résultats non classants";
        }
        return "ouvert à tous les licenciés FFTT";
    }

    private String jugesArbitres() {
        if (tournoi.getJugesArbitres().isEmpty()) {
            return "À désigner";
        }
        StringBuilder sb = new StringBuilder();
        for (AffectationJugeArbitre a : tournoi.getJugesArbitres()) {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append(a.getPrenom() == null ? "" : a.getPrenom() + " ").append(a.getNom())
                    .append(" — ").append(Ui.libelle(a.getGrade()))
                    .append(a.getRole() == null ? "" : ", " + Ui.libelle(a.getRole()).toLowerCase());
        }
        return sb.toString();
    }

    private static int info(GridPane g, int ligne, String libelle, String valeur) {
        Label l = Ui.discret(libelle);
        Label v = Ui.texte(valeur == null ? "—" : valeur);
        g.add(l, 0, ligne);
        g.add(v, 1, ligne);
        GridPane.setValignment(l, javafx.geometry.VPos.TOP);
        return ligne + 1;
    }

    // ------------------------------------------------------------------
    // Tableaux
    // ------------------------------------------------------------------

    private Node carteTableaux() {
        VBox lignes = new VBox();
        Optional<FicheLicence> fiche = contexte.ficheLicenceLiee();
        LocalDate jourCourant = null;
        for (RemplissageTableaux.EtatTableau e : RemplissageTableaux.parTableau(tournoi)) {
            Tableau t = e.tableau();
            LocalDate jour = t.getHoraires() == null ? null : t.getHoraires().getJour();
            if (jour != null && !jour.equals(jourCourant) && tournoi.nombreDeJours() > 1) {
                Label j = Ui.gras(Formats.capitale(Formats.jourLong(jour)));
                j.getStyleClass().add("intertitre-jour");
                lignes.getChildren().add(j);
                jourCourant = jour;
            }
            Label nom = Ui.gras(t.getNom());
            Label sous = Ui.discret(BlocsInscriptions.sousTitreTableau(t)
                    + (t.getConfiguration().getCategoriesAutorisees().isEmpty() ? ""
                    : " · " + LibellesTableau.categories(t.getConfiguration())));
            VBox ident = new VBox(2, nom, sous);
            ident.setPrefWidth(330);
            ident.setMinWidth(260);

            VBox jauge = BlocsInscriptions.jauge(e);
            ((Label) jauge.getChildren().get(1)).setText(BlocsInscriptions.placesPourJoueur(e));
            jauge.setPrefWidth(210);
            jauge.setMinWidth(180);

            VBox prix = new VBox(2, Ui.gras(tarif(t.getConfiguration().getTarif())),
                    Ui.discret(recompenses(t)));
            prix.setMinWidth(150);
            HBox.setHgrow(prix, Priority.ALWAYS);

            HBox ligne = new HBox(16, ident, jauge, prix);
            ligne.setAlignment(Pos.CENTER_LEFT);
            ligne.getStyleClass().add("ligne-liste");
            VBox bloc = new VBox(4, ligne);
            // Mes conditions : ce tableau est-il à ma portée (points, sexe, catégorie) ?
            if (fiche.isPresent()) {
                ServiceInscriptionEnLigne.Bilan b = contexte.simulerInscription(tournoi, List.of(t));
                ResultatInscriptionTournoi r = b.parTableau().get(t);
                if (r != null && isRefusConditions(r)) {
                    Label motif = new Label(MotifsInscription.texte(r, tournoi, b.joueur(), t));
                    motif.getStyleClass().add("motif-refus");
                    bloc.getChildren().add(motif);
                    bloc.getStyleClass().add("tableau-grise");
                }
            }
            lignes.getChildren().add(bloc);
        }
        String regles = String.join(" ", reglesNombreDeTableaux());
        VBox c = Ui.carte(Ui.titreAvecCompteur("Tableaux", tournoi.getTableaux().size()), lignes);
        if (!regles.isBlank()) {
            c.getChildren().add(Ui.aide(regles));
        }
        c.getChildren().add(Ui.aide("Tarif : inscription en ligne / sur place."));
        return c;
    }

    /** Refus lié aux conditions du tableau lui-même (pas aux autres choix ni à la période). */
    private static boolean isRefusConditions(ResultatInscriptionTournoi r) {
        ResultatInscriptionTournoi.Motif m = r.getMotif();
        return m == ResultatInscriptionTournoi.Motif.NON_ELIGIBLE
                || m == ResultatInscriptionTournoi.Motif.CATEGORIE_NON_ADMISE
                || m == ResultatInscriptionTournoi.Motif.CATEGORIE_NON_AUTORISEE;
    }

    private List<String> reglesNombreDeTableaux() {
        try {
            return new GenerateurReglement().apercuNombreDeTableaux(tournoi, tournoi.getQuotasTableaux());
        } catch (RuntimeException e) {
            return List.of();
        }
    }

    static String tarif(TarifInscription t) {
        if (t == null) {
            return "—";
        }
        if (t.isEnLigneProposee() && t.isSurPlaceProposee()) {
            return Formats.montant(t.getMontantEnLigne()) + " / " + Formats.montant(t.getMontantSurPlace());
        }
        return LibellesTableau.prix(t);
    }

    private static String recompenses(Tableau t) {
        if (t.getConfiguration().getRecompenses() == null) {
            return "";
        }
        double argent = LibellesTableau.totalArgent(t.getConfiguration().getRecompenses());
        double part = LibellesTableau.partInscriptions(t.getConfiguration().getRecompenses());
        if (argent > 0) {
            return "Dotation " + Formats.montant(argent) + (part > 0 ? " + part des inscriptions" : "");
        }
        return part > 0 ? "Dotation : part des inscriptions" : "Lots";
    }

    // ------------------------------------------------------------------
    // Documents
    // ------------------------------------------------------------------

    private Node carteDocuments() {
        Button reglement = Ui.boutonSecondaire("Règlement (PDF)…");
        Button feuille = Ui.boutonSecondaire("Feuille d'engagement (PDF)…");
        Label message = Ui.message();
        reglement.setOnAction(e -> telecharger("Reglement - ", message, true));
        feuille.setOnAction(e -> telecharger("Feuille d'engagement - ", message, false));
        Node lien = null;
        if (tournoi.getPaiement() != null && tournoi.getPaiement().getLien() != null) {
            lien = Ui.discret("Billetterie du club : " + tournoi.getPaiement().getLien());
        }
        VBox c = Ui.carte("Documents", Ui.ligne(10, reglement, feuille), message);
        if (lien != null) {
            c.getChildren().add(lien);
        }
        return c;
    }

    private void telecharger(String prefixe, Label message, boolean reglement) {
        FileChooser fc = new FileChooser();
        fc.setTitle(reglement ? "Télécharger le règlement" : "Télécharger la feuille d'engagement");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Document PDF", "*.pdf"));
        fc.setInitialFileName(prefixe + tournoi.getNom().replaceAll("[\\\\/:*?\"<>|]", "") + ".pdf");
        File f = fc.showSaveDialog(racine.getScene() == null ? null : racine.getScene().getWindow());
        if (f == null) {
            return;
        }
        try {
            Path cible = f.toPath();
            // Tournoi publié : versions figées à la publication (VueReglementFige).
            if (reglement) {
                VueReglementFige.ecrireReglement(tournoi, contexte.documents(), cible);
            } else {
                VueReglementFige.ecrireFeuille(tournoi, cible);
            }
            Ui.succes(message, "Enregistré : " + cible.getFileName());
        } catch (Exception ex) {
            Ui.erreur(message, "Téléchargement impossible : " + ex.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Participants
    // ------------------------------------------------------------------

    private Node carteParticipants() {
        List<RemplissageTableaux.EtatTableau> etats = RemplissageTableaux.parTableau(tournoi);
        int n = RemplissageTableaux.nombreDeParticipants(tournoi);
        VBox c = Ui.carte(Ui.titreAvecCompteur("Participants", n));
        if (etats.isEmpty()) {
            return c;
        }
        ComboBox<Tableau> choix = new ComboBox<>();
        etats.forEach(e -> choix.getItems().add(e.tableau()));
        choix.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Tableau t) {
                return t == null ? "" : t.getNom() + " — " + t.getInscrits().size() + " inscrits"
                        + (t.getFileAttente().isEmpty() ? "" : ", " + t.getFileAttente().size() + " en attente");
            }

            @Override
            public Tableau fromString(String s) {
                return null;
            }
        });
        String moi = contexte.ficheLicenceLiee().map(FicheLicence::getNumeroLicence).orElse(null);
        // Par défaut : le premier tableau où je suis, sinon le premier.
        Tableau initial = etats.get(0).tableau();
        if (moi != null) {
            for (RemplissageTableaux.EtatTableau e : etats) {
                if (e.inscrits().stream().anyMatch(j -> j.getNumeroLicence().equals(moi))
                        || e.attente().stream().anyMatch(j -> j.getNumeroLicence().equals(moi))) {
                    initial = e.tableau();
                    break;
                }
            }
        }
        VBox zone = new VBox(8);
        Runnable maj = () -> {
            RemplissageTableaux.EtatTableau e = RemplissageTableaux.etat(choix.getValue());
            ListView<Object> lv = BlocsInscriptions.liste(e, moi);
            lv.setPrefHeight(Math.max(140, Math.min(420, 46 * (lv.getItems().size() + 1))));
            zone.getChildren().setAll(BlocsInscriptions.jauge(e), lv);
        };
        choix.valueProperty().addListener((o, a, b) -> maj.run());
        choix.setValue(initial);
        c.getChildren().addAll(Ui.ligne(10, Ui.discret("Tableau"), choix), zone,
                Ui.aide("Triés par points de licence, puis la liste d'attente dans l'ordre. Mise à jour à chaque inscription."));
        return c;
    }

    // ------------------------------------------------------------------
    // S'inscrire
    // ------------------------------------------------------------------

    private Node barreInscription() {
        String refus = contexte.refusInscription(tournoi);
        boolean dejaInscrit = contexte.ficheLicenceLiee()
                .flatMap(f -> RemplissageTableaux.participant(tournoi, f.getNumeroLicence())).isPresent();
        Button inscrire = Ui.boutonPrimaire(dejaInscrit ? "Ajouter des tableaux" : "S'inscrire");
        inscrire.getStyleClass().add("bouton-large");
        inscrire.setDisable(refus != null);
        inscrire.setOnAction(e -> racine.setCenter(new AssistantInscription(contexte, fichier, tournoi,
                () -> rouvrir.accept(fichier.toString()), onRetour).build()));
        VBox v = new VBox(8, inscrire);
        v.setAlignment(Pos.CENTER);
        if (refus != null) {
            Label l = new Label(refus);
            l.setWrapText(true);
            l.getStyleClass().add("motif-refus");
            v.getChildren().add(l);
        } else {
            v.getChildren().add(Ui.discret("Choix des tableaux avec vérification immédiate des règles du tournoi."));
        }
        v.getStyleClass().add("barre-inscription");
        return v;
    }

    /** Pour le harnais et les tests. */
    Tournoi tournoi() {
        return tournoi;
    }
}
