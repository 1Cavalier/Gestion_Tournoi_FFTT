package com.pingmanager.persistence;

import com.pingmanager.model.ElementPlan;
import com.pingmanager.model.ElementPlan.AlleePlan;
import com.pingmanager.model.ElementPlan.ReperePlan;
import com.pingmanager.model.ElementPlan.TablePlan;
import com.pingmanager.model.OrientationTable;
import com.pingmanager.model.PlanSalle;
import com.pingmanager.model.SalleTournoi;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.TypeRepere;
import com.pingmanager.persistence.json.Json;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Salles et plans du tournoi en JSON (B024), champ facultatif {@code salles}
 * du tournoi : écrit seulement si le club a préparé un plan, si bien qu'un
 * tournoi sans plan garde exactement son ancien format. Les anciens fichiers,
 * sans ce champ, se relisent avec une seule salle implicite (tables en grille).
 *
 * <pre>
 * "salles": [ { "nom": "Gymnase", "etage": 0, "colonnes": 13, "lignes": 8,
 *               "tableauxReserves": ["Dames"], "tableauxPreferes": [],
 *               "elements": [ {"genre": "table", "numero": 1, "colonne": 1, "ligne": 2,
 *                              "orientation": "VERTICALE", "tableauReserve": "Dames"},
 *                             {"genre": "allee", "colonne": 0, "ligne": 1},
 *                             {"genre": "repere", "type": "WC", "libelle": "WC",
 *                              "colonne": 0, "ligne": 0, "largeur": 1, "hauteur": 1} ] } ]
 * </pre>
 *
 * <p>Relecture tolérante : un type de repère ou une orientation inconnus
 * (fichier plus récent) deviennent « Autre » et « horizontale » ; un élément qui
 * ne tient pas est écarté ; une salle dont le nom ou une table est en double est
 * écartée, pour que le fichier s'ouvre toujours.</p>
 */
final class PlanSalleMapper {

    private PlanSalleMapper() {
    }

    /** Écrit le champ {@code salles} si le tournoi a un plan. */
    static void ecrire(Tournoi t, Json o) {
        if (!t.aUnPlanDeSalle()) {
            return;
        }
        Json salles = Json.tableau();
        for (SalleTournoi s : t.getSalles()) {
            salles.add(ecrireSalle(s));
        }
        o.set("salles", salles);
    }

    /** Relit le champ {@code salles} s'il est présent ; sinon le tournoi reste sans plan. */
    static void lire(Json o, Tournoi t) {
        Json salles = o.get("salles");
        if (salles == null || !salles.estTableau()) {
            return;
        }
        List<SalleTournoi> lues = new ArrayList<>();
        Set<String> noms = new HashSet<>();
        Set<Integer> numeros = new HashSet<>();
        for (Json js : salles.elements()) {
            SalleTournoi s = lireSalle(js);
            if (s == null || !noms.add(s.getNom().toLowerCase(Locale.ROOT))) {
                continue;
            }
            List<Integer> siens = s.getPlan().getTables().stream().map(TablePlan::numero).toList();
            if (siens.stream().anyMatch(numeros::contains)) {
                continue;
            }
            numeros.addAll(siens);
            lues.add(s);
        }
        t.setSalles(lues);
    }

    private static Json ecrireSalle(SalleTournoi s) {
        Json o = Json.objet();
        o.set("nom", s.getNom());
        o.set("etage", s.getEtage());
        o.set("colonnes", s.getPlan().getColonnes());
        o.set("lignes", s.getPlan().getLignes());
        o.set("tableauxReserves", noms(s.getTableauxReserves()));
        o.set("tableauxPreferes", noms(s.getTableauxPreferes()));
        Json elements = Json.tableau();
        for (ElementPlan e : s.getPlan().getElements()) {
            elements.add(ecrireElement(e));
        }
        o.set("elements", elements);
        return o;
    }

    private static Json noms(Set<String> noms) {
        Json a = Json.tableau();
        noms.forEach(n -> a.add(Json.of(n)));
        return a;
    }

    private static Json ecrireElement(ElementPlan e) {
        Json o = Json.objet();
        if (e instanceof TablePlan t) {
            o.set("genre", "table");
            o.set("numero", t.numero());
            o.set("colonne", t.colonne());
            o.set("ligne", t.ligne());
            o.set("orientation", t.orientation().name());
            if (t.tableauReserve() != null) {
                o.set("tableauReserve", t.tableauReserve());
            }
        } else if (e instanceof AlleePlan a) {
            o.set("genre", "allee");
            o.set("colonne", a.colonne());
            o.set("ligne", a.ligne());
        } else if (e instanceof ReperePlan r) {
            o.set("genre", "repere");
            o.set("type", r.type().name());
            o.set("libelle", r.libelle());
            o.set("colonne", r.colonne());
            o.set("ligne", r.ligne());
            o.set("largeur", r.largeur());
            o.set("hauteur", r.hauteur());
        }
        return o;
    }

    private static SalleTournoi lireSalle(Json o) {
        if (o == null || !o.estObjet()) {
            return null;
        }
        String nom = o.getString("nom", null);
        if (nom == null || nom.isBlank()) {
            return null;
        }
        List<ElementPlan> elements = new ArrayList<>();
        Json je = o.get("elements");
        if (je != null && je.estTableau()) {
            for (Json e : je.elements()) {
                ElementPlan el = lireElement(e);
                if (el != null) {
                    elements.add(el);
                }
            }
        }
        PlanSalle plan = PlanSalle.reconstruire(o.getInt("colonnes", 8), o.getInt("lignes", 6), elements);
        return new SalleTournoi(nom, o.getInt("etage", 0), plan, lireNoms(o.get("tableauxReserves")),
                lireNoms(o.get("tableauxPreferes")));
    }

    private static Set<String> lireNoms(Json a) {
        Set<String> noms = new LinkedHashSet<>();
        if (a != null && a.estTableau()) {
            for (Json n : a.elements()) {
                if (n.getType() == Json.Type.CHAINE) {
                    noms.add(n.asString());
                }
            }
        }
        return noms;
    }

    private static ElementPlan lireElement(Json o) {
        if (o == null || !o.estObjet()) {
            return null;
        }
        String genre = o.getString("genre", "");
        int c = o.getInt("colonne", -1);
        int l = o.getInt("ligne", -1);
        if ("table".equals(genre)) {
            return new TablePlan(o.getInt("numero", 0), c, l, orientation(o.getString("orientation", null)),
                    o.getString("tableauReserve", null));
        }
        if ("allee".equals(genre)) {
            return new AlleePlan(c, l);
        }
        if ("repere".equals(genre)) {
            TypeRepere type = typeRepere(o.getString("type", null));
            return new ReperePlan(type, o.getString("libelle", null), c, l,
                    o.getInt("largeur", type.getLargeurParDefaut()), o.getInt("hauteur", type.getHauteurParDefaut()));
        }
        return null; // genre inconnu (fichier plus récent) : écarté
    }

    private static OrientationTable orientation(String nom) {
        for (OrientationTable o : OrientationTable.values()) {
            if (o.name().equals(nom)) {
                return o;
            }
        }
        return OrientationTable.HORIZONTALE;
    }

    private static TypeRepere typeRepere(String nom) {
        for (TypeRepere t : TypeRepere.values()) {
            if (t.name().equals(nom)) {
                return t;
            }
        }
        return TypeRepere.AUTRE;
    }
}
