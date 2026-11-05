---
id: B014
titre: Rendu graphique v2 : écrans restants et cohérence d'ensemble
type: charte
priorite: P2
taille: M
statut: a-faire
depend-de: [B005, B012, B013]
fichiers: [ui/ecran (écrans restants), theme.css]
branche:
pr:
---

## Ce qu'il faut faire

Écrans restants (compte, supervision, choix de préparation, tournoi importé, connexion si besoin), puis **passe de cohérence** sur toute l'application : captures de chaque page en clair et sombre (planche dans `docs/captures/v2-final/`), correction des écarts (espacements, tailles de titres, états vides, boutons), suppression des règles CSS mortes.

## Critères de fin

- [ ] planche de captures complète
- [ ] règles CSS inutilisées supprimées
- [ ] `mvn -B test` à 0 échec

## Contraintes / décisions de Quentin

Aucune pour l'instant.

## Passations

## Résultat
