package com.pingmanager.core;

import com.pingmanager.model.Joueur;
import com.pingmanager.model.ModeReglement;
import com.pingmanager.model.MotifRetrait;
import com.pingmanager.model.OperationPointage;
import com.pingmanager.model.StatutTableau;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.TypeOperationPointage;

import java.util.Objects;

/**
 * Actions d'un <b>poste de pointage</b> (B041) : souvent un autre ordinateur que
 * celui du juge-arbitre, où l'on coche simplement les présents et les paiements.
 *
 * <p>Chaque action passe par {@link ServicePointage} (mêmes règles que le
 * pointage du jour J), puis est ajoutée au journal du tournoi sous forme
 * d'{@link OperationPointage} horodatée (port {@link Horloge}) et signée par
 * l'identifiant du poste. Le joueur est désigné par sa licence et le tableau par
 * son nom : l'écran relit le fichier avant chaque action, et une opération se
 * rejouera telle quelle lors d'une future synchronisation.</p>
 *
 * <p>Règles appliquées faute de texte fédéral précis (option la plus prudente,
 * question posée en section 6 du README) :</p>
 * <ul>
 *   <li>tableau lancé : plus de pointage ni de désistement depuis le poste ;
 *       le paiement reste modifiable (c'est de la caisse, pas du sportif) ;</li>
 *   <li>pointage clôturé : on peut encore pointer (repêché arrivé), mais ni
 *       dépointer ni désinscrire : c'est au juge-arbitre de décider
 *       ({@link ServicePointage#desisterCasParticulier}) ;</li>
 *   <li>une désinscription exige un motif ({@link com.pingmanager.model.MotifRetrait},
 *       B043 : décision du joueur ou autre motif) ; elle libère la place et
 *       repêche le premier de la liste d'attente ({@link ServicePointage#desister}) ;
 *       un paiement déjà noté n'est pas marqué remboursé (décision du club) ;</li>
 *   <li>B043 : un retrait s'annule seulement s'il reste une place libre
 *       ({@link #annulerRetrait}).</li>
 * </ul>
 */
public final class ServicePostePointage {

    private final Tournoi tournoi;
    private final ServicePointage pointage;
    private final PolitiqueOuverture politique;
    private final Horloge horloge;
    private final String poste;

    /**
     * @param tournoi le tournoi (relu depuis le fichier juste avant l'action)
     * @param horloge l'heure des opérations
     * @param poste   identifiant du poste (nom de l'ordinateur…)
     */
    public ServicePostePointage(Tournoi tournoi, Horloge horloge, String poste) {
        this.tournoi = Objects.requireNonNull(tournoi, "tournoi");
        this.politique = PolitiqueOuverture.REFUSER_SI_ANNULE;
        this.pointage = new ServicePointage(tournoi, politique);
        this.horloge = Objects.requireNonNull(horloge, "horloge");
        if (poste == null || poste.isBlank()) {
            throw new IllegalArgumentException("Identifiant du poste obligatoire.");
        }
        this.poste = poste.trim();
    }

    public String getPoste() {
        return poste;
    }

    /** Le joueur est présent pour ce tableau (ou un repêché est arrivé, après clôture). */
    public ResultatActionPointage pointer(String nomTableau, String licence) {
        Cible c = cible(nomTableau, licence);
        if (c.refus != null) {
            return ResultatActionPointage.refusee(c.refus);
        }
        if (estLance(c.tableau)) {
            return ResultatActionPointage.refusee(ResultatActionPointage.Motif.TABLEAU_LANCE);
        }
        if (c.tableau.aPointe(c.joueur)) {
            return ResultatActionPointage.sansChangement();
        }
        pointage.pointer(c.tableau, c.joueur);
        return journaliser(c, TypeOperationPointage.POINTER, null, null);
    }

    /** Correction : retire le pointage (refusé après clôture ou lancement). */
    public ResultatActionPointage depointer(String nomTableau, String licence) {
        Cible c = cible(nomTableau, licence);
        if (c.refus != null) {
            return ResultatActionPointage.refusee(c.refus);
        }
        if (estLance(c.tableau)) {
            return ResultatActionPointage.refusee(ResultatActionPointage.Motif.TABLEAU_LANCE);
        }
        if (c.tableau.pointageEstCloture()) {
            return ResultatActionPointage.refusee(ResultatActionPointage.Motif.POINTAGE_CLOTURE);
        }
        if (!c.tableau.aPointe(c.joueur)) {
            return ResultatActionPointage.sansChangement();
        }
        pointage.depointer(c.tableau, c.joueur);
        return journaliser(c, TypeOperationPointage.DEPOINTER, null, null);
    }

    /** L'engagement de ce tableau est réglé, avec ce mode (changer de mode est permis). */
    public ResultatActionPointage marquerPaye(String nomTableau, String licence, ModeReglement mode) {
        if (mode == null) {
            return ResultatActionPointage.refusee(ResultatActionPointage.Motif.MODE_OBLIGATOIRE);
        }
        Cible c = cible(nomTableau, licence);
        if (c.refus != null) {
            return ResultatActionPointage.refusee(c.refus);
        }
        if (c.tableau.aPaye(c.joueur) && c.tableau.modeReglement(c.joueur) == mode) {
            return ResultatActionPointage.sansChangement();
        }
        pointage.marquerPaye(c.tableau, c.joueur);
        c.tableau.marquerPaye(c.joueur, mode);
        return journaliser(c, TypeOperationPointage.MARQUER_PAYE, mode.name(), null);
    }

    /** Correction : annule le paiement (case cochée par erreur). */
    public ResultatActionPointage marquerNonPaye(String nomTableau, String licence) {
        Cible c = cible(nomTableau, licence);
        if (c.refus != null) {
            return ResultatActionPointage.refusee(c.refus);
        }
        if (!c.tableau.aPaye(c.joueur)) {
            return ResultatActionPointage.sansChangement();
        }
        pointage.annulerPaiement(c.tableau, c.joueur);
        return journaliser(c, TypeOperationPointage.MARQUER_NON_PAYE, null, null);
    }

    /**
     * Désistement du joueur sur ce tableau, avec un motif en texte libre (B041) :
     * noté comme une décision du joueur, le texte servant de précision.
     */
    public ResultatActionPointage desinscrire(String nomTableau, String licence, String motif) {
        if (motif == null || motif.isBlank()) {
            return ResultatActionPointage.refusee(ResultatActionPointage.Motif.MOTIF_OBLIGATOIRE);
        }
        return desinscrire(nomTableau, licence, MotifRetrait.DECISION_JOUEUR, motif);
    }

    /**
     * B043 : désinscription du joueur de ce tableau, pour un motif de la liste
     * (décision du joueur, blessure…, « Autre » avec une précision obligatoire) :
     * la place est libérée et le premier de la liste d'attente est repêché (il
     * devra pointer et payer). Refusée après la clôture du pointage ou le
     * lancement : le juge-arbitre décide. Le motif, l'heure, le poste, le jour et
     * le paiement éventuel sont notés au journal (aucun remboursement
     * automatique : le club décide).
     */
    public ResultatActionPointage desinscrire(String nomTableau, String licence, MotifRetrait motif,
                                              String precision) {
        if (motif == null) {
            return ResultatActionPointage.refusee(ResultatActionPointage.Motif.MOTIF_OBLIGATOIRE);
        }
        if (motif.exigePrecision() && (precision == null || precision.isBlank())) {
            return ResultatActionPointage.refusee(ResultatActionPointage.Motif.PRECISION_OBLIGATOIRE);
        }
        Cible c = cible(nomTableau, licence);
        if (c.refus != null) {
            return ResultatActionPointage.refusee(c.refus);
        }
        if (estLance(c.tableau)) {
            return ResultatActionPointage.refusee(ResultatActionPointage.Motif.TABLEAU_LANCE);
        }
        if (c.tableau.pointageEstCloture()) {
            return ResultatActionPointage.refusee(ResultatActionPointage.Motif.POINTAGE_CLOTURE);
        }
        boolean avaitPaye = c.tableau.aPaye(c.joueur);
        Joueur repeche = pointage.desister(c.tableau, c.joueur);
        c.tableau.noterRetire(c.joueur);
        OperationPointage op = JournalRetraits.operation(tournoi, c.tableau, c.joueur,
                TypeOperationPointage.DESINSCRIRE, motif, precision, avaitPaye, horloge, poste);
        tournoi.ajouterOperationPointage(op);
        return ResultatActionPointage.acceptee(op, repeche);
    }

    /**
     * B043 : annule le dernier retrait (désinscription ou non pointé) de ce joueur
     * dans ce tableau : il est réinscrit, <b>seulement si une place est libre</b>
     * (jamais de dépassement du nombre de places, jamais en liste d'attente). Le
     * joueur réinscrit doit (re)pointer ; son paiement n'est pas rétabli
     * d'office (le club le note au besoin). Refusé sur un tableau lancé.
     */
    public ResultatActionPointage annulerRetrait(String nomTableau, String licence) {
        if (!politique.autorise(tournoi.getStatut())) {
            return ResultatActionPointage.refusee(ResultatActionPointage.Motif.TOURNOI_FERME);
        }
        Tableau tableau = tableau(nomTableau);
        if (tableau == null) {
            return ResultatActionPointage.refusee(ResultatActionPointage.Motif.TABLEAU_INCONNU);
        }
        if (!new ListeRetraits(tournoi).estRetire(nomTableau, licence)) {
            return ResultatActionPointage.refusee(ResultatActionPointage.Motif.RETRAIT_INTROUVABLE);
        }
        if (estLance(tableau)) {
            return ResultatActionPointage.refusee(ResultatActionPointage.Motif.TABLEAU_LANCE);
        }
        Joueur joueur = ListeRetraits.ficheDuJoueur(tournoi, tableau, licence);
        if (joueur == null) {
            return ResultatActionPointage.refusee(ResultatActionPointage.Motif.FICHE_JOUEUR_ABSENTE);
        }
        if (tableau.getInscrits().contains(joueur) || tableau.getFileAttente().contains(joueur)) {
            return ResultatActionPointage.refusee(ResultatActionPointage.Motif.DEJA_INSCRIT);
        }
        if (tableau.estPlein()) {
            return ResultatActionPointage.refusee(ResultatActionPointage.Motif.PLUS_DE_PLACE);
        }
        if (!tableau.getConfiguration().joueurEligible(joueur)) {
            return ResultatActionPointage.refusee(ResultatActionPointage.Motif.NON_ELIGIBLE);
        }
        // Place libre et joueur éligible, ni inscrit ni en attente : inscrit directement.
        tableau.inscrire(joueur);
        tableau.oublierRetire(joueur);
        OperationPointage op = JournalRetraits.operation(tournoi, tableau, joueur,
                TypeOperationPointage.ANNULER_RETRAIT, null, null, false, horloge, poste);
        tournoi.ajouterOperationPointage(op);
        return ResultatActionPointage.acceptee(op, null);
    }

    // ------------------------------------------------------------------

    private ResultatActionPointage journaliser(Cible c, TypeOperationPointage type, String valeur, Joueur repeche) {
        OperationPointage op = OperationPointage.builder()
                .licence(c.joueur.getNumeroLicence())
                .tableau(c.tableau.getNom())
                .type(type)
                .valeur(valeur)
                .horodatage(horloge.maintenant())
                .poste(poste)
                .jour(JoursPointage.jourDe(tournoi, c.tableau)) // B043 : le pointage reste par tableau
                .build();
        tournoi.ajouterOperationPointage(op);
        return ResultatActionPointage.acceptee(op, repeche);
    }

    static boolean estLance(Tableau t) {
        return t.getStatut() == StatutTableau.LANCE || t.getStatut() == StatutTableau.PHASE_FINALE;
    }

    private Cible cible(String nomTableau, String licence) {
        if (!politique.autorise(tournoi.getStatut())) {
            return new Cible(ResultatActionPointage.Motif.TOURNOI_FERME, null, null);
        }
        Tableau tableau = tableau(nomTableau);
        if (tableau == null) {
            return new Cible(ResultatActionPointage.Motif.TABLEAU_INCONNU, null, null);
        }
        for (Joueur j : tableau.getInscrits()) {
            if (j.getNumeroLicence().equals(licence)) {
                return new Cible(null, tableau, j);
            }
        }
        return new Cible(ResultatActionPointage.Motif.JOUEUR_NON_INSCRIT, tableau, null);
    }

    private Tableau tableau(String nomTableau) {
        for (Tableau t : tournoi.getTableaux()) {
            if (t.getNom().equals(nomTableau)) {
                return t;
            }
        }
        return null;
    }

    private static final class Cible {
        private final ResultatActionPointage.Motif refus;
        private final Tableau tableau;
        private final Joueur joueur;

        private Cible(ResultatActionPointage.Motif refus, Tableau tableau, Joueur joueur) {
            this.refus = refus;
            this.tableau = tableau;
            this.joueur = joueur;
        }
    }
}
