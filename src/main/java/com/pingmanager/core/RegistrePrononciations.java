package com.pingmanager.core;

import com.pingmanager.model.PrononciationClub;
import com.pingmanager.model.PrononciationJoueur;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Registre des prononciations phonétiques (B018) : par licence pour les joueurs,
 * par numéro de club pour les clubs. Il vit dans les données de l'application
 * (réutilisé d'un tournoi à l'autre), pas dans le tournoi.
 *
 * <p>Toute saisie passe par {@link ValidationPrononciation} : le registre ne
 * contient que des prononciations sûres pour le SSML. Une saisie vide efface.
 * Sûr entre fils : il est lu par le fil des annonces et modifié par l'écran.</p>
 */
public final class RegistrePrononciations {

    private final Map<String, PrononciationJoueur> joueurs = new LinkedHashMap<>();
    private final Map<String, PrononciationClub> clubs = new LinkedHashMap<>();

    /**
     * Enregistre (ou efface si les deux sont vides) la prononciation d'un joueur.
     * Rien n'est modifié si l'une des deux saisies est refusée.
     *
     * @return pour le résultat accepté, {@code ipa} vaut « prénom nom » normalisés
     */
    public synchronized ResultatSaisiePrononciation definirJoueur(String licence, String prenomIpa, String nomIpa) {
        if (licence == null || licence.isBlank()) {
            return ResultatSaisiePrononciation.refusee("Licence du joueur inconnue : prononciation non enregistrée.");
        }
        ResultatSaisiePrononciation p = ValidationPrononciation.verifier(prenomIpa);
        if (!p.valide()) {
            return ResultatSaisiePrononciation.refusee("Prénom : " + p.motif());
        }
        ResultatSaisiePrononciation n = ValidationPrononciation.verifier(nomIpa);
        if (!n.valide()) {
            return ResultatSaisiePrononciation.refusee("Nom : " + n.motif());
        }
        PrononciationJoueur entree = new PrononciationJoueur(licence, p.ipa(), n.ipa());
        if (entree.estVide()) {
            joueurs.remove(entree.licence());
        } else {
            joueurs.put(entree.licence(), entree);
        }
        return ResultatSaisiePrononciation.acceptee((p.ipa() + " " + n.ipa()).strip());
    }

    /**
     * Enregistre (ou efface si vide) la prononciation d'un club.
     *
     * @param nomClub nom du club, gardé pour l'affichage
     */
    public synchronized ResultatSaisiePrononciation definirClub(String numeroClub, String nomClub, String ipa) {
        if (numeroClub == null || numeroClub.isBlank()) {
            return ResultatSaisiePrononciation.refusee("Numéro du club inconnu : prononciation non enregistrée.");
        }
        ResultatSaisiePrononciation r = ValidationPrononciation.verifier(ipa);
        if (!r.valide()) {
            return r;
        }
        PrononciationClub entree = new PrononciationClub(numeroClub, nomClub, r.ipa());
        if (r.estEffacement()) {
            clubs.remove(entree.numero());
        } else {
            clubs.put(entree.numero(), entree);
        }
        return r;
    }

    public synchronized Optional<PrononciationJoueur> joueur(String licence) {
        return licence == null ? Optional.empty() : Optional.ofNullable(joueurs.get(licence.trim()));
    }

    public synchronized Optional<PrononciationClub> club(String numeroClub) {
        return numeroClub == null ? Optional.empty() : Optional.ofNullable(clubs.get(numeroClub.trim()));
    }

    /** @return les prononciations des joueurs, dans l'ordre de saisie (copie) */
    public synchronized List<PrononciationJoueur> tousLesJoueurs() {
        return new ArrayList<>(joueurs.values());
    }

    /** @return les prononciations des clubs, dans l'ordre de saisie (copie) */
    public synchronized List<PrononciationClub> tousLesClubs() {
        return new ArrayList<>(clubs.values());
    }

    public synchronized boolean estVide() {
        return joueurs.isEmpty() && clubs.isEmpty();
    }
}
