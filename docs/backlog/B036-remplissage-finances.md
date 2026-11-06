---
id: B036
titre: Remplissage et finances du tournoi
type: idee
priorite: P2
taille: M
statut: fait
depend-de: [B035]
fichiers: [core (finances), ui/ecran (carte Remplissage et finances)]
branche: tache/B036-remplissage-finances
date: 2026-11-06
pr: 46
---

## Ce qu'il faut faire

Référence : `docs/CAHIER_DES_CHARGES_V0.md`, section 6. Tableau de bord : par tableau et au total, inscrits / places, **taux de remplissage**, liste d'attente ; **argent** : recettes attendues (tarifs × inscrits), encaissées (payés), dotation promise (fixe et % des inscriptions), **solde** (négatif au départ), évolution. Cœur testé, carte dans la deuxième version de l'écran du tournoi (B035).

## Critères de fin

- [x] calculs testés (dotation en % comprise)
- [x] carte affichée après homologation
- [x] `mvn -B test` à 0 échec (940 tests)

## Contraintes / décisions de Quentin

Voir le cahier des charges v0.

## Passations

- `developpeur` (6/11) : cœur `ServiceRemplissageFinances` → `BilanRemplissageFinances` (tarif en ligne réutilisé, dotation fixe + % des inscriptions, solde, liste d'attente gratuite), présentation `PresentationRemplissageFinances` et carte dans `EcranCompletion` (deuxième version), CSS « B036 », captures `docs/captures/B036/apres/`. Tests verts.
  Pas d'évolution dans le temps : aucune date de paiement dans le modèle (format JSON inchangé) ; 4 questions d'argent dans le journal.
  À relire : `PresentationPreparationTournoi.cartesBientot()` (la carte B036 n'est plus « bientôt », `cartesAVenir()` inchangée).
- `verificateur-modele` (6/11) : 9 tests de cohérence verts ; aucun bloquant. Limites relevées : pas de paiement partiel ni de trop-payé dans le modèle (paiement binaire) ; le taux global ignore les tableaux sans limite (voulu, testé).
- `relecteur-code` (6/11) : aucun bloquant. À corriger si Quentin le souhaite : classes `carte-finances` et `finances-nom` sans règle dans `theme.css` ; noms complets au lieu d'imports (style).
- `documentaliste` (6/11) : journal complété (à tester à l'écran, défauts restants, question 5 sur l'arrondi du total) ; critères cochés ci-dessous.

## Résultat

- Cœur : `ServiceRemplissageFinances` → `BilanRemplissageFinances` (immuable) : remplissage et argent par tableau et au total, dotation fixe + part en % des inscriptions, solde négatif au départ, liste d'attente sans argent. Format de sauvegarde inchangé.
- Écran : carte « Remplissage et finances » dans la deuxième version de l'écran du tournoi homologué, au-dessus des cartes « bientôt » Salle et Stratégie.
- Captures clair et sombre dans `docs/captures/B036/apres/` (tournoi homologué sans inscrit). Validation à l'écran restée à Quentin (cas avec inscrits, attente et paiements).
- Limites restantes : paiement binaire (pas de partiel), réduction de dotation non appliquée, dotation et tarif à confirmer (questions du journal).
- Étape de pipeline : fusion à faire par l'`integrateur`.
