package com.pingmanager.core;

import com.pingmanager.model.EvenementAgenda;

/**
 * Résultat détaillé d'une opération sur l'agenda du compte : motif précis et,
 * selon le cas, la date ajoutée, modifiée ou retirée.
 */
public final class ResultatAgenda {

    /** Issue de l'opération. */
    public enum Motif {
        AJOUTE("Date ajoutée à l'agenda."),
        MODIFIE("Date modifiée."),
        RETIRE("Date retirée de l'agenda."),
        INCHANGE("Agenda déjà à jour."),
        TITRE_MANQUANT("Indiquez un titre."),
        TITRE_TROP_LONG("Le titre est trop long (" + ServiceAgenda.LONGUEUR_MAX_TITRE + " caractères au plus)."),
        NOTE_TROP_LONGUE("La note est trop longue (" + ServiceAgenda.LONGUEUR_MAX_NOTE + " caractères au plus)."),
        DATE_MANQUANTE("Indiquez une date."),
        FIN_AVANT_DEBUT("La date de fin ne peut pas précéder la date de début."),
        INTROUVABLE("Cette date n'est plus dans l'agenda."),
        DATE_AUTOMATIQUE("Cette date suit votre inscription au tournoi : elle se retire en vous désinscrivant.");

        private final String message;

        Motif(String message) {
            this.message = message;
        }

        public String message() {
            return message;
        }
    }

    private final Motif motif;
    private final EvenementAgenda evenement;

    ResultatAgenda(Motif motif, EvenementAgenda evenement) {
        this.motif = motif;
        this.evenement = evenement;
    }

    public Motif getMotif() {
        return motif;
    }

    /** @return la date concernée (ajoutée, modifiée, retirée ou inchangée), ou {@code null} en cas de refus. */
    public EvenementAgenda getEvenement() {
        return evenement;
    }

    /** @return vrai si l'opération est acceptée (y compris « déjà à jour »). */
    public boolean estAccepte() {
        return motif == Motif.AJOUTE || motif == Motif.MODIFIE || motif == Motif.RETIRE
                || motif == Motif.INCHANGE;
    }

    public String message() {
        return motif.message();
    }

    @Override
    public String toString() {
        return "ResultatAgenda[" + motif + (evenement == null ? "" : ", " + evenement) + "]";
    }
}
