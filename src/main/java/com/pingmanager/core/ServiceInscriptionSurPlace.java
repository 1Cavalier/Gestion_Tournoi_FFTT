package com.pingmanager.core;

import com.pingmanager.model.Joueur;
import com.pingmanager.model.StatutTableau;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Inscription <b>sur place</b>, le jour J, à la table du juge-arbitre : un joueur (souvent
 * éliminé d'un tableau) s'inscrit à un tableau qui n'a pas encore commencé.
 *
 * <ul>
 *   <li>Seuls les tableaux dont le tirage n'est pas fait sont proposés
 *       ({@link EliminationTableau#nAPasCommence(Tableau)}).</li>
 *   <li>Il faut une place libre : pas de liste d'attente sur place (règlement, article
 *       « Inscriptions » : « s'il reste des places »).</li>
 *   <li>Toutes les règles du tournoi s'appliquent ({@link ServiceInscription}), avec la
 *       clause « nouveau tableau après élimination » si le club l'a choisie.</li>
 *   <li>Le joueur est présent : si le pointage du tableau est ouvert, il est pointé tout de
 *       suite, et marqué payé s'il règle sur place.</li>
 * </ul>
 */
public final class ServiceInscriptionSurPlace {

    /** Issue d'une inscription sur place. */
    public record Resultat(boolean accepte, String motif) {
        static Resultat ok() {
            return new Resultat(true, null);
        }

        static Resultat refus(String motif) {
            return new Resultat(false, motif);
        }
    }

    private final ServiceInscription inscription;

    public ServiceInscriptionSurPlace(ServiceInscription inscription) {
        this.inscription = Objects.requireNonNull(inscription, "inscription");
    }

    /** Avec les règles d'inscription du tournoi (sans l'ancienne validation des successions). */
    public ServiceInscriptionSurPlace() {
        this(ServiceInscription.sansContrainteSuccession());
    }

    /** Tableaux qui n'ont pas commencé, dans l'ordre du tournoi. */
    public static List<Tableau> tableauxOuverts(Tournoi tournoi) {
        List<Tableau> res = new ArrayList<>();
        for (Tableau t : tournoi.getTableaux()) {
            if (EliminationTableau.nAPasCommence(t)) {
                res.add(t);
            }
        }
        return res;
    }

    /** Tableaux du tournoi dont ce joueur est éliminé. */
    public static List<Tableau> tableauxElimines(Tournoi tournoi, Joueur joueur) {
        List<Tableau> res = new ArrayList<>();
        for (Tableau t : tournoi.getTableaux()) {
            if (EliminationTableau.estElimine(t, joueur)) {
                res.add(t);
            }
        }
        return res;
    }

    /**
     * Ce que donnerait l'inscription à chacun des tableaux ouverts, sans rien modifier :
     * {@code null} si elle est possible, sinon le motif en clair.
     */
    public Map<Tableau, String> verifier(Tournoi tournoi, Joueur joueur) {
        Map<Tableau, String> res = new LinkedHashMap<>();
        for (Tableau t : tableauxOuverts(tournoi)) {
            res.put(t, motifRefus(tournoi, joueur, t));
        }
        return res;
    }

    /**
     * Inscrit le joueur sur place.
     *
     * @param payeSurPlace le joueur règle son engagement à la table
     */
    public Resultat inscrire(Tournoi tournoi, Joueur joueur, Tableau tableau, boolean payeSurPlace) {
        String refus = motifRefus(tournoi, joueur, tableau);
        if (refus != null) {
            return Resultat.refus(refus);
        }
        ResultatInscriptionTournoi r = inscription.inscrire(tournoi, joueur, tableau);
        if (!r.estInscrit()) {
            return Resultat.refus(MotifsInscription.texte(r, tournoi, joueur, tableau));
        }
        ServicePointage pointage = new ServicePointage(tournoi);
        if (tableau.getStatut() == StatutTableau.POINTAGE) {
            pointage.pointer(tableau, joueur);
        }
        if (payeSurPlace) {
            pointage.marquerPaye(tableau, joueur);
        }
        return Resultat.ok();
    }

    private String motifRefus(Tournoi tournoi, Joueur joueur, Tableau tableau) {
        Objects.requireNonNull(tournoi, "tournoi");
        Objects.requireNonNull(joueur, "joueur");
        Objects.requireNonNull(tableau, "tableau");
        if (!EliminationTableau.nAPasCommence(tableau)) {
            return "Ce tableau a déjà commencé : le tirage est fait.";
        }
        if (tableau.getInscrits().contains(joueur)) {
            return "Déjà inscrit à ce tableau.";
        }
        if (tableau.estPlein()) {
            return "Complet : plus de place libre (pas de liste d'attente sur place).";
        }
        ResultatInscriptionTournoi r = inscription.simuler(tournoi, joueur, List.of(tableau)).get(tableau);
        if (r == null || r.estInscrit()) {
            return null;
        }
        return MotifsInscription.texte(r, tournoi, joueur, tableau);
    }
}
