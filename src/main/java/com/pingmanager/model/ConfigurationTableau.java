package com.pingmanager.model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Paramètres de configuration d'un {@link Tableau}.
 *
 * <p>Regroupe toutes les options réglables par l'organisateur lors de la création
 * du tableau (axe 1) :</p>
 * <ul>
 *   <li>règles structurelles issues du format : taille des poules, qualifiés par
 *       poule, format de phase finale, best-of ;</li>
 *   <li>limite de places (capacité) ;</li>
 *   <li>tarifs d'inscription (en ligne / sur place) via {@link TarifInscription} ;</li>
 *   <li>grille des récompenses via {@link GrilleRecompenses} ;</li>
 *   <li>restriction d'accès par le classement, selon un {@link ModeLimiteClassement}
 *       (tranche de points, plafond de points, ou toutes séries), avec possibilité
 *       d'exclure les mieux numérotés au classement national ;</li>
 *   <li>restriction d'accès par sexe ({@code null} = mixte) et par catégorie d'âge
 *       (ensemble vide = toutes catégories) ;</li>
 *   <li>petite finale (match pour la 3e place), choisie tableau par tableau.</li>
 * </ul>
 *
 * <p>Objet immuable construit via son {@link Builder}, ce qui évite les
 * constructeurs à rallonge et facilite l'ajout futur de paramètres.</p>
 */
public final class ConfigurationTableau {

    private final int taillePouleSouhaitee;   // 3 ou 4 typiquement
    private final int qualifiesParPoule;       // nb de joueurs sortant de chaque poule
    private final FormatPhaseFinale formatPhaseFinale;
    private final int nombreManchesGagnantes;  // best-of : 3 => premier à 3 manches

    /**
     * Petite finale (match pour la 3e place entre les perdants des demi-finales),
     * réglable par tableau (décision de Quentin du 5 novembre 2026, QUESTIONS.md).
     * Les règlements fédéraux ne l'imposent pas pour les tournois (chapitre IX) :
     * c'est une clause d'organisateur, écrite dans le règlement du tournoi. Sans
     * petite finale, les deux demi-finalistes battus sont 3es ex aequo.
     */
    private final boolean petiteFinale;

    /**
     * Format de tournoi de ce tableau, source des règles de déroulé (taille de
     * poule, qualifiés, sort des non-qualifiés, best-of, petite finale, séparation
     * des clubs). Peut être {@code null} si le tableau a été configuré sans format
     * explicite ; il est alors requis de le fournir au moment du lancement.
     */
    private final com.pingmanager.model.format.FormatTournoi format;

    private final int placesMax;               // 0 = illimité
    private final int tailleMaxListeAttente;    // taille max de la liste d'attente (0 = pas de liste d'attente)

    private final TarifInscription tarif;
    private final GrilleRecompenses recompenses;

    // --- Restriction par classement ---
    private final ModeLimiteClassement modeLimite;
    private final int pointsMin;               // utilisé en mode TRANCHE
    private final int pointsMax;               // utilisé en mode TRANCHE et POINT_MAX
    private final int exclusionRangNational;   // 0 = pas d'exclusion ; sinon exclut les rangs 1..N (POINT_MAX / TOUTES_SERIES)

    // --- Restriction par joueur ---
    private final Sexe sexeRequis;                     // null = mixte / ouvert à tous
    private final Set<CategorieAge> categoriesAutorisees; // vide = toutes catégories admises

    private ConfigurationTableau(Builder b) {
        this.taillePouleSouhaitee = b.taillePouleSouhaitee;
        this.qualifiesParPoule = b.qualifiesParPoule;
        this.formatPhaseFinale = b.formatPhaseFinale;
        this.nombreManchesGagnantes = b.nombreManchesGagnantes;
        this.petiteFinale = b.petiteFinale;
        this.format = b.format;
        this.placesMax = b.placesMax;
        this.tailleMaxListeAttente = b.tailleMaxListeAttente;
        this.tarif = b.tarif;
        this.recompenses = b.recompenses;
        this.modeLimite = b.modeLimite;
        this.pointsMin = b.pointsMin;
        this.pointsMax = b.pointsMax;
        this.exclusionRangNational = b.exclusionRangNational;
        this.sexeRequis = b.sexeRequis;
        this.categoriesAutorisees = b.categoriesAutorisees.isEmpty()
                ? Collections.emptySet()
                : Collections.unmodifiableSet(EnumSet.copyOf(b.categoriesAutorisees));
    }

    public int getTaillePouleSouhaitee() {
        return taillePouleSouhaitee;
    }

    public int getQualifiesParPoule() {
        return qualifiesParPoule;
    }

    public FormatPhaseFinale getFormatPhaseFinale() {
        return formatPhaseFinale;
    }

    public int getNombreManchesGagnantes() {
        return nombreManchesGagnantes;
    }

    /**
     * @return true si le tableau final de ce tableau comporte une petite finale
     *         (match pour la 3e place). Clause d'organisateur, réglée par tableau.
     */
    public boolean avecPetiteFinale() {
        return petiteFinale;
    }

    /** @return le format de tournoi de ce tableau, ou {@code null} si non défini. */
    public com.pingmanager.model.format.FormatTournoi getFormat() {
        return format;
    }

    public int getPlacesMax() {
        return placesMax;
    }

    public boolean aLimiteDePlaces() {
        return placesMax > 0;
    }

    /** @return la taille maximale de la liste d'attente (0 = pas de liste d'attente). */
    public int getTailleMaxListeAttente() {
        return tailleMaxListeAttente;
    }

    public TarifInscription getTarif() {
        return tarif;
    }

    public GrilleRecompenses getRecompenses() {
        return recompenses;
    }

    public ModeLimiteClassement getModeLimite() {
        return modeLimite;
    }

    public int getPointsMin() {
        return pointsMin;
    }

    public int getPointsMax() {
        return pointsMax;
    }

    /** @return le rang national à partir duquel on exclut (0 = aucune exclusion). */
    public int getExclusionRangNational() {
        return exclusionRangNational;
    }

    public boolean aExclusionRangNational() {
        return exclusionRangNational > 0;
    }

    public Sexe getSexeRequis() {
        return sexeRequis;
    }

    /**
     * Liste les éléments de configuration encore manquants pour qu'un tableau
     * soit prêt à la <b>publication</b> (ouverture des inscriptions au public).
     *
     * <p>On ne signale que ce qui n'a pas de valeur exploitable par défaut : le
     * {@link #getFormat() format} de compétition (absent par défaut) et une
     * {@link #getPlacesMax() capacité} strictement positive. Les autres
     * paramètres (best-of, taille de poule, catégories…) ont des valeurs par
     * défaut exploitables. Les horaires, portés par le {@link Tableau} lui-même,
     * sont vérifiés au niveau du tournoi.</p>
     *
     * @param prefixe préfixe identifiant le tableau dans les libellés (ex. son nom)
     * @return les libellés des éléments manquants (jamais {@code null})
     */
    public java.util.List<String> champsManquantsPourPublication(String prefixe) {
        java.util.List<String> manquants = new java.util.ArrayList<>();
        String p = (prefixe == null || prefixe.isBlank()) ? "tableau" : prefixe.trim();
        if (format == null) {
            manquants.add(p + " : format de compétition");
        }
        if (placesMax <= 0) {
            manquants.add(p + " : capacité (places max)");
        }
        return manquants;
    }

    /** @return true si le tableau est complet pour la publication. */
    public boolean estCompletPourPublication() {
        return champsManquantsPourPublication(null).isEmpty();
    }

    public Set<CategorieAge> getCategoriesAutorisees() {
        return categoriesAutorisees;
    }

    public boolean estOuvertATouteCategorie() {
        return categoriesAutorisees.isEmpty();
    }

    /**
     * Vérifie si un joueur satisfait les critères d'accès du tableau : classement
     * (selon le mode), rang national, sexe et catégorie d'âge. Les limites de
     * places sont gérées ailleurs (elles dépendent de l'état des inscriptions, pas
     * de la configuration seule).
     *
     * <p>Note : la catégorie d'âge d'un joueur n'est pas encore portée par
     * {@link Joueur} ; tant que ce n'est pas le cas, la restriction par catégorie
     * est vérifiée par {@link #categorieAutorisee(CategorieAge)} lorsqu'une
     * catégorie est fournie par ailleurs. {@code joueurEligible} n'applique donc
     * que les critères disponibles sur le joueur (points, rang, sexe).</p>
     */
    public boolean joueurEligible(Joueur joueur) {
        Objects.requireNonNull(joueur, "joueur");
        if (!pointsAutorises(joueur.getPoints())) {
            return false;
        }
        if (!rangNationalAutorise(joueur)) {
            return false;
        }
        if (sexeRequis != null && joueur.getSexe() != sexeRequis) {
            return false;
        }
        return true;
    }

    /** @return true si un total de points respecte la limite de classement du tableau. */
    public boolean pointsAutorises(int points) {
        return switch (modeLimite) {
            case TRANCHE -> (pointsMin <= 0 || points >= pointsMin)
                    && (pointsMax <= 0 || points <= pointsMax);
            case POINT_MAX -> pointsMax <= 0 || points <= pointsMax;
            case TOUTES_SERIES -> true;
        };
    }

    /**
     * @return true si le joueur n'est pas exclu par la limite de numérotés. Un
     *         joueur dont le rang national est inconnu n'est jamais exclu (on ne
     *         peut pas présumer qu'il fait partie des mieux numérotés).
     */
    public boolean rangNationalAutorise(Joueur joueur) {
        if (!aExclusionRangNational()) {
            return true;
        }
        if (!joueur.aRangNationalConnu()) {
            return true;
        }
        return joueur.getRangNational() > exclusionRangNational;
    }

    /** @return true si la catégorie d'âge donnée est admise (grille vide = toutes). */
    public boolean categorieAutorisee(CategorieAge categorie) {
        return categoriesAutorisees.isEmpty() || categoriesAutorisees.contains(categorie);
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Crée un builder pré-rempli avec les règles structurelles d'un
     * {@link com.pingmanager.model.format.FormatTournoi} : taille de poule,
     * qualifiés par poule, format de phase finale, best-of et petite finale
     * (valeur du format, modifiable ensuite par tableau). Les paramètres
     * propres à l'organisation (places, tarif, récompenses, plage de classement,
     * sexe, catégories) restent à compléter ensuite.
     *
     * @param format le format de tournoi de référence
     * @return un builder initialisé selon le format
     */
    public static Builder depuisFormat(com.pingmanager.model.format.FormatTournoi format) {
        Objects.requireNonNull(format, "format");
        return new Builder()
                .format(format)
                .taillePouleSouhaitee(format.taillePouleSouhaitee())
                .qualifiesParPoule(format.qualifiesParPoule())
                .formatPhaseFinale(format.formatPhaseFinale())
                .nombreManchesGagnantes(format.nombreManchesGagnantes())
                .petiteFinale(format.avecPetiteFinale());
    }

    /** Builder avec des valeurs par défaut raisonnables. */
    public static final class Builder {
        private com.pingmanager.model.format.FormatTournoi format = null;
        private int taillePouleSouhaitee = 4;
        private int qualifiesParPoule = 2;
        private FormatPhaseFinale formatPhaseFinale = FormatPhaseFinale.ELIMINATION_DIRECTE;
        private int nombreManchesGagnantes = 3; // best-of 5
        private boolean petiteFinale = false;
        private int placesMax = 0;
        private int tailleMaxListeAttente = 4;

        private TarifInscription tarif = TarifInscription.gratuit();
        private GrilleRecompenses recompenses = GrilleRecompenses.vide();

        private ModeLimiteClassement modeLimite = ModeLimiteClassement.TOUTES_SERIES;
        private int pointsMin = 0;
        private int pointsMax = 0;
        private int exclusionRangNational = 0;

        private Sexe sexeRequis = null;
        private final Set<CategorieAge> categoriesAutorisees = EnumSet.noneOf(CategorieAge.class);

        /** Associe un format de tournoi à ce tableau (source des règles de déroulé). */
        public Builder format(com.pingmanager.model.format.FormatTournoi format) {
            this.format = format;
            return this;
        }

        public Builder taillePouleSouhaitee(int v) {
            if (v < 3) {
                throw new IllegalArgumentException("Une poule doit compter au moins 3 joueurs.");
            }
            this.taillePouleSouhaitee = v;
            return this;
        }

        public Builder qualifiesParPoule(int v) {
            if (v < 1) {
                throw new IllegalArgumentException("Il faut au moins 1 qualifié par poule.");
            }
            this.qualifiesParPoule = v;
            return this;
        }

        public Builder formatPhaseFinale(FormatPhaseFinale v) {
            this.formatPhaseFinale = v;
            return this;
        }

        public Builder nombreManchesGagnantes(int v) {
            if (v < 1) {
                throw new IllegalArgumentException("Le best-of doit être d'au moins 1 manche.");
            }
            this.nombreManchesGagnantes = v;
            return this;
        }

        /**
         * Active ou non la petite finale (match pour la 3e place) du tableau
         * final. Sans effet si le tableau final compte moins de quatre joueurs
         * (pas de demi-finales complètes).
         */
        public Builder petiteFinale(boolean v) {
            this.petiteFinale = v;
            return this;
        }

        public Builder placesMax(int v) {
            if (v < 0) {
                throw new IllegalArgumentException("placesMax ne peut pas être négatif.");
            }
            this.placesMax = v;
            return this;
        }

        /**
         * Fixe la taille maximale de la liste d'attente (mécanisme de remplissage,
         * avant le jour J). 0 désactive la liste d'attente : une inscription sur un
         * tableau plein est alors refusée. Par défaut 4.
         */
        public Builder tailleMaxListeAttente(int v) {
            if (v < 0) {
                throw new IllegalArgumentException("La taille de la liste d'attente ne peut pas être négative.");
            }
            this.tailleMaxListeAttente = v;
            return this;
        }

        public Builder tarif(TarifInscription v) {
            this.tarif = Objects.requireNonNull(v, "tarif");
            return this;
        }

        public Builder recompenses(GrilleRecompenses v) {
            this.recompenses = Objects.requireNonNull(v, "recompenses");
            return this;
        }

        // --- Restriction par classement ---

        /**
         * Configure une <b>tranche</b> de points [min, max]. Passe le mode en
         * {@link ModeLimiteClassement#TRANCHE}.
         */
        public Builder tranche(int min, int max) {
            this.modeLimite = ModeLimiteClassement.TRANCHE;
            this.pointsMin = Math.max(0, min);
            this.pointsMax = Math.max(0, max);
            return this;
        }

        /**
         * Configure un <b>plafond</b> de points. Passe le mode en
         * {@link ModeLimiteClassement#POINT_MAX}.
         */
        public Builder pointMax(int max) {
            this.modeLimite = ModeLimiteClassement.POINT_MAX;
            this.pointsMax = Math.max(0, max);
            return this;
        }

        /** Configure le tableau en <b>Toutes Séries</b> (aucune limite de points). */
        public Builder toutesSeries() {
            this.modeLimite = ModeLimiteClassement.TOUTES_SERIES;
            this.pointsMin = 0;
            this.pointsMax = 0;
            return this;
        }

        /**
         * Exclut les joueurs dont le rang national est &lt;= {@code rang}
         * (ex. exclure les 500 premiers Français). N'a de sens qu'en mode
         * {@link ModeLimiteClassement#POINT_MAX} ou
         * {@link ModeLimiteClassement#TOUTES_SERIES} ; validé au {@link #build()}.
         *
         * @param rang rang national d'exclusion, ou 0 pour aucune exclusion
         */
        public Builder exclusionRangNational(int rang) {
            if (rang < 0) {
                throw new IllegalArgumentException("Le rang d'exclusion ne peut pas être négatif : " + rang);
            }
            this.exclusionRangNational = rang;
            return this;
        }

        // --- Rétro-compatibilité : bornes de points « à l'ancienne » ---

        /**
         * Compat : fixe la borne basse de classement et bascule en mode TRANCHE.
         * Conservé pour l'API historique ; préférer {@link #tranche(int, int)}.
         */
        public Builder pointsMin(int v) {
            this.modeLimite = ModeLimiteClassement.TRANCHE;
            this.pointsMin = Math.max(0, v);
            return this;
        }

        /**
         * Compat : fixe la borne haute de classement. Bascule en mode TRANCHE si
         * une borne basse est déjà posée, sinon en POINT_MAX. Conservé pour l'API
         * historique ; préférer {@link #pointMax(int)} ou {@link #tranche(int, int)}.
         */
        public Builder pointsMax(int v) {
            this.pointsMax = Math.max(0, v);
            if (this.pointsMin > 0) {
                this.modeLimite = ModeLimiteClassement.TRANCHE;
            } else if (this.modeLimite == ModeLimiteClassement.TOUTES_SERIES) {
                this.modeLimite = ModeLimiteClassement.POINT_MAX;
            }
            return this;
        }

        // --- Restriction par joueur ---

        public Builder sexeRequis(Sexe v) {
            this.sexeRequis = v;
            return this;
        }

        /** Ajoute une catégorie d'âge autorisée. */
        public Builder autoriserCategorie(CategorieAge categorie) {
            this.categoriesAutorisees.add(Objects.requireNonNull(categorie, "categorie"));
            return this;
        }

        public ConfigurationTableau build() {
            if (modeLimite == ModeLimiteClassement.TRANCHE
                    && pointsMin > 0 && pointsMax > 0 && pointsMin > pointsMax) {
                throw new IllegalArgumentException(
                        "pointsMin (" + pointsMin + ") ne peut pas dépasser pointsMax (" + pointsMax + ").");
            }
            if (exclusionRangNational > 0 && modeLimite == ModeLimiteClassement.TRANCHE) {
                throw new IllegalArgumentException(
                        "L'exclusion par rang national n'a de sens qu'en mode POINT_MAX ou TOUTES_SERIES, "
                                + "pas en mode TRANCHE.");
            }
            return new ConfigurationTableau(this);
        }
    }
}
