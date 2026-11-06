package com.pingmanager.model;

import java.time.LocalDateTime;

/**
 * Calendrier d'un tableau : heures de pointage et de début des matchs.
 *
 * <p>Un tableau ouvre un <b>pointage</b> (les joueurs confirment leur présence)
 * entre {@link #getDebutPointage()} et {@link #getFinPointage()}, puis les
 * matchs démarrent à {@link #getDebutMatchs()} (typiquement quelques minutes
 * après la fin du pointage). Ces horaires servent à ordonner les tableaux dans
 * la journée et à savoir lesquels « se suivent ».</p>
 */
public final class HorairesTableau {

    private LocalDateTime debutPointage;
    private LocalDateTime finPointage;
    private LocalDateTime debutMatchs;
    /** Heure de fin théorique (prévision affichée ; facultative). */
    private LocalDateTime finTheorique;

    public HorairesTableau() {
    }

    public HorairesTableau(LocalDateTime debutPointage, LocalDateTime finPointage,
                           LocalDateTime debutMatchs, LocalDateTime finTheorique) {
        this(debutPointage, finPointage, debutMatchs);
        this.finTheorique = finTheorique;
    }

    public HorairesTableau(LocalDateTime debutPointage, LocalDateTime finPointage,
                           LocalDateTime debutMatchs) {
        this.debutPointage = debutPointage;
        this.finPointage = finPointage;
        this.debutMatchs = debutMatchs;
    }

    public LocalDateTime getDebutPointage() {
        return debutPointage;
    }

    public LocalDateTime getFinPointage() {
        return finPointage;
    }

    public LocalDateTime getDebutMatchs() {
        return debutMatchs;
    }

    public void setDebutMatchs(LocalDateTime debutMatchs) {
        this.debutMatchs = debutMatchs;
    }

    public LocalDateTime getFinTheorique() {
        return finTheorique;
    }

    /** @return le jour du tableau (déduit du début des matchs), ou {@code null}. */
    public java.time.LocalDate getJour() {
        if (debutMatchs != null) {
            return debutMatchs.toLocalDate();
        }
        return debutPointage != null ? debutPointage.toLocalDate() : null;
    }
}
