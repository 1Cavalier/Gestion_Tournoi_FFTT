package com.pingmanager.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/** B041 : l'opération d'un poste de pointage est immuable et complète. */
class OperationPointageTest {

    private static final Instant T0 = Instant.parse("2026-11-15T08:00:00Z");

    private OperationPointage.Builder base(TypeOperationPointage type) {
        return OperationPointage.builder().licence("123").tableau("A").type(type).horodatage(T0).poste("accueil-1");
    }

    @Test
    void paiement_porte_son_mode_de_reglement() {
        OperationPointage op = base(TypeOperationPointage.MARQUER_PAYE).valeur("CHEQUE").build();
        assertEquals(ModeReglement.CHEQUE, op.getModeReglement());
        assertEquals("CHEQUE", op.getValeur());
        assertEquals(T0, op.getHorodatage());
        assertEquals("accueil-1", op.getPoste());
    }

    @Test
    void paiement_sans_mode_ou_mode_inconnu_refuse() {
        assertThrows(IllegalArgumentException.class, () -> base(TypeOperationPointage.MARQUER_PAYE).build());
        assertThrows(IllegalArgumentException.class,
                () -> base(TypeOperationPointage.MARQUER_PAYE).valeur("CARTE_BLEUE").build());
    }

    @Test
    void desinscription_sans_motif_refusee() {
        assertThrows(IllegalArgumentException.class, () -> base(TypeOperationPointage.DESINSCRIRE).valeur("  ").build());
        assertEquals("Blessé", base(TypeOperationPointage.DESINSCRIRE).valeur(" Blessé ").build().getValeur());
    }

    @Test
    void pointage_ignore_la_valeur_et_exige_poste_et_horodatage() {
        assertNull(base(TypeOperationPointage.POINTER).valeur("x").build().getValeur());
        assertThrows(IllegalArgumentException.class, () -> base(TypeOperationPointage.POINTER).poste(" ").build());
        assertThrows(NullPointerException.class, () -> base(TypeOperationPointage.POINTER).horodatage(null).build());
        assertThrows(IllegalArgumentException.class, () -> base(TypeOperationPointage.POINTER).licence(null).build());
    }

    @Test
    void deux_operations_identiques_sont_egales() {
        assertEquals(base(TypeOperationPointage.POINTER).build(), base(TypeOperationPointage.POINTER).build());
        assertNotEquals(base(TypeOperationPointage.POINTER).build(), base(TypeOperationPointage.DEPOINTER).build());
    }

    @Test
    void mode_de_reglement_garde_et_efface_avec_le_paiement() {
        Tableau a = new Tableau("A", ConfigurationTableau.builder().placesMax(4).build());
        Joueur j = new Joueur("1", "Nom", "Prénom", Sexe.HOMME, "Club", 900);
        a.inscrire(j);
        assertNull(a.modeReglement(j));
        a.marquerPaye(j, ModeReglement.ESPECES);
        assertEquals(ModeReglement.ESPECES, a.modeReglement(j));
        a.annulerPaiement(j);
        assertFalse(a.aPaye(j));
        assertNull(a.modeReglement(j));
        a.marquerPaye(j); // ancien appel : payé sans mode
        assertTrue(a.aPaye(j));
        assertNull(a.modeReglement(j));
    }
}
