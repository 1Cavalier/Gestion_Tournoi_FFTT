# B010 : règlement en lecture seule après publication — 2026-11-05

- Branche : `tache/B010-reglement-apres-publication`
- Agent : developpeur, verificateur-modele, correcteur, relecteur-code, documentaliste
- Pull Request : à créer (étape `integrateur`)
- Statut : en attente de validation visuelle (l'écran n'a pas pu être vu en session)

## Pour Quentin

Une fois un tournoi **publié**, son règlement et sa feuille d'engagement sont
figés : ils restent tels qu'à la publication, même si un horaire change ensuite.
Ils ne peuvent plus être modifiés, ni regénérés, ni redéposés. Le cœur refuse
ces changements, pas seulement l'écran.

À l'écran, la grande carte du règlement disparaît de la vue d'ensemble. Il reste
un simple lien « Règlement », qui ouvre l'aperçu en lecture et permet de
télécharger le PDF. Avant publication, rien ne change.

## À tester à l'écran

- Compte de démo : Brunoy (rien à recréer, la démo est inchangée). Le « Tournoi
  National B de Brunoy 2027 » de la démo est **déjà publié** (`TournoisDemo`,
  n° d'homologation fictif) : sa copie figée est créée à sa première ouverture.
- Chemin, tournoi publié : Gestion des tournois → Tournoi National B de Brunoy 2027
  → vue d'ensemble, puis onglet Complémentaire.
  Attendu : plus de grande carte « Règlement » ; un lien « Règlement » à la place,
  qui ouvre l'aperçu en lecture seule et propose le téléchargement PDF.
- Chemin, fiche : onglet Tournois → fiche du tournoi publié → télécharger le
  règlement (PDF). Attendu : le PDF se télécharge.
- Chemin, brouillon : même tournoi avant publication, ou un brouillon de démo.
  Attendu : rien n'a changé (carte complète, modification possible).
- Ce qu'il faut regarder (thème clair et sombre) : lisibilité du lien « Règlement »,
  du bandeau « figés depuis la publication » et de la page en lecture seule.
  L'écran `VueReglementFige` n'a pas pu être vu en session cloud.

## Questions pour Quentin

### 2026-11-05 — Tournoi publié puis annulé
- Contexte : une fois publié, le règlement est figé. Si le tournoi est ensuite
  annulé, la garde ne s'applique plus (elle vaut seulement pour l'état « publié »).
  Le règlement redevient donc modifiable, et `reglement()` régénère le texte en
  direct. La copie figée est conservée mais ignorée.
- Article FFTT : aucun (choix de l'application, pas une règle sportive).
- Options : A) garder le règlement figé même après annulation (sécurité, mais
  un tournoi annulé ne peut pas être corrigé) ; B) rendre modifiable après
  annulation (comportement actuel, plus souple).
- Choix provisoire de l'agent : B, le comportement actuel, qui est le plus
  réversible.
- Réponse de Quentin : à compléter.

## Notes pour l'état du projet

- Classes principales : `model.DocumentReglement` (déplacé depuis `core.reglement`),
  `model.Tournoi` (garde `reglementModifiable()`, `MOTIF_REGLEMENT_FIGE`, copies
  figées posées par `ServiceCycleTournoi.publier`),
  `core.reglement.ServiceReglementTournoi` (figer, reglement, feuilleEngagement,
  modifierOptions, deposer), `core.reglement.ResultatModificationReglement`,
  `persistence.DocumentReglementMapper`, `ui.ecran.VueReglementFige`.
- Format de sauvegarde : deux champs facultatifs dans le JSON du tournoi,
  `reglementFige` et `feuilleEngagementFigee` (copies du PDF, encodées par
  `DocumentReglementMapper`). Un fichier publié sans copie les reçoit à la
  relecture. Une copie illisible est ignorée et régénérée à partir des données
  actuelles (elle peut donc différer de l'original).
  Anciens fichiers relus sans changement (`ReglementFigeRoundTripTest`).
- Démo : inchangée, pas de `.demo-brunoy-vN` à incrémenter.

## Suites proposées

- Repérage de la ligne « Règlement » dans `EcranCompletion` : aujourd'hui il se
  fait par le libellé, ce qui est fragile. Le rendre robuste (constante partagée
  ou test qui vérifie le libellé).
- Retirer la classe CSS `page-reglement-fige` : elle est utilisée mais n'est pas
  définie dans `theme.css`.
- Garder `setFeuilleEngagement` et `setInfosReglement` après publication (ils ne
  sont pas encore gardés ; l'ancienne feuille papier et les infos du règlement
  peuvent encore être modifiés).

## Fusion

À compléter par l'`integrateur` (`python3 outils-dev/verifier-fusion.py`).

## Chiffres

- Tests : 611 → 643 (GitHub Actions : 643 tests, 0 échec ; avant = nombre d'annotations `@Test` sur `origin/main`, mesuré par comptage)
- Version de la démo : inchangée
