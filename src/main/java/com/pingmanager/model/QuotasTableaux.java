package com.pingmanager.model;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * Nombre maximum de tableaux par joueur et par jour, distinct pour les joueurs
 * et les joueuses, éventuellement différent selon le jour du tournoi.
 * {@code null} = pas de limite.
 *
 * <p><b>Tableau supplémentaire des joueuses.</b> Quand la limite des joueuses
 * dépasse celle des joueurs (ex. 3 contre 2), ce surplus peut être réservé aux
 * joueuses inscrites, le même jour, à un tableau précis — en général le tableau
 * Toutes séries dames : {@link #getTableauBonusJoueuses()}. Sans ce tableau, une
 * joueuse est limitée comme un joueur.</p>
 */
public final class QuotasTableaux {

    /** Limites d'un jour : joueurs, joueuses ({@code null} = pas de limite). */
    public record Quota(Integer joueurs, Integer joueuses) {
        public Quota {
            if ((joueurs != null && joueurs < 1) || (joueuses != null && joueuses < 1)) {
                throw new IllegalArgumentException("Un quota de tableaux doit valoir au moins 1.");
            }
        }

        public boolean aLimite() {
            return joueurs != null || joueuses != null;
        }

        /** @return la limite pour un sexe donné, ou {@code null}. */
        public Integer pour(Sexe sexe) {
            return sexe == Sexe.FEMME ? joueuses : joueurs;
        }
    }

    private static final Quota SANS_LIMITE = new Quota(null, null);

    private final Quota tousLesJours;
    private final Map<LocalDate, Quota> parJour;
    private final String tableauBonusJoueuses;
    private final boolean tableauBonusObligatoire;
    private final boolean tableauxConsecutifsInterdits;

    private QuotasTableaux(Quota tousLesJours, Map<LocalDate, Quota> parJour, String tableauBonusJoueuses,
                           boolean tableauBonusObligatoire, boolean tableauxConsecutifsInterdits) {
        this.tousLesJours = tousLesJours == null ? SANS_LIMITE : tousLesJours;
        this.parJour = Collections.unmodifiableMap(new TreeMap<>(parJour == null ? Map.of() : parJour));
        this.tableauBonusJoueuses = tableauBonusJoueuses == null || tableauBonusJoueuses.isBlank()
                ? null : tableauBonusJoueuses.trim();
        this.tableauBonusObligatoire = this.tableauBonusJoueuses != null && tableauBonusObligatoire;
        this.tableauxConsecutifsInterdits = tableauxConsecutifsInterdits;
    }

    private QuotasTableaux(Quota tousLesJours, Map<LocalDate, Quota> parJour) {
        this(tousLesJours, parJour, null, false, false);
    }

    /**
     * @param nomTableau nom du tableau qui ouvre droit au tableau supplémentaire des
     *                   joueuses ({@code null} = surplus accordé sans condition)
     */
    public QuotasTableaux avecTableauBonusJoueuses(String nomTableau) {
        return new QuotasTableaux(tousLesJours, parJour, nomTableau, tableauBonusObligatoire,
                tableauxConsecutifsInterdits);
    }

    /**
     * @param obligatoire le jour où le tableau des joueuses est joué, une joueuse ne
     *                    peut s'inscrire à un autre tableau que si elle est inscrite à
     *                    celui-ci (sans effet si aucun tableau n'est choisi)
     */
    public QuotasTableaux avecTableauBonusObligatoire(boolean obligatoire) {
        return new QuotasTableaux(tousLesJours, parJour, tableauBonusJoueuses, obligatoire,
                tableauxConsecutifsInterdits);
    }

    /** @param interdits un joueur ne peut pas s'inscrire à deux tableaux qui se suivent le même jour */
    public QuotasTableaux avecTableauxConsecutifsInterdits(boolean interdits) {
        return new QuotasTableaux(tousLesJours, parJour, tableauBonusJoueuses, tableauBonusObligatoire, interdits);
    }

    public boolean isTableauBonusObligatoire() {
        return tableauBonusObligatoire;
    }

    public boolean isTableauxConsecutifsInterdits() {
        return tableauxConsecutifsInterdits;
    }

    /** @return le nom du tableau exigé pour le surplus des joueuses, ou {@code null}. */
    public String getTableauBonusJoueuses() {
        return tableauBonusJoueuses;
    }

    /**
     * Limite effective d'un jour pour un sexe donné.
     *
     * @param inscriteAuTableauBonus la joueuse est (ou serait, avec l'inscription en
     *                               cours) inscrite ce jour-là au tableau exigé
     * @return le nombre maximum de tableaux, ou {@code null} si pas de limite
     */
    public Integer limite(LocalDate jour, Sexe sexe, boolean inscriteAuTableauBonus) {
        Quota q = pour(jour);
        if (sexe != Sexe.FEMME) {
            return q.joueurs();
        }
        Integer f = q.joueuses();
        Integer h = q.joueurs();
        boolean surplus = f == null ? h != null : h != null && f > h;
        if (surplus && tableauBonusJoueuses != null && !inscriteAuTableauBonus) {
            return h;
        }
        return f;
    }

    public static QuotasTableaux aucun() {
        return new QuotasTableaux(SANS_LIMITE, Map.of());
    }

    /** Même limite tous les jours. */
    public static QuotasTableaux identiques(Integer joueurs, Integer joueuses) {
        return new QuotasTableaux(new Quota(joueurs, joueuses), Map.of());
    }

    /** Une limite par jour (les jours absents n'ont pas de limite). */
    public static QuotasTableaux parJour(Map<LocalDate, Quota> parJour) {
        return new QuotasTableaux(SANS_LIMITE, parJour);
    }

    public Quota getTousLesJours() {
        return tousLesJours;
    }

    public Map<LocalDate, Quota> getParJour() {
        return parJour;
    }

    public boolean varieSelonLeJour() {
        return !parJour.isEmpty();
    }

    /** @return les limites applicables à un jour. */
    public Quota pour(LocalDate jour) {
        return varieSelonLeJour() ? parJour.getOrDefault(jour, SANS_LIMITE) : tousLesJours;
    }

    public boolean aLimite() {
        return tousLesJours.aLimite() || parJour.values().stream().anyMatch(Quota::aLimite);
    }

    /** @return vrai si une règle d'inscription est définie (limite, obligation ou consécutifs). */
    public boolean aUneRegle() {
        return aLimite() || tableauBonusObligatoire || tableauxConsecutifsInterdits;
    }
}
