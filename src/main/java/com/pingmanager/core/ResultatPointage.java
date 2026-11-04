package com.pingmanager.core;

import com.pingmanager.model.Joueur;

import java.util.Collections;
import java.util.List;

/**
 * Compte rendu de la clôture du pointage d'un tableau (mécanisme de présence,
 * jour J), produit par {@link ServicePointage#cloturerPointage}.
 *
 * <p>Décrit ce que la clôture a modifié :</p>
 * <ul>
 *   <li>{@link #getLiberes()} : les inscrits qui n'avaient pas pointé pour ce
 *       tableau, retirés (leur place est libérée), quel que soit leur état de
 *       paiement ;</li>
 *   <li>{@link #getRepeches()} : les joueurs repêchés depuis la liste d'attente
 *       pour combler les places libérées. Un repêché n'a par définition jamais
 *       pointé ni payé pour ce tableau : il doit obligatoirement passer par le
 *       pointage (où il règlera son engagement). Tous les repêchés sont donc
 *       « à pointer ».</li>
 * </ul>
 *
 * <p>Objet valeur immuable.</p>
 */
public final class ResultatPointage {

    private final List<Joueur> liberes;
    private final List<Joueur> repeches;

    ResultatPointage(List<Joueur> liberes, List<Joueur> repeches) {
        this.liberes = List.copyOf(liberes);
        this.repeches = List.copyOf(repeches);
    }

    public List<Joueur> getLiberes() {
        return Collections.unmodifiableList(liberes);
    }

    /** @return les joueurs repêchés (tous « à pointer » : ni pointés ni payés sur ce tableau). */
    public List<Joueur> getRepeches() {
        return Collections.unmodifiableList(repeches);
    }

    public int nombreLiberes() {
        return liberes.size();
    }

    public int nombreRepeches() {
        return repeches.size();
    }

    @Override
    public String toString() {
        return "ResultatPointage[libérés=" + liberes.size()
                + ", repêchés (à pointer)=" + repeches.size() + "]";
    }
}
