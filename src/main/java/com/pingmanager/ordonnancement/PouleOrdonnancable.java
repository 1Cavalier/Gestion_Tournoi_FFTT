package com.pingmanager.ordonnancement;

import com.pingmanager.model.Joueur;
import com.pingmanager.model.Match;
import com.pingmanager.model.Poule;
import com.pingmanager.model.StatutMatch;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/**
 * Une poule vue comme une <b>unité de lancement</b> : pendant la phase de
 * poules, le juge-arbitre envoie la poule entière sur une table, avec sa feuille
 * de poule, et les joueurs y enchaînent tous leurs matchs. La poule occupe sa
 * table et tous ses joueurs jusqu'au dernier résultat.
 *
 * <p>Les matchs gardent l'ordre officiel déjà calculé par
 * {@code GenerateurMatchsPoule} (I.301 : ordre des parties selon la taille de la
 * poule et le nombre de qualifiés) ; ils se jouent l'un après l'autre dans cet
 * ordre, y compris les deux (ou trois) matchs d'un même tour. Rien n'est
 * réordonné ici.</p>
 *
 * <p>Enveloppe d'ordonnancement, comme {@link MatchOrdonnancable} : le modèle
 * {@link Poule} porte la table et l'heure de lancement (enregistrées avec le
 * tournoi) ; cette classe y ajoute le tableau, l'attente et les joueurs absents
 * signalés par l'appelant.</p>
 */
public final class PouleOrdonnancable {

    private final Poule poule;
    private final String identifiantTableau;
    private final String nomTableau;
    /** Matchs de la poule dans l'ordre I.301, exempts exclus. */
    private final List<MatchOrdonnancable> matchs;
    /** Licences des joueurs de la poule non présents (non pointés), fournies par l'appelant. */
    private final Set<String> absents;
    private LocalDateTime pretDepuis;

    public PouleOrdonnancable(Poule poule, String identifiantTableau, String nomTableau,
                              List<MatchOrdonnancable> matchs, Set<String> absents) {
        this.poule = Objects.requireNonNull(poule, "poule");
        this.identifiantTableau = Objects.requireNonNull(identifiantTableau, "identifiantTableau");
        this.nomTableau = nomTableau == null ? identifiantTableau : nomTableau;
        this.matchs = List.copyOf(matchs);
        this.absents = absents == null ? Set.of() : Set.copyOf(absents);
    }

    public PouleOrdonnancable(Poule poule, String identifiantTableau, String nomTableau,
                              List<MatchOrdonnancable> matchs) {
        this(poule, identifiantTableau, nomTableau, matchs, Set.of());
    }

    public Poule getPoule() {
        return poule;
    }

    public String getIdentifiantTableau() {
        return identifiantTableau;
    }

    public String getNomTableau() {
        return nomTableau;
    }

    /** @return les matchs dans l'ordre où ils se jouent sur la table (I.301), exempts exclus. */
    public List<MatchOrdonnancable> getMatchs() {
        return matchs;
    }

    public Set<String> getAbsents() {
        return absents;
    }

    /** Instant depuis lequel tous les joueurs de la poule sont libres (attente), ou null. */
    public LocalDateTime getPretDepuis() {
        return pretDepuis;
    }

    public void setPretDepuis(LocalDateTime pretDepuis) {
        this.pretDepuis = pretDepuis;
    }

    /** @return true si la poule est lancée en bloc et qu'un de ses matchs attend encore son résultat. */
    public boolean estEnCours() {
        return poule.estLanceeEnBloc() && matchs.stream().anyMatch(m -> m.getMatch().getStatut() != StatutMatch.TERMINE);
    }

    /** @return les matchs encore à jouer (statut « à jouer »), dans l'ordre I.301. */
    public List<MatchOrdonnancable> aJouer() {
        List<MatchOrdonnancable> r = new ArrayList<>();
        for (MatchOrdonnancable mo : matchs) {
            if (mo.getMatch().getStatut() == StatutMatch.A_JOUER) {
                r.add(mo);
            }
        }
        return r;
    }

    /** @return les matchs sans résultat (à jouer ou en cours), dans l'ordre I.301. */
    public List<MatchOrdonnancable> sansResultat() {
        List<MatchOrdonnancable> r = new ArrayList<>();
        for (MatchOrdonnancable mo : matchs) {
            if (mo.getMatch().getStatut() != StatutMatch.TERMINE) {
                r.add(mo);
            }
        }
        return r;
    }

    /**
     * Match qui se joue maintenant sur la table de la poule : le premier match
     * sans résultat dans l'ordre I.301, ou null si tous ont un résultat.
     */
    public MatchOrdonnancable matchEnJeu() {
        for (MatchOrdonnancable mo : matchs) {
            if (mo.getMatch().getStatut() == StatutMatch.EN_COURS) {
                return mo;
            }
        }
        return null;
    }

    /** @return les joueurs de la poule, dans l'ordre de la poule (1 = tête de poule). */
    public List<Joueur> joueurs() {
        return poule.getJoueurs();
    }

    /** @return true si ce joueur (licence) est dans la poule. */
    public boolean contient(String licence) {
        if (licence == null) {
            return false;
        }
        for (Joueur j : poule.getJoueurs()) {
            if (j != null && licence.equals(j.getNumeroLicence())) {
                return true;
            }
        }
        return false;
    }

    /** @return true si le match appartient à cette poule (identité). */
    public boolean contient(Match m) {
        for (MatchOrdonnancable mo : matchs) {
            if (mo.getMatch() == m) {
                return true;
            }
        }
        return false;
    }

    /**
     * Durée prévue de la poule : somme des durées prévues des matchs sans
     * résultat (tous ses matchs avant le lancement).
     *
     * @param dureeDe durée prévue d'un match (typiquement l'{@link EstimateurDuree})
     */
    public Duration dureePrevue(Function<MatchOrdonnancable, Duration> dureeDe) {
        Objects.requireNonNull(dureeDe, "dureeDe");
        Duration total = Duration.ZERO;
        for (MatchOrdonnancable mo : sansResultat()) {
            Duration d = dureeDe.apply(mo);
            if (d != null && !d.isNegative()) {
                total = total.plus(d);
            }
        }
        return total;
    }

    @Override
    public String toString() {
        return nomTableau + " · Poule " + poule.getNom();
    }
}
