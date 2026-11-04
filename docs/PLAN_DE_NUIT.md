# Plan de nuit : plusieurs sessions en parallèle

Une session = une tâche = une Pull Request. Sur claude.ai/code : bouton
« Créer », dépôt `Gestion_Tournoi_FFTT`, environnement Default, coller la
consigne, Entrée. Les sessions ci-dessous touchent des fichiers différents et
peuvent tourner en même temps.

Au réveil : lire chaque PR, tester les écrans chez toi (`git pull` sur la
branche de la PR, puis `mvn clean javafx:run`), répondre dans `QUESTIONS.md`,
fusionner une PR à la fois. Si une PR est en conflit après la fusion d'une
autre, demande à sa session : « Mets ta branche à jour avec main et résous
les conflits. »

---

## Session 1 — Interface du jour J : pointage, tirage, poules (développement)

> Utilise l'agent `developpeur` avec les skills `charte-graphique` et
> `architecture-pingmanager`. Le cœur du jour J existe déjà sans écran
> (`ServicePointage`, `ServiceLancementTableau`, `MoteurTournoi`,
> `GenerateurMatchsPoule`, `ClassementPouleService`). Construis l'interface
> du **jour J** pour un tournoi, tableau par tableau :
> 1. une page « Jour J » accessible depuis la gestion du tournoi, qui liste
>    les tableaux du jour avec leur statut (inscription, pointage, lancé,
>    phase finale) ;
> 2. le **pointage** : ouvrir le pointage, cocher présent / payé, liste
>    d'attente et repêchage, désistement, clôture ;
> 3. le **lancement avec tirage au sort** : bouton lancer, graine affichée
>    (tirage public IX.109), composition des poules ;
> 4. les **poules** : matchs dans l'ordre officiel I.301, saisie des scores
>    manche par manche, issues non normales (forfait, abandon), classement de
>    la poule en direct (I.303), qualifiés mis en évidence.
> Ne fais pas la phase finale (autre session). Si le cœur manque d'une
> méthode, ajoute-la avec ses tests sans changer le comportement existant.
> Ajoute un tournoi de démonstration prêt pour le jour J (inscrits pointables),
> sans toucher à Brunoy, et incrémente `VERSION_DEMO`. Commits par étape.
> Branche `jour-j/pointage-tirage-poules`, Pull Request indiquant le compte de
> démo et les écrans à regarder en thème clair et sombre.

## Session 2 — Interface de la phase finale (développement)

> Utilise l'agent `developpeur` avec les skills `charte-graphique` et
> `architecture-pingmanager`. Le cœur de la phase finale existe sans écran
> (`ServiceLancementTableau.construirePhaseFinale`,
> `PlacementQualifiesService`, `GenerateurTableauElimination`,
> `ProgressionTableau`). Construis, dans **de nouveaux fichiers** du package
> `ui.ecran`, une vue « Phase finale » d'un tableau : construction du tableau
> une fois les poules terminées (second tirage, graine affichée), affichage
> du tableau à élimination tour par tour (byes visibles), saisie des scores,
> progression automatique des vainqueurs, petite finale si le format la
> prévoit, podium final. Une autre session construit en parallèle la page
> « Jour J » : n'y touche pas. Expose ta vue par une méthode publique
> (par exemple `VuePhaseFinale.creer(contexte, tournoi, tableau)`) et, pour
> pouvoir la tester, ajoute un seul point d'entrée provisoire dans la gestion
> du tournoi, clairement commenté. Tests sur toute logique hors JavaFX
> (libellés, ordre d'affichage des tours). Branche
> `jour-j/phase-finale`, Pull Request.

## Session 3 — Audit de sécurité (rapport)

> Utilise l'agent `auditeur-securite` pour auditer tout le dépôt. Commite
> `RAPPORT_SECURITE.md` (et les éventuels tests qui démontrent une faille)
> sur une branche `audit/securite` et ouvre une Pull Request avec le plan de
> correction par lots.

## Session 4 — Recherche : communiquer avec la FFTT (rapport)

> Utilise l'agent `chercheur-fftt`. Rédige `docs/RECHERCHE_FFTT.md` sur les
> moyens d'interroger les données de la FFTT (licences, points, clubs) et
> d'échanger avec SPID et GIRPE, avec sources, options classées et brouillon
> du courriel à envoyer à la FFTT. Branche `recherche/fftt`, Pull Request.

## Session 5 — Optimisation (après les sessions 1 et 2 de préférence)

> Utilise l'agent `optimiseur` sur les packages `core`, `ordonnancement`,
> `persistence` et `importation` (pas `ui`, en cours de modification par
> d'autres sessions). Mesure sur un tournoi de 400 joueurs et 15 tableaux,
> n'applique que des optimisations sûres. Branche `qualite/optimisation`,
> Pull Request avec `RAPPORT_OPTIMISATION.md`.

---

## Déjà proposées (correctifs de l'audit d'architecture)

- `correctif/switch-enumerations` et `correctif/port-horloge` : voir la
  conversation du 5 novembre. Elles touchent `ConfigurationTableau`,
  `Ordonnanceur`, `JsonWriter`, `TournoiMapper`, `GestionCartonsMatch`,
  `ImportJoueurs` : à ne pas lancer en même temps que la session 5.
