# Impression des feuilles de match, de poule et du tableau final — 2026-11-06

- Branche : `tache/B016-feuilles-de-match`
- Agent : `developpeur-jour-j` (étape de pipeline, reprise le 8 novembre après la limite d'usage)
- Pull Request : #66
- Statut : voir Pull Request

## Pour Quentin

Le jour J, PingManager imprime maintenant les papiers de la salle : la **feuille de
match** (les deux joueurs face à face avec nom, prénom, club et points, puis votre
grille 3 × 5 « Manche 1 » à « Manche 5 », vainqueur, cartons, signatures ; deux
feuilles par page A4 à découper), la **feuille de poule** (joueurs, ordre des parties,
grille, classement à remplir) suivie de la **feuille de rencontre** (chaque partie de la
poule avec sa grille de manches), et le **tableau final** à afficher en salle (arbre avec
les résultats déjà saisis). Dans la salle, une case « Feuille à l'envoi » imprime la
feuille dès qu'un match part sur une table ; sinon, bouton « Feuille » sur chaque table.

## À tester à l'écran

- Compte de démo : Quentin, « Tournoi de démonstration du jour J ».
- Pas à pas :
  1. Jour J → tableau **C – Toutes séries** → Poules. Choisir la **Poule B**. En haut de
     la carte : « Imprimer la feuille de poule » → le PDF s'ouvre (2 pages : feuille de
     poule, puis feuille de rencontre). Vérifier l'ordre des parties (1–3, 1–2, 2–3 pour
     une poule de 3 à 2 qualifiés, I.301.2), la diagonale grisée de la grille, le
     classement vide.
  2. Même carte : « Feuilles de match » → une feuille par partie, deux par page, trait de
     coupe au milieu. La partie en cours porte « Table 2 » en gros ; les autres
     « Table : ____ ».
  3. Sur une ligne de partie : lien « Feuille » → une seule feuille.
  4. Jour J → **F – Open du matin** → Tableau final : « Imprimer le tableau final » (en
     haut à droite) → A4 paysage, exempts en pointillés, vainqueurs surlignés, « Table 3 »
     sur le quart en cours, petite finale en bas à droite. Cliquer un match à jouer : dans
     le panneau de saisie, « Imprimer la feuille de match ».
  5. Jour J → Salle : lien « Feuille » sous « Terminer » sur une table occupée.
  6. Cocher « Feuille à l'envoi » (en haut), puis « Envoyer le prochain » sur une table
     libre : le message d'envoi indique « Feuille envoyée à l'imprimante » (ou « ouverte »
     si le système ne sait pas imprimer directement) avec le chemin du fichier. Faire
     « Proposer » : toutes les feuilles des matchs lancés sortent dans un seul PDF.
  7. Fermer puis rouvrir PingManager : la case « Feuille à l'envoi » est toujours cochée.
  8. Les PDF sont rangés dans le dossier de données de PingManager, sous-dossier
     `feuilles` (rien n'est perdu si la visionneuse ne s'ouvre pas).
- Ce qu'il faut regarder (thème clair et sombre) : place des boutons, lisibilité des PDF
  imprimés (noms longs coupés avec « … »), impression directe sous Windows.

## Questions pour Quentin

### 2026-11-06 — « Feuille de rencontre » en tournoi individuel
- Contexte : le cahier des charges v0 (section 9) demande une « feuille de match » et une
  « feuille de rencontre ». Dans les règlements, la feuille de rencontre est celle des
  compétitions **par équipes** (II.106) ; en individuel, le texte parle de **feuille de
  partie** (règlement disciplinaire, art. 3 : cartons inscrits sur la feuille de partie,
  signature du joueur).
- Article FFTT : règlements sportifs II.106 ; règlements généraux, règlement disciplinaire art. 3.
- Options : A) feuille de rencontre = toutes les parties d'une poule, chacune avec sa grille
  3 × 5 (imprimée à la suite de la feuille de poule) ; B) autre document à préciser (format
  officiel attendu).
- Choix provisoire de l'agent : A.
- Réponse de Quentin :

### 2026-11-06 — Impression directe à l'envoi ou ouverture du PDF
- Contexte : avec « Feuille à l'envoi », la feuille part directement vers l'imprimante par
  défaut quand le système le permet (sans fenêtre de choix) ; les impressions à la demande
  ouvrent le PDF (Ctrl+P).
- Article FFTT : aucun (choix d'ergonomie).
- Options : A) impression directe à l'envoi ; B) toujours ouvrir le PDF.
- Choix provisoire de l'agent : A, réversible (une ligne dans `EcranSalle.feuillesALEnvoi`).
- Réponse de Quentin :

### 2026-11-06 — Grille de 5 manches pour une partie au meilleur des 3
- Contexte : la grille 3 × 5 est imprimée avec 5 colonnes même au meilleur des 3 manches
  (2.12.1) ; les deux dernières colonnes restent vides. Au meilleur des 7 manches, la grille
  passe à 7 colonnes (contrôle de conformité 2.12.1, corrigé).
- Options : A) garder 5 colonnes par défaut ; B) réduire à 3 colonnes au meilleur des 3.
- Choix provisoire de l'agent : A (format imposé).
- Réponse de Quentin :

### 2026-11-08 — Manches gagnantes du format non transmises aux matchs
- Contexte : au lancement d'un match, `nombreManchesGagnantes` du format du tableau n'est pas
  recopié dans le match (toujours 3). Les feuilles ne peuvent donc afficher 7 colonnes que si
  cela est corrigé dans le lancement, hors périmètre de B016.
- Article FFTT : 2.12.1.
- Options : A) corriger le lancement dans une tâche à part ; B) laisser en l'état.
- Choix provisoire de l'agent : B (aucune modification hors périmètre).
- Réponse de Quentin :

## Notes pour l'état du projet

- Cœur : paquet `core.feuilles` (sans PDFBox) — `GenerateurFeuilles` (feuille de match,
  feuilles d'une poule, feuille de poule I.301 / I.303, arbre imprimable), `FeuilleMatch`,
  `FeuillePoule`, `ArbreImprimable`, `GrilleManches` (3 × 5), `JoueurFeuille`,
  `EnTeteFeuille`, `ResultatFeuille` + `MotifRefusFeuille` (exempt, joueurs à déterminer,
  match introuvable, poule sans partie, pas de tableau final).
- Rendu : `ui.infra.RenduPdfFeuilles` (PDFBox, police Inter). Placé dans `ui.infra` et pas
  à côté de `RenduPdfReglement` pour ne pas étendre l'exception d'architecture du cœur
  (test `RegleOrModeleSansDependanceTest`) ; le contenu, qui porte les règles, reste testé
  dans le cœur. `ui.infra.ImpressionFeuilles` enregistre toujours le PDF dans
  `<données>/feuilles` puis l'ouvre ou l'imprime (`java.awt.Desktop`, hors fil JavaFX).
- Interface : `ui.ecran.ImpressionJourJ` (préparation testée) ; boutons dans
  `EcranPoulesTableau`, `VuePhaseFinale`, `EcranSalle`.
- Format de sauvegarde : champ facultatif `feuilleALEnvoi` du tournoi (écrit seulement
  s'il vaut vrai ; anciens fichiers relus). `REGLES_CREATION_TOURNOI.md` complété.
- Démo : inchangée.
- Pour la suite : l'algorithme des colonnes de l'arbre est dupliqué entre
  `PresentationPhaseFinale.colonnes` (ui) et `GenerateurFeuilles.arbre` (cœur) ; une
  factorisation dans le cœur est possible. Le harnais `outils-dev/harness` (mode `jourj`)
  est cassé : bouton « Suivre le tournoi » introuvable (sans lien avec B016) ; captures
  faites avec un petit harnais hors dépôt (`/tmp/captures/B016/`, poules, arbre, salle,
  clair et sombre).
- Balayage final : `core.reglement.RenduPdfReglement` a le même défaut de caractères hors
  police (CJK, emoji) que celui corrigé dans `RenduPdfFeuilles.imprimable` ; non corrigé ici
  (hors périmètre), à traiter au balayage final.
- Couleur du tableau (B015) : non reprise, B015 n'est pas faite.

## Fusion

Fusion de `origin/main` (B009, B040 : nouvel ordre du cadre, placement I.302) sans conflit ;
`verifier-fusion.py` : OK, rien à signaler. Un test de B016
(`arbre_imprime_scores_vainqueur_table_et_abandon`) supposait trois vrais matchs au premier
tour avec 16 joueurs ; le nouvel ordre du cadre y place des exempts. Mise en place du test
adaptée (24 joueurs, exempts écartés), assertions inchangées.

## Chiffres

- Tests : 1046 → 1115 (après fusion de main) (mesurés par `outils-dev/tests.sh`, Maven Central inaccessible), 0 échec
- Version de la démo : inchangée

## Passations

- verificateur-modele : cœur et persistance de B016 vérifiés (poules 3 à 6, tableaux de 2/64/128, exempts et forfait, sans club ni points, ancien fichier sans `feuilleALEnvoi`) ; 14 tests ajoutés, 1 défaut démontré.
- Défaut à corriger (`correcteur`) : `RenduPdfFeuilles` lève `IllegalArgumentException: No glyph for U+65E5` sur un nom hors police (CJK, emoji) ; test `RenduPdfFeuillesLimitesTest.caractere_hors_police_ne_fait_pas_echouer_l_impression`. Mineur : `getBoolean` sur `feuilleALEnvoi` de mauvais type rend le fichier illisible (comportement commun à tous les champs).
- controleur-conformite : feuilles B016 vs I.301 (3 à 6, 1 ou 2 qualifiés, ordres relus un à un dans le texte), I.303 (2/1/0, départages) et 2.12.1 : conformes. Seul écart : grille de manches fixée à 5 colonnes alors que 2.12.1 admet 7 manches ; test `ConformiteFeuillesFFTTTest` (rouge volontaire) à faire passer par le `correcteur` (grille à 2×manchesGagnantes−1 colonnes dans `GrilleManches`).
- Questions : (a) arbitre/signatures — aucun article n'impose la feuille ni ses signatures (3.3.2.1 : un arbitre et un adjoint par partie), contenu laissé à l'organisateur ; (b) la note I.303 omet d) (reprise de la procédure) ; (c) le lancement ne propage pas `nombreManchesGagnantes` du format aux matchs (toujours 3). « Feuille de rencontre » : non tranché.
- correcteur : écart 2.12.1 corrigé — `GrilleManches.vide(manchesGagnantes)` donne max(5, 2×mg−1) colonnes (5 par défaut, format de Quentin conservé ; 7 au meilleur des 7) ; `copie` accepte 3 lignes de même taille ≥ 5 ; PDF : étiquettes 150 → 110 pt quand 7 colonnes, texte des cases ajusté. Test du contrôleur non modifié (attend bien 7 pour 4 manches gagnantes).
- Tests : 1096, 0 échec (`outils-dev/tests.sh`). Rendu PDF à 7 colonnes non vérifié visuellement (seul le cas par défaut reste exercé tant que le lancement ne propage pas le format, question (c)).
