# PingManager — passation vers la phase « Remplissage des tableaux et onglet Tournois »

État au 2 novembre 2026. **Phase réalisée** : voir la section 7 (état de fin de phase, 399 tests verts). Les sections 1 à 6 reprennent le cahier des charges de départ.

À lire avec : `ETAT_DU_PROJET.md`, `docs/passation/PASSATION_PHASE_INSCRIPTIONS.md` (ce qui existe pour les inscriptions), `docs/passation/REGLES_CREATION_TOURNOI.md` (règles et champs du tournoi), `docs/passation/CHARTE_GRAPHIQUE.md`, `CONFORMITE_FFTT.md`, et les règlements FFTT 2026-2027 (`docs/reglements/`).

---

## 1. Objectifs de la phase

### A. Prévisualisation du remplissage des tableaux (côté club organisateur)
- Pour chaque tableau : places occupées / capacité (jauge), **liste des inscrits triée par points de licence** (décroissant), puis la liste d'attente dans l'ordre.
- Vue « par joueur » : un participant et tous ses tableaux, avec un tri par points et par club.
- La liste se met à jour à chaque nouvelle inscription.
- Accès : Gestion des tournois → tournoi → nouvelle vue « Inscriptions » (ou carte dans la vue d'ensemble).

### B. Onglet **Tournois** (était `PagePlaceholder` dans `Coquille.rafraichirNavigation`)
1. **Carte interactive** de tous les tournois **publiés** ou **en cours** :
   - « en cours » = `PUBLIE` avec une date du jour comprise dans la période (il n'y a pas de statut EN_COURS) ;
   - un clic sur un marqueur ouvre la fiche du tournoi.
2. **Liste et filtres performants**, synchronisés avec la carte : texte (nom, ville, club), dates, niveau, département ou région, distance au club du joueur, « tableaux à mes points », « places restantes ».
   - Interrupteur **« seulement ceux auxquels je peux participer »**.
   - Les tournois non accessibles **restent visibles** mais **grisés**, avec le motif **en rouge** (ex. « Réservé aux licenciés de l'Essonne (91) »).
3. **Fiche du tournoi** : dates, adresse, niveau, juges-arbitres, dotation ; tableaux avec horaires, points, conditions, places restantes, tarifs ; téléchargement du règlement et de la feuille d'engagement ; en bas, le bouton **« S'inscrire »**.
4. **Inscription du joueur** selon les tableaux et les règles définies par le club, via `ServiceInscription` : quotas par jour ; tableau dames imposé ; pas deux tableaux qui se suivent ; points, sexe et catégories ; catégories non admises ; capacité et liste d'attente. Validation **en direct** au fil des cases cochées, avec le motif en clair. Récapitulatif et montant (tarif en ligne).
5. **Paiement : pas maintenant.** Étape **« Phase de paiement — faite ultérieurement »** avec le montant dû ; inscription enregistrée, paiement « à régler ». Point d'accroche : lien HelloAsso déjà saisi dans `Paiement`.
6. **Liste des participants actualisée** dans la fiche : par tableau, triée par points, visible de tous.
7. **« Mes tournois »** : sections **à venir**, **en cours**, **passés** ; joueur = tournois où sa licence est inscrite ; club = tournois qu'il organise.

---

## 2. Règle FFTT d'accès aux tournois (IX.102, règlements sportifs 2026-2027, p. 109)
| Catégorie | Ouvert à |
|---|---|
| International | licenciés FFTT, pass'tournoi, licenciés des fédérations étrangères ITTF |
| National A | licenciés FFTT, pass'tournoi |
| National B | licenciés FFTT, pass'tournoi, étrangers ITTF de territoire contigu à la région |
| **Régional** | licenciés FFTT **des associations de la région du club organisateur**, pass'tournoi, étrangers contigus |
| **Départemental** | licenciés FFTT **des associations du département du club organisateur**, pass'tournoi, étrangers contigus |
| Promotionnel | licenciés FFTT, pass'tournoi |

- IX.101 : **licence loisir et pass'tournoi → uniquement les tournois promotionnels** et les tableaux qui leur sont réservés ailleurs.
- « Aucune dérogation ne peut être apportée à la présente classification. »
- Département du joueur = celui de son club (`FicheLicence.numeroClub` → `Club.departement`) ; région = ligue du département (`core.Territoires`).

---

## 3. Ce qui existait au départ
- **Comptes** : `CompteUtilisateur.licenceLiee`, `ContexteApplication.ficheLicenceLiee()`, `lierLicence()`, `estOrganisateur()`, `clubsGeres()`, `estTournoiDuCompte(t)`, `fichiersTournois()`, `chargerTournoi(Path)`, `enregistrerTournoi(t, Path)`.
- **Licences** : `RegistreLicences` / `RegistreLicencesFFTT` (démo dans `DonneesDemo`), `FicheLicence`.
- **Inscription** : `ServiceInscription.inscrire(tournoi, joueur, tableau)` → `ResultatInscriptionTournoi`.
- **Comptes de démo** : club Brunoy 08911132 — `soumet.quentin@gmail.com` / `Soumet123/`, licence 9141442 (Quentin SOUMET, Essonne, 923 points) ; ultra admin `admin@pingmanager.fr` / `PingAdmin123/`.
- **Tournoi de référence** : `ui.infra.TournoiDemoBrunoy` (National B, 10–11 avril 2027, 13 tableaux A–M).

---

## 4. Choix techniques retenus
- **Carte** : `WebView` + Leaflet 1.9.4 embarqué (ressources, sans CDN), pont Java ↔ JS (`JSObject`), dépendance `org.openjfx:javafx-web` ; fond CARTO (remplacé depuis par le Plan IGN v2, voir `PASSATION_IMPORT_ET_J-1.md`).
- **Géocodage** : port `Geocodeur` dans le cœur ; implémentation Géoplateforme IGN (`data.geopf.fr/geocodage`, successeur de l'API Adresse) à l'enregistrement de l'adresse ; repli hors ligne : préfecture du département (non stockée).
- **Filtres** : prédicats composables en mémoire (`CatalogueTournois.Criteres`) combinés à `EligibiliteTournoi` ; liste et carte partagent le même résultat.
- **Paiement** : montant « à régler » = tableaux inscrits non payés (`Tableau.aPaye`) ; aucun appel HelloAsso.

---

## 5. Environnement de développement (session cloud)
- Maven Central bloqué : `./outils-dev/tests.sh` et `./outils-dev/run.sh <dossier> [sombre] [brunoy|onglet]` (voir `outils-dev/LISEZMOI.md`). Si les jars JavaFX / PDFBox 2 / JUnit 5 manquent : `apt-get install -y openjfx libopenjfx-jni libpdfbox2-java junit5 xvfb`.
- Le réseau de la session ne passe ni vers les tuiles ni vers le géocodage : à valider chez l'utilisateur.
- Après chaque livraison : zip du projet sans `target/`, `mvn clean javafx:run` chez l'utilisateur. (Avec Claude Code : Pull Request, puis `git pull` et `mvn clean javafx:run` chez Quentin.)

## 6. Conventions de code (à garder)
- **Pas de `switch` sur une énumération** : chaînes de `if`, ou `switch` sur `.name()`.
- Cœur découplé : `model` et `core` ne connaissent ni l'UI ni la persistance.
- UI : fabrique `Ui` (dont `Ui.segment`), jetons CSS dans `theme.css` (clair / sombre). JavaFX CSS ne gère pas `:first-child` / `:last-child` : utiliser des classes.
- Règles FFTT citées par article, d'après les règlements officiels.
- Mettre à jour `docs/passation/PASSATION_*.md` et `docs/passation/REGLES_CREATION_TOURNOI.md` en fin de travail.

---

## 7. État en fin de phase (2 novembre 2026) — FAIT

**399 tests verts** (370 au départ). Démo v11 (`ContexteApplication.VERSION_DEMO = .demo-brunoy-v11`).

### Cœur (sans UI, testé)
- `core.Territoires` + ressource `core/departements.csv` : département → ligue (une ligue par région), préfecture (repli carte), forme « de l'Essonne / du Loiret ».
- `core.EligibiliteTournoi` : accessible / non accessible / à vérifier + motif (IX.101 licence loisir, IX.102 département ou ligue du club organisateur ; département du joueur = celui de son club, à défaut chiffres 3-4 du n° de club).
- `CategorieAge.pour(naissance, date)` : âge au 1er janvier de la **1re année** de la saison (grille 2026-2027 des règlements généraux vérifiée : seniors nés 1987-2007, poussins 2018 et après). `Joueur.categorie` (persistée).
- `ServiceInscription` : nouveaux motifs `CATEGORIE_NON_ADMISE` (catégories exclues du tournoi) et `CATEGORIE_NON_AUTORISEE` (catégories du tableau) ; catégorie inconnue = pas de contrôle. `simuler(tournoi, joueur, sélection)` (essai à blanc cumulatif, tableau dames d'abord puis horaire) et `inscrireSelection` (tout ou rien). Plus de `switch` sur l'énumération.
- `ServiceInscriptionEnLigne` : refus global (licence non liée, IX.102, période d'inscription), simulation, inscription, montants (inscrit / si repêché).
- `CalendrierTournoi` (à venir / en cours / passé ; inscriptions pas encore ouvertes / ouvertes / closes), `CatalogueTournois` (entrées précalculées + `Criteres` composables : texte, période, niveaux, département, ligue, distance, tableaux à mes points, places restantes, seulement accessibles), `RemplissageTableaux` (jauge, inscrits triés par points, attente, vue par joueur, montant à régler = tableaux **inscrits** non payés ; la liste d'attente n'est due qu'au repêchage), `MotifsInscription` (motifs en clair), `JoueurDepuisLicence`.
- `AdresseTournoi.coordonnees` (facultatives, persistées) ; port `Geocodeur` (+ repli `centroideDuDepartement`, jamais stocké).

### Interface
- Gestion des tournois → tournoi → carte **Inscriptions** → `VueInscriptionsClub` : 4 chiffres (participants, places, attente, engagements), « Par tableau » (jauges + liste triée + attente) et « Par joueur » (recherche, tri points / club, puces inscrit / attente). « Actualiser » relit le fichier.
- Onglet **Tournois** (`PageTournois`) : « Trouver un tournoi » (filtres, liste, carte synchronisées ; grisé + motif rouge ; interrupteur « Seulement ceux auxquels je peux participer ») et « Mes tournois » (joueur et club, en cours / à venir / passés).
- Carte `CarteTournois` : WebView + **Leaflet 1.9.4 embarqué** (`ui/carte/`, injecté dans la page, pas de CDN), marqueurs cercles (accessible = primaire, grisé, en cours = anneau cyan), clic → fiche, survol → sélection dans la liste, suit le thème clair/sombre. Fond de carte : voir `PASSATION_IMPORT_ET_J-1.md` (Plan IGN v2). Sans réseau : bandeau « Fond de carte indisponible ».
- `FicheTournoi` : informations (dates, lieu + ligue, niveau et public, homologation, JA, dotation, points retenus, période d'inscription, non admis, contact), tableaux par jour (horaires, points, jauge, places restantes, tarifs en ligne / sur place, dotation, motif rouge si hors de mes conditions), règlement et feuille d'engagement en PDF (règlement déposé copié tel quel), participants par tableau (moi surligné), « S'inscrire » / « Ajouter des tableaux » (désactivé avec motif).
- `AssistantInscription` : 1. Tableaux (validation en direct sur chaque ligne, cochée ou non) → 2. Récapitulatif (+ acceptation du règlement) → 3. **Phase de paiement — faite ultérieurement** (montant, « À régler », bouton HelloAsso grisé, lien du club affiché) → 4. Confirmation (rang par points / position d'attente). L'inscription est enregistrée dans le fichier du tournoi à l'étape 2.
- Géocodage : à l'enregistrement de la section Général (et à la création d'un brouillon), `GeocodeurGeoplateforme` (**`data.geopf.fr/geocodage`**, l'API Adresse ayant été transférée à l'IGN en 2025), délais 2-3 s ; coordonnées gardées si le lieu ne change pas.

### Démo
- 375 joueurs fictifs (`ui.infra.JoueursFictifs`, licences 9900001+, 18 clubs fictifs), graine fixe.
- Brunoy publié (`DÉMO-NB-2027-091 (fictif)`), rempli via `ServiceInscription` : A et H complets avec liste d'attente, C complet, B, I et L à moitié environ, E et J aux trois quarts, D au tiers, F, K et M presque vides ; 233 participants. Inscriptions ouvertes seulement le 1er février 2027 → bouton désactivé avec le motif.
- `ui.infra.TournoisDemo` : régional de Créteil (5 déc. 2026, mêmes règles que Brunoy, accessible), départemental du Loiret (grisé), national A de Lyon (dotation ≥ 5 000 €, JA national), régional de Lille (grisé), promotionnel de Rennes (1er-2 nov. 2026, en cours, Quentin inscrit et réglé).

### Outils
- `outils-dev/tests.sh` et `run.sh` incluent `javafx-web` ; `./run.sh <dossier> onglet [sombre]` capture l'onglet et la vue club (19 écrans).
- `pom.xml` : dépendance `org.openjfx:javafx-web`.

### À valider chez l'utilisateur
- Fond de carte et géocodage IGN (réseau bloqué en session) ; **conditions d'usage commercial des tuiles** à vérifier avant la vente.
- Rendu de la WebView sous Windows/macOS (testé ici en rendu logiciel Xvfb).

### Suite proposée
- Paiement en ligne (HelloAsso : lien par tableau, retour de paiement → `Tableau.marquerPaye`).
- Désinscription par le joueur avant clôture (`ServicePointage.desister`) et politique de forfaits.
- Inscription d'un licencié par son club (saisie de licence), export de la liste des inscrits.
- Tableaux réservés aux licences loisir (IX.101) : à modéliser par tableau.
