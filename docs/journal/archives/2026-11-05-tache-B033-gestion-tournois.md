# Gestion des tournois : tournoi actuel, frise verticale, gros bouton (B033) — 2026-11-05

- Branche : `tache/B033-gestion-tournois`
- Agent : `developpeur-interface` (accueil), `designer-interface` (frise, charte), `relecteur-code` (relecture), `documentaliste` (ce journal)
- Pull Request : à venir (étape 5, `integrateur`)
- Statut : en cours de chaîne (en attente de fusion par l'`integrateur`)

## Pour Quentin

La page « Gestion des tournois » change de visage. Tout en haut, la carte
**« Tournoi actuel »** : le tournoi dont le jour J a commencé, sinon celui
publié pour aujourd'hui, sinon le prochain tournoi publié (« Prochain tournoi »).
Elle donne accès au jour J. En dessous, une **frise verticale** en quatre
étapes, de haut en bas : Publié, Homologué validé, Demande d'homologation,
Brouillon. Chaque étape liste ses tournois. Un gros bouton **« Créer un
tournoi »** est bien visible. Les petites tuiles chiffrées ont disparu. Un lien
« Historique des tournois passés » ouvre la liste des tournois annulés et des
tournois passés (homologués ou publiés dont le dernier jour est dépassé).

## À tester à l'écran

- Compte de démo : Brunoy (compte habituel).
- Chemin : Gestion des tournois. Vérifier la carte « Tournoi actuel », puis la frise, puis le bouton « Créer un tournoi ». Cliquer « Historique des tournois passés ».
- Ce qu'il faut regarder (thème clair et sombre) :
  1. La carte « Tournoi actuel » affiche le bon tournoi (voir question 2 ci-dessous).
  2. La frise se lit de haut en bas, avec les quatre étapes dans cet ordre ; une étape vide reste discrète.
  3. Le bouton « Créer un tournoi » se voit du premier coup d'œil.
  4. L'historique liste les tournois passés et annulés.
- Captures : `docs/captures/B033/avant/` et `docs/captures/B033/apres/` (clair et sombre, démo Brunoy).
- Non vus (absents de la démo) : la carte « Tournoi actuel » quand il n'y a aucun tournoi, la page Historique elle-même, le rendu sous Windows et la mise à l'échelle. À regarder à l'écran.

## Questions pour Quentin

### 2026-11-05 — Brouillon ou demande dont la date est passée
- Contexte : un brouillon, ou une demande d'homologation, dont le dernier jour est passé reste aujourd'hui dans la frise, et n'entre pas dans l'historique (l'historique ne reprend que les tournois annulés et les tournois homologués ou publiés).
- Article FFTT : aucun (choix d'affichage).
- Options : A) rester dans la frise, pour qu'on voie qu'il faut le traiter (choix actuel) ; B) basculer dans l'historique dès que la date est passée.
- Choix provisoire de l'agent : A (on ne fait pas disparaître un tournoi non traité).
- Réponse de Quentin : en attente.

### 2026-11-05 — Tournoi actuel sans tournoi en cours
- Contexte : le cahier des charges (section 4) dit « vide ou le tournoi en cours ». Sans tournoi en cours ni publié aujourd'hui, la carte affiche aujourd'hui le prochain tournoi publié (« Prochain tournoi »).
- Article FFTT : aucun (choix d'affichage).
- Options : A) afficher le prochain tournoi publié (choix actuel) ; B) laisser la carte vide tant qu'aucun tournoi n'est en cours.
- Choix provisoire de l'agent : A (la carte reste utile en dehors du jour J).
- Réponse de Quentin : en attente.

## Notes pour l'état du projet

- Classes principales : `ui.ecran.AccueilGestionTournois` (vue : carte « Tournoi actuel », frise, bouton, historique), `ui.ecran.PresentationGestionTournois` (choix du tournoi actuel, étapes, libellés de l'historique, sans JavaFX, 10 tests). `ui.ecran.PageGestionTournoi` ne fait plus que déléguer.
- Frise : quatre étapes calées sur `StatutTournoi` (aucune étape manquante). Règles CSS en fin de `theme.css`, section B033.
- Entrée « Historique » de la `Coquille` : toujours un espace réservé ; le lien de l'accueil est fonctionnel.
- Défauts non bloquants (relecture) : classe CSS `carte-actuel-vide` sans règle ; quelques noms de classes Java pleinement qualifiés au lieu d'être importés.
- Format de sauvegarde : inchangé. Démo : inchangée.
- JavaFX non vérifié visuellement en session : validation à l'écran par Quentin.

## Fusion

Non faite : c'est l'`integrateur` qui synchronise avec `main` et lance `python3 outils-dev/verifier-fusion.py`.

## Chiffres

- Tests : 687 → 697 (nombre de `@Test` sur `main` et sur la branche ; `mvn -B -q test` code de sortie 0, 0 échec)
- Version de la démo : inchangée
