---
id: B003
titre: Remettre la documentation à niveau
type: doc
priorite: P2
taille: S
statut: a-faire
depend-de: []
fichiers: [README.md, CONFORMITE_FFTT.md, docs/passation]
branche:
pr:
---

## Ce qu'il faut faire

`README.md` (arborescence réelle des packages, dépendances, nombre de tests mesuré), `CONFORMITE_FFTT.md` (articles couverts depuis le jour J, la phase finale et la petite finale). Ne pas toucher `ETAT_DU_PROJET.md` (le récapitulateur s'en charge), mais signaler dans le journal que sa section 4 doit dire SPIDD au lieu de GIRPE pour les tournois individuels (voir `docs/RECHERCHE_FFTT.md`).

## Critères de fin

- [ ] chiffres et arborescence conformes au code
- [ ] `mvn -B test` à 0 échec

## Contraintes / décisions de Quentin

Aucune pour l'instant.

## Passations

## Résultat
