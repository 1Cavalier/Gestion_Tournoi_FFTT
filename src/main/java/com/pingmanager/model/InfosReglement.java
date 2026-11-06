package com.pingmanager.model;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Métadonnées du règlement d'un tournoi (article IX.111).
 *
 * <p>Regroupe les informations que l'organisateur doit fixer pour publier un
 * règlement conforme : le <b>responsable</b> (contact), la <b>salle</b>, le
 * <b>matériel</b> (nombre de tables, balles) et les <b>dates clés</b> de la
 * journée (clôture des inscriptions, clôture du pointage, début des matchs, fin
 * prévue).</p>
 *
 * <p>Objet <b>immuable</b> construit via son {@link Builder} — comme
 * {@link ConfigurationTableau} — afin d'éviter un constructeur à rallonge et de
 * faciliter l'ajout futur de champs. On distingue deux niveaux de validation :</p>
 * <ul>
 *   <li><b>Invariants durs</b>, vérifiés à la construction : le nombre de tables
 *       n'est pas négatif, et la <b>chronologie</b> — quand les dates sont
 *       présentes — est cohérente (clôture inscriptions &lt; clôture pointage
 *       &lt; début matchs). Un dossier incohérent ne peut pas exister.</li>
 *   <li><b>Complétude</b>, <i>calculée à la demande</i> via
 *       {@link #champsManquantsPourHomologation()} : quels champs restent à
 *       remplir pour homologuer. Cette liste n'empêche pas de construire un
 *       règlement partiel (utile en brouillon) ; c'est le service de cycle de vie
 *       qui l'exploite au moment de la transition.</li>
 * </ul>
 */
public final class InfosReglement {

    // --- Responsable / contact ---
    private final String responsableNom;
    private final String responsableEmail;
    private final String responsableTelephone;

    // --- Salle ---
    private final String salleNom;
    private final String salleAdresse;
    private final String salleCodePostal;
    private final String salleVille;

    // --- Matériel ---
    private final int nombreDeTables;      // 0 = non renseigné
    private final String marqueBalles;
    /** Politique de fourniture des balles (optionnel : null = non renseigné). */
    private final PolitiqueBalles politiqueBalles;
    /** Couleur des balles (2.3.3 : blanche ou orange). */
    private final CouleurBalles couleurBalles;
    /**
     * « Liste noire » : balles ou caractéristiques refusées (marques, matières…),
     * utile seulement quand les joueurs apportent leurs balles.
     */
    private final List<String> ballesRefusees;

    // --- Aire de jeu (optionnel) ---
    private final AireDeJeu aireDeJeu;

    // --- Dates clés ---
    /** Ouverture des inscriptions (échéance affichée aux joueurs). */
    private final LocalDateTime ouvertureInscriptions;
    private final LocalDateTime clotureInscriptions;
    private final LocalDateTime cloturePointage;
    private final LocalDateTime debutMatchs;
    private final LocalTime finPrevue;
    /** Horaire prévisionnel de début des finales (optionnel). */
    private final LocalTime horairePrevuFinales;

    // --- Tirage au sort public IX.109 (optionnel) ---
    private final LocalDateTime tiragePublicDateHeure;
    private final String tiragePublicLieu;

    private InfosReglement(Builder b) {
        this.responsableNom = Textes.optionnel(b.responsableNom);
        this.responsableEmail = Textes.optionnel(b.responsableEmail);
        this.responsableTelephone = Textes.optionnel(b.responsableTelephone);
        this.salleNom = Textes.optionnel(b.salleNom);
        this.salleAdresse = Textes.optionnel(b.salleAdresse);
        this.salleCodePostal = Textes.optionnel(b.salleCodePostal);
        this.salleVille = Textes.optionnel(b.salleVille);
        this.nombreDeTables = b.nombreDeTables;
        this.marqueBalles = Textes.optionnel(b.marqueBalles);
        this.politiqueBalles = b.politiqueBalles;
        this.couleurBalles = b.couleurBalles;
        this.ballesRefusees = b.ballesRefusees == null ? List.of() : List.copyOf(b.ballesRefusees);
        this.aireDeJeu = b.aireDeJeu;
        this.ouvertureInscriptions = b.ouvertureInscriptions;
        this.clotureInscriptions = b.clotureInscriptions;
        this.cloturePointage = b.cloturePointage;
        this.debutMatchs = b.debutMatchs;
        this.finPrevue = b.finPrevue;
        this.horairePrevuFinales = b.horairePrevuFinales;
        this.tiragePublicDateHeure = b.tiragePublicDateHeure;
        this.tiragePublicLieu = Textes.optionnel(b.tiragePublicLieu);

        // Invariants durs.
        if (nombreDeTables < 0) {
            throw new IllegalArgumentException("Le nombre de tables ne peut pas être négatif.");
        }
        verifierChronologie();
    }

    /**
     * Vérifie l'ordre des dates présentes : clôture inscriptions &lt; clôture
     * pointage &lt; début matchs. Les dates absentes sont ignorées (dossier en
     * cours de saisie).
     */
    private void verifierChronologie() {
        if (ouvertureInscriptions != null && clotureInscriptions != null
                && !ouvertureInscriptions.isBefore(clotureInscriptions)) {
            throw new IllegalArgumentException(
                    "L'ouverture des inscriptions doit précéder leur clôture.");
        }
        if (clotureInscriptions != null && cloturePointage != null
                && !clotureInscriptions.isBefore(cloturePointage)) {
            throw new IllegalArgumentException(
                    "La clôture des inscriptions doit précéder la clôture du pointage.");
        }
        if (cloturePointage != null && debutMatchs != null
                && !cloturePointage.isBefore(debutMatchs)) {
            throw new IllegalArgumentException(
                    "La clôture du pointage doit précéder le début des matchs.");
        }
    }

    // -------------------------------------------------------------------------
    // Complétude (calculée à la demande, n'empêche pas la construction)
    // -------------------------------------------------------------------------

    /**
     * Liste, en clair, les champs du règlement encore manquants pour pouvoir
     * homologuer le tournoi. Une liste vide signifie que le socle IX.111 est
     * complet.
     *
     * @return les libellés des champs manquants (jamais {@code null})
     */
    public List<String> champsManquantsPourHomologation() {
        List<String> manquants = new ArrayList<>();
        if (estVide(responsableNom)) {
            manquants.add("nom du responsable");
        }
        if (estVide(responsableEmail)) {
            manquants.add("email du responsable");
        }
        if (estVide(salleNom)) {
            manquants.add("nom de la salle");
        }
        if (estVide(salleVille)) {
            manquants.add("ville de la salle");
        }
        if (nombreDeTables <= 0) {
            manquants.add("nombre de tables");
        }
        if (estVide(marqueBalles)) {
            manquants.add("marque des balles");
        }
        if (clotureInscriptions == null) {
            manquants.add("date de clôture des inscriptions");
        }
        if (cloturePointage == null) {
            manquants.add("date de clôture du pointage");
        }
        if (debutMatchs == null) {
            manquants.add("date de début des matchs");
        }
        if (finPrevue == null) {
            manquants.add("heure de fin prévue");
        }
        return Collections.unmodifiableList(manquants);
    }

    /** @return true si le socle IX.111 est complet pour homologuer. */
    public boolean estCompletPourHomologation() {
        return champsManquantsPourHomologation().isEmpty();
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public String getResponsableNom() {
        return responsableNom;
    }

    public String getResponsableEmail() {
        return responsableEmail;
    }

    public String getResponsableTelephone() {
        return responsableTelephone;
    }

    public String getSalleNom() {
        return salleNom;
    }

    public String getSalleAdresse() {
        return salleAdresse;
    }

    public String getSalleCodePostal() {
        return salleCodePostal;
    }

    public String getSalleVille() {
        return salleVille;
    }

    public int getNombreDeTables() {
        return nombreDeTables;
    }

    public String getMarqueBalles() {
        return marqueBalles;
    }

    public CouleurBalles getCouleurBalles() {
        return couleurBalles;
    }

    /** @return les balles / caractéristiques refusées (jamais {@code null}). */
    public List<String> getBallesRefusees() {
        return ballesRefusees;
    }

    public LocalDateTime getOuvertureInscriptions() {
        return ouvertureInscriptions;
    }

    public LocalDateTime getClotureInscriptions() {
        return clotureInscriptions;
    }

    public LocalDateTime getCloturePointage() {
        return cloturePointage;
    }

    public LocalDateTime getDebutMatchs() {
        return debutMatchs;
    }

    public LocalTime getFinPrevue() {
        return finPrevue;
    }

    public PolitiqueBalles getPolitiqueBalles() {
        return politiqueBalles;
    }

    public AireDeJeu getAireDeJeu() {
        return aireDeJeu;
    }

    public LocalTime getHorairePrevuFinales() {
        return horairePrevuFinales;
    }

    public LocalDateTime getTiragePublicDateHeure() {
        return tiragePublicDateHeure;
    }

    public String getTiragePublicLieu() {
        return tiragePublicLieu;
    }

    // -------------------------------------------------------------------------
    // Builder
    // -------------------------------------------------------------------------

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Repart d'un règlement existant pour en produire une variante modifiée
     * (les {@code InfosReglement} étant immuables).
     */
    public Builder toBuilder() {
        return new Builder()
                .responsableNom(responsableNom)
                .responsableEmail(responsableEmail)
                .responsableTelephone(responsableTelephone)
                .salleNom(salleNom)
                .salleAdresse(salleAdresse)
                .salleCodePostal(salleCodePostal)
                .salleVille(salleVille)
                .nombreDeTables(nombreDeTables)
                .marqueBalles(marqueBalles)
                .politiqueBalles(politiqueBalles)
                .couleurBalles(couleurBalles)
                .ballesRefusees(ballesRefusees)
                .aireDeJeu(aireDeJeu)
                .ouvertureInscriptions(ouvertureInscriptions)
                .clotureInscriptions(clotureInscriptions)
                .cloturePointage(cloturePointage)
                .debutMatchs(debutMatchs)
                .finPrevue(finPrevue)
                .horairePrevuFinales(horairePrevuFinales)
                .tiragePublicDateHeure(tiragePublicDateHeure)
                .tiragePublicLieu(tiragePublicLieu);
    }

    /** Builder d'{@link InfosReglement}. */
    public static final class Builder {
        private String responsableNom;
        private String responsableEmail;
        private String responsableTelephone;
        private String salleNom;
        private String salleAdresse;
        private String salleCodePostal;
        private String salleVille;
        private int nombreDeTables;
        private String marqueBalles;
        private PolitiqueBalles politiqueBalles;
        private CouleurBalles couleurBalles;
        private List<String> ballesRefusees;
        private AireDeJeu aireDeJeu;
        private LocalDateTime ouvertureInscriptions;
        private LocalDateTime clotureInscriptions;
        private LocalDateTime cloturePointage;
        private LocalDateTime debutMatchs;
        private LocalTime finPrevue;
        private LocalTime horairePrevuFinales;
        private LocalDateTime tiragePublicDateHeure;
        private String tiragePublicLieu;

        public Builder responsableNom(String v) {
            this.responsableNom = v;
            return this;
        }

        public Builder responsableEmail(String v) {
            this.responsableEmail = v;
            return this;
        }

        public Builder responsableTelephone(String v) {
            this.responsableTelephone = v;
            return this;
        }

        public Builder salleNom(String v) {
            this.salleNom = v;
            return this;
        }

        public Builder salleAdresse(String v) {
            this.salleAdresse = v;
            return this;
        }

        public Builder salleCodePostal(String v) {
            this.salleCodePostal = v;
            return this;
        }

        public Builder salleVille(String v) {
            this.salleVille = v;
            return this;
        }

        public Builder nombreDeTables(int v) {
            this.nombreDeTables = v;
            return this;
        }

        public Builder marqueBalles(String v) {
            this.marqueBalles = v;
            return this;
        }

        public Builder ouvertureInscriptions(LocalDateTime v) {
            this.ouvertureInscriptions = v;
            return this;
        }

        public Builder clotureInscriptions(LocalDateTime v) {
            this.clotureInscriptions = v;
            return this;
        }

        public Builder cloturePointage(LocalDateTime v) {
            this.cloturePointage = v;
            return this;
        }

        public Builder debutMatchs(LocalDateTime v) {
            this.debutMatchs = v;
            return this;
        }

        public Builder finPrevue(LocalTime v) {
            this.finPrevue = v;
            return this;
        }

        public Builder couleurBalles(CouleurBalles v) {
            this.couleurBalles = v;
            return this;
        }

        public Builder ballesRefusees(List<String> v) {
            this.ballesRefusees = v;
            return this;
        }

        public Builder politiqueBalles(PolitiqueBalles v) {
            this.politiqueBalles = v;
            return this;
        }

        public Builder aireDeJeu(AireDeJeu v) {
            this.aireDeJeu = v;
            return this;
        }

        public Builder horairePrevuFinales(LocalTime v) {
            this.horairePrevuFinales = v;
            return this;
        }

        public Builder tiragePublicDateHeure(LocalDateTime v) {
            this.tiragePublicDateHeure = v;
            return this;
        }

        public Builder tiragePublicLieu(String v) {
            this.tiragePublicLieu = v;
            return this;
        }

        public InfosReglement build() {
            return new InfosReglement(this);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static boolean estVide(String s) {
        return s == null || s.isBlank();
    }
}
