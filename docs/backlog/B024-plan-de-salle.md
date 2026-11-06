---
id: B024
titre: Organiser la salle : plusieurs salles, plan des tables
type: idee
priorite: P2
taille: L
statut: fait
depend-de: [B011, B035]
fichiers: [model (salle, plan), ordonnancement/Ordonnanceur, core/ServiceSalle, persistence, ui/ecran/EcranSalle (nouvel éditeur de plan)]
branche: tache/B024-plan-de-salle (prise le 2026-11-06)
pr: 51
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

- 2026-11-06 `developpeur` : modèle (PlanSalle, SalleTournoi), cœur (PlansTypesSalle, ServicePlanSalle, EditionPlanSalles), réservations dans l'Ordonnanceur et ServiceSalle, JSON `salles` facultatif, EcranPlanSalle depuis EcranCompletion, vue Salle du jour J sur le plan réel, captures `docs/captures/b024/`.
  Restes : défilement horizontal au-delà de 7 tables par rangée au jour J ; glisser-déposer non vérifié ; SimulateurTournoi ignore les réservations. 1022 tests verts.
- 2026-11-06 `verificateur-modele` : +20 tests limites (PlanSalleLimitesTest, OrdonnanceurReservationsLimitesTest, PlanSalleMapperLimitesTest), 1042 verts, 0 bloquant.
  Mineurs : table réservée à un tableau absent jamais utilisée (signalée seulement par `ServicePlanSalle.verifier`) ; `Tournoi.setSalles(null)` = « sans plan » non documenté.
- 2026-11-06 `relecteur-code` : 0 bloquant. Non bloquant : classe `plan-grille-jourj` (EcranSalle.java:427) sans règle CSS.

## Résultat

Fusionné. Plan de salle persisté et relu (`salles` facultatif, rétrocompatible), réservations de tables et de salles respectées par l'ordonnanceur, éditeur et vue Salle du jour J sur le plan réel, captures `docs/captures/b024/`. `mvn -B test` : 1042 tests, 0 échec. Validation visuelle et glisser-déposer à faire par Quentin (voir le journal).
