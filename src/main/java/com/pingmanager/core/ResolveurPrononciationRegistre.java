package com.pingmanager.core;

import com.pingmanager.model.PrononciationClub;
import com.pingmanager.model.PrononciationJoueur;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Prononciation des annonces vocales adossée au {@link RegistrePrononciations}
 * (B018) : prénom et nom par la licence du joueur, club par son numéro.
 *
 * <p>Le joueur d'une annonce ne connaît que le nom de son club : le numéro est
 * retrouvé à partir de la licence ({@code numeroClubDuJoueur}, branché sur la
 * base des licences). Sans entrée dans le registre, rien n'est personnalisé :
 * la voix lit le texte, comme avec {@link ResolveurPrononciation#TEXTE_NORMAL}.</p>
 */
public final class ResolveurPrononciationRegistre implements ResolveurPrononciation {

    private final RegistrePrononciations registre;
    private final Function<String, Optional<String>> numeroClubDuJoueur;

    /**
     * @param numeroClubDuJoueur licence → numéro du club (vide si inconnu) ; null = clubs jamais personnalisés
     */
    public ResolveurPrononciationRegistre(RegistrePrononciations registre,
                                          Function<String, Optional<String>> numeroClubDuJoueur) {
        this.registre = Objects.requireNonNull(registre, "registre");
        this.numeroClubDuJoueur = numeroClubDuJoueur == null ? l -> Optional.empty() : numeroClubDuJoueur;
    }

    /** Sans licence, aucune entrée du registre ne peut correspondre. */
    @Override
    public Optional<String> phonemesIpa(SegmentAnnonce segment) {
        return Optional.empty();
    }

    @Override
    public Optional<String> phonemesIpa(SegmentAnnonce segment, String licence) {
        if (segment == null || licence == null || licence.isBlank()) {
            return Optional.empty();
        }
        SegmentAnnonce.Nature n = segment.nature();
        if (n == SegmentAnnonce.Nature.PRENOM) {
            return registre.joueur(licence).map(PrononciationJoueur::prenom).filter(s -> !s.isEmpty());
        }
        if (n == SegmentAnnonce.Nature.NOM) {
            return registre.joueur(licence).map(PrononciationJoueur::nom).filter(s -> !s.isEmpty());
        }
        if (n == SegmentAnnonce.Nature.CLUB) {
            Optional<String> numero = numeroClubDuJoueur.apply(licence);
            return numero == null ? Optional.empty()
                    : numero.flatMap(registre::club).map(PrononciationClub::ipa).filter(s -> !s.isEmpty());
        }
        return Optional.empty();
    }
}
