---
id: B022
titre: Approfondir l'échange avec la FFTT : joueurs, clubs, résultats
type: recherche
priorite: P1
taille: M
statut: a-faire
depend-de: []
fichiers: [docs/RECHERCHE_FFTT.md]
branche:
pr:
---

## Ce qu'il faut faire

Pousser plus loin `docs/RECHERCHE_FFTT.md` :
- **liste des joueurs et des licences** (points, catégorie, club, certificat) : services exacts de l'API Smartping v2, champs, limites, conditions d'usage commercial ; alternatives (fichiers fournis par le club, export de SPID) ;
- **liste des clubs** (numéro, nom, adresse, département) ;
- **remontée des résultats** d'un tournoi : ce que SPIDD importe ou exporte, formats connus, ce qu'il faut demander à la FFTT ;
- proposition d'**architecture** : ports du cœur (`RegistreLicences`, `RegistreClubs` existants, nouveau port d'export des résultats) et implémentations possibles (API, fichier) ;
- liste à jour des démarches de Quentin.
Aucune connexion à un espace authentifié, aucun identifiant essayé, pas de moissonnage.

## Critères de fin

- [ ] sections joueurs, clubs, résultats complétées et sourcées
- [ ] architecture des ports proposée
- [ ] `mvn -B test` à 0 échec

## Contraintes / décisions de Quentin

Aucune pour l'instant.

## Passations

## Résultat
