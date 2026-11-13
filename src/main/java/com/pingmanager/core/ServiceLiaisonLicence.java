package com.pingmanager.core;

import com.pingmanager.model.AccesClub;
import com.pingmanager.model.Club;
import com.pingmanager.model.CompteUtilisateur;
import com.pingmanager.model.FicheLicence;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Service de la <b>démarche 1</b> : lier le compte d'une personne à sa licence FFTT,
 * avec une <b>preuve de possession</b> (point 14 du README).
 *
 * <p>Avant, saisir un numéro suffisait : n'importe quel compte se liait à la licence
 * d'un tiers (et plusieurs comptes à la même), l'inscrivait en son nom et lisait sa
 * fiche. Désormais :</p>
 * <ol>
 *   <li><b>Demander</b> ({@link #demander}) : le compte (email vérifié, point 13)
 *       saisit le numéro <b>et la date de naissance</b> du licencié. Si la licence est
 *       libre et la date juste, un code à usage unique est envoyé à l'<b>adresse
 *       officielle du club de la licence</b> (le référent), qui le transmet à son
 *       licencié s'il le reconnaît : c'est la validation par le club.</li>
 *   <li><b>Confirmer</b> ({@link #confirmer}) : le code reçu lie la licence, si elle
 *       est toujours libre.</li>
 * </ol>
 * <p>Une licence n'est liée qu'à <b>un seul</b> compte : la recherche du compte qui la
 * porte est fournie par l'appelant (le dépôt applicatif). <b>Délier</b>
 * ({@link #delier}) : par le titulaire, ou par un administrateur du club de la
 * licence (licence réclamée par erreur ou usurpée).</p>
 *
 * <p>La fiche fédérale ne porte pas d'email aujourd'hui : l'envoi du code à l'email
 * du licencié reste une alternative pour le jour où la source FFTT le fournira
 * (README section 6). Effets de bord par les ports {@link RegistreLicences},
 * {@link RegistreClubs}, {@link GenerateurCode}, {@link EnvoiEmail},
 * {@link HachageMotDePasse} et {@link Horloge}.</p>
 */
public final class ServiceLiaisonLicence {

    /** Durée de validité d'un code : le référent peut mettre du temps à répondre. */
    public static final Duration VALIDITE_CODE_PAR_DEFAUT = Duration.ofDays(7);

    private final RegistreLicences registreLicences;
    private final RegistreClubs registreClubs;
    private final GenerateurCode generateurCode;
    private final EnvoiEmail envoiEmail;
    private final HachageMotDePasse hachage;
    private final Horloge horloge;
    /** Point 15 : dates de naissance et codes faux, par compte. */
    private final LimiteurEssais limiteEssais;

    public ServiceLiaisonLicence(RegistreLicences registreLicences, RegistreClubs registreClubs,
                                 GenerateurCode generateurCode, EnvoiEmail envoiEmail,
                                 HachageMotDePasse hachage, Horloge horloge) {
        this.registreLicences = Objects.requireNonNull(registreLicences, "registreLicences");
        this.registreClubs = Objects.requireNonNull(registreClubs, "registreClubs");
        this.generateurCode = Objects.requireNonNull(generateurCode, "generateurCode");
        this.envoiEmail = Objects.requireNonNull(envoiEmail, "envoiEmail");
        this.hachage = Objects.requireNonNull(hachage, "hachage");
        this.horloge = Objects.requireNonNull(horloge, "horloge");
        this.limiteEssais = new LimiteurEssais(this.horloge);
    }

    static String objet(String numeroLicence) {
        return "licence:" + numeroLicence.trim();
    }

    /**
     * Première étape : contrôles, puis code envoyé au référent du club de la licence.
     *
     * @param compteDeLaLicence licence → compte qui la porte déjà (fourni par l'appelant)
     */
    public ResultatLiaisonLicence demander(CompteUtilisateur compte, String numeroLicence,
                                           LocalDate dateNaissance,
                                           Function<String, Optional<CompteUtilisateur>> compteDeLaLicence) {
        Objects.requireNonNull(compte, "compte");
        Objects.requireNonNull(compteDeLaLicence, "compteDeLaLicence");
        if (!compte.isEmailVerifie()) {
            return ResultatLiaisonLicence.refus(ResultatLiaisonLicence.Statut.EMAIL_NON_VERIFIE);
        }
        if (compte.aLicenceLiee()) {
            return ResultatLiaisonLicence.refus(ResultatLiaisonLicence.Statut.COMPTE_DEJA_LIE);
        }
        Duration attente = limiteEssais.attenteRestante(compte.getEmail());
        if (!attente.isZero()) {
            return ResultatLiaisonLicence.tropDEssais(attente);
        }
        if (numeroLicence == null || numeroLicence.isBlank()) {
            return ResultatLiaisonLicence.refus(ResultatLiaisonLicence.Statut.LICENCE_INTROUVABLE);
        }
        FicheLicence fiche = registreLicences.trouverParLicence(numeroLicence.trim()).orElse(null);
        if (fiche == null) {
            return ResultatLiaisonLicence.refus(ResultatLiaisonLicence.Statut.LICENCE_INTROUVABLE);
        }
        if (prisParUnAutre(compte, fiche, compteDeLaLicence)) {
            return ResultatLiaisonLicence.refus(ResultatLiaisonLicence.Statut.DEJA_LIEE_A_UN_AUTRE_COMPTE);
        }
        // Fiche sans date : la validation du référent reste exigée ; date saisie fausse : refus
        // compté comme un essai (pas de devinette de la date par essais successifs).
        if (fiche.getDateNaissance() != null && !fiche.getDateNaissance().equals(dateNaissance)) {
            limiteEssais.enregistrerEchec(compte.getEmail());
            return ResultatLiaisonLicence.refus(ResultatLiaisonLicence.Statut.DATE_DE_NAISSANCE_INCORRECTE);
        }
        Club club = fiche.getNumeroClub() == null ? null
                : registreClubs.trouverParNumero(fiche.getNumeroClub()).orElse(null);
        if (club == null || club.getEmailOfficiel() == null || club.getEmailOfficiel().isBlank()) {
            return ResultatLiaisonLicence.refus(ResultatLiaisonLicence.Statut.CLUB_SANS_REFERENT);
        }
        String code = generateurCode.genererCode();
        Instant expiration = horloge.maintenant().plus(VALIDITE_CODE_PAR_DEFAUT);
        envoiEmail.envoyer(club.getEmailOfficiel(),
                "Liaison de licence — " + fiche.getNomComplet() + " (" + fiche.getNumeroLicence() + ")",
                "Le compte PingManager " + compte.getNomComplet() + " (" + compte.getEmail() + ") demande à être lié\n"
                        + "à la licence " + fiche.getNumeroLicence() + " de " + fiche.getNomComplet()
                        + ", licencié de votre club.\n"
                        + "Si vous reconnaissez votre licencié, transmettez-lui ce code : " + code + "\n"
                        + "Sinon, ne faites rien. Ce code est valable "
                        + VALIDITE_CODE_PAR_DEFAUT.toDays() + " jours.");
        DemandeVerification demande = new DemandeVerification(compte.getEmail(), hachage.hacher(code),
                expiration, objet(fiche.getNumeroLicence()));
        return ResultatLiaisonLicence.codeEnvoye(fiche, demande);
    }

    /** Seconde étape : le code transmis par le référent lie la licence au compte. */
    public ResultatLiaisonLicence confirmer(CompteUtilisateur compte, String numeroLicence,
                                            DemandeVerification demande, String codeSaisi,
                                            Function<String, Optional<CompteUtilisateur>> compteDeLaLicence) {
        Objects.requireNonNull(compte, "compte");
        Objects.requireNonNull(compteDeLaLicence, "compteDeLaLicence");
        if (!compte.isEmailVerifie()) {
            return ResultatLiaisonLicence.refus(ResultatLiaisonLicence.Statut.EMAIL_NON_VERIFIE);
        }
        if (numeroLicence == null || demande == null || !demande.viseObjet(objet(numeroLicence))
                || !compte.getEmail().equalsIgnoreCase(demande.getEmail())) {
            return ResultatLiaisonLicence.refus(ResultatLiaisonLicence.Statut.AUCUNE_DEMANDE);
        }
        if (compte.aLicenceLiee()) {
            return ResultatLiaisonLicence.refus(ResultatLiaisonLicence.Statut.COMPTE_DEJA_LIE);
        }
        Duration attente = limiteEssais.attenteRestante(compte.getEmail());
        if (!attente.isZero()) {
            return ResultatLiaisonLicence.tropDEssais(attente);
        }
        if (demande.estExpiree(horloge.maintenant())) {
            return ResultatLiaisonLicence.refus(ResultatLiaisonLicence.Statut.CODE_EXPIRE);
        }
        if (codeSaisi == null || !hachage.verifier(codeSaisi.trim(), demande.getEmpreinteCode())) {
            limiteEssais.enregistrerEchec(compte.getEmail());
            return ResultatLiaisonLicence.refus(ResultatLiaisonLicence.Statut.CODE_INCORRECT);
        }
        FicheLicence fiche = registreLicences.trouverParLicence(numeroLicence.trim()).orElse(null);
        if (fiche == null) {
            return ResultatLiaisonLicence.refus(ResultatLiaisonLicence.Statut.LICENCE_INTROUVABLE);
        }
        // Une autre personne a pu lier la licence entre la demande et la confirmation.
        if (prisParUnAutre(compte, fiche, compteDeLaLicence)) {
            return ResultatLiaisonLicence.refus(ResultatLiaisonLicence.Statut.DEJA_LIEE_A_UN_AUTRE_COMPTE);
        }
        limiteEssais.enregistrerReussite(compte.getEmail());
        compte.lierLicence(fiche.getNumeroLicence());
        return ResultatLiaisonLicence.liee(fiche);
    }

    /**
     * Retire la licence de {@code titulaire}. Permis au titulaire lui-même, ou à un
     * administrateur du club de la licence (accès sur ce club, email vérifié).
     *
     * @param accesDuDemandeur habilitations actives du demandeur
     */
    public ResultatLiaisonLicence delier(CompteUtilisateur titulaire, CompteUtilisateur demandeur,
                                         Collection<AccesClub> accesDuDemandeur) {
        Objects.requireNonNull(titulaire, "titulaire");
        Objects.requireNonNull(demandeur, "demandeur");
        if (!titulaire.aLicenceLiee()) {
            return ResultatLiaisonLicence.refus(ResultatLiaisonLicence.Statut.AUCUNE_LICENCE_LIEE);
        }
        if (!titulaire.equals(demandeur) && !administreLeClubDeLaLicence(titulaire.getLicenceLiee(),
                demandeur, accesDuDemandeur)) {
            return ResultatLiaisonLicence.refus(ResultatLiaisonLicence.Statut.DROIT_INSUFFISANT);
        }
        titulaire.delierLicence();
        return ResultatLiaisonLicence.deliee();
    }

    private boolean administreLeClubDeLaLicence(String licence, CompteUtilisateur demandeur,
                                                Collection<AccesClub> acces) {
        if (!demandeur.isEmailVerifie() || acces == null) {
            return false;
        }
        String club = registreLicences.trouverParLicence(licence).map(FicheLicence::getNumeroClub).orElse(null);
        if (club == null) {
            return false;
        }
        for (AccesClub a : acces) {
            if (club.equals(a.getNumeroClub()) && a.getEmail().equalsIgnoreCase(demandeur.getEmail())) {
                return true;
            }
        }
        return false;
    }

    private static boolean prisParUnAutre(CompteUtilisateur compte, FicheLicence fiche,
                                          Function<String, Optional<CompteUtilisateur>> compteDeLaLicence) {
        Optional<CompteUtilisateur> porteur = compteDeLaLicence.apply(fiche.getNumeroLicence());
        return porteur != null && porteur.isPresent() && !porteur.get().equals(compte);
    }
}
