package com.pingmanager.ui.infra;

import com.pingmanager.persistence.ServiceFichierTournoi;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Met une feuille PDF entre les mains de la table de marque : le fichier est
 * <b>toujours enregistré d'abord</b> (dossier {@code feuilles} des données de
 * PingManager), puis ouvert dans la visionneuse du système (impression avec
 * Ctrl+P) ou, si on le demande et que le système le permet, envoyé
 * directement à l'imprimante par défaut. Si le système ne sait faire ni l'un
 * ni l'autre, le chemin du fichier est rendu pour être indiqué à l'écran.
 *
 * <p>L'ouverture se fait hors du fil JavaFX : sur certains bureaux, les appels
 * à {@link Desktop} bloquent le temps que la visionneuse démarre.</p>
 */
public final class ImpressionFeuilles {

    private static final DateTimeFormatter HORODATAGE = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private ImpressionFeuilles() {
    }

    /**
     * Ce qui a été fait.
     *
     * @param fichier PDF enregistré, ou {@code null} si l'écriture a échoué
     * @param message phrase à afficher (succès ou erreur)
     */
    public record Resultat(Path fichier, String message) {
        public boolean estReussi() {
            return fichier != null;
        }
    }

    /** Dossier des feuilles : {@code <données PingManager>/feuilles}. */
    public static Path dossierParDefaut() {
        return ServiceFichierTournoi.dossierDonneesParDefaut().resolve("feuilles");
    }

    /**
     * Enregistre le PDF dans le dossier par défaut puis l'ouvre (ou l'imprime).
     *
     * @param pdf          contenu du PDF
     * @param nom          début du nom de fichier (« feuille-match-table-4 »), nettoyé
     * @param imprimerDirect envoyer à l'imprimante par défaut si possible, sinon ouvrir
     */
    public static Resultat ouvrir(byte[] pdf, String nom, boolean imprimerDirect) {
        return ouvrir(pdf, nom, imprimerDirect, dossierParDefaut(), true);
    }

    /** @param lancerVisionneuse {@code false} en test : enregistre seulement. */
    static Resultat ouvrir(byte[] pdf, String nom, boolean imprimerDirect, Path dossier, boolean lancerVisionneuse) {
        Path fichier;
        try {
            Files.createDirectories(dossier);
            String n = nomFichier(nom, LocalDateTime.now());
            fichier = dossier.resolve(n);
            // Deux feuilles dans la même seconde : on n'écrase jamais la précédente.
            for (int i = 2; Files.exists(fichier); i++) {
                fichier = dossier.resolve(n.replace(".pdf", "-" + i + ".pdf"));
            }
            Files.write(fichier, pdf);
        } catch (IOException e) {
            return new Resultat(null, "La feuille n'a pas pu être enregistrée (" + e.getMessage() + ").");
        }
        String action = lancerVisionneuse ? lancer(fichier.toFile(), imprimerDirect)
                : "Feuille enregistrée : ouvrez-la pour l'imprimer.";
        return new Resultat(fichier, action + " Fichier : " + fichier.toAbsolutePath());
    }

    /** Lance l'ouverture ou l'impression en arrière-plan ; rend la phrase correspondante. */
    private static String lancer(File f, boolean imprimerDirect) {
        boolean bureau;
        try {
            bureau = !java.awt.GraphicsEnvironment.isHeadless() && Desktop.isDesktopSupported();
        } catch (RuntimeException | LinkageError e) {
            bureau = false;
        }
        if (!bureau) {
            return "Feuille enregistrée : ouvrez-la pour l'imprimer.";
        }
        Desktop d = Desktop.getDesktop();
        boolean imprimer = imprimerDirect && d.isSupported(Desktop.Action.PRINT);
        boolean ouvrir = d.isSupported(Desktop.Action.OPEN);
        if (!imprimer && !ouvrir) {
            return "Feuille enregistrée : ouvrez-la pour l'imprimer.";
        }
        Thread t = new Thread(() -> {
            try {
                if (imprimer) {
                    d.print(f);
                } else {
                    d.open(f);
                }
            } catch (IOException | RuntimeException e) {
                // Rien de perdu : le fichier est enregistré et son chemin affiché.
            }
        }, "impression-feuille");
        t.setDaemon(true);
        t.start();
        return imprimer ? "Feuille envoyée à l'imprimante." : "Feuille ouverte : imprimez-la (Ctrl+P).";
    }

    /** « feuille-poule-a-20261114-093012.pdf » : sans accents ni caractères gênants. */
    static String nomFichier(String nom, LocalDateTime quand) {
        String base = Normalizer.normalize(nom == null ? "" : nom, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.FRENCH)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
        if (base.isEmpty()) {
            base = "feuille";
        }
        if (base.length() > 60) {
            base = base.substring(0, 60).replaceAll("-+$", "");
        }
        return base + "-" + HORODATAGE.format(quand) + ".pdf";
    }
}
