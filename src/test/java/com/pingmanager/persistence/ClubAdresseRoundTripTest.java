package com.pingmanager.persistence;

import com.pingmanager.model.Club;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** L'adresse du club (voie + code postal + ville) sert d'adresse par défaut des tournois. */
class ClubAdresseRoundTripTest {

    @Test
    void adresse_complete_du_club_relue_a_l_identique() {
        Club c = new Club("08911132", "Brunoy", "91", "Brunoy", "club@exemple.fr");
        c.setAdresse1("1 rue du Gymnase");
        c.setCodePostal("91800");

        Club relu = ApplicationMapper.lireClub(ApplicationMapper.ecrireClub(c));

        assertEquals("1 rue du Gymnase", relu.getAdresse1());
        assertEquals("91800", relu.getCodePostal());
        assertEquals("Brunoy", relu.getVille());
    }

    @Test
    void ancien_fichier_sans_code_postal_reste_lisible() {
        Club c = new Club("08911132", "Brunoy", "91", "Brunoy", "club@exemple.fr");
        Club relu = ApplicationMapper.lireClub(ApplicationMapper.ecrireClub(c));
        assertNull(relu.getCodePostal());
    }
}
