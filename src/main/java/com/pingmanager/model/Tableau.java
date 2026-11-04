package com.pingmanager.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Un tableau (une épreuve) au sein d'un tournoi : par exemple
 * "Tableau A - moins de 1500 points" ou "Tableau Dames".
 *
 * <p>Porte sa {@link ConfigurationTableau}, la liste de ses joueurs inscrits,
 * une file d'attente (quand le tableau est plein), et les poules générées.
 * C'est un conteneur : la génération des poules et le déroulé relèvent des
 * services du package {@code core}.</p>
 */
public final class Tableau {

    private final String nom;
    private final ConfigurationTableau configuration;

    private final List<Joueur> inscrits = new ArrayList<>();
    private final List<Joueur> fileAttente = new ArrayList<>();
    private final List<Poule> poules = new ArrayList<>();

    /**
     * Tableau à élimination directe de la phase finale, construit une fois les
     * poules terminées. {@code null} tant que la phase finale n'a pas été montée.
     */
    private TableauElimination phaseFinale = null;

    /**
     * État de chaque joueur vis-à-vis de CE tableau : a-t-il pointé (présent au
     * pointage de ce tableau) et a-t-il payé (réglé l'engagement de ce tableau).
     * Les deux booléens sont indépendants : un inscrit en ligne peut être payé
     * sans être pointé ; un repêché le jour J n'est ni l'un ni l'autre. La
     * présence est donc un fait <b>par tableau</b>, pas au niveau du tournoi.
     */
    private final Map<Joueur, EtatInscrit> etats = new HashMap<>();

    /**
     * Joueurs remboursés pour ce tableau. Conservé séparément des {@code etats}
     * car un remboursement intervient typiquement lors d'un désistement (le joueur
     * quitte alors les inscrits) : la trace doit survivre au retrait.
     */
    private final java.util.Set<Joueur> rembourses = new java.util.HashSet<>();

    /** Calendrier du tableau (pointage, début des matchs). */
    private HorairesTableau horaires = new HorairesTableau();

    /**
     * Type de tableau au sens du formulaire d'homologation SPID (libellé libre,
     * ex. « Messieurs série 1 », « Cadets », « Toutes séries »). Distinct du
     * format de compétition ({@link ConfigurationTableau#getFormat()}) qui décrit
     * le déroulé sportif. Obligatoire pour l'homologation.
     */
    private String type;

    /** Étape du cycle de vie du tableau (inscription → pointage → lancé). */
    private StatutTableau statut = StatutTableau.INSCRIPTION;

    /**
     * Fait indiquant que le pointage a été clôturé (présents figés). Distinct de
     * l'état {@link #statut} : un tableau reste en {@link StatutTableau#POINTAGE}
     * entre la clôture du pointage et le lancement. Sert de pré-condition au
     * lancement.
     */
    private boolean pointageCloture = false;

    public Tableau(String nom, ConfigurationTableau configuration) {
        this.nom = Objects.requireNonNull(nom, "nom");
        this.configuration = Objects.requireNonNull(configuration, "configuration");
    }

    public HorairesTableau getHoraires() {
        return horaires;
    }

    public void setHoraires(HorairesTableau horaires) {
        this.horaires = Objects.requireNonNull(horaires, "horaires");
    }

    public String getNom() {
        return nom;
    }

    /**
     * @return le type de tableau (libellé SPID), ou {@code null} si non renseigné.
     */
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = (type == null || type.isBlank()) ? null : type.trim();
    }

    public ConfigurationTableau getConfiguration() {
        return configuration;
    }

    public List<Joueur> getInscrits() {
        return Collections.unmodifiableList(inscrits);
    }

    public List<Joueur> getFileAttente() {
        return Collections.unmodifiableList(fileAttente);
    }

    public List<Poule> getPoules() {
        return Collections.unmodifiableList(poules);
    }

    public boolean estPlein() {
        return configuration.aLimiteDePlaces()
                && inscrits.size() >= configuration.getPlacesMax();
    }

    /**
     * @return true si la liste d'attente a atteint sa taille maximale (mécanisme
     *         de remplissage). Toujours vraie si le tableau n'a pas de liste
     *         d'attente configurée.
     */
    public boolean fileAttentePleine() {
        return fileAttente.size() >= configuration.getTailleMaxListeAttente();
    }

    /**
     * Tente d'inscrire un joueur.
     *
     * @return le résultat de la tentative : inscrit ; mis en file d'attente
     *         (tableau plein mais liste d'attente non pleine) ; refusé car la
     *         capacité et la liste d'attente sont pleines ; refusé pour
     *         inéligibilité ; ou déjà présent.
     */
    public ResultatInscription inscrire(Joueur joueur) {
        Objects.requireNonNull(joueur, "joueur");
        if (joueur.estADeterminer()) {
            throw new IllegalArgumentException(
                    "Impossible d'inscrire le joueur sentinelle « à déterminer » : "
                            + "il est réservé aux emplacements vacants du tableau à élimination.");
        }
        if (inscrits.contains(joueur) || fileAttente.contains(joueur)) {
            return ResultatInscription.DEJA_PRESENT;
        }
        if (!configuration.joueurEligible(joueur)) {
            return ResultatInscription.NON_ELIGIBLE;
        }
        if (estPlein()) {
            if (fileAttentePleine()) {
                return ResultatInscription.LISTE_ATTENTE_PLEINE;
            }
            fileAttente.add(joueur);
            return ResultatInscription.EN_FILE_ATTENTE;
        }
        inscrits.add(joueur);
        return ResultatInscription.INSCRIT;
    }

    /**
     * Désinscrit un joueur. Si une place se libère et que la file d'attente
     * n'est pas vide, le premier joueur de la file est automatiquement promu.
     *
     * @return le joueur promu depuis la file d'attente, ou {@code null}.
     */
    public Joueur desinscrire(Joueur joueur) {
        Objects.requireNonNull(joueur, "joueur");
        boolean retire = inscrits.remove(joueur);
        etats.remove(joueur);
        if (retire && !fileAttente.isEmpty()) {
            Joueur promu = fileAttente.remove(0);
            inscrits.add(promu);
            return promu;
        }
        fileAttente.remove(joueur);
        return null;
    }

    // --- Cycle de vie du tableau ---

    public StatutTableau getStatut() {
        return statut;
    }

    /**
     * Ouvre le pointage : fait passer le tableau de {@link StatutTableau#INSCRIPTION}
     * à {@link StatutTableau#POINTAGE}. Déclenché le jour J par le juge-arbitre,
     * tableau par tableau.
     *
     * @throws IllegalStateException si le tableau n'est pas en phase d'inscription
     */
    public void ouvrirPointage() {
        if (statut != StatutTableau.INSCRIPTION) {
            throw new IllegalStateException(
                    "Le pointage ne peut être ouvert que depuis la phase d'inscription (statut actuel : " + statut + ").");
        }
        this.statut = StatutTableau.POINTAGE;
    }

    /** @return true si le tableau a été lancé (tirage fait, poules générées). */
    public boolean estLance() {
        return statut == StatutTableau.LANCE;
    }

    /**
     * Marque le pointage comme clôturé (présents figés). N'altère pas le statut, qui
     * reste {@link StatutTableau#POINTAGE} : la clôture est un fait, pas un état.
     */
    public void marquerPointageCloture() {
        this.pointageCloture = true;
    }

    /** @return true si le pointage a été clôturé (pré-condition du lancement). */
    public boolean pointageEstCloture() {
        return pointageCloture;
    }

    /**
     * Fait passer le tableau à {@link StatutTableau#LANCE}. Réservé au service de
     * lancement, qui vérifie les pré-conditions (pointage clôturé) et génère les
     * poules avant d'appeler cette transition.
     *
     * @throws IllegalStateException si le tableau n'est pas en phase de pointage
     */
    public void marquerLance() {
        if (statut != StatutTableau.POINTAGE) {
            throw new IllegalStateException(
                    "Un tableau ne peut être lancé que depuis la phase de pointage (statut actuel : " + statut + ").");
        }
        this.statut = StatutTableau.LANCE;
    }

    /** @return true si toutes les poules du tableau sont terminées (et qu'il y en a). */
    public boolean poulesTerminees() {
        return !poules.isEmpty() && poules.stream().allMatch(Poule::estTerminee);
    }

    /** @return le tableau à élimination de la phase finale, ou {@code null} s'il n'est pas monté. */
    public TableauElimination getPhaseFinale() {
        return phaseFinale;
    }

    public boolean aPhaseFinale() {
        return phaseFinale != null;
    }

    /**
     * Range le tableau à élimination de la phase finale et fait passer le tableau au
     * statut {@link StatutTableau#PHASE_FINALE}. Réservé au service qui construit la
     * phase finale (il vérifie que les poules sont terminées au préalable).
     *
     * @throws IllegalStateException si le tableau n'est pas en phase de poules (LANCE)
     */
    public void definirPhaseFinale(TableauElimination phaseFinale) {
        Objects.requireNonNull(phaseFinale, "phaseFinale");
        if (statut != StatutTableau.LANCE) {
            throw new IllegalStateException(
                    "La phase finale ne peut être montée que depuis la phase de poules (statut actuel : " + statut + ").");
        }
        this.phaseFinale = phaseFinale;
        this.statut = StatutTableau.PHASE_FINALE;
    }

    // --- Pointage et paiement, par tableau (jour J) ---

    /**
     * Marque un joueur comme ayant pointé pour ce tableau (présent au pointage de
     * ce tableau). Le joueur doit être un <b>inscrit</b> : on ne pointe pas un
     * joueur en liste d'attente (il doit d'abord être repêché).
     */
    public void pointer(Joueur joueur) {
        etatInscrit(joueur).pointe = true;
    }

    /** Annule le pointage d'un joueur sur ce tableau (correction du juge-arbitre). */
    public void depointer(Joueur joueur) {
        etatInscrit(joueur).pointe = false;
    }

    /**
     * Marque un joueur comme ayant payé l'engagement de ce tableau. Réservé aux
     * <b>inscrits</b> : on ne paie pas un tableau tant qu'on est en liste d'attente
     * (la place n'y est pas garantie). Un joueur repêché devient inscrit et peut
     * alors payer.
     */
    public void marquerPaye(Joueur joueur) {
        etatInscrit(joueur).paye = true;
    }

    /**
     * Marque un joueur comme remboursé (traçabilité ; typiquement lors d'un
     * désistement pour cas particulier validé par le juge-arbitre). La trace
     * survit au retrait du joueur du tableau.
     */
    public void marquerRembourse(Joueur joueur) {
        Objects.requireNonNull(joueur, "joueur");
        rembourses.add(joueur);
    }

    /** @return true si le joueur a pointé pour ce tableau. */
    public boolean aPointe(Joueur joueur) {
        EtatInscrit e = etats.get(joueur);
        return e != null && e.pointe;
    }

    /** @return true si le joueur a payé l'engagement de ce tableau. */
    public boolean aPaye(Joueur joueur) {
        EtatInscrit e = etats.get(joueur);
        return e != null && e.paye;
    }

    /** @return true si le joueur a été remboursé pour ce tableau. */
    public boolean aEteRembourse(Joueur joueur) {
        return rembourses.contains(joueur);
    }

    private EtatInscrit etatInscrit(Joueur joueur) {
        Objects.requireNonNull(joueur, "joueur");
        if (!inscrits.contains(joueur)) {
            throw new IllegalArgumentException(
                    "Le joueur n'est pas inscrit sur ce tableau (pointage/paiement réservés aux inscrits) : "
                            + joueur.getNumeroLicence());
        }
        return etats.computeIfAbsent(joueur, j -> new EtatInscrit());
    }

    public void definirPoules(List<Poule> nouvellesPoules) {
        poules.clear();
        poules.addAll(Objects.requireNonNull(nouvellesPoules, "nouvellesPoules"));
    }

    /**
     * Retire un inscrit <b>sans</b> promotion automatique depuis la file d'attente.
     * Primitive bas niveau destinée aux services qui pilotent eux-mêmes le
     * repêchage (ex. clôture du pointage). Contraste avec {@link #desinscrire(Joueur)}
     * qui, lui, promeut le premier de la file.
     *
     * @return true si le joueur était bien inscrit et a été retiré
     */
    public boolean retirerInscrit(Joueur joueur) {
        Objects.requireNonNull(joueur, "joueur");
        boolean retire = inscrits.remove(joueur);
        if (retire) {
            etats.remove(joueur);
        }
        return retire;
    }

    /**
     * Repêche explicitement le premier joueur de la liste d'attente vers les
     * inscrits, si la file n'est pas vide et qu'une place est disponible.
     *
     * @return le joueur repêché, ou {@code null} si rien n'a pu être repêché
     */
    public Joueur repecherPremier() {
        if (fileAttente.isEmpty()) {
            return null;
        }
        if (estPlein()) {
            return null;
        }
        Joueur repeche = fileAttente.remove(0);
        inscrits.add(repeche);
        return repeche;
    }

    // -------------------------------------------------------------------------
    // Restauration (rechargement depuis une sauvegarde)
    // -------------------------------------------------------------------------

    /**
     * Réinstalle l'état complet d'un tableau tel qu'il a été sauvegardé, sans
     * repasser par les validations du cycle de vie (éligibilité, capacité,
     * ordre des transitions).
     *
     * <p>Réservé à la couche de persistance : recharger un tableau déjà lancé,
     * voire en phase finale, ne doit pas être bloqué par les gardes qui régissent
     * le déroulé normal (par exemple {@code ouvrirPointage} exige la phase
     * d'inscription). La méthode pose directement l'état ; c'est à l'appelant de
     * fournir un instantané cohérent (celui qui a été écrit à la sauvegarde).</p>
     *
     * <p>Cette méthode vit dans le modèle et ne crée aucune dépendance vers la
     * persistance : le cœur reste découplé (règle d'or du projet).</p>
     */
    public void restaurerEtat(EtatRestaure e) {
        Objects.requireNonNull(e, "e");
        inscrits.clear();
        inscrits.addAll(e.inscrits);
        fileAttente.clear();
        fileAttente.addAll(e.fileAttente);
        poules.clear();
        poules.addAll(e.poules);
        etats.clear();
        for (Joueur j : e.pointes) {
            etats.computeIfAbsent(j, x -> new EtatInscrit()).pointe = true;
        }
        for (Joueur j : e.payes) {
            etats.computeIfAbsent(j, x -> new EtatInscrit()).paye = true;
        }
        rembourses.clear();
        rembourses.addAll(e.rembourses);
        this.phaseFinale = e.phaseFinale;
        this.horaires = e.horaires != null ? e.horaires : new HorairesTableau();
        this.type = e.type;
        this.statut = Objects.requireNonNull(e.statut, "statut");
        this.pointageCloture = e.pointageCloture;
    }

    /**
     * Instantané de l'état d'un {@link Tableau} destiné à la restauration. Simple
     * conteneur mutable rempli par la couche de persistance avant l'appel à
     * {@link Tableau#restaurerEtat(EtatRestaure)}.
     */
    public static final class EtatRestaure {
        public final List<Joueur> inscrits = new ArrayList<>();
        public final List<Joueur> fileAttente = new ArrayList<>();
        public final List<Poule> poules = new ArrayList<>();
        public final List<Joueur> pointes = new ArrayList<>();
        public final List<Joueur> payes = new ArrayList<>();
        public final java.util.Set<Joueur> rembourses = new java.util.HashSet<>();
        public TableauElimination phaseFinale;
        public HorairesTableau horaires;
        public String type;
        public StatutTableau statut = StatutTableau.INSCRIPTION;
        public boolean pointageCloture;
    }

    @Override
    public String toString() {
        return "Tableau " + nom + " (" + inscrits.size() + " inscrits, "
                + fileAttente.size() + " en attente)";
    }

    /** Résultat possible d'une tentative d'inscription. */
    public enum ResultatInscription {
        INSCRIT,
        EN_FILE_ATTENTE,
        LISTE_ATTENTE_PLEINE,
        NON_ELIGIBLE,
        DEJA_PRESENT
    }

    /** État d'un joueur vis-à-vis de ce tableau : pointé, payé. */
    private static final class EtatInscrit {
        private boolean pointe = false;
        private boolean paye = false;
    }
}
