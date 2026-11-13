package com.pingmanager.core;

import com.pingmanager.model.Joueur;
import com.pingmanager.model.Poule;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/**
 * Point 21 du README (RGPD) : purge des tournois anciens et effacement d'un joueur à sa
 * demande. Le cœur décide <b>quoi</b> purger ou anonymiser ; la suppression des fichiers
 * et le journal relèvent de l'appelant (persistance). Date du jour par le port
 * {@link Horloge}.
 *
 * <ul>
 *   <li><b>Purge</b> : un tournoi est à purger quand son dernier jour est passé depuis plus
 *       de N années (3 par défaut). {@link #apercuPurge} liste sans rien supprimer ; la
 *       suppression exige une confirmation explicite de l'appelant.</li>
 *   <li><b>Effacement d'un joueur</b> ({@link #anonymiser}) : nom et prénom remplacés,
 *       numéro de portable et textes des SMS effacés, dans tout tournoi, terminé ou non.
 *       Le numéro de licence et les résultats sportifs restent (obligation fédérale :
 *       durée de conservation à confirmer, README section 6). La date de naissance et
 *       l'email du joueur ne sont pas stockés dans un tournoi.</li>
 * </ul>
 */
public final class ServiceDonneesPersonnelles {

    /** Durée de conservation par défaut des tournois terminés, en années. */
    public static final int ANNEES_CONSERVATION_PAR_DEFAUT = 3;

    /** Nom et prénom mis à la place de ceux du joueur effacé. */
    public static final String NOM_ANONYME = "ANONYME";
    public static final String PRENOM_ANONYME = "Joueur";

    private final Horloge horloge;
    private final ZoneId zone;

    public ServiceDonneesPersonnelles(Horloge horloge, ZoneId zone) {
        this.horloge = Objects.requireNonNull(horloge, "horloge");
        this.zone = Objects.requireNonNull(zone, "zone");
    }

    /** Un tournoi à purger, repéré par la clé de l'appelant (son fichier). */
    public record TournoiAPurger<K>(K cle, String nom, LocalDate dernierJour) {
    }

    /** Bilan de l'anonymisation d'un joueur dans un tournoi. */
    public record BilanAnonymisation(int identitesEffacees, int elementsSmsEffaces) {
        public boolean aModifie() {
            return identitesEffacees > 0 || elementsSmsEffaces > 0;
        }
    }

    /**
     * Aperçu : tournois terminés depuis plus de {@code annees} années. Ne supprime rien.
     *
     * @param tournois clé → tournoi (par exemple fichier → tournoi)
     */
    public <K> List<TournoiAPurger<K>> apercuPurge(Map<K, Tournoi> tournois, int annees) {
        if (annees < 1) {
            throw new IllegalArgumentException("La durée de conservation doit être d'au moins un an.");
        }
        LocalDate limite = horloge.maintenant().atZone(zone).toLocalDate().minusYears(annees);
        List<TournoiAPurger<K>> res = new ArrayList<>();
        for (Map.Entry<K, Tournoi> e : tournois.entrySet()) {
            LocalDate dernier = dernierJour(e.getValue());
            if (dernier != null && dernier.isBefore(limite)) {
                res.add(new TournoiAPurger<>(e.getKey(), e.getValue().getNom(), dernier));
            }
        }
        res.sort(java.util.Comparator.comparing(TournoiAPurger::dernierJour));
        return res;
    }

    /** @return vrai si le tournoi est toujours à purger (contrôle juste avant la suppression). */
    public boolean estAPurger(Tournoi tournoi, int annees) {
        return !apercuPurge(Map.of("t", tournoi), annees).isEmpty();
    }

    /**
     * Efface l'identité du joueur ({@code numeroLicence}) dans le tournoi : nom et prénom,
     * numéro de portable, numéro et texte de ses SMS. Licence et résultats sont gardés.
     */
    public BilanAnonymisation anonymiser(Tournoi tournoi, String numeroLicence) {
        Objects.requireNonNull(tournoi, "tournoi");
        if (numeroLicence == null || numeroLicence.isBlank()) {
            return new BilanAnonymisation(0, 0);
        }
        String licence = numeroLicence.trim();
        Set<Joueur> vus = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        int identites = 0;
        for (Tableau t : tournoi.getTableaux()) {
            List<Joueur> candidats = new ArrayList<>(t.getInscrits());
            candidats.addAll(t.getFileAttente());
            candidats.addAll(t.getRetires());
            for (Poule p : t.getPoules()) {
                candidats.addAll(p.getJoueurs());
            }
            for (Joueur j : candidats) {
                if (j != null && licence.equals(j.getNumeroLicence()) && vus.add(j)
                        && !(NOM_ANONYME.equals(j.getNom()) && PRENOM_ANONYME.equals(j.getPrenom()))) {
                    j.setNom(NOM_ANONYME);
                    j.setPrenom(PRENOM_ANONYME);
                    identites++;
                }
            }
        }
        int sms = tournoi.getConvocationsSms().anonymiser(licence);
        return new BilanAnonymisation(identites, sms);
    }

    /** Dernier jour connu du tournoi (jours, ou date de fin), ou {@code null}. */
    public static LocalDate dernierJour(Tournoi t) {
        LocalDate dernier = t.getJours().isEmpty() ? null : t.getJours().last();
        if (t.getDateFin() != null && (dernier == null || t.getDateFin().isAfter(dernier))) {
            dernier = t.getDateFin();
        }
        return dernier;
    }

    /** Utilitaire : clé → tournoi, à partir d'une liste et d'extracteurs. */
    public static <E, K> Map<K, Tournoi> parCle(List<E> elements, Function<E, K> cle, Function<E, Tournoi> tournoi) {
        Map<K, Tournoi> m = new java.util.LinkedHashMap<>();
        for (E e : elements) {
            m.put(cle.apply(e), tournoi.apply(e));
        }
        return m;
    }
}
