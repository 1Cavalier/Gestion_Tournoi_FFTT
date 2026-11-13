package com.pingmanager.ui;

import com.pingmanager.model.Tournoi;
import com.pingmanager.ui.infra.DonneesDemo;

/**
 * Aides des tests du contexte : depuis le point 16 (B044), créer ou modifier un tournoi
 * exige un compte connecté ayant un accès sur le club, désigné par son numéro.
 */
final class TournoisDeTest {

    private TournoisDeTest() {
    }

    /** Connecte le profil de démo (administrateur fictif de Brunoy). */
    static ContexteApplication connecteBrunoy(ContexteApplication ctx) {
        ctx.connexion(DonneesDemo.EMAIL_PROFIL_DEMO, DonneesDemo.MDP_PROFIL_DEMO);
        return ctx;
    }

    /** Rattache le tournoi au club de Brunoy par son numéro. */
    static Tournoi deBrunoy(Tournoi t) {
        t.setNumeroAffiliationClub(DonneesDemo.CLUB_BRUNOY);
        return t;
    }
}
