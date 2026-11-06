# Mon profil : bloc personnel, bloc Club, droits — 2026-11-06

- Branche : `tache/B029-mon-profil`
- Agent : chaîne `developpeur-interface`, `designer-interface`, `relecteur-code`, `documentaliste` (étape de pipeline)
- Pull Request : aucune à ce stade (complété à l'étape `integrateur`)
- Statut : en attente de fusion (`integrateur`)

## Pour Quentin

- La page « Mon profil » est en deux blocs. En haut, vos informations (nom, prénom, email), la partie Tournoi et l'appel micro à gauche, et votre licence FFTT officielle à droite.
- En bas, le bloc Club. Avant de relier votre compte à un club, un encart d'alerte bien visible vous montre ce que cela implique. Il faut cocher une case pour confirmer : sans cette case, la liaison n'est pas possible.
- Si le compte est relié à un club, un tableau montre ce que le compte peut faire pour ce club (organiser des tournois, voir l'équipe, gérer les autres administrateurs, etc.). La liste des droits est à valider (voir Questions).
- Le téléphone n'existe pas encore dans le compte : la page affiche « Non renseigné ».
- Le champ « Appel micro » affiche « Bientôt » tant que la saisie de la prononciation (B018) n'existe pas.

## À tester à l'écran

- Compte de démo : à préciser. Le harnais teste un compte relié à un club, puis un joueur non relié (case de confirmation cochée).
- Chemin : menu « Mon profil ».
- Ce qu'il faut regarder (thème clair et sombre) :
  - deux blocs bien séparés, colonnes alignées en haut ;
  - encart d'alerte lisible avant la liaison, case de confirmation visible ;
  - tableau des droits : en-tête teinté, lignes filetées, texte lisible ;
  - captures de référence : `docs/captures/B029/avant/` et `docs/captures/B029/apres/` (clair et sombre).
- Non vérifié par l'agent : rendu Windows, mise à l'échelle d'écran.

## Questions pour Quentin

### 2026-11-06 — Téléphone absent du compte
- Contexte : le cahier des charges v0 (section 2) demande le téléphone dans le bloc du haut. Le compte n'a pas de champ téléphone : la page affiche « Non renseigné ».
- Article FFTT : aucun.
- Options : A) ne rien changer, le téléphone reste « Non renseigné » ; B) ajouter le champ au compte (le format JSON change, lecture des anciens fichiers à garder, test `RoundTripTournoiTest` à vérifier).
- Choix provisoire de l'agent : A (pas de changement de format sans accord).
- Réponse de Quentin :

### 2026-11-06 — Liste des droits affichée dans le tableau du club
- Contexte : la liste vient du modèle actuel. Organisateur de tournois et voir l'équipe : pour tous les membres. Le référent ne peut pas se dissocier du club. La gestion des autres administrateurs est « à venir » pour le référent.
- Article FFTT : aucun (clause d'organisation du club, pas une règle fédérale).
- Options : A) garder cette liste telle quelle ; B) revoir les lignes (ajouter, retirer, renommer) avant fusion.
- Choix provisoire de l'agent : A.
- Réponse de Quentin :

### 2026-11-06 — Appel micro « Bientôt » en attendant B018
- Contexte : le champ de prononciation phonétique n'a pas de saisie tant que B018 n'est pas livré. La page affiche « Bientôt ».
- Article FFTT : aucun.
- Options : A) garder « Bientôt » ; B) masquer la section jusqu'à B018.
- Choix provisoire de l'agent : A (la section reste visible, comme demandé par le cahier des charges).
- Réponse de Quentin :

## Notes pour l'état du projet

- Classes principales : `ui.ecran.PageCompte` (refaite, deux blocs), `ui.ecran.PresentationProfil` (texte et règles, sans JavaFX, testée par `PresentationProfilTest`, 10 tests), `Ui.encartAlerte` (composant commun d'alerte).
- Changement de format de sauvegarde : aucun.
- Démo : inchangée (pas de `VERSION_DEMO` touché).
- Harnais : mode `profil` ajouté à `outils-dev/run.sh` (voir `outils-dev/LISEZMOI.md`).
- Réparation de `theme.css` : une fusion entre B028 et B033 avait perdu une accolade et une ouverture de commentaire sur `main`. Rétablies ; la règle `.icone-tuile.accueil-club-tuile` et la section B033 sont de nouveau appliquées.

## Fusion

Non faite à ce stade : `python3 outils-dev/verifier-fusion.py` sera lancé par l'`integrateur` après synchronisation avec `main`.

## Chiffres

- Tests : 740 → 750 (750 mesurés par `mvn -B test`, 0 échec ; 740 calculé : 10 nouveaux tests, aucun supprimé).
- Version de la démo : inchangée.
- Captures : 12 avant, 12 après (clair et sombre).
