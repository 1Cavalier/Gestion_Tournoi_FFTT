package com.pingmanager.ui.infra;

import com.pingmanager.core.OrdonnancementTableaux;
import com.pingmanager.core.ServiceCycleTournoi;
import com.pingmanager.core.reglement.LibellesTableau;
import com.pingmanager.model.AdresseTournoi;
import com.pingmanager.model.AireDeJeu;
import com.pingmanager.model.ArticleOptionnel;
import com.pingmanager.model.ConditionsMaintien;
import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.CouleurBalles;
import com.pingmanager.model.GrilleRecompenses;
import com.pingmanager.model.InfosReglement;
import com.pingmanager.model.AffectationJugeArbitre;
import com.pingmanager.model.GradeJugeArbitre;
import com.pingmanager.model.LotTables;
import com.pingmanager.model.RoleJugeArbitre;
import com.pingmanager.model.NiveauTournoi;
import com.pingmanager.model.OptionsReglement;
import com.pingmanager.model.Paiement;
import com.pingmanager.model.PhasePoints;
import com.pingmanager.model.PolitiqueBalles;
import com.pingmanager.model.PolitiqueForfaits;
import com.pingmanager.model.PolitiqueForfaits.Periode;
import com.pingmanager.model.PolitiqueForfaits.Regle;
import com.pingmanager.model.PolitiqueForfaits.Remboursement;
import com.pingmanager.model.QuotasTableaux;
import com.pingmanager.model.Recompense;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.TarifInscription;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.format.RegistreFormats;

import java.time.LocalDate;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * Le <b>vrai</b> Tournoi National B de Brunoy 2027, à l'état réel : <b>brouillon</b>,
 * sans numéro d'homologation ni inscrit. Repris du règlement du club (version
 * du 10 novembre 2026, 21 articles), qu'il régénère : samedi 10 et
 * dimanche 11 avril 2027 au 157 Route de Brie, 13 tableaux (7 le samedi dont le Toutes
 * séries dames en dernier, 6 le dimanche avec le Non numérotés), dotation de 3 390 €,
 * aucun remboursement, 20 tables + salle d'échauffement de 8, balles fournies par le
 * club, juge-arbitre Samuel LOOBUYCK (JA3) assisté de William PRIOU (JA2), contact
 * Serge BOULIER, inscriptions sur HelloAsso du 17 janvier 2027 à 10 h au 9 avril 2027 à 19 h,
 * salle ouverte dès 7 h.
 *
 * <p>Reste à compléter : le numéro d'homologation (délivré par la FFTT) et les numéros
 * de licence des juges-arbitres. La copie de démonstration, avec un faux numéro et des
 * personnes fictives, est {@link TournoiDemoBrunoyJourJ}.</p>
 */
public final class TournoiDemoBrunoy {

    public static final String NOM = "Tournoi National B de Brunoy 2027";
    /** Tableau dames (lettre G, dernier du samedi) : imposé aux joueuses ce jour-là. */
    public static final String DAMES = "G – Toutes séries dames";
    /** Tableau Non numérotés (lettre M, dernier du dimanche) : toutes séries, hors numérotés 1 à 999. */
    public static final String NON_NUMEROTES = "M – Non numérotés";

    private static final LocalDate SAMEDI = LocalDate.of(2027, 4, 10);
    private static final LocalDate DIMANCHE = LocalDate.of(2027, 4, 11);

    private TournoiDemoBrunoy() {
    }

    public static Tournoi construire() {
        Tournoi t = new Tournoi(NOM, "Brunoy", List.of(SAMEDI, DIMANCHE));

        // --- Général ---
        t.setNumeroAffiliationClub(DonneesDemo.CLUB_BRUNOY);
        t.setNiveau(NiveauTournoi.NATIONAL_B);
        t.setPeriode(SAMEDI, DIMANCHE);
        t.setAdresse(AdresseTournoi.de("157 Route de Brie", "91800", "Brunoy"));
        t.setCorrespondant(DonneesDemo.REFERENT_BRUNOY_NOM, DonneesDemo.REFERENT_BRUNOY_PRENOM,
                DonneesDemo.EMAIL_REFERENT_BRUNOY, null, null);
        t.setPhasePoints(PhasePoints.DEUXIEME);
        // Juges-arbitres désignés (29 octobre 2026) : le JA3 est principal (grade minimum en national B, IX.104).
        // Numéros de licence pas encore communiqués : à remplacer dans l'écran Général.
        t.ajouterJugeArbitre(new AffectationJugeArbitre("LOOBUYCK", "Samuel", null, "Licence à renseigner (S. Loobuyck)",
                GradeJugeArbitre.JA3, null, RoleJugeArbitre.PRINCIPAL));
        t.ajouterJugeArbitre(new AffectationJugeArbitre("PRIOU", "William", null, "Licence à renseigner (W. Priou)",
                GradeJugeArbitre.JA2, null, RoleJugeArbitre.ADJOINT));

        // --- Tableaux (règlement du club, article 4) ---
        // Une lettre par tableau, dans l'ordre du planning :
        // samedi A ≤ 600, B ≤ 1500, C ≤ 900, D ≤ 1800, E ≤ 1200, F ≤ 2000, G – Toutes séries dames (en dernier) ;
        // dimanche H ≤ 800, I ≤ 1600, J ≤ 1000, K ≤ 1900, L ≤ 1300, M – Non numérotés (hors n° 1 à 999).
        // Ordre et horaires : ordonnancement automatique (un tableau sur deux par jour, faible /
        // moyen alternés, dames en dernier), écarts 1 h puis 1 h 30, à partir de 8 h 30 :
        // 8 h 30, 9 h 30, 11 h, 12 h, 13 h 30, 14 h 30, 16 h. Fin du pointage 30 min avant.
        // Dotation : vainqueur, finaliste, chaque ½ finaliste, chaque ¼ de finaliste.
        t.ajouterTableau(points("A", 600, gr(60, 30, 15, 10)));
        t.ajouterTableau(points("H", 800, gr(70, 35, 20, 10)));
        t.ajouterTableau(points("C", 900, gr(70, 35, 20, 10)));
        t.ajouterTableau(points("J", 1000, gr(80, 40, 20, 10)));
        t.ajouterTableau(points("E", 1200, gr(90, 45, 25, 15)));
        t.ajouterTableau(points("L", 1300, gr(100, 50, 25, 15)));
        t.ajouterTableau(points("B", 1500, gr(110, 55, 30, 15)));
        t.ajouterTableau(points("I", 1600, gr(120, 60, 30, 15)));
        t.ajouterTableau(points("D", 1800, gr(130, 65, 35, 20)));
        // Tableaux ≤ 1900, ≤ 2000 et Non numérotés : 10 € en ligne (9 € pour les autres).
        t.ajouterTableau(tableau("K", base(60).pointMax(1900).tarif(DIX_EUROS), gr(140, 70, 35, 20)));
        t.ajouterTableau(tableau("F", base(60).pointMax(2000).tarif(DIX_EUROS), gr(150, 75, 40, 20)));
        t.ajouterTableau(tableau(NON_NUMEROTES, base(60).toutesSeries().exclusionRangNational(999).tarif(DIX_EUROS),
                gr(200, 100, 50, 20)));
        // Tableau dames : dotation en part des inscriptions (50 / 30 / 10 / 10 %), pas de ¼ de finale doté.
        t.ajouterTableau(tableau(DAMES, base(48).toutesSeries().sexeRequis(Sexe.FEMME),
                GrilleRecompenses.builder()
                        .place(1, Recompense.partInscriptions(50)).place(2, Recompense.partInscriptions(30))
                        .plage(3, 4, Recompense.partInscriptions(10)).build()));
        OrdonnancementTableaux.appliquer(OrdonnancementTableaux.proposer(t.getTableaux(), List.of(SAMEDI, DIMANCHE),
                        LocalTime.of(8, 30), List.of(Duration.ofMinutes(60), Duration.ofMinutes(90)), true),
                Duration.ofMinutes(30), Duration.ofMinutes(60));
        t.setDotationGlobale(new ServiceCycleTournoi().calculerDotationGlobale(t));

        // --- Échéances, balles, aire de jeu ---
        t.setInfosReglement(InfosReglement.builder()
                .ouvertureInscriptions(LocalDateTime.of(2027, 1, 17, 10, 0))
                .clotureInscriptions(LocalDateTime.of(2027, 4, 9, 19, 0))
                .politiqueBalles(PolitiqueBalles.FOURNIES_PAR_ORGANISATEUR)
                .couleurBalles(CouleurBalles.BLANCHE)
                .marqueBalles("Donic P40+ 3 étoiles")
                .aireDeJeu(new AireDeJeu(12.0, 6.0, 500))
                .build());

        // --- Règles d'inscription ---
        // 2 tableaux par jour ; le samedi, 3 pour les joueuses avec le Toutes séries dames, qui leur
        // est obligatoire ce jour-là. Jamais deux tableaux qui se suivent.
        t.setQuotasTableaux(QuotasTableaux.parJour(Map.of(
                        SAMEDI, new QuotasTableaux.Quota(2, 3),
                        DIMANCHE, new QuotasTableaux.Quota(2, 2)))
                .avecTableauBonusJoueuses(DAMES)
                .avecTableauBonusObligatoire(true)
                .avecTableauxConsecutifsInterdits(true));
        // Moins de 50 % des places occupées : la dotation peut être réduite (sans taux fixé).
        t.setConditionsMaintien(new ConditionsMaintien(0, null,
                ConditionsMaintien.ModeReduction.REMPLISSAGE, 50, 0, 0));
        // Aucun remboursement, quel que soit le moment du forfait.
        t.setPolitiqueForfaits(new PolitiqueForfaits(Map.of(
                Periode.PENDANT_INSCRIPTIONS, new Regle(Remboursement.AUCUN, 0, false),
                Periode.APRES_CLOTURE, new Regle(Remboursement.AUCUN, 0, false),
                Periode.JOUR_J, new Regle(Remboursement.AUCUN, 0, false)), false));

        // --- Informations sportives ---
        // 20 tables de compétition (marque non précisée) + une salle d'échauffement de 8 tables.
        t.setLotsTables(List.of(new LotTables(20, null, null)));
        t.setTablesEchauffement(8);

        // --- Complémentaire : règlement généré, paiement, conditions ---
        t.setPaiement(new Paiement(Paiement.Mode.HELLOASSO,
                "https://www.helloasso.com/associations/cttb-brunoy/evenements/tournoi-national-b-2027"));
        t.setOptionsReglement(new OptionsReglement(OptionsReglement.Mode.GENERE, List.of(
                ArticleOptionnel.de(ArticleOptionnel.Type.DROIT_IMAGE),
                new ArticleOptionnel(ArticleOptionnel.Type.BUVETTE, Map.of(ArticleOptionnel.HORAIRES,
                        "dès 7 h 30 le matin et jusqu'à 23 h, sans interruption le midi")),
                new ArticleOptionnel(ArticleOptionnel.Type.LIBRE, Map.of(
                        ArticleOptionnel.TITRE, "Remise des récompenses",
                        ArticleOptionnel.TEXTE, "Les dotations sont versées en espèces ou par chèque ; une coupe "
                                + "récompense chaque vainqueur et une médaille chaque finaliste et demi-finaliste. "
                                + "Un joueur absent à la remise perd sa dotation, sauf accord du juge-arbitre.")),
                new ArticleOptionnel(ArticleOptionnel.Type.OUVERTURE_SALLE, Map.of(ArticleOptionnel.HEURE, "7h")))));
        t.setAccepteLicencePromotionnelle(false);
        t.setAccepteJoueursEtrangers(false);
        t.setCategoriesExclues(List.of(com.pingmanager.model.CategorieAge.POUSSIN)); // choix du club
        return t;
    }

    // ------------------------------------------------------------------

    /** 10 € en ligne comme sur place. */
    private static final TarifInscription DIX_EUROS = new TarifInscription(10, 10);

    private static ConfigurationTableau.Builder base(int places) {
        return ConfigurationTableau.depuisFormat(RegistreFormats.formatParDefaut())
                .placesMax(places)
                .tailleMaxListeAttente(10)
                .tarif(new TarifInscription(9, 10));
    }

    private static Tableau points(String nom, int max, GrilleRecompenses g) {
        return tableau(nom, base(60).pointMax(max), g);
    }

    private static Tableau tableau(String nom, ConfigurationTableau.Builder b, GrilleRecompenses g) {
        Tableau tab = new Tableau(nom, b.recompenses(g).build());
        tab.setType(LibellesTableau.type(tab.getConfiguration()));
        return tab;
    }

    /** Vainqueur, finaliste, chaque demi-finaliste, chaque quart de finaliste (en euros). */
    private static GrilleRecompenses gr(int v, int f, int d, int q) {
        return GrilleRecompenses.builder()
                .place(1, Recompense.argent(v)).place(2, Recompense.argent(f))
                .plage(3, 4, Recompense.argent(d))
                .plage(5, 8, Recompense.argent(q))
                .build();
    }
}
