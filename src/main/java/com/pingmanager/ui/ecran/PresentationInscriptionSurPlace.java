package com.pingmanager.ui.ecran;

import com.pingmanager.core.EliminationTableau;
import com.pingmanager.core.JoueurDepuisLicence;
import com.pingmanager.core.reglement.Formats;
import com.pingmanager.model.FicheLicence;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.QuotasTableaux;
import com.pingmanager.model.StatutTableau;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Textes et listes de l'écran « Inscrire sur place » du jour J, sans JavaFX. */
final class PresentationInscriptionSurPlace {

    static final String TITRE = "Inscrire sur place";

    private PresentationInscriptionSurPlace() {
    }

    /** Rappel de la règle du club, en tête de l'écran. */
    static String regle(Tournoi t) {
        QuotasTableaux q = t.getQuotasTableaux();
        String base = "Inscription à un tableau qui n'a pas encore commencé, s'il reste des places : le joueur "
                + "règle à la table, et il est pointé tout de suite si le pointage du tableau est ouvert.";
        if (!q.isInscriptionApresEliminationAutorisee()) {
            return base + " Après une élimination, la limite de tableaux du jour reste la même (clause non "
                    + "choisie dans les règles d'inscription).";
        }
        int n = q.getTableauxApresElimination();
        return base + " Un joueur éliminé d'un tableau peut en prendre " + (n == 1 ? "un" : String.valueOf(n))
                + " de plus le même jour, au-delà de sa limite.";
    }

    /**
     * Joueurs proposés : ceux du tournoi qui correspondent à la recherche, puis les licenciés
     * trouvés dans le référentiel qui ne sont pas encore inscrits.
     */
    static List<Joueur> joueurs(Tournoi t, String recherche, List<FicheLicence> licencies) {
        List<List<Joueur>> listes = new ArrayList<>();
        for (Tableau tab : t.getTableaux()) {
            listes.add(tab.getInscrits());
            listes.add(tab.getFileAttente());
        }
        List<Joueur> res = new ArrayList<>(PresentationPrononciation.joueurs(listes, recherche));
        for (FicheLicence f : licencies) {
            boolean dejaLa = res.stream().anyMatch(j -> j.getNumeroLicence().equals(f.getNumeroLicence()))
                    || participant(t, f.getNumeroLicence()) != null;
            if (!dejaLa) {
                res.add(JoueurDepuisLicence.creer(f, t));
            }
        }
        return res;
    }

    /** Le joueur déjà présent dans le tournoi sous cette licence, ou {@code null}. */
    static Joueur participant(Tournoi t, String licence) {
        for (Tableau tab : t.getTableaux()) {
            for (List<Joueur> l : List.of(tab.getInscrits(), tab.getFileAttente())) {
                for (Joueur j : l) {
                    if (j.getNumeroLicence().equals(licence)) {
                        return j;
                    }
                }
            }
        }
        return null;
    }

    /** « Éliminé de : A, C. Encore en lice : D. », ou « Pas encore inscrit dans ce tournoi. ». */
    static String situation(Tournoi t, Joueur j) {
        List<String> elimine = new ArrayList<>();
        List<String> enLice = new ArrayList<>();
        for (Tableau tab : t.getTableaux()) {
            if (!tab.getInscrits().contains(j)) {
                continue;
            }
            if (EliminationTableau.estElimine(tab, j)) {
                elimine.add(tab.getNom());
            } else {
                enLice.add(tab.getNom());
            }
        }
        if (elimine.isEmpty() && enLice.isEmpty()) {
            return "Pas encore inscrit dans ce tournoi.";
        }
        StringBuilder sb = new StringBuilder();
        if (!elimine.isEmpty()) {
            sb.append("Éliminé de : ").append(String.join(", ", elimine)).append('.');
        }
        if (!enLice.isEmpty()) {
            sb.append(sb.length() > 0 ? " " : "").append(elimine.isEmpty() ? "Inscrit à : " : "Encore inscrit à : ")
                    .append(String.join(", ", enLice)).append('.');
        }
        return sb.toString();
    }

    /** « Samedi 10 avril · 13h30 · pointage ouvert · 12 places libres · 10 € sur place ». */
    static String detail(Tableau tab) {
        List<String> parties = new ArrayList<>();
        if (tab.getHoraires() != null && tab.getHoraires().getDebutMatchs() != null) {
            parties.add(Formats.capitale(Formats.jourLong(tab.getHoraires().getJour()).replaceFirst(" \\d{4}$", "")));
            parties.add(Formats.heure(tab.getHoraires().getDebutMatchs().toLocalTime()));
        }
        parties.add(tab.getStatut() == StatutTableau.POINTAGE
                ? (tab.pointageEstCloture() ? "pointage clôturé" : "pointage ouvert") : "inscriptions");
        int max = tab.getConfiguration().getPlacesMax();
        if (max > 0) {
            int libres = Math.max(0, max - tab.getInscrits().size());
            parties.add(libres == 0 ? "complet" : libres + (libres > 1 ? " places libres" : " place libre"));
        }
        double prix = tab.getConfiguration().getTarif().getMontantSurPlace();
        parties.add(prix > 0 ? Formats.montant(prix) + " sur place" : "gratuit");
        return String.join(" · ", parties);
    }

    /** Tableaux ouverts → motif de refus ({@code null} = possible), dans l'ordre des horaires. */
    static Map<Tableau, String> trier(Map<Tableau, String> verif) {
        List<Tableau> ordre = new ArrayList<>(verif.keySet());
        ordre.sort(java.util.Comparator.comparing((Tableau x) -> x.getHoraires() == null
                || x.getHoraires().getDebutMatchs() == null ? java.time.LocalDateTime.MAX
                : x.getHoraires().getDebutMatchs()).thenComparing(Tableau::getNom));
        Map<Tableau, String> res = new LinkedHashMap<>();
        ordre.forEach(x -> res.put(x, verif.get(x)));
        return res;
    }
}
