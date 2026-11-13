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
mvn -B test              # lancer les 1 698 tests (doit finir à 0 échec)
```

Point d'entrée : `com.pingmanager.ui.Lanceur`.

### Au premier lancement
- Les **données de démonstration** sont créées : clubs, licenciés fictifs et
  deux tournois du club de Brunoy :
  - le **National B de Brunoy** (10-11 avril 2027, 13 tableaux), le vrai
    tournoi du club, à son état réel : **brouillon**, sans numéro
    d'homologation ni inscrit, avec les vraies personnes (juges-arbitres
    Samuel LOOBUYCK et William PRIOU ; contact du club remplacé par un référent fictif). Son règlement
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
- Les **comptes de démo** (club de Brunoy, joueur licencié, identités
  fictives en `example.org`) sont affichés dans la **console** au démarrage.
  Le compte de **supervision** n'existe qu'en mode démo explicite
  (`mvn javafx:run -Dpingmanager.demo=true`) : son mot de passe est tiré au
  hasard à chaque démarrage et affiché dans la console de ce démarrage.
- Mots de passe : 10 caractères au moins à la création ; après 5 essais
  faux (connexion ou code à 6 chiffres), attente de 1 min, puis 2, 4… jusqu'à
  1 h.
- Les données sont rangées dans `~/.pingmanager/pingmanager/` (ou
  `$XDG_DATA_HOME/pingmanager/`) : un fichier applicatif (comptes, clubs,
  accès) et un fichier `.ptt.json` par tournoi.
- Si les données de démo changent dans le code, la constante
  `ContexteApplication.VERSION_DEMO` est incrémentée (v20 actuellement) et la
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
- **Sécurité des comptes (B044)** : un compte créé reçoit un code à son email
  et n'a **aucun droit** (club, organisateur, supervision) avant de l'avoir
  saisi (étape après la création, ou carte « Vérifier mon email » dans Mon
  profil) ; anciens comptes considérés vérifiés. Licence : numéro + date de
  naissance, puis code envoyé au référent du club de la licence ; une licence
  pour un seul compte ; « Délier » par le titulaire ou par un administrateur
  du club. Droits contrôlés à chaque opération (`exigerDroit`, message
  « Action refusée… ») ; un club se désigne par son numéro seulement ; le code
  de rattachement ne vaut que pour le club demandé.
- **Données et RGPD (B044)** : mention d'information à la création de compte,
  à l'inscription (en ligne et sur place) et dans Mon profil ; pour un
  organisateur, Mon profil → « Données personnelles des joueurs » : aperçu
  puis suppression confirmée des tournois terminés depuis N ans (3 par
  défaut), effacement d'un joueur à sa demande (nom, prénom, portable ; licence
  et résultats gardés), journal sans donnée personnelle
  (`pingmanager-journal-rgpd.json`). Écriture sûre de tous les fichiers
  (fsync, copie `.bak` relue si le fichier est abîmé) et verrou court par
  tournoi entre deux postes (`.verrou`, message clair si occupé). Image
  promotionnelle copiée dans `documents/` comme le règlement.

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
- **Tournoi en cours** (B043) : groupe de navigation entre « Général » et
  « Gestion », visible seulement pour un organisateur dont un tournoi se joue
  (homologué ou publié et c'est un de ses jours, ou un tableau commencé et pas
  terminé, jusqu'au lendemain du dernier jour ; en mode démo, la copie « jour J »
  de Brunoy). **Gestion** ouvre directement le jour J du tournoi (et sa fiche,
  avec tous ses onglets), **Tableau** liste les tableaux par jour avec leur
  statut et ouvre pointage et tirage, poules ou arbre, **Pointage** ouvre le
  poste de pointage. Sélecteur en haut des pages s'il y a plusieurs tournois en
  cours. La dernière page ouverte est rouverte à la connexion suivante.
- **Pointage sur deux jours** (B043) : onglets des jours (avec les non-pointés),
  ouverts sur aujourd'hui (la veille pendant la nuit si elle n'est pas finie) ;
  tableaux, filtres et tuiles du jour, ligne « Total du tournoi », case « Tous
  les jours » et pastille du jour ; tableau sans horaire rattaché au premier jour
  (« jour non défini »). Pointage, paiement et clôture restent par tableau ; le
  jour est noté au journal.
- **Retraits** (B043) : « Désinscrire… » ouvre une fenêtre de motifs (décision
  du joueur d'abord, blessure ou maladie, empêchement prévenu, erreur ou
  doublon, retiré par le JA ou l'organisateur, transfert, autre avec précision).
  Vue **Retraits** : non pointés (libérés à la clôture) et désinscrits séparés,
  avec motif, tableau, jour, heure, poste, « avait payé : à rembourser ou à
  reporter ? » et **Annuler le retrait** (seulement s'il reste une place).
  Les retraits faits par le juge-arbitre (clôture, désistement, cas
  particulier) y figurent aussi.
- **Poste de pointage** (B041, menu Tournoi en cours → **Pointage**) : écran autonome
  pour un ordinateur d'accueil, sans score ni accès au reste du tournoi.
  Recherche toujours active (nom, prénom ou licence, sans tenir compte des
  accents), sélecteur de tableau, filtres (non pointés, pointés, non payés,
  payés), tuiles inscrits / pointés / payés / encaissé / en attente ; une
  ligne par tableau avec les cases **Pointé** et **Payé**, le **mode de
  règlement** (en ligne, espèces, chèque, autre ; pastille « Payé en ligne »)
  et **Désinscrire…** avec motif (place libérée, liste d'attente repêchée).
  Le fichier est relu avant chaque action et enregistré juste après ; chaque
  action entre au **journal du pointage** avec l'heure et le nom de
  l'ordinateur, pour préparer la synchronisation entre postes.
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
| Changement de mot de passe | `ServiceAuthentification.changerMotDePasse`, `ContexteApplication.changerMotDePasse` (ancien exigé, 10 caractères, limite d'essais) | un formulaire dans « Mon compte » |

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
   *Navigation remplacée par B043* (groupe « Tournoi en cours ») : reste de
   B039 les croix « payé » au tirage et le bouton « Phase finale (provisoire) ».
2 bis. ~~B043 — Tournoi en cours~~ — **fait** (section 3) ; à vérifier à
   l'écran. Reste : les pages « Gestion » et « Tableau » ouvrent les écrans
   existants, à refaire (tâche suivante).
3. **B005 — Jour J au style v2** (après B039) : pointage, tirage, poules,
   arbre, salle avec les tuiles, pastilles et cartes de la charte. Ne pas
   bouleverser les poules et les arbres, jugés très bons.
4. **B015 — Une couleur par tableau**, attribuée automatiquement et
   modifiable, visible sur les matchs proposés, la salle, les poules, l'arbre,
   les feuilles et le suivi sur téléphone ; persistée (anciens fichiers :
   couleur attribuée à la lecture).

- **B041 suite — Synchroniser le poste de pointage** avec l'ordinateur du
  juge-arbitre (aujourd'hui, les deux écrans travaillent sur le même
  fichier : même ordinateur ou dossier partagé). Le journal horodaté
  `journalPointage` (licence, tableau, type, valeur, heure, poste) est prêt
  à être échangé et rejoué.

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
13. ~~Email vérifié à la création de compte~~ — **fait** (B044).
14. ~~Liaison de licence : un compte, une preuve~~ — **fait** (B044).
16. ~~Droits contrôlés dans le contexte, plus de repli par nom~~ — **fait** (B044).
17. ~~Fichiers piégés (JSON, import)~~ — **fait** (B004b).
18. ~~Règlement et image promotionnelle copiés dans le dossier de données~~ —
    **fait** (B004b, B044).
19. ~~Carte : pont Java et navigation de la WebView~~ — **fait** (B004b).
20. Mettre à jour JavaFX (dernière 21.0.x) et PDFBox.
21. ~~RGPD et écriture sûre~~ — **fait** (B044). Reste : l'**envoi réel des
    emails** (point 9) est indispensable avant la vente : sans lui, les codes
    (email, club, licence) ne partent qu'à la console. Suite possible :
    proposer la purge au démarrage, effacement des comptes utilisateurs.

### Technique
22. **B002** — Toute lecture de la date ou de l'heure du cœur passe par le port
    `Horloge` (tests de catégorie d'âge à date fixe).
23. Chaque écran relit le fichier du tournoi : envisager un objet partagé par
    tournoi ouvert (plus rapide, refonte plus large).
24. Géocodage en arrière-plan (il fige l'écran jusqu'à 5 s sans réseau).

### Plus tard
25. **B014** — Finitions graphiques et cohérence de tous les écrans (en dernier).
26. **B042** (ex-B041, identifiant réattribué au poste de pointage) —
    Statistiques du joueur (rencontres, victoires, adversaires, par
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

**Sécurité des comptes (B004a, points 11, 12, 15)**
- **Réécrire l'historique du dépôt public ?** Les anciens commits contiennent
  toujours l'email, la licence et la date de naissance de Quentin, l'email du
  référent de Brunoy et les mots de passe de démo (`Soumet123/`,
  `PingAdmin123/`). Options : (a) réécrire l'historique (`git filter-repo`,
  poussée forcée sur `main`, branches `tache/*` et Pull Requests à refaire,
  clones et copies déjà faits non couverts) ; (b) considérer ces données comme
  publiques et changer les mots de passe concernés partout où ils servent.
  (Rien de réécrit : les données sont seulement retirées du code actuel.)
- Le profil de démo fictif (`organisateur.demo@example.org`, mot de passe
  fictif écrit dans le code) reste créé sur chaque poste, administrateur du
  club de Brunoy : le réserver lui aussi au mode démo ? (non pour l'instant :
  la démo et les tests s'en servent).
- Un ancien compte de supervision garde son rôle à la relecture, mais son
  privilège n'est honoré qu'en mode démo, et son mot de passe y est retiré au
  hasard. (oui)
- Postes déjà installés : l'ancien compte réel et l'accès de l'ancien
  référent restent dans le fichier applicatif local. Les purger au démarrage ?
  (non : aucun effacement automatique de compte).
- Limite d'essais en mémoire : un redémarrage la remet à zéro. La persister ?
  (non : application de bureau, l'attaque en ligne suppose déjà l'accès au poste).
- Noms des juges-arbitres réels du National B (règlement public du club) dans
  le code et le README : les garder ? (oui).

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
- **Poste de pointage (B041)** — textes FFTT 2026-2027 sur le désistement et
  la clôture du pointage non fournis dans la session, rien n'a été deviné :
  (1) désistement après la clôture du pointage ou sur un tableau lancé :
  refusé au poste, « voir le juge-arbitre » (qui garde le désistement pour
  cas particulier) ; (2) un joueur qui avait payé puis se désiste n'est pas
  marqué remboursé (le club décide) ; (3) paiement modifiable même après le
  lancement (c'est de la caisse) ; (4) montant : tarif en ligne pour « en
  ligne », tarif sur place sinon, et pour un paiement noté avant B041 (mode
  « non noté ») ; (5) les joueurs en liste d'attente n'apparaissent pas au
  poste tant qu'ils ne sont pas repêchés ; (6) nom du poste = nom de
  l'ordinateur. Articles à citer : à fournir.
- **Tournoi en cours (B043)** — choix provisoires, réversibles :
  (1) « en cours » : homologué ou publié et c'est l'un de ses jours, ou un
  tableau commencé et pas terminé, mais seulement jusqu'au lendemain du
  dernier jour (un tableau oublié « commencé » ne fait plus apparaître le
  groupe ; le tournoi reste dans « Gestion des tournois ») ; un brouillon
  importé dont un tableau a commencé compte aussi ; (2) tableau « terminé » :
  phase finale jouée, ou poules terminées pour un tableau sans phase finale ;
  (3) jour du pointage pendant la nuit : avant 6 h, la veille si elle n'est pas
  terminée ; (4) la dernière page ouverte est mémorisée par compte dans les
  préférences du système (elle ne l'était pas avant B043, contrairement à ce
  qu'indiquait la consigne) ; (5) annuler un retrait : seulement sur un
  tableau non lancé et s'il reste une place, y compris après la clôture (le
  joueur devra pointer) ; son paiement n'est pas rétabli d'office ; (6) un
  retrait antérieur à B043 ne garde pas la fiche du joueur : son annulation
  est refusée (« Inscrire sur place » à la place) ; (7) le « cas particulier »
  du juge-arbitre est noté « retiré par le juge-arbitre ou l'organisateur ».
  **Textes FFTT 2026-2027 à fournir** (désistement, forfait, non-présentation
  au pointage, remboursement des engagements) : aucun article n'est cité,
  rien n'a été deviné ; aucun remboursement automatique, le club décide.
- **Sécurité et RGPD (B044)** — choix provisoires, réversibles :
  (1) anciens comptes (fichier sans le champ `emailVerifie`) considérés
  vérifiés ; tant que les emails ne partent pas vraiment, le code n'est lu
  qu'à la console, ou affiché à l'écran en mode démo seulement ;
  (2) **preuve de licence** : la fiche fédérale ne porte pas d'email, donc
  date de naissance + code envoyé au référent du club de la licence (valable
  7 jours) ; alternatives : code à l'email du licencié quand la source FFTT
  le fournira, ou validation directe dans l'écran du référent ; une fiche
  sans date de naissance n'exige que le code du référent ; deux comptes qui
  portaient déjà la même licence dans un ancien fichier restent tels quels ;
  (3) **supervision** (mode démo) : peut modifier, homologuer et consulter
  tous les tournois, mais ni créer, ni supprimer, ni gérer les comptes d'un
  club ; (4) un **ancien tournoi sans numéro de club** n'appartient plus à
  aucun club (plus de repli par nom) : il faudra le rattacher à la main ;
  (5) effacer les numéros de portable d'un tournoi terminé n'exige aucun
  droit (c'est une suppression de données) ; (6) **purge** : seulement les
  tournois des clubs du compte, sur confirmation ; **effacement d'un joueur** :
  seulement dans les tournois des clubs du compte (le joueur s'adresse à
  chaque club) ; le compte utilisateur du joueur n'est pas supprimé ;
  (7) **durée légale de conservation** des résultats et du numéro de licence
  (obligation fédérale) : non devinée, à confirmer ; durée de purge par
  défaut 3 ans ; (8) **texte de la mention RGPD** (`MentionInformation`) à
  faire valider par Quentin ou un juriste, et le contact « administrateur de
  PingManager sur cet ordinateur » à préciser ; (9) verrou : expiré après
  30 s, 3 s d'essais avant le message « tournoi occupé ».
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

Réserves : désistement, motifs de retrait, non-présentation au pointage et
remboursement (B041, B043) appliqués sans article cité, faute de texte fourni (voir section 6) ;
égalité vraiment persistante (I.303.e) laissée à l'organisateur ;
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
