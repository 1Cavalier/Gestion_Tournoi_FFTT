package com.pingmanager.ui.ecran;

import com.pingmanager.core.BilanRemplissageFinances;
import com.pingmanager.core.ListePointage;
import com.pingmanager.core.PrevisionTournoi;
import com.pingmanager.core.RemplissageTableaux;
import com.pingmanager.core.ServicePreparationJourJ;
import com.pingmanager.core.ServicePreparationJourJ.Acces;
import com.pingmanager.core.ServicePreparationJourJ.Alerte;
import com.pingmanager.core.ServicePreparationJourJ.Gravite;
import com.pingmanager.core.ServicePreparationJourJ.TypeAlerte;
import com.pingmanager.core.ServiceRemplissageFinances;
import com.pingmanager.core.StatistiquesSimulation;
import com.pingmanager.core.reglement.Formats;
import com.pingmanager.model.StatutTableau;
import com.pingmanager.model.StrategieOrdonnancement;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Présentation (sans JavaFX) de l'étape « J-7 : préparer le jour J » (B051) :
 * la carte d'accès de la vue d'ensemble du tournoi et l'écran de préparation
 * ({@link EcranPreparationJourJ}), qui reprend la disposition du jour J en
 * <b>mode préparation</b> : tout est en lecture seule, rien n'est lancé.
 *
 * <p>Les données viennent du cœur ({@link ServicePreparationJourJ} pour l'accès
 * et les alertes, {@link ServiceRemplissageFinances} pour les compteurs,
 * {@link ListePointage} pour l'aperçu du pointage) ; cette classe les met en
 * mots et en ordre.</p>
 */
final class PresentationPreparationJourJ {

    static final String TITRE_CARTE = "J-7 : préparer le jour J";
    static final String TITRE = "Préparation du jour J";
    static final String BOUTON_OUVRIR = "Préparer le jour J";
    static final String BANDEAU = "Mode préparation : rien n'est lancé. Aucun pointage, aucun score, aucun tirage : "
            + "cet écran montre le jour J tel qu'il se présentera, sans rien modifier au tournoi.";
    static final String AIDE_CARTE = "Le jour J tel qu'il se présentera, en lecture seule : alertes à traiter, "
            + "tableaux par jour, aperçu du pointage et de la salle, documents à imprimer.";
    static final String AUCUNE_ALERTE = "Rien à signaler : horaires, juge-arbitre, clubs et points sont renseignés.";
    static final String PREVISION_A_LA_DEMANDE = "La prévision rejoue chaque journée cent fois : elle se calcule à la "
            + "demande, avec les réglages par défaut (tables du tournoi, inscrits réels).";
    static final String PREVISION_EN_COURS = "Calcul de la prévision en cours…";
    static final String BOUTON_PREVISION = "Calculer la prévision";
    static final String LIEN_STRATEGIE = "Choisir la stratégie et régler la prévision";
    static final String AIDE_POINTAGE = "Aperçu : la liste telle qu'elle sera au poste de pointage. Les cases sont "
            + "inactives ; le pointage s'ouvre le jour J, tableau par tableau.";
    static final String AIDE_SALLE = "Aperçu : les tables telles qu'elles seront affichées le jour J. Aucun match "
            + "n'est proposé ni envoyé tant que les tableaux ne sont pas lancés.";
    static final String SALLE_SANS_TABLE = "Le nombre de tables du tournoi n'est pas renseigné. Indiquez-le dans "
            + "« Informations sportives et techniques » pour préparer la salle.";
    /** Au-delà, l'aperçu du pointage est tronqué (comme le poste de pointage). */
    static final int LIMITE_APERCU_POINTAGE = PresentationPostePointage.LIMITE_LIGNES;
    /** Nombre de joueurs nommés dans une alerte regroupée. */
    static final int JOUEURS_NOMMES = 5;

    /** Onglets de l'écran, dans l'ordre. */
    enum Onglet {
        GENERAL("Général"),
        TABLEAUX("Tableaux"),
        POINTAGE("Pointage"),
        SALLE("Salle"),
        IMPRESSIONS("À imprimer / à préparer");

        private final String libelle;

        Onglet(String libelle) {
            this.libelle = libelle;
        }

        String libelle() {
            return libelle;
        }
    }

    /** Carte d'accès de la vue d'ensemble : visible ou non, ouverte ou non, et ses phrases. */
    record CarteAcces(boolean visible, boolean ouverte, String pastille, StatutPastille statut, String etat) {
    }

    /** Une alerte mise en mots. */
    record LigneAlerte(String titre, String detail, String pastille, StatutPastille statut) {
    }

    /** Un chiffre clé. */
    record Chiffre(String valeur, String libelle) {
    }

    /** Un tableau dans l'onglet « Tableaux ». */
    record LigneTableau(String nom, String horaires, String inscrits, String attente, String payes, String etat,
                        StatutPastille statut, double jauge) {
    }

    /** Les tableaux d'une journée ({@code jour} nul : sans horaire). */
    record JourneeTableaux(LocalDate jour, String titre, List<LigneTableau> lignes) {
    }

    /** Un tableau dans l'ordre prévu de la salle. */
    record Passage(int rang, String nom, String detail) {
    }

    /** L'ordre prévu des tableaux d'une journée. */
    record JourneeSalle(String titre, List<Passage> passages) {
    }

    /** Ce qu'on peut faire d'un document à préparer. */
    enum Disponibilite {
        /** Existe déjà : un bouton le produit. */
        DISPONIBLE("Disponible", StatutPastille.SUCCES),
        /** Existe, mais seulement une fois le tirage fait, depuis l'écran du jour J. */
        JOUR_J("Le jour J", StatutPastille.INFO),
        /** Pas encore dans PingManager. */
        A_VENIR("À venir", StatutPastille.NEUTRE);

        private final String libelle;
        private final StatutPastille statut;

        Disponibilite(String libelle, StatutPastille statut) {
            this.libelle = libelle;
            this.statut = statut;
        }

        String libelle() {
            return libelle;
        }

        StatutPastille statut() {
            return statut;
        }
    }

    /** Document produit par un bouton de l'onglet « À imprimer ». */
    enum Document {
        REGLEMENT, FEUILLE_ENGAGEMENT
    }

    /**
     * Un document à imprimer ou à préparer.
     *
     * @param document le document produit par le bouton, ou {@code null} (pas de bouton)
     */
    record Impression(String titre, String description, Disponibilite disponibilite, Document document,
                      String libelleBouton) {
        boolean aBouton() {
            return document != null;
        }
    }

    private PresentationPreparationJourJ() {
    }

    // ------------------------------------------------------------------
    // Accès et en-tête
    // ------------------------------------------------------------------

    /** « J-3 », « Jour J » le premier jour, « J+1 » ensuite. */
    static String compteARebours(long joursAvantPremierJour) {
        if (joursAvantPremierJour > 0) {
            return "J-" + joursAvantPremierJour;
        }
        return joursAvantPremierJour == 0 ? "Jour J" : "J+" + (-joursAvantPremierJour);
    }

    /** « Préparation — J-3 ». */
    static String sousTitre(Acces acces) {
        return "Préparation — " + compteARebours(acces.joursAvantPremierJour());
    }

    /** Carte « J-7 : préparer le jour J » de la vue d'ensemble du tournoi. */
    static CarteAcces carte(Acces acces) {
        if (!acces.proposee()) {
            return new CarteAcces(false, false, "", StatutPastille.NEUTRE, "");
        }
        String rebours = compteARebours(acces.joursAvantPremierJour());
        if (!acces.ouverte()) {
            return new CarteAcces(true, false, rebours, StatutPastille.NEUTRE,
                    "Disponible à partir du " + Formats.jourLong(acces.ouverture())
                            + " (sept jours avant le premier jour du tournoi).");
        }
        if (acces.demo()) {
            return new CarteAcces(true, true, rebours, StatutPastille.INFO,
                    "Ouverte dès maintenant pour ce tournoi de démonstration (pour un vrai tournoi : à partir du "
                            + Formats.jourLong(acces.ouverture()) + ").");
        }
        String etat;
        if (acces.joursAvantPremierJour() > 1) {
            etat = "Le tournoi commence dans " + acces.joursAvantPremierJour() + " jours.";
        } else if (acces.joursAvantPremierJour() == 1) {
            etat = "Le tournoi commence demain.";
        } else if (acces.joursAvantPremierJour() == 0) {
            etat = "Le tournoi commence aujourd'hui.";
        } else {
            etat = "Le tournoi a commencé.";
        }
        return new CarteAcces(true, true, rebours, StatutPastille.INFO, etat);
    }

    /** « Samedi 10 avril 2027 » ou « Du samedi 10 avril 2027 au dimanche 11 avril 2027 ». */
    static String dates(Tournoi t) {
        LocalDate premier = t.getJours().first();
        LocalDate dernier = t.getJours().last();
        if (premier.equals(dernier)) {
            return Formats.capitale(Formats.jourLong(premier));
        }
        return "Du " + Formats.jourLong(premier) + " au " + Formats.jourLong(dernier);
    }

    // ------------------------------------------------------------------
    // Général : alertes, chiffres clés, stratégie
    // ------------------------------------------------------------------

    /**
     * Alertes en clair : celles à traiter d'abord, puis celles à savoir. Les
     * joueurs sans club (ou sans points) sont regroupés en une seule ligne, comme
     * « complet » et « liste d'attente » d'un même tableau.
     */
    static List<LigneAlerte> alertes(List<Alerte> alertes) {
        List<LigneAlerte> aTraiter = new ArrayList<>();
        List<LigneAlerte> aSavoir = new ArrayList<>();
        Map<TypeAlerte, List<Alerte>> joueurs = new LinkedHashMap<>();
        // Tableau complet avec liste d'attente : une seule ligne pour les deux.
        Map<String, Alerte> attentes = new LinkedHashMap<>();
        List<String> complets = new ArrayList<>();
        for (Alerte a : alertes) {
            if (a.type() == TypeAlerte.LISTE_ATTENTE_NON_VIDE) {
                attentes.put(a.tableau(), a);
            } else if (a.type() == TypeAlerte.TABLEAU_COMPLET) {
                complets.add(a.tableau());
            }
        }
        for (Alerte a : alertes) {
            if (a.joueur() != null) {
                joueurs.computeIfAbsent(a.type(), x -> new ArrayList<>()).add(a);
                continue;
            }
            if (a.type() == TypeAlerte.LISTE_ATTENTE_NON_VIDE && complets.contains(a.tableau())) {
                continue; // dite avec « complet »
            }
            if (a.type() == TypeAlerte.TABLEAU_COMPLET && attentes.containsKey(a.tableau())) {
                int n = attentes.get(a.tableau()).nombre();
                aSavoir.add(aSavoir(PresentationPostePointage.libelleTableau(a.tableau()) + " : complet, liste d'attente",
                        a.nombre() + " inscrits pour " + a.seuil() + " places · " + n
                                + (n > 1 ? " joueurs attendent" : " joueur attend")
                                + " une place : à repêcher en cas de désistement."));
                continue;
            }
            (a.type().getGravite() == Gravite.A_TRAITER ? aTraiter : aSavoir).add(ligneAlerte(a));
        }
        for (Map.Entry<TypeAlerte, List<Alerte>> e : joueurs.entrySet()) {
            aTraiter.add(ligneJoueurs(e.getKey(), e.getValue()));
        }
        List<LigneAlerte> res = new ArrayList<>(aTraiter);
        res.addAll(aSavoir);
        return res;
    }

    private static LigneAlerte ligneAlerte(Alerte a) {
        TypeAlerte type = a.type();
        String tableau = PresentationPostePointage.libelleTableau(a.tableau());
        if (type == TypeAlerte.JUGE_ARBITRE_NON_DESIGNE) {
            return aTraiter("Juge-arbitre principal non désigné",
                    "Désignez-le dans « Informations sportives et techniques ».");
        }
        if (type == TypeAlerte.TABLEAU_SANS_HORAIRE) {
            return aTraiter(tableau + " : horaire non renseigné",
                    "Indiquez l'heure de début des matchs (et le pointage) dans « Tableaux ».");
        }
        if (type == TypeAlerte.TABLEAU_SOUS_SEUIL) {
            return aTraiter(tableau + " : sous le seuil de maintien",
                    a.nombre() + (a.nombre() > 1 ? " inscrits" : " inscrit") + " pour un minimum de " + a.seuil()
                            + " fixé dans votre règlement : le tableau peut être annulé ou regroupé.");
        }
        if (type == TypeAlerte.TABLEAU_COMPLET) {
            return aSavoir(tableau + " : complet", a.nombre() + " inscrits pour " + a.seuil() + " places.");
        }
        return aSavoir(tableau + " : liste d'attente",
                a.nombre() + (a.nombre() > 1 ? " joueurs attendent" : " joueur attend")
                        + " une place : à repêcher en cas de désistement.");
    }

    private static LigneAlerte ligneJoueurs(TypeAlerte type, List<Alerte> alertes) {
        int n = alertes.size();
        List<String> noms = new ArrayList<>();
        for (int i = 0; i < Math.min(n, JOUEURS_NOMMES); i++) {
            noms.add(PresentationPostePointage.nomAffiche(alertes.get(i).joueur()));
        }
        String liste = String.join(", ", noms) + (n > JOUEURS_NOMMES ? " et " + (n - JOUEURS_NOMMES)
                + (n - JOUEURS_NOMMES > 1 ? " autres" : " autre") : "") + ".";
        if (type == TypeAlerte.JOUEUR_SANS_CLUB) {
            return aTraiter(n + (n > 1 ? " joueurs sans club" : " joueur sans club"), liste);
        }
        return aTraiter(n + (n > 1 ? " joueurs sans points" : " joueur sans points"),
                liste + " Les points servent au tirage des poules.");
    }

    private static LigneAlerte aTraiter(String titre, String detail) {
        return new LigneAlerte(titre, detail, "À traiter", StatutPastille.ATTENTION);
    }

    private static LigneAlerte aSavoir(String titre, String detail) {
        return new LigneAlerte(titre, detail, "À savoir", StatutPastille.INFO);
    }

    /** « 3 alertes à traiter », « Aucune alerte à traiter ». */
    static String resumeAlertes(List<LigneAlerte> lignes) {
        long n = lignes.stream().filter(l -> l.statut() == StatutPastille.ATTENTION).count();
        if (n == 0) {
            return "Aucune alerte à traiter";
        }
        return n + (n > 1 ? " alertes à traiter" : " alerte à traiter");
    }

    /** Chiffres clés : joueurs, inscriptions, attente, payés, tables. */
    static List<Chiffre> chiffres(Tournoi t) {
        BilanRemplissageFinances bilan = new ServiceRemplissageFinances().calculer(t);
        return List.of(
                new Chiffre(String.valueOf(RemplissageTableaux.nombreDeParticipants(t)), "Joueurs inscrits"),
                new Chiffre(String.valueOf(bilan.inscrits()), "Inscriptions aux tableaux"),
                new Chiffre(String.valueOf(bilan.enAttente()), "En liste d'attente"),
                new Chiffre(bilan.payes() + " / " + bilan.inscrits(), "Inscriptions payées"),
                new Chiffre(t.getNombreDeTables() > 0 ? String.valueOf(t.getNombreDeTables()) : "—", "Tables"));
    }

    /**
     * Résumé de la prévision pour la stratégie du tournoi : une phrase par
     * journée (fin moyenne et risque de dépassement).
     */
    static List<String> resumePrevision(PrevisionTournoi prevision, StrategieOrdonnancement strategie) {
        List<String> res = new ArrayList<>();
        for (PrevisionTournoi.PrevisionJournee j : prevision.journees().values()) {
            Optional<StatistiquesSimulation> stats = j.pour(strategie);
            if (stats.isEmpty()) {
                continue;
            }
            StatistiquesSimulation s = stats.get();
            String phrase = PresentationPrevisions.libelleJour(j.jour()) + " : fin moyenne à "
                    + PresentationPrevisions.heureDuJour(s.finMoyenne(), j.jour());
            if (j.finVisee() != null) {
                phrase += " (fin visée " + PresentationPrevisions.heureDuJour(j.finVisee(), j.jour()) + ")";
            }
            res.add(phrase + " · risque de dépassement : "
                    + PresentationPrevisions.pourcent(s.probabiliteDebordement()) + ".");
        }
        if (res.isEmpty()) {
            res.add("Aucun tableau à simuler pour l'instant.");
        }
        return res;
    }

    // ------------------------------------------------------------------
    // Tableaux
    // ------------------------------------------------------------------

    /** Tableaux par jour, dans l'ordre du jour J (heure de début puis nom). */
    static List<JourneeTableaux> journees(Tournoi t) {
        Map<String, BilanRemplissageFinances.LigneTableau> bilan = new LinkedHashMap<>();
        for (BilanRemplissageFinances.LigneTableau l : new ServiceRemplissageFinances().calculer(t).tableaux()) {
            bilan.put(l.nom(), l);
        }
        int minimum = t.getConditionsMaintien() == null ? 0 : t.getConditionsMaintien().getMinimumInscrits();
        List<JourneeTableaux> res = new ArrayList<>();
        for (Map.Entry<LocalDate, List<Tableau>> e : PresentationJourJ.tableauxParJour(t).entrySet()) {
            List<LigneTableau> lignes = new ArrayList<>();
            for (Tableau tab : e.getValue()) {
                lignes.add(ligne(tab, bilan.get(tab.getNom()), minimum));
            }
            String titre = e.getKey() == null ? "Jour non défini (tableaux sans horaire)"
                    : PresentationJourJ.jour(e.getKey());
            res.add(new JourneeTableaux(e.getKey(), titre, lignes));
        }
        return res;
    }

    private static LigneTableau ligne(Tableau tab, BilanRemplissageFinances.LigneTableau b, int minimum) {
        int inscrits = tab.getInscrits().size();
        int attente = tab.getFileAttente().size();
        int payes = b == null ? 0 : b.payes();
        boolean limite = tab.getConfiguration().aLimiteDePlaces();
        int places = tab.getConfiguration().getPlacesMax();
        String etat;
        StatutPastille statut;
        if (tab.getStatut() != StatutTableau.INSCRIPTION) {
            etat = PresentationTournoiEnCours.statut(tab);
            statut = PresentationTournoiEnCours.pastille(tab);
        } else if (tab.getHoraires() == null || tab.getHoraires().getDebutMatchs() == null) {
            etat = "Sans horaire";
            statut = StatutPastille.ATTENTION;
        } else if (minimum > 0 && inscrits < minimum) {
            etat = "Sous le seuil (" + minimum + ")";
            statut = StatutPastille.ATTENTION;
        } else if (tab.estPlein()) {
            etat = "Complet";
            statut = StatutPastille.SUCCES;
        } else {
            etat = "Inscriptions ouvertes";
            statut = StatutPastille.NEUTRE;
        }
        return new LigneTableau(tab.getNom(), PresentationJourJ.horaires(tab),
                limite ? inscrits + " / " + places + " inscrits" : inscrits + (inscrits > 1 ? " inscrits" : " inscrit"),
                attente == 0 ? "Pas d'attente" : attente + " en attente",
                payes + " / " + inscrits + (inscrits > 1 ? " payés" : " payé"),
                etat, statut, limite && places > 0 ? Math.min(1.0, inscrits / (double) places) : -1);
    }

    // ------------------------------------------------------------------
    // Pointage (aperçu)
    // ------------------------------------------------------------------

    /** La liste du poste de pointage, sans filtre, limitée à {@link #LIMITE_APERCU_POINTAGE} joueurs. */
    static List<ListePointage.Ligne> apercuPointage(Tournoi t) {
        List<ListePointage.Ligne> toutes = new ListePointage(t).lignes("", null, ListePointage.FiltreEtat.TOUS);
        return toutes.size() > LIMITE_APERCU_POINTAGE ? toutes.subList(0, LIMITE_APERCU_POINTAGE) : toutes;
    }

    /** « 12 joueurs à pointer » ; « 150 premiers joueurs sur 412 ». */
    static String comptePointage(int affiches, int total) {
        if (total == 0) {
            return "Aucun joueur inscrit pour l'instant.";
        }
        if (affiches < total) {
            return affiches + " premiers joueurs sur " + total + ".";
        }
        return total + (total > 1 ? " joueurs à pointer" : " joueur à pointer");
    }

    // ------------------------------------------------------------------
    // Salle (aperçu)
    // ------------------------------------------------------------------

    /** « 16 tables » (et les tables d'échauffement s'il y en a). */
    static String resumeSalle(Tournoi t) {
        int n = t.getNombreDeTables();
        if (n <= 0) {
            return "Nombre de tables non renseigné";
        }
        String texte = n + (n > 1 ? " tables de jeu" : " table de jeu");
        int echauffement = t.getTablesEchauffement();
        if (echauffement > 0) {
            texte += " · " + echauffement + (echauffement > 1 ? " tables d'échauffement" : " table d'échauffement");
        }
        return texte + " · toutes libres tant que rien n'est lancé";
    }

    /** Ordre prévu des tableaux, journée par journée, d'après leurs horaires de début. */
    static List<JourneeSalle> ordrePrevu(Tournoi t) {
        List<JourneeSalle> res = new ArrayList<>();
        for (Map.Entry<LocalDate, List<Tableau>> e : PresentationJourJ.tableauxParJour(t).entrySet()) {
            if (e.getKey() == null) {
                continue; // sans horaire : pas d'ordre prévu (signalé dans les alertes)
            }
            List<Passage> passages = new ArrayList<>();
            int rang = 1;
            for (Tableau tab : e.getValue()) {
                LocalDateTime debut = tab.getHoraires().getDebutMatchs();
                int inscrits = tab.getInscrits().size();
                String detail = (debut == null ? "Début non renseigné" : "Début " + Formats.heure(debut.toLocalTime()))
                        + " · " + inscrits + (inscrits > 1 ? " joueurs" : " joueur");
                passages.add(new Passage(rang++, PresentationPostePointage.libelleTableau(tab.getNom()), detail));
            }
            res.add(new JourneeSalle(PresentationJourJ.jour(e.getKey()), passages));
        }
        return res;
    }

    // ------------------------------------------------------------------
    // À imprimer / à préparer
    // ------------------------------------------------------------------

    /** Documents : ceux qui existent (bouton), ceux du jour J, ceux à venir (sans bouton). */
    static List<Impression> impressions() {
        return List.of(
                new Impression("Règlement du tournoi",
                        "À afficher dans la salle et à garder à la table du juge-arbitre.",
                        Disponibilite.DISPONIBLE, Document.REGLEMENT, "Ouvrir le PDF"),
                new Impression("Feuille d'engagement",
                        "Le bulletin d'inscription du tournoi, pour les inscriptions sur place.",
                        Disponibilite.DISPONIBLE, Document.FEUILLE_ENGAGEMENT, "Ouvrir le PDF"),
                new Impression("Feuilles de poule et feuilles de match",
                        "Elles portent les noms des joueurs : elles s'impriment depuis l'écran du jour J, "
                                + "une fois le tirage au sort fait.",
                        Disponibilite.JOUR_J, null, ""),
                new Impression("Tableaux finaux",
                        "L'arbre de chaque tableau s'imprime depuis l'écran du jour J, après les poules.",
                        Disponibilite.JOUR_J, null, ""),
                new Impression("Listes de pointage en PDF",
                        "Une liste par tableau, triée par nom, avec les cases « pointé » et « payé ».",
                        Disponibilite.A_VENIR, null, ""),
                new Impression("Mise à jour des points avant le tirage",
                        "Import d'un fichier licence et points, avec aperçu avant application.",
                        Disponibilite.A_VENIR, null, ""));
    }
}
