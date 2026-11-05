package com.pingmanager.core;

import com.pingmanager.model.LigneClassement;
import com.pingmanager.model.Match;
import com.pingmanager.model.Poule;
import com.pingmanager.model.StatutMatch;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Suivi en direct d'une poule pendant le jour J : tour de chaque match dans
 * l'ordre officiel, avancement, et classement provisoire avec les places
 * qualificatives.
 *
 * <p>Les matchs d'une poule sont rangés par {@link GenerateurMatchsPoule} dans
 * l'ordre de l'article I.301 ; chaque tour y compte autant de matchs que la
 * moitié (arrondie à l'inférieur) de l'effectif : 1 en poule de 3, 2 en poule de
 * 4 ou 5, 3 en poule de 6.</p>
 *
 * <p>Le classement vient de {@link ClassementPouleService} (I.303) et ne compte
 * que les matchs terminés. Les qualifiés ne sont <b>acquis</b> qu'une fois tous
 * les matchs de la poule joués ; avant, on parle de place qualificative.</p>
 */
public final class SuiviPoule {

    /** Situation d'un joueur vis-à-vis de la qualification. */
    public enum Qualification {
        /** Poule terminée, joueur classé dans les places qualificatives. */
        QUALIFIE,
        /** Poule terminée, joueur classé hors des places qualificatives. */
        NON_QUALIFIE,
        /** Poule en cours, joueur actuellement dans les places qualificatives. */
        PLACE_QUALIFICATIVE,
        /** Poule en cours, joueur actuellement hors des places qualificatives. */
        HORS_PLACE_QUALIFICATIVE
    }

    /** Une ligne du classement en direct. */
    public record Ligne(LigneClassement classement, Qualification qualification) {
        public Ligne {
            Objects.requireNonNull(classement, "classement");
            Objects.requireNonNull(qualification, "qualification");
        }

        /** @return true si le joueur est qualifié ou en place qualificative. */
        public boolean enPlaceQualificative() {
            return qualification == Qualification.QUALIFIE
                    || qualification == Qualification.PLACE_QUALIFICATIVE;
        }
    }

    private final ClassementPouleService classement;

    public SuiviPoule() {
        this(new ClassementPouleService());
    }

    public SuiviPoule(ClassementPouleService classement) {
        this.classement = Objects.requireNonNull(classement, "classement");
    }

    /**
     * I.301 : tour (à partir de 1) du match d'indice donné dans l'ordre de la poule.
     *
     * @param poule      la poule
     * @param indexMatch indice du match dans {@link Poule#getMatchs()} (à partir de 0)
     */
    public static int tourDuMatch(Poule poule, int indexMatch) {
        Objects.requireNonNull(poule, "poule");
        int parTour = Math.max(1, poule.getTaille() / 2);
        return indexMatch / parTour + 1;
    }

    /** @return le nombre de matchs terminés de la poule. */
    public static int matchsTermines(Poule poule) {
        int n = 0;
        for (Match m : poule.getMatchs()) {
            if (m.getStatut() == StatutMatch.TERMINE) {
                n++;
            }
        }
        return n;
    }

    /**
     * Classement provisoire (ou définitif si la poule est terminée), avec la
     * situation de chaque joueur vis-à-vis des places qualificatives.
     *
     * @param qualifiesParPoule nombre de qualifiés par poule du format
     */
    public List<Ligne> classementEnDirect(Poule poule, int qualifiesParPoule) {
        Objects.requireNonNull(poule, "poule");
        boolean terminee = poule.estTerminee();
        List<Ligne> res = new ArrayList<>();
        for (LigneClassement l : classement.calculer(poule)) {
            boolean dedans = l.getRang() <= qualifiesParPoule;
            Qualification q;
            if (terminee) {
                q = dedans ? Qualification.QUALIFIE : Qualification.NON_QUALIFIE;
            } else {
                q = dedans ? Qualification.PLACE_QUALIFICATIVE : Qualification.HORS_PLACE_QUALIFICATIVE;
            }
            res.add(new Ligne(l, q));
        }
        return res;
    }
}
