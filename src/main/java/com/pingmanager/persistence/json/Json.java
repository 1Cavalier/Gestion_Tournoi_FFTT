package com.pingmanager.persistence.json;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Arbre JSON minimal en mémoire, sans aucune dépendance externe.
 *
 * <p>Représente les six formes du JSON : objet, tableau, chaîne, nombre,
 * booléen, nul. Volontairement simple : la couche {@code persistence} en a
 * besoin pour lire et écrire ses fichiers sans embarquer de bibliothèque
 * (contrainte « zéro dépendance, fonctionne hors ligne »).</p>
 *
 * <p>Un {@code Json} est soit une valeur scalaire, soit un conteneur. Les
 * fabriques statiques créent les nœuds ; les accesseurs typés lisent en
 * signalant clairement une structure inattendue.</p>
 */
public final class Json {

    /** Les types possibles d'un nœud JSON. */
    public enum Type { OBJET, TABLEAU, CHAINE, NOMBRE, BOOLEEN, NUL }

    private final Type type;
    private final Object valeur; // Map, List, String, Double, Boolean, ou null

    private Json(Type type, Object valeur) {
        this.type = type;
        this.valeur = valeur;
    }

    // ------------------------------------------------------------------
    // Fabriques
    // ------------------------------------------------------------------

    public static Json objet() {
        return new Json(Type.OBJET, new LinkedHashMap<String, Json>());
    }

    public static Json tableau() {
        return new Json(Type.TABLEAU, new ArrayList<Json>());
    }

    public static Json of(String s) {
        return s == null ? nul() : new Json(Type.CHAINE, s);
    }

    public static Json of(double d) {
        return new Json(Type.NOMBRE, d);
    }

    public static Json of(long l) {
        return new Json(Type.NOMBRE, (double) l);
    }

    public static Json of(int i) {
        return new Json(Type.NOMBRE, (double) i);
    }

    public static Json of(boolean b) {
        return new Json(Type.BOOLEEN, b);
    }

    public static Json nul() {
        return new Json(Type.NUL, null);
    }

    // ------------------------------------------------------------------
    // Type
    // ------------------------------------------------------------------

    public Type getType() {
        return type;
    }

    public boolean estNul() {
        return type == Type.NUL;
    }

    public boolean estObjet() {
        return type == Type.OBJET;
    }

    public boolean estTableau() {
        return type == Type.TABLEAU;
    }

    // ------------------------------------------------------------------
    // Objet
    // ------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private Map<String, Json> asMap() {
        if (type != Type.OBJET) {
            throw new JsonException("Nœud attendu : objet, mais trouvé : " + type);
        }
        return (Map<String, Json>) valeur;
    }

    /** Ajoute ou remplace une entrée dans un objet. Ignore les valeurs nulles Java. */
    public Json set(String cle, Json v) {
        asMap().put(cle, v == null ? nul() : v);
        return this;
    }

    public Json set(String cle, String v) {
        return set(cle, of(v));
    }

    public Json set(String cle, int v) {
        return set(cle, of(v));
    }

    public Json set(String cle, long v) {
        return set(cle, of(v));
    }

    public Json set(String cle, double v) {
        return set(cle, of(v));
    }

    public Json set(String cle, boolean v) {
        return set(cle, of(v));
    }

    /** @return true si l'objet possède cette clé (avec une valeur non nulle JSON). */
    public boolean has(String cle) {
        Json v = asMap().get(cle);
        return v != null && !v.estNul();
    }

    /** @return le nœud pour cette clé, ou {@link #nul()} si absent. */
    public Json get(String cle) {
        Json v = asMap().get(cle);
        return v == null ? nul() : v;
    }

    public Map<String, Json> entrees() {
        return asMap();
    }

    // ------------------------------------------------------------------
    // Tableau
    // ------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private List<Json> asList() {
        if (type != Type.TABLEAU) {
            throw new JsonException("Nœud attendu : tableau, mais trouvé : " + type);
        }
        return (List<Json>) valeur;
    }

    public Json add(Json v) {
        asList().add(v == null ? nul() : v);
        return this;
    }

    public List<Json> elements() {
        return asList();
    }

    public int taille() {
        return asList().size();
    }

    // ------------------------------------------------------------------
    // Lecture scalaire
    // ------------------------------------------------------------------

    public String asString() {
        if (type == Type.NUL) {
            return null;
        }
        if (type != Type.CHAINE) {
            throw new JsonException("Nœud attendu : chaîne, mais trouvé : " + type);
        }
        return (String) valeur;
    }

    public double asDouble() {
        if (type != Type.NOMBRE) {
            throw new JsonException("Nœud attendu : nombre, mais trouvé : " + type);
        }
        return (Double) valeur;
    }

    public int asInt() {
        return (int) Math.round(asDouble());
    }

    public long asLong() {
        return Math.round(asDouble());
    }

    public boolean asBoolean() {
        if (type != Type.BOOLEEN) {
            throw new JsonException("Nœud attendu : booléen, mais trouvé : " + type);
        }
        return (Boolean) valeur;
    }

    // ------------------------------------------------------------------
    // Accès pratiques avec valeur par défaut (champs optionnels)
    // ------------------------------------------------------------------

    public String getString(String cle, String defaut) {
        return has(cle) ? get(cle).asString() : defaut;
    }

    public int getInt(String cle, int defaut) {
        return has(cle) ? get(cle).asInt() : defaut;
    }

    public double getDouble(String cle, double defaut) {
        return has(cle) ? get(cle).asDouble() : defaut;
    }

    public boolean getBoolean(String cle, boolean defaut) {
        return has(cle) ? get(cle).asBoolean() : defaut;
    }

    @Override
    public String toString() {
        return JsonWriter.ecrire(this, false);
    }
}
