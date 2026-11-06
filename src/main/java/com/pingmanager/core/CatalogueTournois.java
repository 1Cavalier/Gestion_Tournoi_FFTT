package com.pingmanager.core;

import com.pingmanager.model.Coordonnees;
import com.pingmanager.model.FicheLicence;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.NiveauTournoi;
import com.pingmanager.model.StatutTournoi;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Catalogue des tournois visibles dans l'onglet Tournois (publiés, à venir ou en
 * cours) et leurs <b>filtres</b>. Chaque tournoi est décrit une fois pour toutes par
 * une {@link Entree} (position, département, accès IX.102, distance, places restantes,
 * tableau à mes points…) : filtrer ne fait ensuite que comparer des champs précalculés,
 * ce qui reste instantané à chaque frappe. La liste et la carte partagent le même
 * résultat filtré.
 */
public final class CatalogueTournois {

    /** Un tournoi du catalogue et ses informations précalculées. */
    public record Entree(String id, Tournoi tournoi, Coordonnees position, boolean positionApprochee,
                         Territoires.Departement departement, EligibiliteTournoi.Acces acces,
                         Double distanceKm, int placesRestantes, int placesTotales,
                         boolean tableauAMesPoints, CalendrierTournoi.Moment moment, String texteRecherche,
                         boolean tableauxAMonNiveauComplets) {

        /** @return le statut affiché sur la carte et dans l'agenda (quatre cas, voir {@link StatutCarteTournoi}). */
        public StatutCarteTournoi.Verdict statut() {
            return StatutCarteTournoi.de(this);
        }
    }

    /**
     * Critères de filtre ; {@code null} ou vide = pas de contrainte. Valeur immuable,
     * modifiée par copie ({@code avec…}).
     */
    public record Criteres(String texte, LocalDate du, LocalDate au, Set<NiveauTournoi> niveaux,
                           String departement, String ligue, Integer distanceMaxKm,
                           boolean aMesPoints, boolean avecPlaces, boolean seulementAccessibles) {

        public static Criteres aucun() {
            return new Criteres(null, null, null, EnumSet.noneOf(NiveauTournoi.class), null, null, null,
                    false, false, false);
        }

        public Criteres avecTexte(String v) {
            return new Criteres(v, du, au, niveaux, departement, ligue, distanceMaxKm, aMesPoints, avecPlaces, seulementAccessibles);
        }

        public Criteres avecPeriode(LocalDate d, LocalDate a) {
            return new Criteres(texte, d, a, niveaux, departement, ligue, distanceMaxKm, aMesPoints, avecPlaces, seulementAccessibles);
        }

        public Criteres avecNiveaux(Set<NiveauTournoi> v) {
            return new Criteres(texte, du, au, v == null || v.isEmpty() ? EnumSet.noneOf(NiveauTournoi.class) : EnumSet.copyOf(v),
                    departement, ligue, distanceMaxKm, aMesPoints, avecPlaces, seulementAccessibles);
        }

        public Criteres avecDepartement(String v) {
            return new Criteres(texte, du, au, niveaux, v, ligue, distanceMaxKm, aMesPoints, avecPlaces, seulementAccessibles);
        }

        public Criteres avecLigue(String v) {
            return new Criteres(texte, du, au, niveaux, departement, v, distanceMaxKm, aMesPoints, avecPlaces, seulementAccessibles);
        }

        public Criteres avecDistanceMax(Integer km) {
            return new Criteres(texte, du, au, niveaux, departement, ligue, km, aMesPoints, avecPlaces, seulementAccessibles);
        }

        public Criteres avecMesPoints(boolean v) {
            return new Criteres(texte, du, au, niveaux, departement, ligue, distanceMaxKm, v, avecPlaces, seulementAccessibles);
        }

        public Criteres avecPlaces(boolean v) {
            return new Criteres(texte, du, au, niveaux, departement, ligue, distanceMaxKm, aMesPoints, v, seulementAccessibles);
        }

        public Criteres avecSeulementAccessibles(boolean v) {
            return new Criteres(texte, du, au, niveaux, departement, ligue, distanceMaxKm, aMesPoints, avecPlaces, v);
        }
    }

    private CatalogueTournois() {
    }

    /**
     * Construit le catalogue : tournois <b>publiés</b> non terminés, chacun décrit
     * pour le licencié donné.
     *
     * @param tournois    tournois candidats, par identifiant (ex. chemin du fichier)
     * @param fiche       licence du joueur connecté ({@code null} si aucune)
     * @param origine     position du club du joueur, pour la distance ({@code null} : pas de distance)
     * @param eligibilite règles d'accès IX.102
     * @param geocodeur   repli de position quand une adresse n'a pas de coordonnées
     * @param aujourdhui  date du jour
     */
    public static List<Entree> construire(Map<String, Tournoi> tournois, FicheLicence fiche, Coordonnees origine,
                                          EligibiliteTournoi eligibilite, Geocodeur geocodeur, LocalDate aujourdhui) {
        List<Entree> res = new ArrayList<>();
        for (Map.Entry<String, Tournoi> e : tournois.entrySet()) {
            Tournoi t = e.getValue();
            if (t.getStatut() != StatutTournoi.PUBLIE) {
                continue;
            }
            CalendrierTournoi.Moment moment = CalendrierTournoi.moment(t, aujourdhui);
            if (moment == CalendrierTournoi.Moment.PASSE) {
                continue;
            }
            res.add(decrire(e.getKey(), t, fiche, origine, eligibilite, geocodeur, moment));
        }
        res.sort(ORDRE);
        return res;
    }

    /** Ordre de la liste : date de début, puis nom. */
    public static final Comparator<Entree> ORDRE = Comparator
            .comparing((Entree x) -> CalendrierTournoi.debut(x.tournoi()) == null ? LocalDate.MAX
                    : CalendrierTournoi.debut(x.tournoi()))
            .thenComparing(x -> x.tournoi().getNom(), String.CASE_INSENSITIVE_ORDER);

    static Entree decrire(String id, Tournoi t, FicheLicence fiche, Coordonnees origine,
                          EligibiliteTournoi eligibilite, Geocodeur geocodeur, CalendrierTournoi.Moment moment) {
        Coordonnees pos = t.getAdresse() == null ? null : t.getAdresse().getCoordonnees();
        boolean approchee = false;
        if (pos == null) {
            pos = geocodeur.geocoder(t.getAdresse()).orElse(null);
            approchee = pos != null;
        }
        Territoires.Departement dep = eligibilite.departementDuTournoi(t).orElse(null);
        Double distance = pos == null || origine == null ? null : origine.distanceKm(pos);
        int restantes = 0;
        int totales = 0;
        boolean illimite = false;
        Joueur j = fiche == null ? null : JoueurDepuisLicence.creer(fiche, t);
        boolean aMesPoints = j == null;
        // Tableaux ouverts au joueur (tous si aucune licence liée) : sont-ils tous complets ?
        int tableauxAMonNiveau = 0;
        int tableauxAMonNiveauComplets = 0;
        for (Tableau tab : t.getTableaux()) {
            int cap = tab.getConfiguration().getPlacesMax();
            if (cap <= 0) {
                illimite = true;
            } else {
                totales += cap;
                restantes += Math.max(0, cap - tab.getInscrits().size());
            }
            if (j != null && tab.getConfiguration().joueurEligible(j)
                    && (j.getCategorie() == null || tab.getConfiguration().categorieAutorisee(j.getCategorie()))) {
                aMesPoints = true;
            }
            boolean ouvertAuJoueur = j == null || (tab.getConfiguration().joueurEligible(j)
                    && (j.getCategorie() == null || tab.getConfiguration().categorieAutorisee(j.getCategorie())));
            if (ouvertAuJoueur) {
                tableauxAMonNiveau++;
                if (cap > 0 && tab.getInscrits().size() >= cap) {
                    tableauxAMonNiveauComplets++;
                }
            }
        }
        String texte = normaliser(t.getNom() + " " + (t.getClubOrganisateur() == null ? "" : t.getClubOrganisateur())
                + " " + (t.getAdresse() == null ? "" : t.getAdresse().getVille() + " " + t.getAdresse().getCodePostal())
                + " " + (dep == null ? "" : dep.nom()));
        return new Entree(id, t, pos, approchee, dep, eligibilite.evaluer(t, fiche), distance,
                illimite ? -1 : restantes, totales, aMesPoints, moment, texte,
                tableauxAMonNiveau > 0 && tableauxAMonNiveauComplets == tableauxAMonNiveau);
    }

    /** @return les entrées qui satisfont tous les critères, dans l'ordre d'entrée. */
    public static List<Entree> filtrer(List<Entree> entrees, Criteres c) {
        Objects.requireNonNull(c, "criteres");
        String[] mots = c.texte() == null || c.texte().isBlank() ? new String[0]
                : normaliser(c.texte()).split("\\s+");
        List<Entree> res = new ArrayList<>();
        for (Entree e : entrees) {
            if (accepte(e, c, mots)) {
                res.add(e);
            }
        }
        return res;
    }

    private static boolean accepte(Entree e, Criteres c, String[] mots) {
        for (String m : mots) {
            if (!e.texteRecherche().contains(m)) {
                return false;
            }
        }
        LocalDate debut = CalendrierTournoi.debut(e.tournoi());
        LocalDate fin = CalendrierTournoi.fin(e.tournoi());
        if (c.du() != null && fin != null && fin.isBefore(c.du())) {
            return false;
        }
        if (c.au() != null && debut != null && debut.isAfter(c.au())) {
            return false;
        }
        if (c.niveaux() != null && !c.niveaux().isEmpty() && !c.niveaux().contains(e.tournoi().getNiveau())) {
            return false;
        }
        if (c.departement() != null && (e.departement() == null || !c.departement().equals(e.departement().code()))) {
            return false;
        }
        if (c.ligue() != null && (e.departement() == null || !c.ligue().equals(e.departement().ligue()))) {
            return false;
        }
        if (c.distanceMaxKm() != null && (e.distanceKm() == null || e.distanceKm() > c.distanceMaxKm())) {
            return false;
        }
        if (c.aMesPoints() && !e.tableauAMesPoints()) {
            return false;
        }
        if (c.avecPlaces() && e.placesRestantes() == 0) {
            return false;
        }
        return !c.seulementAccessibles() || !e.acces().estRefuse();
    }

    static String normaliser(String s) {
        return Normalizer.normalize(s == null ? "" : s, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).replace('-', ' ').replace('\'', ' ');
    }
}
