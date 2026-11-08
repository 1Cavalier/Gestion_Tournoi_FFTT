---
id: B021
titre: Suivi du tournoi sur téléphone
type: idee
priorite: P2
taille: L
statut: en-cours
depend-de: []
fichiers: [nouveau package de diffusion hors cœur, core (vue publique en lecture), ui/ecran/EcranJourJ]
branche: tache/B021-suivi-telephone
pr:
---

## Ce qu'il faut faire

Les joueurs et le public suivent sur leur **téléphone**, sans application à installer :
- un petit **serveur web local en lecture seule** lancé par PingManager le jour J (classe `com.sun.net.httpserver` du JDK : **aucune dépendance**), accessible sur le Wi-Fi de la salle ;
- pages simples et rapides (charte v2, adaptées au téléphone) : poules (résultats, classement), matchs en cours et à venir avec la table, tableaux finaux, et **« Ma prochaine partie »** (recherche par nom ou licence : contre qui, quelle table, quand) ; rafraîchissement automatique ;
- adresse affichée en grand sur l'écran du juge-arbitre (et imprimable) ;
- sécurité : lecture seule, aucune donnée personnelle au-delà de nom, club, points ; pas de téléphone ni de date de naissance ; port configurable ; arrêt propre.

## Critères de fin

- [ ] pages servies et testées (contenu HTML échappé)
- [ ] « Ma prochaine partie » juste sur la démo
- [ ] aucune écriture possible depuis le réseau (test)
- [ ] `mvn -B test` à 0 échec

## Contraintes / décisions de Quentin

**QR code** : le générer demande soit une bibliothèque (dépendance, accord de Quentin), soit un encodeur écrit à la main. En attendant, afficher l'adresse en grand. Question à poser dans le journal.

## Passations

- developpeur (2026-11-08) : package `ui.diffusion` (instantané immuable pris sur le fil JavaFX, pages HTML échappées sans JS, serveur JDK GET/HEAD seulement, adresse du réseau local, port configurable) + carte « Suivi sur téléphone » dans `EcranJourJ` (adresse en grand, affiche imprimable, arrêt en quittant le jour J et dans `ContexteApplication.fermer()`).
- Tests : `InstantaneSuiviTest`, `PagesSuiviTest` (échappement, démo, aucune licence affichée), `ServeurSuiviPublicTest` (vrai serveur port 0 : POST/PUT/DELETE/PATCH → 405, HEAD, 414, arrêt) ; 1144 verts via `outils-dev/tests.sh`.
- À relire : écran non vérifié visuellement (carte, affiche, impression) ; 4 questions dans le journal (QR code, port enregistré, arrêt en quittant le jour J, recherche par licence).
- auditeur-securite (2026-11-08) : serveur de suivi audité (méthodes, chemins, requêtes, en-têtes, adresse, fuites). Défauts : 4 connexions lentes gèlent les 4 fils (`ServeurSuiviPublic.java:71`, moyenne ; correctif : `sun.net.httpserver.maxReqTime`/`maxRspTime` ou délais + plus de fils) ; 500 sans journalisation (`:155`, faible) ; repli IPv4 publique (`AdresseReseauLocal.java:30`, faible) ; recherche par licence exacte = énumération licence→nom (`PagesSuivi.java:251`, faible, à décider avec Quentin).
- Tests : `ServeurSuiviSecuriteTest` (7 tests dont le gel par connexions lentes, qui échouera une fois corrigé : à retourner) ; 1151 verts.

## Résultat
