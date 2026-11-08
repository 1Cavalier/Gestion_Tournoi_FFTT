---
id: B018
titre: Prononciation des noms en alphabet phonétique
type: idee
priorite: P2
taille: M
statut: fait
depend-de: [B017]
fichiers: [core (registre de prononciations), persistence/DepotApplication, ui/ecran/PageCompte, ui/ecran/EcranJourJ]
branche: tache/B018-prononciation
pr: 73
---

## Ce qu'il faut faire

Pour les noms mal prononcés par la synthèse vocale :
- un **registre de prononciations** (par licence pour les joueurs, par numéro de club pour les clubs), en **alphabet phonétique international (API / IPA)**, conservé dans les données de l'application (réutilisé d'un tournoi à l'autre) ;
- dans le **profil** du joueur (`PageCompte`), une partie **« Appel »** : sa prononciation phonétique, avec un bouton « Écouter » ;
- le juge-arbitre peut aussi la saisir ou la corriger depuis le jour J ;
- l'annonce (B017) utilise la balise SSML `phoneme` (alphabet IPA) quand une prononciation existe, sinon le texte normal ;
- aide à la saisie : quelques exemples et les symboles courants du français.

## Critères de fin

- [x] registre persisté (test aller-retour)
- [x] SSML produit avec la prononciation (test)
- [x] partie « Appel » dans le profil
- [x] `mvn -B test` à 0 échec

## Contraintes / décisions de Quentin

La partie « Appel micro : prononciation phonétique » du profil est mise en place par B029 ; cette tâche fournit la saisie, le registre et l'écoute.

Aucune pour l'instant.

## Passations

- 2026-11-08 `developpeur` : registre (cœur + JSON applicatif rétrocompatible), validation API, résolveur branché au démarrage, profil « Appel micro » (saisie, Écouter, aide), écran jour J « Prononciation des noms… » (menu Voix). 3 commits, 1388 tests verts (tests.sh).
- Points d'attention : `AnnonceVocale` a un 5e composant `licences` (constructeur à 4 arguments gardé) ; `TexteAnnonceVocale.nomPropre` devenu public ; constante `PresentationProfil.APPEL_A_VENIR` retirée (inutilisée).
- Non vérifié : voix réelle (Windows) et écran jour J à l'œil ; profil capturé sous Xvfb (correct). 3 questions dans le journal `docs/journal/2026-11-08-tache-B018-prononciation.md`.

- 2026-11-08 `verificateur-modele` : 2 classes de tests ajoutées (18 tests, 1410 au total, 4 en échec voulu). Injection SSML impossible (balayage de tous les caractères), registre, résolveur et ancien fichier OK.
- Défauts : `ValidationPrononciation` refuse é précomposé mais accepte e+U+0301 et refuse 2 diacritiques empilés (mineurs) ; `DepotApplication.lirePrononciations` (l.~425) lève `JsonException` sur un élément non objet ou une section de mauvais type : démarrage bloqué au lieu d'ignorer.

## Résultat

Registre IPA persisté dans les données de l'application, résolveur SSML branché, carte « Appel micro » du profil (saisie, Écouter, aide), écran jour J « Prononciation des noms… ». 1412 tests verts. Voix réelle à vérifier sous Windows. Détails : `docs/journal/2026-11-08-tache-B018-prononciation.md`.
