---
id: B008
titre: Mise à jour des points avant le tirage
type: idee
priorite: P2
taille: M
statut: a-faire
depend-de: [B006]
fichiers: [core (nouveau service), importation, ui/ecran (nouvel écran)]
branche:
pr:
---

## Ce qu'il faut faire

Importer un fichier de points (licence + points, formats déjà lus par `importation.LecteurTableur`) pour mettre à jour les points des inscrits avant le lancement, avec un aperçu (joueur, anciens points, nouveaux, licences inconnues) avant d'appliquer. Interdit sur un tableau déjà lancé.

## Critères de fin

- [ ] aperçu sans modification, puis application
- [ ] refus sur tableau lancé
- [ ] `mvn -B test` à 0 échec

## Contraintes / décisions de Quentin

Aucune pour l'instant.

## Passations

## Résultat
