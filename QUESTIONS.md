# Questions ouvertes pour Quentin

Les agents notent ici les choix qu'ils n'ont pas voulu trancher seuls (règle
FFTT ambiguë, décision produit). Quentin répond sous chaque question, puis
l'entrée est déplacée dans « Tranché » ou supprimée.

Format d'une entrée :

```
### AAAA-MM-JJ — Titre court
- Contexte : tâche / fichier concerné
- Article FFTT : numéro + extrait court (si applicable)
- Options : A) … B) …
- Choix provisoire de l'agent : …
- Réponse de Quentin :
```

## En attente

### 2026-11-05 — PDFBox dans le cœur
- Contexte : `core/reglement/RenduPdfReglement.java` importe PDFBox, alors que
  la règle d'or veut un cœur sans dépendance externe.
- Options : A) tolérer (situation actuelle, documentée dans `CLAUDE.md`) ;
  B) déplacer le rendu PDF dans un package hors cœur (par exemple
  `com.pingmanager.export.pdf`), le cœur ne produisant que le
  `DocumentReglement`.
- Choix provisoire : A, sans l'étendre.
- Réponse de Quentin :

### 2026-11-05 — Lancer un tableau avec des repêchés qui n'ont pas pointé
- Contexte : écran de pointage du jour J (`EcranPointageTableau`),
  `ServiceLancementTableau.lancer`. Après la clôture, les repêchés sont inscrits
  mais « à pointer ». Le cœur les place en poule s'ils sont encore là au
  lancement (comportement inchangé).
- Options : A) l'écran bloque le tirage tant qu'un repêché n'a pas été confirmé
  ou retiré ; B) l'écran avertit seulement ; C) le lancement retire
  automatiquement les repêchés non pointés.
- Choix provisoire de l'agent : A (à l'écran seulement, le cœur n'est pas
  modifié ; `ResultatVerificationLancement.getNonPointes` donne la liste).
- Réponse de Quentin :

### 2026-11-05 — Tirage « public » et graine (IX.109)
- Contexte : carte « Tirage au sort des poules ».
- Article FFTT : IX.109 « […] un juge-arbitre qui est le responsable direct du
  tirage au sort […] ». Le texte ne parle ni de tirage public ni de graine.
- Options : A) graine de 6 chiffres proposée, modifiable, affichée et conservée
  avec le tableau (même graine + mêmes présents = même tirage) ; B) graine
  imposée par le logiciel, non modifiable ; C) graine tirée physiquement devant
  les joueurs et saisie par le JA (possible avec A).
- Choix provisoire de l'agent : A.
- Réponse de Quentin :

### 2026-11-05 — Manches d'un match perdu par forfait ou abandon
- Contexte : saisie d'une issue non normale (`SaisieResultatMatch.enregistrerIssue`).
- Article FFTT : I.303 a) (0 point pour le fautif) ; I.303 c) départage par
  « le quotient des manches gagnées par les manches perdues » entre ex æquo. Le
  texte ne dit pas quelles manches compter pour une partie non jouée ou
  abandonnée.
- Options : A) aucune manche enregistrée (0-0 pour le quotient) ; B) score
  fictif au meilleur des manches (3-0, 11-0) au profit du vainqueur ;
  C) manches réellement jouées avant l'abandon.
- Choix provisoire de l'agent : A (les manches déjà saisies sont effacées quand
  on déclare l'issue). Réversible : changement localisé dans `SaisieResultatMatch`.
- Réponse de Quentin :

### 2026-11-05 — Inscriptions sur un tableau déjà en pointage ou lancé
- Contexte : `ServiceInscription` ne regarde pas le statut du tableau ; une
  inscription en ligne reste techniquement possible sur un tableau en pointage
  ou lancé (le joueur n'apparaîtrait pas dans les poules).
- Options : A) refuser l'inscription dès l'ouverture du pointage ; B) n'autoriser
  que l'inscription sur place par le JA pendant le pointage ; C) laisser tel quel.
- Choix provisoire de l'agent : C (non modifié dans ce lot, hors périmètre),
  signalé pour décision.
- Réponse de Quentin :

### 2026-11-05 — 3e place sans petite finale
- Contexte : vue « Phase finale » (`core/ServicePhaseFinale.podium`,
  `ui/ecran/PresentationPhaseFinale.lignesPodium`). Le seul format du
  registre (P3Q2) ne prévoit pas de petite finale : en pratique, aucun
  tableau n'en a aujourd'hui.
- Article FFTT : rien dans le chapitre IX (tournois, IX.101 à IX.111) ni en
  I.304 / I.305. La règle « lorsque plusieurs places ne sont pas jouées, les
  joueurs concernés marquent le même nombre de points » (barème de points du
  chapitre III, après III.501) concerne le Critérium fédéral, pas les tournois.
- Options : A) les deux demi-finalistes battus sont 3es ex aequo ;
  B) départage (points-classement, ou adversaire battu par le vainqueur) ;
  C) proposer une petite finale par tableau (nouveau format ou option du
  tableau, à afficher dans le règlement).
- Choix provisoire de l'agent : A (affiché « 3e ex aequo », avec une note).
  Si une petite finale existe, son vainqueur est seul 3e et son perdant 4e.
- Réponse de Quentin :

### 2026-11-05 — Graine du tirage du tableau final non enregistrée
- Contexte : `ServicePhaseFinale.construire` renvoie la graine du second
  tirage ; la vue l'affiche, mais le modèle `Tableau` ne la garde pas et le
  fichier JSON non plus. Après fermeture du logiciel, la vue indique « graine
  non conservée ».
- Article FFTT : IX.109 (le juge-arbitre est responsable du tirage au sort).
- Options : A) ajouter la graine au tableau et au JSON (champ facultatif,
  anciens fichiers relus sans elle, test `RoundTripTournoiTest`) ; B) en rester
  à l'affichage pendant la session.
- Choix provisoire de l'agent : B (pas de changement du format de
  sauvegarde sans accord).
- Réponse de Quentin :

### 2026-11-05 — Place des exempts (byes) dans le tableau final
- Contexte : `core/PlacementQualifiesService` (existant, non modifié) place
  les 1ers puis les 2es par tirage ; les places restées vides deviennent des
  byes, où qu'elles soient. Constaté avec la vue : deux byes peuvent se faire
  face au premier tour (un joueur est alors exempté deux tours de suite, ou
  le vainqueur d'un 1er tour saute le 2e), et deux exemptés peuvent se
  rencontrer au 2e tour (exemple : 26 qualifiés dans un tableau de 32).
- Article FFTT : I.304 / I.305 règlent les têtes de série, les 2es de poule
  et les clubs (y compris au 2e tour si le tableau est incomplet), mais ne
  disent pas où mettre les exempts.
- Options : A) laisser tel quel ; B) donner les byes en priorité aux têtes
  de série (1ers de poule), un bye par paire du premier tour au plus, comme
  dans les tableaux habituels ; C) demander la pratique du juge-arbitre / de
  GIRPE.
- Choix provisoire de l'agent : A (aucun changement du tirage), l'affichage
  gère tous les cas.
- Réponse de Quentin :

### 2026-11-05 — Corriger un résultat quand la suite est jouée
- Contexte : vue « Phase finale ». Un score peut être corrigé ; si la
  correction change le vainqueur, les matchs suivants non joués sont mis à
  jour. Si un match suivant est déjà en cours ou joué, la correction est
  refusée (`SUITE_DEJA_JOUEE`).
- Options : A) refus (actuel) ; B) proposer d'annuler en cascade les matchs
  suivants (avec confirmation du juge-arbitre).
- Choix provisoire de l'agent : A, le plus prudent.
- Réponse de Quentin :

### 2026-11-05 — Données de démo et supervision (sécurité S-01, S-02)
- Contexte : `RAPPORT_SECURITE.md`. Le compte ultra admin, le compte réel de
  Quentin et l'email de Serge BOULIER sont créés sur chaque poste.
- Options : A) mode démo activé à la demande, identités fictives partout,
  supervision retirée de la version de bureau ; B) mode démo, mais supervision
  gardée avec un rôle stocké et un mot de passe choisi à l'installation.
- Recommandation : A.
- Réponse de Quentin :

### 2026-11-05 — Preuve de possession d'une licence (sécurité S-05)
- Contexte : n'importe quel compte peut se lier à n'importe quelle licence.
- Options : A) code envoyé à l'email connu de la FFTT (dépend de l'API FFTT) ;
  B) validation par le club du joueur ; C) A quand l'API sera disponible, B
  en attendant.
- Réponse de Quentin :

### 2026-11-05 — Service d'envoi d'emails (sécurité S-03, S-16)
- Contexte : nécessaire pour vérifier les emails et envoyer les codes.
- Options : SMTP d'un compte dédié (Gmail, OVH…), service transactionnel
  (Brevo, Mailjet…). Identifiants hors du dépôt dans tous les cas.
- Réponse de Quentin :

### 2026-11-05 — Veille des dépendances (sécurité S-11)
- Options : A) Dependabot sur GitHub (aucune dépendance ajoutée au projet) ;
  B) plugin Maven OWASP dependency-check.
- Recommandation : A.
- Réponse de Quentin :

### 2026-11-05 — Lien entre `importation` et `persistence.json` (audit d'architecture M1)
- Contexte : `RAPPORT_ARCHITECTURE.md` (M1, action 3) et PR #2.
  `importation/LecteurTableur.java` importe `persistence.json.Json` et
  `JsonParser`, alors que `CLAUDE.md` présente `importation` comme « sans
  dépendance ». Le rapport demandait d'inscrire ce choix ici ; ajouté par le
  récapitulatif du 5 novembre.
- Options : A) documenter ce lien (dans `CLAUDE.md` et la skill
  d'architecture), sans toucher au code ; B) extraire le lecteur/écrivain JSON
  dans un package neutre (`com.pingmanager.json`) utilisé par les deux.
- Choix provisoire : A (rien n'est modifié tant que Quentin n'a pas tranché).
- Réponse de Quentin :

### 2026-11-05 — GIRPE ou SPIDD pour la remontée des tournois individuels
- Contexte : `docs/RECHERCHE_FFTT.md` et PR #4. `ETAT_DU_PROJET.md`
  section 4 dit que la remontée officielle passe par GIRPE ; la recherche
  indique que GIRPE sert aux rencontres par équipes et que les tournois
  individuels passent par **SPIDD** (logiciel du juge-arbitre). La PR #4
  demandait confirmation avant de corriger. Ajouté par le récapitulatif du
  5 novembre.
- Options : A) corriger `ETAT_DU_PROJET.md` section 4 (SPIDD) ; B) attendre la
  réponse de la FFTT au courriel prévu.
- Recommandation : A (la correction est documentaire et réversible ; le
  format d'échange reste à obtenir de la FFTT dans les deux cas).
- Réponse de Quentin :

## Tranché
