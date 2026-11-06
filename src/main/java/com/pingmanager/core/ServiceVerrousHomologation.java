package com.pingmanager.core;

import com.pingmanager.model.AdresseTournoi;
import com.pingmanager.model.AffectationJugeArbitre;
import com.pingmanager.model.ChampHomologation;
import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.NiveauTournoi;
import com.pingmanager.model.StatutTournoi;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Verrous du dossier d'homologation (cahier des charges v0, section 6) : dès la
 * demande d'homologation, et ensuite (homologué, publié), les champs exigés pour
 * l'homologation ({@link ChampHomologation}, grille 🔴) ne sont plus
 * modifiables ; les autres champs le restent.
 *
 * <p>Chaque méthode {@code modifier…} applique la modification si elle est
 * permise et renvoie un {@link ResultatModificationTournoi} détaillé sinon (rien
 * n'est alors modifié). Redonner à un champ verrouillé <b>sa valeur actuelle</b>
 * est accepté : un formulaire peut donc tout réenregistrer, seuls les vrais
 * changements sont refusés. Même mécanisme que le règlement figé après
 * publication (B010), étendu au dossier d'homologation.</p>
 *
 * <p>Choix de l'application, pas une règle FFTT : un tournoi annulé n'est pas
 * verrouillé (comme le règlement de B010). Service sans état.</p>
 */
public final class ServiceVerrousHomologation {

    // -------------------------------------------------------------------------
    // Lecture (pour l'écran)
    // -------------------------------------------------------------------------

    /** @return les champs verrouillés du tournoi (tous dès la demande, aucun sinon). */
    public List<ChampHomologation> champsVerrouilles(Tournoi tournoi) {
        Objects.requireNonNull(tournoi, "tournoi");
        return tournoi.champsHomologationVerrouilles()
                ? List.of(ChampHomologation.values())
                : List.of();
    }

    /** @return vrai si ce champ ne peut plus être modifié. */
    public boolean estVerrouille(Tournoi tournoi, ChampHomologation champ) {
        Objects.requireNonNull(champ, "champ");
        return champsVerrouilles(tournoi).contains(champ);
    }

    /**
     * @return le motif en clair du verrou sur ces champs, selon le statut (par
     *         exemple pour une infobulle), ou {@code null} si rien n'est verrouillé
     */
    public String motifVerrou(Tournoi tournoi, List<ChampHomologation> champs) {
        Objects.requireNonNull(tournoi, "tournoi");
        if (!tournoi.champsHomologationVerrouilles() || champs == null || champs.isEmpty()) {
            return null;
        }
        StringBuilder liste = new StringBuilder();
        for (ChampHomologation c : champs) {
            if (liste.length() > 0) {
                liste.append(", ");
            }
            liste.append(c.getLibelle());
        }
        String objet = champs.size() == 1
                ? "« " + liste + " » fait partie du dossier d'homologation et ne peut plus être modifié"
                : "ces champs font partie du dossier d'homologation et ne peuvent plus être modifiés ("
                        + liste + ")";
        if (tournoi.getStatut() == StatutTournoi.DEMANDE_HOMOLOGATION) {
            return "L'homologation a été demandée : " + objet
                    + ". Retirez la demande pour le modifier.";
        }
        return "Le tournoi est homologué : " + objet + ".";
    }

    /** @return {@link ResultatModificationTournoi#estAppliquee()} vrai si le champ est modifiable. */
    public ResultatModificationTournoi verifier(Tournoi tournoi, ChampHomologation champ) {
        Objects.requireNonNull(champ, "champ");
        return controler(tournoi, List.of(champ));
    }

    // -------------------------------------------------------------------------
    // Section Général
    // -------------------------------------------------------------------------

    public ResultatModificationTournoi modifierNom(Tournoi tournoi, String nom) {
        Objects.requireNonNull(nom, "nom");
        ResultatModificationTournoi r = controlerSiChange(tournoi, ChampHomologation.NOM,
                !Objects.equals(tournoi.getNom(), nom));
        if (r.estAppliquee()) {
            tournoi.setNom(nom);
        }
        return r;
    }

    public ResultatModificationTournoi modifierCategorie(Tournoi tournoi, NiveauTournoi niveau) {
        ResultatModificationTournoi r = controlerSiChange(tournoi, ChampHomologation.CATEGORIE,
                tournoi.getNiveau() != niveau);
        if (r.estAppliquee()) {
            tournoi.setNiveau(niveau);
        }
        return r;
    }

    public ResultatModificationTournoi modifierClubOrganisateur(Tournoi tournoi, String club) {
        ResultatModificationTournoi r = controlerSiChange(tournoi, ChampHomologation.CLUB_ORGANISATEUR,
                !memeTexte(tournoi.getClubOrganisateur(), club));
        if (r.estAppliquee()) {
            tournoi.setClubOrganisateur(club);
        }
        return r;
    }

    public ResultatModificationTournoi modifierNumeroAffiliation(Tournoi tournoi, String numero) {
        ResultatModificationTournoi r = controlerSiChange(tournoi, ChampHomologation.NUMERO_AFFILIATION,
                !memeTexte(tournoi.getNumeroAffiliationClub(), numero));
        if (r.estAppliquee()) {
            tournoi.setNumeroAffiliationClub(numero);
        }
        return r;
    }

    /**
     * Adresse : seuls voie, code postal et ville sont verrouillés ; résidence,
     * lieu-dit et position géographique (géocodage) restent modifiables.
     */
    public ResultatModificationTournoi modifierAdresse(Tournoi tournoi, AdresseTournoi adresse) {
        AdresseTournoi actuelle = tournoi.getAdresse();
        boolean change = actuelle == null ? adresse != null : !actuelle.memeLieu(adresse);
        ResultatModificationTournoi r = controlerSiChange(tournoi, ChampHomologation.ADRESSE, change);
        if (r.estAppliquee()) {
            tournoi.setAdresse(adresse);
        }
        return r;
    }

    public ResultatModificationTournoi modifierPeriode(Tournoi tournoi, LocalDate debut, LocalDate fin) {
        boolean change = !Objects.equals(tournoi.getDateDebut(), debut)
                || !Objects.equals(tournoi.getDateFin(), fin);
        ResultatModificationTournoi r = controlerSiChange(tournoi, ChampHomologation.DATES, change);
        if (r.estAppliquee()) {
            tournoi.setPeriode(debut, fin);
        }
        return r;
    }

    /** Correspondant : nom, prénom et email verrouillés ; téléphone et autres infos libres. */
    public ResultatModificationTournoi modifierCorrespondant(Tournoi tournoi, String nom, String prenom,
                                                            String email, String telephone,
                                                            String autresInfos) {
        boolean change = !memeTexte(tournoi.getCorrespondantNom(), nom)
                || !memeTexte(tournoi.getCorrespondantPrenom(), prenom)
                || !memeTexte(tournoi.getCorrespondantEmail(), email);
        ResultatModificationTournoi r = controlerSiChange(tournoi, ChampHomologation.CORRESPONDANT, change);
        if (r.estAppliquee()) {
            tournoi.setCorrespondant(nom, prenom, email, telephone, autresInfos);
        }
        return r;
    }

    // -------------------------------------------------------------------------
    // Juges-arbitres
    // -------------------------------------------------------------------------

    public ResultatModificationTournoi ajouterJugeArbitre(Tournoi tournoi, AffectationJugeArbitre ja) {
        Objects.requireNonNull(ja, "ja");
        ResultatModificationTournoi r = verifier(tournoi, ChampHomologation.JUGES_ARBITRES);
        if (r.estAppliquee()) {
            tournoi.ajouterJugeArbitre(ja);
        }
        return r;
    }

    public ResultatModificationTournoi retirerJugeArbitre(Tournoi tournoi, String numeroLicence) {
        ResultatModificationTournoi r = verifier(tournoi, ChampHomologation.JUGES_ARBITRES);
        if (r.estAppliquee()) {
            tournoi.retirerJugeArbitre(numeroLicence);
        }
        return r;
    }

    /**
     * Remplace un juge-arbitre : nom, prénom, grade et rôle sont verrouillés ;
     * licence, email et téléphone restent modifiables (licence encore « à
     * renseigner » au moment de la demande, par exemple).
     *
     * @throws IllegalArgumentException si aucun juge-arbitre n'a cette licence
     */
    public ResultatModificationTournoi remplacerJugeArbitre(Tournoi tournoi, String numeroLicence,
                                                           AffectationJugeArbitre nouveau) {
        Objects.requireNonNull(nouveau, "nouveau");
        AffectationJugeArbitre ancien = null;
        for (AffectationJugeArbitre a : tournoi.getJugesArbitres()) {
            if (a.getNumeroLicence().equals(numeroLicence)) {
                ancien = a;
            }
        }
        if (ancien == null) {
            throw new IllegalArgumentException("Ce juge-arbitre n'est pas affecté au tournoi.");
        }
        boolean change = !memeTexte(ancien.getNom(), nouveau.getNom())
                || !memeTexte(ancien.getPrenom(), nouveau.getPrenom())
                || ancien.getGrade() != nouveau.getGrade()
                || ancien.getRole() != nouveau.getRole();
        ResultatModificationTournoi r = controlerSiChange(tournoi, ChampHomologation.JUGES_ARBITRES, change);
        if (r.estAppliquee()) {
            tournoi.remplacerJugeArbitre(numeroLicence, nouveau);
        }
        return r;
    }

    // -------------------------------------------------------------------------
    // Tableaux
    // -------------------------------------------------------------------------

    public ResultatModificationTournoi ajouterTableau(Tournoi tournoi, Tableau tableau) {
        Objects.requireNonNull(tableau, "tableau");
        ResultatModificationTournoi r = verifier(tournoi, ChampHomologation.LISTE_TABLEAUX);
        if (r.estAppliquee()) {
            tournoi.ajouterTableau(tableau);
        }
        return r;
    }

    /** @throws IllegalStateException si le tableau a des inscrits (règle existante du modèle) */
    public ResultatModificationTournoi retirerTableau(Tournoi tournoi, Tableau tableau) {
        Objects.requireNonNull(tableau, "tableau");
        ResultatModificationTournoi r = verifier(tournoi, ChampHomologation.LISTE_TABLEAUX);
        if (r.estAppliquee()) {
            tournoi.retirerTableau(tableau);
        }
        return r;
    }

    /**
     * Remplace un tableau par sa nouvelle version. Verrouillés : nom, catégorie
     * de points, public (dames, messieurs, mixte) et début des matchs (jour et
     * heure) ; format, places, tarif, récompenses, pointage… restent libres.
     *
     * @throws IllegalStateException si le tableau a des inscrits (règle existante du modèle)
     */
    public ResultatModificationTournoi remplacerTableau(Tournoi tournoi, Tableau ancien, Tableau nouveau) {
        Objects.requireNonNull(ancien, "ancien");
        Objects.requireNonNull(nouveau, "nouveau");
        List<ChampHomologation> changes = new ArrayList<>();
        if (!Objects.equals(ancien.getNom(), nouveau.getNom())) {
            changes.add(ChampHomologation.TABLEAU_NOM);
        }
        ConfigurationTableau ca = ancien.getConfiguration();
        ConfigurationTableau cn = nouveau.getConfiguration();
        if (ca.getModeLimite() != cn.getModeLimite()
                || ca.getPointsMin() != cn.getPointsMin()
                || ca.getPointsMax() != cn.getPointsMax()) {
            changes.add(ChampHomologation.TABLEAU_CATEGORIE_POINTS);
        }
        if (ca.getSexeRequis() != cn.getSexeRequis()) {
            changes.add(ChampHomologation.TABLEAU_PUBLIC);
        }
        if (!Objects.equals(debutMatchs(ancien), debutMatchs(nouveau))) {
            changes.add(ChampHomologation.TABLEAU_DEBUT);
        }
        ResultatModificationTournoi r = controler(tournoi, changes);
        if (r.estAppliquee()) {
            tournoi.remplacerTableau(ancien, nouveau);
        }
        return r;
    }

    // -------------------------------------------------------------------------
    // Outils
    // -------------------------------------------------------------------------

    private ResultatModificationTournoi controlerSiChange(Tournoi tournoi, ChampHomologation champ,
                                                         boolean change) {
        return change ? controler(tournoi, List.of(champ)) : controler(tournoi, List.of());
    }

    private ResultatModificationTournoi controler(Tournoi tournoi, List<ChampHomologation> champs) {
        Objects.requireNonNull(tournoi, "tournoi");
        if (champs.isEmpty() || !tournoi.champsHomologationVerrouilles()) {
            return ResultatModificationTournoi.appliquee();
        }
        return ResultatModificationTournoi.verrouille(champs, motifVerrou(tournoi, champs));
    }

    private static LocalDateTime debutMatchs(Tableau t) {
        return t.getHoraires() == null ? null : t.getHoraires().getDebutMatchs();
    }

    /** Compare deux textes saisis : espaces de bord ignorés, vide équivaut à absent. */
    private static boolean memeTexte(String a, String b) {
        return Objects.equals(normaliser(a), normaliser(b));
    }

    private static String normaliser(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
