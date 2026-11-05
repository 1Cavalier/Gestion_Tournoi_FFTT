package com.pingmanager.ui.ecran;

import com.pingmanager.core.ServiceLancementTableau;
import com.pingmanager.core.ServicePointage;
import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Manche;
import com.pingmanager.model.Match;
import com.pingmanager.model.Poule;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.format.FormatTournoi;
import com.pingmanager.model.format.RegistreFormats;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * PROVISOIRE : tableau d'essai pour tester la vue « Phase finale » à l'écran
 * tant que le pointage et le lancement des poules n'existent pas dans
 * l'interface (page Jour J à venir).
 *
 * <p>Construit en mémoire une <b>copie</b> d'un tableau réel (même format, mêmes
 * inscrits ; des joueurs fictifs s'il y en a moins de six), pointe tout le
 * monde, tire les poules et leur donne des scores aléatoires mais conformes.
 * Rien n'est rattaché au tournoi ni enregistré : le tableau réel n'est pas
 * modifié. À supprimer avec le point d'entrée provisoire.</p>
 */
final class TableauEssaiPhaseFinale {

    /** En dessous de ce nombre d'inscrits, on prend des joueurs fictifs. */
    static final int MINIMUM_INSCRITS = 6;
    /** Nombre de joueurs fictifs utilisés à défaut d'inscrits. */
    static final int JOUEURS_FICTIFS = 24;

    private TableauEssaiPhaseFinale() {
    }

    /**
     * @param modele tableau réel dont on reprend le nom, le format et les inscrits
     * @param graine graine des tirages et des scores aléatoires (reproductible)
     * @return un tableau en mémoire, poules tirées et toutes jouées
     */
    static Tableau creer(Tableau modele, long graine) {
        Objects.requireNonNull(modele, "modele");
        FormatTournoi format = modele.getConfiguration().getFormat() != null
                ? modele.getConfiguration().getFormat() : RegistreFormats.formatParDefaut();
        ConfigurationTableau config = ConfigurationTableau.depuisFormat(format)
                .formatPhaseFinale(modele.getConfiguration().getFormatPhaseFinale())
                .build();
        Tableau essai = new Tableau(modele.getNom() + " (essai)", config);
        Tournoi tournoi = new Tournoi("Essai", "Essai", LocalDate.now());
        tournoi.ajouterTableau(essai);

        List<Joueur> joueurs = new ArrayList<>(modele.getInscrits());
        if (joueurs.size() < MINIMUM_INSCRITS) {
            joueurs = joueursFictifs(JOUEURS_FICTIFS);
        }
        essai.ouvrirPointage();
        for (Joueur j : joueurs) {
            essai.inscrire(j);
            if (essai.getInscrits().contains(j)) {
                essai.pointer(j);
            }
        }
        new ServicePointage(tournoi).cloturerPointage(essai);
        new ServiceLancementTableau().lancer(essai, graine);

        Random rng = new Random(graine);
        for (Poule p : essai.getPoules()) {
            for (Match m : p.getMatchs()) {
                m.definirScore(scoreAleatoire(m.getNombreManchesGagnantes(), rng));
            }
        }
        return essai;
    }

    /** Joueurs fictifs (licences « ESSAI… »), clubs et points variés. */
    static List<Joueur> joueursFictifs(int n) {
        List<Joueur> r = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            r.add(new Joueur("ESSAI" + i, "Essai" + i, "Joueur", Sexe.HOMME,
                    "Club " + (char) ('A' + i % 6), 500 + 37 * i));
        }
        return r;
    }

    /** Score conforme (2.11.1 / 2.12.1) tiré au hasard. */
    static List<Manche> scoreAleatoire(int manchesGagnantes, Random rng) {
        boolean aGagne = rng.nextBoolean();
        int perdues = rng.nextInt(manchesGagnantes);
        List<Boolean> gagneesParVainqueur = new ArrayList<>();
        for (int i = 0; i < manchesGagnantes - 1; i++) {
            gagneesParVainqueur.add(true);
        }
        for (int i = 0; i < perdues; i++) {
            gagneesParVainqueur.add(false);
        }
        java.util.Collections.shuffle(gagneesParVainqueur, rng);
        gagneesParVainqueur.add(true); // la dernière manche est gagnée par le vainqueur

        List<Manche> manches = new ArrayList<>();
        for (boolean pourVainqueur : gagneesParVainqueur) {
            int perdant = rng.nextInt(12); // 0..11 ; 10 ou 11 => prolongation
            int gagnant = perdant >= 10 ? perdant + 2 : 11;
            boolean pourA = pourVainqueur == aGagne;
            manches.add(pourA ? new Manche(gagnant, perdant) : new Manche(perdant, gagnant));
        }
        return manches;
    }
}
