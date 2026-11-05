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

- **developpeur (cœur + écran), 2026-11-05** : `DocumentReglement` déplacé dans `model` ; `Tournoi` garde
  `reglementFige` / `feuilleEngagementFigee` (figées par `ServiceCycleTournoi.publier`), setters des options
  et du fichier déposé refusés (`IllegalStateException`, `Tournoi.MOTIF_REGLEMENT_FIGE`) une fois publié ;
  `core.reglement.ServiceReglementTournoi` (figer, reglement, feuilleEngagement, modifierOptions / deposer →
  `ResultatModificationReglement`). JSON : champs facultatifs (`DocumentReglementMapper`), ancien fichier
  publié figé à la relecture. Écran : `VueReglementFige` (lien compact + page en lecture seule), branchée dans
  `EcranComplementaire`, `EcranCompletion` (ligne « Règlement » de la carte Complémentaire) et `FicheTournoi`.
  Choix à valider : un tournoi publié puis **annulé** redevient modifiable (garde limitée à `PUBLIE`).
- **verificateur-modele, 2026-11-05** : 16 tests ajoutés (`ReglementFigeContournementTest`, `ReglementFigeCorrompuTest`,
  `model/RegleOrModeleSansDependanceTest`) : immuabilité des copies, figé indépendant des données vivantes, copie JSON corrompue, règle d'or.
  Publié puis annulé : `reglementModifiable()` vrai, `estFige` faux, donc options/dépôt à nouveau acceptés et `reglement()` régénère en direct (copie ignorée mais conservée) : question pour Quentin.
  Mineur : `setFeuilleEngagement` (ancienne feuille papier) et `setInfosReglement` ne sont pas gardés.
- **correcteur, 2026-11-05** : `DocumentReglementMapper.lire` attrape `JsonException` (seulement sur la copie figée) et rend `null`,
  la copie est régénérée par `figerSiPublieSansCopie` ; les 3 tests rouges du vérificateur passent. Mineurs non traités.

## Résultat

- Fait : après publication, règlement et feuille d'engagement figés (copies dans le JSON du tournoi, rétrocompatible), modification refusée par le cœur (`IllegalStateException`, `Tournoi.MOTIF_REGLEMENT_FIGE`) ; à l'écran, lien « Règlement » (aperçu en lecture et PDF) à la place de la grande carte (`VueReglementFige`). Avant publication, inchangé.
- Critères : refus par le cœur testé (`ReglementFigeContournementTest`, `ServiceReglementTournoiTest`) ; copie figée toujours rendue après publication (`ServiceReglementTournoiTest`, côté cœur ; l'écran reste à voir) ; `mvn -B test` vert (643 tests, GitHub Actions).
- Non vérifié : la vue allégée n'a pas pu être vue à l'écran en session cloud (captures avant / après à faire par Quentin).
- Question ouverte : tournoi publié puis annulé, redevient modifiable (voir journal).
- Suites : repérage fragile de la ligne « Règlement » dans `EcranCompletion`, classe CSS `page-reglement-fige` non définie, `setFeuilleEngagement` / `setInfosReglement` non gardés après publication.
- Journal : `docs/journal/2026-11-05-tache-B010-reglement-apres-publication.md`.
