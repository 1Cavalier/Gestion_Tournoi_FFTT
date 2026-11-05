---
name: terminer-tache
description: Procédure obligatoire pour terminer toute tâche qui modifie le dépôt PingManager - journal de la tâche, synchronisation avec les dernières fusions de main, résolution des conflits, tests, Pull Request, fusion automatique et description pour Quentin. À utiliser à la fin de chaque session, avant de rendre la main.
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

## 1. Écrire le journal de la tâche

Crée `docs/journal/AAAA-MM-JJ-<branche>.md` (date du jour, `/` du nom de
branche remplacé par `-`) en suivant `docs/journal/MODELE.md`. C'est **le seul
endroit** où la session écrit ce qu'elle a fait, ses questions et ses notes
pour l'état du projet : un fichier par tâche ne provoque jamais de conflit.

**Ne modifie pas** `QUESTIONS.md`, `ETAT_DU_PROJET.md` ni `RECAPITULATIF.md`
(l'agent `recapitulateur` y reporte les journaux), sauf si la consigne le
demande explicitement.

Si la tâche vient du carnet (`docs/backlog/B…md`), mets à jour **son**
fichier : `statut` (`fait`, `a-relire` ou `bloquee` avec la raison), `pr`, et
la section « Résultat ». Un fichier par tâche : pas de conflit possible.

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
| `QUESTIONS.md`, `ETAT_DU_PROJET.md`, `RECAPITULATIF.md`, `RAPPORT_*.md`, `docs/**/*.md` | Garder **les deux** contenus ; aucune ligne de main ne disparaît. |
| `theme.css` | Garder les deux ; chaque session a sa section commentée en fin de fichier ; les variables en tête : prendre celles de main, puis rajouter les tiennes. |
| `ContexteApplication.java` (`VERSION_DEMO`) | Prendre la version de main, puis voir l'étape 3. |
| Code Java | Comprendre les deux changements (`git log origin/main -p -- <fichier>`) et écrire la version qui fait **les deux**. Jamais « prendre la mienne » en bloc. |
| Tests | Garder tous les tests des deux côtés. |
| Fichier entier (`git checkout --ours/--theirs`, « garder ma version ») | **Interdit** : c'est ce qui a effacé les styles de l'arbre et de la salle le 5 novembre. |

Commite la fusion, puis lance **obligatoirement** :

```
python3 outils-dev/verifier-fusion.py
```

Il liste toute ligne que `main` a ajoutée et qui a disparu de ta branche. Pour
chaque ligne listée : soit tu la rétablis, soit tu l'as remplacée volontairement
et tu l'expliques dans le journal (section « Fusion »). Ne passe à l'étape 3
que lorsque le script répond OK ou que chaque écart est justifié.

## 3. Version de la démo (seulement si tu as changé des données de démo)

Ne touche jamais `VERSION_DEMO` pendant le travail. Ici, après la
synchronisation : lis la valeur de main (`.demo-brunoy-vN`) et mets `N + 1`.
Une seule incrémentation par tâche.

## 4. Tester

`mvn -B test` doit passer à **0 échec** sur la branche synchronisée. Sinon,
corrige (jamais en supprimant, désactivant ou affaiblissant un test) et
recommence à l'étape 2. Note le nombre de tests dans le journal.

## 5. Pousser et ouvrir la Pull Request

```
git push -u origin "$BRANCHE"
gh api repos/$DEPOT/pulls -f title="…" -f head="$BRANCHE" -f base=main -F body=@corps.md --jq .number
```

Corps de la PR : le contenu du journal + les lignes d'attribution demandées
par la session. Si une PR existe déjà pour la branche
(`gh api "repos/$DEPOT/pulls?head=1Cavalier:$BRANCHE&state=open"`), réutilise-la.

## 6. Fusionner automatiquement

**Ne fusionne pas** et laisse la PR ouverte avec le titre préfixé
« [À relire] » si l'un de ces cas est vrai :
- la consigne demande une relecture (« relecture », « ne fusionne pas ») ;
- `mvn -B test` n'a pas pu s'exécuter ni dans la session ni sur GitHub
  Actions ;
- `pom.xml` gagne une dépendance, ou `.github/` est modifié ;
- un test existant a été supprimé ou modifié dans son attendu ;
- tu as un doute sérieux sur la conformité à un règlement FFTT.

Sinon :
1. `git fetch origin main` ; si `origin/main` a bougé depuis l'étape 2
   (`git merge-base --is-ancestor origin/main HEAD` échoue), retourne à
   l'étape 2.
2. **Attends le verdict de GitHub Actions sur ce commit exact**, même si
   `mvn -B test` est passé dans la session (c'est le seul juge commun à toutes
   les sessions) :
   `gh api repos/$DEPOT/commits/$(git rev-parse HEAD)/check-runs --jq '.check_runs[] | select(.name=="tests") | "\(.status) \(.conclusion)"'`
   toutes les 30 s environ (avec l'outil de surveillance de la session, pas
   `sleep` au premier plan), 20 minutes au plus.
   - `completed success` : continue.
   - `completed failure` : lis le journal du job
     (`gh api repos/$DEPOT/actions/jobs/<id>/logs`), corrige, recommence à
     l'étape 2.
   - pas de verdict au bout de 20 minutes : ne fusionne pas, PR « [À relire] ».
3. Fusionne exactement le commit validé :
   `gh api -X PUT repos/$DEPOT/pulls/<n>/merge -f merge_method=merge -f sha=$(git rev-parse HEAD)`
4. Réponse 405 ou 409 (main a bougé, ou conflit) : retourne à l'étape 2.
   Au bout de 3 tentatives, laisse la PR ouverte « [À relire] » et explique
   pourquoi dans le journal.
5. Branche distante : ne la supprime pas toi-même (les sessions n'en ont pas
   le droit : refus 403). GitHub la supprime après la fusion si le réglage
   « Automatically delete head branches » est activé ; sinon elle reste,
   sans gêne pour le circuit.

## 7. Décrire ce qui a été fait

1. Commentaire sur la PR (`gh api repos/$DEPOT/issues/<n>/comments -f body=…`)
   avec la section « Pour Quentin » du journal.
2. Ta **réponse finale** dans la session reprend cette même description, en
   français simple :
   - ce qui a changé pour l'utilisateur ;
   - ce qu'il faut tester à l'écran, et comment (`git pull` ou
     `outils-dev\mettre-a-jour.cmd`, compte de démo, chemin) ;
   - les questions posées ;
   - « Fusionné dans main » ou « En attente de relecture : <raison> ».
