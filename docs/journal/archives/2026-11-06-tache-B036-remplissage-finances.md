# Remplissage et finances du tournoi — 2026-11-06

- Branche : `tache/B036-remplissage-finances`
- Agent : chaîne `developpeur` (étape de pipeline), puis `verificateur-modele`, `relecteur-code`, `documentaliste`
- Pull Request : aucune à ce stade (complété à l'étape `integrateur`)
- Statut : en attente de fusion (`integrateur`)

## Pour Quentin

- Une fois le tournoi homologué, l'écran du tournoi affiche une vraie carte « Remplissage et finances » à la place de la carte « bientôt ».
- Remplissage : inscrits sur places et taux de remplissage, au total et tableau par tableau, avec la liste d'attente.
- Argent : recettes attendues (tarif × inscrits), déjà encaissé (joueurs marqués « payé »), dotation promise (fixe + part des inscriptions) et solde, négatif au départ, plus le solde si tous les inscrits règlent.
- La liste d'attente ne compte pas dans l'argent : on ne paie qu'une fois la place obtenue.
- Ce qui n'est pas encore pris en compte : un paiement partiel ou un trop-payé (un joueur est soit « payé », soit non), et la réduction de dotation prévue par certains règlements (voir les questions).

## À tester à l'écran

- Compte de démo : un tournoi homologué ou publié (Brunoy une fois homologué).
- Chemin : Gestion des tournois → le tournoi → écran du tournoi (deuxième version).
- Ce qu'il faut regarder (thème clair et sombre) : la carte « Remplissage et finances » en haut, ses quatre chiffres clés, la jauge, la liste des tableaux ; solde en rouge s'il est négatif.
- Captures (harnais `b035`, tournoi homologué sans inscrit) : `docs/captures/B036/apres/` (clair et sombre). Non capturé : un tournoi avec inscrits, liste d'attente et paiements (à regarder à l'écran après publication).
- Ordre retenu : la carte se place au-dessus des deux cartes « bientôt » restantes (Salle, Stratégie), en pleine largeur.

## Questions pour Quentin

### 2026-11-06 — Tarif retenu pour les recettes attendues
- Contexte : un tableau peut avoir deux tarifs (en ligne / sur place), mais on ne sait pas encore comment chaque joueur paie.
- Article FFTT : aucun (clause d'organisateur).
- Options : A) tarif en ligne (sur place s'il n'y a pas d'en-ligne), comme le « montant à régler » déjà affiché au joueur ; B) tarif sur place ; C) selon le canal réel du joueur (à enregistrer).
- Choix provisoire de l'agent : A, pour rester cohérent avec le montant dû affiché.
- Réponse de Quentin :

### 2026-11-06 — Base de la dotation « % des inscriptions »
- Contexte : la part promise (ex. 50 % au vainqueur du tableau dames) se calcule sur quoi, avant le tournoi ?
- Article FFTT : aucun (règlement du tournoi : « part des droits d'engagement perçus »).
- Options : A) recettes attendues (tous les inscrits) ; B) seulement ce qui est encaissé ; C) les présents au pointage.
- Choix provisoire de l'agent : A (montant promis le plus élevé, donc solde le plus prudent).
- Réponse de Quentin :

### 2026-11-06 — Réduction de dotation (conditions de maintien) dans le solde
- Contexte : le règlement peut prévoir une dotation réduite sous un seuil de remplissage ou selon les absents.
- Article FFTT : aucun (IX.108 ne traite que des récompenses ; la réduction est une clause d'organisateur, `ConditionsMaintien`).
- Options : A) afficher la dotation pleine ; B) appliquer la réduction prévue d'après le remplissage actuel.
- Choix provisoire de l'agent : A (prudent : on ne compte pas sur une réduction qui n'est pas acquise).
- Réponse de Quentin :

### 2026-11-06 — Évolution des encaissements dans le temps
- Contexte : le cahier des charges parle de « données qui évoluent » ; le modèle ne garde pas la date d'un paiement (ni d'une inscription).
- Article FFTT : aucun.
- Options : A) ajouter une date au paiement (format de sauvegarde à étendre, rétrocompatible) pour tracer une courbe ; B) s'en tenir à l'état présent.
- Choix provisoire de l'agent : B pour l'instant (pas de changement du format de sauvegarde sans nécessité) ; la carte se recalcule à chaque ouverture.
- Réponse de Quentin :

### 2026-11-06 — Total arrondi une fois : écart possible de 0,01 € avec les lignes affichées
- Contexte : chaque ligne (tableau) est arrondie au centime, puis le total est la somme de ces lignes arrondies, arrondie une nouvelle fois. À la lecture du code, la somme affichée et le total concordent : l'écart de 0,01 € ne devrait pas apparaître, car chaque ligne est déjà un nombre de centimes. Il resterait possible seulement si l'affichage arrondissait autrement qu'au centime.
- Article FFTT : aucun.
- Options : A) accepter (recommandé : aucun écart attendu, pas de calcul en centimes entiers à refaire) ; B) passer tous les montants en centimes entiers pour supprimer tout doute (changement du cœur, sans effet visible prévu).
- Choix provisoire de l'agent : A.
- Réponse de Quentin :

## Notes pour l'état du projet

- `core/ServiceRemplissageFinances` → `core/BilanRemplissageFinances` (immuable : lignes par tableau et totaux ; tarif de `RemplissageTableaux.tarifEnLigne`, grille via `LibellesTableau.totalArgent` / `partInscriptions`).
- Carte « Remplissage et finances » dans `EcranCompletion` (deuxième version), présentation `ui/ecran/PresentationRemplissageFinances` testée hors JavaFX ; section CSS « B036 » en fin de `theme.css`.
- Format de sauvegarde : inchangé. Démo : inchangée.
- Limites connues (défauts restants, non bloquants) :
  - Paiement binaire : pas de paiement partiel ni de trop-payé (le modèle ne connaît que « payé » ou non).
  - Taux de remplissage global : ne compte que les tableaux limités ; un tableau sans limite de places est affiché sans taux (voulu, test `tableau_sans_limite_de_places_hors_du_taux_global`).
  - Réduction de dotation (conditions de maintien) non appliquée : dotation pleine affichée (question ci-dessus).
  - Classes CSS `carte-finances` et `finances-nom` posées dans `EcranCompletion` sans règle dans `theme.css` : sans effet visuel aujourd'hui, à régler si un style propre est voulu.
  - Quelques noms de classes complets au lieu d'imports dans les fichiers de la carte : style seulement.

## Fusion

À compléter par l'`integrateur`.

## Chiffres

- Tests : à mesurer par l'`integrateur` (`mvn -B test`, non lancé ici).
- Nouveaux tests (`@Test` comptés dans les fichiers ajoutés) : 10 (`ServiceRemplissageFinancesTest`) + 10 (`ServiceRemplissageFinancesCoherenceTest`) + 8 (`PresentationRemplissageFinancesTest`) = 28 annotations ; le compte rendu du développeur (18) et celui du vérificateur (9) ne couvrent pas tout le périmètre.
- Version de la démo : inchangée.
