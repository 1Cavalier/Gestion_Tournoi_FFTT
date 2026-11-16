package com.pingmanager.ordonnancement;

/**
 * Réglage du renfort par les tables d'échauffement (B054) : combien de tables
 * de la salle d'échauffement le juge-arbitre accepte d'utiliser comme tables
 * de match, et quand l'application propose de les ouvrir puis de les rendre.
 *
 * <p><b>Règle de déclenchement.</b> L'application mesure le <i>retard prévu</i>
 * (voir {@link EvaluationRetard#retardPrevuMinutes}) : le plus grand des deux
 * retards suivants, en minutes.</p>
 * <ul>
 *   <li>retard de début : un tableau dont l'heure de début est passée et dont
 *   toutes les poules n'ont pas encore commencé ;</li>
 *   <li>retard de fin : fin projetée avec les seules tables de compétition
 *   (maintenant + travail restant ÷ tables de compétition) moins la fin visée
 *   de la journée.</li>
 * </ul>
 * <p>Elle propose d'<b>ouvrir</b> des tables de renfort quand le retard prévu
 * <b>dépasse</b> {@link #getSeuilDeploiementMinutes()} (15 min par défaut), et
 * de les <b>rendre</b> à l'échauffement quand il retombe à
 * {@link #getSeuilRetraitMinutes()} ou moins (0 min par défaut : retard
 * résorbé). L'écart entre les deux seuils évite d'ouvrir et de fermer les
 * tables à chaque match. Le retard est toujours mesuré <i>sans</i> le renfort :
 * les tables ne sont rendues que lorsque les tables de compétition suffisent de
 * nouveau à finir à l'heure.</p>
 *
 * <p>Le nombre de tables de renfort en service ne dépasse jamais
 * {@link #getTablesMax()} (la valeur saisie pour le tournoi). Ce n'est qu'une
 * proposition : le juge-arbitre programme les parties selon les tables
 * disponibles (règles du jeu 3.3.1.2.2) ; il accepte, refuse ou force (voir
 * {@link PiloteRenfort}).</p>
 *
 * <p>Immuable ; construit par {@link #builder()}.</p>
 */
public final class ReglageRenfort {

    /** Seuil de déploiement par défaut : plus de 15 min de retard prévu. */
    public static final int SEUIL_DEPLOIEMENT_PAR_DEFAUT = 15;
    /** Seuil de retrait par défaut : retard prévu résorbé (0 min ou moins). */
    public static final int SEUIL_RETRAIT_PAR_DEFAUT = 0;

    /** Aucun renfort. */
    public static final ReglageRenfort AUCUN = builder().build();

    private final int tablesMax;
    private final int seuilDeploiementMinutes;
    private final int seuilRetraitMinutes;

    private ReglageRenfort(Builder b) {
        this.tablesMax = b.tablesMax;
        this.seuilDeploiementMinutes = b.seuilDeploiement;
        this.seuilRetraitMinutes = b.seuilRetrait;
    }

    /** Raccourci : {@code tablesMax} tables de renfort, seuils par défaut. */
    public static ReglageRenfort tables(int tablesMax) {
        return builder().tablesMax(tablesMax).build();
    }

    /** @return le nombre maximal de tables de renfort en service (0 : pas de renfort). */
    public int getTablesMax() {
        return tablesMax;
    }

    /** @return vrai si au moins une table de renfort est permise. */
    public boolean estActif() {
        return tablesMax > 0;
    }

    /** @return le retard prévu au-delà duquel l'ouverture est proposée, en minutes. */
    public int getSeuilDeploiementMinutes() {
        return seuilDeploiementMinutes;
    }

    /** @return le retard prévu en deçà duquel (ou égal) le retour à l'échauffement est proposé, en minutes. */
    public int getSeuilRetraitMinutes() {
        return seuilRetraitMinutes;
    }

    /** Copie avec un autre nombre maximal de tables. */
    public ReglageRenfort avecTablesMax(int tables) {
        return builder().tablesMax(tables).seuilDeploiementMinutes(seuilDeploiementMinutes)
                .seuilRetraitMinutes(seuilRetraitMinutes).build();
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Réglage pas à pas. */
    public static final class Builder {
        private int tablesMax;
        private int seuilDeploiement = SEUIL_DEPLOIEMENT_PAR_DEFAUT;
        private int seuilRetrait = SEUIL_RETRAIT_PAR_DEFAUT;

        public Builder tablesMax(int v) {
            if (v < 0) {
                throw new IllegalArgumentException("Le nombre de tables de renfort ne peut pas être négatif.");
            }
            this.tablesMax = v;
            return this;
        }

        public Builder seuilDeploiementMinutes(int v) {
            if (v < 0) {
                throw new IllegalArgumentException("Le seuil de déploiement ne peut pas être négatif.");
            }
            this.seuilDeploiement = v;
            return this;
        }

        public Builder seuilRetraitMinutes(int v) {
            this.seuilRetrait = v;
            return this;
        }

        /** @throws IllegalArgumentException si le seuil de retrait dépasse celui de déploiement */
        public ReglageRenfort build() {
            if (seuilRetrait > seuilDeploiement) {
                throw new IllegalArgumentException(
                        "Le seuil de retrait ne peut pas dépasser le seuil de déploiement.");
            }
            return new ReglageRenfort(this);
        }
    }
}
