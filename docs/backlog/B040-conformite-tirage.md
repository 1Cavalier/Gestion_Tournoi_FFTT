---
id: B040
titre: Conformité du tirage au sort avec la pratique FFTT
type: regle
priorite: P2
taille: M
statut: en-cours
depend-de: []
fichiers: [CONFORMITE_FFTT.md, src/test]
branche: tache/B040-conformite-tirage
pr:
---

## Ce qu'il faut faire

Référence : `docs/CAHIER_DES_CHARGES_V0.md`, section 7. Vérifier le tirage des poules (I.302) et du tableau final (I.304, I.305) contre les règlements et ce qui est **publiquement** documenté du logiciel fédéral (SPID, guides du juge-arbitre) : serpentin, séparation des clubs, têtes de série, places des exempts. Aucune connexion à un espace authentifié. Écarts certains → tests et correction ; doutes → questions dans le journal.

## Critères de fin

- [ ] `CONFORMITE_FFTT.md` à jour pour ces articles
- [ ] `mvn -B test` à 0 échec

## Contraintes / décisions de Quentin

Voir le cahier des charges v0.

## Passations

- Contrôleur de conformité (2026-11-06) : audit I.302 / I.304 / I.305 fait ; `ConformiteTirageI302I305Test` ajouté (12 tests dont 4 rouges volontaires = 4 écarts certains) ; `CONFORMITE_FFTT.md` à jour.
- Pour le correcteur : liste exacte des 4 corrections dans `docs/journal/2026-11-06-tache-B040-conformite-tirage.md` (poules ordonnées, têtes de poule, vainqueurs par numéro de poule, club au 1er tour) ; ne pas toucher aux tests rouges, les faire passer.
- Questions pour Quentin (journal) : vainqueurs par numéro de poule ou force, source du tirage des 2es par blocs, tableau sans poules ; exempts déjà dans `QUESTIONS.md`.

## Résultat
