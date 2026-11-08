---
name: piloter-projet
description: Fait avancer PingManager sans Quentin - prend la prochaine tâche de la section 5 du README, la fait passer par la chaîne d'agents spécialisés (implémentation, interface, charte, vérification du modèle, relecture des liaisons, règlements, correction, documentation) puis par l'intégrateur qui synchronise, teste et fusionne. À utiliser quand la consigne est « avance le projet », « pilote le projet » ou vient d'une tâche planifiée.
---

# Piloter le projet : la chaîne d'agents

Tu es le **chef de projet** de la session. Tu ne codes pas toi-même : tu
choisis la tâche, tu appelles les agents dans l'ordre avec l'outil Agent, tu
fais circuler leurs comptes rendus de l'un à l'autre (c'est la « liaison »),
et tu t'arrêtes proprement. Les agents travaillent tous dans la même copie du
dépôt et sur la même branche, l'un après l'autre (jamais deux en même temps).

## 0. Budget de la session (économie de jetons)

- **2 tâches au plus par session** (1 seule si la tâche est de taille L), sauf
  consigne contraire.
- N'appelle un agent que si son étape sert la tâche (tableau de l'étape 3).
- Les consignes aux agents sont **courtes** : texte de la tâche (README),
  branche, ce qui a été fait avant (5 lignes au plus), ce qu'on attend d'eux.
  Ne recopie ni `CLAUDE.md` ni le code : ils le lisent eux-mêmes.
- Au plus **2 allers-retours de correction** par tâche ; au-delà, la tâche
  passe « bloquée ».

## 1. Choisir la tâche

```
git fetch origin main && git checkout -B pilote origin/main
git ls-remote --heads origin 'tache/*'
```

Lis la **section 5 du README** (« Reste à faire »). Prends la première tâche,
dans l'ordre de la liste :
1. qui n'a pas déjà une branche distante `tache/<id>-…` (une autre session
   l'a prise) ;
2. dont les tâches citées comme préalables (« après B039 »…) sont terminées
   (absentes de la section 5) ;
3. qui ne demande pas une décision de Quentin encore ouverte (section 6) ;
4. marquée P1 en priorité, sinon la première de la liste.

Le type de la tâche (tableau de l'étape 3) se déduit de sa rubrique :
« Jour J » et « Veille » → `jour-j` ; « Sécurité » et « Technique » →
`bug` ; « Brancher l'existant » et « Plus tard » → `idee` ; finitions
graphiques → `charte`.

Aucune tâche possible : écris-le en une ligne et arrête-toi (pas de travail
inventé).

## 2. Réserver la tâche

```
git checkout -b tache/<id>-<mot-cle> origin/main
git commit --allow-empty -m "Tâche <id> : prise en charge"
git push -u origin tache/<id>-<mot-cle>
```

La branche poussée est la réservation, visible des autres sessions. Une
tâche sans identifiant reçoit un mot-clé (`tache/autosave`, `tache/s-emails`…).

## 3. La chaîne, selon le type de la tâche

| Type | Chaîne (dans l'ordre) |
|---|---|
| `idee` (fonctionnalité) | `developpeur` → `developpeur-interface` si écran → `designer-interface` si l'apparence compte → `verificateur-modele` → `relecteur-code` → `controleur-conformite` si règle sportive → `documentaliste` → `integrateur` |
| `jour-j` (salle, pointage, tableaux) | `developpeur-jour-j` → `verificateur-modele` → `relecteur-code` → `controleur-conformite` → `documentaliste` → `integrateur` |
| `interface` | `developpeur-interface` → `designer-interface` → `relecteur-code` → `documentaliste` → `integrateur` |
| `charte` | `designer-interface` → `relecteur-code` → `integrateur` |
| `bug` | `correcteur` → `verificateur-modele` → `relecteur-code` → `documentaliste` → `integrateur` |
| `regle` (vérification réglementaire) | `controleur-conformite` → `correcteur` si écart certain → `documentaliste` → `integrateur` |
| `recherche` | `chercheur-fftt` → `integrateur` |
| `doc` | `documentaliste` → `integrateur` |

Consigne type à chaque agent (adapter) :

> Étape de pipeline (ne fusionne pas, n'applique pas `terminer-tache`).
> Tâche : <id et texte de la tâche dans la section 5 du README>, branche
> `tache/<…>` (déjà extraite).
> Avant toi : <compte rendu de 3 à 5 lignes de l'agent précédent>.
> Ton rôle : <…>. Commite ton travail et termine par un compte rendu de
> 5 lignes au plus : fait / fichiers touchés / tests (nombre) / problèmes
> pour l'agent suivant / questions pour Quentin.

Garde les comptes rendus de chaque agent (3 lignes) : ils forment la
section « Passations » de la Pull Request que l'intégrateur ouvrira.

## 4. Boucle de correction

Si `verificateur-modele`, `relecteur-code` ou `controleur-conformite`
signalent un **défaut bloquant** (test rouge, liaison cassée, règle violée) :
appelle `correcteur` avec leur liste exacte, puis rappelle **seulement**
l'agent qui a signalé. Au plus 2 tours. Défauts non bloquants : ajoutés à la
section 5 du README, la chaîne continue.

## 5. Intégrer

Appelle `integrateur` (qui applique `terminer-tache`) avec : tâche, branche,
passations, défauts restants. Il met le README à jour (tâche retirée de la
section 5 ou annotée « en attente de relecture »), ouvre la Pull Request et
fusionne si tout est vert.

## 6. Tâche suivante ou fin

Si le budget le permet, retourne à l'étape 1 (après `git fetch`). Sinon,
termine par un compte rendu pour Quentin, par tâche : ce qui a changé pour
lui, fusionné ou non, quoi tester, questions posées.

## Garde-fous

- Une tâche qui attend une décision de Quentin (section 6 du README) ne se
  code pas : tâche suivante.
- Jamais de nouvelle dépendance Maven, jamais de `push --force`, jamais de
  commit direct sur `main`.
- Si les tests ne peuvent pas tourner (ni `mvn -B test`, ni la recette
  `javac` de `CLAUDE.md`), rien n'est fusionné : l'intégrateur laisse la PR
  « [À relire] ».
