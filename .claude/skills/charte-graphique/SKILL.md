---
name: charte-graphique
description: Charte graphique v2 « blanc et bleu » de l'interface JavaFX de PingManager (couleurs, typographie, espacements, composants, classes CSS, thèmes clair et sombre, captures d'écran). À utiliser pour tout travail sur un écran du package ui ou sur theme.css.
---

# Charte graphique v2 de PingManager : blanc et bleu

Interface bureau JavaFX, package `com.pingmanager.ui`. Décision de Quentin
(5 novembre 2026) : une interface **beaucoup plus propre et dynamique**, en
**blanc et bleu**, dans l'esprit des tableaux de bord web modernes (référence
citée par Quentin : le site de Gestion Ping, logiciel de gestion de club).
On s'inspire du **style** (fond blanc, bleu roi, tuiles de chiffres, pastilles
de statut, avatars à initiales), **jamais** de son logo, de son nom, de ses
textes ni de sa couleur de marque exacte. Thèmes **clair (par défaut) et
sombre**.

## Où sont les styles

- Une seule feuille : `src/main/resources/com/pingmanager/ui/theme.css`.
- Les couleurs sont des **variables nommées** définies sur `.root.theme-clair`
  et `.root.theme-sombre` (bascule : `ui.ecran.Theme`). **Garder les noms de
  variables existants** (`-primaire`, `-surface`…) : les écrans s'en servent.
  On change leurs valeurs et on en ajoute.
- Composants partagés fabriqués par `ui.ecran.Ui` : y ajouter une méthode
  plutôt que dupliquer du code dans les écrans.
- **Jamais de couleur hexadécimale ni de `setStyle(...)` dans le Java** : une
  classe de style (`getStyleClass().add(...)`) et la règle dans `theme.css`.
- Une section de `theme.css` par domaine, avec un titre en commentaire
  (`/* === Jour J : phase finale et tables === */`), pour que plusieurs
  sessions puissent travailler sans se gêner.

## Palette v2 (valeurs cibles)

| Variable | Rôle | Clair | Sombre |
|---|---|---|---|
| `-fond-app` | fond général | `#F5F7FB` | `#0E1424` |
| `-surface` | cartes, champs, barre latérale | `#FFFFFF` | `#161E33` |
| `-surface-2` | survol, zones secondaires | `#EEF2FB` | `#1E2843` |
| `-bordure` | traits | `#DCE3F0` | `#2A3656` |
| `-bordure-forte` | contours appuyés | `#B8C4DC` | `#3B4A70` |
| `-texte` | texte principal | `#0F1B3D` | `#E7ECF7` |
| `-texte-doux` | texte secondaire | `#5B6785` | `#9AA6C3` |
| `-primaire` | bleu roi : actions, sélection | `#1D3FCF` | `#5B7BFF` |
| `-primaire-fort` | survol / appui | `#1632A8` | `#7D97FF` |
| `-primaire-doux` | fonds teintés, élément actif | `#E8EDFD` | `#1E2B57` |
| `-sur-primaire` | texte sur bleu | `#FFFFFF` | `#0B1020` |
| `-entete-fond` | barre du haut : **blanche** | `#FFFFFF` | `#121A2E` |
| `-entete-texte` | texte de la barre du haut | `#0F1B3D` | `#E7ECF7` |
| `-accent` | cyan, petits détails | `#22B8CF` | `#55D5E0` |
| `-attention` | ambre : obligatoire manquant | `#F6B12D` | `#F6B12D` |
| `-succes` | vert | `#157A52` | `#3FC48E` |
| `-erreur` | rouge | `#C8372F` | `#F07167` |

Vert et rouge du thème clair légèrement foncis par rapport à la cible
(`#1F9D6B`, `#D2403A`) pour tenir 4,5:1 quand ils servent de texte.

Pastilles de statut (fond doux + texte foncé), déclarées dans les deux
thèmes : `-succes-doux` / `-succes-texte`, `-attention-doux` /
`-attention-texte`, `-erreur-doux` / `-erreur-texte`, `-info-doux` /
`-info-texte` (bleu), `-neutre-doux` / `-neutre-texte` (gris).
Autres variables v2 : `-ombre-forte` (popups), `-avatar-1` à `-avatar-5` et
`-sur-avatar` (avatars), `-vitrine-fond`, `-vitrine-texte`,
`-vitrine-texte-doux`, `-vitrine-plateau`, `-vitrine-trait` (panneau de
l'écran de connexion).

Règles : le **blanc et le bleu dominent** ; une seule couleur d'action (le
bleu roi) ; l'ambre reste réservé à « obligatoire manquant » ; vert, ambre,
rouge et gris servent aux **pastilles de statut**, jamais en grands aplats.
Contraste du texte ≥ 4,5:1 sur son fond.

## Structure des écrans

- **Barre du haut blanche** (bordure basse 1 px) : nom du logiciel, titre de
  la page, bascule clair/sombre, compte connecté (avatar à initiales).
- **Barre latérale blanche** à gauche : rubriques avec icône simple ; élément
  actif = fond `-primaire-doux`, texte et barre verticale `-primaire`.
- **Contenu** sur `-fond-app`, en **cartes** blanches (rayon 12, bordure 1 px,
  ombre très légère au plus), marges 24.
- En tête de chaque page de suivi : une rangée de **tuiles de chiffres clés**
  (grand nombre en semi-gras 26-28, libellé dessous en `-texte-doux`, petite
  évolution ou pastille).

## Composants (dans `Ui`, livrés le 5 novembre 2026)

- `Ui.tuileChiffre(valeur, libelle[, complement])` et `Ui.rangeeTuiles(...)` :
  tuiles de chiffres clés (classes `tuile-cle`, `tuile-cle-valeur`,
  `tuile-cle-libelle` ; `tuile-cle-accent` = filet bleu à gauche). L'ancienne
  `Ui.tuile` (classes `tuile`, `tuile-chiffre`) reste pour les écrans du jour J.
- `Ui.pastille(texte, StatutPastille)` : pastille arrondie (rayon 999),
  énumération `StatutPastille` (`SUCCES`, `ATTENTION`, `ERREUR`, `INFO`,
  `NEUTRE`) qui porte sa classe CSS (`pastille-succes`…) ;
  `StatutPastille.pourTournoi(StatutTournoi)` et `libelleTournoi(...)` par
  chaînes de `if`. L'ancienne `Ui.pastille(texte, "etat-…")` reste valable ;
  ses classes `etat-*` / `cadre-*` ont désormais l'allure des pastilles.
- `Ui.avatar(prenom, nom)` / `Ui.avatarGrand(...)` : rond aux initiales
  (classes `avatar`, `avatar-teinte-1…5`, `avatar-initiales`) ; logique pure
  et testée dans `Avatars` (initiales, teinte stable).
- `Ui.ligneElement(visuel, titre, detail, aDroite...)` et
  `Ui.ligneJoueur(prenom, nom, club, points, pastilles...)` : lignes de liste
  (classe `ligne-element`, survol `-surface-2`, `ligne-element-premiere` sans
  filet, `ligne-element-choisie` = `-primaire-doux`).
- `Ui.blocDate(date)` : petit bloc jour + mois (classe `bloc-date`).
- `Ui.etatVide(texte, action)` / `Ui.etatVideCompact(texte)` : états vides.
- `Ui.icone(trace, classe)` / `Ui.iconeCadree(...)` : icônes dessinées
  (tracés 16 × 16 dans `Icones`, couleur par la classe `icone`).
- `Ui.apparition(noeud)` : fondu + glissement de 8 px en 180 ms, appelé par
  la `Coquille` et la page Gestion des tournois à chaque changement de page.
- Onglets segmentés (déjà `Ui.segment`) au style v2.
- Boutons : primaire plein bleu, secondaire blanc bordé, lien ; rayon 8 ;
  hauteur 36.
- Jauges de remplissage fines (6 px), bleu sur `-surface-2`.

## Dynamique

- Survol visible sur tout ce qui se clique ; curseur main.
- Transitions courtes (150-200 ms) à l'ouverture d'une page ou d'un panneau
  (`Ui.apparition`), jamais plus.
- Mises à jour **en direct** : un score saisi met à jour classement, arbre et
  tuiles sans recharger la page.
- États vides soignés (« Aucun match en cours. Les tables libres s'afficheront
  ici. »).

## Typographie et espacements

- Police Inter embarquée (`resources/com/pingmanager/polices`).
- Titre de page 22 semi-gras, titre de carte 15 semi-gras, corps 13-14, aide
  12, chiffres clés 26-28.
- Espacements 4 / 8 / 12 / 16 / 24 / 32 ; rayons : cartes 12, champs et
  boutons 8, pastilles 999.
- Code couleur des formulaires de tournoi inchangé (rond = niveau d'exigence,
  cadre = état), juste réaccordé à la palette.

## Textes à l'écran

Français, phrases courtes, vocabulaire du club et du juge-arbitre. Un refus
explique pourquoi et quoi faire.

## Vérification par captures

Les captures sont possibles en session cloud en lançant l'application sous
Xvfb (jars JavaFX et PDFBox de la recette de `CLAUDE.md`) ; il n'y a plus de
harnais ni d'images de référence dans le dépôt, tout code de capture reste
dans `/tmp`. Pour tout lot
d'interface : produire des captures **avant / après**, en thème clair et
sombre, **hors du dépôt** (`/tmp/captures/<lot>/`, PNG, largeur 1440), les
regarder soi-même avec l'outil de lecture d'image et les décrire dans la Pull
Request. **Ne jamais les committer** (le dépôt avait gonflé à 31 Mo de
captures). Si les captures sont impossibles, le dire.
