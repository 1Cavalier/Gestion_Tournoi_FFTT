package com.pingmanager.ui.ecran;

import com.pingmanager.model.Match;
import com.pingmanager.model.StatutMatch;
import com.pingmanager.model.TableauElimination;
import com.pingmanager.ui.ecran.PresentationPhaseFinale.CaseTableau;
import com.pingmanager.ui.ecran.PresentationPhaseFinale.Colonne;
import com.pingmanager.ui.ecran.PresentationPhaseFinale.TypeCase;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Géométrie de l'<b>arbre des rencontres</b> du tableau final, <b>sans
 * JavaFX</b> : position de chaque carte de match (une colonne par tour, du
 * premier tour à gauche à la finale à droite), traits qui relient chaque carte
 * à la carte du tour suivant, titres des colonnes, place de la petite finale et
 * taille totale (pour le défilement d'un tableau de 64).
 *
 * <p>La vue ({@link VuePhaseFinale}) ne fait que poser les composants aux
 * coordonnées calculées ici ; tout est testé dans {@code DispositionArbreTest}.</p>
 *
 * <p>Principe : la colonne {@code i} (0 = premier tour) découpe la hauteur en
 * cases de pas {@code (hauteurCarte + ecartVertical) × 2^i} ; chaque carte est
 * centrée dans sa case, donc exactement à mi-hauteur des deux cartes qui
 * l'alimentent. Un emplacement vide (deux exempts face à face) n'a ni carte ni
 * trait.</p>
 */
final class DispositionArbre {

    /** Libellé de la carte de petite finale. */
    static final String TITRE_PETITE_FINALE = "Petite finale (3e place)";

    private DispositionArbre() {
    }

    /** Dimensions de l'arbre, en pixels. */
    record Parametres(double largeurCarte, double hauteurCarte, double ecartVertical,
                      double ecartColonnes, double hauteurTitre) {
        Parametres {
            if (largeurCarte <= 0 || hauteurCarte <= 0 || ecartVertical < 0 || ecartColonnes < 0 || hauteurTitre < 0) {
                throw new IllegalArgumentException("Dimensions de l'arbre invalides.");
            }
        }

        /** Réglage de l'écran : cartes lisibles de loin, traits bien visibles. */
        static Parametres standard() {
            return new Parametres(232, 62, 14, 56, 34);
        }
    }

    /** État d'une carte, qui donne sa couleur (pastilles de la charte v2). */
    enum EtatCarte {
        /** Les deux joueurs sont connus, le match n'est pas commencé. */
        A_JOUER,
        /** Match en cours sur une table : mis en évidence. */
        EN_COURS,
        /** Match joué : vainqueur en gras. */
        TERMINE,
        /** Il manque au moins un joueur (match précédent non joué). */
        EN_ATTENTE,
        /** Joueur sans adversaire à ce tour (bye). */
        EXEMPT
    }

    /** Un point d'un trait. */
    record Point(double x, double y) {
    }

    /**
     * Une carte de l'arbre.
     *
     * @param colonne      indice de la colonne (0 = premier tour) ; pour la petite
     *                     finale, celle de la finale
     * @param rang         rang de la carte dans sa colonne, de haut en bas
     * @param contenu      match ou exempt affiché
     * @param petiteFinale la carte est celle de la petite finale
     */
    record Carte(int colonne, int rang, double x, double y, double largeur, double hauteur,
                 CaseTableau contenu, boolean petiteFinale) {

        double centreY() {
            return y + hauteur / 2;
        }

        double droite() {
            return x + largeur;
        }

        /** Le match de la carte (ou, pour un exempt, le match dont il sort), ou {@code null}. */
        Match match() {
            return contenu.match();
        }
    }

    /**
     * Trait reliant une carte à celle du tour suivant : horizontal, coude
     * vertical à mi-chemin, horizontal.
     *
     * @param parcouru le joueur est passé par ce trait (match joué ou exempt connu)
     */
    record Trait(List<Point> points, boolean parcouru) {
        Trait {
            points = List.copyOf(points);
        }
    }

    /** Titre d'une colonne (« Quarts de finale »…), centré au-dessus des cartes. */
    record TitreColonne(String libelle, double x, double largeur) {
    }

    /** Résultat complet : tout ce qu'il faut pour dessiner l'arbre. */
    record Disposition(List<TitreColonne> titres, List<Carte> cartes, List<Trait> traits,
                       Carte petiteFinale, double yTitrePetiteFinale, double largeur, double hauteur) {
        Disposition {
            titres = List.copyOf(titres);
            cartes = List.copyOf(cartes);
            traits = List.copyOf(traits);
        }

        /** @return la carte d'un match (petite finale comprise), ou {@code null}. */
        Carte carteDu(Match m) {
            for (Carte c : cartes) {
                if (c.contenu().type() == TypeCase.MATCH && c.match() == m) {
                    return c;
                }
            }
            return petiteFinale != null && petiteFinale.match() == m ? petiteFinale : null;
        }
    }

    /** Disposition avec les réglages de l'écran. */
    static Disposition calculer(TableauElimination elimination) {
        return calculer(elimination, Parametres.standard());
    }

    /**
     * Calcule la disposition de l'arbre.
     *
     * @param elimination tableau final construit
     * @param p           dimensions
     */
    static Disposition calculer(TableauElimination elimination, Parametres p) {
        Objects.requireNonNull(elimination, "elimination");
        Objects.requireNonNull(p, "p");
        List<Colonne> colonnes = PresentationPhaseFinale.colonnes(elimination);
        List<TitreColonne> titres = new ArrayList<>();
        List<Carte> cartes = new ArrayList<>();
        List<Trait> traits = new ArrayList<>();
        if (colonnes.isEmpty()) {
            return new Disposition(titres, cartes, traits, null, 0, 0, 0);
        }

        double pasPremier = p.hauteurCarte() + p.ecartVertical();
        int casesPremier = colonnes.get(0).cases().size();
        // Cartes posées par colonne et par rang (null pour un emplacement vide),
        // pour relier ensuite chaque carte à ses deux sources.
        List<Carte[]> parColonne = new ArrayList<>();
        for (int i = 0; i < colonnes.size(); i++) {
            Colonne col = colonnes.get(i);
            double x = xColonne(i, p);
            titres.add(new TitreColonne(col.libelle(), x, p.largeurCarte()));
            double pas = pasPremier * (1L << i);
            Carte[] posees = new Carte[col.cases().size()];
            for (int k = 0; k < col.cases().size(); k++) {
                CaseTableau c = col.cases().get(k);
                if (c.type() == TypeCase.VIDE) {
                    continue;
                }
                double centre = p.hauteurTitre() + pas * (k + 0.5);
                Carte carte = new Carte(i, k, x, centre - p.hauteurCarte() / 2, p.largeurCarte(),
                        p.hauteurCarte(), c, false);
                posees[k] = carte;
                cartes.add(carte);
            }
            parColonne.add(posees);
        }

        // Traits : chaque carte d'un tour est reliée à la carte du tour suivant.
        for (int i = 1; i < parColonne.size(); i++) {
            Carte[] parents = parColonne.get(i);
            Carte[] enfants = parColonne.get(i - 1);
            for (int k = 0; k < parents.length; k++) {
                Carte parent = parents[k];
                if (parent == null) {
                    continue;
                }
                for (int e = 2 * k; e <= 2 * k + 1 && e < enfants.length; e++) {
                    Carte enfant = enfants[e];
                    if (enfant != null) {
                        traits.add(relier(enfant, parent, p));
                    }
                }
            }
        }

        double largeur = xColonne(colonnes.size() - 1, p) + p.largeurCarte();
        double hauteur = p.hauteurTitre() + pasPremier * casesPremier;

        // Petite finale : sous la finale, dans la dernière colonne, avec son titre.
        Carte pf = null;
        double yTitre = 0;
        Match matchPf = PresentationPhaseFinale.matchPetiteFinale(elimination);
        if (matchPf != null) {
            Carte finale = parColonne.get(parColonne.size() - 1)[0];
            yTitre = finale.y() + finale.hauteur() + p.hauteurCarte() * 0.75;
            double y = yTitre + p.hauteurTitre();
            pf = new Carte(colonnes.size() - 1, 1, finale.x(), y, p.largeurCarte(), p.hauteurCarte(),
                    CaseTableau.match(matchPf), true);
            hauteur = Math.max(hauteur, y + p.hauteurCarte() + p.ecartVertical() / 2);
        }
        return new Disposition(titres, cartes, traits, pf, yTitre, largeur, hauteur);
    }

    private static double xColonne(int i, Parametres p) {
        return i * (p.largeurCarte() + p.ecartColonnes());
    }

    private static Trait relier(Carte enfant, Carte parent, Parametres p) {
        double xDepart = enfant.droite();
        double xCoude = xDepart + p.ecartColonnes() / 2;
        List<Point> points = List.of(
                new Point(xDepart, enfant.centreY()),
                new Point(xCoude, enfant.centreY()),
                new Point(xCoude, parent.centreY()),
                new Point(parent.x(), parent.centreY()));
        return new Trait(points, estParcouru(enfant.contenu()));
    }

    /** Le joueur issu de cette case est-il connu (match joué, exempt connu) ? */
    static boolean estParcouru(CaseTableau c) {
        if (c.type() == TypeCase.EXEMPT) {
            return c.joueurExempte() != null;
        }
        return c.type() == TypeCase.MATCH && c.match().getStatut() == StatutMatch.TERMINE;
    }

    // ------------------------------------------------------------------
    // États et classes de style des cartes
    // ------------------------------------------------------------------

    static EtatCarte etat(CaseTableau c) {
        if (c.type() == TypeCase.EXEMPT) {
            return EtatCarte.EXEMPT;
        }
        Match m = c.match();
        if (m == null) {
            return EtatCarte.EN_ATTENTE;
        }
        if (m.getStatut() == StatutMatch.TERMINE) {
            return EtatCarte.TERMINE;
        }
        if (m.getStatut() == StatutMatch.EN_COURS) {
            return EtatCarte.EN_COURS;
        }
        return PresentationPhaseFinale.estSaisissable(m) ? EtatCarte.A_JOUER : EtatCarte.EN_ATTENTE;
    }

    /** Classe de style de la carte (définie dans theme.css, section « Jour J : phase finale »). */
    static String classeEtat(EtatCarte e) {
        if (e == EtatCarte.A_JOUER) {
            return "arbre-a-jouer";
        }
        if (e == EtatCarte.EN_COURS) {
            return "arbre-en-cours";
        }
        if (e == EtatCarte.TERMINE) {
            return "arbre-termine";
        }
        if (e == EtatCarte.EXEMPT) {
            return "arbre-exempt";
        }
        return "arbre-attente";
    }

    /** Pastille courte affichée sur la carte : « À jouer », « En cours », « Exempt »… */
    static String libelleEtat(EtatCarte e) {
        if (e == EtatCarte.A_JOUER) {
            return "À jouer";
        }
        if (e == EtatCarte.EN_COURS) {
            return "En cours";
        }
        if (e == EtatCarte.TERMINE) {
            return "Terminé";
        }
        if (e == EtatCarte.EXEMPT) {
            return PresentationPhaseFinale.LIBELLE_EXEMPT;
        }
        return "En attente";
    }

    /** « Table 5 » si le match est affecté à une table, sinon vide. */
    static String libelleTable(Match m) {
        return m == null || m.getNumeroTable() == null ? "" : "Table " + m.getNumeroTable();
    }

    /** Hauteur visible conseillée de la zone de défilement de l'arbre. */
    static double hauteurVisible(Disposition d, double maximum) {
        return Math.min(d.hauteur() + 16, maximum);
    }
}
