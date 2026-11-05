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

- [x] frise verticale et tournoi actuel
- [x] bouton de création très visible
- [x] captures regardées (démo Brunoy, clair et sombre ; voir Résultat pour ce qui reste non vu)
- [x] `mvn -B test` à 0 échec

## Contraintes / décisions de Quentin

Voir le cahier des charges v0.

## Passations

- developpeur-interface : accueil extrait dans `AccueilGestionTournois` (vue) + `PresentationGestionTournois` (10 tests) ; `PageGestionTournoi` ne fait plus que déléguer (tuiles chiffrées retirées, « Tournoi actuel » en haut). Frise 4→1 calée sur `StatutTournoi` (aucune étape manquante), gros bouton « Créer un tournoi », historique (annulés + homologués/publiés dont le dernier jour est passé) affiché dans la page, l'entrée « Historique » de la `Coquille` reste un espace réservé. CSS section « B033 » en fin de theme.css ; captures non regardées (pas d'affichage en session cloud).
- designer-interface : frise retouchée (pastille pleine + halo pour une étape remplie, cercle au trait pour une étape vide, trait vertical `-bordure-forte`, titre et mention « Aucun tournoi » discrets, tournois d'une étape dans un cadre léger) ; seule retouche Java : l'étape vide utilise un label `frise-vide` au lieu d'un `etatVideCompact`. Captures 1440 clair/sombre regardées (démo Brunoy) dans `docs/captures/B033/avant|apres/` ; bouton « Créer un tournoi » laissé tel quel (déjà très visible). Non vu : carte « Tournoi actuel » vide et page Historique (absentes de la démo), rendu Windows et mise à l'échelle.

- relecteur-code (5 novembre 2026, branche `a2f3a7e`) : aucun défaut bloquant. Non bloquants : la classe CSS `carte-actuel-vide` (carte « Tournoi actuel » sans tournoi) n'a aucune règle dans `theme.css` ; quelques noms de classes Java sont pleinement qualifiés au lieu d'être importés. Script de captures corrigé par le relecteur (commit a2f3a7e). Non vérifié : la carte « Tournoi actuel » vide et la page Historique (absentes de la démo). `mvn -B -q test` relancé pour ce compte rendu : code de sortie 0.

## Résultat

- Accueil « Gestion des tournois » refait : « Tournoi actuel » en haut, frise verticale en 4 étapes (Publié, Homologué validé, Demande d'homologation, Brouillon), gros bouton « Créer un tournoi », lien vers l'historique. Les tuiles chiffrées sont retirées.
- Code : `AccueilGestionTournois` (vue), `PresentationGestionTournois` (logique, 10 tests), `PageGestionTournoi` ne fait plus que déléguer.
- Captures : `docs/captures/B033/avant/` et `docs/captures/B033/apres/` (démo Brunoy, clair et sombre).
- Non vu à l'écran : carte « Tournoi actuel » vide, page Historique, rendu Windows et mise à l'échelle.
- Questions ouvertes pour Quentin : voir le journal `docs/journal/2026-11-05-tache-B033-gestion-tournois.md`.
- Tests : `mvn -B test` à 0 échec.
