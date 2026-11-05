package com.pingmanager.core;

import com.pingmanager.model.Joueur;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Service gérant le <b>pointage</b> et le <b>repêchage</b> d'un tableau, le jour
 * du tournoi (mécanisme de présence).
 *
 * <p>À distinguer nettement du mécanisme de remplissage (capacité + liste
 * d'attente bornée), porté par {@link ServiceInscription} et
 * {@link Tableau#inscrire}. Ici on ne gère pas le nombre d'inscrits mais leur
 * <b>présence</b> et leur <b>paiement</b>, qui sont des faits <b>par tableau</b> :
 * un même joueur peut être pointé/payé sur un tableau et pas sur un autre.</p>
 *
 * <p>Il n'existe qu'une seule liste d'attente par tableau, continue : elle amortit
 * les désistements pendant l'inscription et alimente le tableau le jour J.</p>
 *
 * <p>La clôture du pointage d'un tableau, via {@link #cloturerPointage}, applique
 * la séquence :</p>
 * <ol>
 *   <li>les inscrits qui n'ont pas pointé pour ce tableau sont retirés (place
 *       libérée), quel que soit leur paiement ;</li>
 *   <li>on repêche la liste d'attente dans l'ordre, tant qu'il reste de la place
 *       et des candidats ;</li>
 *   <li>tout repêché doit obligatoirement passer par le pointage : il n'a ni
 *       pointé ni payé ce tableau (au pointage, il réglera son engagement). Le
 *       juge-arbitre confirme un repêché arrivé via {@link #confirmerRepeche}, ou
 *       le retire via {@link #retirerRepeche}.</li>
 * </ol>
 *
 * <p>Chaque tableau clôture son pointage indépendamment (les horaires de fin de
 * pointage diffèrent d'un tableau à l'autre).</p>
 */
public final class ServicePointage {

    private final Tournoi tournoi;
    private final PolitiqueOuverture politiqueOuverture;

    public ServicePointage(Tournoi tournoi) {
        this(tournoi, PolitiqueOuverture.REFUSER_SI_ANNULE);
    }

    /**
     * @param tournoi            le tournoi concerné
     * @param politiqueOuverture règle d'autorisation selon le statut du tournoi
     */
    public ServicePointage(Tournoi tournoi, PolitiqueOuverture politiqueOuverture) {
        this.tournoi = Objects.requireNonNull(tournoi, "tournoi");
        this.politiqueOuverture = Objects.requireNonNull(politiqueOuverture, "politiqueOuverture");
    }

    /**
     * Pointe un joueur pour un tableau donné (présence + confirmation sur CE
     * tableau). Le paiement est un acte distinct ({@link #marquerPaye}).
     */
    public void pointer(Tableau tableau, Joueur joueur) {
        exigerTableauDuTournoi(tableau);
        tableau.pointer(joueur);
    }

    /** Annule le pointage d'un joueur sur un tableau (correction du juge-arbitre). */
    public void depointer(Tableau tableau, Joueur joueur) {
        exigerTableauDuTournoi(tableau);
        tableau.depointer(joueur);
    }

    /** Marque le paiement de l'engagement d'un tableau par un joueur. */
    public void marquerPaye(Tableau tableau, Joueur joueur) {
        exigerTableauDuTournoi(tableau);
        tableau.marquerPaye(joueur);
    }

    /**
     * Annule le paiement d'un joueur sur un tableau (correction d'une erreur de
     * saisie au pointage, par exemple une case cochée par mégarde).
     */
    public void annulerPaiement(Tableau tableau, Joueur joueur) {
        exigerTableauDuTournoi(tableau);
        tableau.annulerPaiement(joueur);
    }

    /**
     * Inscrits qui n'ont pas (encore) pointé sur ce tableau. Après la clôture, ce
     * sont les repêchés « à pointer » : le juge-arbitre doit les confirmer
     * ({@link #confirmerRepeche}) ou les retirer ({@link #retirerRepeche}).
     */
    public static List<Joueur> nonPointes(Tableau tableau) {
        Objects.requireNonNull(tableau, "tableau");
        List<Joueur> res = new ArrayList<>();
        for (Joueur j : tableau.getInscrits()) {
            if (!tableau.aPointe(j)) {
                res.add(j);
            }
        }
        return res;
    }

    /**
     * Clôture le pointage d'un tableau : libère les places des inscrits non
     * pointés et repêche la liste d'attente pour combler.
     *
     * @param tableau le tableau dont on clôture le pointage
     * @return le compte rendu des libérations et repêchages
     */
    public ResultatPointage cloturerPointage(Tableau tableau) {
        exigerTableauDuTournoi(tableau);

        // 1) Libérer les inscrits qui n'ont pas pointé POUR CE TABLEAU (le paiement
        // n'entre pas en compte : un joueur payé mais absent libère sa place).
        List<Joueur> liberes = new ArrayList<>();
        for (Joueur inscrit : new ArrayList<>(tableau.getInscrits())) {
            if (!tableau.aPointe(inscrit)) {
                tableau.retirerInscrit(inscrit);
                liberes.add(inscrit);
            }
        }

        // 2) + 3) Repêcher la liste d'attente dans l'ordre. Tout repêché est « à
        // pointer » : il n'a ni pointé ni payé ce tableau.
        List<Joueur> repeches = new ArrayList<>();
        Joueur repeche;
        while ((repeche = tableau.repecherPremier()) != null) {
            repeches.add(repeche);
        }

        // Le pointage du tableau est désormais clôturé : la liste des présents est
        // figée. C'est un fait (pré-condition du lancement), pas un changement
        // d'état : le tableau reste en phase POINTAGE jusqu'au lancement.
        tableau.marquerPointageCloture();

        return new ResultatPointage(liberes, repeches);
    }

    /**
     * Correction manuelle du juge-arbitre : confirme un joueur repêché « à
     * pointer » qui est finalement arrivé (le pointe pour ce tableau). Le paiement
     * reste à marquer séparément via {@link #marquerPaye} si besoin.
     */
    public void confirmerRepeche(Tableau tableau, Joueur joueur) {
        exigerTableauDuTournoi(tableau);
        if (!tableau.getInscrits().contains(joueur)) {
            throw new IllegalArgumentException("Ce joueur n'est pas inscrit à ce tableau.");
        }
        tableau.pointer(joueur);
    }

    /**
     * Correction manuelle du juge-arbitre : retire définitivement un joueur repêché
     * « à pointer » qui ne s'est pas présenté, et repêche à sa place le candidat
     * suivant de la liste d'attente.
     *
     * @return le joueur repêché en remplacement, ou {@code null} si aucun
     */
    public Joueur retirerRepeche(Tableau tableau, Joueur joueur) {
        exigerTableauDuTournoi(tableau);
        if (!tableau.retirerInscrit(joueur)) {
            throw new IllegalArgumentException("Ce joueur n'est pas inscrit à ce tableau.");
        }
        return tableau.repecherPremier();
    }

    /**
     * Désistement d'un joueur <b>pendant</b> le pointage (statut OUVERT) : il libère
     * sa place, et le premier de la liste d'attente est repêché à sa place.
     *
     * <p>Le désistement n'est recevable de façon « normale » que tant que le
     * pointage est ouvert. Après clôture, utiliser
     * {@link #desisterCasParticulier(Tableau, Joueur, boolean)} (blessure…).</p>
     *
     * @return le joueur repêché en remplacement, ou {@code null} si aucun
     * @throws IllegalStateException si le pointage du tableau est déjà clôturé
     */
    public Joueur desister(Tableau tableau, Joueur joueur) {
        exigerTableauDuTournoi(tableau);
        if (tableau.pointageEstCloture()) {
            throw new IllegalStateException(
                    "Pointage clôturé : désistement normal non recevable. "
                            + "Le joueur est tenu de jouer. Utiliser desisterCasParticulier pour un cas particulier.");
        }
        if (!tableau.retirerInscrit(joueur)) {
            throw new IllegalArgumentException("Ce joueur n'est pas inscrit à ce tableau.");
        }
        return tableau.repecherPremier();
    }

    /**
     * Désistement pour <b>cas particulier</b> (blessure…), à la main du
     * juge-arbitre. Fonctionne quel que soit le statut du pointage, y compris après
     * clôture. Permet de tracer un remboursement.
     *
     * <p>Le fil rouge « garder un moyen de manipuler les données » : cette opération
     * autorise ce que la règle automatique interdirait après clôture.</p>
     *
     * @param rembourse true pour marquer le joueur comme remboursé (traçabilité)
     * @return le joueur repêché en remplacement, ou {@code null} si aucun (ou si le
     *         pointage est clôturé : on ne repêche plus automatiquement après coup)
     */
    public Joueur desisterCasParticulier(Tableau tableau, Joueur joueur, boolean rembourse) {
        exigerTableauDuTournoi(tableau);
        if (!tableau.getInscrits().contains(joueur)) {
            throw new IllegalArgumentException("Ce joueur n'est pas inscrit à ce tableau.");
        }
        if (rembourse) {
            tableau.marquerRembourse(joueur);
        }
        boolean avantCloture = !tableau.pointageEstCloture();
        tableau.retirerInscrit(joueur);
        // Avant clôture on repêche encore ; après clôture, la liste est figée et le
        // repêchage éventuel reste une décision manuelle du JA.
        return avantCloture ? tableau.repecherPremier() : null;
    }

    private void exigerTableauDuTournoi(Tableau tableau) {
        Objects.requireNonNull(tableau, "tableau");
        if (!tournoi.getTableaux().contains(tableau)) {
            throw new IllegalArgumentException("Le tableau n'appartient pas à ce tournoi.");
        }
        if (!politiqueOuverture.autorise(tournoi.getStatut())) {
            throw new IllegalStateException(
                    "Opération de pointage impossible : le tournoi est " + tournoi.getStatut() + ".");
        }
    }
}
