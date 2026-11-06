package com.pingmanager.core;

import com.pingmanager.model.AdresseTournoi;
import com.pingmanager.model.AgendaCompte;
import com.pingmanager.model.EvenementAgenda;
import com.pingmanager.model.OrigineEvenement;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Règles de l'agenda personnel du compte (cahier des charges v0, sections 1 et 3) :
 * dates personnelles (titre, date, heure facultative, note), et dates de tournoi
 * <b>tenues à jour automatiquement</b> d'après les inscriptions du joueur.
 *
 * <p>Un tournoi est présent dans l'agenda tant que la licence y est inscrite ou en
 * liste d'attente sur au moins un tableau ; il est retiré à la désinscription
 * complète. Ces dates automatiques ne se modifient ni ne se retirent à la main.</p>
 */
public final class ServiceAgenda {

    public static final int LONGUEUR_MAX_TITRE = 120;
    public static final int LONGUEUR_MAX_NOTE = 2000;

    /** Préfixe de l'identifiant (et de l'UID iCalendar) des dates de tournoi. */
    static final String PREFIXE_TOURNOI = "tournoi-";

    private final Supplier<String> generateurId;

    /** @param generateurId fournit l'identifiant des nouvelles dates personnelles (unique). */
    public ServiceAgenda(Supplier<String> generateurId) {
        this.generateurId = Objects.requireNonNull(generateurId, "generateurId");
    }

    /** Service dont les identifiants sont des UUID aléatoires. */
    public static ServiceAgenda parDefaut() {
        return new ServiceAgenda(() -> UUID.randomUUID().toString());
    }

    // ==================================================================
    // Dates personnelles
    // ==================================================================

    /** Ajoute une date personnelle d'un jour. */
    public ResultatAgenda ajouterDate(AgendaCompte agenda, String titre, LocalDate date, LocalTime heure, String note) {
        return ajouterDate(agenda, titre, date, null, heure, note);
    }

    /** Ajoute une date personnelle (sur plusieurs jours si {@code dateFin} est renseignée). */
    public ResultatAgenda ajouterDate(AgendaCompte agenda, String titre, LocalDate date, LocalDate dateFin,
                                      LocalTime heure, String note) {
        Objects.requireNonNull(agenda, "agenda");
        ResultatAgenda refus = verifierSaisie(titre, date, dateFin, note);
        if (refus != null) {
            return refus;
        }
        EvenementAgenda e = EvenementAgenda.builder().id(generateurId.get()).titre(titre).date(date)
                .dateFin(dateFin).heure(heure).note(note).origine(OrigineEvenement.PERSONNEL).construire();
        agenda.enregistrer(e);
        return new ResultatAgenda(ResultatAgenda.Motif.AJOUTE, e);
    }

    /** Modifie une date personnelle (titre, dates, heure, note). */
    public ResultatAgenda modifierDate(AgendaCompte agenda, String id, String titre, LocalDate date,
                                       LocalDate dateFin, LocalTime heure, String note) {
        Objects.requireNonNull(agenda, "agenda");
        Optional<EvenementAgenda> existante = agenda.trouver(id);
        if (existante.isEmpty()) {
            return new ResultatAgenda(ResultatAgenda.Motif.INTROUVABLE, null);
        }
        if (existante.get().estAutomatique()) {
            return new ResultatAgenda(ResultatAgenda.Motif.DATE_AUTOMATIQUE, existante.get());
        }
        ResultatAgenda refus = verifierSaisie(titre, date, dateFin, note);
        if (refus != null) {
            return refus;
        }
        EvenementAgenda e = existante.get().copie().titre(titre).date(date).dateFin(dateFin).heure(heure)
                .note(note).construire();
        agenda.enregistrer(e);
        return new ResultatAgenda(ResultatAgenda.Motif.MODIFIE, e);
    }

    /** Retire une date personnelle ; une date de tournoi est refusée (elle suit l'inscription). */
    public ResultatAgenda retirerDate(AgendaCompte agenda, String id) {
        Objects.requireNonNull(agenda, "agenda");
        Optional<EvenementAgenda> existante = agenda.trouver(id);
        if (existante.isEmpty()) {
            return new ResultatAgenda(ResultatAgenda.Motif.INTROUVABLE, null);
        }
        if (existante.get().estAutomatique()) {
            return new ResultatAgenda(ResultatAgenda.Motif.DATE_AUTOMATIQUE, existante.get());
        }
        agenda.retirer(id);
        return new ResultatAgenda(ResultatAgenda.Motif.RETIRE, existante.get());
    }

    private static ResultatAgenda verifierSaisie(String titre, LocalDate date, LocalDate dateFin, String note) {
        if (titre == null || titre.isBlank()) {
            return new ResultatAgenda(ResultatAgenda.Motif.TITRE_MANQUANT, null);
        }
        if (titre.trim().length() > LONGUEUR_MAX_TITRE) {
            return new ResultatAgenda(ResultatAgenda.Motif.TITRE_TROP_LONG, null);
        }
        if (note != null && note.trim().length() > LONGUEUR_MAX_NOTE) {
            return new ResultatAgenda(ResultatAgenda.Motif.NOTE_TROP_LONGUE, null);
        }
        if (date == null) {
            return new ResultatAgenda(ResultatAgenda.Motif.DATE_MANQUANTE, null);
        }
        if (dateFin != null && dateFin.isBefore(date)) {
            return new ResultatAgenda(ResultatAgenda.Motif.FIN_AVANT_DEBUT, null);
        }
        return null;
    }

    // ==================================================================
    // Tournois (ajout et retrait automatiques)
    // ==================================================================

    /**
     * Clé d'un tournoi dans l'agenda : son numéro d'homologation s'il en a un,
     * sinon nom, premier jour et club organisateur. Le modèle n'a pas encore
     * d'identifiant stable de tournoi ; l'appelant peut fournir sa propre clé
     * (chemin du fichier, par exemple) avec {@link #synchroniserTournoi(AgendaCompte, Tournoi, String, String)}.
     */
    public static String cleTournoi(Tournoi t) {
        Objects.requireNonNull(t, "tournoi");
        String homologation = t.getNumeroHomologation();
        if (homologation != null && !homologation.isBlank()) {
            return "homologation:" + homologation.trim();
        }
        return "tournoi:" + t.getNom().trim() + "|" + premierJour(t) + "|"
                + (t.getClubOrganisateur() == null ? "" : t.getClubOrganisateur().trim());
    }

    /** Comme {@link #synchroniserTournoi(AgendaCompte, Tournoi, String, String)} avec la clé par défaut. */
    public ResultatAgenda synchroniserTournoi(AgendaCompte agenda, Tournoi t, String licence) {
        return synchroniserTournoi(agenda, t, cleTournoi(t), licence);
    }

    /**
     * Met l'agenda en accord avec la participation de la licence à ce tournoi :
     * ajoute (ou met à jour) la date si elle est inscrite ou en liste d'attente sur
     * au moins un tableau, la retire sinon (désinscription complète).
     *
     * @return {@code AJOUTE}, {@code MODIFIE}, {@code RETIRE} ou {@code INCHANGE}
     */
    public ResultatAgenda synchroniserTournoi(AgendaCompte agenda, Tournoi t, String cle, String licence) {
        Objects.requireNonNull(agenda, "agenda");
        Objects.requireNonNull(t, "tournoi");
        Objects.requireNonNull(cle, "cle");
        Optional<EvenementAgenda> existante = agenda.dateDuTournoi(cle);
        Optional<RemplissageTableaux.Participant> p = licence == null ? Optional.empty()
                : RemplissageTableaux.participant(t, licence);
        if (p.isEmpty()) {
            if (existante.isEmpty()) {
                return new ResultatAgenda(ResultatAgenda.Motif.INCHANGE, null);
            }
            agenda.retirer(existante.get().getId());
            return new ResultatAgenda(ResultatAgenda.Motif.RETIRE, existante.get());
        }
        EvenementAgenda voulue = dateDuTournoi(t, cle, p.get(),
                existante.map(EvenementAgenda::getId).orElse(identifiantTournoi(cle)));
        if (existante.isPresent() && existante.get().equals(voulue)) {
            return new ResultatAgenda(ResultatAgenda.Motif.INCHANGE, voulue);
        }
        agenda.enregistrer(voulue);
        return new ResultatAgenda(existante.isPresent() ? ResultatAgenda.Motif.MODIFIE : ResultatAgenda.Motif.AJOUTE,
                voulue);
    }

    /** Identifiant stable (indépendant de la session) de la date d'un tournoi. */
    static String identifiantTournoi(String cle) {
        return PREFIXE_TOURNOI + UUID.nameUUIDFromBytes(cle.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static EvenementAgenda dateDuTournoi(Tournoi t, String cle, RemplissageTableaux.Participant p, String id) {
        LocalDate debut = premierJour(t);
        LocalDate fin = t.getDateFin() != null ? t.getDateFin() : t.getJours().last();
        if (fin.isBefore(debut)) {
            fin = debut;
        }
        LocalTime heure = null;
        if (fin.equals(debut)) {
            heure = premierPointage(p, debut);
        }
        return EvenementAgenda.builder().id(id).titre(t.getNom()).date(debut).dateFin(fin).heure(heure)
                .lieu(lieu(t)).note(note(t, p)).origine(OrigineEvenement.TOURNOI).cleTournoi(cle).construire();
    }

    private static LocalDate premierJour(Tournoi t) {
        return t.getDateDebut() != null ? t.getDateDebut() : t.getDate();
    }

    /** Heure du premier pointage parmi les tableaux du joueur, ce jour-là (ou {@code null}). */
    private static LocalTime premierPointage(RemplissageTableaux.Participant p, LocalDate jour) {
        LocalDateTime min = null;
        List<Tableau> tous = new ArrayList<>(p.inscrit());
        tous.addAll(p.enAttente());
        for (Tableau tab : tous) {
            LocalDateTime d = tab.getHoraires() == null ? null : tab.getHoraires().getDebutPointage();
            if (d != null && d.toLocalDate().equals(jour) && (min == null || d.isBefore(min))) {
                min = d;
            }
        }
        return min == null ? null : min.toLocalTime();
    }

    private static String lieu(Tournoi t) {
        AdresseTournoi a = t.getAdresse();
        if (a == null) {
            return null;
        }
        String texte = a.toString();
        return texte.isBlank() ? null : texte;
    }

    private static String note(Tournoi t, RemplissageTableaux.Participant p) {
        StringBuilder sb = new StringBuilder();
        if (!p.inscrit().isEmpty()) {
            sb.append(p.inscrit().size() > 1 ? "Tableaux : " : "Tableau : ").append(noms(p.inscrit()));
        }
        if (!p.enAttente().isEmpty()) {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append("Liste d'attente : ").append(noms(p.enAttente()));
        }
        if (t.getClubOrganisateur() != null && !t.getClubOrganisateur().isBlank()) {
            sb.append('\n').append("Organisé par ").append(t.getClubOrganisateur().trim());
        }
        return sb.toString();
    }

    private static String noms(List<Tableau> tableaux) {
        List<String> n = new ArrayList<>();
        for (Tableau tab : tableaux) {
            n.add(tab.getNom());
        }
        return String.join(", ", n);
    }
}
