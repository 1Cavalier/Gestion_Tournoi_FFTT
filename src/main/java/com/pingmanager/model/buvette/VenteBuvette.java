package com.pingmanager.model.buvette;

import java.time.Instant;
import java.util.Objects;

/**
 * Une vente de la caisse de la buvette (B046), ou l'annulation d'une vente.
 * Les ventes ne se suppriment jamais : l'annulation est un nouvel
 * enregistrement qui cite le numéro annulé (traçabilité).
 *
 * <p>Le nom, le prix et le coût de revient sont recopiés au moment de la vente :
 * l'historique reste juste même si le catalogue change ensuite.</p>
 *
 * @param numero        numéro d'ordre dans le tournoi
 * @param horodatage    instant de la vente
 * @param auteur        qui a vendu
 * @param poste         poste de caisse (une seule caisse par défaut)
 * @param idProduit     produit vendu
 * @param nomProduit    nom au moment de la vente
 * @param prixCentimes  prix encaissé (une unité)
 * @param coutCentimes  coût de revient au moment de la vente, ou -1 s'il est inconnu
 * @param numeroAnnule  0 pour une vente ; pour une annulation, le numéro de la vente annulée
 */
public record VenteBuvette(int numero, Instant horodatage, String auteur, String poste, String idProduit,
                           String nomProduit, long prixCentimes, long coutCentimes, int numeroAnnule) {

    public VenteBuvette {
        if (numero < 1) {
            throw new IllegalArgumentException("numéro au moins 1");
        }
        Objects.requireNonNull(horodatage, "horodatage");
        if (idProduit == null || idProduit.isBlank()) {
            throw new IllegalArgumentException("produit obligatoire");
        }
        auteur = auteur == null || auteur.isBlank() ? "inconnu" : auteur.trim();
        poste = poste == null || poste.isBlank() ? "Caisse 1" : poste.trim();
        nomProduit = nomProduit == null || nomProduit.isBlank() ? idProduit : nomProduit.trim();
    }

    /** @return vrai pour l'annulation d'une vente. */
    public boolean estAnnulation() {
        return numeroAnnule > 0;
    }
}
