package com.pingmanager.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Point 21 : la mention d'information dit quelles données, pourquoi, combien de temps, qui contacter. */
class MentionInformationTest {

    @Test
    void la_version_complete_repond_aux_quatre_questions_et_donne_la_duree_configuree() {
        String t = MentionInformation.complete(3);
        assertTrue(t.contains("Quelles données ?"));
        assertTrue(t.contains("Pourquoi ?"));
        assertTrue(t.contains("Combien de temps ?"));
        assertTrue(t.contains("Qui contacter ?"));
        assertTrue(t.contains("après 3 ans"));
        assertTrue(MentionInformation.complete(5).contains("après 5 ans"));
        assertTrue(t.contains("durée à confirmer"), "aucune durée légale devinée pour les résultats");
    }

    @Test
    void les_versions_courtes_renvoient_vers_le_detail_ou_le_correspondant_sans_promesse_juridique() {
        assertTrue(MentionInformation.CREATION_COMPTE.contains("Mon profil"));
        assertTrue(MentionInformation.INSCRIPTION.contains("correspondant du tournoi"));
        for (String s : new String[] {MentionInformation.CREATION_COMPTE, MentionInformation.INSCRIPTION,
                MentionInformation.complete(3)}) {
            assertFalse(s.toLowerCase().contains("conforme au rgpd"), "aucune conformité affirmée");
            assertFalse(s.contains("@"), "aucune adresse réelle dans le texte");
        }
    }
}
