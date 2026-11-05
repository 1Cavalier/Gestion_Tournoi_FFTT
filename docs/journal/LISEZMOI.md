# Journal des tâches

Chaque session Claude Code écrit ici **un fichier par tâche**
(`AAAA-MM-JJ-<branche>.md`, modèle : `MODELE.md`) : ce qu'elle a fait, ce
qu'il faut tester, ses questions et ses notes pour l'état du projet.

Pourquoi : quand toutes les sessions écrivaient dans `QUESTIONS.md` et
`ETAT_DU_PROJET.md`, chaque fusion provoquait des conflits, et des entrées
ont déjà été perdues. Un fichier par tâche n'entre jamais en conflit.

L'agent `recapitulateur` reporte les journaux dans `QUESTIONS.md`,
`ETAT_DU_PROJET.md` et `RECAPITULATIF.md`, puis déplace les journaux
reportés dans `archives/`. Les journaux non encore déplacés sont donc ceux à
lire pour savoir ce qui vient d'être fait.
