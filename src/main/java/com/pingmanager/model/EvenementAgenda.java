package com.pingmanager.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

/**
 * Une date de l'agenda du compte : titre, date (ou période de plusieurs jours),
 * heure facultative, lieu et note facultatifs.
 *
 * <p>Objet immuable construit via son {@link Builder}. Son identifiant est
 * stable : il sert d'identité dans l'agenda et d'{@code UID} à l'export
 * iCalendar, pour qu'un agenda importé deux fois ne crée pas de doublons.</p>
 *
 * <p>Une date d'origine {@link OrigineEvenement#TOURNOI} porte la clé du
 * tournoi concerné ({@link #getCleTournoi()}) ; elle est tenue à jour par le
 * service d'agenda, pas saisie.</p>
 */
public final class EvenementAgenda {

    private final String id;
    private final String titre;
    private final LocalDate date;
    private final LocalDate dateFin;
    private final LocalTime heure;
    private final String lieu;
    private final String note;
    private final OrigineEvenement origine;
    private final String cleTournoi;

    private EvenementAgenda(Builder b) {
        this.id = exigerNonVide(b.id, "id");
        this.titre = exigerNonVide(b.titre, "titre");
        this.date = Objects.requireNonNull(b.date, "date");
        if (b.dateFin != null && b.dateFin.isBefore(b.date)) {
            throw new IllegalArgumentException("La date de fin ne peut pas précéder la date de début.");
        }
        this.dateFin = b.dateFin == null || b.dateFin.equals(b.date) ? null : b.dateFin;
        this.heure = b.heure;
        this.lieu = videEnNull(b.lieu);
        this.note = videEnNull(b.note);
        this.origine = b.origine == null ? OrigineEvenement.PERSONNEL : b.origine;
        this.cleTournoi = videEnNull(b.cleTournoi);
        if (this.origine == OrigineEvenement.TOURNOI && this.cleTournoi == null) {
            throw new IllegalArgumentException("Une date de tournoi doit porter la clé du tournoi.");
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    /** @return un builder prérempli avec les valeurs de cette date (pour une modification). */
    public Builder copie() {
        return new Builder().id(id).titre(titre).date(date).dateFin(dateFin).heure(heure)
                .lieu(lieu).note(note).origine(origine).cleTournoi(cleTournoi);
    }

    public String getId() {
        return id;
    }

    public String getTitre() {
        return titre;
    }

    /** @return le premier jour. */
    public LocalDate getDate() {
        return date;
    }

    /** @return le dernier jour (égal à {@link #getDate()} pour une date d'un jour). */
    public LocalDate getDateFin() {
        return dateFin == null ? date : dateFin;
    }

    /** @return vrai si la date couvre plusieurs jours. */
    public boolean estSurPlusieursJours() {
        return dateFin != null;
    }

    /** @return l'heure, ou {@code null} pour une date « toute la journée ». */
    public LocalTime getHeure() {
        return heure;
    }

    public String getLieu() {
        return lieu;
    }

    public String getNote() {
        return note;
    }

    public OrigineEvenement getOrigine() {
        return origine;
    }

    /** @return vrai pour une date ajoutée automatiquement (tournoi). */
    public boolean estAutomatique() {
        return origine == OrigineEvenement.TOURNOI;
    }

    /** @return la clé du tournoi, ou {@code null} pour une date personnelle. */
    public String getCleTournoi() {
        return cleTournoi;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EvenementAgenda)) {
            return false;
        }
        EvenementAgenda e = (EvenementAgenda) o;
        return id.equals(e.id) && titre.equals(e.titre) && date.equals(e.date)
                && Objects.equals(dateFin, e.dateFin) && Objects.equals(heure, e.heure)
                && Objects.equals(lieu, e.lieu) && Objects.equals(note, e.note)
                && origine == e.origine && Objects.equals(cleTournoi, e.cleTournoi);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, titre, date, dateFin, heure, lieu, note, origine, cleTournoi);
    }

    @Override
    public String toString() {
        return date + (heure == null ? "" : " " + heure) + " " + titre;
    }

    private static String exigerNonVide(String v, String nom) {
        if (v == null || v.isBlank()) {
            throw new IllegalArgumentException(nom + " obligatoire");
        }
        return v.trim();
    }

    private static String videEnNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }

    /** Construction pas à pas d'une {@link EvenementAgenda}. */
    public static final class Builder {
        private String id;
        private String titre;
        private LocalDate date;
        private LocalDate dateFin;
        private LocalTime heure;
        private String lieu;
        private String note;
        private OrigineEvenement origine = OrigineEvenement.PERSONNEL;
        private String cleTournoi;

        private Builder() {
        }

        public Builder id(String v) {
            this.id = v;
            return this;
        }

        public Builder titre(String v) {
            this.titre = v;
            return this;
        }

        public Builder date(LocalDate v) {
            this.date = v;
            return this;
        }

        public Builder dateFin(LocalDate v) {
            this.dateFin = v;
            return this;
        }

        public Builder heure(LocalTime v) {
            this.heure = v;
            return this;
        }

        public Builder lieu(String v) {
            this.lieu = v;
            return this;
        }

        public Builder note(String v) {
            this.note = v;
            return this;
        }

        public Builder origine(OrigineEvenement v) {
            this.origine = v;
            return this;
        }

        public Builder cleTournoi(String v) {
            this.cleTournoi = v;
            return this;
        }

        public EvenementAgenda construire() {
            return new EvenementAgenda(this);
        }
    }
}
