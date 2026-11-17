package com.pingmanager.ui.ecran;

import com.pingmanager.model.buvette.BuvetteTournoi;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Liste des tournois proposés par la page « Buvette » (B026), testée hors
 * JavaFX : les tournois du club (les plus récents d'abord), puis ceux dont
 * le fichier a disparu mais qui ont des ventes. Le tournoi choisi par défaut
 * est celui en cours, sinon le prochain, sinon le plus récent.
 */
final class ChoixTournoiBuvette {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Tournoi du club tel que lu sur le disque. */
    record Candidat(String reference, String nom, LocalDate dateDebut) {
    }

    /** Choix proposé dans la liste. */
    record Option(String reference, String nom, String libelle) {
    }

    private ChoixTournoiBuvette() {
    }

    static List<Option> options(List<Candidat> candidats, Collection<BuvetteTournoi> connus) {
        List<Candidat> tries = new ArrayList<>(candidats);
        tries.sort(Comparator.comparing(Candidat::dateDebut, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(Candidat::nom, Comparator.nullsLast(Comparator.naturalOrder())));
        List<Option> res = new ArrayList<>();
        for (Candidat c : tries) {
            if (res.stream().noneMatch(o -> o.reference().equals(c.reference()))) {
                String nom = c.nom() == null || c.nom().isBlank() ? c.reference() : c.nom();
                res.add(new Option(c.reference(), nom, nom + (c.dateDebut() == null ? "" : " · " + DATE.format(c.dateDebut()))));
            }
        }
        for (BuvetteTournoi t : connus) {
            if (!t.getVentes().isEmpty() && res.stream().noneMatch(o -> o.reference().equals(t.getReferenceTournoi()))) {
                res.add(new Option(t.getReferenceTournoi(), t.getNomTournoi(), t.getNomTournoi() + " · fichier absent"));
            }
        }
        return res;
    }

    /** Référence choisie par défaut : en cours, sinon le prochain à venir, sinon le plus récent ; {@code null} si aucun. */
    static String parDefaut(List<Candidat> candidats, Set<String> enCours, LocalDate aujourdhui) {
        for (Candidat c : candidats) {
            if (enCours.contains(c.reference())) {
                return c.reference();
            }
        }
        Candidat prochain = null;
        Candidat recent = null;
        for (Candidat c : candidats) {
            if (c.dateDebut() == null) {
                continue;
            }
            if (!c.dateDebut().isBefore(aujourdhui)) {
                if (prochain == null || c.dateDebut().isBefore(prochain.dateDebut())) {
                    prochain = c;
                }
            } else if (recent == null || c.dateDebut().isAfter(recent.dateDebut())) {
                recent = c;
            }
        }
        if (prochain != null) {
            return prochain.reference();
        }
        if (recent != null) {
            return recent.reference();
        }
        return candidats.isEmpty() ? null : candidats.get(0).reference();
    }
}
