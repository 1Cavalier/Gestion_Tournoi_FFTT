package com.pingmanager.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Racine du modèle : un tournoi organisé par un club.
 *
 * <p>Regroupe les informations d'homologation FFTT (nom, club organisateur,
 * date, numéro d'homologation) et la liste des {@link Tableau} qui le
 * composent.</p>
 */
public final class Tournoi {

    private String nom;
    private String clubOrganisateur;

    /**
     * Jours du tournoi (un ou plusieurs, ex. un week-end). Ensemble trié et non
     * vide. La méthode de compatibilité {@link #getDate()} renvoie le premier
     * jour.
     */
    private final java.util.TreeSet<LocalDate> jours = new java.util.TreeSet<>();

    /** Numéro d'homologation délivré par la FFTT (peut être vide au début). */
    private String numeroHomologation;

    /** Nombre de tables physiques disponibles dans la salle. */
    private int nombreDeTables;
    private int tablesEchauffement;

    /**
     * Niveau (catégorie) du tournoi : départemental à international. Optionnel
     * pour l'instant ({@code null} = non renseigné) ; conditionnera le grade de
     * juge-arbitre requis.
     */
    private NiveauTournoi niveau;

    /**
     * Règle limitant le nombre de tableaux par jour et par joueur, avec
     * éventuelle exception féminine. Par défaut, aucune limite.
     */
    private RegleTableauxParJour regleTableauxParJour = RegleTableauxParJour.illimitee();

    private final List<Tableau> tableaux = new ArrayList<>();

    /** Juges-arbitres affectés au tournoi (un JA est un joueur licencié + grade). */
    private final List<AffectationJugeArbitre> jugesArbitres = new ArrayList<>();
    private final List<AffectationArbitre> arbitres = new ArrayList<>();
    private final List<LotTables> lotsTables = new ArrayList<>();

    /**
     * Statut du tournoi dans son cycle de vie (voir {@link StatutTournoi}).
     * Un tournoi naît en {@link StatutTournoi#BROUILLON}.
     */
    private StatutTournoi statut = StatutTournoi.BROUILLON;

    /**
     * Métadonnées du règlement (IX.111). Optionnel tant que le tournoi est en
     * brouillon ; devient obligatoire (et complet) pour l'homologation.
     */
    private InfosReglement infosReglement;

    // --- Champs de la demande d'homologation (formulaire SPID) ---

    /**
     * Ancien « type de tournoi ». Remplacé par la catégorie ({@link #getNiveau()},
     * IX.102) : conservé uniquement pour relire les fichiers existants.
     */
    private TypeTournoi typeTournoi;

    /** Date de début (obligatoire). Saisie, indépendante des jours des tableaux. */
    private LocalDate dateDebut;

    /** Date de fin (obligatoire). */
    private LocalDate dateFin;

    /** Adresse structurée du lieu (voie/CP/ville obligatoires pour l'homologation). */
    private AdresseTournoi adresse;

    // --- Correspondant (contact du dossier) ---
    private String correspondantNom;
    private String correspondantPrenom;
    private String correspondantEmail;
    private String correspondantTelephone;   // facultatif
    private String correspondantAutresInfos; // facultatif

    /** Site web du tournoi (facultatif). */
    private String siteWeb;

    /** Numéro d'affiliation FFTT du club organisateur (demande d'homologation). */
    private String numeroAffiliationClub;

    /**
     * Échelon enregistré dans d'anciens fichiers. Désormais calculé à partir de la
     * catégorie et de la dotation : voir {@link #getEchelonHomologation()}.
     */
    private EchelonHomologation echelonHomologation;

    /**
     * Conditions de participation annoncées au règlement. Par défaut ouvertes
     * (licence promotionnelle acceptée, joueurs étrangers autorisés) ; l'organisateur
     * peut restreindre.
     */
    // Par défaut fermées : licence promotionnelle non admise, joueurs étrangers
    // seulement en tournoi international (choix explicite de l'organisateur).
    private boolean accepteLicencePromotionnelle = false;
    private boolean accepteJoueursEtrangers = false;
    /** Catégories d'âge non admises dans le tournoi (choix du club ; ex. poussins). */
    private final java.util.EnumSet<CategorieAge> categoriesExclues = java.util.EnumSet.noneOf(CategorieAge.class);

    /** Dotation globale, calculée automatiquement (non saisie). */
    private double dotationGlobale;

    // --- Documents joints (références de fichiers, pas le binaire) ---
    private DocumentReference reglementDocument;    // PDF règlement
    private DocumentReference feuilleEngagement;    // ancienne feuille d'engagement papier (relue, plus proposée)
    private DocumentReference imagePromotionnelle;  // affiche / visuel du tournoi (facultatif)
    private OptionsReglement optionsReglement = OptionsReglement.parDefaut();
    /**
     * Copies figées à la publication : le règlement généré (absent pour un
     * règlement déposé, déjà figé par nature) et la feuille d'engagement. Après
     * publication, un horaire ou une dotation modifiés ne changent plus ces
     * documents.
     */
    private DocumentReglement reglementFige;
    private DocumentReglement feuilleEngagementFigee;
    private PhasePoints phasePoints;
    /** Mode de préparation (complète ou import), choisi après la création du brouillon. */
    private ModePreparation modePreparation = ModePreparation.COMPLETE;
    private QuotasTableaux quotasTableaux = QuotasTableaux.aucun();
    private ConditionsMaintien conditionsMaintien = ConditionsMaintien.aucune();
    private PolitiqueForfaits politiqueForfaits = PolitiqueForfaits.nonDefinie();
    private Paiement paiement = Paiement.aucun();
    /** Refus d'homologation successifs, du plus ancien au plus récent. */
    private final List<RefusHomologation> refusHomologation = new ArrayList<>();

    /**
     * Crée un tournoi d'un seul jour. Pour un tournoi sur plusieurs jours,
     * utiliser {@link #Tournoi(String, String, java.util.Collection)} ou
     * {@link #ajouterJour(LocalDate)}.
     */
    public Tournoi(String nom, String clubOrganisateur, LocalDate date) {
        this(nom, clubOrganisateur, java.util.List.of(Objects.requireNonNull(date, "date")));
    }

    /**
     * Crée un tournoi sur un ou plusieurs jours.
     *
     * @param nom              nom du tournoi
     * @param clubOrganisateur club organisateur
     * @param jours            jours du tournoi (au moins un, sans doublon effectif)
     */
    public Tournoi(String nom, String clubOrganisateur, java.util.Collection<LocalDate> jours) {
        this.nom = Objects.requireNonNull(nom, "nom");
        this.clubOrganisateur = Objects.requireNonNull(clubOrganisateur, "clubOrganisateur");
        Objects.requireNonNull(jours, "jours");
        if (jours.isEmpty() || jours.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Un tournoi doit avoir au moins un jour (non nul).");
        }
        this.jours.addAll(jours);
        this.numeroHomologation = "";
        this.nombreDeTables = 0;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = Objects.requireNonNull(nom, "nom");
    }

    public String getClubOrganisateur() {
        return clubOrganisateur;
    }

    public void setClubOrganisateur(String clubOrganisateur) {
        this.clubOrganisateur = Objects.requireNonNull(clubOrganisateur, "clubOrganisateur");
    }

    /**
     * @return le premier jour du tournoi (compatibilité mono-jour). Pour l'ensemble
     *         des jours, utiliser {@link #getJours()}.
     */
    public LocalDate getDate() {
        return jours.first();
    }

    /**
     * Fixe le tournoi sur un seul jour (remplace tous les jours existants).
     * Compatibilité mono-jour.
     */
    public void setDate(LocalDate date) {
        Objects.requireNonNull(date, "date");
        jours.clear();
        jours.add(date);
    }

    /** @return les jours du tournoi, triés, en lecture seule (au moins un). */
    public java.util.SortedSet<LocalDate> getJours() {
        return Collections.unmodifiableSortedSet(jours);
    }

    /** Ajoute un jour au tournoi (sans effet s'il est déjà présent). */
    public void ajouterJour(LocalDate jour) {
        jours.add(Objects.requireNonNull(jour, "jour"));
    }

    /**
     * Retire un jour du tournoi.
     *
     * @throws IllegalStateException si c'était le dernier jour (un tournoi doit
     *                               garder au moins un jour)
     */
    public void retirerJour(LocalDate jour) {
        Objects.requireNonNull(jour, "jour");
        if (jours.contains(jour) && jours.size() == 1) {
            throw new IllegalStateException("Un tournoi doit conserver au moins un jour.");
        }
        jours.remove(jour);
    }

    /** @return le nombre de jours du tournoi. */
    public int nombreDeJours() {
        return jours.size();
    }

    public NiveauTournoi getNiveau() {
        return niveau;
    }

    public void setNiveau(NiveauTournoi niveau) {
        this.niveau = niveau;
    }

    public String getNumeroHomologation() {
        return numeroHomologation;
    }

    public void setNumeroHomologation(String numeroHomologation) {
        this.numeroHomologation = numeroHomologation == null ? "" : numeroHomologation;
    }

    public int getNombreDeTables() {
        return nombreDeTables;
    }

    public void setNombreDeTables(int nombreDeTables) {
        if (nombreDeTables < 0) {
            throw new IllegalArgumentException("Le nombre de tables ne peut pas être négatif.");
        }
        this.nombreDeTables = nombreDeTables;
    }

    public RegleTableauxParJour getRegleTableauxParJour() {
        return regleTableauxParJour;
    }

    public void setRegleTableauxParJour(RegleTableauxParJour regleTableauxParJour) {
        this.regleTableauxParJour = Objects.requireNonNull(regleTableauxParJour, "regleTableauxParJour");
    }

    public List<Tableau> getTableaux() {
        return Collections.unmodifiableList(tableaux);
    }

    public void ajouterTableau(Tableau tableau) {
        tableaux.add(Objects.requireNonNull(tableau, "tableau"));
    }

    /**
     * Remplace un tableau par une nouvelle version (même position dans la liste).
     * Refusé dès qu'il a des inscrits : on ne change pas les règles d'un tableau
     * auquel des joueurs se sont déjà engagés.
     */
    public void remplacerTableau(Tableau ancien, Tableau nouveau) {
        Objects.requireNonNull(nouveau, "nouveau");
        int i = tableaux.indexOf(Objects.requireNonNull(ancien, "ancien"));
        if (i < 0) {
            throw new IllegalArgumentException("Ce tableau n'appartient pas au tournoi.");
        }
        exigerSansInscrit(ancien);
        tableaux.set(i, nouveau);
    }

    /** Retire un tableau sans inscrit. */
    public void retirerTableau(Tableau tableau) {
        if (!tableaux.contains(Objects.requireNonNull(tableau, "tableau"))) {
            throw new IllegalArgumentException("Ce tableau n'appartient pas au tournoi.");
        }
        exigerSansInscrit(tableau);
        tableaux.remove(tableau);
    }

    private static void exigerSansInscrit(Tableau t) {
        if (!t.getInscrits().isEmpty() || !t.getFileAttente().isEmpty()) {
            throw new IllegalStateException("Le tableau « " + t.getNom()
                    + " » a déjà des inscrits : il ne peut plus être modifié ni retiré.");
        }
    }

    // -------------------------------------------------------------------------
    // Juges-arbitres
    // -------------------------------------------------------------------------

    public List<AffectationJugeArbitre> getJugesArbitres() {
        return Collections.unmodifiableList(jugesArbitres);
    }

    /**
     * Affecte un juge-arbitre au tournoi. Un même juge-arbitre (même licence) ne
     * peut être affecté qu'une fois.
     *
     * @throws IllegalArgumentException si ce juge-arbitre est déjà affecté
     */
    public void ajouterJugeArbitre(AffectationJugeArbitre affectation) {
        Objects.requireNonNull(affectation, "affectation");
        String licence = affectation.getNumeroLicence();
        boolean deja = jugesArbitres.stream()
                .anyMatch(a -> a.getNumeroLicence().equals(licence));
        if (deja) {
            throw new IllegalArgumentException("Ce juge-arbitre est déjà affecté au tournoi.");
        }
        jugesArbitres.add(affectation);
    }

    /**
     * @param gradeRequis grade minimal recherché
     * @return {@code true} si au moins un juge-arbitre affecté a un grade suffisant
     */
    /** Retire un juge-arbitre (par numéro de licence). @return vrai s'il était affecté. */
    public boolean retirerJugeArbitre(String numeroLicence) {
        return jugesArbitres.removeIf(a -> a.getNumeroLicence().equals(numeroLicence));
    }

    /**
     * Remplace l'affectation d'un juge-arbitre (repéré par sa licence) en gardant
     * sa place dans la liste.
     *
     * @throws IllegalArgumentException si aucun juge-arbitre n'a cette licence, ou
     *         si la nouvelle licence est déjà celle d'un autre juge-arbitre
     */
    public void remplacerJugeArbitre(String numeroLicence, AffectationJugeArbitre nouvelle) {
        Objects.requireNonNull(nouvelle, "nouvelle");
        int i = -1;
        for (int k = 0; k < jugesArbitres.size(); k++) {
            if (jugesArbitres.get(k).getNumeroLicence().equals(numeroLicence)) {
                i = k;
            }
        }
        if (i < 0) {
            throw new IllegalArgumentException("Ce juge-arbitre n'est pas affecté au tournoi.");
        }
        for (int k = 0; k < jugesArbitres.size(); k++) {
            if (k != i && jugesArbitres.get(k).getNumeroLicence().equals(nouvelle.getNumeroLicence())) {
                throw new IllegalArgumentException("Ce juge-arbitre est déjà affecté au tournoi.");
            }
        }
        jugesArbitres.set(i, nouvelle);
    }

    /** @return le juge-arbitre principal, s'il y en a un. */
    public java.util.Optional<AffectationJugeArbitre> getJugeArbitrePrincipal() {
        return jugesArbitres.stream().filter(a -> a.getRole() == RoleJugeArbitre.PRINCIPAL).findFirst();
    }

    // -------------------------------------------------------------------------
    // Arbitres et tables
    // -------------------------------------------------------------------------

    public List<AffectationArbitre> getArbitres() {
        return Collections.unmodifiableList(arbitres);
    }

    /** Ajoute un arbitre ; refusé s'il est déjà affecté (même licence). */
    public void ajouterArbitre(AffectationArbitre a) {
        Objects.requireNonNull(a, "arbitre");
        if (arbitres.contains(a)) {
            throw new IllegalArgumentException(a.getPrenom() + " " + a.getNom() + " est déjà arbitre de ce tournoi.");
        }
        arbitres.add(a);
    }

    public boolean retirerArbitre(String numeroLicence) {
        return arbitres.removeIf(a -> a.getNumeroLicence().equals(numeroLicence));
    }

    public List<LotTables> getLotsTables() {
        return Collections.unmodifiableList(lotsTables);
    }

    /**
     * Remplace la description des tables (marques, couleurs). Si au moins un lot
     * est donné, le nombre de tables du tournoi devient leur total.
     */
    /** @return nombre de tables de la salle d'échauffement (hors tables de compétition), 0 si aucune. */
    public int getTablesEchauffement() {
        return tablesEchauffement;
    }

    public void setTablesEchauffement(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("Le nombre de tables d'échauffement ne peut pas être négatif.");
        }
        this.tablesEchauffement = n;
    }

    public void setLotsTables(List<LotTables> lots) {
        lotsTables.clear();
        if (lots != null) {
            lots.forEach(l -> lotsTables.add(Objects.requireNonNull(l)));
        }
        if (!lotsTables.isEmpty()) {
            setNombreDeTables(lotsTables.stream().mapToInt(LotTables::getNombre).sum());
        }
    }

    public boolean possedeJugeArbitreDeGrade(GradeJugeArbitre gradeRequis) {
        return jugesArbitres.stream().anyMatch(a -> a.couvre(gradeRequis));
    }

    // -------------------------------------------------------------------------
    // Champs de la demande d'homologation
    // -------------------------------------------------------------------------

    /** @deprecated remplacé par la catégorie {@link #getNiveau()} (IX.102). */
    @Deprecated
    public TypeTournoi getTypeTournoi() {
        return typeTournoi;
    }

    /** @deprecated remplacé par la catégorie {@link #setNiveau(NiveauTournoi)} (IX.102). */
    @Deprecated
    public void setTypeTournoi(TypeTournoi typeTournoi) {
        this.typeTournoi = typeTournoi;
    }

    public LocalDate getDateDebut() {
        return dateDebut;
    }

    public LocalDate getDateFin() {
        return dateFin;
    }

    /**
     * Définit les dates de début et de fin (saisies). La fin ne peut pas
     * précéder le début.
     */
    public void setPeriode(LocalDate dateDebut, LocalDate dateFin) {
        if (dateDebut != null && dateFin != null && dateFin.isBefore(dateDebut)) {
            throw new IllegalArgumentException("La date de fin ne peut pas précéder la date de début.");
        }
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
    }

    public AdresseTournoi getAdresse() {
        return adresse;
    }

    public void setAdresse(AdresseTournoi adresse) {
        this.adresse = adresse;
    }

    public String getCorrespondantNom() {
        return correspondantNom;
    }

    public String getCorrespondantPrenom() {
        return correspondantPrenom;
    }

    public String getCorrespondantEmail() {
        return correspondantEmail;
    }

    public String getCorrespondantTelephone() {
        return correspondantTelephone;
    }

    public String getCorrespondantAutresInfos() {
        return correspondantAutresInfos;
    }

    /**
     * Renseigne le correspondant (contact du dossier). Nom, prénom et email sont
     * requis pour l'homologation ; le service de cycle de vie le vérifie.
     */
    public void setCorrespondant(String nom, String prenom, String email,
                                 String telephone, String autresInfos) {
        this.correspondantNom = normaliserOpt(nom);
        this.correspondantPrenom = normaliserOpt(prenom);
        this.correspondantEmail = normaliserOpt(email);
        this.correspondantTelephone = normaliserOpt(telephone);
        this.correspondantAutresInfos = normaliserOpt(autresInfos);
    }

    public String getSiteWeb() {
        return siteWeb;
    }

    public void setSiteWeb(String siteWeb) {
        this.siteWeb = normaliserOpt(siteWeb);
    }

    public String getNumeroAffiliationClub() {
        return numeroAffiliationClub;
    }

    public void setNumeroAffiliationClub(String numeroAffiliationClub) {
        this.numeroAffiliationClub = normaliserOpt(numeroAffiliationClub);
    }

    /**
     * Échelon qui homologue ce tournoi : déduit de la catégorie et de la dotation
     * globale (IX.103). Tant que la catégorie n'est pas choisie, on rend la valeur
     * éventuellement relue d'un ancien fichier.
     */
    public EchelonHomologation getEchelonHomologation() {
        EchelonHomologation calcule = EchelonHomologation.pour(niveau, dotationGlobale);
        return calcule != null ? calcule : echelonHomologation;
    }

    /** Valeur stockée (relecture d'anciens fichiers) ; ignorée dès que la catégorie est connue. */
    public void setEchelonHomologation(EchelonHomologation echelonHomologation) {
        this.echelonHomologation = echelonHomologation;
    }

    /** Phase dont les points servent aux inscriptions ({@code null} = non choisie). */
    public ModePreparation getModePreparation() {
        return modePreparation;
    }

    public void setModePreparation(ModePreparation mode) {
        this.modePreparation = java.util.Objects.requireNonNull(mode, "mode");
    }

    public PhasePoints getPhasePoints() {
        return phasePoints;
    }

    public void setPhasePoints(PhasePoints phasePoints) {
        this.phasePoints = phasePoints;
    }

    public QuotasTableaux getQuotasTableaux() {
        return quotasTableaux;
    }

    public void setQuotasTableaux(QuotasTableaux q) {
        this.quotasTableaux = q == null ? QuotasTableaux.aucun() : q;
    }

    public ConditionsMaintien getConditionsMaintien() {
        return conditionsMaintien;
    }

    public void setConditionsMaintien(ConditionsMaintien c) {
        this.conditionsMaintien = c == null ? ConditionsMaintien.aucune() : c;
    }

    public PolitiqueForfaits getPolitiqueForfaits() {
        return politiqueForfaits;
    }

    public void setPolitiqueForfaits(PolitiqueForfaits p) {
        this.politiqueForfaits = p == null ? PolitiqueForfaits.nonDefinie() : p;
    }

    public Paiement getPaiement() {
        return paiement;
    }

    public void setPaiement(Paiement p) {
        this.paiement = p == null ? Paiement.aucun() : p;
    }

    public DocumentReference getImagePromotionnelle() {
        return imagePromotionnelle;
    }

    public void setImagePromotionnelle(DocumentReference image) {
        this.imagePromotionnelle = image;
    }

    public OptionsReglement getOptionsReglement() {
        return optionsReglement;
    }

    /**
     * @throws IllegalStateException si le tournoi est publié : son règlement est
     *         figé (passer par le service du règlement pour un refus motivé)
     */
    public void setOptionsReglement(OptionsReglement options) {
        exigerReglementModifiable();
        this.optionsReglement = options == null ? OptionsReglement.parDefaut() : options;
    }

    /** @return vrai si un règlement est disponible : fichier déposé, ou règlement généré. */
    public boolean aUnReglement() {
        if (optionsReglement.getMode() == OptionsReglement.Mode.GENERE) {
            return true;
        }
        return reglementDocument != null;
    }

    /** @return les catégories d'âge non admises (copie ; vide = toutes admises). */
    public java.util.Set<CategorieAge> getCategoriesExclues() {
        return java.util.Collections.unmodifiableSet(java.util.EnumSet.copyOf(
                categoriesExclues.isEmpty() ? java.util.EnumSet.noneOf(CategorieAge.class) : categoriesExclues));
    }

    public void setCategoriesExclues(java.util.Collection<CategorieAge> categories) {
        categoriesExclues.clear();
        if (categories != null) {
            categoriesExclues.addAll(categories);
        }
    }

    public boolean accepteLicencePromotionnelle() {
        return accepteLicencePromotionnelle;
    }

    public void setAccepteLicencePromotionnelle(boolean valeur) {
        this.accepteLicencePromotionnelle = valeur;
    }

    public boolean accepteJoueursEtrangers() {
        return accepteJoueursEtrangers;
    }

    public void setAccepteJoueursEtrangers(boolean valeur) {
        this.accepteJoueursEtrangers = valeur;
    }

    public double getDotationGlobale() {
        return dotationGlobale;
    }

    public void setDotationGlobale(double dotationGlobale) {
        if (dotationGlobale < 0) {
            throw new IllegalArgumentException("La dotation globale ne peut pas être négative.");
        }
        this.dotationGlobale = dotationGlobale;
    }

    public DocumentReference getReglementDocument() {
        return reglementDocument;
    }

    /**
     * @throws IllegalStateException si le tournoi est publié : le fichier déposé
     *         ne peut plus être remplacé ni retiré
     */
    public void setReglementDocument(DocumentReference reglementDocument) {
        exigerReglementModifiable();
        this.reglementDocument = reglementDocument;
    }

    /** Message du refus de toute modification du règlement d'un tournoi publié. */
    public static final String MOTIF_REGLEMENT_FIGE =
            "Le tournoi est publié : son règlement et sa feuille d'engagement sont figés "
                    + "et ne peuvent plus être modifiés.";

    /** @return vrai si le règlement (options, fichier déposé) peut encore être modifié. */
    public boolean reglementModifiable() {
        return statut != StatutTournoi.PUBLIE;
    }

    private void exigerReglementModifiable() {
        if (!reglementModifiable()) {
            throw new IllegalStateException(MOTIF_REGLEMENT_FIGE);
        }
    }

    /** @return le règlement généré figé à la publication, ou {@code null}. */
    public DocumentReglement getReglementFige() {
        return reglementFige;
    }

    /**
     * Enregistre la copie figée du règlement généré.
     *
     * @throws IllegalStateException si le tournoi est publié et a déjà une copie :
     *         elle ne se remplace pas
     */
    public void setReglementFige(DocumentReglement document) {
        if (!reglementModifiable() && reglementFige != null) {
            throw new IllegalStateException(MOTIF_REGLEMENT_FIGE);
        }
        this.reglementFige = document;
    }

    /** @return la feuille d'engagement figée à la publication, ou {@code null}. */
    public DocumentReglement getFeuilleEngagementFigee() {
        return feuilleEngagementFigee;
    }

    /**
     * Enregistre la copie figée de la feuille d'engagement.
     *
     * @throws IllegalStateException si le tournoi est publié et a déjà une copie
     */
    public void setFeuilleEngagementFigee(DocumentReglement document) {
        if (!reglementModifiable() && feuilleEngagementFigee != null) {
            throw new IllegalStateException(MOTIF_REGLEMENT_FIGE);
        }
        this.feuilleEngagementFigee = document;
    }

    public DocumentReference getFeuilleEngagement() {
        return feuilleEngagement;
    }

    public void setFeuilleEngagement(DocumentReference feuilleEngagement) {
        this.feuilleEngagement = feuilleEngagement;
    }

    private static String normaliserOpt(String v) {
        if (v == null) {
            return null;
        }
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }

    // -------------------------------------------------------------------------
    // Cycle de vie
    // -------------------------------------------------------------------------

    public StatutTournoi getStatut() {
        return statut;
    }

    /**
     * Vrai dès la demande d'homologation (demande, homologué, publié) : les
     * champs du dossier ({@link ChampHomologation}) ne sont plus modifiables.
     * Un brouillon, ou un tournoi annulé (comme pour le règlement figé, B010),
     * reste modifiable.
     */
    public boolean champsHomologationVerrouilles() {
        return statut == StatutTournoi.DEMANDE_HOMOLOGATION
                || statut == StatutTournoi.HOMOLOGUE
                || statut == StatutTournoi.PUBLIE;
    }

    /** @return les refus d'homologation, du plus ancien au plus récent (lecture seule). */
    public List<RefusHomologation> getRefusHomologation() {
        return Collections.unmodifiableList(refusHomologation);
    }

    /** @return le dernier refus d'homologation, s'il y en a eu un. */
    public java.util.Optional<RefusHomologation> getDernierRefusHomologation() {
        return refusHomologation.isEmpty()
                ? java.util.Optional.empty()
                : java.util.Optional.of(refusHomologation.get(refusHomologation.size() - 1));
    }

    /**
     * Ajoute un refus à l'historique. Ne change pas le statut : la transition est
     * portée par le service de cycle de vie ; la persistance s'en sert aussi
     * pour relire l'historique.
     */
    public void enregistrerRefusHomologation(RefusHomologation refus) {
        refusHomologation.add(Objects.requireNonNull(refus, "refus"));
    }

    public InfosReglement getInfosReglement() {
        return infosReglement;
    }

    public void setInfosReglement(InfosReglement infosReglement) {
        this.infosReglement = infosReglement;
    }

    /**
     * Applique une transition de statut <b>structurelle</b>. Cette méthode ne
     * vérifie que la légalité de la transition dans le graphe d'états
     * ({@link StatutTournoi#peutAllerVers(StatutTournoi)}) ; elle ne contrôle pas
     * les pré-requis de contenu (règlement complet, numéro d'homologation, tableau
     * féminin…), qui relèvent du service de cycle de vie.
     *
     * @param cible statut visé
     * @throws IllegalStateException si la transition n'est pas permise depuis le
     *                               statut courant (erreur de programmation)
     */
    public void changerStatut(StatutTournoi cible) {
        Objects.requireNonNull(cible, "cible");
        if (!statut.peutAllerVers(cible)) {
            throw new IllegalStateException(
                    "Transition de statut illégale : " + statut + " → " + cible + ".");
        }
        this.statut = cible;
    }

    @Override
    public String toString() {
        String periode = jours.size() == 1
                ? jours.first().toString()
                : jours.first() + " → " + jours.last();
        return "Tournoi \"" + nom + "\" - " + clubOrganisateur + " (" + periode + ")";
    }
}
