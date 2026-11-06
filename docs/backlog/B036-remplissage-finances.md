---
id: B036
titre: Remplissage et finances du tournoi
type: idee
priorite: P2
taille: M
statut: en-cours
depend-de: [B035]
fichiers: [core (finances), ui/ecran (carte Remplissage et finances)]
branche: tache/B036-remplissage-finances
date: 2026-11-06
pr:
---

## Ce qu'il faut faire

Référence : `docs/CAHIER_DES_CHARGES_V0.md`, section 6. Tableau de bord : par tableau et au total, inscrits / places, **taux de remplissage**, liste d'attente ; **argent** : recettes attendues (tarifs × inscrits), encaissées (payés), dotation promise (fixe et % des inscriptions), **solde** (négatif au départ), évolution. Cœur testé, carte dans la deuxième version de l'écran du tournoi (B035).

## Critères de fin

- [ ] calculs testés (dotation en % comprise)
- [ ] carte affichée après homologation
- [ ] `mvn -B test` à 0 échec

## Contraintes / décisions de Quentin

Voir le cahier des charges v0.

## Passations

- `developpeur` (6/11) : cœur `ServiceRemplissageFinances` → `BilanRemplissageFinances` (tarif en ligne réutilisé, dotation fixe + % des inscriptions, solde, liste d'attente gratuite), présentation `PresentationRemplissageFinances` et carte dans `EcranCompletion` (deuxième version), CSS « B036 », captures `docs/captures/B036/apres/`. Tests verts.
  Pas d'évolution dans le temps : aucune date de paiement dans le modèle (format JSON inchangé) ; 4 questions d'argent dans le journal.
  À relire : `PresentationPreparationTournoi.cartesBientot()` (la carte B036 n'est plus « bientôt », `cartesAVenir()` inchangée).

## Résultat
