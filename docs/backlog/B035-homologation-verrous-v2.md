---
id: B035
titre: Homologation : verrous, refus, et deuxième version de l'écran du tournoi
type: idee
priorite: P1
taille: L
statut: en-cours
depend-de: []
fichiers: [core/ServiceCycleTournoi, core/ServiceCompletudeTournoi, model/Tournoi, ui/ecran/EcranCompletion, ui/ecran/FormulairesSection]
branche: tache/B035-homologation-verrous
date: 2026-11-05
pr:
---

## Ce qu'il faut faire

Référence : `docs/CAHIER_DES_CHARGES_V0.md`, section 6. (1) **Cœur** : dès la demande d'homologation, tout champ exigé pour l'homologation (grille 🔴 de `docs/passation/REGLES_CREATION_TOURNOI.md`) est **refusé en modification** (motif clair) ; nouvelle transition **« Homologation refusée »** → retour au brouillon (motif saisi) ; acceptation → homologué. (2) **Écran** : préparation plus agréable et dynamique ; après acceptation, **deuxième version** de la vue du tournoi : le règlement **en bas** (lecture), et de nouvelles cartes **Salle** (lien vers le plan, B024), **Stratégie d'ordonnancement** (B025) et **Remplissage et finances** (B036) — cartes de lien « bientôt » si ces tâches ne sont pas encore fusionnées. Mettre à jour `REGLES_CREATION_TOURNOI.md`.

## Critères de fin

- [ ] verrous testés champ par champ
- [ ] refus → brouillon testé
- [ ] deuxième version affichée après homologation
- [ ] captures regardées
- [ ] `mvn -B test` à 0 échec

## Contraintes / décisions de Quentin

Voir le cahier des charges v0.

## Passations

- developpeur (cœur, 05/11) : verrous du dossier 🔴 dès la demande (`ServiceVerrousHomologation`, refus détaillé `ResultatModificationTournoi`), refus d'homologation avec motif → brouillon (`ServiceCycleTournoi.refuserHomologation`, JSON `refusHomologation` rétrocompatible). Tests 697 → 742.
- Pour l'écran : passer toutes les modifications des champs 🔴 par `ServiceVerrousHomologation` (les setters ne refusent rien) ; griser via `champsVerrouilles`/`motifVerrou` ; bouton « Homologation refusée » (saisie du motif) en demande ; afficher `getDernierRefusHomologation()` sur le brouillon.

## Résultat
