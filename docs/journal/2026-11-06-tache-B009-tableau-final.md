# Conformité du tableau final (B009) — 2026-11-06

- Branche : `tache/B009-tableau-final`
- Agent : `controleur-conformite`
- Pull Request : #<n> (complété à l'étape 5)
- Statut : en attente d'intégration

## Pour Quentin

Le déroulé du tableau final (avancée des vainqueurs, forfaits, abandons, petite finale)
a été comparé au règlement 2026-2027 : aucun écart certain. Le règlement ne décrit pas ce
déroulé pour les tournois, c'est donc ta clause d'organisateur. Les têtes de série suivent I.304.2.
Deux points attendent toujours ta réponse (exempts, abandon en demie).

## À tester à l'écran

Rien de visible : audit et tests seulement.

## Questions pour Quentin

### 2026-11-06 — Place des exempts (rappel, déjà dans QUESTIONS.md)
- Contexte : deux exempts peuvent se faire face ; I.304 / I.305 ne disent pas où les mettre.
- Article FFTT : I.304.1 / I.305.1 (« Si le tableau est incomplet, il faut prendre également en compte le deuxième tour »), muets sur la place des exempts.
- Options : A) tel quel ; B) exempts aux têtes de série, un par paire ; C) demander à GIRPE.
- Choix provisoire de l'agent : A (inchangé).
- Réponse de Quentin :

### 2026-11-06 — Abandon en demie et petite finale (rappel)
- Contexte : le perdant d'une demie, même par abandon ou disqualification, joue la petite finale.
- Article FFTT : 3.5.3.1.1 (« ne doivent pas abandonner, sauf pour cause de maladie ou de blessure »), 3.5.2.10 (« disqualifier pour le restant de la compétition »).
- Options : A) il y est placé, le juge-arbitre déclare son forfait ; B) petite finale donnée d'office à l'autre perdant ; C) B seulement pour disqualification.
- Choix provisoire de l'agent : A (inchangé).
- Réponse de Quentin :

### 2026-11-06 — Parties de classement au-delà de la 4e place
- Contexte : aucune partie pour les places 5 et suivantes n'existe ; le règlement (IX) n'en parle pas.
- Article FFTT : aucun.
- Options : A) ne rien prévoir ; B) option par tableau.
- Choix provisoire de l'agent : A.
- Réponse de Quentin :

## Notes pour l'état du projet

Nouveau test `ConformiteTableauFinalTest` (10 tests). Aucun changement de code ni de sauvegarde.

## Fusion

À faire par l'intégrateur.

## Chiffres

- Tests : 1055 → 1065 (outils-dev/tests.sh), 0 échec
- Version de la démo : inchangée
