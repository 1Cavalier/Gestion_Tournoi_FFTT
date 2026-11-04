package com.pingmanager.ui.ecran;

import com.pingmanager.core.ServiceCompletudeTournoi.Niveau;
import com.pingmanager.core.reglement.GenerateurReglement;
import com.pingmanager.model.ConditionsMaintien;
import com.pingmanager.model.ConditionsMaintien.ModeReduction;
import com.pingmanager.model.PolitiqueForfaits;
import com.pingmanager.model.PolitiqueForfaits.Periode;
import com.pingmanager.model.PolitiqueForfaits.Regle;
import com.pingmanager.model.PolitiqueForfaits.Remboursement;
import com.pingmanager.model.QuotasTableaux;
import com.pingmanager.model.QuotasTableaux.Quota;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Carte « Règles d'inscription » de la section Tableaux, sous la grille :
 * <ol>
 *   <li><b>Tableaux par jour</b> : nombre maximum de tableaux par joueur et par
 *       joueuse, identique chaque jour ou différent selon le jour ;</li>
 *   <li><b>Maintien des tableaux et dotations</b> : tableau annulable sous N
 *       inscrits (décision à une date), dotation réduite selon le remplissage ou
 *       le nombre d'absents ;</li>
 *   <li><b>Forfaits</b> : pour chacun des trois moments (pendant les
 *       inscriptions, après la clôture, le jour J), un choix en un clic —
 *       aucun remboursement, partiel (part en %) ou complet — avec ou sans
 *       justificatif.</li>
 * </ol>
 * Tout est repris par le règlement généré.
 */
final class CarteReglesInscription {

    private final Tournoi tournoi;
    private final Runnable enregistrer;

    // Quotas
    private final TextField joueurs = court("illimité");
    private final TextField joueuses = court("illimité");
    private final CheckBox varie = new CheckBox("Le nombre varie selon le jour");
    private final Map<LocalDate, TextField[]> parJour = new LinkedHashMap<>();
    private static final String SANS_CONDITION = "Sans condition (n'importe quel tableau)";
    private final ComboBox<String> tableauBonus = new ComboBox<>();
    private final CheckBox bonusObligatoire =
            new CheckBox("Obligatoire : le jour où il est joué, une joueuse doit s'inscrire à ce tableau");
    private final CheckBox consecutifs = new CheckBox("Interdire deux tableaux qui se suivent dans la journée");

    // Maintien
    private final TextField minimum = court("—");
    private final DatePicker dateDecision;
    private final ToggleGroup reduction = new ToggleGroup();
    private final TextField seuil = court("ex. 50");
    // Vide : le règlement dit « sa dotation en espèces peut être réduite ».
    private final TextField reductionRemplissage = court("facultatif");
    private final TextField tranche = court("ex. 5");
    private final TextField reductionAbsents = court("ex. 10");

    // Forfaits
    private final Map<Periode, ToggleGroup> choix = new EnumMap<>(Periode.class);
    private final Map<Periode, TextField> parts = new EnumMap<>(Periode.class);
    private final Map<Periode, CheckBox> justificatifs = new EnumMap<>(Periode.class);
    private final CheckBox fraisDeduits = new CheckBox("Les frais de paiement en ligne ne sont pas remboursés");

    CarteReglesInscription(Tournoi tournoi, Runnable enregistrer) {
        this.tournoi = tournoi;
        this.enregistrer = enregistrer;
        LocalDate j1 = tournoi.getDateDebut();
        ConditionsMaintien m = tournoi.getConditionsMaintien();
        this.dateDecision = Saisie.selecteurDate(m.getDateDecision(),
                j1 == null ? null : LocalDate.of(2000, 1, 1), j1);
    }

    Node build() {
        Label message = Ui.message();
        Button valider = Ui.boutonPrimaire("Enregistrer les règles");
        valider.setOnAction(e -> {
            Ui.cacher(message);
            try {
                QuotasTableaux q = lireQuotas();
                ConditionsMaintien m = lireMaintien();
                PolitiqueForfaits f = lireForfaits();
                tournoi.setQuotasTableaux(q);
                tournoi.setConditionsMaintien(m);
                tournoi.setPolitiqueForfaits(f);
                enregistrer.run();
                Ui.succes(message, "Règles d'inscription enregistrées.");
            } catch (IllegalArgumentException ex) {
                Ui.erreur(message, ex.getMessage());
            }
        });

        boolean forfaitsOk = tournoi.getPolitiqueForfaits().estComplete();
        Node entete = Ui.enteteAvecActions(Ui.titreCarte("Règles d'inscription"),
                Ui.badgeEtat(Niveau.RECOMMANDE, forfaitsOk));
        return Ui.carte(entete,
                Ui.note("Ces règles figurent dans le règlement généré (section Complémentaire)."),
                sousTitre("Tableaux par jour", Niveau.OPTIONNEL), quotas(),
                sousTitre("Maintien des tableaux et dotations", Niveau.OPTIONNEL), maintien(),
                sousTitre("Forfaits et remboursements", Niveau.RECOMMANDE), forfaits(),
                message, Ui.actions(valider));
    }

    private static Node sousTitre(String s, Niveau n) {
        Label l = Ui.gras(s);
        HBox h = Ui.ligne(8, Ui.puceDependance(n), l);
        h.getStyleClass().add("sous-titre-carte");
        return h;
    }

    // ==================================================================
    // Tableaux par jour
    // ==================================================================

    private Node quotas() {
        QuotasTableaux q = tournoi.getQuotasTableaux();
        Quota tous = q.getTousLesJours();
        joueurs.setText(texte(tous == null ? null : tous.joueurs()));
        joueuses.setText(texte(tous == null ? null : tous.joueuses()));

        List<LocalDate> jours = jours();
        VBox lignesJours = new VBox(8);
        GridPane g = new GridPane();
        g.setHgap(16);
        g.setVgap(8);
        g.add(Ui.enteteColonne("Jour"), 0, 0);
        g.add(Ui.enteteColonne("Joueurs"), 1, 0);
        g.add(Ui.enteteColonne("Joueuses"), 2, 0);
        int r = 1;
        for (LocalDate j : jours) {
            Quota qj = q.varieSelonLeJour() ? q.pour(j) : tous;
            TextField h = court("illimité");
            TextField f = court("illimité");
            h.setText(texte(qj == null ? null : qj.joueurs()));
            f.setText(texte(qj == null ? null : qj.joueuses()));
            parJour.put(j, new TextField[] {h, f});
            g.add(Ui.texte(Saisie.jourLong(j)), 0, r);
            g.add(h, 1, r);
            g.add(f, 2, r);
            r++;
        }
        lignesJours.getChildren().add(g);
        lignesJours.getStyleClass().add("liste-depliee");

        HBox identiques = Ui.ligne(16,
                Ui.champEtiquete("Joueurs (max. par jour)", joueurs),
                Ui.champEtiquete("Joueuses (max. par jour)", joueuses));
        varie.setSelected(q.varieSelonLeJour() && jours.size() > 1);
        varie.setDisable(jours.size() < 2);
        Runnable maj = () -> {
            visible(identiques, !varie.isSelected());
            visible(lignesJours, varie.isSelected());
        };
        varie.selectedProperty().addListener((o, a, n) -> {
            if (n) { // on part des valeurs communes
                parJour.values().forEach(c -> {
                    c[0].setText(joueurs.getText());
                    c[1].setText(joueuses.getText());
                });
            }
            maj.run();
        });
        maj.run();
        // Tableau exigé pour le tableau supplémentaire des joueuses (ex. Toutes séries dames).
        tableauBonus.getItems().add(SANS_CONDITION);
        tournoi.getTableaux().stream()
                .sorted((a, b) -> Boolean.compare(b.getConfiguration().getSexeRequis() == Sexe.FEMME,
                        a.getConfiguration().getSexeRequis() == Sexe.FEMME))
                .forEach(x -> tableauBonus.getItems().add(x.getNom()));
        String choisi = q.getTableauBonusJoueuses();
        if (choisi == null && !q.aUneRegle()) { // première saisie : on propose le tableau dames
            choisi = tournoi.getTableaux().stream()
                    .filter(x -> x.getConfiguration().getSexeRequis() == Sexe.FEMME)
                    .map(Tableau::getNom).findFirst().orElse(null);
        }
        tableauBonus.setValue(choisi != null && tableauBonus.getItems().contains(choisi) ? choisi : SANS_CONDITION);
        tableauBonus.setMaxWidth(Double.MAX_VALUE);
        VBox blocBonus = new VBox(6,
                Ui.champEtiquete("Tableau supplémentaire des joueuses : seulement si elles jouent", tableauBonus),
                Ui.note("Quand les joueuses ont droit à plus de tableaux que les joueurs (ex. 3 contre 2), le "
                        + "tableau en plus n'est accordé que si l'un de leurs tableaux du jour est celui-ci. "
                        + "Sinon, elles sont limitées comme les joueurs. Contrôlé à l'inscription."));
        bonusObligatoire.setWrapText(true);
        bonusObligatoire.setSelected(q.isTableauBonusObligatoire());
        Runnable majObligatoire = () -> {
            boolean sans = SANS_CONDITION.equals(tableauBonus.getValue());
            bonusObligatoire.setDisable(sans);
            if (sans) {
                bonusObligatoire.setSelected(false);
            }
        };
        tableauBonus.valueProperty().addListener((o, a, n) -> majObligatoire.run());
        majObligatoire.run();
        blocBonus.getChildren().add(bonusObligatoire);
        consecutifs.setWrapText(true);
        consecutifs.setSelected(q.isTableauxConsecutifsInterdits());
        VBox blocConsecutifs = new VBox(4, consecutifs, Ui.note("Un joueur inscrit à un tableau ne peut pas "
                + "prendre celui qui le précède ou le suit le même jour. Contrôlé à l'inscription."));
        // Aperçu du texte de l'article, recalculé à chaque changement d'option.
        apercu.getStyleClass().add("encart-regles");
        java.util.List<javafx.beans.Observable> sources = new ArrayList<>(List.of(joueurs.textProperty(),
                joueuses.textProperty(), varie.selectedProperty(), tableauBonus.valueProperty(),
                bonusObligatoire.selectedProperty(), consecutifs.selectedProperty()));
        parJour.values().forEach(c -> {
            sources.add(c[0].textProperty());
            sources.add(c[1].textProperty());
        });
        sources.forEach(src -> src.addListener(o -> majApercu()));
        majApercu();
        return new VBox(10,
                Ui.note("Nombre maximum de tableaux qu'un joueur ou une joueuse peut disputer le même jour. "
                        + "Vide = pas de limite. Exemple courant : 2 pour les joueurs, 3 pour les joueuses "
                        + "si elles prennent le tableau dames."),
                identiques, varie,
                jours.size() < 2 ? Ui.discret("Tournoi sur un seul jour : un seul réglage.") : lignesJours,
                blocBonus, blocConsecutifs,
                Ui.gras("Ce que dira le règlement"), apercu);
    }

    private final VBox apercu = new VBox(4);
    private final GenerateurReglement generateur = new GenerateurReglement();

    /** Met à jour l'aperçu de l'article « Nombre de tableaux par joueur » selon les choix en cours. */
    private void majApercu() {
        apercu.getChildren().clear();
        QuotasTableaux q = lireQuotasSilencieux();
        List<String> texte = q == null ? null : generateur.apercuNombreDeTableaux(tournoi, q);
        if (texte == null) {
            apercu.getChildren().add(Ui.discret("Saisie incomplète : corrigez les nombres pour voir le texte."));
        } else if (texte.isEmpty()) {
            apercu.getChildren().add(Ui.discret("Aucune règle : l'article n'apparaîtra pas dans le règlement."));
        } else {
            texte.forEach(x -> {
                Label l = new Label(x);
                l.setWrapText(true);
                apercu.getChildren().add(l);
            });
        }
    }

    /** Comme {@link #lireQuotas()}, sans signaler d'erreur ; {@code null} si une saisie est invalide. */
    private QuotasTableaux lireQuotasSilencieux() {
        try {
            QuotasTableaux base;
            if (varie.isSelected()) {
                Map<LocalDate, Quota> m = new LinkedHashMap<>();
                for (Map.Entry<LocalDate, TextField[]> e : parJour.entrySet()) {
                    Quota qj = new Quota(lu(e.getValue()[0]), lu(e.getValue()[1]));
                    if (qj.aLimite()) {
                        m.put(e.getKey(), qj);
                    }
                }
                base = m.isEmpty() ? QuotasTableaux.aucun() : QuotasTableaux.parJour(m);
            } else {
                Quota qt = new Quota(lu(joueurs), lu(joueuses));
                base = qt.aLimite() ? QuotasTableaux.identiques(qt.joueurs(), qt.joueuses()) : QuotasTableaux.aucun();
            }
            return base.avecTableauBonusJoueuses(bonus())
                    .avecTableauBonusObligatoire(bonusObligatoire.isSelected())
                    .avecTableauxConsecutifsInterdits(consecutifs.isSelected());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static Integer lu(TextField f) {
        String t = f.getText() == null ? "" : f.getText().trim();
        return t.isEmpty() ? null : Integer.valueOf(t); // NumberFormatException = IllegalArgumentException
    }

    private List<LocalDate> jours() {
        List<LocalDate> l = new ArrayList<>();
        LocalDate d = tournoi.getDateDebut();
        LocalDate f = tournoi.getDateFin();
        if (d != null && f != null && !f.isBefore(d)) {
            for (LocalDate x = d; !x.isAfter(f) && l.size() < 14; x = x.plusDays(1)) {
                l.add(x);
            }
        }
        return l;
    }

    private QuotasTableaux lireQuotas() {
        return lireLimites()
                .avecTableauBonusObligatoire(bonusObligatoire.isSelected())
                .avecTableauxConsecutifsInterdits(consecutifs.isSelected());
    }

    private QuotasTableaux lireLimites() {
        if (varie.isSelected()) {
            Map<LocalDate, Quota> m = new LinkedHashMap<>();
            for (Map.Entry<LocalDate, TextField[]> e : parJour.entrySet()) {
                String jour = Saisie.jourCourt(e.getKey());
                Quota q = quota(e.getValue()[0], e.getValue()[1], jour);
                if (q.aLimite()) {
                    m.put(e.getKey(), q);
                }
            }
            return (m.isEmpty() ? QuotasTableaux.aucun() : QuotasTableaux.parJour(m)).avecTableauBonusJoueuses(bonus());
        }
        Quota q = quota(joueurs, joueuses, null);
        return (q.aLimite() ? QuotasTableaux.identiques(q.joueurs(), q.joueuses()) : QuotasTableaux.aucun())
                .avecTableauBonusJoueuses(bonus());
    }

    private String bonus() {
        String v = tableauBonus.getValue();
        return v == null || SANS_CONDITION.equals(v) ? null : v;
    }

    private static Quota quota(TextField h, TextField f, String jour) {
        String suffixe = jour == null ? "" : " (" + jour + ")";
        Integer a = positif(h, "Tableaux par jour, joueurs" + suffixe);
        Integer b = positif(f, "Tableaux par jour, joueuses" + suffixe);
        return new Quota(a, b);
    }

    // ==================================================================
    // Maintien des tableaux et dotations
    // ==================================================================

    private Node maintien() {
        ConditionsMaintien m = tournoi.getConditionsMaintien();
        minimum.setText(m.getMinimumInscrits() > 0 ? String.valueOf(m.getMinimumInscrits()) : "");
        HBox annulation = Ui.ligne(16,
                Ui.champEtiquete("Tableau annulable sous … inscrits", minimum),
                Ui.champEtiquete("Décision prise le", dateDecision));

        ToggleButton sans = segment("Dotation maintenue", null);
        ToggleButton parRemplissage = segment("Selon le remplissage", ModeReduction.REMPLISSAGE);
        ToggleButton parAbsents = segment("Selon les absents", ModeReduction.ABSENTS);
        HBox seg = groupe(reduction, sans, parRemplissage, parAbsents);

        seuil.setText(m.getReduction() == ModeReduction.REMPLISSAGE ? String.valueOf(m.getSeuilRemplissagePct()) : "");
        reductionRemplissage.setText(m.getReduction() == ModeReduction.REMPLISSAGE && m.getReductionPct() > 0
                ? String.valueOf(m.getReductionPct()) : "");
        tranche.setText(m.getReduction() == ModeReduction.ABSENTS ? String.valueOf(m.getTrancheAbsents()) : "");
        reductionAbsents.setText(m.getReduction() == ModeReduction.ABSENTS ? String.valueOf(m.getReductionPct()) : "");

        HBox detailRemplissage = Ui.ligne(8, Ui.texte("Sous"), seuil, Ui.texte("% de places occupées, dotation réduite de"),
                reductionRemplissage, Ui.texte("%"));
        HBox detailAbsents = Ui.ligne(8, Ui.texte("Par tranche de"), tranche, Ui.texte("absents, dotation réduite de"),
                reductionAbsents, Ui.texte("%"));
        detailRemplissage.getStyleClass().add("liste-depliee");
        detailAbsents.getStyleClass().add("liste-depliee");

        Runnable maj = () -> {
            visible(detailRemplissage, parRemplissage.isSelected());
            visible(detailAbsents, parAbsents.isSelected());
        };
        reduction.selectedToggleProperty().addListener((o, a, n) -> maj.run());
        (m.getReduction() == ModeReduction.REMPLISSAGE ? parRemplissage
                : m.getReduction() == ModeReduction.ABSENTS ? parAbsents : sans).setSelected(true);
        maj.run();

        return new VBox(10,
                Ui.note("Un tableau qui n'atteint pas ce nombre d'inscrits à la date indiquée peut être annulé ; "
                        + "les inscrits sont remboursés ou reportés sur un autre tableau. Vide = pas de clause."),
                annulation,
                Ui.texte("Réduction de la dotation"),
                seg, detailRemplissage, detailAbsents);
    }

    private ConditionsMaintien lireMaintien() {
        Integer min = positif(minimum, "Tableau annulable sous … inscrits");
        LocalDate d = dateDecision.getValue();
        LocalDate j1 = tournoi.getDateDebut();
        if (min != null && d != null && j1 != null && d.isAfter(j1)) {
            Ui.marquerErreur(dateDecision);
            throw new IllegalArgumentException("La décision de maintien doit être prise au plus tard le premier jour du tournoi.");
        }
        ModeReduction mode = (ModeReduction) reduction.getSelectedToggle().getUserData();
        int s = 0;
        int t = 0;
        int r = 0;
        if (mode == ModeReduction.REMPLISSAGE) {
            s = obligatoire(pourcentage(seuil, "Seuil de remplissage"), seuil, "Seuil de remplissage");
            Integer pr = pourcentage(reductionRemplissage, "Réduction de dotation");
            r = pr == null ? 0 : pr; // vide : le règlement dit « peut être réduite »
        } else if (mode == ModeReduction.ABSENTS) {
            t = obligatoire(positif(tranche, "Tranche d'absents"), tranche, "Tranche d'absents");
            r = obligatoire(pourcentage(reductionAbsents, "Réduction de dotation"), reductionAbsents, "Réduction de dotation");
        }
        return new ConditionsMaintien(min == null ? 0 : min, min == null ? null : d, mode, s, t, r);
    }

    // ==================================================================
    // Forfaits
    // ==================================================================

    private Node forfaits() {
        PolitiqueForfaits p = tournoi.getPolitiqueForfaits();
        VBox v = new VBox(10, Ui.note("Pour chaque moment, choisissez en un clic : aucun remboursement, "
                + "remboursement partiel ou complet, avec ou sans justificatif (certificat médical, etc.)."));
        for (Periode per : Periode.values()) {
            v.getChildren().add(blocPeriode(per, p.pour(per)));
        }
        fraisDeduits.setSelected(p.isFraisEnLigneDeduits());
        fraisDeduits.setWrapText(true);
        v.getChildren().add(fraisDeduits);
        return v;
    }

    private Node blocPeriode(Periode per, Regle regle) {
        ToggleGroup g = new ToggleGroup();
        choix.put(per, g);
        ToggleButton aucun = segment("Aucun remboursement", Remboursement.AUCUN);
        ToggleButton partiel = segment("Partiel", Remboursement.PARTIEL);
        ToggleButton complet = segment("Complet", Remboursement.COMPLET);
        HBox seg = groupe(g, aucun, partiel, complet);

        TextField part = court("ex. 50");
        parts.put(per, part);
        HBox detailPart = Ui.ligne(8, Ui.texte("Part remboursée :"), part, Ui.texte("%"));
        CheckBox justif = new CheckBox("Sur justificatif");
        justificatifs.put(per, justif);

        if (regle != null) {
            (regle.remboursement() == Remboursement.PARTIEL ? partiel
                    : regle.remboursement() == Remboursement.COMPLET ? complet : aucun).setSelected(true);
            if (regle.remboursement() == Remboursement.PARTIEL) {
                part.setText(String.valueOf(regle.pourcentage()));
            }
            justif.setSelected(regle.justificatif());
        }
        Label etat = Ui.discret(regle == null ? "À choisir" : "");
        Runnable maj = () -> {
            visible(detailPart, partiel.isSelected());
            justif.setDisable(!partiel.isSelected() && !complet.isSelected());
            if (justif.isDisabled()) {
                justif.setSelected(false);
            }
            etat.setText(g.getSelectedToggle() == null ? "À choisir" : "");
        };
        g.selectedToggleProperty().addListener((o, a, n) -> maj.run());
        maj.run();

        VBox b = new VBox(8,
                Ui.enteteAvecActions(Ui.gras(per.getLibelle()), etat),
                Ui.ligne(16, seg, justif),
                detailPart);
        b.getStyleClass().add("bloc-regle");
        return b;
    }

    private PolitiqueForfaits lireForfaits() {
        Map<Periode, Regle> m = new EnumMap<>(Periode.class);
        for (Periode per : Periode.values()) {
            ToggleGroup g = choix.get(per);
            if (g.getSelectedToggle() == null) {
                continue;
            }
            Remboursement r = (Remboursement) g.getSelectedToggle().getUserData();
            int pct = 0;
            if (r == Remboursement.PARTIEL) {
                TextField part = parts.get(per);
                Integer v = Saisie.lireEntier(part.getText(), "Part remboursée");
                if (v == null || v < 1 || v > 99) {
                    Ui.marquerErreur(part);
                    throw new IllegalArgumentException("Forfait « " + per.getLibelle().toLowerCase()
                            + " » : indiquez une part remboursée entre 1 et 99 %.");
                }
                pct = v;
            }
            m.put(per, new Regle(r, pct, justificatifs.get(per).isSelected()));
        }
        return new PolitiqueForfaits(m, fraisDeduits.isSelected());
    }

    // ==================================================================
    // Outils
    // ==================================================================

    private static ToggleButton segment(String libelle, Object valeur) {
        ToggleButton b = new ToggleButton(libelle);
        b.setUserData(valeur);
        return b;
    }

    /** Boutons accolés à choix exclusif ; un clic sur le bouton actif ne le désélectionne pas. */
    private static HBox groupe(ToggleGroup g, ToggleButton... boutons) {
        HBox h = new HBox();
        h.getStyleClass().add("segment");
        boutons[0].getStyleClass().add("segment-premier");
        boutons[boutons.length - 1].getStyleClass().add("segment-dernier");
        for (ToggleButton b : boutons) {
            b.setToggleGroup(g);
            b.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
            h.getChildren().add(b);
        }
        g.selectedToggleProperty().addListener((o, ancien, nouveau) -> {
            if (nouveau == null && ancien != null) {
                ancien.setSelected(true);
            }
        });
        return h;
    }

    private static TextField court(String invite) {
        TextField t = Ui.champ(invite);
        t.getStyleClass().add("champ-court");
        return t;
    }

    private static Integer positif(TextField champ, String nom) {
        Integer v;
        try {
            v = Saisie.lireEntier(champ.getText(), nom);
        } catch (IllegalArgumentException e) {
            Ui.marquerErreur(champ);
            throw e;
        }
        if (v != null && v < 1) {
            Ui.marquerErreur(champ);
            throw new IllegalArgumentException(nom + " : un nombre supérieur ou égal à 1.");
        }
        return v;
    }

    private static Integer pourcentage(TextField champ, String nom) {
        Integer v = positif(champ, nom);
        if (v != null && v > 100) {
            Ui.marquerErreur(champ);
            throw new IllegalArgumentException(nom + " : un pourcentage entre 1 et 100.");
        }
        return v;
    }

    private static int obligatoire(Integer v, TextField champ, String nom) {
        if (v == null) {
            Ui.marquerErreur(champ);
            throw new IllegalArgumentException(nom + " : valeur à renseigner.");
        }
        return v;
    }

    private static String texte(Integer i) {
        return i == null ? "" : String.valueOf(i);
    }

    private static void visible(Node n, boolean v) {
        n.setVisible(v);
        n.setManaged(v);
    }
}
