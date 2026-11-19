package com.pingmanager.core;

import com.pingmanager.model.Match;
import com.pingmanager.ordonnancement.ResultatAffectationTable;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Ce que l'interface « Lancer un match » montre au juge-arbitre (B055, B057), calculé
 * par {@link ServiceSalle#listeLancements} : chaque match à jouer dont les deux
 * joueurs sont connus, avec la table que l'ordonnanceur propose, les tables
 * libres qui peuvent le recevoir, et, s'il ne peut pas partir maintenant, le
 * motif précis du refus (joueur en jeu sur une table, aucune table libre…).
 *
 * <p>Deux ordres : {@link #getProposes()} suit la priorité de la stratégie en
 * place (matchs lançables d'abord, puis les matchs bloqués) ;
 * {@link #getParTableau()} suit l'ordre du tournoi (tableau, poule, tour). Les
 * deux listes contiennent les mêmes lignes. {@link #getJouables()} ne garde que
 * les matchs dont les deux joueurs sont libres (liste de l'interface B057) et
 * {@link #propositionApres} donne la proposition suivante du bouton « Autre »,
 * dans le même ordre : il n'y a pas de second calcul de priorité.</p>
 *
 * <p><b>Poules.</b> Pendant la phase de poules, une ligne peut porter une
 * poule entière ({@link Ligne#poule()}) : elle se lance en bloc sur une table
 * ({@link ServiceSalle#lancerPoule}). Sa ligne est identifiée par son premier
 * match à jouer ({@link Ligne#match()}), si bien que poules et matchs du tableau
 * final cohabitent dans le même ordre de priorité.</p>
 *
 * <p>La liste n'est qu'une photographie : le lancement lui-même repasse
 * toujours par {@link ServiceSalle#affecter}, qui refait toutes les
 * vérifications (un joueur jamais sur deux tables, table libre).</p>
 *
 * <p>Objet immuable et sans JavaFX.</p>
 */
public final class ListeLancements {

    /**
     * Un match de la liste.
     *
     * @param match          le match vu depuis la salle (tableau, tour, couleur de priorité)
     * @param propose        true si la stratégie l'enverrait maintenant (« Proposer »)
     * @param tableProposee  table proposée (celle de l'ordonnanceur, sinon la première
     *                       table libre qui l'accepte), ou null s'il ne peut pas partir
     * @param tablesPossibles tables libres qui peuvent le recevoir maintenant (vide si refus)
     * @param attenteDepuis  instant depuis lequel ses deux joueurs l'attendent, ou null
     * @param refus          motif du refus, ou null si le match peut être lancé
     * @param numero         numéro du match dans son tableau (1, 2…), dans l'ordre du
     *                       tableau : poules (ordre officiel I.301), puis tours du tableau final
     * @param echelon        échelon dans le tableau : « Poule 3 », « 1/8 de finale »
     * @param poule          poule entière à lancer en bloc (phase de poules), ou null pour un match ;
     *                       {@code match} est alors son premier match à jouer (ordre I.301)
     */
    public record Ligne(TableauDeBordSalle.MatchSalle match, boolean propose, Integer tableProposee,
                        List<Integer> tablesPossibles, LocalDateTime attenteDepuis,
                        ResultatAffectationTable refus, int numero, String echelon, PouleALancer poule) {

        /** Ligne d'un match seul. */
        public Ligne(TableauDeBordSalle.MatchSalle match, boolean propose, Integer tableProposee,
                     List<Integer> tablesPossibles, LocalDateTime attenteDepuis,
                     ResultatAffectationTable refus, int numero, String echelon) {
            this(match, propose, tableProposee, tablesPossibles, attenteDepuis, refus, numero, echelon, null);
        }

        /** @return true si la ligne porte une poule entière. */
        public boolean estPoule() {
            return poule != null;
        }

        public Ligne {
            Objects.requireNonNull(match, "match");
            echelon = echelon == null ? "" : echelon;
            tablesPossibles = tablesPossibles == null ? List.of() : List.copyOf(tablesPossibles);
            if (refus != null && refus.estReussi()) {
                refus = null;
            }
            if (refus != null) {
                tableProposee = null;
                tablesPossibles = List.of();
            }
        }

        /** @return true si le match peut être envoyé maintenant sur une table libre. */
        public boolean estLancable() {
            return refus == null && tableProposee != null;
        }

        /**
         * @return true si les deux joueurs sont disponibles : le match est prêt,
         *         même s'il manque une table libre pour le recevoir
         */
        public boolean estPret() {
            return refus == null
                    || refus.getMotif() == ResultatAffectationTable.Motif.AUCUNE_TABLE_LIBRE
                    || refus.getMotif() == ResultatAffectationTable.Motif.TABLE_RESERVEE;
        }

        /** Attente des joueurs à un instant donné (jamais négative), ou null. */
        public Duration attente(LocalDateTime maintenant) {
            if (attenteDepuis == null || maintenant == null) {
                return null;
            }
            Duration d = Duration.between(attenteDepuis, maintenant);
            return d.isNegative() ? Duration.ZERO : d;
        }
    }

    private final LocalDateTime maintenant;
    private final List<Integer> tablesLibres;
    private final List<Ligne> proposes;
    private final List<Ligne> parTableau;

    ListeLancements(LocalDateTime maintenant, List<Integer> tablesLibres, List<Ligne> proposes,
                    List<Ligne> parTableau) {
        this.maintenant = maintenant;
        this.tablesLibres = List.copyOf(tablesLibres);
        this.proposes = List.copyOf(proposes);
        this.parTableau = List.copyOf(parTableau);
    }

    /** Instant de la photographie. */
    public LocalDateTime getMaintenant() {
        return maintenant;
    }

    /** Tables libres de la salle, dans l'ordre. */
    public List<Integer> getTablesLibres() {
        return tablesLibres;
    }

    /** Lignes par priorité décroissante : lançables d'abord, puis bloquées (avec leur motif). */
    public List<Ligne> getProposes() {
        return proposes;
    }

    /** Les mêmes lignes dans l'ordre du tournoi (tableau, poule ou tour), pour le choix manuel. */
    public List<Ligne> getParTableau() {
        return parTableau;
    }

    /**
     * Matchs réellement jouables (les deux joueurs connus et libres), du plus
     * prioritaire au moins prioritaire selon la stratégie en place, qu'une table
     * libre les attende ou non. Liste de l'interface « Lancer un match » (B057).
     */
    public List<Ligne> getJouables() {
        return proposes.stream().filter(Ligne::estPret).toList();
    }

    /**
     * Proposition suivante du bouton « Autre » (B057) : le match jouable qui suit
     * {@code precedent} dans l'ordre de priorité, en revenant au premier après le
     * dernier. Sans précédent (premier appui sur « Proposer un match »), ou si
     * le précédent n'est plus jouable, la proposition la plus prioritaire.
     *
     * @return la proposition, ou vide s'il n'y a aucun match jouable
     */
    public Optional<Ligne> propositionApres(Match precedent) {
        List<Ligne> jouables = getJouables();
        if (jouables.isEmpty()) {
            return Optional.empty();
        }
        for (int i = 0; precedent != null && i < jouables.size(); i++) {
            if (jouables.get(i).match().match() == precedent) {
                return Optional.of(jouables.get((i + 1) % jouables.size()));
            }
        }
        return Optional.of(jouables.get(0));
    }

    /** Ligne jouable d'un match, ou vide s'il n'est plus jouable (lancé, joueur en jeu…). */
    public Optional<Ligne> jouable(Match match) {
        return getJouables().stream().filter(l -> l.match().match() == match).findFirst();
    }

    /** Nombre de matchs prêts (joueurs disponibles), qu'une table libre les attende ou non. */
    public int nombrePrets() {
        return (int) proposes.stream().filter(Ligne::estPret).count();
    }

    /** Nombre de matchs qui peuvent partir maintenant. */
    public int nombreLancables() {
        return (int) proposes.stream().filter(Ligne::estLancable).count();
    }
}
