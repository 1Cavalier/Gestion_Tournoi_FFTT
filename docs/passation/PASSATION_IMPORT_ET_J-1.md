# PingManager — passation : liste d'attente, carte sans clé, import de joueurs → phase « J-1, tournoi prêt »

État au 2 novembre 2026 (après-midi). **409 tests verts.** Démo `.demo-brunoy-v12`.
À lire avec `ETAT_DU_PROJET.md` (section 0) et `docs/passation/PASSATION_ONGLET_TOURNOIS.md` (section 7).

## 1. Liste d'attente (retour de Quentin)
- Règle : en liste d'attente, **rien n'est dû** (aucune place garantie). Montant à régler = tableaux **obtenus** non payés (`RemplissageTableaux.montantARegler`).
- Si un inscrit se désiste avant le tournoi (`Tableau.desinscrire`), le premier en attente est promu : son tableau devient « à régler ». Le jour J, un repêché règle **au pointage** (`ServicePointage`, inchangé).
- Assistant : statut en direct « liste d'attente, 4e position — rien à payer sauf si repêché » ; étape « Phase de paiement — faite ultérieurement » détaillée par tableau (« Place obtenue 8 € » / « Liste d'attente · 4e — 0 € (10 € si repêché) »), « Montant dû maintenant », encart explicatif ; si tout est en attente : « Rien à régler pour l'instant », pas de bouton de paiement.
- Fiche et « Mes tournois » : pastille « X € à régler », « Réglé » ou « Liste d'attente — rien à régler » (`BlocsInscriptions.pastillePaiement`).
- Démo pour tester : **Créteil, tableau D (toutes séries, 13 h 30) complet, 3 en attente sur 6**. Quentin : cocher B + D → B inscrit (8 €), D en attente (0 €).
- Tests : `ServiceInscriptionEnLigneTest.liste_d_attente_rien_a_payer_puis_a_regler_apres_repechage`, `TournoisDemoTest`.

## 2. Carte : « API KEY REQUIRED »
- Les tuiles CARTO exigent désormais une clé. Remplacées par le **Plan IGN v2** de la Géoplateforme (`data.geopf.fr/wmts`, couche `GEOGRAPHICALGRIDSYSTEMS.PLANIGNV2`, `TILEMATRIXSET=PM`) : **sans clé, licence ouverte Etalab, usage commercial permis**. Repli automatique sur `tile.openstreetmap.org` si l'IGN ne renvoie aucune tuile (6 erreurs). User-agent « PingManager/0.1 » ajouté à la WebView.
- Thème sombre : filtre CSS (inversion + teinte) sur les tuiles, l'IGN n'ayant pas de plan sombre.
- **Non vérifié en session** (réseau bloqué vers data.geopf.fr) : à confirmer chez Quentin.

## 3. Import de joueurs (tournoi préparé hors de PingManager)
- Cœur `core.ImportJoueurs` (pur) : `analyser(cellules)` → `planifier(tournoi, analyse, options)` (aucune modification) → `appliquer(tournoi, plan, horaires)`.
  - En-tête : première des 15 premières lignes contenant ≥ 2 colonnes obligatoires (titre au-dessus toléré). Intitulés tolérants (accents, casse, synonymes : « Nb points », « first name », « Association »…).
  - **Obligatoires : Nom, Prénom, Points, Club** (points → classement et tirage ; club → séparation des clubs, I.302). Facultatives : Licence, Sexe (H/M/F/D…), Tableau (plusieurs séparés par ; , / +), Date de naissance (jj/mm/aaaa, aaaa-mm-jj, n° de série tableur) → catégorie d'âge.
  - Erreurs par ligne (numéro du tableur) : champ manquant, points illisibles ; avertissements : points hors échelle, sexe ou date illisible, doublon.
  - Sans licence : identifiant stable `IMP-` + empreinte (nom, prénom, club) → même joueur sur plusieurs tableaux ; affiché « sans licence ».
  - Tableau du fichier rapproché d'un existant (nom exact, ou lettre avant « – ») ; sinon créé (si autorisé) : toutes séries, places = liste, gratuit, format par défaut, horaires choisis à l'import.
  - Tableau existant : conditions (points, sexe, catégories) et capacité appliquées ; surplus → liste d'attente puis refus. Quotas / tableaux consécutifs **non** appliqués (liste constituée par le club). Pas de doublon si on réimporte le même fichier. Refus si tournoi annulé.
- Lecture `importation.LecteurTableur` (paquet hors cœur, **sans dépendance**) : `.xlsx/.xlsm` (zip + XML, chaînes partagées, texte enrichi, inlineStr), `.ods` (content.xml, cellules et lignes répétées), `.csv/.txt/.tsv` (« ; », « , », tabulation ; UTF-8 avec/sans BOM, sinon Windows-1252), `.json` (liste d'objets ou `{"joueurs": [...]}`) ; `.xls` refusé avec la marche à suivre. XML durci (pas de DTD ni d'entités externes), limites de taille.
- Modèles `importation.ModeleImport` : `.xlsx` (écrit à la main) et `.csv` (UTF-8 BOM, « ; »).
- Écran `EcranImportJoueurs` (Gestion → tournoi → carte Inscriptions « Importer des joueurs… », ou vue Inscriptions) : 1. fichier + modèles ; 2. destination (selon la colonne Tableau / tous dans un tableau existant ou nouveau ; créer les tableaux absents) ; 3. aperçu (colonnes repérées, par tableau : nouveau/existant, +inscrits, attente, refus, jour et heure des nouveaux tableaux ; lignes non importées et points à vérifier) ; « Importer N inscriptions » ; bilan → « Voir les inscriptions ».
- Tableaux figés (avec inscrits) : nouveau lien **« Horaires »** dans la grille des tableaux (jour, début, pointage) ; les règles restent figées comme décidé le 16/10.
- `EcranCompletion` relit le tournoi à chaque retour sur la vue d'ensemble (évite d'écraser un import ou une inscription en ligne avec une copie périmée).
- Tests : `importation.ImportJoueursTest` (CSV 1252 avec titre et guillemets, CSV virgule/BOM/tabulation, modèle xlsx relu, xlsx chaînes partagées + date série, ods répété, JSON, colonnes obligatoires, plan et application, tableau par défaut).
- Captures : `./outils-dev/run.sh <dossier> import` (liste d'attente puis import de 40 joueurs dans « Open d'automne »).
- `docs/passation/REGLES_CREATION_TOURNOI.md` non modifié dans ce lot (l'import ne change pas les champs du tournoi) ; l'ajout « Horaires » des tableaux figés est décrit ici.

## 4. Phase suivante : J-1, tournoi prêt
Objectif : la veille, le club vérifie et fige ce qui sert en salle. Pistes :
- tableau de bord J-1 : par tableau, inscrits / attente / payés, horaires et pointage, alertes (tableau sans horaire, sous le seuil de maintien, JA non désigné, joueur sans club ou points) ;
- export des listes de pointage (PDF par tableau, triées par nom, cases pointé / payé) ;
- mise à jour des points (fichier de points mensuels → `Joueur.setPoints`) avant le tirage ;
- ouverture du pointage tableau par tableau le jour J (`Tableau.ouvrirPointage`, `ServicePointage`) et lancement (`ServiceLancementTableau`), déjà dans le cœur, sans écran ;
- adaptation des écrans aux petites tailles (demandée, non prioritaire).
