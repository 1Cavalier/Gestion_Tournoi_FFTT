---
id: B024
titre: Organiser la salle : plusieurs salles, plan des tables
type: idee
priorite: P2
taille: L
statut: a-faire
depend-de: [B011]
fichiers: [model (salle, plan), ordonnancement/Ordonnanceur, core/ServiceSalle, persistence, ui/ecran/EcranSalle (nouvel éditeur de plan)]
branche:
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

- [ ] plan persisté et relu (test)
- [ ] réservations respectées par l'ordonnanceur (tests)
- [ ] captures de l'éditeur et de la salle
- [ ] `mvn -B test` à 0 échec

## Contraintes / décisions de Quentin

Aucune pour l'instant.

## Passations

## Résultat
