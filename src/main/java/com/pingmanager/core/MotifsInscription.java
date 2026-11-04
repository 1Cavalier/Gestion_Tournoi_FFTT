package com.pingmanager.core;

import com.pingmanager.core.reglement.Formats;
import com.pingmanager.core.reglement.LibellesTableau;
import com.pingmanager.model.CategorieAge;
import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;

/**
 * Motif en clair d'un résultat d'inscription, pour l'écran (validation en direct) :
 * on dit précisément <i>pourquoi</i> (« Vos 923 points dépassent la limite du tableau
 * (≤ 900 pts) ») plutôt qu'un code.
 */
public final class MotifsInscription {

    private MotifsInscription() {
    }

    /**
     * @param r       le résultat (réel ou simulé)
     * @param tournoi le tournoi
     * @param joueur  le joueur
     * @param tableau le tableau concerné
     */
    public static String texte(ResultatInscriptionTournoi r, Tournoi tournoi, Joueur joueur, Tableau tableau) {
        ResultatInscriptionTournoi.Motif m = r.getMotif();
        ConfigurationTableau c = tableau.getConfiguration();
        if (m == ResultatInscriptionTournoi.Motif.INSCRIT) {
            return "Place disponible";
        }
        if (m == ResultatInscriptionTournoi.Motif.EN_FILE_ATTENTE) {
            int position = tableau.getFileAttente().contains(joueur)
                    ? tableau.getFileAttente().indexOf(joueur) + 1 : tableau.getFileAttente().size() + 1;
            return "Tableau complet : liste d'attente, " + position + (position == 1 ? "re" : "e")
                    + " position — rien à payer sauf si repêché";
        }
        if (m == ResultatInscriptionTournoi.Motif.DEJA_PRESENT) {
            return "Déjà inscrit à ce tableau";
        }
        if (m == ResultatInscriptionTournoi.Motif.LISTE_ATTENTE_PLEINE) {
            return "Complet, liste d'attente pleine";
        }
        if (m == ResultatInscriptionTournoi.Motif.TOURNOI_FERME) {
            return "Les inscriptions à ce tournoi sont fermées";
        }
        if (m == ResultatInscriptionTournoi.Motif.NON_ELIGIBLE) {
            return nonEligible(c, joueur);
        }
        if (m == ResultatInscriptionTournoi.Motif.CATEGORIE_NON_ADMISE) {
            return "Les " + joueur.getCategorie().getLibelle().toLowerCase(java.util.Locale.ROOT)
                    + " ne sont pas admis dans ce tournoi";
        }
        if (m == ResultatInscriptionTournoi.Motif.CATEGORIE_NON_AUTORISEE) {
            return "Tableau réservé aux catégories : " + LibellesTableau.categories(c);
        }
        if (m == ResultatInscriptionTournoi.Motif.QUOTA_TABLEAUX_ATTEINT) {
            String jour = tableau.getHoraires() == null || tableau.getHoraires().getJour() == null ? ""
                    : " le " + Formats.jourLong(tableau.getHoraires().getJour()).replaceAll(" \\d{4}$", "");
            Integer max = tableau.getHoraires() == null || tableau.getHoraires().getJour() == null ? null
                    : tournoi.getQuotasTableaux().limite(tableau.getHoraires().getJour(), joueur.getSexe(), true);
            return "Nombre maximal de tableaux atteint" + jour + (max == null ? "" : " (" + max + " au plus)");
        }
        if (m == ResultatInscriptionTournoi.Motif.TABLEAUX_SUCCESSIFS) {
            Tableau v = r.getTableauEnConflit();
            return "Interdit : suit ou précède directement « " + (v == null ? "?" : v.getNom())
                    + " » (pas deux tableaux qui se suivent)";
        }
        if (m == ResultatInscriptionTournoi.Motif.TABLEAU_FEMININ_OBLIGATOIRE) {
            Tableau f = r.getTableauEnConflit();
            return "Ce jour-là, cochez d'abord « " + (f == null ? "le tableau dames" : f.getNom())
                    + " », obligatoire pour les joueuses";
        }
        return m.name();
    }

    private static String nonEligible(ConfigurationTableau c, Joueur j) {
        if (c.getSexeRequis() != null && j.getSexe() != c.getSexeRequis()) {
            return c.getSexeRequis() == Sexe.FEMME ? "Tableau réservé aux dames" : "Tableau réservé aux messieurs";
        }
        if (!c.pointsAutorises(j.getPoints())) {
            if (c.getPointsMin() > 0 && j.getPoints() < c.getPointsMin()) {
                return "Vos " + j.getPoints() + " points sont sous la tranche du tableau (" + LibellesTableau.points(c) + ")";
            }
            return "Vos " + j.getPoints() + " points dépassent la limite du tableau (" + LibellesTableau.points(c) + ")";
        }
        if (!c.rangNationalAutorise(j)) {
            return "Réservé aux non numérotés (hors n° 1 à " + c.getExclusionRangNational() + ")";
        }
        return "Ne remplit pas les conditions du tableau";
    }

    /** Libellé court d'une catégorie d'âge pour l'affichage (« Senior », « Vétéran 45 »). */
    public static String categorie(CategorieAge c) {
        if (c == null) {
            return "Catégorie inconnue";
        }
        String l = c.getLibelle();
        return l.replace("Vétérans", "Vétéran").replaceAll("s$", "");
    }
}
