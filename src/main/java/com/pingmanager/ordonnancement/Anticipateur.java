package com.pingmanager.ordonnancement;

import com.pingmanager.model.Joueur;
import com.pingmanager.model.Match;
import com.pingmanager.model.StatutMatch;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Anticipation à court terme : repère les matchs qui deviendront <b>bientôt</b>
 * jouables — parce que le(s) joueur(s) qui manquent finissent leur match en
 * cours sous peu — et les propose à la préparation, en priorité ceux qui
 * débloquent le plus la suite du tournoi.
 *
 * <p>C'est le scénario visé : un joueur en train de jouer bloque un match
 * important ; on estime sa fin et on suggère de préparer ce match pour qu'il
 * enchaîne dès qu'il a terminé, sans laisser une table inutilement vide.</p>
 */
public final class Anticipateur {

    private final Ordonnanceur ordonnanceur;
    private final PrevisionSalle prevision;

    public Anticipateur(Ordonnanceur ordonnanceur, PrevisionSalle prevision) {
        this.ordonnanceur = Objects.requireNonNull(ordonnanceur, "ordonnanceur");
        this.prevision = Objects.requireNonNull(prevision, "prevision");
    }

    /**
     * Liste les matchs qui deviendront jouables dans la fenêtre à venir, classés
     * par intérêt décroissant (pouvoir de déblocage, puis imminence).
     *
     * <p>Un match est retenu si : ses deux joueurs sont connus, il n'est pas déjà
     * jouable maintenant, et le (ou les) joueur(s) actuellement occupé(s) finissent
     * tous dans la fenêtre {@code horizon}. L'heure « jouable vers » est le moment
     * où le dernier joueur nécessaire se libère.</p>
     *
     * @param maintenant instant courant
     * @param horizon    fenêtre d'anticipation (par ex. 10 minutes)
     * @return suggestions d'anticipation, les plus utiles d'abord
     */
    public List<SuggestionAnticipation> suggererPreparation(LocalDateTime maintenant, Duration horizon) {
        Objects.requireNonNull(maintenant, "maintenant");
        Objects.requireNonNull(horizon, "horizon");
        LocalDateTime limite = maintenant.plus(horizon);

        List<SuggestionAnticipation> suggestions = new ArrayList<>();

        for (MatchOrdonnancable mo : ordonnanceur.tousLesMatchs()) {
            Match m = mo.getMatch();
            // On ne s'intéresse qu'aux matchs à jouer, joueurs connus, pas byes.
            if (m.getStatut() != StatutMatch.A_JOUER || m.estBye()) continue;
            Joueur a = m.getJoueurA();
            Joueur b = m.getJoueurB();
            if (a == null || b == null || a.estADeterminer() || b.estADeterminer()) continue;

            // Déjà jouable maintenant ? Ce n'est pas de l'anticipation.
            if (ordonnanceur.estJouableMaintenant(mo)) continue;

            // Quand chaque joueur se libère-t-il ? (null = déjà libre)
            LocalDateTime dispoA = prevision.disponibiliteJoueur(a);
            LocalDateTime dispoB = prevision.disponibiliteJoueur(b);

            // Si un joueur est occupé sans fin estimable, on ne peut pas anticiper.
            boolean aOccupeSansEstimation = estOccupeSansEstimation(a, dispoA);
            boolean bOccupeSansEstimation = estOccupeSansEstimation(b, dispoB);
            if (aOccupeSansEstimation || bOccupeSansEstimation) continue;

            // Le match devient jouable quand le DERNIER joueur nécessaire se libère.
            LocalDateTime jouableVers = maintenant;
            if (dispoA != null && dispoA.isAfter(jouableVers)) jouableVers = dispoA;
            if (dispoB != null && dispoB.isAfter(jouableVers)) jouableVers = dispoB;

            // Retenu seulement si ça tombe dans l'horizon d'anticipation.
            if (jouableVers.isAfter(limite)) continue;

            int deblocage = ordonnanceur.compterEngagementsAilleurs(mo);
            String explication = construireExplication(jouableVers, maintenant, deblocage);
            suggestions.add(new SuggestionAnticipation(mo, jouableVers, deblocage, explication));
        }

        // Priorité : d'abord le pouvoir de déblocage, puis l'imminence.
        suggestions.sort(Comparator
                .comparingInt(SuggestionAnticipation::getPouvoirDeblocage).reversed()
                .thenComparing(SuggestionAnticipation::getJouableVers));
        return suggestions;
    }

    /** Un joueur est « occupé sans estimation » s'il joue mais sans heure de fin estimable. */
    private boolean estOccupeSansEstimation(Joueur joueur, LocalDateTime dispo) {
        boolean occupe = ordonnanceur.joueursOccupes().contains(joueur.getNumeroLicence());
        return occupe && dispo == null;
    }

    private String construireExplication(LocalDateTime jouableVers, LocalDateTime maintenant, int deblocage) {
        long minutes = Math.max(0, Duration.between(maintenant, jouableVers).toMinutes());
        List<String> parts = new ArrayList<>();
        parts.add("prêt dans ~" + minutes + " min");
        if (deblocage > 0) {
            parts.add("débloque " + deblocage + " match(s)");
        }
        return String.join(", ", parts);
    }
}
