# Conformité du tirage (poules I.302, tableau I.304 / I.305) — 2026-11-06

- Branche : `tache/B040-conformite-tirage`
- Agent : `controleur-conformite`
- Pull Request : #<n> (complété à l'étape 5)
- Statut : en attente de relecture (4 tests rouges volontaires, à faire passer par le `correcteur`)

## Pour Quentin

Le tirage a été relu article par article contre les règlements sportifs 2026-2027.
Le serpentin des poules, les têtes de série par blocs, le demi-tableau opposé des
2es et le quart opposé des 3es sont conformes. Quatre écarts certains sont
prouvés par des tests (voir ci-dessous) ; aucun code de production n'a été
modifié. Trois questions de règle vous sont posées.

## À tester à l'écran

Rien : tâche d'audit, aucun changement visible.

## Écarts certains (tests rouges dans `ConformiteTirageI302I305Test`)

Corrections attendues pour le `correcteur` (code de production, non touché) :

1. **I.302, poules ordonnées** (`GenerateurPoules.separerLesClubs`) : l'échange
   place le joueur déplacé en FIN de l'autre poule (`autre.add(joueur)`), donc
   une poule n'est plus ordonnée par points (exemple : `[R7, R10, R1]`). Attendu :
   conserver l'ordre par points dans chaque poule (insertion à sa place, ou
   retri après les échanges).
2. **I.302, têtes de poule** (même méthode) : le joueur signalé en premier est
   le mieux classé du doublon, donc c'est lui qu'on déplace. Le n°1 peut quitter
   la tête de sa poule (les 4 meilleurs ne sont plus têtes de 4 poules).
   Attendu : déplacer le moins bien classé du doublon, jamais une tête de poule
   si un autre échange existe.
3. **I.305.2, numéro de poule** (`PlacementQualifiesService.calculerPlacement`) :
   les vainqueurs sont triés par points ; le texte désigne les numéros de poule.
   Attendu : vainqueur poule 1 → seed 1, poule 2 → seed 2, poules 3-4 → bloc
   3-4, poules 5-8 → bloc 5-8, etc. (ordre de première apparition des poules
   dans la liste des qualifiés). Voir question 1.
4. **I.304.1 / I.305.1, club au 1er tour** (`choisirSeedEvitantClub`,
   `creeConflitClub`) : sur un tableau complet de 16 (8 poules, 2 qualifiés,
   4 clubs de 4), 137 tirages sur 200 mettent deux joueurs du même club face à
   face au 1er tour alors qu'une solution sans conflit existe dans les 200 cas
   (vérifié par recherche exhaustive hors dépôt). Causes probables : choix glouton
   sans retour arrière ; `tableauIncomplet` est vrai pendant le placement (les
   places non encore remplies valent « vides »), donc le critère du 2e tour est
   appliqué comme le 1er et, si aucune place ne les satisfait tous les deux, le
   1er tour est sacrifié au hasard. Attendu : priorité stricte au 1er tour,
   critère du 2e tour seulement s'il y a réellement des exemptés, retour arrière
   si besoin.

Aussi à corriger (documentation) : `GenerateurTableauElimination.ordreDuCadre`
annonce `[1,4,3,2]` et `[1,8,5,4,3,6,7,2]` ; le code produit `[1,4,2,3]` et
`[1,8,4,5,2,7,3,6]` (le placement est correct, seul le commentaire est faux).

## Questions pour Quentin

### 2026-11-06 — Vainqueurs de poule : numéro de poule ou force ?
- Contexte : le code classe les vainqueurs de poule par points de classement
  pour les placer (seed 1, 2, blocs 3-4, 5-8…).
- Article FFTT : I.305.2 « Le vainqueur de la poule 1 en haut du tableau. Le
  vainqueur de la poule 2 en bas du tableau » ; « les vainqueurs des poules 3 et
  4 dans un groupe 3e-4e ».
- Options : A) suivre le texte à la lettre (numéro de poule, la poule 1 étant
  celle du n°1 du serpentin) ; B) garder la force des vainqueurs (pratique
  courante possible, non écrite dans les règlements fournis).
- Choix provisoire de l'agent : A (texte explicite), test rouge `i305_2_ECART_…`.
- Réponse de Quentin :

### 2026-11-06 — Origine du « tirage des 2es par blocs »
- Contexte : `placerDeuxiemes` tire les 2es par blocs de n/2 « selon le manuel des
  juges-arbitres ». Ce manuel n'est pas dans `docs/reglements/`.
- Article FFTT : I.305.3 dit seulement « Les joueurs classés 2e de poule par
  tirage au sort dans le demi-tableau opposé de leur premier respectif ».
- Options : A) simple tirage dans le demi opposé (texte seul) ; B) garder les
  blocs si vous avez la source (à citer dans le code et `CONFORMITE_FFTT.md`).
- Choix provisoire de l'agent : B (aucun changement, signalé « non vérifiable »).
- Réponse de Quentin :

### 2026-11-06 — Tableau sans poules (I.304)
- Contexte : `GenerateurTableauElimination.genererTableau(liste)` n'est appelé
  par aucun écran : l'application ne construit un tableau qu'après des poules.
  Un tableau direct n'aurait ni tirage des têtes de série (3-4, 5-8…) ni
  séparation des clubs (I.304.1, I.304.2).
- Article FFTT : I.304.1 et I.304.2.
- Options : A) hors périmètre (l'application ne propose que poules + tableau) ;
  B) à prévoir plus tard (nouvelle tâche).
- Choix provisoire de l'agent : A.
- Réponse de Quentin :

### Rappel (déjà ouverte dans `QUESTIONS.md`, 2026-11-05) — Place des exempts
Les règlements fournis ne disent pas où placer les exempts (I.304 / I.305 muets).
Constaté en plus par cet audit : une 2e ou 3e de poule peut occuper la place qui
fait face à une tête de série, qui ne bénéficie alors pas d'un exempt. Pas de
test écrit tant que la réponse n'est pas donnée.

## Réserves relevées (sans test rouge)

- I.305.1 : le critère du 2e tour est appliqué même si le tableau est complet
  (cf. cause probable de l'écart 4) ; effet à vérifier après correction.
- I.305.4 : si le quart opposé est plein, le 3e est placé dans tout le demi (repli
  silencieux qui viole « le quart opposé »). Jamais constaté sur 4 et 8 poules
  (tests verts), cas d'un tableau très incomplet non couvert.
- I.302 : « points qualificatifs / licence / situation mensuelle » : le choix de
  la source des points relève de l'import, pas du tirage (clause configurable
  d'organisateur, hors tirage).
- I.302 (clubs impossibles à séparer → rencontre au 1er tour) : assuré par
  `GenerateurMatchsPoule` (déjà testé).

## Notes pour l'état du projet

Nouveau test `ConformiteTirageI302I305Test` (4 rouges volontaires). Aucune
classe de production modifiée.

## Fusion

À compléter à l'étape de fusion.

## Chiffres

- Tests : 1043 → 1051 (dont 4 rouges signalés pour le correcteur)
- Version de la démo : inchangée
