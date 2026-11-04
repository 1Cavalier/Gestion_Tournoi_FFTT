package com.pingmanager.ui.ecran;

import com.pingmanager.core.ServiceCompletudeTournoi.Niveau;
import com.pingmanager.model.Club;
import com.pingmanager.model.NiveauTournoi;
import com.pingmanager.model.StatutTournoi;
import com.pingmanager.model.Tournoi;
import com.pingmanager.persistence.ProjetTournoi;
import com.pingmanager.ui.ContexteApplication;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
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
    private static final DateTimeFormatter SAISIE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

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
        racine.setCenter(Ui.defilant(contenu));
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

        VBox contenu = Ui.page(820, barre);

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
        Resume enCours = tournois.stream()
                .filter(r -> r.statut == StatutTournoi.PUBLIE)
                .findFirst().orElse(null);

        if (enCours == null) {
            return Ui.carte("Tournoi en cours",
                    Ui.discret("Aucun tournoi en cours pour le moment. Les tournois publiés, "
                            + "prêts pour le jour J, apparaîtront ici."));
        }
        Label nom = Ui.gras(enCours.nom);
        Label meta = Ui.discret(enCours.descriptifLong);
        Button suivre = Ui.boutonPrimaire("Suivre le tournoi");
        suivre.setDisable(true); // branché à l'étape « déroulé »
        VBox infos = new VBox(4, nom, meta);
        HBox.setHgrow(infos, Priority.ALWAYS);
        return Ui.carte("Tournoi en cours", Ui.ligne(16, infos, suivre));
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
            corps.getChildren().add(Ui.discret("Aucun pour l'instant."));
        } else {
            for (Resume r : liste) {
                corps.getChildren().add(ligneTournoi(r));
            }
        }
        VBox entete = new VBox(2, Ui.titreAvecCompteur(titre, liste.size()), Ui.aide(capitaliser(aide)));
        return Ui.carte(entete, corps);
    }

    private static String capitaliser(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1) + ".";
    }

    private Node ligneTournoi(Resume r) {
        VBox infos = new VBox(2, Ui.gras(r.nom), Ui.discret(r.descriptifCourt));
        if (r.mode != com.pingmanager.model.ModePreparation.COMPLETE) {
            Label m = r.mode == com.pingmanager.model.ModePreparation.IMPORT
                    ? Ui.pastille("Tournoi importé", Ui.ETAT_RECOMMANDE) : Ui.pastille("Mode à choisir", Ui.ETAT_ATTENTION);
            infos.getChildren().set(0, Ui.ligne(8, Ui.gras(r.nom), m));
        }

        Button completer = Ui.boutonSecondaire(r.mode == com.pingmanager.model.ModePreparation.COMPLETE
                ? "Compléter" : r.mode == com.pingmanager.model.ModePreparation.IMPORT ? "Ouvrir" : "Choisir le mode");
        completer.setOnAction(e -> afficherCompletion(r.fichier));

        return Ui.ligneListe(infos, Ui.espace(), completer);
    }

    // ==================================================================
    // Formulaire de création d'un brouillon
    // ==================================================================

    private void afficherFormulaire() {
        Niveau H = Niveau.OBLIGATOIRE_HOMOLOGATION;
        List<Club> clubs = contexte.clubsGeres();
        Club[] club = {clubs.isEmpty() ? null : clubs.get(0)};

        // Club organisateur : repris de la base FFTT (choix si plusieurs clubs gérés).
        TextField clubLecture = Ui.champLecture(libelleClub(club[0]));
        ComboBox<Club> choixClub = new ComboBox<>();
        choixClub.getItems().addAll(clubs);
        choixClub.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Club c) {
                return libelleClub(c);
            }

            @Override
            public Club fromString(String s) {
                return null;
            }
        });
        choixClub.setValue(club[0]);

        // Nom : libre, avec une proposition « Tournoi de/du… » tant qu'on n'y touche pas.
        TextField nom = Ui.champ(null);
        nom.setText(PropositionsTournoi.nomTournoi(club[0] == null ? null : club[0].getNom()));
        boolean[] nomModifie = {false};
        nom.setOnKeyTyped(e -> nomModifie[0] = true);
        choixClub.valueProperty().addListener((o, a, n) -> {
            club[0] = n;
            if (!nomModifie[0]) {
                nom.setText(PropositionsTournoi.nomTournoi(n == null ? null : n.getNom()));
            }
        });

        ComboBox<NiveauTournoi> niveau = Ui.combo(NiveauTournoi.values(), null);

        TextField debut = Ui.champ("jj/mm/aaaa");
        TextField fin = Ui.champ("jj/mm/aaaa");

        Label erreur = Ui.message();

        Button creer = Ui.boutonPrimaire("Créer le brouillon");
        creer.setDefaultButton(true);
        creer.setOnAction(e -> {
            cacher(erreur);
            String n = texte(nom);
            if (n.isBlank()) {
                Ui.marquerErreur(nom);
                montrerErreur(erreur, "Donnez un nom au tournoi.");
                return;
            }
            if (club[0] == null) {
                montrerErreur(erreur, "Aucun club rattaché à votre compte : rattachez-en un "
                        + "depuis « Mon profil ».");
                return;
            }
            LocalDate d = parse(debut);
            if (d == null) {
                Ui.marquerErreur(debut);
                montrerErreur(erreur, "Indiquez la date de début (jj/mm/aaaa).");
                return;
            }
            LocalDate f = fin.getText() == null || fin.getText().isBlank() ? d : parse(fin);
            if (f == null) {
                Ui.marquerErreur(fin);
                montrerErreur(erreur, "La date de fin n'est pas valide (jj/mm/aaaa).");
                return;
            }
            if (f.isBefore(d)) {
                Ui.marquerErreur(fin);
                montrerErreur(erreur, "La date de fin ne peut pas précéder la date de début.");
                return;
            }
            Tournoi cree = construire(n, club[0], d, f, niveau.getValue());
            cree.setModePreparation(com.pingmanager.model.ModePreparation.A_CHOISIR);
            // Choix imposé juste après la création : préparation complète ou import.
            afficherCompletion(contexte.creerBrouillon(cree));
        });

        Button annuler = Ui.boutonSecondaire("Annuler");
        annuler.setCancelButton(true);
        annuler.setOnAction(e -> afficherAccueil());

        VBox blocDebut = Ui.champEtiquete("Date de début", debut, H);
        // À la création, une date de fin vide = tournoi d'un jour : facultatif ici.
        VBox blocFin = Ui.champEtiquete("Date de fin", fin, Niveau.OPTIONNEL);
        HBox dates = new HBox(16, blocDebut, blocFin);
        HBox.setHgrow(blocDebut, Priority.ALWAYS);
        HBox.setHgrow(blocFin, Priority.ALWAYS);

        Node champClub = clubs.size() > 1
                ? Ui.champEtiquete("Club organisateur", choixClub, H)
                : Ui.champEtiquete("Club organisateur", clubLecture, H);

        VBox champs = new VBox(14,
                champClub,
                Ui.champEtiquete("Nom du tournoi", nom, H),
                Ui.champEtiquete("Niveau du tournoi (catégorie)", niveau, H),
                dates,
                Ui.note("Du premier au dernier jour de l'ensemble du tournoi, toutes journées "
                        + "comprises. Pour un tournoi d'un jour, laissez la date de fin vide."));

        VBox contenu = Ui.page(640,
                new VBox(6, Ui.titre("Nouveau tournoi"),
                        Ui.discret("Juste l'essentiel pour démarrer. L'adresse du club et "
                                + "l'administrateur principal sont repris par défaut ; vous "
                                + "compléterez le reste ensuite.")),
                Ui.legende(EnumSet.of(H, Niveau.OPTIONNEL)),
                Ui.carteNue(champs),
                erreur,
                Ui.actions(creer, annuler));
        poser(contenu);
    }

    private static String libelleClub(Club c) {
        return c == null ? "" : c.getNom() + " (n° " + c.getNumero() + ")";
    }

    /**
     * Brouillon avec les valeurs par défaut de la base : club et n° d'affiliation,
     * adresse du club, et correspondant = administrateur principal. Tout reste
     * modifiable ensuite dans la section « Général ».
     */
    private Tournoi construire(String nom, Club club, LocalDate debut, LocalDate fin, NiveauTournoi niveau) {
        List<LocalDate> jours = new ArrayList<>();
        for (LocalDate d = debut; !d.isAfter(fin); d = d.plusDays(1)) {
            jours.add(d);
        }
        Tournoi tournoi = new Tournoi(nom.trim(), club.getNom(), jours);
        tournoi.setNumeroAffiliationClub(club.getNumero());
        tournoi.setNiveau(niveau);
        tournoi.setPeriode(debut, fin);
        tournoi.setAdresse(FormulairesSection.adresseGeocodee(FormulairesSection.adresse(
                club.getAdresse1(), club.getCodePostal(), club.getVille()), null));
        String[] cor = contexte.correspondantParDefaut(club.getNumero());
        tournoi.setCorrespondant(cor[0], cor[1], cor[2], null, null);
        return tournoi;
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
    // Utilitaires
    // ==================================================================

    private static String texte(TextField t) {
        return t.getText() == null ? "" : t.getText().trim();
    }

    private static LocalDate parse(TextField champ) {
        try {
            return LocalDate.parse(texte(champ), SAISIE);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static void montrerErreur(Label l, String m) {
        Ui.erreur(l, m);
    }

    private static void cacher(Node n) {
        Ui.cacher(n);
    }

    // ==================================================================
    // Résumé léger d'un tournoi pour l'affichage
    // ==================================================================

    private record Resume(String nom, StatutTournoi statut, String descriptifCourt,
                          String descriptifLong, Path fichier, com.pingmanager.model.ModePreparation mode) {

        static Resume depuis(Tournoi t, Path fichier) {
            String nom = (t.getNom() == null || t.getNom().isBlank()) ? "Sans titre" : t.getNom();
            return new Resume(nom, t.getStatut(), court(t), lng(t), fichier, t.getModePreparation());
        }

        private static String court(Tournoi t) {
            String date = t.getDateDebut() != null ? t.getDateDebut().format(JOUR) : "dates à définir";
            int nb = t.getTableaux().size();
            return date + " · " + nb + (nb > 1 ? " tableaux" : " tableau");
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
