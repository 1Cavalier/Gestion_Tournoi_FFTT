package com.pingmanager.ui.ecran;

import com.pingmanager.core.RemplissageTableaux;
import com.pingmanager.model.ModePreparation;
import com.pingmanager.model.StatutTableau;
import com.pingmanager.model.StatutTournoi;
import com.pingmanager.model.Tournoi;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Présentation de l'accueil « Gestion des tournois » (sans JavaFX), cahier des
 * charges v0, section 4 : le <b>tournoi actuel</b> en haut, puis une <b>frise
 * verticale</b> des étapes du cycle de vie (4 Publié, 3 Homologué validé,
 * 2 Demande d'homologation en cours, 1 Brouillon), un gros bouton de création
 * et l'accès à l'<b>historique</b> des tournois passés.
 *
 * <p>Les étapes reprennent telles quelles les valeurs de {@link StatutTournoi}
 * (aucune règle métier n'est refaite ici) ; la vue {@link AccueilGestionTournois}
 * ne fait qu'afficher ce qui est décidé ici.</p>
 */
final class PresentationGestionTournois {

    static final String TITRE = "Gestion des tournois";
    static final String BOUTON_CREER = "Créer un tournoi";
    static final String AIDE_CREER = "Un brouillon en quelques champs : nom, club, niveau et dates.";
    static final String LIEN_HISTORIQUE = "Historique des tournois passés";
    static final String TITRE_ACTUEL = "Tournoi actuel";
    static final String SURTITRE_EN_COURS = "TOURNOI EN COURS";
    static final String SURTITRE_PROCHAIN = "PROCHAIN TOURNOI";
    static final String ACTUEL_VIDE = "Aucun tournoi en cours pour le moment. Dès qu'un tournoi est "
            + "publié, il apparaît ici avec l'accès au jour J.";
    static final String BOUTON_JOUR_J = "Ouvrir le jour J";
    static final String TITRE_FRISE = "Vos tournois, étape par étape";
    static final String AIDE_FRISE = "Un tournoi monte d'une étape à chaque validation : "
            + "brouillon, demande d'homologation, homologation, publication.";
    static final String ETAPE_VIDE = "Aucun tournoi à cette étape.";
    static final String PASTILLE_ACTUEL = "Tournoi actuel";
    static final String PASTILLE_JOUR_J = "Jour J commencé";
    static final String PASTILLE_IMPORTE = "Tournoi importé";
    static final String PASTILLE_MODE = "Mode à choisir";

    static final String TITRE_HISTORIQUE = "Historique des tournois";
    static final String AIDE_HISTORIQUE = "Les tournois du club dont la date est passée, et les tournois annulés.";
    static final String HISTORIQUE_VIDE = "Aucun tournoi passé pour l'instant.";
    static final String RETOUR = "← Gestion des tournois";

    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH);

    /** Résumé léger d'un tournoi, lu une fois depuis son fichier. */
    record Fiche(String nom, StatutTournoi statut, ModePreparation mode, boolean jourJEntame,
                 LocalDate debut, LocalDate fin, int nbTableaux, int participants, int nbJours,
                 int nbTables, Path fichier) {

        static Fiche depuis(Tournoi t, Path fichier) {
            String nom = (t.getNom() == null || t.getNom().isBlank()) ? "Sans titre" : t.getNom();
            boolean entame = t.getStatut() != StatutTournoi.ANNULE && t.getTableaux().stream()
                    .anyMatch(x -> x.getStatut() != StatutTableau.INSCRIPTION);
            return new Fiche(nom, t.getStatut(), t.getModePreparation(), entame, t.getDateDebut(),
                    t.getDateFin(), t.getTableaux().size(), RemplissageTableaux.nombreDeParticipants(t),
                    t.getJours().size(), t.getNombreDeTables(), fichier);
        }

        /** Dernier jour connu du tournoi (fin, sinon début), ou {@code null}. */
        LocalDate dernierJour() {
            return fin != null ? fin : debut;
        }
    }

    /** Une étape de la frise, avec ses tournois triés par date. */
    record Etape(int numero, String titre, String aide, StatutTournoi statut, List<Fiche> tournois) {
        boolean estVide() {
            return tournois.isEmpty();
        }

        String libelle() {
            return "Étape " + numero;
        }
    }

    /** Tournoi mis en avant en haut de page, et pourquoi. */
    record Actuel(Fiche fiche, boolean enCours) {
        String surtitre() {
            return enCours ? SURTITRE_EN_COURS : SURTITRE_PROCHAIN;
        }
    }

    private static final Comparator<Fiche> PAR_DATE = Comparator
            .comparing(Fiche::debut, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(Fiche::nom, String.CASE_INSENSITIVE_ORDER);

    private final List<Fiche> fiches;
    private final LocalDate aujourdhui;

    PresentationGestionTournois(List<Fiche> fiches, LocalDate aujourdhui) {
        this.fiches = List.copyOf(fiches);
        this.aujourdhui = aujourdhui;
    }

    /**
     * Un tournoi est « passé » s'il est annulé, ou s'il est homologué / publié et
     * que son dernier jour est derrière nous. Un brouillon ou une demande
     * d'homologation restent dans la frise même si leur date est dépassée :
     * le club doit encore les corriger ou les abandonner.
     */
    boolean estPasse(Fiche f) {
        if (f.statut() == StatutTournoi.ANNULE) {
            return true;
        }
        if (f.statut() != StatutTournoi.PUBLIE && f.statut() != StatutTournoi.HOMOLOGUE) {
            return false;
        }
        LocalDate dernier = f.dernierJour();
        return dernier != null && dernier.isBefore(aujourdhui);
    }

    /**
     * Le tournoi actuel : d'abord celui dont le jour J a commencé, sinon un
     * tournoi publié qui se déroule aujourd'hui, sinon le prochain tournoi
     * publié à venir (pour garder l'accès au jour J). Vide sinon.
     */
    Optional<Actuel> actuel() {
        List<Fiche> candidats = new ArrayList<>();
        for (Fiche f : fiches) {
            if (!estPasse(f)) {
                candidats.add(f);
            }
        }
        candidats.sort(PAR_DATE);
        for (Fiche f : candidats) {
            if (f.jourJEntame()) {
                return Optional.of(new Actuel(f, true));
            }
        }
        for (Fiche f : candidats) {
            if (f.statut() == StatutTournoi.PUBLIE && seDerouleAujourdhui(f)) {
                return Optional.of(new Actuel(f, true));
            }
        }
        for (Fiche f : candidats) {
            if (f.statut() == StatutTournoi.PUBLIE) {
                return Optional.of(new Actuel(f, false));
            }
        }
        return Optional.empty();
    }

    private boolean seDerouleAujourdhui(Fiche f) {
        return f.debut() != null && !f.debut().isAfter(aujourdhui)
                && !f.dernierJour().isBefore(aujourdhui);
    }

    /** La frise, de l'étape 4 (en haut) à l'étape 1 (en bas). */
    List<Etape> frise() {
        return List.of(
                etape(4, "Publié", "Inscriptions ouvertes, en attente du jour J.", StatutTournoi.PUBLIE),
                etape(3, "Homologué validé", "Numéro d'homologation reçu, prêt à être publié.",
                        StatutTournoi.HOMOLOGUE),
                etape(2, "Demande d'homologation en cours",
                        "Dossier complet envoyé, en attente du numéro d'homologation.",
                        StatutTournoi.DEMANDE_HOMOLOGATION),
                etape(1, "Brouillon", "En préparation, pas encore envoyé à l'homologation.",
                        StatutTournoi.BROUILLON));
    }

    private Etape etape(int numero, String titre, String aide, StatutTournoi statut) {
        List<Fiche> liste = new ArrayList<>();
        for (Fiche f : fiches) {
            if (f.statut() == statut && !estPasse(f)) {
                liste.add(f);
            }
        }
        liste.sort(PAR_DATE);
        return new Etape(numero, titre, aide, statut, List.copyOf(liste));
    }

    /** Tournois passés, du plus récent au plus ancien (sans date en dernier). */
    List<Fiche> historique() {
        List<Fiche> liste = new ArrayList<>();
        for (Fiche f : fiches) {
            if (estPasse(f)) {
                liste.add(f);
            }
        }
        liste.sort(Comparator.comparing(Fiche::debut, Comparator.nullsLast(Comparator.<LocalDate>reverseOrder()))
                .thenComparing(Fiche::nom, String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(liste);
    }

    /** Libellé du lien d'historique, avec le nombre de tournois passés. */
    String lienHistorique() {
        int n = historique().size();
        return n == 0 ? LIEN_HISTORIQUE : LIEN_HISTORIQUE + " (" + n + ")";
    }

    boolean estActuel(Fiche f) {
        return actuel().map(a -> a.fiche().fichier().equals(f.fichier())).orElse(false);
    }

    // ------------------------------------------------------------------
    // Libellés d'une ligne
    // ------------------------------------------------------------------

    /** Action d'ouverture selon le mode de préparation. */
    static String libelleAction(Fiche f) {
        if (f.mode() == ModePreparation.IMPORT) {
            return "Ouvrir";
        }
        if (f.mode() == ModePreparation.A_CHOISIR) {
            return "Choisir le mode";
        }
        return f.statut() == StatutTournoi.BROUILLON ? "Compléter" : "Ouvrir";
    }

    /** Pastille de mode (importé / à choisir), ou {@code null}. */
    static String pastilleMode(Fiche f) {
        if (f.mode() == ModePreparation.IMPORT) {
            return PASTILLE_IMPORTE;
        }
        if (f.mode() == ModePreparation.A_CHOISIR) {
            return PASTILLE_MODE;
        }
        return null;
    }

    /** « 10 avril 2027 · 8 tableaux · 120 joueurs ». */
    static String detailCourt(Fiche f) {
        String date = f.debut() != null ? f.debut().format(JOUR) : "dates à définir";
        String s = date + " · " + f.nbTableaux() + (f.nbTableaux() > 1 ? " tableaux" : " tableau");
        if (f.participants() > 0) {
            s += " · " + f.participants() + (f.participants() > 1 ? " joueurs" : " joueur");
        }
        return s;
    }

    /** « 2 jours · 10 avril 2027 · 24 tables · 120 joueurs » pour le tournoi actuel. */
    static String detailActuel(Fiche f) {
        StringBuilder sb = new StringBuilder();
        int jours = Math.max(1, f.nbJours());
        sb.append(jours).append(jours > 1 ? " jours" : " jour");
        if (f.debut() != null) {
            sb.append("  ·  ").append(f.debut().format(JOUR));
        }
        sb.append("  ·  ").append(f.nbTables()).append(f.nbTables() > 1 ? " tables" : " table");
        if (f.participants() > 0) {
            sb.append("  ·  ").append(f.participants()).append(f.participants() > 1 ? " joueurs" : " joueur");
        }
        return sb.toString();
    }

    /** Pastille d'un tournoi de l'historique. */
    static String libelleHistorique(Fiche f) {
        return f.statut() == StatutTournoi.ANNULE ? "Annulé" : "Terminé";
    }
}
