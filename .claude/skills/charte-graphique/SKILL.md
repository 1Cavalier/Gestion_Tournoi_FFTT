---
name: charte-graphique
description: Charte graphique de l'interface JavaFX de PingManager (couleurs, typographie, espacements, classes CSS, thèmes clair et sombre). À utiliser pour tout travail sur un écran du package ui ou sur theme.css.
---

# Charte graphique de l'interface PingManager

Interface bureau JavaFX, package `com.pingmanager.ui`. Objectif : pro, lisible,
agréable, surtout pour les écrans de saisie. Thèmes **clair et sombre**.

## Où sont les styles

- Une seule feuille : `src/main/resources/com/pingmanager/ui/theme.css`.
- Les couleurs sont des **variables nommées** définies sur `.root.theme-clair`
  et `.root.theme-sombre` (bascule gérée par `ui.ecran.Theme`).
- Les composants partagés sont fabriqués par `ui.ecran.Ui` : y ajouter une
  méthode plutôt que dupliquer du code dans les écrans.
- **Jamais de couleur hexadécimale ni de `setStyle(...)` dans le Java** : poser
  une classe de style (`getStyleClass().add(...)`) et écrire la règle dans
  `theme.css`, en utilisant les variables.

## Palette (variables de theme.css)

- Structure : `-fond-app`, `-surface`, `-surface-2`, `-bordure`, `-bordure-forte`.
- Texte : `-texte`, `-texte-doux`.
- Actions : `-primaire` (#335F8A en clair), `-primaire-fort`, `-primaire-doux`,
  `-sur-primaire`.
- En-tête : `-entete-fond` (bleu ardoise #2F4558), `-entete-texte`.
- `-accent` (cyan #55D5E0) : sélection et petits détails seulement.
- `-attention` (ambre #F6B12D) : **uniquement** « obligatoire manquant ». Jamais
  en grand fond.
- `-succes` / `-erreur` : succès et erreurs de saisie seulement.
- Dépendances (rond devant le libellé) : `-dep-homologation` (bleu),
  `-dep-publication` (cyan), `-dep-recommande` (anneau bleu), `-dep-optionnel`
  (gris).

Le bleu domine ; chaque nouvelle couleur doit être déclarée dans **les deux**
thèmes.

## Code couleur des formulaires de tournoi

- Rond devant le libellé = niveau d'exigence du champ.
- Cadre du champ = état, en direct : vert rempli, orange obligatoire vide,
  gris facultatif vide.
- Légende courte en tête de chaque formulaire.

## Typographie et espacements

- Police Inter embarquée (`resources/com/pingmanager/polices`).
- Échelle : titre de page 20, section 15 semi-gras, corps 13-14, aide 11-12.
- Espacements 4 / 8 / 12 / 16 / 24 / 32 ; rayons : cartes 10, champs et
  boutons 8, badges 6 ; pas d'ombres lourdes (surface + bordure 1 px).
- Libellés au-dessus des champs ; densité confortable (vgap 10-12).

## Textes à l'écran

- Français, phrases courtes, vocabulaire du club et du juge-arbitre (pointage,
  liste d'attente, tableau, poule), pas de jargon technique.
- Un refus explique pourquoi et quoi faire (« Ce tableau est réservé aux
  joueuses. »).

## Vérification

JavaFX ne s'affiche pas en session cloud. Dire dans la Pull Request quels
écrans ont changé et ce que Quentin doit regarder (thème clair **et** sombre).
Les captures peuvent se faire chez lui avec `outils-dev/run.sh`.
