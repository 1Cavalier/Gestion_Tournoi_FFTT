package com.pingmanager.core;

import com.pingmanager.core.SyntheseVocale.ParametresVoix;
import com.pingmanager.core.SyntheseVocale.ResultatSynthese;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Bouton « Écouter » d'une prononciation (B018). */
class EcoutePrononciationTest {

    private static final class VoixFactice implements SyntheseVocale {
        boolean disponible = true;
        final List<String> dits = new ArrayList<>();

        @Override
        public boolean estDisponible() {
            return disponible;
        }

        @Override
        public ResultatSynthese dire(String ssml, ParametresVoix voix) {
            dits.add(ssml);
            return ResultatSynthese.ok();
        }
    }

    private final VoixFactice voix = new VoixFactice();
    private final ServiceAnnoncesVocales service = new ServiceAnnoncesVocales(Horloge.systeme(), voix);

    @Test
    void ecouter_dit_la_prononciation_en_balise_phoneme() {
        ResultatSynthese r = service.ecouterPrononciation(List.of(new GenerateurSsml.Morceau("Quentin", "kɑ̃.tɛ̃"),
                new GenerateurSsml.Morceau("Soumet", "")), null);
        assertTrue(r.dite());
        assertEquals(1, voix.dits.size());
        assertTrue(voix.dits.get(0).contains("<phoneme alphabet=\"ipa\" ph=\"kɑ̃.tɛ̃\">Quentin</phoneme> Soumet"),
                voix.dits.get(0));
    }

    @Test
    void voix_indisponible_echec_avec_motif_sans_rien_dire() {
        voix.disponible = false;
        ResultatSynthese r = service.ecouterPrononciation(List.of(new GenerateurSsml.Morceau("Soumet", "su.mɛ")),
                ParametresVoix.PAR_DEFAUT);
        assertFalse(r.dite());
        assertTrue(r.motif().contains("indisponible"), r.motif());
        assertTrue(voix.dits.isEmpty());
    }

    @Test
    void prononciation_refusee_ou_rien_a_dire_n_est_pas_dite() {
        ResultatSynthese r = service.ecouterPrononciation(List.of(new GenerateurSsml.Morceau("Soumet", "su\"mɛ")), null);
        assertFalse(r.dite());
        assertTrue(r.motif().startsWith("Caractère non autorisé"), r.motif());
        assertFalse(service.ecouterPrononciation(List.of(new GenerateurSsml.Morceau(" ", "a")), null).dite());
        assertFalse(service.ecouterPrononciation(null, null).dite());
        assertTrue(voix.dits.isEmpty());
    }
}
