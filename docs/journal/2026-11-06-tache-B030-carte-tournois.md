# Carte des tournois : vue carte, « Voir plus », agenda par jour, statuts — 2026-11-06

- Branche : `tache/B030-carte-tournois`
- Agent : chaîne `developpeur-interface`, `designer-interface`, `relecteur-code`, `documentaliste` (étape de pipeline)
- Pull Request : aucune à ce stade (complété à l'étape `integrateur`)
- Statut : en attente de fusion (`integrateur`)

## Pour Quentin

- L'onglet Tournois a deux onglets : « Carte des tournois » (nouveau) et « Mes tournois ».
- La carte des tournois a deux présentations, au choix : « Carte » (filtres à gauche, carte et liste à droite) et « Agenda par jour » (les tournois classés par date, lisibles d'un coup d'œil).
- Chaque tournoi a un bouton « Voir plus » qui ouvre sa fiche. Le double-clic n'est plus nécessaire. La fiche s'ouvre même si l'on ne peut pas s'inscrire.
- Chaque tournoi porte un statut, avec une pastille, une couleur et un texte : En cours, À venir, Non accessible (avec la raison), Complet ou pas à votre niveau.

## À tester à l'écran

- Compte de démo : Brunoy, licencié 9141442 (Quentin SOUMET), 923 points.
- Chemin : menu « Tournois », puis « Carte des tournois ». Boutons « Carte » et « Agenda par jour » en haut à droite de la liste.
- Ce qu'il faut regarder (thème clair et sombre, bouton « Thème sombre » / « Thème clair » en haut) :
  - **Carte** : la liste à gauche, la carte à droite ; les marqueurs des tournois non accessibles sont grisés ; la légende des 4 statuts sous les filtres.
  - **Agenda par jour** : un titre par jour ; le jour courant est encadré ; les pastilles ont la même largeur ; le motif (en rouge ou en gris) passe bien à la ligne.
  - **« Voir plus »** : présent sur chaque tournoi, dans les deux présentations ; il ouvre la fiche, même pour un tournoi « Non accessible » ou « Complet ».
  - **Les 4 statuts** : dans la démo, on voit « Complet » (Tournoi de démonstration du jour J), « À venir » (Créteil, Lyon), « Non accessible » (Loiret, réservé aux licenciés du 45). Le statut « En cours » n'apparaît pas à l'écran : aucun tournoi du jour dans la démo (vérifié par test seulement).
  - **Tournoi sur deux jours** : l'Open national A de Lyon (20 et 21 février 2027) apparaît sous chacun de ses deux jours dans l'agenda. À confirmer.
- Captures de référence : `docs/captures/B030/avant/` et `docs/captures/B030/apres/` (clair et sombre, 1440 px).
- Non vérifié par l'agent : rendu Windows, mise à l'échelle d'écran. Sans connexion, le fond de carte est remplacé par un message ; les tournois restent sur la liste et les marqueurs restent visibles (c'est voulu).

## Questions pour Quentin

### 2026-11-06 — Tournoi en cours mais non accessible
- Contexte : un tournoi qui se joue aujourd'hui, et auquel on ne peut pas accéder (par exemple réservé à un comité), doit-il s'afficher « En cours » ou « Non accessible » ?
- Article FFTT : accès au tournoi, IX.101 / IX.102 (déjà cités dans le code). Aucun article trouvé sur l'affichage : je ne devine pas.
- Options : A) « En cours » d'abord, quelle que soit l'accessibilité (appliqué) ; B) « Non accessible » d'abord, « En cours » seulement si l'on peut participer.
- Choix provisoire de l'agent : A (l'information « le tournoi se joue maintenant » est la plus utile le jour même ; le motif reste dans la fiche).
- Réponse de Quentin :

### 2026-11-06 — Pastille « Complet » avec liste d'attente
- Contexte : tous les tableaux ouverts à son niveau sont pleins, mais une liste d'attente existe. Faut-il afficher « Complet » ou « À venir » ?
- Article FFTT : non trouvé dans le code ; à confirmer dans les textes 2026-2027 (liste d'attente).
- Options : A) « Complet » dès que les tableaux à son niveau sont pleins, avec la mention « liste d'attente éventuelle » (appliqué) ; B) « À venir » tant qu'une place d'attente reste possible.
- Choix provisoire de l'agent : A (la pastille dit tout de suite que l'inscription directe est fermée ; la liste d'attente est signalée dans le motif).
- Réponse de Quentin :

## Remarques (sans décision demandée)

- Harnais de captures cassé avant B030 : les étapes « Créer un tournoi » et « Suivre le tournoi » ne sont pas trouvées, et la compilation demande `LANG=C.UTF-8`. Les captures ont été faites avec une copie du harnais qui saute l'étape en échec. À réparer dans une tâche dédiée.
- Erreur CSS préexistante dans `theme.css` ligne 2816 (« Expected COLON ») : une accolade et l'ouverture d'un commentaire avaient été perdues lors d'une fusion de B028. Corrigé : la règle `.icone-tuile.accueil-club-tuile .icone-tuile-trace` et l'en-tête de section B033 sont de nouveau valides. `theme.css` est un fichier commun : à relire.
- Classes CSS posées en Java sans règle dans `theme.css` : `filtres-carte-tournois`, `agenda-tournois`, `carte-des-tournois`. Ce sont des repères de structure, sans effet visuel. Pas de correction nécessaire.
- `PresentationCarteTournois.carte` suppose que `acces()` n'est pas nul, comme le reste du catalogue. Rien à changer tant que le catalogue ne renvoie jamais de valeur nulle.
- Javadoc : présente sur les deux nouvelles classes (`core.StatutCarteTournoi`, `ui.ecran.PresentationCarteTournois`).

## Notes pour l'état du projet

- Classes principales :
  - `core.StatutCarteTournoi` : les 4 statuts et le motif, testés (`StatutCarteTournoiTest`).
  - `core.CatalogueTournois` : ajout de `Entree.tableauxAMonNiveauComplets`.
  - `ui.ecran.PresentationCarteTournois` : pastilles, cartes, légende, agenda par jour (testée, `PresentationCarteTournoisTest`).
  - `ui.ecran.PageTournois` : segments « Carte des tournois » / « Mes tournois », filtres, bascule Carte / Agenda par jour, « Voir plus ».
  - `ui.theme.css` : section « carte des tournois (B030) » en fin de fichier, plus la correction de la ligne 2816.
- Format de sauvegarde : inchangé.
- Démo Brunoy : inchangée (`ContexteApplication.VERSION_DEMO` non modifié, `TournoiDemoBrunoy` non touché).

## Fusion

Non fait ici : le résultat de `python3 outils-dev/verifier-fusion.py` est à reporter par l'`integrateur` à l'étape 5, après synchronisation avec `main`.

## Chiffres

- Tests : avant non mesuré dans cette session → après 812 tests, 0 échec (dernier passage vert, lu dans `target/surefire-reports`, aucun code modifié depuis ; `mvn -B test` à relancer par l'`integrateur`).
- Version de la démo : inchangée.
