---
id: B025
titre: Prévisions d'ordonnancement et stratégies du club
type: idee
priorite: P2
taille: L
statut: a-faire
depend-de: []
fichiers: [ordonnancement (simulation, stratégies), core, ui/ecran (écran Prévisions)]
branche:
pr:
---

## Ce qu'il faut faire

**Vrais essais à grande échelle** et prévisions avant le tournoi :
- un **simulateur** qui déroule un tournoi complet (400 joueurs, 15 tableaux, tableaux de 128, N tables) avec l'`EstimateurDuree`, les poules, les tableaux finaux et l'ordonnanceur, en accéléré ;
- des **stratégies** au choix du club : toutes les poules d'abord ; tableau par tableau ; équilibrer l'attente des joueurs ; finir les petits tableaux tôt ; priorité aux joueurs engagés dans plusieurs tableaux… (paramètres de `PoidsOrdonnancement` présentés en clair) ;
- un écran **« Prévisions »** : heure de fin prévue par tableau, attente moyenne et maximale des joueurs, occupation des tables, tableaux qui débordent, comparaison de deux stratégies ;
- tests de charge : temps de calcul de l'ordonnanceur et du simulateur sur ces volumes (mesurés).

## Critères de fin

- [ ] simulation reproductible (graine) testée
- [ ] comparaison de stratégies affichée
- [ ] mesures de performance dans le journal
- [ ] `mvn -B test` à 0 échec

## Contraintes / décisions de Quentin

Aucune pour l'instant.

## Passations

## Résultat
