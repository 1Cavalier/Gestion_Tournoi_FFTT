package com.pingmanager.core;

import com.pingmanager.model.GradeJugeArbitre;
import com.pingmanager.model.GrilleRecompenses;
import com.pingmanager.model.InfosReglement;
import com.pingmanager.model.NiveauTournoi;
import com.pingmanager.model.Recompense;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.StatutTournoi;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.TypeRecompense;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Service pilotant le cycle de vie d'un {@link Tournoi} :
 * {@code BROUILLON → HOMOLOGUE → PUBLIE}, avec {@code ANNULE} en sortie.
 *
 * <p>Le logiciel ne délivre pas l'homologation (c'est le rôle de la FFTT) : il
 * <b>rassemble et vérifie les paramètres requis</b> à chaque étape, puis suit
 * l'état. Deux natures de refus, volontairement distinctes :</p>
 * <ul>
 *   <li>une <b>transition structurellement impossible</b> (p. ex. publier un
 *       brouillon, homologuer un tournoi annulé) lève une
 *       {@link IllegalStateException} — c'est un bug appelant, pas un cas
 *       utilisateur ;</li>
 *   <li>un <b>dossier incomplet</b> (champs du règlement manquants, aucun tableau,
 *       pas de tableau féminin, numéro d'homologation absent) renvoie un
 *       {@link ResultatTransition} listant précisément ce qui manque.</li>
 * </ul>
 *
 * <p>Les pré-requis appliqués :</p>
 * <ul>
 *   <li><b>Homologuer</b> : le socle règlement IX.111 doit être complet et
 *       cohérent, au moins un tableau doit exister et l'un d'eux doit être réservé
 *       aux dames (IX.104), et un numéro d'homologation FFTT doit être fourni —
 *       c'est lui qui matérialise l'homologation.</li>
 *   <li><b>Publier</b> : le tournoi doit être homologué (donc numéro déjà présent) ;
 *       la publication ouvre les inscriptions.</li>
 *   <li><b>Annuler</b> : possible depuis tout état non terminal.</li>
 * </ul>
 *
 * <p>Service sans état : il opère sur le {@link Tournoi} qu'on lui passe et n'en
 * conserve aucune trace, conformément à la règle d'architecture ({@code core}
 * dépend de {@code model}, jamais l'inverse ni d'une UI/persistance).</p>
 */
public final class ServiceCycleTournoi {

    /**
     * Vérifie les pré-requis d'homologation puis, s'ils sont réunis, fige le
     * numéro d'homologation et fait passer le tournoi en
     * {@link StatutTournoi#HOMOLOGUE}.
     *
     * @param tournoi           le tournoi à homologuer (doit être en BROUILLON)
     * @param numeroHomologation numéro délivré par la FFTT (obligatoire)
     * @return {@link ResultatTransition#effectuee()} si l'homologation est faite,
     *         sinon {@link ResultatTransition#dossierIncomplet(List)} avec le détail
     * @throws IllegalStateException si le tournoi n'est pas en BROUILLON
     */
    public ResultatTransition homologuer(Tournoi tournoi, String numeroHomologation) {
        Objects.requireNonNull(tournoi, "tournoi");
        if (tournoi.getStatut() != StatutTournoi.DEMANDE_HOMOLOGATION) {
            exigerStatut(tournoi, StatutTournoi.BROUILLON, "homologuer");
        }

        // Complétude du dossier de demande (grille H). Le numéro d'homologation
        // n'en fait pas partie : il est délivré par la FFTT, pas saisi par le club.
        List<String> manquants = new ArrayList<>(elementsManquantsPourHomologation(tournoi));

        // En revanche, enregistrer l'homologation suppose que la FFTT a répondu :
        // le numéro délivré est requis à cet instant (il matérialise l'homologation).
        if (estVide(numeroHomologation)) {
            manquants.add("numéro d'homologation délivré par la FFTT");
        }

        if (!manquants.isEmpty()) {
            return ResultatTransition.dossierIncomplet(manquants);
        }

        tournoi.setNumeroHomologation(numeroHomologation.trim());
        tournoi.changerStatut(StatutTournoi.HOMOLOGUE);
        return ResultatTransition.effectuee();
    }

    /**
     * Fait la demande d'homologation : vérifie que le dossier (grille H) est
     * complet, puis passe le tournoi de {@link StatutTournoi#BROUILLON} à
     * {@link StatutTournoi#DEMANDE_HOMOLOGATION}. Le numéro n'est pas exigé : il
     * sera délivré en réponse à la demande.
     *
     * @throws IllegalStateException si le tournoi n'est pas en BROUILLON
     */
    public ResultatTransition demanderHomologation(Tournoi tournoi) {
        Objects.requireNonNull(tournoi, "tournoi");
        exigerStatut(tournoi, StatutTournoi.BROUILLON, "demander l'homologation de");
        List<String> manquants = elementsManquantsPourHomologation(tournoi);
        if (!manquants.isEmpty()) {
            return ResultatTransition.dossierIncomplet(manquants);
        }
        tournoi.changerStatut(StatutTournoi.DEMANDE_HOMOLOGATION);
        return ResultatTransition.effectuee();
    }

    /** Retire la demande d'homologation : retour au brouillon. */
    public void retirerDemande(Tournoi tournoi) {
        Objects.requireNonNull(tournoi, "tournoi");
        exigerStatut(tournoi, StatutTournoi.DEMANDE_HOMOLOGATION, "retirer la demande de");
        tournoi.changerStatut(StatutTournoi.BROUILLON);
    }

    /**
     * Publie le tournoi depuis la demande d'homologation ou l'état homologué : si le
     * tournoi est encore en demande, le numéro d'homologation saisi (section
     * Général) enregistre d'abord l'homologation, puis la publication ouvre les
     * inscriptions. Rien n'est modifié si l'une des deux étapes échoue.
     */
    public ResultatTransition publierDepuisDemande(Tournoi tournoi) {
        Objects.requireNonNull(tournoi, "tournoi");
        if (tournoi.getStatut() == StatutTournoi.DEMANDE_HOMOLOGATION) {
            List<String> manquants = new ArrayList<>(elementsManquantsPourPublication(tournoi));
            if (estVide(tournoi.getNumeroHomologation())
                    && !manquants.contains("numéro d'homologation")) {
                manquants.add(0, "numéro d'homologation délivré par la FFTT");
            }
            if (!manquants.isEmpty()) {
                return ResultatTransition.dossierIncomplet(manquants);
            }
            ResultatTransition h = homologuer(tournoi, tournoi.getNumeroHomologation());
            if (!h.estEffectuee()) {
                return h;
            }
        }
        return publier(tournoi);
    }

    /**
     * Publie un tournoi homologué : ouvre les inscriptions.
     *
     * @param tournoi le tournoi à publier (doit être en HOMOLOGUE)
     * @return {@link ResultatTransition#effectuee()} en cas de succès, sinon le
     *         détail des manques (par sécurité, si le numéro d'homologation avait
     *         disparu)
     * @throws IllegalStateException si le tournoi n'est pas en HOMOLOGUE
     */
    public ResultatTransition publier(Tournoi tournoi) {
        Objects.requireNonNull(tournoi, "tournoi");
        exigerStatut(tournoi, StatutTournoi.HOMOLOGUE, "publier");

        List<String> manquants = elementsManquantsPourPublication(tournoi);
        if (!manquants.isEmpty()) {
            return ResultatTransition.dossierIncomplet(manquants);
        }

        // La dotation globale est calculée automatiquement à partir des tableaux.
        tournoi.setDotationGlobale(calculerDotationGlobale(tournoi));

        tournoi.changerStatut(StatutTournoi.PUBLIE);
        return ResultatTransition.effectuee();
    }

    /**
     * Calcule la dotation globale d'un tournoi : somme, sur tous les tableaux, des
     * récompenses en argent, chaque entrée de grille étant comptée autant de fois
     * qu'elle couvre de places (une entrée « 1ᵉ→3ᵉ place : 50 € » compte 3 × 50 €).
     *
     * @param tournoi le tournoi
     * @return la dotation globale en euros (≥ 0)
     */
    public double calculerDotationGlobale(Tournoi tournoi) {
        Objects.requireNonNull(tournoi, "tournoi");
        double total = 0.0;
        for (Tableau t : tournoi.getTableaux()) {
            GrilleRecompenses grille = t.getConfiguration().getRecompenses();
            if (grille == null) {
                continue;
            }
            for (GrilleRecompenses.Entree e : grille.getEntrees()) {
                Recompense r = e.getRecompense();
                if (r != null && r.getType() == TypeRecompense.ARGENT) {
                    int nbPlaces = e.getPlaceFin() - e.getPlaceDebut() + 1;
                    total += r.getMontant() * nbPlaces;
                }
            }
        }
        return total;
    }

    /**
     * Annule un tournoi. Possible tant qu'il n'est pas déjà annulé.
     *
     * @param tournoi le tournoi à annuler
     * @throws IllegalStateException si le tournoi est déjà annulé
     */
    public void annuler(Tournoi tournoi) {
        Objects.requireNonNull(tournoi, "tournoi");
        if (tournoi.getStatut().estTerminal()) {
            throw new IllegalStateException("Le tournoi est déjà annulé.");
        }
        tournoi.changerStatut(StatutTournoi.ANNULE);
    }

    // -------------------------------------------------------------------------
    // Calcul des pré-requis (exposé publiquement : l'UI peut l'appeler à tout
    // moment pour afficher ce qu'il reste à faire, sans tenter la transition).
    // -------------------------------------------------------------------------

    /**
     * Liste, en clair, tout ce qui manque pour homologuer le tournoi (règlement,
     * tableaux, tableau féminin, numéro). Une liste vide signifie « prêt à
     * homologuer ».
     *
     * @param tournoi            le tournoi
     * @param numeroHomologation le numéro envisagé (peut être {@code null} à ce stade)
     * @return les éléments manquants (jamais {@code null})
     */
    public List<String> elementsManquantsPourHomologation(Tournoi tournoi) {
        Objects.requireNonNull(tournoi, "tournoi");
        List<String> manquants = new ArrayList<>();

        // --- Correspondant (nom, prénom, email) ---
        if (estVide(tournoi.getCorrespondantNom())) {
            manquants.add("correspondant : nom");
        }
        if (estVide(tournoi.getCorrespondantPrenom())) {
            manquants.add("correspondant : prénom");
        }
        if (estVide(tournoi.getCorrespondantEmail())) {
            manquants.add("correspondant : email");
        }

        // --- Juge-arbitre (au moins un, saisi) ---
        if (tournoi.getJugesArbitres().isEmpty()) {
            manquants.add("un juge-arbitre (nom, prénom, licence, grade)");
        } else if (!gradeJugeArbitreConforme(tournoi)) {
            // IX.104 : grade du juge-arbitre selon la catégorie (dérogation possible en promotionnel).
            manquants.add("un juge-arbitre de grade " + tournoi.getNiveau().getGradeJugeArbitreRequis().getLibelle()
                    + " au moins pour un tournoi " + tournoi.getNiveau().getLibelleMinuscule()
                    + " (IX.104)");
        }

        // --- Tournoi : nom, catégorie, dates début/fin ---
        // La catégorie (IX.102) remplace l'ancien « type » : elle détermine aussi
        // l'échelon qui homologue (IX.103), donc elle est due dès la demande.
        if (estVide(tournoi.getNom())) {
            manquants.add("nom du tournoi");
        }
        if (tournoi.getNiveau() == null) {
            manquants.add("catégorie du tournoi");
        }
        if (tournoi.getDateDebut() == null) {
            manquants.add("date de début");
        }
        if (tournoi.getDateFin() == null) {
            manquants.add("date de fin");
        }

        // --- Adresse (voie, code postal, ville) ---
        var adresse = tournoi.getAdresse();
        if (adresse == null || !adresse.estComplete()) {
            manquants.add("adresse du tournoi (voie, code postal, ville)");
        }

        // --- Tableaux : au moins un, au moins un féminin (IX.104),
        //     et chaque tableau doit avoir nom, type et date (début des matchs) ---
        List<Tableau> tableaux = tournoi.getTableaux();
        if (tableaux.isEmpty()) {
            manquants.add("au moins un tableau");
        } else {
            if (!auMoinsUnTableauFeminin(tableaux)) {
                manquants.add("au moins un tableau réservé aux dames (IX.104)");
            }
            for (Tableau t : tableaux) {
                String ref = estVide(t.getNom()) ? "un tableau" : "tableau « " + t.getNom() + " »";
                if (estVide(t.getNom())) {
                    manquants.add(ref + " : nom");
                }
                if (estVide(t.getType())) {
                    manquants.add(ref + " : type");
                }
                if (t.getHoraires() == null || t.getHoraires().getDebutMatchs() == null) {
                    manquants.add(ref + " : date (début des matchs)");
                }
            }
        }

        return manquants;
    }

    /**
     * @deprecated Le numéro d'homologation ne fait plus partie de la complétude
     * du dossier de demande (il est délivré par la FFTT, pas saisi par le club).
     * Utiliser {@link #elementsManquantsPourHomologation(Tournoi)}. Cette surcharge
     * ignore le second paramètre et subsiste pour compatibilité.
     */
    @Deprecated
    public List<String> elementsManquantsPourHomologation(Tournoi tournoi, String numeroIgnore) {
        return elementsManquantsPourHomologation(tournoi);
    }

    /**
     * les inscriptions). Le niveau publication est notre décision produit : il
     * exige, en plus de l'homologation, ce qui est nécessaire pour faire tourner
     * le tournoi.
     *
     * <p>Pré-requis vérifiés : nombre de tables, numéro
     * d'homologation, document de règlement (PDF), et — pour le règlement IX.111 —
     * la politique des balles, les dates de clôture des inscriptions et du
     * pointage et le début des matchs. Enfin, <b>chaque tableau</b> doit être
     * complet pour la publication (format de compétition et capacité).</p>
     *
     * @param tournoi le tournoi
     * @return les éléments manquants (jamais {@code null})
     */
    public List<String> elementsManquantsPourPublication(Tournoi tournoi) {
        Objects.requireNonNull(tournoi, "tournoi");
        List<String> manquants = new ArrayList<>();

        // (La catégorie est exigée dès l'homologation, voir plus haut.)
        // Nombre de tables.
        if (tournoi.getNombreDeTables() <= 0) {
            manquants.add("nombre de tables");
        }
        // Numéro d'homologation (reçu de la FFTT).
        if (estVide(tournoi.getNumeroHomologation())) {
            manquants.add("numéro d'homologation");
        }
        // Document de règlement (PDF).
        if (!tournoi.aUnReglement()) {
            manquants.add("document de règlement (PDF)");
        }

        // Règlement IX.111 : politique balles, clôtures, début des matchs.
        InfosReglement infos = tournoi.getInfosReglement();
        if (infos == null) {
            manquants.add("règlement du tournoi (politique balles, clôtures, début des matchs)");
        } else {
            if (infos.getPolitiqueBalles() == null) {
                manquants.add("politique des balles");
            }
            if (infos.getClotureInscriptions() == null) {
                manquants.add("date de clôture des inscriptions");
            }
            // La clôture du pointage n'est plus une échéance du tournoi : chaque
            // tableau porte ses propres heures de pointage.
            if (infos.getOuvertureInscriptions() == null) {
                manquants.add("date d'ouverture des inscriptions");
            }
            // Le début des matchs se règle désormais tableau par tableau (exigé
            // dès l'homologation pour chaque tableau).
        }

        // Chaque tableau doit être complet pour la publication.
        for (Tableau t : tournoi.getTableaux()) {
            manquants.addAll(t.getConfiguration().champsManquantsPourPublication(t.getNom()));
        }

        return manquants;
    }

    /**
     * Avertissements non bloquants à afficher avant homologation. Aujourd'hui :
     * signale si aucun juge-arbitre n'a le grade attendu pour le niveau du
     * tournoi (quand un niveau est renseigné). N'empêche jamais l'homologation.
     *
     * @param tournoi le tournoi
     * @return la liste des avertissements (jamais {@code null})
     */
    public List<String> avertissementsPourHomologation(Tournoi tournoi) {
        Objects.requireNonNull(tournoi, "tournoi");
        List<String> avertissements = new ArrayList<>();
        NiveauTournoi niveau = tournoi.getNiveau();
        if (niveau != null && !tournoi.getJugesArbitres().isEmpty()) {
            GradeJugeArbitre requis = niveau.getGradeJugeArbitreRequis();
            if (!tournoi.possedeJugeArbitreDeGrade(requis) && niveau.derogationJugeArbitrePossible()) {
                avertissements.add("pas de " + requis.getLibelle() + " : l'avis préalable de la commission "
                        + "d'arbitrage est requis (IX.104)");
            }
        }
        return avertissements;
    }

    /**
     * IX.104 : vrai si un juge-arbitre couvre le grade requis par la catégorie,
     * si la catégorie n'est pas encore choisie, ou si une dérogation est possible
     * (promotionnel, avec avis de la commission d'arbitrage).
     */
    public static boolean gradeJugeArbitreConforme(Tournoi tournoi) {
        NiveauTournoi niveau = tournoi.getNiveau();
        if (niveau == null || niveau.derogationJugeArbitrePossible()) {
            return true;
        }
        return tournoi.possedeJugeArbitreDeGrade(niveau.getGradeJugeArbitreRequis());
    }

    /** @return true si au moins un tableau est réservé aux dames. */
    private static boolean auMoinsUnTableauFeminin(List<Tableau> tableaux) {
        for (Tableau t : tableaux) {
            if (t.getConfiguration() != null
                    && t.getConfiguration().getSexeRequis() == Sexe.FEMME) {
                return true;
            }
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static void exigerStatut(Tournoi tournoi, StatutTournoi attendu, String action) {
        if (tournoi.getStatut() != attendu) {
            throw new IllegalStateException(
                    "Impossible de « " + action + " » : le tournoi est " + tournoi.getStatut()
                            + " (attendu : " + attendu + ").");
        }
    }

    private static boolean estVide(String s) {
        return s == null || s.isBlank();
    }
}
