# Conformité FFTT — PingManager

Vérification de conformité aux **règlements officiels FFTT 2026-2027** (règlements
sportifs et règlements généraux), effectuée directement sur les textes fournis,
et non sur des sources secondaires. Chaque point a été confronté au code et, pour
les règles les plus délicates, vérifié empiriquement par des tests.

Légende : ✅ implémenté et testé — ⚠️ partiel — ❌ non traité (prévu).

---

## Déroulé sportif (articles I.30x)

### I.301 — Ordre des parties dans une poule
Ordres officiels respectés pour toutes les tailles de poule (3, 4, 5, 6) et selon
le nombre de qualifiés (1, 2 ou 3). Exemples vérifiés : poule de 3 avec 1 ou 3
qualifiés → 1-3, 2-3, 1-2 ; poule de 3 avec 2 qualifiés → 1-3, 1-2, 2-3. Règle
anti-arrangement appliquée (mêmes clubs au premier tour). ✅

### I.302 — Placement des joueurs dans une poule
Répartition en serpentin, séparation des clubs, départage aléatoire
**reproductible** des ex æquo (graine conservée). ✅

### I.303 — Classement des joueurs dans une poule
Points 2 (victoire) / 1 (défaite) / 0 (issue non normale : forfait, abandon,
refus de jouer, disqualification). En cas d'égalité, mini-classement du
sous-groupe des ex æquo établi sur leurs seules confrontations directes : total
des points de confrontation, puis quotient de manches, puis quotient de points.
Vérifié empiriquement sur un cycle à trois ex æquo (A bat B, B bat C, C bat A),
correctement départagé. ✅

### I.304 — Placement des joueurs dans un tableau
Cadre du tableau, exemptions (byes), têtes de série tirées au sort par blocs. ✅

### I.305 — Placement des joueurs issus de poules qualificatives
Vainqueur de poule 1 en haut du tableau, de poule 2 en bas ; têtes de série
tirées au sort par blocs (3-4, 5-8, 9-16) ; deuxièmes placés dans la demie opposée
à leur premier ; troisièmes dans la même demie que le deuxième mais le quart
opposé ; séparation des clubs au premier **et au deuxième tour** lorsque le
tableau est incomplet. Tous les alinéas vérifiés. ✅

---

## Inscription et configuration des tableaux (IX.104 / IX.111)

Le règlement de tournoi (IX.111) fixe, tableau par tableau, les joueurs autorisés,
la capacité, les horaires, les tarifs et les récompenses ; IX.104 impose au moins
un tableau féminin.

- **Restriction par classement**, trois modes : tranche de points [min, max],
  plafond de points seul, ou toutes séries. ✅
- **Exclusion des mieux numérotés** au classement national français (par rang, non
  par points), en mode plafond ou toutes séries ; un rang inconnu n'exclut jamais.
  ✅ (le rang national du joueur sera importé ultérieurement)
- **Réservation par sexe** (mixte, dames, ou messieurs) — permet le tableau
  féminin exigé par IX.104. ✅
- **Réservation par catégorie d'âge** (II.302 / II.303 : poussin à V90 ; ensemble
  vide = toutes catégories admises). ✅
- **Capacité et liste d'attente bornée** (taille réglable par tableau, valeur par
  défaut 4) : une inscription sur un tableau plein bascule en liste d'attente, et
  est refusée si la liste est elle aussi pleine. ✅
- **Tarifs d'inscription** distincts en ligne / sur place. ✅ (le paiement en ligne
  réel n'est pas implémenté ; l'espace est prévu)
- **Récompenses** aux premières places (nombre de places libre, par place exacte
  ou par plage, cohérence validée) — précise le mode d'attribution attendu par
  IX.108. ✅
- **Quota de tableaux par jour et par joueur**, avec exception féminine
  configurable (tableau supplémentaire, sans condition ou conditionné à un tableau
  spécial). ✅
- **Clause « pas deux tableaux qui se suivent »** (clause d'organisateur, non
  fédérale) : activable, seuil de proximité paramétrable, orchestrée par le service
  d'inscription. ✅

---

## Pointage, désistement et lancement (jour J, IX.109)

- **Pointage et paiement suivis par tableau** : un même joueur peut être
  pointé/payé sur un tableau et pas sur un autre ; les deux états sont
  indépendants (un inscrit en ligne peut être payé sans être encore pointé). ✅
- **Clôture du pointage** : les inscrits non pointés libèrent leur place (quel que
  soit leur paiement) ; la liste d'attente unique est repêchée dans l'ordre. Tout
  joueur repêché doit repasser par le pointage — c'est là qu'il règle son
  engagement. Le juge-arbitre confirme ou retire à la main. ✅
- **Désistement** : avant la clôture du pointage il libère la place (repêchage) ;
  après clôture il est refusé (le joueur est tenu de jouer, sans remboursement),
  sauf cas particulier validé par le juge-arbitre (blessure…) avec remboursement
  tracé. ✅
- **Lancement du tableau** (IX.109) : possible seulement une fois le pointage
  clôturé ; transforme les présents en poules via le moteur, avec tirage au sort
  reproductible (graine conservée pour rejouer ou vérifier le tirage public). ✅
  Le texte d'IX.109 dit que le juge-arbitre est « responsable direct du tirage au
  sort » ; il ne parle ni de tirage « public » ni de graine. Depuis le 5 novembre
  2026, la graine est **conservée avec le tableau** (`Tableau.getGrainePoules`,
  sauvegardée) et affichée à l'écran du jour J pour que le tirage soit
  vérifiable ; voir `QUESTIONS.md`.
- **Saisie des scores de poule** (`SaisieResultatMatch`, 5 novembre 2026) :
  manche valide selon **2.11.1** (11 points, 2 points d'écart après 10-10) ;
  partie au meilleur de 2n − 1 manches selon **2.12.1** (pas de manche après la
  décision, pas de match non décidé) ; issue non normale (absence à l'appel,
  abandon, refus de jouer, disqualification) : le fautif marque 0 point
  (**I.303 a**). Résultats figés une fois la phase finale construite. ✅
- **Suivi de poule** (`SuiviPoule`) : tour de chaque partie selon les tableaux
  d'**I.301** (1 partie par tour en poule de 3, 2 en poule de 4 ou 5, 3 en poule
  de 6) ; classement en direct I.303, qualifiés acquis seulement poule terminée. ✅
- **Phase finale** (I.304/I.305) : une fois toutes les poules terminées,
  construction du tableau à élimination directe reliée au tableau ; second tirage
  public reproductible, distinct de celui des poules. ✅

---

## Discipline (article 3.5.2)

Cartons et points de pénalité (jaune, puis pénalités progressives), cartons hors
match, et règle 3.5.2.9 : deux disqualifications entraînent l'exclusion de la
compétition. Une issue non normale d'un match fait marquer zéro point au joueur
fautif et déclare l'adversaire vainqueur. ✅

---

## Tableau de synthèse

| Article | Sujet | État |
|---------|-------|------|
| I.301 | Ordre des parties en poule (3/4/5/6, selon qualifiés) | ✅ |
| I.302 | Placement en poule, départage aléatoire, séparation clubs | ✅ |
| I.303 | Classement 2/1/0, 0 pour issue non normale, départages | ✅ |
| I.304 | Placement tableau, têtes de série tirées au sort | ✅ |
| I.305 | Placement des qualifiés (2es demi opposé, 3es quart opposé) | ✅ |
| IX.104 | Au moins un tableau féminin | ✅ |
| IX.109 | Lancement, tirage au sort (graine conservée et affichée) | ✅ |
| 2.11.1 / 2.12.1 | Score d'une manche, nombre de manches d'une partie (saisie des scores) | ✅ `SaisieResultatMatch` |
| IX.111 | Inscription, classement, capacité, tarifs, récompenses | ✅ (déroulé) |
| IX.111 | Métadonnées du règlement (responsable, salle, balles, dates, JA…) | ✅ (pré-tournoi) |
| SPID | Champs de la demande d'homologation (correspondant, JA, type, dates, adresse, tableaux, documents PDF) | ✅ |
| 3.5.2 | Cartons, pénalités, 2 disqualifications = exclusion | ✅ |
| IX.101 | Licence loisir / pass'tournoi : tournois promotionnels (et tableaux réservés ailleurs) | ✅ `EligibiliteTournoi` |
| IX.102 | Régional : ligue du club organisateur ; départemental : son département ; aucune dérogation | ✅ `EligibiliteTournoi` |
| II.302/II.303 | Catégorie d'âge au 1er janvier de la saison (grille 2026-2027 vérifiée) | ✅ `CategorieAge.pour` |

---

## Réserves connues

Aucune n'est bloquante pour le déroulé normal d'un tournoi.

- **I.303.e** — En cas d'égalité *vraiment* persistante après tous les quotients,
  le règlement prévoit un tirage au sort. Ce cas, rarissime, est laissé à la
  décision de l'organisateur et n'est pas automatisé.
- **Séparation des clubs au-delà du deuxième tour** — non contrainte (non exigée
  par le règlement).
- **Validation grandeur nature** par un juge-arbitre sur un vrai tournoi : pas
  encore réalisée. Tout est validé par simulation et tests unitaires (276 tests).

---

## Homologation et cycle de vie du tournoi (pré-tournoi)

Le **cycle de vie** est implémenté : brouillon → homologué → publié (+ annulé).
La logique s'aligne sur le formulaire officiel **SPID V2** (« Mon Espace Club →
Demande d'homologation de tournois »), avec une **complétude à trois niveaux** :

- **Obligatoire pour l'homologation** (les champs `*` du formulaire SPID) :
  correspondant (nom, prénom, email) ; juge-arbitre (nom, prénom, email, licence,
  grade) ; tournoi (nom, type, dates début/fin) ; adresse (voie, code postal,
  ville) ; au moins un tableau, dont un tableau féminin (IX.104), chaque tableau
  ayant nom, type et date.
- **Obligatoire pour la publication** (décision produit, pour faire tourner le
  tournoi) : niveau, nombre de tables, numéro d'homologation délivré par la FFTT,
  document de règlement (PDF), politique des balles, clôtures des inscriptions et
  du pointage, début des matchs, et chaque tableau complet (format + capacité).
- **Facultatif** : téléphones, autres infos, site web, dotation (calculée
  automatiquement), résidence/lieu-dit, feuille d'engagement, marque des balles,
  aire de jeu, heure de fin, horaire des finales, tirage public.

Métadonnées portées par le modèle (`InfosReglement`, IX.111) : responsable, salle,
tables, balles + **politique des balles**, **aire de jeu**, dates clés, **horaire
des finales**, **tirage au sort public** (date/heure/lieu). Le juge-arbitre au
grade insuffisant pour le niveau produit un **avertissement non bloquant**.

## Ce qui reste à traiter (au-delà du pré-tournoi)

- **Génération du règlement PDF** : produire un règlement officiel type à partir
  des métadonnées (le modèle porte déjà une `DocumentReference` prête à recevoir
  soit ce PDF généré, soit un PDF fourni par le club).
- **Homologation et remontée fédérale** (SPID V2 / GIRPE) : le logiciel rassemble
  les paramètres et suit l'état, mais ne délivre pas le numéro (rôle de la FFTT) ;
  le format d'échange GIRPE n'est communiqué que lors de l'homologation.
- **Persistance** : **faite**. Sauvegarde/rechargement local d'un tournoi complet
  **et de son registre disciplinaire** (cartons et exclusions 3.5.2.9 préservés
  au rechargement), en JSON versionné sans dépendance, avec écriture atomique et
  autosave 15 min + reprise sur incident. La traçabilité disciplinaire survit donc
  désormais à une fermeture ou à une coupure en pleine compétition.
