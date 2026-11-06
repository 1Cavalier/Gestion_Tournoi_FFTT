package com.pingmanager.ordonnancement;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Estime la durée d'un match selon sa {@link CategorieDuree} (phase + niveau).
 *
 * <p>Fonctionne en deux temps, conformément au réglage retenu :</p>
 * <ol>
 *   <li>une <b>valeur de départ</b> est fournie par l'organisateur (globale et/ou
 *       par catégorie) et sert tant qu'on manque d'observations ;</li>
 *   <li>au fil du tournoi, chaque match terminé fournit sa durée réelle, qui
 *       <b>affine</b> l'estimation de sa catégorie.</li>
 * </ol>
 *
 * <p><b>Robustesse.</b> L'affinage utilise une moyenne mobile pondérée (EWMA) :
 * une observation isolée anormale (blessure, litige) ne bouleverse pas
 * l'estimation. De plus, tant qu'une catégorie n'a pas atteint un minimum
 * d'observations, la valeur de départ prime, pour ne pas suivre le bruit
 * initial.</p>
 */
public final class EstimateurDuree {

    /** Poids d'une nouvelle observation dans la moyenne mobile (0..1). */
    private final double alpha;

    /** Nombre minimal d'observations avant de se fier à la valeur apprise. */
    private final int minObservations;

    /** Valeur de départ globale, utilisée si aucune valeur par catégorie. */
    private Duration valeurParDefautGlobale;

    /** Valeurs de départ par catégorie (facultatives). */
    private final Map<CategorieDuree, Duration> valeursDepart = new HashMap<>();

    /** Estimations apprises par catégorie (EWMA des durées observées). */
    private final Map<CategorieDuree, Double> minutesApprises = new HashMap<>();

    /** Nombre d'observations par catégorie. */
    private final Map<CategorieDuree, Integer> nbObservations = new HashMap<>();

    public EstimateurDuree(Duration valeurParDefautGlobale) {
        this(valeurParDefautGlobale, 0.3, 3);
    }

    public EstimateurDuree(Duration valeurParDefautGlobale, double alpha, int minObservations) {
        this.valeurParDefautGlobale = Objects.requireNonNull(valeurParDefautGlobale, "valeurParDefautGlobale");
        if (alpha <= 0 || alpha > 1) {
            throw new IllegalArgumentException("alpha doit être dans ]0, 1].");
        }
        if (minObservations < 0) {
            throw new IllegalArgumentException("minObservations >= 0.");
        }
        this.alpha = alpha;
        this.minObservations = minObservations;
    }

    /** Définit une valeur de départ spécifique à une catégorie. */
    public void definirValeurDepart(CategorieDuree categorie, Duration duree) {
        Objects.requireNonNull(categorie, "categorie");
        Objects.requireNonNull(duree, "duree");
        valeursDepart.put(categorie, duree);
    }

    /**
     * Enregistre la durée réelle d'un match terminé pour affiner l'estimation de
     * sa catégorie (moyenne mobile pondérée).
     *
     * @param categorie catégorie du match
     * @param dureeReelle durée observée (doit être positive)
     */
    public void observer(CategorieDuree categorie, Duration dureeReelle) {
        Objects.requireNonNull(categorie, "categorie");
        Objects.requireNonNull(dureeReelle, "dureeReelle");
        double minutes = dureeReelle.toSeconds() / 60.0;
        if (minutes <= 0) {
            return; // durée aberrante ignorée
        }
        Double courant = minutesApprises.get(categorie);
        double nouveau = (courant == null) ? minutes : (alpha * minutes + (1 - alpha) * courant);
        minutesApprises.put(categorie, nouveau);
        nbObservations.merge(categorie, 1, Integer::sum);
    }

    /**
     * Estime la durée d'un match de la catégorie donnée. Utilise la valeur
     * apprise si la catégorie a assez d'observations, sinon la valeur de départ
     * (par catégorie si définie, sinon globale).
     */
    public Duration estimer(CategorieDuree categorie) {
        Objects.requireNonNull(categorie, "categorie");
        int obs = nbObservations.getOrDefault(categorie, 0);
        if (obs >= minObservations && minutesApprises.containsKey(categorie)) {
            double minutes = minutesApprises.get(categorie);
            return Duration.ofSeconds(Math.round(minutes * 60));
        }
        return valeursDepart.getOrDefault(categorie, valeurParDefautGlobale);
    }

    /** Nombre d'observations accumulées pour une catégorie. */
    public int observationsPour(CategorieDuree categorie) {
        return nbObservations.getOrDefault(categorie, 0);
    }

    /** Indique si l'estimation d'une catégorie provient de l'apprentissage. */
    public boolean estAppris(CategorieDuree categorie) {
        return nbObservations.getOrDefault(categorie, 0) >= minObservations
                && minutesApprises.containsKey(categorie);
    }

    // -------------------------------------------------------------------------
    // Lecture et restauration de l'état (persistance de session)
    // -------------------------------------------------------------------------
    //
    // Ces accesseurs exposent les paramètres et la mémoire d'apprentissage pour
    // qu'une couche externe puisse la sauvegarder puis la restaurer telle quelle
    // (reprise après un arrêt en salle). Ils ne créent aucune dépendance vers la
    // persistance : le cœur reste découplé.

    public double getAlpha() {
        return alpha;
    }

    public int getMinObservations() {
        return minObservations;
    }

    public Duration getValeurParDefautGlobale() {
        return valeurParDefautGlobale;
    }

    /** @return vue en lecture seule des valeurs de départ par catégorie. */
    public Map<CategorieDuree, Duration> getValeursDepart() {
        return java.util.Collections.unmodifiableMap(valeursDepart);
    }

    /** @return vue en lecture seule des durées apprises (minutes, EWMA) par catégorie. */
    public Map<CategorieDuree, Double> getMinutesApprises() {
        return java.util.Collections.unmodifiableMap(minutesApprises);
    }

    /** @return vue en lecture seule du nombre d'observations par catégorie. */
    public Map<CategorieDuree, Integer> getNombreObservations() {
        return java.util.Collections.unmodifiableMap(nbObservations);
    }

    /**
     * Réinstalle une entrée d'apprentissage (durée moyenne apprise et nombre
     * d'observations) pour une catégorie. Réservé au rechargement d'une session :
     * repose directement la mémoire accumulée sans rejouer les observations.
     */
    public void restaurerApprentissage(CategorieDuree categorie, double minutesApprisesValeur,
                                       int observations) {
        Objects.requireNonNull(categorie, "categorie");
        if (observations < 0) {
            throw new IllegalArgumentException("observations >= 0.");
        }
        minutesApprises.put(categorie, minutesApprisesValeur);
        nbObservations.put(categorie, observations);
    }
}
