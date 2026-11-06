---
id: B016
titre: Impression des feuilles de match, de poule et du tableau final
type: jour-j
priorite: P2
taille: M
statut: en-cours
depend-de: []
fichiers: [core/reglement (nouveaux générateurs), ui/ecran/EcranPoulesTableau, ui/ecran/VuePhaseFinale]
branche: tache/B016-feuilles-de-match
pr:
---

## Ce qu'il faut faire

PDF imprimables, sur le modèle de `RenduPdfReglement` :
- **feuille de partie** (tableau, tour, table, joueurs avec club et points, cases des manches, signature, arbitre) ; impression à l'envoi d'un match sur une table (option) ou à la demande ;
- **feuille de poule** (joueurs, ordre des parties I.301, grille des résultats, classement à remplir) ;
- **tableau final** imprimable (arbre) pour l'affichage en salle.
Couleur du tableau reprise si B015 est faite.

## Critères de fin

- [ ] contenu des documents testé
- [ ] boutons d'impression dans les poules, l'arbre et la salle
- [ ] `mvn -B test` à 0 échec

## Contraintes / décisions de Quentin

Format provisoire (cahier des charges v0, section 9) : **feuille de match** et **feuille de rencontre** ; nom, prénom, club, points de chaque joueur face à son adversaire ; tableau 3 × 5 : manches 1 à 5 en en-tête, autres cases vides. Le format officiel sera fourni plus tard par Quentin.

Aucune pour l'instant.

## Passations

## Résultat
