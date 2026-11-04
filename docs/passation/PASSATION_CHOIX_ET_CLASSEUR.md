# PingManager — choix de la méthode de préparation, classeur d'import, carte corrigée

État au 2 novembre 2026, 13 h. **412 tests verts.** Fait suite à `docs/passation/PASSATION_IMPORT_ET_J-1.md` (sections 1 à 4 toujours valables ; la phase suivante reste « J-1, tournoi prêt »).

## 1. Choix imposé après la création d'un brouillon
- `model.ModePreparation` : `A_CHOISIR`, `COMPLETE`, `IMPORT` ; porté par `Tournoi.modePreparation`, persisté (`modePreparation`) ; un fichier antérieur sans ce champ est relu en `COMPLETE`.
- Création (Gestion → Nouveau tournoi) : le brouillon est créé en `A_CHOISIR` et s'ouvre aussitôt sur **`EcranChoixPreparation`** : deux cartes « Tout préparer dans PingManager » / « Importer les tableaux et les joueurs ». Tant que le choix n'est pas fait, le tournoi rouvre sur cet écran (badge « Mode à choisir », bouton « Choisir le mode » dans la liste).
- `PageGestionTournoi.afficherCompletion` aiguille selon le mode : `EcranChoixPreparation`, `EcranCompletion` (complet) ou **`EcranTournoiImporte`** (import).
- `EcranTournoiImporte` : avancement en 3 étapes (informations, tableaux et joueurs importés, horaires), informations (Modifier → formulaire Général), « Importer le classeur… », « Voir les inscriptions », liste des tableaux avec jauges, « Gérer les tableaux » (lien Horaires), pas d'homologation ni de publication.
- Changement de méthode possible tant qu'aucun joueur n'est inscrit : lien en bas de chaque vue d'ensemble (complet → import si non publié ; import → complet).

## 2. Classeur Excel modèle (`importation.ModeleImport.ecrireXlsx`)
- Écrit à la main (zip + XML, sans bibliothèque), vérifié en l'ouvrant avec LibreOffice. Couleurs de la charte, titre, mode d'emploi, en-tête figé, zone de saisie encadrée, impression ajustée à la largeur.
- **Feuille « Tableaux »** : Nom du tableau* | Points min | Points max | Public (Mixte / Dames / Messieurs, liste) | Jour | Heure | Places | Liste d'attente | Tarif (€). Max seul = « ≤ » ; min + max = tranche ; les deux vides = toutes séries. 4 exemples (A, B, Dames, Open) à modifier ou effacer.
- **Feuille « Joueurs »** : Nom* | Prénom* | Points* | Club* (en-têtes bleu foncé) | Licence | Sexe (H/F, liste) | Catégorie (liste) | Date de naissance | Tableau 1 | Tableau 2 | Tableau 3 (listes reprises de la feuille Tableaux). Lignes « EXEMPLE » ignorées à l'import.
- Feuille cachée « Listes » (catégories). Le CSV modèle reste une liste simple de joueurs.

## 3. Lecture et import
- `LecteurTableur.lireClasseur` : toutes les feuilles (xlsx, ods) par nom ; JSON `{"tableaux": [...], "joueurs": [...]}` → deux feuilles ; CSV → une feuille. `Classeur.feuille("tableau")`, `feuille("joueur")`.
- `core.ImportTableaux` : analyse de la feuille Tableaux (synonymes d'en-têtes, min > max refusé, jour = date ou n° de série, heure = « 9h », « 09:30 » ou fraction de journée), `construire(definition, places, tournoi)` (jour déduit si le tournoi dure un jour).
- `core.ImportJoueurs` : colonne **Catégorie** (« Senior », « V45 », « Vétéran 45 », « J2 », « U13 »… → `lireCategorie`, utilisée si pas de date de naissance) ; plusieurs colonnes **Tableau 1…n** ; `planifier(…, definitions)` : les tableaux définis sont créés (même sans joueur) et leurs points / sexe / places contrôlent les joueurs (surplus en attente puis refus) ; places vides = taille de la liste importée ; un tableau déjà présent n'est pas remplacé (remarque). `Plan.remarques()`.
- `EcranImportJoueurs` : détecte les feuilles, aperçu « feuille Tableaux : N tableaux définis », règles de chaque tableau (points, public, places), jour et heure préremplis depuis la feuille, bouton « Importer N tableaux et M inscriptions » (feuille Tableaux seule acceptée).

## 4. Carte (tuiles décalées, zooms mélangés)
- Cause : transformations CSS 3D de la WebView JavaFX (aggravé par la mise à l'échelle Windows). Correctifs dans `carte.html` : `window.L_DISABLE_3D = true` avant Leaflet, animations de zoom / fondu coupées, tuiles chargées à l'arrêt (`updateWhenIdle`), `detectRetina: false`, `invalidateSize` différé après la mise en page et avant chaque recadrage.
- Vérifié en session avec un serveur de tuiles local (captures `run.sh <dossier> choix`, étape c06) : tuiles jointives, un seul niveau de zoom, marqueurs dans les bonnes tuiles. Le rendu avec les vraies tuiles IGN et la mise à l'échelle Windows reste à confirmer chez Quentin.

## 5. Tests ajoutés
`ImportJoueursTest` : classeur modèle relu (3 feuilles, 4 définitions, exemples ignorés, 3 colonnes Tableau), classeur tableaux + joueurs appliqué (places, attente, refus points/sexe, tableau vide créé, horaires, catégorie V45), catégories en clair ou en code ; `InscriptionsCarteRoundTripTest.mode_de_preparation_relu_et_complet_par_defaut`.
