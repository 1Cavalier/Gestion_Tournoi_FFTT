package com.pingmanager.ordonnancement;

import com.pingmanager.model.Joueur;
import com.pingmanager.model.Tableau;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Valide les inscriptions au regard de la règle — <b>configurable par
 * l'organisateur</b> — interdisant à un joueur de s'inscrire à deux tableaux
 * qui « se suivent », afin d'éviter les blocages en salle.
 *
 * <p>Cette règle n'est pas imposée par le règlement fédéral : c'est une clause
 * que l'organisateur inscrit dans le règlement de son tournoi. Elle est donc
 * activable/désactivable, et le seuil de « proximité » entre deux tableaux est
 * paramétrable.</p>
 *
 * <p>Deux tableaux sont considérés comme « se suivant » si l'écart entre leurs
 * heures de début de matchs est inférieur ou égal à un seuil (par défaut, ils
 * sont consécutifs dans l'ordre horaire).</p>
 */
public final class ValidationInscription {

    private boolean interdireTableauxSuccessifs;
    private Duration seuilProximite;

    /** Univers complet des tableaux du tournoi (pour juger la consécutivité). */
    private List<Tableau> tousLesTableaux = new ArrayList<>();

    /**
     * @param interdireTableauxSuccessifs active la règle
     * @param seuilProximite              deux tableaux dont les débuts sont
     *                                    séparés d'au plus cette durée sont
     *                                    « successifs » ; {@code null} = uniquement
     *                                    les tableaux immédiatement consécutifs
     */
    public ValidationInscription(boolean interdireTableauxSuccessifs, Duration seuilProximite) {
        this.interdireTableauxSuccessifs = interdireTableauxSuccessifs;
        this.seuilProximite = seuilProximite;
    }

    /**
     * Renseigne l'ensemble des tableaux du tournoi. Nécessaire pour juger la
     * consécutivité quand aucun seuil de proximité n'est défini.
     */
    public void definirTousLesTableaux(List<Tableau> tableaux) {
        this.tousLesTableaux = new ArrayList<>(Objects.requireNonNull(tableaux, "tableaux"));
    }

    public static ValidationInscription desactivee() {
        return new ValidationInscription(false, null);
    }

    public void setInterdireTableauxSuccessifs(boolean v) {
        this.interdireTableauxSuccessifs = v;
    }

    public boolean isInterdireTableauxSuccessifs() {
        return interdireTableauxSuccessifs;
    }

    public void setSeuilProximite(Duration seuilProximite) {
        this.seuilProximite = seuilProximite;
    }

    /**
     * Vérifie si un joueur peut s'inscrire à un tableau cible, compte tenu des
     * tableaux auxquels il est déjà inscrit.
     *
     * @param joueur           le joueur candidat
     * @param tableauCible     le tableau visé
     * @param dejaInscrit      tableaux où le joueur est déjà inscrit
     * @return un résultat indiquant l'autorisation et, si refus, le tableau en
     *         conflit
     */
    public ResultatValidation verifier(Joueur joueur, Tableau tableauCible, List<Tableau> dejaInscrit) {
        Objects.requireNonNull(joueur, "joueur");
        Objects.requireNonNull(tableauCible, "tableauCible");
        Objects.requireNonNull(dejaInscrit, "dejaInscrit");

        if (!interdireTableauxSuccessifs) {
            return ResultatValidation.autorise();
        }
        LocalDateTime debutCible = debutDe(tableauCible);
        if (debutCible == null) {
            return ResultatValidation.autorise(); // sans horaire, on ne peut pas juger
        }

        for (Tableau autre : dejaInscrit) {
            if (autre == tableauCible) continue;
            LocalDateTime debutAutre = debutDe(autre);
            if (debutAutre == null) continue;
            if (seSuivent(debutCible, debutAutre, tableauCible, autre)) {
                return ResultatValidation.refuse(autre);
            }
        }
        return ResultatValidation.autorise();
    }

    private LocalDateTime debutDe(Tableau t) {
        return t.getHoraires() == null ? null : t.getHoraires().getDebutMatchs();
    }

    /**
     * Deux tableaux se suivent si l'écart de leurs débuts est sous le seuil
     * (si un seuil est défini), ou s'ils sont immédiatement consécutifs dans
     * l'ordre horaire de l'ensemble des tableaux (si aucun seuil n'est défini).
     */
    private boolean seSuivent(LocalDateTime debutCible, LocalDateTime debutAutre,
                              Tableau cible, Tableau autre) {
        if (seuilProximite != null) {
            long ecart = Math.abs(Duration.between(debutAutre, debutCible).toMinutes());
            return ecart <= seuilProximite.toMinutes();
        }
        // Sans seuil : sont-ils consécutifs dans l'ordre horaire de TOUS les
        // tableaux du tournoi (pas seulement ceux où le joueur est inscrit) ?
        List<Tableau> ordonnes = new ArrayList<>(tousLesTableaux);
        if (!ordonnes.contains(cible)) ordonnes.add(cible);
        if (!ordonnes.contains(autre)) ordonnes.add(autre);
        ordonnes.removeIf(t -> debutDe(t) == null);
        ordonnes.sort(Comparator.comparing(this::debutDe));
        int i1 = ordonnes.indexOf(cible);
        int i2 = ordonnes.indexOf(autre);
        return i1 >= 0 && i2 >= 0 && Math.abs(i1 - i2) == 1;
    }

    /** Résultat d'une validation d'inscription. */
    public static final class ResultatValidation {
        private final boolean autorise;
        private final Tableau tableauEnConflit;

        private ResultatValidation(boolean autorise, Tableau tableauEnConflit) {
            this.autorise = autorise;
            this.tableauEnConflit = tableauEnConflit;
        }

        public static ResultatValidation autorise() {
            return new ResultatValidation(true, null);
        }

        public static ResultatValidation refuse(Tableau tableauEnConflit) {
            return new ResultatValidation(false, tableauEnConflit);
        }

        public boolean estAutorise() {
            return autorise;
        }

        public Tableau getTableauEnConflit() {
            return tableauEnConflit;
        }
    }
}
