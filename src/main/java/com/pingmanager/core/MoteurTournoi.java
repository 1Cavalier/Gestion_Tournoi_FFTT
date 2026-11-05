package com.pingmanager.core;

import com.pingmanager.model.Joueur;
import com.pingmanager.model.LigneClassement;
import com.pingmanager.model.Poule;
import com.pingmanager.model.Qualifie;
import com.pingmanager.model.TableauElimination;
import com.pingmanager.model.format.FormatTournoi;
import com.pingmanager.model.format.SortNonQualifies;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Orchestre le déroulement d'une épreuve <b>selon un {@link FormatTournoi}</b> :
 * constitution des poules, génération des rencontres, puis (une fois les poules
 * jouées) extraction des qualifiés et construction du tableau à élimination.
 *
 * <p>Ce moteur applique les règles du format fourni sans les coder en dur : il
 * lit la taille de poule, le nombre de qualifiés, le sort des non-qualifiés et
 * le format de phase finale. Ainsi, passer d'un format à un autre (aujourd'hui
 * « poule de 3, 2 qualifiés », demain d'autres) ne change pas ce code, seulement
 * le format passé en paramètre.</p>
 *
 * <p>La règle anti-arrangement (même club au premier tour) est appliquée
 * automatiquement par le générateur de matchs, indépendamment du format.</p>
 */
public final class MoteurTournoi {

    private final GenerateurPoules generateurPoules;
    private final GenerateurMatchsPoule generateurMatchs;
    private final ClassementPouleService classementService;
    private final PlacementQualifiesService placementService;

    public MoteurTournoi() {
        this(new GenerateurPoules(), new GenerateurMatchsPoule(),
                new ClassementPouleService(), new PlacementQualifiesService());
    }

    public MoteurTournoi(GenerateurPoules generateurPoules,
                         GenerateurMatchsPoule generateurMatchs,
                         ClassementPouleService classementService,
                         PlacementQualifiesService placementService) {
        this.generateurPoules = Objects.requireNonNull(generateurPoules);
        this.generateurMatchs = Objects.requireNonNull(generateurMatchs);
        this.classementService = Objects.requireNonNull(classementService);
        this.placementService = Objects.requireNonNull(placementService);
    }

    /**
     * Constitue les poules d'une épreuve selon le format, et y génère les
     * rencontres (avec la règle anti-arrangement appliquée).
     *
     * @param joueurs joueurs inscrits à l'épreuve
     * @param format  format de l'épreuve
     * @return les poules prêtes à être jouées
     */
    public List<Poule> preparerPoules(List<Joueur> joueurs, FormatTournoi format) {
        return preparerPoules(joueurs, format, null);
    }

    /**
     * Constitue les poules avec départage aléatoire reproductible des joueurs à
     * égalité de points (article I.302), puis génère les rencontres.
     *
     * @param joueurs joueurs inscrits à l'épreuve
     * @param format  format de l'épreuve
     * @param graine  graine du tirage de départage ; {@code null} pour l'ordre
     *                stable sans tirage
     * @return les poules prêtes à être jouées
     */
    public List<Poule> preparerPoules(List<Joueur> joueurs, FormatTournoi format, Long graine) {
        Objects.requireNonNull(joueurs, "joueurs");
        Objects.requireNonNull(format, "format");

        List<Poule> poules = generateurPoules.genererPoules(
                joueurs,
                format.taillePouleSouhaitee(),
                format.separerLesClubsEnPoule(),
                graine);

        for (Poule poule : poules) {
            generateurMatchs.genererMatchs(poule, format.qualifiesParPoule());
        }
        return poules;
    }

    /**
     * Extrait les qualifiés des poules jouées, selon le nombre de qualifiés du
     * format. Les joueurs non qualifiés sont traités selon
     * {@link FormatTournoi#sortNonQualifies()} : dans le format actuel, ils sont
     * éliminés (ils n'apparaissent simplement pas dans le résultat).
     *
     * @param poules poules dont tous les matchs sont terminés
     * @param format format de l'épreuve
     * @return les qualifiés pour le tableau, avec leur rang de poule
     */
    public List<Qualifie> extraireQualifies(List<Poule> poules, FormatTournoi format) {
        Objects.requireNonNull(poules, "poules");
        Objects.requireNonNull(format, "format");
        return placementService.extraireQualifies(poules, format.qualifiesParPoule());
    }

    /**
     * Retourne les joueurs non qualifiés (par exemple les 3es en poule de 3),
     * pour information ou pour un futur tableau de consolante. Dans le format
     * actuel, ces joueurs sont éliminés.
     *
     * @param poules poules jouées
     * @param format format de l'épreuve
     * @return les joueurs non qualifiés, avec leur rang de poule
     */
    public List<Qualifie> nonQualifies(List<Poule> poules, FormatTournoi format) {
        Objects.requireNonNull(poules, "poules");
        Objects.requireNonNull(format, "format");
        int seuil = format.qualifiesParPoule();
        List<Qualifie> restants = new ArrayList<>();
        for (Poule poule : poules) {
            List<LigneClassement> classement = classementService.calculer(poule);
            for (int i = seuil; i < classement.size(); i++) {
                restants.add(new Qualifie(classement.get(i).getJoueur(), poule.getNom(), i + 1));
            }
        }
        return restants;
    }

    /**
     * Construit le tableau à élimination directe à partir des poules jouées, en
     * appliquant le format (nombre de qualifiés) et le placement FFTT I.305.
     * Applique également les best-of du format : best-of standard sur tous les
     * matchs, best-of spécifique sur la finale, et ajoute une petite finale si
     * le format la prévoit. Valable uniquement si le format prévoit une phase à
     * élimination directe.
     *
     * @param poules poules jouées
     * @param format format de l'épreuve
     * @param graine graine du tirage au sort (reproductible)
     * @return le tableau à élimination prêt à être joué
     */
    public TableauElimination construireTableau(List<Poule> poules, FormatTournoi format, long graine) {
        Objects.requireNonNull(format, "format");
        return construireTableau(poules, format, graine, format.avecPetiteFinale());
    }

    /**
     * Comme {@link #construireTableau(List, FormatTournoi, long)}, mais la petite
     * finale est décidée par l'appelant (option du tableau,
     * {@code ConfigurationTableau#avecPetiteFinale()}) plutôt que par le format.
     *
     * @param petiteFinale true pour ajouter un match pour la 3e place (si le
     *                     tableau a deux demi-finales)
     */
    public TableauElimination construireTableau(List<Poule> poules, FormatTournoi format, long graine,
                                                boolean petiteFinale) {
        Objects.requireNonNull(poules, "poules");
        Objects.requireNonNull(format, "format");
        TableauElimination tableau =
                placementService.construireTableau(poules, format.qualifiesParPoule(), graine);
        appliquerBestOf(tableau, format);
        if (petiteFinale) {
            ajouterPetiteFinale(tableau, format);
        }
        return tableau;
    }

    /**
     * Applique les best-of du format aux matchs du tableau : le best-of standard
     * partout, puis le best-of de finale sur l'unique match du dernier tour.
     */
    private void appliquerBestOf(TableauElimination tableau, FormatTournoi format) {
        for (com.pingmanager.model.Tour tour : tableau.getTours()) {
            for (com.pingmanager.model.Match m : tour.getMatchs()) {
                m.setNombreManchesGagnantes(format.nombreManchesGagnantes());
            }
        }
        com.pingmanager.model.Tour finale = tableau.getFinale();
        if (finale != null && !finale.getMatchs().isEmpty()) {
            finale.getMatchs().get(0)
                    .setNombreManchesGagnantes(format.nombreManchesGagnantesFinale());
        }
    }

    /**
     * Ajoute une petite finale (match pour la 3e place).
     * Le match oppose les perdants des deux demi-finales ; ses joueurs sont donc
     * « à déterminer » au départ et alimentés par la progression du tableau. Le
     * best-of de petite finale est appliqué.
     *
     * <p>Requiert qu'il existe un tour de demi-finales (au moins 4 joueurs dans
     * le tableau). Pour un tableau plus petit, la petite finale n'a pas de sens
     * et n'est pas ajoutée.</p>
     */
    private void ajouterPetiteFinale(TableauElimination tableau, FormatTournoi format) {
        List<com.pingmanager.model.Tour> tours = tableau.getTours();
        if (tours.size() < 2) {
            return; // pas de demi-finales identifiables
        }
        com.pingmanager.model.Tour demiFinales = tours.get(tours.size() - 2);
        if (demiFinales.getMatchs().size() != 2) {
            return; // structure inattendue : on n'ajoute pas
        }

        com.pingmanager.model.Match demi1 = demiFinales.getMatchs().get(0);
        com.pingmanager.model.Match demi2 = demiFinales.getMatchs().get(1);

        // Match pour la 3e place : joueurs à déterminer (les perdants des demies).
        com.pingmanager.model.Match petiteFinale = new com.pingmanager.model.Match(
                com.pingmanager.model.Joueur.aDeterminer(),
                com.pingmanager.model.Joueur.aDeterminer());
        petiteFinale.setStatut(com.pingmanager.model.StatutMatch.EN_ATTENTE);
        petiteFinale.setNombreManchesGagnantes(format.nombreManchesGagnantesPetiteFinale());
        // Les sources renseignent d'où viennent les joueurs (les demi-finales),
        // mais la petite finale prend les PERDANTS : la progression standard
        // propage les vainqueurs, donc la petite finale sera renseignée
        // spécifiquement (voir ProgressionTableau#propagerPetiteFinale).
        petiteFinale.setSourceA(demi1);
        petiteFinale.setSourceB(demi2);

        com.pingmanager.model.Tour tourPetiteFinale =
                new com.pingmanager.model.Tour(tours.size() + 1, ServicePhaseFinale.NOM_TOUR_PETITE_FINALE);
        tourPetiteFinale.ajouterMatch(petiteFinale);
        tableau.ajouterTour(tourPetiteFinale);
    }

    /** @return true si, dans ce format, les non-qualifiés sont éliminés. */
    public boolean nonQualifiesElimines(FormatTournoi format) {
        return format.sortNonQualifies() == SortNonQualifies.ELIMINE;
    }
}
