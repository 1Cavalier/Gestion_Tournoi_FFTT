---
id: B039
titre: Page du tournoi le jour J avec onglets ; pointage sans croix « payé »
type: jour-j
priorite: P1
taille: L
statut: a-faire
depend-de: [B037]
fichiers: [ui/ecran/EcranJourJ, ui/ecran/EcranPointageTableau, ui/ecran/EcranPoulesTableau]
branche:
pr:
---

## Ce qu'il faut faire

Référence : `docs/CAHIER_DES_CHARGES_V0.md`, section 7. Quand le tournoi devient « jour J », une page au **nom du tournoi** avec des **onglets** : **Général** (l'écran du juge-arbitre), **Pointage** (tous les tableaux), **un onglet par tableau** (poules, arbre), **Salle**. Au tirage, **retirer les croix « payé »** (présent dans la liste = a payé ; le paiement se règle au pointage).

## Critères de fin

- [ ] navigation par onglets
- [ ] plus de croix « payé » au tirage
- [ ] captures regardées
- [ ] `mvn -B test` à 0 échec

## Contraintes / décisions de Quentin

Voir le cahier des charges v0.

## Passations

## Résultat
