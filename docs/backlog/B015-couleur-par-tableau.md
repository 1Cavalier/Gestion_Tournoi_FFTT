---
id: B015
titre: Une couleur par tableau, partout
type: idee
priorite: P2
taille: M
statut: a-faire
depend-de: []
fichiers: [model/ConfigurationTableau, persistence/ConfigMapper, ui/ecran/FormulaireTableau, ui/ecran/Ui, theme.css]
branche:
pr:
---

## Ce qu'il faut faire

Chaque tableau a une **couleur** (palette fixe d'une douzaine de teintes lisibles en clair et sombre, attribuée automatiquement, modifiable dans le formulaire du tableau). Elle sert de repère aux encadrants et aux joueurs : pastille sur les matchs proposés, les tables, les poules, l'arbre, les feuilles imprimées et le suivi sur téléphone. Persistée (anciens fichiers : couleur attribuée à la lecture).

## Critères de fin

- [ ] couleur persistée et rétrocompatible (test aller-retour)
- [ ] visible sur la salle, les poules et l'arbre
- [ ] `mvn -B test` à 0 échec

## Contraintes / décisions de Quentin

Lecture retenue de la demande « système de couleur pour le tableau des encadrants » : une couleur d'identification par tableau. Si Quentin voulait autre chose, il le précisera.

## Passations

## Résultat
