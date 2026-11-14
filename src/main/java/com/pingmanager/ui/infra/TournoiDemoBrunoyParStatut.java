package com.pingmanager.ui.infra;

import com.pingmanager.core.ResultatTransition;
import com.pingmanager.core.ServiceCycleTournoi;
import com.pingmanager.model.Coordonnees;
import com.pingmanager.model.FicheLicence;
import com.pingmanager.model.Tournoi;

import java.util.List;

/**
 * B047 : copies du National B de Brunoy ({@link TournoiDemoBrunoy}), une par statut, pour
 * comparer l'interface de chaque état d'un même tournoi. Le brouillon reste l'original ;
 * la copie au jour J est {@link TournoiDemoBrunoyJourJ}.
 *
 * <ul>
 *   <li><b>demande d'homologation</b> : dossier complet, pas encore de numéro ;</li>
 *   <li><b>homologation validée</b> : numéro (fictif) enregistré, pas encore publié,
 *       tableaux remplis de joueurs fictifs ;</li>
 *   <li><b>publié</b> : règlement figé, inscriptions ouvertes, tableaux remplis de joueurs
 *       fictifs (avec listes d'attente).</li>
 * </ul>
 *
 * <p>Les changements de statut passent par {@link ServiceCycleTournoi} (mêmes contrôles que
 * dans l'application) et les inscriptions par le service d'inscription
 * ({@link TournoisDemo#remplir}). Comme pour la copie au jour J, les personnes
 * (juges-arbitres, correspondant), le lien de paiement, les joueurs et le numéro
 * d'homologation sont <b>fictifs</b>.</p>
 */
public final class TournoiDemoBrunoyParStatut {

    public static final String NOM_DEMANDE = TournoiDemoBrunoy.NOM + " — demande d'homologation";
    public static final String NOM_HOMOLOGUE = TournoiDemoBrunoy.NOM + " — homologation validée";
    public static final String NOM_PUBLIE = TournoiDemoBrunoy.NOM + " — publié";

    /** Graine du remplissage : la même pour les deux copies remplies (mêmes inscrits). */
    private static final long GRAINE = 1011L;

    private TournoiDemoBrunoyParStatut() {
    }

    /** Copie en demande d'homologation : dossier complet, sans numéro ni inscrit. */
    public static Tournoi demandeHomologation() {
        Tournoi t = base(NOM_DEMANDE);
        exiger(new ServiceCycleTournoi().demanderHomologation(t), "demande d'homologation");
        return t;
    }

    /**
     * Copie homologuée (numéro fictif), pas encore publiée, remplie de joueurs fictifs.
     *
     * @param joueurs licenciés fictifs (la réserve de la démo)
     */
    public static Tournoi homologue(List<FicheLicence> joueurs) {
        Tournoi t = base(NOM_HOMOLOGUE);
        ServiceCycleTournoi cycle = new ServiceCycleTournoi();
        exiger(cycle.demanderHomologation(t), "demande d'homologation");
        exiger(cycle.homologuer(t, TournoisDemo.HOMOLOGATION_BRUNOY_DEMO), "homologation");
        TournoisDemo.remplir(t, joueurs, TournoiDemoBrunoyJourJ.cibles(t), GRAINE, null);
        return t;
    }

    /**
     * Copie publiée (numéro fictif, règlement figé), remplie de joueurs fictifs.
     *
     * @param joueurs licenciés fictifs (la réserve de la démo)
     */
    public static Tournoi publie(List<FicheLicence> joueurs) {
        Tournoi t = base(NOM_PUBLIE);
        ServiceCycleTournoi cycle = new ServiceCycleTournoi();
        exiger(cycle.demanderHomologation(t), "demande d'homologation");
        exiger(cycle.homologuer(t, TournoisDemo.HOMOLOGATION_BRUNOY_DEMO), "homologation");
        exiger(cycle.publier(t), "publication");
        TournoisDemo.remplir(t, joueurs, TournoiDemoBrunoyJourJ.cibles(t), GRAINE, null);
        return t;
    }

    /** Le National B, renommé, géolocalisé (carte) et sans personne réelle. */
    private static Tournoi base(String nom) {
        Tournoi t = TournoiDemoBrunoy.construire();
        t.setNom(nom);
        t.setAdresse(t.getAdresse().avecCoordonnees(new Coordonnees(48.6929, 2.5115)));
        TournoiDemoBrunoyJourJ.personnesFictives(t);
        return t;
    }

    private static void exiger(ResultatTransition r, String etape) {
        if (!r.estEffectuee()) {
            throw new IllegalStateException("Copie de démo du National B : " + etape + " refusée (" + r + ").");
        }
    }
}
