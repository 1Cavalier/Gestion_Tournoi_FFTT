# Récapitulatif PingManager — 5 novembre 2026, 9 h

> Tenu à jour par l'agent `recapitulateur`. Pour le régénérer, nouvelle
> session Claude Code : « Utilise l'agent `recapitulateur` et fais le point. »

## 1. En bref

- **Le jour J existe enfin à l'écran** : pointage, tirage au sort, poules avec
  saisie des scores et classement en direct, et une vue de phase finale
  jusqu'au podium.
- La phase finale n'est **pas encore reliée** à la page Jour J : on y accède
  par un bouton provisoire.
- L'audit de sécurité trouve **4 problèmes sérieux** à corriger avant toute
  vente (comptes de démo et données réelles livrés dans le logiciel).
- La FFTT a une **API officielle**, mais ses conditions excluent l'usage
  commercial sans accord : il faut lui écrire.
- **13 questions** attendent ta réponse dans `QUESTIONS.md`.

## 2. Livré cette nuit (Pull Requests fusionnées)

| PR | Ce que ça change pour toi |
|---|---|
| #6 Jour J | Nouvelle page « Jour J » par tournoi : pointage (présent / payé, liste d'attente, désistement, clôture), tirage au sort des poules avec graine affichée, saisie des matchs de poule manche par manche, forfaits et abandons, classement en direct. Tournoi de démonstration prêt (14 novembre 2026). |
| #7 Phase finale | Construction du tableau à élimination une fois les poules finies, saisie des scores, progression automatique des vainqueurs, petite finale, podium. Accès provisoire par un bouton « Phase finale (provisoire) ». |
| #5 Sécurité | `RAPPORT_SECURITE.md` : 23 constats (1 critique, 3 élevés, 11 moyens), un plan de correction en 5 lots, et 14 tests qui démontrent les failles. Aucun code modifié. |
| #4 Recherche FFTT | `docs/RECHERCHE_FFTT.md` : ce qu'offre l'API FFTT, comment l'obtenir, et un courriel prêt à envoyer. |

**Corrigé ce matin** : en fusionnant, la session Jour J avait effacé les 4
questions de la phase finale et son paragraphe dans `ETAT_DU_PROJET.md`. Les
deux sont restaurés, et `CLAUDE.md` impose désormais de garder les entrées des
autres sessions lors d'une fusion.

## 3. À décider par toi

### Bloquant avant toute vente

1. **Données de démo et supervision** (sécurité S-01, S-02). Le logiciel crée
   sur chaque poste le compte ultra admin, ton compte réel (email, mot de
   passe, licence, date de naissance) et l'email de Serge BOULIER.
   **Recommandation** : mode démo activé seulement à la demande, identités
   fictives partout, supervision retirée de la version de bureau.
   Le dépôt est **privé**, donc pas d'urgence sur l'historique Git. En revanche,
   **si le mot de passe de démo est aussi celui d'un vrai compte (Gmail…),
   change-le** : il figure dans le code et dans `docs/passation/`.
2. **Preuve de licence** (S-05) : qui peut lier une licence à un compte ? Un
   code envoyé à l'email connu de la FFTT, ou une validation par le club ?
3. **Envoi d'emails** (S-03) : quel service pour envoyer les codes de
   vérification (Gmail SMTP, Brevo…) ?

### Important (règles sportives, jour J)

4. **Repêchés non pointés au lancement** : l'écran bloque le tirage tant
   qu'ils ne sont ni confirmés ni retirés (choix provisoire). *Recommandation :
   garder.*
5. **Graine du tirage** : 6 chiffres proposés et modifiables, conservés avec
   le tableau. *Recommandation : garder.* Le règlement (IX.109) ne parle ni de
   graine ni de tirage public.
6. **Graine du tableau final non sauvegardée** : elle est perdue à la
   fermeture. *Recommandation : la sauvegarder, comme celle des poules.*
7. **Manches d'un forfait ou d'un abandon** pour le quotient de départage
   (I.303 c) : aucune manche comptée (provisoire), score fictif 3-0, ou manches
   réellement jouées ? À demander à un juge-arbitre.
8. **3e place sans petite finale** : 3es ex aequo (provisoire), départage, ou
   petite finale en option ? *Recommandation : ex aequo, et petite finale en
   option du tableau.*
9. **Place des exempts (byes)** : aujourd'hui, deux exempts peuvent se faire
   face. *Recommandation : donner les byes aux têtes de série, un par paire au
   plus.* À confirmer avec la pratique des JA.
10. **Inscription sur un tableau déjà en pointage ou lancé** : techniquement
    possible aujourd'hui. *Recommandation : refuser en ligne dès l'ouverture du
    pointage, n'autoriser que le juge-arbitre sur place.*
11. **Correction d'un résultat quand la suite est jouée** : refusée
    (provisoire). *Recommandation : garder.*

### Peut attendre

12. **PDFBox dans le cœur** : tolérer (actuel) ou déplacer le rendu PDF hors
    du cœur.
13. **Veille des dépendances** : Dependabot sur GitHub (rien à installer) ou
    un plugin Maven. *Recommandation : Dependabot.*

Pour répondre : écris tes réponses dans `QUESTIONS.md` (ligne « Réponse de
Quentin »), ou donne-les dans une session en disant « applique mes réponses ».

## 4. À tester à l'écran

Sur ton PC : `git pull`, puis `mvn clean javafx:run`. Compte de démo : ton
compte habituel (club de Brunoy).

- **Jour J** : Gestion des tournois → « Tournoi de démonstration du jour J »
  → carte « Jour J ».
  - Tableau B (pointage ouvert) : cocher présent / payé, désister un joueur,
    clôturer, confirmer les repêchés.
  - Tableau D : lancer le tirage, regarder la graine et les poules.
  - Tableau C : saisir des scores (« 11-9 » ou « 9 / -9 »), déclarer un
    forfait, regarder le classement se mettre à jour.
- **Phase finale** : Gestion des tournois → bouton « Phase finale
  (provisoire) » sur un tournoi : construire le tableau, saisir les matchs
  jusqu'au podium.
- Tout regarder en **thème clair et sombre**. Note ce qui te gêne, on
  corrigera par lot.

## 5. Risques et dettes

- Sécurité : lot 1 (bloquant pour la vente) non corrigé.
- Six `switch` sur énumération (risque de casser ton build) et lecture de
  l'heure hors du port `Horloge` : correctifs proposés mais **pas encore
  lancés**.
- Bouton provisoire de phase finale à remplacer.
- `ETAT_DU_PROJET.md` section 4 : GIRPE est cité à tort pour les tournois
  individuels ; c'est SPIDD.
- `README.md` et `CONFORMITE_FFTT.md` : nombres de tests et arborescence
  périmés.
- Nombre de tests après fusion de tous les lots : 524 méthodes `@Test`,
  **non encore mesuré** par `mvn -B test` sur `main`.

## 6. Prochaines tâches proposées

À lancer chacune dans une nouvelle session (« Créer », dépôt
`Gestion_Tournoi_FFTT`). **1, 2 et 3 peuvent tourner en même temps** (fichiers
différents) ; 4 après tes réponses aux questions 1 à 3.

**1. Relier la phase finale au jour J**
> Utilise l'agent `developpeur`. Branche la vue `VuePhaseFinale` sur la page
> Jour J : quand toutes les poules d'un tableau sont terminées, le tableau
> propose « Construire la phase finale », puis affiche la vue. Retire le
> bouton « Phase finale (provisoire) », `AccesPhaseFinaleProvisoire` et
> `TableauEssaiPhaseFinale` s'ils ne servent plus. Mets à jour la démo du
> jour J pour qu'un tableau soit prêt pour la phase finale (incrémente
> `VERSION_DEMO`). Branche `jour-j/phase-finale-reliee`, Pull Request.

**2. Remplacer les switch sur énumération**
> Utilise l'agent `developpeur`. Remplace tous les `switch` sur une
> énumération de `src/main/java` (voir `RAPPORT_ARCHITECTURE.md`) par des
> chaînes de `if` ou un `switch` sur `.name()`, sans changer le comportement.
> Vérifie après compilation qu'aucune classe annexe `$1` liée à un switch
> d'énumération ne subsiste dans `target/classes`. `mvn -B test` à 0 échec.
> Branche `correctif/switch-enumerations`, Pull Request.

**3. Documentation à jour**
> Utilise l'agent `developpeur`. Mets à jour la documentation sans toucher au
> code : `ETAT_DU_PROJET.md` section 4 (SPIDD au lieu de GIRPE pour les
> tournois individuels, d'après `docs/RECHERCHE_FFTT.md`), nombres de tests
> mesurés par `mvn -B test`, arborescence des packages dans `README.md` et
> `ETAT_DU_PROJET.md` section 5, `CONFORMITE_FFTT.md`. Branche
> `doc/mise-a-jour`, Pull Request.

**4. Sécurité, lot 1** (après tes réponses aux questions 1 à 3)
> Utilise l'agent `developpeur`. Applique le lot 1 de `RAPPORT_SECURITE.md`
> (S-01, S-02, S-03, S-12, S-16) selon les réponses de Quentin dans
> `QUESTIONS.md` : mode démo explicite, aucune identité réelle ni compte
> privilégié hors démo, vérification de l'email, politique de mot de passe.
> Inverse les tests `faille_S01` à `faille_S03`. Branche `securite/lot-1`,
> Pull Request.

**5. Port Horloge** (après la tâche 2, mêmes fichiers)
> Utilise l'agent `developpeur`. Fais passer toute lecture de la date ou de
> l'heure du cœur par le port `Horloge` (GestionCartonsMatch, ImportJoueurs,
> JoueurDepuisLicence…), avec des tests à horloge fixe (catégorie d'âge,
> II.302 / II.303). Branche `correctif/port-horloge`, Pull Request.

**Pour toi, hors code** : relire `docs/RECHERCHE_FFTT.md` section 5 et
envoyer le courriel à la FFTT (prêt à copier). C'est ce qui débloquera la
vérification des licences et des points.

## 7. Chiffres

- Pull Requests fusionnées : 7 (dont 4 cette nuit).
- Tests : 524 méthodes `@Test` sur `main` (447 et 475 mesurés sur les lots
  séparés ; total fusionné à mesurer).
- Questions en attente : 13.
- Constats de sécurité ouverts : 23 (1 critique, 3 élevés).
