# Déblayage du dépôt (captures, plans obsolètes, code mort) — 2026-11-06

- Branche : `tache/nettoyage-depot`
- Agent : session chef de projet (demande directe de Quentin)
- Pull Request : voir la PR de la branche
- Statut : fusionné si GitHub Actions est vert

## Pour Quentin

Le dépôt passe d'environ 33 Mo de fichiers suivis à environ 4 Mo : les captures
d'écran des anciens lots (31 Mo) sont supprimées, seule la référence de la
charte v2 (`docs/captures/v2-apres/`) est gardée. Les captures continuent
d'être faites et regardées par les agents, mais **dans `/tmp`, sans être
committées**. Quatre plans ponctuels déjà exécutés (consignes et plan de nuit,
tâches autonomes, plan d'interface v2) sont retirés. Côté code : une
cinquantaine de méthodes, champs et imports jamais utilisés, et la classe
`RenduHtmlReglement` (rendu HTML du règlement, jamais appelé), sont supprimés.
Aucun changement visible dans l'application.

## À tester à l'écran

- Rien de particulier : aucun écran n'a changé. Un lancement rapide suffit.

## Questions pour Quentin

### 2026-11-06 — Captures d'écran hors du dépôt
- Contexte : les captures avant / après de chaque lot gonflaient le dépôt.
- Options : A) ne plus les committer (seule `docs/captures/v2-apres/` est versionnée) B) revenir à l'ancien fonctionnement.
- Choix provisoire de l'agent : A (réversible : skill `charte-graphique`, agents `designer-interface` et `integrateur`).
- Réponse de Quentin :

## Notes pour l'état du projet

- Captures : seule `docs/captures/v2-apres/` est versionnée ; les autres vont dans `/tmp/captures/<lot>/`.
- Supprimés : `docs/CONSIGNES_NUIT.md`, `docs/PLAN_DE_NUIT.md`, `docs/TACHES_AUTONOMES.md`, `docs/PLAN_INTERFACE.md`, `core.reglement.RenduHtmlReglement`.
- Méthodes inutilisées retirées (setters du modèle jamais appelés, aides d'interface, accès de persistance) ; getters du modèle gardés.
- Format JSON inchangé.
- L'historique git garde les anciennes captures : un clone complet reste lourd ; un clone `--depth 1` est léger.

## Fusion

À compléter après synchronisation.

## Chiffres

- Tests : 1042 → 1042 (`outils-dev/tests.sh` ; Maven Central inaccessible dans la session, GitHub Actions fait foi)
- Version de la démo : inchangée
