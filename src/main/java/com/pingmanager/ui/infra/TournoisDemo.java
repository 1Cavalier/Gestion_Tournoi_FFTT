package com.pingmanager.ui.infra;

import com.pingmanager.core.EligibiliteTournoi;
import com.pingmanager.core.JoueurDepuisLicence;
import com.pingmanager.core.RegistreClubs;
import com.pingmanager.core.ServiceCycleTournoi;
import com.pingmanager.core.ServiceInscription;
import com.pingmanager.core.reglement.LibellesTableau;
import com.pingmanager.model.AdresseTournoi;
import com.pingmanager.model.AffectationJugeArbitre;
import com.pingmanager.model.ArticleOptionnel;
import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.Coordonnees;
import com.pingmanager.model.FicheLicence;
import com.pingmanager.model.GradeJugeArbitre;
import com.pingmanager.model.GrilleRecompenses;
import com.pingmanager.model.HorairesTableau;
import com.pingmanager.model.InfosReglement;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.LotTables;
import com.pingmanager.model.NiveauTournoi;
import com.pingmanager.model.OptionsReglement;
import com.pingmanager.model.Paiement;
import com.pingmanager.model.PhasePoints;
import com.pingmanager.model.PolitiqueBalles;
import com.pingmanager.model.QuotasTableaux;
import com.pingmanager.model.Recompense;
import com.pingmanager.model.RoleJugeArbitre;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.StatutTournoi;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.TarifInscription;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.format.RegistreFormats;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Tournois <b>publiés</b> de la démo, pour l'onglet Tournois : le National B de Brunoy
 * publié et rempli par des joueurs fictifs, et cinq tournois inventés ailleurs, à
 * d'autres niveaux (régional Île-de-France, départemental du Loiret, national A à Lyon,
 * régional des Hauts-de-France, promotionnel à Rennes en cours le 2 novembre 2026).
 *
 * <p>Toutes les inscriptions passent par {@link ServiceInscription} : quotas, tableau
 * dames imposé, tableaux consécutifs, points, catégories, capacité et liste d'attente
 * sont respectés. Clubs, juges-arbitres, numéros d'homologation et liens de paiement
 * sont <b>fictifs</b>.</p>
 */
public final class TournoisDemo {

    /** Numéro d'homologation fictif de Brunoy (le vrai sera délivré par la FFTT). */
    public static final String HOMOLOGATION_BRUNOY_DEMO = "DÉMO-NB-2027-091 (fictif)";

    private TournoisDemo() {
    }

    // ==================================================================
    // Brunoy : publié et rempli
    // ==================================================================

    /**
     * Objectifs de remplissage du National B (inscrits + attente) : complets avec liste
     * d'attente (A, H), complet sans attente (C), à moitié (B, E, L…), presque vides (F, K, M).
     */
    private static final Map<String, Integer> CIBLES_BRUNOY = Map.ofEntries(
            Map.entry("A", 70), Map.entry("B", 31), Map.entry("C", 60), Map.entry("D", 21),
            Map.entry("E", 44), Map.entry("F", 5), Map.entry("G", 17),
            Map.entry("H", 66), Map.entry("I", 27), Map.entry("J", 50), Map.entry("K", 8),
            Map.entry("L", 36), Map.entry("M", 3));

    /** National B de Brunoy, publié (n° d'homologation fictif) et rempli par des joueurs fictifs. */
    public static Tournoi brunoyPublieEtRempli(List<FicheLicence> joueurs) {
        Tournoi t = TournoiDemoBrunoy.construire();
        t.setNumeroHomologation(HOMOLOGATION_BRUNOY_DEMO);
        t.setAdresse(t.getAdresse().avecCoordonnees(new Coordonnees(48.6929, 2.5115)));
        publier(t);
        remplir(t, joueurs, CIBLES_BRUNOY, 7L, null);
        return t;
    }

    /**
     * Remplit chaque tableau jusqu'à sa cible (inscrits + attente), joueuses d'abord au
     * tableau dames (imposé), en tirant des joueurs au hasard (graine fixe) et en passant
     * par {@link ServiceInscription} : un refus (quota, tableaux consécutifs, points,
     * catégorie…) fait simplement passer au joueur suivant.
     */
    static void remplir(Tournoi t, List<FicheLicence> joueurs, Map<String, Integer> cibles, long graine,
                        java.util.function.Predicate<FicheLicence> admis) {
        ServiceInscription service = ServiceInscription.sansContrainteSuccession();
        List<Joueur> candidats = new ArrayList<>();
        for (FicheLicence f : joueurs) {
            if (admis == null || admis.test(f)) {
                candidats.add(JoueurDepuisLicence.creer(f, t));
            }
        }
        Random r = new Random(graine);
        List<Tableau> ordre = new ArrayList<>(t.getTableaux());
        // Le tableau des joueuses d'abord (il conditionne leurs autres tableaux du jour).
        String dames = t.getQuotasTableaux().getTableauBonusJoueuses();
        ordre.sort(java.util.Comparator.comparing((Tableau x) -> x.getNom().equals(dames) ? 0 : 1));
        for (Tableau tab : ordre) {
            Integer cible = cibles.get(cle(tab.getNom()));
            if (cible == null) {
                continue;
            }
            List<Joueur> melange = new ArrayList<>(candidats);
            Collections.shuffle(melange, r);
            for (Joueur j : melange) {
                if (tab.getInscrits().size() + tab.getFileAttente().size() >= cible) {
                    break;
                }
                service.inscrire(t, j, tab);
            }
        }
    }

    /** « A », ou la lettre avant le tiret (« G – Toutes séries dames » → « G »), ou le nom. */
    private static String cle(String nom) {
        int i = nom.indexOf(" – ");
        return i > 0 ? nom.substring(0, i) : nom;
    }

    private static void publier(Tournoi t) {
        t.changerStatut(StatutTournoi.HOMOLOGUE);
        t.changerStatut(StatutTournoi.PUBLIE);
    }

    // ==================================================================
    // Autres tournois publiés
    // ==================================================================

    /**
     * @return les tournois de démo hors Brunoy, par nom de fichier (sans extension)
     */
    public static Map<String, Tournoi> autres(List<FicheLicence> joueurs, RegistreClubs clubs, FicheLicence quentin) {
        EligibiliteTournoi elig = new EligibiliteTournoi(clubs);
        Map<String, Tournoi> m = new LinkedHashMap<>();
        m.put("Demo_Regional_Creteil_2026", creteil(joueurs, elig));
        m.put("Demo_Departemental_Loiret_2027", loiret(joueurs, elig));
        m.put("Demo_National_A_Lyon_2027", lyon(joueurs, elig));
        m.put("Demo_Regional_Lille_2027", lille(joueurs, elig));
        Tournoi rennes = rennes(joueurs, elig);
        if (quentin != null) {
            // Quentin y est inscrit (tournoi en cours le 2 novembre 2026) : visible dans « Mes tournois ».
            Joueur q = JoueurDepuisLicence.creer(quentin, rennes);
            ServiceInscription service = ServiceInscription.sansContrainteSuccession();
            rennes.getTableaux().stream().filter(x -> x.getNom().startsWith("B")).findFirst()
                    .ifPresent(x -> { service.inscrire(rennes, q, x); x.marquerPaye(q); });
        }
        m.put("Demo_Promotionnel_Rennes_2026", rennes);
        return m;
    }

    /**
     * Régional Île-de-France, samedi 5 décembre 2026 : ouvert à Quentin, mêmes règles que Brunoy.
     * Le tableau D (toutes séries) est complet avec 3 joueurs en attente sur 6 : on peut y essayer
     * l'inscription en liste d'attente (rien à payer tant qu'on n'est pas repêché).
     */
    private static Tournoi creteil(List<FicheLicence> joueurs, EligibiliteTournoi elig) {
        LocalDate j = LocalDate.of(2026, 12, 5);
        Tournoi t = base("Tournoi régional de Créteil", "Créteil Ping", "08940106", NiveauTournoi.REGIONAL,
                List.of(j), AdresseTournoi.de("12 avenue des Sports", "94000", "Créteil"),
                new Coordonnees(48.7837, 2.4545), "R-IDF-2026-118");
        ajouter(t, "A – ≤ 799 pts", j, 9, 0, 32, b().pointMax(799), 8, gr(60, 30, 15));
        ajouter(t, "B – ≤ 1399 pts", j, 10, 30, 32, b().pointMax(1399), 8, gr(90, 45, 20));
        ajouter(t, "C – ≤ 1099 pts", j, 12, 0, 32, b().pointMax(1099), 8, gr(70, 35, 15));
        ajouter(t, "D – Toutes séries", j, 13, 30, 24, b().toutesSeries(), 10, gr(150, 75, 35));
        ajouter(t, "E – Toutes séries dames", j, 15, 0, 16, b().toutesSeries().sexeRequis(Sexe.FEMME), 8, gr(60, 30, 15));
        t.setQuotasTableaux(QuotasTableaux.identiques(2, 3).avecTableauBonusJoueuses("E – Toutes séries dames")
                .avecTableauBonusObligatoire(true).avecTableauxConsecutifsInterdits(true));
        finaliser(t, LocalDateTime.of(2026, 10, 1, 0, 0), LocalDateTime.of(2026, 12, 3, 20, 0));
        remplir(t, joueurs, Map.of("A", 34, "B", 14, "C", 20, "D", 27, "E", 5), 11L,
                f -> elig.evaluer(t, f).estAccessible());
        return t;
    }

    /** Départemental du Loiret, dimanche 17 janvier 2027 : grisé pour un licencié de l'Essonne. */
    private static Tournoi loiret(List<FicheLicence> joueurs, EligibiliteTournoi elig) {
        LocalDate j = LocalDate.of(2027, 1, 17);
        Tournoi t = base("Tournoi départemental du Loiret", "Orléans Loiret Ping", "07450115",
                NiveauTournoi.DEPARTEMENTAL, List.of(j), AdresseTournoi.de("5 rue du Stade", "45000", "Orléans"),
                new Coordonnees(47.9029, 1.9093), "D-45-2027-007");
        ajouter(t, "A – ≤ 899 pts", j, 9, 0, 24, b().pointMax(899), 7, gr(40, 20, 10));
        ajouter(t, "B – ≤ 1299 pts", j, 11, 0, 24, b().pointMax(1299), 7, gr(50, 25, 10));
        ajouter(t, "C – Toutes séries", j, 13, 30, 24, b().toutesSeries(), 8, gr(80, 40, 20));
        ajouter(t, "D – Dames", j, 15, 0, 12, b().toutesSeries().sexeRequis(Sexe.FEMME), 7, gr(40, 20, 10));
        t.setQuotasTableaux(QuotasTableaux.identiques(2, 2));
        finaliser(t, LocalDateTime.of(2026, 11, 1, 0, 0), LocalDateTime.of(2027, 1, 15, 20, 0));
        remplir(t, joueurs, Map.of("A", 6, "B", 4, "C", 3, "D", 1), 13L, f -> elig.evaluer(t, f).estAccessible());
        return t;
    }

    /** National A de Lyon, 20 et 21 février 2027, dotation ≥ 5 000 €. */
    private static Tournoi lyon(List<FicheLicence> joueurs, EligibiliteTournoi elig) {
        LocalDate sa = LocalDate.of(2027, 2, 20);
        LocalDate di = LocalDate.of(2027, 2, 21);
        Tournoi t = base("Open national A de Lyon", "Villeurbanne Ping", "10690116", NiveauTournoi.NATIONAL_A,
                List.of(sa, di), AdresseTournoi.de("30 rue du Palais des Sports", "69007", "Lyon"),
                new Coordonnees(45.7334, 4.8366), "NA-2027-012");
        t.retirerJugeArbitre("Licence fictive (JA)"); // un JA national est exigé en national A (IX.104)
        t.ajouterJugeArbitre(new AffectationJugeArbitre("DORIAN", "Hélène", null, "Licence fictive (JAN)",
                GradeJugeArbitre.JA_NATIONAL, null, RoleJugeArbitre.PRINCIPAL));
        ajouter(t, "A – ≤ 1199 pts", sa, 9, 0, 48, b().pointMax(1199), 10, gr(300, 150, 75));
        ajouter(t, "B – ≤ 1799 pts", sa, 12, 0, 48, b().pointMax(1799), 12, gr(500, 250, 120));
        ajouter(t, "C – Dames toutes séries", sa, 15, 0, 32, b().toutesSeries().sexeRequis(Sexe.FEMME), 12, gr(600, 300, 150));
        ajouter(t, "D – ≤ 1499 pts", di, 9, 0, 48, b().pointMax(1499), 10, gr(400, 200, 100));
        ajouter(t, "E – Toutes séries", di, 12, 0, 64, b().toutesSeries(), 15, gr(1500, 750, 375));
        t.setQuotasTableaux(QuotasTableaux.identiques(2, 2));
        finaliser(t, LocalDateTime.of(2026, 10, 15, 0, 0), LocalDateTime.of(2027, 2, 18, 20, 0));
        remplir(t, joueurs, Map.of("A", 20, "B", 18, "C", 6, "D", 24, "E", 30), 17L, null);
        return t;
    }

    /** Régional des Hauts-de-France, 13 mars 2027 : grisé pour un licencié d'Île-de-France. */
    private static Tournoi lille(List<FicheLicence> joueurs, EligibiliteTournoi elig) {
        LocalDate j = LocalDate.of(2027, 3, 13);
        Tournoi t = base("Tournoi régional de Lille", "Lille Métropole Ping", "09590117", NiveauTournoi.REGIONAL,
                List.of(j), AdresseTournoi.de("2 boulevard des Sports", "59000", "Lille"),
                new Coordonnees(50.6292, 3.0573), "R-HDF-2027-031");
        ajouter(t, "A – ≤ 999 pts", j, 9, 0, 32, b().pointMax(999), 8, gr(60, 30, 15));
        ajouter(t, "B – ≤ 1499 pts", j, 11, 0, 32, b().pointMax(1499), 8, gr(90, 45, 20));
        ajouter(t, "C – Toutes séries", j, 13, 30, 32, b().toutesSeries(), 10, gr(150, 75, 35));
        ajouter(t, "D – Dames", j, 15, 0, 16, b().toutesSeries().sexeRequis(Sexe.FEMME), 8, gr(60, 30, 15));
        finaliser(t, LocalDateTime.of(2026, 12, 1, 0, 0), LocalDateTime.of(2027, 3, 11, 20, 0));
        remplir(t, joueurs, Map.of("A", 3, "B", 2, "C", 1), 19L, f -> elig.evaluer(t, f).estAccessible());
        return t;
    }

    /** Promotionnel de Rennes, 1er et 2 novembre 2026 : en cours, Quentin y est inscrit. */
    private static Tournoi rennes(List<FicheLicence> joueurs, EligibiliteTournoi elig) {
        LocalDate di = LocalDate.of(2026, 11, 1);
        LocalDate lu = LocalDate.of(2026, 11, 2);
        Tournoi t = base("Tournoi promotionnel de la Toussaint", "Rennes Ping Loisirs", "04350118",
                NiveauTournoi.PROMOTIONNEL, List.of(di, lu), AdresseTournoi.de("8 rue de la Salle", "35000", "Rennes"),
                new Coordonnees(48.1173, -1.6778), "P-BRE-2026-044");
        t.setAccepteLicencePromotionnelle(true);
        ajouter(t, "A – Loisirs et ≤ 799 pts", di, 10, 0, 32, b().pointMax(799), 5, lots());
        ajouter(t, "B – Toutes séries", di, 14, 0, 32, b().toutesSeries(), 6, lots());
        ajouter(t, "C – Double surprise", lu, 10, 0, 32, b().toutesSeries(), 5, lots());
        finaliser(t, LocalDateTime.of(2026, 9, 15, 0, 0), LocalDateTime.of(2026, 10, 30, 20, 0));
        remplir(t, joueurs, Map.of("A", 18, "B", 24, "C", 20), 23L, null);
        return t;
    }

    // ------------------------------------------------------------------

    static Tournoi base(String nom, String club, String numClub, NiveauTournoi niveau, List<LocalDate> jours,
                                AdresseTournoi adresse, Coordonnees pos, String homologation) {
        Tournoi t = new Tournoi(nom, club, jours);
        t.setNumeroAffiliationClub(numClub);
        t.setNiveau(niveau);
        t.setPeriode(jours.get(0), jours.get(jours.size() - 1));
        t.setAdresse(adresse.avecCoordonnees(pos));
        t.setNumeroHomologation(homologation + " (fictif)");
        t.setPhasePoints(PhasePoints.PREMIERE);
        t.setCorrespondant("ORGANISATEUR", "Démo", "tournoi@" + numClub + ".demo", null, null);
        t.ajouterJugeArbitre(new AffectationJugeArbitre("JUGE", "Fictif", null, "Licence fictive (JA)",
                GradeJugeArbitre.JA3, null, RoleJugeArbitre.PRINCIPAL));
        t.setLotsTables(List.of(new LotTables(16, "Cornilleau", "Bleue")));
        t.setPaiement(new Paiement(Paiement.Mode.HELLOASSO,
                "https://www.helloasso.com/associations/exemple-" + numClub + "/evenements/tournoi"));
        t.setOptionsReglement(new OptionsReglement(OptionsReglement.Mode.GENERE,
                List.of(ArticleOptionnel.de(ArticleOptionnel.Type.DROIT_IMAGE))));
        return t;
    }

    static void finaliser(Tournoi t, LocalDateTime ouverture, LocalDateTime cloture) {
        t.setInfosReglement(InfosReglement.builder()
                .ouvertureInscriptions(ouverture)
                .clotureInscriptions(cloture)
                .politiqueBalles(PolitiqueBalles.FOURNIES_PAR_ORGANISATEUR)
                .build());
        t.setDotationGlobale(new ServiceCycleTournoi().calculerDotationGlobale(t));
        publier(t);
    }

    static ConfigurationTableau.Builder b() {
        return ConfigurationTableau.depuisFormat(RegistreFormats.formatParDefaut()).tailleMaxListeAttente(6);
    }

    static void ajouter(Tournoi t, String nom, LocalDate jour, int h, int min, int places,
                                ConfigurationTableau.Builder b, double prix, GrilleRecompenses g) {
        Tableau tab = new Tableau(nom, b.placesMax(places).tarif(new TarifInscription(prix, prix + 2))
                .recompenses(g).build());
        tab.setType(LibellesTableau.type(tab.getConfiguration()));
        LocalDateTime debut = LocalDateTime.of(jour, LocalTime.of(h, min));
        tab.setHoraires(new HorairesTableau(debut.minusMinutes(60), debut.minusMinutes(20), debut));
        t.ajouterTableau(tab);
    }

    static GrilleRecompenses gr(int v, int f, int d) {
        return GrilleRecompenses.builder().place(1, Recompense.argent(v)).place(2, Recompense.argent(f))
                .plage(3, 4, Recompense.argent(d)).build();
    }

    private static GrilleRecompenses lots() {
        return GrilleRecompenses.builder().place(1, Recompense.autre("Raquette"))
                .place(2, Recompense.autre("Bon d'achat")).build();
    }
}
