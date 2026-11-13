# PingManager

Logiciel de bureau (Java 21, JavaFX) pour **gérer les tournois individuels
officiels de tennis de table FFTT**, de la création du tournoi à la finale :
homologation, inscriptions en ligne, pointage, poules, tableau final,
ordonnancement des tables, annonces en salle.
Hors périmètre : le Critérium fédéral et les épreuves par équipes.

> Ce README est **le seul document du projet**. Il décrit l'état réel du code au
> 8 novembre 2026 et tout ce qui reste à faire. Le mettre à jour à chaque lot
> de travail.

---

## 1. Lancer le programme

### Prérequis
- **Java 21** (JDK, par exemple Temurin 21)
- **Maven 3.9+**
- Accès à Internet au premier lancement (Maven télécharge JavaFX, PDFBox et JUnit)

### Commandes (Windows, macOS, Linux)

```
git pull                 # récupérer la dernière version
mvn clean javafx:run     # compiler et lancer l'application
mvn -B test              # lancer les 1 535 tests (doit finir à 0 échec)
```

Point d'entrée : `com.pingmanager.ui.Lanceur`.

### Au premier lancement
- Les **données de démonstration** sont créées : clubs, licenciés fictifs et
  deux tournois du club de Brunoy :
  - le **National B de Brunoy** (10-11 avril 2027, 13 tableaux), le vrai
    tournoi du club, à son état réel : **brouillon**, sans numéro
    d'homologation ni inscrit, avec les vraies personnes (juges-arbitres
    Samuel LOOBUYCK et William PRIOU, contact Serge BOULIER). Son règlement
    généré reprend les 21 articles du règlement du club (version du
    10 novembre 2026) ;
  - sa **copie au jour J** (« … – jour J (copie de démo) »), publiée avec un
    **faux numéro d'homologation et des personnes fictives** (juges-arbitres,
    correspondant, lien de paiement) : mêmes tableaux,
    756 inscrits sur 768 places, samedi en cours. Dans l'ordre des horaires du
    samedi : tableau final entamé (un match en cours, table 1), poules
    terminées (tableau final à tirer), poules en cours (trois parties aux
    tables 4, 7 et 9), pointage clôturé (tirage à lancer), pointage ouvert ;
    les autres tableaux et tout le dimanche sont encore en inscription. Le
    compte de démo y est inscrit aux 2e et 4e tableaux du samedi.
- Quand la version de la démo change, les autres fichiers de tournoi du dossier
  sont **archivés** dans `tournois/archives` (jamais supprimés), et les deux
  tournois de démo sont recréés.
- Les **comptes de démo** (club de Brunoy, joueur licencié, supervision) sont
  affichés dans la **console** au démarrage.
- Les données sont rangées dans `~/.pingmanager/pingmanager/` (ou
  `$XDG_DATA_HOME/pingmanager/`) : un fichier applicatif (comptes, clubs,
  accès) et un fichier `.ptt.json` par tournoi.
- Si les données de démo changent dans le code, la constante
  `ContexteApplication.VERSION_DEMO` est incrémentée (v19 actuellement) et la
  démo est recréée au lancement suivant.

### Suivi sur téléphone
Le jour J, l'application peut ouvrir un petit serveur web en lecture seule
(port **8080** par défaut) sur le Wi-Fi de la salle. Au premier lancement,
le pare-feu Windows demandera d'autoriser Java.

### Réglages et valeurs par défaut
L'application n'a ni argument de ligne de commande ni fichier de configuration :
tout se règle à l'écran et s'enregistre dans le fichier du tournoi.

| Réglage | Valeur par défaut | Où |
|---|---|---|
| Dossier de données | `$XDG_DATA_HOME/pingmanager`, sinon `~/.pingmanager/pingmanager` | `ServiceFichierTournoi` |
| Fichiers | `pingmanager-application.json`, un `.ptt.json` par tournoi, `.autosave.json` | `persistence` |
| Sauvegarde automatique (pas encore branchée) | toutes les 15 min | `ServiceAutosave` |
| Suivi sur téléphone | port 8080 (de 1024 à 65535) | `DiffusionSuivi` |
| Tableau | poules de 4, 2 qualifiés, 3 manches gagnantes, liste d'attente de 4, toutes séries, gratuit, sans petite finale | `ConfigurationTableau` |
| Horaires calculés | écarts de 1 h 30 puis 2 h, en alternance | `EcranTableaux`, `OrdonnancementTableaux` |
| Codes de vérification | valables 30 min | `ServiceComptesOrganisateurs`, `ServiceLiaisonClub` |
| Matchs proposés | orange après 10 min d'attente, rouge après 20 min | `ReglePrioriteSalle` |
| Prévisions | 100 tirages, 16 tables, fin visée 20 h, partie de 20 min, environ 7 min autour du jeu | `ScenarioSimulation`, `TempsAnnexesMatch` |
| Annonces vocales | 2 diffusions (3 au plus), débit de −10 à 10 | `ReglagesAnnoncesVocales` |
| Import | 20 000 lignes, 200 colonnes ; fichiers d'échange : 10 Mo | `LecteurTableur`, `CsvFederal` |
| Voix (Windows) | variables `PINGMANAGER_SSML`, `PINGMANAGER_VOIX`, `PINGMANAGER_DEBIT` passées à PowerShell | `SyntheseVocaleWindows` |

---

## 2. Organisation du code

Environ 66 600 lignes (368 classes) et 217 classes de test.

```
com.pingmanager
├── model, model.format       entités pures (joueur, tableau, tournoi, formats…)
├── core                      services métier (inscription, pointage, lancement…)
│   ├── core.feuilles         feuilles de match et de poule
│   └── core.reglement        règlement et feuille d'engagement en PDF
├── ordonnancement            tables, priorités, durées, prévision, anticipation
├── importation               lecture xlsx / ods / csv / json, sans dépendance
├── persistence(.json)        sauvegarde JSON locale, autosave, reprise
└── ui                        interface JavaFX
    ├── ui.ecran              écrans
    ├── ui.infra              démo, géocodage, SMS simulé, impression…
    └── ui.diffusion          serveur du suivi sur téléphone
```

**Règle d'or** : `model`, `core` et `ordonnancement` ne connaissent ni
l'interface, ni la persistance, ni l'importation. Les effets de bord passent
par des ports (`EnvoiEmail`, `Horloge`, `Geocodeur`, `EnvoiSms`…). Un test
(`RegleOrModeleSansDependanceTest`) le vérifie.
Exception connue : `core.reglement.RenduPdfReglement` importe PDFBox.

Dépendances : JavaFX 21.0.5 (controls, web), PDFBox 2.0.29, JUnit 5.
Aucune autre sans décision de Quentin.

Conventions : tout en français, jamais de `switch` sur une énumération,
format JSON toujours relisible pour les anciens fichiers (voir `CLAUDE.md`).

---

## 3. Ce qui fonctionne (utilisable à l'écran)

> Aucun écran livré depuis le 5 novembre n'a encore été vu par un humain :
> tout a été vérifié par les tests et par des captures automatiques.
> La première étape est donc de **tout parcourir à l'écran**.

**Comptes et club**
- Connexion, création de compte, licence liée, rattachement à un club avec
  niveaux d'accès, page « Mon profil », accueil en cartes, supervision.

**Préparation du tournoi**
- Nouveau tournoi avec calendrier, puis choix imposé : tout préparer dans
  PingManager, ou importer un classeur (tableaux et joueurs).
- Sections Général, Tableaux (horaires calculés, par défaut 1 h 30 entre deux
  tableaux et 2 h entre deux blocs), Sportif, Complémentaire. Plafond de points
  seul affiché « NC à 1499 » (non classés jusqu'au plafond).
- Cycle BROUILLON → DEMANDE D'HOMOLOGATION → HOMOLOGUÉ → PUBLIÉ (ou ANNULÉ),
  dossier verrouillé dès la demande, refus avec motif.
- Règlement généré (aperçu et PDF), figé après publication ; feuille
  d'engagement en PDF. Avec une salle d'échauffement, l'article « Matériel »
  précise que le juge-arbitre peut en utiliser les tables comme tables de
  match en cas de nécessité (3.3.1.2.2).
- Gestion des tournois du club : tournoi actuel, frise en 4 étapes, historique.
- Vue d'ensemble d'un brouillon (refonte du 11 novembre) : pleine largeur,
  « Préparation du dossier » avec légende sur une bande et quatre blocs en
  grille (4, 2 ou 1 colonne selon la fenêtre, éléments manquants en premier),
  puis « Homologation et publication » en trois grandes cartes (demande,
  homologation obtenue, publication). Plus de bloc Inscriptions avant
  l'homologation. Titres en Bricolage Grotesque (police embarquée, OFL).
- Tournoi préparé ailleurs (mode import) : trois étapes, déposer le règlement
  (PDF), importer le classeur, vérifier l'import (dates, horaire de chaque
  tableau), puis bandeau d'accès au jour J.
- Toutes les pages occupent la largeur de la fenêtre (`Ui.page`).
- Remplissage et finances : taux de remplissage, recettes attendues,
  encaissé, dotation promise, solde.

**Import**
- Joueurs et tableaux depuis Excel (.xlsx), LibreOffice (.ods), CSV ou JSON,
  avec un classeur modèle à deux feuilles (listes déroulantes).
- Import des licenciés et des clubs par fichier (format provisoire PingManager).

**Côté joueur**
- Onglet Tournois : carte (Plan IGN, repli OpenStreetMap) et agenda par jour,
  filtres, tournois non accessibles grisés avec le motif (IX.101, IX.102).
- Fiche du tournoi, assistant d'inscription avec validation en direct
  (quotas par jour, points, sexe, catégories, tableaux qui se suivent,
  liste d'attente), paiement « à régler plus tard ».
- Mes tournois (en cours, à venir, passés avec victoires et défaites),
  désinscription, agenda du compte et export .ics.

**Jour J**
- **Inscription sur place** (bouton « Inscrire sur place… ») : un joueur du
  tournoi ou un licencié, à un tableau qui n'a pas commencé et qui a une place
  libre ; pointé et payé à la table. Clause du club (Règles d'inscription) :
  un joueur éliminé peut prendre N tableaux de plus le même jour (Brunoy : 1).
- Pointage et repêchage de la liste d'attente, tirage reproductible des
  poules et du tableau final (conforme I.302 à I.305). Tableau incomplet :
  un seul tour d'exempts, le 2e tour est toujours complet, les exempts vont
  d'abord aux 1ers de poule (décision du 11 novembre 2026).
- Salle : matchs proposés en rouge, orange ou vert, joueurs en attente avec
  chronomètre et explication, « Lancer un match », plan de salle, stratégies
  d'ordonnancement. « Tableaux commencés tôt » : chaque tableau avance seul,
  tours avancés d'abord, et joue sa finale à son heure. « Petit à petit » :
  tour par tour dans chaque tableau, et toutes les finales gardées pour la fin
  de la journée (aucune finale tant qu'un autre match ou un tableau du jour
  reste à jouer). Réglages conservés par la sauvegarde de session.
- Prévisions : chaque journée est simulée séparément, avec sa propre heure de
  fin visée (réglable) ; le tournoi est rejoué de nombreuses fois (100 tirages par
  défaut, en parallèle) avec chacune des trois stratégies, sur les mêmes
  tirages ; durée d'un match tirée selon une loi exponentielle entre un
  minimum et un maximum, de moyenne réglable (20 min, de 10 à 50 par défaut).
  Indicateurs : début des tableaux à l'heure / léger retard / gros retard
  (+30 min ; début réel = dernière poule lancée), fin moyenne (jour et heure),
  première finale, fin moyenne d'un tableau, cas rares de fin tôt et tard
  (1 fois sur 20), attente d'un joueur sur
  1 tableau et sur 2 tableaux ou plus, plus longue attente, occupation des
  tables, risque de finir après l'heure visée ; puis chaque tableau avec les
  trois stratégies côte à côte, un indicateur à la fois (fin moyenne, fin au
  plus tard, retard au début, attente, après l'heure visée), une valeur par
  case, tableaux dans l'ordre de leur début. Avec assez de tables, l'écran dit que le choix de stratégie ne
  change rien. Temps autour du jeu, ajoutés à chaque match (table et joueurs
  occupés) : appel et rappel d'un joueur qui n'a pas entendu, échauffement
  2 min (3.4.3.1), pauses entre manches (3.4.4.1.1), temps morts (3.4.4.2),
  retour de la feuille ; environ 7 min par match avec les réglages par
  défaut, tous modifiables à l'écran. Les joueurs fictifs ajoutés par « Compléter » ne sont inscrits
  que sur un tableau.
- Poules, classement, tableau final, petite finale, suivi de tous les tableaux.
- Feuilles de match et de poule imprimables (format provisoire 3 × 5).
- Annonces vocales des rencontres, prononciation des noms (profil ou juge-arbitre).
- Suivi sur téléphone en lecture seule, avec « Ma prochaine partie ».
- Export des résultats d'un tableau terminé (format provisoire).
- Fichiers piégés refusés proprement : JSON trop gros ou trop imbriqué,
  bombes zip, numéros de ligne géants, XML à entités (message lisible, jamais
  de plantage). Le PDF du règlement déposé est **copié** dans
  `~/.pingmanager/documents/` (anciens tournois : copie à la première
  ouverture, sinon « fichier introuvable »). La carte n'affiche que sa page :
  un lien d'attribution s'ouvre dans le navigateur du système.

**Moteur sportif (testé)**
- Ordre des parties I.301, placement I.302, classement I.303, placement du
  tableau I.304 et I.305, cartons et disqualifications 3.5.2, catégories
  d'âge II.302 et II.303, accès aux tournois IX.101 et IX.102.

---

## 4. Prêt dans le moteur, mais pas encore branché

| Fonction | Ce qui existe | Ce qui manque |
|---|---|---|
| Sauvegarde automatique et reprise après incident | `ServiceAutosave`, `SessionSalle`, détection d'arrêt anormal (testés) | l'activer dans l'interface et proposer la reprise au démarrage ; aujourd'hui chaque action enregistre le fichier du tournoi, ce qui suffit à rouvrir la salle |
| Convocations par SMS | écran, consentement, textes, journal ; envoi **simulé** (`EnvoiSmsSimule`) | brancher un vrai prestataire (recommandation : Brevo, environ 0,045 € HT par SMS) et décider à qui appartient le compte |
| Emails (vérification de compte et de club) | port `EnvoiEmail` ; envoi **à la console** (`EmailConsole`) | un envoi réel (SMTP avec TLS ou service tiers) |
| Échanges FFTT | import licenciés/clubs et export des résultats en CSV **provisoires** | le format officiel (voir section 8) |
| Paiement en ligne | lien HelloAsso saisi par le club | paiement par tableau et retour qui marque l'inscrit « payé » |
| Phase finale | accès par un bouton « Phase finale (provisoire) » | l'intégrer à la page du jour J (tâche B039) |
| Pointage et lancement | `ServicePointage`, `ServiceLancementTableau` complets | écrans de la veille (B006, B007, B008) |

À valider chez l'utilisateur, jamais vu en session cloud : tuiles IGN et
géocodage sur le vrai réseau, rendu Windows avec mise à l'échelle, voix,
impression directe, pare-feu pour le suivi sur téléphone.

---

## 5. Reste à faire

Ordre conseillé. P1 = indispensable pour un premier tournoi réel.

### Jour J
1. **B038 (P1) — Saisie du score en grille et cartons.** Une case par manche
   et par joueur (Tab pour avancer, Entrée pour valider) au lieu de « 11-9 »
   dans une cellule ; le même composant pour les poules et le tableau final ;
   issues anormales conservées ; un **espace cartons** (jaune, jaune-rouge,
   rouge, par joueur) branché sur `GestionCartonsMatch` et
   `RegistreDisciplinaire`, persisté. La branche `tache/B038-saisie-score-grille`
   existe mais ne contient aucun code.
2. **B039 (P1) — Page du jour J en onglets.** Quand le tournoi passe « jour J »,
   une page au nom du tournoi avec les onglets **Général** (écran du
   juge-arbitre), **Pointage** (tous les tableaux), **un onglet par tableau**
   (poules, arbre) et **Salle**. Retirer les croix « payé » au tirage : être
   dans la liste veut dire avoir payé, le paiement se règle au pointage.
   Supprimer le bouton « Phase finale (provisoire) ».
3. **B005 — Jour J au style v2** (après B039) : pointage, tirage, poules,
   arbre, salle avec les tuiles, pastilles et cartes de la charte. Ne pas
   bouleverser les poules et les arbres, jugés très bons.
4. **B015 — Une couleur par tableau**, attribuée automatiquement et
   modifiable, visible sur les matchs proposés, la salle, les poules, l'arbre,
   les feuilles et le suivi sur téléphone ; persistée (anciens fichiers :
   couleur attribuée à la lecture).

### Veille du tournoi (J-1)
5. **B006 — Tableau de bord de la veille** : par tableau, inscrits, attente,
   payés, horaires et pointage, et des alertes (tableau sans horaire, sous le
   seuil de maintien, juge-arbitre principal non désigné, joueur sans club ou
   sans points).
6. **B007 — Listes de pointage en PDF** : une par tableau, triée par nom (nom,
   prénom, licence, club, points, cases « pointé » et « payé », montant), bouton
   dans la vue Inscriptions.
7. **B008 — Mise à jour des points avant le tirage** : importer un fichier
   licence + points, aperçu (anciens et nouveaux points, licences inconnues),
   puis application ; interdit sur un tableau lancé.

### Brancher l'existant (section 4)
8. Autosave et reprise au démarrage.
9. Envoi réel des emails, puis des SMS.
10. Paiement HelloAsso.

### Sécurité, indispensable avant la vente (B004 et suite)
11. **Données personnelles réelles dans le code** (`ui.infra.DonneesDemo`) :
    l'email de Quentin et celui d'un autre licencié, la licence et la date de
    naissance de Quentin, et des mots de passe de démo écrits en clair.
    **Le dépôt GitHub est public** : remplacer par des identités fictives,
    changer le mot de passe s'il sert ailleurs, et décider s'il faut réécrire
    l'historique git.
12. **Compte de supervision** créé à chaque démarrage, avec un mot de passe
    écrit dans le code : le réserver à un mode démo explicite
    (`-Dpingmanager.demo=true`) et stocker le rôle au lieu de le déduire de
    l'email.
13. Vérifier l'email à la création de compte : sinon, qui crée un compte avec
    l'email d'un référent devient administrateur du club.
14. Liaison de licence : une licence pour un seul compte, et une preuve
    de possession.
15. Politique de mot de passe (10 caractères minimum) et limite d'essais
    (connexion et code à 6 chiffres).
16. Contrôle des droits dans le contexte (`exigerDroit`) et pas seulement par
    la navigation ; suppression du repli par nom de club.
17. ~~Fichiers piégés (JSON, import)~~ — **fait** (B004b).
18. ~~Règlement déposé copié dans le dossier de données~~ — **fait** (B004b).
    Reste : l'**image promotionnelle** garde un chemin absolu libre, même
    correction à faire (`DocumentsDeposes`).
19. ~~Carte : pont Java et navigation de la WebView~~ — **fait** (B004b).
20. Mettre à jour JavaFX (dernière 21.0.x) et PDFBox.
21. RGPD : mention d'information, purge des tournois anciens, effacement d'un
    joueur ; écriture sûre (`fsync`, copie `.bak`, verrou entre deux instances).

### Technique
22. **B002** — Toute lecture de la date ou de l'heure du cœur passe par le port
    `Horloge` (tests de catégorie d'âge à date fixe).
23. Chaque écran relit le fichier du tournoi : envisager un objet partagé par
    tournoi ouvert (plus rapide, refonte plus large).
24. Géocodage en arrière-plan (il fige l'écran jusqu'à 5 s sans réseau).

### Plus tard
25. **B014** — Finitions graphiques et cohérence de tous les écrans (en dernier).
26. **B041** — Statistiques du joueur (rencontres, victoires, adversaires, par
    saison).
27. **B026** — Buvette (cahier des charges à écrire : qui vend, sur quel
    poste, carte ou non, stock).
28. Tableaux réservés aux licences loisir (IX.101), inscription d'un licencié
    par son club, export de la liste des inscrits (attention à l'injection de
    formules dans les cellules).
29. Remise des récompenses (podium à afficher et exporter), version web.

---

## 6. Décisions à prendre (Quentin)

En attendant, le code applique l'option la plus prudente, indiquée entre
parenthèses.

**Règlement sportif**
- « Poule 1 » de l'article I.305.2 = poule A ? (oui)
- Petite finale quand un joueur abandonne en demi-finale.
- Parties de classement pour les places 5 et suivantes ? (non)
- Ordre des rencontres de poule I.301 à confirmer sur un vrai tournoi.
- Remplacement du juge-arbitre après la demande d'homologation (verrouillé ;
  recommandé : autoriser si le grade reste conforme, à vérifier avec IX.104).
- Tirage public et graine (IX.109) : enregistrer la graine du tableau final.
- Manches d'un match perdu par forfait ou abandon ; victoires et défaites par
  forfait dans les statistiques (comptées et signalées à part).
- Lancer un tableau avec des repêchés qui n'ont pas pointé ; inscriptions sur
  un tableau déjà en pointage.

**Produit**
- SMS : compte du club ou de PingManager ? Lire une colonne « portable » à
  l'import ? (non : l'accord du joueur ne se déduit pas d'un tableur)
- Feuilles : « feuille de rencontre » (vocabulaire des équipes) ou « feuille
  de partie » (tournois individuels) ?
- Renommer « Appel vocal », qui peut faire croire à un appel sur le portable ?
- Suivi sur téléphone : ajouter un QR code (nouvelle dépendance ou encodeur
  maison) ? Garder la recherche par licence ?
- Délai de désinscription en ligne (jusqu'à la fin des inscriptions).
- Seuils de couleur des matchs proposés (fixes : 10 et 20 minutes).
- Prévisions : temps autour du match par défaut (appel 1 min, 15 % de
  rappels de 3 min, échauffement 2 min, 30 s de pause entre manches, 20 % de
  temps mort par joueur, feuille 1 min 30) à confirmer sur un vrai tournoi ;
  la durée moyenne du jeu (20 min) est désormais le jeu seul.
- Tarif et dotation dans les finances (tarif en ligne, dotation sur les
  recettes attendues).
- Téléphone dans le compte utilisateur (absent : « Non renseigné »).
- Tournoi annulé : verrous et règlement levés (oui).
- Tournoi importé : le règlement déposé est-il obligatoire avant d'ouvrir le
  jour J ? (non : le bandeau rappelle ce qui manque, le jour J reste ouvert dès
  qu'il y a des tableaux).
- Bornes contre les fichiers piégés (B004b) : JSON 64 Mo et 128 niveaux ;
  tableur 50 Mo, 50 Mo décompressés, taux de décompression 100, 10 000
  fichiers dans l'archive ; PDF du règlement 50 Mo et en-tête `%PDF`. Un
  tableur de plus de 20 000 lignes est tronqué (anomalie signalée), comme
  avant. À ajuster si un vrai fichier est refusé.
- PDF déposés puis remplacés : l'ancienne copie reste dans
  `documents/` (aucune purge, par prudence). Purger les copies orphelines ?
  (non pour l'instant)
- Carte : un lien externe (attributions IGN, OSM, Leaflet) s'ouvre dans le
  navigateur du système ; toute autre navigation est bloquée. (oui)
- Formulaires de saisie (Général, tableau, nouveau tournoi) désormais en
  pleine largeur comme le reste : garder ou les recentrer sur une colonne plus
  étroite ? (pleine largeur, à juger à l'écran).

---

## 7. Conformité FFTT

Les règles sont vérifiées sur les **règlements sportifs et généraux FFTT
2026-2027** (textes officiels, pas des sources secondaires), avec l'article
cité dans le code et les tests.

| Article | Sujet | État |
|---|---|---|
| I.301 | Ordre des parties en poule (3 à 6 joueurs, selon les qualifiés) | ✅ |
| I.302 | Placement en poule, séparation des clubs, départage aléatoire | ✅ |
| I.303 | Classement 2/1/0, forfait/abandon/disqualification, départages | ✅ |
| I.304, I.305 | Placement du tableau final, têtes de série, 2es et 3es | ✅ |
| II.302, II.303 | Catégories d'âge | ✅ |
| IX.101, IX.102 | Accès selon la catégorie du tournoi | ✅ |
| IX.104, IX.111 | Inscription, classement, capacité, tarifs, récompenses | ✅ |
| 3.5.2 | Cartons, points de pénalité, exclusion après 2 disqualifications | ✅ |

Réserves : égalité vraiment persistante (I.303.e) laissée à l'organisateur ;
séparation des clubs non contrainte au-delà du 2e tour ; **aucune validation
par un juge-arbitre sur un vrai tournoi** pour l'instant.

---

## 8. Échanges avec la FFTT

- L'homologation et la remontée officielle passent par les outils fédéraux.
  Pour les **tournois individuels**, c'est **SPIDD** (SPID déconnecté, logiciel
  du juge-arbitre) et non GIRPE (épreuves par équipes).
- Aucun format d'échange ni fichier officiel de licenciés n'est public.
  PingManager utilise en attendant des CSV provisoires (import licenciés et
  clubs, export des résultats).
- Prochaine étape : écrire à la FFTT (**spidv2@fftt.email**) pour obtenir le
  format de remontée et un accord sur l'accès aux données. Pas de moissonnage
  du site fédéral.

---

## 9. Travailler avec Claude

`CLAUDE.md` donne les consignes aux sessions Claude (langue, architecture,
tests, branches) et `.claude/` contient les agents et les skills. Le travail à
faire est la **section 5 de ce README** : une session prend une tâche, la
réalise sur une branche, met ce README à jour et ouvre une Pull Request.
