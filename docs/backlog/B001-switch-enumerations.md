---
id: B001
titre: Remplacer les switch sur énumération
type: bug
priorite: P1
taille: S
statut: en-cours
depend-de: []
fichiers: [model/ConfigurationTableau, model/RegleTableauxParJour, ordonnancement/Ordonnanceur, persistence/json/JsonWriter, persistence/TournoiMapper]
branche: tache/B001-switch-enumerations (prise le 2026-11-05)
pr:
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

## Résultat
