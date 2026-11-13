package com.pingmanager.core;

import com.pingmanager.model.CompteUtilisateur;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Point 13 du README : vérification de l'email d'un {@link CompteUtilisateur} à sa
 * création.
 *
 * <p>Sans cette étape, quiconque créait un compte avec l'adresse d'un référent de
 * club héritait de l'accès « administrateur principal » pré-enregistré pour cette
 * adresse. Désormais, un compte n'a <b>aucun droit</b> (organisateur, club,
 * supervision) tant que son email n'est pas confirmé par le code à usage unique
 * envoyé <b>à cette adresse</b> : seule la personne qui lit cette boîte peut
 * l'activer.</p>
 *
 * <ol>
 *   <li>{@link #envoyerCode} : génère un code ({@link GenerateurCode}), l'envoie à
 *       l'email du compte ({@link EnvoiEmail}) et renvoie une
 *       {@link DemandeVerification} qui n'en garde que l'empreinte ;</li>
 *   <li>{@link #confirmer} : vérifie expiration puis code (5 essais faux, puis
 *       blocage croissant, point 15) et marque le compte vérifié.</li>
 * </ol>
 *
 * <p>Tous les effets de bord passent par des ports ; le service est sans état hors
 * de sa limite d'essais.</p>
 */
public final class ServiceVerificationEmail {

    /** Objet porté par les demandes de vérification d'email. */
    static final String OBJET = "email";

    /** Durée de validité par défaut d'un code. */
    public static final Duration VALIDITE_CODE_PAR_DEFAUT = Duration.ofMinutes(30);

    private final GenerateurCode generateurCode;
    private final EnvoiEmail envoiEmail;
    private final HachageMotDePasse hachage;
    private final Horloge horloge;
    private final Duration validiteCode;
    private final LimiteurEssais limiteCodes;

    public ServiceVerificationEmail(GenerateurCode generateurCode, EnvoiEmail envoiEmail,
                                    HachageMotDePasse hachage, Horloge horloge) {
        this.generateurCode = Objects.requireNonNull(generateurCode, "generateurCode");
        this.envoiEmail = Objects.requireNonNull(envoiEmail, "envoiEmail");
        this.hachage = Objects.requireNonNull(hachage, "hachage");
        this.horloge = Objects.requireNonNull(horloge, "horloge");
        this.validiteCode = VALIDITE_CODE_PAR_DEFAUT;
        this.limiteCodes = new LimiteurEssais(this.horloge);
    }

    /**
     * Envoie un code à l'email du compte.
     *
     * @return la demande (empreinte du code, expiration), à conserver jusqu'à la confirmation
     * @throws IllegalStateException si l'email est déjà vérifié
     */
    public DemandeVerification envoyerCode(CompteUtilisateur compte) {
        Objects.requireNonNull(compte, "compte");
        if (compte.isEmailVerifie()) {
            throw new IllegalStateException("L'email de ce compte est déjà vérifié.");
        }
        String code = generateurCode.genererCode();
        Instant expiration = horloge.maintenant().plus(validiteCode);
        envoiEmail.envoyer(compte.getEmail(),
                "PingManager — code de vérification de votre email",
                "Bonjour " + compte.getPrenom() + ",\n"
                        + "Voici le code qui active votre compte PingManager : " + code + "\n"
                        + "Ce code est valable " + validiteCode.toMinutes() + " minutes.\n"
                        + "Si vous n'avez pas créé de compte, ignorez ce message.");
        return new DemandeVerification(compte.getEmail(), hachage.hacher(code), expiration, OBJET);
    }

    /**
     * Confirme le code saisi ; en cas de succès, le compte est marqué vérifié (la
     * persistance est laissée à l'appelant).
     *
     * @param demande la demande renvoyée par {@link #envoyerCode}, ou {@code null} si aucune
     */
    public ResultatVerificationEmail confirmer(CompteUtilisateur compte, DemandeVerification demande,
                                               String codeSaisi) {
        Objects.requireNonNull(compte, "compte");
        if (compte.isEmailVerifie()) {
            return ResultatVerificationEmail.de(ResultatVerificationEmail.Statut.DEJA_VERIFIE);
        }
        if (demande == null || !demande.viseObjet(OBJET)
                || !compte.getEmail().equalsIgnoreCase(demande.getEmail())) {
            return ResultatVerificationEmail.aucuneDemande();
        }
        if (codeSaisi == null || codeSaisi.isBlank()) {
            return ResultatVerificationEmail.de(ResultatVerificationEmail.Statut.CODE_INCORRECT);
        }
        String cle = OBJET + "|" + compte.getEmail();
        Duration attente = limiteCodes.attenteRestante(cle);
        if (!attente.isZero()) {
            return ResultatVerificationEmail.tropDEssais(attente);
        }
        if (demande.estExpiree(horloge.maintenant())) {
            return ResultatVerificationEmail.de(ResultatVerificationEmail.Statut.CODE_EXPIRE);
        }
        if (!hachage.verifier(codeSaisi.trim(), demande.getEmpreinteCode())) {
            limiteCodes.enregistrerEchec(cle);
            return ResultatVerificationEmail.de(ResultatVerificationEmail.Statut.CODE_INCORRECT);
        }
        limiteCodes.enregistrerReussite(cle);
        compte.marquerEmailVerifie();
        return ResultatVerificationEmail.de(ResultatVerificationEmail.Statut.VERIFIE);
    }
}
