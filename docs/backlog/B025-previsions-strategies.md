---
id: B025
titre: Prévisions d'ordonnancement et stratégies du club
type: idee
priorite: P2
taille: L
statut: fait (PR #49)
depend-de: []
fichiers: [ordonnancement (simulation, stratégies), core, ui/ecran (écran Prévisions)]
branche: tache/B025-previsions-strategies (prise le 2026-11-06)
pr: 49
---

## Ce qu'il faut faire

**Vrais essais à grande échelle** et prévisions avant le tournoi :
- un **simulateur** qui déroule un tournoi complet (400 joueurs, 15 tableaux, tableaux de 128, N tables) avec l'`EstimateurDuree`, les poules, les tableaux finaux et l'ordonnanceur, en accéléré ;
- des **stratégies** au choix du club : toutes les poules d'abord ; tableau par tableau ; équilibrer l'attente des joueurs ; finir les petits tableaux tôt ; priorité aux joueurs engagés dans plusieurs tableaux… (paramètres de `PoidsOrdonnancement` présentés en clair) ;
- un écran **« Prévisions »** : heure de fin prévue par tableau, attente moyenne et maximale des joueurs, occupation des tables, tableaux qui débordent, comparaison de deux stratégies ;
- tests de charge : temps de calcul de l'ordonnanceur et du simulateur sur ces volumes (mesurés).

## Critères de fin

- [x] simulation reproductible (graine) testée (`SimulateurTournoiTest`, `JeuDeSimulation`)
- [x] comparaison de stratégies affichée (`ComparaisonStrategies`, écran « Prévisions et stratégie »)
- [x] mesures de performance dans le journal
- [x] `mvn -B test` à 0 échec (983 tests, 2026-11-06)

## Contraintes / décisions de Quentin

Précisions du cahier des charges v0 (section 6), **très important** : le club choisit **une stratégie principale**, chacune **expliquée en clair** : (1) avancer au maximum les tableaux commencés tôt pour libérer de la place ; (2) tout faire avancer petit à petit ; (3) faire attendre le moins possible les joueurs, quitte à ralentir l'avancement général. Le choix se fait dans la deuxième version de l'écran du tournoi (B035) et règle les couleurs de priorité des matchs proposés (B037).

Aucune pour l'instant.

## Passations

- 2026-11-06, `developpeur` : trois stratégies (`StrategieOrdonnancement`, défaut « faire attendre le moins possible », poids d'origine) persistées avec le tournoi (champ facultatif `strategieOrdonnancement`).
  Simulateur reproductible par graine (`SimulateurTournoi`, `ComparaisonStrategies`), tests de charge mesurés.
  Écran « Prévisions et stratégie » dans la 2e version ; la salle (couleurs B037) suit la stratégie du tournoi. 969 tests verts.
- 2026-11-06, `verificateur-modele` : 14 tests aux limites ajoutés (simulateur et stratégies) ; 983 tests verts ; 0 défaut bloquant.
  Aucun changement de règle sportive (organisation, pas d'article FFTT).
- 2026-11-06, `relecteur-code` : 0 point bloquant. Non bloquant : `javafx.geometry.Pos` écrit en nom complet dans `EcranCompletion.carteStrategie` (à raccourcir par import, à faire au prochain passage sur l'écran).
  Détails et questions : `docs/journal/2026-11-06-tache-B025-previsions-strategies.md`.

## Résultat

Livré sur la branche `tache/B025-previsions-strategies` : simulateur reproductible, trois stratégies expliquées et enregistrées avec le tournoi, comparaison de deux stratégies, écran « Prévisions et stratégie ». Salle (B037) branchée sur la stratégie du tournoi. 983 tests verts. Validation visuelle de l'écran par Quentin restant. Fusion par l'`integrateur`.
