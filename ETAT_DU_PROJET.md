# PingManager — État du projet

> Document central, à garder à la racine du projet. Il donne à tout moment une
> vue complète : ce qu'est le projet, ce qui est fait, comment c'est organisé,
> les décisions prises, les réserves connues, et la feuille de route.
>
> **Pour reprendre le travail dans un nouveau chat** : fournir ce document + le
> zip du projet, et indiquer sur quel domaine on travaille. Tout le contexte
> nécessaire est ici.

---

## 0. Point au 2 novembre 2026 (à lire en premier)

Les sections suivantes datent du chat « persistance » ; depuis, ont été réalisés :
- **Interface JavaFX** : connexion, comptes (licence liée, rattachement à un club), navigation (`Coquille`), gestion des tournois du club, supervision « ultra admin ».
- **Pré-tournoi complet à l'écran** : sections Général, Tableaux et inscriptions (ordonnancement automatique des horaires), Informations sportives et techniques, Complémentaire ; cycle BROUILLON → DEMANDE_HOMOLOGATION → HOMOLOGUE → PUBLIE.
- **Règlement généré** (aperçu + PDF PDFBox, police Inter) et **feuille d'engagement** PDF remplie depuis le tournoi.
- Tournoi de référence : National B de Brunoy, 10-11 avril 2027 (`ui.infra.TournoiDemoBrunoy`).
- **Phase « remplissage des tableaux et onglet Tournois » (2 novembre 2026)** :
  - cœur : `Territoires` (département → ligue, ressource `departements.csv`), `EligibiliteTournoi` (IX.101 / IX.102), catégorie d'âge depuis la date de naissance (II.302 / II.303) contrôlée par `ServiceInscription` (catégories non admises, catégories du tableau), `ServiceInscription.simuler` / `inscrireSelection` (essai à blanc, tout ou rien), `ServiceInscriptionEnLigne`, `CalendrierTournoi`, `CatalogueTournois` (filtres), `RemplissageTableaux`, `MotifsInscription`, port `Geocodeur` ; coordonnées facultatives dans `AdresseTournoi` ;
  - interface : vue **Inscriptions** du club (par tableau, par joueur), onglet **Tournois** (carte WebView + Leaflet embarqué, liste et filtres, fiche, assistant d'inscription, paiement « fait ultérieurement », Mes tournois) ;
  - démo v11 : Brunoy **publié** (n° d'homologation fictif) et rempli par 375 joueurs fictifs, 5 autres tournois publiés.
- **Suite du 2 novembre (après-midi)** : liste d'attente sans paiement dû (repêché → « à régler », ou réglé au pointage le jour J) ; carte sur le **Plan IGN v2** (sans clé) avec repli OpenStreetMap ; **import de joueurs** (Excel .xlsx, LibreOffice .ods, CSV, JSON ; Nom, Prénom, Points, Club obligatoires) pour un tournoi préparé hors de PingManager, avec création des tableaux ; horaires modifiables sur un tableau qui a des inscrits. Démo v12.
- **Suite (13 h)** : après la création d'un brouillon, **choix imposé** entre préparation complète et import (`ModePreparation`, `EcranChoixPreparation`, `EcranTournoiImporte`) ; **classeur Excel modèle à deux feuilles** (Tableaux avec points imposés, Joueurs avec catégorie et tableaux en listes déroulantes) lu par l'import ; carte corrigée (Leaflet en 2D dans la WebView).
- **412 tests verts.** Outils de compilation hors Maven dans `outils-dev/`.

Détails : `claude/PASSATION_CHOIX_ET_CLASSEUR.md` (dernier lot), `claude/PASSATION_IMPORT_ET_J-1.md` (import et phase J-1), `claude/PASSATION_ONGLET_TOURNOIS.md` (état de fin de phase), `claude/REGLES_CREATION_TOURNOI.md`, `claude/PASSATION_PHASE_INSCRIPTIONS.md`.
**Phase suivante** : J-1, tournoi prêt (tableau de bord de la veille, listes de pointage, mise à jour des points, pointage et lancement à l'écran). Plus tard : paiement HelloAsso, désinscription par le joueur, écrans adaptés aux petites tailles.

---

## 1. En une phrase

Logiciel de bureau (Java) pour **gérer des tournois individuels officiels de
tennis de table FFTT**, destiné à la vente, avec un cœur métier totalement
découplé de l'interface et de la base de données.

---

## 2. Objectif et périmètre

Le logiciel vise quatre axes :

1. **Pré-tournoi** : création/publication d'un tournoi, homologation FFTT,
   règlement du tournoi.
2. **Déroulé du tournoi** : poules, tableaux, ordre des matchs, ordonnancement
   des tables. ← *cœur réalisé à ce jour*
3. **Enregistrement** : saisie des résultats, remontée fédérale.
4. **Bonus** : buvette, saisie mobile des résultats par les joueurs.

Tournois visés : épreuves **individuelles** homologuées (type Maisons-Alfort,
Sucy, Lille), **pas** le Critérium ni les épreuves par équipes.

---

## 3. Choix structurants (non négociables)

- **Stack** : Java 21 + Maven. Interface **JavaFX** prévue pour la v1 (bureau),
  web plus tard.
- **Architecture MVC à cœur découplé** : les packages `model`, `model.format`,
  `core` et `ordonnancement` ne connaissent **ni l'UI ni la base de données**.
  C'est la règle d'or, pour la revente et un futur passage au web.
- **Format cible v1** : poule de 3, **2 qualifiés** (1er + 2e), 3e éliminé,
  élimination directe ensuite, matchs au meilleur des 5 manches (best-of 5).
  Le moteur reste **générique** (d'autres formats s'ajouteront comme fichiers).
- **Conformité FFTT vérifiée sur les règlements officiels** (PDF fournis,
  édition 2026-2027), pas sur des sources secondaires.

---

## 4. Contrainte FFTT importante (intégration officielle)

L'homologation et la remontée officielle des résultats passent **obligatoirement**
par les outils fédéraux **SPID V2** (portail) et **GIRPE** (logiciel de salle),
verrouillés derrière authentification FFTT. PingManager ne les remplace pas : il
apporte le confort de gestion en salle et produira un **export**. Le format
d'échange GIRPE n'est pas public : il est communiqué **lors de l'homologation**
(contact : spidv2@fftt.org). À traiter dans le chat « pré-tournoi / homologation ».

---

## 5. Organisation du code

```
com.pingmanager
├── model              (entités pures : joueur, match, poule, tableau…)
│   └── format         (définition des formats de tournoi)
├── core               (services métier : génération, classement, placement…)
└── ordonnancement     (tables, priorités, prévision, anticipation)
```

### 5.1 `model` — entités

`Joueur`, `Sexe`, `Manche`, `Match`, `StatutMatch`, `Poule`, `Tour`,
`TableauElimination`, `ConfigurationTableau`, `Tableau`, `Tournoi`,
`FormatPhaseFinale`, `LigneClassement`, `Qualifie`, `HorairesTableau`,
`IssueMatch`, `CouleurCarton`, `Carton`, `ModeLimiteClassement`, `CategorieAge`,
`TypeRecompense`, `Recompense`, `GrilleRecompenses`, `TarifInscription`,
`RegleTableauxParJour`, `StatutTableau`.

Points clés :
- `Joueur` : la **licence** est l'identité. Sentinelle « à déterminer » pour les
  emplacements de tableau non encore connus. Porte un **rang national** optionnel
  (numérotation française), importé plus tard, pour l'exclusion des mieux numérotés.
- `ConfigurationTableau` (immuable, builder) : règles structurelles du format +
  paramètres d'organisation : capacité, **tarifs en ligne/sur place**
  (`TarifInscription`), **grille de récompenses** (`GrilleRecompenses`),
  **mode de limite de classement** (`ModeLimiteClassement` : tranche / plafond /
  toutes séries), **exclusion par rang national**, **sexe requis**, **catégories
  d'âge autorisées** (`CategorieAge`). Anciennes bornes `pointsMin/pointsMax`
  conservées en raccourcis (compat).
- `Tableau` : inscrits, file d'attente (unique et continue), poules ;
  `inscrire`/`desinscrire` bas niveau (capacité, doublon, promotion depuis la
  file). **Liste d'attente bornée** (`tailleMaxListeAttente`, défaut 4) : quand
  capacité et file sont pleines, l'inscription est refusée. **Pointé/payé par
  tableau** : chaque tableau sait qui a pointé et qui a payé (deux booléens
  indépendants par inscrit) ; la présence est un fait *par tableau*, pas au niveau
  du tournoi. **Pointage et paiement réservés aux inscrits** (pas la liste
  d'attente). **Cycle de vie unifié** (`StatutTableau` INSCRIPTION → POINTAGE →
  LANCE) : `ouvrirPointage()` (le JA, tableau par tableau), `marquerLance()` (via
  le service de lancement) ; le fait « pointage clôturé » (`pointageEstCloture`)
  est un marqueur distinct, pré-condition du lancement, pas un état. **Format**
  (`FormatTournoi`) porté par la configuration. **Phase finale reliée** : le tableau
  porte son `TableauElimination` (statut `PHASE_FINALE`) une fois les poules
  terminées (`poulesTerminees()`). **Remboursement tracé** (survit au retrait).
  Refuse l'inscription du joueur sentinelle « à déterminer ». Primitives
  `retirerInscrit`/`repecherPremier` pour un repêchage piloté par les services.
- `Tournoi` : porte la **règle de tableaux par jour** (`RegleTableauxParJour`,
  quota + exception féminine configurable).

### 5.2 `model.format` — formats

`FormatTournoi` (interface), `FormatPoule3Qualifie2` (**le format actuel**,
identifiant `P3Q2`), `FormatConfigurable` (builder : best-of finale, petite
finale…), `SortNonQualifies`, `RegistreFormats` (catalogue + format par défaut).

**Ajouter un format** = créer une classe implémentant `FormatTournoi` et
l'enregistrer dans `RegistreFormats`. Le moteur n'a pas à changer.

### 5.3 `core` — services métier

- `GenerateurPoules` : répartition en serpentin, séparation des clubs, **départage
  aléatoire reproductible des ex æquo** (I.302).
- `GenerateurMatchsPoule` : **ordres officiels I.301** (poules 3/4/5/6, selon 1-ou-3
  vs 2 qualifiés) + **règle anti-arrangement** (mêmes clubs au 1er tour).
- `ClassementPouleService` : classement I.303 (2/1/0 points, 0 pour forfait/
  abandon/refus/disqualification, départages).
- `GenerateurTableauElimination` : cadre, exemptions (byes), seeding.
- `PlacementQualifiesService` : placement I.304/I.305 (têtes de série tirées au
  sort par blocs 3-4/5-8/9-16, 2es en demi opposé, 3es en quart opposé,
  séparation des clubs au 1er **et 2e tour** si tableau incomplet).
- `ProgressionTableau` : propage les vainqueurs ; gère la petite finale (perdants
  des demies).
- `MoteurTournoi` : **orchestre une épreuve selon un format** (poules → qualifiés
  → tableau), applique les best-of.
- `RegistreDisciplinaire` : historique des cartons ; règle **3.5.2.9** (2
  disqualifications → exclusion de la compétition).
- `GestionCartonsMatch` : progression des cartons (jaune → +1 → +2), cartons hors
  match.
- `ServiceInscription` : **inscription d'un joueur à un tableau au niveau tournoi**.
  Orchestre, dans l'ordre : éligibilité (classement/rang/sexe), **quota de tableaux
  par jour** (avec exception féminine), règle configurable « pas deux tableaux qui
  se suivent » (`ValidationInscription`), puis délègue à `Tableau.inscrire`
  (capacité + liste d'attente bornée). Renvoie un `ResultatInscriptionTournoi` avec
  motif précis (dont `LISTE_ATTENTE_PLEINE`). Fait le pont `model` ↔
  `ordonnancement` sans polluer le modèle.
- `ServicePointage` : **pointage et repêchage (jour J)**, distinct du remplissage.
  Présence et paiement sont des faits **par tableau** (`pointer`/`marquerPaye` sur
  un tableau donné). `cloturerPointage(tableau)` libère les inscrits **non pointés**
  (quel que soit le paiement) et repêche la liste d'attente (unique) dans l'ordre.
  Tout repêché doit **repasser par le pointage** (il n'a ni pointé ni payé ce
  tableau — c'est au pointage qu'il règle) ; corrections manuelles du juge-arbitre
  via `confirmerRepeche` / `retirerRepeche`. **Désistement** : `desister` (avant
  clôture, libère et repêche) ; après clôture le désistement normal est refusé
  (joueur tenu de jouer), seul `desisterCasParticulier` du JA (blessure…) force le
  retrait et trace un remboursement. Chaque tableau clôture indépendamment.
- `ServiceLancementTableau` : **charnière inscription → déroulé sportif**.
  `lancer(tableau, graine)` exige la phase POINTAGE et un pointage clôturé
  (pré-condition stricte), prend les présents (inscrits restants), appelle
  `MoteurTournoi.preparerPoules` avec le format du tableau et un **tirage
  reproductible** (graine conservée, tirage public IX.109), range les poules et
  passe le tableau en LANCE. Renvoie un `ResultatLancement` (poules, graine,
  présents). **`construirePhaseFinale(tableau, graine)`** : une fois toutes les
  poules terminées (bloqué sinon), construit le tableau à élimination via
  `MoteurTournoi.construireTableau` (qualifiés I.305, best-of, petite finale), le
  range dans le tableau et passe au statut PHASE_FINALE. Second tirage public
  reproductible, graine propre distincte de celle des poules.

### 5.4 `ordonnancement` — tables et fluidité

- `Ordonnanceur` : moteur **réactif semi-automatique**. Contrainte dure (un
  joueur ≠ deux tables). Score de priorité réglable : vagues (poules avant
  tableau), équité (attente), déblocage (joueur engagé sur plusieurs tableaux).
- `PropositionLancement`, `PoidsOrdonnancement`, `MatchOrdonnancable`, `PhaseMatch`.
- `ValidationInscription` : règle **configurable** « pas deux tableaux qui se
  suivent » (clause d'organisateur, pas règle fédérale).
- `EstimateurDuree` : durée d'un match par catégorie (phase + niveau), valeur de
  départ puis **apprentissage** (moyenne mobile robuste).
- `PrevisionSalle` : quand une table se libère, quand un joueur est de nouveau
  disponible.
- `Anticipateur` : suggère de **préparer** les matchs bientôt jouables (un joueur
  clé finit sous peu), pour enchaîner sans temps mort.

### 5.5 `core` — pré-tournoi (comptes, cycle de vie, habilitation)

- **Comptes & clubs.** `ServiceComptesOrganisateurs` : inscription d'un
  organisateur → génération d'un code → envoi à l'**email officiel du club** →
  vérification → création d'un `AccesClub`. Sans état ; unicité de l'email
  déléguée à un prédicat fourni par l'appelant (repository plus tard). Effets de
  bord derrière des **ports** : `EnvoiEmail`, `GenerateurCode`,
  `HachageMotDePasse`, `Horloge` (aucune implémentation de production dans le
  livrable — le cœur reste pur). Référentiel `RegistreClubs` (+ impl mémoire
  `RegistreClubsMemoire`). Objets : `DemandeVerification`,
  `ResultatInscriptionOrganisateur`, `ResultatConfirmation`.
- **Cycle de vie.** `ServiceCycleTournoi` : `homologuer` (enregistre le numéro
  délivré par la FFTT + vérifie la **grille H**), `publier` (vérifie la **grille
  P** + calcule la **dotation globale**), `annuler`, plus
  `elementsManquantsPourHomologation` / `elementsManquantsPourPublication`
  (consultables à tout moment) et `avertissementsPourHomologation` (JA/niveau, non
  bloquant). Transition illégale → `IllegalStateException` ; dossier incomplet →
  `ResultatTransition` avec la liste des manques.
- **Habilitation.** `ServiceTournois` : création et transitions réservées à un
  organisateur **habilité pour le club** (contrôle d'`AccesClub`, `SecurityException`
  sinon). Double filet (création + chaque transition).
- **Couplage statut ↔ déroulé.** `PolitiqueOuverture` (partagée) : `ServiceInscription`,
  `ServicePointage` et `ServiceLancementTableau` refusent un tournoi ANNULE (défaut)
  ou exigent PUBLIE (mode strict).

### 5.6 `persistence` — sauvegarde locale et reprise sur incident

Couche dédiée, entièrement découplée du cœur : le `model`, le `core` et
l'`ordonnancement` ne connaissent pas la persistance (règle d'or). Format **JSON
versionné**, **zéro dépendance externe**, pensé pour tourner en salle **sans
réseau** et pour survivre à une coupure de courant.

- **Socle JSON** (`persistence.json`) : `Json` (arbre en mémoire), `JsonWriter`
  (écriture compacte ou indentée), `JsonParser` (analyseur récursif), `JsonException`.
  Fait maison pour maîtriser le versionnage du format et éviter toute bibliothèque.
- **Document (sauvegarde volontaire).** `DepotTournoi` sauvegarde et recharge un
  `Tournoi` complet + son `RegistreDisciplinaire`. **Écriture atomique** (fichier
  temporaire → flush → renommage) : une coupure en pleine écriture ne peut pas
  corrompre la sauvegarde précédente. Mappers : `TournoiMapper` (assemblage),
  `ConfigMapper` (objets immuables : config tableau, tarifs, récompenses, adresse,
  règlement IX.111, JA…), `DerouleMapper` (poules, matchs, tableau à élimination).
- **Trois difficultés du modèle, résolues** via `ContexteSerialisation` :
  **déduplication des joueurs** (annuaire unique en tête, référence par licence,
  une seule instance par licence au rechargement) ; **graphe des matchs** de la
  phase finale (`sourceA`/`sourceB` écrits par identifiant stable, reliés en
  seconde passe) ; **restauration d'un état arbitraire** sans buter sur les gardes
  du cycle de vie, via `Tableau.restaurerEtat(EtatRestaure)` (méthode portée par
  le modèle, sans dépendance vers la persistance).
- **Session + reprise sur incident.** `SessionSalle` agrège tournoi + discipline +
  `Ordonnanceur` + `EstimateurDuree`. `SessionMapperComplet` / `DepotSession` la
  sérialisent en partageant le même annuaire et les mêmes identifiants de matchs,
  de sorte que les matchs sous l'ordonnanceur soient **les mêmes instances** que
  ceux du tournoi. `ServiceAutosave` écrit périodiquement (**15 min** par défaut)
  dans un fichier d'autosave **distinct** du document ; un **marqueur de fermeture
  propre** permet, au lancement suivant, de **détecter un arrêt anormal** et de
  proposer la reprise (`detecterReprise` / `ignorerReprise`). L'occupation des
  tables est reconstruite à partir des matchs en cours ; l'apprentissage des durées
  de l'`EstimateurDuree` est restauré tel quel.
- **Retouches minimales du cœur, pour la restauration seulement** (aucune
  dépendance ajoutée) : `Tableau.restaurerEtat`, accesseurs de lecture +
  `restaurerApprentissage` sur `EstimateurDuree`, `Ordonnanceur.getPoids` +
  `restaurerOccupation`.
- **Données d'application (entre les tournois).** `DepotApplication` persiste le
  référentiel des **clubs**, les **comptes organisateurs**, les **accès**
  (habilitations) et la liste des **tournois récents** dans un fichier applicatif
  JSON distinct du document de tournoi. Il **implémente le port `RegistreClubs`**
  (donc se branche directement sur `ServiceComptesOrganisateurs`), fournit le
  prédicat d'unicité d'email attendu à l'inscription, et enregistre les
  comptes/accès produits par le service pour qu'ils survivent entre deux sessions.
  Mots de passe stockés **uniquement sous forme d'empreinte**. Mapper :
  `ApplicationMapper`.
- **Pilotage « fichier » (cycle de vie applicatif).** `ServiceFichierTournoi`
  relie toutes les briques en un cycle cohérent : démarrage (chargement du dépôt
  applicatif depuis un dossier de données par défaut — `XDG_DATA_HOME` ou
  `~/.pingmanager`), **nouveau/ouvrir** avec **détection de reprise sur incident**,
  **autosave** branché sur la session courante (et qui suit le fichier lors d'un
  « enregistrer-sous »), **enregistrer/enregistrer-sous**, **récents** tenus à
  jour, et **fermeture propre**. Le service ne décide jamais seul d'une reprise :
  il la signale via un **écouteur d'événements** (prêt pour l'interface) et laisse
  l'appelant trancher, avec un **défaut sûr** (ignorer) si rien n'est décidé.
  `ProjetTournoi` décrit une entrée de récents.

---

## 6. Conformité réglementaire

Le détail article par article est dans **`CONFORMITE_FFTT.md`**. Résumé :

| Article | Sujet | État |
|---------|-------|------|
| I.301 | Ordre des parties en poule (3/4/5/6, selon qualifiés) | ✅ |
| I.302 | Placement en poule, départage aléatoire, séparation clubs | ✅ |
| I.303 | Classement 2/1/0, 0 pour forfait/abandon/disqualif, départages | ✅ |
| I.304 | Placement tableau, têtes de série tirées au sort, séparation | ✅ |
| I.305 | Placement des qualifiés (2es demi opposé, 3es quart opposé) | ✅ |
| IX.104/IX.111 | Inscription, plage de classement, capacité, tarifs, récompenses | ✅ |
| IX.101/IX.102 | Accès selon la catégorie du tournoi (département, région, licence loisir) | ✅ |
| II.302/II.303 | Catégories d'âge (âge au 1er janvier de la 1re année de la saison) | ✅ |
| 3.5.2 | Cartons, points de pénalité, 2 disqualifs = exclusion | ✅ |

**Réserves connues** (documentées, sans impact sur le déroulé normal) :
- I.303.e : tirage au sort en cas d'**égalité vraiment persistante** laissé à
  l'organisateur.
- Séparation des clubs au-delà du 2e tour non contrainte (non exigé par le
  règlement).
- Validation « grandeur nature » par un juge-arbitre sur un vrai tournoi : pas
  encore faite (tout est validé par simulation et tests).

---

## 7. État des tests

- **289 tests unitaires, 0 échec** au dernier build (263 du cœur + 26 de
  persistance).
- Le déroulé sportif (poules, classement, tableau, discipline, ordonnancement),
  le **pré-tournoi complet** (comptes, habilitation, cycle de vie, complétude
  H/P, câblage) et la **persistance** (round-trip document, reprise sur incident,
  socle JSON) sont couverts.
- Fichiers principaux : `GenerateurPoulesTest`, `GenerateurMatchsPouleTest`,
  `TableauInscriptionTest`, `ClassementPouleServiceTest`,
  `GenerateurTableauEliminationTest`, `PlacementQualifiesServiceTest`,
  `MoteurTournoiTest`, `BestOfEtPetiteFinaleTest`, `ConformiteTirageTest`,
  `DisciplineEtForfaitTest`, `OrdonnanceurTest`, `PrevisionEtAnticipationTest`,
  `ConfigurationTableauTest`, `GrilleRecompensesTest`, `ServiceInscriptionTest`,
  `ListeAttenteBorneeTest`, `ServicePointageTest`, `ScenarioInscriptionPointageTest`,
  `ServiceLancementTableauTest`, `BoucleCompleteTableauTest`.
- Fichiers du pré-tournoi : `CompteEtClubTest`, `ServiceComptesOrganisateursTest`,
  `ParametresPreTournoiTest`, `ServiceCycleTournoiTest`, `ChampsHomologationTest`,
  `CompletudeTroisNiveauxTest`, `PublicationEtDotationTest`,
  `CablagePreTournoiTest`, `CoutureStatutDerouleTest`.
- Fichiers de persistance : `RoundTripTournoiTest` (sauvegarde → rechargement
  fidèle, déduplication des joueurs, graphe de matchs, idempotence),
  `RepriseSessionTest` (autosave, détection d'arrêt anormal, occupation des tables,
  apprentissage des durées, fermeture propre), `JsonSocleTest` (aller-retour,
  échappements Unicode, robustesse du parseur), `DepotApplicationTest` (clubs,
  comptes et accès persistés ; parcours inscription → confirmation → rechargement),
  `ServiceFichierTournoiTest` (cycle de vie applicatif : démarrage, création,
  enregistrer-sous, récents, ouverture, protocole de reprise, bascule de l'autosave).
- Un **tournoi complet** (effectif non rond, poules → tableau → finale) a été
  simulé de bout en bout, ainsi que des scénarios mixtes (forfait + cartons +
  disqualification) et le **parcours pré-tournoi entier** (création habilitée →
  homologation → publication → inscription). Ce tournoi complet est aussi
  **sauvegardé puis rechargé** et comparé à l'original (round-trip fidèle,
  y compris le graphe de matchs de la phase finale).

### Compiler et tester
```
mvn test              # chez l'utilisateur (Maven Central accessible)
# ou compilation directe :
javac -d target/classes $(find src/main/java -name "*.java")
```
> Note : pendant le développement en environnement isolé, JUnit a été simulé par
> un petit stub + un lanceur par réflexion (Maven Central bloqué), qui exécutent
> l'intégralité des 289 tests. Chez l'utilisateur, `mvn test` fonctionne
> normalement — ni le stub ni le lanceur ne sont dans le livrable.

---

## 8. Ce qui est FAIT

Le **cœur du déroulé sportif** est complet et testé :
- formation des poules, formats, ordre des matchs (conformes I.301/I.302) ;
- classement et départages (I.303) ;
- construction et placement du tableau (I.304/I.305) ;
- discipline complète (forfaits, abandons, cartons, disqualifications, 3.5.2) ;
- ordonnancement des tables (réactif + semi-auto) ;
- estimation des durées, prévision, anticipation court terme.

Le **pré-tournoi** est désormais complet et intégré de bout en bout :
- **Comptes & clubs** : `Club` (référentiel FFTT), `CompteOrganisateur`,
  `AccesClub` ; `ServiceComptesOrganisateurs` orchestre inscription → code envoyé
  à l'**email officiel du club** → vérification → création de l'accès. Effets de
  bord isolés derrière des **ports** (`EnvoiEmail`, `GenerateurCode`,
  `HachageMotDePasse`, `Horloge`) ; référentiel `RegistreClubs` (impl mémoire).
- **Cycle de vie** : `StatutTournoi` (BROUILLON → HOMOLOGUE → PUBLIE, + ANNULE),
  transitions gardées ; `ServiceCycleTournoi` avec **complétude à trois niveaux**
  (voir §6) et calcul automatique de la **dotation globale** à la publication.
- **Métadonnées** : `InfosReglement` (IX.111, immuable + builder), champs de la
  demande d'homologation SPID (`TypeTournoi`, dates début/fin, `AdresseTournoi`,
  correspondant, `DocumentReference` pour les PDF), juges-arbitres en saisie libre
  (`AffectationJugeArbitre` + `GradeJugeArbitre`), `NiveauTournoi`,
  `PolitiqueBalles`, `AireDeJeu`.
- **Habilitation** : `ServiceTournois` — création et transitions réservées à un
  organisateur habilité pour le club (contrôle d'`AccesClub`, double filet).
- **Câblage complet** : le statut du tournoi conditionne le déroulé via une
  `PolitiqueOuverture` partagée (inscription, **pointage** et **lancement**
  refusent un tournoi annulé ; mode strict « exiger publié » disponible). Les deux
  horloges (statut tournoi / statut tableaux) ne peuvent plus diverger.

La **persistance** est désormais en place (voir §5.6) :
- **sauvegarde/rechargement** d'un tournoi complet + sa discipline, en JSON
  versionné, sans dépendance, avec **écriture atomique** ;
- **autosave 15 min + reprise sur incident** embarquant l'état vivant de
  l'ordonnancement et l'apprentissage des durées ;
- déduplication des joueurs et **graphe des matchs** de la phase finale préservés ;
- **données d'application persistées** (clubs, comptes organisateurs, accès,
  tournois récents) dans un fichier applicatif distinct — `DepotApplication` se
  branche comme `RegistreClubs` et conserve les habilitations entre les tournois ;
- **service de pilotage « fichier »** (`ServiceFichierTournoi`) qui orchestre tout
  le cycle de vie applicatif — démarrage, nouveau/ouvrir avec reprise sur incident
  signalée par événements, autosave branché sur la session, enregistrer/
  enregistrer-sous, récents, fermeture propre — et se pilote via un écouteur, prêt
  pour l'interface.

> Il reste à construire l'**interface** (JavaFX) par-dessus ce moteur désormais
> persistant, et le **générateur de règlement PDF**.

---

## 9. Feuille de route (prochains chats)

Découpage par domaine, chaque bloc fini à fond avant le suivant :

1. **Inscription / règles de tableau / liste d'attente / lancement de tableau**
   — **TERMINÉ**.
2. **Pré-tournoi / homologation / règlement** (axe 1) — **TERMINÉ**. Comptes &
   clubs, habilitation, cycle de vie BROUILLON → HOMOLOGUE → PUBLIE, métadonnées
   IX.111 + champs SPID, complétude à trois niveaux (homologation / publication /
   facultatif), dotation calculée, câblage complet du statut au déroulé.
3. **Persistance** (sauvegarde/rechargement) — **TERMINÉ**. Couche `persistence`
   dédiée, JSON versionné sans dépendance, derrière la règle d'or. Document
   (tournoi + discipline) avec écriture atomique ; **autosave 15 min + reprise sur
   incident** embarquant l'ordonnancement et l'apprentissage des durées ;
   déduplication des joueurs et graphe des matchs préservés. Prochain gros bloc
   naturel : l'**interface**.
4. **Générateur de règlement PDF** — produire un règlement officiel type à partir
   des métadonnées (le `DocumentReference` est déjà prévu pour accueillir soit ce
   PDF généré, soit un PDF fourni par le club).
5. **Interface (JavaFX)** — sur un moteur métier stable.
6. **Remise des récompenses** — acte de fin (podium à afficher/exporter).
7. Plus tard : remontée fédérale SPID/GIRPE (axe 3), buvette et saisie mobile
   (axe 4), version web.

---

## 10. Emplacements

- Livrable : `pingmanager/` (+ `pingmanager.zip`).
- Documents à la racine : `README.md` (présentation + composants),
  `CONFORMITE_FFTT.md` (conformité détaillée), `ETAT_DU_PROJET.md` (ce document).
- Règlements de référence (fournis par l'utilisateur) : règlement sportif et
  règlement général FFTT 2026-2027.

---

*Dernière mise à jour : chat « persistance » — couche `persistence` dédiée, JSON
versionné sans dépendance (fonctionne hors ligne, écriture atomique). Document
tournoi + discipline sauvegardé/rechargé à l'identique (déduplication des joueurs,
graphe des matchs de la phase finale, idempotence vérifiée). Autosave toutes les
15 min dans un fichier séparé, avec reprise sur incident détectée au démarrage
(marqueur de fermeture propre), embarquant l'état vivant de l'ordonnancement et
l'apprentissage des durées. Retouches minimales du cœur pour la seule restauration
(sans dépendance ajoutée). Puis **persistance des données d'application** :
`DepotApplication` stocke clubs, comptes organisateurs et accès dans un fichier
applicatif distinct, se branche comme `RegistreClubs` et fournit le contrôle
d'unicité d'email — le parcours inscription → confirmation → rechargement conserve
l'email vérifié et l'habilitation. Enfin, **service de pilotage « fichier »**
(`ServiceFichierTournoi`) reliant toutes les briques : démarrage, nouveau/ouvrir
avec reprise sur incident signalée par événements (défaut sûr si l'appelant ne
tranche pas), autosave branché sur la session et suivant le document lors d'un
« enregistrer-sous », récents persistés, fermeture propre — le tout pilotable par
un écouteur, prêt pour l'UI. 289 tests au vert (263 + 26). La persistance est
close ; prochain bloc naturel : l'interface (JavaFX) ou le générateur de règlement
PDF.*
