---
id: B024
titre: Organiser la salle : plusieurs salles, plan des tables
type: idee
priorite: P2
taille: L
statut: en-cours
depend-de: [B011, B035]
fichiers: [model (salle, plan), ordonnancement/Ordonnanceur, core/ServiceSalle, persistence, ui/ecran/EcranSalle (nouvel éditeur de plan)]
branche: tache/B024-plan-de-salle (prise le 2026-11-06)
pr:
---

## Ce qu'il faut faire

Le club organise l'espace comme il veut :
- **plusieurs salles** (ex. « Gymnase », « Dojo »), chacune avec ses tables ;
- **plan** : tables placées sur une grille, rangées, **couloirs** et espaces vides, numérotation libre, table d'arbitrage ou de marque ;
- éditeur de plan simple (ajouter, déplacer, numéroter, orienter) et plans types (2 rangées, 3 rangées…) ;
- la vue Salle (B011) affiche le plan réel au lieu d'une grille ;
- l'ordonnanceur tient compte des salles : tables réservées à un tableau ou à une salle, préférence « tableau X en salle 2 » ;
- persistance rétrocompatible (ancien tournoi = une salle, tables en grille).

## Critères de fin

- [x] plan persisté et relu (test)
- [x] réservations respectées par l'ordonnanceur (tests)
- [x] captures de l'éditeur et de la salle
- [x] `mvn -B test` à 0 échec

## Contraintes / décisions de Quentin

Précisions du cahier des charges v0 (section 6) : plan sur une **grille** (table = 2 × 2 cases, allée = 1 × 1), plusieurs salles, **repères** (table d'arbitrage, entrée / sortie, WC…), **mini-carte** pour plusieurs salles ou étages. Accessible depuis la deuxième version de l'écran du tournoi (après homologation, B035) ; affiché dans l'onglet « Salle » du jour J (B037).

Aucune pour l'instant.

## Passations

- 2026-11-06 `developpeur` : modèle (PlanSalle, SalleTournoi), cœur (PlansTypesSalle, ServicePlanSalle, EditionPlanSalles), réservations dans l'Ordonnanceur, JSON `salles` rétrocompatible, EcranPlanSalle + vue Salle sur le plan réel, captures `docs/captures/b024/`. 1022 tests verts. Questions dans le journal.

## Résultat
