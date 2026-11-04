# Rapport d'architecture — PingManager

- **Date** : 4 novembre 2026
- **Auteur** : agent gardien de l'architecture (lecture seule, aucun code modifié)
- **Révision auditée** : `d91a4fd` (branche courante, arbre de travail propre)

## Périmètre

- Code : `src/main/java/com/pingmanager/**` (188 fichiers compilés) et
  `src/test/java/com/pingmanager/**` (59 fichiers), ressources `src/main/resources`.
- Configuration : `pom.xml`, `.gitignore`, `.claude/`, `outils-dev/`.
- Documentation : `README.md`, `ETAT_DU_PROJET.md`, `CLAUDE.md`, `QUESTIONS.md`,
  `CONFORMITE_FFTT.md`, `docs/passation/*.md`.
- Vérifications : règle d'or (imports interdits), sens des dépendances, ports et
  effets de bord, dépendances Maven, exception `RenduPdfReglement`, `switch` sur
  énumération, rétrocompatibilité JSON, styles en dur dans l'interface, cohérence
  documentation / code, fichiers générés ou réglages d'IDE committés.

## Résultat de l'exécution des tests

| Élément | Valeur |
|---|---|
| Commande exécutée | `mvn -B test` (à la racine) |
| Résultat | **BUILD SUCCESS** (code de sortie 0, 25,6 s) |
| Tests exécutés | **412** |
| Échecs | **0** |
| Erreurs | **0** |
| Ignorés | **0** |
| Compilation | 188 sources principales + 59 sources de test, `release 21` |

Maven Central était accessible : le repli `outils-dev/tests.sh` n'a pas été
nécessaire. Le décompte `grep -r "@Test" src/test | wc -l` donne aussi **412**,
aucun `@Disabled`, aucun test paramétré : chiffre cohérent avec Surefire.

Répartition des `@Test` : model 83, core 209, core.reglement 25,
ordonnancement 17, persistence 42, persistence.json 5, importation 11, ui 3,
ui.ecran 7, ui.infra 10 (model.format : 0).

## Constats, classés par gravité

### Gravité haute

**H1. Six `switch` sur une énumération (interdit par `CLAUDE.md`)**

`CLAUDE.md` interdit tout `switch` sur une énumération : javac génère une classe
annexe `X$1` qui a déjà cassé le build chez Quentin. Le build du jour a bien
produit `ConfigurationTableau$1.class`, `Ordonnanceur$1.class`,
`JsonWriter$1.class` et `TournoiMapper$1.class`.

| Fichier:ligne | Énumération testée | `X$1` généré |
|---|---|---|
| `src/main/java/com/pingmanager/model/ConfigurationTableau.java:214` | `ModeLimiteClassement` | oui |
| `src/main/java/com/pingmanager/model/RegleTableauxParJour.java:119` | `ExceptionFeminine` (imbriquée) | non (énumération imbriquée), mais la règle s'applique quand même |
| `src/main/java/com/pingmanager/ordonnancement/Ordonnanceur.java:208` | `PhaseMatch` | oui |
| `src/main/java/com/pingmanager/ordonnancement/Ordonnanceur.java:256` | `PhaseMatch` | oui (même `$1`) |
| `src/main/java/com/pingmanager/persistence/json/JsonWriter.java:30` | `Json.Type` | oui |
| `src/main/java/com/pingmanager/persistence/TournoiMapper.java:622` | `RegleTableauxParJour.ExceptionFeminine` | oui |

Tous datent du premier commit (`a1080e2`). Les `switch` de
`ui/ecran/FormulaireTableau.java:195, 465, 486` portent sur des `String` : ils
sont conformes.

*Correction proposée* : remplacer chacun par une chaîne de `if (x == Enum.A)`
(ou un `switch (x.name())`), à comportement identique, puis vérifier après
`mvn -B test` que `find target/classes -name '*$1.class'` ne renvoie plus ces
classes. On peut ajouter un garde-fou dans `outils-dev/` (grep des `switch` dont
le sélecteur est une énumération) ou un test qui vérifie l'absence de
`ConfigurationTableau$1`, etc.

### Gravité moyenne

**M1. `importation` dépend de `persistence.json`**

- `src/main/java/com/pingmanager/importation/LecteurTableur.java:3` : `import com.pingmanager.persistence.json.Json;`
- `src/main/java/com/pingmanager/importation/LecteurTableur.java:4` : `import com.pingmanager.persistence.json.JsonParser;`

Le graphe documenté (`CLAUDE.md`, skill `architecture-pingmanager`) place
`persistence` et `importation` au même niveau, tous deux au-dessus de `core`.
Cette arête latérale n'y figure pas, et la doc présente `importation` comme
« sans dépendance » (`CLAUDE.md:46`,
`docs/passation/PASSATION_IMPORT_ET_J-1.md:27`). Ce n'est pas une violation de la
règle d'or, puisque le cœur n'est pas touché. En revanche, c'est un couplage non
déclaré : si la persistance change (passage au web, base de données), l'import
JSON des joueurs casse.

*Correction proposée* : soit sortir le socle JSON dans un package neutre (par
exemple `com.pingmanager.json`) utilisé par `persistence` et `importation`, sans
changer le format des fichiers ; soit acter l'arête dans `CLAUDE.md` et dans la
skill. C'est une décision d'architecture : à noter dans `QUESTIONS.md`.

**M2. Effets de bord temporels dans le cœur, hors du port `Horloge`**

| Fichier:ligne | Appel | Gravité |
|---|---|---|
| `src/main/java/com/pingmanager/core/GestionCartonsMatch.java:74` | `LocalDateTime.now()` (horodatage d'un carton) | moyenne |
| `src/main/java/com/pingmanager/core/GestionCartonsMatch.java:96` | `LocalDateTime.now()` (carton de disqualification) | moyenne |
| `src/main/java/com/pingmanager/core/ImportJoueurs.java:525` | repli `LocalDate.now()` pour le calcul de la catégorie d'âge | moyenne |
| `src/main/java/com/pingmanager/core/JoueurDepuisLicence.java:33` | repli `LocalDate.now()` pour le calcul de la catégorie d'âge | moyenne |
| `src/main/java/com/pingmanager/core/ServiceLancementTableau.java:130` | graine de tirage `System.nanoTime()` (surcharge sans graine) | faible : la graine est reportée dans `ResultatLancement`, donc le tirage reste rejouable |

Le port `Horloge` existe (`core/Horloge.java:14`) et sert déjà à
`ServiceComptesOrganisateurs`, mais pas ici. Conséquence : l'horodatage des
cartons ne peut pas être testé de façon déterministe. Autre conséquence : la
catégorie d'âge d'un joueur importé dépend du jour de l'import quand le tournoi
n'a pas de date, et la saison sportive peut alors être fausse (II.302 / II.303).

*Correction proposée* : injecter `Horloge` dans `GestionCartonsMatch`, en gardant
un constructeur par défaut sur `Horloge.systeme()`. Pour la catégorie d'âge,
exiger une date de référence (date du tournoi ou saison) ou la passer par le même
port, puis ajouter les tests correspondants.

**M3. Documentation en retard sur le code (README, ETAT_DU_PROJET, CONFORMITE)**

Les chiffres de tête sont justes : `README.md:50` et `ETAT_DU_PROJET.md:26`
annoncent 412 tests, comme la réalité. Les sections suivantes sont en revanche
périmées et peuvent induire un agent en erreur :

| Fichier:ligne | Écart | Correction proposée |
|---|---|---|
| `ETAT_DU_PROJET.md:84-92` (section 5) | Arborescence réduite à `model`, `core`, `ordonnancement` ; `importation`, `persistence`, `ui` absents | Reprendre l'arborescence de `CLAUDE.md` |
| `ETAT_DU_PROJET.md:318-319` (section 7) | « 289 tests unitaires (263 + 26) » | 412, avec répartition par package |
| `ETAT_DU_PROJET.md:357-360` | Note sur le stub JUnit et les « 289 tests » | Renvoyer à `outils-dev/tests.sh` |
| `ETAT_DU_PROJET.md:428-434` (section 9) | Générateur de règlement PDF et interface JavaFX présentés comme à venir | Les marquer TERMINÉS, ajouter la phase J-1 |
| `ETAT_DU_PROJET.md:443` (section 10) | « Livrable : `pingmanager/` (+ `pingmanager.zip`) » | Livraison par Pull Request (cf. section 0) |
| `ETAT_DU_PROJET.md:450-469` | Pied « Dernière mise à jour : chat persistance… 289 tests » | Dater au passage Claude Code |
| `README.md:14` | Interface JavaFX « prévue pour la v1 » | Elle existe déjà |
| `README.md:22-33` | Arborescence sans `importation` ni `ui` ; le texte (`README.md:18-20`) ne décrit pas `ui → persistence/importation → core` | Aligner sur `CLAUDE.md` |
| `README.md:247-249` | « Prochain bloc : générateur de règlement PDF et interface JavaFX » | Phase suivante : J-1, tournoi prêt |
| `CONFORMITE_FFTT.md:138` | « 276 tests » | 412, ou supprimer le chiffre |
| `pom.xml:26-27` (commentaire) | Range `persistence` dans le « coeur » | Corriger le commentaire |

### Gravité faible

**F1. Entrées/sorties fichier dans l'exception PDFBox**
`src/main/java/com/pingmanager/core/reglement/RenduPdfReglement.java:56` et `:66` :
`Files.write(cible, …)`. L'exception tolérée est l'import de PDFBox, pas
l'écriture disque. Les appelants (`ui/ecran/EcranComplementaire.java:470, 475`,
`ui/ecran/FicheTournoi.java:381, 383`) pourraient appeler `rendre(...)` et écrire
eux-mêmes les octets.
*Correction* : retirer les deux `ecrire(...)` du cœur. Ce serait un premier pas
vers l'option B de `QUESTIONS.md` (sortir le rendu PDF du cœur).

**F2. Styles en dur dans l'interface (cas justifiés, mais à encadrer)**
- `src/main/java/com/pingmanager/ui/ecran/Theme.java:63` : `racine.setStyle("-fx-font-family: …")`.
  C'est le seul `setStyle(` du package `ui`. La police est choisie à l'exécution,
  ce qui est acceptable.
- `src/main/java/com/pingmanager/ui/ecran/CarteTournois.java:110-111` : couleurs
  hexadécimales transmises à Leaflet dans la WebView, qui ne lit pas le CSS
  JavaFX. Ce sont des doublons des jetons de `theme.css`, qui divergeront si la
  charte change.
*Correction* : regrouper ces valeurs dans des constantes nommées, avec un
commentaire qui renvoie aux jetons de `theme.css` (ou les relire depuis une
ressource partagée).

**F3. Rétrocompatibilité JSON couverte champ par champ, sans fichier figé**
`RoundTripTournoiTest` existe (4 tests : aller-retour complet, déduplication,
graphe de progression, idempotence). La relecture des anciens fichiers est
testée sur des fragments construits à la main
(`TableauxEtEcheancesTest.java:49, 74`, `SportifEtMaterielTest.java:29, 51`,
`ClubAdresseRoundTripTest.java:26`). Il n'y a en revanche aucun fichier
`.json` complet d'une version antérieure dans `src/test/resources`.
`TournoiMapper.VERSION`, `SessionMapperComplet.VERSION` et
`DepotApplication.VERSION` valent toujours 1, malgré de nombreux ajouts de champs.
*Correction* : figer un fichier tournoi réel (par exemple Brunoy démo v12) dans
`src/test/resources/…/persistence/` et ajouter un test qui le relit à chaque
build.

**F4. Emplacement et couverture des tests**
- `src/test/java/com/pingmanager/importation/ImportJoueursTest.java` teste
  surtout `core.ImportJoueurs` et `core.ImportTableaux`, qui n'ont aucun test dans
  `src/test/java/com/pingmanager/core/`. C'est contraire à la consigne « tests
  dans le même package que la classe testée ».
- `model.format` (5 classes) n'a aucun test dédié : sa couverture est seulement
  indirecte, via `MoteurTournoiTest`, `RoundTripTournoiTest`, etc.
*Correction* : ajouter `core/ImportJoueursTest` (analyse et validation pures,
sans lecture de fichier) et `model/format/RegistreFormatsTest`.

**F5. `.gitignore` incomplet**
Il couvre `target/`, `*.class`, `.idea/`, `*.iml`, `.vscode/`, `.DS_Store`. Il
manque `*.zip` (l'ancien livrable `pingmanager.zip` est cité dans
`ETAT_DU_PROJET.md:443`), `out/`, `.settings/`, `.project`, `.classpath` et
`*.log`. Aucun de ces fichiers n'est committé aujourd'hui : c'est une mesure
préventive.

## Points conformes

- **Règle d'or, imports** : aucun `import` (ni nom pleinement qualifié) de
  `javafx`, `com.pingmanager.ui`, `com.pingmanager.persistence` ou
  `com.pingmanager.importation` dans `model`, `model.format`, `core`,
  `core.reglement` ou `ordonnancement`. Seule mention : un commentaire Javadoc,
  `core/ImportJoueurs.java:32`, sans effet.
- **Bibliothèques externes dans le cœur** : seul PDFBox apparaît, et uniquement
  dans `core/reglement/RenduPdfReglement.java:8-14` (exception connue, non
  étendue). Côté tests, PDFBox ne sert que dans
  `GenerateurReglementTest` et `GenerateurFeuilleEngagementTest`, pour relire le
  PDF. `core/Territoires.java:83` lit une ressource du classpath
  (`departements.csv`) : c'est une donnée embarquée, pas un effet de bord.
- **JavaFX** : présent uniquement sous `com.pingmanager.ui`.
- **Sens des dépendances** : `model` n'importe ni `core` ni `ordonnancement` ;
  `ordonnancement` n'importe pas `core` ; `core` importe `ordonnancement`
  (`ServiceInscription`), ce qui est autorisé ; `persistence` n'importe ni `ui`,
  ni `importation`, ni JavaFX. Seul écart : M1.
- **Ports** : `Horloge`, `Geocodeur`, `EnvoiEmail`, `GenerateurCode`,
  `HachageMotDePasse`, `RegistreLicences` et `RegistreClubs` sont des interfaces
  du cœur. Elles sont implémentées hors du cœur (`ui/infra/*`,
  `persistence/DepotApplication`) ou en mémoire, sans effet de bord
  (`core/RegistreClubsMemoire`). Le réseau (`GeocodeurGeoplateforme`) et
  `SecureRandom` restent dans `ui.infra`. Aucun `new Random()` sans graine dans le
  cœur (`PlacementQualifiesService.java:121` est amorcé), aucun accès réseau, aucun
  `System.out`.
- **Dépendances Maven** : `javafx-controls` 21.0.5, `javafx-web` 21.0.5,
  `pdfbox` 2.0.29 et `junit-jupiter` 5.10.2 (test), ce qui correspond à la liste
  autorisée (JavaFX, PDFBox 2, JUnit 5). Plugins : `maven-surefire-plugin` 3.2.5
  et `javafx-maven-plugin` 0.0.8. Aucune dépendance nouvelle. Leaflet 1.9.4 est
  embarqué en ressource (licence fournie), sans dépendance Maven, comme documenté.
- **Tests** : 412 en tout, aucun `@Disabled`, 0 échec. `RoundTripTournoiTest` est
  présent, et les lecteurs refusent un fichier de version plus récente
  (`TournoiMapper.java:468`, `SessionMapperComplet.java:68`, `DepotApplication.java:317`).
- **Fichiers committés** : aucun `target/`, `.class`, `.jar`, réglage d'IDE ni
  secret. `.claude/settings.json` ne contient que des permissions d'agent : pas de
  secret, et les poussées forcées ou directes sur `main` y sont interdites.
- **Cohérence documentaire** : les nombres de tests en tête de `README.md` et de
  `ETAT_DU_PROJET.md` (412) sont exacts. La liste des dépendances de
  `CLAUDE.md:58-59` et `README.md:44-48` correspond au `pom.xml`. L'unique entrée
  de `QUESTIONS.md` (PDFBox dans le cœur) concorde avec `CLAUDE.md:60-61`.
  `VERSION_DEMO = ".demo-brunoy-v12"` (`ui/ContexteApplication.java:204`)
  concorde avec les passations. Toutes les classes citées dans la section 0
  d'`ETAT_DU_PROJET.md` et dans `CLAUDE.md` existent.

## Les trois actions les plus utiles

1. **Supprimer les six `switch` sur énumération (H1)** : 5 fichiers,
   `ConfigurationTableau.java:214`, `RegleTableauxParJour.java:119`,
   `Ordonnanceur.java:208` et `:256`, `JsonWriter.java:30`, `TournoiMapper.java:622`.
   Les remplacer par des chaînes de `if` à comportement identique, relancer
   `mvn -B test`, puis vérifier que `find target/classes -name '*$1.class'` ne
   liste plus ces classes. C'est le seul risque connu de casse du build chez
   Quentin.
2. **Faire passer le temps par le port `Horloge` (M2)** : injecter `Horloge` dans
   `GestionCartonsMatch` (lignes 74 et 96) et supprimer les replis
   `LocalDate.now()` de `ImportJoueurs.java:525` et `JoueurDepuisLicence.java:33`
   (date de référence obligatoire, ou issue de l'horloge). Ajouter des tests
   déterministes, dont un sur la catégorie d'âge calculée par rapport à la saison
   du tournoi.
3. **Remettre la documentation à niveau et trancher l'arête
   `importation → persistence.json` (M3 + M1)** : mettre à jour les sections 5, 7,
   9 et 10 et le pied d'`ETAT_DU_PROJET.md`, l'arborescence et le « prochain bloc »
   du `README.md`, ainsi que `CONFORMITE_FFTT.md:138`. Ajouter dans `QUESTIONS.md`
   le choix entre un socle JSON neutre (`com.pingmanager.json`) et la
   documentation de l'arête existante.
