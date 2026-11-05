---
id: B006
titre: Tableau de bord de la veille (J-1)
type: jour-j
priorite: P2
taille: M
statut: a-faire
depend-de: []
fichiers: [core (nouveau service), ui/ecran (nouvel écran), ui/ecran/PageGestionTournoi]
branche:
pr:
---

## Ce qu'il faut faire

La veille du tournoi, le club voit par tableau : inscrits, liste d'attente, payés, horaires et pointage, et une liste d'alertes (tableau sans horaire, sous le seuil de maintien `ConditionsMaintien`, juge-arbitre principal non désigné, joueur sans club ou sans points). Voir `docs/passation/PASSATION_IMPORT_ET_J-1.md` section 4.

## Critères de fin

- [ ] service du cœur testé (une alerte par cas)
- [ ] écran accessible depuis la gestion du tournoi
- [ ] `mvn -B test` à 0 échec

## Contraintes / décisions de Quentin

Aucune pour l'instant.

## Passations

## Résultat
