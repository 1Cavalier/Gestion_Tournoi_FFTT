---
id: B010
titre: Règlement en lecture seule après publication
type: idee
priorite: P1
taille: S
statut: en-cours
depend-de: []
fichiers: [ui/ecran/EcranComplementaire, ui/ecran/EcranCompletion, core/ServiceCycleTournoi]
branche: tache/B010-reglement-apres-publication (prise le 2026-11-05)
pr:
---

## Ce qu'il faut faire

Une fois le tournoi **publié**, le règlement n'est plus une étape de travail :
- il ne peut **plus être modifié** (ni généré autrement, ni redéposé, ni ses articles changés) : refus dans le cœur (motif clair), pas seulement à l'écran ;
- à l'écran, la grande carte du règlement disparaît de la vue d'ensemble ; il reste un simple onglet ou lien « Règlement » qui ouvre l'aperçu en lecture et le téléchargement PDF ;
- même chose pour la feuille d'engagement.
Avant publication, rien ne change.

## Critères de fin

- [ ] modification refusée par le cœur sur un tournoi publié (test)
- [ ] aperçu et PDF toujours accessibles
- [ ] vue d'ensemble allégée après publication (captures avant / après)
- [ ] `mvn -B test` à 0 échec

## Contraintes / décisions de Quentin

Aucune pour l'instant.

## Passations

## Résultat
