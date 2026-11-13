package com.pingmanager.ui.ecran;

import com.pingmanager.model.Tournoi;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.util.Optional;

/**
 * Bloc de saisie facultative du portable avec case de consentement explicite
 * (B019), commun à l'inscription en ligne et à l'ajout par le club. Vue mince :
 * textes dans {@link PresentationSaisieSms}, règles dans le cœur.
 */
final class BlocSaisieSms {

    private final TextField numero = Ui.champ(PresentationSaisieSms.INVITE_NUMERO);
    private final CheckBox consentement;
    private final Label etat = Ui.discret("");
    private final VBox racine;

    BlocSaisieSms(String libelleConsentement, String aide) {
        numero.setPrefColumnCount(14);
        numero.setMaxWidth(260);
        consentement = new CheckBox(libelleConsentement);
        consentement.setWrapText(true);
        consentement.setSelected(false); // RGPD : jamais coché d'avance
        consentement.getStyleClass().add("sms-consentement");
        etat.getStyleClass().add("sms-etat-numero");
        racine = new VBox(8);
        if (aide != null) {
            racine.getChildren().add(Ui.aide(aide));
        }
        racine.getChildren().addAll(etat, Ui.libelleChamp("Numéro de portable"), numero, consentement);
        racine.getStyleClass().add("sms-saisie");
    }

    /** Met à jour l'état du numéro déjà connu pour cette licence et le rappel d'effacement. */
    void montrerEtat(Tournoi t, String licence) {
        etat.setText(PresentationSaisieSms.etat(t, licence) + " " + PresentationSaisieSms.rappelEffacement(t));
    }

    VBox noeud() {
        return racine;
    }

    String saisie() {
        return numero.getText();
    }

    boolean consenti() {
        return consentement.isSelected();
    }

    boolean estVide() {
        return numero.getText() == null || numero.getText().isBlank();
    }

    /** Refus éventuel de la saisie (champ marqué en erreur). */
    Optional<String> refus() {
        Optional<String> r = PresentationSaisieSms.refus(saisie(), consenti());
        if (r.isPresent()) {
            Ui.marquerErreur(numero);
        }
        return r;
    }
}
