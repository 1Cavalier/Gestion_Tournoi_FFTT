package com.pingmanager.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Article facultatif ajouté au règlement généré, avec ses paramètres
 * (horaires de la buvette, délai du tirage au sort…).
 *
 * @param type       le modèle d'article
 * @param parametres valeurs saisies (clés propres à chaque type)
 */
public record ArticleOptionnel(Type type, Map<String, String> parametres) {

    /** Modèles d'articles proposés. */
    public enum Type {
        DROIT_IMAGE("Droit à l'image et données personnelles"),
        BUVETTE("Buvette"),
        OUVERTURE_SALLE("Ouverture de la salle"),
        TIRAGE_AU_SORT("Tirage au sort public"),
        SUR_CLASSEMENT("Sur-classement des jeunes"),
        FORFAIT_NON_EXCUSE("Forfait non excusé"),
        INFOS_PRATIQUES("Informations pratiques"),
        LIBRE("Article libre");

        private final String libelle;

        Type(String libelle) {
            this.libelle = libelle;
        }

        public String getLibelle() {
            return libelle;
        }

        /** Tous les modèles, sauf l'article libre, ne s'ajoutent qu'une fois. */
        public boolean unique() {
            return this != LIBRE;
        }
    }

    // Clés de paramètres
    public static final String HORAIRES = "horaires";
    public static final String HEURE = "heure";
    public static final String MINUTES = "minutes";
    public static final String CATEGORIE = "categorie";
    public static final String PARKING = "parking";
    public static final String VESTIAIRES = "vestiaires";
    public static final String PMR = "pmr";
    public static final String TITRE = "titre";
    public static final String TEXTE = "texte";

    public ArticleOptionnel {
        if (type == null) {
            throw new IllegalArgumentException("type obligatoire");
        }
        LinkedHashMap<String, String> m = new LinkedHashMap<>();
        if (parametres != null) {
            parametres.forEach((k, v) -> {
                if (v != null && !v.isBlank()) {
                    m.put(k, v.trim());
                }
            });
        }
        parametres = Collections.unmodifiableMap(m);
    }

    public static ArticleOptionnel de(Type type) {
        return new ArticleOptionnel(type, Map.of());
    }

    /** @return la valeur d'un paramètre, ou {@code null}. */
    public String param(String cle) {
        return parametres.get(cle);
    }

    public boolean coche(String cle) {
        return "oui".equals(parametres.get(cle));
    }
}
