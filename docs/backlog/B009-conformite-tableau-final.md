---
id: B009
titre: Vérifier le tableau final (I.304, I.305, petite finale, exempts)
type: regle
priorite: P2
taille: M
statut: en-cours
depend-de: []
fichiers: [CONFORMITE_FFTT.md, src/test]
branche: tache/B009-tableau-final
pr:
---

## Ce qu'il faut faire

Confronter au règlement le placement des qualifiés, les têtes de série, la séparation des clubs, la place des exempts et la petite finale. Écarts certains → correction ; ambiguïtés → questions dans le journal (la place des exempts est déjà en question dans `QUESTIONS.md`).

## Critères de fin

- [ ] `CONFORMITE_FFTT.md` à jour pour ces articles
- [ ] `mvn -B test` à 0 échec

## Contraintes / décisions de Quentin

Aucune pour l'instant.

## Passations

- controleur-conformite : audit I.304.2, déroulé, forfaits/abandons, exempts, petite finale ; aucun écart certain, 10 tests ajoutés (`ConformiteTableauFinalTest`), tous verts.
- Questions au journal `2026-11-06-tache-B009-tableau-final.md` : exempts, abandon en demie (déjà ouvertes), parties de classement.
- Pour le correcteur : rien à corriger ; `CONFORMITE_FFTT.md` mis à jour.

## Résultat
