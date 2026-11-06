# Récapitulatif PingManager — 6 novembre 2026 (nuit du 5 au 6)

> Tenu à jour par l'agent `recapitulateur`. Pour le régénérer, nouvelle
> session Claude Code : « Utilise l'agent `recapitulateur` et fais le point. »
> Point établi sur `origin/main` au commit `c2bc83a` (PR 46 fusionnée à 10 h 23).

## 1. En bref

- 11 tâches du carnet livrées pendant la nuit (PR 28 à 46), plus 4 le 5 novembre dans la journée. Aucune PR n'est restée ouverte.
- Couloir B (côté joueur) et C (gestion du club) : terminés. Couloir D (accueil, profil, formulaires) : terminé.
- Couloir A (jour J) : B037 est fait ; **B038 (saisie du score en grille) n'a produit aucun code** (seule la PR « prise en charge » est fusionnée) et **B039 n'est pas commencée**.
- Le CI GitHub est vert sur `main` ; le nombre exact de tests n'a pas pu être mesuré ici (voir section 7).
- 50 questions attendent ta réponse dans `QUESTIONS.md` (33 viennent des journaux de la nuit). Rien n'a été vu à l'écran par un humain.

## 2. Livré depuis le dernier point

Couloir A, jour J
- **PR 33 — B037 Jour J v2** : matchs proposés en rouge/orange/vert, joueurs en attente avec chrono et explication, « Salle », bouton « Lancer un match », « Terminer la rencontre/poule », choix du mode d'annonce (SMS, vocal, rien).
- **B038** (saisie en grille, cartons) : branche `tache/B038-saisie-score-grille` = prise en charge seulement (PR 38). Rien de livré.
- **B039** (onglets du jour J, pointage sans croix « payé ») : non commencée.

Couloir B, côté joueur
- **PR 35 — B032 Agenda** : l'agenda du compte existe (ajout automatique à l'inscription, retrait à la désinscription, dates perso, export .ics). Pas d'écran propre.
- **PR 42 — B030 Carte des tournois** : onglets « Carte » et « Agenda par jour », bouton « Voir plus » sur chaque tournoi, quatre statuts (En cours, À venir, Non accessible, Complet).
- **PR 44 — B031 Mes tournois** : trois parties (en cours, inscriptions + agenda, passés avec mes victoires/défaites), désinscription, export .ics.

Couloir C, gestion du club
- **PR 28 — B034 Nouveau tournoi** : texte explicatif, deux cartes, calendrier cliquable, dates impossibles refusées.
- **PR 32 — B033 Gestion des tournois** : carte « Tournoi actuel », frise verticale en 4 étapes, gros bouton « Créer un tournoi », historique.
- **PR 45 (et 39) — B035 Homologation** : dossier verrouillé dès la demande, refus avec motif, nouvelle vue du tournoi homologué ; l'import ne crée plus de tableau sur un tournoi verrouillé.
- **PR 46 — B036 Remplissage et finances** : carte avec taux de remplissage, recettes attendues, encaissé, dotation promise et solde.

Couloir D, accueil et profil
- **PR 34 — B028 Accueil** : cartes expliquées (Profil, Tournois, Agenda, Statistiques) et bandeau « Gestion de votre club » réservé aux comptes club.
- **PR 40 — B029 Mon profil** : bloc personnel + licence, bloc Club avec avertissement, case à cocher et tableau des droits. Répare aussi une erreur de `theme.css`.
- **PR 41 — B012 Préparation du tournoi v2** : tuiles, cartes, grille des tableaux lisible.

Hier (5 novembre, avant la nuit) : PR 18 B001 (plus de `switch` sur énumération), 19 B010 (règlement figé après publication), 23 B011 (jour J simplifié), 24 B027 (suivre tous les tableaux), 25 (cahier des charges v0 et consignes de nuit). PR de « prise en charge » (sans code) : 26, 27, 29, 30, 31, 36, 37, 38, 43.

## 3. À décider par Quentin

Aucune question n'a de réponse. Dans tous les cas l'agent a gardé l'option la plus prudente. Détail complet dans `QUESTIONS.md`.

**Bloquant pour la vente** : aucune nouvelle cette nuit. Restent bloquantes les anciennes sur le tirage (IX.109, graine non enregistrée) et la sécurité (S-01 à S-05) listées en tête de `QUESTIONS.md`.

**Important**
- Délai de désinscription en ligne (B032) : jusqu'à la fin des inscriptions (choix agent) ou date limite fixée par l'organisateur. Recommandation : garder le choix agent, puis ajouter la date limite plus tard.
- Juge-arbitre après la demande d'homologation (B035) : tout verrouillé sauf licence et contact (choix agent), ou remplacement possible si le grade reste conforme. Recommandation : autoriser le remplacement (un JA malade bloquerait le tournoi), à vérifier contre IX.104.
- Tableaux après la demande d'homologation (B035) : verrouillage complet (choix agent) ou décalage d'horaire le même jour permis. Recommandation : garder le verrouillage.
- Tournoi annulé (B010 et B035) : règlement et verrous levés après annulation (choix agent) ou conservés. Recommandation : garder le choix agent.
- Couleurs du jour J (B037) : seuils fixes 10 et 20 minutes (choix agent), réglables, ou calculés. Recommandation : garder fixe jusqu'à B025.
- « Orange = prochain à envoyer » (B037) : presque tout est orange au début du tournoi. Recommandation : ne garder que le premier proposé, ou attendre B025.
- Victoires/défaites par abandon ou forfait (B031) : comptées et signalées à part (choix agent) ou exclues. Aucun article pour un tournoi individuel. Recommandation : garder le choix agent.
- Téléphone absent du compte (B029) : « Non renseigné » (choix agent) ou ajout du champ (changement du format JSON). Recommandation : l'ajouter à la prochaine évolution du compte, avec test de relecture.
- Liste des droits du tableau Club (B029) : à relire ligne par ligne.
- Tarif et dotation dans « Remplissage et finances » (B036) : tarif en ligne, dotation sur les recettes attendues, dotation pleine sans réduction. Recommandation : garder les trois choix.
- Enchaîner un joueur qui vient de finir (B011) et table choisie par « Table N » : plus petite table libre (choix agent). À trancher avec B025.

**Peut attendre**
- Bascule en mode suivi avant le jour J (B027) et libellé « Inscriptions ouvertes » pendant le tournoi.
- Brouillon dont la date est passée dans la frise ; « Tournoi actuel » quand rien n'est en cours (B033).
- Liste « Mes prochains tournois » retirée de l'accueil ; bandeau bleu du club ; page Agenda provisoire (B028).
- Liste d'attente dans l'agenda ; retrait manuel d'une date automatique (B032).
- « Terminer la poule » ; joueurs qualifiés en attente du tableau final ; ordre des parties de poule I.301 (B037).
- Tournoi « En cours » mais non accessible ; pastille « Complet » avec liste d'attente (B030).
- Adoucir partout l'ambre des rappels (B012) ; « Bientôt » sur l'appel micro (B029).
- Dates de paiement pour une courbe d'encaissements ; arrondi au centime (B036).
- Anomalie : la question « 3e place sans petite finale » porte déjà une réponse de Quentin (option C) mais reste dans « En attente » ; à déplacer dans « Tranché ».

## 4. À tester à l'écran

Aucun de ces écrans n'a été vu par un humain (sessions cloud). Chaque point : regarder en thème clair et sombre.

| Écran | Compte | Chemin | À regarder |
|---|---|---|---|
| Jour J v2 (B037) | Quentin, « Tournoi de démonstration du jour J » | Gestion des tournois, Suivre le tournoi | couleurs des matchs, joueurs en attente et clic « pourquoi », question d'annonce, Terminer/Annuler, Alt+L |
| Carte des tournois (B030) | Brunoy, licencié 9141442 | Tournois, Carte des tournois | Carte et Agenda par jour, « Voir plus », 4 statuts (« En cours » absent de la démo), Lyon sur deux jours |
| Mes tournois (B031) | idem | Tournois, Mes tournois | 3 parties, désinscription (s'inscrire d'abord à Créteil), agenda, export .ics dans Google/Outlook, tournoi passé |
| Gestion des tournois (B033) | Brunoy | Gestion des tournois | Tournoi actuel, frise, gros bouton, Historique |
| Nouveau tournoi (B034) | Brunoy | Gestion, Nouveau tournoi | calendrier, 31/02/2027 refusé, dates de fin grisées |
| Homologation (B035) | Brunoy 2027 | Gestion, tournoi, préparation | cadenas, bandeau motif, refus avec motif, vue homologuée |
| Remplissage et finances (B036) | tournoi homologué | écran du tournoi | 4 chiffres, jauge, solde rouge s'il est négatif ; non vu avec des inscrits |
| Accueil (B028) | club, joueur licencié, sans licence | premier écran | bandeau club absent pour le joueur, chaque bouton |
| Mon profil (B029) | compte relié à un club et joueur non relié | menu Mon profil | encart d'alerte, case à cocher, tableau des droits |
| Préparation v2 (B012) | Brunoy | tournoi, préparation | tuiles, cartes, grille des tableaux, nouveau tableau |
| Suivi (B027) et règlement figé (B010), de la veille | Brunoy / Quentin | fiche du tournoi publié | lecture seule, lien « Règlement » |

Non vu par les agents : rendu Windows, mise à l'échelle, popup du calendrier, tournoi passé joué, tournoi du jour.

## 5. Risques et dettes

- **Harnais de captures cassé** (étapes « Créer un tournoi » et « Suivre le tournoi » introuvables, compilation exigeant `LANG=C.UTF-8`) : aucune tâche prévue au carnet.
- **`theme.css`** : une accolade et un commentaire perdus lors d'une fusion (B028) ont cassé le fichier ; réparé par B029/B030, de même qu'une accolade dans `RoundTripTournoiTest` (B031). Les fusions parallèles de fichiers communs restent le point fragile.
- **Chiffres de tests incohérents** dans les journaux : 879 (B035), 798 (B012), 830 (B031), 812 (B030), 750 (B029). Le comptage des `@Test` donne 940 sur `main`. Une valeur mesurée par `mvn -B test` est à relever.
- **Documentation périmée** : `ETAT_DU_PROJET.md` section 7 annonce encore 412 tests ; la section 0 s'allonge (notes des journaux reportées en bloc) et mérite un nettoyage.
- **Provisoires** : bouton « Phase finale (provisoire) » (`AccesPhaseFinaleProvisoire`) ; page Agenda « à venir » (B028) à vérifier maintenant que B032/B031 existent ; tâches du carnet encore marquées `a-faire` pour la sécurité (B004 bloquée) et le port Horloge (B002).
- **Sécurité** : l'audit du 5 novembre (`origin/audit/securite`) n'a pas été revu cette nuit ; B004 reste bloquée.
- **Règles FFTT non tranchées** : ordre des rencontres I.301, remplacement de JA (IX.104), abandon/forfait en individuel (III.109 vise le Critérium).
- **Nettoyage** : la branche `claude/busy-feynman-qf2x4o` (ancien récapitulatif) n'est pas fusionnée ; beaucoup de branches fusionnées traînent encore sur le dépôt distant.

## 6. Prochaines tâches proposées

Parallélisme : 1 et 2 touchent le jour J (même écran), à lancer l'une après l'autre. 3, 4 et 5 sont indépendantes des deux premières et entre elles.

1. **B038 — saisie du score en grille et cartons** (agent `developpeur-jour-j`, branche `tache/B038-saisie-score-grille`, fichiers `EditeurScorePhaseFinale`, saisie des poules, `GestionCartonsMatch`).
   Consigne : « Avance le projet en suivant la skill piloter-projet, uniquement sur la tâche B038 (reprise : la branche existe, seule la prise en charge est faite). Référence : docs/CAHIER_DES_CHARGES_V0.md sections 7 et 8. »
2. **B039 — jour J en onglets, pointage sans croix « payé »** (même agent, branche `tache/B039-jour-j-onglets-pointage`), après B038.
   Consigne : « … uniquement sur la tâche B039, après B038 fusionnée. »
3. **Réparer le harnais de captures et vérifier `theme.css`** (agent `correcteur`, branche `tache/harnais-captures`) : étapes cassées, `LANG`, relecture de `theme.css`. Consigne : « Répare `outils-dev/run.sh` et le harnais de captures (étapes ‹ Créer un tournoi › et ‹ Suivre le tournoi ›, compilation sans LANG), puis relis `theme.css` à la recherche d'accolades ou commentaires perdus. Ne touche à rien d'autre. »
4. **Mise à niveau de la documentation (B003)** (agent `documentaliste`, branche `tache/B003-documentation`) : `ETAT_DU_PROJET.md` (section 7, nettoyage de la section 0), nombre de tests réel. Consigne : « Avance sur B003 avec la skill piloter-projet. »
5. **B004 sécurité lot 1 après tes réponses** (agent `developpeur`, branche `tache/B004-securite-lot-1`) : à débloquer d'abord en répondant aux questions S-01, S-02, S-03, S-05. Ou, plus léger, **B002 port Horloge** (branche `tache/B002-port-horloge`).
6. **Après la v0** : B040 (conformité du tirage), B009 (tableau final), B025 (stratégies, qui règle plusieurs questions de la section 3).

## 7. Chiffres

- Tests : **940 annotations `@Test`** dans `src/test` sur `main` (comptage). Exécution locale impossible : `mvn` hors ligne (Maven Central inaccessible) et `outils-dev/tests.sh` échoue faute de JavaFX. **CI GitHub « Tests » : succès sur `c2bc83a`, `7e8f231` et `c2d5e4d`** (0 échec) ; le nombre exécuté n'est pas affiché par l'API.
- PR fusionnées depuis le 5 novembre 20 h : 21 (PR 26 à 46), dont 11 livraisons de tâches et 10 prises en charge ; 5 PR le 5 novembre dans la journée (18, 19, 23, 24, 25).
- PR ouvertes : 0. Branche non fusionnée : `claude/busy-feynman-qf2x4o` (récapitulatif du 5 novembre, 2026-11-05) et la branche courante `claude/peaceful-thompson-ikcmrb`.
- Questions ouvertes : 50 dans `QUESTIONS.md` (dont 33 ajoutées ce matin depuis les journaux, 1 réponse déjà donnée à classer).
- Journaux reportés puis archivés : 15 (`docs/journal/archives/`).
