package com.pingmanager.ui.ecran;

import com.pingmanager.core.ResultatContactSms;
import com.pingmanager.core.ServiceConvocationsSms;
import com.pingmanager.model.ContactSms;
import com.pingmanager.model.ConvocationsSms;
import com.pingmanager.model.Tournoi;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

/**
 * Textes de la saisie facultative du numéro de portable pour les convocations
 * par SMS (B019), sans JavaFX pour être testés : inscription en ligne du joueur
 * ({@link AssistantInscription}) et ajout par le club ({@link VueInscriptionsClub},
 * {@link EcranConvocationsSms}).
 *
 * <p>Le consentement est <b>explicite</b> : la case n'est jamais cochée
 * d'avance et sans elle aucun numéro n'est gardé (contrôle par
 * {@link ServiceConvocationsSms}).</p>
 */
final class PresentationSaisieSms {

    static final String TITRE = "Convocations par SMS (facultatif)";

    static final String INVITE_NUMERO = "06 12 34 56 78";

    static final String AIDE_JOUEUR = "Laissez votre portable pour recevoir la table et l'adversaire de chaque "
            + "match. Messages écrits seulement, jamais d'appel. Sans numéro, rien ne change pour vous.";

    static final String CONSENTEMENT_JOUEUR = "J'accepte de recevoir mes convocations par SMS pour ce tournoi. "
            + "Mon numéro sert uniquement à cela et il sera effacé à la fin du tournoi.";

    static final String CONSENTEMENT_CLUB = "La personne accepte de recevoir ses convocations par SMS pour ce "
            + "tournoi. Son numéro sert uniquement à cela et il sera effacé à la fin du tournoi.";

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRANCE);

    private PresentationSaisieSms() {
    }

    /** État du numéro d'une licence : « Aucun numéro enregistré. » ou le numéro masqué et la date d'accord. */
    static String etat(Tournoi t, String licence) {
        ConvocationsSms c = t.getConvocationsSms();
        ContactSms k = licence == null ? null : c.contact(licence);
        if (k != null) {
            return "Numéro enregistré : " + PresentationConvocationsSms.detailContact(k) + ".";
        }
        if (c.getNumerosEffacesLe() != null) {
            return "Aucun numéro : les numéros ont été effacés le "
                    + DATE.format(c.getNumerosEffacesLe().toLocalDate()) + ".";
        }
        return "Aucun numéro enregistré.";
    }

    /** Un numéro est-il déjà enregistré pour cette licence (bouton « Retirer » proposé) ? */
    static boolean aUnNumero(Tournoi t, String licence) {
        return licence != null && t.getConvocationsSms().contact(licence) != null;
    }

    /** Rappel de l'effacement : « Effacé d'office le 15 novembre 2026, lendemain du dernier jour. » */
    static String rappelEffacement(Tournoi t) {
        LocalDate d = ServiceConvocationsSms.dateEffacementAutomatique(t);
        return d == null ? "Effacé d'office à la fin du tournoi."
                : "Effacé d'office le " + DATE.format(d) + ", lendemain du dernier jour du tournoi.";
    }

    /** Motif du refus d'une saisie facultative, ou vide si on peut poursuivre (saisie vide ou recevable). */
    static Optional<String> refus(String saisie, boolean consentement) {
        return ServiceConvocationsSms.refusSaisieFacultative(saisie, consentement).map(ResultatContactSms::message);
    }

    /** Ligne pour la confirmation de l'inscription en ligne ; vide si aucun numéro n'a été saisi. */
    static Optional<String> apresInscription(ResultatContactSms r) {
        if (r == null) {
            return Optional.empty();
        }
        return Optional.of(r.estReussi()
                ? "Convocations par SMS : " + r.message().substring(0, 1).toLowerCase(Locale.FRANCE)
                + r.message().substring(1)
                : "Convocations par SMS : numéro non enregistré. " + r.message());
    }
}
