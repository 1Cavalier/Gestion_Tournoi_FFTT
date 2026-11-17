package com.pingmanager.core.buvette;

import com.pingmanager.model.buvette.MoyenPaiement;

import java.time.LocalDate;
import java.util.List;

/**
 * Bilan des ventes de la buvette (B026) pour un tournoi ou une journée.
 * Montants en centimes entiers ; les ventes annulées ne comptent pas (elles
 * sont seulement dénombrées).
 *
 * <p>Arrondis : le coût total est la somme exacte des coûts, arrondie au
 * centime une seule fois ; le coût de chaque article est arrondi à part.
 * La somme des lignes peut donc différer du total d'un centime ou deux.</p>
 *
 * @param jour            journée du bilan, {@code null} pour tout le tournoi
 * @param nombreVentes    ventes valables
 * @param nombreAnnulees  ventes annulées (non comptées)
 * @param nombreArticles  portions vendues
 * @param recetteCentimes total encaissé
 * @param coutCentimes    coût des articles vendus (produit + emballage)
 * @param parArticle      détail par article, par recette décroissante
 * @param parMoyen        détail par moyen de paiement (les trois, même à 0)
 */
public record BilanBuvette(LocalDate jour, int nombreVentes, int nombreAnnulees, int nombreArticles,
                           long recetteCentimes, long coutCentimes, List<LigneArticle> parArticle,
                           List<LigneMoyen> parMoyen) {

    public BilanBuvette {
        parArticle = List.copyOf(parArticle);
        parMoyen = List.copyOf(parMoyen);
    }

    /** Marge : recette moins coût des articles vendus. */
    public long margeCentimes() {
        return recetteCentimes - coutCentimes;
    }

    /** Panier moyen (0 sans vente), arrondi au centime. */
    public long panierMoyenCentimes() {
        return nombreVentes == 0 ? 0 : Math.round((double) recetteCentimes / nombreVentes);
    }

    /**
     * Ligne d'un article.
     *
     * @param idArticle       article
     * @param nom             nom à la dernière vente
     * @param quantite        portions vendues
     * @param recetteCentimes recette
     * @param coutCentimes    coût arrondi au centime
     */
    public record LigneArticle(String idArticle, String nom, long quantite, long recetteCentimes,
                               long coutCentimes) {

        public long margeCentimes() {
            return recetteCentimes - coutCentimes;
        }
    }

    /**
     * Ligne d'un moyen de paiement.
     *
     * @param moyen           moyen
     * @param nombreVentes    ventes valables réglées ainsi
     * @param recetteCentimes recette
     */
    public record LigneMoyen(MoyenPaiement moyen, int nombreVentes, long recetteCentimes) {
    }
}
