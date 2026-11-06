# Agenda personnel, ajout automatique, export .ics — 2026-11-05

- Branche : `tache/B032-agenda`
- Agent : developpeur, verificateur-modele, correcteur, relecteur-code, documentaliste
- Pull Request : à créer (étape `integrateur`)
- Statut : en attente de fusion (étape `integrateur`) ; aucun écran à valider pour cette tâche

## Pour Quentin

L'agenda de chaque compte existe désormais, sans écran pour l'instant. Quand
un joueur s'inscrit en ligne à un tournoi, la date du tournoi s'ajoute toute
seule à son agenda ; s'il se désinscrit complètement, elle se retire.
Il peut aussi enregistrer ses propres dates (titre, date, heure facultative, note).
Un export au format `.ics` est prévu : le fichier s'ouvre dans Google Agenda,
Outlook ou Apple. Les tournois déjà en place et les fichiers existants ne changent pas.
Les écrans Accueil et Mes tournois, qui affichent cet agenda, viendront dans leurs tâches.

## À tester à l'écran

- Compte de démo : aucun (pas d'écran dans cette tâche).
- Chemin : aucun pour l'instant.
- Ce qu'il faut regarder (thème clair et sombre) : rien à l'écran. La validation
  viendra avec les tâches Accueil et Mes tournois.

## Questions pour Quentin

### 2026-11-05 — Liste d'attente dans l'agenda
- Contexte : un joueur qui n'est qu'en liste d'attente sur un tableau est-il
  inscrit au tournoi pour son agenda ?
- Article FFTT : aucun (choix d'interface, pas une règle sportive).
- Options : A) oui, la date reste dans l'agenda avec la mention « Liste d'attente » ;
  B) non, la date n'apparaît qu'après une inscription confirmée.
- Choix provisoire de l'agent : A (option prudente : le joueur ne rate pas la date,
  la mention évite la confusion ; réversible en une ligne).
- Réponse de Quentin :

### 2026-11-05 — Délai de désinscription en ligne
- Contexte : jusqu'à quand un joueur peut-il se désinscrire lui-même en ligne ?
- Article FFTT : non vérifié dans `docs/reglements/` pour cette tâche (à confirmer avant de fixer une règle).
- Options : A) tant que le tableau est en phase d'inscription, sans date limite
  ni remboursement ; B) jusqu'à une date limite fixée par l'organisateur.
- Choix provisoire de l'agent : A (aucun délai inventé ; la désinscription n'est
  possible qu'en phase d'inscription, le reste relève de l'organisateur).
- Réponse de Quentin :

### 2026-11-05 — Retrait manuel d'un tournoi de l'agenda
- Contexte : un joueur peut-il retirer lui-même une date de tournoi de son agenda ?
- Article FFTT : aucun (choix d'interface).
- Options : A) non, la date suit l'inscription (elle se retire seulement à la
  désinscription) ; B) oui, le joueur peut la masquer à la main.
- Choix provisoire de l'agent : A (une date automatique ne se modifie pas à la main,
  pour éviter un agenda qui ne correspond plus aux inscriptions).
- Réponse de Quentin :

## Notes pour l'état du projet

- Nouvelles classes principales : `model.AgendaCompte`, `model.EvenementAgenda`,
  `model.OrigineEvenement` ; `core.ServiceAgenda`, `core.ExportIcs`,
  `core.ResultatAgenda` ; `ServiceInscriptionEnLigne.desinscrire` ;
  `persistence.DepotApplication.agendaDe` ; `ui.ecran.PresentationAgenda` (sans JavaFX).
- Format de sauvegarde : ajout facultatif `agendas` dans le fichier applicatif.
  Les anciens fichiers se relisent sans changement (tests `AgendaRoundTripTest`,
  `AgendaRoundTripLimitesTest`).
- Aucun écran n'appelle encore `desinscrire` ni `ExportIcs` (prévu : Accueil, Mes tournois).
- Clé d'agenda d'un tournoi : n° d'homologation, sinon nom | premier jour | club.
  Renommer un tournoi non homologué crée une nouvelle entrée (le modèle `Tournoi`
  n'a pas d'identifiant stable).
- Démo : inchangée.

## Fusion

Fusion de `origin/main` sans conflit ; `verifier-fusion.py` : OK. Tests après fusion : 788, 0 échec.

## Chiffres

- Tests : 687 → 735 (mesurés par `mvn -B test`, 0 échec)
- Version de la démo : inchangée
