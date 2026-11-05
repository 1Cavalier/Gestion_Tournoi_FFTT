---
id: B033
titre: Gestion des tournois : tournoi actuel, frise verticale des étapes, gros bouton de création
type: interface
priorite: P1
taille: M
statut: en-cours
depend-de: []
fichiers: [ui/ecran/PageGestionTournoi]
branche: tache/B033-gestion-tournois
date: 2026-11-05
pr:
---

## Ce qu'il faut faire

Référence : `docs/CAHIER_DES_CHARGES_V0.md`, section 4. En haut **« Tournoi actuel »** (vide ou le tournoi en cours, accès au jour J). En dessous une **frise chronologique verticale** : étape 4 Publié, étape 3 Homologué validé, étape 2 Demande d'homologation en cours, étape 1 Brouillon, chaque étape avec ses tournois. **Gros bouton** bien visible « Créer un tournoi ». Lien vers l'**historique** des tournois passés du club.

## Critères de fin

- [ ] frise verticale et tournoi actuel
- [ ] bouton de création très visible
- [ ] captures regardées
- [ ] `mvn -B test` à 0 échec

## Contraintes / décisions de Quentin

Voir le cahier des charges v0.

## Passations

- developpeur-interface : accueil extrait dans `AccueilGestionTournois` (vue) + `PresentationGestionTournois` (10 tests) ; `PageGestionTournoi` ne fait plus que déléguer (tuiles chiffrées retirées, « Tournoi actuel » en haut). Frise 4→1 calée sur `StatutTournoi` (aucune étape manquante), gros bouton « Créer un tournoi », historique (annulés + homologués/publiés dont le dernier jour est passé) affiché dans la page, l'entrée « Historique » de la `Coquille` reste un espace réservé. CSS section « B033 » en fin de theme.css ; captures non regardées (pas d'affichage en session cloud).

## Résultat
