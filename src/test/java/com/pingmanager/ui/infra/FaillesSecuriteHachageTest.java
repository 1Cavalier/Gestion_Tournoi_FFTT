package com.pingmanager.ui.infra;

import org.junit.jupiter.api.Test;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DÉMONSTRATION DE FAILLE (audit de sécurité, voir RAPPORT_SECURITE.md).
 *
 * <p>Ce test passe <b>parce que</b> le code actuel est permissif. Lors de la
 * correction, il devra être <b>inversé</b> (empreinte refusée ou marquée à
 * re-hacher) et renommé sans le suffixe « faille_S19 ».</p>
 */
class FaillesSecuriteHachageTest {

    /**
     * Faille S-19 : le nombre d'itérations est lu dans l'empreinte stockée, sans
     * plancher ni plafond. Une empreinte à 1 itération (affaiblie, ou forgée dans le
     * fichier) est acceptée ; à l'inverse, 2 147 483 647 itérations bloquent la
     * connexion (déni de service). Correctif : bornes [120 000 ; 2 000 000], et
     * re-hachage transparent à la connexion si l'empreinte est sous le paramètre courant.
     */
    @Test
    void empreinte_a_une_seule_iteration_acceptee_faille_S19() throws Exception {
        byte[] sel = new byte[16];
        PBEKeySpec spec = new PBEKeySpec("secret".toCharArray(), sel, 1, 256);
        byte[] hash = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        Base64.Encoder b64 = Base64.getEncoder().withoutPadding();
        String empreinteFaible = "pbkdf2$1$" + b64.encodeToString(sel) + "$" + b64.encodeToString(hash);

        assertTrue(new HachageMotDePassePBKDF2().verifier("secret", empreinteFaible),
                "comportement permissif : 1 itération et sel nul acceptés");
    }
}
