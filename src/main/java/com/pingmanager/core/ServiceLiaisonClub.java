package com.pingmanager.core;

import com.pingmanager.model.AccesClub;
import com.pingmanager.model.Club;
import com.pingmanager.model.CompteUtilisateur;
import com.pingmanager.model.NiveauAcces;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Service de la <b>démarche 2</b> : lier un compte existant à un club, en tant
 * qu'organisateur.
 *
 * <p>Le principe reprend celui de {@link ServiceComptesOrganisateurs}, mais pour
 * un compte <b>déjà créé</b> ({@link CompteUtilisateur}) : on ne crée pas de
 * compte ici, on ajoute une habilitation.</p>
 * <ol>
 *   <li><b>Demander</b> ({@link #demander}) : la personne saisit le numéro du
 *       club. Le service le retrouve dans le {@link RegistreClubs}, génère un
 *       code et l'envoie à l'<b>email officiel du club</b> (celui du référent) —
 *       ce qui garantit que le demandeur a bien accès au club. Le code n'est
 *       jamais conservé en clair : seule son empreinte l'est, dans la
 *       {@link DemandeVerification} renvoyée.</li>
 *   <li><b>Confirmer</b> ({@link #confirmer}) : la personne saisit le code reçu.
 *       Si tout est bon, un {@link AccesClub} est créé. Le <b>niveau</b> est
 *       {@link NiveauAcces#ADMIN_PRINCIPAL} lorsque la personne est le référent
 *       du club (email de connexion = email officiel du club), sinon
 *       {@link NiveauAcces#ADMIN_NIVEAU_1}.</li>
 * </ol>
 *
 * <p>Tous les effets de bord passent par des ports ({@link GenerateurCode},
 * {@link EnvoiEmail}, {@link HachageMotDePasse}, {@link Horloge}). Le service est
 * sans état ; l'appelant conserve la {@link DemandeVerification} entre les deux
 * étapes et persiste l'accès produit.</p>
 */
public final class ServiceLiaisonClub {

    /** Durée de validité par défaut d'un code de vérification. */
    public static final Duration VALIDITE_CODE_PAR_DEFAUT = Duration.ofMinutes(30);

    private final RegistreClubs registreClubs;
    private final GenerateurCode generateurCode;
    private final EnvoiEmail envoiEmail;
    private final HachageMotDePasse hachage;
    private final Horloge horloge;
    private final Duration validiteCode;
    /** Point 15 : 5 codes faux pour un compte et un club, puis blocage temporaire croissant. */
    private final LimiteurEssais limiteCodes;

    public ServiceLiaisonClub(RegistreClubs registreClubs, GenerateurCode generateurCode,
                              EnvoiEmail envoiEmail, HachageMotDePasse hachage, Horloge horloge) {
        this(registreClubs, generateurCode, envoiEmail, hachage, horloge, VALIDITE_CODE_PAR_DEFAUT);
    }

    public ServiceLiaisonClub(RegistreClubs registreClubs, GenerateurCode generateurCode,
                              EnvoiEmail envoiEmail, HachageMotDePasse hachage, Horloge horloge,
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
     * Lance la démarche : envoie un code à l'email officiel du club.
     *
     * @param compte     le compte demandeur (déjà connecté)
     * @param numeroClub le numéro FFTT du club visé
     * @return la demande de vérification (à conserver pour la confirmation), ou
     *         {@link Optional#empty()} si aucun club ne porte ce numéro
     */
    public Optional<DemandeVerification> demander(CompteUtilisateur compte, String numeroClub) {
        Objects.requireNonNull(compte, "compte");
        Club club = registreClubs.trouverParNumero(numeroClub).orElse(null);
        if (club == null) {
            return Optional.empty();
        }

        String code = generateurCode.genererCode();
        String empreinteCode = hachage.hacher(code);
        Instant expiration = horloge.maintenant().plus(validiteCode);

        envoiEmail.envoyer(
                club.getEmailOfficiel(),
                "Code de rattachement — club " + club.getNom(),
                "Une demande de rattachement au club " + club.getNom() + " (" + club.getNumero()
                        + ") a été faite par " + compte.getNomComplet() + " (" + compte.getEmail() + ").\n"
                        + "Code de vérification : " + code + "\n"
                        + "Ce code est valable " + validiteCode.toMinutes() + " minutes.");

        return Optional.of(new DemandeVerification(compte.getEmail(), empreinteCode, expiration));
    }

    /**
     * Confirme le code saisi : si correct et non expiré, crée l'accès au club.
     *
     * @param compte     le compte demandeur
     * @param numeroClub le numéro du club visé (celui de la demande)
     * @param demande    la demande de vérification renvoyée par {@link #demander}
     * @param codeSaisi  le code saisi par la personne
     * @return le résultat détaillé (accès créé en cas de succès)
     */
    public ResultatLiaisonClub confirmer(CompteUtilisateur compte, String numeroClub,
                                         DemandeVerification demande, String codeSaisi) {
        Objects.requireNonNull(compte, "compte");
        Objects.requireNonNull(demande, "demande");

        if (!compte.getEmail().equalsIgnoreCase(demande.getEmail())) {
            throw new IllegalArgumentException("La demande ne correspond pas à ce compte.");
        }

        Club club = registreClubs.trouverParNumero(numeroClub).orElse(null);
        if (club == null) {
            return ResultatLiaisonClub.clubIntrouvable();
        }
        if (codeSaisi == null || codeSaisi.isBlank()) {
            return ResultatLiaisonClub.codeIncorrect();
        }
        // Limite d'essais (point 15) : la clé ne dépend pas de la demande, redemander un
        // code ne remet donc pas le compteur à zéro.
        String cle = compte.getEmail() + "|" + club.getNumero();
        Duration attente = limiteCodes.attenteRestante(cle);
        if (!attente.isZero()) {
            return ResultatLiaisonClub.tropDEssais(attente);
        }
        // Expiration d'abord : un code juste mais périmé est refusé.
        if (demande.estExpiree(horloge.maintenant())) {
            return ResultatLiaisonClub.codeExpire();
        }
        if (!hachage.verifier(codeSaisi.trim(), demande.getEmpreinteCode())) {
            limiteCodes.enregistrerEchec(cle);
            return ResultatLiaisonClub.codeIncorrect();
        }
        limiteCodes.enregistrerReussite(cle);

        NiveauAcces niveau = estReferent(compte, club)
                ? NiveauAcces.ADMIN_PRINCIPAL
                : NiveauAcces.ADMIN_NIVEAU_1;
        AccesClub acces = new AccesClub(
                club.getNumero(), compte.getEmail(), compte.getNom(), compte.getPrenom(), niveau);
        return ResultatLiaisonClub.verifie(acces);
    }

    /**
     * Détermine si la personne est le référent du club et mérite donc le niveau
     * principal.
     *
     * <p>Règle actuelle, volontairement simple : la personne s'est connectée
     * avec l'<b>adresse officielle du club</b> (celle où arrive le code). C'est
     * un signal fort — elle a la main sur la boîte du référent. Quand la source
     * FFTT fournira l'identité exacte du référent (par licence), cette règle
     * pourra être affinée en comparant la licence liée du compte au référent
     * déclaré du club.</p>
     */
    private boolean estReferent(CompteUtilisateur compte, Club club) {
        return compte.getEmail().equalsIgnoreCase(club.getEmailOfficiel().trim());
    }
}
