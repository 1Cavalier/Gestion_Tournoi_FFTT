---
id: B032
titre: Agenda personnel, ajout automatique des tournois, export
type: idee
priorite: P1
taille: M
statut: en-cours
depend-de: []
fichiers: [model (agenda), core (agenda), persistence/DepotApplication]
branche: tache/B032-agenda (2026-11-05)
pr:
---

## Ce qu'il faut faire

Section 1 et 3 du cahier des charges. Cœur et persistance de l'**agenda du compte** : dates personnelles (titre, date, heure facultative, note), **ajout automatique** d'un tournoi à l'inscription (et retrait à la désinscription complète), **export** au format iCalendar `.ics` (fichier importable dans Google Agenda, Outlook, Apple), écrit à la main sans dépendance. Pas d'écran ici (les tâches Accueil et Mes tournois l'afficheront) sauf une méthode de présentation testée.

## Critères de fin

- [ ] ajout et retrait automatiques testés
- [ ] fichier .ics valide (testé : en-têtes, échappement, fin de ligne CRLF)
- [ ] persistance rétrocompatible
- [ ] `mvn -B test` à 0 échec

## Contraintes / décisions de Quentin

Voir le cahier des charges v0.

## Passations

## Résultat
