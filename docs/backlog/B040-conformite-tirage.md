---
id: B040
titre: Conformité du tirage au sort avec la pratique FFTT
type: regle
priorite: P2
taille: M
statut: fait
depend-de: []
fichiers: [CONFORMITE_FFTT.md, src/test]
branche: tache/B040-conformite-tirage
pr: 61
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
- Correcteur (2026-11-06) : les 4 écarts corrigés (`GenerateurPoules.separerLesClubs`, `PlacementQualifiesService` : vainqueurs par numéro de poule, retour arrière pour les clubs au 1er tour, 2e tour seulement si exempts) ; commentaire de `ordreDuCadre` corrigé ; aucun test modifié.
- Tests : 1051 / 1051 verts (`outils-dev/tests.sh`). Retour arrière borné (200 000 essais, environ 0,2 s au pire mesuré sur 64 places), sinon repli sur le tirage glouton historique.
- Pour le relecteur : I.305.2 corrigé car le texte dit « vainqueur de la poule 1 » sans ambiguïté sur le critère ; la question à Quentin reste ouverte pour confirmation (poule A = poule 1). Les tirages d'une graine donnée changent (ordre aléatoire consommé différemment).

## Résultat
- Contrôleur (relecture 2026-11-06) : corrections `aaa62da` revérifiées contre I.302, I.304, I.305.1 à I.305.4 (vainqueurs par numéro de poule, clubs au 1er tour, 2es opposés au 1er, 3es même demi et quart opposé, serpentin et têtes de poule conservés) : conformes, `CONFORMITE_FFTT.md` cohérent.
- Tests : 1051 / 1051 verts (`SP=/tmp/pm-B040b bash outils-dev/tests.sh`). Question ouverte pour Quentin inchangée : poule A = poule 1 pour I.305.2.
- Fusion : main synchronisé sans conflit, verifier-fusion OK, 1055 tests verts, PR #61.
