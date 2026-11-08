---
name: terminer-tache
description: Procédure obligatoire pour terminer toute tâche qui modifie le dépôt PingManager - mise à jour du README, synchronisation avec les dernières fusions de main, résolution des conflits, tests, Pull Request, fusion automatique et description pour Quentin. À utiliser à la fin de chaque session, avant de rendre la main.
---

# Terminer une tâche : le circuit automatique

Plusieurs sessions travaillent en parallèle. Pour que Quentin n'ait plus de
conflit à régler, chaque session **s'adapte elle-même aux fusions déjà
faites**, puis fusionne son travail et le décrit. Suis les étapes dans
l'ordre, sans en sauter.

Variables utilisées ci-dessous :
`DEPOT=1Cavalier/Gestion_Tournoi_FFTT`, `BRANCHE=$(git branch --show-current)`.
L'API GitHub passe par `gh api` (REST) ; `gh pr …` (GraphQL) ne marche pas
dans les sessions.

## 1. Mettre le README à jour

Le README est **le seul document** du projet. Modifications localisées :
- section 5 : retirer la tâche terminée (ou l'annoter « en attente de
  relecture : <raison> ») ; ajouter les défauts non bloquants découverts ;
- section 3 ou 4 : ce qui fonctionne désormais à l'écran, ou ce qui reste à
  brancher ;
- section 6 : les questions pour Quentin, avec le choix provisoire appliqué ;
- section 7 : un article FFTT nouvellement couvert ;
- section 1 : le nombre de tests.

Ne crée aucun autre fichier de documentation (pas de journal, de rapport, de
capture). Ce que tu as fait, ce qu'il faut tester et les passations vont
dans le **corps de la Pull Request** (étape 5).

Commite tout (`git status` doit être propre).

## 2. Se synchroniser avec main

```
git fetch origin main
git merge --no-edit origin/main
```

Toujours `merge`, jamais `rebase` ni `push --force` (la branche est peut-être
déjà poussée). En cas de conflit, pour chaque fichier :

| Fichier | Règle |
|---|---|
| `README.md` | Garder **les deux** contenus ; aucune ligne de main ne disparaît (sauf une tâche que main a retirée de la section 5 parce qu'elle est terminée). |
| `theme.css` | Garder les deux ; chaque session a sa section commentée en fin de fichier ; les variables en tête : prendre celles de main, puis rajouter les tiennes. |
| `ContexteApplication.java` (`VERSION_DEMO`) | Prendre la version de main, puis voir l'étape 3. |
| Code Java | Comprendre les deux changements (`git log origin/main -p -- <fichier>`) et écrire la version qui fait **les deux**. Jamais « prendre la mienne » en bloc. |
| Tests | Garder tous les tests des deux côtés. |
| Fichier entier (`git checkout --ours/--theirs`, « garder ma version ») | **Interdit** : c'est ce qui a effacé les styles de l'arbre et de la salle le 5 novembre. |

Commite la fusion, puis vérifie **obligatoirement** qu'aucune ligne ajoutée
par `main` n'a disparu de ta branche :

```
git diff origin/main HEAD -- . ':!README.md' | grep '^-[^-]'
```

Chaque ligne listée doit venir de **ton** travail (suppression voulue). Une
ligne de main perdue pendant la fusion se rétablit ; une suppression voulue
s'explique dans le corps de la PR (rubrique « Fusion »).

## 3. Version de la démo (seulement si tu as changé des données de démo)

Ne touche jamais `VERSION_DEMO` pendant le travail. Ici, après la
synchronisation : lis la valeur de main (`.demo-brunoy-vN`) et mets `N + 1`.
Une seule incrémentation par tâche.

## 4. Tester

`mvn -B test` (ou, en session cloud, la recette `javac` de `CLAUDE.md`)
doit passer à **0 échec** sur la branche synchronisée. Sinon, corrige (jamais
en supprimant, désactivant ou affaiblissant un test) et recommence à
l'étape 2. Note le nombre de tests dans la PR et dans le README.

## 5. Pousser et ouvrir la Pull Request

```
git push -u origin "$BRANCHE"
gh api repos/$DEPOT/pulls -f title="…" -f head="$BRANCHE" -f base=main -F body=@corps.md --jq .number
```

Corps de la PR, en français simple : **Pour Quentin** (ce qui change pour
l'utilisateur), **À tester à l'écran** (compte de démo, chemin), **Questions**
(reportées aussi dans la section 6 du README), **Passations** (comptes rendus
des agents), **Fusion** (écarts justifiés), **Tests** (nombre), puis les
lignes d'attribution demandées par la session. Si une PR existe déjà pour la branche
(`gh api "repos/$DEPOT/pulls?head=1Cavalier:$BRANCHE&state=open"`), réutilise-la.

## 6. Fusionner automatiquement

**Ne fusionne pas** et laisse la PR ouverte avec le titre préfixé
« [À relire] » si l'un de ces cas est vrai :
- la consigne demande une relecture (« relecture », « ne fusionne pas ») ;
- les tests n'ont pas pu s'exécuter dans la session ;
- `pom.xml` gagne une dépendance ;
- un test existant a été supprimé ou modifié dans son attendu ;
- tu as un doute sérieux sur la conformité à un règlement FFTT.

Sinon :
1. `git fetch origin main` ; si `origin/main` a bougé depuis l'étape 2
   (`git merge-base --is-ancestor origin/main HEAD` échoue), retourne à
   l'étape 2.
2. Les tests de l'étape 4 ont tourné sur **ce commit exact** (après la
   dernière synchronisation) : sinon, relance-les. Il n'y a plus
   d'intégration continue sur GitHub, les tests de la session font foi.
3. Fusionne exactement le commit validé :
   `gh api -X PUT repos/$DEPOT/pulls/<n>/merge -f merge_method=merge -f sha=$(git rev-parse HEAD)`
4. Réponse 405 ou 409 (main a bougé, ou conflit) : retourne à l'étape 2.
   Au bout de 3 tentatives, laisse la PR ouverte « [À relire] » et explique
   pourquoi dans la PR.
5. Branche distante : ne la supprime pas toi-même (les sessions n'en ont pas
   le droit : refus 403). GitHub la supprime après la fusion si le réglage
   « Automatically delete head branches » est activé ; sinon elle reste,
   sans gêne pour le circuit.

## 7. Décrire ce qui a été fait

1. Commentaire sur la PR (`gh api repos/$DEPOT/issues/<n>/comments -f body=…`)
   avec la rubrique « Pour Quentin » de la PR.
2. Ta **réponse finale** dans la session reprend cette même description, en
   français simple :
   - ce qui a changé pour l'utilisateur ;
   - ce qu'il faut tester à l'écran, et comment (`git pull` puis
     `mvn clean javafx:run`, compte de démo, chemin) ;
   - les questions posées ;
   - « Fusionné dans main » ou « En attente de relecture : <raison> ».
