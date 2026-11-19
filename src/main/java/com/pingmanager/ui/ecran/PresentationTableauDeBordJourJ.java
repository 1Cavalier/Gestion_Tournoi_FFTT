package com.pingmanager.ui.ecran;

import com.pingmanager.core.CalculEtapeTableau;
import com.pingmanager.core.EtapeTableau;
import com.pingmanager.core.TableauDeBordSalle;
import com.pingmanager.model.ModeAnnonce;
import com.pingmanager.model.ReglagesAnnoncesVocales;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;

import java.util.ArrayList;
import java.util.List;

/**
 * Présentation (sans JavaFX) de l'écran du jour J du juge-arbitre : bandeau,
 * trois cartes (« Lancer un match », « Annonces », « Stratégie en place »),
 * liste des tableaux avec leur frise en six étapes, puis la salle.
 *
 * <p>L'étape d'un tableau vient du cœur ({@link CalculEtapeTableau}) ; ici, on
 * ne fait que la mettre en mots et choisir les actions de la ligne. Pas de
 * {@code switch} sur une énumération : des chaînes de {@code if}.</p>
 */
final class PresentationTableauDeBordJourJ {

    static final String TITRE = "Jour J";
    static final String CARTE_LANCER = "Lancer un match";
    static final String CARTE_ANNONCES = "Annonces";
    static final String CARTE_STRATEGIE = "Stratégie en place";
    static final String TITRE_TABLEAUX = "Tableaux";
    static final String MESSAGE_STRATEGIE = "Les prochains matchs proposés suivent ce choix. "
            + "Les matchs déjà sur une table ne changent pas.";

    /** Largeur minimale d'une étape de la frise pour son libellé complet, puis pour son libellé court. */
    static final double LARGEUR_LIBELLE_COMPLET = 118;
    static final double LARGEUR_LIBELLE_COURT = 66;

    private PresentationTableauDeBordJourJ() {
    }

    // ------------------------------------------------------------------
    // Frise d'un tableau
    // ------------------------------------------------------------------

    /** Place d'une étape par rapport à l'étape courante. */
    enum EtatPas {
        PASSE,
        COURANT,
        A_VENIR
    }

    /** Ce que montre la frise selon la place : libellés complets, courts, ou numéros seuls. */
    enum ModeFrise {
        COMPLET,
        COURT,
        NUMEROS
    }

    /** Une étape de la frise. */
    record Pas(int numero, String libelle, String libelleCourt, EtatPas etat) {

        /** Texte à afficher dans ce mode ; vide en numéros seuls. */
        String texte(ModeFrise mode) {
            if (mode == ModeFrise.COMPLET) {
                return libelle;
            }
            if (mode == ModeFrise.COURT) {
                return libelleCourt;
            }
            return "";
        }

        String classe() {
            if (etat == EtatPas.COURANT) {
                return "jourj-frise-pas-courant";
            }
            return etat == EtatPas.PASSE ? "jourj-frise-pas-passe" : "jourj-frise-pas-a-venir";
        }
    }

    /**
     * Les six étapes, l'étape courante mise en avant ; à l'étape 5, le libellé
     * est le tour en cours (« 1/8 de finale »).
     */
    static List<Pas> frise(CalculEtapeTableau.EtapeCourante courante) {
        List<Pas> pas = new ArrayList<>();
        for (EtapeTableau e : EtapeTableau.values()) {
            EtatPas etat = e == courante.etape() ? EtatPas.COURANT
                    : e.estAvant(courante.etape()) ? EtatPas.PASSE : EtatPas.A_VENIR;
            String libelle = e.libelle();
            String court = e.libelleCourt();
            if (e == EtapeTableau.TABLEAU_FINAL && etat == EtatPas.COURANT && !courante.precision().isEmpty()) {
                libelle = courante.precision();
                court = courant(courante.precision());
            }
            pas.add(new Pas(e.numero(), libelle, court, etat));
        }
        return pas;
    }

    /** Libellé court d'un tour : « 1/8 », « Quarts », « Demies », « Finale », « Pte finale ». */
    static String courant(String tour) {
        if (tour.startsWith("1/")) {
            int espace = tour.indexOf(' ');
            return espace < 0 ? tour : tour.substring(0, espace);
        }
        if (tour.startsWith("Quarts")) {
            return "Quarts";
        }
        if (tour.startsWith("Demi")) {
            return "Demies";
        }
        if (tour.startsWith("Petite")) {
            return "Pte finale";
        }
        if (tour.startsWith("Finale")) {
            return "Finale";
        }
        return tour;
    }

    /** Mode de la frise selon sa largeur (six étapes côte à côte). */
    static ModeFrise modeFrise(double largeur) {
        double parEtape = largeur / EtapeTableau.values().length;
        if (parEtape >= LARGEUR_LIBELLE_COMPLET) {
            return ModeFrise.COMPLET;
        }
        return parEtape >= LARGEUR_LIBELLE_COURT ? ModeFrise.COURT : ModeFrise.NUMEROS;
    }

    // ------------------------------------------------------------------
    // Ligne d'un tableau : étape, frise, actions
    // ------------------------------------------------------------------

    /** Une action de la ligne : libellé, vue ouverte, principale (bouton bleu) ou non. */
    record Action(String libelle, PresentationAvancement.Detail vue, boolean principale) {
    }

    /**
     * Ligne d'un tableau.
     *
     * @param avancement texte d'avancement, jauge et parties en jeu (bandeau historique)
     */
    record LigneTableau(Tableau tableau, CalculEtapeTableau.EtapeCourante etape, List<Pas> frise,
                        PresentationAvancement.Ligne avancement, List<Action> actions) {

        /** Pastille d'étape : « Étape 5 · 1/8 de finale ». */
        String libelleEtape() {
            return "Étape " + etape.etape().numero() + " · " + etape.libelle();
        }

        /** Vue ouverte par un clic sur la ligne : celle de l'action principale. */
        PresentationAvancement.Detail vue() {
            return actions.get(0).vue();
        }
    }

    static LigneTableau ligne(Tableau t) {
        CalculEtapeTableau.EtapeCourante e = CalculEtapeTableau.calculer(t);
        return new LigneTableau(t, e, frise(e), PresentationAvancement.ligne(t), actions(t, e));
    }

    /**
     * Actions propres à l'étape courante. La première est l'action principale
     * (et celle d'un clic sur la ligne) ; une fois les poules lancées, « Poules »
     * reste accessible à côté de l'arbre.
     */
    static List<Action> actions(Tableau t, CalculEtapeTableau.EtapeCourante e) {
        EtapeTableau etape = e.etape();
        List<Action> a = new ArrayList<>();
        if (etape == EtapeTableau.AVANT_POINTAGE) {
            a.add(new Action("Ouvrir le pointage", PresentationAvancement.Detail.OUVRIR_POINTAGE, true));
        } else if (etape == EtapeTableau.POINTAGE_OUVERT) {
            a.add(new Action("Pointer et clôturer", PresentationAvancement.Detail.POINTAGE, true));
        } else if (etape == EtapeTableau.POINTAGE_FERME) {
            a.add(new Action("Lancer le tableau", PresentationAvancement.Detail.POINTAGE, true));
        } else if (etape == EtapeTableau.POULES) {
            if (CalculEtapeTableau.TABLEAU_FINAL_A_TIRER.equals(e.precision())) {
                a.add(new Action("Tirer le tableau final", PresentationAvancement.Detail.TABLEAU_FINAL, true));
                a.add(new Action("Poules", PresentationAvancement.Detail.POULES, false));
            } else {
                a.add(new Action("Poules", PresentationAvancement.Detail.POULES, true));
            }
        } else if (etape == EtapeTableau.TABLEAU_FINAL) {
            a.add(new Action("Arbre", PresentationAvancement.Detail.TABLEAU_FINAL, true));
            a.add(new Action("Poules", PresentationAvancement.Detail.POULES, false));
        } else if (t.aPhaseFinale()) {
            a.add(new Action("Arbre", PresentationAvancement.Detail.TABLEAU_FINAL, false));
            a.add(new Action("Poules", PresentationAvancement.Detail.POULES, false));
        } else {
            a.add(new Action("Poules", PresentationAvancement.Detail.POULES, false));
        }
        return a;
    }

    // ------------------------------------------------------------------
    // Cartes
    // ------------------------------------------------------------------

    /** Carte « Lancer un match » : nombre de matchs proposés. */
    static String valeurLancer(TableauDeBordSalle bord) {
        int n = bord.getPrets().size();
        if (!bord.getPoulesPretes().isEmpty()) {
            return PresentationSalle.aLancer(bord, " prêtes", " prête", " matchs prêts", " match prêt");
        }
        return n == 0 ? "Aucun match prêt" : n + (n > 1 ? " matchs prêts" : " match prêt");
    }

    /** Carte « Lancer un match » : tables libres et joueurs en attente. */
    static String detailLancer(TableauDeBordSalle bord) {
        if (bord.getNombreDeTables() == 0) {
            return "Nombre de tables non renseigné.";
        }
        int libres = bord.tablesLibres().size();
        int attente = bord.getJoueursEnAttente().size();
        return libres + (libres > 1 ? " tables libres" : " table libre") + " · " + attente
                + (attente > 1 ? " joueurs en attente" : " joueur en attente");
    }

    /**
     * État des annonces en un mot, d'après l'annonce choisie pour les rencontres :
     * « Voix », « SMS », « Voix et SMS », « Coupées », ou « À chaque match » si le
     * choix est demandé à chaque lancement.
     */
    static String etatAnnonces(ModeAnnonce mode) {
        if (mode == null) {
            return "À chaque match";
        }
        if (mode.avecAppel() && mode.avecSms()) {
            return "Voix et SMS";
        }
        if (mode.avecAppel()) {
            return "Voix";
        }
        return mode.avecSms() ? "SMS" : "Coupées";
    }

    /** Détail sous l'état : comment la voix et les SMS partent réellement. */
    static String detailAnnonces(ModeAnnonce mode, ReglagesAnnoncesVocales voix, boolean voixDisponible,
                                 boolean smsAutomatiques) {
        List<String> parties = new ArrayList<>();
        if (mode == null || mode.avecAppel()) {
            if (!voixDisponible) {
                parties.add("voix indisponible : annonce à l'écran");
            } else {
                parties.add(voix != null && voix.isAnnonceAutomatique() ? "voix automatique" : "voix à la demande");
            }
        }
        if (mode == null || mode.avecSms()) {
            parties.add(smsAutomatiques ? "SMS automatiques" : "SMS non automatiques");
        }
        if (parties.isEmpty()) {
            return "Aucune annonce au lancement d'un match.";
        }
        String texte = String.join(" · ", parties);
        return Character.toUpperCase(texte.charAt(0)) + texte.substring(1) + ".";
    }

    /** Carte « Stratégie en place » : nom de la stratégie qui s'applique. */
    static String valeurStrategie(Tournoi t) {
        return t.getStrategieOrdonnancement().getLibelle();
    }

    /** « Stratégie choisie » ou « Appliquée par défaut ». */
    static String detailStrategie(Tournoi t) {
        return t.strategieOrdonnancementChoisie() ? PresentationSelecteurStrategie.MARQUE_CHOISIE
                : PresentationSelecteurStrategie.MARQUE_PAR_DEFAUT;
    }

    /** Message après un changement de stratégie depuis le jour J. */
    static String messageChangementStrategie(Tournoi t) {
        return "Stratégie « " + valeurStrategie(t) + " » en place. " + MESSAGE_STRATEGIE;
    }
}
