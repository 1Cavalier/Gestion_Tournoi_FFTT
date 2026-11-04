package com.pingmanager.core;

import com.pingmanager.model.Club;
import com.pingmanager.model.FicheLicence;
import com.pingmanager.model.NiveauTournoi;
import com.pingmanager.model.Tournoi;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Accès d'un licencié à un tournoi selon sa <b>catégorie</b> (règlements sportifs
 * 2026-2027, IX.101 et IX.102). Service pur, sans état : réutilisé par le filtre de
 * la liste, la carte (marqueur grisé) et la fiche du tournoi.
 *
 * <ul>
 *   <li>IX.101 : les licenciés <b>loisir</b> (et les pass'tournoi) ne participent qu'aux
 *       tournois <b>promotionnels</b> et aux tableaux qui leur sont réservés ailleurs ;</li>
 *   <li>IX.102 : un tournoi <b>régional</b> est ouvert aux licenciés des associations de
 *       la région du club organisateur ; un tournoi <b>départemental</b>, à ceux des
 *       associations de son département. « Aucune dérogation ne peut être apportée à la
 *       présente classification. »</li>
 * </ul>
 *
 * <p>Le département d'un licencié est celui de son club ({@link FicheLicence#getNumeroClub()}
 * → {@link Club#getDepartement()}) ; celui d'un tournoi, celui du club organisateur (à
 * défaut, le code postal du lieu). La région est la ligue du département
 * ({@link Territoires}). Les contrôles propres à chaque tableau (points, sexe,
 * catégories d'âge) restent ceux de {@link ServiceInscription}.</p>
 */
public final class EligibiliteTournoi {

    /** Verdict d'accès. */
    public enum Statut {
        ACCESSIBLE,
        NON_ACCESSIBLE,
        /** Pas de licence liée, ou données insuffisantes pour conclure. */
        A_VERIFIER
    }

    /**
     * Résultat : le statut, le motif en clair (rouge à l'écran quand non accessible)
     * et une remarque éventuelle (ex. tableaux réservés aux licences loisir).
     */
    public record Acces(Statut statut, String motif, String remarque) {
        public boolean estAccessible() {
            return statut == Statut.ACCESSIBLE;
        }

        public boolean estRefuse() {
            return statut == Statut.NON_ACCESSIBLE;
        }
    }

    private final RegistreClubs clubs;

    public EligibiliteTournoi(RegistreClubs clubs) {
        this.clubs = Objects.requireNonNull(clubs, "clubs");
    }

    /**
     * @param tournoi le tournoi
     * @param fiche   la licence du joueur ({@code null} si aucune licence n'est liée au compte)
     */
    public Acces evaluer(Tournoi tournoi, FicheLicence fiche) {
        Objects.requireNonNull(tournoi, "tournoi");
        if (fiche == null) {
            return new Acces(Statut.A_VERIFIER, "Liez votre licence (Mon profil) pour vérifier votre accès.", null);
        }
        NiveauTournoi niveau = tournoi.getNiveau();
        if (niveau == null) {
            return new Acces(Statut.A_VERIFIER, "Catégorie du tournoi non précisée.", null);
        }

        // IX.101 : licence loisir → tournois promotionnels (ou tableaux réservés ailleurs).
        String remarque = null;
        if (estLicenceLoisir(fiche) && niveau != NiveauTournoi.PROMOTIONNEL) {
            if (!tournoi.accepteLicencePromotionnelle()) {
                return new Acces(Statut.NON_ACCESSIBLE,
                        "Licence loisir : réservé aux tournois promotionnels (IX.101).", null);
            }
            remarque = "Licence loisir : seulement les tableaux réservés aux licences loisir (IX.101).";
        }

        if (niveau == NiveauTournoi.DEPARTEMENTAL || niveau == NiveauTournoi.REGIONAL) {
            Optional<Territoires.Departement> depTournoi = departementDuTournoi(tournoi);
            Optional<Territoires.Departement> depJoueur = departementDuJoueur(fiche);
            if (depTournoi.isEmpty() || depJoueur.isEmpty()) {
                return new Acces(Statut.A_VERIFIER,
                        "Département du club organisateur ou de votre club inconnu.", remarque);
            }
            Territoires.Departement dt = depTournoi.get();
            if (niveau == NiveauTournoi.DEPARTEMENTAL && !dt.code().equals(depJoueur.get().code())) {
                return new Acces(Statut.NON_ACCESSIBLE, "Réservé aux licenciés " + dt.formeDe()
                        + " (" + dt.code() + ") — tournoi départemental (IX.102).", null);
            }
            if (niveau == NiveauTournoi.REGIONAL && !dt.ligue().equals(depJoueur.get().ligue())) {
                return new Acces(Statut.NON_ACCESSIBLE, "Réservé aux licenciés de la ligue "
                        + dt.ligue() + " — tournoi régional (IX.102).", null);
            }
        }
        return new Acces(Statut.ACCESSIBLE, null, remarque);
    }

    /** Département du tournoi : celui du club organisateur, à défaut le code postal du lieu. */
    public Optional<Territoires.Departement> departementDuTournoi(Tournoi t) {
        if (t.getNumeroAffiliationClub() != null) {
            Optional<Territoires.Departement> d = clubs.trouverParNumero(t.getNumeroAffiliationClub())
                    .flatMap(c -> Territoires.departement(c.getDepartement()));
            if (d.isPresent()) {
                return d;
            }
        }
        return t.getAdresse() == null ? Optional.empty()
                : Territoires.departementDuCodePostal(t.getAdresse().getCodePostal());
    }

    /**
     * Département du licencié : celui de son club ; à défaut, les 3e et 4e chiffres du
     * numéro de club FFTT (« 08911132 » → 91).
     */
    public Optional<Territoires.Departement> departementDuJoueur(FicheLicence f) {
        String num = f.getNumeroClub();
        if (num == null) {
            return Optional.empty();
        }
        Optional<Territoires.Departement> d = clubs.trouverParNumero(num)
                .flatMap(c -> Territoires.departement(c.getDepartement()));
        if (d.isPresent() || num.length() < 4) {
            return d;
        }
        return Territoires.departement(num.substring(2, 4));
    }

    /** Licence loisir (ou promotionnelle) d'après le type porté par la fiche. */
    public static boolean estLicenceLoisir(FicheLicence f) {
        String type = f.getTypeLicence() == null ? "" : f.getTypeLicence().toLowerCase(Locale.ROOT);
        return type.contains("loisir") || type.contains("promo");
    }
}
