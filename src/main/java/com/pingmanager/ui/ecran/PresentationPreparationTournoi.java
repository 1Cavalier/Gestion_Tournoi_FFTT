package com.pingmanager.ui.ecran;

import com.pingmanager.core.ResultatModificationTournoi;
import com.pingmanager.core.ResultatTransition;
import com.pingmanager.core.ServiceCompletudeTournoi;
import com.pingmanager.core.ServiceCompletudeTournoi.Item;
import com.pingmanager.core.ServiceCompletudeTournoi.Niveau;
import com.pingmanager.core.ServiceCompletudeTournoi.Section;
import com.pingmanager.core.ServiceVerrousHomologation;
import com.pingmanager.model.ChampHomologation;
import com.pingmanager.model.RefusHomologation;
import com.pingmanager.model.StatutTournoi;
import com.pingmanager.model.Tournoi;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Présentation (sans JavaFX) de la vue d'ensemble d'un tournoi en préparation
 * (B035, cahier des charges v0 section 6) : parcours en quatre étapes,
 * progression du dossier, refus d'homologation à rappeler sur le brouillon,
 * verrous du dossier d'homologation par section, et deuxième version de la vue
 * une fois l'homologation acceptée (cartes nouvelles en haut, règlement en bas).
 *
 * <p>Les données viennent du cœur ({@link ServiceCompletudeTournoi},
 * {@link ServiceVerrousHomologation}) ; cette classe ne fait que les mettre en
 * mots et en ordre pour l'écran {@link EcranCompletion}.</p>
 */
final class PresentationPreparationTournoi {

    /** Index des sections de {@link ServiceCompletudeTournoi#sections(Tournoi)}. */
    static final int SECTION_GENERAL = 0;
    static final int SECTION_TABLEAUX = 1;
    static final int SECTION_SPORTIF = 2;
    static final int SECTION_COMPLEMENTAIRE = 3;

    private static final ZoneId FUSEAU = ZoneId.of("Europe/Paris");
    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH'h'mm");

    /** État d'une étape du parcours. */
    enum EtatEtape {
        FAITE("faite"), EN_COURS("en-cours"), A_VENIR("a-venir");

        private final String suffixe;

        EtatEtape(String suffixe) {
            this.suffixe = suffixe;
        }

        /** @return la classe CSS de la pastille d'étape. */
        String classeCss() {
            return "parcours-etape-" + suffixe;
        }
    }

    /** Une étape du parcours (de la préparation à la publication). */
    record Etape(int numero, String titre, EtatEtape etat) {
    }

    /** Progression : éléments renseignés sur le total, et la phrase à afficher. */
    record Progression(int renseignes, int total, String libelle) {
        double fraction() {
            return total == 0 ? 1 : renseignes / (double) total;
        }

        int pourcentage() {
            return (int) Math.round(fraction() * 100);
        }
    }

    /** Carte d'une partie qui arrive avec la deuxième version (tâche non encore livrée). */
    record CarteAVenir(String titre, String description, String tache) {
    }

    private final Tournoi tournoi;
    private final ServiceCompletudeTournoi completude;
    private final ServiceVerrousHomologation verrous;

    PresentationPreparationTournoi(Tournoi tournoi, ServiceCompletudeTournoi completude,
                                   ServiceVerrousHomologation verrous) {
        this.tournoi = Objects.requireNonNull(tournoi, "tournoi");
        this.completude = Objects.requireNonNull(completude, "completude");
        this.verrous = Objects.requireNonNull(verrous, "verrous");
    }

    PresentationPreparationTournoi(Tournoi tournoi) {
        this(tournoi, new ServiceCompletudeTournoi(), new ServiceVerrousHomologation());
    }

    // ------------------------------------------------------------------
    // Version de la vue
    // ------------------------------------------------------------------

    /** @return vrai une fois l'homologation acceptée (homologué ou publié) : deuxième version de la vue. */
    boolean deuxiemeVersion() {
        StatutTournoi s = tournoi.getStatut();
        return s == StatutTournoi.HOMOLOGUE || s == StatutTournoi.PUBLIE;
    }

    /** @return vrai si les boutons « Homologation acceptée / refusée » doivent être proposés. */
    boolean reponseHomologationAttendue() {
        return tournoi.getStatut() == StatutTournoi.DEMANDE_HOMOLOGATION;
    }

    /** @return les cartes « bientôt » de la deuxième version, dans l'ordre d'affichage. */
    List<CarteAVenir> cartesAVenir() {
        if (!deuxiemeVersion()) {
            return List.of();
        }
        return List.of(
                new CarteAVenir("Salle",
                        "Préparez le plan de la salle : tables, allées et repères (table d'arbitrage, "
                                + "entrée, sortie…), sur une ou plusieurs salles.", "B024"),
                new CarteAVenir("Stratégie d'ordonnancement",
                        "Choisissez comment faire avancer la journée : finir vite les tableaux "
                                + "commencés tôt, tout faire avancer petit à petit, ou faire attendre "
                                + "le moins possible les joueurs.", "B025"),
                new CarteAVenir("Remplissage et finances",
                        "Suivez le taux de remplissage des tableaux, le total des inscriptions et le "
                                + "budget du tournoi, qui part du négatif.", "B036"));
    }

    /** Tâches de la deuxième version déjà livrées : leur vraie carte remplace la carte « bientôt ». */
    static final List<String> PARTIES_LIVREES = List.of("B024", "B025", "B036");

    /** @return vrai si la carte « Remplissage et finances » (B036) s'affiche. */
    boolean afficheRemplissageEtFinances() {
        return deuxiemeVersion();
    }

    /** @return vrai si la carte « Plan de la salle » (B024) s'affiche. */
    boolean afficheSalle() {
        return deuxiemeVersion();
    }

    /** @return vrai si la carte « Stratégie d'ordonnancement » (B025) s'affiche. */
    boolean afficheStrategie() {
        return deuxiemeVersion();
    }

    /** @return les cartes encore « bientôt » : celles de {@link #cartesAVenir()} non livrées. */
    List<CarteAVenir> cartesBientot() {
        return cartesAVenir().stream().filter(c -> !PARTIES_LIVREES.contains(c.tache())).toList();
    }

    /** @return le titre du bloc des sections du dossier. */
    String titreDossier() {
        return deuxiemeVersion() ? "Dossier du tournoi et règlement" : "Préparation du dossier";
    }

    // ------------------------------------------------------------------
    // Parcours et progression
    // ------------------------------------------------------------------

    /** @return les quatre étapes, de la préparation à la publication, avec leur état. */
    List<Etape> parcours() {
        StatutTournoi s = tournoi.getStatut();
        int courante;
        if (s == StatutTournoi.DEMANDE_HOMOLOGATION) {
            courante = 2;
        } else if (s == StatutTournoi.HOMOLOGUE) {
            courante = 3;
        } else if (s == StatutTournoi.PUBLIE) {
            courante = 5; // tout est fait
        } else {
            courante = 1;
        }
        String[] titres = {"Préparer le dossier", "Demande d'homologation", "Homologation obtenue",
                "Publication"};
        List<Etape> etapes = new ArrayList<>();
        for (int i = 0; i < titres.length; i++) {
            int n = i + 1;
            EtatEtape e = n < courante ? EtatEtape.FAITE : n == courante ? EtatEtape.EN_COURS : EtatEtape.A_VENIR;
            etapes.add(new Etape(n, titres[i], e));
        }
        return etapes;
    }

    /** @return la progression des éléments obligatoires pour l'homologation. */
    Progression progressionHomologation() {
        return progression(List.of(Niveau.OBLIGATOIRE_HOMOLOGATION), "pour la demande d'homologation");
    }

    /** @return la progression des éléments obligatoires (homologation et publication). */
    Progression progressionPublication() {
        return progression(List.of(Niveau.OBLIGATOIRE_HOMOLOGATION, Niveau.OBLIGATOIRE_PUBLICATION),
                "pour la publication");
    }

    /** @return la progression d'une section (tous niveaux) : « 4 / 6 renseignés ». */
    Progression progressionSection(int index) {
        Section s = completude.sections(tournoi).get(index);
        int r = 0;
        for (Item it : s.items()) {
            if (it.renseigne()) {
                r++;
            }
        }
        return new Progression(r, s.items().size(), r + " / " + s.items().size() + " renseignés");
    }

    private Progression progression(List<Niveau> niveaux, String objet) {
        int total = 0;
        int renseignes = 0;
        for (Section s : completude.sections(tournoi)) {
            for (Item it : s.items()) {
                if (niveaux.contains(it.niveau())) {
                    total++;
                    if (it.renseigne()) {
                        renseignes++;
                    }
                }
            }
        }
        String libelle = renseignes == total
                ? "Tout est renseigné " + objet + "."
                : renseignes + " / " + total + " éléments obligatoires renseignés " + objet + ".";
        return new Progression(renseignes, total, libelle);
    }

    // ------------------------------------------------------------------
    // Refus d'homologation
    // ------------------------------------------------------------------

    /**
     * @return le rappel du dernier refus, à afficher sur le brouillon seulement
     *         (après une nouvelle demande, il n'a plus lieu d'être en tête)
     */
    Optional<String> titreRefus() {
        Optional<RefusHomologation> r = refusAffiche();
        return r.map(x -> "Homologation refusée le " + JOUR.format(x.date().atZone(FUSEAU)));
    }

    /** @return le motif du dernier refus, à afficher sur le brouillon. */
    Optional<String> motifRefus() {
        return refusAffiche().map(RefusHomologation::motif);
    }

    String conseilApresRefus() {
        return "Corrigez le dossier selon ce motif, puis refaites la demande d'homologation.";
    }

    private Optional<RefusHomologation> refusAffiche() {
        if (tournoi.getStatut() != StatutTournoi.BROUILLON) {
            return Optional.empty();
        }
        return tournoi.getDernierRefusHomologation();
    }

    /** @return le message d'erreur d'une transition refusée (acceptation ou refus d'homologation). */
    static String messageTransition(ResultatTransition r) {
        if (r.estEffectuee()) {
            return null;
        }
        if (r.getMotif() == ResultatTransition.Motif.MOTIF_REFUS_ABSENT) {
            return "Saisissez le motif du refus : il est obligatoire et restera visible sur le brouillon.";
        }
        return "Impossible pour l'instant : " + String.join(", ", r.getManquants()) + ".";
    }

    /** @return le message d'une modification refusée par un verrou, ou {@code null} si elle est appliquée. */
    static String messageModification(ResultatModificationTournoi r) {
        return r.estAppliquee() ? null : r.getMessage();
    }

    // ------------------------------------------------------------------
    // Verrous du dossier d'homologation
    // ------------------------------------------------------------------

    /** @return les champs du dossier d'homologation portés par une section. */
    static List<ChampHomologation> champsSection(int index) {
        if (index == SECTION_GENERAL) {
            return List.of(ChampHomologation.NOM, ChampHomologation.CATEGORIE,
                    ChampHomologation.CLUB_ORGANISATEUR, ChampHomologation.NUMERO_AFFILIATION,
                    ChampHomologation.ADRESSE, ChampHomologation.DATES, ChampHomologation.CORRESPONDANT);
        }
        if (index == SECTION_TABLEAUX) {
            return List.of(ChampHomologation.LISTE_TABLEAUX, ChampHomologation.TABLEAU_NOM,
                    ChampHomologation.TABLEAU_CATEGORIE_POINTS, ChampHomologation.TABLEAU_PUBLIC,
                    ChampHomologation.TABLEAU_DEBUT);
        }
        if (index == SECTION_SPORTIF) {
            return List.of(ChampHomologation.JUGES_ARBITRES);
        }
        return List.of();
    }

    /** @return vrai si au moins un champ de la section est verrouillé. */
    boolean sectionVerrouillee(int index) {
        for (ChampHomologation c : champsSection(index)) {
            if (verrous.estVerrouille(tournoi, c)) {
                return true;
            }
        }
        return false;
    }

    /** @return le motif du verrou d'une section (bandeau en tête du formulaire), ou {@code null}. */
    String motifVerrouSection(int index) {
        return sectionVerrouillee(index) ? verrous.motifVerrou(tournoi, champsSection(index)) : null;
    }

    /** @return la mention courte d'une carte de section verrouillée, ou {@code null}. */
    String mentionVerrouSection(int index) {
        return sectionVerrouillee(index) ? "Dossier d'homologation verrouillé" : null;
    }
}
