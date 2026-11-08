---
id: B016
titre: Impression des feuilles de match, de poule et du tableau final
type: jour-j
priorite: P2
taille: M
statut: fait
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

- [x] contenu des documents testé
- [x] boutons d'impression dans les poules, l'arbre et la salle
- [x] `mvn -B test` à 0 échec

## Contraintes / décisions de Quentin

Format provisoire (cahier des charges v0, section 9) : **feuille de match** et **feuille de rencontre** ; nom, prénom, club, points de chaque joueur face à son adversaire ; tableau 3 × 5 : manches 1 à 5 en en-tête, autres cases vides. Le format officiel sera fourni plus tard par Quentin.

Aucune pour l'instant.

## Passations

- developpeur-jour-j (8 nov.) : contenu des feuilles dans `core.feuilles` (testé, sans PDFBox), rendu `ui.infra.RenduPdfFeuilles`, boutons poules / arbre / salle, option « Feuille à l'envoi » (JSON `feuilleALEnvoi` facultatif).
  Tests 1046 → 1079, 0 échec (`outils-dev/tests.sh`) ; PDF et écrans regardés (captures hors dépôt `/tmp/captures/B016/`).
  À vérifier : impression directe sous Windows ; 3 questions dans le journal (sens de « feuille de rencontre », impression directe, 5 colonnes au meilleur des 3).
- correcteur (8 nov.) : caractère hors police (CJK, emoji) remplacé par « ? » via `RenduPdfFeuilles.imprimable` (par code point), partagé par mesure, coupe et dessin ; tests 1095, 0 échec.
  Même défaut latent dans `core.reglement.RenduPdfReglement` (`propre` seul avant `showText`/`getStringWidth`) : signalé, non corrigé ici.

## Résultat
