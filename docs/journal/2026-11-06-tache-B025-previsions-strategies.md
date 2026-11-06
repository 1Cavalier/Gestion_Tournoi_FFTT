# Prévisions d'ordonnancement et stratégies du club (B025) — 2026-11-06

- Branche : `tache/B025-previsions-strategies`
- Agent : `developpeur` (premier maillon de la chaîne)
- Pull Request : (complété à l'étape 5)
- Statut : en attente des agents suivants puis de l'intégrateur

## Pour Quentin

Le club choisit maintenant sa **stratégie principale** parmi trois, chacune
expliquée en clair avec sa contrepartie : « Avancer les tableaux commencés
tôt », « Tout faire avancer petit à petit », « Faire attendre le moins possible
les joueurs ». Le choix est enregistré avec le tournoi et sert le jour J : les
matchs proposés en salle (et leurs couleurs de priorité) suivent cette
stratégie. Sans choix, rien ne change par rapport à avant.

Un nouvel écran **« Prévisions et stratégie »** simule toute la journée en une
fraction de seconde : heure de fin de chaque tableau, attente moyenne et
maximale des joueurs, occupation des tables, tableaux qui débordent. On peut
comparer deux stratégies côte à côte (mêmes poules, mêmes résultats : seul
l'ordre des matchs change).

## À tester à l'écran

- Compte de démo : Quentin, « Tournoi National B de Brunoy 2027 » (publié, donc deuxième version de l'écran).
- Chemin : Gestion des tournois → Brunoy 2027 → carte « Stratégie d'ordonnancement » (entre le parcours et « Remplissage et finances ») → « Choisir la stratégie et prévoir la journée ».
- Ce qu'il faut regarder (thème clair et sombre) :
  1. Les trois cartes de stratégie : titre, explication, « En contrepartie : … » ; la carte choisie est bleutée. Cliquer une autre carte : message vert « Stratégie enregistrée… » ; revenir au tournoi : la carte reprend le nouveau choix.
  2. « Simuler la journée » : tables (celles du tournoi, sinon 16), graine 1, « Lancer la simulation » : tuiles (fin prévue, attentes, occupation, tableaux qui débordent), phrase de synthèse, une ligne par tableau avec pastille « À l'heure » / « Déborde de … ».
  3. Relancer avec la même graine : résultat identique. Changer la graine : résultat différent.
  4. « Comparer avec » une autre stratégie : carte de comparaison, meilleure valeur en gras bleu, phrase de verdict.
  5. Case « Compléter les tableaux jusqu'à leurs places maximales » : simulation sur des tableaux pleins (joueurs fictifs).
  6. Saisir « 0 » ou « abc » dans Tables : message d'erreur rouge, pas de plantage.
  7. Jour J : choisir « Avancer les tableaux commencés tôt » puis ouvrir la salle du tournoi de démonstration du jour J : l'explication d'un match proposé peut contenir « tableau commencé depuis N min ».
- **Non vérifié visuellement** (session cloud, pas d'affichage JavaFX) : mise en page de l'écran, largeur des grilles, thème sombre.

## Questions pour Quentin

### 2026-11-06 — Stratégie appliquée sans choix du club
- Contexte : un tournoi sans choix garde exactement l'ancien réglage de l'ordonnanceur, qui correspond à « Faire attendre le moins possible les joueurs ». C'est donc la stratégie par défaut, affichée « par défaut » tant que le club n'a pas choisi.
- Article FFTT : aucun (organisation ; le juge-arbitre programme les parties, 3.3.1.2.2, IX.109).
- Options : A) défaut « Faire attendre le moins possible les joueurs » (rien ne change pour les tournois existants) ; B) imposer un choix au club avant le jour J ; C) autre défaut.
- Choix provisoire de l'agent : A (réversible).
- Réponse de Quentin :

### 2026-11-06 — Ce que montrent les simulations
- Contexte : sur nos scénarios d'essai, « Avancer les tableaux commencés tôt » finit le premier tableau 3 à 5 h plus tôt avec peu de tables, et donne souvent **aussi** l'attente moyenne la plus basse (les joueurs éliminés tôt partent). « Faire attendre le moins possible » (l'ancien réglage) n'est pas toujours la meilleure sur l'attente : il classe les matchs selon l'attente du match (depuis que les deux joueurs sont libres), pas selon le joueur qui attend le plus. Sur le grand tournoi de 400 joueurs, « Tout faire avancer petit à petit » a eu l'attente moyenne la plus basse (20 min contre 22) mais l'attente maximale la plus haute (4 h 28).
- Article FFTT : aucun.
- Options : A) garder les réglages actuels et laisser le club comparer à l'écran ; B) régler « Faire attendre le moins possible » sur le joueur qui attend le plus (change le comportement de la salle pour les tournois sans choix) ; C) faire une campagne de simulations sur Brunoy avant de décider.
- Choix provisoire de l'agent : A.
- Réponse de Quentin :

### 2026-11-06 — Débordement d'un tableau sans fin théorique
- Contexte : un tableau « déborde » s'il finit après sa fin théorique ; s'il n'en a pas, après 20 h le jour de son début.
- Article FFTT : aucun.
- Options : A) 20 h fixe ; B) heure de fin de journée réglable par tournoi ; C) ne pas signaler sans fin théorique.
- Choix provisoire de l'agent : A (constante `ScenarioSimulation.HEURE_LIMITE_PAR_DEFAUT`).
- Réponse de Quentin :

### 2026-11-06 — Simulation : vainqueur, durées, pas de forfait
- Contexte : le vainqueur est tiré selon l'écart de points (courbe logistique, 250 points), la durée part de l'estimation (20 min par défaut, plus longue en 5 manches, ±20 %), sans forfait, abandon ni temps de repos (aucun repos imposé par les textes pour un tournoi individuel). Les joueurs fictifs (« tableaux complets ») ne jouent qu'un tableau.
- Article FFTT : aucun.
- Options : A) garder ce modèle simple ; B) régler la durée d'une partie et la variabilité à l'écran ; C) apprendre des durées réelles des tournois passés (`EstimateurDuree` de la session).
- Choix provisoire de l'agent : A.
- Réponse de Quentin :

## Notes pour l'état du projet

- Modèle : `StrategieOrdonnancement` (3 stratégies, libellé, explication, contrepartie ; `PAR_DEFAUT = MINIMISER_ATTENTE`), `Tournoi.getStrategieOrdonnancement` / `strategieOrdonnancementChoisie` / `setStrategieOrdonnancement`.
- Ordonnancement : `PoidsOrdonnancement.pour(strategie)` et nouveau critère `poidsTableauCommence` (minutes depuis le premier lancement du tableau, nul par défaut) ; `Ordonnanceur.proposerLancements` calcule ses index une fois par vague (scores et explications identiques, testé).
- Cœur : `SimulateurTournoi` (simulation par événements, reproductible par graine, `scenarioDepuis(tournoi, tables, graine, completer)`, `comparer`), `ScenarioSimulation`, `ResultatSimulation` (bilan par tableau, débordements, attentes, occupation), `ComparaisonStrategies`. `ServiceSalle` suit la stratégie du tournoi (constructeurs sans poids) ; poids imposés toujours possibles.
- Persistance : champ facultatif `strategieOrdonnancement` du tournoi (écrit seulement si choisi ; absent ou inconnu → défaut) ; session : `poidsTableauCommence` facultatif (écrit seulement s'il est non nul).
- Interface : `EcranPrevisions` + `PresentationPrevisions`, carte « Stratégie d'ordonnancement » dans la deuxième version de `EcranCompletion` (B025 ajouté à `PARTIES_LIVREES`) ; section `theme.css` « Prévisions et stratégie d'ordonnancement (B025) ».
- Lien B037 : fait. Les couleurs « prochain à envoyer » viennent du classement de l'ordonnanceur, désormais réglé par la stratégie du tournoi (question B037 « Stratégie d'ordonnancement choisie », option C).
- Test modifié (pas affaibli) : `PresentationPreparationTournoiTest.remplissage_et_finances_remplace_sa_carte_bientot_apres_acceptation` attend désormais `[B024]` seul en « bientôt », B025 étant livré.

## Mesures de performance

Mesurées le 6 novembre 2026 en session cloud (`ChargeSimulationTest`, après chauffe de la JVM) :

| Mesure | Résultat |
|---|---|
| Ordonnanceur, 2 000 matchs prêts, 40 tables libres | 3,2 à 3,4 ms par vague de propositions |
| Simulation complète, 400 joueurs, 15 tableaux sur deux jours (Open de 128 et de 256 places), 40 tables, 1 531 parties — « Avancer les tableaux commencés tôt » | 225 ms ; fin dimanche 17 h 53 ; attente moyenne 22 min, max 179 min ; occupation 56 % ; 2 tableaux débordent |
| idem — « Tout faire avancer petit à petit » | 144 ms ; fin 17 h 38 ; attente moyenne 20 min, max 268 min ; occupation 56 % ; 4 tableaux débordent |
| idem — « Faire attendre le moins possible les joueurs » | 159 ms ; fin 17 h 48 ; attente moyenne 22 min, max 215 min ; occupation 54 % ; 3 tableaux débordent |
| Petit tournoi (3 tableaux, 63 joueurs, 4 tables) : fin du premier tableau | 12 h 51 (avancer) contre 18 h 08 (petit à petit) et 18 h 21 (attente) |

Avant l'indexation, chaque vague recalculait le déblocage en parcourant tous
les matchs pour chaque comparaison du tri (de l'ordre de N² log N par vague).

## Fusion

(à compléter par l'intégrateur)

## Chiffres

- Tests : 940 → 969 (mesurés par `mvn -B test`)
- Version de la démo : inchangée
