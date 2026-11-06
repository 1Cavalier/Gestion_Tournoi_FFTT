# B012 : rendu graphique v2 de la préparation du tournoi — 2026-11-06

- Branche : `tache/B012-rendu-v2-pre-tournoi`
- Agent : `designer-interface`, `relecteur-code`, `integrateur`
- Pull Request : #41
- Statut : fusionné

## Pour Quentin

Les écrans de préparation du tournoi (vue d'ensemble, Général, Tableaux et
inscriptions, Informations sportives, Complémentaire) passent au rendu v2 :
vue d'ensemble en tuiles, sections en cartes à deux colonnes avec jauge et
icône, formulaires en une carte par groupe, grille des tableaux lisible avec
pastille de public, rappels ambre adoucis. La logique est inchangée.

## À tester à l'écran

- Compte de démo : celui de Brunoy habituel.
- Chemin : Gestion des tournois → un tournoi → préparation (vue d'ensemble, Général, Tableaux, Sportif, Complémentaire) ; nouveau tableau.
- À regarder en clair et sombre : tuiles, cartes, grille des tableaux, rappels, largeurs de pages élargies.
- Vérification visuelle par clic réel non faite (session cloud) : seules les captures du mode `prepa` du harnais (28 clair + 28 sombre, `docs/captures/B012`) ont été regardées. Validation visuelle à faire par Quentin.

## Questions pour Quentin

### 2026-11-06 — Adoucir partout l'aplat ambre des rappels ?
- Contexte : `.rappel-attention` a été adouci dans ces écrans ; faut-il le faire partout ?
- Options : A) oui, partout B) seulement ici
- Choix provisoire de l'agent : B (réversible)
- Réponse de Quentin :

## Notes pour l'état du projet

- Section B012 en fin de `theme.css` (équilibré 512/512), 4 icônes ajoutées à `Icones`, mode capture `prepa` dans le harnais.
- Règles de grille partagées avec `ApercuReglement` et `CarteReglesInscription` (CSS seulement).
- Largeurs de pages élargies : à valider.
- Non bloquant, préexistant et hors périmètre (relève de B001) : `switch` sur `type.getValue()` (énumération probable, interdit par CLAUDE.md) dans `FormulaireTableau.java` l.469 et 490.
- Démo inchangée.

## Fusion

main déjà intégré (merge : Already up to date). `verifier-fusion.py` : OK, rien à signaler. theme.css équilibré 512/512.

## Chiffres

- Tests : 798, 0 échec (`mvn -B test`)
- Version de la démo : inchangée
