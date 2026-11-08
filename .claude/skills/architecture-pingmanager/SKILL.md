---
name: architecture-pingmanager
description: Conventions d'architecture et de code de PingManager. À utiliser avant d'ajouter une classe, un service, un écran, un champ au tournoi, un format de compétition, ou de toucher à la persistance JSON.
---

# Architecture et conventions de PingManager

## Packages et sens des dépendances

```
ui (JavaFX)  ──►  persistence, importation  ──►  core  ──►  model, ordonnancement
```

- `model` : entités pures. Objets immuables avec builder (`ConfigurationTableau`,
  `InfosReglement`…) ou entités à cycle de vie gardé (`Tableau`, `Tournoi`).
- `model.format` : formats de compétition. Ajouter un format = une classe qui
  implémente `FormatTournoi` + l'enregistrer dans `RegistreFormats`. Le moteur
  ne change pas.
- `core` : services métier sans état autant que possible ; ils renvoient des
  objets `Resultat…` avec un motif précis (enum) au lieu de lever des exceptions
  pour les refus métier. `IllegalStateException` pour une transition illégale,
  `SecurityException` pour un défaut d'habilitation.
- `ordonnancement` : affectation des tables, estimation des durées, prévision.
- `importation` : lecture xlsx/ods/csv/json **sans dépendance** (XML durci).
- `persistence` : JSON maison (`persistence.json`), écriture atomique, autosave.
- `ui` : écrans JavaFX (`ui.ecran`), démo et infrastructure (`ui.infra`).

Vérifier qu'aucun import interdit n'apparaît :

```
Grep pattern "^import (javafx|org\.apache|com\.pingmanager\.(ui|persistence|importation))" path src/main/java/com/pingmanager/model
```

(à répéter pour `core` et `ordonnancement`). Seule exception tolérée :
`core/reglement/RenduPdfReglement.java` (PDFBox).

## Ajouter un champ au tournoi ou au tableau

Un champ traverse toujours les mêmes couches, dans cet ordre :

1. `model` : champ + accesseurs (ou builder pour un objet immuable).
2. `core.ServiceCompletudeTournoi` / `ServiceCycleTournoi` : son niveau
   d'exigence (homologation, publication, recommandé, optionnel). Le
   classement des champs existants se lit dans ces deux services (et
   `model.ChampHomologation`) ; en cas de doute, question dans la section 6
   du README.
3. `persistence` (mappers `TournoiMapper`, `ConfigMapper`…) : écriture **et**
   relecture des anciens fichiers sans ce champ (valeur par défaut).
4. `core.reglement.GenerateurReglement` si le champ apparaît dans le règlement.
5. `ui.ecran` : le formulaire de la section concernée.
6. Tests à chaque étape, dont un aller-retour de persistance.

## Persistance : règles intangibles

- Format JSON **versionné** et **rétrocompatible** : un fichier d'une version
  antérieure doit toujours se relire.
- Joueurs dédupliqués par licence ; matchs de phase finale reliés par
  identifiant stable ; mêmes instances partagées avec l'ordonnanceur.
- Toute modification passe les tests `RoundTripTournoiTest`,
  `RepriseSessionTest` et `ServiceFichierTournoiTest`.

## Tests

- JUnit 5, même package que la classe testée, noms en snake_case français.
- Tirages aléatoires toujours **reproductibles** (graine explicite) : les tests
  fixent la graine.
- Préférer de petits jeux de données construits dans le test ; regarder les
  tests voisins pour réutiliser leurs méthodes d'aide.
- Pour l'interface : tester la logique (contrôleurs, formatages, libellés) sans
  lancer JavaFX ; la validation visuelle se fait chez Quentin.
