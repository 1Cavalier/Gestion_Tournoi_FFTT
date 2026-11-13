package com.pingmanager.core;

import com.pingmanager.model.AccesClub;
import com.pingmanager.model.Club;
import com.pingmanager.model.CompteOrganisateur;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Service d'inscription d'un organisateur et de rattachement à son club.
 *
 * <p>Il orchestre le scénario de la brique « pré-tournoi / comptes » :</p>
 * <ol>
 *   <li><b>Inscription</b> ({@link #inscrire}) : la personne crée son compte et
 *       demande le rattachement à un club (par numéro). Le service retrouve le
 *       club dans le {@link RegistreClubs}, hache le mot de passe, crée un
 *       {@link CompteOrganisateur} non vérifié, génère un code et l'envoie à
 *       l'<b>email officiel du club</b> (et non à celui de la personne) : c'est ce
 *       qui garantit que le demandeur a bien accès au club. Le code n'est jamais
 *       conservé en clair — seule son empreinte l'est, dans une
 *       {@link DemandeVerification} renvoyée à l'appelant.</li>
 *   <li><b>Confirmation</b> ({@link #confirmer}) : la personne saisit le code reçu.
 *       Le service vérifie l'expiration puis le code contre son empreinte ; si
 *       tout est bon, il marque le compte « email vérifié » et crée l'
 *       {@link AccesClub} — le droit d'administrer ce club.</li>
 * </ol>
 *
 * <p>Tous les effets de bord passent par des <b>ports</b> ({@link GenerateurCode},
 * {@link EnvoiEmail}, {@link HachageMotDePasse}, {@link Horloge}) : le service ne
 * dépend d'aucune technologie concrète et reste testable. Il est par ailleurs
 * <b>sans état</b> : il ne conserve ni comptes ni demandes ; l'appelant fournit
 * ce qu'il faut et reçoit ce qui est produit. La détection d'un email déjà pris
 * est déléguée à un prédicat fourni par l'appelant, en attendant un repository
 * (persistance traitée plus tard).</p>
 */
public final class ServiceComptesOrganisateurs {

    /** Durée de validité par défaut d'un code de vérification. */
    public static final Duration VALIDITE_CODE_PAR_DEFAUT = Duration.ofMinutes(30);

    private final RegistreClubs registreClubs;
    private final GenerateurCode generateurCode;
    private final EnvoiEmail envoiEmail;
    private final HachageMotDePasse hachage;
    private final Horloge horloge;
    private final Duration validiteCode;
    /** Point 15 : 5 codes faux pour un compte, puis blocage temporaire croissant. */
    private final LimiteurEssais limiteCodes;

    public ServiceComptesOrganisateurs(RegistreClubs registreClubs,
                                       GenerateurCode generateurCode,
                                       EnvoiEmail envoiEmail,
                                       HachageMotDePasse hachage,
                                       Horloge horloge) {
        this(registreClubs, generateurCode, envoiEmail, hachage, horloge, VALIDITE_CODE_PAR_DEFAUT);
    }

    public ServiceComptesOrganisateurs(RegistreClubs registreClubs,
                                       GenerateurCode generateurCode,
                                       EnvoiEmail envoiEmail,
                                       HachageMotDePasse hachage,
                                       Horloge horloge,
                                       Duration validiteCode) {
        this.registreClubs = Objects.requireNonNull(registreClubs, "registreClubs");
        this.generateurCode = Objects.requireNonNull(generateurCode, "generateurCode");
        this.envoiEmail = Objects.requireNonNull(envoiEmail, "envoiEmail");
        this.hachage = Objects.requireNonNull(hachage, "hachage");
        this.horloge = Objects.requireNonNull(horloge, "horloge");
        this.limiteCodes = new LimiteurEssais(this.horloge);
        this.validiteCode = Objects.requireNonNull(validiteCode, "validiteCode");
        if (validiteCode.isZero() || validiteCode.isNegative()) {
            throw new IllegalArgumentException("La validité du code doit être strictement positive.");
        }
    }

    /**
     * Inscrit un organisateur et envoie un code de vérification à l'email officiel
     * de son club.
     *
     * @param email             email de connexion de la personne
     * @param nom               nom de la personne
     * @param prenom            prénom de la personne
     * @param motDePasseEnClair mot de passe choisi (haché immédiatement, non conservé)
     * @param numeroClub        numéro FFTT du club de rattachement
     * @param emailDejaUtilise  prédicat indiquant si un email est déjà pris (fourni
     *                          par l'appelant en attendant un repository) ;
     *                          {@code null} = aucun contrôle d'unicité
     * @return le résultat détaillé (compte + demande en cas de succès)
     */
    public ResultatInscriptionOrganisateur inscrire(String email, String nom, String prenom,
                                                    String motDePasseEnClair, String numeroClub,
                                                    Predicate<String> emailDejaUtilise) {
        Objects.requireNonNull(email, "email");
        Objects.requireNonNull(motDePasseEnClair, "motDePasseEnClair");
        Objects.requireNonNull(numeroClub, "numeroClub");

        String emailNormalise = email.trim().toLowerCase();

        // 1) Le club doit exister dans le référentiel.
        Club club = registreClubs.trouverParNumero(numeroClub).orElse(null);
        if (club == null) {
            return ResultatInscriptionOrganisateur.clubIntrouvable();
        }

        // 2) L'email ne doit pas déjà être pris (si un contrôle est fourni).
        if (emailDejaUtilise != null && emailDejaUtilise.test(emailNormalise)) {
            return ResultatInscriptionOrganisateur.emailDejaUtilise();
        }

        // 2 bis) Politique de mot de passe (point 15 : 10 caractères au moins).
        java.util.Optional<String> refus = PolitiqueMotDePasse.motifRefus(motDePasseEnClair);
        if (refus.isPresent()) {
            return ResultatInscriptionOrganisateur.motDePasseTropCourt(refus.get());
        }

        // 3) Création du compte (non vérifié), mot de passe haché via le port.
        String empreinte = hachage.hacher(motDePasseEnClair);
        CompteOrganisateur compte =
                new CompteOrganisateur(emailNormalise, nom, prenom, empreinte, numeroClub);

        // 4) Génération du code + envoi à l'EMAIL OFFICIEL DU CLUB.
        String code = generateurCode.genererCode();
        String empreinteCode = hachage.hacher(code);
        Instant expiration = horloge.maintenant().plus(validiteCode);
        DemandeVerification demande =
                new DemandeVerification(emailNormalise, empreinteCode, expiration);

        envoiEmail.envoyer(
                club.getEmailOfficiel(),
                "Code de vérification — rattachement au club " + club.getNom(),
                "Une demande de rattachement au club " + club.getNom() + " (" + club.getNumero()
                        + ") a été faite par " + prenom + " " + nom + " (" + emailNormalise + ").\n"
                        + "Code de vérification : " + code + "\n"
                        + "Ce code est valable " + validiteCode.toMinutes() + " minutes.");

        return ResultatInscriptionOrganisateur.codeEnvoye(compte, demande);
    }

    /**
     * Renvoie un nouveau code de vérification (nouvelle demande) pour un compte
     * non encore vérifié. Utile si le premier code a expiré ou n'est pas parvenu.
     *
     * @param compte le compte concerné (doit ne pas être déjà vérifié)
     * @return la nouvelle demande de vérification
     * @throws IllegalStateException    si le compte est déjà vérifié
     * @throws IllegalArgumentException si le club du compte est introuvable
     */
    public DemandeVerification renvoyerCode(CompteOrganisateur compte) {
        Objects.requireNonNull(compte, "compte");
        if (compte.isEmailVerifie()) {
            throw new IllegalStateException("L'email de ce compte est déjà vérifié.");
        }
        Club club = registreClubs.trouverParNumero(compte.getNumeroClub())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Club introuvable pour le numéro " + compte.getNumeroClub() + "."));

        String code = generateurCode.genererCode();
        String empreinteCode = hachage.hacher(code);
        Instant expiration = horloge.maintenant().plus(validiteCode);

        envoiEmail.envoyer(
                club.getEmailOfficiel(),
                "Nouveau code de vérification — club " + club.getNom(),
                "Nouveau code de vérification pour " + compte.getEmail() + " : " + code + "\n"
                        + "Ce code est valable " + validiteCode.toMinutes() + " minutes.");

        return new DemandeVerification(compte.getEmail(), empreinteCode, expiration);
    }

    /**
     * Confirme le code saisi : si correct et non expiré, marque le compte comme
     * vérifié et crée l'accès au club.
     *
     * @param compte     le compte concerné (celui renvoyé à l'inscription)
     * @param demande    la demande de vérification associée
     * @param codeSaisi  le code saisi par la personne
     * @return le résultat détaillé (accès créé en cas de succès)
     */
    public ResultatConfirmation confirmer(CompteOrganisateur compte,
                                          DemandeVerification demande,
                                          String codeSaisi) {
        Objects.requireNonNull(compte, "compte");
        Objects.requireNonNull(demande, "demande");

        if (!compte.getEmail().equalsIgnoreCase(demande.getEmail())) {
            throw new IllegalArgumentException("La demande ne correspond pas à ce compte.");
        }
        if (codeSaisi == null || codeSaisi.isBlank()) {
            return ResultatConfirmation.codeIncorrect();
        }

        // Limite d'essais (point 15), indépendante de la demande (un code renvoyé ne
        // remet pas le compteur à zéro).
        Duration attente = limiteCodes.attenteRestante(compte.getEmail());
        if (!attente.isZero()) {
            return ResultatConfirmation.tropDEssais(attente);
        }

        // Expiration d'abord : un code juste mais périmé est refusé.
        if (demande.estExpiree(horloge.maintenant())) {
            return ResultatConfirmation.codeExpire();
        }

        if (!hachage.verifier(codeSaisi.trim(), demande.getEmpreinteCode())) {
            limiteCodes.enregistrerEchec(compte.getEmail());
            return ResultatConfirmation.codeIncorrect();
        }
        limiteCodes.enregistrerReussite(compte.getEmail());

        compte.marquerEmailVerifie();
        AccesClub acces = new AccesClub(
                compte.getNumeroClub(), compte.getEmail(), compte.getNom(), compte.getPrenom());
        return ResultatConfirmation.verifie(acces);
    }
}
