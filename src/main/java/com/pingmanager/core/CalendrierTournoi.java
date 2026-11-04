package com.pingmanager.core;

import com.pingmanager.model.InfosReglement;
import com.pingmanager.model.StatutTournoi;
import com.pingmanager.model.Tournoi;
import com.pingmanager.core.reglement.Formats;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Situation d'un tournoi dans le temps : à venir, en cours ou passé (il n'y a pas de
 * statut EN_COURS : un tournoi « en cours » est PUBLIE avec la date du jour comprise
 * dans sa période), et état de ses inscriptions (pas encore ouvertes, ouvertes, closes).
 */
public final class CalendrierTournoi {

    /** Moment du tournoi par rapport à une date. */
    public enum Moment { A_VENIR, EN_COURS, PASSE }

    /** État de la période d'inscription. */
    public enum EtatInscriptions { PAS_ENCORE_OUVERTES, OUVERTES, CLOSES }

    private CalendrierTournoi() {
    }

    public static LocalDate debut(Tournoi t) {
        if (t.getDateDebut() != null) {
            return t.getDateDebut();
        }
        return t.getJours().isEmpty() ? null : t.getJours().first();
    }

    public static LocalDate fin(Tournoi t) {
        if (t.getDateFin() != null) {
            return t.getDateFin();
        }
        return t.getJours().isEmpty() ? debut(t) : t.getJours().last();
    }

    public static Moment moment(Tournoi t, LocalDate aujourdhui) {
        LocalDate d = debut(t);
        LocalDate f = fin(t);
        if (d == null || aujourdhui.isBefore(d)) {
            return Moment.A_VENIR;
        }
        return f != null && aujourdhui.isAfter(f) ? Moment.PASSE : Moment.EN_COURS;
    }

    /** @return vrai si le tournoi est publié et se déroule à cette date. */
    public static boolean estEnCours(Tournoi t, LocalDate aujourdhui) {
        return t.getStatut() == StatutTournoi.PUBLIE && moment(t, aujourdhui) == Moment.EN_COURS;
    }

    /**
     * Inscriptions en ligne : ouvertes entre l'ouverture et la clôture du règlement
     * (sans date d'ouverture : dès la publication ; sans clôture : jusqu'à la veille du
     * premier jour). Un tournoi non publié n'est jamais ouvert.
     */
    public static EtatInscriptions etatInscriptions(Tournoi t, LocalDateTime maintenant) {
        if (t.getStatut() != StatutTournoi.PUBLIE) {
            return t.getStatut() == StatutTournoi.ANNULE ? EtatInscriptions.CLOSES : EtatInscriptions.PAS_ENCORE_OUVERTES;
        }
        InfosReglement r = t.getInfosReglement();
        LocalDateTime ouverture = r == null ? null : r.getOuvertureInscriptions();
        LocalDateTime cloture = r == null ? null : r.getClotureInscriptions();
        if (cloture == null && debut(t) != null) {
            cloture = debut(t).atStartOfDay();
        }
        if (ouverture != null && maintenant.isBefore(ouverture)) {
            return EtatInscriptions.PAS_ENCORE_OUVERTES;
        }
        if (cloture != null && !maintenant.isBefore(cloture)) {
            return EtatInscriptions.CLOSES;
        }
        return EtatInscriptions.OUVERTES;
    }

    /** « Inscriptions ouvertes jusqu'au vendredi 9 avril 2027 à 20h », etc. */
    public static String texteInscriptions(Tournoi t, LocalDateTime maintenant) {
        EtatInscriptions e = etatInscriptions(t, maintenant);
        InfosReglement r = t.getInfosReglement();
        if (e == EtatInscriptions.PAS_ENCORE_OUVERTES) {
            if (t.getStatut() != StatutTournoi.PUBLIE) {
                return "Tournoi pas encore publié";
            }
            return "Inscriptions à partir du " + Formats.dateHeure(r.getOuvertureInscriptions());
        }
        if (e == EtatInscriptions.CLOSES) {
            return t.getStatut() == StatutTournoi.ANNULE ? "Tournoi annulé" : "Inscriptions closes";
        }
        if (r != null && r.getClotureInscriptions() != null) {
            return "Inscriptions ouvertes jusqu'au " + Formats.dateHeure(r.getClotureInscriptions());
        }
        return "Inscriptions ouvertes";
    }
}
