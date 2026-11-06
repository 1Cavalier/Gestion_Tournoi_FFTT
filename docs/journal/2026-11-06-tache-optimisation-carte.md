# Optimisation : carte des tournois, flux de données, lisibilité — 2026-11-06

- Branche : `tache/optimisation-carte`
- Agents : session chef de projet (carte), `optimiseur` (passe générale), `relecteur-code` (verdict : OK, aucun défaut bloquant)
- Pull Request : voir la PR de la branche
- Statut : fusionné si GitHub Actions est vert

## Pour Quentin

La **carte des tournois** est plus légère. Une seule carte sert maintenant pour toute
l'application : avant, chaque passage sur l'onglet créait un nouveau navigateur
intégré, jamais libéré, ce qui alourdissait l'application au fil des allers-retours.
Les points sont dessinés d'un seul bloc, et la recherche attend une courte pause dans
la frappe avant de filtrer. Seule la vue affichée (carte ou agenda) est recalculée.
Si le serveur de l'IGN ne répond pas, la carte bascule sur OpenStreetMap au bout de
5 s : avant, elle pouvait rester vide. Les listes de tournois (onglet Tournois,
accueil, supervision, suivi) ne relisent plus un fichier qui n'a pas changé. Le
calcul de la salle est nettement plus rapide sur les gros tournois. Une partie du
code recopié d'écran en écran est désormais regroupée dans des aides communes.

## À tester à l'écran

- Compte de démo habituel → Tournois → Carte des tournois : la carte s'affiche,
  les points sont cliquables, la bulle apparaît au survol, la recherche filtre.
- Aller sur Accueil puis revenir sur Tournois plusieurs fois : la carte revient
  tout de suite, sans repartir de zéro.
- Basculer en thème sombre avec la carte ouverte ; basculer Carte ↔ Agenda par jour.
- Gestion des tournois → un tournoi → Général : le formulaire est inchangé (méthode découpée).

## Questions pour Quentin

### 2026-11-06 — Un seul objet par tournoi ouvert
- Contexte : chaque écran relit le fichier du tournoi (EcranJourJ à chaque retour, FicheTournoi à chaque bascule).
- Options : A) garder la relecture (simple, sûr) B) un objet partagé par tournoi ouvert (plus rapide, refonte plus large).
- Choix provisoire : A ; détail dans `RAPPORT_OPTIMISATION.md`.
- Réponse de Quentin :

Les deux autres propositions (inscription en ligne via `ServiceFichierTournoi`, découpage d'`EcranSalle`, `GenerateurReglement`, `TournoiMapper`) sont dans `RAPPORT_OPTIMISATION.md`.

## Notes pour l'état du projet

- `CarteTournois.partagee(...)` : carte unique ; `carte.html` en rendu canvas, repli OSM après 5 s sans tuile.
- `ContexteApplication` : cache de lecture (date + taille) pour `tousLesTournois` et `lireTournoi` (lecture seule) ; `chargerTournoi` relit toujours.
- Nouvelles aides : `model.Textes`, `Ui.message`, `Ui.filet`, `SuiviTournoi.ORDRE_DE_LA_JOURNEE`.
- `outils-dev/tests.sh` compile par défaut dans un dossier temporaire privé (les sessions parallèles s'écrasaient).
- Format JSON inchangé.

## Fusion

À compléter après synchronisation.

## Chiffres

- Tests : 1042 → 1046 (`outils-dev/tests.sh` ; GitHub Actions fait foi)
- Ordonnanceur (≈ 6 000 matchs) : 19 ms → 1,2 ms ; relecture des tournois en liste : 40 à 240 ms → 0 ms si rien n'a changé
- Version de la démo : inchangée
