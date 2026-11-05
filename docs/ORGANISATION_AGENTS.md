# Organisation des agents de PingManager

## L'équipe et la chaîne

Une session « chef de projet » (skill `piloter-projet`) prend une tâche du
carnet (`docs/backlog/`), la fait passer d'agent en agent, puis la confie à
l'intégrateur. Chaque agent laisse un compte rendu court dans la section
« Passations » du fichier de la tâche : c'est la **liaison** entre eux.

| Rôle demandé | Agent | Modèle |
|---|---|---|
| Implémentation des idées | `developpeur` (et `developpeur-jour-j` pour la salle) | celui de la session |
| Interfaces (écrans) | `developpeur-interface` | celui de la session |
| Charte graphique | `designer-interface` | celui de la session |
| Vérification du modèle | `verificateur-modele` | Sonnet |
| Correction des erreurs | `correcteur` | celui de la session |
| Vérification du nouveau code et des liaisons | `relecteur-code` | Sonnet |
| Documentation | `documentaliste` | Haiku |
| Règlements officiels | `controleur-conformite` (+ `chercheur-fftt` pour le web) | Sonnet |
| Push, arborescence, reprise de la dernière mise à jour, fusion | `integrateur` | Sonnet |
| Point d'ensemble pour Quentin | `recapitulateur` | Sonnet |

Exemple de chaîne pour une idée avec écran :
`developpeur` → `developpeur-interface` → `designer-interface` →
`verificateur-modele` → `relecteur-code` → (`correcteur` si défaut, puis
nouvelle vérification) → `documentaliste` → `integrateur`.

## Lancer

- **À la main** : nouvelle session sur claude.ai/code, dépôt
  `Gestion_Tournoi_FFTT`, consigne : « Avance le projet. » (2 tâches au plus
  par session).
- **Sans toi** : une tâche planifiée qui envoie la même consigne à heures
  fixes (par exemple 23 h, 3 h et 7 h). Chaque passage prend les tâches
  suivantes du carnet ; une tâche déjà prise (branche `tache/…`) est laissée
  aux autres.
- **Le matin** : `outils-dev\mettre-a-jour.cmd` (récupère et affiche ce qui
  a été fait), ou une session « Utilise l'agent `recapitulateur` et fais le
  point ».

## Économiser les jetons

Ce qui coûte : chaque agent **repart de zéro** (il relit `CLAUDE.md`, sa
skill, les fichiers utiles). Huit agents sur une tâche coûtent donc nettement
plus qu'un seul. Ce qui est en place pour limiter :

1. **Modèles moins chers** pour les rôles de vérification, de relecture et de
   documentation (Sonnet, Haiku), le modèle principal étant réservé au code.
2. **La chaîne saute les étapes inutiles** (pas de designer pour un bug, pas
   de relecteur pour une tâche de documentation).
3. **2 tâches au plus par session**, 2 allers-retours de correction au plus.
4. **Règles de lecture économe** dans `CLAUDE.md` (pas de gros fichier lu en
   entier, sorties de tests filtrées, comptes rendus de 5 à 10 lignes).
5. **Des tâches bien décrites** dans le carnet : une consigne précise évite
   à l'agent de chercher, et c'est le premier facteur d'économie.

Ce que tu peux régler toi-même : l'effort de la session (en bas de l'écran :
« Faible » ou « Moyen » suffit pour la documentation et les petits
correctifs) et le nombre de passages planifiés par nuit. Avec l'abonnement
Pro, la limite d'utilisation est vite atteinte si plusieurs longues sessions
tournent ; l'abonnement Max est fait pour un usage de plusieurs heures par
jour.

## Des agents « à toi », sans Claude ?

C'est possible avec des **modèles ouverts installés sur ton PC** : un outil
comme Ollama fait tourner le modèle, un outil comme Aider, OpenHands ou
Continue le fait travailler sur le dépôt. Rien n'est facturé à l'usage.

À savoir avant de se lancer :
- il faut une **carte graphique** solide : les modèles de code de taille
  moyenne demandent en pratique 12 à 24 Go de mémoire vidéo, sinon c'est très
  lent ;
- sur un projet de cette taille (plus de 250 classes, plus de 600 tests,
  règles FFTT, conventions strictes), ces modèles locaux font nettement plus
  d'erreurs, surtout pour les modifications sur plusieurs fichiers et les
  résolutions de conflits. Un mauvais agent coûte alors du temps de
  correction ;
- les skills, les agents et le circuit de ce dépôt sont faits pour Claude
  Code : il faudrait les adapter (Aider, par exemple, peut lire un fichier de
  conventions comme `CLAUDE.md`).

Compromis raisonnable : garder Claude pour le code et l'intégration, et, si
tu veux essayer un modèle local, lui confier d'abord des tâches mécaniques et
faciles à vérifier (relecture de textes, documentation), sur une branche, en
gardant le même circuit (tests et GitHub Actions avant toute fusion).
