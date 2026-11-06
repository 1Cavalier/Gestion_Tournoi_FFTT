# Mes tournois : tournoi en cours, inscriptions et agenda, tournois passés — 2026-11-06

- Branche : `tache/B031-mes-tournois`
- Agent : chaîne `developpeur-interface`, `designer-interface`, `correcteur`, `verificateur-modele`, `relecteur-code`, `documentaliste` (étape de pipeline)
- Pull Request : aucune à ce stade (complété à l'étape `integrateur`)
- Statut : en attente de fusion (`integrateur`)

## Pour Quentin

- L'onglet « Mes tournois » est refait en trois parties, dans l'ordre : le tournoi en cours (accès direct au suivi), mes inscriptions (triées par date), puis les tournois passés.
- Mes inscriptions ont un agenda : je peux ajouter ou retirer une date personnelle, et exporter mes dates dans un fichier .ics pour Google Agenda ou Outlook.
- Je peux désinscrire un tournoi à venir depuis sa carte. La désinscription est enregistrée tout de suite, et l'agenda est mis à jour.
- Un tournoi passé s'ouvre en lecture seule : on revoit ma poule, les scores et le tableau principal, sans relecture automatique ni bouton « Actualiser ». Le badge « Terminé » l'indique.
- Mes performances (victoires, défaites) sont calculées pour chaque tournoi passé, par tableau et au total. Un match perdu sur abandon est compté comme défaite, et signalé à part (voir Questions).
- La partie « organisés par le club » est retirée de cet onglet.

## À tester à l'écran

- Compte de démo : Brunoy, licencié 9141442 (Quentin SOUMET), 923 points.
- Chemin : menu « Tournois », puis onglet « Mes tournois ».
- Ce qu'il faut regarder (thème clair et sombre, bouton « Thème sombre » / « Thème clair » en haut) :
  - **Les 3 parties** : tournoi en cours (en premier), mes inscriptions, puis les tournois passés, dans cet ordre.
  - **Désinscription** : dans la démo, il faut d'abord s'inscrire à Créteil (fiche du tournoi). Le harnais de captures a fait cette inscription préalable. Ensuite, désinscrire depuis « Mes inscriptions » : la carte disparaît et le tournoi n'est plus dans l'agenda après rechargement.
  - **Agenda** : ajouter puis retirer une date personnelle ; les titres des étiquettes restent alignés.
  - **Export .ics** : depuis l'agenda, enregistrer le fichier, puis l'importer dans Google Agenda et dans Outlook (vérification manuelle, non faite par l'agent).
  - **Tournoi passé** : « Revoir poule et tableau » ouvre le suivi en lecture seule, badge « Terminé », pas de « Actualiser ». Non capturé faute de tournoi joué dans la démo : à regarder à l'écran.
  - **Mes performances** : victoires et défaites par tableau, cohérentes avec les scores affichés.
  - **Non capturés** : la carte « Tournoi en cours » (aucun tournoi de démo à la date du jour) et le badge « Terminé ». À regarder à l'écran avec un tournoi adapté.
- Captures de référence : `docs/captures/B031/avant/` et `docs/captures/B031/apres/` (clair et sombre).
- Non vérifié par l'agent : rendu Windows, mise à l'échelle d'écran.

## Questions pour Quentin

### 2026-11-06 — Victoires et défaites par abandon ou forfait
- Contexte : un match gagné ou perdu parce que l'adversaire a abandonné, refusé de jouer ou été disqualifié. Faut-il le compter dans mes victoires et défaites ?
- Article FFTT : abandon, III.109 (texte lu dans `docs/reglements/reglements_sportifs_2026_2027.txt`). Les mentions de « forfait » du texte visent surtout les rencontres par équipes ; je n'ai pas trouvé de règle de forfait pour un tournoi individuel. Je ne devine pas.
- Options : A) compté dans victoires ou défaites, et relevé à part (« dont forfaits et abandons ») pour être signalé (appliqué) ; B) exclu des totaux, seuls les matchs réellement joués comptent.
- Choix provisoire de l'agent : A. Le bilan reste fidèle au tableau, et le signalement permet de le lire tel quel. Passer en B revient à retirer ce compte dans `PerformancesJoueur`.
- Réponse de Quentin :

## Notes pour l'état du projet

- Classes principales :
  - `core.PerformancesJoueur` : victoires, défaites et forfaits/abandons par tableau et au total, testée (`PerformancesJoueurTest`).
  - `ui.ecran.PresentationMesTournois` : textes, règles d'affichage et désinscription de l'onglet, testée (`PresentationMesTournoisTest`).
  - `ui.ecran.MesTournois` : vue mince qui pose les composants (en cours, inscriptions et agenda, passés).
  - `ui.ecran.EcranSuiviTournoi` : mode lecture seule pour un tournoi passé (`enArchive()`).
  - `ui.ContexteApplication` : `desinscrireEnLigne` (enregistre tournoi et dépôt).
  - `ui.theme.css` : section « Mes tournois (B031) » en fin de fichier, et réparation de la fusion B030 (voir Remarques).
- Format de sauvegarde : inchangé.
- Démo Brunoy : inchangée (`ContexteApplication.VERSION_DEMO` non modifié, `TournoiDemoBrunoy` non touché).

## Remarques (sans décision demandée)

- Défaut connu, non bloquant : depuis un tournoi passé, « Informations du tournoi » repasse par `FicheTournoi.build` et ouvre le suivi en direct « En cours », avec relecture automatique. C'est en lecture seule, mais ce n'est pas le mode « passé » voulu. À corriger dans une tâche dédiée.
- `theme.css` réparé après la fusion de B030 : cette fusion avait mêlé les règles `.agenda-jour-titre` et le bandeau B029, et provoqué des erreurs d'analyse CSS (vers les lignes 3037 à 3090). Vérifié règle par règle contre B029 et B030 : aucune règle perdue, aucun doublon. `theme.css` est un fichier commun : à relire.
- Mineurs :
  - `ContexteApplication.mesTournoisClub` n'est plus utilisé que par un test (`OngletTournoisContexteTest`). À supprimer dans une tâche de nettoyage.
  - La classe CSS `.intertitre-groupe` n'a plus d'usage en Java, seulement sa règle CSS.
  - Des classes de structure posées en Java n'ont pas de règle CSS (`mes-tournois-en-cours`, `mes-tournois-inscriptions`, `mes-tournois-passes`, `mes-tournois-agenda`, `mes-agenda-liste`). Ce sont des repères, sans effet visuel.
- Pas de « double forfait » dans le modèle : un seul joueur est fautif par match.
- Locale : `PresentationMesTournoisTest` ne dépend plus de l'encodage (le nom de fichier « Passé.json » est en ASCII dans le test).

## Fusion

Non fait ici : le résultat de `python3 outils-dev/verifier-fusion.py` est à reporter par l'`integrateur` à l'étape 5, après synchronisation avec `main`.

## Chiffres

- Tests : 812 → 830 (`@Test` dans `src/test` : 812 sur `main`, 830 sur la branche ; 830 au passage vert du `verificateur-modele`, 0 échec, aucun code modifié depuis ; `mvn -B test` à relancer par l'`integrateur`).
- Version de la démo : inchangée.
