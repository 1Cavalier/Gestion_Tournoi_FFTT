package com.pingmanager.core;

import com.pingmanager.core.reglement.LibellesTableau;
import com.pingmanager.model.GrilleRecompenses;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Calcule le tableau de bord <b>remplissage et finances</b> d'un tournoi (B036,
 * cahier des charges v0, section 6) : voir {@link BilanRemplissageFinances}.
 *
 * <p>Lecture seule. Réutilise le tarif de {@link RemplissageTableaux#tarifEnLigne}
 * (le même que le montant à régler d'un joueur) et les totaux de grille de
 * {@link LibellesTableau} (dotation en argent, part des inscriptions).</p>
 */
public final class ServiceRemplissageFinances {

    /** @return le bilan, tableaux dans l'ordre du planning. */
    public BilanRemplissageFinances calculer(Tournoi tournoi) {
        Objects.requireNonNull(tournoi, "tournoi");
        List<BilanRemplissageFinances.LigneTableau> lignes = new ArrayList<>();
        for (Tableau tab : RemplissageTableaux.ordrePlanning(tournoi)) {
            lignes.add(ligne(tab));
        }
        return new BilanRemplissageFinances(lignes);
    }

    static BilanRemplissageFinances.LigneTableau ligne(Tableau tab) {
        int inscrits = tab.getInscrits().size();
        int payes = 0;
        for (Joueur j : tab.getInscrits()) {
            if (tab.aPaye(j)) {
                payes++;
            }
        }
        double tarif = RemplissageTableaux.tarifEnLigne(tab);
        double attendues = BilanRemplissageFinances.centimes(tarif * inscrits);
        double encaisse = BilanRemplissageFinances.centimes(tarif * payes);
        GrilleRecompenses grille = tab.getConfiguration().getRecompenses();
        double fixe = BilanRemplissageFinances.centimes(LibellesTableau.totalArgent(grille));
        // Part des droits d'engagement du tableau, prise sur les recettes attendues (voir le journal B036).
        double pct = Math.min(100, LibellesTableau.partInscriptions(grille));
        double part = BilanRemplissageFinances.centimes(attendues * pct / 100.0);
        return new BilanRemplissageFinances.LigneTableau(tab.getNom(), inscrits,
                Math.max(0, tab.getConfiguration().getPlacesMax()), tab.getFileAttente().size(), payes, tarif,
                attendues, encaisse, fixe, pct, part);
    }
}
