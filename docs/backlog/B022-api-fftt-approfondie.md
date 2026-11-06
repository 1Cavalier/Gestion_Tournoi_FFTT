---
id: B022
titre: Approfondir l'échange avec la FFTT : joueurs, clubs, résultats
type: recherche
priorite: P1
taille: M
statut: fait
depend-de: []
fichiers: [docs/RECHERCHE_FFTT.md]
branche: tache/B022-api-fftt
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

- **chercheur-fftt (6 nov. 2026)** : `docs/RECHERCHE_FFTT.md` complété (§2.6 joueurs/licences avec noms exacts des champs `xml_licence_b`, `xml_liste_joueur(_o)` ; §2.7 clubs `xml_club_detail`, `xml_club_dep2`, `xml_club_b` ; §2.8 remontée des résultats ; sources [42] à [51]).
  Inconnus : champ certificat dans l'API 2026, export de licenciés depuis SPID, format SPIDD → SPID (aucun public) ; documents FFTT 2025-2026 repérés mais illisibles (fftt.com bloqué).
  Pour B023 (§4.1) : ports `RegistreLicences`/`RegistreClubs` inchangés + nouveau `ExportResultatsFederaux.produire(ResultatsTournoi)` ; fichiers CSV `licencies.csv`, `clubs.csv`, `parties.csv`, `classements.csv` (colonnes détaillées) ; démarches de Quentin 8 à 12 ajoutées au §5.

## Résultat

Recherche complétée dans `docs/RECHERCHE_FFTT.md` (§2.6 à 2.8, §4.1, démarches 8 à 12). Aucun code. Journal : `docs/journal/2026-11-06-tache-B022-api-fftt.md`.
