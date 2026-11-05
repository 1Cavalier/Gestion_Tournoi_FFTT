---
id: B002
titre: Faire passer la date et l'heure du cœur par le port Horloge
type: bug
priorite: P2
taille: M
statut: a-faire
depend-de: [B001]
fichiers: [core/GestionCartonsMatch, core/ImportJoueurs, core/JoueurDepuisLicence]
branche:
pr:
---

## Ce qu'il faut faire

Toute lecture de la date ou de l'heure dans `model`, `core` et `ordonnancement` (`now()`, `LocalDate.now()`…) passe par le port `Horloge`, avec une valeur par défaut pour ne pas casser les appelants. Tests à horloge fixe, notamment la catégorie d'âge à une date donnée (II.302 / II.303).

## Critères de fin

- [ ] `Grep "now\(\)"` ne trouve plus rien dans model, core, ordonnancement (hors port)
- [ ] tests de catégorie d'âge à date fixe
- [ ] `mvn -B test` à 0 échec

## Contraintes / décisions de Quentin

Aucune pour l'instant.

## Passations

## Résultat
