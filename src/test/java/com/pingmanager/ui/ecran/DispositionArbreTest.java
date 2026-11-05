package com.pingmanager.ui.ecran;

import com.pingmanager.core.GenerateurTableauElimination;
import com.pingmanager.core.ServicePhaseFinale;
import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Manche;
import com.pingmanager.model.Match;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.StatutMatch;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.TableauElimination;
import com.pingmanager.model.Tour;
import com.pingmanager.model.format.RegistreFormats;
import com.pingmanager.ui.ecran.DispositionArbre.Carte;
import com.pingmanager.ui.ecran.DispositionArbre.Disposition;
import com.pingmanager.ui.ecran.DispositionArbre.EtatCarte;
import com.pingmanager.ui.ecran.DispositionArbre.Parametres;
import com.pingmanager.ui.ecran.DispositionArbre.Point;
import com.pingmanager.ui.ecran.DispositionArbre.Trait;
import com.pingmanager.ui.ecran.PresentationPhaseFinale.TypeCase;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Géométrie de l'arbre des rencontres (cartes, traits, titres), sans JavaFX. */
class DispositionArbreTest {

    /** Dimensions rondes pour vérifier les calculs à la main. */
    private static final Parametres P = new Parametres(200, 60, 20, 40, 30);
    private static final double E = 1e-9;

    private static Joueur j(String licence, int points) {
        return new Joueur(licence, "Nom" + licence, "Prenom" + licence, Sexe.HOMME, "Club" + licence, points);
    }

    /** Tableau final construit à partir de {@code n} inscrits (poules jouées au hasard). */
    private static Tableau phaseFinale(int n, long graine, boolean petiteFinale) {
        Tableau modele = new Tableau("Modèle", ConfigurationTableau.depuisFormat(RegistreFormats.formatParDefaut())
                .petiteFinale(petiteFinale).build());
        for (Joueur joueur : TableauEssaiPhaseFinale.joueursFictifs(n)) {
            modele.inscrire(joueur);
        }
        Tableau essai = TableauEssaiPhaseFinale.creer(modele, graine);
        assertTrue(new ServicePhaseFinale().construire(essai, graine).estConstruit());
        return essai;
    }

    /** Placement explicite par seed (index 0 inutilisé, null = bye), cadre 8. */
    private static TableauElimination depuisPlacement(Integer... seedsOccupes) {
        Joueur[] parSeed = new Joueur[9];
        for (int s : seedsOccupes) {
            parSeed[s] = j("S" + s, 2000 - s);
        }
        return new GenerateurTableauElimination().genererDepuisPlacement(parSeed, seedsOccupes.length);
    }

    private static List<Carte> colonne(Disposition d, int i) {
        return d.cartes().stream().filter(c -> c.colonne() == i).toList();
    }

    @Test
    void tableau_de_huit_une_colonne_par_tour_avec_ses_titres() {
        Disposition d = DispositionArbre.calculer(phaseFinale(12, 3L, false).getPhaseFinale(), P);
        assertEquals(List.of("Quarts de finale", "Demi-finales", "Finale"),
                d.titres().stream().map(DispositionArbre.TitreColonne::libelle).toList());
        assertEquals(4, colonne(d, 0).size());
        assertEquals(2, colonne(d, 1).size());
        assertEquals(1, colonne(d, 2).size());
        // Colonnes espacées de largeur + écart.
        assertEquals(0, d.titres().get(0).x(), E);
        assertEquals(240, d.titres().get(1).x(), E);
        assertEquals(480, d.titres().get(2).x(), E);
        assertEquals(680, d.largeur(), E);
        // Titre (30) + 4 cases de 80.
        assertEquals(350, d.hauteur(), E);
        assertNull(d.petiteFinale());
    }

    @Test
    void premier_tour_cartes_empilees_sous_le_titre() {
        Disposition d = DispositionArbre.calculer(phaseFinale(12, 3L, false).getPhaseFinale(), P);
        List<Carte> quarts = colonne(d, 0);
        // Centre de la case k : 30 + 80 × (k + 0,5) ; haut = centre - 30.
        assertEquals(40, quarts.get(0).y(), E);
        assertEquals(120, quarts.get(1).y(), E);
        assertEquals(280, quarts.get(3).y(), E);
        assertEquals(70, quarts.get(0).centreY(), E);
        assertEquals(200, quarts.get(0).droite(), E);
    }

    @Test
    void chaque_carte_est_centree_entre_les_deux_cartes_qui_l_alimentent() {
        Disposition d = DispositionArbre.calculer(phaseFinale(24, 5L, false).getPhaseFinale(), P);
        for (int i = 1; i < d.titres().size(); i++) {
            List<Carte> parents = colonne(d, i);
            List<Carte> enfants = colonne(d, i - 1);
            for (Carte parent : parents) {
                Carte haut = enfants.stream().filter(c -> c.rang() == 2 * parent.rang()).findFirst().orElseThrow();
                Carte bas = enfants.stream().filter(c -> c.rang() == 2 * parent.rang() + 1).findFirst().orElseThrow();
                assertEquals((haut.centreY() + bas.centreY()) / 2, parent.centreY(), E);
            }
        }
    }

    @Test
    void chaque_carte_est_reliee_au_tour_suivant_par_un_trait_coude() {
        Disposition d = DispositionArbre.calculer(phaseFinale(12, 3L, false).getPhaseFinale(), P);
        // 4 quarts + 2 demies reliés à leur carte suivante.
        assertEquals(6, d.traits().size());
        Trait premier = d.traits().get(0);
        Carte quart = colonne(d, 0).get(0);
        Carte demi = colonne(d, 1).get(0);
        assertEquals(List.of(new Point(200, quart.centreY()), new Point(220, quart.centreY()),
                new Point(220, demi.centreY()), new Point(240, demi.centreY())), premier.points());
        for (Trait t : d.traits()) {
            assertEquals(4, t.points().size());
            assertEquals(t.points().get(1).x(), t.points().get(2).x(), E, "coude vertical");
        }
    }

    @Test
    void trait_parcouru_quand_le_match_est_joue() {
        Tableau tableau = phaseFinale(12, 3L, false);
        TableauElimination elim = tableau.getPhaseFinale();
        Disposition avant = DispositionArbre.calculer(elim, P);
        assertTrue(avant.traits().stream().noneMatch(Trait::parcouru));

        Match quart = elim.getPremierTour().getMatchs().get(0);
        new ServicePhaseFinale().saisirScore(tableau, quart,
                List.of(new Manche(11, 3), new Manche(11, 3), new Manche(11, 3)));
        Disposition apres = DispositionArbre.calculer(elim, P);
        assertEquals(1, apres.traits().stream().filter(Trait::parcouru).count());
        assertEquals(EtatCarte.TERMINE, DispositionArbre.etat(apres.carteDu(quart).contenu()));
    }

    @Test
    void emplacement_vide_sans_carte_ni_trait_et_exempts_visibles() {
        // Cadre 8 : places 4 et 5 vides (deux byes face à face).
        TableauElimination elim = depuisPlacement(1, 2, 3, 6, 7, 8);
        Disposition d = DispositionArbre.calculer(elim, P);
        List<Carte> premiers = colonne(d, 0);
        assertEquals(3, premiers.size(), "pas de carte pour l'emplacement vide");
        assertTrue(premiers.stream().noneMatch(c -> c.rang() == 1));
        Carte exempt = colonne(d, 1).stream().filter(c -> c.rang() == 0).findFirst().orElseThrow();
        assertEquals(TypeCase.EXEMPT, exempt.contenu().type());
        assertEquals(EtatCarte.EXEMPT, DispositionArbre.etat(exempt.contenu()));
        // 3 cartes du 1er tour + 2 en demies reliées à la finale.
        assertEquals(5, d.traits().size());
    }

    @Test
    void joueur_exempte_trait_parcouru_des_le_depart() {
        // Places 8, 4 et 5 vides : le joueur 1 est exempt en quart et en demie.
        Disposition d = DispositionArbre.calculer(depuisPlacement(1, 2, 3, 6, 7), P);
        Carte exemptQuart = colonne(d, 0).get(0);
        assertEquals(TypeCase.EXEMPT, exemptQuart.contenu().type());
        Trait trait = d.traits().stream()
                .filter(t -> t.points().get(0).y() == exemptQuart.centreY() && t.points().get(0).x() == exemptQuart.droite())
                .findFirst().orElseThrow();
        assertTrue(trait.parcouru());
    }

    @Test
    void petite_finale_sous_la_finale_dans_la_derniere_colonne() {
        Disposition d = DispositionArbre.calculer(phaseFinale(12, 3L, true).getPhaseFinale(), P);
        Carte finale = colonne(d, 2).get(0);
        Carte pf = d.petiteFinale();
        assertNotNull(pf);
        assertTrue(pf.petiteFinale());
        assertEquals(finale.x(), pf.x(), E);
        assertTrue(d.yTitrePetiteFinale() > finale.y() + finale.hauteur());
        assertEquals(d.yTitrePetiteFinale() + 30, pf.y(), E);
        assertTrue(d.hauteur() >= pf.y() + pf.hauteur());
        assertFalse(d.cartes().contains(pf), "la petite finale n'est pas une carte de l'arbre principal");
        assertSame(pf, d.carteDu(pf.match()));
        assertEquals(6, d.traits().size(), "pas de trait vers la petite finale");
    }

    @Test
    void tableau_de_soixante_quatre_reste_aligne_et_donne_la_taille_du_defilement() {
        // 96 joueurs : 32 poules de 3, 64 qualifiés, tableau de 64 sans exempt.
        Tableau tableau = phaseFinale(96, 11L, true);
        TableauElimination elim = tableau.getPhaseFinale();
        assertEquals(64, elim.getCadre());
        Disposition d = DispositionArbre.calculer(elim);
        assertEquals(List.of("1/32 de finale", "1/16 de finale", "1/8 de finale", "Quarts de finale",
                        "Demi-finales", "Finale"),
                d.titres().stream().map(DispositionArbre.TitreColonne::libelle).toList());
        assertEquals(63, d.cartes().size());
        assertEquals(62, d.traits().size());
        Parametres s = Parametres.standard();
        assertEquals(s.hauteurTitre() + 32 * (s.hauteurCarte() + s.ecartVertical()), d.hauteur(), E);
        assertEquals(6 * s.largeurCarte() + 5 * s.ecartColonnes(), d.largeur(), E);
        assertEquals(640, DispositionArbre.hauteurVisible(d, 640), E);
        // Aucune carte ne se chevauche dans une colonne.
        for (int i = 0; i < 6; i++) {
            List<Carte> col = colonne(d, i);
            for (int k = 1; k < col.size(); k++) {
                assertTrue(col.get(k).y() >= col.get(k - 1).y() + col.get(k - 1).hauteur());
            }
        }
    }

    @Test
    void petit_tableau_hauteur_visible_sans_espace_inutile() {
        Disposition d = DispositionArbre.calculer(phaseFinale(6, 1L, false).getPhaseFinale(), P);
        assertEquals(d.hauteur() + 16, DispositionArbre.hauteurVisible(d, 640), E);
    }

    @Test
    void etats_des_cartes_et_classes_de_style() {
        Tableau tableau = phaseFinale(12, 3L, false);
        TableauElimination elim = tableau.getPhaseFinale();
        Disposition d = DispositionArbre.calculer(elim, P);
        Match quart = elim.getPremierTour().getMatchs().get(0);
        Match finale = ServicePhaseFinale.matchFinale(elim);
        assertEquals(EtatCarte.A_JOUER, DispositionArbre.etat(d.carteDu(quart).contenu()));
        assertEquals(EtatCarte.EN_ATTENTE, DispositionArbre.etat(d.carteDu(finale).contenu()));
        quart.setStatut(StatutMatch.EN_COURS);
        assertEquals(EtatCarte.EN_COURS, DispositionArbre.etat(d.carteDu(quart).contenu()));

        assertEquals("arbre-a-jouer", DispositionArbre.classeEtat(EtatCarte.A_JOUER));
        assertEquals("arbre-en-cours", DispositionArbre.classeEtat(EtatCarte.EN_COURS));
        assertEquals("arbre-termine", DispositionArbre.classeEtat(EtatCarte.TERMINE));
        assertEquals("arbre-attente", DispositionArbre.classeEtat(EtatCarte.EN_ATTENTE));
        assertEquals("arbre-exempt", DispositionArbre.classeEtat(EtatCarte.EXEMPT));
        for (EtatCarte e : EtatCarte.values()) {
            assertFalse(DispositionArbre.libelleEtat(e).isBlank());
        }
        assertEquals("En cours", DispositionArbre.libelleEtat(EtatCarte.EN_COURS));
    }

    @Test
    void numero_de_table_affiche_quand_le_match_en_a_un() {
        TableauElimination elim = phaseFinale(12, 3L, false).getPhaseFinale();
        Match quart = elim.getPremierTour().getMatchs().get(0);
        assertEquals("", DispositionArbre.libelleTable(quart));
        quart.setNumeroTable(7);
        assertEquals("Table 7", DispositionArbre.libelleTable(quart));
        assertEquals("", DispositionArbre.libelleTable(null));
    }

    @Test
    void tableau_reduit_a_une_finale() {
        Joueur[] parSeed = {null, j("S1", 1500), j("S2", 1400)};
        TableauElimination elim = new GenerateurTableauElimination().genererDepuisPlacement(parSeed, 2);
        Disposition d = DispositionArbre.calculer(elim, P);
        assertEquals(List.of("Finale"), d.titres().stream().map(DispositionArbre.TitreColonne::libelle).toList());
        assertEquals(1, d.cartes().size());
        assertTrue(d.traits().isEmpty());
        assertNull(d.petiteFinale(), "deux qualifiés : pas de petite finale possible");
        assertEquals(P.hauteurTitre() + P.hauteurCarte() + P.ecartVertical(), d.hauteur(), E);
    }

    @Test
    void dimensions_invalides_refusees() {
        assertThrows(IllegalArgumentException.class, () -> new Parametres(0, 60, 10, 10, 10));
        assertThrows(IllegalArgumentException.class, () -> new Parametres(100, 60, -1, 10, 10));
    }

    @Test
    void tours_du_tableau_et_colonnes_concordent() {
        TableauElimination elim = phaseFinale(24, 5L, true).getPhaseFinale();
        Disposition d = DispositionArbre.calculer(elim, P);
        List<Tour> principaux = ServicePhaseFinale.toursPrincipaux(elim);
        assertEquals(principaux.size(), d.titres().size());
        for (Tour t : principaux) {
            for (Match m : t.getMatchs()) {
                assertNotNull(d.carteDu(m), "chaque match a sa carte : " + m);
            }
        }
    }
}
