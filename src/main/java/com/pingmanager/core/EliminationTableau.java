package com.pingmanager.core;

import com.pingmanager.model.Joueur;
import com.pingmanager.model.Match;
import com.pingmanager.model.Poule;
import com.pingmanager.model.StatutMatch;
import com.pingmanager.model.StatutTableau;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tour;
import com.pingmanager.model.format.FormatTournoi;
import com.pingmanager.model.format.RegistreFormats;
import com.pingmanager.model.format.SortNonQualifies;

import java.util.Objects;

/**
 * Situation d'un joueur dans un tableau le jour J : encore en lice ou éliminé, et
 * tableau commencé ou non. Sert à la clause d'organisateur « inscription à un nouveau
 * tableau après élimination » ({@link com.pingmanager.model.QuotasTableaux#getTableauxApresElimination()}).
 */
public final class EliminationTableau {

    private EliminationTableau() {
    }

    /**
     * Un tableau n'a pas commencé tant que son tirage n'est pas fait : inscriptions ou
     * pointage (ouvert ou clôturé).
     */
    public static boolean nAPasCommence(Tableau t) {
        StatutTableau s = t.getStatut();
        return s == StatutTableau.INSCRIPTION || s == StatutTableau.POINTAGE;
    }

    /**
     * Le joueur ne joue plus dans ce tableau :
     * <ul>
     *   <li>sa poule est terminée et il n'est pas qualifié (format où les non-qualifiés
     *       sont éliminés) ;</li>
     *   <li>le tableau final est construit et il n'y figure pas ;</li>
     *   <li>il a perdu une partie du tableau final et n'a plus de partie à jouer
     *       (petite finale comprise).</li>
     * </ul>
     * Un joueur absent des poules (pas inscrit, ou non pointé) n'est pas « éliminé ».
     */
    public static boolean estElimine(Tableau t, Joueur j) {
        Objects.requireNonNull(t, "tableau");
        Objects.requireNonNull(j, "joueur");
        if (nAPasCommence(t)) {
            return false;
        }
        Poule sienne = null;
        for (Poule p : t.getPoules()) {
            if (p.getJoueurs().contains(j)) {
                sienne = p;
                break;
            }
        }
        if (sienne == null) {
            return false;
        }
        FormatTournoi format = t.getConfiguration().getFormat() != null
                ? t.getConfiguration().getFormat() : RegistreFormats.formatParDefaut();
        boolean nonQualifiesElimines = format.sortNonQualifies() == SortNonQualifies.ELIMINE;

        if (t.getPhaseFinale() != null) {
            boolean present = false;
            boolean battu = false;
            boolean resteAJouer = false;
            for (Tour tour : t.getPhaseFinale().getTours()) {
                for (Match m : tour.getMatchs()) {
                    if (!j.equals(m.getJoueurA()) && !j.equals(m.getJoueurB())) {
                        continue;
                    }
                    present = true;
                    if (m.getStatut() == StatutMatch.TERMINE && !m.estBye() && j.equals(m.getPerdant())) {
                        battu = true;
                    } else if (m.getStatut() != StatutMatch.TERMINE) {
                        resteAJouer = true;
                    }
                }
            }
            if (!present) {
                return nonQualifiesElimines;
            }
            return battu && !resteAJouer;
        }

        if (!sienne.estTerminee() || !nonQualifiesElimines) {
            return false;
        }
        for (SuiviPoule.Ligne l : new SuiviPoule().classementEnDirect(sienne, format.qualifiesParPoule())) {
            if (j.equals(l.classement().getJoueur())) {
                return l.qualification() == SuiviPoule.Qualification.NON_QUALIFIE;
            }
        }
        return false;
    }
}
