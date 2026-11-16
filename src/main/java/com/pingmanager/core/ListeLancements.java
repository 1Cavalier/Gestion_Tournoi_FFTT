package com.pingmanager.core;

import com.pingmanager.ordonnancement.ResultatAffectationTable;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Ce que l'interface « Lancer un match » montre au juge-arbitre (B055), calculé
 * par {@link ServiceSalle#listeLancements} : chaque match à jouer dont les deux
 * joueurs sont connus, avec la table que l'ordonnanceur propose, les tables
 * libres qui peuvent le recevoir, et, s'il ne peut pas partir maintenant, le
 * motif précis du refus (joueur en jeu sur une table, aucune table libre…).
 *
 * <p>Deux ordres : {@link #getProposes()} suit la priorité de la stratégie en
 * place (matchs lançables d'abord, puis les matchs bloqués) ;
 * {@link #getParTableau()} suit l'ordre du tournoi (tableau, poule, tour), pour
 * le choix manuel. Les deux listes contiennent les mêmes lignes.</p>
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
     */
    public record Ligne(TableauDeBordSalle.MatchSalle match, boolean propose, Integer tableProposee,
                        List<Integer> tablesPossibles, LocalDateTime attenteDepuis,
                        ResultatAffectationTable refus) {
        public Ligne {
            Objects.requireNonNull(match, "match");
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

    /** Nombre de matchs qui peuvent partir maintenant. */
    public int nombreLancables() {
        return (int) proposes.stream().filter(Ligne::estLancable).count();
    }
}
