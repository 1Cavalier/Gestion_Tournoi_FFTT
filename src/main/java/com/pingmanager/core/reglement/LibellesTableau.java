package com.pingmanager.core.reglement;

import com.pingmanager.model.CategorieAge;
import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.GrilleRecompenses;
import com.pingmanager.model.ModeLimiteClassement;
import com.pingmanager.model.Recompense;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.TarifInscription;
import com.pingmanager.model.TypeRecompense;

import java.util.Set;

/** Résumés lisibles d'un tableau, communs à la grille et au règlement. */
public final class LibellesTableau {

    private LibellesTableau() {
    }

    /** « 500 à 899 pts », « ≤ 1299 pts », « Toutes séries (hors n° 1 à 1000) ». */
    public static String points(ConfigurationTableau c) {
        String base;
        if (c.getModeLimite() == ModeLimiteClassement.TRANCHE) {
            base = c.getPointsMin() + " à " + c.getPointsMax() + " pts";
        } else if (c.getModeLimite() == ModeLimiteClassement.POINT_MAX) {
            base = "≤ " + c.getPointsMax() + " pts";
        } else {
            base = "Toutes séries";
        }
        if (c.aExclusionRangNational()) {
            base += " (hors n° 1 à " + c.getExclusionRangNational() + ")";
        }
        return base;
    }

    /** Type du tableau (exigé pour l'homologation), déduit de la catégorie de points. */
    public static String type(ConfigurationTableau c) {
        String p = points(c);
        return p.startsWith("Toutes séries") ? p : "Points " + p;
    }

    /** « Mixte », « Femmes, Vétérans 40 », « Mixte, 5 catégories ». */
    public static String publicVise(ConfigurationTableau c) {
        String sexe = c.getSexeRequis() == Sexe.FEMME ? "Femmes"
                : c.getSexeRequis() == Sexe.HOMME ? "Hommes" : "Mixte";
        Set<CategorieAge> cats = c.getCategoriesAutorisees();
        if (cats.isEmpty()) {
            return sexe;
        }
        if (cats.size() <= 2) {
            StringBuilder sb = new StringBuilder(sexe);
            for (CategorieAge a : cats) {
                sb.append(", ").append(a.getLibelle());
            }
            return sb.toString();
        }
        return sexe + ", " + cats.size() + " catégories";
    }

    /** Catégories en toutes lettres pour le règlement : « Poussins, Benjamins ». */
    public static String categories(ConfigurationTableau c) {
        Set<CategorieAge> cats = c.getCategoriesAutorisees();
        if (cats.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (CategorieAge a : cats) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(a.getLibelle());
        }
        return sb.toString();
    }

    public static String places(ConfigurationTableau c) {
        if (c.getPlacesMax() <= 0) {
            return "—";
        }
        return c.getTailleMaxListeAttente() > 0
                ? c.getPlacesMax() + " + " + c.getTailleMaxListeAttente() + " att."
                : String.valueOf(c.getPlacesMax());
    }

    public static String prix(TarifInscription t) {
        if (t == null) {
            return "—";
        }
        if (t.isEnLigneProposee() && t.isSurPlaceProposee()) {
            if (t.getMontantEnLigne() == 0 && t.getMontantSurPlace() == 0) {
                return "Gratuit";
            }
            return t.getMontantEnLigne() == t.getMontantSurPlace()
                    ? Formats.montant(t.getMontantEnLigne())
                    : Formats.montant(t.getMontantEnLigne()) + " / " + Formats.montant(t.getMontantSurPlace());
        }
        return t.isEnLigneProposee()
                ? Formats.montant(t.getMontantEnLigne()) + " en ligne"
                : Formats.montant(t.getMontantSurPlace()) + " sur place";
    }

    /** Total en argent d'une grille de récompenses (chaque place d'une plage comptée). */
    public static double totalArgent(GrilleRecompenses g) {
        double total = 0;
        if (g != null) {
            for (GrilleRecompenses.Entree e : g.getEntrees()) {
                if (e.getRecompense().getType() == TypeRecompense.ARGENT) {
                    total += e.getRecompense().getMontant() * (e.getPlaceFin() - e.getPlaceDebut() + 1);
                }
            }
        }
        return total;
    }

    /** Part totale des inscriptions distribuée (chaque place d'une plage comptée), en %. */
    public static double partInscriptions(GrilleRecompenses g) {
        double total = 0;
        if (g != null) {
            for (GrilleRecompenses.Entree e : g.getEntrees()) {
                if (e.getRecompense().getType() == TypeRecompense.PART_INSCRIPTIONS) {
                    total += e.getRecompense().getPourcentage() * (e.getPlaceFin() - e.getPlaceDebut() + 1);
                }
            }
        }
        return total;
    }

    static String pourcent(double p) {
        return (p == Math.rint(p) ? String.valueOf((long) p) : String.valueOf(p).replace('.', ',')) + " %";
    }

    /** « 80 € + lots », « Réductions », « 100 % des inscriptions », « — ». */
    public static String recompenses(GrilleRecompenses g) {
        if (g == null || g.estVide()) {
            return "—";
        }
        boolean lots = false;
        boolean reductions = false;
        for (GrilleRecompenses.Entree e : g.getEntrees()) {
            TypeRecompense t = e.getRecompense().getType();
            lots |= t == TypeRecompense.AUTRE;
            reductions |= t == TypeRecompense.REDUCTION_POURCENTAGE;
        }
        double argent = totalArgent(g);
        double part = partInscriptions(g);
        StringBuilder sb = new StringBuilder();
        if (part > 0) {
            sb.append(pourcent(part)).append(" des inscriptions");
        }
        if (argent > 0) {
            sb.append(sb.length() > 0 ? " + " : "").append(Formats.montant(argent));
        }
        if (lots) {
            sb.append(sb.length() > 0 ? " + lots" : "Lots");
        }
        if (reductions) {
            sb.append(sb.length() > 0 ? " + réductions" : "Réductions");
        }
        return sb.toString();
    }

    /** Une récompense en toutes lettres : « 60 € », « Coupe », « Réduction de 20 % ». */
    public static String recompense(Recompense r) {
        if (r.getType() == TypeRecompense.ARGENT) {
            return Formats.montant(r.getMontant());
        }
        if (r.getType() == TypeRecompense.REDUCTION_POURCENTAGE) {
            double p = r.getPourcentage();
            return "Réduction de " + (p == Math.rint(p) ? String.valueOf((long) p) : String.valueOf(p)) + " %";
        }
        if (r.getType() == TypeRecompense.PART_INSCRIPTIONS) {
            return pourcent(r.getPourcentage()) + " des inscr.";
        }
        return r.getLibelle();
    }
}
