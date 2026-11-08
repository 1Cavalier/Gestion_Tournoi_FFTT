---
id: B023
titre: Ports d'échange FFTT et import / export par fichier
type: idee
priorite: P2
taille: M
statut: en-cours
depend-de: [B022]
fichiers: [core (ports), importation, persistence]
branche: tache/B023-ports-fftt
pr:
---

## Ce qu'il faut faire

D'après B022 : préparer le code pour la FFTT **sans dépendre de l'accès à l'API** :
- ports du cœur pour les licences, les clubs et l'export des résultats ;
- implémentation **par fichier** : import d'une liste de licenciés et de clubs fournie par le club (format le plus proche de ce que produit SPID), export des résultats d'un tournoi dans un format documenté (provisoire tant que le format officiel n'est pas connu) ;
- l'implémentation par l'API viendra quand la FFTT aura donné son accord.

## Critères de fin

- [ ] ports et implémentations fichier testés
- [ ] export des résultats d'un tableau terminé
- [ ] `mvn -B test` à 0 échec

## Contraintes / décisions de Quentin

Aucune pour l'instant.

## Passations

- 2026-11-08 developpeur : ports (`ExportResultatsFederaux`, défauts `listerParClub` / `rechercher`), `ServiceResultatsFederaux`, lecteurs/registres/export CSV dans `importation`, format dans `docs/FORMAT_ECHANGES_FFTT.md` ; 1075 tests, 0 échec (`outils-dev/tests.sh`).
- Branchement : `ui.infra.RegistresImportes` (session seulement, superposé à la base) dans `ContexteApplication` ; boutons dans `EcranImportJoueurs` et sous le podium de `VuePhaseFinale` (tableau terminé). Interface non vue à l'écran.
- À faire ensuite : relecture sécurité/architecture ; 3 questions pour Quentin dans le journal (conservation de l'import, classement au-delà du podium, clubs sans courriel) ; persistance non touchée.

## Résultat
