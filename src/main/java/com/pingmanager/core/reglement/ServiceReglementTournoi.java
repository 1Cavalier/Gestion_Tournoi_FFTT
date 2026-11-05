package com.pingmanager.core.reglement;

import com.pingmanager.model.DocumentReference;
import com.pingmanager.model.DocumentReglement;
import com.pingmanager.model.OptionsReglement;
import com.pingmanager.model.StatutTournoi;
import com.pingmanager.model.Tournoi;

import java.util.Objects;

/**
 * Règlement et feuille d'engagement d'un tournoi, avant et après publication.
 *
 * <ul>
 *   <li>Avant publication, les deux documents sont <b>générés</b> à chaque
 *       demande depuis les données du tournoi, et les options du règlement se
 *       modifient librement.</li>
 *   <li>À la publication ({@link #figer}), une <b>copie figée</b> est gardée
 *       dans le tournoi : le règlement publié ne change plus si un horaire ou
 *       une dotation est modifié ensuite. Un règlement <b>déposé</b> (PDF du
 *       club) est figé par nature : seul son fichier compte.</li>
 *   <li>Après publication, toute modification des options du règlement ou du
 *       fichier déposé est <b>refusée</b> avec un motif
 *       ({@link ResultatModificationReglement.Motif#TOURNOI_PUBLIE}).</li>
 * </ul>
 *
 * <p>Un tournoi publié avant l'existence des copies figées (ancien fichier)
 * reçoit sa copie à la première demande : c'est l'état du tournoi à ce
 * moment-là qui est figé.</p>
 */
public final class ServiceReglementTournoi {

    /** @return vrai si le tournoi est publié (documents figés). */
    public static boolean estFige(Tournoi tournoi) {
        return tournoi.getStatut() == StatutTournoi.PUBLIE;
    }

    /**
     * Garde les copies figées : le règlement s'il est généré, et la feuille
     * d'engagement. Ne remplace pas une copie déjà présente sur un tournoi publié.
     */
    public void figer(Tournoi tournoi) {
        Objects.requireNonNull(tournoi, "tournoi");
        if (tournoi.getReglementFige() == null || tournoi.reglementModifiable()) {
            tournoi.setReglementFige(estGenere(tournoi) ? new GenerateurReglement().generer(tournoi) : null);
        }
        if (tournoi.getFeuilleEngagementFigee() == null || tournoi.reglementModifiable()) {
            tournoi.setFeuilleEngagementFigee(new GenerateurFeuilleEngagement().generer(tournoi));
        }
    }

    /**
     * @return le règlement à afficher ou télécharger : la copie figée d'un tournoi
     *         publié (créée si elle manque), sinon le règlement généré à l'instant
     */
    public DocumentReglement reglement(Tournoi tournoi) {
        Objects.requireNonNull(tournoi, "tournoi");
        if (!estFige(tournoi)) {
            return new GenerateurReglement().generer(tournoi);
        }
        if (tournoi.getReglementFige() == null) {
            tournoi.setReglementFige(new GenerateurReglement().generer(tournoi));
        }
        return tournoi.getReglementFige();
    }

    /**
     * @return la feuille d'engagement : la copie figée d'un tournoi publié (créée
     *         si elle manque), sinon la feuille générée à l'instant
     */
    public DocumentReglement feuilleEngagement(Tournoi tournoi) {
        Objects.requireNonNull(tournoi, "tournoi");
        if (!estFige(tournoi)) {
            return new GenerateurFeuilleEngagement().generer(tournoi);
        }
        if (tournoi.getFeuilleEngagementFigee() == null) {
            tournoi.setFeuilleEngagementFigee(new GenerateurFeuilleEngagement().generer(tournoi));
        }
        return tournoi.getFeuilleEngagementFigee();
    }

    /**
     * Modifie les options du règlement (mode généré / déposé, articles ajoutés).
     *
     * @return {@code APPLIQUEE}, ou {@code TOURNOI_PUBLIE} sans rien changer
     */
    public ResultatModificationReglement modifierOptions(Tournoi tournoi, OptionsReglement options) {
        Objects.requireNonNull(tournoi, "tournoi");
        if (!tournoi.reglementModifiable()) {
            return refus();
        }
        tournoi.setOptionsReglement(options);
        return ResultatModificationReglement.appliquee();
    }

    /**
     * Dépose (ou retire, avec {@code null}) le fichier PDF du règlement du club.
     *
     * @return {@code APPLIQUEE}, ou {@code TOURNOI_PUBLIE} sans rien changer
     */
    public ResultatModificationReglement deposer(Tournoi tournoi, DocumentReference fichier) {
        Objects.requireNonNull(tournoi, "tournoi");
        if (!tournoi.reglementModifiable()) {
            return refus();
        }
        tournoi.setReglementDocument(fichier);
        return ResultatModificationReglement.appliquee();
    }

    private static ResultatModificationReglement refus() {
        return ResultatModificationReglement.refusee(ResultatModificationReglement.Motif.TOURNOI_PUBLIE,
                Tournoi.MOTIF_REGLEMENT_FIGE);
    }

    /** Mode « généré », ou mode non choisi sans fichier déposé (le règlement généré sert alors). */
    private static boolean estGenere(Tournoi tournoi) {
        OptionsReglement o = tournoi.getOptionsReglement();
        if (o.getMode() == OptionsReglement.Mode.DEPOSE) {
            return false;
        }
        return o.getMode() == OptionsReglement.Mode.GENERE || tournoi.getReglementDocument() == null;
    }
}
