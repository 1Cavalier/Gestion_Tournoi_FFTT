---
id: B011
titre: Jour J plus simple : matchs proposés et avancement des tableaux
type: jour-j
priorite: P1
taille: L
statut: en-cours
depend-de: []
fichiers: [ui/ecran/EcranJourJ, ui/ecran/EcranSalle, ui/ecran/PresentationJourJ, ui/ecran/PresentationSalle]
branche: tache/B011-jour-j-simplifie (prise le 2026-11-05)
pr:
---

## Ce qu'il faut faire

Retour de Quentin : le contenu du jour J est bon mais **mal positionné** ; il faut plus simple et plus fluide. Cible : un **écran principal unique** pour le juge-arbitre, sans aller-retour entre pages :
- à gauche ou en haut : **« Matchs proposés »** (les prochains matchs jouables, tous tableaux confondus, triés par l'ordonnanceur), chacun envoyable sur une table libre en un clic ou glisser-déposer ;
- au centre : les **tables** (libres / occupées, match, durée) ;
- un bandeau ou une colonne **« Où en sont les tableaux »** : une ligne par tableau avec son étape (pointage, poules x/y jouées, tableau final : tour en cours), une jauge, et un clic pour ouvrir le détail (poules ou arbre) ;
- saisie du score directement depuis la table ou le match, sans changer de page ;
- tout se met à jour en direct.
Garder les écrans détaillés existants (pointage, poules, arbre) comme vues secondaires ouvertes depuis cet écran.

## Critères de fin

- [ ] un juge-arbitre enchaîne « envoyer un match → saisir le score → match suivant » sans changer de page
- [ ] l'avancement de chaque tableau est visible d'un coup d'œil
- [ ] captures avant / après regardées
- [ ] `mvn -B test` à 0 échec

## Contraintes / décisions de Quentin

Ne pas changer les règles de l'ordonnanceur (la tâche B025 s'en occupe).

## Passations

## Résultat
