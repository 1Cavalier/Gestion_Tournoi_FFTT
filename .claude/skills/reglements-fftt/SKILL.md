---
name: reglements-fftt
description: Vérifier ou implémenter une règle sportive FFTT (poules, classement, placement, tableau, inscriptions, catégories d'âge, juge-arbitre, cartons, homologation). À utiliser dès qu'une tâche cite un article (I.301, IX.104, 3.5.2…) ou touche au déroulé sportif d'un tournoi.
---

# Règlements FFTT 2026-2027 dans PingManager

## Sources officielles (seules valables)

Les **règlements sportifs** (chapitres I épreuves, poules, tableaux ; II
catégories ; IX tournois…) et les **règlements généraux** FFTT 2026-2027.
Ils ne sont plus versionnés dans le dépôt : Quentin les fournit (PDF) quand
une règle doit être vérifiée. Sans le texte sous les yeux, ne pas
interpréter de mémoire : appliquer l'option la plus prudente et ajouter la
question dans la section 6 du README.

## Où chaque règle vit dans le code

| Article | Sujet | Code |
|---|---|---|
| I.301 | Ordre des parties en poule (3/4/5/6), anti-arrangement | `core.GenerateurMatchsPoule` |
| I.302 | Placement en poule, serpentin, séparation des clubs, ex æquo | `core.GenerateurPoules` |
| I.303 | Classement 2/1/0, issue non normale = 0, départages | `core.ClassementPouleService` |
| I.304 / I.305 | Placement tableau, têtes de série par blocs, 2es/3es opposés | `core.PlacementQualifiesService`, `core.GenerateurTableauElimination` |
| IX.101 / IX.102 | Accès selon la catégorie du tournoi, licence loisir | `core.EligibiliteTournoi`, `core.Territoires` |
| IX.103 / IX.105 | Échelon d'homologation | `model.EchelonHomologation` |
| IX.104 | Dossier d'homologation, tableau dames, grade du JA | `core.ServiceCycleTournoi`, `core.ServiceCompletudeTournoi` |
| IX.109 | JA ne joue pas, tirage public | `ServiceLancementTableau` (graine), écrans JA |
| IX.111 | Contenu du règlement du tournoi | `model.InfosReglement`, `core.reglement.GenerateurReglement` |
| II.302 / II.303 | Catégorie d'âge (âge au 1er janvier de la saison) | `model.CategorieAge`, `core.ServiceInscription` |
| 3.5.2 | Cartons, pénalités, 2 disqualifications = exclusion | `core.GestionCartonsMatch`, `core.RegistreDisciplinaire` |

Vérifier l'emplacement avec Glob avant de s'y fier : le code évolue.
L'état article par article est tenu dans la section 7 du README.

## Méthode

1. Trouver l'article dans le texte officiel et le citer (numéro + résumé).
2. Lire le code concerné et ses tests existants.
3. Écrire d'abord un test qui exprime la règle (cas nominal + cas limites :
   effectif non rond, ex æquo, forfait, club unique…).
4. Implémenter, puis `mvn -B test`.
5. Mettre à jour la section 7 du README si la conformité d'un article change.

## Réserves connues (ne pas « corriger » sans décision de Quentin)

- I.303.e : tirage au sort en cas d'égalité vraiment persistante laissé à
  l'organisateur.
- Séparation des clubs au-delà du 2e tour non contrainte (non exigée).
- Échelon d'homologation d'un tournoi promotionnel : rattaché à la ligue (IX.105),
  à confirmer.
- Joueurs étrangers hors tournoi international (IX.102, territoires contigus) :
  à confirmer.

## Règle fédérale ou clause d'organisateur ?

Les quotas de tableaux par jour, « pas deux tableaux qui se suivent », le tableau
dames obligatoire, les catégories non admises et la politique de forfaits sont
des **choix du club**, configurables, pas des règles fédérales. Le préciser dans
le code et ne jamais les imposer par défaut.
