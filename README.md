# PingManager

Logiciel de bureau (Java) pour **gérer des tournois individuels officiels de
tennis de table FFTT**. Le cœur métier est totalement découplé de l'interface et
de la base de données, en vue d'une revente et d'un futur passage au web.

Tournois visés : épreuves **individuelles** homologuées (type Maisons-Alfort,
Sucy, Lille). Ni le Critérium, ni les épreuves par équipes.

---

## Stack et architecture

- **Java 21 + Maven.** Interface **JavaFX** prévue pour la v1 (bureau), web plus tard.
- **Architecture MVC à cœur découplé.** Les packages `model`, `model.format`,
  `core` et `ordonnancement` ne connaissent **ni l'interface ni la base de
  données**. C'est la règle d'or du projet : elle garantit la revente et un futur
  portage web. Les dépendances autorisées vont de `core` vers `model` et
  `ordonnancement` ; jamais l'inverse, jamais vers une UI ou une couche de
  persistance.

```
com.pingmanager
├── model              entités pures (joueur, match, poule, tableau, tournoi,
│   │                  club, compte organisateur, règlement, juge-arbitre…)
│   └── format         définition des formats de tournoi
├── core               services métier (génération, classement, placement,
│                      inscription, pointage, lancement, comptes, cycle de vie
│                      du tournoi, habilitation…)
├── ordonnancement     tables, priorités, prévision, anticipation
└── persistence        sauvegarde/rechargement local (JSON sans dépendance),
    └── json           autosave + reprise sur incident — dépend de model/core/
                       ordonnancement, jamais l'inverse
```

---

## Compiler et tester

```
mvn test
```

Le projet ne dépend, pour ses tests, que de JUnit 5 (récupéré par Maven).
Aucune dépendance de production hors la bibliothèque standard — **y compris la
persistance**, qui lit et écrit son propre JSON.

**289 tests unitaires, 0 échec** au dernier build.

---

## Ce que fait le logiciel aujourd'hui

Le **déroulé sportif complet d'un tableau** est implémenté et testé, des
inscriptions jusqu'à la finale :

```
INSCRIPTION → POINTAGE → LANCE (poules) → PHASE_FINALE (tableau à élimination → finale)
```

Chaque étape est conforme aux règlements officiels FFTT 2026-2027 (voir
`CONFORMITE_FFTT.md` pour le détail article par article).

### Le modèle (`model`) — entités pures

- `Joueur` — identité fondée sur le **numéro de licence** ; classement en points ;
  rang national optionnel (numérotation française, pour l'exclusion des mieux
  numérotés) ; sentinelle « à déterminer » pour les emplacements de tableau non
  encore connus.
- `Match` — best-of propre à chaque match (finale réglable), numéro de table,
  liens de progression (`sourceA`/`sourceB`), issue (normale, forfait, abandon,
  refus, disqualification), points de pénalité, horodatage lancement/fin.
- `Manche`, `Poule`, `Tour`, `TableauElimination`, `LigneClassement`, `Qualifie` —
  briques du déroulé et du classement.
- `Tableau` + `ConfigurationTableau` — une épreuve et tous ses paramètres :
  format, taille de poule, qualifiés par poule, best-of, **limite de places**,
  **liste d'attente bornée** (taille réglable par tableau), **tarifs** en ligne /
  sur place, **grille de récompenses** (nombre de places libre, par place ou par
  plage), **mode de limite de classement** (tranche de points, plafond, ou toutes
  séries), **exclusion par rang national**, **réservation par sexe** et **par
  catégorie d'âge**. Le tableau suit un **cycle de vie** (`StatutTableau` :
  inscription → pointage → lancé → phase finale), tient ses inscrits, sa liste
  d'attente, l'état pointé/payé de chacun (par tableau) et sa phase finale.
- `Tournoi` — nom, club organisateur, **un ou plusieurs jours**, **niveau**
  (départemental → international), **type** (SPID), dates début/fin, adresse
  structurée, correspondant, numéro d'homologation, nombre de tables, la **règle
  de tableaux par jour** (quota + exception féminine), les **juges-arbitres**
  (saisie libre + grade), les **métadonnées de règlement** (`InfosReglement`,
  IX.111), les **documents** (règlement / feuille d'engagement PDF), un **statut
  de cycle de vie** (`StatutTournoi` : brouillon → homologué → publié, + annulé)
  et la liste des tableaux.
- `Carton`, `CouleurCarton`, `IssueMatch`, `Sexe`, `StatutMatch`,
  `FormatPhaseFinale`, `ModeLimiteClassement`, `CategorieAge`, `TypeRecompense`,
  `Recompense`, `GrilleRecompenses`, `TarifInscription`, `RegleTableauxParJour`,
  `StatutTableau`, `HorairesTableau` — types de support.

### Les formats (`model.format`)

- `FormatTournoi` (interface), `FormatPoule3Qualifie2` (format par défaut,
  identifiant `P3Q2` : poule de 3, 2 qualifiés), `FormatConfigurable` (builder :
  best-of finale, petite finale…), `SortNonQualifies`, `RegistreFormats`
  (catalogue + format par défaut).
- **Ajouter un format** consiste à créer une classe implémentant `FormatTournoi`
  et à l'enregistrer dans `RegistreFormats`. Le moteur n'a pas à changer.

### Les services métier (`core`)

- `GenerateurPoules` — répartition en serpentin, séparation des clubs, départage
  aléatoire reproductible des ex æquo (I.302).
- `GenerateurMatchsPoule` — ordres officiels des parties I.301 (poules 3/4/5/6
  selon le nombre de qualifiés) et règle anti-arrangement.
- `ClassementPouleService` — classement I.303 (2/1/0 points, 0 pour issue non
  normale, départages par mini-classement des ex æquo).
- `GenerateurTableauElimination` — cadre du tableau, exemptions (byes), seeding.
- `PlacementQualifiesService` — placement I.304/I.305 (têtes de série tirées au
  sort par blocs 3-4/5-8/9-16, 2es en demi opposé, 3es en quart opposé, séparation
  des clubs au 1er et 2e tour si le tableau est incomplet).
- `ProgressionTableau` — propage les vainqueurs, gère la petite finale.
- `MoteurTournoi` — orchestre une épreuve selon un format (poules → qualifiés →
  tableau), applique les best-of.
- `RegistreDisciplinaire` — historique des cartons ; règle 3.5.2.9 (deux
  disqualifications entraînent l'exclusion de la compétition).
- `GestionCartonsMatch` — progression des cartons (jaune, puis pénalités),
  cartons hors match.
- `ServiceInscription` — inscription d'un joueur à un tableau, au niveau du
  tournoi. Applique dans l'ordre l'éligibilité (classement selon le mode, rang
  national, sexe), le quota de tableaux par jour (avec exception féminine
  configurable), la clause « pas deux tableaux qui se suivent », puis délègue à
  `Tableau.inscrire` (capacité + liste d'attente bornée). Renvoie un résultat au
  motif précis (`ResultatInscriptionTournoi`).
- `ServicePointage` — pointage et repêchage le jour J. La présence et le paiement
  sont suivis **par tableau** (deux états indépendants). Clôturer le pointage
  libère les inscrits non pointés et repêche la liste d'attente ; tout joueur
  repêché doit repasser par le pointage (c'est là qu'il règle son engagement). Le
  juge-arbitre confirme ou retire à la main. Gère aussi le désistement (avant
  clôture il libère la place ; après clôture il est refusé, sauf cas particulier
  du juge-arbitre avec remboursement tracé).
- `ServiceLancementTableau` — la charnière entre l'inscription et le déroulé
  sportif. Une fois le pointage clôturé, `lancer` prend les présents et génère les
  poules via `MoteurTournoi` (tirage reproductible, graine conservée). Une fois
  toutes les poules terminées, `construirePhaseFinale` monte le tableau à
  élimination directe (placement des qualifiés I.305, best-of, petite finale) et
  fait passer le tableau en phase finale.
- `ResultatInscriptionTournoi`, `ResultatPointage`, `ResultatLancement` — comptes
  rendus détaillés renvoyés par les services correspondants.

### Le pré-tournoi (`core` + `model`)

- **Comptes & clubs** : `Club` (référentiel FFTT), `CompteOrganisateur`,
  `AccesClub`. `ServiceComptesOrganisateurs` orchestre inscription → code envoyé à
  l'**email officiel du club** → vérification → création de l'accès. Les effets de
  bord passent par des **ports** (`EnvoiEmail`, `GenerateurCode`,
  `HachageMotDePasse`, `Horloge`) : aucune implémentation de production dans le
  livrable, le cœur reste pur. `RegistreClubs` (+ impl mémoire) fournit le
  référentiel.
- **Cycle de vie** : `ServiceCycleTournoi` fait passer un tournoi de
  **brouillon → homologué → publié** (ou annulé). L'homologation vérifie la
  **grille H** (dossier de demande SPID : correspondant, juge-arbitre, type,
  dates, adresse, tableaux avec nom/type/date, au moins un tableau dames — IX.104)
  et enregistre le numéro délivré par la FFTT. La publication vérifie la **grille
  P** (notre décision produit : niveau, tables, règlement PDF, politique balles,
  clôtures, et chaque tableau complet) et **calcule la dotation globale**. Les
  éléments manquants sont consultables à tout moment, en clair.
- **Habilitation** : `ServiceTournois` réserve la création et les transitions à un
  organisateur habilité pour le club (contrôle d'`AccesClub`).
- **Couplage** : une `PolitiqueOuverture` partagée fait que l'inscription, le
  pointage et le lancement respectent le statut du tournoi (refus si annulé ; mode
  strict « exiger publié »).

### L'ordonnancement (`ordonnancement`)

- `Ordonnanceur` — moteur réactif semi-automatique d'affectation des matchs aux
  tables. Contrainte dure (un joueur ne peut pas être à deux tables). Score de
  priorité réglable : vagues (poules avant tableau), équité (attente), déblocage
  (joueur engagé sur plusieurs tableaux).
- `ValidationInscription` — règle configurable « pas deux tableaux qui se
  suivent » (clause d'organisateur, pas une règle fédérale).
- `EstimateurDuree` — durée d'un match par catégorie, avec apprentissage (moyenne
  mobile robuste).
- `PrevisionSalle`, `Anticipateur` — prévision de disponibilité des tables et des
  joueurs, suggestions de préparation des matchs bientôt jouables.
- Types de support : `MatchOrdonnancable`, `PhaseMatch`, `PoidsOrdonnancement`,
  `PropositionLancement`, `SuggestionAnticipation`, `CategorieDuree`.

### La persistance (`persistence`)

Conçue pour la salle : **local, hors ligne, résistant aux coupures**. Format JSON
versionné, **sans aucune dépendance** (socle `persistence.json` fait maison).

- `DepotTournoi` — sauvegarde/rechargement d'un tournoi complet + sa discipline,
  en **écriture atomique** (une coupure de courant ne corrompt jamais le fichier
  précédent). Le fichier est lisible et inspectable à la main en cas de pépin.
- `ServiceAutosave` — sauvegarde automatique toutes les **15 minutes** dans un
  fichier séparé, avec **reprise sur incident** : au lancement suivant, un arrêt
  anormal est détecté (marqueur de fermeture propre) et la reprise peut être
  proposée. L'autosave embarque l'état vivant de l'ordonnancement et
  l'apprentissage des durées, en plus du tournoi.
- Points délicats gérés : **déduplication des joueurs** (une instance par licence),
  **graphe des matchs** de la phase finale rétabli au rechargement, et **mêmes
  instances de matchs** partagées entre le tournoi et l'ordonnanceur.
- `DepotApplication` — persistance des **données d'application** (référentiel des
  clubs, comptes organisateurs, accès, tournois récents), dans un fichier distinct
  du document de tournoi. Il **implémente `RegistreClubs`** et se branche donc
  directement sur le service de comptes ; les habilitations survivent d'un tournoi
  à l'autre. Les mots de passe n'y figurent que sous forme d'**empreinte**.
- `ServiceFichierTournoi` — le **cycle de vie applicatif** : démarrage, nouveau/
  ouvrir (avec **reprise sur incident** signalée par événements), autosave branché
  sur la session, enregistrer/enregistrer-sous, **tournois récents**, fermeture
  propre. Pilotable par un écouteur, prêt à être branché sur une interface.
- Le cœur reste découplé : quelques accesseurs de restauration ont été ajoutés au
  `model`/`ordonnancement`, mais aucune dépendance vers `persistence`.

---

## Contrainte d'intégration fédérale (à traiter au pré-tournoi)

L'homologation et la remontée officielle des résultats passent obligatoirement
par les outils fédéraux **SPID V2** (portail) et **GIRPE** (logiciel de salle),
verrouillés derrière authentification FFTT. PingManager ne les remplace pas : il
apporte le confort de gestion en salle et produira un **export**. Le format
d'échange GIRPE n'est pas public et n'est communiqué que **lors de
l'homologation** (contact : spidv2@fftt.org).

---

## État d'avancement

- **Fait** : le déroulé sportif complet d'un tableau (inscription, pointage,
  liste d'attente, lancement, poules, phase finale, discipline, ordonnancement,
  prévision), conforme aux règlements ; le **pré-tournoi complet** (comptes &
  clubs, habilitation, cycle de vie brouillon → homologué → publié, métadonnées
  IX.111 + champs de la demande SPID, complétude à trois niveaux, dotation
  calculée, câblage du statut au déroulé) ; et la **persistance** (sauvegarde/
  rechargement local en JSON sans dépendance, écriture atomique, autosave 15 min
  et reprise sur incident embarquant l'ordonnancement et l'apprentissage des
  durées). Les **données d'application** (clubs, comptes organisateurs, accès)
  sont elles aussi persistées, dans un fichier distinct. Un **service de pilotage
  « fichier »** (`ServiceFichierTournoi`) orchestre tout le cycle de vie
  applicatif (démarrage, ouvrir/nouveau, autosave, enregistrer, récents, fermeture)
  et se pilote par événements, prêt pour l'interface.
- **Prochain bloc** : le générateur de règlement PDF et l'**interface JavaFX**
  par-dessus le moteur désormais persistant, puis la remontée fédérale
  SPID/GIRPE.

Le détail de l'avancement et la feuille de route sont dans `ETAT_DU_PROJET.md`.
La conformité réglementaire article par article est dans `CONFORMITE_FFTT.md`.
