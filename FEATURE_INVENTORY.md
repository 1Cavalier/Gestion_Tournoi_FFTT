# FEATURE_INVENTORY — contrat de la refonte de nuit du 13 novembre 2026

Référence : `main` au commit `d54a72d` (1 480 tests verts). Ce document liste tout ce
qui doit rester identique après la refonte. Il se lit en trois niveaux :

1. **Sections 1 à 9** : les fonctionnalités, par domaine, avec leur point de définition
   et la colonne « Preuve » (cochée en phase 5 : ☑ suivi du test, du filet S2/S3/S4 ou de la vérification manuelle « M: »).
2. **Annexe A** (générée) : toutes les énumérations et leurs valeurs, toutes les
   constantes, toutes les valeurs initiales, toutes les clés JSON, avec fichier et ligne.
3. **Annexe B** (générée) : les 367 classes, leur rôle, leur surface publique, leur
   couverture de test. **Annexe C** (générée) : les commandes et champs de chaque écran.

PingManager est une application de bureau JavaFX : il n'y a **ni argument de ligne de
commande, ni fichier de configuration externe**. Les « options » sont donc les réglages
saisis à l'écran (portés par le modèle et persistés en JSON), les constantes par défaut,
les formats de fichiers lus et écrits, et les sorties (PDF, HTML, CSV, ICS, console).

Filets de sécurité outillés (hors dépôt, voir `REFACTOR_LOG.md`) :
- **S1** : les 1 480 tests JUnit d'origine, jamais modifiés ni affaiblis ;
- **S2** : la surface publique (`javap -protected` de toutes les classes) comparée à la référence ;
- **S3** : les sorties de référence (JSON des tournois de démo, règlement, feuilles PDF en texte,
  pages HTML du suivi, exports CSV et ICS, simulation à graine fixe) comparées octet par octet ;
- **S4** : le parcours automatique de l'interface sous écran virtuel (arbre des composants de
  chaque écran atteint) comparé à la référence.

Chemins relatifs à `src/main/java/com/pingmanager/`.

## 1. Lancement, données, environnement

| ID | Élément | Défini dans | Preuve |
|---|---|---|---|
| F-001 | Point d'entrée `com.pingmanager.ui.Lanceur` (ne dérive pas d'`Application`, lançable en `java -jar`) ; `mvn clean javafx:run` ; propriété Maven `app.mainClass` | `ui/Lanceur.java:15`, `pom.xml` | ☑ M: lancement réel sous écran virtuel par `com.pingmanager.ui.Lanceur` (écran de connexion affiché) ; `pom.xml` inchangé |
| F-002 | Fenêtre 1040 × 700, minimum 820 × 560, titre « PingManager », thème `theme.css` clair/sombre | `ui/AppPingManager.java:30`, `ui/ecran/Theme.java:20` | ☑ S4 (fenêtre, thème clair) ; `AppPingManager` et `Theme` non modifiés |
| F-003 | Enchaînement connexion → coquille → déconnexion ; fermeture propre (`contexte.fermer()`) | `ui/AppPingManager.java:24` | ☑ S4 (connexion puis coquille) ; `AppPingManager` non modifié |
| F-004 | Dossier de données : `$XDG_DATA_HOME/pingmanager` si défini et non vide, sinon `~/.pingmanager/pingmanager` | `persistence/ServiceFichierTournoi.java:104` | ☑ T: ServiceFichierTournoiTest ; `ServiceFichierTournoi` non modifié |
| F-005 | Fichier applicatif `pingmanager-application.json` (version 1) : clubs, comptes, accès, récents | `persistence/DepotApplication.java:55` | ☑ T: DepotApplicationTest, FaillesSecuritePersistanceTest |
| F-006 | Un fichier `.ptt.json` par tournoi (version 1), écriture atomique par fichier `.tmp` puis renommage | `persistence/DepotTournoi.java:30`, `persistence/TournoiMapper.java:51` | ☑ T: RoundTripTournoiTest, RoundTripPhaseFinaleEnCoursTest ; S3 (JSON des deux tournois de démo, écrit puis relu) |
| F-007 | Autosave `.autosave.json` toutes les 15 min, marqueur `.cloture`, détection d'arrêt anormal, reprise proposée par écouteur (défaut sûr : ignorer) | `persistence/ServiceAutosave.java:36`, `persistence/ServiceFichierTournoi.java:40` | ☑ T: RepriseSessionTest, ServiceFichierTournoiTest |
| F-008 | Données de démo recréées quand `VERSION_DEMO` change (`.demo-brunoy-v19`), autres tournois archivés dans `tournois/archives` ; mémo des comptes de démo sur la console | `ui/ContexteApplication.java:66`, `ui/infra/TournoisDemo.java:26`, `ui/infra/DonneesDemo.java:20` | ☑ T: TournoisDemoTest, TournoiDemoBrunoyTest, TournoiDemoBrunoyJourJTest ; S3 (mémo) ; S4 (démo créée à chaque parcours) |
| F-009 | Variables d'environnement de la synthèse vocale Windows `PINGMANAGER_SSML`, `PINGMANAGER_VOIX`, `PINGMANAGER_DEBIT` (passées à PowerShell), limites 60 s et 15 s | `ui/infra/SyntheseVocaleWindows.java:34` | ☑ T: SyntheseVocaleWindowsTest, SyntheseVocaleWindowsLimitesTest ; classe non modifiée |
| F-010 | Propriétés système `sun.net.httpserver.maxReqTime` (5), `maxRspTime` (15), `idleInterval` (30) posées si absentes | `ui/diffusion/ServeurSuiviPublic.java:36` | ☑ T: ServeurSuiviPublicTest, ServeurSuiviSecuriteTest ; classe non modifiée |
| F-011 | Lecture tolérante des anciens fichiers JSON (champs facultatifs, valeurs par défaut à la lecture) | `persistence/TournoiMapper.java:51`, `persistence/ConfigMapper.java:40` | ☑ T: ConvocationsSmsRetrocompatibiliteTest, ReglementFigeCorrompuTest, RoundTripTournoiTest |

## 2. Comptes, clubs, droits

| ID | Élément | Défini dans | Preuve |
|---|---|---|---|
| F-020 | Connexion, création de compte, mot de passe haché PBKDF2-HMAC-SHA256 (120 000 itérations, sel 16 octets, clé 256 bits, préfixe `pbkdf2`) | `core/ServiceAuthentification.java:27`, `ui/infra/HachageMotDePassePBKDF2.java:22` | ☑ T: FaillesSecuriteHachageTest, CompteEtClubTest ; S4 (connexion des deux comptes de démo) |
| F-021 | Inscription d'un organisateur : code envoyé à l'email officiel du club, validité 30 min, motifs `CODE_ENVOYE / CLUB_INTROUVABLE / EMAIL_DEJA_UTILISE`, confirmation `VERIFIE / CODE_INCORRECT / CODE_EXPIRE` | `core/ServiceComptesOrganisateurs.java:39` | ☑ T: ServiceComptesOrganisateursTest |
| F-022 | Liaison d'une licence au compte (démarche 1) et rattachement à un club (démarche 2, code 30 min) | `core/ServiceLiaisonLicence.java:23`, `core/ServiceLiaisonClub.java:39` | ☑ T: FaillesSecuriteLiaisonsTest, CompteEtClubTest |
| F-023 | Niveaux d'accès au club (`NiveauAcces`), habilitation exigée à la création et à chaque transition (`SecurityException`) | `model/AccesClub.java:22`, `core/ServiceTournois.java:35` | ☑ T: CablagePreTournoiTest, FaillesSecuriteContexteTest |
| F-024 | Compte de supervision (« ultra admin »), page Supervision | `ui/ContexteApplication.java:66`, `ui/ecran/PageSupervision.java:30` | ☑ T: ContexteSupervisionTest ; S4 (compte de supervision) |
| F-025 | Page « Mon profil », agenda du compte (titre ≤ 120, note ≤ 2000, motifs `ResultatAgenda.Motif`), export `.ics` (RFC 5545, `agenda-pingmanager.ics`) | `ui/ecran/PageCompte.java:41`, `core/ServiceAgenda.java:29`, `core/ExportIcs.java:30` | ☑ T: ServiceAgendaTest, ExportIcsTest, PresentationProfilTest, PresentationAgendaTest ; S4 (Mon profil) |
| F-026 | Ports et implémentations : `EnvoiEmail` → console, `GenerateurCode` → aléatoire, `Horloge`, `RegistreClubs`, `RegistreLicences` (fausse base FFTT + fichiers importés) | `ui/infra/EmailConsole.java:22`, `ui/infra/GenerateurCodeAleatoire.java:11`, `ui/infra/RegistreLicencesFFTT.java:20`, `ui/infra/RegistresImportes.java:32` | ☑ T: RegistresImportesTest, DepotApplicationTest |

## 3. Préparation du tournoi

| ID | Élément | Défini dans | Preuve |
|---|---|---|---|
| F-030 | Nouveau tournoi (calendrier), choix imposé `ModePreparation` : préparation complète ou import | `ui/ecran/FormulaireNouveauTournoi.java:39`, `ui/ecran/EcranChoixPreparation.java:21`, `model/ModePreparation.java:16` | ☑ T: PresentationNouveauTournoiTest ; S4 |
| F-031 | Sections Général, Tableaux, Sportif, Complémentaire ; tous les champs de `Tournoi` et `InfosReglement` | `ui/ecran/FormulairesSection.java:44`, `ui/ecran/EcranTableaux.java:49`, `ui/ecran/EcranSportif.java:62`, `ui/ecran/EcranComplementaire.java:61`, `model/Tournoi.java:16`, `model/InfosReglement.java:33` | ☑ T: ParametresPreTournoiTest, SportifEtMaterielTest, ChampsHomologationTest ; S4 (quatre sections) |
| F-032 | Configuration d'un tableau : format, taille de poule 4, 2 qualifiés, élimination directe, 3 manches gagnantes, petite finale non, places, liste d'attente 4, tarifs, récompenses, mode de limite (`TOUTES_SERIES` / tranche / plafond), exclusion par rang national, sexe requis, catégories d'âge ; raccourcis `pointsMin/pointsMax` | `model/ConfigurationTableau.java:30`, `ui/ecran/FormulaireTableau.java:52` | ☑ T: ConfigurationTableauTest, GrilleRecompensesTest, LibellesTableauCaracterisationTest ; S3 |
| F-033 | Ordonnancement automatique des horaires des tableaux (1 h 30 entre tableaux, 2 h entre blocs) ; horaires modifiables avec inscrits | `core/OrdonnancementTableaux.java:39`, `model/HorairesTableau.java:14` | ☑ T: OrdonnancementTableauxTest, TableauxEtEcheancesTest |
| F-034 | Quotas de tableaux par jour (joueurs / joueuses), exception féminine (3 modes), clause « pas deux tableaux qui se suivent », clause « éliminé : N tableaux de plus » | `model/QuotasTableaux.java:24`, `model/RegleTableauxParJour.java:26`, `ordonnancement/ValidationInscription.java:27`, `ui/ecran/CarteReglesInscription.java:51` | ☑ T: QuotasJoueusesInscriptionTest, ReglesInscriptionRoundTripTest, InscriptionApresEliminationTest, ArticleNombreDeTableauxTest |
| F-035 | Cycle de vie `BROUILLON → DEMANDE_HOMOLOGATION → HOMOLOGUE → PUBLIE` (+ `ANNULE`), refus avec motif, dossier verrouillé dès la demande, complétude à trois niveaux, avertissements JA/niveau, dotation globale à la publication | `core/ServiceCycleTournoi.java:54`, `core/ServiceVerrousHomologation.java:34`, `core/ServiceCompletudeTournoi.java:23`, `model/StatutTournoi.java:37` | ☑ T: ServiceCycleTournoiTest, CycleHomologationVerrousTest, ServiceVerrousHomologationTest, CompletudeTroisNiveauxTest, PublicationEtDotationTest, RefusHomologationTest ; S3 (complétude) |
| F-036 | Règlement généré (aperçu, PDF A4 police Inter), articles optionnels (8 types), figé après publication ; règlement déposé (PDF) ; feuille d'engagement PDF ; mention salle d'échauffement (3.3.1.2.2) | `core/reglement/GenerateurReglement.java:62`, `core/reglement/RenduPdfReglement.java:35`, `core/reglement/ServiceReglementTournoi.java:31`, `core/reglement/GenerateurFeuilleEngagement.java:47`, `model/ArticleOptionnel.java:14` | ☑ T: GenerateurReglementTest, ServiceReglementTournoiTest, GenerateurFeuilleEngagementTest, RenduPdfCaracteresHorsPoliceTest, TablesEchauffementReglementTest ; S3 (règlement et feuille d'engagement, texte et PDF) |
| F-037 | Gestion des tournois du club (tournoi actuel, frise, historique), vue d'ensemble d'un brouillon, remplissage et finances | `ui/ecran/AccueilGestionTournois.java:30`, `ui/ecran/PageGestionTournoi.java:20`, `core/ServiceRemplissageFinances.java:21` | ☑ T: PresentationGestionTournoisTest, PresentationPreparationTournoiTest, ServiceRemplissageFinancesTest ; S3 (finances) ; S4 |
| F-038 | Tournoi importé : trois étapes (règlement PDF, classeur, vérification), bandeau jour J | `ui/ecran/EcranTournoiImporte.java:33`, `ui/ecran/PresentationTournoiImporte.java:18` | ☑ T: PresentationTournoiImporteTest |
| F-039 | Plans de salle : salles, lots de tables, repères, 4 plans types, réservations de tables | `core/ServicePlanSalle.java:25`, `core/EditionPlanSalles.java:33`, `core/PlansTypesSalle.java:20`, `model/PlanSalle.java:26`, `ordonnancement/ReservationsTables.java:26` | ☑ T: PlanSalleTest, PlanSalleTournoiTest, PresentationPlanSalleTest, OrdonnanceurReservationsTest |
| F-040 | Maintien des tableaux et dotations, politique de forfaits (3 périodes × 3 remboursements), paiement (lien HelloAsso), politique de balles, aire de jeu | `model/ConditionsMaintien.java:10`, `model/PolitiqueForfaits.java:12`, `model/Paiement.java:4`, `model/AireDeJeu.java:11` | ☑ T: ParametresPreTournoiTest, RoundTripTournoiTest ; S3 (JSON) |

## 4. Import et export

| ID | Élément | Défini dans | Preuve |
|---|---|---|---|
| F-050 | Lecture de tableurs sans dépendance : `.xlsx`, `.ods`, `.csv`/`.txt` (séparateur « ; », « , » ou tabulation), `.json` ; bornes 20 000 lignes × 200 colonnes ; message pour l'ancien `.xls` | `importation/LecteurTableur.java:43` | ☑ T: FaillesSecuriteLecteurTableurTest, LecteurTableurClasseurCaracterisationTest, ImportJoueursTest |
| F-051 | Import de joueurs : colonnes NOM, PRENOM, POINTS, CLUB obligatoires + LICENCE, SEXE, TABLEAU, NAISSANCE, CATEGORIE ; identité interne stable sans licence ; création des tableaux | `core/ImportJoueurs.java:52` | ☑ T: ImportJoueursTest, ImportJoueursVerrousHomologationTest |
| F-052 | Feuille « Tableaux » du classeur : NOM, MIN, MAX, PUBLIC, JOUR, HEURE, PLACES, ATTENTE, TARIF | `core/ImportTableaux.java:27` | ☑ T: ImportJoueursTest |
| F-053 | Classeurs modèles `modele-joueurs-pingmanager.xlsx` (deux feuilles, listes déroulantes) et `.csv` | `importation/ModeleImport.java:32` | ☑ T: ImportJoueursTest ; S3 (modèle CSV et en-têtes) |
| F-054 | Import provisoire `licencies.csv` et `clubs.csv` (10 Mo, 20 000 lignes, cellule ≤ 200, dates `dd/MM/uuuu` strictes, anomalies par ligne, certificat médical) | `importation/CsvFederal.java:30`, `importation/LecteurLicencesCsv.java:47`, `importation/LecteurClubsCsv.java:27` | ☑ T: LecteurLicencesCsvTest, LecteurClubsCsvTest, CsvHostileTest |
| F-055 | Export provisoire des résultats d'un tableau terminé : `parties.csv`, `classements.csv`, motifs de refus | `core/ServiceResultatsFederaux.java:44`, `importation/ExportResultatsCsv.java:27` | ☑ T: ServiceResultatsFederauxTest, ExportResultatsCsvTest, ExportResultatsCibleLibreTest ; S3 |

## 5. Côté joueur

| ID | Élément | Défini dans | Preuve |
|---|---|---|---|
| F-060 | Onglet Tournois : carte WebView + Leaflet embarqué (Plan IGN, repli OpenStreetMap), agenda par jour, filtres, tournois non accessibles grisés avec motif (IX.101, IX.102) | `ui/ecran/PageTournois.java:50`, `ui/ecran/CarteTournois.java:38`, `core/CatalogueTournois.java:30`, `core/EligibiliteTournoi.java:32` | ☑ T: EligibiliteEtCatalogueTest, PresentationCarteTournoisTest, StatutCarteTournoiTest, OngletTournoisContexteTest ; S4 (onglet Tournois) |
| F-061 | Géocodage Géoplateforme (`data.geopf.fr`, score minimal 0,4) | `ui/infra/GeocodeurGeoplateforme.java:26` | ☑ classe `GeocodeurGeoplateforme` non modifiée (réseau indisponible en session : non rejouable) |
| F-062 | Fiche du tournoi, assistant d'inscription avec validation en direct, simulation à blanc, tout ou rien, paiement « à régler plus tard » | `ui/ecran/FicheTournoi.java:47`, `ui/ecran/AssistantInscription.java:50`, `core/ServiceInscriptionEnLigne.java:26`, `core/MotifsInscription.java:17` | ☑ T: ServiceInscriptionEnLigneTest, SimulationInscriptionTest ; S4 (fiche et assistant) |
| F-063 | Règles d'inscription : éligibilité (points, rang, sexe), catégories d'âge II.302/II.303, quotas, capacité, liste d'attente bornée, tous les motifs de `ResultatInscriptionTournoi.Motif` | `core/ServiceInscription.java:42` | ☑ T: ServiceInscriptionTest, ListeAttenteBorneeTest, TableauInscriptionTest, CategorieAgeSaisonTest |
| F-064 | Mes tournois (en cours, à venir, passés avec performances), désinscription en ligne | `ui/ecran/MesTournois.java:47`, `core/PerformancesJoueur.java:24` | ☑ T: PresentationMesTournoisTest, PerformancesJoueurTest, DesinscriptionEnLigneContexteTest |

## 6. Jour J

| ID | Élément | Défini dans | Preuve |
|---|---|---|---|
| F-070 | Pointage par tableau, paiement, clôture, repêchage de la liste d'attente, désistement normal et cas particulier (remboursement tracé) | `core/ServicePointage.java:40`, `ui/ecran/EcranPointageTableau.java:49` | ☑ T: ServicePointageTest, PointageJourJTest, ScenarioInscriptionPointageTest |
| F-071 | Inscription sur place | `core/ServiceInscriptionSurPlace.java:29`, `ui/ecran/EcranInscriptionSurPlace.java:29` | ☑ T: PresentationInscriptionSurPlaceTest, InscriptionApresEliminationTest |
| F-072 | Lancement d'un tableau : vérifications (8 motifs), tirage reproductible par graine, poules en serpentin, séparation des clubs (I.302) | `core/ServiceLancementTableau.java:40`, `core/GenerateurPoules.java:34` | ☑ T: ServiceLancementTableauTest, LancementJourJTest, GenerateurPoulesTest, ConformiteTirageI302I305Test |
| F-073 | Ordre des parties de poule I.301, anti-arrangement ; classement I.303 | `core/GenerateurMatchsPoule.java:30`, `core/ClassementPouleService.java:42` | ☑ T: GenerateurMatchsPouleTest, ClassementPouleServiceTest |
| F-074 | Saisie des scores (notation « 11-9 », validation, issues anormales : forfait, abandon, refus, disqualification) | `core/SaisieResultatMatch.java:37`, `core/ValidationScoreMatch.java:27`, `core/NotationManche.java:26` | ☑ T: SaisieResultatMatchTest, ValidationScoreMatchTest, NotationMancheTest, SaisieScorePhaseFinaleTest |
| F-075 | Phase finale : construction (I.304, I.305, exempts, 2e tour complet), progression, petite finale, podium, diagnostic | `core/ServicePhaseFinale.java:38`, `core/PlacementQualifiesService.java:43`, `core/GenerateurTableauElimination.java:34`, `core/ProgressionTableau.java:24` | ☑ T: ServicePhaseFinaleTest, PlacementQualifiesServiceTest, ConformiteTableauFinalTest, ExemptsTableauTest, PetiteFinaleParTableauTest |
| F-076 | Discipline : cartons (jaune, jaune-rouge, rouge), progression 3.5.2, exclusion après 2 disqualifications | `core/GestionCartonsMatch.java:31`, `core/RegistreDisciplinaire.java:23` | ☑ T: DisciplineEtForfaitTest |
| F-077 | Salle : propositions de matchs en rouge/orange/vert (seuils 20 et 10 min), joueurs en attente avec raison, lancement, tableau de bord | `core/ServiceSalle.java:58`, `core/ReglePrioriteSalle.java:31`, `core/TableauDeBordSalle.java:32`, `ui/ecran/EcranSalle.java:98` | ☑ T: ServiceSalleTest, ServiceSalleAttenteTest, ReglePrioriteSalleTest, PresentationSalleTest ; S3 (tableau de bord et propositions de la salle) |
| F-078 | Ordonnanceur : contrainte dure un joueur ≠ deux tables, poids (vague 1, attente 2, déblocage 3, 1 point par minute…), stratégies (`StrategieOrdonnancement`), tour par tour, finales en fin de journée, réservations | `ordonnancement/Ordonnanceur.java:38`, `ordonnancement/PoidsOrdonnancement.java:15`, `model/StrategieOrdonnancement.java:18` | ☑ T: OrdonnanceurTest, OrdonnanceurAffectationTest, StrategiesOrdonnancementTest, TourParTourTest, FinalesEnFinDeJourneeTest, PoidsStrategiesPersistanceTest |
| F-079 | Estimation des durées (apprentissage), prévision de salle, anticipation | `ordonnancement/EstimateurDuree.java:25`, `ordonnancement/PrevisionSalle.java:22`, `ordonnancement/Anticipateur.java:24` | ☑ T: PrevisionEtAnticipationTest |
| F-080 | Prévisions par simulation : 16 tables, graine 1, variabilité 0,2, heure limite 20 h, partie 20 min, loi de durée bornée (0,5 à 2,5 × moyenne), temps annexes (appel 60 s, rappel 15 % de 180 s, échauffement 120 s, pause 30 s, temps mort 20 % de 60 s, feuille 90 s), 100 tirages, indicateurs (à l'heure ≤ 5 min, gros retard ≥ 30 min) | `core/SimulateurTournoi.java:59`, `core/ScenarioSimulation.java:24`, `core/StatistiquesSimulation.java:49`, `ordonnancement/LoiDureeMatch.java:25`, `ordonnancement/TempsAnnexesMatch.java:27`, `ui/ecran/EcranPrevisions.java:42` | ☑ T: SimulateurTournoiTest, SimulationMonteCarloTest, LoiDureeMatchTest, TempsAnnexesMatchTest, PresentationPrevisionsTest ; S3 (simulations à graine fixe, trois stratégies) |
| F-081 | Feuilles de match et de poule, arbre imprimable ; rendu PDF (format provisoire 3 × 5), impression ou enregistrement | `core/feuilles/GenerateurFeuilles.java:41`, `ui/infra/RenduPdfFeuilles.java:39`, `ui/infra/ImpressionFeuilles.java:26` | ☑ T: GenerateurFeuillesTest, RenduPdfFeuillesTest, ImpressionFeuillesTest, ConformiteFeuillesFFTTTest ; S3 (feuilles et arbre en PDF) |
| F-082 | Annonces vocales : modes d'annonce, 2 diffusions par défaut (max 3), débit −10 à 10, rappel ≤ 30 min, SSML, prononciations phonétiques (API) des joueurs et clubs | `core/ServiceAnnoncesVocales.java:48`, `core/GenerateurSsml.java:16`, `model/ReglagesAnnoncesVocales.java:13`, `core/RegistrePrononciations.java:21`, `core/ValidationPrononciation.java:19` | ☑ T: ServiceAnnoncesVocalesTest, GenerateurSsmlTest, TexteAnnonceVocaleTest, ValidationPrononciationTest, RegistrePrononciationsTest |
| F-083 | Convocations par SMS : consentement, numéro de portable, textes GSM 7 bits (160/153, UCS-2 70/67), expéditeur, journal ; envoi simulé | `core/ServiceConvocationsSms.java:37`, `core/TexteSms.java:16`, `core/TexteConvocation.java:21`, `ui/infra/EnvoiSmsSimule.java:19` | ☑ T: ServiceConvocationsSmsTest, TexteSmsTest, TexteConvocationTest, EnvoiSmsSimuleTest, NumeroPortableTest |
| F-084 | Suivi sur téléphone : serveur HTTP en lecture seule, port 8080 (1024 à 65535), routes `/`, `/matchs`, `/tableau?n=`, `/partie?id=` et recherche `q`, chemin ≤ 128, 16 fils, motifs de démarrage | `ui/diffusion/DiffusionSuivi.java:20`, `ui/diffusion/ServeurSuiviPublic.java:36`, `ui/diffusion/PagesSuivi.java:27`, `ui/diffusion/InstantaneSuivi.java:52` | ☑ T: ServeurSuiviPublicTest, ServeurSuiviSecuriteTest, PagesSuiviTest, InstantaneSuiviTest ; S3 (toutes les pages HTML, requêtes valides et invalides) |
| F-085 | Suivi de tous les tableaux, suivi de poule, élimination d'un joueur | `core/SuiviTournoi.java:27`, `core/SuiviPoule.java:26`, `core/EliminationTableau.java:21`, `ui/ecran/EcranSuiviTournoi.java:49` | ☑ T: SuiviTournoiTest, SuiviPouleTest, PresentationSuiviTournoiTest ; S3 |
| F-086 | Couplage statut du tournoi ↔ déroulé : `REFUSER_SI_ANNULE` (défaut) ou `EXIGER_PUBLIE` | `core/PolitiqueOuverture.java:12` | ☑ T: CoutureStatutDerouleTest |

## 7. Formats de compétition

| ID | Élément | Défini dans | Preuve |
|---|---|---|---|
| F-090 | Format `P3Q2` (poule de 3, 2 qualifiés), format configurable (builder), sort des non-qualifiés, registre et format par défaut | `model/format/FormatPoule3Qualifie2.java:22`, `model/format/FormatConfigurable.java:29`, `model/format/RegistreFormats.java:17` | ☑ T: MoteurTournoiTest, BestOfEtPetiteFinaleTest |
| F-091 | Orchestration d'une épreuve selon un format | `core/MoteurTournoi.java:29` | ☑ T: MoteurTournoiTest, BoucleCompleteTableauTest |
| F-092 | Référentiel départements → ligues (`departements.csv`), niveaux de tournoi, échelons d'homologation, grades de juge-arbitre | `core/Territoires.java:24`, `model/NiveauTournoi.java:17`, `model/EchelonHomologation.java:19`, `model/GradeJugeArbitre.java:20` | ☑ T: EligibiliteEtCatalogueTest, EchelonHomologationTest ; S3 (territoires) |

## 8. Règles d'architecture vérifiées par des tests

| ID | Élément | Défini dans | Preuve |
|---|---|---|---|
| F-100 | `model`, `core`, `ordonnancement` sans dépendance vers l'interface, la persistance ou l'importation (seule exception : `RenduPdfReglement` → PDFBox) | test `RegleOrModeleSansDependanceTest` | ☑ T: RegleOrModeleSansDependanceTest (vert avec la nouvelle classe `core.TextesSuivi`) |
| F-101 | Jamais de `switch` sur une énumération | `CLAUDE.md` | ☑ M: recherche de `switch` sur une énumération dans le diff : aucun |
| F-102 | Dépendances : JavaFX controls + web 21.0.5, PDFBox 2.0.29, JUnit 5.10.2 ; aucune autre | `pom.xml` | ☑ M: `pom.xml` identique à `main` |

## 9. Ressources embarquées

| ID | Élément | Défini dans | Preuve |
|---|---|---|---|
| F-110 | `theme.css` (3 901 lignes), polices Inter (Regular, SemiBold) et Bricolage Grotesque (Bold, ExtraBold) avec licences OFL | `src/main/resources/com/pingmanager/ui`, `.../polices` | ☑ M: `src/main/resources` identique à `main` |
| F-111 | Carte : `carte.html`, `leaflet.js`, `leaflet.css`, licence Leaflet | `src/main/resources/com/pingmanager/ui/carte` | ☑ M: `src/main/resources` identique à `main` |
| F-112 | `departements.csv` (104 lignes) | `src/main/resources/com/pingmanager/core` | ☑ M: `src/main/resources` identique à `main` ; S3 (territoires) |


## Annexe A — Relevé automatique

### A.1 Énumérations et leurs valeurs

| Énumération | Fichier | Valeurs (dans l'ordre) |
|---|---|---|
| `Moment` | `core/CalendrierTournoi.java:19` | A_VENIR, EN_COURS, PASSE |
| `EtatInscriptions` | `core/CalendrierTournoi.java:22` | PAS_ENCORE_OUVERTES, OUVERTES, CLOSES |
| `Etat` | `core/DiagnosticPhaseFinale.java:17` | SANS_FORMAT, SANS_ELIMINATION, POULES_NON_LANCEES, POULES_EN_COURS, QUALIFIES_INSUFFISANTS, PRETE_A_CONSTRUIRE, EN_COURS, TERMINEE |
| `Statut` | `core/EligibiliteTournoi.java:35` | ACCESSIBLE, NON_ACCESSIBLE, A_VERIFIER |
| `Champ` | `core/ImportJoueurs.java:55` | NOM, PRENOM, POINTS, CLUB, LICENCE, SEXE, TABLEAU, NAISSANCE, CATEGORIE |
| `Col` | `core/ImportTableaux.java:49` | NOM, MIN, MAX, PUBLIC, JOUR, HEURE, PLACES, ATTENTE, TARIF |
| `MotifRefusExport` | `core/MotifRefusExport.java:4` | TABLEAU_INCONNU, TABLEAU_NON_TERMINE, AUCUN_TABLEAU_TERMINE |
| `NiveauPriorite` | `core/NiveauPriorite.java:11` | URGENT, IMPORTANT, NORMAL |
| `Phase` | `core/PartieExportee.java:18` | POULE, TABLEAU |
| `PlanType` | `core/PlansTypesSalle.java:23` | UNE_RANGEE, DEUX_RANGEES, TROIS_RANGEES, QUATRE_RANGEES |
| `PolitiqueOuverture` | `core/PolitiqueOuverture.java:12` | REFUSER_SI_ANNULE, EXIGER_PUBLIE |
| `Motif` | `core/ResultatAgenda.java:12` | AJOUTE, MODIFIE, RETIRE, INCHANGE, TITRE_MANQUANT, TITRE_TROP_LONG, NOTE_TROP_LONGUE, DATE_MANQUANTE, FIN_AVANT_DEBUT, INTROUVABLE, DATE_AUTOMATIQUE |
| `Motif` | `core/ResultatConfirmation.java:18` | VERIFIE, CODE_INCORRECT, CODE_EXPIRE |
| `Motif` | `core/ResultatContactSms.java:12` | ENREGISTRE, CONSENTEMENT_MANQUANT, NUMERO_INVALIDE, LICENCE_MANQUANTE, TOURNOI_TERMINE |
| `Motif` | `core/ResultatEditionSalles.java:12` | SALLE_AJOUTEE, SALLE_RETIREE, SALLE_MODIFIEE, NOM_VIDE, NOM_DEJA_PRIS, DERNIERE_SALLE, SALLE_INTROUVABLE, TABLEAU_INCONNU |
| `Motif` | `core/ResultatInscriptionOrganisateur.java:18` | CODE_ENVOYE, CLUB_INTROUVABLE, EMAIL_DEJA_UTILISE |
| `Motif` | `core/ResultatInscriptionTournoi.java:20` | INSCRIT, EN_FILE_ATTENTE, NON_ELIGIBLE, DEJA_PRESENT, QUOTA_TABLEAUX_ATTEINT, TABLEAUX_SUCCESSIFS, TABLEAU_FEMININ_OBLIGATOIRE, LISTE_ATTENTE_PLEINE, TOURNOI_FERME, CATEGORIE_NON_ADMISE, CATEGORIE_NON_AUTORISEE |
| `Statut` | `core/ResultatLiaisonClub.java:14` | VERIFIE, CLUB_INTROUVABLE, CODE_EXPIRE, CODE_INCORRECT |
| `Motif` | `core/ResultatModificationTournoi.java:15` | APPLIQUEE, CHAMP_VERROUILLE |
| `Motif` | `core/ResultatPropositionSalle.java:13` | LANCES, AUCUNE_TABLE_LIBRE, AUCUN_MATCH_PRET, TABLES_LIBRES_RESERVEES, SALLE_SANS_TABLE, TOURNOI_FERME |
| `Motif` | `core/ResultatSaisieMatch.java:13` | ENREGISTRE, POULES_NON_MODIFIABLES, MATCH_HORS_TABLEAU, MATCH_EXEMPT, AUCUNE_MANCHE, MANCHE_INVALIDE, TROP_DE_MANCHES, MANCHE_APRES_DECISION, MATCH_NON_DECIDE, ISSUE_NORMALE, FAUTIF_HORS_MATCH |
| `Motif` | `core/ResultatSaisieScore.java:18` | VALIDE, AUCUNE_MANCHE, MANCHE_A_EGALITE, MANCHE_SOUS_ONZE_POINTS, MANCHE_ECART_INSUFFISANT, MANCHE_PROLONGATION_INCORRECTE, MANCHE_EN_TROP, MATCH_NON_DECIDE, MATCH_HORS_TABLEAU, JOUEURS_NON_CONNUS, SUITE_DEJA_JOUEE, ISSUE_INVALIDE |
| `Motif` | `core/ResultatTransition.java:23` | EFFECTUEE, DOSSIER_INCOMPLET, MOTIF_REFUS_ABSENT |
| `Motif` | `core/ResultatVerificationLancement.java:16` | PRET, HORS_TOURNOI, TOURNOI_FERME, POINTAGE_NON_OUVERT, POINTAGE_NON_CLOTURE, DEJA_LANCE, SANS_FORMAT, PAS_ASSEZ_DE_PRESENTS |
| `Nature` | `core/SegmentAnnonce.java:16` | LIBRE, PRENOM, NOM, CLUB |
| `Niveau` | `core/ServiceCompletudeTournoi.java:24` | OBLIGATOIRE_HOMOLOGATION, OBLIGATOIRE_PUBLICATION, RECOMMANDE, OPTIONNEL |
| `Statut` | `core/StatutCarteTournoi.java:27` | EN_COURS, A_VENIR, NON_ACCESSIBLE, COMPLET_OU_HORS_NIVEAU |
| `Qualification` | `core/SuiviPoule.java:29` | QUALIFIE, NON_QUALIFIE, PLACE_QUALIFICATIVE, HORS_PLACE_QUALIFICATIVE |
| `RaisonAttente` | `core/TableauDeBordSalle.java:63` | PRET, ADVERSAIRE_EN_JEU, ADVERSAIRE_INCONNU |
| `TypeCase` | `core/feuilles/ArbreImprimable.java:47` | MATCH, EXEMPT, VIDE |
| `MotifRefusFeuille` | `core/feuilles/MotifRefusFeuille.java:7` | MATCH_EXEMPT, JOUEURS_A_DETERMINER, MATCH_INTROUVABLE, POULE_SANS_PARTIE, PAS_DE_TABLEAU_FINAL |
| `Motif` | `core/reglement/ResultatModificationReglement.java:13` | APPLIQUEE, TOURNOI_PUBLIE |
| `Format` | `importation/LecteurTableur.java:46` | XLSX, ODS, CSV, JSON |
| `SituationCertificat` | `importation/SituationCertificat.java:8` | EN_REGLE, SANS_PRATIQUE, INCONNU |
| `Type` | `model/ArticleOptionnel.java:17` | DROIT_IMAGE, BUVETTE, OUVERTURE_SALLE, TIRAGE_AU_SORT, SUR_CLASSEMENT, FORFAIT_NON_EXCUSE, INFOS_PRATIQUES, LIBRE |
| `CategorieAge` | `model/CategorieAge.java:17` | POUSSIN, BENJAMIN, MINIME, CADET, JUNIOR, SENIOR, VETERAN_40, VETERAN_45, VETERAN_50, VETERAN_55, VETERAN_60, VETERAN_65, VETERAN_70, VETERAN_75, VETERAN_80, VETERAN_85, VETERAN_90 |
| `ChampHomologation` | `model/ChampHomologation.java:13` | NOM, CATEGORIE, CLUB_ORGANISATEUR, NUMERO_AFFILIATION, ADRESSE, DATES, CORRESPONDANT, JUGES_ARBITRES, LISTE_TABLEAUX, TABLEAU_NOM, TABLEAU_CATEGORIE_POINTS, TABLEAU_PUBLIC, TABLEAU_DEBUT |
| `ModeReduction` | `model/ConditionsMaintien.java:13` | REMPLISSAGE, ABSENTS |
| `CouleurBalles` | `model/CouleurBalles.java:4` | BLANCHE, ORANGE |
| `CouleurCarton` | `model/CouleurCarton.java:16` | JAUNE, JAUNE_ROUGE, ROUGE |
| `EchelonHomologation` | `model/EchelonHomologation.java:19` | COMITE_DEPARTEMENTAL, LIGUE, FFTT |
| `FormatPhaseFinale` | `model/FormatPhaseFinale.java:16` | ELIMINATION_DIRECTE, AUCUNE |
| `GradeJugeArbitre` | `model/GradeJugeArbitre.java:20` | JA1, JA2, JA3, JA_NATIONAL |
| `IssueMatch` | `model/IssueMatch.java:11` | NORMALE, FORFAIT, ABANDON, REFUS_DE_JOUER, DISQUALIFICATION |
| `ModeAnnonce` | `model/ModeAnnonce.java:16` | AUCUNE, SMS, APPEL, SMS_ET_APPEL |
| `ModeLimiteClassement` | `model/ModeLimiteClassement.java:24` | TRANCHE, POINT_MAX, TOUTES_SERIES |
| `ModePreparation` | `model/ModePreparation.java:16` | A_CHOISIR, COMPLETE, IMPORT |
| `NiveauAcces` | `model/NiveauAcces.java:16` | ADMIN_NIVEAU_1, ADMIN_PRINCIPAL |
| `NiveauTournoi` | `model/NiveauTournoi.java:17` | PROMOTIONNEL, DEPARTEMENTAL, REGIONAL, NATIONAL_B, NATIONAL_A, INTERNATIONAL |
| `Mode` | `model/OptionsReglement.java:14` | DEPOSE, GENERE |
| `OrientationTable` | `model/OrientationTable.java:8` | HORIZONTALE, VERTICALE |
| `OrigineEvenement` | `model/OrigineEvenement.java:8` | PERSONNEL, TOURNOI |
| `Mode` | `model/Paiement.java:7` | HELLOASSO, PLATEFORME, AUTRE |
| `PhasePoints` | `model/PhasePoints.java:4` | PREMIERE, DEUXIEME |
| `PolitiqueBalles` | `model/PolitiqueBalles.java:6` | FOURNIES_PAR_ORGANISATEUR, APPORTEES_PAR_JOUEURS |
| `Periode` | `model/PolitiqueForfaits.java:15` | PENDANT_INSCRIPTIONS, APRES_CLOTURE, JOUR_J |
| `Remboursement` | `model/PolitiqueForfaits.java:32` | AUCUN, PARTIEL, COMPLET |
| `ExceptionFeminine` | `model/RegleTableauxParJour.java:29` | AUCUNE, TABLEAU_SUPPLEMENTAIRE_SANS_CONDITION, TABLEAU_SUPPLEMENTAIRE_SI_TABLEAU_SPECIAL |
| `Motif` | `model/ResultatPlacementPlan.java:14` | PLACE, DEPLACE, RETIRE, MODIFIE, REDIMENSIONNE, HORS_GRILLE, CHEVAUCHEMENT, NUMERO_INVALIDE, NUMERO_DEJA_PRIS, ELEMENT_INTROUVABLE, DIMENSIONS_INVALIDES, GRILLE_TROP_PETITE |
| `RoleJugeArbitre` | `model/RoleJugeArbitre.java:4` | PRINCIPAL, ADJOINT |
| `Sexe` | `model/Sexe.java:7` | HOMME, FEMME |
| `StatutEnvoiSms` | `model/StatutEnvoiSms.java:7` | ENVOYE, ECHEC, LIMITE_ATTEINTE |
| `StatutMatch` | `model/StatutMatch.java:6` | EN_ATTENTE, A_JOUER, EN_COURS, TERMINE |
| `StatutTableau` | `model/StatutTableau.java:30` | INSCRIPTION, POINTAGE, LANCE, PHASE_FINALE |
| `StatutTournoi` | `model/StatutTournoi.java:37` | BROUILLON, DEMANDE_HOMOLOGATION, HOMOLOGUE, PUBLIE, ANNULE |
| `StrategieOrdonnancement` | `model/StrategieOrdonnancement.java:18` | AVANCER_TABLEAUX_COMMENCES, FAIRE_AVANCER_PETIT_A_PETIT, MINIMISER_ATTENTE |
| `ResultatInscription` | `model/Tableau.java:500` | INSCRIT, EN_FILE_ATTENTE, LISTE_ATTENTE_PLEINE, NON_ELIGIBLE, DEJA_PRESENT |
| `TypeConvocation` | `model/TypeConvocation.java:7` | MATCH_POULE, TOUR_TABLEAU, MATCH_CLASSEMENT, ARBITRAGE |
| `TypeRecompense` | `model/TypeRecompense.java:14` | ARGENT, REDUCTION_POURCENTAGE, PART_INSCRIPTIONS, AUTRE |
| `TypeRepere` | `model/TypeRepere.java:9` | TABLE_ARBITRAGE, TABLE_DE_MARQUE, ENTREE, SORTIE, SORTIE_SECOURS, WC, VESTIAIRES, BUVETTE, ESCALIER, ECHAUFFEMENT, PUBLIC, AUTRE |
| `TypeTournoi` | `model/TypeTournoi.java:10` | PROMOTIONNEL, NATIONAL, INTERNATIONAL, AUTRE |
| `SortNonQualifies` | `model/format/SortNonQualifies.java:11` | ELIMINE, CONSOLANTE, PARTIES_DE_CLASSEMENT |
| `PhaseMatch` | `ordonnancement/PhaseMatch.java:10` | POULE, PREMIER_TOUR_TABLEAU, TOUR_SUIVANT_TABLEAU |
| `Motif` | `ordonnancement/ResultatAffectationTable.java:20` | AFFECTE, AFFECTATION_ANNULEE, TABLE_LIBEREE, TABLE_HORS_LIMITES, TABLE_OCCUPEE, TABLE_RESERVEE, MATCH_INCONNU, TOURNOI_FERME, MATCH_DEJA_EN_COURS, MATCH_TERMINE, MATCH_EXEMPT, JOUEURS_NON_CONNUS, JOUEUR_DEJA_EN_JEU, MATCH_PAS_EN_COURS, RESULTAT_NON_SAISI |
| `Type` | `persistence/json/Json.java:23` | OBJET, TABLEAU, CHAINE, NOMBRE, BOOLEEN, NUL |
| `Motif` | `ui/diffusion/DiffusionSuivi.java:29` | DEMARRE, DEJA_DEMARRE, PORT_INVALIDE, PORT_OCCUPE, PAS_DE_RESEAU_LOCAL, ERREUR |
| `EtatCarte` | `ui/ecran/DispositionArbre.java:54` | A_JOUER, EN_COURS, TERMINE, EN_ATTENTE, EXEMPT |
| `EtatOfficiel` | `ui/ecran/EcranCompletion.java:521` | FAITE, EN_COURS, A_VENIR |
| `Detail` | `ui/ecran/PresentationAvancement.java:38` | OUVRIR_POINTAGE, POINTAGE, POULES, TABLEAU_FINAL |
| `Vue` | `ui/ecran/PresentationCarteTournois.java:26` | CARTE, AGENDA |
| `Etape` | `ui/ecran/PresentationJourJ.java:51` | INSCRIPTION, POINTAGE_OUVERT, POINTAGE_CLOTURE, POULES, POULES_TERMINEES, PHASE_FINALE |
| `Champ` | `ui/ecran/PresentationNouveauTournoi.java:59` | CLUB, NOM, DEBUT, FIN |
| `TypeCase` | `ui/ecran/PresentationPhaseFinale.java:65` | MATCH, EXEMPT, VIDE |
| `EtatEtape` | `ui/ecran/PresentationPreparationTournoi.java:45` | FAITE, EN_COURS, A_VENIR |
| `IndicateurTableau` | `ui/ecran/PresentationPrevisions.java:518` | FIN, FIN_TARD, RETARD, ATTENTE, DEPASSEMENT |
| `EtatDroit` | `ui/ecran/PresentationProfil.java:39` | OUI, NON, A_VENIR |
| `Tonalite` | `ui/ecran/PresentationRemplissageFinances.java:22` | NEUTRE, POSITIF, NEGATIF |
| `StatutPastille` | `ui/ecran/StatutPastille.java:13` | SUCCES, ATTENTION, ERREUR, INFO, NEUTRE |

### A.2 Constantes (valeurs par défaut, seuils, libellés techniques)

| Constante | Fichier | Valeur |
|---|---|---|
| `CatalogueTournois.ORDRE` (Comparator<Entree>) | `core/CatalogueTournois.java:129` | `Comparator .comparing((Entree x) -> CalendrierTournoi.debut(x.tournoi()) == null ? LocalDate.MAX : Calendri…` |
| `ExpediteurSms.LONGUEUR_MAXIMALE` (int) | `core/ExpediteurSms.java:17` | `11` |
| `ExpediteurSms.PAR_DEFAUT` (String) | `core/ExpediteurSms.java:19` | `"PINGMANAGER"` |
| `ExportIcs.CRLF` (String) | `core/ExportIcs.java:33` | `"\r\n"` |
| `ExportIcs.OCTETS_PAR_LIGNE` (int) | `core/ExportIcs.java:36` | `75` |
| `ExportIcs.DOMAINE_UID` (String) | `core/ExportIcs.java:39` | `"pingmanager"` |
| `ExportIcs.JOUR` (DateTimeFormatter) | `core/ExportIcs.java:41` | `DateTimeFormatter.ofPattern("yyyyMMdd")` |
| `ExportIcs.HEURE_LOCALE` (DateTimeFormatter) | `core/ExportIcs.java:42` | `DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")` |
| `ExportIcs.HEURE_UTC` (DateTimeFormatter) | `core/ExportIcs.java:43` | `DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'") .withZone(ZoneOffset.UTC)` |
| `GenerateurMatchsPoule.P3_UN_OU_TROIS` (int[][]) | `core/GenerateurMatchsPoule.java:40` | `{ {1, 3}, {2, 3}, {1, 2} }` |
| `GenerateurMatchsPoule.P3_DEUX` (int[][]) | `core/GenerateurMatchsPoule.java:44` | `{ {1, 3}, {1, 2}, {2, 3} }` |
| `GenerateurMatchsPoule.P4_UN_OU_TROIS` (int[][]) | `core/GenerateurMatchsPoule.java:49` | `{ {1, 4}, {2, 3}, {1, 3}, {2, 4}, {1, 2}, {3, 4} }` |
| `GenerateurMatchsPoule.P4_DEUX` (int[][]) | `core/GenerateurMatchsPoule.java:53` | `{ {1, 3}, {2, 4}, {1, 2}, {3, 4}, {1, 4}, {2, 3} }` |
| `GenerateurMatchsPoule.P5_UN` (int[][]) | `core/GenerateurMatchsPoule.java:58` | `{ {2, 5}, {3, 4}, {1, 5}, {2, 3}, {1, 4}, {3, 5}, {1, 3}, {2, 4}, {1, 2}, {4, 5} }` |
| `GenerateurMatchsPoule.P5_DEUX` (int[][]) | `core/GenerateurMatchsPoule.java:63` | `{ {2, 5}, {3, 4}, {1, 4}, {3, 5}, {1, 3}, {2, 4}, {1, 2}, {4, 5}, {1, 5}, {2, 3} }` |
| `GenerateurMatchsPoule.P6_UN` (int[][]) | `core/GenerateurMatchsPoule.java:69` | `{ {1, 6}, {2, 5}, {3, 4}, {1, 5}, {4, 6}, {2, 3}, {1, 4}, {3, 5}, {2, 6}, {1, 3}, {2, 4}, {5, 6}, {1, 2}, {…` |
| `GenerateurMatchsPoule.P6_DEUX` (int[][]) | `core/GenerateurMatchsPoule.java:75` | `{ {1, 6}, {2, 5}, {3, 4}, {1, 4}, {3, 5}, {2, 6}, {1, 3}, {2, 4}, {5, 6}, {1, 2}, {3, 6}, {4, 5}, {1, 5}, {…` |
| `GenerateurSsml.SILENCE_ENTRE_DIFFUSIONS` (String) | `core/GenerateurSsml.java:19` | `"<break time=\"1500ms\"/>"` |
| `ImportJoueurs.SYNONYMES` (Map<String, Champ>) | `core/ImportJoueurs.java:78` | `new LinkedHashMap<>()` |
| `ImportJoueurs.VERROUS` (ServiceVerrousHomologation) | `core/ImportJoueurs.java:389` | `new ServiceVerrousHomologation()` |
| `ImportTableaux.SYNONYMES` (Map<String, Col>) | `core/ImportTableaux.java:52` | `new LinkedHashMap<>()` |
| `NotationManche.COMPLETE` (Pattern) | `core/NotationManche.java:28` | `Pattern.compile("^\\s*(\\d{1,2})\\s*[-/: ]\\s*(\\d{1,2})\\s*$")` |
| `NotationManche.COURTE` (Pattern) | `core/NotationManche.java:29` | `Pattern.compile("^\\s*([+-]?)\\s*(\\d{1,2})\\s*$")` |
| `PlacementQualifiesService.PLACE_EXEMPT` (Joueur) | `core/PlacementQualifiesService.java:234` | `new Joueur("__EXEMPT__", "Exempt", "", Sexe.HOMME, null, 0)` |
| `PlacementQualifiesService.BUDGET_RETOUR_ARRIERE` (int) | `core/PlacementQualifiesService.java:237` | `200_000` |
| `ReglePrioriteSalle.ATTENTE_LONGUE` (Duration) | `core/ReglePrioriteSalle.java:34` | `Duration.ofMinutes(20)` |
| `ReglePrioriteSalle.ATTENTE_MOYENNE` (Duration) | `core/ReglePrioriteSalle.java:37` | `Duration.ofMinutes(10)` |
| `RemplissageTableaux.PAR_POINTS` (Comparator<Joueur>) | `core/RemplissageTableaux.java:24` | `Comparator.comparingInt(Joueur::getPoints).reversed() .thenComparing(Joueur::getNom, String.CASE_INSENSITIV…` |
| `RemplissageTableaux.PAR_CLUB` (Comparator<Joueur>) | `core/RemplissageTableaux.java:29` | `Comparator .comparing((Joueur j) -> j.getClub() == null ? "" : j.getClub(), String.CASE_INSENSITIVE_ORDER) …` |
| `ScenarioSimulation.HEURE_LIMITE_PAR_DEFAUT` (LocalTime) | `core/ScenarioSimulation.java:27` | `LocalTime.of(20, 0)` |
| `ScenarioSimulation.DUREE_PARTIE_PAR_DEFAUT` (Duration) | `core/ScenarioSimulation.java:30` | `Duration.ofMinutes(20)` |
| `ServiceAgenda.LONGUEUR_MAX_TITRE` (int) | `core/ServiceAgenda.java:31` | `120` |
| `ServiceAgenda.LONGUEUR_MAX_NOTE` (int) | `core/ServiceAgenda.java:32` | `2000` |
| `ServiceAgenda.PREFIXE_TOURNOI` (String) | `core/ServiceAgenda.java:35` | `"tournoi-"` |
| `ServiceComptesOrganisateurs.VALIDITE_CODE_PAR_DEFAUT` (Duration) | `core/ServiceComptesOrganisateurs.java:42` | `Duration.ofMinutes(30)` |
| `ServiceLiaisonClub.VALIDITE_CODE_PAR_DEFAUT` (Duration) | `core/ServiceLiaisonClub.java:42` | `Duration.ofMinutes(30)` |
| `ServicePhaseFinale.NOM_TOUR_PETITE_FINALE` (String) | `core/ServicePhaseFinale.java:44` | `"Petite finale"` |
| `ServicePlanSalle.NOM_SALLE_PAR_DEFAUT` (String) | `core/ServicePlanSalle.java:28` | `"Salle"` |
| `SimulateurTournoi.PALIER_NIVEAU` (int) | `core/SimulateurTournoi.java:62` | `500` |
| `SimulateurTournoi.DEBUT_PAR_DEFAUT` (LocalTime) | `core/SimulateurTournoi.java:65` | `LocalTime.of(9, 0)` |
| `SimulateurTournoi.TIRAGES_PAR_DEFAUT` (int) | `core/SimulateurTournoi.java:68` | `100` |
| `StatistiquesSimulation.RETARD_A_L_HEURE` (int) | `core/StatistiquesSimulation.java:61` | `5` |
| `StatistiquesSimulation.GROS_RETARD` (int) | `core/StatistiquesSimulation.java:63` | `30` |
| `StatutCarteTournoi.MOTIF_HORS_NIVEAU` (String) | `core/StatutCarteTournoi.java:42` | `"Aucun tableau à votre niveau (points ou catégorie)."` |
| `StatutCarteTournoi.MOTIF_COMPLET` (String) | `core/StatutCarteTournoi.java:44` | `"Tableaux à votre niveau complets (liste d'attente éventuelle)."` |
| `SuiviTournoi.ORDRE_DE_LA_JOURNEE` (Comparator<Tableau>) | `core/SuiviTournoi.java:98` | `Comparator .comparing((Tableau t) -> t.getHoraires().getDebutMatchs(), Comparator.nullsLast(Comparator.<Loc…` |
| `SyntheseVocale.PAR_DEFAUT` (ParametresVoix) | `core/SyntheseVocale.java:47` | `new ParametresVoix(null, 0)` |
| `Territoires.PAR_CODE` (Map<String, Departement>) | `core/Territoires.java:33` | `charger()` |
| `TexteSms.LONGUEUR_GSM7` (int) | `core/TexteSms.java:19` | `160` |
| `TexteSms.LONGUEUR_UCS2` (int) | `core/TexteSms.java:21` | `70` |
| `TexteSms.SEGMENT_GSM7` (int) | `core/TexteSms.java:22` | `153` |
| `TexteSms.SEGMENT_UCS2` (int) | `core/TexteSms.java:23` | `67` |
| `TexteSms.GSM7_BASE` (String) | `core/TexteSms.java:26` | `"@£$¥èéùìòÇ\nØø\rÅåΔ_ΦΓΛΩΠΨΣΘΞÆæßÉ !\"#¤%&'()*+,-./0123456789:;<=>?" + "¡ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÑÜ§¿ab…` |
| `TexteSms.GSM7_EXTENSION` (String) | `core/TexteSms.java:29` | `"^{}\\[~]\|€\f"` |
| `ValidationPrononciation.LONGUEUR_MAX` (int) | `core/ValidationPrononciation.java:22` | `60` |
| `ValidationScoreMatch.POINTS_MANCHE` (int) | `core/ValidationScoreMatch.java:30` | `11` |
| `ArbreImprimable.TITRE` (String) | `core/feuilles/ArbreImprimable.java:23` | `"Tableau final"` |
| `ArbreImprimable.TITRE_PETITE_FINALE` (String) | `core/feuilles/ArbreImprimable.java:25` | `"Petite finale (3e place)"` |
| `FeuilleMatch.TITRE` (String) | `core/feuilles/FeuilleMatch.java:33` | `"Feuille de match"` |
| `FeuillePoule.TITRE` (String) | `core/feuilles/FeuillePoule.java:33` | `"Feuille de poule"` |
| `FeuillePoule.TITRE_RENCONTRE` (String) | `core/feuilles/FeuillePoule.java:35` | `"Feuille de rencontre"` |
| `FeuillePoule.CASE_BARREE` (String) | `core/feuilles/FeuillePoule.java:37` | `"X"` |
| `GenerateurFeuilles.JJ_MM_AAAA` (DateTimeFormatter) | `core/feuilles/GenerateurFeuilles.java:43` | `DateTimeFormatter.ofPattern("dd/MM/yyyy")` |
| `GenerateurFeuilles.SIGNATURES` (List<String>) | `core/feuilles/GenerateurFeuilles.java:46` | `List.of("Joueur A", "Joueur B", "Arbitre")` |
| `GenerateurFeuilles.NOTE_I303` (String) | `core/feuilles/GenerateurFeuilles.java:53` | `"I.303 : victoire 2 points, défaite 1 point ; 0 point en cas d'absence à l'appel, " + "d'abandon, de refus …` |
| `GenerateurFeuilles.VIDE` (Place) | `core/feuilles/GenerateurFeuilles.java:271` | `new Place(TypeCase.VIDE, null, null)` |
| `GrilleManches.LIGNES` (int) | `core/feuilles/GrilleManches.java:21` | `3` |
| `GrilleManches.COLONNES` (int) | `core/feuilles/GrilleManches.java:23` | `5` |
| `JoueurFeuille.A_DETERMINER` (String) | `core/feuilles/JoueurFeuille.java:22` | `"À déterminer"` |
| `Formats.JJ_MM_AAAA` (DateTimeFormatter) | `core/reglement/Formats.java:12` | `DateTimeFormatter.ofPattern("dd/MM/yyyy")` |
| `Formats.JOUR_LONG` (DateTimeFormatter) | `core/reglement/Formats.java:13` | `DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH)` |
| `Formats.JOUR_COURT` (DateTimeFormatter) | `core/reglement/Formats.java:14` | `DateTimeFormatter.ofPattern("EEE d MMM", Locale.FRENCH)` |
| `Formats.JOUR_MOIS` (DateTimeFormatter) | `core/reglement/Formats.java:15` | `DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH)` |
| `GenerateurFeuilleEngagement.FORMAT_TITRE` (String) | `core/reglement/GenerateurFeuilleEngagement.java:50` | `"%d. %s"` |
| `GenerateurFeuilleEngagement.POINTILLES` (String) | `core/reglement/GenerateurFeuilleEngagement.java:52` | `"……………………"` |
| `GenerateurFeuilleEngagement.CASE` (String) | `core/reglement/GenerateurFeuilleEngagement.java:53` | `"□"` |
| `GenerateurReglement.RENVOI_DROITS` (String) | `core/reglement/GenerateurReglement.java:273` | `"\u0000DROITS\u0000"` |
| `RenduPdfReglement.POLICE_NORMALE` (String) | `core/reglement/RenduPdfReglement.java:37` | `"/com/pingmanager/polices/Inter-Regular.ttf"` |
| `RenduPdfReglement.POLICE_GRASSE` (String) | `core/reglement/RenduPdfReglement.java:38` | `"/com/pingmanager/polices/Inter-SemiBold.ttf"` |
| `RenduPdfReglement.LARGEUR_PAGE` (float) | `core/reglement/RenduPdfReglement.java:40` | `PDRectangle.A4.getWidth()` |
| `RenduPdfReglement.HAUTEUR_PAGE` (float) | `core/reglement/RenduPdfReglement.java:41` | `PDRectangle.A4.getHeight()` |
| `RenduPdfReglement.MARGE_X` (float) | `core/reglement/RenduPdfReglement.java:42` | `56` |
| `RenduPdfReglement.MARGE_HAUT` (float) | `core/reglement/RenduPdfReglement.java:43` | `58` |
| `RenduPdfReglement.MARGE_BAS` (float) | `core/reglement/RenduPdfReglement.java:44` | `62` |
| `RenduPdfReglement.LARGEUR_UTILE` (float) | `core/reglement/RenduPdfReglement.java:45` | `LARGEUR_PAGE - 2 * MARGE_X` |
| `RenduPdfReglement.TEXTE` (float[]) | `core/reglement/RenduPdfReglement.java:47` | `rgb(0x2F4558)` |
| `RenduPdfReglement.DOUX` (float[]) | `core/reglement/RenduPdfReglement.java:48` | `rgb(0x5C6E7D)` |
| `RenduPdfReglement.PRIMAIRE` (float[]) | `core/reglement/RenduPdfReglement.java:49` | `rgb(0x335F8A)` |
| `RenduPdfReglement.FILET` (float[]) | `core/reglement/RenduPdfReglement.java:50` | `rgb(0xD4DEE7)` |
| `RenduPdfReglement.TROU` (float[]) | `core/reglement/RenduPdfReglement.java:51` | `rgb(0xFCE7B6)` |
| `CsvFederal.TAILLE_MAX_OCTETS` (int) | `importation/CsvFederal.java:33` | `10 * 1024 * 1024` |
| `CsvFederal.LONGUEUR_MAX_CELLULE` (int) | `importation/CsvFederal.java:35` | `200` |
| `CsvFederal.LIGNES_MAX` (int) | `importation/CsvFederal.java:37` | `20_000` |
| `CsvFederal.DATE` (DateTimeFormatter) | `importation/CsvFederal.java:39` | `DateTimeFormatter.ofPattern("dd/MM/uuuu", Locale.FRANCE) .withResolverStyle(ResolverStyle.STRICT)` |
| `ExportResultatsCsv.NOM_FORMAT` (String) | `importation/ExportResultatsCsv.java:29` | `"CSV provisoire PingManager v1"` |
| `ExportResultatsCsv.ENTETES_PARTIES` (List<String>) | `importation/ExportResultatsCsv.java:31` | `List.of("N° homologation", "Tableau", "Phase", "Étape", "N° partie", "Date", "Heure", "Table", "Licence A",…` |
| `ExportResultatsCsv.ENTETES_CLASSEMENTS` (List<String>) | `importation/ExportResultatsCsv.java:35` | `List.of("N° homologation", "Tableau", "Rang", "Licence", "Nom", "Prénom", "Club", "Points")` |
| `ExportResultatsCsv.DATE` (DateTimeFormatter) | `importation/ExportResultatsCsv.java:38` | `DateTimeFormatter.ofPattern("dd/MM/uuuu")` |
| `ExportResultatsCsv.HEURE` (DateTimeFormatter) | `importation/ExportResultatsCsv.java:39` | `DateTimeFormatter.ofPattern("HH:mm")` |
| `ExportResultatsCsv.MIME` (String) | `importation/ExportResultatsCsv.java:40` | `"text/csv"` |
| `LecteurClubsCsv.OBLIGATOIRES` (List<String>) | `importation/LecteurClubsCsv.java:29` | `List.of("N° club", "Nom")` |
| `LecteurClubsCsv.CLUB` (Pattern) | `importation/LecteurClubsCsv.java:30` | `Pattern.compile("[0-9]{7,8}")` |
| `LecteurClubsCsv.CODE_POSTAL` (Pattern) | `importation/LecteurClubsCsv.java:31` | `Pattern.compile("[0-9]{5}")` |
| `LecteurClubsCsv.DEPARTEMENT` (Pattern) | `importation/LecteurClubsCsv.java:32` | `Pattern.compile("[0-9]{2,3}\|2[AB]")` |
| `LecteurClubsCsv.COURRIEL` (Pattern) | `importation/LecteurClubsCsv.java:33` | `Pattern.compile("[^@\\s;,<>\"]+@[^@\\s;,<>\"]+\\.[^@\\s;,<>\"]+")` |
| `LecteurLicencesCsv.OBLIGATOIRES` (List<String>) | `importation/LecteurLicencesCsv.java:49` | `List.of("Licence", "Nom", "Prénom", "Sexe", "N° club", "Points")` |
| `LecteurLicencesCsv.LICENCE` (Pattern) | `importation/LecteurLicencesCsv.java:50` | `Pattern.compile("[0-9A-Za-z]{1,12}")` |
| `LecteurLicencesCsv.CLUB` (Pattern) | `importation/LecteurLicencesCsv.java:51` | `Pattern.compile("[0-9]{7,8}")` |
| `LecteurLicencesCsv.POINTS_MAX` (int) | `importation/LecteurLicencesCsv.java:52` | `100_000` |
| `LecteurTableur.MAX_LIGNES` (int) | `importation/LecteurTableur.java:147` | `20_000` |
| `LecteurTableur.MAX_COLONNES` (int) | `importation/LecteurTableur.java:148` | `200` |
| `LecteurTableur.NS_TABLE` (String) | `importation/LecteurTableur.java:487` | `"urn:oasis:names:tc:opendocument:xmlns:table:1.0"` |
| `LecteurTableur.NS_OFFICE` (String) | `importation/LecteurTableur.java:488` | `"urn:oasis:names:tc:opendocument:xmlns:office:1.0"` |
| `ModeleImport.ENTETES` (List<String>) | `importation/ModeleImport.java:34` | `List.of("Nom", "Prénom", "Points", "Club", "Licence", "Sexe", "Tableau", "Date de naissance")` |
| `ModeleImport.ENTETES_TABLEAUX` (List<String>) | `importation/ModeleImport.java:37` | `List.of("Nom du tableau", "Points min", "Points max", "Public", "Jour", "Heure", "Places", "Liste d'attente…` |
| `ModeleImport.ENTETES_JOUEURS` (List<String>) | `importation/ModeleImport.java:40` | `List.of("Nom", "Prénom", "Points", "Club", "Licence", "Sexe", "Catégorie", "Date de naissance", "Tableau 1"…` |
| `ModeleImport.CATEGORIES` (List<String>) | `importation/ModeleImport.java:43` | `List.of("Poussin", "Benjamin", "Minime", "Cadet", "Junior", "Senior", "Vétéran 40", "Vétéran 45", "Vétéran …` |
| `ModeleImport.LIGNES_TABLEAUX` (int) | `importation/ModeleImport.java:47` | `60` |
| `ModeleImport.LIGNES_JOUEURS` (int) | `importation/ModeleImport.java:48` | `400` |
| `ModeleImport.S_ENTETE_OBLIG` (int) | `importation/ModeleImport.java:51` | `1` |
| `ModeleImport.S_ENTETE_FACULT` (int) | `importation/ModeleImport.java:52` | `2` |
| `ModeleImport.S_TITRE` (int) | `importation/ModeleImport.java:53` | `3` |
| `ModeleImport.S_AIDE` (int) | `importation/ModeleImport.java:54` | `4` |
| `ModeleImport.S_SAISIE` (int) | `importation/ModeleImport.java:55` | `5` |
| `ModeleImport.S_DATE` (int) | `importation/ModeleImport.java:56` | `6` |
| `ModeleImport.S_HEURE` (int) | `importation/ModeleImport.java:57` | `7` |
| `ModeleImport.S_NOMBRE` (int) | `importation/ModeleImport.java:58` | `8` |
| `ModeleImport.S_EXEMPLE` (int) | `importation/ModeleImport.java:59` | `9` |
| `ModeleImport.CONTENT_TYPES` (String) | `importation/ModeleImport.java:336` | `"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" + "<Types xmlns=\"http://schemas.openxmlfor…` |
| `ModeleImport.RELS` (String) | `importation/ModeleImport.java:348` | `"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" + "<Relationships xmlns=\"http://schemas.op…` |
| `ModeleImport.STYLES` (String) | `importation/ModeleImport.java:355` | `"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" + "<styleSheet xmlns=\"http://schemas.openx…` |
| `AgendaCompte.ORDRE` (Comparator<EvenementAgenda>) | `model/AgendaCompte.java:22` | `Comparator .comparing(EvenementAgenda::getDate) .thenComparing(e -> e.getHeure(), Comparator.nullsFirst(Com…` |
| `ArticleOptionnel.HORAIRES` (String) | `model/ArticleOptionnel.java:44` | `"horaires"` |
| `ArticleOptionnel.HEURE` (String) | `model/ArticleOptionnel.java:45` | `"heure"` |
| `ArticleOptionnel.MINUTES` (String) | `model/ArticleOptionnel.java:46` | `"minutes"` |
| `ArticleOptionnel.CATEGORIE` (String) | `model/ArticleOptionnel.java:47` | `"categorie"` |
| `ArticleOptionnel.PARKING` (String) | `model/ArticleOptionnel.java:48` | `"parking"` |
| `ArticleOptionnel.VESTIAIRES` (String) | `model/ArticleOptionnel.java:49` | `"vestiaires"` |
| `ArticleOptionnel.PMR` (String) | `model/ArticleOptionnel.java:50` | `"pmr"` |
| `ArticleOptionnel.TITRE` (String) | `model/ArticleOptionnel.java:51` | `"titre"` |
| `ArticleOptionnel.TEXTE` (String) | `model/ArticleOptionnel.java:52` | `"texte"` |
| `ConvocationsSms.LIMITE_PAR_DEFAUT` (int) | `model/ConvocationsSms.java:24` | `300` |
| `ConvocationsSms.LIMITE_MAXIMALE` (int) | `model/ConvocationsSms.java:26` | `5000` |
| `Coordonnees.RAYON_TERRE_KM` (double) | `model/Coordonnees.java:12` | `6371.0` |
| `DocumentReglement.A_COMPLETER` (Pattern) | `model/DocumentReglement.java:21` | `Pattern.compile("\\[à compléter[^\\]]*\\]")` |
| `EchelonHomologation.SEUIL_DOTATION_FEDERALE` (double) | `model/EchelonHomologation.java:26` | `3000.0` |
| `ElementPlan.TAILLE` (int) | `model/ElementPlan.java:46` | `2` |
| `Joueur.RANG_NATIONAL_INCONNU` (int) | `model/Joueur.java:42` | `0` |
| `Joueur.LICENCE_A_DETERMINER` (String) | `model/Joueur.java:45` | `"__A_DETERMINER__"` |
| `PlanSalle.COTE_MAXIMAL` (int) | `model/PlanSalle.java:29` | `120` |
| `QuotasTableaux.SANS_LIMITE` (Quota) | `model/QuotasTableaux.java:44` | `new Quota(null, null)` |
| `ReglagesAnnoncesVocales.DIFFUSIONS_PAR_DEFAUT` (int) | `model/ReglagesAnnoncesVocales.java:16` | `2` |
| `ReglagesAnnoncesVocales.DIFFUSIONS_MAX` (int) | `model/ReglagesAnnoncesVocales.java:18` | `3` |
| `ReglagesAnnoncesVocales.DEBIT_MIN` (int) | `model/ReglagesAnnoncesVocales.java:20` | `-10` |
| `ReglagesAnnoncesVocales.DEBIT_MAX` (int) | `model/ReglagesAnnoncesVocales.java:21` | `10` |
| `ReglagesAnnoncesVocales.RAPPEL_MAX_MINUTES` (int) | `model/ReglagesAnnoncesVocales.java:23` | `30` |
| `StrategieOrdonnancement.PAR_DEFAUT` (StrategieOrdonnancement) | `model/StrategieOrdonnancement.java:50` | `MINIMISER_ATTENTE` |
| `FormatPoule3Qualifie2.INSTANCE` (FormatPoule3Qualifie2) | `model/format/FormatPoule3Qualifie2.java:25` | `new FormatPoule3Qualifie2()` |
| `RegistreFormats.FORMATS` (List<FormatTournoi>) | `model/format/RegistreFormats.java:19` | `List.of( FormatPoule3Qualifie2.INSTANCE // Les futurs formats (poule de 4, avec consolante, etc.) s'ajouten…` |
| `LoiDureeMatch.RAPPORT_MINIMUM` (double) | `ordonnancement/LoiDureeMatch.java:28` | `0.5` |
| `LoiDureeMatch.RAPPORT_MAXIMUM` (double) | `ordonnancement/LoiDureeMatch.java:29` | `2.5` |
| `ReservationsTables.AUCUNE` (ReservationsTables) | `ordonnancement/ReservationsTables.java:29` | `builder().construire()` |
| `TempsAnnexesMatch.PAR_DEFAUT` (TempsAnnexesMatch) | `ordonnancement/TempsAnnexesMatch.java:31` | `builder().build()` |
| `TempsAnnexesMatch.AUCUN` (TempsAnnexesMatch) | `ordonnancement/TempsAnnexesMatch.java:34` | `builder().appel(0).probabiliteRappel(0).rappel(0) .echauffement(0).pauseEntreManches(0).probabiliteTempsMor…` |
| `TempsAnnexesMatch.DUREE_TEMPS_MORT` (int) | `ordonnancement/TempsAnnexesMatch.java:38` | `60` |
| `DepotApplication.VERSION` (int) | `persistence/DepotApplication.java:58` | `1` |
| `DepotApplication.NOM_FICHIER` (String) | `persistence/DepotApplication.java:61` | `"pingmanager-application.json"` |
| `DepotApplication.MAX_RECENTS` (int) | `persistence/DepotApplication.java:76` | `12` |
| `DepotTournoi.EXTENSION` (String) | `persistence/DepotTournoi.java:33` | `".ptt.json"` |
| `ServiceAutosave.FREQUENCE_PAR_DEFAUT` (Duration) | `persistence/ServiceAutosave.java:39` | `Duration.ofMinutes(15)` |
| `ServiceAutosave.SUFFIXE_AUTOSAVE` (String) | `persistence/ServiceAutosave.java:42` | `".autosave.json"` |
| `ServiceAutosave.SUFFIXE_MARQUEUR` (String) | `persistence/ServiceAutosave.java:45` | `".cloture"` |
| `SessionMapperComplet.VERSION` (int) | `persistence/SessionMapperComplet.java:28` | `1` |
| `TournoiMapper.VERSION` (int) | `persistence/TournoiMapper.java:54` | `1` |
| `ContexteApplication.VERSION_DEMO` (String) | `ui/ContexteApplication.java:239` | `".demo-brunoy-v19"` |
| `ContexteApplication.ANCIENNES_DEMOS` (List<String>) | `ui/ContexteApplication.java:242` | `List.of("Demo_Jour_J_Brunoy_2026", "Demo_Regional_Creteil_2026", "Demo_Departemental_Loiret_2027", "Demo_Na…` |
| `ContexteApplication.MARQUEUR_NETTOYAGE` (String) | `ui/ContexteApplication.java:280` | `".nettoyage-brunoy-2026-10-21"` |
| `DiffusionSuivi.PORT_PAR_DEFAUT` (int) | `ui/diffusion/DiffusionSuivi.java:23` | `8080` |
| `DiffusionSuivi.PORT_MIN` (int) | `ui/diffusion/DiffusionSuivi.java:25` | `1024` |
| `DiffusionSuivi.PORT_MAX` (int) | `ui/diffusion/DiffusionSuivi.java:26` | `65535` |
| `Html.LONGUEUR_MAX_PARAMETRE` (int) | `ui/diffusion/Html.java:12` | `60` |
| `Html.LONGUEUR_MAX_REQUETE` (int) | `ui/diffusion/Html.java:14` | `512` |
| `Html.NOMBRE_MAX_PARAMETRES` (int) | `ui/diffusion/Html.java:16` | `8` |
| `InstantaneSuivi.MAX_A_VENIR` (int) | `ui/diffusion/InstantaneSuivi.java:56` | `30` |
| `PagesSuivi.RAFRAICHISSEMENT_SECONDES` (int) | `ui/diffusion/PagesSuivi.java:30` | `30` |
| `PagesSuivi.MAX_RESULTATS` (int) | `ui/diffusion/PagesSuivi.java:32` | `20` |
| `PagesSuivi.TYPE_HTML` (String) | `ui/diffusion/PagesSuivi.java:34` | `"text/html; charset=utf-8"` |
| `PagesSuivi.TYPE_TEXTE` (String) | `ui/diffusion/PagesSuivi.java:35` | `"text/plain; charset=utf-8"` |
| `PagesSuivi.CSS` (String) | `ui/diffusion/PagesSuivi.java:357` | `":root{--fond:#F5F7FB;--surface:#FFFFFF;--bordure:#DCE3F0;--texte:#0F1B3D;" + "--doux:#5B6785;--primaire:#1…` |
| `ServeurSuiviPublic.LONGUEUR_MAX_CHEMIN` (int) | `ui/diffusion/ServeurSuiviPublic.java:39` | `128` |
| `ServeurSuiviPublic.FILS` (int) | `ui/diffusion/ServeurSuiviPublic.java:40` | `16` |
| `ServeurSuiviPublic.DELAI_REQUETE_S` (int) | `ui/diffusion/ServeurSuiviPublic.java:42` | `5` |
| `ServeurSuiviPublic.DELAI_REPONSE_S` (int) | `ui/diffusion/ServeurSuiviPublic.java:44` | `15` |
| `ServeurSuiviPublic.DELAI_INACTIVITE_S` (int) | `ui/diffusion/ServeurSuiviPublic.java:46` | `30` |
| `ServeurSuiviPublic.JOURNAL` (System.Logger) | `ui/diffusion/ServeurSuiviPublic.java:48` | `System.getLogger(ServeurSuiviPublic.class.getName())` |
| `AssistantInscription.ETAPES` (String[]) | `ui/ecran/AssistantInscription.java:52` | `{"Tableaux", "Récapitulatif", "Paiement", "Confirmation"}` |
| `Avatars.NOMBRE_TEINTES` (int) | `ui/ecran/Avatars.java:13` | `5` |
| `CarteReglesInscription.SANS_CONDITION` (String) | `ui/ecran/CarteReglesInscription.java:61` | `"Sans condition (n'importe quel tableau)"` |
| `DispositionArbre.TITRE_PETITE_FINALE` (String) | `ui/ecran/DispositionArbre.java:33` | `"Petite finale (3e place)"` |
| `EcranCompletion.ELEMENTS_PAR_BLOC` (int) | `ui/ecran/EcranCompletion.java:372` | `5` |
| `EcranCompletion.ICONES_SECTIONS` (String[]) | `ui/ecran/EcranCompletion.java:374` | `{Icones.FICHE, Icones.TABLEAUX, Icones.RAQUETTE, Icones.DOCUMENT}` |
| `EcranImportJoueurs.NOUVEAU` (String) | `ui/ecran/EcranImportJoueurs.java:44` | `"Nouveau tableau…"` |
| `EcranPlanSalle.CASE` (double) | `ui/ecran/EcranPlanSalle.java:57` | `30` |
| `EcranPlanSalle.PREFIXE_GLISSER` (String) | `ui/ecran/EcranPlanSalle.java:58` | `"plan-salle:"` |
| `EcranPlanSalle.SANS_RESERVATION` (String) | `ui/ecran/EcranPlanSalle.java:456` | `"Aucune réservation"` |
| `EcranSalle.FREQUENCE_CHRONO` (Duration) | `ui/ecran/EcranSalle.java:100` | `Duration.seconds(20)` |
| `EcranSalle.COLONNE_TABLE` (double) | `ui/ecran/EcranSalle.java:606` | `115` |
| `EcranSalle.CASE_ETROITE` (double) | `ui/ecran/EcranSalle.java:608` | `26` |
| `EcranSportif.MAX_RESULTATS` (int) | `ui/ecran/EcranSportif.java:64` | `8` |
| `EcranTableaux.COLONNES` (String[]) | `ui/ecran/EcranTableaux.java:383` | `{"Nom", "Jour", "Début", "Points", "Public", "Places", "Prix", "Récompenses", ""}` |
| `FormulaireTableau.NON_NUMEROTES` (String) | `ui/ecran/FormulaireTableau.java:60` | `"Non numérotés"` |
| `FormulaireTableau.OPEN` (String) | `ui/ecran/FormulaireTableau.java:61` | `"Open"` |
| `FormulaireTableau.FEMININ` (String) | `ui/ecran/FormulaireTableau.java:62` | `"Féminin"` |
| `FormulaireTableau.SEXE_MIXTE` (String) | `ui/ecran/FormulaireTableau.java:98` | `"Mixte (ouvert à tous)"` |
| `FormulaireTableau.SEXE_HOMMES` (String) | `ui/ecran/FormulaireTableau.java:99` | `"Hommes uniquement"` |
| `FormulaireTableau.SEXE_FEMMES` (String) | `ui/ecran/FormulaireTableau.java:100` | `"Femmes uniquement"` |
| `FormulaireTableau.ARGENT` (String) | `ui/ecran/FormulaireTableau.java:436` | `"Argent (€)"` |
| `FormulaireTableau.LOT` (String) | `ui/ecran/FormulaireTableau.java:437` | `"Lot"` |
| `FormulaireTableau.REDUCTION` (String) | `ui/ecran/FormulaireTableau.java:438` | `"Réduction (%)"` |
| `FormulaireTableau.PART` (String) | `ui/ecran/FormulaireTableau.java:439` | `"% des inscriptions"` |
| `FormulairesSection.DT` (DateTimeFormatter) | `ui/ecran/FormulairesSection.java:47` | `DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")` |
| `Icones.ACCUEIL` (String) | `ui/ecran/Icones.java:13` | `"M8 1.5 L15 7.5 L13.5 7.5 L13.5 14.5 L9.5 14.5 L9.5 10 L6.5 10 " + "L6.5 14.5 L2.5 14.5 L2.5 7.5 L1 7.5 Z"` |
| `Icones.PROFIL` (String) | `ui/ecran/Icones.java:15` | `"M8 1.5 A3.25 3.25 0 1 1 7.99 1.5 Z M1.5 14.5 C1.5 10.5 4.5 9 8 9 " + "C11.5 9 14.5 10.5 14.5 14.5 Z"` |
| `Icones.TOURNOIS` (String) | `ui/ecran/Icones.java:17` | `"M8 1 C5.2 1 3 3.2 3 6 C3 9.8 8 15 8 15 C8 15 13 9.8 13 6 C13 3.2 10.8 1 8 1 Z " + "M8 3.8 A2.2 2.2 0 1 0 8…` |
| `Icones.STATS` (String) | `ui/ecran/Icones.java:19` | `"M1.5 14.5 L1.5 8.5 L4.5 8.5 L4.5 14.5 Z M6.5 14.5 L6.5 3 L9.5 3 L9.5 14.5 Z " + "M11.5 14.5 L11.5 6 L14.5 …` |
| `Icones.GESTION` (String) | `ui/ecran/Icones.java:21` | `"M1.5 3 L14.5 3 L14.5 14.5 L1.5 14.5 Z M3 6.5 L3 13 L13 13 L13 6.5 Z " + "M4.5 1 L6 1 L6 4.5 L4.5 4.5 Z M10…` |
| `Icones.HISTORIQUE` (String) | `ui/ecran/Icones.java:23` | `"M8 1 A7 7 0 1 1 7.99 1 Z M8 2.6 A5.4 5.4 0 1 0 8.01 2.6 Z " + "M7.2 4 L8.8 4 L8.8 7.6 L11.2 9.2 L10.4 10.5…` |
| `Icones.BUVETTE` (String) | `ui/ecran/Icones.java:25` | `"M2.5 4 L11 4 L11 10 C11 12.4 9.3 14 6.75 14 C4.2 14 2.5 12.4 2.5 10 Z " + "M11 5.5 L12.5 5.5 C14.3 5.5 14.…` |
| `Icones.AGENDA` (String) | `ui/ecran/Icones.java:28` | `"M1.5 3 L14.5 3 L14.5 14.5 L1.5 14.5 Z M3 6.5 L3 13 L13 13 L13 6.5 Z " + "M4.5 1 L6 1 L6 4.5 L4.5 4.5 Z M10…` |
| `Icones.SUPERVISION` (String) | `ui/ecran/Icones.java:30` | `"M8 1 L14 3.5 L14 8 C14 11.4 11.4 14 8 15 C4.6 14 2 11.4 2 8 L2 3.5 Z"` |
| `Icones.CADENAS` (String) | `ui/ecran/Icones.java:32` | `"M3 7 L13 7 L13 14.5 L3 14.5 Z M5 7 L5 5 C5 1.2 11 1.2 11 5 L11 7 L9.5 7 L9.5 5 " + "C9.5 3.2 6.5 3.2 6.5 5…` |
| `Icones.SALLE` (String) | `ui/ecran/Icones.java:35` | `"M1.5 2 L14.5 2 L14.5 14 L1.5 14 Z M3 3.5 L3 12.5 L13 12.5 L13 3.5 Z " + "M4.5 5 L7 5 L7 7 L4.5 7 Z M9 5 L1…` |
| `Icones.STRATEGIE` (String) | `ui/ecran/Icones.java:38` | `"M1.5 2.5 L9 2.5 L9 5 L1.5 5 Z M5 6.75 L13 6.75 L13 9.25 L5 9.25 Z " + "M8 11 L14.5 11 L14.5 13.5 L8 13.5 Z"` |
| `Icones.FINANCES` (String) | `ui/ecran/Icones.java:41` | `"M1.5 11.5 L8.5 11.5 L8.5 14 L1.5 14 Z M2.5 8 L9.5 8 L9.5 10.5 L2.5 10.5 Z " + "M1.5 4.5 L8.5 4.5 L8.5 7 L1…` |
| `Icones.FICHE` (String) | `ui/ecran/Icones.java:45` | `"M3 1 L13 1 L13 15 L3 15 Z M4.5 2.5 L4.5 13.5 L11.5 13.5 L11.5 2.5 Z " + "M6 5 L10 5 L10 6.2 L6 6.2 Z M6 7.…` |
| `Icones.TABLEAUX` (String) | `ui/ecran/Icones.java:48` | `"M1.5 1.5 L7 1.5 L7 7 L1.5 7 Z M9 1.5 L14.5 1.5 L14.5 7 L9 7 Z " + "M1.5 9 L7 9 L7 14.5 L1.5 14.5 Z M9 9 L1…` |
| `Icones.RAQUETTE` (String) | `ui/ecran/Icones.java:51` | `"M8 0.5 A5.25 5.25 0 1 1 7.99 0.5 Z M6.75 11.4 L9.25 11.4 L9.25 15.5 L6.75 15.5 Z"` |
| `Icones.DOCUMENT` (String) | `ui/ecran/Icones.java:53` | `"M3 1 L10 1 L13.5 4.5 L13.5 15 L3 15 Z M4.5 2.5 L4.5 13.5 L12 13.5 L12 5.5 " + "L9 5.5 L9 2.5 Z"` |
| `ImpressionJourJ.FEUILLES` (GenerateurFeuilles) | `ui/ecran/ImpressionJourJ.java:27` | `new GenerateurFeuilles()` |
| `PageAccueil.JOUR` (DateTimeFormatter) | `ui/ecran/PageAccueil.java:34` | `DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH)` |
| `PageSupervision.JOUR` (DateTimeFormatter) | `ui/ecran/PageSupervision.java:32` | `DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH)` |
| `PresentationAccueil.PROFIL` (String) | `ui/ecran/PresentationAccueil.java:25` | `"profil"` |
| `PresentationAccueil.TOURNOIS` (String) | `ui/ecran/PresentationAccueil.java:26` | `"tournois"` |
| `PresentationAccueil.AGENDA` (String) | `ui/ecran/PresentationAccueil.java:27` | `"agenda"` |
| `PresentationAccueil.STATS` (String) | `ui/ecran/PresentationAccueil.java:28` | `"stats"` |
| `PresentationAccueil.GESTION` (String) | `ui/ecran/PresentationAccueil.java:29` | `"gestion"` |
| `PresentationAccueil.HISTORIQUE` (String) | `ui/ecran/PresentationAccueil.java:30` | `"historique"` |
| `PresentationAccueil.BUVETTE` (String) | `ui/ecran/PresentationAccueil.java:31` | `"buvette"` |
| `PresentationAccueil.DATES_MAX` (int) | `ui/ecran/PresentationAccueil.java:34` | `3` |
| `PresentationAccueil.DATE_COURTE` (DateTimeFormatter) | `ui/ecran/PresentationAccueil.java:36` | `DateTimeFormatter.ofPattern("EEE d MMM", Locale.FRENCH)` |
| `PresentationCarteTournois.TITRE_ONGLET` (String) | `ui/ecran/PresentationCarteTournois.java:28` | `"Carte des tournois"` |
| `PresentationCarteTournois.VOIR_PLUS` (String) | `ui/ecran/PresentationCarteTournois.java:29` | `"Voir plus"` |
| `PresentationCarteTournois.AUCUN_RESULTAT` (String) | `ui/ecran/PresentationCarteTournois.java:30` | `"Aucun tournoi ne correspond à ces filtres."` |
| `PresentationCarteTournois.JOUR_SANS_DATE` (String) | `ui/ecran/PresentationCarteTournois.java:31` | `"Dates à préciser"` |
| `PresentationConvocationsSms.HEURE` (DateTimeFormatter) | `ui/ecran/PresentationConvocationsSms.java:34` | `DateTimeFormatter.ofPattern("dd/MM HH:mm", Locale.FRANCE)` |
| `PresentationConvocationsSms.JOURNAL_AFFICHE` (int) | `ui/ecran/PresentationConvocationsSms.java:49` | `50` |
| `PresentationConvocationsSms.EXPEDITEUR_REFUSE` (String) | `ui/ecran/PresentationConvocationsSms.java:51` | `"Expéditeur : 3 à 11 lettres ou chiffres, sans espace ni accent, avec " + "au moins une lettre (le nom du c…` |
| `PresentationGestionTournois.TITRE` (String) | `ui/ecran/PresentationGestionTournois.java:31` | `"Gestion des tournois"` |
| `PresentationGestionTournois.BOUTON_CREER` (String) | `ui/ecran/PresentationGestionTournois.java:32` | `"Créer un tournoi"` |
| `PresentationGestionTournois.AIDE_CREER` (String) | `ui/ecran/PresentationGestionTournois.java:33` | `"Un brouillon en quelques champs : nom, club, niveau et dates."` |
| `PresentationGestionTournois.LIEN_HISTORIQUE` (String) | `ui/ecran/PresentationGestionTournois.java:34` | `"Historique des tournois passés"` |
| `PresentationGestionTournois.TITRE_ACTUEL` (String) | `ui/ecran/PresentationGestionTournois.java:35` | `"Tournoi actuel"` |
| `PresentationGestionTournois.SURTITRE_EN_COURS` (String) | `ui/ecran/PresentationGestionTournois.java:36` | `"TOURNOI EN COURS"` |
| `PresentationGestionTournois.SURTITRE_PROCHAIN` (String) | `ui/ecran/PresentationGestionTournois.java:37` | `"PROCHAIN TOURNOI"` |
| `PresentationGestionTournois.ACTUEL_VIDE` (String) | `ui/ecran/PresentationGestionTournois.java:38` | `"Aucun tournoi en cours pour le moment. Dès qu'un tournoi est " + "publié, il apparaît ici avec l'accès au …` |
| `PresentationGestionTournois.BOUTON_JOUR_J` (String) | `ui/ecran/PresentationGestionTournois.java:40` | `"Ouvrir le jour J"` |
| `PresentationGestionTournois.TITRE_FRISE` (String) | `ui/ecran/PresentationGestionTournois.java:41` | `"Vos tournois, étape par étape"` |
| `PresentationGestionTournois.AIDE_FRISE` (String) | `ui/ecran/PresentationGestionTournois.java:42` | `"Un tournoi monte d'une étape à chaque validation : " + "brouillon, demande d'homologation, homologation, p…` |
| `PresentationGestionTournois.ETAPE_VIDE` (String) | `ui/ecran/PresentationGestionTournois.java:44` | `"Aucun tournoi à cette étape."` |
| `PresentationGestionTournois.PASTILLE_ACTUEL` (String) | `ui/ecran/PresentationGestionTournois.java:45` | `"Tournoi actuel"` |
| `PresentationGestionTournois.PASTILLE_JOUR_J` (String) | `ui/ecran/PresentationGestionTournois.java:46` | `"Jour J commencé"` |
| `PresentationGestionTournois.PASTILLE_IMPORTE` (String) | `ui/ecran/PresentationGestionTournois.java:47` | `"Tournoi importé"` |
| `PresentationGestionTournois.PASTILLE_MODE` (String) | `ui/ecran/PresentationGestionTournois.java:48` | `"Mode à choisir"` |
| `PresentationGestionTournois.TITRE_HISTORIQUE` (String) | `ui/ecran/PresentationGestionTournois.java:50` | `"Historique des tournois"` |
| `PresentationGestionTournois.AIDE_HISTORIQUE` (String) | `ui/ecran/PresentationGestionTournois.java:51` | `"Les tournois du club dont la date est passée, et les tournois annulés."` |
| `PresentationGestionTournois.HISTORIQUE_VIDE` (String) | `ui/ecran/PresentationGestionTournois.java:52` | `"Aucun tournoi passé pour l'instant."` |
| `PresentationGestionTournois.RETOUR` (String) | `ui/ecran/PresentationGestionTournois.java:53` | `"← Gestion des tournois"` |
| `PresentationGestionTournois.JOUR` (DateTimeFormatter) | `ui/ecran/PresentationGestionTournois.java:55` | `DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH)` |
| `PresentationGestionTournois.PAR_DATE` (Comparator<Fiche>) | `ui/ecran/PresentationGestionTournois.java:95` | `Comparator .comparing(Fiche::debut, Comparator.nullsLast(Comparator.naturalOrder())) .thenComparing(Fiche::…` |
| `PresentationInscriptionSurPlace.TITRE` (String) | `ui/ecran/PresentationInscriptionSurPlace.java:21` | `"Inscrire sur place"` |
| `PresentationJourJ.ISSUES_NON_NORMALES` (List<IssueMatch>) | `ui/ecran/PresentationJourJ.java:474` | `List.of( IssueMatch.FORFAIT, IssueMatch.ABANDON, IssueMatch.REFUS_DE_JOUER, IssueMatch.DISQUALIFICATION)` |
| `PresentationMesTournois.TITRE_EN_COURS` (String) | `ui/ecran/PresentationMesTournois.java:32` | `"Tournoi en cours"` |
| `PresentationMesTournois.TITRE_INSCRIPTIONS` (String) | `ui/ecran/PresentationMesTournois.java:33` | `"Mes inscriptions"` |
| `PresentationMesTournois.TITRE_AGENDA` (String) | `ui/ecran/PresentationMesTournois.java:34` | `"Mon agenda"` |
| `PresentationMesTournois.TITRE_PASSES` (String) | `ui/ecran/PresentationMesTournois.java:35` | `"Tournois passés"` |
| `PresentationNouveauTournoi.TITRE` (String) | `ui/ecran/PresentationNouveauTournoi.java:19` | `"Nouveau tournoi"` |
| `PresentationNouveauTournoi.TEXTE_EXPLICATIF` (String) | `ui/ecran/PresentationNouveauTournoi.java:22` | `"Ces informations sont obligatoires pour demander " + "l'homologation du tournoi, ou simplement pour le gér…` |
| `PresentationNouveauTournoi.TITRE_CARTE_TOURNOI` (String) | `ui/ecran/PresentationNouveauTournoi.java:27` | `"Le tournoi"` |
| `PresentationNouveauTournoi.TITRE_CARTE_DATES` (String) | `ui/ecran/PresentationNouveauTournoi.java:28` | `"Dates"` |
| `PresentationNouveauTournoi.LIBELLE_CLUB` (String) | `ui/ecran/PresentationNouveauTournoi.java:30` | `"Club organisateur"` |
| `PresentationNouveauTournoi.LIBELLE_NOM` (String) | `ui/ecran/PresentationNouveauTournoi.java:31` | `"Nom du tournoi"` |
| `PresentationNouveauTournoi.LIBELLE_NIVEAU` (String) | `ui/ecran/PresentationNouveauTournoi.java:32` | `"Niveau du tournoi (catégorie)"` |
| `PresentationNouveauTournoi.LIBELLE_DEBUT` (String) | `ui/ecran/PresentationNouveauTournoi.java:33` | `"Date de début"` |
| `PresentationNouveauTournoi.LIBELLE_FIN` (String) | `ui/ecran/PresentationNouveauTournoi.java:34` | `"Date de fin"` |
| `PresentationNouveauTournoi.FORMAT_DATE` (String) | `ui/ecran/PresentationNouveauTournoi.java:36` | `"jj/mm/aaaa"` |
| `PresentationNouveauTournoi.AIDE_CALENDRIER` (String) | `ui/ecran/PresentationNouveauTournoi.java:37` | `"Choisir dans le calendrier"` |
| `PresentationNouveauTournoi.NOTE_DATES` (String) | `ui/ecran/PresentationNouveauTournoi.java:39` | `"Du premier au dernier jour de l'ensemble du tournoi, " + "toutes journées comprises. Tapez la date ou cliq…` |
| `PresentationNouveauTournoi.BOUTON_CREER` (String) | `ui/ecran/PresentationNouveauTournoi.java:43` | `"Créer le brouillon"` |
| `PresentationNouveauTournoi.BOUTON_ANNULER` (String) | `ui/ecran/PresentationNouveauTournoi.java:44` | `"Annuler"` |
| `PresentationNouveauTournoi.MOTIF_NOM` (String) | `ui/ecran/PresentationNouveauTournoi.java:46` | `"Donnez un nom au tournoi."` |
| `PresentationNouveauTournoi.MOTIF_CLUB` (String) | `ui/ecran/PresentationNouveauTournoi.java:47` | `"Aucun club rattaché à votre compte : rattachez-en un " + "depuis « Mon profil »."` |
| `PresentationNouveauTournoi.MOTIF_DEBUT_VIDE` (String) | `ui/ecran/PresentationNouveauTournoi.java:49` | `"Indiquez la date de début (" + FORMAT_DATE + ")."` |
| `PresentationNouveauTournoi.MOTIF_DEBUT_ILLISIBLE` (String) | `ui/ecran/PresentationNouveauTournoi.java:50` | `"La date de début n'est pas valide (" + FORMAT_DATE + ")."` |
| `PresentationNouveauTournoi.MOTIF_FIN_ILLISIBLE` (String) | `ui/ecran/PresentationNouveauTournoi.java:51` | `"La date de fin n'est pas valide (" + FORMAT_DATE + ")."` |
| `PresentationNouveauTournoi.MOTIF_FIN_AVANT_DEBUT` (String) | `ui/ecran/PresentationNouveauTournoi.java:52` | `"La date de fin ne peut pas précéder la date de début."` |
| `PresentationPhaseFinale.LIBELLE_EXEMPT` (String) | `ui/ecran/PresentationPhaseFinale.java:30` | `"Exempt"` |
| `PresentationPhaseFinale.AIDE_EXEMPT` (String) | `ui/ecran/PresentationPhaseFinale.java:32` | `"Sans adversaire : qualifié d'office."` |
| `PresentationPhaseFinale.A_DETERMINER` (String) | `ui/ecran/PresentationPhaseFinale.java:34` | `"À déterminer"` |
| `PresentationPlanSalle.TITRE_CARTE` (String) | `ui/ecran/PresentationPlanSalle.java:31` | `"Plan de la salle"` |
| `PresentationPreparationTournoi.SECTION_GENERAL` (int) | `ui/ecran/PresentationPreparationTournoi.java:36` | `0` |
| `PresentationPreparationTournoi.SECTION_TABLEAUX` (int) | `ui/ecran/PresentationPreparationTournoi.java:37` | `1` |
| `PresentationPreparationTournoi.SECTION_SPORTIF` (int) | `ui/ecran/PresentationPreparationTournoi.java:38` | `2` |
| `PresentationPreparationTournoi.SECTION_COMPLEMENTAIRE` (int) | `ui/ecran/PresentationPreparationTournoi.java:39` | `3` |
| `PresentationPreparationTournoi.FUSEAU` (ZoneId) | `ui/ecran/PresentationPreparationTournoi.java:41` | `ZoneId.of("Europe/Paris")` |
| `PresentationPreparationTournoi.JOUR` (DateTimeFormatter) | `ui/ecran/PresentationPreparationTournoi.java:42` | `DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH'h'mm")` |
| `PresentationPreparationTournoi.PARTIES_LIVREES` (List<String>) | `ui/ecran/PresentationPreparationTournoi.java:128` | `List.of("B024", "B025", "B036")` |
| `PresentationPrevisions.TITRE` (String) | `ui/ecran/PresentationPrevisions.java:27` | `"Prévisions et stratégie"` |
| `PresentationPrevisions.TITRE_CARTE` (String) | `ui/ecran/PresentationPrevisions.java:28` | `"Stratégie d'ordonnancement"` |
| `PresentationPrevisions.TABLES_PAR_DEFAUT` (int) | `ui/ecran/PresentationPrevisions.java:29` | `16` |
| `PresentationPrevisions.ERREUR_TABLES` (String) | `ui/ecran/PresentationPrevisions.java:30` | `"Indiquez un nombre de tables entre 1 et 200."` |
| `PresentationPrevisions.ERREUR_GRAINE` (String) | `ui/ecran/PresentationPrevisions.java:31` | `"La graine doit être un nombre entier (par exemple 2027)."` |
| `PresentationPrevisions.AVERTISSEMENT` (String) | `ui/ecran/PresentationPrevisions.java:32` | `"Prévision indicative : les parties sont simulées avec la durée estimée " + "et un vainqueur tiré selon les…` |
| `PresentationPrevisions.TIRAGES_PAR_DEFAUT` (int) | `ui/ecran/PresentationPrevisions.java:239` | `100` |
| `PresentationPrevisions.ERREUR_TIRAGES` (String) | `ui/ecran/PresentationPrevisions.java:240` | `"Indiquez un nombre de tirages entre 1 et 1000."` |
| `PresentationPrevisions.ERREUR_DUREES` (String) | `ui/ecran/PresentationPrevisions.java:241` | `"Indiquez les durées d'un match en minutes (nombres entiers)."` |
| `PresentationPrevisions.DUREE_MOYENNE` (int) | `ui/ecran/PresentationPrevisions.java:243` | `20` |
| `PresentationPrevisions.DUREE_MINIMUM` (int) | `ui/ecran/PresentationPrevisions.java:244` | `10` |
| `PresentationPrevisions.DUREE_MAXIMUM` (int) | `ui/ecran/PresentationPrevisions.java:245` | `50` |
| `PresentationPrevisions.AVERTISSEMENT_STATISTIQUE` (String) | `ui/ecran/PresentationPrevisions.java:246` | `"Le tournoi est rejoué de nombreuses fois : à chaque " + "tirage, d'autres poules, d'autres vainqueurs (sel…` |
| `PresentationPrevisions.ERREUR_ANNEXES` (String) | `ui/ecran/PresentationPrevisions.java:302` | `"Indiquez les temps autour du match en nombres (minutes, secondes " + "ou pourcentages, par exemple 1,5)."` |
| `PresentationPrevisions.APPEL` (String) | `ui/ecran/PresentationPrevisions.java:306` | `"1"` |
| `PresentationPrevisions.RAPPEL_POURCENT` (String) | `ui/ecran/PresentationPrevisions.java:307` | `"15"` |
| `PresentationPrevisions.RAPPEL_MINUTES` (String) | `ui/ecran/PresentationPrevisions.java:308` | `"3"` |
| `PresentationPrevisions.ECHAUFFEMENT` (String) | `ui/ecran/PresentationPrevisions.java:309` | `"2"` |
| `PresentationPrevisions.PAUSE_SECONDES` (String) | `ui/ecran/PresentationPrevisions.java:310` | `"30"` |
| `PresentationPrevisions.TEMPS_MORT_POURCENT` (String) | `ui/ecran/PresentationPrevisions.java:311` | `"20"` |
| `PresentationPrevisions.FEUILLE` (String) | `ui/ecran/PresentationPrevisions.java:312` | `"1,5"` |
| `PresentationPrevisions.ECART_NEGLIGEABLE` (int) | `ui/ecran/PresentationPrevisions.java:356` | `5` |
| `PresentationPrevisions.FIN_VISEE_PAR_DEFAUT` (String) | `ui/ecran/PresentationPrevisions.java:640` | `"20:00"` |
| `PresentationPrevisions.ERREUR_FIN_VISEE` (String) | `ui/ecran/PresentationPrevisions.java:641` | `"Indiquez l'heure de fin visée sous la forme 20:00 (ou 20 h 30)."` |
| `PresentationProfil.DATE_FR` (DateTimeFormatter) | `ui/ecran/PresentationProfil.java:29` | `DateTimeFormatter.ofPattern("dd/MM/yyyy")` |
| `PresentationProfil.NON_RENSEIGNE` (String) | `ui/ecran/PresentationProfil.java:32` | `"Non renseigné"` |
| `PresentationProfil.TITRE` (String) | `ui/ecran/PresentationProfil.java:77` | `"Mon profil"` |
| `PresentationProfil.TITRE_IDENTITE` (String) | `ui/ecran/PresentationProfil.java:78` | `"Mes informations"` |
| `PresentationProfil.TITRE_LICENCE` (String) | `ui/ecran/PresentationProfil.java:79` | `"Licence FFTT officielle"` |
| `PresentationProfil.TITRE_TOURNOI` (String) | `ui/ecran/PresentationProfil.java:80` | `"Tournoi"` |
| `PresentationProfil.TITRE_APPEL` (String) | `ui/ecran/PresentationProfil.java:81` | `"Appel micro : prononciation phonétique"` |
| `PresentationProfil.TITRE_CLUB` (String) | `ui/ecran/PresentationProfil.java:82` | `"Club"` |
| `PresentationProfil.INTRO_CLUB` (String) | `ui/ecran/PresentationProfil.java:83` | `"Cette partie ne concerne que les personnes qui gèrent les " + "tournois d'un club. Pour vous inscrire à un…` |
| `PresentationProfil.TITRE_AVERTISSEMENT` (String) | `ui/ecran/PresentationProfil.java:86` | `"Avant de relier votre compte à un club"` |
| `PresentationProfil.MOTIF_SANS_NUMERO` (String) | `ui/ecran/PresentationProfil.java:97` | `"Saisissez un numéro de club."` |
| `PresentationPrononciation.TITRE_JOUR_J` (String) | `ui/ecran/PresentationPrononciation.java:27` | `"Prononciation des noms"` |
| `PresentationPrononciation.AIDE_SAISIE` (String) | `ui/ecran/PresentationPrononciation.java:28` | `"Écrivez le nom tel qu'il se prononce, en alphabet phonétique international " + "(API) : cliquez sur un sym…` |
| `PresentationPrononciation.AIDE_PROFIL_SANS_LICENCE` (String) | `ui/ecran/PresentationPrononciation.java:31` | `"Reliez votre licence FFTT (à droite) pour indiquer comment " + "prononcer votre nom lors des appels au mic…` |
| `PresentationPrononciation.AIDE_JOUR_J` (String) | `ui/ecran/PresentationPrononciation.java:33` | `"Pour un nom mal prononcé par la voix : choisissez le joueur, écrivez sa " + "prononciation, écoutez, enreg…` |
| `PresentationPrononciation.CLUB_SANS_NUMERO` (String) | `ui/ecran/PresentationPrononciation.java:35` | `"Numéro du club inconnu (licence absente de la base) : la prononciation " + "du club ne peut pas être enreg…` |
| `PresentationPrononciation.SYMBOLES` (List<Symbole>) | `ui/ecran/PresentationPrononciation.java:51` | `List.of( new Symbole("ɑ̃", "enfant"), new Symbole("ɛ̃", "vin"), new Symbole("ɔ̃", "bon"), new Symbole("œ̃",…` |
| `PresentationPrononciation.EXEMPLES` (List<Exemple>) | `ui/ecran/PresentationPrononciation.java:73` | `List.of( new Exemple("Dupont", "dy.pɔ̃"), new Exemple("Jean", "ʒɑ̃"), new Exemple("Lefebvre", "lə.fɛvʁ"), n…` |
| `PresentationPrononciation.VOIX_INDISPONIBLE` (String) | `ui/ecran/PresentationPrononciation.java:107` | `"Écoute impossible : la voix n'est disponible que sous Windows. " + "La prononciation peut quand même être …` |
| `PresentationRemplissageFinances.TITRE` (String) | `ui/ecran/PresentationRemplissageFinances.java:19` | `"Remplissage et finances"` |
| `PresentationSaisieSms.TITRE` (String) | `ui/ecran/PresentationSaisieSms.java:26` | `"Convocations par SMS (facultatif)"` |
| `PresentationSaisieSms.INVITE_NUMERO` (String) | `ui/ecran/PresentationSaisieSms.java:28` | `"06 12 34 56 78"` |
| `PresentationSaisieSms.AIDE_JOUEUR` (String) | `ui/ecran/PresentationSaisieSms.java:30` | `"Laissez votre portable pour recevoir la table et l'adversaire de chaque " + "match. Messages écrits seulem…` |
| `PresentationSaisieSms.CONSENTEMENT_JOUEUR` (String) | `ui/ecran/PresentationSaisieSms.java:33` | `"J'accepte de recevoir mes convocations par SMS pour ce tournoi. " + "Mon numéro sert uniquement à cela et …` |
| `PresentationSaisieSms.CONSENTEMENT_CLUB` (String) | `ui/ecran/PresentationSaisieSms.java:36` | `"La personne accepte de recevoir ses convocations par SMS pour ce " + "tournoi. Son numéro sert uniquement …` |
| `PresentationSaisieSms.DATE` (DateTimeFormatter) | `ui/ecran/PresentationSaisieSms.java:39` | `DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRANCE)` |
| `PresentationSalle.DUREE_LONGUE` (Duration) | `ui/ecran/PresentationSalle.java:36` | `Duration.ofMinutes(40)` |
| `PresentationSalle.PREFIXE_GLISSER` (String) | `ui/ecran/PresentationSalle.java:39` | `"pingmanager-match:"` |
| `PresentationSuiviTournoi.INTERVALLE_ACTUALISATION_SECONDES` (int) | `ui/ecran/PresentationSuiviTournoi.java:36` | `30` |
| `PresentationTournoiImporte.NOMBRE_ETAPES` (int) | `ui/ecran/PresentationTournoiImporte.java:20` | `3` |
| `PropositionsTournoi.MASCULINS` (Set<String>) | `ui/ecran/PropositionsTournoi.java:14` | `Set.of( "club", "cercle", "stade", "racing", "sporting", "foyer", "ping", "comite", "patronage")` |
| `PropositionsTournoi.ELIDES_ARTICLE` (Set<String>) | `ui/ecran/PropositionsTournoi.java:18` | `Set.of( "entente", "union", "association", "amicale", "avenir", "elan", "etoile", "olympique", "esperance",…` |
| `Saisie.JJ_MM_AAAA` (DateTimeFormatter) | `ui/ecran/Saisie.java:22` | `DateTimeFormatter.ofPattern("dd/MM/yyyy")` |
| `Saisie.JOUR_LONG` (DateTimeFormatter) | `ui/ecran/Saisie.java:23` | `DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH)` |
| `Saisie.JOUR_COURT` (DateTimeFormatter) | `ui/ecran/Saisie.java:24` | `DateTimeFormatter.ofPattern("EEE d MMM", Locale.FRENCH)` |
| `Saisie.HEURE` (Pattern) | `ui/ecran/Saisie.java:26` | `Pattern.compile("^(\\d{1,2})\\s*(?:[:hH.]\\s*(\\d{2})?)?$")` |
| `Theme.CLAIR` (String) | `ui/ecran/Theme.java:22` | `"theme-clair"` |
| `Theme.SOMBRE` (String) | `ui/ecran/Theme.java:23` | `"theme-sombre"` |
| `Theme.FEUILLE` (String) | `ui/ecran/Theme.java:25` | `"/com/pingmanager/ui/theme.css"` |
| `Theme.PILE_POLICES` (List<String>) | `ui/ecran/Theme.java:26` | `List.of("Segoe UI", "Inter")` |
| `Theme.POLICES_AFFICHAGE` (List<String>) | `ui/ecran/Theme.java:47` | `List.of( "/com/pingmanager/polices/BricolageGrotesque-Bold.ttf", "/com/pingmanager/polices/BricolageGrotesq…` |
| `Ui.ETAT_OK` (String) | `ui/ecran/Ui.java:47` | `"etat-ok"` |
| `Ui.ETAT_ATTENTION` (String) | `ui/ecran/Ui.java:48` | `"etat-attention"` |
| `Ui.ETAT_RECOMMANDE` (String) | `ui/ecran/Ui.java:49` | `"etat-recommande"` |
| `Ui.ETAT_OPTIONNEL` (String) | `ui/ecran/Ui.java:50` | `"etat-optionnel"` |
| `Ui.EN_ERREUR` (String) | `ui/ecran/Ui.java:51` | `"en-erreur"` |
| `Ui.MOIS_COURT` (DateTimeFormatter) | `ui/ecran/Ui.java:311` | `DateTimeFormatter.ofPattern("MMM", Locale.FRENCH)` |
| `Ui.DUREE_APPARITION` (Duration) | `ui/ecran/Ui.java:378` | `Duration.millis(180)` |
| `Ui.CADRE_OK` (String) | `ui/ecran/Ui.java:539` | `"cadre-ok"` |
| `Ui.CADRE_MANQUANT` (String) | `ui/ecran/Ui.java:540` | `"cadre-manquant"` |
| `Ui.CADRE_FACULTATIF` (String) | `ui/ecran/Ui.java:541` | `"cadre-facultatif"` |
| `Ui.CADRES` (List<String>) | `ui/ecran/Ui.java:542` | `List.of(CADRE_OK, CADRE_MANQUANT, CADRE_FACULTATIF)` |
| `Ui.LIBELLES` (Map<String, String>) | `ui/ecran/Ui.java:825` | `Map.ofEntries( Map.entry("NATIONAL_A", "National A"), Map.entry("NATIONAL_B", "National B"), Map.entry("DEP…` |
| `VuePhaseFinale.HAUTEUR_ARBRE_MAX` (double) | `ui/ecran/VuePhaseFinale.java:66` | `640` |
| `VueReglementFige.REGLEMENTS` (ServiceReglementTournoi) | `ui/ecran/VueReglementFige.java:31` | `new ServiceReglementTournoi()` |
| `DonneesDemo.CLUB_BRUNOY` (String) | `ui/infra/DonneesDemo.java:26` | `"08911132"` |
| `DonneesDemo.CLUB_PARIS_15` (String) | `ui/infra/DonneesDemo.java:27` | `"07500123"` |
| `DonneesDemo.EMAIL_QUENTIN` (String) | `ui/infra/DonneesDemo.java:30` | `"soumet.quentin@gmail.com"` |
| `DonneesDemo.LICENCE_QUENTIN` (String) | `ui/infra/DonneesDemo.java:31` | `"9141442"` |
| `DonneesDemo.EMAIL_ULTRA_ADMIN` (String) | `ui/infra/DonneesDemo.java:35` | `"admin@pingmanager.fr"` |
| `DonneesDemo.MDP_ULTRA_ADMIN` (String) | `ui/infra/DonneesDemo.java:36` | `"PingAdmin123/"` |
| `DonneesDemo.ULTRA_ADMINS` (java.util.Set<String>) | `ui/infra/DonneesDemo.java:37` | `java.util.Set.of(EMAIL_ULTRA_ADMIN)` |
| `DonneesDemo.EMAIL_REFERENT_BRUNOY` (String) | `ui/infra/DonneesDemo.java:40` | `"sboulier@gmail.com"` |
| `DonneesDemo.REFERENT_BRUNOY_NOM` (String) | `ui/infra/DonneesDemo.java:41` | `"BOULIER"` |
| `DonneesDemo.REFERENT_BRUNOY_PRENOM` (String) | `ui/infra/DonneesDemo.java:42` | `"Serge"` |
| `DonneesDemo.JOUEURS_FICTIFS` (java.util.List<FicheLicence>) | `ui/infra/DonneesDemo.java:87` | `JoueursFictifs.generer()` |
| `EnvoiSmsSimule.CAPACITE` (int) | `ui/infra/EnvoiSmsSimule.java:22` | `200` |
| `GeocodeurGeoplateforme.INSTANCE` (GeocodeurGeoplateforme) | `ui/infra/GeocodeurGeoplateforme.java:28` | `new GeocodeurGeoplateforme()` |
| `GeocodeurGeoplateforme.URL` (String) | `ui/infra/GeocodeurGeoplateforme.java:30` | `"https://data.geopf.fr/geocodage/search?index=address&limit=1&q="` |
| `GeocodeurGeoplateforme.SCORE_MIN` (double) | `ui/infra/GeocodeurGeoplateforme.java:32` | `0.4` |
| `HachageMotDePassePBKDF2.ALGO` (String) | `ui/infra/HachageMotDePassePBKDF2.java:24` | `"PBKDF2WithHmacSHA256"` |
| `HachageMotDePassePBKDF2.ITERATIONS` (int) | `ui/infra/HachageMotDePassePBKDF2.java:25` | `120_000` |
| `HachageMotDePassePBKDF2.TAILLE_SEL` (int) | `ui/infra/HachageMotDePassePBKDF2.java:26` | `16;   // octets private static final int TAILLE_CLE = 256;  // bits private static final String PREFIXE = "…` |
| `HachageMotDePassePBKDF2.TAILLE_CLE` (int) | `ui/infra/HachageMotDePassePBKDF2.java:27` | `256;  // bits private static final String PREFIXE = "pbkdf2"` |
| `HachageMotDePassePBKDF2.PREFIXE` (String) | `ui/infra/HachageMotDePassePBKDF2.java:28` | `"pbkdf2"` |
| `ImpressionFeuilles.HORODATAGE` (DateTimeFormatter) | `ui/infra/ImpressionFeuilles.java:28` | `DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")` |
| `JoueursFictifs.CLUBS` (String[][]) | `ui/infra/JoueursFictifs.java:24` | `{ {"08910101", "Ping Club de Montgeron", "91", "Montgeron", "91230"}, {"08910102", "Yerres Tennis de Table"…` |
| `JoueursFictifs.POIDS` (int[]) | `ui/infra/JoueursFictifs.java:46` | `{9, 8, 8, 6, 6, 7, 5, 5, 6, 4, 4, 4, 3, 3, 3, 3, 3, 3}` |
| `JoueursFictifs.NOMS` (String[]) | `ui/infra/JoueursFictifs.java:48` | `{ "MARCHAND", "LEBRUN", "FONTAINE", "CHEVALIER", "ROBIN", "MASSON", "SANCHEZ", "GERARD", "NGUYEN", "BOYER",…` |
| `JoueursFictifs.PRENOMS_H` (String[]) | `ui/infra/JoueursFictifs.java:58` | `{ "Lucas", "Hugo", "Louis", "Nathan", "Gabriel", "Arthur", "Jules", "Adam", "Raphaël", "Léo", "Tom", "Théo"…` |
| `JoueursFictifs.PRENOMS_F` (String[]) | `ui/infra/JoueursFictifs.java:65` | `{ "Emma", "Jade", "Louise", "Alice", "Chloé", "Lina", "Léa", "Manon", "Inès", "Camille", "Sarah", "Zoé", "C…` |
| `JoueursFictifs.TRANCHES` (int[][]) | `ui/infra/JoueursFictifs.java:72` | `{ {500, 600, 95}, {601, 800, 70}, {801, 1000, 65}, {1001, 1300, 55}, {1301, 1600, 40}, {1601, 2000, 30}, {2…` |
| `RenduPdfFeuilles.POLICE_NORMALE` (String) | `ui/infra/RenduPdfFeuilles.java:41` | `"/com/pingmanager/polices/Inter-Regular.ttf"` |
| `RenduPdfFeuilles.POLICE_GRASSE` (String) | `ui/infra/RenduPdfFeuilles.java:42` | `"/com/pingmanager/polices/Inter-SemiBold.ttf"` |
| `RenduPdfFeuilles.TEXTE` (float[]) | `ui/infra/RenduPdfFeuilles.java:44` | `rgb(0x2F4558)` |
| `RenduPdfFeuilles.DOUX` (float[]) | `ui/infra/RenduPdfFeuilles.java:45` | `rgb(0x5C6E7D)` |
| `RenduPdfFeuilles.PRIMAIRE` (float[]) | `ui/infra/RenduPdfFeuilles.java:46` | `rgb(0x335F8A)` |
| `RenduPdfFeuilles.FILET` (float[]) | `ui/infra/RenduPdfFeuilles.java:47` | `rgb(0x9FB2C3)` |
| `RenduPdfFeuilles.FOND_ENTETE` (float[]) | `ui/infra/RenduPdfFeuilles.java:48` | `rgb(0xE6EEF6)` |
| `RenduPdfFeuilles.FOND_BARRE` (float[]) | `ui/infra/RenduPdfFeuilles.java:49` | `rgb(0xD4DEE7)` |
| `RenduPdfFeuilles.FOND_VAINQUEUR` (float[]) | `ui/infra/RenduPdfFeuilles.java:50` | `rgb(0xEAF4EC)` |
| `RenduPdfFeuilles.A4_L` (float) | `ui/infra/RenduPdfFeuilles.java:52` | `PDRectangle.A4.getWidth()` |
| `RenduPdfFeuilles.A4_H` (float) | `ui/infra/RenduPdfFeuilles.java:53` | `PDRectangle.A4.getHeight()` |
| `RenduPdfFeuilles.MARGE` (float) | `ui/infra/RenduPdfFeuilles.java:54` | `36` |
| `SyntheseVocaleWindows.VAR_SSML` (String) | `ui/infra/SyntheseVocaleWindows.java:37` | `"PINGMANAGER_SSML"` |
| `SyntheseVocaleWindows.VAR_VOIX` (String) | `ui/infra/SyntheseVocaleWindows.java:38` | `"PINGMANAGER_VOIX"` |
| `SyntheseVocaleWindows.VAR_DEBIT` (String) | `ui/infra/SyntheseVocaleWindows.java:39` | `"PINGMANAGER_DEBIT"` |
| `SyntheseVocaleWindows.LIMITE_ANNONCE` (Duration) | `ui/infra/SyntheseVocaleWindows.java:42` | `Duration.ofSeconds(60)` |
| `SyntheseVocaleWindows.LIMITE_VOIX` (Duration) | `ui/infra/SyntheseVocaleWindows.java:44` | `Duration.ofSeconds(15)` |
| `SyntheseVocaleWindows.SCRIPT_DIRE` (String) | `ui/infra/SyntheseVocaleWindows.java:47` | `String.join("\n", "$ErrorActionPreference = 'Stop'", "Add-Type -AssemblyName System.Speech", "$s = New-Obje…` |
| `SyntheseVocaleWindows.SCRIPT_VOIX` (String) | `ui/infra/SyntheseVocaleWindows.java:62` | `String.join("\n", "[Console]::OutputEncoding = [Text.Encoding]::UTF8", "Add-Type -AssemblyName System.Speec…` |
| `TournoiDemoBrunoy.NOM` (String) | `ui/infra/TournoiDemoBrunoy.java:59` | `"Tournoi National B de Brunoy 2027"` |
| `TournoiDemoBrunoy.DAMES` (String) | `ui/infra/TournoiDemoBrunoy.java:61` | `"G – Toutes séries dames"` |
| `TournoiDemoBrunoy.NON_NUMEROTES` (String) | `ui/infra/TournoiDemoBrunoy.java:63` | `"M – Non numérotés"` |
| `TournoiDemoBrunoy.SAMEDI` (LocalDate) | `ui/infra/TournoiDemoBrunoy.java:65` | `LocalDate.of(2027, 4, 10)` |
| `TournoiDemoBrunoy.DIMANCHE` (LocalDate) | `ui/infra/TournoiDemoBrunoy.java:66` | `LocalDate.of(2027, 4, 11)` |
| `TournoiDemoBrunoy.DIX_EUROS` (TarifInscription) | `ui/infra/TournoiDemoBrunoy.java:180` | `new TarifInscription(10, 10)` |
| `TournoiDemoBrunoyJourJ.NOM` (String) | `ui/infra/TournoiDemoBrunoyJourJ.java:56` | `"Tournoi National B de Brunoy – jour J (copie de démo)"` |
| `TournoiDemoBrunoyJourJ.GRAINE_POULES` (long) | `ui/infra/TournoiDemoBrunoyJourJ.java:59` | `101027L` |
| `TournoiDemoBrunoyJourJ.GRAINE_TABLEAU_FINAL` (long) | `ui/infra/TournoiDemoBrunoyJourJ.java:60` | `101028L` |
| `TournoiDemoBrunoyJourJ.TABLE_TABLEAU_FINAL` (int) | `ui/infra/TournoiDemoBrunoyJourJ.java:63` | `1` |
| `TournoiDemoBrunoyJourJ.TABLES_POULES` (List<Integer>) | `ui/infra/TournoiDemoBrunoyJourJ.java:66` | `List.of(4, 7, 9)` |
| `TournoiDemoBrunoyJourJ.JA_FICTIF` (AffectationJugeArbitre) | `ui/infra/TournoiDemoBrunoyJourJ.java:120` | `new AffectationJugeArbitre("ARBITRE", "Jean", null, "Licence fictive (JA démo)", GradeJugeArbitre.JA3, null…` |
| `TournoiDemoBrunoyJourJ.ADJOINT_FICTIF` (AffectationJugeArbitre) | `ui/infra/TournoiDemoBrunoyJourJ.java:123` | `new AffectationJugeArbitre("ADJOINT", "Julie", null, "Licence fictive (adjoint démo)", GradeJugeArbitre.JA2…` |
| `TournoisDemo.HOMOLOGATION_BRUNOY_DEMO` (String) | `ui/infra/TournoisDemo.java:29` | `"DÉMO-NB-2027-091 (fictif)"` |

### A.3 Clés JSON lues ou écrites par la persistance

| Fichier | Clés |
|---|---|
| `importation/LecteurTableur.java` | `Id`, `Joueurs`, `Tableaux`, `Target`, `content.xml`, `joueurs`, `name`, `r`, `t`, `tableaux` |
| `persistence/AnnoncesVocalesMapper.java` | `annonceAutomatique`, `annoncesVocales`, `avecTableauEtTour`, `debit`, `diffusions`, `rappelMinutes`, `voix` |
| `persistence/ApplicationMapper.java` | `adresse1`, `adresse2`, `cleTournoi`, `codePostal`, `date`, `dateFin`, `departement`, `email`, `emailOfficiel`, `emailVerifie`, `empreinteMotDePasse`, `evenements`, `heure`, `id`, `licenceLiee`, `lieu`, `niveau`, `nom`, `note`, `numero`, `numeroClub`, `origine`, `prenom`, `responsableNom`, `responsablePrenom`, `titre`, `ville` |
| `persistence/ConfigMapper.java` | `aireDeJeu`, `ballesRefusees`, `categoriesAutorisees`, `clotureInscriptions`, `cloturePointage`, `codePostal`, `couleur`, `couleurBalles`, `debutMatchs`, `debutPointage`, `eclairageLux`, `email`, `emplacement`, `enLigne`, `enLigneProposee`, `exclusionRangNational`, `finPointage`, `finPrevue`, `finTheorique`, `formatId`, `formatPhaseFinale`, `grade`, `horairePrevuFinales`, `largeur`, `latitude`, `libelle`, `licence`, `lieuDit`, `longitude`, `longueur`, `marque`, `marqueBalles`, `modeLimite`, `montant`, `nom`, `nombre`, `nombreDeTables`, `nombreManchesGagnantes`, `ouvertureInscriptions`, `petiteFinale`, `placeDebut`, `placeFin`, `placesMax`, `pointsMax`, `pointsMin`, `politiqueBalles`, `pourcentage`, `prenom`, `qualifiesParPoule`, `recompense`, `recompenses`, `residenceBatiment`, `responsableEmail`, `responsableNom`, `responsableTelephone`, `role`, `salleAdresse`, `salleCodePostal`, `salleNom`, `salleVille`, `sexeRequis`, `surPlace`, `surPlaceProposee`, `tailleMaxListeAttente`, `taillePouleSouhaitee`, `tarif`, `telephone`, `tiragePublicDateHeure`, `tiragePublicLieu`, `type`, `ville`, `voie` |
| `persistence/ConversionsCommunes.java` | `categorie`, `club`, `licence`, `nom`, `points`, `prenom`, `rangNational`, `sexe` |
| `persistence/ConvocationsSmsMapper.java` | `consentementLe`, `contacts`, `convocationsSms`, `destinataire`, `detail`, `envoiAutomatique`, `expediteur`, `instant`, `journal`, `licence`, `limiteEnvois`, `numero`, `numerosEffacesLe`, `statut`, `texte`, `type` |
| `persistence/DepotApplication.java` | `acces`, `agendas`, `clubs`, `comptes`, `comptesUtilisateurs`, `derniereOuverture`, `fichier`, `ipa`, `joueurs`, `kind`, `licence`, `nom`, `numero`, `prenom`, `prononciations`, `recents`, `version` |
| `persistence/DerouleMapper.java` | `a`, `b`, `cadre`, `id`, `instantFin`, `instantLancement`, `issue`, `joueurA`, `joueurB`, `joueurFautif`, `joueurs`, `manches`, `matchs`, `nom`, `nombreJoueurs`, `nombreManchesGagnantes`, `numero`, `numeroTable`, `penaliteContreA`, `penaliteContreB`, `sourceA`, `sourceB`, `statut`, `tours` |
| `persistence/DocumentReglementMapper.java` | `aCompleter`, `articles`, `blocs`, `elements`, `entetes`, `lignes`, `note`, `numero`, `sousTitre`, `texte`, `titre`, `type` |
| `persistence/PlanSalleMapper.java` | `colonne`, `colonnes`, `elements`, `etage`, `genre`, `hauteur`, `largeur`, `libelle`, `ligne`, `lignes`, `nom`, `numero`, `orientation`, `salles`, `tableauReserve`, `tableauxPreferes`, `tableauxReserves`, `type` |
| `persistence/ServiceFichierTournoi.java` | `XDG_DATA_HOME`, `user.home` |
| `persistence/SessionMapper.java` | `alpha`, `apprentissage`, `duree`, `finalesEnFinDeJournee`, `identifiantTableau`, `matchId`, `matchs`, `minObservations`, `minutes`, `nomTableau`, `nombreDeTables`, `observations`, `phase`, `poids`, `poidsAttente`, `poidsDeblocage`, `poidsProfondeur`, `poidsTableauCommence`, `poidsVague`, `pointsParMinuteAttente`, `pretDepuis`, `tourParTour`, `trancheNiveau`, `valeurParDefautGlobale`, `valeursDepart` |
| `persistence/SessionMapperComplet.java` | `discipline`, `estimateur`, `joueurs`, `kind`, `ordonnanceur`, `tournoi`, `version` |
| `persistence/TournoiMapper.java` | `accepteJoueursEtrangers`, `accepteLicencePromotionnelle`, `adresse`, `arbitres`, `articles`, `cartons`, `categoriesExclues`, `clubOrganisateur`, `conditionsMaintien`, `configuration`, `correspondantAutresInfos`, `correspondantEmail`, `correspondantNom`, `correspondantPrenom`, `correspondantTelephone`, `couleur`, `date`, `dateDebut`, `dateDecision`, `dateFin`, `discipline`, `dotationGlobale`, `droitImage`, `echelonHomologation`, `exceptionFeminine`, `feuilleALEnvoi`, `feuilleEngagement`, `feuilleEngagementFigee`, `fileAttente`, `fraisEnLigneDeduits`, `grainePoules`, `graineTableauFinal`, `horaires`, `horsMatch`, `imagePromotionnelle`, `infosReglement`, `inscrits`, `instant`, `joueur`, `joueurs`, `joueuses`, `jours`, `jugesArbitres`, `justificatif`, `lien`, `lotsTables`, `max`, `minimumInscrits`, `mode`, `modeAnnonce`, `modePreparation`, `motif`, `niveau`, `nom`, `nombreDeTables`, `numeroAffiliationClub`, `numeroHomologation`, `optionsReglement`, `paiement`, `parJour`, `parametres`, `payes`, `phaseFinale`, `phasePoints`, `pointageCloture`, `pointes`, `pointsPenalite`, `politiqueForfaits`, `poules`, `pourcentage`, `quotasTableaux`, `reduction`, `reductionPct`, `refusHomologation`, `regleTableauxParJour`, `reglementDocument`, `reglementFige`, `regles`, `remboursement`, `rembourses`, `seuilRemplissagePct`, `siteWeb`, `statut`, `strategieOrdonnancement`, `tableauBonusJoueuses`, `tableauBonusObligatoire`, `tableauSpecialNom`, `tableaux`, `tableauxApresElimination`, `tableauxConsecutifsInterdits`, `tablesEchauffement`, `tournoi`, `tousLesJours`, `trancheAbsents`, `type`, `typeTournoi`, `version` |
| `ui/diffusion/PagesSuivi.java` | `id`, `n`, `q` |
| `ui/diffusion/ServeurSuiviPublic.java` | `Allow`, `Cache-Control`, `Content-Security-Policy`, `Content-Type`, `Referrer-Policy`, `X-Content-Type-Options`, `X-Frame-Options` |

### A.4 Valeurs initiales des champs modifiables (builders, réglages)

| Champ | Fichier | Valeur initiale |
|---|---|---|
| `ScenarioSimulation.nombreDeTables` (int) | `core/ScenarioSimulation.java:189` | `16` |
| `ScenarioSimulation.graine` (long) | `core/ScenarioSimulation.java:190` | `1L` |
| `ScenarioSimulation.variabilite` (double) | `core/ScenarioSimulation.java:192` | `0.2` |
| `ScenarioSimulation.heureLimite` (LocalTime) | `core/ScenarioSimulation.java:195` | `HEURE_LIMITE_PAR_DEFAUT` |
| `ServiceAnnoncesVocales.resolveur` (volatile ResolveurPrononciation) | `core/ServiceAnnoncesVocales.java:52` | `ResolveurPrononciation.TEXTE_NORMAL` |
| `SimulateurTournoi.attenteMax` (long) | `core/SimulateurTournoi.java:246` | `-1` |
| `AccesClub.niveau` (NiveauAcces) | `model/AccesClub.java:32` | `NiveauAcces.ADMIN_NIVEAU_1` |
| `ConfigurationTableau.format` (com.pingmanager.model.format.FormatTournoi) | `model/ConfigurationTableau.java:285` | `null` |
| `ConfigurationTableau.taillePouleSouhaitee` (int) | `model/ConfigurationTableau.java:286` | `4` |
| `ConfigurationTableau.qualifiesParPoule` (int) | `model/ConfigurationTableau.java:287` | `2` |
| `ConfigurationTableau.formatPhaseFinale` (FormatPhaseFinale) | `model/ConfigurationTableau.java:288` | `FormatPhaseFinale.ELIMINATION_DIRECTE` |
| `ConfigurationTableau.nombreManchesGagnantes` (int) | `model/ConfigurationTableau.java:289` | `3` |
| `ConfigurationTableau.petiteFinale` (boolean) | `model/ConfigurationTableau.java:290` | `false` |
| `ConfigurationTableau.placesMax` (int) | `model/ConfigurationTableau.java:291` | `0` |
| `ConfigurationTableau.tailleMaxListeAttente` (int) | `model/ConfigurationTableau.java:292` | `4` |
| `ConfigurationTableau.tarif` (TarifInscription) | `model/ConfigurationTableau.java:294` | `TarifInscription.gratuit()` |
| `ConfigurationTableau.recompenses` (GrilleRecompenses) | `model/ConfigurationTableau.java:295` | `GrilleRecompenses.vide()` |
| `ConfigurationTableau.modeLimite` (ModeLimiteClassement) | `model/ConfigurationTableau.java:297` | `ModeLimiteClassement.TOUTES_SERIES` |
| `ConfigurationTableau.pointsMin` (int) | `model/ConfigurationTableau.java:298` | `0` |
| `ConfigurationTableau.pointsMax` (int) | `model/ConfigurationTableau.java:299` | `0` |
| `ConfigurationTableau.exclusionRangNational` (int) | `model/ConfigurationTableau.java:300` | `0` |
| `ConfigurationTableau.sexeRequis` (Sexe) | `model/ConfigurationTableau.java:302` | `null` |
| `ConvocationsSms.limiteEnvois` (int) | `model/ConvocationsSms.java:30` | `LIMITE_PAR_DEFAUT` |
| `EvenementAgenda.origine` (OrigineEvenement) | `model/EvenementAgenda.java:154` | `OrigineEvenement.PERSONNEL` |
| `Joueur.rangNational` (int) | `model/Joueur.java:33` | `RANG_NATIONAL_INCONNU` |
| `Match.nombreManchesGagnantes` (int) | `model/Match.java:35` | `3` |
| `Match.issue` (IssueMatch) | `model/Match.java:46` | `IssueMatch.NORMALE` |
| `Match.pointsPenaliteContreA` (int) | `model/Match.java:60` | `0` |
| `Match.pointsPenaliteContreB` (int) | `model/Match.java:61` | `0` |
| `ReglagesAnnoncesVocales.diffusions` (int) | `model/ReglagesAnnoncesVocales.java:28` | `DIFFUSIONS_PAR_DEFAUT` |
| `Tableau.phaseFinale` (TableauElimination) | `model/Tableau.java:32` | `null` |
| `Tableau.horaires` (HorairesTableau) | `model/Tableau.java:51` | `new HorairesTableau()` |
| `Tableau.statut` (StatutTableau) | `model/Tableau.java:62` | `StatutTableau.INSCRIPTION` |
| `Tableau.pointageCloture` (boolean) | `model/Tableau.java:70` | `false` |
| `Tableau.pointe` (boolean) | `model/Tableau.java:510` | `false` |
| `Tableau.paye` (boolean) | `model/Tableau.java:511` | `false` |
| `Tournoi.regleTableauxParJour` (RegleTableauxParJour) | `model/Tournoi.java:71` | `RegleTableauxParJour.illimitee()` |
| `Tournoi.statut` (StatutTournoi) | `model/Tournoi.java:86` | `StatutTournoi.BROUILLON` |
| `Tournoi.accepteLicencePromotionnelle` (boolean) | `model/Tournoi.java:137` | `false` |
| `Tournoi.accepteJoueursEtrangers` (boolean) | `model/Tournoi.java:138` | `false` |
| `Tournoi.optionsReglement` (OptionsReglement) | `model/Tournoi.java:149` | `OptionsReglement.parDefaut()` |
| `Tournoi.modePreparation` (ModePreparation) | `model/Tournoi.java:160` | `ModePreparation.COMPLETE` |
| `Tournoi.quotasTableaux` (QuotasTableaux) | `model/Tournoi.java:161` | `QuotasTableaux.aucun()` |
| `Tournoi.conditionsMaintien` (ConditionsMaintien) | `model/Tournoi.java:162` | `ConditionsMaintien.aucune()` |
| `Tournoi.politiqueForfaits` (PolitiqueForfaits) | `model/Tournoi.java:163` | `PolitiqueForfaits.nonDefinie()` |
| `Tournoi.paiement` (Paiement) | `model/Tournoi.java:164` | `Paiement.aucun()` |
| `FormatConfigurable.nom` (String) | `model/format/FormatConfigurable.java:72` | `"Format personnalisé"` |
| `FormatConfigurable.identifiant` (String) | `model/format/FormatConfigurable.java:73` | `"PERSO"` |
| `FormatConfigurable.taillePouleSouhaitee` (int) | `model/format/FormatConfigurable.java:74` | `3` |
| `FormatConfigurable.qualifiesParPoule` (int) | `model/format/FormatConfigurable.java:75` | `2` |
| `FormatConfigurable.sortNonQualifies` (SortNonQualifies) | `model/format/FormatConfigurable.java:76` | `SortNonQualifies.ELIMINE` |
| `FormatConfigurable.formatPhaseFinale` (FormatPhaseFinale) | `model/format/FormatConfigurable.java:77` | `FormatPhaseFinale.ELIMINATION_DIRECTE` |
| `FormatConfigurable.nombreManchesGagnantes` (int) | `model/format/FormatConfigurable.java:78` | `3` |
| `FormatConfigurable.nombreManchesGagnantesFinale` (int) | `model/format/FormatConfigurable.java:79` | `3` |
| `FormatConfigurable.avecPetiteFinale` (boolean) | `model/format/FormatConfigurable.java:80` | `false` |
| `FormatConfigurable.nombreManchesGagnantesPetiteFinale` (int) | `model/format/FormatConfigurable.java:81` | `3` |
| `Ordonnanceur.reservations` (ReservationsTables) | `ordonnancement/Ordonnanceur.java:52` | `ReservationsTables.AUCUNE` |
| `Ordonnanceur.tableauxAttendus` (Set<String>) | `ordonnancement/Ordonnanceur.java:239` | `Set.of()` |
| `PoidsOrdonnancement.poidsVague` (double) | `ordonnancement/PoidsOrdonnancement.java:17` | `1.0` |
| `PoidsOrdonnancement.poidsAttente` (double) | `ordonnancement/PoidsOrdonnancement.java:18` | `2.0` |
| `PoidsOrdonnancement.poidsDeblocage` (double) | `ordonnancement/PoidsOrdonnancement.java:19` | `3.0` |
| `PoidsOrdonnancement.pointsParMinuteAttente` (double) | `ordonnancement/PoidsOrdonnancement.java:22` | `1.0` |
| `PoidsOrdonnancement.poidsTableauCommence` (double) | `ordonnancement/PoidsOrdonnancement.java:29` | `0.0` |
| `PoidsOrdonnancement.tourParTour` (boolean) | `ordonnancement/PoidsOrdonnancement.java:37` | `false` |
| `PoidsOrdonnancement.poidsProfondeur` (double) | `ordonnancement/PoidsOrdonnancement.java:44` | `0.0` |
| `PoidsOrdonnancement.finalesEnFinDeJournee` (boolean) | `ordonnancement/PoidsOrdonnancement.java:54` | `false` |
| `TempsAnnexesMatch.appel` (int) | `ordonnancement/TempsAnnexesMatch.java:143` | `60` |
| `TempsAnnexesMatch.probabiliteRappel` (double) | `ordonnancement/TempsAnnexesMatch.java:144` | `0.15` |
| `TempsAnnexesMatch.rappel` (int) | `ordonnancement/TempsAnnexesMatch.java:145` | `180` |
| `TempsAnnexesMatch.echauffement` (int) | `ordonnancement/TempsAnnexesMatch.java:146` | `120` |
| `TempsAnnexesMatch.pauseEntreManches` (int) | `ordonnancement/TempsAnnexesMatch.java:147` | `30` |
| `TempsAnnexesMatch.probabiliteTempsMort` (double) | `ordonnancement/TempsAnnexesMatch.java:148` | `0.2` |
| `TempsAnnexesMatch.feuille` (int) | `ordonnancement/TempsAnnexesMatch.java:149` | `90` |
| `ContexteSerialisation.compteurMatch` (int) | `persistence/ContexteSerialisation.java:37` | `0` |
| `DiffusionSuivi.port` (int) | `ui/diffusion/DiffusionSuivi.java:48` | `PORT_PAR_DEFAUT` |
| `DiffuseurAnnonces.ecouteur` (volatile Consumer<ResultatAnnonceVocale>) | `ui/infra/DiffuseurAnnonces.java:25` | `r -> { }` |


## Annexe B — Catalogue de toutes les classes (rôle, taille, surface publique)

| Classe | Lignes | Méthodes publiques | Rôle (première phrase de la documentation) | Lignes couvertes | Classes de test qui la citent |
|---|---|---|---|---|---|
| `core/AnnonceRencontre` | 52 | 3 | Annonce d'une rencontre envoyée sur une table (cahier des charges v0, section 7) : | 100 % | 4 |
| `core/AnnonceVocale` | 66 | 5 | Annonce vocale d'une rencontre envoyée sur une table (B017), découpée en morceaux (SegmentAnnonce) pour la prononciation. | 83 % | 5 |
| `core/BilanRemplissageFinances` | 165 | 21 | Tableau de bord remplissage et finances d'un tournoi (B036), calculé par ServiceRemplissageFinances : | 100 % | 3 |
| `core/CalendrierTournoi` | 97 | 6 | Situation d'un tournoi dans le temps : | 86 % | 2 |
| `core/CatalogueTournois` | 234 | 13 | Catalogue des tournois visibles dans l'onglet Tournois (publiés, à venir ou en cours) et leurs filtres. | 97 % | 3 |
| `core/ClassementExporte` | 21 | 1 | Une ligne du classement final d'un tableau, dans un export de résultats. | 83 % | 2 |
| `core/ClassementPouleService` | 189 | 1 | Calcule le classement d'une poule selon les règles FFTT. | 96 % | 3 |
| `core/ComparaisonStrategies` | 39 | 3 | Comparaison de deux stratégies simulées sur le même scénario et la même graine (B025) : | 100 % | 3 |
| `core/ContexteConvocation` | 90 | 6 | Où se joue un match, pour le texte d'une convocation par SMS (B019) : | 88 % | 3 |
| `core/DemandeVerification` | 67 | 5 | État transitoire d'une vérification d'email en cours. | 85 % | 2 |
| `core/DiagnosticPhaseFinale` | 89 | 9 | Où en est la phase finale d'un tableau, et pourquoi on ne peut pas (encore) la construire. | 92 % | 5 |
| `core/EditionPlanSalles` | 312 | 29 | Brouillon de l'éditeur de plan (B024) : | 89 % | 1 |
| `core/EligibiliteTournoi` | 141 | 7 | Accès d'un licencié à un tournoi selon sa catégorie (règlements sportifs 2026-2027, IX.101 et IX.102). | 90 % | 6 |
| `core/EliminationTableau` | 99 | 2 | Situation d'un joueur dans un tableau le jour J : | 55 % | 2 |
| `core/EnvoiEmail` | 22 | 0 | Port : | – | 3 |
| `core/EnvoiSms` | 24 | 0 | Port : | – | 1 |
| `core/ExpediteurSms` | 45 | 2 | Nom d'expéditeur alphanumérique des SMS (B019, voir docs/RECHERCHE_SMS.md). | 100 % | 2 |
| `core/ExportIcs` | 158 | 5 | Export de l'agenda au format iCalendar (.ics, RFC 5545), écrit à la main sans dépendance : | 100 % | 2 |
| `core/ExportResultatsFederaux` | 28 | 0 | Port : | – | 0 |
| `core/FichierExporte` | 59 | 7 | Fichier produit en mémoire par un port d'export (ExportResultatsFederaux). | 67 % | 3 |
| `core/GenerateurCode` | 19 | 0 | Port : | – | 3 |
| `core/GenerateurMatchsPoule` | 276 | 2 | Génère les rencontres d'une poule en championnat toutes rondes : | 100 % | 2 |
| `core/GenerateurPoules` | 302 | 2 | Service métier chargé de répartir une liste de joueurs en poules équilibrées, selon les principes de la FFTT. | 99 % | 3 |
| `core/GenerateurSsml` | 134 | 3 | Document SSML d'une annonce vocale (B017), lu par la synthèse vocale du système. | 100 % | 6 |
| `core/GenerateurTableauElimination` | 236 | 2 | Construit un tableau à élimination directe à partir d'une liste de joueurs classés par ordre de force (le meilleur en premier), en appliquant les règles FFTT de placement | 93 % | 9 |
| `core/Geocodeur` | 51 | 0 | Port de géocodage : | 82 % | 3 |
| `core/GestionCartonsMatch` | 104 | 3 | Applique la progression officielle des sanctions au cours d'un match (règles 3.5.2.2 et 3.5.2.3) et alimente le RegistreDisciplinaire. | 100 % | 1 |
| `core/HachageMotDePasse` | 29 | 0 | Port : | – | 3 |
| `core/Horloge` | 23 | 0 | Port : | 100 % | 10 |
| `core/ImportJoueurs` | 694 | 20 | Import d'une liste de joueurs dans les tableaux d'un tournoi, pour un club qui a préparé son tournoi hors de PingManager (règlement, tableaux, inscriptions) et veut l'uti | 95 % | 3 |
| `core/ImportTableaux` | 241 | 3 | Lecture de la feuille « Tableaux » d'un classeur d'import : | 90 % | 2 |
| `core/JoueurDepuisLicence` | 57 | 1 | Construit le Joueur d'une inscription à partir de sa fiche de licence : | 47 % | 3 |
| `core/JoueurExporte` | 28 | 1 | Joueur tel qu'il figure dans un export de résultats : | 100 % | 0 |
| `core/MoteurTournoi` | 230 | 9 | Orchestre le déroulement d'une épreuve selon un FormatTournoi : | 99 % | 2 |
| `core/MotifRefusExport` | 22 | 1 | Raison pour laquelle aucun export de résultats ne peut être produit. | 88 % | 2 |
| `core/MotifsInscription` | 103 | 2 | Motif en clair d'un résultat d'inscription, pour l'écran (validation en direct) : | 70 % | 1 |
| `core/NiveauPriorite` | 30 | 1 | Niveau de priorité d'un match proposé dans la salle, affiché en couleur sur l'écran du jour J (cahier des charges v0, section 7) : | 100 % | 4 |
| `core/NotationManche` | 75 | 3 | Lecture et écriture du score d'une manche tel qu'on le tape en salle. | 100 % | 1 |
| `core/NumeroPortable` | 52 | 2 | Numéro de portable saisi pour les convocations par SMS (B019). | 100 % | 2 |
| `core/OrdonnancementTableaux` | 158 | 4 | Ordonnancement automatique des tableaux d'un tournoi. | 98 % | 1 |
| `core/PartieExportee` | 187 | 24 | Une partie jouée, telle qu'elle figure dans un export de résultats (B023). | 100 % | 2 |
| `core/PerformancesJoueur` | 121 | 6 | Performances d'un joueur dans un tournoi (« Mes tournois », tournois passés) : | 98 % | 2 |
| `core/PlacementQualifiesService` | 665 | 6 | Fait la liaison entre la phase de poules et le tableau à élimination directe, en plaçant les qualifiés selon les règles FFTT (article I.305). | 92 % | 4 |
| `core/PlansTypesSalle` | 104 | 5 | Plans types de salle (B024) : | 97 % | 3 |
| `core/PodiumPhaseFinale` | 41 | 2 | Podium d'un tableau à élimination directe, calculé par ServicePhaseFinale#podium. | 100 % | 6 |
| `core/PolitiqueOuverture` | 32 | 1 | Détermine, selon le StatutTournoi statut d'un tournoi, si les opérations de déroulé (inscription, pointage, lancement) sont autorisées. | 100 % | 2 |
| `core/ProgressionTableau` | 134 | 1 | Fait progresser un TableauElimination : | 96 % | 4 |
| `core/RegistreClubs` | 50 | 0 | Port : | 100 % | 3 |
| `core/RegistreClubsMemoire` | 50 | 3 | Implémentation en mémoire de RegistreClubs, indexée par numéro de club. | 100 % | 9 |
| `core/RegistreDisciplinaire` | 86 | 5 | Registre disciplinaire d'une compétition : | 100 % | 15 |
| `core/RegistreLicences` | 62 | 0 | Port : | 67 % | 3 |
| `core/RegistrePrononciations` | 96 | 7 | Registre des prononciations phonétiques (B018) : | 100 % | 3 |
| `core/ReglePrioriteSalle` | 88 | 3 | Règle de couleur des matchs proposés (cahier des charges v0, section 7) : | 100 % | 2 |
| `core/RemplissageTableaux` | 152 | 12 | Lecture du remplissage des tableaux d'un tournoi, pour la prévisualisation côté club et la liste des participants de la fiche : | 95 % | 7 |
| `core/ResolveurPrononciation` | 34 | 0 | Point d'accroche de la prononciation des noms dans les annonces vocales (B017, complété par B018) : | 100 % | 2 |
| `core/ResolveurPrononciationRegistre` | 58 | 3 | Prononciation des annonces vocales adossée au RegistrePrononciations (B018) : | 94 % | 2 |
| `core/ResultatAgenda` | 67 | 6 | Résultat détaillé d'une opération sur l'agenda du compte : | 84 % | 2 |
| `core/ResultatAnnonceVocale` | 24 | 2 | Issue de la diffusion d'une annonce (B017) : | 100 % | 2 |
| `core/ResultatConfirmation` | 65 | 7 | Résultat d'une tentative de confirmation du code de vérification, passée par le ServiceComptesOrganisateurs. | 93 % | 2 |
| `core/ResultatConstructionPhaseFinale` | 59 | 4 | Résultat de la construction du tableau final d'un tableau (ServicePhaseFinale#construire). | 100 % | 3 |
| `core/ResultatContactSms` | 49 | 2 | Issue de la saisie d'un numéro de portable pour les convocations par SMS (B019). | 95 % | 3 |
| `core/ResultatConvocationsSms` | 58 | 5 | Bilan d'une convocation par SMS (B019) : | 89 % | 2 |
| `core/ResultatDemandeAnnonce` | 27 | 1 | Réponse à une demande d'annonce vocale (B017) : | 100 % | 2 |
| `core/ResultatEditionSalles` | 62 | 6 | Résultat d'une opération sur la liste des salles d'un tournoi (B024) : | 95 % | 2 |
| `core/ResultatEnvoiSms` | 24 | 2 | Issue d'un envoi par le port EnvoiSms. | 100 % | 3 |
| `core/ResultatInscriptionOrganisateur` | 74 | 8 | Résultat d'une tentative d'inscription d'un organisateur passée par le ServiceComptesOrganisateurs. | 94 % | 2 |
| `core/ResultatInscriptionTournoi` | 129 | 16 | Résultat détaillé d'une tentative d'inscription passée par le ServiceInscription. | 97 % | 7 |
| `core/ResultatLancement` | 50 | 5 | Compte rendu du lancement d'un tableau, produit par ServiceLancementTableau#lancer. | 90 % | 2 |
| `core/ResultatLiaisonClub` | 62 | 7 | Compte rendu de la confirmation d'une liaison à un club (saisie du code reçu à l'adresse officielle du club). | 88 % | 1 |
| `core/ResultatModificationTournoi` | 66 | 5 | Réponse de ServiceVerrousHomologation à une demande de modification d'un champ du tournoi. | 94 % | 2 |
| `core/ResultatPointage` | 58 | 5 | Compte rendu de la clôture du pointage d'un tableau (mécanisme de présence, jour J), produit par ServicePointage#cloturerPointage. | 80 % | 3 |
| `core/ResultatPreparationExport` | 58 | 5 | Résultat de ServiceResultatsFederaux#construire : | 73 % | 2 |
| `core/ResultatPropositionSalle` | 69 | 4 | Résultat du bouton « Proposer » de la salle : | 91 % | 3 |
| `core/ResultatSaisieMatch` | 75 | 4 | Compte rendu d'une saisie de résultat de match par SaisieResultatMatch : | 100 % | 2 |
| `core/ResultatSaisiePrononciation` | 32 | 2 | Issue de la vérification ou de l'enregistrement d'une prononciation phonétique (B018). | 100 % | 2 |
| `core/ResultatSaisieScore` | 101 | 7 | Résultat de la saisie du résultat d'un match (score ou issue non normale), produit par ValidationScoreMatch et ServicePhaseFinale. | 88 % | 4 |
| `core/ResultatSimulation` | 114 | 7 | Résultat détaillé d'une simulation de tournoi (B025) pour une stratégie : | 100 % | 4 |
| `core/ResultatTransition` | 80 | 7 | Résultat d'une tentative de transition dans le cycle de vie d'un tournoi, passée par le ServiceCycleTournoi. | 80 % | 6 |
| `core/ResultatVerificationLancement` | 80 | 6 | Réponse de ServiceLancementTableau#verifierLancement : | 100 % | 2 |
| `core/ResultatsTournoi` | 36 | 0 | Résultats d'un tournoi prêts à exporter (objet neutre, sans format de fichier), produits par ServiceResultatsFederaux#construire. | 100 % | 4 |
| `core/SaisieResultatMatch` | 180 | 7 | Saisie des résultats des matchs de poule, le jour J : | 92 % | 6 |
| `core/ScenarioSimulation` | 249 | 23 | Données d'entrée d'une simulation de tournoi (B025) : | 99 % | 5 |
| `core/SegmentAnnonce` | 35 | 1 | Morceau d'une annonce vocale (B017) : | 100 % | 3 |
| `core/ServiceAgenda` | 252 | 9 | Règles de l'agenda personnel du compte (cahier des charges v0, sections 1 et 3) : | 97 % | 4 |
| `core/ServiceAnnoncesVocales` | 322 | 16 | Annonces vocales des rencontres dans la salle (B017) : | 92 % | 4 |
| `core/ServiceAuthentification` | 98 | 4 | Service d'authentification : | 81 % | 0 |
| `core/ServiceCompletudeTournoi` | 216 | 4 | Vue de complétude enrichie d'un tournoi, pour l'écran de gestion. | 94 % | 0 |
| `core/ServiceComptesOrganisateurs` | 200 | 5 | Service d'inscription d'un organisateur et de rattachement à son club. | 98 % | 2 |
| `core/ServiceConvocationsSms` | 263 | 14 | Convocations par SMS (B019) : | 99 % | 2 |
| `core/ServiceCycleTournoi` | 467 | 15 | Service pilotant le cycle de vie d'un Tournoi : | 97 % | 16 |
| `core/ServiceInscription` | 369 | 7 | Service d'inscription d'un joueur à un tableau, à l'échelle du tournoi. | 96 % | 10 |
| `core/ServiceInscriptionEnLigne` | 226 | 11 | Inscription en ligne d'un licencié par lui-même (onglet Tournois). | 94 % | 5 |
| `core/ServiceInscriptionSurPlace` | 130 | 6 | Inscription sur place, le jour J, à la table du juge-arbitre : | 98 % | 2 |
| `core/ServiceLancementTableau` | 222 | 8 | Service de lancement d'un tableau : | 92 % | 34 |
| `core/ServiceLiaisonClub` | 155 | 4 | Service de la démarche 2 : | 85 % | 1 |
| `core/ServiceLiaisonLicence` | 45 | 2 | Service de la démarche 1 : | 100 % | 1 |
| `core/ServicePhaseFinale` | 368 | 12 | Déroulé de la phase finale d'un tableau (tableau à élimination directe après les poules), pour l'écran du juge-arbitre : | 98 % | 25 |
| `core/ServicePlanSalle` | 172 | 6 | Salles et plans d'un tournoi (B024) : | 99 % | 4 |
| `core/ServicePointage` | 229 | 12 | Service gérant le pointage et le repêchage d'un tableau, le jour du tournoi (mécanisme de présence). | 90 % | 37 |
| `core/ServiceRemplissageFinances` | 53 | 1 | Calcule le tableau de bord remplissage et finances d'un tournoi (B036, cahier des charges v0, section 6) : | 100 % | 2 |
| `core/ServiceResultatsFederaux` | 201 | 5 | Service pur : | 96 % | 4 |
| `core/ServiceSalle` | 587 | 10 | Pilotage de la salle le jour J : | 97 % | 9 |
| `core/ServiceTournois` | 124 | 7 | Point d'entrée habilité pour créer et piloter les tournois d'un club. | 100 % | 1 |
| `core/ServiceVerrousHomologation` | 307 | 17 | Verrous du dossier d'homologation (cahier des charges v0, section 6) : | 97 % | 4 |
| `core/SimulateurTournoi` | 583 | 6 | Simulateur de tournoi accéléré (B025) : | 98 % | 4 |
| `core/StatistiquesSimulation` | 207 | 1 | Statistiques d'une stratégie sur de nombreux tirages du même tournoi (B025, méthode de Monte-Carlo) : | 98 % | 2 |
| `core/StatutCarteTournoi` | 66 | 3 | Statut d'un tournoi dans la carte des tournois (carte et agenda), pour le licencié connecté. | 100 % | 2 |
| `core/SuiviPoule` | 109 | 7 | Suivi en direct d'une poule pendant le jour J : | 100 % | 4 |
| `core/SuiviTournoi` | 222 | 13 | Suivi public d'un tournoi en cours, en lecture seule : | 99 % | 2 |
| `core/SyntheseVocale` | 69 | 5 | Port : | 92 % | 7 |
| `core/TableauDeBordSalle` | 238 | 21 | Photographie de la salle à un instant donné, calculée par ServiceSalle#tableauDeBord : | 98 % | 7 |
| `core/Territoires` | 103 | 4 | Référentiel des départements et de leur ligue régionale FFTT (une ligue par région administrative), lu depuis la ressource departements.csv du cœur. | 86 % | 1 |
| `core/TexteAnnonceVocale` | 99 | 2 | Texte d'une annonce vocale (B017) : | 100 % | 6 |
| `core/TexteConvocation` | 98 | 2 | Textes des convocations par SMS (B019), toujours ramenés à l'alphabet GSM 7 bits et à un seul SMS (160 caractères) : | 97 % | 2 |
| `core/TexteSms` | 129 | 4 | Longueur et alphabet d'un SMS (B019). | 100 % | 3 |
| `core/ValidationPrononciation` | 98 | 2 | Vérifie une prononciation saisie en alphabet phonétique international (API / IPA) avant de l'enregistrer (B018). | 100 % | 3 |
| `core/ValidationScoreMatch` | 94 | 2 | Vérifie qu'un score saisi est un score de tennis de table possible, avant de l'enregistrer sur un com.pingmanager.model.Match. | 97 % | 2 |
| `core/feuilles/ArbreImprimable` | 87 | 3 | Contenu du tableau final imprimable (arbre des rencontres) pour l'affichage en salle : | 96 % | 4 |
| `core/feuilles/EnTeteFeuille` | 30 | 1 | En-tête commun aux feuilles imprimées : | 100 % | 1 |
| `core/feuilles/FeuilleMatch` | 50 | 2 | Contenu d'une feuille de match (la « feuille de partie » des règlements pour une compétition individuelle), au format provisoire demandé par Quentin (cahier des charges v | 100 % | 4 |
| `core/feuilles/FeuillePoule` | 80 | 4 | Contenu de la feuille de poule : | 100 % | 4 |
| `core/feuilles/GenerateurFeuilles` | 384 | 5 | Prépare le contenu des feuilles imprimées du jour J, sans rien savoir du PDF (le rendu est fait hors du cœur, par ui.infra.RenduPdfFeuilles) : | 91 % | 5 |
| `core/feuilles/GrilleManches` | 72 | 2 | Grille des manches d'une feuille de match, au format provisoire de Quentin (cahier des charges v0, section 9) : | 92 % | 1 |
| `core/feuilles/JoueurFeuille` | 62 | 4 | Un joueur tel qu'il est imprimé sur une feuille : | 100 % | 2 |
| `core/feuilles/MotifRefusFeuille` | 30 | 1 | Pourquoi une feuille ne peut pas être préparée. | 100 % | 2 |
| `core/feuilles/ResultatFeuille` | 38 | 5 | Résultat de la préparation d'une feuille : | 88 % | 1 |
| `core/reglement/Formats` | 89 | 8 | Formatage à la française partagé par l'interface et le règlement généré. | 93 % | 2 |
| `core/reglement/GenerateurFeuilleEngagement` | 330 | 1 | Feuille d'engagement d'un tournoi : | 95 % | 2 |
| `core/reglement/GenerateurReglement` | 1119 | 3 | Construit le règlement du tournoi à partir des informations saisies dans les autres sections. | 94 % | 9 |
| `core/reglement/LibellesTableau` | 179 | 10 | Résumés lisibles d'un tableau, communs à la grille et au règlement. | 58 % | 3 |
| `core/reglement/RenduPdfReglement` | 503 | 4 | Rendu PDF (A4) du règlement, avec PDFBox et la police Inter intégrée (elle couvre « ≤ », « ½ », « € », les guillemets français…). | 97 % | 4 |
| `core/reglement/ResultatModificationReglement` | 57 | 4 | Réponse de ServiceReglementTournoi à une demande de modification du règlement (mode généré / déposé, articles ajoutés, fichier déposé). | 93 % | 2 |
| `core/reglement/ServiceReglementTournoi` | 123 | 6 | Règlement et feuille d'engagement d'un tournoi, avant et après publication. | 97 % | 3 |
| `importation/AnomalieImport` | 17 | 1 | Anomalie relevée sur une ligne d'un fichier d'échange (licenciés, clubs). | 100 % | 0 |
| `importation/CsvFederal` | 244 | 0 | Outils communs aux fichiers d'échange FFTT provisoires (B023, format documenté dans docs/FORMAT_ECHANGES_FFTT.md). | 95 % | 3 |
| `importation/ExportResultatsCsv` | 98 | 2 | Export des résultats en CSV provisoire (B023) : | 100 % | 2 |
| `importation/LecteurClubsCsv` | 119 | 2 | Lit clubs.csv, une liste de clubs fournie par l'organisateur (B023). | 94 % | 2 |
| `importation/LecteurLicencesCsv` | 194 | 2 | Lit licencies.csv, la liste des licenciés fournie par un club (B023). | 93 % | 2 |
| `importation/LecteurTableur` | 620 | 8 | Lit la première feuille d'un fichier de joueurs en cellules texte, sans bibliothèque externe (même choix que le JSON maison de la persistance) : | 83 % | 5 |
| `importation/ModeleImport` | 391 | 2 | Modèles d'import à remplir par le club. | 99 % | 1 |
| `importation/RegistreClubsFichier` | 69 | 5 | RegistreClubs alimenté par un fichier de clubs (LecteurClubsCsv, format provisoire B023). | 93 % | 1 |
| `importation/RegistreLicencesFichier` | 96 | 7 | RegistreLicences alimenté par un fichier de licenciés fourni par le club (LecteurLicencesCsv, format provisoire B023). | 93 % | 1 |
| `importation/ResultatImportClubs` | 24 | 1 | Résultat de LecteurClubsCsv#lire : | 100 % | 2 |
| `importation/ResultatImportLicences` | 31 | 1 | Résultat de LecteurLicencesCsv#lire : | 100 % | 2 |
| `importation/SituationCertificat` | 12 | 0 | Situation déclarée du licencié vis-à-vis du certificat médical, telle que fournie par le club dans licencies.csv (II.606.1). | 100 % | 1 |
| `model/AccesClub` | 125 | 14 | Lien d'autorisation entre une personne (identifiée par son email) et un club (identifié par son numéro FFTT). | 76 % | 6 |
| `model/AdresseTournoi` | 159 | 15 | Adresse structurée du lieu d'un tournoi, calquée sur le formulaire d'homologation SPID. | 78 % | 24 |
| `model/AffectationArbitre` | 73 | 8 | Arbitre officiel affecté au tournoi, repris de sa fiche de licence. | 68 % | 3 |
| `model/AffectationJugeArbitre` | 124 | 14 | Juge-arbitre affecté à un tournoi, en saisie libre. | 77 % | 19 |
| `model/AgendaCompte` | 85 | 9 | Agenda personnel d'un compte (identifié par son email) : | 93 % | 8 |
| `model/AireDeJeu` | 56 | 5 | Caractéristiques de l'aire de jeu d'un tournoi : | 94 % | 1 |
| `model/ArticleOptionnel` | 81 | 6 | Article facultatif ajouté au règlement généré, avec ses paramètres (horaires de la buvette, délai du tirage au sort…). | 93 % | 4 |
| `model/Carton` | 72 | 9 | Un carton reçu par un joueur, conservé dans l'historique disciplinaire. | 80 % | 2 |
| `model/CategorieAge` | 104 | 6 | Catégories d'âge FFTT (règlements généraux 2026-2027, articles II.302 pour les adultes et II.303 pour les jeunes). | 97 % | 6 |
| `model/ChampHomologation` | 39 | 1 | Champs du dossier d'homologation (grille 🔴 de docs/passation/REGLES_CREATION_TOURNOI.md) : | 100 % | 3 |
| `model/Club` | 148 | 20 | Représente un club de tennis de table affilié à la FFTT. | 86 % | 99 |
| `model/CompteOrganisateur` | 129 | 13 | Compte d'une personne administrant les tournois d'un club. | 81 % | 3 |
| `model/CompteUtilisateur` | 139 | 14 | Compte de base d'une personne sur l'application, commun à tous les usages : | 57 % | 1 |
| `model/ConditionsMaintien` | 78 | 8 | Maintien des tableaux et des dotations : | 88 % | 2 |
| `model/ConfigurationTableau` | 482 | 45 | Paramètres de configuration d'un Tableau. | 95 % | 80 |
| `model/ContactSms` | 28 | 1 | Numéro de portable d'un licencié pour les convocations par SMS (B019). | 100 % | 6 |
| `model/ConvocationsSms` | 128 | 17 | Réglages, numéros de portable consentis et journal des convocations par SMS d'un tournoi (B019). | 100 % | 4 |
| `model/Coordonnees` | 33 | 2 | Position géographique (WGS84, degrés décimaux) : | 92 % | 5 |
| `model/CouleurBalles` | 17 | 1 | Couleur des balles (règles du jeu 2.3.3 : | 100 % | 2 |
| `model/CouleurCarton` | 23 | 0 | Couleur d'un carton, au sens du règlement (section 3.5.2). | 100 % | 2 |
| `model/DocumentReference` | 60 | 6 | Référence vers un document joint au tournoi (règlement, feuille d'engagement), sans en porter le contenu binaire. | 43 % | 9 |
| `model/DocumentReglement` | 73 | 5 | Règlement généré, indépendant de tout affichage : | 100 % | 11 |
| `model/EchelonHomologation` | 54 | 2 | Échelon qui délivre l'homologation d'un tournoi. | 92 % | 1 |
| `model/ElementPlan` | 140 | 16 | Élément posé sur la grille d'un PlanSalle (B024) : | 86 % | 6 |
| `model/EvenementAgenda` | 209 | 26 | Une date de l'agenda du compte : | 92 % | 7 |
| `model/FicheLicence` | 283 | 39 | Fiche renvoyée par le référentiel FFTT lorsqu'on recherche une licence par son numéro. | 94 % | 17 |
| `model/FormatPhaseFinale` | 19 | 0 | Format de la phase qui suit les poules dans un tableau. | 100 % | 2 |
| `model/GradeJugeArbitre` | 87 | 4 | Grade d'un juge-arbitre (JA) FFTT, par ordre croissant de qualification : | 91 % | 19 |
| `model/GrilleRecompenses` | 179 | 16 | Grille des récompenses d'un Tableau : | 79 % | 7 |
| `model/HorairesTableau` | 65 | 9 | Calendrier d'un tableau : | 100 % | 29 |
| `model/InfosReglement` | 447 | 47 | Métadonnées du règlement d'un tournoi (article IX.111). | 99 % | 13 |
| `model/IssueMatch` | 37 | 0 | Manière dont un match se conclut, au sens du règlement FFTT (article I.303). | 100 % | 19 |
| `model/Joueur` | 159 | 19 | Représente un joueur licencié FFTT. | 89 % | 103 |
| `model/LigneClassement` | 163 | 23 | Ligne de classement d'un joueur au sein d'une poule. | 76 % | 3 |
| `model/LotTables` | 35 | 4 | Un lot de tables identiques : | 90 % | 3 |
| `model/Manche` | 31 | 3 | Score d'une manche (set) de tennis de table. | 83 % | 46 |
| `model/Match` | 322 | 33 | Une rencontre entre deux joueurs (joueur A vs joueur B). | 88 % | 75 |
| `model/MessageSms` | 32 | 1 | Ligne du journal des envois de SMS d'un tournoi (B019). | 100 % | 6 |
| `model/ModeAnnonce` | 51 | 3 | Façon d'annoncer une rencontre aux joueurs quand le juge-arbitre l'envoie sur une table (cahier des charges v0, section 7) : | 100 % | 8 |
| `model/ModeLimiteClassement` | 34 | 0 | Mode de restriction d'accès d'un Tableau selon le classement (points) des joueurs. | 100 % | 1 |
| `model/ModePreparation` | 30 | 1 | Comment le club prépare son tournoi dans PingManager, choisi juste après la création du brouillon (choix imposé) : | 88 % | 2 |
| `model/NiveauAcces` | 30 | 1 | Niveau d'habilitation d'une personne sur un club (porté par AccesClub). | 100 % | 2 |
| `model/NiveauTournoi` | 67 | 5 | Catégorie d'un tournoi homologué FFTT (règlements sportifs IX.102), de la plus locale à la plus élevée : | 100 % | 22 |
| `model/OptionsReglement` | 45 | 5 | Choix propres au règlement : | 100 % | 7 |
| `model/OrientationTable` | 28 | 2 | Orientation d'une table sur le plan de la salle (B024) : | 100 % | 4 |
| `model/OrigineEvenement` | 13 | 0 | Origine d'une date de l'agenda du compte : | 100 % | 5 |
| `model/Paiement` | 61 | 8 | Moyen de paiement en ligne des inscriptions. | 86 % | 5 |
| `model/PhasePoints` | 17 | 1 | Phase de la saison dont les points classement servent aux inscriptions. | 100 % | 3 |
| `model/PlanSalle` | 316 | 23 | Plan d'une salle sur une grille (B024, cahier des charges v0, section 6) : | 98 % | 6 |
| `model/PolitiqueBalles` | 12 | 0 | Politique de fourniture des balles pour un tournoi. | 100 % | 9 |
| `model/PolitiqueForfaits` | 105 | 9 | Remboursement des droits d'engagement en cas de forfait, selon le moment où le joueur se retire : | 92 % | 2 |
| `model/Poule` | 58 | 8 | Une poule : | 93 % | 45 |
| `model/PrononciationClub` | 22 | 1 | Prononciation phonétique du nom d'un club pour les appels au micro (B018), en alphabet phonétique international (API / IPA), identifiée par le numéro FFTT du club. | 100 % | 2 |
| `model/PrononciationJoueur` | 30 | 2 | Prononciation phonétique d'un joueur pour les appels au micro (B018), en alphabet phonétique international (API / IPA), identifiée par la licence. | 100 % | 4 |
| `model/Qualifie` | 31 | 2 | Un joueur qualifié pour le tableau à élimination, avec la trace de sa poule d'origine et de son rang dans cette poule. | 71 % | 5 |
| `model/QuotasTableaux` | 188 | 22 | Nombre maximum de tableaux par joueur et par jour, distinct pour les joueurs et les joueuses, éventuellement différent selon le jour du tournoi. | 94 % | 6 |
| `model/Recompense` | 120 | 11 | Une récompense attribuée à une place (ou une plage de places) d'un tableau. | 61 % | 7 |
| `model/RefusHomologation` | 25 | 1 | Refus d'une demande d'homologation : | 100 % | 3 |
| `model/ReglagesAnnoncesVocales` | 97 | 13 | Réglages des annonces vocales d'un tournoi (B017) : | 100 % | 2 |
| `model/RegleTableauxParJour` | 131 | 9 | Règle, définie au niveau du Tournoi, limitant le nombre de tableaux auxquels un joueur peut s'inscrire sur une même journée. | 88 % | 1 |
| `model/ResultatPlacementPlan` | 84 | 7 | Résultat détaillé d'une modification du plan d'une salle (B024) : | 100 % | 4 |
| `model/RoleJugeArbitre` | 20 | 1 | Rôle d'un juge-arbitre dans un tournoi. | 86 % | 4 |
| `model/SalleTournoi` | 132 | 16 | Une salle du tournoi (B024) : | 95 % | 6 |
| `model/Sexe` | 10 | 0 | Sexe du joueur, utilisé notamment pour filtrer l'accès aux tableaux réservés (Dames / Messieurs) selon les règles FFTT. | 100 % | 120 |
| `model/StatutEnvoiSms` | 25 | 1 | Issue d'un envoi de SMS inscrite au journal des envois (B019). | 100 % | 6 |
| `model/StatutMatch` | 15 | 0 | Cycle de vie d'un match au cours du tournoi. | 100 % | 33 |
| `model/StatutTableau` | 35 | 0 | Cycle de vie d'un Tableau, le long d'une progression linéaire : | 100 % | 9 |
| `model/StatutTournoi` | 76 | 2 | Cycle de vie d'un Tournoi, du montage du dossier à l'ouverture des inscriptions : | 100 % | 33 |
| `model/StrategieOrdonnancement` | 76 | 3 | Stratégie principale d'ordonnancement choisie par le club pour faire avancer la journée (cahier des charges v0, section 6). | 100 % | 13 |
| `model/Tableau` | 513 | 41 | Un tableau (une épreuve) au sein d'un tournoi : | 97 % | 98 |
| `model/TableauElimination` | 73 | 10 | Représente un tableau à élimination directe complet : | 79 % | 17 |
| `model/TarifInscription` | 82 | 8 | Tarifs d'inscription à un Tableau, selon le mode de règlement. | 67 % | 11 |
| `model/Textes` | 32 | 0 | Nettoyage des textes saisis, commun aux entités du modèle (auparavant recopié dans chaque classe). | 100 % | 3 |
| `model/Tour` | 61 | 8 | Un tour d'un tableau à élimination directe (ex. | 88 % | 23 |
| `model/Tournoi` | 904 | 110 | Racine du modèle : | 96 % | 124 |
| `model/TypeConvocation` | 27 | 1 | Nature d'une convocation par SMS (B019). | 100 % | 6 |
| `model/TypeRecompense` | 20 | 0 | Nature d'une Recompense attribuée à une place d'un tableau. | 100 % | 2 |
| `model/TypeRepere` | 50 | 4 | Repère posé sur le plan d'une salle (B024, cahier des charges v0, section 6) : | 100 % | 6 |
| `model/TypeTournoi` | 20 | 0 | Type d'un tournoi, tel que déclaré dans la demande d'homologation FFTT (champ obligatoire du formulaire SPID). | 100 % | 2 |
| `model/format/FormatConfigurable` | 122 | 12 | Format de tournoi paramétrable, construit via un Builder. | 90 % | 8 |
| `model/format/FormatPoule3Qualifie2` | 61 | 7 | Format « Poule de 3 — 2 qualifiés ». | 89 % | 3 |
| `model/format/FormatTournoi` | 87 | 0 | Décrit les règles structurelles d'un format de tournoi : | 100 % | 12 |
| `model/format/RegistreFormats` | 49 | 3 | Catalogue des FormatTournoi disponibles dans l'application. | 100 % | 51 |
| `model/format/SortNonQualifies` | 18 | 0 | Sort réservé aux joueurs d'une poule qui ne sont pas qualifiés pour le tableau principal. | 100 % | 1 |
| `ordonnancement/Anticipateur` | 109 | 2 | Anticipation à court terme : | 100 % | 1 |
| `ordonnancement/CategorieDuree` | 68 | 7 | Clé identifiant une catégorie de matchs supposés de durée homogène, pour estimer leur temps : | 92 % | 2 |
| `ordonnancement/EstimateurDuree` | 164 | 14 | Estime la durée d'un match selon sa CategorieDuree (phase + niveau). | 92 % | 4 |
| `ordonnancement/LoiDureeMatch` | 152 | 9 | Loi de la durée d'un match pour la simulation (B025) : | 89 % | 2 |
| `ordonnancement/MatchOrdonnancable` | 72 | 9 | Enveloppe un Match avec le contexte nécessaire à l'ordonnancement : | 100 % | 11 |
| `ordonnancement/Ordonnanceur` | 766 | 26 | Moteur d'ordonnancement des matchs sur les tables, réactif et semi-automatique : | 97 % | 13 |
| `ordonnancement/PhaseMatch` | 17 | 0 | Phase d'un match dans le déroulé d'un tableau, utilisée pour prioriser les lancements « par vagues » : | 100 % | 12 |
| `ordonnancement/PoidsOrdonnancement` | 160 | 17 | Pondération configurable des critères de priorité de l'ordonnanceur. | 100 % | 10 |
| `ordonnancement/PrevisionSalle` | 103 | 5 | Fournit des prévisions à court terme sur le déroulé en salle, à partir des matchs en cours, de leur heure de lancement et de l'EstimateurDuree. | 94 % | 1 |
| `ordonnancement/PropositionLancement` | 50 | 6 | Proposition faite à l'organisateur de lancer un match donné sur une table donnée, avec un score de priorité et une explication lisible. | 83 % | 7 |
| `ordonnancement/ReservationsTables` | 121 | 9 | Réservations des tables (B024) que l'Ordonnanceur respecte : | 100 % | 3 |
| `ordonnancement/ResultatAffectationTable` | 121 | 10 | Résultat détaillé d'une opération sur une table de la salle : | 100 % | 5 |
| `ordonnancement/SuggestionAnticipation` | 52 | 6 | Suggestion d'anticipation : | 67 % | 1 |
| `ordonnancement/TempsAnnexesMatch` | 209 | 20 | Temps passés autour du jeu d'un match, pendant lesquels la table et les joueurs sont occupés sans jouer (B025, simulation) : | 95 % | 2 |
| `ordonnancement/ValidationInscription` | 146 | 8 | Valide les inscriptions au regard de la règle — configurable par l'organisateur — interdisant à un joueur de s'inscrire à deux tableaux qui « se suivent », afin d'éviter  | 98 % | 3 |
| `persistence/AnnoncesVocalesMapper` | 65 | 0 | Réglages des annonces vocales en JSON (B017), champ facultatif annoncesVocales du tournoi : | 100 % | 0 |
| `persistence/ApplicationMapper` | 236 | 0 | Traduit les entités d'application — clubs, comptes organisateurs et accès — entre le modèle et le JSON. | 95 % | 1 |
| `persistence/ConfigMapper` | 451 | 0 | Traduit les objets immuables de configuration et de métadonnées entre le modèle et le JSON. | 95 % | 3 |
| `persistence/ContexteSerialisation` | 139 | 0 | État de travail partagé pendant l'écriture puis la lecture d'un document. | 90 % | 0 |
| `persistence/ConversionsCommunes` | 121 | 0 | Fonctions de conversion partagées entre les mappers de la couche persistence. | 95 % | 0 |
| `persistence/ConvocationsSmsMapper` | 120 | 0 | Convocations par SMS en JSON (B019), champ facultatif convocationsSms du tournoi : | 100 % | 0 |
| `persistence/DepotApplication` | 475 | 24 | Stockage des données d'application qui vivent entre les tournois : | 91 % | 7 |
| `persistence/DepotSession` | 43 | 2 | Sauvegarde et recharge une SessionSalle complète (tournoi + discipline + ordonnancement) dans un fichier local, via une écriture atomique. | 80 % | 4 |
| `persistence/DepotTournoi` | 108 | 3 | Point d'entrée de la persistance « document » : | 84 % | 15 |
| `persistence/DerouleMapper` | 245 | 0 | Traduit le déroulé sportif (matchs, manches, poules, tableau à élimination) entre le modèle et le JSON. | 97 % | 0 |
| `persistence/DocumentReglementMapper` | 139 | 0 | Conversion JSON d'un DocumentReglement : | 97 % | 1 |
| `persistence/PlanSalleMapper` | 206 | 0 | Salles et plans du tournoi en JSON (B024), champ facultatif salles du tournoi : | 95 % | 0 |
| `persistence/ProjetTournoi` | 61 | 7 | Entrée de la liste des « tournois récents » : | 53 % | 1 |
| `persistence/ServiceAutosave` | 218 | 9 | Sauvegarde automatique périodique d'une session, pour la reprise après un arrêt non prévu (coupure de courant, plantage, batterie). | 91 % | 3 |
| `persistence/ServiceFichierTournoi` | 380 | 17 | Chef d'orchestre de la persistance côté application : | 85 % | 1 |
| `persistence/SessionMapper` | 223 | 0 | Traduit l'état vivant de l'ordonnancement en salle : | 97 % | 0 |
| `persistence/SessionMapperComplet` | 93 | 2 | Sérialise et reconstruit une SessionSalle complète : | 97 % | 0 |
| `persistence/SessionSalle` | 59 | 6 | Photographie de tout ce qui doit survivre à une interruption en salle : | 100 % | 4 |
| `persistence/TournoiMapper` | 839 | 4 | Assemble le document complet : | 94 % | 7 |
| `persistence/json/Json` | 228 | 34 | Arbre JSON minimal en mémoire, sans aucune dépendance externe. | 89 % | 7 |
| `persistence/json/JsonException` | 19 | 2 | Erreur de lecture, d'écriture ou de structure d'un document JSON. | 50 % | 2 |
| `persistence/json/JsonParser` | 245 | 1 | Analyseur JSON récursif descendant, sans dépendance. | 85 % | 7 |
| `persistence/json/JsonWriter` | 123 | 2 | Sérialise un arbre Json en texte JSON valide, sans dépendance. | 93 % | 3 |
| `ui/AppPingManager` | 71 | 2 | Application JavaFX PingManager. | 0 % | 0 |
| `ui/ContexteApplication` | 951 | 52 | Contexte applicatif : | 69 % | 10 |
| `ui/Lanceur` | 23 | 1 | Point d'entrée de l'application. | 0 % | 2 |
| `ui/diffusion/AdresseReseauLocal` | 57 | 2 | Adresse du réseau local (Wi-Fi de la salle) sur laquelle écoute le suivi sur téléphone : | 40 % | 2 |
| `ui/diffusion/DiffusionSuivi` | 135 | 10 | Cycle de vie du suivi sur téléphone (B021) pour l'application : | 85 % | 2 |
| `ui/diffusion/Html` | 82 | 2 | Outils HTML du suivi sur téléphone : | 100 % | 2 |
| `ui/diffusion/InstantaneSuivi` | 495 | 3 | Photographie immuable du tournoi pour le suivi sur téléphone (B021). | 89 % | 4 |
| `ui/diffusion/PagesSuivi` | 392 | 5 | Pages HTML du suivi sur téléphone (B021), produites depuis un InstantaneSuivi : | 97 % | 1 |
| `ui/diffusion/ServeurSuiviPublic` | 201 | 9 | Petit serveur web local en lecture seule du suivi sur téléphone (B021), sur la classe com.sun.net.httpserver du JDK (aucune dépendance). | 90 % | 2 |
| `ui/ecran/AccueilGestionTournois` | 248 | 0 | Vue de l'accueil « Gestion des tournois » (B033) : | 0 % | 0 |
| `ui/ecran/ApercuReglement` | 132 | 0 | Rendu JavaFX d'un DocumentReglement : | 0 % | 0 |
| `ui/ecran/AssistantInscription` | 457 | 0 | Assistant d'inscription d'un joueur à un tournoi, en quatre étapes : | 0 % | 0 |
| `ui/ecran/Avatars` | 52 | 0 | Logique des avatars à initiales (charte v2), sans JavaFX : | 100 % | 1 |
| `ui/ecran/BlocSaisieSms` | 73 | 0 | Bloc de saisie facultative du portable avec case de consentement explicite (B019), commun à l'inscription en ligne et à l'ajout par le club. | 0 % | 0 |
| `ui/ecran/BlocsInscriptions` | 222 | 0 | Briques d'affichage du remplissage des tableaux, partagées par la vue « Inscriptions » du club et la fiche d'un tournoi (liste des participants) : | 0 % | 0 |
| `ui/ecran/CarteReglesInscription` | 597 | 0 | Carte « Règles d'inscription » de la section Tableaux, sous la grille : | 0 % | 0 |
| `ui/ecran/CarteTournois` | 263 | 2 | Carte interactive des tournois : | 0 % | 0 |
| `ui/ecran/Coquille` | 254 | 4 | Accueil de l'application une fois connecté (charte v2) : | 0 % | 0 |
| `ui/ecran/DispositionArbre` | 318 | 0 | Géométrie de l'arbre des rencontres du tableau final, sans JavaFX : | 98 % | 1 |
| `ui/ecran/DispositionGrille` | 33 | 0 | Calcul pur (sans JavaFX) du nombre de colonnes d'une grille adaptative (Ui#grilleAdaptative) : | 100 % | 1 |
| `ui/ecran/EcranChoixPreparation` | 94 | 0 | Choix imposé juste après la création d'un brouillon : | 0 % | 0 |
| `ui/ecran/EcranComplementaire` | 720 | 0 | Section « Complémentaire » : | 0 % | 0 |
| `ui/ecran/EcranCompletion` | 810 | 0 | Écran de complétion d'un tournoi : | 0 % | 0 |
| `ui/ecran/EcranConnexion` | 243 | 2 | Écran d'entrée : | 0 % | 0 |
| `ui/ecran/EcranConvocationsSms` | 332 | 0 | Convocations par SMS (B019) : | 0 % | 0 |
| `ui/ecran/EcranImportJoueurs` | 505 | 2 | Import d'une liste de joueurs dans les tableaux d'un tournoi (club qui a tout préparé hors de PingManager). | 0 % | 0 |
| `ui/ecran/EcranInscriptionSurPlace` | 152 | 0 | Jour J, « Inscrire sur place » : | 0 % | 0 |
| `ui/ecran/EcranJourJ` | 483 | 0 | Page « Jour J » d'un tournoi (Gestion des tournois → tournoi → « Jour J »). | 0 % | 0 |
| `ui/ecran/EcranPlanSalle` | 589 | 4 | Éditeur du plan de salle (B024), ouvert depuis la deuxième version de l'écran du tournoi : | 0 % | 0 |
| `ui/ecran/EcranPointageTableau` | 515 | 0 | Pointage d'un tableau le jour J, puis lancement du tirage au sort des poules. | 0 % | 0 |
| `ui/ecran/EcranPoulesTableau` | 394 | 0 | Phase de poules d'un tableau lancé : | 0 % | 0 |
| `ui/ecran/EcranPrevisions` | 473 | 0 | Écran « Prévisions et stratégie » (B025), ouvert depuis la deuxième version de l'écran du tournoi : | 0 % | 0 |
| `ui/ecran/EcranPrononciations` | 106 | 0 | Jour J, « Prononciation des noms » (B018) : | 0 % | 0 |
| `ui/ecran/EcranSalle` | 1476 | 0 | Écran principal du juge-arbitre le jour J (B011, B037) : | 0 % | 0 |
| `ui/ecran/EcranSportif` | 827 | 2 | Section « Informations sportives et techniques » : | 0 % | 0 |
| `ui/ecran/EcranSuiviTournoi` | 363 | 0 | Fiche d'un tournoi en mode suivi (onglet Tournois et « Mes tournois »), dès le lancement du premier tableau : | 0 % | 0 |
| `ui/ecran/EcranTableaux` | 632 | 0 | Section « Tableaux et inscriptions », en deux parties : | 0 % | 0 |
| `ui/ecran/EcranTournoiImporte` | 292 | 0 | Vue d'ensemble d'un tournoi préparé hors de PingManager (mode import). | 0 % | 0 |
| `ui/ecran/EditeurScorePhaseFinale` | 185 | 4 | Panneau de saisie du résultat d'un match du tableau final : | 0 % | 0 |
| `ui/ecran/EditeurScorePoule` | 165 | 2 | Saisie du résultat d'une partie de poule : | 0 % | 0 |
| `ui/ecran/FicheTournoi` | 484 | 2 | Fiche d'un tournoi (onglet Tournois) : | 0 % | 1 |
| `ui/ecran/FormulaireNouveauTournoi` | 223 | 3 | Formulaire « Nouveau tournoi » (mise en page v2) : | 0 % | 0 |
| `ui/ecran/FormulairePrononciation` | 216 | 0 | Saisie de la prononciation phonétique d'un joueur (et, au jour J, de son club) pour les appels au micro (B018) : | 0 % | 0 |
| `ui/ecran/FormulaireTableau` | 775 | 2 | Formulaire complet d'un tableau (création ou modification), ouvert depuis l'EcranTableaux écran Tableaux. | 0 % | 0 |
| `ui/ecran/FormulairesSection` | 632 | 2 | Formulaires d'édition d'une section d'un tournoi. | 0 % | 0 |
| `ui/ecran/Icones` | 55 | 0 | Icônes simples de l'interface : | – | 0 |
| `ui/ecran/ImpressionJourJ` | 141 | 0 | Boutons d'impression du jour J (poules, arbre, salle) : | 83 % | 1 |
| `ui/ecran/MesTournois` | 367 | 0 | « Mes tournois » (cahier des charges v0, section 3), en trois parties dans l'ordre : | 0 % | 0 |
| `ui/ecran/PageAccueil` | 168 | 3 | Page d'accueil (« home ») : | 0 % | 0 |
| `ui/ecran/PageCompte` | 531 | 3 | Page « Mon profil » (cahier des charges v0, section 2). | 0 % | 0 |
| `ui/ecran/PageGestionTournoi` | 99 | 2 | Accueil de la gestion des tournois (réservé aux comptes rattachés à un club). | 0 % | 0 |
| `ui/ecran/PagePlaceholder` | 21 | 1 | Page d'attente réutilisable : | 0 % | 0 |
| `ui/ecran/PageSupervision` | 125 | 2 | Supervision (compte « ultra admin ») : | 0 % | 0 |
| `ui/ecran/PageTournois` | 546 | 2 | Onglet Tournois : | 0 % | 0 |
| `ui/ecran/PresentationAccueil` | 192 | 8 | Contenu de l'accueil (« home », cahier des charges v0, section 1), sans JavaFX : | 94 % | 1 |
| `ui/ecran/PresentationAgenda` | 89 | 0 | Logique d'affichage de l'agenda du compte, sans JavaFX (testée) : | 100 % | 2 |
| `ui/ecran/PresentationAvancement` | 278 | 0 | Bandeau « Où en sont les tableaux » de l'écran principal du jour J : | 95 % | 2 |
| `ui/ecran/PresentationCarteTournois` | 170 | 0 | Logique d'affichage de la carte des tournois (onglet Tournois), sans JavaFX (testée) : | 93 % | 1 |
| `ui/ecran/PresentationConvocationsSms` | 262 | 1 | Textes et listes de l'écran des convocations par SMS (B019), sans JavaFX pour être testés. | 72 % | 1 |
| `ui/ecran/PresentationGestionTournois` | 262 | 0 | Présentation de l'accueil « Gestion des tournois » (sans JavaFX), cahier des charges v0, section 4 : | 94 % | 1 |
| `ui/ecran/PresentationInscriptionSurPlace` | 132 | 0 | Textes et listes de l'écran « Inscrire sur place » du jour J, sans JavaFX. | 97 % | 1 |
| `ui/ecran/PresentationJourJ` | 543 | 0 | Textes et calculs d'affichage des écrans du jour J (liste des tableaux, pointage, tirage, poules). | 72 % | 3 |
| `ui/ecran/PresentationMesTournois` | 234 | 0 | Logique d'affichage de « Mes tournois » (cahier des charges v0, section 3), sans JavaFX : | 93 % | 1 |
| `ui/ecran/PresentationNouveauTournoi` | 153 | 0 | Présentation du formulaire « Nouveau tournoi » (sans JavaFX) : | 100 % | 1 |
| `ui/ecran/PresentationPhaseFinale` | 522 | 0 | Présentation de la vue « Phase finale », sans JavaFX : | 97 % | 5 |
| `ui/ecran/PresentationPlanSalle` | 320 | 0 | Mots et disposition du plan de salle (B024), sans JavaFX : | 90 % | 1 |
| `ui/ecran/PresentationPreparationTournoi` | 311 | 0 | Présentation (sans JavaFX) de la vue d'ensemble d'un tournoi en préparation (B035, cahier des charges v0 section 6) : | 98 % | 1 |
| `ui/ecran/PresentationPrevisions` | 838 | 0 | Présentation (sans JavaFX) de l'écran « Prévisions » (B025) : | 94 % | 2 |
| `ui/ecran/PresentationProfil` | 260 | 1 | Contenu de « Mon profil » (cahier des charges v0, section 2), sans JavaFX. | 94 % | 1 |
| `ui/ecran/PresentationPrononciation` | 153 | 0 | Contenu des écrans de prononciation phonétique (B018) : | 98 % | 1 |
| `ui/ecran/PresentationRemplissageFinances` | 180 | 0 | Présentation (sans JavaFX) de la carte « Remplissage et finances » de la deuxième version de l'écran du tournoi (B036) : | 99 % | 1 |
| `ui/ecran/PresentationSaisieSms` | 85 | 0 | Textes de la saisie facultative du numéro de portable pour les convocations par SMS (B019), sans JavaFX pour être testés : | 100 % | 1 |
| `ui/ecran/PresentationSalle` | 575 | 0 | Logique d'affichage de la salle (textes, durées, messages de refus), sans JavaFX : | 92 % | 3 |
| `ui/ecran/PresentationSuiviTournoi` | 357 | 0 | Présentation du suivi d'un tournoi en cours (fiche du tournoi en mode suivi, onglet Tournois et « Mes tournois »), sans JavaFX : | 94 % | 2 |
| `ui/ecran/PresentationTournoiImporte` | 106 | 0 | Logique d'affichage (sans JavaFX) du tournoi préparé hors de PingManager. | 95 % | 1 |
| `ui/ecran/PropositionsTournoi` | 63 | 0 | Valeurs proposées par défaut lors de la saisie d'un tournoi (toujours modifiables). | 100 % | 1 |
| `ui/ecran/Saisie` | 151 | 3 | Lecture tolérante des saisies courantes (heures, montants, entiers) et formatage à la française. | 46 % | 4 |
| `ui/ecran/SaisieScorePhaseFinale` | 146 | 0 | Logique, sans JavaFX, du formulaire de saisie du score d'un match de phase finale : | 100 % | 1 |
| `ui/ecran/StatutPastille` | 62 | 0 | Statut d'une pastille (charte v2) : | 100 % | 5 |
| `ui/ecran/Theme` | 99 | 3 | Thème visuel de l'application : | 0 % | 0 |
| `ui/ecran/Ui` | 939 | 2 | Point central de la présentation : | 0 % | 0 |
| `ui/ecran/VueInscriptionsClub` | 309 | 0 | Prévisualisation du remplissage des tableaux côté club organisateur (Gestion des tournois → tournoi → « Inscriptions ») : | 0 % | 0 |
| `ui/ecran/VuePhaseFinale` | 589 | 2 | Vue « Phase finale » d'un tableau, ouverte depuis la page Jour J : | 2 % | 1 |
| `ui/ecran/VueReglementFige` | 160 | 0 | Règlement et feuille d'engagement d'un tournoi publié : | 0 % | 0 |
| `ui/infra/DiffuseurAnnonces` | 91 | 6 | Fait parler la file d'annonces (B017) sur un fil de fond unique, pour ne jamais figer l'écran pendant que la voix parle. | 18 % | 0 |
| `ui/infra/DonneesDemo` | 150 | 3 | Jeu de données FFTT de démonstration, alimenté à la main, pour dérouler les deux démarches (liaison licence, liaison club) sans vraie base fédérale. | 100 % | 16 |
| `ui/infra/EmailConsole` | 81 | 6 | Implémentation de démonstration du port EnvoiEmail : | 4 % | 0 |
| `ui/infra/EnvoiSmsSimule` | 48 | 2 | Implémentation simulée du port EnvoiSms (B019) : | 100 % | 1 |
| `ui/infra/GenerateurCodeAleatoire` | 20 | 1 | Implémentation concrète du port GenerateurCode : | 50 % | 0 |
| `ui/infra/GeocodeurGeoplateforme` | 75 | 1 | Géocodage par l'API de la Géoplateforme IGN (ex-API Adresse de la Base Adresse Nationale, transférée à l'IGN en 2025 : | 0 % | 0 |
| `ui/infra/HachageMotDePassePBKDF2` | 84 | 2 | Implémentation concrète du port HachageMotDePasse avec PBKDF2 (algorithme du JDK, aucune dépendance externe). | 75 % | 1 |
| `ui/infra/ImpressionFeuilles` | 129 | 3 | Met une feuille PDF entre les mains de la table de marque : | 52 % | 1 |
| `ui/infra/JoueursFictifs` | 164 | 2 | Joueurs fictifs de la démo (noms inventés, licences « 99… » qui n'existent pas à la FFTT) et leurs clubs, pour remplir les tableaux et voir la prévisualisation du remplis | 98 % | 1 |
| `ui/infra/RegistreLicencesFFTT` | 84 | 5 | Fausse base FFTT des licences, en mémoire, pour les tests manuels. | 84 % | 0 |
| `ui/infra/RegistresImportes` | 153 | 11 | Licenciés et clubs importés d'un fichier fourni par le club (B023), superposés à la base existante : | 78 % | 1 |
| `ui/infra/RenduPdfFeuilles` | 630 | 3 | Rendu PDF (PDFBox, police Inter intégrée) des feuilles du jour J préparées par le cœur (core.feuilles.GenerateurFeuilles) : | 96 % | 2 |
| `ui/infra/SyntheseVocaleWindows` | 220 | 4 | Synthèse vocale de Windows (B017) : | 62 % | 2 |
| `ui/infra/TournoiDemoBrunoy` | 207 | 1 | Le vrai Tournoi National B de Brunoy 2027, à l'état réel : | 100 % | 11 |
| `ui/infra/TournoiDemoBrunoyJourJ` | 306 | 2 | Copie du National B de Brunoy (TournoiDemoBrunoy) mise au jour J : | 100 % | 5 |
| `ui/infra/TournoisDemo` | 80 | 0 | Outils des tournois de démo : | 96 % | 2 |

## Annexe C — Commandes et champs de chaque écran (libellés, avec la ligne)

| Écran | Boutons, liens, cases, onglets | Champs étiquetés |
|---|---|---|
| `AccueilGestionTournois` | +   (60) · Consulter (231) |  |
| `AssistantInscription` | ← Fiche du tournoi (83) · Continuer (135) · Annuler (189) · J'ai lu le règlement du tournoi et je l'accepte. (294) · Confirmer l'inscription (296) · ← Modifier mes tableaux (315) · Payer en ligne (bientôt disponible) (387) · Continuer (408) · Voir la fiche et les participants (432) · Retour aux tournois (434) |  |
| `BlocSaisieSms` |  | Numéro de portable (35) |
| `CarteReglesInscription` | Le nombre varie selon le jour (59) · Obligatoire : le jour où il est joué, une joueuse doit s'inscrire à ce tableau (64) · Interdire deux tableaux qui se suivent dans la journée (65) · Après une élimination, autoriser l'inscription sur place  (66) · Les frais de paiement en ligne ne sont pas remboursés (84) · Enregistrer les règles (97) · Sur justificatif (472) | Joueurs (max. par jour) (167) · Joueuses (max. par jour) (168) · Tableau supplémentaire des joueuses : seulement si elles jouent (200) · Tableau annulable sous … inscrits (383) · Décision prise le (384) |
| `Coquille` | Se déconnecter (110) |  |
| `EcranChoixPreparation` | ← Gestion des tournois (37) |  |
| `EcranComplementaire` | Générer le règlement à partir des informations saisies (73) · Déposer mon propre règlement (fichier PDF) (74) · Licence promotionnelle acceptée (88) · Joueurs étrangers autorisés (89) · ← Vue d'ensemble (114) · Enregistrer (119) · Choisir un fichier PDF… (170) · Retirer (176) · Aperçu du règlement (195) · Télécharger le règlement (PDF)… (206) · Feuille d'engagement (PDF)… (217) · Retirer (272) · ← Complémentaire (443) · Télécharger le règlement (PDF)… (446) · Choisir une image… (512) · Retirer (518) · HelloAsso (598) · Plateforme PingManager — bientôt disponible (600) · Autre lien de paiement — bientôt disponible (602) | Horaires de la buvette (291) · Heure d'ouverture de la salle (296) · Minutes après la fin du pointage (vide = dès la fin) (300) · Catégorie minimale admise dans les tableaux toutes catégories (305) · Précisions (facultatif) (316) · Titre de l'article (320) · Texte (321) · Site web du tournoi (inscriptions, informations) (589) · Lien de la billetterie HelloAsso (638) |
| `EcranCompletion` | Tournoi déjà préparé ailleurs ? Déposer le règlement et importer  (134) · Voir les inscriptions (160) · Importer des joueurs… (163) · Choisir la stratégie et prévoir la journée (257) · Demander l'homologation (550) · Retirer la demande (569) · Publier le tournoi (613) · Homologation acceptée (684) · Homologation refusée (694) · Enregistrer le refus (701) · Annuler (702) | Numéro d'homologation (603) · Motif du refus (703) · Numéro d'homologation (728) |
| `EcranConnexion` | Se connecter (49) · Pas encore de compte ? Créer un compte (50) | Prénom (68) · Nom (69) · Adresse email (77) · Mot de passe (78) |
| `EcranConvocationsSms` | ← Jour J (76) · Envoyer les SMS automatiquement quand un match est envoyé sur une table (109) · Enregistrer les réglages (117) · Enregistrer le numéro (162) · Retirer (181) · Annuler (209) · SMS aux joueurs (239) · SMS à l'arbitre (245) | Expéditeur (nom affiché sur le téléphone) (137) · Limite d'envois pour le tournoi (140) · Joueur ou arbitre (221) |
| `EcranImportJoueurs` | Selon la colonne « Tableau » du fichier (66) · Tous dans le tableau : (67) · Créer les tableaux du fichier qui n'existent pas encore (70) · ← Retour (87) · Choisir un fichier… (109) · Modèle Excel… (122) · Modèle CSV… (124) · Licenciés du club (CSV)… (148) · Clubs (CSV)… (150) · Voir les inscriptions (434) · Importer un autre fichier (436) |  |
| `EcranInscriptionSurPlace` | Engagement réglé à la table (41) · ← Jour J (51) · Inscrire (129) |  |
| `EcranJourJ` | Ouvrir le jour J (96) · ← Jour précédent (219) · Jour suivant → (222) · Afficher en grand et imprimer (249) · Arrêter le suivi (251) · Ouvrir le suivi sur téléphone (267) · ← Jour J (295) · Imprimer (297) · Ouvrir le pointage de  (442) · Annuler (443) |  |
| `EcranPlanSalle` | ← Retour au tournoi (105) · Enregistrer le plan (115) · Renommer / changer d'étage (152) · Ajouter une salle (165) · Oui, retirer «  (173) · Non (175) · Retirer cette salle (188) · Renuméroter (420) · Tourner ( (431) · Retirer du plan (446) · Appliquer le plan type (502) · Changer la taille (527) · Oui, remplacer (535) · Non (536) | Salle affichée (160) · Étage (0 = rez-de-chaussée) (161) · Nouvelle salle (167) · Réservée au tableau (441) · Plan type (516) · Tables (516) · Colonnes (529) · Lignes (529) · Salle réservée aux tableaux (566) · Tableaux joués de préférence ici (567) |
| `EcranPointageTableau` | ← Jour J (116) · Présent (218) · Payé (221) · Confirmer (260) · Retirer (266) · Désistement (271) · Annuler (292) · Confirmer le désistement (295) · Retirer pour cas particulier (311) · Rembourser l'engagement (312) · Retirer  (332) · Annuler (333) · Clôturer le pointage… (393) · Clôturer maintenant (399) · Annuler (400) · Voir les poules (425) · Lancer le tirage au sort… (444) · Faire le tirage (458) · Annuler (459) | Graine du tirage (475) |
| `EcranPoulesTableau` | ← Jour J (105) · Poule  (203) · Imprimer la feuille de poule (235) · Feuilles de match (239) · Corriger (323) · Saisir (323) · Feuille (332) |  |
| `EcranPrevisions` | ← Retour au tournoi (64) · Compléter les tableaux jusqu'à leurs places maximales (joueurs fictifs) (161) · Simuler les trois stratégies (164) | Fin visée –  (159) · Tables (187) · Tirages (187) · Graine (188) · Durée moyenne du jeu (min) (190) · Minimum (min) (191) · Maximum (min) (191) · Appel (min) (196) · Joueur à rappeler (%) (197) · Retard d'un rappel (min) (198) · Échauffement (min) (199) · Pause entre manches (s) (201) · Temps mort pris (% par joueur) (202) · Retour de la feuille (min) (203) |
| `EcranPrononciations` | ← Jour J (45) |  |
| `EcranSalle` | Fermer (228) · _Proposer (398) · _Lancer un match (405) · Demander à chaque lancement (427) · Feuille à l'envoi (432) · SMS… (454) · Inscrire sur place… (463) · Répétition (503) · Rappel si la table reste vide (511) · Débit (519) · Voix (526) · Prononciation des noms… (536) · Envoyer  (571) · Voir toutes les salles (664) · Envoyer le prochain (731) · Feuille (766) · Annoncer (781) · Joueurs à la table (788) · Oui, annuler (802) · Non (804) · Annuler (816) · Remettre à jouer (886) · Envoyer à la table  (999) · ← Matchs proposés (1037) · Envoyer à la table  (1069) · Voir le tableau (1080) · ← Matchs proposés (1132) · Retenir ce choix pour ce tournoi (modifiable en haut de la page) (1215) · Ne pas lancer (1241) |  |
| `EcranSportif` | ← Vue d'ensemble (106) · Enregistrer le matériel et l'aire de jeu (111) · Désigner (343) · Retirer (383) · + Ajouter des tables d'une autre marque ou couleur (454) · Ajouter (473) · +  (488) · Retirer (515) · Retirer (546) · Remettre les valeurs réglementaires (625) | Rôle (174) · Marque (441) · Tables de compétition : nombre, marque et couleur (463) · Salle d'échauffement : nombre de tables (465) · Balles refusées (liste noire) (499) · Longueur de l'aire (m) (636) · Largeur de l'aire (m) (637) · Éclairage (lux) (638) |
| `EcranSuiviTournoi` | ← Retour aux tournois (155) · Informations du tournoi (163) · Actualiser (171) |  |
| `EcranTableaux` | ← Vue d'ensemble (81) · Enregistrer les échéances (121) · Proposer un ordre (223) · Appliquer cet ordre (256) · Annuler (262) · Ajouter un tableau (352) · Modifier (451) · Retirer (453) · Horaires (479) · Enregistrer les horaires (506) · Annuler (507) · ← Tableaux (545) | Ouverture des inscriptions (113) · Heure d'ouverture (115) · Clôture des inscriptions (116) · Heure de clôture (118) · Début du premier tableau, chaque jour (214) · Écart entre le 1er et le 2e tableau (minutes) (215) · Puis écart suivant, en alternance (minutes) (216) · Fin du pointage … minutes avant le début (217) · Ouverture du pointage … minutes avant le début (218) · Jour (548) · Heure de début (549) · Début du pointage (550) · Fin du pointage (551) |
| `EcranTournoiImporte` | ← Gestion des tournois (59) · Remplacer (106) · Choisir le PDF… (106) · Réimporter un classeur… (147) · Importer le classeur… (148) · Gérer les tableaux (175) · Dates et lieu (179) · Ouvrir le jour J → (242) · Voir les inscriptions (272) · Préparer plutôt ce tournoi entièrement dans PingManager (282) |  |
| `EditeurScorePhaseFinale` | Fermer (124) | Joueur absent, qui abandonne ou disqualifié (84) · Résultat (129) |
| `EditeurScorePoule` | Enregistrer le score (77) · Annuler (88) · Enregistrer l'issue (104) · Effacer le résultat (124) | Score, manche par manche (113) · Ou issue non normale (119) |
| `FicheTournoi` | ← Retour aux tournois (83) · Suivre le tournoi en direct (101) · Règlement (PDF)… (363) · Feuille d'engagement (PDF)… (364) |  |
| `FormulairePrononciation` | Écouter (49) · Enregistrer (101) · Aide : symboles et exemples (108) |  |
| `FormulaireTableau` | Inscription en ligne (89) · Inscription sur place (91) · Petite finale (match pour la 3e place) (96) · Annuler (124) · Tous les jeunes (299) · Tous les vétérans (301) · Tout décocher (303) · Toutes catégories (311) · Sélectionner des catégories (312) · Retirer (475) · + Ajouter une récompense (562) | Nom (200) · Points minimum (249) · Points maximum (250) · Exclure les numérotés : rangs nationaux 1 à (251) · Catégories concernées (331) · Catégorie de points (345) · Sexe (348) · Jour (384) · Heure de début (385) · Début du pointage (386) · Fin du pointage (387) · Fin théorique (388) · Capacité (joueurs) (406) · Liste d'attente (places) (407) · Montant en ligne (€) (419) · Montant sur place (€) (420) · Dotation du tableau (565) · Format de compétition (604) |
| `FormulairesSection` |  (171) · Reprendre l'adresse du club (230) · Reprendre l'administrateur principal (261) · Enregistrer (490) · Annuler (499) | Club (296) · Nom du club (299) · N° d'affiliation (300) · Nom du tournoi (345) · Niveau du tournoi (catégorie) (346) · Numéro d'homologation (347) · Phase concernée (points retenus) (349) · Adresse (voie) (355) · Code postal (356) · Ville (357) · Date de début (363) · Date de fin (364) · Prénom (368) · Nom (369) · Email (370) · Téléphone (371) |
| `MesTournois` | Voir la fiche (148) · Me désinscrire (155) · Confirmer la désinscription (175) · Annuler (176) · Exporter (.ics) (270) · Retirer (293) · Ajouter la date (319) | Ajouter une date personnelle (336) |
| `PageCompte` | Envoyer le code (54) · Vérifier et lier (141) · Voir les tournois (173) · Valider le code (214) · Se dissocier (299) |  |
| `PageSupervision` | Ouvrir (98) |  |
| `PageTournois` | Mes tournois (104) · Tableaux à mes points (201) · Places restantes (204) · Seulement ceux auxquels je peux participer (206) · Réinitialiser les filtres (215) | Statuts (230) · Recherche (237) · Période (238) · Niveau (239) · Zone (240) · Distance (241) |
| `VueInscriptionsClub` | ← Vue d'ensemble du tournoi (79) · Par tableau (85) · Par joueur (86) · Actualiser (91) · Importer des joueurs… (93) · Enregistrer le numéro (267) · Retirer le numéro (276) |  |
| `VuePhaseFinale` | ← Jour J (178) · Imprimer le tableau final (190) · Tirer le tableau final (228) · Exporter les résultats (CSV)… (305) · Imprimer la feuille de match (563) | Graine du tirage (225) |
| `VueReglementFige` | Règlement (41) · Feuille d'engagement (PDF)… (43) · Télécharger le règlement (PDF)… (58) · Feuille d'engagement (PDF)… (68) |  |
