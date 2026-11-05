---
id: B038
titre: Saisie du score en grille et cartons
type: jour-j
priorite: P1
taille: M
statut: a-faire
depend-de: [B037]
fichiers: [ui/ecran/EditeurScorePhaseFinale, ui/ecran (saisie des poules), core/GestionCartonsMatch]
branche:
pr:
---

## Ce qu'il faut faire

Référence : `docs/CAHIER_DES_CHARGES_V0.md`, section 8. Saisie du score dans un **tableau** (une case par manche et par joueur, Tab pour avancer, Entrée pour valider), plus de « 11-9 » dans une cellule ; même composant pour les poules et le tableau final ; issues anormales conservées ; **espace cartons** (jaune, jaune-rouge, rouge, par joueur) branché sur `GestionCartonsMatch` / `RegistreDisciplinaire` (3.5.2).

## Critères de fin

- [ ] même saisie en poule et en tableau final
- [ ] cartons enregistrés et persistés (testé)
- [ ] captures regardées
- [ ] `mvn -B test` à 0 échec

## Contraintes / décisions de Quentin

Voir le cahier des charges v0.

## Passations

## Résultat
