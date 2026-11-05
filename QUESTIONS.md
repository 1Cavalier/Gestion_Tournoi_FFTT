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

## Tranché
