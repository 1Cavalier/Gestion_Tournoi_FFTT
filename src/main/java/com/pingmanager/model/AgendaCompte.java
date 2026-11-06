package com.pingmanager.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Agenda personnel d'un compte (identifié par son email) : dates saisies à la
 * main et tournois ajoutés automatiquement à l'inscription.
 *
 * <p>Conteneur simple, indexé par identifiant de date. Les règles (titre
 * obligatoire, dates de tournoi non modifiables à la main, synchronisation avec
 * les inscriptions) sont dans {@code core.ServiceAgenda}.</p>
 */
public final class AgendaCompte {

    /** Ordre d'affichage : jour, puis « toute la journée » avant les heures, puis titre. */
    public static final Comparator<EvenementAgenda> ORDRE = Comparator
            .comparing(EvenementAgenda::getDate)
            .thenComparing(e -> e.getHeure(), Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(EvenementAgenda::getTitre, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(EvenementAgenda::getId);

    private final String email;
    private final Map<String, EvenementAgenda> parId = new LinkedHashMap<>();

    public AgendaCompte(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email obligatoire");
        }
        this.email = email.trim().toLowerCase();
    }

    public String getEmail() {
        return email;
    }

    /** Ajoute une date, ou remplace celle de même identifiant. */
    public AgendaCompte enregistrer(EvenementAgenda evenement) {
        Objects.requireNonNull(evenement, "evenement");
        parId.put(evenement.getId(), evenement);
        return this;
    }

    /** @return vrai si une date a été retirée. */
    public boolean retirer(String id) {
        return id != null && parId.remove(id) != null;
    }

    public Optional<EvenementAgenda> trouver(String id) {
        return id == null ? Optional.empty() : Optional.ofNullable(parId.get(id));
    }

    /** @return la date automatique de ce tournoi, s'il y en a une. */
    public Optional<EvenementAgenda> dateDuTournoi(String cleTournoi) {
        if (cleTournoi == null) {
            return Optional.empty();
        }
        for (EvenementAgenda e : parId.values()) {
            if (e.estAutomatique() && cleTournoi.equals(e.getCleTournoi())) {
                return Optional.of(e);
            }
        }
        return Optional.empty();
    }

    /** @return toutes les dates, dans l'ordre chronologique ({@link #ORDRE}). */
    public List<EvenementAgenda> evenements() {
        List<EvenementAgenda> res = new ArrayList<>(parId.values());
        res.sort(ORDRE);
        return res;
    }

    public int taille() {
        return parId.size();
    }

    public boolean estVide() {
        return parId.isEmpty();
    }
}
