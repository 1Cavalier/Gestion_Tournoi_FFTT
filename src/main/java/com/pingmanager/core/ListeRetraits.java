package com.pingmanager.core;

import com.pingmanager.model.Joueur;
import com.pingmanager.model.MotifRetrait;
import com.pingmanager.model.OperationPointage;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.TypeOperationPointage;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * B043 : la vue « Retraits » du poste de pointage, sans JavaFX. Elle sépare
 * clairement, par joueur et par tableau :
 *
 * <ul>
 *   <li>les <b>non pointés</b>, libérés à la clôture du pointage (jamais un choix
 *       du joueur) ;</li>
 *   <li>les <b>désinscrits par décision du joueur</b> ;</li>
 *   <li>les <b>désinscrits pour un autre motif</b> ({@link MotifRetrait}).</li>
 * </ul>
 *
 * <p>Tout vient du journal du pointage : pour chaque joueur et chaque tableau,
 * seule la dernière opération de retrait ou d'annulation compte ; un joueur de
 * nouveau inscrit au tableau (retrait annulé, inscription sur place) n'y figure
 * plus. Instantané : à reconstruire après chaque action.</p>
 */
public final class ListeRetraits {

    /** Nature d'un retrait. */
    public enum Nature {
        NON_POINTE("Non pointé"),
        DECISION_JOUEUR("Désinscrit — décision du joueur"),
        AUTRE_MOTIF("Désinscrit — autre motif");

        private final String libelle;

        Nature(String libelle) {
            this.libelle = libelle;
        }

        public String getLibelle() {
            return libelle;
        }
    }

    /**
     * Un retrait en cours.
     *
     * @param joueur      la fiche du joueur, ou {@code null} si elle n'a pas été gardée (retrait ancien)
     * @param motif       le motif d'une désinscription, {@code null} pour un non-pointé
     * @param precision   texte libre éventuel (motif « Autre », ancien motif B041)
     * @param jour        jour du tableau
     * @param avaitPaye   le joueur avait payé ce tableau : à rembourser ou à reporter (le club décide)
     * @param tableauLance le tableau est lancé : le retrait ne peut plus être annulé
     */
    public record Retrait(String licence, Joueur joueur, String tableau, LocalDate jour, Nature nature,
                          MotifRetrait motif, String precision, Instant horodatage, String poste,
                          boolean avaitPaye, boolean tableauLance) {
    }

    private final Tournoi tournoi;

    public ListeRetraits(Tournoi tournoi) {
        this.tournoi = Objects.requireNonNull(tournoi, "tournoi");
    }

    /** @return les retraits en cours de ce jour ({@code null} = tous), le plus récent d'abord. */
    public List<Retrait> retraits(LocalDate jour) {
        List<Retrait> res = new ArrayList<>();
        for (OperationPointage op : dernieresOperations().values()) {
            Retrait r = retrait(op);
            if (r != null && (jour == null || jour.equals(r.jour()))) {
                res.add(r);
            }
        }
        res.sort(Comparator.comparing(Retrait::horodatage).reversed());
        return res;
    }

    /** @return les non-pointés libérés à la clôture, ce jour. */
    public List<Retrait> nonPointes(LocalDate jour) {
        List<Retrait> res = new ArrayList<>();
        for (Retrait r : retraits(jour)) {
            if (r.nature() == Nature.NON_POINTE) {
                res.add(r);
            }
        }
        return res;
    }

    /** @return les désinscrits (décision du joueur ou autre motif), ce jour. */
    public List<Retrait> desinscrits(LocalDate jour) {
        List<Retrait> res = new ArrayList<>();
        for (Retrait r : retraits(jour)) {
            if (r.nature() != Nature.NON_POINTE) {
                res.add(r);
            }
        }
        return res;
    }

    /** @return vrai si ce joueur a un retrait en cours dans ce tableau. */
    public boolean estRetire(String nomTableau, String licence) {
        OperationPointage op = dernieresOperations().get(cle(nomTableau, licence));
        return op != null && retrait(op) != null;
    }

    /**
     * @return la fiche du joueur retiré : gardée par le tableau, sinon trouvée dans
     *         un autre tableau du tournoi, sinon {@code null}
     */
    public static Joueur ficheDuJoueur(Tournoi tournoi, Tableau tableau, String licence) {
        Joueur j = tableau.retrouverRetire(licence);
        if (j != null) {
            return j;
        }
        for (Tableau t : tournoi.getTableaux()) {
            for (Joueur x : t.getInscrits()) {
                if (x.getNumeroLicence().equals(licence)) {
                    return x;
                }
            }
            for (Joueur x : t.getFileAttente()) {
                if (x.getNumeroLicence().equals(licence)) {
                    return x;
                }
            }
            Joueur r = t.retrouverRetire(licence);
            if (r != null) {
                return r;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------

    /** Dernière opération de retrait ou d'annulation, par tableau et licence (ordre du journal). */
    private Map<String, OperationPointage> dernieresOperations() {
        Map<String, OperationPointage> res = new LinkedHashMap<>();
        for (OperationPointage op : tournoi.getJournalPointage()) {
            TypeOperationPointage t = op.getType();
            if (t == TypeOperationPointage.DESINSCRIRE || t == TypeOperationPointage.NON_POINTE
                    || t == TypeOperationPointage.ANNULER_RETRAIT) {
                res.put(cle(op.getTableau(), op.getLicence()), op);
            }
        }
        return res;
    }

    private static String cle(String tableau, String licence) {
        return tableau + "\u0000" + licence;
    }

    /** @return le retrait décrit par cette opération, ou {@code null} s'il n'est plus en cours. */
    private Retrait retrait(OperationPointage op) {
        if (op.getType() == TypeOperationPointage.ANNULER_RETRAIT) {
            return null;
        }
        Tableau tableau = null;
        for (Tableau t : tournoi.getTableaux()) {
            if (t.getNom().equals(op.getTableau())) {
                tableau = t;
            }
        }
        if (tableau == null) {
            return null; // tableau supprimé ou renommé depuis
        }
        for (Joueur j : tableau.getInscrits()) {
            if (j.getNumeroLicence().equals(op.getLicence())) {
                return null; // de nouveau inscrit (inscription sur place…)
            }
        }
        Nature nature;
        if (op.getType() == TypeOperationPointage.NON_POINTE) {
            nature = Nature.NON_POINTE;
        } else if (op.getMotifRetrait().estDecisionDuJoueur()) {
            nature = Nature.DECISION_JOUEUR;
        } else {
            nature = Nature.AUTRE_MOTIF;
        }
        LocalDate jour = op.getJour() != null ? op.getJour() : JoursPointage.jourDe(tournoi, tableau);
        return new Retrait(op.getLicence(), ficheDuJoueur(tournoi, tableau, op.getLicence()), tableau.getNom(),
                jour, nature, op.getMotifRetrait(), op.getPrecision(), op.getHorodatage(), op.getPoste(),
                op.avaitPaye(), ServicePostePointage.estLance(tableau));
    }
}
