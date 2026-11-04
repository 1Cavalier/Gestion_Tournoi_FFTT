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

## Tranché
