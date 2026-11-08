package com.pingmanager.ui.infra;

import com.pingmanager.core.EnvoiSms;
import com.pingmanager.core.NumeroPortable;
import com.pingmanager.core.ResultatEnvoiSms;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Implémentation <b>simulée</b> du port {@link EnvoiSms} (B019) : aucun SMS ne
 * part, le message est retenu (numéro masqué) pour être montré à l'écran.
 *
 * <p>Le vrai prestataire (Brevo recommandé, {@code docs/RECHERCHE_SMS.md})
 * remplacera cette classe sans toucher au cœur.</p>
 */
public final class EnvoiSmsSimule implements EnvoiSms {

    /** Nombre maximal de messages gardés en mémoire (les plus anciens sortent). */
    static final int CAPACITE = 200;

    /** Un SMS « envoyé » par la simulation. */
    public record Message(LocalDateTime instant, String expediteur, String numeroMasque, String texte,
                          String identifiant) {
    }

    private final List<Message> messages = new ArrayList<>();
    private int compteur;

    @Override
    public synchronized ResultatEnvoiSms envoyer(String expediteur, String numero, String texte) {
        compteur++;
        String identifiant = "simulation-" + compteur;
        messages.add(new Message(LocalDateTime.now(), expediteur, NumeroPortable.masquer(numero), texte,
                identifiant));
        if (messages.size() > CAPACITE) {
            messages.remove(0);
        }
        return ResultatEnvoiSms.envoye(identifiant);
    }

    /** Messages simulés, du plus ancien au plus récent. */
    public synchronized List<Message> messages() {
        return Collections.unmodifiableList(new ArrayList<>(messages));
    }
}
