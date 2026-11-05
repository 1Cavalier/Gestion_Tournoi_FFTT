package com.pingmanager.ui.ecran;

import com.pingmanager.model.StatutTournoi;
import com.pingmanager.model.Tournoi;
import com.pingmanager.persistence.ProjetTournoi;
import com.pingmanager.ui.ContexteApplication;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Accueil de la gestion des tournois (réservé aux comptes rattachés à un club).
 *
 * <p>Trois temps : la liste des tournois (regroupés par statut du cycle de vie
 * <b>brouillon → homologué → publié</b>), un formulaire de création d'un
 * brouillon, et l'écran de complétion d'un tournoi. Tout passe par le
 * {@link ContexteApplication} ; la présentation vient de {@code theme.css}.</p>
 */
public final class PageGestionTournoi {

    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("d MMMM yyyy", java.util.Locale.FRENCH);
    private static final DateTimeFormatter JOUR_COURT = DateTimeFormatter.ofPattern("d MMM", java.util.Locale.FRENCH);

    private final ContexteApplication contexte;
    private final BorderPane racine = new BorderPane();

    public PageGestionTournoi(ContexteApplication contexte) {
        this.contexte = contexte;
    }

    public Parent build() {
        afficherAccueil();
        return racine;
    }

    private void poser(Node contenu) {
        Node page = Ui.defilant(contenu);
        racine.setCenter(page);
        Ui.apparition(page);
    }

    // ==================================================================
    // Accueil : liste des tournois par statut
    // ==================================================================

    private void afficherAccueil() {
        List<Resume> tournois = chargerResumes();

        Button nouveau = Ui.boutonPrimaire("Nouveau tournoi");
        nouveau.setOnAction(e -> afficherFormulaire());

        VBox titres = new VBox(4, Ui.titre("Gestion des tournois"), clubsGeres());
        HBox barre = Ui.enteteAvecActions(titres, nouveau);

        VBox contenu = Ui.page(1040, barre, tuiles(tournois));

        contenu.getChildren().add(sectionEnCours(tournois));
        contenu.getChildren().add(groupe("Publiés", StatutTournoi.PUBLIE, tournois,
                "validés, inscriptions ouvertes, en attente du jour J"));
        contenu.getChildren().add(groupe("Homologués", StatutTournoi.HOMOLOGUE, tournois,
                "numéro FFTT reçu, prêts à être publiés"));
        contenu.getChildren().add(groupe("Demandes d'homologation", StatutTournoi.DEMANDE_HOMOLOGATION, tournois,
                "dossier complet, en attente du numéro d'homologation"));
        contenu.getChildren().add(groupe("Brouillons", StatutTournoi.BROUILLON, tournois,
                "en préparation, pas encore homologués"));
        poser(contenu);
    }

    /** Chiffres clés du club : publiés, en préparation, joueurs inscrits, prochain tournoi. */
    private static Node tuiles(List<Resume> tournois) {
        int publies = 0;
        int preparation = 0;
        int inscrits = 0;
        Resume prochain = null;
        LocalDate auj = LocalDate.now();
        for (Resume r : tournois) {
            if (r.statut == StatutTournoi.PUBLIE) {
                publies++;
                inscrits += r.participants;
            } else if (r.statut != StatutTournoi.ANNULE) {
                preparation++;
            }
            if (r.statut != StatutTournoi.ANNULE && r.debut != null && !r.debut.isBefore(auj)
                    && (prochain == null || r.debut.isBefore(prochain.debut))) {
                prochain = r;
            }
        }
        VBox tPublies = Ui.tuileChiffre(String.valueOf(publies), publies > 1 ? "tournois publiés" : "tournoi publié");
        tPublies.getStyleClass().add("tuile-cle-accent");
        Node tProchain = prochain == null
                ? Ui.tuileChiffre("—", "aucun tournoi à venir")
                : Ui.tuileChiffre(prochain.debut.format(JOUR_COURT), "prochain : " + prochain.nom,
                        Ui.pastille(StatutPastille.libelleTournoi(prochain.statut),
                                StatutPastille.pourTournoi(prochain.statut)));
        return Ui.rangeeTuiles(
                tPublies,
                Ui.tuileChiffre(String.valueOf(preparation), "en préparation (brouillons, homologation)"),
                Ui.tuileChiffre(String.valueOf(inscrits), "joueurs engagés (tournois publiés, attente comprise)"),
                tProchain);
    }

    private List<Resume> chargerResumes() {
        List<Resume> resumes = new ArrayList<>();
        for (ProjetTournoi projet : contexte.tournoisRecents()) {
            try {
                Tournoi t = contexte.chargerTournoi(projet.getFichier());
                if (contexte.estTournoiDuCompte(t)) { // seulement les tournois des clubs du compte
                    resumes.add(Resume.depuis(t, projet.getFichier()));
                }
            } catch (RuntimeException ignore) {
                // Fichier déplacé, supprimé ou illisible : on ne l'affiche pas.
            }
        }
        return resumes;
    }

    private Node sectionEnCours(List<Resume> tournois) {
        // Priorité au tournoi dont le jour J a commencé (un tableau en pointage ou lancé),
        // sinon le premier tournoi publié.
        Resume enCours = tournois.stream()
                .filter(r -> r.jourJEntame)
                .findFirst()
                .orElse(tournois.stream().filter(r -> r.statut == StatutTournoi.PUBLIE).findFirst().orElse(null));

        if (enCours == null) {
            return Ui.carte("Tournoi en cours",
                    Ui.etatVide("Aucun tournoi en cours pour le moment. Les tournois publiés, "
                            + "prêts pour le jour J, apparaîtront ici.", null));
        }
        Label nom = Ui.label(enCours.nom, "en-cours-nom");
        Label meta = Ui.discret(enCours.descriptifLong);
        Button suivre = Ui.boutonPrimaire("Suivre le tournoi");
        suivre.setOnAction(e -> racine.setCenter(new EcranJourJ(contexte, enCours.fichier, this::afficherAccueil,
                "← Gestion des tournois").build()));
        Label etat = enCours.jourJEntame
                ? Ui.pastille("Jour J commencé", StatutPastille.INFO)
                : Ui.pastille(StatutPastille.libelleTournoi(enCours.statut), StatutPastille.pourTournoi(enCours.statut));
        VBox infos = new VBox(6, Ui.ligne(10, nom, etat), meta);
        HBox.setHgrow(infos, Priority.ALWAYS);
        Label surtitre = Ui.label("TOURNOI EN COURS", "surtitre");
        VBox carte = Ui.carte(surtitre, Ui.ligne(16, Ui.blocDate(enCours.debut), infos, suivre));
        carte.getStyleClass().add("carte-en-cours");
        return carte;
    }

    private Node groupe(String titre, StatutTournoi statut, List<Resume> tous, String aide) {
        List<Resume> liste = new ArrayList<>();
        for (Resume r : tous) {
            if (r.statut == statut) {
                liste.add(r);
            }
        }
        VBox corps = new VBox(0);
        if (liste.isEmpty()) {
            corps.getChildren().add(Ui.etatVideCompact("Aucun pour l'instant."));
        } else {
            for (int i = 0; i < liste.size(); i++) {
                HBox l = ligneTournoi(liste.get(i));
                if (i == 0) {
                    l.getStyleClass().add("ligne-element-premiere");
                }
                corps.getChildren().add(l);
            }
        }
        HBox titreGroupe = Ui.ligne(10, Ui.titreCarte(titre),
                Ui.pastille(String.valueOf(liste.size()), liste.isEmpty()
                        ? StatutPastille.NEUTRE : StatutPastille.pourTournoi(statut)));
        VBox entete = new VBox(2, titreGroupe, Ui.aide(capitaliser(aide)));
        return Ui.carte(entete, corps);
    }

    private static String capitaliser(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1) + ".";
    }

    private HBox ligneTournoi(Resume r) {
        List<Node> droite = new ArrayList<>();
        if (r.mode == com.pingmanager.model.ModePreparation.IMPORT) {
            droite.add(Ui.pastille("Tournoi importé", StatutPastille.INFO));
        } else if (r.mode == com.pingmanager.model.ModePreparation.A_CHOISIR) {
            droite.add(Ui.pastille("Mode à choisir", StatutPastille.ATTENTION));
        }
        if (r.jourJEntame) {
            droite.add(Ui.pastille("Jour J commencé", StatutPastille.INFO));
        }

        Button completer = Ui.boutonSecondaire(r.mode == com.pingmanager.model.ModePreparation.COMPLETE
                ? "Compléter" : r.mode == com.pingmanager.model.ModePreparation.IMPORT ? "Ouvrir" : "Choisir le mode");
        completer.setOnAction(e -> afficherCompletion(r.fichier));
        droite.add(completer);

        return Ui.ligneElement(Ui.blocDate(r.debut), r.nom, r.descriptifCourt, droite.toArray(new Node[0]));
    }

    // ==================================================================
    // Formulaire de création d'un brouillon
    // ==================================================================

    private void afficherFormulaire() {
        poser(new FormulaireNouveauTournoi(contexte, this::afficherCompletion, this::afficherAccueil).build());
    }

    // ==================================================================
    // Écran de complétion
    // ==================================================================

    private void afficherCompletion(Path fichier) {
        com.pingmanager.model.ModePreparation mode = contexte.chargerTournoi(fichier).getModePreparation();
        if (mode == com.pingmanager.model.ModePreparation.A_CHOISIR) {
            racine.setCenter(new EcranChoixPreparation(contexte, fichier, () -> afficherCompletion(fichier),
                    this::afficherAccueil).build());
        } else if (mode == com.pingmanager.model.ModePreparation.IMPORT) {
            racine.setCenter(new EcranTournoiImporte(contexte, fichier, this::afficherAccueil,
                    () -> afficherCompletion(fichier)).build());
        } else {
            // L'écran de complétion gère son propre défilement : pas de ScrollPane imbriqué.
            racine.setCenter(new EcranCompletion(contexte, fichier, this::afficherAccueil).build());
        }
    }

    // ==================================================================
    // En-tête : club(s) gérés
    // ==================================================================

    private Label clubsGeres() {
        List<com.pingmanager.model.AccesClub> acces = contexte.accesCourants();
        StringBuilder sb = new StringBuilder(acces.size() > 1 ? "Clubs : " : "Club : ");
        for (int i = 0; i < acces.size(); i++) {
            String num = acces.get(i).getNumeroClub();
            String nom = contexte.clubParNumero(num)
                    .map(com.pingmanager.model.Club::getNom).orElse("Club " + num);
            sb.append(nom).append(" (").append(num).append(")");
            if (i < acces.size() - 1) {
                sb.append(", ");
            }
        }
        return Ui.discret(sb.toString());
    }

    // ==================================================================
    // Résumé léger d'un tournoi pour l'affichage
    // ==================================================================

    private record Resume(String nom, StatutTournoi statut, String descriptifCourt,
                          String descriptifLong, Path fichier, com.pingmanager.model.ModePreparation mode,
                          boolean jourJEntame, LocalDate debut, int participants) {

        static Resume depuis(Tournoi t, Path fichier) {
            String nom = (t.getNom() == null || t.getNom().isBlank()) ? "Sans titre" : t.getNom();
            boolean entame = t.getStatut() != StatutTournoi.ANNULE && t.getTableaux().stream()
                    .anyMatch(x -> x.getStatut() != com.pingmanager.model.StatutTableau.INSCRIPTION);
            int participants = com.pingmanager.core.RemplissageTableaux.nombreDeParticipants(t);
            return new Resume(nom, t.getStatut(), court(t, participants), lng(t), fichier, t.getModePreparation(),
                    entame, t.getDateDebut(), participants);
        }

        private static String court(Tournoi t, int participants) {
            String date = t.getDateDebut() != null ? t.getDateDebut().format(JOUR) : "dates à définir";
            int nb = t.getTableaux().size();
            String s = date + " · " + nb + (nb > 1 ? " tableaux" : " tableau");
            if (participants > 0) {
                s += " · " + participants + (participants > 1 ? " joueurs" : " joueur");
            }
            return s;
        }

        private static String lng(Tournoi t) {
            StringBuilder sb = new StringBuilder();
            int nbJours = t.getJours().size();
            sb.append("Jour 1 / ").append(Math.max(1, nbJours));
            if (t.getDateDebut() != null) {
                sb.append("  ·  ").append(t.getDateDebut().format(JOUR));
            }
            sb.append("  ·  ").append(t.getNombreDeTables()).append(" tables");
            return sb.toString();
        }
    }
}
