package com.pingmanager.ui.ecran;

import com.pingmanager.core.reglement.Formats;
import com.pingmanager.model.AgendaCompte;
import com.pingmanager.model.EvenementAgenda;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Logique d'affichage de l'agenda du compte, sans JavaFX (testée) : une ligne par
 * date, prête à afficher par l'Accueil et « Mes tournois » (tâches suivantes).
 */
final class PresentationAgenda {

    /** Une date de l'agenda, mise en forme. */
    record LigneAgenda(String id, String jour, String heure, String titre, String detail, String etiquette,
                       boolean automatique, boolean passee) {
    }

    private PresentationAgenda() {
    }

    /**
     * Lignes de l'agenda dans l'ordre chronologique. Une date est « passée » quand
     * son dernier jour est avant {@code aujourdhui} ; les dates passées ne sont
     * gardées que si {@code avecPassees}.
     */
    static List<LigneAgenda> lignes(AgendaCompte agenda, LocalDate aujourdhui, boolean avecPassees) {
        Objects.requireNonNull(aujourdhui, "aujourdhui");
        List<LigneAgenda> res = new ArrayList<>();
        if (agenda == null) {
            return res;
        }
        for (EvenementAgenda e : agenda.evenements()) {
            boolean passee = e.getDateFin().isBefore(aujourdhui);
            if (passee && !avecPassees) {
                continue;
            }
            res.add(new LigneAgenda(e.getId(), jour(e), heure(e), e.getTitre(), detail(e),
                    e.estAutomatique() ? "Tournoi" : "Date personnelle", e.estAutomatique(), passee));
        }
        return res;
    }

    /** « samedi 17 avril 2027 », ou « du samedi 10 au dimanche 11 avril 2027 ». */
    static String jour(EvenementAgenda e) {
        if (!e.estSurPlusieursJours()) {
            return Formats.jourLong(e.getDate());
        }
        LocalDate d = e.getDate();
        LocalDate f = e.getDateFin();
        String debut = Formats.jourLong(d);
        if (d.getYear() == f.getYear()) {
            // retire l'année (et le mois s'il est commun) du premier jour
            String suffixe = d.getMonth() == f.getMonth()
                    ? " " + Formats.jourLong(f).replaceFirst("^\\S+ \\S+ ", "")
                    : " " + f.getYear();
            if (debut.endsWith(suffixe)) {
                debut = debut.substring(0, debut.length() - suffixe.length());
            }
        }
        return "du " + debut + " au " + Formats.jourLong(f);
    }

    /** « 18h30 », ou « Toute la journée ». */
    static String heure(EvenementAgenda e) {
        return e.getHeure() == null ? "Toute la journée" : Formats.heure(e.getHeure());
    }

    /** Lieu puis note (première ligne), séparés par « · » ; vide s'il n'y a rien. */
    static String detail(EvenementAgenda e) {
        List<String> morceaux = new ArrayList<>();
        if (e.getLieu() != null) {
            morceaux.add(e.getLieu());
        }
        if (e.getNote() != null) {
            morceaux.add(e.getNote().lines().findFirst().orElse(""));
        }
        return String.join(" · ", morceaux);
    }

    /** Texte d'état vide de l'agenda. */
    static String texteVide() {
        return "Aucune date à venir. Vos tournois s'ajoutent ici dès votre inscription.";
    }
}
