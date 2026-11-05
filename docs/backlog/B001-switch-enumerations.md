---
id: B001
titre: Remplacer les switch sur énumération
type: bug
priorite: P1
taille: S
statut: fait
depend-de: []
fichiers: [model/ConfigurationTableau, model/RegleTableauxParJour, ordonnancement/Ordonnanceur, persistence/json/JsonWriter, persistence/TournoiMapper]
branche: tache/B001-switch-enumerations (prise le 2026-11-05)
pr: 18
---

## Ce qu'il faut faire

Remplacer tous les `switch` sur une énumération de `src/main/java` (liste dans `RAPPORT_ARCHITECTURE.md`, à recontrôler : certains `switch` portent sur des chaînes ou des entiers et sont permis) par des chaînes de `if` ou un `switch` sur `.name()`, sans changer le comportement.

## Critères de fin

- [ ] plus aucune classe annexe `$1` due à un switch d'énumération dans `target/classes`
- [ ] comportement inchangé (aucun attendu de test modifié)
- [ ] `mvn -B test` à 0 échec

## Contraintes / décisions de Quentin

Aucune pour l'instant.

## Passations

- **correcteur** : 6 `switch` d'énumération passés sur `.name()` (ConfigurationTableau, RegleTableauxParJour, Ordonnanceur ×2, JsonWriter, TournoiMapper) ; `default -> throw IllegalStateException` ajouté aux 4 `switch` qui renvoient une valeur (inatteignable). Les 13 autres `switch` portent sur int / char / String. `javac` hors ui OK, plus de `$SwitchMap$` ; tests via GitHub Actions (mvn bloqué ici).

## Résultat

Fait, PR #18. 6 switch d'énumération remplacés, comportement inchangé.
