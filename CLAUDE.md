# PingManager — consignes pour Claude

Logiciel de bureau Java pour gérer des **tournois individuels officiels FFTT**
(pas le Critérium, pas les épreuves par équipes). Destiné à la vente, avec un
futur passage au web. Propriétaire et décideur produit : **Quentin**.

À lire avant toute tâche importante : `ETAT_DU_PROJET.md` (section 0 en
premier), puis `CONFORMITE_FFTT.md` si la tâche touche une règle sportive.

Le contexte détaillé (décisions de Quentin, champs du tournoi, historique des
phases) est dans `docs/passation/` :
- `REGLES_CREATION_TOURNOI.md` : chaque champ du tournoi, son niveau
  d'exigence (homologation / publication / recommandé / optionnel) et les
  décisions prises. **Référence pour tout ce qui touche la création d'un
  tournoi.**
- `PASSATION_CHOIX_ET_CLASSEUR.md` puis `PASSATION_IMPORT_ET_J-1.md` : dernier
  état (2 novembre 2026) et phase suivante « J-1, tournoi prêt ».
- `PASSATION_ONGLET_TOURNOIS.md`, `PASSATION_PHASE_INSCRIPTIONS.md` : phases
  précédentes.
- `CHARTE_GRAPHIQUE.md` : brief de la charte (détail opérationnel dans la skill
  `charte-graphique`).

## Langue et style

- Tout est en **français** : noms de classes, méthodes, variables, tests,
  commentaires, messages de commit, Pull Requests, documentation.
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
  `main` (skill `terminer-tache`, étape 3), jamais pendant le travail.

## Règle d'or : l'architecture

```
com.pingmanager
├── model, model.format   entités pures
├── core, core.reglement  services métier
├── ordonnancement        tables, priorités, prévision
├── importation           lecture de tableurs (sans dépendance)
├── persistence(.json)    sauvegarde JSON locale, autosave, reprise
└── ui(.ecran, .infra)    interface JavaFX
```

- `model`, `core` et `ordonnancement` ne connaissent **ni l'interface ni la
  persistance** : jamais d'`import javafx…`, `com.pingmanager.ui…`,
  `com.pingmanager.persistence…` ou `com.pingmanager.importation…` dedans.
- Les dépendances vont de `ui` → `persistence`/`importation` → `core` →
  `model`/`ordonnancement`. Jamais l'inverse.
- Les effets de bord du cœur passent par des **ports** (interfaces : `EnvoiEmail`,
  `Horloge`, `Geocodeur`…), implémentés hors du cœur.
- Aucune nouvelle dépendance Maven sans accord explicite de Quentin. Dépendances
  actuelles : JavaFX (ui), PDFBox 2 (rendu PDF), JUnit 5 (tests).
- Exception connue, à ne pas étendre : `core.reglement.RenduPdfReglement`
  importe PDFBox.

## Règles sportives

- Toute règle FFTT se vérifie dans les **textes officiels 2026-2027** fournis
  dans `docs/reglements/` (texte brut, recherche avec Grep sur le numéro
  d'article, par exemple `I.305`), jamais de mémoire ni sur un site tiers.
- Citer l'article dans le code ou le test concerné (`// I.303 : …`).
- Si un texte est ambigu ou muet : **ne pas deviner**. Noter la question dans
  le journal de la tâche (`docs/journal/`, voir plus bas : article, cas,
  options possibles) et continuer le reste.
- Distinguer règle fédérale et **clause d'organisateur** configurable (par
  exemple « pas deux tableaux qui se suivent »).

## Compiler et tester

```
mvn -B test
```

- Une tâche n'est terminée que si `mvn -B test` passe **à 0 échec**.
- Toute fonctionnalité ou correction du cœur s'accompagne de tests JUnit 5
  dans le même package que la classe testée (`src/test/java/...`).
- Ne jamais supprimer, désactiver (`@Disabled`) ni affaiblir un test existant
  pour faire passer le build. Si un test semble faux, le signaler.
- Si Maven Central est inaccessible, `outils-dev/tests.sh` compile et lance les
  tests avec `javac` (voir `outils-dev/LISEZMOI.md`).
- L'interface JavaFX ne peut pas être vérifiée visuellement en session cloud :
  le dire clairement dans la Pull Request et laisser la validation visuelle à
  Quentin.

## Travail autonome : le circuit des sessions

Plusieurs sessions tournent en parallèle et **fusionnent elles-mêmes** leur
travail. Pour éviter les conflits :

1. Travailler sur une **branche** dédiée, jamais directement sur `main`.
2. Au début : `git fetch origin main` et lire les journaux récents de
   `docs/journal/` (ce que les autres sessions viennent de faire), en plus de
   `ETAT_DU_PROJET.md` et `QUESTIONS.md`.
3. Découper : petits commits cohérents, messages en français. Lancer
   `mvn -B test` avant chaque commit significatif.
4. **Fichiers partagés** : ne pas modifier `QUESTIONS.md`,
   `ETAT_DU_PROJET.md` ni `RECAPITULATIF.md` ; tout va dans le journal de la
   tâche (`docs/journal/AAAA-MM-JJ-<branche>.md`, modèle
   `docs/journal/MODELE.md`). L'agent `recapitulateur` les y reporte.
   Exception : `docs/passation/REGLES_CREATION_TOURNOI.md` si un champ du
   tournoi change.
5. **Points de passage communs** (`theme.css`, `PageGestionTournoi`,
   `Coquille`, `ContexteApplication`, `Ui`) : modifications minimales et
   localisées ; dans `theme.css`, une section commentée par domaine, en fin
   de fichier.
6. En cas de blocage ou de choix produit : question dans le journal, option la
   plus prudente et réversible, continuer.
7. **Pour finir, toujours appliquer la skill `terminer-tache`** :
   synchronisation avec les dernières fusions de `main`, résolution des
   conflits, tests, Pull Request, **fusion automatique** si les tests passent
   (sauf cas listés dans la skill, ou consigne « relecture »), description
   finale pour Quentin.

## Ce qu'il ne faut pas faire

- Modifier le format de sauvegarde JSON sans garder la relecture des anciens
  fichiers (rétrocompatibilité obligatoire, tests `RoundTripTournoiTest`).
- Toucher aux données de démo de Brunoy (`ui.infra.TournoiDemoBrunoy`) sans
  que la tâche le demande.
- Committer des fichiers générés (`target/`), des réglages d'IDE ou des secrets.
