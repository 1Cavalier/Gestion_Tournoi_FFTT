# Tâches prêtes à confier à Claude (en ton absence)

Comment s'en servir : sur claude.ai/code, choisis le dépôt
`Gestion_Tournoi_FFTT`, copie-colle **une** tâche ci-dessous, puis pars.
Au retour, relis la Pull Request, teste chez toi (`mvn javafx:run` pour les
écrans), réponds dans `QUESTIONS.md` si besoin, puis fusionne.

Règle : une tâche = une Pull Request. Les tâches 1 à 3 sont sans risque (tests
et audits) ; commence par elles pour juger la qualité du travail.

---

## 1. Audit d'architecture (≈ 15 min, lecture seule)

> Utilise l'agent `gardien-architecture` pour faire l'audit complet du dépôt.
> Commite `RAPPORT_ARCHITECTURE.md` sur une branche `audit/architecture` et
> ouvre une Pull Request avec les trois actions les plus utiles.

## 2. Tests du cœur sportif (≈ 1 h)

> Utilise l'agent `testeur-junit` sur les packages `core` et `model` :
> repère les méthodes publiques et les motifs de refus des `Resultat…` sans
> test, en commençant par `ServiceInscription`, `ServicePointage`,
> `PlacementQualifiesService` et `ClassementPouleService`. Ajoute les tests
> manquants (cas limites : effectif non rond, ex æquo, forfait, capacité
> atteinte). Ne modifie pas le code de production ; consigne les bugs
> suspectés dans `RAPPORT_BUGS.md`. Branche `tests/coeur-sportif`, Pull Request
> avec le nombre de tests avant/après.

## 3. Audit de conformité I.301 à I.305 (≈ 1 h)

> Utilise l'agent `controleur-conformite` pour auditer les articles I.301 à
> I.305 des règlements sportifs 2026-2027 contre le code et les tests. Mets à
> jour `CONFORMITE_FFTT.md`, note les ambiguïtés dans `QUESTIONS.md`, ajoute
> les tests qui démontrent un écart. Branche `audit/conformite-I30x`, Pull
> Request.

## 4. J-1 : tableau de bord de la veille, côté cœur (≈ 2 h)

> Utilise l'agent `developpeur`. Crée dans `core` un service
> `TableauDeBordJ1` qui, pour un tournoi, donne par tableau : inscrits, liste
> d'attente, payés, horaires et pointage, et une liste d'alertes (tableau sans
> horaire, sous le seuil de maintien de `ConditionsMaintien`, juge-arbitre
> principal non désigné, joueur sans club ou sans points). Uniquement le cœur
> et ses tests, pas d'écran. Branche `j-1/tableau-de-bord-coeur`, Pull Request.

## 5. J-1 : listes de pointage en PDF (≈ 2 h)

> Utilise l'agent `developpeur`. Génère une liste de pointage par tableau
> (joueurs triés par nom : nom, prénom, licence, club, points, cases
> « pointé » et « payé », montant à régler), sur le modèle de
> `GenerateurFeuilleEngagement` et du rendu PDF du règlement. Ajoute un bouton
> dans la vue Inscriptions du club. Tests sur le contenu généré. Branche
> `j-1/listes-pointage`, Pull Request avec les écrans à vérifier.

## 6. J-1 : mise à jour des points avant le tirage (≈ 2 h)

> Utilise l'agent `developpeur`. Permets d'importer un fichier de points
> (licence + points, formats déjà lus par `importation.LecteurTableur`) pour
> mettre à jour les points des inscrits d'un tournoi avant le lancement,
> avec un aperçu des changements (joueur, anciens points, nouveaux points,
> licences inconnues) avant application. Cœur + tests d'abord, écran ensuite.
> Branche `j-1/mise-a-jour-points`, Pull Request.

---

## Tâches régulières (à planifier)

- **Chaque nuit** : tâche 2 sur le package le moins couvert.
- **Chaque lundi** : tâche 1, pour repérer tôt les écarts à la règle d'or.
