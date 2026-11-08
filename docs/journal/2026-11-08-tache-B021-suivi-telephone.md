# Suivi du tournoi sur téléphone (B021) — 2026-11-08

- Branche : `tache/B021-suivi-telephone`
- Agent : `developpeur` (étape de pipeline)
- Pull Request : #<n> (complété par l'intégrateur)
- Statut : fait (fusion par l'intégrateur)

## Pour Quentin

Le jour J, le juge-arbitre peut ouvrir un **suivi sur téléphone** : un petit
site web servi par PingManager sur le Wi-Fi de la salle, sans application à
installer. Les joueurs et le public y voient les poules (résultats et
classement), les matchs en cours avec leur table, la file des matchs à venir,
les tableaux finaux et **« Ma prochaine partie »** (recherche par nom ou
licence : contre qui, quelle table, place dans la file). Les pages se
rafraîchissent toutes seules toutes les 30 s. C'est de la **consultation
seule** : rien ne peut être modifié depuis un téléphone, et seuls le nom, le
club et les points des joueurs apparaissent. L'adresse s'affiche en grand sur
l'écran du jour J, avec une affiche imprimable.

## À tester à l'écran

- Compte de démo : celui de Quentin (tournoi de démonstration du jour J).
- Chemin : Gestion des tournois → Tournoi de démonstration du jour J → Jour J →
  carte « Suivi sur téléphone » (sous « Où en sont les tableaux ») → « Ouvrir le
  suivi sur téléphone » ; puis « Afficher en grand et imprimer ».
- Ce qu'il faut regarder : l'adresse affichée (192.168.x.x:8080), l'ouvrir sur
  un téléphone relié au même Wi-Fi (pare-feu Windows : autoriser Java au
  premier lancement), « Ma prochaine partie » pour un joueur des tables 2 / 5,
  thème clair et sombre (le téléphone suit son propre réglage), l'arrêt en
  quittant le jour J. **Non vérifié visuellement en session cloud.**

## Questions pour Quentin

### 2026-11-08 — QR code de l'adresse du suivi
- Contexte : un QR code sur l'affiche éviterait de taper l'adresse. Le générer
  demande une bibliothèque (nouvelle dépendance) ou un encodeur QR écrit à la
  main (quelques centaines de lignes, à tester soigneusement).
- Article FFTT : sans objet.
- Options : A) adresse en grand seulement (actuel) ; B) encodeur QR maison sans
  dépendance ; C) bibliothèque (ZXing ou équivalent), avec votre accord.
- Choix provisoire de l'agent : A.
- Réponse de Quentin :

### 2026-11-08 — Port du suivi enregistré ?
- Contexte : le port (8080 par défaut) se change dans la carte du jour J ; il
  est gardé tant que l'application est ouverte, pas d'une session à l'autre.
- Article FFTT : sans objet.
- Options : A) en mémoire seulement (actuel) ; B) enregistré dans les réglages
  de l'application ; C) enregistré par tournoi.
- Choix provisoire de l'agent : A (réversible).
- Réponse de Quentin :

### 2026-11-08 — Arrêt du suivi en quittant le jour J
- Contexte : le suivi s'arrête dès que la page Jour J quitte la fenêtre (retour
  à la vue d'ensemble, autre rubrique) et à la fermeture de l'application ; les
  téléphones voient alors une erreur de connexion jusqu'à la réouverture.
- Article FFTT : sans objet.
- Options : A) arrêt en quittant le jour J (actuel, consigne de la tâche) ;
  B) garder le suivi ouvert tant que l'application tourne, arrêt explicite.
- Choix provisoire de l'agent : A.
- Réponse de Quentin :

### 2026-11-08 — Recherche par numéro de licence
- Contexte : « Ma prochaine partie » accepte le nom ou le numéro de licence
  exact. Quelqu'un qui connaît une licence peut donc voir la prochaine partie de
  ce joueur (information de toute façon publique dans la salle). La licence
  n'est jamais affichée.
- Options : A) nom ou licence (actuel, demandé) ; B) nom seulement.
- Choix provisoire de l'agent : A.
- Réponse de Quentin :

## Notes pour l'état du projet

- Nouveau package `ui.diffusion` (JDK `com.sun.net.httpserver`, aucune
  dépendance, ni JavaFX ni persistance) : `InstantaneSuivi` (photographie
  immuable prise sur le fil JavaFX), `PagesSuivi` (HTML échappé, sans
  JavaScript, meta refresh 30 s, charte v2 avec thème sombre automatique,
  GET/HEAD seulement, 405 sinon), `Html` (échappement, paramètres bornés),
  `ServeurSuiviPublic` (16 fils démons, délais de requête 5 s / réponse 15 s, en-têtes CSP / nosniff / no-store, 414
  sur URL trop longue), `AdresseReseauLocal` (IPv4 privée, jamais toutes les
  interfaces), `DiffusionSuivi` (port 1024-65535, motifs de refus, cycle de vie).
- `ContexteApplication.diffusionSuivi()`, arrêté par `fermer()` ;
  `EcranJourJ` : carte « Suivi sur téléphone », adresse en grand, affiche
  imprimable (`PrinterJob`), publication après chaque action, arrêt quand la
  page quitte la fenêtre.
- Format JSON inchangé ; démo inchangée.

## Passations

- correcteur : audit corrigé (16 fils + délais `sun.net.httpserver.maxReqTime/maxRspTime/idleInterval`, connexions lentes coupées ; erreurs journalisées par `System.Logger` sans trace au client ; IPv4 privées seules, plus de repli public).
- correcteur : tests de l'audit retournés pour exiger le bon comportement ; `ServeurSuiviPublicTest` (nouveau sur la branche) attend désormais « vide » pour une IPv4 publique ; 1154 tests, 0 échec.

## Fusion

À compléter par l'intégrateur.

## Chiffres

- Tests : 1116 → 1154 (`outils-dev/tests.sh`, Maven inaccessible ; GitHub Actions fait foi)
- Version de la démo : inchangée
