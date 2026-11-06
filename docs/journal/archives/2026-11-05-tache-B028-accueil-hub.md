# Accueil « home » : cartes expliquées et section club séparée (B028) — 2026-11-05

- Branche : `tache/B028-accueil-hub`
- Agent : `developpeur-interface` (accueil, coquille), `designer-interface` (charte v2, captures), `relecteur-code` (relecture), `correcteur` (doublon « Bientôt : »), `documentaliste` (ce journal)
- Pull Request : à venir (étape 5, `integrateur`)
- Statut : en cours de chaîne (en attente de fusion par l'`integrateur`)

## Pour Quentin

L'accueil est refait comme un point de départ. Chaque grand bloc a une carte
avec une phrase qui dit à quoi il sert, et un bouton qui y mène : Profil,
Tournois, Agenda, Statistiques. Les cartes « Agenda » et « Statistiques » sont
marquées « Bientôt » tant que ces rubriques ne sont pas livrées ; l'Agenda
affiche déjà les prochaines dates du joueur. Pour un compte relié à un club,
une section séparée « Gestion de votre club » apparaît en bas, dans un bandeau
bleu doux : gestion des tournois, historique, buvette et autres (à venir). Un
compte joueur ne la voit pas. Les icônes sont dans des tuiles uniformes sur
toutes les cartes.

## À tester à l'écran

- Compte de démo : un compte club (section visible), un compte joueur licencié
  (section absente), un compte sans licence liée (carte Profil qui invite à lier la licence).
- Chemin : écran d'accueil (premier écran après connexion) → cliquer le bouton de chaque carte (par exemple « Ouvrir mon profil »).
- Ce qu'il faut regarder (thème clair et sombre) :
  1. Compte club : le bandeau « Gestion de votre club » est bien séparé des quatre cartes du haut, titre lisible.
  2. Compte joueur : pas de bandeau club ; les quatre cartes restent alignées.
  3. Chaque carte mène à la bonne rubrique. « Agenda » ouvre pour l'instant une page provisoire (« à venir », B032).
  4. Les tuiles d'icônes sont nettes ; les cartes « Bientôt » ont une tuile grise.
  5. En sombre, les textes restent lisibles sur le bandeau bleu doux.
- Captures : `docs/captures/B028/avant/` et `docs/captures/B028/apres/` (clair et sombre, trois comptes : club, joueur, sans licence). Harnais : `outils-dev/run.sh <dossier> accueil [sombre]`.
- Non capturés : le survol des boutons et le passage au clavier. À regarder à l'écran.

## Questions pour Quentin

### 2026-11-05 — Liste « Mes prochains tournois » sur l'accueil
- Contexte : une liste de tournois avec une pastille d'état (inscrit, liste d'attente…) était sur l'accueil. Elle a été retirée de l'accueil ; seules les dates restent, sur la carte Agenda.
- Article FFTT : aucun (interface).
- Options : A) garder la liste avec pastilles sur l'accueil ; B) dates seulement sur la carte Agenda (choix actuel).
- Choix provisoire de l'agent : B (accueil plus court, pas de doublon avec l'Agenda).
- Réponse de Quentin :

### 2026-11-05 — Bandeau bleu doux pour la section club
- Contexte : la section « Gestion de votre club » est dans un bandeau bleu doux encadré, titre plus grand, tuile bleu plein. Charte v2, décision de design.
- Article FFTT : aucun (interface).
- Options : A) bandeau bleu doux (choix actuel) ; B) bloc gris neutre comme les autres cartes ; C) simple titre sans cadre.
- Choix provisoire de l'agent : A, pour bien la séparer des cartes personnelles.
- Réponse de Quentin :

### 2026-11-05 — Entrée « Agenda » provisoire dans la coquille
- Contexte : la carte Agenda mène à une page « à venir » en attendant la tâche B032. Cette entrée de menu est provisoire et sera remplacée par B032.
- Article FFTT : aucun (interface).
- Options : A) garder la page provisoire jusqu'à B032 (choix actuel) ; B) masquer l'entrée de menu tant que B032 n'est pas fusionnée.
- Choix provisoire de l'agent : A, réversible.
- Réponse de Quentin :

## Notes pour l'état du projet

- Classes principales : `ui.ecran.PresentationAccueil` (contenu des cartes et règle « section club seulement si club relié », sans JavaFX, 9 tests dans `PresentationAccueilTest`), `ui.ecran.PageAccueil` (vue mince ; le constructeur à un seul argument ne sert qu'aux tests), `ui.ecran.Coquille` (nouvelle entrée « Agenda » provisoire), `ui.ecran.Ui.iconeTuile` (tuile d'icône commune), `ui.ecran.Icones.AGENDA`.
- CSS : section « B028 » en fin de `theme.css` (57 lignes, variables de thème uniquement).
- Harnais de captures : `Capture` (dossier `harness`, hors `src/main`) étendu pour la page d'accueil.
- Format de sauvegarde : inchangé. Démo : inchangée.
- Défaut non bloquant (relecture) : le constructeur à un argument de `PageAccueil` n'est utilisé que par les tests.
- JavaFX non vérifié en session cloud : validation à l'écran par Quentin (captures comprises).

## Fusion

Non faite : c'est l'`integrateur` qui synchronise avec `main` et lance `python3 outils-dev/verifier-fusion.py`.

## Chiffres

- Tests : 687 → 697 (mesurés par `mvn -B test` ; 0 échec ; nombre de `@Test` sur `main` et sur la branche)
- Version de la démo : inchangée
