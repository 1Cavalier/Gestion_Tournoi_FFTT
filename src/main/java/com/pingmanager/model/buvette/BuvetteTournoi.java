package com.pingmanager.model.buvette;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Buvette d'un club pendant un tournoi (B026) : le réglage (stock seulement ou
 * stock et ventes) et les ventes du tournoi. Le stock, lui, reste celui du
 * club ({@link StockBuvette}).
 *
 * <p>Entité mutable gardée : les ventes ne s'ajoutent et ne s'annulent que
 * par {@code core.buvette.ServiceVentesBuvette}. Aucune vente n'est retirée.</p>
 */
public final class BuvetteTournoi {

    private final String referenceTournoi;
    private String nomTournoi;
    private ModeBuvette mode = ModeBuvette.STOCK_SEUL;
    private final List<VenteBuvette> ventes = new ArrayList<>();

    /**
     * @param referenceTournoi identifiant du tournoi (nom de son fichier, sans dossier ni extension)
     * @param nomTournoi       nom affiché (gardé si le fichier du tournoi disparaît)
     */
    public BuvetteTournoi(String referenceTournoi, String nomTournoi) {
        if (referenceTournoi == null || referenceTournoi.isBlank()) {
            throw new IllegalArgumentException("tournoi obligatoire");
        }
        this.referenceTournoi = referenceTournoi.trim();
        renommer(nomTournoi);
    }

    public String getReferenceTournoi() {
        return referenceTournoi;
    }

    public String getNomTournoi() {
        return nomTournoi;
    }

    /** Met à jour le nom affiché (vide : la référence). */
    public void renommer(String nom) {
        this.nomTournoi = nom == null || nom.isBlank() ? referenceTournoi : nom.trim();
    }

    public ModeBuvette getMode() {
        return mode;
    }

    public void setMode(ModeBuvette mode) {
        this.mode = Objects.requireNonNull(mode, "mode");
    }

    /** @return les ventes, de la plus ancienne à la plus récente, annulées comprises (lecture seule). */
    public List<VenteBuvette> getVentes() {
        return Collections.unmodifiableList(ventes);
    }

    public Optional<VenteBuvette> vente(int numero) {
        for (VenteBuvette v : ventes) {
            if (v.numero() == numero) {
                return Optional.of(v);
            }
        }
        return Optional.empty();
    }

    /** Numéro de la prochaine vente. */
    public int prochainNumero() {
        int max = 0;
        for (VenteBuvette v : ventes) {
            max = Math.max(max, v.numero());
        }
        return max + 1;
    }

    /** La plus récente vente non annulée. */
    public Optional<VenteBuvette> derniereVenteValable() {
        for (int i = ventes.size() - 1; i >= 0; i--) {
            if (!ventes.get(i).estAnnulee()) {
                return Optional.of(ventes.get(i));
            }
        }
        return Optional.empty();
    }

    /** Ajoute une vente (numéro unique dans le tournoi). */
    public void ajouter(VenteBuvette v) {
        Objects.requireNonNull(v, "vente");
        if (!v.referenceTournoi().equals(referenceTournoi)) {
            throw new IllegalArgumentException("vente d'un autre tournoi");
        }
        if (vente(v.numero()).isPresent()) {
            throw new IllegalArgumentException("numéro de vente déjà pris : " + v.numero());
        }
        ventes.add(v);
    }

    /** Remplace une vente par sa version annulée (même numéro) ; rien n'est effacé. */
    public void remplacer(VenteBuvette v) {
        for (int i = 0; i < ventes.size(); i++) {
            if (ventes.get(i).numero() == v.numero()) {
                ventes.set(i, v);
                return;
            }
        }
        throw new IllegalArgumentException("vente inconnue : " + v.numero());
    }

    /** @return vrai si rien n'est à garder (mode par défaut, aucune vente). */
    public boolean estVide() {
        return mode == ModeBuvette.STOCK_SEUL && ventes.isEmpty();
    }
}
