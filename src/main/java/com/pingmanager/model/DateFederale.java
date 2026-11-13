package com.pingmanager.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Une date repère du calendrier fédéral de la saison (B045) : tour de Critérium
 * fédéral, journée de championnat par équipes, date administrative… Ce n'est
 * qu'un <b>repère d'agenda</b> : aucune règle sportive n'y est attachée.
 *
 * <p>Chaque date porte sa source (adresse), sa date de relecture et son statut
 * ({@link StatutDateFederale}). Objet immuable construit via son {@link Builder} ;
 * son identifiant est stable d'une version du fichier à l'autre, pour reconnaître
 * une date déjà copiée dans l'agenda.</p>
 */
public final class DateFederale {

    private final String id;
    private final String categorie;
    private final String titre;
    private final LocalDate date;
    private final LocalDate dateFin;
    private final StatutDateFederale statut;
    private final String source;
    private final LocalDate reluLe;
    private final String note;

    private DateFederale(Builder b) {
        this.id = exiger(b.id, "id");
        this.categorie = exiger(b.categorie, "categorie");
        this.titre = exiger(b.titre, "titre");
        this.date = Objects.requireNonNull(b.date, "date");
        if (b.dateFin != null && b.dateFin.isBefore(b.date)) {
            throw new IllegalArgumentException("La date de fin ne peut pas précéder la date de début.");
        }
        this.dateFin = b.dateFin == null || b.dateFin.equals(b.date) ? null : b.dateFin;
        this.statut = Objects.requireNonNull(b.statut, "statut");
        this.source = exiger(b.source, "source");
        this.reluLe = Objects.requireNonNull(b.reluLe, "reluLe");
        this.note = b.note == null || b.note.isBlank() ? null : b.note.trim();
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getId() {
        return id;
    }

    /** @return la catégorie (« Critérium fédéral », « Championnat par équipes »…). */
    public String getCategorie() {
        return categorie;
    }

    public String getTitre() {
        return titre;
    }

    public LocalDate getDate() {
        return date;
    }

    /** @return le dernier jour (égal à {@link #getDate()} pour une date d'un jour). */
    public LocalDate getDateFin() {
        return dateFin == null ? date : dateFin;
    }

    public boolean estSurPlusieursJours() {
        return dateFin != null;
    }

    public StatutDateFederale getStatut() {
        return statut;
    }

    public boolean estAVerifier() {
        return statut == StatutDateFederale.A_VERIFIER;
    }

    /** @return l'adresse de la source officielle (ou la plus proche) de cette date. */
    public String getSource() {
        return source;
    }

    /** @return le jour où la source a été relue. */
    public LocalDate getReluLe() {
        return reluLe;
    }

    public String getNote() {
        return note;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DateFederale)) {
            return false;
        }
        DateFederale d = (DateFederale) o;
        return id.equals(d.id) && categorie.equals(d.categorie) && titre.equals(d.titre) && date.equals(d.date)
                && Objects.equals(dateFin, d.dateFin) && statut == d.statut && source.equals(d.source)
                && reluLe.equals(d.reluLe) && Objects.equals(note, d.note);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, categorie, titre, date, dateFin, statut, source, reluLe, note);
    }

    @Override
    public String toString() {
        return date + " " + titre + " (" + statut.libelle() + ")";
    }

    private static String exiger(String v, String nom) {
        if (v == null || v.isBlank()) {
            throw new IllegalArgumentException(nom + " obligatoire");
        }
        return v.trim();
    }

    /** Construction pas à pas d'une {@link DateFederale}. */
    public static final class Builder {
        private String id;
        private String categorie;
        private String titre;
        private LocalDate date;
        private LocalDate dateFin;
        private StatutDateFederale statut;
        private String source;
        private LocalDate reluLe;
        private String note;

        private Builder() {
        }

        public Builder id(String v) {
            this.id = v;
            return this;
        }

        public Builder categorie(String v) {
            this.categorie = v;
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

        public Builder statut(StatutDateFederale v) {
            this.statut = v;
            return this;
        }

        public Builder source(String v) {
            this.source = v;
            return this;
        }

        public Builder reluLe(LocalDate v) {
            this.reluLe = v;
            return this;
        }

        public Builder note(String v) {
            this.note = v;
            return this;
        }

        public DateFederale construire() {
            return new DateFederale(this);
        }
    }
}
