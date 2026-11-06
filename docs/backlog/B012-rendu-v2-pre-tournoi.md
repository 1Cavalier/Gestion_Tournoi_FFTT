---
id: B012
titre: Rendu graphique v2 : préparation du tournoi
type: charte
priorite: P1
taille: M
statut: a-relire
depend-de: []
fichiers: [ui/ecran/EcranCompletion, ui/ecran/FormulairesSection, ui/ecran/EcranTableaux, ui/ecran/FormulaireTableau, ui/ecran/EcranSportif, ui/ecran/EcranComplementaire, ui/ecran/CarteReglesInscription, theme.css]
branche: tache/B012-rendu-v2-pre-tournoi (2026-11-06)
pr:
---

## Ce qu'il faut faire

Passer au **vrai rendu v2** (skill `charte-graphique`) tous les écrans de préparation : vue d'ensemble (étapes et complétude sous forme de cartes et pastilles), sections Général, Tableaux et inscriptions (grille des tableaux lisible, formulaire du tableau), Informations sportives, Complémentaire. Formulaires aérés, libellés au-dessus, code couleur d'exigence conservé et réaccordé.

## Critères de fin

- [x] captures avant / après en clair et sombre, regardées
- [x] aucun style en dur restant dans ces écrans
- [ ] `mvn -B test` à 0 échec

## Contraintes / décisions de Quentin

Aucune pour l'instant.

## Passations

- designer-interface (2026-11-06) : rendu v2 des écrans de préparation : vue d'ensemble en tuiles, sections en cartes à deux colonnes avec jauge et icône ; formulaires Général et tableau en une carte par groupe ; grille des tableaux à bande d'en-tête et pastille de public ; rappels ambre adoucis ; menu « Ajouter un article » en bouton secondaire. CSS dans la section B012 en fin de `theme.css` (équilibre vérifié), 4 icônes ajoutées à `Icones`.
- Captures avant/après clair et sombre dans `docs/captures/B012/avant/` et `apres/` (mode `prepa` du harnais), relues. `mvn -B -q test` vert. Logique inchangée.
- À regarder : `CarteReglesInscription` n'a reçu que les règles CSS communes (grille, rappels), pas de modification Java ; rendu Windows et mise à l'échelle non vérifiés.

## Résultat
