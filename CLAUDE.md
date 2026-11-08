# PingManager — consignes pour Claude

Logiciel de bureau Java pour gérer des **tournois individuels officiels FFTT**
(pas le Critérium, pas les épreuves par équipes). Destiné à la vente, avec un
futur passage au web. Propriétaire et décideur produit : **Quentin**.

**`README.md` est le seul document du projet** : état réel, lancement,
architecture, ce qui fonctionne, ce qui reste à faire (section 5), décisions
à prendre (section 6), conformité FFTT (section 7). Le lire avant toute tâche
importante (Grep puis lecture de la section utile). Aucun autre fichier de
documentation, de rapport, de journal ou d'image n'est versionné : ne pas en
recréer.

## Langue et style

- Tout est en **français** : noms de classes, méthodes, variables, tests,
  commentaires, messages de commit, Pull Requests, README.
- Noms de tests descriptifs en snake_case français :
  `liste_d_attente_rien_a_payer_puis_a_regler_apres_repechage`.
- Suivre le style du code voisin (builders pour les objets immuables,
  résultats détaillés `Resultat…` avec motif précis plutôt que des booléens).
- **Jamais de `switch` sur une énumération** : javac génère une classe annexe
  `X$1` qui a déjà cassé le build chez Quentin. Utiliser une chaîne de `if`
  ou un `switch` sur `.name()`.
- JavaFX CSS ne gère pas `:first-child` / `:last-child` : poser des classes.
- Données de démo : quand elles changent, `ContexteApplication.VERSION_DEMO`
  (marqueur `.demo-brunoy-vN`) doit être incrémenté pour qu'elles soient
  recréées au démarrage. **Uniquement à la fin**, après synchronisation avec
  `main` (skill `terminer-tache`), jamais pendant le travail.

## Règle d'or : l'architecture

```
com.pingmanager
├── model, model.format   entités pures
├── core (.feuilles, .reglement)  services métier
├── ordonnancement        tables, priorités, prévision
├── importation           lecture de tableurs (sans dépendance)
├── persistence(.json)    sauvegarde JSON locale, autosave, reprise
└── ui(.ecran, .infra, .diffusion)  interface JavaFX
```

- `model`, `core` et `ordonnancement` ne connaissent **ni l'interface ni la
  persistance** : jamais d'`import javafx…`, `com.pingmanager.ui…`,
  `com.pingmanager.persistence…` ou `com.pingmanager.importation…` dedans.
- Les dépendances vont de `ui` → `persistence`/`importation` → `core` →
  `model`/`ordonnancement`. Jamais l'inverse.
- Les effets de bord du cœur passent par des **ports** (interfaces : `EnvoiEmail`,
  `Horloge`, `Geocodeur`, `EnvoiSms`…), implémentés hors du cœur.
- Aucune nouvelle dépendance Maven sans accord explicite de Quentin. Dépendances
  actuelles : JavaFX (ui), PDFBox 2 (rendu PDF), JUnit 5 (tests).
- Exception connue, à ne pas étendre : `core.reglement.RenduPdfReglement`
  importe PDFBox.

## Règles sportives

- Toute règle FFTT se vérifie dans les **textes officiels 2026-2027**
  (règlements sportifs et généraux), jamais de mémoire ni sur un site tiers.
  Ils ne sont plus dans le dépôt : Quentin les fournit sur demande.
- Citer l'article dans le code ou le test concerné (`// I.303 : …`).
- Si le texte n'est pas disponible, ambigu ou muet : **ne pas deviner**.
  Appliquer l'option la plus prudente et ajouter la question dans la
  section 6 du README (article, cas, options, choix provisoire).
- Distinguer règle fédérale et **clause d'organisateur** configurable (par
  exemple « pas deux tableaux qui se suivent »).

## Compiler et tester

```
mvn -B test
```

- Une tâche n'est terminée que si les tests passent **à 0 échec**.
- Toute fonctionnalité ou correction du cœur s'accompagne de tests JUnit 5
  dans le même package que la classe testée (`src/test/java/...`).
- Ne jamais supprimer, désactiver (`@Disabled`) ni affaiblir un test existant
  pour faire passer le build. Si un test semble faux, le signaler.
- Il n'y a plus d'intégration continue sur GitHub : les tests de la session
  font foi.

**Session cloud (Maven Central inaccessible)** : depuis la racine du dépôt,

```
apt-get update -qq && apt-get install -y -qq openjfx libopenjfx-jni libpdfbox2-java junit5 xvfb
J=/usr/share/java/junit-platform-console-standalone.jar
FX=$(ls /usr/share/java/javafx-{base,controls,graphics,web,media}.jar | paste -sd:)
PDF=/usr/share/java/pdfbox2.jar:/usr/share/java/fontbox2.jar:/usr/share/java/commons-logging.jar
B=$(mktemp -d); export LANG=C.UTF-8
javac -nowarn -d $B -cp $J:$FX:$PDF $(find src/main/java src/test/java -name "*.java")
cp -r src/main/resources/* $B/
java -Duser.home=$B/home -jar $J -cp $B:$FX:$PDF --scan-classpath --disable-banner --details=summary \
  | grep -E "tests (successful|failed|found)|✘|MethodSource|=>"
```

Lancer depuis la racine : certains tests lisent `src/main/java`.
L'interface JavaFX ne peut pas être vérifiée visuellement en session cloud :
le dire clairement dans la Pull Request et laisser la validation visuelle à
Quentin. Les captures éventuelles restent dans `/tmp`, jamais committées.

## Travail autonome : le circuit des sessions

Plusieurs sessions peuvent tourner en parallèle et **fusionnent elles-mêmes**
leur travail. Pour éviter les conflits :

1. Travailler sur une **branche** dédiée `tache/<id>-<mot-cle>`, jamais
   directement sur `main`.
2. Au début : `git fetch origin main`, lire la section du README concernée et
   `git ls-remote --heads origin 'tache/*'` (tâches déjà prises).
3. Découper : petits commits cohérents, messages en français. Lancer les
   tests avant chaque commit significatif.
4. **README** : seul fichier partagé. Modifications **localisées** à la fin
   de la tâche : cocher ou retirer la tâche terminée (section 5), déplacer ce
   qui fonctionne dans la section 3 ou 4, ajouter les questions (section 6),
   mettre à jour la conformité (section 7) et le nombre de tests.
5. **Points de passage communs** (`theme.css`, `PageGestionTournoi`,
   `Coquille`, `ContexteApplication`, `Ui`) : modifications minimales et
   localisées ; dans `theme.css`, une section commentée par domaine, en fin
   de fichier.
6. En cas de blocage ou de choix produit : question dans la section 6 du
   README, option la plus prudente et réversible, continuer.
7. **Étape de pipeline** : si ta consigne commence par « Étape de pipeline »
   (elle vient de la skill `piloter-projet`), tu **n'appliques pas**
   `terminer-tache` et tu ne fusionnes pas : tu commites sur la branche
   indiquée et tu rends ton compte rendu court. Seul l'agent `integrateur`
   termine la tâche.
8. **Pour finir, toujours appliquer la skill `terminer-tache`** :
   mise à jour du README, synchronisation avec `main`, tests, Pull Request,
   **fusion automatique** si les tests passent (sauf cas listés dans la
   skill, ou consigne « relecture »), description finale pour Quentin.

## Avancer sans Quentin : la chaîne d'agents

- Le travail à faire est la **section 5 du README** (identifiants `B0xx`,
  priorité P1 avant le reste). « Avance le projet » ou une tâche planifiée →
  skill `piloter-projet` : elle prend la tâche suivante et la fait passer par
  la chaîne d'agents, jusqu'à l'`integrateur`.
- Les agents de la chaîne (`.claude/agents/`) : `developpeur` (idées),
  `developpeur-jour-j`, `developpeur-interface` (écrans), `designer-interface`
  (charte), `verificateur-modele` (cœur), `relecteur-code` (nouveau code et
  liaisons), `controleur-conformite` (règlements), `correcteur` (erreurs),
  `documentaliste` (README), `integrateur` (synchronisation, tests, fusion).
  Hors chaîne : `recapitulateur`, `chercheur-fftt`, `auditeur-securite`,
  `gardien-architecture`, `optimiseur`, `testeur-junit`.
- Les rapports (sécurité, architecture, optimisation, bugs, recherches) ne
  sont plus des fichiers : ils vont dans la Pull Request, et les actions à
  mener deviennent des lignes de la section 5 du README.

## Économie de jetons (s'applique à tous les agents)

- **Lire peu** : Grep d'abord, puis Read sur la zone utile (offset/limit).
  Jamais en entier : `theme.css`, les gros écrans.
- **Sorties courtes** : en cas d'échec des tests, seulement les lignes
  `ERROR` / `FAIL` / `expected` ; jamais de journal entier collé.
- **Pas de travail en double** : ne relis pas ce qu'un agent précédent a
  résumé ; ne relance pas les tests si rien n'a changé depuis le dernier
  passage vert.
- **Comptes rendus brefs** (5 à 10 lignes) : ils sont relus par l'agent
  suivant et par Quentin.

## Ce qu'il ne faut pas faire

- Modifier le format de sauvegarde JSON sans garder la relecture des anciens
  fichiers (rétrocompatibilité obligatoire, tests `RoundTripTournoiTest`).
- Toucher aux données de démo de Brunoy (`ui.infra.TournoiDemoBrunoy`) sans
  que la tâche le demande.
- Committer des fichiers générés (`target/`), des réglages d'IDE, des
  captures, des documents autres que le README, ou des secrets.
