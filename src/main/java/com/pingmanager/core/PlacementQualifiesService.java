package com.pingmanager.core;

import com.pingmanager.model.Joueur;
import com.pingmanager.model.LigneClassement;
import com.pingmanager.model.Poule;
import com.pingmanager.model.Qualifie;
import com.pingmanager.model.TableauElimination;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Fait la liaison entre la phase de poules et le tableau à élimination directe,
 * en plaçant les qualifiés selon les règles FFTT (article I.305).
 *
 * <p><b>Règles appliquées :</b></p>
 * <ol>
 *   <li><b>Extraction des qualifiés.</b> Pour chaque poule classée, on retient
 *       les {@code qualifiesParPoule} premiers (1ers, puis 2es, puis 3es…).</li>
 *   <li><b>1ers de poule = têtes de série.</b> Ils sont classés entre eux selon
 *       leur force (points de classement licence) et placés aux positions de
 *       tête de série du cadre (seed 1, seed 2…), de sorte à se rencontrer le
 *       plus tard possible.</li>
 *   <li><b>2es de poule dans le demi opposé (I.305).</b> Chaque 2e de poule est
 *       placé par tirage au sort dans le <i>demi-tableau opposé</i> à celui où
 *       se trouve le 1er de sa propre poule : un joueur et le vainqueur de sa
 *       poule ne peuvent ainsi se recroiser qu'en finale.</li>
 *   <li><b>3es de poule (si qualifiés).</b> Placés dans le <i>même demi</i> que
 *       leur 2e de poule, mais dans le <i>quart opposé</i>.</li>
 *   <li><b>Séparation des clubs.</b> Le tirage évite, dans la mesure du possible,
 *       de faire s'affronter au premier tour deux joueurs d'une même association.</li>
 * </ol>
 *
 * <p><b>Reproductibilité.</b> Le tirage au sort utilise un {@link Random} ensemencé
 * par une graine fournie, ce qui rend chaque tirage rejouable et traçable. Un
 * placement manuel reste possible en contournant ce service (le générateur de
 * tableau accepte un placement par seed explicite).</p>
 */
public final class PlacementQualifiesService {

    private final ClassementPouleService classementService;
    private final GenerateurTableauElimination generateur;

    public PlacementQualifiesService() {
        this(new ClassementPouleService(), new GenerateurTableauElimination());
    }

    public PlacementQualifiesService(ClassementPouleService classementService,
                                     GenerateurTableauElimination generateur) {
        this.classementService = Objects.requireNonNull(classementService, "classementService");
        this.generateur = Objects.requireNonNull(generateur, "generateur");
    }

    /**
     * Extrait les qualifiés de chaque poule (les {@code qualifiesParPoule}
     * premiers), en calculant d'abord le classement de chaque poule.
     *
     * @param poules            les poules jouées
     * @param qualifiesParPoule nombre de qualifiés retenus par poule (1, 2 ou 3)
     * @return la liste des qualifiés, tous rangs confondus
     */
    public List<Qualifie> extraireQualifies(List<Poule> poules, int qualifiesParPoule) {
        Objects.requireNonNull(poules, "poules");
        if (qualifiesParPoule < 1) {
            throw new IllegalArgumentException("Il faut au moins 1 qualifié par poule.");
        }
        List<Qualifie> qualifies = new ArrayList<>();
        for (Poule poule : poules) {
            List<LigneClassement> classement = classementService.calculer(poule);
            int nb = Math.min(qualifiesParPoule, classement.size());
            for (int i = 0; i < nb; i++) {
                LigneClassement ligne = classement.get(i);
                qualifies.add(new Qualifie(ligne.getJoueur(), poule.getNom(), i + 1));
            }
        }
        return qualifies;
    }

    /**
     * Construit le tableau à élimination à partir des poules classées, en
     * plaçant les qualifiés selon I.305 avec un tirage au sort reproductible.
     *
     * @param poules            les poules jouées
     * @param qualifiesParPoule nombre de qualifiés par poule (1, 2 ou 3)
     * @param graine            graine du tirage au sort (même graine => même tirage)
     * @return le tableau à élimination prêt à être joué
     */
    public TableauElimination construireTableau(List<Poule> poules,
                                                int qualifiesParPoule,
                                                long graine) {
        List<Qualifie> qualifies = extraireQualifies(poules, qualifiesParPoule);
        return placer(qualifies, graine);
    }

    /**
     * Place les qualifiés selon I.305 et retourne le placement par seed, sans
     * construire l'arbre. Utile pour l'affichage du tableau et pour vérifier le
     * tirage. {@code résultat[seed]} est le joueur au seed (1-based), ou null.
     *
     * @param qualifies les qualifiés avec leur rang de poule
     * @param graine    graine du tirage
     * @return tableau indexé de 0 à cadre inclus (index 0 inutilisé)
     */
    public Joueur[] calculerPlacement(List<Qualifie> qualifies, long graine) {
        Objects.requireNonNull(qualifies, "qualifies");
        int n = qualifies.size();
        if (n < 2) {
            throw new IllegalArgumentException(
                    "Il faut au moins 2 qualifiés pour un tableau (reçu : " + n + ").");
        }

        // java.util.Random présente une corrélation entre graines proches (le
        // premier tirage varie peu de graine à graine). On brouille donc la
        // graine via SplittableRandom avant de créer le Random : le tirage reste
        // parfaitement reproductible (même graine → même résultat) mais deux
        // graines voisines produisent des tirages réellement différents.
        long graineBrouillee = new java.util.SplittableRandom(graine).nextLong();
        Random rng = new Random(graineBrouillee);
        int cadre = generateur.puissanceDeDeuxSuperieureOuEgale(n);
        Joueur[] parSeed = new Joueur[cadre + 1];

        // Placement des 1ers selon I.305.2 : « Le vainqueur de la poule 1 en haut
        // du tableau, le vainqueur de la poule 2 en bas » ; « les vainqueurs des
        // poules 3 et 4 dans un groupe 3e-4e », 5 à 8, 9 à 16… Le texte désigne
        // le NUMÉRO de poule, pas la force du vainqueur : les 1ers sont donc pris
        // dans l'ordre des poules (ordre de la liste des qualifiés, poule A = 1),
        // puis tirés au sort à l'intérieur de chaque groupe.
        List<Qualifie> premiers = filtrerParRang(qualifies, 1);

        java.util.Map<String, Integer> seedDuPremierParPoule = new java.util.HashMap<>();
        int nbPremiers = premiers.size();
        int index = 0;
        int debutBloc = 1;
        while (index < nbPremiers) {
            // Un bloc couvre les seeds [debutBloc, finBloc]. Les deux premiers
            // seeds (1 et 2) forment chacun un bloc singleton (positions fixes) ;
            // ensuite les blocs sont 3-4, 5-8, 9-16, …
            int finBloc = (debutBloc == 1 || debutBloc == 2) ? debutBloc : (debutBloc * 2 - 2);

            // Joueurs de ce bloc (dans l'ordre de force), puis mélange pour le tirage.
            List<Qualifie> blocJoueurs = new ArrayList<>();
            for (int s = debutBloc; s <= finBloc && index < nbPremiers; s++, index++) {
                blocJoueurs.add(premiers.get(index));
            }
            // Seeds effectivement disponibles pour ce bloc.
            List<Integer> seedsBloc = new ArrayList<>();
            for (int s = debutBloc; s < debutBloc + blocJoueurs.size(); s++) {
                seedsBloc.add(s);
            }
            // Tirage au sort à l'intérieur du bloc (reproductible via la graine).
            Collections.shuffle(seedsBloc, rng);

            for (int k = 0; k < blocJoueurs.size(); k++) {
                Qualifie premier = blocJoueurs.get(k);
                int seed = seedsBloc.get(k);
                parSeed[seed] = premier.joueur();
                seedDuPremierParPoule.put(premier.nomPoule(), seed);
            }
            debutBloc = finBloc + 1;
        }

        boolean[] seedEnHaut = calculerDemiParSeed(cadre);
        int[] quartParSeed = calculerQuartParSeed(cadre);

        // Placement des 2es : demi opposé à leur 1er. On mémorise le seed attribué
        // à chaque 2e, par poule, pour pouvoir contraindre les 3es ensuite.
        // I.304.1 / I.305.1 : le 2e tour ne compte que si le tableau est
        // réellement incomplet (exempts), pas parce que des places restent à
        // remplir pendant le tirage.
        boolean incomplet = n < cadre;
        java.util.Map<String, Integer> seedDuDeuxiemeParPoule = new java.util.HashMap<>();
        List<Qualifie> deuxiemes = ordreDeTirageDesDeuxiemes(filtrerParRang(qualifies, 2), rng);
        List<Qualifie> troisiemes = new ArrayList<>(filtrerParRang(qualifies, 3));
        Collections.shuffle(troisiemes, rng);

        // I.305.1 : tirage des 2es puis des 3es avec retour arrière, pour
        // garantir qu'aucun joueur ne rencontre au 1er tour un joueur de son
        // association quand une telle répartition existe. À défaut (impossible
        // ou recherche trop longue), tirage glouton historique.
        Joueur[] avantTirage = parSeed.clone();
        List<Qualifie> aTirer = new ArrayList<>(deuxiemes);
        aTirer.addAll(troisiemes);
        int[] budget = {BUDGET_RETOUR_ARRIERE};
        if (!placerAvecRetourArriere(aTirer, 0, parSeed, seedDuPremierParPoule,
                seedDuDeuxiemeParPoule, seedEnHaut, quartParSeed, cadre, incomplet, rng, budget)) {
            System.arraycopy(avantTirage, 0, parSeed, 0, parSeed.length);
            seedDuDeuxiemeParPoule.clear();
            placerDeuxiemes(deuxiemes, parSeed, seedDuPremierParPoule, seedEnHaut,
                    cadre, incomplet, rng, seedDuDeuxiemeParPoule);
            // Placement des 3es : même demi que leur 2e, mais dans le quart
            // opposé à celui de leur 2e (règle I.305.4).
            placerTroisiemes(troisiemes, parSeed, seedDuDeuxiemeParPoule, seedEnHaut,
                    quartParSeed, cadre, incomplet, rng);
        }

        List<Qualifie> autres = new ArrayList<>();
        for (Qualifie q : qualifies) {
            if (q.rangDansPoule() > 3) {
                autres.add(q);
            }
        }
        placerSurPositionsLibres(autres, parSeed, cadre, incomplet, rng);

        return parSeed;
    }

    /** Nombre maximal de places essayées par la recherche avec retour arrière. */
    private static final int BUDGET_RETOUR_ARRIERE = 200_000;

    /**
     * Place récursivement {@code aTirer[k..]} (2es puis 3es) en respectant
     * I.305.3 / I.305.4 (demi opposé au 1er ; 3e dans le demi du 2e, quart
     * opposé) et, strictement, I.305.1 au 1er tour. Si le tableau est incomplet,
     * les places sans conflit au 2e tour sont essayées en premier. Retourne
     * false si aucune répartition n'existe ou si le budget est épuisé.
     */
    private boolean placerAvecRetourArriere(List<Qualifie> aTirer, int k, Joueur[] parSeed,
                                            java.util.Map<String, Integer> seedDuPremierParPoule,
                                            java.util.Map<String, Integer> seedDuDeuxiemeParPoule,
                                            boolean[] seedEnHaut, int[] quartParSeed, int cadre,
                                            boolean incomplet, Random rng, int[] budget) {
        if (k == aTirer.size()) {
            return true;
        }
        if (--budget[0] < 0) {
            return false;
        }
        Qualifie q = aTirer.get(k);
        List<Integer> candidats;
        if (q.rangDansPoule() == 2) {
            Integer seedPremier = seedDuPremierParPoule.get(q.nomPoule());
            boolean premierEnHaut = (seedPremier != null) && seedEnHaut[seedPremier];
            candidats = seedsLibresDansDemi(parSeed, seedEnHaut, cadre, !premierEnHaut);
        } else {
            candidats = placesDuTroisieme(q, parSeed, seedDuDeuxiemeParPoule, seedEnHaut,
                    quartParSeed, cadre);
        }
        Collections.shuffle(candidats, rng);
        // 1er tour : contrainte stricte ; 2e tour (tableau incomplet) : préférence.
        List<Integer> essais = new ArrayList<>();
        List<Integer> essaisConflit2eTour = new ArrayList<>();
        for (int seed : candidats) {
            if (conflitPremierTour(seed, q.joueur(), parSeed, cadre)) {
                continue;
            }
            if (incomplet && conflitDeuxiemeTour(seed, q.joueur(), parSeed, cadre)) {
                essaisConflit2eTour.add(seed);
            } else {
                essais.add(seed);
            }
        }
        essais.addAll(essaisConflit2eTour);
        for (int seed : essais) {
            parSeed[seed] = q.joueur();
            if (q.rangDansPoule() == 2) {
                seedDuDeuxiemeParPoule.put(q.nomPoule(), seed);
            }
            if (placerAvecRetourArriere(aTirer, k + 1, parSeed, seedDuPremierParPoule,
                    seedDuDeuxiemeParPoule, seedEnHaut, quartParSeed, cadre, incomplet, rng, budget)) {
                return true;
            }
            parSeed[seed] = null;
            if (q.rangDansPoule() == 2) {
                seedDuDeuxiemeParPoule.remove(q.nomPoule());
            }
            if (budget[0] < 0) {
                return false;
            }
        }
        return false;
    }

    /**
     * Places possibles d'un 3e (I.305.4) : même demi que son 2e, quart opposé ;
     * repli sur tout le demi si ce quart est plein (comme le tirage glouton),
     * sur toute place libre si son 2e n'est pas placé.
     */
    private List<Integer> placesDuTroisieme(Qualifie q, Joueur[] parSeed,
                                            java.util.Map<String, Integer> seedDuDeuxiemeParPoule,
                                            boolean[] seedEnHaut, int[] quartParSeed, int cadre) {
        Integer seedDeuxieme = seedDuDeuxiemeParPoule.get(q.nomPoule());
        List<Integer> candidats = new ArrayList<>();
        if (seedDeuxieme == null) {
            for (int seed = 1; seed <= cadre; seed++) {
                if (parSeed[seed] == null) {
                    candidats.add(seed);
                }
            }
            return candidats;
        }
        boolean demiCible = seedEnHaut[seedDeuxieme];
        int quartCible = quartOppose(quartParSeed[seedDeuxieme]);
        for (int seed = 1; seed <= cadre; seed++) {
            if (parSeed[seed] == null && seedEnHaut[seed] == demiCible
                    && quartParSeed[seed] == quartCible) {
                candidats.add(seed);
            }
        }
        if (candidats.isEmpty()) {
            candidats = seedsLibresDansDemi(parSeed, seedEnHaut, cadre, demiCible);
        }
        return candidats;
    }

    /**
     * Place une liste de qualifiés (déjà extraits) dans le tableau selon I.305.
     *
     * @param qualifies les qualifiés, avec leur rang de poule
     * @param graine    graine du tirage au sort
     * @return le tableau à élimination
     */
    public TableauElimination placer(List<Qualifie> qualifies, long graine) {
        int n = qualifies.size();
        Joueur[] parSeed = calculerPlacement(qualifies, graine);
        return generateur.genererDepuisPlacement(parSeed, n);
    }

    /** Filtre les qualifiés d'un rang donné (copie modifiable). */
    private List<Qualifie> filtrerParRang(List<Qualifie> qualifies, int rang) {
        List<Qualifie> r = new ArrayList<>();
        for (Qualifie q : qualifies) {
            if (q.rangDansPoule() == rang) {
                r.add(q);
            }
        }
        return r;
    }

    /**
     * Détermine, pour chaque seed du cadre, s'il se trouve dans le demi-tableau
     * du haut (true) ou du bas (false), d'après l'ordre standard des positions.
     */
    private boolean[] calculerDemiParSeed(int cadre) {
        int[] ordre = generateur.ordreDuCadre(cadre);
        boolean[] enHaut = new boolean[cadre + 1];
        for (int pos = 0; pos < cadre; pos++) {
            int seed = ordre[pos];
            enHaut[seed] = (pos < cadre / 2);
        }
        return enHaut;
    }

    /** Retourne le quart (0..3) d'un seed d'après sa position dans l'ordre. */
    private int[] calculerQuartParSeed(int cadre) {
        int[] ordre = generateur.ordreDuCadre(cadre);
        int[] quart = new int[cadre + 1];
        // Tableau de 2 (une seule poule, deux qualifiés) : pas de quarts, chaque
        // place forme son propre « quart » (évite une division par zéro).
        int placesParQuart = Math.max(1, cadre / 4);
        for (int pos = 0; pos < cadre; pos++) {
            quart[ordre[pos]] = pos / placesParQuart;
        }
        return quart;
    }

    /**
     * Place chaque 2e de poule dans le demi-tableau opposé à celui de son 1er,
     * par tirage au sort sur les seeds libres du bon demi, en évitant les
     * conflits de club au premier tour. Mémorise le seed attribué à chaque 2e
     * (par poule) pour permettre le placement contraint des 3es.
     */
    /**
     * Place les 2es de poule par <b>tirage au sort par blocs</b> (méthode du
     * manuel des juges-arbitres FFTT), tout en garantissant la règle I.305 : un
     * 2e va dans le demi-tableau opposé à son 1er.
     *
     * <p><b>Principe du tirage par blocs.</b> Plutôt que de tirer chaque 2e
     * indépendamment sur tout un demi (ce qui peut concentrer les meilleurs 2es
     * au même endroit), on regroupe les 2es par <i>blocs de force</i> : les
     * meilleurs 2es forment le premier bloc, les suivants le second, etc. Chaque
     * bloc est ensuite tiré au sort collectivement sur un ensemble de positions
     * cibles réparties dans le tableau. Cela étale la valeur et introduit une
     * part de hasard équitable, tout en respectant :</p>
     * <ul>
     *   <li>le demi opposé au 1er de chaque poule ;</li>
     *   <li>la séparation des clubs au premier tour.</li>
     * </ul>
     *
     * <p>La taille d'un bloc est le nombre de poules (donc de 2es) divisé par 2,
     * ce qui correspond au regroupement du manuel (rangs 9-12 entre poules 5-8,
     * rangs 13-16 entre poules 1-4, etc.). En deçà de 4 poules, le tirage par
     * blocs se réduit à un tirage simple dans le demi opposé, ce qui reste
     * conforme.</p>
     */
    private void placerDeuxiemes(List<Qualifie> deuxiemesOrdonnes, Joueur[] parSeed,
                                 java.util.Map<String, Integer> seedDuPremierParPoule,
                                 boolean[] seedEnHaut, int cadre, boolean incomplet, Random rng,
                                 java.util.Map<String, Integer> seedDuDeuxiemeParPoule) {
        for (Qualifie q : deuxiemesOrdonnes) {
            Integer seedPremier = seedDuPremierParPoule.get(q.nomPoule());
            boolean premierEnHaut = (seedPremier != null) && seedEnHaut[seedPremier];
            boolean cibleHaut = !premierEnHaut; // demi opposé au 1er

            List<Integer> candidats = seedsLibresDansDemi(parSeed, seedEnHaut, cadre, cibleHaut);
            int seedChoisi = choisirSeedEvitantClub(candidats, q.joueur(), parSeed, cadre, incomplet, rng);
            if (seedChoisi > 0) {
                parSeed[seedChoisi] = q.joueur();
                seedDuDeuxiemeParPoule.put(q.nomPoule(), seedChoisi);
            }
        }
    }

    /**
     * Ordre de tirage des 2es : blocs de force (moitié du nombre de 2es),
     * ordre tiré au sort à l'intérieur de chaque bloc (voir {@link #placerDeuxiemes}).
     */
    private List<Qualifie> ordreDeTirageDesDeuxiemes(List<Qualifie> deuxiemes, Random rng) {
        List<Qualifie> ordre = new ArrayList<>();
        // Trie les 2es par force décroissante pour constituer les blocs.
        List<Qualifie> parForce = new ArrayList<>(deuxiemes);
        parForce.sort((x, y) -> Integer.compare(y.joueur().getPoints(), x.joueur().getPoints()));

        int nbDeuxiemes = parForce.size();
        // Taille de bloc : moitié du nombre de 2es (au moins 1). Reproduit le
        // regroupement du manuel (blocs de la taille d'un demi-ensemble de poules).
        int tailleBloc = Math.max(1, nbDeuxiemes / 2);

        // Découpe en blocs et tire chaque bloc.
        for (int debut = 0; debut < nbDeuxiemes; debut += tailleBloc) {
            int fin = Math.min(debut + tailleBloc, nbDeuxiemes);
            List<Qualifie> bloc = new ArrayList<>(parForce.subList(debut, fin));
            // Ordre de tirage aléatoire à l'intérieur du bloc (reproductible).
            Collections.shuffle(bloc, rng);
            ordre.addAll(bloc);
        }
        return ordre;
    }

    /**
     * Place chaque 3e de poule dans le <b>même demi que son 2e</b> (donc opposé à
     * son 1er) mais dans le <b>quart opposé</b> à celui de son 2e, conformément à
     * I.305. À défaut de place dans le quart cible (tableau incomplet), on
     * élargit au demi entier plutôt que d'échouer.
     */
    private void placerTroisiemes(List<Qualifie> troisiemes, Joueur[] parSeed,
                                  java.util.Map<String, Integer> seedDuDeuxiemeParPoule,
                                  boolean[] seedEnHaut, int[] quartParSeed,
                                  int cadre, boolean incomplet, Random rng) {
        // Ordre de tirage déjà mélangé par l'appelant.
        for (Qualifie q : troisiemes) {
            Integer seedDeuxieme = seedDuDeuxiemeParPoule.get(q.nomPoule());
            if (seedDeuxieme == null) {
                // Pas de 2e placé (cas dégénéré) : repli sur positions libres.
                placerSurPositionsLibres(List.of(q), parSeed, cadre, incomplet, rng);
                continue;
            }
            boolean demiCible = seedEnHaut[seedDeuxieme];         // même demi que le 2e
            int quartDuDeuxieme = quartParSeed[seedDeuxieme];
            int quartOppose = quartOppose(quartDuDeuxieme);       // quart opposé (dans ce demi)

            // Candidats prioritaires : même demi que le 2e ET quart opposé au 2e.
            List<Integer> candidats = new ArrayList<>();
            for (int seed = 1; seed <= cadre; seed++) {
                if (parSeed[seed] == null
                        && seedEnHaut[seed] == demiCible
                        && quartParSeed[seed] == quartOppose) {
                    candidats.add(seed);
                }
            }
            // Repli : tout le demi cible si le quart opposé est plein.
            if (candidats.isEmpty()) {
                candidats = seedsLibresDansDemi(parSeed, seedEnHaut, cadre, demiCible);
            }

            int seedChoisi = choisirSeedEvitantClub(candidats, q.joueur(), parSeed, cadre, incomplet, rng);
            if (seedChoisi > 0) {
                parSeed[seedChoisi] = q.joueur();
            }
        }
    }

    /**
     * Donne le quart « opposé » à l'intérieur d'un même demi-tableau. Les quarts
     * 0 et 1 forment le demi haut ; 2 et 3 le demi bas. L'opposé se fait par
     * paires : 0↔1, 2↔3.
     */
    private int quartOppose(int quart) {
        return switch (quart) {
            case 0 -> 1;
            case 1 -> 0;
            case 2 -> 3;
            case 3 -> 2;
            default -> quart;
        };
    }

    /** Place des qualifiés sur n'importe quelle position libre (dernier recours). */
    private void placerSurPositionsLibres(List<Qualifie> rang, Joueur[] parSeed,
                                          int cadre, boolean incomplet, Random rng) {
        List<Qualifie> aPlacer = new ArrayList<>(rang);
        Collections.shuffle(aPlacer, rng);
        for (Qualifie q : aPlacer) {
            List<Integer> libres = new ArrayList<>();
            for (int seed = 1; seed <= cadre; seed++) {
                if (parSeed[seed] == null) {
                    libres.add(seed);
                }
            }
            int seedChoisi = choisirSeedEvitantClub(libres, q.joueur(), parSeed, cadre, incomplet, rng);
            if (seedChoisi > 0) {
                parSeed[seedChoisi] = q.joueur();
            }
        }
    }

    /** Seeds libres appartenant au demi haut (cibleHaut=true) ou bas. */
    private List<Integer> seedsLibresDansDemi(Joueur[] parSeed, boolean[] seedEnHaut,
                                              int cadre, boolean cibleHaut) {
        List<Integer> libres = new ArrayList<>();
        for (int seed = 1; seed <= cadre; seed++) {
            if (parSeed[seed] == null && seedEnHaut[seed] == cibleHaut) {
                libres.add(seed);
            }
        }
        return libres;
    }

    /**
     * Choisit un seed parmi les candidats en privilégiant ceux dont l'adversaire
     * de premier tour n'est pas du même club que le joueur. Si aucun candidat
     * « sans conflit » n'existe, prend un candidat au hasard (le conflit est
     * alors inévitable). Retourne -1 si la liste est vide.
     */
    private int choisirSeedEvitantClub(List<Integer> candidats, Joueur joueur,
                                       Joueur[] parSeed, int cadre, boolean incomplet, Random rng) {
        if (candidats.isEmpty()) {
            return -1;
        }
        List<Integer> sansConflit = new ArrayList<>();
        for (int seed : candidats) {
            if (!creeConflitClub(seed, joueur, parSeed, cadre, incomplet)) {
                sansConflit.add(seed);
            }
        }
        List<Integer> pool = sansConflit.isEmpty() ? candidats : sansConflit;
        return pool.get(rng.nextInt(pool.size()));
    }

    /**
     * Indique si placer {@code joueur} au {@code seed} créerait un affrontement
     * de premier tour contre un joueur du même club déjà positionné.
     */
    /**
     * Indique si placer {@code joueur} au {@code seed} créerait un affrontement
     * contre un joueur du même club, au premier tour, ou — <b>si le tableau est
     * incomplet (présence d'exemptés)</b> — au deuxième tour (I.304.1 / I.305.1).
     */
    private boolean creeConflitClub(int seed, Joueur joueur, Joueur[] parSeed, int cadre,
                                    boolean incomplet) {
        return conflitPremierTour(seed, joueur, parSeed, cadre)
                || (incomplet && conflitDeuxiemeTour(seed, joueur, parSeed, cadre));
    }

    /** Adversaire direct de 1er tour du même club (I.305.1). */
    private boolean conflitPremierTour(int seed, Joueur joueur, Joueur[] parSeed, int cadre) {
        String club = joueur.getClub();
        if (club == null || club.isBlank()) {
            return false;
        }
        int adversaire = seedAdversairePremierTour(seed, cadre);
        Joueur autre = (adversaire >= 1 && adversaire <= cadre) ? parSeed[adversaire] : null;
        return autre != null && club.equalsIgnoreCase(autre.getClub());
    }

    /**
     * Adversaire possible au 2e tour du même club : les deux autres places du
     * « groupe de 4 ». À n'utiliser que si le tableau est incomplet (I.305.1 :
     * « Si le tableau est incomplet, il faut prendre également en compte le
     * deuxième tour »).
     */
    private boolean conflitDeuxiemeTour(int seed, Joueur joueur, Joueur[] parSeed, int cadre) {
        String club = joueur.getClub();
        if (club == null || club.isBlank()) {
            return false;
        }
        for (int autreSeed : seedsDeuxiemeTour(seed, cadre)) {
            Joueur j2 = (autreSeed >= 1 && autreSeed <= cadre) ? parSeed[autreSeed] : null;
            if (j2 != null && club.equalsIgnoreCase(j2.getClub())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Seeds susceptibles d'affronter {@code seed} au 2e tour : ce sont les deux
     * autres positions du « groupe de 4 » (bloc de 4 positions consécutives dans
     * l'ordre du cadre) auquel appartient le seed, hors son adversaire direct de
     * 1er tour.
     */
    private java.util.List<Integer> seedsDeuxiemeTour(int seed, int cadre) {
        int[] ordre = generateur.ordreDuCadre(cadre);
        int pos = -1;
        for (int i = 0; i < cadre; i++) {
            if (ordre[i] == seed) {
                pos = i;
                break;
            }
        }
        java.util.List<Integer> resultat = new java.util.ArrayList<>();
        if (pos < 0 || cadre < 4) {
            return resultat;
        }
        // Bloc de 4 positions consécutives contenant pos.
        int debutBloc = (pos / 4) * 4;
        int adversaire1erTour = (pos % 2 == 0) ? pos + 1 : pos - 1;
        for (int p = debutBloc; p < debutBloc + 4 && p < cadre; p++) {
            if (p != pos && p != adversaire1erTour) {
                resultat.add(ordre[p]);
            }
        }
        return resultat;
    }

    /**
     * Donne le seed qui affronte {@code seed} au premier tour, d'après l'ordre
     * standard : dans l'ordre linéaire des positions, les paires sont
     * (position 2k, 2k+1). On retrouve donc la position du seed, on prend sa
     * position sœur, et on retraduit en seed.
     */
    private int seedAdversairePremierTour(int seed, int cadre) {
        int[] ordre = generateur.ordreDuCadre(cadre);
        int pos = -1;
        for (int i = 0; i < cadre; i++) {
            if (ordre[i] == seed) {
                pos = i;
                break;
            }
        }
        if (pos < 0) {
            return -1;
        }
        int posSoeur = (pos % 2 == 0) ? pos + 1 : pos - 1;
        return ordre[posSoeur];
    }
}
