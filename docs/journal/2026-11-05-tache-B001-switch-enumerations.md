# B001 : remplacer les switch sur énumération — 2026-11-05

- Branche : `tache/B001-switch-enumerations`
- Agent : correcteur, relecteur-code, integrateur
- Pull Request : #18
- Statut : fusionné si GitHub Actions est vert (voir PR)

## Pour Quentin

Rien ne change à l'écran. Les 6 `switch` sur énumération du code ont été
réécrits (sur `.name()`), donc le risque de casse du build lié aux classes
annexes `X$1` disparaît. Comportement identique, aucun test modifié.

## À tester à l'écran

Rien de particulier : lancer l'application comme d'habitude.

## Questions pour Quentin

Aucune.

## Notes pour l'état du projet

`ConfigurationTableau`, `RegleTableauxParJour`, `Ordonnanceur` (×2), `JsonWriter`,
`TournoiMapper` : plus de `switch` d'énumération. Les 3 `switch` restants
portent sur des String / int. Format de sauvegarde et démo inchangés.

## Fusion

OK, rien à signaler (`origin/main` déjà intégré, aucun conflit).

## Chiffres

- Tests : mesurés par GitHub Actions (Maven Central bloqué en session)
- Version de la démo : inchangée
