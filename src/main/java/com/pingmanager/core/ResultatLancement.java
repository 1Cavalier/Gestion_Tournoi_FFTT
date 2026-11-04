package com.pingmanager.core;

import com.pingmanager.model.Poule;

import java.util.Collections;
import java.util.List;

/**
 * Compte rendu du lancement d'un tableau, produit par
 * {@link ServiceLancementTableau#lancer}.
 *
 * <p>Porte les poules générées, la graine de tirage utilisée (pour rejouer ou
 * vérifier le tirage au sort public) et le nombre de joueurs présents au moment
 * du lancement. Objet valeur immuable.</p>
 */
public final class ResultatLancement {

    private final List<Poule> poules;
    private final long graine;
    private final int nombreDePresents;

    ResultatLancement(List<Poule> poules, long graine, int nombreDePresents) {
        this.poules = List.copyOf(poules);
        this.graine = graine;
        this.nombreDePresents = nombreDePresents;
    }

    public List<Poule> getPoules() {
        return Collections.unmodifiableList(poules);
    }

    public int nombreDePoules() {
        return poules.size();
    }

    /** @return la graine du tirage au sort (tirage reproductible). */
    public long getGraine() {
        return graine;
    }

    public int getNombreDePresents() {
        return nombreDePresents;
    }

    @Override
    public String toString() {
        return "ResultatLancement[poules=" + poules.size()
                + ", présents=" + nombreDePresents + ", graine=" + graine + "]";
    }
}
