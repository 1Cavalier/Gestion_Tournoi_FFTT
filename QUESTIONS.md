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

## Tranché
