# Carnet de tâches (backlog)

C'est la liste de travail que les agents traitent **sans Quentin** (skill
`piloter-projet`). **Un fichier par tâche** : deux sessions ne modifient
jamais le même fichier, donc pas de conflit.

- Nom : `B<numéro sur 3 chiffres>-<mot-cle>.md` (ex. `B012-listes-pointage.md`).
- Modèle : `MODELE.md`.
- Une tâche prise par une session a une branche distante `tache/<id>-…` :
  c'est la réservation.

## Statuts

| Statut | Sens |
|---|---|
| `a-faire` | prête à être prise |
| `en-cours` | une session y travaille (branche indiquée) |
| `bloquee` | attend une décision ou une autre tâche (raison indiquée) |
| `a-relire` | Pull Request ouverte, attend Quentin (raison indiquée) |
| `fait` | fusionnée (numéro de PR indiqué) |

## Types et chaîne d'agents

`idee`, `jour-j`, `interface`, `charte`, `bug`, `regle`, `recherche`, `doc` :
voir le tableau de la skill `.claude/skills/piloter-projet`.

## Ajouter une tâche (Quentin)

Copier `MODELE.md`, prendre le numéro suivant, remplir l'en-tête et
« Ce qu'il faut faire » en quelques lignes, puis commit sur `main` (ou le
demander à une session : « ajoute au carnet : … »). Plus la description est
précise (ce que voit l'utilisateur, critère de fin), meilleur est le
résultat et moins la session consomme.
