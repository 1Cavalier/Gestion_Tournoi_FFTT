package com.pingmanager.core;

import com.pingmanager.model.ContactSms;
import com.pingmanager.model.ConvocationsSms;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Match;
import com.pingmanager.model.MessageSms;
import com.pingmanager.model.ModeAnnonce;
import com.pingmanager.model.StatutEnvoiSms;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.TypeConvocation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Convocations par SMS (B019) : numéros consentis, déclenchement, limite
 * d'envois, journal. <b>Jamais d'appel</b> : même avec l'annonce « SMS et appel
 * vocal », ce service n'envoie que des messages écrits.
 *
 * <p>Règles :</p>
 * <ul>
 *   <li>un numéro n'est gardé qu'avec le <b>consentement explicite</b> de la
 *       personne, et il est effacé à la fin du tournoi
 *       ({@link #effacerSiTermine}) ou à la demande du club ;</li>
 *   <li>envoi automatique quand un match est envoyé sur une table, si l'annonce
 *       choisie (B037, {@link ModeAnnonce}) comprend le SMS <b>et</b> si le club a
 *       activé l'envoi automatique ; sinon envoi manuel ;</li>
 *   <li>au-delà de la limite d'envois du tournoi, rien ne part et le refus est
 *       inscrit au journal.</li>
 * </ul>
 */
public final class ServiceConvocationsSms {

    private final EnvoiSms envoi;

    public ServiceConvocationsSms(EnvoiSms envoi) {
        this.envoi = Objects.requireNonNull(envoi, "envoi");
    }

    // ------------------------------------------------------------------
    // Numéros et consentement
    // ------------------------------------------------------------------

    /**
     * Enregistre le numéro de portable d'un licencié, seulement avec son
     * consentement explicite ; un numéro déjà connu est remplacé.
     */
    public ResultatContactSms enregistrerContact(Tournoi tournoi, String numeroLicence, String saisie,
                                                 boolean consentement, LocalDateTime maintenant) {
        if (numeroLicence == null || numeroLicence.isBlank()) {
            return new ResultatContactSms(ResultatContactSms.Motif.LICENCE_MANQUANTE, "");
        }
        if (!consentement) {
            return new ResultatContactSms(ResultatContactSms.Motif.CONSENTEMENT_MANQUANT, "");
        }
        Optional<String> numero = NumeroPortable.normaliser(saisie);
        if (numero.isEmpty()) {
            return new ResultatContactSms(ResultatContactSms.Motif.NUMERO_INVALIDE, "");
        }
        if (estTermine(tournoi, maintenant.toLocalDate())) {
            return new ResultatContactSms(ResultatContactSms.Motif.TOURNOI_TERMINE, "");
        }
        tournoi.getConvocationsSms().enregistrerContact(new ContactSms(numeroLicence, numero.get(), maintenant));
        return new ResultatContactSms(ResultatContactSms.Motif.ENREGISTRE, numero.get());
    }

    /**
     * Contrôle une saisie <b>facultative</b> avant de l'enregistrer (formulaire
     * d'inscription) : rien n'est enregistré ni modifié.
     *
     * @return vide si la saisie est vide (rien à garder) ou recevable ; sinon le
     *         refus (consentement manquant, numéro non reconnu)
     */
    public static Optional<ResultatContactSms> refusSaisieFacultative(String saisie, boolean consentement) {
        if (saisie == null || saisie.isBlank()) {
            return Optional.empty();
        }
        if (!consentement) {
            return Optional.of(new ResultatContactSms(ResultatContactSms.Motif.CONSENTEMENT_MANQUANT, ""));
        }
        if (NumeroPortable.normaliser(saisie).isEmpty()) {
            return Optional.of(new ResultatContactSms(ResultatContactSms.Motif.NUMERO_INVALIDE, ""));
        }
        return Optional.empty();
    }

    /** Retire le numéro d'un licencié (retrait du consentement). */
    public boolean retirerContact(Tournoi tournoi, String numeroLicence) {
        return tournoi.getConvocationsSms().retirerContact(numeroLicence);
    }

    /** Efface tous les numéros du tournoi (le journal des envois reste, sans numéro). */
    public int effacerNumeros(Tournoi tournoi, LocalDateTime maintenant) {
        ConvocationsSms cs = tournoi.getConvocationsSms();
        int n = cs.getContacts().size();
        cs.effacerNumeros(maintenant);
        return n;
    }

    /**
     * Efface les numéros si le tournoi est terminé (le lendemain de son dernier
     * jour) et qu'il en reste. À appeler à l'ouverture du tournoi.
     *
     * @return le nombre de numéros effacés
     */
    public int effacerSiTermine(Tournoi tournoi, LocalDateTime maintenant) {
        if (tournoi.getConvocationsSms().getContacts().isEmpty() || !estTermine(tournoi, maintenant.toLocalDate())) {
            return 0;
        }
        return effacerNumeros(tournoi, maintenant);
    }

    /** Le tournoi est-il terminé à cette date (date strictement après son dernier jour connu) ? */
    public static boolean estTermine(Tournoi tournoi, LocalDate aujourdhui) {
        LocalDate dernier = dernierJour(tournoi);
        return dernier != null && aujourdhui.isAfter(dernier);
    }

    /**
     * Date à partir de laquelle les numéros sont effacés d'eux-mêmes : le
     * lendemain du dernier jour du tournoi, ou {@code null} si aucune date n'est connue.
     */
    public static LocalDate dateEffacementAutomatique(Tournoi tournoi) {
        LocalDate dernier = dernierJour(tournoi);
        return dernier == null ? null : dernier.plusDays(1);
    }

    private static LocalDate dernierJour(Tournoi t) {
        LocalDate dernier = t.getJours().isEmpty() ? null : t.getJours().last();
        if (t.getDateFin() != null && (dernier == null || t.getDateFin().isAfter(dernier))) {
            dernier = t.getDateFin();
        }
        return dernier;
    }

    // ------------------------------------------------------------------
    // Expéditeur
    // ------------------------------------------------------------------

    /** Expéditeur des SMS : celui choisi par le club s'il est valide, sinon celui tiré du nom du club. */
    public static String expediteur(Tournoi tournoi) {
        String choisi = tournoi.getConvocationsSms().getExpediteur();
        return ExpediteurSms.estValide(choisi) ? choisi : ExpediteurSms.proposer(tournoi.getClubOrganisateur());
    }

    /** Choisit l'expéditeur ; refusé (false) s'il ne respecte pas la charte. Vide : retour à la proposition. */
    public boolean definirExpediteur(Tournoi tournoi, String expediteur) {
        if (expediteur == null || expediteur.isBlank()) {
            tournoi.getConvocationsSms().setExpediteur(null);
            return true;
        }
        String e = expediteur.trim();
        if (!ExpediteurSms.estValide(e)) {
            return false;
        }
        tournoi.getConvocationsSms().setExpediteur(e);
        return true;
    }

    // ------------------------------------------------------------------
    // Envois
    // ------------------------------------------------------------------

    /**
     * Faut-il envoyer les SMS d'elles-mêmes au lancement d'un match ? Oui si
     * l'annonce choisie comprend le SMS (« SMS » ou « SMS et appel vocal ») et si
     * le club a activé l'envoi automatique. Jamais pour « Rien » ni « Appel vocal ».
     */
    public static boolean envoiAutomatique(Tournoi tournoi, ModeAnnonce mode) {
        return mode != null && mode.avecSms() && tournoi.getConvocationsSms().isEnvoiAutomatique();
    }

    /** Après l'envoi d'un match sur une table : convoque les deux joueurs si la règle le demande. */
    public ResultatConvocationsSms apresLancement(Tournoi tournoi, Match match, int table, ModeAnnonce mode,
                                                  LocalDateTime maintenant) {
        if (!envoiAutomatique(tournoi, mode)) {
            return ResultatConvocationsSms.aucun();
        }
        return convoquerJoueurs(tournoi, match, table, maintenant);
    }

    /** Convoque les deux joueurs d'un match (envoi manuel ou automatique). */
    public ResultatConvocationsSms convoquerJoueurs(Tournoi tournoi, Match match, int table, LocalDateTime maintenant) {
        Objects.requireNonNull(match, "match");
        ContexteConvocation contexte = ContexteConvocation.de(tournoi, match).orElse(null);
        TypeConvocation type = contexte == null ? TypeConvocation.TOUR_TABLEAU : contexte.type();
        List<MessageSms> messages = new ArrayList<>();
        List<String> sansNumero = new ArrayList<>();
        Joueur a = match.getJoueurA();
        Joueur b = match.getJoueurB();
        if (connu(a) && connu(b)) {
            envoyerA(tournoi, type, a.getNumeroLicence(), a.getNomComplet(),
                    TexteConvocation.joueur(a, b, table, contexte), maintenant, messages, sansNumero);
            envoyerA(tournoi, type, b.getNumeroLicence(), b.getNomComplet(),
                    TexteConvocation.joueur(b, a, table, contexte), maintenant, messages, sansNumero);
        }
        return new ResultatConvocationsSms(true, messages, sansNumero);
    }

    /** Convoque un arbitre pour une partie. */
    public ResultatConvocationsSms convoquerArbitre(Tournoi tournoi, String numeroLicence, String prenom,
                                                    String nomComplet, Match match, int table,
                                                    LocalDateTime maintenant) {
        Objects.requireNonNull(match, "match");
        ContexteConvocation contexte = ContexteConvocation.de(tournoi, match).orElse(null);
        List<MessageSms> messages = new ArrayList<>();
        List<String> sansNumero = new ArrayList<>();
        if (connu(match.getJoueurA()) && connu(match.getJoueurB())) {
            envoyerA(tournoi, TypeConvocation.ARBITRAGE, numeroLicence, nomComplet,
                    TexteConvocation.arbitre(prenom, match.getJoueurA(), match.getJoueurB(), table, contexte),
                    maintenant, messages, sansNumero);
        }
        return new ResultatConvocationsSms(true, messages, sansNumero);
    }

    private void envoyerA(Tournoi tournoi, TypeConvocation type, String licence, String nom, String texte,
                          LocalDateTime maintenant, List<MessageSms> messages, List<String> sansNumero) {
        ConvocationsSms cs = tournoi.getConvocationsSms();
        ContactSms contact = cs.contact(licence);
        if (contact == null) {
            sansNumero.add(nom == null || nom.isBlank() ? licence : nom);
            return;
        }
        MessageSms message;
        if (cs.nombreEnvoyes() >= cs.getLimiteEnvois()) {
            message = new MessageSms(maintenant, type, licence, nom, texte, StatutEnvoiSms.LIMITE_ATTEINTE,
                    "limite de " + cs.getLimiteEnvois() + " SMS");
        } else {
            ResultatEnvoiSms r;
            try {
                r = envoi.envoyer(expediteur(tournoi), contact.numero(), texte);
            } catch (RuntimeException e) {
                r = ResultatEnvoiSms.echec(e.getMessage());
            }
            if (r == null) {
                r = ResultatEnvoiSms.echec("aucune réponse du service d'envoi");
            }
            message = new MessageSms(maintenant, type, licence, nom, texte,
                    r.reussi() ? StatutEnvoiSms.ENVOYE : StatutEnvoiSms.ECHEC,
                    r.reussi() ? r.identifiant() : masquerNumeros(r.motif()));
        }
        cs.journaliser(message);
        messages.add(message);
    }

    /**
     * RGPD : le motif d'échec du prestataire peut citer le numéro ; il resterait
     * dans le journal après l'effacement. Toute suite d'au moins 6 chiffres
     * (espaces, points, tirets, parenthèses, + compris) est masquée.
     */
    static String masquerNumeros(String texte) {
        return texte == null ? null : texte.replaceAll("\\+?\\(?\\d(?:[\\s.\\-()\\u00a0\\u202f]*\\d){5,}", "••");
    }

    private static boolean connu(Joueur j) {
        return j != null && !j.estADeterminer();
    }
}
