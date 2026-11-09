package com.pingmanager.ui.infra;

import com.pingmanager.core.JoueurDepuisLicence;
import com.pingmanager.core.ServiceInscription;
import com.pingmanager.model.Coordonnees;
import com.pingmanager.model.FicheLicence;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.StatutTournoi;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Tournoi <b>publié</b> de la démo : le National B de Brunoy, publié (n° d'homologation
 * fictif) et rempli par des joueurs fictifs. Sa copie au jour J est
 * {@link TournoiDemoBrunoyJourJ}.
 *
 * <p>Toutes les inscriptions passent par {@link ServiceInscription} : quotas, tableau
 * dames imposé, tableaux consécutifs, points, catégories, capacité et liste d'attente
 * sont respectés. Joueurs et numéro d'homologation sont <b>fictifs</b>.</p>
 */
public final class TournoisDemo {

    /** Numéro d'homologation fictif de Brunoy (le vrai sera délivré par la FFTT). */
    public static final String HOMOLOGATION_BRUNOY_DEMO = "DÉMO-NB-2027-091 (fictif)";

    private TournoisDemo() {
    }

    // ==================================================================
    // Brunoy : publié et rempli
    // ==================================================================

    /**
     * Objectifs de remplissage du National B (inscrits + attente) : complets avec liste
     * d'attente (A, H), complet sans attente (C), à moitié (B, E, L…), presque vides (F, K, M).
     */
    private static final Map<String, Integer> CIBLES_BRUNOY = Map.ofEntries(
            Map.entry("A", 70), Map.entry("B", 31), Map.entry("C", 60), Map.entry("D", 21),
            Map.entry("E", 44), Map.entry("F", 5), Map.entry("G", 17),
            Map.entry("H", 66), Map.entry("I", 27), Map.entry("J", 50), Map.entry("K", 8),
            Map.entry("L", 36), Map.entry("M", 3));

    /** National B de Brunoy, publié (n° d'homologation fictif) et rempli par des joueurs fictifs. */
    public static Tournoi brunoyPublieEtRempli(List<FicheLicence> joueurs) {
        Tournoi t = TournoiDemoBrunoy.construire();
        t.setNumeroHomologation(HOMOLOGATION_BRUNOY_DEMO);
        t.setAdresse(t.getAdresse().avecCoordonnees(new Coordonnees(48.6929, 2.5115)));
        publier(t);
        remplir(t, joueurs, CIBLES_BRUNOY, 7L, null);
        return t;
    }

    /**
     * Remplit chaque tableau jusqu'à sa cible (inscrits + attente), joueuses d'abord au
     * tableau dames (imposé), en tirant des joueurs au hasard (graine fixe) et en passant
     * par {@link ServiceInscription} : un refus (quota, tableaux consécutifs, points,
     * catégorie…) fait simplement passer au joueur suivant.
     */
    static void remplir(Tournoi t, List<FicheLicence> joueurs, Map<String, Integer> cibles, long graine,
                        java.util.function.Predicate<FicheLicence> admis) {
        ServiceInscription service = ServiceInscription.sansContrainteSuccession();
        List<Joueur> candidats = new ArrayList<>();
        for (FicheLicence f : joueurs) {
            if (admis == null || admis.test(f)) {
                candidats.add(JoueurDepuisLicence.creer(f, t));
            }
        }
        Random r = new Random(graine);
        List<Tableau> ordre = new ArrayList<>(t.getTableaux());
        // Le tableau des joueuses d'abord (il conditionne leurs autres tableaux du jour).
        String dames = t.getQuotasTableaux().getTableauBonusJoueuses();
        ordre.sort(java.util.Comparator.comparing((Tableau x) -> x.getNom().equals(dames) ? 0 : 1));
        for (Tableau tab : ordre) {
            Integer cible = cibles.get(cle(tab.getNom()));
            if (cible == null) {
                continue;
            }
            List<Joueur> melange = new ArrayList<>(candidats);
            Collections.shuffle(melange, r);
            for (Joueur j : melange) {
                if (tab.getInscrits().size() + tab.getFileAttente().size() >= cible) {
                    break;
                }
                service.inscrire(t, j, tab);
            }
        }
    }

    /** « A », ou la lettre avant le tiret (« G – Toutes séries dames » → « G »), ou le nom. */
    private static String cle(String nom) {
        int i = nom.indexOf(" – ");
        return i > 0 ? nom.substring(0, i) : nom;
    }

    static void publier(Tournoi t) {
        t.changerStatut(StatutTournoi.HOMOLOGUE);
        t.changerStatut(StatutTournoi.PUBLIE);
    }
}
