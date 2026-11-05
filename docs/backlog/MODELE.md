---
id: B000
titre: Titre court de la tâche
type: idee            # idee | jour-j | interface | charte | bug | regle | recherche | doc
priorite: P2          # P1 urgent, P2 normal, P3 plus tard
taille: M             # S (< 1 h), M (demi-journée), L (journée : une seule par session)
statut: a-faire       # a-faire | en-cours | bloquee | a-relire | fait
depend-de: []         # ex. [B003]
fichiers: []          # zones touchées, pour éviter deux tâches parallèles sur les mêmes fichiers
branche:
pr:
---

## Ce qu'il faut faire

Ce que l'utilisateur doit pouvoir faire à la fin, en quelques lignes.

## Critères de fin

- [ ] …
- [ ] `mvn -B test` à 0 échec

## Contraintes / décisions de Quentin

## Passations

(Une ligne ou trois par agent de la chaîne : qui, quoi, problèmes pour la suite.)

## Résultat

(Rempli par le documentaliste puis l'intégrateur.)
