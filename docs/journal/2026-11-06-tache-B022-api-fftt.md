# B022 : recherche FFTT approfondie (licences, clubs, résultats) — 2026-11-06

- Branche : `tache/B022-api-fftt`
- Agent : `chercheur-fftt` puis `integrateur`
- Pull Request : #56
- Statut : fusionné après verdict GitHub Actions

## Pour Quentin

`docs/RECHERCHE_FFTT.md` détaille désormais les services de la FFTT pour les licenciés, les clubs et la remontée des résultats, et propose l'architecture pour la suite (B023) : un nouveau port d'export des résultats et quatre fichiers CSV (`licencies.csv`, `clubs.csv`, `parties.csv`, `classements.csv`). Les démarches 8 à 12 ont été ajoutées à ta liste (§5). Aucun changement à l'écran.

## À tester à l'écran

Sans objet (documentation seulement).

## Questions pour Quentin

Aucune nouvelle. Inconnus : champ certificat dans l'API 2026, export de licenciés depuis SPID, format SPIDD vers SPID ; www.fftt.com illisible depuis la session.

## Notes pour l'état du projet

Pas de code. Port prévu pour B023 : `ExportResultatsFederaux.produire(ResultatsTournoi)`, implémentations CSV dans `importation`.

## Fusion

Voir la section Chiffres ; résultat de `verifier-fusion.py` consigné ci-dessous.

## Chiffres

- Tests : 1042 réussis, 0 échec (`bash outils-dev/tests.sh`, après synchronisation avec main) ; verifier-fusion.py : OK, aucun conflit.
- Version de la démo : inchangée
