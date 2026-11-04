package com.pingmanager.core;

import com.pingmanager.model.Joueur;
import com.pingmanager.model.Poule;
import com.pingmanager.model.StatutTableau;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.TableauElimination;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.format.FormatTournoi;

import java.util.List;
import java.util.Objects;

/**
 * Service de <b>lancement d'un tableau</b> : la charnière entre la phase
 * d'inscription/pointage (bloc 1) et le déroulé sportif (moteur de poules et
 * tableau).
 *
 * <p>Le lancement transforme la liste des présents (les inscrits restants après
 * clôture du pointage) en poules jouables, via {@link MoteurTournoi}, puis fait
 * passer le tableau au statut {@link StatutTableau#LANCE}.</p>
 *
 * <p>Séquence de {@link #lancer(Tableau, long)} :</p>
 * <ol>
 *   <li>vérifier que le tableau est en phase {@link StatutTableau#POINTAGE} et que
 *       son pointage a été <b>clôturé</b> (pré-condition stricte : on ne lance pas
 *       un tableau dont le pointage n'est pas figé) ;</li>
 *   <li>prendre les inscrits (= présents pointés, les absents ayant été libérés à
 *       la clôture) ;</li>
 *   <li>constituer les poules selon le {@link FormatTournoi} du tableau, avec le
 *       tirage au sort reproductible (article IX.109 : tirage public), la graine
 *       étant fournie et conservée pour rejouer/vérifier le tirage ;</li>
 *   <li>ranger les poules dans le tableau et passer son statut à
 *       {@link StatutTableau#LANCE}.</li>
 * </ol>
 *
 * <p>Vit dans {@code core} : il relie {@code model} et le moteur sans qu'aucun
 * package du modèle ne dépende de l'UI ou de la persistance.</p>
 */
public final class ServiceLancementTableau {

    private final MoteurTournoi moteur;
    private final Tournoi tournoi;
    private final PolitiqueOuverture politiqueOuverture;

    public ServiceLancementTableau() {
        this(new MoteurTournoi(), null, PolitiqueOuverture.REFUSER_SI_ANNULE);
    }

    public ServiceLancementTableau(MoteurTournoi moteur) {
        this(moteur, null, PolitiqueOuverture.REFUSER_SI_ANNULE);
    }

    /**
     * @param tournoi le tournoi concerné ({@code null} = pas de contrôle de statut,
     *                pour les usages bas niveau/tests) ; s'il est fourni, la
     *                {@code politiqueOuverture} s'applique au lancement
     */
    public ServiceLancementTableau(Tournoi tournoi) {
        this(new MoteurTournoi(), tournoi, PolitiqueOuverture.REFUSER_SI_ANNULE);
    }

    public ServiceLancementTableau(MoteurTournoi moteur, Tournoi tournoi,
                                   PolitiqueOuverture politiqueOuverture) {
        this.moteur = Objects.requireNonNull(moteur, "moteur");
        this.tournoi = tournoi;
        this.politiqueOuverture = Objects.requireNonNull(politiqueOuverture, "politiqueOuverture");
    }

    /**
     * Lance un tableau : génère ses poules à partir des présents et le fait passer
     * en phase {@link StatutTableau#LANCE}.
     *
     * @param tableau le tableau à lancer
     * @param graine  graine du tirage au sort (reproductible, tirage public)
     * @return le compte rendu du lancement (poules générées, graine)
     * @throws IllegalStateException    si le tableau n'est pas prêt (pas en pointage
     *                                  ou pointage non clôturé)
     * @throws IllegalArgumentException si le tableau n'a pas de format défini
     */
    public ResultatLancement lancer(Tableau tableau, long graine) {
        Objects.requireNonNull(tableau, "tableau");

        // Si un tournoi est associé, son statut doit autoriser le lancement.
        if (tournoi != null) {
            if (!tournoi.getTableaux().contains(tableau)) {
                throw new IllegalArgumentException("Le tableau n'appartient pas à ce tournoi.");
            }
            if (!politiqueOuverture.autorise(tournoi.getStatut())) {
                throw new IllegalStateException(
                        "Lancement impossible : le tournoi est " + tournoi.getStatut() + ".");
            }
        }

        if (tableau.getStatut() != StatutTableau.POINTAGE) {
            throw new IllegalStateException(
                    "Le tableau doit être en phase de pointage pour être lancé (statut actuel : "
                            + tableau.getStatut() + ").");
        }
        if (!tableau.pointageEstCloture()) {
            throw new IllegalStateException(
                    "Le pointage doit être clôturé avant de lancer le tableau.");
        }
        FormatTournoi format = tableau.getConfiguration().getFormat();
        if (format == null) {
            throw new IllegalArgumentException(
                    "Le tableau n'a pas de format défini : impossible de générer les poules.");
        }

        List<Joueur> presents = List.copyOf(tableau.getInscrits());
        if (presents.size() < format.taillePouleSouhaitee()) {
            throw new IllegalStateException(
                    "Pas assez de présents pour lancer le tableau : " + presents.size()
                            + " présent(s) pour des poules de " + format.taillePouleSouhaitee()
                            + ". Ajuster les inscriptions ou le format avant de lancer.");
        }
        List<Poule> poules = moteur.preparerPoules(presents, format, graine);

        tableau.definirPoules(poules);
        tableau.marquerLance();

        return new ResultatLancement(poules, graine, presents.size());
    }

    /**
     * Lance un tableau en laissant le service choisir une graine de tirage. La
     * graine retenue est reportée dans le résultat pour traçabilité.
     */
    public ResultatLancement lancer(Tableau tableau) {
        return lancer(tableau, System.nanoTime());
    }

    /**
     * Construit la phase finale (tableau à élimination directe) d'un tableau dont
     * les poules sont terminées, et le fait passer au statut
     * {@link StatutTableau#PHASE_FINALE}.
     *
     * <p>Second tirage au sort du tableau (placement des qualifiés I.304/I.305),
     * distinct du tirage des poules et effectué à un autre moment (une fois les
     * qualifiés connus). Sa graine, propre à ce tirage, est fournie et conservée
     * pour rejouer/vérifier le tirage public.</p>
     *
     * <p>Réutilise le moteur : extraction des qualifiés, placement, best-of et
     * petite finale sont appliqués par {@link MoteurTournoi#construireTableau}.</p>
     *
     * @param tableau le tableau dont on monte la phase finale
     * @param graine  graine du tirage du tableau à élimination (reproductible)
     * @return le tableau à élimination construit
     * @throws IllegalStateException    si le tableau n'est pas en phase de poules,
     *                                  ou si toutes les poules ne sont pas terminées
     * @throws IllegalArgumentException si le tableau n'a pas de format défini
     */
    public TableauElimination construirePhaseFinale(Tableau tableau, long graine) {
        Objects.requireNonNull(tableau, "tableau");

        if (tableau.getStatut() != StatutTableau.LANCE) {
            throw new IllegalStateException(
                    "La phase finale ne peut être construite que depuis la phase de poules (statut actuel : "
                            + tableau.getStatut() + ").");
        }
        if (!tableau.poulesTerminees()) {
            throw new IllegalStateException(
                    "Toutes les poules doivent être terminées avant de construire la phase finale.");
        }
        FormatTournoi format = tableau.getConfiguration().getFormat();
        if (format == null) {
            throw new IllegalArgumentException(
                    "Le tableau n'a pas de format défini : impossible de construire la phase finale.");
        }

        TableauElimination elimination =
                moteur.construireTableau(tableau.getPoules(), format, graine);
        tableau.definirPhaseFinale(elimination);
        return elimination;
    }
}
