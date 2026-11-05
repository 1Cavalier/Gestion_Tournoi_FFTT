# Récapitulatif PingManager — 5 novembre 2026, 9 h 30

> Tenu à jour par l'agent `recapitulateur`. Pour le régénérer, nouvelle
> session Claude Code : « Utilise l'agent `recapitulateur` et fais le point. »
> Remplace le récapitulatif de 9 h (PR #8).

## 1. En bref

- **Tout est fusionné** : 8 Pull Requests sur `main`, aucune ouverte, aucune
  branche en avance sur `main`. Les tests sont **tous verts : 524, 0 échec**
  (mesuré ce matin).
- Le **jour J** existe à l'écran (pointage, tirage, poules). La **phase
  finale** aussi, mais on l'ouvre encore par un bouton provisoire.
- **4 failles sérieuses** restent à corriger avant toute vente. Elles viennent
  des comptes de démo et de données réelles livrés dans le logiciel.
- **15 questions** attendent ta réponse. 3 d'entre elles bloquent la vente.

## 2. Livré depuis le dernier point

Depuis le récapitulatif de 9 h, seule la PR #8 (ce récapitulatif et la
documentation restaurée) a été fusionnée. Pour mémoire, voici ce que la nuit
a livré :

| PR | Ce que ça change pour toi |
|---|---|
| #6 Jour J | Nouvelle page « Jour J » par tournoi : pointage (présent, payé, liste d'attente, désistement, clôture), tirage au sort des poules avec une graine affichée, saisie des scores manche par manche, forfaits et abandons, classement de poule en direct. Un tournoi de démonstration est prêt (Brunoy, 14 novembre 2026). |
| #7 Phase finale | Le tableau à élimination se construit quand les poules sont finies. Les scores se saisissent, les vainqueurs avancent seuls, et le podium s'affiche. On y accède encore par le bouton « Phase finale (provisoire) ». |
| #5 Sécurité | `RAPPORT_SECURITE.md` : 23 constats et un plan de correction en 5 lots. 14 tests montrent les failles. Le code n'a pas été modifié. |
| #4 Recherche FFTT | `docs/RECHERCHE_FFTT.md` : ce qu'offre l'API de la FFTT, comment y accéder, et un courriel prêt à envoyer. |
| #8 Récapitulatif | Agent `recapitulateur`. Il a aussi restauré les questions de la phase finale, que la fusion du jour J avait effacées. |
| #1, #2, #3 | Mise en place (consignes Claude, agents, audit d'architecture, plan de nuit). |

**État des branches** : en plus de `main`, il y a 9 branches distantes, et
aucune n'a de commit absent de `main`. Aucun travail n'est en cours ni
abandonné.
- 8 branches correspondent aux PR #1 à #8, déjà fusionnées. Tu peux les
  supprimer sur GitHub (onglet *Branches*).
- `claude/busy-feynman-qf2x4o` n'a pas de PR. C'est la branche de la session
  de ce récapitulatif. Elle est identique à `main` et accueillera ce
  récapitulatif.

**Vérifications faites** :
- Aucune question ni entrée d'`ETAT_DU_PROJET.md` perdue lors des fusions.
- Ajouté ce matin dans `QUESTIONS.md` : **2 questions** que des rapports
  demandaient d'y inscrire, mais qui n'y étaient pas. La première vient de
  l'audit d'architecture (M1, lien `importation` → `persistence.json`). La
  seconde vient de la PR #4 (GIRPE ou SPIDD).

## 3. À décider par toi

Réponds dans `QUESTIONS.md` (ligne « Réponse de Quentin »). Tu peux aussi
dire dans une session : « applique mes réponses ».

### Bloquant pour la vente

1. **Données de démo et supervision** (sécurité S-01, S-02). Chaque poste
   reçoit le compte « ultra admin », ton compte réel et l'email de Serge
   BOULIER. Choix : A) démo à la demande, identités fictives partout,
   supervision retirée du bureau ; B) démo à la demande, mais supervision
   gardée avec un mot de passe choisi à l'installation.
   **Recommandation : A.** Si le mot de passe de démo sert aussi pour un vrai
   compte (Gmail…), change-le : il est écrit dans le code et dans
   `docs/passation/`.
2. **Preuve de licence** (S-05) : qui peut lier une licence à un compte ?
   A) code envoyé à l'email connu de la FFTT ; B) validation par le club ;
   C) B tout de suite, puis A quand l'API FFTT sera ouverte.
   **Recommandation : C**, l'accès à l'API n'étant pas encore obtenu.
3. **Envoi d'emails** (S-03, S-16) : quel service envoie les codes de
   vérification ? Un compte SMTP dédié (Gmail, OVH…) ou un service spécialisé
   (Brevo, Mailjet…) ? Les identifiants resteront hors du dépôt.

### Important (règles sportives, jour J)

4. **Repêchés non pointés au lancement** : l'écran bloque le tirage tant
   qu'ils ne sont ni confirmés ni retirés (choix provisoire).
   *Recommandation : garder.*
5. **Graine du tirage des poules** (IX.109 ne parle ni de graine ni de tirage
   public) : 6 chiffres proposés, modifiables, conservés avec le tableau.
   *Recommandation : garder.*
6. **Graine du tableau final** : elle est perdue à la fermeture du logiciel.
   A) la sauvegarder, comme celle des poules ; B) l'afficher seulement.
   *Recommandation : A.*
7. **Manches d'un forfait ou d'un abandon** pour le départage (I.303 c) :
   aucune manche comptée (provisoire), score fictif 3-0, ou manches vraiment
   jouées ? *À demander à un juge-arbitre.*
8. **3e place sans petite finale** : 3es ex aequo (provisoire), départage, ou
   petite finale en option ? *Recommandation : ex aequo par défaut, et petite
   finale en option du tableau.*
9. **Place des exempts (byes)** : deux exempts peuvent aujourd'hui se faire
   face. *Recommandation : donner les byes aux têtes de série, au plus un par
   paire.* À confirmer avec un juge-arbitre.
10. **Inscription sur un tableau en pointage ou lancé** : encore possible
    aujourd'hui. *Recommandation : refuser en ligne dès l'ouverture du
    pointage. Seul le juge-arbitre peut alors inscrire, sur place.*
11. **Corriger un résultat quand la suite est jouée** : c'est refusé
    (provisoire). *Recommandation : garder.*

### Peut attendre

12. **GIRPE ou SPIDD** (nouvelle) : la recherche montre que les tournois
    individuels remontent par SPIDD, pas GIRPE. *Recommandation : corriger la
    documentation maintenant.*
13. **Lien `importation` → `persistence.json`** (nouvelle) : le documenter tel
    quel, ou extraire un petit package JSON commun ? *Recommandation : le
    documenter.*
14. **PDFBox dans le cœur** : le tolérer (situation actuelle) ou déplacer le
    rendu PDF hors du cœur.
15. **Veille des dépendances** : Dependabot sur GitHub (rien à installer) ou
    un plugin Maven. *Recommandation : Dependabot.*

**Hors code, pour toi** : relire la section 5 de `docs/RECHERCHE_FFTT.md`,
puis envoyer le courriel à la FFTT. Il est prêt à copier. C'est lui qui
débloquera la vérification des licences et des points.

## 4. À tester à l'écran

Rien n'a pu être affiché en session cloud. Sur ton PC : `git pull`, puis
`mvn clean javafx:run`. Compte : ton compte habituel (administrateur de
Brunoy). Les démos sont recréées au premier démarrage (version v13).
Regarde chaque écran en **thème clair puis sombre**.

**Page Jour J**, par deux chemins possibles :
Gestion des tournois → carte « Tournoi en cours » → « Suivre le tournoi » ;
ou Publiés → « Compléter » sur « Tournoi de démonstration du jour J » →
carte « Jour J » → « Ouvrir le jour J ».

| Écran | Chemin depuis la page Jour J | À regarder |
|---|---|---|
| Page Jour J | — | pastilles d'étape (contraste en sombre), tuiles de compteurs, action proposée par tableau |
| Ouvrir le pointage | tableau A → « Ouvrir le pointage » | encart de confirmation |
| Pointage | tableau B → « Pointer », puis « Clôturer le pointage… » | colonnes Présent / Payé alignées, recherche sans accents, repêchés « à pointer » (filet ambre), confirmation ou retrait des repêchés |
| Tirage | tableau D → « Lancer le tirage » → « Lancer le tirage au sort… » | graine en grand, modifiable ; composition des poules |
| Poules | tableau C → « Poules » → une poule → « Saisir » ou « Corriger » | largeur des champs de manche, « 11-9 » et « 9 / -9 » acceptés, forfait ou abandon, classement mis à jour, qualifiés en évidence, défilement conservé après chaque action |

**Phase finale** : Gestion des tournois → bouton « Phase finale
(provisoire) » sur un tournoi → un tableau.
- « Essai (non enregistré) » : les poules sont jouées au hasard en mémoire.
  Construis le tableau, saisis les matchs jusqu'au podium.
- Regarde les exempts, le panneau de saisie, la correction d'un score et le
  podium (« 3e ex aequo »).

Note ce qui te gêne : on corrigera par lot.

## 5. Risques et dettes

- **Sécurité** : les lots 1 à 5 de `RAPPORT_SECURITE.md` ne sont pas
  corrigés. Le lot 1 bloque la vente.
- **`switch` sur énumération** : il en reste 6, que `CLAUDE.md` interdit.
  Vérifié après compilation ce matin : 4 classes annexes en dépendent encore
  (`ConfigurationTableau$1`, `Ordonnanceur$1`, `JsonWriter$1`,
  `TournoiMapper$1`). C'est le risque connu de casse du build chez toi. Les
  lots de la nuit n'en ont pas ajouté.
- **Heure lue sans le port `Horloge`** (`GestionCartonsMatch`,
  `ImportJoueurs`, `JoueurDepuisLicence`) : des tests pourraient dépendre de
  la date du jour.
- **Provisoires** : le bouton « Phase finale (provisoire) »,
  `AccesPhaseFinaleProvisoire` et `TableauEssaiPhaseFinale`. Aucun `TODO`
  oublié dans le code.
- **Limites du jour J** (PR #6) : pas de désistement après le lancement
  (forfait match par match). L'égalité qui persiste (I.303 e) n'est pas
  signalée. Chaque case cochée réécrit tout le fichier, ce qui est à
  surveiller sur un gros tournoi.
- **Documentation périmée** :
  - `README.md` annonce 412 tests, `CONFORMITE_FFTT.md` 276,
    `ETAT_DU_PROJET.md` 412 et 289 selon les sections. La section 0 dit
    « 524, à confirmer » : **c'est confirmé, 524**.
  - La section 4 d'`ETAT_DU_PROJET.md` cite GIRPE au lieu de SPIDD.
  - Les arborescences de `README.md` et de la section 5 d'`ETAT_DU_PROJET.md`
    ne citent ni `persistence`, ni `importation`, ni `ui`.
- **Recommandations sans tâche lancée** : la session 5 du plan de nuit
  (optimisation) n'a pas été lancée. Les tâches 2 à 6 de
  `docs/TACHES_AUTONOMES.md` non plus (tests du cœur, conformité I.301 à
  I.305, et les trois tâches J-1).
- **Conflits probables** entre les prochaines tâches :
  - les tâches 1 et 4 ci-dessous touchent toutes deux aux démos et à
    `ContexteApplication` ;
  - les tâches 1, 3 et 4 touchent `ETAT_DU_PROJET.md`. La règle « garder les
    deux versions » s'applique.

## 6. Prochaines tâches proposées

Chaque tâche se lance dans une nouvelle session (« Créer », dépôt
`Gestion_Tournoi_FFTT`), en collant le bloc tel quel. Les tâches **1, 2, 3, 5
et 6 peuvent tourner en même temps** : elles touchent des fichiers
différents (seul `ETAT_DU_PROJET.md` est partagé, et il se fusionne sans
perte). La tâche **4 attend tes réponses aux questions 1 à 3** et doit passer
après la 1.

### Tâche 1 — Relier la phase finale à la page Jour J (priorité 1)

```
Utilise l'agent `developpeur` avec les skills `charte-graphique` et
`architecture-pingmanager`.

Objectif : la phase finale d'un tableau s'ouvre depuis la page Jour J, et le
point d'entrée provisoire disparaît.

À faire :
1. Dans la page Jour J (`ui/ecran/EcranJourJ.java`, logique d'affichage dans
   `PresentationJourJ.java`), quand toutes les poules d'un tableau sont
   terminées, proposer « Construire la phase finale », puis ouvrir
   `VuePhaseFinale.creer(contexte, tournoi, tableau, fichier)`. Un tableau dont
   la phase finale est construite affiche l'étape « Phase finale » et un bouton
   pour y retourner.
2. Retirer le bouton « Phase finale (provisoire) » de
   `ui/ecran/PageGestionTournoi.java`, et supprimer `AccesPhaseFinaleProvisoire`
   et `TableauEssaiPhaseFinale` (avec leurs tests) s'ils ne servent plus.
3. Dans la démo du jour J (package `ui.infra`, PAS `TournoiDemoBrunoy`), ajouter
   un tableau dont toutes les poules sont jouées, prêt pour la phase finale.
   Incrémenter `ContexteApplication.VERSION_DEMO` (v13 -> v14).

Règles FFTT à vérifier dans `docs/reglements/` (Grep sur le numéro) : I.304 et
I.305 (placement des qualifiés), IX.109 (tirage au sort). Ne pas modifier le
placement ni le format JSON. Les questions ouvertes de `QUESTIONS.md` (3e
place, graine du tableau final, exempts) restent en l'état : ne pas les
trancher.

Contraintes : tout en français ; pas de `switch` sur une énumération ; logique
d'affichage testée sans JavaFX (tests JUnit 5 dans le même package) ; aucune
nouvelle dépendance.

Critères de fin : `mvn -B test` à 0 échec, en donnant le nombre total de tests ;
`ETAT_DU_PROJET.md` section 0 mise à jour (sans effacer les entrées des autres
sessions).
Branche `jour-j/phase-finale-reliee`. Mets-la à jour avec `main` avant d'ouvrir
une Pull Request en français. La PR indique ce qui a été fait, les tests
ajoutés, le nombre total de tests, et comment tester chez Quentin (`git pull`,
`mvn clean javafx:run`, compte administrateur de Brunoy, « Tournoi de
démonstration du jour J », thème clair et sombre). Préciser que l'écran n'a pas
pu être vérifié en session cloud. Ne fusionne pas.
```

### Tâche 2 — Supprimer les `switch` sur énumération (priorité 2, parallèle)

```
Utilise l'agent `developpeur` avec la skill `architecture-pingmanager`.

Objectif : plus aucun `switch` sur une énumération dans `src/main/java`, sans
changer le comportement (règle de `CLAUDE.md`, constat H1 de
`RAPPORT_ARCHITECTURE.md`).

Fichiers concernés : `model/ConfigurationTableau.java` (switch sur
`ModeLimiteClassement`), `model/RegleTableauxParJour.java` (`ExceptionFeminine`),
`ordonnancement/Ordonnanceur.java` (deux switch sur `PhaseMatch`),
`persistence/json/JsonWriter.java` (`Json.Type`), `persistence/TournoiMapper.java`
(`ExceptionFeminine`). Remplacer chacun par une chaîne de `if (x == Enum.A)` ou
un `switch (x.name())`. Les `switch` sur `String`, `char` ou `int` sont
autorisés : ne pas y toucher.

Vérification : après `mvn -B test`, la commande
`for f in $(find target/classes -name '*$1.class'); do javap -p "$f" | grep -q SwitchMap && echo "$f"; done`
ne doit plus rien afficher. Ajouter un garde-fou : un script dans `outils-dev/`
(documenté dans `outils-dev/LISEZMOI.md`), ou un test JUnit qui échoue si une
classe `$1` contient un champ `$SwitchMap$`.

Aucune règle FFTT en jeu. Ne pas modifier le format JSON, garder
`RoundTripTournoiTest` vert. Ne supprimer ni n'affaiblir aucun test.

Critères de fin : `mvn -B test` à 0 échec, avec le nombre total de tests.
Branche `correctif/switch-enumerations`. Mets-la à jour avec `main` avant
d'ouvrir une Pull Request en français (fichiers modifiés, résultat de la
vérification `javap`, nombre de tests, rien à vérifier à l'écran). Ne fusionne
pas.
```

### Tâche 3 — Remettre la documentation à jour (priorité 3, parallèle)

```
Utilise l'agent `developpeur`. Tâche de documentation seulement : ne modifie
aucun fichier de `src/`.

Objectif : une documentation exacte au 5 novembre 2026.
1. `ETAT_DU_PROJET.md` :
   - section 4 : les tournois individuels se remontent avec SPIDD (logiciel
     du juge-arbitre), pas GIRPE (rencontres par équipes), d'après
     `docs/RECHERCHE_FFTT.md` ;
   - section 5 : arborescence complète des packages (model, model.format,
     core, core.reglement, ordonnancement, importation, persistence(.json),
     ui(.ecran, .infra)) ;
   - sections 7, 9 et 10 et pied de page : nombres de tests périmés ;
   - section 0 : remplacer « total à confirmer » par le nombre mesuré.
2. `README.md` : nombre de tests, arborescence et « prochain bloc ».
3. `CONFORMITE_FFTT.md` : nombre de tests (ligne « 276 tests »).
4. `CLAUDE.md` : ne le modifie pas. Si `QUESTIONS.md` contient une réponse de
   Quentin sur le lien `importation` -> `persistence.json`, signale-le
   simplement dans la PR.
Mesure le nombre de tests avec `mvn -B test` (ne recopie pas un nombre
existant). Ne supprime aucune entrée écrite par une autre session.

Critères de fin : `mvn -B test` à 0 échec (pour la mesure). `git diff
origin/main` ne doit montrer que de la documentation.
Branche `doc/mise-a-jour`. Mets-la à jour avec `main` avant d'ouvrir une Pull
Request en français. Ne fusionne pas.
```

### Tâche 4 — Sécurité, lot 1 (priorité 4, après tes réponses aux questions 1 à 3 et après la tâche 1)

```
Utilise l'agent `developpeur` avec la skill `architecture-pingmanager`.

Objectif : appliquer le lot 1 de `RAPPORT_SECURITE.md` (S-01, S-02, S-03, S-12,
S-16), selon les réponses de Quentin dans `QUESTIONS.md` (« Données de démo et
supervision », « Service d'envoi d'emails »). Si une réponse manque, prends
l'option la plus prudente et réversible, et note-le dans la PR.
1. Mode démo explicite (propriété `-Dpingmanager.demo=true` ou choix au premier
   lancement). Hors démo, aucun compte n'est créé.
2. Plus aucune identité réelle (email, licence, date de naissance de Quentin,
   email de Serge BOULIER) dans le code, les tests, la documentation et
   `docs/passation/` : la remplacer par des identités fictives `@exemple.fr`.
   Plus de mot de passe écrit dans le code.
3. Supervision « ultra admin » : appliquer la réponse de Quentin (retrait du
   produit de bureau, ou rôle stocké).
4. Vérification de l'email à la création du compte (`GenerateurCode` +
   port `EnvoiEmail`). Le compte reste inactif tant qu'il n'est pas confirmé.
5. Politique de mot de passe (au moins 10 caractères, petite liste noire) et
   format d'email, avec un résultat `ResultatCreationCompte` à motif précis.
6. Plus aucun code imprimé sur la sortie standard. `EmailConsole` est réservée
   au mode démo.
Inverser les tests `faille_S01` à `faille_S03`. Ajouter :
`aucun_compte_cree_hors_mode_demo`,
`creation_de_compte_refusee_si_mot_de_passe_trop_court`,
`acces_pre_enregistre_inactif_tant_que_l_email_n_est_pas_verifie`,
`code_de_verification_absent_de_la_sortie_standard`.

Contraintes : le cœur ne connaît ni l'interface ni la persistance (ports) ;
aucune nouvelle dépendance Maven sans accord de Quentin ; JSON rétrocompatible
(`RoundTripTournoiTest`). Incrémenter `ContexteApplication.VERSION_DEMO` si les
données de démo changent.

Critères de fin : `mvn -B test` à 0 échec, avec le nombre total de tests.
`ETAT_DU_PROJET.md` section 0 et `RAPPORT_SECURITE.md` mis à jour (constats
corrigés).
Branche `securite/lot-1`. Mets-la à jour avec `main` avant d'ouvrir une Pull
Request en français. Elle indique comment lancer la démo, quels écrans vérifier
(connexion, création de compte, liaison club) et que l'écran n'a pas été vu en
session cloud. Ne fusionne pas.
```

### Tâche 5 — Faire passer l'heure par le port `Horloge` (priorité 5, parallèle)

```
Utilise l'agent `developpeur` avec la skill `architecture-pingmanager`.

Objectif : le cœur ne lit plus jamais la date ou l'heure du système
directement (constat M2 de `RAPPORT_ARCHITECTURE.md`). Il passe par le port
`Horloge`.
Fichiers concernés : `core/GestionCartonsMatch.java` (injecter `Horloge`),
`core/ImportJoueurs.java` et `JoueurDepuisLicence.java` (supprimer les replis
`LocalDate.now()` : date de référence obligatoire, ou issue de l'horloge). Les
appelants de `ui` fournissent l'horloge système. Chercher toute autre lecture
(`LocalDate.now`, `LocalDateTime.now`, `Instant.now`, `System.currentTimeMillis`)
dans `model`, `core` et `ordonnancement`.

Règles FFTT à vérifier dans `docs/reglements/` : II.302 et II.303 (catégorie
d'âge calculée selon la saison). Ajouter des tests à horloge fixe, dont
`categorie_d_age_calculee_selon_la_saison_du_tournoi_et_non_la_date_du_jour`.

Contraintes : ne pas toucher aux fichiers de la tâche « switch sur
énumération » (`ConfigurationTableau`, `RegleTableauxParJour`, `Ordonnanceur`,
`JsonWriter`, `TournoiMapper`) ; pas de `switch` sur une énumération ; aucune
nouvelle dépendance.

Critères de fin : `mvn -B test` à 0 échec, avec le nombre total de tests.
Branche `correctif/port-horloge`. Mets-la à jour avec `main` avant d'ouvrir
une Pull Request en français. Ne fusionne pas.
```

### Tâche 6 — J-1 : tableau de bord de la veille, côté cœur (priorité 6, parallèle)

```
Utilise l'agent `developpeur` avec la skill `architecture-pingmanager`. Lis
d'abord `docs/passation/PASSATION_IMPORT_ET_J-1.md`.

Objectif : créer dans `core` un service `TableauDeBordJ1` qui donne, pour un
tournoi et pour chaque tableau : inscrits, liste d'attente, payés, horaires et
état du pointage. Il donne aussi une liste d'alertes à motif précis :
tableau sans horaire, tableau sous le seuil de maintien
(`ConditionsMaintien`), juge-arbitre principal non désigné, joueur sans club
ou sans points. Uniquement le cœur et ses tests JUnit 5 (même package) : pas
d'écran.

Règles FFTT à vérifier dans `docs/reglements/` (Grep) : chapitre IX (IX.101 à
IX.111, notamment le juge-arbitre et le maintien des tableaux). Citer
l'article dans le code et les tests. Si un texte est muet, noter la question
dans `QUESTIONS.md` sans deviner.

Contraintes : nouveaux fichiers seulement, si possible. Aucun import JavaFX,
`ui`, `persistence` ou `importation` dans `core`. Objets immuables avec
builder, et résultats détaillés plutôt que booléens. Pas de `switch` sur une
énumération.

Critères de fin : `mvn -B test` à 0 échec, avec le nombre total de tests ;
`ETAT_DU_PROJET.md` section 0 mise à jour.
Branche `j-1/tableau-de-bord-coeur`. Mets-la à jour avec `main` avant d'ouvrir
une Pull Request en français. Ne fusionne pas.
```

## 7. Chiffres

- **Tests mesurés** (`mvn -B test` sur `main`, 5 novembre, 9 h 30) :
  **524 tests, 0 échec, 0 erreur, 0 ignoré**. Ce total comprend les 14 tests
  qui démontrent les failles de sécurité.
- **Pull Requests** : 8 fusionnées, aucune ouverte.
- **Branches non fusionnées** : aucune. 8 branches de PR fusionnées sont à
  supprimer, plus la branche de session `claude/busy-feynman-qf2x4o`, sans PR.
- **Questions en attente** : 15, dont 3 bloquantes pour la vente et
  2 ajoutées ce matin.
- **Constats de sécurité ouverts** : 23 (1 critique, 3 élevés, 11 moyens).
- **`switch` sur énumération restants** : 6, dans 5 fichiers.
