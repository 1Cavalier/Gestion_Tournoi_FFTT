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
- Réponse de Quentin (5 novembre) : **prévoir la petite finale** → option C,
  réglable par tableau. Sans petite finale, garder A (3es ex aequo) en
  attendant une décision contraire.
- **Traité** (5 novembre, branche `jour-j/arbre-petite-finale`) : case
  « Petite finale (match pour la 3e place) » dans le formulaire du tableau
  (`ConfigurationTableau.avecPetiteFinale`), écrite dans le règlement généré,
  appliquée au tirage du tableau final ; sans elle, 3es ex aequo.

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
- **Traité** (5 novembre, branche `jour-j/arbre-petite-finale`, à la demande
  du plan B de `docs/PLAN_INTERFACE.md`) : option A, champ JSON facultatif
  `graineTableauFinal` (anciens fichiers relus sans graine,
  `RoundTripTournoiTest`), affichée sur le tableau final.

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

### 2026-11-05 — Temps de repos entre deux parties d'un même joueur (salle)
- Contexte : vue « Salle » du jour J (`core.ServiceSalle`, `Ordonnanceur`).
  Un match devient « prêt » dès que ses deux joueurs sont libres, même s'ils
  sortent à l'instant d'une autre table.
- Article FFTT : 3.3.1.2.2 « Le juge-arbitre est responsable de […] la
  programmation des parties en fonction de l'horaire retenu et des tables
  disponibles » ; IX.109 « […] de l'horaire des parties ». Aucun texte trouvé
  dans `docs/reglements/` sur un temps de repos minimal en tournoi individuel
  (recherche « repos », « retard », « minutes »).
- Options : A) aucun repos imposé, le juge-arbitre décide (la priorité favorise
  déjà les joueurs qui attendent depuis longtemps) ; B) délai minimal
  configurable par le club (clause d'organisateur), les matchs concernés
  apparaissant dans « Bloqués » avec le motif « au repos jusqu'à 10 h 42 ».
- Choix provisoire de l'agent : A (comportement de l'ordonnanceur inchangé).
- Réponse de Quentin :

### 2026-11-05 — Autosave de session et reprise à l'écran
- Contexte : vue « Salle ». Chaque action (envoi sur une table, annulation,
  score) enregistre aussitôt le fichier du tournoi, qui porte déjà le statut
  « en cours », le numéro de table et l'heure de début : rouvrir le tournoi
  rend la salle telle qu'elle était (testé). L'autosave de session
  (`ServiceAutosave`, `SessionSalle`) sait aussi la sauvegarder et la
  reprendre (testé), mais l'interface ne l'active pas et ne propose pas de
  reprise au démarrage (situation antérieure à ce lot).
- Options : A) garder l'enregistrement immédiat du tournoi comme seule source
  (choix actuel) ; B) activer en plus l'autosave de session pendant le jour J
  et proposer la reprise au démarrage (utile surtout pour l'apprentissage des
  durées de match, qui n'est pas encore affiché).
- Choix provisoire de l'agent : A (une seule source de vérité, pas de risque
  de divergence entre deux fichiers).
- Réponse de Quentin :

### 2026-11-05 — Durée « longue » d'une partie en salle
- Contexte : carte d'une table, la durée passe en rouge au-delà de 40 minutes
  (`PresentationSalle.DUREE_LONGUE`), pour attirer l'œil du juge-arbitre.
- Options : A) seuil fixe de 40 min ; B) seuil réglable par tournoi ; C) seuil
  tiré de l'estimateur de durées (`EstimateurDuree`, selon la phase et le
  niveau).
- Choix provisoire de l'agent : A (affichage seulement, aucune règle).
- Réponse de Quentin :

## Tranché

### 2026-11-05 — Abandon ou disqualification en demi-finale, puis petite finale
- Contexte : `core/ServicePhaseFinale`, `ProgressionTableau`. Avec une petite
  finale, le perdant d'une demi-finale y est placé, quelle que soit l'issue de
  la demie : un joueur qui a déclaré forfait, abandonné (blessure) ou été
  disqualifié en demi-finale se retrouve en petite finale.
- Article FFTT : rien pour les tournois (chapitre IX). 3.5.3.1.1 autorise
  l'abandon pour maladie ou blessure ; 3.5.2.10 permet au juge-arbitre de
  disqualifier « pour le restant de la compétition ». III.109 (abandon :
  « considéré battu pour la partie ou les parties qui lui reste(nt) à
  disputer ») vise le Critérium fédéral.
- Options : A) le fautif est placé en petite finale ; le juge-arbitre y
  déclare ensuite son forfait (rien d'automatique) ; B) après un abandon, une
  disqualification ou un forfait en demie, la petite finale est donnée
  automatiquement à l'autre demi-finaliste battu ; C) B seulement pour la
  disqualification (3.5.2.10), A sinon.
- Choix provisoire de l'agent : A (aucune décision automatique, réversible,
  le juge-arbitre garde la main ; test
  `PetiteFinaleParTableauTest.petite_finale_attend_ses_deux_joueurs_avant_d_etre_jouable`).
- Réponse de Quentin :
