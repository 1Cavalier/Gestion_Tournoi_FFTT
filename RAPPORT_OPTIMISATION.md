# Rapport d'optimisation — 6 novembre 2026 (branche `tache/optimisation-carte`)

Comportement inchangé, aucun test modifié ; 1044 → 1046 tests verts (2 ajoutés).
Mesures : tests temporaires chronométrés hors dépôt, JVM chaude, session cloud.

## 1. Mesures

| Parcours | Mesure |
|---|---|
| Démarrage, 1er lancement (création des 7 tournois de démo) | 500 ms ; lancements suivants : 2 ms |
| Relecture complète des 7 fichiers de démo (267 Ko) | 40 à 90 ms par passage (270 ms à froid) |
| Lecture d'un tournoi (Brunoy, 86 Ko) / enregistrement | 6–7 ms / 8–9 ms |
| `ServiceSalle.tableauDeBord` (démo jour J), refait à chaque action en salle | 1,1 ms |
| `Ordonnanceur.ajouterMatch` × 5 987 matchs | 19 ms → **1,2 ms** |
| Règlement Brunoy : génération / rendu PDF | 10 ms / 40–75 ms |
| Ordonnanceur, 2 000 matchs prêts (test B025 existant) | inchangé, quelques ms par vague |

Plus grosses classes : EcranSalle 1152 l., GenerateurReglement 1102, Tournoi ~870,
ContexteApplication ~870, EcranSportif ~830, Ui ~860, TournoiMapper 825.
Plus longues méthodes : FormulairesSection.general 255 l. (découpée ici),
PageTournois.construireExplorateur 192, FormulaireTableau.construire 148,
EcranImportJoueurs.majApercu 130, FormulaireNouveauTournoi.build 124.
Un détecteur de doublons (fenêtres de 5 à 8 lignes) trouve peu de copies longues :
la duplication porte surtout sur de petites méthodes d'aide recopiées.

## 2. Corrections faites (un commit chacune)

1. **Lecture seule en cache** : `ContexteApplication.lireTournoi(Path)`. Un fichier
   n'est relu que si sa date ou sa taille a changé ; chaque enregistrement fait par
   l'application le retire du cache. Utilisée par l'accueil de la gestion, la
   supervision, le choix de l'écran de complétion et la relecture toutes les 30 s du
   suivi en direct. Avant : 40–90 ms par affichage (7 fichiers) ; après : 0 ms si rien
   n'a changé. 2 tests ajoutés (`LectureSeuleTournoiTest`).
2. **Ordonnanceur** : un ensemble répond à « ce match est-il déjà géré ? » en temps
   constant (avant : parcours de la liste à chaque ajout). 19 → 1,2 ms.
3. **Aides `Ui`** : `Ui.filet()`, `Ui.afficherSi(nœud, visible)`, `Ui.libelleChamp`
   à la place de 8 copies locales (6 écrans).
4. **Modèle** : classe `Textes` (`obligatoire`, `optionnel`) à la place de 14 copies
   d'`exigerNonVide` / « trim, sinon null ». Les variantes au message ou au test
   différents (isBlank contre trim) sont gardées.
5. **Formulaire « Général »** : la méthode de 255 lignes devient la classe
   `SectionGenerale` en étapes nommées (club, nom, échelon, phase, lieu, dates,
   correspondant, verrous, enregistrer), sans tableaux `x[0]` pour partager l'état.
6. **`Ui.message(erreur, succès)`** : bloc recopié dans EcranSalle et VuePhaseFinale.
7. **`SuiviTournoi.ORDRE_DE_LA_JOURNEE`** : un seul comparateur des tableaux pour le cœur
   et l'interface.

L'interface n'a pas pu être vérifiée à l'écran (session cloud) : validation visuelle
du formulaire « Général », des grilles (filets) et des messages de la salle à faire.

## 3. Flux de données : constats

- **Un seul point d'accès** : tous les écrans lisent et écrivent les tournois par
  `ContexteApplication` (`chargerTournoi`, `lireTournoi`, `enregistrerTournoi`) ;
  `DepotTournoi` n'est appelé directement qu'à l'amorçage des démos et dans
  `inscrireEnLigne` / `desinscrireEnLigne`.
- **Deux chemins d'écriture** : `enregistrerTournoi` passe par `ServiceFichierTournoi`
  (récents, autosave) ; l'inscription en ligne écrit par `DepotTournoi.sauvegarder`
  sans passer par le service. À harmoniser (voir 4).
- **État dupliqué entre écrans** : chaque écran recharge sa propre copie du tournoi
  (21 appels à `chargerTournoi` dans `ui.ecran`). Exemples : `FicheTournoi` relit le
  fichier à chaque bascule fiche ↔ suivi ; `EcranJourJ.afficherListe` relit tout le
  fichier à chaque retour sur l'écran principal alors que le tournoi en mémoire vient
  d'être enregistré ; `EcranChoixPreparation` lit le fichier à l'affichage puis le relit
  au clic.
- **Écritures sur le fil JavaFX** : `EcranCompletion` enregistre le tournoi complet en
  12 endroits (8–25 ms par écriture selon la taille). Acceptable aujourd'hui, à suivre
  pour les gros tournois.
- **Recalculs** : `ServiceSalle.tableauDeBord` reconstruit tout (ordonnanceur compris)
  à chaque action ; 1,1 ms sur la démo, donc rien d'urgent.
- **Outil de test** : `outils-dev/tests.sh` compile par défaut dans `/tmp/pingmanager-dev`,
  partagé par les sessions parallèles : deux lancements simultanés se marchent dessus
  (faux échecs `NoSuchMethodError`). Contournement : `SP=/tmp/<propre> bash outils-dev/tests.sh`.

## 4. Propositions demandant une décision de Quentin

1. **Session de tournoi ouverte** : un objet unique par fichier ouvert (tournoi en
   mémoire + enregistrement) partagé par les écrans d'un même tournoi, au lieu de
   relire le fichier à chaque changement d'écran. Supprime les relectures de
   `EcranJourJ`, `FicheTournoi`, `EcranChoixPreparation`. Refonte moyenne, touche
   `PageGestionTournoi` et la plupart des écrans.
2. **Un seul chemin d'écriture** : faire passer l'inscription en ligne par
   `ServiceFichierTournoi` comme les autres écritures (change le comportement des
   « récents » et de l'autosave : décision produit).
3. **EcranSalle (1152 l.) en composants** : carte de table, liste des matchs prêts,
   panneau « pourquoi ne joue-t-il pas ? », comme `EditeurScorePoule`. Gain de
   lisibilité, risque visuel : à faire avec captures.
4. **GenerateurReglement (1102 l.)** : une classe par groupe d'articles (dotations,
   articles optionnels, inscriptions) derrière le générateur actuel, sans changer le
   document produit (tests de règlement existants comme filet).
5. **TournoiMapper (825 l.)** : découper `lireTournoi` / `ecrireTournoi` (~100 l.
   chacune) en sous-mappers, comme `DerouleMapper` et `PlanSalleMapper`, format
   JSON inchangé.
6. **Autres méthodes géantes de l'interface** : `PageTournois.construireExplorateur`,
   `FormulaireTableau.construire`, `EcranImportJoueurs.majApercu`,
   `FormulaireNouveauTournoi.build` : même découpage que pour `SectionGenerale`, à faire
   avec une validation visuelle.
7. **Écritures hors du fil JavaFX** si de gros tournois rendent l'enregistrement
   perceptible (mesurer d'abord sur un vrai fichier de 400 joueurs).
