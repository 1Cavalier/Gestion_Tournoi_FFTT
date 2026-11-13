package com.pingmanager.ui.ecran;

import com.pingmanager.core.reglement.GenerateurFeuilleEngagement;
import com.pingmanager.core.reglement.RenduPdfReglement;
import com.pingmanager.core.reglement.ServiceReglementTournoi;
import com.pingmanager.model.OptionsReglement;
import com.pingmanager.model.Tournoi;
import com.pingmanager.persistence.DocumentsDeposes;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

/**
 * Règlement et feuille d'engagement d'un tournoi <b>publié</b> : ils sont figés
 * ({@link ServiceReglementTournoi}), donc plus de grande carte d'édition. Il
 * reste un lien compact « Règlement » ({@link #lien}) qui ouvre une page en
 * lecture seule ({@link #page}) avec l'aperçu de la version figée et les
 * téléchargements PDF.
 */
final class VueReglementFige {

    private static final ServiceReglementTournoi REGLEMENTS = new ServiceReglementTournoi();

    private VueReglementFige() {
    }

    /**
     * Ligne compacte : lien « Règlement » (ouvre la page de lecture), lien de
     * téléchargement de la feuille d'engagement, mention « figés ».
     */
    static Node lien(Tournoi tournoi, DocumentsDeposes documents, Path dossier, Runnable enregistrer,
                     Runnable ouvrir) {
        Button reglement = Ui.boutonLien("Règlement");
        reglement.setOnAction(e -> ouvrir.run());
        Button feuille = Ui.boutonLien("Feuille d'engagement (PDF)…");
        Label message = Ui.message();
        feuille.setOnAction(e -> telechargerFeuille(tournoi, documents, dossier, enregistrer, message, feuille));
        Label mention = Ui.discret("figés depuis la publication");
        VBox v = new VBox(4, Ui.ligne(12, reglement, feuille, mention), message);
        v.getStyleClass().add("reglement-fige");
        return v;
    }

    /** Page de lecture du règlement figé, avec téléchargements. */
    static Node page(Tournoi tournoi, DocumentsDeposes documents, Path dossier, Runnable enregistrer,
                     Runnable retour, String libelleRetour) {
        garantirCopies(tournoi, documents, enregistrer);
        Button btnRetour = Ui.boutonLien(libelleRetour);
        btnRetour.setOnAction(e -> retour.run());
        Label message = Ui.message();
        Button pdf = Ui.boutonPrimaire("Télécharger le règlement (PDF)…");
        pdf.setOnAction(e -> {
            try {
                telecharger(pdf, dossier, "Télécharger le règlement", "Reglement - ", tournoi,
                        cible -> ecrireReglement(tournoi, documents, cible))
                        .ifPresent(f -> Ui.succes(message, "Règlement enregistré : " + f + "."));
            } catch (IOException ex) {
                Ui.erreur(message, ex.getMessage());
            }
        });
        Button feuille = Ui.boutonSecondaire("Feuille d'engagement (PDF)…");
        feuille.setOnAction(e -> telechargerFeuille(tournoi, documents, dossier, enregistrer, message, feuille));

        Label etat = Ui.note("Le tournoi est publié : le règlement et la feuille d'engagement sont figés "
                + "tels qu'à la publication et ne peuvent plus être modifiés.");
        Node contenu;
        if (estDepose(tournoi, documents)) {
            contenu = Ui.carte("Règlement déposé par le club",
                    Ui.note("Fichier : " + DocumentsDeposes.nomAffiche(tournoi.getReglementDocument())));
        } else {
            if (deposeIntrouvable(tournoi, documents)) {
                Ui.erreur(message, "Règlement déposé : fichier introuvable ("
                        + DocumentsDeposes.nomAffiche(tournoi.getReglementDocument())
                        + "). La version générée est affichée.");
            }
            contenu = Ui.carteNue(ApercuReglement.rendre(REGLEMENTS.reglement(tournoi)));
        }
        VBox page = Ui.page(860, Ui.enteteAvecActions(btnRetour, pdf, feuille), etat, message, contenu);
        page.getStyleClass().add("page-reglement-fige");
        return Ui.defilant(page);
    }

    /** Écrit le règlement publié : copie du PDF déposé, ou PDF de la version figée. */
    static void ecrireReglement(Tournoi tournoi, DocumentsDeposes documents, Path cible) throws IOException {
        Optional<Path> depose = estDepose(tournoi, documents)
                ? documents.fichier(tournoi.getReglementDocument()) : Optional.empty();
        if (depose.isPresent()) {
            Files.copy(depose.get(), cible, StandardCopyOption.REPLACE_EXISTING);
        } else {
            RenduPdfReglement.ecrire(REGLEMENTS.reglement(tournoi), cible);
        }
    }

    /** Écrit la feuille d'engagement (version figée si le tournoi est publié). */
    static void ecrireFeuille(Tournoi tournoi, Path cible) throws IOException {
        RenduPdfReglement.ecrire(REGLEMENTS.feuilleEngagement(tournoi), cible,
                GenerateurFeuilleEngagement.FORMAT_TITRE);
    }

    /**
     * Règlement déposé dont la copie existe dans le dossier de données (sinon, la
     * version générée sert). Un ancien chemin absolu n'est jamais ouvert.
     */
    static boolean estDepose(Tournoi tournoi, DocumentsDeposes documents) {
        return modeDepose(tournoi) && documents.fichier(tournoi.getReglementDocument()).isPresent();
    }

    /** Mode « déposé » avec une référence dont le fichier est introuvable. */
    static boolean deposeIntrouvable(Tournoi tournoi, DocumentsDeposes documents) {
        return modeDepose(tournoi) && tournoi.getReglementDocument() != null
                && documents.fichier(tournoi.getReglementDocument()).isEmpty();
    }

    private static boolean modeDepose(Tournoi tournoi) {
        OptionsReglement o = tournoi.getOptionsReglement();
        return o != null && o.getMode() == OptionsReglement.Mode.DEPOSE && tournoi.getReglementDocument() != null;
    }

    /** Tournoi publié par une ancienne version : crée les copies manquantes et les enregistre. */
    private static void garantirCopies(Tournoi tournoi, DocumentsDeposes documents, Runnable enregistrer) {
        if (!ServiceReglementTournoi.estFige(tournoi)) {
            return;
        }
        boolean manque = tournoi.getFeuilleEngagementFigee() == null
                || (tournoi.getReglementFige() == null && !estDepose(tournoi, documents));
        if (manque) {
            REGLEMENTS.reglement(tournoi);
            REGLEMENTS.feuilleEngagement(tournoi);
            enregistrer.run();
        }
    }

    private static void telechargerFeuille(Tournoi tournoi, DocumentsDeposes documents, Path dossier,
                                           Runnable enregistrer, Label message, Node source) {
        Ui.cacher(message);
        try {
            garantirCopies(tournoi, documents, enregistrer);
            telecharger(source, dossier, "Télécharger la feuille d'engagement", "Feuille d'engagement - ",
                    tournoi, cible -> ecrireFeuille(tournoi, cible))
                    .ifPresent(f -> Ui.succes(message, "Feuille d'engagement enregistrée : " + f + "."));
        } catch (IOException ex) {
            Ui.erreur(message, ex.getMessage());
        }
    }

    private interface EcrivainPdf {
        void ecrire(Path cible) throws IOException;
    }

    private static Optional<File> telecharger(Node source, Path dossier, String titre, String prefixe,
                                              Tournoi tournoi, EcrivainPdf ecrivain) throws IOException {
        FileChooser fc = new FileChooser();
        fc.setTitle(titre);
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Document PDF", "*.pdf"));
        String nom = tournoi.getNom() == null ? "tournoi" : tournoi.getNom();
        fc.setInitialFileName(prefixe + nom.replaceAll("[\\\\/:*?\"<>|]", "-") + ".pdf");
        if (dossier != null && Files.isDirectory(dossier)) {
            fc.setInitialDirectory(dossier.toFile());
        }
        Window fenetre = source.getScene() == null ? null : source.getScene().getWindow();
        File f = fc.showSaveDialog(fenetre);
        if (f == null) {
            return Optional.empty();
        }
        Path cible = f.getName().toLowerCase().endsWith(".pdf") ? f.toPath()
                : f.toPath().resolveSibling(f.getName() + ".pdf");
        ecrivain.ecrire(cible);
        return Optional.of(cible.toFile());
    }
}
