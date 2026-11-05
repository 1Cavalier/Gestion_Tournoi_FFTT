# Plan « interface v2 et fin du jour J » (5 novembre 2026)

Demande de Quentin : une fois les poules terminées, l'arbre des rencontres
s'affiche ; on peut affecter les tables pour faire jouer les matchs ; la
petite finale est prévue ; et l'interface devient beaucoup plus propre et
dynamique, en blanc et bleu (charte v2 : skill `charte-graphique`).

Agents : `designer-interface` (apparence, captures avant / après) et
`developpeur-jour-j` (déroulé en salle, du cœur à l'écran).

Une session = une consigne = une Pull Request. **A et B peuvent tourner en
même temps** (fichiers différents). **C** après la fusion de B. **D** après la
fusion de A et C.

```
A  Refonte v2 : fondations ─────────────┐
B  Phase finale reliée, arbre, petite    ├──► D  Jour J au style v2
   finale ──► C  Salle et tables ───────┘
```

---

## A — Refonte visuelle v2 : fondations

> Utilise l'agent `designer-interface` (charte v2, blanc et bleu). Objectif :
> une interface beaucoup plus propre et dynamique.
> 1. Prends des **captures « avant »** de l'existant (`outils-dev/run.sh`, en
>    clair et sombre ; adapte les chemins des jars au dépôt Maven `~/.m2` si
>    besoin) dans `docs/captures/v2-avant/`.
> 2. `theme.css` : nouvelles valeurs de la palette v2 dans les deux thèmes
>    (mêmes noms de variables), variables de pastilles, base (typographie,
>    boutons, champs, cartes, listes, onglets, jauges, survols).
> 3. `Ui` : `tuileChiffre`, `pastille` (avec une énumération de statut, sans
>    `switch` dessus), `avatar` aux initiales, ligne de liste ; transitions
>    courtes d'ouverture de page.
> 4. `Coquille` : barre du haut blanche et barre latérale blanche avec élément
>    actif bleu ; `PageAccueil` avec tuiles de chiffres clés ; `EcranConnexion` ;
>    la liste de Gestion des tournois (cartes, pastilles de statut).
> 5. **Ne modifie pas** les écrans du jour J ni de la phase finale
>    (`EcranJourJ`, `EcranPointageTableau`, `EcranPoulesTableau`,
>    `VuePhaseFinale`, `AccesPhaseFinaleProvisoire`) : une autre session y
>    travaille. Ils hériteront de la palette.
> 6. Captures **« après »** dans `docs/captures/v2-apres/`, regarde-les,
>    corrige ce qui ne va pas (contrastes, alignements), puis cite-les dans la
>    Pull Request.
> Branche `ui/charte-v2-fondations`, Pull Request avec les captures avant /
> après et la liste des écrans à vérifier chez Quentin.

## B — Phase finale reliée au jour J, arbre des rencontres, petite finale

> Utilise l'agent `developpeur-jour-j`.
> 1. **Relier** : sur la page Jour J, quand toutes les poules d'un tableau sont
>    terminées, le tableau propose « Construire le tableau final » (second
>    tirage, graine affichée), puis ouvre la phase finale. Retire le bouton
>    « Phase finale (provisoire) », `AccesPhaseFinaleProvisoire` et
>    `TableauEssaiPhaseFinale` s'ils ne servent plus.
> 2. **Arbre des rencontres** : un vrai arbre graphique, une colonne par tour
>    (1/16, 1/8, quarts, demies, finale), cartes de match reliées par des
>    traits au tour suivant, vainqueur en gras, exempts visibles, match en
>    cours mis en évidence ; clic sur un match → saisie du score ; mise à jour
>    en direct. Défilement si le tableau est grand (64 joueurs).
> 3. **Petite finale par tableau** (réponse de Quentin dans `QUESTIONS.md`) :
>    option « Petite finale (match pour la 3e place) » dans le formulaire du
>    tableau, portée par la configuration du tableau, persistée (anciens
>    fichiers relus sans petite finale), écrite dans le règlement généré,
>    prise en compte par la construction du tableau final et le podium (sinon
>    3es ex aequo). Mets à jour `docs/passation/REGLES_CREATION_TOURNOI.md`.
> 4. **Graine du tableau final** sauvegardée avec le tableau, comme celle des
>    poules (champ JSON facultatif, `RoundTripTournoiTest`).
> 5. Démo du jour J : un tableau avec poules terminées prêt pour le tableau
>    final, et un tableau avec petite finale ; incrémente `VERSION_DEMO`.
> 6. Ne touche pas aux variables de couleur en tête de `theme.css` (une autre
>    session les change) : tes règles vont dans une section
>    `/* === Jour J : phase finale === */` en fin de fichier.
> Tests sur toute logique hors JavaFX (positions de l'arbre, libellés des
> tours, podium avec ou sans petite finale). Captures de l'arbre en
> `docs/captures/phase-finale/` si possible. Branche
> `jour-j/arbre-petite-finale`, Pull Request.

## C — Salle et affectation des tables (après la fusion de B)

> Utilise l'agent `developpeur-jour-j`. Le cœur `ordonnancement.Ordonnanceur` (tables libres,
> propositions de lancement par priorité, lancer, terminer, joueur occupé) et
> `SessionSalle` / l'autosave existent, sans écran. Construis la vue
> **« Salle »** du jour J :
> 1. une **grille des tables** du tournoi (nombre de tables du tournoi) :
>    carte par table, libre ou occupée, match en cours (tableau, tour,
>    joueurs), durée écoulée ;
> 2. une colonne **« Matchs prêts »** (poules et tableau final de tous les
>    tableaux), triée par la priorité de l'ordonnanceur, avec les matchs
>    bloqués et leur motif (joueur déjà en jeu) ;
> 3. **affecter** un match à une table : bouton « Proposer » qui remplit les
>    tables libres selon l'ordonnanceur, ou choix manuel (glisser-déposer si
>    simple, sinon menu « Envoyer à la table N ») ; le numéro de table
>    s'affiche aussi dans les poules et dans l'arbre ;
> 4. **terminer** un match depuis sa table ouvre la saisie du score ; la
>    table se libère et la suite se met à jour en direct ;
> 5. l'état de la salle survit à une fermeture (autosave existante).
> Une seule règle dure : un joueur ne peut pas être sur deux tables.
> Démo : tournoi du jour J avec des matchs en cours sur quelques tables.
> Tests sur la logique hors JavaFX. Branche `jour-j/salle-tables`, Pull Request.

## D — Écrans du jour J au style v2 (après A et C)

> Utilise l'agent `designer-interface`. Passe les
> écrans du jour J (page Jour J, pointage, tirage, poules, arbre, salle) au
> style v2 avec les composants de `Ui` (tuiles de chiffres en tête de page,
> pastilles de statut, avatars, cartes) ; supprime les styles devenus
> inutiles. Captures avant / après en clair et sombre. Branche
> `ui/charte-v2-jour-j`, Pull Request.
