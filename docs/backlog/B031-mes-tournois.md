---
id: B031
titre: Mes tournois : tournoi en cours, inscriptions et agenda, tournois passés
type: interface
priorite: P1
taille: M
statut: a-faire
depend-de: [B032]
fichiers: [ui/ecran/MesTournois]
branche:
pr:
---

## Ce qu'il faut faire

Référence : `docs/CAHIER_DES_CHARGES_V0.md`, section 3. En premier le **tournoi en cours** avec accès direct au suivi (B027). Puis **mes inscriptions** triées par date et l'**agenda** (B032 : dates personnelles, export .ics). Puis les **tournois passés** avec mes **performances** (victoires, défaites) ; un clic rouvre ma poule, les scores, le tableau principal (lecture seule, réutiliser `EcranSuiviTournoi`). **Retirer** la partie « organisés par le club ».

## Critères de fin

- [ ] trois parties dans l'ordre
- [ ] performances calculées (testé)
- [ ] captures regardées
- [ ] `mvn -B test` à 0 échec

## Contraintes / décisions de Quentin

Voir le cahier des charges v0.

## Passations

## Résultat
