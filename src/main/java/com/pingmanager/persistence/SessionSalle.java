package com.pingmanager.persistence;

import com.pingmanager.core.RegistreDisciplinaire;
import com.pingmanager.model.Tournoi;
import com.pingmanager.ordonnancement.EstimateurDuree;
import com.pingmanager.ordonnancement.Ordonnanceur;

import java.util.Objects;

/**
 * Photographie de tout ce qui doit survivre à une interruption en salle : le
 * tournoi, son registre disciplinaire, et l'état vivant de l'ordonnancement
 * (l'{@link Ordonnanceur} et l'{@link EstimateurDuree} avec sa mémoire de
 * durées apprises).
 *
 * <p>C'est l'unité que l'autosave écrit périodiquement et que la reprise
 * recharge. Le tournoi et la discipline forment l'état métier de référence ;
 * l'ordonnanceur et l'estimateur sont l'état de pilotage, en partie
 * recalculable mais dont l'apprentissage des durées serait perdu sans
 * sauvegarde.</p>
 *
 * <p>Conteneur volontairement simple et sans logique : les champs peuvent être
 * nuls quand la session n'a pas encore d'ordonnancement (avant le jour J).</p>
 */
public final class SessionSalle {

    private final Tournoi tournoi;
    private final RegistreDisciplinaire discipline;
    private final Ordonnanceur ordonnanceur;
    private final EstimateurDuree estimateur;

    public SessionSalle(Tournoi tournoi, RegistreDisciplinaire discipline,
                        Ordonnanceur ordonnanceur, EstimateurDuree estimateur) {
        this.tournoi = Objects.requireNonNull(tournoi, "tournoi");
        this.discipline = discipline;
        this.ordonnanceur = ordonnanceur;
        this.estimateur = estimateur;
    }

    /** Session sans ordonnancement (phase de préparation). */
    public static SessionSalle preparation(Tournoi tournoi, RegistreDisciplinaire discipline) {
        return new SessionSalle(tournoi, discipline, null, null);
    }

    public Tournoi getTournoi() {
        return tournoi;
    }

    public RegistreDisciplinaire getDiscipline() {
        return discipline;
    }

    public Ordonnanceur getOrdonnanceur() {
        return ordonnanceur;
    }

    public EstimateurDuree getEstimateur() {
        return estimateur;
    }

    public boolean aOrdonnancement() {
        return ordonnanceur != null;
    }
}
