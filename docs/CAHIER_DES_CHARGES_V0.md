# Cahier des charges de la version 0 (retours de Quentin, 5 novembre 2026, 21 h)

Référence pour toutes les tâches B028 et suivantes. En cas de doute, ce texte
prime sur les écrans existants. Principe général : **séparer la logique du
joueur « lambda » (90 % des utilisateurs) de celle du joueur organisateur**
(compte relié à son club).

---

## 1. Accueil (à refaire complètement)

L'accueil actuel ne sert pas à grand-chose. Il devient un **point d'entrée
(« home »)** qui mène à chaque section et **explique** à quoi elle sert.

Partie pour tous :
- **Profil** : nom, prénom, points, sexe ; en dessous une phrase du type « Vous
  retrouverez vos informations personnelles ci-dessous » et l'accès à Mon profil.
- **Tournois** : présentation et accès à la carte interactive pour chercher
  des tournois.
- **Agenda** (nouveau) : on peut y ajouter des dates ; quand on s'inscrit à un
  tournoi, il s'y enregistre **automatiquement** ; option pour **exporter**
  vers d'autres agendas (Google Agenda…).
- **Statistiques** : pas important pour le moment ; à terme, recense les
  rencontres et donne des statistiques.

Puis une section **bien séparée**, titrée **« Gestion de votre club »**,
**uniquement** pour les comptes reliés à leur club : gestion des tournois,
historique des tournois, buvette (à venir) et autres à venir.

## 2. Mon profil

Deux blocs :
1. **En haut, pour tout le monde** : nom, prénom, email, téléphone ; à côté,
   la **licence FFTT officielle** avec toutes les informations personnelles.
   En dessous : la section **Tournoi** et la section **Appel micro :
   prononciation phonétique** (voir tâche B018).
2. **En dessous, la partie Club** : relier son compte à un club, avec un
   **avertissement bien visible avant** la liaison (pas discret comme
   aujourd'hui) ; si le compte est relié, un **tableau des droits**.

## 3. Onglet Tournois : deux parties, « Mes tournois » et « Carte des tournois »

### Carte des tournois — deux présentations
- **Carte** : la carte et les choix (filtres) à gauche ; ajouter un bouton
  **« Voir plus »** sur chaque tournoi, le double-clic n'étant pas intuitif.
- **Grille / agenda** : présentation ordonnée **par jour**, comme un agenda
  géant, très visible.
- On doit **toujours** pouvoir ouvrir les informations d'un tournoi, même si
  l'on ne peut pas s'y inscrire.
- **Distinction visuelle** nette entre : tournois **en cours**, **à venir**,
  **non accessibles** (on ne peut pas y participer), et non accessibles parce
  que les **tableaux sont pleins** ou qu'**aucun n'est à son niveau**.

### Mes tournois
- En premier, le **tournoi en cours** (s'il y en a un), avec un accès direct
  pour basculer dessus (suivi en direct, B027).
- En dessous, **mes inscriptions** triées par date, et un **agenda**
  (dates personnelles ajoutables, export vers d'autres agendas).
- Une section **tournois passés** : liste, avec mes **performances**
  (victoires, défaites) ; un clic sur un tournoi permet de **revoir ma poule,
  les scores, le tableau principal**…
- **Supprimer** la partie « organisés par le club » : elle va dans
  « Gestion de votre club ».

## 4. Gestion des tournois (dans « Gestion de votre club »)

- En haut, une partie **« Tournoi actuel »** : vide, ou le tournoi en cours.
- En dessous, à la place des groupes « publié / homologation… », une **frise
  chronologique verticale** : **étape 4 Publié**, **étape 3 Tournoi homologué
  validé**, **étape 2 Demande d'homologation en cours**, **étape 1
  Brouillon**, chaque étape listant ses tournois.
- Un moyen **beaucoup plus visible** de créer un tournoi que le petit bouton
  « Nouveau tournoi ».

## 5. Nouveau tournoi

- Un **texte explicatif** : ces informations sont obligatoires pour demander
  l'homologation, ou simplement pour gérer le tournoi.
- Une **icône calendrier** pour choisir les dates.
- Sinon, la nouvelle mise en page, puis on passe à la page suivante.

## 6. Préparation, homologation, publication

- Revoir **complètement** l'affichage de la préparation du règlement : plus
  agréable, plus dynamique.
- Dès la **demande d'homologation**, tous les points exigés pour
  l'homologation **ne sont plus modifiables**.
- **Refusée** → retour au brouillon. **Acceptée** → une **deuxième version**
  de l'écran du tournoi : le règlement **en bas**, et de nouvelles parties
  apparaissent :
  - **Gestion de la salle** : préparer un **plan** ; plusieurs salles,
    visuellement, avec des tables et des allées sur une grille (table = 2 × 2
    cases, allée = 1 × 1…), des repères (table d'arbitrage, entrée / sortie,
    WC…) ; une **mini-carte** pour les sites à plusieurs salles ou plusieurs
    étages.
  - **Stratégie d'ordonnancement** (**très importante**) : le club choisit sa
    stratégie principale, chacune **expliquée** :
    1. avancer au maximum les tableaux commencés tôt, pour libérer de la place ;
    2. tout faire avancer petit à petit ;
    3. faire attendre le moins possible les joueurs, quitte à ralentir
       l'avancement général des tableaux.
  - **Tableau de remplissage** et **gestion de l'argent** : au départ en
    négatif ; total des inscriptions sur le nombre total de places, taux de
    remplissage… beaucoup de données qui évoluent.
- **Publié** ne change presque rien : le règlement n'est plus modifiable,
  seulement consultable (fait par B010).

## 7. Jour J

Comme aujourd'hui, avec :
- **Matchs proposés** gardés, avec un **ordre de priorité en couleur** :
  rouge (urgent), orange, vert, selon l'attente longue, l'importance du match
  et la stratégie d'ordonnancement choisie.
- « Bloqués » ne sert à rien : à la place, la **liste des joueurs en attente
  avec leur chrono d'attente** ; un clic sur un joueur montre **pourquoi il ne
  peut pas jouer** (affiche le ou les tableaux avec son nom **surligné**).
- Garder la vue du jour, mais pouvoir **voir le jour suivant**.
- Tableaux dans l'**ordre des horaires de début**.
- « Tables » devient plutôt **« Salle »**, avec la **disposition** réelle.
- Un bouton **« Lancer un match »** pour ne pas dépendre des matchs proposés.
- En gros **« Terminer la rencontre / la poule »**, et en plus petit, en rouge,
  **« Annuler »** avec une fenêtre de confirmation.
- Au lancement d'un match : **« Comment voulez-vous annoncer la rencontre ? »**
  — rien / par téléphone (SMS) / appel automatique (voix) / les deux ; choix
  **mémorisable** pour ne pas le refaire à chaque fois, mais **facilement
  modifiable**.
- **Pointage et tirage** : au tirage, enlever les croix « payé » (si le joueur
  est dans la liste, il a payé). Créer un **onglet Pointage**, ou mieux : quand
  le tournoi devient « jour J », une page au **nom du tournoi** avec des
  **onglets** : Pointage, un onglet par tableau, Général…
- Tirage : vérifier la conformité avec la manière de faire du logiciel de la
  FFTT.
- **Poules** : très propre, rien à redire. **Arbres** : parfaits.

## 8. Saisie des scores

- Un **tableau** de saisie (une case par manche et par joueur), pas « 11-1 »
  dans une seule cellule.
- Issues anormales : parfait.
- **Ajouter un espace cartons**.

## 9. Feuilles imprimées

- **Feuille de match** et **feuille de rencontre**, pour que les joueurs
  notent le score sur papier et le rapportent.
- Format provisoire (le format officiel viendra plus tard) : nom, prénom, club,
  points, avec l'adversaire ; un tableau 3 × 5 : manches 1 à 5 en en-tête, les
  autres cases vides.

## 10. Plus tard

Statistiques, buvette, optimisation et vérifications du code une fois tout
fusionné.
