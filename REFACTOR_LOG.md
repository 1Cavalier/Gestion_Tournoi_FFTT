# REFACTOR_LOG — nuit du 13 novembre 2026

Branche `refactor/night-2026-11-13`, partie de `main` au commit `d54a72d`.

## Référence (phase 0)

| Mesure | Valeur |
|---|---|
| Langage, build | Java 21, Maven (`mvn -B test`) ; en session cloud : `javac` + JUnit console (commande de `CLAUDE.md`) |
| Tests | 1 480 trouvés, 1 480 verts, 0 ignoré (28 s) |
| Linter | aucun configuré ; `javac -Xlint:all` sur le code principal : 4 avertissements (2 `deprecation`, 2 `serial`) |
| Fichiers Java | 582 (367 principaux, 215 de test) |
| Lignes Java (physiques) | 95 909 (66 697 principales, 29 212 de test) |
| Lignes de code (`cloc`) | 72 143 de code, 13 209 de commentaire, 10 557 vides |
| Couverture de lignes (JaCoCo) | 59,2 % au total ; cœur, modèle, ordonnancement, importation, persistance : 91 à 96 % ; `ui/ecran` : 19,5 % |
| Plus gros fichiers | `EcranSalle` 1 476, `GenerateurReglement` 1 119, `ContexteApplication` 951, `Ui` 939, `Tournoi` 904, `TournoiMapper` 839, `PresentationPrevisions` 838, `EcranSportif` 827, `EcranCompletion` 810, `FormulaireTableau` 775 |
| Lignes par paquet | `ui/ecran` 24 928 · `core` 15 925 · `model` 8 284 · `persistence` 4 221 · `ui/infra` 2 522 · `core/reglement` 2 400 · `ordonnancement` 2 310 · `importation` 1 915 · `ui/diffusion` 1 362 · `ui` 1 045 · `core/feuilles` 833 · `persistence/json` 615 · `model/format` 337 |

## Décisions prises seul

| N° | Décision | Pourquoi |
|---|---|---|
| D1 | Le dépôt de travail est `1Cavalier/Gestion_Tournoi_FFTT` (le seul qui contient PingManager). | La mission ne nommait pas le dépôt. |
| D2 | Branche `refactor/night-2026-11-13` et non `tache/<id>-…` ; pas de skill `terminer-tache`, **aucune fusion dans `main`**. | La mission l'exige ; elle est plus récente et plus prudente que le circuit de fusion automatique de `CLAUDE.md`. |
| D3 | Les documents de la nuit (`FEATURE_INVENTORY.md`, `REFACTOR_PLAN.md`, `REFACTOR_LOG.md`, `REFACTOR_REPORT.md`) sont committés à la racine de la branche, dans des commits qui ne touchent que la documentation. | La mission les demande, alors que `CLAUDE.md` dit « aucun autre document que le README ». À retirer (ou à garder) avant une fusion : décision de Quentin. |
| D4 | Messages de commit en français, au format `refactor(module): …`. | Concilie la mission (format) et `CLAUDE.md` (langue). |
| D5 | Les règles de `CLAUDE.md` restent en vigueur pendant la refonte : pas de `switch` sur une énumération, règle d'or de l'architecture, aucune dépendance Maven nouvelle, format JSON relisible, démo de Brunoy intouchée, aucun test existant modifié. | Ce sont des comportements et des contraintes du projet, donc couverts par « zéro perte ». |
| D6 | Les outils de vérification (couverture JaCoCo, comparaison de la surface publique, sorties de référence, parcours de l'interface) restent hors du dépôt, dans la session. | `CLAUDE.md` interdit de committer des fichiers générés ; JaCoCo n'est pas une dépendance du projet. |

## Journal des transformations

| Commit | Quoi | Pourquoi | Lignes avant → après |
|---|---|---|---|
| `503bd20` | T1 : `RenduPdfFeuilles` réutilise `propre`, `imprimable`, `rgb` de `RenduPdfReglement` | copies identiques (23 lignes) | 66697 → 66662 |
| `11380c3` | T3 : `core.TextesSuivi` (étape, tour, joueur, issue, score, podium), utilisé par `InstantaneSuivi`, `PresentationSuiviTournoi`, `PresentationPhaseFinale`, `GenerateurFeuilles` | mêmes textes recopiés dans le suivi sur téléphone et dans les écrans | 66662 → 66642 |
| `6c27192` | T6 : dix chaînes « if (x == ENUM) return constante » → tables `EnumMap` (`PresentationJourJ`, `DispositionArbre`, `PresentationPhaseFinale`, `StatutPastille`, `TextesSuivi`) | même résultat pour chaque valeur et pour null, en trois fois moins de lignes | 66642 → 66588 |
| `1e48088` | T7 : `GenerateurFeuilles.Place` → record privé | classe interne qui ne portait que trois champs finals | 66588 → 66579 |
| `65cfdb5` | T9 : 20 commentaires qui citaient `QUESTIONS.md`, `docs/…`, `CHARTE_GRAPHIQUE.md` mis à jour | ces documents ont été retirés du dépôt | 66579 → 66578 |
| `bf7b814` | Finition : 4 avertissements `javac -Xlint:all` → 0 (`serialVersionUID`, usage voulu de `typeTournoi` annoté) | base saine pour un futur linter | 66578 → 66584 |
| `e748a23` | T8 : retrait de `BlocSaisieSms.vider`, `PresentationRemplissageFinances.tonaliteSoldePrevisionnel`, `LoiDureeMatch.getMoyenneMinutes` (non publiques, jamais appelées). `cocher` et `ouvrirFicheParNom` sont gardées : elles servent au harnais de captures, hors dépôt | code mort | 66584 → 66571 |
