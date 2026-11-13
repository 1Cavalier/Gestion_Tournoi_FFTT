package com.pingmanager.core;

import com.pingmanager.model.AccesClub;
import com.pingmanager.model.CompteUtilisateur;
import com.pingmanager.model.RoleCompte;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Point 16 : chaque opération sensible, chaque cas de refus. */
class ControleDroitsTest {

    private static final String CLUB = "08910001";
    private static final String AUTRE = "08750002";

    private static CompteUtilisateur compte(String email, boolean verifie) {
        CompteUtilisateur c = new CompteUtilisateur(email, "NOM", "Prenom", "h:x");
        if (verifie) {
            c.marquerEmailVerifie();
        }
        return c;
    }

    private static DroitRefuseException.Motif motif(CompteUtilisateur c, List<AccesClub> acces, boolean supervision,
                                                    OperationProtegee op, String club) {
        return ControleDroits.refus(c, acces, supervision, op, club).map(DroitRefuseException::getMotif).orElse(null);
    }

    @Test
    void organisateur_du_club_a_tous_les_droits_du_club_sauf_la_supervision() {
        CompteUtilisateur c = compte("orga@example.org", true);
        List<AccesClub> acces = List.of(new AccesClub(CLUB, c.getEmail()));
        for (OperationProtegee op : OperationProtegee.values()) {
            DroitRefuseException.Motif m = motif(c, acces, false, op, op == OperationProtegee.SUPERVISION ? null : CLUB);
            if (op == OperationProtegee.SUPERVISION) {
                assertEquals(DroitRefuseException.Motif.SUPERVISION_REQUISE, m);
            } else {
                assertEquals(null, m, op.name());
            }
        }
    }

    @Test
    void chaque_operation_refusee_sans_compte_sans_email_verifie_et_pour_un_autre_club() {
        CompteUtilisateur nonVerifie = compte("nv@example.org", false);
        CompteUtilisateur autreClub = compte("autre@example.org", true);
        for (OperationProtegee op : OperationProtegee.values()) {
            assertEquals(DroitRefuseException.Motif.NON_CONNECTE, motif(null, List.of(), false, op, CLUB), op.name());
            assertEquals(DroitRefuseException.Motif.EMAIL_NON_VERIFIE,
                    motif(nonVerifie, List.of(new AccesClub(CLUB, nonVerifie.getEmail())), false, op, CLUB), op.name());
            DroitRefuseException.Motif attendu = op == OperationProtegee.SUPERVISION
                    ? DroitRefuseException.Motif.SUPERVISION_REQUISE : DroitRefuseException.Motif.AUCUN_DROIT_SUR_LE_CLUB;
            assertEquals(attendu, motif(autreClub, List.of(new AccesClub(AUTRE, autreClub.getEmail())), false, op, CLUB),
                    op.name());
        }
    }

    @Test
    void acces_d_un_autre_compte_ne_compte_pas() {
        CompteUtilisateur c = compte("orga@example.org", true);
        assertEquals(DroitRefuseException.Motif.AUCUN_DROIT_SUR_LE_CLUB, motif(c,
                List.of(new AccesClub(CLUB, "quelquun@example.org")), false, OperationProtegee.MODIFIER_TOURNOI, CLUB));
    }

    @Test
    void tournoi_sans_numero_de_club_n_appartient_a_personne() {
        CompteUtilisateur c = compte("orga@example.org", true);
        assertEquals(DroitRefuseException.Motif.CLUB_NON_DESIGNE, motif(c, List.of(new AccesClub(CLUB, c.getEmail())),
                false, OperationProtegee.MODIFIER_TOURNOI, " "));
    }

    @Test
    void supervision_honoree_modifie_et_consulte_mais_ne_cree_ni_ne_supprime_ni_ne_gere_les_comptes() {
        CompteUtilisateur sup = compte("supervision@example.org", true);
        sup.setRole(RoleCompte.SUPERVISION);
        assertEquals(null, motif(sup, List.of(), true, OperationProtegee.SUPERVISION, null));
        assertEquals(null, motif(sup, List.of(), true, OperationProtegee.MODIFIER_TOURNOI, CLUB));
        assertEquals(null, motif(sup, List.of(), true, OperationProtegee.HOMOLOGUER_TOURNOI, CLUB));
        assertEquals(null, motif(sup, List.of(), true, OperationProtegee.DONNEES_CLUB, CLUB));
        assertEquals(DroitRefuseException.Motif.AUCUN_DROIT_SUR_LE_CLUB,
                motif(sup, List.of(), true, OperationProtegee.CREER_TOURNOI, CLUB));
        assertEquals(DroitRefuseException.Motif.AUCUN_DROIT_SUR_LE_CLUB,
                motif(sup, List.of(), true, OperationProtegee.SUPPRIMER_TOURNOI, CLUB));
        assertEquals(DroitRefuseException.Motif.AUCUN_DROIT_SUR_LE_CLUB,
                motif(sup, List.of(), true, OperationProtegee.COMPTES_CLUB, CLUB));
        // Privilège non honoré (hors mode démo) : plus rien.
        assertEquals(DroitRefuseException.Motif.SUPERVISION_REQUISE,
                motif(sup, List.of(), false, OperationProtegee.SUPERVISION, null));
        assertEquals(DroitRefuseException.Motif.AUCUN_DROIT_SUR_LE_CLUB,
                motif(sup, List.of(), false, OperationProtegee.MODIFIER_TOURNOI, CLUB));
    }

    @Test
    void exiger_leve_une_exception_au_message_clair() {
        DroitRefuseException e = assertThrows(DroitRefuseException.class, () -> ControleDroits.exiger(
                compte("x@example.org", true), List.of(), false, OperationProtegee.MODIFIER_TOURNOI, CLUB));
        assertEquals("Action refusée : vous n'avez pas le droit de modifier ce tournoi : votre compte n'est pas "
                + "rattaché au club n° " + CLUB + ".", e.getMessage());
        assertEquals(CLUB, e.getNumeroClub());
        assertTrue(e.getOperation() == OperationProtegee.MODIFIER_TOURNOI);
    }
}
