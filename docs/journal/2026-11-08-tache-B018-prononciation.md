# Prononciation des noms en alphabet phonétique (B018) — 2026-11-08

- Branche : `tache/B018-prononciation`
- Agent : `developpeur` (étape de pipeline)
- Pull Request : #73
- Statut : fusionné après verdict GitHub Actions

## Pour Quentin

Quand la voix des annonces écorche un nom, on peut maintenant lui apprendre à
le dire, en alphabet phonétique (par exemple « ŋɡu.jɛn » pour Nguyen). Chacun
peut saisir sa propre prononciation dans « Mon profil », partie « Appel
micro », avec un bouton « Écouter » et une aide (symboles du français et
exemples, cliquables pour les insérer). Le juge-arbitre peut aussi la saisir
ou la corriger le jour J, pour un joueur et pour son club (menu « Voix » →
« Prononciation des noms… »). Les prononciations sont gardées dans les données
du logiciel : elles resservent à tous les tournois suivants.

**La session cloud ne peut pas écouter** : la voix (Windows) n'a été vérifiée
que par des tests sans son. L'écran du profil a été capturé sous Xvfb
(affichage correct) ; l'écran du jour J n'a pas été capturé.

## À tester à l'écran

- Compte de démo : profil par défaut (Quentin, licence reliée).
- Chemin 1 : Mon profil → carte « Appel micro : prononciation phonétique ».
  1. Cliquer « Aide : symboles et exemples » : les symboles (ɑ̃, ɛ̃, ʁ, ʃ…)
     s'affichent ; survoler un symbole montre « comme dans … ».
  2. Cliquer dans le champ « Nom », puis sur des symboles : ils s'insèrent au
     curseur. Cliquer un exemple (« Dupont → dy.pɔ̃ ») l'insère aussi.
  3. Taper une majuscule ou « < » puis « Enregistrer » : message d'erreur précis.
  4. « Écouter » (sous Windows) : la voix dit « Quentin Soumet » avec la
     prononciation. Hors Windows : message « Écoute impossible… ».
  5. « Enregistrer », fermer et rouvrir le logiciel : la saisie est gardée.
- Chemin 2 : Gestion des tournois → « Tournoi de démonstration du jour J » →
  Suivre le tournoi → menu « Voix » → « Prononciation des noms… ».
  1. Rechercher un joueur (nom, licence ou club), le choisir.
  2. Saisir prénom / nom / club, « Écouter », « Enregistrer » : la liste
     affiche la prononciation à côté du nom.
  3. Revenir (← Jour J), envoyer un match de ce joueur sur une table avec
     l'annonce vocale : le nom est dit avec la prononciation.
  4. Un joueur dont la licence n'est pas dans la base : la ligne « Club »
     explique que le numéro du club est inconnu.
- Thème clair et sombre : touches de symboles (fond gris doux, bleu au survol),
  encart d'aide, liste des joueurs.

## Questions pour Quentin

### 2026-11-08 — Une prononciation pour le prénom et une pour le nom ?
- Contexte : l'annonce découpe déjà prénom, nom et club ; le registre garde
  donc deux champs par joueur (prénom, nom), chacun facultatif.
- Article FFTT : sans objet (fonction d'organisation).
- Options : A) deux champs (prénom, nom) ; B) un seul champ pour « Prénom Nom ».
- Choix provisoire de l'agent : A (plus précis, on ne corrige que ce qui est mal dit).
- Réponse de Quentin :

### 2026-11-08 — Qui peut modifier la prononciation d'un joueur ?
- Contexte : le joueur (profil) et le juge-arbitre (jour J) écrivent la même
  entrée ; la dernière saisie gagne. La prononciation d'un club n'est saisie
  qu'au jour J.
- Options : A) garder ; B) la saisie du joueur prime sur celle du juge-arbitre ;
  C) le club se règle aussi depuis la gestion du club.
- Choix provisoire de l'agent : A.
- Réponse de Quentin :

### 2026-11-08 — Clubs : retrouvés par la licence du joueur
- Contexte : un joueur inscrit ne porte que le nom de son club ; le numéro est
  retrouvé par sa licence dans la base des licences. Joueur absent de la base :
  pas de prononciation de club possible pour lui.
- Options : A) garder ; B) chercher aussi le club par son nom dans le
  référentiel des clubs.
- Choix provisoire de l'agent : A (aucune confusion entre homonymes).
- Réponse de Quentin :

## Notes pour l'état du projet

- Cœur : `core.RegistrePrononciations` (licence → prénom/nom, numéro de club →
  club), `core.ValidationPrononciation` (liste blanche API, 60 caractères au
  plus, refus de `< > & " '`, chiffres, majuscules, contrôles),
  `core.ResolveurPrononciationRegistre` ; `model.PrononciationJoueur`,
  `model.PrononciationClub`. `AnnonceVocale` porte la licence de chaque
  morceau (`licences`, constructeur à 4 arguments gardé) ;
  `ResolveurPrononciation.phonemesIpa(segment, licence)` (méthode par défaut) ;
  `GenerateurSsml.essai` et `ServiceAnnoncesVocales.ecouterPrononciation`
  (même verrou de parole que les annonces).
- Persistance : section `prononciations` (`joueurs`, `clubs`) du fichier
  applicatif `pingmanager-application.json`, version inchangée (1) ; anciens
  fichiers relus avec un registre vide ; entrées invalides ignorées à la lecture.
- Interface : `FormulairePrononciation` (partagé), `PresentationPrononciation`,
  `EcranPrononciations` (jour J, menu « Voix »), carte « Appel micro » de
  `PageCompte` (la pastille « Bientôt » disparaît). Résolveur branché dans
  `ContexteApplication`. Section CSS « Prononciation phonétique des noms (B018) ».

## Passations

- correcteur : lecture des `prononciations` tolérante (section, tableau ou élément de mauvais type ignoré) ; validation API sur la forme NFD, stockage NFC, diacritiques empilés acceptés.
- correcteur : classes CSS orphelines retirées, javadocs remises à leur place et imports au lieu de noms qualifiés (`ContexteApplication`, `EcranSalle`) ; 1412 tests, 0 échec (`outils-dev/tests.sh`).

## Fusion

Fusion de `origin/main` sans conflit ; `verifier-fusion.py` : OK, rien à signaler.

## Chiffres

- Tests : 1358 → 1412, 0 échec après synchronisation avec main (mesurés par `outils-dev/tests.sh`, Maven inaccessible ; GitHub Actions fait foi)
- Version de la démo : inchangée
