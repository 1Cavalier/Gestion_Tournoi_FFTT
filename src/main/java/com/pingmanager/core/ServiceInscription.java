package com.pingmanager.core;

import com.pingmanager.model.CategorieAge;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.QuotasTableaux;
import com.pingmanager.model.RegleTableauxParJour;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.ordonnancement.ValidationInscription;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Service d'inscription d'un joueur à un tableau, à l'échelle du <b>tournoi</b>.
 *
 * <p>Il fait le pont entre le modèle (qui ne connaît pas les règles inter-tableaux)
 * et les règles de niveau tournoi. Pour chaque tentative, il applique, dans l'ordre :</p>
 * <ol>
 *   <li>l'<b>éligibilité</b> au tableau visé (classement, rang national, sexe) —
 *       déléguée à {@link com.pingmanager.model.ConfigurationTableau#joueurEligible(Joueur)} ;</li>
 *   <li>le <b>quota de tableaux par jour</b> du tournoi
 *       ({@link RegleTableauxParJour}), avec l'exception féminine éventuelle ;</li>
 *   <li>la règle configurable « <b>pas deux tableaux qui se suivent</b> »
 *       ({@link ValidationInscription}) ;</li>
 *   <li>la délégation à {@link Tableau#inscrire(Joueur)}, qui gère la capacité
 *       (bascule en file d'attente) et les doublons.</li>
 * </ol>
 *
 * <p>Le service ne modifie rien tant que toutes les règles amont ne sont pas
 * satisfaites : un refus (quota, tableaux successifs, non-éligibilité) n'inscrit
 * pas le joueur et ne le met pas en file d'attente. Le résultat renvoyé
 * ({@link ResultatInscriptionTournoi}) porte le motif précis.</p>
 *
 * <p>Conforme à la règle d'architecture : ce service vit dans {@code core} et peut
 * donc dépendre de {@code model} et {@code ordonnancement}, sans qu'aucun de ces
 * paquets ne dépende de l'UI ou de la persistance.</p>
 */
public final class ServiceInscription {

    private final ValidationInscription validationTableauxSuccessifs;
    private final PolitiqueOuverture politiqueOuverture;

    /**
     * @param validationTableauxSuccessifs règle configurable de succession des
     *        tableaux ; utiliser {@link ValidationInscription#desactivee()} pour
     *        ne pas l'appliquer
     */
    public ServiceInscription(ValidationInscription validationTableauxSuccessifs) {
        this(validationTableauxSuccessifs, PolitiqueOuverture.REFUSER_SI_ANNULE);
    }

    /**
     * @param validationTableauxSuccessifs règle de succession des tableaux
     * @param politiqueOuverture           politique d'acceptation selon le statut du tournoi
     */
    public ServiceInscription(ValidationInscription validationTableauxSuccessifs,
                              PolitiqueOuverture politiqueOuverture) {
        this.validationTableauxSuccessifs =
                Objects.requireNonNull(validationTableauxSuccessifs, "validationTableauxSuccessifs");
        this.politiqueOuverture =
                Objects.requireNonNull(politiqueOuverture, "politiqueOuverture");
    }

    /** Service sans contrainte de succession de tableaux. */
    public static ServiceInscription sansContrainteSuccession() {
        return new ServiceInscription(ValidationInscription.desactivee());
    }

    /**
     * Tente d'inscrire un joueur à un tableau du tournoi.
     *
     * @param tournoi      le tournoi (fournit la règle de quota et l'univers des tableaux)
     * @param joueur       le joueur candidat
     * @param tableauCible le tableau visé (doit appartenir au tournoi)
     * @return le résultat détaillé de la tentative
     */
    public ResultatInscriptionTournoi inscrire(Tournoi tournoi, Joueur joueur, Tableau tableauCible) {
        Objects.requireNonNull(tournoi, "tournoi");
        Objects.requireNonNull(joueur, "joueur");
        Objects.requireNonNull(tableauCible, "tableauCible");
        if (!tournoi.getTableaux().contains(tableauCible)) {
            throw new IllegalArgumentException("Le tableau cible n'appartient pas à ce tournoi.");
        }
        ResultatInscriptionTournoi refus = verifierRegles(tournoi, joueur, tableauCible, List.of());
        if (refus != null) {
            return refus;
        }
        // 4) Délégation au tableau (capacité + file d'attente + doublon bas niveau).
        return traduire(tableauCible.inscrire(joueur));
    }

    /**
     * Essai à blanc d'une <b>sélection</b> de tableaux, sans rien modifier : pour la
     * validation en direct d'un formulaire d'inscription. Les tableaux sont examinés
     * dans l'{@link #ordreDeTraitement(Tournoi, java.util.Collection) ordre de
     * traitement} (tableau des joueuses d'abord, puis par heure de début) et chaque
     * tableau accepté compte pour les suivants (quotas, tableaux successifs, tableau
     * des joueuses obligatoire). La capacité est prévue : inscrit, liste d'attente,
     * ou liste d'attente pleine.
     *
     * @return le résultat prévu pour chaque tableau de la sélection, dans l'ordre de traitement
     */
    public java.util.LinkedHashMap<Tableau, ResultatInscriptionTournoi> simuler(
            Tournoi tournoi, Joueur joueur, java.util.Collection<Tableau> selection) {
        Objects.requireNonNull(tournoi, "tournoi");
        Objects.requireNonNull(joueur, "joueur");
        java.util.LinkedHashMap<Tableau, ResultatInscriptionTournoi> res = new java.util.LinkedHashMap<>();
        List<Tableau> acceptes = new ArrayList<>();
        for (Tableau t : ordreDeTraitement(tournoi, selection)) {
            ResultatInscriptionTournoi r = verifierRegles(tournoi, joueur, t, acceptes);
            if (r == null) {
                r = prevoirCapacite(t);
            }
            if (r.estAccepte()) {
                acceptes.add(t);
            }
            res.put(t, r);
        }
        return res;
    }

    /**
     * Inscrit réellement le joueur aux tableaux choisis, dans le même ordre que
     * {@link #simuler}. Tout ou rien : si la simulation refuse au moins un tableau,
     * rien n'est modifié et les résultats simulés sont renvoyés.
     *
     * @return le résultat de chaque tableau, dans l'ordre de traitement
     */
    public java.util.LinkedHashMap<Tableau, ResultatInscriptionTournoi> inscrireSelection(
            Tournoi tournoi, Joueur joueur, java.util.Collection<Tableau> selection) {
        java.util.LinkedHashMap<Tableau, ResultatInscriptionTournoi> prevu = simuler(tournoi, joueur, selection);
        if (prevu.values().stream().anyMatch(r -> !r.estAccepte())) {
            return prevu;
        }
        java.util.LinkedHashMap<Tableau, ResultatInscriptionTournoi> res = new java.util.LinkedHashMap<>();
        for (Tableau t : prevu.keySet()) {
            res.put(t, inscrire(tournoi, joueur, t));
        }
        return res;
    }

    /**
     * Ordre dans lequel une sélection est traitée : le tableau des joueuses
     * ({@code QuotasTableaux.getTableauBonusJoueuses()}) d'abord, puisqu'il peut
     * conditionner les autres le même jour ; puis par jour et heure de début ;
     * les tableaux sans horaire en dernier, par nom.
     */
    public static List<Tableau> ordreDeTraitement(Tournoi tournoi, java.util.Collection<Tableau> selection) {
        String bonus = tournoi.getQuotasTableaux().getTableauBonusJoueuses();
        List<Tableau> res = new ArrayList<>(new java.util.LinkedHashSet<>(selection));
        res.sort(java.util.Comparator
                .comparing((Tableau t) -> bonus != null && bonus.equals(t.getNom()) ? 0 : 1)
                .thenComparing(t -> t.getHoraires() == null || t.getHoraires().getDebutMatchs() == null
                        ? java.time.LocalDateTime.MAX : t.getHoraires().getDebutMatchs())
                .thenComparing(Tableau::getNom));
        return res;
    }

    /**
     * Règles amont (tout sauf la capacité). {@code enPlus} : tableaux supposés déjà
     * acceptés (simulation), qui comptent comme des inscriptions du joueur.
     *
     * @return le refus, ou {@code null} si toutes les règles sont satisfaites
     */
    private ResultatInscriptionTournoi verifierRegles(Tournoi tournoi, Joueur joueur, Tableau tableauCible,
                                                      List<Tableau> enPlus) {
        // 0) Le tournoi accepte-t-il les inscriptions dans son statut actuel ?
        if (!politiqueOuverture.autorise(tournoi.getStatut())) {
            return ResultatInscriptionTournoi.tournoiFerme();
        }

        // Le joueur est-il déjà présent (inscrit ou en attente) sur le tableau visé ?
        if (estPresent(tableauCible, joueur) || enPlus.contains(tableauCible)) {
            return ResultatInscriptionTournoi.dejaPresent();
        }

        // 1) Éligibilité au tableau visé (classement, rang national, sexe).
        if (!tableauCible.getConfiguration().joueurEligible(joueur)) {
            return ResultatInscriptionTournoi.nonEligible();
        }

        // 1 bis) Catégorie d'âge : catégories non admises au tournoi (choix du club),
        //        puis catégories prévues par le tableau. Catégorie inconnue : pas de contrôle.
        CategorieAge categorie = joueur.getCategorie();
        if (categorie != null) {
            if (tournoi.getCategoriesExclues().contains(categorie)) {
                return ResultatInscriptionTournoi.categorieNonAdmise();
            }
            if (!tableauCible.getConfiguration().categorieAutorisee(categorie)) {
                return ResultatInscriptionTournoi.categorieNonAutorisee();
            }
        }

        // Tableaux du tournoi où le joueur est déjà présent (hors tableau cible).
        List<Tableau> dejaInscrit = tableauxDuJoueur(tournoi, joueur, tableauCible);
        for (Tableau t : enPlus) {
            if (t != tableauCible && !dejaInscrit.contains(t)) {
                dejaInscrit.add(t);
            }
        }

        // 2) Quota de tableaux par jour : règles d'inscription du tournoi (par jour,
        //    joueurs / joueuses, tableau exigé pour le surplus des joueuses) ;
        //    à défaut, ancienne règle unique avec exception féminine.
        QuotasTableaux quotas = tournoi.getQuotasTableaux();
        LocalDate jour = jourDe(tableauCible);

        // Clause d'organisateur : un joueur éliminé d'un tableau du jour peut prendre jusqu'à
        // N tableaux de plus, parmi ceux qui n'ont pas commencé. Les tableaux dont il est
        // éliminé ne comptent plus pour la règle des tableaux qui se suivent.
        List<Tableau> elimine = new ArrayList<>();
        if (quotas.isInscriptionApresEliminationAutorisee() && jour != null
                && EliminationTableau.nAPasCommence(tableauCible)) {
            for (Tableau t : dejaInscrit) {
                if (jour.equals(jourDe(t)) && EliminationTableau.estElimine(t, joueur)) {
                    elimine.add(t);
                }
            }
        }
        int bonusElimination = elimine.isEmpty() ? 0 : quotas.getTableauxApresElimination();
        List<Tableau> enLice = new ArrayList<>(dejaInscrit);
        enLice.removeAll(elimine);
        if (quotas.aLimite() && jour != null) {
            List<Tableau> memeJour = new ArrayList<>();
            for (Tableau t : dejaInscrit) {
                if (jour.equals(jourDe(t))) {
                    memeJour.add(t);
                }
            }
            String bonus = quotas.getTableauBonusJoueuses();
            boolean auTableauBonus = bonus != null && (bonus.equals(tableauCible.getNom())
                    || memeJour.stream().anyMatch(t -> bonus.equals(t.getNom())));
            Integer max = quotas.limite(jour, joueur.getSexe(), auTableauBonus);
            if (max != null && memeJour.size() + 1 > max + bonusElimination) {
                return ResultatInscriptionTournoi.quotaTableauxAtteint();
            }
        }
        // 2 bis) Tableau des joueuses obligatoire le jour où il est joué.
        if (jour != null && quotas.isTableauBonusObligatoire() && joueur.getSexe() == Sexe.FEMME
                && quotas.getTableauBonusJoueuses() != null
                && !quotas.getTableauBonusJoueuses().equals(tableauCible.getNom())) {
            Tableau feminin = tournoi.getTableaux().stream()
                    .filter(t -> t.getNom().equals(quotas.getTableauBonusJoueuses()))
                    .findFirst().orElse(null);
            if (feminin != null && jour.equals(jourDe(feminin)) && !dejaInscrit.contains(feminin)) {
                return ResultatInscriptionTournoi.tableauFemininObligatoire(feminin);
            }
        }

        // 2 ter) Pas deux tableaux qui se suivent le même jour (règle du tournoi).
        if (jour != null && quotas.isTableauxConsecutifsInterdits()) {
            Tableau voisin = voisinInscrit(tournoi, tableauCible, jour, enLice);
            if (voisin != null) {
                return ResultatInscriptionTournoi.tableauxSuccessifs(voisin);
            }
        }

        RegleTableauxParJour regle = tournoi.getRegleTableauxParJour();
        if (!quotas.aLimite() && regle.aLimite()) {
            boolean inscritTableauSpecial = estInscritAuTableauSpecial(regle, dejaInscrit, tableauCible);
            int max = regle.maxPour(joueur, inscritTableauSpecial);
            // dejaInscrit.size() tableaux déjà pris ; en ajouter un dépasserait-il le quota ?
            if (dejaInscrit.size() + 1 > (long) max + bonusElimination) {
                return ResultatInscriptionTournoi.quotaTableauxAtteint();
            }
        }

        // 3) Règle « pas deux tableaux qui se suivent ».
        ValidationInscription.ResultatValidation succ =
                validationTableauxSuccessifs.verifier(joueur, tableauCible, enLice);
        if (!succ.estAutorise()) {
            return ResultatInscriptionTournoi.tableauxSuccessifs(succ.getTableauEnConflit());
        }
        return null;
    }

    /** Capacité prévue sans modifier le tableau : inscrit, en attente ou attente pleine. */
    private static ResultatInscriptionTournoi prevoirCapacite(Tableau t) {
        if (!t.estPlein()) {
            return ResultatInscriptionTournoi.inscrit();
        }
        return t.fileAttentePleine() ? ResultatInscriptionTournoi.listeAttentePleine()
                : ResultatInscriptionTournoi.enFileAttente();
    }

    /** Traduction du résultat bas niveau (chaîne de {@code if} : pas de switch sur une énumération). */
    private static ResultatInscriptionTournoi traduire(Tableau.ResultatInscription bas) {
        if (bas == Tableau.ResultatInscription.INSCRIT) {
            return ResultatInscriptionTournoi.inscrit();
        }
        if (bas == Tableau.ResultatInscription.EN_FILE_ATTENTE) {
            return ResultatInscriptionTournoi.enFileAttente();
        }
        if (bas == Tableau.ResultatInscription.LISTE_ATTENTE_PLEINE) {
            return ResultatInscriptionTournoi.listeAttentePleine();
        }
        if (bas == Tableau.ResultatInscription.NON_ELIGIBLE) {
            return ResultatInscriptionTournoi.nonEligible();
        }
        return ResultatInscriptionTournoi.dejaPresent();
    }

    /**
     * Tableau où le joueur est déjà inscrit et qui précède ou suit immédiatement le
     * tableau cible dans l'ordre des débuts de ce jour-là ; {@code null} sinon.
     */
    private static Tableau voisinInscrit(Tournoi tournoi, Tableau cible, LocalDate jour, List<Tableau> dejaInscrit) {
        if (cible.getHoraires().getDebutMatchs() == null) {
            return null;
        }
        List<Tableau> duJour = new ArrayList<>();
        for (Tableau t : tournoi.getTableaux()) {
            if (jour.equals(jourDe(t)) && t.getHoraires().getDebutMatchs() != null) {
                duJour.add(t);
            }
        }
        duJour.sort(java.util.Comparator.comparing(t -> t.getHoraires().getDebutMatchs()));
        int i = duJour.indexOf(cible);
        for (int k : new int[] {i - 1, i + 1}) {
            if (i >= 0 && k >= 0 && k < duJour.size() && dejaInscrit.contains(duJour.get(k))) {
                return duJour.get(k);
            }
        }
        return null;
    }

    private static LocalDate jourDe(Tableau t) {
        return t.getHoraires() == null ? null : t.getHoraires().getJour();
    }

    private boolean estPresent(Tableau tableau, Joueur joueur) {
        return tableau.getInscrits().contains(joueur) || tableau.getFileAttente().contains(joueur);
    }

    private List<Tableau> tableauxDuJoueur(Tournoi tournoi, Joueur joueur, Tableau sauf) {
        List<Tableau> resultat = new ArrayList<>();
        for (Tableau t : tournoi.getTableaux()) {
            if (t == sauf) {
                continue;
            }
            if (estPresent(t, joueur)) {
                resultat.add(t);
            }
        }
        return resultat;
    }

    /**
     * Détermine si, pour l'application de l'exception féminine conditionnée, le
     * joueur est (ou serait) inscrit au tableau spécial. On considère le tableau
     * cible comme comptant s'il est lui-même le tableau spécial.
     */
    private boolean estInscritAuTableauSpecial(RegleTableauxParJour regle,
                                               List<Tableau> dejaInscrit,
                                               Tableau tableauCible) {
        Tableau special = regle.getTableauSpecial();
        if (special == null) {
            return false;
        }
        if (special == tableauCible) {
            return true;
        }
        return dejaInscrit.contains(special);
    }
}
